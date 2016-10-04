/*
 * The gotrack project
 * 
 * Copyright (c) 2016 University of British Columbia
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

import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CompareTermsTableValues implements Comparable<CompareTermsTableValues> {

    public enum TermComparison {

        GAIN("Gain"), LOSS("Loss"), CONSTANT("Constant");

        private String label;

        private TermComparison( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    public CompareTermsTableValues( GeneOntologyTerm t, TermComparison comparison ) {
        this.term = t;
        this.comparison = comparison;
    }

    private final TermComparison comparison;
    private final GeneOntologyTerm term;

    public TermComparison getComparison() {
        return comparison;
    }

    public GeneOntologyTerm getTerm() {
        return term;
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
        CompareTermsTableValues other = ( CompareTermsTableValues ) obj;
        if ( term == null ) {
            if ( other.term != null ) return false;
        } else if ( !term.equals( other.term ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "CompareTermsTableValues [comparison=" + comparison + ", term=" + term + "]";
    }

    @Override
    public int compareTo( CompareTermsTableValues o ) {
        int comparison = this.getComparison().compareTo( o.getComparison() );
        return comparison == 0 ? this.getTerm().compareTo( o.getTerm() ) : comparison;
    }

}
