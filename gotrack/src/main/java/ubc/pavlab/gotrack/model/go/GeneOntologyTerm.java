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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import gnu.trove.set.hash.THashSet;
import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.dto.GOTermDTO;

/**
 * Represents a node in a Gene Ontology. The instances of this class may be shared between threads (sessions).
 * Two instances of this class with the same ID will be equal but may not both represent the same instance in time and
 * thus might have different metadata.
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
    private Set<Relation<GeneOntologyTerm>> relations = new THashSet<>();
    private Set<Relation<GeneOntologyTerm>> children = new THashSet<>();

    /**
     * Make child/parent sets immutable
     */
    public void freeze() {
        this.relations = ImmutableSet.copyOf( this.relations );
        this.children = ImmutableSet.copyOf( this.children );
    }

    /**
     * @param goid GO id (ex. 'GO:0000001')
     * @return id portion of the GO Id (ex. 1)
     */
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

    /**
     * Create Term from dto
     */
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
        freeze();
    }

    /**
     * USE WITH CAUTION, SHOULD NOT BE ALLOWED TO MINGLE WITH 'REAL' TERMS
     */
    public GeneOntologyTerm( String goId ) {

        this.id = convertGOId( goId );
        this.goId = goId;
        this.name = "";
        this.aspect = null;
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

    public Set<Relation<GeneOntologyTerm>> getParents() {
        return relations;
    }

    public Set<Relation<GeneOntologyTerm>> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return "GeneOntologyTerm [goId=" + goId + ", name=" + name + ", aspect=" + aspect + "]"; //", parents=" + relations + ", children=" + children + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    /*
     * Keep in mind that terms can be equal to each other across edition (as long as they have the same ID).
     * These terms, however, will most likely not have equivalent information.
     * 
     * This essentially means that Terms represent the same ontological node but may not represent the same instance in
     * time.
     */
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
