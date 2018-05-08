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

"""(DEPRECATED) Creates post-analysis graphics."""

from __future__ import division
from operator import methodcaller
import glob
import matplotlib.pyplot as plt
import numpy as np
import sys
from collections import defaultdict
import operator
import scipy.stats as stats

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

def correct_pvalues_for_multiple_testing(pvalues, correction_type = "Benjamini-Hochberg"):                
    """                                                                                                   
    consistent with R - print correct_pvalues_for_multiple_testing([0.0, 0.01, 0.029, 0.03, 0.031, 0.05, 0.069, 0.07, 0.071, 0.09, 0.1]) 
    """
    from numpy import array, empty                                                                        
    pvalues = array(pvalues) 
    n = float(pvalues.shape[0])                                                                           
    new_pvalues = empty(n)
    if correction_type == "Bonferroni":                                                                   
        new_pvalues = n * pvalues
    elif correction_type == "Bonferroni-Holm":                                                            
        values = [ (pvalue, i) for i, pvalue in enumerate(pvalues) ]                                      
        values.sort()
        for rank, vals in enumerate(values):                                                              
            pvalue, i = vals
            new_pvalues[i] = (n-rank) * pvalue                                                            
    elif correction_type == "Benjamini-Hochberg":                                                         
        values = [ (pvalue, i) for i, pvalue in enumerate(pvalues) ]                                      
        values.sort()
        values.reverse()                                                                                  
        new_values = []
        for i, vals in enumerate(values):                                                                 
            rank = n - i
            pvalue, index = vals                                                                          
            new_values.append((n/rank) * pvalue)                                                          
        for i in xrange(0, int(n)-1):  
            if new_values[i] < new_values[i+1]:                                                           
                new_values[i+1] = new_values[i]                                                           
        for i, vals in enumerate(values):
            pvalue, index = vals
            new_pvalues[index] = new_values[i]                                                                                                                  
    return new_pvalues

