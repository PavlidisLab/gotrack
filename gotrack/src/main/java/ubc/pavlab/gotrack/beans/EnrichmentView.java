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

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.analysis.*;
import ubc.pavlab.gotrack.beans.service.EnrichmentService;
import ubc.pavlab.gotrack.model.*;
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
import javax.faces.event.ActionEvent;
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
public class EnrichmentView implements Serializable {

    /**
     * Model for a select drop down in front end
     */
    public enum EnrichmentChartType {
        SELECTED("Selected"), TOP("Top");

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
        RANK("Rank"), PVALUE("P-Value");

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

    private static final Logger log = Logger.getLogger( EnrichmentView.class );

    // Max results returned during autocompletion
    private static final Integer MAX_RESULTS = 15;

    @Inject
    private Cache cache;

    @Inject
    private EnrichmentService enrichmentService;

    @Inject
    private SessionManager session;

    // View parameters
    private Gene queryGene;
    // LEE_LIVER_CANCER
    // http://www.broadinstitute.org/gsea/msigdb/geneset_page.jsp?geneSetName=LEE_LIVER_CANCER&keywords=lee%20AND%20liver
    // ACAA1 ACADS ADH1B ADH4 ADRA1A APOF C6 C8A C8B C9orf103 CD4 CETN2 CIDEB COLEC10 CYP2C18 CYP2C19 CYP2C8 CYP2E1
    // CYP4A11 DMGDH DNASE1L3 ETS2 FAM20C FCN1 FETUB GBA3 HPX IGFALS ITIH4 LCAT LDHD LOC401022 LPA NDRG2 PEMT PLG PPAP2B
    // RCAN1 RSPO3 RXRB SLC22A1 SLC22A3 SLC28A1 SLCO1B3 TMED8 TMEM27 TTC36 TTR UROC1

    // Adrenaline and noradrenaline biosynthesis
    // MAOA DDC VTI1B MAOB STX12 COMT SNAP25 SNAP23 SLC6A20 SLC6A2 TH SLC6A3 SNAP29 PNMT STX7 SLC18A1 VAMP1 SLC6A18
    // SLC6A19 SLC18A2 VAMP2 SLC6A15 SLC6A16 SLC6A17 DBH
    //private String bulkQuery = "ACAA1 ACADS ADH1B ADH4 ADRA1A APOF C6 C8A C8B C9orf103 CD4 CETN2 CIDEB COLEC10 CYP2C18 CYP2C19 CYP2C8 CYP2E1 CYP4A11 DMGDH DNASE1L3 ETS2 FAM20C FCN1 FETUB GBA3 HPX IGFALS ITIH4 LCAT LDHD LOC401022 LPA NDRG2 PEMT PLG PPAP2B RCAN1 RSPO3 RXRB SLC22A1 SLC22A3 SLC28A1 SLCO1B3 TMED8 TMEM27 TTC36 TTR UROC1";
    private String bulkQuery = "";

    // Holds matches for bulk adding genes
    private List<GeneMatch> geneMatches;

    // Map of species id -> hit list
    private Map<Species, List<Gene>> speciesToSelectedGenes = Maps.newHashMap();

    // Holds gene to be removed from hit list
    private Gene geneToRemove;

    // Holds gene to be viewed from hit list
    private Gene viewGene;

    // Enrichment Settings
    private Species currentSpecies = null;
    private int minAnnotatedPopulation = 5; // minimum size of gene set for a term to not be rejected
    private int maxAnnotatedPopulation = 200; // maximum size of gene set for a term to not be rejected
    private MultipleTestCorrection multipleTestCorrection = MultipleTestCorrection.BH; // method of multiple test correction
    private double pThreshold = 0.05; // threshold for bonferroni
    private double fdr = 0.05; //  fdr level for BH Step-up
    private List<Aspect> aspects = Lists.newArrayList( Aspect.values() ); // Aspect restriction

