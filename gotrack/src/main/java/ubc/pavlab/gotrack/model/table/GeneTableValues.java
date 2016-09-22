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

import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneTableValues implements Comparable<GeneTableValues> {

    // private final String rowKey;
    private final Edition recentEdition;
    private final GeneOntologyTerm term;
    private final boolean alwaysExist;
    private final boolean currentlyExist;
    private final boolean previouslyExist;

    public GeneTableValues( Edition recentEdition, GeneOntologyTerm term, boolean alwaysExist, boolean currentlyExist,
            boolean previouslyExist ) {
        super();
        this.recentEdition = recentEdition;
        this.term = term;
        this.alwaysExist = alwaysExist;
        this.currentlyExist = currentlyExist;
        this.previouslyExist = previouslyExist;
    }

    public Edition getRecentEdition() {
        return recentEdition;
    }

    public GeneOntologyTerm getTerm() {
        return term;
    }

    public boolean isAlwaysExist() {
        return alwaysExist;
    }

    public boolean isCurrentlyExist() {
        return currentlyExist;
    }

    public boolean isPreviouslyExist() {
        return previouslyExist;
    }

    @Override
    public String toString() {
        return "GeneTableValues [recentEdition=" + recentEdition + ", term=" + term + ", alwaysExist=" + alwaysExist
                + ", currentlyExist=" + currentlyExist + ", previouslyExist=" + previouslyExist + "]";
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
        GeneTableValues other = ( GeneTableValues ) obj;
        if ( term == null ) {
            if ( other.term != null ) return false;
        } else if ( !term.equals( other.term ) ) return false;
        return true;
    }

    @Override
    public int compareTo( GeneTableValues o ) {
        // sort according to existence, followed by goId
        int comparison = Boolean.compare( o.isAlwaysExist(), alwaysExist );
        if ( comparison == 0 ) {
            comparison = Boolean.compare( o.isCurrentlyExist(), currentlyExist );
        } else {
            return comparison;
        }

        if ( comparison == 0 ) {
            comparison = Boolean.compare( o.isPreviouslyExist(), previouslyExist );
        } else {
            return comparison;
        }

        return comparison == 0 ? this.term.compareTo( o.getTerm() ) : comparison;

    }

}