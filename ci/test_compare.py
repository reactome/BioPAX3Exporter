"""Tests for ci/compare.py: python3 -I -m unittest discover -s ci"""
import collections
import contextlib
import io
import os
import tempfile
import unittest

import compare

HEAD = (
    '<?xml version="1.0"?><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" '
    'xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:bp="http://www.biopax.org/release/biopax-level3.owl#" '
    'xml:base="http://www.reactome.org/biopax/97/1#"><owl:Ontology rdf:about="" />'
)


def dbid(name, number):
    return (
        f'<bp:UnificationXref rdf:ID="{name}"><bp:db>Reactome Database ID Release 97</bp:db>'
        f"<bp:id>{number}</bp:id></bp:UnificationXref>"
    )


def protein(name, xref):
    return f'<bp:Protein rdf:ID="{name}"><bp:xref rdf:resource="#{xref}" /></bp:Protein>'


def document(*parts):
    return HEAD + "".join(parts) + "</rdf:RDF>"


# A set of two proteins, a complex of one of them twice, and a reaction with
# its regulation written as two Controls, one per controlled reaction.
BASE = [
    dbid("X1", 11), dbid("X2", 12), dbid("X3", 13), dbid("X4", 14), dbid("X5", 15),
    dbid("X6", 16), dbid("X7", 17), dbid("X8", 18),
    protein("P1", "X1"), protein("P2", "X2"), protein("P3", "X3"),
    '<bp:PhysicalEntity rdf:ID="S1"><bp:xref rdf:resource="#X4" />'
    '<bp:memberPhysicalEntity rdf:resource="#P1" /><bp:memberPhysicalEntity rdf:resource="#P2" /></bp:PhysicalEntity>',
    '<bp:Stoichiometry rdf:ID="St1"><bp:physicalEntity rdf:resource="#P1" />'
    "<bp:stoichiometricCoefficient>2</bp:stoichiometricCoefficient></bp:Stoichiometry>",
    '<bp:Complex rdf:ID="C1"><bp:xref rdf:resource="#X5" /><bp:component rdf:resource="#P1" />'
    '<bp:componentStoichiometry rdf:resource="#St1" /></bp:Complex>',
    '<bp:BiochemicalReaction rdf:ID="R1"><bp:xref rdf:resource="#X6" /><bp:left rdf:resource="#P1" /></bp:BiochemicalReaction>',
    '<bp:BiochemicalReaction rdf:ID="R2"><bp:xref rdf:resource="#X7" /><bp:left rdf:resource="#P2" /></bp:BiochemicalReaction>',
    '<bp:Control rdf:ID="K1"><bp:xref rdf:resource="#X8" /><bp:controlled rdf:resource="#R1" />'
    '<bp:controller rdf:resource="#P1" /></bp:Control>',
    '<bp:Control rdf:ID="K2"><bp:xref rdf:resource="#X8" /><bp:controlled rdf:resource="#R2" />'
    '<bp:controller rdf:resource="#P2" /></bp:Control>',
    '<bp:PathwayStep rdf:ID="W1"><bp:stepProcess rdf:resource="#R1" /><bp:nextStep rdf:resource="#W2" /></bp:PathwayStep>',
    '<bp:PathwayStep rdf:ID="W2"><bp:stepProcess rdf:resource="#R2" /><bp:nextStep rdf:resource="#W1" /></bp:PathwayStep>',
    "<bp:Provenance rdf:ID=\"V1\"><bp:comment>Authored: Someone, A (2001-01-01)</bp:comment></bp:Provenance>",
    '<bp:PublicationXref rdf:ID="B1"><bp:author>Someone A</bp:author><bp:db>Pubmed</bp:db>'
    "<bp:id>1</bp:id></bp:PublicationXref>",
]


class CompareTest(unittest.TestCase):
    def write(self, *parts):
        handle, path = tempfile.mkstemp(suffix=".owl")
        with os.fdopen(handle, "w") as f:
            f.write(document(*parts))
        self.addCleanup(os.remove, path)
        return path

    def compared(self, parts):
        return compare.differences(compare.triples(self.write(*BASE)), compare.triples(self.write(*parts)))

    def changed(self, old, new):
        parts = [part.replace(old, new) for part in BASE]
        self.assertNotEqual(parts, BASE, f"{old} is not in the fixture")
        return self.compared(parts)

    def test_ids_and_order_do_not_count(self):
        parts = [part.replace('"P1"', '"Q9"').replace('"#P1"', '"#Q9"') for part in reversed(BASE)]
        self.assertEqual(self.compared(parts), (collections.Counter(), collections.Counter()))

    def test_a_reference_written_absolute_names_the_same_individual(self):
        parts = [part.replace('"#P1"', '"http://www.reactome.org/biopax/97/1#P1"') for part in BASE]
        self.assertEqual(sum(sum(side.values()) for side in self.compared(parts)), 0)

    def test_a_set_member_re_pointed_is_one_triple_each_side(self):
        only_reference, only_other = self.changed('<bp:memberPhysicalEntity rdf:resource="#P2" />',
                                                  '<bp:memberPhysicalEntity rdf:resource="#P3" />')
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (1, 1))
        self.assertEqual({compare.kind(t) for t in only_other}, {"PhysicalEntity.memberPhysicalEntity"})

    def test_a_coefficient_changed_is_three_triples_each_side(self):
        only_reference, only_other = self.changed(">2</bp:stoichiometricCoefficient>", ">3</bp:stoichiometricCoefficient>")
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (3, 3))

    def test_a_control_is_keyed_by_what_it_controls(self):
        # The controllers swapped between the two Controls of one regulation:
        # keyed by the regulation alone, the two would read the same.
        parts = [part.replace('"#P1" /></bp:Control>', '"#TMP" /></bp:Control>')
                 .replace('"#P2" /></bp:Control>', '"#P1" /></bp:Control>')
                 .replace('"#TMP" /></bp:Control>', '"#P2" /></bp:Control>') for part in BASE]
        only_reference, only_other = self.compared(parts)
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (2, 2))

    def test_counts_a_repeated_individual(self):
        only_reference, only_other = self.compared(BASE + [BASE[-2].replace('"V1"', '"V2"')])
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (0, 1))

    def test_steps_that_chain_in_a_cycle_are_compared(self):
        only_reference, only_other = self.changed('<bp:nextStep rdf:resource="#W1" />', "")
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (1, 0))

    def test_names_are_compared_but_never_printed(self):
        only_reference, only_other = self.changed("Someone", "Another")
        self.assertEqual(sum(only_other.values()), 2)
        text = "\n".join(compare.report(only_reference, only_other, examples=10))
        for name in ("Someone", "Another"):
            self.assertNotIn(name, text)

    def test_refuses_an_individual_nested_in_another(self):
        path = self.write('<bp:Protein rdf:ID="P1"><bp:xref><bp:UnificationXref rdf:ID="X1" /></bp:xref></bp:Protein>')
        with self.assertRaises(ValueError):
            compare.triples(path)

    def test_refuses_two_individuals_of_one_name(self):
        with self.assertRaises(ValueError):
            compare.triples(self.write(protein("P1", "X1"), protein("P1", "X2")))

    def test_exits_by_whether_the_files_differ(self):
        same, other = self.write(*BASE), self.write(*BASE[:-1])
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(compare.main(["compare.py", same, same]), 0)
            self.assertEqual(compare.main(["compare.py", same, other]), 1)
            self.assertEqual(compare.main(["compare.py", same]), 2)


if __name__ == "__main__":
    unittest.main()
