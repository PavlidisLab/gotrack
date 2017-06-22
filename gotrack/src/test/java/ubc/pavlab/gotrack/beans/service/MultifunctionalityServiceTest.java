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

package ubc.pavlab.gotrack.beans.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import java.util.Set;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class MultifunctionalityServiceTest {

    private static final Logger log = Logger.getLogger( MultifunctionalityServiceTest.class );

    @InjectMocks
    private MultifunctionalityService multifunctionalityService = new MultifunctionalityService();

    private List<GeneOntologyTerm> terms;
    private List<Gene> genes;

    private Edition ed1;
    private Edition ed2;

    private Species species = new Species( 1234, "", "", 1234, 12345 );

    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> populationMap;
    private Map<Edition, Map<Gene, Set<GeneOntologyTerm>>> inversePopulationMap;

    @Mock
    private Cache cache;

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
            genes.add( new Gene.GeneBuilder( i, "Gene" + String.format( "%02d", i ), "",
                    new Species( 7, "Human", "", 9606, null ), null ).build() );
        }
        return genes;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks( this );
        populationMap = Maps.newHashMap();
        inversePopulationMap = Maps.newHashMap();

        Map<GeneOntologyTerm, Set<Gene>> geneGOMap = Maps.newHashMap();
        Map<Gene, Set<GeneOntologyTerm>> GOGeneMap = Maps.newHashMap();
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

        i = 0;
        for ( Gene g : genes ) {
            GOGeneMap.put( g, Sets.newHashSet( terms.subList( i, 100 ) ) );
            i++;
        }

        ed1 = new Edition( new EditionDTO( 7, 1, Date.valueOf( "2016-01-01" ), 1, 1 ),
                new GOEdition( new GOEditionDTO( 1, Date.valueOf( "2016-01-01" ) ) ) );

        populationMap.put( ed1, geneGOMap );
        inversePopulationMap.put( ed1, GOGeneMap );

        geneGOMap = Maps.newHashMap();
        GOGeneMap = Maps.newHashMap();

        i = -1;
        for ( GeneOntologyTerm t : terms ) {
            geneGOMap.put( t, Sets.newHashSet( genes.subList( 0, i + 1 ) ) );
            i++;
        }

        i = 1;
        for ( Gene g : genes ) {
            GOGeneMap.put( g, Sets.newHashSet( terms.subList( i, 100 ) ) );
            i++;
        }

        ed2 = new Edition( new EditionDTO( 7, 2, Date.valueOf( "2016-02-01" ), 2, 2 ),
                new GOEdition( new GOEditionDTO( 2, Date.valueOf( "2016-02-01" ) ) ) );

        populationMap.put( ed2, geneGOMap );
        inversePopulationMap.put( ed2, GOGeneMap );

        Mockito.when( cache.getInferredAnnotationCount( Mockito.any( Species.class ), Mockito.any( Edition.class ),
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

        Mockito.when( cache.getGeneCount( Mockito.any( Species.class ), Mockito.any( Edition.class ) ) )
                .thenAnswer( new Answer<Integer>() {
                    @Override
                    public Integer answer( InvocationOnMock invocation ) throws Throwable {
                        return genes.size();
                    }
                } );

    }

    @Test
    public void testCalculateMultifunctionality() {
        // Gene 99 has 2 terms, Term 100 and 99, Term 100 has all genes and is therefore left out of the calc
        // Term 99 has all genes except for Gene 100
        // multi should = 1/99

        Set<GeneOntologyTerm> terms = inversePopulationMap.get( ed1 ).get( genes.get( 98 ) );
        Double multi = multifunctionalityService.multifunctionality( terms, species, ed1 );

        Assert.assertThat( multi, Matchers.is( 1.0 / 99 ) );

        // Gene 1 has all terms
        // so multi = SUM_i( 1 / ( i * ( 100 - i) ) ), i=1..99
        terms = inversePopulationMap.get( ed1 ).get( genes.get( 0 ) );
        multi = multifunctionalityService.multifunctionality( terms, species, ed1 );

        Assert.assertThat( multi, Matchers.closeTo( 0.103547550352792, 0.000000000000001 ) );

        // Gene 48 has 53 terms, Term 48-100
        // so multi = SUM_i( 1 / ( i * ( 100 - i) ) ), i=48..99
        terms = inversePopulationMap.get( ed1 ).get( genes.get( 47 ) );
        multi = multifunctionalityService.multifunctionality( terms, species, ed1 );

        Assert.assertThat( multi, Matchers.closeTo( 0.052774576266062, 0.000000000000001 ) );
    }

}
