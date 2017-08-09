function onLoad() {
    $("#right-toggler").append('<span class="vertical toggled-header">Terms Annotated in Selected Edition</span>');
}

function centerResize() {
    try {
        var index = PrimeFaces.widgets.centerTabWdg.getActiveIndex();
        tabShowed(index);
        PrimeFaces.widgets.funcTable.render();
    } catch (e) {
        console.log(e);
    }
}

function tabShowed(index) {
    try {
        if (index == 0) {
            plotting.charts.annotation.resize();
        } else if (index == 1) {
            plotting.charts.similarity.resize();
        } else if (index == 2) {
            plotting.charts.multi.resize();
        } else if (index == 3) {
            plotting.charts.lossgain.resize();
        }
    } catch (e) {
        console.log(e);
    }
}

function timelineDlgHide() {
    plotting.charts.timeline.destroy();
}

function timelineDlgResize() {
    plotting.charts.timeline.resize();
}

function afterRowSelection() {
    var cnt = PF('funcTable').getSelectedRowsCount();
    try {
        if (cnt > 1) {
            // enable View Terms button
            PF('viewTermsWdg').enable();
            PF('viewGOGraphWdg').enable();
        } else {
            // disable View Terms button
            PF('viewTermsWdg').disable();
            PF('viewGOGraphWdg').disable();
        }
    } catch (e) {
        console.log(e);
    }

}

function showViewAnnotationsDlg() {
    try {
        var dlg = PF('viewAnnotationsDlgWdg');
        dlg.show();
        dlg.moveToTop();
    } catch (e) {
        console.log(e);
    }
}

function handleFetchData(xhr, status, args) {
    if (!utility.isUndefined(args.dateToEdition)) {
        GLOBALS.dateToEdition = JSON.parse(args.dateToEdition);
        var dates = Object.keys(GLOBALS.dateToEdition).map(Number);
        dates.sort();
        GLOBALS.xMin = Number(dates[0])
        GLOBALS.xMax = Number(dates[dates.length - 1])
    }

}

function fetchCharts() {
    try {
        PF('viewTermsWdg').disable();
    } catch (e) {
        console.log(e);
    }

    try {
        PF('viewGOGraphWdg').disable();
    } catch (e) {
        console.log(e);
    }

    try {
        plotting.destroyAllCharts();
        fetchAnnotationChart();
        fetchSimilarityChart();
        fetchMultiChart();
    } catch (e) {
        console.log(e);
    }
}

function handleFetchEditionsForSelectedTerms(xhr, status, args) {
    var missing_edition_arr = JSON.parse(args.missing_editions);

    var missing_edition = {};
    for (var i = 0; i < missing_edition_arr.length; ++i) {
        missing_edition[missing_edition_arr[i]] = 1;
    }

    console.log(missing_edition);

    var chart = plotting.charts.annotation.chart;
    var series = chart.series;

    // deselect all points;
    Highcharts.each(chart.getSelectedPoints(), function (point) {
        point.select(false);
    });

    for (var j = 0; j < series.length; j++) {
        var s = series[j];
        for (var k = 0; k < s.points.length; k++) {
            var p = s.points[k];
            if (!missing_edition[p.x]) {
                p.select(true, true);
            }
        }
    }

}

function handleFetchGraphDialog(xhr, status, args) {
    console.log(args);
    gograph.createNewGraph('#dagDialog', JSON.parse(args.graph_data));
}

function handleFetchAnnotationChart(xhr, status, args) {

    try {
        $('#loading-spinner-annotation').hide();
    } catch (e) {
        console.log(e);
    }

    try {
        args.HC = JSON.parse(args.HC);
    } catch (e) {
        console.log(e);
        return;
    }

    console.log('handleFetchAnnotationChart', args);

    var options = plotting.defaultHCOptions('hc_annotation_container', args.HC.chart);
    commonOptions(options, args.HC);

    plotting.charts.annotation.options = options;
    plotting.charts.annotation.recreate(options, function (c) {
        redrawSelectedEditionPlotLine(c, c.series[0].points[c.series[0].points.length - 1]);
        plotting.mainCharts.push(c);
    });
}

