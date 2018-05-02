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
import numpy as np
import itertools
import math
from collections import namedtuple

import logging
import logging.config

# Example logging
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()


class memoized:

    def __init__(self, f):
        self.f = f
        self.cache = {}

    def __call__(self, *args):
        if args in self.cache:
            return self.cache[args]
        else:
            value = self.f(*args)
            self.cache[args] = value
            return value

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

GeneSetAnalysis = namedtuple('GeneSetAnalysis', [h[0] for h in DATA_HEADER])

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
        filecontents = [GeneSetAnalysis(*map(lambda x,y:x(y), type_parser, line)) for line in reader]

    settings['reference_edition_date'] = parse_date( settings['reference_edition_date'], '%Y-%m-%d')
    settings['reference_edition_go_date'] = parse_date( settings['reference_edition_go_date'], '%Y-%m-%d')
    return filecontents, settings

def parse_date(d, format):
    return datetime.datetime.strptime(d, format).date()

def tversky_proto_weighted(prototype, variant):
    if len(prototype) == 0 and len(variant) == 0: return 1.0
    if len(prototype) == 0 or len(variant) == 0: return 0.0
    intersect_size = len(prototype.intersection(variant))
    return intersect_size / (intersect_size + len(prototype.difference(variant)))

def jaccard_similarity(a, b):
    if len(a) == 0 and len(b) == 0: return 1.0
    if len(a) == 0 or len(b) == 0: return 0.0
    return len(a.intersection(b)) / len(a.union(b))

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

def flatten(l):
    return (item for sublist in l for item in sublist)

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
                logging.info("%s / %s : %s", cnt, total, cnt/total)

            # Compare m1.old -> m2.new

            age = abs((settings['reference_edition_date'] - m1.edition_date).days)

            age_row.append(age)
            sim_row.append(jaccard_similarity(m1.top_parents, m2.top_parents_current))
            study_row.append((i,j, 0))

            # Compare m1.old -> m2.old unless m1=m2
            if i!=j and (m1.significant_terms > 5 or m2.significant_terms > 5):
                age = abs((m2.edition_date - m1.edition_date).days)

                age_row.append(age)
                sim_row.append(jaccard_similarity(m1.top_parents, m2.top_parents))
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


def permutation_test(data):
    average_sim = []
    n = len(data)

    # Memoize similarities
    sims = np.zeros((n,n))
    for i, j in itertools.product(range(n), repeat=2):
        sims[i, j] = jaccard_similarity(data[i].top_parents, data[j].top_parents_current)

    matched_pairs_permutations = (list(zip(range(n), p)) for p in itertools.permutations(range(n)))

    cnt = 0
    total = math.factorial(n)
    for pairings in matched_pairs_permutations:
        if cnt % 1000000 == 0:
            logging.info("%s%%", cnt/total)
        s = 0
        for m1, m2 in pairings:
            s += sims[m1,m2]
        average_sim.append(s/n)
        cnt += 1
    return average_sim


def mc_permutation_test(data, samples=1000):
    sample_permutations = []
    n = len(data)


    @memoized
    def sims(i, j):
        return jaccard_similarity(data[i].top_parents, data[j].top_parents_current), tversky_proto_weighted(data[i].top_parents, data[j].top_parents_current)

    cnt = 0
    for _ in xrange(samples):
        if cnt % 100 == 0:
            logging.info("%s%%", 100*cnt/samples)
        sample_permutations.append([(sims(m1,m2), m1, m2) for m1, m2 in enumerate(np.random.permutation(n))])


        cnt += 1
    logging.info("%s%%", 100*cnt/samples)
    return sample_permutations

def is_up_down_pair(data, i, j):
    m1 = data[i].name
    m2 = data[j].name
    if m1.endswith('_UP') or m1.endswith('_DN'):
        m1 = m1[:-3]
    if m2.endswith('_UP') or m2.endswith('_DN'):
        m2 = m2[:-3]
    return m1 == m2

if __name__ == '__main__':
    if len(sys.argv[1:]) > 1:
        data_file = sys.argv[1]
        null_out = sys.argv[2]

        if len(sys.argv[1:]) > 2:
            samples = int(sys.argv[3])
        else:
            samples = 1000

        if len(sys.argv[1:]) > 3:
            term_filter_or = sys.argv[4].lower() == 'true'
        else:
            term_filter_or = False

        if len(sys.argv[1:]) > 4:
            control_file = sys.argv[5]
        else:
            control_file = False

        raw_data, settings = parse_results(data_file)

        # Filter out studies that have only ever had few or no significant terms
        if term_filter_or:
            def term_filter(r):
                return r.significant_terms > 5 or r.significant_terms_current > 5
        else:
            def term_filter(r):
                return r.significant_terms > 5 and r.significant_terms_current > 5

        data = [row for row in raw_data if term_filter(row)]

        sim = lambda r: r.top_parents_jaccard

        log.info('Actual Mean: %s', mean([sim(r) for r in data]))
        log.info('Actual Standard Deviation: %s', sstdev([sim(r) for r in data]))
        log.info('Actual Mean Age: %s', mean([r.age for r in data]))
        log.info('Actual Standard Deviation Age: %s', sstdev([r.age for r in data]))

        if control_file:
            raw_control_data, control_settings = parse_results(control_file)
            assert settings == control_settings
            control_data = [row for row in raw_control_data if term_filter(row)]

            log.info('Control Mean: %s', mean([sim(r) for r in control_data]))
            log.info('Control Standard Deviation: %s', sstdev([sim(r) for r in control_data]))
            log.info('Control Mean Age: %s', mean([r.age for r in control_data]))
            log.info('Control Standard Deviation Age: %s', sstdev([r.age for r in control_data]))

            all_data = data + control_data
        else:
            all_data = data

        # Null Hypothesis: Correct pairings are no more similar than random
        # Therefore, pairings are exchangeable under the null

        log.info("Creating null distribution with %s samples", samples)
        null_perms = mc_permutation_test(all_data, samples=samples)

        log.info("Null Size before pruning: %s", sum(len(x) for x in null_perms))

        # Make sure we don't compare any _UP to _DN of the same name
        # is_up_down_pair = lambda x,y,z: False
        null_perms = [[s for s in p if not is_up_down_pair(all_data, s[1], s[2])] for p in null_perms]

        log.info("Null Size after pruning: %s", sum(len(x) for x in null_perms))

        log.info('Null Mean: %s', mean([mean([s[0][0] for s in p]) for p in null_perms]))
        log.info('Null Standard Deviation: %s', sstdev([sstdev([s[0][0] for s in p]) for p in null_perms]))

        log.info("Writing null distribution to %s.tsv", null_out )
        with open(null_out + ".tsv", 'w+') as f_out:
            # Write null ditribution
            f_out.write("\t".join(["PERMUTATION_RUN", "SYSTEMATIC_NAME_THEN", "SYSTEMATIC_NAME_NOW", "THEN_DATE", "JACCARD", "TVERSKY"]) + "\n")
            cnt = 0
            total = len(null_perms)
            for i, p in enumerate(null_perms):
                if cnt % 100 == 0:
                    log.info("%s%%", 100*cnt/total)
                cnt += 1
                for sims, m1, m2 in p:
                    try:
                        f_out.write("\t".join(map(str, [i, all_data[m1].sys_name, all_data[m2].sys_name, all_data[m2].edition_date.strftime("%Y-%m-%d"), sims[0], sims[1]])) + "\n")
                    except Exception as e:
                        log.error("%s,%s: %s", m1, m2, repr(e) )
            log.info("%s%%", 100*cnt/samples)
