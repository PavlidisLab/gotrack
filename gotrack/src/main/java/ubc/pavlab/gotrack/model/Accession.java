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

import java.util.Collection;
import java.util.HashSet;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Accession {

    private String accession;
    private String symbol;
    private Collection<String> secondary = new HashSet<String>();

    /**
     * 
     */
    public Accession() {
    }

    public Accession( String accession ) {
        super();
        this.accession = accession;
    }

    public Accession( String accession, String symbol ) {
        super();
        this.accession = accession;
        this.symbol = symbol;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol( String symbol ) {
        this.symbol = symbol;
    }

    public Collection<String> getSecondary() {
        return secondary;
    }

    public void setSecondary( Collection<String> secondary ) {
        this.secondary = secondary;
    }

    public boolean addSecondary( String secondary ) {
        return this.secondary.add( secondary );
    }

    @Override
    public String toString() {
        return "GeneSymbol [accession=" + accession + ", symbol=" + symbol + "]";
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
