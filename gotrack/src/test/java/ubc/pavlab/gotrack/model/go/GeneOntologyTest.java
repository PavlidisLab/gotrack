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

package ubc.pavlab.gotrack.model.go;

import java.sql.Date;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ubc.pavlab.gotrack.model.Annotation;
import ubc.pavlab.gotrack.model.Evidence;
import ubc.pavlab.gotrack.model.GOEdition;
import ubc.pavlab.gotrack.model.dto.EvidenceDTO;
import ubc.pavlab.gotrack.model.dto.GOEditionDTO;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntologyTest {
    private static final Logger log = Logger.getLogger( GeneOntologyTest.class );

    private GeneOntology go;
    private GOEdition goEdition;
    private Collection<GeneOntologyTerm> allTerms;
    private GeneOntologyTerm go0;
    private GeneOntologyTerm go1;
    private GeneOntologyTerm go2;
    private GeneOntologyTerm go3;
    private GeneOntologyTerm go4;
    private GeneOntologyTerm go5;
    private GeneOntologyTerm go6;
    private GeneOntologyTerm go7;
    private GeneOntologyTerm go8;
    private GeneOntologyTerm go9;

    private Map<GeneOntologyTerm, Set<Annotation>> annotationMap;
    private Evidence ev0;
    private Evidence ev1;
    private Evidence evOverlap;
    private Evidence evNot;

    private Set<Annotation> go7Annotations;
    private Set<Annotation> go9Annotations;

    @BeforeClass
    public static void classSetup() {
    }

    @Before
    public void setup() {
        goEdition = new GOEdition( new GOEditionDTO( 1, Date.valueOf( "2016-03-30" ) ) );
        go = new GeneOntology( goEdition );

        go0 = new GeneOntologyTerm( "GO:0008150" );//, "biological_process", "BP" );
        go1 = new GeneOntologyTerm( "GO:0071840" );//, "cellular component organization or biogenesis", "BP" );
        go2 = new GeneOntologyTerm( "GO:0009987" );//, "cellular process", "BP" );
        go3 = new GeneOntologyTerm( "GO:0044699" );//, "single-organism process", "BP" );
        go4 = new GeneOntologyTerm( "GO:0016043" );//, "cellular component organization", "BP" );
        go5 = new GeneOntologyTerm( "GO:0006996" );//, "organelle organization", "BP" );
        go6 = new GeneOntologyTerm( "GO:0044763" );//, "single-organism cellular process", "BP" );
        go7 = new GeneOntologyTerm( "GO:1902589" );//, "single-organism organelle organization", "BP" );

        go8 = new GeneOntologyTerm( "GO:0044085" );//, "cellular component biogenesis", "BP" );
        go9 = new GeneOntologyTerm( "GO:0022607" );//, "cellular component assembly", "BP" );

        allTerms = Sets.newHashSet( go0, go1, go2, go3, go4, go5, go6, go7, go8, go9 );

        // Create the ancestor chart for (GO:1902589 single-organism organelle organization) and (GO:0022607 cellular component assembly)
        // at the time of writing

        //        0
        //      / | \
        //     1  2  3
        //    /\ / \ /
        //   8  4   |
        //   ! /|   |
        //   !/ 5   6
        //   9   \ /
        //        7

        go.addTerm( go0 );
        go.addTerm( go1 );
        go.addTerm( go2 );
        go.addTerm( go3 );
        go.addTerm( go4 );
        go.addTerm( go5 );
        go.addTerm( go6 );
        go.addTerm( go7 );
        go.addTerm( go8 );
        go.addTerm( go9 );

        // first leaf

        go.addRelationship( go1.getGoId(), go0.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go2.getGoId(), go0.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go3.getGoId(), go0.getGoId(), RelationshipType.IS_A );

        go.addRelationship( go4.getGoId(), go1.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go4.getGoId(), go2.getGoId(), RelationshipType.IS_A );

        go.addRelationship( go5.getGoId(), go4.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go6.getGoId(), go2.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go6.getGoId(), go3.getGoId(), RelationshipType.IS_A );

        go.addRelationship( go7.getGoId(), go6.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go7.getGoId(), go5.getGoId(), RelationshipType.IS_A );

        // second leaf

        go.addRelationship( go8.getGoId(), go1.getGoId(), RelationshipType.IS_A );

        go.addRelationship( go9.getGoId(), go4.getGoId(), RelationshipType.IS_A );
        go.addRelationship( go9.getGoId(), go8.getGoId(), RelationshipType.PART_OF );

        go.freeze();

        // Create some annotations for the leafs
        ev0 = new Evidence( new EvidenceDTO( 1, "EXP", "Inferred from Experiment", "Experimental" ) );
        ev1 = new Evidence(
                new EvidenceDTO( 7, "ISS", "Inferred from Sequence or structural Similarity", "Computational" ) );
        evOverlap = new Evidence( new EvidenceDTO( 17, "TAS", "Traceable Author Statement", "Author" ) );

        evNot = new Evidence( new EvidenceDTO( 5, "IGI", "Inferred from Genetic Interaction", "Experimental" ) );

        annotationMap = Maps.newHashMap();

        go9Annotations = Sets.newHashSet( new Annotation( "", ev0, "9FakeRef1" ),
                new Annotation( "", ev1, "9FakeRef2" ), new Annotation( "", evOverlap, "OverlapRef" ),
                new Annotation( "NOT", evNot, "9FakeRef4" ) );

        annotationMap.put( go9, go9Annotations );

        go7Annotations = Sets.newHashSet( new Annotation( "", ev0, "7FakeRef1" ),
                new Annotation( "", ev1, "7FakeRef2" ), new Annotation( "", evOverlap, "OverlapRef" ),
                new Annotation( "NOT", evNot, "7FakeRef4" ) );

        annotationMap.put( go7, go7Annotations );

    }

    @After
    public void after() {
        go = null;
    }

    @Test
    public void testAddDuplicateTerm() {
        try {
            go.addTerm( go0 );
            Assert.fail( "Failed to throw error on adding dupliate term to ontology" );
        } catch ( IllegalArgumentException e ) {
            // Expected
        }
    }

    @Test
    public void testAddDuplicateRelationship() {
        try {
            go.addRelationship( go7.getGoId(), go5.getGoId(), RelationshipType.IS_A );
            Assert.fail( "Failed to throw error on adding dupliate relationship to ontology" );
        } catch ( IllegalArgumentException e ) {
            // Expected
        }
    }

    // getTerm

    @Test
    public void testGetTermStringExists() {
        GeneOntologyTerm res = go.getTerm( go7.getGoId() );
        Assert.assertThat( res, Matchers.notNullValue() );
        Assert.assertThat( res, Matchers.is( go7 ) );
    }

    @Test
    public void testGetTermStringNotExists() {
        GeneOntologyTerm res = go.getTerm( "GO:1234567" );
        Assert.assertThat( res, Matchers.nullValue() );
    }

    @Test
    public void testGetTermStringMalformed() {
        GeneOntologyTerm res = go.getTerm( "qwerty1234567" );
        Assert.assertThat( res, Matchers.nullValue() );
    }

    @Test
    public void testGetTermStringMalformedLength() {
        GeneOntologyTerm res = go.getTerm( "GO:123456" );
        Assert.assertThat( res, Matchers.nullValue() );
    }

    @Test
    public void testGetTermIntegerExists() {

        GeneOntologyTerm res = go.getTerm( 1902589 );
        Assert.assertThat( res, Matchers.notNullValue() );
        Assert.assertThat( res, Matchers.is( go7 ) );
    }

    @Test
    public void testGetTermIntegerNotExists() {

        GeneOntologyTerm res = go.getTerm( 1234567 );
        Assert.assertThat( res, Matchers.nullValue() );
    }

    // Size

    @Test
    public void testSize() {
        Assert.assertThat( go.size(), Matchers.is( allTerms.size() ) );
    }

    // propagate

    @Test
    public void testPropagate() {
        // Default propagate through part_of
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go9 ) );
        Assert.assertThat( res.size(), Matchers.is( 6 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go9, go8, go4, go1, go2, go0 ) );
    }

    @Test
    public void testPropagatePartOf() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go9 ), true );
        Assert.assertThat( res.size(), Matchers.is( 6 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go9, go8, go4, go1, go2, go0 ) );
    }

    @Test
    public void testPropagateNoPartOf() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go9 ), false );
        Assert.assertThat( res.size(), Matchers.is( 5 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go9, go4, go1, go2, go0 ) );
    }

    @Test
    public void testPropagateEmpty() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.<GeneOntologyTerm> newHashSet() );
        Assert.assertThat( res, Matchers.notNullValue() );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );
    }

    @Test
    public void testPropagateRoot() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go0 ) );
        Assert.assertThat( res.size(), Matchers.is( 1 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go0 ) );
    }

    @Test
    public void testPropagateMultiple() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go9, go6 ) );
        Assert.assertThat( res.size(), Matchers.is( 8 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go9, go8, go4, go1, go2, go0, go6, go3 ) );
    }

    @Test
    public void testPropagateMultipleNoPartOf() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go9, go6 ), false );
        Assert.assertThat( res.size(), Matchers.is( 7 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go9, go4, go1, go2, go0, go6, go3 ) );
    }

    @Test
    public void testPropagateMultipleNoPartOf2() {
        Set<GeneOntologyTerm> res = go.propagate( Sets.newHashSet( go9, go8 ), false );
        Assert.assertThat( res.size(), Matchers.is( 6 ) );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go9, go8, go4, go1, go2, go0 ) );
    }

    @Test
    public void testPropagateMultipleAll() {
        Set<GeneOntologyTerm> res = go.propagate( allTerms );
        Assert.assertThat( res.size(), Matchers.is( allTerms.size() ) );
        Assert.assertThat( res, Matchers.is( allTerms ) );
    }

    // propagateAnnotations

    @Test
    public void testPropagateAnnotationsOverview() {
        // Default propagate through part_of
        Map<GeneOntologyTerm, Set<Annotation>> res = go.propagateAnnotations( annotationMap );
        Assert.assertThat( res.size(), Matchers.is( allTerms.size() ) );
        Assert.assertThat( res.keySet(),
                Matchers.containsInAnyOrder( allTerms.toArray( new GeneOntologyTerm[allTerms.size()] ) ) );

    }

    @Test
    public void testPropagateAnnotationsBasic() {
        Map<GeneOntologyTerm, Set<Annotation>> res = go.propagateAnnotations( annotationMap );
        Assert.assertThat( res.get( go9 ), Matchers.equalTo( go9Annotations ) );
        Assert.assertThat( res.get( go7 ), Matchers.equalTo( go7Annotations ) );
    }

    @Test
    public void testPropagateAnnotationsParentsNoOverlap() {
        Map<GeneOntologyTerm, Set<Annotation>> res = go.propagateAnnotations( annotationMap );
        Assert.assertThat( res.get( go3 ), Matchers.equalTo( go7Annotations ) );
        Assert.assertThat( res.get( go8 ), Matchers.equalTo( go9Annotations ) );
    }

    @Test
    public void testPropagateAnnotationsParentsNoOverlapNoPartOf() {
        Map<GeneOntologyTerm, Set<Annotation>> res = go.propagateAnnotations( annotationMap, false );
        Assert.assertThat( res.get( go8 ), Matchers.nullValue() );
    }

    @Test
    public void testPropagateAnnotationsParentsOverlap() {
        Map<GeneOntologyTerm, Set<Annotation>> res = go.propagateAnnotations( annotationMap );
        Assert.assertThat( res.get( go2 ).size(), Matchers.is( Sets.union( go7Annotations, go9Annotations ).size() ) );
        Assert.assertThat( res.get( go2 ),
                Matchers.hasItems( go7Annotations.toArray( new Annotation[go7Annotations.size()] ) ) );

        Assert.assertThat( res.get( go2 ),
                Matchers.hasItems( go9Annotations.toArray( new Annotation[go9Annotations.size()] ) ) );
    }

    // getAncestors

    @Test
    public void testGetAncestorsString() {
        Set<GeneOntologyTerm> res = go.getAncestors( go9.getGoId(), true,
                Maps.<GeneOntologyTerm, Set<GeneOntologyTerm>> newHashMap() );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go8, go4, go1, go2, go0 ) );
    }

    @Test
    public void testGetAncestorsInteger() {
        Set<GeneOntologyTerm> res = go.getAncestors( 22607, true,
                Maps.<GeneOntologyTerm, Set<GeneOntologyTerm>> newHashMap() );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go8, go4, go1, go2, go0 ) );
    }

    @Test
    public void testGetAncestors() {
        Set<GeneOntologyTerm> res = go.getAncestors( go7, true,
                Maps.<GeneOntologyTerm, Set<GeneOntologyTerm>> newHashMap() );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go5, go6, go4, go1, go2, go3, go0 ) );
    }

    @Test
    public void testGetAncestorsPartOf() {
        Set<GeneOntologyTerm> res = go.getAncestors( go9, true,
                Maps.<GeneOntologyTerm, Set<GeneOntologyTerm>> newHashMap() );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go8, go4, go1, go2, go0 ) );
    }

    @Test
    public void testGetAncestorsNoPartOf() {
        Set<GeneOntologyTerm> res = go.getAncestors( go9, false,
                Maps.<GeneOntologyTerm, Set<GeneOntologyTerm>> newHashMap() );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go4, go1, go2, go0 ) );
    }

    @Test
    public void testGetAncestorsCacheless() {
        Set<GeneOntologyTerm> res = go.getAncestors( go9, false, null );
        Assert.assertThat( res, Matchers.containsInAnyOrder( go4, go1, go2, go0 ) );
    }

    @Test
    public void testGetAncestorsNull() {
        GeneOntologyTerm g = null;
        try {
            Set<GeneOntologyTerm> res = go.getAncestors( g, false, null );
            Assert.fail( "Should error with null term" );
        } catch ( NullPointerException e ) {
            // Expected
        }

    }

    @Test
    public void testGetAncestorsRoot() {
        Set<GeneOntologyTerm> res = go.getAncestors( go0, false, null );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );
    }

    // getParents

    @Test
    public void testGetParentsString() {
        Set<Relation> res = go.getParents( go9.getGoId(), true );
        Assert.assertThat( res, Matchers.containsInAnyOrder( new Relation( go8, RelationshipType.PART_OF ),
                new Relation( go4, RelationshipType.IS_A ) ) );
    }

    @Test
    public void testGetParentsInteger() {
        Set<Relation> res = go.getParents( 22607, true );
        Assert.assertThat( res, Matchers.containsInAnyOrder( new Relation( go8, RelationshipType.PART_OF ),
                new Relation( go4, RelationshipType.IS_A ) ) );
    }

    @Test
    public void testGetParentsNoPartOf() {
        Set<Relation> res = go.getParents( go9.getGoId(), false );
        Assert.assertThat( res, Matchers.containsInAnyOrder( new Relation( go4, RelationshipType.IS_A ) ) );
    }

    @Test
    public void testGetParents() {
        Set<Relation> res = go.getParents( go7.getGoId(), true );
        Assert.assertThat( res,
                Matchers.containsInAnyOrder( new Relation( go5, RelationshipType.IS_A ),
                        new Relation( go6, RelationshipType.IS_A ) ) );
    }

    @Test
    public void testGetParentsNull() {
        String g = null;
        try {
            Set<Relation> res = go.getParents( g, true );
            Assert.fail( "Should error with null term" );
        } catch ( NullPointerException e ) {
            // Expected
        }
    }

    @Test
    public void testGetParentsRoot() {
        Set<Relation> res = go.getParents( go0.getGoId(), false );
        Assert.assertThat( res.size(), Matchers.is( 0 ) );
    }

    // getEdition

    @Test
    public void testGetEdition() {
        Assert.assertThat( go.getEdition(), Matchers.is( goEdition ) );
    }

}
