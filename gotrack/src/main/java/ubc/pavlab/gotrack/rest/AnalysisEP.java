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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ubc.pavlab.gotrack.analysis.Enrichment;
import ubc.pavlab.gotrack.analysis.EnrichmentAnalysis;
import ubc.pavlab.gotrack.analysis.EnrichmentResult;
import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.service.EnrichmentService;
import ubc.pavlab.gotrack.model.Aspect;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.StatusPoller;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.rest.marshal.EnrichmentHistoricalRequest;
import ubc.pavlab.gotrack.model.rest.marshal.EnrichmentRequest;
import ubc.pavlab.gotrack.model.table.GeneMatches;
import ubc.pavlab.gotrack.model.table.GeneMatches.MatchType;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Path("/analysis")
@Singleton
public class AnalysisEP {

    private static final Logger log = Logger.getLogger( AnalysisEP.class );

    @Inject
    private Cache cache;

    @Inject
    EnrichmentService enrichmentService;

    @Context
    UriInfo uri;

    public AnalysisEP() {
        log.info( "AnalysisEP REST created" );
    }

    /**
     * @param request
     * @param msg
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response postEnrichmentCurrent( final EnrichmentRequest req ) {
        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).build();
            }

            Edition ed = cache.getCurrentEditions( species.getId() );

            response.put( "edition", new JSONObject( ed ) );

            MultipleTestCorrection mulTestCor = MultipleTestCorrection.BH;
            double threshold = 0.05;
            int min = 5;
            int max = 200;
            Set<Aspect> aspectsFilter = null;

            // Convert list of strings to best possible matches in genes
            Map<MatchType, List<GeneMatches>> gmMap = deserializeGenes( req.genes, species );

            Set<Gene> hitList = new HashSet<>();
            Set<GeneMatches> unknown = new HashSet<>();
            for ( Entry<MatchType, List<GeneMatches>> entry : gmMap.entrySet() ) {
                if ( entry.getKey().equals( MatchType.EXACT ) || entry.getKey().equals( MatchType.EXACT_SYNONYM ) ) {
                    // Create histlist from exact and exact_synonym matches
                    for ( GeneMatches gm : entry.getValue() ) {
                        hitList.add( gm.getSelectedGene() );
                    }
                } else {
                    // Create unknown list from other types
                    for ( GeneMatches gm : entry.getValue() ) {
                        unknown.add( gm );
                    }
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", gmMap.get( MatchType.EXACT ) );
            genesJSON.put( "exact_synonym", gmMap.get( MatchType.EXACT_SYNONYM ) );
            genesJSON.put( "unknown", unknown );
            response.put( "input_genes", genesJSON );

            // Attach Species

            response.put( "species", new JSONObject( species ) );

            // Settings

            response.put( "mt_corr_method", new JSONObject( mulTestCor ).put( "key", mulTestCor ) );
            response.put( "threshold", threshold );
            response.put( "min_go_geneset", min );
            response.put( "max_go_geneset", max );
            response.put( "aspect_filter", aspectsFilter );

            if ( hitList == null || hitList.size() == 0 ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).build();
            }

            Enrichment<GeneOntologyTerm, Gene> enrichment = enrichmentService.singleEnrichment( ed, hitList,
                    species.getId(), mulTestCor, threshold, min, max, aspectsFilter );

            JSONObject enrichmentJSON = enrichmentToJSON( ed, enrichment );

            response.put( "enrichment_results", enrichmentJSON );

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
     * @param request
     * @param msg
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/historical")
    public Response postEnrichmentHistorical( final EnrichmentHistoricalRequest req ) {
        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).build();
            }
            Calendar c = Calendar.getInstance();

            // Get date
            if ( req.month < 1 || req.month > 12 ) {
                return Response.status( 400 ).entity( fail( 400, "Invalid month." ).toString() ).build();
            }
            if ( req.year < 1990 || req.year > c.get( Calendar.YEAR ) ) {
                return Response.status( 400 ).entity( fail( 400, "Invalid year." ).toString() ).build();
            }

            c.set( req.year, req.month - 1, 1, 0, 0 );
            Date inputDate = c.getTime();

            Collection<Edition> eds = cache.getAllEditions( species.getId() );
            Edition closestEdition = eds.iterator().next();
            long minDayDiff = Math.abs( getDateDiff( closestEdition.getDate(), inputDate ) );

            for ( Edition edition : eds ) {
                long dayDiff = Math.abs( getDateDiff( edition.getDate(), inputDate ) );
                if ( dayDiff < minDayDiff ) {
                    // new closest
                    closestEdition = edition;
                    minDayDiff = dayDiff;
                }
            }

            response.put( "edition", new JSONObject( closestEdition ) );

            MultipleTestCorrection mulTestCor = MultipleTestCorrection.BH;
            double threshold = 0.05;
            int min = 5;
            int max = 200;
            Set<Aspect> aspectsFilter = null;

            // Convert list of strings to best possible matches in genes
            Map<MatchType, List<GeneMatches>> gmMap = deserializeGenes( req.genes, species );

            Set<Gene> hitList = new HashSet<>();
            Set<GeneMatches> unknown = new HashSet<>();
            for ( Entry<MatchType, List<GeneMatches>> entry : gmMap.entrySet() ) {
                if ( entry.getKey().equals( MatchType.EXACT ) || entry.getKey().equals( MatchType.EXACT_SYNONYM ) ) {
                    // Create histlist from exact and exact_synonym matches
                    for ( GeneMatches gm : entry.getValue() ) {
                        hitList.add( gm.getSelectedGene() );
                    }
                } else {
                    // Create unknown list from other types
                    for ( GeneMatches gm : entry.getValue() ) {
                        unknown.add( gm );
                    }
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", gmMap.get( MatchType.EXACT ) );
            genesJSON.put( "exact_synonym", gmMap.get( MatchType.EXACT_SYNONYM ) );
            genesJSON.put( "unknown", unknown );
            response.put( "input_genes", genesJSON );

            // Attach Species

            response.put( "species", new JSONObject( species ) );

            // Settings

            response.put( "mt_corr_method", new JSONObject( mulTestCor ).put( "key", mulTestCor ) );
            response.put( "threshold", threshold );
            response.put( "min_go_geneset", min );
            response.put( "max_go_geneset", max );
            response.put( "aspect_filter", aspectsFilter );

            if ( hitList == null || hitList.size() == 0 ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).build();
            }

            Enrichment<GeneOntologyTerm, Gene> enrichment = enrichmentService.singleEnrichment( closestEdition, hitList,
                    species.getId(), mulTestCor, threshold, min, max, aspectsFilter );

            JSONObject enrichmentJSON = enrichmentToJSON( closestEdition, enrichment );

            response.put( "enrichment_results", enrichmentJSON );

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
     * @param request
     * @param msg
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/complete")
    public Response postEnrichmentComplete( final EnrichmentRequest req ) {
        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).build();
            }

            MultipleTestCorrection mulTestCor = MultipleTestCorrection.BH;
            double threshold = 0.05;
            int min = 5;
            int max = 200;
            Set<Aspect> aspectsFilter = null;

            // Convert list of strings to best possible matches in genes
            Map<MatchType, List<GeneMatches>> gmMap = deserializeGenes( req.genes, species );

            Set<Gene> hitList = new HashSet<>();
            Set<GeneMatches> unknown = new HashSet<>();
            for ( Entry<MatchType, List<GeneMatches>> entry : gmMap.entrySet() ) {
                if ( entry.getKey().equals( MatchType.EXACT ) || entry.getKey().equals( MatchType.EXACT_SYNONYM ) ) {
                    // Create histlist from exact and exact_synonym matches
                    for ( GeneMatches gm : entry.getValue() ) {
                        hitList.add( gm.getSelectedGene() );
                    }
                } else {
                    // Create unknown list from other types
                    for ( GeneMatches gm : entry.getValue() ) {
                        unknown.add( gm );
                    }
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", gmMap.get( MatchType.EXACT ) );
            genesJSON.put( "exact_synonym", gmMap.get( MatchType.EXACT_SYNONYM ) );
            genesJSON.put( "unknown", unknown );
            response.put( "input_genes", genesJSON );

            // Attach Species

            response.put( "species", new JSONObject( species ) );

            // Settings 

            response.put( "mt_corr_method", new JSONObject( mulTestCor ).put( "key", mulTestCor ) );
            response.put( "threshold", threshold );
            response.put( "min_go_geneset", min );
            response.put( "max_go_geneset", max );
            response.put( "aspect_filter", aspectsFilter );

            if ( hitList == null || hitList.size() == 0 ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).build();
            }

            EnrichmentAnalysis analysis = enrichmentService.enrichment( hitList, species.getId(), mulTestCor,
                    threshold, min, max, aspectsFilter, new StatusPoller() );

            JSONArray dataJSON = new JSONArray();

            for ( Entry<Edition, Enrichment<GeneOntologyTerm, Gene>> editionEntry : analysis.getRawResults()
                    .entrySet() ) {

                Edition ed = editionEntry.getKey();
                Enrichment<GeneOntologyTerm, Gene> enrichmentResults = editionEntry.getValue();

                JSONObject enrichmentJSON = enrichmentToJSON( ed, enrichmentResults );

                dataJSON.put( enrichmentJSON );

            }

            response.put( "enrichment_data", dataJSON );

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
     * Get a diff between two dates
     * 
     * @param date1 the oldest date
     * @param date2 the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    private static long getDateDiff( Date date1, Date date2 ) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return TimeUnit.DAYS.convert( diffInMillies, TimeUnit.MILLISECONDS );
    }

    /**
     * Convert list of strings to best possible matches in genes
     * 
     * @param geneInputs
     * @return
     */
    private Map<MatchType, List<GeneMatches>> deserializeGenes( Collection<String> geneInputs, Species species ) {

        Map<MatchType, List<GeneMatches>> results = new EnumMap<>( MatchType.class );

        for ( String geneInput : geneInputs ) {

            List<GeneMatches> g = cache.complete( geneInput, species.getId(), Integer.MAX_VALUE,
                    MatchType.EXACT_SYNONYM );
            MatchType bestType;
            if ( g.isEmpty() ) {
                // No Match
                g.add( new GeneMatches( geneInput, null, MatchType.NO_MATCH ) );
                bestType = MatchType.NO_MATCH;
            } else {
                bestType = g.get( 0 ).getType();
            }

            for ( GeneMatches gm : g ) {
                if ( !gm.getType().equals( bestType ) ) {
                    // We've moved on to worse types because this list is sorted.
                    break;
                }
                List<GeneMatches> gmList = results.get( gm.getType() );
                if ( gmList == null ) {
                    gmList = new ArrayList<>();
                    results.put( gm.getType(), gmList );
                }
                gmList.add( gm );
            }

        }
        return results;
    }

    private static JSONObject enrichmentToJSON( Edition ed, Enrichment<GeneOntologyTerm, Gene> enrichment ) {
        JSONObject results = new JSONObject();
        JSONArray enrichmentData = new JSONArray();

        for ( Entry<GeneOntologyTerm, EnrichmentResult> entry : enrichment.getSignificantResults().entrySet() ) {
            JSONObject entryJSON = new JSONObject();

            GeneOntologyTerm term = entry.getKey();
            JSONObject termJSON = new JSONObject();
            termJSON.put( "go_id", term.getGoId() );
            termJSON.put( "aspect", term.getAspect().getLabel() );
            termJSON.put( "name", term.getName() );

            entryJSON.put( "term", termJSON );
            entryJSON.put( "result", new JSONObject( entry.getValue() ) );

            enrichmentData.put( entryJSON );
        }

        results.put( "edition", new JSONObject( ed ) );
        results.put( "results", enrichmentData );

        return results;
    }

}