function handleFetchSimilarityChart(xhr, status, args) {

    try {
        $('#loading-spinner-similarity').hide();
    } catch (e) {
        console.log(e);
    }

    try {
        args.HC = JSON.parse(args.HC);
    } catch (e) {
        console.log(e);
        return;
    }

    console.log('handleFetchSimilarityChart', args);

    var options = plotting.defaultHCOptions('hc_similarity_container', args.HC.chart);
    commonOptions(options, args.HC);

    options.yAxis.endOnTick = false; // Make sure log axis follows our given max

    plotting.charts.similarity.options = options;
    plotting.charts.similarity.recreate(options, function (c) {
        redrawSelectedEditionPlotLine(c, c.series[0].points[c.series[0].points.length - 1]);
        plotting.mainCharts.push(c);
    });
}

function handleFetchMultiChart(xhr, status, args) {

    try {
        $('#loading-spinner-multifunctionality').hide();
    } catch (e) {
        console.log(e);
    }

    try {
        args.HC = JSON.parse(args.HC);
    } catch (e) {
        console.log(e);
        return;
    }
    console.log('handleFetchMultiChart', args);

    var options = plotting.defaultHCOptions('hc_multi_container', args.HC.chart);
    commonOptions(options, args.HC);

    plotting.charts.multi.options = options;
    plotting.charts.multi.recreate(options, function (c) {
        redrawSelectedEditionPlotLine(c, c.series[0].points[c.series[0].points.length - 1]);
        plotting.mainCharts.push(c);
    });
}

