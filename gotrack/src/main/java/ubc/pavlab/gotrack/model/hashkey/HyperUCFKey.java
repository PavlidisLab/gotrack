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

package ubc.pavlab.gotrack.model.hashkey;

/**
 * Represents a combined key to a map where the hashcode should be a combination of each cell in a contingency table.
 * Hypergeometric Upper Cumulative Function Key (HyperUCFKey)
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class HyperUCFKey {
    private final int r;
    private final int m;
    private final int k;
    private final int t;

    public HyperUCFKey( int r, int m, int k, int t ) {
        super();
        this.r = r;
        this.m = m;
        this.k = k;
        this.t = t;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + k;
        result = prime * result + m;
        result = prime * result + r;
        result = prime * result + t;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        HyperUCFKey other = ( HyperUCFKey ) obj;
        if ( k != other.k ) return false;
        if ( m != other.m ) return false;
        if ( r != other.r ) return false;
        if ( t != other.t ) return false;
        return true;
    }

}
