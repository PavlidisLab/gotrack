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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.dao.SpeciesDAO;
import ubc.pavlab.gotrack.go.GeneOntology;
import ubc.pavlab.gotrack.go.Relationship;
import ubc.pavlab.gotrack.go.Term;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Dataset;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.EvidenceReference;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.StatsEntry;

import com.google.common.collect.Iterables;

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
    private final int MAX_ENRICHMENT_ENTRIES = 5;

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    private List<Species> speciesList;
    private Map<Integer, Edition> currentEditions = new HashMap<>();
    private Map<Integer, List<Edition>> allEditions = new HashMap<>();
    private Map<Integer, Map<String, Gene>> speciesToCurrentGenes = new HashMap<>();

    private Map<Integer, Map<Edition, StatsEntry>> aggregates = new HashMap<>();

    // Map<species, Map<edition, Map<go_id, count>>>
    private Map<Integer, Map<Integer, Map<String, Integer>>> goSetSizes = new HashMap<>();

    private Map<Integer, GeneOntology> ontologies = new HashMap<>();

    // Static
    private Map<String, String> evidenceCodeCategories = new HashMap<>();

    private Map<Gene, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>>> applicationLevelDataCache = new LinkedHashMap<Gene, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>>>(
            MAX_DATA_ENTRIES + 1, 0.75F, true ) {
        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry(
                Map.Entry<Gene, Map<Accession, Map<Edition, Map<GeneOntologyTerm, Set<EvidenceReference>>>>> eldest ) {
            return size() > MAX_DATA_ENTRIES;
        }
    };

    private Map<Set<Gene>, Map<Edition, Map<GeneOntologyTerm, Integer>>> applicationLevelEnrichmentCache = new LinkedHashMap<Set<Gene>, Map<Edition, Map<GeneOntologyTerm, Integer>>>(
            MAX_ENRICHMENT_ENTRIES + 1, 0.75F, true ) {
        // This method is called just after a new entry has been added
        @Override
        public boolean removeEldestEntry( Map.Entry<Set<Gene>, Map<Edition, Map<GeneOntologyTerm, Integer>>> eldest ) {
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

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Cache init" );

        applicationLevelDataCache = Collections.synchronizedMap( applicationLevelDataCache );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        // Obtain SpeciesDAO.
        SpeciesDAO speciesDAO = daoFactoryBean.getGotrack().getSpeciesDAO();
        log.info( "SpeciesDAO successfully obtained: " + speciesDAO );

        speciesList = speciesDAO.list();

        log.info( "Species List successfully obtained: " + speciesList );

        // Obtain CacheDAO.
        CacheDAO cacheDAO = daoFactoryBean.getGotrack().getCacheDAO();
        log.info( "CacheDAO successfully obtained: " + cacheDAO );

        evidenceCodeCategories = cacheDAO.getEvidenceCategories();

        // System.gc();();
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        // TODO Change to new goa_go_aggregate table
        goSetSizes = cacheDAO.getGOSizes();

        log.info( "GO Set sizes successfully obtained" );

        // System.gc();
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        if ( settingsCache.getProperty( "gotrack.ontologyInMemory" ).equals( "true" ) ) {

            List<Integer> eds = new ArrayList<Integer>( cacheDAO.getGOEditions() );

            // for ( Integer goEd : eds ) {
            // Set<Term> goTerms = cacheDAO.getGoTerms( goEd );
            // Set<Relationship> goAdjacency = cacheDAO.getAdjacencies( goEd );
            // ontologies.put( goEd, new GeneOntology( goTerms, goAdjacency ) );
            //
            // System.gc();
            // log.info( "GO Ontologies Loaded: " + ontologies.keySet().size() + "/" + eds.size() );
            // log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
            // / 1000000 + " MB" );
            //
            // }

            Map<Integer, Set<Term>> goTerms = cacheDAO.getGoTerms();
            Map<Integer, Set<Relationship>> goAdjacency = cacheDAO.getAdjacencies();

            for ( Integer goEd : goTerms.keySet() ) {
                ontologies.put( goEd, new GeneOntology( goTerms.get( goEd ), goAdjacency.get( goEd ) ) );
                goTerms.get( goEd ).clear();
                goAdjacency.get( goEd ).clear();

                // System.gc();
                log.info( "GO Ontologies Loaded: " + ontologies.keySet().size() + "/" + eds.size() );
                log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                        / 1000000 + " MB" );

            }

            System.gc();
            log.info( "GO Ontologies Loaded: " + ontologies.keySet().size() + "/" + eds.size() );
            log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                    / 1000000 + " MB" );

        }

        aggregates = cacheDAO.getAggregates();

        log.info( "Aggregates successfully obtained" );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        allEditions = cacheDAO.getAllEditions();
        log.debug( "All Editions Size: " + allEditions.size() );

        for ( Integer species : allEditions.keySet() ) {
            List<Edition> l = allEditions.get( species );
            Collections.sort( l );
            Edition ed = Iterables.getLast( l, null );
            log.debug( "Current edition for species_id (" + species + "): " + ed );
            currentEditions.put( species, ed );
        }

        speciesToCurrentGenes = cacheDAO.getCurrentGenes();
        log.info( "Done loading current genes..." );

        for ( Species species : speciesList ) {
            if ( speciesToCurrentGenes.keySet().contains( species.getId() ) ) {
                log.info( "Current gene size for species (" + species + "): "
                        + speciesToCurrentGenes.get( species.getId() ).size() );
            }
        }

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        log.info( "Cache Completed" );

    }

    public List<String> complete( String query, Integer species, Integer maxResults ) {

        if ( query == null ) return new ArrayList<String>();
        String queryUpper = query.toUpperCase();
        Collection<String> exact = new HashSet<String>();
        Collection<String> exactSynonym = new HashSet<String>();
        Collection<String> possible = new HashSet<String>();
        // Map<GOTerm, Long> results = new HashMap<GOTerm, Long>();
        // log.info( "search: " + queryString );
        // Map<String, Accession> gs = currrentAccessions.get( species );
        Map<String, Gene> gs = speciesToCurrentGenes.get( species );

        if ( gs != null ) {

            for ( Gene gene : gs.values() ) {
                if ( queryUpper.equals( gene.getSymbol().toUpperCase() ) ) {
                    exact.add( gene.getSymbol() );
                    continue;
                }

                Set<String> synonyms = gene.getSynonyms();

                for ( String symbol : synonyms ) {
                    if ( queryUpper.equals( symbol.toUpperCase() ) ) {
                        exactSynonym.add( gene.getSymbol() );
                        continue;
                    }
                }

                String pattern = "(.*)" + queryUpper + "(.*)";
                // Pattern r = Pattern.compile(pattern);
                String m = gene.getSymbol().toUpperCase();
                // Matcher m = r.matcher( term.getTerm() );
                if ( m.matches( pattern ) ) {
                    possible.add( gene.getSymbol() );
                    continue;
                }

            }

        }

        log.debug( "Found " + ( exact.size() + exactSynonym.size() + possible.size() ) + " matches." );

        List<String> orderedResults = new ArrayList<>();

        orderedResults.addAll( exact );
        orderedResults.addAll( exactSynonym );

        if ( maxResults != null && orderedResults.size() >= maxResults ) {
            // Return early for performance reasons if enough results have been obtained
            return orderedResults;
        }

        ArrayList<String> p = new ArrayList<String>( possible );
        Collections.sort( p, new LevenshteinComparator( query ) );

        orderedResults.addAll( p );

        if ( maxResults != null && orderedResults.size() > maxResults ) {
            // If there are more than maxResults, remove the excess range
            orderedResults.subList( maxResults, orderedResults.size() ).clear();
        }

        return orderedResults;

    }

    public List<Species> getSpeciesList() {
        return Collections.unmodifiableList( speciesList );
    }

    public Edition getCurrentEditions( Integer speciesId ) {
        return currentEditions.get( speciesId );
    }

    public List<Edition> getAllEditions( Integer speciesId ) {
        List<Edition> tmp = allEditions.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableList( tmp );
        } else {
            return null;
        }
    }

    public Integer getGoSetSizes( Integer speciesId, Integer Edition, String goId ) {
        Map<Integer, Map<String, Integer>> map2 = goSetSizes.get( speciesId );
        if ( map2 != null ) {
            Map<String, Integer> map3 = map2.get( Edition );
            if ( map3 != null ) {
                return map3.get( goId );
            }
        }
        return null;
    }

    public Map<String, Gene> getSpeciesToCurrentGenes( Integer speciesId ) {
        Map<String, Gene> tmp = speciesToCurrentGenes.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableMap( tmp );
        } else {
            return null;
        }
    }

    public Map<Edition, StatsEntry> getAggregates( Integer speciesId ) {
        Map<Edition, StatsEntry> tmp = aggregates.get( speciesId );
        if ( tmp != null ) {
            return Collections.unmodifiableMap( tmp );
        } else {
            return null;
        }

    }

    public Gene getCurrentGene( Integer speciesId, String symbol ) {
        if ( speciesId == null || symbol == null ) {
            return null;
        }
        Map<String, Gene> map2 = speciesToCurrentGenes.get( speciesId );
        if ( map2 != null ) {
            return map2.get( symbol.toUpperCase() );
        }
        return null;
    }

    public boolean currentSymbolExists( Integer speciesId, String symbol ) {
        if ( speciesId == null || symbol == null ) {
            return false;
        }
        Map<String, Gene> map2 = speciesToCurrentGenes.get( speciesId );
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

    public String getEvidenceCategory( String evidence ) {
        if ( evidence == null ) {
            return null;
        }
        return evidenceCodeCategories.get( evidence );
    }

    public Map<GeneOntologyTerm, Set<EvidenceReference>> propagate(
            Map<GeneOntologyTerm, Set<EvidenceReference>> goAnnotations, Integer goEdition ) {
        if ( goAnnotations == null || goEdition == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( goEdition );
        if ( o != null ) {
            return o.propagate( goAnnotations );
        }
        return null;
    }

    public Set<GeneOntologyTerm> propagate( Set<GeneOntologyTerm> terms, Integer goEdition ) {
        if ( terms == null || goEdition == null ) {
            return null;
        }
        GeneOntology o = ontologies.get( goEdition );
        if ( o != null ) {
            return o.propagate( terms );
        }
        return null;
    }

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

    public Map<Edition, Map<GeneOntologyTerm, Integer>> getEnrichmentData( Set<Gene> genes ) {
        // TODO not sure if necessary, not a big deal either way
        synchronized ( applicationLevelEnrichmentCache ) {
            return applicationLevelEnrichmentCache.get( genes );
        }
    }

    public void addEnrichmentData( Set<Gene> genes, Map<Edition, Map<GeneOntologyTerm, Integer>> data ) {
        synchronized ( applicationLevelEnrichmentCache ) {
            // New HashSet because: The behavior of a map is not specified if the value of an object is changed in a
            // manner that affects equals comparisons while the object is a key in the map
            applicationLevelEnrichmentCache.put( new HashSet<>( genes ), data );
        }
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

}

class LevenshteinComparator implements Comparator<String> {

    private String compareTo;

    public LevenshteinComparator( String compareTo ) {
        super();
        this.compareTo = compareTo;
    }

    @Override
    public int compare( String a, String b ) {
        int d1 = StringUtils.getLevenshteinDistance( a, compareTo );
        int d2 = StringUtils.getLevenshteinDistance( b, compareTo );
        return d1 < d2 ? -1 : d1 == d2 ? 0 : 1;
    }
}
