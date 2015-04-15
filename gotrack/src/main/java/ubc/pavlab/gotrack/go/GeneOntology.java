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

package ubc.pavlab.gotrack.go;

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

    private TIntObjectHashMap<Term> termMap;

    // private Map<String, Term> termMapLarge = new HashMap<>();

    public GeneOntology() {
    }

    public GeneOntology( Set<Term> goTerms, Set<Relationship> goAdjacency ) {
        termMap = new TIntObjectHashMap<>();

        for ( Term t : goTerms ) {
            termMap.put( t.getId(), t );
        }

        for ( Relationship rel : goAdjacency ) {

            Term term = termMap.get( rel.getChildId() );

            if ( term == null ) {
                log.warn( "Relationship (" + rel + ") child not found in term map!" );
            }

            Term parentTerm = termMap.get( rel.getParentId() );
            if ( parentTerm == null ) {
                log.warn( "Relationship (" + rel + ") parent not found in term map!" );
            }

            term.addParent( new Link( parentTerm, rel.getType() ) );

        }
    }

    public Term getTerm( String goid ) {
        int id = Integer.parseInt( goid.substring( goid.length() - 7 ) );
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
        for ( GeneOntologyTerm go : new HashSet<GeneOntologyTerm>( goSet ) ) {
            allPropagations.add( go );
            Term term = termMap.get( convertGOId( go.getGoId() ) );
            if ( term != null ) {
                Set<Link> propagations = getAncestors( term, includePartOf );
                for ( Link termLink : propagations ) {
                    Term t = termLink.getLink();
                    GeneOntologyTerm propagatedGO = new GeneOntologyTerm( convertGOId( t.getId() ), t.getName(), t
                            .getAspect().toString() );
                    allPropagations.add( propagatedGO );

                }
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

            Term term = termMap.get( convertGOId( goEntry.getKey().getGoId() ) );
            if ( term != null ) {
                Set<Link> propagations = getAncestors( term, includePartOf );
                for ( Link termLink : propagations ) {
                    Term t = termLink.getLink();
                    GeneOntologyTerm propagatedGO = new GeneOntologyTerm( convertGOId( t.getId() ), t.getName(), t
                            .getAspect().toString() );
                    evidence = propagatedAnnotations.get( propagatedGO );
                    if ( evidence == null ) {
                        evidence = new HashSet<>();
                        propagatedAnnotations.put( propagatedGO, evidence );
                    }

                    evidence.addAll( ev );
                }
            } else {
                log.warn( "Could not find GO Term (" + goEntry.getKey() + ") while propagating!" );
            }
        }

        return propagatedAnnotations;
    }

    public Set<Link> getAncestors( String goid, boolean includePartOf ) throws IllegalArgumentException {
        return getAncestors( convertGOId( goid ), includePartOf );
    }

    public Set<Link> getAncestors( int id, boolean includePartOf ) {
        return getAncestors( termMap.get( id ), includePartOf );
    }

    private Set<Link> getAncestors( Term t, boolean includePartOf ) {
        Set<Link> ancestors = new HashSet<>();

        Set<Link> parents = getParents( t );

        for ( Link parent : parents ) {

            if ( includePartOf || parent.getType().equals( RelationshipType.IS_A ) ) {
                ancestors.add( parent );
                ancestors.addAll( getAncestors( parent.getLink(), includePartOf ) );
            }

        }

        return Collections.unmodifiableSet( ancestors );

    }

    public Set<Link> getParents( String goid ) throws IllegalArgumentException {
        return getParents( convertGOId( goid ) );
    }

    public Set<Link> getParents( int id ) {
        return getParents( termMap.get( id ) );
    }

    private Set<Link> getParents( Term t ) {
        return t.getParents();
    }

}
