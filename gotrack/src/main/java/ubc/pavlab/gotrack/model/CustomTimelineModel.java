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

import org.primefaces.extensions.model.timeline.TimelineModel;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class CustomTimelineModel<T> extends TimelineModel {

    /**
     * 
     */
    private static final long serialVersionUID = -2774559777621029334L;

    private final T title;

    public CustomTimelineModel( T title ) {
        super();
        this.title = title;
    }

    public T getTitle() {
        return title;
    }

}
