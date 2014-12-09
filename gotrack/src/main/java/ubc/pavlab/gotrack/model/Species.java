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

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Species {

    private Integer id;
    private String commonName;
    private String scientificName;
    private Integer taxon;
    private Integer interactingTaxon;

    /**
     * 
     */
    public Species() {
    }

    public Species( Integer id, String commonName, String scientificName, Integer taxon, Integer interactingTaxon ) {
        super();
        this.id = id;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.taxon = taxon;
        this.interactingTaxon = interactingTaxon;
    }

    public Integer getId() {
        return id;
    }

    public void setId( Integer id ) {
        this.id = id;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName( String commonName ) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName( String scientificName ) {
        this.scientificName = scientificName;
    }

    public Integer getTaxon() {
        return taxon;
    }

    public void setTaxon( Integer taxon ) {
        this.taxon = taxon;
    }

    public Integer getInteractingTaxon() {
        return interactingTaxon;
    }

    public void setInteractingTaxon( Integer interactingTaxon ) {
        this.interactingTaxon = interactingTaxon;
    }

    @Override
    public String toString() {
        return "Species [id=" + id + ", commonName=" + commonName + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Species other = ( Species ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

}
