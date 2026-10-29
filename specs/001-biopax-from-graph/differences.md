# Differences from the release's BioPAX

Every way this exporter's output differs from the release's published `biopax.zip`, with its count and its reason (spec 001, FR-002). A difference may stay for one of three reasons only: the order and ids; the data, with its ticket; or an obvious bug of the old converter, fixed here. The comparison is by meaning (`ci/compare.py`, research R2).

## Found while building the comparer

- **`conversionDirection`.** Paxtools 6.0.0 writes `LEFT_TO_RIGHT` where release 97 has `LEFT-TO-RIGHT`. In _P. falciparum_ that is 966 triples on each side, one per BiochemicalReaction, measured by reading the published file with Paxtools 6.0.0 and writing it back. Which form the exporter writes, and whether the validator accepts both, is decided in T012.
- **What a Paxtools round trip changes.** Reading the published _P. falciparum_ file with Paxtools 6.0.0 and writing it back gives 4,176 triples only in the published file and 4,163 only in the copy. At the root (`ci/compare.py`) there are 2,639; the rest is carried by rehashed keys. The root changes:
  - the direction above, 966 a side;
  - 334 Evidence comments, which #17's review traced to Paxtools' reader, not its writer;
  - names: displayName (9 on the published side, 7 on the copy's), name (BiochemicalReaction 4 a side, SmallMoleculeReference 1 a side), 8 Protein and 3 SmallMolecule names on the published side only;
  - one PublicationXref title, 1 a side.

  What the reader does to names is not yet traced. A file written from the graph is compared in T020.

- **Values repeated within one individual.** Release 97 writes some values twice on one individual: 13 in _P. falciparum_ and 2,421 in human, 2,292 of them a `Protein.feature` written twice. The comparer counts a repeat once, since Paxtools keeps values as sets. Two individuals of the same content, referred to twice, still count twice; human has 15 such references. Whether a protein's repeated modification should be one feature or two is decided in T011.

## Physical entities (T011)

Measured 28 Oct 2026 on _P. falciparum_, converted from a private copy of release 97's graph before and after T011, each compared with the published file.

- **The whole file** (`ci/compare.py`): before, 253,233 triples only in the published file and 80,413 only in the converted one; after, 196,639 and 67,333. Most of what remains is not T011's: the catalysts' entities (T013) and the sets' members (T017) are not written yet, so the entities only they reach are missing.
- **Entity by entity:** the 2,619 entities the converted file writes are all in the published file. Compared one by one, with ids aside and other entities named by their database id, 1,498 agree as written, and 711 complexes more differ only in how a coefficient is spelled (below). The rest differ only in these ways:
  - **Sets** (384): written as PhysicalEntity with no members or "Converted from EntitySet" comment yet. That is T017's.
  - **Mondo cross-references** (14 xrefs). They are in the graph, put there by `reactome/graph-add-links` after the relational database the release's converter read. The published human file has no Mondo xref either. The exporter writes the graph's cross-references, so these are written: a difference of the data.
  - **Cross-references and diseases the old converter skipped** (2 KEGG COMPOUND, 3 DOID). Its loop over an entity's cross-references, and its loop over its diseases, stopped at the first one it had already written for another entity. Every one is written here: an obvious bug, fixed. The release's events have the same bug, fixed in T014.
  - **A name equal to the display name** (8 proteins, 2 small molecules). Where the graph lists a name twice, the release's converter took one copy as the display name and wrote the other as a name. Paxtools adds the display name to an individual's names and writes it only as `displayName`, so that name cannot be written apart. The same 8 and 2 appeared in the Paxtools round trip above: a difference of the writer.
  - **A modification repeated on one protein** (2 proteins). The graph's `hasModifiedResidue` has a stoichiometry of 2, and the release wrote its one ModificationFeature twice. A BioPAX feature carries no count, and Paxtools keeps an individual's values as a set, so it is written once. `ci/compare.py` already counts a repeated value once. This settles the question left above.
  - **`stoichiometricCoefficient`** is written `1.0` where the release wrote `1`. Both are the same `xsd:float`, and `ci/compare.py` compares them by value, so this is not a difference.
- **Read from the graph's `databaseName`.** An xref's `db` is the record's `databaseName`, the name of its reference database as the graph keeps it on the record, so the database itself is never read. In release 97 it equals the reference database's display name on every reference entity, cross-reference and ontology term that has one. The exceptions are two malformed ReferenceDNASequence nodes that no entity refers to, so they are never written.
- **The organism.** One BioSource per taxon, with its NCBI Taxonomy UnificationXref, shared by pathways and entity references, as the release writes it.

## Reactions (T012)

Measured 28 Oct 2026 on _P. falciparum_ from a private copy of release 98's graph, against release 98's published file, and on the fixture's human events against release 98's human file.

- **Reaction by reaction:** all 970 _P. falciparum_ reactions are in the published file under their database id. Their kind, names, participants (`left`, `right`), stoichiometries, EC numbers, Reactome's two ids and data source agree. What differs belongs to other tasks or to the writer:
  - **`conversionDirection`** is written `LEFT_TO_RIGHT` where the release writes `LEFT-TO-RIGHT` (970). Paxtools' `ConversionDirectionType` has only the underscored form (4.3.1, and 6.0.0 still), and its reader takes either. It is a difference of the writer, settling the question above. The validator 6.0.0 reports no error on it: the fixture's output, every reaction written so, has none.
  - **The summation comment** has its HTML stripped (970): T016 keeps it.
  - **Evidence** is written for every reaction, empty, where the release writes it for inferred events with its evidence code (970): T016.
  - **GO biological process RelationshipXrefs** are missing (60): T014.
  - **`stoichiometricCoefficient`** is written `3.0` where the release writes `3`: the same `xsd:float`, so not a difference by `ci/compare.py`.
- **The whole file** (`ci/compare.py`): T011's output against the published file, 197,744 triples only in the published file and 67,553 only in the converted one; T012's, 181,838 and 81,839. The converted side grows because each reaction is written once for every pathway that reaches it: 4,136 BiochemicalReactions against the release's 970, and each copy now carries its names, ids and direction. Writing a shared event once is T015's; until then the reaction-by-reaction comparison is the measure.
- **The kinds**, on the fixture's human events: R-HSA-174731 is a Degradation (`left` only) and R-HSA-6790038 a TemplateReaction (`product` only), as release 98 writes them.
- **A TemplateReaction's product is not also a participant.** Paxtools (4.3.1, and 6.0.0 still) adds a product to an interaction's participants and writes both, though `product` is itself a participant by BioPAX's definition. The release writes the product alone, so the exporter removes the extra participant.
- **EC numbers in release 98.** Release 98's published human file has 3,594 `eCNumber` values, against release 97's 20,384 (research R1). The graph gives exactly those, by this task's rule (from #42's review): 3,597 distinct reaction and EC number pairs in human reactions a pathway reaches, less 4 on Degradations, which take none, plus one value the release writes twice (R-HSA-9698988's two catalysts share 3.4.21.1; the same triple, written once here). The data's difference in EC numbers (reactome/graph-importer#9), the largest at release 97, is gone at release 98; the full run (T020) confirms it on every species.

## Catalyses and regulations (T013)

Measured 28 Oct 2026 on _P. falciparum_ from a private copy of release 98's graph, against release 98's published file.

- **The published file's catalyses and controls are matched.** At the root, before and after this task, the published side's triples that the converted file lacks:

  | Triples                                                           | Before   | After |
  | ----------------------------------------------------------------- | -------- | ----- |
  | Catalysis: `controlType`, `dataSource`                            | 691 each | 0     |
  | Catalysis: `xref` (molecular function, and the activity's own id) | 1,382    | 0     |
  | Catalysis: `controller`                                           | 691      | 156   |
  | Catalysis: active units                                           | 134      | 28    |
  | Control: `controlType`, `controlled`, `dataSource`                | 67 each  | 0     |
  | Control: `xref`                                                   | 106      | 0     |
  | Control: `controller`                                             | 67       | 4     |

- **What remains are sets** (T017): each remaining controller or active unit is a set, which the release writes as the class its members share (a Protein, a Complex) and this exporter still writes as a PhysicalEntity; or the catalysis names one, so its key, made from its content, differs.
- **The converted side has more:** 2,933 catalyses against 691, because a reaction was then written once for each pathway that reaches it, its catalyses with it (T015 writes it once).
- **A regulation listed twice on a reaction is written once.** graph-core lists a related object once for each count of its relationship's stoichiometry, and at release 98 eleven `regulatedBy` links have a stoichiometry of 2 (the one Requirement of R-xxx-6814559, inferred into eleven species, none of them human or _P. falciparum_). The release's converter gathers a reaction's regulations into a set, so each is one Control; the exporter takes them once each too (from #57's review).
- **A pathway is never regulated in the graph:** graph-core's `Regulation` regulates reactions only. The release's converter would put a pathway's regulations in its step; there are none to put.
- **A catalyst activity regulated as an entity** (the converter's Modulation, "not supported in the new data model after release 63"): the graph has no such link, so none is written, as none is published.
- **The active unit's comment names an rdf:ID** (`activeUnit: #Protein12`), numbered as each file is written. `ci/compare.py` now reads it as a reference to the entity it names, so the comparison sees the entity, not the number.

## Events' own cross-references (T014)

Measured 28 Oct 2026 on _P. falciparum_ from a private copy of release 98's graph, against release 98's published file, and on the fixture's human events against release 98's human file.

- **What events carry:** in release 98's files, an event's only RelationshipXrefs are its GO biological process ("gene ontology term for cellular process") and its diseases (DOID, "additional information"): human has 2,294 and 2,846 of them, _P. falciparum_ 506 GO terms and no disease. These are what the release's converter writes (`handleEventGOBP`, `handleDisease`), and what this exporter writes, for reactions and pathways alike. "The rest" in the task's wording turned out to be nothing more.
- **Reactions:** the published file's 60 GO xrefs on _P. falciparum_ reactions are all matched. The converted file has more (159 at the root), because a reaction was then written once for each pathway that reaches it (T015 writes it once).
- **Pathways:** their 446 GO xrefs are written. The comparison could not key a pathway by its database id before T015 gave it one; matched by name, the GO terms are identical for each of the 628 _P. falciparum_ pathways whose name is unique in the file (#59's review).
- **Order:** for a reaction, the GO term comes before its regulations and the diseases after the data source, as in the converter. For a pathway the converter writes its children first and then its own xrefs, while this exporter then wrote the pathway's own first. Only ids were numbered differently, which `ci/compare.py` does not see; T015 writes them in the converter's order.
- **Diseases the old converter skipped:** as for an entity's (T011), its loop over an event's diseases stopped at the first one it had already written. Every one is written here: an obvious bug, fixed. In the fixture, 23 disease xrefs the release left out are written: on reactions, DOID 162 (cancer) on 19 and DOID 5603 on 2 where 162 came first; on pathways, DOID 162 on 2. For the events of release 98's human file, the graph gives 379 such xrefs (269 on reactions, 110 on pathways): for each of 280 events, the published diseases are an ordered prefix of the graph's list, and for no event do they differ otherwise (#59's review, against the private copy).
- **A GO term's xref is written once** and shared by everything that names it; no term is both a molecular function and a biological process in release 98.

## Pathways and steps (T015)

Measured 29 Oct 2026 on _P. falciparum_, from a private copy of release 98's graph, against release 98's published file.

- **Which events a file is written from:** those of the species alone, less those another such event holds, as the release's `SpeciesAllPathwaysConverter` chose them (`filterEventsForSpecies`, then `grepTopLevelEvents`). That gives 26 events for _P. falciparum_ and 29 for human, the top-level events of the published files. Without the species filter, human would start from 345. The fixture has no event of two species, so CI does not hold that filter; it was checked here, on the private copy.
- **Each event once:** a pathway or reaction that several pathways hold is written once and is a component of each. The species' file has no header pathway. _P. falciparum_'s pathways (660), reactions (970), steps (1,604) and catalyses (691) number what the published file's do. Triples only in the published file fall from 160,049 to 146,281, and only in the converted file from 95,201 to 9,842.
- **Steps:** one for each event however many pathways hold it, in the order of each. Its processes are its event, and for a reaction its catalyses and controls. Its next steps are those of the events the event precedes, where they are in the file. Every Pathway, PathwayStep, `pathwayOrder`, `stepProcess` and `nextStep` difference left is carried by the reactions' differences, which are T016's and the writer's.
- **Order:** a pathway is made and kept before its events, and its own properties and xrefs come after them, as in the converter. Ids are now numbered in that order, and an event that holds the pathway again (a cycle, which release 98 does not have) finds it there rather than making it again.
- **A reaction of the species alone that no such event holds:** the release's converter writes every top-level event of the species, a reaction too, alone and with no step, and so does this exporter. Release 98 has none, so its files have no event outside a pathway (research R1).
- **What still differs at the root, for pathways:** the comment (660; HTML kept, T016) and the evidence (660, T016). Six display names also differ, by a trailing space the graph has and the published file does not. The release's writer trims every literal (no value in the published file starts or ends with white space), so that is the writer's, T018.
- **The fixture:** its expected files had each pathway once per pathway above it, and a header: 217 event individuals for human, 33 for _P. falciparum_. They now have 57 (28 pathways, 29 reactions) and 8, with the same reactions. The validator gives 0 errors on both (558 and 32 warnings).

## Paxtools 6, Kotlin 2.4, JUnit 6 (#65)

Measured 29 Oct 2026. The fixture's output is unchanged to the byte, and _P. falciparum_ from a private copy of release 98's graph is unchanged by `ci/compare.py`. Its bytes differ in one way only: Paxtools 4.3.1 wrote a character outside ASCII as a character reference (`&#8722;`, 8 in the file), and Paxtools 6.0.0 writes it as itself, as release 98's published file does (937 lines with such characters, no references). The two behaviours recorded above as Paxtools 4.3.1's, the underscored `conversionDirection` and a product added to the participants, are 6.0.0's too. Paxtools 6 deprecates `Control.controlled` with no replacement yet; each control here has one process, as the release writes it.

## The validator's baseline

The BioPAX validator 6.0.0 (`--profile=notstrict`), read by `ci/validate.py`, on the published files of release 97. A written file may have no ERROR its published counterpart lacks (SC-003); warnings are recorded, not held.

| Species                 | Errors | Warnings | Codes |
| ----------------------- | ------ | -------- | ----- |
| _Plasmodium falciparum_ | 0      | 16,863   | 19    |

Measured 27 Oct 2026. A run takes about four minutes by the shell's clock (249 s with the network off, 242 s with it on), most of it the validator's start-up. The other species' baselines are made in T020.
