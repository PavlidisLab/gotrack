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

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.hashkey.HyperUCFKey;
import ubc.pavlab.gotrack.utilities.Jaccard;

import com.google.common.collect.Iterables;

/**
 * Calculates scores which attempt to explore the impact that annotation stability has on the performance of gene set
 * enrichment analyses. The methods of exploration are as follows: completeTermJaccard: Looks at how consistent the
 * enriched GO terms were; previous annotations for each gene set were compared to what would be the 'current' state.
 * topGeneJaccard: Focused on how consistent the genes supporting the top 5 results were across editions.
 * topTermJaccard: Aimed to explore how semantically similar the top 5 results were across editions.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class StabilityAnalysis {

    private static final Logger log = Logger.getLogger( StabilityAnalysis.class );

    private final Map<Edition, SimilarityScore> similarityScores;
    private final Map<GeneOntologyTerm, Map<Edition, StabilityScore>> stabilityScores;
    private final SimilarityCompareMethod similarityCompareMethod;

    /**
     * @param analysis results of enrichment analysis
     * @param TOP_N_JACCARD number of top terms to use for top N series
     * @param scm compare method to be used in the analysis
     * @param cache Cache bean in order to access propagation needed for parent similarity, null to leave out parent
     *        similarity
     */
    public StabilityAnalysis( EnrichmentAnalysis analysis, int TOP_N_JACCARD, SimilarityCompareMethod scm, Cache cache ) {
        this.similarityCompareMethod = scm;
        Map<Edition, SimilarityScore> similarityScores = new LinkedHashMap<>();
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults = analysis.getSignificantResults();

        List<Edition> orderedEditions = new ArrayList<>( enrichmentResults.keySet() );
        Collections.sort( orderedEditions );

        Edition compareEdition = scm.equals( SimilarityCompareMethod.CURRENT ) ? Iterables.getLast( orderedEditions,
                null ) : Iterables.getFirst( orderedEditions, null );

        Set<GeneOntologyTerm> compareTopTerms = analysis.getTopNTerms( TOP_N_JACCARD, compareEdition );

        Set<Gene> compareTopGenes = new HashSet<>();

        for ( GeneOntologyTerm term : compareTopTerms ) {
            compareTopGenes.addAll( analysis.getGeneSet( compareEdition, term ) );
        }
        Set<GeneOntologyTerm> compareTopParents = null;
        if ( cache != null ) {
            compareTopParents = cache.propagate( compareTopTerms, compareEdition );
        }

        for ( Edition testingEdition : orderedEditions ) {

            // Complete Terms
            Double completeTermJaccard = Jaccard.similarity( analysis.getTermsSignificant( testingEdition ),
                    analysis.getTermsSignificant( compareEdition ) );

            // Top Terms
            Set<GeneOntologyTerm> testingTopTerms = analysis.getTopNTerms( TOP_N_JACCARD, testingEdition );

            Double topTermJaccard = Jaccard.similarity( testingTopTerms, compareTopTerms );

            // Top Genes

            Set<Gene> testingTopGenes = new HashSet<>();

            for ( GeneOntologyTerm term : testingTopTerms ) {
                testingTopGenes.addAll( analysis.getGeneSet( testingEdition, term ) );
            }

            Double topGeneJaccard = Jaccard.similarity( testingTopGenes, compareTopGenes );

            // Top Parents
            Set<GeneOntologyTerm> testingTopParents = new HashSet<>();
            Double topParentsJaccard = null;
            if ( cache != null ) {
                testingTopParents = cache.propagate( testingTopTerms, testingEdition );

                topParentsJaccard = Jaccard.similarity( testingTopParents, compareTopParents );
            }

            similarityScores.put(
                    testingEdition,
                    new SimilarityScore( completeTermJaccard, topTermJaccard, topGeneJaccard, topParentsJaccard,
                            Collections.unmodifiableSet( testingTopTerms ), Collections
                                    .unmodifiableSet( testingTopGenes ), Collections
                                    .unmodifiableSet( testingTopParents ) ) );

            if ( scm.equals( SimilarityCompareMethod.PROXIMAL ) ) {
                compareEdition = testingEdition;
                compareTopTerms = testingTopTerms;
                compareTopGenes = testingTopGenes;
                compareTopParents = testingTopParents;
            }

        }

        this.similarityScores = Collections.unmodifiableMap( similarityScores );

        // Compute stability of individual GOTerms over time
        // ***********************************
        // First we compute the change in sample and population successes between editions
        // then compute weighted mean/std of these over previous N (12) editions
        // Compute min/max difference in 95% confidence interval (ex. mean+2*sig = upper limit diff)
        // Apply these difference limits to original data points rounding where necessary giving 4 new data points
        // Compute upper and lower bound p-values using the worst/best case values from the 4 new success numbers
        // ***********************************

        // Log Probability Memoization Cache
        TObjectDoubleHashMap<HyperUCFKey> logProbCache = new TObjectDoubleHashMap<>();
        double noEntryValue = logProbCache.getNoEntryValue();

        Map<GeneOntologyTerm, Map<Edition, StabilityScore>> stabilityScores = new HashMap<>();

        int[] weights = new int[6];
        for ( int i = 0; i < weights.length; i++ ) {
            weights[i] = i + 1;
        }
        for ( GeneOntologyTerm t : analysis.getTermsSignificantInAnyEdition() ) {

            Map<Edition, EnrichmentResult> data = analysis.getResults( t );
            Map<Edition, StabilityScore> scores = new HashMap<>();

            LinkedHashMap<Edition, int[]> queue = new LinkedHashMap<Edition, int[]>() {
                @Override
                protected boolean removeEldestEntry( Map.Entry<Edition, int[]> eldest ) {
                    return this.size() > 6;
                }
            };
            EnrichmentResult previousResult = null;
            double runningScore = 0;
            int runningScoreCnt = 0;
            for ( Edition ed : orderedEditions ) {
                EnrichmentResult er = data.get( ed );
                if ( er != null ) {
                    if ( previousResult != null ) {
                        int[] arr = new int[2];
                        arr[0] = er.getSampleAnnotated() - previousResult.getSampleAnnotated();
                        arr[1] = er.getPopulationAnnotated() - previousResult.getPopulationAnnotated();
                        queue.put( ed, arr );
                        // int r, int m, int k, int t
                        double[] a = weightedAverage( queue, weights );
                        double[] b = weightedStd( queue, weights );
                        // StabilityScore s = new StabilityScore( er.getSampleAnnotated(), er.getPopulationAnnotated(),
                        // a[0], a[1], b[0], b[1] );
                        // scores.put( ed, s );

                        // now to calculate upper and lower bound p values

                        // 95% confidence
                        int rMin = ( int ) Math.round( er.getSampleAnnotated() + a[0] - 2 * b[0] );
                        int rMax = ( int ) Math.round( er.getSampleAnnotated() + a[0] + 2 * b[0] );

                        int mMin = ( int ) Math.round( er.getPopulationAnnotated() + a[1] - 2 * b[1] );
                        int mMax = ( int ) Math.round( er.getPopulationAnnotated() + a[1] + 2 * b[1] );

                        // Makes sure new values make physical sense
                        int[] params = new int[4]; // smallest p-value
                        params[0] = rMax; // most hits in sample
                        params[1] = mMin; // least hits in population
                        params[2] = er.getSampleSize();
                        params[3] = er.getPopulationSize();
                        fixParameters( params );

                        HyperUCFKey key = new HyperUCFKey( params[0], params[1], params[2], params[3] );
                        double minp = logProbCache.get( key );
                        if ( minp == noEntryValue ) {
                            minp = EnrichmentAnalysis.upperCumulativeProbabilityLogMethod( params[0], params[1],
                                    params[2], params[3] );
                            logProbCache.put( key, minp );
                        }

                        params[0] = rMin; // least hits in sample
                        params[1] = mMax; // most hits in population
                        params[2] = er.getSampleSize();
                        params[3] = er.getPopulationSize();
                        fixParameters( params );

                        key = new HyperUCFKey( params[0], params[1], params[2], params[3] );
                        double maxp = logProbCache.get( key );
                        if ( maxp == noEntryValue ) {
                            maxp = EnrichmentAnalysis.upperCumulativeProbabilityLogMethod( params[0], params[1],
                                    params[2], params[3] );
                            logProbCache.put( key, maxp );
                        }

                        double score = ( maxp - minp ) / er.getPvalue();
                        runningScore += score;
                        runningScoreCnt++;

                        scores.put(
                                ed,
                                new StabilityScore( b[0], b[1], minp, maxp, Math.log( score ), Math.log( runningScore
                                        / runningScoreCnt ) ) );

                        if ( er.getPvalue() > maxp || er.getPvalue() < minp ) {
                            log.debug( t.getGoId() );
                            log.debug( ed.getEdition() );
                            log.debug( er );
                            log.debug( Arrays.toString( a ) );
                            log.debug( Arrays.toString( b ) );
                            log.debug( Arrays.toString( new int[] { rMin, rMax, mMin, mMax } ) );

                        }

                    } else {
                        scores.put( ed, new StabilityScore( -1, -1, er.getPvalue(), er.getPvalue(), Double.NaN,
                                Double.NaN ) );
                    }
                    previousResult = er;
                }

            }

            stabilityScores.put( t, Collections.unmodifiableMap( scores ) );

        }

        this.stabilityScores = Collections.unmodifiableMap( stabilityScores );

    }

    /**
     * @param r sampleAnnotated
     * @param m populationAnnotated
     * @param k sampleSize
     * @param t populationSize (N+M)
     * @return if, after previously having changed r and/or m, the numbers make physical sense
     */
    private static boolean fixParameters( int[] arr ) {
        // int r, int m, int k, int t
        int r = arr[0];
        int m = arr[1];
        int k = arr[2];
        int t = arr[3];
        boolean fixed = false;
        if ( m > t ) {
            m = t;
            fixed = true;
        }
        if ( r > k ) {
            r = k;
            fixed = true;
        }
        if ( r > m ) {
            m = r;
            fixed = true;
        }
        if ( k > t - m && r < k - ( t - m ) ) {
            r = k - ( t - m );
            fixed = true;
        } else if ( r < 0 ) {
            r = 0;
            fixed = true;
        }
        arr[0] = r;
        arr[1] = m;
        arr[2] = k;
        arr[3] = t;
        return fixed;
    }

    private static double[] weightedAverage( LinkedHashMap<Edition, int[]> queue, int[] weights ) {
        if ( queue.size() > weights.length ) throw new RuntimeException( "Not enough weights for queue" );
        // int r, int m, int k, int t

        int size = queue.values().iterator().next().length;

        int sumWeights = 0;
        for ( int i : weights )
            sumWeights += i;

        int i = 0;
        double[] rollingSums = new double[size];

        for ( int[] vals : queue.values() ) {
            int weight = weights[i++];
            for ( int j = 0; j < size; j++ ) {
                int v = vals[j];
                rollingSums[j] += weight * v / ( double ) sumWeights;
            }
        }

        return rollingSums;

    }

    private static double[] weightedStd( LinkedHashMap<Edition, int[]> queue, int[] weights ) {
        if ( queue.size() == 0 ) {
            return null;
        }

        int sumWeight = 0;

        int size = queue.values().iterator().next().length;

        double[] stds = new double[size];

        if ( queue.size() == 1 ) {
            for ( int j = 0; j < size; j++ ) {
                stds[j] = 0;
            }
            return stds;
        }

        double[] means = new double[size];
        double[] M2s = new double[size];

        int i = 0;
        for ( int[] vals : queue.values() ) {
            int weight = weights[i++];
            int temp = weight + sumWeight;

            for ( int j = 0; j < size; j++ ) {
                int v = vals[j];
                double delta = v - means[j];
                double R = weight * delta / temp;
                means[j] += R;
                M2s[j] += sumWeight * delta * R;
            }

            sumWeight = temp;
        }

        for ( int j = 0; j < size; j++ ) {
            double M2 = M2s[j];

            double variance_n = M2 / sumWeight;
            double variance = variance_n * queue.size() / ( queue.size() - 1 );
            stds[j] = Math.sqrt( variance );
        }

        return stds;

    }

    public Map<Edition, SimilarityScore> getSimilarityScores() {
        return similarityScores;
    }

    public SimilarityScore getSimilarityScores( Edition ed ) {
        if ( ed == null ) return null;
        return similarityScores.get( ed );
    }

    public Map<GeneOntologyTerm, Map<Edition, StabilityScore>> getStabilityScores() {
        return stabilityScores;
    }

    public Map<Edition, StabilityScore> getStabilityScores( GeneOntologyTerm t ) {
        if ( t == null ) return null;
        return stabilityScores.get( t );
    }

    public StabilityScore getStabilityScores( GeneOntologyTerm t, Edition ed ) {
        if ( t == null || ed == null ) return null;
        Map<Edition, StabilityScore> m = stabilityScores.get( t );
        if ( m != null ) {
            return m.get( ed );
        }

        return null;

    }

    public SimilarityCompareMethod getSimilarityCompareMethod() {
        return similarityCompareMethod;
    }

}
