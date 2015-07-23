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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.StatsService;
import ubc.pavlab.gotrack.exception.GeneNotFoundException;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationType;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.StatsEntry;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Jaccard;

/**
 * Backing bean for the gene tracking functionality.
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class GeneView {

    private static final Logger log = Logger.getLogger( GeneView.class );

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{statsService}")
    private StatsService statsService;

    @ManagedProperty("#{annotationService}")
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
    private Collection<Annotation> viewAnnotations = new ArrayList<>();
    private Collection<Annotation> filteredViewAnnotations;
    private GeneOntologyTerm viewTerm;

    // Click event lists
    private Collection<GeneOntologyTerm> clickTerms = new HashSet<>();
    private List<GeneOntologyTerm> selectedClickTerms;
    private Collection<GeneOntologyTerm> filteredClickTerms;
    private Edition clickEdition;

    public GeneView() {
        log.info( "GeneView created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
    }

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
            throw new GeneNotFoundException();
        } else {
            // Count gene hit
            statsService.countHit( gene );

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
            directRawData = cachedData.get( AnnotationType.DIRECT ).rowMap();
            inferredRawData = cachedData.get( AnnotationType.INFERRED ).rowMap();
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

        rawData.put( AnnotationType.DIRECT, rawdataBuilder.build() );

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
        rawData.put( AnnotationType.INFERRED, rawdataBuilder.build() );

        // If no data existed in cache and we did not filter it then cache the data
        if ( cachedData == null && bypassFilter ) {
            cache.addGeneData( gene, rawData );
        }

        return rawData;

    }

    public void fetchData() {
        log.debug( "fetchData" );

        rawData = retrieveData();

        // A map that will be needed in the front end for drilling down
        Map<Long, Integer> dateToEdition = new HashMap<>();

        for ( Edition ed : rawData.get( AnnotationType.DIRECT ).rowKeySet() ) {
            dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
        }

        RequestContext.getCurrentInstance().addCallbackParam( "dateToEdition", new Gson().toJson( dateToEdition ) );

        // Now create a list of terms that will be displayed in a front-end table

        allTerms = rawData.get( AnnotationType.DIRECT ).columnKeySet();

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

    private void fetchAnnotationChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchAnnotationChart" );

        // Collect data from cache about species aggregates
        Map<Edition, StatsEntry> aggregates = cache.getAggregates( species.getId() );

        ChartValues chart = new ChartValues();

        // Create series for species aggregates
        Series aggregateSeries = new Series( "Species Direct Avg" );
        for ( Entry<Edition, StatsEntry> entry : aggregates.entrySet() ) {
            aggregateSeries.addDataPoint( entry.getKey().getDate(), entry.getValue().getAvgDirectByGene() );
        }
        chart.addSeries( aggregateSeries );

        //Create series for direct annotations count
        Series directCountSeries = new Series( "Direct Annotation Count" );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.DIRECT )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            int count = entry.getValue().size();
            directCountSeries.addDataPoint( ed.getDate(), count );
        }
        chart.addSeries( directCountSeries );

        // Create series for inferred annotations count
        Series inferredCountSeries = new Series( "Inferred Annotation Count" );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.INFERRED )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            int count = entry.getValue().size();
            inferredCountSeries.addDataPoint( ed.getDate(), count );
        }
        chart.addSeries( inferredCountSeries );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Annotations vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Annotations Count" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );
    }

    public void fetchAnnotationChart() {
        fetchAnnotationChart( rawData );

    }

    private void fetchJaccardChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchJaccardChart" );

        ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>> directData = rawData.get( AnnotationType.DIRECT );
        ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>> inferredData = rawData
                .get( AnnotationType.INFERRED );

        ChartValues chart = new ChartValues();

        // Create jaccard between edition and current edition
        Edition currentEdition = Collections.max( directData.rowKeySet() );

        // For direct annotations
        Series directSeries = new Series( "Direct Similarity" );

        Set<GeneOntologyTerm> currentGOSet = directData.row( currentEdition ).keySet();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : directData.rowMap().entrySet() ) {
            Double jaccard = Jaccard.similarity( entry.getValue().keySet(), currentGOSet );
            directSeries.addDataPoint( entry.getKey().getDate(), jaccard );
        }

        chart.addSeries( directSeries );

        // For Inferred annotations
        Series inferredSeries = new Series( "Inferred Similarity" );

        currentGOSet = inferredData.row( currentEdition ).keySet();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : inferredData.rowMap().entrySet() ) {
            Double jaccard = Jaccard.similarity( entry.getValue().keySet(), currentGOSet );
            inferredSeries.addDataPoint( entry.getKey().getDate(), jaccard );
        }

        chart.addSeries( inferredSeries );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Similarity vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Jaccard Similarity Index" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );

    }

    public void fetchJaccardChart() {
        fetchJaccardChart( rawData );

    }

    private void fetchMultifunctionalityChart(
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData ) {
        log.debug( "fetchMultifunctionalityChart" );
        ChartValues chart = new ChartValues();

        // Calculate multifunctionality of the gene in each edition
        Series multiSeries = new Series( "Multifunctionality" );
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.INFERRED )
                .rowMap().entrySet() ) {
            Edition ed = entry.getKey();
            Integer total = cache.getGeneCount( species.getId(), ed );
            if ( total != null ) {
                Double multi = 0.0;
                for ( GeneOntologyTerm t : entry.getValue().keySet() ) {
                    Integer inGroup = cache.getInferredAnnotationCount( species.getId(), ed, t );
                    if ( inGroup != null ) {
                        multi += 1.0 / ( inGroup * ( total - inGroup ) );
                    }
                }
                multiSeries.addDataPoint( ed.getDate(), multi );
            }
        }
        chart.addSeries( multiSeries );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Multifunctionality vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Multifunctionality" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );
    }

    public void fetchMultifunctionalityChart() {
        fetchMultifunctionalityChart( rawData );
    }

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
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.DIRECT )
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
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : rawData.get( AnnotationType.INFERRED )
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
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Loss/Gain vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "Change" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );

    }

    public void fetchLossGainChart() {
        fetchLossGainChart( rawData );

    }

    public void filterCharts() {
        if ( selectedTerms == null || selectedTerms.size() == 0 ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_filtered", false );
            return;
        }

        HashSet<GeneOntologyTerm> filterTerms = new HashSet<>( selectedTerms );

        rawData = retrieveData( filterTerms );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_filtered", true );

    }

    public void resetCharts() {
        rawData = retrieveData();
        RequestContext.getCurrentInstance().addCallbackParam( "hc_filtered", true );
    }

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
                .get( AnnotationType.INFERRED );

        // Create an ordering for the categories
        int i = 0;
        Map<String, Integer> categoryPositions = new HashMap<>();
        for ( String cat : cache.getEvidenceCategories() ) {
            categoryPositions.put( cat, i++ );
        }

        ChartValues chart = new ChartValues();

        Collection<Edition> allEditions = cache.getAllEditions( species.getId() );

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
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", "Annotation Category vs Time" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_ylabel", "" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_xlabel", "Date" );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_data", chart );

        RequestContext.getCurrentInstance().addCallbackParam( "hc_category_positions",
                new Gson().toJson( categoryPositions ) );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_term_names", new Gson().toJson( termNames ) );

    }

    // View Term's Annotations
    public void fetchAnnotations() {
        log.debug( "fetchAnnotations" );
        //        if ( selectedClickTerms == null || selectedClickTerms.isEmpty() ) {
        //            return;
        //        }
        //viewTerm = selectedClickTerms.iterator().next();
        log.debug( viewTerm );

        Set<Annotation> data = rawData.get( AnnotationType.DIRECT ).get( clickEdition, viewTerm );

        viewAnnotations = ( data == null ) ? Sets.<Annotation> newHashSet() : data;

        filteredViewAnnotations = null;
    }

    // Click event functionality

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
            clickTerms = rawData.get( AnnotationType.DIRECT ).row( clickEdition ).keySet();
        } catch ( NullPointerException e ) {
            RequestContext.getCurrentInstance().addCallbackParam( "hc_success", false );
            return;
        }

        selectedClickTerms = null;
        filteredClickTerms = null;

        RequestContext.getCurrentInstance().addCallbackParam( "hc_success", true );

    }

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

        Set<Annotation> data = rawData.get( AnnotationType.INFERRED ).get( clickEdition, viewTerm );

        viewAnnotations = ( data == null ) ? Sets.<Annotation> newHashSet() : data;

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

    public Collection<Annotation> getViewAnnotations() {
        return viewAnnotations;
    }

    public Collection<Annotation> getFilteredViewAnnotations() {
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

    public void setCache( Cache cache ) {
        this.cache = cache;
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

    public void setStatsService( StatsService statsService ) {
        this.statsService = statsService;
    }

    public void setAnnotationService( AnnotationService annotationService ) {
        this.annotationService = annotationService;
    }

}
