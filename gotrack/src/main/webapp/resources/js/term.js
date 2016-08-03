var MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff80", "#ffff00", "#ff5005"]

function onLoad() {}

function centerResize() {
   try {
      var index = PrimeFaces.widgets.centerTabWdg.getActiveIndex();
      tabShowed(index);
   } catch (e) {
      console.log(e);
   }
}

function tabShowed(index) {
   try {
      if (index==0) {
         CS.overview.graph.resize();
      } else if (index==2) {
         HC.charts.overview.resize();
      } else if (index==3) {
         HC.charts.gene.resize();
      } else if (index==4) {
         HC.charts.evidence.resize();
      }
   } catch(e) {
      console.log(e);
   }
}

function handleFetchDAGData(xhr, status, args) {
   console.log(args);
   try {
      $('#loading-spinner-DAG').hide();
      $('#currentAncestry').show();
   } catch(e) {
      console.log(e);
   }
   CS.overview = createVisGraph(args, '#currentAncestry');
}

function handleFetchOverviewChart(xhr, status, args) {
   console.log(args);
   try {
      $('#loading-spinner-overview').hide();
      GLOBALS.dateToGOEditionId = JSON.parse(args.dateToGOEditionId);
      GLOBALS.nameChange = JSON.parse(args.dateToNameChange);
   } catch(e) {
      console.log(e);
   }

   createOverviewChart(args);
}

function handleFetchGeneChart(xhr, status, args) {
   console.log(args);
   try {
      $('#loading-spinner-gene').hide();
   } catch(e) {
      console.log(e);
   }
   createGeneCountChart(args);
}

function handleFetchEvidenceChart(xhr, status, args) {
   console.log(args);
   try {
      $('#loading-spinner-evidence').hide();
   } catch(e) {
      console.log(e);
   }
   createEvidenceCountChart(args);
}

//function revertOverview() {

//revertGraph(CS.overview, function() {
//CS.overview.graph.nodes().lock();
//CS.overview.graph.panningEnabled( false );
//CS.overview.graph.zoomingEnabled( false );
//CS.overview.graph.boxSelectionEnabled( false );
//});

//}


function revertGraph(cs) {
   var new_cs = createVisGraph(cs.args, cs.selector)
   cs.graph = new_cs.graph;
   cs.diff = false;
}


//function handleFetchDiff(xhr, status, args) {
//console.log(args);
//revertGraph(CS.overview,function(){showDiff(CS.overview, args)})
////showDiff(CS.overview, args)
//}

function handleFetchGraphDialog(xhr, status, args) {
   console.log(args);
   CS.dialogGraph = createVisGraph(args, "#dagDialog");
}


