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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.dto.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Tuples;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;

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
     * @param g gene
     * @return Sets of annotations for each term in each edition related to this gene
     */
    public Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> fetchTrackData( Gene g ) {
        return fetchTrackData( g, null, null );
    }

    /**
     * Method used to fetch data from database related to the GeneView where  @minimum <= edition <= @maximum
     *
     * @param g gene
     * @return Sets of annotations for each term in each edition related to this gene
     */
    private Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> fetchTrackData( Gene g, Edition min, Edition max ) {
        if ( g == null ) {
            return null;
        }

        int minEdition = min == null ? cache.getGlobalMinEdition( g.getSpecies() ).getEdition() : min.getEdition();
        int maxEdition = max == null ? cache.getCurrentEditions( g.getSpecies() ).getEdition() : max.getEdition();

        List<AnnotationDTO> resultset = annotationDAO.fullAnnotationRangeEditions( g, minEdition, maxEdition );

        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> trackData = Maps.newLinkedHashMap();
        for ( AnnotationDTO dto : resultset ) {

            Edition ed = cache.getEdition( g.getSpecies(), dto.getEdition() ); //TODO: current-edition-refactor

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
     * @param species species
     * @param genes   gene
     * @return Sets of terms directly annotated to each gene in each edition
     */
    public Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> fetchEnrichmentData( Species species, Collection<Gene> genes ) {
        return fetchEnrichmentData( species, genes, null, null );
    }

    /**
     * Method used to fetch data from database related to the EnrichmentView where @minimum <= edition <= @maximum
     *
     * @param species species
     * @param genes   gene
     * @return Sets of terms directly annotated to each gene in each edition
     */
    private Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> fetchEnrichmentData( Species species, Collection<Gene> genes, Edition min, Edition max ) {
        Set<Gene> geneSet = new HashSet<>( genes );

        // Used to map strings back to genes, small efficiency boost as we don't have to hit gene cache in Cache service
        Map<String, Gene> givenGenes = new HashMap<>();
        for ( Gene g : genes ) {
            givenGenes.put( g.getAccession().getAccession(), g );
        }

        int minEdition = min == null ? cache.getGlobalMinEdition( species ).getEdition() : min.getEdition();
        int maxEdition = max == null ? cache.getCurrentEditions( species ).getEdition() : max.getEdition();

        List<EnrichmentDTO> resultset = annotationDAO.simpleAnnotationRangeEditions( geneSet, minEdition, maxEdition );

        Multimap<String, EnrichmentDTO> missingTerms = ArrayListMultimap.create();

        Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> data = new HashMap<>();

        for ( EnrichmentDTO enrichmentDTO : resultset ) {

            String geneAccession = enrichmentDTO.getAccession();
            Gene g = givenGenes.get( geneAccession );
            if ( g == null ) {
                log.warn( "Could not find Accession:" + geneAccession + " in given genes." );
                // g = new Gene( symbol, species );
            }

            Edition ed = cache.getEdition( species, enrichmentDTO.getEdition() ); //TODO: current-edition-refactor

            GeneOntologyTerm go = cache.getTerm( ed, enrichmentDTO.getGoId() );

            if ( go == null ) {
                log.debug(
                        "Could not find (" + enrichmentDTO.getGoId() + ") in GO Edition Id: " + ed.getGoEdition().getId() );
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

                String geneAccession = enrichmentDTO.getAccession();
                Gene g = givenGenes.get( geneAccession );
                if ( g == null ) {
                    log.warn( "Could not find Accession:" + geneAccession + " in given genes." );
                    // g = new Gene( symbol, species );
                }

                Edition ed = cache.getEdition( species, enrichmentDTO.getEdition() ); //TODO: current-edition-refactor

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
     * @param ed    edition
     * @param genes gene
     * @return Sets of terms directly annotated to each gene in each edition
     */
    public Map<Gene, Set<GeneOntologyTerm>> fetchSingleEnrichmentData( Edition ed, Collection<Gene> genes ) {
        Set<Gene> geneSet = new HashSet<>( genes );

        // Used to map strings back to genes, small efficiency boost as we don't have to hit gene cache in Cache service
        Map<String, Gene> givenGenes = new HashMap<>();
        for ( Gene g : genes ) {
            givenGenes.put( g.getAccession().getAccession(), g );
        }

        List<SimpleAnnotationDTO> resultset = annotationDAO.simpleAnnotationSingleEdition( ed, geneSet );

        Multimap<String, SimpleAnnotationDTO> missingTerms = ArrayListMultimap.create();

        Map<Gene, Set<GeneOntologyTerm>> data = new HashMap<>();

        for ( SimpleAnnotationDTO enrichmentDTO : resultset ) {

            String geneAccession = enrichmentDTO.getAccession();
            Gene g = givenGenes.get( geneAccession );
            if ( g == null ) {
                log.warn( "Could not find Accession:" + geneAccession + " in given genes." );
                // g = new Gene( symbol, species );
            }

            GeneOntologyTerm go = cache.getTerm( ed, enrichmentDTO.getGoId() );

            if ( go == null ) {
                log.debug(
                        "Could not find (" + enrichmentDTO.getGoId() + ") in GO Edition Id: " + ed.getGoEdition().getId() );
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
        return fetchCategoryCounts( t.getGoId(), null, null );
    }

    /**
     * Method used to get annotation count grouped by evidence category for a term between two dates
     *
     * @param t term
     * @return Counts annotations for each evidence category grouped by date
     */
    private Map<String, Map<Date, Integer>> fetchCategoryCounts( GeneOntologyTerm t, Date min, Date max ) {
        return fetchCategoryCounts( t.getGoId(), min, max );
    }

    /**
     * Method used to get annotation count grouped by evidence category for a term between two dates
     *
     * @param goId term
     * @return Counts annotations for each evidence category grouped by date
     */
    private Map<String, Map<Date, Integer>> fetchCategoryCounts( String goId, Date min, Date max ) {
        Map<String, Map<Date, Integer>> results = new LinkedHashMap<>();

        Date startDate = min == null ? cache.getGlobalMinGOEdition().getDate() : min;
        Date stopDate = max == null ? cache.getGlobalMaxGOEdition().getDate() : max;

        List<CategoryCountDTO> resultset = annotationDAO.categoryCountsRangeDates( goId, startDate, stopDate );
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
     * Method used to get annotation count in a species grouped by evidence category for a term
     *
     * @param term term
     * @param species species
     * @return Counts annotations for each evidence category grouped by date
     */
    public Map<String, Map<Date, Integer>> fetchCategoryCountsInSpecies( GeneOntologyTerm term, Species species ) {
        return fetchCategoryCountsInSpecies( term.getGoId(), species, null, null );
    }

    /**
     * Method used to get annotation count in a species grouped by evidence category for a term between two editions
     *
     * @param goId term
     * @param species species
     * @return Counts annotations for each evidence category grouped by date
     */
    private Map<String, Map<Date, Integer>> fetchCategoryCountsInSpecies( String goId, Species species, Edition min, Edition max ) {
        Map<String, Map<Date, Integer>> results = new LinkedHashMap<>();

        int minEdition = min == null ? cache.getGlobalMinEdition( species ).getEdition() : min.getEdition();
        int maxEdition = max == null ? cache.getCurrentEditions( species ).getEdition() : max.getEdition();

        for ( EditionCategoryCountDTO dto : annotationDAO.categoryCountsSingleSpeciesRangeEditions( goId, species, minEdition, maxEdition ) ) {

            Edition ed = cache.getEdition( species, dto.getEdition() ); //TODO: current-edition-refactor

            if ( ed == null ) {
                log.warn( "Edition (" + dto.getEdition() + ") not found!" );
                continue;
            }

            results.computeIfAbsent( dto.getCategory(), k -> new HashMap<>() ).put( ed.getDate(), dto.getCount() );
        }
        return results;
    }

    /**
     * This is not used as we are currently doing this with cached data.
     *
     * @param t term
     * @return Counts of genes that are directly annotated with this term grouped by species and edition.
     */
    public Map<Integer, Map<Edition, Integer>> fetchDirectGeneCounts( GeneOntologyTerm t ) {
        return fetchDirectGeneCounts( t.getGoId() );
    }

    /**
     * This is not used as we are currently doing this with cached data.
     *
     * @param goId term
     * @return Counts of genes that are directly annotated with this term grouped by species and edition.
     */
    public Map<Integer, Map<Edition, Integer>> fetchDirectGeneCounts( String goId ) {
        Map<Integer, Map<Edition, Integer>> results = new HashMap<>();
        List<DirectAnnotationCountDTO> resultset = annotationDAO.directGeneCountsAllEditions( goId );
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

    public Map<Gene, Set<GeneOntologyTerm>> fetchEditionSimple(Species species, Edition edition) {

        List<Tuples.Tuple2<String, String>> resultset = annotationDAO.simpleAnnotationSingleEditionCompleteSpecies( species, edition);

        Multimap<String, Tuples.Tuple2<String, String>> missingTerms = ArrayListMultimap.create();

        Map<Gene, Set<GeneOntologyTerm>> data  = Maps.newHashMap();

        for ( Tuples.Tuple2<String, String> tup : resultset ) {

            Gene g = cache.getCurrentGene( tup.getT1() );
            if ( g == null ) {
                log.warn( "Could not find Accession:" + tup.getT1() );
                continue;
                //TODO: Create mock gene? requires loading symbols from db. ex. g = new Gene( symbol, species );
            }

            GeneOntologyTerm go = cache.getTerm( edition, tup.getT2());

            if ( go == null ) {
                log.debug(
                        "Could not find (" + tup.getT2() + ") in GO Edition Id: " + edition.getGoEdition().getId() );
                missingTerms.put( tup.getT2(), tup );
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

        // Propagate terms
        for ( Entry<Gene, Set<GeneOntologyTerm>> geneSetEntry : data.entrySet() ) {
            geneSetEntry.setValue( cache.propagate(geneSetEntry.getValue(), edition) );
        }

        return data;
    }



}
