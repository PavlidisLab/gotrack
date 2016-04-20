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

package ubc.pavlab.gotrack.model.dto;

import ubc.pavlab.gotrack.model.Aggregate;

/**
 * Data Transfer Object for {@link Aggregate}
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class AggregateDTO {
    private final Integer species;
    private final Integer edition;
    private final Integer geneCount;
    private final Double avgDirectTermsForGene;
    private final Double avgInferredTermsForGene;
    private final Double avgInferredGenesForTerm;
    private final Double avgMultifunctionality;
    private final Double avgDirectSimilarity;
    private final Double avgInferredSimilarity;

    public AggregateDTO( Integer species, Integer edition, Integer geneCount, Double avgDirectTermsForGene,
            Double avgInferredTermsForGene, Double avgInferredGenesForTerm, Double avgMultifunctionality,
            Double avgDirectSimilarity, Double avgInferredSimilarity ) {
        super();
        this.species = species;
        this.edition = edition;
        this.geneCount = geneCount;
        this.avgDirectTermsForGene = avgDirectTermsForGene;
        this.avgInferredTermsForGene = avgInferredTermsForGene;
        this.avgInferredGenesForTerm = avgInferredGenesForTerm;
        this.avgMultifunctionality = avgMultifunctionality;
        this.avgDirectSimilarity = avgDirectSimilarity;
        this.avgInferredSimilarity = avgInferredSimilarity;
    }

    public Integer getSpecies() {
        return species;
    }

    public Integer getEdition() {
        return edition;
    }

    public Integer getGeneCount() {
        return geneCount;
    }

    public Double getAvgDirectTermsForGene() {
        return avgDirectTermsForGene;
    }

    public Double getAvgInferredTermsForGene() {
        return avgInferredTermsForGene;
    }

    public Double getAvgInferredGenesForTerm() {
        return avgInferredGenesForTerm;
    }

    public Double getAvgMultifunctionality() {
        return avgMultifunctionality;
    }

    public Double getAvgDirectSimilarity() {
        return avgDirectSimilarity;
    }

    public Double getAvgInferredSimilarity() {
        return avgInferredSimilarity;
    }

}
