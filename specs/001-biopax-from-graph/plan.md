# Implementation Plan: BioPAX Level 3 from the graph, as the release publishes it

**Branch**: `spec/001-plan` | **Date**: 2026-10-27 | **Spec**: [spec.md](spec.md)

## Summary

Make the exporter write what release 97's `biopax.zip` holds, from the graph, and prove it by meaning. The checks become real first: tests that compile and run, lint that checks, a validator over a fixture graph's output in CI, and a Python comparer that reduces two files to the same triples (research R2, R3). The code then gets one context per conversion and a library module beside a thin command line (R4), and the content follows the published file, class by class (R1): references and cross-references, stoichiometry, catalysis with its controller, regulation, literature, evidence for inferred events, sets as the published files type them, shared children written once. A full run against a private copy of release 97's graph is compared with every species file, and each difference is fixed or recorded under one of FR-002's three reasons.

## Technical Context

**Language/Version**: Kotlin (Maven), JDK 17; Python 3 for the comparer and the fixture tools.

**Primary Dependencies**: graph-core (the version sbml-exporter pins), Paxtools core; BioPAX validator 6.0.0 (CI and the command line only); lxml for the comparer.

**Storage**: none; the graph is read through graph-core.

**Testing**: JUnit and Mockito for the builders; the fixture graph in a Neo4j service in CI; the comparer; the validator.

**Target Platform**: the release's step (a container) and the website's library service (spec 010 in reactome/WebsiteAngular), both JDK 17.

**Project Type**: a library and a command line.

**Performance Goals**: the comparer reads human's file in about a minute (R2); a full run's time is recorded against today's step (SC-007).

**Constraints**: nothing relational; no production service; no people's names in fixtures or reports; a deterministic order; conversions independent of each other.

**Scale/Scope**: 15 species files; human's holds 546,714 individuals.

## Constitution Check

| Principle | How the plan meets it |
| --- | --- |
| I. Proven against the published files, by meaning | The comparer (R2) over every species of release 97 (Order of work 5); every difference fixed or recorded under FR-002's three reasons |
| II. Verify the instrument | The comparer shown sound before use (two proteins swapped: no difference; one coefficient changed: three triples each side); the validator's report read, not its exit status |
| III. Prove a test fails before believing it passes | Every check and every fix with a test seen failing first; the tests are bound and repaired before anything else |
| IV. One implementation, for the release and the website | The library module, the command line built on it (R4) |
| V. Production services are not test fixtures | The published `biopax.zip` (fetched once, outside the repository) and a private graph copy only |
| VI. People's names stay out of the repository | Fixture placeholders and their check; the comparer hashes authors and curation comments |

**Result**: pass, no violations.

## Project Structure

```text
specs/001-biopax-from-graph/   spec, research, plan, contracts/checks.md, quickstart.md, tasks.md

lib/                           biopax3-exporter-lib: ConversionContext, builders; graph-core, paxtools-core
cli/                           biopax3-exporter: launcher, options, logging, validator; shaded
ci/
  compare.py                   the comparer (R2)
  validate.py                  the validator's reports, as counts per code
  fixture/                     extract.py, placeholders and their check, the fixture's expected output
.github/workflows/ci.yml       build, test, lint, fixture run, validation, comparison with expected
```

**Structure Decision**: two Maven modules as sbml-exporter's (R4); the Python tools under `ci/`, as there.

## Order of work

1. **The checks** (User Story 1): bind and repair the tests; lint that checks; the validator's report reader; CI running all three, each shown failing.
2. **The comparer and the fixture**: `ci/compare.py` shown sound on the published file; the fixture graph extracted from a private copy, placeholders checked; the fixture's run in CI, its output validated and compared with what it is expected to be.
3. **The code's shape**: the context per conversion and the library and command-line modules (User Story 3's thread safety, constitution IV), behaviour unchanged, the parallel check in CI.
4. **The content**, class by class, each by its own pull request against the fixture and _P. falciparum_: entities with references and cross-references; reactions (BiochemicalReaction, Degradation, TemplateReaction) with stoichiometry; catalysis and regulation; pathway steps; literature, evidence and provenance; sets; shared children once; `xml:base` and ids (User Story 2's rules, R1).
5. **The full run**: every species from a private copy of release 97's graph, compared with the published files, differences recorded with counts and reasons (`differences.md`), tickets opened for the data's; validated against the published files' baseline (User Story 2).
6. **One event** through the library: the 30-event sample compared with its species file (User Story 3).
7. **The release step**: the Jenkinsfile's command, the zip, the release's `xml:base` check, the time (User Story 4).

## Complexity Tracking

No constitution violations to justify.
