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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ubc.pavlab.gotrack.analysis.EnrichmentResult;
import ubc.pavlab.gotrack.analysis.StabilityScore;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Represents a row in the enrichment table of EnrichmentView
 *
 * @author mjacobson
 */
@Getter
@EqualsAndHashCode(of = {"edition", "term"})
@ToString
public class EnrichmentTableValues implements Comparable<EnrichmentTableValues> {

    // private final String rowKey;
    private final Edition edition;
    private final GeneOntologyTerm term;
    private final EnrichmentResult result;
    private final StabilityScore stability;
    private final int stabilityQuantile;

    public EnrichmentTableValues( Edition edition, GeneOntologyTerm term, EnrichmentResult result,
                                  StabilityScore stability, int stabilityQuantile ) {
        super();
        this.edition = edition;
        this.term = term;
        this.result = result;
        this.stability = stability;
        this.stabilityQuantile = stabilityQuantile;
        // this.rowKey = edition.getEdition().toString() + term.getGoId();
    }

    @Override
    public int compareTo( EnrichmentTableValues o ) {
        // sort according to rank, if ranks are the same sort by goId
        int comparison = Double.compare( this.result.getRank(), o.getResult().getRank() );

        return comparison == 0 ? this.term.compareTo( o.getTerm() ) : comparison;
    }

}
