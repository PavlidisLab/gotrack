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
public final class Annotation {

    private final String qualifier;
    private final Evidence evidence;
    private final String reference;

    public Annotation( String qualifier, Evidence evidence, String reference ) {
        super();
        this.qualifier = qualifier;
        this.evidence = evidence;
        this.reference = reference;
    }

    public String getQualifier() {
        return qualifier;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( evidence == null ) ? 0 : evidence.hashCode() );
        result = prime * result + ( ( qualifier == null ) ? 0 : qualifier.hashCode() );
        result = prime * result + ( ( reference == null ) ? 0 : reference.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Annotation other = ( Annotation ) obj;
        if ( evidence == null ) {
            if ( other.evidence != null ) return false;
        } else if ( !evidence.equals( other.evidence ) ) return false;
        if ( qualifier == null ) {
            if ( other.qualifier != null ) return false;
        } else if ( !qualifier.equals( other.qualifier ) ) return false;
        if ( reference == null ) {
            if ( other.reference != null ) return false;
        } else if ( !reference.equals( other.reference ) ) return false;
        return true;
    }

}
