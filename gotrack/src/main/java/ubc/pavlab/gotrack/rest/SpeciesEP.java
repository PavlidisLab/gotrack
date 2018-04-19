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

package ubc.pavlab.gotrack.rest;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Species;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Path("/species")
@Singleton
public class SpeciesEP {

    private static final Logger log = Logger.getLogger( SpeciesEP.class );

    @Inject
    private Cache cache;

    @Context
    UriInfo uri;

    public SpeciesEP() {
        log.info( "SpeciesEP REST created" );
    }

    /**
     * @return All species
     */
    @GET
    @Path("/list")
    public Response list() {
        JSONObject response = new JSONObject();
        for ( Species species : cache.getSpeciesList() ) {
            response.put( species.getId().toString(), new JSONObject( species ) );
        }
        return Response.ok( response.toString(), MediaType.APPLICATION_JSON ).build();
    }

    /**
     * @return Single species
     */
    @GET
    @Path("/id/{speciesId}")
    public Response getSpecies(@PathParam("speciesId") Integer speciesId) {
        Species species = cache.getSpecies( speciesId );
        JSONObject response = new JSONObject();
        if ( species == null ) {
            response.put("error", "Unknown Species ID");
            return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
        } else {
            response = new JSONObject(species);
        }

        return Response.ok( response.toString(), MediaType.APPLICATION_JSON ).build();
    }

}
