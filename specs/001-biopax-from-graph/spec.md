# Feature Specification: BioPAX Level 3 from the graph, as the release publishes it

**Feature Branch**: `spec/001-biopax-from-graph`

**Created**: 2026-10-27

**Status**: Draft

**Input**: Decided with the repository's owners on 27 Oct 2026: "biopax should be the same concept of sbml where we try to match current biopax.zip and make only obvious bug fixes. and then we make tickets on the relevant repos for issues that we find and improvements as well." The work goes to `main` by pull request, with no long-lived branch: by the owners' account nothing uses this exporter yet. (Each merge to `main` still republishes its container image, as `:latest`.) Its website half is reactome/WebsiteAngular spec 015 (User Story 2, tasks T045–T054), which also holds the research it starts from (R4, R9, data-model.md).

## Background

Reactome publishes BioPAX Level 3 twice:

- **the release's `biopax.zip`:** one `.owl` file per species, made by Pathway-Exchange's converter from the relational database;
- **the website's per-event downloads:** one pathway or reaction, made by another of Pathway-Exchange's converters (`ReactomeToBioPAX3XMLConverter`) behind a legacy service; the species files come from `SpeciesAllPathwaysLevel3Converter`.

Both converters read the relational database, which the release is retiring in favour of the graph. This exporter reads the graph and was written to replace them, but it was never compared with what they publish. Measured on 24 Oct 2026 (WebsiteAngular spec 015, R9), it writes:

- pathways with their name, summation and organism, and an empty evidence on each pathway and reaction;
- every reaction-like event as a BiochemicalReaction (never a Degradation or a TemplateReaction), with its left and right sides but no stoichiometry;
- physical entities by name only: no entity references, cross-references, components or locations;
- catalyses without their controller;
- no regulation, no literature, and no stable-identifier cross-references.

Release 97's published `biopax.zip` (174 MB, 15 species files) has all of these. For example, _Plasmodium falciparum_'s file has 658 pathways, 966 reactions, 993 protein references, 17,026 unification cross-references, 5,648 publication cross-references, 2,960 stoichiometries and 689 catalyses.

The exporter also has defects of its own:

- species mode writes every pathway of a species on its own, sub-pathways included, so a sub-pathway (with its reactions and entities) is written again under every pathway that contains it, beside a header pathway named for the species;
- the events in no pathway are found but never written;
- `-l` writes an empty pathway;
- its `xml:base` is not the form the release checks for (`http://www.reactome.org/biopax/{release}/…`);
- its base URI, its database version and its per-class id counters are shared across conversions, and each conversion resets the counters, so two conversions at once interfere;
- a single reaction cannot be exported;
- its release step's command has a stray quote;
- its tests run nothing (`mvn test` compiles no tests), its lint never runs in CI, and its validator call is unused.

## User Scenarios & Testing _(mandatory)_

The "users" are the release, which publishes `biopax.zip`; the website, which serves one event's BioPAX; and the people who download either and load them into pathway tools.

### User Story 1 - The checks are real (Priority: P1)

A change to this repository is tested before it merges: its unit tests compile and run, its lint checks rather than rewrites, and its output is validated as BioPAX. Each check is shown failing on a deliberate mistake before it is trusted.

**Why this priority**: every other story is measured by these checks. Today none of them runs.

**Independent Test**: break a unit test, a lint rule and a written element in turn: each check fails in continuous integration, naming it.

**Acceptance Scenarios**:

1. **Given** a unit test that fails, **When** the build runs in CI, **Then** it fails.
2. **Given** a lint violation, **When** CI runs, **Then** it fails, and nothing is rewritten.
3. **Given** a file the BioPAX validator reports an error in, **When** CI validates the output of a small fixture graph, **Then** it fails, reading the validator's report since the validator's exit status says nothing.

---

### User Story 2 - A species file matches the release's, by meaning (Priority: P1)

The release runs the exporter on the release's graph and gets one file per species. Each, reduced to (class, property, target) triples with each `rdf:ID` replaced by its content, matches the same species' file in the release's published `biopax.zip`. Every difference is either fixed or recorded with its reason.

**Why this priority**: it is what the release publishes, and the comparison is what proves the exporter writes what today's converter writes.

**Independent Test**: export three species from a private copy of release 97's graph and compare each with release 97's published file; the comparison reports only recorded differences.

**Acceptance Scenarios**:

1. **Given** a private copy of release 97's graph, **When** the exporter writes every species, **Then** it writes the same set of files, by name, as release 97's `biopax.zip`.
2. **Given** a species file, **When** it is compared with the published one by meaning, **Then** every difference is in the recorded list, with its count and reason, and nothing else differs.
3. **Given** the published files, **When** a difference comes from the graph rather than the exporter (multiple EC numbers per molecular function, which the graph keeps one of, say), **Then** it is recorded as the data's, and a ticket is opened on the repository that owns it.
4. **Given** each written file, **When** the BioPAX validator checks it, **Then** it reports no error that the published file does not also have.

---

### User Story 3 - One event, for the website (Priority: P1)

The website asks for one pathway's or one reaction's BioPAX. A library module, with no command-line or release dependencies, writes it from the graph: the event, everything it contains, and what that refers to. Two conversions at once do not interfere.

**Why this priority**: the website's per-event download reads the relational database through a legacy service today. It is half of what this exporter is for (constitution IV).

**Independent Test**: convert a reaction, a pathway, a black-box event and a disease reaction through the library, and compare each with what the release's species file holds for that event.

**Acceptance Scenarios**:

