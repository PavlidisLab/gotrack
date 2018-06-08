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
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Tuples;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ubc.pavlab.gotrack.dao.DAOUtil.close;

/**
 * @author mjacobson
 */
public class AnnotationDAOImpl implements AnnotationDAO {

    private static final Logger log = Logger.getLogger( AnnotationDAOImpl.class );

    /* CURRENT QUERIES */

    private static final String SQL_ANNOTATION = "annotation";
    private static final String SQL_ACCESSION_HISTORY = "pp_accession_history";
    private static final String SQL_ACCESSION = "accession";
    private static final String SQL_EDITION = "edition";
    private static final String SQL_EVIDENCE = "evidence_categories";

    // Get information from single gene, should be fast < 0.2s
    // species, symbol,species
    private static final String SQL_FULL_ANNOTATION_RANGE_EDITIONS_SINGLE_GENE = "select distinct edition, go_id, qualifier, evidence, reference from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where ppah.ac = ? AND species_id= ? AND edition between ? and ? " +
            "order by edition";

    // Get information from multiple genes for running enrichment, should be quite fast and scale sublinearly
    private static final String SQL_ENRICHMENT_ANNOTATION_RANGE_EDITIONS_MULTIPLE_GENES = "select distinct edition, go_id, ac from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where ppah.ac in (%s) AND species_id= ? AND edition between ? and ? and qualifier not like 'NOT%%'" +
            "order by NULL";

    // Get information from multiple genes for running enrichment in a single edition, should be extremely fast
    private static final String SQL_ENRICHMENT_ANNOTATION_SINGLE_EDITION_MULTIPLE_GENES = "select distinct go_id, ac from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where ppah.ac in (%s) AND species_id= ? AND edition = ? and qualifier not like 'NOT%%'" +
            "order by NULL";

    // Collect evidence breakdown for a specific term, should be not horribly slow, try and keep under 5s for slowest queries (ones for root level terms)
    private static final String SQL_EVIDENCE_BREAKDOWN_RANGE_DATES_SINGLE_TERM = "select date, evidence , COUNT(*) count from " + SQL_ANNOTATION + " ann " +
            "inner join " + SQL_ACCESSION + " acc on acc.id=ann.accession_id  " +
            "inner join " + SQL_EDITION + " ed using(species_id, edition) " +
            "where go_id=? AND DATE BETWEEN ? AND ? group by date, evidence " +
            "order by date";

    // Collect evidence breakdown for a specific term in a specific species, should be not horribly slow, try and keep under 5s for slowest queries (ones for root level terms)
    private static final String SQL_EVIDENCE_BREAKDOWN_RANGE_EDITIONS_SINGLE_SPECIES_SINGLE_TERM = "select edition, evidence , COUNT(*) count from " + SQL_ANNOTATION + " ann " +
            "inner join " + SQL_ACCESSION + " acc on acc.id=ann.accession_id  " +
            "where go_id=? AND species_id=? AND edition between ? and ? group by edition, evidence " +
            "order by edition";

    // Collect unique, directly annotated genes for multiple terms in a single species. Meant to be used on a term and
    // all of its descendants where the 'direct' boolean corresponds to the main term being queried.
    // Not used at the moment.
    private static final String SQL_DIRECT_GENES_RANGE_EDITIONS_SINGLE_SPECIES_MULTIPLE_TERMS = "select distinct edition, ppah.ac, max(go_id=?) as direct from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where acc.species_id = ? and acc.edition between ? and ? and go_id in (%s) " +
            "group by edition, ac";

    // Collect unique, directly annotated genes for multiple terms in a single species. Meant to be used on a term and
    // all of its descendants where the 'direct' boolean corresponds to the main term being queried.
    private static final String SQL_DIRECT_GENES_SINGLE_EDITION_SINGLE_SPECIES_MULTIPLE_TERMS = "select distinct ppah.ac, max(go_id=?) as direct from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where acc.species_id = ? and acc.edition=? and go_id in (%s) " +
            "group by ac";

    // Collect unique, directly annotated genes for single term in a single species.
    private static final String SQL_DIRECT_GENES_SINGLE_EDITION_SINGLE_SPECIES_SINGLE_TERM = "select distinct ppah.ac from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where acc.species_id = ? and acc.edition=? and go_id = ?";

