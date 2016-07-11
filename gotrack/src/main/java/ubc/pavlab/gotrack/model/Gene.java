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

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Represents a Gene. Thread-safe.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Gene {
    private final int id;
    private final String symbol;
    private final Species species;
    private final Set<Accession> accessions;
    private final Set<String> synonyms;

    private Gene( GeneBuilder builder ) {
        this.id = builder.id;
        this.symbol = builder.symbol;
        this.species = builder.species;
        this.accessions = builder.accessions;
        this.synonyms = builder.synonyms.build();
    }

    public int getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Set<Accession> getAccessions() {
        return accessions;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }

    public Species getSpecies() {
        return species;
    }

    @Override
    public String toString() {
        return "Gene [symbol=" + symbol + ", species=" + species.getCommonName() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( species == null ) ? 0 : species.hashCode() );
        result = prime * result + ( ( symbol == null ) ? 0 : symbol.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Gene other = ( Gene ) obj;
        if ( species == null ) {
            if ( other.species != null ) return false;
        } else if ( !species.equals( other.species ) ) return false;
        if ( symbol == null ) {
            if ( other.symbol != null ) return false;
        } else if ( !symbol.equals( other.symbol ) ) return false;
        return true;
    }

    public static class GeneBuilder {
        private final int id;
        private final String symbol;
        private final Species species;
        private final Set<Accession> accessions;
        private Builder<String> synonyms = new ImmutableSet.Builder<String>();

        public GeneBuilder( int id, String symbol, Species species, Collection<Accession> accessions ) {
            this.id = id;
            this.symbol = symbol;
            this.species = species;
            this.accessions = ImmutableSet.copyOf( accessions );
        }

        public GeneBuilder synonym( String syn ) {
            this.synonyms.add( syn );
            return this;
        }

        public Gene build() {
            return new Gene( this );
        }
    }

}
