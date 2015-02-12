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
public class Edition {

    private Integer edition;
    private Date date;
    private Date goDate;

    /**
     * 
     */
    public Edition() {
        // TODO Auto-generated constructor stub
    }

    public Edition( Integer edition, Date date ) {
        super();
        this.edition = edition;
        this.date = date;
    }

    public Edition( Integer edition, Date date, Date goDate ) {
        super();
        this.edition = edition;
        this.date = date;
        this.goDate = goDate;
    }

    public Integer getEdition() {
        return edition;
    }

    public void setEdition( Integer edition ) {
        this.edition = edition;
    }

    public Date getDate() {
        return date;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

    public Date getGoDate() {
        return goDate;
    }

    public void setGoDate( Date goDate ) {
        this.goDate = goDate;
    }

    @Override
    public String toString() {
        return "Edition [edition=" + edition + ", date=" + date + ", goDate=" + goDate + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( edition == null ) ? 0 : edition.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Edition other = ( Edition ) obj;
        if ( edition == null ) {
            if ( other.edition != null ) return false;
        } else if ( !edition.equals( other.edition ) ) return false;
        return true;
    }

}
