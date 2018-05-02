#!/usr/bin/env python
#
# The GOTrack project
#
# Copyright (c) 2015 University of British Columbia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

"""Analysis"""

from __future__ import division
from operator import methodcaller
import sys
import datetime
import csv
import matplotlib.pyplot as plt
import numpy as np
import random
from collections import namedtuple
from itertools import groupby

import logging
import logging.config
# Example logging
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()

def date_(x):
    return datetime.datetime.strptime(x, "%Y-%m-%d").date()
def list_(x):
    return set(x.split(",")) if x else set()
def list2_(x):
    return map(methodcaller("split", "|"), x.split(",")) if x else []

DATA_HEADER = [("sys_name", str),
               ("name", str),
               ("pmid", str),
               ("species", str),
               ("age", int),
               ("date", str),
               ("date_requested", date_),
               ("edition", int),
               ("edition_date", date_),
               ("edition_go_date", date_),
               ("significant_terms", int),
               ("significant_terms_current", int),
               ("complete_term_jaccard", float),
               ("top_term_jaccard", float),
               ("top_gene_jaccard", float),
               ("top_parents_jaccard", float),
               ("top_term_tversky", float),
               ("top_gene_tversky", float),
               ("top_parents_tversky", float),
               ("top_parents_mf", float),
               ("top_parents_mf_current", float),
               ("top_terms", list_),
               ("top_terms_current", list_),
               ("top_parents", list_),
               ("top_parents_current", list_),
               ("top_genes", list_),
               ("top_genes_current", list_),
               ("genes_found", list2_),
               ("genes_missed", list_)]

NULL_HEADER = [("PERMUTATION_RUN", int),
               ("SYSTEMATIC_NAME_THEN", str),
               ("SYSTEMATIC_NAME_NOW", str),
               ("THEN_DATE", date_),
               ("JACCARD", float),
               ("TVERSKY", float)]

GeneSetAnalysis = namedtuple('GeneSetAnalysis', [h[0] for h in DATA_HEADER])
NullSet = namedtuple('NullDistribution', [h[0] for h in NULL_HEADER])

# [0] "sys_name"
# [1] "name"                      "pmid"                      "species"                   "age"
# [5] "date"                      "date_requested"            "edition"                   "edition_date"              "edition_go"
# [10] "edition_go_date"          "significant_terms"         "significant_terms_current" "complete_term_jaccard"
#[14] "top_term_jaccard"          "top_gene_jaccard"          "top_parents_jaccard"       "top_parents_mf"
#[18] "top_parents_mf_current"    "top_terms"                 "top_terms_current"         "top_parents"
#[22] "top_parents_current"       "top_genes"                 "top_genes_current"         "genes_found"
#[26] "genes_missed"


def parse_results(file):
    settings = {}
    # read tab-delimited file
    with open(file,'rb') as infile:
        reader = csv.reader(infile, delimiter='\t')

        for r in reader:
            if not r[0].startswith("# "):
                # Check header
                assert r == [h[0] for h in DATA_HEADER]
                break
            # parse settings
            settings[r[0][2:-1]] = r[1]
        type_parser = [h[1] for h in DATA_HEADER]
        filecontents = [GeneSetAnalysis(*map(lambda x, y:x(y), type_parser, line)) for line in reader]

    settings['reference_edition_date'] = parse_date( settings['reference_edition_date'], '%Y-%m-%d')
    settings['reference_edition_go_date'] = parse_date( settings['reference_edition_go_date'], '%Y-%m-%d')
    return filecontents, settings


def parse_null(f):
    # read tab-delimited file
    with open(f, 'rb') as infile:
        reader = csv.reader(infile, delimiter='\t')
        header = next(reader)
        assert header == [h[0] for h in NULL_HEADER]

        type_parser = [header[1] for header in NULL_HEADER]
        contents = [NullSet(*map(lambda t, c:t(c), type_parser, line)) for line in reader]

    return contents


def parse_date(d, format):
    return datetime.datetime.strptime(d, format).date()

def mean(data):
    """Return the sample arithmetic mean of data."""
    n = len(data)
    if n < 1:
        raise ValueError('mean requires at least one data point')
    return sum(data)/n # in Python 2 use sum(data)/float(n)

def _ss(data):
    """Return sum of square deviations of sequence data."""
    c = mean(data)
    ss = sum((x-c)**2 for x in data)
    return ss

def sstdev(data):
    """Calculates the sample standard deviation."""
    n = len(data)
    if n < 2:
        raise ValueError('variance requires at least two data points')
    ss = _ss(data)
    pvar = ss/(n-1) # the sample variance
    return pvar**0.5

