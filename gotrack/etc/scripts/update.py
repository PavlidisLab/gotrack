#!/usr/bin/env python
from __future__ import with_statement, division
import glob
import os
import tempfile
import shutil

from utility import *
from parsers import *
from gotrack import *
from model import *
import download
import MySQLdb.cursors
LOG = Log(sys.stdout)

# CREDS = {'host': 'localhost',
#          'user': 'root',
#          'passwd': 'toast',
#          'db': 'gotrack_test'
#          }

CREDS = {'host': 'abe',
         'user': 'gotrack',
         'passwd': 'gotrack321#',
         'db': 'gotrack'
         }


@timeit
def main(resource_directory=None, cron=False, no_dl=False, force_pp=False):
    tmp_directory = None
    try:
        # Connect to database
        gotrack = GOTrack(cursorclass=MySQLdb.cursors.SSCursor, **CREDS)
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

        # First check for GO Versions
        if not no_dl and (cron or query_yes_no("Check FTP sites for new GO Version data?")):
            if cron:
                LOG.info("Checking FTP sites for new GO Versions")
            ftp_go_dates = download.fetch_go_dates()
            missing_go_dates_from_ftp = [(d, f) for d, f in ftp_go_dates.iteritems()
                                         if d not in folder_go and d not in db_go]

            if len(missing_go_dates_from_ftp) > 0:
                LOG.warn("Missing {0} GO Versions from FTP".format(len(missing_go_dates_from_ftp)))

                if cron or query_yes_no("Download missing GO Versions?"):
                    if cron:
                        LOG.info("Downloading missing GO Versions")
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
        if not no_dl and (cron or query_yes_no("Check FTP sites for new GOA Edition data?")):
            if cron:
                LOG.info("Checking FTP sites for new GOA Edition data...")
            ftp_goa_editions = {}
            missing_goa_editions_from_ftp = {}
            for sp, sp_id in sp_map.iteritems():
                LOG.info("Checking {0}".format(sp))
                dl_fge = download.fetch_goa_editions(sp)
                ftp_goa_editions[sp_id] = dl_fge
                missing = [(x, f) for x, f in dl_fge.iteritems()  if x not in folder_goa.setdefault(sp_id, {}) and
                           x not in db_goa[sp_id]]
                missing_goa_editions_from_ftp[sp_id] = missing
                if len(missing) > 0:
                    LOG.info("Missing {0} Editions".format(len(missing)))

            count_missing_goa_editions = sum(len(v) for v in missing_goa_editions_from_ftp.itervalues())
            if count_missing_goa_editions > 0:
                LOG.warn("Missing {0} GOA Editions from FTP".format(count_missing_goa_editions))

                if cron or query_yes_no("Download missing GOA Editions?"):
                    if cron:
                        LOG.info("Downloading missing GOA Editions")
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
            if not no_dl and (cron or query_yes_no("Could not find sec_ac file, download?")):
                if cron:
                    LOG.info("Downloading sec_ac file")
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
            if not no_dl and (cron or query_yes_no("Could not find acindex file, download?")):
                if cron:
                    LOG.info("Downloading acindex file")
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
            if cron:
                LOG.info("Updating GO tables")
            for d, f in go_new:
                LOG.info("Begin:", d.strftime('%Y-%m-%d'))
                ont = Ontology.from_file_data(d, f)
                gotrack.update_go_table(ont)

        if count_goa_new > 0 and (cron or query_yes_no("Update GOA tables? (Affected tables: '{edition}', "
                                                       "'{gene_annotation}')".format(**gotrack.TABLES))):
            if cron:
                LOG.info("Updating GOA tables")
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
            if cron:
                LOG.info("Updating sec_ac table")
            data = process_sec_ac(sec_ac)
            gotrack.update_sec_ac_table(data, False)

        if acindex is not None and (cron or query_yes_no("Update acindex table? (Affected tables: '{acindex}')"
                                                         .format(**gotrack.TABLES))):
            if cron:
                LOG.info("Updating acindex table")
            data = process_acindex(acindex)
            gotrack.update_acindex_table(data, False)

        if cron or force_pp or query_yes_no("Check database for consistency? (Used for pre-processing)"):
            if cron and not force_pp:
                LOG.info("Checking database for consistency")
            elif force_pp:
                LOG.info("Forcing database preprocess (skipping consistency check)")
            if not force_pp:
                inconsistent, inc_results = check_database_consistency(gotrack)
                print inc_results

            if force_pp or inconsistent:
                if cron or force_pp or query_yes_no("Database requires preprocessing, run now? (Make sure new GO and GOA data has "
                                                    "been imported as well as new sec_ac and acindex files) (Affects tables: "
                                                    "'{pp_genes_staging}', '{pp_goa_staging}', '{pp_accession_staging}', "
                                                    "'{pp_synonym_staging}')"
                                                    .format(**GOTrack.TABLES)):
                    if cron and not force_pp:
                        LOG.info("Database requires preprocessing, running now...")
                    elif force_pp:
                        LOG.info("Preprocessing database...")

                    if force_pp or inc_results['preprocess_ood']:
                        LOG.info("Creating current genes tables...")
                        gotrack.pre_process_current_genes_tables()
                        LOG.info("Creating GOA table...")
                        gotrack.pre_process_goa_table()

                    if force_pp or inc_results['aggregate_ood']:
                        LOG.info("Creating aggregate tables...")
                        go_ed_to_sp_ed = defaultdict(list)
                        for sp_id, ed, goa_date, go_ed, go_date in gotrack.fetch_editions():
                            go_ed_to_sp_ed[go_ed].append((sp_id, ed))

                        gotrack.create_aggregate_staging()

                        i = 0
                        for go_ed, eds in sorted(go_ed_to_sp_ed.iteritems()):
                            i += 1
                            LOG.info("Starting Ontology:", i, "/", len(go_ed_to_sp_ed))
                            # node_list = gotrack.fetch_term_list(go_ed)
                            adjacency_list = gotrack.fetch_adjacency_list(go_ed)
                            ont = Ontology.from_adjacency("1900-01-01", adjacency_list)
                            for sp_id, ed in eds:
                                process_aggregate(gotrack, ont, sp_id, ed)

            else:
                LOG.info("Database does not require preprocessing.")
        else:
            LOG.info("Consistency Check Skipped")

    finally:
        cleanup(tmp_directory, cron)


