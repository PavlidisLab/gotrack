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
public class EvidenceReference {

    private final String evidence;
    private final String reference;
    private final String category;

    // public EvidenceReference( String evidence, String reference ) {
    // this( evidence, reference, null );
    // }

    public EvidenceReference( String evidence, String reference, String category ) {
        super();
        this.evidence = evidence;
        this.reference = reference;
        this.category = category;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getReference() {
        return reference;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "EvidenceReference [evidence=" + evidence + ", reference=" + reference + ", category=" + category + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( evidence == null ) ? 0 : evidence.hashCode() );
        result = prime * result + ( ( reference == null ) ? 0 : reference.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        EvidenceReference other = ( EvidenceReference ) obj;
        if ( evidence == null ) {
            if ( other.evidence != null ) return false;
        } else if ( !evidence.equals( other.evidence ) ) return false;
        if ( reference == null ) {
            if ( other.reference != null ) return false;
        } else if ( !reference.equals( other.reference ) ) return false;
        return true;
    }

}
