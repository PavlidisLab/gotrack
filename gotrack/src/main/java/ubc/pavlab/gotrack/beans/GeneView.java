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
import org.primefaces.context.RequestContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
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
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.table.AnnotationValues;
import ubc.pavlab.gotrack.model.table.LossGainTableValues;
import ubc.pavlab.gotrack.model.table.LossGainTableValues.LossGain;
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

    // Right Panel
    private Collection<GeneOntologyTerm> allTerms = new HashSet<>();
    private List<GeneOntologyTerm> selectedTerms;
    private Collection<GeneOntologyTerm> filteredAllTerms;

    // View Annotations List
    private Collection<AnnotationValues> viewAnnotations = new ArrayList<>();
    private Collection<AnnotationValues> filteredViewAnnotations; // TODO use this
    private GeneOntologyTerm viewTerm;

    // Click event lists
    private Edition clickEdition;

    // Annotation clicks
    private Collection<GeneOntologyTerm> clickTerms = new HashSet<>();
    private List<GeneOntologyTerm> selectedClickTerms;
    private Collection<GeneOntologyTerm> filteredClickTerms;

    // Loss Gain Clicks
    private List<LossGainTableValues> clickLGTerms = new ArrayList<>();
    private List<LossGainTableValues> selectedClickLGTerms;
    private Collection<LossGainTableValues> filteredClickLGTerms;

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
            return null;

        }

    }

    /**
     * Attempt to get data from cache, if not in cache get from DB.
     */
    private Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> retrieveData() {
        return retrieveData( null );
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
        // We want the most recent of each term

        //allTerms = rawData.get( AnnotationType.DIRECT ).columnKeySet();
        allTerms = new HashSet<>();
        ArrayList<Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>>> reversedData = new ArrayList<>(
                rawData.get( AnnotationType.I ).rowMap().entrySet() );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : Lists.reverse( reversedData ) ) {

            allTerms.addAll( entry.getValue().keySet() );
        }

        allTerms = new ArrayList<>( allTerms );
        Collections.sort( ( List<GeneOntologyTerm> ) allTerms );

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
        Series aggregateSeries = new Series( "Species Direct Avg" );
        Series aggregateInferredSeries = new Series( "Species Inferred Avg" );
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

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title",
                "Terms Annotated to " + gene.getSymbol() + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Annotations Count" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );
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
        Series averageDirectSeries = new Series( "Direct Species Average" );

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
        Series averageInferredSeries = new Series( "Inferred Species Average" );

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

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title",
                "Similarity of " + gene.getSymbol() + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Jaccard Similarity Index" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );

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
        Series averageSeries = new Series( "Species Average" );
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

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title",
                "Multifunctionality of " + gene.getSymbol() + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Multifunctionality" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );
    }

    /**
     * Entry point for fetching the multifunctionality chart
     */
    public void fetchMultifunctionalityChart() {
        fetchMultifunctionalityChart( rawData );
    }

    /**
     * Create chart showing total number of terms lost and gained between editions (both directly and through
     * propagation)
     * 
     * @param rawData data
     */
    private void fetchLossGainChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchLossGainChart" );

        ChartValues chart = new ChartValues();

        // Calculate both the losses of GO terms and gains of GO terms between editions

        // For direct annotations
        Series directLossSeries = new Series( "Direct Loss" );
        Series directGainSeries = new Series( "Direct Gain" );
        directLossSeries.setExtra( 0 ); //Stack 0
        directGainSeries.setExtra( 0 ); //Stack 0

        Set<GeneOntologyTerm> previousGOSet = null;
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.D )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            Set<GeneOntologyTerm> currentGOSet = entry.getValue().keySet();

            if ( previousGOSet != null ) {
                Integer loss = -1 * Sets.difference( previousGOSet, currentGOSet ).size();
                Integer gain = Sets.difference( currentGOSet, previousGOSet ).size();
                if ( !( loss == 0 && gain == 0 ) ) {
                    directLossSeries.addDataPoint( ed.getDate(), loss );
                    directGainSeries.addDataPoint( ed.getDate(), gain );
                }
            }

            previousGOSet = currentGOSet;
        }

        chart.addSeries( directGainSeries );
        chart.addSeries( directLossSeries );

        // For inferred annotations

        Series inferredLossSeries = new Series( "Inferred Loss" );
        Series inferredGainSeries = new Series( "Inferred Gain" );
        inferredLossSeries.setExtra( 1 ); //Stack 1
        inferredGainSeries.setExtra( 1 ); //Stack 1

        previousGOSet = null;
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.I )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            Set<GeneOntologyTerm> currentGOSet = entry.getValue().keySet();

            if ( previousGOSet != null ) {
                Integer loss = -1 * Sets.difference( previousGOSet, currentGOSet ).size();
                Integer gain = Sets.difference( currentGOSet, previousGOSet ).size();
                if ( !( loss == 0 && gain == 0 ) ) {
                    inferredLossSeries.addDataPoint( ed.getDate(), loss );
                    inferredGainSeries.addDataPoint( ed.getDate(), gain );
                }
            }

            previousGOSet = currentGOSet;
        }

        chart.addSeries( inferredGainSeries );
        chart.addSeries( inferredLossSeries );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title",
                "Loss/Gain of " + gene.getSymbol() + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Change" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );

    }

    /**
     * Entry point for fetching the loss/gain chart
     */
    public void fetchLossGainChart() {
        fetchLossGainChart( rawData );

    }

    /**
     * Filter charts to include only those terms selected from right panel
     */
    public void filterCharts() {
        if ( selectedTerms == null || selectedTerms.size() == 0 ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_filtered", false );
            return;
        }

        HashSet<GeneOntologyTerm> filterTerms = new HashSet<>( selectedTerms );

        rawData = retrieveData( filterTerms );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_filtered", true );

    }

    /**
     * Remove all filters on charts
     */
    public void resetCharts() {
        rawData = retrieveData();
        RequestContext.getCurrentInstance().addCallbackParam( "hc_filtered", true );
    }

    /**
     * Entry point for fetching data to create a gantt chart of annotations categories over time
     */
    public void fetchTimeline() {
        log.debug( "fetchTimeline" );
        if ( selectedTerms == null || selectedTerms.size() == 0 ) {
            // No Terms
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_info", "No Terms Selected." );
            return;
        }

        if ( selectedTerms.size() > 20 ) {
            // Too many terms
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_info", "Too Many Terms Selected." );
            return;
        }

        HashSet<GeneOntologyTerm> filterTerms = new HashSet<>( selectedTerms );

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

        for ( GeneOntologyTerm t : filterTerms ) {
            termNames.put( t.getGoId(), t.getName() );
            Series s = new Series( t.getGoId() );
            s.setExtra( t.getName() );
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

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title",
                "Annotation Categories of " + gene.getSymbol() + " vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_category_positions",
                new Gson().toJson( categoryPositions ) );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_term_names", new Gson().toJson( termNames ) );

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
     * Click event functionality for annotation chart
     */
    public void fetchAnnotationPointData() {
        log.debug( "fetchAnnotationPointData" );
        Integer editionId;
        try {

            editionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        } catch ( NumberFormatException e ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            return;
        }

        clickEdition = cache.getEdition( species.getId(), editionId );

        try {
            clickTerms = rawData.get( AnnotationType.I ).row( clickEdition ).keySet();
        } catch ( NullPointerException e ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            return;
        }

        selectedClickTerms = null;
        filteredClickTerms = null;

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );

    }

    /**
     * Click event functionality for loss/gain chart
     */
    public void fetchLossGainPointData() {
        log.debug( "fetchLossGainPointData" );
        Integer editionId;
        try {

            editionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        } catch ( NumberFormatException e ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            return;
        }

        clickEdition = cache.getEdition( species.getId(), editionId );

        // get previous edition, yes this is ugly. I don't wanna talk about it.

        Edition previousEdition = null;
        for ( Edition e : rawData.get( AnnotationType.I ).rowMap().keySet() ) {
            if ( e.equals( clickEdition ) ) {
                break;
            }
            previousEdition = e;
        }

        if ( previousEdition == null ) {
            clickLGTerms = Lists.newArrayList();
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            return;
        }

        try {
            // Direct 
            clickLGTerms = Lists.newArrayList();

            ImmutableSet<GeneOntologyTerm> currentGOSet = rawData.get( AnnotationType.D ).row( clickEdition )
                    .keySet();

            ImmutableSet<GeneOntologyTerm> previousGOSet = rawData.get( AnnotationType.D ).row( previousEdition )
                    .keySet();

            for ( GeneOntologyTerm t : Sets.difference( previousGOSet, currentGOSet ) ) {
                LossGainTableValues lg = new LossGainTableValues( t, LossGain.LOSS, AnnotationType.D );
                clickLGTerms.add( lg );
            }

            for ( GeneOntologyTerm t : Sets.difference( currentGOSet, previousGOSet ) ) {
                LossGainTableValues lg = new LossGainTableValues( t, LossGain.GAIN, AnnotationType.D );
                clickLGTerms.add( lg );
            }

            // Inferred 

            currentGOSet = rawData.get( AnnotationType.I ).row( clickEdition )
                    .keySet();

            previousGOSet = rawData.get( AnnotationType.I ).row( previousEdition )
                    .keySet();

            for ( GeneOntologyTerm t : Sets.difference( previousGOSet, currentGOSet ) ) {
                LossGainTableValues lg = new LossGainTableValues( t, LossGain.LOSS, AnnotationType.I );
                clickLGTerms.add( lg );
            }

            for ( GeneOntologyTerm t : Sets.difference( currentGOSet, previousGOSet ) ) {
                LossGainTableValues lg = new LossGainTableValues( t, LossGain.GAIN, AnnotationType.I );
                clickLGTerms.add( lg );
            }

            Collections.sort( clickLGTerms );

        } catch ( NullPointerException e ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            return;
        }

        selectedClickLGTerms = null;
        filteredClickLGTerms = null;

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );

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
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
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

    public Collection<GeneOntologyTerm> getClickTerms() {
        return clickTerms;
    }

    public Edition getClickEdition() {
        return clickEdition;
    }

    public List<GeneOntologyTerm> getSelectedClickTerms() {
        return selectedClickTerms;
    }

    public void setSelectedClickTerms( List<GeneOntologyTerm> selectedClickTerms ) {
        this.selectedClickTerms = selectedClickTerms;
    }

    public Collection<GeneOntologyTerm> getFilteredClickTerms() {
        return filteredClickTerms;
    }

    public void setFilteredClickTerms( Collection<GeneOntologyTerm> filteredClickTerms ) {
        this.filteredClickTerms = filteredClickTerms;
    }

    public Collection<LossGainTableValues> getClickLGTerms() {
        return clickLGTerms;
    }

    public List<LossGainTableValues> getSelectedClickLGTerms() {
        return selectedClickLGTerms;
    }

    public void setSelectedClickLGTerms( List<LossGainTableValues> selectedClickLGTerms ) {
        this.selectedClickLGTerms = selectedClickLGTerms;
    }

    public Collection<LossGainTableValues> getFilteredClickLGTerms() {
        return filteredClickLGTerms;
    }

    public void setFilteredClickLGTerms( Collection<LossGainTableValues> filteredClickLGTerms ) {
        this.filteredClickLGTerms = filteredClickLGTerms;
    }

    public Collection<GeneOntologyTerm> getAllTerms() {
        return allTerms;
    }

    public List<GeneOntologyTerm> getSelectedTerms() {
        return selectedTerms;
    }

    public void setSelectedTerms( List<GeneOntologyTerm> selectedTerms ) {
        this.selectedTerms = selectedTerms;
    }

    public Collection<GeneOntologyTerm> getFilteredAllTerms() {
        return filteredAllTerms;
    }

    public void setFilteredAllTerms( Collection<GeneOntologyTerm> filteredAllTerms ) {
        this.filteredAllTerms = filteredAllTerms;
    }

}
