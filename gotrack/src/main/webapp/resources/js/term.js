var MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff80", "#ffff00", "#ff5005"]

function onLoad() {}

function tabShowed(index) {
   try {
      if (index==0) {
         CS.overview.graph.resize();
      } else if (index==1) {
         HC.charts.overview.resize();
      } else if (index==2) {
         HC.charts.gene.resize();
      } else if (index==3) {
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
   CS.overview = createVisGraph(args, '#currentAncestry', function() {

      CS.overview.graph.nodes().lock();
      //CS.overview.graph.panningEnabled( false );
      //CS.overview.graph.zoomingEnabled( false );
      CS.overview.graph.boxSelectionEnabled( false );       
   });
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
//   
//   revertGraph(CS.overview, function() {
//      CS.overview.graph.nodes().lock();
//      CS.overview.graph.panningEnabled( false );
//      CS.overview.graph.zoomingEnabled( false );
//      CS.overview.graph.boxSelectionEnabled( false );
//   });
//   
//}


function revertGraph(cs, callback) {
   var new_cs = createVisGraph(cs.args, cs.selector,callback)
   cs.graph = new_cs.graph;
   cs.diff = false;
}


function handleFetchDiff(xhr, status, args) {
   console.log(args);
   revertGraph(CS.overview,function(){showDiff(CS.overview, args)})
   //showDiff(CS.overview, args)
}

function handleFetchGraphDialog(xhr, status, args) {
   console.log(args);
   CS.dialogGraph = createVisGraph(args, "#cytoDialog", function(){showDiff(CS.dialogGraph, args)});
   
}


function createVisGraph(args, selector, callback) {
   
   if (utility.isUndefined(callback)) {
         callback = function(){};
   }

   
   var eles = JSON.parse(args.cyto_graph);

   var rawnodes = eles.nodes;
   var rawedges = eles.edges;
   
   var nodes = [];
   var edges= [];
      
   for (var i = 0; i < rawnodes.length; i++) {
      var n = rawnodes[i];
      var label = utility.wordwrap(n.label.substring(0, 30) + ( (n.label.length > 30) ? "...": "" ),15,'\n');
      nodes.push({ data: { 
         id: String(n.id), 
         label: label,
         href: "/gotrack/trends.xhtml?query=GO%3A" + utility.pad(n.id,7),
            } 
      });
   }
   
   for (var i = 0; i < rawedges.length; i++) {
      var e = rawedges[i];
      edges.push({ data: { id: String(e.from) + "," + String(e.to) , source: String(e.from), target: String(e.to), type: e.type } });
   }
   
   var elesJson = {
      nodes: nodes, 
      edges: edges
   };
   
   
   var graph = cytoscape({
      container: $(selector)[0],
      style: cytoscape.stylesheet()
      .selector('node')
        .css({
          'content': 'data(label)',
          'text-valign': 'center',
          'color': 'white',
          'text-outline-width': 2,
          'text-outline-color': '#888',
          'text-wrap': 'wrap',
          'text-max-width': 6000,
          'height':50,
          'width':120,
          'font-size':12,
          'shape': 'rectangle',
        })
        .selector('node[id="'+args.current_id+'"]')
        .css({
           'shape':'star',
           'height':80,
           'width':80,
           //'background-color':'#f00'
        })
        .selector('node[deleted=1]')
        .css({
           //'shape':'star',
           'background-color':'#f00'
        })
        .selector('node[added=1]')
        .css({
           //'shape':'star',
           'background-color':'#0f0'
        })
          .selector('edge')
            .css({
              'line-color': '#000000',
              'target-arrow-color': '#000000',
              'width': 2,
              'target-arrow-shape': 'triangle',
              'opacity': 0.8
            })
         .selector('edge[type="PART_OF"]')
            .css({
               'line-color': '#00f',
               'target-arrow-color': '#00f',
               'width': 2,
               'target-arrow-shape': 'triangle',
               'opacity': 0.8
             })
         .selector('edge[deleted=1]')
            .css({
               'line-color': '#f00',
               'target-arrow-color': '#f00',
               'width': 2,
               'target-arrow-shape': 'triangle',
               'opacity': 0.8
             })
         .selector('edge[added=1]')
            .css({
               'line-color': '#0f0',
               'target-arrow-color': '#0f0',
               'width': 2,
               'target-arrow-shape': 'triangle',
               'opacity': 0.8
             })
          .selector('.faded')
            .css({
              'opacity': 0.25,
              'text-opacity': 0
            }),
      
      elements: elesJson,
      
      layout: {
        name: 'dagre',
        rankDir: 'BT',
        nodeSep: 50,
        rankSep: 20,
      },
      
      ready: function(){
         callback();
       }

    });
   
   graph.on('tap', 'node', function(){
      try { // your browser may block popups
        window.open( this.data('href') );
      } catch(e){ // fall back on url change
        window.location.href = this.data('href'); 
      } 
    });
   
   return {graph: graph, diff: false, args: args, selector: selector}
   
   
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
               href: "/gotrack/trends.xhtml?query=GO%3A" + utility.pad(n.id,7),
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
                           // align: 'right', // by default
                           // verticalAlign: 'top', // by default
                           x: -20,
                           y: -20
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
   var options = {
                  chart: {
                     renderTo: 'hc_gene_container',
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           // align: 'right', // by default
                           // verticalAlign: 'top', // by default
                           x: -10,
                           y: -30
                        }
                     },
//                     events: {
//                        click: function(event) {
//                           fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.hoverPoint.x]} ]);
//                     }
//                     }
                  },
                  title: {
                     text: args.hc_gene_title
                  },

                  xAxis: {
                     type: 'datetime',
                     title: {
                        text: args.hc_gene_xlabel
                     },
                     minRange: 60 * 24 * 3600000 // fourteen days
                  },

                  yAxis: {
                     type: 'linear',
                     title: {
                        text: args.hc_gene_ylabel
                     },
                     labels: {
                        formatter: function () {
                           return this.value;
                        }
                     }
                  },

                  plotOptions : {
                     series : {
//                        point: {
//                           events: {
//                              click: function () {
//                                 fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.x]} ]);
//                              }
//                           }
//                        },
                        events: {
                           legendItemClick: function(event) {

                              var defaultBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;

                              if (!defaultBehaviour) {

                                 var seriesIndex = this.index;
                                 var series = this.chart.series;

                                 var reset = this.isolated;


                                 for (var i = 0; i < series.length; i++)
                                 {
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
                     csv: {
                        dateFormat: '%Y-%m-%d'
                     }
                  }
   }
   
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
            data : data
         })
   
      }
   

   HC.charts.gene.options = options;
   HC.charts.gene.recreate(options);
   
   }
}

function createEvidenceCountChart(args) {
   var options = {
                  chart: {
                     renderTo: 'hc_evidence_container',
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           // align: 'right', // by default
                           // verticalAlign: 'top', // by default
                           x: -10,
                           y: -30
                        }
                     },
//                     events: {
//                        click: function(event) {
//                           fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.hoverPoint.x]} ]);
//                     }
//                     }
                  },
                  title: {
                     text: args.hc_evidence_title
                  },

                  xAxis: {
                     type: 'datetime',
                     title: {
                        text: args.hc_evidence_xlabel
                     },
                     minRange: 60 * 24 * 3600000 // fourteen days
                  },

                  yAxis: {
                     type: 'linear',
                     title: {
                        text: args.hc_evidence_ylabel
                     },
                     labels: {
                        formatter: function () {
                           return this.value;
                        }
                     }
                  },

                  plotOptions : {
                     series : {
//                        point: {
//                           events: {
//                              click: function () {
//                                 fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.x]} ]);
//                              }
//                           }
//                        },
                        events: {
                           legendItemClick: function(event) {

                              var defaultBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;

                              if (!defaultBehaviour) {

                                 var seriesIndex = this.index;
                                 var series = this.chart.series;

                                 var reset = this.isolated;


                                 for (var i = 0; i < series.length; i++)
                                 {
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
                     csv: {
                        dateFormat: '%Y-%m-%d'
                     }
                  }
   }
   
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
   this.create = function() {
      if ( !this.exists() ) {
         this.chart = new Highcharts.Chart(this.options);
         this._exists=true;
         console.log("Chart created");
      }
   }
   this.reset = function() {
      this.recreate(this.options);
   }
   this.recreate = function(options) {
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
            this.chart = new Highcharts.Chart(options);
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
