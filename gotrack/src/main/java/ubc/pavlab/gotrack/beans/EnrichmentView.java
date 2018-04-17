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
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.analysis.*;
import ubc.pavlab.gotrack.beans.service.EnrichmentService;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.StatusPoller;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.chart.SeriesExtra;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.search.GeneMatch;
import ubc.pavlab.gotrack.model.table.EnrichmentTableValues;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * View bean based around the enrichment functionality of GOTrack.
 *
 * @author mjacobson
 * @version $Id$
 */
@Named
@SessionScoped
@Log
public class EnrichmentView implements Serializable {

    /**
     * Model for a select drop down in front end
     */
    public enum EnrichmentChartType {
        SELECTED( "Selected" ), TOP( "Top" );

        private String label;

        private EnrichmentChartType( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Model for a select drop down in front end
     */
    public enum EnrichmentChartMeasure {
        RANK( "Rank" ), PVALUE( "P-Value" );

        private String label;

        private EnrichmentChartMeasure( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     *
     */
    private static final long serialVersionUID = 166880636358923147L;

    // Max results returned during autocompletion
    private static final Integer MAX_RESULTS = 15;

    // Similarity Settings
    private static final int TOP_N_JACCARD = 5; // Number of terms to use in the similarity top N
    private static final int TOP_N_ENRICHMENT_CHART = 10; // Number of terms to use in the top N enrichment charts

    @Inject
    private Cache cache;

    @Inject
    private EnrichmentService enrichmentService;

    @Inject
    private SessionManager session;

    // View parameters
    @Getter
    @Setter
    private Gene queryGene;

    @Getter
    @Setter
    private String bulkQuery = "";

    // Holds matches for bulk adding genes
    @Getter
    private List<GeneMatch> geneMatches;

    // Map of species id -> hit list
    private Map<Species, List<Gene>> speciesToSelectedGenes = Maps.newHashMap();

    // Holds gene to be viewed from hit list
    @Getter
    @Setter
    private Gene viewGene;

    // Enrichment Settings
    @Getter
    @Setter
    private Species selectedSpecies = null;

    @Getter
    EnrichmentAnalysisOptions enrichmentOptions = new EnrichmentAnalysisOptions();

    // Date selection properties ***************

    @Getter
    @Setter
    private Integer similarityReferenceYear = null;

    @Getter
    private List<Edition> filteredSimilarityReferenceEditions = Lists.newArrayList();

    @Getter
    @Setter
    private Edition similarityReferenceEdition = null;

    // *****************************************

    // Enrichment Feedback
    @Getter
    private StatusPoller statusPoller = new StatusPoller( " completed" ); // Used to display feedback in the overview tab

    // Analysis Data
    @Getter
    CombinedAnalysis combinedAnalysis;

    // Enrichment Data TODO: Could be cached in the EnrichmentAnalysis object instead of here
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults; // Results of the enrichment analysis


    // Enrichment Table Data ****************************
    @Getter
    private List<EnrichmentTableValues> enrichmentTableValues = new ArrayList<>(); // data backing the enrichment table

    @Getter
    @Setter
    private List<EnrichmentTableValues> filteredEnrichmentTableValues;

    @Getter
    @Setter
    private List<EnrichmentTableValues> selectedEnrichmentTableValues;

    @Getter
    @Setter
    private Edition enrichmentTableEdition; // currently displayed edition

    // ********************************************************

    // Enrichment Chart
    @Getter
    @Setter
    private EnrichmentChartType enrichmentChartType = EnrichmentChartType.TOP; //TODO

    @Getter
    @Setter
    private EnrichmentChartMeasure enrichmentChartMeasure = EnrichmentChartMeasure.RANK; //TODO

    // Select Data Point functionality
    @Getter
    private Edition selectedEdition;

    @Getter
    private GeneOntologyTerm selectedTerm;

    @Getter
    private Number selectedValue;

    @Getter
    private String selectedValueName;

    // Select Data Point functionality Enrichment Chart
    @Getter
    private EnrichmentResult selectedEnrichmentResult;

    @Getter
    private StabilityScore selectedStabilityScore;

    // Select Data Point functionality Similarity Chart
    @Getter
    private SimilarityScore selectedSimilarityScore;

    // Static final

    public static EnrichmentChartType[] getEnrichmentChartTypes() {
        return EnrichmentChartType.values();
    }

    public static EnrichmentChartMeasure[] getEnrichmentChartMeasures() {
        return EnrichmentChartMeasure.values();
    }

    public EnrichmentView() {
        log.info( "EnrichmentView created" );
        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
                + " MB" );

    }

    @PostConstruct
    public void postConstruct() {
        log.info( "postConstruct" );
        selectedSpecies = session.getSpecies();
        resetSimilarityDateSelector();
        for ( Species species : cache.getSpeciesList() ) {
            speciesToSelectedGenes.put( species, Lists.newArrayList() );
        }
    }

    /**
     * pre-render view
     * <p>
     * This is kept lightweight so that the page loads quickly and lazy loads the data using remote commands
     */
    public String init() {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "EnrichmentView init" );
        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            FacesContext.getCurrentInstance().addMessage( "betaMessage", new FacesMessage( FacesMessage.SEVERITY_WARN,
                    "This is the DEVELOPMENT version of GOTrack!", "" ) );
        }
        if ( combinedAnalysis != null && combinedAnalysis.isSuccess() ) {
            // When loading a previous analysis, we want to show the gene hit list that is associated with it, 
            // not the most recently viewed one.
            selectedSpecies = combinedAnalysis.getEnrichmentAnalysis().getCurrentSpecies();
            if ( !similarityReferenceEdition.getSpecies().equals( selectedSpecies ) ) {
                resetSimilarityDateSelector();
            }

        } else {
            enrichmentTableEdition = cache.getCurrentEditions( selectedSpecies );

        }

        return null;
    }

