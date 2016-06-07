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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.dao.DAOConfigurationException;
import ubc.pavlab.gotrack.dao.DAOFactory;

/**
 * Bean injected into services in order to give access to the DAO Factory
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named("daoFactoryBean")
@ApplicationScoped
public class DAOFactoryBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8655251522916437925L;

    private static final Logger log = Logger.getLogger( DAOFactoryBean.class );

    private static final String PROPERTY_DB = "gotrack.db";

    private static DAOFactory gotrack;

    @Inject
    private SettingsCache settingsCache;

    /**
     * 
     */
    public DAOFactoryBean() {
        log.info( "DAOFactoryBean created" );
    }

    @PostConstruct
    public void init() {
        log.info( "DAOFactoryBean init" );
        // Obtain DAOFactory.
        String dbKey = settingsCache.getProperty( PROPERTY_DB );
        if ( dbKey == null ) {
            throw new DAOConfigurationException( "Required property '" + PROPERTY_DB + "'"
                    + " is missing in properties file '" + settingsCache.getPropertiesFile() + "'." );
        }

        gotrack = DAOFactory.getInstance( dbKey );

        log.info( "DAOFactory successfully obtained: " + gotrack );
    }

    public DAOFactory getGotrack() {
        return gotrack;
    }

}
