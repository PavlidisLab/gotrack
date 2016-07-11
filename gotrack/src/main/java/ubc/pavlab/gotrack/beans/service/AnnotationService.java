/*
 * The gotrack project
 * 
 * Copyright (c) 2015 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.gotrack.beans.service;

import java.io.Serializable;
import java.sql.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Evidence;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.AnnotationDTO;
import ubc.pavlab.gotrack.model.dto.CategoryCountDTO;
import ubc.pavlab.gotrack.model.dto.DirectAnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.EnrichmentDTO;
import ubc.pavlab.gotrack.model.dto.SimpleAnnotationDTO;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Service layer on top of annotation DAO. Contains methods for fetching information related to annotations and counts
 * thereof from the database. This does NOT include methods that are purely for caching.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ApplicationScoped
public class AnnotationService implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8586832855128663515L;

    private static final Logger log = Logger.getLogger( AnnotationService.class );

    @Inject
    private DAOFactoryBean daoFactoryBean;

    @Inject
    private Cache cache;

    private AnnotationDAO annotationDAO;

    /**
     * 
     */
    public AnnotationService() {
        log.info( "AnnotationService created" );
    }

    @PostConstruct
    public void init() {
        log.info( "AnnotationService init" );
        annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();

    }

    /**
     * Method used to fetch data from database related to the GeneView
     * 
     * @param species species
     * @param g gene
     * @return Sets of annotations for each term in each edition related to this gene
     */
    public Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> fetchTrackData( Species species, Gene g ) {

        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> trackData = new LinkedHashMap<>();

        List<AnnotationDTO> resultset = annotationDAO.track( g );

        for ( AnnotationDTO dto : resultset ) {

            Edition ed = cache.getEdition( species.getId(), dto.getEdition() );

            if ( ed == null ) {
                log.warn( "Edition (" + dto.getEdition() + ") not found!" );
                continue;
            }

            GeneOntologyTerm go = cache.getTerm( ed, dto.getGoId() );

            if ( go == null ) {
                log.warn( "GeneOntologyTerm (" + dto.getGoId() + ") not found!" );
                continue;
            }

            Evidence evidence = cache.getEvidence( dto.getEvidence() );

            if ( evidence == null ) {
                log.warn( "Evidence (" + dto.getEvidence() + ") not found!" );
                continue;
            }

            Map<GeneOntologyTerm, Set<Annotation>> goMap = trackData.get( ed );
            if ( goMap == null ) {
                goMap = new HashMap<>();
                trackData.put( ed, goMap );
            }

            Set<Annotation> annotationSet = goMap.get( go );

            if ( annotationSet == null ) {
                annotationSet = new HashSet<>();
                goMap.put( go, annotationSet );
            }

            annotationSet.add( new Annotation( dto.getQualifier(), evidence, dto.getReference() ) );

        }

        return trackData;

    }

    /**
     * Method used to fetch data from database related to the EnrichmentView
     * 
     * @param speciesId species id
     * @param genes gene
     * @return Sets of terms directly annotated to each gene in each edition
     */
    public Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> fetchEnrichmentData( Integer speciesId,
            Collection<Gene> genes ) {
        Set<Gene> geneSet = new HashSet<>( genes );

        // Used to map strings back to genes, small efficiency boost as we don't have to hit gene cache in Cache service
        Map<Integer, Gene> givenGenes = new HashMap<>();
        for ( Gene g : genes ) {
            givenGenes.put( g.getId(), g );
        }

        List<EnrichmentDTO> resultset = annotationDAO.enrich( geneSet );

        Multimap<String, EnrichmentDTO> missingTerms = ArrayListMultimap.create();

        Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> data = new HashMap<>();

        for ( EnrichmentDTO enrichmentDTO : resultset ) {

            Integer geneId = enrichmentDTO.getGeneId();
            Gene g = givenGenes.get( geneId );
            if ( g == null ) {
                log.warn( "Could not find Gene Id:" + geneId + " in given genes." );
                // g = new Gene( symbol, species );
            }

            Edition ed = cache.getEdition( speciesId, enrichmentDTO.getEdition() );

            GeneOntologyTerm go = cache.getTerm( ed, enrichmentDTO.getGoId() );

            if ( go == null ) {
                log.debug(
                        "Could not find (" + enrichmentDTO.getGoId() + ") in GO Edition Id: " + ed.getGoEditionId() );
                missingTerms.put( enrichmentDTO.getGoId(), enrichmentDTO );
                continue;
            }

            Map<Edition, Set<GeneOntologyTerm>> geneEntry = data.get( g );
            if ( geneEntry == null ) {
                geneEntry = new HashMap<>();
                data.put( g, geneEntry );
            }

            Set<GeneOntologyTerm> goSet = geneEntry.get( ed );
            if ( goSet == null ) {
                goSet = new HashSet<>();
                geneEntry.put( ed, goSet );
            }

            goSet.add( go );

        }

        // Because of reasons (badly annotated / misdated / mismatched annotations to GO editions / missing secondary ids)
        // We will have terms that are not found in their given GO edition. We attempt to find this term in another edition.
        if ( !missingTerms.isEmpty() ) {
            Map<EnrichmentDTO, GeneOntologyTerm> mapped = new HashMap<>();
            log.info( "Attempting to find information for (" + missingTerms.size() + ") missing terms" );
            for ( Map<Edition, Set<GeneOntologyTerm>> m1 : data.values() ) {
                if ( missingTerms.isEmpty() ) {
                    break;
                }
                for ( Set<GeneOntologyTerm> m2 : m1.values() ) {
                    if ( missingTerms.isEmpty() ) {
                        break;
                    }
                    for ( GeneOntologyTerm t : m2 ) {
                        if ( missingTerms.isEmpty() ) {
                            break;
                        }
                        if ( missingTerms.containsKey( t.getGoId() ) ) {
                            log.debug( "Mapping (" + t.getGoId() + ") to Term: " + t );
                            for ( EnrichmentDTO dto : missingTerms.get( t.getGoId() ) ) {
                                mapped.put( dto, t );
                            }
                            missingTerms.removeAll( t.getGoId() );

                        }
                    }
                }

            }

            if ( !missingTerms.isEmpty() ) {
                log.warn( "Could not find information for (" + missingTerms.size() + ") terms" );
            }

            // For the terms which we have successfully found in another edition, we create 'dummy' terms as stand-ins.

            // For those we couldn't match, that usually means it was annotated under an incorrect id or our annotation/GO 
            // editions are mismatched. Either way we refrain from attempting to fix these. This is a big TODO.
            for ( Entry<EnrichmentDTO, GeneOntologyTerm> entry : mapped.entrySet() ) {
                EnrichmentDTO enrichmentDTO = entry.getKey();

                Integer geneId = enrichmentDTO.getGeneId();
                Gene g = givenGenes.get( geneId );
                if ( g == null ) {
                    log.warn( "Could not find Gene Id:" + geneId + " in given genes." );
                    // g = new Gene( symbol, species );
                }

                Edition ed = cache.getEdition( speciesId, enrichmentDTO.getEdition() );

                GeneOntologyTerm go = new GeneOntologyTerm( entry.getValue() );

                Map<Edition, Set<GeneOntologyTerm>> geneEntry = data.get( g );
                if ( geneEntry == null ) {
                    geneEntry = new HashMap<>();
                    data.put( g, geneEntry );
                }

                Set<GeneOntologyTerm> goSet = geneEntry.get( ed );
                if ( goSet == null ) {
                    goSet = new HashSet<>();
                    geneEntry.put( ed, goSet );
                }

                goSet.add( go );

            }

        }

        return data;

    }

    /**
     * Method used to fetch data from database related to the Enrichment for a single edition
     * 
     * @param ed edition
     * @param speciesId species id
     * @param genes gene
     * @return Sets of terms directly annotated to each gene in each edition
     */
    public Map<Gene, Set<GeneOntologyTerm>> fetchSingleEnrichmentData( Edition ed, Integer speciesId,
            Collection<Gene> genes ) {
        Set<Gene> geneSet = new HashSet<>( genes );

        // Used to map strings back to genes, small efficiency boost as we don't have to hit gene cache in Cache service
        Map<Integer, Gene> givenGenes = new HashMap<>();
        for ( Gene g : genes ) {
            givenGenes.put( g.getId(), g );
        }

        List<SimpleAnnotationDTO> resultset = annotationDAO.enrichSingleEdition( ed, geneSet );

        Multimap<String, SimpleAnnotationDTO> missingTerms = ArrayListMultimap.create();

        Map<Gene, Set<GeneOntologyTerm>> data = new HashMap<>();

        for ( SimpleAnnotationDTO enrichmentDTO : resultset ) {

            Integer geneId = enrichmentDTO.getGeneId();
            Gene g = givenGenes.get( geneId );
            if ( g == null ) {
                log.warn( "Could not find Gene Id:" + geneId + " in given genes." );
                // g = new Gene( symbol, species );
            }

            GeneOntologyTerm go = cache.getTerm( ed, enrichmentDTO.getGoId() );

            if ( go == null ) {
                log.debug(
                        "Could not find (" + enrichmentDTO.getGoId() + ") in GO Edition Id: " + ed.getGoEditionId() );
                missingTerms.put( enrichmentDTO.getGoId(), enrichmentDTO );
                continue;
            }

            Set<GeneOntologyTerm> goSet = data.get( g );
            if ( goSet == null ) {
                goSet = new HashSet<>();
                data.put( g, goSet );
            }

            goSet.add( go );

        }

        // Because of reasons (badly annotated / misdated / mismatched annotations to GO editions / missing secondary ids)
        // We will have terms that are not found in their given GO edition.
        if ( !missingTerms.isEmpty() ) {
            log.warn( "Could not find information for (" + missingTerms.size() + ") terms" );
        }

        return data;

    }

    /**
     * Method used to get annotation count grouped by evidence category for a term.
     * 
     * @param t term
     * @return Counts annotations for each evidence category grouped by date
     */
    public Map<String, Map<Date, Integer>> fetchCategoryCounts( GeneOntologyTerm t ) {
        return fetchCategoryCounts( t.getGoId() );
    }

    /**
     * Method used to get annotation count grouped by evidence category for a term.
     * 
     * @param goId term
     * @return Counts annotations for each evidence category grouped by date
     */
    public Map<String, Map<Date, Integer>> fetchCategoryCounts( String goId ) {
        Map<String, Map<Date, Integer>> results = new LinkedHashMap<>();
        List<CategoryCountDTO> resultset = annotationDAO.categoryCounts( goId );
        for ( CategoryCountDTO dto : resultset ) {
            Map<Date, Integer> m2 = results.get( dto.getCategory() );
            if ( m2 == null ) {
                m2 = new HashMap<>();
                results.put( dto.getCategory(), m2 );
            }
            m2.put( dto.getDate(), dto.getCount() );
        }
        return results;
    }

    /**
     * This is not used as we are currently doing this with cached data.
     * 
     * @param goId term
     * @return Counts of genes that are directly annotated with this term grouped by species and edition.
     */
    public Map<Integer, Map<Edition, Integer>> fetchDirectGeneCounts( GeneOntologyTerm t ) {
        return fetchDirectGeneCounts( t.getGoId() );
    }

    /**
     * This is not used as we are currently doing this with cached data.
     * 
     * @param t term
     * @return Counts of genes that are directly annotated with this term grouped by species and edition.
     */
    public Map<Integer, Map<Edition, Integer>> fetchDirectGeneCounts( String goId ) {
        Map<Integer, Map<Edition, Integer>> results = new HashMap<>();
        List<DirectAnnotationCountDTO> resultset = annotationDAO.directGeneCounts( goId );
        for ( DirectAnnotationCountDTO dto : resultset ) {
            Map<Edition, Integer> m2 = results.get( dto.getSpecies() );
            if ( m2 == null ) {
                m2 = new HashMap<>();
                results.put( dto.getSpecies(), m2 );
            }

            Edition ed = cache.getEdition( dto.getSpecies(), dto.getEdition() );

            m2.put( ed, dto.getCount() );
        }
        return results;
    }

}
