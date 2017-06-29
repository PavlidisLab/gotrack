__author__ = 'mjacobson'

"""
Contains functionality related to connecting, retrieving information and inserting information to the GOTrack
database.

############################################################
TABLE DEFINITIONS

CREATE TABLE `annotation` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `accession_id` int(11) unsigned NOT NULL,
  `qualifier` varchar(255) NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `reference` varchar(255) NOT NULL,
  `evidence` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acc_go_ev_ref_qual` (`accession_id`, `go_id`, `evidence`, `reference`, `qualifier`),
  CONSTRAINT `annotation_idfk` FOREIGN KEY (`accession_id`) REFERENCES `accession` (`id`)
)

CREATE TABLE `accession` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `edition` int(11) NOT NULL,
  `accession` varchar(10) DEFAULT NULL,
  `symbol` varchar(255) NOT NULL,
  `db` varchar(255) NOT NULL,
  `db_object_id` varchar(255) NOT NULL,
  `db_object_name` text NOT NULL,
  `db_object_type` varchar(255) NOT NULL,
  `subset` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sp_ed_dbi` (`species_id`,`edition`,`db_object_id`),
  #CONSTRAINT `accession_edfk` FOREIGN KEY (`accession_id`) REFERENCES `accession` (`id`),
  #CONSTRAINT `accession_spfk` FOREIGN KEY (`accession_id`) REFERENCES `accession` (`id`)
)

CREATE TABLE `synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `accession_id` int(11) unsigned NOT NULL,
  `synonym` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acc_syn` (`accession_id`,`synonym`),
  CONSTRAINT `synonyms_ibfk_1` FOREIGN KEY (`accession_id`) REFERENCES `accession` (`id`)
)

CREATE TABLE `edition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `edition` int(11) NOT NULL,
  `species_id` int(11) NOT NULL,
  `date` date NOT NULL,
  `go_edition_id_fk` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `spec_ed` (`species_id`,`edition`)
)

CREATE TABLE `sec_ac` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sec` varchar(10) NOT NULL,
  `ac` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ac` (`ac`,`sec`)
)

CREATE TABLE `go_adjacency` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `go_edition_id_fk` int(10) unsigned NOT NULL,
  `child` varchar(10) NOT NULL,
  `parent` varchar(10) NOT NULL,
  `relationship` enum('IS_A','PART_OF') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `go_edition_id_fk` (`go_edition_id_fk`,`child`,`parent`,`relationship`)
)

CREATE TABLE `go_alternate` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `go_edition_id_fk` bigint(20) unsigned NOT NULL,
  `alt` varchar(10) NOT NULL,
  `primary` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ed_alt` (`go_edition_id_fk`,`alt`),
  KEY `ed_alt_primary` (`go_edition_id_fk`,`alt`,`primary`),
  KEY `alt_primary` (`alt`,`primary`),
  CONSTRAINT `fk_go_alt_go_edition` FOREIGN KEY (`go_edition_id_fk`) REFERENCES `go_edition` (`id`)
)

CREATE TABLE `go_definition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `go_id` varchar(10) NOT NULL,
  `definition` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `go_id` (`go_id`)
)

CREATE TABLE `go_edition` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `date` (`date`)
)

CREATE TABLE `go_term` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `go_edition_id_fk` bigint(20) unsigned NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `name` text NOT NULL,
  `aspect` enum('CC','BP','MF') DEFAULT NULL,
  `is_obsolete` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `go_ed_id_go_id` (`go_edition_id_fk`,`go_id`),
  KEY `go_id` (`go_id`),
  CONSTRAINT `fk_go_term_go_edition` FOREIGN KEY (`go_edition_id_fk`) REFERENCES `go_edition` (`id`)
)

Pre-processed

CREATE TABLE `pp_accession_history` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `accession_id` int(11) unsigned NOT NULL,
  `secondary_accession_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acc_acc` (`accession_id`,`secondary_accession_id`),
  CONSTRAINT `acc_hist_acfk` FOREIGN KEY (`accession_id`) REFERENCES `accession` (`id`),
  CONSTRAINT `acc_hist_acfk2` FOREIGN KEY (`secondary_accession_id`) REFERENCES `accession` (`id`)
)

CREATE TABLE `pp_current_edition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `edition` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sp` (`species_id`),
  KEY `sp_ed` (`species_id`, `edition`),
  CONSTRAINT `ppcured_fk` FOREIGN KEY (`species_id`, `edition`) REFERENCES `edition` (`species_id`, `edition`)
)

CREATE TABLE `pp_edition_aggregates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `edition` int(11) NOT NULL,
  `gene_count` int(11) NOT NULL,
  `avg_direct_terms_for_gene` double NOT NULL,
  `avg_inferred_terms_for_gene` double NOT NULL,
  `avg_inferred_genes_for_term` double NOT NULL,
  `avg_multifunctionality` double NOT NULL,
  `avg_direct_jaccard` double NOT NULL,
  `avg_inferred_jaccard` double NOT NULL,
  PRIMARY KEY (`id`)
)

CREATE TABLE `pp_go_annotation_counts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `edition` int(11) NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `direct_annotation_count` int(11) DEFAULT NULL,
  `inferred_annotation_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sp_ed_go` (`species_id`,`edition`,`go_id`)
)

CREATE TABLE `track_popular_genes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `accession` varchar(255) NOT NULL,
  `symbol` varchar(255) NOT NULL,
  `count` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acc` (`accession`)
)

##################################################################
"""

