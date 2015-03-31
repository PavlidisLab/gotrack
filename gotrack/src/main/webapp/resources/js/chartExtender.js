      function chartExtender() {
         // this = chart widget instance        
         // this.cfg = options 
         this.cfg.legend = {
//            renderer : $.jqplot.EnhancedLegendRenderer,
            show : false,
//            location : 's',
//            placement : 'outsideGrid',
//            rendererOptions : {
//               numberRows : 0,
//               numberColumns : 10,
//               seriesToggle: true,
//               seriesToggleReplot : {resetAxes: true}
//            }
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