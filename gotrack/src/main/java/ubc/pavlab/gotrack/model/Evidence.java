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

package ubc.pavlab.gotrack.model;

import ubc.pavlab.gotrack.model.dto.EvidenceDTO;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class Evidence {
    private final int id;
    private final String evidence;
    private final String description;
    private final String category;

    public Evidence( EvidenceDTO dto ) {
        super();
        this.id = dto.getId();
        this.evidence = dto.getEvidence();
        this.description = dto.getDescription();
        this.category = dto.getCategory();
    }

    public int getId() {
        return id;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "Evidence [id=" + id + ", evidence=" + evidence + ", description=" + description + ", category="
                + category + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Evidence other = ( Evidence ) obj;
        if ( id != other.id ) return false;
        return true;
    }

}
