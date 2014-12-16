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

package ubc.pavlab.gotrack.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.NavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.TrackValue;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class TrackView implements Serializable {

    @ManagedProperty("#{cache}")
    private Cache cache;

    @ManagedProperty("#{daoFactoryBean}")
    private DAOFactoryBean daoFactoryBean;

    /**
     * 
     */
    private static final long serialVersionUID = 7413572739210227872L;
    private Integer currentSpecies;
    private String query;
    private AnnotationDAO annotationDAO;
    // private List<TrackValue> trackValues;

    private LineChartModel dateModel;
    private Map<Integer, Map<String, TrackValue>> seriesData;
    private Edition currentEdition;
    private Map<String, Accession> currentPrimaryAccessions = new HashMap<String, Accession>();

    private TrackValue selectedItem;
    private Collection<GeneOntologyTerm> terms = new ArrayList<GeneOntologyTerm>();
    private Collection<GeneOntologyTerm> filteredTerms;
    private Set<String> codes = new HashSet<String>();

    /**
     * 
     */
    public TrackView() {
        System.out.println( "TrackView created" );
    }

    public void init() {
        if ( FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest() ) {
            return; // Skip ajax requests.
        }
        System.out.println( "TrackView init: " + currentSpecies + ": " + query );
        Map<String, Collection<Accession>> c = cache.getSymbolToCurrentAccessions().get( currentSpecies );
        Collection<Accession> primaryAccessions;
        if ( query == null
                || currentSpecies == null
                || c == null
                || ( primaryAccessions = cache.getSymbolToCurrentAccessions().get( currentSpecies ).get( query ) ) == null ) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
            navigationHandler.handleNavigation( facesContext, null, "error400?faces-redirect=true" );
        } else {
            // Get secondary accessions
            Map<String, Collection<String>> primaryToSecondary = new HashMap<String, Collection<String>>();
            for ( Accession accession : primaryAccessions ) {
                primaryToSecondary.put( accession.getAccession(), accession.getSecondary() );
                currentPrimaryAccessions.put( accession.getAccession(), accession );
            }

            // Obtain AnnotationDAO.
            annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();
            currentEdition = cache.getCurrentEditions().get( currentSpecies );
            Map<String, List<TrackValue>> tvsMap = annotationDAO.trackCounts( currentSpecies, primaryToSecondary );

            dateModel = createDateModel( tvsMap );
        }

    }

    private LineChartModel createDateModel( Map<String, List<TrackValue>> tvsMap ) {
        LineChartModel dateModel = new LineChartModel();
        seriesData = new HashMap<Integer, Map<String, TrackValue>>();
        Integer index = 0;
        for ( Entry<String, List<TrackValue>> es : tvsMap.entrySet() ) {
            String primary = es.getKey();
            List<TrackValue> tvs = es.getValue();
            Collections.sort( tvs, new CustomComparator() );
            LineChartSeries s = new LineChartSeries();
            s.setLabel( primary );
            s.setMarkerStyle( "filledDiamond" );

            Map<String, TrackValue> data = new HashMap<String, TrackValue>();
            seriesData.put( index++, data );
            for ( TrackValue tv : tvs ) {
                s.set( tv.getDate().toString(), tv.getDirectAnnotations() );
                data.put( tv.getDate().toString(), tv );
                // s.set( new Edition(tv.getEdition(), tv.getDate()), tv.getDirectAnnotations() );
            }
            dateModel.addSeries( s );
        }

        // Map<String, LineChartSeries> allSeries = new HashMap<String, LineChartSeries>();
        // for ( TrackValue tv : trackValues ) {
        // String acc = tv.getAccession();
        // LineChartSeries s = allSeries.get( acc );
        // if ( s == null ) {
        // s = new LineChartSeries();
        // s.setLabel( acc );
        // s.setMarkerStyle( "filledDiamond" );
        // s.set( tv.getDate().toString(), tv.getDirectAnnotations() );
        // allSeries.put( acc, s );
        // } else {
        // s.set( tv.getDate().toString(), tv.getDirectAnnotations() );
        // }
        //
        // }
        //
        // for ( LineChartSeries series : allSeries.values() ) {
        // dateModel.addSeries( series );
        // }

        dateModel.setTitle( "Changes in the number of annotation properties over time" );
        dateModel.setZoom( true );

        dateModel.setLegendPosition( "nw" );
        // dateModel.setAnimate( true );
        dateModel.setLegendRows( 8 );
        dateModel.setMouseoverHighlight( true );
        dateModel.setExtender( "chartExtender" );

        dateModel.getAxis( AxisType.Y ).setLabel( "Direct Annotations" );
        dateModel.getAxis( AxisType.Y ).setMin( 0 );
        DateAxis axis = new DateAxis( "Dates" );
        // CategoryAxis axis = new CategoryAxis( "Editions" );
        axis.setTickAngle( -50 );
        axis.setMax( currentEdition.getDate().toString() );
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;
    }

    public void itemSelect( ItemSelectEvent event ) {

        // dateModel.getSeries().get( event.getSeriesIndex() ).getData().get( key );
        List<Entry<Object, Number>> es = new ArrayList<Entry<Object, Number>>( dateModel.getSeries()
                .get( event.getSeriesIndex() ).getData().entrySet() );
        String date = ( String ) es.get( event.getItemIndex() ).getKey();
        selectedItem = seriesData.get( event.getSeriesIndex() ).get( date );

        List<String> accessions = new ArrayList<String>();
        accessions.add( selectedItem.getAccession() );
        accessions.addAll( currentPrimaryAccessions.get( selectedItem.getAccession() ).getSecondary() );
        terms = annotationDAO.findUniqueGO( accessions, selectedItem.getEdition(), currentSpecies );
        // RequestContext.getCurrentInstance().update( "dialogMsg" );
        codes = new HashSet<String>();
        for ( GeneOntologyTerm t : terms ) {
            codes.add( t.getEvidence() );
        }
        // System.out.println( selectedItem.getEdition() );

    }

    public void showDialog() {
        RequestContext.getCurrentInstance().openDialog( "dlg2" );
    }

    public LineChartModel getDateModel() {
        return dateModel;
    }

    // public List<TrackValue> getTrackValues() {
    // return trackValues;
    // }

    public Integer getCurrentSpecies() {
        return currentSpecies;
    }

    public void setCurrentSpecies( Integer currentSpecies ) {
        this.currentSpecies = currentSpecies;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public void setCache( Cache cache ) {
        this.cache = cache;
    }

    public TrackValue getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem( TrackValue selectedItem ) {
        this.selectedItem = selectedItem;
    }

    public Collection<GeneOntologyTerm> getTerms() {
        return terms;
    }

    public Collection<GeneOntologyTerm> getFilteredTerms() {
        return filteredTerms;
    }

    public void setFilteredTerms( Collection<GeneOntologyTerm> filteredTerms ) {
        this.filteredTerms = filteredTerms;
    }

    public Set<String> getCodes() {
        return codes;
    }

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

}

class CustomComparator implements Comparator<TrackValue> {
    @Override
    public int compare( TrackValue o1, TrackValue o2 ) {
        return o1.getEdition().compareTo( o2.getEdition() );
    }
}
