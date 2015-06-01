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

package ubc.pavlab.gotrack.model.go;

import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Parent {
    private final GeneOntologyTerm parent;
    private final RelationshipType type;

    public Parent( GeneOntologyTerm parent, RelationshipType type ) {
        super();
        this.parent = parent;
        this.type = type;
    }

    public GeneOntologyTerm getParent() {
        return parent;
    }

    public RelationshipType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Parent [parent=" + parent + ", type=" + type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( parent == null ) ? 0 : parent.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Parent other = ( Parent ) obj;
        if ( parent == null ) {
            if ( other.parent != null ) return false;
        } else if ( !parent.equals( other.parent ) ) return false;
        if ( type != other.type ) return false;
        return true;
    }

}
