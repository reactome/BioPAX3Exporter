# The fixture graph (spec 001, T006)

A small graph for CI: nine events of release 98 (chosen on release 97), their parent pathways, and everything
the exporter's classes read about them (T011–T019), with every person's name, and all
free text that could cite one, replaced.

- **`fixture.cypher`**: 2,373 nodes and 4,714 relationships, for

  | Event         | What it is here for                                                                                         |
  | ------------- | ----------------------------------------------------------------------------------------------------------- |
  | R-HSA-68689   | a small pathway (two reactions), with its events                                                            |
  | R-HSA-6790038 | a TemplateReaction with its regulation (TemplateReactionRegulation)                                         |
  | R-HSA-174731  | a Degradation                                                                                               |
  | R-HSA-2894858 | a disease pathway (DOID cross-references)                                                                   |
  | R-HSA-9911234 | DNA and RNA                                                                                                 |
  | R-HSA-5696356 | sets within sets                                                                                            |
  | R-HSA-68825   | a black-box event                                                                                           |
  | R-HSA-5660013 | the first of research R3's greedy cover: a disease reaction (a FailedReaction), with catalysis and controls |
  | R-PFA-68947   | an inferred event, for Evidence (IEA)                                                                       |

  each with its parent pathways up to the top, and the release's `DBInfo` node.
  The shapes were chosen as the smallest human events with each (by the published human
  file); no human event is inferred, so that one is _P. falciparum_'s.

- **`expected/`**: the exporter's output from this graph, one file per species as the
  release writes them (`-s 48887`, Homo sapiens; `-s 170928`, _P. falciparum_). CI's
  `fixture` job converts both species, twice, and holds each file to its expected one by
  meaning (`ci/compare.py`), the second run to the first byte for byte (FR-006), and
  both to the BioPAX validator with no error allowed (`ci/validate.py`). Each content
  task (T011–T019) changes these files with the triples it means to change.
- **`extract.py`**: wrote `fixture.cypher`.
- **`check-placeholders.sh`**: fails if the fixture leaves its allowlists (below); CI runs it.
- **`audit.py`**: searches the fixture for its people's real names, against the private
  copy, at extraction; it prints counts only.

## How it was made

From a private copy of the release's graph: release 98 since 28 Oct 2026, release 97
before, each made with the same command (Neo4j 4.x, database `graph.db`, on the
loopback interface; never a shared or production graph):

```
python3 -I ci/fixture/extract.py http://<private-copy>:7474 R-HSA-68689 R-HSA-6790038 \
  R-HSA-174731 R-HSA-2894858 R-HSA-9911234 R-HSA-5696356 R-HSA-68825 R-HSA-5660013 \
  R-PFA-68947 > ci/fixture/fixture.cypher
ci/fixture/check-placeholders.sh
python3 -I ci/fixture/audit.py http://<private-copy>:7474
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
  fixture person's surname), and its display name, which is the text's opening;
- every publication's display name, which begins with its first author's;
- every comment.

The expected output (T007) is written from this graph, so nothing a test needs is lost.
Publications keep their PubMed ids, titles and journals, which are public: their authors
can be found through PubMed, not through this repository.
A dbId is kept, as the exporter writes it; the content service resolves it to the
object, a person's name included, so the placeholders hide a name from this repository,
not from the service.

## How it was checked

- **It loads:** into an empty Neo4j 4.4 (`graph.db`) with `cypher-shell -f`, in 67 s,
  giving 2,373 nodes and 4,714 relationships.
- **No names, by allowlist (CI):** `check-placeholders.sh` passes on the fixture. It holds
  people, edits, summations, publications' display names and comments to placeholders;
  every line to the four shapes `extract.py` writes; every node's labels to the graph
  classes the fixture holds, its `schemaClass` among them; and refuses, on any node, a
  key from its list of person-like
  names (in any capitalisation, underscores and hyphens aside; a longer key containing
  one, such as `authorName`, is not matched) and any value
  like an email address or an ORCID. It cannot judge other free text (a
  title, a journal): the audit below covers that, at extraction. It fails on the first
  two drafts (the second kept summations' display names) and on copies of the fixture
  altered in each of these ways (invented values only):
  - a placeholder's surname replaced;
  - a property added to a person that is not on the allowlist;
  - a person's display name naming another placeholder;
  - an `InstanceEdit`'s display name replaced;
  - an `orcidId` put on another node;
  - a `SET` line appended, or a person written another way, or an indented `CREATE`
    (every line must be one of the four shapes `extract.py` writes);
  - a first name on a node that is not a person;
  - a comment added to an `InstanceEdit`;
  - a property added to a relationship, or a relationship's number written as text;
  - a summation's display name replaced;
  - an email address and an ORCID in a publication's title;
  - an edit's date that is not a date;
  - a node of a class the fixture does not hold;
  - `lastName`, or `Surname` capitalised, on another node;
  - a fixture with no people.
- **No names, by search (at extraction):** `audit.py` compares every string in the
  fixture (19,190, list items included) with the real surnames and first names (of three
  letters or more) of its 1,235 people in the private copy, and looks for emails and
  ORCIDs, counts only. One string matches: a UniProt keyword naming a disease after a
  person, one of whose names is also a fixture person's first name. It is judged a
  coincidence and listed in `audit.py`'s `KNOWN`, by its node; any other hit fails it. It matches
  whole words, as written: an upper-cased name, a name with a suffix, a name under three
  letters, a part of a multi-word name, or one spelled without its diacritics is not
  searched, which #25's review measured as noise on this fixture. The first draft had 44 (43 summations and a Book's
  display name). #23's review compared it with all 176,891 people in the graph (short
  and multi-word surnames, first names, initial-and-surname pairs, emails, ORCIDs) and
  found only common words, a journal abbreviation and a protein name. Repeat both
  whenever the events change.
- **URLs:** a URL publication's address can hold an author's name, and the fixture keeps
  no real name to check it against. So at extraction each URL publication's address was
  checked against its own authors' surnames in the private copy: neither of the two
  holds one. Repeat that check whenever the events change.
- **Complete:** whether it holds all the exporter reads is shown in T007, where CI runs
  the exporter over it, and grows with each content task (T011–T019). A parent pathway
  holds only the child on the way down, so only R-HSA-68689 has its whole step graph
  (`nextStep`); the others' steps are partial, which T015 should know.

To change the events, rerun both commands against a private copy and repeat the checks.
