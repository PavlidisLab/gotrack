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

package ubc.pavlab.gotrack.model;

import java.util.List;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AnnotationDetailed {

    private String accession;
    private Integer edition;
    private String goId;
    private Integer species;
    private String symbol;
    private String reference;
    private String evidence;
    private String name;
    private List<String> synonyms;
    private String type;
    private Integer taxon;
    private Integer interactingTaxon;

    /**
     * 
     */
    public AnnotationDetailed() {
    }

    public AnnotationDetailed( String accession, Integer edition, String goId, Integer species, String symbol,
            String reference, String evidence, String name, List<String> synonyms, String type, Integer taxon,
            Integer interactingTaxon ) {
        super();
        this.accession = accession;
        this.edition = edition;
        this.goId = goId;
        this.species = species;
        this.symbol = symbol;
        this.reference = reference;
        this.evidence = evidence;
        this.name = name;
        this.synonyms = synonyms;
        this.type = type;
        this.taxon = taxon;
        this.interactingTaxon = interactingTaxon;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public Integer getEdition() {
        return edition;
    }

    public void setEdition( Integer edition ) {
        this.edition = edition;
    }

    public String getGoId() {
        return goId;
    }

    public void setGoId( String goId ) {
        this.goId = goId;
    }

    public Integer getSpecies() {
        return species;
    }

    public void setSpecies( Integer species ) {
        this.species = species;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol( String symbol ) {
        this.symbol = symbol;
    }

    public String getReference() {
        return reference;
    }

    public void setReference( String reference ) {
        this.reference = reference;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence( String evidence ) {
        this.evidence = evidence;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms( List<String> synonyms ) {
        this.synonyms = synonyms;
    }

    public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public Integer getTaxon() {
        return taxon;
    }

    public void setTaxon( Integer taxon ) {
        this.taxon = taxon;
    }

    public Integer getInteractingTaxon() {
        return interactingTaxon;
    }

    public void setInteractingTaxon( Integer interactingTaxon ) {
        this.interactingTaxon = interactingTaxon;
    }

    @Override
    public String toString() {
        return "AnnotationDetailed [accession=" + accession + ", edition=" + edition + ", goId=" + goId + ", species="
                + species + ", symbol=" + symbol + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( accession == null ) ? 0 : accession.hashCode() );
        result = prime * result + ( ( edition == null ) ? 0 : edition.hashCode() );
        result = prime * result + ( ( evidence == null ) ? 0 : evidence.hashCode() );
        result = prime * result + ( ( goId == null ) ? 0 : goId.hashCode() );
        result = prime * result + ( ( reference == null ) ? 0 : reference.hashCode() );
        result = prime * result + ( ( species == null ) ? 0 : species.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        AnnotationDetailed other = ( AnnotationDetailed ) obj;
        if ( accession == null ) {
            if ( other.accession != null ) return false;
        } else if ( !accession.equals( other.accession ) ) return false;
        if ( edition == null ) {
            if ( other.edition != null ) return false;
        } else if ( !edition.equals( other.edition ) ) return false;
        if ( evidence == null ) {
            if ( other.evidence != null ) return false;
        } else if ( !evidence.equals( other.evidence ) ) return false;
        if ( goId == null ) {
            if ( other.goId != null ) return false;
        } else if ( !goId.equals( other.goId ) ) return false;
        if ( reference == null ) {
            if ( other.reference != null ) return false;
        } else if ( !reference.equals( other.reference ) ) return false;
        if ( species == null ) {
            if ( other.species != null ) return false;
        } else if ( !species.equals( other.species ) ) return false;
        return true;
    }

}
