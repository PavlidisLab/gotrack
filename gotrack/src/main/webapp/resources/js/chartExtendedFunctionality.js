function hideSeries(sidx, replot) {
   if ( utility.isUndefined(sidx) ) return false;
   if ( utility.isUndefined(replot) ) replot=false;
   var my_plot = PrimeFaces.widgets.chart.plot;
   var s = my_plot.series[sidx ];
   //if (!s.canvas._elem.is(':hidden') || s.show) {
   if (s.show) {

      my_plot.legend._elem.find('td').eq(sidx * 2).addClass('jqplot-series-hidden');
      my_plot.legend._elem.find('td').eq((sidx * 2) + 1).addClass('jqplot-series-hidden');
      
      s.toggleDisplay({data:{}});
      s.canvas._elem.hide();
      s.show = false;
      
      if ( replot ) {
         PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
      } 
      return true;
      //my_plot.legend._elem.find('td').eq(sidx * 2).click()
   }
   return false;
}

function showSeries(sidx, replot) {
   if ( utility.isUndefined(sidx) ) return false;
   if ( utility.isUndefined(replot) ) replot=false;
   var my_plot = PrimeFaces.widgets.chart.plot;
   var s = my_plot.series[sidx ];
   //if (s.canvas._elem.is(':hidden') || !s.show) {
   if (!s.show) {

   
      my_plot.legend._elem.find('td').eq(sidx * 2).removeClass('jqplot-series-hidden');
      my_plot.legend._elem.find('td').eq((sidx * 2) + 1).removeClass('jqplot-series-hidden');
      
      s.toggleDisplay({data:{}});
      s.canvas._elem.show();
      s.show = true;
      
      if ( replot ) {
         PrimeFaces.widgets.chart.plot.replot({resetAxes:true});
      }
      return true;
      //my_plot.legend._elem.find('td').eq(sidx * 2).click()
   }
   return false;
}

function isolateSeries(sidx) {
   if ( utility.isUndefined(sidx) ) return;
   var my_plot = PrimeFaces.widgets.chart.plot;
   
   var changes = false;
   
   for (var i = 0; i < my_plot.series.length; i++) {
      if (i == sidx) {
         changes |= !showSeries(i, false);
      } else {
         changes |= hideSeries(i, false);
      }
      
   }

   if (!changes) {
      showAllSeries(); 
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