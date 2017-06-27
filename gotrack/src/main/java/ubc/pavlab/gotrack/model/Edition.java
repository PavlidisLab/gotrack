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

import ubc.pavlab.gotrack.model.dto.EditionDTO;

import java.sql.Date;

/**
 * Immutable representation of an edition of gene annotations
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Edition implements Comparable<Edition> {

    private final Integer edition;
    private final Date date;
    private final GOEdition goEdition;
    private final Integer release;

    /**
     * 
     */
    public Edition( EditionDTO dto, GOEdition goEdition ) {
        this.edition = dto.getEdition();
        this.date = dto.getDate();
        this.release = dto.getRelease();
        this.goEdition = goEdition;
    }

    public Integer getEdition() {
        return edition;
    }

    public Date getDate() {
        return date;
    }

    public Date getGoDate() {
        return goEdition.getDate();
    }

    public Integer getGoEditionId() {
        return goEdition.getId();
    }

    public GOEdition getGoEdition() {
        return goEdition;
    }

    public Integer getRelease() {
        return release;
    }

    @Override
    public String toString() {
        return "Edition [edition=" + edition + ", date=" + date + ", release=" + release + ", goDate=" +
                goEdition.getDate() + ", goEditionId=" + goEdition.getId() + "]";
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

    @Override
    public int compareTo( Edition o ) {
        return this.getEdition().compareTo( o.getEdition() );
    }
}
