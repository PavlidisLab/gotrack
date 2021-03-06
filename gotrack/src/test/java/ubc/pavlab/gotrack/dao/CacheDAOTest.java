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

package ubc.pavlab.gotrack.dao;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.*;
import ubc.pavlab.gotrack.BaseTest;
import ubc.pavlab.gotrack.model.dto.*;
import ubc.pavlab.gotrack.utilities.Tuples;
import ubc.pavlab.gotrack.utilities.Tuples.Tuple2;

import java.sql.Date;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CacheDAOTest extends BaseTest {

    private static final Logger log = Logger.getLogger( CacheDAOTest.class );
    private static CacheDAO cacheDAO;

    @BeforeClass
    public static void classSetup() {
        cacheDAO = daoFactory.getCacheDAO();
    }

    @Before
    public void setup() {

    }

    @After
    public void after() {

    }

    @Test
    public void testGetGOAnnotationCountsDeep() {
        List<AnnotationCountDTO> res = cacheDAO.getGOAnnotationCounts( 7,0 );
        Assert.assertThat( res.size(), Matchers.is( 3020 ) );

        for ( AnnotationCountDTO dto : res ) {
            // Individual spot checks
            if ( dto.getEdition() == 144 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 145 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 146 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 147 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            }
        }

        res = cacheDAO.getGOAnnotationCounts( 8,0 );
        Assert.assertThat( res.size(), Matchers.is( 2395 ) );

        for ( AnnotationCountDTO dto : res ) {
                if ( dto.getEdition() == 130 && dto.getGoId().equals( "GO:0098772" ) ) {
                    Assert.assertThat( dto.getDirectCount(), Matchers.is( 0 ) );
                    Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
                } else if ( dto.getEdition() == 131 && dto.getGoId().equals( "GO:0098772" ) ) {
                    Assert.assertThat( dto.getDirectCount(), Matchers.is( 0 ) );
                    Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
                } else if ( dto.getEdition() == 132 && dto.getGoId().equals( "GO:0098772" ) ) {
                    Assert.assertThat( dto.getDirectCount(), Matchers.is( 0 ) );
                    Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
                } else if ( dto.getEdition() == 133 && dto.getGoId().equals( "GO:0098772" ) ) {
                    Assert.assertThat( dto.getDirectCount(), Matchers.is( 0 ) );
                    Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
                }
//            }
        }

        res = cacheDAO.getGOAnnotationCounts( 7,146 );
        Assert.assertThat( res.size(), Matchers.is( 1542 ) );

        for ( AnnotationCountDTO dto : res ) {
            // Individual spot checks
            if ( dto.getEdition() == 144 ) {
                fail("Edition 144 should not be in this query.");
            } else if ( dto.getEdition() == 145 ) {
                fail("Edition 145 should not be in this query.");
            } else if ( dto.getEdition() == 146 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 147 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            }
        }
    }

    @Test
    public void testGetGOAnnotationCounts() {
        List<AnnotationCountDTO> res = cacheDAO.getGOAnnotationCounts( 7,0 );
        Assert.assertThat( res.size(), Matchers.is( 3020 ) );

        Multiset<Integer> editions = HashMultiset.create();
        Multiset<String> someGoIds = HashMultiset.create();

        for ( AnnotationCountDTO dto : res ) {
            someGoIds.add( dto.getGoId() );
            editions.add( dto.getEdition() );

        }
        res = cacheDAO.getGOAnnotationCounts( 8,0 );
        Assert.assertThat( res.size(), Matchers.is( 2395 ) );

        for ( AnnotationCountDTO dto : res ) {
            someGoIds.add( dto.getGoId() );
            editions.add( dto.getEdition() );

        }

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 8 ) );
        Assert.assertThat( editions.count( 130 ), Matchers.is( 577 ) );
        Assert.assertThat( editions.count( 131 ), Matchers.is( 582 ) );
        Assert.assertThat( editions.count( 132 ), Matchers.is( 617 ) );
        Assert.assertThat( editions.count( 133 ), Matchers.is( 619 ) );
        Assert.assertThat( editions.count( 144 ), Matchers.is( 737 ) );
        Assert.assertThat( editions.count( 145 ), Matchers.is( 741 ) );
        Assert.assertThat( editions.count( 146 ), Matchers.is( 771 ) );
        Assert.assertThat( editions.count( 147 ), Matchers.is( 771 ) );

        // some distinct goids
        Assert.assertThat( someGoIds.elementSet().size(), Matchers.is( 831 ) );
        Assert.assertThat( someGoIds.count( "GO:1901677" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1901679" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1901698" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1901699" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1901700" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1901701" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1901858" ), Matchers.is( 4 ) );
        Assert.assertThat( someGoIds.count( "GO:1902235" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902236" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902531" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902532" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902578" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902580" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902582" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902589" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902679" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1902680" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903426" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903429" ), Matchers.is( 4 ) );
        Assert.assertThat( someGoIds.count( "GO:1903431" ), Matchers.is( 4 ) );
        Assert.assertThat( someGoIds.count( "GO:1903506" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903507" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903508" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903522" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903530" ), Matchers.is( 4 ) );
        Assert.assertThat( someGoIds.count( "GO:1903573" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1903578" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1904356" ), Matchers.is( 1 ) );
        Assert.assertThat( someGoIds.count( "GO:1904357" ), Matchers.is( 1 ) );
        Assert.assertThat( someGoIds.count( "GO:1990314" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1990519" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:1990542" ), Matchers.is( 8 ) );
        Assert.assertThat( someGoIds.count( "GO:2000026" ), Matchers.is( 4 ) );
        Assert.assertThat( someGoIds.count( "GO:2000112" ), Matchers.is( 8 ) );

    }

    @Test
    public void testGetGOAnnotationCountsSpeciesRestrictedDeep() {
        List<AnnotationCountDTO> res = cacheDAO.getGOAnnotationCounts( 7, 0 );
        Assert.assertThat( res.size(), Matchers.is( 3020 ) );

        for ( AnnotationCountDTO dto : res ) {

            // Individual spot checks


            if ( dto.getEdition() == 144 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 145 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 146 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            } else if ( dto.getEdition() == 147 && dto.getGoId().equals( "GO:0004252" ) ) {
                Assert.assertThat( dto.getDirectCount(), Matchers.is( 1 ) );
                Assert.assertThat( dto.getInferredCount(), Matchers.is( 1 ) );
            }

        }
    }

    @Test
    public void testGetGOAnnotationCountsSpeciesRestricted() {
        List<AnnotationCountDTO> res = cacheDAO.getGOAnnotationCounts( 7,0 );
        Assert.assertThat( res.size(), Matchers.is( 3020 ) );

        Multiset<Integer> editions = HashMultiset.create();

        for ( AnnotationCountDTO dto : res ) {
            editions.add( dto.getEdition() );
        }

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 4 ) );

        Assert.assertThat( editions.count( 144 ), Matchers.is( 737 ) );
        Assert.assertThat( editions.count( 145 ), Matchers.is( 741 ) );
        Assert.assertThat( editions.count( 146 ), Matchers.is( 771 ) );
        Assert.assertThat( editions.count( 147 ), Matchers.is( 771 ) );

        res = cacheDAO.getGOAnnotationCounts( 8,0 );
        Assert.assertThat( res.size(), Matchers.is( 2395 ) );

        editions = HashMultiset.create();

        for ( AnnotationCountDTO dto : res ) {
            editions.add( dto.getEdition() );
        }

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 4 ) );

        Assert.assertThat( editions.count( 130 ), Matchers.is( 577 ) );
        Assert.assertThat( editions.count( 131 ), Matchers.is( 582 ) );
        Assert.assertThat( editions.count( 132 ), Matchers.is( 617 ) );
        Assert.assertThat( editions.count( 133 ), Matchers.is( 619 ) );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllGOEditions() {
        List<GOEditionDTO> res = cacheDAO.getAllGOEditions();

        List<Tuple2<Integer, Date>> records = Lists.newArrayList();

        for ( GOEditionDTO dto : res ) {
            records.add( Tuples.tuple2( dto.getId(), dto.getDate() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple2( 170, Date.valueOf( "2015-04-01" ) ),
                        Tuples.tuple2( 175, Date.valueOf( "2015-05-01" ) ),
                        Tuples.tuple2( 173, Date.valueOf( "2015-06-01" ) ),
                        Tuples.tuple2( 174, Date.valueOf( "2015-07-01" ) ) ) );
    }

    @Test
    public void testGetAllEditions() {
        List<EditionDTO> res = cacheDAO.getAllEditions( null );
        Assert.assertThat( res.size(), Matchers.is( 8 ) );

        // Should be ordered by edition
        EditionDTO dto = res.get( 0 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 8 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 130 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-04-27" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 170 ) );

        dto = res.get( 1 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 8 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 131 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-06-02" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 173 ) );

        dto = res.get( 2 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 8 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 132 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-06-22" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 173 ) );

        dto = res.get( 3 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 8 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 133 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-07-20" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 174 ) );

        dto = res.get( 4 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 144 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-04-27" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 170 ) );

        dto = res.get( 5 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 145 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-05-26" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 175 ) );

        dto = res.get( 6 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 146 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-06-22" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 173 ) );

        dto = res.get( 7 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 147 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-07-20" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 174 ) );

    }

    @Test
    public void testGetAllEditionsSpeciesRestricted() {
        List<EditionDTO> res = cacheDAO.getAllEditions( new int[] { 7 } );
        Assert.assertThat( res.size(), Matchers.is( 4 ) );

        // Should be ordered by edition

        EditionDTO dto = res.get( 0 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 144 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-04-27" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 170 ) );

        dto = res.get( 1 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 145 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-05-26" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 175 ) );

        dto = res.get( 2 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 146 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-06-22" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 173 ) );

        dto = res.get( 3 );
        Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getEdition(), Matchers.is( 147 ) );
        Assert.assertThat( dto.getDate(), Matchers.is( Date.valueOf( "2015-07-20" ) ) );
        Assert.assertThat( dto.getGoEditionId(), Matchers.is( 174 ) );
    }

    @Test
    public void testGetAggregates() {
        List<AggregateDTO> res = cacheDAO.getAggregates( null );
        Assert.assertThat( res.size(), Matchers.is( 8 ) );

        for ( AggregateDTO dto : res ) {
            if ( dto.getSpecies() == 7 ) {
                if ( dto.getEdition() == 144 ) {
                    Assert.assertThat( dto.getGeneCount(), Matchers.is( 10 ) );
                    Assert.assertThat( dto.getAvgDirectTermsForGene(), Matchers.is( 21.4 ) );
                    Assert.assertThat( dto.getAvgInferredTermsForGene(), Matchers.is( 138.9 ) );
                    Assert.assertThat( dto.getAvgInferredGenesForTerm(), Matchers.closeTo( 1.885, 0.001 ) );
                    Assert.assertThat( dto.getAvgMultifunctionality(),
                            Matchers.closeTo( 0.000052743329, 0.000000001 ) );
                    Assert.assertThat( dto.getAvgDirectSimilarity(), Matchers.closeTo( 0.950, 0.001 ) );
                    Assert.assertThat( dto.getAvgInferredSimilarity(), Matchers.closeTo( 0.954, 0.001 ) );
                } else if ( dto.getEdition() == 145 ) {
                    Assert.assertThat( dto.getGeneCount(), Matchers.is( 10 ) );
                    Assert.assertThat( dto.getAvgDirectTermsForGene(), Matchers.is( 21.4 ) );
                    Assert.assertThat( dto.getAvgInferredTermsForGene(), Matchers.is( 139.5 ) );
                    Assert.assertThat( dto.getAvgInferredGenesForTerm(), Matchers.closeTo( 1.883, 0.001 ) );
                    Assert.assertThat( dto.getAvgMultifunctionality(),
                            Matchers.closeTo( 0.000052782596, 0.000000001 ) );
                    Assert.assertThat( dto.getAvgDirectSimilarity(), Matchers.closeTo( 0.954, 0.001 ) );
                    Assert.assertThat( dto.getAvgInferredSimilarity(), Matchers.closeTo( 0.959, 0.001 ) );
                } else if ( dto.getEdition() == 146 ) {
                    Assert.assertThat( dto.getGeneCount(), Matchers.is( 10 ) );
                    Assert.assertThat( dto.getAvgDirectTermsForGene(), Matchers.is( 21.8 ) );
                    Assert.assertThat( dto.getAvgInferredTermsForGene(), Matchers.is( 143.4 ) );
                    Assert.assertThat( dto.getAvgInferredGenesForTerm(), Matchers.closeTo( 1.860, 0.001 ) );
                    Assert.assertThat( dto.getAvgMultifunctionality(),
                            Matchers.closeTo( 0.000053296431, 0.000000001 ) );
                    Assert.assertThat( dto.getAvgDirectSimilarity(), Matchers.closeTo( 0.991, 0.001 ) );
                    Assert.assertThat( dto.getAvgInferredSimilarity(), Matchers.closeTo( 0.990, 0.001 ) );
                } else if ( dto.getEdition() == 147 ) {
                    Assert.assertThat( dto.getGeneCount(), Matchers.is( 10 ) );
                    Assert.assertThat( dto.getAvgDirectTermsForGene(), Matchers.is( 21.8 ) );
                    Assert.assertThat( dto.getAvgInferredTermsForGene(), Matchers.is( 143.4 ) );
                    Assert.assertThat( dto.getAvgInferredGenesForTerm(), Matchers.closeTo( 1.860, 0.001 ) );
                    Assert.assertThat( dto.getAvgMultifunctionality(),
                            Matchers.closeTo( 0.000053310363, 0.000000001 ) );
                    Assert.assertThat( dto.getAvgDirectSimilarity(), Matchers.closeTo( 1.0, 0.001 ) );
                    Assert.assertThat( dto.getAvgInferredSimilarity(), Matchers.closeTo( 1.0, 0.001 ) );
                }
            } else if ( dto.getSpecies() != 8 ) {
                fail( "Unknown Species" );
            }
        }
    }

    @Test
    public void testGetAggregatesSpeciesRestricted() {
        List<AggregateDTO> res = cacheDAO.getAggregates( new int[] { 7 } );
        Assert.assertThat( res.size(), Matchers.is( 4 ) );

        for ( AggregateDTO dto : res ) {
            Assert.assertThat( dto.getSpecies(), Matchers.is( 7 ) );
            Assert.assertThat( dto.getGeneCount(), Matchers.is( 10 ) );
        }
    }

    @Test
    public void testGetCurrentGenes() {
        List<AccessionDTO> res = cacheDAO.getAccessions( null );
        Assert.assertThat( res.size(), Matchers.is( 70 ) );

        Multiset<String> symbols = HashMultiset.create();

        for ( AccessionDTO dto : res ) {
            symbols.add( dto.getSymbol() );
        }

        // distinct symbols
        Assert.assertThat( symbols.elementSet().size(), Matchers.is( 20 ) );
        Assert.assertThat( symbols.count( "LONP1" ), Matchers.is( 8 ) );
        Assert.assertThat( symbols.count( "MEF2A" ), Matchers.is( 4 ) );
        Assert.assertThat( symbols.count( "MGME1" ), Matchers.is( 3 ) );
        Assert.assertThat( symbols.count( "MPV17" ), Matchers.is( 8 ) );
        Assert.assertThat( symbols.count( "MRPL17" ), Matchers.is( 2 ) );
        Assert.assertThat( symbols.count( "OPA1" ), Matchers.is( 5 ) );
        Assert.assertThat( symbols.count( "SLC25A33" ), Matchers.is( 1 ) );
        Assert.assertThat( symbols.count( "SLC25A36" ), Matchers.is( 6 ) );
        Assert.assertThat( symbols.count( "SLC25A4" ), Matchers.is( 2 ) );
        Assert.assertThat( symbols.count( "TYMP" ), Matchers.is( 2 ) );
        Assert.assertThat( symbols.count( "Mef2a" ), Matchers.is( 3 ) );
        Assert.assertThat( symbols.count( "Mgme1" ), Matchers.is( 1 ) );
        Assert.assertThat( symbols.count( "Mpv17" ), Matchers.is( 6 ) );
        Assert.assertThat( symbols.count( "Mrpl15" ), Matchers.is( 3 ) );
        Assert.assertThat( symbols.count( "Mrpl17" ), Matchers.is( 3 ) );
        Assert.assertThat( symbols.count( "Mrpl39" ), Matchers.is( 1 ) );
        Assert.assertThat( symbols.count( "Opa1" ), Matchers.is( 4 ) );
        Assert.assertThat( symbols.count( "Pif1" ), Matchers.is( 3 ) );
        Assert.assertThat( symbols.count( "Slc25a33" ), Matchers.is( 1 ) );
        Assert.assertThat( symbols.count( "Slc25a36" ), Matchers.is( 4 ) );

    }

    @Test
    public void testGetCurrentGenesSpeciesRestricted() {
        List<AccessionDTO> res = cacheDAO.getAccessions( new int[]{7} );
        Assert.assertThat( res.size(), Matchers.is( 41 ) );

        Multiset<String> symbols = HashMultiset.create();

        for ( AccessionDTO dto : res ) {
            symbols.add( dto.getSymbol() );
        }

        // distinct symbols
        Assert.assertThat( symbols.elementSet().size(), Matchers.is( 10 ) );
        Assert.assertThat( symbols.count( "LONP1" ), Matchers.is( 8 ) );
        Assert.assertThat( symbols.count( "MEF2A" ), Matchers.is( 4 ) );
        Assert.assertThat( symbols.count( "MGME1" ), Matchers.is( 3 ) );
        Assert.assertThat( symbols.count( "MPV17" ), Matchers.is( 8 ) );
        Assert.assertThat( symbols.count( "MRPL17" ), Matchers.is( 2 ) );
        Assert.assertThat( symbols.count( "OPA1" ), Matchers.is( 5 ) );
        Assert.assertThat( symbols.count( "SLC25A33" ), Matchers.is( 1 ) );
        Assert.assertThat( symbols.count( "SLC25A36" ), Matchers.is( 6 ) );
        Assert.assertThat( symbols.count( "SLC25A4" ), Matchers.is( 2 ) );
        Assert.assertThat( symbols.count( "TYMP" ), Matchers.is( 2 ) );
    }

    @Test
    public void testGetAccessions() {
        List<AccessionDTO> res = cacheDAO.getAccessions( null );
        Assert.assertThat( res.size(), Matchers.is( 70 ) );

        Multiset<Integer> genes = HashMultiset.create();
        Multiset<String> accessions = HashMultiset.create();
        Multiset<String> swissprot = HashMultiset.create();

        for ( AccessionDTO dto : res ) {
            genes.add( dto.getId() );
            accessions.add( dto.getAccession() );
            swissprot.add( dto.getSubset() );
        }

        // distinct genes
        Assert.assertThat( genes.elementSet().size(), Matchers.is( 70 ) );

        // Count distinct accessions
        Assert.assertThat( accessions.elementSet().size(), Matchers.is( 70 ) );

        // distinct sp
        Assert.assertThat( swissprot.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( swissprot.count( "Swiss-Prot" ), Matchers.is( 51 ) );
        Assert.assertThat( swissprot.count( "TrEMBL" ), Matchers.is( 19 ) );;
    }

    @Test
    public void testGetAccessionsSpeciesRestricted() {
        List<AccessionDTO> res = cacheDAO.getAccessions( new int[] { 7 } );
        Assert.assertThat( res.size(), Matchers.is( 41 ) );

        Multiset<Integer> genes = HashMultiset.create();
        Multiset<String> accessions = HashMultiset.create();
        Multiset<String> swissprot = HashMultiset.create();

        for ( AccessionDTO dto : res ) {
            genes.add( dto.getId() );
            accessions.add( dto.getAccession() );
            swissprot.add( dto.getSubset() );
        }

        // distinct genes
        Assert.assertThat( genes.elementSet().size(), Matchers.is( 41 ) );

        // Count distinct accessions
        Assert.assertThat( accessions.elementSet().size(), Matchers.is( 41 ) );

        // distinct sp
        Assert.assertThat( swissprot.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( swissprot.count( "Swiss-Prot" ), Matchers.is( 28 ) );
        Assert.assertThat( swissprot.count( "TrEMBL" ), Matchers.is( 13 ) );

    }

    @Test
    public void testGetGoTerms() {
        List<GOTermDTO> res = cacheDAO.getGoTerms(170); // FIXME: recreate for individual go editions
        Assert.assertThat( res.size(), Matchers.is( 794 ) );

        res.addAll( cacheDAO.getGoTerms(173) );
        Assert.assertThat( res.size(), Matchers.is( 1623 ) );

        res.addAll( cacheDAO.getGoTerms(174) );
        Assert.assertThat( res.size(), Matchers.is( 2454 ) );

        res.addAll( cacheDAO.getGoTerms(175) );
        Assert.assertThat( res.size(), Matchers.is( 3195 ) );

        Multiset<String> goIds = HashMultiset.create();

        // spot checks
        boolean found1 = false;
        boolean found2 = false;

        for ( GOTermDTO dto : res ) {
            found1 |= ( dto.getGoEdition() == 175 && dto.getGoId().equals( "GO:0031328" )
                    && dto.getAspect().equals( "BP" )
                    && dto.getName().equals( "positive regulation of cellular biosynthetic process" ) );

            found2 |= ( dto.getGoEdition() == 173 && dto.getGoId().equals( "GO:0000287" )
                    && dto.getAspect().equals( "MF" )
                    && dto.getName().equals( "magnesium ion binding" ) );
            goIds.add( dto.getGoId() );
        }

        if ( !found1 || !found2 ) {
            fail( "Failed spot check(s) for GO Term(s)" );
        }

        // Count distinct secs
        Assert.assertThat( goIds.elementSet().size(), Matchers.is( 831 ) );

    }

    @Test
    public void testGetAdjacencies() {
        List<AdjacencyDTO> res = cacheDAO.getAdjacencies(170);
        Assert.assertThat( res.size(), Matchers.is( 1414 ) );

        res.addAll( cacheDAO.getAdjacencies(173) );
        Assert.assertThat( res.size(), Matchers.is( 2917 ) );

        res.addAll( cacheDAO.getAdjacencies(174) );
        Assert.assertThat( res.size(), Matchers.is( 4396 ) );

        res.addAll( cacheDAO.getAdjacencies(175) );
        Assert.assertThat( res.size(), Matchers.is( 5728 ) );

        Multiset<Integer> editions = HashMultiset.create();
        Multiset<String> children = HashMultiset.create();
        Multiset<String> parents = HashMultiset.create();
        Multiset<String> relationships = HashMultiset.create();

        for ( AdjacencyDTO dto : res ) {
            editions.add( dto.getGoEdition() );
            children.add( dto.getChild() );
            parents.add( dto.getParent() );
            relationships.add( dto.getType() );
        }

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 4 ) );
        Assert.assertThat( editions.count( 170 ), Matchers.is( 1414 ) );
        Assert.assertThat( editions.count( 173 ), Matchers.is( 1503 ) );
        Assert.assertThat( editions.count( 174 ), Matchers.is( 1479 ) );
        Assert.assertThat( editions.count( 175 ), Matchers.is( 1332 ) );

        // Count distinct children
        Assert.assertThat( children.elementSet().size(), Matchers.is( 828 ) );

        // Count distinct parents
        Assert.assertThat( parents.elementSet().size(), Matchers.is( 690 ) );

        // distinct types
        Assert.assertThat( relationships.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( relationships.count( "IS_A" ), Matchers.is( 5277 ) );
        Assert.assertThat( relationships.count( "PART_OF" ), Matchers.is( 451 ) );
    }

    @Test
    public void testGetEvidence() {
        List<EvidenceDTO> res = cacheDAO.getEvidence();
        Assert.assertThat( res.size(), Matchers.is( 27 ) );

        Multiset<String> categories = HashMultiset.create();

        // spot checks
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;

        for ( EvidenceDTO dto : res ) {
            found1 |= ( dto.getEvidence().equals( "ISM" )
                    && dto.getDescription().equals( "Inferred from Sequence Model" )
                    && dto.getCategory().equals( "Computational" )
                    && dto.getCurated().equals( true ));

            found2 |= ( dto.getEvidence().equals( "NAS" )
                    && dto.getDescription().equals( "Non-traceable Author Statement" )
                    && dto.getCategory().equals( "Author" )
                    && dto.getCurated().equals( true ));

            found3 |= ( dto.getEvidence().equals( "IEA" )
                    && dto.getDescription().equals( "Inferred from Electronic Annotation" )
                    && dto.getCategory().equals( "Automatic" )
                    && dto.getCurated().equals( false ));
            categories.add( dto.getCategory() );
        }

        if ( !found1 ) {
            fail( "Failed spot check 1 for Evidence" );
        }

        if ( !found2 ) {
            fail( "Failed spot check 2 for Evidence" );
        }

        if ( !found3 ) {
            fail( "Failed spot check 3 for Evidence" );
        }

        // distinct categories
        Assert.assertThat( categories.elementSet().size(), Matchers.is( 6 ) );
        Assert.assertThat( categories.count( "Experimental" ), Matchers.is( 6 ) );
        Assert.assertThat( categories.count( "Computational" ), Matchers.is( 10 ) );
        Assert.assertThat( categories.count( "Author" ), Matchers.is( 2 ) );
        Assert.assertThat( categories.count( "Curatorial" ), Matchers.is( 3 ) );
        Assert.assertThat( categories.count( "High Throughput" ), Matchers.is( 5 ) );
        Assert.assertThat( categories.count( "Automatic" ), Matchers.is( 1 ) );

    }

}