function handleFetchTimeline(xhr, status, args) {
    try {
        args.HC = JSON.parse(args.HC);
    } catch (e) {
        console.log(e);
        return;
    }
    console.log('handleFetchTimeline', args);


    if (!args.HC.success) {
        console.log(args.HC.info);
        $('#hc_timeline_container').empty();
        $('#hc_timeline_container').append("<p>" + args.HC.info + "</p>");
        return;
    }

    var tmp = args.HC.category_positions;
    var evidenceCategories = new Array(tmp.length);

    // Inverse the map
    for (var cat in tmp) {
        evidenceCategories[tmp[cat]] = cat;
    }

    var categories = [];
    var tooltipData = {};
    for (var i = 0; i < args.HC.chart.series.length; i++) {
        categories.push(args.HC.chart.series[i].name);
        tooltipData[i] = {};
    }

    var termNames = args.HC.term_names;

    var options = plotting.ganttHCOptions('hc_timeline_container', args.HC.chart);
    options.colors = plotting.MAXIMALLY_DISTINCT_COLORS;

    options.legend = {};
    options.yAxis = {
        categories: categories,
        min: 0,
        max: categories.length - 1,
        title: '',
        labels: {
            formatter: function () {
                return this.value;
            }
        }
    };

    options.plotOptions = {
        series: {
            grouping: false,
            turboThreshold: 0,
            cropThreshold: 100000,
            point: {
                events: {
                    click: function () {
                        fetchTimelinePointData([{
                            name: 'edition',
                            value: GLOBALS.dateToEdition[this.x]
                        }, {name: 'termId', value: categories[this.y]}]);
                    }
                }
            }
        }
    };

    options.yAxis.labels.formatter = function () {
        var name = termNames[this.value];
        var trimName = name;
        if (name.length > 25) {
            trimName = name.substring(0, 22) + "...";
        }
        var styleTooltip = function (n, d) {
            return "<p class='name'>" + n + "</p><p class='description'>" + d + "</p>";
        };
        return '<span class="timeline-yaxis-category clearfix" title="' + styleTooltip(this.value, name) + '"><div style="text-align: right;">' + this.value + '</div><div style="text-align: right;">' + trimName + '</div></span>'
        //return '<span title="'+termNames[this.value]+'">' + this.value + '</span><span>'+termNames[this.value]+'</span>';
    };
    options.yAxis.labels.useHTML = true;

    var height = $('#hc_timeline_container').height();
    var pointWidth = 5;
    if (height != null) {
        var pointWidth = (height - 100) / categories.length / evidenceCategories.length / 2 - 2
        pointWidth = (pointWidth < 0.5 ? 0.5 : pointWidth);
    }


    for (var k = 0; k < evidenceCategories.length; k++) {
        var cat = evidenceCategories[k];
        options.series.push({
            name: cat,
            pointWidth: pointWidth,
            data: []
        });
    }

    for (var i = 0; i < args.HC.chart.series.length; i++) {
        var series = args.HC.chart.series[i];
        var name = series.name;
        var data = []
        var tData = tooltipData[i];
        for (var j = 0; j < series.data.length; j++) {
            var point = series.data[j];
            var nextPointX = (j > series.data.length - 2) ? point.x + 2629740000 : series.data[j + 1].x; //get next edition or add month

            var tArr = [];

            for (var k = 0; k < evidenceCategories.length; k++) {
                var cat = evidenceCategories[k];
                var mask = 1 << k;
                if ((point.y & mask) != 0) {
                    // bit is set
                    options.series[k].data.push({x: point.x, x2: nextPointX, y: i, name: cat, yOffset: pointWidth * k});
                    tArr.push(k);
                } else {
                    // bit is not set
                }
            }

            tData[point.x] = tArr;
        }

    }

    // Highcharts requires sorted data
    for (var k = 0; k < options.series.length; k++) {
        options.series[k].data.sort(function (a, b) {
            return a.x - b.x;
        });
    }

    options.tooltip.formatter = function () {
        var s = '<b>' + Highcharts.dateFormat('%b %Y',
                new Date(this.x)) + '</b>';

        var data = tooltipData[this.y][this.x];

        for (var i = 0; i < data.length; i++) {
            var k = data[i];
            s += '<br/>' + evidenceCategories[k]
        }

        return s;
    }
//   options.tooltip.positioner = function () {
//      return { x: 80, y: 50 };
//   }

    plotting.charts.timeline.options = options;
    plotting.charts.timeline.recreate(options, function (c) {
        $("span.timeline-yaxis-category").tipsy({gravity: "w", opacity: 0.8, html: true});
    });


}

var comparisons = [];
function setCompareEdition(p, color) {
    destroyCompareEdition();
    addCompareEdition(p, color);

}

function addCompareEdition(p, color) {
    var edition = GLOBALS.dateToEdition[p.x];
    if ( comparisons.length > 2 || comparisons.indexOf(edition) > -1 ) {
        return false;
    }

    comparisons.push(edition);

    if (utility.isUndefined(color) ) {
        color = plotting.comparisonColors[comparisons.length];
    }

    plotting.mainCharts.forEach(function (c) {

        c.xAxis[0].addPlotLine({
            value: p.x,
            color: color,
            width: 1,
            id: 'plot-line-compare'
        });
    });
    return true;
}

function destroyCompareEdition() {
    comparisons = [];
    plotting.mainCharts.forEach(function (c) {
        destroyCompareEditionLine(c);
    });
}

function destroyCompareEditionLine(c) {
    c.xAxis[0].removePlotLine('plot-line-compare');
}

function destroySelectedEdition(c) {
    c.xAxis[0].removePlotLine('plot-line-selected');
}

function redrawSelectedEditionPlotLine(c, p) {
    var chart = c;

    destroySelectedEdition(chart);

    chart.xAxis[0].addPlotLine({
        value: p.x,
        color: plotting.comparisonColors[0],
        width: 1,
        id: 'plot-line-selected'
    });
}

