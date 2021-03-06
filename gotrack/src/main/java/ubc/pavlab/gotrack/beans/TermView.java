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
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.exception.TermNotFoundException;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.chart.SeriesExtra;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.go.Relation;
import ubc.pavlab.gotrack.model.go.RelationshipType;
import ubc.pavlab.gotrack.model.visualization.Graph;
import ubc.pavlab.gotrack.utilities.Tuples;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Backing bean for the term tracking functionality.
 *
 * @author mjacobson
 */
@Log4j
@Named
@ViewScoped
public class TermView implements Serializable {

    private static final long serialVersionUID = 3768269202724289260L;

    private static final int MAX_INFERRED_GENES_DISPLAY_COUNT = 500;

    @Inject
    private Cache cache;

    @Inject
    private AnnotationService annotationService;

    @Inject
    private SessionManager session;

    @Getter
    @Setter
    private String query;

    @Getter
    @Setter
    private boolean allSpecies;

    @Getter
    private GeneOntologyTerm currentTerm;

    @Getter
    private List<Tuples.Tuple2<Evidence, Integer>> evidenceChartClickData;
    @Getter
    private Edition evidenceChartClickEdition;

    public TermView() {
        log.info( "TermView created" );
        // System.gc();
        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
                + " MB" );
    }

    @PostConstruct
    public void postConstruct() {
        log.info( "TermView postConstruct" );
    }

    /**
     * pre-render view
     * <p>
     * This is kept lightweight so that the page loads quickly and lazy loads the data using remote commands
     */
    public String init() throws TermNotFoundException {
        log.info( "TermView init" );

        if ( query == null ) {
            return null;
        }

        if ( FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development ) {
            FacesContext.getCurrentInstance().addMessage( "betaMessage", new FacesMessage( FacesMessage.SEVERITY_WARN,
                    "This is the DEVELOPMENT version of GOTrack!", "" ) );

        }

        currentTerm = cache.getCurrentTerm( query );

        if ( currentTerm == null ) {
            // not found
            throw new TermNotFoundException();

        }

        return null;
    }

    public boolean displayInferred() {
        Integer cnt = cache.getInferredAnnotationCount( cache.getCurrentEditions( session.getSpecies() ), currentTerm );
        return cnt == null || cnt  <= MAX_INFERRED_GENES_DISPLAY_COUNT;
    }

    public Map<Gene, Boolean> fetchCurrentGenesMap() {
        Edition ed = cache.getCurrentEditions( session.getSpecies() );
        if ( displayInferred() ) {
            return annotationService.fetchInferredGenes( currentTerm, ed ).entrySet().stream()
                    .sorted( Map.Entry.<Gene, Boolean>comparingByValue().reversed().thenComparing( Map.Entry.comparingByKey() ) )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue,
                            ( e1, e2 ) -> e1, LinkedHashMap::new ) );
        } else {
            return annotationService.fetchDirectGenes( currentTerm, ed ).collect(
                    Collectors.toMap( Function.identity(),
                            g -> true,
                            ( u, v ) -> v,
                            LinkedHashMap::new
                    ) );
        }

    }

    public List<Relation<GeneOntologyTerm>> getChildTerms( GeneOntologyTerm term ) {
        return term.getChildren().stream()
                .sorted( Comparator.comparing( (Function<Relation<GeneOntologyTerm>, RelationshipType>) Relation::getType )
                        .thenComparing( Relation::getRelation ) )
                .collect( Collectors.toList() );
    }

    private Map<String, Object> createHCCallbackParamMap( ChartValues chart ) {
        Map<String, Object> hcGsonMap = Maps.newHashMap();
        hcGsonMap.put( "success", true );
        hcGsonMap.put( "chart", chart );
        return hcGsonMap;
    }

    /**
     * Entry point to fetch data for the overview gantt chart.
     */
    public void fetchOverviewChart() {
        // Going to need this
        Map<Long, Integer> dateToGOEditionId = new HashMap<>();
        Map<Long, String[]> dateToNameChange = new HashMap<>();

        // Create the 'did this term exist' chart
        ChartValues existChart = new ChartValues( "History", "", "Date" );
        existChart.setSubtitle( currentTerm.getGoId() + " - " + currentTerm.getName() );
        existChart.setMin( 0 );
        existChart.setMax( 2 );

        Series existSeries = new Series( "Existence" );
        Series structureSeries = new Series( "Structure Change" );
        Series nameChange = new Series( "Name Change" );
        Graph prevEles = null;
        String prevName = null;

        Collection<GOEdition> sortedGOEditions = cache.getAllGOEditions().stream().sorted().collect( Collectors.toList() );

        for ( GOEdition goEdition : sortedGOEditions ) {
            GeneOntologyTerm t = cache.getTerm( goEdition, currentTerm.getGoId() );
            long time = goEdition.getDate().getTime();

            existSeries.addDataPoint( time, t != null ? 1 : 0 );
            dateToGOEditionId.put( time, goEdition.getId() );
            if ( t != null ) {

                Graph eles = Graph.fromGO( t, false );

                if ( prevEles != null ) {
                    Graph diff = Graph.fromGraphDiff( prevEles, eles );
                    structureSeries.addDataPoint( time, diff != null ? 1 : 0 );
                } else {
                    structureSeries.addDataPoint( time, -1 );
                }

                if ( prevName != null ) {
                    int changed = t.getName().equals( prevName ) ? 0 : 1;
                    nameChange.addDataPoint( time, changed );
                    if ( changed == 1 ) {
                        dateToNameChange.put( time,
                                new String[]{prevName, t.getName()} );
                    }

                } else {
                    nameChange.addDataPoint( time, -1 );
                }

                prevEles = eles;
                prevName = t.getName();

            } else {
                structureSeries.addDataPoint( time, -1 );
                nameChange.addDataPoint( time, -1 );
            }
        }

        // Order here is order in the chart, so it matters.
        existChart.addSeries( nameChange );
        existChart.addSeries( structureSeries );
        existChart.addSeries( existSeries );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( existChart );

        hcGsonMap.put( "dateToGOEditionId", dateToGOEditionId );
        hcGsonMap.put( "dateToNameChange", dateToNameChange );
        hcGsonMap.put( "goId", currentTerm.getGoId() );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_overview", new Gson().toJson( hcGsonMap ) );

    }

    /**
     * Entry point to fetch data for the Gene chart which shows counts of genes with this term annotated
     * to it over time (both directly and through propagation)
     */
    public void fetchGeneChart() {
        // Create the 'Gene Count' chart
        Collection<Species> species = allSpecies ? cache.getSpeciesList() : Lists.newArrayList( session.getSpecies() );
        ChartValues geneChart = new ChartValues( "Genes Annotated" + (allSpecies ? "" : " in " + session.getSpecies().getCommonName()), "Gene Count", "Date" );
        geneChart.setSubtitle( currentTerm.getGoId() + " - " + currentTerm.getName() );
        Map<Species, Series> series = new HashMap<>();
        Map<Species, Series> directSeries = new HashMap<>();
        Map<Long, Integer> totalSeriesData = new HashMap<>();
        Map<Long, Integer> directTotalSeriesData = new HashMap<>();
        for ( Species sp : species ) {

            Collection<Edition> eds = cache.getAllEditions( sp );

            if ( eds != null ) {

                SeriesExtra s = new SeriesExtra( allSpecies ? sp.getScientificName() : "All" );
                s.putExtra( "visible", false );
                Series s2 = new Series( allSpecies ? sp.getScientificName() + " Direct" : "Direct Only" );
                series.put( sp, s );
                directSeries.put( sp, s2 );
                for ( Edition ed : eds ) {
                    GeneOntologyTerm t = cache.getTerm( ed.getGoEdition(), currentTerm.getGoId() );

                    if ( t != null ) {
                        // If term existed
                        if ( cache.getAggregate( ed ) != null ) {
                            // if this returns null it means the edition has no data in it
                            // most likely missing data

                            // Inferred annotations

                            Integer cnt = cache.getInferredAnnotationCount( ed, t );
                            cnt = (cnt == null) ? 0 : cnt;
                            s.addDataPoint( ed.getDate(), cnt );

                            // Total Inferred

                            Integer totalCnt = totalSeriesData.get( ed.getDate().getTime() );
                            totalSeriesData.put( ed.getDate().getTime(), totalCnt == null ? cnt : totalCnt + cnt );

                            // Direct annotations

                            cnt = cache.getDirectAnnotationCount( ed, t );
                            cnt = (cnt == null) ? 0 : cnt;
                            s2.addDataPoint( ed.getDate(), cnt );

                            // Total Direct

                            totalCnt = directTotalSeriesData.get( ed.getDate().getTime() );
                            directTotalSeriesData.put( ed.getDate().getTime(),
                                    totalCnt == null ? cnt : totalCnt + cnt );
                        }

                    }
                }
            }
        }

        // Totals
        Series totalSeries = new Series( "Total" );
        for ( Entry<Long, Integer> entry : totalSeriesData.entrySet() ) {
            totalSeries.addDataPoint( entry.getKey(), entry.getValue() );
        }

        //TODO show totals or no? Ask Paul.
        //geneChart.addSeries( totalSeries );

        Series directTotalSeries = new Series( "Total Direct" );
        for ( Entry<Long, Integer> entry : directTotalSeriesData.entrySet() ) {
            directTotalSeries.addDataPoint( entry.getKey(), entry.getValue() );
        }

        //geneChart.addSeries( directTotalSeries );

        // Direct & Inferred

        for ( Species sp : series.keySet() ) {
            geneChart.addSeries( series.get( sp ) );
            geneChart.addSeries( directSeries.get( sp ) );
        }

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( geneChart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_gene", new Gson().toJson( hcGsonMap ) );
    }

    /**
     * Entry point to fetch data for the creation of the evidence chart, which shows total counts of annotations over
     * time grouped by evidence category (not unique genes, this retrieves total counts of unique annotations from the
     * database)
     */
    public void fetchEvidenceChart() {
        // Make evidence charts

        Map<Evidence, Map<Date, Integer>> evidenceCounts = allSpecies ?
                annotationService.fetchEvidenceCounts( currentTerm ) :
                annotationService.fetchEvidenceCountsInSpecies( currentTerm, session.getSpecies() );

        ChartValues evidenceChart = new ChartValues( "Annotation Evidence Counts" + (allSpecies ? "" : " in " + session.getSpecies().getCommonName()),
                "Annotation Count", "Date" );
        evidenceChart.setSubtitle( currentTerm.getGoId() + " - " + currentTerm.getName() );

        Map<Date, Integer> curatedData = Maps.newLinkedHashMap();
        Map<Date, Integer> automaticData = Maps.newLinkedHashMap();

        for ( Entry<Evidence, Map<Date, Integer>> entry : evidenceCounts.entrySet() ) {
            Evidence evidence = entry.getKey();
            Map<Date, Integer> data = evidence.isCurated() ? curatedData : automaticData;
            entry.getValue().forEach(
                    (k, v) -> data.merge(k, v, (v1, v2) -> v1 + v2)
            );
        }

        Series curated = new Series( "Curated" );
        for ( Entry<Date, Integer> entry : curatedData.entrySet() ) {
            curated.addDataPoint( entry.getKey(), entry.getValue() );
        }

        Series automatic = new Series( "Automatic" );
        for ( Entry<Date, Integer> entry : automaticData.entrySet() ) {
            automatic.addDataPoint( entry.getKey(), entry.getValue() );
        }

        evidenceChart.addSeries( curated );
        evidenceChart.addSeries( automatic );

        evidenceChart.getExtra().put( "dateToEdition", createDateToEditionMap() );

        Map<String, Object> hcGsonMap = createHCCallbackParamMap( evidenceChart );

        RequestContext.getCurrentInstance().addCallbackParam( "HC_evidence", new Gson().toJson( hcGsonMap ) );
    }

    private Map<Long, Integer> createDateToEditionMap() {

        // A map that will be needed in the front end for drilling down
        Map<Long, Integer> dateToEdition = new HashMap<>();

        cache.getAllEditions( session.getSpecies() ).forEach( ed -> {
            dateToEdition.put( ed.getDate().getTime(), ed.getEdition() );
        } );

        return dateToEdition;
    }

    public void evidenceChartClickEvent() {
        Integer editionId;
        try {
            editionId = Integer.valueOf(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );
        } catch (NumberFormatException e) {
            log.warn( e );
            return;
        }

        if (allSpecies ) {
            return; // Not supported yet
        }

        evidenceChartClickEdition = cache.getEdition( session.getSpecies(), editionId );

        evidenceChartClickData = annotationService.fetchEvidenceCountsInSpecies( currentTerm, session.getSpecies(), evidenceChartClickEdition )
        .entrySet().stream().map( e -> new Tuples.Tuple2<>( e.getKey(), e.getValue() ) ).collect( Collectors.toList());
    }

}
