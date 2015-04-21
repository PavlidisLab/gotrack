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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntologyDAOImpl implements GeneOntologyDAO {

    // Constants ----------------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger( GeneOntologyDAOImpl.class );

    private static final String SQL_TERM_INFO_MULTIPLE = "select go_id, name, aspect from go_term where go_edition_id_fk=? and go_id in (%s)";
    private static final String SQL_TERM_INFO = "select go_id, name, aspect from go_term where go_edition_id_fk=? and go_id =?";
    // Vars ---------------------------------------------------------------------------------------

    private DAOFactory daoFactory;

    // Constructors -------------------------------------------------------------------------------

    /**
     * Construct an User DAO for the given DAOFactory. Package private so that it can be constructed inside the DAO
     * package only.
     * 
     * @param daoFactory The DAOFactory to construct this User DAO for.
     */
    GeneOntologyDAOImpl( DAOFactory daoFactory ) {
        this.daoFactory = daoFactory;
    }

    // Actions ------------------------------------------------------------------------------------

    @Override
    public boolean loadTermInfo( int goEdition, Collection<GeneOntologyTerm> terms ) throws DAOException {
        return loadTermInfo( goEdition, terms, false );
    }

    @Override
    public boolean loadTermInfo( int goEdition, Collection<GeneOntologyTerm> terms, boolean force ) throws DAOException {

        Collection<GeneOntologyTerm> termsToLoad = new HashSet<>();

        if ( !force ) {
            // Check whether loading is necessary
            for ( GeneOntologyTerm term : terms ) {
                if ( term.getName() == null && term.getAspect() == null ) {
                    termsToLoad.add( term );
                }
            }

            if ( termsToLoad.isEmpty() ) {
                log.info( "Terms already loaded." );
                return false;
            }

        } else {
            termsToLoad = terms;
        }

        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_TERM_INFO_MULTIPLE, DAOUtil.preparePlaceHolders( termsToLoad.size() ) );

        Map<String, GeneOntologyTerm> givenTerms = new HashMap<>();

        // species, symbols
        params.add( goEdition );
        for ( GeneOntologyTerm t : termsToLoad ) {
            params.add( t.getGoId() );
            givenTerms.put( t.getGoId(), t );
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + ( endTime - startTime ) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + ( endTime - startTime ) + "ms" );

            startTime = System.currentTimeMillis();
            while ( resultSet.next() ) {

                String goId = resultSet.getString( "go_id" );

                GeneOntologyTerm term = givenTerms.get( goId );
                if ( term == null ) {
                    log.warn( "Could not find symbol:" + goId + " in given genes." );

                    term = new GeneOntologyTerm( goId, resultSet.getString( "name" ), resultSet.getString( "aspect" ) );
                } else {
                    term.setName( resultSet.getString( "name" ) );
                    term.setAspect( resultSet.getString( "aspect" ) );
                }

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }
        return true;
    }

    @Override
    public boolean loadTermInfo( int goEdition, GeneOntologyTerm term ) throws DAOException {
        return loadTermInfo( goEdition, term, false );
    }

    @Override
    public boolean loadTermInfo( int goEdition, GeneOntologyTerm term, boolean force ) throws DAOException {

        if ( !force && term.getName() != null && term.getAspect() != null ) {
            log.info( "Term already loaded." );
            return false;
        }

        List<Object> params = new ArrayList<Object>();

        String sql = SQL_TERM_INFO;

        // species, term
        params.add( goEdition );
        params.add( term.getGoId() );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + ( endTime - startTime ) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + ( endTime - startTime ) + "ms" );

            startTime = System.currentTimeMillis();
            while ( resultSet.next() ) {

                String goId = resultSet.getString( "go_id" );

                if ( term.getGoId().equals( goId ) ) {
                    term.setName( resultSet.getString( "name" ) );
                    term.setAspect( resultSet.getString( "aspect" ) );
                } else {
                    log.warn( "GO Id:" + goId + " from resultset did not match given term." );

                }
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return true;

    }

}
