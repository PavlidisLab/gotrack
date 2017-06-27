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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ubc.pavlab.gotrack.dao.DAOUtil.close;
import static ubc.pavlab.gotrack.dao.DAOUtil.prepareStatement;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */

public class StatsDAOImpl implements StatsDAO {

    // Constants ----------------------------------------------------------------------------------
    private static final String SQL_TRACK_GENE = "track_popular_genes";
    private static final String SQL_TRACK_TERM = "track_popular_terms";
    private static final String SQL_SEC_AC = "sec_ac";

    private static final String SQL_LIST_GENE = "SELECT tg.id, species_id, IFNULL(ac, accession) primary_accession, symbol, SUM(count) count FROM " + SQL_TRACK_GENE + " tg left join " + SQL_SEC_AC + " on accession=sec GROUP BY species_id, primary_accession";
    private static final String SQL_LIST_TERM = "SELECT id, go_id, count FROM " + SQL_TRACK_TERM;
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
