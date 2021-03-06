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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.MultifunctionalityService;
import ubc.pavlab.gotrack.beans.service.StatsService;
import ubc.pavlab.gotrack.exception.GeneNotFoundException;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.chart.SeriesExtra;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.table.GeneViewRightPanelRow;
import ubc.pavlab.gotrack.utilities.Jaccard;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backing bean for the gene tracking functionality.
 *
 * @author mjacobson
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

    @Inject
    private MultifunctionalityService multifunctionalityService;

    @Getter
    @Setter
    private String queryAccession;

    @Getter
    @Setter
    private Gene gene;

    // Data
    private Map<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> annotationData;

    // Meta
    private Edition currentEdition;

    // Right Panel
    @Getter
    @Setter
    private List<GeneViewRightPanelRow> rightPanelTerms = Lists.newArrayList();
    @Getter
    @Setter
    private List<GeneViewRightPanelRow> rightPanelSelectedTerms;
    @Getter
    @Setter
    private Collection<GeneViewRightPanelRow> rightPanelFilteredTerms;
    @Getter
    private Edition rightPanelEdition;

    // Necessary to keep default filters from clearing on table update
    @Getter
    private Object[] rightPanelFilters = new Object[] {"",""};

    // Comparisons
    @Getter
    private List<Edition> comparisons = Lists.newArrayList();

    // Right Panel Click
