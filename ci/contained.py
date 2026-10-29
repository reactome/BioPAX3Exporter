"""Is each event's file contained in its species' file? (spec 001 T021).

Usage: python3 -I ci/contained.py SPECIES EVENT...

Reads SPECIES once, as ci/compare.py reduces a file to triples, and then each
EVENT file: an event converted alone should hold nothing its species' file
does not. Prints, for each EVENT, its triples and how many are not in the
species' file, with the kinds of those (class and property) and counts. Exits
0 when every EVENT is contained, 1 when one is not, and 2 when a file cannot
be read.

It prints kinds and counts only, never a literal (constitution VI).
"""

import collections
import sys
from pathlib import Path
from xml.etree import ElementTree

sys.path.insert(0, str(Path(__file__).resolve().parent))
import compare  # noqa: E402


def main(argv):
    if len(argv) < 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    try:
        species = compare.triples(argv[1])
        status = 0
        for path in argv[2:]:
            event = compare.triples(path)
            outside = event - species
            kinds = collections.Counter(compare.kind(t) for t in outside.elements())
            print(
                f"{Path(path).name}: {sum(event.values())} triples, {sum(outside.values())} not in the species' file"
            )
            for kind, count in kinds.most_common():
                print(f"  {kind} {count}")
            if outside:
                status = 1
        return status
    except (OSError, ElementTree.ParseError, ValueError, RecursionError) as failure:
        print(f"cannot compare: {failure}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
