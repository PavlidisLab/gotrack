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

package ubc.pavlab.gotrack.analysis;

import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Jaccard;
import ubc.pavlab.gotrack.utilities.Tversky;

import java.util.*;

/**
 * Calculates scores which attempt to explore the impact that annotation similarity has on the performance of
 * gene set enrichment analyses. The methods of exploration for similarity compare previous annotations for each gene
 * set with either the most current state or a proximal state.
 * 
 * completeTermJaccard: Look at similarity of all enriched GO Terms.
 * topGeneJaccard: Similarity of the genes supporting the top 5 (N) results.
 * topTermJaccard: Similarity of the top 5 (N) results.
 * topParentsJaccard: Similarity of the parents of the top 5 (N) results.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class SimilarityAnalysis {

    private static final Logger log = Logger.getLogger( SimilarityAnalysis.class );

    private final Map<Edition, SimilarityScore> similarityScores;
    private final SimilarityCompareMethod similarityCompareMethod;
    private final SimilarityMethod similarityMethod;

    public SimilarityAnalysis( EnrichmentAnalysis analysis, int topN, SimilarityCompareMethod scm,
            Cache cache ) {
        this( analysis, topN, scm, SimilarityMethod.JACCARD, cache );
    }

    /**
     * @param analysis results of enrichment analysis
     * @param topN number of top terms to use for top N series
     * @param scm compare method to be used in the analysis
     * @param cache Cache bean in order to access propagation needed for parent similarity, null to leave out parent
     *        similarity
     */
    public SimilarityAnalysis( EnrichmentAnalysis analysis, int topN, SimilarityCompareMethod scm, SimilarityMethod sm,
            Cache cache ) {
        // Store method used to run this analysis
        this.similarityCompareMethod = scm;
        this.similarityMethod = sm;

        // Container
        Map<Edition, SimilarityScore> similarityScores = new LinkedHashMap<>();

        // ordered list of editions
        List<Edition> orderedEditions = new ArrayList<>( analysis.getEditions() );
        Collections.sort( orderedEditions );

        // Edition to compare to (or in the case of proximal, the first edition)
        Edition compareEdition = scm.equals( SimilarityCompareMethod.CURRENT )
                ? Iterables.getLast( orderedEditions, null ) : Iterables.getFirst( orderedEditions, null );

        // Top N terms of the compare Edition
        Set<GeneOntologyTerm> compareTopTerms = analysis.getTopNTerms( topN, compareEdition );

        // Genes of top N terms of the compare Edition
        Set<Gene> compareTopGenes = new HashSet<>();
        for ( GeneOntologyTerm term : compareTopTerms ) {
            compareTopGenes.addAll( analysis.getGeneSet( compareEdition, term ) );
        }

        // Parents of top N terms of the compare Edition
        Set<GeneOntologyTerm> compareTopParents = null;
        if ( cache != null ) {
            compareTopParents = cache.propagate( compareTopTerms, compareEdition );
        }

        for ( Edition testingEdition : orderedEditions ) {

            // Complete Terms
            Double completeTermJaccard = null;
            if ( sm.equals( SimilarityMethod.JACCARD ) ) {
                completeTermJaccard = Jaccard.similarity(
                        cache.convertTerms( compareEdition, analysis.getTermsSignificant( testingEdition ) ),
                        analysis.getTermsSignificant( compareEdition ) );
            } else {
                // Changes contents of backing similarity terms, should only be used for meta-analyses
                completeTermJaccard = Tversky.similarityPrototypeWeighted(
                        cache.convertTerms( compareEdition, analysis.getTermsSignificant( testingEdition ) ),
                        analysis.getTermsSignificant( compareEdition ) );
            }

            // Top Terms
            Set<GeneOntologyTerm> testingTopTerms = analysis.getTopNTerms( topN, testingEdition );

            Set<Gene> testingTopGenes = new HashSet<>();

            for ( GeneOntologyTerm term : testingTopTerms ) {
                testingTopGenes.addAll( analysis.getGeneSet( testingEdition, term ) );
            }

            Double topTermJaccard = null;
            if ( sm.equals( SimilarityMethod.JACCARD ) ) {
                topTermJaccard = Jaccard.similarity( cache.convertTerms( compareEdition, testingTopTerms ),
                        compareTopTerms );
            } else {
                // Changes contents of backing similarity terms, should only be used for meta-analyses
                testingTopTerms = cache.convertTerms( compareEdition, testingTopTerms );
                topTermJaccard = Tversky.similarityPrototypeWeighted( testingTopTerms, compareTopTerms );
            }

            // Top Genes

            Double topGeneJaccard = null;
            if ( sm.equals( SimilarityMethod.JACCARD ) ) {
                topGeneJaccard = Jaccard.similarity( testingTopGenes, compareTopGenes );
            } else {
                // Changes contents of backing similarity terms, should only be used for meta-analyses
                topGeneJaccard = Tversky.similarityPrototypeWeighted( testingTopGenes, compareTopGenes );
            }

            // Top Parents
            Set<GeneOntologyTerm> testingTopParents = new HashSet<>();
            Double topParentsJaccard = null;
            if ( cache != null ) {
                testingTopParents = cache.propagate( testingTopTerms, testingEdition );

                if ( sm.equals( SimilarityMethod.JACCARD ) ) {
                    topParentsJaccard = Jaccard.similarity( compareTopParents,
                            cache.convertTerms( compareEdition, testingTopParents ) );
                } else {
                    // Changes contents of backing similarity terms, should only be used for meta-analyses
                    testingTopParents = cache.convertTerms( compareEdition, testingTopParents );
                    topParentsJaccard = Tversky.similarityPrototypeWeighted( testingTopParents, compareTopParents );
                }
            }

            similarityScores.put( testingEdition,
                    new SimilarityScore( completeTermJaccard, topTermJaccard, topGeneJaccard, topParentsJaccard,
                            Collections.unmodifiableSet( testingTopTerms ),
                            Collections.unmodifiableSet( testingTopGenes ),
                            Collections.unmodifiableSet( testingTopParents ) ) );

            if ( scm.equals( SimilarityCompareMethod.PROXIMAL ) ) {
                compareEdition = testingEdition;
                compareTopTerms = testingTopTerms;
                compareTopGenes = testingTopGenes;
                compareTopParents = testingTopParents;
            }

        }

        this.similarityScores = Collections.unmodifiableMap( similarityScores );
    }

    public Map<Edition, SimilarityScore> getSimilarityScores() {
        return similarityScores;
    }

    public SimilarityScore getSimilarityScore( Edition ed ) {
        if ( ed == null ) return null;
        return similarityScores.get( ed );
    }

    public SimilarityCompareMethod getSimilarityCompareMethod() {
        return similarityCompareMethod;
    }

    public SimilarityMethod getSimilarityMethod() {
        return similarityMethod;
    }

}
