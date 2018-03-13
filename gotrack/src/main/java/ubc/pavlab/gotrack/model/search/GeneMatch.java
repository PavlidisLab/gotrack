package ubc.pavlab.gotrack.model.search;

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

import ubc.pavlab.gotrack.model.Gene;

/**
 * Represents a potential match when attempting to search for a gene by symbol.
 *
 * @author mjacobson
 * @version $Id$
 */
public class GeneMatch implements Comparable<GeneMatch> {

    public enum Level {

        CURATED( "Curated" ), // Manually chosen by user
        PRIMARY( "Primary Symbol" ), // Gene's symbol is an exact match to the query
        SYNONYM( "Synonym Symbol" ), // Gene's synonym symbol is an exact match to the query
        PREFIX( "Prefix" ), // The query is a prefix to this Gene's symbol
        SIMILAR( "Similar" ), // The query is similar to this Gene's symbol
        NO_MATCH( "No Match" ); // No match was found, Gene will be Null

        private String label;

        private Level( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    public enum Type {

        SINGLE( "Single" ), // Single matching result
        MULTIPLE( "Multiple" ), // Multiple matching results
        NO_MATCH( "No Match" ); // No match was found, Gene will be Null

        private String label;

        private Type( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    private final String querySymbol; // The query that was used to search
    private Gene selectedGene; // The current best guess at a gene
    private Level level;
    private Type type;

    public GeneMatch( String querySymbol, Gene selectedGene, Level level, Type type ) {
        this.querySymbol = querySymbol;
        this.selectedGene = selectedGene;
        this.level = level;
        this.type = type;
    }

    public Gene getSelectedGene() {
        return selectedGene;
    }

    public void setSelectedGene( Gene selectedGene ) {
        if ( selectedGene != null ) {
            this.type = Type.SINGLE;
            this.level = Level.CURATED;
        }
        this.selectedGene = selectedGene;
    }

    public String getQuerySymbol() {
        return querySymbol;
    }

    public String getSymbol() {
        return selectedGene == null ? null : selectedGene.getSymbol();
    }

    public Type getType() {
        return type;
    }

    public Level getLevel() {
        return level;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        GeneMatch geneMatch = (GeneMatch) o;

        if ( querySymbol != null ? !querySymbol.equals( geneMatch.querySymbol ) : geneMatch.querySymbol != null )
            return false;
        return selectedGene != null ? selectedGene.equals( geneMatch.selectedGene ) : geneMatch.selectedGene == null;
    }

    @Override
    public int hashCode() {
        int result = querySymbol != null ? querySymbol.hashCode() : 0;
        result = 31 * result + (selectedGene != null ? selectedGene.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GeneMatch{" +
                "querySymbol='" + querySymbol + '\'' +
                ", selectedGene=" + selectedGene +
                ", level=" + level +
                ", type=" + type +
                '}';
    }

    @Override
    public int compareTo( GeneMatch that ) {
        if ( this.type.compareTo( that.type ) < 0 ) {
            return -1;
        } else if ( this.type.compareTo( that.type ) > 0 ) {
            return 1;
        }

        if ( this.level.compareTo( that.level ) < 0 ) {
            return -1;
        } else if ( this.level.compareTo( that.level ) > 0 ) {
            return 1;
        }
        return 0;
    }

}
