__author__ = 'mjacobson'

''' Object Models '''

from collections import namedtuple
from parsers import parse_go_obo
from datetime import datetime

Relationship = namedtuple("Relationship", ["term", "type"])


class GOTerm:
    def __init__(self, t):
        self.id = t['id'][0]
        try:
            self.obsolete = t['is_obsolete'][0] == 'true'
        except KeyError:
            # Not obsolete
            self.obsolete = False

        parents = []
        try:
            rel = t['relationship']
            parents += [(r.split(' ', 2)[1], 'part_of') for r in rel if r.startswith('part_of')]
        except KeyError:
            # No relationship
            pass

        try:
            rel = t['is_a']
            parents += [(r.split(' ', 1)[0], 'is_a') for r in rel]
        except KeyError:
            # No relationship
            pass

        self.parents = parents

        self.name = t['name'][0]

        namespace = t['namespace'][0].upper()

        if namespace == 'BP' or 'PROC' in namespace:
            self.aspect = 'BP'
        elif namespace == 'CC' or 'COMP' in namespace:
            self.aspect = 'CC'
        elif namespace == 'MF' or 'FUNC' in namespace:
            self.aspect = 'MF'
        else:
            print 'Uknown aspect', namespace
            self.aspect = namespace

        self.definition = t['def'][0].split("\"", 2)[1]

    def __eq__(self, other):
        return (isinstance(other, self.__class__)
                and self.id == other.id)

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.id)

    def __str__(self):
        return str(self.__dict__)


class Ontology:

    def __init__(self, date, file_or_data):

        if date is None:
            raise ValueError("Requires a date")

        try:
            date.strftime('%Y-%m-%d')
            self.date = date
        except AttributeError:
            try:
                self.date = datetime.strptime(date, '%Y-%m-%d').date()
            except ValueError:
                raise ValueError("Cannot parse date")

        if file_or_data is None:
            raise ValueError("Requires either filename to be parsed or already parsed data")

        term_map = {}

        if isinstance(file_or_data, basestring):
            obo = parse_go_obo(file_or_data)
        else:
            obo = file_or_data

        for t in obo:
            g = GOTerm(t)
            term_map[g.id] = g

        # replace parents with GOTerm instances

        for goId, t in term_map.iteritems():
            t.parents = [Relationship(term_map[pId[0]], pId[1]) for pId in t.parents]

        self.term_map = term_map

    def list_terms(self):
        """
        Generate list of nodes (GO Terms) in this ontology.
        """
        return ((goTerm.id, goTerm.name, goTerm.aspect, goTerm.obsolete) for goTerm in
                self.term_map.itervalues())

    def adjacency_list(self):
        """
        Generate adjacency list of relationships in this ontology.
        """
        return ((goTerm.id, rel.term.id, rel.type.upper()) for goTerm in self.term_map.itervalues()
                for rel in goTerm.parents)

    def transitive_closure(self, reflexive=True):
        """
        Generate (reflexive) transitive closure of relationships in this ontology along with minimum distance.
        """
        print 'Creating Reflexive Transitive Closure'

        for goId, t in self.term_map.iteritems():
            ancestor_distance = {}
            if reflexive:
                ancestor_distance[Relationship(t, 'is_a')] = 0

            parent_queue = [(t, False, 0)]

            while parent_queue:
                p, part_of_transitive, dist = parent_queue.pop()

                for rel in p.parents:

                    r = 'part_of' if part_of_transitive else rel.type
                    transitive_rel = Relationship(rel.term, r)

                    try:
                        old_dist = ancestor_distance[transitive_rel]
                    except KeyError:
                        # No previous entry
                        ancestor_distance[Relationship(rel.term, r)] = dist + 1
                    else:
                        # there was a previous entry
                        if dist + 1 < old_dist:
                            ancestor_distance[Relationship(rel.term, r)] = dist + 1

                    parent_queue.append((rel.term, r == 'part_of', dist + 1))

            for rel, dist in ancestor_distance.iteritems():
                # print rel.term.id, rel.type, dist
                yield t.id, rel.term.id, rel.type.upper(), dist
