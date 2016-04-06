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

package ubc.pavlab.gotrack.analysis;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Represents a population of entities with associated properties. Needs to make the ability to count the number of
 * entities with a certain property available, the means is left up to the particular implementation.
 * 
 * @author mjacobson
 * @version $Id$
 */
public abstract class Population<T, G> {
    public abstract Integer countProperty( T t );

    public abstract int size();

    public static <T, G> StandardPopulation<T, G> standardPopulation( Map<T, Set<G>> annotationMap ) {
        return new StandardPopulation<T, G>( annotationMap );
    }

    public static CachedGOPopulation cachedGOPopulation( Cache cache, Integer speciesId, Edition edition ) {
        return new CachedGOPopulation( cache, speciesId, edition );
    }

}

/**
 * Implementation of population backed by an already existing cache.
 */
abstract class CachedPopulation<C, T, G> extends Population<T, G> {

    C cache;

    public CachedPopulation( C cache ) {
        this.cache = cache;
    }
}

/**
 * Standard implementation of population.
 */
class StandardPopulation<T, G> extends Population<T, G> {
    //    private Map<T, Set<G>> annotationMap;
    private Map<T, Integer> annotationCountMap;
    private Integer distinctEntityCount;

    public StandardPopulation( Map<T, Set<G>> annotationMap ) {
        //        this.annotationMap = new HashMap<>( annotationMap );
        annotationCountMap = Maps.newHashMap();
        Set<G> distinctEntities = Sets.newHashSet();
        for ( Entry<T, Set<G>> e : annotationMap.entrySet() ) {
            distinctEntities.addAll( e.getValue() );
            annotationCountMap.put( e.getKey(), e.getValue().size() );
        }
        distinctEntityCount = distinctEntities.size();
    }

    //    public Set<G> getEntities( T t ) {
    //        return annotationMap.get( t );
    //    }

    public Set<T> getProperties() {
        return annotationCountMap.keySet();
    }

    @Override
    public Integer countProperty( T t ) {
        return annotationCountMap.get( t );
    }

    @Override
    public int size() {
        return distinctEntityCount;
    }
}

/**
 * Cached Implementation of Gene population annotated with GeneOntology based on GOTrack's Cache Bean.
 */
class CachedGOPopulation extends CachedPopulation<Cache, GeneOntologyTerm, Gene> {
    private Integer speciesId;
    private Edition edition;

    public CachedGOPopulation( Cache cache, Integer speciesId, Edition edition ) {
        super( cache );
        this.edition = edition;
        this.speciesId = speciesId;
    }

    @Override
    public Integer countProperty( GeneOntologyTerm t ) {
        return cache.getInferredAnnotationCount( speciesId, edition, t );
    }

    @Override
    public int size() {
        return cache.getGeneCount( speciesId, edition );
    }

};