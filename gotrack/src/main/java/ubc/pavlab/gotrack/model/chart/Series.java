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

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a series in a chart, all methods that return the underlying data should make sure it is sorted first.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Series {

    private String name;
    private Object extra;
    private List<Point> data = new ArrayList<>();
    private boolean sorted = true;

    public Series( String name ) {
        super();
        this.name = name;
    }

    public boolean addDataPoint( long x, Number y ) {
        this.sorted = false;
        return this.data.add( new CartesianPoint( x, y ) );
    }

    public boolean addDataPoint( Date d, Number y ) {
        this.sorted = false;
        return this.data.add( new CartesianPoint( d, y ) );
    }

    public boolean addDataPoint( Date d, Number left, Number right ) {
        this.sorted = false;
        return this.data.add( new RangePoint( d, left, right ) );
    }

    public void addDataPoint( List<Point> ps ) {
        this.sorted = false;
        this.data.addAll( ps );
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Collection<Point> getData() {
        if ( !this.sorted ) {
            Collections.sort( data );
            this.sorted = true;
        }
        return data;
    }

    public void setData( Collection<Point> data ) {
        this.sorted = false;
        this.data = new ArrayList<>( data );
    }

    public Object getExtra() {
        return extra;
    }

    public void setExtra( Object extra ) {
        this.extra = extra;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Series other = ( Series ) obj;
        if ( name == null ) {
            if ( other.name != null ) return false;
        } else if ( !name.equals( other.name ) ) return false;
        return true;
    }

}
