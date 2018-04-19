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

import com.google.common.collect.ImmutableSet;
import gnu.trove.set.hash.THashSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.dto.GOTermDTO;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a node in a Gene Ontology. The instances of this class may be shared between threads (sessions).
 * Two instances of this class with the same ID will be equal but may not both represent the same instance in time and
 * thus might have different metadata.
 * <p>
 * This essentially means that Terms represent the same ontological node but may not represent the same instance in
 * time.
 *
 * @author mjacobson
 */
@Getter
@EqualsAndHashCode(of = "id")
@ToString(of = {"goId", "name", "aspect"})
public class GeneOntologyTerm implements Comparable<GeneOntologyTerm> {

    private final int id;
    private final String goId;
    private final String name;
    private final Aspect aspect;
    private final boolean obsolete = false;
    private Set<Relation<GeneOntologyTerm>> parents = new THashSet<>();
    private Set<GeneOntologyTerm> ancestors = null;
    private Set<Relation<GeneOntologyTerm>> children = new THashSet<>();

    /**
     * Make child/parent sets immutable
     */
    public void freeze() {
        this.parents = ImmutableSet.copyOf( this.parents );
        this.children = ImmutableSet.copyOf( this.children );
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
     * @param t Term
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

    public Stream<Relation<GeneOntologyTerm>> streamParents() {
        return streamParents( true );
    }

    public Stream<GeneOntologyTerm> streamAncestors() {
        return streamAncestors( true );
    }

    public Stream<Relation<GeneOntologyTerm>> streamChildren() {
        return streamChildren( true );
    }

    public Stream<GeneOntologyTerm> streamDescendants() {
        return streamDescendants( true );
    }

    Stream<Relation<GeneOntologyTerm>> streamParents( boolean includePartOf ) {
        if ( includePartOf ) {
            // Short Circuit
            return parents.stream();
        } else {
            return parents.stream().filter( r -> r.getType().equals( RelationshipType.IS_A ) );
        }
    }

    Stream<GeneOntologyTerm> streamAncestors( boolean includePartOf ) {
        if ( includePartOf && ancestors != null ) {
            return ancestors.stream();
        }

        Stream<GeneOntologyTerm> ancestorStream = streamParents( includePartOf ).flatMap( r -> r.getRelation().propagate( includePartOf ) );

        if ( includePartOf ) {
            // Only cache complete ancestors, incomplete will always be computed.
            ancestors = ancestorStream.collect( Collectors.toCollection( THashSet :: new ) );
            return ancestors.stream();
        } else {
            return ancestorStream;
        }

    }

    Stream<GeneOntologyTerm> propagate( boolean includePartOf ) {
        return Stream.concat( Stream.of( this ), streamAncestors( includePartOf ) );
    }

    Stream<Relation<GeneOntologyTerm>> streamChildren( boolean includePartOf ) {
        if ( includePartOf ) {
            // Short Circuit
            return children.stream();
        } else {
            return children.stream().filter( r -> r.getType().equals( RelationshipType.IS_A ) );
        }

    }

    Stream<GeneOntologyTerm> streamDescendants( boolean includePartOf ) {
        return streamChildren( includePartOf ).flatMap( r -> r.getRelation().propagateDown( includePartOf ) );
    }

    Stream<GeneOntologyTerm> propagateDown( boolean includePartOf ) {
        return Stream.concat( Stream.of( this ), streamDescendants( includePartOf ) );
    }

    @Override
    public int compareTo( GeneOntologyTerm o ) {
        // TODO Auto-generated method stub
        return this.getGoId().compareTo( o.getGoId() );
    }

    public static Stream<GeneOntologyTerm> propagate( Stream<GeneOntologyTerm> terms ) {
        return propagate( terms, true );
    }

    static Stream<GeneOntologyTerm> propagate( Stream<GeneOntologyTerm> terms, boolean includePartOf ) {
        return terms.flatMap( t -> t.propagate( includePartOf ) );
    }

    public static <A> Map<GeneOntologyTerm, Set<A>> propagateAnnotations( Stream<Map.Entry<GeneOntologyTerm, Set<A>>> termEntries ) {
        return propagateAnnotations( termEntries, true );
    }

    /**
     * Propagates terms and their annotations to parents terms
     *
     * @return Map of all propagated terms to their propagated annotations
     */
    static <A> Map<GeneOntologyTerm, Set<A>> propagateAnnotations( Stream<Map.Entry<GeneOntologyTerm, Set<A>>> termEntries, boolean includePartOf ) {
        Map<GeneOntologyTerm, Set<A>> propagatedAnnotations = new HashMap<>();
        termEntries.forEach( entry -> entry.getKey().propagate(includePartOf)
                .forEach( p -> propagatedAnnotations.computeIfAbsent( p, k -> new HashSet<>() ).addAll( entry.getValue() ) ) );

        return propagatedAnnotations;
    }


    /**
     * @param goId GO id (ex. 'GO:0000001')
     * @return id portion of the GO Id (ex. 1)
     */
    static int convertGOId( String goId ) {
        if ( goId.startsWith( "GO:" ) ) {
            int id;
            try {
                id = Integer.parseInt( goId.substring( goId.length() - 7 ) );
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new IllegalArgumentException( "Gene Ontology ID (" + goId + ") not in correct format." );
            }
            return id;
        } else {
            throw new IllegalArgumentException( "Gene Ontology ID (" + goId + ") not in correct format." );
        }
    }


}
