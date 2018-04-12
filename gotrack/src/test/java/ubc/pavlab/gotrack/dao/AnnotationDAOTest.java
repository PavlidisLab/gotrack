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
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.*;
import org.mockito.Mockito;
import ubc.pavlab.gotrack.BaseTest;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Gene.GeneBuilder;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Tuples;
import ubc.pavlab.gotrack.utilities.Tuples.Tuple3;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author mjacobson
 */
public class AnnotationDAOTest extends BaseTest {

    private static final Logger log = Logger.getLogger( AnnotationDAOTest.class );
    private static AnnotationDAO annotationDAO;

    private Gene gh1;
    private Gene gh2;

    private Gene gm1;
    private Gene gm2;

    private Gene gfake;

    @BeforeClass
    public static void classSetup() {
        annotationDAO = daoFactory.getAnnotationDAO();
    }

    @Before
    public void setup() {

        // Human
        Species s = new Species( 7, "Human", "", 9606, null );

        // Human Gene 1
        AccessionDTO accMock = Mockito.mock(AccessionDTO.class);
        Mockito.when( accMock.getId() ).thenReturn( -1 ); // Not being tested, but needs to be initialized
        Mockito.when( accMock.getSubset() ).thenReturn( "" ); // Not being tested, but needs to be initialized
        Mockito.when( accMock.getAccession() ).thenReturn( "P36776" );
        GeneBuilder gb = new Gene.GeneBuilder( 242, "LONP1", "", s, new Accession( accMock ) ); //P36776
        gh1 = gb.build();

        // Human Gene 2
        Mockito.when( accMock.getAccession() ).thenReturn( "Q9BSK2" );
        gb = new Gene.GeneBuilder( 249, "SLC25A33", "", s, new Accession( accMock ) ); //Q9BSK2
        gh2 = gb.build();

        // Mouse
        s = new Species( 8, "Mouse", "", 10090, null );

        // Mouse Gene 1
        Mockito.when( accMock.getAccession() ).thenReturn( "P19258" );
        gb = new Gene.GeneBuilder( 269, "Mpv17", "", s, new Accession( accMock ) ); //P19258
        gm1 = gb.build();

        // Mouse Gene 2
        Mockito.when( accMock.getAccession() ).thenReturn( "Q80SX8" );
        gb = new Gene.GeneBuilder( 273, "Pif1", "", s, new Accession( accMock ) ); //Q80SX8
        gm2 = gb.build();

        // Alien
        s = new Species( 123456789, "E.T.", "", 123456789, null );

        // Alien Gene 1
        Mockito.when( accMock.getAccession() ).thenReturn( "XXXXXX" );
        gb = new Gene.GeneBuilder( 123456789, "ufo", "", s, new Accession( accMock ) );
        gfake = gb.build();

    }

    @After
    public void after() {
        gh1 = null;
        gh2 = null;
        gm1 = null;
        gm2 = null;
        gfake = null;
    }

    @Test
    public void testTrackDeep() {
        List<AnnotationDTO> res = annotationDAO.fullAnnotationRangeEditions( gh1, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 152 ) );

        Multiset<String> goIds = HashMultiset.create();
        Multiset<Integer> editions = HashMultiset.create();
        Multiset<String> evidences = HashMultiset.create();
        Multiset<String> references = HashMultiset.create();
        Multiset<String> qualifiers = HashMultiset.create();

        for ( AnnotationDTO dto : res ) {
            goIds.add( dto.getGoId() );
            editions.add( dto.getEdition() );
            evidences.add( dto.getEvidence() );
            references.add( dto.getReference() );
            qualifiers.add( dto.getQualifier() );
        }

