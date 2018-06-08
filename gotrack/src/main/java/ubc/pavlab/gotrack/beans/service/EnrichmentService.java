/*
 * The gotrack project
 * 
 * Copyright (c) 2016 University of British Columbia
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

import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.analysis.*;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO Document Me
 *
 * @author mjacobson
 * @version $Id$
 */
@Named
@ApplicationScoped
public class EnrichmentService implements Serializable {

    private static final long serialVersionUID = -4619509776706358143L;

    private static final Logger log = Logger.getLogger( EnrichmentService.class );

    // Max size of hit list
    private static final int MAX_GENESET_SIZE = 20000;

    @Inject
    private Cache cache;

    @Inject
    private AnnotationService annotationService;

    public EnrichmentService() {
        log.info( "EnrichmentService created" );
    }

    @PostConstruct
    public void init() {
        log.info( "EnrichmentService init" );
    }

    @PreDestroy
    public void destroyed() {
        log.info( "EnrichmentService destroyed" );
    }

    /**
     * Runs enrichment/similarity/stability analyses given input settings.
     *
     * @param genes             hitlist
     * @param species           species id
     * @param enrichmentOptions Enrichment options
     * @param topN              number of top terms to use for top N series
     * @param statusPoller      poller for live status updates
     * @return Container class holding the enrichment and stability/similarity analyses
     */
    public CombinedAnalysis combinedAnalysis( Set<Gene> genes, Species species, EnrichmentAnalysisOptions enrichmentOptions, Edition similarityReferenceEdition, int topN, StatusPoller statusPoller ) {

        statusPoller.newStatus( "Starting Enrichment Analysis", 0 );
        EnrichmentAnalysis analysis = enrichment( genes,
                species,
                enrichmentOptions.getMultipleTestCorrection(),
                enrichmentOptions.getThreshold(),
                enrichmentOptions.getMinAnnotatedPopulation(),
                enrichmentOptions.getMaxAnnotatedPopulation(),
                Sets.newHashSet( enrichmentOptions.getAspects() ),
                statusPoller );

        if ( analysis == null ) {
            statusPoller.newStatus( "Failed", 100 );
            return new CombinedAnalysis( null, null, null, false );
        }

        log.info( "Running similarity analysis" );

        statusPoller.newStatus( "Running Similarity Analyses on all editions...", 75 );

        SimilarityAnalysis similarityAnalysis = new SimilarityAnalysis( analysis, topN, similarityReferenceEdition, SimilarityMethod.JACCARD, cache );
        statusPoller.completeStatus();
        log.info( "Running stability analysis" );

        statusPoller.newStatus( "Running Stability Analyses on all editions...", 85 );

        StabilityAnalysis stabilityAnalysis = new StabilityAnalysis( analysis );
        statusPoller.completeStatus();
        log.info( "Analysis Complete" );

        return new CombinedAnalysis( analysis, stabilityAnalysis, similarityAnalysis, true );

    }

    /**
     * Runs enrichment/similarity/stability analyses given input settings.
     *
     * @param genes        hitlist
     * @param species      species id
     * @param mtc          method of multiple tests correction
     * @param thresh       Either p-value cutoff if using Bonferroni or FDR level if using BH step-up
     * @param min          minimum geneset size a specific term must have to be included in results
     * @param max          maximum geneset size a specific term must have to be included in results
     * @param aspects      only add these aspects, ignore filter if null or empty
     * @param statusPoller poller for live status updates
     * @return Container class holding the enrichment and stability/similarity analyses
     */
    public EnrichmentAnalysis enrichment( Set<Gene> genes, Species species, MultipleTestCorrection mtc, double thresh, int min,
                                          int max, Set<Aspect> aspects, StatusPoller statusPoller ) {

        Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = retrieveData( genes, species, aspects, statusPoller );

        if ( geneGOMap == null || geneGOMap.isEmpty() ) {
            statusPoller.newStatus( "Failed", 100 );
            return null;
        }

        statusPoller.newStatus( "Retrieving Sample Sizes...", 50 );
        // This contains a part of the contingency tables we will need to create
        //        Map<Edition, Integer> sampleSizes = calculateSampleSizes( geneGOMap );
        statusPoller.completeStatus();

        statusPoller.newStatus( "Running Overrepresentation Analyses on all editions...", 55 );
        log.info( "Running enrichment analysis" );

        EnrichmentAnalysis analysis = new EnrichmentAnalysis( geneGOMap, min, max, mtc, thresh, cache, species );

        statusPoller.completeStatus();

        return analysis;

    }

