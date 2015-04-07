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

package ubc.pavlab.gotrack.model;

import java.util.Map;
import java.util.Set;

import org.primefaces.model.chart.LineChartModel;

/**
 * Holds a tuple that of GOCharts and LineChartModels
 * 
 * @author mjacobson
 * @version $Id$
 */
public class ChartTuple {

    private final Map<GraphTypeKey, LineChartModel> lineChartModelMap;
    private final Map<GraphTypeKey, GoChart<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> goChartMap;

    public ChartTuple( Map<GraphTypeKey, LineChartModel> lineChartModelMap,
            Map<GraphTypeKey, GoChart<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> goChartMap ) {
        super();
        this.lineChartModelMap = lineChartModelMap;
        this.goChartMap = goChartMap;
    }

    public Map<GraphTypeKey, LineChartModel> getLineChartModelMap() {
        return lineChartModelMap;
    }

    public Map<GraphTypeKey, GoChart<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> getGoChartMap() {
        return goChartMap;
    }

}
