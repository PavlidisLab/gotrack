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
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneMatches {

    public enum MatchType {

        EXACT("Exact"), EXACT_SYNONYM("Exact Synonym"), MULTIPLE_EXACT_SYNONYMS("Multiple Exact Synonyms"), NO_MATCH(
                "No Match");

        private String label;

        private MatchType( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    private final String querySymbol;
    private Gene selectedGene;
    private final MatchType type;

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

    public MatchType getType() {
        return type;
    }

}
