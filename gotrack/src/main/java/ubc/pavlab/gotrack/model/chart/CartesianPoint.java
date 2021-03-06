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

/**
 * Represents a simple point with a long x value and an arbitrary number y value
 * 
 * @author mjacobson
 */
public class CartesianPoint implements Point<Double, Number> {
    private final double x;
    private final Number y;

    public CartesianPoint( Number x, Number y ) {
        super();
        this.x = x.doubleValue();
        this.y = y;
    }

    public CartesianPoint( Date date, Number y ) {
        this.x = date.getTime();
        this.y = y;
    }

    @Override
    public Double getX() {
        return x;
    }

    @Override
    public Number getY() {
        return y;
    }

    @Override
    public int compareTo( Point<Double, Number> o ) {
        return this.getX().compareTo( o.getX() );
    }
}
