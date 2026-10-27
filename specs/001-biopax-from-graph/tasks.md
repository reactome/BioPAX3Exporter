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

- [x] T001 [US1] Bind the tests: `pom.xml` gains the Kotlin `test-compile` execution, `src/test/kotlin` as the test source directory, and surefire's `**/*Test.class`. Repair the tests that then fail to compile (82 errors in five files, research R4) or remove those that test nothing the exporter keeps, saying which. Show a test made to fail failing `mvn test`. — #11, replacing #10 after its review. Bound: the tests compiled with 82 errors, as measured. Repaired: two `Pathway`s told apart by name (the graph's and BioPAX's), a mock that verified a property an `Entity` does not have, a line that was not Kotlin, Paxtools elements made by its factory rather than by hand, Mockito's class arguments as matchers, and the shared base, version and counters reset before each test (T008 removes them). Removed, with their reasons: the species builder's two null-argument tests, which its constructor's types rule out, and its third, which describes the header pathway the published files lack (R1), leaving its file empty; four that expect HTML stripped from comments, which the published files keep (T016); `getID`'s format test, which asserts a base no file uses and fails (the kept `WriteBioPAX3Test` assertions of today's bases pass, and carry a note that T018 changes them); and six mocked flows (an existing Evidence reused, where the published files write one per inferred event; reaction reuse through a cache that does not exist, whole pathways with their children, and a null model that throws on `thisModel!!`, a latent fault T008's context removes), which T008, T015 and the fixture (T007) replace. Restored from #10's review: one Provenance per file, which the published files keep, repaired (its mock had no id) and shown failing when the Provenance is not found. 28 tests run and pass; one made to fail fails `mvn test`.
- [x] T002 [US1] Lint that checks: remove the `ktlint:format` execution; bind `ktlint:check` at `validate`, tests included; fix the violations it finds (five in two files today). Show a violation failing it and nothing rewritten. — #12. The `format` execution removed and `check` bound at `validate`; tests are linted now that they are sources (T001). The violations it then found were fixed: ktlint's formatter run once, by hand, for whitespace (in the main sources, two blank lines and a trailing space only), and wildcard imports in the tests written out by hand. A trailing space put into a source failed `mvn validate` and left the file as it was.
- [x] T003 [US1] CI: a job that runs `mvn verify` (tests and lint) on every pull request and fails on Maven's status; the Dockerfile's lint stage goes, since it formats before it looks. Show a failing test and a lint violation each failing the job (throwaway branches, deleted after). — #13. A `verify` job (`mvn -B verify` on JDK 17, Maven cached) on every pull request, push to `main` and dispatch; the image is published only after it passes. The Dockerfile's lint stage, which could only ever pass, is gone. Shown on two throwaway branches, dispatched and then deleted: a failing test failed `verify` (run 40440949125), a trailing space failed it on ktlint (run 40440952876); the image build failed with each, since it runs `mvn clean package`.
- [x] T004 [P] [US1] `ci/validate.py`: the BioPAX validator 6.0.0's XML reports read as counts per code, failing on any `error[@type='ERROR']` or `notFixedErrors` beyond a given baseline, printing codes and counts only (its log echoes display names). Its baseline on the published _P. falciparum_ file, 0 errors and 16,863 warnings in 19 codes, recorded in `differences.md` (the other species' in T020). Show it failing on a file made invalid. — #15, replacing #14 after its review. On the published _P. falciparum_ file: 0 errors, 16,863 warnings in 19 codes, as research measured, recorded in `differences.md`; but a run takes about four minutes by the shell's clock (249 s with the container's network off, 242 s with it on), not research's 29 s. The validator needs a writable working directory, where it writes its report. Shown failing: one reference in that file pointed at an individual that does not exist; the validator exited 0 and reported `syntax.error`, and `validate.py` exited 1 naming it. Its own unit tests (`ci/test_validate.py`, nine) run in CI's `verify` job. From #14's review: a report with no validation is refused, an ERROR is counted by the larger of its total and not-fixed cases, a code repeated across validations adds up, and bad arguments get the usage; each shown failing on #14's script.

