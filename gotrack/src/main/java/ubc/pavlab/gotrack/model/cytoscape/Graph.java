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

package ubc.pavlab.gotrack.model.cytoscape;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Graph {

    private final Collection<Node> nodes;
    private final Collection<Edge> edges;

    public Graph( Collection<Node> nodes, Collection<Edge> edges ) {
        super();
        this.nodes = nodes;
        this.edges = edges;
    }

    public Graph() {
        this( new LinkedHashSet<Node>(), new LinkedHashSet<Edge>() );
    }

    public Collection<Node> getNodes() {
        return nodes;
    }

    public Collection<Edge> getEdges() {
        return edges;
    }

}
