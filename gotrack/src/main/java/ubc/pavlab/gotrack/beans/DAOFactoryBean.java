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

import ubc.pavlab.gotrack.dao.DAOFactory;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean(name = "daoFactoryBean", eager = true)
@ApplicationScoped
public class DAOFactoryBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8655251522916437925L;
    private static DAOFactory gotrack;

    /**
     * 
     */
    public DAOFactoryBean() {
        System.out.println( "DAOFactoryBean created" );
    }

    @PostConstruct
    public void init() {
        // You can do here your initialization thing based on managed properties, if necessary.
        System.out.println( "DAOFactoryBean init" );
        // Obtain DAOFactory.
        gotrack = DAOFactory.getInstance( "gotrack" );
        System.out.println( "DAOFactory successfully obtained: " + gotrack );
    }

    public DAOFactory getGotrack() {
        return gotrack;
    }

}
