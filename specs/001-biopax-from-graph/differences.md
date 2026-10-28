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

Measured 28 Oct 2026 on _P. falciparum_ from a private copy of release 98's graph, against release 98's published file, and on the fixture's human reactions against release 98's human file.

- **Reaction by reaction:** all 970 _P. falciparum_ reactions are in the published file under their database id. Their kind, names, participants (`left`, `right`), stoichiometries, EC numbers, Reactome's two ids and data source agree. What differs belongs to other tasks or to the writer:
  - **`conversionDirection`** is written `LEFT_TO_RIGHT` where the release writes `LEFT-TO-RIGHT` (970). Paxtools 4.3.1's `ConversionDirectionType` has only the underscored form, and its reader takes either. It is a difference of the writer, settling the question above. The validator 6.0.0 reports no error on it: the fixture's output, every reaction written so, has none.
  - **The summation comment** has its HTML stripped (970): T016 keeps it.
  - **Evidence** is written for every reaction, empty, where the release writes it for inferred events with its evidence code (970): T016.
  - **GO biological process RelationshipXrefs** are missing (60): T014.
  - **`stoichiometricCoefficient`** is written `3.0` where the release writes `3`: the same `xsd:float`, so not a difference by `ci/compare.py`.
- **The whole file** (`ci/compare.py`): T011's output against the published file, 197,744 triples only in the published file and 67,553 only in the converted one; T012's, 181,838 and 81,839. The converted side grows because each reaction is written once for every pathway that reaches it: 4,136 BiochemicalReactions against the release's 970, and each copy now carries its names, ids and direction. Writing a shared event once is T015's; until then the reaction-by-reaction comparison is the measure.
- **The kinds**, on the fixture's human events: R-HSA-174731 is a Degradation (`left` only) and R-HSA-6790038 a TemplateReaction (`product` only), as release 98 writes them.
- **A TemplateReaction's product is not also a participant.** Paxtools 4.3.1 adds a product to an interaction's participants and writes both, though `product` is itself a participant by BioPAX's definition. The release writes the product alone, so the exporter removes the extra participant.
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
- **The converted side has more:** 2,242 catalyses against 691, because a reaction is still written once for each pathway that reaches it, its catalyses with it (T015).
- **A pathway is never regulated in the graph:** graph-core's `Regulation` regulates reactions only. The release's converter would put a pathway's regulations in its step; there are none to put.
- **A catalyst activity regulated as an entity** (the converter's Modulation, "not supported in the new data model after release 63"): the graph has no such link, so none is written, as none is published.
- **The active unit's comment names an rdf:ID** (`activeUnit: #Protein12`), numbered as each file is written. `ci/compare.py` now reads it as a reference to the entity it names, so the comparison sees the entity, not the number.

The BioPAX validator 6.0.0 (`--profile=notstrict`), read by `ci/validate.py`, on the published files of release 97. A written file may have no ERROR its published counterpart lacks (SC-003); warnings are recorded, not held.

| Species                 | Errors | Warnings | Codes |
| ----------------------- | ------ | -------- | ----- |
| _Plasmodium falciparum_ | 0      | 16,863   | 19    |

Measured 27 Oct 2026. A run takes about four minutes by the shell's clock (249 s with the network off, 242 s with it on), most of it the validator's start-up. The other species' baselines are made in T020.
