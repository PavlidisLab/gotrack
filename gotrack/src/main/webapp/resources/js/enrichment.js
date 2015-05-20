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
      PrimeFaces.widgets.similarityChartWdg.plot.replot( {resetAxes:true} );
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
   if (index==1) {
      HC.charts.similarity.resize();
   }

}
function enrichmentChartHide() {
   HC.charts.enrichment.destroy();
   HC.charts.enrichmentMaster.destroy();
}

function showTerminal() {
   PF('terminalDialogWdg').show();
   PF('terminalWdg').focus();
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
         this.destroy();
      } catch (e) {
         console.log(e);
      } finally {
         this.chart = new Highcharts.Chart(options);
         this._exists=true;
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
   console.log(args);

   var options = {
                  chart: {
                     renderTo: 'hc_similarity_container',
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
                        return '<b>'+this.series.name+'</b><br />' + new Date(this.x).toLocaleDateString() + "<br> index: " + this.y;
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
   if (!utility.isUndefined( args.hc_data ) ){
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
   

   HC.charts.similarity.options = options;
   HC.charts.similarity.recreate(options);
   PF('tableEnrichmentWdg').filter()
   }

}

function handleGraphSelected(xhr, status, args) {
   console.log(args);
   
   var dateToEdition = JSON.parse(args.hc_dateToEdition);
   
   var options = {};
   options.chart = {
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
                 };
   options.title = { text: args.hc_title };
   options.subtitle = {
      text: 'Select an area by dragging across the lower chart'
  };
   options.xAxis = {
                    type: 'datetime',
                    title: {
                       text: args.hc_xlabel
                    },
                    minRange: 60 * 24 * 3600000 // fourteen days
                 };
   options.yAxis = {};
   options.plotOptions = {
                          series : {
                             states : {
                                        hover: {
                                           lineWidth: 2
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
                                },
                                mouseOver: function() {
                                   var item = this.legendItem;
                                   Highcharts.each(this.chart.series, function(series, i) {
                                       if(series.legendItem !== item && series.visible) {
                                           series.legendItem.css({
                                               color: 'grey' 
                                           });
//                                           series.legendLine.attr({
//                                               stroke: 'grey' 
//                                           });
//                                           series.legendSymbol.attr({
//                                               fill: 'grey' 
//                                           });
                                       }
                                   });
                                   
                               },
                               mouseOut: function() {
                                   Highcharts.each(this.chart.series, function(series, i) {
                                       if(series.visible) {
                                           series.legendItem.css({
                                               color: 'black' 
                                           });
//                                           series.legendLine.attr({
//                                               stroke: series.color 
//                                           });
//                                           series.legendSymbol.attr({
//                                               fill: series.color
//                                           });
                                       }
                                   });
                               }
                             }
                          }
                       };
   options.tooltip = {};
   options.legend = {
                     enabled:true,
                     align : 'right',
                     verticalAlign: 'top',
                     layout: 'vertical',
                     y:20
                  };
   options.series = [];
   options.colors = MAXIMALLY_DISTINCT_COLORS;
   options.exporting = { csv: { dateFormat: '%Y-%m-%d' } };
   
   if ( args.hc_type == "pvalue") {
      options.plotOptions.series.point = {
                                             events: {
                                                click: function () {
                                                   fetchTermInformation([{name:'termId', value:this.series.name},{name:'edition', value:dateToEdition[this.x]} ]);
                                                },
                                             }
                                          };
      options.yAxis = {
                       type: 'logarithmic',
                       reversed: true,
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
      };
      options.plotOptions.series.lineWidth = 0.8;
      options.tooltip = {
                         headerFormat: '<b>{series.name}</b><br />',
                         pointFormat: 'x = {point.x}, y = {point.y}',
                         formatter:function(){
                            return '<b>'+this.series.name+'</b><br/> Date: ' + new Date(this.x).toLocaleDateString() + "<br/> Edition: " + dateToEdition[this.x] + "<br/> p-value: " + this.y;
                         }
      };
   } else {
      //rank
      var maxRank = args.hc_maxRank;

      var opacity = Math.min(10/args.hc_data.series.length+1/100,0.1);
      //var opacity = Math.min(10/maxRank+1/100,0.1);

      var dateToMaxSigRank = JSON.parse(args.hc_dateToMaxSigRank);
      var topN = args.hc_topN;
      var outsideTopNCheck = args.hc_outsideTopNCheck;
      var insignificantCheck = args.hc_insignificantCheck;

      options.plotOptions.series.point = {
                                          events: {
                                             click: function () {
                                                fetchTermInformation([{name:'termId', value:this.series.name},{name:'edition', value:dateToEdition[this.x]}, {name:'value', value:utility.roundHalf(this.y)} ]);
                                             }
                                          }
                                       };
      options.yAxis = {
                       type: 'linear',
                       reversed: true,
                       lineColor: 'black',
                       lineWidth: 2,
                       gridLineWidth : 0,
                       offset: 10,
                       tickInterval:1,
                       min:0,
                       allowDecimals: false,
                       title: {
                          text: args.hc_ylabel
                       },
                       labels: {
                          formatter: function () {
                             return this.value;
                          }
                       }
      };
      options.plotOptions.series.shadow = {opacity:opacity};
      options.plotOptions.series.lineWidth = 0;

      options.tooltip = {
                         headerFormat: '<b>{series.name}</b><br />',
                         pointFormat: 'x = {point.x}, y = {point.y}',
                         formatter:function(){
                            return '<b>'+this.series.name+'</b><br/> Date: ' + new Date(this.x).toLocaleDateString() + "<br/> Edition: " + dateToEdition[this.x] + "<br/> Relative Rank: " + ( this.y >= dateToMaxSigRank[this.x] ? "Insignificant": utility.roundHalf(this.y) );
                         }
      };
      
      if (outsideTopNCheck || insignificantCheck ) {

         var tempPoints = [];

         for (var key in dateToMaxSigRank) {
            var r = dateToMaxSigRank[key];
            tempPoints.push([parseInt(key), r-0.5]);
         }

         tempPoints.sort(function(a,b) {return a[0] - b[0]} );

         polygonPoints = [];

         for (var i = 0; i < tempPoints.length; i++) {
            var p1 = tempPoints[i];
            var r1 = p1[1];
            polygonPoints.push([ p1[0], r1]);
            if ( i < tempPoints.length - 1 ) {

               var p2 = tempPoints[i+1];
               var midPoint = ( p1[0] + p2[0] ) / 2


               var r2 = p2[1];


               polygonPoints.push([ midPoint, r1]);
               polygonPoints.push([ midPoint, r2]);

            }
         }
         if (insignificantCheck ) {
            var s = {name:"Insignificant Region", 
                     type: 'polygon', 
                     data: polygonPoints.slice(), 
                     color: Highcharts.Color(MAXIMALLY_DISTINCT_COLORS[2]).setOpacity(0.2).get(),
                     enableMouseTracking: false,
                     includeInCSVExport: false};
//            for (var i = polygonPoints.length-1; i >= 0; i--) {
//               var p = polygonPoints[i];
//               s.data.push([ p[0], maxRank +0.5 ])
//            }
            var p = polygonPoints[polygonPoints.length-1];
            s.data.push([ p[0], maxRank +0.5 ])
            var p = polygonPoints[0];
            s.data.push([ p[0], maxRank +0.5 ])

            options.series.push(s);
            //console.log(s);
         }
         if (outsideTopNCheck ) {
            
            // This loop is to flatten regions that have been erroneously attributed to be significant because
            // the region of insignificance extended lower (less magnitude) than topN
            for (var i = 0; i < polygonPoints.length; i++) {
               var barrierPoint = polygonPoints[i];
               if ( barrierPoint[1] < topN - 0.5 ) {
                     console.log(i)
                  polygonPoints[i][1] = topN - 0.5;
               }
               
            }
            
            s = {name:"Outside Top " + topN, 
                 type: 'polygon', 
                 data: polygonPoints.slice(), 
                 color: Highcharts.Color(MAXIMALLY_DISTINCT_COLORS[0]).setOpacity(0.2).get(),
                 enableMouseTracking: false,
                 includeInCSVExport: false};
//            for (var i = polygonPoints.length-1; i >= 0; i--) {
//               var p = polygonPoints[i];
//               s.data.push([ p[0], topN - 0.5 ])
//            }
            var p = polygonPoints[polygonPoints.length-1];
            s.data.push([ p[0], topN - 0.5 ])
            var p = polygonPoints[0];
            s.data.push([ p[0], topN - 0.5 ])
            options.series.push(s);
            //console.log(s);
         }
      }

//    for (var i = polygonPoints.length-1; i >=0; i--) {
//    var p = polygonPoints[i];
//    s.data.push([ p[0], insigRank +5 ]);
//    }



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
      });

   }
   
   
   // create the detail chart
   HC.charts.enrichment.options = options;
   HC.charts.enrichment.recreate(options);  
   
   // create the master chart
   var optionsCopy = $.extend(true, {}, options);
   optionsCopy.chart.renderTo = 'hc_enrichmentMaster_container';
   optionsCopy.chart.reflow = false;
   optionsCopy.chart.borderWidth= 0;
   optionsCopy.chart.backgroundColor= null;
   optionsCopy.chart.marginLeft= 50;
   optionsCopy.chart.marginRight= 20;
   optionsCopy.chart.zoomType= 'x';
   optionsCopy.chart.events = {

                               // listen to the selection event on the master chart to update the
                               // extremes of the detail chart
                               selection: function (event) {
                                  var extremesObject = event.xAxis[0],
                                  min = extremesObject.min,
                                  max = extremesObject.max,
                                  xAxis = this.xAxis[0];

                                  // Smooth hacks
                                  HC.charts.enrichment.chart.xAxis[0].setExtremes(min, max);
                                  HC.charts.enrichment.chart.showResetZoom();
                                  var oldOnClick = HC.charts.enrichment.chart.resetZoomButton.element.onclick;
                                  HC.charts.enrichment.chart.resetZoomButton.element.onclick = function(event) {
                                     oldOnClick();
                                     xAxis.removePlotBand('mask-selection');
                                  }
                                  
                                  
                                xAxis.removePlotBand('mask-selection');
                                xAxis.addPlotBand({
                                   id: 'mask-selection',
                                   from: min,
                                   to: max,
                                   color: 'rgba(0, 0, 150, 0.2)'
                                });
                                  

                                  return false;
                               }
                            };
   optionsCopy.title.text = null;
   optionsCopy.subtitle.text = null;
   optionsCopy.xAxis.showLastTickLabel = true;
   optionsCopy.xAxis.title = {
                                 text: null
                              };
   optionsCopy.yAxis.gridLineWidth = 0;
   optionsCopy.yAxis.labels = {enabled: false};
   optionsCopy.yAxis.title = {text: null};
   optionsCopy.yAxis.showFirstLabel = false;
   optionsCopy.yAxis.lineWidth = 0;
   optionsCopy.yAxis.plotLines = [];
   optionsCopy.tooltip = {
                          formatter: function () {
                             return false;
                          }
   };
   optionsCopy.legend = { enabled: false };
   optionsCopy.credits = { enabled: false };
   optionsCopy.plotOptions.series.enableMouseTracking = false;
   optionsCopy.plotOptions.series.marker = { enabled: false };
   optionsCopy.plotOptions.series.states = {
                                            hover: {
                                               lineWidth: 0
                                            }
                                            
   };
   optionsCopy.exporting = { enabled: false };
   
   if (outsideTopNCheck) {
      optionsCopy.series.shift();
   }
   
   if (insignificantCheck) {
      optionsCopy.series.shift();
   }

   HC.charts.enrichmentMaster.options = optionsCopy;
   HC.charts.enrichmentMaster.recreate(optionsCopy); 
   
}


function enrichmentChartDlgResize() {
   HC.charts.enrichment.resize();
   HC.charts.enrichmentMaster.resize();
}

/**
 * On top of each column, draw a zigzag line where the axis break is.
 */
function pointBreakColumn(e) {    
    var point = e.point,
        brk = e.brk,
        shapeArgs = point.shapeArgs,
        x = shapeArgs.x,
        y = this.toPixels(brk.from, true),
        w = shapeArgs.width,
        key = ['brk', brk.from, brk.to],
        path = ['M', x, y, 'L', x + w * 0.25, y + 4, 'L', x + w * 0.75, y - 4, 'L', x + w, y];
    
    if (!point[key]) {
        point[key] = this.chart.renderer.path(path)
            .attr({
                'stroke-width': 3,
                stroke: point.series.options.borderColor
            })
            .add(point.graphic.parentGroup);
    } else {
        point[key].attr({
            d: path
        });
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
         }
   }

   HC.createNewChart( 'similarity' );
   HC.createNewChart( 'enrichment' );
   HC.createNewChart( 'enrichmentMaster' );

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

       Highcharts.each(this.breakArray || [], function (brk) {
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
       });
       return path;
   });




})