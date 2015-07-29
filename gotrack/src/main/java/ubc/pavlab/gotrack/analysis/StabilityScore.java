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

/**
 * Scores which attempt to explore the impact that annotation stability has on the performance of gene set enrichment
 * analyses
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class StabilityScore {

    private final double sampleSigma;
    private final double populationSigma;
    private final double minPvalue;
    private final double maxPvalue;
    private final double score;
    private final double averageScore;

    /**
     * @param sampleSigma standard deviation of sample annotated
     * @param populationSigma standard deviation of population annotated
     * @param minPvalue minimum p-value given greedy variation of parameters around 95% confidence interval
     * @param maxPvalue maximum p-value given greedy variation of parameters around 95% confidence interval
     * @param score Stability score (log( ( maxPvalue - minPvalue ) / pvalue ), essentially a log of coefficient of
     *        variation
     * @param averageScore simple average of score sup to this point: log( SUM_edition (e^score(edition)) )
     */
    public StabilityScore( double sampleSigma, double populationSigma, double minPvalue, double maxPvalue, double score,
            double averageScore ) {
        super();
        this.sampleSigma = sampleSigma;
        this.populationSigma = populationSigma;
        this.minPvalue = minPvalue;
        this.maxPvalue = maxPvalue;
        this.score = score; // Math.log( ( maxPvalue - minPvalue ) / pvalue );
        this.averageScore = averageScore;
    }

    public double getSampleSigma() {
        return sampleSigma;
    }

    public double getPopulationSigma() {
        return populationSigma;
    }

    public double getMinPvalue() {
        return minPvalue;
    }

    public double getMaxPvalue() {
        return maxPvalue;
    }

    public double getScore() {
        return score;
    }

    public double getAverageScore() {
        return averageScore;
    }

    @Override
    public String toString() {
        return "StabilityScore [sampleSigma=" + sampleSigma + ", populationSigma=" + populationSigma + ", minPvalue="
                + minPvalue + ", maxPvalue=" + maxPvalue + ", score=" + score + "]";
    }

}
