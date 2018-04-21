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

import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Species;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Backing bean for user sessions.
 * 
 * @author mjacobson
 */
@Named
@SessionScoped
public class SessionManager implements Serializable {

    private static final long serialVersionUID = -5782108853082685874L;
    private static final Logger log = Logger.getLogger( SessionManager.class );
    private final int MAX_ENTRIES = 5;
    private Boolean authenticated = false;

    private Species species = null;

    @Inject
    private Security security;

    @Inject
    private SettingsCache settingsCache;

    @Inject
    private Cache cache;

    public SessionManager() {
        log.info( "SessionManager created" );
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "SessionManager init" );
        species = cache.getSpecies( 7 );
        if ( species == null) {
            species = cache.getSpeciesList().iterator().next();
        }
    }

    @PreDestroy
    public void destroy() {
        log.info( "SessionManager destroyed" );
    }

    public String authenticate( String password ) {
        synchronized ( authenticated ) {
            authenticated = security.checkPassword( password );
            log.info( "authenticated: " + authenticated );
            if ( !authenticated ) {
                // Brute force authentication delay
                try {
                    Thread.sleep( 3000 );
                } catch ( InterruptedException e ) {
                    log.error( "Authentication Delay Interrupted", e );
                }
                return "Authentication Failed";
            } else {
                return "Successfully authenticated, welcome...";
            }
        }
    }

    public synchronized void reloadSettings() {
        settingsCache.reload();
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public Species getSpecies() {
        return species;
    }

    public Edition getCurrentEdition() {
        return cache.getCurrentEditions( species );
    }

    public void setSpecies( Species species ) {
        if (species == null) {
            log.warn( "Species cannot be null!" );
            return;
        }
        this.species = species;
    }
}
