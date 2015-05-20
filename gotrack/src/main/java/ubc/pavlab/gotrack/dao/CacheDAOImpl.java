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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.StatsEntry;
import ubc.pavlab.gotrack.model.go.Relationship;
import ubc.pavlab.gotrack.model.go.RelationshipType;
import ubc.pavlab.gotrack.model.go.Term;

/**
 * Holds methods for retrieving data that is meant to be cached
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CacheDAOImpl implements CacheDAO {

    private static final Logger log = Logger.getLogger( CacheDAOImpl.class );

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_CURRENT_EDITIONS = "select species_id, edition, date, go_date, go_edition_id_fk "
            + "from (select species_id, edition, edition.date, go_edition.date as go_date, edition.go_edition_id_fk "
            + "from edition INNER JOIN go_edition on edition.go_edition_id_fk=go_edition.id order by edition DESC) "
            + "as temp group by species_id";
    private static final String SQL_ALL_EDITIONS = "select species_id, edition, edition.date, go_edition.date as go_date, go_edition_id_fk "
            + "from edition left join go_edition on edition.go_edition_id_fk=go_edition.id order by edition";
    private static final String SQL_ALL_GO_SIZES = "select species_id, edition, go_id, annotation_count from goa_go_aggregate";
    private static final String SQL_AGGREGATE = "select aggregate.species_id, aggregate.edition, edition.date, edition.go_edition_id_fk, go_edition.date go_date, avg_directs_by_gene, count_genes, avg_directs_by_accession, count_accessions from aggregate inner join edition on edition.edition=aggregate.edition and edition.species_id = aggregate.species_id INNER JOIN go_edition on edition.go_edition_id_fk=go_edition.id";
    private static final String SQL_GO_EDITIONS = "SELECT distinct id from go_edition";
    private static final String SQL_EVIDENCE_CATEGORIES = "select evidence, category from evidence_categories";

    private static final String SQL_CURRENT_GENES = "select species_id, symbol, accession, synonyms, sec from current_genes LEFT JOIN sec_ac on accession=ac";

    private static final String SQL_GO_TERMS = "Select go_ontology_tclosure.go_edition_id_fk, child go_id, name, aspect from go_ontology_tclosure inner join go_term on go_term.go_id=go_ontology_tclosure.child and go_term.go_edition_id_fk=go_ontology_tclosure.go_edition_id_fk where min_distance=0";
    private static final String SQL_GO_ADJACENCY = "select go_edition_id_fk, child, parent, relationship from go_ontology_tclosure where min_distance =1";

    private static final String SQL_GO_TERMS_BY_EDITION = "Select child go_id, name, aspect from go_ontology_tclosure inner join go_term on go_term.go_id=go_ontology_tclosure.child and go_term.go_edition_id_fk=go_ontology_tclosure.go_edition_id_fk where go_ontology_tclosure.go_edition_id_fk=? and min_distance=0";
    private static final String SQL_GO_ADJACENCY_BY_EDITION = "select child, parent, relationship from go_ontology_tclosure where go_ontology_tclosure.go_edition_id_fk=? and min_distance =1";

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
    public Map<Integer, Map<Integer, Map<String, Integer>>> getGOSizes() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Map<Integer, Map<String, Integer>>> goSetSizes = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_ALL_GO_SIZES );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                Integer speciesId = resultSet.getInt( "species_id" );
                Integer ed = resultSet.getInt( "edition" );
                String goid = resultSet.getString( "go_id" );
                Integer count = resultSet.getInt( "annotation_count" );

                Map<Integer, Map<String, Integer>> a = goSetSizes.get( speciesId );
                if ( a == null ) {
                    a = new HashMap<Integer, Map<String, Integer>>();
                    goSetSizes.put( speciesId, a );
                }

                Map<String, Integer> b = a.get( ed );
                if ( b == null ) {
                    b = new HashMap<String, Integer>();
                    a.put( ed, b );
                }

                Integer c = b.get( goid );
                if ( c == null ) {
                    b.put( goid, count );
                } else {
                    log.warn( goid + "|" + speciesId + "|" + ed );
                }

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return goSetSizes;

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
            log.debug( preparedStatement );
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
    public Map<Integer, List<Edition>> getAllEditions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, List<Edition>> results = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_ALL_EDITIONS );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();

            while ( resultSet.next() ) {
                int speciesId = resultSet.getInt( "species_id" );
                Edition edition = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) );
                List<Edition> editionsInSpecies = results.get( speciesId );

                if ( editionsInSpecies == null ) {
                    editionsInSpecies = new ArrayList<Edition>();
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
    public Map<Integer, Map<Edition, StatsEntry>> getAggregates() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Map<Edition, StatsEntry>> aggregates = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_AGGREGATE );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                Integer speciesId = resultSet.getInt( "species_id" );
                Edition edition = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) );
                StatsEntry se = new StatsEntry( resultSet.getInt( "count_accessions" ),
                        resultSet.getDouble( "avg_directs_by_accession" ), resultSet.getInt( "count_genes" ),
                        resultSet.getDouble( "avg_directs_by_gene" ) );

                Map<Edition, StatsEntry> speciesMap = aggregates.get( speciesId );

                if ( speciesMap == null ) {
                    speciesMap = new HashMap<>();
                    aggregates.put( speciesId, speciesMap );
                }

                speciesMap.put( edition, se );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return aggregates;
    }

    @Override
    public Set<Integer> getGOEditions() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Set<Integer> eds = new HashSet<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_EDITIONS );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {

                eds.add( resultSet.getInt( "id" ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return eds;
    }

    @Override
    public Map<String, String> getEvidenceCategories() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<String, String> results = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_EVIDENCE_CATEGORIES );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                String evidence = resultSet.getString( "evidence" );
                String category = resultSet.getString( "category" );
                results.put( evidence, category );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return results;
    }

    @Override
    public Map<Integer, Map<String, Gene>> getCurrentGenes() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Map<String, Gene>> currentGenes = new HashMap<>();
        Map<String, Accession> currentAccessions = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_CURRENT_GENES );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                Integer speciesId = resultSet.getInt( "species_id" );

                Map<String, Gene> symbolMap = currentGenes.get( speciesId );

                if ( symbolMap == null ) {
                    symbolMap = new HashMap<>();
                    currentGenes.put( speciesId, symbolMap );
                }

                String symbol = resultSet.getString( "symbol" );

                Gene g = symbolMap.get( symbol.toUpperCase() );

                if ( g == null ) {
                    // New gene
                    g = new Gene( symbol, speciesId );

                    symbolMap.put( symbol.toUpperCase(), g );
                }

                Set<String> synonyms = new HashSet<>( Arrays.asList( resultSet.getString( "synonyms" ).split( "\\|" ) ) );
                g.getSynonyms().addAll( synonyms );
                String accession = resultSet.getString( "accession" );

                Accession acc = currentAccessions.get( accession );

                if ( acc == null ) {
                    acc = new Accession( accession );
                    currentAccessions.put( accession, acc );
                }

                String sec = resultSet.getString( "sec" );
                if ( !resultSet.wasNull() ) {
                    // Null secondary values means that it was not found in sec_ac primary column and therefore has
                    // no secondary accessions
                    acc.addSecondary( sec );
                }

                g.getAccessions().add( acc );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return currentGenes;
    }

    @Override
    public Map<Integer, Set<Term>> getGoTerms() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Set<Term>> goTermsMap = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_TERMS );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_edition_id_fk, go_id, name, aspect
                Integer goEdition = resultSet.getInt( "go_edition_id_fk" );
                String goId = resultSet.getString( "go_id" );
                int id;
                try {
                    id = Integer.parseInt( goId.substring( goId.length() - 7 ) );
                } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                    log.warn( "Gene Ontology ID (" + goId + ") not in correct format." );
                    continue;
                }

                String name = resultSet.getString( "name" );
                Aspect aspect = Aspect.valueOf( resultSet.getString( "aspect" ) );

                Set<Term> goTerms = goTermsMap.get( goEdition );

                if ( goTerms == null ) {
                    goTerms = new HashSet<>();
                    goTermsMap.put( goEdition, goTerms );
                }

                goTerms.add( new Term( id, name, aspect ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return goTermsMap;
    }

    @Override
    public Set<Term> getGoTerms( int goEdition ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Set<Term> goTerms = new HashSet<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_TERMS_BY_EDITION );
            DAOUtil.setValues( preparedStatement, goEdition );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_id, name, aspect
                String goId = resultSet.getString( "go_id" );
                int id;
                try {
                    id = Integer.parseInt( goId.substring( goId.length() - 7 ) );
                } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                    log.warn( "Gene Ontology ID (" + goId + ") not in correct format." );
                    continue;
                }

                String name = resultSet.getString( "name" );
                Aspect aspect = Aspect.valueOf( resultSet.getString( "aspect" ) );

                goTerms.add( new Term( id, name, aspect ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return goTerms;
    }

    @Override
    public Map<Integer, Set<Relationship>> getAdjacencies() throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<Integer, Set<Relationship>> relsMap = new HashMap<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_ADJACENCY );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // go_edition_id_fk, child, parent, relationship
                Integer goEdition = resultSet.getInt( "go_edition_id_fk" );
                String childGoId = resultSet.getString( "child" );
                int childId;
                try {
                    childId = Integer.parseInt( childGoId.substring( childGoId.length() - 7 ) );
                } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                    log.warn( "Gene Ontology ID (" + childGoId + ") not in correct format." );
                    continue;
                }

                String parentGoId = resultSet.getString( "parent" );
                int parentId;
                try {
                    parentId = Integer.parseInt( parentGoId.substring( parentGoId.length() - 7 ) );
                } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                    log.warn( "Gene Ontology ID (" + parentGoId + ") not in correct format." );
                    continue;
                }

                RelationshipType type = RelationshipType.valueOf( resultSet.getString( "relationship" ) );

                Set<Relationship> rels = relsMap.get( goEdition );

                if ( rels == null ) {
                    rels = new HashSet<>();
                    relsMap.put( goEdition, rels );
                }

                rels.add( new Relationship( childId, parentId, type ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return relsMap;
    }

    @Override
    public Set<Relationship> getAdjacencies( int goEdition ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Set<Relationship> rels = new HashSet<>();

        try {
            connection = daoFactory.getConnection();
            preparedStatement = connection.prepareStatement( SQL_GO_ADJACENCY_BY_EDITION );
            DAOUtil.setValues( preparedStatement, goEdition );
            log.debug( preparedStatement );
            resultSet = preparedStatement.executeQuery();
            while ( resultSet.next() ) {
                // child, parent, relationship
                String childGoId = resultSet.getString( "child" );
                int childId;
                try {
                    childId = Integer.parseInt( childGoId.substring( childGoId.length() - 7 ) );
                } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                    log.warn( "Gene Ontology ID (" + childGoId + ") not in correct format." );
                    continue;
                }

                String parentGoId = resultSet.getString( "parent" );
                int parentId;
                try {
                    parentId = Integer.parseInt( parentGoId.substring( parentGoId.length() - 7 ) );
                } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                    log.warn( "Gene Ontology ID (" + parentGoId + ") not in correct format." );
                    continue;
                }

                RelationshipType type = RelationshipType.valueOf( resultSet.getString( "relationship" ) );

                rels.add( new Relationship( childId, parentId, type ) );

            }

        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return rels;
    }
}
