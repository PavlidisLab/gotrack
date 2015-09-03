__author__ = 'mjacobson'

"""
Contains functionality related to connecting, retrieving information and inserting information to the GOTrack
database.
"""

import _mysql
import MySQLdb
import time


from utility import grouper, Log, max_default
import warnings
warnings.filterwarnings("ignore", "Unknown table.*")

log = Log()


class GOTrack:

    TABLES = {'go_term': "go_term",
              'go_adjacency': "go_adjacency",
              'gene_annotation': "gene_annotation",
              'edition': "edition",
              'species': "species",
              'go_edition': "go_edition",
              'sec_ac': "sec_ac",
              'acindex': "acindex",
              'current_genes': "pp_current_genes",
              'goa': "pp_goa",
              'accession': "pp_primary_accessions",
              'synonym': "pp_synonyms",
              'go_annotation_counts': 'go_annotation_counts',
              'new': "_tmp_newdata",  # tmp table creation suffix
              'old': "_tmp_olddata"  # tmp table creation suffix
              }

    CONCURRENT_INSERTIONS = 1000

    def __init__(self, **kwargs):
        self.creds = kwargs
        try:
            self.con = MySQLdb.connect(**self.creds)
        except Exception, e:
            log.error("Problem with database connection", e)
            raise

    def test_and_reconnect(self):
        try:
            self.con.ping(True)
        except MySQLdb.OperationalError:
            log.warn('Reconnecting to DB...')
            self.con = MySQLdb.connect(**self.creds)
        return self.con

    def fetch_consistency(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    results = {}
                    cur.execute("SELECT id, short_name FROM {species}".format(**GOTrack.TABLES))
                    species = [(x[0], x[1]) for x in cur.fetchall()]

                    editions = {}
                    for sp_id, sp in species:
                        editions[sp_id] = []
                    cur.execute("SELECT distinct species_id, edition from {edition}".format(**GOTrack.TABLES))
                    for r in cur.fetchall():
                        editions[r[0]].append(r[1])

                    db_state = tuple((sp_id, max(eds)) for sp_id, eds in editions.iteritems() if len(eds) > 0)

                    # Checks to see if pre-processed data is out of date. Specifically, if max edition present in
                    # the pre-processed data is the same as the max edition inserted in the database for all species
                    cur.execute("SELECT species_id, max(edition) from {goa} goa inner join {current_genes} cg on "
                                "goa.pp_current_genes_id=cg.id group by species_id"
                                .format(**GOTrack.TABLES))
                    pp_state = cur.fetchall()
                    results['preprocess_ood'] = pp_state != db_state

                    # Checks to see if the aggregate data is out of date with the database. If not then a server
                    # restart is required with suitable options for recreation of aggregates.
                    cur.execute("SELECT species_id, max(edition) from {go_annotation_counts} group by species_id"
                                .format(**GOTrack.TABLES))
                    pp_state = cur.fetchall()
                    results['aggregate_ood'] = pp_state != db_state

                    # Checks to see if all GOA editions have been linked to a GO Edition
                    cur.execute("SELECT species_id, edition from {edition} where go_edition_id_fk is NULL"
                                .format(**GOTrack.TABLES))
                    unlinked_editions = cur.fetchall()
                    results['unlinked_editions'] = unlinked_editions

                    return results
                except Exception as inst:
                    log.error('Error rolling back', inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def fetch_current_state(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    results = {}
                    cur.execute("SELECT id, short_name FROM {species}".format(**GOTrack.TABLES))
                    species = [(x[0], x[1]) for x in cur.fetchall()]
                    results['species'] = species

                    editions = {}
                    for sp_id, sp in species:
                        editions[sp_id] = []
                    cur.execute("SELECT distinct species_id, edition from {edition}".format(**GOTrack.TABLES))
                    for r in cur.fetchall():
                        editions[r[0]].append(r[1])
                    results['editions'] = editions

                    cur.execute("SELECT distinct id, date from {go_edition}".format(**GOTrack.TABLES))
                    go_editions = cur.fetchall()
                    results['go_editions'] = go_editions

                    return results
                except Exception as inst:
                    log.error('Error rolling back', inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def update_sec_ac_table(self, data, verbose=False):

        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    sql = "CREATE TABLE {sec_ac}{new} like {sec_ac}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    log.info("Inserting secondary accessions table")
                    cnt = self.insert_multiple(GOTrack.TABLES['sec_ac'] + GOTrack.TABLES['new'], ["sec", "ac"], data,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, verbose)

                    if cnt == 0:
                        raise (ValueError("sec_ac.txt is either empty or malformed, code might need to be altered to "
                                          "deal with a new file structure."))

                    sql = "INSERT INTO {sec_ac}{new}(ac, sec) select distinct ac, ac from {sec_ac}{new}"\
                        .format(**GOTrack.TABLES)
                    cur.execute(sql)  # Reflexive associations

                    sql = "RENAME TABLE {sec_ac} TO {sec_ac}{old}, {sec_ac}{new} to {sec_ac}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back', inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.execute("DROP TABLE IF EXISTS {sec_ac}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {sec_ac}{new}".format(**GOTrack.TABLES))
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def update_acindex_table(self, data, verbose=False):

        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    cur.execute("CREATE TABLE {acindex}{new} like {acindex}".format(**GOTrack.TABLES))

                    log.info("Inserting Swiss-Prot Accession Index Table")
                    cnt = self.insert_multiple(GOTrack.TABLES['acindex'] + GOTrack.TABLES['new'],
                                               ["accession", "symbol"], data, GOTrack.CONCURRENT_INSERTIONS, cur,
                                               verbose)

                    if cnt == 0:
                        raise (ValueError("acindex.txt is either empty or malformed, code might need to be altered to "
                                          "deal with a new file structure."))

                    cur.execute("RENAME TABLE {acindex} TO {acindex}{old}, {acindex}{new} to {acindex}"
                                .format(**GOTrack.TABLES))

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back', inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.execute("DROP TABLE IF EXISTS {acindex}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {acindex}{new}".format(**GOTrack.TABLES))
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def update_go_table(self, ont):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                try:
                    # Insert new edition into go_edition table and retrieve insertion primary key
                    log.info("Inserting New GO Edition")
                    self.insert_multiple(GOTrack.TABLES['go_edition'], ["date"],  [[ont.date.strftime('%Y-%m-%d')]],
                                         1, cur, False)
                    go_edition_id = self.con.insert_id()

                    # Insert meta data for the meta data of individual GO nodes
                    log.info("Inserting Term Nodes")
                    data_generator = ((go_edition_id,) + x for x in ont.list_terms())
                    self.insert_multiple(GOTrack.TABLES['go_term'],
                                         ["go_edition_id_fk", "go_id", "name", "aspect", "is_obsolete"],
                                         data_generator, self.CONCURRENT_INSERTIONS, cur, False)

                    # Insert reflexive transitive closure
                    # print "Inserting Reflexive Transitive Closure Table"
                    # data_generator = transitiveClosure(termMap)
                    # insert_multiple(cur, "go_ontology_tclosure",
                    # ["go_edition_id_fk", "child", "parent", "relationship","min_distance"] ,
                    # data_generator, concurrent_insertions, False)

                    # Insert adjacency table
                    log.info("Inserting Adjacency Table")
                    data_generator = ((go_edition_id,) + x for x in ont.adjacency_list())
                    self.insert_multiple(GOTrack.TABLES['go_adjacency'],
                                         ["go_edition_id_fk", "child", "parent", "relationship"], data_generator,
                                         self.CONCURRENT_INSERTIONS, cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back', ont.date.strftime('%Y-%m-%d'), inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def update_goa_table(self, sp_id, ed, date, data):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                try:

                    log.info("Attempting to link GOA Edition to GO Version")
                    sql = "select id, date from {go_edition} where date <= %s order by date DESC LIMIT 1".format(
                        **GOTrack.TABLES)
                    cur.execute(sql, date.strftime('%Y-%m-%d'))
                    go_edition_id_fk = cur.fetchone()

                    if go_edition_id_fk is None:
                        log.warn("Failed to link date: {0} to a GO Release. Setting as NULL. FIX MANUALLY."
                                 .format(date.strftime('%Y-%m-%d')))
                    else:

                        log.info("Linked date: {0} to GO Release: {1}".format(date.strftime('%Y-%m-%d'),
                                 go_edition_id_fk[1]))

                    log.info("Insert new edition")
                    self.insert_multiple(GOTrack.TABLES['edition'],
                                         ["edition", "species_id", "date", "go_edition_id_fk"],
                                         [[ed, sp_id, date.strftime('%Y-%m-%d'), go_edition_id_fk[0]]], 1, cur, False)

                    cols = ["edition", "species_id", "accession", "db", "db_object_id", "symbol", "qualifier",
                            "go_id", "reference", "evidence", "db_object_name", "synonyms", "db_object_type",
                            "taxon"]
                    self.insert_multiple(GOTrack.TABLES['gene_annotation'], cols, data, self.CONCURRENT_INSERTIONS,
                                         cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back', sp_id, ed, date.strftime('%Y-%m-%d'), inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def pre_process_current_genes_tables(self):

        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                log.info("pre_process_current_genes_tables (~1-2min)")
                try:
                    cur.execute("SELECT id from {species} order by id".format(**GOTrack.TABLES))
                    species = [x[0] for x in cur.fetchall()]

                    sql = """select distinct ga.species_id, symbol, accession, synonyms
                                from {gene_annotation} ga inner join
                                (select species_id, max(edition) as current_edition from {edition} group by species_id)
                                    as current_editions
                                on current_editions.current_edition=ga.edition
                                    and current_editions.species_id = ga.species_id
                            where accession is not null""".format(**GOTrack.TABLES)

                    cur.execute(sql)
                    raw_data = (x for x in cur.fetchall())
                    log.info('Data fetched')

                    gene_id_maps = {x: {} for x in species}

                    pp_current_genes = []
                    pp_synonyms = []
                    pp_primary_accessions = []
                    pp_current_genes_id = 1
                    for row in raw_data:
                        sp_id = row[0]
                        gmap = gene_id_maps[sp_id]
                        symbol = row[1]

                        try:
                            gene_id = gmap[symbol.upper()]
                        except KeyError:
                            gmap[symbol.upper()] = pp_current_genes_id
                            gene_id = pp_current_genes_id
                            pp_current_genes.append((gene_id, sp_id, symbol))
                            pp_current_genes_id += 1

                        pp_primary_accessions.append((gene_id, row[2]))

                        for syn in row[3].split("|"):
                            pp_synonyms.append((gene_id, syn.strip()))

                    # case insensitive
                    pp_current_genes = sorted({(x[0], x[1], x[2].lower()): x for x in pp_current_genes}.values())
                    pp_synonyms = sorted({(x[0], x[1].lower()): x for x in pp_synonyms}.values())
                    pp_primary_accessions = sorted({(x[0], x[1].lower()): x for x in pp_primary_accessions}.values())

                    # print len(pp_current_genes)
                    # print len(pp_synonyms)
                    # print len(pp_primary_accessions)
                    #
                    # for i,gmap in enumerate(gene_id_maps):
                    #     print i+1, len(gmap)

                    sql = "CREATE TABLE {current_genes}{new} like {current_genes}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    log.info("Inserting current_genes table")
                    cnt = self.insert_multiple(GOTrack.TABLES['current_genes'] + GOTrack.TABLES['new'],
                                               ["id", "species_id", "symbol"], pp_current_genes,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, False)
                    if cnt == 0:
                        raise (ValueError("No values inserted for current_genes table"))

                    sql = "RENAME TABLE {current_genes} TO {current_genes}{old}, " \
                          "{current_genes}{new} to {current_genes}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    #####################

                    sql = "CREATE TABLE {synonym}{new} like {synonym}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    log.info("Inserting synonym table")
                    cnt = self.insert_multiple(GOTrack.TABLES['synonym'] + GOTrack.TABLES['new'],
                                               ["pp_current_genes_id", "synonym"], pp_synonyms,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, False)

                    if cnt == 0:
                        raise (ValueError("No values inserted for synonym table"))

                    sql = "RENAME TABLE {synonym} TO {synonym}{old}, {synonym}{new} to {synonym}"\
                        .format(**GOTrack.TABLES)
                    cur.execute(sql)

                    #####################

                    sql = "CREATE TABLE {accession}{new} like {accession}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    log.info("Inserting accession table")
                    cnt = self.insert_multiple(GOTrack.TABLES['accession'] + GOTrack.TABLES['new'],
                                               ["pp_current_genes_id", "accession"], pp_primary_accessions,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, False)

                    if cnt == 0:
                        raise (ValueError("No values inserted for accession table"))

                    sql = "RENAME TABLE {accession} TO {accession}{old}, {accession}{new} to {accession}"\
                        .format(**GOTrack.TABLES)
                    cur.execute(sql)

                    #####################

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back', inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.execute("DROP TABLE IF EXISTS {current_genes}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {current_genes}{new}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {synonym}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {synonym}{new}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {accession}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {accession}{new}".format(**GOTrack.TABLES))
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def pre_process_goa_table(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                log.info("pre_process_goa_table (~90min)")
                try:
                    sql = "CREATE TABLE {goa}{new} like {goa}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    sql = """INSERT IGNORE INTO {goa}{new}(pp_current_genes_id, edition, qualifier, go_id,
                                evidence, reference)
                             select distinct pp_current_genes_id, edition, qualifier , go_id,
                                evidence, reference from {gene_annotation}
                             inner join
                                (select pp_current_genes_id, sec from {accession} inner join {sec_ac} on ac=accession
                                UNION ALL
                                select pp_current_genes_id, ac from {accession} inner join {sec_ac} on ac=accession
                                UNION
                                select pp_current_genes_id, accession from {accession}) as gene_mapping on accession=sec
                             where accession is not null and species_id=%s""".format(**GOTrack.TABLES)

                    cur.execute("SELECT id from {species} order by id".format(**GOTrack.TABLES))
                    species = [x[0] for x in cur.fetchall()]

                    for sp_id in species:
                        log.info('species: {0}'.format(sp_id))
                        cur.execute(sql, [sp_id])

                    sql = "RENAME TABLE {goa} TO {goa}{old}, {goa}{new} to {goa}".format(**GOTrack.TABLES)
                    cur.execute(sql)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back', inst)
                    self.con.rollback()
                finally:
                    if cur:
                        cur.execute("DROP TABLE IF EXISTS {goa}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {goa}{new}".format(**GOTrack.TABLES))
                        cur.close()
        except _mysql.Error, e:
            log.error("Problem with database connection", e)
            return

    def insert_multiple(self, table, columns, data, concurrent_insertions=1000, cur=None, verbose=False):
        value_length = len(columns)

        if cur is None:
            cur = self.con.cursor()
            close_cursor = True
        else:
            close_cursor = False

        # It is actually extremely important that 'values' be lowercase. DO NOT CHANGE.
        # See http://stackoverflow.com/a/3945860/4907830.
        sql_template = "INSERT INTO {0}({1}) values {2}"

        values_template = "(" + ",".join(["%s"] * value_length) + ")"

        sql = sql_template.format(table, ",".join(columns), values_template)

        cnt = 0
        start = time.time()
        for row_list in grouper(concurrent_insertions, data):
            cnt += len(row_list)
            if verbose:
                log.info(cnt)
            cur.executemany(sql, row_list)

        log.info("Row Count: {0}, Time: {1}".format(cnt, time.time() - start))

        if close_cursor and cur:
            cur.close()

        return cnt

    def insert_multiple2(self, table, columns, data, concurrent_insertions=1000, cur=None, verbose=False):
        # When you are finally fed up with executemany and its impressive amount of bugs, re-implement this.
        value_length = len(columns)

        if cur is None:
            cur = self.con.cursor()
            close_cursor = True
        else:
            close_cursor = False

        sql_template = "INSERT INTO {0}({1}) VALUES {2}"

        values_template = ",".join(["(" + ",".join(["%s"] * value_length) + ")"] * concurrent_insertions)

        sql = sql_template.format(table, ",".join(columns), values_template)

        row_list = []
        i = 0
        cnt = 0
        start = time.time()
        for r in data:
            i += 1
            row_list += r
            if i == concurrent_insertions:
                cnt += i
                cur.execute(sql, row_list)
                if verbose:
                    log.info(cnt)
                i = 0
                row_list = []

        # insert the remainder rows
        if i > 0:
            values_template = ",".join(["(" + ",".join(["%s"] * value_length) + ")"] * i)
            sql = sql_template.format(table, ",".join(columns), values_template)

            cnt += i
            cur.execute(sql, row_list)

        log.info("Row Count: {0}, Time: {1}".format(cnt, time.time() - start))

        if close_cursor and cur:
            cur.close()

        return cnt
