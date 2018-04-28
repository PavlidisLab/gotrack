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

    createGeneCountChart(args.HC_map.geneCount);
    createOntSizeChart(args.HC_map.ontSize);
    createTermsPerGeneChart(args.HC_map.termsPerGene);
    createGenesPerTermChart(args.HC_map.genesPerTerm);
    createMultiChart(args.HC_map.multi);
    createSimilarityChart(args.HC_map.similarity);

}

function createGeneCountChart(chart) {
    var options = plotting.defaultHCOptions('hc-geneCount', chart);
    commonOptions(options);
    options.legend = {enabled: false};
    createChart(options, 'geneCount');
}

function createOntSizeChart(chart) {
    var options = plotting.defaultHCOptions('hc-ontSize', chart);
    commonOptions(options);
    options.legend = {enabled: false};
    createChart(options, 'ontSize');
}

function createTermsPerGeneChart(chart) {
    var options = plotting.defaultHCOptions('hc-termsPerGene', chart);
    options.exporting.chartOptions.legend.enabled = true;
    plotting.addSynchronization(options, 2);
    commonOptions(options);
    createChart(options, 'termsPerGene');
}

function createGenesPerTermChart(chart) {
    var options = plotting.defaultHCOptions('hc-genesPerTerm', chart);
    plotting.addSynchronization(options, 2);
    commonOptions(options);
    createChart(options, 'genesPerTerm');
}

function createMultiChart(chart) {
    var options = plotting.defaultHCOptions('hc-multi', chart);
    plotting.addSynchronization(options, 3);
    commonOptions(options);
    createChart(options, 'multi');
}

function createSimilarityChart(chart) {
    var options = plotting.defaultHCOptions('hc-similarity', chart);
    options.exporting.chartOptions.legend.enabled = true;
    plotting.addSynchronization(options, 3);
    commonOptions(options);
    createChart(options, 'similarity');
}

function createChart(options, ckey) {
    $('#loading-spinner-' + ckey).hide();
    plotting.createNewChart(ckey);
    plotting.charts[ckey].options = options;
    plotting.charts[ckey].recreate(options, function (c) {
        c.syncGroup = options.syncGroup;
    });
}

function commonOptions(options) {
    plotting.addLegend(options);
    options.legend = {
        margin: 0,
        verticalAlign: 'bottom',
        y: 17
    };
    options.xAxis.crosshair = true;

    options.tooltip.pointFormatter = function () {
        return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + utility.sigFigs(this.y, 3) + '</b><br/>';
    };
}


$(document).ready(function () {
    CS = {};
    GLOBALS = {dateToEdition: {}};
});
