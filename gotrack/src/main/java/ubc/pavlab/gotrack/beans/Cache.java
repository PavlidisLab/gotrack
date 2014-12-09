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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang.StringUtils;

import ubc.pavlab.gotrack.dao.CacheDAO;
import ubc.pavlab.gotrack.dao.SpeciesDAO;
import ubc.pavlab.gotrack.model.GeneSymbol;
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
    private static Map<Integer, Integer> currentEditions;
    private static Map<Integer, Map<GeneSymbol, String>> currentGeneSymbolsToAccession = new HashMap<Integer, Map<GeneSymbol, String>>();

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

        System.out.println( "Loading gene symbol cache..." );
        for ( Species species : speciesList ) {
            Integer speciesId = species.getId();
            currentGeneSymbolsToAccession.put( speciesId,
                    cacheDAO.getUniqueSymbols( speciesId, currentEditions.get( speciesId ) ) );
            System.out.println( "Done loading gene symbols for species (" + speciesId + "), size: "
                    + currentGeneSymbolsToAccession.get( speciesId ).size() );
        }
        System.out.println( "Done loading gene symbol cache..." );

    }

    public List<String> complete( String query, Integer species ) {

        if ( query == null ) return new ArrayList<String>();
        String queryUpper = query.toUpperCase();
        ArrayList<String> exact = new ArrayList<String>();
        ArrayList<String> exactSynonym = new ArrayList<String>();
        ArrayList<String> possible = new ArrayList<String>();
        // Map<GOTerm, Long> results = new HashMap<GOTerm, Long>();
        // log.info( "search: " + queryString );
        Map<GeneSymbol, String> geneSymbols = currentGeneSymbolsToAccession.get( species );
        if ( geneSymbols != null ) {
            for ( GeneSymbol gene : geneSymbols.keySet() ) {
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
            return exact;
        } else if ( exactSynonym.size() > 0 ) {
            return exactSynonym;
        } else if ( possible.size() > 0 ) {
            Collections.sort( possible, new LevenshteinComparator( query ) );
            return possible;
        } else {
            return new ArrayList<String>();
        }

    }

    public List<Species> getSpeciesList() {
        return speciesList;
    }

    public static Map<Integer, Integer> getCurrentEditions() {
        return currentEditions;
    }

    public static Map<Integer, Map<GeneSymbol, String>> getCurrentGeneSymbolsToAccession() {
        return currentGeneSymbolsToAccession;
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
