/*
 * The gotrack project
 * 
 * Copyright (c) 2015 University of British Columbia
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

package ubc.pavlab.gotrack.beans.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.beans.SettingsCache;
import ubc.pavlab.gotrack.dao.StatsDAO;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.dto.StatsDTO;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ApplicationScoped
public class StatsService implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -3585829856737970908L;

    private static final Logger log = Logger.getLogger( StatsService.class );

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    @ManagedProperty("#{cache}")
    private Cache cache;

    private StatsDAO statsDAO;

    // private Map<Gene, Integer> trackPopularGenes = new ConcurrentHashMap<>();
    private Multiset<Gene> trackPopularGenes = ConcurrentHashMultiset.create();

    private List<String> topMultifunc = new ArrayList<String>();

    public StatsService() {
        log.info( "StatsService created" );
    }

    @PostConstruct
    public void init() {
        log.info( "StatsService init" );
        statsDAO = daoFactoryBean.getGotrack().getStatsDAO();
        List<StatsDTO> l = statsDAO.list();
        for ( StatsDTO dto : l ) {
            trackPopularGenes.setCount( cache.getCurrentGene( dto.getSpeciesId(), dto.getSymbol().toUpperCase() ),
                    dto.getCount() );
        }
        topMultifunc.addAll( Arrays.asList( new String[] { "One", "Two", "Three", "Four" } ) );

    }

    @PreDestroy
    public void destroyed() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Stats destroyed" );
        log.info( "Hits Map: " + trackPopularGenes );

    }

    public List<Gene> getPopularGenes() {
        List<Gene> tmp = new ArrayList<>();
        ImmutableMultiset<Gene> a = Multisets.copyHighestCountFirst( trackPopularGenes );
        for ( Iterator<Entry<Gene>> iterator = Iterables.limit( a.entrySet(), 5 ).iterator(); iterator.hasNext(); ) {
            tmp.add( iterator.next().getElement() );

        }
        return tmp;
    }

    public void countHit( Gene g ) {
        trackPopularGenes.add( g );
        if ( settingsCache.isPopularTableUpdateable() ) {
            statsDAO.incrementGeneHit( g.getSpecies().getId(), g.getSymbol() );
        }
        log.debug( "Hits Map: " + trackPopularGenes );
    }

    public List<String> getTopMultifunc() {
        return topMultifunc;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }
}
