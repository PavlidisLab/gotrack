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

package ubc.pavlab.gotrack.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import ubc.pavlab.gotrack.utilities.Jaccard;

/**
 * scores which attempt to explore the impact that annotation stability has on the performance of gene set enrichment
 * analyses
 * 
 * @author mjacobson
 * @version $Id$
 */
public class StabilityScore {

    private final Double completeTermJaccard;
    private final Double topTermJaccard;
    private final Double topGeneJaccard;
    private final Set<GeneOntologyTerm> topTerms;
    private final Set<Gene> topGenes;

    public StabilityScore( Double completeTermJaccard, Double topTermJaccard, Double topGeneJaccard ) {
        super();
        this.completeTermJaccard = completeTermJaccard;
        this.topTermJaccard = topTermJaccard;
        this.topGeneJaccard = topGeneJaccard;
        this.topTerms = null;
        this.topGenes = null;
    }

    public StabilityScore( LinkedHashMap<GeneOntologyTerm, Set<Gene>> testingEdition,
            LinkedHashMap<GeneOntologyTerm, Set<Gene>> currentEdition, int n ) {

        completeTermJaccard = Jaccard.similarity( testingEdition.keySet(), currentEdition.keySet() );
        Set<GeneOntologyTerm> testingTopTerms = getTopNTerms( testingEdition.keySet(), n );
        Set<GeneOntologyTerm> currentTopTerms = getTopNTerms( currentEdition.keySet(), n );
        topTermJaccard = Jaccard.similarity( testingTopTerms, currentTopTerms );

        Set<Gene> testingTopGenes = new HashSet<>();

        for ( GeneOntologyTerm term : testingTopTerms ) {
            testingTopGenes.addAll( testingEdition.get( term ) );
        }

        Set<Gene> currentTopGenes = new HashSet<>();

        for ( GeneOntologyTerm term : currentTopTerms ) {
            currentTopGenes.addAll( currentEdition.get( term ) );
        }

        topGeneJaccard = Jaccard.similarity( testingTopGenes, currentTopGenes );

        this.topTerms = Collections.unmodifiableSet( testingTopTerms );
        this.topGenes = Collections.unmodifiableSet( testingTopGenes );

    }

    private static Set<GeneOntologyTerm> getTopNTerms( Collection<GeneOntologyTerm> set, int n ) {
        int cnt = 0;
        Set<GeneOntologyTerm> results = new LinkedHashSet<>();
        for ( GeneOntologyTerm geneOntologyTerm : set ) {
            cnt++;
            if ( cnt > n ) {
                break;
            }
            results.add( geneOntologyTerm );
        }
        return results;
    }

    public Double getCompleteTermJaccard() {
        return completeTermJaccard;
    }

    public Double getTopTermJaccard() {
        return topTermJaccard;
    }

    public Double getTopGeneJaccard() {
        return topGeneJaccard;
    }

    public Set<GeneOntologyTerm> getTopTerms() {
        return topTerms;
    }

    public Set<Gene> getTopGenes() {
        return topGenes;
    }

}
