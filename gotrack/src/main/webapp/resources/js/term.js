var MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff80", "#ffff00", "#ff5005"]

function onLoad() {}


var hideLoadingSpinner = function() {
  $('#loading-spinner').hide();
 
};

function tabShowed(index) {
   if (index==1) {
      HC.charts.exist.resize();
   } else if (index==2) {
      HC.charts.gene.resize();
   }
}
   

function handleFetchCharts(xhr, status, args) {
   console.log(args);
   GLOBALS.dateToEdition = JSON.parse(args.dateToEdition);
   createExistenceChart(args);
   createGeneCountChart(args);
   CS.overview = createVisGraph(args, '#mynetwork', function() {

      CS.overview.graph.nodes().lock();
      CS.overview.graph.panningEnabled( false );
      CS.overview.graph.zoomingEnabled( false );
      CS.overview.graph.boxSelectionEnabled( false );       
   });

}

function revertOverview() {
   
   revertGraph(CS.overview, function() {
      CS.overview.graph.nodes().lock();
      CS.overview.graph.panningEnabled( false );
      CS.overview.graph.zoomingEnabled( false );
      CS.overview.graph.boxSelectionEnabled( false );
   });
   
}


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
   CS.dialogGraph = createVisGraph(args, "#cytoDialog");
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
      var label = utility.wordwrap(n.label.substring(0, 30) + ( (n.label.length > 30) ? "...": "" ),13,'\n');
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
          'height':40,
          'width':40
        })
        .selector('node[id="'+args.current_id+'"]')
        .css({
           'shape':'star',
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
        nodeSep: 100,
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

   var graph = cs.graph;
   
   graph.nodes().lock();
   var diff = JSON.parse(args.vis_diff);
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

function createExistenceChart(args) {
   var options = {
                  chart: {
                     renderTo: 'hc_exist_container',
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           // align: 'right', // by default
                           // verticalAlign: 'top', // by default
                           x: -20,
                           y: -20
                        }
                     },
                     events: {
                        click: function(event) {
                           fetchGraph([{name:'edition', value:GLOBALS.dateToEdition[this.hoverPoint.x]} ]);
                     }
                     }
                  },
                  title: {
                     text: args.hc_exist_title
                  },

                  xAxis: {
                     type: 'datetime',
                     title: {
                        text: args.hc_exist_xlabel
                     },
                     minRange: 60 * 24 * 3600000 // fourteen days
                  },

                  yAxis: {
                     type: 'linear',
                     title: {
                        text: args.hc_exist_ylabel
                     },
                     labels: {
                        formatter: function () {
                           return this.value;
                        }
                     }
                  },

                  plotOptions : {
                     series : {
                        point: {
                           events: {
                              click: function () {
                                 fetchGraph([{name:'edition', value:GLOBALS.dateToEdition[this.x]} ]);
                              }
                           }
                        },
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
                     enabled: false,
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
   
   if (!utility.isUndefined( args.hc_exist_data ) ){
      for (var i = 0; i < args.hc_exist_data.series.length; i++) {
         var series = args.hc_exist_data.series[i];
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
   

   HC.charts.exist.options = options;
   HC.charts.exist.recreate(options);
   
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
      

   HC.createNewChart( 'exist' );
   HC.createNewChart( 'gene' );
   
})
