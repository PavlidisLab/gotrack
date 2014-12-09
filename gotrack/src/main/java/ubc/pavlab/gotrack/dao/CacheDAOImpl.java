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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ubc.pavlab.gotrack.model.GeneSymbol;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CacheDAOImpl implements CacheDAO {

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_CURRENT_EDITIONS = "select species_id, edition from (select distinct species_id, edition from gene_annotation order by edition DESC) as temp group by species_id";
    private static final String SQL_ACCESSION_TO_SYMBOL = "select distinct symbol, accession, synonyms from gene_annotation where species_id = ? AND edition=?";
    private static final String SQL_UNIQUE_SYMBOL = "select symbol from gene_annotation where species_id = ? AND edition=?";

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
    public Map<Integer, Integer> getCurrentEditions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Integer> editions = new HashMap<Integer, Integer>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_CURRENT_EDITIONS );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                editions.put( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return editions;

    }

    @Override
    public Map<String, GeneSymbol> getAccessionToGeneSymbol( Integer species, Integer edition ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<String, GeneSymbol> results = new HashMap<String, GeneSymbol>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_ACCESSION_TO_SYMBOL, false, species, edition );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                List<String> synonyms = Arrays.asList( resultSet.getString( "synonyms" ).split( "\\|" ) );
                results.put( resultSet.getString( "accession" ), new GeneSymbol( resultSet.getString( "symbol" ),
                        synonyms ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public Collection<String> getUniqueGeneSymbols( Integer species, Integer edition ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Collection<String> results = new HashSet<String>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_UNIQUE_SYMBOL, false, species, edition );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                results.add( resultSet.getString( "symbol" ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

}
