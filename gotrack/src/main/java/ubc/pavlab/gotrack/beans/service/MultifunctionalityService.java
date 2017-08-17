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

package ubc.pavlab.gotrack.beans.service;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@ApplicationScoped
public class MultifunctionalityService implements Serializable {

    private static final Logger log = Logger.getLogger( MultifunctionalityService.class );

    @Inject
    private Cache cache;

    @Inject
    private AnnotationService annotationService;

    public MultifunctionalityService() {
        log.info( "MultifunctionalityService created" );
    }

    @PostConstruct
    public void init() {
        log.info( "MultifunctionalityService init" );
    }

    @PreDestroy
    public void destroyed() {
        log.info( "MultifunctionalityService destroyed" );
    }

    public Double multifunctionality( Gene g, Edition ed ) {
        Set<GeneOntologyTerm> terms = retrieveData( g, ed );
        return multifunctionality( terms, ed );
    }

    public Map<Edition, Double> multifunctionality( Gene g ) {
        Map<Edition, Set<GeneOntologyTerm>> data = retrieveData( g );
        return multifunctionality( data );
    }

    public Map<Edition, Double> multifunctionality( Map<Edition, Set<GeneOntologyTerm>> data ) {
        Map<Edition, Double> results = Maps.newHashMap();
        for ( Entry<Edition, Set<GeneOntologyTerm>> entry : data.entrySet() ) {
            Edition ed = entry.getKey();
            Set<GeneOntologyTerm> terms = entry.getValue();
            Double multi = multifunctionality( terms, ed );
            if ( multi != null ) {
                results.put( ed, multi );
            }
        }
        return results;
    }

    public Double multifunctionality( Set<GeneOntologyTerm> terms, Edition ed ) {
        Integer total = cache.getGeneCount( ed );
        Double multi = null;
        if ( total != null ) {
            multi = 0.0;
            for ( GeneOntologyTerm t : terms ) {
                // Multifunctionality

                Integer inGroup = cache.getInferredAnnotationCount( ed, t );
                if ( inGroup != null && inGroup < total ) {
                    multi += 1.0 / ( inGroup * ( total - inGroup ) );
                }

            }
        }
        return multi;
    }

    /**
     * Attempt to get data from cache, if not in cache get from DB.
     */
    private Map<Edition, Set<GeneOntologyTerm>> retrieveData( Gene gene ) {

        Map<Edition, Set<GeneOntologyTerm>> data = annotationService
                .fetchEnrichmentData( gene.getSpecies(), Lists.newArrayList( gene ) ).get( gene );

        return propagate( data );

    }

    /**
     * Attempt to get data from cache, if not in cache get from DB.
     */
    private Set<GeneOntologyTerm> retrieveData( Gene gene, Edition ed ) {

        Set<GeneOntologyTerm> data = annotationService
                .fetchSingleEnrichmentData( ed, Lists.newArrayList( gene ) ).get( gene );

        return cache.propagate( data, ed );

    }

    /**
     * Propagates the annotations from the retrieved data to ancestor terms.
     */
    private Map<Edition, Set<GeneOntologyTerm>> propagate(
            Map<Edition, Set<GeneOntologyTerm>> map ) {
        Map<Edition, Set<GeneOntologyTerm>> propagatedData = new HashMap<>();

        for ( Entry<Edition, Set<GeneOntologyTerm>> entry : map.entrySet() ) {
            Edition ed = entry.getKey();
            propagatedData.put( ed, cache.propagate( entry.getValue(), ed ) );
        }
        return propagatedData;
    }

}
