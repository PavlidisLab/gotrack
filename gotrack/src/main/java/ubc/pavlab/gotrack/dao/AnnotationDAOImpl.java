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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.TrackValue;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AnnotationDAOImpl implements AnnotationDAO {

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_FIND_BY_ID = "SELECT edition, species_id, accession, symbol, go_id, reference, evidence, db_object_name, synonyms, db_object_type, taxon FROM gene_annotation WHERE id = ?";
    private static final String SQL_FIND = "SELECT edition, species_id, accession, symbol, go_id, reference, evidence, db_object_name, synonyms, db_object_type, taxon FROM gene_annotation WHERE accession = ? AND edition = ? AND species_id = ?";
    private static final String SQL_FIND_LIST = "SELECT edition, species_id, accession, symbol, go_id, reference, evidence, db_object_name, synonyms, db_object_type, taxon FROM gene_annotation WHERE accession IN (%s) AND edition = ? AND species_id = ?";
    private static final String SQL_EXIST_SYMBOL = "SELECT COUNT(*) FROM gene_annotation WHERE symbol = ?";
    private static final String SQL_TRACK2 = "select date, gene_annotation.edition, accession, COUNT(distinct gene_annotation.go_id) as direct from gene_annotation INNER JOIN edition on edition.edition=gene_annotation.edition AND edition.species_id = ? where gene_annotation.species_id=? and accession IN (%s) GROUP BY gene_annotation.edition, accession";
    private static final String SQL_TRACK = "select date, gene_annotation.edition, CASE %s ELSE accession END as `primary`, COUNT(distinct gene_annotation.go_id) as direct from gene_annotation INNER JOIN edition on edition.edition=gene_annotation.edition AND edition.species_id = ? where edition.species_id=? and accession IN (%s) GROUP BY gene_annotation.edition, `primary`";
    private static final String SQL_TRACK_WHEN_THEN = "WHEN accession IN (%s) THEN ? ";

    // Vars ---------------------------------------------------------------------------------------

    private DAOFactory daoFactory;

    // Constructors -------------------------------------------------------------------------------

    /**
     * Construct an User DAO for the given DAOFactory. Package private so that it can be constructed inside the DAO
     * package only.
     * 
     * @param daoFactory The DAOFactory to construct this User DAO for.
     */
    AnnotationDAOImpl( DAOFactory daoFactory ) {
        this.daoFactory = daoFactory;
    }

    // Actions ------------------------------------------------------------------------------------

    public static String preparePlaceHolders( int length ) {
        StringBuilder builder = new StringBuilder( length * 2 - 1 );
        for ( int i = 0; i < length; i++ ) {
            if ( i > 0 ) builder.append( ',' );
            builder.append( '?' );
        }
        return builder.toString();
    }

    public static void setValues( PreparedStatement preparedStatement, Object... values ) throws SQLException {
        for ( int i = 0; i < values.length; i++ ) {
            preparedStatement.setObject( i + 1, values[i] );
        }
    }

    @Override
    public Map<Integer, List<TrackValue>> trackCounts( Integer species,
            Map<String, Collection<String>> primaryToSecondary ) throws DAOException {

        List<Object> params = new ArrayList<Object>();
        List<String> allAccessions = new ArrayList<String>();

        // create WHEN THEN strings for CASE END for those primaries with secondary accessions
        String sql_when = "";
        for ( Entry<String, Collection<String>> e : primaryToSecondary.entrySet() ) {
            String primary = e.getKey();
            Collection<String> secs = e.getValue();
            allAccessions.add( primary );
            if ( secs.size() > 0 ) {
                sql_when += String.format( SQL_TRACK_WHEN_THEN, preparePlaceHolders( secs.size() + 1 ) );
                params.add( primary );
                params.addAll( secs );
                params.add( primary );
                allAccessions.addAll( secs );
            }

        }
        System.out.println( sql_when );

        params.add( species );
        params.add( species );
        for ( String a : allAccessions ) {
            params.add( a );
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        // List<TrackValue> tvs = new ArrayList<TrackValue>();
        Map<Integer, List<TrackValue>> tvsMap = new HashMap<Integer, List<TrackValue>>();
        String sql = String.format( SQL_TRACK, sql_when, preparePlaceHolders( allAccessions.size() ) );

        System.out.println( sql );

        try {
            connection = daoFactory.getConnection();
            statement = connection.prepareStatement( sql );
            setValues( statement, params.toArray() );
            System.out.println( statement );
            resultSet = statement.executeQuery();
            while ( resultSet.next() ) {
                Integer edition = resultSet.getInt( "edition" );
                List<TrackValue> tvs = tvsMap.get( edition );
                if ( tvs == null ) {
                    tvs = new ArrayList<TrackValue>();
                    tvsMap.put( edition, tvs );
                }
                tvs.add( new TrackValue( resultSet.getString( "primary" ), resultSet.getDate( "date" ), edition,
                        resultSet.getInt( "direct" ) ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return tvsMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.lang.Long)
     */
    @Override
    public Annotation find( Long id ) throws DAOException {
        return execute( SQL_FIND_BY_ID, id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.lang.String, java.lang.String, java.lang.Integer)
     */
    @Override
    public List<Annotation> find( String accession, String edition, Integer species ) throws DAOException {
        List<String> accessions = new ArrayList<String>();
        accessions.add( accession );
        return find( accessions, edition, species );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.util.List, java.lang.String, java.lang.Integer)
     */
    @Override
    public List<Annotation> find( List<String> accessions, String edition, Integer species ) throws DAOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Annotation> annotations = new ArrayList<Annotation>();
        String sql = String.format( SQL_FIND, preparePlaceHolders( accessions.size() ) );

        try {
            connection = daoFactory.getConnection();
            statement = connection.prepareStatement( sql );
            setValues( statement, accessions.toArray(), edition, species );
            resultSet = statement.executeQuery();
            while ( resultSet.next() ) {
                annotations.add( map( resultSet ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return annotations;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#existSymbol(java.lang.String)
     */
    @Override
    public boolean existSymbol( String symbol ) throws DAOException {
        // TODO Auto-generated method stub
        return false;
    }

    private Annotation execute( String sql, Object... values ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Annotation annotation = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, sql, false, values );
            resultSet = preparedStatement.executeQuery();
            if ( resultSet.next() ) {
                annotation = map( resultSet );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return annotation;
    }

    /**
     * Map the current row of the given ResultSet to an Annotation.
     * 
     * @param resultSet The ResultSet of which the current row is to be mapped to an Annotation.
     * @return The mapped Annotation from the current row of the given ResultSet.
     * @throws SQLException If something fails at database level.
     */
    private static Annotation map( ResultSet resultSet ) throws SQLException {
        Annotation annotation = new Annotation();
        annotation.setAccession( resultSet.getString( "accession" ) );
        annotation.setEdition( resultSet.getInt( "edition" ) );
        annotation.setEvidence( resultSet.getString( "evidence" ) );
        annotation.setGoId( resultSet.getString( "go_id" ) );
        annotation.setName( resultSet.getString( "db_object_name" ) );
        annotation.setReference( resultSet.getString( "reference" ) );
        annotation.setSpecies( resultSet.getInt( "species_id" ) );
        annotation.setSymbol( resultSet.getString( "symbol" ) );
        annotation.setSynonyms( Arrays.asList( resultSet.getString( "synonyms" ).split( "|" ) ) );
        String[] taxons = resultSet.getString( "taxon" ).split( "\\|" );
        annotation.setTaxon( Integer.parseInt( taxons[0].split( ":" )[1] ) );

        if ( taxons.length == 2 ) {
            annotation.setInteractingTaxon( Integer.parseInt( taxons[1].split( ":" )[1] ) );
        }

        annotation.setType( resultSet.getString( "db_object_type" ) );
        return annotation;
    }

}