    /**
     * Run enrichment analysis given settings from front-end enrichment page followed by creation of tables and charts
     * for display.
     */
    public void enrich() {
        StopWatch timer = new StopWatch();
        timer.start();

        statusPoller = new StatusPoller( " completed" );

        combinedAnalysis = enrichmentService.combinedAnalysis(
                new HashSet<>( speciesToSelectedGenes.get( selectedSpecies ) ),
                selectedSpecies,
                enrichmentOptions,
                similarityReferenceEdition,
                TOP_N_JACCARD,
                statusPoller );

        enrichmentResults = combinedAnalysis.getEnrichmentAnalysis().getResults();

        statusPoller.newStatus( "Creating tables and charts...", 90 );

        createTables();
        createSimilarityChart();
        createTermCountChart();
        addDateToEditionCallbackParam();

        statusPoller.completeStatus();
        timer.stop();
        statusPoller.newStatus( "Finished in: " + timer.getTime() / 1000.0 + " seconds", 100 );

    }

    private void resetSimilarityDateSelector() {
        similarityReferenceEdition = cache.getCurrentEditions( selectedSpecies );
        Calendar cal = Calendar.getInstance();
        cal.setTime( similarityReferenceEdition.getDate() );
        similarityReferenceYear = cal.get( Calendar.YEAR );
        filterSimilarityReferenceEditions();
    }

    /**
     * Load the current enrichment analysis results stored in this session.
     */
    public void loadPreviousEnrichment() {
        if ( combinedAnalysis != null && combinedAnalysis.isSuccess() ) {

            createSimilarityChart();
            createTermCountChart();
            addDateToEditionCallbackParam();

            RequestContext.getCurrentInstance().addCallbackParam( "success", true );
        } else {
            RequestContext.getCurrentInstance().addCallbackParam( "success", false );
        }
    }

    private void addDateToEditionCallbackParam() {
        // Container for a mapping that will be used on the front-end for ajax queries
        Map<Long, Integer> currentDateToEdition = Maps.newHashMap();
        for ( Edition ed : cache.getAllEditions( selectedSpecies ) ) {
            currentDateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
        }
        RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( currentDateToEdition ) );
    }

    /**
     * Prepares table (enrichment, stability) data
     */
    private void createTables() {
        enrichmentTableEdition = Collections.max( enrichmentResults.keySet() );

        loadEnrichmentTableData( enrichmentTableEdition );

        selectedSimilarityScore = combinedAnalysis.getSimilarityAnalysis().getSimilarityScore( enrichmentTableEdition );

    }

