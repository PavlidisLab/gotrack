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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.beans.service.MultifunctionalityService;
import ubc.pavlab.gotrack.model.Aggregate;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.utilities.Tuples;
import ubc.pavlab.gotrack.utilities.Zipper;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Endpoint for downloading large data dumps.
 *
 * @author mjacobson
 */
@Path("/data")
@Singleton
public class BulkDataEP {

    private static final Logger log = Logger.getLogger( BulkDataEP.class );

    //            private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String SEPARATOR = "\t";

    @Inject
    private Cache cache;

    @Inject
    private AnnotationService annotationService;

    @Inject
    private MultifunctionalityService multifunctionalityService;

    private Map<Species, Map<Edition, byte[]>> speciesByGeneCache = Maps.newConcurrentMap();


    @Context
    UriInfo uri;

    public BulkDataEP() {
        log.info( "BulkDataEP REST created" );
    }

    @GET
    @Path("/species/{speciesId}/summary")
    public Response fetchEditionLevelSummaryStatistics( @PathParam("speciesId") Integer speciesId, @QueryParam("edition") Integer editionId ) {

        try {

            Species species = cache.getSpecies( speciesId );


            if ( species == null ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Unknown Species ID" );
                return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            String filename = "summary." + species.getCommonName();

            final List<Edition> editions = selectEditions( species, editionId );

            if ( editions.isEmpty() ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Cannot find edition(s)" );
                return Response.status( 204 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            } else if ( editions.size() == 1 ) {
                Edition ed = editions.get( 0 );
                filename += "." + ed.getEdition() + "-" + ed.getDate();
            }

            Collections.sort( editions );

            String[] headers = new String[]{
                    "edition",
                    "date",
                    "go_date",
                    "avg_direct_by_gene",
                    "avg_inferred_by_gene",
                    "avg_direct_similarity",
                    "avg_inferred_similarity",
                    "avg_inferred_by_term",
                    "avg_multifunctionality",
                    "gene_count"
            };

            StreamingOutput stream = os -> {
                GZIPOutputStream zip = new GZIPOutputStream( os );
                Writer writer = new BufferedWriter( new OutputStreamWriter( zip, "UTF-8" ) );

                writer.write( Arrays.stream( headers ).collect( Collectors.joining( SEPARATOR ) ) );
                writer.write( LINE_SEPARATOR );

                for ( Edition edition : editions ) {

                    Aggregate aggregate = cache.getAggregate( edition );

                    if ( aggregate != null ) {
                        writer.write( edition.getEdition().toString() );
                        writer.write( SEPARATOR );
                        writer.write( edition.getDate().toString() );
                        writer.write( SEPARATOR );
                        writer.write( edition.getGoEdition().getDate().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getAvgDirectByGene().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getAvgInferredByGene().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getAvgDirectSimilarity().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getAvgInferredSimilarity().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getAvgGenesByTerm().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getAvgMultifunctionality().toString() );
                        writer.write( SEPARATOR );
                        writer.write( aggregate.getGeneCount().toString() );
                        writer.write( LINE_SEPARATOR );
                    }
                }
                writer.flush();
                zip.close();
                writer.close();
            };

            return Response.ok( stream )
                    .header( "Content-Disposition", "attachment; filename=" + filename + ".tsv.gz" )
                    .build();

        } catch (Exception ex) {
            log.warn( "error", ex );
            JSONObject response = new JSONObject();
            response.put( "error", ex.getMessage() );
            return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
        }

    }

    @GET
    @Path("/species/{speciesId}/summary/by-gene")
    public Response fetchGeneData( @PathParam("speciesId") Integer speciesId,
                                   @QueryParam("edition") Integer editionId ) {
        // Gene x Date including direct/inferred term counts and mf

        try {

            Species species = cache.getSpecies( speciesId );


            if ( species == null ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Unknown Species ID" );
                return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            String filename = "summary.by-gene." + species.getCommonName();

            final List<Edition> editions = selectEditions( species, editionId );

            if ( editions.isEmpty() ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Cannot find edition(s)" );
                return Response.status( 204 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            } else if ( editions.size() == 1 ) {
                Edition ed = editions.get( 0 );
                filename += "." + ed.getEdition() + "-" + ed.getDate();
            }

            Collections.sort( editions );

            String[] headers = new String[]{
                    "edition",
                    "date",
                    "go_date",
                    "accession",
                    "symbol",
                    "direct_count",
                    "inferred_count",
                    "multifunctionality",
            };

            Map<Edition, byte[]> byGeneCache = speciesByGeneCache.computeIfAbsent( species, k -> Maps.newConcurrentMap() );

            StreamingOutput stream = os -> {
                BufferedOutputStream buff = new BufferedOutputStream( os );

                Charset charset = Charset.forName( "UTF-8" );

                buff.write( Zipper.zip( Arrays.stream( headers ).collect( Collectors.joining( SEPARATOR ) ) + LINE_SEPARATOR, charset ) );

                for ( Edition edition : editions ) {
                    log.info( edition.getEdition() );

                    byte[] cachedData = byGeneCache.computeIfAbsent( edition, k -> {
                        Map<Gene, Set<GeneOntologyTerm>> data = annotationService.fetchEditionSimple( species, edition );
                        StringBuilder editionData = new StringBuilder();
                        for ( Map.Entry<Gene, Set<GeneOntologyTerm>> entry : data.entrySet() ) {
                            Gene gene = entry.getKey();

                            Set<GeneOntologyTerm> inferred = GeneOntologyTerm.propagate( entry.getValue().stream() ).collect( Collectors.toSet() );

                            editionData.append( edition.getEdition().toString() );
                            editionData.append( SEPARATOR );
                            editionData.append( edition.getDate().toString() );
                            editionData.append( SEPARATOR );
                            editionData.append( edition.getGoEdition().getDate().toString() );
                            editionData.append( SEPARATOR );

                            editionData.append( gene.getAccession().getAccession() );
                            editionData.append( SEPARATOR );
                            editionData.append( gene.getSymbol() );
                            editionData.append( SEPARATOR );

                            editionData.append( String.valueOf( entry.getValue().size() ) );
                            editionData.append( SEPARATOR );
                            editionData.append( String.valueOf( inferred.size() ) );
                            editionData.append( SEPARATOR );
                            editionData.append( multifunctionalityService.multifunctionality( inferred, edition ).toString() );
                            editionData.append( LINE_SEPARATOR );

                        }

                        return Zipper.zip( editionData.toString(), charset );
                    } );

                    buff.write( cachedData );
                }
                buff.flush();
                buff.close();
            };

            return Response.ok( stream )
                    .header( "Content-Disposition", "attachment; filename=" + filename + ".tsv.gz" )
                    .build();

        } catch (Exception ex) {
            log.warn( "error", ex );
            JSONObject response = new JSONObject();
            response.put( "error", ex.getMessage() );
            return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
        }

    }

    @GET
    @Path("/species/{speciesId}/summary/by-term")
    public Response fetchAnnotationCountsPerTerm( @PathParam("speciesId") Integer speciesId,
                                                  @QueryParam("edition") Integer editionId ) {

        try {

            Species species = cache.getSpecies( speciesId );


            if ( species == null ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Unknown Species ID" );
                return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            String filename = "summary.by-term." + species.getCommonName();

            final List<Edition> editions = selectEditions( species, editionId );

            if ( editions.isEmpty() ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Cannot find edition(s)" );
                return Response.status( 204 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            } else if ( editions.size() == 1 ) {
                Edition ed = editions.get( 0 );
                filename += "." + ed.getEdition() + "-" + ed.getDate();
            }

            Collections.sort( editions );

//            StringBuilder builder = new StringBuilder();

            String[] headers = new String[]{
                    "edition",
                    "date",
                    "go_date",
                    "go_id",
                    "direct_count",
                    "inferred_count",
            };

            StreamingOutput stream = os -> {
                GZIPOutputStream zip = new GZIPOutputStream( os );
                Writer writer = new BufferedWriter( new OutputStreamWriter( zip, "UTF-8" ) );

                writer.write( Arrays.stream( headers ).collect( Collectors.joining( SEPARATOR ) ) );
                writer.write( LINE_SEPARATOR );

                for ( Edition edition : editions ) {
                    Map<GeneOntologyTerm, Integer> data = cache.getInferredAnnotationCount( edition );
                    if ( data != null ) {
                        Map<GeneOntologyTerm, Integer> directData = cache.getDirectAnnotationCount( edition );

                        for ( Map.Entry<GeneOntologyTerm, Integer> entry : data.entrySet() ) {
                            writer.write( edition.getEdition().toString() );
                            writer.write( SEPARATOR );
                            writer.write( edition.getDate().toString() );
                            writer.write( SEPARATOR );
                            writer.write( edition.getGoEdition().getDate().toString() );
                            writer.write( SEPARATOR );

                            writer.write( entry.getKey().getGoId() );
                            writer.write( SEPARATOR );
                            writer.write( directData.get( entry.getKey() ).toString() );
                            writer.write( SEPARATOR );
                            writer.write( entry.getValue().toString() );
                            writer.write( LINE_SEPARATOR );
                        }
                    }

                }

                writer.flush();
                zip.close();
                writer.close();
            };

            return Response.ok( stream )
                    .header( "Content-Disposition", "attachment; filename=" + filename + ".tsv.gz" )
                    .build();

        } catch (Exception ex) {
            log.warn( "error", ex );
            JSONObject response = new JSONObject();
            response.put( "error", ex );
            return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
        }

    }

    @GET
    @Path("/species/{speciesId}/annotations")
    public Response fetchEditionLevelAnnotations( @PathParam("speciesId") Integer speciesId, @QueryParam("edition") Integer editionId,
                                                  @QueryParam("erminej") @DefaultValue("false") Boolean erminej ) {

        try {

            Species species = cache.getSpecies( speciesId );


            if ( species == null ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Unknown Species ID" );
                return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            String filename = "annotations." + species.getCommonName();

            final List<Edition> editions = selectEditions( species, editionId );

            if ( editions.isEmpty() ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Cannot find edition(s)" );
                return Response.status( 204 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            } else if ( editions.size() == 1 ) {
                Edition ed = editions.get( 0 );
                filename += "." + ed.getEdition() + "-" + ed.getDate();
            }

            boolean isMultipleEditions = editions.size() > 1;

            if ( isMultipleEditions && erminej ) {
                JSONObject response = new JSONObject();
                response.put( "error", "Multiple editions not allowed for ermineJ compatible file." );
                return Response.status( 405 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            } else if ( erminej ) {
                filename += ".erminej";
            }

            if ( isMultipleEditions ) {
                // Temporarily disallowed
                JSONObject response = new JSONObject();
                response.put( "error", "Multiple editions temporarily disallowed. Please use '?edition=<id>'." );
                return Response.status( 403 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
            }

            Collections.sort( editions );

            String[] headers;

            if ( isMultipleEditions ) {
                headers = new String[]{
                        "edition",
                        "date",
                        "go_date",
                        "accession",
                        "symbol",
                        "name",
                        "go_ids",
                };
            } else if ( erminej ) {
                headers = new String[]{
                        "symbol",
                        "symbol",
                        "name",
                        "go_ids",
                };
            } else {
                headers = new String[]{
                        "accession",
                        "symbol",
                        "name",
                        "go_ids",
                };
            }

            StreamingOutput stream = os -> {
                GZIPOutputStream zip = new GZIPOutputStream( os );
                Writer writer = new BufferedWriter( new OutputStreamWriter( zip, "UTF-8" ) );

                writer.write( Arrays.stream( headers ).collect( Collectors.joining( SEPARATOR ) ) );
                writer.write( LINE_SEPARATOR );

                for ( Edition edition : editions ) {

                    Stream<Tuples.Tuple2<Gene, GeneOntologyTerm>> dataStream = annotationService.streamEditionSimple( species, edition );
                    Map<Gene, Set<GeneOntologyTerm>> data;
                    if ( erminej ) {
                        // Make sure symbols are unique and merge sets on conflict
                        // Collect to Map<String, ...> revert to Map<Gene, ...> with merged term sets.
                        data = dataStream.filter( t -> !t.getT1().getSymbol().equals( "" ) ).collect( Collectors.groupingBy( t -> t.getT1().getSymbol() ) ).entrySet().stream()
                                .collect( Collectors.toMap(
                                        e -> e.getValue().iterator().next().getT1(),
                                        e -> e.getValue().stream().map( Tuples.Tuple2::getT2 )
                                                .collect( Collectors.toSet() ) ) );
                    } else {
                        data = dataStream.collect( Collectors.groupingBy( Tuples.Tuple2::getT1, Collectors.mapping( Tuples.Tuple2::getT2, Collectors.toSet() ) ) );
                    }


                    if ( data != null ) {

                        for ( Map.Entry<Gene, Set<GeneOntologyTerm>> geneEntry : data.entrySet() ) {
                            Gene gene = geneEntry.getKey();

                            if ( isMultipleEditions ) {
                                writer.write( edition.getEdition().toString() );
                                writer.write( SEPARATOR );
                                writer.write( edition.getDate().toString() );
                                writer.write( SEPARATOR );
                                writer.write( edition.getGoEdition().getDate().toString() );
                                writer.write( SEPARATOR );
                            }

                            writer.write( erminej ? gene.getSymbol() : gene.getAccession().getAccession() );
                            writer.write( SEPARATOR );

                            writer.write( gene.getSymbol() );
                            writer.write( SEPARATOR );
                            writer.write( gene.getName() );
                            writer.write( SEPARATOR );
                            writer.write( geneEntry.getValue().stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.joining( "|" ) ) );
                            writer.write( LINE_SEPARATOR );
                        }
                    }
                }
                writer.flush();
                zip.close();
                writer.close();
            };

            return Response.ok( stream )
                    .header( "Content-Disposition", "attachment; filename=" + filename + ".tsv.gz" )
                    .build();

        } catch (Exception ex) {
            log.warn( "error", ex );
            JSONObject response = new JSONObject();
            response.put( "error", ex.getMessage() );
            return Response.status( 400 ).entity( response.toString() ).type( MediaType.APPLICATION_JSON ).build();
        }

    }

    private List<Edition> selectEditions( Species species, Integer editionId ) {
        if ( editionId == null ) {
            return Lists.newArrayList( cache.getAllEditions( species ) );
        }

        Edition ed = cache.getEdition( species, editionId );

        if ( ed == null ) {
            return Lists.newArrayList();
        }

        return Lists.newArrayList( ed );
    }

}
