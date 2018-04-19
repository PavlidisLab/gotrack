package ubc.pavlab.gotrack.beans;

import org.omnifaces.cdi.Eager;
import org.primefaces.context.RequestContext;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;
import ubc.pavlab.gotrack.model.visualization.Graph;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collection;

/**
 * Entry points to fetch graph data through EL
 * <p>
 * Created by mjacobson on 04/04/18.
 */
@Named
@Eager
@ApplicationScoped
public class GraphFactory implements Serializable {
    private static final long serialVersionUID = -7316259293351506803L;


    @Inject
    private Cache cache;

    /**
     * Entry point for fetching data necessary to create an ancestry DAG for a term
     */
    public void fetchGraph( GeneOntologyTerm term ) {
        Graph graph = Graph.fromGO( term );
        RequestContext.getCurrentInstance().addCallbackParam( "graph_data", graph.getJsonString() );
    }

    /**
     * Entry point for fetching data necessary to create the current ancestry DAG for a GOId
     */
    public void fetchGraph( String goId ) {
        fetchGraph( cache.getCurrentTerm( goId ) );
    }

    /**
     * Entry point for fetching data necessary to create the an ancestry DAG for a GOId in a GO edition
     */
    public void fetchGraph( String goId, Integer goEdition ) {
        fetchGraph( cache.getTerm( cache.getGOEdition( goEdition ), goId ) );
    }

    public void fetchGraph( Collection<GeneOntologyTerm> terms ) {
        Graph graph = Graph.fromGO( terms );
        RequestContext.getCurrentInstance().addCallbackParam( "graph_data", graph.getJsonString() );
    }

    /**
     * Entry point for fetching data necessary to create the an ancestry DAG comparison for two terms
     */
    public void fetchGraphDiff( GeneOntologyTerm term, GeneOntologyTerm compareTerm ) {
        Graph graph = Graph.fromGODiff( compareTerm, term );
        if ( graph == null ) {
            // Default to standard graph is there's a problem
            graph = Graph.fromGO( term );
        }
        RequestContext.getCurrentInstance().addCallbackParam( "graph_data", graph.getJsonString() );
    }

    /**
     * Entry point for fetching data necessary to create a term's ancestry DAG for a given edition
     * passed as a requestparam
     */
    public void fetchGraph() {

        String goId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "goId" );

        Integer goEditionId = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );

        fetchGraph( goId, goEditionId );

    }

    /**
     * Entry point for fetching data necessary to create a terms' ancestry DAG comparison for
     * given editions passed as a requestparam
     */
    public void fetchGraphDiff() {

        String goId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "goId" );

        Integer goEditionId = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "edition" ) );

        Integer compareGOEditionId = Integer.valueOf(
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get( "compareEdition" ) );


        GeneOntologyTerm term = cache.getTerm( cache.getGOEdition( goEditionId ), goId );
        GeneOntologyTerm compareTerm = cache.getTerm( cache.getGOEdition( compareGOEditionId ), goId );

        fetchGraphDiff( term, compareTerm );

    }

}
