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

package ubc.pavlab.gotrack.handlers;

import java.net.URI;
import java.net.URISyntaxException;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class ReverseProxyViewHandler extends ViewHandlerWrapper {

    private static final Logger log = Logger.getLogger( ReverseProxyViewHandler.class );

    private ViewHandler wrapped;

    public ReverseProxyViewHandler( ViewHandler wrapped ) {
        this.wrapped = wrapped;
    }

    @Override
    public String getActionURL( final FacesContext context, final String viewId ) {
        //log.info( "Wrapped getActionURL" );
        return getRelativeURL( context, this.wrapped.getActionURL( context, viewId ) );
    }

    @Override
    public String getResourceURL( final FacesContext context, final String path ) {
        //log.info( "Wrapped getResourceURL" );
        return getRelativeURL( context, this.wrapped.getResourceURL( context, path ) );
    }

    @Override
    public ViewHandler getWrapped() {
        return wrapped;
    }

    /**
     * Transform the given URL to a relative URL <b>in the context of the current
     * faces request</b>. If the given URL is not absolute do nothing and return
     * the given url. The returned relative URL is "equal" to the original url but
     * will not start with a '/'. So the browser can request the "same" resource
     * but in a relative way and this is important behind reverse proxies!
     *
     * @param context
     * @param theURL
     * @return
     */
    private String getRelativeURL( final FacesContext context, final String theURL ) {
        final HttpServletRequest request = ( ( HttpServletRequest ) context.getExternalContext().getRequest() );
        String result = theURL;
        if ( theURL.startsWith( "/" ) ) {
            int subpath = StringUtils.countMatches( getPath( request ), "/" ) - 2;
            String pathPrefix = "";
            if ( subpath > 0 ) {
                while ( subpath > 0 ) {
                    pathPrefix += "/..";
                    subpath--;
                }

            }
            result = StringUtils.removeStart( result, "/gotrack" );
            pathPrefix = StringUtils.removeStart( pathPrefix, "/" );
            result = pathPrefix + result;
        }

        return result;
    }

    /**
     * Get the url-path from the given request.
     *
     * @param request
     * @return clean path
     */
    private String getPath( final HttpServletRequest request ) {
        try {
            // TODO handle more than two '/'
            return StringUtils.replace( new URI( request.getRequestURI() ).getPath(), "//", "/" );
        } catch ( final URISyntaxException e ) {
            // XXX URISyntaxException ignored
            return StringUtils.EMPTY;
        }
    }
}