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

package ubc.pavlab.gotrack.analysis;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class HyperGeometricUtilities {
    /**
     * Optimized method of calculating probability tails, relies on the fact that given a probability from a
     * hypergeometric distribution with given M,N,k at r : h_M,N,k(r) we can then find h_M,N,k(r+1) without calculating
     * the 9 factorials which are usually required. h_M,N,k(r+1) = h_M,N,k(r) * (k-r) * (M-r) / [ (r+1) * (N-k+r+1) ]
     * proof can be demonstrated relatively easily.
     * 
     * @param r sampleAnnotated
     * @param m populationAnnotated
     * @param k sampleSize
     * @param t populationSize (N+M)
     * @return upper cumulative probability at r given m,k,t
     */
    @Deprecated
    public static double upperCumulativeProbability( int r, int m, int k, int t ) {
        double h_r = sampleProbability( r, m, k, t );
        double pvalue = h_r;
        int min = Math.min( k, m );
        for ( int r_ = r + 1; r_ <= min; r_++ ) {
            h_r = h_r * ( k - r_ + 1 ) * ( m - r_ + 1 ) / ( ( r_ ) * ( t - m - k + r_ ) );

            pvalue += h_r;
        }
        return pvalue;
    }

    /**
     * Optimized method of calculating probability tails, relies on the fact that given a probability from a
     * hypergeometric distribution with given M,N,k at r : h_M,N,k(r) we can then find h_M,N,k(r+1) without calculating
     * the 9 factorials which are usually required. h_M,N,k(r+1) = h_M,N,k(r) * (k-r) * (M-r) / [ (r+1) * (N-k+r+1) ]
     * proof can be demonstrated relatively easily. Utilizes log probabilities to be able to handle large integer inputs
     * > 1000~
     * 
     * @param r sampleAnnotated
     * @param m populationAnnotated
     * @param k sampleSize
     * @param t populationSize (N+M)
     * @return upper cumulative probability at r given m,k,t
     */
    public static double upperCumulativeProbabilityLogMethod( int r, int m, int k, int t ) {
        double h_r_log = sampleProbabilityLog( r, m, k, t );
        double pvalue = Math.exp( h_r_log );
        int min = Math.min( k, m );
        for ( int r_ = r + 1; r_ <= min; r_++ ) {
            h_r_log = h_r_log
                    + FastMath.log( ( k - r_ + 1 ) * ( m - r_ + 1 ) / ( ( double ) ( r_ ) * ( t - m - k + r_ ) ) );

            pvalue += FastMath.exp( h_r_log );
        }
        return pvalue;
    }

    @Deprecated
    public static double sampleProbability( int r, int m, int k, int t ) {
        // if ( r > m ) return 0d;
        return CombinatoricsUtils.binomialCoefficientDouble( m, r )
                / CombinatoricsUtils.binomialCoefficientDouble( t, k )
                * CombinatoricsUtils.binomialCoefficientDouble( t - m, k - r );
    }

    public static double sampleProbabilityLog( int r, int m, int k, int t ) {
        return ( CombinatoricsUtils.binomialCoefficientLog( m, r ) - CombinatoricsUtils.binomialCoefficientLog( t, k ) )
                + CombinatoricsUtils.binomialCoefficientLog( t - m, k - r );
    }
}
