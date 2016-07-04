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
__author__ = 'mjacobson'


import logging
import logging.config
import analysis
import sys
import csv
import dateutil.parser
import json

# Example logging
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()

if __name__ == '__main__':
    # import argparse
    
    if len(sys.argv[1:]) > 1:
        input_data = sys.argv[1]
        out_folder = sys.argv[2]
        try:
            errors = []
            with open(sys.argv[3],'rb') as f_error:
                errortsv = csv.reader(f_error, delimiter='\t')
                for sys_name, e in errortsv:
                    errors.append(sys_name)
        except IndexError:
            errors = None

        with open(input_data,'rb') as f:
            tsvin = csv.reader(f, delimiter='\t')
            total = sum(1 for row in tsvin if (errors == None or len(errors) == 0 or row[1] in errors))
            f.seek(0)
            tsvin = csv.reader(f, delimiter='\t')

            open(out_folder + "/errors", 'w').close()

            print "0 / {0}".format(total)
            i = 1
            for name, sys_name, pmid, species, pubdate, epubdate, genes in tsvin:
                if errors == None or len(errors) == 0 or sys_name in errors:
                    try:
                        if i % 100 == 0:
                            print "{0} / {1}".format(i, total)
                        i+=1
                        species = int(species)
                        genes = genes.split(",")
                        d_pubdate = dateutil.parser.parse(pubdate)
                        d_epubdate = dateutil.parser.parse(epubdate)
                        d = min(d_pubdate, d_epubdate)
                        res = analysis.similarity(d.month, d.year, genes, 7)
                        with open(out_folder + "/" + sys_name, 'w+') as out_file:
                            out_file.write("name:\t{0}".format(name) + "\n")
                            out_file.write("sys_name:\t{0}".format(sys_name) + "\n")
                            out_file.write("pmid:\t{0}".format(pmid) + "\n")
                            out_file.write("species:\t{0}".format(species) + "\n")
                            out_file.write("date:\t{0}".format([pubdate, epubdate][[d_pubdate,d_epubdate].index(d)]) + "\n")

                            out_file.write("edition:\t{0}".format(json.dumps(res['edition'])) + "\n")
                            out_file.write("exact:\t{0}".format(",".join(["|".join((x['querySymbol'],x['symbol'])) for x in res['input_genes']['exact']])) + "\n")
                            out_file.write("exact_synonym:\t{0}".format(",".join(["|".join((x['querySymbol'],x['symbol'])) for x in res['input_genes']['exact_synonym']])) + "\n")
                            out_file.write("unknown:\t{0}".format(",".join([x['querySymbol'] for x in res['input_genes']['unknown']])) + "\n")

                            sim = res['similarity_data'][0]
                            out_file.write("age:\t{0}".format(sim['age_days']) + "\n")
                            out_file.write("significant_terms:\t{0}".format(sim['significant_terms']) + "\n")
                            out_file.write("significant_terms_current:\t{0}".format(res['similarity_data'][1]['significant_terms']) + "\n")

                            for k,v in sim['values'].iteritems():
                                out_file.write(k + ":\t{0}".format(v) + "\n")
                    except Exception, e:
                        with open(out_folder + "/errors", "a") as myfile:
                            myfile.write(sys_name + "\t" + str(e) + "\n")

            print "{0} / {1}".format(i, total)
            print "Creating settings file"

            with open(out_folder + "/settings", 'w+') as out_file:
                out_file.write("topN:\t{0}".format(res['topN']) + "\n")
                out_file.write("mt_corr_method:\t{0}".format( json.dumps(res['mt_corr_method'])) + "\n")
                out_file.write("min_go_geneset:\t{0}".format(res['min_go_geneset']) + "\n")
                out_file.write("max_go_geneset:\t{0}".format(res['max_go_geneset']) + "\n")
                out_file.write("threshold:\t{0}".format(res['threshold']) + "\n")
                out_file.write("similarity_compare_method:\t{0}".format(json.dumps(res['similarity_compare_method'])) + "\n")
                out_file.write("aspect_filter:\t{0}".format(res['aspect_filter']) + "\n")

            print "Complete"