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

import java.sql.Date;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import ubc.pavlab.gotrack.BaseTest;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Gene.GeneBuilder;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.AnnotationDTO;
import ubc.pavlab.gotrack.model.dto.CategoryCountDTO;
import ubc.pavlab.gotrack.model.dto.DirectAnnotationCountDTO;
import ubc.pavlab.gotrack.model.dto.EnrichmentDTO;
import ubc.pavlab.gotrack.utilities.Tuples;
import ubc.pavlab.gotrack.utilities.Tuples.Tuple3;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
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
        GeneBuilder gb = new Gene.GeneBuilder( 5, "LONP1", s, Collections.<Accession> emptySet() );
        gh1 = gb.build();

        // Human Gene 2
        gb = new Gene.GeneBuilder( 10, "SLC25A33", s, Collections.<Accession> emptySet() );
        gh2 = gb.build();

        // Mouse
        s = new Species( 8, "Mouse", "", 10090, null );

        // Mouse Gene 1
        gb = new Gene.GeneBuilder( 12, "Mpv17", s, Collections.<Accession> emptySet() );
        gm1 = gb.build();

        // Mouse Gene 2
        gb = new Gene.GeneBuilder( 18, "Pif1", s, Collections.<Accession> emptySet() );
        gm2 = gb.build();

        // Alien
        s = new Species( 123456789, "E.T.", "", 123456789, null );

        // Alien Gene 1
        gb = new Gene.GeneBuilder( 123456789, "ufo", s, Collections.<Accession> emptySet() );
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
        List<AnnotationDTO> res = annotationDAO.track( gh1 );
        Assert.assertThat( res.size(), Matchers.is( 188 ) );

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
        Assert.assertThat( goIds.count( "GO:0005524" ), Matchers.is( 12 ) );
        Assert.assertThat( goIds.count( "GO:0010044" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051260" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070407" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0000002" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005737" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0016020" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051603" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0090296" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0001666" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005739" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0032042" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051880" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0003697" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005759" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0034599" ), Matchers.is( 12 ) );
        Assert.assertThat( goIds.count( "GO:0070182" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0003727" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0006515" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0042645" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070361" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0004176" ), Matchers.is( 16 ) );
        Assert.assertThat( goIds.count( "GO:0007005" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0043531" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070362" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0004252" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0007568" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0043565" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0070363" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0005515" ), Matchers.is( 8 ) );
        Assert.assertThat( goIds.count( "GO:0009725" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0051131" ), Matchers.is( 4 ) );
        Assert.assertThat( goIds.count( "GO:0070364" ), Matchers.is( 4 ) );

        // distinct editions
        Assert.assertThat( editions.elementSet().size(), Matchers.is( 4 ) );
        Assert.assertThat( editions.count( 144 ), Matchers.is( 47 ) );
        Assert.assertThat( editions.count( 145 ), Matchers.is( 47 ) );
        Assert.assertThat( editions.count( 146 ), Matchers.is( 47 ) );
        Assert.assertThat( editions.count( 147 ), Matchers.is( 47 ) );

        // distinct evidence
        Assert.assertThat( evidences.elementSet().size(), Matchers.is( 8 ) );
        Assert.assertThat( evidences.count( "NAS" ), Matchers.is( 8 ) );
        Assert.assertThat( evidences.count( "IEP" ), Matchers.is( 4 ) );
        Assert.assertThat( evidences.count( "IBA" ), Matchers.is( 12 ) );
        Assert.assertThat( evidences.count( "IDA" ), Matchers.is( 80 ) );
        Assert.assertThat( evidences.count( "IEA" ), Matchers.is( 56 ) );
        Assert.assertThat( evidences.count( "IPI" ), Matchers.is( 12 ) );
        Assert.assertThat( evidences.count( "IMP" ), Matchers.is( 12 ) );
        Assert.assertThat( evidences.count( "IC" ), Matchers.is( 4 ) );

        // distinct reference
        Assert.assertThat( references.elementSet().size(), Matchers.is( 17 ) );
        Assert.assertThat( references.count( "PMID:18063578" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:18174225" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:17420247" ), Matchers.is( 16 ) );
        Assert.assertThat( references.count( "PMID:9485316" ), Matchers.is( 16 ) );
        Assert.assertThat( references.count( "PMID:17418790" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "GO_REF:0000033" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "PMID:14739292" ), Matchers.is( 32 ) );
        Assert.assertThat( references.count( "PMID:8248235" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "GO_REF:0000002" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "GO_REF:0000020" ), Matchers.is( 28 ) );
        Assert.assertThat( references.count( "PMID:24520911" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "GO_REF:0000052" ), Matchers.is( 8 ) );
        Assert.assertThat( references.count( "GO_REF:0000040" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "PMID:12198491" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "PMID:15683722" ), Matchers.is( 4 ) );
        Assert.assertThat( references.count( "GO_REF:0000019" ), Matchers.is( 12 ) );
        Assert.assertThat( references.count( "PMID:19946888" ), Matchers.is( 4 ) );

        // distinct qualifiers
        Assert.assertThat( qualifiers.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( qualifiers.count( "NOT" ), Matchers.is( 8 ) );
        Assert.assertThat( qualifiers.count( "" ), Matchers.is( 180 ) );

    }

    @Test
    public void testTrackShallow() {
        List<AnnotationDTO> res = annotationDAO.track( gh1 );
        Assert.assertThat( res.size(), Matchers.is( 188 ) );

        res = annotationDAO.track( gh2 );
        Assert.assertThat( res.size(), Matchers.is( 96 ) );

        res = annotationDAO.track( gm1 );
        Assert.assertThat( res.size(), Matchers.is( 56 ) );

        res = annotationDAO.track( gm2 );
        Assert.assertThat( res.size(), Matchers.is( 80 ) );

        res = annotationDAO.track( gm2 );
        Assert.assertThat( res.size(), Matchers.is( 80 ) );

        res = annotationDAO.track( gfake );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

    @Test
    public void testEnrichDeep() {
        List<EnrichmentDTO> res = annotationDAO.enrich( Sets.newHashSet( gh1, gh2 ) );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        Multiset<String> goIds = HashMultiset.create();
        Multiset<Integer> editions = HashMultiset.create();
        Multiset<Integer> geneIds = HashMultiset.create();

        for ( EnrichmentDTO dto : res ) {
            goIds.add( dto.getGoId() );
            editions.add( dto.getEdition() );
            geneIds.add( dto.getGeneId() );
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
        Assert.assertThat( geneIds.elementSet().size(), Matchers.is( 2 ) );
        Assert.assertThat( geneIds.count( 5 ), Matchers.is( 132 ) );
        Assert.assertThat( geneIds.count( 10 ), Matchers.is( 80 ) );
    }

    @Test
    public void testEnrichShallow() {
        List<EnrichmentDTO> res = annotationDAO.enrich( Sets.newHashSet( gh1, gh2 ) );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        // Redundancy
        res = annotationDAO.enrich( Sets.newHashSet( gh1, gh2, gh1, gh1, gh2 ) );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        // bad gene
        res = annotationDAO.enrich( Sets.newHashSet( gh1, gh2, gfake ) );
        Assert.assertThat( res.size(), Matchers.is( 212 ) );

        // empty gene
        res = annotationDAO.enrich( Collections.<Gene> emptySet() );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        // null gene
        res = annotationDAO.enrich( null );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.enrich( Sets.newHashSet( gh1 ) );
        Assert.assertThat( res.size(), Matchers.is( 132 ) );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCategoryCounts() {
        List<CategoryCountDTO> res = annotationDAO.categoryCounts( "GO:0007005" );

        List<Tuple3<Date, String, Integer>> records = Lists.newArrayList();

        for ( CategoryCountDTO dto : res ) {
            records.add( Tuples.tuple3( dto.getDate(), dto.getCategory(), dto.getCount() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Author", 2 ),
                        Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Computational", 5 ),
                        Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Experimental", 6 ),
                        Tuples.tuple3( Date.valueOf( "2015-05-26" ), "Author", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-05-26" ), "Experimental", 5 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-02" ), "Author", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-02" ), "Computational", 5 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-02" ), "Experimental", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Author", 2 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Computational", 5 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Experimental", 6 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Author", 2 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Computational", 5 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Experimental", 6 ) ) );

        res = annotationDAO.categoryCounts( "GO:0008150" ); // BP
        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.categoryCounts( "GO:0003674" ); //MF

        records = Lists.newArrayList();

        for ( CategoryCountDTO dto : res ) {
            records.add( Tuples.tuple3( dto.getDate(), dto.getCategory(), dto.getCount() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( Date.valueOf( "2015-04-27" ), "Curatorial", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-05-26" ), "Curatorial", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-06-22" ), "Curatorial", 1 ),
                        Tuples.tuple3( Date.valueOf( "2015-07-20" ), "Curatorial", 1 ) ) );

        res = annotationDAO.categoryCounts( "" );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.categoryCounts( null );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDirectGeneCounts() {
        List<DirectAnnotationCountDTO> res = annotationDAO.directGeneCounts( "GO:0007005" );

        List<Tuple3<Integer, Integer, Integer>> records = Lists.newArrayList();

        for ( DirectAnnotationCountDTO dto : res ) {
            records.add( Tuples.tuple3( dto.getSpecies(), dto.getEdition(), dto.getCount() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( 7, 144, 4 ),
                        Tuples.tuple3( 7, 145, 4 ),
                        Tuples.tuple3( 7, 146, 4 ),
                        Tuples.tuple3( 7, 147, 4 ),
                        Tuples.tuple3( 8, 130, 3 ),
                        Tuples.tuple3( 8, 131, 3 ),
                        Tuples.tuple3( 8, 132, 3 ),
                        Tuples.tuple3( 8, 133, 3 ) ) );

        res = annotationDAO.directGeneCounts( "GO:0007568" );

        records = Lists.newArrayList();

        for ( DirectAnnotationCountDTO dto : res ) {
            records.add( Tuples.tuple3( dto.getSpecies(), dto.getEdition(), dto.getCount() ) );
        }

        Assert.assertThat( records,
                Matchers.containsInAnyOrder( Tuples.tuple3( 7, 144, 1 ),
                        Tuples.tuple3( 7, 145, 1 ),
                        Tuples.tuple3( 7, 146, 1 ),
                        Tuples.tuple3( 7, 147, 1 ) ) );

        res = annotationDAO.directGeneCounts( "" );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );

        res = annotationDAO.directGeneCounts( null );

        Assert.assertThat( res.size(), Matchers.is( 0 ) );
    }

}
