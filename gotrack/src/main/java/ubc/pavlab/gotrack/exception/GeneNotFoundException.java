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

package ubc.pavlab.gotrack.exception;

/**
 * Exception to be executed when a forced attempt at loading GeneView with a non-existent gene symbol, species occurs.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneNotFoundException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -5753193760419505116L;

    /**
     * 
     */
    public GeneNotFoundException() {
    }

    /**
     * @param message
     */
    public GeneNotFoundException( String message ) {
        super( message );
    }

    /**
     * @param cause
     */
    public GeneNotFoundException( Throwable cause ) {
        super( cause );
    }

    /**
     * @param message
     * @param cause
     */
    public GeneNotFoundException( String message, Throwable cause ) {
        super( message, cause );
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public GeneNotFoundException( String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

}
