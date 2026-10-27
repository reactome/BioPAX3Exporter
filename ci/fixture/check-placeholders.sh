#!/usr/bin/env bash
# Fails if a person's name could have survived in the fixture graph (spec 001, T006),
# by allowlists, not by looking for names:
# - every line is one of the four shapes extract.py writes (a comment, an index, a node,
#   a relationship), so nothing can be added beside them;
# - a Person holds exactly its dbId, its class and the placeholders;
# - an InstanceEdit's display name and note, a summation's text, a publication's display
#   name and every comment are placeholders;
# - no other node holds a property that names or reaches a person, and a relationship
#   holds only `order` and `stoichiometry`;
# - and there are people to check.
# It prints line numbers and property names, never values.
# Usage: ci/fixture/check-placeholders.sh [ci/fixture/fixture.cypher]
set -euo pipefail
fixture=${1:-$(dirname "$0")/fixture.cypher}
exec python3 -I - "$fixture" <<'EOF'
import json
import re
import sys

NODE = re.compile(r"CREATE \(:((?:`\w+`:)*`\w+`) (\{.*\})\);")
LINK = re.compile(r"MATCH \(a:DatabaseObject \{dbId: \d+\}\), \(b:DatabaseObject \{dbId: \d+\}\) "
                  r"CREATE \(a\)-\[:`\w+` (\{.*\})\]->\(b\);")
INDEX = re.compile(r"CREATE INDEX fixture_\w+ IF NOT EXISTS FOR \(n:DatabaseObject\) ON \(n\.\w+\);")
PERSONAL = {"firstname", "surname", "initial", "orcidId", "eMailAddress", "project"}
EDIT_KEPT = {"dbId", "schemaClass", "displayName", "note", "dateTime"}
LINK_KEPT = {"order", "stoichiometry"}


def parse(text):
    """{`key`: value, ...} as written by extract.py: keys in backticks, values as JSON."""
    decoder, found, at = json.JSONDecoder(), {}, 1
    if text == "{}":
        return found
    while True:
        m = re.compile(r"`(\w+)`: ").match(text, at)
        if not m:
            raise ValueError("not a property list")
        value, at = decoder.raw_decode(text, m.end())
        found[m[1]] = value
        if text[at:] == "}":
            return found
        if text[at:at + 2] != ", ":
            raise ValueError("not a property list")
        at += 2


problems, people, edits = [], 0, 0
for number, line in enumerate(open(sys.argv[1], encoding="utf-8"), 1):
    line = line.rstrip("\n")
    if line.startswith("// ") and number == 1 or INDEX.fullmatch(line):
        continue
    node, link = NODE.fullmatch(line), LINK.fullmatch(line)
    try:
        props = parse((node or link)[2 if node else 1]) if node or link else None
    except ValueError:
        props = None
    if props is None:
        problems.append(f"line {number}: not a line extract.py writes")
        continue
    if link:
        if set(props) - LINK_KEPT:
            problems.append(f"line {number}: a relationship with {sorted(set(props) - LINK_KEPT)}")
        continue
    labels = set(re.findall(r"`(\w+)`", node[1]))
    if "Person" in labels:
        people += 1
        n = re.fullmatch(r"Person(\d+)", str(props.get("surname")))
        expected = {"dbId": props.get("dbId"), "schemaClass": "Person", "firstname": "Placeholder",
                    "initial": "P", "surname": f"Person{n[1]}" if n else None,
                    "displayName": f"Person{n[1]}, P" if n else None}
        if not n or not isinstance(props.get("dbId"), int) or props != expected:
            problems.append(f"line {number}: a Person that is not a placeholder")
        continue
    personal = PERSONAL & set(props)
    if personal:
        problems.append(f"line {number}: a {sorted(labels)[0]} with {sorted(personal)}")
    if "InstanceEdit" in labels:
        edits += 1
        if (set(props) - EDIT_KEPT or not re.fullmatch(r"Placeholder edit \d+", str(props.get("displayName")))
                or props.get("note", "Placeholder note") != "Placeholder note"):
            problems.append(f"line {number}: an InstanceEdit that is not a placeholder")
    if "Summation" in labels and not re.fullmatch(r"Placeholder summation \d+", str(props.get("text", "Placeholder summation 0"))):
        problems.append(f"line {number}: a summation's text that is not a placeholder")
    if "Publication" in labels and not re.fullmatch(r"Placeholder publication \d+", str(props.get("displayName"))):
        problems.append(f"line {number}: a publication's display name that is not a placeholder")
    if props.get("comment", "Placeholder comment") != "Placeholder comment":
        problems.append(f"line {number}: a comment that is not a placeholder")
if not people:
    problems.append("no people to check")
print("\n".join(problems))
print(f"{people} people, {edits} instance edits checked")
sys.exit(1 if problems else 0)
EOF
