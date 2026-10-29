"""Tests for ci/contained.py: python3 -I -m unittest discover -s ci"""

import contextlib
import io
import os
import tempfile
import unittest

import contained
from test_compare import dbid, document, protein

# A species' file of two reactions; an event's file of one of them, with other ids.
SPECIES = document(
    dbid("X1", 11),
    dbid("X2", 12),
    dbid("X3", 13),
    protein("P1", "X1"),
    protein("P2", "X2"),
    '<bp:BiochemicalReaction rdf:ID="R1"><bp:xref rdf:resource="#X3" /><bp:left rdf:resource="#P1" /></bp:BiochemicalReaction>',
)
EVENT = document(
    dbid("A", 11),
    dbid("B", 13),
    protein("Q", "A"),
    '<bp:BiochemicalReaction rdf:ID="R9"><bp:xref rdf:resource="#B" /><bp:left rdf:resource="#Q" /></bp:BiochemicalReaction>',
)
# The same reaction without its participant: contained, but not whole
NARROWER = EVENT.replace('<bp:left rdf:resource="#Q" />', "")
# The same protein twice over: one more of its triples than the species' file has
TWICE = EVENT.replace(protein("Q", "A"), protein("Q", "A") + protein("Q2", "A"))
# A species' file holding the protein twice over, of which an event holds it once
SPECIES_TWICE = SPECIES.replace(protein("P1", "X1"), protein("P1", "X1") + protein("P1b", "X1"))
# The same reaction with a participant the species' file does not give it
WIDER = EVENT.replace(
    '<bp:left rdf:resource="#Q" />', '<bp:left rdf:resource="#Q" /><bp:right rdf:resource="#Q" />'
)


class ContainedTest(unittest.TestCase):
    def run_on(self, *texts):
        paths = []
        with tempfile.TemporaryDirectory() as folder:
            for n, text in enumerate(texts):
                path = os.path.join(folder, f"f{n}.owl")
                with open(path, "w") as out:
                    out.write(text)
                paths.append(path)
            printed = io.StringIO()
            with contextlib.redirect_stdout(printed):
                status = contained.main(["contained.py", *paths])
        return status, printed.getvalue()

    def test_an_event_its_species_holds_is_contained(self):
        status, printed = self.run_on(SPECIES, EVENT)
        self.assertEqual(0, status)
        self.assertIn("0 not in the species' file", printed)
        self.assertIn("0 lacking", printed)

    def test_a_triple_the_species_lacks_is_counted_and_fails(self):
        status, printed = self.run_on(SPECIES, EVENT, WIDER)
        self.assertEqual(1, status)
        self.assertIn("1 not in the species' file", printed)
        self.assertIn("BiochemicalReaction.right 1", printed)

    def test_an_event_missing_its_own_triples_is_not_whole(self):
        status, printed = self.run_on(SPECIES, NARROWER)
        self.assertEqual(1, status)
        self.assertIn("0 not in the species' file", printed)
        self.assertIn("1 lacking", printed)
        self.assertIn("- BiochemicalReaction.left 1", printed)

    def test_a_triple_held_more_often_than_in_the_species_counts(self):
        status, printed = self.run_on(SPECIES, TWICE)
        self.assertEqual(1, status)
        self.assertIn("1 not in the species' file", printed)

    def test_an_individual_held_less_often_than_in_the_species_is_not_whole(self):
        status, printed = self.run_on(SPECIES_TWICE, EVENT)
        self.assertEqual(1, status)
        self.assertIn("0 not in the species' file", printed)
        self.assertIn("1 lacking", printed)

    def test_a_file_that_cannot_be_read(self):
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(
                2, contained.main(["contained.py", "/nonexistent.owl", "/nonexistent.owl"])
            )


if __name__ == "__main__":
    unittest.main()
