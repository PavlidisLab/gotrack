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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import gnu.trove.map.hash.TIntObjectHashMap;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.GOEdition;

/**
 * Memory efficient Directed Acyclic Graph representing a Gene Ontology Structure
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntology {

    private static final Logger log = Logger.getLogger( GeneOntology.class );

    private final GOEdition edition;

    private TIntObjectHashMap<GeneOntologyTerm> termMap = new TIntObjectHashMap<>();

    // private LoadingCache<GeneOntologyTerm, ImmutableSet<GeneOntologyTerm>> ancestorsCache = CacheBuilder.newBuilder()
    // .maximumWeight( 10000 ).weigher( new Weigher<GeneOntologyTerm, ImmutableSet<GeneOntologyTerm>>() {
    // public int weigh( GeneOntologyTerm k, ImmutableSet<GeneOntologyTerm> g ) {
    // return g.size();
    // }
    // } ).build( new CacheLoader<GeneOntologyTerm, ImmutableSet<GeneOntologyTerm>>() {
    // public ImmutableSet<GeneOntologyTerm> load( GeneOntologyTerm key ) {
    // return ImmutableSet.copyOf( getAncestors( key, true ) );
    // }
    // } );
    //
    //
    // public void getCacheStats() {
    // // Need .recordStats() on CacheBuilder for these work
    // CacheStats s = ancestorsCache.stats();
    // log.info( "hitRate: " + s.hitRate() );
    // log.info( "averageLoadPenalty: " + s.averageLoadPenalty() );
    // log.info( "evictionCount: " + s.evictionCount() );
    // log.info( "hitCount: " + s.hitCount() );
    // }

    public GeneOntology( GOEdition edition ) {
        this.edition = edition;
    }

    /**
     * Return collection of all terms in this ontology
     */
    public Collection<GeneOntologyTerm> getAllTerms() {
        return termMap.valueCollection();
    }

    /**
     * Add term to this ontology
     */
    public void addTerm( GeneOntologyTerm t ) {
        if ( termMap.contains( t.getId() ) ) {
            throw new IllegalArgumentException( "Gene Ontology ID (" + t.getGoId() + ") already exists in ontology." );
        }
        GeneOntologyTerm a = termMap.put( t.getId(), t );

    }

    /**
     * Add a relationship between two terms that are already a part of this ontology
     * 
     * @param child child
     * @param parent parent of child
     * @param type type of relationship
     */
    public void addRelationship( String child, String parent, RelationshipType type ) {
        GeneOntologyTerm term = termMap.get( convertGOId( child ) );

        if ( term == null ) {
            log.warn( "Relationship (" + child + ") child not found in term map!" );
        }

        GeneOntologyTerm parentTerm = termMap.get( convertGOId( parent ) );
        if ( parentTerm == null ) {
            log.warn( "Relationship (" + parent + ") parent not found in term map!" );
        }

        Relation parentRelation = new Relation( parentTerm, type );
        Relation childRelation = new Relation( term, type );

        if ( term.getParents().contains( parentRelation ) || parentTerm.getChildren().contains( childRelation ) ) {
            throw new IllegalArgumentException(
                    "Relationship between " + child + " and " + parent + " already exists." );
        }

        term.getParents().add( parentRelation );
        parentTerm.getChildren().add( childRelation );
    }

    /**
     * Makes the parent/children sets of all terms in this ontology immutable.
     */
    public void freeze() {
        for ( GeneOntologyTerm t : termMap.valueCollection() ) {
            t.freeze();
        }
    }

    /**
     * Retrieve term by GO Id
     * 
     * @param goid GO Id
     * @return Term or null if non-existent / malformed id
     */
    public GeneOntologyTerm getTerm( String goid ) {
        try {
            int id = Integer.parseInt( goid.substring( goid.length() - 7 ) );
            return termMap.get( id );
        } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
            return null;
        }

    }

    /**
     * Retrieve term by Id
     * 
     * @param id Id
     * @return Term or null if non-existent
     */
    public GeneOntologyTerm getTerm( int id ) {
        return termMap.get( id );

    }

    /**
     * @return Number of terms in this ontology
     */
    public int size() {
        // return termMapLarge.size();
        return termMap.size();
    }

    /**
     * @param goid GO id (ex. 'GO:0000001')
     * @return id portion of the GO Id (ex. 1)
     */
    private int convertGOId( String goid ) {
        if ( goid.startsWith( "GO:" ) ) {
            int id;
            try {
                id = Integer.parseInt( goid.substring( goid.length() - 7 ) );
            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                throw new IllegalArgumentException( "Gene Ontology ID (" + goid + ") not in correct format." );
            }
            return id;
        } else {
            throw new IllegalArgumentException( "Gene Ontology ID (" + goid + ") not in correct format." );
        }
    }

    /**
     * @param id id (ex. 1)
     * @return GO Id created from id (ex. 'GO:0000001')
     */
    private String convertGOId( int id ) {
        return "GO:" + String.format( "%07d", id );
    }

    /**
     * @param goSet set of Terms
     * @return propagated set of terms including part of relationships
     */
    public Set<GeneOntologyTerm> propagate( Collection<GeneOntologyTerm> goSet ) {
        return propagate( goSet, true );
    }

    /**
     * @param goSet set of Terms
     * @param includePartOf whether or not to propagate over part of relationships
     * @return propagated set of terms
     */
    public Set<GeneOntologyTerm> propagate( Collection<GeneOntologyTerm> goSet, boolean includePartOf ) {
        Set<GeneOntologyTerm> allPropagations = new HashSet<>();
        Map<GeneOntologyTerm, Set<GeneOntologyTerm>> cache = new HashMap<>();
        for ( GeneOntologyTerm go : goSet ) {
            allPropagations.add( go );

            // allPropagations.addAll( ancestorsCache.getUnchecked( go ) );

            // TODO this cache method might be a really bad idea, check back later
            allPropagations.addAll( getAncestors( go, includePartOf, cache ) );

        }
        return Collections.unmodifiableSet( allPropagations );

    }

    /**
     * @param map Map of Term -> Set of annotations
     * @return propagated map of Term -> Set of annotations (adding annotations to all parents), propagation is done
     *         over part of relationships
     */
    public Map<GeneOntologyTerm, Set<Annotation>> propagateAnnotations( Map<GeneOntologyTerm, Set<Annotation>> map ) {
        return propagateAnnotations( map, true );
    }

    /**
     * @param goAnnotations Map of Term -> Set of annotations
     * @param includePartOf whether or not to propagate over part of relationships
     * @return propagated map of Term -> Set of annotations (adding annotations to all parents)
     */
    public Map<GeneOntologyTerm, Set<Annotation>> propagateAnnotations(
            Map<GeneOntologyTerm, Set<Annotation>> goAnnotations, boolean includePartOf ) {
        Map<GeneOntologyTerm, Set<Annotation>> propagatedAnnotations = new HashMap<>();
        for ( Entry<GeneOntologyTerm, Set<Annotation>> goEntry : goAnnotations.entrySet() ) {
            GeneOntologyTerm go = goEntry.getKey();
            Set<Annotation> ev = goEntry.getValue();
            // Add current terms annotations
            Set<Annotation> evidence = propagatedAnnotations.get( go );
            if ( evidence == null ) {
                evidence = new HashSet<>();
                propagatedAnnotations.put( go, evidence );
            }

            evidence.addAll( ev );

            Set<GeneOntologyTerm> propagations = getAncestors( go, includePartOf, null );
            for ( GeneOntologyTerm parent : propagations ) {
                evidence = propagatedAnnotations.get( parent );
                if ( evidence == null ) {
                    evidence = new HashSet<>();
                    propagatedAnnotations.put( parent, evidence );
                }

                evidence.addAll( ev );
            }

        }

        return propagatedAnnotations;
    }

    /**
     * @param goid GO Id
     * @param includePartOf whether or not to propagate over part of relationships
     * @param cache cache used to memoize ancestors
     * @return set of ancestors of Term with given GO Id
     * @throws IllegalArgumentException
     */
    public Set<GeneOntologyTerm> getAncestors( String goid, boolean includePartOf,
            Map<GeneOntologyTerm, Set<GeneOntologyTerm>> cache ) throws IllegalArgumentException {
        return getAncestors( convertGOId( goid ), includePartOf, cache );
    }

    /**
     * @param goid Id
     * @param includePartOf whether or not to propagate over part of relationships
     * @param cache cache used to memoize ancestors
     * @return set of ancestors of Term with given Id
     * @throws IllegalArgumentException
     */
    public Set<GeneOntologyTerm> getAncestors( int id, boolean includePartOf,
            Map<GeneOntologyTerm, Set<GeneOntologyTerm>> cache ) {
        return getAncestors( termMap.get( id ), includePartOf, cache );
    }

    /**
     * @param t Term
     * @param includePartOf whether or not to propagate over part of relationships
     * @param cache cache used to memoize ancestors
     * @return set of ancestors of given Term
     * @throws IllegalArgumentException
     */
    public Set<GeneOntologyTerm> getAncestors( GeneOntologyTerm t, boolean includePartOf,
            Map<GeneOntologyTerm, Set<GeneOntologyTerm>> cache ) {

        Set<GeneOntologyTerm> a;
        if ( cache != null && ( a = cache.get( t ) ) != null ) {
            return a;
        }

        Set<GeneOntologyTerm> ancestors = new HashSet<>();

        for ( Relation relation : getParents( t, includePartOf ) ) {

            ancestors.add( relation.getRelation() );
            // ancestors.addAll( ancestorsCache.getUnchecked( parent.getParent() ) );
            ancestors.addAll( getAncestors( relation.getRelation(), includePartOf, cache ) );

        }

        if ( cache != null ) {
            cache.put( t, ancestors );
        }

        return ancestors;

    }

    /**
     * @param goid GO Id
     * @param includePartOf whether or not to propagate over part of relationships
     * @return Set of relations of the Term with this GO Id
     * @throws IllegalArgumentException
     */
    public Set<Relation> getParents( String goid, boolean includePartOf ) throws IllegalArgumentException {
        return getParents( convertGOId( goid ), includePartOf );
    }

    /**
     * @param id Id
     * @param includePartOf whether or not to propagate over part of relationships
     * @return Set of relations of the Term with this Id
     */
    public Set<Relation> getParents( int id, boolean includePartOf ) {
        return getParents( termMap.get( id ), includePartOf );
    }

    /**
     * @param t Term
     * @param includePartOf whether or not to propagate over part of relationships
     * @return Set of relations of the given Term
     */
    private Set<Relation> getParents( GeneOntologyTerm t, boolean includePartOf ) {
        if ( includePartOf ) {
            return t.getParents();
        } else {
            Set<Relation> relations = new HashSet<>();
            for ( Relation p : t.getParents() ) {
                if ( p.getType().equals( RelationshipType.IS_A ) ) {
                    relations.add( p );
                }
            }

            return relations;

        }

    }

    /**
     * @return GO Edition of this ontology
     */
    public GOEdition getEdition() {
        return edition;
    }

    // private ImmutableCollection<GeneOntologyTerm> getParents2( GeneOntologyTerm t ) {
    // return t.getParents2();
    // }

}
