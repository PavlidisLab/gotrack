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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationDetailed;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AnnotationDAOImpl implements AnnotationDAO {

    private static final Logger log = Logger.getLogger( AnnotationDAOImpl.class );

    // Constants ----------------------------------------------------------------------------------

    private static final String SQL_FIND_BY_ID = "SELECT edition, species_id, accession, symbol, go_id, reference, evidence, db_object_name, synonyms, db_object_type, taxon FROM gene_annotation WHERE id = ?";
    private static final String SQL_FIND_LIST = "SELECT edition, species_id, accession, symbol, go_id, reference, evidence, db_object_name, synonyms, db_object_type, taxon FROM gene_annotation WHERE accession IN (%s) AND edition = ? AND species_id = ?";

    /* OLD QUERIES KEPT FOR METHOD REFERENCE */
    private static final String SQL_TRACK_OLD = "select edition.date, gene_annotation.edition, CASE %s ELSE accession END as `primary`, gene_annotation.go_id, gt1.name, gt1.aspect, gt2.is_obsolete "
            + "from gene_annotation INNER JOIN edition on edition.edition=gene_annotation.edition AND edition.species_id = ? "
            + "LEFT JOIN go_term gt1 on edition.go_edition_id_fk=gt1.go_edition_id_fk and gt1.go_id=gene_annotation.go_id "
            + "LEFT JOIN go_term gt2 on gt2.go_id=gene_annotation.go_id AND gt2.go_edition_id_fk = ? "
            + "where gene_annotation.species_id=? and accession IN (%s) GROUP BY gene_annotation.edition, `primary`, gene_annotation.go_id ORDER BY NULL";
    private static final String SQL_TRACK_PROPAGATED_OLD = "select edition.date, gene_annotation.edition, CASE %s ELSE accession END as `primary`, go_ontology_tclosure.parent as go_id, gt1.name, gt1.aspect, gt2.is_obsolete "
            + "from gene_annotation INNER JOIN edition on edition.edition=gene_annotation.edition AND edition.species_id = ? "
            + "INNER JOIN go_ontology_tclosure ON gene_annotation.go_id=child and go_ontology_tclosure.go_edition_id_fk=edition.go_edition_id_fk "
            + "LEFT JOIN go_term gt1 on edition.go_edition_id_fk=gt1.go_edition_id_fk and gt1.go_id=go_ontology_tclosure.parent "
            + "LEFT JOIN go_term gt2 on gt2.go_id=go_ontology_tclosure.parent AND gt2.go_edition_id_fk = ? "
            + "where gene_annotation.species_id=? and accession IN (%s) GROUP BY gene_annotation.edition, `primary`, parent ORDER BY NULL";
    private static final String SQL_TRACK_WHEN_THEN = "WHEN accession IN (%s) THEN ? ";

    /* CURRENT QUERIES */

    // species, symbol,species
    private static final String SQL_TRACK = "select edition.date, edition.edition, edition.go_edition_id_fk, current_genes.accession as `primary`, gene_annotation.go_id, go_term.name, go_term.aspect, gene_annotation.evidence, gene_annotation.reference, evidence_categories.category, acindex.symbol as sp_gene from current_genes left join acindex using (accession) left join sec_ac on ac=current_genes.accession  inner join gene_annotation PARTITION (p?) IGNORE INDEX (ed_go_acc_spec, ed_acc_spec) on current_genes.accession = gene_annotation.accession OR sec=gene_annotation.accession  INNER JOIN evidence_categories on gene_annotation.evidence=evidence_categories.evidence INNER JOIN edition on edition.edition=gene_annotation.edition and edition.species_id =gene_annotation.species_id  LEFT JOIN go_term on go_term.go_id=gene_annotation.go_id and go_term.go_edition_id_fk=edition.go_edition_id_fk  WHERE current_genes.symbol = ? and current_genes.species_id=? GROUP BY gene_annotation.edition, current_genes.accession, gene_annotation.go_id, evidence, reference ORDER BY NULL";

    // species, symbol
    private static final String SQL_TRACK_PROPAGATE = "select edition.date, goa_symbol.edition, edition.go_edition_id_fk, edition.date go_date, goa_symbol.primary_accession, goa_annot.qualifier, IFNULL(go_ontology_tclosure.parent, goa_annot.go_id) ancestor, IF(go_ontology_tclosure.min_distance=0 or go_ontology_tclosure.parent is null, TRUE, FALSE) direct, goa_annot.evidence, goa_annot.reference, go_term.name, go_term.aspect, evidence_categories.category, acindex.symbol as sp_gene from goa_symbol left join acindex using (accession) inner join goa_annot on goa_symbol.id=goa_annot.goa_symbol_id INNER JOIN evidence_categories on goa_annot.evidence=evidence_categories.evidence INNER JOIN edition on edition.edition=goa_symbol.edition and edition.species_id =goa_symbol.species_id LEFT JOIN go_ontology_tclosure on edition.go_edition_id_fk=go_ontology_tclosure.go_edition_id_fk and goa_annot.go_id = go_ontology_tclosure.child LEFT JOIN go_term on go_term.go_id=IFNULL(go_ontology_tclosure.parent, goa_annot.go_id) and go_term.go_edition_id_fk=edition.go_edition_id_fk where goa_symbol.species_id=? and goa_symbol.symbol = ? GROUP BY goa_symbol.edition, goa_symbol.primary_accession, goa_annot.qualifier, ancestor, goa_annot.evidence, goa_annot.reference, direct ORDER BY NULL";
    // species, symbols
    private static final String SQL_ENRICH_PROPAGATE = "select edition.date, goa_symbol.edition, edition.go_edition_id_fk, edition.date go_date, goa_symbol.symbol, IFNULL(go_ontology_tclosure.parent, goa_annot.go_id) ancestor, go_term.name, go_term.aspect from goa_symbol inner join goa_annot on goa_symbol.id=goa_annot.goa_symbol_id INNER JOIN edition on edition.edition=goa_symbol.edition and edition.species_id =goa_symbol.species_id LEFT JOIN go_ontology_tclosure  on edition.go_edition_id_fk=go_ontology_tclosure.go_edition_id_fk and goa_annot.go_id = go_ontology_tclosure.child LEFT JOIN go_term on go_term.go_id=IFNULL(go_ontology_tclosure.parent, goa_annot.go_id) and go_term.go_edition_id_fk=edition.go_edition_id_fk where goa_symbol.species_id=? and goa_symbol.symbol in (%s) GROUP BY goa_symbol.edition, goa_symbol.symbol, ancestor ORDER BY NULL";

    // species, symbols, species
    private static final String SQL_ENRICHMENT_GENE_DATA = "select edition.date, edition.edition, edition.go_edition_id_fk, current_genes.symbol, gene_annotation.go_id, go_term.name, go_term.aspect from current_genes left join sec_ac on ac=current_genes.accession inner join gene_annotation PARTITION (p?) IGNORE INDEX (ed_go_acc_spec, ed_acc_spec) on current_genes.accession = gene_annotation.accession OR sec=gene_annotation.accession INNER JOIN edition on edition.edition=gene_annotation.edition and edition.species_id =gene_annotation.species_id LEFT JOIN go_term on go_term.go_id=gene_annotation.go_id and go_term.go_edition_id_fk=edition.go_edition_id_fk WHERE current_genes.symbol in (%s) and current_genes.species_id=? GROUP BY gene_annotation.edition, current_genes.symbol, gene_annotation.go_id ORDER BY NULL";
    private static final String SQL_ENRICHMENT_GENE_DATA_OLD = "select edition.date, gene_annotation.edition, edition.go_edition_id_fk, temp.symbol, gene_annotation.go_id, go_term.name, go_term.aspect from (select distinct symbol, accession from gene_annotation WHERE symbol IN (%s) AND edition=? and species_id=?) as temp LEFT JOIN sec_ac ON ac=accession INNER JOIN gene_annotation ON temp.accession=gene_annotation.accession OR sec=gene_annotation.accession INNER JOIN edition on edition.edition=gene_annotation.edition AND edition.species_id = gene_annotation.species_id LEFT JOIN go_term on go_term.go_id=gene_annotation.go_id and go_term.go_edition_id_fk=edition.go_edition_id_fk where gene_annotation.species_id = ? GROUP BY gene_annotation.edition, temp.symbol, gene_annotation.go_id ORDER BY NULL";

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

    @Deprecated
    @Override
    public Map<String, Map<Edition, Set<Annotation>>> trackOld( Integer species,
            Map<String, Collection<String>> primaryToSecondary, Integer goEditionId, boolean ancestorsOnly )
            throws DAOException {

        List<Object> params = new ArrayList<Object>();
        List<String> allAccessions = new ArrayList<String>();

        // create WHEN THEN strings for CASE END for those primaries with secondary accessions
        String sql_when = "";
        for ( Entry<String, Collection<String>> e : primaryToSecondary.entrySet() ) {
            String primary = e.getKey();
            Collection<String> secs = e.getValue();
            allAccessions.add( primary );
            if ( secs.size() > 0 ) {
                sql_when += String.format( SQL_TRACK_WHEN_THEN, DAOUtil.preparePlaceHolders( secs.size() + 1 ) );
                params.add( primary );
                params.addAll( secs );
                params.add( primary );
                allAccessions.addAll( secs );
            }

        }
        // System.out.println( sql_when );

        params.add( species );
        params.add( goEditionId );
        params.add( species );
        for ( String a : allAccessions ) {
            params.add( a );
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        // List<TrackValue> tvs = new ArrayList<TrackValue>();
        Map<String, Map<Edition, Set<Annotation>>> allSeries = new HashMap<String, Map<Edition, Set<Annotation>>>();
        String sql = String.format( ancestorsOnly ? SQL_TRACK_PROPAGATED_OLD : SQL_TRACK_OLD, sql_when,
                DAOUtil.preparePlaceHolders( allAccessions.size() ) );

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
                // Integer edition = resultSet.getInt( "edition" );
                String primary = resultSet.getString( "primary" );
                // TODO fill rest of Edition properties
                Edition edition = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ) );
                Annotation term = new Annotation();
                term.setGoId( resultSet.getString( "go_id" ) );
                term.setName( resultSet.getString( "name" ) );
                term.setAspect( resultSet.getString( "aspect" ) );
                Byte b = resultSet.getByte( "is_obsolete" );
                if ( b == null ) {
                    term.setObsolete( null );
                } else {
                    term.setObsolete( b.intValue() == 0 ? false : true );
                }

                Map<Edition, Set<Annotation>> series = allSeries.get( primary );
                if ( series == null ) {
                    series = new HashMap<Edition, Set<Annotation>>();
                    allSeries.put( primary, series );
                }

                Set<Annotation> dataTerms = series.get( edition );
                if ( dataTerms == null ) {
                    dataTerms = new HashSet<Annotation>();
                    series.put( edition, dataTerms );
                }

                dataTerms.add( term );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return allSeries;
    }

    @Override
    public Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> track( Integer species,
            String symbol ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        // species, symbol,species
        params.add( species );
        params.add( symbol );
        params.add( species );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        // Map<String, Map<Edition, Map<String, Set<Annotation>>>> allSeries = new HashMap<>();
        Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allSeries = new HashMap<>();
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
                Accession primary = new Accession( resultSet.getString( "primary" ), resultSet.getString( "sp_gene" ) );
                Edition ed = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getInt( "go_edition_id_fk" ) );
                GeneOntologyTerm go = new GeneOntologyTerm( resultSet.getString( "go_id" ),
                        resultSet.getString( "name" ), resultSet.getString( "aspect" ) );
                EvidenceReference er = new EvidenceReference( resultSet.getString( "evidence" ),
                        resultSet.getString( "reference" ), resultSet.getString( "category" ), primary.getDataset() );

                Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> series = allSeries.get( primary );
                if ( series == null ) {
                    series = new HashMap<>();
                    allSeries.put( primary, series );
                }

                Map<GeneOntologyTerm, Set<EvidenceReference>> goIdMap = series.get( ed );
                if ( goIdMap == null ) {
                    goIdMap = new HashMap<>();
                    series.put( ed, goIdMap );
                }

                Set<EvidenceReference> evidence = goIdMap.get( go );
                if ( evidence == null ) {
                    evidence = new HashSet<>();
                    goIdMap.put( go, evidence );
                }

                evidence.add( er );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return allSeries;
    }

    @Override
    public Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> trackPropagate( Integer species,
            String symbol ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        // species, symbol
        params.add( species );
        params.add( symbol );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        // Map<String, Map<Edition, Map<String, Set<Annotation>>>> allSeries = new HashMap<>();
        Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> allSeries = new HashMap<>();
        String sql = SQL_TRACK_PROPAGATE;

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
                Accession primary = new Accession( resultSet.getString( "primary_accession" ),
                        resultSet.getString( "sp_gene" ) );
                Edition ed = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) );
                GeneOntologyTerm go = new GeneOntologyTerm( resultSet.getString( "ancestor" ),
                        resultSet.getString( "name" ), resultSet.getString( "aspect" ) );

                if ( go.getName() == null ) {
                    log.warn( "Could not find (" + go.getGoId() + ") in go_edition: " + ed.getGoEditionId() );
                }

                EvidenceReference er = new EvidenceReference( resultSet.getString( "evidence" ),
                        resultSet.getString( "reference" ), resultSet.getString( "category" ), primary.getDataset(),
                        resultSet.getBoolean( "direct" ) );

                Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>> series = allSeries.get( primary );
                if ( series == null ) {
                    series = new HashMap<>();
                    allSeries.put( primary, series );
                }

                Map<GeneOntologyTerm, Set<EvidenceReference>> goIdMap = series.get( ed );
                if ( goIdMap == null ) {
                    goIdMap = new HashMap<>();
                    series.put( ed, goIdMap );
                }

                Set<EvidenceReference> evidence = goIdMap.get( go );
                if ( evidence == null ) {
                    evidence = new HashSet<>();
                    goIdMap.put( go, evidence );
                }

                evidence.add( er );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return allSeries;
    }

    @Override
    public Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> enrichmentDataPropagate( Integer species, Set<Gene> genes )
            throws DAOException {
        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_ENRICH_PROPAGATE, DAOUtil.preparePlaceHolders( genes.size() ) );

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

        Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> data = new HashMap<>();

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

                String symbol = resultSet.getString( "symbol" );
                Gene g = givenGenes.get( symbol.toUpperCase() );
                if ( g == null ) {
                    log.warn( "Could not find symbol:" + symbol + " in given genes." );
                    g = new Gene( symbol );
                }

                Edition ed = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getDate( "go_date" ), resultSet.getInt( "go_edition_id_fk" ) );
                GeneOntologyTerm go = new GeneOntologyTerm( resultSet.getString( "ancestor" ),
                        resultSet.getString( "name" ), resultSet.getString( "aspect" ) );

                if ( go.getName() == null ) {
                    log.warn( "Could not find (" + go.getGoId() + ") in go_edition: " + ed.getGoEditionId() );
                }

                Map<Gene, Set<GeneOntologyTerm>> edEntry = data.get( ed );
                if ( edEntry == null ) {
                    edEntry = new HashMap<>();
                    data.put( ed, edEntry );
                }

                Set<GeneOntologyTerm> goSet = edEntry.get( g );
                if ( goSet == null ) {
                    goSet = new HashSet<>();
                    edEntry.put( g, goSet );
                }

                goSet.add( go );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return data;
    }

    @Override
    public Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> enrichmentData( Integer species, Set<Gene> genes )
            throws DAOException {

        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_ENRICHMENT_GENE_DATA, DAOUtil.preparePlaceHolders( genes.size() ) );

        Map<String, Gene> givenGenes = new HashMap<>();

        // species, symbols, species
        params.add( species );
        for ( Gene g : genes ) {
            params.add( g.getSymbol() );
            givenGenes.put( g.getSymbol().toUpperCase(), g );
        }

        params.add( species );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> data = new HashMap<>();

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

                String symbol = resultSet.getString( "symbol" );
                Gene g = givenGenes.get( symbol.toUpperCase() );
                if ( g == null ) {
                    log.warn( "Could not find symbol:" + symbol + " in given genes." );
                    g = new Gene( symbol );
                }
                // Gene g = new Gene( resultSet.getString( "symbol" ) );
                Edition ed = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getInt( "go_edition_id_fk" ) );
                GeneOntologyTerm go = new GeneOntologyTerm( resultSet.getString( "go_id" ),
                        resultSet.getString( "name" ), resultSet.getString( "aspect" ) );

                Map<Gene, Set<GeneOntologyTerm>> edEntry = data.get( ed );
                if ( edEntry == null ) {
                    edEntry = new HashMap<>();
                    data.put( ed, edEntry );
                }

                Set<GeneOntologyTerm> goSet = edEntry.get( g );
                if ( goSet == null ) {
                    goSet = new HashSet<>();
                    edEntry.put( g, goSet );
                }

                goSet.add( go );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return data;
    }

    @Override
    public Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> enrichmentDataOld( Integer species, Set<Gene> genes,
            Integer currentEdition ) throws DAOException {

        List<Object> params = new ArrayList<Object>();

        String sql = String.format( SQL_ENRICHMENT_GENE_DATA_OLD, DAOUtil.preparePlaceHolders( genes.size() ) );

        Map<String, Gene> givenGenes = new HashMap<>();

        // symbols, edition, species, species
        for ( Gene g : genes ) {
            params.add( g.getSymbol() );
            givenGenes.put( g.getSymbol().toUpperCase(), g );
        }

        params.add( currentEdition );
        params.add( species );
        params.add( species );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> data = new HashMap<>();

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

                String symbol = resultSet.getString( "symbol" );
                Gene g = givenGenes.get( symbol.toUpperCase() );
                if ( g == null ) {
                    log.warn( "Could not find symbol:" + symbol + " in given genes." );
                    g = new Gene( symbol );
                }
                // Gene g = new Gene( resultSet.getString( "symbol" ) );
                Edition ed = new Edition( resultSet.getInt( "edition" ), resultSet.getDate( "date" ),
                        resultSet.getInt( "go_edition_id_fk" ) );
                GeneOntologyTerm go = new GeneOntologyTerm( resultSet.getString( "go_id" ),
                        resultSet.getString( "name" ), resultSet.getString( "aspect" ) );

                Map<Gene, Set<GeneOntologyTerm>> edEntry = data.get( ed );
                if ( edEntry == null ) {
                    edEntry = new HashMap<>();
                    data.put( ed, edEntry );
                }

                Set<GeneOntologyTerm> goSet = edEntry.get( g );
                if ( goSet == null ) {
                    goSet = new HashSet<>();
                    edEntry.put( g, goSet );
                }

                goSet.add( go );

            }
            endTime = System.currentTimeMillis();
            log.debug( "while ( resultSet.next() ): " + ( endTime - startTime ) + "ms" );
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.lang.Long)
     */
    @Override
    public AnnotationDetailed find( Long id ) throws DAOException {
        return execute( SQL_FIND_BY_ID, id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.gotrack.dao.AnnotationDAO#find(java.lang.String, java.lang.String, java.lang.Integer)
     */
    @Override
    public List<AnnotationDetailed> find( String accession, Integer edition, Integer species ) throws DAOException {
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
    public List<AnnotationDetailed> find( List<String> accessions, Integer edition, Integer species )
            throws DAOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<AnnotationDetailed> annotationDetaileds = new ArrayList<AnnotationDetailed>();
        String sql = String.format( SQL_FIND_LIST, DAOUtil.preparePlaceHolders( accessions.size() ) );
        log.debug( sql );
        List<Object> params = new ArrayList<Object>();

        for ( String acc : accessions ) {
            params.add( acc );
        }
        params.add( edition );
        params.add( species );

        try {
            connection = daoFactory.getConnection();
            statement = connection.prepareStatement( sql );
            DAOUtil.setValues( statement, params.toArray() );
            log.debug( statement );
            resultSet = statement.executeQuery();
            while ( resultSet.next() ) {
                annotationDetaileds.add( map( resultSet ) );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, statement, resultSet );
        }

        return annotationDetaileds;

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

    private AnnotationDetailed execute( String sql, Object... values ) throws DAOException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        AnnotationDetailed annotationDetailed = null;

        try {
            connection = daoFactory.getConnection();
            preparedStatement = prepareStatement( connection, sql, false, values );
            resultSet = preparedStatement.executeQuery();
            if ( resultSet.next() ) {
                annotationDetailed = map( resultSet );
            }
        } catch ( SQLException e ) {
            throw new DAOException( e );
        } finally {
            close( connection, preparedStatement, resultSet );
        }

        return annotationDetailed;
    }

    /**
     * Map the current row of the given ResultSet to an Annotation.
     * 
     * @param resultSet The ResultSet of which the current row is to be mapped to an Annotation.
     * @return The mapped Annotation from the current row of the given ResultSet.
     * @throws SQLException If something fails at database level.
     */
    private static AnnotationDetailed map( ResultSet resultSet ) throws SQLException {
        AnnotationDetailed annotationDetailed = new AnnotationDetailed();
        annotationDetailed.setAccession( resultSet.getString( "accession" ) );
        annotationDetailed.setEdition( resultSet.getInt( "edition" ) );
        annotationDetailed.setEvidence( resultSet.getString( "evidence" ) );
        annotationDetailed.setGoId( resultSet.getString( "go_id" ) );
        annotationDetailed.setName( resultSet.getString( "db_object_name" ) );
        annotationDetailed.setReference( resultSet.getString( "reference" ) );
        annotationDetailed.setSpecies( resultSet.getInt( "species_id" ) );
        annotationDetailed.setSymbol( resultSet.getString( "symbol" ) );
        annotationDetailed.setSynonyms( Arrays.asList( resultSet.getString( "synonyms" ).split( "|" ) ) );
        String[] taxons = resultSet.getString( "taxon" ).split( "\\|" );
        annotationDetailed.setTaxon( Integer.parseInt( taxons[0].split( ":" )[1] ) );

        if ( taxons.length == 2 ) {
            annotationDetailed.setInteractingTaxon( Integer.parseInt( taxons[1].split( ":" )[1] ) );
        }

        annotationDetailed.setType( resultSet.getString( "db_object_type" ) );
        return annotationDetailed;
    }

}
