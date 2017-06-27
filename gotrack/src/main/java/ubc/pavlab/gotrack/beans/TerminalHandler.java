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

import org.apache.log4j.Logger;
import ubc.pavlab.gotrack.model.Aggregate;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Species;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Handled reqests to the terminal (available only in development mode currently)
 * 
 * @author mjacobson
 * @version $Id$
 */
@Named
@RequestScoped
public class TerminalHandler implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3311870883348699626L;
    private static final Logger log = Logger.getLogger( TerminalHandler.class );

    private static final List<String> OPEN_COMMANDS = Arrays.asList( "greet", "date", "genes", "aggregate", "term",
            "auth", "help", "genes" );
    private static final List<String> AUTH_COMMANDS = Arrays.asList( "greet", "date", "genes", "aggregate", "term",
            "auth", "help", "genes", "reload_settings", "stability", "enrich" );

    @Inject
    private SessionManager sessionManager;

    @Inject
    private Cache cache;

    @Inject
    EnrichmentView enrichmentView;

    public TerminalHandler() {
        log.info( "TerminalHandler created" );
        log.info( "Used Memory: " + ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000
                + " MB" );

    }

    @PostConstruct
    public void postConstruct() {
        log.info( "TerminalHandler PostConstruct" );
    }

    /**
     * Handle a command from the terminal
     * 
     * @param command
     * @param params
     * @return
     */
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
        } else if ( command.equals( "aggregate" ) ) {
            if ( params.length == 2 ) {
                Species species;
                try {
                    Integer speciesId = Integer.valueOf( params[0] );
                    species = cache.getSpecies( speciesId );
                } catch ( NumberFormatException e ) {
                    return "Malformed Input : speciesId, edition";
                }
                Integer editionId;
                try {
                    editionId = Integer.valueOf( params[1] );
                } catch ( NumberFormatException e ) {
                    return "Malformed Input : speciesId, edition";
                }
                Edition ed = cache.getEdition( species, editionId );
                Aggregate agg = cache.getAggregates( species, ed );
                if ( agg == null ) {
                    return "Null";
                }
                return agg.toString();
            } else {
                return "Malformed Input : speciesId, edition";
            }
        } else if ( command.equals( "term" ) ) {
            Edition ed;
            String goId;
            Species species;
            if ( params.length > 1 ) {
                try {
                    Integer speciesId = Integer.valueOf( params[0] );
                    species = cache.getSpecies( speciesId );
                } catch ( NumberFormatException e ) {
                    return "Malformed Input : speciesId, term [, GO Edition]";
                }
                if ( species == null ) {
                    return "Unknown species";
                }
                goId = params[1];
                if ( params.length > 2 ) {
                    Integer editionId;
                    try {
                        editionId = Integer.valueOf( params[2] );
                    } catch ( NumberFormatException e ) {
                        return "Malformed Input : speciesId, term [, GO Edition]";
                    }
                    ed = cache.getEdition( species, editionId );
                } else {
                    ed = cache.getCurrentEditions( species );
                }

            } else
                return "Malformed Input : speciesId, term [, GO Edition]";

            GeneOntologyTerm t = cache.getTerm( ed, goId );

            if ( t == null ) {
                return "Term not found.";
            }

            Integer val = cache.getInferredAnnotationCount( species, ed, t );

            return t.toString() + "<br/>" + "GO Set Size: " + ( val == null ? "" : val.toString() );
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

            }
        }

        return command + " not found";
    }

}
