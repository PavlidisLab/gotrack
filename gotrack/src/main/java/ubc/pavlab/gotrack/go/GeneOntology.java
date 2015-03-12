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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.Relationship;
import ubc.pavlab.gotrack.model.RelationshipType;

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

    public GeneOntology( FileInputStream i ) throws IOException {
        parseOBO( i );

    }

    public GeneOntology( Set<Relationship> relationships ) {
        parseAdjacencyList( relationships );

    }

    public Term getTerm( String goid ) {
        int id = Integer.parseInt( goid.substring( goid.length() - 7 ) );
        return termMap.get( id );

    }

    public int size() {
        // return termMapLarge.size();
        return termMap.size();
    }

    public void parseAdjacencyList( Set<Relationship> relationships ) {
        termMap = new TIntObjectHashMap<>();

        for ( Relationship rel : relationships ) {

            Term term = termMap.get( rel.getChildId() );
            if ( term == null ) {
                term = new Term( rel.getChildId() );
                termMap.put( rel.getChildId(), term );
            }
            if ( rel.getType() != null ) {
                if ( rel.getType().equals( RelationshipType.IS_A ) ) {// TODO only IS_A until I figure out how to store
                                                                      // the relationship data efficiently
                    Term parentTerm = termMap.get( rel.getParentId() );
                    if ( parentTerm == null ) {
                        parentTerm = new Term( rel.getParentId() );
                        termMap.put( rel.getParentId(), parentTerm );
                    }

                    term.addParent( parentTerm );
                }
            }

        }

    }

    public void parseOBO( FileInputStream i ) throws IOException {
        termMap = new TIntObjectHashMap<>();
        BufferedReader br = null;

        try {

            if ( i.available() == 0 ) {
                throw new IOException( "Stream contains no data." );
            }

            br = new BufferedReader( new InputStreamReader( new GZIPInputStream( i ) ) );

            Term currentNode = null;

            String line = "";
            boolean inTerm = false;
            while ( ( line = br.readLine() ) != null ) {
                if ( inTerm && line.equals( "" ) ) {
                    // term ends
                    inTerm = false;
                    currentNode = null;
                } else if ( inTerm ) {
                    // term continues
                    String[] tagValuePair = line.split( ": ", 2 );
                    String tag;
                    String value;
                    try {
                        tag = tagValuePair[0];
                        value = tagValuePair[1];
                    } catch ( Exception e ) {
                        log.error( line );
                        throw e;
                    }

                    switch ( tag ) {
                        case "id":
                            try {
                                int id = Integer.parseInt( value.substring( value.length() - 7 ) );
                                currentNode = termMap.get( id );
                                // currentNode = termMapLarge.get( value );
                                if ( currentNode == null ) {
                                    currentNode = new Term( id );
                                    termMap.put( id, currentNode );
                                    // termMapLarge.put( value, currentNode );
                                }
                            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                                log.warn( "ID: (" + value + ") in incorrect format" );
                                // Skip this term
                                inTerm = false;
                            }

                            break;
                        case "name":
                            currentNode.setName( value );
                            break;
                        case "namespace":
                            if ( value.matches( "(?i:.*proc.*)" ) ) {
                                currentNode.setAspect( Aspect.BP );
                            } else if ( value.matches( "(?i:.*func.*)" ) ) {
                                currentNode.setAspect( Aspect.MF );
                            } else if ( value.matches( "(?i:.*comp.*)" ) ) {
                                currentNode.setAspect( Aspect.CC );
                            }
                            break;
                        case "def":
                            // currentNode.setDefinition( tagValuePair[1].split( "\"" )[1] );
                            break;
                        case "is_a":
                            String parentID = value.split( " " )[0];

                            // Find parent in map
                            try {
                                int id = Integer.parseInt( parentID.substring( parentID.length() - 7 ) );

                                Term parentNode = termMap.get( id );
                                // Term parentNode = termMapLarge.get( parentID );
                                if ( parentNode == null ) {
                                    parentNode = new Term( id );
                                    termMap.put( id, parentNode );
                                    // termMapLarge.put( parentID, parentNode );
                                }
                                boolean success = currentNode.addParent( parentNode );

                                if ( !success ) {
                                    log.warn( "Failed to add parent (" + parentNode.getId() + ") to Node ("
                                            + currentNode.getId() + ")" );
                                }

                            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                                log.warn( "Parent ID: (" + parentID + ") in incorrect format" );
                            }

                            break;
                        case "relationship":
                            // values = tagValuePair[1].split( " " );
                            // if ( values[0].equals( "part_of" ) ) {
                            // currentNode.addParent( new Term.Relationship( values[1],
                            // Term.Relationship.RelationshipType.PART_OF ) );
                            // if ( !termMap.containsKey( values[1] ) ) {
                            // // parent exists in map
                            // parentNode = new Term( values[1] );
                            // termMap.put( values[1], parentNode );
                            // } else {
                            // parentNode = termMap.get( values[1] );
                            // }
                            // parentNode.addChild( new Term.Relationship( currentNode.getId(),
                            // Term.Relationship.RelationshipType.PART_OF ) );
                            // }
                            break;
                        case "is_obsolete":
                            currentNode.setObsolete( value.equals( "true" ) );
                            break;
                        default:
                            break;
                    }

                } else if ( !inTerm && line.equals( "[Term]" ) ) {
                    // term starts
                    inTerm = true;
                }

            }

        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } finally {
            br.close();
            i.close();
        }

    }

}