        // distinct go ids
        Assert.assertThat( goIds.elementSet().size(), Matchers.is( 33 ) );
        Assert.assertThat( goIds.count( "GO:0000002" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0001666" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0003697" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0003727" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0004176" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0004252" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005515" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0005524" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005737" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005739" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0005759" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0006515" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0007005" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0007568" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0009725" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0010044" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0016020" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0032042" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0034599" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0042645" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0043531" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0043565" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051131" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051260" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051603" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0051880" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070182" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070361" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070362" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070363" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070364" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070407" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0090296" ), Matchers.is( 4 ) );

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 4 ) );
        Assert.assertThat( editions.count( 144 ), Matchers.is( 38 ) );
        Assert.assertThat( editions.count( 145 ), Matchers.is( 38 ) );
        Assert.assertThat( editions.count( 146 ), Matchers.is( 38 ) );
        Assert.assertThat( editions.count( 147 ), Matchers.is( 38 ) );

        // distinct evidence
        Assert.assertThat( evidences.elementSet().size(), Matchers.is( 8 ) );
        Assert.assertThat( evidences.count( "IBA" ), Matchers.is( 12 ) );
        Assert.assertThat( evidences.count( "IC" ), Matchers.is( 4 ) );
        Assert.assertThat( evidences.count( "IDA" ), Matchers.is( 80 ) );
        Assert.assertThat( evidences.count( "IEA" ), Matchers.is( 20 ) );
        Assert.assertThat( evidences.count( "IEP" ), Matchers.is( 4 ) );
        Assert.assertThat( evidences.count( "IMP" ), Matchers.is( 12 ) );
        Assert.assertThat( evidences.count( "IPI" ), Matchers.is( 12 ) );
        Assert.assertThat( evidences.count( "NAS" ), Matchers.is( 8 ) );

        // distinct reference
        Assert.assertThat( references.elementSet().size(), Matchers.is( 15 ) );
        Assert.assertThat( references.count( "GO_REF:0000019" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "GO_REF:0000020" ), Matchers.is( 8 ) );
        Assert.assertThat( references.count( "GO_REF:0000033" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "GO_REF:0000052" ), Matchers.is( 8 ) );
        Assert.assertThat( references.count( "PMID:12198491" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "PMID:14739292" ), Matchers.is( 32 ) );
        Assert.assertThat( references.count( "PMID:15683722" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:17418790" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:17420247" ), Matchers.is( 16 ) );
        Assert.assertThat( references.count( "PMID:18063578" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:18174225" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:19946888" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:24520911" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:8248235" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "PMID:9485316" ), Matchers.is( 16 ) );

        // distinct qualifiers
        Assert.assertThat( qualifiers.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( qualifiers.count( "NOT" ), Matchers.is( 8 ) );
        Assert.assertThat( qualifiers.count( "" ), Matchers.is( 144 ) );

    }

    @Test
    public void testTrackShallow() {
        List<AnnotationDTO> res = annotationDAO.fullAnnotationRangeEditions( gh1, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 152 ) );

        res = annotationDAO.fullAnnotationRangeEditions( gh2, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 96 ) );

        res = annotationDAO.fullAnnotationRangeEditions( gm1, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 52 ) );

        res = annotationDAO.fullAnnotationRangeEditions( gm2, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 72 ) );

        res = annotationDAO.fullAnnotationRangeEditions( gfake, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

    @Test
    public void testEnrichDeep() {
        List<EnrichmentDTO> res = annotationDAO.simpleAnnotationRangeEditions( Sets.newHashSet( gh1, gh2 ), 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        Multiset<String> goIds = HashMultiset.create();
        Multiset<Integer> editions = HashMultiset.create();
        Multiset<String> accessions = HashMultiset.create();

        for ( EnrichmentDTO dto : res ) {
            goIds.add( dto.getGoId() );
            editions.add( dto.getEdition() );
            accessions.add( dto.getAccession() );
        }

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 4 ) );
        Assert.assertThat( editions.count( 144 ), Matchers.is( 53 ) );
        Assert.assertThat( editions.count( 145 ), Matchers.is( 53 ) );
        Assert.assertThat( editions.count( 146 ), Matchers.is( 53 ) );
        Assert.assertThat( editions.count( 147 ), Matchers.is( 53 ) );

        // distinct go ids
        Assert.assertThat( goIds.elementSet().size(), Matchers.is( 51 ) );
        Assert.assertThat( goIds.count( "GO:0000002" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0001666" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0002082" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0003697" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0003727" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0004176" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0004252" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005515" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005524" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005737" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005739" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005743" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005759" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0006264" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0006390" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0006515" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0006864" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0007005" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0007568" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0008284" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0009725" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0010044" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0015218" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0016020" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0016021" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0030307" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0031930" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0031966" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0032042" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0032869" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0034551" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0034599" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0042645" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0043531" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0043565" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051131" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051260" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051603" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051880" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051881" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070182" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070361" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070362" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070363" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070364" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070407" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0071156" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0090296" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:1903426" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:1990314" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:1990519" ), Matchers.is( 4 ) );

        // distinct gene ids
        Assert.assertThat( accessions.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( accessions.count( gh1.getAccession().getAccession() ), Matchers.is( 132 ) );
        Assert.assertThat( accessions.count( gh2.getAccession().getAccession() ), Matchers.is( 80 ) );
    }

    @Test
    public void testEnrichShallow() {
        List<EnrichmentDTO> res = annotationDAO.simpleAnnotationRangeEditions( Sets.newHashSet( gh1, gh2 ), 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        // Redundancy
        res = annotationDAO.simpleAnnotationRangeEditions( Sets.newHashSet( gh1, gh2, gh1, gh1, gh2 ), 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        // bad gene
        res = annotationDAO.simpleAnnotationRangeEditions( Sets.newLinkedHashSet( Lists.newArrayList( gh1, gh2, gfake ) ), 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        // empty gene
        res = annotationDAO.simpleAnnotationRangeEditions( Collections.<Gene>emptySet(), 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        // null gene
        res = annotationDAO.simpleAnnotationRangeEditions( null, 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.simpleAnnotationRangeEditions( Sets.newHashSet( gh1 ), 0, 200 );
        Assert.assertThat( res.size(), Matchers.is( 132 ) );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCategoryCounts() {
        List<CategoryCountDTO> res = annotationDAO.categoryCountsRangeDates( "GO:0007005",
                java.sql.Date.valueOf( "2015-01-01" ),  java.sql.Date.valueOf( "2016-01-01" ) );

        List<Tuple3<Date, String, Integer>> records = Lists.newArrayList();

        for ( CategoryCountDTO dto : res ) {
            records.add( Tuples.tuple3( dto.getDate(), dto.getCategory(), dto.getCount() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Author", 2 ),
                        Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Computational", 11 ),
                        Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Experimental", 9 ),
                        Tuples.tuple3( Date.valueOf( "2015-05-26" ), "Author", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-05-26" ), "Experimental", 5 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-02" ), "Author", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-02" ), "Computational", 11 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-02" ), "Experimental", 4 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Author", 2 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Computational", 11 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Experimental", 9 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Author", 2 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Computational", 11 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Experimental", 9 ) ) );

        res = annotationDAO.categoryCountsRangeDates( "GO:0008150",
                java.sql.Date.valueOf( "2015-01-01" ),  java.sql.Date.valueOf( "2016-01-01" ) ); // BP
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.categoryCountsRangeDates( "GO:0003674",
                java.sql.Date.valueOf( "2015-01-01" ),  java.sql.Date.valueOf( "2016-01-01" ) ); //MF

        records = Lists.newArrayList();

        for ( CategoryCountDTO dto : res ) {
            records.add( Tuples.tuple3( dto.getDate(), dto.getCategory(), dto.getCount() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Curatorial", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-05-26" ), "Curatorial", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Curatorial", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Curatorial", 1 ) ) );

        res = annotationDAO.categoryCountsRangeDates( "",
                java.sql.Date.valueOf( "2015-01-01" ),  java.sql.Date.valueOf( "2016-01-01" ) );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.categoryCountsRangeDates( null,
                java.sql.Date.valueOf( "2015-01-01" ),  java.sql.Date.valueOf( "2016-01-01" ) );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

    @Test
    public void testSingleSpeciesCategoryCounts() {
        Species human = new Species( 7, "Human", "", 9606, null );
        Species mouse = new Species( 8, "Mouse", "", 10090, null );

        List<Tuple3<Integer, String, Integer>> records = annotationDAO.categoryCountsSingleSpeciesRangeEditions( "GO:0007005", human,
                144, 147 ).stream()
                .map( dto -> Tuples.tuple3( dto.getEdition(), dto.getCategory(), dto.getCount() ) )
                .collect( Collectors.toList() );

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3(144, "Author", 1 ),
                        Tuples.tuple3( 144, "Experimental", 5 ),
                        Tuples.tuple3( 145, "Author", 1 ),
                        Tuples.tuple3( 145, "Experimental", 5 ),
                        Tuples.tuple3( 146, "Author", 1 ),
                        Tuples.tuple3( 146, "Experimental", 5 ),
                        Tuples.tuple3( 147, "Author", 1 ),
                        Tuples.tuple3( 147, "Experimental", 5 ) ) );

        records = annotationDAO.categoryCountsSingleSpeciesRangeEditions( "GO:0007005", mouse,
                130, 133 ).stream()
                .map( dto -> Tuples.tuple3( dto.getEdition(), dto.getCategory(), dto.getCount() ) )
                .collect( Collectors.toList() );

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( 130, "Author", 1 ),
                        Tuples.tuple3( 130, "Computational", 11 ),
                        Tuples.tuple3( 130, "Experimental", 4 ),
                        Tuples.tuple3( 131, "Author", 1 ),
                        Tuples.tuple3( 131, "Computational", 11 ),
                        Tuples.tuple3( 131, "Experimental", 4 ),
                        Tuples.tuple3( 132, "Author", 1 ),
                        Tuples.tuple3( 132, "Computational", 11 ),
                        Tuples.tuple3( 132, "Experimental", 4 ),
                        Tuples.tuple3( 133, "Author", 1 ),
                        Tuples.tuple3( 133, "Computational", 11 ),
                        Tuples.tuple3( 133, "Experimental", 4) ) );

        Assert.assertThat( annotationDAO.categoryCountsSingleSpeciesRangeEditions( "GO:0008150", human,
                144,  145 ).size(), Matchers.is( 0 ) );

        records = annotationDAO.categoryCountsSingleSpeciesRangeEditions( "GO:0003674", human,
                144, 145 ).stream()
                .map( dto -> Tuples.tuple3( dto.getEdition(), dto.getCategory(), dto.getCount() ) )
                .collect( Collectors.toList() );

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( 144, "Curatorial", 1 ),
                        Tuples.tuple3( 145, "Curatorial", 1 ) ) );

        Assert.assertThat( annotationDAO.categoryCountsSingleSpeciesRangeEditions( "", human,
                144,  147 ).size(), Matchers.is( 0 ) );

        Assert.assertThat( annotationDAO.categoryCountsSingleSpeciesRangeEditions( null, human,
                144, 147 ).size(), Matchers.is( 0 ) );

    }

    @Test
    public void testDirectGeneCounts() {
        Species human = new Species( 7, "Human", "", 9606, null );
        Species mouse = new Species( 8, "Mouse", "", 10090, null );

        Edition edMock1 = Mockito.mock(Edition.class);
        Mockito.when( edMock1.getEdition() ).thenReturn( 145 );
        Mockito.when( edMock1.getSpecies() ).thenReturn( human );
        Edition edMock2 = Mockito.mock(Edition.class);
        Mockito.when( edMock2.getEdition() ).thenReturn( 131 );
        Mockito.when( edMock2.getSpecies() ).thenReturn( mouse );

        List<String> res = annotationDAO.directGenesSingleEdition( new GeneOntologyTerm( "GO:0007005" ), edMock1 );

        Assert.assertThat( res.size(), Matchers.is( 4 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( "O60313", "P36776", "Q96CQ1", "Q9BSK2" ) );

        res = annotationDAO.directGenesSingleEdition( new GeneOntologyTerm( "GO:0007005" ), edMock2 );

        Assert.assertThat( res.size(), Matchers.is( 9 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder(
                "D6RH79",
                "E0CXD1",
                "F6R114",
                "F6U775",
                "H7BX01",
                "P58281",
                "Q3TZX3",
                "Q8BZJ4",
                "Q922G0"
        ) );

        res = annotationDAO.directGenesSingleEdition( new GeneOntologyTerm( "GO:0007568" ), edMock1 );

        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( "P36776" ) );

        res = annotationDAO.directGenesSingleEdition( null, edMock1 );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

}
