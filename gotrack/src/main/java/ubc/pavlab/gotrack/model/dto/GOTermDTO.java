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

import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Data Transfer Object for {@link GeneOntologyTerm}
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GOTermDTO {
    private final Integer goEdition;
    private final String goId;
    private final String name;
    private final String aspect;
    private final Boolean obsolete;

    public GOTermDTO( Integer goEdition, String goId, String name, String aspect, Boolean obsolete ) {
        super();
        this.goEdition = goEdition;
        this.goId = goId;
        this.name = name;
        this.aspect = aspect;
        this.obsolete = obsolete;
    }

    public Integer getGoEdition() {
        return goEdition;
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

    public boolean isObsolete() {    return obsolete;   }
}
