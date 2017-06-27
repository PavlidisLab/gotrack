from __future__ import with_statement

__author__ = 'mjacobson'

''' File parsers '''

from collections import defaultdict
import gzip
import re
import logging
from datetime import datetime
from utility import deprecated

log = logging.getLogger(__name__)


def process_sec_ac(f):
    with open(f, "r") as infile:
        header_found = False
        separator_found = False
        for line in infile:
            if header_found and separator_found and line.strip():
                yield line.split()
            elif header_found and line.startswith("_____"):
                separator_found = True
            elif line.startswith("Secondary AC"):
                header_found = True


@deprecated
def process_acindex(f):
    with open(f, "r") as infile:
        header_found = False
        separator_found = False
        for line in infile:
            if header_found and separator_found:

                if not line.strip():
                    break  # end of data

                split_line = line.split()
                if not line.startswith(" "):  # new accession, otherwise we keep same accession as previous iteration
                    acc = split_line.pop(0)

                for symb in split_line:
                    yield acc, symb.strip(",")

            elif header_found and line.startswith("_____"):
                separator_found = True
            elif line.split() == ["AC", "Entry"]:
                header_found = True


def check_goa_gaf(f):
    """Returns generated date and go version if everything looks OK else None"""
    version = None
    generated_tag = None
    go_tag = None
    error = False

    with gzip.open(f, "r") as infile:
        for line in infile:
            if line.startswith('!gaf-version'):
                version = line.split()[1]
            elif line.startswith('!Generated'):
                generated_tag = line.split()[1]
            elif line.startswith('!GO-version'):
                go_tag = line.split()[1]
            elif line.strip() and not line.startswith('!'):
                break

    if version is None:
        log.warn('Missing GAF Version Tag')
        error = True
    else:
        if version not in ["2.0", "2.1"]:
            log.warn('Unsupported GAF Version: %s', version)
            error = True

    if generated_tag is None:
        log.warn('Missing Generated Tag')
        error = True
    else:
        match = re.search(r'[\d]{4}\-[\d]{2}\-[\d]{2}', generated_tag)
        if match is not None:
            generated = datetime.strptime(match.group(0), "%Y-%m-%d").date()
        else:
            log.warn('Cannot parse Generated Tag as date: %s', generated_tag)
            error = True

    if go_tag is None:
        log.warn('Missing GO-Version Tag')
        error = True
    else:
        match = re.search(r'[\d]{4}\-[\d]{2}\-[\d]{2}', go_tag)
        if match is not None:
            go = datetime.strptime(match.group(0), "%Y-%m-%d").date()
        else:
            log.warn('Cannot parse GO-Version Tag as date: %s', go_tag)
            error = True

    return (generated, go) if not error else None

def check_goa_gpa(f):
    """Returns generated date and go version if everything looks OK else None"""
    version = None
    generated_tag = None
    go_tag = None
    error = False

    with gzip.open(f, "r") as infile:
        for line in infile:
            if line.startswith('!gpa-version'):
                version = line.split()[1]
            elif line.startswith('!Generated'):
                generated_tag = line.split()[1]
            elif line.startswith('!GO-version'):
                go_tag = line.split()[1]
            elif line.strip() and not line.startswith('!'):
                break

    if version is None:
        log.warn('Missing GPAD Version Tag')
        error = True
    else:
        if version not in ["1.1"]:
            log.warn('Unsupported GPAD Version: %s', version)
            error = True

    if generated_tag is None:
        log.warn('Missing Generated Tag')
        error = True
    else:
        match = re.search(r'[\d]{4}\-[\d]{2}\-[\d]{2}', generated_tag)
        if match is not None:
            generated = datetime.strptime(match.group(0), "%Y-%m-%d").date()
        else:
            log.warn('Cannot parse Generated Tag as date: %s', generated_tag)
            error = True

    if go_tag is None:
        log.warn('Missing GO-Version Tag')
        error = True
    else:
        match = re.search(r'[\d]{4}\-[\d]{2}\-[\d]{2}', go_tag)
        if match is not None:
            go = datetime.strptime(match.group(0), "%Y-%m-%d").date()
        else:
            log.warn('Cannot parse GO-Version Tag as date: %s', go_tag)
            error = True

    return (generated, go) if not error else None

def check_goa_gpi(f):
    """Returns generated date if everything looks OK else None"""
    version = None
    generated_tag = None
    error = False

    with gzip.open(f, "r") as infile:
        for line in infile:
            if line.startswith('!gpi-version'):
                version = line.split()[1]
            elif line.startswith('!Generated'):
                generated_tag = line.split()[1]
            elif line.strip() and not line.startswith('!'):
                break

    if version is None:
        log.warn('Missing GPI Version Tag')
        error = True
    else:
        if version not in ["1.2"]:
            log.warn('Unsupported GPI Version: %s', version)
            error = True

    if generated_tag is None:
        log.warn('Missing Generated Tag')
        error = True
    else:
        match = re.search(r'[\d]{4}\-[\d]{2}\-[\d]{2}', generated_tag)
        if match is not None:
            generated = datetime.strptime(match.group(0), "%Y-%m-%d").date()
        else:
            log.warn('Cannot parse Generated Tag as date: %s', generated_tag)
            error = True

    return (generated,) if not error else None


