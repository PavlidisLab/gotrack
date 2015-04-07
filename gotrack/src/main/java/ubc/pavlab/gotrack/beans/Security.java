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

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ApplicationScoped
public class Security {

    private static final Logger log = Logger.getLogger( Security.class );
    private String passwordHash;

    @ManagedProperty(value = "#{settingsCache}")
    private SettingsCache settingsCache;

    public Security() {
        log.info( "Security created" );
    }

    @PostConstruct
    public void init() {
        log.info( "Security init" );
        loadPassword();
    }

    public boolean checkPassword( String password ) {
        return BCrypt.checkpw( password, passwordHash );
    }

    private void loadPassword() {
        passwordHash = BCrypt.hashpw( settingsCache.getProperty( "gotrack.auth" ), BCrypt.gensalt() );
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

}
