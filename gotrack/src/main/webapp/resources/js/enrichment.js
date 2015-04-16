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
  
  function centerResize() {
	  try {
	   PrimeFaces.widgets.chart.plot.replot( {resetAxes:true} );
	  } catch (e) {
		  
	  }
	   PrimeFaces.widgets.tableGenesWdg.render();
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
      this.cfg.axes.yaxis.tickOptions = {
                                         formatString: "%.2p"
                                     };
      
      this.cfg.highlighter = {
         show : true,
         tooltipLocation : 'sw',
         useAxesFormatters : true,
         tooltipAxes : 'xy',
         yvalues : 1,
         formatString : 'Date: %s ~ P-Value: %.2p',
         tooltipContentEditor : function(str, seriesIndex, pointIndex, plot) {
            return plot.series[seriesIndex].label + ": " + str;
         },
         bringSeriesToFront : true

      }
      this.cfg.canvasOverlay = {
              show:true,
              objects: [{
            	  dashedHorizontalLine: { color: 'rgb(89, 198, 154)', y: 0.05, lineWidth: 2,shadow:false}
               }]
      }
      //console.log(this.cfg.axes);
      //this.cfg.axes.yaxis.label = "Per Capita Expenditure (local currency)";
      this.cfg.axes.yaxis.renderer = $.jqplot.LogAxisRenderer;
      //this.cfg.axes.yaxis.ticks = [1,10, 100, 1000];
   }
  
  function drawThreshold(t) {
     try {
	  PrimeFaces.widgets.chart.plot.plugins.canvasOverlay.objects[0].options.y=t;
	  PrimeFaces.widgets.chart.plot.plugins.canvasOverlay.draw(PrimeFaces.widgets.chart.plot);
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
  
  $(document).ready(function() {
	  escDialog();
	   $('body').on('click', function(e) {
	      if ( $(e.target).hasClass('jqplot-table-legend-swatch') ) {
	         isolateSeriesFromSwatch( e.target );
	      }
	   });
	   
	   

	})