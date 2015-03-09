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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CacheDAOImpl implements CacheDAO {

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_CURRENT_EDITIONS = "select species_id, edition, date, go_date, go_edition_id_fk "
            + "from (select species_id, edition, edition.date, go_edition.date as go_date, edition.go_edition_id_fk "
            + "from edition INNER JOIN go_edition on edition.go_edition_id_fk=go_edition.id order by edition DESC) "
            + "as temp group by species_id";
    private static final String SQL_ALL_EDITIONS = "select species_id, edition, edition.date, go_edition.date as go_date, go_edition_id_fk "
            + "from edition inner join go_edition on edition.go_edition_id_fk=go_edition.id order by edition";
    private static final String SQL_CURRENT_ACCESSIONS = "select distinct symbol, accession, synonyms, sec from gene_annotation LEFT JOIN sec_ac on accession=ac where species_id = ? AND edition=?";
    private static final String SQL_UNIQUE_SYMBOL = "select distinct symbol from gene_annotation where species_id = ? AND edition=?";
    private static final String SQL_SPECIES_AVERAGES = "select aggregate.species_id, aggregate.edition, date, avg_direct "
            + "from aggregate inner join edition on aggregate.species_id=edition.species_id and aggregate.edition = edition.edition";
    private static final String SQL_GO_ACCESSION_SIZES = "select go_id, COUNT(distinct accession) as count from gene_annotation where species_id=? and edition=? group by go_id having count > ? order by null";
    private static final String SQL_GO_SYMBOL_SIZES = "select go_id, COUNT(distinct symbol) as count from gene_annotation where species_id=? and edition=? group by go_id having count > ? order by null";
    private static final String SQL_ALL_GO_SIZES = "select edition, go_id, COUNT(distinct accession) as count from gene_annotation where species_id=? group by edition, go_id having count > ? order by null";
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
    public Map<String, Integer> getGOSizes( Integer speciesId, Integer edition, int minimum, boolean useSymbols )
            throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<String, Integer> goTerms = new HashMap<String, Integer>();

        try {
            connection = daoFactory.getConnection();
            String sql = useSymbols ? SQL_GO_SYMBOL_SIZES : SQL_GO_ACCESSION_SIZES;
            preparedStatement = prepareStatement( connection,
                    useSymbols ? SQL_GO_SYMBOL_SIZES : SQL_GO_ACCESSION_SIZES, false, speciesId, edition, minimum );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                goTerms.put( resultSet.getString( "go_id" ), resultSet.getInt( "count" ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return goTerms;

    }

    @Override
    public Map<Integer, Edition> getCurrentEditions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Edition> editions = new HashMap<Integer, Edition>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_CURRENT_EDITIONS );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                editions.put(
                        resultSet.getInt( "species_id" ),
                        new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ), resultSet
                                .getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return editions;

    }

    @Override
    public Map<Integer, LinkedList<Edition>> getAllEditions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, LinkedList<Edition>> results = new HashMap<Integer, LinkedList<Edition>>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_ALL_EDITIONS );
            resultSet = preparedStatement.executeQuery();

            while ( resultSet.next() ) {
                int speciesId = resultSet.getInt( "species_id" );
                Edition edition = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) );
                LinkedList<Edition> editionsInSpecies = results.get( speciesId );

                if ( editionsInSpecies == null ) {
                    editionsInSpecies = new LinkedList<Edition>();
                    results.put( speciesId, editionsInSpecies );
                }

                editionsInSpecies.add( edition );

            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;

    }

    @Override
    public Map<String, Accession> getCurrentAccessions( Integer species, Integer edition ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<String, Accession> accessions = new HashMap<String, Accession>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_CURRENT_ACCESSIONS, false, species, edition );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                String accession = resultSet.getString( "accession" );
                Accession acc = accessions.get( accession );
                if ( acc == null ) {
                    acc = new Accession( accession );
                    List<String> synonyms = Arrays.asList( resultSet.getString( "synonyms" ).split( "\\|" ) );
                    acc.setSymbol( resultSet.getString( "symbol" ) );
                    acc.setSynonyms( synonyms );

                    String sec = resultSet.getString( "sec" );
                    if ( !resultSet.wasNull() ) {
                        // Null secondary values means that it was not found in sec_ac primary column and therefore has
                        // no secondary accessions
                        acc.addSecondary( sec );
                    }

                    accessions.put( acc.getAccession(), acc );
                } else {
                    acc.addSecondary( resultSet.getString( "sec" ) );
                }

            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return accessions;
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

    @Override
    public Map<Integer, Map<Edition, Double>> getSpeciesAverages() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Map<Edition, Double>> results = new HashMap<Integer, Map<Edition, Double>>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, SQL_SPECIES_AVERAGES, false );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                int speciesId = resultSet.getInt( "species_id" );
                Edition edition = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ) );
                double avg = resultSet.getDouble( "avg_direct" );
                Map<Edition, Double> speciesMap = results.get( speciesId );

                if ( speciesMap == null ) {
                    speciesMap = new HashMap<Edition, Double>();
                    results.put( speciesId, speciesMap );
                }

                speciesMap.put( edition, avg );

            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }
}
