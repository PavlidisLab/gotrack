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

package ubc.pavlab.gotrack.model.table;

import ubc.pavlab.gotrack.model.Gene;

/**
 * Represents a potential match when attempting to search for a gene by symbol.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneMatches {

    public enum MatchType {

        EXACT("Exact"), // Gene's symbol is an exact match to the query
        EXACT_SYNONYM("Exact Synonym"), // This Gene (and only this Gene) has a synonym that is an exact match this the query
        MULTIPLE_EXACT_SYNONYMS("Multiple Exact Synonyms"), // This Gene and others have a synonym that is an exact match to the query
        PREFIX("Prefix"), // The query is a prefix to this Gene's symbol
        SIMILAR("Similar"), // The query is similar to this Gene's symbol 
        NO_MATCH("No Match"); // No match was found, Gene will be Null

        private String label;

        private MatchType( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    private final String querySymbol; // The query that was used to search
    private Gene selectedGene; // The current best guess at a gene
    private final MatchType type; // What type of match this best guess is

    public GeneMatches( String querySymbol, Gene selectedGene, MatchType type ) {
        this.querySymbol = querySymbol;
        this.selectedGene = selectedGene;
        this.type = type;
    }

    public Gene getSelectedGene() {
        return selectedGene;
    }

    public void setSelectedGene( Gene selectedGene ) {
        this.selectedGene = selectedGene;
    }

    public String getQuerySymbol() {
        return querySymbol;
    }

    public String getSymbol() {
        return selectedGene == null ? null : selectedGene.getSymbol();
    }

    public MatchType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "GeneMatches [querySymbol=" + querySymbol + ", selectedGene=" + selectedGene.getSymbol() + ", type="
                + type + "]";
    }

}
