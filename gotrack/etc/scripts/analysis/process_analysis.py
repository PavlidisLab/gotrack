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

"""Reads tsv from process_xml.py and runs analysis using GOTrack RESTful services. 
   Creates one file per line as well as a settings and error file."""
from __future__ import division

import csv
import datetime
import json
import logging.config
import sys

import dateutil.parser

import analysis

__author__ = 'mjacobson'

# Example logging
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()

def tversky_proto_weighted(prototype, variant):
    if len(prototype) == 0 and len(variant) == 0: return 1.0
    if len(prototype) == 0 or len(variant) == 0: return 0.0
    intersect_size = len(prototype.intersection(variant))
    return intersect_size / (intersect_size + len(prototype.difference(variant)))

if __name__ == '__main__':
    # import argparse

    if len(sys.argv[1:]) > 1:
        input_data = sys.argv[1]
        out_file = sys.argv[2]

        if len(sys.argv[1:]) > 3:
            default_year = int(sys.argv[3])
            default_month = int(sys.argv[4])
        else:
            default_year = 2000
            default_month = 12

        errors = []
        # try:
        #     # If there were previous errors attempt only them again
        #     with open(sys.argv[3],'rb') as f_error:
        #         errortsv = csv.reader(f_error, delimiter='\t')
        #         for sys_name, e in errortsv:
        #             errors.append(sys_name)
        # except IndexError:
        #     errors = None

        with open(input_data,'rb') as f:
            tsvin = csv.reader(f, delimiter='\t')
            total = sum(1 for row in tsvin if (errors == None or len(errors) == 0 or row[1] in errors))
            f.seek(0)
            tsvin = csv.reader(f, delimiter='\t')
            next(tsvin, None)  # skip the headers

            # Clear previous error file
            open(out_file + ".errors", 'w').close()

            log.info("0 / %s", total)
            i = 1
            header = ["sys_name", "name", "pmid", "species", "age", "date", "date_requested", "edition", "edition_date",
                      "edition_go_date", "significant_terms", "significant_terms_current", "tested_terms",
                      "tested_terms_current", "complete_term_jaccard", "top_term_jaccard", "top_gene_jaccard",
                      "top_parents_jaccard", "top_term_tversky", "top_gene_tversky", "top_parents_tversky",
                      "top_parents_mf", "top_parents_mf_current", "complete_terms", "complete_terms_current",
                      "top_terms", "top_terms_current", "top_parents",
                      "top_parents_current", "top_genes", "top_genes_current", "genes_found", "genes_missed"]
            with open(out_file + ".tsv", 'w+') as f_out:
                writer = csv.DictWriter(f_out, fieldnames = header, delimiter='\t')
                firstrow = True
                for name, sys_name, pmid, organism, pubdate, genes in tsvin:
                    if errors == None or len(errors) == 0 or sys_name in errors:
                        try:
                            if i % 100 == 0:
                                log.info("%s / %s", i, total)
                            i+=1

                            genes = genes.split(",")

                            try:
                                d_pubdate = dateutil.parser.parse(pubdate, default=datetime.datetime(default_year, default_month, 1))
                            except ValueError:
                                d_pubdate = datetime.datetime(default_year, default_month, 1)

                            res = analysis.similarity(d_pubdate.month, d_pubdate.year, genes, 7)

                            data = res['data']
                            reference_data = res['reference_data']

                            out_line = {}
                            out_line["sys_name"] = sys_name
                            out_line["name"] = name
                            out_line["pmid"] = pmid
                            # out_line["species_id"] = species_id
                            out_line["species"] = organism
                            out_line["age"] = data['similarity_age']

                            out_line["date"] = pubdate
                            out_line["date_requested"] = d_pubdate.strftime("%Y-%m-%d")

                            out_line["edition"] = data['edition']['edition']
                            out_line["edition_date"] = data['edition']['date']
                            out_line["edition_go_date"] = data['edition']['goEdition']['date']

                            out_line["significant_terms"] = data['significant_terms']
                            out_line["significant_terms_current"] = reference_data['significant_terms']

                            out_line["tested_terms"] = data['distinct_tested_terms']
                            out_line["tested_terms_current"] = reference_data['distinct_tested_terms']

                            out_line["complete_term_jaccard"] = data['values']['complete_term_sim']
                            out_line["top_term_jaccard"] = data['values']['top_term_sim']
                            out_line["top_gene_jaccard"] = data['values']['top_gene_sim']
                            out_line["top_parents_jaccard"] = data['values']['top_parents_sim']

                            out_line["top_term_tversky"] = tversky_proto_weighted(set(data['top_terms']), set(reference_data['top_terms']))
                            out_line["top_gene_tversky"] = tversky_proto_weighted(set([str(x['symbol']) for x in data['top_genes']]), set([str(x['symbol']) for x in reference_data['top_genes']]))
                            out_line["top_parents_tversky"] = tversky_proto_weighted(set(data['top_parents']), set(reference_data['top_parents']))

                            out_line["top_parents_mf"] = data['top_parents_mf']
                            out_line["top_parents_mf_current"] = reference_data['top_parents_mf']

                            out_line["complete_terms"] = ",".join(data['complete_terms'])
                            out_line["complete_terms_current"] = ",".join(reference_data['complete_terms'])
                            out_line["top_terms"] = ",".join(data['top_terms'])
                            out_line["top_terms_current"] = ",".join(reference_data['top_terms'])
                            out_line["top_parents"] = ",".join(data['top_parents'])
                            out_line["top_parents_current"] = ",".join(reference_data['top_parents'])
                            out_line["top_genes"] = ",".join([str(x['symbol']) for x in data['top_genes']])
                            out_line["top_genes_current"] = ",".join([str(x['symbol']) for x in reference_data['top_genes']])

                            out_line["genes_found"] = ",".join(["|".join((x['querySymbol'],x['symbol'])) for x in res['input_genes']['exact'] + res['input_genes']['exact_synonym'] ])
                            out_line["genes_missed"] = ",".join([x['querySymbol'] for x in res['input_genes']['unknown']])

                            if firstrow:
                                # Run Parameters
                                f_out.write("# reference_edition:\t{0}".format(res['reference_data']['edition']['edition']) + "\n")
                                f_out.write("# reference_edition_date:\t{0}".format(res['reference_data']['edition']['date']) + "\n")
                                f_out.write("# reference_edition_go_date:\t{0}".format(res['reference_data']['edition']['goEdition']['date']) + "\n")
                                f_out.write("# top_n:\t{0}".format(res['top_n']) + "\n")
                                f_out.write("# mt_corr_method:\t{0}".format( json.dumps(res['mt_corr_method'])) + "\n")
                                f_out.write("# similarity_method:\t{0}".format(json.dumps(res['similarity_method'])) + "\n")
                                f_out.write("# min_go_geneset:\t{0}".format(res['min_go_geneset']) + "\n")
                                f_out.write("# max_go_geneset:\t{0}".format(res['max_go_geneset']) + "\n")
                                f_out.write("# threshold:\t{0}".format(res['threshold']) + "\n")
                                f_out.write("# aspect_filter:\t{0}".format(res['aspect_filter']) + "\n")

                                # header
                                writer.writeheader()
                                firstrow = False

                            writer.writerow(out_line)

                        except Exception as e:
                            log.error("%s: %s", sys_name, repr(e) )
                            with open(out_file + ".errors", "a") as myfile:
                                myfile.write(sys_name + "\t" + repr(e) + "\n")

            print "{0} / {1}".format(i, total)

            print "Complete"