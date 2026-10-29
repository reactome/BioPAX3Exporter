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
- **What still differs at the root, for pathways:** the comment (660; HTML kept, T016) and the evidence (660, T016). Six display names also differ, by a trailing space the graph has and the published file does not. The release's writer trims every literal (no value in the published _P. falciparum_ file starts or ends with white space), so that is the writer's, T018.
- **The fixture:** its expected files had each pathway once per pathway above it, and a header: 217 event individuals for human, 33 for _P. falciparum_. They now have 57 (28 pathways, 29 reactions) and 8, with the same reactions. The validator gives 0 errors on both (558 and 32 warnings).

## Literature, evidence and curation (T016)

Measured 29 Oct 2026 on _P. falciparum_, from a private copy of release 98's graph, against release 98's published file; and on the fixture's human events against release 98's human file.

- **What the release writes**, read from its converter (`handleEvent`, `handleInferredFrom`, `ReactomeToBioPAXPublicationConverter`):
  - each summation's text as a comment, its HTML kept;
  - the event's literature and its summations' as PublicationXrefs, each publication once per file;
  - for an event with an evidence type, an Evidence for each event it was inferred from, new each time, with that event's first summation as its comment, its literature and that summation's, and the evidence type as an EvidenceCodeVocabulary ("inferred from electronic annotation", ECO:0000203);
  - an "Authored: ", "Reviewed: " or "Edited: " comment for each of its curation records (not its revisions);
  - on a regulation's Control, the regulation's literature and its summations' (`createModulationFromRegulation`), but not their text.
