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

import java.sql.Date;

import ubc.pavlab.gotrack.model.dto.GOEditionDTO;

/**
 * Represents an edition of GO. Currently the edition numbers are out of order, so rely on sorting by date.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class GOEdition implements Comparable<GOEdition> {

    private final Integer edition;
    private final Date date;

    public GOEdition( GOEditionDTO dto ) {
        this.edition = dto.getEdition();
        this.date = dto.getDate();
    }

    public Integer getEdition() {
        return edition;
    }

    public Date getDate() {
        return date;
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
        GOEdition other = ( GOEdition ) obj;
        if ( edition == null ) {
            if ( other.edition != null ) return false;
        } else if ( !edition.equals( other.edition ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "GOEdition [edition=" + edition + ", date=" + date + "]";
    }

    @Override
    public int compareTo( GOEdition o ) {
        // TODO Auto-generated method stub
        return this.date.compareTo( o.getDate() );
    }

}
