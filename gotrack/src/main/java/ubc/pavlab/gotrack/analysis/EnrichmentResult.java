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
 * (Almost but not really) immutable result of GO enrichment analysis for a single term
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentResult {

    private double pvalue;
    private final int sampleAnnotated;
    private final int populationAnnotated;
    private final int sampleSize;
    private final int populationSize;
    private int rank;
    private double fractionalRank;

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
        return sampleSize * ( ( double ) populationAnnotated ) / populationSize;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    public int getRank() {
        return rank;
    }

    public void setRank( int rank ) {
        this.rank = rank;
    }

    public double getFractionalRank() {
        return fractionalRank;
    }

    public void setFractionalRank( double fractionalRank ) {
        this.fractionalRank = fractionalRank;
    }

    public int getSampleAnnotated() {
        return sampleAnnotated;
    }

    public int getPopulationAnnotated() {
        return populationAnnotated;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + populationAnnotated;
        result = prime * result + populationSize;
        result = prime * result + sampleAnnotated;
        result = prime * result + sampleSize;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        EnrichmentResult other = ( EnrichmentResult ) obj;
        if ( populationAnnotated != other.populationAnnotated ) return false;
        if ( populationSize != other.populationSize ) return false;
        if ( sampleAnnotated != other.sampleAnnotated ) return false;
        if ( sampleSize != other.sampleSize ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "EnrichmentResult [pvalue=" + pvalue + ", sampleAnnotated=" + sampleAnnotated + ", populationAnnotated="
                + populationAnnotated + ", sampleSize=" + sampleSize + ", populationSize=" + populationSize + ", rank="
                + rank + ", fractionalRank=" + fractionalRank + "]";
    }

}
