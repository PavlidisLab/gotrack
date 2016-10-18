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

import ubc.pavlab.gotrack.analysis.EnrichmentResult;
import ubc.pavlab.gotrack.analysis.StabilityScore;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Represents a row in the stability table of EnrichmentView
 * 
 * @author mjacobson
 * @version $Id$
 */
@Deprecated
public class StabilityTableValues implements Comparable<StabilityTableValues> {

    private final GeneOntologyTerm term;
    private final EnrichmentResult result;
    private final StabilityScore stability;
    private final int quantileAvg;
    private final int quantile;

    public StabilityTableValues( GeneOntologyTerm term, EnrichmentResult result, StabilityScore stability, int quantile,
            int quantileAvg ) {
        super();
        this.term = term;
        this.result = result;
        this.stability = stability;
        this.quantileAvg = quantileAvg;
        this.quantile = quantile;
        // this.rowKey = edition.getEdition().toString() + term.getGoId();
    }

    public GeneOntologyTerm getTerm() {
        return term;
    }

    public EnrichmentResult getResult() {
        return result;
    }

    public StabilityScore getStability() {
        return stability;
    }

    public int getQuantileAvg() {
        return quantileAvg;
    }

    public int getQuantile() {
        return quantile;
    }

    @Override
    public String toString() {
        return "StabilityTableValues [term=" + term + ", result=" + result + ", stability=" + stability + ", quantile="
                + quantileAvg + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( term == null ) ? 0 : term.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        StabilityTableValues other = ( StabilityTableValues ) obj;
        if ( term == null ) {
            if ( other.term != null ) return false;
        } else if ( !term.equals( other.term ) ) return false;
        return true;
    }

    @Override
    public int compareTo( StabilityTableValues o ) {
        // sort according to stability, if stability is the same sort by goId
        int comparison = Double.compare( this.stability.getAverageScore(), o.getStability().getAverageScore() );

        return comparison == 0 ? this.term.compareTo( o.getTerm() ) : comparison;
    }

}
