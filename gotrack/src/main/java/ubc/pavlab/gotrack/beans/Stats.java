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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ApplicationScoped
public class Stats implements Serializable {

    // @ManagedProperty("#{daoFactoryBean}")
    // private DAOFactoryBean daoFactoryBean;

    /**
     * 
     */
    private static final long serialVersionUID = -4059683356445323944L;

    private static final Logger log = Logger.getLogger( Stats.class );

    private Map<String, Integer> hits = new HashMap<>();

    private List<String> popularGenes = new ArrayList<String>();
    private List<String> topMultifunc = new ArrayList<String>();

    /**
     * 
     */
    public Stats() {
        log.info( "Stats created" );
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Stats init" );

        // Obtain UserDAO.
        // SpeciesDAO speciesDAO = daoFactoryBean.getGotrack().getSpeciesDAO();
        // System.out.println( "UserDAO successfully obtained: " + speciesDAO );

        popularGenes.addAll( Arrays.asList( new String[] { "spades", "hearts", "diamonds", "clubs" } ) );
        topMultifunc.addAll( Arrays.asList( new String[] { "One", "Two", "Three", "Four" } ) );

    }

    @PreDestroy
    public void destroyed() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "Stats destroyed" );
        log.info( "Hits Map: " + hits );

    }

    public void countHit( String key ) {
        synchronized ( hits ) {
            Integer cnt = hits.get( key.toUpperCase() );
            if ( cnt == null ) {
                cnt = 0;
            }
            hits.put( key.toUpperCase(), ++cnt );
        }
        log.info( hits );
    }

    public Map<String, Integer> getHits() {
        return Collections.unmodifiableMap( hits );
    }

    public List<String> getPopularGenes() {
        return popularGenes;
    }

    public List<String> getTopMultifunc() {
        return topMultifunc;
    }

    // public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
    // this.daoFactoryBean = daoFactoryBean;
    // }

}