//    private Collection<Annotation> rightPanelAnnotations = Sets.newHashSet();

    // View Annotations List
    @Getter
    private Collection<Annotation> viewAnnotations = new ArrayList<>();
    @Getter
    private Collection<Annotation> filteredViewAnnotations; // TODO use this
    @Getter
    @Setter
    private GeneOntologyTerm viewTerm;

    // Click event lists
    @Getter
    private Edition clickEdition;

    public GeneView() {
        log.info( "GeneView created" );
        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
                + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
    }

    /**
     * pre-render view
     * <p>
     * This is kept lightweight so that the page loads quickly and lazy loads the data using remote commands
     */
    public String init() throws GeneNotFoundException, IOException {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "GeneView init: " + queryAccession + " - " + gene );

        boolean isGeneSet = !StringUtils.isEmpty( queryAccession );

        if ( !isGeneSet ) {
            // no parameters; show search form
            gene = null;
            return null;
        }

        // display development message
        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            FacesContext.getCurrentInstance().addMessage( "betaMessage", new FacesMessage( FacesMessage.SEVERITY_WARN,
                    "This is the DEVELOPMENT version of GOTrack!", "" ) );
        }

        gene = cache.getCurrentGene( queryAccession );

        if ( gene == null ) {
            // gene accession not found
            // Send to error page
            throw new GeneNotFoundException();
        }

        // Count gene hit
        statsService.countGeneHit( gene );
        log.info( "Gene: " + gene );

        currentEdition = cache.getCurrentEditions( gene.getSpecies() );
        rightPanelEdition = currentEdition;

        return null;

    }

    private Map<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> fetchAnnotationData() {
        Map<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> data = Maps.newLinkedHashMap();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : annotationService.fetchTrackData( gene ).entrySet() ) {
            data.put( entry.getKey(), GeneOntologyTerm.propagateAnnotations( entry.getValue().entrySet().stream() ) );
        }
        return data;
    }

    private List<GeneViewRightPanelRow> fetchRightPanelRows( Edition edition ) {
        return annotationData.get( edition ).entrySet().stream().map( e -> new GeneViewRightPanelRow( e.getKey(),
                e.getValue().stream().anyMatch( FullAnnotation::isDirect ),
                e.getValue().stream().anyMatch( a -> a.getAnnotation().getEvidence().isCurated() ),
                 e.getValue() ) ).sorted().collect( Collectors.toList() );
    }

    private List<GeneViewRightPanelRow> fetchRightPanelRowsComparison( Edition editionA, Edition editionB ) {
        Map<GeneOntologyTerm, Set<FullAnnotation>> annotationsA = annotationData.get( editionA );
        Map<GeneOntologyTerm, Set<FullAnnotation>> annotationsB = annotationData.get( editionB );

        List<GeneViewRightPanelRow> results = annotationsA.entrySet().stream().map( e -> {
            BitSet inSet = new BitSet();
            inSet.set( 0 );
            inSet.set( 1, annotationsB.containsKey( e.getKey() ) );
            return new GeneViewRightPanelRow( e.getKey(),
                    e.getValue().stream().anyMatch( FullAnnotation::isDirect ),
                    e.getValue().stream().anyMatch( a -> a.getAnnotation().getEvidence().isCurated() ),
                    e.getValue(),
                    inSet );
        } ).collect( Collectors.toList() );

        // Considered using Maps.difference. However, this method is not as efficient
        // as the view returned by Sets.difference
        BitSet inSet = new BitSet();
        inSet.set( 0, 0 );
        inSet.set( 1 );
        for ( GeneOntologyTerm term : Sets.difference( annotationsB.keySet(), annotationsA.keySet() ) ) {
            results.add( new GeneViewRightPanelRow(
                    term,
                    null, // This term is not direct or indirect in the reference edition, it doesn't exist there
                    null, // This term doesn't exist in the reference edition
                    annotationsB.get( term ), inSet ) );

        }

        comparisons = Lists.newArrayList( editionB );

        Collections.sort( results );
        return results;
    }


    private List<GeneViewRightPanelRow> addRightPanelRowsComparison( Edition newComparison ) {

//        if (rightPanelTerms == null || rightPanelTerms.isEmpty()) {
//            return fetchRightPanelRowsComparison( rightPanelEdition, newComparison );
//        }

        int nextBitIndex = comparisons.size() + 1;

        Map<GeneOntologyTerm, Set<FullAnnotation>> annotationsNew = annotationData.get( newComparison );

        for ( GeneViewRightPanelRow rightPanelTerm : rightPanelTerms ) {
            BitSet inSet = rightPanelTerm.getInSet();
            inSet.set( nextBitIndex, annotationsNew.containsKey( rightPanelTerm.getTerm() ) );
        }

        // Not too fast but better than alternatives... probably
        Set<GeneOntologyTerm> termsInRightPanel = rightPanelTerms.stream().map( GeneViewRightPanelRow::getTerm )
                .collect( Collectors.toSet() );

        BitSet inSet = new BitSet( nextBitIndex );
        inSet.set( nextBitIndex );
        for ( GeneOntologyTerm term : Sets.difference( annotationsNew.keySet(), termsInRightPanel ) ) {
            rightPanelTerms.add( new GeneViewRightPanelRow(
                    term,
                    null, // This term is not direct or indirect in the reference edition, it doesn't exist there
                    null, // This term doesn't exist in the reference edition
                    annotationsNew.get( term ),
                    inSet ) );

        }

        comparisons.add( newComparison );

        Collections.sort( rightPanelTerms );
        return rightPanelTerms;
    }

    /**
     * Entry point for initiating the retrieval of necessary data.
     */
    public void fetchData() {
        log.debug( "fetchData" );

        // retrieve data
        annotationData = fetchAnnotationData();

        // Prune inferred root terms
        Set<GeneOntologyTerm> rootGOTerms = Sets.newHashSet( "GO:0003674", "GO:0008150", "GO:0005575" ).stream()
                .map( s -> cache.getCurrentTerm( s ) )
                .collect( Collectors.toSet() );

        annotationData.values().forEach( editionData -> {
            for ( GeneOntologyTerm term : rootGOTerms ) {
                Set<FullAnnotation> annotations = editionData.get( term );
                if ( annotations != null && annotations.stream().noneMatch( FullAnnotation::isDirect ) ) {
                    editionData.remove( term );
                }
            }
        } );

        // A map that will be needed in the front end for drilling down
        Map<Long, Integer> dateToEdition = new HashMap<>();

        for ( Edition ed : annotationData.keySet() ) {
            dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( dateToEdition ) );

        //allTerms = rawData.get( AnnotationType.DIRECT ).columnKeySet();
        rightPanelTerms = fetchRightPanelRows( rightPanelEdition );
//        Collections.sort( rightPanelTerms );

    }

    private Map<String, Object> createHCCallbackParamFail( String info ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", false );
        hcGsonMap.put( "info", info );
        return hcGsonMap;
    }

    private Map<String, Object> createHCCallbackParamMap( ChartValues chart ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", true );
        hcGsonMap.put( "chart", chart );
        return hcGsonMap;
    }

    /**
     * Create chart showing counts of unique terms annotated to this gene over time (both directly and through
     * propagation)
     *
     * @param rawData data
     */
    private void fetchAnnotationChart(
            Map<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> rawData ) {
        log.debug( "fetchAnnotationChart" );

        // Collect data from cache about species aggregates
        //        Map<Edition, Aggregate> aggregates = cache.getAggregates( species.getId() );

        ChartValues chart = new ChartValues( "Terms Annotated to " + gene.getSymbol(),
                "Annotations Count", "Date" );
        chart.setMin( 0 );

        //        // Create series for species aggregates
        //        Series aggregateSeries = new Series( "Species Direct Avg" );
        //        for ( Entry<Edition, Aggregate> entry : aggregates.entrySet() ) {
        //            aggregateSeries.addDataPoint( entry.getKey().getDate(), entry.getValue().getAvgDirectByGene() );
        //        }
        //        chart.addSeries( aggregateSeries );

        //Create series for direct annotations count
        Series directCountSeries = new Series( "Direct" );

        // Create series for inferred annotations count
        SeriesExtra inferredCountSeries = new SeriesExtra( "Inferred" );
        inferredCountSeries.putExtra( "visible", false );

        SeriesExtra aggregateSeries = new SeriesExtra( "Direct Species Mean" );
        aggregateSeries.putExtra( "dashStyle", "Dash" );
        aggregateSeries.putExtra( "color", "#24A93B" );
        aggregateSeries.putExtra( "marker", ImmutableMap.of("enabled", false) );

        SeriesExtra aggregateInferredSeries = new SeriesExtra( "Inferred Species Mean" );
        aggregateInferredSeries.putExtra( "visible", false );
        aggregateInferredSeries.putExtra( "dashStyle", "Dash" );
        aggregateInferredSeries.putExtra( "color", "#0060B5" );
        aggregateInferredSeries.putExtra( "marker", ImmutableMap.of("enabled", false) );

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> entry : rawData.entrySet() ) {
            Edition ed = entry.getKey();
            directCountSeries.addDataPoint( ed.getDate(),
                    entry.getValue().entrySet().stream().filter( e -> e.getValue().stream().anyMatch( FullAnnotation :: isDirect ) ).count() );
//            directCountSeries.addDataPoint( ed.getDate(),
//                    entry.getValue().entrySet().stream().flatMap( e -> e.getValue().stream() ).filter( FullAnnotation::isDirect )
//                            .map( FullAnnotation::getTerm ).distinct().count() );
            inferredCountSeries.addDataPoint( ed.getDate(),
                    entry.getValue().size());

            // Averages
            Aggregate agg = cache.getAggregate( ed );
            if ( agg != null ) {
                aggregateSeries.addDataPoint( ed.getDate(), agg.getAvgDirectByGene() );
                aggregateInferredSeries.addDataPoint( ed.getDate(), agg.getAvgInferredByGene() );
            }
        }

        chart.addSeries( aggregateSeries );
        chart.addSeries( aggregateInferredSeries );
        chart.addSeries( directCountSeries );
        chart.addSeries( inferredCountSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( chart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point for fetching the annotation chart
     */
    public void fetchAnnotationChart() {
        fetchAnnotationChart( annotationData );

    }

    /**
     * Create chart showing similarity of terms annotated to this gene in an edition compared to the most current
     * edition (both directly and through propagation)
     *
     * @param rawData data
     */
    private void fetchJaccardChart(
            Map<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> rawData ) {
        log.debug( "fetchJaccardChart" );

        ChartValues chart = new ChartValues( "Similarity of " + gene.getSymbol(),
                "Jaccard Index", "Date" );
        chart.setMin( 0 );
        chart.setMax( 1 );

        // Create jaccard between edition and current edition
        Edition currentEdition = Collections.max( rawData.keySet() );

        // For direct annotations
        Series directSeries = new Series( "Direct" );
        SeriesExtra averageDirectSeries = new SeriesExtra( "Direct Species Mean" );
        averageDirectSeries.putExtra( "color", "#24A93B" );
        averageDirectSeries.putExtra( "dashStyle", "Dash" );
        averageDirectSeries.putExtra( "marker", ImmutableMap.of("enabled", false) );

        // For Inferred annotations
        SeriesExtra inferredSeries = new SeriesExtra( "Inferred" );
        inferredSeries.putExtra( "visible", false );
        SeriesExtra averageInferredSeries = new SeriesExtra( "Inferred Species Mean" );
        averageInferredSeries.putExtra( "visible", false );
        averageInferredSeries.putExtra( "dashStyle", "Dash" );
        averageInferredSeries.putExtra( "color", "#0060B5" );
        averageInferredSeries.putExtra( "marker", ImmutableMap.of("enabled", false) );

        Set<GeneOntologyTerm> compareDirectGOSet = rawData.get( currentEdition ).entrySet().stream()
                .filter( e -> e.getValue().stream().anyMatch( FullAnnotation :: isDirect ) )
                .map( Entry::getKey )
                .collect( Collectors.toSet());
        Set<GeneOntologyTerm> compareGOSet =rawData.get( currentEdition ).keySet();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> entry : rawData.entrySet() ) {
            Edition ed = entry.getKey();
            inferredSeries.addDataPoint( ed.getDate(), Jaccard.similarity( entry.getValue().keySet(), compareGOSet ) );
            directSeries.addDataPoint( ed.getDate(), Jaccard.similarity( entry.getValue().entrySet().stream()
                            .filter( e -> e.getValue().stream().anyMatch( FullAnnotation :: isDirect ) )
                            .map( Entry::getKey )
                            .collect( Collectors.toSet()),
                    compareDirectGOSet ) );

            // Averages
            Aggregate agg = cache.getAggregate( ed );
            if ( agg != null ) {
                averageDirectSeries.addDataPoint( ed.getDate(), agg.getAvgDirectSimilarity() );
                averageInferredSeries.addDataPoint( ed.getDate(), agg.getAvgInferredSimilarity() );
            }
        }

        chart.addSeries( averageDirectSeries );
        chart.addSeries( averageInferredSeries );
        chart.addSeries( directSeries );
        chart.addSeries( inferredSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( chart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point for fetching the similarity chart
     */
    public void fetchJaccardChart() {
        fetchJaccardChart( annotationData );

    }

    /**
     * Create chart showing multifunctionality of this gene over time (Gillis J, Pavlidis P (2011) The Impact of
     * Multifunctional Genes on "Guilt by Association" Analysis. PLoS ONE 6(2): e17258. doi:
     * 10.1371/journal.pone.0017258)
     *
     * @param rawData data
     */
    private void fetchMultifunctionalityChart(
            Map<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> rawData ) {
        log.debug( "fetchMultifunctionalityChart" );

        ChartValues chart = new ChartValues( "Multifunctionality of " + gene.getSymbol(), "Multifunctionality [10^-5]",
                "Date" );
        chart.setMin( 0 );

        // Calculate multifunctionality of the gene in each edition
        Series multiSeries = new Series( "Multifunctionality" );
        SeriesExtra averageSeries = new SeriesExtra( "Species Mean" );
        averageSeries.putExtra( "color", "#24A93B" );
        averageSeries.putExtra( "dashStyle", "Dash" );
        averageSeries.putExtra( "marker", ImmutableMap.of("enabled", false) );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<FullAnnotation>>> entry : rawData.entrySet() ) {
            Edition ed = entry.getKey();

            Double multi = multifunctionalityService.multifunctionality( entry.getValue().keySet(), ed );
            if ( multi != null ) {
                // Scaled by 10^4
                multiSeries.addDataPoint( ed.getDate(), MULTIFUNCTIONALITY_SCALE * multi );
            }

            // Averages
            Aggregate agg = cache.getAggregate( ed );
            if ( agg != null ) {
                // Scaled by 10^4
                averageSeries.addDataPoint( ed.getDate(), MULTIFUNCTIONALITY_SCALE * agg.getAvgMultifunctionality() );
            }
        }
        chart.addSeries( averageSeries );
        chart.addSeries( multiSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( chart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point for fetching the multifunctionality chart
     */
    public void fetchMultifunctionalityChart() {
        fetchMultifunctionalityChart( annotationData );
    }

    /**
     * Entry point for fetching data to create a gantt chart of annotations categories over time
     */
    public void fetchTimeline() {
        log.debug( "fetchTimeline" );
        Set<GeneOntologyTerm> filterTerms;
        String subtitle;
        if ( rightPanelSelectedTerms == null || rightPanelSelectedTerms.size() == 0 ) {
            filterTerms = Sets.newHashSet();
            subtitle = "All Terms";
        } else {
            filterTerms = rightPanelSelectedTerms.stream().map( r -> r.getTerm() ).collect( Collectors.toSet() );
            if (rightPanelSelectedTerms.size() > 3) {
                subtitle = rightPanelSelectedTerms.size() + " Terms";
            } else if (rightPanelSelectedTerms.size() == 1) {
                GeneOntologyTerm t = rightPanelSelectedTerms.iterator().next().getTerm();
                subtitle = t.getGoId() + " - " + t.getName();
            } else {
                subtitle =  rightPanelSelectedTerms.stream().map( t -> t.getTerm().getGoId() ).collect( Collectors.joining( ", " ) );
            }
        }

        ChartValues chart = new ChartValues( "Annotation Categories of Terms in " + gene.getSymbol(),
                "Counts", "Date" );
        chart.setSubtitle( subtitle );
        Map<String, Series> seriesMap = Maps.newHashMap();

        List<Edition> allEditions = new ArrayList<>( cache.getAllEditions( this.gene.getSpecies() ) );
        Collections.sort( allEditions );

        for ( Edition ed : allEditions ) {

            Map<String, Long> categoryCounts = Maps.newHashMap();

            Map<GeneOntologyTerm, Set<FullAnnotation>> editionData = annotationData.get( ed );

            if (editionData != null) {
                Stream<Entry<GeneOntologyTerm, Set<FullAnnotation>>> dataStream = editionData.entrySet().stream();

                if ( !filterTerms.isEmpty() ) {
                    dataStream = dataStream.filter( e -> filterTerms.contains( e.getKey() ) );
                }

                // Group by annotation.evidence.category
                categoryCounts = dataStream.flatMap( e -> e.getValue().stream() )
                        .collect( Collectors.groupingBy( o -> o.getAnnotation().getEvidence().getCategory(), Collectors.counting() ) );

            }

            for (String category : cache.getEvidenceCategories().keySet() ) {
                seriesMap.computeIfAbsent( category, Series::new ).addDataPoint( ed.getDate(), categoryCounts.getOrDefault( category, 0L ) );
            }
        }

        for ( Series series : seriesMap.values().stream().sorted( Comparator.comparing( Series::getName ) ).collect( Collectors.toList() ) ) {
            chart.addSeries( series );
        }

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( chart );
        hcGsonMap.put( "categories", cache.getEvidenceCategories().keySet().stream().sorted().collect( Collectors.toList() ) );
        RequestContext.getCurrentInstance().addCallbackParam( "HC", new Gson().toJson( hcGsonMap ) );

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
        } catch (NumberFormatException e) {
            log.error( e );
            return;
        }

        clickEdition = cache.getEdition( this.gene.getSpecies(), editionId );

        if ( clickEdition == null ) {
            log.warn( "Selected edition id has no corresponding edition object" );
            return;
        }

        try {
            rightPanelTerms = fetchRightPanelRows( clickEdition );
            rightPanelFilteredTerms = null;
            rightPanelSelectedTerms = null;
            rightPanelEdition = clickEdition;

            // Reset comparison fields
            comparisons = Lists.newArrayList();
        } catch (NullPointerException e) {
            log.error( e );
            return;
        }

    }

    /**
     * Ctrl-Click event functionality for annotation chart
     */
    public void fetchAnnotationComparisonData() {
        log.debug( "fetchAnnotationComparisonData" );
        Integer compareEditionId;
        try {
            compareEditionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "compareEdition" ) );
        } catch (NumberFormatException e) {
            log.error( e );
            return;
        }

        Edition compareEdition = cache.getEdition( this.gene.getSpecies(), compareEditionId );

        if ( compareEdition == null ) {
            log.warn( "Selected compare edition id has no corresponding edition object" );
            return;
        }

        try {
            rightPanelTerms = fetchRightPanelRowsComparison( rightPanelEdition, compareEdition );
            rightPanelFilteredTerms = null;
            rightPanelSelectedTerms = null;
        } catch (NullPointerException e) {
            log.error( e, e );
            return;
        }

    }

    /**
     * Ctrl-Shift-Click event functionality for annotation chart
     */
    public void addAnnotationComparisonData() {
        log.debug( "addAnnotationComparisonData" );
        Integer compareEditionId;
        try {
            compareEditionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "compareEdition" ) );
        } catch (NumberFormatException e) {
            log.error( e );
            return;
        }

        Edition compareEdition = cache.getEdition( this.gene.getSpecies(), compareEditionId );

        if ( compareEdition == null ) {
            log.warn( "Selected compare edition id has no corresponding edition object" );
            return;
        }

        try {
            addRightPanelRowsComparison( compareEdition );
            rightPanelFilteredTerms = null;
            rightPanelSelectedTerms = null;
        } catch (NullPointerException e) {
            log.error( e, e );
            return;
        }

    }

    /**
     * Click event functionality for clicking timeline gantt chart
     */
    public void fetchTimelinePointData() {
        Integer editionId;
        try {

            editionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        } catch (NumberFormatException e) {
            log.error( e );
            return;
        }
        clickEdition = cache.getEdition( this.gene.getSpecies(), editionId );

        String goId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "termId" );

        viewTerm = cache.getTerm( clickEdition, goId );

        viewAnnotations = annotationData.get( clickEdition ).get( viewTerm ).stream().map( a -> a.getAnnotation() ).collect( Collectors.toSet());
        filteredViewAnnotations = null;
    }

    /**
     * custom filter function for primefaces data table column, filters by multiple booleans
     */
    public boolean filterByBitSet( Object value, Object filter, Locale locale ) {
        Set<String> filterIndices = (filter == null) ? null : Sets.newHashSet( (String[]) filter );
        if ( filterIndices == null || filterIndices.isEmpty() ) {
            return true;
        }

        if ( value == null ) {
            return false;
        }

        BitSet enabledBits = (BitSet) value;

        BitSet filterBits = new BitSet( enabledBits.length() );
        for ( String i : filterIndices ) {
            filterBits.set( Integer.valueOf( i ) );
        }

        return filterBits.equals( enabledBits );
    }

    /**
     * custom filter function for primefaces data table column, filters by single boolean with options:
     * true: true only
     * false: no filter
     */
    public boolean filterByTrueNull( Boolean value, Boolean filter, Locale locale ) {
        return !filter || value != null && value;
    }

    /**
     * custom filter function for primefaces data table column with tristatecheckbox:
     * check(1): true only
     * cross(2): false only
     * empty(0): no filter
     */
    public boolean filterTriStateCheckbox( Boolean value, Integer filter, Locale locale ) {
        return filter == 0 || value != null && ((filter == 1 && value) || (filter == 2 && !value));

    }

    // Getters & Setters

    public List<GeneOntologyTerm> getRightPanelSelectedGeneOntologyTerms() {
        return rightPanelSelectedTerms.stream().map( GeneViewRightPanelRow::getTerm ).collect( Collectors.toList() );
    }

}
