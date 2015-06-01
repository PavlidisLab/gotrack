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

import java.util.List;

import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.SpeciesDTO;

/**
 * This interface represents a contract for a DAO for the {@link Species} model. Note that all methods are read-only.
 */
public interface SpeciesDAO {

    // Actions ------------------------------------------------------------------------------------

    /**
     * Returns the Species from the database matching the given ID, otherwise null.
     * 
     * @param id The ID of the annotation to be returned.
     * @return The annotation from the database matching the given ID, otherwise null.
     * @throws DAOException If something fails at database level.
     */
    public SpeciesDTO find( Long id ) throws DAOException;

    /**
     * Returns a list of all Species from the database ordered by Species common_name. The list is never null and is
     * empty when the database does not contain any Species.
     * 
     * @return A list of all Species from the database ordered by Species common_name.
     * @throws DAOException If something fails at database level.
     */
    public List<SpeciesDTO> list() throws DAOException;

}