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
 * TODO Immutable entry of aggregate table
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class StatsEntry {

    private final Integer accessionCount;
    private final Double avgDirectByAccession;

    private final Integer geneCount;
    private final Double avgDirectByGene;

    public StatsEntry( AggregateDTO dto ) {
        super();
        this.accessionCount = dto.getAccessionCount();
        this.avgDirectByAccession = dto.getAccessionAverage();
        this.geneCount = dto.getGeneCount();
        this.avgDirectByGene = dto.getGeneAverage();
    }

    public Integer getAccessionCount() {
        return accessionCount;
    }

    public Double getAvgDirectByAccession() {
        return avgDirectByAccession;
    }

    public Integer getGeneCount() {
        return geneCount;
    }

    public Double getAvgDirectByGene() {
        return avgDirectByGene;
    }

    @Override
    public String toString() {
        return "StatsEntry [accessionCount=" + accessionCount + ", avgDirectByAccession=" + avgDirectByAccession
                + ", geneCount=" + geneCount + ", avgDirectByGene=" + avgDirectByGene + "]";
    }

}
