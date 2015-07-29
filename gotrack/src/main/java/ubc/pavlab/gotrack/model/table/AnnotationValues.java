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

package ubc.pavlab.gotrack.model.table;

import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.AnnotationType;

/**
 * Represents an annotation combined with whether or not it is direct or inferred from a child term.
 * The equals method purposefully leaves out type in its comparison. This is so that we may only have one type for each
 * unique annotation, it is up the the use case to ensure the correct type is kept in a set. Direct implies Inferred but
 * Inferred does NOT imply Direct.
 * (ex. if an annotation is both direct and inferred then it is direct )
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class AnnotationValues {

    private final Annotation annotation;
    private final AnnotationType type;

    public AnnotationValues( Annotation annotation, AnnotationType type ) {
        super();
        this.annotation = annotation;
        this.type = type;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public AnnotationType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( annotation == null ) ? 0 : annotation.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        AnnotationValues other = ( AnnotationValues ) obj;
        if ( annotation == null ) {
            if ( other.annotation != null ) return false;
        } else if ( !annotation.equals( other.annotation ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "AnnotationValues [annotation=" + annotation + ", type=" + type + "]";
    }

}
