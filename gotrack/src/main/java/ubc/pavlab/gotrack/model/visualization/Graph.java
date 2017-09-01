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

package ubc.pavlab.gotrack.model.visualization;

import com.google.common.collect.Maps;
import org.json.JSONArray;
import org.json.JSONObject;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.go.Relation;

import java.util.*;

/**
 * Represents a collection of nodes and edges
 *
 * @author mjacobson
 */
public class Graph {
    private final Map<Integer, Node> nodes;
    private final Collection<Edge> edges;

    /**
     * Creates a graph object with required information to create an ancestry DAG in the front-end.
     * *
     *
     * @param term term
     * @return ancestry graph
     */
    public static Graph fromGO( GeneOntologyTerm term ) {
        if ( term == null ) {
            return new Graph();
        }

        return Graph.fromGO( Collections.singleton( term ) );
    }

    public static Graph fromGO( Collection<GeneOntologyTerm> terms ) {

        if ( terms == null || terms.isEmpty() ) {
            return new Graph();
        }

        Map<Integer, Node> nodes = Maps.newLinkedHashMap();
        Set<Edge> edges = new LinkedHashSet<>();

        for ( GeneOntologyTerm t : terms ) {

            Queue<GeneOntologyTerm> termQ = new LinkedList<>();
            termQ.add( t );

            while (!termQ.isEmpty()) {
                GeneOntologyTerm term = termQ.remove();
                Node node = new Node( term.getId(), term.getName() );
                nodes.put(node.getId(), node );

                // Sort for consistent graph layouts in the front-end
                List<Relation<GeneOntologyTerm>> sortedParents = new ArrayList<>( term.getParents() );
                Collections.sort( sortedParents, new Comparator<Relation<GeneOntologyTerm>>() {
                    @Override
                    public int compare( Relation<GeneOntologyTerm> o1, Relation<GeneOntologyTerm> o2 ) {
                        return o1.getRelation().compareTo( o2.getRelation() );
                    }
                } );

                for ( Relation<GeneOntologyTerm> p : sortedParents ) {
                    edges.add( new Edge( term.getId(), p.getRelation().getId(), p.getType() ) );
                    if ( !nodes.containsKey( p.getRelation().getId() ) ) {
                        termQ.add( p.getRelation() );
                    }
                }

            }
        }

        // Apply class for base terms
        for ( GeneOntologyTerm term : terms ) {
            nodes.get( term.getId() ).addClass( "base" );
        }

        return new Graph( nodes, edges );
    }

    public static Graph fromGODiff( GeneOntologyTerm term1, GeneOntologyTerm term2 ) {
        return Graph.fromGODiff( Collections.singleton( term1 ), Collections.singleton( term2 ) );
    }

    /**
     * Collects the difference between two term sets. Uses the first set as the base and
     * annotates changes to it by adding either 'added' or 'deleted' classes to nodes/edges.
     *
     * @param termSet1
     * @param termSet2
     * @return Annotated modified graph of termSet1 including nodes/edges that exist only in termSet2
     */
    public static Graph fromGODiff( Collection<GeneOntologyTerm> termSet1, Collection<GeneOntologyTerm> termSet2 ) {

        if ( termSet1 == null || termSet2 == null ) {
            return new Graph();
        }

        if ( termSet1.isEmpty() && termSet2.isEmpty() ) {
            return new Graph();
        }

        Graph baseGraph = Graph.fromGO( termSet1 );
        Graph newGraph = Graph.fromGO( termSet2 );

        return Graph.fromGraphDiff( baseGraph, newGraph );
    }

    public static Graph fromGraphDiff( Graph baseGraph, Graph newGraph ) {

        boolean changed = false;

        // Find new nodes
        for ( Node n : newGraph.getNodes() ) {
            if ( !baseGraph.getNodes().contains( n ) ) {
                n.addClass( "added" );
                baseGraph.nodes.put( n.getId(), n );
                changed = true;
            }
        }

        // Find deleted nodes
        for ( Node n : baseGraph.getNodes() ) {
            if ( !newGraph.getNodes().contains( n ) ) {
                n.addClass( "deleted" );
                changed = true;
            }
        }

        // Find new edges
        for ( Edge e : newGraph.getEdges() ) {
            if ( !baseGraph.getEdges().contains( e ) ) {
                e.addClass( "added" );
                baseGraph.edges.add( e );
                changed = true;
            }
        }

        // Find deleted edges
        for ( Edge e : baseGraph.getEdges() ) {
            if ( !newGraph.getEdges().contains( e ) ) {
                e.addClass( "deleted" );
                changed = true;
            }
        }

        if ( !changed ) {
            return null;
        }

        return baseGraph;
    }

    private Graph( Map<Integer, Node> nodes, Collection<Edge> edges ) {
        super();
        this.nodes = nodes;
        this.edges = edges;
    }

    public Graph() {
        this( Maps.<Integer, Node>newLinkedHashMap(), new LinkedHashSet<Edge>() );
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Collection<Edge> getEdges() {
        return edges;
    }


    public String getJsonString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put( "nodes", new JSONArray(this.getNodes()) );
        jsonObject.put( "edges", new JSONArray(this.getEdges()) );
        return jsonObject.toString();
    }
}