    private Map<String, Object> createHCCallbackParamMap( ChartValues chart ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", true );
        hcGsonMap.put( "chart", chart );
        return hcGsonMap;
    }

    // GO Term Count Chart ---------------------------------------------------------------------------------------

    /**
     * Create Term Count chart
     */
    private void createTermCountChart() {
        ChartValues cv = new ChartValues( "GO Term Counts by Edition",
                "Count of Unique GO Terms", "Date" );
        Series significantTerms = new Series( "Significant Terms" );

        SeriesExtra allTerms = new SeriesExtra( "All Tested Terms" );
        allTerms.putExtra( "visible", false );
        SeriesExtra rejectedTerms = new SeriesExtra( "Rejected Terms" );
        rejectedTerms.putExtra( "visible", false );

        for ( Entry<Edition, Enrichment<GeneOntologyTerm, Gene>> rawResultsEntry : combinedAnalysis.getEnrichmentAnalysis().getRawResults()
                .entrySet() ) {
            Date date = rawResultsEntry.getKey().getDate();
            Enrichment<GeneOntologyTerm, Gene> enrichment = rawResultsEntry.getValue();
            significantTerms.addDataPoint( date, enrichment.getSignificantTerms().size() );
            allTerms.addDataPoint( date, enrichment.getResults().size() );
            rejectedTerms.addDataPoint( date, enrichment.getRejectedTerms().size() );
        }

        cv.addSeries( allTerms );
        cv.addSeries( significantTerms );
        cv.addSeries( rejectedTerms );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( cv );
        hcGsonMap.put( "selectedEdition", enrichmentTableEdition.getDate().getTime() );
        hcGsonMap.put( "referenceEdition", similarityReferenceEdition.getDate().getTime() );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_terms", new Gson().toJson( hcGsonMap ) );
    }

    // Similarity Charts ---------------------------------------------------------------------------------------

