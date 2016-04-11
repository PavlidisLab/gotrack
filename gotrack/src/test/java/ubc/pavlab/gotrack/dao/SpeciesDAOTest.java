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
import ubc.pavlab.gotrack.model.dto.SpeciesDTO;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class SpeciesDAOTest extends BaseTest {

    private static final Logger log = Logger.getLogger( SpeciesDAOTest.class );
    private static SpeciesDAO speciesDAO;

    @BeforeClass
    public static void classSetup() {
        speciesDAO = daoFactory.getSpeciesDAO();
    }

    @Before
    public void setup() {

    }

    @After
    public void after() {

    }

    @Test
    public void testFind1() {
        SpeciesDTO dto = speciesDAO.find( 7L );
        Assert.assertThat( dto, Matchers.notNullValue() );
        Assert.assertThat( dto.getId(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getCommonName(), Matchers.is( "Human" ) );
        Assert.assertThat( dto.getScientificName(), Matchers.is( "Homo sapiens" ) );
        Assert.assertThat( dto.getTaxon(), Matchers.is( 9606 ) );
        Assert.assertThat( dto.getInteractingTaxon(), Matchers.nullValue() );
    }

    @Test
    public void testFind2() {
        SpeciesDTO dto = speciesDAO.find( 8L );
        Assert.assertThat( dto, Matchers.notNullValue() );
        Assert.assertThat( dto.getId(), Matchers.is( 8 ) );
        Assert.assertThat( dto.getCommonName(), Matchers.is( "Mouse" ) );
        Assert.assertThat( dto.getScientificName(), Matchers.is( "Mus musculus" ) );
        Assert.assertThat( dto.getTaxon(), Matchers.is( 10090 ) );
        Assert.assertThat( dto.getInteractingTaxon(), Matchers.nullValue() );
    }

    @Test
    public void testList() {
        List<SpeciesDTO> dtos = speciesDAO.list();
        Assert.assertThat( dtos.size(), Matchers.is( 2 ) );

        // ordered by id

        SpeciesDTO dto = dtos.get( 0 );

        Assert.assertThat( dto, Matchers.notNullValue() );
        Assert.assertThat( dto.getId(), Matchers.is( 7 ) );
        Assert.assertThat( dto.getCommonName(), Matchers.is( "Human" ) );
        Assert.assertThat( dto.getScientificName(), Matchers.is( "Homo sapiens" ) );
        Assert.assertThat( dto.getTaxon(), Matchers.is( 9606 ) );
        Assert.assertThat( dto.getInteractingTaxon(), Matchers.nullValue() );

        dto = dtos.get( 1 );

        Assert.assertThat( dto, Matchers.notNullValue() );
        Assert.assertThat( dto.getId(), Matchers.is( 8 ) );
        Assert.assertThat( dto.getCommonName(), Matchers.is( "Mouse" ) );
        Assert.assertThat( dto.getScientificName(), Matchers.is( "Mus musculus" ) );
        Assert.assertThat( dto.getTaxon(), Matchers.is( 10090 ) );
        Assert.assertThat( dto.getInteractingTaxon(), Matchers.nullValue() );
    }

}
