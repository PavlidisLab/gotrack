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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.Species;

class CustomComparator implements Comparator<Edition> {
    @Override
    public int compare( Edition o1, Edition o2 ) {
        return o1.getEdition().compareTo( o2.getEdition() );
    }
}

/**
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
    
    private static final Logger log = Logger.getLogger(TrackView.class);

    // DAO
    private AnnotationDAO annotationDAO;

    // Query params
    private Integer currentSpeciesId;
    private Species currentSpecies;
    private String query;

    // private List<TrackValue> trackValues;

    // Static data
    // private Edition currentEdition;
    private Map<String, Accession> currentPrimaryAccessions = new HashMap<String, Accession>();
    private Map<String, Collection<String>> primaryToSecondary = new HashMap<String, Collection<String>>();
    private Collection<GeneOntologyTerm> allTerms = new HashSet<GeneOntologyTerm>();

    /* Chart Stuff */
    private Map<String, Map<Edition, Set<GeneOntologyTerm>>> tmpData;
    private LineChartModel currentChart; // Current chart
    private Map<String, Map<String, Set<GeneOntologyTerm>>> seriesData; // Current data in chart used for select
    private Map<String, LineChartModel> allCharts = new HashMap<String, LineChartModel>(); // All Charts
    private Map<String, Map<String, Map<String, Set<GeneOntologyTerm>>>> allSeriesData = new HashMap<String, Map<String, Map<String, Set<GeneOntologyTerm>>>>(); // All
                                                                                                                                                                 // series
    private boolean chartsReady = false;

    // Select Data Point functionality
    private String selectedDate;
    private Collection<GeneOntologyTerm> itemSelectTerms;
    private Collection<GeneOntologyTerm> filteredTerms;
    private List<GeneOntologyTerm> itemSelectViewTerms;

    // Functionality
    private List<GeneOntologyTerm> selectedTerms;
    private Collection<GeneOntologyTerm> filteredAllTerms;

    // Static
    private static final List<String> aspects = Arrays.asList( "BP", "MF", "CC" );
    private static final List<String> graphs = Arrays.asList( "direct", "propagated" );
    private static final String COMBINED_TITLE = "All accessions";
    private static final String COMBINED_SUFFIX = "-combined";

    // Settings
    private boolean splitAccessions = true;
    private boolean propagate = false;
    private String graphType = "direct";
    private String scale = "linear";
    
    // Timeline
    private TimelineModel timelineModel;
    
    /**
     * 
     */
    public TrackView() {
        System.out.println( "TrackView created" );
        log.info( "TrackView created" );
    }

    public void changeGraph( String graphType ) {
        // System.out.println( "New value: " + graphType );
        this.graphType = graphType;
        reloadGraph();
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
        result.put( COMBINED_TITLE, combinedSeries );

        return result;

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
        // axis.setMax( currentEdition.getDate());
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;

    }

    private Map<String, Set<GeneOntologyTerm>> editionMapToDateMap( Map<Edition, Set<GeneOntologyTerm>> editionMap ) {
        Map<String, Set<GeneOntologyTerm>> res = new HashMap<String, Set<GeneOntologyTerm>>();
        for ( Entry<Edition, Set<GeneOntologyTerm>> es : editionMap.entrySet() ) {
            res.put( es.getKey().getDate().toString(), es.getValue() );
        }
        return res;
    }

    public void fetchDirectChart() {
        // Direct Annotations Chart
        System.out.println( "fetch Direct" );
        Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeriesDirect = annotationDAO.track( currentSpeciesId,
                primaryToSecondary, false );

        tmpData = allSeriesDirect;

        initChart( "direct", allSeriesDirect, "Direct Annotations vs Time", "Direct Annotations", cache
                .getSpeciesAverages().get( currentSpeciesId ) );

        seriesData = allSeriesData.get( "direct" );
        currentChart = allCharts.get( "direct" );

        chartsReady = true;
    }

    public void fetchAllTerms() {
        System.out.println( "fetch All Terms" );
        allTerms.clear();
        Map<String, Map<String, Set<GeneOntologyTerm>>> t = allSeriesData.get( "direct" );
        for ( Map<String, Set<GeneOntologyTerm>> series : t.values() ) {
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

    public void fetchPropagatedChart() {
        // Propagated Annotations Chart
        System.out.println( "fetch Propagated" );
        Map<String, Map<Edition, Set<GeneOntologyTerm>>> allSeries = annotationDAO.track( currentSpeciesId,
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
    
    public void fetchTimelineFromPanel() {
        fetchTimeline(true);
    }
    
    public void fetchTimelineFromDialog() {
        fetchTimeline(false);
    }
    
    private void fetchTimeline(boolean fromPanel) {
        
        // <Selected Term, <Date, Exists>>
        Map<GeneOntologyTerm, Map<Date, Boolean>> timelineData = new HashMap<GeneOntologyTerm, Map<Date, Boolean>>();
        
        Collection<GeneOntologyTerm> selected = fromPanel ? selectedTerms : itemSelectViewTerms ;
        
        for ( GeneOntologyTerm geneOntologyTerm : selected ) {
            timelineData.put( geneOntologyTerm, new HashMap<Date, Boolean>() );
        }
        
        Map<String, Set<GeneOntologyTerm>> allCombinedDirectSeries = allSeriesData.get( "direct" + COMBINED_SUFFIX ).get(COMBINED_TITLE);
        
        for ( Entry<String, Set<GeneOntologyTerm>> esSeries : allCombinedDirectSeries.entrySet() ) {
            String dateString = esSeries.getKey();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = null;
            try {
                date = format.parse(dateString);
            } catch ( ParseException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            
            
            Set<GeneOntologyTerm> terms = esSeries.getValue();
            
            for ( GeneOntologyTerm geneOntologyTerm : selected ) {
                Map<Date, Boolean> data = timelineData.get(geneOntologyTerm);
                Boolean exists = data.get( date );
                
                if ( exists == null || !exists ) {
                    data.put( date, terms.contains( geneOntologyTerm ) );
                }
                
            }
            
        }

        timelineModel = createTimeline(timelineData);

        
        
    }
    
    private TimelineModel createTimeline( Map<GeneOntologyTerm, Map<Date, Boolean>> timelineData) {
        
        TimelineModel model = new TimelineModel();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        
        List<TimelineGroup> groups = new ArrayList<TimelineGroup>();
        List<TimelineEvent> events = new ArrayList<TimelineEvent>();
        
        for ( Entry<GeneOntologyTerm, Map<Date, Boolean>> esTermData : timelineData.entrySet() ) {
            GeneOntologyTerm term = esTermData.getKey();
            
            Map<Date, Boolean> data = esTermData.getValue();
            
            TimelineGroup group = new TimelineGroup(term.getGoId(), term);
            groups.add(group);
            //model.addGroup( group );
            

            SortedSet<Date> dates = new TreeSet<Date>(data.keySet());
            Date prevDate = null;
            for (Date date : dates) {
                if ( prevDate != null ) {
                    boolean exists = data.get(prevDate);
                    TimelineEvent event = new TimelineEvent( df.format(prevDate), prevDate, date, false, term.getGoId(),
                            exists ? "timeline-true timeline-hidden" : "timeline-false timeline-hidden" );
                    //model.add( event );
                    events.add(event);
                    
                }

                prevDate = date;
            }
            
            // give the last edition a span of 1 month
            if ( dates.size() > 1) {
                boolean exists = data.get(prevDate);
                cal.setTime(prevDate);
                cal.add(Calendar.MONTH, 1);
                TimelineEvent event = new TimelineEvent( df.format(prevDate), prevDate, cal.getTime(), false, term.getGoId(),
                        exists ? "timeline-true timeline-hidden" : "timeline-false timeline-hidden" );
                //model.add( event );
                events.add(event);
            }
            
            
            

                   
        }
        
        model.setGroups( groups );
        model.addAll(events);
        
        return model;
        
    }
    
    public void cleanTimeline() {
        timelineModel = null;
    }
    
    public void  cleanFilteredTerms() {
        filteredTerms = null;
    }
    
    public void init() throws GeneNotFoundException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return; // Skip ajax requests.
        }
        System.out.println( "TrackView init: " + currentSpeciesId + ": " + query );
        Map<String, Collection<Accession>> c = cache.getSymbolToCurrentAccessions().get( currentSpeciesId );
        Collection<Accession> primaryAccessions;
        if ( query == null
                || currentSpeciesId == null
                || c == null
                || ( primaryAccessions = cache.getSymbolToCurrentAccessions().get( currentSpeciesId ).get( query ) ) == null ) {

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
            // currentEdition = cache.getCurrentEditions().get( currentSpeciesId );
            for ( Species s : cache.getSpeciesList() ) {
                if ( s.getId().equals( currentSpeciesId ) ) {
                    currentSpecies = s;
                }
            }

        }

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

        allCharts.put( identifier + COMBINED_SUFFIX, createChart( allSeriesCombined, title, yAxis, null ) );

        sData = new HashMap<String, Map<String, Set<GeneOntologyTerm>>>();
        for ( Entry<String, Map<Edition, Set<GeneOntologyTerm>>> es : allSeriesCombined.entrySet() ) {
            sData.put( es.getKey(), editionMapToDateMap( es.getValue() ) );
        }

        allSeriesData.put( identifier + COMBINED_SUFFIX, sData );
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
            itemSelectTerms = new HashSet<GeneOntologyTerm>();
        } else {
            itemSelectTerms = series.get( date );
        }

    }

    public void keepAlive() {
        System.out.println( "Kept alive" );
    }

    public void reloadGraph() {

        if ( graphType == null || graphType.equals( "" ) ) graphType = "direct";
        graphType = propagate ? "propagated" : "direct";
        System.out.println( graphType + ( splitAccessions ? "" : COMBINED_SUFFIX ) );
        currentChart = allCharts.get( graphType + ( splitAccessions ? "" : COMBINED_SUFFIX ) );
        seriesData = allSeriesData.get( graphType + ( splitAccessions ? "" : COMBINED_SUFFIX ) );
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

    public Map<String, Accession> getCurrentPrimaryAccessions() {
        return currentPrimaryAccessions;
    }

    public ArrayList<Accession> getCurrentPrimaryAccessionsValues() {
        return new ArrayList<Accession>( currentPrimaryAccessions.values() );
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

    public boolean isChartsReady() {
        return chartsReady;
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

    public List<GeneOntologyTerm> getItemSelectViewTerms() {
        return itemSelectViewTerms;
    }

    public void setItemSelectViewTerms( List<GeneOntologyTerm> itemSelectViewTerms ) {
        this.itemSelectViewTerms = itemSelectViewTerms;
    }

}
