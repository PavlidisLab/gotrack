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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ubc.pavlab.gotrack.model.dto.EvidenceDTO;

/**
 * Represents an evidence code to be applied to an annotation
 * 
 * @author mjacobson
 * @version $Id$
 */
@Getter
@ToString
@EqualsAndHashCode(of={"id"})
public final class Evidence {
    private final int id;
    private final String evidence;
    private final String description;
    private final String  category;
    private final boolean curated;

    public Evidence( EvidenceDTO dto ) {
        super();
        this.id = dto.getId();
        this.evidence = dto.getEvidence();
        this.description = dto.getDescription();
        this.category = dto.getCategory();
        this.curated = dto.getCurated();
    }

}