    private static final String SQL_SIMPLE_ANNOTATION_SINGLE_EDITION = "select distinct ac, go_id from " + SQL_ACCESSION + " acc " +
            "inner join " + SQL_ACCESSION_HISTORY + " ppah on acc.db_object_id = ppah.sec " +
            "inner join " + SQL_ANNOTATION + " ann on acc.id=ann.accession_id " +
            "where species_id = ? AND edition = ? " +
            "order by NULL";

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
    public List<AnnotationDTO> fullAnnotationRangeEditions( Gene g, Integer minimum, Integer maximum ) throws DAOException {

        List<Object> params = new ArrayList<>();

        params.add( g.getAccession().getAccession() );
        params.add( g.getSpecies().getId() ); // See Issue #32
        params.add( minimum );
        params.add( maximum );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<AnnotationDTO> results = new ArrayList<>();
        String sql = SQL_FULL_ANNOTATION_RANGE_EDITIONS_SINGLE_GENE;

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + (endTime - startTime) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + (endTime - startTime) + "ms" );

            startTime = System.currentTimeMillis();
            while (resultSet.next()) {
                results.add( new AnnotationDTO( resultSet.getInt( 1 ), resultSet.getString( 2 ),
                        resultSet.getString( 3 ), resultSet.getString( 4 ), resultSet.getString( 5 ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + (endTime - startTime) + "ms" );
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<EnrichmentDTO> simpleAnnotationRangeEditions( Set<Gene> genes, Integer minEdition, Integer maxEdition ) throws DAOException {

        if ( genes == null || genes.size() == 0 ) {
            return Lists.newArrayList();
        }

        Species species = genes.iterator().next().getSpecies();

        List<Object> params = Lists.newArrayList();

        String sql = String.format( SQL_ENRICHMENT_ANNOTATION_RANGE_EDITIONS_MULTIPLE_GENES, DAOUtil.preparePlaceHolders( genes.size() ) );

        for ( Gene g : genes ) {
            params.add( g.getAccession().getAccession() );
        }


        params.add( species.getId() ); // See Issue #32
        params.add( minEdition );
        params.add( maxEdition );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<EnrichmentDTO> results = new ArrayList<>();

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + (endTime - startTime) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + (endTime - startTime) + "ms" );

            startTime = System.currentTimeMillis();
            while (resultSet.next()) {

                results.add( enrichmentMap( resultSet ) );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + (endTime - startTime) + "ms" );
        } catch (SQLException e) {
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

        Species species = genes.iterator().next().getSpecies();

        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_ENRICHMENT_ANNOTATION_SINGLE_EDITION_MULTIPLE_GENES, DAOUtil.preparePlaceHolders( genes.size() ) );

        for ( Gene g : genes ) {
            params.add( g.getAccession().getAccession() );
        }

        params.add( species.getId() ); // See Issue #32
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
            log.debug( "daoFactory.getConnection(): " + (endTime - startTime) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + (endTime - startTime) + "ms" );

            startTime = System.currentTimeMillis();
            while (resultSet.next()) {

                results.add( simpleEnrichmentMap( resultSet ) );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + (endTime - startTime) + "ms" );
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<EvidenceCountDTO> categoryCountsRangeDates( String goId, Date min, Date max ) throws DAOException {
        // TODO pretty slow...

        if ( goId == null || goId.equals( "" ) ) {
            return Lists.newArrayList();
        }

        List<Object> params = new ArrayList<Object>();

        // species, symbol,species
        params.add( goId );
        params.add( min );
        params.add( max );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<EvidenceCountDTO> results = new ArrayList<>();
        String sql = SQL_EVIDENCE_BREAKDOWN_RANGE_DATES_SINGLE_TERM;

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + (endTime - startTime) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + (endTime - startTime) + "ms" );

            startTime = System.currentTimeMillis();
            while (resultSet.next()) {
                results.add( new EvidenceCountDTO( resultSet.getDate( "date" ), resultSet.getString( "evidence" ),
                        resultSet.getInt( "count" ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + (endTime - startTime) + "ms" );
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }


    @Override
    public List<EditionEvidenceCountDTO> categoryCountsSingleSpeciesRangeEditions( String goId, Species species, Integer minEdition, Integer maxEdition ) throws DAOException {

        if ( goId == null || goId.equals( "" ) || species == null ) {
            return Lists.newArrayList();
        }

        List<Object> params = new ArrayList<>();

        params.add( goId );
        params.add( species.getId() );
        params.add( minEdition );
        params.add( maxEdition );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<EditionEvidenceCountDTO> results = new ArrayList<>();
        String sql = SQL_EVIDENCE_BREAKDOWN_RANGE_EDITIONS_SINGLE_SPECIES_SINGLE_TERM;

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + (endTime - startTime) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + (endTime - startTime) + "ms" );

            startTime = System.currentTimeMillis();
            while (resultSet.next()) {
                results.add( new EditionEvidenceCountDTO( resultSet.getInt( "edition" ), resultSet.getString( "evidence" ),
                        resultSet.getInt( "count" ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + (endTime - startTime) + "ms" );
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<Tuples.Tuple3<Integer, String, Boolean>> inferredGenesRangeEditions( GeneOntologyTerm term, Species species, Integer minEdition, Integer maxEdition ) throws DAOException {
        if ( term == null ) {
            return Lists.newArrayList();
        }

        List<Object> params = Lists.newArrayList();

        params.add( term.getGoId() );
        params.add( species.getId() );
        params.add( minEdition );
        params.add( maxEdition );

        List<GeneOntologyTerm> descendants = term.streamDescendants().collect( Collectors.toList() );
        descendants.add( term );

        String sql = String.format( SQL_DIRECT_GENES_RANGE_EDITIONS_SINGLE_SPECIES_MULTIPLE_TERMS, DAOUtil.preparePlaceHolders( descendants.size() ) );

        for ( GeneOntologyTerm t : descendants ) {
            params.add( t.getGoId() );
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Tuples.Tuple3<Integer, String, Boolean>> results = new ArrayList<>();

        log.debug( sql );

        try {
            connection = daoFactory.getConnection();
            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add( new Tuples.Tuple3<>( resultSet.getInt( "edition" ),
                        resultSet.getString( "ac" ),
                        resultSet.getBoolean( "direct" ) ) );
            }
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<Tuples.Tuple2<String, Boolean>> inferredGenesSingleEdition( GeneOntologyTerm term, Edition edition ) throws DAOException {
        if ( term == null ) {
            return Lists.newArrayList();
        }

        List<Object> params = Lists.newArrayList();

        params.add( term.getGoId() );
        params.add( edition.getSpecies().getId() );
        params.add( edition.getEdition() );

        List<GeneOntologyTerm> descendants = term.streamDescendants().collect( Collectors.toList() );
        descendants.add( term );

        String sql = String.format( SQL_DIRECT_GENES_SINGLE_EDITION_SINGLE_SPECIES_MULTIPLE_TERMS, DAOUtil.preparePlaceHolders( descendants.size() ) );

        for ( GeneOntologyTerm t : descendants ) {
            params.add( t.getGoId() );
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Tuples.Tuple2<String, Boolean>> results = new ArrayList<>();

        log.debug( sql );

        try {
            connection = daoFactory.getConnection();
            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add( new Tuples.Tuple2<>( resultSet.getString( "ac" ), resultSet.getBoolean( "direct" ) ) );
            }
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<String> directGenesSingleEdition( GeneOntologyTerm term, Edition edition ) throws DAOException {
        if ( term == null ) {
            return Lists.newArrayList();
        }

        List<Object> params = Lists.newArrayList();

        params.add( edition.getSpecies().getId() );
        params.add( edition.getEdition() );
        params.add( term.getGoId() );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<String> results = new ArrayList<>();
        String sql = SQL_DIRECT_GENES_SINGLE_EDITION_SINGLE_SPECIES_SINGLE_TERM;

        log.debug( sql );

        try {
            connection = daoFactory.getConnection();
            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add( resultSet.getString( "ac" ) );
            }
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    @Override
    public List<Tuples.Tuple2<String, String>> simpleAnnotationSingleEditionCompleteSpecies( Species species, Edition edition ) throws DAOException {

        List<Object> params = new ArrayList<>();

        params.add( species.getId() );
        params.add( edition.getEdition() );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Tuples.Tuple2<String, String>> results = new ArrayList<>();
        String sql = SQL_SIMPLE_ANNOTATION_SINGLE_EDITION;

        log.debug( sql );

        try {

            long startTime = System.currentTimeMillis();
            connection = daoFactory.getConnection();
            long endTime = System.currentTimeMillis();
            log.debug( "daoFactory.getConnection(): " + (endTime - startTime) + "ms" );

            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );

            startTime = System.currentTimeMillis();
            resultSet = statement.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug( "statement.executeQuery(): " + (endTime - startTime) + "ms" );

            startTime = System.currentTimeMillis();
            while (resultSet.next()) {
                results.add( new Tuples.Tuple2<>( resultSet.getString( "ac" ), resultSet.getString( "go_id" ) ) );
            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + (endTime - startTime) + "ms" );
        } catch (SQLException e) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return results;
    }

    private static EnrichmentDTO enrichmentMap( ResultSet resultSet ) throws SQLException {
        Integer edition = resultSet.getInt( "edition" );
        String accession = resultSet.getString( "ac" );
        String goId = resultSet.getString( "go_id" );
        return new EnrichmentDTO( edition, accession, goId );
    }

    private static SimpleAnnotationDTO simpleEnrichmentMap( ResultSet resultSet ) throws SQLException {
        String accession = resultSet.getString( "ac" );
        String goId = resultSet.getString( "go_id" );
        return new SimpleAnnotationDTO( goId, accession );
    }

}
