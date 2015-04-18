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

    public StabilityScore( Double completeTermJaccard, Double topTermJaccard, Double topGeneJaccard ) {
        super();
        this.completeTermJaccard = completeTermJaccard;
        this.topTermJaccard = topTermJaccard;
        this.topGeneJaccard = topGeneJaccard;
    }

    public StabilityScore( LinkedHashSet<GeneOntologyTerm> testingEdition,
            LinkedHashSet<GeneOntologyTerm> currentEdition, int n ) {

        completeTermJaccard = Jaccard.similarity( testingEdition, currentEdition );
        topTermJaccard = Jaccard.similarity( getTopNTerms( testingEdition, n ), getTopNTerms( currentEdition, n ) );
        // TODO figure out how to calculate topGeneJaccard
        topGeneJaccard = null;

    }

    private static Set<GeneOntologyTerm> getTopNTerms( LinkedHashSet<GeneOntologyTerm> set, int n ) {
        int cnt = 0;
        Set<GeneOntologyTerm> results = new LinkedHashSet<>();
        for ( GeneOntologyTerm geneOntologyTerm : set ) {
            results.add( geneOntologyTerm );
            cnt++;
            if ( cnt > n ) {
                break;
            }
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

}
