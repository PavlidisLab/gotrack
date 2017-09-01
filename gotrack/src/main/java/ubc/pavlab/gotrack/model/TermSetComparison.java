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

package ubc.pavlab.gotrack.model;

import com.google.common.collect.Sets;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import java.util.Collection;
import java.util.Set;

/**
 *
 * @author mjacobson
 */
public final class TermSetComparison {

    private final Collection<GeneOntologyTerm> setAOnly;
    private final Collection<GeneOntologyTerm> setBOnly;
    private final Collection<GeneOntologyTerm> overlap;

    public TermSetComparison( Set<GeneOntologyTerm> setA, Set<GeneOntologyTerm> setB ) {

        overlap = Sets.intersection( setA, setB );
        setAOnly = Sets.difference( setA, setB );
        setBOnly = Sets.difference( setB, setA );

    }

    public Collection<GeneOntologyTerm> getSetAOnly() {
        return setAOnly;
    }

    public Collection<GeneOntologyTerm> getSetBOnly() {
        return setBOnly;
    }

    public Collection<GeneOntologyTerm> getOverlap() {
        return overlap;
    }
}
