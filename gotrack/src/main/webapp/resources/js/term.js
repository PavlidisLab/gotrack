function onLoad() {
}

function centerResize() {
    try {
        var index = PrimeFaces.widgets.centerTabWdg.getActiveIndex();
        tabShowed(index);
    } catch (e) {
        console.log(e);
    }
}

function tabShowed(index) {
    try {
        if (index == 0) {
            CS.overview.graph.resize();
        } else if (index == 2) {
            plotting.charts.overview.resize();
        } else if (index == 3) {
            plotting.charts.gene.resize();
        } else if (index == 4) {
            plotting.charts.evidence.resize();
        }
    } catch (e) {
        console.log(e);
    }
}

function handleFetchDAGData(xhr, status, args) {
    console.log(args);
    try {
        $('#loading-spinner-DAG').hide();
        $('#currentAncestry').show();
    } catch (e) {
        console.log(e);
    }
    CS.overview = gograph.createNewGraph('#currentAncestry', JSON.parse(args.graph_data));
    // CS.overview = createVisGraph(args, '#currentAncestry');
}

function handleFetchOverviewChart(xhr, status, args) {

    try {
        $('#loading-spinner-overview').hide();
        args.HC_overview = JSON.parse(args.HC_overview);
    } catch (e) {
        console.log(e);
        return;
    }
    console.log(args);

    createOverviewChart(args);
}

function handleFetchGeneChart(xhr, status, args) {
    console.log(args);
    try {
        $('#loading-spinner-gene').hide();
    } catch (e) {
        console.log(e);
    }

    try {
        args.HC_gene = JSON.parse(args.HC_gene);
    } catch (e) {
        console.log(e);
        return;
    }
    createGeneCountChart(args);
}

function handleFetchEvidenceChart(xhr, status, args) {
    console.log(args);
    try {
        $('#loading-spinner-evidence').hide();
    } catch (e) {
        console.log(e);
    }

    try {
        args.HC_evidence = JSON.parse(args.HC_evidence);
    } catch (e) {
        console.log(e);
        return;
    }

    createEvidenceCountChart(args);
}


function handleFetchGraphDialog(xhr, status, args) {
    console.log(args);
    CS.dialogGraph = gograph.createNewGraph('#dagDialog', JSON.parse(args.graph_data));
    // CS.dialogGraph = createVisGraph(args, "#dagDialog");
}

function createOverviewChart(args) {

    var dateToGOEditionId = args.HC_overview.dateToGOEditionId;
    var nameChange = args.HC_overview.dateToNameChange;

    args.HC_overview.renderTo = 'hc_overview_container';
    var options = plotting.ganttHCOptions(args.HC_overview); // Custom data importer ahead

    //options.yAxis.categories = ['Name Change', 'Structure Change','Existence']

    options.plotOptions = {
        series: {
            grouping: false,
            point: {
                events: {
                    click: function () {
                        if (this.y != 0) {
                            fetchGraph([{name: 'edition', value: dateToGOEditionId[this.x]}, {
                                name: 'showDiff',
                                value: this.y == 1
                            }]);
                        }
                    }
                }
            },
        }
    };

    if (!utility.isUndefined(args.HC_overview.chart)) {

        for (var i = 0; i < args.HC_overview.chart.series.length; i++) {
            var series = args.HC_overview.chart.series[i];
            var name = series.name;

            options.yAxis.categories.push(name);

            var data = [];
            // Ugly but effective
            var pointAdder = function () {
            };
            if (i === 0) {
                pointAdder = function (point, nextPointX) {
                    if (point.y == 1) {
                        return {
                            x: point.x,
                            x2: nextPointX,
                            y: 0,
                            name: 'Name Has Changed From <b>' + nameChange[point.x][0] + '</b> To <b>' + nameChange[point.x][1] + '</b>',
                            color: '#2bce48'
                        };
                    } else {
                        return null;
                    }
                };
            } else if (i === 1) {
                pointAdder = function (point, nextPointX) {
                    if (point.y == 1) {
                        return {x: point.x, x2: nextPointX, y: i, name: 'Structure Has Changed', color: '#2bce48'};
                    } else {
                        return null;
                    }
                };
            } else if (i === 2) {

                pointAdder = function (point, nextPointX) {
                    return {
                        x: point.x,
                        x2: nextPointX,
                        y: i,
                        name: point.y == 1 ? 'Exists' : 'Does Not Exist',
                        color: point.y == 1 ? '#2bce48' : '#d63232'
                    };
                };

            } else {
                console.log("Unknown History Category", name);
            }

            for (var j = 0; j < series.data.length; j++) {
                var point = series.data[j];
                var nextPointX = (j > series.data.length - 2) ? point.x + 2629740000 : series.data[j + 1].x; //add month
                var dataPoint = pointAdder(point, nextPointX);
                if (dataPoint != null) {
                    data.push(dataPoint);
                }

            }

            options.series.push({
                name: name,
                pointWidth: 40,
                data: data
            });
        }
        plotting.charts.overview.options = options;
        plotting.charts.overview.recreate(options);


    }

}

