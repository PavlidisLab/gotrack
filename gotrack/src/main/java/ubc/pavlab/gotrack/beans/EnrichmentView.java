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

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;

import com.google.gson.Gson;

import ubc.pavlab.gotrack.analysis.CombinedAnalysis;
import ubc.pavlab.gotrack.analysis.EnrichmentAnalysis;
import ubc.pavlab.gotrack.analysis.EnrichmentResult;
import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityCompareMethod;
import ubc.pavlab.gotrack.analysis.SimilarityScore;
import ubc.pavlab.gotrack.analysis.StabilityAnalysis;
import ubc.pavlab.gotrack.analysis.StabilityScore;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.StatusPoller;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.table.EnrichmentTableValues;
import ubc.pavlab.gotrack.model.table.GeneMatches;
import ubc.pavlab.gotrack.model.table.GeneMatches.MatchType;
import ubc.pavlab.gotrack.model.table.StabilityTableValues;

/**
 * View bean based around the enrichment functionality of GOTrack.
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
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

    // Max size of hit list
    private static final int MAX_GENESET_SIZE = 300;

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{annotationService}")
    private AnnotationService annotationService;

    // View parameters
    private String query;
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
    private List<GeneMatches> geneMatches;

    // Map of species id -> hit list
    private Map<Integer, List<Gene>> speciesToSelectedGenes = new HashMap<>();

    // Holds gene to be removed from hit list
    private Gene geneToRemove;

    // Holds gene to be viewed from hit list
    private Gene viewGene;

    // Enrichment Settings
    private Integer currentSpeciesId = 7; // species id
    private int minAnnotatedPopulation = 5; // minimum size of gene set for a term to not be rejected
    private int maxAnnotatedPopulation = 200; // maximum size of gene set for a term to not be rejected
    private MultipleTestCorrection multipleTestCorrection = MultipleTestCorrection.BH; // method of multiple test correction
    private double pThreshold = 0.05; // threshold for bonferroni
    private double fdr = 0.05; //  fdr level for BH Step-up
    private List<Aspect> aspects = Arrays.asList( Aspect.values() ); // Aspect restriction

    // Enrichment Feedback
    private StatusPoller statusPoller = new StatusPoller( " completed" ); // Used to display feedback in the overview tab

    // Enrichment Data
    private Edition currentEdition; // Most current edition
    private EnrichmentAnalysis analysis; // Enrichment analysis
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults; // Results of the enrichment analysis
    private boolean enrichmentSuccess = false; // Whether the analysis succeeded

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

    // Stability Data
    private StabilityAnalysis stabilityAnalysis;

    // Stability Table Data
    private List<StabilityTableValues> stabilityTableValues = new ArrayList<>();
    private List<StabilityTableValues> filteredStabilityTableValues;
    private StabilityTableValues selectedStabilityTableValue;

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

        enrichmentTableEdition = cache.getCurrentEditions( currentSpeciesId ).getEdition();
        currentEdition = cache.getCurrentEditions( currentSpeciesId );
        return null;
    }

    /**
     * Retrieves data from cache and/or from database for given hit list, writing live status udpates to the
     * StatusPoller.
     * 
     * @param genes set of genes to either retrieve
     * @param currentSpeciesId species id of genes
     * @param filterAspect keep only terms of these aspects, if null or empty the filter is not applied
     * @param statusPoller poller for live status updates
     * @return data necessary for enrichment of given hitlist
     */
    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> retrieveData( Set<Gene> genes, Integer currentSpeciesId,
            Set<Aspect> filterAspect, StatusPoller statusPoller ) {

        if ( genes != null && !genes.isEmpty() ) {
            log.info( "Current species: " + currentSpeciesId );
            log.info( "Geneset Size: " + genes.size() );

            if ( genes.size() > MAX_GENESET_SIZE ) {
                statusPoller.newStatus( "Gene Hit List too large; maximum geneset size is " + MAX_GENESET_SIZE + "!",
                        50 );
                return null;
            }

            log.info( "retrieving gene data..." );
            statusPoller.newStatus( "Retrieving Gene Information from cache...", 5 );

            // Container for final enrichment data
            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = new HashMap<>();

            Set<Gene> genesToLoad = new HashSet<>();
            for ( Gene gene : genes ) {
                Map<Edition, Set<GeneOntologyTerm>> cachedGeneData = cache.getEnrichmentData( gene );
                if ( cachedGeneData != null ) {
                    addGeneData( gene, cachedGeneData, filterAspect, geneGOMap );
                } else {
                    genesToLoad.add( gene );
                }
            }
            statusPoller.completeStatus();

            statusPoller.newStatus( "Retrieving Gene Information from database...", 10 );

            // If not all genes have been loaded from cache we must hit the database for the rest and cache the results
            if ( !genesToLoad.isEmpty() ) {

                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB;

                statusPoller.completeStatus();
                statusPoller.newStatus( "Propagating GO Terms...", 30 );

                geneGOMapFromDB = annotationService.fetchEnrichmentData( currentSpeciesId, genesToLoad );

                log.info( "Propagating GO Terms..." );
                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> prop = propagate( geneGOMapFromDB );
                log.info( "Propagating GO Terms... COMPLETE " );

                for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : prop.entrySet() ) {

                    addGeneData( geneEntry.getKey(), geneEntry.getValue(), filterAspect, geneGOMap );
                    cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
                }
                statusPoller.completeStatus();

                log.info( "Retrieved (" + genesToLoad.size() + ") genes from db and ("
                        + ( genes.size() - genesToLoad.size() ) + ") from cache" );
            } else {
                log.info( "Retrieved all (" + genes.size() + ") genes from cache" );

            }
            // timer.stop();
            // log.info( "Total time: " + timer );
            // for ( Entry<String, Long> entry : timerMap.entrySet() ) {
            // log.info( entry.getKey() + " - " + entry.getValue() + " - " + entry.getValue()
            // / ( double ) timer.getNanoTime() );
            // }

            return geneGOMap;

        } else {
            log.info( "Empty geneset" );
            return null;
        }
    }

    /**
     * Add a new gene/data pair to the data of previously retrieved genes.
     * 
     * @param g gene we are adding data for
     * @param cachedGeneData cached data for given gene that will be added to enrichment data
     * @param filterAspect only add these aspects, ignore filter if null or empty
     * @param data enrichment data for previously retrieved genes
     */
    private void addGeneData( Gene g, Map<Edition, Set<GeneOntologyTerm>> cachedGeneData, Set<Aspect> filterAspect,
            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> data ) {

        boolean bypassFilter = ( filterAspect == null || filterAspect.size() == 0
                || filterAspect.size() == Aspect.values().length );

        for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : cachedGeneData.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, Set<Gene>> m1 = data.get( ed );

            if ( m1 == null ) {
                // Previously had no data for this edition
                m1 = new HashMap<>();
                data.put( ed, m1 );
            }

            for ( GeneOntologyTerm term : editionEntry.getValue() ) {
                if ( bypassFilter || filterAspect.contains( term.getAspect() ) ) {
                    Set<Gene> geneSet = m1.get( term );
                    if ( geneSet == null ) {
                        // Previously had no genes for this term in this edition
                        geneSet = new HashSet<>();
                        m1.put( term, geneSet );
                    }

                    geneSet.add( g );
                }

            }

        }
    }

    /**
     * Propagates the term sets in data retrieved from database
     * 
     * @param geneGOMapFromDB unpropagated data retrieved straight from database
     * @return Map adding the propagated terms into the term sets
     */
    private Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> propagate(
            Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB ) {
        Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> propagatedData = new HashMap<>();
        for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : geneGOMapFromDB.entrySet() ) {
            Gene g = geneEntry.getKey();
            Map<Edition, Set<GeneOntologyTerm>> propagatedSeries = new HashMap<>();
            propagatedData.put( g, propagatedSeries );
            Map<Edition, Set<GeneOntologyTerm>> series = geneEntry.getValue();
            for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : series.entrySet() ) {
                Edition ed = editionEntry.getKey();
                Set<GeneOntologyTerm> propagatedTerms = cache.propagate( editionEntry.getValue(), ed );

                if ( propagatedTerms == null ) {
                    // No ontology exists for this edition
                } else {
                    propagatedSeries.put( ed, propagatedTerms );
                }

            }

        }
        // cache.ontologyStats();
        return propagatedData;
    }

    /**
     * Run enrichment analysis given settings from front-end enrichment page followed by creation of tables and charts
     * for display.
     */
    public void enrich() {
        StopWatch timer = new StopWatch();
        timer.start();
        currentEdition = cache.getCurrentEditions( currentSpeciesId );
        enrichmentSuccess = false;
        statusPoller = new StatusPoller( " completed" );
        double thresh = multipleTestCorrection.equals( MultipleTestCorrection.BONFERRONI ) ? pThreshold : fdr;
        CombinedAnalysis ca = enrich( new HashSet<>( speciesToSelectedGenes.get( currentSpeciesId ) ), currentSpeciesId,
                multipleTestCorrection, thresh, minAnnotatedPopulation, maxAnnotatedPopulation,
                new HashSet<>( aspects ), similarityCompareMethod, TOP_N_JACCARD, statusPoller );
        enrichmentSuccess = ca.isSuccess();
        analysis = ca.getEnrichmentAnalysis();
        enrichmentResults = analysis.getResults();

        statusPoller.newStatus( "Creating tables and charts...", 90 );

        stabilityAnalysis = ca.getStabilityAnalysis();

        createTables();

        createSimilarityChart();

        statusPoller.completeStatus();
        timer.stop();
        statusPoller.newStatus( "Finished in: " + timer.getTime() / 1000.0 + " seconds", 100 );

    }

    /**
     * Runs enrichment/similarity/stability analyses given input settings.
     * 
     * @param genes hitlist
     * @param spId species id
     * @param mtc method of multiple tests correction
     * @param thresh Either p-value cutoff if using Bonferroni or FDR level if using BH step-up
     * @param min minimum geneset size a specific term must have to be included in results
     * @param max maximum geneset size a specific term must have to be included in results
     * @param aspects only add these aspects, ignore filter if null or empty
     * @param scm method for similarity comparison
     * @param topN number of top terms to use for top N series
     * @param statusPoller poller for live status updates
     * @return Container class holding the enrichment and stability/similarity analyses
     */
    public CombinedAnalysis enrich( Set<Gene> genes, int spId, MultipleTestCorrection mtc, double thresh, int min,
            int max, Set<Aspect> aspects, SimilarityCompareMethod scm, int topN, StatusPoller statusPoller ) {
        statusPoller.newStatus( "Starting Enrichment Analysis", 0 );

        Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = retrieveData( genes, spId, aspects, statusPoller );

        if ( geneGOMap == null || geneGOMap.isEmpty() ) {
            statusPoller.newStatus( "Failed", 100 );
            return new CombinedAnalysis( null, null, false );
        }

        statusPoller.newStatus( "Retrieving Sample Sizes...", 50 );
        // This contains a part of the contingency tables we will need to create
        Map<Edition, Integer> sampleSizes = calculateSampleSizes( geneGOMap );
        statusPoller.completeStatus();

        statusPoller.newStatus( "Running Overrepresentation Analyses on all editions...", 55 );
        log.info( "Running enrichment analysis" );

        EnrichmentAnalysis analysis = new EnrichmentAnalysis( geneGOMap, sampleSizes, min, max, mtc, thresh, cache,
                spId );

        statusPoller.completeStatus();

        log.info( "Running stability analysis" );

        statusPoller.newStatus( "Running Stability Analyses on all editions...", 85 );

        StabilityAnalysis stabilityAnalysis = new StabilityAnalysis( analysis, topN, scm, cache );
        statusPoller.completeStatus();
        log.info( "Analysis Complete" );

        return new CombinedAnalysis( analysis, stabilityAnalysis, true );

    }

    /**
     * 
     * Calculate total number of genes existing in each edition from the hitlist
     * 
     * @param geneGOMap enrichment data
     * @return Map of Edition -> Number of genes from hit list that existed in that edition (existence meaning had any
     *         annotations)
     */
    private Map<Edition, Integer> calculateSampleSizes( Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap ) {
        log.info( "Retrieving sample sizes" );

        // Map<Edition, Integer> sampleSizes = annotationDAO.enrichmentSampleSizes( currentSpeciesId, genes );
        Map<Edition, Integer> sampleSizes = new HashMap<>();
        Map<Edition, Set<Gene>> testMap = new HashMap<>();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Gene>>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Set<Gene> gset = new HashSet<>();
            testMap.put( ed, gset );
            for ( Entry<GeneOntologyTerm, Set<Gene>> termEntry : editionEntry.getValue().entrySet() ) {
                gset.addAll( termEntry.getValue() );

            }

        }

        for ( Entry<Edition, Set<Gene>> editionEntry : testMap.entrySet() ) {
            sampleSizes.put( editionEntry.getKey(), editionEntry.getValue().size() );
        }

        return sampleSizes;
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
        loadStabilityTableData();

    }

    // Similarity Charts ---------------------------------------------------------------------------------------

    /**
     * Create similarity chart
     */
    private void createSimilarityChart() {
        // Create Similarity Chart
        ChartValues cv = new ChartValues();

        // Prepare series
        Series completeTermJaccard = new Series( "All Terms" );
        Series topTermJaccard = new Series( "Top " + TOP_N_JACCARD + " Terms" );
        Series topGeneJaccard = new Series( "Genes Backing Top Terms" );
        Series topParentsJaccard = new Series( "Parents of Top Terms" );

        // Container for a mapping that will be used on the front-end for ajax queries
        Map<Long, Integer> dateToEdition = new HashMap<>();

        // Fill in series
        for ( Entry<Edition, SimilarityScore> editionEntry : stabilityAnalysis.getSimilarityScores().entrySet() ) {
            dateToEdition.put( editionEntry.getKey().getDate().getTime(), editionEntry.getKey().getEdition() );
            SimilarityScore score = editionEntry.getValue();
            Date date = editionEntry.getKey().getDate();
            completeTermJaccard.addDataPoint( date, score.getCompleteTermJaccard() );
            topTermJaccard.addDataPoint( date, score.getTopTermJaccard() );
            topGeneJaccard.addDataPoint( date, score.getTopGeneJaccard() );
            if ( score.getTopParentsJaccard() != null ) {
                topParentsJaccard.addDataPoint( date, score.getTopParentsJaccard() );
            }

        }

        cv.addSeries( completeTermJaccard );
        cv.addSeries( topTermJaccard );
        cv.addSeries( topGeneJaccard );

        if ( topParentsJaccard.getData().size() > 0 ) {
            cv.addSeries( topParentsJaccard );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", cv );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Similarity to "
                + ( similarityCompareMethod.equals( SimilarityCompareMethod.PROXIMAL ) ? "Previous" : "Current" )
                + " Edition" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Jaccard Similarity Index" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_dateToEdition", new Gson().toJson( dateToEdition ) );

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

        // Container for a mapping that will be used on the front-end for ajax queries
        Map<Long, Integer> dateToEdition = new HashMap<>();

        // Chart
        ChartValues cv = null;

        double maxRank = -1; // for use in rank metric

        if ( pvalue ) {
            // Graph p-values
            Map<GeneOntologyTerm, Series> series = new HashMap<>();
            for ( Edition ed : eds ) {
                dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
                for ( GeneOntologyTerm term : selectedTerms ) {
                    EnrichmentResult er = data.get( ed ).get( term );
                    if ( er != null ) {
                        Series s = series.get( term );
                        if ( s == null ) {
                            s = new Series( term.getGoId() );
                            series.put( term, s );
                        }
                        s.addDataPoint( ed.getDate(), er.getPvalue() );
                    }
                }

            }
            cv = new ChartValues();

            for ( Series s : series.values() ) {
                cv.addSeries( s );
            }
            if ( selectedTerms.size() == 1 ) {
                RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Stability Results" );
            } else {
                RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Results" );
            }
            RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "P-Value" );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_type", "pvalue" );

            Map<Long, Double> cutoffs = new TreeMap<>();
            for ( Entry<Edition, Double> entry : analysis.getCutoffs().entrySet() ) {
                cutoffs.put( entry.getKey().getDate().getTime(), entry.getValue() );
            }

            RequestContext.getCurrentInstance().addCallbackParam( "hc_cutoffs", new Gson().toJson( cutoffs ) );
        } else {
            // rank

            // Holds map of date -> max rank that is still significant, this is used in the font-end
            // to determine the regions of the graph
            Map<Long, Double> dateToMaxSigRank = new HashMap<>();

            Map<GeneOntologyTerm, Series> series = new HashMap<>();

            boolean outsideTopNCheck = false; // Is any term outside of the topN in any edition
            boolean insignificantCheck = false; // Are there any terms which are insignificant in an edition
            for ( Edition ed : eds ) {
                int maxSignificantRank = analysis.getTermsSignificant().get( ed ).size() - 1;

                // First we compute the relative ranks among the terms selected
                dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
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
                    Series s = series.get( term );
                    if ( s == null ) {
                        s = new Series( term.getGoId() );
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

            cv = new ChartValues();

            for ( Series s : series.values() ) {
                cv.addSeries( s );
            }
            RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Results by Relative Rank" );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Relative Rank" );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_maxRank", maxRank );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_dateToMaxSigRank",
                    new Gson().toJson( dateToMaxSigRank ) );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_insignificantCheck", insignificantCheck );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_outsideTopNCheck", outsideTopNCheck );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_type", "rank" );
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

        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", cv );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_dateToEdition", new Gson().toJson( dateToEdition ) );

        if ( selectedTerms.size() == 1 && pvalue ) {
            // if there is a single graphed term and pvalue is the chosen metric we also create error bands to display
            // the 95% confidence around each point
            GeneOntologyTerm term = selectedTerms.iterator().next();
            Map<Edition, StabilityScore> scores = stabilityAnalysis.getStabilityScores( term );
            Map<Long, Object> dateToStabilityScore = new HashMap<>();

            Series rangeSeries = new Series( "95% Confidence" );

            for ( Edition ed : eds ) {
                dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
                EnrichmentResult er = data.get( ed ).get( term );
                if ( er != null ) {
                    StabilityScore sc = scores.get( ed );
                    rangeSeries.addDataPoint( ed.getDate(), sc.getMinPvalue(), sc.getMaxPvalue() );
                    Double s = sc.getScore();
                    // JSON does not support NaN types so we convert them to strings and deal with them in the front-end
                    dateToStabilityScore.put( ed.getDate().getTime(), s.isInfinite() || s.isNaN() ? s.toString() : s );
                }

            }
            cv = new ChartValues();
            cv.addSeries( rangeSeries );

            RequestContext.getCurrentInstance().addCallbackParam( "hc_errors", cv );

            RequestContext.getCurrentInstance().addCallbackParam( "hc_dateToStabilityScore",
                    new Gson().toJson( dateToStabilityScore ) );

        }
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

            RequestContext.getCurrentInstance().addCallbackParam( "hc_topN", enrichmentChartTopN );
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
        Edition ed = cache.getEdition( currentSpeciesId, edition );
        selectedSimilarityScore = stabilityAnalysis.getSimilarityScores( ed );
    }

    /**
     * Used for clicks on data points in enrichment/stability chart
     */
    public void fetchTermInformation() {
        String termId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "termId" );
        Integer edition = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );

        Edition ed = cache.getEdition( currentSpeciesId, edition );
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

            selectedTerm = null;

        }

    }

    // Stability Table ---------------------------------------------------------------------------------------

    /**
     * Entry point to ajax request for a graph of a single term with p-value error bands based on stability confidence
     */
    public void createStabilityChartFromSelected() {

        if ( selectedStabilityTableValue == null ) {
            return;
            // return failed
        }

        Set<GeneOntologyTerm> selectedTerms = new HashSet<>();
        selectedTerms.add( selectedStabilityTableValue.getTerm() );

        createChart( selectedTerms, null, true );
    }

    /**
     * Prepares stability table data
     */
    public void loadStabilityTableData() {
        filteredStabilityTableValues = null;
        stabilityTableValues = new ArrayList<>();
        Edition ed = currentEdition;

        Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );

        // Figure out range of values for Stability scores
        double minStabilityAvg = Double.POSITIVE_INFINITY;
        double maxStabilityAvg = Double.NEGATIVE_INFINITY;
        double minStability = Double.POSITIVE_INFINITY;
        double maxStability = Double.NEGATIVE_INFINITY;
        for ( GeneOntologyTerm term : editionData.keySet() ) {
            StabilityScore sc = stabilityAnalysis.getStabilityScores( term, ed );
            Double v = sc.getAverageScore();
            if ( !v.isInfinite() && !v.isNaN() ) {
                if ( v > maxStabilityAvg ) maxStabilityAvg = v;
                if ( v < minStabilityAvg ) minStabilityAvg = v;
            }
            v = sc.getScore();
            if ( !v.isInfinite() && !v.isNaN() ) {
                if ( v > maxStability ) maxStability = v;
                if ( v < minStability ) minStability = v;
            }
        }

        // Assign each score a quantile which will determine its color shading in a  gradient
        for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : editionData.entrySet() ) {
            GeneOntologyTerm term = termEntry.getKey();
            EnrichmentResult er = termEntry.getValue();
            StabilityScore sc = stabilityAnalysis.getStabilityScores( term, ed );
            Double val = sc.getAverageScore();
            int quantileAvg = 0;
            if ( !val.isNaN() && !val.isInfinite() ) {
                quantileAvg = ( int ) Math
                        .round( 19 * ( val - minStabilityAvg ) / ( maxStabilityAvg - minStabilityAvg ) ) + 1;
            }
            val = sc.getScore();
            int quantile = 0;
            if ( !val.isNaN() && !val.isInfinite() ) {
                quantile = ( int ) Math.round( 19 * ( val - minStability ) / ( maxStability - minStability ) ) + 1;
            }
            stabilityTableValues.add( new StabilityTableValues( term, er, sc, quantile, quantileAvg ) );
        }
        Collections.sort( stabilityTableValues );

    }

    /**
     * Right click -> view functionality for stability table
     */
    public void viewStabilityTableValue() {
        if ( selectedStabilityTableValue == null ) {
            return;
        }
        GeneOntologyTerm t = selectedStabilityTableValue.getTerm();
        Set<GeneOntologyTerm> sigTerms = analysis.getTermsSignificant().get( currentEdition );
        EnrichmentResult er = enrichmentResults.get( currentEdition ).get( t );
        viewEnrichmentRow = new EnrichmentTableValues( currentEdition, t, er,
                selectedStabilityTableValue.getStability(), sigTerms.contains( t ) );
        viewEnrichmentRowGeneSet = analysis.getGeneSet( viewEnrichmentRow.getEdition(), viewEnrichmentRow.getTerm() );
    }

    // Enrichment Table ---------------------------------------------------------------------------------------

    /**
     * Prepares data for enrichment table
     */
    public void loadEnrichmentTableData() {
        filteredEnrichmentTableValues = null;
        enrichmentTableValues = new ArrayList<>();
        Edition ed = enrichmentTableAllEditions.get( enrichmentTableEdition );
        if ( ed != null ) {

            Set<GeneOntologyTerm> sigTerms = analysis.getTermsSignificant().get( ed );
            Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : editionData.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                EnrichmentResult er = termEntry.getValue();
                StabilityScore sc = stabilityAnalysis.getStabilityScores( term, ed );
                enrichmentTableValues.add( new EnrichmentTableValues( ed, term, er, sc, sigTerms.contains( term ) ) );
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
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes != null ) {
            selectGenes.clear();
        }
    }

    /**
     * Add gene to to hit list
     */
    public void addGene( ActionEvent actionEvent ) {
        Gene gene = cache.getCurrentGene( currentSpeciesId, query );
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpeciesId, selectGenes );
        }
        if ( gene != null ) {
            if ( !selectGenes.contains( gene ) ) {
                selectGenes.add( gene );
                addMessage( "Gene (" + query + ") successfully added.", FacesMessage.SEVERITY_INFO );
            } else {
                addMessage( "Gene (" + query + ") already added.", FacesMessage.SEVERITY_WARN );
            }

        } else {
            addMessage( "Gene (" + query + ") could not be found.", FacesMessage.SEVERITY_WARN );
        }

    }

    /**
     * Add multiple genes based on input to the 'Add Multiple' modal field, this string is then split
     * and turned into GeneMatches to then be confirmed or altered by the user
     */
    public void searchMultipleGenes( ActionEvent actionEvent ) {
        Set<String> inputSet = new HashSet<>( Arrays.asList( bulkQuery.split( "\\s*(,|\\s)\\s*" ) ) );
        geneMatches = new ArrayList<>();
        Map<GeneMatches.MatchType, Integer> cntMap = new HashMap<>();
        for ( String geneInput : inputSet ) {
            Gene gene = cache.getCurrentGene( currentSpeciesId, geneInput );
            GeneMatches.MatchType type = null;
            if ( gene != null ) {
                type = GeneMatches.MatchType.EXACT;

            } else {
                Set<Gene> synonyms = cache.getCurrentGeneBySynonym( currentSpeciesId, geneInput );

                if ( synonyms.size() == 1 ) {
                    gene = synonyms.iterator().next();
                    type = GeneMatches.MatchType.EXACT_SYNONYM;
                } else if ( synonyms.size() > 1 ) {
                    type = GeneMatches.MatchType.MULTIPLE_EXACT_SYNONYMS;
                } else {
                    type = GeneMatches.MatchType.NO_MATCH;
                }

            }

            geneMatches.add( new GeneMatches( geneInput, gene, type ) );
            Integer cnt = cntMap.get( type );
            cntMap.put( type, ( cnt == null ? 0 : cnt ) + 1 );

        }

        if ( !geneMatches.isEmpty() ) {

            Collections.sort( geneMatches, new Comparator<GeneMatches>() {
                @Override
                public int compare( GeneMatches e1, GeneMatches e2 ) {
                    return -e1.getType().compareTo( e2.getType() );
                }
            } );

            String message = "";

            for ( Entry<MatchType, Integer> typeEntry : cntMap.entrySet() ) {
                message += typeEntry.getKey().toString() + " : " + typeEntry.getValue().toString() + "<br/>";
            }
            addMessage( message, FacesMessage.SEVERITY_INFO );
        }

    }

    /**
     * Confirm GeneMatches after alterations (or not) and add these genes to hit list
     */
    public void confirmMatches() {
        int genesAdded = 0;
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpeciesId, selectGenes );
        }

        for ( GeneMatches gm : geneMatches ) {
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
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpeciesId, selectGenes );
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
    public List<GeneMatches> complete( String query ) {
        if ( StringUtils.isEmpty( query.trim() ) || currentSpeciesId == null ) return new ArrayList<>();
        return this.cache.complete( query, currentSpeciesId, MAX_RESULTS );
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

    public Integer getCurrentSpeciesId() {
        return currentSpeciesId;
    }

    public void setCurrentSpeciesId( Integer currentSpeciesId ) {
        this.currentSpeciesId = currentSpeciesId;
    }

    public String getQuery() {
        return this.query;
    }

    public void setQuery( String query ) {
        this.query = query;
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
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpeciesId, selectGenes );
        }
        return selectGenes;
    }

    public Edition getCurrentEdition() {
        return cache.getCurrentEditions( currentSpeciesId );
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

    public List<GeneMatches> getGeneMatches() {
        return geneMatches;
    }

    public List<StabilityTableValues> getFilteredStabilityTableValues() {
        return filteredStabilityTableValues;
    }

    public void setFilteredStabilityTableValues( List<StabilityTableValues> filteredStabilityTableValues ) {
        this.filteredStabilityTableValues = filteredStabilityTableValues;
    }

    public StabilityTableValues getSelectedStabilityTableValue() {
        return selectedStabilityTableValue;
    }

    public void setSelectedStabilityTableValue( StabilityTableValues selectedStabilityTableValue ) {
        this.selectedStabilityTableValue = selectedStabilityTableValue;
    }

    public List<StabilityTableValues> getStabilityTableValues() {
        return stabilityTableValues;
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

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

    public void setAnnotationService( AnnotationService annotationService ) {
        this.annotationService = annotationService;
    }
}
