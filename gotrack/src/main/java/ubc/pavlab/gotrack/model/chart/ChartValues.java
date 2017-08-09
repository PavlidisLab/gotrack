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

package ubc.pavlab.gotrack.model.chart;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a chart which holds series, points in a simple manner for conversion to JSON
 * 
 * @author mjacobson
 * @version $Id$
 */
public class ChartValues {

    private Set<Series> series = new LinkedHashSet<>();
    private final String title;
    private final String xLabel;
    private final String yLabel;
    private Integer min;
    private Integer max;

    public ChartValues() {
        this(null, null, null);
    }

    public ChartValues( String title, String xLabel, String yLabel ) {
        super();
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }


    public boolean addSeries( Series s ) {
        s.sort();
        return series.add( s );
    }

    public Series addSeries( String name, List<Point> ps ) {
        Series s = new Series( name );
        s.addDataPoint( ps );
        s.sort();
        series.add( s );
        return s;
    }

    public Set<Series> getSeries() {
        return series;
    }

    public String getTitle() {
        return title;
    }

    public String getxLabel() {
        return xLabel;
    }

    public String getyLabel() {
        return yLabel;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin( Integer min ) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax( Integer max ) {
        this.max = max;
    }

    public void sortSeries() {
        for ( Series s : series ) {
            s.sort();
        }
    }

}
