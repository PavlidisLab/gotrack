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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ubc.pavlab.gotrack.analysis.*;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.service.EnrichmentService;
import ubc.pavlab.gotrack.beans.service.MultifunctionalityService;
import ubc.pavlab.gotrack.model.*;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.rest.marshal.EnrichmentHistoricalRequest;
import ubc.pavlab.gotrack.model.rest.marshal.EnrichmentRequest;
import ubc.pavlab.gotrack.model.search.GeneMatch;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
@Path("/analysis")
@Singleton
public class AnalysisEP {

    private static final Logger log = Logger.getLogger( AnalysisEP.class );

    @Inject
    private Cache cache;

    @Inject
    EnrichmentService enrichmentService;

    @Inject
    private MultifunctionalityService multifunctionalityService;

    @Context
    UriInfo uri;

    public AnalysisEP() {
        log.info( "AnalysisEP REST created" );
    }

    /**
     * @param req
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/enrichment")
    public Response postEnrichmentCurrent( final EnrichmentRequest req ) {
        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            Edition ed = cache.getCurrentEditions( species );

            response.put( "edition", new JSONObject( ed ) );

            MultipleTestCorrection mulTestCor = MultipleTestCorrection.BH;
            double threshold = 0.05;
            int min = 5;
            int max = 200;
            Set<Aspect> aspectsFilter = null;


            Set<Gene> hitList = Sets.newHashSet();
            Set<GeneMatch> exact = Sets.newHashSet();
            Set<GeneMatch> synonym = Sets.newHashSet();
            Set<GeneMatch> unknown = Sets.newHashSet();
            // Convert list of strings to best possible matches in genes
            Map<String, GeneMatch> gmMap = deserializeGenes( req.genes, species );

            for ( GeneMatch match : gmMap.values() ) {
                if ( match.getType().equals( GeneMatch.Type.SINGLE ) ) {
                    if ( match.getLevel().equals( GeneMatch.Level.PRIMARY ) ) {
                        exact.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else if ( match.getLevel().equals( GeneMatch.Level.SYNONYM ) ) {
                        synonym.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else {
                        unknown.add( match ); // This shouldn't happen, we restrict result in deserialize
                    }
                } else {
                    unknown.add( match );
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", exact );
            genesJSON.put( "exact_synonym", synonym );
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

            if ( hitList.isEmpty() ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            Enrichment<GeneOntologyTerm, Gene> enrichment = enrichmentService.singleEnrichment( ed, hitList,
                    species, mulTestCor, threshold, min, max, aspectsFilter );

            JSONObject enrichmentJSON = enrichmentToJSON( ed, enrichment );

            response.put( "enrichment_results", enrichmentJSON );

            response.put( "httpstatus", 200 );
            response.put( "success", true );
        } catch (JSONException e1) {
            log.error( "Malformed JSON", e1 );
            return Response.status( 400 ).entity( fail( 400, "Malformed JSON" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e1) {
            log.error( "Something went wrong!", e1 );
            return Response.status( 500 ).entity( fail( 500, e1.getMessage() ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        }
        return Response.ok( response.toString(), MediaType.APPLICATION_JSON ).build();

    }

    /**
     * @param req
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/enrichment/historical")
    public Response postEnrichmentHistorical( final EnrichmentHistoricalRequest req ) {
        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }
            Calendar c = Calendar.getInstance();

            // Get date
            if ( req.month < 1 || req.month > 12 ) {
                return Response.status( 400 ).entity( fail( 400, "Invalid month." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }
            if ( req.year < 1990 || req.year > c.get( Calendar.YEAR ) ) {
                return Response.status( 400 ).entity( fail( 400, "Invalid year." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            c.set( req.year, req.month - 1, 1, 0, 0 );
            Date inputDate = c.getTime();

            Edition closestEdition = closestEdition( inputDate, species );

            response.put( "edition", new JSONObject( closestEdition ) );

            MultipleTestCorrection mulTestCor = MultipleTestCorrection.BH;
            double threshold = 0.05;
            int min = 5;
            int max = 200;
            Set<Aspect> aspectsFilter = null;

            Set<Gene> hitList = Sets.newHashSet();
            Set<GeneMatch> exact = Sets.newHashSet();
            Set<GeneMatch> synonym = Sets.newHashSet();
            Set<GeneMatch> unknown = Sets.newHashSet();
            // Convert list of strings to best possible matches in genes
            Map<String, GeneMatch> gmMap = deserializeGenes( req.genes, species );

            for ( GeneMatch match : gmMap.values() ) {
                if ( match.getType().equals( GeneMatch.Type.SINGLE ) ) {
                    if ( match.getLevel().equals( GeneMatch.Level.PRIMARY ) ) {
                        exact.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else if ( match.getLevel().equals( GeneMatch.Level.SYNONYM ) ) {
                        synonym.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else {
                        unknown.add( match ); // This shouldn't happen, we restrict result in deserialize
                    }
                } else {
                    unknown.add( match );
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", exact );
            genesJSON.put( "exact_synonym", synonym );
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

            if ( hitList.isEmpty() ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            Enrichment<GeneOntologyTerm, Gene> enrichment = enrichmentService.singleEnrichment( closestEdition, hitList,
                    species, mulTestCor, threshold, min, max, aspectsFilter );

            JSONObject enrichmentJSON = enrichmentToJSON( closestEdition, enrichment );

            response.put( "enrichment_results", enrichmentJSON );

            response.put( "httpstatus", 200 );
            response.put( "success", true );
        } catch (JSONException e1) {
            log.error( "Malformed JSON", e1 );
            return Response.status( 400 ).entity( fail( 400, "Malformed JSON" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e1) {
            log.error( "Something went wrong!", e1 );
            return Response.status( 500 ).entity( fail( 500, e1.getMessage() ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        }
        return Response.ok( response.toString(), MediaType.APPLICATION_JSON ).build();

    }

    /**
     * @param req
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/enrichment/complete")
    public Response postEnrichmentComplete( final EnrichmentRequest req ) {
        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            MultipleTestCorrection mulTestCor = MultipleTestCorrection.BH;
            double threshold = 0.05;
            int min = 5;
            int max = 200;
            Set<Aspect> aspectsFilter = null;

            Set<Gene> hitList = Sets.newHashSet();
            Set<GeneMatch> exact = Sets.newHashSet();
            Set<GeneMatch> synonym = Sets.newHashSet();
            Set<GeneMatch> unknown = Sets.newHashSet();
            // Convert list of strings to best possible matches in genes
            Map<String, GeneMatch> gmMap = deserializeGenes( req.genes, species );

            for ( GeneMatch match : gmMap.values() ) {
                if ( match.getType().equals( GeneMatch.Type.SINGLE ) ) {
                    if ( match.getLevel().equals( GeneMatch.Level.PRIMARY ) ) {
                        exact.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else if ( match.getLevel().equals( GeneMatch.Level.SYNONYM ) ) {
                        synonym.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else {
                        unknown.add( match ); // This shouldn't happen, we restrict result in deserialize
                    }
                } else {
                    unknown.add( match );
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", exact );
            genesJSON.put( "exact_synonym", synonym );
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

            if ( hitList.isEmpty() ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            EnrichmentAnalysis analysis = enrichmentService.enrichment( hitList, species, mulTestCor,
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

            response.put( "httpstatus", 200 );
            response.put( "success", true );
        } catch (JSONException e1) {
            log.error( "Malformed JSON", e1 );
            return Response.status( 400 ).entity( fail( 400, "Malformed JSON" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e1) {
            log.error( "Something went wrong!", e1 );
            return Response.status( 500 ).entity( fail( 500, e1.getMessage() ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        }
        return Response.ok( response.toString(), MediaType.APPLICATION_JSON ).build();

    }

    /**
     * @param req
     * @return Enrichment results from most recent edition
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/similarity")
    public Response postSimilarity( final EnrichmentHistoricalRequest req ) {
//        log.info( req );
        JSONObject response = new JSONObject();
        try {
            // Get Species
            Species species = cache.getSpecies( req.speciesId );

            if ( species == null ) {
                return Response.status( 400 ).entity( fail( 400, "Unknown Species ID" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }
            Calendar c = Calendar.getInstance();

            // Get date
            if ( req.month < 1 || req.month > 12 ) {
                return Response.status( 400 ).entity( fail( 400, "Invalid month." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }
            if ( req.year < 1990 || req.year > c.get( Calendar.YEAR ) ) {
                return Response.status( 400 ).entity( fail( 400, "Invalid year." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            c.set( req.year, req.month - 1, 1, 0, 0 );
            Date inputDate = c.getTime();

            Edition closestEdition = closestEdition( inputDate, species );

            Set<Gene> hitList = Sets.newHashSet();
            Set<GeneMatch> exact = Sets.newHashSet();
            Set<GeneMatch> synonym = Sets.newHashSet();
            Set<GeneMatch> unknown = Sets.newHashSet();
            // Convert list of strings to best possible matches in genes
            Map<String, GeneMatch> gmMap = deserializeGenes( req.genes, species );

            for ( GeneMatch match : gmMap.values() ) {
                if ( match.getType().equals( GeneMatch.Type.SINGLE ) ) {
                    if ( match.getLevel().equals( GeneMatch.Level.PRIMARY ) ) {
                        exact.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else if ( match.getLevel().equals( GeneMatch.Level.SYNONYM ) ) {
                        synonym.add( match );
                        hitList.add( match.getSelectedGene() );
                    } else {
                        unknown.add( match ); // This shouldn't happen, we restrict result in deserialize
                    }
                } else {
                    unknown.add( match );
                }
            }

            // Attach genes

            JSONObject genesJSON = new JSONObject();
            genesJSON.put( "exact", exact );
            genesJSON.put( "exact_synonym", synonym );
            genesJSON.put( "unknown", unknown );
            response.put( "input_genes", genesJSON );

            // Attach Species

            response.put( "species", new JSONObject( species ) );

            // Settings

            response.put( "mt_corr_method", new JSONObject( req.multipleTestCorrection ).put( "key", req.multipleTestCorrection ) );
            response.put( "threshold", req.threshold );
            response.put( "min_go_geneset", req.min );
            response.put( "max_go_geneset", req.max );
            response.put( "aspect_filter", req.aspects );

            if ( hitList.isEmpty() ) {
                return Response.status( 400 ).entity( fail( 400, "0 matching genes." ).toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            Edition referenceEdition = cache.getCurrentEditions( species );

            EnrichmentAnalysis analysis = enrichmentService.enrichment( Sets.newHashSet( closestEdition, referenceEdition )
                    , hitList, species, req.multipleTestCorrection, req.threshold, req.min, req.max, req.aspects );

            SimilarityAnalysis similarityAnalysis = new SimilarityAnalysis( analysis, req.topN, referenceEdition, req.similarityMethod, cache );
            response.put( "similarity_compare_edition", new JSONObject( similarityAnalysis.getReferenceEdition() ) );
            response.put( "similarity_method", new JSONObject( similarityAnalysis.getSimilarityMethod() ).put( "key", similarityAnalysis.getSimilarityMethod() ) );
            response.put( "top_n", similarityAnalysis.getTopN() );

            // Data
            response.put( "data", similarityToJSON( similarityAnalysis, analysis, closestEdition, referenceEdition, req.includeSets) );
            response.put( "reference_data", similarityToJSON( similarityAnalysis, analysis, referenceEdition, referenceEdition, req.includeSets) );

            response.put( "httpstatus", 200 );
            response.put( "success", true );
        } catch (JSONException e1) {
            log.error( "Malformed JSON", e1 );
            return Response.status( 400 ).entity( fail( 400, "Malformed JSON" ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e1) {
            log.error( "Something went wrong!", e1 );
            return Response.status( 500 ).entity( fail( 500, e1.getMessage() ).toString() ).type( MediaType.APPLICATION_JSON ).build();
        }
        return Response.ok( response.toString(), MediaType.APPLICATION_JSON ).build();

    }

    private static JSONObject fail( int httpStatus, String message ) {
        JSONObject response = new JSONObject();
        try {
            response.put( "httpstatus", httpStatus );
            response.put( "success", false );
            response.put( "message", message );
        } catch (JSONException e1) {
            log.error( "Malformed JSON", e1 );
        }
        return response;
    }

    /**
     * Get a diff between two dates
     *
     * @param date1    the oldest date
     * @param date2    the newest date
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
            if ( cache.getAggregate( edition ) != null ) {
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
     * Convert list of strings to best possible matches in genes
     *
     * @param geneInputs
     * @return
     */
    private Map<String, GeneMatch> deserializeGenes( Collection<String> geneInputs, Species species ) {

        Map<String, GeneMatch> results = Maps.newHashMap();

        for ( String geneInput : geneInputs ) {
            GeneMatch match = cache.guessGeneBySymbol( geneInput, species );
            results.put( geneInput, match );
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

    private static JSONArray goSetToJSON( Collection<GeneOntologyTerm> goset ) {
        JSONArray results = new JSONArray();

        for ( GeneOntologyTerm go : goset ) {
            results.put( go.getGoId() );
        }

        return results;
    }

    private JSONObject similarityToJSON(SimilarityAnalysis similarityAnalysis, EnrichmentAnalysis analysis, Edition edition, Edition referenceEdition, boolean includeSets) {
        JSONObject entryJSON = new JSONObject();

        SimilarityScore score = similarityAnalysis.getSimilarityScore( edition );

        JSONObject valuesJSON = new JSONObject();
        valuesJSON.put( "complete_term_sim", score.getCompleteTermSim() );
        valuesJSON.put( "top_term_sim", score.getTopTermSim() );
        valuesJSON.put( "top_gene_sim", score.getTopGeneSim() );
        valuesJSON.put( "top_parents_sim", score.getTopParentsSim() );
        entryJSON.put( "values", valuesJSON );

        // Conversion accounts for obsoletion and alternate ids
        if ( includeSets ) {
            entryJSON.put( "complete_terms", goSetToJSON( cache.convertTerms( referenceEdition, analysis.getTermsSignificant( edition ) ) ) );
            entryJSON.put( "top_terms", goSetToJSON( cache.convertTerms( referenceEdition, score.getTopTerms() ) ) );
            entryJSON.put( "top_parents", goSetToJSON( cache.convertTerms( referenceEdition, score.getTopParents() ) ) );
            entryJSON.put( "top_genes", score.getTopGenes() );
        }

        entryJSON.put( "edition", new JSONObject( edition ) );
        entryJSON.put( "similarity_age", getDateDiff( edition.getDate(), referenceEdition.getDate(), TimeUnit.DAYS ) );
        entryJSON.put( "significant_terms", analysis.getTermsSignificant( edition ).size() );
        // TODO: We don't convert here as the caches which help calculate MF don't yet account for alternate ids
        entryJSON.put( "top_parents_mf",
                multifunctionalityService.multifunctionality( score.getTopParents(), edition ) );
        entryJSON.put( "distinct_tested_terms", analysis.getRawResults( edition ).getCountTestedTerms() );
        return entryJSON;
    }

}
