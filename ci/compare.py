"""Two BioPAX Level 3 files compared by meaning (spec 001 T005, research R2).

Usage: python3 -I ci/compare.py [--examples N] REFERENCE FILE

Each file is reduced to (subject, property, target) triples, in which every
rdf:ID is replaced by a key made of the individual's content, so ids and order
do not count:

- an event or entity: its class and its "Reactome Database ID Release N"
  xrefs; one that controls (a Control, Catalysis, Modulation or
  TemplateReactionRegulation) also by what it controls, since one regulation
  is written as one Control per controlled reaction;
- anything else: its class and a hash of its properties, built bottom-up
  (`nextStep` is compared as a triple but left out of keys: steps chain), with
  an xref's db and id and a vocabulary's term shown for reading.

A hashed key moves with any change to its individual, so one change shows
again in every triple that points at it, and at whatever points at those. The
report therefore names the changes at the root apart: a differing triple is
carried when it points at, or belongs to an individual that points at, a key
found in one file only; the rest are where the files really part. An
individual keyed by content that changes itself shows all its triples at the
root, the unchanged ones with it, since its key is new.

Within one individual a value counts once (in RDF a repeated triple adds
nothing, and Paxtools keeps values as sets); across individuals they count as
a multiset, since the published files repeat individuals of the same content.
A literal is compared with its datatype, a number by its value as that type
holds it (an xsd:float as a 32-bit float: "1", "1.0" and "1e0" are one value).

It prints the differences by kind (class and property) and count, and exits 0
when the files mean the same, 1 when they differ, and 2 when it cannot compare
them (usage, a file it cannot read, or a form it does not read: a nested
individual, rdf:nodeID, a property written as an attribute).

People's names stay out of the output (constitution VI): the literal of an
`author`, and of a comment beginning "Authored:", "Reviewed:" or "Edited:" (in
any case, and the like), is replaced by a hash, salted for the run, before
anything is compared; and an example prints a free-text literal (a comment, a
title, a citation's source, which can name authors and editors) only as its
length and a salted hash.
"""
import collections
import hashlib
import os
import re
import struct
import sys
from xml.etree import ElementTree

RDF = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}"
XML_BASE = "{http://www.w3.org/XML/1998/namespace}base"
XSD = "http://www.w3.org/2001/XMLSchema#"
NAMED = re.compile(r"\s*(authored|reviewed|edited|created|revised|modified|curated)\s*:", re.I)
UNKEYED = {"nextStep"}
DBID = "Reactome Database ID"
SALT = os.urandom(16)
INTEGER = re.compile(r"[+-]?\d+")
DECIMAL = re.compile(r"[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?|NaN|-?INF")
# Literals an example may print as they are: codes, ids and numbers, never free text.
SHOWN = {
    "db", "id", "idVersion", "term", "stoichiometricCoefficient", "conversionDirection", "controlType",
    "catalysisDirection", "sequencePosition", "positionStatus", "year", "eCNumber", "dbVersion",
    "templateDirection", "stepDirection", "url",
}


class Refused(ValueError):
    """A file this comparer does not read."""


def local(tag):
    return tag.rsplit("}", 1)[-1]


def digest(text):
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]


def salted(text):
    return hashlib.sha1(SALT + text.encode("utf-8")).hexdigest()[:12]


def number(text, kind):
    """A numeric literal by its value as its datatype holds it, or None when it is not one."""
    text = text.strip()
    if kind in ("int", "integer", "long", "short") and INTEGER.fullmatch(text):
        return str(int(text))
    if kind in ("float", "double", "decimal") and DECIMAL.fullmatch(text):
        value = float(text.replace("INF", "inf"))
        if kind == "float":
            value = struct.unpack("f", struct.pack("f", value))[0]
        return repr(value)
    return None


def literal(prop, text, datatype):
    """('l', text, datatype) as compared: a person's name hashed; a number by its value."""
    kind = datatype.removeprefix(XSD) if datatype else ""
    if prop == "author" or (prop == "comment" and NAMED.match(text)):
        return ("l", f"<hashed {salted(text)}>", kind)
    value = number(text, kind)
    return ("l", text if value is None else value, kind)


def individuals(path):
    """{name: (class, [(property, ('r', name) | ('l', text, datatype))])}, the file's top-level individuals.

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
                props.append((prop, literal(prop, child.text or "", child.get(RDF + "datatype"))))
        if name in nodes:
            raise Refused(f"{path}: two individuals named {name}")
        nodes[name] = (cls, sorted(set(props)))
        element.clear()
    return nodes


def triples(path):
    """The file's (subject key, property, target) triples, counted; a target is ('r', key) or ('l', text, datatype)."""
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
            controlled = sorted(key(v[1]) for p, v in props if p == "controlled" and v[0] == "r")
            if controlled:
                found += "|controls:" + ",".join(controlled)
        else:
            if name in open_keys:
                raise Refused(f"{path}: a cycle through a {cls}")
            open_keys.add(name)
            parts = sorted(
                f"{prop}>{key(value[1])}" if value[0] == "r" else f"{prop}={value[1]}^^{value[2]}"
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


def carried(reference, other):
    """The differing triples that only carry a change made elsewhere, for each side, as sets."""
    subjects = [{t[0] for t in side} for side in (reference, other)]
    one_sided = subjects[0] ^ subjects[1]

    def points_away(triple):
        return triple[2][0] == "r" and triple[2][1] in one_sided

    result = []
    for side in differences(reference, other):
        pointing = {t[0] for t in side if points_away(t) and t[0] in one_sided}
        result.append({t for t in side if points_away(t) or t[0] in pointing})
    return result


def shown(triple):
    prop, target = triple[1], triple[2]
    if target[0] == "r":
        return f"-> {target[1]}"
    text = target[1] if prop in SHOWN or target[1].startswith("<hashed ") else f"<{len(target[1])} chars {salted(target[1])}>"
    return f"{text!r}^^{target[2]}" if target[2] else repr(text)


def report(reference, other, examples=0):
    """Lines describing the differences of two files' triples by kind and count, the root changes apart."""
    only = differences(reference, other)
    away = carried(reference, other)
    lines = [f"only in the reference: {sum(only[0].values())}", f"only in the file: {sum(only[1].values())}"]
    for title, wanted in (("at the root", False), ("carried by a change at the root", True)):
        kinds = collections.Counter()
        for side, found, skip in zip("-+", only, away):
            for triple, count in found.items():
                if (triple in skip) == wanted:
                    kinds[(kind(triple), side)] += count
        if kinds:
            lines.append(f" {title}: {sum(kinds.values())}")
            lines.extend(f"  {side} {name} {count}" for (name, side), count in sorted(kinds.items()))
    if examples:
        for side, found, skip in zip("-+", only, away):
            ordered = sorted(found.items(), key=lambda item: (item[0] in skip, item[0][0], item[0][1], repr(item[0][2])))
            for triple, count in ordered[:examples]:
                lines.append(f"    {side} {count} x {triple[0]} {triple[1]} {shown(triple)}")
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
        reference, other = triples(args[0]), triples(args[1])
    except (OSError, ElementTree.ParseError, Refused) as failure:
        print(f"cannot compare: {failure}", file=sys.stderr)
        return 2
    print("\n".join(report(reference, other, examples)))
    return 1 if reference != other else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
