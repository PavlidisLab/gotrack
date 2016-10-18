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
    private final int age;

    public GeneTableValues( Edition recentEdition, GeneOntologyTerm term, int age ) {
        super();
        this.recentEdition = recentEdition;
        this.term = term;
        this.age = age;
    }

    public Edition getRecentEdition() {
        return recentEdition;
    }

    public GeneOntologyTerm getTerm() {
        return term;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "GeneTableValues [recentEdition=" + recentEdition + ", term=" + term.getGoId() + "]";
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
        int comparison = o.getRecentEdition().compareTo( recentEdition );

        return comparison == 0 ? this.term.compareTo( o.getTerm() ) : comparison;

    }

}