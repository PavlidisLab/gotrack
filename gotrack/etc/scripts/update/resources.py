#!/usr/bin/env python

import logging
import os 
import re
from ftplib import FTP, error_perm
from datetime import datetime
import glob
from collections import defaultdict

log = logging.getLogger(__name__)


class Resources:

    go_ftp_host = 'ftp.geneontology.org'
    go_ftp_directory = '/go/ontology-archive/'
    go_regex = r'^gene_ontology_edit\.obo\.(.*)\.gz$'
    go_template = 'gene_ontology_edit.obo.{0}.gz'

    goa_ftp_host = 'ftp.ebi.ac.uk'
    goa_ftp_directory_template = '/pub/databases/GO/goa/old/{0}/'
    goa_gaf_regex = r'^goa_([a-zA-Z]+)\.gpa\.(\d+)\.gz$'
    goa_gpi_regex = r'^goa_([a-zA-Z]+)\.gpi\.(\d+)\.gz$'
    goa_gaf_template = 'goa_{0}.gpa.{1}.gz'
    goa_gpi_template = 'goa_{0}.gpi.{1}.gz'

    uniprot_ftp_host = 'ftp.uniprot.org'
    uniprot_ftp_directory = '/pub/databases/uniprot/knowledgebase/docs/'

    def __init__(self, directory, database_state, check_ftp=True):
        self.directory = directory
        self.sp_map = {name: sp_id for sp_id, name in database_state['species']}
        self.db_go = [x[1] for x in database_state['go_editions']]
        self.db_goa = database_state['editions']

        self.sec_ac = None
        self.goa = defaultdict(dict)
        self.go = {}
        self.populate_existing_files()

        self.missing_go = {}
        self.missing_goa = defaultdict(dict)
        self.ftp_checked = False

        if check_ftp:
            self.populate_missing_data()

    def populate_existing_files(self):
        log.info("Checking for existing resources")
        self.sec_ac = None
        self.goa = defaultdict(dict)
        self.go = {}

        files = [f for f in glob.iglob(os.path.join(self.directory, "*"))]

        sec_ac = Resources.match_file_patterns(files, "sec_ac.txt")
        if len(sec_ac) > 1:
            log.warn("Found multiple sec_ac files")
        elif sec_ac:
            self.sec_ac = sec_ac[0]['file']

        self.go = self.search_files_for_go(files)
        gpa = self.search_files_for_goa_gaf(files)
        gpi = self.search_files_for_goa_gpi(files)

        for sp in gpa:
            gaf_eds = gpa[sp]
            gpi_eds = gpi[sp]

            for ed, f in gaf_eds.iteritems():
                try:
                    self.goa[sp][ed] = (f, gpi_eds[ed])
                except KeyError:
                    log.warn("Missing GPI file for species (%s) in edition (%s)", sp, ed)

    def populate_missing_data(self):
        log.info("Checking for missing resources")
        self.missing_go = {}
        self.missing_goa = defaultdict(dict)
        self.ftp_checked = False

        go = self.fetch_go_dates()
        self.missing_go = {d: f for d, f in go.iteritems() if d not in self.go and d not in self.db_go}

        for sp, sp_id in self.sp_map.iteritems():
            log.debug("Checking %s", sp)
            goa = self.fetch_goa_editions_for_species(sp)
            missing = {ed: f for ed, f in goa.iteritems() if ed not in self.goa[sp] and ed not in self.db_goa[sp_id]}
            self.missing_goa[sp] = missing

        self.ftp_checked = True

    def is_missing_data(self):
        return self.missing_go or sum([len(goa_sp) for goa_sp in self.missing_goa.values()]) or not self.sec_ac

    @staticmethod
    def match_file_patterns(files, pattern):
        matches = []
        if files:
            for f in files:
                match = re.match(pattern, os.path.basename(f))
                if match is not None:
                    matches.append({'match': match.groups(), 'file': f})

        return matches

    @staticmethod
    def search_files_for_go(files):
        # Parse filenames to get edition number. Structure is gene_association.goa_[species].[edition].gz

        date_map = {}
        matches = Resources.match_file_patterns(files, Resources.go_regex)
        for m in matches:
            date_string = m['match'][0]
            f = m['file']
            try:
                file_date = datetime.strptime(date_string, "%Y-%m-%d").date()
                date_map[file_date] = f
            except ValueError:
                log.warn('Cannot parse date: %s', os.path.basename(f))
                continue
            except Exception as inst:
                log.warn('Something went wrong %s: %s', os.path.basename(f), inst)
                continue

        return date_map

    @staticmethod
    def search_files_for_goa_gaf(files):
        matches = Resources.match_file_patterns(files, Resources.goa_gaf_regex)
        species_files = defaultdict(dict)

        for m in matches:
            sp = m['match'][0]
            ed = int(m['match'][1])
            species_files[sp][ed] = m['file']

        return species_files

    @staticmethod
    def search_files_for_goa_gpi(files):
        matches = Resources.match_file_patterns(files, Resources.goa_gpi_regex)
        species_files = defaultdict(dict)

        for m in matches:
            sp = m['match'][0]
            ed = int(m['match'][1])
            species_files[sp][ed] = m['file']

        return species_files

    @staticmethod
    def ftp_list(host, directory):
        ftp = None
        try:
            ftp = FTP(host)
            ftp.login()

            ftp.cwd(directory)

            return ftp.nlst()
        except error_perm:
            log.warn("Cannot find directory in FTP site: %s", directory)
        finally:
            if ftp is not None:
                ftp.close()

    def ftp_files(self, host, directory, files, skip_if_exists=True):
        ftp = None
        fname_list = []
        if files:
            try:
                ftp = FTP(host)
                ftp.login()
                ftp.cwd(directory)

                for fname in files:
                    log.info('Downloading: {0} ...'.format(fname))
                    try:
                        ftp.voidcmd("NOOP")
                    except Exception as inst:
                        log.warn('Timeout: Reconnecting to FTP Server, %s', inst)
                        ftp.close()
                        ftp = FTP(host)
                        ftp.login()
                        ftp.cwd(directory)
                    full_path = os.path.join(self.directory, fname)
                    if skip_if_exists and os.path.isfile(full_path):
                        log.warn('%s already exists, skipping...', full_path)
                        continue

                    try:
                        lf = open(full_path, "wb")
                    except Exception as inst:
                        log.warn('Problem creating output file %s, skipping..., %s', full_path, inst)
                        continue

                    try:
                        ftp.retrbinary('RETR ' + fname, lf.write)
                        fname_list.append(full_path)
                        log.info('Download Complete')
                    except Exception as inst:
                        log.warn('Problem downloading file, %s', inst)
                    finally:
                        lf.close()
            except error_perm:
                log.warn("Cannot find directory in FTP site: %s", directory)
            finally:
                if ftp is not None:
                    ftp.close()

        return fname_list

    def fetch_goa_editions_for_species(self, sp):
        files = Resources.ftp_list(Resources.goa_ftp_host, Resources.goa_ftp_directory_template.format(sp.upper()))
        gpa = self.search_files_for_goa_gaf(files)
        return gpa[sp]

    def fetch_go_dates(self):
        files = Resources.ftp_list(Resources.go_ftp_host, Resources.go_ftp_directory)
        return self.search_files_for_go(files)

    def download_missing_goa_data(self, skip_if_exists=True):
        for sp, goa_eds in self.missing_goa.iteritems():
            files = goa_eds.values()
            self.ftp_files(Resources.goa_ftp_host, Resources.goa_ftp_directory_template.format(sp.upper()), files,
                           skip_if_exists)

            # GPI Files
            gpi_files = [Resources.goa_gpi_template.format(sp, ed) for ed in goa_eds]
            self.ftp_files(Resources.goa_ftp_host, Resources.goa_ftp_directory_template.format(sp.upper()), gpi_files,
                           skip_if_exists)

    def download_missing_go_data(self, skip_if_exists=True):
        self.ftp_files(Resources.go_ftp_host, Resources.go_ftp_directory, self.missing_go.values(), skip_if_exists)

    def download_accession_history(self, skip_if_exists=True):
        return self.ftp_files(Resources.uniprot_ftp_host, Resources.uniprot_ftp_directory, ['sec_ac.txt'],
                              skip_if_exists)

    def get_new_go(self):
        return {d: f for d, f in self.go.iteritems() if d not in self.db_go}

    def get_new_goa(self):
        return {sp: {ed: fs for ed, fs in self.goa[sp].iteritems() if ed not in self.db_goa[sp_id]} for sp, sp_id in self.sp_map.iteritems()}

    def __str__(self):
        to_string = ['Current state of resources:',
                     'GO Editions found in resource directory: {0}'.format(len(self.go)),
                     'GOA Editions found in resource directory: {0}'.format(sum([len(goa_sp) for goa_sp in self.goa.values()])),
                     'Secondary accession file (sec_ac.txt): ' + (self.sec_ac if self.sec_ac else 'NOT FOUND')]

        if self.ftp_checked:
            if self.missing_go:
                to_string += ['Missing GO dates: {0}'.format(sorted([d.strftime("%Y-%m-%d") for d in self.missing_go]))]
            else:
                to_string += ['No missing GO Dates']

            missing_goa_cnt = sum([len(goa_sp) for goa_sp in self.missing_goa.values()])
            if missing_goa_cnt:
                to_string += ['{0} missing GOA Editions: \n{1}'.format(missing_goa_cnt, "\n".join([sp + ": " + str(sorted(self.missing_goa[sp].keys())) for sp in self.sp_map]))]
            else:
                to_string += ['No missing GOA Editions']
        return '\n\n'.join(to_string)
