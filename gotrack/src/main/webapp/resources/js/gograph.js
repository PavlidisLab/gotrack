/**
 * @memberOf GOGraph
 */
(function (gograph, $, undefined) {

    gograph.createNewGraph = function (id, graph_data, callback) {
        // console.log("Creating graph: " + id);
        return new GOGraph(id, graph_data, callback, false);
    };

    gograph.createNewGraphLogo = function (id, graph_data, callback) {
        // console.log("Creating graph logo: " + id);
        return new GOGraph(id, graph_data, callback, true);
    };

    function GOGraph(id, graph_data, callback, is_logo) {
        /*
         * id -> html ID of container. Ex. #dag-graph
         * graph_data -> {"nodes":[
         *                           {"id":71625,"label":"vocalization behavior"},
         *                           {"id":44708,"label":"single-organism behavior"},
         *                           {"id":7610,"label":"behavior"},
         *                           {"id":44699,"label":"single-organism process"},
         *                           {"id":8150,"label":"biological_process"}],
         *                 "edges":[
         *                           {"from":71625,"to":44708,"type":"IS_A"},
         *                           {"from":44708,"to":7610,"type":"IS_A"},
         *                           {"from":44708,"to":44699,"type":"IS_A"},
         *                           {"from":7610,"to":8150,"type":"IS_A"},
         *                           {"from":44699,"to":8150,"type":"IS_A"}]
         *                }
         * callback -> callback function after creation.         *
         *
         * */
        if (isUndefined(callback)) {
            callback = function () {
            };
        }


        var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(function () {
            return {};
        });

        loadGraphData(g, graph_data, is_logo);

        g.nodes().forEach(function (v) {
            var node = g.node(v);
            // Round the corners of the nodes
            node.rx = node.ry = 5;
            if (is_logo) {
                node.width = node.height = 1;
            }
        });

        // Set up an SVG group so that we can translate the final graph.
        var svg = d3.select(id);
        svg.selectAll("*").remove();
        var svgGroup = svg.append("g");

        // Graph options
        g.graph().rankDir = 'BT';
        g.graph().nodesep = 10;
        g.graph().ranksep = 12;
        g.graph().edgesep = 0;

        // g.graph().transition = function(selection) {
        // return selection.transition().duration(1000);
        // };


        // Create get entire path functionality
        g.getEntirePathLinks = function (centerId) {
            // Returns a list containing all edges leading into or from the center node
            var g = this;
            var nodes = {};

            var ancestors = {};

            var explore_parents = function (nid) {
                if (ancestors[nid]) {
                    return;
                }
                ancestors[nid] = {};
                var parents = g.successors(nid);
                for (var i = 0; i < parents.length; i++) {
                    var pid = parents[i];
                    ancestors[nid][pid] = true;
                    explore_parents(pid);
                }

            }

            var descendants = {};

            var explore_children = function (nid) {
                if (descendants[nid]) {
                    return;
                }
                descendants[nid] = {};
                var children = g.predecessors(nid);
                for (var i = 0; i < children.length; i++) {
                    var cid = children[i];
                    descendants[nid][cid] = true;
                    explore_children(cid);
                }

            }

            explore_parents(centerId);
            explore_children(centerId);

            var path = [];

            for (var targetid in ancestors) {
                var sourceids = ancestors[targetid];
                for (var sourceid in sourceids) {
                    path.push(g.nodeEdges(sourceid, targetid)[0]);
                }
            }

            for (var sourceid in descendants) {
                var targetids = descendants[sourceid];
                for (var targetid in targetids) {
                    path.push(g.nodeEdges(sourceid, targetid)[0]);
                }
            }

            return path;
        };

        // Create renderer
        var render = createRenderer(g, svgGroup);

        // Set up zoom support
        var zoom = d3.behavior.zoom().on("zoom", function () {
            if (d3.event.sourceEvent) d3.event.sourceEvent.stopPropagation();
            svgGroup.attr("transform", "translate(" + d3.event.translate + ")" +
                "scale(" + d3.event.scale + ")");
        });
        if (!is_logo) {
            svg.call(zoom);
        }

        // Run the renderer. This is what draws the final graph.
        render(svgGroup, g);

        /**POST RENDER**/

            // tooltips

        if (!is_logo) {
            var nodes = svgGroup.selectAll("g.node");
            // Simple function to style the tooltip for the given node.
            var styleTooltip = function (name, description) {
                return "<p class='name'>" + name + "</p><p class='description'>" + description + "</p>";
            };


            nodes.attr("title", function (v) {
                return styleTooltip(g.node(v).go_id, g.node(v).description)
            });
            nodes.each(function (v) {
                $(this).tipsy({gravity: "w", opacity: 0.8, html: true});
            });

            // events
            nodes.on("click", function (id) {
                if (d3.event.defaultPrevented) return;
                var _node = g.node(id);
                try { // your browser may block popups
                    window.open(_node.href);
                } catch (e) { // fall back on url change
                    window.location.href = _node.href;
                }
            });


            var nodeEdges;
            var neighbors;
            var path;

            nodes.on("mouseover", function (id) {
                //var _node = g.node(id);
                svgGroup.select("g.nodes").classed("hover", true); // Used to lower opacity of other nodes
                svgGroup.select("g.edgePaths").classed("hover", true); // Used to lower opacity of other edges

                d3.select(this).classed("hover", true);

                nodeEdges = g.nodeEdges(id);
                nodeEdges.forEach(function (neid) {
                    d3.select(g.edge(neid).elem).classed("hover", true);
                });

                neighbors = g.neighbors(id);
                neighbors.forEach(function (nid) {
                    d3.select(g.node(nid).elem).classed("neighbour", true);
                });

                path = g.getEntirePathLinks(id);
                path.forEach(function (e) {
                    d3.select(g.node(e.w).elem).classed("family", true);
                    d3.select(g.node(e.v).elem).classed("family", true);
                    d3.select(g.edge(e).elem).classed("family", true);
                });
            })
                .on("mouseout", function (id) {
                    svgGroup.select("g.nodes").classed("hover", false);
                    svgGroup.select("g.edgePaths").classed("hover", false);

                    d3.select(this).classed("hover", false);

                    //var nodeEdges = g.nodeEdges(id);
                    nodeEdges.forEach(function (neid) {
                        d3.select(g.edge(neid).elem).classed("hover", false);
                    });

                    //var neighbors = g.neighbors(id);
                    neighbors.forEach(function (nid) {
                        d3.select(g.node(nid).elem).classed("neighbour", false);
                    });

                    path.forEach(function (e) {
                        d3.select(g.node(e.w).elem).classed("family", false);
                        d3.select(g.node(e.v).elem).classed("family", false);
                        d3.select(g.edge(e).elem).classed("family", false);
                    });
                });

            // Add Legend

            var legend = drawLegend(svg);

            var legendWidth = legend.node().getBBox().width;
            var legendHeight = legend.node().getBBox().height;

            legend.append("text")
                .attr("class", "title")
                .attr("x", legendWidth / 2)
                .attr("y", -15)
                .attr("alignment-baseline", "middle")
                .attr("text-anchor", "middle")
                .text("Legend");

            var legendSideMargin = 20;

        } else {
            var legendWidth = 0;
            var legendHeight = 0;
            var legendSideMargin = 0;
        }
        var globalMargin = is_logo ? 2 : 20;

        // Center the graph
        var parentHeight = $(svg[0][0].parentNode).height();
        var parentWidth = $(svg[0][0].parentNode).width();
        var initialScale = is_logo ? parentHeight / g.graph().height : 0.75;

        var adjustedHeight = g.graph().height * initialScale + 2 * globalMargin;
        var adjustedWidth = g.graph().width * initialScale + 2 * globalMargin + legendWidth + legendSideMargin;
        var yTranslate = globalMargin;
        var xTranslate = globalMargin;

        // Correct height if legend is larger that graph
        if (!is_logo && g.graph().height * initialScale < legendHeight) {
            adjustedHeight = legendHeight + 2 * globalMargin;
            yTranslate = globalMargin + (legendHeight - g.graph().height * initialScale) / 2;
        }

        if ( is_logo && adjustedWidth < parentWidth ) {
            // Scale x if logo is too narrow
            svg.attr("transform", "scale(" + parentWidth/adjustedWidth + "," + 1 + ")");
            // adjustedWidth = parentWidth;
        }

        zoom
            .translate([xTranslate, yTranslate])
            .scale(initialScale)
            .event(svg);

        svg.attr('width', adjustedWidth);
        svg.attr('height', adjustedHeight);

        if (!is_logo) {
            legend.attr("transform", "translate(" + ( svg.attr("width") - legendWidth - legendSideMargin) + "," + (globalMargin + 15) + ")");
            this.legend = legend;
        }

        this.graph = g;

        this.renderer = render;
        this.nodes = function () {
            return svgGroup.selectAll("g.node");
        };
        this.edges = function () {
            return svgGroup.selectAll("g.edge");
        };
        this.id = id;
        callback();
    }

    var isUndefined = function (variable) {
        return ( typeof variable === 'undefined' );
    };

    function loadGraphData(g, graph_data, is_logo) {
        var rawnodes = graph_data.nodes;
        var rawedges = graph_data.edges;

        var createNodeOpts = function (n) {
            return {
                class: n.classes.join(" "),
                label: is_logo ? "" : n.label.substring(0, 15) + ( (n.label.length > 15) ? "..." : "" ),
                // label: "",
                href: "terms.xhtml?query=GO%3A" + utility.pad(n.id, 7),
                description: n.label,
                go_id: "GO:" + utility.pad(n.id, 7)
            };
        };

        for (var i = 0; i < rawnodes.length; i++) {
            var n = rawnodes[i];
            g.setNode(n.id, createNodeOpts(n));
        }

        var createEdgeOpts = function (e) {
            return {class: e.classes.join(" ") + ( e.type ? " " + e.type : "" ), lineInterpolate: 'basis', minlen: 2};
        };

        for (var i = 0; i < rawedges.length; i++) {
            var e = rawedges[i];
            g.setEdge(e.from, e.to, createEdgeOpts(e));
        }
    }

    function createRenderer(g, svgGroup) {
        // Create the renderer
        var render = new dagreD3.render();

        var oldCreateEdgePaths = render.createEdgePaths();
        render.createEdgePaths(function () {
            oldCreateEdgePaths.apply(this, arguments);

            // New functionality

            // We modify the nodes here instead of in createNodes because we need to be post-layout

            // Collect height information
            var ranks = {};
            svgGroup.selectAll("g.node").each(function (n) {
                var node = g.node(n);
                var a = ranks[node.y]
                if (a === undefined) {
                    a = [];
                }
                a.push(node);
                ranks[node.y] = a;
            });

            // Order them into ranks
            var orderedHeights = Object.keys(ranks).map(function (x) {
                return parseInt(x, 10);
            }).sort(function (a, b) {
                return a - b
            });

            // Assign rank information to nodes
            svgGroup.selectAll("g.node").each(function (n) {
                var node = g.node(n);
                node.rank = orderedHeights.indexOf(node.y);
            });

            //    // Align edges to between ranks
            //    if (orderedHeights.length > 1) {
            //    var nodeSep = orderedHeights[1] - orderedHeights[0];
            //    var edgeHeights = [];
            //    for (var i = 0; i < orderedHeights.length - 1; i++) {
            //    edgeHeights.push( orderedHeights[i] + nodeSep / 2 );
            //    }
            //    }

            // Node edge entry and exit points on top and bottom only
            // Adds a standoff point to each control point so the path intersects at a right angle.
            // Removes jutting edge paths
            svgGroup.selectAll("g.edgePath").selectAll("path.path").each(function (e) {
                var domEdge = d3.select(this);
                var edge = g.edge(e);
                edge.points = calcPoints(g, e);
                //g.graph().transition(domEdge).attr('d', calcPoints(g, e));

                var n1 = g.node(e.v);
                var n2 = g.node(e.w);
                if (n1.rank - n2.rank == 1) {
                    // Here we test those single rank edges for weird jutting paths

                    // We look for segments of the path that are not travelling in the same
                    // left/right direction as the average displacement vector
                    if (edge.points.length > 2) {
                        //var avgLeft = edge.points[edge.points.length - 1].x < edge.points[0].x; // left?
                        var avgDir = edge.points[edge.points.length - 1].x < edge.points[0].x ? -1 : ( edge.points[edge.points.length - 1].x == edge.points[0].x ? 0 : 1 );
                        var p2 = edge.points[0];
                        for (i = 1; i < edge.points.length - 1; ++i) {
                            var p = edge.points[i];
                            var dir = p.x < p2.x ? -1 : ( p.x == p2.x ? 0 : 1);
                            if (dir != avgDir && dir != 0) { // opposite direction
                                // remove p
                                //edge.points.splice(i, 1);
                                //i--;
                                //p = p2;
                                p.x = p2.x;
                            } else if (avgDir * p.x > avgDir * edge.points[edge.points.length - 1].x) { // overtakes the end point
                                p.x = edge.points[edge.points.length - 1].x;
                            }

                            p2 = p;
                        }
                    }
                }

//       var rnd = 2*getRandomInt(-2,2);
//       for (i = 1; i < edge.points.length - 1; ++i) {
//       var p = edge.points[i];
//       // Align to specific heights
//       p.y = closest(p.y, edgeHeights) + rnd;
//       }
                domEdge.attr('d', renderLine(edge.points));

            });


        });

        return render;
    }

    function drawLegend(svg) {
        svg.selectAll("g.legend").remove();
        var legendDataset = [{label: "Is a", color: "black"}, {label: "Part of", color: "#056aff"}];

        var legend = svg.append('g').attr("class", "legend");

        var xPadding = 10;
        var yPadding = 10;

        legend.append('marker')
            .attr("id", "triangle")
            .attr("viewBox", "0 0 10 10")
            .attr("refX", "9")
            .attr("refY", "5")
            .attr("markerUnits", "strokeWidth")
            .attr("markerWidth", "8")
            .attr("markerHeight", "10")
            .attr("orient", "auto")
            .append("path")
            .attr("d", "M 0 0 L 10 5 L 0 10 z");

        legend.append('marker')
            .attr("id", "triangle-blue")
            .attr("viewBox", "0 0 10 10")
            .attr("refX", "9")
            .attr("refY", "5")
            .attr("markerUnits", "strokeWidth")
            .attr("markerWidth", "8")
            .attr("markerHeight", "10")
            .attr("orient", "auto")
            .append("path")
            .attr("d", "M 0 0 L 10 5 L 0 10 z")
            .style("fill", "#056aff");

        var legendBoxDim = 20;
        var legendPathLength = 60;

        var width = legendBoxDim * 2 + legendPathLength;

        var legendGroup = legend.selectAll('rect')
            .data(legendDataset)
            .enter();

        legendGroup.append("rect")
            .attr("class", "mockNode")
            .attr("x", xPadding)
            .attr("y", function (d, i) {
                return yPadding + i * 2 * legendBoxDim;
            })
            .attr("width", legendBoxDim)
            .attr("height", legendBoxDim)
            .attr("rx", 5)
            .attr("ry", 5)

        legendGroup.append("rect")
            .attr("class", "mockNode")
            .attr("x", xPadding + legendBoxDim + legendPathLength)
            .attr("y", function (d, i) {
                return yPadding + i * 2 * legendBoxDim;
            })
            .attr("width", legendBoxDim)
            .attr("height", legendBoxDim)
            .attr("rx", 5)
            .attr("ry", 5);

        legendGroup = legend.selectAll('text')
            .data(legendDataset)
            .enter();

        legendGroup.append("text")
            .attr("x", xPadding + legendBoxDim / 2)
            .attr("y", function (d, i) {
                return yPadding + legendBoxDim / 2 + 1 + i * 2 * legendBoxDim;
            })
            .attr("alignment-baseline", "middle")
            .attr("text-anchor", "middle")
            .text("A");

        legendGroup.append("text")
            .attr("x", xPadding + legendBoxDim + legendPathLength + legendBoxDim / 2)
            .attr("y", function (d, i) {
                return yPadding + legendBoxDim / 2 + 1 + i * 2 * legendBoxDim;
            })
            .attr("alignment-baseline", "middle")
            .attr("text-anchor", "middle")
            .text("B");

        legendGroup.append("text")
            .attr("x", xPadding + legendBoxDim + legendPathLength / 2)
            .attr("y", function (d, i) {
                return yPadding + legendBoxDim / 4 + 1 + i * 2 * legendBoxDim;
            })
            .attr("alignment-baseline", "middle")
            .attr("text-anchor", "middle")
            .text(function (d) {
                return d.label;
            });

        legendGroup = legend.selectAll('line')
            .data(legendDataset)
            .enter();

        legendGroup.append("line")
            .attr("x1", xPadding + legendBoxDim)
            .attr("y1", function (d, i) {
                return yPadding + legendBoxDim / 2 + 1 + i * 2 * legendBoxDim;
            })
            .attr("x2", xPadding + legendBoxDim + legendPathLength)
            .attr("y2", function (d, i) {
                return yPadding + legendBoxDim / 2 + 1 + i * 2 * legendBoxDim;
            })
            .attr("marker-end", function (d) {
                return d.color == "black" ? "url(#triangle)" : "url(#triangle-blue)"
            })
            .style("stroke", function (d) {
                return d.color;
            });

        legend.insert("rect", ":first-child")
            .attr("class", "background")
            .attr("x", 0)
            .attr("y", 0)
            .attr("rx", 5)
            .attr("ry", 5)
            .attr("width", 2 * xPadding + width)
            .attr("height", 2 * yPadding + legend.node().getBBox().height);

        return legend;
    }

    function calcPoints(g, e) {
        var edge = g.edge(e.v, e.w),
            tail = g.node(e.v),
            head = g.node(e.w);
        var points = edge.points.slice(1, edge.points.length - 1);
        var i = intersectRect(tail, points[0]);
        points.unshift(i[1], i[0]);
        i = intersectRect(head, points[points.length - 1]);
        points.push(i[0], i[1]);
        return points;
    }

    function renderLine(points) {
        return d3.svg.line()
            .x(function (d) {
                return d.x;
            })
            .y(function (d) {
                return d.y;
            })
            .interpolate("basis")
            (points);
    }

//taken from dagre-d3 source code (not the exact same)
    function intersectRect(node, point) {
        var x = node.x;
        var y = node.y;
        var dx = point.x - x;
        var dy = point.y - y;
        var w = node.width / 2;
        var h = node.height / 2;
        var pad = 8;
        if (Math.abs(dy) * w > Math.abs(dx) * h) {
            // Intersection is top or bottom of rect.
            if (dy < 0) {
                h = -h;
                pad = -pad;
            }
            return [{x: x, y: y + h + pad}, {x: x, y: y + h}];
        } else {
            // Intersection is left or right of rect.
            if (point.y < y) {
                h = -h;
                pad = -pad;
            }
            return [{x: x, y: y + h + pad}, {x: x, y: y + h}];
        }
    }

}(window.gograph = window.gograph || {}, jQuery));
