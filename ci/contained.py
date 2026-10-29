"""Is each event's file contained in its species' file? (spec 001 T021).

Usage: python3 -I ci/contained.py SPECIES EVENT...

Reads SPECIES once, as ci/compare.py reduces a file to triples, and then each
EVENT file: an event converted alone should hold nothing its species' file
does not, and should be whole: each individual it holds with a database id has
all the triples the species' file gives it (a file missing a reaction's
participant, or a pathway's components, is contained but not whole). A
triple held more often than in the species' file counts as not in it.
Prints, for each EVENT, its triples, how many are not in the species' file and
how many of its own individuals' triples it lacks, with their kinds (class and
property) and counts. Exits 0 when every EVENT is contained and whole, 1 when
one is not, and 2 when a file cannot be read.

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
        by_subject = collections.defaultdict(collections.Counter)
        for triple, count in species.items():
            if not compare.hashed(triple[0]):
                by_subject[triple[0]][triple] = count
        status = 0
        for path in argv[2:]:
            event = compare.triples(path)
            outside = event - species
            subjects = {t[0] for t in event if not compare.hashed(t[0])}
            lacking = collections.Counter()
            for subject in subjects:
                for triple, count in by_subject.get(subject, {}).items():
                    if event[triple] < count:
                        lacking[triple] += count - event[triple]
            kinds = collections.Counter(compare.kind(t) for t in outside.elements())
            missing = collections.Counter(compare.kind(t) for t in lacking.elements())
            print(
                f"{Path(path).name}: {sum(event.values())} triples, {sum(outside.values())} not in the species' file"
            )
            for kind, count in kinds.most_common():
                print(f"  {kind} {count}")
            print(f"  of its own individuals' triples, {sum(lacking.values())} lacking")
            for kind, count in missing.most_common():
                print(f"  - {kind} {count}")
            if outside or lacking:
                status = 1
        return status
    except (OSError, ElementTree.ParseError, ValueError, RecursionError) as failure:
        print(f"cannot compare: {failure}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
