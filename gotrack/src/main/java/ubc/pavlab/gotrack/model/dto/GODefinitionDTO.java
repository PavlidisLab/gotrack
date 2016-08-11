/*
 * The gotrack project
 * 
 * Copyright (c) 2016 University of British Columbia
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
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GODefinitionDTO {

    private final String goId;
    private final String definition;

    public GODefinitionDTO( String goId, String definition ) {
        super();
        this.goId = goId;
        this.definition = definition;
    }

    public String getGoId() {
        return goId;
    }

    public String getDefinition() {
        return definition;
    }

}
