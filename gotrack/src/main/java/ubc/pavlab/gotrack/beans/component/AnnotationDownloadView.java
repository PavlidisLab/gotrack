/*
 * The gotrack project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubc.pavlab.gotrack.beans.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.beans.SessionManager;
import ubc.pavlab.gotrack.beans.service.AnnotationService;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Backing bean for a annotation download input
 * Deprecated for downloads page.
 * @author mjacobson
 */
@Named
@ViewScoped
public class AnnotationDownloadView implements Serializable {
    private static final long serialVersionUID = -3780317482652691524L;

    private static final Logger log = Logger.getLogger( AnnotationDownloadView.class );

    private Integer year = null;
    private Edition edition = null;

    private List<Edition> filteredEditions = Lists.newArrayList();
    private StreamedContent file = null;

    @Inject
    private Cache cache;

    @Inject
    private AnnotationService annotationService;

    @Inject
    private SessionManager session;


    public AnnotationDownloadView() {
        log.info( "AnnotationDownloadView created" );
    }

    @PostConstruct
    public void init() {
        log.info( "AnnotationDownloadView init" );
        edition = cache.getCurrentEditions( session.getSpecies() );
        Calendar cal = Calendar.getInstance();
        cal.setTime( edition.getDate() );
        year = cal.get( Calendar.YEAR );
        filterEditions();
    }

    public ImmutableList<Integer> getAllYears() {
        return cache.getSpeciesYears( session.getSpecies() );
    }

    public Integer getYear() {
        return year;
    }

    public void setYear( Integer year ) {
        this.year = year;
    }

    public Edition getEdition() {
        return edition;
    }

    public void setEdition( Edition edition ) {
        this.edition = edition;
    }

    public void filterEditions() {
        Calendar calendar = Calendar.getInstance();
        filteredEditions = cache.getAllEditions( session.getSpecies() ).stream().filter( ed -> {
            calendar.setTime( ed.getDate() );
            return calendar.get( Calendar.YEAR ) == year;
        } ).sorted().collect( Collectors.toList() );
    }

    public List<Edition> getFilteredEditions(){
        return filteredEditions;
    }

    private void loadFile() {
        // TODO: Cache files to disk
        Map<Gene, Set<GeneOntologyTerm>> data =  annotationService.fetchEditionSimple( session.getSpecies(), edition );
        final StringBuilder sb = new StringBuilder();
        for ( Map.Entry<Gene, Set<GeneOntologyTerm>> geneEntry: data.entrySet()) {
            Gene gene = geneEntry.getKey();
            sb.append( gene.getSymbol() );
            sb.append( "\t" );
            sb.append( gene.getSymbol() );
            sb.append( "\t" );
            sb.append( gene.getName() );
            sb.append( "\t" );

            sb.append( GeneOntologyTerm.propagate( geneEntry.getValue().stream() ).map( GeneOntologyTerm::getGoId ).collect( Collectors.joining( "|" ) ) );
            sb.append( System.lineSeparator() );
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes( StandardCharsets.UTF_8));
        log.info( "Finished loading file." );
        String filename = session.getSpecies().getCommonName() + "-" + edition.getEdition() + "-" + edition.getDate().toString() + ".tsv";
        file = new DefaultStreamedContent( in, "text/plain", filename );
    }

    public StreamedContent downloadFile() {
        log.info( "downloadFile" );
        loadFile();
        return file;

    }
}
