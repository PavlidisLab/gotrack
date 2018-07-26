/*
 * The gotrack project
 * 
 * Copyright (c) 2016 University of British Columbia
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
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.model.Aggregate;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.chart.ChartValues;
import ubc.pavlab.gotrack.model.chart.Series;
import ubc.pavlab.gotrack.model.chart.SeriesExtra;
import ubc.pavlab.gotrack.model.go.GeneOntology;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * @author mjacobson
 */
@Named
@ApplicationScoped
public class Trends {

    private static final Logger log = Logger.getLogger( Trends.class );

    private static int MULTIFUNCTIONALITY_SCALE = 10000;

    @Inject
    private Cache cache;

    private Map<Species, String> allChartsJSON = Maps.newConcurrentMap();

    public Trends() {
        log.info( "Trends created" );
    }

    @PostConstruct
    public void postConstruct() {

        ChartValues ontSize = new ChartValues( "Size of Gene Ontology", "# Terms", "Date" );
        Series termCountSeries = new Series( "Distinct Terms" );


        for ( GeneOntology ont : cache.getAllOntologies() ) {
            // changed to use sizeWithoutObsolete instead of size
            termCountSeries.addDataPoint( ont.getEdition().getDate(), ont.sizeWithoutObsolete() );
        }
        ontSize.addSeries( termCountSeries );

        for ( Species sp : cache.getSpeciesList() ) {
            ChartValues geneCountChart = new ChartValues( "Distinct Annotated Genes", "# Genes", "Date" );
            ChartValues termsForGeneChart = new ChartValues( "Terms Annotated Per Gene", "# Terms", "Date" );
            ChartValues inferredGenesForTermChart = new ChartValues( "Genes Annotated Per Term", "# Genes", "Date" );
            ChartValues multifunctionalityChart = new ChartValues( "Multifunctionality", "Multifunctionality [10^-5]", "Date" );
            ChartValues geneJaccardChart = new ChartValues( "Semantic Similarity", "Jaccard Index", "Date" );

            geneCountChart.setSubtitle( sp.getScientificName() );
            termsForGeneChart.setSubtitle( sp.getScientificName() );
            inferredGenesForTermChart.setSubtitle( sp.getScientificName() );
            multifunctionalityChart.setSubtitle( sp.getScientificName() );
            geneJaccardChart.setSubtitle( sp.getScientificName() );

            geneJaccardChart.setMin( 0 );
            geneJaccardChart.setMax( 1 );

            Series geneCountSeries = new Series( "Distinct Genes" );
            Series directTermsForGeneSeries = new Series( "Direct" );
            SeriesExtra inferredTermsForGeneSeries = new SeriesExtra( "Inferred" );
            inferredTermsForGeneSeries.putExtra( "visible", false );
            Series inferredGenesForTermSeries = new Series( "Group Size" );
            Series multifunctionalitySeries = new Series( "Multifunctionality" );
            Series geneDirectJaccardSeries = new Series( "Direct" );
            SeriesExtra geneInferredJaccardSeries = new SeriesExtra( "Inferred" );
            geneInferredJaccardSeries.putExtra( "visible", false );

            for ( Edition ed : cache.getAllEditions( sp ) ) {

                Aggregate agg = cache.getAggregate( ed );

                if (agg != null) {
                    geneCountSeries.addDataPoint( ed.getDate(), agg.getGeneCount() );
                    directTermsForGeneSeries.addDataPoint( ed.getDate(), agg.getAvgDirectByGene() );
                    geneDirectJaccardSeries.addDataPoint( ed.getDate(), agg.getAvgDirectSimilarity() );
                    inferredGenesForTermSeries.addDataPoint( ed.getDate(), agg.getAvgGenesByTerm() );
                    inferredTermsForGeneSeries.addDataPoint( ed.getDate(), agg.getAvgInferredByGene() );
                    geneInferredJaccardSeries.addDataPoint( ed.getDate(), agg.getAvgInferredSimilarity() );
                    multifunctionalitySeries.addDataPoint( ed.getDate(), MULTIFUNCTIONALITY_SCALE * agg.getAvgMultifunctionality() );
                }
            }

            geneCountChart.addSeries( geneCountSeries );
            termsForGeneChart.addSeries( inferredTermsForGeneSeries );
            termsForGeneChart.addSeries( directTermsForGeneSeries );
            geneJaccardChart.addSeries( geneInferredJaccardSeries );
            geneJaccardChart.addSeries( geneDirectJaccardSeries );
            inferredGenesForTermChart.addSeries( inferredGenesForTermSeries );
            multifunctionalityChart.addSeries( multifunctionalitySeries );

            Map<String, ChartValues> chartsMap = Maps.newHashMap();

            chartsMap.put( "geneCount", geneCountChart );
            chartsMap.put( "termsPerGene", termsForGeneChart );
            chartsMap.put( "genesPerTerm", inferredGenesForTermChart );
            chartsMap.put( "multi", multifunctionalityChart );
            chartsMap.put( "similarity", geneJaccardChart );

            chartsMap.put( "ontSize", ontSize );

            allChartsJSON.put( sp, new Gson().toJson( chartsMap ) );

        }
    }

    public void loadCharts( Species species ) {
        RequestContext.getCurrentInstance().addCallbackParam( "HC_map", allChartsJSON.get( species ) );
    }

}
