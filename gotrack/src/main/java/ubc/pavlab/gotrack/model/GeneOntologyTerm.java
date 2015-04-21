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
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntologyTerm {

    private final String goId;
    private String name;
    private String aspect;
    private boolean obsolete = false;

    public GeneOntologyTerm( String goId ) {
        super();
        this.goId = goId;
        this.name = null;
        this.aspect = null;
    }

    public GeneOntologyTerm( String goId, String name, String aspect ) {
        super();
        this.goId = goId;
        this.name = name;
        this.aspect = aspect;
    }

    public String getGoId() {
        return goId;
    }

    public String getName() {
        return name;
    }

    public String getAspect() {
        return aspect;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setAspect( String aspect ) {
        this.aspect = aspect;
    }

    public void setObsolete( boolean obsolete ) {
        this.obsolete = obsolete;
    }

    @Override
    public String toString() {
        return "GeneOntologyTerm [goId=" + goId + ", name=" + name + ", aspect=" + aspect + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( goId == null ) ? 0 : goId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GeneOntologyTerm other = ( GeneOntologyTerm ) obj;
        if ( goId == null ) {
            if ( other.goId != null ) return false;
        } else if ( !goId.equals( other.goId ) ) return false;
        return true;
    }

    public boolean isObsolete() {
        return obsolete;
    }

}
