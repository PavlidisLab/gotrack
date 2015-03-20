var plot;

var hideLoadingSpinner = function() {
  $('#loading-spinner').hide();
  //console.log($("#left-toggler"))
  $("#left-toggler").append('<span class="vertical toggled-header">Options</span>');
  $("#right-toggler").append('<span class="vertical toggled-header">Functionality</span>');
  plot = PrimeFaces.widgets.chart.plot;
//  console.log($('table.jqplot-table-legend'));
//  addToggleToLegend();
//  $('body').on('click', 'thead th.toggleSplit',function() {
//     toggleSplit();
//     //$('i', $(this)).toggleClass("go");
//  });
  
};


//var addToggleToLegend = function() {
//   $('table.jqplot-table-legend').prepend('<thead></thead>');
//   $('table.jqplot-table-legend thead').append('<tr><th colspan="2" class="toggleSplit"> <i class="fa fa-refresh rotate"/> Toggle Split</th></tr>');
//   $('table.jqplot-table-legend thead tr th i').toggleClass("go");
//}

function getWidgetVarById(id) {
   for (var propertyName in PrimeFaces.widgets) {
     if (PrimeFaces.widgets[propertyName].id === id) {
       return PrimeFaces.widgets[propertyName];
     }
   }
}

function centerResize() {
   PrimeFaces.widgets.chart.plot.replot( {resetAxes:true} );
   PrimeFaces.widgets.funcTable.render();
}

function changeGraphScale() {
   var renderer;
   var scale = $("#leftForm\\:scaleSelect div.ui-state-active > input").val();
   var options = PrimeFaces.widgets.chart.plot.options;
   var data = PrimeFaces.widgets.chart.plot.data;
   
   if (scale == "log") {
      renderer = $.jqplot.LogAxisRenderer;              
   } else {
      options.axes.yaxis.min = 0;
      renderer = $.jqplot.LinearAxisRenderer;        
   }
   options.axes.yaxis.renderer = renderer;
   plot.destroy();
   PrimeFaces.widgets.chart.plot = $.jqplot('chartForm\\:chart1', data, options);

   PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
   
}


function timelineSelect(timelineIndex) {
   var obj = PF('timelineWidget-'+timelineIndex).getSelectedEvent();
   //console.log( obj ); 
   var str = obj.className;
   var patt = new RegExp("true");
   var exists = patt.test(str);
   
   $('#timelineSelectMsgTerm').html('<b>Annotation Category:</b> '+ obj.group);
   $('#timelineSelectMsgDate').html('<b>Date:</b> '+ obj.content);
   $('#timelineSelectMsgExists').html('<b>Annotated:</b> ' + exists);
}

function cleanMsg() {
   $('#timelineSelectMsgTerm').html('');
   $('#timelineSelectMsgDate').html('');
   $('#timelineSelectMsgExists').html('');
}

function hideSeries(sidx, replot) {
   if ( utility.isUndefined(sidx) ) return;
   if ( utility.isUndefined(replot) ) replot=false;
   var my_plot = PrimeFaces.widgets.chart.plot;
   var s = my_plot.series[sidx ];
   if (!s.canvas._elem.is(':hidden') || s.show) {

      my_plot.legend._elem.find('td').eq(sidx * 2).addClass('jqplot-series-hidden');
      my_plot.legend._elem.find('td').eq((sidx * 2) + 1).addClass('jqplot-series-hidden');
      
      s.toggleDisplay({data:{}});
      s.canvas._elem.hide();
      s.show = false;
      
      if ( replot ) {
         PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
      } 
      
   
      //my_plot.legend._elem.find('td').eq(sidx * 2).click()
   }
      
}

function showSeries(sidx, replot) {
   if ( utility.isUndefined(sidx) ) return;
   if ( utility.isUndefined(replot) ) replot=false;
   var my_plot = PrimeFaces.widgets.chart.plot;
   var s = my_plot.series[sidx ];
   if (s.canvas._elem.is(':hidden') || !s.show) {

   
      my_plot.legend._elem.find('td').eq(sidx * 2).removeClass('jqplot-series-hidden');
      my_plot.legend._elem.find('td').eq((sidx * 2) + 1).removeClass('jqplot-series-hidden');
      
      s.toggleDisplay({data:{}});
      s.canvas._elem.show();
      s.show = true;
      
      if ( replot ) {
         PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
      }
   
   
      //my_plot.legend._elem.find('td').eq(sidx * 2).click()
   }
}

function isolateSeries(sidx) {
   if ( utility.isUndefined(sidx) ) return;
   var my_plot = PrimeFaces.widgets.chart.plot;
   for (var i = 0; i < my_plot.series.length; i++) {
      if (i == sidx) {
        showSeries(i, false);
      } else {
         hideSeries(i, false);
      }
      
   }
   PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
   
}

function isolateSeriesFromSwatch(el) {
   if ( utility.isUndefined(el) ) return;
   
   var my_plot = PrimeFaces.widgets.chart.plot;
   var sidx = $(el).closest('table').find('td').index($(el).closest('td'))/2;
   isolateSeries(sidx);
   
/*      for (var i = 0; i < my_plot.series.length; i++) {
      if (i == sidx) {
        showSeries(i, false);
      } else {
         hideSeries(i, false);
      }*/
      
   }

function showAllSeries() {
   var my_plot = PrimeFaces.widgets.chart.plot;
   for (var i = 0; i < my_plot.series.length; i++) {
      showSeries(i, false);
   }
   PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
}

function onTimelineRangeChange(timelineIndex) {
   var range = PF('timelineWidget-'+timelineIndex).getVisibleRange(); 
   var timelines = PrimeFaces.widgets.timelineDataGridWidget.content.find('tr').length
   for (var int = 0; int < timelines; int++) {
      if ( timelineIndex !== int ) {
         PF('timelineWidget-'+int).setVisibleRange(range.start, range.end); 
      }       
   }
   
}

$(document).ready(function() {
   //calling remoteCommands
   fetchCharts();
   
   $('body').on('click', function(e) {
      if ( $(e.target).hasClass('jqplot-table-legend-swatch') ) {
         isolateSeriesFromSwatch( e.target );
      }
   });
   
   

})