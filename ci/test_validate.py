"""Tests for ci/validate.py: python3 -I -m unittest discover -s ci"""
import json
import os
import tempfile
import unittest

import validate


def report(*errors, not_fixed_errors=None):
    """A validator report with the given (type, code, cases) errors."""
    cases = sum(n for kind, _, n in errors if kind == "ERROR")
    nfe = cases if not_fixed_errors is None else not_fixed_errors
    body = "".join(
        f'<error type="{kind}" code="{code}" category="x" message="m" totalCases="{n}" notFixedCases="{n}"/>'
        for kind, code, n in errors
    )
    return (
        '<?xml version="1.0"?><validatorResponse xmlns="http://biopax.org/validator/2.0/schema">'
        f'<validation notFixedErrors="{nfe}" profile="notstrict">{body}</validation></validatorResponse>'
    )


class ValidateTest(unittest.TestCase):
    def write(self, text):
        handle, path = tempfile.mkstemp(suffix=".validation.xml")
        with os.fdopen(handle, "w") as f:
            f.write(text)
        self.addCleanup(os.remove, path)
        return path

    def test_passes_on_warnings_only(self):
        lines, failed = validate.check([self.write(report(("WARNING", "unknown.db", 5)))], {})
        self.assertFalse(failed)
        self.assertIn("0 errors, 5 warnings, 1 codes", lines[0])

    def test_fails_on_an_error(self):
        lines, failed = validate.check([self.write(report(("ERROR", "syntax.error", 1)))], {})
        self.assertTrue(failed)
        self.assertIn("  over the baseline: ERROR syntax.error 1 (allowed 0)", lines)

    def test_allows_errors_up_to_the_baseline_and_no_more(self):
        path = self.write(report(("ERROR", "cardinality.violated", 2)))
        self.assertFalse(validate.check([path], {"cardinality.violated": 2})[1])
        self.assertTrue(validate.check([path], {"cardinality.violated": 1})[1])

    def test_fails_when_the_summary_counts_errors_the_codes_do_not(self):
        path = self.write(report(("WARNING", "x", 1), not_fixed_errors=3))
        self.assertTrue(validate.check([path], {})[1])

    def test_prints_no_message(self):
        lines, _ = validate.check([self.write(report(("WARNING", "no.display.name", 1)))], {})
        self.assertFalse(any(" m" in line.split("no.display.name")[-1] for line in lines))

    def test_reads_a_baseline_file(self):
        path = self.write(report(("ERROR", "e", 1)))
        handle, base = tempfile.mkstemp(suffix=".json")
        with os.fdopen(handle, "w") as f:
            json.dump({"e": 1}, f)
        self.addCleanup(os.remove, base)
        self.assertEqual(validate.main(["validate.py", "--baseline", base, path]), 0)


if __name__ == "__main__":
    unittest.main()
