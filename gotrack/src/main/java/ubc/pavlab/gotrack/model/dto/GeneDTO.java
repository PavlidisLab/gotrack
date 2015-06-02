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
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class GeneDTO {
    private final Integer species;
    private final String symbol;
    private final String synonyms;
    private final String accession;

    public GeneDTO( Integer species, String symbol, String synonyms, String accession ) {
        super();
        this.species = species;
        this.symbol = symbol;
        this.synonyms = synonyms;
        this.accession = accession;
    }

    public Integer getSpecies() {
        return species;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public String getAccession() {
        return accession;
    }
}
