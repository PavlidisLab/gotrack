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

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.model.GOEdition;

import java.util.Collection;

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
    private TIntIntMap altMap = new TIntIntHashMap();

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
        termMap.put( t.getId(), t );

    }

    /**
     * Add a relationship between two terms that are already a part of this ontology
     * 
     * @param child child
     * @param parent parent of child
     * @param type type of relationship
     */
    public void addRelationship( String child, String parent, RelationshipType type ) {
        GeneOntologyTerm term = termMap.get( GeneOntologyTerm.convertGOId( child ) );

        if ( term == null ) {
            log.warn( "Relationship (" + child + ") child not found in term map!" );
        }

        GeneOntologyTerm parentTerm = termMap.get( GeneOntologyTerm.convertGOId( parent ) );
        if ( parentTerm == null ) {
            log.warn( "Relationship (" + parent + ") parent not found in term map!" );
        }

        addRelationship( term, parentTerm, type );
    }

    public void addRelationship( GeneOntologyTerm child, GeneOntologyTerm parent, RelationshipType type ) {
        Relation<GeneOntologyTerm> parentRelation = new Relation<>( parent, type );
        Relation<GeneOntologyTerm> childRelation = new Relation<>( child, type );

        if ( child.getParents().contains( parentRelation ) || parent.getChildren().contains( childRelation ) ) {
            throw new IllegalArgumentException(
                    "Relationship between " + child.getGoId() + " and " + parent.getGoId() + " already exists." );
        }

        child.getParents().add( parentRelation );
        parent.getChildren().add( childRelation );
    }

    public void addAlt( String alt, String primary ) {
        altMap.put( Integer.parseInt( alt.substring( alt.length() - 7 ) ),
                Integer.parseInt( primary.substring( primary.length() - 7 ) ) );
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
        if ( goid == null ) return null;
        try {
            int id = Integer.parseInt( goid.substring( goid.length() - 7 ) );
            GeneOntologyTerm res = getTerm( id );
            if ( res == null && altMap.containsKey( id ) ) {
                res = getTerm( altMap.get( id ) );
            }
            return res;
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
     * @return GO Edition of this ontology
     */
    public GOEdition getEdition() {
        return edition;
    }

    public int getId() {
        return edition.getId();
    }

    // private ImmutableCollection<GeneOntologyTerm> getParents2( GeneOntologyTerm t ) {
    // return t.getParents2();
    // }

}
