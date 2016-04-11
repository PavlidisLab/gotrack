/*
 * The gotrack project
 * 
 * Copyright (c) 2016 University of British Columbia
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

package ubc.pavlab.gotrack;

import org.apache.log4j.Logger;
import org.mockito.Mockito;

import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.beans.SettingsCache;
import ubc.pavlab.gotrack.dao.DAOFactory;
import ubc.pavlab.gotrack.exception.ConfigurationException;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class BaseTest {

    private static final Logger log = Logger.getLogger( BaseTest.class );

    private static final String PROPERTY_TESTDB = "gotrack.testdb";

    protected static final SettingsCache settingsCache;
    protected static final DAOFactory daoFactory;
    protected static final DAOFactoryBean daoFactoryBean;

    static {

        settingsCache = new SettingsCache();
        settingsCache.init();
        //        applicationProperties.getBasePropertiesFile().mute();

        String testdbKey = settingsCache.getProperty( PROPERTY_TESTDB );
        if ( testdbKey == null ) {
            throw new ConfigurationException( "Required Property '" + PROPERTY_TESTDB + "'"
                    + " is missing in properties file '" + settingsCache.getPropertiesFile() + "'." );
        }

        daoFactory = DAOFactory.getInstance( testdbKey );
        log.info( "TestDAOFactory successfully obtained: " + daoFactory );

        daoFactoryBean = Mockito.mock( DAOFactoryBean.class );
        Mockito.when( daoFactoryBean.getGotrack() ).thenReturn( daoFactory );

        // Application Scoped Services

    }

    public BaseTest() {

    }
}
