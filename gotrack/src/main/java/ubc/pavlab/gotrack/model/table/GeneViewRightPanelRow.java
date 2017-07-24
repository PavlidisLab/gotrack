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

import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationType;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import java.util.Collection;

/**
 * 
 * @author mjacobson
 */
public class GeneViewRightPanelRow implements Comparable<GeneViewRightPanelRow> {

    private final GeneOntologyTerm term;
    private final AnnotationType type;
    private final Collection<Annotation> annotations;

    public GeneViewRightPanelRow( GeneOntologyTerm term, AnnotationType type, Collection<Annotation> annotations ) {
        this.term = term;
        this.type = type;
        this.annotations = annotations;
    }

    public GeneOntologyTerm getTerm() {
        return term;
    }

    public AnnotationType getType() {
        return type;
    }

    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "GeneViewRightPanelRow{" +
                "term=" + term +
                ", type=" + type +
                '}';
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
        GeneViewRightPanelRow other = (GeneViewRightPanelRow) obj;
        if ( term == null ) {
            if ( other.term != null ) return false;
        } else if ( !term.equals( other.term ) ) return false;
        return true;
    }


    @Override
    public int compareTo( GeneViewRightPanelRow that ) {
        if ( this.type.compareTo( that.type ) < 0 ) {
            return -1;
        } else if ( this.type.compareTo( that.type ) > 0 ) {
            return 1;
        }

        if ( this.term.compareTo( that.term ) < 0 ) {
            return -1;
        } else if ( this.term.compareTo( that.term ) > 0 ) {
            return 1;
        }
        return 0;
    }
}