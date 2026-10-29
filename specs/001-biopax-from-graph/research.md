# Research: BioPAX Level 3 from the graph

Measured 27 Oct 2026 on release 97's published `biopax.zip` (15 species files, 2.1 GB unpacked; fetched once from the release's download directory, kept outside the repository) and on this repository at `d88d5df`. No graph database or Reactome service was used. People's names in the files (literature authors, curation comments) were counted or hashed, never printed. Earlier research is reactome/WebsiteAngular spec 015's R4, R5 and R9, and its data-model.md, which maps each BioPAX class and property to the graph.

## R1. What the published species file holds

**Measured** on _Plasmodium falciparum_ (30.5 MB) and, where cheap, _Homo sapiens_ (333 MB, streamed).

- **The writing:** JDOM's (CRLF line endings, 2-space indent), `rdf:datatype` on every literal; namespaces `rdf`, `bp`, `rdfs`, `owl`, `xsd`; `owl:Ontology rdf:about=""` with one `owl:imports`.
- **`xml:base`** is `http://www.reactome.org/biopax/{release}/{species dbId}#` (`…/97/170928#`, `…/97/48887#` for human).
- **Ids** are `{Class}{n}` in every individual (52,302 in _P. falciparum_, 546,714 in human).
- **Classes** (_P. falciparum_; human in brackets; "in human" below means in human and not in _P. falciparum_, the two files measured): UnificationXref 17,026 (182,500), PublicationXref 5,648 (39,543), Protein 3,161 (33,688), Stoichiometry 2,960 (44,180), BiochemicalReaction 966 (16,079), Catalysis 689 (6,833), Pathway 658 (2,883), PhysicalEntity 413 (2,158), Control 67 (3,247), Evidence 1,624 (0), PathwayStep 1,598, Complex 1,091, SmallMolecule 1,269, ProteinReference 993, SmallMoleculeReference 723, features and vocabularies. In human, not in _P. falciparum_: Dna, DnaReference, Rna, RnaReference, **Degradation (20), TemplateReaction (8), TemplateReactionRegulation (3)**.
- **Reactions:** `conversionDirection` LEFT-TO-RIGHT; Degradation has `left` only, TemplateReaction `product` only. Most stoichiometries hang off `Complex.componentStoichiometry`.
- **Catalysis:** `controlType` ACTIVATION; controller a Complex, Protein or PhysicalEntity; two RelationshipXrefs (the catalyst activity and its GO molecular function); an `activeUnit:` comment where there is one.
- **Control:** ACTIVATION or INHIBITION; one individual per controlled reaction (in _P. falciparum_ six regulations are written as 18 Controls, each with its own `controlled`); UnificationXrefs for the regulation's database id (67) and, on 39 of them, its stable id; it controls BiochemicalReaction and, in human, Degradation.
- **Events' own cross-references:** RelationshipXrefs on pathways (446) and reactions (56): GO biological process, disease (DOID) and others; where the old converter returns early on a repeat (WebsiteAngular spec 015, R4), fewer than the graph holds.
- **PathwayStep:** `stepProcess` names a reaction, a catalysis, a control or a pathway; `nextStep` follows the event order. Every Catalysis and Control is a step's process, none a pathway component.
- **Evidence** exists only for inferred events (IEA, with an xref to the publication and a comment). Human, curated, has none: its literature is a PublicationXref on the event itself.
- **Every physical entity** carries a "Reactome DB_ID" comment and two UnificationXrefs: the database id of the release and the stable id with its version.
- **Curation comments** ("Authored:", "Reviewed:", "Edited:") on human events carry people's names, as PublicationXref's `author` does.
- **EC numbers** are literal `eCNumber` values, several per reaction: up to 84 on one human reaction, 20,384 in all. The graph keeps one per molecular function (reactome/graph-importer#9): the largest of the data's differences.
- **Sets** are written as the class their members share, else as PhysicalEntity, with "Converted from EntitySet in Reactome" and `memberPhysicalEntity`. Of the 136 single-class sets written as PhysicalEntity in _P. falciparum_, 103 have a set among their members, and every one of those also has a member with no entity reference: the two rules fit equally, and 33 more all-SmallMolecule sets are PhysicalEntity with neither nesting. The graph decides (R6; answered in T017).
- **No header pathway** named for the species (0 in both files). Top-level pathways: 26 (_P. falciparum_), 29 (human).
- **No reaction outside a pathway** (0 of 966 in _P. falciparum_, none in human). The converter writes a species' top-level events, so a reaction of the species alone that no such event holds would be written alone. Release 98 has none either: its graph's top-level events, 26 and 29, are the top-level pathways of release 98's published files (T015, `differences.md`).
- **A shared child event is written once** and referenced by each parent (in _P. falciparum_, 61 children have two parents, six have three to seven). A Control is the exception: one per controlled reaction (above).
- **Individuals of identical content repeat** (SequenceSite 4,912 extra, Stoichiometry 1,077 in _P. falciparum_; far more in human), so files compare as multisets.

**Decision:** these are what the exporter writes. Of spec 001's two edge cases left to measurement, one is settled (an event in no pathway is written only if it is top-level, and release 98 has none); the sets rule is left to the graph.

## R2. The comparison by meaning

| Approach                                             | _P. falciparum_   | Human (333 MB)                        |
| ---------------------------------------------------- | ----------------- | ------------------------------------- |
| Python lxml, streaming, to triples with content keys | 3.4 s, 122 MB     | 56 s, 1.05 GB (2,711,269 triples)     |
| Paxtools 6.0.0 (load only)                           | 2.0 s, 386 MB RSS | 15 s, 2.3 GB RSS                      |
| rdflib 7.1.4 (parse only)                            | 21.7 s, 420 MB    | not run (about 4 min, 4.5 GB by size) |

**Decision:** a Python comparer (`ci/compare.py`), streaming. Each individual's `rdf:ID` is replaced by a content key: an event or entity by its "Reactome Database ID Release {n}" xrefs; anything else, xrefs and vocabularies included, by a hash of its content, built bottom-up, leaving `nextStep` out of the key. Individuals count as a multiset, a value within one individual once. Literals of `author` and of comments beginning "Authored:", "Reviewed:" or "Edited:" are hashed, never printed. (As built in T005, #18; at first xrefs were keyed by (class, db, id) and vocabularies by term, which T005's review showed blind to which chapter of a book is cited.)

