function onLoad() {
    $("#left-toggler").append('<span class="vertical toggled-header">Options</span>');
}

var hideLoadingSpinner = function () {
    $('.loading-spinner').hide();
};


var showLoadingSpinner = function () {
    $('.loading-spinner').show();
};

//$(window).load(function(){  // should be  $(window).load to load widget
//$('#slder').slider({
//step: 0.1
//});
//});

function handleFetchGraphDialog(xhr, status, args) {
    gograph.createNewGraph('#dagDialog', JSON.parse(args.graph_data));
}

function runEnrichmentOnClick() {
    $('#progressBarContainer').removeClass('disabled');
    try {
        PF('runEnrichmentBtnWdg').disable();
        PF('enrichmentProgressBarWdg').start();
        window.progressBarId = PF('enrichmentProgressBarWdg').progressPoll;
    } catch (e) {
        console.log(e);
    }
}

function loadPreviousEnrichmentComplete(xhr, status, args) {
    if (args.success) {
        console.log('Processing Previous Enrichment Analysis.');
        runEnrichmentComplete(xhr, status, args);
    } else {
        console.log('Load Previous Encrichment Analysis Failed.');
    }

}

function runEnrichmentComplete(xhr, status, args) {
    // Slight hack here to make sure that the polling functionality of the progress bar stops
    // Ran into a problem with viewexpiredexception not stopping the polling while also
    // clearing the PrimeFaces widgets...
    try {
        PF('enrichmentProgressBarWdg').stop()
    } catch (e) {
        window.clearInterval(window.progressBarId);
        window.clearTimeout(window.progressBarId);
    }
    try {
        PF('runEnrichmentBtnWdg').enable();
    } catch (e) {

    }

    try {
        PF('tableEnrichmentWdg').filter();
    } catch (e) {

    }

    try {
        reInitializeCharts();

        if (!utility.isUndefined(args.dateToEdition)) {
            GLOBALS.dateToEdition = JSON.parse(args.dateToEdition);
        }

        try {
            args.HC_terms = JSON.parse(args.HC_terms);
        } catch (e) {
            console.log(e);
            return;
        }

        createTermsChart(xhr, status, args);

        try {
            args.HC_similarity = JSON.parse(args.HC_similarity);
        } catch (e) {
            console.log(e);
            return;
        }

        createSimilarityChart(xhr, status, args);

    } catch (e) {
        console.log(e);
    }
}

function centerResize() {
    onresize();
}