    // Enrichment Feedback
    private StatusPoller statusPoller = new StatusPoller( " completed" ); // Used to display feedback in the overview tab

    // Enrichment Data
    private Edition currentEdition; // Most current edition
    private EnrichmentAnalysis analysis; // Enrichment analysis
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults; // Results of the enrichment analysis
    private boolean enrichmentSuccess = false; // Whether the analysis succeeded
    private Species analysisSpecies;
    private Map<Long, Integer> currentDateToEdition;

    // Similarity Settings
    private static final int TOP_N_JACCARD = 5; // Number of terms to use in the similarity top N
    private SimilarityCompareMethod similarityCompareMethod = SimilarityCompareMethod.CURRENT; // Method to use in the similarity analysis

    // Enrichment Table Select
    private Map<Integer, Edition> enrichmentTableAllEditions = new HashMap<>(); // Holds the editions to be displayed in dropdown

    // Enrichment Table Data
    private List<EnrichmentTableValues> enrichmentTableValues = new ArrayList<>(); // data backing the enrichment table
    private List<EnrichmentTableValues> filteredEnrichmentTableValues;
    private List<EnrichmentTableValues> selectedEnrichmentTableValues;
    private Integer enrichmentTableEdition; // currently displayed edition
    private EnrichmentTableValues viewEnrichmentRow;
    private Set<Gene> viewEnrichmentRowGeneSet;
    private int enrichmentTableTotalSignificant;

    // Similarity Data
    private SimilarityAnalysis similarityAnalysis;

    // Stability Data
    private StabilityAnalysis stabilityAnalysis;
    private Map<Edition, double[]> stabilityRangeCache = Maps.newConcurrentMap();

    // Enrichment Chart
    private EnrichmentChartType enrichmentChartType = EnrichmentChartType.TOP;
    private EnrichmentChartMeasure enrichmentChartMeasure = EnrichmentChartMeasure.RANK;
    private int enrichmentChartTopN = 10;

    // Select Data Point functionality
    private Edition selectedEdition;
    private GeneOntologyTerm selectedTerm;
    private Number selectedValue;
    private String selectedValueName;

    // Select Data Point functionality Enrichment Chart
    private EnrichmentResult selectedEnrichmentResult;
    private StabilityScore selectedStabilityScore;

    // Select Data Point functionality Similarity Chart
    private SimilarityScore selectedSimilarityScore;

    // Static final

    public EnrichmentChartType[] getEnrichmentChartTypes() {
        return EnrichmentChartType.values();
    }

    public EnrichmentChartMeasure[] getEnrichmentChartMeasures() {
        return EnrichmentChartMeasure.values();
    }

