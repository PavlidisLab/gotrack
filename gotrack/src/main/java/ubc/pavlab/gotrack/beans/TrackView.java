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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.extensions.model.timeline.TimelineEvent;
import org.primefaces.extensions.model.timeline.TimelineGroup;
import org.primefaces.extensions.model.timeline.TimelineModel;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.exception.GeneNotFoundException;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.GoChart;
import ubc.pavlab.gotrack.model.GraphTypeKey;
import ubc.pavlab.gotrack.model.GraphTypeKey.GraphType;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.utilities.Jaccard;

/**
 * @author mjacobson
 * @version $Id$
 */
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

    private static final Logger log = Logger.getLogger( TrackView.class );

    // DAO
    private AnnotationDAO annotationDAO;

    // Query params
    private Integer currentSpeciesId;
    private Species currentSpecies;
    private String query;

    // private List<TrackValue> trackValues;

    // Static data
    private Gene currentGene;
    private Edition currentEdition;
    private List<Edition> allEditions = new ArrayList<>();
    private Collection<GeneOntologyTerm> allTerms = new HashSet<GeneOntologyTerm>();

    /* Current Chart Stuff */
    private LineChartModel currentChart; // Current chart
    private GoChart<Edition, Set<GeneOntologyTerm>> currentGoChart;

    // All charts
    private Map<GraphTypeKey, LineChartModel> lineChartModelMap = new HashMap<GraphTypeKey, LineChartModel>();
    private Map<GraphTypeKey, GoChart<Edition, Set<GeneOntologyTerm>>> goChartMap = new HashMap<GraphTypeKey, GoChart<Edition, Set<GeneOntologyTerm>>>();

    // Select Data Point functionality
    private String selectedDate;
    private Collection<GeneOntologyTerm> itemSelectTerms;
    private Collection<GeneOntologyTerm> filteredTerms;
    private List<GeneOntologyTerm> itemSelectViewTerms;
    private Number chartValue;

    // Right Panel
    private List<GeneOntologyTerm> selectedTerms;
    private Collection<GeneOntologyTerm> filteredAllTerms;

    // Static
    private static final List<String> aspects = Arrays.asList( "BP", "MF", "CC" );
    private static final List<String> graphs = Arrays.asList( "direct", "propagated" );
    private static final String COMBINED_TITLE = "All accessions";

    // Settings
    private boolean splitAccessions = true;
    private boolean propagate = false;
    private String graphType = "annotation";
    private String scale = "linear";
    private boolean chartsReady = false;
    private boolean firstChartReady = false;

    // Timeline
    private TimelineModel timelineModel;

    /**
     * 
     */
    public TrackView() {
        log.info( "TrackView created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );
    }

    /**
     * Change current graph
     * 
     * @param graphType graph type to change to
     */
    public void changeGraph( String graphType ) {
        // System.out.println( "New value: " + graphType );
        this.graphType = graphType;
        reloadGraph();
    }

    /**
     * Creates LineChartModel based on series data
     * 
     * @param <T>
     * @param allSeries map of unique series title to data, data is a map of edition to go term set
     * @param title title of chart
     * @param yAxis y-axis label
     * @param staticData data for a single series that will not contain a breakdown of it's value into go terms, usually
     *        for species averages
     * @return LineChartModel for given data and specifications
     */
    private <T extends Number> LineChartModel createChart( GoChart<Edition, Set<GeneOntologyTerm>> goChart,
            GoChart<Edition, T> staticData ) {

        LineChartModel dateModel = new LineChartModel();

        if ( staticData != null ) {
            for ( Entry<String, LinkedHashMap<Edition, T>> es : staticData.getSeries().entrySet() ) {
                String label = es.getKey();
                Map<Edition, T> sData = es.getValue();

                LineChartSeries series = new LineChartSeries();
                series.setLabel( label );
                series.setShowMarker( false );

                for ( Entry<Edition, T> dataPoint : sData.entrySet() ) {
                    String date = dataPoint.getKey().getDate().toString();
                    T val = dataPoint.getValue();
                    series.set( date, val );
                }

                dateModel.addSeries( series );
            }
        }

        for ( Entry<String, LinkedHashMap<Edition, Set<GeneOntologyTerm>>> es : goChart.getSeries().entrySet() ) {
            String primary = es.getKey();
            Map<Edition, Set<GeneOntologyTerm>> sData = es.getValue();

            LineChartSeries series = new LineChartSeries();
            series.setLabel( primary );
            series.setMarkerStyle( "filledDiamond" );

            for ( Entry<Edition, Set<GeneOntologyTerm>> dataPoint : sData.entrySet() ) {
                String date = dataPoint.getKey().getDate().toString();
                Integer count = dataPoint.getValue().size();
                series.set( date, count );
            }

            dateModel.addSeries( series );
        }

        dateModel.setTitle( goChart.getTitle() );
        dateModel.setZoom( true );

        dateModel.setLegendPosition( "nw" );
        // dateModel.setAnimate( true );
        dateModel.setLegendRows( 8 );
        dateModel.setMouseoverHighlight( true );
        dateModel.setExtender( "chartExtender" );

        dateModel.getAxis( AxisType.Y ).setLabel( goChart.getyLabel() );

        if ( goChart.getMin() != null ) {
            dateModel.getAxis( AxisType.Y ).setMin( goChart.getMin() );
        }

        if ( goChart.getMax() != null ) {
            dateModel.getAxis( AxisType.Y ).setMax( goChart.getMax() );
        }

        DateAxis axis = new DateAxis( goChart.getxLabel() );
        // CategoryAxis axis = new CategoryAxis( "Editions" );
        axis.setTickAngle( -50 );
        // axis.setMax( currentEdition.getDate());
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;

    }

    /**
     * Creates chart based on direct annotation data, this is done separately for lazy-loading purposes in the front-end
     */
    public void fetchDirectChart() {
        // Direct Annotations Chart
        log.info( "fetch Direct" );

        GoChart<Edition, Set<GeneOntologyTerm>> goChart = new GoChart<Edition, Set<GeneOntologyTerm>>(
                "Direct Annotations vs Time", "Dates", "Direct Annotations", annotationDAO.track2( currentSpeciesId,
                        query, currentEdition.getEdition(), currentEdition.getGoEditionId(), false ) );

        log.info( "fetched Direct" );

        GraphTypeKey gtk = new GraphTypeKey( GraphType.annotation, true, false );

        Map<String, Map<Edition, Double>> staticData = new HashMap<String, Map<Edition, Double>>();
        staticData.put( "Species Avg", cache.getSpeciesAverage( currentSpeciesId ) );
        // Base Chart
        initChart( gtk, goChart, new GoChart<Edition, Double>( "Direct Annotations vs Time", "Dates",
                "Direct Annotations", staticData ) );
        // Combined Chart
        initChart( new GraphTypeKey( GraphType.annotation, false, false ),
                GoChart.combineSeries( COMBINED_TITLE, goChart ), null );

        // currentGoChart = allGoCharts.get( "direct" );
        currentGoChart = goChartMap.get( gtk );
        // currentChart = allCharts.get( "direct" );
        currentChart = lineChartModelMap.get( gtk );

        // chart is rendered on the front-end when this is set to true
        firstChartReady = true;

    }

    /**
     * Fetch Union of all terms in all editions for this gene to be displayed in a table on the front-end
     */
    public void fetchAllTerms() {
        log.info( "fetch All Terms" );
        allTerms.clear();
        // GoChart<Edition, Set<GeneOntologyTerm>> t = allGoCharts.get( "direct" );
        GoChart<Edition, Set<GeneOntologyTerm>> t = goChartMap
                .get( new GraphTypeKey( GraphType.annotation, true, false ) );
        for ( Map<Edition, Set<GeneOntologyTerm>> series : t.getSeries().values() ) {
            for ( Set<GeneOntologyTerm> terms : series.values() ) {
                for ( GeneOntologyTerm geneOntologyTerm : terms ) {
                    // if ( allTerms.contains( geneOntologyTerm ) && geneOntologyTerm.getAspect() != null ) {
                    // System.out.println(geneOntologyTerm);
                    // allTerms.remove( geneOntologyTerm );
                    // allTerms.add( geneOntologyTerm );
                    // } else {
                    // allTerms.add( geneOntologyTerm );
                    // }
                    if ( geneOntologyTerm.getAspect() != null ) {
                        allTerms.add( geneOntologyTerm );
                    }

                }
            }
        }
    }

    /**
     * Creates chart based on direct annotation data, this is done separately for lazy-loading purposes in the front-end
     */
    public void fetchPropagatedChart() {
        // Propagated Annotations Chart
        log.info( "fetch Propagated" );

        // Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries = annotationDAO.track( currentSpeciesId,
        // primaryToSecondary, currentEdition.getGoEditionId(), true );
        Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries = annotationDAO.track2( currentSpeciesId, query,
                currentEdition.getEdition(), currentEdition.getGoEditionId(), true );

        log.info( "fetched Propagated" );

        // Add back direct parents if not there
        // for ( Entry<String, LinkedHashMap<Edition, Set<GeneOntologyTerm>>> seriesEntry : allGoCharts.get( "direct"
        // ).getSeries().entrySet() ) {
        for ( Entry<String, LinkedHashMap<Edition, Set<GeneOntologyTerm>>> seriesEntry : goChartMap
                .get( new GraphTypeKey( GraphType.annotation, true, false ) ).getSeries().entrySet() ) {
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

        GoChart<Edition, Set<GeneOntologyTerm>> goChart = new GoChart<Edition, Set<GeneOntologyTerm>>(
                "Propagated Annotations vs Time", "Dates", "Propagated Annotations", allSeries );

        // Base Chart
        initChart( new GraphTypeKey( GraphType.annotation, true, true ), goChart, null );
        // Combined Chart
        initChart( new GraphTypeKey( GraphType.annotation, false, true ),
                GoChart.combineSeries( COMBINED_TITLE, goChart ), null );

        chartsReady = true;
    }

    public void fetchJaccardChart() {
        log.info( "fetch Jaccard" );

        Map<String, Map<Edition, Double>> staticData = new HashMap<String, Map<Edition, Double>>();

        for ( Entry<String, LinkedHashMap<Edition, Set<GeneOntologyTerm>>> seriesEntry : goChartMap
                .get( new GraphTypeKey( GraphType.annotation, true, false ) ).getSeries().entrySet() ) {
            String seriesAccession = seriesEntry.getKey();
            LinkedHashMap<Edition, Set<GeneOntologyTerm>> series = seriesEntry.getValue();
            // Loop through to get current set of Go Ids
            Set<GeneOntologyTerm> currentGoSet = new HashSet<GeneOntologyTerm>();
            for ( Iterator<Set<GeneOntologyTerm>> iterator = series.values().iterator(); iterator.hasNext(); ) {
                if ( iterator.hasNext() ) {
                    currentGoSet = iterator.next();
                }
            }

            Map<Edition, Double> jaccardSeries = new HashMap<Edition, Double>();

            for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : series.entrySet() ) {
                Double jaccard = Jaccard.similarity( editionEntry.getValue(), currentGoSet );
                jaccardSeries.put( editionEntry.getKey(), jaccard );
            }

            staticData.put( seriesAccession, jaccardSeries );

        }

        GraphTypeKey gtk = new GraphTypeKey( GraphType.jaccard, true, false );
        GoChart<Edition, Set<GeneOntologyTerm>> template = new GoChart<Edition, Set<GeneOntologyTerm>>(
                "Jaccard Similarity vs Time", "Dates", "Jaccard Similarity" );
        template.setMin( 0 );
        template.setMax( 1 );
        initChart( gtk, template, new GoChart<Edition, Double>( staticData ) );
        // initChart( gtk, new GoChart<Edition, Set<GeneOntologyTerm>>(), staticData );
        copyChart( gtk, new GraphTypeKey( GraphType.jaccard, false, false ) );
        copyChart( gtk, new GraphTypeKey( GraphType.jaccard, false, true ) );
        copyChart( gtk, new GraphTypeKey( GraphType.jaccard, true, true ) );

        log.info( "fetched Jaccard" );

    }

    public void fetchMultiChart() {
        log.info( "fetch Multifunctionality" );

        Map<String, Map<Edition, Double>> staticData = createMultiData( goChartMap.get( new GraphTypeKey(
                GraphType.annotation, true, false ) ) );

        GraphTypeKey gtk = new GraphTypeKey( GraphType.multifunctionality, true, false );
        GoChart<Edition, Set<GeneOntologyTerm>> template = new GoChart<Edition, Set<GeneOntologyTerm>>(
                "Multifunctionality vs Time", "Dates", "Multifunctionality" );
        initChart( gtk, template, new GoChart<Edition, Double>( staticData ) );

        // Combined
        staticData = createMultiData( goChartMap.get( new GraphTypeKey( GraphType.annotation, false, false ) ) );
        gtk = new GraphTypeKey( GraphType.multifunctionality, false, false );
        initChart( gtk, template, new GoChart<Edition, Double>( staticData ) );

        // initChart( gtk, new GoChart<Edition, Set<GeneOntologyTerm>>(), staticData );
        copyChart( gtk, new GraphTypeKey( GraphType.multifunctionality, false, true ) );
        copyChart( gtk, new GraphTypeKey( GraphType.multifunctionality, true, true ) );
        log.info( "fetched Multifunctionality" );
    }

    private Map<String, Map<Edition, Double>> createMultiData( GoChart<Edition, Set<GeneOntologyTerm>> goChart ) {
        Map<String, Map<Edition, Double>> staticData = new HashMap<>();
        for ( Entry<String, LinkedHashMap<Edition, Set<GeneOntologyTerm>>> seriesEntry : goChart.getSeries().entrySet() ) {
            String seriesAccession = seriesEntry.getKey();
            LinkedHashMap<Edition, Set<GeneOntologyTerm>> series = seriesEntry.getValue();

            Map<Edition, Double> multiSeries = new HashMap<>();

            for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : series.entrySet() ) {

                Double multi = 0.0;
                Edition ed = editionEntry.getKey();
                Integer total = cache.getAccessionSize( currentSpeciesId, ed );
                Set<GeneOntologyTerm> data = editionEntry.getValue();
                if ( total != null ) {
                    for ( GeneOntologyTerm geneOntologyTerm : data ) {
                        Integer inGroup = cache.getGoSetSizes( currentSpeciesId, ed.getEdition(),
                                geneOntologyTerm.getGoId() );
                        if ( inGroup != null ) {
                            multi += 1.0 / ( inGroup * ( total - inGroup ) );
                        }
                    }

                    multiSeries.put( ed, multi );

                }

            }

            staticData.put( seriesAccession, multiSeries );

        }
        return staticData;
    }

    public void fetchLossGainChart() {
        log.info( "fetch Loss / Gain" );

        Map<String, Map<Edition, Integer>> staticData = new HashMap<String, Map<Edition, Integer>>();

        for ( Entry<String, LinkedHashMap<Edition, Set<GeneOntologyTerm>>> seriesEntry : goChartMap
                .get( new GraphTypeKey( GraphType.annotation, false, false ) ).getSeries().entrySet() ) {
            String seriesAccession = seriesEntry.getKey();
            LinkedHashMap<Edition, Set<GeneOntologyTerm>> series = seriesEntry.getValue();

            Map<Edition, Integer> lossSeries = new HashMap<Edition, Integer>();
            Map<Edition, Integer> gainSeries = new HashMap<Edition, Integer>();
            Set<GeneOntologyTerm> previousGoSet = null;
            for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : series.entrySet() ) {
                if ( previousGoSet != null ) {
                    lossSeries.put( editionEntry.getKey(), setDifferenceSize( previousGoSet, editionEntry.getValue() ) );
                    gainSeries.put( editionEntry.getKey(), setDifferenceSize( editionEntry.getValue(), previousGoSet ) );

                }
                previousGoSet = editionEntry.getValue();
            }

            staticData.put( seriesAccession + " - loss", lossSeries );
            staticData.put( seriesAccession + " - gain", gainSeries );

        }

        GraphTypeKey gtk = new GraphTypeKey( GraphType.lossgain, false, false );
        GoChart<Edition, Set<GeneOntologyTerm>> template = new GoChart<Edition, Set<GeneOntologyTerm>>(
                "Loss & Gain vs Time", "Dates", "Change" );
        initChart( gtk, template, new GoChart<Edition, Integer>( staticData ) );
        // initChart( gtk, new GoChart<Edition, Set<GeneOntologyTerm>>(), staticData );
        copyChart( gtk, new GraphTypeKey( GraphType.lossgain, true, false ) );
        copyChart( gtk, new GraphTypeKey( GraphType.lossgain, false, true ) );
        copyChart( gtk, new GraphTypeKey( GraphType.lossgain, true, true ) );
        log.info( "fetched Loss / Gain" );
    }

    private static <T> Integer setDifferenceSize( Set<T> setA, Set<T> setB ) {
        Set<T> tmp = new HashSet<T>( setA );
        tmp.removeAll( setB );
        return tmp.size();
    }

    /**
     * Entry-point for creating timeline from the sidepanel of all terms
     */
    public void fetchTimelineFromPanel() {
        fetchTimeline( true );
    }

    /**
     * Entry-point for creating timeline from the dialog breakdown of an edition's go terms
     */
    public void fetchTimelineFromDialog() {
        fetchTimeline( false );
    }

    /**
     * @param fromPanel Fetch data then create timeline from panel or not (from dialog)
     */
    private void fetchTimeline( boolean fromPanel ) {

        // <Selected Term, <Date, Exists>>
        Map<GeneOntologyTerm, Map<Date, Boolean>> timelineData = new HashMap<GeneOntologyTerm, Map<Date, Boolean>>();

        Collection<GeneOntologyTerm> selected = fromPanel ? selectedTerms : itemSelectViewTerms;

        for ( GeneOntologyTerm geneOntologyTerm : selected ) {
            timelineData.put( geneOntologyTerm, new HashMap<Date, Boolean>() );
        }

        // Map<Edition, Set<GeneOntologyTerm>> allCombinedDirectSeries = allGoCharts.get( "direct" + COMBINED_SUFFIX
        // ).get( COMBINED_TITLE );
        Map<Edition, Set<GeneOntologyTerm>> allCombinedDirectSeries = goChartMap.get(
                new GraphTypeKey( GraphType.annotation, false, false ) ).get( COMBINED_TITLE );

        for ( Entry<Edition, Set<GeneOntologyTerm>> esSeries : allCombinedDirectSeries.entrySet() ) {
            Date date = esSeries.getKey().getDate();

            Set<GeneOntologyTerm> terms = esSeries.getValue();

            for ( GeneOntologyTerm geneOntologyTerm : selected ) {
                Map<Date, Boolean> data = timelineData.get( geneOntologyTerm );
                Boolean exists = data.get( date );

                if ( exists == null || !exists ) {
                    data.put( date, terms.contains( geneOntologyTerm ) );
                }

            }

        }

        timelineModel = createTimeline( timelineData );

    }

    /**
     * create timeline based on data
     * 
     * @param timelineData timeline data, map of terms to map of date to boolean representing whether the term existed
     *        on that date
     * @return
     */
    private TimelineModel createTimeline( Map<GeneOntologyTerm, Map<Date, Boolean>> timelineData ) {

        TimelineModel model = new TimelineModel();
        DateFormat df = new SimpleDateFormat( "yyyy-MM-dd" );
        Calendar cal = Calendar.getInstance();

        List<TimelineGroup> groups = new ArrayList<TimelineGroup>();
        List<TimelineEvent> events = new ArrayList<TimelineEvent>();

        for ( Entry<GeneOntologyTerm, Map<Date, Boolean>> esTermData : timelineData.entrySet() ) {
            GeneOntologyTerm term = esTermData.getKey();

            Map<Date, Boolean> data = esTermData.getValue();

            TimelineGroup group = new TimelineGroup( term.getGoId(), term );
            groups.add( group );
            // model.addGroup( group );

            SortedSet<Date> dates = new TreeSet<Date>( data.keySet() );
            Date prevDate = null;
            for ( Date date : dates ) {
                if ( prevDate != null ) {
                    boolean exists = data.get( prevDate );
                    TimelineEvent event = new TimelineEvent( df.format( prevDate ), prevDate, date, false,
                            term.getGoId(), exists ? "timeline-true timeline-hidden" : "timeline-false timeline-hidden" );
                    // model.add( event );
                    events.add( event );

                }

                prevDate = date;
            }

            // give the last edition a span of 1 month
            if ( dates.size() > 1 ) {
                boolean exists = data.get( prevDate );
                cal.setTime( prevDate );
                cal.add( Calendar.MONTH, 1 );
                TimelineEvent event = new TimelineEvent( df.format( prevDate ), prevDate, cal.getTime(), false,
                        term.getGoId(), exists ? "timeline-true timeline-hidden" : "timeline-false timeline-hidden" );
                // model.add( event );
                events.add( event );
            }

        }

        model.setGroups( groups );
        model.addAll( events );

        return model;

    }

    public void cleanTimeline() {
        timelineModel = null;
    }

    public void cleanFilteredTerms() {
        filteredTerms = null;
    }

    public String init() throws GeneNotFoundException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "TrackView init: " + currentSpeciesId + ": " + query );
        currentGene = cache.getCurrentGene( currentSpeciesId, query );
        if ( currentGene == null ) {

            throw new GeneNotFoundException();
            /*
             * FacesContext facesContext = FacesContext.getCurrentInstance(); NavigationHandler navigationHandler =
             * facesContext.getApplication().getNavigationHandler(); navigationHandler.handleNavigation( facesContext,
             * null, "error400?faces-redirect=true" );
             */
        } else {
            // Get secondary accessions
            // Map<String, Collection<String>> primaryToSecondary = new HashMap<String, Collection<String>>();

            // Obtain AnnotationDAO.
            log.info( "symbol: " + currentGene.getSymbol() );
            log.info( "synonyms: " + currentGene.getSynonyms() );
            log.info( "accessions: " + currentGene.getAccessions() );

            log.info( "Gene: " + currentGene );

            annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();
            currentEdition = cache.getCurrentEditions( currentSpeciesId );
            allEditions = cache.getAllEditions( currentSpeciesId );
            for ( Species s : cache.getSpeciesList() ) {
                if ( s.getId().equals( currentSpeciesId ) ) {
                    currentSpecies = s;
                }
            }

            return null;

        }

    }

    /**
     * Initialize a chart, store it in map of all charts and it's data in a similar map of all data
     * 
     * @param <T>
     * @param identifier unique identifier to access the chart
     * @param goChart contains data
     * @param staticData data for a single series that will not contain a breakdown of it's value into go terms, usually
     *        for species averages
     */
    private <T extends Number> void initChart( GraphTypeKey graphTypeKey,
            GoChart<Edition, Set<GeneOntologyTerm>> goChart, GoChart<Edition, T> staticData ) {

        /* Base chart */
        lineChartModelMap.put( graphTypeKey, createChart( goChart, staticData ) );
        goChartMap.put( graphTypeKey, goChart );

        /*
         * Combined chart
         * 
         * GoChart<Edition, Set<GeneOntologyTerm>> g = GoChart.combineSeries( COMBINED_TITLE, goChart );
         * lineChartModelMap.put( graphTypeKey, createChart( g, null ) ); goChartMap.put( graphTypeKey, g );
         */
        log.info( graphTypeKey + " chart initialized" );
    }

    private void copyChart( GraphTypeKey fromGraphTypeKey, GraphTypeKey toGraphTypeKey ) {
        lineChartModelMap.put( toGraphTypeKey, lineChartModelMap.get( fromGraphTypeKey ) );
        goChartMap.put( toGraphTypeKey, goChartMap.get( fromGraphTypeKey ) );
    }

    public void itemSelect( ItemSelectEvent event ) {

        // dateModel.getSeries().get( event.getSeriesIndex() ).getData().get( key );
        List<Entry<Object, Number>> es = new ArrayList<Entry<Object, Number>>( currentChart.getSeries()
                .get( event.getSeriesIndex() ).getData().entrySet() );

        // Keep in mind that the list of entry sets is in the order that the data was inserted, not the order it is
        // displayed!

        // log.info( "Key: " + es.get( event.getItemIndex() ).getKey() );
        // log.info( "Value: " + es.get( event.getItemIndex() ).getValue() );

        String date = ( String ) es.get( event.getItemIndex() ).getKey();
        chartValue = es.get( event.getItemIndex() ).getValue();

        String label = currentChart.getSeries().get( event.getSeriesIndex() ).getLabel();

        selectedDate = date;

        Collection<Edition> ed = getGoEditionsFromDate( date );

        itemSelectTerms = new HashSet<GeneOntologyTerm>();
        if ( ed.size() == 0 ) {
            log.warn( "Found no editions for date (" + date + ")" );
        } else {

            if ( ed.size() > 1 ) log.warn( "Found more than one edition for date (" + date + ")" );

            Map<Edition, Set<GeneOntologyTerm>> series = currentGoChart.get( label );

            if ( series == null ) {
                log.debug( "Could not find series for  (" + label + ")" );
            } else {
                itemSelectTerms = series.get( ed.iterator().next() );
            }

        }

    }

    private Collection<Edition> getGoEditionsFromDate( Date date ) {
        return getGoEditionsFromDate( date.toString() );
    }

    private Collection<Edition> getGoEditionsFromDate( String date ) {
        List<Edition> results = new ArrayList<Edition>();
        for ( Edition ed : allEditions ) {
            if ( ed.getDate().toString().equals( date ) ) {
                results.add( ed );
            }

        }
        return results;
    }

    public void keepAlive() {
        log.info( "Kept alive" );
    }

    /**
     * Sets current graph and series data based on @graphType
     */
    public void reloadGraph() {

        if ( graphType == null || graphType.equals( "" ) ) graphType = "annotation";
        // graphType = propagate ? "propagated" : "direct";

        GraphTypeKey gtk = new GraphTypeKey( GraphType.valueOf( graphType ), splitAccessions, propagate );
        log.info( gtk );
        // log.info( graphType + ( splitAccessions ? "" : COMBINED_SUFFIX ) );

        currentChart = lineChartModelMap.get( gtk );
        currentGoChart = goChartMap.get( gtk );
    }

    public void showDialog() {
        RequestContext.getCurrentInstance().openDialog( "dlg2" );
    }

    public void toggleSplit() {
        splitAccessions = !splitAccessions;
        reloadGraph();
    }

    public boolean isPropagate() {
        return propagate;
    }

    public void setPropagate( boolean propagate ) {
        this.propagate = propagate;
    }

    public String getScale() {
        return scale;
    }

    public void setScale( String scale ) {
        this.scale = scale;
    }

    public List<String> getAspects() {
        return aspects;
    }

    public LineChartModel getCurrentChart() {
        return currentChart;
    }

    public ArrayList<Accession> getCurrentPrimaryAccessionsValues() {
        return new ArrayList<Accession>( currentGene.getAccessions() );
    }

    public Species getCurrentSpecies() {
        return currentSpecies;
    }

    public Integer getCurrentSpeciesId() {
        return currentSpeciesId;
    }

    public Collection<GeneOntologyTerm> getFilteredTerms() {
        return filteredTerms;
    }

    public Collection<GeneOntologyTerm> getFilteredAllTerms() {
        return filteredAllTerms;
    }

    public List<String> getGraphs() {
        return graphs;
    }

    // public List<TrackValue> getTrackValues() {
    // return trackValues;
    // }

    public String getQuery() {
        return query;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public Collection<GeneOntologyTerm> getItemSelectTerms() {
        return itemSelectTerms;
    }

    public Collection<GeneOntologyTerm> getAllTerms() {
        return allTerms;
    }

    public List<GeneOntologyTerm> getSelectedTerms() {
        return selectedTerms;
    }

    public TimelineModel getTimelineModel() {
        return timelineModel;
    }

    public String getGraphType() {
        return this.graphType;
    }

    public Number getChartValue() {
        return chartValue;
    }

    public boolean isChartsReady() {
        return chartsReady;
    }

    public boolean isFirstChartReady() {
        return firstChartReady;
    }

    public void setFirstChartReady( boolean firstChartReady ) {
        this.firstChartReady = firstChartReady;
    }

    public boolean isSplitAccessions() {
        return splitAccessions;
    }

    public void setSelectedTerms( List<GeneOntologyTerm> selectedTerms ) {
        this.selectedTerms = selectedTerms;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

    public void setCurrentSpeciesId( Integer currentSpeciesId ) {
        this.currentSpeciesId = currentSpeciesId;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public void setFilteredTerms( Collection<GeneOntologyTerm> filteredTerms ) {
        this.filteredTerms = filteredTerms;
    }

    public void setFilteredAllTerms( Collection<GeneOntologyTerm> filteredAllTerms ) {
        this.filteredAllTerms = filteredAllTerms;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public void setSelectedDate( String selectedDate ) {
        this.selectedDate = selectedDate;
    }

    public void setSplitAccessions( boolean splitAccessions ) {
        this.splitAccessions = splitAccessions;
    }

    public void setGraphType( String graphType ) {
        this.graphType = graphType;
    }

    public List<GeneOntologyTerm> getItemSelectViewTerms() {
        return itemSelectViewTerms;
    }

    public void setItemSelectViewTerms( List<GeneOntologyTerm> itemSelectViewTerms ) {
        this.itemSelectViewTerms = itemSelectViewTerms;
    }

}
