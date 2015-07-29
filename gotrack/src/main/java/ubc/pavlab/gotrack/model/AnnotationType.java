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

/**
 * Represents a type of annotation.
 * 
 * Direct means the annotation was made directly between the given Term and Gene.
 * 
 * Inferred means the annotation was made to some child of the given Term and was propagated to be applied to this Gene.
 * 
 * @author mjacobson
 * @version $Id$
 */
public enum AnnotationType {
    DIRECT, INFERRED;
}
