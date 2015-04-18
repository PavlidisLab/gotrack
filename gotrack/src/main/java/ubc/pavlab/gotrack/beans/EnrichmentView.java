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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.StabilityScore;

import com.google.common.collect.Iterables;

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

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    // Application settings
    // private boolean ontologyInMemory = false;

    // DAO
    private AnnotationDAO annotationDAO;

    private Integer currentSpeciesId = 7;
    private String query;
    private String bulkQuery;

    private Map<Integer, List<Gene>> speciesToSelectedGenes = new HashMap<>();
    private Gene geneToRemove;
    private Gene viewGene;

    // Stability Stuff
    private static final int TOP_N_JACCARD = 5;
    private Map<Edition, StabilityScore> stabilityScores = new HashMap<>();
    List<Entry<Edition, StabilityScore>> stabilityTableData = new ArrayList<>();
    List<Entry<Edition, StabilityScore>> filteredStabilityTableData;
    LineChartModel stabilityChart;

    // Chart stuff
    private LineChartModel enrichmentChart;
    private Map<Edition, Set<GeneOntologyTerm>> currentRunGoSets;
    private Set<GeneOntologyTerm> currentRunTerms;
    Map<Edition, Map<GeneOntologyTerm, Double>> enrichmentData;
    private boolean chartReady = false;
    private boolean bonferroniCorrection = true;
    private double pThreshold = 0.05;
    private int minAnnotatedPopulation = 5;

    // Table stuff
    private List<Entry<GeneOntologyTerm, Double>> enrichmentTableData = new ArrayList<>();
    private List<Entry<GeneOntologyTerm, Double>> filteredEnrichmentTableData;
    private Integer enrichmentTableEdition;
    private Map<Integer, Edition> enrichmentTableAllEditions = new HashMap<>();

    // Chart Filters
    private List<String> filterAspect;
    private String filterId;
    private String filterName;
    private boolean chartEmpty = false;

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

    @PostConstruct
    public void postConstruct() {
        filterAspect = new ArrayList<>( aspects );
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
        enrichmentTableEdition = cache.getCurrentEditions( currentSpeciesId ).getEdition();
        // ontologyInMemory = settingsCache.getProperty( ONTOLOGY_SETTING_PROPERTY ).equals( "true" );
        return null;
    }

    public void enrich() {
        clearOptionFilters( false );
        chartReady = false;
        allEditions = cache.getAllEditions( currentSpeciesId );
        currentRunTerms = new HashSet<>();
        Set<Gene> genes = new HashSet<>( speciesToSelectedGenes.get( currentSpeciesId ) );
        log.info( "Current species: " + currentSpeciesId );
        log.info( "Geneset: " + genes );

        if ( genes != null && !genes.isEmpty() ) {

            if ( genes.size() > MAX_GENESET_SIZE ) {
                addMessage( "Maximum geneset size is " + MAX_GENESET_SIZE + "!", FacesMessage.SEVERITY_ERROR );
                return;
            }

            log.info( "retreiving gene data..." );

            Map<Edition, Map<GeneOntologyTerm, Integer>> geneGOMap = cache.getEnrichmentData( genes );

            if ( geneGOMap == null ) {

                geneGOMap = annotationDAO.enrichmentDataPropagateCountsOnly( currentSpeciesId, genes );

                cache.addEnrichmentData( genes, geneGOMap );
                log.info( "Retrieved data from db" );
            } else {
                log.info( "Retrieved data from cache" );

            }

            Map<Edition, Integer> sampleSizes = annotationDAO.enrichmentSampleSizes( currentSpeciesId, genes );

            currentRunGoSets = new HashMap<>();
            currentRunTerms = new HashSet<>();

            // get gene-collapsed data as well as completely flattened
            for ( Entry<Edition, Map<GeneOntologyTerm, Integer>> editionEntry : geneGOMap.entrySet() ) {
                Edition ed = editionEntry.getKey();
                Set<GeneOntologyTerm> goSet = editionEntry.getValue().keySet();
                currentRunGoSets.put( ed, goSet );
                currentRunTerms.addAll( goSet );

            }

            enrichmentAnalysis( geneGOMap, sampleSizes, minAnnotatedPopulation );

        } else {
            log.info( "Empty geneset" );
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    private Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> propagate(
            Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> geneGOMap ) {
        Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> propagatedGeneGOMap = new HashMap<>();
        for ( Entry<Edition, Map<Gene, Set<GeneOntologyTerm>>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            HashMap<Gene, Set<GeneOntologyTerm>> propagatedGenes = new HashMap<>();
            propagatedGeneGOMap.put( ed, propagatedGenes );

            for ( Entry<Gene, Set<GeneOntologyTerm>> geneEntry : editionEntry.getValue().entrySet() ) {

                Set<GeneOntologyTerm> propagatedSet = cache.propagate( geneEntry.getValue(), ed.getGoEditionId() );
                propagatedGenes.put( geneEntry.getKey(), propagatedSet );
            }
        }
        return propagatedGeneGOMap;
    }

    private void enrichmentAnalysis( Map<Edition, Map<GeneOntologyTerm, Integer>> geneGOMap,
            Map<Edition, Integer> sampleSizes, int minAnnotedPopulation ) {
        enrichmentData = new HashMap<>();

        Map<Edition, Map<GeneOntologyTerm, Double>> results = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, Integer>> editionEntry : geneGOMap.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, Integer> data = editionEntry.getValue();

            Map<GeneOntologyTerm, Double> pvalues = new HashMap<>();
            results.put( ed, pvalues );

            int populationSize = cache.getGeneCount( currentSpeciesId, ed );
            int sampleSize = sampleSizes.get( ed );

            for ( Entry<GeneOntologyTerm, Integer> termEntry : data.entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                Integer sampleAnnotated = termEntry.getValue();

                Integer populationAnnotated = cache.getGoSetSizes( currentSpeciesId, ed.getEdition(), term.getGoId() );

                if ( populationAnnotated != null && populationAnnotated >= minAnnotedPopulation ) {

                    HypergeometricDistribution hyper = new HypergeometricDistribution( populationSize,
                            populationAnnotated, sampleSize );
                    Double res = hyper.upperCumulativeProbability( sampleAnnotated );
                    pvalues.put( term, res );

                }

            }

            if ( bonferroniCorrection ) {

                // Apply Bonferonni Correction
                for ( Entry<GeneOntologyTerm, Double> termEntry : pvalues.entrySet() ) {
                    Double correctedPValue = Math.min( termEntry.getValue() * pvalues.keySet().size(), 1 );
                    termEntry.setValue( correctedPValue );
                }
            }

        }

        // Filter view based on current positive terms under threshold

        Edition currentEdition = cache.getCurrentEditions( currentSpeciesId );
        Map<GeneOntologyTerm, Double> currentPValues = results.get( currentEdition );

        for ( Iterator<Entry<GeneOntologyTerm, Double>> iterator = currentPValues.entrySet().iterator(); iterator
                .hasNext(); ) {
            Entry<GeneOntologyTerm, Double> termEntry = iterator.next();

            if ( termEntry.getValue() > pThreshold ) {
                iterator.remove();
            }

        }

        if ( currentPValues.isEmpty() ) {
            // Nothing matched the criteria in the current edition
            chartReady = false;
            log.info( "Chart Empty" );
            return;
        }

        Set<GeneOntologyTerm> TermsToView = currentPValues.keySet();

        for ( Entry<Edition, Map<GeneOntologyTerm, Double>> editionEntry : results.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, Double> enrich = new HashMap<>();
            enrichmentData.put( ed, enrich );

            for ( Entry<GeneOntologyTerm, Double> termEntry : editionEntry.getValue().entrySet() ) {
                GeneOntologyTerm term = termEntry.getKey();
                Double correctedPValue = termEntry.getValue();
                if ( TermsToView.contains( term ) ) {
                    enrich.put( term, correctedPValue );
                }
            }

        }

        enrichmentChart = createEnrichmentChart( enrichmentData, "Enrichment Chart", "Date", "p-value" );
        enrichmentTableEdition = currentEdition.getEdition();
        enrichmentTableAllEditions = new LinkedHashMap<>();
        ArrayList<Edition> eds = new ArrayList<>( enrichmentData.keySet() );
        Collections.sort( eds, Collections.reverseOrder() );
        for ( Edition ed : eds ) {
            enrichmentTableAllEditions.put( ed.getEdition(), ed );
        }

        loadEnrichmentTableData();

        // Calculate stabilities and load stability tables / charts

        stabilityScores = calculateStabilityScores( enrichmentData );
        stabilityTableData = new ArrayList<>( stabilityScores.entrySet() );

        Collections.sort( stabilityTableData, new Comparator<Entry<Edition, StabilityScore>>() {
            public int compare( Entry<Edition, StabilityScore> e1, Entry<Edition, StabilityScore> e2 ) {
                return -e1.getKey().compareTo( e2.getKey() );
            }
        } );

        stabilityChart = createStabilityChart( stabilityScores, "Enrichment Stability", "Date", "Score" );

        chartReady = true;
        log.info( "Chart created" );

    }

    /**
     * Calculates scores which attempt to explore the impact that annotation stability has on the performance of gene
     * set enrichment analyses. The methods of exploration are as follows: completeTermJaccard: Looks at how consistent
     * the enriched GO terms were; previous annotations for each gene set were compared to what would be the 'current'
     * state. topGeneJaccard: Focused on how consistent the genes supporting the top 5 results were across editions.
     * topTermJaccard: Aimed to explore how semantically similar the top 5 results were across editions.
     * 
     * @param enrichmentData
     * @return
     */
    private Map<Edition, StabilityScore> calculateStabilityScores(
            Map<Edition, Map<GeneOntologyTerm, Double>> enrichmentData ) {

        Map<Edition, StabilityScore> stabilityScores = new LinkedHashMap<>();

        List<Edition> orderedEditions = new ArrayList<>( enrichmentData.keySet() );
        Collections.sort( orderedEditions );
        Edition currentEdition = Iterables.getLast( orderedEditions, null );
        LinkedHashSet<GeneOntologyTerm> currentSortedTerms = getSortedKeySetByValue( enrichmentData
                .get( currentEdition ) );

        // completeTermJaccard

        for ( Entry<Edition, Map<GeneOntologyTerm, Double>> editionEntry : enrichmentData.entrySet() ) {
            Edition ed = editionEntry.getKey();
            LinkedHashSet<GeneOntologyTerm> sortedTerms = getSortedKeySetByValue( editionEntry.getValue() );

            stabilityScores.put( ed, new StabilityScore( sortedTerms, currentSortedTerms, TOP_N_JACCARD ) );

        }

        return stabilityScores;

    }

    private LinkedHashSet<GeneOntologyTerm> getSortedKeySetByValue( Map<GeneOntologyTerm, Double> data ) {
        LinkedHashSet<GeneOntologyTerm> results = new LinkedHashSet<>();

        List<Entry<GeneOntologyTerm, Double>> entryList = new ArrayList<>( data.entrySet() );

        Collections.sort( entryList, new Comparator<Entry<GeneOntologyTerm, Double>>() {
            public int compare( Entry<GeneOntologyTerm, Double> e1, Entry<GeneOntologyTerm, Double> e2 ) {
                return e1.getValue().compareTo( e2.getValue() );
            }
        } );

        for ( Entry<GeneOntologyTerm, Double> entry : entryList ) {
            results.add( entry.getKey() );
        }

        return results;

    }

    /**
     * @param r successes in sample
     * @param k sample size
     * @param M successes in population
     * @param N failures in population
     * @return
     */
    @SuppressWarnings("unused")
    @Deprecated
    private double hypergeometric( int r, int k, int M, int N ) {
        double res = 0;
        for ( int i = r; i <= k; i++ ) {
            res += ( CombinatoricsUtils.binomialCoefficientDouble( M, i ) * CombinatoricsUtils
                    .binomialCoefficientDouble( N, k - i ) ) / CombinatoricsUtils.binomialCoefficientDouble( N + M, k );
        }

        return res;
    }

    @SuppressWarnings("unused")
    @Deprecated
    private static Map<String, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> combineDataByEdition(
            String seriesLabel, Map<String, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> data ) {
        Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> combinedSeries = new HashMap<>();
        for ( Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> s : data.values() ) {
            for ( Entry<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> editionEntry : s.entrySet() ) {
                Edition e = editionEntry.getKey();
                Map<GeneOntologyTerm, Set<EvidenceReference>> details = editionEntry.getValue();

                Map<GeneOntologyTerm, Set<EvidenceReference>> combinedDetails = combinedSeries.get( e );

                if ( combinedDetails == null ) {
                    combinedDetails = new HashMap<>();
                    combinedSeries.put( e, combinedDetails );
                }

                for ( Entry<GeneOntologyTerm, Set<EvidenceReference>> goEntry : details.entrySet() ) {
                    GeneOntologyTerm go = goEntry.getKey();
                    Set<EvidenceReference> evidence = combinedDetails.get( go );
                    if ( evidence == null ) {
                        evidence = new HashSet<>();
                        combinedDetails.put( go, evidence );
                    }
                    evidence.addAll( goEntry.getValue() );
                }

            }
        }

        Map<String, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> combinedData = new HashMap<>();
        combinedData.put( seriesLabel, combinedSeries );

        return combinedData;
    }

    private LineChartModel createEnrichmentChart( Map<Edition, Map<GeneOntologyTerm, Double>> enrichmentData,
            String title, String xLabel, String yLabel ) {

        LineChartModel dateModel = new LineChartModel();
        Double minVal = 1.0;
        Map<GeneOntologyTerm, LineChartSeries> allSeries = new HashMap<>();
        if ( enrichmentData != null ) {
            List<Edition> eds = new ArrayList<Edition>( enrichmentData.keySet() );
            Collections.sort( eds );

            for ( Edition edition : eds ) {
                String date = edition.getDate().toString();
                for ( Entry<GeneOntologyTerm, Double> dataPoint : enrichmentData.get( edition ).entrySet() ) {

                    GeneOntologyTerm term = dataPoint.getKey();
                    LineChartSeries s = allSeries.get( term );
                    if ( s == null ) {
                        s = new LineChartSeries();
                        s.setLabel( term.getGoId() );
                        s.setMarkerStyle( "filledDiamond" );
                        allSeries.put( term, s );
                    }
                    Double pValue = dataPoint.getValue();
                    if ( pValue < minVal ) minVal = pValue;
                    s.set( date, pValue );
                }
            }

            for ( LineChartSeries series : allSeries.values() ) {
                dateModel.addSeries( series );
            }
        }

        dateModel.setTitle( title );
        dateModel.setZoom( true );

        dateModel.setLegendPosition( "nw" );
        // dateModel.setAnimate( true );
        dateModel.setLegendRows( 8 );
        dateModel.setMouseoverHighlight( true );
        dateModel.setExtender( "enrichmentChartExtender" );
        // dateModel.getAxis( AxisType.Y ).setMax( 1.0 );
        // dateModel.getAxis( AxisType.Y ).setMin( Math.min( minVal, 0.1 ) );
        dateModel.getAxis( AxisType.Y ).setLabel( yLabel );

        DateAxis axis = new DateAxis( xLabel );
        axis.setTickAngle( -50 );
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;

    }

    private LineChartModel createStabilityChart( Map<Edition, StabilityScore> stabilityData, String title,
            String xLabel, String yLabel ) {

        LineChartModel dateModel = new LineChartModel();

        LineChartSeries completeTermSeries = new LineChartSeries();
        LineChartSeries topTermSeries = new LineChartSeries();

        completeTermSeries.setLabel( "completeTermJaccard" );
        completeTermSeries.setMarkerStyle( "filledDiamond" );

        topTermSeries.setLabel( "topTermJaccard" );
        topTermSeries.setMarkerStyle( "filledDiamond" );

        if ( stabilityData != null ) {
            List<Edition> eds = new ArrayList<Edition>( stabilityData.keySet() );
            Collections.sort( eds );

            for ( Edition edition : eds ) {
                String date = edition.getDate().toString();
                StabilityScore score = stabilityData.get( edition );
                completeTermSeries.set( date, score.getCompleteTermJaccard() );
                topTermSeries.set( date, score.getTopTermJaccard() );

            }

            dateModel.addSeries( completeTermSeries );
            dateModel.addSeries( topTermSeries );
        }

        dateModel.setTitle( title );
        dateModel.setZoom( true );

        dateModel.setLegendPosition( "nw" );
        // dateModel.setAnimate( true );
        dateModel.setLegendRows( 8 );
        dateModel.setMouseoverHighlight( true );
        dateModel.setExtender( "stabilityChartExtender" );
        // dateModel.getAxis( AxisType.Y ).setMax( 1.0 );
        // dateModel.getAxis( AxisType.Y ).setMin( Math.min( minVal, 0.1 ) );
        dateModel.getAxis( AxisType.Y ).setLabel( yLabel );

        DateAxis axis = new DateAxis( xLabel );
        axis.setTickAngle( -50 );
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;

    }

    public void itemSelect( ItemSelectEvent event ) {

        // dateModel.getSeries().get( event.getSeriesIndex() ).getData().get( key );
        List<Entry<Object, Number>> es = new ArrayList<Entry<Object, Number>>( enrichmentChart.getSeries()
                .get( event.getSeriesIndex() ).getData().entrySet() );

        // Keep in mind that the list of entry sets is in the order that the data was inserted, not the order it is
        // displayed!

        // log.info( "Key: " + es.get( event.getItemIndex() ).getKey() );
        // log.info( "Value: " + es.get( event.getItemIndex() ).getValue() );

        String date = ( String ) es.get( event.getItemIndex() ).getKey();
        selectedValue = es.get( event.getItemIndex() ).getValue();

        String label = enrichmentChart.getSeries().get( event.getSeriesIndex() ).getLabel();

        Collection<Edition> eds = getGoEditionsFromDate( date );

        selectedTerm = null;
        selectedEdition = null;
        if ( eds.size() == 0 ) {
            log.warn( "Found no editions for date (" + date + ")" );
        } else {
            selectedEdition = eds.iterator().next();
            if ( eds.size() > 1 ) {
                log.warn( "Found more than one edition for date (" + date + ")" );

            }
            for ( GeneOntologyTerm term : currentRunGoSets.get( selectedEdition ) ) {
                if ( term.getGoId().equals( label ) ) {
                    selectedTerm = term;
                }
            }
            if ( selectedTerm == null ) {
                log.debug( "Could not find series for  (" + label + ")" );
            }

        }

    }

    private Collection<Edition> getGoEditionsFromDate( String date ) {
        List<Edition> results = new ArrayList<Edition>();
        for ( Edition ed : allEditions ) {
            if ( ed.getDate().toString().equals( date ) ) {
                results.add( ed );
            }

        }
        return results;
    }

    public void clearOptionFilters() {
        clearOptionFilters( true );
    }

    private void clearOptionFilters( boolean reload ) {
        log.info( "Option Filters Cleared." );
        filterAspect = new ArrayList<>( aspects );
        filterId = null;
        filterName = null;
        if ( reload ) {
            reloadGraph();
        }

    }

    public void reloadGraph() {
        log.info( "reload" );
        boolean idBypass = StringUtils.isEmpty( filterId );
        boolean nameBypass = StringUtils.isEmpty( filterName );
        boolean aspectBypass = filterAspect.containsAll( aspects );
        if ( !idBypass || !nameBypass || !aspectBypass ) {
            filter( idBypass, nameBypass, aspectBypass );
        } else {
            enrichmentChart = createEnrichmentChart( enrichmentData, "Enrichment Stability", "Date", "p-value" );
            chartReady = true;
            chartEmpty = false;
        }

    }

    private void filter( boolean idBypass, boolean nameBypass, boolean aspectBypass ) {
        log.info( "Filter: ID: " + filterId + " Name: " + filterName + " Aspects: " + filterAspect );
        Map<Edition, Map<GeneOntologyTerm, Double>> filteredData = new HashMap<>();
        boolean emptyChart = true;
        for ( Entry<Edition, Map<GeneOntologyTerm, Double>> editionEntry : enrichmentData.entrySet() ) {
            Edition ed = editionEntry.getKey();
            Map<GeneOntologyTerm, Double> termsInEdition = new HashMap<>();
            filteredData.put( ed, termsInEdition );

            for ( Entry<GeneOntologyTerm, Double> termsEntry : editionEntry.getValue().entrySet() ) {
                GeneOntologyTerm term = termsEntry.getKey();
                Double pvalue = termsEntry.getValue();

                if ( ( aspectBypass || filterAspect.contains( term.getAspect() ) )
                        && ( idBypass || term.getGoId().equals( filterId ) )
                        && ( nameBypass || StringUtils.containsIgnoreCase( term.getName(), filterName ) ) ) {
                    termsInEdition.put( term, pvalue );
                    emptyChart = false;
                }
            }
        }

        if ( !emptyChart ) {
            enrichmentChart = createEnrichmentChart( filteredData, "Filtered Enrichment Stability", "Date", "p-value" );
            chartEmpty = false;
            log.info( "Chart filtered" );
        } else {
            chartEmpty = true;
            log.info( "Chart Empty" );
            enrichmentChart = null;
        }

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
        for ( GeneOntologyTerm term : currentRunTerms ) {
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

    public void loadEnrichmentTableData() {
        filteredEnrichmentTableData = null;
        enrichmentTableData = new ArrayList<>();
        Edition ed = enrichmentTableAllEditions.get( enrichmentTableEdition );
        if ( ed != null ) {
            enrichmentTableData = new ArrayList<>( enrichmentData.get( ed ).entrySet() );
            Collections.sort( enrichmentTableData, new Comparator<Entry<GeneOntologyTerm, Double>>() {
                public int compare( Entry<GeneOntologyTerm, Double> e1, Entry<GeneOntologyTerm, Double> e2 ) {
                    return e1.getValue().compareTo( e2.getValue() );
                }
            } );
        }

    }

    public boolean filterByPValue( Object value, Object filter, Locale locale ) {
        String filterText = ( filter == null ) ? null : filter.toString().trim();
        if ( filterText == null || filterText.equals( "" ) ) {
            return true;
        }

        if ( value == null ) {
            return false;
        }

        return ( ( Double ) value ).compareTo( Double.valueOf( filterText ) ) < 0;
    }

    private void addMessage( String summary, FacesMessage.Severity severity ) {
        FacesMessage message = new FacesMessage( severity, summary, null );
        FacesContext.getCurrentInstance().addMessage( null, message );
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

    public LineChartModel getEnrichmentChart() {
        return enrichmentChart;
    }

    public boolean isChartReady() {
        return chartReady;
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

    public Gene getViewGene() {
        return viewGene;
    }

    public void setViewGene( Gene viewGene ) {
        this.viewGene = viewGene;
    }

    public Edition getSelectedEdition() {
        return selectedEdition;
    }

    public void setSelectedDate( Edition selectedEdition ) {
        this.selectedEdition = selectedEdition;
    }

    public GeneOntologyTerm getSelectedTerm() {
        return selectedTerm;
    }

    public void setSelectedTerm( GeneOntologyTerm selectedTerm ) {
        this.selectedTerm = selectedTerm;
    }

    public Number getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue( Number selectedValue ) {
        this.selectedValue = selectedValue;
    }

    public List<String> getFilterAspect() {
        return filterAspect;
    }

    public void setFilterAspect( List<String> filterAspect ) {
        this.filterAspect = filterAspect;
    }

    public String getFilterId() {
        return filterId;
    }

    public void setFilterId( String filterId ) {
        this.filterId = filterId;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName( String filterName ) {
        this.filterName = filterName;
    }

    public boolean isChartEmpty() {
        return chartEmpty;
    }

    public int getMinAnnotatedPopulation() {
        return minAnnotatedPopulation;
    }

    public void setMinAnnotatedPopulation( int minAnnotatedPopulation ) {
        this.minAnnotatedPopulation = minAnnotatedPopulation;
    }

    public List<Entry<GeneOntologyTerm, Double>> getFilteredEnrichmentTableData() {
        return filteredEnrichmentTableData;
    }

    public void setFilteredEnrichmentTableData( List<Entry<GeneOntologyTerm, Double>> filteredEnrichmentTableData ) {
        this.filteredEnrichmentTableData = filteredEnrichmentTableData;
    }

    public List<Entry<GeneOntologyTerm, Double>> getEnrichmentTableData() {
        return enrichmentTableData;
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

    public Map<Edition, StabilityScore> getStabilityScores() {
        return stabilityScores;
    }

    public List<Entry<Edition, StabilityScore>> getStabilityTableData() {
        return stabilityTableData;
    }

    public List<Entry<Edition, StabilityScore>> getFilteredStabilityTableData() {
        return filteredStabilityTableData;
    }

    public void setFilteredStabilityTableData( List<Entry<Edition, StabilityScore>> filteredStabilityTableData ) {
        this.filteredStabilityTableData = filteredStabilityTableData;
    }

    public LineChartModel getStabilityChart() {
        return stabilityChart;
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