function escDialog() {
    $(document).keyup(function (e) {
        if (e.keyCode == 27) { // esc code is 27
            closeAllDialog();
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

function postAjaxSortTable(datatable) {
    var selectedColumn = undefined;

    // multisort support
    if (datatable && datatable.cfg.multiSort) {
        if (datatable.sortMeta.length > 0) {
            var lastSort = datatable.sortMeta[datatable.sortMeta.length - 1];
            selectedColumn = $(document.getElementById(lastSort.col));
        }
    } else {
        selectedColumn = datatable.jq.find('.ui-state-active');
    }

    // no sorting selected -> quit
    if (!selectedColumn || selectedColumn.length <= 0) {
        return;
    }

    var sortorder = selectedColumn.data('sortorder') || "DESCENDING";
    datatable.sort(selectedColumn, sortorder, datatable.cfg.multiSort);
}


function enrichmentChartHide() {
    plotting.charts.enrichment.destroy();
    plotting.charts.enrichmentMaster.destroy();
}

function createTermsChart(xhr, status, args) {
    // console.log(xhr, status, args);

    var options = plotting.defaultHCOptions('hc_terms_container', args.HC_terms.chart);
    commonOptions(options, args.HC_terms);

    options.subtitle = {
        text: "<b>&lt;Click&gt;</b> to view enrichment results at a specific date.",
        style: {"font-size": "10px"}
    };
    options.exporting.chartOptions.subtitle.text = ""; // Remove subtitle
    options.exporting.chartOptions.legend.margin = 12;
    options.exporting.chartOptions.legend.y = 0;

    plotting.charts.terms.options = options;
    plotting.charts.terms.recreate(options, function (c) {
        redrawSelectedEditionPlotLine(c, {x:args.HC_terms.selectedEdition});
        plotting.mainCharts.push(c);
    });
}

function createSimilarityChart(xhr, status, args) {
    // console.log(xhr, status, args);

    var options = plotting.defaultHCOptions('hc_similarity_container', args.HC_similarity.chart);
    commonOptions(options, args.HC_similarity);

    options.subtitle = {
        text: "<b>&lt;Click&gt;</b> to view similarity details at a specific date.",
        style: {"font-size": "10px"}
    };
    options.exporting.chartOptions.subtitle.text = ""; // Remove subtitle
    options.exporting.chartOptions.legend.margin = 12;
    options.exporting.chartOptions.legend.y = 0;

    // options.yAxis.minorTickInterval = 0.05;

    options.tooltip.pointFormatter = function () {
        return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + utility.sigFigs(this.y, 2) + '</b><br/>';
    };

    plotting.charts.similarity.options = options;
    plotting.charts.similarity.recreate(options, function (c) {
        redrawSelectedEditionPlotLine(c, {x:args.HC_similarity.selectedEdition});
        plotting.mainCharts.push(c);
    });

}

function destroySelectedEdition(c) {
    c.xAxis[0].removePlotLine('plot-line-selected');
}

function redrawSelectedEditionPlotLine(c, p) {
    var chart = c;

    destroySelectedEdition(chart);

    chart.xAxis[0].addPlotLine({
        value: p.x,
        color: '#FF0000',
        width: 1,
        id: 'plot-line-selected',
        zIndex: 3,
        label: {
            text: 'Selected',
            verticalAlign: 'top',
            textAlign: 'center',
            y: 30,
            style: {
                color: '#5a0000',
                fontSize: '12px'
            }
        }
    });
}

function commonOptions(options, config) {
    plotting.addLegend(options);
    options.legend = {
        margin: 0,
        verticalAlign: 'bottom',
        y: 17
    };

    options.xAxis.plotLines = [{
        value: config.referenceEdition,
        color: '#a9a9a9',
        width: 1,
        dashStyle: 'Dot',
        id: 'plot-line-reference',
        className: 'export',
        zIndex: 3,
        label: {
            text: 'Reference',
            verticalAlign: 'bottom',
            textAlign: 'center',
            y: -30,
            style: {
                fontSize: '12px'
            }
        }
    }];
    // options.legend.layout = 'horizontal';
    // options.legend.align = 'center';
    // options.legend.verticalAlign = 'bottom';
    // options.legend.margin = 0;


    var clickBehaviour = function (event, p) {
        fetchPointData([{name: 'edition', value: GLOBALS.dateToEdition[p.x]}]);
        plotting.mainCharts.forEach(function (c) {
            redrawSelectedEditionPlotLine(c, p);
        });
    };

    options.plotOptions.series.point = {
        events: {
            click: function (event) {
                var p = this;
                clickBehaviour(event, p);
            }
        }
    };

    options.chart.events = {
        click: function (event) {
            var p = this.hoverPoint;
            clickBehaviour(event, p);
        }
    };

    options.xAxis.crosshair = {
        width: 1,
        color: 'red',
        dashStyle: 'shortdot'
    };
}

function handleGraphHistogram(xhr, status, args) {
    try {
        args.HC_histo = JSON.parse(args.HC_histo);
        args.edition = JSON.parse(args.edition);
    } catch (e) {
        console.log(e);
        return;
    }

    console.log(args);
    args.HC_histo.sort(function(a, b) {
        return a - b;
    });

    var chart = {
        title:"P-Value Distribution",
        subtitle: 'Edition ' + args.edition.edition + ' : ' + args.edition.date,
        series:[],
    };

    var options = plotting.defaultHCOptions('hc_chart_dlg_container', chart);
    options.exporting.chartOptions.xAxis.labels.rotation = -45;
    options.exporting.chartOptions.legend.enabled = false;

    // Multiple axis does not merge additional export axis options. This is a workaround.
    // See https://github.com/highcharts/highcharts/issues/2022
    options.exporting.chartOptions.chart.events.load = function() {
        // for (var i = 0; i < this.xAxis.length; i++) {
        //     this.xAxis[i].update(options.exporting.chartOptions.xAxis);
        // }
        // for (i = 0; i < this.yAxis.length; i++) {
        //     this.yAxis[i].update(options.exporting.chartOptions.yAxis);
        // }
        this.xAxis[1].update({visible: false});
        this.yAxis[1].update({visible: false});
        this.series[1].update({visible: false});

    };

    options.xAxis = [{
        title: { text: 'P-Value' },
        alignTicks: false
    },{
        title: { text: 'Rank (Scatter)' },
        alignTicks: false,
        opposite: true
    }];

    options.yAxis = [{
        title: { text: 'Number of Terms' }
    },{
        title: { text: 'P-Value (Scatter)' },
        opposite: true
        // type: 'logarithmic'
    }];

    options.series = [{
        name: 'Histogram',
        type: 'histogram',
        baseSeries: 's1',
        zIndex: -1,
        tooltip: {
            pointFormatter: function () {
                return '<span style="font-size:10px">' + utility.sigFigs(this.x, 2) + ' - ' + utility.sigFigs(this.x2, 2) + '</span><br/><span style="color:' + this.color + '">\u25CF</span> Number of Terms: <b>' + this.y + '</b><br/>';
            }
        }
    }, {
        name: 'Data',
        type: 'scatter',
        xAxis: 1,
        yAxis: 1,
        data: args.HC_histo,
        id: 's1',
        marker: {
            radius: 1.5
        },
        tooltip: {
            pointFormatter: function () {
                return '<br/>Rank: <b>' + this.x + '</b>'
                    + '<br/>P-Value: <b>' + utility.sigFigs(this.y, 2) + '</b><br/>';
            }
        }
    }];
    delete options.colors;

    plotting.addLegend(options);
    options.legend = {
        align: 'center',
        verticalAlign: 'bottom',
        layout: 'horizontal'
    };

    Highcharts.chart('hc_chart_dlg_container', options);
}

function handleGraphSelected(xhr, status, args) {
    try {
        args.HC_enrichment = JSON.parse(args.HC_enrichment);
    } catch (e) {
        console.log(e);
        return;
    }

    console.log(args);

    var options = plotting.defaultHCOptions('hc_enrichment_container', args.HC_enrichment.chart);
    plotting.addLegend(options);
    plotting.addLegendTooltips(options);
    options.chart.zoomType = 'xy';

    options.subtitle = {
        text: 'Select an area by dragging across the lower chart'
    };
    options.exporting.chartOptions.subtitle.text = ""; // Remove subtitle
    options.plotOptions.series.states = {
        hover: {
            lineWidth: 2
        }
    };
    options.plotOptions.series.events.mouseOver = function () {
        var item = this.legendItem;
        Highcharts.each(this.chart.series, function (series, i) {
            if (series.legendItem !== item && series.legendItem != null && series.visible) {
                series.legendItem.css({
                    color: 'grey'
                });
//              series.legendLine.attr({
//                  stroke: 'grey' 
//              });
//              series.legendSymbol.attr({
//                  fill: 'grey' 
//              });
            }
        });

    };
    options.plotOptions.series.events.mouseOut = function () {
        Highcharts.each(this.chart.series, function (series, i) {
            if (series.legendItem != null && series.visible) {
                series.legendItem.css({
                    color: 'black'
                });
//             series.legendLine.attr({
//                 stroke: series.color 
//             });
//             series.legendSymbol.attr({
//                 fill: series.color
//             });
            }
        });
    };

    if (args.HC_enrichment.type == "pvalue") {
        options.plotOptions.series.point = {
            events: {
                click: function () {
                    fetchTermInformation([{name: 'termId', value: this.series.name}, {
                        name: 'edition',
                        value: GLOBALS.dateToEdition[this.x]
                    }]);
                },
            }
        };
        options.yAxis = {
            type: 'logarithmic',
            reversed: true,
            title: {
                text: args.HC_enrichment.yLabel
            },
            max: 1,
            minorTickInterval: 0.1,
            labels: {
                formatter: function () {
                    return this.value;
                }
            },
//                       plotLines : [{
//                          value : args.hc_threshold,
//                          color : 'black',
//                          dashStyle : 'shortdash',
//                          width : 2,
//                          label : {
//                             text : 'Threshold'
//                          },
//                          zIndex: 5
//                       }]
        };
        options.plotOptions.series.lineWidth = 1;
        options.tooltip = {
            headerFormat: '<b>{series.name}</b><br />',
            pointFormat: 'x = {point.x}, y = {point.y}',
            useHTML: true,
            formatter: function () {
                return '<span style="color:' + this.series.color + '">\u25CF</span><b>' + this.series.name +
                    '</b><br/><p>Name: ' + this.series.options.title +
                    '</p><p>Date: ' + new Date(this.x).toLocaleDateString() +
                    "</p><p>Edition: " + GLOBALS.dateToEdition[this.x] +
                    "</p><p>P-value: " + utility.sigFigs(this.y, 3) +
                    "</p>";
            }
        };

        // add threshold line
        var cutoffsData = []

        for (var key in args.HC_enrichment.cutoffs) {
            cutoffsData.push([parseInt(key, 10), args.HC_enrichment.cutoffs[key]]);
        }

        options.series.unshift({
            name: "Threshold",
            data: cutoffsData,
            type: 'line',
            lineWidth: 2,
            zIndex: 5,
            enableMouseTracking: false,
            dashStyle: 'shortdash',
            color: 'black',
        });


    } else {
        //rank
        var maxRank = args.HC_enrichment.maxRank;

        var opacity = Math.min(10 / args.HC_enrichment.chart.series.length + 1 / 100, 0.1);
        //var opacity = Math.min(10/maxRank+1/100,0.1);

        var dateToMaxSigRank = args.HC_enrichment.dateToMaxSigRank;
        var topN = args.HC_enrichment.topN;
        var outsideTopNCheck = args.HC_enrichment.outsideTopNCheck;
        var insignificantCheck = args.HC_enrichment.insignificantCheck;

        options.plotOptions.series.point = {
            events: {
                click: function () {
                    fetchTermInformation([{name: 'termId', value: this.series.name}, {
                        name: 'edition',
                        value: GLOBALS.dateToEdition[this.x]
                    }, {name: 'value', value: utility.roundHalf(this.y)}, {
                        name: 'valueLabel',
                        value: "Relative Rank"
                    }]);
                }
            }
        };
        options.yAxis = {
            type: 'linear',
            reversed: true,
            lineColor: 'black',
            lineWidth: 2,
            gridLineWidth: 0,
            offset: 10,
            tickInterval: 1,
            min: 0,
            allowDecimals: false,
            title: {
                text: args.HC_enrichment.yLabel
            },
            labels: {
                formatter: function () {
                    return this.value;
                }
            }
        };
        options.plotOptions.series.shadow = {opacity: opacity};
        options.plotOptions.series.lineWidth = 0.01;

        options.tooltip = {
            headerFormat: '<b>{series.name}</b><br />',
            pointFormat: 'x = {point.x}, y = {point.y}',
            useHTML: true,
            formatter: function () {
                return '<span style="color:' + this.series.color + '">\u25CF</span><b>' + this.series.name +
                    '</b><br/><p>Name: ' + this.series.options.title +
                    '</p><p>Date: ' + new Date(this.x).toLocaleDateString() +
                    "</p><p>Edition: " + GLOBALS.dateToEdition[this.x] +
                    "</p><p>Relative Rank: " + ( this.y >= dateToMaxSigRank[this.x] ? "Insignificant" : utility.roundHalf(this.y) ) +
                    "</p>";
            }
        };

        if (outsideTopNCheck || insignificantCheck) {

            var tempPoints = [];

            for (var key in dateToMaxSigRank) {
                var r = dateToMaxSigRank[key];
                tempPoints.push([parseInt(key), r - 0.5]);
            }

            tempPoints.sort(function (a, b) {
                return a[0] - b[0]
            });

            polygonPoints = [];

            for (var i = 0; i < tempPoints.length; i++) {
                var p1 = tempPoints[i];
                var r1 = p1[1];
                polygonPoints.push([p1[0], r1]);
                if (i < tempPoints.length - 1) {

                    var p2 = tempPoints[i + 1];
                    var midPoint = ( p1[0] + p2[0] ) / 2


                    var r2 = p2[1];


                    polygonPoints.push([midPoint, r1]);
                    polygonPoints.push([midPoint, r2]);

                }
            }
            if (insignificantCheck) {
                var s = {
                    name: "Insignificant Region",
                    type: 'polygon',
                    data: polygonPoints.slice(),
                    color: Highcharts.Color(plotting.MAXIMALLY_DISTINCT_COLORS[2]).setOpacity(0.2).get(),
                    enableMouseTracking: false,
                    includeInCSVExport: false
                };
//            for (var i = polygonPoints.length-1; i >= 0; i--) {
//               var p = polygonPoints[i];
//               s.data.push([ p[0], maxRank +0.5 ])
//            }
                var p = polygonPoints[polygonPoints.length - 1];
                s.data.push([p[0], maxRank + 0.5])
                var p = polygonPoints[0];
                s.data.push([p[0], maxRank + 0.5])

                options.series.unshift(s);
                //console.log(s);
            }
            if (outsideTopNCheck) {

                // This loop is to flatten regions that have been erroneously attributed to be significant because
                // the region of insignificance extended lower (less magnitude) than topN
                for (var i = 0; i < polygonPoints.length; i++) {
                    var barrierPoint = polygonPoints[i];
                    if (barrierPoint[1] < topN - 0.5) {
                        polygonPoints[i][1] = topN - 0.5;
                    }

                }

                s = {
                    name: "Outside Top " + topN,
                    type: 'polygon',
                    data: polygonPoints.slice(),
                    color: Highcharts.Color(plotting.MAXIMALLY_DISTINCT_COLORS[0]).setOpacity(0.2).get(),
                    enableMouseTracking: false,
                    includeInCSVExport: false
                };
//            for (var i = polygonPoints.length-1; i >= 0; i--) {
//               var p = polygonPoints[i];
//               s.data.push([ p[0], topN - 0.5 ])
//            }
                var p = polygonPoints[polygonPoints.length - 1];
                s.data.push([p[0], topN - 0.5])
                var p = polygonPoints[0];
                s.data.push([p[0], topN - 0.5])
                options.series.unshift(s);
                //console.log(s);
            }
        }

//    for (var i = polygonPoints.length-1; i >=0; i--) {
//    var p = polygonPoints[i];
//    s.data.push([ p[0], insigRank +5 ]);
//    }


    }

    for (var i = 0; i < options.series.length; i++) {
        var series = options.series[i];
        series.marker = {enabled: false};
    }

    if (!utility.isUndefined(args.HC_enrichment.errors) && args.HC_enrichment.type == "pvalue") {
        // Essentially if stability graph
        console.log("Errors", args.HC_enrichment.errors);
        var series = args.HC_enrichment.errors.series[0];
        var name = series.name;
        var data = []

        for (var j = 0; j < series.data.length; j++) {
            var point = series.data[j];
            data.push([point.x, point.y.left, point.y.right]);
        }

        options.series.push({
            name: name,
            data: data,
            type: 'arearange',
            lineWidth: 0,
            linkedTo: ':previous',
            fillOpacity: 0.3,
            zIndex: -1,
            enableMouseTracking: false
//         point : {
//            events: {
//               click: function () { //do nothing
//               },
//            }
//         },
//         states : {
//            hover: {
//               lineWidth: 1
//            }
//         }

        });

        var dateToStabilityScore = args.HC_enrichment.dateToStabilityScore;

        // This is necessary as special double types are not allowed in the JSON spec
        // We bypass this via a string type adaptor for Infinity and NaN
        for (var key in dateToStabilityScore) {
            if (dateToStabilityScore.hasOwnProperty(key)) {
                var stabilityScore = dateToStabilityScore[key];
                if (stabilityScore === 'Infinity') {
                    stabilityScore = 'No Change';
                } else if (stabilityScore === 'NaN') {
                    stabilityScore = 'No Data';
                } else {
                    stabilityScore = utility.sigFigs(stabilityScore, 3)
                }
                dateToStabilityScore[key] = stabilityScore;
            }
        }

        options.tooltip = {
            headerFormat: '<b>{series.name}</b><br />',
            pointFormat: 'x = {point.x}, y = {point.y}',
            useHTML: true,
            formatter: function () {
                return '<span style="color:' + this.series.color + '">\u25CF</span><b>' + this.series.name +
                    '</b><br/><p>Name: ' + this.series.options.title +
                    '</p><p>Date: ' + new Date(this.x).toLocaleDateString() +
                    "</p><p>Edition: " + GLOBALS.dateToEdition[this.x] +
                    "</p><p>P-value: " + utility.sigFigs(this.y, 3) +
                    "</p><p>Stability Score: " + dateToStabilityScore[this.x] +
                    "</p>";
            }
        }

//      options.plotOptions.series.point = {
//                                          events: {
//                                             click: function () {
//                                                fetchTermInformation([{name:'termId', value:this.series.name},{name:'edition', value:dateToEdition[this.x]}, {name:'value', value:dateToStabilityScore[this.x]}, {name:'valueLabel', value:"Stability Score"} ]);
//                                             }
//                                          }
//                                       };


    }


    // create the detail chart
    plotting.charts.enrichment.options = options;
    plotting.charts.enrichment.recreate(options);

    // create the master chart
    var optionsCopy = $.extend(true, {}, options);
    optionsCopy.chart.renderTo = 'hc_enrichmentMaster_container';
    optionsCopy.chart.reflow = false;
    optionsCopy.chart.borderWidth = 0;
    optionsCopy.chart.backgroundColor = null;
    optionsCopy.chart.marginLeft = 50;
    optionsCopy.chart.marginRight = 20;
    optionsCopy.chart.zoomType = 'x';
    optionsCopy.chart.events = {

        // listen to the selection event on the master chart to update the
        // extremes of the detail chart
        selection: function (event) {
            var extremesObject = event.xAxis[0],
                min = extremesObject.min,
                max = extremesObject.max,
                xAxis = this.xAxis[0];

            // Smooth hacks
            plotting.charts.enrichment.chart.xAxis[0].setExtremes(min, max);
            plotting.charts.enrichment.chart.showResetZoom();
            var oldOnClick = plotting.charts.enrichment.chart.resetZoomButton.element.onclick;
            plotting.charts.enrichment.chart.resetZoomButton.element.onclick = function (event) {
                oldOnClick();
                xAxis.removePlotBand('mask-selection');
            }


            xAxis.removePlotBand('mask-selection');
            xAxis.addPlotBand({
                id: 'mask-selection',
                from: min,
                to: max,
                color: 'rgba(0, 0, 150, 0.2)'
            });


            return false;
        }
    };
    optionsCopy.title.text = null;
    optionsCopy.subtitle.text = null;
    optionsCopy.xAxis.showLastTickLabel = true;
    optionsCopy.xAxis.title = {
        text: null
    };
    optionsCopy.yAxis.gridLineWidth = 0;
    optionsCopy.yAxis.labels = {enabled: false};
    optionsCopy.yAxis.title = {text: null};
    optionsCopy.yAxis.showFirstLabel = false;
    optionsCopy.yAxis.lineWidth = 0;
    optionsCopy.yAxis.plotLines = [];
    optionsCopy.tooltip = {
        formatter: function () {
            return false;
        }
    };
    optionsCopy.legend = {enabled: false};
    optionsCopy.credits = {enabled: false};
    optionsCopy.plotOptions.series.enableMouseTracking = false;
    optionsCopy.plotOptions.series.marker = {enabled: false};
    optionsCopy.plotOptions.series.states = {
        hover: {
            lineWidth: 0
        }

    };
    optionsCopy.exporting = {enabled: false};

    if (outsideTopNCheck) {
        optionsCopy.series.shift();
    }

    if (insignificantCheck) {
        optionsCopy.series.shift();
    }

    plotting.charts.enrichmentMaster.options = optionsCopy;
    plotting.charts.enrichmentMaster.recreate(optionsCopy);

}


function enrichmentChartDlgResize() {
    plotting.charts.enrichment.resize();
    plotting.charts.enrichmentMaster.resize();
}

function reInitializeCharts() {
    try {
        GLOBALS = {};
        plotting.mainCharts = [];
        plotting.removeAllCharts();
        plotting.createNewChart('terms');
        plotting.createNewChart('similarity');
        plotting.createNewChart('enrichment');
        plotting.createNewChart('enrichmentMaster');
    } catch (e) {
        console.log('Error initializing charts');
    }
}

/**
 * On top of each column, draw a zigzag line where the axis break is.
 */
function pointBreakColumn(e) {
    var point = e.point,
        brk = e.brk,
        shapeArgs = point.shapeArgs,
        x = shapeArgs.x,
        y = this.toPixels(brk.from, true),
        w = shapeArgs.width,
        key = ['brk', brk.from, brk.to],
        path = ['M', x, y, 'L', x + w * 0.25, y + 4, 'L', x + w * 0.75, y - 4, 'L', x + w, y];

    if (!point[key]) {
        point[key] = this.chart.renderer.path(path)
            .attr({
                'stroke-width': 3,
                stroke: point.series.options.borderColor
            })
            .add(point.graphic.parentGroup);
    } else {
        point[key].attr({
            d: path
        });
    }
}

$(document).ready(function () {
    //escDialog();

    // This self-executing anon func creates a resize event on the enrichment chart dialog
    // that will only run once the resize event has stopped
    ;
    (function () {
        var id;
        $("#enrichmentChartDlg").resize(function () {
            clearTimeout(id);
            id = setTimeout(enrichmentChartDlgResize, 200);
        });
    })();

    // Resize plots on window resize
    window.onresize = function (event) {
        plotting.charts.terms.resize();
        plotting.charts.similarity.resize();
    };

    /**
     * Extend the Axis.getLinePath method in order to visualize breaks with two parallel
     * slanted lines. For each break, the slanted lines are inserted into the line path.
     */
    Highcharts.wrap(Highcharts.Axis.prototype, 'getLinePath', function (proceed, lineWidth) {
        var axis = this,
            path = proceed.call(this, lineWidth),
            x = path[1];

        Highcharts.each(this.breakArray || [], function (brk) {
            var from;
            if (!axis.horiz) {
                y = axis.toPixels(brk.from);
                path.splice(3, 0,
                    'L', x, y - 4, // stop
                    'M', x + 5, y - 9, 'L', x - 5, y + 1, // lower slanted line
                    'M', x + 5, y - 1, 'L', x - 5, y + 9, // higher slanted line
                    'M', x, y + 4
                );
            }
        });
        return path;
    });


})