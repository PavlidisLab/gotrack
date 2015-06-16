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
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityCompareMethod;
import ubc.pavlab.gotrack.beans.service.SpeciesService;
import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Dataset;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Evidence;
import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.GOEdition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.StatsEntry;
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
import ubc.pavlab.gotrack.model.table.GeneMatches;
import ubc.pavlab.gotrack.model.table.GeneMatches.MatchType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

/**
 * NOTE: Most maps here do not require synchronicity locks as they are both read-only and accessing threads are
 * necessarily created AFTER initialization, thus prohibiting stale memory
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

    private Map<Integer, Species> speciesCache = new ConcurrentHashMap<>();
    private Map<Integer, Edition> currentEditions = new ConcurrentHashMap<>();
    private Map<Integer, Map<Integer, Edition>> allEditions = new ConcurrentHashMap<>();
    private Map<Integer, GOEdition> allGOEditions = new ConcurrentHashMap<>();
    private GOEdition currentGOEdition;
    private Map<Integer, Map<String, Gene>> speciesToSymbolGenes = new ConcurrentHashMap<>();

    // These are used for autocompletion
    // *********************************
    private Map<Integer, Multimap<String, Gene>> speciesToSecondarySymbolGenes = new ConcurrentHashMap<>();
    // TODO Consider replacing speciesToSymbolGenes with this
    private Map<Integer, RadixTree<Gene>> speciesToRadixGenes = new ConcurrentHashMap<>();
    // These two together create the (slightly hacky) term autocompletion
    // private RadixTree<String> radixWords = new ConcurrentRadixTree<String>( new DefaultCharArrayNodeFactory() );
    // private Map<String, ImmutableSet<GeneOntologyTerm>> wordsToTerm = new ConcurrentHashMap<>();
    private RadixTree<ImmutableSet<GeneOntologyTerm>> radixTerms = new ConcurrentRadixTree<ImmutableSet<GeneOntologyTerm>>(
            new DefaultCharArrayNodeFactory() );
    // *********************************

    private GeneOntology currentOntology;

    private Map<String, Accession> primaryAccession = new ConcurrentHashMap<>();

    private Map<Integer, Map<Edition, StatsEntry>> aggregates = new ConcurrentHashMap<>();

    // Map<species, Map<edition, Map<go_id, count>>>
    private Map<MultiKey, Integer> goSetSizes = new ConcurrentHashMap<>();

    private Map<GOEdition, GeneOntology> ontologies = new ConcurrentHashMap<>();

    // Static
    private Map<String, Evidence> evidenceCache = new ConcurrentHashMap<>();

    private Map<Gene, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>>> applicationLevelDataCache = new LinkedHashMap<Gene, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>>>(
            MAX_DATA_ENTRIES + 1, 0.75F, true ) {
        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry(
                Map.Entry<Gene, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>>> eldest ) {
            return size() > MAX_DATA_ENTRIES;
        }
    };

    // TODO This should be changed to an LFU cache instead of LRU
    private Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> applicationLevelEnrichmentCache = new LinkedHashMap<Gene, Map<Edition, Set<GeneOntologyTerm>>>(
            MAX_ENRICHMENT_ENTRIES + 1, 0.75F, true ) {
        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry( Map.Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> eldest ) {
            return size() > MAX_ENRICHMENT_ENTRIES;
        }
    };

    /**
     * 
     */
    public Cache() {
        log.info( "Cache created" );
    }

    /* Getters for View Static */

    public Dataset[] getDatasets() {
        return Dataset.values();
    }

    public MultipleTestCorrection[] getMultipleTestCorrections() {
        return MultipleTestCorrection.values();
    }

    public SimilarityCompareMethod[] getSimilarityCompareMethods() {
        return SimilarityCompareMethod.values();
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Cache init" );

        applicationLevelDataCache = Collections.synchronizedMap( applicationLevelDataCache );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

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
            allGOEditions.put( dto.getEdition(), new GOEdition( dto ) );
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

        // System.gc();
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

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
            log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                    / 1000000 + " MB" );
        }
        // ****************************

        // goSetSize cache creation
        // ****************************
        if ( !settingsCache.isDryRun() ) {
            for ( AnnotationCountDTO dto : cacheDAO.getGOSizes( speciesRestrictions ) ) {
                MultiKey k = new MultiKey( dto );
                Integer cnt = this.goSetSizes.put( k, dto.getCount() );
                if ( cnt != null ) {
                    // key existed before
                    log.warn( k );
                }
            }

            log.info( "GO Set sizes successfully obtained" );
            // System.gc();();
            log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                    / 1000000 + " MB" );
        }
        // ****************************

        // Aggregate cache creation
        // ****************************
        if ( !settingsCache.isDryRun() ) {
            for ( AggregateDTO dto : cacheDAO.getAggregates( speciesRestrictions ) ) {
                Map<Edition, StatsEntry> m1 = aggregates.get( dto.getSpecies() );

                if ( m1 == null ) {
                    m1 = new ConcurrentHashMap<>();
                    aggregates.put( dto.getSpecies(), m1 );
                }

                Edition ed = allEditions.get( dto.getSpecies() ).get( dto.getEdition() );

                m1.put( ed, new StatsEntry( dto ) );

            }
            log.info( "Aggregates successfully obtained" );

            log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                    / 1000000 + " MB" );
        }
        // ****************************

        // Accession cache creation
        // ****************************
        Map<String, Set<String>> secMap = new HashMap<>();
        Map<String, Boolean> spMap = new HashMap<>();
        for ( AccessionDTO dto : cacheDAO.getAccessions( speciesRestrictions ) ) {
            // secs
            String accession = dto.getAccession();
            Set<String> secs = secMap.get( accession );

            if ( secs == null ) {
                secs = new HashSet<>();
                secMap.put( accession, secs );
            }

            String sec = dto.getSec();
            if ( sec != null ) {
                // Null secondary values means it has no secondary accessions
                secs.add( sec );
            }

            // sp
            spMap.put( accession, dto.getSp() );

        }
        for ( String accession : spMap.keySet() ) {
            primaryAccession
                    .put( accession, new Accession( accession, spMap.get( accession ), secMap.get( accession ) ) );
        }
        spMap.clear();
        secMap.clear();
        // ****************************

        // Gene cache creation
        // ****************************
        Map<Integer, Map<String, Set<String>>> allSynMap = new HashMap<>();
        Map<Integer, Map<String, Set<Accession>>> allAccMap = new HashMap<>();
        for ( GeneDTO dto : cacheDAO.getCurrentGenes( speciesRestrictions ) ) {
            Integer species = dto.getSpecies();
            String symbol = dto.getSymbol();

            // Synonyms
            Map<String, Set<String>> synMap = allSynMap.get( species );

            if ( synMap == null ) {
                synMap = new HashMap<>();
                allSynMap.put( species, synMap );
            }

            Set<String> syns = synMap.get( symbol );

            if ( syns == null ) {
                syns = new HashSet<>();
                synMap.put( symbol, syns );
            }

            syns.addAll( Arrays.asList( dto.getSynonyms().split( "\\|" ) ) );

            // Accessions
            Map<String, Set<Accession>> accMap = allAccMap.get( species );

            if ( accMap == null ) {
                accMap = new HashMap<>();
                allAccMap.put( species, accMap );
            }

            Set<Accession> accs = accMap.get( symbol );

            if ( accs == null ) {
                accs = new HashSet<>();
                accMap.put( symbol, accs );
            }

            Accession acc = primaryAccession.get( dto.getAccession() );

            if ( acc != null ) {
                accs.add( acc );
            } else {
                log.warn( "Accession (" + acc + ") somehow missing when creating genes" );
            }

        }

        for ( Entry<Integer, Map<String, Set<String>>> speciesEntry : allSynMap.entrySet() ) {
            Integer species = speciesEntry.getKey();
            Map<String, Gene> m = speciesToSymbolGenes.get( species );

            if ( m == null ) {
                m = new ConcurrentHashMap<>();
                speciesToSymbolGenes.put( species, m );
            }

            Map<String, Set<Accession>> accMap = allAccMap.get( species );
            Map<String, Set<String>> synMap = allSynMap.get( species );

            for ( String symbol : speciesEntry.getValue().keySet() ) {
                Gene g = new Gene( symbol, speciesCache.get( species ), accMap.get( symbol ), synMap.get( symbol ) );
                m.put( symbol.toUpperCase(), g );
            }

        }
        log.info( "Done loading current genes..." );

        for ( Species species : speciesList ) {
            if ( speciesToSymbolGenes.keySet().contains( species.getId() ) ) {
                log.info( "Current gene size for species (" + species + "): "
                        + speciesToSymbolGenes.get( species.getId() ).size() );
            }
        }

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );
        // ****************************

        // Gene caches for auto-completion
        // ****************************
        // secondary cache
        for ( Entry<Integer, Map<String, Gene>> spEntry : speciesToSymbolGenes.entrySet() ) {
            Integer speciesId = spEntry.getKey();
            Multimap<String, Gene> msec = speciesToSecondarySymbolGenes.get( speciesId );
            if ( msec == null ) {
                msec = HashMultimap.create();
                speciesToSecondarySymbolGenes.put( speciesId, msec );
            }
            for ( Gene g : spEntry.getValue().values() ) {
                for ( String syn : g.getSynonyms() ) {
                    msec.put( syn.toUpperCase(), g );
                }

            }
        }

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

    public Collection<Species> getSpeciesList() {
        return Collections.unmodifiableCollection( speciesCache.values() );
    }

    public Edition getCurrentEditions( Integer speciesId ) {
        return currentEditions.get( speciesId );
    }

    public Collection<Edition> getAllEditions( Integer speciesId ) {
        Map<Integer, Edition> tmp = allEditions.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableCollection( tmp.values() );
        } else {
            return null;
        }
    }

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

    public Integer getGoSetSizes( Integer speciesId, Edition ed, GeneOntologyTerm t ) {
        if ( speciesId == null || ed == null || t == null ) return null;
        MultiKey k = new MultiKey( speciesId, ed, t );
        return goSetSizes.get( k );

    }

    public Map<String, Gene> getSpeciesToCurrentGenes( Integer speciesId ) {
        Map<String, Gene> tmp = speciesToSymbolGenes.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableMap( tmp );
        } else {
            return null;
        }
    }

    public Map<Edition, StatsEntry> getAggregates( Integer speciesId ) {
        if ( speciesId == null ) return null;
        Map<Edition, StatsEntry> tmp = aggregates.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableMap( tmp );
        } else {
            return null;
        }

    }

    public StatsEntry getAggregates( Integer speciesId, Edition ed ) {
        if ( speciesId == null || ed == null ) return null;
        Map<Edition, StatsEntry> tmp = aggregates.get( speciesId );
        if ( tmp != null ) {
            return tmp.get( ed );
        }
        return null;

    }

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

    public Integer getAccessionCount( Integer speciesId, Edition edition ) {
        if ( speciesId == null || edition == null ) {
            return null;
        }
        Map<Edition, StatsEntry> map2 = aggregates.get( speciesId );
        if ( map2 != null ) {
            StatsEntry se = map2.get( edition );
            if ( se != null ) {
                return se.getAccessionCount();
            }

        }
        return null;
    }

    public Integer getGeneCount( Integer speciesId, Edition edition ) {
        if ( speciesId == null || edition == null ) {
            return null;
        }
        Map<Edition, StatsEntry> map2 = aggregates.get( speciesId );
        if ( map2 != null ) {
            StatsEntry se = map2.get( edition );
            if ( se != null ) {
                return se.getGeneCount();
            }

        }
        return null;
    }

    public Map<GeneOntologyTerm, Set<EvidenceReference>> propagate(
            Map<GeneOntologyTerm, Set<EvidenceReference>> goAnnotations, Edition ed ) {
        if ( goAnnotations == null || ed == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( ed.getGoEdition() );
        if ( o != null ) {
            return o.propagate( goAnnotations );
        }
        return null;
    }

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

    // public void ontologyStats() {
    // for ( GeneOntology o : ontologies.values() ) {
    // o.getCacheStats();
    // }
    //
    // }

    public GOEdition getCurrentGOEdition() {
        return currentGOEdition;
    }

    // cache retrieval getters

    public Evidence getEvidence( String evidence ) {
        if ( evidence == null ) return null;
        return evidenceCache.get( evidence );
    }

    public Accession getAccession( String acc ) {
        if ( acc == null ) return null;
        return primaryAccession.get( acc );

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

    public Map<GOEdition, GeneOntologyTerm> getTerm( String goId ) {
        if ( goId == null ) {
            return null;
        }

        Map<GOEdition, GeneOntologyTerm> termsMap = new HashMap<>();

        for ( Entry<GOEdition, GeneOntology> entry : ontologies.entrySet() ) {
            GeneOntologyTerm term = entry.getValue().getTerm( goId );
            // yes we want null values
            termsMap.put( entry.getKey(), term );

        }

        return termsMap;
    }

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

    // Application Level Caching get/set

    public Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> getData( Gene g ) {
        // TODO not sure if necessary, not a big deal either way
        synchronized ( applicationLevelDataCache ) {
            return applicationLevelDataCache.get( g );
        }
    }

    public void addData( Gene g, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>> data ) {
        synchronized ( applicationLevelDataCache ) {
            applicationLevelDataCache.put( g, data );
        }
    }

    public Map<Edition, Set<GeneOntologyTerm>> getEnrichmentData( Gene gene ) {
        // TODO not sure if necessary, not a big deal either way
        synchronized ( applicationLevelEnrichmentCache ) {
            return applicationLevelEnrichmentCache.get( gene );
        }
    }

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

class LevenshteinComparator implements Comparator<GeneOntologyTerm> {

    private String compareTo;

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

final class MultiKey {
    private final Integer species;
    private final Integer edition;
    private final String goId;

    MultiKey( Integer species, Edition ed, GeneOntologyTerm t ) {
        this.species = species;
        this.edition = ed.getEdition();
        this.goId = t.getGoId();
    }

    MultiKey( AnnotationCountDTO dto ) {
        this.species = dto.getSpecies();
        this.edition = dto.getEdition();
        this.goId = dto.getGoId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( edition == null ) ? 0 : edition.hashCode() );
        result = prime * result + ( ( goId == null ) ? 0 : goId.hashCode() );
        result = prime * result + ( ( species == null ) ? 0 : species.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        MultiKey other = ( MultiKey ) obj;
        if ( edition == null ) {
            if ( other.edition != null ) return false;
        } else if ( !edition.equals( other.edition ) ) return false;
        if ( goId == null ) {
            if ( other.goId != null ) return false;
        } else if ( !goId.equals( other.goId ) ) return false;
        if ( species == null ) {
            if ( other.species != null ) return false;
        } else if ( !species.equals( other.species ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MultiKey [species=" + species + ", edition=" + edition + ", goId=" + goId + "]";
    }
}
