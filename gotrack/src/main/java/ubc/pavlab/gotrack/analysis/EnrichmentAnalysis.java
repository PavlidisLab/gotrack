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

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.EnrichmentResult;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * Runs enrichment analysis over all editions given geneset data and term population data
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentAnalysis {

    private static final Logger log = Logger.getLogger( EnrichmentAnalysis.class );

    private final int currentSpeciesId;
    private final int minAnnotatedPopulation;
    private final int maxAnnotatedPopulation;

    private Double threshold = null;

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

    /**
     * @param geneGOMap Map containing raw data from db
     * @param sampleSizes Map containing sample size data from db
     * @param min minimum geneset size a specific term must have to be included in results
     * @param max maximum geneset size a specific term must have to be included in results
     * @param bonferroniCorrection true to apply Bonferroni multiple tests correction else false
     * @param cache Cache object containing contingency table results (partly)
     * @param currentSpeciesId used to get correct data from cache
     */
    public EnrichmentAnalysis( Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap,
            Map<Edition, Integer> sampleSizes, int min, int max, MultipleTestCorrection test, double threshold,
            Cache cache, int currentSpeciesId ) {

        this.minAnnotatedPopulation = min;
        this.maxAnnotatedPopulation = max == 0 ? Integer.MAX_VALUE : max;
        this.currentSpeciesId = currentSpeciesId;
        this.threshold = threshold;

        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> rawResults = new HashMap<>();
        Map<Edition, Set<GeneOntologyTerm>> unmappedTerms = new HashMap<>();
        Map<Edition, Set<GeneOntologyTerm>> rejectedTerms = new HashMap<>();
        Set<GeneOntologyTerm> termsSignificantInAnyEdition = new HashSet<>();
        Map<Edition, Set<GeneOntologyTerm>> termsSignificant = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Gene>>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, Set<Gene>> data = editionEntry.getValue();

            Map<GeneOntologyTerm, EnrichmentResult> resultsInEdition = new HashMap<>();

            Set<GeneOntologyTerm> um = new HashSet<>();
            Set<GeneOntologyTerm> rj = new HashSet<>();
            Set<GeneOntologyTerm> sig = new HashSet<>();

            int populationSize = cache.getGeneCount( currentSpeciesId, ed );
            int sampleSize = sampleSizes.get( ed );

            // pre loop to remove terms that don't fit population limits, to get test set size for bonferroni and to get
            // ranks

            for ( Entry<GeneOntologyTerm, Set<Gene>> termEntry : data.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                Integer populationAnnotated = cache.getGoSetSizes( currentSpeciesId, ed.getEdition(), term.getGoId() );

                if ( populationAnnotated != null && populationAnnotated >= minAnnotatedPopulation
                        && populationAnnotated <= maxAnnotatedPopulation ) {
                    Integer sampleAnnotated = termEntry.getValue().size();
                    resultsInEdition.put( term, new EnrichmentResult( sampleAnnotated, populationAnnotated, sampleSize,
                            populationSize ) );

                    // Everything starts out significant until we apply a threshold
                    // termsSignificantInAnyEdition.add( term );
                    // sig.add( term );

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

            // Create EnrichmentResults
            int testSetSize = resultsInEdition.keySet().size();

            LinkedHashSet<GeneOntologyTerm> termsSortedByRank = EnrichmentAnalysis
                    .getSortedKeySetByValue( resultsInEdition );
            int rank = -1;
            int k = 0;
            Set<GeneOntologyTerm> maybePile = new HashSet<>();
            for ( GeneOntologyTerm term : termsSortedByRank ) {
                k++;
                EnrichmentResult er = resultsInEdition.get( term );
                if ( test.equals( MultipleTestCorrection.BONFERRONI ) ) {
                    er.setPvalue( Math.min( er.getPvalue() * testSetSize, 1 ) );
                    if ( er.getPvalue() <= this.threshold ) {
                        termsSignificantInAnyEdition.add( term );
                        sig.add( term );
                    }

                } else if ( test.equals( MultipleTestCorrection.BH ) ) {
                    // Single pass method of BH step-up
                    double qTresh = ( k ) * this.threshold / testSetSize;
                    if ( er.getPvalue() <= qTresh ) {

                        // add this term and all terms in maybe pile
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
                } else {
                    termsSignificantInAnyEdition.add( term );
                    sig.add( term );
                }
                er.setRank( ++rank );
            }

            rawResults.put( ed, Collections.unmodifiableMap( resultsInEdition ) );
            unmappedTerms.put( ed, Collections.unmodifiableSet( um ) );
            rejectedTerms.put( ed, Collections.unmodifiableSet( rj ) );
            termsSignificant.put( ed, Collections.unmodifiableSet( sig ) );

        }

        this.rawResults = Collections.unmodifiableMap( rawResults );
        this.unmappedTerms = Collections.unmodifiableMap( unmappedTerms );
        this.rejectedTerms = Collections.unmodifiableMap( rejectedTerms );
        this.termsSignificantInAnyEdition = Collections.unmodifiableSet( termsSignificantInAnyEdition );
        this.termsSignificant = Collections.unmodifiableMap( termsSignificant );

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

    public int getCurrentSpeciesId() {
        return currentSpeciesId;
    }

    public int getMinAnnotatedPopulation() {
        return minAnnotatedPopulation;
    }

    public int getMaxAnnotatedPopulation() {
        return maxAnnotatedPopulation;
    }

    public Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> getRawResults() {
        return rawResults;
    }

    public Map<Edition, Set<GeneOntologyTerm>> getUnmappedTerms() {
        return unmappedTerms;
    }

    public Map<Edition, Set<GeneOntologyTerm>> getRejectedTerms() {
        return rejectedTerms;
    }

    public Set<GeneOntologyTerm> getTermsSignificantInAnyEdition() {
        return termsSignificantInAnyEdition;
    }

    public Map<Edition, Set<GeneOntologyTerm>> getTermsSignificant() {
        return termsSignificant;
    }

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

    public static LinkedHashSet<GeneOntologyTerm> getSortedKeySetByValue( Map<GeneOntologyTerm, EnrichmentResult> data ) {
        LinkedHashSet<GeneOntologyTerm> results = new LinkedHashSet<>();

        List<Entry<GeneOntologyTerm, EnrichmentResult>> entryList = new ArrayList<>( data.entrySet() );

        Collections.sort( entryList, new Comparator<Entry<GeneOntologyTerm, EnrichmentResult>>() {
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
