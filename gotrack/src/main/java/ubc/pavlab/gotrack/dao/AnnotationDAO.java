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

import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Tuples;

import java.sql.Date;
import java.util.List;
import java.util.Set;

/**
 * This interface represents a contract for {@link AnnotationDAOImpl}. Note that all methods are
 * read-only.
 */
public interface AnnotationDAO {
    // Actions ------------------------------------------------------------------------------------
    /**
     * Retrieve data necessary for enrichment of given set of genes where @minimum <= edition <= @maximum
     */
    List<EnrichmentDTO> simpleAnnotationRangeEditions( Set<Gene> genes, Integer minEdition, Integer maxEdition ) throws DAOException;

    /**
     * Retrieves counts of unique annotations grouped by evidence category over time for a specific GO Id
     * where date falls between @min and @max
     */
    List<EvidenceCountDTO> categoryCountsRangeDates( String goId, Date min, Date max ) throws DAOException;

    /**
     * Retrieves counts of unique annotations grouped by evidence category over time for a specific GO Id and species
     * where where @minimum <= edition <= @maximum
     */
    List<EditionEvidenceCountDTO> categoryCountsSingleSpeciesRangeEditions( String goId, Species species, Integer minEdition, Integer maxEdition ) throws DAOException;

    /**
     * Retrieve data necessary for tracking a specific gene over time where @minimum <= edition <= @maximum
     */
    List<AnnotationDTO> fullAnnotationRangeEditions( Gene g, Integer minimum, Integer maximum ) throws DAOException;

    /**
     * Retrieve data necessary for enrichment of given set of genes in a given edition
     */
    List<SimpleAnnotationDTO> simpleAnnotationSingleEdition( Edition ed, Set<Gene> genes ) throws DAOException;

    List<Tuples.Tuple3<Integer, String, Boolean>> inferredGenesRangeEditions( GeneOntologyTerm term, Species species, Integer minEdition, Integer maxEdition ) throws DAOException;

    List<Tuples.Tuple2<String, Boolean>> inferredGenesSingleEdition( GeneOntologyTerm term, Edition edition ) throws DAOException;

    List<String> directGenesSingleEdition( GeneOntologyTerm term, Edition edition ) throws DAOException;

    /**
     * Retrieve data necessary for bulk download of a single edition - terms only
     */
    List<Tuples.Tuple2<String,String>> simpleAnnotationSingleEditionCompleteSpecies( Species species, Edition edition ) throws DAOException;
}