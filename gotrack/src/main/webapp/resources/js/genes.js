var MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff80", "#ffff00", "#ff5005"]

function onLoad() {
   $("#right-toggler").append('<span class="vertical toggled-header">Functionality</span>');
}

function centerResize() {
   try {
      var index = PrimeFaces.widgets.centerTabWdg.getActiveIndex();
      tabShowed(index);
      PrimeFaces.widgets.funcTable.render();
   } catch (e) {
      console.log(e);
   }
}

function tabShowed(index) {
   try {
      if (index==0) {
         HC.charts.annotation.resize();
      } else if (index==1) {
         HC.charts.similarity.resize();
      } else if (index==2) {
         HC.charts.multi.resize();
      } else if (index==3) {
         HC.charts.lossgain.resize();
      }
   } catch(e) {
      console.log(e);
   }
}

function timelineDlgHide() {
   HC.charts.timeline.destroy();
}

function timelineDlgResize() {
   HC.charts.timeline.resize();
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
            console.log("Failed to create chart",e);
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

function handleFetchData(xhr, status, args) {
   if (!utility.isUndefined(args.dateToEdition)) {
      GLOBALS.dateToEdition = JSON.parse(args.dateToEdition);
   }

}

function fetchCharts() {
   HC.destroyAllCharts();
   fetchAnnotationChart();
   fetchSimilarityChart();
   fetchMultiChart();
   fetchLossGainChart();
}

function handleFilterCharts(xhr, status, args) {
   console.log(args);
   if (args.hc_filtered) {
      fetchCharts();
   }
}

function handleFetchAnnotationChart(xhr, status, args) {
   console.log('handleFetchAnnotationChart', args);
   try {
      $('#loading-spinner-annotation').hide();
   } catch(e) {
      console.log(e);
   }

   var options = createGenericLineChart('hc_annotation_container', args, 0, null);

   options.plotOptions.series.point = {
                                       events: {
                                          click: function () {
                                             fetchAnnotationPointData([{name:'edition', value:GLOBALS.dateToEdition[this.x]} ]);
                                          }
                                       }
   }
   
   options.chart.events = {
         click: function(event) {
            fetchAnnotationPointData([{name:'edition', value:GLOBALS.dateToEdition[this.hoverPoint.x]} ]);
         }
   }
      
   HC.charts.annotation.options = options;
   HC.charts.annotation.recreate(options);
}

function handleFetchSimilarityChart(xhr, status, args) {
   console.log('handleFetchSimilarityChart', args);
   try {
      $('#loading-spinner-similarity').hide();
   } catch(e) {
      console.log(e);
   }
   var options = createGenericLineChart('hc_similarity_container', args, 0, 1);
   
   options.yAxis.endOnTick = false; // Make sure log axis follows our given max
   
   HC.charts.similarity.options = options;
   HC.charts.similarity.recreate(options);
}

function handleFetchMultiChart(xhr, status, args) {
   console.log('handleFetchMultiChart', args);
   try {
      $('#loading-spinner-multifunctionality').hide();
   } catch(e) {
      console.log(e);
   }  
   
   var options = createGenericLineChart('hc_multi_container', args, 0, null);
   //options.chart.type = 'area';
   //options.plotOptions.area = {fillColor: {
//   options.plotOptions.line = {color: {
//      linearGradient: [ 0,0,0,500],
//      stops: [
//          [0, MAXIMALLY_DISTINCT_COLORS[0]],
//          [1, MAXIMALLY_DISTINCT_COLORS[2]]
//      ]
//  }};
   
   HC.charts.multi.options = options;
   HC.charts.multi.recreate(options);
}

function handleFetchLossGainChart(xhr, status, args) {
   console.log('handleFetchLossGainChart', args);
   try {
      $('#loading-spinner-lossgain').hide();
   } catch(e) {
      console.log(e);
   } 
   
   var options = createGenericLineChart('hc_lossgain_container', args);
   options.chart.type = 'column';
   options.plotOptions.series.stacking = 'normal';

   options.plotOptions.column = {
                                 states: {
                                    hover: {
                                        borderColor: 'yellow'
                                    }
                                }
   };
   
   for (var i = 0; i < args.hc_data.series.length; i++) {
      options.series[i].stack = args.hc_data.series[i].extra;
   }
   
   // Remove axis scale toggle, because negative values and log don't mix
   options.exporting = {
                        enabled: true,
                        sourceWidth  : 1600,
                        sourceHeight : 900,
                        csv: {
                           dateFormat: '%Y-%m-%d'
                        }
   };
   
   // Click event functionality
   options.plotOptions.series.point = {
                                       events: {
                                          click: function () {
                                             fetchLossGainPointData([{name:'edition', value:GLOBALS.dateToEdition[this.x]} ]);
                                          }
                                       }
   }
   
   options.chart.events = {
         click: function(event) {
            fetchLossGainPointData([{name:'edition', value:GLOBALS.dateToEdition[this.hoverPoint.x]} ]);
         }
   }
   
   HC.charts.lossgain.options = options;
   HC.charts.lossgain.recreate(options);
}

function handleFetchTimeline(xhr, status, args) {
   console.log('handleFetchTimeline', args);
   
   if (!args.hc_success) {
      console.log(args.hc_info);
      $('#hc_timeline_container').empty();
      $('#hc_timeline_container').append( "<p>"+args.hc_info+"</p>" );
      return;
   }
   
   var tmp = JSON.parse(args.hc_category_positions);
   var evidenceCategories = new Array(tmp.length);
   
   // Inverse the map
   for ( var cat in tmp) {
      evidenceCategories[tmp[cat]] = cat;
   }
   
   
   var categories = [];
   var tooltipData = {};
   for (var i = 0; i < args.hc_data.series.length; i++) {
      categories.push(args.hc_data.series[i].name);
      tooltipData[i] = {};
   }
   
   var termNames = JSON.parse(args.hc_term_names);
   
   
   var options = createGenericLineChart('hc_timeline_container', args);
   options.chart.type = 'xrange';
   options.chart.zoomType = 'xy';
   options.yAxis =  {
      categories: categories,
      min:0,
      max:categories.length-1,
      title: '',
      labels: {
         formatter: function () {
            return this.value;
         }
      }
   };
   options.plotOptions.series.grouping = false;
   options.plotOptions.series.turboThreshold = 0;
   options.plotOptions.series.cropThreshold = 100000;
   options.tooltip.shared = false;
   //options.legend.enabled = false;
   options.series = []; // Not optimal as we're recreating for no reason, change later
   //options.colors = [];
   
   // Remove axis scale toggle, because negative values and log don't mix
   options.exporting = {
                        enabled: true,
                        sourceWidth  : 1600,
                        sourceHeight : 900,
                        csv: {
                           dateFormat: '%Y-%m-%d'
                        }
   };
   
   options.plotOptions.series.point = {
                                       events: {
                                          click: function () {
                                             fetchTimelinePointData([{name:'edition', value:GLOBALS.dateToEdition[this.x]}, {name:'termId', value:categories[this.y]} ]);
                                          }
                                       }
   }  
   
   options.yAxis.labels.formatter = function() {
      var name = termNames[this.value];
      var trimName = name;
      if (name.length > 25) {
         trimName = name.substring(0, 22) + "...";
      }
      return '<span title="'+name+'"><div style="text-align: right;">' + this.value + '</div><div style="text-align: right;">' + trimName + '</div></span>'
      //return '<span title="'+termNames[this.value]+'">' + this.value + '</span><span>'+termNames[this.value]+'</span>';
   };
   options.yAxis.labels.useHTML = true;
   
   var height = $('#hc_timeline_container').height();
   var pointWidth = 5;
   if (height != null) {
      var pointWidth = (height - 100) / categories.length / evidenceCategories.length / 2 - 2
      pointWidth = (pointWidth < 0.5 ? 0.5 : pointWidth);  
   }
   

   
   for (var k = 0; k < evidenceCategories.length; k++) {
      var cat = evidenceCategories[k];
      options.series.push({
         name : cat,
         pointWidth: pointWidth,
         data : []
      });
   }
   
   for (var i = 0; i < args.hc_data.series.length; i++) {
      var series = args.hc_data.series[i];
      var name = series.name;
      var data = []
      var tData = tooltipData[i];
      for (var j = 0; j < series.data.length; j++) {
         var point = series.data[j];
         var nextPointX = (j > series.data.length - 2) ? point.x + 2629740000: series.data[j+1].x; //get next edition or add month
         
         var tArr = [];
         
         for (var k = 0; k < evidenceCategories.length; k++) {
            var cat = evidenceCategories[k];
            var mask = 1 << k;
            if ((point.y & mask) != 0) {
               // bit is set
               options.series[k].data.push({x:point.x,x2:nextPointX, y:i,name:cat,yOffset: pointWidth*k });
               tArr.push(k);
             } else {
               // bit is not set
             }
         }
         
         tData[point.x] = tArr;
      }
      
   }
   
   options.tooltip.formatter = function() {
      var s = '<b>' + Highcharts.dateFormat('%b %Y',
         new Date(this.x)) + '</b>';
      
      var data = tooltipData[this.y][this.x];
      
      for (var i = 0; i < data.length; i++) {
         var k = data[i];
         s += '<br/>' + evidenceCategories[k]
      }

      return s;
   }
//   options.tooltip.positioner = function () {
//      return { x: 80, y: 50 };
//   }
   
   HC.charts.timeline.options = options;
   HC.charts.timeline.recreate(options);
   
}

function createGenericLineChart(renderTo, args, baseMin, baseMax) {
   var options = {
                  chart: {
                     renderTo: renderTo,
                     zoomType: 'x',
                     resetZoomButton: {
                        position: {
                           // align: 'right', // by default
                           // verticalAlign: 'top', // by default
                           x: -30,
                           y: -35
                        }
                     },
//                     events: {
//                        click: function(event) {
//                           fetchSimilarityInformation([{name:'edition', value:dateToEdition[this.hoverPoint.x]} ]);
//                     }
//                     }
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
                     labels: {
                        formatter: function () {
                           return this.value;
                        }
                     },
                     min: baseMin,
                     max: baseMax,
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
                     },
                     pointFormatter:function(){
                        return '<span style="color:'+this.color+'">\u25CF</span> '+this.series.name+': <b>'+Number(this.y.toPrecision(5))+'</b><br/>';
                        }
                  },
                  legend : {
//                     align : 'right',
//                     verticalAlign: 'top',
//                     layout: 'vertical',
//                     y:20
                  },

                  series: [],

                  colors : MAXIMALLY_DISTINCT_COLORS,

                  exporting: {
                     sourceWidth: 1600,
                     sourceHeight: 900,
                     csv: {
                        dateFormat: '%Y-%m-%d'
                     },
                     buttons: {
                        scaleToggle: {
                           align:'left',
                           //verticalAlign:'middle', 
                           x: 20, 
                           onclick: function () {
                              // The toggling of the text is not using an official API, can break with version update!
                             if (this.yAxis[0].isLog) {
                                this.exportSVGElements[3].element.nextSibling.innerHTML = "Linear";
                                this.yAxis[0].update({ type: 'linear', min:baseMin, max:baseMax});
                             } else {
                                this.exportSVGElements[3].element.nextSibling.innerHTML = "Log";
                                this.yAxis[0].update({ type: 'logarithmic', min: null, max:baseMax});
                             }
                             
                           },
                           symbol: 'circle',
                           symbolFill: '#bada55',
                           symbolStroke: '#330033',
                           symbolStrokeWidth: 1,
                           _titleKey: 'axis_toggle', 
                           text: 'Linear'
                        }
                     }
               },
               lang : {
                       axis_toggle: 'Toggle Axis Type: Logarithmic/Linear'
               }
                  
   }
   
   if (!utility.isUndefined( args.hc_data ) ){
      //args.hc_gene_data.series.sort(function(a,b) {return (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0);} );
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
   
   }
   return options;
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
            
         },
         destroyAllCharts: function() {
            
            for (name in this.charts) {
               
               this.charts[name].destroy();
               
            }
            
         }
   };
   
   GLOBALS = {};
      

   HC.createNewChart( 'annotation' );
   HC.createNewChart( 'similarity' );
   HC.createNewChart( 'multi' );
   HC.createNewChart( 'lossgain' );
   HC.createNewChart( 'timeline' );
   
})