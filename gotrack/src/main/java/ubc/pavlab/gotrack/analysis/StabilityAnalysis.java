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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
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

    private final Map<Edition, StabilityScore> stabilityScores;
    private final EnrichmentAnalysis analysis;

    /**
     * @param analysis results of enrichment analysis
     * @param TOP_N_JACCARD number of top terms to use for top N series
     * @param scm compare method to be used in the analysis
     * @param cache Cache bean in order to access propagation needed for parent similarity, null to leave out parent
     *        similarity
     */
    public StabilityAnalysis( EnrichmentAnalysis analysis, int TOP_N_JACCARD, SimilarityCompareMethod scm, Cache cache ) {
        this.analysis = analysis;
        Map<Edition, StabilityScore> stabilityScores = new LinkedHashMap<>();
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
            compareTopParents = cache.propagate( compareTopTerms, compareEdition.getGoEditionId() );
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
                testingTopParents = cache.propagate( testingTopTerms, testingEdition.getGoEditionId() );

                topParentsJaccard = Jaccard.similarity( testingTopParents, compareTopParents );
            }

            stabilityScores.put(
                    testingEdition,
                    new StabilityScore( completeTermJaccard, topTermJaccard, topGeneJaccard, topParentsJaccard,
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

        this.stabilityScores = Collections.unmodifiableMap( stabilityScores );

    }

    public Map<Edition, StabilityScore> getStabilityScores() {
        return stabilityScores;
    }

}
