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

import com.google.common.collect.Lists;
import ubc.pavlab.gotrack.model.dto.GeneStatsDTO;
import ubc.pavlab.gotrack.model.dto.TermStatsDTO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ubc.pavlab.gotrack.dao.DAOUtil.close;
import static ubc.pavlab.gotrack.dao.DAOUtil.prepareStatement;

/**
 *
 * @author mjacobson
 */

public class StatsDAOImpl implements StatsDAO {

    // Constants ----------------------------------------------------------------------------------
    private static final String SQL_TRACK_GENE = "track_popular_genes";
    private static final String SQL_TRACK_TERM = "track_popular_terms";
    private static final String SQL_ACCESSION_HISTORY = "pp_accession_history";
    private static final String SQL_ACCESSION = "accession";
    private static final String SQL_ANNOTATION = "annotation";
    private static final String SQL_TERM = "go_term";
    private static final String SQL_ADJACENCY = "go_adjacency";
    private static final String SQL_EDITION = "edition";
    private static final String SQL_GO_EDITION = "go_edition";
    private static final String SQL_CURRENT_EDITION = "pp_current_edition";


    private static final String SQL_LIST_GENE = "SELECT tg.id, species_id, IFNULL(ac, accession) primary_accession, symbol, SUM(count) count FROM " + SQL_TRACK_GENE + " tg " +
            "left join " + SQL_ACCESSION_HISTORY + " on accession=sec GROUP BY species_id, primary_accession";
    private static final String SQL_LIST_TERM = "SELECT id, go_id, count FROM " + SQL_TRACK_TERM;

    private static final String SQL_ROW_COUNT = "SELECT table_rows FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";

    private static final String SQL_LAST_UPDATED = "select max(date) last_updated from " + SQL_EDITION + " inner join " + SQL_CURRENT_EDITION + " using (species_id, edition)";

    private static final String SQL_SPECIES_COUNT = "select COUNT(distinct species_id) species_count from " + SQL_ACCESSION;

    private static final String SQL_YEARS_AVAILABLE = "select ROUND(TIMESTAMPDIFF(MONTH, min(date), max(date))/12) years from " + SQL_EDITION;

    private static final String SQL_GO_EDITION_COUNT = "select COUNT(*) from " + SQL_GO_EDITION;
    private static final String SQL_EDITION_COUNT = "select COUNT(*) from " + SQL_EDITION;

    private static final String SQL_INCREMENT_GENE = "INSERT INTO " + SQL_TRACK_GENE + "(species_id, accession, symbol, count) values (?,?,?,1) ON DUPLICATE KEY UPDATE count = count + 1";
    private static final String SQL_INCREMENT_TERM = "INSERT INTO " + SQL_TRACK_TERM + "(go_id, count) values (?,1) ON DUPLICATE KEY UPDATE count = count + 1";


    // Vars ---------------------------------------------------------------------------------------

    private DAOFactory daoFactory;

    // Constructors -------------------------------------------------------------------------------

    /**
     * Construct an Stats DAO for the given DAOFactory. Package private so that it can be constructed inside the DAO
     * package only.
     * 
     * @param daoFactory The DAOFactory to construct this User DAO for.
     */
    StatsDAOImpl( DAOFactory daoFactory ) {
        this.daoFactory = daoFactory;
    }

    // Actions ------------------------------------------------------------------------------------

    @Override
    public List<GeneStatsDTO> listGenes() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<GeneStatsDTO> list = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_LIST_GENE );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                list.add( geneMap( resultSet ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return list;
    }

    @Override
    public List<TermStatsDTO> listTerms() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<TermStatsDTO> list = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_LIST_TERM );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                list.add( termMap( resultSet ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return list;
    }

    @Override
    public Integer annotationCount() throws DAOException {
        return ((BigInteger) fetchSingle( SQL_ROW_COUNT, SQL_ANNOTATION )).intValue();
    }

    @Override
    public Integer goCount() throws DAOException {
        return ((BigInteger) fetchSingle( SQL_ROW_COUNT, SQL_TERM )).intValue();
    }

    @Override
    public Integer adjacencyCount() throws DAOException {
        return ((BigInteger) fetchSingle( SQL_ROW_COUNT, SQL_ADJACENCY )).intValue();
    }

    @Override
    public Date latestUpdate() throws DAOException {
        return (Date) fetchSingle( SQL_LAST_UPDATED );
    }

    @Override
    public Integer speciesCount() throws DAOException {
        return ((Long) fetchSingle( SQL_SPECIES_COUNT )).intValue();
    }

    @Override
    public Integer yearsAvailable() throws DAOException {
        return ((BigDecimal) fetchSingle( SQL_YEARS_AVAILABLE )).intValue();
    }

    @Override
    public Integer goEditionCount() throws DAOException {
        return ((Long) fetchSingle( SQL_GO_EDITION_COUNT )).intValue();
    }

    @Override
    public Integer editionCount() throws DAOException {
        return ((Long) fetchSingle( SQL_EDITION_COUNT )).intValue();
    }

    private Object fetchSingle(String sql ) throws DAOException {
        return fetchSingle( sql, Lists.newArrayList() );
    }

    private Object fetchSingle(String sql, Object param) throws DAOException {
        return fetchSingle( sql, Lists.newArrayList(param) );
    }

    private Object fetchSingle(String sql, List<Object> params) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Object result = null;
        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( sql );
            DAOUtil.setValues( preparedStatement, params.toArray() );
            resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                result = resultSet.getObject( 1 );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return result;
    }

    @Override
    public void incrementGeneHit( Integer speciesId, String accession, String symbol ) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_INCREMENT_GENE, false, speciesId, accession, symbol );
            int result = preparedStatement.executeUpdate();
            if ( result == 0 ) {
                throw new SQLException( "Something went wrong in the popular genes update!" );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, null );
        }
    }

    @Override
    public void incrementTermHit( String goId ) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_INCREMENT_TERM, false, goId );
            int result = preparedStatement.executeUpdate();
            if ( result == 0 ) {
                throw new SQLException( "Something went wrong in the popular terms update!" );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, null );
        }
    }

    private GeneStatsDTO execute( String sql, Object... values ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        GeneStatsDTO species = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, sql, false, values );
            resultSet = preparedStatement.executeQuery();
            if ( resultSet.next() ) {
                species = geneMap( resultSet );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return species;
    }

    private static GeneStatsDTO geneMap( ResultSet resultSet ) throws SQLException {

        return new GeneStatsDTO( resultSet.getInt( "id" ), resultSet.getInt( "species_id" ), resultSet.getString( "primary_accession" ),
                resultSet.getString( "symbol" ), resultSet.getInt( "count" ) );
    }

    private static TermStatsDTO termMap( ResultSet resultSet ) throws SQLException {

        return new TermStatsDTO( resultSet.getInt( "id" ), resultSet.getString( "go_id" ),
                resultSet.getInt( "count" ) );
    }

}
