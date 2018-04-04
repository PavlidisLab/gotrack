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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.StatsService;
import ubc.pavlab.gotrack.exception.TermNotFoundException;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.GOEdition;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.visualization.Graph;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Backing bean for the term tracking functionality.
 *
 * @author mjacobson
 * @version $Id$
 */
@Log
@Named
@ViewScoped
public class TermView implements Serializable {

    private static final long serialVersionUID = 3768269202724289260L;

    @Inject
    private Cache cache;

    @Inject
    private StatsService statsService;

    @Inject
    private AnnotationService annotationService;

    @Inject
    private SessionManager session;

    @Getter
    @Setter
    private String query;

    @Getter
    @Setter
    private boolean allSpecies;

    @Getter
    private GeneOntologyTerm currentTerm;

    @Getter
    private String definition;

    @Getter
    private List<GOEdition> allGOEditions;

    // Data
    @Getter
    private Map<GOEdition, GeneOntologyTerm> trackedTerms = null;

    public TermView() {
        log.info( "TermView created" );
        // System.gc();
        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
                + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
        log.info( "TermView postConstruct" );
    }

    /**
     * pre-render view
     * <p>
     * This is kept lightweight so that the page loads quickly and lazy loads the data using remote commands
     */
    public String init() throws TermNotFoundException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "TermView init" );

        GOEdition currentGOEdition = cache.getCurrentGOEdition();

        if ( query == null ) {
            trackedTerms = null;
            return null;
        }

        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            FacesContext.getCurrentInstance().addMessage( "betaMessage", new FacesMessage( FacesMessage.SEVERITY_WARN,
                    "This is the DEVELOPMENT version of GOTrack!", "" ) );

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

            trackedTerms = temp.entrySet().stream().sorted( Entry.comparingByKey() )
                    .collect( LinkedHashMap::new, ( m, e ) -> m.put( e.getKey(), e.getValue() ), LinkedHashMap::putAll );

            currentTerm = trackedTerms.get( currentGOEdition );

            allGOEditions = trackedTerms.entrySet().stream()
                    .filter( ( e ) -> e.getValue() != null )
                    .map( Entry::getKey )
                    .sorted()
                    .collect( Collectors.toList() );

            definition = cache.getCurrentDefinition( currentTerm );

            statsService.countTermHit( currentTerm );

        }

        return null;
    }

    /**
     * entry point to fetch data to create the most current ancestry DAG
     */
    public void fetchDAGData() {

//        Collection<String> goids = Sets.newHashSet("GO:0005783","GO:0005886","GO:0005887","GO:0008021","GO:0009986","GO:0014069","GO:0017146","GO:0030054","GO:0030425","GO:0043005","GO:0043083","GO:0043195","GO:0043197","GO:0045202","GO:0045211","GO:0060076");


        Set<GeneOntologyTerm> terms = Sets.newHashSet();
        terms.add( currentTerm );

//        for ( String goid : goids ) {
//            GeneOntologyTerm t = cache.getCurrentTerm( goid );
//            if (t != null) {
//                terms.add( t );
//            }
//        }

        Graph eles = Graph.fromGO( terms );

        RequestContext.getCurrentInstance().addCallbackParam( "graph_data", eles.getJsonString() );
    }

    private Map<String, Object> createHCCallbackParamMap( ChartValues chart ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", true );
        hcGsonMap.put( "chart", chart );
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
        ChartValues existChart = new ChartValues( "History", "", "Date" );
        existChart.setSubtitle( currentTerm.getGoId() + " - " + currentTerm.getName() );
        existChart.setMin( 0 );
        existChart.setMax( 2 );

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

                Graph eles = Graph.fromGO( t );

                if ( prevEles != null ) {
                    Graph diff = Graph.fromGraphDiff( prevEles, eles );
                    structureSeries.addDataPoint( entry.getKey().getDate().getTime(), diff != null ? 1 : 0 );
                } else {
                    structureSeries.addDataPoint( entry.getKey().getDate().getTime(), -1 );
                }

                if ( prevName != null ) {
                    int changed = t.getName().equals( prevName ) ? 0 : 1;
                    nameChange.addDataPoint( entry.getKey().getDate().getTime(), changed );
                    if ( changed == 1 ) {
                        dateToNameChange.put( entry.getKey().getDate().getTime(),
                                new String[]{prevName, t.getName()} );
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

        // Order here is order in the chart, so it matters.
        existChart.addSeries( nameChange );
        existChart.addSeries( structureSeries );
        existChart.addSeries( existSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( existChart );

        hcGsonMap.put( "dateToGOEditionId", dateToGOEditionId );
        hcGsonMap.put( "dateToNameChange", dateToNameChange );
        hcGsonMap.put( "goId", currentTerm.getGoId() );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_overview", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Entry point to fetch data for the Gene chart which shows counts of genes with this term annotated
     * to it over time (both directly and through propagation)
     */
    public void fetchGeneChart() {
        // Create the 'Gene Count' chart
        Collection<Species> species = allSpecies ? cache.getSpeciesList() : Lists.newArrayList( session.getSpecies() );
        ChartValues geneChart = new ChartValues( "Genes Annotated" + (allSpecies ? "" : " in " + session.getSpecies().getCommonName()), "Gene Count", "Date" );
        geneChart.setSubtitle( currentTerm.getGoId() + " - " + currentTerm.getName() );
        Map<Species, Series> series = new HashMap<>();
        Map<Species, Series> directSeries = new HashMap<>();
        Map<Long, Integer> totalSeriesData = new HashMap<>();
        Map<Long, Integer> directTotalSeriesData = new HashMap<>();
        for ( Species sp : species ) {

            Collection<Edition> eds = cache.getAllEditions( sp );

            if ( eds != null ) {

                Series s = new Series( allSpecies ? sp.getScientificName() : "All" );
                Series s2 = new Series( allSpecies ? sp.getScientificName() + " Direct" : "Direct Only" );
                series.put( sp, s );
                directSeries.put( sp, s2 );
                for ( Edition ed : eds ) {
                    GeneOntologyTerm t = trackedTerms.get( ed.getGoEdition() );
                    if ( t != null ) {
                        // If term existed
                        if ( cache.getAggregate( ed ) != null ) {
                            // if this returns null it means the edition has no data in it
                            // most likely missing data

                            // Inferred annotations

                            Integer cnt = cache.getInferredAnnotationCount( ed, t );
                            cnt = (cnt == null) ? 0 : cnt;
                            s.addDataPoint( ed.getDate(), cnt );

                            // Total Inferred

                            Integer totalCnt = totalSeriesData.get( ed.getDate().getTime() );
                            totalSeriesData.put( ed.getDate().getTime(), totalCnt == null ? cnt : totalCnt + cnt );

                            // Direct annotations

                            cnt = cache.getDirectAnnotationCount( ed, t );
                            cnt = (cnt == null) ? 0 : cnt;
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

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( geneChart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_gene", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point to fetch data for the creation of the evidence chart, which shows total counts of annotations over
     * time grouped by evidence category (not unique genes, this retrieves total counts of unique annotations from the
     * database)
     */
    public void fetchEvidenceChart() {
        // Make evidence charts

        Map<String, Map<Date, Integer>> evidenceCounts = allSpecies ?
                annotationService.fetchCategoryCounts( currentTerm ) :
                annotationService.fetchCategoryCountsInSpecies( currentTerm, session.getSpecies() );

        ChartValues evidenceChart = new ChartValues( "Annotation Category Counts" + (allSpecies ? "" : " in " + session.getSpecies().getCommonName()),
                "Annotation Count", "Date" );
        evidenceChart.setSubtitle( currentTerm.getGoId() + " - " + currentTerm.getName() );
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

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( evidenceChart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_evidence", new Gson().toJson( hcGsonMap ) );
    }

}
