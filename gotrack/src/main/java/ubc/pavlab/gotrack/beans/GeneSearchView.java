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

import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.search.GeneMatch;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Backing bean for a gene search input that leads to the GeneView page
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ViewScoped
public class GeneSearchView implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3038133837848883737L;

    private static final Logger log = Logger.getLogger( GeneSearchView.class );
    private static final Integer MAX_RESULTS = 15;

    private Species species = null;
    private Gene queryGene;

    @Inject
    private Cache cache;

    public GeneSearchView() {
        log.info( "GeneSearchView created" );
    }

    @PostConstruct
    public void init() {
        species = cache.getSpecies( 7 );
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies( Species species ) {
        this.species = species;
    }

    public String go() {
        if ( queryGene == null ) {
            return null;
        }
        return "/genes?faces-redirect=true&accession=" + queryGene.getAccession().getAccession();
    }

    public List<GeneMatch> complete( String query ) {
        return Lists.newArrayList( this.cache.searchGeneBySymbol( query, species, MAX_RESULTS ) );
    }

    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }

    public Gene getQueryGene() {
        return queryGene;
    }
}
