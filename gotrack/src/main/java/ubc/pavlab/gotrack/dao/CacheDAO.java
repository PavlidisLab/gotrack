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
import java.util.List;
import java.util.Map;
import java.util.Set;

import ubc.pavlab.gotrack.go.GeneOntology;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Relationship;
import ubc.pavlab.gotrack.model.StatsEntry;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface CacheDAO {

    public Map<Integer, Edition> getCurrentEditions() throws DAOException;

    public Map<String, Accession> getAccessions( Integer species, Integer edition ) throws DAOException;

    public Collection<String> getUniqueGeneSymbols( Integer species, Integer edition ) throws DAOException;

    public Map<Integer, Map<Edition, Double>> getSpeciesAverages() throws DAOException;

    /**
     * @return Map of species to ordered linkedlist of editions
     * @throws DAOException
     */
    public Map<Integer, List<Edition>> getAllEditions() throws DAOException;

    public Map<Integer, Map<Integer, Map<String, Integer>>> getGOSizes( Integer speciesId, int minimum )
            throws DAOException;

    public Map<Integer, Integer> getAccessionSizes( Integer speciesId ) throws DAOException;

    public Map<Integer, Map<Integer, Map<String, Integer>>> getGOSizesFromPrecompute() throws DAOException;

    public Map<Integer, Map<Edition, StatsEntry>> getAggregates() throws DAOException;

    public Set<Relationship> getAdjacencyList( int go_edition_id_fk ) throws DAOException;

    public Set<Integer> getGOEditions() throws DAOException;

    public Map<Integer, GeneOntology> getOntologies() throws DAOException;

    public Map<Integer, Set<Relationship>> getOntologies( Collection<Integer> range ) throws DAOException;

    public Map<String, String> getEvidenceCategories() throws DAOException;

}
