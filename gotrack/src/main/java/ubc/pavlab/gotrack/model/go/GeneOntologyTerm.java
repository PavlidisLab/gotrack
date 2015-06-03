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

import gnu.trove.set.hash.THashSet;

import java.util.HashSet;
import java.util.Set;

import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.dto.GOTermDTO;

import com.google.common.collect.ImmutableSet;

/**
 * The instances of this class may be shared between threads (sessions)
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntologyTerm implements Comparable<GeneOntologyTerm> {

    private final int id;
    private final String goId;
    private final String name;
    private final Aspect aspect;
    private final boolean obsolete = false;
    private Set<Parent> parents = new THashSet<>();

    public void freezeParents() {
        this.parents = ImmutableSet.copyOf( this.parents );
        // this.parents = Collections.unmodifiableSet( this.parents );
    }

    private int convertGOId( String goId ) {
        if ( goId.startsWith( "GO:" ) ) {
            int id;
            try {
                id = Integer.parseInt( goId.substring( goId.length() - 7 ) );
            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                throw new IllegalArgumentException( "Gene Ontology ID (" + goId + ") not in correct format." );
            }
            return id;
        } else {
            throw new IllegalArgumentException( "Gene Ontology ID (" + goId + ") not in correct format." );
        }
    }

    public GeneOntologyTerm( GOTermDTO dto ) {

        this.id = convertGOId( dto.getGoId() );
        this.goId = dto.getGoId();
        this.name = dto.getName();
        this.aspect = Aspect.valueOf( dto.getAspect() );
    }

    /**
     * Used for when an edition has been wrongly connected with a go_edition. In this situation an annotated term may
     * not be found within the given Ontology edition. What we do in this case is create a solitary term to represent it
     * in any analysis this term however will have no relationships to others (ie no parents/children)
     * 
     * @param goId
     * @param name
     * @param aspect
     */
    public GeneOntologyTerm( GeneOntologyTerm t ) {

        this.id = t.getId();
        this.goId = t.getGoId();
        this.name = t.getName();
        this.aspect = t.getAspect();
        this.parents = ImmutableSet.copyOf( new HashSet<Parent>() );
    }

    public int getId() {
        return id;
    }

    public String getGoId() {
        return goId;
    }

    public String getName() {
        return name;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public Set<Parent> getParents() {
        return parents;
    }

    @Override
    public String toString() {
        return "GOTerm [goId=" + goId + ", name=" + name + ", aspect=" + aspect + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GeneOntologyTerm other = ( GeneOntologyTerm ) obj;
        if ( id != other.id ) return false;
        return true;
    }

    @Override
    public int compareTo( GeneOntologyTerm o ) {
        // TODO Auto-generated method stub
        return this.getGoId().compareTo( o.getGoId() );
    }

}
