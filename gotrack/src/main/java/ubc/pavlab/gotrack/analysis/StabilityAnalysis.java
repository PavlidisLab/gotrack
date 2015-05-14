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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;

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

    private final Map<Edition, StabilityScore> stabilityScores;

    public StabilityAnalysis( Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults,
            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap, int TOP_N_JACCARD ) {
        Map<Edition, StabilityScore> stabilityScores = new LinkedHashMap<>();

        List<Edition> orderedEditions = new ArrayList<>( enrichmentResults.keySet() );
        Collections.sort( orderedEditions );
        Edition currentEdition = Iterables.getLast( orderedEditions, null );
        LinkedHashSet<GeneOntologyTerm> currentSortedTerms = getSortedKeySetByValue( enrichmentResults
                .get( currentEdition ) );

        LinkedHashMap<GeneOntologyTerm, Set<Gene>> currentSortedTermsMap = new LinkedHashMap<>();

        for ( GeneOntologyTerm term : currentSortedTerms ) {
            currentSortedTermsMap.put( term, geneGOMap.get( currentEdition ).get( term ) );
        }

        // completeTermJaccard

        for ( Edition ed : orderedEditions ) {
            LinkedHashSet<GeneOntologyTerm> sortedTerms = getSortedKeySetByValue( enrichmentResults.get( ed ) );

            LinkedHashMap<GeneOntologyTerm, Set<Gene>> sortedTermsMap = new LinkedHashMap<>();

            for ( GeneOntologyTerm term : sortedTerms ) {
                sortedTermsMap.put( term, geneGOMap.get( ed ).get( term ) );
            }

            stabilityScores.put( ed, new StabilityScore( sortedTermsMap, currentSortedTermsMap, TOP_N_JACCARD ) );
        }

        this.stabilityScores = Collections.unmodifiableMap( stabilityScores );
    }

    public Map<Edition, StabilityScore> getStabilityScores() {
        return stabilityScores;
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
