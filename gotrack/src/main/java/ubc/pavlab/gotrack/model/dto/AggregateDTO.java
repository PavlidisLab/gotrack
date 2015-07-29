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
    private final Integer accessionCount;
    private final Integer geneCount;
    private final Double accessionAverage;
    private final Double geneAverage;

    public AggregateDTO( Integer species, Integer edition, Integer accessionCount, Integer geneCount,
            Double accessionAverage, Double geneAverage ) {
        super();
        this.species = species;
        this.edition = edition;
        this.accessionCount = accessionCount;
        this.geneCount = geneCount;
        this.accessionAverage = accessionAverage;
        this.geneAverage = geneAverage;
    }

    public Integer getSpecies() {
        return species;
    }

    public Integer getEdition() {
        return edition;
    }

    public Integer getAccessionCount() {
        return accessionCount;
    }

    public Integer getGeneCount() {
        return geneCount;
    }

    public Double getAccessionAverage() {
        return accessionAverage;
    }

    public Double getGeneAverage() {
        return geneAverage;
    }

}
