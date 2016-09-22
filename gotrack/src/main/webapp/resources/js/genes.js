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
         plotting.charts.annotation.resize();
      } else if (index==1) {
         plotting.charts.similarity.resize();
      } else if (index==2) {
         plotting.charts.multi.resize();
      } else if (index==3) {
         plotting.charts.lossgain.resize();
      }
   } catch(e) {
      console.log(e);
   }
}

function timelineDlgHide() {
   plotting.charts.timeline.destroy();
}

function timelineDlgResize() {
   plotting.charts.timeline.resize();
}

function afterRowSelection() {
   var cnt = PF('funcTable').getSelectedRowsCount();
   var wdg = PF('viewTermsWdg');
   try {
      if (cnt > 1) {
         // enable View Terms button
         wdg.enable();
      } else {
         // disable View Terms button
         wdg.disable();
      }
   } catch (e) {
      console.log(e);
   }
   
}


function handleDatatableDialog(wdgDlgId, wdgTabId, noLoop) {
   var dlg = PF(wdgDlgId);
   
   if ( dlg.isVisible() ) {
      // If the dialog is already visible, we may safely filter
      PF(wdgTabId).filter();
   } else {
      // Dialog has yet to  be shown, filtering now will error, leave it to dialog's onShow
      if (!noLoop) {
         dlg.show();
      }
      
   } 
}

function showViewAnnotationsDlg() {
   try {
   var dlg = PF('viewAnnotationsDlgWdg');
   dlg.show();
   dlg.moveToTop();
   } catch (e) {
     console.log(e); 
   }
}

function handleFetchData(xhr, status, args) {
   if (!utility.isUndefined(args.dateToEdition)) {
      GLOBALS.dateToEdition = JSON.parse(args.dateToEdition);
   }

}

function fetchCharts() {
   try {
      PF('viewTermsWdg').disable();
   } catch (e) {
      console.log(e);
   }
   
   try {
      plotting.destroyAllCharts();
      fetchAnnotationChart();
      fetchSimilarityChart();
      fetchMultiChart();
      fetchLossGainChart();
   } catch (e) {
      console.log(e);
   }
}

function handleFilterCharts(xhr, status, args) {
   console.log(args);
   if (args.filtered) {
      fetchCharts();
   }
}

function handleFetchAnnotationChart(xhr, status, args) {
   
   try {
      $('#loading-spinner-annotation').hide();
   } catch(e) {
      console.log(e);
   }
   
   try {
      args.HC = JSON.parse(args.HC);
   } catch(e) {
      console.log(e);
      return;
   }
   
   console.log('handleFetchAnnotationChart', args);

   args.HC.renderTo = 'hc_annotation_container';
   var options = plotting.defaultHCOptions(args.HC, true);
   
   options.legend = {};
   
   options.tooltip.pointFormatter = function(){
      return '<span style="color:'+this.color+'">\u25CF</span> '+this.series.name+': <b>'+utility.sigFigs(this.y, 3)+'</b><br/>';
   };

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
      
   plotting.charts.annotation.options = options;
   plotting.charts.annotation.recreate(options);
}

function handleFetchSimilarityChart(xhr, status, args) {
   
   try {
      $('#loading-spinner-similarity').hide();
   } catch(e) {
      console.log(e);
   }
   
   try {
      args.HC = JSON.parse(args.HC);
   } catch(e) {
      console.log(e);
      return;
   }
   
   console.log('handleFetchSimilarityChart', args);

   args.HC.renderTo = 'hc_similarity_container';
   var options = plotting.defaultHCOptions(args.HC, true);
   
   options.legend = {};
   
   options.tooltip.pointFormatter = function(){
      return '<span style="color:'+this.color+'">\u25CF</span> '+this.series.name+': <b>'+utility.sigFigs(this.y, 3)+'</b><br/>';
   };
   
   options.yAxis.endOnTick = false; // Make sure log axis follows our given max
   
   plotting.charts.similarity.options = options;
   plotting.charts.similarity.recreate(options);
}

