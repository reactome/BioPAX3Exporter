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
- [x] T010 graph-core moves to the version sbml-exporter pins (research R4), before Phase 4; the fixture's expected output does not move, or each change is explained. (Publishing the library later needs a released graph-core, not a snapshot pinned by its content: a ticket on graph-core, T022.) — #34.
  - **The version:** graph-core 2.0.9 → 3.0.0-SNAPSHOT, resolving to the very build sbml-exporter pins: the four snapshot files (graph-core's jar and pom, reactome-parent and reactome-bom 1.0.5-SNAPSHOT) have the same sha1s as its `ci/snapshots.sha1`. No source change was needed; the 27 tests pass.
  - **Pinned by content:** `ci/snapshots.sh` (sbml-exporter's) and `ci/snapshots.sha1`; the image's build resolves with `-nsu` and runs the check over its fresh local repository. Seen failing on a changed graph-core jar.
  - **The output does not move:** the image over the fixture graph (a private Neo4j 4.4.48, as CI's job) writes both species files equal by meaning to the unchanged `ci/fixture/expected` (`compare.py`: nothing only in either).
  - The library's dependencies are still graph-core, Paxtools and Kotlin's (`ci/lib-dependencies.sh`), and no relational driver arrives (`ci/no-relational.sh`, 48 dependencies).
  - **From #33's review:** the help plugin that reads the effective model is pinned (3.5.2), and the check's known limit is stated: a profile switched off only on CI is unseen.
  - **From #34's review, in #35:**
    - **The bytes move, the meaning does not.** _P. falciparum_ is byte-identical to 2.0.9's; _Homo sapiens_ differs by reordering and id renumbering only (2,076 diff lines; identical with `rdf:ID`s stripped and lines sorted). graph-core 3 orders a lazily loaded collection by its relationship's `order` (`ORDER BY r.order`), which 2.0.9 left to Neo4j; most likely the curated order. A second run is byte-identical.
    - **Which build:** the resolved jar is `graph-core-3.0.0-20250925.120535-19`, the snapshot published on 25 Sep 2025, which WebsiteAngular's `deploy/java/PINS.md` maps to `62aef35`. sbml-exporter resolves the same artifact (same sha1s); WebsiteAngular's java-library service builds the same commit from source, not this artifact (PINS.md: 172 of 377 classes byte-identical, the rest identical by `javap` but two in order). So research R6's open question (spec 015's T029 settling another version) is answered by one commit for all three, and one artifact for the two exporters. It is pinned by artifact sha1, not by commit.
    - Read by the review, not exercised by any test: a relationship with no `stoichiometry` is now read as 1 (2.0.9 threw); the fixture has it on every relationship.
    - `ci/snapshots.sh` passed with an empty list and a wrong repository path (the failing `cd` was inside a process substitution); it now fails when the repository is missing or the list is empty. The Dockerfile's `-nsu` comment said more than `-nsu` does.
    - The command line's help said "A tool for generating SBML files" and defaulted the port to 7474 (Neo4j's HTTP port, which Bolt never answers): now BioPAX Level 3 and 7687. The release passes `-b 7687`, so its runs do not change.
    - From #35's review (replaced by #36): `--help` printed the help and then connected to Neo4j with the default credentials (no check of `messagePrinted()`); it now stops there, exit 0. `ci/snapshots.sh list` on a missing repository exits 1 rather than writing a marker line to stdout as a list.

## Phase 4: The content (User Story 2, P1)

**Goal**: each class written as the release writes it (release 97, then 98: below). Each task changes the fixture's expected output for its class only, and compares _P. falciparum_ from the private copy before and after, its differences falling.

**From 28 Oct 2026, release 98.** The release published its `biopax.zip` for 98, so the tasks from T012 on, T020's full run included, generate from a private copy of release 98's graph and compare with release 98's published files. The fixture was regenerated from release 98 by `extract.py`, its events unchanged (#41, replacing #40 after its review). T011's measurements, made on release 97, stand as recorded.

- [x] T011 [US2] Physical entities: their entity references (ProteinReference, SmallMoleculeReference, DnaReference, RnaReference) with cross-references; the "Reactome DB_ID" comment and the two UnificationXrefs (database id of the release, stable id with version); cellular location; complexes with `component` and `componentStoichiometry`; features (modifications, fragments). — #38, replacing #37 after its review.
  - **The rules** are Pathway-Exchange's `ReactomeToBioPAX3XMLConverter.createPhysicalEntity` and `BioPAX3FeatureHandler`, read from the graph:
    - the class from the reference entity's;
    - the shortest name as the display name, the rest as names;
    - the first compartment, with its GO term;
    - modifications (the first PSI-MOD term, else what the residue is modified by, cut at "[ChEBI") and the fragment from the start and end coordinates;
    - the reference with its organism, its names (display name, names, gene names), its own xref (with the `UniProt Isoform`, `MOD:`, `GO:` and `CHEBI:` rules) and its first comment, else its first description;
    - the "Reactome DB_ID" comment and Reactome's two ids, with the release's exact comments;
    - the data source;
    - cross-references and diseases as "additional information" RelationshipXrefs;
    - a drug's class and comment.
  - **Each entity, reference, location, vocabulary and xref is written once per file.** They are kept in the `ConversionContext` by kind and dbId, as the release keeps them; the events' cache stays T015's. The context now carries the release, for the database-id xrefs. One BioSource per taxon, with its NCBI Taxonomy xref, is shared by pathways (their first species) and references.
  - **New:** `BioPAXPhysicalEntityBuilder` and `BioPAXXrefBuilder`, the second shared with the events to come. Sets are written as PhysicalEntity with their own properties, and their class and members are left to T017.
  - **Decided** (`differences.md`):
    - every cross-reference and disease is written, where the old converter stopped at one it had written before (an obvious bug);
    - the graph's Mondo cross-references, put there by `graph-add-links`, are written;
    - a modification repeated on a protein is one feature.
  - **The fixture:**
    - **Comments as lists.** `extract.py` wrote a reference sequence's `comment` list as one string, which graph-core could not read, so every reference failed to load once it was read. It now keeps the list, and `check-placeholders.sh` accepts a list of placeholders (an empty one too).
    - **Species and PSI-MOD terms** are taken with their cross-references and databases.
    - **Regenerated** from the private copy: 2,378 nodes and 4,739 relationships, five nodes and 25 relationships more than before (the species' NCBI Taxonomy xrefs and three reference databases). With the comment fix alone, the file differed from the old one only in the 110 references' comments, now lists. `audit.py` finds only its known coincidence. `expected` is the new output.
  - **Against the published _P. falciparum_:** before, 253,233 triples only in the published file and 80,413 only in the converted one; after, 196,639 and 67,333. Entity by entity, what remains is recorded in `differences.md`.
  - **Tests:** `BioPAXPhysicalEntityBuilderTest`, 16 tests. Each of 22 mutants of the rules made one fail:
    - an early return on a disease already written;
    - no `UniProt Isoform`;
    - the first name rather than the shortest;
    - no `CHEBI:` prefix, or no `GO:` prefix (two mutants);
    - a fragment for the default coordinates;
    - nothing kept once;
    - no "[ChEBI" cut;
    - no gene names;
    - the last comment;
    - no taxonomy xref;
    - a coefficient of 1;
    - no drug comment;
    - PSI-MOD's last name;
    - no release in the database name;
    - no version in the stable-id URL;
    - from #37's review, which found a ModifiedNucleotide written as an empty feature (it is a transcriptional modification; 79 residues in release 97):
      - no coordinate for a ModifiedNucleotide, or no modification for one (two mutants);
      - no coordinate for a ReplacedResidue;
      - no ProteinReference for a protein drug;
      - no description in place of a missing comment;
      - what a residue is modified by ahead of its PSI-MOD term.

    One more, sets typed by their reference, was equivalent, and the clause it changed is removed. `ParallelConversionTest`'s pathways now have a species, so their organisms are still exercised.
- [x] T012 [US2] Reactions: BiochemicalReaction, Degradation (`left` only) and TemplateReaction (`product` only) as the event's kind; `conversionDirection`; `participantStoichiometry`; `eCNumber`; their literature. — #43, replacing #42 after its review.
  - **The rules** are `handleReaction` and `guessReactionType` from Pathway-Exchange's converter. A black box with one input, no output and "degradation" in its display name is a Degradation, with its inputs on the left. One with no input and one output is a TemplateReaction, with its products only and no catalysis. Any other reaction is a BiochemicalReaction, which goes left to right and has its catalysts' EC numbers. Inputs and outputs are each written once, with a new Stoichiometry where they take part more than once; graph-core's `getInput` and `getOutput` repeat a participant by its count, as the relational lists did.
  - **Shared with every event**, and so written here for reactions: the names, by T011's rule (now `BioPAX3Utils.addNames`); Reactome's two ids; the data source. A pathway step lists the catalyses of any interaction, not only of a BiochemicalReaction.
  - **Left to their tasks:** the reactions' literature and evidence (T016), with every event's; catalyses' content (T013); the GO and disease xrefs (T014); a shared reaction written once (T015).
  - **Measured on release 98** (`differences.md`). All 970 _P. falciparum_ reactions agree with the published file in what this task writes. A TemplateReaction's product is not written again as a participant, which Paxtools would otherwise do.
  - **Tests:** `BioPAXInteractionBuilderTest`. Its third test mocked the Paxtools model, which the reaction's xrefs could not use, so it is replaced by four tests on a real model. Each of 11 mutants made one fail:
    - no check of the name for "degradation";
    - a Degradation from any number of inputs;
    - a TemplateReaction from any black box without inputs;
    - no direction;
    - no EC numbers;
    - the participant kept;
    - a stoichiometry for every participant;
    - every count 1;
    - no ids;
    - no data source;
    - the graph's display name, not the shortest name.
  - **From #42's review:**
    - The validator-baseline heading `differences.md` lost is back.
    - The EC numbers are recorded as matching release 98's exactly. The comment that said otherwise is corrected.
    - Eight mutants that survived are now caught:
      - a case-sensitive check for "degradation" (the release's names are capitalised);
      - a Degradation that ignores its outputs;
      - a TemplateReaction that ignores its inputs;
      - catalyses only for a BiochemicalReaction;
      - a step listing only a BiochemicalReaction's catalyses;
      - the last of the shortest names;
      - no names on a TemplateReaction;
      - no ids on a TemplateReaction.
    - One more mutant, the display name kept among the names, is equivalent: Paxtools keeps the display name in the name set either way.
    - Left for T013, which found none to write: the release's converter also puts a pathway's own regulations into its step.
- [x] T013 [US2] Catalysis with its controller and its two RelationshipXrefs and the `activeUnit:` comment; Control (ACTIVATION, INHIBITION) and TemplateReactionRegulation from regulation, one per controlled reaction, with the regulation's UnificationXrefs (database id, and stable id where the published file has one). — #58, replacing #57 after its review.
  - **The rules** are `handleCatalystActivity`, `handleActiveUnits`, `handleRegulation` and `createModulationFromRegulation` from Pathway-Exchange's converter.
    - **A catalyst activity** is a new Catalysis for each reaction (none for a TemplateReaction, which returns before it), with:
      - its catalyst as controller, ACTIVATION;
      - its GO molecular function and its own database id ("Same Catalyst Activity", no PSI-MI xref) as RelationshipXrefs;
      - the data source, and an `activeUnit:` comment for each active unit, the unit written.
    - **A regulation** is a new Control for each reaction it regulates (a TemplateReactionRegulation for a TemplateReaction), with:
      - ACTIVATION for a positive one (a Requirement too), INHIBITION for a negative one;
      - Reactome's two ids, the data source, its regulator as controller, and its active units.
  - **Active units** are taken in dbId order: graph-core gives a catalyst activity's as a set, and the ids they number must be the same on every run.
  - **`ci/compare.py`** reads an `activeUnit: #id` comment as a reference to the entity it names. A test, which fails on the old comparer, holds it: the same entity under another id compares equal.
  - **Measured on release 98** (`differences.md`): the published file's catalyses and controls are matched but for sets (T017) and repetition (T015). The validator reports 0 errors on the fixture's output.
  - **Not written, as the graph has none:** a pathway's regulations (graph-core's `Regulation` regulates reactions only) and a catalyst activity's own regulation (the converter's Modulation).
  - **Left to their tasks:** a regulation's literature (T016, done in #69).
  - **From #57's review:**
    - A regulation graph-core lists twice (its link's stoichiometry 2, eleven times at release 98) is written once, as the converter's set of regulations makes it.
    - Regulations follow the reaction's ids, in the converter's order.
    - The validator heading `differences.md` lost is back, and the catalysis count is corrected.
  - **Tests:** `BioPAXControlTest`, 7 tests. Each of 25 mutants made one fail:
    - no controller;
    - no ACTIVATION;
    - no molecular function, or the function typed as a biological process;
    - no own id, or the own id as a UnificationXref;
    - active units unsorted, missing, or named by their full URI;
    - no regulations, or regulations only off a TemplateReaction;
    - always a Control;
    - INHIBITION for a positive regulation, or no INHIBITION;
    - no regulation ids, regulator, active units or data source;
    - "Same Catalyst Activity" with a PSI-MI xref;
    - a GO id without its prefix;
    - from #57's review: regulations not taken once each; the GO xref not shared; the "Same Catalyst Activity" comment changed; a catalysis's units numbered in descending dbId order; a regulation's units out of their order.
- [x] T014 [US2] Events' own cross-references: RelationshipXrefs for GO biological process, disease and the rest, all of them where the old converter returned early on a repeat (an obvious bug fixed; recorded in `differences.md`). — #60, replacing #59 after its review.
  - **What is written:** the event's GO biological process, as a RelationshipXref of type "gene ontology term for cellular process" (PSI-MI MI:0359), and each of its diseases, as an "additional information" RelationshipXref shared with the entities that name it. This covers reactions and pathways, as the release's `handleEventGOBP` and `handleDisease` write them. Release 98's files carry no other RelationshipXref on events.
  - **Order:** for a reaction, the GO term before the regulations and the diseases after the data source, as in the converter; literature (T016) goes before the regulations too. A pathway's own xrefs then came before its children's, where the converter writes them after: ids only, left to T015.
  - **Measured on release 98** (`differences.md`): _P. falciparum_'s 60 reaction GO xrefs are matched. Its pathways' were written but awaited their ids (given in T015). The fixture gains 23 disease xrefs the old converter skipped (on reactions, 19 DOID 162 and 2 DOID 5603; on pathways, 2 DOID 162); across release 98's human file there are 379.
  - **From #59's review:** the order claim made true (reactions only), the pathways' skipped diseases recorded, and two behaviours tested: an event's disease xref is the entity's own, and a TemplateReaction and a Degradation have their GO term and diseases.
  - **Tests:** `BioPAXEventXrefTest`, 5 tests. Each of 11 mutants made one fail:
    - no GO term, or no diseases, on a reaction;
    - no GO term, or no diseases, on a pathway;
    - the process typed as a function;
    - only the first disease;
    - a disease's xref not shared;
    - an event's diseases keyed apart from an entity's;
    - no GO term, or no diseases, on a TemplateReaction;
    - no GO term on a Degradation.
- [x] T015 [US2] Pathways (and the caches of events and entities by dbId, kept in the `ConversionContext`, which T008 left for this task: shared across a species' pathways, they write a shared child once): components, PathwayStep with `stepProcess` (reactions, catalyses, controls, pathways) and `nextStep`; a shared child written once and referenced by each parent; no header pathway; no event outside a pathway (research R1; T015 found that the release writes a top-level reaction alone, and release 98 has none). — #62, replacing #61 after its review; #64, replacing #63, from #62's review.
  - **What is written:** a species' file starts from the events of that species alone that no other such event holds (`SpeciesEvents`, as the release chose them: 26 for _P. falciparum_, 29 for human). Each event is written once and shared by every pathway that holds it, with one PathwayStep per event; there is no header pathway. A step's processes are its event and that event's catalyses and controls, and its next steps come from the events it precedes. A reaction of the species alone that no such event holds would be written alone, with no step, as the release would write it; release 98 has none, hence "no event outside a pathway". The launcher's unused orphan code and `BioPAXSpeciesBuilder` are gone.
  - **Measured on release 98** (`differences.md`): _P. falciparum_'s pathways, reactions, steps and catalyses number the published file's. What still differs for pathways is T016's (comments, evidence) and the writer's (six display names with a trailing space, T018). The fixture's expected files are regenerated; the validator gives 0 errors.
  - **From #61's review:** a pathway is kept before its events, as the converter keeps it, so a `hasEvent` cycle ends rather than overflowing the stack (the new test failed with a StackOverflowError before); the wording on events outside a pathway corrected here, in research R1 and in `SpeciesEvents`; stale "still" lines in the T014 section; the unused `species` parameter, `schemaService` and import removed.
  - **From #62's review (#64):** spec.md's edge case agrees with research R1 and the code; which reactions are written alone is stated as `SpeciesEvents` selects them; the remaining stale lines are dated; the cycle test also pins the step the pathway met again gets, as in the converter.
  - **Tests:** `BioPAXPathwayBuilderTest`, 7 new tests. Each of 13 mutants made one fail:
    - a shared pathway, or a shared reaction, written once per parent;
    - a step per parent;
    - no `pathwayOrder`, or no `pathwayComponent`;
    - children made before their pathway;
    - `nextStep` reversed, or none;
    - no controls on a step, or catalyses only;
    - a species' file without step links, with a header pathway, or without its top-level reactions.
  - `SpeciesEvents`' query is held by the fixture job, apart from its species filter; dropping that filter would start human from 345 events (checked on the private copy).
- [x] T016 [US2] Literature and provenance: PublicationXref (PubMed, ISBN, URL) on the event; Evidence (IEA) for inferred events only; the single Provenance; curation comments, with HTML kept as the published files keep it. — #69, replacing #68 after its review.
  - **What is written:** each summation as a comment with its HTML; the event's literature and its summations' as PublicationXrefs, each once per file (an article by PubMed id, a book by ISBN, a web page by address, as `ReactomeToBioPAXPublicationConverter` writes them); for an inferred event, an Evidence for each source, with that source's first summation, its literature and the evidence code; and the "Authored: ", "Reviewed: " and "Edited: " comments; and a regulation's literature on its Control. All in `BioPAXLiteratureBuilder`; the empty Evidence every event had, and `BioPAX3ReferenceUtils`, which stripped the HTML, are gone.
  - **Measured on release 98** (`differences.md`): every _P. falciparum_ event, and every one of the fixture's human events, agrees with the published file in its literature ids, curation comments, summations and Evidence. What still differs at the root is the writer's trimming (T018) and, in 23 of 5,750 PublicationXrefs, the form of an author's name the release's database held.
  - **From #68's review:** a regulation's literature, as the converter wrote it (none in release 98's graph); tests for a book titled by its own title, an Evidence's blank summation kept, the sources' order, and no comment for a revision.
  - **Tests:** `BioPAXLiteratureTest`, 16 tests. Each of 34 mutants made one fail:
    - the HTML stripped, or a blank summation kept;
    - the summations' literature left out, or a publication not shared;
    - "PubMed" for "Pubmed"; no year; a source without its colon;
    - a book titled by its title, or its source without "(Book)"; no url; no authors;
    - an Evidence shared between events; the source's last summation for its first; no Evidence comment, or no Evidence literature;
    - the vocabulary not shared; Reactome's own name for the code kept; ECO for every type;
    - no "Reviewed"; "Revised" for "Edited"; an unnamed edit kept;
    - a reaction without its literature, evidence or curation;
    - a pathway without its summations, literature, evidence or curation;
    - a Control without its regulation's literature, or without its summations';
    - a book without the fallback to its title; a blank Evidence comment dropped; the sources unsorted; revisions written.
- [x] T017 [US2] Sets: typed by the class their members share, else PhysicalEntity; "Converted from EntitySet in Reactome" and `memberPhysicalEntity`. Which single-class sets are PhysicalEntity (a set among the members, or a member with no entity reference, or another reason: research R1, R6) decided against the graph over the whole of _P. falciparum_, and the rule that reproduces it written. — #71, replacing #70 after its review.
  - **The rule:** the release's own (`getReactomeEntityType`): the one kind the set's members share, by their reference entities' schema classes (an isoform its gene product's, a complex Complex), else PhysicalEntity; a set or polymer among them has no kind. The single-class sets of small molecules that are PhysicalEntity hold chemical drugs, whose ReferenceTherapeutic kind is written as PhysicalEntity (27 of release 98's 34 only drugs, 6 drugs and simple entities, 1 a set). One change, recorded: a set whose member is written as another class (a member with no reference) is a PhysicalEntity, as Paxtools requires; release 98's _P. falciparum_ file has none (human is measured in T020).
  - **Measured on release 98** (`differences.md`): every one of _P. falciparum_'s 846 sets, and the fixture's 26 human sets, has the published class, members and comment. Triples only in the published file fall from 72,683 to 2,383; what remains is the writer's (T018), the authors' names (T016) and the cross-references the release skipped or did not yet have.
  - **From #70's review:** a test that a candidate counts in the kind; the measured claims narrowed to _P. falciparum_; a ReferenceGroup kept apart though graph-core lets no member have one (equivalent, so untested); research R1's 33 and R6 answered.
  - **Tests:** 6 in `BioPAXPhysicalEntityBuilderTest`, replacing the T011 placeholder. Each of 13 mutants made one fail: sets left PhysicalEntity; no comment; no members; no candidates; members made first; a nested set, or a polymer, counting for nothing; an isoform its own kind; a drug's reference as a molecule's; a majority kind; a set of complexes as PhysicalEntity; no downgrade; candidates left out of the kind. Looking deeper than the members is equivalent, so the code looks at the members.
- [x] T018 [US2] `xml:base` as `http://www.reactome.org/biopax/{release}/{dbId}#` (the species' or the event's), ids `{Class}{n}` in a deterministic order, the same on every run; literals trimmed, as the release's writer trims them (T015 found six display names with a trailing space). — #73, replacing #72 after its review.
  - **What changed:** the base is the release's (`http://www.reactome.org/biopax/98/170928#` for _P. falciparum_), literals are trimmed as Java's `String.trim()` trims them and a carriage return kept as `&#xD;` when the model is written, and a failure to write is an error rather than a message, leaving no part of a file. The three outputs (file, standard output, string) share one writer.
  - **Ids:** already `{Class}{n}` in the converter's order and the same on every run. The release's own cannot be reproduced: its adaptor gave a species' events as a set ordered by identity hashes (`differences.md`).
  - **Measured on release 98** (`differences.md`): triples only in the published _P. falciparum_ file fall from 2,383 to 1,128; what remains was recorded by T011, T012, T016 and T017.
  - **From #72's review:** trimming as Java's `String.trim()` (Kotlin's took off a no-break space, which the release kept); `&#xD;` as the release wrote it; the id order's source named (the adaptor's set, not `grepTopLevelEvents`); a comment on the order Paxtools gives its editors in; a file written beside its name and moved into place.
  - **From #71's review**, carried here: R1's 33 said to be release 97's count, not matched set by set; R1 points to T017.
  - **Tests:** `WriteBioPAX3Test`, the three bases changed and 6 tests added. Each of 10 mutants made one fail: an https base; a release of 0 kept; a species segment; no trimming; trimming at the end only; the untrimmed value kept beside the trimmed; a raw carriage return; a failure to write caught; Kotlin's trim for Java's; the part written left behind after a failure. That a file is never seen half written rests on the move, which is not tested.

- [x] T019 [US3] One event: `convert(event)` for a pathway, a reaction, a black box, a degradation and a template reaction (the event builder today asks for an abstract `Process` and is never called); `-l` either converts each listed event or goes, saying which. — #75, replacing #74 after its review.
  - **What changed:** `WriteBioPAX3` converts any event alone (`createModel`), or events listed together (`createModelForEvents`, for `-l`), each with the release's base. `-t` and `-m` take any event; `-l` converts each listed event into one model, or, if an id names no event, says which and writes nothing; any such id ends the run with exit 1. `BioPAXEventBuilder`, never called, is removed.
  - **Measured on the fixture graph** (`differences.md`): a pathway, a reaction, a black box, a Degradation, a TemplateReaction, a disease reaction and a DNA/RNA reaction, each alone, are contained in the human file. The ontology header's comment of a one-event file is not written (no place for it in a Paxtools model).
  - **From #73's review**, carried here: the file is renamed into place with an atomic move (the move that replaced deleted the old file first, so a run killed between the two lost it); the carriage returns counted as values.
  - **From #74's review:** more than one mode, and an event of a kind not converted, end the run with exit 1; an error reading an id is reported as itself; every mode makes the output directory; CI's step also requires the event in its file (an empty file is contained in any); a test that a file written again replaces the one there; the ontology comment's omission said to be a choice.
  - **Tests:** `WriteBioPAX3Test`, 4 tests. Each of 6 mutants made one fail: only pathways converting alone; a list's base naming the release; a list without its release in its xrefs; an event's base without its id; the events' steps unlinked; a move that does not replace. The launcher is held by a new step in CI's fixture job (one Degradation contained in the human file, and present in its own; a list with a species' id refused and not written), which two launcher mutants (no failing exit; the list written anyway) each made fail, run locally.

## Phase 5: Proof (User Stories 2 and 3)

- [x] T020 [US2] The full run: every species from a private copy of release 98's graph, the same 15 file names as the published zip, each compared by T005. `differences.md` records each remaining difference by kind, count and reason (order and ids; the data's, with its ticket, the EC numbers first, reactome/graph-importer#9; an obvious bug fixed), and nothing else. Validated by T004 against each published file's own baseline, made in this task for all 15 (human with enough memory for its size, measured). The private copy removed after. — #79, replacing #78 after its review.
  - **Found on the way (#76):** the first full run spent its time reading every object of the model for the one Provenance each individual names (`getObjects` filters the whole model), quadratic in a species' size; the Provenance is now made once per file and kept in the context, and the helpers that scanned are removed. Also, from #75's review: an output directory that is a file, or cannot be made, is said so; it is made only when the arguments are sound; a parsing failure exits 1.
  - **Found on the way (#77):** human then ran out of a 4 GB heap. Two causes, measured with class histograms: the conversion held every event it had written (the steps kept their events, and the species' top-level events were held in one list), so all that was loaded beneath them stayed; and the file was made in memory, then copied three times. A step now keeps the ids of the events before it, the top-level events are loaded one at a time, and the file is written as it is made, carriage returns escaped on the way. Human converts in 15 min 44 s, at most 1.7 GB resident; _P. falciparum_'s peak falls from 983 MB to 460 MB; the output is the same to the byte.
  - **Measured** (`differences.md`, the full run): every species in 1 h 55 min; the 15 published files by name and a sixteenth, _M. tuberculosis_, which the release step's species configuration leaves out (T023 decides what the release command writes). What differs at the root falls in five kinds, each recorded with its reason; no file has a validator error its published one lacks. The validator's baseline is made for all 15. Two of the task's conditions are carried: the private copy is kept for T021 and removed after it, and the sixteenth file is T023's.
  - **From #78's review:** the extra cross-references' databases listed in full, those whose cause was not traced said so; the three empty names counted with the empty comments; the _M. tuberculosis_ explanation narrowed to what the sources show; standard output's last newline tested; `SpeciesEvents.topLevel`, no longer called, removed.
  - **From #77's review:** the escaping stream tested directly (a single byte, part of an array); standard output ends with a newline again; a species' emptiness checked by its ids.
- [x] T021 [US3] One event through the library: the 30-event sample (pathways of several sizes, reactions, a black box, a disease event, an inferred one) each contained in its species file by meaning but for recorded differences. The private copy of the graph removed after (carried from T020). — #81, replacing #80 after its review.
  - **Measured** (`differences.md`): the 30 events (ten pathways of five sizes, nine reactions of five kinds, six disease events, five inferred mouse events), each converted alone, are each contained in their species' file from T020, by `ci/compare.py`'s triples, with no difference to excuse. The sample's ids are in `differences.md`; `ci/contained.py` (new, with 6 tests) does the comparison, both ways: nothing in an event's file that its species' file lacks, and nothing of the event's own individuals missing. A reversed difference fails five of the six tests, dropping the second way two, and comparing it as sets one. The private copy is to be removed once this merges. From #79's review: this line carries the copy's removal; the _M. tuberculosis_ wording says only what the published zip shows; the standard-output test's name says it checks the last newline. From #80's review: the check made two-way (a truncated event's file was contained), and a triple held more often than in the species' file tested.
- [ ] T022 Tickets: each issue found on the way, and each improvement, opened on the repository that owns it and linked from `differences.md` (spec FR-003).

## Phase 6: The release step (User Story 4, P2)

- [x] T023 [US4] The Jenkinsfile's command runs as written (its stray quote, its jar, its options); it writes `biopax.zip` with one file per species, the release's species and no others (the full run of T020 wrote a sixteenth, _M. tuberculosis_, which the release step's configuration leaves out); the release's own `xml:base` check passes on it. — #86, replacing #85 after its review.
  - **The species:** `-s` takes a list of species ids, each written to its own file; the Jenkinsfile passes the 15 of release 98's published zip (human and the 14 it infers to), read from those files' own `xml:base`. Whether to add _M. tuberculosis_ is issue #84; if so, it is one more id there.
  - **The command:** the stray quote at the end of the `docker run` line, which would have stopped the shell, is gone; the jar is the image's entry point and `-o ./output` lands in the mounted folder (the image's working directory is the container root the Jenkinsfile mounts under).
  - **The release's check** is the DownloadDirectory step's (release-download-directory's Jenkinsfile): every `.owl` in `biopax.zip` must hold `xml:base="http://www.reactome.org/biopax/{release}`. CI's fixture job now runs the same grep on its output; it passes for 98 and fails for 97.
  - **From #85's review:** a species named with `-s` that has no events now ends the run with exit 1, since the release's check counts files and would not miss one (shown with a species added to the private fixture graph for the run and removed after); CI's second run of the fixture job uses the list form, which its byte comparison holds to the one-by-one run; the Jenkinsfile's credentials reach the shell as `"$user"` and `"$pass"`, quoted, rather than through Groovy (#87, from #86's review, with differences.md's account of what a list with a bad id writes). Noted then: the release's pattern had no `/` after the release number, so release 9 would match 98's files (the exporter's own check, below, has it); the list of 15 is kept in step with what each release publishes by hand.
  - **The check is the exporter's now (#89):** release-download-directory, whose Jenkinsfile held the `xml:base` check, is on its way to being archived as its files move to their own repositories and the graph. The exporter's Jenkinsfile now checks its own output before the zip is published: one `.owl` for each species named, each with `xml:base="http://www.reactome.org/biopax/{release}/`, the release number ended by its slash so release 9 cannot pass 98's files. CI's mirror has the slash too. Simulated on the fixture's files: it passes for the right release and fails for another, or for one species too many.
  - **Run as written**, as far as this host allows: the image built from this branch, the command's own shape (`--network=host`, the mount, `-o ./output`, `-s` with two species the fixture graph holds) against the private fixture graph, then the Jenkinsfile's `zip -r` and the release's check on the unzipped files: two files, both with the release's base. The release server's own run, with its credentials and graph, is the release's; its time is T024.
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
