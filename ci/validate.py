"""The BioPAX validator's XML reports, as counts per code (spec 001 T004).

Usage: python3 -I ci/validate.py [--baseline baseline.json] REPORT...

Each REPORT is a validator 6.0.0 report (`--out-format=xml`, one
`{file}.validation.xml` per file). The validator's exit status says nothing,
so this reads the report: every code with its type and count, and the totals.
It fails on a case of type ERROR beyond the baseline, a JSON object of code to
the number of not-fixed ERROR cases allowed (none given: none allowed).

It prints codes and counts only. The validator's messages and its log echo
display names, which can be people's names (constitution VI).
"""

import json
import sys
from xml.etree import ElementTree


def local(tag):
    return tag.rsplit("}", 1)[-1]


def read(path):
    """{(type, code): cases} and the validations' notFixedErrors, of one report.

    A code's cases are the larger of its totalCases and notFixedCases: with
    fixing off (as run) they are equal, and a report where they are not is
    read so that no case goes uncounted. A code repeated across validations
    adds up. A report with no validation at all is refused: an empty or
    partial report is not a clean one.
    """
    root = ElementTree.parse(path).getroot()
    validations = [element for element in root.iter() if local(element.tag) == "validation"]
    if not validations:
        raise ValueError(f"{path}: no validation in the report")
    codes, not_fixed_errors = {}, 0
    for validation in validations:
        not_fixed_errors += int(validation.get("notFixedErrors", "0"))
        for error in validation:
            if local(error.tag) == "error":
                key = (error.get("type", ""), error.get("code", ""))
                cases = max(int(error.get("totalCases", "0")), int(error.get("notFixedCases", "0")))
                codes[key] = codes.get(key, 0) + cases
    return codes, not_fixed_errors


def check(paths, baseline):
    """Lines to print, and whether any ERROR goes beyond the baseline."""
    lines, failed = [], False
    for path in paths:
        codes, not_fixed_errors = read(path)
        errors = {code: count for (kind, code), count in codes.items() if kind == "ERROR"}
        warnings = sum(count for (kind, _), count in codes.items() if kind == "WARNING")
        lines.append(
            f"{path}: {sum(errors.values())} errors, {warnings} warnings, {len(codes)} codes"
        )
        for (kind, code), count in sorted(codes.items()):
            lines.append(f"  {kind} {code} {count}")
        over = {code: count for code, count in errors.items() if count > baseline.get(code, 0)}
        allowed = sum(min(count, baseline.get(code, 0)) for code, count in errors.items())
        for code, count in sorted(over.items()):
            lines.append(
                f"  over the baseline: ERROR {code} {count} (allowed {baseline.get(code, 0)})"
            )
        # notFixedErrors counts ERROR cases: a report saying more than its codes do fails too.
        if not_fixed_errors > allowed and not over:
            lines.append(
                f"  over the baseline: {not_fixed_errors} errors by the report's own count"
            )
        failed = failed or bool(over) or not_fixed_errors > allowed
    return lines, failed


def main(argv):
    args = argv[1:]
    baseline = {}
    if args[:1] == ["--baseline"]:
        if len(args) < 2:
            print(__doc__.strip(), file=sys.stderr)
            return 2
        with open(args[1], encoding="utf-8") as f:
            baseline = json.load(f)
        if not isinstance(baseline, dict) or not all(isinstance(n, int) for n in baseline.values()):
            print(f"{args[1]}: a baseline is an object of code to a number", file=sys.stderr)
            return 2
        args = args[2:]
    if not args:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    try:
        lines, failed = check(args, baseline)
    except ValueError as refused:
        print(refused, file=sys.stderr)
        return 1
    print("\n".join(lines))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
