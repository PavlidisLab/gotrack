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

package ubc.pavlab.gotrack.dao;

/**
 * Exception to capture problems in the configuration of the DAO layer.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class DAOConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a DAOConfigurationException with the given detail message.
     * 
     * @param message The detail message of the DAOConfigurationException.
     */
    public DAOConfigurationException( String message ) {
        super( message );
    }

    /**
     * Constructs a DAOConfigurationException with the given root cause.
     * 
     * @param cause The root cause of the DAOConfigurationException.
     */
    public DAOConfigurationException( Throwable cause ) {
        super( cause );
    }

    /**
     * Constructs a DAOConfigurationException with the given detail message and root cause.
     * 
     * @param message The detail message of the DAOConfigurationException.
     * @param cause The root cause of the DAOConfigurationException.
     */
    public DAOConfigurationException( String message, Throwable cause ) {
        super( message, cause );
    }

}
