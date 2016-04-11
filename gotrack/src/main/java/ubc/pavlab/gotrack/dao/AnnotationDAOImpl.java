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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.dto.AnnotationDTO;
import ubc.pavlab.gotrack.model.dto.CategoryCountDTO;
import ubc.pavlab.gotrack.model.dto.DirectAnnotationCountDTO;
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

    // Get information from single gene, should be fast < 0.2s
    // species, symbol,species
    private static final String SQL_TRACK = "SELECT distinct edition, go_id, qualifier, evidence, reference from pp_current_genes "
            + "inner join pp_goa on pp_goa.pp_current_genes_id=pp_current_genes.id "
            + "where pp_current_genes_id=? "
            + "order by edition";

    // Get information from multiple genes for running enrichment, should be quite fast and scale sublinearly
    private static final String SQL_ENRICH = "SELECT distinct edition, go_id, pp_current_genes.id gene_id from pp_current_genes "
            + "inner join pp_goa on pp_current_genes.id = pp_goa.pp_current_genes_id "
            + "where pp_current_genes.id in (%s) "
            + "ORDER BY NULL";

    // Collect evidence breakdown for a specific term, should be not horribly slow, try and keep under 5s for slowest queries (ones for root level terms)
    private static final String SQL_CATEGORY_BREAKDOWN_FOR_TERM = "select date, evidence_categories.category , COUNT(*) count from pp_current_genes "
            + "inner join pp_goa on pp_goa.pp_current_genes_id = pp_current_genes.id "
            + "inner join edition on edition.edition=pp_goa.edition and edition.species_id = pp_current_genes.species_id "
            + "inner join evidence_categories on evidence_categories.evidence = pp_goa.evidence "
            + "where go_id=? group by date, evidence_categories.category "
            + "order by date";

    // Collect unique, directly annotated gene counts for a term, not used at the moment (pretty slow)
    private static final String SQL_DIRECT_GENE_COUNTS_FOR_TERM = "select species_id, edition, COUNT(distinct symbol) count from pp_current_genes "
            + "inner join pp_goa on pp_current_genes.id=pp_goa.pp_current_genes_id "
            + "where go_id = ? "
            + "group by species_id, edition";

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
    public List<AnnotationDTO> track( Gene g ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        params.add( g.getId() );

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
    public List<EnrichmentDTO> enrich( Set<Gene> genes ) throws DAOException {

        if ( genes == null || genes.size() == 0 ) {
            return Lists.newArrayList();
        }

        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_ENRICH, DAOUtil.preparePlaceHolders( genes.size() ) );

        for ( Gene g : genes ) {
            params.add( g.getId() );
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
        // TODO This method of collecting the data is not robust, 
        // If editions across species in the same 'release' are have slightly differing dates, 
        // they will not be grouped appropriately

        if ( goId == null || goId.equals( "" ) ) {
            return Lists.newArrayList();
        }

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

        if ( goId == null || goId.equals( "" ) ) {
            return Lists.newArrayList();
        }

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
                results.add( new DirectAnnotationCountDTO( resultSet.getInt( "species_id" ),
                        resultSet.getInt( "edition" ), "", resultSet.getInt( "count" ) ) );
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
        Integer geneId = resultSet.getInt( "gene_id" );
        String goId = resultSet.getString( "go_id" );
        return new EnrichmentDTO( edition, geneId, goId );
    }

}
