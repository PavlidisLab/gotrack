/**
 * @memberOf plotting
 */
(function (plotting, $, undefined) {

    var idx = $.inArray('viewData', Highcharts.getOptions().exporting.buttons.contextButton.menuItems);
    if (idx >= 0 ) {
        Highcharts.getOptions().exporting.buttons.contextButton.menuItems.splice(idx, 1);
    }

    function HChart(id) {
        this.id = id;
        this.chart = null;
        this.options = {};
        this._exists = false;
        this.exists = function () {
            return (this.chart != null && this._exists);
        }
        this.destroy = function () {
            if (this.exists()) {
                try {
                    this.chart.destroy();
                } catch (e) {
                    console.log(e);
                }
            } else {
                console.log('Chart not yet created');
            }
            this._exists = false;

        }
        this.create = function (callback) {
            if (typeof callback === 'undefined') {
                callback = function () {
                };
            }
            if (!this.exists()) {
                this.chart = new Highcharts.Chart(this.options, callback);
                this._exists = true;
                console.log("Chart created");
            }
        }
        this.reset = function () {
            this.recreate(this.options);
        }
        this.recreate = function (options, callback) {
            if (typeof callback === 'undefined') {
                callback = function () {
                };
            }
            try {
                if (typeof options === 'undefined') {
                    // options not supplied
                    options = this.options; //fallback incase chart is not made yet
                    options = this.chart.options;
                }
                this.destroy();
            } catch (e) {
                console.log(e);
            } finally {
                try {
                    this.chart = new Highcharts.Chart(options, callback);
                    this._exists = true;
                } catch (e) {
                    console.log("Failed to create chart", e);
                }

            }

        }
        this.resize = function () {
            try {
                this.chart.reflow();
            } catch (e) {
                console.log(e);
            }
        }

    };

    plotting.MAXIMALLY_DISTINCT_COLORS = ["#2bce48", "#0075dc", "#993f00", "#4c005c", "#191919", "#005c31", "#f0a3ff", "#ffcc99", "#808080", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#990000", "#ffff00", "#ff5005"];

    var isUndefined = function (variable) {
        return ( typeof variable === 'undefined' );
    }

    plotting.charts = {};

    plotting.chart = function (id) {
        return this.charts[id];
    };

    plotting.createNewChart = function (id) {
        if (this.charts[id]) {
            throw "Id already exists"
        } else {
            this.charts[id] = new HChart(id);
        }
    };

    plotting.removeAllCharts = function () {

        for (name in this.charts) {

            this.charts[name].destroy();

        }

        this.charts = {};

    };

    plotting.destroyAllCharts = function () {

        for (name in this.charts) {

            this.charts[name].destroy();

        }

    };

    plotting.ganttHCOptions = function (renderTo, chart) {
        var options = {
            chart: {
                renderTo: renderTo,
                type: 'xrange',
                zoomType: 'x',
                resetZoomButton: {
                    position: {
                        align: 'left',
                        // verticalAlign: 'top', // by default
                        x: 0,
                        y: -35,
                    }
                }
            },
            title: {
                text: chart.title
            },

            xAxis: {
                type: 'datetime',
                title: {
                    text: chart.xLabel
                },
                minRange: 60 * 24 * 3600000 // fourteen days
            },

            yAxis: {
                type: 'linear',
                title: {
                    text: ''
                },
                labels: {
                    formatter: function () {
                        return this.value;
                    }
                },
                min: isUndefined(chart.min) ? null : chart.min,
                max: isUndefined(chart.max) ? null : chart.max,
                categories: []
            },

            plotOptions: {
                series: {
                    events: {}
                }
            },

            tooltip: {
                shared: false,
                formatter: function () {
                    return '<b>' + Highcharts.dateFormat('%b %Y',
                            new Date(this.x)) + '</b><br/>' + this.key
                },
                dateTimeLabelFormats: {
                    hour: "%B %Y",
                    minute: "%B %Y"
                }
            },
            legend: {
                enabled: false
            },

            series: [],

            colors: [],

            exporting: {
                enabled: true,
                sourceWidth: 1600,
                sourceHeight: 900,
                csv: {
                    dateFormat: '%Y-%m-%d'
                }
            }
        };

        return options;
    };


    plotting.defaultHCOptions = function (renderTo, chart) {

        var options = {
            chart: {
                renderTo: renderTo,
                zoomType: 'x',
                resetZoomButton: {
                    relativeTo: 'chart',
                    position: {
                        align: 'left',
                        // verticalAlign: 'top', // by default
                        x: 30,
                        y: 10
                    }
                }
            },
            title: {
                text: chart.title
            },

            xAxis: {
                type: 'datetime',
                title: {
                    text: chart.xLabel
                },
                minRange: 60 * 24 * 3600000 // fourteen days
            },

            yAxis: {
                type: 'linear',
                title: {
                    text: chart.yLabel
                },
                labels: {
                    formatter: function () {
                        return this.value;
                    }
                },
                min: isUndefined(chart.min) ? null : chart.min,
                max: isUndefined(chart.max) ? null : chart.max,
            },

            plotOptions: {
                series: {
                    events: {}
                }
            },

            tooltip: {
                shared: true,
                dateTimeLabelFormats: {
                    hour: "%B %Y",
                    minute: "%B %Y"
                }
            },

            series: [],

            colors: plotting.MAXIMALLY_DISTINCT_COLORS,

            exporting: {
                enabled: true,
                sourceWidth: 1600,
                sourceHeight: 900,
                csv: {
                    dateFormat: '%Y-%m-%d'
                }
            }
        };

        if (!isUndefined(chart)) {
            for (var i = 0; i < chart.series.length; i++) {
                var series = chart.series[i];
                var name = series.name;
                var data = [];

                for (var j = 0; j < series.data.length; j++) {
                    var point = series.data[j];
                    data.push([point.x, point.y]);
                }

                var seriesOptions = {
                    name: name,
                    data: data
                };
                if (!isUndefined(series.extra) && !isUndefined(series.extra.color)) {
                    seriesOptions.color = series.extra.color;
                }
                if (!isUndefined(series.extra) && !isUndefined(series.extra.title)) {
                    seriesOptions.title = series.extra.title;
                }
                if (!isUndefined(series.extra) && !isUndefined(series.extra.visible)) {
                    seriesOptions.visible = series.extra.visible;
                }
                options.series.push(seriesOptions)

            }

        }

        return options;

    };

    plotting.addLegend = function (options) {
        var doubleClicker = {
            target: -1,
            clickedOnce: false,
            timer: null,
            timeBetweenClicks: 400
        };

        var resetDoubleClick = function () {
            clearTimeout(doubleClicker.timer);
            doubleClicker.timer = null;
            doubleClicker.clickedOnce = false;
            doubleClicker.target = -1;
        };

        var isolate = function (self) {
            var seriesIndex = self.index;
            var series = self.chart.series;

            var reset = self.isolated;


            for (var i = 0; i < series.length; i++) {
                if (series[i].index != seriesIndex) {
                    if (reset) {
                        series[i].setVisible(true, false)
                        series[i].isolated = false;
                    } else {
                        series[i].setVisible(false, false)
                        series[i].isolated = false;
                    }

                } else {
                    if (reset) {
                        series[i].setVisible(true, false)
                        series[i].isolated = false;
                    } else {
                        series[i].setVisible(true, false)
                        series[i].isolated = true;
                    }
                }
            }
            self.chart.redraw();
        };

        options.legend = {
            align: 'right',
            verticalAlign: 'top',
            layout: 'vertical',
            y: 20
        };

        options.plotOptions.series = options.plotOptions.series || {};
        options.plotOptions.series.events = options.plotOptions.series.events || {};
        $.extend(options.plotOptions.series.events, {
            legendItemClick: function (event) {
                var s = event.target;
                var isolateBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;

                if (isolateBehaviour) {

                    isolate(s);

                    return false;
                } else {

                    if (doubleClicker.clickedOnce === true && doubleClicker.target === s.index && doubleClicker.timer) {
                        resetDoubleClick();
                        isolate(s);
                        return false;
                    } else {
                        doubleClicker.clickedOnce = true;
                        doubleClicker.target = s.index;
                        doubleClicker.timer = setTimeout(function () {
                            resetDoubleClick();
                        }, doubleClicker.timeBetweenClicks);
                    }
                }

            }
        });

    };

    plotting.addScaleToggle = function (options, config) {
        options.chart.resetZoomButton.position.x = 105;
        options.exporting = options.exporting || {};
        options.exporting.buttons = options.exporting.buttons || {};
        $.extend(options.exporting.buttons, {
            scaleToggle: {
                align: 'left',
                //verticalAlign:'middle',
                x: 20,
                onclick: function () {
                    // The toggling of the text is not using an official API, can break with version update!
                    if (this.yAxis[0].isLog) {
                        this.exportSVGElements[3].element.nextSibling.innerHTML = "Linear";
                        this.yAxis[0].update({type: 'linear', min: config.chart.min, max: config.chart.max});
                    } else {
                        this.exportSVGElements[3].element.nextSibling.innerHTML = "Log";
                        this.yAxis[0].update({type: 'logarithmic', min: null, max: config.chart.max});
                    }

                },
                symbol: 'circle',
                symbolFill: '#bada55',
                symbolStroke: '#330033',
                symbolStrokeWidth: 1,
                _titleKey: 'axis_toggle',
                text: 'Linear'
            }
        });
        options.lang = options.lang || {};
        $.extend(options.lang, {
            axis_toggle: 'Toggle Axis Type: Logarithmic/Linear'
        });
    };

    plotting.addLegendTooltips = function (options) {
        var styleTooltip = function (description) {
            return "<p class='description'>" + description + "</p>";
        };
        options.chart.events = options.chart.events || {};
        $.extend(options.chart.events, {
            load: function () {
                var chart = this,
                    legend = chart.legend;
                for (var i = 0, len = legend.allItems.length; i < len; i++) {
                    (function (i) {
                        var item = legend.allItems[i];
                        if (!isUndefined(item.userOptions.title)) {
                            item.legendGroup.element.setAttribute("title", styleTooltip(item.userOptions.title));
                            $(item.legendGroup.element).tipsy({gravity: "w", opacity: 0.8, html: true});
                        }
                    })(i);
                }

            }
        });
    };

    plotting.addSynchronization = function(options) {
        var that = this;
        $.extend(options.plotOptions.series, {
            point: {
                events: {
                    mouseOver: function (e) {
                        var p = this;
                        var hoverChart = p.series.chart;
                        for (var ckey in that.charts) {
                            var chart = that.charts[ckey].chart;
                            if ( hoverChart.syncGroup === chart.syncGroup ) {
                                var point = chart.series[0].data[p.index];
                                // chart.series[0].data[chart.hoverIndex].setState();
                                // chart.series[0].data[p.index].setState('hover');
                                // chart.xAxis[0].drawCrosshair(e, point); // Show the crosshair
                                chart.xAxis[0].removePlotLine('plot-line-sync');
                                chart.xAxis[0].addPlotLine({
                                    value: point.x,
                                    color: "#cccccc",
                                    width: 1,
                                    id: 'plot-line-sync'
                                });
                                // chart.hoverIndex = p.index;
                            } else {
                                chart.xAxis[0].removePlotLine('plot-line-sync');
                                // chart.xAxis[0].hideCrosshair(); // hide other crosshairs
                            }
                        }
                    }
                }
            }
        });
    }


}(window.plotting = window.plotting || {}, jQuery));