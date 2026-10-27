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
    """[(type, code, not-fixed cases)] and the validation's notFixedErrors, of one report."""
    root = ElementTree.parse(path).getroot()
    codes, not_fixed_errors = [], 0
    for validation in root.iter():
        if local(validation.tag) != "validation":
            continue
        not_fixed_errors += int(validation.get("notFixedErrors", "0"))
        for error in validation:
            if local(error.tag) == "error":
                codes.append((error.get("type", ""), error.get("code", ""), int(error.get("notFixedCases", "0"))))
    return codes, not_fixed_errors


def check(paths, baseline):
    """Lines to print, and whether any ERROR goes beyond the baseline."""
    lines, failed = [], False
    for path in paths:
        codes, not_fixed_errors = read(path)
        errors = {code: count for kind, code, count in codes if kind == "ERROR"}
        warnings = sum(count for kind, _, count in codes if kind == "WARNING")
        lines.append(f"{path}: {sum(errors.values())} errors, {warnings} warnings, {len(codes)} codes")
        for kind, code, count in sorted(codes):
            lines.append(f"  {kind} {code} {count}")
        over = {code: count for code, count in errors.items() if count > baseline.get(code, 0)}
        # notFixedErrors counts ERROR cases; a report saying more than its codes do is read as failing.
        if over or not_fixed_errors > sum(min(count, baseline.get(code, 0)) for code, count in errors.items()):
            failed = True
            for code, count in sorted(over.items()):
                lines.append(f"  over the baseline: ERROR {code} {count} (allowed {baseline.get(code, 0)})")
    return lines, failed


def main(argv):
    args = argv[1:]
    baseline = {}
    if args[:1] == ["--baseline"]:
        with open(args[1], encoding="utf-8") as f:
            baseline = json.load(f)
        args = args[2:]
    if not args:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    lines, failed = check(args, baseline)
    print("\n".join(lines))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
