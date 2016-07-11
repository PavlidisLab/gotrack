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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.Aggregate;
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
import ubc.pavlab.gotrack.model.hashkey.MultiKey;

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

    // Edition
    private static final String SQL_ALL_EDITIONS = "select species_id, edition, edition.date, go_edition_id_fk from edition order by edition";
    private static final String SQL_ALL_EDITIONS_RESTRICT = "select species_id, edition, edition.date, go_edition_id_fk from edition WHERE edition.species_id in (%s) order by edition";

    // GOEdition
    private static final String SQL_ALL_GO_EDITIONS = "SELECT id, date from go_edition";

    // directAnnotationCount & inferredAnnotationCount
    private static final String SQL_GO_ANNOTATION_COUNTS = "select species_id, edition, go_id, direct_annotation_count, inferred_annotation_count from pp_go_annotation_counts";
    private static final String SQL_GO_ANNOTATION_COUNTS_RESTRICT = "select species_id, edition, go_id, direct_annotation_count, inferred_annotation_count from pp_go_annotation_counts WHERE species_id in (%s)";

    // Aggregate
    private static final String SQL_AGGREGATE = "select species_id, edition, gene_count, avg_direct_terms_for_gene, avg_inferred_terms_for_gene, avg_inferred_genes_for_term, avg_multifunctionality, avg_direct_jaccard, avg_inferred_jaccard from pp_edition_aggregates";
    private static final String SQL_AGGREGATE_RESTRICT = "select species_id, edition, gene_count, avg_direct_terms_for_gene, avg_inferred_terms_for_gene, avg_inferred_genes_for_term, avg_multifunctionality, avg_direct_jaccard, avg_inferred_jaccard from pp_edition_aggregates WHERE species_id in (%s)";

    // Accession
    private static final String SQL_ACCESSIONS = "select distinct pp_current_genes_id, accession, sec, acindex.symbol IS NOT NULL as sp from pp_primary_accessions left join acindex using (accession) LEFT JOIN sec_ac on accession=ac";
    private static final String SQL_ACCESSIONS_RESTRICT = "select distinct pp_current_genes_id, accession, sec, acindex.symbol IS NOT NULL as sp from pp_primary_accessions inner join pp_current_genes on pp_current_genes.id = pp_primary_accessions.pp_current_genes_id left join acindex using (accession) LEFT JOIN sec_ac on accession=ac WHERE species_id in (%s)";

    // Gene
    private static final String SQL_CURRENT_GENES = "select pp_current_genes.id, species_id, symbol, synonym from pp_current_genes left join pp_synonyms on pp_current_genes.id=pp_synonyms.pp_current_genes_id order by id";
    private static final String SQL_CURRENT_GENES_RESTRICT = "select pp_current_genes.id, species_id, symbol, synonym from pp_current_genes left join pp_synonyms on pp_current_genes.id=pp_synonyms.pp_current_genes_id WHERE species_id in (%s) order by id";

    // GeneOntology and GeneOntologyTerm
    private static final String SQL_GO_TERMS = "SELECT go_edition_id_fk, go_id, name, aspect from go_term";
    private static final String SQL_GO_ADJACENCY = "select go_edition_id_fk, child, parent, relationship from go_adjacency";
    private static final String SQL_GO_ALTERNATE = "select go_edition_id_fk, alt, `primary` from go_alternate";

    // Evidence
    private static final String SQL_EVIDENCE = "SELECT id, evidence, description, category FROM evidence_categories";

    // Used to recompute directAnnotationCount & inferredAnnotationCount & Aggregate Tables
    private static final String SQL_SIMPLE_ANNOTATIONS = "select distinct go_id, pp_current_genes.id gene_id from pp_current_genes inner join pp_goa on pp_current_genes.id=pp_goa.pp_current_genes_id where species_id=? and edition = ?";

    // These are WRITE oriented -----------------------------------------------------------------

    private static final String[] SQL_WRITE_ANNOTATION_COUNTS_TABLE_CREATION = new String[] {
            "DROP TABLE IF EXISTS go_annotation_counts_new",
            "CREATE TABLE go_annotation_counts_new ( id INTEGER NOT NULL AUTO_INCREMENT, species_id INTEGER NOT NULL, edition INTEGER NOT NULL, go_id VARCHAR(10) NOT NULL, direct_annotation_count INTEGER NULL DEFAULT NULL, inferred_annotation_count INTEGER NULL DEFAULT NULL, PRIMARY KEY (id) )" };
    private static final String SQL_WRITE_ANNOTATION_COUNTS = "INSERT INTO go_annotation_counts_new (species_id, edition, go_id, direct_annotation_count, inferred_annotation_count) VALUES ";
    private static final String[] SQL_WRITE_ANNOTATION_COUNTS_TABLE_DROP_SWAP = new String[] {
            "DROP TABLE IF EXISTS go_annotation_counts_old",
            "RENAME TABLE go_annotation_counts TO go_annotation_counts_old, go_annotation_counts_new To go_annotation_counts" };

    private static final String[] SQL_WRITE_AGGREGATE_TABLE_CREATION = new String[] {
            "DROP TABLE IF EXISTS edition_aggregates_new",
            "CREATE TABLE edition_aggregates_new ( id INTEGER NOT NULL AUTO_INCREMENT, species_id INTEGER NOT NULL, edition INTEGER NOT NULL, gene_count INTEGER NOT NULL, avg_direct_terms_for_gene DOUBLE NOT NULL, avg_inferred_terms_for_gene DOUBLE NOT NULL, avg_inferred_genes_for_term DOUBLE NOT NULL, PRIMARY KEY (id) )" };

    private static final String SQL_WRITE_AGGREGATES = "INSERT INTO edition_aggregates_new(species_id, edition, gene_count, avg_direct_terms_for_gene, avg_inferred_terms_for_gene,avg_inferred_genes_for_term) VALUES(?, ?, ?, ?, ?, ?)";
    private static final String[] SQL_WRITE_AGGREGATE_TABLE_DROP_SWAP = new String[] {
            "DROP TABLE IF EXISTS edition_aggregates_old",
            "RENAME TABLE edition_aggregates TO edition_aggregates_old, edition_aggregates_new To edition_aggregates" };

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

        List<Object> params = new ArrayList<Object>();

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
    public List<GeneDTO> getCurrentGenes( int[] speciesRestrictions ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<GeneDTO> results = new ArrayList<>();

        List<Object> params = new ArrayList<Object>();

        String sql = SQL_CURRENT_GENES;
        if ( speciesRestrictions != null && speciesRestrictions.length != 0 ) {
            sql = String.format( SQL_CURRENT_GENES_RESTRICT,
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
                results.add( new GeneDTO( resultSet.getInt( "id" ), resultSet.getInt( "species_id" ),
                        resultSet.getString( "symbol" ),
                        resultSet.getString( "synonym" ) ) );

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
                results.add( new AccessionDTO( resultSet.getInt( "pp_current_genes_id" ),
                        resultSet.getString( "accession" ), resultSet.getString( "sec" ),
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
    public List<SimpleAnnotationDTO> getSimpleAnnotations( Integer speciesId, Edition ed ) throws DAOException {

        List<Object> params = new ArrayList<Object>();
        params.add( speciesId );
        params.add( ed.getEdition() );

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<SimpleAnnotationDTO> list = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_SIMPLE_ANNOTATIONS );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                list.add( new SimpleAnnotationDTO( resultSet.getString( "go_id" ), resultSet.getInt( "gene_id" ) ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return list;
    }

    // WRITE ORIENTED ***************** THESE ARE NO LONGER USED BUT ARE BEING KEPT FOR NOW... JUST IN CASE ******************

    @Override
    @Deprecated
    public void writeAnnotationCounts( Map<MultiKey, Integer> direct, Map<MultiKey, Integer> inferred ) {
        if ( direct == null || inferred == null ) {
            return;
        }
        Set<MultiKey> combinedKeyset = new HashSet<>( direct.keySet() );
        combinedKeyset.addAll( inferred.keySet() );

        createAnnotationCountsTable();

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean saveStateAutoCommit = true;
        try {
            connection = daoFactory.getConnection();
            saveStateAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit( false );

            int BATCH_SIZE = 1000;
            preparedStatement = connection
                    .prepareStatement( buildBatchInsertSQL( SQL_WRITE_ANNOTATION_COUNTS, 5, BATCH_SIZE ) );
            int i = 0;

            for ( MultiKey mk : combinedKeyset ) {
                int param_number_start = 5 * ( i % BATCH_SIZE );
                preparedStatement.setInt( param_number_start + 1, mk.getSpecies() );
                preparedStatement.setInt( param_number_start + 2, mk.getEdition() );
                preparedStatement.setString( param_number_start + 3, mk.getGoId() );

                Integer count = direct.get( mk );
                if ( count == null ) {
                    preparedStatement.setNull( param_number_start + 4, java.sql.Types.INTEGER );
                } else {
                    preparedStatement.setInt( param_number_start + 4, count );
                }

                count = inferred.get( mk );
                if ( count == null ) {
                    preparedStatement.setNull( param_number_start + 5, java.sql.Types.INTEGER );
                } else {
                    preparedStatement.setInt( param_number_start + 5, count );
                }

                //preparedStatement.addBatch();
                i++;
                if ( i % BATCH_SIZE == 0 || i == combinedKeyset.size() ) {
                    preparedStatement.execute(); // Execute every BATCH_SIZE items.
                    preparedStatement.clearParameters();
                    int itemsLeft = combinedKeyset.size() - i;
                    if ( itemsLeft < BATCH_SIZE && itemsLeft != 0 ) {
                        close( preparedStatement );
                        preparedStatement = connection
                                .prepareStatement( buildBatchInsertSQL( SQL_WRITE_ANNOTATION_COUNTS, 5, itemsLeft ) );
                    }
                }
                if ( i % 100000 == 0 || i == combinedKeyset.size() ) {
                    log.info( i + " / " + combinedKeyset.size() );
                }
            }

            connection.commit();

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            try {
                connection.setAutoCommit( saveStateAutoCommit );
            } catch ( SQLException e ) {
                log.error( e );
            }
            close( connection, preparedStatement );
        }

        dropSwapAnnotationCountsTable();
    }

    @Deprecated
    private String buildBatchInsertSQL( final String sql, int columnSize, int batchSize ) {

        final StringBuilder paramBuilder = new StringBuilder( "(" );
        for ( int i = 0; i < columnSize; i++ ) {
            if ( i != 0 ) {
                paramBuilder.append( "," );
            }
            paramBuilder.append( "?" );
        }
        final String placeholders = paramBuilder.append( ")" ).toString();

        final StringBuilder sqlBuilder = new StringBuilder( sql );

        for ( int i = 0; i < batchSize; i++ ) {
            if ( i != 0 ) {
                sqlBuilder.append( "," );
            }
            sqlBuilder.append( placeholders );
        }
        return sqlBuilder.toString();

    }

    @Deprecated
    private void createAnnotationCountsTable() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            for ( int i = 0; i < SQL_WRITE_ANNOTATION_COUNTS_TABLE_CREATION.length; i++ ) {
                String sql = SQL_WRITE_ANNOTATION_COUNTS_TABLE_CREATION[i];
                preparedStatement = connection.prepareStatement( sql );
                preparedStatement.executeUpdate();
            }

            preparedStatement.executeBatch();
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement );
        }

    }

    @Deprecated
    private void dropSwapAnnotationCountsTable() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            for ( int i = 0; i < SQL_WRITE_ANNOTATION_COUNTS_TABLE_DROP_SWAP.length; i++ ) {
                String sql = SQL_WRITE_ANNOTATION_COUNTS_TABLE_DROP_SWAP[i];
                preparedStatement = connection.prepareStatement( sql );
                preparedStatement.executeUpdate();
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement );
        }

    }

    @Override
    @Deprecated
    public void writeAggregates( Map<Integer, Map<Edition, Aggregate>> aggs ) {
        if ( aggs == null ) {
            return;
        }

        createAggregateTable();

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean saveStateAutoCommit = true;
        try {
            connection = daoFactory.getConnection();
            saveStateAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit( false );
            preparedStatement = connection.prepareStatement( SQL_WRITE_AGGREGATES );
            int total = 0;
            for ( Map<Edition, Aggregate> m : aggs.values() ) {
                total += m.size();
            }
            int i = 0;
            for ( Entry<Integer, Map<Edition, Aggregate>> e1 : aggs.entrySet() ) {
                int speciesId = e1.getKey();
                for ( Entry<Edition, Aggregate> e2 : e1.getValue().entrySet() ) {
                    int ed = e2.getKey().getEdition();
                    Aggregate a = e2.getValue();

                    preparedStatement.setInt( 1, speciesId );
                    preparedStatement.setInt( 2, ed );
                    preparedStatement.setInt( 3, a.getGeneCount() );
                    preparedStatement.setDouble( 4, a.getAvgDirectByGene() );
                    preparedStatement.setDouble( 5, a.getAvgInferredByGene() );
                    preparedStatement.setDouble( 6, a.getAvgGenesByTerm() );

                    preparedStatement.addBatch();
                    i++;
                    if ( i % 1000 == 0 || i == total ) {

                        preparedStatement.executeBatch(); // Execute every 1000 items.
                        connection.commit();
                    }
                    if ( i % 100000 == 0 || i == total ) {
                        log.info( i + " / " + total );
                    }
                }

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            try {
                connection.setAutoCommit( saveStateAutoCommit );
            } catch ( SQLException e ) {
                log.error( e );
            }
            close( connection, preparedStatement );
        }

        dropSwapAggregateTable();
    }

    @Deprecated
    private void createAggregateTable() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            for ( int i = 0; i < SQL_WRITE_AGGREGATE_TABLE_CREATION.length; i++ ) {
                String sql = SQL_WRITE_AGGREGATE_TABLE_CREATION[i];
                preparedStatement = connection.prepareStatement( sql );
                preparedStatement.executeUpdate();
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement );
        }

    }

    @Deprecated
    private void dropSwapAggregateTable() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            for ( int i = 0; i < SQL_WRITE_AGGREGATE_TABLE_DROP_SWAP.length; i++ ) {
                String sql = SQL_WRITE_AGGREGATE_TABLE_DROP_SWAP[i];
                preparedStatement = connection.prepareStatement( sql );
                preparedStatement.executeUpdate();
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement );
        }

    }

}
