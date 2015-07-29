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
import java.util.Set;

import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.dto.AnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.AnnotationDTO;
import ubc.pavlab.gotrack.model.dto.CategoryCountDTO;
import ubc.pavlab.gotrack.model.dto.EnrichmentDTO;

/**
 * This interface represents a contract for {@link AnnotationDAOImpl}. Note that all methods are
 * read-only.
 */
public interface AnnotationDAO {
    // Actions ------------------------------------------------------------------------------------
    /**
     * Retrieve data necessary for enrichment of given set of genes
     */
    public List<EnrichmentDTO> enrich( Integer species, Set<Gene> genes ) throws DAOException;

    /**
     * Retrieves counts of unique annotations grouped by evidence category over time for a specific GO Id
     */
    public List<CategoryCountDTO> categoryCounts( String goId ) throws DAOException;

    /**
     * Retrieves counts of unique genes which have this GO Id annotated to it over time
     */
    public List<AnnotationCountDTO> directGeneCounts( String goId ) throws DAOException;

    /**
     * Retrieve data necessary for tracking a specific gene over time
     */
    public List<AnnotationDTO> track( Integer speciesId, String symbol ) throws DAOException;

}