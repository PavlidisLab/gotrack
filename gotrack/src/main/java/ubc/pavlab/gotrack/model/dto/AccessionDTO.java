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

import ubc.pavlab.gotrack.model.Accession;

/**
 * Data Transfer Object for {@link Accession}
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AccessionDTO {
    private final int id;
    private final int speciesId;
    private final int edition;
    private final String accession;
    private final String symbol;
    private final String name;
    private final String subset;

    public AccessionDTO( int id, int speciesId, int edition, String accession, String symbol, String name, String subset ) {
        this.id = id;
        this.speciesId = speciesId;
        this.edition = edition;
        this.accession = accession;
        this.symbol = symbol;
        this.name = name;
        this.subset = subset;
    }

    public int getId() {
        return id;
    }

    public int getSpeciesId() {
        return speciesId;
    }

    public int getEdition() {
        return edition;
    }

    public String getAccession() {
        return accession;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getSubset() {
        return subset;
    }
}
