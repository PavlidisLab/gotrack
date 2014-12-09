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
import java.util.List;

import ubc.pavlab.gotrack.model.Annotation;

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
