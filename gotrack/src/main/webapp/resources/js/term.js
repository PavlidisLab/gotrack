function onLoad() {
}

function centerResize() {
    onresize();
}

function fetchCharts() {
    $('.loading').show();
    fetchDAGData();
    fetchEvidenceChart();
    fetchOverviewChart();
    fetchGeneChart();
}

function preSwitchSpecies() {
    try {
        $('#loading-spinner-gene').show();
        $('#hc_gene_container').hide();
        $('#loading-spinner-evidence').show();
        $('#hc_evidence_container').hide();
    } catch (e) {
        console.log(e);
    }

    plotting.charts.gene.destroy();
    // delete plotting.charts.gene;
    plotting.charts.evidence.destroy();
    // delete plotting.charts.evidence;

}

function postSwitchSpecies() {

    fetchEvidenceChart();
    fetchGeneChart();
}

function handleFetchDAGData(xhr, status, args) {
    // console.log(args);
    try {
        $('#loading-spinner-DAG').hide();
        $('#currentAncestryLogo').show();
    } catch (e) {
        console.log(e);
    }
    CS.logo = gograph.createNewGraphLogo('#currentAncestryLogo', JSON.parse(args.graph_data));

    $('#currentAncestryLogo').click(function(e) {
        PF('graphDlgWdg').show();
        handleFetchGraphDialog(null, null, args);
    });
    // $.onclick()
    // CS.overview = createVisGraph(args, '#currentAncestry');
}

function handleFetchOverviewChart(xhr, status, args) {
    // console.log(args);
    try {
        $('#loading-spinner-overview').hide();
        $('#hc_overview_container').show();
        args.HC_overview = JSON.parse(args.HC_overview);
    } catch (e) {
        console.log(e);
        return;
    }

    createOverviewChart(args);
}

function handleFetchGeneChart(xhr, status, args) {
    // console.log(args);
    try {
        $('#loading-spinner-gene').hide();
        $('#hc_gene_container').show();
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
    // console.log(args);
    try {
        $('#loading-spinner-evidence').hide();
        $('#hc_evidence_container').show();

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
    // console.log(args);
    if (!$.isEmptyObject(args)) {
        CS.dialogGraph = gograph.createNewGraph('#dagDialog', JSON.parse(args.graph_data));
    }
    // CS.dialogGraph = createVisGraph(args, "#dagDialog");
}

function createOverviewChart(args) {

    var dateToGOEditionId = args.HC_overview.dateToGOEditionId;
    var nameChange = args.HC_overview.dateToNameChange;

    var options = plotting.ganttHCOptions('hc_overview_container', args.HC_overview.chart); // Custom data importer ahead

    options.credits = {enabled:false};

    options.subtitle.style= {
        fontSize: "0.9em"
    };

    options.plotOptions = {
        series: {
            grouping: false,
            point: {
                events: {
                    click: function (e) {
                        if (this.y === 1) {
                            var indexAmongAllEditions = this.series.chart.series[2].xData.indexOf(this.x);
                            var previousX = this.series.chart.series[2].xData[indexAmongAllEditions-1];
                            fetchGraphDiff([{name: 'goId', value: args.HC_overview.goId},
                                {name: 'edition', value: dateToGOEditionId[this.x]},
                                {name: 'compareEdition', value: dateToGOEditionId[previousX]}]);
                        } else if (this.y === 2) {
                            fetchGraph([{name: 'goId', value: args.HC_overview.goId},
                                {name: 'edition', value: dateToGOEditionId[this.x]}]);
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
                    if (point.y === 1) {
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
                    if (point.y === 1) {
                        return {x: point.x, x2: nextPointX, y: 1, name: 'Structure Has Changed', color: '#2bce48'};
                    } else {
                        return null;
                    }
                };
            } else if (i === 2) {

                pointAdder = function (point, nextPointX) {
                    return {
                        x: point.x,
                        x2: nextPointX,
                        y: 2,
                        name: point.y === 1 ? 'Exists' : 'Does Not Exist',
                        color: point.y === 1 ? '#2bce48' : '#d63232'
                    };
                };

            } else {
                console.log("Unknown History Category", name);
            }

            for (var j = 0; j < series.data.length; j++) {
                var point = series.data[j];
                var nextPointX = (j > series.data.length - 2) ? point.x + 2629740000 : series.data[j + 1].x; //add month
                var dataPoint = pointAdder(point, nextPointX);
                if (dataPoint !== null) {
                    data.push(dataPoint);
                }

            }

            options.series.push({
                name: name,
                data: data
            });
        }
        plotting.charts.overview.options = options;
        plotting.charts.overview.recreate(options);


    }

}

function createGeneCountChart(args) {

    var options = plotting.defaultHCOptions('hc_gene_container', args.HC_gene.chart);
    commonOptions(options);


    if (!utility.isUndefined(args.HC_gene.chart)) {

        var multipleSpecies = options.series.length > 2;

        for (var i = 0; i < options.series.length; i++) {
            var series = options.series[i];
            if (multipleSpecies) {
                series.visible = series.name.indexOf("Direct") > -1;
            }
            series.isolated = false;
        }

        if (multipleSpecies) {

            options.exporting.buttons = {
                toggleSubsets: {
                    align: 'right',
                    verticalAlign: "top",
                    x: -30,
                    y: 0,
                    onclick: function () {
                        this.toggleSubsets = (this.toggleSubsets + 1) % 3;
                        var button = this.exportSVGElements[3];
                        button.attr({fill: '#33cc33'});

                        var test = function () {
                            return true;
                        };
                        switch (this.toggleSubsets) {
                            case 0: //Direct only
                                text = "Subset: Direct";
                                test = function (s) {
                                    return s.name.indexOf("Direct") > -1;
                                };
                                break;
                            case 1: //Indirect only
                                text = "Subset: Indirect";
                                test = function (s) {
                                    return s.name.indexOf("Direct") === -1;
                                };
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
                                s.setVisible(true, false);
                                s.isolated = false;
                            } else {
                                s.setVisible(false, false);
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
            };
        }

        plotting.charts.gene.options = options;
        plotting.charts.gene.recreate(options, function (c) {
            c.toggleSubsets = 0;
        });

    }
}

function createEvidenceCountChart(args) {

    var options = plotting.defaultHCOptions('hc_evidence_container', args.HC_evidence.chart);
    commonOptions(options);

    plotting.charts.evidence.options = options;
    plotting.charts.evidence.recreate(options);


}

function commonOptions(options) {
    plotting.addLegend(options);
    options.legend = {
        margin: 0,
        verticalAlign: 'bottom',
        y: 17
    };
    // options.credits = {enabled:false};

    options.subtitle.style= {
        fontSize: "0.9em"
    };

    options.xAxis.crosshair = {
        width: 1,
        color: 'red',
        dashStyle: 'shortdot'
    };
}

$(document).ready(function () {
    //escDialog();

    // Resize plots on window resize
    window.onresize = function (event) {
        plotting.charts.overview.resize();
        plotting.charts.gene.resize();
        plotting.charts.evidence.resize();
    };

    try {

        Highcharts.setOptions({
            lang: {
                toggleSubsetsTitle: "Toggle Subsets"
            }
        });
    } catch (err) {

    }

    CS = {};

    plotting.createNewChart('overview');
    plotting.createNewChart('gene');
    plotting.createNewChart('evidence');

});
