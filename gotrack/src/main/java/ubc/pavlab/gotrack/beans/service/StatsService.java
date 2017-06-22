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

import com.google.common.collect.*;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.beans.SettingsCache;
import ubc.pavlab.gotrack.dao.StatsDAO;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.dto.GeneStatsDTO;
import ubc.pavlab.gotrack.model.dto.TermStatsDTO;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service layer on top of stats DAO. Contains methods for fetching information related to the tracking of
 * the hit counts of popular genes and terms as well as the incrementing of said hit counts.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ApplicationScoped
public class StatsService implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -3585829856737970908L;

    private static final Logger log = Logger.getLogger( StatsService.class );

    @Inject
    private SettingsCache settingsCache;

    @Inject
    private DAOFactoryBean daoFactoryBean;

    @Inject
    private Cache cache;

    private StatsDAO statsDAO;

    // private Map<Gene, Integer> trackPopularGenes = new ConcurrentHashMap<>();
    private Multiset<Gene> trackPopularGenes = ConcurrentHashMultiset.create();
    private Multiset<GeneOntologyTerm> trackPopularTerms = ConcurrentHashMultiset.create();

    private List<String> topMultifunc = new ArrayList<>();

    public StatsService() {
        log.info( "StatsService created" );
    }

    @PostConstruct
    public void init() {
        log.info( "StatsService init" );
        statsDAO = daoFactoryBean.getGotrack().getStatsDAO();
        for ( GeneStatsDTO dto : statsDAO.listGenes() ) {
            Gene gene = cache.getCurrentGene( dto.getAccession() );
            if ( gene == null ) {
                log.warn( "Tracked gene no longer exists: " + dto.getAccession() + " - " + dto.getSymbol() );
            } else {
                trackPopularGenes.setCount( gene, dto.getCount() );
            }
        }

        for ( TermStatsDTO dto : statsDAO.listTerms() ) {
            GeneOntologyTerm t = cache.getCurrentTerm( dto.getGoId() );
            if ( t == null ) t = new GeneOntologyTerm( dto.getGoId() );
            trackPopularTerms.setCount( t, dto.getCount() );
        }

        topMultifunc.addAll( Arrays.asList( "One", "Two", "Three", "Four" ) );

    }

    @PreDestroy
    public void destroyed() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Stats destroyed" );
        log.info( "Hits Map: " + trackPopularGenes );

    }

    public List<Gene> getPopularGenes() {
        return Lists.newArrayList( Iterables.limit( Multisets.copyHighestCountFirst( trackPopularGenes ).elementSet(), 5 ) );
    }

    public List<GeneOntologyTerm> getPopularTerms() {
        return Lists.newArrayList( Iterables.limit( Multisets.copyHighestCountFirst( trackPopularTerms ).elementSet(), 5 ) );
    }

    public void countGeneHit( Gene g ) {
        trackPopularGenes.add( g );
        if ( settingsCache.isPopularTableUpdateable() ) {
            statsDAO.incrementGeneHit( g.getSpecies().getId(), g.getAccession().getAccession(), g.getSymbol() );
        }
        // log.debug( "Hits Map: " + trackPopularGenes );
    }

    public void countTermHit( GeneOntologyTerm t ) {
        trackPopularTerms.add( t );
        if ( settingsCache.isPopularTableUpdateable() ) {
            statsDAO.incrementTermHit( t.getGoId() );
        }
        // log.debug( "Hits Map: " + trackPopularTerms );
    }

    public List<String> getTopMultifunc() {
        return topMultifunc;
    }
}
