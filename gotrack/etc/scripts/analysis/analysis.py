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

"""Functional API forGOTrack's RESTful interface."""

__author__ = 'mjacobson'

import logging
import json
import urllib2
import threading
import time

log = logging.getLogger(__name__)
# log.addHandler(logging.NullHandler())

BASE_URL = "https://dev.gotrack.msl.ubc.ca/rest/"

class Edition:
    """Represents an edition of GeneOntology Annotations"""
    def __init__(self, edition, date, go_edition):
        self.edition = edition
        self.date = date
        self.go_edition = go_edition

class Enrichment:
    """Represents the results of an Enrichment Analysis sent to GOTrack's RESTful web services"""

    def __init__(self, edition, results):
        self.edition = edition
        self.results = results

def enrichment_historical(month, year, genes, species_id):
    content = {'month':month, 'year':year, 'genes':genes, 'speciesId':species_id}
    location = BASE_URL + 'analysis/enrichment/historical/'
    return __send_request(location, content)

def enrichment_current(genes, species_id):
    content = {'genes':genes, 'speciesId':species_id}
    location = BASE_URL + 'analysis/enrichment/'
    return __send_request(location, content)

def enrichment_complete(genes, species_id):
    content = {'genes':genes, 'speciesId':species_id}
    location = BASE_URL + 'analysis/enrichment/complete/'
    return __send_request(location, content)

def similarity(month, year, genes, species_id):
    content = {'month': month, 'year': year, 'genes': genes, 'speciesId': species_id, 'threshold': 0.05, 'min': 20,
               'max': 200, 'topN': 5, 'aspects': ['BP'], 'similarityMethod': 'JACCARD', 'multipleTestCorrection': 'BH'}
    location = BASE_URL + 'analysis/similarity/'
    return __send_request(location, content)

def gene_complete(month, year, genes, species_id):
    content = {'month':month, 'year':year, 'symbols':",".join(genes), 'speciesId':species_id}
    location = BASE_URL + 'gene/complete/species/{speciesId}/symbol/{symbols}?month={month}&year={year}&minimal=true'.format(**content)
    return __send_request(location)

def gene_mf(month, year, genes, species_id):
    content = {'month':month, 'year':year, 'symbols':",".join(genes), 'speciesId':species_id}
    location = BASE_URL + 'gene/mf/species/{speciesId}/symbol/{symbols}?month={month}&year={year}&minimal=true'.format(**content)
    return __send_request(location)


def __send_request(location, content_dict=None):
    """Send HTTP Requests"""
    req = urllib2.Request(location)
    req.add_header('Content-Type', 'application/json')
    try:
        if content_dict is not None:
            response = urllib2.urlopen(req, json.dumps(content_dict))
        else:
            response = urllib2.urlopen(req)
        contents = response.read()
    except urllib2.HTTPError, error:
        contents = error.read()

    try:
        json_response = json.loads(contents)
    except ValueError:
        log.warning("Response could not be parsed as JSON", contents)
        return

    return json_response