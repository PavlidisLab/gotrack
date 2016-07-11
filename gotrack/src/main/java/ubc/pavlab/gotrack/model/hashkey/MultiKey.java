package ubc.pavlab.gotrack.model.hashkey;

import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.dto.AnnotationCountDTO;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

/**
 * Combined key for use in Maps based on species, edition and goId. Most useful if intermediate maps are not
 * required.
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class MultiKey {
    private final Integer species;
    private final Integer edition;
    private final String goId;

    public MultiKey( Integer species, Edition ed, GeneOntologyTerm t ) {
        this.species = species;
        this.edition = ed.getEdition();
        this.goId = t.getGoId();
    }

    public MultiKey( AnnotationCountDTO dto ) {
        this.species = dto.getSpecies();
        this.edition = dto.getEdition();
        this.goId = dto.getGoId();
    }

    public Integer getSpecies() {
        return species;
    }

    public Integer getEdition() {
        return edition;
    }

    public String getGoId() {
        return goId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( edition == null ) ? 0 : edition.hashCode() );
        result = prime * result + ( ( goId == null ) ? 0 : goId.hashCode() );
        result = prime * result + ( ( species == null ) ? 0 : species.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        MultiKey other = ( MultiKey ) obj;
        if ( edition == null ) {
            if ( other.edition != null ) return false;
        } else if ( !edition.equals( other.edition ) ) return false;
        if ( goId == null ) {
            if ( other.goId != null ) return false;
        } else if ( !goId.equals( other.goId ) ) return false;
        if ( species == null ) {
            if ( other.species != null ) return false;
        } else if ( !species.equals( other.species ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MultiKey [species=" + species + ", edition=" + edition + ", goId=" + goId + "]";
    }
}