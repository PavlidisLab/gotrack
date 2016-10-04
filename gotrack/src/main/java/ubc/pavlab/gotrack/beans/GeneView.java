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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.StatsService;
import ubc.pavlab.gotrack.exception.GeneNotFoundException;
import ubc.pavlab.gotrack.model.Aggregate;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationType;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.chart.SeriesExtra;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.table.AnnotationValues;
import ubc.pavlab.gotrack.model.table.CompareTermsTableValues;
import ubc.pavlab.gotrack.model.table.CompareTermsTableValues.TermComparison;
import ubc.pavlab.gotrack.model.table.GeneTableValues;
import ubc.pavlab.gotrack.utilities.Jaccard;

/**
 * Backing bean for the gene tracking functionality.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ViewScoped
public class GeneView implements Serializable {

    private static final long serialVersionUID = -585608351928148001L;

    private static final Logger log = Logger.getLogger( GeneView.class );

    private static int MULTIFUNCTIONALITY_SCALE = 10000;

    @Inject
    private Cache cache;

    @Inject
    private StatsService statsService;

    @Inject
    private AnnotationService annotationService;

    private Species species;
    private String query;
    private Gene gene;

    // Data
    private Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData;
    private boolean filtered = false;

    // Meta
    private Edition currentEdition;

    // Right Panel
    private Collection<GeneTableValues> allTerms = new HashSet<>();
    private List<GeneTableValues> selectedTerms;
    private Collection<GeneTableValues> filteredAllTerms;

    // View Annotations List
    private Collection<AnnotationValues> viewAnnotations = new ArrayList<>();
    private Collection<AnnotationValues> filteredViewAnnotations; // TODO use this
    private GeneOntologyTerm viewTerm;

    // Click event lists
    private Edition clickEdition;

    // Range select event lists
    private Edition selectEditionStart;
    private Edition selectEditionStop;

    // Annotation clicks
    private Collection<CompareTermsTableValues> allClickTerms = new HashSet<>();
    private Collection<CompareTermsTableValues> clickTerms = new HashSet<>();
    private List<CompareTermsTableValues> selectedClickTerms;
    private Collection<CompareTermsTableValues> filteredClickTerms;

    // Annotation Click Meta
    private int totalTerms;
    private int gainTerms;
    private int lossTerms;

    // Annotation Clicks TermComparison Selector
    private TermComparison termComparisonFilter = null;

    public GeneView() {
        log.info( "GeneView created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
    }

    /**
     * pre-render view
     * 
     * This is kept lightweight so that the page loads quickly and lazy loads the data using remote commands
     */
    public String init() throws GeneNotFoundException, IOException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "GeneView init: " + species + ": " + query );

        boolean isGeneSet = !StringUtils.isEmpty( query );
        boolean isSpeciesSet = species != null;

        if ( !isSpeciesSet && !isGeneSet ) {
            // no parameters; show search form
            gene = null;
            return null;
        } else if ( !( isSpeciesSet && isGeneSet ) ) {
            // malformed input if only one parameter is specified
            // Send to error page
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.getExternalContext().responseSendError( 400, "Missing Parameter" );
            facesContext.responseComplete();
            return null;
        }

        // display development message
        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            FacesContext.getCurrentInstance().addMessage( "betaMessage", new FacesMessage( FacesMessage.SEVERITY_WARN,
                    "This is the DEVELOPMENT version of GOTrack!", null ) );
        }

        gene = cache.getCurrentGene( species.getId(), query );
        if ( gene == null ) {
            // gene symbol not found
            // Send to error page
            throw new GeneNotFoundException();
        } else {
            // Count gene hit
            statsService.countGeneHit( gene );
            log.info( "Gene: " + gene );

        }

        currentEdition = cache.getCurrentEditions( species.getId() );

        return null;

    }

    /**
     * Attempt to get data from cache, if not in cache get from DB.
     */
    private Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> retrieveData() {
        return retrieveData( Sets.<GeneOntologyTerm> newHashSet() );
    }

    /**
     * Attempt to get data from cache, if not in cache get from DB. Afterwards apply filter.
     */
    private Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> retrieveData(
            Collection<GeneTableValues> filterTableValues ) {
        Set<GeneOntologyTerm> filterTerms = Sets.newHashSet();
        for ( GeneTableValues tv : filterTableValues ) {
            filterTerms.add( tv.getTerm() );
        }
        return retrieveData( filterTerms );
    }

    /**
     * Attempt to get data from cache, if not in cache get from DB. Afterwards apply filter.
     */
    private Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> retrieveData(
            Set<GeneOntologyTerm> filterTerms ) {

        boolean bypassFilter = ( filterTerms == null || filterTerms.size() == 0 );

        Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData = new HashMap<>();

        Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> cachedData = cache
                .getGeneData( gene );

        // Speedy return if no filters are required and data was cached
        if ( cachedData != null && bypassFilter ) {
            return cachedData;
        }

        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> directRawData = null;
        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> inferredRawData = null;

        // Get unfiltered data
        if ( cachedData == null ) {
            directRawData = annotationService.fetchTrackData( species, gene );
            inferredRawData = propagate( directRawData );
        } else {
            directRawData = cachedData.get( AnnotationType.D ).rowMap();
            inferredRawData = cachedData.get( AnnotationType.I ).rowMap();
        }

        ImmutableTable.Builder<Edition, GeneOntologyTerm, Set<Annotation>> rawdataBuilder = new ImmutableTable.Builder<Edition, GeneOntologyTerm, Set<Annotation>>();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : directRawData.entrySet() ) {
            Edition ed = entry.getKey();
            for ( Entry<GeneOntologyTerm, Set<Annotation>> entry2 : entry.getValue().entrySet() ) {
                GeneOntologyTerm t = entry2.getKey();
                if ( bypassFilter || filterTerms.contains( t ) ) {
                    rawdataBuilder.put( ed, t, entry2.getValue() );
                }

            }

        }
        rawdataBuilder.orderRowsBy( new Comparator<Edition>() {
            @Override
            public int compare( Edition o1, Edition o2 ) {
                return o1.compareTo( o2 );
            }
        } );

        rawData.put( AnnotationType.D, rawdataBuilder.build() );

        rawdataBuilder = new ImmutableTable.Builder<Edition, GeneOntologyTerm, Set<Annotation>>();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : inferredRawData.entrySet() ) {
            Edition ed = entry.getKey();
            for ( Entry<GeneOntologyTerm, Set<Annotation>> entry2 : entry.getValue().entrySet() ) {
                GeneOntologyTerm t = entry2.getKey();
                if ( bypassFilter || filterTerms.contains( t ) ) {
                    rawdataBuilder.put( ed, t, entry2.getValue() );
                }

            }

        }
        rawdataBuilder.orderRowsBy( new Comparator<Edition>() {
            @Override
            public int compare( Edition o1, Edition o2 ) {
                return o1.compareTo( o2 );
            }
        } );
        rawData.put( AnnotationType.I, rawdataBuilder.build() );

        // If no data existed in cache and we did not filter it then cache the data
        if ( cachedData == null && bypassFilter ) {
            cache.addGeneData( gene, rawData );
        }

        return rawData;

    }

    /**
     * Entry point for initiating the retrieval of necessary data.
     */
    public void fetchData() {
        log.debug( "fetchData" );

        // retrieve data nad apply no filters
        rawData = retrieveData();

        // A map that will be needed in the front end for drilling down
        Map<Long, Integer> dateToEdition = new HashMap<>();

        for ( Edition ed : rawData.get( AnnotationType.I ).rowKeySet() ) {
            dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( dateToEdition ) );

        // Now create a list of terms that will be displayed in a front-end right panel
        // Cannot simply use columnKeySet as there are no guarantees as to which terms will be chosen
        // We want the most recent of each term.

        //allTerms = rawData.get( AnnotationType.DIRECT ).columnKeySet();
        allTerms = new HashSet<>();
        ArrayList<Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>>> data = new ArrayList<>(
                rawData.get( AnnotationType.I ).rowMap().entrySet() );

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : Lists.reverse( data ) ) {
            Set<GeneOntologyTerm> terms = entry.getValue().keySet();
            Edition ed = entry.getKey();
            int age = currentEdition.getEdition() - ed.getEdition();
            for ( GeneOntologyTerm t : terms ) {
                if ( !allTerms.contains( t ) ) {
                    // Only keep the most recent
                    allTerms.add( new GeneTableValues( ed, t, age ) );
                }
            }
        }

        allTerms = new ArrayList<>( allTerms );
        Collections.sort( ( List<GeneTableValues> ) allTerms );

    }

    /**
     * Propagates the annotations from the retrieved data to ancestor terms.
     */
    private Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> propagate(
            Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> map ) {
        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> propagatedData = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : map.entrySet() ) {
            Edition ed = entry.getKey();
            propagatedData.put( ed, cache.propagateAnnotations( entry.getValue(), ed ) );
        }
        return propagatedData;
    }

    private Map<String, Object> createHCCallbackParamFail( String info ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", false );
        hcGsonMap.put( "info", info );
        return hcGsonMap;
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
     * Create chart showing counts of unique terms annotated to this gene over time (both directly and through
     * propagation)
     * 
     * @param rawData data
     */
    private void fetchAnnotationChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchAnnotationChart" );

        // Collect data from cache about species aggregates
        //        Map<Edition, Aggregate> aggregates = cache.getAggregates( species.getId() );

        ChartValues chart = new ChartValues();

        //        // Create series for species aggregates
        //        Series aggregateSeries = new Series( "Species Direct Avg" );
        //        for ( Entry<Edition, Aggregate> entry : aggregates.entrySet() ) {
        //            aggregateSeries.addDataPoint( entry.getKey().getDate(), entry.getValue().getAvgDirectByGene() );
        //        }
        //        chart.addSeries( aggregateSeries );

        //Create series for direct annotations count
        Series directCountSeries = new Series( "Direct Annotation Count" );
        SeriesExtra aggregateSeries = new SeriesExtra( "Species Direct Mean" );
        aggregateSeries.putExtra( "color", "#939393" );
        SeriesExtra aggregateInferredSeries = new SeriesExtra( "Species Inferred Mean" );
        aggregateInferredSeries.putExtra( "color", "#939393" );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.D )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            int count = entry.getValue().size();
            directCountSeries.addDataPoint( ed.getDate(), count );

            // Averages
            Aggregate agg = cache.getAggregates( species.getId(), ed );
            if ( agg != null ) {
                aggregateSeries.addDataPoint( ed.getDate(), agg.getAvgDirectByGene() );
                aggregateInferredSeries.addDataPoint( ed.getDate(), agg.getAvgInferredByGene() );
            }

        }
        chart.addSeries( aggregateSeries );
        chart.addSeries( aggregateInferredSeries );
        chart.addSeries( directCountSeries );

        // Create series for inferred annotations count
        Series inferredCountSeries = new Series( "Inferred Annotation Count" );

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.I )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            int count = entry.getValue().size();
            inferredCountSeries.addDataPoint( ed.getDate(), count );
        }
        chart.addSeries( inferredCountSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( "Terms Annotated to " + gene.getSymbol() + " vs Time",
                "Annotations Count", "Date", 0, null, chart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point for fetching the annotation chart
     */
    public void fetchAnnotationChart() {
        fetchAnnotationChart( rawData );

    }

    /**
     * Create chart showing similarity of terms annotated to this gene in an edition compared to the most current
     * edition (both directly and through propagation)
     * 
     * @param rawData data
     */
    private void fetchJaccardChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchJaccardChart" );

        ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>> directData = rawData.get( AnnotationType.D );
        ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>> inferredData = rawData
                .get( AnnotationType.I );

        ChartValues chart = new ChartValues();

        // Create jaccard between edition and current edition
        Edition currentEdition = Collections.max( directData.rowKeySet() );

        // For direct annotations
        Series directSeries = new Series( "Direct Similarity" );
        SeriesExtra averageDirectSeries = new SeriesExtra( "Direct Species Mean" );
        averageDirectSeries.putExtra( "color", "#939393" );

        Set<GeneOntologyTerm> currentGOSet = directData.row( currentEdition ).keySet();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : directData.rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            Double jaccard = Jaccard.similarity( entry.getValue().keySet(), currentGOSet );
            directSeries.addDataPoint( ed.getDate(), jaccard );

            // Averages
            Aggregate agg = cache.getAggregates( species.getId(), ed );
            if ( agg != null ) {
                averageDirectSeries.addDataPoint( ed.getDate(), agg.getAvgDirectSimilarity() );
            }
        }

        // For Inferred annotations
        Series inferredSeries = new Series( "Inferred Similarity" );
        SeriesExtra averageInferredSeries = new SeriesExtra( "Inferred Species Average" );
        averageInferredSeries.putExtra( "color", "#939393" );

        currentGOSet = inferredData.row( currentEdition ).keySet();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : inferredData.rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            Double jaccard = Jaccard.similarity( entry.getValue().keySet(), currentGOSet );
            inferredSeries.addDataPoint( ed.getDate(), jaccard );

            // Averages
            Aggregate agg = cache.getAggregates( species.getId(), ed );
            if ( agg != null ) {
                averageInferredSeries.addDataPoint( ed.getDate(), agg.getAvgInferredSimilarity() );
            }
        }

        chart.addSeries( averageDirectSeries );
        chart.addSeries( averageInferredSeries );
        chart.addSeries( directSeries );
        chart.addSeries( inferredSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( "Similarity of " + gene.getSymbol() + " vs Time",
                "Jaccard Similarity Index", "Date", 0, 1, chart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point for fetching the similarity chart
     */
    public void fetchJaccardChart() {
        fetchJaccardChart( rawData );

    }

    /**
     * Create chart showing multifunctionality of this gene over time (Gillis J, Pavlidis P (2011) The Impact of
     * Multifunctional Genes on "Guilt by Association" Analysis. PLoS ONE 6(2): e17258. doi:
     * 10.1371/journal.pone.0017258)
     * 
     * @param rawData data
     */
    private void fetchMultifunctionalityChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchMultifunctionalityChart" );

        ChartValues chart = new ChartValues();

        // Calculate multifunctionality of the gene in each edition
        Series multiSeries = new Series( "Multifunctionality" );
        SeriesExtra averageSeries = new SeriesExtra( "Multifunctionality Species Mean" );
        averageSeries.putExtra( "color", "#939393" );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.I )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            Integer total = cache.getGeneCount( species.getId(), ed );
            if ( total != null ) {
                Double multi = 0.0;
                for ( GeneOntologyTerm t : entry.getValue().keySet() ) {
                    Integer inGroup = cache.getInferredAnnotationCount( species.getId(), ed, t );
                    if ( inGroup != null && inGroup < total ) {
                        multi += 1.0 / ( inGroup * ( total - inGroup ) );
                    }
                }
                // Scaled by 10^4
                multiSeries.addDataPoint( ed.getDate(), MULTIFUNCTIONALITY_SCALE * multi );
            }

            // Averages
            Aggregate agg = cache.getAggregates( species.getId(), ed );
            if ( agg != null ) {
                // Scaled by 10^4
                averageSeries.addDataPoint( ed.getDate(), MULTIFUNCTIONALITY_SCALE * agg.getAvgMultifunctionality() );
            }
        }
        chart.addSeries( averageSeries );
        chart.addSeries( multiSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap(
                "Multifunctionality of " + gene.getSymbol() + " vs Time", "Multifunctionality [10^-5]",
                "Date", 0, null, chart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point for fetching the multifunctionality chart
     */
    public void fetchMultifunctionalityChart() {
        fetchMultifunctionalityChart( rawData );
    }

    /**
     * Filter charts to include only those terms selected from right panel
     */
    public void filterCharts() {
        if ( selectedTerms == null || selectedTerms.size() == 0 ) {
            RequestContext.getCurrentInstance().addCallbackParam( "filtered", false );
            return;
        }

        HashSet<GeneTableValues> filterTerms = new HashSet<>( selectedTerms );

        rawData = retrieveData( filterTerms );
        filtered = true;

        RequestContext.getCurrentInstance().addCallbackParam( "filtered", true );

    }

    /**
     * Remove all filters on charts
     */
    public void resetCharts() {
        rawData = retrieveData();
        filtered = false;
        RequestContext.getCurrentInstance().addCallbackParam( "filtered", true );
    }

    /**
     * Entry point for fetching data to create a gantt chart of annotations categories over time
     */
    public void fetchTimeline() {
        log.debug( "fetchTimeline" );
        if ( selectedTerms == null || selectedTerms.size() == 0 ) {
            // No Terms
            RequestContext.getCurrentInstance().addCallbackParam( "HC",
                    new Gson().toJson( createHCCallbackParamFail( "No Terms Selected." ) ) );
            return;
        }

        if ( selectedTerms.size() > 20 ) {
            // Too many terms
            RequestContext.getCurrentInstance().addCallbackParam( "HC",
                    new Gson().toJson( createHCCallbackParamFail( "Too Many Terms Selected. Maximum 20." ) ) );
            return;
        }

        HashSet<GeneTableValues> filterTerms = new HashSet<>( selectedTerms );

        ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>> data = retrieveData( filterTerms )
                .get( AnnotationType.I );

        // Create an ordering for the categories
        int i = 0;
        Map<String, Integer> categoryPositions = new HashMap<>();
        for ( String cat : cache.getEvidenceCategories() ) {
            categoryPositions.put( cat, i++ );
        }

        ChartValues chart = new ChartValues();

        List<Edition> allEditions = new ArrayList<>( cache.getAllEditions( species.getId() ) );
        Collections.sort( allEditions );

        Map<String, String> termNames = new HashMap<>();

        for ( GeneTableValues tv : filterTerms ) {
            GeneOntologyTerm t = tv.getTerm();
            termNames.put( t.getGoId(), t.getName() );
            Series s = new Series( t.getGoId() );
            ImmutableMap<Edition, Set<Annotation>> termData = data.column( t );
            // We iterate over this collection to insure that every term has all of the editions in its data
            // otherwise we get wonky date ranges
            for ( Edition ed : allEditions ) {
                byte existenceByte = 0;
                Set<Annotation> annotationSet = termData.get( ed );
                if ( annotationSet != null ) {
                    for ( Annotation annotation : annotationSet ) {
                        int pos = categoryPositions.get( annotation.getEvidence().getCategory() );
                        // Set bit in this position to 1
                        existenceByte |= ( 1 << pos );
                    }
                }

                s.addDataPoint( ed.getDate(), existenceByte );
            }
            chart.addSeries( s );
        }

        Map<String, Object> hcGsonMap = createHCCallbackParamMap(
                "Annotation Categories of " + gene.getSymbol() + " vs Time",
                "", "Date", null, null, chart );
        hcGsonMap.put( "category_positions", categoryPositions );
        hcGsonMap.put( "term_names", termNames );
        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Entry point to view a terms annotations in a specific edition. This is reached by clicking a term in a table of
     * terms after clicking a data point in the annotation chart.
     */
    public void fetchAnnotations() {
        log.debug( "fetchAnnotations" );
        //        if ( selectedClickTerms == null || selectedClickTerms.isEmpty() ) {
        //            return;
        //        }
        //viewTerm = selectedClickTerms.iterator().next();
        log.debug( viewTerm );

        Set<Annotation> data = rawData.get( AnnotationType.D ).get( clickEdition, viewTerm );

        viewAnnotations = new HashSet<>();
        if ( data != null ) {

            // Add direct annotations
            for ( Annotation annotation : data ) {
                viewAnnotations.add( new AnnotationValues( annotation, AnnotationType.D ) );
            }
        }
        data = rawData.get( AnnotationType.I ).get( clickEdition, viewTerm );

        if ( data != null ) {

            // Next add the inferred as they will not overwrite the direct if the direct already exists
            for ( Annotation annotation : data ) {
                viewAnnotations.add( new AnnotationValues( annotation, AnnotationType.I ) );
            }
        }

        filteredViewAnnotations = null;
    }

    /**
     * Range select event functionality for annotation chart
     */
    public void fetchAnnotationRangeData() {
        log.debug( "fetchAnnotationRangeData" );
        Integer editionId1;
        Integer editionId2;
        try {

            editionId1 = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition1" ) );
            editionId2 = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition2" ) );

        } catch ( NumberFormatException e ) {
            log.error( e );
            return;
        }

        selectEditionStart = cache.getEdition( species.getId(), editionId1 );
        if ( selectEditionStart == null ) {
            log.warn( "selectEditionStart edition id has no corresponding edition object" );
            return;
        }

        selectEditionStop = cache.getEdition( species.getId(), editionId2 );
        if ( selectEditionStop == null ) {
            log.warn( "selectEditionStop edition id has no corresponding edition object" );
            return;
        }

        try {
            allClickTerms = compareEditions( selectEditionStop, selectEditionStart );
        } catch ( NullPointerException e ) {
            log.error( e );
            return;
        }

    }

    /**
     * Click event functionality for annotation chart
     */
    public void fetchAnnotationPointData() {
        log.debug( "fetchAnnotationPointData" );
        Integer editionId;
        try {

            editionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        } catch ( NumberFormatException e ) {
            log.error( e );
            return;
        }

        clickEdition = cache.getEdition( species.getId(), editionId );

        if ( clickEdition == null ) {
            log.warn( "Selected edition id has no corresponding edition object" );
            return;
        }

        try {
            allClickTerms = compareEditions( clickEdition );
        } catch ( NullPointerException e ) {
            log.error( e );
            return;
        }

    }

    /**
     * Click event functionality for annotation chart
     */
    public void fetchFilteredAnnotationClickTerms() {
        if ( termComparisonFilter != null ) {
            Predicate<CompareTermsTableValues> predicate = new Predicate<CompareTermsTableValues>() {
                @Override
                public boolean apply( CompareTermsTableValues input ) {
                    return input.getComparison().equals( termComparisonFilter );
                }
            };
            clickTerms = Collections2.filter( allClickTerms, predicate );
        } else {
            clickTerms = allClickTerms;
        }
        selectedClickTerms = null;
        filteredClickTerms = null;

        // This prevents datatable filter issues
        DataTable dataTable = ( DataTable ) FacesContext.getCurrentInstance().getViewRoot()
                .findComponent( ":clickTermsForm:clickTermsTable" );
        dataTable.reset();
        // Resets custom filter inputs
        RequestContext.getCurrentInstance().reset( "clickTermsForm:clickTermsTable" );
    }

    /**
     * Click event functionality for clicking timeline gantt chart
     */
    public void fetchTimelinePointData() {
        Integer editionId;
        try {

            editionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        } catch ( NumberFormatException e ) {
            log.error( e );
            return;
        }
        clickEdition = cache.getEdition( species.getId(), editionId );

        String goId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "termId" );

        viewTerm = cache.getTerm( clickEdition, goId );

        Set<Annotation> data = rawData.get( AnnotationType.D ).get( clickEdition, viewTerm );

        viewAnnotations = new HashSet<>();
        if ( data != null ) {

            // Add direct annotations
            for ( Annotation annotation : data ) {
                viewAnnotations.add( new AnnotationValues( annotation, AnnotationType.D ) );
            }
        }
        data = rawData.get( AnnotationType.I ).get( clickEdition, viewTerm );

        if ( data != null ) {

            // Next add the inferred as they will not overwrite the direct if the direct already exists
            for ( Annotation annotation : data ) {
                viewAnnotations.add( new AnnotationValues( annotation, AnnotationType.I ) );
            }
        }

        filteredViewAnnotations = null;

    }

    /**
     * Compare the loss and gain of GO Terms and edition and the edition before it.
     */
    private Collection<CompareTermsTableValues> compareEditions( Edition ed ) {
        // get previous edition, yes this is ugly. I don't wanna talk about it.
        Edition previousEdition = null;
        for ( Edition e : rawData.get( AnnotationType.I ).rowMap().keySet() ) {
            if ( e.equals( ed ) ) {
                break;
            }
            previousEdition = e;
        }

        if ( previousEdition == null ) {
            log.warn( "Selected Edition on has no previous edition to compare to." );
            return compareEditions( ed, ed );
        }
        return compareEditions( ed, previousEdition );
    }

    /**
     * Compare the loss and gain of GO Terms between two editions.
     * Gains : GO Terms present in ed1 but not ed2.
     * Loss: GO Terms present in ed2 but not ed1.
     */
    private Collection<CompareTermsTableValues> compareEditions( Edition ed1, Edition ed2 ) {
        List<CompareTermsTableValues> results = Lists.newArrayList();
        totalTerms = 0;
        gainTerms = 0;
        lossTerms = 0;
        try {
            ImmutableSet<GeneOntologyTerm> set1 = rawData.get( AnnotationType.I ).row( ed1 ).keySet();

            ImmutableSet<GeneOntologyTerm> set2 = rawData.get( AnnotationType.I ).row( ed2 ).keySet();
            for ( GeneOntologyTerm t : set1 ) {
                if ( set2.contains( t ) ) {
                    results.add( new CompareTermsTableValues( t, TermComparison.CONSTANT ) );
                    totalTerms++;
                } else {
                    results.add( new CompareTermsTableValues( t, TermComparison.GAIN ) );
                    totalTerms++;
                    gainTerms++;
                }
            }

            for ( GeneOntologyTerm t : Sets.difference( set2, set1 ) ) {
                results.add( new CompareTermsTableValues( t, TermComparison.LOSS ) );
                lossTerms++;
            }

            Collections.sort( results );

        } catch ( NullPointerException e ) {
            log.error( e );
            return Lists.newArrayList();
        }
        return results;
    }

    // Getters & Setters

    public Species getSpecies() {
        return species;
    }

    public void setSpecies( Species species ) {
        this.species = species;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public Gene getGene() {
        return gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    public Collection<AnnotationValues> getViewAnnotations() {
        return viewAnnotations;
    }

    public Collection<AnnotationValues> getFilteredViewAnnotations() {
        return filteredViewAnnotations;
    }

    public GeneOntologyTerm getViewTerm() {
        return viewTerm;
    }

    public void setViewTerm( GeneOntologyTerm viewTerm ) {
        this.viewTerm = viewTerm;
    }

    public Edition getClickEdition() {
        return clickEdition;
    }

    public Edition getSelectEditionStart() {
        return selectEditionStart;
    }

    public Edition getSelectEditionStop() {
        return selectEditionStop;
    }

    public Collection<CompareTermsTableValues> getClickTerms() {
        return clickTerms;
    }

    public Collection<CompareTermsTableValues> getAllClickTerms() {
        return allClickTerms;
    }

    public List<CompareTermsTableValues> getSelectedClickTerms() {
        return selectedClickTerms;
    }

    public void setSelectedClickTerms( List<CompareTermsTableValues> selectedClickTerms ) {
        this.selectedClickTerms = selectedClickTerms;
    }

    public Collection<CompareTermsTableValues> getFilteredClickTerms() {
        return filteredClickTerms;
    }

    public void setFilteredClickTerms( Collection<CompareTermsTableValues> filteredClickTerms ) {
        this.filteredClickTerms = filteredClickTerms;
    }

    public Collection<GeneTableValues> getAllTerms() {
        return allTerms;
    }

    public List<GeneTableValues> getSelectedTerms() {
        return selectedTerms;
    }

    public void setSelectedTerms( List<GeneTableValues> selectedTerms ) {
        this.selectedTerms = selectedTerms;
    }

    public Collection<GeneTableValues> getFilteredAllTerms() {
        return filteredAllTerms;
    }

    public void setFilteredAllTerms( Collection<GeneTableValues> filteredAllTerms ) {
        this.filteredAllTerms = filteredAllTerms;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public TermComparison getTermComparisonFilter() {
        return termComparisonFilter;
    }

    public void setTermComparisonFilter( TermComparison termComparisonFilter ) {
        this.termComparisonFilter = termComparisonFilter;
    }

    public int getTotalTerms() {
        return totalTerms;
    }

    public int getGainTerms() {
        return gainTerms;
    }

    public int getLossTerms() {
        return lossTerms;
    }

}