function handleFetchMultiChart(xhr, status, args) {
   
   try {
      $('#loading-spinner-multifunctionality').hide();
   } catch(e) {
      console.log(e);
   }  
   
   try {
      args.HC = JSON.parse(args.HC);
   } catch(e) {
      console.log(e);
      return;
   }
   console.log('handleFetchMultiChart', args);

   args.HC.renderTo = 'hc_multi_container';
   var options = plotting.defaultHCOptions(args.HC, true);
   
   options.legend = {};
   
   options.tooltip.pointFormatter = function(){
      return '<span style="color:'+this.color+'">\u25CF</span> '+this.series.name+': <b>'+utility.sigFigs(this.y, 3)+'</b><br/>';
   };
   
   plotting.charts.multi.options = options;
   plotting.charts.multi.recreate(options);
}

function handleFetchLossGainChart(xhr, status, args) {
   
   try {
      $('#loading-spinner-lossgain').hide();
   } catch(e) {
      console.log(e);
   } 
   
   try {
      args.HC = JSON.parse(args.HC);
   } catch(e) {
      console.log(e);
      return;
   }
   
   console.log('handleFetchLossGainChart', args);
      
   args.HC.renderTo = 'hc_lossgain_container';
   var options = plotting.defaultHCOptions(args.HC, false);
   
   options.legend = {};
   
   options.chart.type = 'column';
   options.plotOptions.series.stacking = 'normal';

   options.plotOptions.column = {
                                 states: {
                                    hover: {
                                        borderColor: 'yellow'
                                    }
                                }
   };
   
   for (var i = 0; i < args.HC.data.series.length; i++) {
      options.series[i].stack = args.HC.data.series[i].extra.stack;
   }
   
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
   
   plotting.charts.lossgain.options = options;
   plotting.charts.lossgain.recreate(options);
}

function handleFetchTimeline(xhr, status, args) {
   try {
      args.HC = JSON.parse(args.HC);
   } catch(e) {
      console.log(e);
      return;
   }
   console.log('handleFetchTimeline', args);
   
   
   
   if (!args.HC.success) {
      console.log(args.HC.info);
      $('#hc_timeline_container').empty();
      $('#hc_timeline_container').append( "<p>"+args.HC.info+"</p>" );
      return;
   }
   
   var tmp = args.HC.category_positions;
   var evidenceCategories = new Array(tmp.length);
   
   // Inverse the map
   for ( var cat in tmp) {
      evidenceCategories[tmp[cat]] = cat;
   }
   
   
   var categories = [];
   var tooltipData = {};
   for (var i = 0; i < args.HC.data.series.length; i++) {
      categories.push(args.HC.data.series[i].name);
      tooltipData[i] = {};
   }
   
   var termNames = args.HC.term_names;
      
   args.HC.renderTo = 'hc_timeline_container';
   var options = plotting.defaultHCOptions(args.HC, false);
   
   options.legend = {};
   
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
      var styleTooltip = function(n, d) {
         return "<p class='name'>" + n + "</p><p class='description'>" + d + "</p>";
      };
      return '<span class="timeline-yaxis-category clearfix" title="'+styleTooltip(this.value, name)+'"><div style="text-align: right;">' + this.value + '</div><div style="text-align: right;">' + trimName + '</div></span>'
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
   
   for (var i = 0; i < args.HC.data.series.length; i++) {
      var series = args.HC.data.series[i];
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
   
   // Highcharts requires sorted data
   for (var k = 0; k < options.series.length; k++) {
      options.series[k].data.sort( function(a, b) {
         return a.x-b.x;
      });
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
   
   plotting.charts.timeline.options = options;
   plotting.charts.timeline.recreate(options, function(c) {
      $("span.timeline-yaxis-category").tipsy({ gravity: "w", opacity: 0.8, html: true });
   });
   
   
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
   
   GLOBALS = {};
      

   plotting.createNewChart( 'annotation' );
   plotting.createNewChart( 'similarity' );
   plotting.createNewChart( 'multi' );
   plotting.createNewChart( 'lossgain' );
   plotting.createNewChart( 'timeline' );
   
})