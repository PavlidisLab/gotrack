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

/**
 * Data Transfer Object for statistics on hit counts of genes.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneStatsDTO {
    private final Integer id;
    private final Integer speciesId;
    private final String accession;
    private final String symbol;
    private final Integer count;

    public GeneStatsDTO( Integer id, Integer species_id, String accession, String symbol, Integer count ) {
        super();
        this.id = id;
        this.speciesId = species_id;
        this.accession = accession;
        this.symbol = symbol;
        this.count = count;
    }

    public Integer getId() {
        return id;
    }

    public Integer getSpeciesId() {
        return speciesId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Integer getCount() {
        return count;
    }

    public String getAccession() {
        return accession;
    }
}
