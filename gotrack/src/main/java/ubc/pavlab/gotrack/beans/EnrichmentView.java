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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import ubc.pavlab.gotrack.analysis.StabilityAnalysis;
import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.dao.GeneOntologyDAO;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.EnrichmentResult;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.StabilityScore;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.table.EnrichmentTableValues;

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
    // private static final String ONTOLOGY_SETTING_PROPERTY = "gotrack.ontologyInMemory";

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

    // DAO
    private AnnotationDAO annotationDAO;
    private GeneOntologyDAO geneOntologyDAO;

    // View parameters
    private String query;
    private String bulkQuery;

    private Map<Integer, List<Gene>> speciesToSelectedGenes = new HashMap<>();
    private Gene geneToRemove;
    private Gene viewGene;

    // Enrichment Settings
    private Integer currentSpeciesId = 7;
    private int minAnnotatedPopulation = 5;
    private int maxAnnotatedPopulation = 0;
    private boolean bonferroniCorrection = true;
    private double pThreshold = 0.05;

    // Enrichment Data
    private EnrichmentAnalysis analysis;
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResults;
    private Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> enrichmentResultsStrict;
    private boolean enrichmentSuccess = false;

    // Stability Settings
    private static final int TOP_N_JACCARD = 5;

    // Stability Data
    private Map<Edition, StabilityScore> stabilityScores = new HashMap<>();

    // Enrichment Table Select
    private Map<Integer, Edition> enrichmentTableAllEditions = new HashMap<>();

    // Enrichment Table Data
    private List<EnrichmentTableValues> enrichmentTableValues = new ArrayList<>();
    private List<EnrichmentTableValues> filteredEnrichmentTableValues;
    private List<EnrichmentTableValues> selectedEnrichmentTableValues;
    private Integer enrichmentTableEdition;

    // Enrichment Chart
    private String enrichmentChartMeasureScale = "rank";

    // Select Data Point functionality
    private Edition selectedEdition;
    private GeneOntologyTerm selectedTerm;
    private Number selectedValue;

    private List<Edition> allEditions;

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
        // ontologyInMemory = settingsCache.getProperty( ONTOLOGY_SETTING_PROPERTY ).equals( "true" );
        return null;
    }

    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> retrieveData( Set<Gene> genes, Integer currentSpeciesId ) {
        if ( genes != null && !genes.isEmpty() ) {
            log.info( "Current species: " + currentSpeciesId );
            log.info( "Geneset Size: " + genes.size() );

            if ( genes.size() > MAX_GENESET_SIZE ) {
                addMessage( "Maximum geneset size is " + MAX_GENESET_SIZE + "!", FacesMessage.SEVERITY_ERROR );
                return null;
            }

            log.info( "retreiving gene data..." );

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

            if ( !genesToLoad.isEmpty() ) {

                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB = annotationDAO
                        .enrichmentDataPropagateNoTermInfo( currentSpeciesId, genesToLoad );

                for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : geneGOMapFromDB.entrySet() ) {
                    addGeneData( geneEntry.getKey(), geneEntry.getValue(), geneGOMap );
                    cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
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

    public void enrich() {
        allEditions = cache.getAllEditions( currentSpeciesId );
        Set<Gene> genes = new HashSet<>( speciesToSelectedGenes.get( currentSpeciesId ) );

        Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = retrieveData( genes, currentSpeciesId );

        // TODO
        if ( geneGOMap == null ) {
            enrichmentSuccess = false;
            return;
        }

        log.info( "Retrieving sample sizes from db" );
        Map<Edition, Integer> sampleSizes = annotationDAO.enrichmentSampleSizes( currentSpeciesId, genes );

        log.info( "Running enrichment analysis" );
        analysis = new EnrichmentAnalysis( geneGOMap, sampleSizes, minAnnotatedPopulation, maxAnnotatedPopulation,
                bonferroniCorrection, cache, currentSpeciesId );
        analysis.applyThreshold( pThreshold );

        enrichmentResults = analysis.getResults();
        enrichmentResultsStrict = analysis.getSignificantResults();

        log.info( "Running stability analysis" );
        StabilityAnalysis stabilityAnalysis = new StabilityAnalysis( enrichmentResultsStrict, geneGOMap, TOP_N_JACCARD );
        stabilityScores = stabilityAnalysis.getStabilityScores();

        if ( !LAZY_LOADING ) {
            log.info( "Loading Term Info for All Editions" );
            for ( Entry<Edition, Map<GeneOntologyTerm, EnrichmentResult>> editionEntry : enrichmentResults.entrySet() ) {
                geneOntologyDAO.loadTermInfo( editionEntry.getKey().getGoEditionId(), editionEntry.getValue().keySet() );
            }
            log.info( "Loading Complete." );
        }

        createTables();

        // Create Stability Chart
        ChartValues cv = new ChartValues();

        Map<String, Number> completeTermJaccard = new LinkedHashMap<>();
        Map<String, Number> topTermJaccard = new LinkedHashMap<>();
        Map<String, Number> topGeneJaccard = new LinkedHashMap<>();

        for ( Entry<Edition, StabilityScore> editionEntry : stabilityScores.entrySet() ) {
            StabilityScore score = editionEntry.getValue();
            String date = editionEntry.getKey().getDate().toString();
            completeTermJaccard.put( date, score.getCompleteTermJaccard() );
            topTermJaccard.put( date, score.getTopTermJaccard() );
            topGeneJaccard.put( date, score.getTopGeneJaccard() );
        }

        cv.addSeries( "All Terms", completeTermJaccard );
        cv.addSeries( "Top " + TOP_N_JACCARD + " Terms", topTermJaccard );
        cv.addSeries( "Genes Backing Top Terms", topGeneJaccard );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", cv );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Stability" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Jaccard Score" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );

        enrichmentSuccess = true;

    }

    private void clearAllEnrichmentRunData() {
        analysis = null;
        enrichmentResults = null;
        enrichmentResultsStrict = null;
        enrichmentTableEdition = null;
        enrichmentTableAllEditions = new HashMap<>();
        enrichmentTableValues = new ArrayList<>();
        filteredEnrichmentTableValues = null;
        stabilityScores = new HashMap<>();

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

    private void createChart( Set<GeneOntologyTerm> selectedTerms,
            Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data ) {
        // Make the series
        Map<GeneOntologyTerm, Map<String, Number>> series = new HashMap<>();
        Map<GeneOntologyTerm, Map<String, Number>> series2 = new HashMap<>();
        List<Edition> eds = new ArrayList<>( data.keySet() );
        Collections.sort( eds );
        ChartValues cv = null;
        if ( enrichmentChartMeasureScale.equals( "pvalue" ) ) {
            for ( Edition ed : eds ) {
                for ( GeneOntologyTerm term : selectedTerms ) {
                    EnrichmentResult er = data.get( ed ).get( term );
                    if ( er != null ) {
                        Map<String, Number> s = series.get( term );
                        if ( s == null ) {
                            s = new LinkedHashMap<>();
                            series.put( term, s );
                        }
                        s.put( ed.getDate().toString(), er.getPvalue() );
                    }
                }

            }
            cv = new ChartValues();

            for ( Entry<GeneOntologyTerm, Map<String, Number>> termEntry : series.entrySet() ) {
                cv.addSeries( termEntry.getKey().getGoId(), termEntry.getValue() );
            }
            RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Results" );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "P-Value" );
        } else {
            // rank
            for ( Edition ed : eds ) {
                Map<GeneOntologyTerm, EnrichmentResult> allTerms = data.get( ed );
                LinkedHashSet<GeneOntologyTerm> rankedTerms = StabilityAnalysis.getSortedKeySetByValue( allTerms );
                int i = 0;
                for ( GeneOntologyTerm term : rankedTerms ) {
                    Map<String, Number> s = series2.get( term );
                    if ( s == null ) {
                        s = new LinkedHashMap<>();
                        series2.put( term, s );
                    }
                    s.put( ed.getDate().toString(), i );
                    i++;
                }
                // Now add in null values for terms with missing edition
                for ( GeneOntologyTerm term : selectedTerms ) {
                    if ( !rankedTerms.contains( term ) ) {
                        Map<String, Number> s = series2.get( term );
                        if ( s == null ) {
                            s = new LinkedHashMap<>();
                            series2.put( term, s );
                        }
                        s.put( ed.getDate().toString(), null );
                    }
                }
            }

            cv = new ChartValues();

            for ( Entry<GeneOntologyTerm, Map<String, Number>> termEntry : series2.entrySet() ) {
                cv.addSeries( termEntry.getKey().getGoId(), termEntry.getValue() );
            }
            RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Enrichment Results by Rank" );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Rank" );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", cv );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_type", enrichmentChartMeasureScale );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_threshold", pThreshold );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );

        if ( cv.getSeries().size() == 1 ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_errors", cv );
        }
    }

    public void createChartFromTop() {
        Set<GeneOntologyTerm> selectedTerms = new HashSet<>();
        for ( StabilityScore score : stabilityScores.values() ) {
            selectedTerms.addAll( score.getTopTerms() );
        }

        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> data = filter( selectedTerms );

        createChart( selectedTerms, data );

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

        createChart( selectedTerms, data );

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

            Map<GeneOntologyTerm, EnrichmentResult> editionData = enrichmentResults.get( ed );
            for ( Entry<GeneOntologyTerm, EnrichmentResult> termEntry : editionData.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                EnrichmentResult er = termEntry.getValue();
                enrichmentTableValues.add( new EnrichmentTableValues( ed, term, er ) );
            }
            Collections.sort( enrichmentTableValues );

        }
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

    private void addMessage( String summary, FacesMessage.Severity severity ) {
        FacesMessage message = new FacesMessage( severity, summary, null );
        FacesContext.getCurrentInstance().addMessage( null, message );
    }

    public boolean isEnrichmentSuccess() {
        return enrichmentSuccess;
    }

    public boolean isBonferroniCorrection() {
        return bonferroniCorrection;
    }

    public void setBonferroniCorrection( boolean bonferroniCorrection ) {
        this.bonferroniCorrection = bonferroniCorrection;
    }

    public double getpThreshold() {
        return pThreshold;
    }

    public void setpThreshold( double pThreshold ) {
        this.pThreshold = pThreshold;
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
