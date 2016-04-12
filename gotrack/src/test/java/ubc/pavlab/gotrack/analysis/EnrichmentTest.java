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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import ubc.pavlab.gotrack.BaseTest;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.hashkey.HyperUCFKey;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentTest extends BaseTest {

    private static final Logger log = Logger.getLogger( EnrichmentTest.class );
    private List<GeneOntologyTerm> terms;
    private List<Gene> genes;

    private StandardPopulation<GeneOntologyTerm, Gene> sample;
    private StandardPopulation<GeneOntologyTerm, Gene> population;

    @BeforeClass
    public static void classSetup() {
    }

    private List<GeneOntologyTerm> createTerms( Integer n ) {
        List<GeneOntologyTerm> terms = Lists.newArrayList();

        for ( int i = 1; i < n + 1; i++ ) {
            String goId = "GO:00000" + String.format( "%02d", i );
            terms.add( new GeneOntologyTerm( goId ) );//, RandomStringUtils.randomAlphabetic( 10 ), aspects.get( i % 3 ) ) );
        }
        return terms;
    }

    private List<Gene> createGenes( Integer n ) {
        List<Gene> genes = Lists.newArrayList();

        for ( int i = 1; i < n + 1; i++ ) {
            genes.add( new Gene.GeneBuilder( i, "Gene" + String.format( "%02d", i ),
                    new Species( 7, "Human", "", 9606, null ), Collections.<Accession> emptySet() ).build() );
        }
        return genes;
    }

    @Before
    public void setup() {
        Map<GeneOntologyTerm, Set<Gene>> geneGOMap = Maps.newHashMap();
        terms = createTerms( 100 );
        genes = createGenes( 100 );

        /*
         * Term 1 has (Gene 1)
         * Term 2 has (Gene 1, Gene 2)
         * ...
         * Term 100 has (Gene 1, Gene 2, ..., Gene 100)
         * 
         * Gene 1 has (Term 1, Term 2, ..., Term 100)
         * Gene 2 has (Term 2, Term 3, ..., Term 100)
         * ...
         * Gene 100 has (Term 100)
         * 
         */
        int i = 0;
        for ( GeneOntologyTerm t : terms ) {
            geneGOMap.put( t, Sets.newHashSet( genes.subList( 0, i + 1 ) ) );
            i++;
        }

        Set<Integer> geneIds = Sets.newHashSet( 0, 1, 2, 3, 4, 100 );

        Map<GeneOntologyTerm, Set<Gene>> sampleMap = filterMap( geneGOMap, geneIds );
        //        log.info( sampleMap.size() );
        //        log.info( sampleMap.get( new GeneOntologyTerm( "GO:0000001" ) ) );
        sample = Population.standardPopulation( sampleMap );

        population = Population.standardPopulation( geneGOMap );

    }

    private Map<GeneOntologyTerm, Set<Gene>> filterMap(
            Map<GeneOntologyTerm, Set<Gene>> allGeneMap, Set<Integer> geneIds ) {
        Map<GeneOntologyTerm, Set<Gene>> geneGOMap = Maps.newHashMap();
        for ( Entry<GeneOntologyTerm, Set<Gene>> e2 : allGeneMap.entrySet() ) {
            GeneOntologyTerm go = e2.getKey();
            Set<Gene> s = e2.getValue();
            Set<Gene> newSet = Sets.newHashSet();
            for ( Gene gene : s ) {
                if ( geneIds.contains( gene.getId() ) ) {
                    newSet.add( gene );
                }
            }
            if ( newSet.size() > 0 ) {
                geneGOMap.put( go, newSet );
            }
        }

        return geneGOMap;

    }

    @After
    public void after() {

    }

    @Test
    public void testEnrichmentSingleCutoff() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );
        Assert.assertThat( e.getCutoff(), Matchers.is( 0.05 ) );

    }

    @Test
    public void testEnrichmentSingleSampleAnnotated() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Assert.assertThat( er.getSampleAnnotated(), Matchers.is( sample.countProperty( t ) ) );
    }

    @Test
    public void testEnrichmentSingleSampleSize() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Assert.assertThat( er.getSampleSize(), Matchers.is( sample.size() ) );
    }

    @Test
    public void testEnrichmentSinglePopulationAnnotated() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Assert.assertThat( er.getPopulationAnnotated(), Matchers.is( population.countProperty( t ) ) );
    }

    @Test
    public void testEnrichmentSinglePopulationSize() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Assert.assertThat( er.getPopulationSize(), Matchers.is( population.size() ) );
    }

    @Test
    public void testEnrichmentSingleExpected() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Integer sampleAnnotated = sample.countProperty( t );
        Integer sampleSize = sample.size();
        Integer populationSize = population.size();

        Assert.assertThat( er.getExpected(),
                Matchers.is( sampleSize * ( ( double ) sampleAnnotated / populationSize ) ) );

    }

    @Test
    public void testEnrichmentSingleRank() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Assert.assertThat( er.getRank(), Matchers.is( 0 ) );

    }

    @Test
    public void testEnrichmentSingleFractionalRank() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Assert.assertThat( er.getFractionalRank(), Matchers.is( 0.0 ) );

    }

    @Test
    public void testEnrichmentSinglePValue() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );

        Integer sampleAnnotated = sample.countProperty( t );
        Integer sampleSize = sample.size();
        Integer populationAnnotated = population.countProperty( t );
        Integer populationSize = population.size();

        Assert.assertThat( er.getPvalue(),
                Matchers.is( HyperGeometricUtilities.upperCumulativeProbabilityLogMethod( sampleAnnotated,
                        populationAnnotated, sampleSize, populationSize ) ) );

    }

    @Test
    public void testEnrichmentSingleSigResults() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        Map<GeneOntologyTerm, EnrichmentResult> res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

    @Test
    public void testEnrichmentSingleTopN() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );

        // Only Gene 1 is annotated with Term 1

        e.runAnalysis( sample, population, Sets.newHashSet( t ) );

        Set<GeneOntologyTerm> res = e.getTopNTerms( 5 );
        // TODO does TopN mean top N significant or top N any?

        //        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        //        Assert.assertThat( res, Matchers.contains( t ) );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

    @Test
    public void testEnrichmentMultipleRank() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" );

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getRank(), Matchers.is( 3 ) );

        er = e.getResult( t2 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getRank(), Matchers.is( 2 ) );

        er = e.getResult( t3 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getRank(), Matchers.is( 1 ) );

        er = e.getResult( t4 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getRank(), Matchers.is( 0 ) );

    }

    @Test
    public void testEnrichmentMultipleFractionalRank() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" );

        // Let's make t2 = t3 by mocking population counts, in other words I'm annotating Gene 3 with Term 2
        StandardPopulation<GeneOntologyTerm, Gene> sampleSpy = Mockito.spy( sample );
        Mockito.when( sampleSpy.countProperty( t2 ) ).thenReturn( 3 );

        StandardPopulation<GeneOntologyTerm, Gene> populationSpy = Mockito.spy( population );
        Mockito.when( populationSpy.countProperty( t2 ) ).thenReturn( 3 );

        e.runAnalysis( sampleSpy, populationSpy, Sets.newHashSet( t, t2, t3, t4 ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getFractionalRank(), Matchers.is( 3.0 ) );

        er = e.getResult( t2 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getFractionalRank(), Matchers.is( 1.5 ) );

        er = e.getResult( t3 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getFractionalRank(), Matchers.is( 1.5 ) );

        er = e.getResult( t4 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getFractionalRank(), Matchers.is( 0.0 ) );

    }

    @Test
    public void testEnrichmentMultiplePValue() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" );

        HashSet<GeneOntologyTerm> testSet = Sets.newHashSet( t, t2, t3, t4 );

        e.runAnalysis( sample, population, testSet );

        Integer sampleSize = sample.size();
        Integer populationSize = population.size();

        for ( GeneOntologyTerm test : testSet ) {
            Integer sampleAnnotated = sample.countProperty( test );
            Integer populationAnnotated = population.countProperty( test );

            EnrichmentResult er = e.getResult( test );
            Assert.assertThat( er, Matchers.notNullValue() );
            Assert.assertThat( er.getPvalue(), Matchers
                    .is( testSet.size() * HyperGeometricUtilities.upperCumulativeProbabilityLogMethod( sampleAnnotated,
                            populationAnnotated, sampleSize, populationSize ) ) );
        }

    }

    @Test
    public void testEnrichmentMultipleSigResults() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" );

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        Map<GeneOntologyTerm, EnrichmentResult> res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 3 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t2, t3, t4 ) );

        e.setThreshold( 1.0 );
        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 4 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t, t2, t3, t4 ) );

        e.setThreshold( 0.005 );
        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 2 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t3, t4 ) );

    }

    @Test
    public void testEnrichmentMultipleTopN() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" );

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        Set<GeneOntologyTerm> res = e.getTopNTerms( 5 );
        // TODO does TopN mean top N significant or top N any?

        //        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        //        Assert.assertThat( res, Matchers.contains( t ) );

        Assert.assertThat( res.size(), Matchers.is( 3 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( t2, t3, t4 ) );

        res = e.getTopNTerms( 1 );

        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        Assert.assertThat( res, Matchers.contains( t4 ) );

    }

    @Test
    public void testEnrichmentMultipleAll() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );

        e.runAnalysis( sample, population );

        Integer sampleSize = sample.size();
        Integer populationSize = population.size();

        for ( GeneOntologyTerm test : terms ) {
            Integer sampleAnnotated = sample.countProperty( test );
            Integer populationAnnotated = population.countProperty( test );

            EnrichmentResult er = e.getResult( test );
            Assert.assertThat( er, Matchers.notNullValue() );
            Assert.assertThat( er.getPvalue(), Matchers
                    .is( Math.min( 1.0,
                            terms.size() * HyperGeometricUtilities.upperCumulativeProbabilityLogMethod( sampleAnnotated,
                                    populationAnnotated, sampleSize, populationSize ) ) ) );
        }

    }

    @Test
    public void testEnrichmentMultipleRankRejections() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 2, 3 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" ); // Rejected
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" ); // Rejected

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.nullValue() );

        er = e.getResult( t2 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getRank(), Matchers.is( 1 ) );

        er = e.getResult( t3 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getRank(), Matchers.is( 0 ) );

        er = e.getResult( t4 );
        Assert.assertThat( er, Matchers.nullValue() );

    }

    @Test
    public void testEnrichmentMultipleFractionalRankRejections() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 2, 3 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" ); // Rejected
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" ); // Rejected

        // Let's make t2 = t3 by mocking population counts, in other words I'm annotating Gene 3 with Term 2
        StandardPopulation<GeneOntologyTerm, Gene> sampleSpy = Mockito.spy( sample );
        Mockito.when( sampleSpy.countProperty( t2 ) ).thenReturn( 3 );

        StandardPopulation<GeneOntologyTerm, Gene> populationSpy = Mockito.spy( population );
        Mockito.when( populationSpy.countProperty( t2 ) ).thenReturn( 3 );

        e.runAnalysis( sampleSpy, populationSpy, Sets.newHashSet( t, t2, t3, t4 ) );

        EnrichmentResult er = e.getResult( t );
        Assert.assertThat( er, Matchers.nullValue() );

        er = e.getResult( t2 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getFractionalRank(), Matchers.is( 0.5 ) );

        er = e.getResult( t3 );
        Assert.assertThat( er, Matchers.notNullValue() );
        Assert.assertThat( er.getFractionalRank(), Matchers.is( 0.5 ) );

        er = e.getResult( t4 );
        Assert.assertThat( er, Matchers.nullValue() );

    }

    @Test
    public void testEnrichmentMultiplePValueRejections() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 2, 3 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" ); // Rejected
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" ); // Rejected

        Set<GeneOntologyTerm> testSet = Sets.newHashSet( t, t2, t3, t4 );
        Set<GeneOntologyTerm> accepted = Sets.newHashSet( t2, t3 );
        Set<GeneOntologyTerm> rejected = Sets.newHashSet( t, t4 );

        e.runAnalysis( sample, population, testSet );

        Integer sampleSize = sample.size();
        Integer populationSize = population.size();

        for ( GeneOntologyTerm test : accepted ) {
            Integer sampleAnnotated = sample.countProperty( test );
            Integer populationAnnotated = population.countProperty( test );

            EnrichmentResult er = e.getResult( test );
            Assert.assertThat( er, Matchers.notNullValue() );
            Assert.assertThat( er.getPvalue(), Matchers
                    .is( accepted.size() * HyperGeometricUtilities.upperCumulativeProbabilityLogMethod( sampleAnnotated,
                            populationAnnotated, sampleSize, populationSize ) ) );
        }

        for ( GeneOntologyTerm test : rejected ) {
            EnrichmentResult er = e.getResult( test );
            Assert.assertThat( er, Matchers.nullValue() );
        }

    }

    @Test
    public void testEnrichmentMultipleSigResultsRejections() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 2, 3 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" ); // Rejected
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" ); // Rejected

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        Map<GeneOntologyTerm, EnrichmentResult> res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 2 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t2, t3 ) );

        e.setThreshold( 1.0 );
        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 2 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t2, t3 ) );

        e.setThreshold( 0.005 );
        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 2 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t2, t3 ) );

    }

    @Test
    public void testEnrichmentMultipleTopNRejections() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 2, 3 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" ); // Rejected
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" ); // Rejected

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        Set<GeneOntologyTerm> res = e.getTopNTerms( 5 );
        // TODO does TopN mean top N significant or top N any?

        //        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        //        Assert.assertThat( res, Matchers.contains( t ) );

        Assert.assertThat( res.size(), Matchers.is( 2 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( t2, t3 ) );

        res = e.getTopNTerms( 1 );

        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        Assert.assertThat( res, Matchers.contains( t3 ) );

    }

    @Test
    public void testEnrichmentMultipleAllRejections() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 10, 20 );

        e.runAnalysis( sample, population );

        Integer sampleSize = sample.size();
        Integer populationSize = population.size();

        Set<GeneOntologyTerm> accepted = Sets.newHashSet( terms.subList( 9, 20 ) );
        Set<GeneOntologyTerm> rejected = Sets.difference( Sets.newHashSet( terms ), accepted );

        Assert.assertThat( e.getRejectedTerms().size(), Matchers.is( rejected.size() ) );
        Assert.assertEquals( e.getRejectedTerms(), rejected );

        for ( GeneOntologyTerm test : accepted ) {
            Integer sampleAnnotated = sample.countProperty( test );
            Integer populationAnnotated = population.countProperty( test );

            EnrichmentResult er = e.getResult( test );
            Assert.assertThat( er, Matchers.notNullValue() );
            Assert.assertThat( er.getPvalue(), Matchers
                    .is( accepted.size() * HyperGeometricUtilities.upperCumulativeProbabilityLogMethod( sampleAnnotated,
                            populationAnnotated, sampleSize, populationSize ) ) );
        }

        for ( GeneOntologyTerm test : rejected ) {
            EnrichmentResult er = e.getResult( test );
            Assert.assertThat( er, Matchers.nullValue() );
        }

    }

    @Test
    public void testEnrichmentMultipleSigResultsBenjaminiHochbergStepUp() {
        Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BH, 0.05, 0, 0 );

        GeneOntologyTerm t = new GeneOntologyTerm( "GO:0000001" );
        GeneOntologyTerm t2 = new GeneOntologyTerm( "GO:0000002" );
        GeneOntologyTerm t3 = new GeneOntologyTerm( "GO:0000003" );
        GeneOntologyTerm t4 = new GeneOntologyTerm( "GO:0000004" );

        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        Map<GeneOntologyTerm, EnrichmentResult> res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 3 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t2, t3, t4 ) );

        e.setThreshold( 1.0 );
        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 4 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t, t2, t3, t4 ) );

        e.setThreshold( 0.005 );
        e.runAnalysis( sample, population, Sets.newHashSet( t, t2, t3, t4 ) );

        res = e.getSignificantResults();
        Assert.assertThat( res.size(), Matchers.is( 3 ) );
        Assert.assertThat( res.keySet(), Matchers.containsInAnyOrder( t2, t3, t4 ) );

    }

    @Test(timeout = 60000)
    public void testLoad() {
        int gsetSize = 100;
        int hitListSize = 200;
        Random random = new Random( 1 );
        Map<GeneOntologyTerm, Set<Gene>> geneGOMap = Maps.newHashMap();
        terms = createTerms( 40000 );
        genes = createGenes( 20000 );

        for ( GeneOntologyTerm t : terms ) {
            int size = random.nextInt( gsetSize );
            int rnd = random.nextInt( genes.size() - size );
            geneGOMap.put( t, Sets.newHashSet( genes.subList( rnd, rnd + size ) ) );
            //            HashSet<Integer> gset = getNRandom( 50, genes.size() );
            //            for ( Integer integer : gset ) {
            //                
            //            }

        }

        // Make a sample hit list of size hitListSize
        Set<Integer> geneIds = getNRandom( hitListSize, genes.size() );

        Map<GeneOntologyTerm, Set<Gene>> sampleMap = filterMap( geneGOMap, geneIds );
        //        log.info( sampleMap.size() );
        //        log.info( sampleMap.get( new GeneOntologyTerm( "GO:0000001" ) ) );
        sample = Population.standardPopulation( sampleMap );

        population = Population.standardPopulation( geneGOMap );

        // run analysis X times, get average time
        long start = System.currentTimeMillis();
        int iterations = 1000;
        TObjectDoubleHashMap<HyperUCFKey> logProbCache = new TObjectDoubleHashMap<>(); // simulates warm caches
        for ( int i = 0; i < iterations; i++ ) {
            Enrichment<GeneOntologyTerm, Gene> e = new Enrichment<>( MultipleTestCorrection.BONFERRONI, 0.05, 0, 0 );
            e.runAnalysis( sample, population, logProbCache );
        }

        long end = System.currentTimeMillis();
        log.info( iterations + " iterations of enrichment analysis took " + ( end - start )
                + " MilliSeconds. An Average of "
                + ( end - start ) / iterations );

    }

    private static HashSet<Integer> getNRandom( int n, int max ) {
        HashSet<Integer> set = new HashSet<Integer>();
        Random random = new Random();

        while ( set.size() < n ) {
            int thisOne = random.nextInt( max - 1 );
            set.add( thisOne );
        }

        return set;
    }

}