    public EnrichmentView() {
        log.info( "EnrichmentView created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );

    }

    @PostConstruct
    public void postConstruct() {
        log.info( "postConstruct" );
        currentSpecies = session.getSpecies();

        for ( Species species : cache.getSpeciesList() ) {
            speciesToSelectedGenes.put( currentSpecies, Lists.<Gene>newArrayList() );
        }
    }

    /**
     * pre-render view
     * 
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
        if ( enrichmentSuccess ) {
            // When loading a previous analysis, we want to show the gene hit list that is associated with it, 
            // not the most recently viewed one.
            currentSpecies = analysisSpecies;
        } else {
            enrichmentTableEdition = cache.getCurrentEditions( currentSpecies ).getEdition();
            currentEdition = cache.getCurrentEditions( currentSpecies );
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
        currentEdition = cache.getCurrentEditions( currentSpecies );
        enrichmentSuccess = false;
        statusPoller = new StatusPoller( " completed" );
        double thresh = multipleTestCorrection.equals( MultipleTestCorrection.BONFERRONI ) ? pThreshold : fdr;
        CombinedAnalysis ca = enrichmentService.combinedAnalysis(
                new HashSet<>( speciesToSelectedGenes.get( currentSpecies ) ),
                currentSpecies,
                multipleTestCorrection, thresh, minAnnotatedPopulation, maxAnnotatedPopulation,
                new HashSet<>( aspects ), similarityCompareMethod, TOP_N_JACCARD, statusPoller );
        enrichmentSuccess = ca.isSuccess();
        analysisSpecies = currentSpecies;
        analysis = ca.getEnrichmentAnalysis();
        enrichmentResults = analysis.getResults();

        statusPoller.newStatus( "Creating tables and charts...", 90 );

        similarityAnalysis = ca.getSimilarityAnalysis();
        stabilityAnalysis = ca.getStabilityAnalysis();

        createTables();

        createSimilarityChart();

        createTermCountChart();

        // Container for a mapping that will be used on the front-end for ajax queries
        currentDateToEdition = Maps.newHashMap();
        for ( Edition ed : cache.getAllEditions( currentSpecies ) ) {
            currentDateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( currentDateToEdition ) );

        statusPoller.completeStatus();
        timer.stop();
        statusPoller.newStatus( "Finished in: " + timer.getTime() / 1000.0 + " seconds", 100 );

    }

    /**
     * Load the current enrichment analysis results stored in this session.
     */
    public void loadPreviousEnrichment() {
        if ( enrichmentSuccess ) {

            createSimilarityChart();
            createTermCountChart();

            RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( currentDateToEdition ) );

            RequestContext.getCurrentInstance().addCallbackParam( "success", true );
        } else {
            RequestContext.getCurrentInstance().addCallbackParam( "success", false );
        }
    }

