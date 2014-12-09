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

package ubc.pavlab.gotrack.dao;

import java.util.Collection;
import java.util.Map;

import ubc.pavlab.gotrack.model.GeneSymbol;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface CacheDAO {

    public Map<Integer, Integer> getCurrentEditions() throws DAOException;

    public Map<String, GeneSymbol> getAccessionToGeneSymbol( Integer species, Integer edition ) throws DAOException;

    public Collection<String> getUniqueGeneSymbols( Integer species, Integer edition ) throws DAOException;

}
