"""Searches the fixture graph for the real names of its people (spec 001, T006).

Usage: python3 -I ci/fixture/audit.py <private-copy-http-url> [ci/fixture/fixture.cypher]

check-placeholders.sh holds the fixture to allowlists, but it cannot judge free text on
other classes (a title, a journal), and it has no real names to look for. This does, at
extraction time, against the same private copy of the graph extract.py read (never a
shared or production graph): it fetches the real surnames and first names of the
fixture's people and searches every string in the fixture for them as whole words, and
for anything shaped like an email address or an ORCID.

It prints counts by node class and property, never a name or a value, and exits 1 if
anything is found beyond KNOWN, the hits judged by hand to be coincidences. Run it
whenever the events change, and judge any new hit before adding it there.
"""

import collections
import json
import re
import sys
import urllib.request

EMAIL = re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+")
ORCID = re.compile(r"\b\d{4}-\d{4}-\d{4}-\d{3}[\dX]\b")
# Hits judged to be coincidences, by class, property and the node's dbId, with their
# count: a UniProt keyword naming a disease after a person, one of whose names is also
# a fixture person's first name. Keyed by node, so a new hit elsewhere in the same
# property is not taken for it.
KNOWN = {("ReferenceGeneProduct", "keyword", 57641): 1}


def values(text):
    """A node's {`key`: value, ...}, as extract.py writes it: keys in backticks, values JSON."""
    decoder, found, at = json.JSONDecoder(), {}, 1
    while text[at:] != "}":
        key = re.compile(r"`(\w+)`: ").match(text, at)
        found[key[1]], at = decoder.raw_decode(text, key.end())
        at += 2 if text[at : at + 2] == ", " else 0
    return found


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for each in value:
            yield from strings(each)


def main(argv):
    if len(argv) not in (2, 3):
        print(__doc__.strip(), file=sys.stderr)
        return 2
    url, path = argv[1], argv[2] if len(argv) == 3 else "ci/fixture/fixture.cypher"
    fixture = open(path, encoding="utf-8").read()
    people = [
        int(m) for m in re.findall(r"^CREATE \(:[^{]*`Person`[^{]*\{`dbId`: (\d+)", fixture, re.M)
    ]
    body = json.dumps(
        {
            "statements": [
                {
                    "statement": "MATCH (p:Person) WHERE p.dbId IN $ids RETURN p.surname, p.firstname",
                    "parameters": {"ids": people},
                }
            ]
        }
    ).encode()
    request = urllib.request.Request(
        url.rstrip("/") + "/db/graph.db/tx/commit", body, {"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(request) as response:
        rows = json.load(response)["results"][0]["data"]
    names = {n for row in rows for n in row["row"] if n and len(n) >= 3}
    pattern = re.compile(
        r"\b(" + "|".join(re.escape(n) for n in sorted(names, key=len, reverse=True)) + r")\b"
    )
    hits, count = collections.Counter(), 0
    for line in fixture.splitlines():
        node = re.match(r"CREATE \(:(.*?) \{(.*)\}\);$", line)
        if not node or "`Person`" in node[1]:
            continue
        props = values("{" + node[2] + "}")
        for key, value in props.items():
            for text in strings(value):
                count += 1
                if pattern.search(text) or EMAIL.search(text) or ORCID.search(text):
                    hits[(str(props.get("schemaClass")), key, props.get("dbId"))] += 1
    unknown = hits - collections.Counter(KNOWN)
    print(
        f"{len(people)} people ({len(rows)} found), {len(names)} names of 3+ letters, {count} strings"
    )
    print(
        f"strings holding a name, an email or an ORCID: {sum(hits.values())}",
        dict(
            sum(
                (collections.Counter({f"{c}.{k}": n}) for (c, k, _), n in hits.items()),
                collections.Counter(),
            )
        ),
    )
    print(f"beyond those known to be coincidences: {sum(unknown.values())}")
    return 1 if unknown or not people or len(rows) != len(people) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
