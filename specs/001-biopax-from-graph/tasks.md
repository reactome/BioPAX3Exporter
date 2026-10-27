---
description: 'Tasks for spec 001: BioPAX Level 3 from the graph, as the release publishes it'
---

# Tasks: BioPAX Level 3 from the graph, as the release publishes it

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [contracts/checks.md](contracts/checks.md), [quickstart.md](quickstart.md)

**Every task**:

- Is one pull request to `main`, with its own adversarial review.
- Ticks this file with its PR number, in that same pull request.
- Shows each new check or fix failing first (constitution III).
- Prints no person's name, and puts none in the repository (constitution VI).
- Uses no production service: the published `biopax.zip` is fetched once and kept outside the repository; runs use a private graph copy (constitution V).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: The checks (User Story 1, P1)

**Goal**: tests, lint and validation that run and fail when they should.

- [ ] T001 [US1] Bind the tests: `pom.xml` gains the Kotlin `test-compile` execution, `src/test/kotlin` as the test source directory, and surefire's `**/*Test.class`. Repair the tests that then fail to compile (82 errors in five files, research R4) or remove those that test nothing the exporter keeps, saying which. Show a test made to fail failing `mvn test`.
- [ ] T002 [US1] Lint that checks: remove the `ktlint:format` execution; bind `ktlint:check` at `validate`, tests included; fix the violations it finds (five in two files today). Show a violation failing it and nothing rewritten.
- [ ] T003 [US1] CI: a job that runs `mvn verify` (tests and lint) on every pull request and fails on Maven's status; the Dockerfile's lint stage goes, since it formats before it looks. Show a failing test and a lint violation each failing the job (throwaway branches, deleted after).
- [ ] T004 [P] [US1] `ci/validate.py`: the BioPAX validator 6.0.0's XML reports read as counts per code, failing on any `error[@type='ERROR']` or `notFixedErrors` beyond a given baseline, printing codes and counts only (its log echoes display names). Its baseline on the published _P. falciparum_ file, 0 errors and 16,863 warnings in 19 codes, recorded in `differences.md` (the other species' in T020). Show it failing on a file made invalid.

## Phase 2: The instruments

