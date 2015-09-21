from __future__ import with_statement

__author__ = 'mjacobson'

''' File parsers '''

from collections import defaultdict
import gzip
import re


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


def retrieve_meta_goa(f):
    results = [None, None, None]
    with gzip.open(f, "r") as infile:
        for line in infile:
            if line.startswith('!gaf-version'):
                results[0] = line.split()[1]
            elif line.startswith('!Generated'):
                results[1] = line.split()[1]
            elif line.startswith('!GO-version'):
                results[2] = line.split()[1]

            if None not in results:
                break

    return results


def process_goa(f, sp_id, ed):
    with gzip.open(f, "r") as infile:
        for line in infile:
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

                # yield ed, sp, @2, 1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13
                acc = sl[1]

                # match = re.match(r"^[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){2}$", accession)
                #
                # if match == None:
                #     match = re.match(r"^[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1}$", accession)
                #     if match == None:
                #         match = re.match(r"^[OPQ][0-9][A-Z0-9]{3}[0-9]$", accession)
                #         if match == None:
                #             # Not an accession
                #             accession = None
                #         else:
                #             accession = accession[:6]
                #     else:
                #         accession = accession[:6]
                # else:
                #     accession = accession[:10]

                # See http://www.uniprot.org/help/accession_numbers

                match = re.search(r"^[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}", acc)

                if match:
                    acc = match.group(0)
                else:
                    acc = None

                yield ed, sp_id, acc, sl[0], sl[1], sl[2], sl[3], sl[4], sl[5], sl[6], sl[9], sl[10], sl[11], sl[12]


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