    /**
     * Retrieves data from cache and/or from database for given hit list, writing live status udpates to the
     * StatusPoller.
     *
     * @param genes        set of genes to either retrieve
     * @param species      species id of genes
     * @param filterAspect keep only terms of these aspects, if null or empty the filter is not applied
     * @param statusPoller poller for live status updates
     * @return data necessary for enrichment of given hitlist
     */
    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> retrieveData( Set<Gene> genes, Species species,
                                                                         Set<Aspect> filterAspect, StatusPoller statusPoller ) {

        if ( genes != null && !genes.isEmpty() ) {
            log.info( "Current species: " + species );
            log.info( "Geneset Size: " + genes.size() );

            if ( genes.size() > MAX_GENESET_SIZE ) {
                statusPoller.newStatus( "Gene Hit List too large; maximum geneset size is " + MAX_GENESET_SIZE + "!",
                        50 );
                return null;
            }

            log.info( "retrieving gene data..." );
            statusPoller.newStatus( "Retrieving Gene Information from cache...", 5 );

            // Container for final enrichment data
            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = new HashMap<>();

            Set<Gene> genesToLoad = new HashSet<>();
            for ( Gene gene : genes ) {
                Map<Edition, Set<GeneOntologyTerm>> cachedGeneData = cache.getEnrichmentData( gene );
                if ( cachedGeneData != null ) {
                    addGeneData( gene, cachedGeneData, filterAspect, geneGOMap );
                } else {
                    genesToLoad.add( gene );
                }
            }
            statusPoller.completeStatus();

            statusPoller.newStatus( "Retrieving Gene Information from database...", 10 );

            // If not all genes have been loaded from cache we must hit the database for the rest and cache the results
            if ( !genesToLoad.isEmpty() ) {

                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB;

                geneGOMapFromDB = annotationService.fetchEnrichmentData( species, genesToLoad );

                statusPoller.completeStatus();
                statusPoller.newStatus( "Propagating GO Terms...", 30 );

                log.info( "Propagating GO Terms..." );
                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> prop = propagate( geneGOMapFromDB );
                log.info( "Propagating GO Terms... COMPLETE " );

                for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : prop.entrySet() ) {

                    addGeneData( geneEntry.getKey(), geneEntry.getValue(), filterAspect, geneGOMap );
                    cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
                }
                statusPoller.completeStatus();

                log.info( "Retrieved (" + genesToLoad.size() + ") genes from db and ("
                        + (genes.size() - genesToLoad.size()) + ") from cache" );
            } else {
                log.info( "Retrieved all (" + genes.size() + ") genes from cache" );

            }
            // timer.stop();
            // log.info( "Total time: " + timer );
            // for ( Entry<String, Long> entry : timerMap.entrySet() ) {
            // log.info( entry.getKey() + " - " + entry.getValue() + " - " + entry.getValue()
            // / ( double ) timer.getNanoTime() );
            // }

            return geneGOMap;

        } else {
            log.info( "Empty geneset" );
            return null;
        }
    }

    /**
     * Add a new gene/data pair to the data of previously retrieved genes.
     *
     * @param g              gene we are adding data for
     * @param cachedGeneData cached data for given gene that will be added to enrichment data
     * @param filterAspect   only add these aspects, ignore filter if null or empty
     * @param data           enrichment data for previously retrieved genes
     */
    private void addGeneData( Gene g, Map<Edition, Set<GeneOntologyTerm>> cachedGeneData, Set<Aspect> filterAspect,
                              Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> data ) {

        boolean bypassFilter = (filterAspect == null || filterAspect.size() == 0
                || filterAspect.size() == Aspect.values().length);

        for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : cachedGeneData.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, Set<Gene>> m1 = data.get( ed );

            if ( m1 == null ) {
                // Previously had no data for this edition
                m1 = new HashMap<>();
                data.put( ed, m1 );
            }

            for ( GeneOntologyTerm term : editionEntry.getValue() ) {
                if ( bypassFilter || filterAspect.contains( term.getAspect() ) ) {
                    Set<Gene> geneSet = m1.get( term );
                    if ( geneSet == null ) {
                        // Previously had no genes for this term in this edition
                        geneSet = new HashSet<>();
                        m1.put( term, geneSet );
                    }

                    geneSet.add( g );
                }

            }

        }
    }

    /**
     * Propagates the term sets in data retrieved from database
     *
     * @param geneGOMapFromDB unpropagated data retrieved straight from database
     * @return Map adding the propagated terms into the term sets
     */
    private Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> propagate(
            Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB ) {
        Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> propagatedData = new HashMap<>();
        for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : geneGOMapFromDB.entrySet() ) {
            Gene g = geneEntry.getKey();
            Map<Edition, Set<GeneOntologyTerm>> propagatedSeries = new HashMap<>();
            propagatedData.put( g, propagatedSeries );
            Map<Edition, Set<GeneOntologyTerm>> series = geneEntry.getValue();
            for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : series.entrySet() ) {
                Edition ed = editionEntry.getKey();
                Set<GeneOntologyTerm> propagatedTerms = GeneOntologyTerm.propagate( editionEntry.getValue().stream() ).collect( Collectors.toSet() );

                if ( propagatedTerms == null ) {
                    // No ontology exists for this edition
                } else {
                    propagatedSeries.put( ed, propagatedTerms );
                }

            }

        }
        // cache.ontologyStats();
        return propagatedData;
    }

    /**
     * Calculate total number of genes existing in each edition from the hitlist
     *
     * @param geneGOMap enrichment data
     * @return Map of Edition -> Number of genes from hit list that existed in that edition (existence meaning had any
     * annotations)
     */
    private Map<Edition, Integer> calculateSampleSizes( Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap ) {
        log.info( "Retrieving sample sizes" );

        // Map<Edition, Integer> sampleSizes = annotationDAO.enrichmentSampleSizes( currentSpeciesId, genes );
        Map<Edition, Integer> sampleSizes = new HashMap<>();
        Map<Edition, Set<Gene>> testMap = new HashMap<>();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Gene>>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Set<Gene> gset = new HashSet<>();
            testMap.put( ed, gset );
            for ( Entry<GeneOntologyTerm, Set<Gene>> termEntry : editionEntry.getValue().entrySet() ) {
                gset.addAll( termEntry.getValue() );

            }

        }

        for ( Entry<Edition, Set<Gene>> editionEntry : testMap.entrySet() ) {
            sampleSizes.put( editionEntry.getKey(), editionEntry.getValue().size() );
        }

        return sampleSizes;
    }

    /**
     * Runs enrichment/similarity/stability analyses given input settings.
     *
     * @param eds     set of editions to run the analysis on
     * @param genes   hitlist
     * @param species species id
     * @param mtc     method of multiple tests correction
     * @param thresh  Either p-value cutoff if using Bonferroni or FDR level if using BH step-up
     * @param min     minimum geneset size a specific term must have to be included in results
     * @param max     maximum geneset size a specific term must have to be included in results
     * @param aspects only add these aspects, ignore filter if null or empty
     * @return Enrichment Results
     */
    public EnrichmentAnalysis enrichment( Set<Edition> eds, Set<Gene> genes, Species species,
                                          MultipleTestCorrection mtc, double thresh, int min, int max, Set<Aspect> aspects ) {

        Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = new HashMap<>();

        for ( Edition ed : eds ) {
            geneGOMap.put( ed, retrieveData( ed, genes, aspects ) );
        }

        if ( geneGOMap.isEmpty() ) {
            return null;
        }

        return new EnrichmentAnalysis( geneGOMap, min, max, mtc, thresh, cache, species );
    }

    /**
     * Runs enrichment/similarity/stability analyses given input settings.
     *
     * @param ed      edition
     * @param genes   hitlist
     * @param species species id
     * @param mtc     method of multiple tests correction
     * @param thresh  Either p-value cutoff if using Bonferroni or FDR level if using BH step-up
     * @param min     minimum geneset size a specific term must have to be included in results
     * @param max     maximum geneset size a specific term must have to be included in results
     * @param aspects only add these aspects, ignore filter if null or empty
     * @return Enrichment Results
     */
    public Enrichment<GeneOntologyTerm, Gene> singleEnrichment( Edition ed, Set<Gene> genes, Species species,
                                                                MultipleTestCorrection mtc, double thresh, int min, int max, Set<Aspect> aspects ) {

        Map<GeneOntologyTerm, Set<Gene>> data = retrieveData( ed, genes, aspects );

        if ( data == null || data.isEmpty() ) {
            return null;
        }

        max = max < 1 ? Integer.MAX_VALUE : max;

        Population<GeneOntologyTerm> population = Population.cachedGOPopulation( cache, ed );

        Enrichment<GeneOntologyTerm, Gene> enrichment = new Enrichment<>( mtc, thresh, min, max );

        enrichment.runAnalysis( CompletePopulation.standardCompletePopulation( data ), population );

        return enrichment;
    }

    /**
     * Retrieves data from cache/database for given hit list in given edition.
     *
     * @param ed             Edition to retrieve data from
     * @param genes          set of genes to either retrieve
     * @param filterAspect   keep only terms of these aspects, if null or empty the filter is not applied
     * @return data necessary for enrichment of given hitlist
     */
    private Map<GeneOntologyTerm, Set<Gene>> retrieveData( Edition ed, Set<Gene> genes, Set<Aspect> filterAspect ) {

        if ( genes != null && !genes.isEmpty() ) {
            log.info( "Current species: " + ed.getSpecies() );
            log.info( "Geneset Size: " + genes.size() );

            if ( genes.size() > MAX_GENESET_SIZE ) {
                log.info( "Gene Hit List too large; maximum geneset size is " + MAX_GENESET_SIZE + "!" );
                return null;
            }

            log.info( "retrieving gene data..." );

            // Container for final enrichment data
            Map<GeneOntologyTerm, Set<Gene>> geneGOMap = new HashMap<>();

            Set<Gene> genesToLoad = new HashSet<>();
            for ( Gene gene : genes ) {
                Map<Edition, Set<GeneOntologyTerm>> cachedGeneData = cache.getEnrichmentData( gene );
                if ( cachedGeneData != null ) {
                    Set<GeneOntologyTerm> edData = cachedGeneData.get( ed );
                    if ( edData != null ) {
                        addGeneData( gene, cachedGeneData.get( ed ), filterAspect, geneGOMap );
                    }
                } else {
                    genesToLoad.add( gene );
                }
            }

            // If not all genes have been loaded from cache we must hit the database for the rest
            if ( !genesToLoad.isEmpty() ) {

                Map<Gene, Set<GeneOntologyTerm>> geneGOMapFromDB;

                geneGOMapFromDB = annotationService.fetchSingleEnrichmentData( ed, genesToLoad );

                for ( Entry<Gene, Set<GeneOntologyTerm>> geneEntry : geneGOMapFromDB.entrySet() ) {
                    addGeneData( geneEntry.getKey(), GeneOntologyTerm.propagate( geneEntry.getValue().stream() ).collect( Collectors.toSet() ), filterAspect, geneGOMap );
                }

                log.info( "Retrieved (" + genesToLoad.size() + ") genes from db and ("
                        + (genes.size() - genesToLoad.size()) + ") from cache" );
            } else {
                log.info( "Retrieved all (" + genes.size() + ") genes from cache" );

            }
            // timer.stop();
            // log.info( "Total time: " + timer );
            // for ( Entry<String, Long> entry : timerMap.entrySet() ) {
            // log.info( entry.getKey() + " - " + entry.getValue() + " - " + entry.getValue()
            // / ( double ) timer.getNanoTime() );
            // }

            return geneGOMap;

        } else {
            log.info( "Empty geneset" );
            return null;
        }
    }

    private void addGeneData( Gene gene, Set<GeneOntologyTerm> goSet, Set<Aspect> filterAspect,
                              Map<GeneOntologyTerm, Set<Gene>> geneGOMap ) {
        boolean bypassFilter = (filterAspect == null || filterAspect.size() == 0
                || filterAspect.size() == Aspect.values().length);

        for ( GeneOntologyTerm term : goSet ) {
            if ( bypassFilter || filterAspect.contains( term.getAspect() ) ) {
                Set<Gene> geneSet = geneGOMap.get( term );
                if ( geneSet == null ) {
                    // Previously had no genes for this term
                    geneSet = new HashSet<>();
                    geneGOMap.put( term, geneSet );
                }

                geneSet.add( gene );
            }

        }

    }

}