def process_goa_gaf(f):
    meta = check_goa_gaf(f)
    if meta is None:
        log.warn("Issues parsing GAF file, returning...")
        return

    with gzip.open(f, "r") as infile:
        for line in infile:
            line=line.strip()
            if line.startswith('!'):
                continue  # comment/metadata
            if line.strip():  # not empty
                sl = line.split("\t")

                # Column    Content                         Required?   Cardinality     Example
                # 1         DB                              required    1               UniProtKB
                # 2         DB Object ID                    required    1               P12345
                # 3         DB Object Symbol                required    1               PHO3
                # 4         Qualifier                       optional    0 or greater    NOT
                # 5         GO ID                           required    1               GO:0003993
                # 6         DB:Reference (|DB:Reference)    required    1 or greater    PMID:2676709
                # 7         Evidence Code                   required    1               IMP
                # 8         With (or) From                  optional    0 or greater    GO:0000346
                # 9         Aspect                          required    1               F
                # 10        DB Object Name                  optional    0 or 1          Toll-like receptor 4
                # 11        DB Object Synonym (|Synonym)    optional    0 or greater    hToll|Tollbooth
                # 12        DB Object Type                  required    1               protein
                # 13        Taxon(|taxon)                   required    1 or 2          taxon:9606
                # 14        Date                            required    1               20090118
                # 15        Assigned By                     required    1               SGD
                # 16        Annotation Extension            optional    0 or greater    part_of(CL:0000576)
                # 17        Gene Product Form ID            optional    0 or 1          UniProtKB:P12345-2

                # yield 1, 2, 4, 5, 6, 7

                yield sl[0], sl[1], sl[3], sl[4], sl[5], sl[6]

def process_goa_gpa(f):
    """Returns DB, DB_Object_ID, Qualifier, GO ID, DB:Reference(s), GO Evidence"""
    meta = check_goa_gpa(f)
    if meta is None:
        log.warn("Issues parsing GPAD file, returning...")
        return

    with gzip.open(f, "r") as infile:
        for line in infile:
            line=line.strip()
            if line.startswith('!'):
                continue  # comment/metadata
            if line.strip():  # not empty
                sl = line.split("\t")

                # Column  Content                 Required  Cardinality     Example
                # 1       DB                      required  1               SGD
                # 2       DB Object ID            required  1               P12345
                # 3       Qualifier               required  1 or greater    enables
                # 4       GO ID                   required  1               GO:0019104
                # 5       DB:Reference(s)         required  1 or greater    PMID:20727966
                # 6       Evidence code           required  1               ECO:0000021
                # 7       With (or) From          optional  0 or greater    Ensembl:ENSRNOP00000010579
                # 8       Interacting taxon ID    optional  0 or 1          4896
                # 9       Date                    required  1               20130529
                # 10      Assigned by             required  1               PomBase
                # 11      Annotation Extension    optional  0 or greater    occurs_in(GO:0005739)
                # 12      Annotation Properties   optional  0 or greater    annotation_identifier = 2113431320

                # yield 1, 2, 3, 4, 5, @12

                # Pipe delimited in property=value form: p1=v1|p2=v2|p3=v3|...
                properties = {k: v for k, v in [p.split("=") for p in sl[11].split("|")]}

                yield sl[0], sl[1], sl[2], sl[3], sl[4], properties.setdefault('go_evidence', None)

def process_goa_gpi(f):
    """Returns DB, accession, DB_Object_ID, DB_Object_Symbol, DB_Object_Name, DB_Object_Type, subset, List[Synonyms]"""
    meta = check_goa_gpi(f)
    if meta is None:
        log.warn("Issues parsing GPI file, returning...")
        return

    with gzip.open(f, "r") as infile:
        for line in infile:
            line=line.strip()
            if line.startswith('!'):
                continue  # comment/metadata
            if line.strip():  # not empty
                sl = line.split("\t")

                # column	name                   required? cardinality   GAF column #  Example content
                # 1	        DB                     required  1             1             UniProtKB
                # 2	        DB_Object_ID           required  1             2/17          Q4VCS5-1
                # 3	        DB_Object_Symbol       required  1             3             AMOT
                # 4	        DB_Object_Name         optional  0 or greater  10            Angiomotin
                # 5	        DB_Object_Synonym(s)   optional  0 or greater  11            AMOT|KIAA1071
                # 6	        DB_Object_Type         required  1             12            protein
                # 7	        Taxon                  required  1             13            taxon:9606
                # 8	        Parent_Object_ID       optional  0 or 1        -             UniProtKB:Q4VCS5
                # 9	        DB_Xref(s)             optional  0 or greater  -             (not populated in gp_information files supplied by UniProt-GOA)
                # 10	    Properties             optional  0 or greater  -             db_subset=Swiss-Prot

                # yield 1, @2, 2, 3, 4, 6, @10, @5
                acc = sl[1]

                # See http://www.uniprot.org/help/accession_numbers

                match = re.search(r"^[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}", acc)

                if match:
                    acc = match.group(0)
                else:
                    acc = None

                # Pipe delimited in property=value form: p1=v1|p2=v2|p3=v3|...
                properties = {k: v for k, v in [p.split("=") for p in sl[9].split("|")]}

                yield sl[0], acc, sl[1], sl[2], sl[3], sl[5], properties.setdefault('db_subset', None), sl[4].split("|")


def parse_go_obo(filename):
    """
    Parses a Gene Ontology dump in OBO v1.2 format.
    Yields each
    Keyword arguments:
        filename: The filename to read
    """
    with gzip.open(filename, "r") as infile:
        current_go_term = None
        for line in infile:
            line = line.strip()
            if not line:
                continue  # Skip empty
            if line == "[Term]":
                if current_go_term:
                    yield dict(current_go_term)
                current_go_term = defaultdict(list)
            elif line == "[Typedef]":
                # Skip [Typedef sections]
                current_go_term = None
            else:  # Not [Term]
                # Only process if we're inside a [Term] environment
                if current_go_term is None:
                    continue
                key, sep, val = line.partition(":")
                current_go_term[key].append(val.strip())
        # Add last term
        if current_go_term is not None:
            yield dict(current_go_term)
