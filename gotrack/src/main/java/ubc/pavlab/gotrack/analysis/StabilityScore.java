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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Scores which attempt to explore the impact that annotation stability has on the performance of gene set enrichment
 * analyses
 * 
 * @author mjacobson
 */
@Getter
@RequiredArgsConstructor
@ToString
public final class StabilityScore {

    private final double sampleSigma; //standard deviation of sample annotated
    private final double populationSigma; //standard deviation of population annotated
    private final double minPvalue; //minimum p-value given greedy variation of parameters around 95% confidence interval
    private final double maxPvalue; //maximum p-value given greedy variation of parameters around 95% confidence interval
    private final double score; //Stability score (log( ( maxPvalue - minPvalue ) / cutoff )
    private final double averageScore; //simple average of scores up to this point: log( SUM_edition (e^score(edition)) )
    private final double pastSignificancePercentage; // % of editions this was significant in recent past

}
