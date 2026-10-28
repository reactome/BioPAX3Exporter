"""Writes the fixture graph (spec 001, T006) from a private copy of a release's graph.

Usage: python3 -I ci/fixture/extract.py <neo4j-http-url> <stId>... > ci/fixture/fixture.cypher

It talks to Neo4j's HTTP endpoint for database `graph.db`, and never to a shared or
production graph (constitution V). It follows sbml-exporter's extract, widened to what
BioPAX writes (research R3): from each given event, the relationships the exporter's
classes read, in their direction, each node with its full label set and every property.

- An event is taken with its participants, catalysis, regulation, literature,
  summations, cross-references, GO terms, diseases, compartments and species; its
  curation records (the InstanceEdits that created, authored, reviewed, revised or
  edited it, and their people); what it was inferred from; and, if it is a pathway,
  the events it holds.
- Its parent pathways up to the top are taken too, for themselves and the one
  `hasEvent` on the way down, but not their other events.
- Entities are taken with their components, members, candidates, repeated units,
  modifications and reference entities, and those with their cross-references.
- Some nodes are read only for themselves and taken as leaves, without their own
  relationships: compartments, GO terms, diseases, reference databases, people, and
  events outside the fixture that a taken one names (a preceding event, a normal
  reaction, a source of an inference). Taxa (species) and external ontology terms
  (PSI-MOD, sequence ontology) are taken with their cross-references and databases,
  which a BioSource and a modification type write (T011); nothing else of theirs is
  followed, since none of their other relationships is in OUTGOING.
- The `DBInfo` node is taken as it is.

People's names never leave the graph (constitution VI): every `Person` keeps only its
dbId and class and is named `Placeholder` `PersonN`; every `InstanceEdit`'s display
name (which carries a surname) and note, every summation's text and display name, every publication's
display name and every comment are replaced. `check-placeholders.sh` checks the output.
"""

import json
import sys
import urllib.request

# Outgoing relationships followed from every node reached (but from a parent pathway
# only the one `hasEvent` on the way down; a given pathway's sub-pathways are expanded).
OUTGOING = {
    # events
    "hasEvent",
    "input",
    "output",
    "catalystActivity",
    "regulatedBy",
    "literatureReference",
    "summation",
    "crossReference",
    "goBiologicalProcess",
    "disease",
    "compartment",
    "species",
    "relatedSpecies",
    "requiredInputComponent",
    "entityFunctionalStatus",
    "precedingEvent",
    "normalReaction",
    "evidenceType",
    "catalystActivityReference",
    "regulationReference",
    "reactionType",
    # catalysis and regulation
    "physicalEntity",
    "activity",
    "activeUnit",
    "regulator",
    # entities
    "hasComponent",
    "hasMember",
    "hasCandidate",
    "repeatedUnit",
    "referenceEntity",
    "hasModifiedResidue",
    "psiMod",
    "modification",
    "referenceSequence",
    "secondReferenceSequence",
    "goCellularComponent",
    "functionalStatus",
    "functionalStatusType",
    "structuralVariant",
    "entityOnOtherCell",
    # references (not `referenceGene`: the published files write a reference entity's
    # own identifier and cross-references, never its genes)
    "referenceDatabase",
}
# Incoming relationships, by the label of the node they come into: an event's curation
# records (InstanceEdit -> event), their people and a publication's authors
# (Person -> InstanceEdit or publication), and what an event or entity was inferred
# from (source -inferredTo-> this).
INCOMING = {
    "created": "Event",
    "modified": "Event",
    "authored": "Event",
    "reviewed": "Event",
    "revised": "Event",
    "edited": "Event",
    "author": ("InstanceEdit", "Publication"),
    "inferredTo": ("Event", "PhysicalEntity"),
}
# Read only for themselves: taken as leaves.
LEAVES = {"Person", "GO_Term", "Disease", "ReferenceDatabase", "Compartment"}
# Relationships whose far end is taken as a leaf whatever it is: an event or entity
# outside the fixture that a taken one names.
TO_LEAF = {"precedingEvent", "normalReaction", "inferredTo"}
# A person keeps only these, beside the placeholders.
PERSON_KEPT = {"dbId", "schemaClass"}


def follow(labels, kind, outgoing, other, here_is_parent):
    """Return (take the relationship, expand from the node at its other end)."""
    if outgoing:
        if kind not in OUTGOING or (kind == "hasEvent" and here_is_parent):
            return False, False
        # A protein's or gene product's own cross-references (dozens each) are not
        # written: its ProteinReference carries its UniProt identifier alone.
        if kind == "crossReference" and "ReferenceSequence" in labels:
            return False, False
    else:
        wanted = INCOMING.get(kind)
        wanted = (wanted,) if isinstance(wanted, str) else wanted or ()
        if not set(wanted) & set(labels):
            return False, False
    return True, kind not in TO_LEAF and not LEAVES.intersection(other)