function createGeneCountChart(args) {

    args.HC_gene.renderTo = 'hc_gene_container';
    var options = plotting.defaultHCOptions(args.HC_gene);
    plotting.addLegend(options);


    if (!utility.isUndefined(args.HC_gene.chart)) {
        //args.hc_gene_data.series.sort(function(a,b) {return (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0);} );
        for (var i = 0; i < options.series.length; i++) {
            var series = options.series[i];
            series.visible = series.name.indexOf("Direct") > -1;
            series.isolated = false;
        }

        options.exporting.buttons = {
            toggleSubsets: {
                align: 'right',
                verticalAlign: "top",
                x: -138,
                y: 0,
                onclick: function () {
                    this.toggleSubsets = (this.toggleSubsets + 1) % 3;
                    var button = this.exportSVGElements[3];
                    button.attr({fill: '#33cc33'});

                    var test = function () {
                        return true;
                    }
                    switch (this.toggleSubsets) {
                        case 0: //Direct only
                            text = "Subset: Direct";
                            test = function (s) {
                                return s.name.indexOf("Direct") > -1;
                            }
                            break;
                        case 1: //Indirect only
                            text = "Subset: Indirect";
                            test = function (s) {
                                return s.name.indexOf("Direct") == -1;
                            }
                            break;
                        case 2: //All
                            text = "Subset: All";
                            break;
                    }

                    button.parentGroup.attr({text: text});
                    var series = this.series;
                    for (var i = 0; i < series.length; i++) {
                        var s = series[i];
                        if (test(s)) {
                            s.setVisible(true, false)
                            s.isolated = false;
                        } else {
                            s.setVisible(false, false)
                            s.isolated = false;
                        }
                    }
                    this.redraw();
                },
                symbol: 'circle',
                symbolFill: '#33cc33',
                symbolStrokeWidth: 1,
                _titleKey: "toggleSubsetsTitle",
                text: 'Subset: Direct'
            }
        };

        // Monkey patch some functionality to happen before any existing legendItemClick event
        var oldLegendItemClick = options.plotOptions.series.events.legendItemClick || function () {
            };

        options.plotOptions.series.events.legendItemClick = function (event) {
            var button = this.chart.exportSVGElements[3];
            button.attr({fill: '#E0E0E0'});
            button.parentGroup.attr({text: "Subset: Custom"});
            event.target.chart.toggleSubsets = -1;
            return oldLegendItemClick(event);
        }

        plotting.charts.gene.options = options;
        plotting.charts.gene.recreate(options, function (c) {
            c.toggleSubsets = 0;
        });

    }
}

function createEvidenceCountChart(args) {

    args.HC_evidence.renderTo = 'hc_evidence_container';
    var options = plotting.defaultHCOptions(args.HC_evidence);
    plotting.addLegend(options);

    plotting.charts.evidence.options = options;
    plotting.charts.evidence.recreate(options);


}

$(document).ready(function () {
    //escDialog();

    try {

        Highcharts.setOptions({
            lang: {
                toggleSubsetsTitle: "Toggle Subsets"
            }
        });
    } catch (err) {

    }

    /**
     * Highcharts X-range series plugin
     */
    (function (H) {
        var defaultPlotOptions = H.getOptions().plotOptions,
            columnType = H.seriesTypes.column,
            each = H.each;

        defaultPlotOptions.xrange = H.merge(defaultPlotOptions.column, {});
        H.seriesTypes.xrange = H.extendClass(columnType, {
            type: 'xrange',
            parallelArrays: ['x', 'x2', 'y'],
            animate: H.seriesTypes.line.prototype.animate,

            /**
             * Borrow the column series metrics, but with swapped axes. This gives free access
             * to features like groupPadding, grouping, pointWidth etc.
             */
            getColumnMetrics: function () {
                var metrics,
                    chart = this.chart,
                    swapAxes = function () {
                        each(chart.series, function (s) {
                            var xAxis = s.xAxis;
                            s.xAxis = s.yAxis;
                            s.yAxis = xAxis;
                        });
                    };

                swapAxes();

                this.yAxis.closestPointRange = 1;
                metrics = columnType.prototype.getColumnMetrics.call(this);

                swapAxes();

                return metrics;
            },
            translate: function () {
                columnType.prototype.translate.apply(this, arguments);
                var series = this,
                    xAxis = series.xAxis,
                    yAxis = series.yAxis,
                    metrics = series.columnMetrics;

                H.each(series.points, function (point) {
                    barWidth = xAxis.translate(H.pick(point.x2, point.x + (point.len || 0))) - point.plotX;
                    point.shapeArgs = {
                        x: point.plotX,
                        y: point.plotY + metrics.offset + (point.options.yOffset || 0),
                        width: barWidth,
                        height: metrics.width
                    };
                    point.tooltipPos[0] += barWidth / 2;
                    point.tooltipPos[1] -= metrics.width / 2;
                });
            }
        });

        /**
         * Max x2 should be considered in xAxis extremes
         */
        H.wrap(H.Axis.prototype, 'getSeriesExtremes', function (proceed) {
            var axis = this,
                dataMax = Number.MIN_VALUE;

            proceed.call(this);
            if (this.isXAxis) {
                each(this.series, function (series) {
                    each(series.x2Data || [], function (val) {
                        if (val > dataMax) {
                            dataMax = val;
                        }
                    });
                });
                if (dataMax > Number.MIN_VALUE) {
                    axis.dataMax = dataMax;
                }
            }
        });
    }(Highcharts));

    CS = {};

    plotting.createNewChart('overview');
    plotting.createNewChart('gene');
    plotting.createNewChart('evidence');

});
