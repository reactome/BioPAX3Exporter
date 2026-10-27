"""Two BioPAX Level 3 files compared by meaning (spec 001 T005, research R2).

Usage: python3 -I ci/compare.py [--examples N] REFERENCE FILE

Each file is reduced to a multiset of (subject, property, target) triples, in
which every rdf:ID is replaced by a key made of the individual's content, so
ids and order do not count:

- an event or entity: its class and its "Reactome Database ID Release N"
  xrefs; a Control, TemplateReactionRegulation or Catalysis also by what it
  controls, since one regulation is written as one Control per controlled
  reaction;
- anything else: its class and a hash of its properties, built bottom-up
  (`nextStep` is compared as a triple but left out of keys: steps chain), with
  an xref's db and id and a vocabulary's term shown for reading. Such a key
  moves with any change to the individual, so one change shows again in each
  triple that points at it.

A literal is compared with its datatype, a number by its value ("1" and "1.0"
as xsd:float are one value). Triples count as a multiset: the published files
repeat individuals of the same content. It prints the differences by kind
(class and property) and count, and exits 0 when the files mean the same, 1
when they differ, and 2 when it cannot compare them (usage, a file it cannot
read, or a form it does not read: a nested individual, rdf:nodeID, a property
written as an attribute).

People's names stay out of the output (constitution VI): the literal of an
`author`, and of a comment beginning "Authored:", "Reviewed:" or "Edited:" (in
any case, and the like), is replaced by a hash, salted for the run, before
anything is compared or printed.
"""
import collections
import hashlib
import os
import re
import sys
from xml.etree import ElementTree

RDF = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}"
XML_BASE = "{http://www.w3.org/XML/1998/namespace}base"
XSD = "http://www.w3.org/2001/XMLSchema#"
NAMED = re.compile(r"\s*(authored|reviewed|edited|created|revised|modified|curated)\s*:", re.I)
CONTROLS = {"Control", "TemplateReactionRegulation", "Catalysis"}
UNKEYED = {"nextStep"}
DBID = "Reactome Database ID"
SALT = os.urandom(16)


class Refused(ValueError):
    """A file this comparer does not read."""


def local(tag):
    return tag.rsplit("}", 1)[-1]


def digest(text):
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]


def literal(prop, text, datatype):
    """A literal as compared: a person's name hashed; a number by its value; the datatype kept."""
    if prop == "author" or (prop == "comment" and NAMED.match(text)):
        return f"<hashed {hashlib.sha1(SALT + text.encode('utf-8')).hexdigest()[:12]}>"
    kind = datatype.removeprefix(XSD) if datatype else ""
    try:
        if kind in ("float", "double", "decimal"):
            return f"{float(text)!r}^^{kind}"
        if kind in ("int", "integer", "long", "short"):
            return f"{int(text)}^^{kind}"
    except ValueError:
        pass
    return f"{text}^^{kind}" if kind else text


def individuals(path):
    """{name: (class, [(property, ('r', name) | ('l', literal))])}, the file's top-level individuals.

    Names are resolved against the file's xml:base, so `rdf:ID="X"`,
    `rdf:about="#X"` and an absolute `rdf:about` all name what a reference to
    them names, whichever way the writer chose.
    """
    nodes, depth, stem = {}, 0, ""

    def resolve(value):
        return stem + value if value.startswith("#") else value

    for event, element in ElementTree.iterparse(path, events=("start", "end")):
        if event == "start":
            depth += 1
            if depth == 1:
                stem = element.get(XML_BASE, "").split("#", 1)[0]
            continue
        depth -= 1
        if depth != 1:
            continue
        cls = local(element.tag)
        if cls == "Ontology":
            element.clear()
            continue
        if element.get(RDF + "ID") is not None:
            name = f"{stem}#{element.get(RDF + 'ID')}"
        elif element.get(RDF + "about") is not None:
            name = resolve(element.get(RDF + "about"))
        else:
            raise Refused(f"{path}: a {cls} with no rdf:ID or rdf:about")
        if set(element.keys()) - {RDF + "ID", RDF + "about"}:
            raise Refused(f"{path}: a {cls} with a property written as an attribute")
        props = []
        for child in element:
            prop = local(child.tag)
            if len(child):
                raise Refused(f"{path}: an individual nested in a {cls}'s {prop}")
            if set(child.keys()) - {RDF + "resource", RDF + "datatype"}:
                raise Refused(f"{path}: a {cls}'s {prop} with an attribute this does not read")
            target = child.get(RDF + "resource")
            if target is not None:
                props.append((prop, ("r", resolve(target))))
            else:
                props.append((prop, ("l", literal(prop, child.text or "", child.get(RDF + "datatype")))))
        if name in nodes:
            raise Refused(f"{path}: two individuals named {name}")
        nodes[name] = (cls, props)
        element.clear()
    return nodes


