# The fixture graph (spec 001, T006)

A small graph for CI: nine events of release 97, their parent pathways, and everything
the exporter's classes read about them (T011–T019), with every person's name, and all
free text that could cite one, replaced.

- **`fixture.cypher`**: 2,373 nodes and 4,714 relationships, for

  | Event | What it is here for |
  | --- | --- |
  | R-HSA-68689 | a small pathway (two reactions), with its events |
  | R-HSA-6790038 | a TemplateReaction with its regulation (TemplateReactionRegulation) |
  | R-HSA-174731 | a Degradation |
  | R-HSA-2894858 | a disease pathway (DOID cross-references) |
  | R-HSA-9911234 | DNA and RNA |
  | R-HSA-5696356 | sets within sets |
  | R-HSA-68825 | a black-box event |
  | R-HSA-5660013 | the first of research R3's greedy cover: a disease reaction (a FailedReaction), with catalysis and controls |
  | R-PFA-68947 | an inferred event, for Evidence (IEA) |

  each with its parent pathways up to the top, and release 97's `DBInfo` node.
  The shapes were chosen as the smallest human events with each (by the published human
  file); no human event is inferred, so that one is _P. falciparum_'s.
- **`extract.py`**: wrote `fixture.cypher`.
- **`check-placeholders.sh`**: fails if a person's name could have survived; CI runs it.

## How it was made

From a private copy of release 97's graph (Neo4j 4.4, database `graph.db`, on the
loopback interface; never a shared or production graph):

```
python3 -I ci/fixture/extract.py http://<private-copy>:7474 R-HSA-68689 R-HSA-6790038 \
  R-HSA-174731 R-HSA-2894858 R-HSA-9911234 R-HSA-5696356 R-HSA-68825 R-HSA-5660013 \
  R-PFA-68947 > ci/fixture/fixture.cypher
ci/fixture/check-placeholders.sh
```

`extract.py` follows, in their direction, the relationships the exporter's classes read,
and keeps every node's full label set and properties, which Spring Data Neo4j maps by.
Parent pathways are taken for themselves and the one `hasEvent` on the way down, not
their other events. Species, compartments, GO terms, diseases, PSI-MOD terms, reference
databases and people are taken as leaves, as are events outside the fixture that a taken
one names (a preceding event, a normal reaction, the source of an inference).
Reference sequences' own cross-references (7,483 of the first draft's 7,617) and genes
are left out: the published files write a reference entity's own identifier, never its
genes or its long cross-reference lists.

What could name a person is replaced:
- every `Person` keeps only its dbId and class and becomes `Placeholder` `PersonN`;
- every `InstanceEdit`'s display name and note;
- every summation's text, which cites authors ("X et al": 43 of the first draft's held a
  fixture person's surname);
- every publication's display name, which begins with its first author's;
- every comment.

The expected output (T007) is written from this graph, so nothing a test needs is lost.
A dbId is kept, as the exporter writes it; the content service resolves it to the
object, a person's name included, so the placeholders hide a name from this repository,
not from the service.

## How it was checked

- **It loads:** into an empty Neo4j 4.4 (`graph.db`) with `cypher-shell -f`, in 67 s,
  giving 2,373 nodes and 4,714 relationships.
- **No names, by allowlist:** `check-placeholders.sh` passes on the fixture. It fails on
  the first draft, which kept summation texts and publication display names, and on
  copies of the fixture altered in each of these ways (invented values only):
  - a placeholder's surname replaced;
  - a property added to a person that is not on the allowlist;
  - a person's display name naming another placeholder;
  - an `InstanceEdit`'s display name replaced;
  - an `orcidId` put on another node;
  - a `SET` line appended, or a person written another way, or an indented `CREATE`
    (every line must be one of the four shapes `extract.py` writes);
  - a first name on a node that is not a person;
  - a comment added to an `InstanceEdit`;
  - a property added to a relationship;
  - a fixture with no people.
- **No names, by search:** every string in the fixture was compared with the real surnames
  (of four letters or more) of its 1,235 people in the private copy, counts only: none
  holds one. The first draft had 44 (43 summations and a Book's display name).
- **URLs:** a URL publication's address can hold an author's name, and the fixture keeps
  no real name to check it against. So at extraction each URL publication's address was
  checked against its own authors' surnames in the private copy: neither of the two
  holds one. Repeat that check whenever the events change.
- **Complete:** whether it holds all the exporter reads is shown in T007, where CI runs
  the exporter over it, and grows with each content task (T011–T019). A parent pathway
  holds only the child on the way down, so only R-HSA-68689 has its whole step graph
  (`nextStep`); the others' steps are partial, which T015 should know.

To change the events, rerun both commands against a private copy and repeat the checks.