function toggleTags() {
    var chkBox = PF('colToggler').itemContainer.children('li:nth-child(2)').children('div.ui-chkbox');
    PF('colToggler').toggle(chkBox);
}

function showTags() {
    var chkBox = PF('colToggler').itemContainer.children('li:nth-child(2)').children('div.ui-chkbox');
    PF('colToggler').check(chkBox);
}

function hideTags() {
    var chkBox = PF('colToggler').itemContainer.children('li:nth-child(2)').children('div.ui-chkbox');
    PF('colToggler').uncheck(chkBox);
}

function commonOptions(options, config) {
    plotting.addLegend(options);
    plotting.addScaleToggle(options, config);
    options.subtitle = {
        text: "Click to view annotations at a specific date",
        style: {"font-size": "10px"}
    };
    options.legend = {};
    options.tooltip.pointFormatter = function () {
        return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + utility.sigFigs(this.y, 3) + '</b><br/>';
    };

    var clickBehaviour = function(event, p) {
        var compareBehaviour = event.metaKey || event.ctrlKey;
        if (!compareBehaviour) {
            hideTags();
            fetchAnnotationPointData([{name: 'edition', value: GLOBALS.dateToEdition[p.x]}]);
            plotting.mainCharts.forEach(function (c) {
                redrawSelectedEditionPlotLine(c, p);
            });
            destroyCompareEdition();
        } else {
            showTags();
            if (event.shiftKey) {
                var added = addCompareEdition(p);
                if (added) {
                    addAnnotationComparisonData([{name: 'compareEdition', value: GLOBALS.dateToEdition[p.x]}]);
                }
            } else {
                fetchAnnotationComparisonData([{name: 'compareEdition', value: GLOBALS.dateToEdition[p.x]}]);
                setCompareEdition(p);

            }
        }
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

    // ********** Used for Highlighting editions **********
    options.plotOptions.series.marker = {
        states: {
            select: {
                fillColor: null,
                lineColor: "red",
                lineWidth: 1
            }
        }
    };
    // ****************************************************

    $.extend(options.xAxis, {
        crosshair: {
            width: 1,
            color: 'red',
            dashStyle: 'shortdot'
        },
        min: utility.isUndefined(GLOBALS.xMin) ? null : GLOBALS.xMin,
        max: utility.isUndefined(GLOBALS.xMax) ? null : GLOBALS.xMax
    });
}

$(document).ready(function () {
    //escDialog();

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

    GLOBALS = {dateToEdition: {}};

    plotting.createNewChart('annotation');
    plotting.createNewChart('similarity');
    plotting.createNewChart('multi');
    plotting.createNewChart('timeline');

    plotting.mainCharts = [];

    plotting.comparisonColors = ['#FF0000','#800080','#80ff1c', '#05feff'];

    // This fixes some strange functionality in PrimeFaces Datatable filtering.
    // By default pressing ctrl (among other things) while a filter is focused
    // will activate a table filter. This combined with the fact that clicking
    // on our HighCharts charts does not make the filers lose focus means we
    // needlessly reloading table data on Chart ctrl-clicks.
    $('div.ui-tabs-panels').on("click", "div.highcharts-container", function() {
        $('input.ui-column-filter').blur();
    });

// Used for arbitrary numbers of tags
/*    var n = plotting.MAXIMALLY_DISTINCT_COLORS.length;
    var style = document.createElement('style');
    style.type = 'text/css';
    style.innerHTML = '';
    for (var i = 0; i < plotting.MAXIMALLY_DISTINCT_COLORS.length; i++) {
        var color = plotting.MAXIMALLY_DISTINCT_COLORS[n - 1 - i];
        style.innerHTML += '.tag-' + i + ' { color: ' + color + '; }\n';
        style.innerHTML += 'div.selectButtonFA > div.ui-button:nth-child(' + (i+1) + ') > span:before { color: ' + color + '; }\n';
    }
    document.getElementsByTagName('head')[0].appendChild(style);*/

});