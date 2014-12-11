$(document).ready(function() {
$('#chart1').bind('jqplotMouseMove', function(event, xy, axesData, neighbor, plot) {
        var drawingCanvas = $(".jqplot-highlight-canvas")[0];
        var context = drawingCanvas.getContext('2d');
        context.clearRect(0, 0, drawingCanvas.width, drawingCanvas.height);     
        for (var i = 0; i < plot.series.length; i++){
            var value = hitTestSegments(plot.series[i].gridData, xy, plot.series[i].lineWidth);
            if (value > -1) {
                //draw the highlight
                context.beginPath();
                for(var j = 0; j < plot.series[i].gridData.length; j++){
                    var p = plot.series[i].gridData[j];
                    if(j == 0)
                       context.moveTo(p[0],p[1]);
                    else context.lineTo(p[0],p[1]);
                }
                //adding some sort of a glow effect
                context.lineWidth = plot.series[i].lineWidth + 3;
                //context.strokeStyle = "rgba(154,205,50, 0.25)";//#9ACD32
                context.strokeStyle = convertHex(plot.series[i].color, 25)
                context.stroke();
              //  context.lineWidth = plot.series[i].lineWidth + 6;
             //   context.strokeStyle = "rgba(154,205,50, 0.5)";
             //   context.stroke();
                context.lineWidth = plot.series[i].lineWidth + 1;
                //context.strokeStyle = "rgba(154,205,50, 0.75)";
                context.strokeStyle = convertHex(plot.series[i].color, 75)
                context.stroke();
                //just highlight the line using its actual width
               // context.lineWidth = plot.series[i].lineWidth;
               /// context.strokeStyle = "rgba(154,205,50, 1)";//"yellowgreen";//#9ACD32 
              //  context.stroke();
                break;
            }
        }
    });

    function hitTestSegments(points, point, thickness) {
        if (points.length < 2) return -1;
        for (var i = 0; i < points.length - 1; i++) {
            var p0 = points[i];
            var p1 = points[i + 1];
            var distance = dotLineLength(point.x, point.y, p0[0], p0[1], p1[0], p1[1], true);
            if (distance < thickness+1) return i;
        }
        return -1;
    }

    //+ Jonas Raoni Soares Silva
    //@ http://jsfromhell.com/math/dot-line-length [rev. #1]
    dotLineLength = function(x, y, x0, y0, x1, y1, o) {
        function lineLength(x, y, x0, y0) {
            return Math.sqrt((x -= x0) * x + (y -= y0) * y);
        }
        if (o && !(o = function(x, y, x0, y0, x1, y1) {
            if (!(x1 - x0)) return {
                x: x0,
                y: y
            };
            else if (!(y1 - y0)) return {
                x: x,
                y: y0
            };
            var left, tg = -1 / ((y1 - y0) / (x1 - x0));
            return {
                x: left = (x1 * (x * tg - y + y0) + x0 * (x * -tg + y - y1)) / (tg * (x1 - x0) + y0 - y1),
                y: tg * left - tg * x + y
            };
        }(x, y, x0, y0, x1, y1), o.x >= Math.min(x0, x1) && o.x <= Math.max(x0, x1) && o.y >= Math.min(y0, y1) && o.y <= Math.max(y0, y1))) {
            var l1 = lineLength(x, y, x0, y0),
                l2 = lineLength(x, y, x1, y1);
            return l1 > l2 ? l2 : l1;
        }
        else {
            var a = y0 - y1,
                b = x1 - x0,
                c = x0 * y1 - y0 * x1;
            return Math.abs(a * x + b * y + c) / Math.sqrt(a * a + b * b);
        }
    };
    function convertHex(hex,opacity){
       hex = hex.replace('#','');
       r = parseInt(hex.substring(0,2), 16);
       g = parseInt(hex.substring(2,4), 16);
       b = parseInt(hex.substring(4,6), 16);

       result = 'rgba('+r+','+g+','+b+','+opacity/100+')';
       return result;
   }
});