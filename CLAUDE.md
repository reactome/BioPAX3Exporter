# Working in this repository

Day-to-day guidance for anyone, human or agent, changing BioPAX3Exporter. The
principles are in [`.specify/memory/constitution.md`](.specify/memory/constitution.md);
where this file and the constitution disagree, the constitution wins.

## What it is

BioPAX3Exporter writes Reactome's BioPAX Level 3 from the Neo4j graph:

- through graph-core;
- with Paxtools building the model;
- in Kotlin, built with Maven on JDK 17.

It is to become the one implementation of BioPAX Level 3, used for two things:

- **the release's `biopax.zip`:** one `.owl` file per species, made by the command line;
- **the website's per-event downloads:** one pathway or reaction, made through the library.

The plan is in [issue #4](https://github.com/reactome/BioPAX3Exporter/issues/4) and in this repository's specs.

## Specs first: use Spec Kit

Feature-sized work goes through Spec Kit before any code, and the specs live in
`specs/`:

- **Feature-sized** means a new capability, a change to what is written, or
  anything that will take more than one pull request.
- **The sequence** is `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`, then
  implementation task by task.
- **Keep `tasks.md` current.** The pull request that completes a task ticks it and
  adds its number, in the same pull request.
- **Work that also touches the website** (`reactome/WebsiteAngular`) keeps its
  website part in that repository's specs. Its spec 015 is the website's half of
  this work.

## Proving what is written

- **The reference** is the release's own published `biopax.zip`, from the
  release's download directory. Fetch one file at a time, and only when a
  comparison needs it. Keep it out of the repository: it holds people's names.
- **Compare by meaning**, not bytes. Reduce each file to (class, property, target)
  triples, with each `rdf:ID` replaced by its content, and record every
  difference with its reason.
- **Run against a private copy of the release's graph**, not a shared database,
  and never against Reactome's production services.
- **The BioPAX validator always exits 0.** Read its XML reports.

## Before pushing

- `mvn test` passes, and every fix has a test seen failing first.
- ktlint passes, as the Docker build's lint stage runs it.
- No personal names in fixtures, logs or test output.
