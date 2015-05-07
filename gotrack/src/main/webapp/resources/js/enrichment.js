var MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff80", "#ffff00", "#ff5005"]

function onLoad() {
   $("#left-toggler").append('<span class="vertical toggled-header">Options</span>');
}

var hideLoadingSpinner = function() {
   $('.loading-spinner').hide();   
};


var showLoadingSpinner = function() {
   $('.loading-spinner').show();   
};

//$(window).load(function(){  // should be  $(window).load to load widget
//$('#slder').slider({
//step: 0.1
//});
//});

function centerResize() {
   //updateCenterPanel();
   try {
      PrimeFaces.widgets.stabilityChartWdg.plot.replot( {resetAxes:true} );
   } catch (e) {

   }
}

function escDialog() {
   $(document).keyup(function(e) {
      if (e.keyCode == 27) { // esc code is 27 
         closeAllDialog() ;
      }   
   });
}

function closeAllDialog() {
   for (var propertyName in PrimeFaces.widgets) {
      if (PrimeFaces.widgets[propertyName] instanceof PrimeFaces.widget.Dialog ||
         PrimeFaces.widgets[propertyName] instanceof PrimeFaces.widget.LightBox) {
         PrimeFaces.widgets[propertyName].hide();
      }
   }
}

function postAjaxSortTable(datatable) {
   var selectedColumn = undefined;

   // multisort support
   if(datatable && datatable.cfg.multiSort) {
      if(datatable.sortMeta.length > 0) {
         var lastSort = datatable.sortMeta[datatable.sortMeta.length-1];
         selectedColumn = $(document.getElementById(lastSort.col));
      }
   } else {
      selectedColumn = datatable.jq.find('.ui-state-active');
   }

   // no sorting selected -> quit
   if(!selectedColumn || selectedColumn.length <= 0) {
      return;
   }

   var sortorder = selectedColumn.data('sortorder')||"DESCENDING";
   datatable.sort(selectedColumn, sortorder, datatable.cfg.multiSort);
}

function tabChanged(index)
{
   alert("Tab Changed:"+index);
}
function tabShowed(index)
{
   if (index==0) {
      HC.charts.stability.resize();
   }

}

