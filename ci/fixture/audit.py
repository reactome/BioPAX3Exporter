"""Searches the fixture graph for the real names of its people (spec 001, T006).

Usage: python3 -I ci/fixture/audit.py <private-copy-http-url> [ci/fixture/fixture.cypher]

check-placeholders.sh holds the fixture to allowlists, but it cannot judge free text on
other classes (a title, a journal), and it has no real names to look for. This does, at
extraction time, against the same private copy of the graph extract.py read (never a
shared or production graph): it fetches the real surnames and first names of the
fixture's people and searches every string in the fixture for them as whole words, and
for anything shaped like an email address or an ORCID.

It prints counts by node class and property, never a name or a value, and exits 1 if
anything is found. Run it whenever the events change.
"""
import collections
import json
import re
import sys
import urllib.request

EMAIL = re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+")
ORCID = re.compile(r"\b\d{4}-\d{4}-\d{4}-\d{3}[\dX]\b")


def main(argv):
    if len(argv) not in (2, 3):
        print(__doc__.strip(), file=sys.stderr)
        return 2
    url, path = argv[1], argv[2] if len(argv) == 3 else "ci/fixture/fixture.cypher"
    fixture = open(path, encoding="utf-8").read()
    people = [int(m) for m in re.findall(r"^CREATE \(:[^{]*`Person`[^{]*\{`dbId`: (\d+)", fixture, re.M)]
    body = json.dumps({"statements": [{
        "statement": "MATCH (p:Person) WHERE p.dbId IN $ids RETURN p.surname, p.firstname",
        "parameters": {"ids": people}}]}).encode()
    request = urllib.request.Request(url.rstrip("/") + "/db/graph.db/tx/commit", body,
                                     {"Content-Type": "application/json"})
    with urllib.request.urlopen(request) as response:
        rows = json.load(response)["results"][0]["data"]
    names = {n for row in rows for n in row["row"] if n and len(n) >= 3}
    pattern = re.compile(r"\b(" + "|".join(re.escape(n) for n in sorted(names, key=len, reverse=True)) + r")\b")
    hits, strings = collections.Counter(), 0
    for line in fixture.splitlines():
        node = re.match(r"CREATE \(:(.*?) \{(.*)\}\);$", line)
        if not node or "`Person`" in node[1]:
            continue
        cls = re.search(r'`schemaClass`: "(\w+)"', line)
        for key, value in re.findall(r'`(\w+)`: ("(?:[^"\\]|\\.)*")', node[2]):
            strings += 1
            text = json.loads(value)
            if pattern.search(text) or EMAIL.search(text) or ORCID.search(text):
                hits[(cls[1] if cls else "?", key)] += 1
    print(f"{len(people)} people ({len(rows)} found), {len(names)} names of 3+ letters, {strings} strings")
    print(f"strings holding a name, an email or an ORCID: {sum(hits.values())}",
          {f"{c}.{k}": n for (c, k), n in hits.most_common()})
    return 1 if hits or len(rows) != len(people) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
