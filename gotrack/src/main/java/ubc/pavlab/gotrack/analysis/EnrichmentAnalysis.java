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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.hashkey.HyperUCFKey;

/**
 * Runs enrichment analysis over all editions given geneset data and term population data.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentAnalysis {

    private static final Logger log = Logger.getLogger( EnrichmentAnalysis.class );

    private final int currentSpeciesId;
    private final int minAnnotatedPopulation;
    private final int maxAnnotatedPopulation;
    private final int totalEditions;
    private final int totalGenes;
    private final int totalTerms;
    private final int totalResults;
    private final Map<Edition, Double> cutoffs;

    private final double threshold;

    private final Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> rawData;

    // Holds those unmodifiable results which met the population limits and had population data present in cache
    private final Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> rawResults;
    // Holds those unmodifiable terms which met the population limits but did not have population data present in cache
    private final Map<Edition, Set<GeneOntologyTerm>> unmappedTerms;
    // Holds those unmodifiable terms which did not meet the population limits
    private final Map<Edition, Set<GeneOntologyTerm>> rejectedTerms;

    // Holds those unmodifiable terms which are significant in any edition (used to create useful graphing views)
    private Set<GeneOntologyTerm> termsSignificantInAnyEdition = new HashSet<>();
    // Holds those unmodifiable terms which are significant in its own edition
    private Map<Edition, Set<GeneOntologyTerm>> termsSignificant = new HashMap<>();

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
     * @param sampleSizes Map containing sample size data from db
     * @param min minimum geneset size a specific term must have to be included in results
     * @param max maximum geneset size a specific term must have to be included in results
     * @param test Which method for multiple test correction
     * @param threshold Either p-value cutoff if using Bonferroni or FDR level if using BH step-up
     * @param cache Cache object containing part of the contingency table
     * @param currentSpeciesId Used to get correct data from cache
     */
    public EnrichmentAnalysis( Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap,
            Map<Edition, Integer> sampleSizes, int min, int max, MultipleTestCorrection test, double threshold,
            Cache cache, int currentSpeciesId ) {

        /*
         * This makes use of memoization to reduce computations of log probabilities. This is necessary from the sheer
         * magnitude that we will be computing.
         */

        // Used to test for no entry in memoization cache
        double noEntryValue = logProbCache.getNoEntryValue();

        // Store raw data used in this analysis
        this.rawData = geneGOMap;

        // Store options
        this.minAnnotatedPopulation = min;
        this.maxAnnotatedPopulation = max == 0 ? Integer.MAX_VALUE : max;
        this.currentSpeciesId = currentSpeciesId;

        // Store some stats
        this.totalEditions = geneGOMap.keySet().size();
        Set<Gene> totalGenes = new HashSet<>();
        Set<GeneOntologyTerm> totalTerms = new HashSet<>();
        int totalResults = 0;

        //  Containers for results
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> rawResults = new HashMap<>();
        Map<Edition, Set<GeneOntologyTerm>> unmappedTerms = new HashMap<>();
        Map<Edition, Set<GeneOntologyTerm>> rejectedTerms = new HashMap<>();
        Set<GeneOntologyTerm> termsSignificantInAnyEdition = new HashSet<>();
        Map<Edition, Set<GeneOntologyTerm>> termsSignificant = new HashMap<>();
        Map<Edition, Double> cutoffs = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Gene>>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, Set<Gene>> data = editionEntry.getValue();

            Map<GeneOntologyTerm, EnrichmentResult> resultsInEdition = new HashMap<>();

            Set<GeneOntologyTerm> um = new HashSet<>(); // Unmapped terms
            Set<GeneOntologyTerm> rj = new HashSet<>(); // Rejected Terms
            Set<GeneOntologyTerm> sig = new HashSet<>(); // Significant Terms

            Integer populationSize = cache.getGeneCount( currentSpeciesId, ed );
            Integer sampleSize = sampleSizes.get( ed );

            if ( populationSize == null || sampleSize == null ) {
                log.warn( "Could not find aggregate data for edition (" + ed + "), skipping..." );
                continue;
            }

            /*
             * pre-loop to remove terms that don't fit population limits, to get test set size for bonferroni and to get
             * ranks
             */
            for ( Entry<GeneOntologyTerm, Set<Gene>> termEntry : data.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                Integer populationAnnotated = cache.getInferredAnnotationCount( currentSpeciesId, ed, term );

                if ( populationAnnotated != null && populationAnnotated >= minAnnotatedPopulation
                        && populationAnnotated <= maxAnnotatedPopulation ) { // Rejection test
                    Integer sampleAnnotated = termEntry.getValue().size();

                    // Combined map key
                    HyperUCFKey key = new HyperUCFKey( sampleAnnotated, populationAnnotated, sampleSize,
                            populationSize );

                    // Get log probability from memoization cache or compute it
                    double p = logProbCache.get( key );
                    if ( p == noEntryValue ) {
                        p = upperCumulativeProbabilityLogMethod( sampleAnnotated, populationAnnotated, sampleSize,
                                populationSize );
                        // Cache result
                        logProbCache.put( key, p );
                    }

                    // Add result to all results in this edition
                    resultsInEdition.put( term, new EnrichmentResult( p, sampleAnnotated, populationAnnotated,
                            sampleSize, populationSize ) );

                    // These are used for stats collection
                    totalResults++;
                    totalGenes.addAll( termEntry.getValue() );
                    totalTerms.add( term );

                } else if ( populationAnnotated == null ) {
                    um.add( term );

                } else if ( populationAnnotated < minAnnotatedPopulation
                        || populationAnnotated > maxAnnotatedPopulation ) {
                    rj.add( term );
                } else {
                    log.warn( "Something went wrong with analysis term (" + term + ") with population: "
                            + populationAnnotated );
                }

            }

            /*
             * Create EnrichmentResults
             */

            // Number of statistical tests done in analysis (within edition)
            int testSetSize = resultsInEdition.keySet().size();

            // Holds terms sorted by p-value
            LinkedHashSet<GeneOntologyTerm> termsSortedByRank = EnrichmentAnalysis
                    .getSortedKeySetByValue( resultsInEdition );

            // We keep track of the cutoff p-value in each edition, set to some useful initial value
            if ( test.equals( MultipleTestCorrection.BH ) ) {
                // set lowest pvalue as cutoff for now;
                cutoffs.put( ed, resultsInEdition.get( termsSortedByRank.iterator().next() ).getPvalue() );
            } else {
                cutoffs.put( ed, threshold );
            }

            // Containers
            int rank = -1;
            int k = 0;
            EnrichmentResult previousResult = null;
            Set<GeneOntologyTerm> maybePile = new HashSet<>();
            Map<Integer, List<GeneOntologyTerm>> standardRanks = new HashMap<>();

            /*
             * Loops through each term in order of p-value and compute rank, significance based on chosen method
             */
            for ( GeneOntologyTerm term : termsSortedByRank ) {
                k++;
                EnrichmentResult er = resultsInEdition.get( term );
                if ( test.equals( MultipleTestCorrection.BONFERRONI ) ) {
                    er.setPvalue( Math.min( er.getPvalue() * testSetSize, 1 ) ); // Apply bonferroni correction
                    if ( er.getPvalue() <= threshold ) { // Check against threshold
                        termsSignificantInAnyEdition.add( term );
                        sig.add( term );
                    }

                } else if ( test.equals( MultipleTestCorrection.BH ) ) {
                    // Single pass method of BH step-up
                    double qTresh = ( k ) * threshold / testSetSize;
                    if ( er.getPvalue() <= qTresh ) {
                        // add this term and all terms in maybe pile
                        cutoffs.put( ed, er.getPvalue() ); //update cutoff
                        termsSignificantInAnyEdition.add( term );
                        sig.add( term );
                        if ( !maybePile.isEmpty() ) {
                            termsSignificantInAnyEdition.addAll( maybePile );
                            sig.addAll( maybePile );
                            maybePile.clear();
                        }
                    } else {
                        // add this term to the maybe pile
                        maybePile.add( term );
                    }
                } else { // Shouldn't get here
                    termsSignificantInAnyEdition.add( term );
                    sig.add( term );
                }

                // ranks

                if ( !er.equals( previousResult ) ) {
                    rank = k - 1;
                }
                List<GeneOntologyTerm> termSet = standardRanks.get( rank );
                if ( termSet == null ) {
                    termSet = new ArrayList<>();
                    standardRanks.put( rank, termSet );
                }
                termSet.add( term );
                er.setRank( rank );
                previousResult = er;
            }

            // log.debug( "Results " + ed + " size: " + resultsInEdition.size() );
            // log.debug( "Significant " + ed + " size: " + sig.size() );
            // log.debug( "unmappedTerms " + ed + " size: " + um.size() );
            // log.debug( "rejectedTerms " + ed + " size: " + rj.size() );

            // Compute fractional Ranks

            for ( Entry<Integer, List<GeneOntologyTerm>> rankEntry : standardRanks.entrySet() ) {
                int standardRank = rankEntry.getKey();
                List<GeneOntologyTerm> termSet = rankEntry.getValue();
                double newRank = standardRank + ( termSet.size() - 1 ) / 2.0;
                for ( GeneOntologyTerm term : termSet ) {
                    EnrichmentResult er = resultsInEdition.get( term );
                    er.setFractionalRank( newRank );
                }
            }

            rawResults.put( ed, Collections.unmodifiableMap( resultsInEdition ) );
            unmappedTerms.put( ed, Collections.unmodifiableSet( um ) );
            rejectedTerms.put( ed, Collections.unmodifiableSet( rj ) );
            termsSignificant.put( ed, Collections.unmodifiableSet( sig ) );

        }

        // Store results

        this.rawResults = Collections.unmodifiableMap( rawResults );
        this.unmappedTerms = Collections.unmodifiableMap( unmappedTerms );
        this.rejectedTerms = Collections.unmodifiableMap( rejectedTerms );
        this.termsSignificantInAnyEdition = Collections.unmodifiableSet( termsSignificantInAnyEdition );
        this.termsSignificant = Collections.unmodifiableMap( termsSignificant );
        this.cutoffs = Collections.unmodifiableMap( cutoffs );
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
     * @return cutoff p-values in each edition
     */
    public Map<Edition, Double> getCutoffs() {
        return cutoffs;
    }

    /**
     * @return unmodifiable map containing only significant results for each edition
     */
    public Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> getSignificantResults() {
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> significantResultsOnly = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : rawResults.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, EnrichmentResult> data = editionEntry.getValue();
            Map<GeneOntologyTerm, EnrichmentResult> significantData = new HashMap<>();

            Set<GeneOntologyTerm> sig = termsSignificant.get( ed );
            if ( sig != null ) {
                for ( GeneOntologyTerm term : sig ) {
                    EnrichmentResult er = data.get( term );
                    if ( er != null ) {
                        significantData.put( term, er );
                    }
                }
            }
            significantResultsOnly.put( ed, Collections.unmodifiableMap( significantData ) );
        }

        return Collections.unmodifiableMap( significantResultsOnly );

    }

    /**
     * @param specificEdition
     * @return unmodifiable map containing only significant results for specific edition
     */
    public Map<GeneOntologyTerm, EnrichmentResult> getSignificantResults( Edition specificEdition ) {
        Map<GeneOntologyTerm, EnrichmentResult> data = rawResults.get( specificEdition );
        Map<GeneOntologyTerm, EnrichmentResult> significantData = new HashMap<>();

        Set<GeneOntologyTerm> sig = termsSignificant.get( specificEdition );
        if ( sig != null ) {
            for ( GeneOntologyTerm term : sig ) {
                EnrichmentResult er = data.get( term );
                if ( er != null ) {
                    significantData.put( term, er );
                }
            }
        }

        return Collections.unmodifiableMap( significantData );

    }

    /**
     * @return unmodifiable map containing results significant in any edition
     */
    public Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> getResults() {
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> significantInAnyEdition = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : rawResults.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, EnrichmentResult> data = editionEntry.getValue();
            Map<GeneOntologyTerm, EnrichmentResult> filteredData = new HashMap<>();

            for ( GeneOntologyTerm term : termsSignificantInAnyEdition ) {
                EnrichmentResult er = data.get( term );
                if ( er != null ) {
                    filteredData.put( term, er );
                }
            }
            significantInAnyEdition.put( ed, Collections.unmodifiableMap( filteredData ) );
        }

        return Collections.unmodifiableMap( significantInAnyEdition );

    }

    /**
     * @param specificEdition
     * @return unmodifiable map containing results significant in any edition for a specific edition
     */
    public Map<GeneOntologyTerm, EnrichmentResult> getResults( Edition specificEdition ) {
        Map<GeneOntologyTerm, EnrichmentResult> data = rawResults.get( specificEdition );
        Map<GeneOntologyTerm, EnrichmentResult> filteredData = new HashMap<>();

        for ( GeneOntologyTerm term : termsSignificantInAnyEdition ) {
            EnrichmentResult er = data.get( term );
            if ( er != null ) {
                filteredData.put( term, er );
            }
        }

        return Collections.unmodifiableMap( filteredData );

    }

    /**
     * @param specificEdition
     * @param specificTerm
     * @return Enrichment Result for specific edition and term else null if doesn't exist
     */
    public EnrichmentResult getResult( Edition specificEdition, GeneOntologyTerm specificTerm ) {

        Map<GeneOntologyTerm, EnrichmentResult> editionData = rawResults.get( specificEdition );
        if ( editionData != null ) {
            return editionData.get( specificTerm );
        }

        return null;

    }

    /**
     * @return species this analysis was done on.
     */
    public int getCurrentSpeciesId() {
        return currentSpeciesId;
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
     * @return raw enrichment results both significant and not which passed all rejection tests.
     */
    public Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> getRawResults() {
        return rawResults;
    }

    /**
     * @return Terms which were not mapped to anything in Cache
     */
    public Map<Edition, Set<GeneOntologyTerm>> getUnmappedTerms() {
        return unmappedTerms;
    }

    /**
     * @return Terms which were rejected before significance tests
     */
    public Map<Edition, Set<GeneOntologyTerm>> getRejectedTerms() {
        return rejectedTerms;
    }

    /**
     * @return Set of terms that were significant in at least one edition
     */
    public Set<GeneOntologyTerm> getTermsSignificantInAnyEdition() {
        return termsSignificantInAnyEdition;
    }

    /**
     * @return Terms significant for each edition
     */
    public Map<Edition, Set<GeneOntologyTerm>> getTermsSignificant() {
        return termsSignificant;
    }

    /**
     * @param ed Edition
     * @return Significant terms in a specific edition
     */
    public Set<GeneOntologyTerm> getTermsSignificant( Edition ed ) {
        return termsSignificant.get( ed );
    }

    /**
     * @param ed Edition
     * @param term Term
     * @return Genes which were annotated to a specific term in a specific edition in this analysis.
     */
    public Set<Gene> getGeneSet( Edition ed, GeneOntologyTerm term ) {
        Map<GeneOntologyTerm, Set<Gene>> m1 = rawData.get( ed );
        if ( m1 != null ) {
            return Collections.unmodifiableSet( m1.get( term ) );
        }

        return null;

    }

    /**
     * @param n
     * @return Top N terms from each edition
     */
    public Set<GeneOntologyTerm> getTopNTerms( int n ) {
        HashSet<GeneOntologyTerm> top = new HashSet<>();
        for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : termsSignificant.entrySet() ) {
            Edition ed = editionEntry.getKey();
            for ( GeneOntologyTerm term : editionEntry.getValue() ) {
                EnrichmentResult er = getResult( ed, term );
                if ( er.getRank() < n ) {
                    top.add( term );
                }
            }

        }
        return top;
    }

    /**
     * @param n
     * @param ed Edition
     * @return Top N terms from specific edition
     */
    public Set<GeneOntologyTerm> getTopNTerms( int n, Edition ed ) {
        HashSet<GeneOntologyTerm> top = new HashSet<>();
        Set<GeneOntologyTerm> data = termsSignificant.get( ed );
        for ( GeneOntologyTerm term : data ) {
            EnrichmentResult er = getResult( ed, term );
            if ( er.getRank() < n ) {
                top.add( term );
            }
        }
        return top;
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
        for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : rawResults.entrySet() ) {
            Edition ed = editionEntry.getKey();
            EnrichmentResult er = editionEntry.getValue().get( t );
            if ( t != null ) {
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

    /**
     * Optimized method of calculating probability tails, relies on the fact that given a probability from a
     * hypergeometric distribution with given M,N,k at r : h_M,N,k(r) we can then find h_M,N,k(r+1) without calculating
     * the 9 factorials which are usually required. h_M,N,k(r+1) = h_M,N,k(r) * (k-r) * (M-r) / [ (r+1) * (N-k+r+1) ]
     * proof can be demonstrated relatively easily.
     * 
     * @param r sampleAnnotated
     * @param m populationAnnotated
     * @param k sampleSize
     * @param t populationSize (N+M)
     * @return upper cumulative probability at r given m,k,t
     */
    @Deprecated
    public static double upperCumulativeProbability( int r, int m, int k, int t ) {
        double h_r = sampleProbability( r, m, k, t );
        double pvalue = h_r;
        int min = Math.min( k, m );
        for ( int r_ = r + 1; r_ <= min; r_++ ) {
            h_r = h_r * ( k - r_ + 1 ) * ( m - r_ + 1 ) / ( ( r_ ) * ( t - m - k + r_ ) );

            pvalue += h_r;
        }
        return pvalue;
    }

    /**
     * Optimized method of calculating probability tails, relies on the fact that given a probability from a
     * hypergeometric distribution with given M,N,k at r : h_M,N,k(r) we can then find h_M,N,k(r+1) without calculating
     * the 9 factorials which are usually required. h_M,N,k(r+1) = h_M,N,k(r) * (k-r) * (M-r) / [ (r+1) * (N-k+r+1) ]
     * proof can be demonstrated relatively easily. Utilizes log probabilities to be able to handle large integer inputs
     * > 1000~
     * 
     * @param r sampleAnnotated
     * @param m populationAnnotated
     * @param k sampleSize
     * @param t populationSize (N+M)
     * @return upper cumulative probability at r given m,k,t
     */
    public static double upperCumulativeProbabilityLogMethod( int r, int m, int k, int t ) {
        double h_r_log = sampleProbabilityLog( r, m, k, t );
        double pvalue = Math.exp( h_r_log );
        int min = Math.min( k, m );
        for ( int r_ = r + 1; r_ <= min; r_++ ) {
            h_r_log = h_r_log
                    + FastMath.log( ( k - r_ + 1 ) * ( m - r_ + 1 ) / ( ( double ) ( r_ ) * ( t - m - k + r_ ) ) );

            pvalue += FastMath.exp( h_r_log );
        }
        return pvalue;
    }

    @Deprecated
    public static double sampleProbability( int r, int m, int k, int t ) {
        // if ( r > m ) return 0d;
        return CombinatoricsUtils.binomialCoefficientDouble( m, r )
                / CombinatoricsUtils.binomialCoefficientDouble( t, k )
                * CombinatoricsUtils.binomialCoefficientDouble( t - m, k - r );
    }

    public static double sampleProbabilityLog( int r, int m, int k, int t ) {
        return ( CombinatoricsUtils.binomialCoefficientLog( m, r ) - CombinatoricsUtils.binomialCoefficientLog( t, k ) )
                + CombinatoricsUtils.binomialCoefficientLog( t - m, k - r );
    }

    public static LinkedHashSet<GeneOntologyTerm> getSortedKeySetByValue(
            Map<GeneOntologyTerm, EnrichmentResult> data ) {
        LinkedHashSet<GeneOntologyTerm> results = new LinkedHashSet<>();

        List<Entry<GeneOntologyTerm, EnrichmentResult>> entryList = new ArrayList<>( data.entrySet() );

        Collections.sort( entryList, new Comparator<Entry<GeneOntologyTerm, EnrichmentResult>>() {
            @Override
            public int compare( Entry<GeneOntologyTerm, EnrichmentResult> e1,
                    Entry<GeneOntologyTerm, EnrichmentResult> e2 ) {
                return Double.compare( e1.getValue().getPvalue(), e2.getValue().getPvalue() );
            }
        } );

        for ( Entry<GeneOntologyTerm, EnrichmentResult> entry : entryList ) {
            results.add( entry.getKey() );
        }

        return results;

    }

}
