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

import lombok.Getter;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Jaccard;
import ubc.pavlab.gotrack.utilities.Tversky;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
@Getter
public class SimilarityAnalysis {

    private static final Logger log = Logger.getLogger( SimilarityAnalysis.class );

    private final Map<Edition, SimilarityScore> similarityScores;
    private final int topN;
    private final Edition referenceEdition;
    private final SimilarityMethod similarityMethod;

    public SimilarityAnalysis( EnrichmentAnalysis analysis, int topN, Cache cache ) {
        this( analysis, topN, analysis.getEditions().stream().max( Edition ::compareTo ).get(), SimilarityMethod.JACCARD, cache );
    }

    public SimilarityAnalysis( EnrichmentAnalysis analysis, int topN, SimilarityMethod sm, Cache cache ) {
        this( analysis, topN, analysis.getEditions().stream().max( Edition ::compareTo ).get(), sm, cache );
    }

    /**
     * @param analysis results of enrichment analysis
     * @param topN number of top terms to use for top N series
     * @param referenceEdition compare all editions to this one
     * @param cache Cache bean in order to access propagation needed for parent similarity
     */
    public SimilarityAnalysis( EnrichmentAnalysis analysis, int topN, Edition referenceEdition, SimilarityMethod sm,
                               Cache cache ) {
        // Store method used to run this analysis
        this.referenceEdition = referenceEdition;
        this.similarityMethod = sm;
        this.topN = topN;

        // Container
        Map<Edition, SimilarityScore> similarityScores = new LinkedHashMap<>();

        SimilarityEnrichmentWrapper reference = new SimilarityEnrichmentWrapper( analysis.getRawResults( referenceEdition ), referenceEdition, topN );

        analysis.getEditions().stream().sorted().forEach( testingEdition -> {

            SimilarityEnrichmentWrapper test = new SimilarityEnrichmentWrapper( analysis.getRawResults( testingEdition ), testingEdition, topN );

            similarityScores.put( testingEdition, compareEnrichments( reference, test, sm, cache ) );

        } );

        this.similarityScores = Collections.unmodifiableMap( similarityScores );
    }

    public SimilarityScore compareEnrichments( SimilarityEnrichmentWrapper reference, SimilarityEnrichmentWrapper test, SimilarityMethod sm, Cache cache) {

        // Complete Terms
        Double completeTermSim = SimilarityAnalysis.similarity( sm,
                cache.convertTerms( reference.getEdition(), test.getEnrichment().getSignificantTerms() ),
                reference.getEnrichment().getSignificantTerms() );

        // Top Terms
        Double topTermSim = SimilarityAnalysis.similarity( sm,
                cache.convertTerms( reference.getEdition(), test.getTopTerms() ),
                reference.getTopTerms() );

        // Top Genes
        Double topGeneSim = SimilarityAnalysis.similarity( sm,
                test.getTopGenes(), reference.getTopGenes() );

        // Top Parents
        Double topParentsSim = SimilarityAnalysis.similarity( sm,
                cache.convertTerms( reference.getEdition(), test.getTopParents() ),
                reference.getTopParents() );

        return new SimilarityScore( completeTermSim, topTermSim, topGeneSim, topParentsSim,
                        Collections.unmodifiableSet( test.getTopTerms() ),
                        Collections.unmodifiableSet( test.getTopGenes() ),
                        Collections.unmodifiableSet( test.getTopParents() ) );
    }

    public SimilarityScore getSimilarityScore( Edition ed ) {
        if ( ed == null ) return null;
        return similarityScores.get( ed );
    }

    private static <T> Double similarity( SimilarityMethod sm, Set<T> test, Set<T> reference ) {
        if ( sm.equals( SimilarityMethod.JACCARD ) ) {
            return Jaccard.similarity( test, reference );
        } else {
            return Tversky.similarityPrototypeWeighted( test, reference );
        }
    }

}

@Getter
class SimilarityEnrichmentWrapper {

    private final Enrichment<GeneOntologyTerm, Gene> enrichment;

    private final Edition edition;

    // Top N terms of the compare Edition
    private final Set<GeneOntologyTerm> topTerms;

    // Genes of top N terms of the compare Edition
    private final Set<Gene> topGenes;

    // Parents of top N terms of the compare Edition
    private final Set<GeneOntologyTerm> topParents;

    SimilarityEnrichmentWrapper( Enrichment<GeneOntologyTerm, Gene> enrichment, Edition edition, int topN ) {
        this.enrichment = enrichment;
        this.edition = edition;

        this.topTerms = enrichment.getTopNTerms( topN );

        // Genes of top N terms of the compare Edition
        this.topGenes = topTerms.stream()
                .flatMap( t -> enrichment.getSamplePopulation().getEntities( t ).stream() )
                .collect( Collectors.toSet() );

        // Parents of top N terms of the compare Edition
        this.topParents = GeneOntologyTerm.propagate( topTerms.stream() ).collect( Collectors.toSet() );
    }

}