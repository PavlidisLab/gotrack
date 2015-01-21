$(document).ready(function() {
   //calling remoteCommands
   fetchCharts();

})

var hideLoadingSpinner = function() {
  $('#loading-spinner').hide();
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