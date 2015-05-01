var MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff80", "#ffff00", "#ff5005"]

function onLoad() {
   $("#left-toggler").append('<span class="vertical toggled-header">Options</span>');
}

var hideLoadingSpinner = function() {
   PrimeFaces.widgets.stabilityChartWdg.jq.show()
   $('.loading-spinner').hide();   
 };

 
 var showLoadingSpinner = function() {
    PrimeFaces.widgets.stabilityChartWdg.jq.hide()
    $('.loading-spinner').show();   
  };
  
//  $(window).load(function(){  // should be  $(window).load to load widget
//     $('#slder').slider({
//            step: 0.1
//           });
// });
  
  function centerResize() {
     //updateCenterPanel();
	  try {
	   PrimeFaces.widgets.stabilityChartWdg.plot.replot( {resetAxes:true} );
	  } catch (e) {
		  
	  }
	}
  
  function enrichmentChartExtender() {
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
         formatString : 'Date: %s <br /> P-Value: %.2p',
         tooltipContentEditor : function(str, seriesIndex, pointIndex, plot) {
            return plot.series[seriesIndex].label + "<br />" + str;
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
  
  function stabilityChartExtender() {
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
                                        formatString: "%.2f",
                                        forceTickAt0: true,
                                        forceTickAt1: true
                                    };
     this.cfg.highlighter = {
        show : true,
        tooltipLocation : 'sw',
        useAxesFormatters : true,
        tooltipAxes : 'xy',
        yvalues : 1,
        formatString : 'Date: %s <br /> Jaccard: %.2f',
        tooltipContentEditor : function(str, seriesIndex, pointIndex, plot) {
           return plot.series[seriesIndex].label + "<br />" + str;
        },
        bringSeriesToFront : true

     }
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
	
	function tabChanged(index)
	{
	   alert("Tab Changed:"+index);
	}
	function tabShowed(index)
	{
	   if (index==3) {
	      HC.charts.enrichment.resize();
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
	   this.recreate = function() {
         try {
            this.chart.destroy();
         } catch (e) {
            console.log(e);
         } finally {
            this.chart = new Highcharts.Chart(this.options);
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
	
	function handleGraphSelected(xhr, status, args) {
	    console.log(args.hc_data);
	    
	    var options = {
	                   chart: {
	                      renderTo: 'hc_container',
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
	                      text: 'Enrichment Results'
	                   },

	                   xAxis: {
	                      type: 'datetime',
	                      title: {
	                          text: 'Date'
	                      },
	                      minRange: 60 * 24 * 3600000 // fourteen days
	                   },

	                   yAxis: {
	                      type: 'logarithmic',
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
	    HC.charts.enrichment.recreate();
	    
	    

	}
	
	  
	function enrichmentChartDlgResize() {
	   HC.charts.enrichment.resize();
	  }
  
  $(document).ready(function() {
	  escDialog();
	   $('body').on('click', function(e) {
	      if ( $(e.target).hasClass('jqplot-table-legend-swatch') ) {
	         isolateSeriesFromSwatch( e.target );
	      }
	   });
	   
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
	   
	   
	   

	})