1. **Given** a reaction's or a pathway's stable id, **When** the library converts it, **Then** it writes that event and what it contains, with `xml:base="http://www.reactome.org/biopax/{release}/{event dbId}#"`, as today's per-event files do.
2. **Given** the event's triples, **When** they are compared with the same species' file by meaning, **Then** they are contained in it, but for the recorded differences.
3. **Given** two conversions in parallel, **When** both finish, **Then** each file equals the one written alone.
4. **Given** the library's dependencies, **When** they are listed, **Then** they include the graph and the BioPAX model, and nothing of the command line or the release.

---

### User Story 4 - The release step runs (Priority: P2)

The release builds `biopax.zip` with this exporter in place of Pathway-Exchange's converter: the command line writes every species, the files carry the `xml:base` the release's check of the download directory requires, and the step's command runs as written.

**Why this priority**: it is the switch, which comes after the proof (User Stories 1–3) and needs the release's owners to agree on its timing.

**Independent Test**: run the release step's command against the private copy: it writes the zip, and the release's own check of the file passes.

**Acceptance Scenarios**:

1. **Given** the release step as written in the repository, **When** it runs, **Then** it writes `biopax.zip` with one file per species.
2. **Given** each file, **When** the release's check reads its `xml:base`, **Then** it is `http://www.reactome.org/biopax/{release}/{species dbId}#`, as release 97's are.
3. **Given** a full run, **When** it is timed, **Then** its time is recorded against today's step's, which the release's owners provide.

### Edge Cases

- **Order and ids:** the old converter's order and `rdf:ID`s cannot be reproduced, even against itself, so the exporter writes the same order on every run and the comparison replaces each id by its content.
- **Sets:** a set is written as the class its members share, else as a PhysicalEntity, with its members; but some sets of a single class are PhysicalEntity too, by a rule the graph decides (research R1, R6).
- **The old converter's early return** on a repeated cross-reference or disease: an obvious bug. The exporter writes all of them, and the difference is recorded.
- **`Book.chapterAuthors`:** not in the graph; recorded as the data's.
- **People's names** in literature and curation comments: in the output, but never in a fixture, a test log or a comparison's printout (constitution VI).
- **An event in no pathway:** written alone, with no step, when it is one of the species' top-level events, as the release's converter writes it; release 98's species files have none (research R1, T015).
- **A species with no pathways:** whether a file is written for it is whatever release 97 published.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: The exporter MUST read only the graph, through graph-core, and nothing relational.
- **FR-002**: Each species file MUST match the release's published file of the same release by meaning. A difference may stay only for one of three reasons, recorded with its count: the order and ids (replaced by content in the comparison); the data, where the graph holds something other than the relational database (with a ticket on the repository that owns it); or an obvious bug of the old converter, fixed here. Any other difference fails.
- **FR-003**: Changes to what is written MUST be limited to matching the published files and fixing obvious bugs. Every other issue or improvement found MUST be opened as a ticket on the repository that owns it, and the written files left matching.
- **FR-004**: The library MUST convert one pathway or one reaction, with its contents and references, and be safe to call from several threads at once: nothing a conversion writes is shared with another.
- **FR-005**: The library MUST NOT depend on the command line or the release's tooling; the command line MUST be built on the library.
- **FR-006**: The output MUST be in a deterministic order.
- **FR-007**: `xml:base` MUST be `http://www.reactome.org/biopax/{release}/{dbId}#`, the species' dbId for a species file and the event's for one event.
- **FR-008**: The unit tests MUST compile and run in CI, the lint MUST check without rewriting, and the output of a fixture graph MUST be validated in CI, failing on a validator error. Each check MUST be shown failing first.
- **FR-009**: Fixtures, test output and comparison reports MUST NOT contain people's names.
- **FR-010**: No production Reactome service may be used. The release's published files are fetched one at a time and kept out of the repository; runs use a private copy of the release's graph.
- **FR-011**: The release step's command MUST run as written in the repository.

### Key Entities

- **Species file:** one `.owl` file per species, the release's unit.
- **Event file:** one pathway or reaction with what it contains, the website's unit.
- **Triple:** a (class, property, target) fact of a file, with each `rdf:ID` replaced by its content; the unit of comparison.
- **Recorded difference:** a difference kept, with its count and reason, and its ticket where it is the data's or an improvement.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: For all 15 species of release 97, the exporter writes the same file names as the published `biopax.zip`.
- **SC-002**: Every species file's differences from the published one, by meaning, are of the three kinds FR-002 allows, each recorded with its count; none is of any other kind.
- **SC-003**: No written file has a validator error its published counterpart lacks.
- **SC-004**: A sample of 30 events (pathways of several sizes, reactions, black-box, disease, inferred), each converted through the library, is contained in its species file by meaning, but for recorded differences.
- **SC-005**: Over 20 runs of eight events converted in parallel, every file equals the one written alone.
- **SC-006**: For each check (unit tests, lint, validation of the fixture's output), a deliberate mistake fails CI when tried, and each check's failing run is linked from the pull request that adds it.
- **SC-007**: The full run's time is recorded against today's step's.

## Assumptions

- The reference is release 97's published `biopax.zip`, fetched once from the release's download directory and kept outside the repository; a later release's file is compared in the same way when one is run.
- A private copy of release 97's graph is available for runs, as for sbml-exporter's dry runs, and removed after.
- The website's half (serving the library through its Java library service, the routes and the site's links) is WebsiteAngular spec 015's, not this one's.
- Publishing the library as a versioned artefact is the aim, once it is proven; how the website takes it until then is the website's spec's.
- The release's owners agree the switch's timing (User Story 4).
