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

import ubc.pavlab.gotrack.model.dto.AggregateDTO;

/**
 * Represents a collection of aggregate statistics for a specific edition and species
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Aggregate {

    private final Integer geneCount;
    private final Double avgDirectByGene;
    private final Double avgInferredByGene;
    private final Double avgGenesByTerm;

    public Aggregate( Integer geneCount, Double avgDirectByGene, Double avgInferredByGene, Double avgGenesByTerm ) {
        super();
        this.geneCount = geneCount;
        this.avgDirectByGene = avgDirectByGene;
        this.avgInferredByGene = avgInferredByGene;
        this.avgGenesByTerm = avgGenesByTerm;
    }

    public Aggregate( AggregateDTO dto ) {
        this.geneCount = dto.getGeneCount();
        this.avgDirectByGene = dto.getAvgDirectTermsForGene();
        this.avgInferredByGene = dto.getAvgInferredTermsForGene();
        this.avgGenesByTerm = dto.getAvgInferredGenesForTerm();
    }

    public Integer getGeneCount() {
        return geneCount;
    }

    public Double getAvgDirectByGene() {
        return avgDirectByGene;
    }

    public Double getAvgInferredByGene() {
        return avgInferredByGene;
    }

    public Double getAvgGenesByTerm() {
        return avgGenesByTerm;
    }

    @Override
    public String toString() {
        return "Aggregate [geneCount=" + geneCount + ", avgDirectByGene=" + avgDirectByGene + ", avgInferredByGene="
                + avgInferredByGene + ", avgGenesByTerm=" + avgGenesByTerm + "]";
    }

}
