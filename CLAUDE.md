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
- **the website's per-event downloads:** one pathway or reaction, made through a
  library module that does not exist yet.

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

## Checks that do not work yet

Don't trust these until the first spec has fixed them (constitution, Principle II):

- **`mvn test` runs no tests.** The Kotlin plugin binds only `compile`, so
  `src/test` is never compiled. A green `mvn test` means nothing.
- **ktlint.** `mvn package` runs `ktlint:format`, which rewrites the sources.
  Check with `mvn ktlint:check` on its own, and expect it to fail on `main` today.
- **The validator.** `validateBioPAXFile` (validator 5.1.0) is never called. The
  standalone validator exits 0 whatever it finds, so read its reports.

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
- `mvn ktlint:check` passes, run on its own.
- No personal names in fixtures, logs or test output.
