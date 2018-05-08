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

"""(DEPRECATED) Creates post-analysis tables."""

from __future__ import division
import glob
import json
from operator import methodcaller
import sys
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

def tversky_proto_weighted(prototype, variant):
    if len(prototype) == 0 and len(variant) == 0: return 1.0
    if len(prototype) == 0 or len(variant) == 0: return 0.0
    intersect_size = len(prototype.intersection(variant))
    return intersect_size / (intersect_size + len(prototype.difference(variant)))

if __name__ == '__main__':  
    if len(sys.argv[1:]) > 1:
        input_data = sys.argv[1]
        out_file = sys.argv[2]
        sim_matrix_file = sys.argv[3]
        age_matrix_file = sys.argv[4]
        filtered_content = []
        with open(out_file, 'w+') as f_out:
            f_out.write("sys_name\tname\tage(days)\tgenes_found\tgenes_missed\tsig_terms\tsig_terms_current\tTopGeneSim\tTopParentsSim\tCompleteTermSim\tTopTermSim\n")
            for filename in glob.iglob(input_data + '/M*'):
                try:
                    with open(filename,'rb') as f:
                        raw_content = f.readlines()
                        content = parseFile(raw_content)
                        # Write table
                        f_out.write("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n".format( content['sys_name'], content['name'], content['age'],
                             len(content["exact"]) + len(content["exact_synonym"]), len(content["unknown"]), content['significant_terms'], content['significant_terms_current'],
                             content["TopGeneSim"], content["TopParentsSim"], content["CompleteTermSim"], content["TopTermSim"]))
                        if content['significant_terms'] != 0 or content['significant_terms_current'] != 0:
                            filtered_content.append(content)
                except IOError, e:
                    print e

        # # Create Matrix
        # age_matrix = []
        # sim_matrix = []
        # total = len(filtered_content) * len(filtered_content)
        # cnt = 0
        # for i, old in enumerate(filtered_content):
        #     age_row = []
        #     sim_row = []
        #     for j, new in enumerate(filtered_content):
        #         if cnt % 1000000 == 0:
        #             print "{0} / {1} : {2}".format(cnt, total, cnt/total)
        #         old_pub_date = datetime.datetime.strptime(old['edition']['date'], "%Y-%m-%d").date()
        #         new_pub_date = datetime.datetime.strptime('2016-04-11', '%Y-%m-%d').date()
        #         #new_pub_date = datetime.datetime.strptime(new['edition']['date'], "%Y-%m-%d").date()
        #         age = abs((new_pub_date - old_pub_date).days)

        #         age_row.append(age)
        #         sim_row.append(tversky_proto_weighted(old['top_parents'], new['top_parents_current']))
        #         cnt += 1
        #     age_matrix.append(age_row)
        #     sim_matrix.append(sim_row)

        # Create Matrix
        age_matrix = []
        sim_matrix = []
        total = len(filtered_content) * len(filtered_content)
        cnt = 0
        for i, m1 in enumerate(filtered_content):
            age_row = []
            sim_row = []
            for j, m2 in enumerate(filtered_content):
                if cnt % 1000000 == 0:
                    print "{0} / {1} : {2}".format(cnt, total, cnt/total)

                # Compare m1.old -> m2.new

                m1_pub_date = datetime.datetime.strptime(m1['edition']['date'], "%Y-%m-%d").date()
                m2_pub_date = datetime.datetime.strptime('2016-04-11', '%Y-%m-%d').date()
                #m2_pub_date = datetime.datetime.strptime(m2['edition']['date'], "%Y-%m-%d").date()
                age = abs((m2_pub_date - m1_pub_date).days)

                age_row.append(age)
                sim_row.append(tversky_proto_weighted(m1['top_parents'], m2['top_parents_current']))

                # Compare m1.old -> m2.old
                m2_pub_date = datetime.datetime.strptime(m2['edition']['date'], "%Y-%m-%d").date()
                age = abs((m2_pub_date - m1_pub_date).days)

                age_row.append(age)
                sim_row.append(tversky_proto_weighted(m1['top_parents'], m2['top_parents']))

                cnt += 1
            age_matrix.append(age_row)
            sim_matrix.append(sim_row)

        with open(sim_matrix_file, 'w+') as f_out:
            for row in sim_matrix:
                f_out.write(",".join(map(str, row)) + "\n")

        with open(age_matrix_file, 'w+') as f_out:
            for row in age_matrix:
                f_out.write(",".join(map(str, row)) + "\n")
