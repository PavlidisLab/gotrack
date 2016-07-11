#!/usr/bin/env python
from __future__ import with_statement, division
import logging
import logging.config
import glob
import os
import tempfile
import shutil
from collections import defaultdict
import re
from datetime import datetime
import MySQLdb.cursors

from utility import query_yes_no, timeit
import parsers
import gotrack as gtdb
from model import Ontology
import download


# log = Log(sys.stdout)
logging.config.fileConfig('logging.conf', disable_existing_loggers=False)
logging.addLevelName(logging.WARNING, "\033[1;31m%s\033[1;0m" % logging.getLevelName(logging.WARNING))
logging.addLevelName(logging.ERROR, "\033[1;41m%s\033[1;0m" % logging.getLevelName(logging.ERROR))
log = logging.getLogger()

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
        gotrack = gtdb.GOTrack(cursorclass=MySQLdb.cursors.SSCursor, **CREDS)
        log.info("Connected to host: {host}, db: {db}, user: {user}".format(**CREDS))

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
            log.info("Searching given directory for resource files...")
            files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_ontology_edit.obo*gz'))]
            folder_go = search_files_for_go(files)
            log.info('GO dates found in resource directory: %s',
                     sorted([d.strftime("%Y-%m-%d") for d in folder_go.keys()]))

            files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_association.goa*gz'))]
            folder_goa = {sp_map[sp]: {x[0]: x[1] for x in v} for sp, v in search_files_for_goa(files).iteritems()}
            log.info('GOA Editions found in resource directory: \n%s', "\n".join([sp + ": " + str(folder_goa.setdefault(sp_id, {}).keys())
                       for sp, sp_id in sp_map.iteritems()]))
        else:
            resource_directory = tempfile.mkdtemp()
            tmp_directory = resource_directory
            log.warn("No resource directory specified. Creating temporary folder at {0}".format(resource_directory))

        # Check for missing data from FTP sites

        # First check for GO Versions
        if not no_dl and (cron or query_yes_no("Check FTP sites for new GO Version data?")):
            if cron:
                log.info("Checking FTP sites for new GO Versions")
            ftp_go_dates = download.fetch_go_dates()
            missing_go_dates_from_ftp = [(d, f) for d, f in ftp_go_dates.iteritems()
                                         if d not in folder_go and d not in db_go]

            if len(missing_go_dates_from_ftp) > 0:
                log.warn("Missing {0} GO Versions from FTP".format(len(missing_go_dates_from_ftp)))

                if cron or query_yes_no("Download missing GO Versions?"):
                    if cron:
                        log.info("Downloading missing GO Versions")
                    download.download_go_data([x[1] for x in missing_go_dates_from_ftp], resource_directory)

                    files = [f for f in glob.iglob(os.path.join(resource_directory, 'gene_ontology_edit.obo*gz'))]
                    folder_go = search_files_for_go(files)

                    missing_go_dates_from_ftp = [(d, f) for d, f in ftp_go_dates.iteritems()
                                                 if d not in folder_go and d not in db_go]

                    if len(missing_go_dates_from_ftp) > 0:
                        log.error("Still missing {0} GO Versions from FTP, something went wrong with download. "
                                  "Exiting...".format(len(missing_go_dates_from_ftp)))
                        return
            else:
                log.info("No new GO Versions found on FTP site")

        # Now check for GOA Editions
        if not no_dl and (cron or query_yes_no("Check FTP sites for new GOA Edition data?")):
            if cron:
                log.info("Checking FTP sites for new GOA Edition data...")
            ftp_goa_editions = {}
            missing_goa_editions_from_ftp = {}
            for sp, sp_id in sp_map.iteritems():
                log.info("Checking {0}".format(sp))
                dl_fge = download.fetch_goa_editions(sp)
                ftp_goa_editions[sp_id] = dl_fge
                missing = [(x, f) for x, f in dl_fge.iteritems()  if x not in folder_goa.setdefault(sp_id, {}) and
                           x not in db_goa[sp_id]]
                missing_goa_editions_from_ftp[sp_id] = missing
                if len(missing) > 0:
                    log.info("Missing {0} Editions".format(len(missing)))

            count_missing_goa_editions = sum(len(v) for v in missing_goa_editions_from_ftp.itervalues())
            if count_missing_goa_editions > 0:
                log.warn("Missing {0} GOA Editions from FTP".format(count_missing_goa_editions))

                if cron or query_yes_no("Download missing GOA Editions?"):
                    if cron:
                        log.info("Downloading missing GOA Editions")
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
                        log.error("Still missing {0} GOA Editions from FTP, something went wrong with download. "
                                  "Exiting...".format(count_missing_goa_editions))
                        return
            else:
                log.info("No new GOA Editions found on FTP site")

        # Now check mapping files (sec_ac, acindex)
        log.info('Checking for new GOA resource files')
        files = [f for f in glob.iglob(os.path.join(resource_directory, 'sec_ac.txt'))]

        if len(files) == 1:
            sec_ac = files[0]
            log.info("Found sec_ac file ({0}). Ready for update.".format(sec_ac))
        else:
            if not no_dl and (cron or query_yes_no("Could not find sec_ac file, download?")):
                if cron:
                    log.info("Downloading sec_ac file")
                download.download_sec_ac(resource_directory)
                files = [f for f in glob.iglob(os.path.join(resource_directory, 'sec_ac.txt'))]

            if len(files) == 1:
                sec_ac = files[0]
                log.info("Found sec_ac file ({0}). Ready for update.".format(sec_ac))
            else:
                log.warn("No sec_ac file present, update might produce unknown results if the table is not up to date")
                sec_ac = None

        files = [f for f in glob.iglob(os.path.join(resource_directory, 'acindex.txt'))]

        if len(files) == 1:
            acindex = files[0]
            log.info("Found acindex file ({0}). Ready for update.".format(acindex))
        else:
            if not no_dl and (cron or query_yes_no("Could not find acindex file, download?")):
                if cron:
                    log.info("Downloading acindex file")
                download.download_acindex(resource_directory)
                files = [f for f in glob.iglob(os.path.join(resource_directory, 'acindex.txt'))]

            if len(files) == 1:
                acindex = files[0]
                log.info("Found acindex file ({0}). Ready for update.".format(acindex))
            else:
                log.warn("No acindex file present, update might produce unknown results if the table is not up to date")
                acindex = None

        go_new = [(d, f) for d, f in folder_go.iteritems() if d not in db_go]

        if len(go_new) > 0:
            log.info("New GO Versions ready to update: {0}".format(len(go_new)))
        else:
            log.info("There are no new GO Versions available to update")

        goa_new = {sp_id: [(x, f) for x, f in folder_goa.setdefault(sp_id, {}).iteritems() if x not in db_goa[sp_id]]
                   for sp, sp_id in sp_map.iteritems()}
        count_goa_new = sum(len(v) for v in goa_new.itervalues())

        if count_goa_new > 0:
            log.info("New GOA Editions ready to update: {0}".format(count_goa_new))
        else:
            log.info("There are no new GOA Editions available to update")

        # Begin updates
        if len(go_new) > 0 and (cron or query_yes_no("Update GO tables? (Affected tables: '{go_edition}', '{go_term}', "
                                                     "'{go_adjacency}')".format(**gotrack.TABLES))):
            if cron:
                log.info("Updating GO tables")
            for d, f in go_new:
                log.info("Begin: %s", d.strftime('%Y-%m-%d'))
                ont = Ontology.from_file_data(d, f)
                gotrack.update_go_table(ont)

        if count_goa_new > 0 and (cron or query_yes_no("Update GOA tables? (Affected tables: '{edition}', "
                                                       "'{gene_annotation}')".format(**gotrack.TABLES))):
            if cron:
                log.info("Updating GOA tables")
            for sp_id, files in goa_new.iteritems():
                log.info("Begin Species: %s", sp_id)
                for e, f in files:
                    log.info("Edition: %s", e)
                    meta = parsers.retrieve_meta_goa(f)
                    if meta[0] != "2.0" and meta[0] != "2.1":
                        log.warn('Illegal GAF Version: %s', meta[0])
                        continue

                    if meta[1] is None:
                        log.warn('Missing Generated Tag')
                        continue
                    try:
                        generated = datetime.strptime(meta[1], "%Y-%m-%d").date()
                    except ValueError:
                        log.warn('Cannot parse Generated Tag as date: %s', meta[1])
                        continue
                    data = parsers.process_goa(f, sp_id, e)
                    gotrack.update_goa_table(sp_id, e, generated, data)

        if sec_ac is not None and (cron or query_yes_no("Update sec_ac table? (Affected tables: '{sec_ac}')"
                                                        .format(**gotrack.TABLES))):
            if cron:
                log.info("Updating sec_ac table")
            data = parsers.process_sec_ac(sec_ac)
            gotrack.update_sec_ac_table(data, False)

        if acindex is not None and (cron or query_yes_no("Update acindex table? (Affected tables: '{acindex}')"
                                                         .format(**gotrack.TABLES))):
            if cron:
                log.info("Updating acindex table")
            data = parsers.process_acindex(acindex)
            gotrack.update_acindex_table(data, False)

        if cron or force_pp or query_yes_no("Check database for consistency? (Used for pre-processing)"):
            if cron and not force_pp:
                log.info("Checking database for consistency")
            elif force_pp:
                log.info("Forcing database preprocess (skipping consistency check)")
            if not force_pp:
                inconsistent, inc_results = check_database_consistency(gotrack)

            if force_pp or inconsistent:
                if cron or force_pp or query_yes_no("Database requires preprocessing, run now? (Make sure new GO and GOA data has "
                                                    "been imported as well as new sec_ac and acindex files) (Affects tables: "
                                                    "'{pp_genes_staging}', '{pp_goa_staging}', '{pp_accession_staging}', "
                                                    "'{pp_synonym_staging}')"
                                                    .format(**gotrack.TABLES)):
                    if cron and not force_pp:
                        log.info("Database requires preprocessing, running now...")
                    elif force_pp:
                        log.info("Preprocessing database...")

                    if force_pp or inc_results['preprocess_ood']:
                        log.info("Creating current genes tables...")
                        gotrack.pre_process_current_genes_tables()
                        log.info("Creating GOA table...")
                        gotrack.pre_process_goa_table()

                    if force_pp or inc_results['aggregate_ood']:
                        log.info("Creating aggregate tables...")
                        preprocess_aggregates(gotrack)

            else:
                log.info("Database does not require preprocessing.")
        else:
            log.info("Consistency Check Skipped")

    finally:
        cleanup(tmp_directory, cron)