- [ ] T005 [P] `ci/compare.py`: two BioPAX files reduced to multisets of (class, property, target) triples, each `rdf:ID` replaced by its content key (research R2; a Control by its regulation's id and the reaction it controls), authors' names and "Authored:/Reviewed:/Edited:" comments hashed; differences printed by kind and count. Show it sound on the published _P. falciparum_ file: one set's member re-pointed gives one triple on each side; one stoichiometric coefficient changed gives three. Its own unit tests in CI.
- [ ] T006 `ci/fixture/`: `extract.py` over a private copy of release 97's graph (sbml-exporter's pattern), extended to publications' authors, events' created, authored, reviewed and edited records, entity references with their cross-references, GO terms, diseases, PSI-MOD terms, `inferredFrom`, catalyst activities and regulation; at least eight events, each with its parent pathways (today's exporter writes pathways only): a pathway, a TemplateReaction with its regulation, a Degradation, a disease reaction, a reaction with nested sets, DNA and RNA, and one inferred event (research R3). People and curation comments replaced by placeholders, held by `check-placeholders.sh` in CI as an allowlist; shown failing on a value not on it.
- [ ] T007 CI runs the exporter over the fixture graph (a Neo4j service), validates its output (T004) and compares it with `ci/fixture/expected` (T005); `expected` is today's output, and each later task updates it with the triples it changes. Today's validator errors on it, if any, are its first baseline, which may only fall, and is zero by T020. CI also runs it twice and compares the bytes (FR-006), and checks the dependency lists for any relational driver (FR-001); each shown failing (a written value changed; an order from a hash set; a JDBC driver added).

## Phase 3: The code's shape (User Story 3's independence, constitution IV)

- [ ] T008 [US3] One `ConversionContext` per conversion (the model, the base, the id counters, caches of events and entities by dbId), passed to the builders; no companion or `object` state; the validator out of the library. Behaviour unchanged: the fixture's expected output does not move. A CI check converts eight events in parallel, 20 times, and compares each with its single run; shown failing with the shared counters put back.
- [ ] T009 [US3] Two Maven modules, `lib` (`biopax3-exporter-lib`: builders and context, graph-core and paxtools-core only) and `cli` (`biopax3-exporter`: launcher, options, logging, validator; shaded), with the library's dependency list checked in CI (no command-line or release dependency). The Dockerfile and the Jenkinsfile build the command line's jar.
- [ ] T010 graph-core moves to the version sbml-exporter pins (research R4), before Phase 4; the fixture's expected output does not move, or each change is explained. (Publishing the library later needs a released graph-core, not a snapshot pinned by commit: a ticket on graph-core, T022.)

## Phase 4: The content (User Story 2, P1)

**Goal**: each class written as release 97 writes it. Each task changes the fixture's expected output for its class only, and compares _P. falciparum_ from the private copy before and after, its differences falling.

- [ ] T011 [US2] Physical entities: their entity references (ProteinReference, SmallMoleculeReference, DnaReference, RnaReference) with cross-references; the "Reactome DB_ID" comment and the two UnificationXrefs (database id of the release, stable id with version); cellular location; complexes with `component` and `componentStoichiometry`; features (modifications, fragments).
- [ ] T012 [US2] Reactions: BiochemicalReaction, Degradation (`left` only) and TemplateReaction (`product` only) as the event's kind; `conversionDirection`; `participantStoichiometry`; `eCNumber`; their literature.
- [ ] T013 [US2] Catalysis with its controller and its two RelationshipXrefs and the `activeUnit:` comment; Control (ACTIVATION, INHIBITION) and TemplateReactionRegulation from regulation, one per controlled reaction, with the regulation's UnificationXrefs (database id, and stable id where the published file has one).
- [ ] T014 [US2] Events' own cross-references: RelationshipXrefs for GO biological process, disease and the rest, all of them where the old converter returned early on a repeat (an obvious bug fixed; recorded in `differences.md`).
- [ ] T015 [US2] Pathways: components, PathwayStep with `stepProcess` (reactions, catalyses, controls, pathways) and `nextStep`; a shared child written once and referenced by each parent; no header pathway; no event outside a pathway (research R1).
- [ ] T016 [US2] Literature and provenance: PublicationXref (PubMed, ISBN, URL) on the event; Evidence (IEA) for inferred events only; the single Provenance; curation comments, with HTML kept as the published files keep it.
- [ ] T017 [US2] Sets: typed by the class their members share, else PhysicalEntity; "Converted from EntitySet in Reactome" and `memberPhysicalEntity`. Which single-class sets are PhysicalEntity (a set among the members, or a member with no entity reference, or another reason: research R1, R6) decided against the graph over the whole of _P. falciparum_, and the rule that reproduces it written.
- [ ] T018 [US2] `xml:base` as `http://www.reactome.org/biopax/{release}/{dbId}#` (the species' or the event's), ids `{Class}{n}` in a deterministic order, the same on every run.

- [ ] T019 [US3] One event: `convert(event)` for a pathway, a reaction, a black box, a degradation and a template reaction (the event builder today asks for an abstract `Process` and is never called); `-l` either converts each listed event or goes, saying which.

## Phase 5: Proof (User Stories 2 and 3)

- [ ] T020 [US2] The full run: every species from a private copy of release 97's graph, the same 15 file names as the published zip, each compared by T005. `differences.md` records each remaining difference by kind, count and reason (order and ids; the data's, with its ticket, the EC numbers first, reactome/graph-importer#9; an obvious bug fixed), and nothing else. Validated by T004 against each published file's own baseline, made in this task for all 15 (human with enough memory for its size, measured). The private copy removed after.
- [ ] T021 [US3] One event through the library: the 30-event sample (pathways of several sizes, reactions, a black box, a disease event, an inferred one) each contained in its species file by meaning but for recorded differences.
- [ ] T022 Tickets: each issue found on the way, and each improvement, opened on the repository that owns it and linked from `differences.md` (spec FR-003).

## Phase 6: The release step (User Story 4, P2)

- [ ] T023 [US4] The Jenkinsfile's command runs as written (its stray quote, its jar, its options); it writes `biopax.zip` with one file per species; the release's own `xml:base` check passes on it.
- [ ] T024 [US4] The full run's time recorded against today's step's, which the release's owners provide; the switch's timing agreed with them.

## Dependencies

- Phase 1 before everything; T005 and T006 before T007; T007 before Phases 3 and 4 (each change is held by the fixture's expected output).
- Phase 3 before Phase 4, so the content is written once, into the final shape.
- Phase 4's tasks in any order after T008–T010; T015 after T011–T014 (steps name what they contain); T019 after T011–T013.
- Phase 5 after Phase 4; Phase 6 after T020.

## Parallel examples

- T004 and T005 (different tools).
- T011, T013 and T016 once Phase 3 is done (different builders); their fixture expectations conflict only where they touch the same individuals: merge one at a time.

## Implementation strategy

- **First**: Phases 1 and 2, the instruments, without which nothing after can be judged.
- **Then**: the shape, the content class by class, the full run, one event, and the release step, in that order.
