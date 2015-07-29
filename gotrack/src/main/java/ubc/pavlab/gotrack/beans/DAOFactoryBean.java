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
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.dao.DAOConfigurationException;
import ubc.pavlab.gotrack.dao.DAOFactory;

/**
 * Bean injected into services in order to give access to the DAO Factory
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean(name = "daoFactoryBean")
@ApplicationScoped
public class DAOFactoryBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8655251522916437925L;

    private static final Logger log = Logger.getLogger( DAOFactoryBean.class );

    private static final String PROPERTY_DB = "gotrack.db";

    private static final String PROPERTY_URL = "url";
    private static final String PROPERTY_DRIVER = "driver";
    private static final String PROPERTY_USERNAME = "username";
    private static final String PROPERTY_PASSWORD = "password";

    private static DAOFactory gotrack;

    @ManagedProperty("#{settingsCache}")
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

        String url = getProperty( dbKey, PROPERTY_URL, true );
        String driverClassName = getProperty( dbKey, PROPERTY_DRIVER, false );
        String password = getProperty( dbKey, PROPERTY_PASSWORD, false );
        String username = getProperty( dbKey, PROPERTY_USERNAME, password != null );

        gotrack = DAOFactory.getInstance( url, driverClassName, password, username );
        log.info( "DAOFactory successfully obtained: " + gotrack );
    }

    public DAOFactory getGotrack() {
        return gotrack;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

    /**
     * Returns the DAOProperties instance specific property value associated with the given key with the option to
     * indicate whether the property is mandatory or not.
     * 
     * @param key The key to be associated with a DAOProperties instance specific value.
     * @param mandatory Sets whether the returned property value should not be null nor empty.
     * @return The DAOProperties instance specific property value associated with the given key.
     * @throws DAOConfigurationException If the returned property value is null or empty while it is mandatory.
     */
    private String getProperty( String specificKey, String key, boolean mandatory ) throws DAOConfigurationException {
        String fullKey = specificKey + "." + key;
        String property = settingsCache.getProperty( fullKey );

        if ( property == null || property.trim().length() == 0 ) {
            if ( mandatory ) {
                throw new DAOConfigurationException( "Required property '" + fullKey + "'"
                        + " is missing in properties file '" + settingsCache.getPropertiesFile() + "'." );
            } else {
                // Make empty value null. Empty Strings are evil.
                property = null;
            }
        }

        return property;
    }

}
