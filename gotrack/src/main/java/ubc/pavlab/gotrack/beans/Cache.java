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

package ubc.pavlab.gotrack.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityCompareMethod;
import ubc.pavlab.gotrack.beans.service.SpeciesService;
import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Aggregate;
import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationType;
import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.Dataset;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Evidence;
import ubc.pavlab.gotrack.model.GOEdition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.AccessionDTO;
import ubc.pavlab.gotrack.model.dto.AdjacencyDTO;
import ubc.pavlab.gotrack.model.dto.AggregateDTO;
import ubc.pavlab.gotrack.model.dto.AnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.EditionDTO;
import ubc.pavlab.gotrack.model.dto.EvidenceDTO;
import ubc.pavlab.gotrack.model.dto.GOEditionDTO;
import ubc.pavlab.gotrack.model.dto.GOTermDTO;
import ubc.pavlab.gotrack.model.dto.GeneDTO;
import ubc.pavlab.gotrack.model.go.GeneOntology;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.go.RelationshipType;
import ubc.pavlab.gotrack.model.hashkey.MultiKey;
import ubc.pavlab.gotrack.model.table.GeneMatches;
import ubc.pavlab.gotrack.model.table.GeneMatches.MatchType;

/**
 * NOTE: Most maps here do not require synchronicity locks as they are both read-only and accessing threads are
 * necessarily created AFTER initialization, thus prohibiting stale memory. We do, however, attempt to implement
 * concurrency wherever possible... just in case.
 * 
 * This is the application cache for most static data in GOTrack as well as cached database queries for the various
 * Views. Takes ~15 minutes for a initialization using all species on production system.
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class Cache implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -113622419234682946L;

    private static final Logger log = Logger.getLogger( Cache.class );

    private final int MAX_DATA_ENTRIES = 20;
    private final int MAX_ENRICHMENT_ENTRIES = 1000;

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    @ManagedProperty("#{speciesService}")
    private SpeciesService speciesService;

    // Maps species id -> species;
    private Map<Integer, Species> speciesCache = new ConcurrentHashMap<>();

    // Maps species id -> most current edition
    private Map<Integer, Edition> currentEditions = new ConcurrentHashMap<>();

    // Maps species id, edition id -> edition
    private Map<Integer, Map<Integer, Edition>> allEditions = new ConcurrentHashMap<>();

    // Maps GO edition id -> GO edition
    private Map<Integer, GOEdition> allGOEditions = new ConcurrentHashMap<>();

    // Most current GO edition
    private GOEdition currentGOEdition;

    // Maps species id, gene symbol -> gene
    private Map<Integer, Map<String, Gene>> speciesToSymbolGenes = new ConcurrentHashMap<>();

    // These are used for autocompletion
    // *********************************
    private Map<Integer, Multimap<String, Gene>> speciesToSecondarySymbolGenes = new ConcurrentHashMap<>();
    // TODO Consider replacing speciesToSymbolGenes with this
    private Map<Integer, RadixTree<Gene>> speciesToRadixGenes = new ConcurrentHashMap<>();
    private RadixTree<ImmutableSet<GeneOntologyTerm>> radixTerms = new ConcurrentRadixTree<ImmutableSet<GeneOntologyTerm>>(
            new DefaultCharArrayNodeFactory() );

    // *********************************

    // Most current GeneOntology
    private GeneOntology currentOntology;

    // Maps species if, edition -> aggregate
    // Holds information about aggregate statistics, such as species averages in an edition, etc
    private Map<Integer, Map<Edition, Aggregate>> aggregates = new ConcurrentHashMap<>();

    //private Map<Gene, Map<Edition, Integer>> geneRanksByInferredTermCount = new ConcurrentHashMap<>();

    // Maps species, edition, goId -> count of unique genes annotated this term or any of its children
    private Map<MultiKey, Integer> inferredAnnotationCount = new ConcurrentHashMap<>();

    // Maps species, edition, goId -> count of unique genes annotated this term only
    private Map<MultiKey, Integer> directAnnotationCount = new ConcurrentHashMap<>();

    // Maps GOEdition -> GeneOntology
    // Holds our created ontologies
    private Map<GOEdition, GeneOntology> ontologies = new ConcurrentHashMap<>();

    // Maps evidence code -> Evidence
    // Holds all evidence codes
    private Map<String, Evidence> evidenceCache = new ConcurrentHashMap<>();

    // Holds all unique evidence categories (Automatic, Author, etc).
    // TODO consider turning into objects (or possibly enum if we are sure the categories won't be changing any time soon...)
    private ImmutableSet<String> evidenceCategoryCache = null;

    /*
     * Page specific caches
     */

    // Holds data for enrichment used in EnrichmentView.
    private Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> applicationLevelEnrichmentCache = new LinkedHashMap<Gene, Map<Edition, Set<GeneOntologyTerm>>>(
            MAX_ENRICHMENT_ENTRIES + 1, 0.75F, true) {
        /**
                 * 
                 */
        private static final long serialVersionUID = -2166216884887056632L;

        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry( Map.Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> eldest ) {
            return size( ) > MAX_ENRICHMENT_ENTRIES;
        }
    };

    // Holds data for GeneView
    // TODO Does this have the potential for being merged with applicationLevelEnrichmentCache while retrieving annotation sets on the fly?
    private Map<Gene, Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>>> applicationLevelGeneCache = new LinkedHashMap<Gene, Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>>>(
            MAX_DATA_ENTRIES + 1, 0.75F, true) {
        /**
                 * 
                 */
        private static final long serialVersionUID = -6677016445725453615L;

        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry(
                Map.Entry<Gene, Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>>> eldest ) {
            return size( ) > MAX_DATA_ENTRIES;
        }
    };

    /**
     * 
     */
    public Cache() {
        log.info( "Cache created" );
    }

    /* Getters for View Static info */

    public Dataset[] getDatasets() {
        return Dataset.values();
    }

    public MultipleTestCorrection[] getMultipleTestCorrections() {
        return MultipleTestCorrection.values();
    }

    public SimilarityCompareMethod[] getSimilarityCompareMethods() {
        return SimilarityCompareMethod.values();
    }

    public Aspect[] getAspects() {
        return Aspect.values();
    }

    public AnnotationType[] getAnnotationTypes() {
        return AnnotationType.values();
    }

    /**
     * Create lots of static data caches to be used by Views.
     */
    @PostConstruct
    public void init() {
        log.info( "Cache init" );

        applicationLevelEnrichmentCache = Collections.synchronizedMap( applicationLevelEnrichmentCache );
        applicationLevelGeneCache = Collections.synchronizedMap( applicationLevelGeneCache );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );

        // Cache creation is restricted to these species (not necessarily database retrieval calls in Views though)
        int[] speciesRestrictions = settingsCache.getSpeciesRestrictions();

        if ( speciesRestrictions == null || speciesRestrictions.length == 0 ) {
            log.info( "restriction species to: " + Arrays.toString( speciesRestrictions ) );
        }

        List<Species> speciesList = speciesService.list();

        for ( Species species : speciesList ) {
            speciesCache.put( species.getId(), species );
        }

        log.info( "Species Cache successfully created: " + speciesCache );

        // Obtain CacheDAO
        CacheDAO cacheDAO = daoFactoryBean.getGotrack().getCacheDAO();
        log.info( "CacheDAO successfully obtained: " + cacheDAO );

        // GOEdition Cache creation
        // ****************************
        for ( GOEditionDTO dto : cacheDAO.getAllGOEditions() ) {
            allGOEditions.put( dto.getId(), new GOEdition( dto ) );
        }

        currentGOEdition = Collections.max( allGOEditions.values() );

        // ****************************

        // Edition Cache creation
        // ****************************

        for ( EditionDTO dto : cacheDAO.getAllEditions( speciesRestrictions ) ) {
            Map<Integer, Edition> m = allEditions.get( dto.getSpecies() );

            if ( m == null ) {
                m = new ConcurrentHashMap<>();
                allEditions.put( dto.getSpecies(), m );
            }

            GOEdition goEdition = allGOEditions.get( dto.getGoEditionId() );

            Edition ed = new Edition( dto, goEdition );
            m.put( dto.getEdition(), ed );
            // Since getAllEditions is ordered by edition the final put will always be the most current
            currentEditions.put( dto.getSpecies(), ed );
        }

        log.debug( "All Editions Size: " + allEditions.size() );
        // ****************************

        // Evidence cache creation
        // ****************************

        for ( EvidenceDTO e : cacheDAO.getEvidence() ) {
            evidenceCache.put( e.getEvidence(), new Evidence( e ) );
        }
        // ****************************

        // Evidence Category cache creation
        // ****************************
        Set<String> tmpCategories = new TreeSet<>();
        for ( Evidence e : evidenceCache.values() ) {
            tmpCategories.add( e.getCategory() );
        }

        evidenceCategoryCache = ImmutableSet.copyOf( tmpCategories );

        // ****************************

        // System.gc();
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );

        // GOTerm creation
        // ****************************
        if ( !settingsCache.isDryRun() ) {
            for ( GOTermDTO dto : cacheDAO.getGoTerms() ) {

                GOEdition goEdition = allGOEditions.get( dto.getGoEdition() );

                if ( goEdition == null ) {
                    log.error( "Cannot find GO Edition for: " + dto.getGoEdition() );
                }

                GeneOntology go = ontologies.get( goEdition );

                if ( go == null ) {
                    go = new GeneOntology( goEdition );
                    ontologies.put( goEdition, go );
                }
                go.addTerm( new GeneOntologyTerm( dto ) );
            }
            log.info( "GO Terms fetched" );

            for ( AdjacencyDTO dto : cacheDAO.getAdjacencies() ) {
                GOEdition goEdition = allGOEditions.get( dto.getGoEdition() );

                if ( goEdition == null ) {
                    log.error( "Cannot find GO Edition for: " + dto.getGoEdition() );
                }
                GeneOntology go = ontologies.get( goEdition );
                go.addRelationship( dto.getChild(), dto.getParent(), RelationshipType.valueOf( dto.getType() ) );
            }
            log.info( "GO Adjacencies fetched" );

            for ( GeneOntology go : ontologies.values() ) {
                go.freeze();
            }
            log.info( "GO Ontologies frozen" );

            System.gc();
            log.info( "GO Ontologies Loaded: " + ontologies.keySet().size() );
            log.info( "Used Memory: "
                    + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000 + " MB" );
        }
        // ****************************

        // goSetSize cache creation &
        // Aggregate Stats

        // This may or may not be the most efficient method for doing this. Getting the database to be able to create this data
        // in a reasonable amount of time has proven troublesome. Increasing startup time by 10 minutes seems better than
        // adding in a 6-10 hour process once a month.

        // ****************************
        if ( !settingsCache.isDryRun() ) {

            // *************** Reads from database ******************
            Map<Integer, Integer> mostRecentAggregateEditions = new HashMap<>();
            Map<Integer, Integer> mostRecentCountEditions = new HashMap<>();

            // Aggregate cache creation
            // ****************************
            log.info( "Attempting to create Aggregates from database cache" );
            for ( AggregateDTO dto : cacheDAO.getAggregates( speciesRestrictions ) ) {
                Map<Edition, Aggregate> m1 = aggregates.get( dto.getSpecies() );

                if ( m1 == null ) {
                    m1 = new ConcurrentHashMap<>();
                    aggregates.put( dto.getSpecies(), m1 );
                }

                Edition ed = allEditions.get( dto.getSpecies() ).get( dto.getEdition() );

                m1.put( ed, new Aggregate( dto ) );

                Integer recentEdition = mostRecentAggregateEditions.get( dto.getSpecies() );

                if ( recentEdition == null || dto.getEdition() > recentEdition ) {
                    mostRecentAggregateEditions.put( dto.getSpecies(), dto.getEdition() );
                }

            }
            // ****************************
            // Annotation Counts cache creation
            // ****************************
            log.info( "Attempting to create Annotation Counts from database cache" );
            for ( AnnotationCountDTO dto : cacheDAO.getGOAnnotationCounts( speciesRestrictions ) ) {
                MultiKey k = new MultiKey( dto );

                if ( dto.getDirectCount() != null ) {
                    Integer prev = directAnnotationCount.put( k, dto.getDirectCount() );
                    if ( prev != null ) {
                        // key existed before
                        log.warn( k );
                    }
                }

                if ( dto.getInferredCount() != null ) {
                    Integer prev = inferredAnnotationCount.put( k, dto.getInferredCount() );
                    if ( prev != null ) {
                        // key existed before
                        log.warn( k );
                    }
                }

                Integer recentEdition = mostRecentCountEditions.get( dto.getSpecies() );

                if ( recentEdition == null || dto.getEdition() > recentEdition ) {
                    mostRecentCountEditions.put( dto.getSpecies(), dto.getEdition() );
                }

            }
            // ****************************

            // ******************************************************

            // Check to see if aggregates are out of date with data
            boolean outOfDate = false;
            for ( Entry<Integer, Map<Integer, Edition>> speciesEntry : allEditions.entrySet() ) {
                Integer speciesId = speciesEntry.getKey();
                Collection<Edition> eds = speciesEntry.getValue().values();
                if ( !eds.isEmpty() ) {
                    Integer mostRecentEdition = Collections.max( eds ).getEdition();
                    // Check against aggregates and counts
                    Integer aggRecentEdition = mostRecentAggregateEditions.get( speciesId );
                    boolean ood = ( !mostRecentEdition.equals( aggRecentEdition ) );
                    if ( ood ) {
                        log.warn(
                                "Most recent edition in Aggregate Data (" + aggRecentEdition
                                        + ") does not match most recent edition in Edition Table (" + mostRecentEdition
                                        + ") for species (" + speciesId + ")" );
                    }
                    outOfDate |= ood;

                    Integer cntRecentEdition = mostRecentCountEditions.get( speciesId );
                    ood = ( !mostRecentEdition.equals( cntRecentEdition ) );
                    if ( ood ) {
                        log.warn(
                                "Most recent edition in Aggregate Count Data (" + cntRecentEdition
                                        + ") does not match most recent edition in Edition Table (" + mostRecentEdition
                                        + ") for species (" + speciesId + ")" );
                    }
                    outOfDate |= ood;
                }
            }

            // If aggregates are out of date
            if ( outOfDate ) {
                log.warn(
                        "Aggregate Data and/or Aggregate Count Data Tables appear to be in need of pre-processing, "
                                + "this will most likely break functionality somewhere" );
            }
        }
        // ****************************

        // Accession creation
        // ****************************
        Multimap<Integer, Accession> geneIdToPrimary = HashMultimap.create();
        String previousAccession = null;
        Accession.AccessionBuilder accessionBuilder = null;
        for ( AccessionDTO dto : cacheDAO.getAccessions( speciesRestrictions ) ) {
            // Iteration ordered by accession!
            String accession = dto.getAccession();
            String sec = dto.getSec();
            if ( accession.equals( previousAccession ) ) {
                // Still on same primary accession
                try {
                    accessionBuilder.secondary( sec );
                } catch ( NullPointerException e ) {
                    log.error( dto );
                    log.error( accession );
                    log.error( sec );
                }
            } else { // New primary accession

                // Create Accession object from previous one
                if ( accessionBuilder != null ) {
                    Accession acc = accessionBuilder.build();
                    geneIdToPrimary.put( acc.getGeneId(), acc );
                }

                accessionBuilder = new Accession.AccessionBuilder( dto.getGeneId(), accession, dto.getSp() );

                // Add first secondary

                if ( sec != null ) {
                    // Null secondary values means it has no secondary accessions
                    if ( !sec.equals( accession ) ) { // Remove reflexive associations that were added for performance reasons
                        accessionBuilder.secondary( sec );
                    }
                }

            }
            previousAccession = accession;
        }

        // Create Accession object from last entry
        if ( accessionBuilder != null ) {
            Accession acc = accessionBuilder.build();
            geneIdToPrimary.put( acc.getGeneId(), acc );
        }
        // ****************************

        // Gene cache creation
        // ****************************
        Set<Gene> genes = new HashSet<>();
        Integer previousGeneId = null;
        Gene.GeneBuilder geneBuilder = null;
        for ( GeneDTO dto : cacheDAO.getCurrentGenes( speciesRestrictions ) ) {
            // Iteration ordered by geneId
            Integer geneId = dto.getGeneId();
            String syn = dto.getSynonym();
            if ( geneId.equals( previousGeneId ) ) { // Still on same gene
                geneBuilder.synonym( syn );
            } else { // New Gene

                // Finalize Gene
                if ( geneBuilder != null ) {
                    genes.add( geneBuilder.build() );
                }

                Collection<Accession> accs = geneIdToPrimary.get( geneId );
                geneBuilder = new Gene.GeneBuilder( geneId, dto.getSymbol(), speciesCache.get( dto.getSpeciesId() ),
                        accs );

                // Add first synonym
                if ( syn != null ) {
                    // Null values means it has no synonyms
                    geneBuilder.synonym( syn );
                }

            }
            previousGeneId = geneId;
        }

        // Finalize Gene
        if ( geneBuilder != null ) {
            genes.add( geneBuilder.build() );
        }

        // Creating caches

        for ( Species species : speciesList ) {
            speciesToSymbolGenes.put( species.getId(), new ConcurrentHashMap<String, Gene>() );
            speciesToSecondarySymbolGenes.put( species.getId(), HashMultimap.<String, Gene> create() );
        }

        for ( Gene g : genes ) {
            Integer speciesId = g.getSpecies().getId();
            String symbol = g.getSymbol();

            // Official symbol cache
            Map<String, Gene> m = speciesToSymbolGenes.get( speciesId );
            m.put( symbol.toUpperCase(), g );

            // Secondary symbol cache
            Multimap<String, Gene> msec = speciesToSecondarySymbolGenes.get( speciesId );
            for ( String syn : g.getSynonyms() ) {
                msec.put( syn.toUpperCase(), g );
            }

        }

        log.info( "Done loading current genes..." );

        for ( Species species : speciesList ) {
            if ( speciesToSymbolGenes.keySet().contains( species.getId() ) ) {
                log.info( "Current gene size for species (" + species + "): "
                        + speciesToSymbolGenes.get( species.getId() ).size() );
            }
        }

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );

        // ****************************

        // Gene caches for auto-completion
        // ****************************

        // radix trie for symbols
        for ( Entry<Integer, Map<String, Gene>> spEntry : speciesToSymbolGenes.entrySet() ) {
            Integer speciesId = spEntry.getKey();
            RadixTree<Gene> rt = new ConcurrentRadixTree<Gene>( new DefaultCharArrayNodeFactory() );
            for ( Entry<String, Gene> symEntry : spEntry.getValue().entrySet() ) {
                rt.put( symEntry.getKey(), symEntry.getValue() );
            }
            speciesToRadixGenes.put( speciesId, rt );
        }

        // Raddix trie for terms
        if ( !settingsCache.isDryRun() ) {
            currentOntology = ontologies.get( currentGOEdition );
            Multimap<String, GeneOntologyTerm> wordsToTerms = HashMultimap.create();
            for ( GeneOntologyTerm t : currentOntology.getAllTerms() ) {
                String name = t.getName();
                String[] words = name.split( "\\s" );
                for ( int i = 0; i < words.length; i++ ) {
                    String w = words[i];
                    if ( w.length() > 2 ) {
                        wordsToTerms.put( w.toUpperCase(), t );
                        // radixWords.put( w.toUpperCase(), t );
                    }

                }

            }
            for ( Entry<String, Collection<GeneOntologyTerm>> entry : wordsToTerms.asMap().entrySet() ) {
                radixTerms.put( entry.getKey(), ImmutableSet.copyOf( entry.getValue() ) );

            }
        }

        log.info( "Done loading Gene caches for autocompletion..." );
        // ****************************

        log.info( "Cache Completed" );

    }

    /**
     * Autocompletes Gene Ontology Terms by id or name
     * 
     * @param query query
     * @param maxResults max results
     * @param retain Whether to take intersection of results (true) or union of results (false)
     * @return list of terms in order goodness
     */
    public List<GeneOntologyTerm> completeTerm( String query, int maxResults, boolean retain ) {
        if ( query == null || maxResults < 1 ) return Lists.newArrayList();
        Set<GeneOntologyTerm> results = new HashSet<>();
        String queryUpper = query.toUpperCase();
        // Find exact match
        GeneOntologyTerm t = null;
        if ( queryUpper.startsWith( "GO:" ) ) {
            t = currentOntology.getTerm( queryUpper );
        } else {
            try {
                int id = Integer.parseInt( queryUpper );
                t = currentOntology.getTerm( id );
            } catch ( NumberFormatException e ) {
                // pass
            }

        }

        if ( t != null ) {
            results.add( t );
            return new ArrayList<>( results );
        }

        // split into words
        String[] words = queryUpper.split( "\\s" );

        for ( String w : words ) {
            Set<GeneOntologyTerm> currentWordResults = new HashSet<>();
            // Find prefix matches
            boolean findSimilarMatches = true;
            if ( radixTerms != null ) {
                Iterable<ImmutableSet<GeneOntologyTerm>> gs = radixTerms.getValuesForKeysStartingWith( w );
                if ( gs.iterator().hasNext() ) {
                    // If prefix matches were found, do not return similar matches
                    findSimilarMatches = false;
                }
                for ( Iterator<ImmutableSet<GeneOntologyTerm>> iterator = gs.iterator(); iterator.hasNext(); ) {
                    ImmutableSet<GeneOntologyTerm> terms = iterator.next();
                    for ( GeneOntologyTerm term : terms ) {
                        currentWordResults.add( term );
                    }

                }

                // Find similar matches
                if ( retain || findSimilarMatches ) {
                    gs = radixTerms.getValuesForClosestKeys( w );
                    for ( Iterator<ImmutableSet<GeneOntologyTerm>> iterator = gs.iterator(); iterator.hasNext(); ) {
                        ImmutableSet<GeneOntologyTerm> terms = iterator.next();
                        for ( GeneOntologyTerm term : terms ) {
                            currentWordResults.add( term );
                        }
                    }
                }

                if ( !retain || results.isEmpty() ) {
                    results.addAll( currentWordResults );
                } else if ( retain && !currentWordResults.isEmpty() ) {
                    results.retainAll( currentWordResults );
                }

            }
        }

        List<GeneOntologyTerm> p = new ArrayList<>( results );
        Collections.sort( p, new LevenshteinComparator( query ) );

        if ( p.size() > maxResults ) {
            // If there are more than maxResults, remove the excess range
            p.subList( maxResults, p.size() ).clear();
        }

        return p;
    }

    /**
     * Autocompletes genes by symbol
     * 
     * @param query query
     * @param species species
     * @param maxResults max results to return
     * @return list of matches in order of goodness
     */
    public List<GeneMatches> complete( String query, Integer species, Integer maxResults ) {
        List<GeneMatches> results = new ArrayList<>();
        Set<String> duplicateChecker = new HashSet<>();
        if ( query == null || maxResults < 1 ) return results;

        String queryUpper = query.toUpperCase();

        // Find exact match
        Map<String, Gene> m = speciesToSymbolGenes.get( species );
        if ( m != null ) {
            Gene g = m.get( queryUpper );
            if ( g != null ) {
                results.add( new GeneMatches( query, g, MatchType.EXACT ) );
                duplicateChecker.add( g.getSymbol() );
            }
        }

        if ( results.size() >= maxResults ) {
            return results;
        }

        // Find exact synonyms
        Multimap<String, Gene> msec = speciesToSecondarySymbolGenes.get( species );
        if ( msec != null ) {
            Collection<Gene> gs = msec.get( queryUpper );
            MatchType type = gs.size() > 1 ? MatchType.MULTIPLE_EXACT_SYNONYMS : MatchType.EXACT_SYNONYM;
            for ( Gene gene : gs ) {
                if ( !duplicateChecker.contains( gene.getSymbol() ) ) {
                    results.add( new GeneMatches( query, gene, type ) );
                    duplicateChecker.add( gene.getSymbol() );
                }
                if ( results.size() >= maxResults ) {
                    return results;
                }
            }
        }

        // Find prefix matches
        RadixTree<Gene> rt = speciesToRadixGenes.get( species );
        boolean findSimilarMatches = true;
        if ( rt != null ) {
            Iterable<Gene> gs = rt.getValuesForKeysStartingWith( queryUpper );
            if ( gs.iterator().hasNext() ) {
                // If prefix matches were found, do not return similar matches
                findSimilarMatches = false;
            }
            for ( Iterator<Gene> iterator = gs.iterator(); iterator.hasNext(); ) {
                Gene gene = iterator.next();
                if ( !duplicateChecker.contains( gene.getSymbol() ) ) {
                    results.add( new GeneMatches( query, gene, MatchType.PREFIX ) );
                    duplicateChecker.add( gene.getSymbol() );
                }
                if ( results.size() >= maxResults ) {
                    return results;
                }
            }

            // Find similar matches
            if ( findSimilarMatches ) {
                gs = rt.getValuesForClosestKeys( queryUpper );
                for ( Iterator<Gene> iterator = gs.iterator(); iterator.hasNext(); ) {
                    Gene gene = iterator.next();
                    if ( !duplicateChecker.contains( gene.getSymbol() ) ) {
                        results.add( new GeneMatches( query, gene, MatchType.SIMILAR ) );
                        duplicateChecker.add( gene.getSymbol() );
                    }
                    if ( results.size() >= maxResults ) {
                        return results;
                    }
                }
            }
        }

        return results;
    }

    /**
     * list of all species
     */
    public Collection<Species> getSpeciesList() {
        return Collections.unmodifiableCollection( speciesCache.values() );
    }

    /**
     * @param speciesId species id
     * @return most current edition for this species
     */
    public Edition getCurrentEditions( Integer speciesId ) {
        return currentEditions.get( speciesId );
    }

    /**
     * @param speciesId
     * @return UNORDERED collection of all editions for this species
     */
    public Collection<Edition> getAllEditions( Integer speciesId ) {
        Map<Integer, Edition> tmp = allEditions.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableCollection( tmp.values() );
        } else {
            return null;
        }
    }

    /**
     * @param speciesId species id
     * @param edition edition
     * @return fetch edition from cache or null if not there
     */
    public Edition getEdition( Integer speciesId, Integer edition ) {
        if ( speciesId == null || edition == null ) {
            return null;
        }
        Map<Integer, Edition> tmp = allEditions.get( speciesId );
        if ( tmp != null ) {
            return tmp.get( edition );
        } else {
            return null;
        }
    }

    /**
     * @param speciesId species id
     * @param ed edition
     * @param t term
     * @return count of genes annotated with this term or any of its children
     */
    public Integer getInferredAnnotationCount( Integer speciesId, Edition ed, GeneOntologyTerm t ) {
        if ( speciesId == null || ed == null || t == null ) return null;
        MultiKey k = new MultiKey( speciesId, ed, t );
        return inferredAnnotationCount.get( k );

    }

    /**
     * @param speciesId species id
     * @param ed edition
     * @param t term
     * @return count of genes annotated with this term
     */
    public Integer getDirectAnnotationCount( Integer speciesId, Edition ed, GeneOntologyTerm t ) {
        if ( speciesId == null || ed == null || t == null ) return null;
        MultiKey k = new MultiKey( speciesId, ed, t );
        return directAnnotationCount.get( k );

    }

    /**
     * @param speciesId species id
     * @return map of symbol to gene for all current genes in a species
     */
    public Map<String, Gene> getSpeciesToCurrentGenes( Integer speciesId ) {
        Map<String, Gene> tmp = speciesToSymbolGenes.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableMap( tmp );
        } else {
            return null;
        }
    }

    //    public Map<Edition, Aggregate> getAggregates( Integer speciesId ) {
    //        if ( speciesId == null ) return null;
    //        Map<Edition, Aggregate> tmp = aggregates.get( speciesId );
    //        if ( tmp != null ) {
    //            return Collections.unmodifiableMap( tmp );
    //        } else {
    //            return null;
    //        }
    //
    //    }

    /**
     * @param speciesId species id
     * @param ed edition
     * @return aggregate
     */
    public Aggregate getAggregates( Integer speciesId, Edition ed ) {
        if ( speciesId == null || ed == null ) return null;
        Map<Edition, Aggregate> tmp = aggregates.get( speciesId );
        if ( tmp != null ) {
            return tmp.get( ed );
        }
        return null;

    }

    /**
     * @param speciesId species id
     * @param symbol gene symbol
     * @return gene with this symbol from this species or null
     */
    public Gene getCurrentGene( Integer speciesId, String symbol ) {
        if ( speciesId == null || symbol == null ) {
            return null;
        }
        Map<String, Gene> map2 = speciesToSymbolGenes.get( speciesId );
        if ( map2 != null ) {
            return map2.get( symbol.toUpperCase() );
        }
        return null;
    }

    /**
     * @param speciesId species id
     * @param symbol gene symbol
     * @return set of genes with this symbol as a synonym
     */
    public Set<Gene> getCurrentGeneBySynonym( Integer speciesId, String symbol ) {
        if ( speciesId == null || symbol == null ) {
            return null;
        }

        String symbolUpper = symbol.toUpperCase();
        Set<Gene> exactSynonym = new HashSet<>();

        Multimap<String, Gene> gs = speciesToSecondarySymbolGenes.get( speciesId );
        if ( gs != null ) {
            exactSynonym = new HashSet<>( gs.get( symbolUpper ) );
        }

        if ( exactSynonym.size() > 1 ) {
            log.warn( "Secondary symbol (" + symbol + ") has multiple associated genes (" + exactSynonym + ")" );
        }

        return exactSynonym;
    }

    /**
     * @param speciesId species id
     * @param symbol gene symbol
     * @return true if gene exists with this symbol else false
     */
    public boolean currentSymbolExists( Integer speciesId, String symbol ) {
        if ( speciesId == null || symbol == null ) {
            return false;
        }
        Map<String, Gene> map2 = speciesToSymbolGenes.get( speciesId );
        if ( map2 != null ) {
            return map2.containsKey( symbol.toUpperCase() );
        }
        return false;
    }

    //    public Integer getAccessionCount( Integer speciesId, Edition edition ) {
    //        if ( speciesId == null || edition == null ) {
    //            return null;
    //        }
    //        Map<Edition, StatsEntry> map2 = aggregates.get( speciesId );
    //        if ( map2 != null ) {
    //            StatsEntry se = map2.get( edition );
    //            if ( se != null ) {
    //                return se.getAccessionCount();
    //            }
    //
    //        }
    //        return null;
    //    }

    /**
     * @param speciesId species id
     * @param edition edition
     * @return Count of unique genes in this edition under this species
     */
    public Integer getGeneCount( Integer speciesId, Edition edition ) {
        if ( speciesId == null || edition == null ) {
            return null;
        }
        Map<Edition, Aggregate> map2 = aggregates.get( speciesId );
        if ( map2 != null ) {
            Aggregate se = map2.get( edition );
            if ( se != null ) {
                return se.getGeneCount();
            }

        }
        return null;
    }

    /**
     * Propagates terms and their annotations to parents terms
     * 
     * @param map map of term to a set of annotation for that term
     * @param ed edition
     * @return Map of all propagated terms to their propagated annotations
     */
    public Map<GeneOntologyTerm, Set<Annotation>> propagateAnnotations( Map<GeneOntologyTerm, Set<Annotation>> map,
            Edition ed ) {
        if ( map == null || ed == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( ed.getGoEdition() );
        if ( o != null ) {
            return o.propagateAnnotations( map );
        }
        return null;
    }

    /**
     * Propagates terms up the ontology
     * 
     * @param terms set of terms to propagate
     * @param ed edition
     * @return set of propagated terms
     */
    public Set<GeneOntologyTerm> propagate( Set<GeneOntologyTerm> terms, Edition ed ) {
        if ( terms == null || ed == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( ed.getGoEdition() );
        if ( o != null ) {
            return o.propagate( terms );
        }
        return null;
    }

    /**
     * Propagates a single term up the ontology
     * 
     * @param term term to propagate
     * @param goEd GO edition
     * @return set of propagated terms
     */
    public Set<GeneOntologyTerm> propagate( GeneOntologyTerm term, GOEdition goEd ) {
        if ( term == null || goEd == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( goEd );
        if ( o != null ) {
            return o.getAncestors( term, true, null );
        }
        return null;
    }

    // public void ontologyStats() {
    // for ( GeneOntology o : ontologies.values() ) {
    // o.getCacheStats();
    // }
    //
    // }

    /**
     * @return most current GO edition
     */
    public GOEdition getCurrentGOEdition() {
        return currentGOEdition;
    }

    // cache retrieval getters

    public Evidence getEvidence( String evidence ) {
        if ( evidence == null ) return null;
        return evidenceCache.get( evidence );
    }

    public Set<String> getEvidenceCategories() {
        return evidenceCategoryCache;
    }

    public Species getSpecies( Integer id ) {
        if ( id == null ) return null;
        return speciesCache.get( id );

    }

    public GeneOntologyTerm getTerm( Edition ed, Integer id ) {
        if ( id == null || ed == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( ed.getGoEdition() );
        if ( o != null ) {
            return o.getTerm( id );
        }
        return null;
    }

    public GeneOntologyTerm getTerm( Edition ed, String goId ) {
        if ( goId == null || ed == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( ed.getGoEdition() );
        if ( o != null ) {
            return o.getTerm( goId );
        }
        return null;
    }

    /**
     * Will always contain ever GO Edition in keyset as editions where the term did not exist will have null values
     * 
     * @param goId
     * @return map of GO edition -> term or null if this GO edition did not contains said term.
     */
    public Map<GOEdition, GeneOntologyTerm> getTerm( String goId ) {
        if ( goId == null ) {
            return null;
        }

        Map<GOEdition, GeneOntologyTerm> termsMap = new HashMap<>();

        boolean found = false;

        for ( Entry<GOEdition, GeneOntology> entry : ontologies.entrySet() ) {
            GeneOntologyTerm term = entry.getValue().getTerm( goId );
            // yes we want null values
            termsMap.put( entry.getKey(), term );
            found |= ( term != null );
        }

        return found ? termsMap : null;
    }

    /**
     * @param goId
     * @return most current term with this goId
     */
    public GeneOntologyTerm getCurrentTerm( String goId ) {
        if ( goId == null ) {
            return null;
        }
        if ( currentOntology != null ) {
            return currentOntology.getTerm( goId );
        }

        return null;

    }

    /**
     * @param goId
     * @return true if any edition contains a term under this goId else false
     */
    public boolean termExists( String goId ) {
        if ( goId == null ) {
            return false;
        }

        for ( Entry<GOEdition, GeneOntology> entry : ontologies.entrySet() ) {
            GeneOntologyTerm term = entry.getValue().getTerm( goId );
            if ( term != null ) {
                return true;
            }

        }

        return false;
    }

    public GOEdition getGOEdition( Integer edId ) {
        if ( edId == null ) return null;
        return allGOEditions.get( edId );
    }

    // Application Level Caching get/set

    /**
     * @param g gene
     * @return cached data for GeneView under given gene
     */
    public Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> getGeneData( Gene g ) {
        // TODO not sure if necessary, not a big deal either way
        synchronized ( applicationLevelGeneCache ) {
            return applicationLevelGeneCache.get( g );
        }
    }

    /**
     * Add data to GeneView cache under given gene
     * 
     * @param g gene to cache data under
     * @param data data to be cached
     */
    public void addGeneData( Gene g,
            Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> data ) {
        synchronized ( applicationLevelGeneCache ) {
            applicationLevelGeneCache.put( g, data );
        }
    }

    /**
     * @param gene
     * @return cache data for EnrichmentView under given gene
     */
    public Map<Edition, Set<GeneOntologyTerm>> getEnrichmentData( Gene gene ) {
        // TODO not sure if necessary, not a big deal either way
        synchronized ( applicationLevelEnrichmentCache ) {
            return applicationLevelEnrichmentCache.get( gene );
        }
    }

    /**
     * Add data to EnrichmentView cache under given gene
     * 
     * @param g gene to cache data under
     * @param data data to be cached
     */
    public void addEnrichmentData( Gene gene, Map<Edition, Set<GeneOntologyTerm>> data ) {
        synchronized ( applicationLevelEnrichmentCache ) {
            applicationLevelEnrichmentCache.put( gene, data );
        }
    }

    // Bean Injection

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

    public void setSpeciesService( SpeciesService speciesService ) {
        this.speciesService = speciesService;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

}

/**
 * Comparator to order autocompleted terms based on similarity to query
 * 
 * @author mjacobson
 * @version $Id$
 */
class LevenshteinComparator implements Comparator<GeneOntologyTerm> {

    private String compareTo;

    /**
     * @param compareTo this is the string which every item in a list will be compared to for an ordering
     */
    public LevenshteinComparator( String compareTo ) {
        super();
        this.compareTo = compareTo;
    }

    @Override
    public int compare( GeneOntologyTerm a, GeneOntologyTerm b ) {
        int d1 = StringUtils.getLevenshteinDistance( a.getName(), compareTo );
        int d2 = StringUtils.getLevenshteinDistance( b.getName(), compareTo );
        return d1 < d2 ? -1 : d1 == d2 ? 0 : 1;
    }
}
