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

package ubc.pavlab.gotrack.converter;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

import ubc.pavlab.gotrack.model.Dataset;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */

@FacesConverter(value = "datasetConverter")
public class DatasetConverter extends EnumConverter {

    public DatasetConverter() {
        super( Dataset.class );
    }

}
