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
    'xml:base="http://www.reactome.org/biopax/97/1#"><owl:Ontology rdf:about="">'
    '<owl:imports rdf:resource="http://www.biopax.org/release/biopax-level3.owl#" /></owl:Ontology>'
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
    dbid("X1", 11),
    dbid("X2", 12),
    dbid("X3", 13),
    dbid("X4", 14),
    dbid("X5", 15),
    dbid("X6", 16),
    dbid("X7", 17),
    dbid("X8", 18),
    protein("P1", "X1"),
    protein("P2", "X2"),
    protein("P3", "X3"),
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
    '<bp:Provenance rdf:ID="V1"><bp:comment>Authored: Someone, A (2001-01-01)</bp:comment></bp:Provenance>',
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
        return compare.differences(
            compare.triples(self.write(*BASE)), compare.triples(self.write(*parts))
        )

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
        only_reference, only_other = self.changed(
            '<bp:memberPhysicalEntity rdf:resource="#P2" />',
            '<bp:memberPhysicalEntity rdf:resource="#P3" />',
        )
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (1, 1))
        self.assertEqual(
            {compare.kind(t) for t in only_other}, {"PhysicalEntity.memberPhysicalEntity"}
        )

    def test_a_coefficient_changed_is_three_triples_each_side(self):
        only_reference, only_other = self.changed(
            ">2</bp:stoichiometricCoefficient>", ">3</bp:stoichiometricCoefficient>"
        )
        self.assertEqual((sum(only_reference.values()), sum(only_other.values())), (3, 3))

    def test_a_control_is_keyed_by_what_it_controls(self):
        # The controllers swapped between the two Controls of one regulation:
        # keyed by the regulation alone, the two would read the same.
        parts = [
            part.replace('"#P1" /></bp:Control>', '"#TMP" /></bp:Control>')
            .replace('"#P2" /></bp:Control>', '"#P1" /></bp:Control>')
            .replace('"#TMP" /></bp:Control>', '"#P2" /></bp:Control>')
            for part in BASE
        ]
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
        # The Provenance's comment, and the citation's three triples: its key
        # is its content, author included.
        self.assertEqual(sum(only_other.values()), 4)
        changed = [part.replace("Someone", "Another") for part in BASE]
        text = "\n".join(
            compare.report(
                compare.triples(self.write(*BASE)), compare.triples(self.write(*changed)), 10
            )
        )
        for name in ("Someone", "Another"):
            self.assertNotIn(name, text)

    def counts(self, one, other):
        only_one, only_other = compare.differences(
            compare.triples(self.write(*one)), compare.triples(self.write(*other))
        )
        return sum(only_one.values()), sum(only_other.values())

    def test_an_active_unit_is_compared_by_the_entity_it_names(self):
        def catalysis(unit):
            return (
                '<bp:Catalysis rdf:ID="K3"><bp:controlled rdf:resource="#R1" />'
                f'<bp:controller rdf:resource="#C1" /><bp:comment>activeUnit: #{unit}</bp:comment></bp:Catalysis>'
            )

        one = BASE + [catalysis("P1")]
        # The same entity under another rdf:ID, as another writer numbers it
        renamed = [
            part.replace('rdf:ID="P1"', 'rdf:ID="Q1"').replace('"#P1"', '"#Q1"') for part in BASE
        ] + [catalysis("Q1")]
        self.assertEqual(self.counts(one, renamed), (0, 0))
        # Another entity named: a difference. A catalysis has no database-id UnificationXref,
        # so it is keyed by its content, and all three of its triples differ.
        self.assertEqual(self.counts(one, BASE + [catalysis("P2")]), (3, 3))

    def test_every_person_prefix_is_hashed(self):
        for comment in (
            "Authored: N",
            "Reviewed: N",
            "Edited: N",
            "authored : N",
            " REVISED: N",
            "Created: N",
            "Modified: N",
            "Curated: N",
        ):
            parts = [
                f'<bp:Provenance rdf:ID="V1"><bp:comment>{comment}</bp:comment></bp:Provenance>'
            ]
            (triple,) = compare.triples(self.write(*parts))
            self.assertTrue(triple[2][1].startswith("<hashed "), comment)
        plain = [
            '<bp:Provenance rdf:ID="V1"><bp:comment>activeUnit: N</bp:comment></bp:Provenance>'
        ]
        self.assertEqual(
            next(iter(compare.triples(self.write(*plain))))[2], ("l", "activeUnit: N", "")
        )

    def test_a_citation_is_told_from_another_of_the_same_book(self):
        # Two chapters of one book share db and id: which one an event cites
        # still counts.
        def chapter(name, title):
            return (
                f'<bp:PublicationXref rdf:ID="{name}"><bp:db>ISBN</bp:db><bp:id>1</bp:id>'
                f"<bp:title>{title}</bp:title></bp:PublicationXref>"
            )

        cites = '<bp:Pathway rdf:ID="E1"><bp:xref rdf:resource="#X1" /><bp:xref rdf:resource="#{}" /></bp:Pathway>'
        one = [dbid("X1", 1), chapter("B1", "One"), chapter("B2", "Two"), cites.format("B1")]
        self.assertEqual(self.counts(one, one[:3] + [cites.format("B2")]), (1, 1))

    def test_a_vocabulary_is_told_from_another_of_the_same_term(self):
        def term(name, go):
            return (
                f'<bp:CellularLocationVocabulary rdf:ID="{name}"><bp:term>cytosol</bp:term>'
                f'<bp:xref rdf:resource="#{go}" /></bp:CellularLocationVocabulary>'
            )

        go = '<bp:UnificationXref rdf:ID="{}"><bp:db>GO</bp:db><bp:id>{}</bp:id></bp:UnificationXref>'
        placed = '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:cellularLocation rdf:resource="#{}" /></bp:Protein>'
        one = [
            dbid("X1", 1),
            go.format("G1", "1"),
            go.format("G2", "2"),
            term("L1", "G1"),
            term("L2", "G2"),
        ]
        self.assertEqual(
            self.counts(one + [placed.format("L1")], one + [placed.format("L2")]), (1, 1)
        )

    def test_an_xref_is_keyed_by_all_it_says(self):
        def xref(kind):
            return (
                f'<bp:RelationshipXref rdf:ID="R1"><bp:db>GO</bp:db><bp:id>1</bp:id>'
                f'<bp:relationshipType rdf:resource="#{kind}" /></bp:RelationshipXref>'
            )

        types = [
            f'<bp:RelationshipTypeVocabulary rdf:ID="{t}"><bp:term>{t}</bp:term></bp:RelationshipTypeVocabulary>'
            for t in ("a", "b")
        ]
        self.assertNotEqual(self.counts(types + [xref("a")], types + [xref("b")]), (0, 0))

    def test_a_number_is_compared_by_value_and_type(self):
        def coefficient(text, datatype="float"):
            return (
                '<bp:Stoichiometry rdf:ID="S1"><bp:stoichiometricCoefficient '
                f'rdf:datatype="http://www.w3.org/2001/XMLSchema#{datatype}">{text}</bp:stoichiometricCoefficient></bp:Stoichiometry>'
            )

        self.assertEqual(self.counts([coefficient("1")], [coefficient("1.0")]), (0, 0))
        self.assertEqual(self.counts([coefficient("1")], [coefficient("2")]), (1, 1))
        self.assertEqual(self.counts([coefficient("1")], [coefficient("1", "string")]), (1, 1))

    def test_whitespace_in_a_literal_counts(self):
        def named(text):
            return f'<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:name>{text}</bp:name></bp:Protein>'

        self.assertEqual(
            self.counts([dbid("X1", 1), named("a b")], [dbid("X1", 1), named("a  b")]), (1, 1)
        )

    def test_a_literal_is_not_a_reference(self):
        one = [
            dbid("X1", 1),
            dbid("X2", 2),
            protein("P2", "X2"),
            '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:comment>Protein|dbId:2</bp:comment></bp:Protein>',
        ]
        other = one[:3] + [
            '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:comment rdf:resource="#P2" /></bp:Protein>'
        ]
        self.assertEqual(self.counts(one, other), (1, 1))

    def test_a_literal_is_not_a_reference_inside_a_key(self):
        # A Stoichiometry is keyed by its content: a literal spelling what a
        # reference would must still key it apart, so its referrer differs too.
        def part(entity):
            return [
                dbid("X1", 1),
                dbid("X2", 2),
                protein("P2", "X2"),
                f'<bp:Stoichiometry rdf:ID="S1"><bp:physicalEntity{entity}</bp:Stoichiometry>',
                '<bp:Complex rdf:ID="C1"><bp:xref rdf:resource="#X1" /><bp:componentStoichiometry rdf:resource="#S1" /></bp:Complex>',
            ]

        self.assertEqual(
            self.counts(
                part(">&gt;Protein|dbId:2</bp:physicalEntity>"), part(' rdf:resource="#P2" />')
            ),
            (2, 2),
        )

    def test_several_database_ids_in_any_order_are_one_key(self):
        def two(first, second):
            return [
                dbid("X1", 1),
                dbid("X2", 2),
                f'<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#{first}" /><bp:xref rdf:resource="#{second}" />'
                "<bp:name>n</bp:name></bp:Protein>",
            ]

        self.assertEqual(self.counts(two("X1", "X2"), two("X2", "X1")), (0, 0))
        (subject,) = {
            t[0]
            for t in compare.triples(self.write(*two("X2", "X1")))
            if t[0].startswith("Protein")
        }
        self.assertEqual(subject, "Protein|dbId:1,2")

    def test_only_a_reactome_unification_xref_is_a_database_id(self):
        other = (
            '<bp:RelationshipXref rdf:ID="X9"><bp:db>Reactome Database ID Release 97</bp:db>'
            "<bp:id>9</bp:id></bp:RelationshipXref>"
        )
        uniprot = '<bp:UnificationXref rdf:ID="X8"><bp:db>UniProt</bp:db><bp:id>9</bp:id></bp:UnificationXref>'
        for xref in (other, uniprot):
            keys = {
                t[0] for t in compare.triples(self.write(xref, protein("P1", xref.split('"')[1])))
            }
            self.assertFalse(any("dbId:" in k for k in keys), xref)

    def test_whatever_controls_is_keyed_by_what_it_controls(self):
        for cls in ("Catalysis", "Modulation", "TemplateReactionRegulation"):

            def control(name, reaction, controller, cls=cls):
                return (
                    f'<bp:{cls} rdf:ID="{name}"><bp:xref rdf:resource="#X8" /><bp:controlled rdf:resource="#{reaction}" />'
                    f'<bp:controller rdf:resource="#{controller}" /></bp:{cls}>'
                )

            common = [part for part in BASE if "bp:Control" not in part]
            one = common + [control("K1", "R1", "P1"), control("K2", "R2", "P2")]
            other = common + [control("K1", "R1", "P2"), control("K2", "R2", "P1")]
            self.assertEqual(self.counts(one, other), (2, 2), cls)

    def test_names_are_hashed_with_a_salt(self):
        hashed = compare.literal("author", "N", None)[1]
        self.assertNotIn(compare.digest("N"), hashed)
        self.assertEqual(hashed, compare.literal("author", "N", None)[1])

    def test_an_individual_named_by_a_relative_about_is_the_one_its_id_names(self):
        about = [part.replace('rdf:ID="P1"', 'rdf:about="#P1"') for part in BASE]
        self.assertNotEqual(about, BASE)
        self.assertEqual(sum(sum(side.values()) for side in self.compared(about)), 0)

    def typed(self, text, datatype):
        kind = f' rdf:datatype="http://www.w3.org/2001/XMLSchema#{datatype}"' if datatype else ""
        return [
            f'<bp:Stoichiometry rdf:ID="S1"><bp:stoichiometricCoefficient{kind}>{text}</bp:stoichiometricCoefficient></bp:Stoichiometry>'
        ]

    def test_an_integer_is_compared_by_value(self):
        self.assertEqual(self.counts(self.typed("+01", "int"), self.typed("1", "int")), (0, 0))
        self.assertEqual(self.counts(self.typed("1_0", "int"), self.typed("10", "int")), (1, 1))

    def test_a_float_is_compared_as_a_float_holds_it(self):
        self.assertEqual(
            self.counts(self.typed("0.1", "float"), self.typed("0.10000000149011612", "float")),
            (0, 0),
        )
        self.assertEqual(
            self.counts(self.typed("0.1", "double"), self.typed("0.10000000149011612", "double")),
            (1, 1),
        )
        self.assertEqual(self.counts(self.typed("1", "float"), self.typed("1", "double")), (1, 1))
        self.assertEqual(self.counts(self.typed("1_0", "float"), self.typed("10", "float")), (1, 1))

    def test_a_datatype_is_part_of_a_literal(self):
        self.assertEqual(self.counts(self.typed("a", "string"), self.typed("a", None)), (1, 1))
        self.assertEqual(
            self.counts(self.typed("a", "string"), self.typed("a^^string", None)), (1, 1)
        )
        # And of the key it is in: the complex that holds it differs too.
        holder = [
            dbid("X1", 1),
            '<bp:Complex rdf:ID="C1"><bp:xref rdf:resource="#X1" /><bp:componentStoichiometry rdf:resource="#S1" /></bp:Complex>',
        ]
        self.assertEqual(
            self.counts(self.typed("a", "string") + holder, self.typed("a", None) + holder), (2, 2)
        )

    def test_a_value_repeated_within_one_individual_counts_once(self):
        def named(*names):
            return [
                dbid("X1", 1),
                '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" />'
                + "".join(f"<bp:name>{n}</bp:name>" for n in names)
                + "</bp:Protein>",
            ]

        self.assertEqual(self.counts(named("a", "a"), named("a")), (0, 0))
        self.assertEqual(self.counts(named("a", "b"), named("a")), (1, 0))

    def test_refuses_a_cycle_of_individuals_keyed_by_content(self):
        cycle = [
            '<bp:Evidence rdf:ID="A"><bp:xref rdf:resource="#B" /></bp:Evidence>',
            '<bp:Evidence rdf:ID="B"><bp:xref rdf:resource="#A" /></bp:Evidence>',
        ]
        with self.assertRaises(compare.Refused):
            compare.triples(self.write(*cycle))

    def location(self, go):
        return [
            dbid("X1", 1),
            dbid("X2", 2),
            f'<bp:UnificationXref rdf:ID="G1"><bp:db>GO</bp:db><bp:id>{go}</bp:id></bp:UnificationXref>',
            '<bp:CellularLocationVocabulary rdf:ID="L1"><bp:term>cytosol</bp:term><bp:xref rdf:resource="#G1" /></bp:CellularLocationVocabulary>',
            '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:cellularLocation rdf:resource="#L1" /></bp:Protein>',
            '<bp:Protein rdf:ID="P2"><bp:xref rdf:resource="#X2" /><bp:cellularLocation rdf:resource="#L1" /></bp:Protein>',
        ]

    def roots(self, one, other):
        """The kinds at the root, each side, and the report's lines."""
        one, other = compare.triples(self.write(*one)), compare.triples(self.write(*other))
        sides, _ = compare.at_the_root(one, other)
        return [sorted(compare.kind(t) for t in side.elements()) for side in sides], compare.report(
            one, other, examples=1
        )

    def test_names_the_change_at_the_root_apart_from_what_it_carries(self):
        (gone, came), lines = self.roots(self.location("1"), self.location("2"))
        self.assertEqual((gone, came), (["UnificationXref.id"], ["UnificationXref.id"]))
        self.assertIn(" at the root: 2", lines)
        self.assertIn(
            " not at the root (carried by a rehashed key, or swapped among content keys): 10", lines
        )
        self.assertTrue(lines[-1].startswith("    + 1 x UnificationXref|GO|2|"), lines[-1])

    def test_a_change_of_its_own_stays_at_the_root_beside_a_carried_one(self):
        # The vocabulary's term changes, and so does the xref it points at:
        # both are where the files part.
        changed = [
            part.replace("<bp:term>cytosol", "<bp:term>nucleus") for part in self.location("2")
        ]
        (gone, came), _ = self.roots(self.location("1"), changed)
        self.assertEqual(came, ["CellularLocationVocabulary.term", "UnificationXref.id"])

    def test_an_event_added_is_at_the_root(self):
        reaction = '<bp:BiochemicalReaction rdf:ID="R9"><bp:xref rdf:resource="#X9" /><bp:left rdf:resource="#P1" /></bp:BiochemicalReaction>'
        (gone, came), _ = self.roots(BASE, BASE + [dbid("X9", 99), reaction])
        self.assertEqual(gone, [])
        self.assertEqual(
            came,
            [
                "BiochemicalReaction.left",
                "BiochemicalReaction.xref",
                "UnificationXref.db",
                "UnificationXref.id",
            ],
        )

    def test_a_re_pointing_between_events_or_entities_is_at_the_root(self):
        changed = [
            part.replace('<bp:left rdf:resource="#P1" />', '<bp:left rdf:resource="#P2" />')
            for part in BASE
        ]
        (gone, came), _ = self.roots(BASE, changed)
        self.assertEqual((gone, came), (["BiochemicalReaction.left"], ["BiochemicalReaction.left"]))

    def test_a_re_pointing_between_individuals_both_files_have_is_at_the_root(self):
        # A step's next step changed to another step both files have: the
        # steps are keyed by content, but neither key is new.
        changed = [
            part.replace('<bp:nextStep rdf:resource="#W1" />', '<bp:nextStep rdf:resource="#W2" />')
            for part in BASE
        ]
        (gone, came), _ = self.roots(BASE, changed)
        self.assertEqual((gone, came), (["PathwayStep.nextStep"], ["PathwayStep.nextStep"]))

    def stoichiometries(self, first, second, entity="P1"):
        return [
            dbid("X1", 1),
            dbid("X2", 2),
            protein("P1", "X1"),
            protein("P2", "X2"),
            f'<bp:Stoichiometry rdf:ID="S1"><bp:physicalEntity rdf:resource="#P1" /><bp:stoichiometricCoefficient>{first}</bp:stoichiometricCoefficient></bp:Stoichiometry>',
            f'<bp:Stoichiometry rdf:ID="S2"><bp:physicalEntity rdf:resource="#{entity}" /><bp:stoichiometricCoefficient>{second}</bp:stoichiometricCoefficient></bp:Stoichiometry>',
            '<bp:Complex rdf:ID="C1"><bp:xref rdf:resource="#X1" /><bp:componentStoichiometry rdf:resource="#S1" /></bp:Complex>',
            '<bp:Complex rdf:ID="C2"><bp:xref rdf:resource="#X2" /><bp:componentStoichiometry rdf:resource="#S2" /></bp:Complex>',
        ]

    def test_a_swap_between_individuals_both_files_have_is_at_the_root(self):
        (gone, came), _ = self.roots(self.stoichiometries("1", "2"), self.stoichiometries("2", "1"))
        self.assertEqual(
            (gone, came),
            (["Complex.componentStoichiometry"] * 2, ["Complex.componentStoichiometry"] * 2),
        )

    def test_a_swap_beside_a_root_change_is_not_called_carried(self):
        one, other = self.stoichiometries("1", "2", "P2"), self.stoichiometries("2", "1", "P2")
        other = [
            part.replace(
                '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" />',
                '<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:comment>c</bp:comment>',
            )
            for part in other
        ]
        (gone, came), lines = self.roots(one, other)
        self.assertEqual(came, ["Protein.comment"])
        self.assertIn(
            " not at the root (carried by a rehashed key, or swapped among content keys): 12", lines
        )

    def test_identical_files_say_nothing_of_a_root(self):
        _, lines = self.roots(BASE, BASE)
        self.assertEqual(lines, ["only in the reference: 0", "only in the file: 0"])

    def test_a_changed_individual_nothing_points_at_is_at_the_root_alone(self):
        # Its key is new, though nothing points at it: coarsened, its unchanged
        # physicalEntity is not at the root.
        alone = [
            dbid("X1", 1),
            protein("P1", "X1"),
            '<bp:Stoichiometry rdf:ID="S9"><bp:physicalEntity rdf:resource="#P1" /><bp:stoichiometricCoefficient>{}</bp:stoichiometricCoefficient></bp:Stoichiometry>',
        ]
        (gone, came), _ = self.roots(
            alone[:2] + [alone[2].format(1)], alone[:2] + [alone[2].format(2)]
        )
        self.assertEqual(
            (gone, came),
            (
                ["Stoichiometry.stoichiometricCoefficient"],
                ["Stoichiometry.stoichiometricCoefficient"],
            ),
        )

    def test_a_dangling_reference_re_pointed_is_at_the_root(self):
        def dangling(target):
            return [
                HEAD.replace(' xml:base="http://www.reactome.org/biopax/97/1#"', "")
                + dbid("X1", 1)
                + f'<bp:Protein rdf:ID="P1"><bp:xref rdf:resource="#X1" /><bp:cellularLocation rdf:resource="#{target}" /></bp:Protein></rdf:RDF>'
            ]

        one, other = (self.write_raw(*dangling(t)) for t in ("A", "B"))
        sides, _ = compare.at_the_root(compare.triples(one), compare.triples(other))
        self.assertEqual([sum(side.values()) for side in sides], [1, 1])

    def write_raw(self, text):
        handle, path = tempfile.mkstemp(suffix=".owl")
        with os.fdopen(handle, "w") as f:
            f.write(text)
        self.addCleanup(os.remove, path)
        return path

    def test_a_swap_that_makes_new_keys_is_said_to_have_no_root(self):
        one, other = self.stoichiometries("1", "2", "P2"), self.stoichiometries("2", "1", "P2")
        (gone, came), lines = self.roots(one, other)
        self.assertEqual((gone, came), ([], []))
        self.assertNotEqual(self.counts(one, other), (0, 0))
        self.assertIn(
            " nothing differs at the root: what differs is swapped among individuals keyed by content",
            lines,
        )
        self.assertIn(
            " not at the root (carried by a rehashed key, or swapped among content keys): 12", lines
        )

    def test_a_database_id_key_is_never_coarsened(self):
        # Two Catalyses with database ids control one reaction keyed by
        # content. The reaction changes, so their keys (which name it) are new
        # in the other file; and their controllers are swapped. Coarsened,
        # the swap would vanish; a database-id key stays exact, so it shows.
        def catalyses(first, second, side="P1"):
            return [
                dbid("X1", 1),
                dbid("X2", 2),
                dbid("X3", 3),
                protein("P1", "X1"),
                protein("P2", "X2"),
                f'<bp:BiochemicalReaction rdf:ID="R1"><bp:left rdf:resource="#{side}" /></bp:BiochemicalReaction>',
                f'<bp:Catalysis rdf:ID="K1"><bp:xref rdf:resource="#X3" /><bp:controlled rdf:resource="#R1" /><bp:controller rdf:resource="#{first}" /></bp:Catalysis>',
                f'<bp:Catalysis rdf:ID="K2"><bp:xref rdf:resource="#X1" /><bp:controlled rdf:resource="#R1" /><bp:controller rdf:resource="#{second}" /></bp:Catalysis>',
            ]

        (gone, came), _ = self.roots(catalyses("P1", "P2"), catalyses("P2", "P1", side="P2"))
        self.assertEqual(gone.count("Catalysis.controller"), 2)
        self.assertEqual(came.count("Catalysis.controller"), 2)

    def test_a_coefficient_changed_is_at_the_root_and_its_holder_carried(self):
        changed = [
            part.replace(">2</bp:stoichiometricCoefficient>", ">3</bp:stoichiometricCoefficient>")
            for part in BASE
        ]
        (gone, came), lines = self.roots(BASE, changed)
        self.assertEqual(
            (gone, came),
            (
                ["Stoichiometry.stoichiometricCoefficient"],
                ["Stoichiometry.stoichiometricCoefficient"],
            ),
        )
        self.assertIn(
            " not at the root (carried by a rehashed key, or swapped among content keys): 4", lines
        )

    def test_free_text_is_never_printed(self):
        for prop in ("name", "displayName", "url", "comment", "title", "source", "standardName"):
            self.assertIn(
                "chars", compare.shown(("Protein|dbId:1", prop, ("l", "Words", "string"))), prop
            )
        for prop in ("db", "id", "term"):
            self.assertIn(
                "Words", compare.shown(("Protein|dbId:1", prop, ("l", "Words", "string"))), prop
            )

    def test_a_number_never_equals_text(self):
        self.assertEqual(
            self.counts(self.typed("inf", "float"), self.typed("INF", "float")), (1, 1)
        )
        # Nor inside a key: the complex holding it differs too.
        holder = [
            dbid("X1", 1),
            '<bp:Complex rdf:ID="C1"><bp:xref rdf:resource="#X1" /><bp:componentStoichiometry rdf:resource="#S1" /></bp:Complex>',
        ]
        self.assertEqual(
            self.counts(self.typed("inf", "float") + holder, self.typed("INF", "float") + holder),
            (2, 2),
        )
        self.assertEqual(
            self.counts(self.typed("+INF", "float"), self.typed("INF", "float")), (0, 0)
        )
        self.assertEqual(self.counts(self.typed(" 1", "long"), self.typed("1", "long")), (0, 0))
        self.assertEqual(self.counts(self.typed("1", "short"), self.typed("01", "short")), (0, 0))

    def test_a_decimal_is_compared_exactly(self):
        self.assertEqual(
            self.counts(self.typed("0.1", "decimal"), self.typed("0.10", "decimal")), (0, 0)
        )
        self.assertEqual(
            self.counts(
                self.typed("0.1", "decimal"), self.typed("0.1000000000000000000001", "decimal")
            ),
            (1, 1),
        )
        self.assertEqual(
            self.counts(
                self.typed("1", "decimal"), self.typed("1.00000000000000000000000000001", "decimal")
            ),
            (1, 1),
        )
        self.assertEqual(
            self.counts(self.typed("-0", "decimal"), self.typed("0.0", "decimal")), (0, 0)
        )
        self.assertEqual(
            self.counts(self.typed("007.50", "decimal"), self.typed("+7.5", "decimal")), (0, 0)
        )

    def test_a_decimal_keeps_its_sign_and_refuses_what_is_not_one(self):
        self.assertEqual(
            self.counts(self.typed("-1.5", "decimal"), self.typed("1.5", "decimal")), (1, 1)
        )
        for text in (".", "+", "1e5", "1.2.3"):
            self.assertIsNone(compare.number(text, "decimal"), text)

    def test_a_non_ascii_number_is_text_and_kept_whole(self):
        self.assertEqual(
            self.counts(self.typed("\u0663.50", "decimal"), self.typed("\u0663.5", "decimal")),
            (1, 1),
        )
        self.assertEqual(
            self.counts(self.typed("0\u0663", "int"), self.typed("\u0663", "int")), (1, 1)
        )

    def test_only_ascii_space_is_stripped(self):
        self.assertEqual(self.counts(self.typed("7\u2003", "int"), self.typed("7", "int")), (1, 1))
        self.assertEqual(self.counts(self.typed(" 7\n", "int"), self.typed("7", "int")), (0, 0))

    def test_an_integer_with_many_leading_zeros_is_read(self):
        self.assertEqual(compare.number("-" + "0" * 5000 + "12", "int"), "-12")
        self.assertEqual(compare.number("-0000", "int"), "0")

    def test_only_ascii_digits_make_a_number(self):
        for datatype in ("int", "decimal", "float"):
            self.assertEqual(
                self.counts(self.typed("\u0663", datatype), self.typed("3", datatype)),
                (1, 1),
                datatype,
            )

    def test_an_integer_is_read_up_to_forty_digits(self):
        self.assertEqual(
            self.counts(self.typed("0" * 50 + "1", "int"), self.typed("1", "int")), (0, 0)
        )
        self.assertEqual(
            self.counts(self.typed("1" + "0" * 40, "int"), self.typed("1" + "0" * 40, "int")),
            (0, 0),
        )
        self.assertEqual(compare.number("1" + "0" * 39, "int"), "1" + "0" * 39)
        self.assertIsNone(compare.number("1" + "0" * 40, "int"))

    def test_a_number_prints_as_it_is(self):
        self.assertEqual(
            compare.shown(("Protein|dbId:1", "comment", ("n", "1.5", "float"))), "'1.5'^^float"
        )

    def test_a_database_id_written_as_a_number_is_one(self):
        typed = (
            '<bp:UnificationXref rdf:ID="X1"><bp:db>Reactome Database ID Release 97</bp:db>'
            '<bp:id rdf:datatype="http://www.w3.org/2001/XMLSchema#int">11</bp:id></bp:UnificationXref>'
        )
        keys = {t[0] for t in compare.triples(self.write(typed, protein("P1", "X1")))}
        self.assertIn("Protein|dbId:11", keys)

    def test_a_file_it_cannot_read_through_exits_2(self):
        same = self.write(*BASE)
        real = compare.triples
        for failure in (RecursionError(), ValueError()):

            def broken(path, failure=failure):
                raise failure

            compare.triples = broken
            try:
                with (
                    contextlib.redirect_stdout(io.StringIO()),
                    contextlib.redirect_stderr(io.StringIO()),
                ):
                    self.assertEqual(compare.main(["compare.py", same, same]), 2)
            finally:
                compare.triples = real

    def test_an_example_prints_no_free_text(self):
        def cited(title):
            return [
                dbid("X1", 1),
                '<bp:PublicationXref rdf:ID="B1"><bp:db>ISBN</bp:db><bp:id>123</bp:id>'
                f"<bp:title>{title}</bp:title><bp:source>{title}</bp:source></bp:PublicationXref>",
                '<bp:Pathway rdf:ID="E1"><bp:xref rdf:resource="#X1" /><bp:xref rdf:resource="#B1" />'
                f"<bp:comment>{title}</bp:comment></bp:Pathway>",
            ]

        one, other = (
            compare.triples(self.write(*cited("Words One"))),
            compare.triples(self.write(*cited("Words Two"))),
        )
        text = "\n".join(compare.report(one, other, examples=20))
        self.assertNotIn("Words", text)
        self.assertIn("'123'", text)

    def test_refuses_an_individual_nested_in_another(self):
        path = self.write(
            '<bp:Protein rdf:ID="P1"><bp:xref><bp:UnificationXref rdf:ID="X1" /></bp:xref></bp:Protein>'
        )
        with self.assertRaises(compare.Refused):
            compare.triples(path)

    def test_refuses_two_individuals_of_one_name(self):
        with self.assertRaises(compare.Refused):
            compare.triples(self.write(protein("P1", "X1"), protein("P1", "X2")))

    def test_refuses_what_it_does_not_read(self):
        for part in (
            '<bp:Protein rdf:nodeID="n1"><bp:name>a</bp:name></bp:Protein>',
            '<bp:Protein rdf:ID="P1"><bp:xref rdf:nodeID="n1" /></bp:Protein>',
            '<bp:Protein rdf:ID="P1" bp:displayName="a" />',
            '<bp:Protein rdf:ID="P1"><bp:name xml:lang="en">a</bp:name></bp:Protein>',
        ):
            with self.assertRaises(compare.Refused, msg=part):
                compare.triples(self.write(part))

    def test_exits_by_whether_the_files_differ(self):
        same, other = self.write(*BASE), self.write(*BASE[:-1])
        refused = self.write('<bp:Protein rdf:nodeID="n1" />')
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(compare.main(["compare.py", same, same]), 0)
            self.assertEqual(compare.main(["compare.py", same, other]), 1)
            self.assertEqual(compare.main(["compare.py", same]), 2)
            self.assertEqual(compare.main(["compare.py", same, refused]), 2)
            self.assertEqual(compare.main(["compare.py", same, same + ".missing"]), 2)


if __name__ == "__main__":
    unittest.main()
