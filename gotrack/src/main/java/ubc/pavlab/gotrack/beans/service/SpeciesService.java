/*
 * The gotrack project
 * 
 * Copyright (c) 2015 University of British Columbia
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import ubc.pavlab.gotrack.beans.DAOFactoryBean;
import ubc.pavlab.gotrack.dao.SpeciesDAO;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.dto.SpeciesDTO;

/**
 * Service layer on top of species DAO. Contains methods for fetching information related to species from the database.
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ApplicationScoped
public class SpeciesService implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3894579931798244757L;

    private static final Logger log = Logger.getLogger( SpeciesService.class );

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    private SpeciesDAO speciesDAO;

    public SpeciesService() {
        log.info( "SpeciesService created" );
    }

    @PostConstruct
    public void init() {
        log.info( "SpeciesService init" );
        speciesDAO = daoFactoryBean.getGotrack().getSpeciesDAO();

    }

    /**
     * @return list of all species ordered by id.
     */
    public List<Species> list() {

        List<SpeciesDTO> dto = speciesDAO.list();
        List<Species> species = new ArrayList<>();

        for ( SpeciesDTO speciesDTO : dto ) {
            species.add( new Species( speciesDTO.getId(), speciesDTO.getCommonName(), speciesDTO.getScientificName(),
                    speciesDTO.getTaxon(), speciesDTO.getInteractingTaxon() ) );
        }

        return species;

    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }
}
