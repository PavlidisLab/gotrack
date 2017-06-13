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

import ubc.pavlab.gotrack.model.dto.*;

import java.util.List;

/**
 * Holds methods for retrieving data that is meant to be cached.
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface CacheDAO {

    /**
     * Retrieve ordered list of edition
     */
    public List<EditionDTO> getAllEditions( int[] speciesRestrictions ) throws DAOException;

    /**
     * Retrieve data for number of genes annotated to each term or their children (GO Gene Set Sizes)
     */
    public List<AnnotationCountDTO> getGOAnnotationCounts( int[] speciesRestrictions ) throws DAOException;

    /**
     * Map of species to ordered linkedlist of editions
     */
    public List<AggregateDTO> getAggregates( int[] speciesRestrictions ) throws DAOException;

    /**
     * Retrieve current synonyms
     */
    public List<SynonymDTO> getSynonyms( int[] speciesRestrictions ) throws DAOException;

    /**
     * Retrieve all GO Terms
     */
    public List<GOTermDTO> getGoTerms() throws DAOException;

    /**
     * Retrieve all relationships between GO Terms
     */
    public List<AdjacencyDTO> getAdjacencies() throws DAOException;

    /**
     * Retrieve all alternate GO Terms
     */
    public List<AdjacencyDTO> getAlternates() throws DAOException;

    /**
     * Retrieve all evidence codes
     */
    public List<EvidenceDTO> getEvidence() throws DAOException;

    /**
     * Retrieve all accessions
     */
    public List<AccessionDTO> getAccessions( int[] speciesRestrictions ) throws DAOException;

    /**
     * Retrieve all GO editions
     */
    public List<GOEditionDTO> getAllGOEditions() throws DAOException;

    /**
     * Retrieve all definitions for most current ontology
     */
    public List<GODefinitionDTO> getGODefinitions() throws DAOException;

}
