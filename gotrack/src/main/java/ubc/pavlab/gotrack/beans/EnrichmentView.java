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
import ubc.pavlab.gotrack.model.visualization.Graph;

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

    @Getter
    @Setter
    private SimilarityCompareMethod similarityCompareMethod = SimilarityCompareMethod.CURRENT; // Method to use in the similarity analysis

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

    @Getter
    @Setter
    private int enrichmentChartTopN = 10; //TODO

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

        for ( Species species : cache.getSpeciesList() ) {
            speciesToSelectedGenes.put( species, Lists.<Gene>newArrayList() );
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
                    "This is the DEVELOPMENT version of GOTrack!", null ) );
        }
        if ( combinedAnalysis != null && combinedAnalysis.isSuccess() ) {
            // When loading a previous analysis, we want to show the gene hit list that is associated with it, 
            // not the most recently viewed one.
            selectedSpecies = combinedAnalysis.getEnrichmentAnalysis().getCurrentSpecies();
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
                similarityCompareMethod,
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

        RequestContext.getCurrentInstance().addCallbackParam( "HC_terms", new Gson().toJson( hcGsonMap ) );
    }

    // Similarity Charts ---------------------------------------------------------------------------------------

    /**
     * Create similarity chart
     */
    private void createSimilarityChart() {
        // Create Similarity Chart
        ChartValues cv = new ChartValues( "Enrichment Similarity to "
                + (similarityCompareMethod.equals( SimilarityCompareMethod.PROXIMAL ) ? "Previous" : "Current")
                + " Edition",
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

        RequestContext.getCurrentInstance().addCallbackParam( "HC_similarity", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Entry point to ajax request for a graph of a single term with p-value error bands based on stability confidence
     */
    public void createStabilityChart( GeneOntologyTerm term ) {

        if ( term == null ) {
            return;
            // return failed
        }

        createChart( Sets.newHashSet( term ), null, true );
    }

    // Enrichment Charts ---------------------------------------------------------------------------------------

    public void fetchTermGraph( GeneOntologyTerm term ) {
        Graph graph = Graph.fromGO( term );
        RequestContext.getCurrentInstance().addCallbackParam( "graph_data", graph.getJsonString() );
    }

    /**
     * Create chart for comparing terms and seeing how their p-values or ranks change over time. If a single term is
     * selected and the graph is to be based on p-value (not rank) then error bands are also sent.
     *
     * @param selectedTerms Terms to be graphed, if null grap everything
     * @param topN          Cutoff for the 'desired' region in the graph, selectedTerms should already be trimmed down to topN
     * @param pvalue        true if p-value is to be graphed, false if rank
     */
    private void createChart( Set<GeneOntologyTerm> selectedTerms, Integer topN, boolean pvalue ) {
        // Get data
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data = null;
        if ( selectedTerms == null ) {
            selectedTerms = combinedAnalysis.getEnrichmentAnalysis().getTermsSignificantInAnyEdition();
            data = enrichmentResults;
        } else {
            data = filter( selectedTerms );
        }

        // Get sorted editions
        List<Edition> eds = new ArrayList<>( data.keySet() );
        Collections.sort( eds );

        // Chart
        ChartValues cv = null;

        double maxRank = -1; // for use in rank metric

        Map<String, Object> hcGsonMap = null;

        if ( pvalue ) {
            // Graph p-values
            Map<GeneOntologyTerm, SeriesExtra> series = new HashMap<>();
            for ( Edition ed : eds ) {
                for ( GeneOntologyTerm term : selectedTerms ) {
                    EnrichmentResult er = data.get( ed ).get( term );
                    if ( er != null ) {
                        SeriesExtra s = series.get( term );
                        if ( s == null ) {
                            s = new SeriesExtra( term.getGoId() );
                            s.putExtra( "title", term.getName() );
                            series.put( term, s );
                        }
                        s.addDataPoint( ed.getDate(), er.getPvalue() );
                    }
                }

            }
            cv = new ChartValues(
                    selectedTerms.size() == 1 ? "Enrichment Stability Results" : "Enrichment Results",
                    "P-Value", "Date" );

            for ( Series s : series.values() ) {
                cv.addSeries( s );
            }

            Map<Long, Double> cutoffs = new TreeMap<>();
            for ( Edition ed : eds ) {
                cutoffs.put( ed.getDate().getTime(), combinedAnalysis.getEnrichmentAnalysis().getCutoff( ed ) );
            }

            hcGsonMap = createHCCallbackParamMap( cv );
            hcGsonMap.put( "type", "pvalue" );
            hcGsonMap.put( "cutoffs", cutoffs );

        } else {
            // rank

            // Holds map of date -> max rank that is still significant, this is used in the font-end
            // to determine the regions of the graph
            Map<Long, Double> dateToMaxSigRank = new HashMap<>();

            Map<GeneOntologyTerm, SeriesExtra> series = new HashMap<>();

            boolean outsideTopNCheck = false; // Is any term outside of the topN in any edition
            boolean insignificantCheck = false; // Are there any terms which are insignificant in an edition
            for ( Edition ed : eds ) {
                int maxSignificantRank = combinedAnalysis.getEnrichmentAnalysis().getTermsSignificant( ed ).size() - 1;

                // First we compute the relative ranks among the terms selected
                Map<GeneOntologyTerm, EnrichmentResult> editionData = data.get( ed );
                ArrayList<Entry<GeneOntologyTerm, EnrichmentResult>> sortedData = new ArrayList<>(
                        editionData.entrySet() );

                // Sort by ranks
                Collections.sort( sortedData, new Comparator<Entry<GeneOntologyTerm, EnrichmentResult>>() {
                    @Override
                    public int compare( Entry<GeneOntologyTerm, EnrichmentResult> e1,
                                        Entry<GeneOntologyTerm, EnrichmentResult> e2 ) {
                        return Integer.compare( e1.getValue().getRank(), e2.getValue().getRank() );
                    }
                } );

                // Compute standard relative ranks

                Map<GeneOntologyTerm, Double> relativeRanks = new HashMap<>();
                Map<Integer, List<GeneOntologyTerm>> standardRanks = new HashMap<>();
                int r = -1;
                EnrichmentResult previousResult = null;
                int cnt = -1;
                for ( Entry<GeneOntologyTerm, EnrichmentResult> entry : sortedData ) {
                    cnt++;
                    EnrichmentResult er = entry.getValue();
                    if ( er.equals( previousResult ) ) {
                        // pass
                    } else {
                        r = cnt;
                    }
                    List<GeneOntologyTerm> termSet = standardRanks.get( r );
                    if ( termSet == null ) {
                        termSet = new ArrayList<>();
                        standardRanks.put( r, termSet );
                    }
                    termSet.add( entry.getKey() );
                    previousResult = er;
                }

                // Compute fractional relative ranks with jitter

                // We use jitter because if two points have the exact same y-value in the front-end in makes mousing over
                // the data a pain
                for ( Entry<Integer, List<GeneOntologyTerm>> rankEntry : standardRanks.entrySet() ) {
                    int standardRank = rankEntry.getKey();
                    List<GeneOntologyTerm> termSet = rankEntry.getValue();
                    Collections.sort( termSet );
                    double newRank = standardRank + (termSet.size() - 1) / 2.0;
                    double jitter = 0;
                    for ( GeneOntologyTerm term : termSet ) {
                        relativeRanks.put( term, newRank + jitter );
                        jitter += 0.01;

                    }
                }

                double breakPoint = relativeRanks.size(); // Where the cutoff is for significance among the terms in this edition
                for ( GeneOntologyTerm term : selectedTerms ) {
                    // for ( Entry<GeneOntologyTerm, EnrichmentResult> entry : editionData ) {
                    // GeneOntologyTerm term = entry.getKey();
                    SeriesExtra s = series.get( term );
                    if ( s == null ) {
                        s = new SeriesExtra( term.getGoId() );
                        s.putExtra( "title", term.getName() );
                        series.put( term, s );
                    }
                    EnrichmentResult er = editionData.get( term );
                    Double relativeRank = relativeRanks.get( term );
                    if ( er != null ) {
                        int rank = er.getRank();
                        // find smallest relative rank that is insignificant
                        if ( rank > maxSignificantRank && relativeRank < breakPoint ) {
                            insignificantCheck = true; // There are some insignificant points
                            breakPoint = relativeRank;
                        }

                        if ( topN != null && rank > topN && rank <= maxSignificantRank ) {
                            outsideTopNCheck = true;
                        }

                        if ( relativeRank > maxRank ) {
                            maxRank = relativeRank;
                        }

                        s.addDataPoint( ed.getDate(), relativeRank );
                    } else {
                        s.addDataPoint( ed.getDate(), null );
                    }
                }

                dateToMaxSigRank.put( ed.getDate().getTime(), breakPoint );

            }

            cv = new ChartValues( "Enrichment Results by Relative Rank",
                    "Relative Rank", "Date" );

            for ( Series s : series.values() ) {
                cv.addSeries( s );
            }

            hcGsonMap = createHCCallbackParamMap( cv );
            hcGsonMap.put( "maxRank", maxRank );
            hcGsonMap.put( "dateToMaxSigRank", dateToMaxSigRank );
            hcGsonMap.put( "insignificantCheck", insignificantCheck );
            hcGsonMap.put( "outsideTopNCheck", outsideTopNCheck );
            hcGsonMap.put( "type", "rank" );
            hcGsonMap.put( "topN", topN );
            // if ( topN != null ) {
            // if ( insigCheck ) {
            // RequestContext.getCurrentInstance().addCallbackParam(
            // "hc_breakAt",
            // new Gson().toJson( Arrays.asList( Arrays.asList( topN + 1, maxRank + 1, 3 ),
            // Arrays.asList( maxRank + 2, m, 0 ) ) ) );
            // } else {
            // RequestContext.getCurrentInstance().addCallbackParam( "hc_breakAt",
            // new Gson().toJson( Arrays.asList( Arrays.asList( topN + 1, maxRank + 1, 3 ) ) ) );
            // }
            // }

        }

        if ( selectedTerms.size() == 1 && pvalue ) {
            // if there is a single graphed term and pvalue is the chosen metric we also create error bands to display
            // the 95% confidence around each point
            GeneOntologyTerm term = selectedTerms.iterator().next();
            Map<Edition, StabilityScore> scores = combinedAnalysis.getStabilityAnalysis().getStabilityScores( term );
            Map<Long, Object> dateToStabilityScore = new HashMap<>();

            Series rangeSeries = new Series( "95% Confidence" );

            for ( Edition ed : eds ) {
                EnrichmentResult er = data.get( ed ).get( term );
                if ( er != null ) {
                    StabilityScore sc = scores.get( ed );
                    rangeSeries.addDataPoint( ed.getDate(), sc.getMinPvalue(), sc.getMaxPvalue() );
                    Double s = sc.getScore();
                    // JSON does not support special Double types so we convert them to strings and deal with them in the front-end
                    dateToStabilityScore.put( ed.getDate().getTime(), s.isInfinite() || s.isNaN() ? s.toString() : s );
                }

            }
            cv = new ChartValues();
            cv.addSeries( rangeSeries );
            hcGsonMap.put( "errors", cv );
            hcGsonMap.put( "dateToStabilityScore", dateToStabilityScore );

        }

        RequestContext.getCurrentInstance().addCallbackParam( "HC_enrichment",
                new GsonBuilder().serializeNulls().create().toJson( hcGsonMap ) );
    }

    /**
     * Entry point to ajax call for creating different charts based on front-end settings such as:
     * <p>
     * by pvalue or rank
     * topN or selected
     */
    public void createChart( boolean byRank, boolean selected ) {
        if ( !selected ) {
            Set<GeneOntologyTerm> selectedTerms = combinedAnalysis.getEnrichmentAnalysis().getTopNTerms( enrichmentChartTopN );

            createChart( selectedTerms, enrichmentChartTopN, !byRank );
        } else {
            if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
                // If nothing selected graph everything
                createChart( null, null, !byRank );
            } else {
                // Graph selected terms
                Set<GeneOntologyTerm> selectedTerms = new HashSet<>();

                for ( EnrichmentTableValues entry : selectedEnrichmentTableValues ) {
                    selectedTerms.add( entry.getTerm() );
                }

                createChart( selectedTerms, null, !byRank );
            }
        }
    }

    /**
     * Filter enrichmentResults for just those terms in the given set
     *
     * @param terms terms wanted
     * @return filtered enrichment results with just those terms from the given set
     */
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> filter( Set<GeneOntologyTerm> terms ) {
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> filteredData = new HashMap<>();
        for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : enrichmentResults.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, EnrichmentResult> termsInEdition = new HashMap<>();
            filteredData.put( ed, termsInEdition );

            Map<GeneOntologyTerm, EnrichmentResult> editionData = editionEntry.getValue();

            for ( GeneOntologyTerm term : terms ) {
                EnrichmentResult er = editionData.get( term );
                if ( er != null ) {
                    termsInEdition.put( term, er );
                }
            }

        }

        return filteredData;

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
                    quantile = (int) Math.max(1, Math.min(20, 20 - (Math.ceil( val ) + 8 ) ));
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

    // Getters / Setters ---------------------------------------------------------------------------------------

    public List<Gene> getSelectedGenes() {
        return speciesToSelectedGenes.get( selectedSpecies );
    }

    public Edition getCurrentEdition() {
        return cache.getCurrentEditions( selectedSpecies );
    }

}
