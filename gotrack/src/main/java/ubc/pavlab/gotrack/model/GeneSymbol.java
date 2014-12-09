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

import java.util.List;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneSymbol {

    private String symbol;
    private List<String> synonyms;

    /**
     * 
     */
    public GeneSymbol() {
        // TODO Auto-generated constructor stub
    }

    public GeneSymbol( String symbol, List<String> synonyms ) {
        super();
        this.symbol = symbol;
        this.synonyms = synonyms;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol( String symbol ) {
        this.symbol = symbol;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms( List<String> synonyms ) {
        this.synonyms = synonyms;
    }

    @Override
    public String toString() {
        return "Gene [symbol=" + symbol + "]";
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
        GeneSymbol other = ( GeneSymbol ) obj;
        if ( symbol == null ) {
            if ( other.symbol != null ) return false;
        } else if ( !symbol.equals( other.symbol ) ) return false;
        return true;
    }

}
