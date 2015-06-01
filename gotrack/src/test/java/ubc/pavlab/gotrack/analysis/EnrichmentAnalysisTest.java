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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentAnalysisTest {

    private static final Logger log = Logger.getLogger( EnrichmentAnalysisTest.class );

    private static List<int[]> rndints = new ArrayList<>();

    // private EnrichmentAnalysis enrichmentAnalysis = new EnrichmentAnalysis();;

    @BeforeClass
    public static void classSetup() {
        log.info( "EnrichmentAnalysisTest @BeforeClass" );
        Random rnd = new Random( 12345 );
        for ( int i = 0; i < 10000; i++ ) {
            int[] arr = new int[4];
            arr[0] = rndInt( 10, 20000, rnd ); // population size
            arr[1] = rndInt( 1, arr[0] - 1, rnd ); // successes
            arr[2] = rndInt( 1, arr[0], rnd ); // sample size
            arr[3] = rndInt( arr[2] > arr[0] - arr[1] ? arr[2] - ( arr[0] - arr[1] ) : 0, Math.min( arr[2], arr[1] ),
                    rnd ); // sample successes

            rndints.add( arr );

            // if ( Double.isNaN( c ) || Double.isInfinite( c ) ) {
            // log.info( Arrays.toString( new int[] { r, m, k, t } ) );
            // }

        }
    }

    @Before
    public void setup() {
        log.info( "EnrichmentAnalysisTest @Before" );

    }

    @After
    public void after() {
        log.info( "EnrichmentAnalysisTest @After" );
    }

    @Test
    public void testUpperCumulativeProbability() {
        log.info( "EnrichmentAnalysisTest testUpperCumulativeProbability" );
        Random rnd = new Random( 12345 );
        RandomGenerator rg = RandomGeneratorFactory.createRandomGenerator( rnd );
        int successes = 0;
        for ( int[] arr : rndints ) {
            HypergeometricDistribution hd = new HypergeometricDistribution( rg, arr[0], arr[1], arr[2] );
            double c = hd.upperCumulativeProbability( arr[3] );
            try {
                assertEquals( c,
                        EnrichmentAnalysis.upperCumulativeProbabilityLogMethod( arr[3], arr[1], arr[2], arr[0] ),
                        c / 10000 );
                successes++;
            } catch ( AssertionError e ) {
                log.info( successes + " / " + rndints.size() );
                log.info( Arrays.toString( arr ) );
                throw e;
            }
        }

    }

    @Test
    public void testSampleProbability() {
        log.info( "EnrichmentAnalysisTest testSampleProbability" );
        Random rnd = new Random( 12345 );
        RandomGenerator rg = RandomGeneratorFactory.createRandomGenerator( rnd );
        int successes = 0;
        for ( int[] arr : rndints ) {
            HypergeometricDistribution hd = new HypergeometricDistribution( rg, arr[0], arr[1], arr[2] );
            double c = hd.logProbability( arr[3] );
            try {
                assertEquals( c, EnrichmentAnalysis.sampleProbabilityLog( arr[3], arr[1], arr[2], arr[0] ),
                        0.434 / 10000 ); // error comes from logarithmic error propagation
                successes++;
            } catch ( AssertionError e ) {
                log.info( successes + " / " + rndints.size() );
                log.info( Arrays.toString( arr ) );
                throw e;
            }
        }

    }

    private static int rndInt( int min, int max, Random r ) {
        return min + ( int ) ( r.nextDouble() * ( ( max - min ) + 1 ) );
    }

}
