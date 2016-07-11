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
 * Scores which attempt to explore the impact that annotation similarity has on the performance of gene set enrichment
 * analyses
 * 
 * @author mjacobson
 * @version $Id$
 */
public class SimilarityScore {

    private final Double completeTermSim;
    private final Double topTermSim;
    private final Double topGeneSim;
    private final Double topParentsSim;
    private final Set<GeneOntologyTerm> topTerms;
    private final Set<Gene> topGenes;
    private final Set<GeneOntologyTerm> topParents;

    /**
     * @param completeTermJaccard Jaccard between all terms
     * @param topTermJaccard Jaccard between the top N terms
     * @param topGeneJaccard Jaccard between the genes backing the top N terms
     * @param topParentsJaccard Jaccard between the parents of the top N terms
     * @param topTerms top N terms
     * @param topGenes genes backing top N terms
     * @param topParents parents of top N terms
     */
    public SimilarityScore( Double completeTermSim, Double topTermSim, Double topGeneSim,
            Double topParentsSim, Set<GeneOntologyTerm> topTerms, Set<Gene> topGenes,
            Set<GeneOntologyTerm> topParents ) {
        super();
        this.completeTermSim = completeTermSim;
        this.topTermSim = topTermSim;
        this.topGeneSim = topGeneSim;
        this.topParentsSim = topParentsSim;
        this.topTerms = topTerms;
        this.topGenes = topGenes;
        this.topParents = topParents;
    }

    public Double getCompleteTermSim() {
        return completeTermSim;
    }

    public Double getTopTermSim() {
        return topTermSim;
    }

    public Double getTopGeneSim() {
        return topGeneSim;
    }

    public Double getTopParentsSim() {
        return topParentsSim;
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
