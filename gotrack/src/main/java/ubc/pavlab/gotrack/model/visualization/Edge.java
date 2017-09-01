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

package ubc.pavlab.gotrack.model.visualization;

import com.google.common.collect.Lists;
import ubc.pavlab.gotrack.model.go.RelationshipType;

import java.util.List;

/**
 * Represents a connection between two nodes using their ids
 * 
 * @author mjacobson
 */
public class Edge {
    private final int from;
    private final int to;
    private final RelationshipType type;
    private final List<String> classes;

    public Edge( int from, int to, RelationshipType type, List<String> classes ) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.classes = classes;
    }

    public Edge ( int from, int to, RelationshipType type) {
        this( from, to, type, Lists.<String>newArrayList() );
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public RelationshipType getType() {
        return type;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void addClass(String clazz) {
        classes.add( clazz );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + from;
        result = prime * result + to;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Edge other = ( Edge ) obj;
        if ( from != other.from ) return false;
        if ( to != other.to ) return false;
        return true;
    }

}
