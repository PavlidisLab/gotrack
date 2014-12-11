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
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class IndexView implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3038133837848883737L;
    private Integer currentSpecies;
    private String query;

    @ManagedProperty("#{cache}")
    private Cache cache;

    /**
     * private Integer currentSpecies; private String query; /**
     */
    public IndexView() {
        System.out.println( "IndexView created" );
    }

    public Integer getCurrentSpecies() {
        return currentSpecies;
    }

    public void setCurrentSpecies( Integer currentSpecies ) {
        this.currentSpecies = currentSpecies;
    }

    public String go() {
        // return "track?faces-redirect=true&includeViewParams=true";
        return "track?faces-redirect=true&query=" + query + "&currentSpecies=" + currentSpecies;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public List<String> complete( String query ) {
        List<String> result = this.cache.complete( query, currentSpecies );
        System.out.println( "Found " + result.size() + " matches." );
        return result;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

}