## Phase 2: The instruments

- [x] T005 [P] `ci/compare.py`: two BioPAX files reduced to multisets of (class, property, target) triples, each `rdf:ID` replaced by its content key (research R2; a Control by its regulation's id and the reaction it controls), authors' names and "Authored:/Reviewed:/Edited:" comments hashed; differences printed by kind and count. Show it sound on the published _P. falciparum_ file: one set's member re-pointed gives one triple on each side; one stoichiometric coefficient changed gives three. Its own unit tests in CI. — #20, replacing #16 to #19 after their reviews. It uses Python's own XML reader, not research's lxml, so CI installs nothing.

  - **Keys.** Events and entities have keys of their own: their database-id UnificationXrefs, all of them, sorted. Anything with `controlled` (Control, Catalysis, Modulation, TemplateReactionRegulation) is keyed also by what it controls. Everything else, xrefs and vocabularies included, is keyed by a hash of its whole content: in #16, two chapters of one book sharing an ISBN compared as one.
  - **Values.** Within one individual a value counts once, since Paxtools keeps sets (release 97 repeats 13 in _P. falciparum_ and 2,421 in human, recorded in `differences.md`). A literal keeps its datatype. A number is compared by its value as that type holds it: an `xsd:float` as 32 bits, an `xsd:decimal` exactly at any length, -0 as 0 (from #19's review: `normalize()` rounded at 28 digits). It is never equal to text, however spelled: a malformed `inf` is not `INF`, and only ASCII digits make a number (Python's readers take any script's). An integer is read up to 40 digits, leading zeros aside. Text is compared exactly, and literals are kept apart from references.
  - **Root and carried.** One change carries to every referrer of a content key. In _P. falciparum_, changing the one evidence code's xref gives 12,798 triples a side. So the report compares the files a second time, with each content key found in one file only replaced by its class; database-id keys, and content keys both files have, stay exact.
    - What still differs is the root: here the xref's id, 1 a side, against 25,594 not at the root (both sides together). The root triples are listed first among the examples.
    - **Hidden in #18:** the first rule, by the keys a triple pointed at, hid real changes: an Evidence's own comment changed beside the code, an event added or removed, a reaction re-pointed to a new entity.
    - **Hidden in #19:** coarsening every content key hid a step's `nextStep` re-pointed to another existing step; it now shows at the root, 1 a side. It also coarsened a database-id key that names a content-keyed reaction it controls; such a key now stays exact.
    - **Still no root:** a swap among content keys that are new in the other file (two coefficients traded between two complexes, say). The files differ and the exit says so, and the report says that nothing differs at the root.
    - **Noisier when old content survives:** where the old content is still found elsewhere, the referrer shows at the root too. A coefficient changed in _P. falciparum_ gives 3 a side at the root, not 1, because other stoichiometries keep the old key.
  - **People's names.** Free text (comments, titles, sources, names and URLs, which cite authors and editors) is printed only as a length and a salted hash; codes, ids and numbers are printed as they are, by an allowlist the tests hold. An author, and a comment beginning "Authored:", "Reviewed:" or "Edited:" (or "Created:", "Revised:", "Modified:" or "Curated:"), in any case, is hashed with a salt for the run. The published files hold 70,522 such comments, in exactly those three spellings, none of them in _P. falciparum_.
  - **Refusals.** A form it does not read exits 2, as a missing file, a value Python cannot read or a chain too deep to key does: a nested individual, `rdf:nodeID`, or an attribute other than `rdf:resource` and `rdf:datatype`. Names resolve against `xml:base`.
  - **On _P. falciparum_.** The file against itself, or with two ids swapped, gives no difference. A set's member re-pointed gives 1 a side, and a coefficient changed 3. A Paxtools 6.0.0 round trip shows `LEFT_TO_RIGHT` against release 97's `LEFT-TO-RIGHT`, recorded in `differences.md` for T012, with the rest of what the round trip changes at the root.
  - **All species.** All 15 published files read without refusal, human in 36 s and 1.4 GB.
  - **Tests.** `ci/test_compare.py` (48 tests at #20) runs in CI's `verify` job. Each test was shown failing on a broken comparer, with these mutants:
    - names unsalted, or a prefix dropped;
    - `rdf:about="#X"` not resolved;
    - numbers by spelling, leniently, or a float held as a double;
    - datatypes dropped, or left out of keys;
    - the ontology read;
    - cycles not refused;
    - repeats counted;
    - only Controls keyed by what they control;
    - free text printed;
    - nothing carried, or examples not root-first;
    - the exit always 0;
    - attributes allowed;
    - any UnificationXref taken as a database id;
    - in #19, the root comparison: keys not coarsened, or database ids coarsened too, subjects or targets left exact, carried counting the roots, a name or URL printed, numbers not stripped or tagged as text, long, short, INF or decimals read loosely, a number's tag left out of keys, a crash exiting 1;
    - in #20, shared keys coarsened, database-id keys coarsened, non-ASCII integers or floats, no cap on integers, leading zeros counted against it, -0 kept, zeros kept in decimals, numbers hidden, numeric ids unread, the note or title for a rootless difference.
  - **Equivalent mutants.** Database ids in written order, and references unmarked in keys, are equivalent now: properties are sorted when read, and a literal always carries its `=`.
  - **After #20, from its review (#21):**
    - The rest of the differences is titled "not at the root (carried by a rehashed key, or swapped among content keys)". #20 called them carried whenever any root change existed, which credited a swap to an unrelated change.
    - A decimal's sign is tested; `.`, `+` and exponents are refused.
    - #20 said non-ASCII decimals were an equivalent mutant; they are not, since zeros are still stripped. A test now holds them as text, kept whole.
    - Only ASCII space is stripped from a number.
    - An integer's leading zeros are dropped before it is read, so 5,000 of them no longer exceed Python's limit.
    - A dangling reference (`EXTERNAL|`) is never treated as a content key.
    - A changed individual nothing points at keeps its unchanged properties off the root.
    - Python's stack stops a chain at about 5,000.
    - 56 tests in `ci/test_compare.py` (65 with `ci/test_validate.py`'s nine); each of the review's surviving mutants now fails one.
- [x] T006 `ci/fixture/`: `extract.py` over a private copy of release 97's graph (sbml-exporter's pattern), extended to publications' authors, events' created, authored, reviewed and edited records, entity references with their cross-references, GO terms, diseases, PSI-MOD terms, `inferredFrom`, catalyst activities and regulation; at least eight events, each with its parent pathways (today's exporter writes pathways only): a pathway, a TemplateReaction with its regulation, a Degradation, a disease reaction, a reaction with nested sets, DNA and RNA, and one inferred event (research R3). People and curation comments replaced by placeholders, held by `check-placeholders.sh` in CI as an allowlist; shown failing on a value not on it. — #24, replacing #22 and #23 after their reviews. The events and why each is there are in `ci/fixture/README.md`. There are nine, not eight:
  - the smallest human event of each shape, plus a small pathway with its two reactions and a black box, chosen from the published human file;
  - the first of research R3's cover, R-HSA-5660013, which is the disease reaction (a FailedReaction); R-HSA-2894858 is a disease pathway;
  - the inferred one, R-PFA-68947, which is _P. falciparum_'s, since no human event is inferred.

  - **Size:** 2,373 nodes and 4,714 relationships.
  - **What it follows:** parent pathways only along the way down. Reference sequences' own cross-references and genes are left out; the published files write neither, and they were 7,483 of the first draft's cross-references.
  - **It loads:** into an empty Neo4j 4.4 in 67 s.
  - **Names, from #22's review:** #22's fixture kept a Book's display name, which held a real author's surname, and 43 summation texts citing fixture people. Its branch was deleted.
    - Now every summation's text, publication's display name and comment is a placeholder too.
    - `check-placeholders.sh` is an allowlist of whole lines: each must be one of the four shapes `extract.py` writes, a person holds exactly its id, class and placeholders, and those free texts are placeholders. It was shown failing on #22's fixture and on twelve altered copies, with invented values only. CI's `verify` job runs it.
    - Every string was compared with the real surnames of the fixture's 1,235 people, counts only: none holds one.
    - From #23's review: a summation's display name (its text's opening) is a placeholder too. The check now also allowlists node labels, refuses person-like keys in any spelling and email- or ORCID-like values anywhere, requires relationships' values to be numbers and edits' dates to be dates. Free text on other classes is the extract-time audit's to judge, which the README says. #23's fixture and twelve altered copies fail it.
    - From #24's review, in #26 (replacing #25 after its review): the check also requires each node's `schemaClass` among its labels and applies its class rules to both, treats Book, URL and LiteratureReference as publications, and folds underscores and hyphens in person-like keys (and adds `orcid` and `author`). `ci/fixture/audit.py` makes the name search repeatable: against the private copy, it compares every string with the fixture people's real surnames and first names of three letters or more, and with emails and ORCIDs, printing counts only. It searches list items too (#25's searched only single strings, a third of them, and missed a real surname planted in a `name` list): 19,190 strings, and one hit judged a coincidence (a UniProt disease keyword), listed in its `KNOWN`. It finds 71 in #22's fixture.

- [x] T007 CI runs the exporter over the fixture graph (a Neo4j service), validates its output (T004) and compares it with `ci/fixture/expected` (T005); `expected` is today's output, and each later task updates it with the triples it changes. Today's validator errors on it, if any, are its first baseline, which may only fall, and is zero by T020. CI also runs it twice and compares the bytes (FR-006), and checks the dependency lists for any relational driver (FR-001); each shown failing (a written value changed; an order from a hash set; a JDBC driver added). — #27.
  - **The `fixture` job.** A Neo4j 4.4 service loads `ci/fixture/fixture.cypher`. The image `docker-build` made converts each species twice, as the release does (`-s`, one file per species). The files written must be exactly `ci/fixture/expected/`'s (Homo sapiens 235 kB, _P. falciparum_ 24 kB).
  - **Three checks on the output:** each file matches its expected one by meaning (`ci/compare.py`); the second run matches the first byte for byte (FR-006); and both pass the BioPAX validator 6.0.0, fetched from Maven Central, checked by sha1 and cached.
  - **Validator baseline:** today's output has no validator error, so the baseline is none allowed. The warnings are human 1,152 in 8 codes and _P. falciparum_ 103 in 6.
  - **No relational driver (FR-001):** `ci/no-relational.sh` reads Maven's resolved runtime dependencies (70, transitive ones included) in the `verify` job. `docker-push` now waits for `fixture` too.
  - **Shown failing:**
    - a JDBC driver (`mysql-connector-j`) added to a copy of the POM fails `no-relational.sh`;
    - one written name changed fails the comparison (1 a side at the root, the rest carried);
    - two proteins written in the other order, as an order from a hash set would, pass the comparison by design and fail the byte check.
  - **What today's output already holds:** more than pathways — reactions, catalyses, complexes, proteins and steps. It also holds the old converter's oddities, which the content tasks fix and these files record as they go: Evidence for every human event, where release 97 writes it for inferred events only. Its built-in validator fails to start on JDK 17, which is harmless here; T008 takes the validator out of the library.
  - **From #26's review:** `audit.py` keys its one known coincidence by the node as well as the class and property. The same keyword on another node now fails it, which was shown. The check's comments and README now say a person-like key is one from its list, not any key containing one.
  - **From #27's review, in #28:** the job requires one validator report per file written, so a validator that stops part way cannot pass on what it did write. `no-relational.sh` fails when Maven lists no dependency (it once printed a pass for 0), shows Maven's own output, and knows more drivers, pools and ORMs (jTDS, DB2, Firebird, DuckDB, Snowflake, Teradata, HikariCP, c3p0, DBCP, EclipseLink, JPA); a Neo4j JDBC driver is allowed. `audit.py`'s summary adds hits of one class and property across nodes (it showed the last node's).

## Phase 3: The code's shape (User Story 3's independence, constitution IV)

- [x] T008 [US3] One `ConversionContext` per conversion (the model, the base, the id counters, caches of events and entities by dbId), passed to the builders; no companion or `object` state; the validator out of the library. Behaviour unchanged: the fixture's expected output does not move. A CI check converts eight events in parallel, 20 times, and compares each with its single run; shown failing with the shared counters put back. — #29.
  - **`ConversionContext`** holds one conversion's model, `xml:base`, release and id counters; the builders take it in place of the model. Gone from shared state: `WriteBioPAX3`'s companion (the base, the release, the validator) and `BioPAX3Utils`' counters. A writer's base and release are its own, so `WriteBioPAX3(pathway)` no longer takes whatever release an earlier writer set.
  - **Caches:** no cache by dbId is added yet. The pathway builder's own cache of pathways stays where it is. Sharing it across a species' pathways would write a shared child once, which changes the output; that is T015's to do, under the fixture.
  - **The validator is out of the library.** `validateBioPAXFile` was never called, and its start-up failed on JDK 17 in every run. The command line gets a validator, if any, in T009.
  - **The parallel check:** `ParallelConversionTest` converts eight pathways (graph-core mocks, stubbed before any thread reads them) on eight threads, 20 times, and requires each result to equal its single run. It runs in `verify`. With the counters shared again, it fails in round 1, and so does `ConversionContextTest`.
  - **Behaviour unchanged:** CI's `fixture` job compares the output with the unchanged `ci/fixture/expected`.
  - **From #28's review:** the job counts `*.validation.xml` reports only.
  - **From #29's review, in #30:**
    - The old shared state leaked a writer's whole base, not only its release: built before another, a writer took the other's base and counters.
    - `setDBVersion`, which no longer reached the output, is removed, and with it the release the context held and nothing read.
    - The parallel check now reaches every builder that numbers ids: catalyses, a generic entity (the builder's last branch), and two species conversions with their BioSource and header. A shared counter for Catalysis alone fails it.
    - The Provenance lookup's test uses a real base, so a lookup that dropped it would fail.
    - Left for T009, which takes the launcher into the command line: its options as companion state (one run per process, no conversion shares them) and the validator's dependencies and Spring files still in the jar.
- [x] T009 [US3] Two Maven modules, `lib` (`biopax3-exporter-lib`: builders and context, graph-core and paxtools-core only) and `cli` (`biopax3-exporter`: launcher, options, logging, validator; shaded), with the library's dependency list checked in CI (no command-line or release dependency). The Dockerfile and the Jenkinsfile build the command line's jar. — #31.
  - **The split:** `lib` holds the builders, the context and all the tests (27 run); `cli` holds the launcher and `logback.xml`. The parent pom keeps the versions (dependencyManagement) and the Kotlin, ktlint and surefire plugins; only `cli` shades.
  - **The library's dependencies:** graph-core, paxtools-core and Kotlin's standard library. graph-core brought `logback-classic`, a logging binding a library should not impose, so `lib` excludes it and `cli` adds it back. `ci/lib-dependencies.sh` fails on any other direct dependency, and on `jsap`, the validator or `logback-classic` at any depth: seen failing with `jsap` added to `lib`, and with the exclusion removed.
  - **No validator in the jar:** the validator 5.1.0 dependencies, its Spring files and its four placeholder `.properties` files are removed (never called, and it failed to start on JDK 17). CI's standalone validator 6.0.0 is the validator.
  - **`ci/no-relational.sh`** lists both modules (`compile` first, so the command line finds the library in the build); seen failing with a MySQL driver added to `cli`.
  - **The image** runs `cli/target/biopax3-exporter-0.0.1-SNAPSHOT.jar`. The Jenkinsfile pulls that image and builds no jar, so it is unchanged.
  - **The launcher's companion state** stays: one run per process, and no conversion reads it. Its unused `model` is removed.
  - `dependency-reduced-pom.xml`, which shading generates, is no longer tracked, and `.gitignore` names it (in #32: it did not before).
  - **From #31's review, in #32:**
    - `ci/lib-dependencies.sh` read the direct dependencies from Maven's resolved list, which names a relocated artifact by its new name: `mysql:mysql-connector-java` added to `lib` passed it. It now reads them as declared in `lib`'s pom and the parent's, in every scope but test (a `provided` validator passed before), and a disallowed one no longer stops it before the check at any depth.
    - CLAUDE.md's pointer to the renamed section is corrected.
  - **From #32's review, in #33:** a dependency added by an active profile, or by an ancestor above the parent, passed: the declared list was read from two files. It is now read from `lib`'s effective model (`help:effective-pom`), every ancestor and active profile merged in; seen failing on MySQL in an active-by-default profile and on a `provided` artifact in a JDK-activated one. An inactive profile, which puts nothing on the classpath, passes.
- [x] T010 graph-core moves to the version sbml-exporter pins (research R4), before Phase 4; the fixture's expected output does not move, or each change is explained. (Publishing the library later needs a released graph-core, not a snapshot pinned by commit: a ticket on graph-core, T022.) — #34.
  - **The version:** graph-core 2.0.9 → 3.0.0-SNAPSHOT, resolving to the very build sbml-exporter pins: the four snapshot files (graph-core's jar and pom, reactome-parent and reactome-bom 1.0.5-SNAPSHOT) have the same sha1s as its `ci/snapshots.sha1`. No source change was needed; the 27 tests pass.
  - **Pinned by content:** `ci/snapshots.sh` (sbml-exporter's) and `ci/snapshots.sha1`; the image's build resolves with `-nsu` and runs the check over its fresh local repository. Seen failing on a changed graph-core jar.
  - **The output does not move:** the image over the fixture graph (a private Neo4j 4.4.48, as CI's job) writes both species files equal by meaning to the unchanged `ci/fixture/expected` (`compare.py`: nothing only in either).
  - The library's dependencies are still graph-core, Paxtools and Kotlin's (`ci/lib-dependencies.sh`), and no relational driver arrives (`ci/no-relational.sh`, 48 dependencies).
  - **From #33's review:** the help plugin that reads the effective model is pinned (3.5.2), and the check's known limit is stated: a profile switched off only on CI is unseen.

## Phase 4: The content (User Story 2, P1)

**Goal**: each class written as release 97 writes it. Each task changes the fixture's expected output for its class only, and compares _P. falciparum_ from the private copy before and after, its differences falling.

- [ ] T011 [US2] Physical entities: their entity references (ProteinReference, SmallMoleculeReference, DnaReference, RnaReference) with cross-references; the "Reactome DB_ID" comment and the two UnificationXrefs (database id of the release, stable id with version); cellular location; complexes with `component` and `componentStoichiometry`; features (modifications, fragments).
- [ ] T012 [US2] Reactions: BiochemicalReaction, Degradation (`left` only) and TemplateReaction (`product` only) as the event's kind; `conversionDirection`; `participantStoichiometry`; `eCNumber`; their literature.
- [ ] T013 [US2] Catalysis with its controller and its two RelationshipXrefs and the `activeUnit:` comment; Control (ACTIVATION, INHIBITION) and TemplateReactionRegulation from regulation, one per controlled reaction, with the regulation's UnificationXrefs (database id, and stable id where the published file has one).
- [ ] T014 [US2] Events' own cross-references: RelationshipXrefs for GO biological process, disease and the rest, all of them where the old converter returned early on a repeat (an obvious bug fixed; recorded in `differences.md`).
- [ ] T015 [US2] Pathways (and the caches of events and entities by dbId, kept in the `ConversionContext`, which T008 left for this task: shared across a species' pathways, they write a shared child once): components, PathwayStep with `stepProcess` (reactions, catalyses, controls, pathways) and `nextStep`; a shared child written once and referenced by each parent; no header pathway; no event outside a pathway (research R1).
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
