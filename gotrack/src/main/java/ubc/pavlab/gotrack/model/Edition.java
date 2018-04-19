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

import lombok.Getter;
import ubc.pavlab.gotrack.model.dto.EditionDTO;

import java.sql.Date;

/**
 * Immutable representation of an edition of gene annotations
 * 
 * @author mjacobson
 * @version $Id$
 */
@Getter
public class Edition implements Comparable<Edition> {

    private final Species species;
    private final Integer edition;
    private final Date date;
    private final GOEdition goEdition;
    private final Integer release;

    /**
     * 
     */
    public Edition( EditionDTO dto, Species species, GOEdition goEdition ) {
        this.species = species;
        this.edition = dto.getEdition();
        this.date = dto.getDate();
        this.release = dto.getRelease();
        this.goEdition = goEdition;
    }

    @Override
    public String toString() {
        return "Edition{" +
                "species=" + species +
                ", edition=" + edition +
                ", date=" + date +
                ", goEdition=" + goEdition +
                ", release=" + release +
                '}';
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        Edition edition1 = (Edition) o;

        if ( !species.equals( edition1.species ) ) return false;
        return edition.equals( edition1.edition );
    }

    @Override
    public int hashCode() {
        int result = species.hashCode();
        result = 31 * result + edition.hashCode();
        return result;
    }

    @Override
    public int compareTo( Edition o ) {
        return this.getEdition().compareTo( o.getEdition() );
    }
}
