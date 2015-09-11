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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * redirect the JSF’s logging from java.util.properties to log4j
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Sl4jBridgeListener implements ServletContextListener {

    @Override
    public void contextInitialized( ServletContextEvent arg ) {

        //remove the jsf root logger, avoid duplicated logging
        //try comment out this and see the different on the console
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @Override
    public void contextDestroyed( ServletContextEvent arg ) {
        // No-op.
    }

}
