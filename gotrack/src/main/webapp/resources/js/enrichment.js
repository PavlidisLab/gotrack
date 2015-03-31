function onLoad() {
   $("#left-toggler").append('<span class="vertical toggled-header">Options</span>');
   $("#right-toggler").append('<span class="vertical toggled-header">Selected Genes</span>');
}

var hideLoadingSpinner = function() {
   $('#formEnrich\\:enrichmentChart').show();
   $('#loading-spinner').hide();   
 };

 
 var showLoadingSpinner = function() {
    $('#formEnrich\\:enrichmentChart').hide();
    $('#loading-spinner').show();   
  };
  
//  $(window).load(function(){  // should be  $(window).load to load widget
//     $('#slder').slider({
//            step: 0.1
//           });
// });