def triples(path):
    """The file's multiset of (subject key, property, target) triples; a target is ('r', key) or ('l', literal)."""
    nodes = individuals(path)
    keys, open_keys = {}, set()

    def literals(props):
        return {prop: value[1] for prop, value in props if value[0] == "l"}

    def is_dbid(value):
        if value[0] != "r" or value[1] not in nodes or nodes[value[1]][0] != "UnificationXref":
            return False
        return literals(nodes[value[1]][1]).get("db", "").startswith(DBID)

    def key(name):
        if name in keys:
            return keys[name]
        if name not in nodes:
            return f"EXTERNAL|{name}"
        cls, props = nodes[name]
        dbids = sorted(literals(nodes[v[1]][1]).get("id", "") for p, v in props if p == "xref" and is_dbid(v))
        if dbids:
            found = f"{cls}|dbId:{','.join(dbids)}"
            if cls in CONTROLS:
                controlled = sorted(key(v[1]) for p, v in props if p == "controlled" and v[0] == "r")
                found += "|controls:" + ",".join(controlled)
        else:
            if name in open_keys:
                raise Refused(f"{path}: a cycle through a {cls}")
            open_keys.add(name)
            parts = sorted(
                f"{prop}{'>' + key(value[1]) if value[0] == 'r' else '=' + value[1]}"
                for prop, value in props
                if prop not in UNKEYED
            )
            open_keys.discard(name)
            shown = literals(props)
            label = "".join(f"{shown[p]}|" for p in ("db", "id", "term") if p in shown)
            found = f"{cls}|{label}#{digest(chr(10).join(parts))}"
        keys[name] = found
        return found

    sys.setrecursionlimit(max(sys.getrecursionlimit(), 100000))
    counted = collections.Counter()
    for name, (cls, props) in nodes.items():
        subject = key(name)
        for prop, value in props:
            counted[(subject, prop, ("r", key(value[1])) if value[0] == "r" else value)] += 1
    return counted


def differences(reference, other):
    """The triples only in each, as Counters."""
    return reference - other, other - reference


def kind(triple):
    return f"{triple[0].split('|', 1)[0]}.{triple[1]}"


def shown(target):
    return f"-> {target[1]}" if target[0] == "r" else repr(target[1])


def report(only_reference, only_other, examples=0):
    """Lines describing the differences by kind and count."""
    lines = [f"only in the reference: {sum(only_reference.values())}", f"only in the file: {sum(only_other.values())}"]
    kinds = collections.Counter()
    for side, found in (("-", only_reference), ("+", only_other)):
        for triple, count in found.items():
            kinds[(kind(triple), side)] += count
    for (name, side), count in sorted(kinds.items()):
        lines.append(f"  {side} {name} {count}")
    if examples:
        for side, found in (("-", only_reference), ("+", only_other)):
            for triple, count in sorted(found.items())[:examples]:
                lines.append(f"    {side} {count} x {triple[0]} {triple[1]} {shown(triple[2])}")
    return lines


def main(argv):
    args = argv[1:]
    examples = 0
    if args[:1] == ["--examples"] and len(args) > 1 and args[1].isdigit():
        examples, args = int(args[1]), args[2:]
    if len(args) != 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    try:
        only_reference, only_other = differences(triples(args[0]), triples(args[1]))
    except (OSError, ElementTree.ParseError, Refused) as failure:
        print(f"cannot compare: {failure}", file=sys.stderr)
        return 2
    print("\n".join(report(only_reference, only_other, examples)))
    return 1 if only_reference or only_other else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
