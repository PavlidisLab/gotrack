function onLoad() {
    $("#right-toggler").append('<span class="vertical toggled-header">Terms Annotated in Selected Edition</span>');
}

function centerResize() {
    try {
        onresize();
        PrimeFaces.widgets.funcTable.render();
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
        if (cnt >= 1) {
            // enable button
            PF('viewGOGraphWdg').enable();
        } else {
            // disable button
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
        GLOBALS.xMin = Number(dates[0]);
        GLOBALS.xMax = Number(dates[dates.length - 1]);
    }

}

function fetchCharts() {
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
    gograph.createNewGraph('#dagDialog', JSON.parse(args.graph_data));
}

function handleFetchAnnotationChart(xhr, status, args) {

    try {
        $('#loading-spinner-annotation').hide();
        $('#hc_annotation_container').show();
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
    options.exporting.chartOptions.subtitle.text = ""; // Remove subtitle
    commonOptions(options, args.HC);

    options.subtitle = {
        text: "<b>&lt;Click&gt;</b> to view annotations at a specific date. <b>&lt;Ctrl/Command&gt; + &lt;Click&gt;</b> will compare that edition to the currently selected edition.",
        style: {"font-size": "10px"}
    };

    plotting.charts.annotation.options = options;
    plotting.charts.annotation.recreate(options, function (c) {
        redrawSelectedEditionPlotLine(c, c.series[0].points[c.series[0].points.length - 1]);
        plotting.mainCharts.push(c);
    });
}

function handleFetchSimilarityChart(xhr, status, args) {

    try {
        $('#loading-spinner-similarity').hide();
        $('#hc_similarity_container').show();
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
        $('#hc_multi_container').show();
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
        var hc_timeline_container = $('#hc_timeline_container');
        hc_timeline_container.empty();
        hc_timeline_container.append("<p>" + args.HC.info + "</p>");
        return;
    }

    var options = plotting.defaultHCOptions('hc_timeline_container', args.HC.chart);
    plotting.addLegend(options);
    plotting.addAreaStreamGraphToggle(options);
    options.legend = {};
    options.chart.type = "area";
    options.chart.zoomType = 'x';

    // options.plotOptions.series.stacking='percent';
    for (var i = 0; i < options.series.length; i++) {
        options.series[i].fillColor = {
            linearGradient: { x1: 0, x2: 0, y1: 0, y2: 1 },
            stops: [
                [0, Highcharts.Color(plotting.MAXIMALLY_DISTINCT_COLORS[i]).setOpacity(1).get('rgba')],
                [1, Highcharts.Color(plotting.MAXIMALLY_DISTINCT_COLORS[i]).setOpacity(0).get('rgba')]
            ]
        };
    }

    plotting.charts.timeline.options = options;
    plotting.charts.timeline.recreate(options);

}

var comparisons = [];
function setCompareEdition(p, color) {
    destroyCompareEdition();
    addCompareEdition(p, color);

}

function addCompareEdition(p, color) {
    var edition = GLOBALS.dateToEdition[p.x];
    if (comparisons.length > 2 || comparisons.indexOf(edition) > -1) {
        return false;
    }

    comparisons.push(edition);

    if (utility.isUndefined(color)) {
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

    options.legend = {
        margin: 0,
        verticalAlign: 'bottom',
        y: 17
    };
    options.tooltip.pointFormatter = function () {
        return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + utility.sigFigs(this.y, 3) + '</b><br/>';
    };

    var clickBehaviour = function (event, p) {
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
        }
        // min: utility.isUndefined(GLOBALS.xMin) ? null : GLOBALS.xMin,
        // max: utility.isUndefined(GLOBALS.xMax) ? null : GLOBALS.xMax
    });
}

$(document).ready(function () {
    //escDialog();

    // Resize plots on window resize
    window.onresize = function (event) {
        plotting.charts.annotation.resize();
        plotting.charts.similarity.resize();
        plotting.charts.multi.resize();
    };

    GLOBALS = {dateToEdition: {}};

    plotting.createNewChart('annotation');
    plotting.createNewChart('similarity');
    plotting.createNewChart('multi');
    plotting.createNewChart('timeline');

    plotting.mainCharts = [];

    plotting.comparisonColors = ['#FF0000', '#800080', '#80ff1c', '#05feff'];

    // This fixes some strange functionality in PrimeFaces Datatable filtering.
    // By default pressing ctrl (among other things) while a filter is focused
    // will activate a table filter. This combined with the fact that clicking
    // on our HighCharts charts does not make the filers lose focus means we
    // needlessly reloading table data on Chart ctrl-clicks.
    $('div.ui-tabs-panels').on("click", "div.highcharts-container", function () {
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