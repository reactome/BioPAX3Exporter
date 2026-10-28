"""Tests for ci/validate.py: python3 -I -m unittest discover -s ci"""

import contextlib
import io
import json
import os
import tempfile
import unittest

import validate


def report(*errors, not_fixed_errors=None, not_fixed=None):
    """A validator report with the given (type, code, cases) errors, each with a case's message."""
    cases = sum(n for kind, _, n in errors if kind == "ERROR")
    nfe = cases if not_fixed_errors is None else not_fixed_errors
    body = "".join(
        f'<error type="{kind}" code="{code}" category="x" message="m" totalCases="{n}" '
        f'notFixedCases="{n if not_fixed is None else not_fixed}">'
        f'<errorCase fixed="false"><message>A Person Name</message></errorCase></error>'
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
        self.assertFalse(any("Person" in line for line in lines))

    def test_refuses_a_report_with_no_validation(self):
        path = self.write('<?xml version="1.0"?><validatorResponse/>')
        with self.assertRaises(ValueError):
            validate.check([path], {})

    def test_counts_an_error_whatever_its_not_fixed_count_says(self):
        path = self.write(report(("ERROR", "e", 2), not_fixed_errors=0, not_fixed=0))
        lines, failed = validate.check([path], {})
        self.assertTrue(failed)
        self.assertIn("  ERROR e 2", lines)

    def test_adds_up_a_code_repeated_across_validations(self):
        one = report(("ERROR", "e", 1))
        two = one.replace(
            "</validatorResponse>", one.split("<validatorResponse")[1].split(">", 1)[1]
        )
        lines, failed = validate.check([self.write(two)], {"e": 1})
        self.assertTrue(failed)
        self.assertIn("  ERROR e 2", lines)

    def test_reads_a_baseline_file(self):
        path = self.write(report(("ERROR", "e", 1)))
        handle, base = tempfile.mkstemp(suffix=".json")
        with os.fdopen(handle, "w") as f:
            json.dump({"e": 1}, f)
        self.addCleanup(os.remove, base)
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(validate.main(["validate.py", "--baseline", base, path]), 0)


if __name__ == "__main__":
    unittest.main()
