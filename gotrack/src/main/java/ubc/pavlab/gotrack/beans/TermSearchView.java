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
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Backing bean for a term search input that leads to the TermView page
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ViewScoped
public class TermSearchView implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7801218819288632412L;
    private static final Logger log = Logger.getLogger( TermSearchView.class );
    private static final Integer MAX_RESULTS = 15;

    private String query;

    @Inject
    private Cache cache;

    public TermSearchView() {
        log.info( "TermSearchView created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );
    }

    public String go() {
        if ( !cache.termExists( query ) ) {
            return null;
        }
        return "/trends?faces-redirect=true&query=" + query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public List<GeneOntologyTerm> complete( String query ) {
        return cache.completeTerm( query, MAX_RESULTS, true );

    }

}