def cleanup(d, cron=False):
    if d is not None and (cron or query_yes_no("Delete temporary folder {0} ?".format(d))):
        shutil.rmtree(d)
    elif d is not None:
        LOG.info("Temporary Folder: {0}".format(d))


def push_to_production():
    # Connect to database
    gotrack = GOTrack(**CREDS)
    LOG.info("Connected to host: {host}, db: {db}, user: {user}".format(**CREDS))

    gotrack.push_staging_to_production()

    log.info("Staging area has been pushed to production, a restart of GOTrack is now necessary")
    log.info("Remember to delete temporary old data tables if everything works")


def process_aggregate(gotrack, ont, sp_id, ed):

    all_annotations_stream = gotrack.fetch_all_annotations(sp_id, ed)

    # These are for computing the number of genes associated with each go term
    direct_counts_per_term = defaultdict(int)
    gene_id_set_per_term = defaultdict(set)

    # These are for computing the number of go terms associated with each gene
    term_set_per_gene_id = defaultdict(set)

    # Propagation Cache for performance purposes
    ancestor_cache = defaultdict(set)
    annotation_count = 0
    # ancestor_total_time = 0
    # ancestor_total_runs = 0
    # ancestor_total_cache_hits = 0
    for go_id, gene_id in all_annotations_stream:
        term = ont.get_term(go_id)

        if term is not None:
            annotation_count += 1
            # Deal with direct counts

            direct_counts_per_term[term] += 1

            # Deal with inferred counts
            # tmp_time = time.time()
            if term in ancestor_cache:
                ancestors = ancestor_cache[term]
                # ancestor_total_cache_hits += 1
            else:
                ancestors = ont.get_ancestors(term, True, ancestor_cache)
                ancestor_cache[term] = ancestors
            # ancestor_total_time += time.time() - tmp_time
            # ancestor_total_runs += 1

            for anc in ancestors:  # gene counts
                gene_id_set_per_term[anc].add(gene_id)

            term_set_per_gene_id[gene_id].update(ancestors)
        # else:
        #     LOG.warn(go_id + " not found in edition ({0}), go ediiton ({1})".format(ed, go_ed))

    # print "Total Ancestor Time: {0}".format(ancestor_total_time)
    # print "Total Ancestor Runs: {0}".format(ancestor_total_runs)
    # print "Total Ancestor Cache Hits: {0}".format(ancestor_total_cache_hits)
    # print "% Cache Hits: {0}".format(100 * ancestor_total_cache_hits / ancestor_total_runs)

    # Convert sets into counts

    inferred_counts_per_term = {t: len(s) for t, s in gene_id_set_per_term.iteritems()}

    total_gene_set_size = sum(len(s) for s in gene_id_set_per_term.itervalues())
    total_term_set_size = sum(len(s) for s in term_set_per_gene_id.itervalues())
    gene_count = len(term_set_per_gene_id)

    # write_total_time = time.time()

    # Write aggregates
    if gene_count > 0:
        gotrack.write_aggregate(sp_id, ed, gene_count, annotation_count / gene_count,
                                total_term_set_size / gene_count, total_gene_set_size / len(gene_id_set_per_term))
    else:
        LOG.warn("No Genes in Species ({0}), Edition ({1})".format(sp_id, ed))

    # Write Term Counts
    if total_gene_set_size > 0 or total_term_set_size > 0:
        gotrack.write_term_counts(sp_id, ed, direct_counts_per_term, inferred_counts_per_term)
    else:
        LOG.warn("No Annotations in Species ({0}), Edition ({1})".format(sp_id, ed))

    # write_total_time = time.time() - write_total_time

    # print "Total Write Time: {0}".format(write_total_time)

    # total_time = time.time() - total_time
    # print ""
    # print "Total Time: {0}".format(total_time)
    # print "Ancestor %: {0}".format(100 * ancestor_total_time / total_time)
    # print "Write %: {0}".format(100 * write_total_time / total_time)


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
    return results['preprocess_ood'] or results['aggregate_ood'], results


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
    import argparse

    parser = argparse.ArgumentParser(description='Update GOTrack Database')
    parser.add_argument('resource_directory', metavar='d', type=str, nargs='?', default=None,
                        help='a folder holding resources to use in the update')
    parser.add_argument('--push', dest='push', action='store_true',
                        help='Push Staging to Production, does not insert data')
    parser.add_argument('--meta', dest='meta', action='store_true',
                        help='Display Connection and Table Information')
    parser.add_argument('--cron', dest='cron', action='store_true',
                        help='No interactivity mode')
    parser.add_argument('--no-downloads', dest='dl', action='store_true',
                        help='Prevent all downloads')
    parser.add_argument('--force-pp', dest='force_pp', action='store_true',
                        help='Force preprocessing of database (regardless of need)')
    parser.add_argument('--update-push', dest='update_push', action='store_true',
                        help='Runs Update with options following by update with --push')

    args = parser.parse_args()
    if args.push and (args.resource_directory is not None or args.cron or args.dl or args.meta or args.force_pp or args.update_push):
        LOG.error("--push cannot be specified with other options")
    elif args.meta:
        # Push Staging to Production
        LOG.info("Host: {host}, db: {db}, user: {user}".format(**CREDS))
        LOG.info(*['TABLES:'] + sorted([x for x in GOTrack.TABLES.iteritems()]), sep="\n")
    elif args.update_push:
        # Update followed by push to production
        main(args.resource_directory, args.cron, args.dl, args.force_pp)
        if args.cron or query_yes_no("Push staging to production?"):
            push_to_production()
    elif args.push:
        # Push Staging to Production
        push_to_production()
    else:
        # Update database
        main(args.resource_directory, args.cron, args.dl, args.force_pp)



