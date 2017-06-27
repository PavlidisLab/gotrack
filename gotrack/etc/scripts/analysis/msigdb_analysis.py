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
import glob
from operator import methodcaller
import sys
import datetime
import csv
import matplotlib.pyplot as plt
import numpy as np
import numpy.random
import random
import itertools
import math

def date_(x):
    return datetime.datetime.strptime(x, "%Y-%m-%d").date()
def list_(x):
    return set(x.split(",")) if x else set()
def list2_(x):
    return map(methodcaller("split", "|"), x.split(",")) if x else []

DATA_HEADER = ["sys_name","name","pmid","species","age","date","edition","edition_date","edition_go","edition_go_date","significant_terms","significant_terms_current","CompleteTermSim","TopTermSim","TopGeneSim","TopParentsSim","top_parents_mf","top_parents_mf_current","top_terms","top_terms_current","top_parents","top_parents_current","top_genes","top_genes_current","genes_found","genes_missed"]
DATA_PARSERS = [str,str, int, int, int, str, int, date_, int, date_, int, int, float, float, float, float, float, float, list_, list_, list_, list_, list_, list_, list2_, list_]
# [0] "sys_name"
# [1] "name"                      "pmid"                      "species"                   "age"                      
# [5] "date"                      "edition"                   "edition_date"              "edition_go"               
# [9] "edition_go_date"           "significant_terms"         "significant_terms_current" "CompleteTermSim"          
#[13] "TopTermSim"                "TopGeneSim"                "TopParentsSim"             "top_parents_mf"           
#[17] "top_parents_mf_current"    "top_terms"                 "top_terms_current"         "top_parents"              
#[21] "top_parents_current"       "top_genes"                 "top_genes_current"         "genes_found"              
#[25] "genes_missed" 

def parse_settings(settings_file):
    with open(settings_file,'rb') as f:
        raw_content = f.readlines()
        content = {}
        for row in raw_content:
            r = map(str.strip, row.split(":",1))
            if r[0] == "current_edition_date":
                r[1] = parse_date(r[1], "%Y-%m-%d")
            else:
                r[1] = eval(r[1])
            content[r[0]] = r[1]
        return content

def parse_results(file):
    # read tab-delimited file
    with open(file,'rb') as infile:
        reader = csv.reader(infile, delimiter='\t')
        next(reader)
        filecontents = [map(lambda x,y:x(y), DATA_PARSERS, line) for line in reader]
    return filecontents

def parse_date(d, format):
    return datetime.datetime.strptime(d, format).date()

def tversky_proto_weighted(prototype, variant):
    if len(prototype) == 0 and len(variant) == 0: return 1.0
    if len(prototype) == 0 or len(variant) == 0: return 0.0
    intersect_size = len(prototype.intersection(variant))
    return intersect_size / (intersect_size + len(prototype.difference(variant)))

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

    >>> median([1, 3, 5])
    3
    >>> median([1, 3, 5, 7])
    4.0

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
    return [item for sublist in l for item in sublist]

def create_random_null_similarity(data, settings, flat=False):
    '''Creates a random null distribution. That is creating a distribution of similarities by 
        comparing pairs of studies at random. The key here would be to decide if matched pairs 
        of studies are more similar than random pairs'''
    # Create Matrix
    age_matrix = []
    sim_matrix = []
    study_matrix = []
    total = len(data) * len(data)
    cnt = 0
    for i, m1 in enumerate(data):
        age_row = []
        sim_row = []
        study_row = []
        for j, m2 in enumerate(data):
            if cnt % 1000000 == 0:
                print "{0} / {1} : {2}".format(cnt, total, cnt/total)

            # Compare m1.old -> m2.new

            age = abs((settings['current_edition_date'] - m1[7]).days)

            age_row.append(age)
            sim_row.append(tversky_proto_weighted(m1[20], m2[21]))
            study_row.append((i,j, 0))

            # Compare m1.old -> m2.old unless m1=m2
            if i!=j and (m1[10] > 5 or m2[10] > 5):
                age = abs((m2[7] - m1[7]).days)

                age_row.append(age)
                sim_row.append(tversky_proto_weighted(m1[20], m2[20]))
                study_row.append((i,j, 1))

            # Compare m1.new -> m2.new
            # age = 0

            # age_row.append(age)
            # sim_row.append(tversky_proto_weighted(m1[21], m2[21]))

            cnt += 1
        if not flat:
            age_matrix.append(age_row)
            sim_matrix.append(sim_row)
            study_matrix.append(study_row)
        else:
            age_matrix += age_row
            sim_matrix += sim_row
            study_matrix += study_row
    return age_matrix, sim_matrix, study_matrix

