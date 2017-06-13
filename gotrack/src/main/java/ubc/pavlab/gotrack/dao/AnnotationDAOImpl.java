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

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.dto.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ubc.pavlab.gotrack.dao.DAOUtil.close;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AnnotationDAOImpl implements AnnotationDAO {

    private static final Logger log = Logger.getLogger( AnnotationDAOImpl.class );

    /* CURRENT QUERIES */

    private static final String SQL_STAR = "*";
    private static final String SQL_ANNOTATION = "annotation";
    private static final String SQL_ACCESSION_HISTORY = "staging_pp_accession_history";
    private static final String SQL_ACCESSION = "accession";
    private static final String SQL_EDITION = "edition_tmp"; // TODO: data-format - replace with edition
    private static final String SQL_EVIDENCE = "evidence_categories";

    // Get information from single gene, should be fast < 0.2s
    // species, symbol,species

    private static final String SQL_FULL_ANNOTATION_ALL_EDITIONS_SINGLE_GENE = "select distinct edition, go_id, qualifier, evidence, reference from " + SQL_ANNOTATION + " ann " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on ppah.secondary_accession_id = ann.accession_id " +
            "inner join " + SQL_ACCESSION + " acc on acc.id = ppah.secondary_accession_id " +
            "where ppah.accession_id = ? " +
            "order by edition";

    // Get information from multiple genes for running enrichment, should be quite fast and scale sublinearly
    private static final String SQL_SIMPLE_ANNOTATION_ALL_EDITIONS_MULTIPLE_GENES = "select distinct acc.edition, go_id, ppah.accession_id gene_id from " + SQL_ANNOTATION + " ann " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on ppah.secondary_accession_id = ann.accession_id " +
            "inner join " + SQL_ACCESSION + " acc on acc.id=ppah.secondary_accession_id " +
            "where ppah.accession_id in (%s) " +
            "ORDER BY NULL";

    // Get information from multiple genes for running enrichment in a single edition, should be extremely fast
    private static final String SQL_SIMPLE_ANNOTATION_SINGLE_EDITION_MULTIPLE_GENES = "select distinct go_id, ppah.accession_id gene_id from " + SQL_ANNOTATION + " ann " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on ppah.secondary_accession_id = ann.accession_id " +
            "inner join " + SQL_ACCESSION + " acc on acc.id=ppah.secondary_accession_id " +
            "where edition = ? and ppah.accession_id in (%s) " +
            "ORDER BY NULL";

    // Collect evidence breakdown for a specific term, should be not horribly slow, try and keep under 5s for slowest queries (ones for root level terms)
    private static final String SQL_CATEGORY_BREAKDOWN_ALL_EDITIONS_SINGLE_TERM = "select date, evcat.category , COUNT(*) count from " + SQL_ANNOTATION + " ann " +
            "inner join " + SQL_EVIDENCE + " evcat on evcat.evidence = ann.evidence " +
            "inner join " + SQL_ACCESSION + " acc on acc.id=ann.accession_id  " +
            "inner join " + SQL_EDITION + " ed using(species_id, edition) " +
            "where go_id=? group by date, evcat.category " +
            "order by date";

    // Collect unique, directly annotated gene counts for a term, not used at the moment (pretty slow)
    private static final String SQL_DIRECT_GENE_CNT_ALL_EDITIONS_SINGLE_TERM = "select species_id, edition, COUNT(distinct accession_id) count from annotation ann " +
            "inner join accession acc on acc.id=ann.accession_id " +
            "where go_id = ? " +
            "group by species_id, edition";

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
    public List<AnnotationDTO> fullAnnotationAllEditions( Gene g ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        params.add( g.getId() );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<AnnotationDTO> results = new ArrayList<>();
        String sql = SQL_FULL_ANNOTATION_ALL_EDITIONS_SINGLE_GENE;

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
    public List<EnrichmentDTO> simpleAnnotationAllEditions( Set<Gene> genes ) throws DAOException {

        if ( genes == null || genes.size() == 0 ) {
            return Lists.newArrayList();
        }

        List<Object> params = Lists.newArrayList();

        String sql = String.format( SQL_SIMPLE_ANNOTATION_ALL_EDITIONS_MULTIPLE_GENES, DAOUtil.preparePlaceHolders( genes.size() ) );

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
    public List<SimpleAnnotationDTO> simpleAnnotationSingleEdition( Edition ed, Set<Gene> genes ) throws DAOException {

        if ( genes == null || genes.size() == 0 || ed == null ) {
            return Lists.newArrayList();
        }

        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_SIMPLE_ANNOTATION_SINGLE_EDITION_MULTIPLE_GENES, DAOUtil.preparePlaceHolders( genes.size() ) );

        for ( Gene g : genes ) {
            params.add( g.getId() );
        }

        params.add( ed.getEdition() );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<SimpleAnnotationDTO> results = new ArrayList<>();

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

                results.add( simpleEnrichmentMap( resultSet ) );

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
    public List<CategoryCountDTO> categoryCountsAllEditions( String goId ) throws DAOException {
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
        String sql = SQL_CATEGORY_BREAKDOWN_ALL_EDITIONS_SINGLE_TERM;

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
    public List<DirectAnnotationCountDTO> directGeneCountsAllEditions( String goId ) throws DAOException {

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
        String sql = SQL_DIRECT_GENE_CNT_ALL_EDITIONS_SINGLE_TERM;

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

    private static SimpleAnnotationDTO simpleEnrichmentMap( ResultSet resultSet ) throws SQLException {
        Integer geneId = resultSet.getInt( "gene_id" );
        String goId = resultSet.getString( "go_id" );
        return new SimpleAnnotationDTO( goId, geneId );
    }

}
