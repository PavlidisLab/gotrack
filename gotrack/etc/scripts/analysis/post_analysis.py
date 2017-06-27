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

"""Creates post-analysis graphics."""

from __future__ import division
import glob
import csv
import json
from operator import methodcaller
import sys
import matplotlib.pyplot as plt
import numpy as np
import datetime

def parseFile(raw_content):
  content = {}
  for row in raw_content:
    r = map(str.strip, row.split(":",1))
    if r[0] == "name":
        pass
    elif r[0] == "sys_name":
        pass
    elif r[0] == "pmid":
        r[1] = int(r[1])
    elif r[0] == "species":
        r[1] = int(r[1])
    elif r[0] == "date":
        pass
    elif r[0] == "edition":
        r[1] = eval(r[1])
    elif r[0] == "exact":
        r[1] = map(methodcaller("split", "|"), r[1].split(",")) if r[1] else []
    elif r[0] == "exact_synonym":
        r[1] = map(methodcaller("split", "|"), r[1].split(",")) if r[1] else []
    elif r[0] == "unknown":
        r[1] = r[1].split(",") if r[1] else []
    elif r[0] == "age":
        r[1] = int(r[1])
    elif r[0] == "significant_terms":
        r[1] = int(r[1])
    elif r[0] == "significant_terms_current":
        r[1] = int(r[1])
    elif r[0] == "TopGeneSim":
        r[1] = float(r[1])
    elif r[0] == "TopParentsSim":
        r[1] = float(r[1])
    elif r[0] == "CompleteTermSim":
        r[1] = float(r[1])
    elif r[0] == "TopTermSim":
        r[1] = float(r[1])
    elif r[0] == "top_terms":
        r[1] = set(r[1].split(",")) if r[1] else []
    elif r[0] == "top_terms_current":
        r[1] = set(r[1].split(",")) if r[1] else []
    elif r[0] == "top_parents":
        r[1] = set(r[1].split(",")) if r[1] else []
    elif r[0] == "top_parents_current":
        r[1] = set(r[1].split(",")) if r[1] else []
    elif r[0] == "top_genes":
        r[1] = set(map(int, r[1].split(","))) if r[1] else []
    elif r[0] == "top_genes_current":
        r[1] = set(map(int, r[1].split(","))) if r[1] else []
    content[r[0]] = r[1]
  return content

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


if __name__ == '__main__':  
    if len(sys.argv[1:]) > 1:
        input_data_folder = sys.argv[1]
        age_matrix_file = sys.argv[2]
        sim_matrix_file = sys.argv[3]
        out_file = sys.argv[4]
        data = []
        genes = {'exact':0, 'exact_synonym':0, 'unknown':0}
        unknown = set()
        value_keys = ["TopGeneSim", "TopParentsSim", "CompleteTermSim", "TopTermSim"]
        all_content = []
        filtered_content = []
        for filename in glob.iglob(input_data_folder + '/M*'):
            try:
                with open(filename,'rb') as f:
                    raw_content = f.readlines()
                    content = parseFile(raw_content)

                    # collect data
                    all_content.append(content)
                    if content['significant_terms'] != 0 or content['significant_terms_current'] != 0:
                        filtered_content.append(content)
                        data.append([content[k] for k in value_keys])
                        for k in genes:
                            genes[k] += len(content[k])
                        unknown.update(zip([content['species']]*len( content['unknown']), content['unknown']))

            except IOError, e:
                print e

        flat_sim_matrix = []
        with open(sim_matrix_file, 'rb') as f:
            tsv = csv.reader(f, delimiter=',')
            for l in tsv:
                flat_sim_matrix.append(map(float, l))

        flat_sim_matrix = [item for sublist in flat_sim_matrix for item in sublist]
        #print "Randomized Similarity: mean={1}, std={2}, median={3}".format(k, mean(flat_sim_matrix), sstdev(flat_sim_matrix), median(flat_sim_matrix))
        print "sim data loaded"
        flat_age_matrix = []
        with open(age_matrix_file, 'rb') as f:
            tsv = csv.reader(f, delimiter=',')
            for l in tsv:
                flat_age_matrix.append(map(float, l))

        flat_age_matrix = np.array([item for sublist in flat_age_matrix for item in sublist])
        #print "Randomized Age: mean={1}, std={2}, median={3}, min={4}, max={5}".format(k, mean(flat_age_matrix), sstdev(flat_age_matrix), median(flat_age_matrix), min(flat_age_matrix), max(flat_age_matrix))
        print "age data loaded"
        n, bins, patches =  plt.hist(flat_age_matrix, 100,  alpha=0.5)
        bin_indices=np.digitize(flat_age_matrix, bins)
        binned_data = [[] for i in range(len(bins))] 
        print "bins: {0}".format(len(bins))
        for i, ind in enumerate(bin_indices):
            binned_data[ind - 1].append(flat_sim_matrix[i])

        with open(out_file, 'w+') as f_out:
            for i, bin_data in enumerate(binned_data):
                try:
                    f_out.write(",".join(map(str, [bins[i], mean(bin_data), sstdev(bin_data)])) + "\n")
                except ValueError:
                    continue
 

        means = [mean([x[i] for x in data]) for i in range(len(value_keys))]
        stds = [sstdev([x[i] for x in data]) for i in range(len(value_keys))]
        medians = [median([x[i] for x in data]) for i in range(len(value_keys))]

        unknown = list(unknown)

        print "N: {0}".format(len(data))
        print "genes: {0}".format(genes)
        total = sum([genes[k] for k in genes])
        print "genes: {0}".format({k: round(x / total, 2) for k, x in genes.iteritems()})
        print "distinct unknown genes: {0}".format(len(unknown))
        fig_num = 0
        for i, k in enumerate(["TopParentsSim"]):
            print "{0}: mean={1}, std={2}, median={3}".format(k, means[i], stds[i], medians[i])

            f = plt.figure(fig_num)
            fig_num +=1
            d = [x[i] for x in data]
            weights = np.ones_like(d)/float(len(d))
            plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

            weights = np.ones_like(flat_sim_matrix)/float(len(flat_sim_matrix))
            plt.hist(flat_sim_matrix, 100, weights=weights, alpha=0.5, label='Randomized')

            plt.xlabel('Similarity')
            plt.ylabel(value_keys[i])
            plt.title('Histogram of ' + k + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$')
            plt.legend(loc='upper right')
            f.show()

            f = plt.figure(fig_num)
            fig_num +=1
            x = [r['age'] for r in filtered_content]
            y = [r[k] for r in filtered_content]
            fit = np.polyfit(x,y,1)
            fit_fn = np.poly1d(fit)
            plt.plot(x,y, 'k.', x, fit_fn(x), '--g')
            plt.xlabel('Age')
            plt.ylabel("Similarity Index")
            plt.title("Ancestors of Top Terms vs Age" + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$' )

            f.show()

        f = plt.figure(fig_num)
        fig_num +=1

        d = [x['age'] for x in filtered_content]
        weights = np.ones_like(d)/float(len(d))
        plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

        weights = np.ones_like(flat_age_matrix)/float(len(flat_age_matrix))
        plt.hist(flat_age_matrix, 100, weights=weights, alpha=0.5, label='Randomized')
        plt.xlabel('Age')
        f.show()
        raw_input()