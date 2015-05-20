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

package ubc.pavlab.gotrack.beans;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import ubc.pavlab.gotrack.analysis.EnrichmentAnalysis;
import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityCompareMethod;
import ubc.pavlab.gotrack.analysis.StabilityAnalysis;
import ubc.pavlab.gotrack.analysis.StabilityScore;
import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.Species;

import com.google.common.base.Joiner;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@RequestScoped
public class TerminalHandler implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3311870883348699626L;
    private static final Logger log = Logger.getLogger( TerminalHandler.class );

    private static final List<String> OPEN_COMMANDS = Arrays.asList( "greet", "auth", "help", "genes" );
    private static final List<String> AUTH_COMMANDS = Arrays.asList( "greet", "auth", "help", "genes",
            "reload_settings" );

    // Application settings
    private boolean ontologyInMemory = false;

    private Map<String, StabilityAnalysis> stabilityResults = new HashMap<>();
    private Map<String, Set<String>> missingGenes = new HashMap<>();
    private Map<String, Map<String, Gene>> mappedToSecondary = new HashMap<>();

    @ManagedProperty("#{sessionManager}")
    private SessionManager sessionManager;

    @ManagedProperty("#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{enrichmentView}")
    EnrichmentView enrichmentView;

    private AnnotationDAO annotationDAO;

    public TerminalHandler() {
        log.info( "TerminalHandler created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )
                / 1000000 + " MB" );

    }

    @PostConstruct
    public void postConstruct() {
        log.info( "TerminalHandler PostConstruct" );
        ontologyInMemory = settingsCache.getOntologyInMemory();
    }

    public String handleCommand( String command, String[] params ) {
        if ( command.equals( "greet" ) ) {
            if ( params.length > 0 )
                return "Hello " + params[0];
            else
                return "Hello Stranger";
        } else if ( command.equals( "date" ) ) {
            return new Date().toString();
        } else if ( command.equals( "genes" ) ) {
            return enrichmentView.getSelectedGenes().toString();
        } else if ( command.equals( "help" ) ) {
            return sessionManager.getAuthenticated() ? AUTH_COMMANDS.toString() : OPEN_COMMANDS.toString();
        } else if ( command.equals( "" ) ) {
            return "";
        }

        if ( command.equals( "auth" ) ) {
            if ( sessionManager.getAuthenticated() ) {
                return "Already authenticated";
            }
            if ( params.length > 0 ) {
                return sessionManager.authenticate( params[0] );
            } else {
                return "auth requires passphrase";
            }
        }

        if ( sessionManager.getAuthenticated() ) {
            if ( command.equals( "reload_settings" ) ) {
                sessionManager.reloadSettings();
                return "Settings reloaded";

            } else if ( command.equals( "stability" ) ) {
                log.info( System.getProperty( "user.dir" ) );
                StopWatch timer = new StopWatch();
                timer.start();
                if ( params.length != 2 ) {
                    timer.stop();
                    return "Malformed Input : inputFile outputFolder";
                }

                String inputFile = params[0];

                BufferedReader bReader;
                try {
                    bReader = new BufferedReader( new FileReader( inputFile ) );
                } catch ( FileNotFoundException e1 ) {
                    timer.stop();
                    return "File Not Found!";
                }

                String outputFolder = params[1];

                SAXReader reader = new SAXReader();
                Document document = null;
                try {
                    document = reader.read( bReader );
                } catch ( DocumentException e ) {
                    timer.stop();
                    return "Error parsing XML!";
                }

                List<? extends Node> genesets = ( List<? extends Node> ) document.selectNodes( "//MSIGDB/GENESET" );

                log.info( "Total Genesets: " + genesets.size() );

                Map<String, Integer> organismMap = new HashMap<>();

                for ( Species sp : cache.getSpeciesList() ) {
                    organismMap.put( sp.getScientificName(), sp.getId() );
                }

                // To join secondary map
                Joiner.MapJoiner joiner = Joiner.on( "," ).withKeyValueSeparator( ":" );

                for ( Node node : genesets ) {
                    Element element = ( Element ) node;
                    String organism = element.attributeValue( "ORGANISM" );
                    Integer species = organismMap.get( organism );
                    if ( species == null ) {
                        log.warn( "Unsupported species (" + organism + ")" );
                        continue;
                    }
                    String standardName = element.attributeValue( "STANDARD_NAME" );
                    String systematicName = element.attributeValue( "SYSTEMATIC_NAME" );
                    String chip = element.attributeValue( "CHIP" );
                    String description = element.attributeValue( "DESCRIPTION_BRIEF" );
                    String[] genes = element.attributeValue( "MEMBERS_SYMBOLIZED" ).split( "," );

                    Set<String> missing = new HashSet<>();

                    Map<String, Gene> secondaryMap = new HashMap<>();

                    Set<Gene> hitList = new HashSet<>();

                    for ( int i = 0; i < genes.length; i++ ) {
                        String geneInput = genes[i];

                        Gene g = cache.getCurrentGene( species, geneInput );

                        if ( g != null ) {
                            hitList.add( g );
                        } else {
                            Set<Gene> gs = cache.getCurrentGeneBySynonym( species, geneInput );
                            if ( gs.size() == 1 ) {
                                g = gs.iterator().next();
                                hitList.add( g );
                                secondaryMap.put( geneInput, g );
                            } else {
                                missing.add( geneInput );
                            }

                        }
                    }

                    StabilityAnalysis sa = enrich( hitList, species );

                    String outputFile = outputFolder + "/" + systematicName + "_results.txt";

                    PrintWriter writer = null;
                    try {
                        writer = new PrintWriter( outputFile, "UTF-8" );

                        writer.println( "Edition\tDate\tCompleteTermJaccard\tTopTermJaccard\tTopGeneJaccard" );
                        for ( Entry<Edition, StabilityScore> editionEntry : sa.getStabilityScores().entrySet() ) {
                            Edition ed = editionEntry.getKey();
                            StabilityScore score = editionEntry.getValue();
                            writer.println( ed.getEdition() + "\t" + ed.getDate() + "\t"
                                    + score.getCompleteTermJaccard() + "\t" + score.getTopTermJaccard() + "\t"
                                    + score.getTopGeneJaccard() );
                        }
                        writer.close();

                        outputFile = outputFolder + "/" + systematicName + "_info.txt";
                        writer = new PrintWriter( outputFile, "UTF-8" );

                        writer.println( "STANDARD_NAME: " + standardName );
                        writer.println( "SYSTEMATIC_NAME: " + systematicName );
                        writer.println( "ORGANISM: " + organism + "|" + species );
                        writer.println( "CHIP: " + chip );
                        writer.println( "DESCRIPTION: " + description );
                        writer.println( "SIZE: " + genes.length );
                        writer.println( "GENES: " + Arrays.toString( genes ) );
                        writer.println( "MISSING: " + missing );
                        writer.println( "MAPPED_TO_SECONDARY: " + joiner.join( secondaryMap ) );

                        writer.close();

                    } catch ( FileNotFoundException | UnsupportedEncodingException e ) {
                        log.error( systematicName + " failed to write" );
                        log.error( e );
                    } finally {
                        if ( writer != null ) {
                            writer.close();
                        }
                    }

                    log.info( systematicName + " -- " + timer );

                }

                timer.stop();
                return "Completed in " + timer;

            } else if ( command.equals( "enrich" ) ) {
                String returnString = "";
                StopWatch timer = new StopWatch();
                timer.start();
                if ( params.length == 0 ) {

                    enrich( new HashSet<>( enrichmentView.getSelectedGenes() ), enrichmentView.getCurrentSpeciesId() );

                } else if ( params.length > 2 ) {

                    int currentSpeciesId;
                    try {
                        currentSpeciesId = Integer.parseInt( params[0] );
                    } catch ( NumberFormatException e ) {
                        timer.stop();
                        return "Malformed Input : First parameter should be species Id.";
                    }

                    Set<Gene> hitList = new HashSet<>();

                    for ( int i = 1; i < params.length; i++ ) {
                        String geneInput = params[i];

                        Gene g = cache.getCurrentGene( currentSpeciesId, geneInput );

                        if ( g != null ) {
                            hitList.add( g );
                        } else {
                            returnString += "Could not find " + geneInput + "<br/>";
                        }
                    }

                    enrich( hitList, currentSpeciesId );
                } else {
                    timer.stop();
                    return "Malformed Input : speciesId gene1 gene2 ... OR speciesId path/to/hitlists";
                }
                timer.stop();

                return returnString += "Completed in " + timer;
            }
        }

        return command + " not found";
    }

    public StabilityAnalysis enrich( Set<Gene> genes, Integer currentSpeciesId ) {
        if ( annotationDAO == null ) {
            annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();
        }

        Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = retrieveData( genes, currentSpeciesId );

        if ( geneGOMap == null ) {
            return null;
        }

        Map<Edition, Integer> sampleSizes = annotationDAO.enrichmentSampleSizes( currentSpeciesId, genes );
        log.info( "retrieving sample sizes..." );

        EnrichmentAnalysis analysis = new EnrichmentAnalysis( geneGOMap, sampleSizes, 5, 200,
                MultipleTestCorrection.BH, 0.05, cache, currentSpeciesId );
        log.info( "Similarity Analysis" );

        return new StabilityAnalysis( analysis, 5, SimilarityCompareMethod.CURRENT );

    }

    private Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> retrieveData( Set<Gene> genes, Integer currentSpeciesId ) {
        if ( genes != null && !genes.isEmpty() ) {
            log.info( "Current species: " + currentSpeciesId );
            log.info( "Geneset Size: " + genes.size() );

            log.info( "retrieving gene data..." );

            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> geneGOMap = new HashMap<>();
            Set<Gene> genesToLoad = new HashSet<>();
            for ( Gene gene : genes ) {
                Map<Edition, Set<GeneOntologyTerm>> cachedGeneData = cache.getEnrichmentData( gene );
                if ( cachedGeneData != null ) {
                    addGeneData( gene, cachedGeneData, geneGOMap );
                } else {
                    genesToLoad.add( gene );
                }
            }

            if ( !genesToLoad.isEmpty() ) {

                Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB;

                if ( ontologyInMemory ) {
                    geneGOMapFromDB = annotationDAO.enrichmentDataNoPropagateNoTermInfo( currentSpeciesId, genesToLoad );
                    for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : propagate( geneGOMapFromDB )
                            .entrySet() ) {
                        addGeneData( geneEntry.getKey(), geneEntry.getValue(), geneGOMap );
                        cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
                    }

                } else {
                    geneGOMapFromDB = annotationDAO.enrichmentDataPropagateNoTermInfo( currentSpeciesId, genesToLoad );

                    for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : geneGOMapFromDB.entrySet() ) {
                        addGeneData( geneEntry.getKey(), geneEntry.getValue(), geneGOMap );
                        cache.addEnrichmentData( geneEntry.getKey(), geneEntry.getValue() );
                    }
                }

                log.info( "Retrieved (" + genesToLoad.size() + ") genes from db and ("
                        + ( genes.size() - genesToLoad.size() ) + ") from cache" );
            } else {
                log.info( "Retrieved all (" + genes.size() + ") genes from cache" );

            }

            return geneGOMap;

        } else {
            log.info( "Empty geneset" );
            return null;
        }
    }

    private void addGeneData( Gene g, Map<Edition, Set<GeneOntologyTerm>> cachedGeneData,
            Map<Edition, Map<GeneOntologyTerm, Set<Gene>>> data ) {

        for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : cachedGeneData.entrySet() ) {
            Edition ed = editionEntry.getKey();

            Map<GeneOntologyTerm, Set<Gene>> m1 = data.get( ed );

            if ( m1 == null ) {
                // Previously had no data for this edition
                m1 = new HashMap<>();
                data.put( ed, m1 );
            }

            for ( GeneOntologyTerm term : editionEntry.getValue() ) {
                Set<Gene> geneSet = m1.get( term );

                if ( geneSet == null ) {
                    // Previously had no genes for this term in this edition
                    geneSet = new HashSet<>();
                    m1.put( term, geneSet );
                }

                geneSet.add( g );

            }

        }
    }

    private Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> propagate(
            Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneGOMapFromDB ) {
        Map<Gene, Map<Edition, Set<GeneOntologyTerm>>> propagatedData = new HashMap<>();
        for ( Entry<Gene, Map<Edition, Set<GeneOntologyTerm>>> geneEntry : geneGOMapFromDB.entrySet() ) {
            Gene g = geneEntry.getKey();
            Map<Edition, Set<GeneOntologyTerm>> propagatedSeries = new HashMap<>();
            propagatedData.put( g, propagatedSeries );
            Map<Edition, Set<GeneOntologyTerm>> series = geneEntry.getValue();
            for ( Entry<Edition, Set<GeneOntologyTerm>> editionEntry : series.entrySet() ) {
                Edition ed = editionEntry.getKey();
                Set<GeneOntologyTerm> propagatedTerms = cache.propagate( editionEntry.getValue(), ed.getGoEditionId() );

                if ( propagatedTerms == null ) {
                    // No ontology exists for this edition
                } else {
                    propagatedSeries.put( ed, propagatedTerms );
                }

            }

        }
        return propagatedData;
    }

    public void setSessionManager( SessionManager sessionManager ) {
        this.sessionManager = sessionManager;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

    public void setEnrichmentView( EnrichmentView enrichmentView ) {
        this.enrichmentView = enrichmentView;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

}
