/*
 * The gotrack project
 * 
 * Copyright (c) 2014 University of British Columbia
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

import ubc.pavlab.gotrack.model.dto.AccessionDTO;

/**
 * Represents a primary accession of the most current edition.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Accession {

    private final int id;
    private final String accession;
    private final Dataset dataset; // Swiss-Prot vs TrEMBL

    public Accession( AccessionDTO dto ) {
        this.id = dto.getId();
        this.accession = dto.getAccession();

        switch (dto.getSubset()) {
            case "Swiss-Prot":
                this.dataset = Dataset.SwissProt;
                break;
            case "TrEMBL":
                this.dataset = Dataset.TrEMBL;
                break;
            default:
                this.dataset = Dataset.Other;
                break;
        }
    }

    public int getId() {
        return id;
    }

    public String getAccession() {
        return accession;
    }

    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return accession;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( accession == null ) ? 0 : accession.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Accession other = ( Accession ) obj;
        if ( accession == null ) {
            if ( other.accession != null ) return false;
        } else if ( !accession.equals( other.accession ) ) return false;
        return true;
    }

}
