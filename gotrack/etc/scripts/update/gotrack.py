__author__ = 'mjacobson'

"""
Contains functionality related to connecting, retrieving information and inserting information to the GOTrack
database.
"""

import logging
import _mysql
import MySQLdb
import time


from utility import grouper, timeit
import warnings
warnings.filterwarnings("ignore", "Unknown table.*")

log = logging.getLogger(__name__)


class GOTrack:

    TABLES = {'go_term': "go_term",
              'go_adjacency': "go_adjacency",
              'go_alternate': "go_alternate",
              'go_definition': "go_definition",
              'gene_annotation': "gene_annotation",
              'edition': "edition",
              'species': "species",
              'go_edition': "go_edition",
              'sec_ac': "sec_ac",
              'acindex': "acindex",
              'pp_edition_aggregates': 'pp_edition_aggregates',
              'pp_edition_aggregates_staging': 'pp_edition_aggregates_staging',
              'pp_go_annotation_counts': 'pp_go_annotation_counts',
              'pp_go_annotation_counts_staging': 'pp_go_annotation_counts_staging',
              'pp_genes_staging': "pp_current_genes_staging",
              'pp_goa_staging': "pp_goa_staging",
              'pp_accession_staging': "pp_primary_accessions_staging",
              'pp_synonym_staging': "pp_synonyms_staging",
              'pp_genes': "pp_current_genes",
              'pp_goa': "pp_goa",
              'pp_accession': "pp_primary_accessions",
              'pp_synonym': "pp_synonyms",
              'new': "_tmp_newdata",  # tmp table creation suffix
              'old': "_tmp_olddata"  # tmp table creation suffix
              }

    CONCURRENT_INSERTIONS = 1000

    def __init__(self, **kwargs):
        self.creds = kwargs
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
                    cur.execute("SELECT species_id, max(edition) from {pp_goa} goa "
                                "inner join {pp_genes} cg on "
                                "goa.pp_current_genes_id=cg.id group by species_id"
                                .format(**GOTrack.TABLES))
                    pp_state = cur.fetchall()
                    results['preprocess_ood'] = pp_state != db_state

                    # Checks to see if the aggregate data is out of date with the database. If not then a server
                    # restart is required with suitable options for recreation of aggregates.
                    cur.execute("SELECT species_id, max(edition) from {pp_go_annotation_counts} group by species_id"
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
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

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
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

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
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.execute("DROP TABLE IF EXISTS {sec_ac}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {sec_ac}{new}".format(**GOTrack.TABLES))
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

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
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.execute("DROP TABLE IF EXISTS {acindex}{old}".format(**GOTrack.TABLES))
                        cur.execute("DROP TABLE IF EXISTS {acindex}{new}".format(**GOTrack.TABLES))
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

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

                    # Insert alternate table
                    log.info("Inserting Alternate Table")
                    data_generator = ((go_edition_id,) + x for x in ont.alternate_list())
                    self.insert_multiple(GOTrack.TABLES['go_alternate'],
                                         ["go_edition_id_fk", "alt", "primary"], data_generator,
                                         self.CONCURRENT_INSERTIONS, cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back %s, %s', ont.date.strftime('%Y-%m-%d'), inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def update_goa_table(self, sp_id, ed, date, data):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                try:

                    log.info("Attempting to link GOA Edition to GO Version")
                    sql = "select id, date from {go_edition} where date <= %s order by date DESC LIMIT 1".format(
                        **GOTrack.TABLES)
                    cur.execute(sql, [date.strftime('%Y-%m-%d')])
                    go_edition_id_fk = cur.fetchone()
                    cur.nextset()

                    if go_edition_id_fk is None:
                        log.error("Failed to link date: {0} to a GO Release. Setting as NULL. FIX MANUALLY."
                                  .format(date.strftime('%Y-%m-%d')))
                        raise ValueError
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
                    log.error('Error rolling back sp: %s, ed: %s, date: %s, %s', sp_id, ed, date.strftime('%Y-%m-%d'), inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    @timeit
    def pre_process_current_genes_tables(self):

        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                log.info("pre_process_current_genes_tables (~4min)")
                try:
                    cur.execute("DROP TABLE IF EXISTS {pp_genes_staging}".format(**GOTrack.TABLES))
                    cur.execute("CREATE TABLE {pp_genes_staging} LIKE {pp_genes}".format(**GOTrack.TABLES))

                    cur.execute("DROP TABLE IF EXISTS {pp_accession_staging}".format(**GOTrack.TABLES))
                    cur.execute("CREATE TABLE {pp_accession_staging} LIKE {pp_accession}".format(**GOTrack.TABLES))

                    cur.execute("DROP TABLE IF EXISTS {pp_synonym_staging}".format(**GOTrack.TABLES))
                    cur.execute("CREATE TABLE {pp_synonym_staging} LIKE {pp_synonym}".format(**GOTrack.TABLES))

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

                    log.info("Inserting current_genes table")
                    cnt = self.insert_multiple(GOTrack.TABLES['pp_genes_staging'],
                                               ["id", "species_id", "symbol"], pp_current_genes,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, False)
                    if cnt == 0:
                        raise (ValueError("No values inserted for current_genes table"))

                    #####################

                    log.info("Inserting synonym table")
                    cnt = self.insert_multiple(GOTrack.TABLES['pp_synonym_staging'],
                                               ["pp_current_genes_id", "synonym"], pp_synonyms,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, False)

                    if cnt == 0:
                        raise (ValueError("No values inserted for synonym table"))

                    #####################

                    log.info("Inserting accession table")
                    cnt = self.insert_multiple(GOTrack.TABLES['pp_accession_staging'],
                                               ["pp_current_genes_id", "accession"], pp_primary_accessions,
                                               GOTrack.CONCURRENT_INSERTIONS, cur, False)

                    if cnt == 0:
                        raise (ValueError("No values inserted for accession table"))

                    #####################

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    @timeit
    def pre_process_goa_table(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                log.info("pre_process_goa_table (~90min)")
                try:
                    cur.execute("DROP TABLE IF EXISTS {pp_goa_staging}".format(**GOTrack.TABLES))
                    cur.execute("CREATE TABLE {pp_goa_staging} LIKE {pp_goa}".format(**GOTrack.TABLES))

                    sql = """INSERT IGNORE INTO {pp_goa_staging}(pp_current_genes_id, edition, qualifier, go_id,
                                evidence, reference)
                             select distinct pp_current_genes_id, edition, qualifier , go_id,
                                evidence, reference from {gene_annotation} ga
                             inner join ( select species_id, pp_current_genes_id, sec from
                                (select pp_current_genes_id, sec from {pp_accession_staging}
                                    inner join {sec_ac} on ac=accession
                                UNION ALL
                                select pp_current_genes_id, ac from {pp_accession_staging}
                                    inner join {sec_ac} on ac=accession
                                UNION
                                select pp_current_genes_id, accession from {pp_accession_staging}) as gene_mapping
                                inner join
                                {pp_genes_staging} cg on cg.id=pp_current_genes_id and species_id=%s ) as gmap
                             on accession=sec and ga.species_id=gmap.species_id
                             where accession is not null and ga.species_id=%s""".format(**GOTrack.TABLES)

                    cur.execute("SELECT id from {species} order by id".format(**GOTrack.TABLES))
                    species = [x[0] for x in cur.fetchall()]

                    for sp_id in species:
                        log.info('species: {0}'.format(sp_id))
                        cur.execute(sql, [sp_id, sp_id])

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()
        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def push_staging_to_production(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    # Check to see if staging area has data
                    # for t in ['{pp_genes_staging}', '{pp_goa_staging}', '{pp_accession_staging}',
                    #           '{pp_synonym_staging}']:
                    #
                    #     sql = "SELECT COUNT(*) from " + t
                    #     cur.execute(sql.format(**GOTrack.TABLES))
                    #     cnt = cur.fetchone()
                    #
                    #     if cnt == 0:
                    #         raise ValueError((t + " is empty; cancelling push to production").format(**GOTrack.TABLES))

                    # Swap Staging and Production tables
                    swap_template = "rename table {prod} TO {prod}{old}, {staging} to {prod}"

                    for staging, prod in [('{pp_genes_staging}', '{pp_genes}'),
                                          ('{pp_goa_staging}', '{pp_goa}'),
                                          ('{pp_accession_staging}', '{pp_accession}'),
                                          ('{pp_synonym_staging}', '{pp_synonym}'),
                                          ('{pp_edition_aggregates_staging}', '{pp_edition_aggregates}'),
                                          ('{pp_go_annotation_counts_staging}', '{pp_go_annotation_counts}')]:
                        staging = staging.format(**GOTrack.TABLES)
                        prod = prod.format(**GOTrack.TABLES)

                        cur.execute("DROP TABLE IF EXISTS {prod}{old}".format(prod=prod, **GOTrack.TABLES))

                        sql = swap_template.format(staging=staging, prod=prod, **GOTrack.TABLES)
                        cur.execute(sql)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    # Stuff to do with aggregate creation

    def fetch_editions(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    cur.execute("select species_id, edition, ed.date goa_date, go_edition_id_fk, "
                                "ged.date go_date from {edition} ed inner join {go_edition} ged on "
                                "ed.go_edition_id_fk = ged.id".format(**GOTrack.TABLES))
                    for row in cur:
                            yield row
                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def fetch_adjacency_list(self, go_ed):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    cur.execute("select child, parent, relationship from {go_adjacency} where go_edition_id_fk = %s"
                                .format(**GOTrack.TABLES), [go_ed])
                    for row in cur:
                            yield row
                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def fetch_term_list(self, go_ed):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    cur.execute("select go_id, name, aspect from {go_term} where go_edition_id_fk = %s"
                                .format(**GOTrack.TABLES), [go_ed])
                    for row in cur:
                            yield row
                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def fetch_all_annotations(self, sp_id, ed):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    cur.execute("select distinct go_id, ppc.id gene_id from {pp_genes_staging} ppc inner join "
                                "{pp_goa_staging} ppg on ppc.id=ppg.pp_current_genes_id where species_id=%s "
                                "and edition = %s".format(**GOTrack.TABLES), [sp_id, ed])
                    for row in cur:
                        yield row
                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def create_aggregate_staging(self):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    cur.execute("DROP TABLE IF EXISTS {pp_edition_aggregates_staging}".format(**GOTrack.TABLES))
                    cur.execute("CREATE TABLE {pp_edition_aggregates_staging} LIKE {pp_edition_aggregates}".format(**GOTrack.TABLES))

                    cur.execute("DROP TABLE IF EXISTS {pp_go_annotation_counts_staging}".format(**GOTrack.TABLES))
                    cur.execute("CREATE TABLE {pp_go_annotation_counts_staging} LIKE {pp_go_annotation_counts}".format(**GOTrack.TABLES))

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def write_aggregate(self, sp_id, ed, gene_count, avg_direct_per_gene, avg_inferred_per_gene, avg_gene_per_term, avg_mf, avg_direct_jaccard, avg_inferred_jaccard):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    self.insert_multiple(GOTrack.TABLES['pp_edition_aggregates_staging'],
                                         ["species_id", "edition", "gene_count", "avg_direct_terms_for_gene", "avg_inferred_terms_for_gene", "avg_inferred_genes_for_term", "avg_multifunctionality", "avg_direct_jaccard", "avg_inferred_jaccard"],
                                         [[sp_id, ed, gene_count, avg_direct_per_gene, avg_inferred_per_gene, avg_gene_per_term, avg_mf, avg_direct_jaccard, avg_inferred_jaccard]], 1, cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def write_term_counts(self, sp_id, ed, d_map, i_map):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:

                try:
                    data_gen = ((sp_id, ed, term.id, d_map[term] if term in d_map else None , i_map[term] if term in i_map else None) for term in (d_map.viewkeys() | i_map.keys()))

                    self.insert_multiple(GOTrack.TABLES['pp_go_annotation_counts_staging'],
                                         ["species_id", "edition", "go_id", "direct_annotation_count", "inferred_annotation_count"],
                                         data_gen, GOTrack.CONCURRENT_INSERTIONS, cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back, %s', inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def write_alternate(self, go_edition_id, ont):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                try:

                    # Insert alternate table
                    log.info("Inserting Alternate Table")
                    data_generator = ((go_edition_id,) + x for x in ont.alternate_list())
                    self.insert_multiple(GOTrack.TABLES['go_alternate'],
                                         ["go_edition_id_fk", "alt", "`primary`"], data_generator,
                                         self.CONCURRENT_INSERTIONS, cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back %s, %s', ont.date.strftime('%Y-%m-%d'), inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    def write_definitions(self, ont):
        try:
            self.con = self.test_and_reconnect()
            with self.con as cur:
                try:

                    # Insert definition table
                    log.info("Inserting Definition Table")
                    data_generator = ont.list_definitions()
                    self.insert_multiple(GOTrack.TABLES['go_definition'],
                                         ["go_id", "definition"], data_generator,
                                         self.CONCURRENT_INSERTIONS, cur, False)

                    self.con.commit()
                except Exception as inst:
                    log.error('Error rolling back %s, %s', ont.date.strftime('%Y-%m-%d'), inst)
                    self.con.rollback()
                    raise
                finally:
                    if cur:
                        cur.close()

        except _mysql.Error, e:
            log.error("Problem with database connection, %s", e)
            raise

    # General use

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

        if verbose:
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