def cleanup(d, cron=False):
    if d is not None and (cron or query_yes_no("Delete temporary folder {0} ?".format(d))):
        shutil.rmtree(d)
    elif d is not None:
        log.info("Temporary Folder: {0}".format(d))


def preprocess_aggregates(gotrack=None):
    """Manually: update pp_edition_aggregates inner join (select species_id, edition, sum(cast( 1/mf as decimal(10,8) ))/gene_count avg_mf from 
        (select species_id, edition, (gene_count - inferred_annotation_count) mf from pp_go_annotation_counts inner join pp_edition_aggregates using(species_id, edition)) as t1 
        inner join pp_edition_aggregates using(species_id, edition) group by species_id, edition) as mftable using (species_id, edition) set avg_multifunctionality=avg_mf;"""
    if gotrack is None:
        gotrack = gtdb.GOTrack(cursorclass=MySQLdb.cursors.SSCursor, **CREDS)
        log.info("Connected to host: {host}, db: {db}, user: {user}".format(**CREDS))

    log.info("Creating aggregate tables...")
    go_ed_to_sp_ed = defaultdict(list)
    current_editions = defaultdict(list)
    for sp_id, ed, goa_date, go_ed, go_date in gotrack.fetch_editions():
        go_ed_to_sp_ed[go_ed].append((sp_id, ed))
        try:
            if ed > current_editions[sp_id][0]:
                current_editions[sp_id] = [ed, go_ed]
        except IndexError, e:
            current_editions[sp_id] = [ed, go_ed]

    # in order to calculate jaccard similarity of terms over time
    # we need a reference edition. I have chosen the msot current edition.
    # Load this data into memory.

    # invert current editions for more memory efficient use of ontologies
    inverted = defaultdict(list)
    for sp_id, ce in current_editions.iteritems():
        inverted[ce[1]] += [[sp_id, ce[0]]]

    log.info("Caching term sets for genes in current editions")
    current_term_set_cache = {}
    for go_ed, eds in inverted.iteritems():
        adjacency_list = gotrack.fetch_adjacency_list(go_ed)
        ont = Ontology.from_adjacency("1900-01-01", adjacency_list)  
        for sp_id, ed in eds:
            log.info("Starting Species (%s), Edition (%s)", sp_id, ed)
            annotation_count, direct_term_set_per_gene_id, term_set_per_gene_id, direct_counts_per_term, gene_id_set_per_term = aggregate_annotations(gotrack, ont, sp_id, ed)
            current_term_set_cache[sp_id] = [direct_term_set_per_gene_id, term_set_per_gene_id]

    gotrack.create_aggregate_staging()

    i = 0
    for go_ed, eds in sorted(go_ed_to_sp_ed.iteritems()):
        i += 1
        log.info("Starting Ontology: %s / %s", i, len(go_ed_to_sp_ed))
        # node_list = gotrack.fetch_term_list(go_ed)
        adjacency_list = gotrack.fetch_adjacency_list(go_ed)
        ont = Ontology.from_adjacency("1900-01-01", adjacency_list)
        j = 0
        for sp_id, ed in eds:
            j += 1
            if j % 25 == 0:
                log.info("Editions: %s / %s", j, len(eds))
            caches = current_term_set_cache[sp_id]
            process_aggregate(gotrack, ont, sp_id, ed, caches) 
        log.info("Editions: %s / %s", j, len(eds))


