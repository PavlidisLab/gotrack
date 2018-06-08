#
# The GOTrack project
#
# Copyright (c) 2018 University of British Columbia
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

"""Parsers for analysis files"""

import csv
import datetime
import logging
from collections import namedtuple
from operator import methodcaller

__author__ = 'mjacobson'

log = logging.getLogger(__name__)


# log.addHandler(logging.NullHandler())

def date_(x):
    return datetime.datetime.strptime(x, "%Y-%m-%d").date()


def list_(x):
    return set(x.split(",")) if x else set()


def list2_(x):
    return map(methodcaller("split", "|"), x.split(",")) if x else []


ANALYSIS_HEADER = [("sys_name", str),
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
                   ("tested_terms", int),
                   ("tested_terms_current", int),
                   ("complete_term_jaccard", float),
                   ("top_term_jaccard", float),
                   ("top_gene_jaccard", float),
                   ("top_parents_jaccard", float),
                   ("top_term_tversky", float),
                   ("top_gene_tversky", float),
                   ("top_parents_tversky", float),
                   ("top_parents_mf", float),
                   ("top_parents_mf_current", float),
                   ("complete_terms", list_),
                   ("complete_terms_current", list_),
                   ("top_terms", list_),
                   ("top_terms_current", list_),
                   ("top_parents", list_),
                   ("top_parents_current", list_),
                   ("top_genes", list_),
                   ("top_genes_current", list_),
                   ("genes_found", list2_),
                   ("genes_missed", list_)]

NULL_HEADER = [("PERMUTATION_RUN", int),
               ("SYSTEMATIC_NAME_THEN", str),
               ("SYSTEMATIC_NAME_NOW", str),
               ("SIGNIFICANT_TERMS_THEN", int),
               ("SIGNIFICANT_TERMS_NOW", int),
               ("THEN_DATE", date_),
               ("COMPLETE_TERMS_JACCARD", float),
               ("COMPLETE_TERMS_TVERSKY", float),
               ("TOP_TERMS_JACCARD", float),
               ("TOP_TERMS_TVERSKY", float),
               ("TOP_PARENTS_JACCARD", float),
               ("TOP_PARENTS_TVERSKY", float),
               ("TOP_GENES_JACCARD", float),
               ("TOP_GENES_TVERSKY", float)]

GeneSetAnalysis = namedtuple('GeneSetAnalysis', [h[0] for h in ANALYSIS_HEADER])
NullSetAnalysis = namedtuple('NullSetAnalysis', [h[0] for h in NULL_HEADER])


def parse_date(d, date_format):
    return datetime.datetime.strptime(d, date_format).date()


def parse_analysis_results(f):
    settings = {}
    # read tab-delimited file
    with open(f, 'rb') as infile:
        reader = csv.reader(infile, delimiter='\t')

        for r in reader:
            if not r[0].startswith("# "):
                # Check header
                assert r == [ch[0] for ch in ANALYSIS_HEADER]
                break
            # parse settings
            settings[r[0][2:-1]] = r[1]
        type_parser = [ch[1] for ch in ANALYSIS_HEADER]
        file_contents = [GeneSetAnalysis(*map(lambda x, y: x(y), type_parser, line)) for line in reader]

    settings['reference_edition_date'] = parse_date(settings['reference_edition_date'], '%Y-%m-%d')
    settings['reference_edition_go_date'] = parse_date(settings['reference_edition_go_date'], '%Y-%m-%d')
    return file_contents, settings


def parse_null(f):
    # read tab-delimited file
    with open(f, 'rb') as infile:
        reader = csv.reader(infile, delimiter='\t')
        header = next(reader)
        assert header == [ch[0] for ch in NULL_HEADER]

        type_parser = [header[1] for header in NULL_HEADER]
        contents = [NullSetAnalysis(*map(lambda t, c: t(c), type_parser, line)) for line in reader]

    return contents
