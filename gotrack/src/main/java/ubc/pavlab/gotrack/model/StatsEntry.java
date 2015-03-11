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
 * TODO Immutable entry of aggregate table
 * 
 * @author mjacobson
 * @version $Id$
 */
public class StatsEntry {

    private final Integer uniqueAccesions;
    private final Double averageDirects;

    public StatsEntry( Integer uniqueAccesions, Double averageDirects ) {
        super();
        this.uniqueAccesions = uniqueAccesions;
        this.averageDirects = averageDirects;
    }

    public Integer getUniqueAccesions() {
        return uniqueAccesions;
    }

    public Double getAverageDirects() {
        return averageDirects;
    }

    @Override
    public String toString() {
        return "StatsEntry [uniqueAccesions=" + uniqueAccesions + ", averageDirects=" + averageDirects + "]";
    }

}
