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

import com.google.common.collect.*;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.omnifaces.cdi.Eager;
import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityCompareMethod;
import ubc.pavlab.gotrack.beans.service.SpeciesService;
import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.dto.*;
import ubc.pavlab.gotrack.model.go.GeneOntology;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.go.RelationshipType;
import ubc.pavlab.gotrack.model.hashkey.MultiKey;
import ubc.pavlab.gotrack.model.search.GeneMatch;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NOTE: Most maps here do not require synchronicity locks as they are both read-only and accessing threads are
 * necessarily created AFTER initialization, thus prohibiting stale memory. We do, however, attempt to implement
 * concurrency wherever possible... just in case.
 * <p>
 * This is the application cache for most static data in GOTrack as well as cached database queries for the various
 * Views. Takes ~15 minutes for a initialization using all species on production system.
 *
 * @author mjacobson
 * @version $Id$
 */
@Named
@Eager
@ApplicationScoped
public class Cache implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -113622419234682946L;

    private static final Logger log = Logger.getLogger( Cache.class );

    private final int MAX_DATA_ENTRIES = 20;
    private final int MAX_ENRICHMENT_ENTRIES = 1000;

    @Inject
    private SettingsCache settingsCache;

    @Inject
    private DAOFactoryBean daoFactoryBean;

    @Inject
    private SpeciesService speciesService;

    // Maps species id -> species;
    private Map<Integer, Species> speciesCache = new ConcurrentHashMap<>();

    // Maps species id -> most current edition
    private Map<Species, Edition> currentEditions = new ConcurrentHashMap<>();

    // Maps species id, edition id -> edition
    private Map<Species, Map<Integer, Edition>> allEditions = new ConcurrentHashMap<>();

    // Maps GO edition id -> GO edition
    private Map<Integer, GOEdition> allGOEditions = new ConcurrentHashMap<>();

    // Most current GO edition
    private GOEdition currentGOEdition;

    // Maps gene id to gene
    private Map<String, Gene> accessionToGene = new ConcurrentHashMap<>();

    // These are used for autocompletion
    // *********************************

    private Map<Species, RadixTree<ImmutableSet<Gene>>> speciesToPrimaryRadixGenes = new ConcurrentHashMap<>();
    private Map<Species, RadixTree<ImmutableSet<Gene>>> speciesToSecondaryRadixGenes = new ConcurrentHashMap<>();
    private RadixTree<ImmutableSet<GeneOntologyTerm>> radixTerms = new ConcurrentRadixTree<>( new DefaultCharArrayNodeFactory() );

    // *********************************

    // Most current GeneOntology
    private GeneOntology currentOntology;

    // Most current GO Term definitions
    private Map<String, String> geneOntologyDefinitions = new ConcurrentHashMap<>();

    // Maps species if, edition -> aggregate
    // Holds information about aggregate statistics, such as species averages in an edition, etc
    private Map<Species, Map<Edition, Aggregate>> aggregates = new ConcurrentHashMap<>();

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
            MAX_ENRICHMENT_ENTRIES + 1, 0.75F, true ) {
        /**
         *
         */
        private static final long serialVersionUID = -2166216884887056632L;

        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry( Map.Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> eldest ) {
            return size() > MAX_ENRICHMENT_ENTRIES;
        }
    };

    // Holds data for GeneView
    // TODO Does this have the potential for being merged with applicationLevelEnrichmentCache while retrieving annotation sets on the fly?
    private Map<Gene, Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>>> applicationLevelGeneCache = new LinkedHashMap<Gene, Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>>>(
            MAX_DATA_ENTRIES + 1, 0.75F, true ) {
        /**
         *
         */
        private static final long serialVersionUID = -6677016445725453615L;

        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry(
                Map.Entry<Gene, Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>>> eldest ) {
            return size() > MAX_DATA_ENTRIES;
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

        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
                + " MB" );

        // Cache creation is restricted to these species (not necessarily database retrieval calls in Views though)
        int[] speciesRestrictions = settingsCache.getSpeciesRestrictions();

        if ( speciesRestrictions == null || speciesRestrictions.length == 0 ) {
            log.info( "restriction species to: " + Arrays.toString( speciesRestrictions ) );
        }

        List<Species> speciesList = speciesService.list();

        for ( Species species : speciesList ) {
            // TODO: restrict this list as well?
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
            Species species = speciesCache.get( dto.getSpecies() );
            Map<Integer, Edition> m = allEditions.get( species );

            if ( m == null ) {
                m = new ConcurrentHashMap<>();
                allEditions.put( species, m );
            }

            GOEdition goEdition = allGOEditions.get( dto.getGoEditionId() );

            Edition ed = new Edition( dto, goEdition );
            m.put( dto.getEdition(), ed );
            // Since getAllEditions is ordered by edition the final put will always be the most current
            currentEditions.put( species, ed );
        }

        // TODO: data-format - load current editions via pp_current_edition

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
        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
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

            for ( AdjacencyDTO dto : cacheDAO.getAlternates() ) {
                GOEdition goEdition = allGOEditions.get( dto.getGoEdition() );

                if ( goEdition == null ) {
                    log.error( "Cannot find GO Edition for: " + dto.getGoEdition() );
                }
                GeneOntology go = ontologies.get( goEdition );
                go.addAlt( dto.getChild(), dto.getParent() );
            }
            log.info( "GO Alternates fetched" );

            for ( GODefinitionDTO dto : cacheDAO.getGODefinitions() ) {
                geneOntologyDefinitions.put( dto.getGoId(), dto.getDefinition() );
            }
            log.info( "GO Definitions fetched" );

            for ( GeneOntology go : ontologies.values() ) {
                go.freeze();
            }
            log.info( "GO Ontologies frozen" );

            System.gc();
            log.info( "GO Ontologies Loaded: " + ontologies.keySet().size() );
            log.info( "Used Memory: "
                    + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + " MB" );
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
            Map<Species, Integer> mostRecentAggregateEditions = new HashMap<>();
            Map<Species, Integer> mostRecentCountEditions = new HashMap<>();

            // Aggregate cache creation
            // ****************************
            log.info( "Attempting to create Aggregates from database cache" );
            for ( AggregateDTO dto : cacheDAO.getAggregates( speciesRestrictions ) ) {
                Species species = speciesCache.get( dto.getSpecies() );
                Map<Edition, Aggregate> m1 = aggregates.get( species );

                if ( m1 == null ) {
                    m1 = new ConcurrentHashMap<>();
                    aggregates.put( species, m1 );
                }

                Edition ed = allEditions.get( species ).get( dto.getEdition() );

                m1.put( ed, new Aggregate( dto ) );

                Integer recentEdition = mostRecentAggregateEditions.get( species );

                if ( recentEdition == null || dto.getEdition() > recentEdition ) {
                    mostRecentAggregateEditions.put( species, dto.getEdition() );
                }

            }
            // ****************************
            // Annotation Counts cache creation
            // ****************************
            log.info( "Attempting to create Annotation Counts from database cache" );
            for ( AnnotationCountDTO dto : cacheDAO.getGOAnnotationCounts( speciesRestrictions ) ) {
                Species species = speciesCache.get( dto.getSpecies() );
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

                Integer recentEdition = mostRecentCountEditions.get( species );

                if ( recentEdition == null || dto.getEdition() > recentEdition ) {
                    mostRecentCountEditions.put( species, dto.getEdition() );
                }

            }
            // ****************************

            // ******************************************************

            // Check to see if aggregates are out of date with data
            boolean outOfDate = false;
            for ( Species species : allEditions.keySet() ) {
                Edition mostRecentEdition = currentEditions.get( species );

                // Check against aggregates and counts
                Integer aggRecentEdition = mostRecentAggregateEditions.get( species );
                boolean ood = (!aggRecentEdition.equals( mostRecentEdition.getEdition() ));
                if ( ood ) {
                    log.warn(
                            "Most recent edition in Aggregate Data (" + aggRecentEdition
                                    + ") does not match most recent edition in Edition Table (" + mostRecentEdition
                                    + ") for species (" + species + ")" );
                }
                outOfDate |= ood;

                Integer cntRecentEdition = mostRecentCountEditions.get( species );
                ood = (!cntRecentEdition.equals( mostRecentEdition.getEdition() ));
                if ( ood ) {
                    log.warn(
                            "Most recent edition in Aggregate Count Data (" + cntRecentEdition
                                    + ") does not match most recent edition in Edition Table (" + mostRecentEdition
                                    + ") for species (" + species + ")" );
                }
                outOfDate |= ood;

            }

            // If aggregates are out of date
            if ( outOfDate ) {
                log.warn(
                        "Aggregate Data and/or Aggregate Count Data Tables appear to be in need of pre-processing, "
                                + "this will most likely break functionality somewhere" );
            }
        }
        // ****************************

        // ****************************
        // Accession and Gene creation

        Map<Integer, Gene.GeneBuilder> geneBuilders = Maps.newHashMap();
        for ( AccessionDTO dto : cacheDAO.getAccessions( speciesRestrictions ) ) {
            Accession accession = new Accession( dto );

            // Need synonyms to complete the genes
            geneBuilders.put( dto.getId(), new Gene.GeneBuilder( dto.getId(), dto.getSymbol(), dto.getName(), speciesCache.get( dto.getSpeciesId() ), accession ) );
        }

        // Fill in synonyms
        for ( SynonymDTO dto : cacheDAO.getSynonyms( speciesRestrictions ) ) {
            Gene.GeneBuilder gb = geneBuilders.get( dto.getId() );
            gb.synonym( dto.getSynonym() );
        }

        // Builds genes
        for ( Gene.GeneBuilder gb : geneBuilders.values() ) {
            Gene gene = gb.build();
            accessionToGene.put( gene.getAccession().getAccession(), gene );
        }
        log.info( "Done loading current genes..." );
        log.info( "Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000
                + " MB" );
        // ****************************

        // ****************************
        // Creating Gene caches for auto-completion
        Map<Species, Multimap<String, Gene>> speciesToPrimarySymbolGenes = Maps.newHashMap();
        Map<Species, Multimap<String, Gene>> speciesToSecondarySymbolGenes = Maps.newHashMap();

        for ( Species species : speciesCache.values() ) {
            speciesToPrimarySymbolGenes.put( species, HashMultimap.<String, Gene>create() );
            speciesToSecondarySymbolGenes.put( species, HashMultimap.<String, Gene>create() );
        }

        // Group genes by species and symbol
        for ( Gene g : accessionToGene.values() ) {

            // Primary symbol cache
            if ( !StringUtils.isEmpty( g.getSymbol() ) ) {
                speciesToPrimarySymbolGenes.get( g.getSpecies() ).put( g.getSymbol().toUpperCase(), g );
            }

            // Secondary symbol cache
            Multimap<String, Gene> msec = speciesToSecondarySymbolGenes.get( g.getSpecies() );
            for ( String syn : g.getSynonyms() ) {
                msec.put( syn.toUpperCase(), g );
            }

        }

        // radix trie for primary symbols
        for ( Species species : speciesCache.values() ) {
            Multimap<String, Gene> mm = speciesToPrimarySymbolGenes.get( species );
            if ( mm != null ) {
                log.info( "Current gene size for species (" + species + "): " + mm.size() );

                RadixTree<ImmutableSet<Gene>> rt = new ConcurrentRadixTree<>( new DefaultCharArrayNodeFactory() );

                for ( Entry<String, Collection<Gene>> symEntry : mm.asMap().entrySet() ) {
                    rt.put( symEntry.getKey(), ImmutableSet.copyOf( symEntry.getValue() ) );
                }
                speciesToPrimaryRadixGenes.put( species, rt );
            }
        }

        // radix trie for secondary symbols
        for ( Species species : speciesCache.values() ) {
            Multimap<String, Gene> mm = speciesToSecondarySymbolGenes.get( species );
            if ( mm != null ) {

                RadixTree<ImmutableSet<Gene>> rt = new ConcurrentRadixTree<>( new DefaultCharArrayNodeFactory() );

                for ( Entry<String, Collection<Gene>> symEntry : mm.asMap().entrySet() ) {
                    rt.put( symEntry.getKey(), ImmutableSet.copyOf( symEntry.getValue() ) );
                }
                speciesToSecondaryRadixGenes.put( species, rt );
            }
        }

        // ****************************

        // ****************************
        // Creating Term caches for auto-completion

        // Radix trie for terms
        if ( !settingsCache.isDryRun() ) {
            currentOntology = ontologies.get( currentGOEdition );
            Multimap<String, GeneOntologyTerm> wordsToTerms = HashMultimap.create();
            for ( GeneOntologyTerm t : currentOntology.getAllTerms() ) {
                String name = t.getName();
                String[] words = name.split( "\\s" );
                for ( String w : words ) {
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

        log.info( "Done loading caches for autocompletion..." );
        // ****************************

        log.info( "Cache Completed" );

    }

    /**
     * Autocompletes Gene Ontology Terms by id or name
     *
     * @param query      query
     * @param maxResults max results
     * @param retain     Whether to take intersection of results (true) or union of results (false)
     * @return list of terms in order goodness
     */
    public List<GeneOntologyTerm> completeTerm( String query, int maxResults, boolean retain ) {
        // TODO: Clean this up
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
            } catch (NumberFormatException e) {
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

    public Set<GeneMatch> searchGeneBySymbol( String query, Species species, Integer fuzzyLimit ) {
        return searchGeneBySymbol( query, species, fuzzyLimit, null );
    }

    /**
     * Searches for a gene by its symbols
     *
     * @param query      query
     * @param species    species
     * @param fuzzyLimit Stop looking for more general matches after this threshold has been reached
     * @return list of matches in order of goodness
     */
    public Set<GeneMatch> searchGeneBySymbol( String query, Species species, Integer fuzzyLimit, GeneMatch.Level worstMatchLevel ) {
        Set<GeneMatch> results = Sets.newLinkedHashSet();
        if ( query == null || species == null || fuzzyLimit < 1 ) return results;
        worstMatchLevel = worstMatchLevel == null ? GeneMatch.Level.NO_MATCH : worstMatchLevel;

        String queryUpper = query.toUpperCase();

        RadixTree<ImmutableSet<Gene>> primaryRadix = speciesToPrimaryRadixGenes.get( species );
        RadixTree<ImmutableSet<Gene>> secondaryRadix = speciesToSecondaryRadixGenes.get( species );


        results.addAll( searchGeneByExactMatch( queryUpper, primaryRadix ) );
        if ( worstMatchLevel.equals( GeneMatch.Level.PRIMARY ) || results.size() >= fuzzyLimit ) return results;

        results.addAll( searchGeneByExactMatch( queryUpper, secondaryRadix ) );
        if ( worstMatchLevel.equals( GeneMatch.Level.SYNONYM ) || results.size() >= fuzzyLimit ) return results;

        Set<GeneMatch> prefix = searchGeneByPrefixMatch( queryUpper, primaryRadix );
        results.addAll( prefix );
        if ( worstMatchLevel.equals( GeneMatch.Level.PREFIX ) || results.size() >= fuzzyLimit ) return results;

        if ( prefix.isEmpty() ) {
            results.addAll( searchGeneBySimilarMatch( queryUpper, primaryRadix ) );
        }

        return results;
    }

    /**
     * Guess a gene by its symbol
     *
     * @param query   query
     * @param species species
     * @return A single GeneMatch with a best guess at the desired Gene
     */
    public GeneMatch guessGeneBySymbol( String query, Species species ) {
        if ( query == null || species == null ) return null;

        Set<GeneMatch> matches = searchGeneBySymbol( query, species, 1, GeneMatch.Level.SYNONYM );
        if ( matches.isEmpty() ) {
            return new GeneMatch( query, null, GeneMatch.Level.NO_MATCH, GeneMatch.Type.NO_MATCH );
        }
        return matches.iterator().next();
    }

    private Set<GeneMatch> searchGeneByExactMatch( String query, RadixTree<ImmutableSet<Gene>> radix ) {
        Set<GeneMatch> results = Sets.newHashSet();
        if ( StringUtils.isEmpty( query ) || radix == null ) {
            return results;
        }

        ImmutableSet<Gene> genes = radix.getValueForExactKey( query );
        if ( genes != null ) {
            GeneMatch.Type type = genes.size() > 1 ? GeneMatch.Type.MULTIPLE : GeneMatch.Type.SINGLE;

            for ( Gene gene : genes ) {
                results.add( new GeneMatch( query, gene, GeneMatch.Level.PRIMARY, type ) );
            }
        }
        return results;
    }

    private Set<GeneMatch> searchGeneByPrefixMatch( String query, RadixTree<ImmutableSet<Gene>> radix ) {
        Set<GeneMatch> results = Sets.newHashSet();
        if ( StringUtils.isEmpty( query ) || radix == null ) {
            return results;
        }

        List<ImmutableSet<Gene>> geneSets = Lists.newArrayList( radix.getValuesForKeysStartingWith( query ) );
        GeneMatch.Type type = geneSets.size() > 1 ? GeneMatch.Type.MULTIPLE : GeneMatch.Type.SINGLE;

        for ( ImmutableSet<Gene> genes : geneSets ) {
            type = genes.size() > 1 ? GeneMatch.Type.MULTIPLE : type;
            for ( Gene gene : genes ) {
                results.add( new GeneMatch( query, gene, GeneMatch.Level.PREFIX, type ) );

            }
        }

        return results;
    }

    private Set<GeneMatch> searchGeneBySimilarMatch( String query, RadixTree<ImmutableSet<Gene>> radix ) {
        Set<GeneMatch> results = Sets.newHashSet();
        if ( StringUtils.isEmpty( query ) || radix == null ) {
            return results;
        }

        List<ImmutableSet<Gene>> geneSets = Lists.newArrayList( radix.getValuesForClosestKeys( query ) );
        GeneMatch.Type type = geneSets.size() > 1 ? GeneMatch.Type.MULTIPLE : GeneMatch.Type.SINGLE;

        for ( ImmutableSet<Gene> genes : geneSets ) {
            type = genes.size() > 1 ? GeneMatch.Type.MULTIPLE : type;
            for ( Gene gene : genes ) {
                results.add( new GeneMatch( query, gene, GeneMatch.Level.SIMILAR, type ) );

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
     * @param species species
     * @return most current edition for this species
     */
    public Edition getCurrentEditions( Species species ) {
        return currentEditions.get( species );
    }

    /**
     * @param species
     * @return UNORDERED collection of all editions for this species
     */
    public Collection<Edition> getAllEditions( Species species ) {
        Map<Integer, Edition> tmp = allEditions.get( species );
        if ( tmp != null ) {
            return Collections.unmodifiableCollection( tmp.values() );
        } else {
            return null;
        }
    }

    /**
     * @param species species
     * @param edition edition
     * @return fetch edition from cache or null if not there
     */
    public Edition getEdition( Species species, Integer edition ) {
        if ( species == null || edition == null ) {
            return null;
        }
        Map<Integer, Edition> tmp = allEditions.get( species );
        if ( tmp != null ) {
            return tmp.get( edition );
        } else {
            return null;
        }
    }

    /**
     * @param speciesId species id
     * @param edition   edition
     * @return fetch edition from cache or null if not there
     */
    public Edition getEdition( Integer speciesId, Integer edition ) {
        if ( speciesId == null || edition == null ) {
            return null;
        }
        Species species = speciesCache.get( speciesId );
        if ( species == null ) return null;

        Map<Integer, Edition> tmp = allEditions.get( species );
        if ( tmp != null ) {
            return tmp.get( edition );
        } else {
            return null;
        }
    }

    /**
     * @param species species id
     * @param ed      edition
     * @param t       term
     * @return count of genes annotated with this term or any of its children
     */
    public Integer getInferredAnnotationCount( Species species, Edition ed, GeneOntologyTerm t ) {
        if ( species == null || ed == null || t == null ) return null;
        return inferredAnnotationCount.get( new MultiKey( species, ed, t ) );

    }

    /**
     * @param species species id
     * @param ed      edition
     * @param t       term
     * @return count of genes annotated with this term
     */
    public Integer getDirectAnnotationCount( Species species, Edition ed, GeneOntologyTerm t ) {
        if ( species == null || ed == null || t == null ) return null;
        return directAnnotationCount.get( new MultiKey( species, ed, t ) );

    }

    /**
     * @param species species id
     * @param ed      edition
     * @return aggregate
     */
    public Aggregate getAggregates( Species species, Edition ed ) {
        if ( species == null || ed == null ) return null;
        Map<Edition, Aggregate> tmp = aggregates.get( species );
        if ( tmp != null ) {
            return tmp.get( ed );
        }
        return null;

    }

    /**
     * @param species species id
     * @return aggregates
     */
    public Map<Edition, Aggregate> getAggregates( Species species ) {
        if ( species == null ) return null;
        return aggregates.get( species );

    }

    /**
     * @return aggregates
     */
    public Map<Species, Map<Edition, Aggregate>> getAggregates() {
        return aggregates;

    }

    /**
     * @param accession primary accession
     * @return gene with this primary accession or null
     */
    public Gene getCurrentGene( String accession ) {
        if ( accession == null ) {
            return null;
        }

        return accessionToGene.get( accession );
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
     * @param species species
     * @param edition edition
     * @return Count of unique genes in this edition under this species
     */
    public Integer getGeneCount( Species species, Edition edition ) {
        if ( species == null || edition == null ) {
            return null;
        }
        Map<Edition, Aggregate> map2 = aggregates.get( species );
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
     * @param ed  edition
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
     * @param ed    edition
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
            found |= (term != null);
        }

        return found ? termsMap : null;
    }

    /**
     * Used when comparing similarity of sets of terms in separate editions.
     * Searching the term in the more current edition will account for obsoletion in the form
     * of alternate ids.
     *
     * @param ed
     * @param terms
     * @return
     */
    public Set<GeneOntologyTerm> convertTerms( Edition ed, Set<GeneOntologyTerm> terms ) {
        Set<GeneOntologyTerm> results = new HashSet<>();

        if ( terms == null || ed == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( ed.getGoEdition() );
        if ( o == null ) {
            return null;
        }

        for ( GeneOntologyTerm t : terms ) {
            results.add( o.getTerm( t.getGoId() ) );
        }
        return results;
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

    public String getCurrentDefinition( GeneOntologyTerm term ) {
        if ( term == null ) {
            return null;
        }
        return geneOntologyDefinitions.get( term.getGoId() );
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
        synchronized (applicationLevelGeneCache) {
            return applicationLevelGeneCache.get( g );
        }
    }

    /**
     * Add data to GeneView cache under given gene
     *
     * @param g    gene to cache data under
     * @param data data to be cached
     */
    public void addGeneData( Gene g,
                             Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> data ) {
        synchronized (applicationLevelGeneCache) {
            applicationLevelGeneCache.put( g, data );
        }
    }

    /**
     * @param gene
     * @return cache data for EnrichmentView under given gene
     */
    public Map<Edition, Set<GeneOntologyTerm>> getEnrichmentData( Gene gene ) {
        // TODO not sure if necessary, not a big deal either way
        synchronized (applicationLevelEnrichmentCache) {
            return applicationLevelEnrichmentCache.get( gene );
        }
    }

    /**
     * Add data to EnrichmentView cache under given gene
     *
     * @param gene gene to cache data under
     * @param data data to be cached
     */
    public void addEnrichmentData( Gene gene, Map<Edition, Set<GeneOntologyTerm>> data ) {
        synchronized (applicationLevelEnrichmentCache) {
            applicationLevelEnrichmentCache.put( gene, data );
        }
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
