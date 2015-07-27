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

import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.dto.AccessionDTO;
import ubc.pavlab.gotrack.model.dto.AdjacencyDTO;
import ubc.pavlab.gotrack.model.dto.AggregateDTO;
import ubc.pavlab.gotrack.model.dto.AnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.EditionDTO;
import ubc.pavlab.gotrack.model.dto.EvidenceDTO;
import ubc.pavlab.gotrack.model.dto.GOEditionDTO;
import ubc.pavlab.gotrack.model.dto.GOTermDTO;
import ubc.pavlab.gotrack.model.dto.GeneDTO;
import ubc.pavlab.gotrack.model.dto.SimpleAnnotationDTO;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface CacheDAO {

    /**
     * @return Map of species to ordered linkedlist of editions
     * @throws DAOException
     */
    public List<EditionDTO> getAllEditions( int[] speciesRestrictions ) throws DAOException;

    public List<AnnotationCountDTO> getGOSizes( int[] speciesRestrictions ) throws DAOException;

    public List<AggregateDTO> getAggregates( int[] speciesRestrictions ) throws DAOException;

    public List<GeneDTO> getCurrentGenes( int[] speciesRestrictions ) throws DAOException;

    public List<GOTermDTO> getGoTerms() throws DAOException;

    public List<AdjacencyDTO> getAdjacencies() throws DAOException;

    public List<EvidenceDTO> getEvidence() throws DAOException;

    public List<AccessionDTO> getAccessions( int[] speciesRestrictions ) throws DAOException;

    public List<GOEditionDTO> getAllGOEditions() throws DAOException;

    public List<SimpleAnnotationDTO> getSimpleAnnotations( Integer speciesId, Edition ed ) throws DAOException;

}
