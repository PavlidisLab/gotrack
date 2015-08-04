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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.dto.DirectAnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.AnnotationDTO;
import ubc.pavlab.gotrack.model.dto.CategoryCountDTO;
import ubc.pavlab.gotrack.model.dto.EnrichmentDTO;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AnnotationDAOImpl implements AnnotationDAO {

    private static final Logger log = Logger.getLogger( AnnotationDAOImpl.class );

    /* CURRENT QUERIES */

    // species, symbol,species
    private static final String SQL_TRACK = "select distinct edition, go_id, qualifier, evidence, reference from goa_symbol inner join goa_annot on goa_symbol.id=goa_annot.goa_symbol_id where symbol=? and species_id=? order by edition";

    private static final String SQL_ENRICH = "select goa_symbol.edition, goa_annot.go_id, goa_symbol.symbol from goa_symbol inner join goa_annot on goa_symbol.id=goa_annot.goa_symbol_id where goa_symbol.species_id=? and goa_symbol.symbol in (%s) GROUP BY goa_symbol.edition, go_id, goa_symbol.symbol ORDER BY NULL";

    // Collect evidence breakdown for a specific term
    private static final String SQL_CATEGORY_BREAKDOWN_FOR_TERM = "select date, evidence_categories.category , COUNT(*) count from goa_symbol inner join goa_annot on goa_symbol.id = goa_annot.goa_symbol_id inner join edition on edition.edition=goa_symbol.edition and edition.species_id = goa_symbol.species_id inner join evidence_categories on evidence_categories.evidence  = goa_annot.evidence where go_id=? group by date, evidence_categories.category order by date";

    // Collect unique ,directly annotated gene counts for a term
    private static final String SQL_DIRECT_GENE_COUNTS_FOR_TERM = "select species_id, edition, COUNT(distinct symbol) count from goa_symbol inner join goa_annot on goa_symbol.id=goa_annot.goa_symbol_id where go_id = ? group by species_id, edition";

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

    @Override
    public List<AnnotationDTO> track( Integer speciesId, String symbol ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        // species, symbol,species
        params.add( symbol );
        params.add( speciesId );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<AnnotationDTO> results = new ArrayList<>();
        String sql = SQL_TRACK;

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
                results.add( new AnnotationDTO( resultSet.getInt( 1 ), resultSet.getString( 2 ),
                        resultSet.getString( 3 ), resultSet.getString( 4 ), resultSet.getString( 5 ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<EnrichmentDTO> enrich( Integer species, Set<Gene> genes ) throws DAOException {
        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_ENRICH, DAOUtil.preparePlaceHolders( genes.size() ) );

        Map<String, Gene> givenGenes = new HashMap<>();

        // species, symbols
        params.add( species );
        for ( Gene g : genes ) {
            params.add( g.getSymbol() );
            givenGenes.put( g.getSymbol().toUpperCase(), g );
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<EnrichmentDTO> results = new ArrayList<>();

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

                results.add( enrichmentMap( resultSet ) );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<CategoryCountDTO> categoryCounts( String goId ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        // species, symbol,species
        params.add( goId );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<CategoryCountDTO> results = new ArrayList<>();
        String sql = SQL_CATEGORY_BREAKDOWN_FOR_TERM;

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
                results.add( new CategoryCountDTO( resultSet.getDate( "date" ), resultSet.getString( "category" ),
                        resultSet.getInt( "count" ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<DirectAnnotationCountDTO> directGeneCounts( String goId ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        // species, symbol,species
        params.add( goId );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<DirectAnnotationCountDTO> results = new ArrayList<>();
        String sql = SQL_DIRECT_GENE_COUNTS_FOR_TERM;

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
                results.add( new DirectAnnotationCountDTO( resultSet.getInt( "species_id" ), resultSet.getInt( "edition" ),
                        "", resultSet.getInt( "count" ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    private static EnrichmentDTO enrichmentMap( ResultSet resultSet ) throws SQLException {
        Integer edition = resultSet.getInt( "edition" );
        String symbol = resultSet.getString( "symbol" );
        String goId = resultSet.getString( "go_id" );
        return new EnrichmentDTO( edition, symbol, goId );
    }

}
