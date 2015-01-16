/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var data = '';
var data2 = '';
function drawVisualization() {
  eval(data);
  if (data === 'null') {
    data = '';
    return;
  }
  var motionchart = new google.visualization.MotionChart(document
          .getElementById('visualization'));
  motionchart.draw(data, {
    'width': 700,
    'height': 400,
    "stateVersion": 3,
    "yAxisOption": "2",
    "xAxisOption": "_TIME",
    "iconType": "LINE",
    "showTrails": false,   
    "sizeOption": "_UNISIZE", 'showSidePanel':false, showChartButtons: false,
    "orderedByY": false,state: '{"iconType":"LINE", "colorOption":"_UNIQUE_COLOR"}'
  });
}
function callgoogle() {
  var tmp = document.getElementById('initial:tabView:hidden2');
  if (tmp !== null) {
    data = tmp.value;
    google.setOnLoadCallback(drawVisualization());
  }
}
var data2;
function drawVisualization2() {
  eval(data2);
  if (data === 'null') {
    return;
  }
  var motionchart = new google.visualization.MotionChart(document
          .getElementById('visual2'));
  motionchart.draw(data2, {
    'width': 700,
    'height': 400,
    'stateVersion': 3,
    'yAxisOption': 2,
    'xAxisOption': '_TIME',
    
    'showTrails': false,
    'sizeOption': '_UNISIZE','showSidePanel':false,
    'orderedByY': true, showChartButtons: false, state: '{"iconType":"LINE", "colorOption":"_UNIQUE_COLOR"}'
  });
}
function callgoogle2() {
  data2 = document.getElementById('initial:tabView:data3').value;
  google.setOnLoadCallback(drawVisualization2());
}
var data3;
var dataTable;
optionsevc = {
  "width": "100%",
  "height": "98%",
  "style": "box", // optional
  "zoomable": "true",
  timeline: {rowLabelStyle: {fontName: 'Helvetica', fontSize: 11},
    barLabelStyle: {fontName: 'Helvetica', fontSize: 10},
    groupByRowLabel: true},
  avoidOverlappingGridLines: true
};
function drawChart() {
  eval(data3);
  if (dataTable === 'null') {
    return;
  }
  var container = document.getElementById('evidenceCodes');
  var chart = new google.visualization.Timeline(container);
  chart.draw(dataTable, optionsevc);
}

function callgoogle3() {
  data3 = document.getElementById('initial:tabView:data4').value;
  google.setOnLoadCallback(drawChart());
}

function clearchart1() {
  document.getElementById('visualization').innerHTML = '';
  var g1 = document.getElementById('initial:tabView:grid1');
  if (g1 != null)
    g1.innerHTML = '';
  var g2 = document.getElementById('initial:tabView:grid2');
  if (g2 != null)
    g2.innerHTML = '';
  var g3 = document.getElementById('initial:tabView:grid3');
  if (g3 != null)
    g3.innerHTML = '';
  var evc = document.getElementById('evidenceCodes');
  if (evc != null)
    evc.innerHTML = '';
  var ch2 = document.getElementById('visual2');
  if (ch2 != null)
    ch2.innerHTML = '';
  var pmed = document.getElementById('initial:tabView:pubmedtab');
  if (pmed != null)
    pmed.innedHTML = '';
}
