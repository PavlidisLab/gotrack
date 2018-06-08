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

"""Extracts and converts GSEA | MSigDB C2 xml file into tsv."""

__author__ = 'mjacobson'


from collections import namedtuple
import logging.config
import analysis
import sys
from xml.dom import minidom
import time

# Example logging
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()

ORGANISM_MAP = {
    "Arabidopsis thaliana" : 1,
    "Gallus gallus" : 2,
    "Bos taurus" : 3,
    "Dictyostelium discoideum" : 4,
    "Canis lupus familiaris" : 5,
    "Drosophila melanogaster" : 6,
    "Homo sapiens" : 7,
    "Mus musculus" : 8,
    "Sus scrofa" : 9,
    "Rattus norvegicus" : 10,
    "Caenorhabditis elegans" : 11,
    "Saccharomyces cerevisiae S288c" : 12,
    "Danio rerio" : 13,
    "Escherichia coli" : 14,
}

GeneSet = namedtuple('GeneSet', ['name', 'sys_name', 'pmid', 'organism', 'genes'])

def parse_XML(c_xml):
    doc = minidom.parse(c_xml)
    gsets = doc.getElementsByTagName("GENESET")
    results = []
    for gset in gsets:
        name = gset.getAttribute("STANDARD_NAME")
        sys_name = gset.getAttribute("SYSTEMATIC_NAME")
        pmid = gset.getAttribute("PMID")
        organism = gset.getAttribute("ORGANISM")
        genes = gset.getAttribute("MEMBERS_SYMBOLIZED").split(",")
        results.append(GeneSet(name, sys_name, pmid, organism, genes))

    return results

def chunker(seq, size):
    return (seq[pos:pos + size] for pos in xrange(0, len(seq), size))

if __name__ == '__main__':
    # import argparse
    
    if len(sys.argv[1:]) > 1:
        input_xml = sys.argv[1]
        output = sys.argv[2]
        data_gen = parse_XML(input_xml)
        log.info("Genesets: %s", len(data_gen))
        pmids = set()
        for dat in data_gen:
            pmids.add(dat.pmid)
        pmids = list(pmids)
        log.info("PMIDS: %s", len(pmids))
        pmid_map = {}
        for pmids_chunk in chunker(pmids, 50):
            location = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=' + ",".join(pmids_chunk) + '&retmode=json'
            log.info(location)
            req = analysis.__send_request(location)
            for pmid in pmids_chunk:
                try:
                    req_info = req['result'][pmid]
                except KeyError:
                    log.info("Unknown pmid: %s", pmid)
                    continue
                try:
                    pubdate = req_info['pubdate']
                    pmid_map[pmid] = pubdate
                except KeyError:
                    log.info("pmid has no date: %s", pmid)
            time.sleep(1)

        log.info("pmids mapped %s / %s", len(pmid_map), len(pmids))

        errors = []
        success = []
        with open(output, 'w+') as out_file:
            out_file.write("\t".join(["STANDARD_NAME", "SYSTEMATIC_NAME", "PMID", "ORGANISM", "pubdate", "MEMBERS_SYMBOLIZED"]) + "\n")
            for dat in data_gen:
                try:
                    pubdate = pmid_map[dat.pmid]
                    success.append(dat.pmid)
                except KeyError:
                    errors.append(dat.pmid)
                    log.info("geneset has no pmid date: %s - %s", dat.sys_name, dat.pmid)
                    pubdate = "unknown"

                out_file.write("\t".join([dat.name, dat.sys_name, dat.pmid, dat.organism, pubdate,
                                          ",".join(dat.genes)]) + "\n")
        log.info("Success: %s, Errors: %s", len(success), len(errors))
