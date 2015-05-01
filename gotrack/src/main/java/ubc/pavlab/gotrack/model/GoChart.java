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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * X Should be some series identifier (like Accession)
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GoChart<X> {

    private final String title;
    private final String xLabel;
    private final String yLabel;
    private final Double min;
    private final Double max;
    private final boolean empty;

    private final Map<X, LinkedHashMap<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allDetailedSeries = new HashMap<>();
    private final Map<String, LinkedHashMap<Edition, ? extends Number>> allStaticSeries = new HashMap<>();

    public GoChart( Map<X, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allDetailedSeries ) {
        this( "title", "x", "y", null, null, allDetailedSeries, null );
    }

    public GoChart( String title, String xLabel, String yLabel,
            Map<X, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allDetailedSeries ) {
        this( title, xLabel, yLabel, null, null, allDetailedSeries, null );
    }

    public <T extends Number> GoChart( String title, String xLabel, String yLabel,
            Map<X, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allDetailedSeries,
            Map<String, Map<Edition, T>> allStaticSeries ) {
        this( title, xLabel, yLabel, null, null, allDetailedSeries, allStaticSeries );
    }

    public <T extends Number> GoChart( String title, String xLabel, String yLabel, Double min, Double max,
            Map<X, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> ds, Map<String, Map<Edition, T>> ss ) {
        super();
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.min = min;
        this.max = max;
        boolean empty = true;
        if ( ds != null ) {
            for ( Entry<X, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> sEntry : ds.entrySet() ) {
                Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> s = sEntry.getValue();
                if ( s != null ) {
                    X label = sEntry.getKey();
                    List<Edition> editions = new ArrayList<Edition>( s.keySet() );
                    Collections.sort( editions );
                    LinkedHashMap<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> orderedMap = new LinkedHashMap<>();
                    for ( Edition e : editions ) {
                        orderedMap.put( e, s.get( e ) );
                        empty = false;
                    }
                    allDetailedSeries.put( label, orderedMap );
                }
            }
        }

        if ( ss != null ) {
            for ( Entry<String, Map<Edition, T>> sEntry : ss.entrySet() ) {
                Map<Edition, T> s = sEntry.getValue();
                if ( s != null ) {
                    String label = sEntry.getKey();
                    List<Edition> editions = new ArrayList<Edition>( s.keySet() );
                    Collections.sort( editions );
                    LinkedHashMap<Edition, Number> orderedMap = new LinkedHashMap<>();
                    for ( Edition e : editions ) {
                        orderedMap.put( e, s.get( e ) );
                        empty = false;
                    }
                    allStaticSeries.put( label, orderedMap );
                }
            }
        }

        this.empty = empty;

    }

    public Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> getDetailedSeries( X label ) {
        LinkedHashMap<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> a = allDetailedSeries.get( label );
        if ( a != null ) {
            return Collections.unmodifiableMap( a );
        } else {
            return null;
        }
    }

    public Map<Edition, Number> getStaticSeries( String label ) {
        LinkedHashMap<Edition, ? extends Number> a = allStaticSeries.get( label );
        if ( a != null ) {
            return Collections.unmodifiableMap( a );
        } else {
            return null;
        }
    }

    public Map<X, LinkedHashMap<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> getAllDetailedSeries() {
        return Collections.unmodifiableMap( allDetailedSeries );
    }

    public Map<String, LinkedHashMap<Edition, ? extends Number>> getAllStaticSeries() {
        return Collections.unmodifiableMap( allStaticSeries );
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "GoChart [title=" + title + ", xLabel=" + xLabel + ", yLabel=" + yLabel + ", allDetailedSeriesSize="
                + allDetailedSeries.size() + ", allStaticSeriesSize=" + allStaticSeries.size() + "]";
    }

    public String getxLabel() {
        return xLabel;
    }

    public String getyLabel() {
        return yLabel;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public boolean isEmpty() {
        return empty;
    }

}
