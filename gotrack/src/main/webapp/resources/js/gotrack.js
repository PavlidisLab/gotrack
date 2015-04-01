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
   var tags = obj.content.split("(|)");
   $('#timelineSelectMsgTerm').html('<b>Evidence:</b> '+ tags[1]);
   $('#timelineSelectMsgDate').html('<b>Date:</b> '+ obj.start.toLocaleDateString());
   $('#timelineSelectMsgReference').html('<b>Reference:</b> ' + tags[0]);
}

function cleanMsg() {
   $('#timelineSelectMsgTerm').html('');
   $('#timelineSelectMsgDate').html('');
   $('#timelineSelectMsgReference').html('');
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

function test() {
   console.log('test');
}

function showTerminal() {
   PF('terminalDialogWdg').show();
   PF('terminalWdg').focus();
}

function chartExtender() {
    // this = chart widget instance        
    // this.cfg = options 
    this.cfg.legend = {
       renderer : $.jqplot.EnhancedLegendRenderer,
       show : true,
       location : 's',
       placement : 'outside',
       marginTop : '100px',
       rendererOptions : {
          numberRows : 0,
          numberColumns : 10,
          seriesToggle: true,
          seriesToggleReplot : {resetAxes: true}
       }
    }
    this.cfg.highlighter = {
       show : true,
       tooltipLocation : 'sw',
       useAxesFormatters : true,
       tooltipAxes : 'xy',
       yvalues : 1,
       formatString : 'Date: %s ~ Count: %s',
       tooltipContentEditor : function(str, seriesIndex, pointIndex, plot) {
          return plot.series[seriesIndex].label + ": " + str;
       },
       bringSeriesToFront : true

    }
    //console.log(this.cfg.axes);
    //this.cfg.axes.yaxis.label = "Per Capita Expenditure (local currency)";
    this.cfg.axes.yaxis.renderer = $.jqplot.LinearAxisRenderer;
    //this.cfg.axes.yaxis.ticks = [1,10, 100, 1000];
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