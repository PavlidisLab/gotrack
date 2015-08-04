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

package ubc.pavlab.gotrack.model.dto;

/**
 * Data Transfer Object for gathering direct gene counts for go terms.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class DirectAnnotationCountDTO {
    private final Integer species;
    private final Integer edition;
    private final String goId;
    private final Integer count;

    public DirectAnnotationCountDTO( Integer speciesId, Integer ed, String goid, Integer count ) {
        super();
        this.species = speciesId;
        this.edition = ed;
        this.goId = goid;
        this.count = count;
    }

    public Integer getSpecies() {
        return species;
    }

    public Integer getEdition() {
        return edition;
    }

    public String getGoId() {
        return goId;
    }

    public Integer getCount() {
        return count;
    }
}