    /**
     * Prepares table (enrichment, stability) data
     */
    private void createTables() {
        enrichmentTableEdition = Collections.max( enrichmentResults.keySet() ).getEdition();
        enrichmentTableAllEditions = new LinkedHashMap<>();
        ArrayList<Edition> eds = new ArrayList<>( enrichmentResults.keySet() );
        Collections.sort( eds, Collections.reverseOrder() );
        for ( Edition ed : eds ) {
            enrichmentTableAllEditions.put( ed.getEdition(), ed );
        }

        loadEnrichmentTableData();

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
        ChartValues cv = new ChartValues("GO Term Counts by Edition",
                "Count of Unique GO Terms", "Date");
        Series significantTerms = new Series( "Significant Terms" );
        Series allTerms = new Series( "All Tested Terms" );
        Series rejectedTerms = new Series( "Rejected Terms" );

        for ( Entry<Edition, Enrichment<GeneOntologyTerm, Gene>> rawResultsEntry : analysis.getRawResults()
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

        RequestContext.getCurrentInstance().addCallbackParam( "HC_terms", new Gson().toJson( hcGsonMap ) );
    }

    // Similarity Charts ---------------------------------------------------------------------------------------

    /**
     * Create similarity chart
     */
    private void createSimilarityChart() {
        // Create Similarity Chart
        ChartValues cv = new ChartValues("Enrichment Similarity to "
                + ( similarityCompareMethod.equals( SimilarityCompareMethod.PROXIMAL ) ? "Previous" : "Current" )
                + " Edition",
                "Jaccard Similarity Index", "Date");
        cv.setMin( 0 );
        cv.setMax( 1 );

        // Prepare series
        Series completeTermJaccard = new Series( "All Terms" );
        Series topTermJaccard = new Series( "Top " + TOP_N_JACCARD + " Terms" );
        Series topGeneJaccard = new Series( "Genes Backing Top Terms" );
        Series topParentsJaccard = new Series( "Parents of Top Terms" );

        // Fill in series
        for ( Entry<Edition, SimilarityScore> editionEntry : similarityAnalysis.getSimilarityScores().entrySet() ) {
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

        RequestContext.getCurrentInstance().addCallbackParam( "HC_similarity", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Entry point to ajax request for a graph of a single term with p-value error bands based on stability confidence
     */
    public void createStabilityChartFromSelected() {

        if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.size() != 1 ) {
            return;
            // return failed
        }
        EnrichmentTableValues sel = selectedEnrichmentTableValues.iterator().next();
        Set<GeneOntologyTerm> selectedTerms = new HashSet<>();
        selectedTerms.add( sel.getTerm() );

        createChart( selectedTerms, null, true );
    }

    // Enrichment Charts ---------------------------------------------------------------------------------------

    /**
     * Create chart for comparing terms and seeing how their p-values or ranks change over time. If a single term is
     * selected and the graph is to be based on p-value (not rank) then error bands are also sent.
     * 
     * @param selectedTerms Terms to be graphed, if null grap everything
     * @param topN Cutoff for the 'desired' region in the graph, selectedTerms should already be trimmed down to topN
     * @param pvalue true if p-value is to be graphed, false if rank
     */
    private void createChart( Set<GeneOntologyTerm> selectedTerms, Integer topN, boolean pvalue ) {
        // Get data
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data = null;
        if ( selectedTerms == null ) {
            selectedTerms = analysis.getTermsSignificantInAnyEdition();
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
                    "P-Value", "Date");

            for ( Series s : series.values() ) {
                cv.addSeries( s );
            }

            Map<Long, Double> cutoffs = new TreeMap<>();
            for ( Edition ed : eds ) {
                cutoffs.put( ed.getDate().getTime(), analysis.getCutoff( ed ) );
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
                int maxSignificantRank = analysis.getTermsSignificant( ed ).size() - 1;

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
                    double newRank = standardRank + ( termSet.size() - 1 ) / 2.0;
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

            cv = new ChartValues("Enrichment Results by Relative Rank",
                    "Relative Rank", "Date");

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
            Map<Edition, StabilityScore> scores = stabilityAnalysis.getStabilityScores( term );
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
     * 
     * by pvalue or rank
     * topN or selected
     */
    public void createChart() {

        if ( enrichmentChartType.equals( EnrichmentChartType.TOP ) ) {
            Set<GeneOntologyTerm> selectedTerms = analysis.getTopNTerms( enrichmentChartTopN );

            createChart( selectedTerms, enrichmentChartTopN,
                    enrichmentChartMeasure.equals( EnrichmentChartMeasure.PVALUE ) );
        } else {
            if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
                // If nothing selected graph everything
                createChart( null, null, enrichmentChartMeasure.equals( EnrichmentChartMeasure.PVALUE ) );
            } else {
                // Graph selected terms
                Set<GeneOntologyTerm> selectedTerms = new HashSet<>();

                for ( EnrichmentTableValues entry : selectedEnrichmentTableValues ) {
                    selectedTerms.add( entry.getTerm() );
                }

                createChart( selectedTerms, null, enrichmentChartMeasure.equals( EnrichmentChartMeasure.PVALUE ) );
            }
        }

    }

    /**
     * 
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
     * Entry point for retrieving data on click of similarity chart
     */
    public void fetchSimilarityInformation() {
        Integer edition = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        Edition ed = cache.getEdition( currentSpecies, edition );
        selectedSimilarityScore = similarityAnalysis.getSimilarityScores( ed );
    }

    /**
     * Used for clicks on data points in enrichment/stability chart
     */
    public void fetchTermInformation() {
        String termId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "termId" );
        Integer edition = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );

        Edition ed = cache.getEdition( currentSpecies, edition );
        selectedEdition = ed;

        Map<GeneOntologyTerm, EnrichmentResult> a = enrichmentResults.get( ed );
        if ( a != null ) {
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : a.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();

                if ( term.getGoId().equals( termId ) ) {
                    EnrichmentResult er = termEntry.getValue();
                    selectedTerm = term;
                    selectedEnrichmentResult = er;
                    selectedStabilityScore = stabilityAnalysis.getStabilityScores( selectedTerm, selectedEdition );
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

    // Stability ---------------------------------------------------------------------------------------

    private double[] stabilityRange( Edition ed ) {
        if ( ed == null ) {
            return null;
        }

        double[] range = stabilityRangeCache.get( ed );
        if ( range == null ) {
            // Figure out range of values for Stability scores

            Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );

            double minStability = Double.POSITIVE_INFINITY;
            double maxStability = Double.NEGATIVE_INFINITY;
            for ( GeneOntologyTerm term : editionData.keySet() ) {
                StabilityScore sc = stabilityAnalysis.getStabilityScores( term, ed );
                Double v = sc.getScore();
                if ( !v.isInfinite() && !v.isNaN() ) {
                    if ( v > maxStability ) maxStability = v;
                    if ( v < minStability ) minStability = v;
                }
            }
            range = new double[2];
            range[0] = minStability;
            range[1] = maxStability;
            stabilityRangeCache.put( ed, range );
        }

        return range;
    }

    // Enrichment Table ---------------------------------------------------------------------------------------

    /**
     * Prepares data for enrichment table
     */
    public void loadEnrichmentTableData() {
        filteredEnrichmentTableValues = null;
        enrichmentTableValues = new ArrayList<>();
        enrichmentTableTotalSignificant = 0;
        Edition ed = enrichmentTableAllEditions.get( enrichmentTableEdition );
        if ( ed != null ) {
            double[] stabilityRange = stabilityRange( ed );
            Set<GeneOntologyTerm> sigTerms = analysis.getTermsSignificant( ed );
            enrichmentTableTotalSignificant = sigTerms.size();
            Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : editionData.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                EnrichmentResult er = termEntry.getValue();
                StabilityScore sc = stabilityAnalysis.getStabilityScores( term, ed );
                Double val = sc.getScore();
                int quantile = 0;
                if ( !val.isNaN() && !val.isInfinite() ) {
                    quantile = ( int ) Math
                            .round( 19 * ( val - stabilityRange[0] ) / ( stabilityRange[1] - stabilityRange[0] ) ) + 1;
                }

                enrichmentTableValues
                        .add( new EnrichmentTableValues( ed, term, er, sc, quantile, sigTerms.contains( term ) ) );
            }
            Collections.sort( enrichmentTableValues );

        }
    }

    /**
     * Right click -> view functionality for enrichment table
     */
    public void viewEnrichmentTableValue() {
        if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
            return;
        }
        viewEnrichmentRow = selectedEnrichmentTableValues.iterator().next();
        viewEnrichmentRowGeneSet = analysis.getGeneSet( viewEnrichmentRow.getEdition(), viewEnrichmentRow.getTerm() );
    }

    /**
     * custom filter function for primefaces data table column, filters by arbitrary number in a 'less than' manner
     */
    public boolean filterByNumberLT( Object value, Object filter, Locale locale ) {
        String filterText = ( filter == null ) ? null : filter.toString().trim();
        if ( filterText == null || filterText.equals( "" ) ) {
            return true;
        }

        if ( value == null ) {
            return false;
        }

        return Double.compare( ( ( Number ) value ).doubleValue(), Double.valueOf( filterText ) ) < 0;
    }

    // Gene List ---------------------------------------------------------------------------------------

    /**
     * Remove all genes from selected species' hit list
     */
    public void removeAllGenes( ActionEvent actionEvent ) {
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpecies );
        if ( selectGenes != null ) {
            selectGenes.clear();
        }
    }

    /**
     * Add gene to to hit list
     */
    public void addGene( ActionEvent actionEvent ) {
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpecies );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpecies, selectGenes );
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
    public void searchMultipleGenes( ActionEvent actionEvent ) {
        Set<String> inputSet = new HashSet<>( Arrays.asList( bulkQuery.split( "\\s*(,|\\s)\\s*" ) ) );
        geneMatches = new ArrayList<>();
        Map<GeneMatch.Level, Integer> cntMap = new HashMap<>();

        for ( String geneInput : inputSet ) {
            GeneMatch geneMatch = cache.guessGeneBySymbol( geneInput, currentSpecies );
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
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpecies );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpecies, selectGenes );
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
    public void removeGene() {
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpecies );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpecies, selectGenes );
        }
        selectGenes.remove( geneToRemove );
        addMessage( "Gene (" + geneToRemove.getSymbol() + ") successfully removed.", FacesMessage.SEVERITY_INFO );
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
        if ( StringUtils.isEmpty( query.trim() ) || currentSpecies == null ) return Lists.newArrayList();
        return Lists.newArrayList( this.cache.searchGeneBySymbol( query.trim(), currentSpecies, MAX_RESULTS ) );
    }

    /**
     * Autocomplete term by id
     */
    public List<String> completeId( String query ) {
        if ( query == null ) return new ArrayList<String>();

        String queryUpper = query.toUpperCase();

        Collection<String> exact = new HashSet<String>();
        Collection<String> possible = new HashSet<String>();
        for ( GeneOntologyTerm term : analysis.getTermsSignificantInAnyEdition() ) {
            if ( queryUpper.equals( term.getGoId().toUpperCase() ) ) {
                exact.add( term.getGoId() );
                continue;
            }

            String pattern = "(.*)" + queryUpper + "(.*)";
            // Pattern r = Pattern.compile(pattern);
            String m = term.getGoId().toUpperCase();
            // Matcher m = r.matcher( term.getTerm() );
            if ( m.matches( pattern ) ) {
                possible.add( term.getGoId() );
                continue;
            }

        }

        List<String> orderedResults = new ArrayList<>();

        orderedResults.addAll( exact );

        ArrayList<String> p = new ArrayList<String>( possible );
        // Collections.sort( p, new LevenshteinComparator( query ) );

        orderedResults.addAll( p );
        return orderedResults;

    }

    // Getters / Setters ---------------------------------------------------------------------------------------


    public Species getCurrentSpecies() {
        return currentSpecies;
    }

    public void setCurrentSpecies( Species currentSpecies ) {
        this.currentSpecies = currentSpecies;
    }

    public boolean isEnrichmentSuccess() {
        return enrichmentSuccess;
    }

    public MultipleTestCorrection getMultipleTestCorrection() {
        return multipleTestCorrection;
    }

    public void setMultipleTestCorrection( MultipleTestCorrection multipleTestCorrection ) {
        this.multipleTestCorrection = multipleTestCorrection;
    }

    public List<Aspect> getAspects() {
        return aspects;
    }

    public void setAspects( List<Aspect> aspects ) {
        this.aspects = aspects;
    }

    public double getpThreshold() {
        return pThreshold;
    }

    public void setpThreshold( double pThreshold ) {
        this.pThreshold = pThreshold;
    }

    public double getFdr() {
        return fdr;
    }

    public void setFdr( double fdr ) {
        this.fdr = fdr;
    }

    public Gene getQueryGene() {
        return this.queryGene;
    }

    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }

    public String getBulkQuery() {
        return bulkQuery;
    }

    public void setBulkQuery( String bulkQuery ) {
        this.bulkQuery = bulkQuery;
    }

    public Gene getGeneToRemove() {
        return geneToRemove;
    }

    public void setGeneToRemove( Gene geneToRemove ) {
        this.geneToRemove = geneToRemove;
    }

    public List<Gene> getSelectedGenes() {
        return speciesToSelectedGenes.get( currentSpecies );
    }

    public Edition getCurrentEdition() {
        return cache.getCurrentEditions( currentSpecies );
    }

    public Gene getViewGene() {
        return viewGene;
    }

    public void setViewGene( Gene viewGene ) {
        this.viewGene = viewGene;
    }

    public int getMinAnnotatedPopulation() {
        return minAnnotatedPopulation;
    }

    public void setMinAnnotatedPopulation( int minAnnotatedPopulation ) {
        this.minAnnotatedPopulation = minAnnotatedPopulation;
    }

    public int getMaxAnnotatedPopulation() {
        return maxAnnotatedPopulation;
    }

    public void setMaxAnnotatedPopulation( int maxAnnotatedPopulation ) {
        this.maxAnnotatedPopulation = maxAnnotatedPopulation;
    }

    public Integer getEnrichmentTableEdition() {
        return enrichmentTableEdition;
    }

    public void setEnrichmentTableEdition( Integer enrichmentTableEdition ) {
        this.enrichmentTableEdition = enrichmentTableEdition;
    }

    public Collection<Edition> getEnrichmentTableAllEditions() {
        return enrichmentTableAllEditions.values();
    }

    public List<EnrichmentTableValues> getFilteredEnrichmentTableValues() {
        return filteredEnrichmentTableValues;
    }

    public void setFilteredEnrichmentTableValues( List<EnrichmentTableValues> filteredEnrichmentTableValues ) {
        this.filteredEnrichmentTableValues = filteredEnrichmentTableValues;
    }

    public List<EnrichmentTableValues> getSelectedEnrichmentTableValues() {
        return selectedEnrichmentTableValues;
    }

    public void setSelectedEnrichmentTableValues( List<EnrichmentTableValues> selectedEnrichmentTableValues ) {
        this.selectedEnrichmentTableValues = selectedEnrichmentTableValues;
    }

    public List<EnrichmentTableValues> getEnrichmentTableValues() {
        return enrichmentTableValues;
    }

    public int getEnrichmentTableTotalSignificant() {
        return enrichmentTableTotalSignificant;
    }

    public EnrichmentChartMeasure getEnrichmentChartMeasure() {
        return enrichmentChartMeasure;
    }

    public void setEnrichmentChartMeasure( EnrichmentChartMeasure enrichmentChartMeasure ) {
        this.enrichmentChartMeasure = enrichmentChartMeasure;
    }

    public int getEnrichmentChartTopN() {
        return enrichmentChartTopN;
    }

    public void setEnrichmentChartTopN( int enrichmentChartTopN ) {
        this.enrichmentChartTopN = enrichmentChartTopN;
    }

    public Edition getSelectedEdition() {
        return selectedEdition;
    }

    public GeneOntologyTerm getSelectedTerm() {
        return selectedTerm;
    }

    public Number getSelectedValue() {
        return selectedValue;
    }

    public String getSelectedValueName() {
        return selectedValueName;
    }

    public EnrichmentResult getSelectedEnrichmentResult() {
        return selectedEnrichmentResult;
    }

    public StabilityScore getSelectedStabilityScore() {
        return selectedStabilityScore;
    }

    public List<String> getEnrichmentStatus() {
        return statusPoller.getStatuses();
    }

    public int getEnrichmentProgress() {
        return statusPoller.getProgress();
    }

    public StatusPoller getStatusPoller() {
        return statusPoller;
    }

    public SimilarityCompareMethod getSimilarityCompareMethod() {
        return similarityCompareMethod;
    }

    public void setSimilarityCompareMethod( SimilarityCompareMethod similarityCompareMethod ) {
        this.similarityCompareMethod = similarityCompareMethod;
    }

    public EnrichmentAnalysis getAnalysis() {
        return analysis;
    }

    public EnrichmentTableValues getViewEnrichmentRow() {
        return viewEnrichmentRow;
    }

    public Set<Gene> getViewEnrichmentRowGeneSet() {
        return viewEnrichmentRowGeneSet;
    }

    public List<GeneMatch> getGeneMatches() {
        return geneMatches;
    }

    public EnrichmentChartType getEnrichmentChartType() {
        return enrichmentChartType;
    }

    public void setEnrichmentChartType( EnrichmentChartType enrichmentChartType ) {
        this.enrichmentChartType = enrichmentChartType;
    }

    public SimilarityScore getSelectedSimilarityScore() {
        return selectedSimilarityScore;
    }
}
