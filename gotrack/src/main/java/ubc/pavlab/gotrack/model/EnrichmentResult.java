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

import org.apache.commons.math3.distribution.HypergeometricDistribution;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentResult {

    private double pvalue;
    private int sampleAnnotated;
    private int populationAnnotated;
    private int sampleSize;
    private int populationSize;

    public EnrichmentResult( double pvalue, int sampleAnnotated, int populationAnnotated, int sampleSize,
            int populationSize ) {
        super();
        this.pvalue = pvalue;
        this.sampleAnnotated = sampleAnnotated;
        this.populationAnnotated = populationAnnotated;
        this.sampleSize = sampleSize;
        this.populationSize = populationSize;
    }

    public EnrichmentResult( int sampleAnnotated, int populationAnnotated, int sampleSize, int populationSize ) {
        super();
        this.sampleAnnotated = sampleAnnotated;
        this.populationAnnotated = populationAnnotated;
        this.sampleSize = sampleSize;
        this.populationSize = populationSize;

        HypergeometricDistribution hyper = new HypergeometricDistribution( populationSize, populationAnnotated,
                sampleSize );
        this.pvalue = hyper.upperCumulativeProbability( sampleAnnotated );

    }

    public double getExpected() {
        return sampleSize * ( ( double ) populationAnnotated ) / populationSize;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    public int getSampleAnnotated() {
        return sampleAnnotated;
    }

    public void setSampleAnnotated( int sampleAnnotated ) {
        this.sampleAnnotated = sampleAnnotated;
    }

    public int getPopulationAnnotated() {
        return populationAnnotated;
    }

    public void setPopulationAnnotated( int populationAnnotated ) {
        this.populationAnnotated = populationAnnotated;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize( int sampleSize ) {
        this.sampleSize = sampleSize;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize( int populationSize ) {
        this.populationSize = populationSize;
    }

}
