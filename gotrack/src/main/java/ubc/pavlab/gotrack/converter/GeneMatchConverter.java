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

package ubc.pavlab.gotrack.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import ubc.pavlab.gotrack.beans.Cache;
import ubc.pavlab.gotrack.model.Gene;
import ubc.pavlab.gotrack.model.table.GeneMatches;

/**
 * Converter for {@link GeneMatches}. Complicated getAsString as the use case in confirming multiple genes necessitates
 * it.
 * 
 * @author mjacobson
 * @version $Id$
 */
@FacesConverter("geneMatchConverter")
public class GeneMatchConverter implements Converter {

    @Override
    public Object getAsObject( FacesContext fc, UIComponent uic, String value ) {
        if ( value != null && value.trim().length() > 0 ) {
            try {
                Integer species = ( Integer ) uic.getAttributes().get( "species" );
                Cache cache = ( Cache ) fc.getExternalContext().getApplicationMap().get( "cache" );
                return cache.getCurrentGene( species, value );
            } catch ( NumberFormatException e ) {
                throw new ConverterException( new FacesMessage( FacesMessage.SEVERITY_ERROR,
                        "Conversion Error: getAsObject", "Not a valid gene." ) );
            }
        } else {
            return null;
        }
    }

    @Override
    public String getAsString( FacesContext fc, UIComponent uic, Object object ) {
        if ( object != null ) {
            if ( object instanceof Gene ) {
                return ( ( Gene ) object ).getSymbol();
            } else if ( object instanceof GeneMatches ) {
                return ( ( GeneMatches ) object ).getSelectedGene().getSymbol();
            } else {
                throw new ConverterException( new FacesMessage( FacesMessage.SEVERITY_ERROR,
                        "Conversion Error: getAsString - " + object, "Not a valid gene or genematch." ) );
            }
        } else {
            return null;
        }
    }
}
