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
import static ubc.pavlab.gotrack.dao.DAOUtil.prepareStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ubc.pavlab.gotrack.model.Species;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class SpeciesDAOImpl implements SpeciesDAO {

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_FIND_BY_ID = "SELECT * FROM species WHERE id = ?";
    private static final String SQL_LIST_ORDER_BY_NAME = "SELECT * FROM species ORDER BY common_name";

    // Vars ---------------------------------------------------------------------------------------

    private DAOFactory daoFactory;

    // Constructors -------------------------------------------------------------------------------

    /**
     * Construct an User DAO for the given DAOFactory. Package private so that it can be constructed inside the DAO
     * package only.
     * 
     * @param daoFactory The DAOFactory to construct this User DAO for.
     */
    SpeciesDAOImpl( DAOFactory daoFactory ) {
        this.daoFactory = daoFactory;
    }

    // Actions ------------------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.lang.Long)
     */
    @Override
    public Species find( Long id ) throws DAOException {
        return execute( SQL_FIND_BY_ID, id );
    }

    @Override
    public List<Species> list() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Species> list = new ArrayList<Species>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_LIST_ORDER_BY_NAME );
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

    private Species execute( String sql, Object... values ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Species species = null;

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
    private static Species map( ResultSet resultSet ) throws SQLException {
        Species species = new Species();

        species.setId( resultSet.getInt( "id" ) );
        species.setCommonName( resultSet.getString( "common_name" ) );
        species.setScientificName( resultSet.getString( "scientific_name" ) );

        String[] taxons = resultSet.getString( "taxon" ).split( "\\|" );
        species.setTaxon( Integer.parseInt( taxons[0].split( ":" )[1] ) );

        if ( taxons.length == 2 ) {
            species.setInteractingTaxon( Integer.parseInt( taxons[1].split( ":" )[1] ) );
        }

        return species;
    }
}
