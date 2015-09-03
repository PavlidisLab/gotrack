#!/usr/bin/env python
from __future__ import with_statement
import glob
import os
import tempfile
import shutil

from utility import *
from parsers import *
from gotrack import *
from model import *
import download

LOG = Log(sys.stdout)

CREDS = {'host': 'localhost',
         'user': 'root',
         'passwd': 'toast',
         'db': 'gotrack_test'
         }

# CREDS = {'host': 'abe',
#          'user': 'gotrack',
#          'passwd': 'gotrack321#',
#          'db': 'gotrack'
#          }


def timeit(method):
    def timed(*args, **kw):
        ts = time.time()
        result = method(*args, **kw)
        te = time.time()

        print '%r: %2.2f sec' % \
              (method.__name__, te - ts)
        return result

    return timed


@timeit
def main(cron=False):
    tmp_directory = None
    try:
        try:
            resource_directory = sys.argv[1]
        except IndexError:
            resource_directory = None

        # Connect to database
        gotrack = GOTrack(**CREDS)
        LOG.info("Connected to host: {host}, db: {db}, user: {user}".format(**CREDS))

        # Get list of current data editions in database
        results = gotrack.fetch_current_state()
        sp_map = {name: sp_id for sp_id, name in results['species']}
        db_go = [x[1] for x in results['go_editions']]
        db_goa = results['editions']
        # check_database_consistency(gotrack)

        # Get list of data in provided resource directory
        folder_goa = defaultdict(list)
        folder_go = {}
        if resource_directory is not None:
            LOG.info("Searching given directory for resource files...")
            files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_ontology_edit.obo*gz'))]
            folder_go = search_files_for_go(files)
            LOG.info('GO dates found in resource directory:',
                     sorted([d.strftime("%Y-%m-%d") for d in folder_go.keys()]))

            files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_association.goa*gz'))]
            folder_goa = {sp_map[sp]: {x[0]: x[1] for x in v} for sp, v in search_files_for_goa(files).iteritems()}
            LOG.info(*['GOA Editions found in resource directory:'] +
                      [sp + ": " + str(folder_goa.setdefault(sp_id, {}).keys())
                       for sp, sp_id in sp_map.iteritems()], sep='\n')
        else:
            resource_directory = tempfile.mkdtemp()
            tmp_directory = resource_directory
            LOG.warn("No resource directory specified. Creating temporary folder at {0}".format(resource_directory))

        # Check for missing data from FTP sites
        LOG.info("Checking FTP sites for new GO Version data...")

        # First check for GO Versions
        if query_yes_no("Check FTP sites for new GO Version data?"):
            ftp_go_dates = download.fetch_go_dates()
            missing_go_dates_from_ftp = [(d, f) for d, f in ftp_go_dates.iteritems()
                                         if d not in folder_go and d not in db_go]

            if len(missing_go_dates_from_ftp) > 0:
                LOG.warn("Missing {0} GO Versions from FTP".format(len(missing_go_dates_from_ftp)))

                if query_yes_no("Download missing GO Versions?"):
                    download.download_go_data([x[1] for x in missing_go_dates_from_ftp], resource_directory)

                    files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_ontology_edit.obo*gz'))]
                    folder_go = search_files_for_go(files)

                    missing_go_dates_from_ftp = [(d, f) for d, f in ftp_go_dates.iteritems()
                                                 if d not in folder_go and d not in db_go]

                    if len(missing_go_dates_from_ftp) > 0:
                        LOG.error("Still missing {0} GO Versions from FTP, something went wrong with download. "
                                  "Exiting...".format(len(missing_go_dates_from_ftp)))
                        return
            else:
                LOG.info("No new GO Versions found on FTP site")

        # Now check for GOA Editions
        if query_yes_no("Check FTP sites for new GOA Edition data?"):
            # LOG.info("Checking FTP sites for new GOA Edition data...")
            ftp_goa_editions = {}
            for sp, sp_id in sp_map.iteritems():
                LOG.info("Checking {0}".format(sp))
                ftp_goa_editions[sp_id] = download.fetch_goa_editions(sp)

            missing_goa_editions_from_ftp = {sp_id: [(x, f) for x, f in ftp_goa_editions[sp_id].iteritems()
                                             if x not in folder_goa.setdefault(sp_id, {}) and x not in db_goa[sp_id]]
                                             for sp, sp_id in sp_map.iteritems()}

            count_missing_goa_editions = sum(len(v) for v in missing_goa_editions_from_ftp.itervalues())
            if count_missing_goa_editions > 0:
                LOG.warn("Missing {0} GOA Editions from FTP".format(count_missing_goa_editions))

                if query_yes_no("Download missing GOA Editions?"):
                    download.download_goa_data({sp: [x[1] for x in missing_goa_editions_from_ftp[sp_id]]
                                                for sp, sp_id in sp_map.iteritems()}, resource_directory)

                    files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_association.goa*gz'))]
                    folder_goa = {sp_map[sp]: {x[0]: x[1] for x in v} for sp, v
                                  in search_files_for_goa(files).iteritems()}
                    missing_goa_editions_from_ftp = {sp_id: [(x, f) for x, f in ftp_goa_editions[sp_id].iteritems()
                                                     if x not in folder_goa.setdefault(sp_id, {}) and x not in
                                                     db_goa[sp_id]] for sp, sp_id in sp_map.iteritems()}
                    count_missing_goa_editions = sum(len(v) for v in missing_goa_editions_from_ftp.itervalues())
                    if count_missing_goa_editions > 0:
                        LOG.error("Still missing {0} GOA Editions from FTP, something went wrong with download. "
                                  "Exiting...".format(count_missing_goa_editions))
                        return
            else:
                LOG.info("No new GOA Editions found on FTP site")

        # Now check mapping files (sec_ac, acindex)
        LOG.info('Checking for new GOA resource files')
        files = [f for f in glob.iglob(os.path.join(resource_directory, 'sec_ac.txt'))]

        if len(files) == 1:
            sec_ac = files[0]
            LOG.info("Found sec_ac file ({0}). Ready for update.".format(sec_ac))
        else:
            if query_yes_no("Could not find sec_ac file, download?"):
                download.download_sec_ac(resource_directory)

            files = [f for f in glob.iglob(os.path.join(resource_directory, 'sec_ac.txt'))]
            if len(files) == 1:
                sec_ac = files[0]
                LOG.info("Found sec_ac file ({0}). Ready for update.".format(sec_ac))
            else:
                LOG.warn("No sec_ac file present, update might produce unknown results if the table is not up to date")
                sec_ac = None

        files = [f for f in glob.iglob(os.path.join(resource_directory, 'acindex.txt'))]

        if len(files) == 1:
            acindex = files[0]
            LOG.info("Found acindex file ({0}). Ready for update.".format(acindex))
        else:
            if query_yes_no("Could not find acindex file, download?"):
                download.download_acindex(resource_directory)

            files = [f for f in glob.iglob(os.path.join(resource_directory, 'acindex.txt'))]
            if len(files) == 1:
                acindex = files[0]
                LOG.info("Found acindex file ({0}). Ready for update.".format(acindex))
            else:
                LOG.warn("No acindex file present, update might produce unknown results if the table is not up to date")
                acindex = None

        go_new = [(d, f) for d, f in folder_go.iteritems() if d not in db_go]

        if len(go_new) > 0:
            LOG.info("New GO Versions ready to update: {0}".format(len(go_new)))
        else:
            LOG.info("There are no new GO Versions available to update")

        goa_new = {sp_id: [(x, f) for x, f in folder_goa.setdefault(sp_id, {}).iteritems() if x not in db_goa[sp_id]]
                   for sp, sp_id in sp_map.iteritems()}
        count_goa_new = sum(len(v) for v in goa_new.itervalues())

        if count_goa_new > 0:
            LOG.info("New GOA Editions ready to update: {0}".format(count_goa_new))
        else:
            LOG.info("There are no new GOA Editions available to update")

        # Begin updates
        if len(go_new) > 0 and (cron or query_yes_no("Update GO tables? (Affected tables: '{go_edition}', '{go_term}', "
                                             "'{go_adjacency}')".format(**gotrack.TABLES))):
            for d, f in go_new:
                LOG.info("Begin:", d.strftime('%Y-%m-%d'))
                ont = Ontology(d, f)
                gotrack.update_go_table(ont)

        if count_goa_new > 0 and (cron or query_yes_no("Update GOA tables? (Affected tables: '{edition}', '{gene_annotation}')"
                                              .format(**gotrack.TABLES))):
            for sp_id, files in goa_new.iteritems():
                LOG.info("Begin Species:", sp_id)
                for e, f in files:
                    LOG.info("Edition:", e)
                    meta = retrieve_meta_goa(f)
                    if meta[0] != "2.0":
                        LOG.warn('Illegal GAF Version:', meta[0])
                        continue

                    if meta[1] is None:
                        LOG.warn('Missing Generated Tag')
                        continue
                    try:
                        generated = datetime.strptime(meta[1], "%Y-%m-%d").date()
                    except ValueError:
                        LOG.warn('Cannot parse Generated Tag as date:', meta[1])
                        continue
                    data = process_goa(f, sp_id, e)
                    gotrack.update_goa_table(sp_id, e, generated, data)

        if sec_ac is not None and (cron or query_yes_no("Update sec_ac table? (Affected tables: '{sec_ac}')"
                                               .format(**gotrack.TABLES))):
            data = process_sec_ac(sec_ac)
            gotrack.update_sec_ac_table(data, False)

        if acindex is not None and (cron or query_yes_no("Update acindex table? (Affected tables: '{acindex}')"
                                                .format(**gotrack.TABLES))):
            data = process_acindex(acindex)
            gotrack.update_acindex_table(data, False)

        inconsistent, inc_results = check_database_consistency(gotrack)
        print inc_results

        if inc_results['preprocess_ood'] or inc_results['aggregate_ood']:
            if cron or query_yes_no("Database requires preprocessing, run now? "
                                    "(Make sure new GO and GOA data has been imported as well as new sec_ac and acindex files) "
                                    "(Affects tables: '{current_genes}', '{goa}', '{accession}', '{synonym}' "
                                    .format(**GOTrack.TABLES)):
                gotrack.pre_process_current_genes_tables()
                gotrack.pre_process_goa_table()
        else:
            LOG.info("Database does not require preprocessing.")

    finally:
        cleanup(tmp_directory)


