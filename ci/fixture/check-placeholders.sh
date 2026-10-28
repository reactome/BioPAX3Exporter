#!/usr/bin/env bash
# Fails if a person's name could have survived in the fixture graph (spec 001, T006),
# by allowlists, not by looking for names:
# - every line is one of the four shapes extract.py writes (a comment, an index, a node,
#   a relationship), so nothing can be added beside them;
# - a Person holds exactly its dbId, its class and the placeholders;
# - an InstanceEdit's display name and note, a summation's text and display name, a
#   publication's display name and every comment are placeholders;
# - every node's labels are graph classes this fixture is known to hold (a new one is
#   added here after the extract-time audit, README.md);
# - each node's schemaClass is among its labels, and the class rules read both;
# - no other node holds a key from PERSONAL's list of person-like names (in any
#   capitalisation, underscores and hyphens aside; a longer key containing one is not
#   matched), and
#   no value anywhere looks like an email address or an ORCID;
# - a relationship holds only a number `order` and a number `stoichiometry`, and an
#   edit's dateTime is a date;
# - and there are people to check.
# Free text on other classes (a title, a journal) is not something an allowlist can
# judge: the extract-time audit against the people's real names covers it (README.md).
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
PERSONAL = {"firstname", "surname", "initial", "orcidid", "orcid", "emailaddress", "email", "project",
            "lastname", "givenname", "familyname", "middlename", "author", "authors", "fullname"}
PUBLICATIONS = {"Publication", "LiteratureReference", "Book", "URL"}
LABELS = set("""AbstractModifiedResidue BlackBoxEvent Book CandidateSet CatalystActivity Compartment
Complex DBInfo DatabaseIdentifier DatabaseObject DefinedSet Deletable Disease
EntityFunctionalStatus EntitySet EntityWithAccessionedSequence Event EvidenceType ExternalOntology
FailedReaction FragmentModification FragmentReplacedModification FunctionalStatusType
FunctionalStatus GO_BiologicalProcess GO_CellularComponent GO_MolecularFunction GO_Term
GeneticallyModifiedResidue GenomeEncodedEntity GroupModifiedResidue InstanceEdit
LiteratureReference MetaDatabaseObject ModifiedResidue NonsenseMutation Pathway Person
PhysicalEntity Polymer PositiveGeneExpressionRegulation PositiveRegulation PsiMod Publication
ReactionLikeEvent Reaction ReferenceDNASequence ReferenceDatabase ReferenceEntity
ReferenceGeneProduct ReferenceIsoform ReferenceMolecule ReferenceRNASequence ReferenceSequence
Regulation Release ReplacedResidue SequenceOntology SimpleEntity Species Summation Taxon
TopLevelPathway Trackable TranslationalModification URL""".split())
EMAIL = re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+")
ORCID = re.compile(r"\b\d{4}-\d{4}-\d{4}-\d{3}[\dX]\b")
DATE = re.compile(r"\d{4}-\d{2}-\d{2}( \d{2}:\d{2}:\d{2}(\.\d+)?)?")


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for each in value:
            yield from strings(each)
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
    if any(EMAIL.search(text) or ORCID.search(text) for value in props.values() for text in strings(value)):
        problems.append(f"line {number}: a value like an email address or an ORCID")
    if link:
        if set(props) - LINK_KEPT or any(type(v) is not int for v in props.values()):
            problems.append(f"line {number}: a relationship with a property beside two numbers")
        continue
    labels = set(re.findall(r"`(\w+)`", node[1]))
    if labels - LABELS:
        problems.append(f"line {number}: labels this fixture is not known to hold: {sorted(labels - LABELS)}")
    if props.get("schemaClass") not in labels:
        problems.append(f"line {number}: a node whose schemaClass is not among its labels")
    labels = labels | {str(props.get("schemaClass"))}
    if "Person" in labels:
        people += 1
        n = re.fullmatch(r"Person(\d+)", str(props.get("surname")))
        expected = {"dbId": props.get("dbId"), "schemaClass": "Person", "firstname": "Placeholder",
                    "initial": "P", "surname": f"Person{n[1]}" if n else None,
                    "displayName": f"Person{n[1]}, P" if n else None}
        if not n or not isinstance(props.get("dbId"), int) or props != expected:
            problems.append(f"line {number}: a Person that is not a placeholder")
        continue
    personal = PERSONAL & {re.sub(r"[_-]", "", key.lower()) for key in props}
    if personal:
        problems.append(f"line {number}: a {sorted(labels)[0]} with {sorted(personal)}")
    if "InstanceEdit" in labels:
        edits += 1
        if (set(props) - EDIT_KEPT or not re.fullmatch(r"Placeholder edit \d+", str(props.get("displayName")))
                or props.get("note", "Placeholder note") != "Placeholder note"
                or not DATE.fullmatch(str(props.get("dateTime", "2000-01-01")))):
            problems.append(f"line {number}: an InstanceEdit that is not a placeholder")
    for key in ("text", "displayName"):
        if "Summation" in labels and not re.fullmatch(r"Placeholder summation \d+", str(props.get(key, "Placeholder summation 0"))):
            problems.append(f"line {number}: a summation's {key} that is not a placeholder")
    if PUBLICATIONS & labels and not re.fullmatch(r"Placeholder publication \d+", str(props.get("displayName"))):
        problems.append(f"line {number}: a publication's display name that is not a placeholder")
    comment = props.get("comment", "Placeholder comment")
    if comment != "Placeholder comment" and not (
            isinstance(comment, list) and set(comment) <= {"Placeholder comment"}):
        problems.append(f"line {number}: a comment that is not a placeholder")
if not people:
    problems.append("no people to check")
print("\n".join(problems))
print(f"{people} people, {edits} instance edits checked")
sys.exit(1 if problems else 0)
EOF
