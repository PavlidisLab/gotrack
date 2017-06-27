__author__ = 'mjacobson'

''' Object Models '''

from collections import namedtuple
from parsers import parse_go_obo
from datetime import datetime
from collections import defaultdict

Relationship = namedtuple("Relationship", ["term", "type"])


class GOTerm:
    def __init__(self, go_id, name=None, definition=None, aspect=None, parents=None, alt_ids=None, obsolete=None):
        self.id = go_id
        self.name = name
        self.definition = definition
        self.aspect = aspect
        self.parents = parents if parents is not None else []  # We do this because default arguments are function member variables
        self.alt_ids = alt_ids if alt_ids is not None else []  # We do this because default arguments are function member variables
        self.obsolete = obsolete

    @classmethod
    def from_obo_entry(cls, obo_entry):
        go_id = obo_entry['id'][0]
        try:
            obsolete = obo_entry['is_obsolete'][0] == 'true'
        except KeyError:
            # Not obsolete
            obsolete = False

        parents = []
        try:
            rel = obo_entry['relationship']
            parents += [(r.split(' ', 2)[1], 'part_of') for r in rel if r.startswith('part_of')]
        except KeyError:
            # No relationship
            pass

        try:
            rel = obo_entry['is_a']
            parents += [(r.split(' ', 1)[0], 'is_a') for r in rel]
        except KeyError:
            # No relationship
            pass

        alt_ids = []
        try:
            alt = obo_entry['alt_id']
            alt_ids += [r.split(' ', 1)[0] for r in alt]
        except KeyError:
            # No alts
            pass

        name = obo_entry['name'][0]

        namespace = obo_entry['namespace'][0].upper()

        if namespace == 'BP' or 'PROC' in namespace:
            aspect = 'BP'
        elif namespace == 'CC' or 'COMP' in namespace:
            aspect = 'CC'
        elif namespace == 'MF' or 'FUNC' in namespace:
            aspect = 'MF'
        else:
            print 'Uknown aspect', namespace
            aspect = namespace

        definition = obo_entry['def'][0].split("\"", 2)[1]

        return cls(go_id, name, definition, aspect, parents, alt_ids, obsolete)

    def add_relationship(self, term, rel):
        if not isinstance(term, GOTerm):
            raise ValueError("Term must be an instance of GOTerm")
        rel_type = str(rel).lower()
        if rel_type not in ['is_a', 'part_of']:
            raise ValueError("Relationship must be either 'is_a' or 'part_of'")
        self.parents.append(Relationship(term, rel_type))

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
    def __init__(self, date, term_map):

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

        self.term_map = term_map
        self.ancestor_cache = defaultdict(set)

    @classmethod
    def from_file_data(cls, date, file_or_data):

        if file_or_data is None:
            raise ValueError("Requires either filename to be parsed or already parsed data")

        term_map = {}

        if isinstance(file_or_data, basestring):
            obo = parse_go_obo(file_or_data)
        else:
            obo = file_or_data

        for t in obo:
            g = GOTerm.from_obo_entry(t)
            term_map[g.id] = g

        # replace parents with GOTerm instances

        for goId, t in term_map.iteritems():
            t.parents = [Relationship(term_map[pId[0]], pId[1]) for pId in t.parents]

        return cls(date, term_map)

    @classmethod
    def from_lists(cls, date, node_list, adjacency_list):
        term_map = {}

        # Create Nodes
        for node in node_list:
            term_map[node[0]] = GOTerm(go_id=node[0], name=node[1], aspect=node[2])

        # Add relationships
        for child_id, parent_id, rel_type in adjacency_list:
            try:
                child_node = term_map[child_id]
            except KeyError:
                raise
            try:
                parent_node = term_map[parent_id]
            except KeyError:
                raise

            child_node.add_relationship(parent_node, rel_type)

        return cls(date, term_map)

    @classmethod
    def from_adjacency(cls, date, adjacency_list):
        term_map = {}

        # Add nodes and relationships
        for child_id, parent_id, rel_type in adjacency_list:
            try:
                child_node = term_map[child_id]
            except KeyError:
                child_node = GOTerm(go_id=child_id)
                term_map[child_id] = child_node
            try:
                parent_node = term_map[parent_id]
            except KeyError:
                parent_node = GOTerm(go_id=parent_id)
                term_map[parent_id] = parent_node

            child_node.add_relationship(parent_node, rel_type)

        return cls(date, term_map)

    def get_term(self, go_id):
        try:
            term = self.term_map[go_id]
        except KeyError:
            term = None
        return term

    def get_ancestors(self, term_or_id, include_self=False):
        if not isinstance(term_or_id, GOTerm):
            try:
                term = self.term_map[term_or_id]
            except KeyError as e:
                raise e
        else:
            term = term_or_id

        if term in self.ancestor_cache:
            return self.ancestor_cache[term]

        ancestors = set()
        if include_self:
            ancestors.add(term)

        parent_queue = [term]

        while parent_queue:
            p = parent_queue.pop()

            for rel in p.parents:
                ancestors.add(rel.term)
                if rel.term in self.ancestor_cache:
                    ancestors.update(self.ancestor_cache[rel.term])
                else:
                    parent_queue.append(rel.term)

        self.ancestor_cache[term] = ancestors
        return ancestors

    def get_ancestors_distances(self, term_or_id, include_self=False):

        if not isinstance(term_or_id, GOTerm):
            try:
                term = self.term_map[term_or_id]
            except KeyError as e:
                raise e
        else:
            term = term_or_id

        ancestor_distance = {}
        if include_self:
            ancestor_distance[Relationship(term, 'is_a')] = 0

        parent_queue = [(term, False, 0)]

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

        return ancestor_distance

    def list_terms(self):
        """
        Generate list of nodes (GO Terms) in this ontology.
        """
        return ((goTerm.id, goTerm.name, goTerm.aspect, goTerm.obsolete) for goTerm in
                self.term_map.itervalues())

    def list_definitions(self):
        """
        Generate list of nodes (GO Terms) definitions in this ontology.
        """
        return ((goTerm.id, goTerm.definition) for goTerm in self.term_map.itervalues())

    def adjacency_list(self):
        """
        Generate adjacency list of relationships in this ontology.
        """
        return ((goTerm.id, rel.term.id, rel.type.upper()) for goTerm in self.term_map.itervalues()
                for rel in goTerm.parents)

    def alternate_list(self):
        """
        Generate list of alternate ids to primary id.
        """
        return ((alt, goTerm.id) for goTerm in self.term_map.itervalues()
                for alt in goTerm.alt_ids)

    def transitive_closure(self, reflexive=True):
        """
        Generate (reflexive) transitive closure of relationships in this ontology along with minimum distance.
        """
        print 'Creating Reflexive Transitive Closure'

        for goId, t in self.term_map.iteritems():
            ancestor_distance = self.get_ancestors_distances(t, reflexive)

            for rel, dist in ancestor_distance.iteritems():
                # print rel.term.id, rel.type, dist
                yield t.id, rel.term.id, rel.type.upper(), dist
