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

import ubc.pavlab.gotrack.model.dto.GeneStatsDTO;
import ubc.pavlab.gotrack.model.dto.TermStatsDTO;

import java.util.Date;
import java.util.List;

/**
 * This interface represents a contract for {@link StatsDAOImpl}.
 */
public interface StatsDAO {

    // Actions ------------------------------------------------------------------------------------

    /**
     * Retrieve all genes which have been stored as having a page hit along with how many times they have been hit.
     */
    List<GeneStatsDTO> listGenes() throws DAOException;

    /**
     * Increment a specific Gene's (symbol, species) hit count by 1
     */
    void incrementGeneHit( Integer id, String accession, String symbol ) throws DAOException;

    /**
     * Retrieve all terms which have been stored as having a page hit along with how many times they have been hit.
     */
    List<TermStatsDTO> listTerms() throws DAOException;

    /**
     * Increment a specific Term's (GO Id) hit count by 1
     */
    void incrementTermHit( String goId ) throws DAOException;

    /**
     * Estimate number of annotations
     */
    Integer annotationCount() throws DAOException;

    /**
     * Estimate number of GO Terms
     */
    Integer goCount() throws DAOException;

    /**
     * Estimate number of GO Term edges.
     */
    Integer adjacencyCount() throws DAOException;

    /**
     * Count of species with data..
     */
    Integer speciesCount() throws DAOException;

    /**
     * Number of years between the earliest and latest edition available rounded.
     */
    Integer yearsAvailable() throws DAOException;

    /**
     * Count of GO Editions.
     */
    Integer goEditionCount() throws DAOException;

    /**
     * Count of Editions.
     */
    Integer editionCount() throws DAOException;

    /**
     * Return most recent edition date.
     */
    Date latestUpdate() throws DAOException;


}
