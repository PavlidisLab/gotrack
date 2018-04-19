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

package ubc.pavlab.gotrack.analysis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * (Almost but not really) immutable result of GO enrichment analysis for a single term
 *
 * @author mjacobson
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"sampleAnnotated", "populationAnnotated", "sampleSize", "populationSize"})
@ToString
public class EnrichmentResult {

    private double pvalue;
    private final int sampleAnnotated;
    private final int populationAnnotated;
    private final int sampleSize;
    private final int populationSize;
    private int rank;
    private double fractionalRank;
    private boolean significant;

    public EnrichmentResult( double pvalue, int sampleAnnotated, int populationAnnotated, int sampleSize,
                             int populationSize ) {
        super();
        this.pvalue = pvalue;
        this.sampleAnnotated = sampleAnnotated;
        this.populationAnnotated = populationAnnotated;
        this.sampleSize = sampleSize;
        this.populationSize = populationSize;
    }

    public double getExpected() {
        return sampleSize * ((double) populationAnnotated) / populationSize;
    }

}