def push_to_production():
    # Connect to database
    gotrack = gtdb.GOTrack(**CREDS)
    log.info("Connected to host: {host}, db: {db}, user: {user}".format(**CREDS))

    gotrack.push_staging_to_production()

    log.info("Staging area has been pushed to production, a restart of GOTrack is now necessary")
    log.info("Remember to delete temporary old data tables if everything works")

def aggregate_annotations(gotrack, ont, sp_id, ed):
    all_annotations_stream = gotrack.fetch_all_annotations(sp_id, ed)

    # These are for computing the number of genes associated with each go term
    direct_counts_per_term = defaultdict(int)
    gene_id_set_per_term = defaultdict(set)

    # These are for computing the number of go terms associated with each gene
    term_set_per_gene_id = defaultdict(set)
    direct_term_set_per_gene_id = defaultdict(set)

    # Propagation Cache for performance purposes
    ancestor_cache = defaultdict(set)
    annotation_count = 0

    for go_id, gene_id in all_annotations_stream:
        term = ont.get_term(go_id)

        if term is not None:
            annotation_count += 1

            # Deal with direct counts
            direct_counts_per_term[term] += 1

            # Deal with inferred counts
            ancestors = ont.get_ancestors(term, True)


            for anc in ancestors:  # gene counts
                gene_id_set_per_term[anc].add(gene_id)

            term_set_per_gene_id[gene_id].update(ancestors)
            direct_term_set_per_gene_id[gene_id].add(term)


    return annotation_count, direct_term_set_per_gene_id, term_set_per_gene_id, direct_counts_per_term, gene_id_set_per_term



