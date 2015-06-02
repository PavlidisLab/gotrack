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

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * Memory efficient Directed Acyclic Graph representing a Gene Ontology Structure
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntology {

    private static final Logger log = Logger.getLogger( GeneOntology.class );

    private TIntObjectHashMap<GeneOntologyTerm> termMap = new TIntObjectHashMap<>();

    // private Map<String, Term> termMapLarge = new HashMap<>();

    public GeneOntology() {
    }

    public void addTerm( GeneOntologyTerm t ) {
        termMap.put( t.getId(), t );
    }

    public void addRelationship( String child, String parent, RelationshipType type ) {
        GeneOntologyTerm term = termMap.get( convertGOId( child ) );

        if ( term == null ) {
            log.warn( "Relationship (" + child + ") child not found in term map!" );
        }

        GeneOntologyTerm parentTerm = termMap.get( convertGOId( parent ) );
        if ( parentTerm == null ) {
            log.warn( "Relationship (" + parent + ") parent not found in term map!" );
        }

        term.getParents().add( new Parent( parentTerm, type ) );
    }

    public void freeze() {
        for ( GeneOntologyTerm t : termMap.valueCollection() ) {
            t.freezeParents();
        }
    }

    public GeneOntologyTerm getTerm( String goid ) {
        int id = Integer.parseInt( goid.substring( goid.length() - 7 ) );
        return termMap.get( id );

    }

    public GeneOntologyTerm getTerm( int id ) {
        return termMap.get( id );

    }

    public int size() {
        // return termMapLarge.size();
        return termMap.size();
    }

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

    private String convertGOId( int id ) {
        return "GO:" + String.format( "%07d", id );
    }

    public Set<GeneOntologyTerm> propagate( Collection<GeneOntologyTerm> goSet ) {
        return propagate( goSet, true );
    }

    public Set<GeneOntologyTerm> propagate( Collection<GeneOntologyTerm> goSet, boolean includePartOf ) {
        Set<GeneOntologyTerm> allPropagations = new HashSet<>();
        for ( GeneOntologyTerm go : goSet ) {
            allPropagations.add( go );

            Set<Parent> propagations = getAncestors( go, includePartOf );
            for ( Parent parent : propagations ) {
                allPropagations.add( parent.getParent() );

            }

        }
        return Collections.unmodifiableSet( allPropagations );

    }

    public Map<GeneOntologyTerm, Set<EvidenceReference>> propagate(
            Map<GeneOntologyTerm, Set<EvidenceReference>> goAnnotations ) {
        return propagate( goAnnotations, true );
    }

    public Map<GeneOntologyTerm, Set<EvidenceReference>> propagate(
            Map<GeneOntologyTerm, Set<EvidenceReference>> goAnnotations, boolean includePartOf ) {

        Map<GeneOntologyTerm, Set<EvidenceReference>> propagatedAnnotations = new HashMap<>();
        for ( Entry<GeneOntologyTerm, Set<EvidenceReference>> goEntry : goAnnotations.entrySet() ) {
            GeneOntologyTerm go = goEntry.getKey();
            Set<EvidenceReference> ev = goEntry.getValue();
            // Add current terms annotations
            Set<EvidenceReference> evidence = propagatedAnnotations.get( go );
            if ( evidence == null ) {
                evidence = new HashSet<>();
                propagatedAnnotations.put( go, evidence );
            }

            evidence.addAll( ev );

            Set<Parent> propagations = getAncestors( go, includePartOf );
            for ( Parent parent : propagations ) {
                evidence = propagatedAnnotations.get( parent.getParent() );
                if ( evidence == null ) {
                    evidence = new HashSet<>();
                    propagatedAnnotations.put( parent.getParent(), evidence );
                }

                evidence.addAll( ev );
            }

        }

        return propagatedAnnotations;
    }

    public Set<Parent> getAncestors( String goid, boolean includePartOf ) throws IllegalArgumentException {
        return getAncestors( convertGOId( goid ), includePartOf );
    }

    public Set<Parent> getAncestors( int id, boolean includePartOf ) {
        return getAncestors( termMap.get( id ), includePartOf );
    }

    private Set<Parent> getAncestors( GeneOntologyTerm t, boolean includePartOf ) {
        Set<Parent> ancestors = new HashSet<>();

        Set<Parent> parents = getParents( t );

        for ( Parent parent : parents ) {

            if ( includePartOf || parent.getType().equals( RelationshipType.IS_A ) ) {
                ancestors.add( parent );
                ancestors.addAll( getAncestors( parent.getParent(), includePartOf ) );
            }

        }

        return Collections.unmodifiableSet( ancestors );

    }

    public Set<Parent> getParents( String goid ) throws IllegalArgumentException {
        return getParents( convertGOId( goid ) );
    }

    public Set<Parent> getParents( int id ) {
        return getParents( termMap.get( id ) );
    }

    private Set<Parent> getParents( GeneOntologyTerm t ) {
        return t.getParents();
    }

}
