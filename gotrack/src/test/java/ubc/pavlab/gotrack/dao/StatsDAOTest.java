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

import java.util.List;

import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ubc.pavlab.gotrack.BaseTest;
import ubc.pavlab.gotrack.model.dto.GeneStatsDTO;
import ubc.pavlab.gotrack.model.dto.TermStatsDTO;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class StatsDAOTest extends BaseTest {

    private static final Logger log = Logger.getLogger( StatsDAOTest.class );
    private static StatsDAO statsDAO;

    @BeforeClass
    public static void classSetup() {
        statsDAO = daoFactory.getStatsDAO();
    }

    @Before
    public void setup() {

    }

    @After
    public void after() {

    }

    @Test
    public void testIncrementGeneHitAndList() {
        List<GeneStatsDTO> res = statsDAO.listGenes();

        int previousGenesCount = 0;
        for ( GeneStatsDTO dto : res ) {
            if ( dto.getSpeciesId() == 7 && dto.getSymbol().equals( "LONP1" ) ) {
                previousGenesCount = dto.getCount();
            }
        }
        statsDAO.incrementGeneHit( 7, "LONP1" );

        res = statsDAO.listGenes();

        boolean found = false;
        for ( GeneStatsDTO dto : res ) {
            if ( dto.getSpeciesId() == 7 && dto.getSymbol().equals( "LONP1" ) ) {
                Assert.assertThat( dto.getCount(), Matchers.is( previousGenesCount + 1 ) );
                found = true;
            }
        }

        if ( !found ) {
            Assert.fail( "Failed to write new gene during incrementGeneHit" );
        }
    }

    @Test
    public void testIncrementTermHitAndList() {
        List<TermStatsDTO> res = statsDAO.listTerms();

        int previousTermsCount = 0;
        for ( TermStatsDTO dto : res ) {
            if ( dto.getGoId().equals( "GO:0030154" ) ) {
                previousTermsCount = dto.getCount();
            }
        }

        statsDAO.incrementTermHit( "GO:0030154" );

        res = statsDAO.listTerms();

        boolean found = false;
        for ( TermStatsDTO dto : res ) {
            if ( dto.getGoId().equals( "GO:0030154" ) ) {
                Assert.assertThat( dto.getCount(), Matchers.is( previousTermsCount + 1 ) );
                found = true;
            }
        }

        if ( !found ) {
            Assert.fail( "Failed to write new term during incrementTermHit" );
        }

    }

}