def process_aggregate(gotrack, ont, sp_id, ed, caches):
    direct_term_set_per_gene_id_cache, term_set_per_gene_id_cache = caches
    annotation_count, direct_term_set_per_gene_id, term_set_per_gene_id, direct_counts_per_term, gene_id_set_per_term = aggregate_annotations(gotrack, ont, sp_id, ed)

    # Convert sets into counts
    inferred_counts_per_term = {t: len(s) for t, s in gene_id_set_per_term.iteritems()}

    total_gene_set_size = sum(len(s) for s in gene_id_set_per_term.itervalues())
    total_term_set_size = sum(len(s) for s in term_set_per_gene_id.itervalues())
    gene_count = len(term_set_per_gene_id)

    # write_total_time = time.time()
    
    # Write aggregates
    if gene_count > 0:
        # Calculate average multifunctionality
        avg_mf = 0
        for t, c in inferred_counts_per_term.iteritems():
            if ( c < gene_count ):
                avg_mf += 1.0/(gene_count - c)

        avg_mf = avg_mf / gene_count

        # Calculate average Jaccard similarity to current edition
        sum_direct_jaccard = 0
        sum_jaccard = 0
        # Start with direct terms
        for gene_id, s in direct_term_set_per_gene_id.iteritems():
            cached_s = direct_term_set_per_gene_id_cache[gene_id]
            sum_direct_jaccard += jaccard_similarity(s, cached_s)

        # Now inferred terms
        for gene_id, s in term_set_per_gene_id.iteritems():
            cached_s = term_set_per_gene_id_cache[gene_id]
            sum_jaccard += jaccard_similarity(s, cached_s)

        avg_direct_jaccard = sum_direct_jaccard / gene_count
        avg_jaccard = sum_jaccard / gene_count

        gotrack.write_aggregate(sp_id, ed, gene_count, annotation_count / gene_count,
                                total_term_set_size / gene_count, total_gene_set_size / len(gene_id_set_per_term), avg_mf, avg_direct_jaccard, avg_jaccard)
    else:
        log.warn("No Genes in Species ({0}), Edition ({1})".format(sp_id, ed))
    
    # Write Term Counts
    if total_gene_set_size > 0 or total_term_set_size > 0:
        gotrack.write_term_counts(sp_id, ed, direct_counts_per_term, inferred_counts_per_term)
    else:
        log.warn("No Annotations in Species ({0}), Edition ({1})".format(sp_id, ed))

    # write_total_time = time.time() - write_total_time

    # print "Total Write Time: {0}".format(write_total_time)

    # total_time = time.time() - total_time
    # print ""
    # print "Total Time: {0}".format(total_time)
    # print "Ancestor %: {0}".format(100 * ancestor_total_time / total_time)
    # print "Write %: {0}".format(100 * write_total_time / total_time)

