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

"""Creates post-analysis tables and charts."""

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
    elif r[0] == "TopGeneJaccard":
        r[1] = float(r[1])
    elif r[0] == "TopParentsJaccard":
        r[1] = float(r[1])
    elif r[0] == "CompleteTermJaccard":
        r[1] = float(r[1])
    elif r[0] == "TopTermJaccard":
        r[1] = float(r[1])
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
        input_data = sys.argv[1]
        out_file = sys.argv[2]
        data = []
        genes = {'exact':0, 'exact_synonym':0, 'unknown':0}
        unknown = set()
        value_keys = ["TopGeneJaccard", "TopParentsJaccard", "CompleteTermJaccard", "TopTermJaccard"]
        all_content = []
        with open(out_file, 'w+') as f_out:
            f_out.write("sys_name\tname\tage(days)\tgenes_found\tgenes_missed\tsig_terms\tsig_terms_current\tTopGeneJaccard\tTopParentsJaccard\tCompleteTermJaccard\tTopTermJaccard\n")
            for filename in glob.iglob(input_data + '/M*'):
                try:
                    with open(filename,'rb') as f:
                        raw_content = f.readlines()
                        content = parseFile(raw_content)
                        # Write table
                        f_out.write("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n".format( content['sys_name'], content['name'], content['age'],
                             len(content["exact"]) + len(content["exact_synonym"]), len(content["unknown"]), content['significant_terms'], content['significant_terms_current'],
                             content["TopGeneJaccard"], content["TopParentsJaccard"], content["CompleteTermJaccard"], content["TopTermJaccard"]))
                        # collect data
                        all_content.append(content)
                        if content['significant_terms'] != 0 or content['significant_terms_current'] != 0:
                            data.append([content[k] for k in value_keys])
                            for k in genes:
                                genes[k] += len(content[k])
                            unknown.update(zip([content['species']]*len( content['unknown']), content['unknown']))

                except IOError, e:
                    print e

        means = [mean([x[i] for x in data]) for i in range(len(value_keys))]
        stds = [sstdev([x[i] for x in data]) for i in range(len(value_keys))]
        medians = [median([x[i] for x in data]) for i in range(len(value_keys))]

        unknown = list(unknown)

        print "N: {0}".format(len(data))
        print "genes: {0}".format(genes)
        total = sum([genes[k] for k in genes])
        print "genes: {0}".format({k: round(x / total, 2) for k, x in genes.iteritems()})
        print "distinct unknown genes: {0}".format(len(unknown))

        for i, k in enumerate(value_keys):
            print "{0}: mean={1}, std={2}, median={3}".format(k, means[i], stds[i], medians[i])

            f = plt.figure(2*i+1)
            d = [x[i] for x in data]
            weights = np.ones_like(d)/float(len(d))
            n, bins, patches = plt.hist(d, 100, weights=weights, facecolor='g', alpha=0.75)
            plt.xlabel('Similarity')
            plt.ylabel(value_keys[i])
            plt.title('Histogram of ' + value_keys[i] + " | " + r'$\mu=' + str(round(means[i], 2)) + r',\ \sigma=' + str(round(stds[i], 2)) + r'$')

            f.show()

            f = plt.figure(2*i+2)
            x = [r['age'] for r in all_content if r['significant_terms'] != 0 or r['significant_terms_current'] != 0 ]
            y = [r[k] for r in all_content if r['significant_terms'] != 0 or r['significant_terms_current'] != 0]
            fit = np.polyfit(x,y,1)
            fit_fn = np.poly1d(fit)
            plt.plot(x,y, 'yo', x, fit_fn(x), '--k')
            plt.xlabel('Age')
            plt.ylabel(value_keys[i])
            plt.title(value_keys[i] + " vs Age" )

            f.show()
        raw_input()
