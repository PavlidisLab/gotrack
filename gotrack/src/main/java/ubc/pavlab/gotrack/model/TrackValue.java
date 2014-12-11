/*
 * The gotrack project
 * 
 * Copyright (c) 2014 University of British Columbia
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

import java.sql.Date;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class TrackValue {

    private String accession;
    private Date date;
    private Integer edition;
    private Integer directAnnotations;

    /**
     * 
     */
    public TrackValue() {
        // TODO Auto-generated constructor stub
    }

    public TrackValue( String accession, Date date, Integer edition, Integer directAnnotations ) {
        super();
        this.accession = accession;
        this.date = date;
        this.edition = edition;
        this.directAnnotations = directAnnotations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( accession == null ) ? 0 : accession.hashCode() );
        result = prime * result + ( ( date == null ) ? 0 : date.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        TrackValue other = ( TrackValue ) obj;
        if ( accession == null ) {
            if ( other.accession != null ) return false;
        } else if ( !accession.equals( other.accession ) ) return false;
        if ( date == null ) {
            if ( other.date != null ) return false;
        } else if ( !date.equals( other.date ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "TrackValue [accession=" + accession + ", date=" + date + ", directAnnotations=" + directAnnotations
                + "]";
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public Date getDate() {
        return date;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

    public Integer getEdition() {
        return edition;
    }

    public void setEdition( Integer edition ) {
        this.edition = edition;
    }

    public Integer getDirectAnnotations() {
        return directAnnotations;
    }

    public void setDirectAnnotations( Integer directAnnotations ) {
        this.directAnnotations = directAnnotations;
    }
}
