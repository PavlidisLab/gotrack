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

package ubc.pavlab.gotrack.model.dto;

/**
 * Data Transfer Object for {@link Annotation}
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class AnnotationDTO {
    private final Integer edition;
    private final String goId;
    private final String qualifier;
    private final String evidence;
    private final String reference;

    public AnnotationDTO( Integer edition, String goId, String qualifier, String evidence, String reference ) {
        super();
        this.edition = edition;
        this.goId = goId;
        this.qualifier = qualifier;
        this.evidence = evidence;
        this.reference = reference;
    }

    public Integer getEdition() {
        return edition;
    }

    public String getGoId() {
        return goId;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getReference() {
        return reference;
    }
}
