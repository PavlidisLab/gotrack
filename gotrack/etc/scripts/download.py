#!/usr/bin/env python

import logging
import os 
import re
from ftplib import FTP, error_perm
from datetime import datetime

log = logging.getLogger(__name__)

go_ftp_host = 'ftp.geneontology.org'
go_ftp_directory = '/go/ontology-archive/'
go_file_name_template = 'gene_ontology_edit.obo.{0}.gz'

goa_ftp_host = 'ftp.ebi.ac.uk'
goa_ftp_directory_template = '/pub/databases/GO/goa/old/{0}/'
goa_file_name_template = 'gene_association.goa_{0}.{1}.gz'

uniprot_ftp_host = 'ftp.uniprot.org'
uniprot_ftp_directory = '/pub/databases/uniprot/knowledgebase/docs/'


def download_sec_ac(save_folder):
    ftp = None
    try:
        ftp = FTP(uniprot_ftp_host)
        ftp.login()
        ftp.cwd(uniprot_ftp_directory)
        file_name = 'sec_ac.txt'
        full_path = os.path.join(save_folder, file_name)

        if os.path.isfile(full_path):
            print full_path + ' already exists, skipping...'
            return

        try:
            lf = open(full_path, "wb")
        except Exception as inst:
            print 'Problem creating output file', full_path, inst
            return

        try:
            ftp.retrbinary('RETR ' + file_name, lf.write)
            return full_path
        except Exception as inst:
            print 'Problem downloading file', inst
        finally:
            lf.close()
    finally:
        if ftp is not None:
            ftp.close()


def download_acindex(save_folder):
    ftp = None
    try:
        ftp = FTP(uniprot_ftp_host)
        ftp.login()
        ftp.cwd(uniprot_ftp_directory)
        file_name = 'acindex.txt'
        full_path = os.path.join(save_folder, file_name)

        if os.path.isfile(full_path):
            print full_path + ' already exists, skipping...'
            return

        try:
            lf = open(full_path, "wb")
        except Exception as inst:
            print 'Problem creating output file', full_path, inst
            return

        try:
            ftp.retrbinary('RETR ' + file_name, lf.write)
            return full_path
        except Exception as inst:
            print 'Problem downloading file', inst
        finally:
            lf.close()
    finally:
        if ftp is not None:
            ftp.close()


def fetch_goa_editions(sp):
    ftp_editions = {}
    ftp = None
    try:
        ftp = FTP(goa_ftp_host)
        ftp.login()

        ftp_directory = goa_ftp_directory_template.format(sp.upper())
        ftp.cwd(ftp_directory)
        ftp_files = ftp.nlst()

        pattern = r"^gene_association.goa_"+sp+".*\.(.*)\..*$"
        for f in ftp_files:
            fname = os.path.split(f)[1]  # Get filename
            match = re.match(pattern, fname)  # Get number between two periods
            if match is not None:
                ftp_editions[int(match.group(1))] = f
    except error_perm:
        log.warn("Cannot find species in FTP site: %s", sp)
    finally:
        if ftp is not None:
            ftp.close()

    return ftp_editions


def download_goa_data(sp_to_files, save_folder):
    ftp = None
    fname_list = []
    for sp, files in sp_to_files.iteritems():
        if files:
            try:
                ftp = FTP(goa_ftp_host)
                ftp.login()
                ftp_directory = goa_ftp_directory_template.format(sp.upper())
                ftp.cwd(ftp_directory)

                for fname in files:
                    log.info('Downloading: {0} ...'.format(fname))
                    try:
                        ftp.voidcmd("NOOP")
                    except Exception as inst:
                        log.warn('Timeout: Reconnecting to FTP Server, %s', inst)
                        ftp.close()
                        ftp = FTP(goa_ftp_host)
                        ftp.login()
                        ftp.cwd(ftp_directory)
                    full_path = os.path.join(save_folder, fname)
                    if os.path.isfile(full_path):
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
                log.warn("Cannot find species in FTP site: %s", sp)
            finally:
                if ftp is not None:
                    ftp.close()

    return fname_list


def fetch_go_dates():
    ftp = None
    try:
        ftp = FTP(go_ftp_host)
        ftp.login()

        ftp.cwd(go_ftp_directory)
        ftp_files = ftp.nlst()
        ftp_date_map = {}
        for f in ftp_files:
            fname = os.path.split(f)[1]  # Get filename
            match = re.match(r"^gene_ontology_edit.obo.*\.(.*)\..*$", fname)  # Get number between two periods
            if match is not None:
                try:
                    file_date = datetime.strptime(match.group(1), "%Y-%m-%d").date()
                    ftp_date_map[file_date] = f
                except ValueError:
                    log.warn('Cannot parse date: %s', fname)
                    continue
                except Exception as inst:
                    log.warn('Something went wrong %s %s', fname, inst)
                    continue
    finally:
        if ftp is not None:
            ftp.close()

    return ftp_date_map


def download_go_data(files, save_folder):
    ftp = None
    fname_list = []
    try:
        ftp = FTP(go_ftp_host)
        ftp.login()
        ftp.cwd(go_ftp_directory)

        for fname in files:
            log.info('Downloading: %s ...', fname)
            try:
                ftp.voidcmd("NOOP")
            except Exception as inst:
                log.warn('Timeout: Reconnecting to FTP Server, %s', inst)
                ftp.close()
                ftp = FTP(go_ftp_host)
                ftp.login()
                ftp.cwd(go_ftp_directory)
            full_path = os.path.join(save_folder, fname)
            if os.path.isfile(full_path):
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
    finally:
        if ftp is not None:
            ftp.close()
    return fname_list
