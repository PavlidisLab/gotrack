/*
 * The gotrack project
 * 
 * Copyright (c) 2014 University of British Columbia
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.exception.GeneNotFoundException;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class TrackView implements Serializable {

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    /**
     * 
     */
    private static final long serialVersionUID = 7413572739210227872L;

    // DAO
    private AnnotationDAO annotationDAO;

    // Query params
    private Integer currentSpecies;
    private String query;

    // private List<TrackValue> trackValues;

    // Static data
    private Edition currentEdition;
    private Map<String, Accession> currentPrimaryAccessions = new HashMap<String, Accession>();
    Map<String, Collection<String>> primaryToSecondary = new HashMap<String, Collection<String>>();

    /* Chart Stuff */
    private Map<String, Map<Edition, Set<GeneOntologyTerm>>> tmpData;
    private LineChartModel currentChart; // Current chart
    private Map<String, Map<String, Set<GeneOntologyTerm>>> seriesData; // Current data in chart used for select
    private Map<String, LineChartModel> allCharts = new HashMap<String, LineChartModel>(); // All Charts
    private Map<String, Map<String, Map<String, Set<GeneOntologyTerm>>>> allSeriesData = new HashMap<String, Map<String, Map<String, Set<GeneOntologyTerm>>>>(); // All
                                                                                                                                                                 // series
    private boolean chartsReady = false;

    // Select functionality
    private String selectedDate;
    private Collection<GeneOntologyTerm> terms = new ArrayList<GeneOntologyTerm>();
    private Collection<GeneOntologyTerm> filteredTerms;

    // Static lists
    private static final List<String> aspects = Arrays.asList( "BP", "MF", "CC" );
    private static final List<String> graphs = Arrays.asList( "direct", "propagated" );

    // Settings
    private String graphType = "direct";
    private boolean splitAccessions = true;

    /**
     * 
     */
    public TrackView() {
        System.out.println( "TrackView created" );
    }

    public void init() throws GeneNotFoundException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return; // Skip ajax requests.
        }
        System.out.println( "TrackView init: " + currentSpecies + ": " + query );
        Map<String, Collection<Accession>> c = cache.getSymbolToCurrentAccessions().get( currentSpecies );
        Collection<Accession> primaryAccessions;
        if ( query == null
                || currentSpecies == null
                || c == null
                || ( primaryAccessions = cache.getSymbolToCurrentAccessions().get( currentSpecies ).get( query ) ) == null ) {

            throw new GeneNotFoundException();
            /*
             * FacesContext facesContext = FacesContext.getCurrentInstance(); NavigationHandler navigationHandler =
             * facesContext.getApplication().getNavigationHandler(); navigationHandler.handleNavigation( facesContext,
             * null, "error400?faces-redirect=true" );
             */
        } else {
            // Get secondary accessions
            // Map<String, Collection<String>> primaryToSecondary = new HashMap<String, Collection<String>>();
            for ( Accession accession : primaryAccessions ) {
                primaryToSecondary.put( accession.getAccession(), accession.getSecondary() );
                currentPrimaryAccessions.put( accession.getAccession(), accession );
            }

            // Obtain AnnotationDAO.
            annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();
            currentEdition = cache.getCurrentEditions().get( currentSpecies );

        }

    }

    private Map<String, Set<GeneOntologyTerm>> editionMapToDateMap( Map<Edition, Set<GeneOntologyTerm>> editionMap ) {
        Map<String, Set<GeneOntologyTerm>> res = new HashMap<String, Set<GeneOntologyTerm>>();
        for ( Entry<Edition, Set<GeneOntologyTerm>> es : editionMap.entrySet() ) {
            res.put( es.getKey().getDate().toString(), es.getValue() );
        }
        return res;
    }

    private LineChartModel createChart( Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries, String title,
            String yAxis, Map<Edition, Double> staticData ) {

        LineChartModel dateModel = new LineChartModel();

        if ( staticData != null ) {
            List<Edition> editions = new ArrayList<Edition>( staticData.keySet() );
            Collections.sort( editions, new CustomComparator() );
            LineChartSeries series = new LineChartSeries();
            series.setLabel( "Species Avg" );
            series.setShowMarker( false );
            for ( Edition edition : editions ) {
                String date = edition.getDate().toString();
                Double count = staticData.get( edition );
                series.set( date, count );
            }
            dateModel.addSeries( series );
        }

        for ( Entry<String, Map<Edition, Set<GeneOntologyTerm>>> es : allSeries.entrySet() ) {
            String primary = es.getKey();
            Map<Edition, Set<GeneOntologyTerm>> sData = es.getValue();

            List<Edition> editions = new ArrayList<Edition>( sData.keySet() );
            // Sort so that display order or series matches ItemIndex order returned by ItemSelectEvent
            Collections.sort( editions, new CustomComparator() );

            LineChartSeries series = new LineChartSeries();
            series.setLabel( primary );
            series.setMarkerStyle( "filledDiamond" );

            for ( Edition edition : editions ) {
                String date = edition.getDate().toString();
                Integer count = sData.get( edition ).size();
                series.set( date, count );
            }

            dateModel.addSeries( series );
        }

        dateModel.setTitle( title );
        dateModel.setZoom( true );

        dateModel.setLegendPosition( "nw" );
        // dateModel.setAnimate( true );
        dateModel.setLegendRows( 8 );
        dateModel.setMouseoverHighlight( true );
        dateModel.setExtender( "chartExtender" );

        dateModel.getAxis( AxisType.Y ).setLabel( yAxis );
        dateModel.getAxis( AxisType.Y ).setMin( 0 );
        DateAxis axis = new DateAxis( "Dates" );
        // CategoryAxis axis = new CategoryAxis( "Editions" );
        axis.setTickAngle( -50 );
        axis.setMax( currentEdition.getDate().toString() );
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;

    }

    public void itemSelect( ItemSelectEvent event ) {

        // dateModel.getSeries().get( event.getSeriesIndex() ).getData().get( key );
        List<Entry<Object, Number>> es = new ArrayList<Entry<Object, Number>>( currentChart.getSeries()
                .get( event.getSeriesIndex() ).getData().entrySet() );

        // Keep in mind that the list of entry sets is in the order that the data was inserted, not the order it is
        // displayed!
        String date = ( String ) es.get( event.getItemIndex() ).getKey();

        selectedDate = date;
        Map<String, Set<GeneOntologyTerm>> series = seriesData.get( currentChart.getSeries()
                .get( event.getSeriesIndex() ).getLabel() );
        if ( series == null ) {
            terms = new HashSet<GeneOntologyTerm>();
        } else {
            terms = series.get( date );
        }

    }

    public void changeGraph() {
        // System.out.println( "New value: " + graphType );
        if ( graphType == null || graphType.equals( "" ) ) graphType = "direct";
        currentChart = allCharts.get( graphType + ( splitAccessions ? "-combined" : "" ) );
        seriesData = allSeriesData.get( graphType + ( splitAccessions ? "-combined" : "" ) );
    }

    private void initChart( String identifier, Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries,
            String title, String yAxis, Map<Edition, Double> staticData ) {

        /* Base chart */
        allCharts.put( identifier, createChart( allSeries, title, yAxis, staticData ) );

        Map<String, Map<String, Set<GeneOntologyTerm>>> sData = new HashMap<String, Map<String, Set<GeneOntologyTerm>>>();
        for ( Entry<String, Map<Edition, Set<GeneOntologyTerm>>> es : allSeries.entrySet() ) {
            sData.put( es.getKey(), editionMapToDateMap( es.getValue() ) );
        }

        allSeriesData.put( identifier, sData );

        /* Combined chart */

        Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeriesCombined = combineSeries( allSeries );

        allCharts.put( identifier + "-combined", createChart( allSeriesCombined, title, yAxis, null ) );

        sData = new HashMap<String, Map<String, Set<GeneOntologyTerm>>>();
        for ( Entry<String, Map<Edition, Set<GeneOntologyTerm>>> es : allSeriesCombined.entrySet() ) {
            sData.put( es.getKey(), editionMapToDateMap( es.getValue() ) );
        }

        allSeriesData.put( identifier + "-combined", sData );
    }

    public void fetchDirectChart() {
        // Direct Annotations Chart
        System.out.println( "fetch Direct" );
        Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeriesDirect = annotationDAO.track( currentSpecies,
                primaryToSecondary, false );

        tmpData = allSeriesDirect;

        initChart( "direct", allSeriesDirect, "Direct Annotations vs Time", "Direct Annotations", cache
                .getSpeciesAverages().get( currentSpecies ) );

        seriesData = allSeriesData.get( "direct" );
        currentChart = allCharts.get( "direct" );

        chartsReady = true;
    }

    public void fetchPropagatedChart() {
        // Propagated Annotations Chart
        System.out.println( "fetch Propagated" );
        Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries = annotationDAO.track( currentSpecies,
                primaryToSecondary, true );

        // Add back direct parents if not there
        for ( Entry<String, Map<Edition, Set<GeneOntologyTerm>>> seriesEntry : tmpData.entrySet() ) {
            String seriesAccession = seriesEntry.getKey();
            for ( Entry<Edition, Set<GeneOntologyTerm>> editionInSeriesEntry : seriesEntry.getValue().entrySet() ) {
                Edition editionInSeries = editionInSeriesEntry.getKey();
                Set<GeneOntologyTerm> dataPoints = allSeries.get( seriesAccession ).get( editionInSeries );
                if ( dataPoints == null ) {
                    dataPoints = new HashSet<GeneOntologyTerm>();
                    // Commented out to ignore those editions where I have no GO structure data
                    // It is misleading to show only direct parents here
                    // allSeriesProp.get( seriesAccession ).put( editionInSeries, dataPoints );
                }
                dataPoints.addAll( editionInSeriesEntry.getValue() );
            }
        }

        tmpData = null;

        initChart( "propagated", allSeries, "Propagated Annotations vs Time", "Propagated Annotations", null );
    }

    private Map<String, Map<Edition, Set<GeneOntologyTerm>>> combineSeries(
            Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries ) {
        Map<Edition, Set<GeneOntologyTerm>> combinedSeries = new HashMap<Edition, Set<GeneOntologyTerm>>();
        for ( Map<Edition, Set<GeneOntologyTerm>> series : allSeries.values() ) {
            for ( Entry<Edition, Set<GeneOntologyTerm>> dataPoint : series.entrySet() ) {
                // String date = dataPoint.getKey();
                Edition edition = dataPoint.getKey();
                Set<GeneOntologyTerm> details = dataPoint.getValue();

                Set<GeneOntologyTerm> combinedDataPoint = combinedSeries.get( edition );

                if ( combinedDataPoint == null ) {
                    combinedDataPoint = new HashSet<GeneOntologyTerm>();
                    combinedSeries.put( edition, combinedDataPoint );
                }

                combinedDataPoint.addAll( details );

            }
        }

        Map<String, Map<Edition, Set<GeneOntologyTerm>>> result = new HashMap<String, Map<Edition, Set<GeneOntologyTerm>>>();
        result.put( "All Accessions", combinedSeries );

        return result;

    }

    public void showDialog() {
        RequestContext.getCurrentInstance().openDialog( "dlg2" );
    }

    public LineChartModel getCurrentChart() {
        return currentChart;
    }

    // public List<TrackValue> getTrackValues() {
    // return trackValues;
    // }

    public Integer getCurrentSpecies() {
        return currentSpecies;
    }

    public void setCurrentSpecies( Integer currentSpecies ) {
        this.currentSpecies = currentSpecies;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate( String selectedDate ) {
        this.selectedDate = selectedDate;
    }

    public Collection<GeneOntologyTerm> getTerms() {
        return terms;
    }

    public Collection<GeneOntologyTerm> getFilteredTerms() {
        return filteredTerms;
    }

    public void setFilteredTerms( Collection<GeneOntologyTerm> filteredTerms ) {
        this.filteredTerms = filteredTerms;
    }

    // public Set<String> getCodes() {
    // return codes;
    // }

    public String getGraphType() {
        return graphType;
    }

    public void setGraphType( String graphType ) {
        this.graphType = graphType;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public List<String> getAspects() {
        return aspects;
    }

    public List<String> getGraphs() {
        return graphs;
    }

    public boolean isSplitAccessions() {
        return splitAccessions;
    }

    public void setSplitAccessions( boolean splitAccessions ) {
        this.splitAccessions = splitAccessions;
    }

    public boolean isChartsReady() {
        return chartsReady;
    }

}

class CustomComparator implements Comparator<Edition> {
    @Override
    public int compare( Edition o1, Edition o2 ) {
        return o1.getEdition().compareTo( o2.getEdition() );
    }
}