**Shown sound** on _P. falciparum_: re-pointing one set's `memberPhysicalEntity` gives one triple on each side; changing one stoichiometric coefficient gives three triples on each side. (Swapping two proteins' ids consistently gives none, as it must for any comparison blind to ids, so it shows nothing.) A Control is keyed by its regulation's database id and the reaction it controls, since one regulation is several Controls.

**Rationale:** independent of the code under test (Paxtools both writes the files and would read them, normalising on load, which would hide what it changes; constitution II); the same language as sbml-exporter's `ci/compare.py`; human in under a minute.

**Alternatives:** Paxtools (above); rdflib (seven times slower, and collapses repeated triples).

## R3. The fixture graph and continuous integration

**Decision:** sbml-exporter's fixture pattern (its `ci/fixture/extract.py` over a private copy's HTTP API, with placeholders and a placeholder check), extended to what BioPAX writes and SBML does not: publications' authors (as placeholder people), events' created, authored, reviewed and edited records, entity references with their cross-references, GO terms, diseases and PSI-MOD terms, `inferredFrom`, catalyst activities with their active units, and regulation.

A greedy cover over the published files finds that 29 human events cover every class, control type, set kind and xref database (2,618 individuals); the first ten reach every class. **The fixture:** about five human events (a pathway, a TemplateReaction with its regulation, a Degradation, a disease reaction, a reaction with nested sets, DNA and RNA) and one inferred event, for Evidence.

