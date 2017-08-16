function onLoad() {
}

function handleFetchCharts(xhr, status, args) {

    try {
        $('.loading').hide();
        args.HC_map = JSON.parse(args.HC_map);
    } catch (e) {
        console.log(e);
        return;
    }

    var syncGroups = {
        // 'ontSize': 1,
        // 'geneCount': 1,
        'termsPerGene': 2,
        'genesPerTerm': 2,
        'multi': 3,
        'similarity': 3
    };

    for (var ckey in args.HC_map) {
        var chart = args.HC_map[ckey];
        $('#loading-spinner-' + ckey).hide();

        var options = plotting.defaultHCOptions('hc-' + ckey, chart);

        if (syncGroups[ckey]) {
            plotting.addSynchronization(options);
        }
        if (options.series.length === 1) {
            options.legend = {enabled: false};
        } else {
            plotting.addLegend(options);
        }
        options.xAxis.crosshair = true;

        options.tooltip.pointFormatter = function () {
            return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + utility.sigFigs(this.y, 3) + '</b><br/>';
        };

        plotting.createNewChart(ckey);

        plotting.charts[ckey].options = options;
        plotting.charts[ckey].recreate(options, function(c) {
            c.syncGroup = syncGroups[ckey];
        });
    }
}


$(document).ready(function () {
    CS = {};
    GLOBALS = {dateToEdition: {}};
});