function createVisGraph(args, selector) {

   var eles = JSON.parse(args.cyto_graph);

   var diff = null;
   var deletedNodes = {};
   var deletedEdges = {};
   if ( args.vis_diff !== undefined ) {
      diff = JSON.parse(args.vis_diff);

      for (var i = 0; i < diff.deleted.nodes.length; i++) {
         deletedNodes[diff.deleted.nodes[i].id] = true;
      }

      for (var i = 0; i < diff.deleted.edges.length; i++) {
         var e = diff.deleted.edges[i];
         deletedEdges[e.from + "," + e.to + "," + e.type] = true;
      }

   }

   var rawnodes = eles.nodes;
   var rawedges = eles.edges;

   var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(function() { return {}; });

   var createNodeOpts = function(n, clazzz) {
      if ( clazzz === undefined ) {
         clazzz = [];
      }
      //var label = utility.wordwrap(n.label.substring(0, 30) + ( (n.label.length > 30) ? "...": "" ),15,'\n');
      var label = n.label.substring(0, 15) + ( (n.label.length > 15) ? "...": "" );
      opts = { class: clazzz.join(" "), label: label, href: "terms.xhtml?query=GO%3A" + utility.pad(n.id,7), description: n.label, go_id:"GO:"+utility.pad(n.id,7) } ;
      return opts;
   }

   for (var i = 0; i < rawnodes.length; i++) {
      var n = rawnodes[i];
      var clazzz = []
      if (n.id == args.current_id) {
         clazzz.push("selectedTerm");
      } 

      if (deletedNodes[n.id]) {
         clazzz.push("deleted");
      }
      g.setNode(n.id, createNodeOpts(n, clazzz));
   }

   if ( diff != null) {
      for (var i = 0; i < diff.added.nodes.length; i++) {
         var n = diff.added.nodes[i];
         var opts = createNodeOpts(n);
         opts['class'] = "added";
         g.setNode(n.id, opts);
      }
   }

   g.nodes().forEach(function(v) {
      var node = g.node(v);
      // Round the corners of the nodes
      node.rx = node.ry = 5;
   });

   var createEdgeOpts = function(e, clazzz) {
      if ( clazzz === undefined ) {
         clazzz = [];
      }
      opts = {class:clazzz.join(" "), lineInterpolate: 'basis', minlen:2};
      return opts;
   }

   for (var i = 0; i < rawedges.length; i++) {
      var e = rawedges[i];
      var clazzz = [e.type]; 
      if (deletedEdges[e.from + "," + e.to + "," + e.type]) {
         clazzz.push("deleted");
      }
      g.setEdge(e.from, e.to, createEdgeOpts(e, clazzz));
   }

   if ( diff != null) {
      for (var i = 0; i < diff.added.edges.length; i++) {
         var e = diff.added.edges[i];
         g.setEdge(e.from, e.to, createEdgeOpts(e, [e.type, "added"]));
      }
   }

// Set up an SVG group so that we can translate the final graph.
   var svg = d3.select(selector);
   svg.selectAll("*").remove();
   var svgGroup = svg.append("g");

   g.graph().rankDir='BT';
   g.graph().nodesep=10;
   g.graph().ranksep=12;
   g.graph().edgesep=0;

// g.graph().transition = function(selection) {
// return selection.transition().duration(1000);
// };

   g.getEntirePathLinks = function(centerId) {
      // Returns a list containing all edges leading into or from the center node
      var g = this;
      var nodes = {};

      var ancestors = {};

      var explore_parents = function(nid) {
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

      var explore_children = function(nid) {
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
   }

   var orderedHeights;

   // Create the renderer
   var render = new dagreD3.render();

   var oldCreateEdgePaths = render.createEdgePaths();
   render.createEdgePaths(function() {
      oldCreateEdgePaths.apply(this, arguments);

      // New functionality

      // We modify the nodes here instead of in createNodes because we need to be post-layout

      // Collect height information
      var ranks = {};
      svgGroup.selectAll("g.node").each(function(n) {
         var node = g.node(n);
         var a = ranks[node.y]
         if ( a === undefined) {
            a = [];
         }
         a.push(node);
         ranks[node.y] = a;
      });

      // Order them into ranks
      orderedHeights = Object.keys(ranks).map(function (x) { 
         return parseInt(x, 10); 
      }).sort(function(a,b){return a - b});

      // Assign rank information to nodes
      svgGroup.selectAll("g.node").each(function(n) {
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
      svgGroup.selectAll("g.edgePath").selectAll("path.path").each(function(e) {
         var domEdge = d3.select(this);
         var edge = g.edge(e);
         edge.points = calcPoints(g, e);
         //g.graph().transition(domEdge).attr('d', calcPoints(g, e));

         var n1 = g.node(e.v);
         var n2 = g.node(e.w);
         if (n1.rank - n2.rank == 1 ) {
            // Here we test those single rank edges for weird jutting paths

            // We look for segments of the path that are not travelling in the same 
            // left/right direction as the average displacement vector
            if ( edge.points.length > 2 ) {
               //var avgLeft = edge.points[edge.points.length - 1].x < edge.points[0].x; // left?
               var avgDir = edge.points[edge.points.length - 1].x < edge.points[0].x ? -1 : ( edge.points[edge.points.length - 1].x == edge.points[0].x ? 0 : 1 );
               var p2 = edge.points[0];
               for (i = 1; i < edge.points.length - 1; ++i) {
                  var p = edge.points[i];
                  var dir = p.x < p2.x ? -1 : ( p.x == p2.x ? 0 : 1);
                  if ( dir != avgDir && dir != 0 ) { // opposite direction
                     // remove p
                     //edge.points.splice(i, 1);
                     //i--;
                     //p = p2;
                     p.x = p2.x;
                  } else if ( avgDir * p.x > avgDir * edge.points[edge.points.length - 1].x  ) { // overtakes the end point
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
         domEdge.attr('d', renderLine( edge.points ));

      });


   });

// Set up zoom support
   var zoom = d3.behavior.zoom().on("zoom", function() {
      if (d3.event.sourceEvent) d3.event.sourceEvent.stopPropagation();
      svgGroup.attr("transform", "translate(" + d3.event.translate + ")" +
         "scale(" + d3.event.scale + ")");
   });
   svg.call(zoom);

   // Run the renderer. This is what draws the final graph.
   render(svgGroup, g);

   /**POST RENDER**/

   // tooltips

   // Simple function to style the tooltip for the given node.
   var styleTooltip = function(name, description) {
      return "<p class='name'>" + name + "</p><p class='description'>" + description + "</p>";
   };

   var nodes = svgGroup.selectAll("g.node");
   nodes.attr("title", function(v) { 
      return styleTooltip(g.node(v).go_id, g.node(v).description) 
   });
   nodes.each(function(v) { 
      $(this).tipsy({ gravity: "w", opacity: 0.8, html: true }); 
   });

   // events
   nodes.on("click", function(id) { 
      if (d3.event.defaultPrevented) return;
      var _node = g.node(id); 
      try { // your browser may block popups
         window.open( _node.href );
      } catch(e){ // fall back on url change
         window.location.href = _node.href; 
      } 
   })

   var nodeEdges;
   var neigbors;
   var path;

   nodes.on("mouseover", function (id) {
      //var _node = g.node(id); 
      svgGroup.select("g.nodes").classed("hover", true); // Used to lower opacity of other nodes
      svgGroup.select("g.edgePaths").classed("hover", true); // Used to lower opacity of other edges

      d3.select(this).classed("hover", true);

      nodeEdges = g.nodeEdges(id);
      nodeEdges.forEach(function(neid) {
         d3.select(g.edge(neid).elem).classed("hover", true);
      });

      neigbors = g.neighbors(id);
      neigbors.forEach(function(nid) {
         d3.select(g.node(nid).elem).classed("neighbour", true);
      });

      path = g.getEntirePathLinks(id);
      path.forEach(function(e) {
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
      nodeEdges.forEach(function(neid) {
         d3.select(g.edge(neid).elem).classed("hover", false);
      });

      //var neigbors = g.neighbors(id);
      neigbors.forEach(function(nid) {
         d3.select(g.node(nid).elem).classed("neighbour", false);
      });

      path.forEach(function(e) {
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
   var globalMargin = 20;
      
   // Center the graph
   var initialScale = 0.75;
   
   var adjustedHeight = g.graph().height * initialScale + 2 * globalMargin;
   var yTranslate = globalMargin;
   if ( g.graph().height * initialScale < legendHeight) {
      adjustedHeight = legendHeight + 2 * globalMargin;
      yTranslate = globalMargin + (legendHeight - g.graph().height * initialScale) / 2;
   }
     
   svg.attr('width', g.graph().width * initialScale + 2 * globalMargin + legendWidth + legendSideMargin);
   zoom
   .translate([(svg.attr("width") - g.graph().width * initialScale - legendWidth - legendSideMargin) / 2, yTranslate])
   .scale(initialScale)
   .event(svg);
 
   svg.attr('height', adjustedHeight);
   
   legend.attr("transform", "translate(" + ( svg.attr("width") - legendWidth - legendSideMargin) + ","+ (globalMargin + 15) +")");

   return {graph: g, diff: false, args: args, selector: selector}


}

function drawLegend(svg) {
   svg.selectAll("g.legend").remove();
   var legendDataset = [{label:"Is a", color:"black"}, {label:"Part of", color:"#056aff"}];
   
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
   .attr("y", function(d, i){ return yPadding + i *  2 * legendBoxDim;})
   .attr("width", legendBoxDim)
   .attr("height", legendBoxDim)
   .attr("rx", 5)
   .attr("ry", 5)
   
   legendGroup.append("rect")
   .attr("class", "mockNode")
   .attr("x", xPadding + legendBoxDim + legendPathLength)
   .attr("y", function(d, i){ return yPadding + i *  2 * legendBoxDim;})
   .attr("width", legendBoxDim)
   .attr("height", legendBoxDim)
   .attr("rx", 5)
   .attr("ry", 5);
   
   legendGroup = legend.selectAll('text')
   .data(legendDataset)
   .enter();
   
   legendGroup.append("text")
   .attr("x", xPadding + legendBoxDim / 2)
   .attr("y", function(d, i){ return yPadding + legendBoxDim / 2 + 1 + i *  2 * legendBoxDim;})
   .attr("alignment-baseline", "middle")
   .attr("text-anchor", "middle")
   .text("A");
   
   legendGroup.append("text")
   .attr("x", xPadding + legendBoxDim + legendPathLength + legendBoxDim / 2)
   .attr("y", function(d, i){ return yPadding + legendBoxDim / 2 + 1 + i *  2 * legendBoxDim;})
   .attr("alignment-baseline", "middle")
   .attr("text-anchor", "middle")
   .text("B");
   
   legendGroup.append("text")
   .attr("x", xPadding + legendBoxDim + legendPathLength / 2)
   .attr("y", function(d, i){ return yPadding + legendBoxDim / 4 + 1 + i *  2 * legendBoxDim;})
   .attr("alignment-baseline", "middle")
   .attr("text-anchor", "middle")
   .text(function(d) {
     return d.label;
   });
   
   legendGroup = legend.selectAll('line')
   .data(legendDataset)
   .enter();
   
   legendGroup.append("line")
   .attr("x1", xPadding + legendBoxDim)
   .attr("y1", function(d, i){ return yPadding + legendBoxDim / 2 + 1 + i *  2 * legendBoxDim;})
   .attr("x2", xPadding + legendBoxDim + legendPathLength)
   .attr("y2", function(d, i){ return yPadding + legendBoxDim / 2 + 1 + i *  2 * legendBoxDim;})
   .attr("marker-end", function(d){ return d.color == "black" ? "url(#triangle)" : "url(#triangle-blue)"})
   .style("stroke", function(d) { 
        return d.color;
      });

   legend.insert("rect",":first-child")
   .attr("class", "background")
   .attr("x", 0)
   .attr("y", 0)
   .attr("rx", 5)
   .attr("ry", 5)
   .attr("width", 2 * xPadding + width)
   .attr("height", 2 * yPadding + legend.node().getBBox().height );
      
   return legend;
}

//function getRandomInt(min, max) {
//return Math.floor(Math.random() * (max - min + 1)) + min;
//}

//function closest(num, arr) { // Don't use this if you need it to be fast. [O(n)]
//var curr = arr[0];
//var diff = Math.abs (num - curr);
//for (var val = 0; val < arr.length; val++) {
//var newdiff = Math.abs (num - arr[val]);
//if (newdiff < diff) {
//diff = newdiff;
//curr = arr[val];
//}
//}
//return curr;
//}

//function intersectLineRect(p1, p2, rect) {
//var minX = rect[0];
//var minY = rect[1];
//var maxX = rect[2];
//var maxY = rect[3];
//// Completely outside.
//if ((p1.x <= minX && p2.x <= minX) || (p1.y <= minY && p2.y <= minY) || (p1.x >= maxX && p2.x >= maxX) || (p1.y >= maxY && p2.y >= maxY))
//return null;

//var m = (p2.y - p1.y) / (p2.x - p1.x);

//var RIGHT_EDGE = p1.x >= maxX; // test right edge
//var BOTTOM_EDGE = p1.y >= maxY;
//var LEFT_EDGE = p1.x <= minX;
//var TOP_EDGE = p1.y <= minY;


//if (LEFT_EDGE) {
//var y = m * (minX - p1.x) + p1.y;
//if (y > minY && y < maxY) return {x: minX, y: y, e:3}; // hits left edge at y
//}

//if (RIGHT_EDGE) {
//y = m * (maxX - p1.x) + p1.y;
//if (y > minY && y < maxY) return {x: maxX, y: y, e:1}; // hits right edge at y
//}
//if (TOP_EDGE) {
//var x = (minY - p1.y) / m + p1.x;
//if (x > minX && x < maxX) return {x: x, y: minY, e:0}; // hits top edge at x
//}
//if (BOTTOM_EDGE) {
//x = (maxY - p1.y) / m + p1.x;
//if (x > minX && x < maxX) return {x: x, y: maxY, e:2}; // hits bottom edge at x
//}

//return null;
//}

//taken from dagre-d3 source code (not the exact same)
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

function toggleDiff(cs, args) {
   if (utility.isUndefined(cs.graph)) {
      console.log("Graph not yet created.");
      return;
   }

   if (cs.diff) {
      revertGraph(cs)
   } else {
      showDiff(cs, args)
   }
}


function showDiff(cs, args) {
   if (utility.isUndefined(cs.graph)) {
      console.log("Graph not yet created.");
      return;
   }

   if (utility.isUndefined( args.vis_diff)) {
      return;
   }

   var graph = cs.graph;

   var diff = JSON.parse(args.vis_diff);
   if (diff == null) {
      return;
   }
   console.log(diff);
   var del = diff.deleted;
   var add = diff.added; 

   var delNodes = del.nodes
   var delEdges = del.edges

   for (var i = 0; i < delNodes.length; i++) {
      var n = delNodes[i]
      graph.getElementById(n.id).data('deleted',1);
   }

   for (var i = 0; i < delEdges.length; i++) {
      var e = delEdges[i]
      graph.getElementById(String(e.from) + "," + String(e.to)).data('deleted',1);
   }

   var addedNodes = add.nodes
   var addedEdges = add.edges

   for (var i = 0; i < addedNodes.length; i++) {
      var n = addedNodes[i];
      var label = utility.wordwrap(n.label.substring(0, 30) + ( (n.label.length > 30) ? "...": "" ),13,'\n');
      graph.add({
         group: "nodes",
         data: { 
            id: String(n.id), 
            label: label,
            href: "terms.xhtml?query=GO%3A" + utility.pad(n.id,7),
            added: 1
         } ,
         //position: { x: 0, y: 0 }
      });
   }

   for (var i = 0; i < addedEdges.length; i++) {
      var e = addedEdges[i];
      graph.add({
         group: "edges",
         data: { id: String(e.from) + "," + String(e.to) , source: String(e.from), target: String(e.to), type: e.type, added: 1 }
      });

   }

   cs.diff = true;
   graph.layout();

}

function createOverviewChart(args) {
   var options = {
                  chart: {
                     renderTo: 'hc_overview_container',
                     type: 'xrange',
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           align: 'left',
                           // verticalAlign: 'top', // by default
                           x: 0,
                           y: -35
                        }
                     },
                  },
                  title: {
                     text: args.hc_overview_title
                  },

                  xAxis: {
                     type: 'datetime',
                     title: {
                        text: args.hc_overview_xlabel
                     },
                     minRange: 60 * 24 * 3600000 // fourteen days
                  },

                  yAxis: {
                     categories: ['Name Change', 'Structure Change','Existence'],
                     min:0,
                     max:2,
                     title: '',
                     labels: {
                        formatter: function () {
                           return this.value;
                        }
                     }
                  },

                  plotOptions : {
                     series : {
                        grouping: false,
                        point: {
                           events: {
                              click: function () {
                                 if (this.y != 0){
                                    fetchGraph([{name:'edition', value:GLOBALS.dateToGOEditionId[this.x]},{name:'showDiff', value:this.y==1} ]);
                                 }
                              }
                           }
                        },
                     }
                  },

                  tooltip: {
                     shared:false,
                     dateTimeLabelFormats:{
                        millisecond:"%B %Y",
                        hour:"%B %Y", 
                        minute:"%B %Y"
                     },
                     formatter: function() {
                        return  '<b>' + Highcharts.dateFormat('%b %Y',
                           new Date(this.x)) +'</b><br/>' + this.key
                     }
                  },
                  legend : {
                     enabled: false,
                     align : 'right',
                     verticalAlign: 'top',
                     layout: 'vertical',
                     y:20
                  },

                  series: [],

                  colors : [],

                  exporting: {
                     enabled: true,
                     sourceWidth  : 1600,
                     sourceHeight : 900,
                     csv: {
                        dateFormat: '%Y-%m-%d'
                     }
                  }
   }

   if (!utility.isUndefined( args.hc_overview_data ) ){
      var existenceSeries;
      var structureSeries;
      var nameSeries;
      for (var i = 0; i < args.hc_overview_data.series.length; i++) {
         var series = args.hc_overview_data.series[i];
         var name = series.name;
         if (name == "Existence" ) {
            existenceSeries = series;
         } else if (name == "Structure Change"){
            structureSeries = series;
         } else {
            nameSeries = series;
         }
      }

      //console.log(existenceSeries)

      var data = []
      var structureData = [];
      var nameData = [];

      for (var j = 0; j < existenceSeries.data.length; j++) {
         var point = existenceSeries.data[j];
         var nextPointX = (j > existenceSeries.data.length - 2) ? point.x + 2629740000: existenceSeries.data[j+1].x; //add month
         data.push({x:point.x,x2:nextPointX, y:2,name:point.y==1 ? 'Exists': 'Does Not Exist', color:point.y==1?'#2bce48':'#d63232'});
         //' From <b>' + GLOBALS.nameChange[this.x][0] + '</b> To <b>' + GLOBALS.nameChange[this.x][1] +'</b>'
         if (structureSeries.data[j].y == 1) {
            structureData.push({x:point.x,x2:nextPointX, y:1, name:'Structure Has Changed', color:'#2bce48'})
         } else {
            //structureData.push({x:point.x,x2:nextPointX, y:1, name:'Structure Has Not Changed'})
         }

         if (nameSeries.data[j].y == 1) {
            nameData.push({x:point.x,x2:nextPointX, y:0, name:'Name Has Changed From <b>' + GLOBALS.nameChange[point.x][0] + '</b> To <b>' + GLOBALS.nameChange[point.x][1] +'</b>', color:'#2bce48'})
         }

      }

      options.series.push({
         name : "Existence",
         pointWidth: 40,
         data : data
      });

      options.series.push({
         name : "Structure",
         pointWidth: 40,
         data : structureData
      });

      options.series.push({
         name : "Name",
         pointWidth: 40,
         data : nameData
      });

      HC.charts.overview.options = options;
      HC.charts.overview.recreate(options);

   }
}

function createGeneCountChart(args) {
   var options = defaultHCOptions(hc_gene_container, args.hc_gene_title, args.hc_gene_xlabel, args.hc_gene_ylabel);

   if (!utility.isUndefined( args.hc_gene_data ) ){
      //args.hc_gene_data.series.sort(function(a,b) {return (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0);} );
      for (var i = 0; i < args.hc_gene_data.series.length; i++) {
         var series = args.hc_gene_data.series[i];
         var name = series.name;
         var data = []

         for (var j = 0; j < series.data.length; j++) {
            var point = series.data[j];
            data.push([point.x,point.y]);
         }

         options.series.push({
            name : name,
            data : data,
            visible: name.indexOf("Direct") > -1,
            isolated: false,
         })

      }
      
      options.exporting.buttons = {
         toggleSubsets: {
            align: 'right',
            verticalAlign: "top",
            x: -138,
            y: 0,
            onclick: function () {
               console.log(this);
               this.toggleSubsets = (this.toggleSubsets + 1) % 3;
               var button = this.exportSVGElements[3];
               button.attr({fill:'#33cc33'});
               
               var test =  function() {
                  return true;
               }
               switch(this.toggleSubsets) {
                  case 0: //Direct only
                     text = "Subset: Direct";
                     test = function(s) {
                        return  s.name.indexOf("Direct")  > -1;
                     }
                     break; 
                  case 1: //Indirect only
                     text = "Subset: Indirect";
                     test = function(s) {
                        return  s.name.indexOf("Direct") == -1;
                     }
                     break;
                  case 2: //All
                     text = "Subset: All";
                     break;
               }
               
               button.parentGroup.attr({text:text});
               var series = this.series;
               for (var i = 0; i < series.length; i++) {
                  var s = series[i];
                  if (test(s)) {
                     s.setVisible(true, false)
                     s.isolated=false;
                  } else {
                     s.setVisible(false, false)
                     s.isolated=false;
                  }
               }
               this.redraw();
            },
            symbol: 'circle',
            symbolFill: '#33cc33',
            _titleKey: "toggleSubsetsTitle",
            text: 'Subset: Direct'
         }                   
      };
      
      options.plotOptions.series.events.legendItemClick = function(event) {

         var defaultBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;
         
         var button = this.chart.exportSVGElements[3];
         button.attr({fill:'#E0E0E0'});
         button.parentGroup.attr({text:"Subset: Custom"});
         this.chart.toggleSubsets = -1;

         if (!defaultBehaviour) {

            var seriesIndex = this.index;
            var series = this.chart.series;

            var reset = this.isolated;


            for (var i = 0; i < series.length; i++) {
               if (series[i].index != seriesIndex)
               {
                  if (reset) {
                     series[i].setVisible(true, false)
                     series[i].isolated=false;
                  } else {
                     series[i].setVisible(false, false)
                     series[i].isolated=false; 
                  }

               } else {
                  if (reset) {
                     series[i].setVisible(true, false)
                     series[i].isolated=false;
                  } else {
                     series[i].setVisible(true, false)
                     series[i].isolated=true;
                  }
               }
            }

            this.chart.redraw();

            return false;
         }
      };


      HC.charts.gene.options = options;
      HC.charts.gene.recreate(options, function(c) {
         c.toggleSubsets = 0;
       });

   }
}

function createEvidenceCountChart(args) {
   var options = defaultHCOptions(hc_evidence_container, args.hc_evidence_title, args.hc_evidence_xlabel, args.hc_evidence_ylabel);

   if (!utility.isUndefined( args.hc_evidence_data ) ){
      for (var i = 0; i < args.hc_evidence_data.series.length; i++) {
         var series = args.hc_evidence_data.series[i];
         var name = series.name;
         var data = []

         for (var j = 0; j < series.data.length; j++) {
            var point = series.data[j];
            data.push([point.x,point.y]);
         }

         options.series.push({
            name : name,
            data : data
         })

      }


      HC.charts.evidence.options = options;
      HC.charts.evidence.recreate(options);

   } 
}

function defaultHCOptions(renderTo, title, xLabel, yLabel ) {
   return {
                  chart: {
                     renderTo: renderTo,
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           align: 'left',
                           // verticalAlign: 'top', // by default
                           x: 0,
                           y: -35,
                        }
                     },
//                   events: {
//                   click: function(event) {
//                   fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.hoverPoint.x]} ]);
//                   }
//                   }
                  },
                  title: {
                     text: title
                  },

                  xAxis: {
                     type: 'datetime',
                     title: {
                        text: xLabel
                     },
                     minRange: 60 * 24 * 3600000 // fourteen days
                  },

                  yAxis: {
                     type: 'linear',
                     title: {
                        text: yLabel
                     },
                     labels: {
                        formatter: function () {
                           return this.value;
                        }
                     }
                  },

                  plotOptions : {
                     series : {
//                      point: {
//                      events: {
//                      click: function () {
//                      fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.x]} ]);
//                      }
//                      }
//                      },
                        events: {
                           legendItemClick: function(event) {

                              var defaultBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;

                              if (!defaultBehaviour) {

                                 var seriesIndex = this.index;
                                 var series = this.chart.series;

                                 var reset = this.isolated;


                                 for (var i = 0; i < series.length; i++) {
                                    if (series[i].index != seriesIndex)
                                    {
                                       if (reset) {
                                          series[i].setVisible(true, false)
                                          series[i].isolated=false;
                                       } else {
                                          series[i].setVisible(false, false)
                                          series[i].isolated=false; 
                                       }

                                    } else {
                                       if (reset) {
                                          series[i].setVisible(true, false)
                                          series[i].isolated=false;
                                       } else {
                                          series[i].setVisible(true, false)
                                          series[i].isolated=true;
                                       }
                                    }
                                 }
                                 this.chart.redraw();

                                 return false;
                              }
                           }
                        }
                     }
                  },

                  tooltip: {
                     shared:true,
                     dateTimeLabelFormats:{
                        hour:"%B %Y", 
                        minute:"%B %Y"
                     }
                  },
                  legend : {
                     align : 'right',
                     verticalAlign: 'top',
                     layout: 'vertical',
                     y:20
                  },

                  series: [],

                  colors : MAXIMALLY_DISTINCT_COLORS,

                  exporting: {
                     enabled: true,
                     sourceWidth  : 1600,
                     sourceHeight : 900,
                     csv: {
                        dateFormat: '%Y-%m-%d'
                     }
                  }
   }
}

function HChart(id) {
   this.id = id;
   this.chart = null;
   this.options = {};
   this._exists = false;
   this.exists = function() {
      return (this.chart!=null && this._exists);
   }
   this.destroy = function() {
      if ( this.exists() ) {
         try {
            this.chart.destroy(); 
         } catch (e) {
            console.log(e);
         }
      } else {
         console.log('Chart not yet created');
      }
      this._exists=false;

   }
   this.create = function(callback) {
      if(typeof callback === 'undefined'){
         callback = function(){};
      }
      if ( !this.exists() ) {
         this.chart = new Highcharts.Chart(this.options, callback);
         this._exists=true;
         console.log("Chart created");
      }
   }
   this.reset = function() {
      this.recreate(this.options);
   }
   this.recreate = function(options, callback) {
      if(typeof callback === 'undefined'){
         callback = function(){};
      }
      try {
         if(typeof options === 'undefined'){
            // options not supplied
            options = this.options; //fallback incase chart is not made yet
            options = this.chart.options;
         }
         this.destroy();
      } catch (e) {
         console.log(e);
      } finally {
         try{
            this.chart = new Highcharts.Chart(options, callback);
            this._exists=true;
         } catch (e) {
            console.log("Failed to create chart");
         }

      }

   }
   this.resize = function() {
      try {
         this.chart.reflow();
      } catch (e) {
         console.log(e);
      }
   }

}

$(document).ready(function() {
   //escDialog();
   
   try {

      Highcharts.setOptions({
         lang: {
            toggleSubsetsTitle: "Toggle Subsets"
         }
      });
   } catch(err) {

   }

   /**
    * Highcharts X-range series plugin
    */
   (function (H) {
      var defaultPlotOptions = H.getOptions().plotOptions,
      columnType = H.seriesTypes.column,
      each = H.each;

      defaultPlotOptions.xrange = H.merge(defaultPlotOptions.column, {});
      H.seriesTypes.xrange = H.extendClass(columnType, {
         type: 'xrange',
         parallelArrays: ['x', 'x2', 'y'],
         animate: H.seriesTypes.line.prototype.animate,

         /**
          * Borrow the column series metrics, but with swapped axes. This gives free access
          * to features like groupPadding, grouping, pointWidth etc.
          */  
         getColumnMetrics: function () {
            var metrics,
            chart = this.chart,
            swapAxes = function () {
               each(chart.series, function (s) {
                  var xAxis = s.xAxis;
                  s.xAxis = s.yAxis;
                  s.yAxis = xAxis;
               });
            };

            swapAxes();

            this.yAxis.closestPointRange = 1;
            metrics = columnType.prototype.getColumnMetrics.call(this);

            swapAxes();

            return metrics;
         },
         translate: function () {
            columnType.prototype.translate.apply(this, arguments);
            var series = this,
            xAxis = series.xAxis,
            yAxis = series.yAxis,
            metrics = series.columnMetrics;

            H.each(series.points, function (point) {
               barWidth = xAxis.translate(H.pick(point.x2, point.x + (point.len || 0))) - point.plotX;
               point.shapeArgs = {
                                  x: point.plotX,
                                  y: point.plotY + metrics.offset + (point.options.yOffset || 0),
                                  width: barWidth,
                                  height: metrics.width
               };
               point.tooltipPos[0] += barWidth / 2;
               point.tooltipPos[1] -= metrics.width / 2;
            });
         }
      });

      /**
       * Max x2 should be considered in xAxis extremes
       */
      H.wrap(H.Axis.prototype, 'getSeriesExtremes', function (proceed) {
         var axis = this,
         dataMax = Number.MIN_VALUE;

         proceed.call(this);
         if (this.isXAxis) {
            each(this.series, function (series) {
               each(series.x2Data || [], function (val) {
                  if (val > dataMax) {
                     dataMax = val;
                  }
               });
            });
            if (dataMax > Number.MIN_VALUE) {
               axis.dataMax = dataMax;
            }
         }                
      });
   }(Highcharts));


   HC = {
         charts: {},
         chart : function(id) {
            return this.charts[id];
         },
         createNewChart :  function(id) {
            if ( this.charts[id] ) {
               throw "Id already exists"
            } else {
               this.charts[id]= new HChart(id) ;
            }
         },
         removeAllCharts: function() {

            for (name in this.charts) {

               this.charts[name].destroy();

            }

            this.charts = {};

         }
   };

   CS = {};

   GLOBALS = {};


   HC.createNewChart( 'overview' );
   HC.createNewChart( 'gene' );
   HC.createNewChart( 'evidence' );

})
