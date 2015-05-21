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

import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;

import ubc.pavlab.gotrack.analysis.EnrichmentAnalysis;
import ubc.pavlab.gotrack.analysis.EnrichmentResult;
import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityCompareMethod;
import ubc.pavlab.gotrack.analysis.StabilityAnalysis;
import ubc.pavlab.gotrack.analysis.StabilityScore;
import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.dao.GeneOntologyDAO;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.table.EnrichmentTableValues;

import com.google.gson.Gson;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class EnrichmentView implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 166880636358923147L;

    private static final Logger log = Logger.getLogger( EnrichmentView.class );
    private static final Integer MAX_RESULTS = 10;
    private static final int MAX_GENESET_SIZE = 200;
    // True if Terms should be lazy-loaded as required, false if all terms should be loaded on enrichment run
    private static final boolean LAZY_LOADING = false;

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    // Application settings
    private boolean ontologyInMemory = false;

    // DAO
    private AnnotationDAO annotationDAO;
    private GeneOntologyDAO geneOntologyDAO;

    // View parameters
    private String query;
    // Liver Cancer
    // ACAA1 ACADS ADH1B ADH4 ADRA1A APOF C6 C8A C8B C9orf103 CD4 CETN2 CIDEB COLEC10 CYP2C18 CYP2C19 CYP2C8 CYP2E1
    // CYP4A11 DMGDH DNASE1L3 ETS2 FAM20C FCN1 FETUB GBA3 HPX IGFALS ITIH4 LCAT LDHD LOC401022 LPA NDRG2 PEMT PLG PPAP2B
    // RCAN1 RSPO3 RXRB SLC22A1 SLC22A3 SLC28A1 SLCO1B3 TMED8 TMEM27 TTC36 TTR UROC1

    // Adrenaline and noradrenaline biosynthesis
    // MAOA DDC VTI1B MAOB STX12 COMT SNAP25 SNAP23 SLC6A20 SLC6A2 TH SLC6A3 SNAP29 PNMT STX7 SLC18A1 VAMP1 SLC6A18
    // SLC6A19 SLC18A2 VAMP2 SLC6A15 SLC6A16 SLC6A17 DBH
    private String bulkQuery = "ACAA1 ACADS ADH1B ADH4 ADRA1A APOF C6 C8A C8B C9orf103 CD4 CETN2 CIDEB COLEC10 CYP2C18 CYP2C19 CYP2C8 CYP2E1 CYP4A11 DMGDH DNASE1L3 ETS2 FAM20C FCN1 FETUB GBA3 HPX IGFALS ITIH4 LCAT LDHD LOC401022 LPA NDRG2 PEMT PLG PPAP2B RCAN1 RSPO3 RXRB SLC22A1 SLC22A3 SLC28A1 SLCO1B3 TMED8 TMEM27 TTC36 TTR UROC1";

    private Map<Integer, List<Gene>> speciesToSelectedGenes = new HashMap<>();
    private Gene geneToRemove;
    private Gene viewGene;

    // Enrichment Settings
    private Integer currentSpeciesId = 7;
    private int minAnnotatedPopulation = 5;
    private int maxAnnotatedPopulation = 200;
    private MultipleTestCorrection multipleTestCorrection = MultipleTestCorrection.BH;
    private double pThreshold = 0.05;
    private double fdr = 0.05;

    // Enrichment Feedback
    private int enrichmentProgress;
    private List<String> enrichmentStatus = new ArrayList<>();

    // Enrichment Data
    private EnrichmentAnalysis analysis;
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults;
    private boolean enrichmentSuccess = false;

    // Stability Settings
    private static final int TOP_N_JACCARD = 5;
    private SimilarityCompareMethod similarityCompareMethod = SimilarityCompareMethod.PROXIMAL;

    // Stability Data
    private Map<Edition, StabilityScore> stabilityScores = new HashMap<>();

    // Enrichment Table Select
    private Map<Integer, Edition> enrichmentTableAllEditions = new HashMap<>();

    // Enrichment Table Data
    private List<EnrichmentTableValues> enrichmentTableValues = new ArrayList<>();
    private List<EnrichmentTableValues> filteredEnrichmentTableValues;
    private List<EnrichmentTableValues> selectedEnrichmentTableValues;
    private Integer enrichmentTableEdition;
    private EnrichmentTableValues viewEnrichmentRow;
    private Set<Gene> viewEnrichmentRowGeneSet;

    // Enrichment Chart
    private String enrichmentChartMeasureScale = "rank";
    private int enrichmentChartTopN = 5;

    // Select Data Point functionality
    private Edition selectedEdition;
    private GeneOntologyTerm selectedTerm;
    private Number selectedValue;
    private String selectedValueName;

    // Select Data Point functionality Enrichment Chart
    private EnrichmentResult selectedEnrichmentResult;

    // Static final
    private static final List<String> aspects = Arrays.asList( "BP", "MF", "CC" );

    public EnrichmentView() {
        log.info( "EnrichmentView created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

    }

    public String init() {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return null; // Skip ajax requests.
        }
        log.info( "EnrichmentView init" );
        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            FacesContext.getCurrentInstance()
                    .addMessage(
                            "betaMessage",
                            new FacesMessage( FacesMessage.SEVERITY_WARN,
                                    "This is the DEVELOPMENT version of GOTrack!", null ) );
        }

        annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();
        geneOntologyDAO = daoFactoryBean.getGotrack().getGeneOntologyDAO();
        enrichmentTableEdition = cache.getCurrentEditions( currentSpeciesId ).getEdition();
        ontologyInMemory = settingsCache.getOntologyInMemory();
        // ontologyInMemory = settingsCache.getProperty( ONTOLOGY_SETTING_PROPERTY ).equals( "true" );
        return null;
    }

    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> retrieveData( Set<Gene> genes, Integer currentSpeciesId ) {
        if ( genes != null && !genes.isEmpty() ) {
            log.info( "Current species: " + currentSpeciesId );
            log.info( "Geneset Size: " + genes.size() );
            String status;
            if ( genes.size() > MAX_GENESET_SIZE ) {
                status = "Gene Hit List too large; maximum geneset size is " + MAX_GENESET_SIZE + "!";
                enrichmentStatus.add( status );
                enrichmentProgress = 50;
                addMessage( "Maximum geneset size is " + MAX_GENESET_SIZE + "!", FacesMessage.SEVERITY_ERROR );
                return null;
            }

            log.info( "retrieving gene data..." );
            status = "Retrieving Gene Information from cache...";
            enrichmentStatus.add( status );
            enrichmentProgress = 5;

            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = new HashMap<>();
            Set<Gene> genesToLoad = new HashSet<>();
            for ( Gene gene : genes ) {
                Map<Edition, Set<GeneOntologyTerm>> cachedGeneData = cache.getEnrichmentData( gene );
                if ( cachedGeneData != null ) {
                    addGeneData( gene, cachedGeneData, geneGOMap );
                } else {
                    genesToLoad.add( gene );
                }
            }
            enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );

            status = "Retrieving Gene Information from database...";
            enrichmentStatus.add( status );
            enrichmentProgress = 10;

            if ( !genesToLoad.isEmpty() ) {

                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB;

                if ( ontologyInMemory ) {
                    enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );
                    status = "Propagating GO Terms...";
                    enrichmentStatus.add( status );
                    enrichmentProgress = 40;
                    geneGOMapFromDB = annotationDAO.enrichmentDataNoPropagateNoTermInfo( currentSpeciesId, genesToLoad );
                    for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : propagate( geneGOMapFromDB )
                            .entrySet() ) {
                        addGeneData( geneEntry.getKey(), geneEntry.getValue(), geneGOMap );
                        cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
                    }
                    enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );
                } else {
                    geneGOMapFromDB = annotationDAO.enrichmentDataPropagateNoTermInfo( currentSpeciesId, genesToLoad );
                    enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );
                    for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : geneGOMapFromDB.entrySet() ) {
                        addGeneData( geneEntry.getKey(), geneEntry.getValue(), geneGOMap );
                        cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
                    }
                }

                log.info( "Retrieved (" + genesToLoad.size() + ") genes from db and ("
                        + ( genes.size() - genesToLoad.size() ) + ") from cache" );
            } else {
                log.info( "Retrieved all (" + genes.size() + ") genes from cache" );

            }
            return geneGOMap;

        } else {
            log.info( "Empty geneset" );
            return null;
        }
    }

    private void addGeneData( Gene g, Map<Edition, Set<GeneOntologyTerm>> cachedGeneData,
            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> data ) {

        for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : cachedGeneData.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, Set<Gene>> m1 = data.get( ed );

            if ( m1 == null ) {
                // Previously had no data for this edition
                m1 = new HashMap<>();
                data.put( ed, m1 );
            }

            for ( GeneOntologyTerm term : editionEntry.getValue() ) {
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
                Set<GeneOntologyTerm> propagatedTerms = cache.propagate( editionEntry.getValue(), ed.getGoEditionId() );

                if ( propagatedTerms == null ) {
                    // No ontology exists for this edition
                } else {
                    propagatedSeries.put( ed, propagatedTerms );
                }

            }

        }
        return propagatedData;
    }

    public void enrich() {
        String status = "";
        enrichmentStatus = new ArrayList<>();
        status = "Starting Enrichment Analysis";
        enrichmentStatus.add( status );
        enrichmentProgress = 0;
        Set<Gene> genes = new HashSet<>( speciesToSelectedGenes.get( currentSpeciesId ) );

        Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = retrieveData( genes, currentSpeciesId );

        // TODO
        if ( geneGOMap == null ) {
            enrichmentStatus.add( "Failed" );
            enrichmentProgress = 100;
            enrichmentSuccess = false;
            return;
        }

        log.info( "Retrieving sample sizes" );
        status = "Retrieving Sample Sizes...";
        enrichmentStatus.add( status );
        enrichmentProgress = 50;
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
        testMap = null;

        enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );

        log.info( "Running enrichment analysis" );
        status = "Running Overrepresentation Analyses on all editions...";
        enrichmentStatus.add( status );
        enrichmentProgress = 60;
        double thresh = multipleTestCorrection.equals( MultipleTestCorrection.BONFERRONI ) ? pThreshold : fdr;
        analysis = new EnrichmentAnalysis( geneGOMap, sampleSizes, minAnnotatedPopulation, maxAnnotatedPopulation,
                multipleTestCorrection, thresh, cache, currentSpeciesId );

        enrichmentResults = analysis.getResults();
        enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );

        log.info( "Running stability analysis" );

        status = "Running Stability Analyses on all editions...";
        enrichmentStatus.add( status );
        enrichmentProgress = 80;
        StabilityAnalysis stabilityAnalysis = new StabilityAnalysis( analysis, TOP_N_JACCARD, similarityCompareMethod,
                ontologyInMemory ? cache : null );
        stabilityScores = stabilityAnalysis.getStabilityScores();
        enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );

        if ( !LAZY_LOADING ) {
            log.info( "Loading Term Info for All Editions" );
            for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : enrichmentResults.entrySet() ) {
                geneOntologyDAO.loadTermInfo( editionEntry.getKey().getGoEditionId(), editionEntry.getValue().keySet() );
            }
            log.info( "Loading Complete." );
        }

        status = "Creating tables and charts...";
        enrichmentStatus.add( status );
        enrichmentProgress = 90;

        createTables();

        // Create Similarity Chart
        ChartValues cv = new ChartValues();

        Series completeTermJaccard = new Series( "All Terms" );
        Series topTermJaccard = new Series( "Top " + TOP_N_JACCARD + " Terms" );
        Series topGeneJaccard = new Series( "Genes Backing Top Terms" );
        Series topParentsJaccard = new Series( "Parents of Top Terms" );

        for ( Entry<Edition, StabilityScore> editionEntry : stabilityScores.entrySet() ) {
            StabilityScore score = editionEntry.getValue();
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
        RequestContext.getCurrentInstance()
                .addCallbackParam(
                        "hc_title",
                        "Enrichment Similarity to "
                                + ( similarityCompareMethod.equals( SimilarityCompareMethod.PROXIMAL ) ? "Previous"
                                        : "Current" ) + " Edition" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Jaccard Index" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );

        enrichmentStatus.set( enrichmentStatus.size() - 1, status + " COMPLETE" );

        enrichmentSuccess = true;
        enrichmentStatus.add( "Finished" );
        enrichmentProgress = 100;

    }

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

    // Enrichment Charts ---------------------------------------------------------------------------------------

    private void createChart( Set<GeneOntologyTerm> selectedTerms,
            Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data, Integer topN ) {
        // Make the series
        List<Edition> eds = new ArrayList<>( data.keySet() );
        Collections.sort( eds );
        ChartValues cv = null;
        double maxRank = -1;
        Map<Long, Integer> dateToEdition = new HashMap<>();
        if ( enrichmentChartMeasureScale.equals( "pvalue" ) ) {
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
            RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Results" );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "P-Value" );
        } else {
            // rank
            Map<GeneOntologyTerm, Series> series = new HashMap<>();
            Map<Long, Double> dateToMaxSigRank = new HashMap<>();
            maxRank = -1;
            boolean outsideTopNCheck = false;
            boolean insignificantCheck = false;
            for ( Edition ed : eds ) {
                int maxSignificantRank = analysis.getTermsSignificant().get( ed ).size() - 1;

                dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
                Map<GeneOntologyTerm, EnrichmentResult> editionData = data.get( ed );
                ArrayList<Entry<GeneOntologyTerm, EnrichmentResult>> sortedData = new ArrayList<>(
                        editionData.entrySet() );

                Collections.sort( sortedData, new Comparator<Entry<GeneOntologyTerm, EnrichmentResult>>() {
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

                double breakPoint = relativeRanks.size();
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
        RequestContext.getCurrentInstance().addCallbackParam( "hc_type", enrichmentChartMeasureScale );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_threshold", pThreshold );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_dateToEdition", new Gson().toJson( dateToEdition ) );

        if ( cv.getSeries().size() == 1 ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_errors", cv );
        }
    }

    public void createChartFromTop() {
        Set<GeneOntologyTerm> selectedTerms = analysis.getTopNTerms( enrichmentChartTopN );

        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data = filter( selectedTerms );

        createChart( selectedTerms, data, enrichmentChartTopN );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_topN", enrichmentChartTopN );

    }

    public void createChartFromSelected() {

        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data = null;
        Set<GeneOntologyTerm> selectedTerms = null;
        if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
            // If nothing selected graph everything
            data = enrichmentResults;
            selectedTerms = analysis.getTermsSignificantInAnyEdition();
        } else {
            selectedTerms = new HashSet<>();

            for ( EnrichmentTableValues entry : selectedEnrichmentTableValues ) {
                selectedTerms.add( entry.getTerm() );
            }

            data = filter( selectedTerms );
            log.info( "Chart filtered" );
        }

        createChart( selectedTerms, data, null );

    }

    @SuppressWarnings("unused")
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> filter( Set<GeneOntologyTerm> terms ) {
        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> filteredData = new HashMap<>();
        for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : enrichmentResults.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, EnrichmentResult> termsInEdition = new HashMap<>();
            filteredData.put( ed, termsInEdition );

            if ( LAZY_LOADING && geneOntologyDAO.loadTermInfo( ed.getGoEditionId(), editionEntry.getValue().keySet() ) ) {
                log.info( "Lazy loading Term Info for Edition: (" + ed + ")." );
            }

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

    public void fetchTermInformation() {
        String termId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "termId" );
        Integer edition = Integer.valueOf( FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get( "edition" ) );

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
                    if ( enrichmentChartMeasureScale.equals( "pvalue" ) ) {
                        // selectedValue = er.getPvalue();
                        selectedValueName = null;
                    } else {
                        // selectedValue = er.getRank();
                        double relativeRank = Double.valueOf( FacesContext.getCurrentInstance().getExternalContext()
                                .getRequestParameterMap().get( "value" ) );
                        selectedValueName = "Relative Rank";
                        selectedValue = relativeRank;
                    }
                    // RequestContext.getCurrentInstance().addCallbackParam( "term", new Gson().toJson( term ) );
                    // RequestContext.getCurrentInstance().addCallbackParam( "result", new Gson().toJson( er ) );
                    return;
                }
            }

            selectedTerm = null;

        }

    }

    // Enrichment Table ---------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    public void loadEnrichmentTableData() {
        filteredEnrichmentTableValues = null;
        enrichmentTableValues = new ArrayList<>();
        Edition ed = enrichmentTableAllEditions.get( enrichmentTableEdition );
        if ( ed != null ) {

            // If this edition of terms has not been lazy loaded then lazy load their term info.

            if ( LAZY_LOADING
                    && geneOntologyDAO.loadTermInfo( ed.getGoEditionId(), enrichmentResults.get( ed ).keySet() ) ) {
                log.info( "Lazy loading Term Info for Edition: (" + ed + ")." );
            }

            Set<GeneOntologyTerm> sigTerms = analysis.getTermsSignificant().get( ed );
            Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : editionData.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                EnrichmentResult er = termEntry.getValue();
                enrichmentTableValues.add( new EnrichmentTableValues( ed, term, er, sigTerms.contains( term ) ) );
            }
            Collections.sort( enrichmentTableValues );

        }
    }

    public void viewEnrichmentTableValue() {
        if ( selectedEnrichmentTableValues == null || selectedEnrichmentTableValues.isEmpty() ) {
            return;
        }
        viewEnrichmentRow = selectedEnrichmentTableValues.iterator().next();
        viewEnrichmentRowGeneSet = analysis.getGeneSet( viewEnrichmentRow.getEdition(), viewEnrichmentRow.getTerm() );
    }

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

    public void removeAllGenes( ActionEvent actionEvent ) {
        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes != null ) {
            selectGenes.clear();
        }
    }

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

    public void addBulkGenes( ActionEvent actionEvent ) {

        String[] bulkGeneInput = bulkQuery.split( "\\s+" );
        Set<String> geneInputsNotFound = new HashSet<>();
        int genesAdded = 0;

        List<Gene> selectGenes = speciesToSelectedGenes.get( currentSpeciesId );
        if ( selectGenes == null ) {
            selectGenes = new ArrayList<>();
            speciesToSelectedGenes.put( currentSpeciesId, selectGenes );
        }

        for ( int i = 0; i < bulkGeneInput.length; i++ ) {
            String geneInput = bulkGeneInput[i];

            Gene g = cache.getCurrentGene( currentSpeciesId, geneInput );

            if ( g != null ) {

                if ( !selectGenes.contains( g ) ) {
                    genesAdded++;
                    selectGenes.add( g );

                }

            } else {
                geneInputsNotFound.add( geneInput );
            }

        }

        if ( genesAdded > 0 ) {
            addMessage( "Successfully added " + genesAdded + " gene(s)", FacesMessage.SEVERITY_INFO );
        }
        if ( geneInputsNotFound.size() > 0 ) {

            String geneString = StringUtils.join( geneInputsNotFound, ", " );

            addMessage( "Could not find matching gene(s) for (" + geneString + ")", FacesMessage.SEVERITY_WARN );
        }

    }

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

    private void addMessage( String summary, FacesMessage.Severity severity ) {
        FacesMessage message = new FacesMessage( severity, summary, null );
        FacesContext.getCurrentInstance().addMessage( null, message );
    }

    public List<String> complete( String query ) {
        if ( StringUtils.isEmpty( query.trim() ) || currentSpeciesId == null ) {
            return new ArrayList<String>();
        }
        return this.cache.complete( query, currentSpeciesId, MAX_RESULTS );
    }

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
        Collections.sort( p, new LevenshteinComparator( query ) );

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

    public String getEnrichmentChartMeasureScale() {
        return enrichmentChartMeasureScale;
    }

    public void setEnrichmentChartMeasureScale( String enrichmentChartMeasureScale ) {
        this.enrichmentChartMeasureScale = enrichmentChartMeasureScale;
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

    public List<String> getEnrichmentStatus() {
        return enrichmentStatus;
    }

    public int getEnrichmentProgress() {
        return enrichmentProgress;
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

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }
}
