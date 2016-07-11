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

package ubc.pavlab.gotrack.utilities;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class JaccardTest {

    private static final Logger log = Logger.getLogger( JaccardTest.class );

    private static Set<String> stringSet1;
    private static String[] stringArray1;

    private static Set<String> stringSet2;
    private static String[] stringArray2;

    private static Set<String> stringSetEmpty;
    private static String[] stringArrayEmpty;

    private static String[] stringArrayRedundant;

    private static Set<Integer> integerSet1;
    private static Integer[] integerArray1;

    private static Set<Integer> integerSet2;
    private static Integer[] integerArray2;

    private static Set<Integer> integerSetEmpty;
    private static Integer[] integerArrayEmpty;

    private static Integer[] integerArrayRedundant;

    @BeforeClass
    public static void classSetup() {
        stringSet1 = Sets.newHashSet( "b", "d", "e", "g", "a", "f", "c" );
        stringArray1 = stringSet1.toArray( new String[stringSet1.size()] );

        stringSet2 = Sets.newHashSet( "k", "h", "u", "a", "f", "c" );
        stringArray2 = stringSet2.toArray( new String[stringSet2.size()] );

        stringSetEmpty = Sets.newHashSet();
        stringArrayEmpty = stringSetEmpty.toArray( new String[stringSetEmpty.size()] );

        stringArrayRedundant = new String[] { "k", "h", "u", "a", "f", "c", "k", "h" };

        integerSet1 = Sets.newHashSet( 1, 2, 4, 6, 7, 8, 9 );
        integerArray1 = integerSet1.toArray( new Integer[integerSet1.size()] );

        integerSet2 = Sets.newHashSet( 1, 2, 4, 6 );
        integerArray2 = integerSet2.toArray( new Integer[integerSet2.size()] );

        integerSetEmpty = Sets.newHashSet();
        integerArrayEmpty = integerSetEmpty.toArray( new Integer[integerSetEmpty.size()] );

        integerArrayRedundant = new Integer[] { 1, 2, 4, 6, 1, 2, 4, 6 };
    }

    // Similarity

    @Test
    public void testSimilarityStringArray() {
        Double res = Jaccard.similarity( stringArray1, stringArray2 );
        Assert.assertThat( res, Matchers.is( 0.3 ) );
    }

    @Test
    public void testSimilarityStringArrayEmpty() {
        Double res = Jaccard.similarity( stringArray1, stringArrayEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testSimilarityStringArrayEmptyToEmpty() {
        Double res = Jaccard.similarity( stringArrayEmpty, stringArrayEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityStringArrayRedundant() {
        Double res = Jaccard.similarity( stringArray1, stringArrayRedundant );
        Assert.assertThat( res, Matchers.is( 0.3 ) );
    }

    @Test
    public void testSimilarityStringSet() {
        Double res = Jaccard.similarity( stringSet1, stringSet2 );
        Assert.assertThat( res, Matchers.is( 0.3 ) );
    }

    @Test
    public void testSimilarityStringSetEmpty() {
        Double res = Jaccard.similarity( stringSet1, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testSimilarityStringSetEmptyToEmpty() {
        Double res = Jaccard.similarity( stringSetEmpty, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityIntegerArray() {
        Double res = Jaccard.similarity( integerArray1, integerArray2 );
        Assert.assertThat( res, Matchers.closeTo( 0.57143, 0.00001 ) );
    }

    @Test
    public void testSimilarityIntegerArrayEmpty() {
        Double res = Jaccard.similarity( integerArray1, integerArrayEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testSimilarityIntegerArrayEmptyToEmpty() {
        Double res = Jaccard.similarity( integerArrayEmpty, integerArrayEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityIntegerArrayRedundant() {
        Double res = Jaccard.similarity( integerArray1, integerArrayRedundant );
        Assert.assertThat( res, Matchers.closeTo( 0.57143, 0.00001 ) );
    }

    @Test
    public void testSimilarityIntegerSet() {
        Double res = Jaccard.similarity( integerSet1, integerSet2 );
        Assert.assertThat( res, Matchers.closeTo( 0.57143, 0.00001 ) );
    }

    @Test
    public void testSimilarityIntegerSetEmpty() {
        Double res = Jaccard.similarity( integerSet1, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testSimilarityIntegerSetEmptyToEmpty() {
        Double res = Jaccard.similarity( integerSetEmpty, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityIntegerSetDuplicate() {
        Double res = Jaccard.similarity( integerSet1, integerSet1 );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityIntegerArrayDuplicate() {
        Double res = Jaccard.similarity( integerArray1, integerArray1 );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityStringSetDuplicate() {
        Double res = Jaccard.similarity( stringSet1, stringSet1 );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testSimilarityStringArrayDuplicate() {
        Double res = Jaccard.similarity( stringArray1, stringArray1 );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    // Distance

    @Test
    public void testDistanceStringArray() {
        Double res = Jaccard.distance( stringArray1, stringArray2 );
        Assert.assertThat( res, Matchers.is( 0.7 ) );
    }

    @Test
    public void testDistanceStringArrayEmpty() {
        Double res = Jaccard.distance( stringArray1, stringArrayEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testDistanceStringArrayEmptyToEmpty() {
        Double res = Jaccard.distance( stringArrayEmpty, stringArrayEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceStringArrayRedundant() {
        Double res = Jaccard.distance( stringArray1, stringArrayRedundant );
        Assert.assertThat( res, Matchers.is( 0.7 ) );
    }

    @Test
    public void testDistanceStringSet() {
        Double res = Jaccard.distance( stringSet1, stringSet2 );
        Assert.assertThat( res, Matchers.is( 0.7 ) );
    }

    @Test
    public void testDistanceStringSetEmpty() {
        Double res = Jaccard.distance( stringSet1, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testDistanceStringSetEmptyToEmpty() {
        Double res = Jaccard.distance( stringSetEmpty, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceIntegerArray() {
        Double res = Jaccard.distance( integerArray1, integerArray2 );
        Assert.assertThat( res, Matchers.closeTo( 0.42857, 0.00001 ) );
    }

    @Test
    public void testDistanceIntegerArrayEmpty() {
        Double res = Jaccard.distance( integerArray1, integerArrayEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testDistanceIntegerArrayEmptyToEmpty() {
        Double res = Jaccard.distance( integerArrayEmpty, integerArrayEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceIntegerArrayRedundant() {
        Double res = Jaccard.distance( integerArray1, integerArrayRedundant );
        Assert.assertThat( res, Matchers.closeTo( 0.42857, 0.00001 ) );
    }

    @Test
    public void testDistanceIntegerSet() {
        Double res = Jaccard.distance( integerSet1, integerSet2 );
        Assert.assertThat( res, Matchers.closeTo( 0.42857, 0.00001 ) );
    }

    @Test
    public void testDistanceIntegerSetEmpty() {
        Double res = Jaccard.distance( integerSet1, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 1.0 ) );
    }

    @Test
    public void testDistanceIntegerSetEmptyToEmpty() {
        Double res = Jaccard.distance( integerSetEmpty, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceIntegerSetDuplicate() {
        Double res = Jaccard.distance( integerSet1, integerSet1 );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceIntegerArrayDuplicate() {
        Double res = Jaccard.distance( integerArray1, integerArray1 );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceStringSetDuplicate() {
        Double res = Jaccard.distance( stringSet1, stringSet1 );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    @Test
    public void testDistanceStringArrayDuplicate() {
        Double res = Jaccard.distance( stringArray1, stringArray1 );
        Assert.assertThat( res, Matchers.is( 0.0 ) );
    }

    // Intersect

    @Test
    public void testIntersectSizeString() {
        int res = Jaccard.intersectSize( stringSet1, stringSet2 );
        Assert.assertThat( res, Matchers.is( 3 ) );
    }

    @Test
    public void testIntersectSizeStringDuplicate() {
        int res = Jaccard.intersectSize( stringSet1, stringSet1 );
        Assert.assertThat( res, Matchers.is( stringSet1.size() ) );
    }

    @Test
    public void testIntersectSizeStringEmpty() {
        int res = Jaccard.intersectSize( stringSet1, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 0 ) );
    }

    @Test
    public void testIntersectSizeStringEmptyToEmpty() {
        int res = Jaccard.intersectSize( stringSetEmpty, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 0 ) );
    }

    @Test
    public void testIntersectSizeInteger() {
        int res = Jaccard.intersectSize( integerSet1, integerSet2 );
        Assert.assertThat( res, Matchers.is( 4 ) );
    }

    @Test
    public void testIntersectSizeIntegerDuplicate() {
        int res = Jaccard.intersectSize( integerSet1, integerSet1 );
        Assert.assertThat( res, Matchers.is( integerSet1.size() ) );
    }

    @Test
    public void testIntersectSizeIntegerEmpty() {
        int res = Jaccard.intersectSize( integerSet1, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 0 ) );
    }

    @Test
    public void testIntersectSizeIntegerEmptyToEmpty() {
        int res = Jaccard.intersectSize( integerSetEmpty, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 0 ) );
    }

    // Union

    @Test
    public void testUnionSizeString() {
        int res = Jaccard.unionSize( stringSet1, stringSet2 );
        Assert.assertThat( res, Matchers.is( 10 ) );
    }

    @Test
    public void testUnionSizeStringDuplicate() {
        int res = Jaccard.unionSize( stringSet1, stringSet1 );
        Assert.assertThat( res, Matchers.is( stringSet1.size() ) );
    }

    @Test
    public void testUnionSizeStringEmpty() {
        int res = Jaccard.unionSize( stringSet1, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( stringSet1.size() ) );
    }

    @Test
    public void testUnionSizeStringEmptyToEmpty() {
        int res = Jaccard.unionSize( stringSetEmpty, stringSetEmpty );
        Assert.assertThat( res, Matchers.is( 0 ) );
    }

    @Test
    public void testUnionSizeInteger() {
        int res = Jaccard.unionSize( integerSet1, integerSet2 );
        Assert.assertThat( res, Matchers.is( 7 ) );
    }

    @Test
    public void testUnionSizeIntegerDuplicate() {
        int res = Jaccard.unionSize( integerSet1, integerSet1 );
        Assert.assertThat( res, Matchers.is( integerSet1.size() ) );
    }

    @Test
    public void testUnionSizeIntegerEmpty() {
        int res = Jaccard.unionSize( integerSet1, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( integerSet1.size() ) );
    }

    @Test
    public void testUnionSizeIntegerEmptyToEmpty() {
        int res = Jaccard.unionSize( integerSetEmpty, integerSetEmpty );
        Assert.assertThat( res, Matchers.is( 0 ) );
    }
}
