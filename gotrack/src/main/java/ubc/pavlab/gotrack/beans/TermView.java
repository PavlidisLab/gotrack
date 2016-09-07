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

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.StatsService;
import ubc.pavlab.gotrack.exception.TermNotFoundException;
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

/**
 * Backing bean for the term tracking functionality.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ViewScoped
public class TermView implements Serializable {

    private static final long serialVersionUID = 3768269202724289260L;

    private static final Logger log = Logger.getLogger( TermView.class );

    @Inject
    private Cache cache;

    @Inject
    private StatsService statsService;

    @Inject
    private AnnotationService annotationService;

    private String query;
    private GOEdition currentGOEdition;
    private GeneOntologyTerm currentTerm;
    private String definition;

    private Integer compareEditionId;
    private List<GOEdition> allGOEditions;

    // Data

    private Map<GOEdition, GeneOntologyTerm> trackedTerms = null;

    public TermView() {
        log.info( "TermView created" );
        // System.gc();
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
        log.info( "TermView postConstruct" );
    }

    /**
     * pre-render view
     * 
     * This is kept lightweight so that the page loads quickly and lazy loads the data using remote commands
     */
    public String init() throws TermNotFoundException {
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
            FacesContext.getCurrentInstance().addMessage( "betaMessage", new FacesMessage( FacesMessage.SEVERITY_WARN,
                    "This is the DEVELOPMENT version of GOTrack!", null ) );

        }

        // Get ordered map

        Map<GOEdition, GeneOntologyTerm> temp = cache.getTerm( query );

        if ( temp == null || temp.isEmpty() ) {
            // gene symbol not found
            throw new TermNotFoundException();
            /*
             * FacesContext facesContext = FacesContext.getCurrentInstance(); NavigationHandler navigationHandler =
             * facesContext.getApplication().getNavigationHandler(); navigationHandler.handleNavigation( facesContext,
             * null, "error400?faces-redirect=true" );
             */
        } else {

            List<GOEdition> sortedKeySet = new ArrayList<>( temp.keySet() );
            Collections.sort( sortedKeySet );

            trackedTerms = new LinkedHashMap<>();

            for ( GOEdition goEdition : sortedKeySet ) {
                GeneOntologyTerm t = temp.get( goEdition );
                trackedTerms.put( goEdition, t );
            }

            currentTerm = trackedTerms.get( currentGOEdition );
            allGOEditions = new ArrayList<>();
            for ( Entry<GOEdition, GeneOntologyTerm> entry : trackedTerms.entrySet() ) {
                if ( entry.getValue() != null ) {
                    allGOEditions.add( entry.getKey() );
                }
            }
            Collections.sort( allGOEditions );

            definition = cache.getCurrentDefinition( currentTerm );

            statsService.countTermHit( currentTerm );

        }

        return null;
    }

    /**
     * entry point to fetch data to create the most current ancestry DAG
     */
    public void fetchDAGData() {
        // make some data for cyto.js

        Graph eles = calcElements( currentTerm );

        RequestContext.getCurrentInstance().addCallbackParam( "cyto_graph", new Gson().toJson( eles ) );

        RequestContext.getCurrentInstance().addCallbackParam( "current_id", currentTerm.getId() );
    }

    private Map<String, Object> createHCCallbackParamMap( String title, String yLabel, String xLabel, Integer min,
            Integer max, ChartValues chart ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", true );
        hcGsonMap.put( "title", title );
        hcGsonMap.put( "yLabel", yLabel );
        hcGsonMap.put( "xLabel", xLabel );
        hcGsonMap.put( "min", min );
        hcGsonMap.put( "max", max );
        hcGsonMap.put( "data", chart );
        return hcGsonMap;
    }

    /**
     * Entry point to fetch data for the overview gantt chart.
     */
    public void fetchOverviewChart() {
        // Going to need this
        Map<Long, Integer> dateToGOEditionId = new HashMap<>();
        Map<Long, String[]> dateToNameChange = new HashMap<>();

        // Create the 'did this term exist' chart
        ChartValues existChart = new ChartValues();
        Series existSeries = new Series( "Existence" );
        Series structureSeries = new Series( "Structure Change" );
        Series nameChange = new Series( "Name Change" );
        Graph prevEles = null;
        String prevName = null;
        for ( Entry<GOEdition, GeneOntologyTerm> entry : trackedTerms.entrySet() ) {
            GeneOntologyTerm t = entry.getValue();
            existSeries.addDataPoint( entry.getKey().getDate().getTime(), t != null ? 1 : 0 );
            dateToGOEditionId.put( entry.getKey().getDate().getTime(), entry.getKey().getId() );
            if ( t != null ) {

                Graph eles = calcElements( t );

                if ( prevEles != null ) {
                    Map<String, Graph> diff = graphDiff( prevEles, eles );
                    structureSeries.addDataPoint( entry.getKey().getDate().getTime(), diff != null ? 1 : 0 );
                } else {
                    structureSeries.addDataPoint( entry.getKey().getDate().getTime(), -1 );
                }

                if ( prevName != null ) {
                    int changed = t.getName().equals( prevName ) ? 0 : 1;
                    nameChange.addDataPoint( entry.getKey().getDate().getTime(), changed );
                    if ( changed == 1 ) {
                        dateToNameChange.put( entry.getKey().getDate().getTime(),
                                new String[] { prevName, t.getName() } );
                    }

                } else {
                    nameChange.addDataPoint( entry.getKey().getDate().getTime(), -1 );
                }

                prevEles = eles;
                prevName = t.getName();

            } else {
                structureSeries.addDataPoint( entry.getKey().getDate().getTime(), -1 );
                nameChange.addDataPoint( entry.getKey().getDate().getTime(), -1 );
            }
        }

        existChart.addSeries( existSeries );
        existChart.addSeries( structureSeries );
        existChart.addSeries( nameChange );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( "Overview of " + query + " vs Time",
                "Exists", "Date", 0, 2, existChart );

        hcGsonMap.put( "dateToGOEditionId", dateToGOEditionId );
        hcGsonMap.put( "dateToNameChange", dateToNameChange );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_overview", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Entry point to fetch data for the Gene chart which shows counts of genes with this term annotated
     * to it over time (both directly and through propagation)
     */
    public void fetchGeneChart() {
        // Create the 'Gene Count' chart
        Collection<Species> species = cache.getSpeciesList();
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
                            directTotalSeriesData.put( ed.getDate().getTime(),
                                    totalCnt == null ? cnt : totalCnt + cnt );
                        }

                    }
                }
            }
        }

        // Totals
        Series totalSeries = new Series( "Total" );
        for ( Entry<Long, Integer> entry : totalSeriesData.entrySet() ) {
            totalSeries.addDataPoint( entry.getKey(), entry.getValue() );
        }

        //TODO show totals or no? Ask Paul.
        //geneChart.addSeries( totalSeries );

        Series directTotalSeries = new Series( "Total Direct" );
        for ( Entry<Long, Integer> entry : directTotalSeriesData.entrySet() ) {
            directTotalSeries.addDataPoint( entry.getKey(), entry.getValue() );
        }

        //geneChart.addSeries( directTotalSeries );

        // Direct & Inferred

        for ( Species sp : series.keySet() ) {
            geneChart.addSeries( series.get( sp ) );
            geneChart.addSeries( directSeries.get( sp ) );
        }

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( "Genes Annotated to " + query + " vs Time",
                "Gene Count", "Date", null, null, geneChart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_gene", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point to fetch data for the creation of the evidence chart, which shows total counts of annotations over
     * time grouped by evidence category (not unique genes, this retrieves total counts of unique annotations from the
     * database)
     */
    public void fetchEvidenceChart() {
        // Make evidence charts

        Map<String, Map<Date, Integer>> evidenceCounts = annotationService.fetchCategoryCounts( currentTerm );

        ChartValues evidenceChart = new ChartValues();

        // Done in this manner to keep order and color of categories constant
        for ( String category : cache.getEvidenceCategories() ) {
            Series s = new Series( category );
            Map<Date, Integer> m = evidenceCounts.get( category );
            if ( m != null ) {
                for ( Entry<Date, Integer> entry : m.entrySet() ) {
                    s.addDataPoint( entry.getKey(), entry.getValue() );
                }
            }
            evidenceChart.addSeries( s );
        }

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( "Annotation Count of " + query + " vs Time",
                "Annotation Count", "Date", null, null, evidenceChart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_evidence", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Fetch data necessary to create a ancestry DAG for a given edition (and possibly overlay changes from previous
     * edition)
     */
    public void fetchGraph() {

        Integer goEditionId = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );

        Boolean showDiff = Boolean.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "showDiff" ) );

        GOEdition selectedEdition = cache.getGOEdition( goEditionId );

        GeneOntologyTerm selectedTerm = trackedTerms.get( selectedEdition );

        Graph graph = calcElements( selectedTerm );

        if ( showDiff ) {

            GOEdition compareEdition = null;

            for ( Iterator<GOEdition> iterator = trackedTerms.keySet().iterator(); iterator.hasNext(); ) {
                GOEdition goEdition = iterator.next();

                if ( goEdition.equals( selectedEdition ) ) {
                    break;
                }

                compareEdition = goEdition;

            }

            if ( compareEdition != null ) {

                Graph oldGraph = calcElements( trackedTerms.get( compareEdition ) );

                Map<String, Graph> diff = graphDiff( oldGraph, graph );
                RequestContext.getCurrentInstance().addCallbackParam( "vis_diff", new Gson().toJson( diff ) );
                // if there is a difference then we want to graph the old one and draw over the changes,
                // if there is no difference then we just graph the new one
                if ( diff != null ) {
                    graph = oldGraph;
                }
            }
        }

        RequestContext.getCurrentInstance().addCallbackParam( "cyto_graph", new Gson().toJson( graph ) );

        RequestContext.getCurrentInstance().addCallbackParam( "current_id", currentTerm.getId() );

    }

    /**
     * Fetches comparison of the ancestry DAG of some edition to the most current one. Not used anywhere anymore.
     */
    public void fetchDiff() {

        GOEdition compareEdition = cache.getGOEdition( compareEditionId );

        Map<String, Graph> diff = graphDiff( calcElements( currentTerm ),
                calcElements( trackedTerms.get( compareEdition ) ) );

        RequestContext.getCurrentInstance().addCallbackParam( "vis_diff", new Gson().toJson( diff ) );

    }

    /**
     * Creates a graph object with required information to create an ancestry DAG using cyto.js in the front-end.
     * Essentially collects all nodes and edges in the ancestry chart.
     * 
     * @param t term
     * @return
     */
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
                if ( !discovered.contains( p.getRelation() ) ) {
                    termQ.add( p.getRelation() );
                }
            }

        }

        return new Graph( nodes, edges );

    }

    /**
     * Collects the difference between two charts
     * 
     * @param baseGraph
     * @param newGraph
     * @return Map of ['added', 'deleted'] -> Graph object containing either added or deleted nodes/edges
     */
    public Map<String, Graph> graphDiff( Graph baseGraph, Graph newGraph ) {

        Set<Node> newNodes = new HashSet<>();
        Set<Node> deletedNodes = new HashSet<>();
        Set<Edge> newEdges = new HashSet<>();
        Set<Edge> deletedEdges = new HashSet<>();

        boolean changed = false;

        // Find new nodes
        for ( Node n : newGraph.getNodes() ) {
            if ( !baseGraph.getNodes().contains( n ) ) {
                newNodes.add( n );
                changed = true;
            }
        }

        // Find deleted nodes
        for ( Node n : baseGraph.getNodes() ) {
            if ( !newGraph.getNodes().contains( n ) ) {
                deletedNodes.add( n );
                changed = true;
            }
        }

        // Find new edges
        for ( Edge n : newGraph.getEdges() ) {
            if ( !baseGraph.getEdges().contains( n ) ) {
                newEdges.add( n );
                changed = true;
            }
        }

        // Find deleted edges
        for ( Edge n : baseGraph.getEdges() ) {
            if ( !newGraph.getEdges().contains( n ) ) {
                deletedEdges.add( n );
                changed = true;
            }
        }

        if ( !changed ) {
            return null;
        }

        Map<String, Graph> results = new HashMap<>();
        results.put( "added", new Graph( newNodes, newEdges ) );
        results.put( "deleted", new Graph( deletedNodes, deletedEdges ) );

        return results;

    }

    public GeneOntologyTerm getCurrentTerm() {
        return currentTerm;
    }

    public String getDefinition() {
        return definition;
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

}