def random_null_analysis(flat_age_matrix, flat_sim_matrix, bin_by_age=False):
    '''Uses the random null distributions to compute summary statistics.
        bin_by_age: Compute summary statistics for bins of random pairs with similar age differences'''
    if bin_by_age:
        n, bins, patches =  plt.hist(flat_age_matrix, 100,  alpha=0.5)
        bin_indices=np.digitize(flat_age_matrix, bins)
        binned_data = [[] for i in range(len(bins))] 
        print "bins: {0}".format(len(bins))
        for i, ind in enumerate(bin_indices):
            binned_data[ind - 1].append(flat_sim_matrix[i])
        results = []
        for i, bin_data in enumerate(binned_data):
            try:
                results.append({'bin': bins[i],'mean': mean(bin_data), 'sstdev': sstdev(bin_data)})
            except ValueError:
                continue
    else:
        results = {'mean': mean(flat_sim_matrix), 'sstdev': sstdev(flat_sim_matrix)}
    return results

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
    print extent
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
    if not flat_age_matrix or not flat_sim_matrix:
        flat_age_matrix, flat_sim_matrix = create_random_null_similarity(data, settings, flat=True)

    value_keys = ["CompleteTermSim", "TopTermSim", "TopGeneSim", "TopParentsSim"]
    value_indices = [12, 13, 14, 15]
    data_values = [[row[k] for k in value_indices] for row in data]
    means = [mean([x[i] for x in data_values]) for i in range(len(value_keys))]
    stds = [sstdev([x[i] for x in data_values]) for i in range(len(value_keys))]
    medians = [median([x[i] for x in data_values]) for i in range(len(value_keys))]

    print "N: {0}".format(len(data_values))
    genes_found, genes_missed = len([genes[1] for row in data for genes in row[24]]),len([genes for row in data for genes in row[25]])
    print "genes_found: {0}, genes_missed: {1}".format(genes_found, genes_missed)
    total = genes_found + genes_missed
    print "genes_found: {0}, genes_missed: {1}".format(round(genes_found / total, 2), round(genes_missed / total, 2))
    #print "distinct unknown genes: {0}".format(len(unknown))
    fig_num = 0
    for i, k in enumerate([15]):
        v_key = value_keys[i]
        print "{0}: mean={1}, std={2}, median={3}".format(v_key, means[i], stds[i], medians[i])

        f = plt.figure(fig_num)
        fig_num +=1
        d = [x[i] for x in data_values]
        weights = np.ones_like(d)/float(len(d))
        plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

        weights = np.ones_like(flat_sim_matrix)/float(len(flat_sim_matrix))
        plt.hist(flat_sim_matrix, 100, weights=weights, alpha=0.5, label='Randomized')

        plt.xlabel('Similarity')
        plt.ylabel(value_keys[i])
        plt.title('Histogram of ' + v_key + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$')
        plt.legend(loc='upper right')
        f.show()

        f = plt.figure(fig_num)
        fig_num +=1
        x = [r[4] for r in data]
        y = [r[k] for r in data]
        fit = np.polyfit(x,y,1)
        fit_fn = np.poly1d(fit)
        plt.plot(x,y, 'k.', [min(x), max(x)], fit_fn([min(x), max(x)]), '--g')
        plt.xlabel('Age')
        plt.ylabel("Similarity Index")
        plt.title("Ancestors of Top Terms vs Age" + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$' )
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
        plt.title("Randomized Ancestors of Top Terms vs Age" + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$' )

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

def permutation_test(data):
    average_sim = []
    n = len(data)

    # Memoize similarities
    sims = np.zeros((n,n))
    for i,j in itertools.product(range(n), repeat=2):
        sims[i,j] = tversky_proto_weighted(data[i][20], data[j][21])
    
    matched_pairs_permutations = (list(zip(range(n), p)) for p in itertools.permutations(range(n)))

    cnt = 0
    total = math.factorial(n)
    for pairings in matched_pairs_permutations:
        if cnt % 1000000 == 0:
            print "{0} %".format(cnt/total)
        s = 0
        for m1, m2 in pairings:
            s += sims[m1,m2]
        average_sim.append(s/n)
        cnt += 1
    return average_sim

def mc_permutation_test(data, samples=1000):
    average_sims = []
    sstdev_sims = []
    all_sims = []
    set_sizes = []
    n = len(data)

    # Memoize similarities
    sims = np.zeros((n,n))
    for i,j in itertools.product(range(n), repeat=2):
        sims[i,j] = tversky_proto_weighted(data[i][20], data[j][21])

    sizes = np.zeros(n)
    for i in xrange(n):
        sizes[i] = len(data[i][21])

    print "Sim Matrix Created"

    cnt = 0
    for _ in xrange(samples):
        if cnt % 10000 == 0:
            print "{0} %".format(100*cnt/samples)

        random_pairings = np.random.permutation(n)
        sim_data = [sims[m1,m2] for m1, m2 in enumerate(random_pairings)]
        average_sims.append(mean(sim_data))
        sstdev_sims.append(sstdev(sim_data))
        all_sims += sim_data
        set_sizes += [sizes[m2] for m2 in random_pairings]
        #f = histogram(sim_data, x_label='Mean Tversky Similarity', title='MC Sampled Permutation Test (N='+str(1)+')', alpha=0.5, label='Actual')
        #plt.legend(loc='upper right')
        #f.show()
        #raw_input()
        #plt.close("all")

        cnt += 1
    print "{0} %".format(100*cnt/samples)
    return average_sims, sstdev_sims, all_sims, set_sizes


if __name__ == '__main__':  
    if len(sys.argv[1:]) > 1:
        data_file = sys.argv[1]
        settings_file = sys.argv[2]
        data = parse_results(data_file)
        settings = parse_settings(settings_file)

        # Filter out studies that have only ever had few or no significant terms
        data = [row for row in data if row[10] > 5 or row[11] > 5]

        # Filter out studies that are duplicatesdata[10]

        # Collect some stats
        stats = {}
        genes_found = [genes[1] for row in data for genes in row[24]]
        genes_missed = [genes for row in data for genes in row[25]]
        stats['genes_found'] = len(genes_found)
        stats['genes_missed'] = len(genes_missed)
        stats['unique_genes_found'] = len(set(genes_found))
        stats['unique_genes_missed'] = len(set(genes_missed))

        #age, sim, studies = create_random_null_similarity(data, settings, True)
        #analysis(data, settings, age, sim)

        #print "Perfectly Matching Studies in Randomized Data: {0}".format(len([studies[i] for i,a in enumerate(age) if a ==0 and sim[i]==1]))
        #print "Perfectly Matching Studies in Randomized Data With same PMID: {0}".format(len([studies[i] for i,a in enumerate(age) if a ==0 and sim[i]==1 and data[studies[i][0]][2] == data[studies[i][1]][2]]))
        #f = scatter_plot(age, sim, alpha=0.002)
        #f.show()


        # Null Hypothesis: Correct pairings are no more similar than random
        # Therefore, pairings are exchangeable under the null
        samples = 10000
        av_sims, sd_sims, all_sims, size_sims = mc_permutation_test(data, samples=samples)
        actual_m = mean([r[15] for r in data])
        actual_sd = sstdev([r[15] for r in data])

        # Add actual data to permutation test
        av_sims.append(actual_m)
        sd_sims.append(actual_sd)
        all_sims += [r[15] for r in data]
        size_sims += [len(r[21]) for r in data]

        random_m = mean(av_sims)
        random_sd = math.sqrt(mean([x*x for x in sd_sims ]))

        print 'actual_m: {0}'.format(actual_m)
        print 'actual_sd: {0}'.format(actual_sd)
        print 'random_m: {0}'.format(random_m)
        print 'random_sd: {0}'.format(random_sd)

        print "Percentage of studies less than Random Pairings: {0}".format(len([r for r in data if r[15] <= random_m ]) / len(data))

        # Compares randomized data means to actual mean
        f = plt.figure()
        plt.axvline(x=actual_m, color='r')
        histogram(av_sims, bins=1000, x_label='Mean Tversky Similarity', title='Permutation Test Means vs Actual', alpha=0.5, label='PT Means')
        plt.legend(loc='upper right')
        #plt.xlim(0, actual_m*1.05)
        f.show()

        # Compares Actual histogram of similaries vs "average" randomized histogram
        f = plt.figure()
        histogram(all_sims, x_label='Mean Tversky Similarity', title='Randomized vs Actual', alpha=0.5, label='Randomized')
        histogram([r[15] for r in data], x_label='Mean Tversky Similarity', title='Randomized vs Actual', alpha=0.5, label='Actual')
        plt.legend(loc='upper right')
        f.show()

        # Are similarities correlated with size of variant (new) term set in randomized
        f = plt.figure()
        scatter_plot(size_sims, all_sims, x_label='||Ancestors of Top Terms|| of "new"', y_label='Tversky Similarity',
            title='Similarity vs (new) term set size in randomized pairings', alpha=0.002 )
        plt.legend(loc='upper right')
        f.show()

        # Are similarities correlated with size of variant (new) term set in actual
        f = plt.figure()
        scatter_plot([len(r[21]) for r in data], [r[15] for r in data], x_label='||Ancestors of Top Terms|| of "new"', y_label='Tversky Similarity',
            title='Similarity vs (new) term set size in actual pairings', alpha=0.2)
        plt.legend(loc='upper right')
        f.show()

        # Are similarities correlated with new mf in actual
        f = plt.figure()
        scatter_plot([np.log10(r[17]) for r in data if r[17]!=0], [r[15] for r in data if r[17]!=0], x_label='Log10 New Multifunctionality', y_label='Tversky Similarity',
            title='Similarity vs new MF in actual pairings', alpha=0.15)
        plt.legend(loc='upper right')
        f.show()

        # Are similarities correlated with log2 mf fold change in actual
        f = plt.figure()
        scatter_plot([np.log2(r[17]/ r[16]) for r in data if r[16]!=0 and r[17]!=0], [r[15] for r in data if r[16]!=0 and r[17]!=0], x_label='Log2 Fold Change Multifunctionality', y_label='Tversky Similarity',
            title='Similarity vs Log2 MF fold change in actual pairings', alpha=0.15)
        plt.legend(loc='upper right')
        f.show()

        # Similarity vs age
        f = plt.figure()
        scatter_plot([r[4] for r in data], [r[15] for r in data], x_label='Age', y_label='Tversky Similarity',
            title='Similarity vs Age', alpha=0.5)
        plt.legend(loc='upper right')
        f.show()

        # Binned Violin plots of similarity vs age
        f = plt.figure()
        violinplot_histo([r[4] for r in data], [r[15] for r in data], bins=20, x_label='Age', y_label='Tversky Similarity',
            title='Similarity vs Age')
        f.show()

        # Are there any genes that are responsible for a lot of volatility



#M14590 - M13788

# Null Hypothesis: Correct pairings will maximize average similarity

# Recalculate similarities with random gene knockouts!!!! This might be similar to how MF effect is calculated actually, check ermineJ...