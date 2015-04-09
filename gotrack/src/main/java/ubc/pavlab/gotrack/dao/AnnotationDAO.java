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

import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationDetailed;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * This interface represents a contract for a DAO for the {@link AnnotationDetailed} model. Note that all methods are
 * read-only.
 */
public interface AnnotationDAO {

    // Actions ------------------------------------------------------------------------------------

    /**
     * Returns the annotation from the database matching the given ID, otherwise null.
     * 
     * @param id The ID of the annotation to be returned.
     * @return The annotation from the database matching the given ID, otherwise null.
     * @throws DAOException If something fails at database level.
     */
    public AnnotationDetailed find( Long id ) throws DAOException;

    /**
     * Returns the annotations from the database matching the given accession, edition and species, otherwise null.
     * 
     * @param accession The accession of the annotations to be returned.
     * @param edition The edition of the annotations to be returned.
     * @param species The species of the annotations to be returned.
     * @return The annotations from the database matching the given email and password, otherwise null.
     * @throws DAOException If something fails at database level.
     */
    public List<AnnotationDetailed> find( String accession, Integer edition, Integer species ) throws DAOException;

    /**
     * Returns the annotations from the database matching the given accessions, edition and species, otherwise null.
     * 
     * @param accession The accession of the annotations to be returned.
     * @param edition The edition of the annotations to be returned.
     * @param species The species of the annotations to be returned.
     * @return The annotations from the database matching the given email and password, otherwise null.
     * @throws DAOException If something fails at database level.
     */
    public List<AnnotationDetailed> find( List<String> accession, Integer edition, Integer species )
            throws DAOException;

    /**
     * Returns true if the given symbol exists in the database.
     * 
     * @param symbol The symbol which is to be checked in the database.
     * @return True if the given symbol exists in the database.
     * @throws DAOException If something fails at database level.
     */
    public boolean existSymbol( String symbol ) throws DAOException;

    public Map<String, Map<Edition, Set<Annotation>>> trackOld( Integer species,
            Map<String, Collection<String>> primaryToSecondary, Integer goEditionId, boolean propagate )
            throws DAOException;

    public Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> enrichmentData( Integer species, Set<Gene> genes )
            throws DAOException;

    public Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> enrichmentDataOld( Integer species, Set<Gene> genes,
            Integer currentEdition ) throws DAOException;

    public Map<String, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> trackOld2( Integer species,
            String symbol, Integer edition, Integer goEditionId ) throws DAOException;

    public Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> track( Integer species,
            String symbol ) throws DAOException;

}