    /**
     * Create similarity chart
     */
    private void createSimilarityChart() {
        // Create Similarity Chart
        ChartValues cv = new ChartValues( "Enrichment Similarity to " + combinedAnalysis.getSimilarityAnalysis().getReferenceEdition().getDate(),
                "Jaccard Similarity Index", "Date" );
        cv.setMin( 0 );
        cv.setMax( 1 );

        // Prepare series
        Series completeTermJaccard = new Series( "All Terms" );
        Series topTermJaccard = new Series( "Top " + TOP_N_JACCARD + " Terms" );
        Series topGeneJaccard = new Series( "Genes Backing Top Terms" );
        Series topParentsJaccard = new Series( "Parents of Top Terms" );

        // Fill in series
        for ( Entry<Edition, SimilarityScore> editionEntry : combinedAnalysis.getSimilarityAnalysis().getSimilarityScores().entrySet() ) {
            SimilarityScore score = editionEntry.getValue();
            Date date = editionEntry.getKey().getDate();
            completeTermJaccard.addDataPoint( date, score.getCompleteTermSim() );
            topTermJaccard.addDataPoint( date, score.getTopTermSim() );
            topGeneJaccard.addDataPoint( date, score.getTopGeneSim() );
            if ( score.getTopParentsSim() != null ) {
                topParentsJaccard.addDataPoint( date, score.getTopParentsSim() );
            }

        }

        cv.addSeries( completeTermJaccard );
        cv.addSeries( topTermJaccard );
        cv.addSeries( topGeneJaccard );

        if ( topParentsJaccard.getData().size() > 0 ) {
            cv.addSeries( topParentsJaccard );
        }

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( cv );
        hcGsonMap.put( "selectedEdition", enrichmentTableEdition.getDate().getTime() );
        hcGsonMap.put( "referenceEdition", similarityReferenceEdition.getDate().getTime() );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_similarity", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Create P-value Histogram
     */
    public void createPValueHistogram() {
        // Create P-value Histogram
        List<Double> pvalues = combinedAnalysis.getEnrichmentAnalysis().getRawResults().get( enrichmentTableEdition ).getResults().values()
                .stream()
                .map( EnrichmentResult::getPvalue )
                .collect( Collectors.toList() );
        RequestContext.getCurrentInstance().addCallbackParam( "HC_histo", new Gson().toJson( pvalues ) );
        RequestContext.getCurrentInstance().addCallbackParam( "edition", new Gson().toJson( enrichmentTableEdition ) );
    }



    // Enrichment Charts ---------------------------------------------------------------------------------------

    /**
     * Entry point to ajax request for a graph of a single term with p-value error bands based on stability confidence
     */
    public void createStabilityChart( GeneOntologyTerm term ) {

        if ( term == null ) {
            return;
            // return failed
        }

        ChartValues chart = createPValueChart( Sets.newHashSet( term ) );
        chart.setTitle( "Enrichment Stability" );
        chart.setSubtitle( term.getGoId() + " - " + term.getName() );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( chart );

        // Create error bands to display the 95% confidence around each point
        Map<Edition, StabilityScore> scores = combinedAnalysis.getStabilityAnalysis().getStabilityScores( term );
        Map<Long, Object> dateToStabilityScore = new HashMap<>();

        Series rangeSeries = new Series( "95% Confidence" );

        enrichmentResults.entrySet().stream().sorted( Map.Entry.comparingByKey() ).forEach( entry -> {
            Edition edition = entry.getKey();
            EnrichmentResult er = entry.getValue().get( term );

            if ( er != null ) {
                StabilityScore sc = scores.get( edition );
                rangeSeries.addDataPoint( edition.getDate(), sc.getMinPvalue(), sc.getMaxPvalue() );
                Double s = sc.getScore();
                // JSON does not support special Double types so we convert them to strings and deal with them in the front-end
                dateToStabilityScore.put( edition.getDate().getTime(), s.isInfinite() || s.isNaN() ? s.toString() : s );
            }
        } );

        ChartValues cv = new ChartValues();
        cv.addSeries( rangeSeries );
        hcGsonMap.put( "errors", cv );
        hcGsonMap.put( "dateToStabilityScore", dateToStabilityScore );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_stability",
                new GsonBuilder().serializeNulls().create().toJson( hcGsonMap ) );
    }

