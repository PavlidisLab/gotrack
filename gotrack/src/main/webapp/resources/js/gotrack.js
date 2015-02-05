var plot;

$(document).ready(function() {
   //calling remoteCommands
   fetchCharts();

})

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

var changeGraphScale =  function() {
   var renderer;
   var scale = $("#leftForm\\:scaleSelect div.ui-state-active > input").val();
   var options = plot.options;
   var data = plot.data;
   
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