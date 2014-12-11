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

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang.StringUtils;

import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.dao.SpeciesDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Species;

/**
 * TODO Document Me
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

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    private static List<Species> speciesList;
    private static Map<Integer, Edition> currentEditions;
    private static Map<Integer, Map<String, Accession>> currrentAccessions = new HashMap<Integer, Map<String, Accession>>();
    // private static Map<Integer, Collection<String>> symbols = new HashMap<Integer, Collection<String>>();
    private static Map<Integer, Map<String, Collection<Accession>>> symbolToCurrentAccessions = new HashMap<Integer, Map<String, Collection<Accession>>>();

    /**
     * 
     */
    public Cache() {
        System.out.println( "Cache created" );
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        System.out.println( "Cache init" );

        // Obtain SpeciesDAO.
        SpeciesDAO speciesDAO = daoFactoryBean.getGotrack().getSpeciesDAO();
        System.out.println( "SpeciesDAO successfully obtained: " + speciesDAO );

        speciesList = speciesDAO.list();

        // Obtain CacheDAO.
        CacheDAO cacheDAO = daoFactoryBean.getGotrack().getCacheDAO();
        System.out.println( "CacheDAO successfully obtained: " + cacheDAO );

        currentEditions = cacheDAO.getCurrentEditions();

        System.out.println( "Loading accession to geneSymbol cache..." );
        for ( Species species : speciesList ) {
            Integer speciesId = species.getId();
            Edition currEd = currentEditions.get( speciesId );
            if ( currEd == null ) continue;

            // Create Map of current accessions
            Map<String, Accession> currAccMap = cacheDAO.getCurrentAccessions( speciesId, currEd.getEdition() );
            currrentAccessions.put( speciesId, currAccMap );

            // Create symbols to collection of associated current accessions map
            Map<String, Collection<Accession>> currSymMap = new HashMap<String, Collection<Accession>>();
            for ( Accession acc : currAccMap.values() ) {
                Collection<Accession> symbolAccessions = currSymMap.get( acc.getSymbol() );
                if ( symbolAccessions == null ) {
                    symbolAccessions = new HashSet<Accession>();
                    symbolAccessions.add( acc );
                    currSymMap.put( acc.getSymbol(), symbolAccessions );
                } else {
                    symbolAccessions.add( acc );
                }

            }
            symbolToCurrentAccessions.put( speciesId, currSymMap );

            // symbols.put( speciesId, cacheDAO.getUniqueGeneSymbols( speciesId, currEd.getEdition() ) );

            System.out.println( "Done loading accession to geneSymbol for species (" + speciesId + "), size: "
                    + currrentAccessions.get( speciesId ).size() + " unique symbols: "
                    + symbolToCurrentAccessions.get( speciesId ).size() );
        }
        System.out.println( "Done loading accession to geneSymbol cache..." );

    }

    public List<String> complete( String query, Integer species ) {

        if ( query == null ) return new ArrayList<String>();
        String queryUpper = query.toUpperCase();
        Collection<String> exact = new HashSet<String>();
        Collection<String> exactSynonym = new HashSet<String>();
        Collection<String> possible = new HashSet<String>();
        // Map<GOTerm, Long> results = new HashMap<GOTerm, Long>();
        // log.info( "search: " + queryString );
        Map<String, Accession> gs = currrentAccessions.get( species );
        if ( gs != null ) {
            for ( Accession gene : gs.values() ) {
                if ( queryUpper.toUpperCase().equals( gene.getSymbol().toUpperCase() ) ) {
                    exact.add( gene.getSymbol() );
                    continue;
                }
                List<String> synonyms = gene.getSynonyms();

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

        if ( exact.size() > 0 ) {
            return new ArrayList<String>( exact );
        } else if ( exactSynonym.size() > 0 ) {
            return new ArrayList<String>( exactSynonym );
        } else if ( possible.size() > 0 ) {
            ArrayList<String> p = new ArrayList<String>( possible );
            Collections.sort( p, new LevenshteinComparator( query ) );
            return p;
        } else {
            return new ArrayList<String>();
        }

    }

    public List<Species> getSpeciesList() {
        return speciesList;
    }

    public Map<Integer, Edition> getCurrentEditions() {
        return currentEditions;
    }

    public Map<Integer, Map<String, Accession>> getCurrrentAccessions() {
        return currrentAccessions;
    }

    public Map<Integer, Map<String, Collection<Accession>>> getSymbolToCurrentAccessions() {
        return symbolToCurrentAccessions;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
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
