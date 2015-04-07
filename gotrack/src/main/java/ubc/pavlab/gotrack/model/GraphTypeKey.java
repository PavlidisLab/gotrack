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

/**
 * Represents a key for a map to a type of graph under specific options
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GraphTypeKey {

    public enum GraphType {
        annotation, jaccard, multifunctionality, lossgain
    };

    private final GraphType graphType;
    private final Boolean split;
    private final Boolean propagate;

    public GraphTypeKey( GraphType graphType, Boolean split, Boolean propagate ) {
        super();
        this.graphType = graphType;
        this.split = split;
        this.propagate = propagate;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public Boolean getSplit() {
        return split;
    }

    public Boolean getPropagate() {
        return propagate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( graphType == null ) ? 0 : graphType.hashCode() );
        result = prime * result + ( ( propagate == null ) ? 0 : propagate.hashCode() );
        result = prime * result + ( ( split == null ) ? 0 : split.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GraphTypeKey other = ( GraphTypeKey ) obj;
        if ( graphType != other.graphType ) return false;
        if ( propagate == null ) {
            if ( other.propagate != null ) return false;
        } else if ( !propagate.equals( other.propagate ) ) return false;
        if ( split == null ) {
            if ( other.split != null ) return false;
        } else if ( !split.equals( other.split ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "GraphTypeKey [graphType=" + graphType + ", split=" + split + ", propagate=" + propagate + "]";
    }

}
