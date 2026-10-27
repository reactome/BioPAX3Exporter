# The fixture graph (spec 001, T006)

A small graph for CI: nine events of release 97, their parent pathways, and everything
the exporter's classes read about them (T011–T019), with every person's name replaced.

- **`fixture.cypher`**: 2,373 nodes and 4,714 relationships, for

  | Event | What it is here for |
  | --- | --- |
  | R-HSA-68689 | a small pathway (two reactions), with its events |
  | R-HSA-6790038 | a TemplateReaction with its regulation (TemplateReactionRegulation) |
  | R-HSA-174731 | a Degradation |
  | R-HSA-2894858 | a disease reaction (DOID cross-references) |
  | R-HSA-9911234 | DNA and RNA |
  | R-HSA-5696356 | sets within sets |
  | R-HSA-68825 | a black-box event |
  | R-HSA-5660013 | the first of research R3's greedy cover (catalysis and controls) |
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
genes or its long cross-reference lists. Every `Person` keeps only its dbId and class
and becomes `Placeholder` `PersonN`; every `InstanceEdit`'s display name and note are
placeholders.

## How it was checked

- **It loads:** into an empty Neo4j 4.4 (`graph.db`) with `cypher-shell -f`, in 67 s,
  giving 2,373 nodes and 4,714 relationships.
- **No names:** `check-placeholders.sh` passes on the fixture, and fails on a copy with:
  - a placeholder's surname replaced;
  - a property added to a person that is not on the allowlist;
  - a person's display name naming another placeholder;
  - an `InstanceEdit`'s display name replaced;
  - an `orcidId` put on another node.
- **URLs:** a URL publication's address can hold an author's name, and the fixture keeps
  no real name to check it against. So at extraction each URL publication's address was
  checked against its own authors' surnames in the private copy: neither of the two
  holds one. Repeat that check whenever the events change.
- **Complete:** whether it holds all the exporter reads is shown in T007, where CI runs
  the exporter over it, and grows with each content task (T011–T019).

To change the events, rerun both commands against a private copy and repeat the checks.
