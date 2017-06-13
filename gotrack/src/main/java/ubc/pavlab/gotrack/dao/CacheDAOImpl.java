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

import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.model.dto.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ubc.pavlab.gotrack.dao.DAOUtil.close;

/**
 * Holds methods for retrieving data that is meant to be cached creating dumps of pre-processed data for faster cache
 * creation.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CacheDAOImpl implements CacheDAO {

    private static final Logger log = Logger.getLogger( CacheDAOImpl.class );

    // Constants ----------------------------------------------------------------------------------

    /*
     * private static final String SQL_CURRENT_EDITIONS = "select species_id, edition, date, go_date, go_edition_id_fk "
     * + "from (select species_id, edition, edition.date, go_edition.date as go_date, edition.go_edition_id_fk "
     * + "from edition INNER JOIN go_edition on edition.go_edition_id_fk=go_edition.id order by edition DESC) "
     * + "as temp group by species_id";
     */

    // Creating caches of models ------------------------------------------------------------------

    private static final String SQL_ACCESSION = "accession";
    private static final String SQL_CURRENT_EDITIONS = "staging_pp_current_edition";
    private static final String SQL_SYNONYM = "synonyms";
    private static final String SQL_EDITION = "edition_tmp";
    private static final String SQL_GO_EDITION = "go_edition";
    private static final String SQL_ANNOTATION_COUNT = "staging_pp_go_annotation_counts";
    private static final String SQL_EDITION_AGGREGATE = "staging_pp_edition_aggregates";

    private static final String SQL_TERM = "go_term";
    private static final String SQL_ADJACENCY = "go_adjacency";
    private static final String SQL_DEFINITION = "go_definition";
    private static final String SQL_ALTERNATE = "go_alternate";
    private static final String SQL_EVIDENCE_CATEGORY = "evidence_categories";

    // Edition
    private static final String SQL_ALL_EDITIONS = "SELECT species_id, edition, date, go_edition_id_fk FROM " + SQL_EDITION + " ORDER BY edition";
    private static final String SQL_ALL_EDITIONS_RESTRICT = "SELECT species_id, edition, date, go_edition_id_fk FROM " + SQL_EDITION + " WHERE species_id IN (%s) ORDER BY edition";

    // GOEdition
    private static final String SQL_ALL_GO_EDITIONS = "SELECT id, date from " + SQL_GO_EDITION + " ORDER BY date";

    // directAnnotationCount & inferredAnnotationCount
    private static final String SQL_GO_ANNOTATION_COUNTS = "select species_id, edition, go_id, direct_annotation_count, inferred_annotation_count from " + SQL_ANNOTATION_COUNT;
    private static final String SQL_GO_ANNOTATION_COUNTS_RESTRICT = "select species_id, edition, go_id, direct_annotation_count, inferred_annotation_count from " + SQL_ANNOTATION_COUNT + " WHERE species_id in (%s)";

    // Aggregate
    private static final String SQL_AGGREGATE = "select species_id, edition, gene_count, avg_direct_terms_for_gene, avg_inferred_terms_for_gene, avg_inferred_genes_for_term, avg_multifunctionality, avg_direct_jaccard, avg_inferred_jaccard from " + SQL_EDITION_AGGREGATE;
    private static final String SQL_AGGREGATE_RESTRICT = "select species_id, edition, gene_count, avg_direct_terms_for_gene, avg_inferred_terms_for_gene, avg_inferred_genes_for_term, avg_multifunctionality, avg_direct_jaccard, avg_inferred_jaccard from " + SQL_EDITION_AGGREGATE + " WHERE species_id in (%s)";

    // Accession
    private static final String SQL_ACCESSIONS = "SELECT acc.id, species_id, edition, db_object_id as accession, symbol, db_object_name as name, subset FROM " + SQL_ACCESSION + " acc INNER JOIN " + SQL_CURRENT_EDITIONS + " ce USING (species_id, edition)";
    private static final String SQL_ACCESSIONS_RESTRICT = "SELECT acc.id, species_id, edition, db_object_id as accession, symbol, db_object_name as name, subset FROM " + SQL_ACCESSION + " acc INNER JOIN " + SQL_CURRENT_EDITIONS + " ce USING (species_id, edition) WHERE species_id in (%s)";

    // Synonyms
    private static final String SQL_SYNONYMS = "SELECT acc.id, syn.synonym FROM " + SQL_ACCESSION + " acc INNER JOIN " + SQL_CURRENT_EDITIONS + " ce USING (species_id, edition) INNER JOIN " + SQL_SYNONYM + " syn on acc.id=syn.accession_id";
    private static final String SQL_SYNONYMS_RESTRICTED = "SELECT acc.id, syn.synonym FROM " + SQL_ACCESSION + " acc INNER JOIN " + SQL_CURRENT_EDITIONS + " ce USING (species_id, edition) INNER JOIN " + SQL_SYNONYM + " syn on acc.id=syn.accession_id WHERE species_id IN (%s)";



    // GeneOntology and GeneOntologyTerm
    private static final String SQL_GO_TERMS = "SELECT go_edition_id_fk, go_id, name, aspect from " + SQL_TERM;
    private static final String SQL_GO_ADJACENCY = "select go_edition_id_fk, child, parent, relationship from " + SQL_ADJACENCY;
    private static final String SQL_GO_ALTERNATE = "select go_edition_id_fk, alt, `primary` from " + SQL_ALTERNATE;
    private static final String SQL_GO_DEFINITION = "select go_id, definition from " + SQL_DEFINITION;

    // Evidence
    private static final String SQL_EVIDENCE = "SELECT id, evidence, description, category FROM " + SQL_EVIDENCE_CATEGORY;

    // Vars ---------------------------------------------------------------------------------------

    private DAOFactory daoFactory;

    // Constructors -------------------------------------------------------------------------------

    /**
     * Construct an Cache DAO for the given DAOFactory. Package private so that it can be constructed inside the DAO
     * package only.
     * 
     * @param daoFactory The DAOFactory to construct this User DAO for.
     */
    CacheDAOImpl( DAOFactory daoFactory ) {
        this.daoFactory = daoFactory;
    }

    // Actions ------------------------------------------------------------------------------------

    @Override
    public List<AnnotationCountDTO> getGOAnnotationCounts( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AnnotationCountDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<>();

        String sql = SQL_GO_ANNOTATION_COUNTS;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_GO_ANNOTATION_COUNTS_RESTRICT,
                    DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
            for ( int i = 0; i < speciesRestrictions.length; i++ ) {
                params.add( speciesRestrictions[i] );
            }
        }

        log.debug( sql );
        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {

                results.add( new AnnotationCountDTO( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ),
                        resultSet.getString( "go_id" ), resultSet.getInt( "direct_annotation_count" ),
                        resultSet.getInt( "inferred_annotation_count" ) ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;

    }

    @Override
    public List<GOEditionDTO> getAllGOEditions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<GOEditionDTO> results = new ArrayList<>();

        String sql = SQL_ALL_GO_EDITIONS;

        log.debug( sql );
        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();

            while ( resultSet.next() ) {
                results.add( new GOEditionDTO( resultSet.getInt( "id" ), resultSet.getDate( "date" ) ) );

            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<EditionDTO> getAllEditions( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<EditionDTO> results = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String sql = SQL_ALL_EDITIONS;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_ALL_EDITIONS_RESTRICT, DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
            for ( int i = 0; i < speciesRestrictions.length; i++ ) {
                params.add( speciesRestrictions[i] );
            }
        }

        log.debug( sql );
        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();

            while ( resultSet.next() ) {
                results.add( new EditionDTO( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ),
                        resultSet.getDate( "date" ), resultSet.getInt( "go_edition_id_fk" ) ) );

            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;

    }

    @Override
    public List<AggregateDTO> getAggregates( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AggregateDTO> aggregates = new ArrayList<>();

        List<Object> params = new ArrayList<>();

        String sql = SQL_AGGREGATE;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_AGGREGATE_RESTRICT, DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
            for ( int i = 0; i < speciesRestrictions.length; i++ ) {
                params.add( speciesRestrictions[i] );
            }
        }

        log.debug( sql );

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                aggregates.add( new AggregateDTO( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ),
                        resultSet.getInt( "gene_count" ), resultSet.getDouble( "avg_direct_terms_for_gene" ),
                        resultSet.getDouble( "avg_inferred_terms_for_gene" ),
                        resultSet.getDouble( "avg_inferred_genes_for_term" ),
                        resultSet.getDouble( "avg_multifunctionality" ),
                        resultSet.getDouble( "avg_direct_jaccard" ),
                        resultSet.getDouble( "avg_inferred_jaccard" ) ) );
            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return aggregates;
    }

    @Override
    public List<AccessionDTO> getAccessions( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AccessionDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<>();

        String sql = SQL_ACCESSIONS;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_ACCESSIONS_RESTRICT, DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
            for ( int i = 0; i < speciesRestrictions.length; i++ ) {
                params.add( speciesRestrictions[i] );
            }
        }

        log.debug( sql );
        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                results.add( new AccessionDTO( resultSet.getInt( "id" ), resultSet.getInt( "species_id" ),
                        resultSet.getInt( "edition" ), resultSet.getString( "accession" ),
                        resultSet.getString( "symbol" ), resultSet.getString( "name" ),
                        resultSet.getString( "subset" ) ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<SynonymDTO> getSynonyms( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<SynonymDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<>();

        String sql = SQL_SYNONYMS;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_SYNONYMS_RESTRICTED, DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
            for ( int i = 0; i < speciesRestrictions.length; i++ ) {
                params.add( speciesRestrictions[i] );
            }
        }

        log.debug( sql );
        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                results.add( new SynonymDTO( resultSet.getInt( "id" ), resultSet.getString( "synonym" ) ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<GOTermDTO> getGoTerms() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<GOTermDTO> results = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_TERMS );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_edition_id_fk, go_id, name, aspect
                results.add( new GOTermDTO( resultSet.getInt( "go_edition_id_fk" ), resultSet.getString( "go_id" ),
                        resultSet.getString( "name" ), resultSet.getString( "aspect" ) ) );
            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<AdjacencyDTO> getAdjacencies() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AdjacencyDTO> results = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_ADJACENCY );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_edition_id_fk, child, parent, relationship
                results.add( new AdjacencyDTO( resultSet.getInt( "go_edition_id_fk" ), resultSet.getString( "child" ),
                        resultSet.getString( "parent" ), resultSet.getString( "relationship" ) ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<AdjacencyDTO> getAlternates() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AdjacencyDTO> results = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_ALTERNATE );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_edition_id_fk, alt, primary
                results.add( new AdjacencyDTO( resultSet.getInt( "go_edition_id_fk" ), resultSet.getString( "alt" ),
                        resultSet.getString( "primary" ), null ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<EvidenceDTO> getEvidence() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<EvidenceDTO> list = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_EVIDENCE );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                list.add( new EvidenceDTO( resultSet.getInt( "id" ), resultSet.getString( "evidence" ),
                        resultSet.getString( "description" ), resultSet.getString( "category" ) ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return list;
    }

    @Override
    public List<GODefinitionDTO> getGODefinitions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<GODefinitionDTO> results = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_DEFINITION );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_edition_id_fk, alt, primary
                results.add(
                        new GODefinitionDTO( resultSet.getString( "go_id" ), resultSet.getString( "definition" ) ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

}
