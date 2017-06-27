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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.MultifunctionalityService;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.search.GeneMatch;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Path("/gene")
@Singleton
public class GeneEP {

    private static final Logger log = Logger.getLogger( GeneEP.class );

    @Inject
    private Cache cache;

    @Inject
    private AnnotationService annotationService;

    @Inject
    private MultifunctionalityService multifunctionalityService;

    @Context
    UriInfo uri;

    public GeneEP() {
        log.info( "GeneEP REST created" );
    }

    /**
     * @return Gene data for given gene(s)
     */
    @GET
    @Path("/complete/species/{speciesId}/symbol/{symbols}")
    public Response complete( @PathParam("speciesId") Integer speciesId, @PathParam("symbols") String symbolsString,
            @QueryParam("year") Integer year, @QueryParam("month") Integer month,
            @QueryParam("minimal") Boolean minimal ) {
        log.info( symbolsString );
        List<String> symbols = Arrays.asList( symbolsString.split( "," ) );

        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).build();
            }

            Edition closestEdition = null;

            minimal = minimal == null ? true : minimal;

            if ( month != null && year != null ) {
                Calendar c = Calendar.getInstance();

                // Get date
                if ( month < 1 || month > 12 ) {
                    return Response.status( 400 ).entity( fail( 400, "Invalid month." ).toString() ).build();
                }
                if ( year < 1990 || year > c.get( Calendar.YEAR ) ) {
                    return Response.status( 400 ).entity( fail( 400, "Invalid year." ).toString() ).build();
                }

                c.set( year, month - 1, 1, 0, 0 );
                Date inputDate = c.getTime();

                closestEdition = closestEdition( inputDate, species );
                response.put( "edition", new JSONObject( closestEdition ) );
            } else {
                if ( month != null || year != null ) {
                    return Response.status( 400 )
                            .entity( fail( 400, "Malformed Date, requires year={int}&month={int}" ).toString() )
                            .build();
                }
            }

            // Convert list of strings to best possible matches in genes
            Map<String, GeneMatch> gms = deserializeGenes( symbols, species );

            // Retrieve info for each matched gene
            JSONArray genesArray = new JSONArray();
            for ( GeneMatch gm : gms.values() ) {
                JSONObject geneJSON = new JSONObject();
                geneJSON.put( "gene_match", minimal ? geneMatchToMinimalJSON( gm ) : new JSONObject( gm ) );
                if ( gm.getType().equals( GeneMatch.Type.SINGLE ) ) {
                    Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> data = retrieveData(
                            gm.getSelectedGene() );

                    ImmutableMap<Edition, Map<GeneOntologyTerm, Set<Annotation>>> directSets = data
                            .get( AnnotationType.D ).rowMap();

                    JSONArray editionArray = new JSONArray();

                    Set<Edition> editionsToIterate;

                    if ( closestEdition != null ) {
                        // Retain only this edition
                        editionsToIterate = Sets.newHashSet( closestEdition );
                    } else {
                        editionsToIterate = data.get( AnnotationType.I ).rowMap().keySet();
                    }

                    for ( Edition ed : editionsToIterate ) {
                        Map<GeneOntologyTerm, Set<Annotation>> termsMap = data.get( AnnotationType.I ).rowMap()
                                .get( ed );
                        Set<GeneOntologyTerm> directTerms = directSets.get( ed ).keySet();
                        JSONObject editionResults = new JSONObject();
                        editionResults.put( "edition", new JSONObject( ed ) );

                        JSONArray termData = new JSONArray();
                        for ( Entry<GeneOntologyTerm, Set<Annotation>> entry2 : termsMap.entrySet() ) {
                            GeneOntologyTerm t = entry2.getKey();
                            termData.put( annotationsToJSON( entry2.getKey(), directTerms.contains( t ), minimal ) );
                        }
                        editionResults.put( "terms", termData );
                        editionResults.put( "mf",
                                multifunctionalityService.multifunctionality( termsMap.keySet(), species, ed ) );

                        editionArray.put( editionResults );
                    }

                    geneJSON.put( "data", editionArray );
                }
                genesArray.put( geneJSON );
            }

            // Attach genes
            response.put( "genes", genesArray );
            // Attach Species
            response.put( "species", new JSONObject( species ) );

            response.put( "httpstatus", 202 );
            response.put( "success", true );
        } catch ( JSONException e1 ) {
            log.error( "Malformed JSON", e1 );
            return Response.status( 400 ).entity( fail( 400, "Malformed JSON" ).toString() ).build();
        } catch ( Exception e1 ) {
            log.error( "Something went wrong!", e1 );
            return Response.status( 500 ).entity( fail( 500, e1.getMessage() ).toString() ).build();
        }
        return Response.status( 202 ).entity( response.toString() ).build();

    }

    /**
     * @return Gene data for given gene(s)
     */
    @GET
    @Path("/mf/species/{speciesId}/symbol/{symbols}")
    public Response mf( @PathParam("speciesId") Integer speciesId, @PathParam("symbols") String symbolsString,
            @QueryParam("year") Integer year, @QueryParam("month") Integer month,
            @QueryParam("minimal") Boolean minimal ) {
        log.info( symbolsString );
        List<String> symbols = Arrays.asList( symbolsString.split( "," ) );

        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).build();
            }

            Edition closestEdition = null;

            minimal = minimal == null ? true : minimal;

            if ( month != null && year != null ) {
                Calendar c = Calendar.getInstance();

                // Get date
                if ( month < 1 || month > 12 ) {
                    return Response.status( 400 ).entity( fail( 400, "Invalid month." ).toString() ).build();
                }
                if ( year < 1990 || year > c.get( Calendar.YEAR ) ) {
                    return Response.status( 400 ).entity( fail( 400, "Invalid year." ).toString() ).build();
                }

                c.set( year, month - 1, 1, 0, 0 );
                Date inputDate = c.getTime();

                closestEdition = closestEdition( inputDate, species );
                response.put( "edition", new JSONObject( closestEdition ) );
            } else {
                if ( month != null || year != null ) {
                    return Response.status( 400 )
                            .entity( fail( 400, "Malformed Date, requires year={int}&month={int}" ).toString() )
                            .build();
                }
            }

            // Convert list of strings to best possible matches in genes
            Map<String, GeneMatch> gms = deserializeGenes( symbols, species );

            // Retrieve info for each matched gene
            JSONArray genesArray = new JSONArray();
            for ( GeneMatch gm : gms.values() ) {
                JSONObject geneJSON = new JSONObject();
                geneJSON.put( "gene_match", minimal ? geneMatchToMinimalJSON( gm ) : new JSONObject( gm ) );
                if ( gm.getType().equals( GeneMatch.Type.SINGLE ) ) {

                    JSONArray editionArray = new JSONArray();

                    Map<Edition, Double> data;
                    if ( closestEdition != null ) {
                        // Retain only this edition
                        data = Maps.newHashMap();
                        data.put( closestEdition,
                                multifunctionalityService.multifunctionality( gm.getSelectedGene(), closestEdition ) );
                    } else {
                        data = multifunctionalityService.multifunctionality( gm.getSelectedGene() );
                    }

                    for ( Entry<Edition, Double> entry : data.entrySet() ) {
                        JSONObject editionResults = new JSONObject();
                        editionResults.put( "edition", new JSONObject( entry.getKey() ) );

                        editionResults.put( "mf", entry.getValue() );

                        editionArray.put( editionResults );
                    }

                    geneJSON.put( "data", editionArray );
                }
                genesArray.put( geneJSON );
            }

            // Attach genes
            response.put( "genes", genesArray );
            // Attach Species
            response.put( "species", new JSONObject( species ) );

            response.put( "httpstatus", 202 );
            response.put( "success", true );
        } catch ( JSONException e1 ) {
            log.error( "Malformed JSON", e1 );
            return Response.status( 400 ).entity( fail( 400, "Malformed JSON" ).toString() ).build();
        } catch ( Exception e1 ) {
            log.error( "Something went wrong!", e1 );
            return Response.status( 500 ).entity( fail( 500, e1.getMessage() ).toString() ).build();
        }
        return Response.status( 202 ).entity( response.toString() ).build();

    }

    private static JSONObject fail( int httpStatus, String message ) {
        JSONObject response = new JSONObject();
        try {
            response.put( "httpstatus", httpStatus );
            response.put( "success", false );
            response.put( "message", message );
        } catch ( JSONException e1 ) {
            log.error( "Malformed JSON", e1 );
        }
        return response;
    }

    /**
     * Attempt to get data from cache, if not in cache get from DB. Afterwards apply filter.
     */
    private Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> retrieveData( Gene gene ) {

        Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> rawData = Maps.newHashMap();

        Map<AnnotationType, ImmutableTable<Edition, GeneOntologyTerm, Set<Annotation>>> cachedData = cache
                .getGeneData( gene );

        // Speedy return if no filters are required and data was cached
        if ( cachedData != null ) {
            return cachedData;
        }

        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> directRawData = annotationService.fetchTrackData( gene );
        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> inferredRawData = propagate( directRawData );

        // Convert to ImmutableTable

        // Direct
        ImmutableTable.Builder<Edition, GeneOntologyTerm, Set<Annotation>> rawdataBuilder = new ImmutableTable.Builder<Edition, GeneOntologyTerm, Set<Annotation>>();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : directRawData.entrySet() ) {
            Edition ed = entry.getKey();
            for ( Entry<GeneOntologyTerm, Set<Annotation>> entry2 : entry.getValue().entrySet() ) {
                rawdataBuilder.put( ed, entry2.getKey(), entry2.getValue() );
            }

        }
        rawdataBuilder.orderRowsBy( new Comparator<Edition>() {
            @Override
            public int compare( Edition o1, Edition o2 ) {
                return o1.compareTo( o2 );
            }
        } );

        rawData.put( AnnotationType.D, rawdataBuilder.build() );

        // Indirect
        rawdataBuilder = new ImmutableTable.Builder<Edition, GeneOntologyTerm, Set<Annotation>>();
        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : inferredRawData.entrySet() ) {
            Edition ed = entry.getKey();
            for ( Entry<GeneOntologyTerm, Set<Annotation>> entry2 : entry.getValue().entrySet() ) {
                rawdataBuilder.put( ed, entry2.getKey(), entry2.getValue() );
            }

        }
        rawdataBuilder.orderRowsBy( new Comparator<Edition>() {
            @Override
            public int compare( Edition o1, Edition o2 ) {
                return o1.compareTo( o2 );
            }
        } );
        rawData.put( AnnotationType.I, rawdataBuilder.build() );

        cache.addGeneData( gene, rawData );

        return rawData;

    }

    /**
     * Propagates the annotations from the retrieved data to ancestor terms.
     */
    private Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> propagate(
            Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> map ) {
        Map<Edition, Map<GeneOntologyTerm, Set<Annotation>>> propagatedData = new HashMap<>();

        for ( Entry<Edition, Map<GeneOntologyTerm, Set<Annotation>>> entry : map.entrySet() ) {
            Edition ed = entry.getKey();
            propagatedData.put( ed, cache.propagateAnnotations( entry.getValue(), ed ) );
        }
        return propagatedData;
    }

    /**
     * Get a diff between two dates
     * 
     * @param date1 the oldest date
     * @param date2 the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    private static long getDateDiff( Date date1, Date date2, TimeUnit timeUnit ) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert( diffInMillies, TimeUnit.MILLISECONDS );
    }

    private Edition closestEdition( Date inputDate, Species species ) {
        Edition closestEdition = cache.getCurrentEditions( species );
        long minDayDiff = Math.abs( getDateDiff( closestEdition.getDate(), inputDate, TimeUnit.DAYS ) );

        for ( Edition edition : cache.getAllEditions( species ) ) {
            if ( cache.getAggregates( species, edition ) != null ) {
                // Make sure there is data for this edition
                long dayDiff = Math.abs( getDateDiff( edition.getDate(), inputDate, TimeUnit.DAYS ) );
                if ( dayDiff < minDayDiff ) {
                    // new closest
                    closestEdition = edition;
                    minDayDiff = dayDiff;
                }
            }
        }

        return closestEdition;
    }

    /**
     * Convert list of strings to its best possible match in available genes.
     * Checks for Exact Symbol matches and Exact Synonym matches.
     *
     * @param geneInputs
     * @return List of GeneMatch, one for every input query. Unsuccessful matches will have a MatchType.NO_MATCH type.
     */
    private Map<String, GeneMatch> deserializeGenes( Collection<String> geneInputs, Species species ) {

        Map<String, GeneMatch> results = Maps.newHashMap();

        for ( String geneInput : geneInputs ) {
            GeneMatch match = cache.guessGeneBySymbol( geneInput, species );
            results.put( geneInput, match );
        }

        return results;
    }

    private static JSONObject annotationsToJSON( GeneOntologyTerm term, boolean direct, boolean minimal ) {
        JSONObject entryJSON = new JSONObject();

        if ( !minimal ) {

            JSONObject termJSON = new JSONObject();
            termJSON.put( "go_id", term.getGoId() );
            termJSON.put( "aspect", term.getAspect().getLabel() );
            termJSON.put( "name", term.getName() );

            entryJSON.put( "term", termJSON );
            //entryJSON.put( "annotations", annotations );
        } else {
            entryJSON.put( "term", term.getGoId() );
        }

        if ( direct ) {
            entryJSON.put( "direct", "Y" );
        }

        return entryJSON;
    }

    private static JSONObject annotationsToJSON( Edition ed,
            Map<GeneOntologyTerm, Set<Annotation>> annotations, boolean minimal ) {
        JSONObject results = new JSONObject();
        JSONArray data = new JSONArray();

        for ( Entry<GeneOntologyTerm, Set<Annotation>> entry : annotations.entrySet() ) {
            JSONObject entryJSON = new JSONObject();

            GeneOntologyTerm term = entry.getKey();
            JSONObject termJSON = new JSONObject();

            if ( !minimal ) {
                termJSON.put( "go_id", term.getGoId() );
                termJSON.put( "aspect", term.getAspect().getLabel() );
                termJSON.put( "name", term.getName() );
                entryJSON.put( "term", termJSON );
                //entryJSON.put( "annotations", annotations );

                data.put( entryJSON );
            } else {
                data.put( term.getGoId() );
            }

        }

        results.put( "edition", new JSONObject( ed ) );
        results.put( "results", data );

        return results;
    }

    private static JSONObject annotationsToJSON( GeneOntologyTerm t, Map<Edition, Set<Annotation>> annotations,
            boolean minimal ) {
        JSONObject results = new JSONObject();
        JSONArray data = new JSONArray();

        for ( Entry<Edition, Set<Annotation>> entry : annotations.entrySet() ) {
            JSONObject entryJSON = new JSONObject();

            Edition ed = entry.getKey();

            if ( !minimal ) {
                JSONObject editionJSON = new JSONObject( ed );
                entryJSON.put( "term", editionJSON );
                //entryJSON.put( "annotations", annotations );

                data.put( entryJSON );
            } else {
                data.put( ed.getEdition() );
            }

        }

        JSONObject termJSON = new JSONObject();
        termJSON.put( "go_id", t.getGoId() );
        termJSON.put( "aspect", t.getAspect().getLabel() );
        termJSON.put( "name", t.getName() );

        results.put( "term", termJSON );
        results.put( "results", data );

        return results;
    }

    private static JSONObject geneMatchToMinimalJSON( GeneMatch gm ) {
        JSONObject results = new JSONObject();
        results.put( "symbol", gm.getSymbol() );
        results.put( "querySymbol", gm.getQuerySymbol() );
        results.put( "geneID", gm.getSelectedGene().getId() );
        results.put( "type", gm.getType() );
        results.put( "level", gm.getLevel() );

        return results;
    }

    private static JSONArray goSetToJSON( Collection<GeneOntologyTerm> goset ) {
        JSONArray results = new JSONArray();

        for ( GeneOntologyTerm go : goset ) {
            results.put( go.getGoId() );
        }

        return results;
    }

}