def median(data):
    """Return the median (middle value) of numeric data.

    When the number of data points is odd, return the middle data point.
    When the number of data points is even, the median is interpolated by
    taking the average of the two middle values:

    median([1, 3, 5]) -> 3
    median([1, 3, 5, 7]) -> 4.0

    """
    data = sorted(data)
    n = len(data)
    if n == 0:
        raise ValueError("no median for empty data")
    if n%2 == 1:
        return data[n//2]
    else:
        i = n//2
        return (data[i - 1] + data[i])/2

def r_square(p1d, x, y):
    # fit values, and mean
    yhat = p1d(x)                         # or [p(z) for z in x]
    ybar = np.sum(y)/len(y)          # or sum(y)/len(y)
    ssreg = np.sum((yhat-ybar)**2)   # or sum([ (yihat - ybar)**2 for yihat in yhat])
    sstot = np.sum((y - ybar)**2)    # or sum([ (yi - ybar)**2 for yi in y])
    return ssreg / sstot

def flatten(l):
    return (item for sublist in l for item in sublist)

def density_plot(x, y, nb=32, xsize=500, ysize=500):
    import sphviewer as sph  
    xmin = np.min(x)
    xmax = np.max(x)
    ymin = np.min(y)
    ymax = np.max(y)
    x0 = (xmin+xmax)/2.
    y0 = (ymin+ymax)/2.
    pos = np.zeros([3, len(x)])
    pos[0,:] = x
    pos[1,:] = y
    w = np.ones(len(x))
    P = sph.Particles(pos, w, nb=nb)
    S = sph.Scene(P)
    S.update_camera(r='infinity', x=x0, y=y0, z=0, xsize=xsize, ysize=ysize)
    R = sph.Render(S)
    R.set_logscale()
    img = R.get_image()
    extent = R.get_extent()
    for i, j in zip(xrange(4), [x0,x0,y0,y0]):
        extent[i] += j
    log.info(extent)
    fig = plt.figure(1)
    plt.imshow(img, extent=extent, origin='lower', aspect='auto')
    plt.title("Smoothing over " + str(nb) + " neighbors")
    plt.show()

def scatter_plot(x, y, alpha=1, x_label='', y_label='', title='', sample=0):
    if sample > 0:
        x, y = zip(*random.sample(list(zip(x, y)), sample))
    fit = np.polyfit(x,y,1)
    fit_fn = np.poly1d(fit)
    plt.plot(x,y, 'k.', alpha=alpha)
    plt.plot([min(x), max(x)], fit_fn([min(x), max(x)]), '-g', alpha=1.0, label = 'R^2=' + str(r_square(fit_fn, x, y)))
    plt.xlabel(x_label)
    plt.ylabel(y_label)
    plt.title(title + " | " + r'$\mu=' + str(round(mean(y), 2)) + r',\ \sigma=' + str(round(sstdev(y), 3)) + r'$' )

def histogram(d, bins=100, x_label='', title='', alpha=1, label='', stats_on_label=True):
    stats_text = " | " + r'$\mu=' + str(round(mean(d), 2)) + r',\ \sigma=' + str(round(sstdev(d), 3)) + r'$'
    weights = np.ones_like(d)/float(len(d))
    plt.hist(d, bins, weights=weights, alpha=alpha, label=label + (stats_text if stats_on_label else ''))
    plt.xlabel(x_label)
    plt.ylabel('Frequency')
    plt.title(title + (stats_text if not stats_on_label else ''))

def violinplot(x, d, x_label='', y_label='', title='', label='', widths=None):
    nans = [float('nan'), float('nan')] # requires at least 2 nans
    plt.violinplot([v if v.any() else nans for v in d], x, showmeans=True, showextrema=True, showmedians=False, widths=widths)
    plt.xlabel(x_label)
    plt.ylabel(y_label)
    plt.title(title)

def violinplot_histo(x, d, bins=100, x_label='', y_label='', title='', label='', widths=None):
    d = np.array(d)
    y,binEdges = np.histogram(x,bins=bins)
    bincenters = 0.5*(binEdges[1:]+binEdges[:-1])
    widths = (bincenters[1] - bincenters[0])
    digitize = np.digitize(x, binEdges)
    binned = [d[digitize == i] for i in range(1, len(binEdges))]
    violinplot(bincenters, binned, x_label=x_label, y_label=y_label, title=title, label=label, widths=widths )


def analysis(data, settings, flat_age_matrix=[], flat_sim_matrix=[], alpha=0.03):
    # value_keys = ["complete_term_jaccard", "top_term_jaccard", "top_gene_jaccard", "top_parents_jaccard"]
    value_keys = ["top_gene_jaccard", "top_parents_jaccard"]
    data_values = [[row._asdict()[k] for k in value_keys] for row in data]
    means = [mean([x[i] for x in data_values]) for i in range(len(value_keys))]
    stds = [sstdev([x[i] for x in data_values]) for i in range(len(value_keys))]
    medians = [median([x[i] for x in data_values]) for i in range(len(value_keys))]

    log.info("N: %s", len(data_values))
    genes_found, genes_missed = len([genes[1] for row in data for genes in row.genes_found]),len([genes for row in data for genes in row.genes_missed])
    log.info("genes_found: %s, genes_missed: %s", genes_found, genes_missed)
    total = genes_found + genes_missed
    log.info("genes_found: %s, genes_missed: %s", round(genes_found / total, 2), round(genes_missed / total, 2))
    #log.info("distinct unknown genes: %s", len(unknown))
    fig_num = 0
    for i, value_key in enumerate(value_keys):
        log.info("%s: mean=%s, std=%s, median=%s", value_key, means[i], stds[i], medians[i])

        f = plt.figure(fig_num)
        fig_num +=1
        d = [x[i] for x in data_values]
        weights = np.ones_like(d)/float(len(d))
        plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

        weights = np.ones_like(flat_sim_matrix)/float(len(flat_sim_matrix))
        plt.hist(flat_sim_matrix, 100, weights=weights, alpha=0.5, label='Randomized')

        plt.xlabel('Similarity')
        plt.ylabel(value_keys[i])
        plt.title('Histogram of ' + value_key + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$')
        plt.legend(loc='upper right')
        f.show()

        f = plt.figure(fig_num)
        fig_num +=1
        x = [r.age for r in data]
        y = [r._asdict()[value_key] for r in data]
        fit = np.polyfit(x,y,1)
        fit_fn = np.poly1d(fit)
        plt.plot(x,y, 'k.', [min(x), max(x)], fit_fn([min(x), max(x)]), '--g')
        plt.xlabel('Age')
        plt.ylabel("Similarity Index")
        plt.title("Ancestors of " + value_key + " vs Age" + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$' )
        f.show()

        f = plt.figure(fig_num)
        fig_num +=1
        x = flat_age_matrix
        y = flat_sim_matrix
        fit = np.polyfit(x,y,1)
        fit_fn = np.poly1d(fit)
        plt.plot(x,y, 'k.', [min(x), max(x)], fit_fn([min(x), max(x)]), '--g', alpha=alpha)
        plt.xlabel('Age')
        plt.ylabel("Similarity Index")
        plt.title("Randomized Ancestors of " + value_key + " vs Age" + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$' )

        f.show()

    f = plt.figure(fig_num)
    fig_num +=1

    d = [x[4] for x in data]
    weights = np.ones_like(d)/float(len(d))
    plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

    weights = np.ones_like(flat_age_matrix)/float(len(flat_age_matrix))
    plt.hist(flat_age_matrix, 100, weights=weights, alpha=0.5, label='Randomized')
    plt.xlabel('Age')
    f.show()
    raw_input()
    plt.close("all")


if __name__ == '__main__':  
    if len(sys.argv[1:]) > 1:
        data_file = sys.argv[1]
        control_file = sys.argv[2]
        null_file = sys.argv[3]

        raw_data, settings = parse_results(data_file)
        raw_control_data, control_settings = parse_results(control_file)

        assert settings == control_settings

        # Null Hypothesis: Correct pairings are no more similar than random
        # Therefore, pairings are exchangeable under the null
        null_data = parse_null(null_file)

        # Filter out studies that have only ever had few or no significant terms
        data = [row for row in raw_data if row.significant_terms > 5 or row.significant_terms_current > 5]
        control_data = [row for row in raw_control_data if row.significant_terms > 5 or row.significant_terms_current > 5]
        all_data = data + control_data

        # Filter out studies that are duplicatesdata[10]

        # Collect some stats
        stats = {}
        genes_found = [genes[1] for row in all_data for genes in row.genes_found]
        genes_missed = [genes for row in all_data for genes in row.genes_missed]
        stats['genes_found'] = len(genes_found)
        stats['genes_missed'] = len(genes_missed)
        stats['unique_genes_found'] = len(set(genes_found))
        stats['unique_genes_missed'] = len(set(genes_missed))
        # exit()

        # age, sim, studies = create_random_null_similarity(data, settings, True)
        # analysis(data, settings, age, sim)
        #
        # log.info("Perfectly Matching Studies in Randomized Data: %s", len([studies[i] for i,a in enumerate(age) if a ==0 and sim[i]==1]))
        # log.info("Perfectly Matching Studies in Randomized Data With same PMID: %s", len([studies[i] for i,a in enumerate(age) if a ==0 and sim[i]==1 and data[studies[i][0]].pmid == data[studies[i][1]].pmid]))
        # f = plt.figure()
        # scatter_plot(age, sim, alpha=0.002)
        # f.show()
        #
        # raw_input()
        # exit()

        actual_m = mean([r.top_parents_jaccard for r in data])
        actual_sd = sstdev([r.top_parents_jaccard for r in data])
        actual_m_tversky = mean([r.top_parents_tversky for r in data])
        actual_sd_tversky = sstdev([r.top_parents_tversky for r in data])

        control_m = mean([r.top_parents_jaccard for r in control_data])
        control_sd = sstdev([r.top_parents_jaccard for r in control_data])
        control_m_tversky = mean([r.top_parents_tversky for r in control_data])
        control_sd_tversky = sstdev([r.top_parents_tversky for r in control_data])

        # Add actual data to permutation test
        # av_sims.append(actual_m)
        # sd_sims.append(actual_sd)
        # all_sims += [r.top_parents_jaccard for r in data]
        # size_sims += [len(r.top_parents_current) for r in data]

        random_m = mean([r.JACCARD for r in null_data])
        random_sd = sstdev([r.JACCARD for r in null_data])
        random_m_tversky = mean([r.TVERSKY for r in null_data])
        random_sd_tversky = sstdev([r.TVERSKY for r in null_data])

        log.info('Actual Jaccard Mean (+- SD): %s (+- %s)', actual_m, actual_sd)
        log.info('Actual Tversky Mean (+- SD): %s (+- %s)', actual_m_tversky, actual_sd_tversky)
        log.info('Actual Age Mean (+- SD): %s (+- %s)', mean([r.age for r in data]), sstdev([r.age for r in data]))
        log.info('')
        log.info('Control Jaccard Mean (+- SD): %s (+- %s)', control_m, control_sd)
        log.info('Control Tversky Mean (+- SD): %s (+- %s)', control_m_tversky, control_sd_tversky)
        log.info('Control Age Mean (+- SD): %s (+- %s)', mean([r.age for r in control_data]), sstdev([r.age for r in control_data]))
        log.info('')
        log.info('Null Jaccard Mean (+- SD): %s (+- %s)', random_m, random_sd)
        log.info('Null Tversky Mean (+- SD): %s (+- %s)', random_m_tversky, random_sd_tversky)
        log.info('')

        # log.info('Null Mean Age: %s', mean(all_ages))
        # log.info('Null Standard Deviation Age: %s', sstdev(all_ages))

        log.info("%% with Jaccard less than Random Pairings: %s%%", 100*len([r for r in data if r.top_parents_jaccard <= random_m ]) / len(data))
        log.info("%% with Tversky less than Random Pairings: %s%%", 100*len([r for r in data if r.top_parents_tversky <= random_m_tversky ]) / len(data))
        log.info('')

        # Compares randomized data means to actual mean
        log.info('Null Sample Means Jaccard')
        f = plt.figure()
        plt.axvline(x=actual_m, color='r', label="Actual")
        plt.axvline(x=control_m, color='b', label="Control")
        histogram([mean([r.JACCARD for r in list(g)]) for k, g in groupby(null_data, lambda r: r[0])], bins=1000, x_label='Mean Jaccard Similarity', title='Null Sample Means Jaccard', alpha=0.5, label='Null Sample Means')
        plt.legend(loc='upper right')
        #plt.xlim(0, actual_m*1.05)
        f.show()

        log.info('Null Sample Means Tversky')
        f = plt.figure()
        plt.axvline(x=actual_m_tversky, color='r', label="Actual")
        plt.axvline(x=control_m_tversky, color='b', label="Control")
        histogram([mean([r.TVERSKY for r in list(g)]) for k, g in groupby(null_data, lambda r: r[0])], bins=1000, x_label='Mean Tversky Similarity', title='Null Sample Means Tversky', alpha=0.5, label='Null Sample Means')
        plt.legend(loc='upper right')
        #plt.xlim(0, actual_m*1.05)
        f.show()

        # Compares Actual histogram of similaries vs "average" randomized histogram
        log.info('Similarity Distribution Jaccard')
        f = plt.figure()
        histogram([r.JACCARD for r in null_data], x_label='Jaccard Similarity', title='Similarity Distribution Jaccard', alpha=0.5, label='Null')
        histogram([r.top_parents_jaccard for r in data], x_label='Jaccard Similarity', title='Similarity Distribution Jaccard', alpha=0.5, label='Actual')
        histogram([r.top_parents_jaccard for r in control_data], x_label='Jaccard Similarity', title='Similarity Distribution Jaccard', alpha=0.5, label='Control')
        plt.legend(loc='upper right')
        f.show()

        log.info('Similarity Distribution Tversky')
        f = plt.figure()
        histogram([r.TVERSKY for r in null_data], x_label='Tversky Similarity', title='Similarity Distribution Tversky', alpha=0.5, label='Null')
        histogram([r.top_parents_tversky for r in data], x_label='Tversky Similarity', title='Similarity Distribution Tversky', alpha=0.5, label='Actual')
        histogram([r.top_parents_tversky for r in control_data], x_label='Tversky Similarity', title='Similarity Distribution Tversky', alpha=0.5, label='Control')
        plt.legend(loc='upper right')
        f.show()

        # # Compares Actual histogram of similaries vs "average" randomized histogram ages
        # log.info('Randomized Age vs Actual Age')
        # f = plt.figure()
        # histogram(all_ages, x_label='Mean Similarity', title='Randomized Age vs Actual Age', alpha=0.5, label='Randomized')
        # histogram([r.age for r in data], x_label='Age (days)', title='Randomized Age vs Actual Age', alpha=0.5, label='Actual')
        # plt.legend(loc='upper right')
        # f.show()
        #
        # # Are similarities correlated with size of variant (new) term set in randomized
        # log.info('Similarity vs (new) term set size in randomized pairings')
        # f = plt.figure()
        # scatter_plot(size_sims, all_sims, x_label='||Ancestors of Top Terms|| of "new"', y_label='Similarity',
        #     title='Similarity vs (new) term set size in randomized pairings', alpha=0.002 )
        # plt.legend(loc='upper right')
        # f.show()
        #
        # # Are similarities correlated with size of variant (new) term set in actual
        # log.info('Similarity vs (new) term set size in actual pairings')
        # f = plt.figure()
        # scatter_plot([len(r.top_parents_current) for r in data], [r.top_parents_jaccard for r in data], x_label='||Ancestors of Top Terms|| of "new"', y_label='Similarity',
        #     title='Similarity vs (new) term set size in actual pairings', alpha=0.2)
        # plt.legend(loc='upper right')
        # f.show()
        #
        # # Are similarities correlated with new mf in actual
        # log.info('Similarity vs new MF in actual pairings')
        # f = plt.figure()
        # scatter_plot([np.log10(r.top_parents_mf_current) for r in data if r.top_parents_mf_current!=0], [r.top_parents_jaccard for r in data if r.top_parents_mf_current!=0], x_label='Log10 New Multifunctionality', y_label='Jaccard Similarity',
        #     title='Similarity vs new MF in actual pairings', alpha=0.15)
        # plt.legend(loc='upper right')
        # f.show()
        #
        # # Are similarities correlated with log2 mf fold change in actual
        # log.info('Similarity vs Log2 MF fold change in actual pairings')
        # f = plt.figure()
        # scatter_plot([np.log2(r.top_parents_mf_current/ r.top_parents_mf) for r in data if r.top_parents_mf!=0 and r.top_parents_mf_current!=0], [r.top_parents_jaccard for r in data if r.top_parents_mf!=0 and r.top_parents_mf_current!=0], x_label='Log2 Fold Change Multifunctionality', y_label='Jaccard Similarity',
        #     title='Similarity vs Log2 MF fold change in actual pairings', alpha=0.15)
        # plt.legend(loc='upper right')
        # f.show()
        #
        # # Similarity vs age
        # log.info('Similarity vs Age')
        # f = plt.figure()
        # scatter_plot([r.age for r in data], [r.top_parents_jaccard for r in data], x_label='Age', y_label='Similarity',
        #     title='Similarity vs Age', alpha=0.5)
        # plt.legend(loc='upper right')
        # f.show()
        #
        # # Binned Violin plots of similarity vs age
        # log.info('Similarity vs Age Violin')
        # f = plt.figure()
        # violinplot_histo([r.age for r in data], [r.top_parents_jaccard for r in data], bins=20, x_label='Age', y_label='Similarity',
        #     title='Similarity vs Age')
        # f.show()

        # Are there any genes that are responsible for a lot of volatility

        raw_input()

#M14590 - M13788

# Null Hypothesis: Correct pairings will maximize average similarity

# Recalculate similarities with random gene knockouts!!!! This might be similar to how MF effect is calculated actually, check ermineJ...