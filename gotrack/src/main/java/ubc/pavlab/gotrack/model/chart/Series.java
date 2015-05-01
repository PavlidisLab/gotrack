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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Series {

    private String name;
    private String description;
    private Collection<Point> data = new ArrayList<>();

    public Series( String name ) {
        super();
        this.name = name;
    }

    public boolean addDataPoint( long x, Number y ) {
        return this.data.add( new Point( x, y ) );
    }

    public void addDataPoint( Map<Object, Number> vals ) {
        Collection<Point> ps = new ArrayList<>();
        for ( Entry<Object, Number> entry : vals.entrySet() ) {
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
            try {
                long timeInMillisSinceEpoch = sdf.parse( ( String ) entry.getKey() ).getTime();
                ps.add( new Point( timeInMillisSinceEpoch, entry.getValue() ) );
            } catch ( ParseException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.data.addAll( ps );
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Collection<Point> getData() {
        return data;
    }

    public void setData( Collection<Point> data ) {
        this.data = data;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
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
