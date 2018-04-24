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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ubc.pavlab.gotrack.model.FullAnnotation;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import java.util.BitSet;
import java.util.Collection;

/**
 * 
 * @author mjacobson
 */
@Getter
@EqualsAndHashCode(exclude = {"annotations", "inSet"})
@RequiredArgsConstructor
public class GeneViewRightPanelRow implements Comparable<GeneViewRightPanelRow> {

    private final GeneOntologyTerm term;
    private final Boolean direct;
    private final Boolean curated;
    private final Collection<FullAnnotation> annotations;

    /* Used for set comparison */
    private final BitSet inSet;

    public GeneViewRightPanelRow( GeneOntologyTerm term, Boolean direct, Boolean curated, Collection<FullAnnotation> annotations ) {
        this( term, direct, curated, annotations, BitSet.valueOf( new long[] {1} ));
    }


    @Override
    public int compareTo( GeneViewRightPanelRow that ) {
        return ComparisonChain.start()
                .compare( this.direct, that.direct, Ordering.natural().reverse().nullsLast() )
                .compare( this.curated, that.curated, Ordering.natural().reverse().nullsLast() )
                .compare( this.term, that.term )
                .result();
    }
}