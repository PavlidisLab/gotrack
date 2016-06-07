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

package ubc.pavlab.gotrack.beans;

import java.io.Serializable;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.omnifaces.cdi.Eager;

import ubc.pavlab.gotrack.utilities.PropertiesFile;

/**
 * Holds settings from the properties file. Alter the static fields here to meet your requirements if necessary.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@Eager
@ApplicationScoped
public class SettingsCache implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -6957259558292229133L;

    private static final Logger log = Logger.getLogger( SettingsCache.class );

    private static final String PROPERTIES_BACKUP_PATH = "/usr/local/tomcat/";
    private static final String PROPERTIES_PATH = System.getProperty( "user.home" );
    private static final String PROPERTIES_FILE = "gotrack.properties";

    private static final String SPECIES_RESTRICTIONS_PROPERTY = "gotrack.speciesRestrictions";
    private static final String UPDATE_POP_TABLE = "gotrack.updatePopularTable";
    private static final String DRY_RUN = "gotrack.dryRun";

    private PropertiesFile prop = new PropertiesFile();

    // Individual settings caches
    private int[] speciesRestrictions = null;

    @PostConstruct
    public void init() {
        log.info( "SettingsCache init" );

        prop.load( PROPERTIES_FILE, PROPERTIES_PATH, PROPERTIES_BACKUP_PATH );
        for ( Entry<Object, Object> e : prop.entrySet() ) {
            log.info( e.getKey().toString() + " : " + e.getValue().toString() );
        }

    }

    public String getProperty( String key ) {
        return prop.getProperty( key );
    }

    public int[] getSpeciesRestrictions() {
        if ( speciesRestrictions == null ) {
            String sr = prop.getProperty( SPECIES_RESTRICTIONS_PROPERTY );
            if ( StringUtils.isBlank( sr ) ) {
                speciesRestrictions = new int[] {};
            } else {
                String[] tokens = sr.split( "," );
                speciesRestrictions = new int[tokens.length];
                for ( int i = 0; i < tokens.length; i++ ) {
                    speciesRestrictions[i] = Integer.parseInt( tokens[i] );
                }
            }
        }

        return speciesRestrictions;
    }

    public boolean isPopularTableUpdateable() {
        String r = prop.getProperty( UPDATE_POP_TABLE );
        return r != null && r.equals( "true" );
    }

    public boolean isDryRun() {
        String r = prop.getProperty( DRY_RUN );
        return r != null && r.equals( "true" );
    }

    public boolean contains( String key ) {
        return prop.contains( key );
    }

    public void reload() {
        init();
    }

    public String getPropertiesFile() {
        return PROPERTIES_FILE;
    }
}
