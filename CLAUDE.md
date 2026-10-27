# Working in this repository

Day-to-day guidance for anyone, human or agent, changing BioPAX3Exporter. The
principles are in [`.specify/memory/constitution.md`](.specify/memory/constitution.md);
where this file and the constitution disagree, the constitution wins.

## What it is

BioPAX3Exporter writes Reactome's BioPAX Level 3 from the Neo4j graph:

- through graph-core;
- with Paxtools building the model;
- in Kotlin, built with Maven, and built and run on JDK 17 images.

It is to become the one implementation of BioPAX Level 3, used for two things:

- **the release's `biopax.zip`:** one `.owl` file per species, made by the command
  line;
- **the website's per-event downloads:** one pathway or reaction, made through the
  library module.

Two Maven modules: `lib` (`biopax3-exporter-lib`, graph in and BioPAX model out,
depending on graph-core and Paxtools alone, as `ci/lib-dependencies.sh` checks)
and `cli` (`biopax3-exporter`, the release's command line on it, shaded; the
image runs its jar).

The plan is in [issue #4](https://github.com/reactome/BioPAX3Exporter/issues/4)
and in this repository's specs.

## Specs first: use Spec Kit

Feature-sized work goes through Spec Kit before any code, and the specs live in
`specs/`:

- **Feature-sized** means a new capability, a change to what is written, or
  anything that will take more than one pull request.
- **The sequence** is `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`, then
  implementation task by task.
- **Keep `tasks.md` current.** The pull request that completes a task ticks it and
  adds its number, in the same pull request.
- **A single-PR fix** needs no new spec, but its description names the spec it
  belongs to, if any.
- **Work that also touches the website** (`reactome/WebsiteAngular`) keeps its
  website part in that repository's specs. Its spec 015 is the website's half of
  this work, and holds the research this work started from.

## The checks

- **`mvn verify`** compiles and runs the library's tests and runs `ktlint`'s check
  (no format step: it rewrites nothing). CI runs it on JDK 17.
- **The validator.** The jar carries none. CI runs the standalone BioPAX
  validator 6.0.0 over the fixture's output. It exits 0 whatever it finds, so
  CI reads its reports.

## Proving what is written

- **The reference** is the release's own published `biopax.zip`, from the
  release's download directory (`download.reactome.org/{release}/`):
  - fetch one file at a time, only when a comparison needs it;
  - keep it out of the repository, since it holds people's names.

  No other Reactome production service is used.
- **Compare by meaning**, not bytes. Reduce each file to (class, property, target)
  triples, with each `rdf:ID` replaced by its content, and record every
  difference with its reason.
- **Run against a private copy of the release's graph**, not a shared database.

## This repository is public

Nothing internal goes in it, in code, comments, commit messages or pull request
descriptions:

- no host names or server paths;
- no credentials;
- no internal release-process detail;
- no people's names from exported data.

## Before pushing

- The unit tests run and pass: see "Checks that do not work yet". Every fix has a
  test seen failing first.
- `mvn verify` passes (tests and ktlint).
- No personal names in fixtures, logs or test output.
