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

package ubc.pavlab.gotrack.model.table;

import ubc.pavlab.gotrack.model.AnnotationType;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class LossGainTableValues implements Comparable<LossGainTableValues> {

    public enum LossGain {

        LOSS("Loss"), GAIN("Gain");

        private String label;

        private LossGain( String label ) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    public LossGainTableValues( GeneOntologyTerm t, LossGain type, AnnotationType annotationType ) {
        this.term = t;
        this.type = type;
        this.annotationType = annotationType;
    }

    private final LossGain type;
    private final AnnotationType annotationType;
    private final GeneOntologyTerm term;

    public LossGain getType() {
        return type;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public GeneOntologyTerm getTerm() {
        return term;
    }

    @Override
    public int compareTo( LossGainTableValues o ) {
        int comparison = this.getAnnotationType().compareTo( o.getAnnotationType() );
        if ( comparison != 0 ) {
            return comparison;
        }
        comparison = this.getType().compareTo( o.getType() );
        return comparison == 0 ? this.getTerm().compareTo( o.getTerm() ) : comparison;
    }

}
