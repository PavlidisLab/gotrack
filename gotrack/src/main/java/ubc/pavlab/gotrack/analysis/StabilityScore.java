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

import java.util.Set;

import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

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
    private final Double topParentsJaccard;
    private final Set<GeneOntologyTerm> topTerms;
    private final Set<Gene> topGenes;
    private final Set<GeneOntologyTerm> topParents;

    /**
     * @param completeTermJaccard
     * @param topTermJaccard
     * @param topGeneJaccard
     * @param topTerms
     * @param topGenes
     */
    public StabilityScore( Double completeTermJaccard, Double topTermJaccard, Double topGeneJaccard,
            Double topParentsJaccard, Set<GeneOntologyTerm> topTerms, Set<Gene> topGenes,
            Set<GeneOntologyTerm> topParents ) {
        super();
        this.completeTermJaccard = completeTermJaccard;
        this.topTermJaccard = topTermJaccard;
        this.topGeneJaccard = topGeneJaccard;
        this.topParentsJaccard = topParentsJaccard;
        this.topTerms = topTerms;
        this.topGenes = topGenes;
        this.topParents = topParents;
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

    public Double getTopParentsJaccard() {
        return topParentsJaccard;
    }

    public Set<GeneOntologyTerm> getTopTerms() {
        return topTerms;
    }

    public Set<Gene> getTopGenes() {
        return topGenes;
    }

    public Set<GeneOntologyTerm> getTopParents() {
        return topParents;
    }

}
