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

package ubc.pavlab.gotrack.dao;

import java.util.List;

import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.GeneStatsDTO;
import ubc.pavlab.gotrack.model.dto.TermStatsDTO;

/**
 * This interface represents a contract for a DAO for the {@link Species} model. Note that all methods are read-only.
 */
public interface StatsDAO {

    // Actions ------------------------------------------------------------------------------------

    public GeneStatsDTO find( Long id ) throws DAOException;

    public List<GeneStatsDTO> listGenes() throws DAOException;

    public void incrementGeneHit( Integer id, String symbol ) throws DAOException;

    public List<TermStatsDTO> listTerms() throws DAOException;

    public void incrementTermHit( String goId ) throws DAOException;

}