def cleanup(d, q=False):
    if d is not None and (q or query_yes_no("Delete temporary folder {0} ?".format(d))):
        shutil.rmtree(d)


def check_database_consistency(gotrack, verbose=True):
    results = gotrack.fetch_consistency()
    if verbose:
        if results['preprocess_ood']:
            LOG.warn('Pre-processed tables are inconsistent with current data, requires updating.')
        if results['aggregate_ood']:
            LOG.warn('Aggregate tables are inconsistent with current data, requires updating.')
        if results['unlinked_editions']:
            LOG.warn('The following GOA editions are unlinked with GO ontologies')
            LOG.warn(results['unlinked_editions'])
    return results['preprocess_ood'] or results['aggregate_ood'] or results['unlinked_editions'], results


def search_files_for_go(files):

    # Parse filenames to get edition number. Structure is gene_association.goa_[species].[edition].gz
    dates = []
    date_map = {}
    for f in files:
        fname = os.path.split(f)[1]  # Get filename
        match = re.match(r"^gene_ontology_edit.obo.*\.(.*)\..*$", fname)  # Get number between two periods
        if match is not None:
            try:
                file_date = datetime.strptime(match.group(1), "%Y-%m-%d").date()
                dates.append(file_date)
                date_map[file_date] = f
            except ValueError:
                print 'Cannot parse date: ', fname
                continue
            except Exception as inst:
                print 'Something went wrong', fname, inst
                continue

    return date_map


def search_files_for_goa(files):

    species_files = defaultdict(list)

    for f in files:
        fname = os.path.split(f)[1]  # Get filename
        match = re.match(r"^gene_association.goa.*\.gz$", fname)
        if match is None:
            continue
        name_parts = fname.split(".")
        try:
            sp = name_parts[1].split('_')[-1]
            edition = int(name_parts[2])
        except Exception as inst:
            print 'Cannot parse filename:', fname, inst
            continue
        files_in_species = species_files[sp]
        files_in_species.append((edition, f))

    return species_files

if __name__ == '__main__':
    main(False)
