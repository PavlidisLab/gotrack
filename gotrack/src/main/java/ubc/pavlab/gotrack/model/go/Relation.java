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

/**
 * Represents a relationship to a Gene Ontology Term.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Relation<T> {
    private final T relation;
    private final RelationshipType type;

    public Relation( T relation, RelationshipType type ) {
        super();
        this.relation = relation;
        this.type = type;
    }

    public T getRelation() {
        return relation;
    }

    public RelationshipType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Relation [relation=" + relation.toString() + ", type=" + type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( relation == null ) ? 0 : relation.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Relation other = ( Relation ) obj;
        if ( relation == null ) {
            if ( other.relation != null ) return false;
        } else if ( !relation.equals( other.relation ) ) return false;
        if ( type != other.type ) return false;
        return true;
    }

}
