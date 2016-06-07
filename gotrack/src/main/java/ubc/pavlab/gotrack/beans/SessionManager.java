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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

/**
 * Backing bean for user sessions. Only used to store security authorization at the moment.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@SessionScoped
public class SessionManager implements Serializable {

    private static final long serialVersionUID = -5782108853082685874L;
    private static final Logger log = Logger.getLogger( SessionManager.class );
    private final int MAX_ENTRIES = 5;
    private Boolean authenticated = false;

    @Inject
    private Security security;

    @Inject
    private SettingsCache settingsCache;

    public SessionManager() {
        log.info( "SessionManager created" );
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        log.info( "SessionManager init" );
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
}
