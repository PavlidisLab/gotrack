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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.dao.SpeciesDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
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

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    private List<Species> speciesList;
    private Map<Integer, Edition> currentEditions = new HashMap<>();
    private Map<Integer, List<Edition>> allEditions = new HashMap<>();
    private Map<Integer, Map<String, Gene>> speciesToCurrentGenes = new HashMap<>();

    private Map<Integer, Map<Edition, StatsEntry>> aggregates = new HashMap<>();
    // view of aggregates for Map<species, Map<edition, avg_direct>>
    private Map<Integer, Map<Edition, Double>> speciesAverage = new HashMap<>();

    // Map<species, Map<edition, Map<go_id, count>>>
    private Map<Integer, Map<Integer, Map<String, Integer>>> goSetSizes = new HashMap<>();

    /**
     * 
     */
    public Cache() {
        log.info( "Cache created" );
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Cache init" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        // Obtain SpeciesDAO.
        SpeciesDAO speciesDAO = daoFactoryBean.getGotrack().getSpeciesDAO();
        log.info( "SpeciesDAO successfully obtained: " + speciesDAO );

        speciesList = speciesDAO.list();

        // Obtain CacheDAO.
        CacheDAO cacheDAO = daoFactoryBean.getGotrack().getCacheDAO();
        log.info( "CacheDAO successfully obtained: " + cacheDAO );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        goSetSizes = cacheDAO.getGOSizesFromPrecompute();

        log.info( "goSetSizes successfully obtained" );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        aggregates = cacheDAO.getAggregates();

        log.info( "aggregates successfully obtained" );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

        // Create speciesAverage view
        for ( Entry<Integer, Map<Edition, StatsEntry>> speciesEntry : aggregates.entrySet() ) {
            Integer speciesId = speciesEntry.getKey();
            HashMap<Edition, Double> sa = new HashMap<Edition, Double>();
            for ( Entry<Edition, StatsEntry> editionEntry : speciesEntry.getValue().entrySet() ) {
                sa.put( editionEntry.getKey(), editionEntry.getValue().getAverageDirects() );
            }
            speciesAverage.put( speciesId, sa );
        }

        log.info( "speciesAverages successfully computed" );

        // currentEditions = cacheDAO.getCurrentEditions();
        // log.debug( "Current Editions Size: " + currentEditions.size() );

        allEditions = cacheDAO.getAllEditions();
        log.debug( "All Editions Size: " + allEditions.size() );

        for ( Integer species : allEditions.keySet() ) {
            List<Edition> l = allEditions.get( species );
            Collections.sort( l );
            Edition ed = Iterables.getLast( l, null );
            log.debug( "Current edition for species_id (" + species + "): " + ed );
            currentEditions.put( species, ed );
        }

        log.info( "Loading accession to geneSymbol cache..." );
        for ( Species species : speciesList ) {
            Integer speciesId = species.getId();
            Edition currEd = currentEditions.get( speciesId );

            if ( currEd == null ) continue;
            log.debug( species.getCommonName() + ": " + currEd.toString() );
            // get current accessions
            Map<String, Accession> currAccMap = cacheDAO.getAccessions( speciesId, currEd.getEdition() );

            // Create Map of current genes
            Map<String, Gene> currentGenes = new HashMap<>();

            for ( Accession acc : currAccMap.values() ) {
                String symbol = acc.getSymbol();
                Gene gene = currentGenes.get( symbol.toUpperCase() );
                if ( gene == null ) {
                    gene = new Gene( symbol );
                    currentGenes.put( symbol.toUpperCase(), gene );
                }
                gene.getAccessions().add( acc );
                gene.getSynonyms().addAll( acc.getSynonyms() );

            }

            speciesToCurrentGenes.put( speciesId, currentGenes );

            log.info( "Done loading accession to geneSymbol for species (" + speciesId + "), size: "
                    + currAccMap.size() + " unique symbols: " + currentGenes.size() );
        }
        log.info( "Done loading accession to geneSymbol cache..." );

        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

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
        return Collections.unmodifiableList( allEditions.get( speciesId ) );
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
        return Collections.unmodifiableMap( speciesToCurrentGenes.get( speciesId ) );
    }

    public Map<Edition, StatsEntry> getAggregates( Integer speciesId ) {
        return Collections.unmodifiableMap( aggregates.get( speciesId ) );
    }

    public Map<Edition, Double> getSpeciesAverage( Integer speciesId ) {
        return Collections.unmodifiableMap( speciesAverage.get( speciesId ) );
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

    public Integer getAccessionSize( Integer speciesId, Edition edition ) {
        if ( speciesId == null || edition == null ) {
            return null;
        }
        Map<Edition, StatsEntry> map2 = aggregates.get( speciesId );
        if ( map2 != null ) {
            StatsEntry se = map2.get( edition );
            if ( se != null ) {
                return se.getUniqueAccesions();
            }

        }
        return null;
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