def jaccard_similarity(s1, s2):
    if s1==None or s2 == None:
        return None
    if len(s1) == 0 and len(s2) == 0:
        return 1.0
    if len(s1) == 0 or len(s2) == 0:
        return 0.0

    return len(s1.intersection(s2)) / len(s1.union(s2))

def check_database_consistency(gotrack, verbose=True):
    results = gotrack.fetch_consistency()
    if verbose:
        if results['preprocess_ood']:
            log.warn('Pre-processed tables are inconsistent with current data, requires updating.')
        if results['aggregate_ood']:
            log.warn('Aggregate tables are inconsistent with current data, requires updating.')
        if results['unlinked_editions']:
            log.warn('The following GOA editions are unlinked with GO ontologies')
            log.warn(results['unlinked_editions'])
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
                log.warn('Cannot parse date: %s', fname)
                continue
            except Exception as inst:
                log.warn('Something went wrong %s %s', fname, inst)
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
            log.warn('Cannot parse filename: %s %s', fname, inst)
            continue
        files_in_species = species_files[sp]
        files_in_species.append((edition, f))

    return species_files

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description='Update GOTrack Database')
    parser.add_argument('resource_directory', metavar='d', type=str, nargs='?', default=None,
                        help='a folder holding resources to use in the update')

    group = parser.add_mutually_exclusive_group(required=True)

    group.add_argument('--push', dest='push', action='store_true',
                        help='Push Staging to Production, does not insert data')

    group.add_argument('--meta', dest='meta', action='store_true',
                        help='Display Connection and Table Information')

    group.add_argument('--update', dest='update', action='store_true',
                        help='Runs Update with options')

    group.add_argument('--update-push', dest='update_push', action='store_true',
                        help='Runs Update with options followed by update with --push')

    group.add_argument('--aggregate', dest='aggregate', action='store_true',
                        help='Updates the aggregate tables only.')

    parser.add_argument('--cron', dest='cron', action='store_true',
                        help='No interactivity mode')
    parser.add_argument('--no-downloads', dest='dl', action='store_true',
                        help='Prevent all downloads')
    parser.add_argument('--force-pp', dest='force_pp', action='store_true',
                        help='Force preprocessing of database (regardless of need)')



    args = parser.parse_args()
    if args.meta:
        # Push Staging to Production
        log.info("Host: {host}, db: {db}, user: {user}".format(**CREDS))
        log.info('Tables: \n%s', "\n".join(sorted([str(x) for x in gtdb.GOTrack.TABLES.iteritems()])))
    elif args.update_push:
        # Update followed by push to production
        main(args.resource_directory, args.cron, args.dl, args.force_pp)
        if args.cron or query_yes_no("Push staging to production?"):
            push_to_production()
    elif args.push:
        # Push Staging to Production
        push_to_production()
    elif args.aggregate:
        # Update aggregate tables
        preprocess_aggregates()
    elif args.update:
        # Update database
        main(args.resource_directory, args.cron, args.dl, args.force_pp)
    else:
        log.error("No goal supplied.")