**The validator:** BioPAX validator 6.0.0 from Maven Central (`org/biopax/validator/distr/6.0.0/distr-6.0.0-all.zip`, 77.9 MB, sha1 `d3c7e7402a3b11915cd8e75b5d7a1d682dfce01c`), JDK 17, run as `java -javaagent:lib/spring-instrument-6.1.5.jar -Xmx4g -jar biopax-validator.jar <dir> --profile=notstrict --out-format=xml` with two `--add-opens`. On the published _P. falciparum_ file: 29 s, 1.0 GB, exit 0, **0 errors** and 16,863 warnings in 19 codes. Its exit status says nothing; CI reads `validation/@notFixedErrors` and the `error[@type='ERROR']` elements, and prints only codes and counts (its log echoes display names). Cached by its sha1.

## R4. The code today

- **Shared state:** `BioPAX3Utils` is an `object` with one global list of id counters, reset by every `WriteBioPAX3` constructor; `getID()` reads the companion's `xmlBase`; `dbVersion` and the validator are companion statics. The companion's `init` sets `java.vm.*` system properties and starts the validator whenever the class loads.
- **Builders:** species mode makes one `BioPAXPathwayBuilder` per pathway, each with its own `visitedPathways`, which is why a sub-pathway is written again under every ancestor; there is no reaction cache, and the entity cache lives per reaction; `getObjectFromSet` scans linearly (quadratic at human scale; `Model.getByID` is constant time). `BioPAXEventBuilder` asks for an abstract `Process`. `BioPAX3ReferenceUtils` strips HTML from comments, where the published files keep it.
- **Tests:** `mvn test` runs none ("No tests to run"): the Kotlin plugin binds only `compile`, there is no `testSourceDirectory`, and surefire's `**/*Test.kt` matches no class. Bound and compiled, the tests show 82 compile errors in five of seven files.
- **Single events and `-l`:** `BioPAXEventBuilder` asks for an abstract `Process` and is never called; `-l` builds an empty pathway. (Resolved in T019.)
- **Lint:** `ktlint:format` at `process-sources` rewrites files on every build; `mvn ktlint:check` alone finds five violations in two files; the Dockerfile's lint stage formats first, so it can never fail. CI only builds and pushes the image.
- **Versions:** graph-core 2.0.9, Paxtools 4.3.1, validator 5.1.0, Kotlin 1.7.10, when measured. Since then: Paxtools 6.0.0, Kotlin 2.4.21 and JUnit 6.1.3 (#66), with the fixture's output unchanged to the byte. sbml-exporter and the deployed content service use graph-core 3.0.0-SNAPSHOT pinned by commit (sbml-exporter: by the resolved files' sha1, since T010 here too).

**Decisions:**

- **One conversion, one context.** A `ConversionContext` per conversion holds the model, the base, the counters and the caches (events and entities by dbId); the builders take it; no companion state. The validator moves out of the library to the command line and CI.
- **Two modules,** as sbml-exporter's: `biopax3-exporter-lib` (builders and context; graph-core and paxtools-core only; `convert(event, release)` and `convertSpecies(species, release)`) and `biopax3-exporter` (the command line, shaded: launcher, options, logging, optionally the validator).
- **graph-core** moves to the version sbml-exporter pins, so the two exporters and the website's library service read the graph through the same code.
- **The tests** are bound and repaired before anything else, each failing first.
- **Lint** checks at `validate`, and nothing rewrites sources.

## R5. Bytes

Release 97's converter writes with JDOM (CRLF, its own indent); Paxtools writes LF and its own layout. The comparison is by meaning (R2), so the bytes differ and nothing reads them; the release's check of the download directory reads only `xml:base` (WebsiteAngular spec 015, R9), which FR-007 keeps.

## R6. Unresolved, carried into the tasks

- Why 33 all-SmallMolecule sets are PhysicalEntity: answered in T017 (`differences.md`, Sets): they hold chemical drugs, whose reference's kind is written as PhysicalEntity.
- That Evidence is written only for inferred events in the other inferred species: checked in the full run.
- The graph-core version to pin: sbml-exporter's, unless WebsiteAngular spec 015's T029 settles another first.