function HChart(id) {
   this.id = id;
   this.chart = null;
   this.options = {};
   this.create = function() {
      if (this.chart === null) {
         this.chart = new Highcharts.Chart(this.options);
      } else {
         console.log('Chart already created');
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
         this.chart.destroy();
      } catch (e) {
         console.log(e);
      } finally {
         this.chart = new Highcharts.Chart(options);
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

function handleEnrichmentComplete(xhr, status, args) {
   console.log(args.hc_data);
   console.log(args.hc_title);
   console.log(args.hc_xlabel);
   console.log(args.hc_ylabel);

   var options = {
                  chart: {
                     renderTo: 'hc_stability_container',
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           // align: 'right', // by default
                           // verticalAlign: 'top', // by default
                           x: -10,
                           y: -30
                        }
                     }
                  },
                  title: {
                     text: args.hc_title
                  },

                  xAxis: {
                     type: 'datetime',
                     title: {
                        text: args.hc_xlabel
                     },
                     minRange: 60 * 24 * 3600000 // fourteen days
                  },

                  yAxis: {
                     type: 'linear',
                     title: {
                        text: args.hc_ylabel
                     },
                     max:1,
                     min:0,
                     minorTickInterval: 0.05,
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
                                 alert('Term: ' + this.series.name + ', score: ' + this.y);
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
                     headerFormat: '<b>{series.name}</b><br />',
                     pointFormat: 'x = {point.x}, y = {point.y}',
                     formatter:function(){
                        return '<b>'+this.series.name+'</b><br />' + new Date(this.x).toLocaleDateString() + "<br> score: " + this.y;
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

   for (var i = 0; i < args.hc_data.series.length; i++) {
      var series = args.hc_data.series[i];
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

   HC.charts.stability.options = options;
   HC.charts.stability.recreate(options);

}

function handleGraphSelected(xhr, status, args) {
   console.log(args.hc_data);
   console.log(args.hc_data2);
   
   var options = {};
   if ( args.hc_type == "pvalue") {
      options = {
                 chart: {
                    renderTo: 'hc_enrichment_container',
                    zoomType: 'x',
                    resetZoomButton: {
                       position: {
                          // align: 'right', // by default
                          // verticalAlign: 'top', // by default
                          x: -10,
                          y: -30
                       }
                    }
                 },
                 title: {
                    text: args.hc_title
                 },

                 xAxis: {
                    type: 'datetime',
                    title: {
                       text: args.hc_xlabel
                    },
                    minRange: 60 * 24 * 3600000 // fourteen days
                 },

                 yAxis: {
                    type: 'logarithmic',
                    title: {
                       text: args.hc_ylabel
                    },
                    max:1,
                    minorTickInterval: 0.1,
                    labels: {
                       formatter: function () {
                          return this.value;
                       }
                    },
                    plotLines : [{
                       value : args.hc_threshold,
                       color : 'black',
                       dashStyle : 'shortdash',
                       width : 2,
                       label : {
                          text : 'Threshold'
                       },
                       zIndex: 5
                    }]
                 },

                 plotOptions : {
                    series : {
                       point: {
                          events: {
                             click: function () {
                                alert('Term: ' + this.series.name + ', p-value: ' + this.y);
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
                    headerFormat: '<b>{series.name}</b><br />',
                    pointFormat: 'x = {point.x}, y = {point.y}',
                    formatter:function(){
                       return '<b>'+this.series.name+'</b><br />' + new Date(this.x).toLocaleDateString() + "<br> p-value: " + this.y;
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
   } else {
      // rank
      
      var maxRank = -1;
      for (var i = 0; i < args.hc_data.series.length; i++) {
         var series = args.hc_data.series[i];
         for (var j = 0; j < series.data.length; j++) {
            var point = series.data[j];
            if (point.y > maxRank) {
               maxRank = point.y;
            }
         }
      }
      var opacity = Math.min(10/args.hc_data.series.length+1/100,0.1);
      //var opacity = Math.min(10/maxRank+1/100,0.1);
      var breakAt = args.hc_breakAt;
      
      var gap = Math.ceil((maxRank - breakAt)/5)
      
     var breaks = []
      for (var i = breakAt ; i<maxRank;i+=gap) {
          breaks.push({from:i,to:i+gap-0.01,breakSize:2})
      }
      console.log(breaks)
      
      options = {
                 chart: {
                    renderTo: 'hc_enrichment_container',
                    zoomType: 'xy',
                    resetZoomButton: {
                       position: {
                          // align: 'right', // by default
                          // verticalAlign: 'top', // by default
                          x: -10,
                          y: -30
                       }
                    }
                 },
                 title: {
                    text: args.hc_title
                 },

                 xAxis: {
                    type: 'datetime',
                    title: {
                       text: args.hc_xlabel
                    },
                    minRange: 60 * 24 * 3600000 // fourteen days
                 },

                 yAxis: {
                    type: 'linear',
                    lineColor: 'black',
                    lineWidth: 2,
                    offset: 10,
                    tickInterval:1,
                    min:0,
                    max:breaks[breaks.length - 1].to,
                    allowDecimals: false,
                    title: {
                       text: args.hc_ylabel
                    },
                    labels: {
                       formatter: function () {
                          return this.value;
                       }
                    },
                    breaks: breaks,
                 },

                 plotOptions : {
                    series : {
                       shadow: {opacity:opacity},
                       lineWidth:0,
                       point: {
                          events: {
                             click: function () {
                                alert('Term: ' + this.series.name + ', score: ' + this.y);
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
                    headerFormat: '<b>{series.name}</b><br />',
                    pointFormat: 'x = {point.x}, y = {point.y}',
                    formatter:function(){
                       return '<b>'+this.series.name+'</b><br />' + new Date(this.x).toLocaleDateString() + "<br> score: " + this.y;
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
   }


   for (var i = 0; i < args.hc_data.series.length; i++) {
      var series = args.hc_data.series[i];
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


   HC.charts.enrichment.options = options;
   HC.charts.enrichment.recreate(options);  

}


function enrichmentChartDlgResize() {
   HC.charts.enrichment.resize();
}

$(document).ready(function() {
   escDialog();

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
         }
   }

   HC.createNewChart( 'stability' );
   HC.createNewChart( 'enrichment' );

   // This self-executing anon func creates a resize event on the enrichment chart dialog
   // that will only run once the resize event has stopped
   (function(){
      var id;
      $("#enrichmentChartDlg").resize(function() {
         clearTimeout(id);
         id = setTimeout(enrichmentChartDlgResize, 200);
      });
   })();
   
   /**
    * Extend the Axis.getLinePath method in order to visualize breaks with two parallel
    * slanted lines. For each break, the slanted lines are inserted into the line path.
    */
   Highcharts.wrap(Highcharts.Axis.prototype, 'getLinePath', function (proceed, lineWidth) {
       var axis = this,
           path = proceed.call(this, lineWidth),
           x = path[1];
       var breakArray = this.breakArray || [];
      
       if (breakArray.length) {
          var brk = breakArray[0];
          var from;
          if (!axis.horiz) {
              y = axis.toPixels(brk.from);
              path.splice(3, 0, 
                  'L', x, y - 4, // stop
                  'M', x + 5, y - 9, 'L', x - 5, y + 1, // lower slanted line
                  'M', x + 5, y - 1, 'L', x - 5, y + 9, // higher slanted line
                  'M', x, y + 4
              );
          }
       }
       return path;
   });




})