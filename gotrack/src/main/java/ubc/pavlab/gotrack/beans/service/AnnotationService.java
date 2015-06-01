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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Evidence;
import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.EnrichmentDTO;
import ubc.pavlab.gotrack.model.dto.TrackDTO;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ApplicationScoped
public class AnnotationService implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8586832855128663515L;

    private static final Logger log = Logger.getLogger( AnnotationService.class );

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    @ManagedProperty("#{cache}")
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

    public Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> fetchTrackData( Species species,
            Gene g ) {

        Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allSeries = new HashMap<>();

        List<TrackDTO> resultset = annotationDAO.track( species.getId(), g.getSymbol() );

        for ( TrackDTO dto : resultset ) {

            Accession acc = cache.getAccession( dto.getAccession() );

            if ( acc == null ) {
                log.warn( "Accession (" + dto.getAccession() + ") not found!" );
                continue;
            }

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

            EvidenceReference er = new EvidenceReference( evidence, dto.getReference(), acc.getDataset() );

            Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> series = allSeries.get( acc );
            if ( series == null ) {
                series = new HashMap<>();
                allSeries.put( acc, series );
            }

            Map<GeneOntologyTerm, Set<EvidenceReference>> goIdMap = series.get( ed );
            if ( goIdMap == null ) {
                goIdMap = new HashMap<>();
                series.put( ed, goIdMap );
            }

            Set<EvidenceReference> ers = goIdMap.get( go );
            if ( ers == null ) {
                ers = new HashSet<>();
                goIdMap.put( go, ers );
            }

            ers.add( er );

        }

        return allSeries;

    }

    public Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> fetchEnrichmentData( Integer speciesId, Collection<Gene> genes ) {
        Set<Gene> geneSet = new HashSet<>( genes );

        Map<String, Gene> givenGenes = new HashMap<>();
        for ( Gene g : genes ) {
            givenGenes.put( g.getSymbol().toUpperCase(), g );
        }

        List<EnrichmentDTO> resultset = annotationDAO.enrich( speciesId, geneSet );

        Multimap<String, EnrichmentDTO> missingTerms = ArrayListMultimap.create();

        Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> data = new HashMap<>();

        for ( EnrichmentDTO enrichmentDTO : resultset ) {

            String symbol = enrichmentDTO.getSymbol();
            Gene g = givenGenes.get( symbol.toUpperCase() );
            if ( g == null ) {
                log.warn( "Could not find symbol:" + symbol + " in given genes." );
                // g = new Gene( symbol, species );
            }

            Edition ed = cache.getEdition( speciesId, enrichmentDTO.getEdition() );

            GeneOntologyTerm go = cache.getTerm( ed, enrichmentDTO.getGoId() );

            if ( go == null ) {
                log.debug( "Could not find (" + enrichmentDTO.getGoId() + ") in go_edition: " + ed.getGoEditionId() );
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

            for ( Entry<EnrichmentDTO, GeneOntologyTerm> entry : mapped.entrySet() ) {
                EnrichmentDTO enrichmentDTO = entry.getKey();

                String symbol = enrichmentDTO.getSymbol();
                Gene g = givenGenes.get( symbol.toUpperCase() );
                if ( g == null ) {
                    log.warn( "Could not find symbol:" + symbol + " in given genes." );
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

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

}
