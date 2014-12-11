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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.NavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

import ubc.pavlab.gotrack.dao.AnnotationDAO;
import ubc.pavlab.gotrack.model.Accession;
import ubc.pavlab.gotrack.model.Edition;
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
    private Edition currentEdition;

    /**
     * 
     */
    public TrackView() {
        System.out.println( "TrackView created" );
    }

    public void init() {
        System.out.println( "TrackView init: " + currentSpecies + ": " + query );
        Collection<Accession> primaryAccessions = cache.getSymbolToCurrentAccessions().get( currentSpecies )
                .get( query );
        if ( primaryAccessions == null ) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
            navigationHandler.handleNavigation( facesContext, null, "error400?faces-redirect=true" );
        } else {
            // Get secondary accessions
            Map<String, Collection<String>> primaryToSecondary = new HashMap<String, Collection<String>>();
            for ( Accession accession : primaryAccessions ) {
                primaryToSecondary.put( accession.getAccession(), accession.getSecondary() );
            }

            // Obtain AnnotationDAO.
            annotationDAO = daoFactoryBean.getGotrack().getAnnotationDAO();
            currentEdition = cache.getCurrentEditions().get( currentSpecies );
            Map<Integer, List<TrackValue>> tvsMap = annotationDAO.trackCounts( currentSpecies, primaryToSecondary );
            List<TrackValue> filteredTrackValues = new ArrayList<TrackValue>();

            for ( List<TrackValue> tvs : tvsMap.values() ) {
                // for ( Iterator<TrackValue> iterator = tvs.iterator(); iterator.hasNext(); ) {
                // TrackValue trackValue = iterator.next();
                // String acc = trackValue.getAccession();
                // for ( Accession primaryAcc : primaryAccessions ) {
                // if ( primaryAcc.getAccession().equals( acc ) ) {
                // // Is primary
                // System.out.println( "primary: " + primaryAcc.getAccession() );
                // break;
                // } else if ( primaryAcc.getSecondary().contains( acc ) ) {
                // // Is secondary
                // // add values to primary and delete
                // System.out.println( "secondary: " + acc + " primary: " + primaryAcc.getAccession() );
                // Integer direct = trackValue.getDirectAnnotations();
                //
                // for ( TrackValue trackValue2 : tvs ) {
                // if ( primaryAcc.getAccession().equals( trackValue2.getAccession() ) ) {
                // // Found primary trackValue, add directs
                // trackValue2.setDirectAnnotations( trackValue2.getDirectAnnotations() + direct );
                // break;
                // }
                // }
                //
                // iterator.remove();
                //
                // break;
                // }
                // }
                //
                // }

                filteredTrackValues.addAll( tvs );

            }

            dateModel = createDateModel( filteredTrackValues );
        }

    }

    private LineChartModel createDateModel( Collection<TrackValue> trackValues ) {
        LineChartModel dateModel = new LineChartModel();
        Map<String, LineChartSeries> allSeries = new HashMap<String, LineChartSeries>();
        for ( TrackValue tv : trackValues ) {
            String acc = tv.getAccession();
            LineChartSeries s = allSeries.get( acc );
            if ( s == null ) {
                s = new LineChartSeries();
                s.setLabel( acc );
                s.setMarkerStyle( "filledDiamond" );
                s.set( tv.getDate().toString(), tv.getDirectAnnotations() );
                allSeries.put( acc, s );
            } else {
                s.set( tv.getDate().toString(), tv.getDirectAnnotations() );
            }

        }

        for ( LineChartSeries series : allSeries.values() ) {
            dateModel.addSeries( series );
        }

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
        axis.setTickAngle( -50 );
        axis.setMax( currentEdition.getDate().toString() );
        axis.setTickFormat( "%b %#d, %y" );

        dateModel.getAxes().put( AxisType.X, axis );
        return dateModel;
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

    public void setDaoFactoryBean( DAOFactoryBean daoFactoryBean ) {
        this.daoFactoryBean = daoFactoryBean;
    }

}