    /**
     * Entry point to ajax request for a graph of top N terms by p-value
     */
    public void createPValueChartByTopN() {
        ChartValues chart = createPValueChart( combinedAnalysis.getEnrichmentAnalysis().getTopNTerms( TOP_N_ENRICHMENT_CHART ) );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_pvalue",
                new GsonBuilder().serializeNulls().create().toJson( createHCCallbackParamMap( chart ) ) );
    }

    /**
     * Entry point to ajax request for a graph of selected terms by p-value
     */
    public void createPValueChartBySelected() {
        ChartValues chart;
        if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
            // If nothing selected graph everything
            chart = createPValueChart( combinedAnalysis.getEnrichmentAnalysis().getTermsSignificantInAnyEdition() );
        } else {
            chart = createPValueChart( selectedEnrichmentTableValues.stream().map( EnrichmentTableValues::getTerm )
                    .collect( Collectors.toList() ) );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "HC_pvalue",
                new GsonBuilder().serializeNulls().create().toJson( createHCCallbackParamMap( chart ) ) );
    }


    private ChartValues createPValueChart( Collection<GeneOntologyTerm> termsToChart ) {
        if (termsToChart == null || termsToChart.isEmpty() ) {
            return null; //failed
        }

        Map<GeneOntologyTerm, SeriesExtra> seriesMap = new HashMap<>();
        SeriesExtra cutoffSeries = new SeriesExtra( "Threshold" );
        cutoffSeries.putExtra( "lineWidth", 2 );
        cutoffSeries.putExtra( "zIndex", 5 );
        cutoffSeries.putExtra( "enableMouseTracking", false );
        cutoffSeries.putExtra( "dashStyle", "shortdash" );
        cutoffSeries.putExtra( "color", "black" );

        enrichmentResults.entrySet().stream().sorted( Map.Entry.comparingByKey() ).forEach( entry -> {
                Edition edition = entry.getKey();
                for ( GeneOntologyTerm term : termsToChart ) {
                    EnrichmentResult er = entry.getValue().get( term );
                    if ( er != null ) {
                        SeriesExtra s = seriesMap.get( term );
                        if ( s == null ) {
                            s = new SeriesExtra( term.getGoId() );
                            s.putExtra( "title", term.getName() );
                            seriesMap.put( term, s );
                        }
                        s.addDataPoint( edition.getDate(), er.getPvalue() );
                    }
                }

            cutoffSeries.addDataPoint( edition.getDate(), combinedAnalysis.getEnrichmentAnalysis().getCutoff( edition ) );
        } );

        ChartValues cv = new ChartValues("Enrichment Results" ,"P-Value", "Date" );

        cv.addSeries( cutoffSeries );

        for ( Series s : seriesMap.values() ) {
            cv.addSeries( s );
        }

        return cv;

    }

    /**
     * Entry point to ajax call for creating rank charts of selected terms
     */
    public void createRankChartBySelected() {

        ChartValues chart;
        if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
            // If nothing selected graph everything
            chart = createRankChart( combinedAnalysis.getEnrichmentAnalysis().getTermsSignificantInAnyEdition(), null );
        } else {
            chart = createRankChart( selectedEnrichmentTableValues.stream().map( EnrichmentTableValues::getTerm )
                    .collect( Collectors.toSet() ), null );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "HC_enrichment",
                new GsonBuilder().serializeNulls().create().toJson( createHCCallbackParamMap( chart ) ) );
    }

    /**
     * Entry point to ajax call for creating rank charts of by Top N
     */
    public void createRankChartByTopN() {

        Set<GeneOntologyTerm> selectedTerms = combinedAnalysis.getEnrichmentAnalysis().getTopNTerms( TOP_N_ENRICHMENT_CHART );

        ChartValues chart = createRankChart( selectedTerms, TOP_N_ENRICHMENT_CHART );
        RequestContext.getCurrentInstance().addCallbackParam( "HC_enrichment",
                new GsonBuilder().serializeNulls().create().toJson( createHCCallbackParamMap( chart ) ) );
    }

    private <E, T extends Comparable, R> Map<T,Double> fractionalRank( Collection<E> sortedData, Function<E,T> getKey, Function<E,R> getValue, boolean jitterRanks) {
        Map<T, Double> relativeRanks = new LinkedHashMap<>();
        int currentRank = 0;
        int idx = 0;
        List<E> previousResultGroup = Lists.newArrayList();
        for ( E termEntry : sortedData ) {
            R er = getValue.apply(termEntry);

            E previousEntry = previousResultGroup.isEmpty() ? null : previousResultGroup.iterator().next();

            if ( previousEntry == null || er.equals( getValue.apply( previousEntry ) ) ) {
                // same as previous group
                previousResultGroup.add( termEntry );
            } else {
                // new group; assign ranks to previous group
                previousResultGroup.sort( Comparator.comparing( getKey::apply ) );
                double fractionalRank = currentRank + (previousResultGroup.size() - 1) / 2.0;
                double jitter = 0;
                for ( E previousResultGroupEntry : previousResultGroup ) {
                    relativeRanks.put( getKey.apply(previousResultGroupEntry), fractionalRank + jitter );
                    if (jitterRanks) jitter += 0.01;
                }

                previousResultGroup = Lists.newArrayList(termEntry);
                currentRank = idx;
            }

            idx++;
        }

        // Assign final group
        previousResultGroup.sort( Comparator.comparing( getKey::apply ) );
        double fractionalRank = currentRank + (previousResultGroup.size() - 1) / 2.0;
        double jitter = 0;
        for ( E previousResultGroupEntry : previousResultGroup ) {
            relativeRanks.put( getKey.apply(previousResultGroupEntry), fractionalRank + jitter );
            if (jitterRanks) jitter += 0.01;
        }

        return relativeRanks;
    }

    private ChartValues createRankChart( Set<GeneOntologyTerm> termsToChart, Integer topN ) {
        if (termsToChart == null || termsToChart.isEmpty() ) {
            return null; //failed
        }

        ChartValues  cv = new ChartValues( "Enrichment Results by Relative Rank", "Relative Rank", "Date" );
        cv.getExtra().put( "maxRank", -1.0 ); // largest rank over all editions and terms to help create polygon regions
        cv.getExtra().put( "outsideTopNCheck", false ); // Is any term outside of the topN in any edition
        cv.getExtra().put( "insignificantCheck", false ); // Are there any terms which are insignificant in an edition

        Map<GeneOntologyTerm, SeriesExtra> seriesMap = new HashMap<>();

        // Holds map of date -> max rank that is still significant, this is used in the font-end
        // to determine the regions of the graph
        Map<Long, Double> dateToMaxSigRank = new HashMap<>();


        enrichmentResults.entrySet().stream().sorted( Map.Entry.comparingByKey() ).forEach( ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> entry ) -> {
            Edition edition = entry.getKey();

            double maxRank = -1; // for use in rank metric
            boolean outsideTopNCheck = false; // Is any term outside of the topN in any edition
            boolean insignificantCheck = false; // Are there any terms which are insignificant in an edition

            LinkedHashMap<GeneOntologyTerm, EnrichmentResult> sortedData = entry.getValue().entrySet().stream()
                    .filter( termEntry -> termsToChart.contains( termEntry.getKey() ) )
                    .sorted( Comparator.comparing( termEntry -> termEntry.getValue().getRank() ) )
                    .collect( Collectors.toMap( Entry::getKey, Entry::getValue,
                            ( e1, e2 ) -> e1, LinkedHashMap::new
                    ) );

            // Compute fractional relative ranks with jitter
            Map<GeneOntologyTerm, Double> relativeRanks = fractionalRank( sortedData.entrySet(), Entry::getKey, Entry::getValue, true );


            double breakPoint = relativeRanks.size(); // Where the cutoff is for significance among the terms in this edition
            for ( GeneOntologyTerm term : termsToChart ) {



                // for ( Entry<GeneOntologyTerm, EnrichmentResult> entry : editionData ) {
                // GeneOntologyTerm term = entry.getKey();
                SeriesExtra s = seriesMap.get( term );
                if ( s == null ) {
                    s = new SeriesExtra( term.getGoId() );
                    s.putExtra( "title", term.getName() );
                    seriesMap.put( term, s );
                }

                Double relativeRank = relativeRanks.get( term );
                EnrichmentResult er = sortedData.get( term );
                if ( er != null ) {

                    // find smallest relative rank that is insignificant
                    if ( !er.isSignificant() && relativeRank < breakPoint ) {
                        insignificantCheck = true; // There are some insignificant points
                        breakPoint = relativeRank;
                    }

                    if ( er.isSignificant() && topN != null && er.getRank() > topN ) {
                        outsideTopNCheck = true;
                    }

                    if ( relativeRank > maxRank ) {
                        // Get the largest rank over all editions and terms to help create polygon regions
                        maxRank = relativeRank;
                    }

                    s.addDataPoint( edition.getDate(), relativeRank );
                } else {
                    s.addDataPoint( edition.getDate(), null );
                }
            }

            if (insignificantCheck) {
                cv.getExtra().put( "insignificantCheck", true );
            }

            if (outsideTopNCheck) {
                cv.getExtra().put( "outsideTopNCheck", true );
            }

            if (maxRank > (double)cv.getExtra().get( "maxRank" ) ) {
                cv.getExtra().put( "maxRank", maxRank );
            }

            dateToMaxSigRank.put( edition.getDate().getTime(), breakPoint );

        } );

        for ( Series s : seriesMap.values() ) {
            cv.addSeries( s );
        }

        cv.getExtra().put( "dateToMaxSigRank", dateToMaxSigRank ); // largest rank over all editions and terms to help create polygon regions
        cv.getExtra().put( "topN", topN ); // largest rank over all editions and terms to help create polygon regions

        return cv;

    }

    /**
     * Used for clicks on data points in enrichment/stability chart
     */
    public void fetchTermInformation() {
        String termId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "termId" );
        Integer edition = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );

        Edition ed = cache.getEdition( selectedSpecies, edition );
        selectedEdition = ed;

        Map<GeneOntologyTerm, EnrichmentResult> a = enrichmentResults.get( ed );
        if ( a != null ) {
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : a.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();

                if ( term.getGoId().equals( termId ) ) {
                    EnrichmentResult er = termEntry.getValue();
                    selectedTerm = term;
                    selectedEnrichmentResult = er;
                    selectedStabilityScore = combinedAnalysis.getStabilityAnalysis().getStabilityScores( selectedTerm, selectedEdition );
                    selectedValueName = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
                            .get( "valueLabel" );
                    if ( selectedValueName != null ) {
                        double value = Double.valueOf( FacesContext.getCurrentInstance().getExternalContext()
                                .getRequestParameterMap().get( "value" ) );
                        selectedValue = value;
                    }

                    return;
                }
            }

        }
        // If ti makes it here, we didn't find any matches.
        resetFetchTermInformation();

    }

    private void resetFetchTermInformation() {
        selectedTerm = null;
        selectedEnrichmentResult = null;
        selectedStabilityScore = null;
        selectedValueName = null;
    }


    /**
     * Used for clicks on data points in term/similarity charts
     */
    public void loadTableData() {
        Integer edition = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        Edition ed = cache.getEdition( selectedSpecies, edition );
        loadEnrichmentTableData( ed );
        selectedSimilarityScore = combinedAnalysis.getSimilarityAnalysis().getSimilarityScore( ed );
    }

    // Enrichment Table ---------------------------------------------------------------------------------------

    /**
     * Prepares data for enrichment table
     */
    private void loadEnrichmentTableData( Edition ed ) {

        filteredEnrichmentTableValues = null;
        enrichmentTableValues = new ArrayList<>();
        enrichmentTableEdition = ed;

        if ( ed != null ) {
            Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : editionData.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                EnrichmentResult er = termEntry.getValue();
                StabilityScore sc = combinedAnalysis.getStabilityAnalysis().getStabilityScores( term, ed );
                Double val = sc.getScore();
                int quantile = 0;
                if ( !val.isNaN() ) {
                    // Scale from -8 -> 10
                    quantile = (int) Math.max( 1, Math.min( 20, 20 - (Math.ceil( val ) + 8) ) );
                }

                enrichmentTableValues
                        .add( new EnrichmentTableValues( ed, term, er, sc, quantile ) );
            }
            Collections.sort( enrichmentTableValues );

        }
    }

    /**
     * custom filter function for primefaces data table column, filters by arbitrary number in a 'less than' manner
     */
    public boolean filterByNumberLT( Object value, Object filter, Locale locale ) {
        String filterText = (filter == null) ? null : filter.toString().trim();
        if ( filterText == null || filterText.equals( "" ) ) {
            return true;
        }

        if ( value == null ) {
            return false;
        }

        return Double.compare( ((Number) value).doubleValue(), Double.valueOf( filterText ) ) < 0;
    }

    // Gene List ---------------------------------------------------------------------------------------

    /**
     * Remove all genes from selected species' hit list
     */
    public void removeAllGenes() {
        List<Gene> selectGenes = speciesToSelectedGenes.get( selectedSpecies );
        if ( selectGenes != null ) {
            selectGenes.clear();
        }
    }

    /**
     * Add gene to to hit list
     */
    public void addGene() {
        List<Gene> selectGenes = speciesToSelectedGenes.get( selectedSpecies );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( selectedSpecies, selectGenes );
        }
        if ( this.queryGene != null ) {
            if ( !selectGenes.contains( this.queryGene ) ) {
                selectGenes.add( this.queryGene );
                addMessage( "Gene (" + this.queryGene.getSymbol() + ") successfully added.",
                        FacesMessage.SEVERITY_INFO );
            } else {
                addMessage( "Gene (" + this.queryGene.getSymbol() + ") already added.", FacesMessage.SEVERITY_WARN );
            }

        } else {
            addMessage( "Gene (" + this.queryGene.getSymbol() + ") could not be found.", FacesMessage.SEVERITY_WARN );
        }

    }

    /**
     * Add multiple genes based on input to the 'Add Multiple' modal field, this string is then split
     * and turned into GeneMatch to then be confirmed or altered by the user
     */
    public void searchMultipleGenes() {
        Set<String> inputSet = new HashSet<>( Arrays.asList( bulkQuery.split( "\\s*(,|\\s)\\s*" ) ) );
        geneMatches = new ArrayList<>();
        Map<GeneMatch.Level, Integer> cntMap = new HashMap<>();

        for ( String geneInput : inputSet ) {
            GeneMatch geneMatch = cache.guessGeneBySymbol( geneInput, selectedSpecies );
            geneMatches.add( geneMatch );
            Integer cnt = cntMap.get( geneMatch.getLevel() );
            cntMap.put( geneMatch.getLevel(), (cnt == null ? 0 : cnt) + 1 );
        }

        if ( !geneMatches.isEmpty() ) {

            Collections.sort( geneMatches, Collections.reverseOrder() );

            StringBuilder message = new StringBuilder();

            for ( Entry<GeneMatch.Level, Integer> typeEntry : cntMap.entrySet() ) {
                message.append( typeEntry.getKey().toString() ).append( " : " ).append( typeEntry.getValue().toString() ).append( "<br/>" );
            }
            addMessage( message.toString(), FacesMessage.SEVERITY_INFO );
        }

    }

    /**
     * Confirm GeneMatch after alterations (or not) and add these genes to hit list
     */
    public void confirmMatches() {
        int genesAdded = 0;
        List<Gene> selectGenes = speciesToSelectedGenes.get( selectedSpecies );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( selectedSpecies, selectGenes );
        }

        for ( GeneMatch gm : geneMatches ) {
            Gene g = gm.getSelectedGene();
            if ( g != null && !selectGenes.contains( g ) ) {
                genesAdded++;
                selectGenes.add( g );
            }
        }

        if ( genesAdded > 0 ) {
            addMessage( "Successfully added " + genesAdded + " gene(s)", FacesMessage.SEVERITY_INFO );
        }

    }

    /**
     * Remove single gene from hit list
     */
    public void removeGene( Gene gene ) {
        List<Gene> selectGenes = speciesToSelectedGenes.get( selectedSpecies );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( selectedSpecies, selectGenes );
        }
        selectGenes.remove( gene );
        addMessage( "Gene (" + gene.getSymbol() + ") successfully removed.", FacesMessage.SEVERITY_INFO );
    }

    // General ---------------------------------------------------------------------------------------

    /**
     * add message to be displayed in a growl in the screen corner
     */
    private void addMessage( String summary, FacesMessage.Severity severity ) {
        FacesMessage message = new FacesMessage( severity, summary, null );
        FacesContext.getCurrentInstance().addMessage( null, message );
    }

    /**
     * autocomplete gene by symbol
     */
    public List<GeneMatch> complete( String query ) {
        if ( StringUtils.isEmpty( query.trim() ) || selectedSpecies == null ) return Lists.newArrayList();
        return Lists.newArrayList( this.cache.searchGeneBySymbol( query.trim(), selectedSpecies, MAX_RESULTS ) );
    }

    public void filterSimilarityReferenceEditions() {
        Calendar calendar = Calendar.getInstance();
        filteredSimilarityReferenceEditions = cache.getAllEditions( selectedSpecies ).stream().filter( ed -> {
            calendar.setTime( ed.getDate() );
            return calendar.get( Calendar.YEAR ) == similarityReferenceYear;
        } ).sorted().collect( Collectors.toList() );
        if ( !filteredSimilarityReferenceEditions.isEmpty() ) {
            similarityReferenceEdition = filteredSimilarityReferenceEditions.get( 0 );
        }
    }

    public List<Gene> getGenesBackingTopTermsForEdition( Edition edition ) {
        if ( combinedAnalysis == null || edition == null ) return Lists.newArrayList();
        return combinedAnalysis.getSimilarityAnalysis().getSimilarityScore( edition ).getTopGenes().stream()
                .sorted().collect( Collectors.toList() );
    }

    // Getters / Setters ---------------------------------------------------------------------------------------

    public List<Gene> getSelectedGenes() {
        return speciesToSelectedGenes.get( selectedSpecies );
    }

    public Edition getCurrentEdition() {
        return cache.getCurrentEditions( selectedSpecies );
    }

}
