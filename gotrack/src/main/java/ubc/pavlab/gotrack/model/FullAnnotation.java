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
import lombok.RequiredArgsConstructor;
import ubc.pavlab.gotrack.model.go.GeneOntologyTerm;

@Getter
//@EqualsAndHashCode( exclude = ("direct") ) // Exclude direct as we only want a single term-annotation pair in a set at once
@EqualsAndHashCode
@RequiredArgsConstructor
public class FullAnnotation {
    private final GeneOntologyTerm annotatedTerm;
    private final Annotation annotation;
    private final boolean direct;
}
