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

package ubc.pavlab.gotrack.analysis;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.hashkey.HyperUCFKey;

import java.util.*;
import java.util.Map.Entry;

/**
 * Runs enrichment analysis over all editions given geneset data and term population data.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentAnalysis {

    private static final Logger log = Logger.getLogger( EnrichmentAnalysis.class );

    private final Species currentSpecies;
    private final int minAnnotatedPopulation;
    private final int maxAnnotatedPopulation;

    private final int totalEditions;
    private final int totalGenes;
    private final int totalTerms;
    private final int totalResults;

    private final double threshold;

    // Holds those unmodifiable results which met the population limits and had population data present in cache
    private final Map<Edition, Enrichment<GeneOntologyTerm, Gene>> rawResults;

    // Holds those unmodifiable terms which are significant in any edition (used to create useful graphing views)
    private Set<GeneOntologyTerm> termsSignificantInAnyEdition = new HashSet<>();

    // ********************
    // Log Probability Memoization Cache
    private TObjectDoubleHashMap<HyperUCFKey> logProbCache = new TObjectDoubleHashMap<>();

    // ********************

    /**
     * Runs analysis with given options and data. All relevant information is set here as there are no plans to make an
     * EnrichmentAnalysis re-runnable. If you must run with different settings, simply create a new instance (though
     * keep in mind that these instances are very memory intensive).
     * 
     * @param geneGOMap Map containing raw data from db
     * @param min minimum geneset size a specific term must have to be included in results
     * @param max maximum geneset size a specific term must have to be included in results
     * @param test Which method for multiple test correction
     * @param threshold Either p-value cutoff if using Bonferroni or FDR level if using BH step-up
     * @param cache Cache object containing part of the contingency table
     * @param currentSpecies Used to get correct data from cache
     */
    public EnrichmentAnalysis( Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap, int min, int max,
                               MultipleTestCorrection test, double threshold,
                               Cache cache, Species currentSpecies ) {

        /*
         * This makes use of memoization to reduce computations of log probabilities. This is necessary from the sheer
         * magnitude that we will be computing.
         */

        // Store options
        this.minAnnotatedPopulation = min;
        this.maxAnnotatedPopulation = max == 0 ? Integer.MAX_VALUE : max;
        this.currentSpecies = currentSpecies;

        // Store some stats
        this.totalEditions = geneGOMap.keySet().size();
        Set<Gene> totalGenes = new HashSet<>();
        Set<GeneOntologyTerm> totalTerms = new HashSet<>();
        int totalResults = 0;

        //  Containers for results

        Map<Edition, Enrichment<GeneOntologyTerm, Gene>> results = Maps.newHashMap();
        Set<GeneOntologyTerm> termsSignificantInAnyEdition = new HashSet<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Gene>>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, Set<Gene>> data = editionEntry.getValue();

            StandardCompletePopulation<GeneOntologyTerm, Gene> sample = CompletePopulation.standardCompletePopulation( data );

            Population<GeneOntologyTerm> population = Population.cachedGOPopulation( cache, ed );

            Enrichment<GeneOntologyTerm, Gene> enrichment = new Enrichment<>( test, threshold, minAnnotatedPopulation,
                    maxAnnotatedPopulation );

            enrichment.runAnalysis( sample, population, logProbCache );

            results.put( ed, enrichment );

            termsSignificantInAnyEdition.addAll( enrichment.getSignificantTerms() );

            // Stats

            totalGenes.addAll( sample.getEntities() );
            totalTerms.addAll( data.keySet() );
            totalResults += enrichment.getCalculations();

        }

        // Store results

        this.rawResults = Collections.unmodifiableMap( results );

        this.termsSignificantInAnyEdition = Collections.unmodifiableSet( termsSignificantInAnyEdition );

        this.totalGenes = totalGenes.size();
        this.totalTerms = totalTerms.size();
        this.totalResults = totalResults;

        this.threshold = threshold;

    }

    /**
     * @return threshold set for either Bonferroni cutoff or FDR level
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * @return cutoff p-values for an edition
     */
    public Double getCutoff( Edition ed ) {
        if ( ed == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }
        return e.getCutoff();
    }

    public Set<Edition> getEditions() {
        return Collections.unmodifiableSet( rawResults.keySet() );
    }

    /**
     * @param ed
     * @return unmodifiable map containing only significant results for specific edition
     */
    public Map<GeneOntologyTerm, EnrichmentResult> getSignificantResults( Edition ed ) {
        if ( ed == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }
        return Collections.unmodifiableMap( e.getSignificantResults() );

    }

    /**
     * @return unmodifiable map containing results significant in any edition
     */
    public Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> getResults() {
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> significantInAnyEdition = new HashMap<>();

        for ( Entry<Edition, Enrichment<GeneOntologyTerm, Gene>> editionEntry : rawResults.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Enrichment<GeneOntologyTerm, Gene> enrichment = editionEntry.getValue();

            significantInAnyEdition.put( ed, Collections
                    .unmodifiableMap( Maps.filterKeys( enrichment.getResults(),
                            Predicates.in( termsSignificantInAnyEdition ) ) ) );

        }

        return Collections.unmodifiableMap( significantInAnyEdition );

    }

    /**
     * @param ed
     * @return unmodifiable map containing results significant in any edition for a specific edition
     */
    public Map<GeneOntologyTerm, EnrichmentResult> getResults( Edition ed ) {
        if ( ed == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }

        return Collections.unmodifiableMap(
                Maps.filterKeys( e.getResults(), Predicates.in( termsSignificantInAnyEdition ) ) );

    }

    /**
     * @param ed
     * @param specificTerm
     * @return Enrichment Result for specific edition and term else null if doesn't exist
     */
    public EnrichmentResult getResult( Edition ed, GeneOntologyTerm specificTerm ) {
        if ( ed == null || specificTerm == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }
        return e.getResult( specificTerm );

    }

    /**
     * @return species this analysis was done on.
     */
    public Species getCurrentSpecies() {
        return currentSpecies;
    }

    /**
     * @return Minimum gene set size a term had to have to not be rejected
     */
    public int getMinAnnotatedPopulation() {
        return minAnnotatedPopulation;
    }

    /**
     * @return Maximum gene set size a term had to have to not be rejected
     */
    public int getMaxAnnotatedPopulation() {
        return maxAnnotatedPopulation;
    }

    /**
     * @return raw enrichment results, both significant and not, which passed all rejection tests.
     */
    public Map<Edition, Enrichment<GeneOntologyTerm, Gene>> getRawResults() {
        return rawResults;
    }

    //    /**
    //     * @return Terms which were not mapped to anything in Cache
    //     */
    //    public Map<Edition, Set<GeneOntologyTerm>> getUnmappedTerms() {
    //        return unmappedTerms;
    //    }

    /**
     * @return Terms which were rejected before significance tests in an edition
     */
    public Set<GeneOntologyTerm> getRejectedTerms( Edition ed ) {
        if ( ed == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }
        return Collections.unmodifiableSet( e.getRejectedTerms() );
    }

    /**
     * @return Set of terms that were significant in at least one edition
     */
    public Set<GeneOntologyTerm> getTermsSignificantInAnyEdition() {
        return termsSignificantInAnyEdition;
    }

    //    /**
    //     * @return Terms significant for each edition
    //     */
    //    public Map<Edition, Set<GeneOntologyTerm>> getTermsSignificant() {
    //        return termsSignificant;
    //    }

    /**
     * @param ed Edition
     * @return Significant terms in a specific edition
     */
    public Set<GeneOntologyTerm> getTermsSignificant( Edition ed ) {
        if ( ed == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }
        return e.getSignificantTerms();
    }

    /**
     * @param ed Edition
     * @param term Term
     * @return Genes which were annotated to a specific term in a specific edition in this analysis.
     */
    public Set<Gene> getGeneSet( Edition ed, GeneOntologyTerm term ) {
        Enrichment<GeneOntologyTerm, Gene> enrichment = rawResults.get( ed );
        if (enrichment == null) {
            return null;
        }

        return enrichment.getSamplePopulation().getEntities( term );
    }

    /**
     * @param n
     * @return Top N terms from each edition
     */
    public Set<GeneOntologyTerm> getTopNTerms( int n ) {
        HashSet<GeneOntologyTerm> top = new HashSet<>();
        for ( Enrichment<GeneOntologyTerm, Gene> e : rawResults.values() ) {
            top.addAll( e.getTopNTerms( n ) );
        }
        return top;
    }

    /**
     * @param n
     * @param ed Edition
     * @return Top N terms from specific edition
     */
    public Set<GeneOntologyTerm> getTopNTerms( int n, Edition ed ) {
        if ( ed == null ) {
            return null;
        }
        Enrichment<GeneOntologyTerm, Gene> e = rawResults.get( ed );
        if ( e == null ) {
            return null;
        }

        return e.getTopNTerms( n );
    }

    /**
     * Keep in mind this is not the most efficient method if iterating all terms
     * 
     * @param t
     * @return Results for this term in each edition it showed up.
     */
    public Map<Edition, EnrichmentResult> getResults( GeneOntologyTerm t ) {
        if ( t == null ) return null;
        Map<Edition, EnrichmentResult> data = new HashMap<>();
        for ( Entry<Edition, Enrichment<GeneOntologyTerm, Gene>> editionEntry : rawResults.entrySet() ) {
            Edition ed = editionEntry.getKey();
            EnrichmentResult er = editionEntry.getValue().getResult( t );
            if ( er != null ) {
                data.put( ed, er );
            }
        }

        return Collections.unmodifiableMap( data );
    }

    /**
     * @return Number of edition analyzed
     */
    public int getTotalEditions() {
        return totalEditions;
    }

    /**
     * @return Number of genes analyzed
     */
    public int getTotalGenes() {
        return totalGenes;
    }

    /**
     * @return Number of terms analyzed
     */
    public int getTotalTerms() {
        return totalTerms;
    }

    /**
     * @return Number of results computed (log-probabilities)
     */
    public int getTotalResults() {
        return totalResults;
    }

}
