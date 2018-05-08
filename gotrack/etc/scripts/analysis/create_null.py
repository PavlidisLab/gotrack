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

import itertools
import logging
import logging.config
import math
import sys

import numpy as np

import parser
from util import mean, sstdev, tversky_proto_weighted, jaccard_similarity, Memoized

# Example logging
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()


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
                log.info("%s / %s : %s", cnt, total, cnt/total)

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
            log.info("%s%%", cnt/total)
        s = 0
        for m1, m2 in pairings:
            s += sims[m1,m2]
        average_sim.append(s/n)
        cnt += 1
    return average_sim


def mc_permutation_test(data, samples=1000):
    sample_permutations = []
    n = len(data)

    @Memoized
    def sims(i, j):
        now = data[j]
        then = data[i]
        return jaccard_similarity(then.complete_terms, now.complete_terms_current), \
               tversky_proto_weighted(then.complete_terms, now.complete_terms_current), \
               jaccard_similarity(then.top_terms, now.top_terms_current), \
               tversky_proto_weighted(then.top_terms, now.top_terms_current), \
               jaccard_similarity(then.top_parents, now.top_parents_current), \
               tversky_proto_weighted(then.top_parents, now.top_parents_current), \
               jaccard_similarity(then.top_genes, now.top_genes_current), \
               tversky_proto_weighted(then.top_genes, now.top_genes_current)

    cnt = 0
    for run in xrange(samples):
        if cnt % 100 == 0:
            log.info("%s%%", 100*cnt/samples)
        sample_permutations.append([(sims(m1,m2), m1, m2) for m1, m2 in enumerate(np.random.permutation(n))])


        cnt += 1
    log.info("%s%%", 100*cnt/samples)
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

        if len(sys.argv[1:]) > 4:
            control_file = sys.argv[5]
        else:
            control_file = False

        raw_data, settings = parser.parse_analysis_results(data_file)

        data = raw_data

        log.info('Actual Top Parents Jaccard Mean (+- SD): %s (+- %s)', mean([r.top_parents_jaccard for r in data]), sstdev([r.top_parents_jaccard for r in data]))
        log.info('Actual Top Parents Tversky Mean (+- SD): %s (+- %s)', mean([r.top_parents_tversky for r in data]), sstdev([r.top_parents_tversky for r in data]))
        log.info('Actual Age Mean (+- SD): %s (+- %s)', mean([r.age for r in data]), sstdev([r.age for r in data]))
        log.info('')

        if control_file:
            raw_control_data, control_settings = parser.parse_analysis_results(control_file)
            assert settings == control_settings
            control_data = raw_control_data

            log.info('Control Top Parents Jaccard Mean (+- SD): %s (+- %s)', mean([r.top_parents_jaccard for r in control_data]), sstdev([r.top_parents_jaccard for r in control_data]))
            log.info('Control Top Parents Tversky Mean (+- SD): %s (+- %s)', mean([r.top_parents_tversky for r in control_data]), sstdev([r.top_parents_tversky for r in control_data]))
            log.info('Control Age Mean (+- SD): %s (+- %s)', mean([r.age for r in control_data]), sstdev([r.age for r in control_data]))
            log.info('')

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

        log.info('Null Top Parents Jaccard Mean (+- SD): %s (+- %s)', mean([mean([s[0][4] for s in p]) for p in null_perms]), sstdev([sstdev([s[0][4] for s in p]) for p in null_perms]))
        log.info('Null Top Parents Tversky Mean (+- SD): %s (+- %s)', mean([mean([s[0][5] for s in p]) for p in null_perms]), sstdev([sstdev([s[0][5] for s in p]) for p in null_perms]))

        log.info("Writing null distribution to %s.tsv", null_out )
        with open(null_out + ".tsv", 'w+') as f_out:
            # Write null distribution
            f_out.write("\t".join([ch[0] for ch in parser.NULL_HEADER]) + "\n")
            cnt = 0
            total = len(null_perms)
            for i, p in enumerate(null_perms):
                if cnt % 100 == 0:
                    log.info("%s%%", 100*cnt/total)
                cnt += 1
                for sims, m_then, m_now in p:
                    try:
                        now = all_data[m_now]
                        then = all_data[m_then]
                        f_out.write("\t".join(map(str, [i, then.sys_name, now.sys_name, then.significant_terms, now.significant_terms_current, then.edition_date.strftime("%Y-%m-%d")] + list(sims))) + "\n")
                    except Exception as e:
                        log.error("%s,%s: %s", m_then, m_now, repr(e) )
            log.info("%s%%", 100*cnt/samples)
