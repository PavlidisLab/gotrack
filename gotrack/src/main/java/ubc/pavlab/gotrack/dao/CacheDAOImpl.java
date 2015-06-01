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

import static ubc.pavlab.gotrack.dao.DAOUtil.close;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.dto.AccessionDTO;
import ubc.pavlab.gotrack.model.dto.AdjacencyDTO;
import ubc.pavlab.gotrack.model.dto.AggregateDTO;
import ubc.pavlab.gotrack.model.dto.AnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.EditionDTO;
import ubc.pavlab.gotrack.model.dto.EvidenceDTO;
import ubc.pavlab.gotrack.model.dto.GOTermDTO;
import ubc.pavlab.gotrack.model.dto.GeneDTO;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CacheDAOImpl implements CacheDAO {

    private static final Logger log = Logger.getLogger( CacheDAOImpl.class );

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_CURRENT_EDITIONS = "select species_id, edition, date, go_date, go_edition_id_fk "
            + "from (select species_id, edition, edition.date, go_edition.date as go_date, edition.go_edition_id_fk "
            + "from edition INNER JOIN go_edition on edition.go_edition_id_fk=go_edition.id order by edition DESC) "
            + "as temp group by species_id";
    private static final String SQL_ALL_EDITIONS = "select species_id, edition, edition.date, go_edition.date as go_date, go_edition_id_fk "
            + "from edition left join go_edition on edition.go_edition_id_fk=go_edition.id order by edition";
    private static final String SQL_ALL_EDITIONS_RESTRICT = "select species_id, edition, edition.date, go_edition.date as go_date, go_edition_id_fk "
            + "from edition left join go_edition on edition.go_edition_id_fk=go_edition.id WHERE edition.species_id in (%s) order by edition";
    private static final String SQL_ALL_GO_SIZES = "select species_id, edition, go_id, annotation_count from goa_go_aggregate";
    private static final String SQL_ALL_GO_SIZES_RESTRICT = "select species_id, edition, go_id, annotation_count from goa_go_aggregate WHERE species_id in (%s)";
    private static final String SQL_AGGREGATE = "select aggregate.species_id, aggregate.edition, avg_directs_by_gene, count_genes, avg_directs_by_accession, count_accessions from aggregate";
    private static final String SQL_AGGREGATE_RESTRICT = "select aggregate.species_id, aggregate.edition, avg_directs_by_gene, count_genes, avg_directs_by_accession, count_accessions from aggregate WHERE aggregate.species_id in (%s)";
    private static final String SQL_GO_EDITIONS = "SELECT distinct id from go_edition";

    private static final String SQL_ACCESSIONS = "select distinct accession, sec, acindex.symbol IS NOT NULL as sp from current_genes left join acindex using (accession) LEFT JOIN sec_ac on accession=ac";
    private static final String SQL_ACCESSIONS_RESTRICT = "select distinct accession, sec, acindex.symbol IS NOT NULL as sp from current_genes left join acindex using (accession) LEFT JOIN sec_ac on accession=ac WHERE species_id in (%s)";

    private static final String SQL_CURRENT_GENES = "select species_id, symbol, accession, synonyms from current_genes";
    private static final String SQL_CURRENT_GENES_RESTRICT = "select species_id, symbol, accession, synonyms from current_genes WHERE species_id in (%s)";
    private static final String SQL_GO_TERMS2 = "Select go_ontology_tclosure.go_edition_id_fk, child go_id, name, aspect from go_ontology_tclosure inner join go_term on go_term.go_id=go_ontology_tclosure.child and go_term.go_edition_id_fk=go_ontology_tclosure.go_edition_id_fk where min_distance=0";
    private static final String SQL_GO_TERMS = "SELECT go_edition_id_fk, go_id, name, aspect from go_term";
    private static final String SQL_GO_ADJACENCY = "select go_edition_id_fk, child, parent, relationship from go_ontology_tclosure where min_distance =1";

    private static final String SQL_GO_TERMS_BY_EDITION = "Select child go_id, name, aspect from go_ontology_tclosure inner join go_term on go_term.go_id=go_ontology_tclosure.child and go_term.go_edition_id_fk=go_ontology_tclosure.go_edition_id_fk where go_ontology_tclosure.go_edition_id_fk=? and min_distance=0";
    private static final String SQL_GO_ADJACENCY_BY_EDITION = "select child, parent, relationship from go_ontology_tclosure where go_ontology_tclosure.go_edition_id_fk=? and min_distance =1";

    // Evidence
    private static final String SQL_EVIDENCE = "SELECT id, evidence, description, category FROM evidence_categories";

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
    public List<AnnotationCountDTO> getGOSizes( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AnnotationCountDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<Object>();

        String sql = SQL_ALL_GO_SIZES;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_ALL_GO_SIZES_RESTRICT, DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
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
                        resultSet.getString( "go_id" ), resultSet.getInt( "annotation_count" ) ) );

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
        List<Object> params = new ArrayList<Object>();

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
                results.add( new EditionDTO( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ), resultSet
                        .getDate( "date" ), resultSet.getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) ) );

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

        List<Object> params = new ArrayList<Object>();

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
                aggregates
                        .add( new AggregateDTO( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ),
                                resultSet.getInt( "count_accessions" ), resultSet.getInt( "count_genes" ), resultSet
                                        .getDouble( "avg_directs_by_accession" ), resultSet
                                        .getDouble( "avg_directs_by_gene" ) ) );
            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return aggregates;
    }

    @Override
    public List<GeneDTO> getCurrentGenes( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<GeneDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<Object>();

        String sql = SQL_CURRENT_GENES;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_CURRENT_GENES_RESTRICT, DAOUtil.preparePlaceHolders( speciesRestrictions.length ) );
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
                results.add( new GeneDTO( resultSet.getInt( "species_id" ), resultSet.getString( "symbol" ), resultSet
                        .getString( "synonyms" ), resultSet.getString( "accession" ) ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public List<AccessionDTO> getAccessions( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<AccessionDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<Object>();

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
                results.add( new AccessionDTO( resultSet.getString( "accession" ), resultSet.getString( "sec" ),
                        resultSet.getBoolean( "sp" ) ) );

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
                list.add( new EvidenceDTO( resultSet.getInt( "id" ), resultSet.getString( "evidence" ), resultSet
                        .getString( "description" ), resultSet.getString( "category" ) ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return list;
    }
}
