package ubc.pavlab.gotrack.model.hashkey;

import ubc.pavlab.gotrack.model.Edition;
import ubc.pavlab.gotrack.model.Species;
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

    public MultiKey( Species species, Edition ed, GeneOntologyTerm t ) {
        this.species = species.getId();
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
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        MultiKey multiKey = (MultiKey) o;

        if ( species != null ? !species.equals( multiKey.species ) : multiKey.species != null ) return false;
        if ( edition != null ? !edition.equals( multiKey.edition ) : multiKey.edition != null ) return false;
        return goId != null ? goId.equals( multiKey.goId ) : multiKey.goId == null;
    }

    @Override
    public int hashCode() {
        int result = species != null ? species.hashCode() : 0;
        result = 31 * result + (edition != null ? edition.hashCode() : 0);
        result = 31 * result + (goId != null ? goId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MultiKey [species=" + species + ", edition=" + edition + ", goId=" + goId + "]";
    }
}