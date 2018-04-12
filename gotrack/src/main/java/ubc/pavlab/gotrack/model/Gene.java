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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import java.util.Set;

/**
 * Represents a Gene. Thread-safe.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Gene implements Comparable<Gene> {
    private final int id;
    private final String symbol;
    private final String name;
    private final Species species;
    private final Accession accession;
    private final Set<String> synonyms;

    private Gene( GeneBuilder builder ) {
        this.id = builder.id;
        this.symbol = builder.symbol;
        this.name = builder.name;
        this.species = builder.species;
        this.accession = builder.accession;
        this.synonyms = builder.synonyms.build();
    }

    public int getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public Accession getAccession() {
        return accession;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }

    public Species getSpecies() {
        return species;
    }

    @Override
    public String toString() {
        return "Gene [symbol=" + symbol + ", accession=" + accession.getAccession() + ", species=" + species.getCommonName() + "]";
    }


    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        Gene gene = (Gene) o;

        return id == gene.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo( Gene o ) {
        return this.getSymbol().compareTo( o.getSymbol() );
    }

    public static class GeneBuilder {
        private final int id;
        private final String symbol;
        private final String name;
        private final Species species;
        private final Accession accession;
        private Builder<String> synonyms = new ImmutableSet.Builder<>();

        public GeneBuilder( int id, String symbol, String name, Species species, Accession accession ) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.species = species;
            this.accession = accession;
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
