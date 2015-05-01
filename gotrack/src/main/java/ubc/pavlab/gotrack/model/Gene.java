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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * TODO I really wish this was immutable...
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Gene {

    String symbol;
    Set<Accession> accessions = new HashSet<>();
    Set<String> synonyms = new HashSet<>();

    public Gene() {
        super();
    }

    public Gene( String symbol ) {
        super();
        this.symbol = symbol;
    }

    public Gene( String symbol, Set<Accession> accessions, Set<String> synonyms ) {
        super();
        this.symbol = symbol;
        this.accessions = accessions;
        this.synonyms = synonyms;
    }

    public String viewSynonyms() {
        return StringUtils.join( synonyms, "|" );
    }

    public String viewAccessions() {
        String result = null;
        if ( accessions != null ) {
            StringBuilder sb = new StringBuilder();
            Iterator<Accession> it = accessions.iterator();
            if ( it.hasNext() ) {
                sb.append( it.next().getAccession() );
            }
            while ( it.hasNext() ) {
                sb.append( "|" ).append( it.next().getAccession() );
            }
            result = sb.toString();
        }
        return result;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol( String symbol ) {
        this.symbol = symbol;
    }

    public Set<Accession> getAccessions() {
        return accessions;
    }

    public void setAccessions( Set<Accession> accessions ) {
        this.accessions = accessions;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms( Set<String> synonyms ) {
        this.synonyms = synonyms;
    }

    @Override
    public String toString() {
        return "Gene [symbol=" + symbol + ", accessions=" + accessions + ", synonyms=" + synonyms + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( symbol == null ) ? 0 : symbol.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Gene other = ( Gene ) obj;
        if ( symbol == null ) {
            if ( other.symbol != null ) return false;
        } else if ( !symbol.equals( other.symbol ) ) return false;
        return true;
    }

}
