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

package ubc.pavlab.gotrack.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;

import ubc.pavlab.gotrack.exception.GeneNotFoundException;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.GOEdition;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.cytoscape.Edge;
import ubc.pavlab.gotrack.model.cytoscape.Graph;
import ubc.pavlab.gotrack.model.cytoscape.Node;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.go.Relation;

import com.google.gson.Gson;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class TermView {

    private static final Logger log = Logger.getLogger( TermView.class );

    @ManagedProperty("#{cache}")
    private Cache cache;

    private String query;
    private GOEdition currentGOEdition;
    private GeneOntologyTerm currentTerm;
    private Map<GOEdition, GeneOntologyTerm> trackedTerms = null;
    private Integer compareEditionId;
    private List<GOEdition> allGOEditions;

    public TermView() {
        log.info( "TermView created" );
        // System.gc();
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
        log.info( "TermView postConstruct" );
    }

    public String init() throws GeneNotFoundException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "TermView init" );

        currentGOEdition = cache.getCurrentGOEdition();

        if ( query == null ) {
            trackedTerms = null;
            return null;
        }

        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            // FacesContext.getCurrentInstance()
            // .addMessage(
            // "betaMessage",
            // new FacesMessage( FacesMessage.SEVERITY_WARN,
            // "This is the DEVELOPMENT version of GOTrack!", null ) );
            FacesContext.getCurrentInstance().addMessage( "betaMessage",
                    new FacesMessage( FacesMessage.SEVERITY_WARN, "This page is Under Construction!", null ) );

        }

        trackedTerms = cache.getTerm( query );

        if ( trackedTerms == null || trackedTerms.isEmpty() ) {
            // gene symbol not found
            throw new GeneNotFoundException();
            /*
             * FacesContext facesContext = FacesContext.getCurrentInstance(); NavigationHandler navigationHandler =
             * facesContext.getApplication().getNavigationHandler(); navigationHandler.handleNavigation( facesContext,
             * null, "error400?faces-redirect=true" );
             */
        } else {
            log.info( trackedTerms.size() );

            currentTerm = trackedTerms.get( currentGOEdition );
            allGOEditions = new ArrayList<>();
            for ( Entry<GOEdition, GeneOntologyTerm> entry : trackedTerms.entrySet() ) {
                if ( entry.getValue() != null ) {
                    allGOEditions.add( entry.getKey() );
                }
            }
            Collections.sort( allGOEditions );

        }

        return null;
    }

    public void fetchCharts() {

        // Going to need this
        Collection<Species> species = cache.getSpeciesList();
        Map<Long, Integer> dateToEdition = new HashMap<>();

        // Create the 'did this term exist' chart
        ChartValues existChart = new ChartValues();
        Series existSeries = new Series( query );
        for ( Entry<GOEdition, GeneOntologyTerm> entry : trackedTerms.entrySet() ) {
            GeneOntologyTerm t = entry.getValue();
            existSeries.addDataPoint( entry.getKey().getDate().getTime(), t != null ? 1 : 0 );
            dateToEdition.put( entry.getKey().getDate().getTime(), entry.getKey().getEdition() );
        }

        existChart.addSeries( existSeries );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_exist_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_exist_title", "Existence of " + query + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_exist_ylabel", "Exists" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_exist_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_exist_data", existChart );

        // Create the 'Gene Count' chart
        ChartValues geneChart = new ChartValues();
        Map<Species, Series> series = new HashMap<>();
        Map<Species, Series> directSeries = new HashMap<>();
        Map<Long, Integer> totalSeriesData = new HashMap<>();
        Map<Long, Integer> directTotalSeriesData = new HashMap<>();
        for ( Species sp : species ) {

            Collection<Edition> eds = cache.getAllEditions( sp.getId() );

            if ( eds != null ) {

                Series s = new Series( sp.getScientificName() );
                Series s2 = new Series( sp.getScientificName() + " Direct" );
                series.put( sp, s );
                directSeries.put( sp, s2 );
                for ( Edition ed : eds ) {
                    GeneOntologyTerm t = trackedTerms.get( ed.getGoEdition() );
                    if ( t != null ) {
                        // If term existed
                        if ( cache.getAggregates( sp.getId(), ed ) != null ) {
                            // if this returns null it means the edition has no data in it
                            // most likely missing data

                            // Inferred annotations

                            Integer cnt = cache.getInferredAnnotationCount( sp.getId(), ed, t );
                            cnt = ( cnt == null ) ? 0 : cnt;
                            s.addDataPoint( ed.getDate(), cnt );

                            // Total Inferred

                            Integer totalCnt = totalSeriesData.get( ed.getDate().getTime() );
                            totalSeriesData.put( ed.getDate().getTime(), totalCnt == null ? cnt : totalCnt + cnt );

                            // Direct annotations

                            cnt = cache.getDirectAnnotationCount( sp.getId(), ed, t );
                            cnt = ( cnt == null ) ? 0 : cnt;
                            s2.addDataPoint( ed.getDate(), cnt );

                            // Total Direct

                            totalCnt = directTotalSeriesData.get( ed.getDate().getTime() );
                            directTotalSeriesData.put( ed.getDate().getTime(), totalCnt == null ? cnt : totalCnt + cnt );
                        }

                    }
                }
            }
        }

        // Inferred
        for ( Series s : series.values() ) {
            geneChart.addSeries( s );
        }

        Series totalSeries = new Series( "Total" );
        for ( Entry<Long, Integer> entry : totalSeriesData.entrySet() ) {
            totalSeries.addDataPoint( entry.getKey(), entry.getValue() );
        }

        geneChart.addSeries( totalSeries );

        // Direct
        for ( Series s : directSeries.values() ) {
            geneChart.addSeries( s );
        }

        Series directTotalSeries = new Series( "Total Direct" );
        for ( Entry<Long, Integer> entry : directTotalSeriesData.entrySet() ) {
            directTotalSeries.addDataPoint( entry.getKey(), entry.getValue() );
        }

        geneChart.addSeries( directTotalSeries );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_gene_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_gene_title",
                "Gene Annotation Count of " + query + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_gene_ylabel", "Gene Count" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_gene_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_gene_data", geneChart );

        // make some data for vis.js

        Graph eles = calcElements( currentTerm );

        RequestContext.getCurrentInstance().addCallbackParam( "cyto_graph", new Gson().toJson( eles ) );

        RequestContext.getCurrentInstance().addCallbackParam( "current_id", currentTerm.getId() );

        RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( dateToEdition ) );

    }

    public void fetchGraph() {

        Integer goEditionId = Integer.valueOf( FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get( "edition" ) );

        GOEdition selectedEdition = cache.getGOEdition( goEditionId );

        GeneOntologyTerm selectedTerm = trackedTerms.get( selectedEdition );

        Graph graph = calcElements( selectedTerm );

        RequestContext.getCurrentInstance().addCallbackParam( "cyto_graph", new Gson().toJson( graph ) );

    }

    public void fetchDiff() {

        GOEdition compareEdition = cache.getGOEdition( compareEditionId );

        Map<String, Graph> diff = graphDiff( calcElements( currentTerm ),
                calcElements( trackedTerms.get( compareEdition ) ) );

        RequestContext.getCurrentInstance().addCallbackParam( "vis_diff", new Gson().toJson( diff ) );

    }

    private Graph calcElements( GeneOntologyTerm t ) {

        if ( t == null ) {
            return new Graph();
        }

        Set<Node> nodes = new LinkedHashSet<>();
        Set<Edge> edges = new LinkedHashSet<>();

        Set<GeneOntologyTerm> discovered = new HashSet<>();

        Queue<GeneOntologyTerm> termQ = new LinkedList<>();
        termQ.add( t );

        while ( !termQ.isEmpty() ) {
            GeneOntologyTerm term = termQ.remove();

            nodes.add( new Node( term.getId(), term.getName() ) );
            discovered.add( term );

            for ( Relation p : term.getParents() ) {
                edges.add( new Edge( term.getId(), p.getRelation().getId(), p.getType() ) );
                if ( !discovered.contains( p.getRelation() ) ) {
                    termQ.add( p.getRelation() );
                }
            }

        }

        return new Graph( nodes, edges );

    }

    public Map<String, Graph> graphDiff( Graph baseGraph, Graph newGraph ) {

        Set<Node> newNodes = new HashSet<>();
        Set<Node> deletedNodes = new HashSet<>();
        Set<Edge> newEdges = new HashSet<>();
        Set<Edge> deletedEdges = new HashSet<>();

        // Find new nodes
        for ( Node n : newGraph.getNodes() ) {
            if ( !baseGraph.getNodes().contains( n ) ) {
                newNodes.add( n );
            }
        }

        // Find deleted nodes
        for ( Node n : baseGraph.getNodes() ) {
            if ( !newGraph.getNodes().contains( n ) ) {
                deletedNodes.add( n );
            }
        }

        // Find new edges
        for ( Edge n : newGraph.getEdges() ) {
            if ( !baseGraph.getEdges().contains( n ) ) {
                newEdges.add( n );
            }
        }

        // Find deleted edges
        for ( Edge n : baseGraph.getEdges() ) {
            if ( !newGraph.getEdges().contains( n ) ) {
                deletedEdges.add( n );
            }
        }

        Map<String, Graph> results = new HashMap<>();
        results.put( "added", new Graph( newNodes, newEdges ) );
        results.put( "deleted", new Graph( deletedNodes, deletedEdges ) );

        return results;

    }

    public GeneOntologyTerm getCurrentTerm() {
        return currentTerm;
    }

    public Integer getCompareEditionId() {
        return compareEditionId;
    }

    public void setCompareEditionId( Integer compareEditionId ) {
        this.compareEditionId = compareEditionId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public Map<GOEdition, GeneOntologyTerm> getTrackedTerms() {
        return trackedTerms;
    }

    public List<GOEdition> getAllGOEditions() {
        return allGOEditions;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

}
