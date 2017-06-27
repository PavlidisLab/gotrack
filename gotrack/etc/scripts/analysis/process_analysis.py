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
            header = ["sys_name","name","pmid","species","age","date","edition","edition_date","edition_go","edition_go_date","significant_terms","significant_terms_current","CompleteTermSim","TopTermSim","TopGeneSim","TopParentsSim","top_parents_mf","top_parents_mf_current","top_terms","top_terms_current","top_parents","top_parents_current","top_genes","top_genes_current","genes_found","genes_missed"]
            with open(out_folder + "/results", 'w+') as out_file:
                writer = csv.DictWriter(out_file, fieldnames = header)
                writer.writeheader()
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
                            sim = res['similarity_data'][0]

                            out_line = {}
                            out_line["sys_name"] = sys_name
                            out_line["name"] = name
                            out_line["pmid"] = pmid
                            out_line["species"] = species
                            out_line["age"] = sim['age_days']

                            out_line["date"] = [pubdate, epubdate][[d_pubdate,d_epubdate].index(d)]
                            out_line["edition"] = res['edition']['edition']
                            out_line["edition_date"] = res['edition']['date']
                            out_line["edition_go"] = res['edition']['goEditionId']
                            out_line["edition_go_date"] = res['edition']['goDate']

                            out_line["significant_terms"] = sim['significant_terms']
                            out_line["significant_terms_current"] = res['similarity_data'][1]['significant_terms']

                            for k,v in sim['values'].iteritems():
                                out_line[k] = v

                            out_line["top_parents_mf"] = sim['top_parents_mf']
                            out_line["top_parents_mf_current"] = res['similarity_data'][1]['top_parents_mf']

                            out_line["top_terms"] = ",".join(sim['top_terms'])
                            out_line["top_terms_current"] = ",".join(res['similarity_data'][1]['top_terms'])
                            out_line["top_parents"] = ",".join(sim['top_parents'])
                            out_line["top_parents_current"] = ",".join(res['similarity_data'][1]['top_parents'])
                            out_line["top_genes"] = ",".join([str(x['symbol']) for x in sim['top_genes']])
                            out_line["top_genes_current"] = ",".join([str(x['symbol']) for x in res['similarity_data'][1]['top_genes']])

                            out_line["genes_found"] = ",".join(["|".join((x['querySymbol'],x['symbol'])) for x in res['input_genes']['exact'] + res['input_genes']['exact_synonym'] ])
                            out_line["genes_missed"] = ",".join([x['querySymbol'] for x in res['input_genes']['unknown']])
                            
                            writer.writerow(out_line)
                                
                        except Exception, e:
                            print e
                            with open(out_folder + "/errors", "a") as myfile:
                                myfile.write(sys_name + "\t" + repr(e) + "\n")

            print "{0} / {1}".format(i, total)
            print "Creating settings file"

            with open(out_folder + "/settings", 'w+') as out_file:
                out_file.write("current_edition:\t{0}".format(res['current_edition']['edition']) + "\n")
                out_file.write("current_edition_date:\t{0}".format(res['current_edition']['date']) + "\n")
                out_file.write("topN:\t{0}".format(res['topN']) + "\n")
                out_file.write("mt_corr_method:\t{0}".format( json.dumps(res['mt_corr_method'])) + "\n")
                out_file.write("min_go_geneset:\t{0}".format(res['min_go_geneset']) + "\n")
                out_file.write("max_go_geneset:\t{0}".format(res['max_go_geneset']) + "\n")
                out_file.write("threshold:\t{0}".format(res['threshold']) + "\n")
                out_file.write("similarity_compare_method:\t{0}".format(json.dumps(res['similarity_compare_method'])) + "\n")
                out_file.write("aspect_filter:\t{0}".format(res['aspect_filter']) + "\n")

            print "Complete"