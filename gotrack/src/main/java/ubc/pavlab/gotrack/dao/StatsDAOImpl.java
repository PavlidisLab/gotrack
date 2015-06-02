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

import static ubc.pavlab.gotrack.dao.DAOUtil.close;
import static ubc.pavlab.gotrack.dao.DAOUtil.prepareStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ubc.pavlab.gotrack.model.dto.StatsDTO;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */

public class StatsDAOImpl implements StatsDAO {

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_FIND_BY_ID = "SELECT id, species_id, symbol, count FROM track_popular_genes WHERE id = ?";
    private static final String SQL_LIST = "SELECT id, species_id, symbol, count FROM track_popular_genes";
    private static final String SQL_INCREMENT = "INSERT INTO track_popular_genes(species_id, symbol, count) values (?,?, 1) ON DUPLICATE KEY UPDATE count = count + 1";

    // Vars ---------------------------------------------------------------------------------------

    private DAOFactory daoFactory;

    // Constructors -------------------------------------------------------------------------------

    /**
     * Construct an User DAO for the given DAOFactory. Package private so that it can be constructed inside the DAO
     * package only.
     * 
     * @param daoFactory The DAOFactory to construct this User DAO for.
     */
    StatsDAOImpl( DAOFactory daoFactory ) {
        this.daoFactory = daoFactory;
    }

    // Actions ------------------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.lang.Long)
     */
    @Override
    public StatsDTO find( Long id ) throws DAOException {
        return execute( SQL_FIND_BY_ID, id );
    }

    @Override
    public List<StatsDTO> list() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<StatsDTO> list = new ArrayList<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_LIST );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                list.add( map( resultSet ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return list;
    }

    public void incrementGeneHit( Integer speciesId, String symbol ) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_INCREMENT, false, speciesId, symbol );
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

    private StatsDTO execute( String sql, Object... values ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        StatsDTO species = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, sql, false, values );
            resultSet = preparedStatement.executeQuery();
            if ( resultSet.next() ) {
                species = map( resultSet );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return species;
    }

    /**
     * Map the current row of the given ResultSet to a Species.
     * 
     * @param resultSet The ResultSet of which the current row is to be mapped to a Species.
     * @return The mapped Species from the current row of the given ResultSet.
     * @throws SQLException If something fails at database level.
     */
    private static StatsDTO map( ResultSet resultSet ) throws SQLException {

        return new StatsDTO( resultSet.getInt( "id" ), resultSet.getInt( "species_id" ),
                resultSet.getString( "symbol" ), resultSet.getInt( "count" ) );
    }

}
