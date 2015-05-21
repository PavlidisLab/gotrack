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

package ubc.pavlab.gotrack.utilities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Calculates Jaccard index and/or distance for two sets.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Jaccard {

    public Jaccard() {
    }

    // public API

    public static <T> Double similarity( T[] a, T[] b ) {
        if ( a == null && b == null ) return null;
        if ( a.length == 0 && b.length == 0 ) return 1.0d;
        if ( a.length == 0 || b.length == 0 ) return 0.0d;

        return calculate( new HashSet<T>( Arrays.asList( a ) ), new HashSet<T>( Arrays.asList( b ) ) );
    }

    public static <T> Double similarity( Set<T> a, Set<T> b ) {
        if ( a == null && b == null ) return null;
        if ( a.size() == 0 && b.size() == 0 ) return 1.0d;
        if ( a.size() == 0 || b.size() == 0 ) return 0.0d;

        return calculate( a, b );
    }

    public static <T> Double distance( T[] a, T[] b ) {
        if ( a == null && b == null ) return null;
        if ( a.length == 0 && b.length == 0 ) return 0.0d;
        if ( a.length == 0 || b.length == 0 ) return 1.0d;

        return 1 - calculate( new HashSet<T>( Arrays.asList( a ) ), new HashSet<T>( Arrays.asList( b ) ) );
    }

    public static <T> Double distance( Set<T> a, Set<T> b ) {
        if ( a == null && b == null ) return null;
        if ( a.size() == 0 && b.size() == 0 ) return 0.0d;
        if ( a.size() == 0 || b.size() == 0 ) return 1.0d;
        return 1 - calculate( a, b );
    }

    // Calculation

    private static <T> Double calculate( Set<T> a, Set<T> b ) {

        return ( ( double ) intersectSize( a, b ) ) / unionSize( a, b );
    }

    // Utilities

    public static <T> int intersectSize( Set<T> a, Set<T> b ) {

        Set<T> set1;
        Set<T> set2;
        // Optimized slightly for performance
        if ( a.size() <= b.size() ) {
            set1 = a;
            set2 = b;
        } else {
            set1 = b;
            set2 = a;
        }
        int count = 0;
        for ( T e : set1 ) {
            if ( set2.contains( e ) ) {
                count++;
            }
        }
        return count;
    }

    public static <T> int unionSize( Set<T> a, Set<T> b ) {
        Set<T> set1;
        Set<T> set2;
        // Optimized slightly for performance
        if ( a.size() <= b.size() ) {
            set1 = a;
            set2 = b;
        } else {
            set1 = b;
            set2 = a;
        }
        int count = set2.size();
        for ( T e : set1 ) {
            if ( !set2.contains( e ) ) {
                count++;
            }
        }
        return count;
    }

}