import MySQLdb
import _mysql
import logging
import time
import warnings

from utility import grouper

warnings.filterwarnings("ignore", "Unknown table.*")

log = logging.getLogger(__name__)


class GOTrack:

    def __init__(self, tables=None, concurrent_insertions=1000, verbose=False, **kwargs):
        self.creds = kwargs
        self.tables = {'go_edition':    'go_edition',
                       'edition':       'edition',
                       'species':       'species',
                       'go_term':       'go_term',
                       'go_adjacency':  'go_adjacency',
                       'go_alternate':  'go_alternate',
                       'accession':     'accession',
                       'synonyms':      'synonyms',
                       'annotation':    'annotation',
                       'sec_ac':        'sec_ac',
                       'go_definition': 'go_definition',

                       'pp_current_edition':       'pp_current_edition',
                       'pp_accession_history':     'pp_accession_history',
                       'pp_edition_aggregates':    'pp_edition_aggregates',
                       'pp_go_annotation_counts':  'pp_go_annotation_counts',

                       'staging_pre':  'staging_',
                       'previous_pre':      'previous_'
                       }
        self.staging_tables = ['pp_current_edition',
                               'pp_accession_history',
                               'pp_edition_aggregates',
                               'pp_go_annotation_counts']
        if tables:
            self.table = tables
        self.concurrent_insertions = concurrent_insertions
        self.verbose = verbose
        try:
            self.con = MySQLdb.connect(**self.creds)
        except Exception, e:
            log.error("Problem with database connection, %s", e)
            raise

    def test_and_reconnect(self):
        try:
            self.con.ping(True)
        except MySQLdb.OperationalError:
            log.warn('Reconnecting to DB...')
            self.con = MySQLdb.connect(**self.creds)
        return self.con

    def reconnect(func):
        """Wrap a function in order to test MySQL connection
        and reconnect if necessary.
        """
        def wrapper(self, *args, **kwargs):
            try:
                self.con = self.test_and_reconnect()
            except _mysql.Error, e:
                log.error("Problem with database connection, %s", e)
                raise
            return func(*args, **kwargs)

        # Tidy up the help()-visible docstrings to be nice
        wrapper.__name__ = func.__name__
        wrapper.__doc__ = func.__doc__

        return wrapper

    def transactional(func):
        """Wrap a function in a SQL transaction as well as
        reconnecting if necessary. Functions wrapped require
        a cursor kwarg which supplies the db connection cursor.
        """
        def wrapper(self, *args, **kwargs):
            try:
                self.con = self.test_and_reconnect()
            except _mysql.Error, e:
                log.error("Problem with database connection, %s", e)
                raise
            with self.con as cursor:
                try:
                    retval = func(self, *args, cursor=cursor, **kwargs)
                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', repr(inst))
                    self.con.rollback()
                    raise
                finally:
                    if cursor:
                        cursor.close()

            return retval

        # Tidy up the help()-visible docstrings to be nice
        wrapper.__name__ = func.__name__
        wrapper.__doc__ = func.__doc__

        return wrapper

    def stream(func):
        """Wrap a function in a SQL transaction as well as
        reconnecting if necessary. Functions wrapped require
        a cursor kwarg which supplies the db connection cursor.

        Must be used if wrapped function is a generator.
        """
        def wrapper(self, *args, **kwargs):
            try:
                self.con = self.test_and_reconnect()
            except _mysql.Error, e:
                log.error("Problem with database connection, %s", e)
                raise
            with self.con as cursor:
                try:
                    for x in func(self, *args, cursor=cursor, **kwargs):
                        yield x
                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', repr(inst))
                    self.con.rollback()
                    raise
                finally:
                    if cursor:
                        cursor.close()

        # Tidy up the help()-visible docstrings to be nice
        wrapper.__name__ = func.__name__
        wrapper.__doc__ = func.__doc__

        return wrapper

    def insert_many(self, table, columns, data, cur, ignore=False):
        value_length = len(columns)

        # It is actually extremely important that 'values' be lowercase. DO NOT CHANGE.
        # See http://stackoverflow.com/a/3945860/4907830.
        sql_template = "INSERT %sINTO {0}({1}) values {2}" % ("IGNORE " if ignore else "")

        values_template = "(" + ",".join(["%s"] * value_length) + ")"

        sql = sql_template.format(table, ",".join(columns), values_template)

        cnt = 0
        start = time.time()
        for row_list in grouper(self.concurrent_insertions, data):
            cnt += len(row_list)
            if self.verbose:
                log.info(cnt)
            cur.executemany(sql, row_list)

        if self.verbose:
            log.info("Row Count: %s, Time: %s", cnt, time.time() - start)

        return cnt

    @transactional
    def fetch_current_state(self, cursor=None):
        results = {}
        cursor.execute("SELECT id, short_name FROM {species}".format(**self.tables))
        species = [(x[0], x[1]) for x in cursor.fetchall()]
        results['species'] = species

        editions = {}
        for sp_id, sp in species:
            editions[sp_id] = []
        cursor.execute("SELECT distinct species_id, edition from {edition}".format(**self.tables))
        for r in cursor.fetchall():
            editions[r[0]].append(r[1])
        results['editions'] = editions

        cursor.execute("SELECT distinct id, date from {go_edition}".format(**self.tables))
        go_editions = cursor.fetchall()
        results['go_editions'] = go_editions

        return results

    @transactional
    def fetch_editions(self, cursor=None):
        sql = "select species_id, edition, ed.date goa_date, go_edition_id_fk, ged.date go_date from {edition} ed " \
              "inner join {go_edition} ged on ed.go_edition_id_fk = ged.id"
        cursor.execute(sql.format(**self.tables))
        results = cursor.fetchall()
        return results

    @transactional
    def fetch_staged_current_editions(self, cursor=None):
        sql = "select species_id, edition, ed.date goa_date, go_edition_id_fk, ged.date go_date from {edition} ed " \
              "inner join {staging_pre}{pp_current_edition} using(species_id, edition) " \
              "inner join {go_edition} ged on ed.go_edition_id_fk = ged.id"
        cursor.execute(sql.format(**self.tables))
        results = cursor.fetchall()
        return results

    @stream
    def stream_adjacency_list(self, go_ed, cursor=None):
        sql = "select child, parent, relationship from {go_adjacency} where go_edition_id_fk = %s"
        cursor.execute(sql.format(**self.tables), [go_ed])
        for row in cursor:
            yield row

    @stream
    def stream_staged_annotations(self, sp_id, ed, cursor=None):
        sql = "select distinct annot.go_id, ppah.accession_id from {annotation} annot " \
              "inner join {staging_pre}{pp_accession_history} ppah on ppah.secondary_accession_id=annot.accession_id " \
              "inner join {accession} acc on acc.id=ppah.secondary_accession_id " \
              "where acc.species_id = %s and acc.edition = %s"
        cursor.execute(sql.format(**self.tables), [sp_id, ed])
        for row in cursor:
            yield row

    def _insert_new_go_edition(self, cursor, date):
        log.info("Insert new GO Edition")
        cols = ["date"]
        data = [[date.strftime('%Y-%m-%d')]]
        self.insert_many(self.tables['go_edition'], cols, data, cursor)
        return self.con.insert_id()

    def _insert_term_nodes(self, cursor, go_edition_id, ont):
        log.info("Inserting Term Nodes")
        cols = ["go_edition_id_fk", "go_id", "name", "aspect", "is_obsolete"]
        data = ((go_edition_id,) + x for x in ont.list_terms())
        self.insert_many(self.tables['go_term'], cols, data, cursor)

    def _insert_term_adjacencies(self, cursor, go_edition_id, ont):
        log.info("Inserting Adjacency Table")
        cols = ["go_edition_id_fk", "child", "parent", "relationship"]
        data = ((go_edition_id,) + x for x in ont.adjacency_list())
        self.insert_many(self.tables['go_adjacency'], cols, data, cursor)

    def _insert_term_alternates(self, cursor, go_edition_id, ont):
        log.info("Inserting Alternate Table")
        cols = ["go_edition_id_fk", "alt", "primary"]
        data = ((go_edition_id,) + x for x in ont.alternate_list())
        self.insert_many(self.tables['go_alternate'], cols, data, cursor)

    @transactional
    def update_go_tables(self, ont, cursor=None):
        # Insert new edition into go_edition table and retrieve insertion primary key
        go_edition_id = self._insert_new_go_edition(cursor, ont.date)

        # Insert meta data for the meta data of individual GO nodes
        self._insert_term_nodes(cursor, go_edition_id, ont)

        # Insert adjacency table
        self._insert_term_adjacencies(cursor, go_edition_id, ont)

        # Insert alternate table
        self._insert_term_alternates(cursor, go_edition_id, ont)

    @transactional
    def update_current_go_definitions(self, ont, cursor=None):
        cols = ["go_id", "definition"]
        data = ont.list_definitions()
        self.insert_many(self.tables['go_definition'], cols, data, cursor)

    def _fetch_closest_go_edition_for_date(self, cursor, date):
        sql = "select id, date from {go_edition} where date <= %s order by date DESC LIMIT 1".format(**self.tables)
        cursor.execute(sql, [date.strftime('%Y-%m-%d')])
        go_edition_id_fk, go_edition_date = cursor.fetchone()
        cursor.nextset()
        return go_edition_id_fk, go_edition_date

    def _fetch_or_create_goa_release_for_date(self, cursor, date):
        sql = "select distinct goa_release from {edition} where date between %s - INTERVAL 1 DAY and " \
              "%s + INTERVAL 1 DAY order by goa_release ASC LIMIT 1;".format(**self.tables)
        cursor.execute(sql, [date.strftime('%Y-%m-%d'), date.strftime('%Y-%m-%d')])
        goa_release = cursor.fetchone()
        cursor.nextset()
        if not goa_release:
            sql = "select max(goa_release) + 1 from {edition}".format(**self.tables)
            cursor.execute(sql)
            goa_release = cursor.fetchone()
            cursor.nextset()
            log.info("Creating new GOA release: %s", goa_release)
        return goa_release

    def _insert_new_edition(self, cursor, edition_id, species_id, date, goa_release, go_edition_id):
        log.info("Insert new edition")
        cols = ["edition", "species_id", "date", "goa_release", "go_edition_id_fk"]
        data = [[edition_id, species_id, date.strftime('%Y-%m-%d'), goa_release, go_edition_id]]
        self.insert_many(self.tables['edition'], cols, data, cursor)

    def _insert_gpi_to_accession(self, cursor, data):
        cols = ["species_id", "edition", "db", "accession", "db_object_id", "symbol", "db_object_name",
                "db_object_type", "subset"]
        self.insert_many(self.tables['accession'], cols, data, cursor)

    def _retrieve_id_map_for_edition(self, species_id, edition_id, cursor):
        cursor.execute("SELECT id, db, db_object_id  FROM {accession} where species_id={species_id} "
                       "and edition={edition_id}".format(species_id=species_id, edition_id=edition_id, **self.tables))
        id_map = {(db, db_object_id): acc_id for acc_id, db, db_object_id in cursor.fetchall()}

        return id_map

    def _insert_synonyms(self, cursor, data):
        cols = ["accession_id", "synonym"]
        self.insert_many(self.tables['synonyms'], cols, data, cursor, ignore=True)

    def _insert_gpa_to_annotation(self, cursor, data):
        cols = ["accession_id", "qualifier", "go_id", "reference", "evidence"]
        self.insert_many(self.tables['annotation'], cols, data, cursor, ignore=True)

    @transactional
    def insert_annotations(self, species_id, edition_id, date, gpi_data, gpa_data, cursor=None):
        go_edition_id, go_edition_date = self._fetch_closest_go_edition_for_date(cursor, date)

        if go_edition_id is None:
            log.error("Failed to link date: %s to a GO Release.", date.strftime('%Y-%m-%d'))
            raise ValueError
        else:
            log.info("Linked date: %s to GO Release: %s", date.strftime('%Y-%m-%d'), go_edition_date)

        # See if another edition from this release is in the database.
        # We determine this by looking for other editions that were released within
        # one day of this (pick the smallest if more than one). If this release
        # doesn't yet exist, create a new one by incrementing the highest existing edition.
        goa_release = self._fetch_or_create_goa_release_for_date(cursor, date)

        self._insert_new_edition(cursor, edition_id, species_id, date, goa_release, go_edition_id)

        synonyms = []

        def accession_generator(data_generator):
            """creates generator for accession table, also stores synonyms for later use"""
            for row in data_generator:
                yield (species_id, edition_id) + row[:-1]
                synonyms.append((row[0], row[2], row[7]))

        log.debug('Insert GPI information to `accession`')
        self._insert_gpi_to_accession(cursor, accession_generator(gpi_data))

        # Retrieve id, db, db_object_id for newly inserted edition from DB
        log.debug('Retrieve id, db, db_object_id for newly inserted edition from DB')
        id_map = self._retrieve_id_map_for_edition(species_id, edition_id, cursor)
        log.info("ID Map Size: %s", len(id_map))

        # Insert synonyms
        log.debug('Insert synonyms')

        def flatten_and_id_synonyms(synonyms_rows):
            for row in synonyms_rows:
                accession_id = id_map[(row[0], row[1])]
                for synonym in row[2]:
                    yield (accession_id, synonym)

        self._insert_synonyms(cursor, flatten_and_id_synonyms(synonyms))

        # Insert annotations
        log.debug('Insert annotations')

        def map_gpa_data(gpa):
            for row in gpa:
                try:
                    yield (id_map[(row[0], row[1])],) + row[2:]
                except KeyError:
                    log.warn("Could not find %s in GPI file, skipping annotation", (row[0], row[1]))

        self._insert_gpa_to_annotation(cursor, map_gpa_data(gpa_data))

    # Pre-process section

    @transactional
    def requires_proprocessing(self, cursor=None):
        requires_proprocessing = False
        cursor.execute("select species_id, max(edition) edition from {edition} "
                       "group by species_id".format(**self.tables))
        edition_max = {x[0]: x[1] for x in cursor.fetchall()}

        cursor.execute("select species_id, edition from {pp_current_edition}".format(**self.tables))
        for r in cursor.fetchall():
            # If returning early remember to move the cursor to the next set
            requires_proprocessing = requires_proprocessing or (r[1] != edition_max[r[0]])

        return requires_proprocessing

    @transactional
    def update_secondary_accession_table(self, data, cursor=None):
        sql = "DROP TABLE IF EXISTS {staging_pre}{sec_ac}".format(**self.tables)
        cursor.execute(sql)

        sql = "CREATE TABLE {staging_pre}{sec_ac} like {sec_ac}".format(**self.tables)
        cursor.execute(sql)

        cols = ["sec", "ac"]
        cnt = self.insert_many("{staging_pre}{sec_ac}".format(**self.tables), cols, data, cursor)

        if cnt == 0:
            raise (ValueError("secondary accession file is either empty or malformed, code might need to be altered to "
                              "deal with a new file structure."))

        # Reflexive associations
        sql = "INSERT INTO {staging_pre}{sec_ac}(ac, sec) select distinct ac, ac from {staging_pre}{sec_ac}"\
            .format(**self.tables)
        cursor.execute(sql)

        sql = "DROP TABLE IF EXISTS {sec_ac}".format(**self.tables)
        cursor.execute(sql)

        sql = "RENAME TABLE {staging_pre}{sec_ac} TO {sec_ac}".format(**self.tables)
        cursor.execute(sql)

    @transactional
    def create_staging_tables(self, cursor=None):
        sql_template_drop = "DROP TABLE IF EXISTS {staging_pre}{%s}"
        sql_template_create = "CREATE TABLE {staging_pre}{%s} LIKE {%s}"
        for table in self.staging_tables:
            sql_drop = sql_template_drop % table
            sql_create = sql_template_create % (table, table)
            cursor.execute(sql_drop.format(**self.tables))
            cursor.execute(sql_create.format(**self.tables))

        # Add missing foreign keys
        sql_fk = "alter table {staging_pre}{pp_current_edition} add foreign key " \
                 "ppcured_fk (species_id, edition) references {edition}(species_id, edition)"
        cursor.execute(sql_fk.format(**self.tables))

        sql_fk = "alter table {staging_pre}{pp_accession_history} add foreign key " \
                 "acc_hist_acfk (accession_id) references {accession}(id)"
        cursor.execute(sql_fk.format(**self.tables))

        sql_fk = "alter table {staging_pre}{pp_accession_history} add foreign key " \
                 "acc_hist_acfk2 (secondary_accession_id) references {accession}(id)"
        cursor.execute(sql_fk.format(**self.tables))

    @transactional
    def push_staging_tables(self, cursor=None):
        # Check if all staging tables exist
        log.info("Checking to see if all staging tables exist")
        sql_template_exist = "SELECT 1 FROM {staging_pre}{%s} LIMIT 1"
        for table in self.staging_tables:
            sql_exist = sql_template_exist % table
            cursor.execute(sql_exist.format(**self.tables))
            cursor.fetchall()

        log.info("Pushing all staging tables")
        sql_template_drop = "DROP TABLE IF EXISTS {previous_pre}{%s}"
        sql_template_swap = "rename table {%s} TO {previous_pre}{%s}, {staging_pre}{%s} to {%s}"
        for table in self.staging_tables:
            sql_drop = sql_template_drop % table
            sql_swap = sql_template_swap % (table,) * 4
            cursor.execute(sql_drop.format(**self.tables))
            cursor.execute(sql_swap.format(**self.tables))

    @transactional
    def stage_current_editions(self, cursor=None):
        sql = "insert into {staging_pre}{pp_current_edition} (species_id, edition) select species_id, " \
              "max(edition) edition from {edition} group by species_id"
        cursor.execute(sql.format(**self.tables))

    @transactional
    def stage_accession_history_table(self, cursor=None):
        # Note, keep in mind this uses the staged version of {pp_current_edition}

        sql = "insert into {staging_pre}{pp_accession_history} (accession_id, secondary_accession_id) " \
              "select acc1.id, acc2.id from {accession} acc1 " \
              "inner join {staging_pre}{pp_current_edition} using (species_id, edition) " \
              "inner join {sec_ac} sec_ac on acc1.db_object_id=sec_ac.ac " \
              "inner join {accession} acc2 on acc2.db_object_id=sec_ac.sec"

        cursor.execute(sql.format(**self.tables))

        # Missing those accession which have no secondary accessions
        sql = "insert into {staging_pre}{pp_accession_history} (accession_id, secondary_accession_id) " \
              "select acc1.id, acc2.id from {accession} acc1 " \
              "inner join {staging_pre}{pp_current_edition} using (species_id, edition) " \
              "left join {sec_ac} sec_ac on acc1.db_object_id=sec_ac.ac " \
              "inner join {accession} acc2 on acc2.db_object_id=acc1.db_object_id " \
              "where sec_ac.ac is null"

        cursor.execute(sql.format(**self.tables))

    @transactional
    def stage_edition_aggregates(self, data, cursor=None):
        cols = ["species_id", "edition", "gene_count", "avg_direct_terms_for_gene", "avg_inferred_terms_for_gene",
                "avg_inferred_genes_for_term", "avg_multifunctionality", "avg_direct_jaccard", "avg_inferred_jaccard"]
        self.insert_many("{staging_pre}{pp_edition_aggregates}".format(**self.tables), cols, data, cursor)

    @transactional
    def stage_term_counts(self, sp_id, ed, d_map, i_map, cursor=None):
        cols = ["species_id", "edition", "go_id", "direct_annotation_count", "inferred_annotation_count"]
        data = ((sp_id, ed, term.id, d_map[term] if term in d_map else None, i_map[term] if term in i_map else None) for term in (d_map.viewkeys() | i_map.keys()))
        self.insert_many("{staging_pre}{pp_go_annotation_counts}".format(**self.tables), cols, data, cursor)
