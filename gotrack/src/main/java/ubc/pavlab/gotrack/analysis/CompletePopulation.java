package ubc.pavlab.gotrack.analysis;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a population of entities with associated properties. In addition to the counting requirements set out by
 * Population, this requires the ability to retrieve the set data behind those counts.
 *
 * Created by mjacobson on 09/04/18.
 */
public abstract class CompletePopulation<T, G> extends Population<T>{
    public abstract Set<G> getEntities( T t );

    public abstract Set<G> getEntities();

    public abstract Set<T> getProperties();

    public static <T, G> StandardCompletePopulation<T, G> standardCompletePopulation( Map<T, Set<G>> annotationMap ) {
        return new StandardCompletePopulation<>( annotationMap );
    }

}

/**
 * Standard implementation of population.
 */
class StandardCompletePopulation<T, G> extends CompletePopulation<T, G> {
    private Map<T, Set<G>> propertyEntityMap;
    private Set<G> distinctEntities;

    StandardCompletePopulation( Map<T, Set<G>> annotationMap ) {
        propertyEntityMap = Maps.newHashMap( annotationMap );
        distinctEntities = annotationMap.values().stream().flatMap( Collection::stream ).collect( Collectors.toSet() );
    }

    public Set<G> getEntities( T t ) {
        return propertyEntityMap.get( t );
    }

    public Set<T> getProperties() {
        return propertyEntityMap.keySet();
    }

    public Set<G> getEntities() {
        return distinctEntities;
    }

    @Override
    public Integer countProperty( T t ) {
        return propertyEntityMap.get( t ).size();
    }

    @Override
    public int size() {
        return distinctEntities.size();
    }
}