- **Regulations:** written as the converter wrote them, though none of release 98's 7,996 regulations has literature, directly or through a summation, and its files have none on a Control. Catalyses carry none of these.
- **Every event agrees in shape:** each of _P. falciparum_'s 1,630 events has the published file's PubMed and ISBN ids, curation comments by kind, summation comments and Evidence count. Its 5,750 PublicationXrefs have the published file's properties: 5,689 by PubMed id, 42 by ISBN, 11 web pages and 8 with no id. The fixture's 57 human events agree in the same four respects with release 98's human file, where 15,712 of 16,159 reactions have literature of their own and none is inferred. There is one Provenance, as in the published files.
- **What still differs at the root, here:**
  - _Evidence comments (128):_ white space at either end, which the release's writer trims (T018). One of them also differs within, which trimming does not explain: T018 looks at it with the writer.
  - _Authors (60 values, in 4 of 5,750 PublicationXrefs):_ the release's form of a person's name is fuller than the graph's display name: the release wrote the relational `_displayName`, and the graph importer rebuilds a Person's display name from its surname and initials ("Authors' names", below). The 23 PublicationXrefs first recorded here (T016, #69, before T017 and T018) are not what `ci/compare.py` reports now: 4, each with its own db, id, year, title and source.
- **Repeated values:** the published file writes an author value twice in some lists (two people of one name, its converter's comment says); Paxtools keeps each value once, which `ci/compare.py` counts once too.
- **Counts:** triples only in the published file fall from 146,281 to 72,683, and only in the converted file from 9,842 to 7,959. The validator gives 0 errors on the fixture's output, with 501 warnings for human and 28 for _P. falciparum_ (558 and 32 at T015).
- **The empty Evidence** every event had is gone, with `BioPAX3ReferenceUtils`, which stripped the HTML.

## Sets (T017)

Measured 29 Oct 2026 on _P. falciparum_, from a private copy of release 98's graph, against release 98's published file; and on the fixture's sets against release 98's human file.

- **The release's rule**, read from its converter (`getReactomeEntityType`, `InstanceUtilities.getContainedInstances`): a set is written as the one kind every entity it holds shares, else as a PhysicalEntity. A complex is a Complex; an entity with a reference entity is its reference's schema class, an isoform its gene product's; an entity that cannot have one (a set, a polymer, a genome-encoded entity) has no kind; one whose reference is missing counts for nothing. The converter looked at every depth, but a set among the members already gives no kind, so the members alone decide.
- **Why the single-class sets are PhysicalEntity** (research R1): a set among the members, as R1 found; or chemical drugs. A drug's reference is a ReferenceTherapeutic, whose kind is written as PhysicalEntity, though each drug is a SmallMolecule. Of release 98's 34 _P. falciparum_ sets of small molecules written as PhysicalEntity, 27 hold only chemical drugs, 6 drugs and simple entities (a ReferenceMolecule), and 1 a set. The 33 with no set among them number what R1 found in release 97; they were not matched set by set.
- **What a set has:** the comment "Converted from EntitySet in Reactome" and its members, then its candidates, as `memberPhysicalEntity`, each written after the set is made, as the converter did. A set has no entity reference of its own, and a set of complexes no components.
- **Every set agrees:** all 846 of _P. falciparum_'s sets, and the 26 of the fixture's human sets, have the published file's class, members (by database id) and comment. The sets' classes against their members' are the published file's, kind for kind.
- **A set Paxtools would refuse:** Paxtools allows a Protein (or any class) only members of its own class. A member with no reference counts for nothing in the kind but is written as a PhysicalEntity, so the release's rule could make a Protein with a PhysicalEntity member, which the release, writing XML itself, would have written invalid. Such a set is a PhysicalEntity here. None of release 98's _P. falciparum_ sets is one, and no set in its published _P. falciparum_ file has a member of another class (human is measured in T020).
- **What still differs at the root:** apart from the known writer differences (`conversionDirection`, trailing spaces, trimmed Evidence comments; T018) and the authors' names (T016), only cross-references the published file lacks: on 215 small molecules, DOID (518), Mondo (206), SYMP (11) and 4 others. Every entity's published cross-references are among the converted ones. Mondo is the graph's later data (T011); the rest are the diseases and cross-references the release's converter skipped after the first one it had already written (T011, T014), on drugs the sets now reach.
- **Counts:** triples only in the published file fall from 72,683 to 2,383, and only in the converted file from 7,959 to 3,604. The validator gives 0 errors on the fixture's output, with 787 warnings for human (501 before: its sets' members are now written) and 28 for _P. falciparum_.

## The writer (T018)

Measured 29 Oct 2026 on _P. falciparum_, from a private copy of release 98's graph, against release 98's published file.

- **The base** is the release's: `http://www.reactome.org/biopax/{release}/{dbId}#`, the species' for a species' file (`…/98/170928#`, as published) and the event's for one event's; without a release, `http://www.reactome.org/biopax/{dbId}#`, as the converter wrote it. It was `https://…/species/{dbId}#`.
- **Literals are trimmed** at both ends, as the release's XML writer trimmed every text it wrote, with Java's `String.trim()`: the characters up to a space, and no other. No value in the published _P. falciparum_ file starts or ends with one, and now none in the converted one does; a no-break space or a paragraph separator stays, as the release kept it (3 in the human file). A carriage return inside a value is written `&#xD;`, as the published files have it (1 value in _P. falciparum_; 24 values, with 44 between them, in human); written as itself, an XML parser reads it as a line feed.
- **Ids** are `{Class}{n}`, numbered in the order the converter made its individuals, and the same on every run (CI converts each species twice and compares the bytes). The release's own numbering cannot be matched: its relational adaptor gave a species' events as a hash set (`MySQLAdaptor.fetchInstance`), and `GKInstance` has no `hashCode` of its own, so the order of its top-level events, which `grepTopLevelEvents` kept, and every id after them, could change from one run of the release to the next. Of the published _P. falciparum_ file's top-level pathways, Pathway1 is neither the first nor the last by database id.
- **A failure to write** is an error that ends the run; it was a printed message, and the run went on with the file missing or half written. A file is written beside its name and then renamed into place in one step (#74: an atomic move, where a move that replaces deletes the old file first), so a failure leaves no part of a file and the file there before stays until the new one is whole.
- **What remains at the root** is what earlier sections recorded: `conversionDirection` as Paxtools writes it (T012's decision), names equal to a display name (T011), the authors' names (T016), and the cross-references the release skipped or did not yet have (T011, T017). Triples only in the published file fall from 2,383 to 1,128, and only in the converted file from 3,604 to 2,347.
- **The fixture's expected files** change only by trimming: 14 protein names and 1 reaction's display name. The validator gives 0 errors, with 787 and 28 warnings.

## One event (T019)

Measured 29 Oct 2026 on the fixture graph.

- **Any event converts alone:** a pathway with all it holds and its steps, or a reaction of any kind (a BiochemicalReaction, a Degradation, a TemplateReaction, a black box as its kind is), each with the event's base, `http://www.reactome.org/biopax/98/{dbId}#`, as the release's converter wrote one event. `-t` and `-m` take any event, not only pathways, and write `{dbId}.owl`.
- **Contained in its species' file:** the fixture's pathway, reaction, black box, Degradation, TemplateReaction, disease reaction and DNA/RNA reaction, each converted alone, have nothing their species' file lacks (`ci/compare.py`, nothing only in the event's file); this holds for an event its species' file holds, which a pathway of the species does. CI holds this for the Degradation, and that its file has one; T021 measures a sample of 30.
- **`-l`** writes the events listed as one model, `events.owl`, each with all it holds; its base names neither a species nor an event, `http://www.reactome.org/biopax#`, as the converter wrote one, and its xrefs name the release. It built an empty pathway before.
- **An id that names no event** (or no species, for `-s`), an event of a kind the release did not convert (an InteractionEvent), or more than one mode given, is an error: the run says which and exits 1. A list of events (`-l`) with one writes nothing; a list of events (`-m`) or of species (`-s`) writes the others, then fails. A species named with `-s` that has no events is an error too (T023). An error reading an event's id is reported as itself; one reading a species' id ends the run with its stack trace. Before, such an id was looked up as a pathway and, when it was not one, nothing was written and the run succeeded. Every mode makes the output directory.
- **Not written, by choice:** the release's one-event files had a comment on the ontology header, `BioPAX pathway converted from "…" in the Reactome database.` A Paxtools model has no place for it, and the comparison reads individuals only; it could be added to the written text, but it says nothing a reader of the file needs, so it is recorded here and not written.
- **Removed:** `BioPAXEventBuilder`, which asked for an abstract `Process` and was never called.

## One event through the library (T021)

Measured 29 Oct 2026 on a private copy of release 98's graph: 30 events, each converted alone (`-m`, `WriteBioPAX3` one event at a time), each compared with its species' file from the full run (T020) by `ci/contained.py`, which reads the species' file once as `ci/compare.py` reduces it to triples.

- **The sample**, the first by database id of each kind: ten human pathways of five sizes (up to 3 events, 20, 100, 500, and more); nine human reactions (three reactions, three black boxes, a polymerisation, a depolymerisation, a cell development step); six human disease events (a black box, a pathway, two reactions, a failed reaction, a top-level pathway); five inferred mouse events (a reaction, a black box, a polymerisation, a pathway, a top-level pathway). By database id: human 68689, 68881, 68616, 68884, 68867, 68875, 15869, 68886, 73857, 74160; 68595, 68603, 68610, 68712, 68825, 68948, 450358, 5229194, 9725621; 112333, 162585, 162590, 162914, 1225956, 1643685; mouse 9633481, 10003583, 10037081, 10117288, 10117294.
- **Each is contained in its species' file, and whole:** of the 30 files, from 123 triples to 657,560, not one triple is missing from the human or the mouse file; and each individual a file holds with a database id has every triple the species' file gives it, so none of the events, reactions or entities was converted short. A step's `nextStep` is not checked so (a step is keyed by its content); a missing step would be, through the pathway's `pathwayOrder`. No recorded difference was needed to hold this.
- **Inferred events** carry their Evidence (1 to 74 in the four with an evidence type); one inferred reaction has no evidence type, and so none, as the release's rule gives.
- **Time:** 7 min 14 s for the 30, at most 1.3 GB resident.

## The full run (T020)

Measured 29 Oct 2026: every species converted from a private copy of release 98's graph by the exporter's default mode, and each file compared with release 98's published one by `ci/compare.py` and validated by the BioPAX validator 6.0.0 (`--profile=notstrict`).

- **The run:** 1 h 55 min for all species, at most 2.9 GB resident with a 4 GB heap. Human alone takes about 16 minutes. Two faults the first runs found are fixed (#76, #77): a lookup that read the whole model for every individual, and graph objects and copies of the file kept in memory.
- **The files:** the 15 of the published zip, by the same names, and a sixteenth, _Mycobacterium tuberculosis_: in the graph it has a top-level pathway of its own and 56 events beneath it, all of that species alone, which the converter's rule (`SpeciesAllPathwaysConverter`) would take. The release step names the species it converts from its species configuration (`FileGeneration.getSpeciesNames`); the published zip has no file for it. Which species the release command writes is T023's.
- **By species:**

| Species                     | Only in the published file | Only in the converted file | Published: errors, warnings | Converted: errors, warnings |
| --------------------------- | -------------------------: | -------------------------: | --------------------------: | --------------------------: |
| _Plasmodium falciparum_     |                      1,128 |                      2,347 |                   0, 16,892 |                   0, 16,892 |
| _Schizosaccharomyces pombe_ |                      1,724 |                      2,677 |                   0, 24,918 |                   0, 24,918 |
| _Saccharomyces cerevisiae_  |                      2,064 |                      2,467 |                   0, 26,546 |                   0, 26,546 |
| _Dictyostelium discoideum_  |                      2,741 |                      3,939 |                   0, 46,593 |                   0, 46,593 |
| _Caenorhabditis elegans_    |                      4,214 |                      5,974 |                   0, 73,368 |                   0, 73,368 |
| _Drosophila melanogaster_   |                      5,282 |                      7,269 |                   0, 80,159 |                   0, 80,159 |
| _Xenopus tropicalis_        |                      5,928 |                      8,334 |                   0, 80,442 |                   0, 80,442 |
| _Danio rerio_               |                      6,412 |                      9,002 |                   0, 92,910 |                   0, 92,910 |
| _Gallus gallus_             |                     13,558 |                     10,274 |                  3, 103,988 |                  3, 103,988 |
| _Canis familiaris_          |                     15,778 |                     10,991 |                  0, 111,014 |                  0, 111,014 |
| _Sus scrofa_                |                     15,784 |                     11,218 |                  0, 115,163 |                  0, 115,163 |
| _Bos taurus_                |                     15,256 |                     11,660 |                  0, 120,651 |                  0, 120,651 |
| _Rattus norvegicus_         |                      9,204 |                     11,987 |                  0, 124,073 |                  0, 124,073 |
| _Mus musculus_              |                     10,948 |                     13,361 |                  0, 143,890 |                  0, 143,890 |
| _Homo sapiens_              |                     16,607 |                     37,197 |                207, 213,784 |                207, 213,785 |

- **What differs at the root, all species together**, and nothing else:
  - `conversionDirection`, written as Paxtools writes it (95,355; T012's decision).
  - A name equal to the display name (28,267, nearly all on proteins of inferred species, whose graph name lists repeat a name): the published files list it among the names too, Paxtools writes it only as the display name (T011). In _Gallus gallus_ all 5,921 are this.
  - Cross-references the published files lack (entities 36,717, events 379; their RelationshipXrefs 4,427), nearly all of two causes: the graph's later data (Mondo, COSMIC and ClinGen, added after the relational database the release read: T011), and those the release's converter skipped after the first one it had already written (DOID, SYMP, ChEBI, COMPOUND, EMBL: T011, T014). In human, 269 on reactions and 110 on pathways, T014's 379. A few more of other databases, whose cause was not traced one by one: PubChem Substance (24, in 14 species), and in human NCBI Nucleotide (3), RNAcentral (2), ENSEMBL, NCBI Gene and EC (1 each). Every published cross-reference is among the converted ones.
  - The form of an author's name (872 values, all of one kind: "Authors' names", below).
  - An empty value the release wrote as an empty element, which Paxtools does not write: a ProteinReference's comment (38), a DNA or RNA reference's name (3, in human).
  - No `eCNumber` difference: the one reactome/graph-importer#9 records is not in release 98 (T012).
- **The validator:** no file has an error its published one lacks (SC-003). The errors are the same, `multi.organism.pathway` (3 in _Gallus gallus_, 207 in human), and so are the warnings, rule by rule, but one more `unknown.db` in human.

## Authors' names

Measured 29 Oct 2026 on all 15 species, from a private copy of release 98's graph, against release 98's published files: each PublicationXref matched to its graph publication (by PubMed id, else ISBN, else title) and its authors compared in order with that publication's, Person by Person. Names were read only to classify them; none is recorded here.

- **One kind, 872 values:** in every one, the published value is the Person's surname and first name, and the graph's display name its surname and initials, both of the same Person, whose own `firstname` and `initial` hold each form. No other kind: no accent, order, missing initial or different person.
- **Few people:** 62 Persons, authors of 10 publications. The values repeat because those publications are in many species' files: 60 in _P. falciparum_, 67 in human, 20 in _Danio rerio_, 53 in _Caenorhabditis elegans_, 60 or 63 in each of the rest. The other authors of those publications agree. Most of the 10 were created in release 98's curation.
- **Why they differ:** the two writers take a Person's name from different rules.
  - The release's converter wrote the relational `_displayName`. The curator tool's rule for it (`InstanceDisplayNameGenerator.generatePersonName`) puts the first name first, and the initials only when there is none.
  - The graph importer does not copy a Person's `_displayName`, as it does every other class's: `ReactomeBatchImporter` rebuilds it as `surname, initial`, the first name only when there is no initial, "to fix different styles in Person display name". It has done so since its first commit (2016), so whichever commit built release 98's graph.
  - So every Person in the graph has the initials form (all but one, a script's, whose display name is its own), whatever the relational database holds; it says nothing of the data. The published files do: there, every other author has the initials form too, so in release 98's relational database these 62 alone held the curator tool's first-name form.
- **Which form is meant:** the importer normalises on purpose, and the exporter writes the graph's display name, as it should; neither is a fault here. Whether a Person's display name is meant to be the importer's `surname, initial` or the curator tool's `surname, firstname` is a question for those two tools' owners (T022). Until it is settled, the files written from the graph give these authors their initials.

## Paxtools 6, Kotlin 2.4, JUnit 6 (#66)

Measured 29 Oct 2026. The fixture's output is unchanged to the byte, and _P. falciparum_ from a private copy of release 98's graph is unchanged by `ci/compare.py`. Its bytes differ in one way only: Paxtools 4.3.1 wrote a character outside ASCII as a character reference (`&#8722;`, 8 in the file), and Paxtools 6.0.0 writes it as itself, as release 98's published file does (937 lines with such characters, no references). The two behaviours recorded above as Paxtools 4.3.1's, the underscored `conversionDirection` and a product added to the participants, are 6.0.0's too. `Control.controlled` is deprecated (in Paxtools 5.1's javadoc, and by annotation in 6.0) with no replacement yet; each control here has one process, as the release writes it. Paxtools 6 brings slf4j-api 2, which the library leaves out, as it does graph-core's logging binding: graph-core brings slf4j-api 1.7, and the command line's logback 1.2 binds to that.

## The validator's baseline

The BioPAX validator 6.0.0 (`--profile=notstrict`), read by `ci/validate.py`, on the published files of release 97. A written file may have no ERROR its published counterpart lacks (SC-003); warnings are recorded, not held.

| Species                 | Errors | Warnings | Codes |
| ----------------------- | ------ | -------- | ----- |
| _Plasmodium falciparum_ | 0      | 16,863   | 19    |

Measured 27 Oct 2026. A run takes about four minutes by the shell's clock (249 s with the network off, 242 s with it on), most of it the validator's start-up. Release 98's baselines for all 15, and the converted files', are in the full run's table above (T020).