if __name__ == '__main__':  
    if len(sys.argv[1:]) > 0:
        input_data_folder = sys.argv[1]
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

        unknown = list(unknown)

        print "N: {0}".format(len(data))
        print "genes: {0}".format(genes)
        total = sum([genes[k] for k in genes])
        print "genes: {0}".format({k: round(x / total, 2) for k, x in genes.iteritems()})
        print "distinct unknown genes: {0}".format(len(unknown))

        # Are there genes that are especially "responsible" for volatility across the C2 lists?
        # We will try a few methods:

        # 1) Average similarity values for all genes in hit list
        # 2) Average similarity values for top genes
        # 3) Average similarity values for curent top genes
        sim_avgs = {'all_genes':defaultdict(list), 'top_genes':defaultdict(list), 'top_genes_current':defaultdict(list)}
        sim_term_avgs = {'top_terms':defaultdict(list), 'top_terms_current':defaultdict(list)}
        study_counts = defaultdict(lambda : [0,0])
        study_term_counts = defaultdict(lambda : [0,0])
        for study in filtered_content:
            # Genes
            all_genes = [g[1] for k in ['exact','exact_synonym'] for g in study[k]]
            sim_val = study['TopParentsSim']
            for g in all_genes:
                sim_avgs['all_genes'][g].append(sim_val)
                g_cnts = study_counts[g]
                g_cnts[1]+=1
                if sim_val <= 0.3:
                    g_cnts[0]+=1

            for g in study['top_genes']:
                sim_avgs['top_genes'][g].append(sim_val)


            for g in study['top_genes_current']:
                sim_avgs['top_genes_current'][g].append(sim_val)

            # Terms
            for t in study['top_terms']:
                sim_term_avgs['top_terms'][t].append(sim_val)
                t_cnts = study_term_counts[t]
                t_cnts[1]+=1
                if sim_val <= 0.3:
                    t_cnts[0]+=1

            for t in study['top_terms_current']:
                sim_term_avgs['top_terms_current'][t].append(sim_val)

        print "Histograms of distribution of average similarities of genes"
        fig_num = 0
        for k, gene_list in sim_avgs.iteritems():
            gene_vals = gene_list.itervalues()
            f = plt.figure(fig_num)
            fig_num +=1
            d = [mean(gv) for gv in gene_vals]
            weights = np.ones_like(d)/float(len(d))
            plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

            plt.xlabel('Similarity')
            #plt.ylabel('')
            plt.title('Histogram of ' + k + " | " + r'$\mu=' + str(round(mean(d), 2)) + r',\ \sigma=' + str(round(sstdev(d), 2)) + r'$')
            plt.legend(loc='upper right')
            f.show()


        raw_input()
        plt.close("all")
        # print ""
        # res = {k:mean(v) for k,v in sim_avgs['all_genes'].iteritems()}
        # sorted_res = sorted(res.items(), key=operator.itemgetter(1))
        # zero_genes = [x for x in sorted_res if x[1]==0]
        # [g for g in zero_genes if len(sim_avgs['all_genes'][g[0]]) > 3]

        # Hypergeometric Test
        # Total Number of 'Bad' Studies with Gene A
        # Total Number of Studies with Gene A
        # Total Number of 'Bad' Studies without Gene A
        # Total Number of Studies without Gene A

        total_studies = len(filtered_content)
        total_bad_studies = len([s for s in filtered_content if s['TopParentsSim'] <= 0.3])
        pvals = []
        tests = 0
        test_min=5
        for g in sim_avgs['all_genes'].iterkeys():
            g_cnts = study_counts[g]
            if g_cnts[1] >= test_min:
                tests+=1
                pvals.append([g, stats.hypergeom.sf(g_cnts[0], total_studies, g_cnts[1], total_bad_studies), g_cnts])
        print "Tests: {0}".format(len(pvals))   
        uz = zip(*pvals)

        # Check P-Value distribution
        fig_num = 0
        f = plt.figure(fig_num)
        d = uz[1]
        weights = np.ones_like(d)/float(len(d))
        plt.hist(d, 1000, weights=weights, alpha=0.5, label='Actual')
        plt.xlabel('P-Values')
        plt.ylabel('Count')
        plt.title('Histogram of P-Values for gene OR | ' + r'$\mu=' + str(round(mean(d), 2)) + r',\ \sigma=' + str(round(sstdev(d), 2)) + r'$')
        f.show()

        #uz[1] = correct_pvalues_for_multiple_testing(uz[1])
        corr = zip(*uz)
        res = [p for p in corr if p[1] < 0.05]
        print "BH: {0}".format(len(res))  


        # [p for p in pvals if p[2][0] > 100]
        # [[s['sys_name'], s['name'], s['pmid'], s['date'], s['TopParentsSim']] for s in filtered_content if 'OSBPL7' in [g[1] for k in ['exact','exact_synonym'] for g in s[k]]]

        # any ones with newer dates?
        # newer = [min([s['age'] for s in filtered_content if n in [g[1] for k in ['exact','exact_synonym'] for g in s[k]]]) for n in [y[0] for y in corr]]
        raw_input()

        print "Histograms of distribution of average similarities of top original terms"
        fig_num = 0
        for k, term_list in sim_term_avgs.iteritems():
            term_vals = term_list.itervalues()
            f = plt.figure(fig_num)
            fig_num +=1
            d = [mean(tval) for tval in term_vals]
            weights = np.ones_like(d)/float(len(d))
            plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')

            plt.xlabel('Similarity')
            #plt.ylabel('')
            plt.title('Histogram of ' + k + " | " + r'$\mu=' + str(round(mean(d), 2)) + r',\ \sigma=' + str(round(sstdev(d), 2)) + r'$')
            plt.legend(loc='upper right')
            f.show()


        raw_input()
        plt.close("all")

        # Hypergeometric Test
        # Total Number of 'Bad' Studies with Term A
        # Total Number of Studies with Term A
        # Total Number of 'Bad' Studies without Term A
        # Total Number of Studies without Term A

        #total_studies = len(filtered_content)
        #total_bad_studies = len([s for s in filtered_content if s['TopParentsSim'] <= 0.3])
        term_pvals = []
        term_tests = 0
        term_test_min=10
        for t in sim_term_avgs['top_terms'].iterkeys():
            t_cnts = study_term_counts[t]
            if t_cnts[1] >= term_test_min:
                term_tests+=1
                term_pvals.append([t, stats.hypergeom.sf(t_cnts[0], total_studies, t_cnts[1], total_bad_studies), t_cnts])
        print "Term Tests: {0}".format(len(term_pvals))   
        t_uz = zip(*term_pvals)

        # Check P-Value distribution
        fig_num = 0
        f = plt.figure(fig_num)
        d = t_uz[1]
        weights = np.ones_like(d)/float(len(d))
        plt.hist(d, 100, weights=weights, alpha=0.5, label='Actual')
        plt.xlabel('P-Values')
        plt.ylabel('Count')
        plt.title('Histogram of P-Values for term OR | ' + r'$\mu=' + str(round(mean(d), 2)) + r',\ \sigma=' + str(round(sstdev(d), 2)) + r'$')
        f.show()

        t_uz[1] = correct_pvalues_for_multiple_testing(t_uz[1])
        t_corr = zip(*t_uz)
        t_res = [p for p in t_corr if p[1] < 0.05]
        print "Term BH: {0}".format(len(t_res))  

