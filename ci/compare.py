"""Two BioPAX Level 3 files compared by meaning (spec 001 T005, research R2).

Usage: python3 -I ci/compare.py [--examples N] REFERENCE FILE

Each file is reduced to a multiset of (subject, property, target) triples, in
which every rdf:ID is replaced by a key made of the individual's content, so
ids and order do not count:

- an event or entity: its class and its "Reactome Database ID Release N" xref;
  a Control (or TemplateReactionRegulation) also by what it controls, since one
  regulation is written as one Control per controlled reaction;
- an xref: its class, db and id; a vocabulary: its class and term;
- anything else: a hash of its class and its properties, built bottom-up
  (`nextStep` is compared as a triple but left out of keys: steps chain).
  Such a key moves with any change to the individual, so one change shows
  again in each triple that points at it.

Triples count as a multiset: the published files repeat individuals of the
same content. It prints the differences by kind (class and property) and
count, and exits 1 when the files differ.

People's names stay out of the output (constitution VI): the literal of an
`author`, and of a comment beginning "Authored:", "Reviewed:" or "Edited:", is
replaced by a hash before anything is compared or printed.
"""
import collections
import hashlib
import sys
from xml.etree import ElementTree

RDF = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}"
XML_BASE = "{http://www.w3.org/XML/1998/namespace}base"
NAMED = ("Authored:", "Reviewed:", "Edited:")
CONTROLS = {"Control", "TemplateReactionRegulation"}
UNKEYED = {"nextStep"}


def local(tag):
    return tag.rsplit("}", 1)[-1]


def digest(text):
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]


def literal(prop, text):
    """A literal as compared: whitespace folded; a person's name hashed."""
    folded = " ".join(text.split())
    if prop == "author" or (prop == "comment" and folded.startswith(NAMED)):
        return f"<hashed {digest(folded)}>"
    return folded


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
        if local(element.tag) == "Ontology":
            element.clear()
            continue
        if element.get(RDF + "ID") is not None:
            name = f"{stem}#{element.get(RDF + 'ID')}"
        elif element.get(RDF + "about") is not None:
            name = resolve(element.get(RDF + "about"))
        else:
            raise ValueError(f"{path}: a {local(element.tag)} with no rdf:ID or rdf:about")
        props = []
        for child in element:
            prop = local(child.tag)
            if len(child):
                raise ValueError(f"{path}: an individual nested in a {local(element.tag)}'s {prop}")
            target = child.get(RDF + "resource")
            if target is not None:
                props.append((prop, ("r", resolve(target))))
            else:
                props.append((prop, ("l", literal(prop, child.text or ""))))
        if name in nodes:
            raise ValueError(f"{path}: two individuals named {name}")
        nodes[name] = (local(element.tag), props)
        element.clear()
    return nodes


def triples(path):
    """The file's multiset of (subject key, property, target) triples."""
    nodes = individuals(path)
    keys, open_keys = {}, set()

    def key(name):
        if name in keys:
            return keys[name]
        if name not in nodes:
            return f"EXTERNAL|{name}"
        cls, props = nodes[name]
        literals = {prop: value[1] for prop, value in props if value[0] == "l"}
        found = None
        if cls.endswith("Xref") and "db" in literals and "id" in literals:
            found = f"{cls}|{literals['db']}|{literals['id']}"
        elif cls.endswith("Vocabulary") and "term" in literals:
            found = f"{cls}|{literals['term']}"
        else:
            for prop, value in props:
                if prop == "xref" and value[0] == "r" and value[1] in nodes:
                    xref_cls, xref_props = nodes[value[1]]
                    xref = {p: v[1] for p, v in xref_props if v[0] == "l"}
                    if xref_cls == "UnificationXref" and xref.get("db", "").startswith("Reactome Database ID"):
                        found = f"{cls}|dbId:{xref.get('id')}"
            if found and cls in CONTROLS:
                controlled = sorted(key(v[1]) for p, v in props if p == "controlled" and v[0] == "r")
                found += "|controls:" + ",".join(controlled)
        if found is None:
            if name in open_keys:
                raise ValueError(f"{path}: a cycle through a {cls}")
            open_keys.add(name)
            parts = sorted(
                f"{prop}={key(value[1]) if value[0] == 'r' else value[1]}"
                for prop, value in props
                if prop not in UNKEYED
            )
            open_keys.discard(name)
            found = f"{cls}|#{digest(chr(10).join(parts))}"
        keys[name] = found
        return found

    sys.setrecursionlimit(max(sys.getrecursionlimit(), 100000))
    counted = collections.Counter()
    for name, (cls, props) in nodes.items():
        subject = key(name)
        for prop, value in props:
            counted[(subject, prop, key(value[1]) if value[0] == "r" else value[1])] += 1
    return counted


def differences(reference, other):
    """The triples only in each, as Counters."""
    return reference - other, other - reference


def kind(triple):
    return f"{triple[0].split('|', 1)[0]}.{triple[1]}"


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
                lines.append(f"    {side} {count} x {triple[0]} {triple[1]} {triple[2]}")
    return lines


def main(argv):
    args = argv[1:]
    examples = 0
    if args[:1] == ["--examples"] and len(args) > 1 and args[1].isdigit():
        examples, args = int(args[1]), args[2:]
    if len(args) != 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    only_reference, only_other = differences(triples(args[0]), triples(args[1]))
    print("\n".join(report(only_reference, only_other, examples)))
    return 1 if only_reference or only_other else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
