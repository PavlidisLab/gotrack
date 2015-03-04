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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GoChart<X extends Comparable<X>, Y> {

    private String title = "title";
    private String xLabel = "x";
    private String yLabel = "y";
    private Map<String, LinkedHashMap<X, Y>> series = new HashMap<String, LinkedHashMap<X, Y>>();
    private Integer min;
    private Integer max;

    public GoChart() {
        super();
    }

    public GoChart( Map<String, Map<X, Y>> sMap ) {
        this( null, null, null, sMap );
    }

    public GoChart( String title ) {
        this( title, null, null, null );
    }

    public GoChart( String title, String xLabel, String yLabel ) {
        this( title, xLabel, yLabel, null );
    }

    public GoChart( String title, String xLabel, String yLabel, Map<String, Map<X, Y>> sMap ) {
        super();
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        if ( sMap != null ) {
            for ( Entry<String, Map<X, Y>> sEntry : sMap.entrySet() ) {
                Map<X, Y> s = sEntry.getValue();
                String label = sEntry.getKey();
                List<X> editions = new ArrayList<X>( s.keySet() );
                Collections.sort( editions );
                LinkedHashMap<X, Y> orderedMap = new LinkedHashMap<X, Y>();
                for ( X x : editions ) {
                    orderedMap.put( x, s.get( x ) );
                }
                series.put( label, orderedMap );
            }
        }

    }

    public Map<X, Y> get( String label ) {
        return Collections.unmodifiableMap( series.get( label ) );
    }

    public Map<X, Y> put( String label, Map<X, Y> s ) {
        List<X> editions = new ArrayList<X>( s.keySet() );
        Collections.sort( editions );
        LinkedHashMap<X, Y> orderedMap = new LinkedHashMap<X, Y>();
        for ( X x : editions ) {
            orderedMap.put( x, s.get( x ) );
        }
        return series.put( label, orderedMap );
    }

    public void putAll( Map<String, Map<X, Y>> sMap ) {
        for ( Entry<String, Map<X, Y>> sEntry : sMap.entrySet() ) {
            Map<X, Y> s = sEntry.getValue();
            String label = sEntry.getKey();
            List<X> editions = new ArrayList<X>( s.keySet() );
            Collections.sort( editions );
            LinkedHashMap<X, Y> orderedMap = new LinkedHashMap<X, Y>();
            for ( X x : editions ) {
                orderedMap.put( x, s.get( x ) );
            }
            series.put( label, orderedMap );
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <U extends Comparable<U>, V extends Set> GoChart<U, V> combineSeries( String title, String xLabel,
            String yLabel, String seriesLabel, GoChart<U, V> goChart ) {
        LinkedHashMap<U, V> combinedSeries = new LinkedHashMap<U, V>();
        for ( LinkedHashMap<U, V> s : goChart.getSeries().values() ) {
            for ( Entry<U, V> dataPoint : s.entrySet() ) {
                U x = dataPoint.getKey();
                V details = dataPoint.getValue();

                V combinedDataPoint = combinedSeries.get( x );

                if ( combinedDataPoint == null ) {
                    combinedDataPoint = ( V ) new HashSet<Object>();
                    combinedSeries.put( x, combinedDataPoint );
                }

                combinedDataPoint.addAll( details );

            }
        }

        GoChart<U, V> newGoChart = new GoChart<U, V>( title, xLabel, yLabel );
        newGoChart.put( seriesLabel, combinedSeries );

        return newGoChart;

    }

    @SuppressWarnings("rawtypes")
    public static <U extends Comparable<U>, V extends Set> GoChart<U, V> combineSeries( String title,
            String seriesLabel, GoChart<U, V> goChart ) {
        return combineSeries( title, goChart.getxLabel(), goChart.getyLabel(), seriesLabel, goChart );

    }

    @SuppressWarnings("rawtypes")
    public static <U extends Comparable<U>, V extends Set> GoChart<U, V> combineSeries( String seriesLabel,
            GoChart<U, V> goChart ) {
        return combineSeries( goChart.getTitle(), goChart.getxLabel(), goChart.getyLabel(), seriesLabel, goChart );

    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getxLabel() {
        return xLabel;
    }

    public void setxLabel( String xLabel ) {
        this.xLabel = xLabel;
    }

    public String getyLabel() {
        return yLabel;
    }

    public void setyLabel( String yLabel ) {
        this.yLabel = yLabel;
    }

    public Map<String, LinkedHashMap<X, Y>> getSeries() {
        return series;
    }

    public void setSeries( Map<String, LinkedHashMap<X, Y>> series ) {
        this.series = series;
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

    @Override
    public String toString() {
        return "GoChart [title=" + title + ", xLabel=" + xLabel + ", yLabel=" + yLabel + ", seriesCount="
                + series.size() + "]";
    }

}