def run(url, statement, **parameters):
    body = json.dumps({"statements": [{"statement": statement, "parameters": parameters}]}).encode()
    request = urllib.request.Request(
        url.rstrip("/") + "/db/graph.db/tx/commit", body, {"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(request) as response:
        answer = json.load(response)
    if answer["errors"]:
        raise RuntimeError(answer["errors"])
    return [row["row"] for row in answer["results"][0]["data"]]


def literal(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return repr(value)
    if isinstance(value, list):
        return "[" + ", ".join(literal(v) for v in value) + "]"
    return json.dumps(value)  # Cypher reads JSON's string escapes


def properties(props):
    return "{" + ", ".join(f"`{k}`: {literal(v)}" for k, v in sorted(props.items())) + "}"


def anonymise(labels, props, people):
    if "Person" in labels:
        n = people.setdefault(props["dbId"], len(people) + 1)
        props = {k: v for k, v in props.items() if k in PERSON_KEPT}
        props.update(
            firstname="Placeholder", surname=f"Person{n}", initial="P", displayName=f"Person{n}, P"
        )
    if "InstanceEdit" in labels:
        props["displayName"] = f"Placeholder edit {props['dbId']}"
        if "note" in props:
            props["note"] = "Placeholder note"
    # Free text that can name people: a summation cites authors ("X et al"), a
    # publication's display name begins with its first author, a comment is free.
    if "Summation" in labels:
        for key in ("text", "displayName"):  # the display name is the text's opening
            if key in props:
                props[key] = f"Placeholder summation {props['dbId']}"
    if "Publication" in labels:
        props["displayName"] = f"Placeholder publication {props['dbId']}"
    # Its shape kept: a reference sequence's comment is a list, which graph-core reads
    # as one (a string there failed every read of the reference, spec 001 T011).
    if "comment" in props:
        comment = props["comment"]
        props["comment"] = (
            ["Placeholder comment"] * len(comment)
            if isinstance(comment, list)
            else "Placeholder comment"
        )
    return props


def parents(url, events):
    """The parent pathways of the given events, up to the top, and the hasEvent links on the way."""
    links, found, frontier = set(), set(), list(events)
    while frontier:
        rows = run(
            url,
            "MATCH (p:Pathway)-[r:hasEvent]->(e) WHERE id(e) IN $ids "
            "RETURN id(p), properties(r), id(e)",
            ids=frontier,
        )
        frontier = []
        for parent, props, child in rows:
            links.add((parent, "hasEvent", json.dumps(props, sort_keys=True), child))
            if parent not in found:
                found.add(parent)
                frontier.append(parent)
    return found, links


def main(argv):
    if len(argv) < 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    url, events = argv[1], argv[2:]
    given = [
        r[0] for r in run(url, "MATCH (e:Event) WHERE e.stId IN $ids RETURN id(e)", ids=events)
    ]
    if len(given) != len(events):
        raise SystemExit(f"found {len(given)} of {len(events)} events")
    above, rels = parents(url, given)
    above -= set(given)
    nodes, expanded, frontier = {}, set(), given + sorted(above)
    while frontier:
        expanded.update(frontier)
        rows = run(
            url,
            "MATCH (n)-[r]-(m) WHERE id(n) IN $ids "
            "RETURN id(n), labels(n), type(r), startNode(r) = n, properties(r), id(m), labels(m)",
            ids=frontier,
        )
        frontier = []
        for here, labels, kind, outgoing, props, there, other in rows:
            take, expand = follow(labels, kind, outgoing, other, here in above)
            if not take:
                continue
            start, end = (here, there) if outgoing else (there, here)
            rels.add((start, kind, json.dumps(props, sort_keys=True), end))
            nodes.setdefault(there, None)
            if expand and there not in expanded and there not in frontier:
                frontier.append(there)
    ids = sorted(expanded | set(nodes))
    ids.append(run(url, "MATCH (d:DBInfo) RETURN id(d)")[0][0])
    people = {}
    out = [
        "// The fixture graph (spec 001, T006): written by ci/fixture/extract.py; see README.md.",
        "CREATE INDEX fixture_dbId IF NOT EXISTS FOR (n:DatabaseObject) ON (n.dbId);",
        "CREATE INDEX fixture_stId IF NOT EXISTS FOR (n:DatabaseObject) ON (n.stId);",
    ]
    key = {}
    for node_id, labels, props in run(
        url,
        "MATCH (n) WHERE id(n) IN $ids RETURN id(n), labels(n), properties(n) ORDER BY n.dbId",
        ids=ids,
    ):
        key[node_id] = props["dbId"]
        props = anonymise(labels, props, people)
        out.append(
            f"CREATE (:{':'.join('`' + label + '`' for label in sorted(labels))} {properties(props)});"
        )
    for start, kind, props, end in sorted(rels, key=lambda r: (key[r[0]], r[1], key[r[3]], r[2])):
        out.append(
            f"MATCH (a:DatabaseObject {{dbId: {key[start]}}}), (b:DatabaseObject {{dbId: {key[end]}}}) "
            f"CREATE (a)-[:`{kind}` {properties(json.loads(props))}]->(b);"
        )
    print("\n".join(out))
    print(f"{len(ids)} nodes, {len(rels)} relationships, {len(people)} people", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
