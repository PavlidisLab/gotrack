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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ubc.pavlab.gotrack.BaseTest;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.GOEdition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.EditionDTO;
import ubc.pavlab.gotrack.model.dto.GOEditionDTO;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tests EnrichmentAnalysis Class, does not test accuracy of individual over-representation analyses as that
 * functionality is tested elsewhere.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class EnrichmentAnalysisTest extends BaseTest {

    private static final Logger log = Logger.getLogger( EnrichmentAnalysisTest.class );
    private List<GeneOntologyTerm> terms;
    private List<Gene> genes;

    private Edition ed1;
    private Edition ed2;

    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> populationMap;
    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> sampleMap;

    private Cache cache;

    private static final Species human = new Species( 7, "Human", "", 9606, null );

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
            genes.add( new Gene.GeneBuilder( i, "Gene" + String.format( "%02d", i ), "", human, null ).build() );
        }
        return genes;
    }

    @Before
    public void setup() {
        sampleMap = Maps.newHashMap();
        populationMap = Maps.newHashMap();

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

        ed1 = new Edition( new EditionDTO( 7, 1, Date.valueOf( "2016-01-01" ), 1, 1 ),
                new GOEdition( new GOEditionDTO( 1, Date.valueOf( "2016-01-01" ) ) ) );

        populationMap.put( ed1, geneGOMap );
        sampleMap.put( ed1, filterMap( geneGOMap, geneIds ) );

        geneGOMap = Maps.newHashMap();

        i = -1;
        for ( GeneOntologyTerm t : terms ) {
            geneGOMap.put( t, Sets.newHashSet( genes.subList( 0, i + 1 ) ) );
            i++;
        }

        ed2 = new Edition( new EditionDTO( 7, 2, Date.valueOf( "2016-02-01" ), 2, 2 ),
                new GOEdition( new GOEditionDTO( 2, Date.valueOf( "2016-02-01" ) ) ) );

        populationMap.put( ed2, geneGOMap );
        sampleMap.put( ed2, filterMap( geneGOMap, geneIds ) );

        Cache cacheSpy = Mockito.spy( new Cache() );

        Mockito.when( cacheSpy.getInferredAnnotationCount( Mockito.any( Species.class ), Mockito.any( Edition.class ),
                Mockito.any( GeneOntologyTerm.class ) ) ).thenAnswer( new Answer<Integer>() {
                    @Override
                    public Integer answer( InvocationOnMock invocation ) throws Throwable {
                        Object[] args = invocation.getArguments();
                        Map<GeneOntologyTerm, Set<Gene>> m = populationMap.get( args[1] );
                        if ( m == null ) return null;
                        Set<Gene> s = m.get( args[2] );
                        if ( s == null ) return null;
                        return s.size();
                    }
                } );

        Mockito.when( cacheSpy.getGeneCount( Mockito.any( Species.class ), Mockito.any( Edition.class ) ) )
                .thenAnswer( new Answer<Integer>() {
                    @Override
                    public Integer answer( InvocationOnMock invocation ) throws Throwable {
                        return genes.size();
                    }
                } );

        cache = cacheSpy;

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
    public void testGetThreshold() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getThreshold(), Matchers.is( 0.05 ) );

    }

    @Test
    public void testGetCutoff() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getCutoff( null ), Matchers.nullValue() );
        Assert.assertThat( e.getCutoff( ed1 ), Matchers.is( 0.05 ) );
        Assert.assertThat( e.getCutoff( ed2 ), Matchers.is( 0.05 ) );

    }

    @Test
    public void testGetEditions() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getEditions(), Matchers.containsInAnyOrder( ed1, ed2 ) );

    }

    @Test
    public void testSignificantResults() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getSignificantResults( null ), Matchers.nullValue() );

        Map<GeneOntologyTerm, EnrichmentResult> actual = e.getSignificantResults( ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 11 ) );
        Assert.assertThat( actual.size(), Matchers.is( expected.size() ) );
        Assert.assertThat( actual.keySet(), Matchers.is( expected ) );

        actual = e.getSignificantResults( ed2 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        expected = Sets.newHashSet( terms.subList( 3, 17 ) );
        Assert.assertThat( actual.size(), Matchers.is( expected.size() ) );
        Assert.assertThat( actual.keySet(), Matchers.is( expected ) );

    }

    @Test
    public void testGetResults() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Map<Edition, Map<GeneOntologyTerm, EnrichmentResult>> actualMap = e.getResults();
        Assert.assertThat( actualMap, Matchers.notNullValue() );

        // Ed1 
        Map<GeneOntologyTerm, EnrichmentResult> actual = actualMap.get( ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 17 ) );
        Assert.assertThat( actual.size(), Matchers.is( expected.size() ) );
        Assert.assertThat( actual.keySet(), Matchers.is( expected ) );

        // Ed2
        actual = actualMap.get( ed2 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        expected = Sets.newHashSet( terms.subList( 2, 17 ) );
        Assert.assertThat( actual.size(), Matchers.is( expected.size() ) );
        Assert.assertThat( actual.keySet(), Matchers.is( expected ) );

    }

    @Test
    public void testGetResults2() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        // Ed1 
        Map<GeneOntologyTerm, EnrichmentResult> actual = e.getResults( ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 17 ) );
        Assert.assertThat( actual.size(), Matchers.is( expected.size() ) );
        Assert.assertThat( actual.keySet(), Matchers.is( expected ) );

        // Ed2
        actual = e.getResults( ed2 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        expected = Sets.newHashSet( terms.subList( 2, 17 ) );
        Assert.assertThat( actual.size(), Matchers.is( expected.size() ) );
        Assert.assertThat( actual.keySet(), Matchers.is( expected ) );

    }

    @Test
    public void testGetResult() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        EnrichmentResult actual = e.getResult( ed1, new GeneOntologyTerm( "GO:0000003" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );

        actual = e.getResult( ed1, new GeneOntologyTerm( "GO:0000001" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );

        actual = e.getResult( ed1, new GeneOntologyTerm( "GO:0000100" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );

        actual = e.getResult( ed1, new GeneOntologyTerm( "GO:9000100" ) );
        Assert.assertThat( actual, Matchers.nullValue() );

    }

    @Test
    public void testGetCurrentSpeciesId() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getCurrentSpecies(), Matchers.is( human ) );

    }

    @Test
    public void testGetMinAnnotatedPopulation() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 10, 100, MultipleTestCorrection.BONFERRONI, 0.05,
                cache,
                human );

        Assert.assertThat( e.getMinAnnotatedPopulation(), Matchers.is( 10 ) );

    }

    @Test
    public void testGetMaxAnnotatedPopulation() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 10, 100, MultipleTestCorrection.BONFERRONI, 0.05,
                cache,
                human );

        Assert.assertThat( e.getMaxAnnotatedPopulation(), Matchers.is( 100 ) );

    }

    @Test
    public void testGetRawResults() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 0, 0, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Map<Edition, Enrichment<GeneOntologyTerm, Gene>> actualMap = e.getRawResults();
        Assert.assertThat( actualMap, Matchers.notNullValue() );

        Enrichment<GeneOntologyTerm, Gene> actual = actualMap.get( ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        actual = actualMap.get( ed2 );
        Assert.assertThat( actual, Matchers.notNullValue() );

        actual = actualMap.get( new Edition( new EditionDTO( 7, 999, Date.valueOf( "2016-01-01" ), 1, 999 ),
                new GOEdition( new GOEditionDTO( 999, Date.valueOf( "2016-01-01" ) ) ) ) );
        Assert.assertThat( actual, Matchers.nullValue() );

    }

    @Test
    public void testGetRejectedTerms() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Set<GeneOntologyTerm> actual = e.getRejectedTerms( ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual,
                Matchers.containsInAnyOrder( terms.get( 0 ), terms.get( 1 ), terms.get( 98 ), terms.get( 99 ) ) );

    }

    @Test
    public void testGetTermsSignificantInAnyEdition() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Set<GeneOntologyTerm> actual = e.getTermsSignificantInAnyEdition();
        Assert.assertThat( actual, Matchers.notNullValue() );
        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 17 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

    }

    @Test
    public void testGetTermsSignificant() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Set<GeneOntologyTerm> actual = e.getTermsSignificant( ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 11 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

        actual = e.getTermsSignificant( ed2 );
        Assert.assertThat( actual, Matchers.notNullValue() );
        expected = Sets.newHashSet( terms.subList( 3, 17 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

    }

    @Test
    public void testGetGeneSet() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Set<Gene> actual = e.getGeneSet( ed1, new GeneOntologyTerm( "GO:0000002" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Set<Gene> expected = Sets.newHashSet( genes.subList( 0, 2 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

    }

    @Test
    public void testGetTopNTerms() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Set<GeneOntologyTerm> actual = e.getTopNTerms( 5 );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual.size(), Matchers.is( 7 ) ); // 5 from each edition slightly overlapping (3-7, 5-9)
        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 9 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

    }

    @Test
    public void testGetTopNTerms2() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Set<GeneOntologyTerm> actual = e.getTopNTerms( 5, ed1 );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual.size(), Matchers.is( 5 ) );
        Set<GeneOntologyTerm> expected = Sets.newHashSet( terms.subList( 2, 7 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

        actual = e.getTopNTerms( 5, ed2 );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual.size(), Matchers.is( 5 ) );
        expected = Sets.newHashSet( terms.subList( 4, 9 ) );
        Assert.assertThat( actual, Matchers.is( expected ) );

    }

    @Test
    public void testGetResults3() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Map<Edition, EnrichmentResult> actual = e.getResults( new GeneOntologyTerm( "GO:0000003" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual.size(), Matchers.is( 1 ) );

        actual = e.getResults( new GeneOntologyTerm( "GO:0000002" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual.size(), Matchers.is( 0 ) );

        actual = e.getResults( new GeneOntologyTerm( "GO:0000004" ) );
        Assert.assertThat( actual, Matchers.notNullValue() );
        Assert.assertThat( actual.size(), Matchers.is( 2 ) );
    }

    @Test
    public void testGetTotalEditions() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getTotalEditions(), Matchers.is( 2 ) );
    }

    @Test
    public void testGetTotalGenes() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getTotalGenes(), Matchers.is( 5 ) ); // Gene 0 has no terms
    }

    @Test
    public void testGetTotalTerms() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getTotalTerms(), Matchers.is( 100 ) );
    }

    @Test
    public void testGetTotalResults() {
        EnrichmentAnalysis e = new EnrichmentAnalysis( sampleMap, 3, 98, MultipleTestCorrection.BONFERRONI, 0.05, cache,
                human );

        Assert.assertThat( e.getTotalResults(), Matchers.is( 192 ) );
    }

}
