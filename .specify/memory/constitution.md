# BioPAX3Exporter Constitution

These principles are not aspirations. Each one is here because breaking it has
already cost Reactome's BioPAX something specific. The cost is named so the rule
can be argued with rather than merely obeyed.

## Core Principles

### I. Proven against the published files, by meaning

This exporter is to make Reactome's BioPAX Level 3, both the release's
`biopax.zip` and the website's per-event files, in place of Pathway-Exchange's
converters, which read the relational database.

The Kotlin port was merged with no comparison against the files Reactome
publishes. Its code, read on 24 Oct 2026 (reactome/WebsiteAngular spec 015,
research R9), builds:

- no entity references and no cross-references;
- no stoichiometry, no catalysis controllers and no regulation;
- no literature and no complex components;
- empty evidence.

Release 97's published `biopax.zip` has all of these. "It doesn't produce
identical files" was true, and hid all of that.

**So:** a change to what is written is measured against the release's published
files. Both files are reduced to (class, property, target) triples, with each
`rdf:ID` replaced by its content. Every difference is either fixed or recorded,
with its reason, in the spec that owns it. Byte equality is not the bar: the old
converter's order and ids cannot be reproduced even against itself.

### II. Verify the instrument

Two checks this repository appears to have do not run:

- **`mvn test` compiles no tests.** The Kotlin plugin binds only `compile`, so the
  42 tests under `src/test` have never run. The command reports "No tests to
  run" and succeeds.
- **ktlint passes nothing.**
  - The Docker lint stage is never started by CI.
  - The build's `ktlint:format` rewrites the sources before any check could see
    them.
  - `main` has violations that no build has ever reported.

There is a third trap of the same kind. The standalone BioPAX validator (6.0.0,
measured) exits 0 whatever it finds. Separately, this repository's own in-process
validator call (`validateBioPAXFile`, validator 5.1.0) is never called.

**So:** name what a check actually reads, and see it fail once before relying on
it. A gate that cannot fail is not a gate.

### III. Prove a test fails before believing it passes

The event-list mode (`-l`) builds an empty pathway, because the events it
collects are never added. The events in no pathway are computed and then dropped.
Neither was caught, and no test could have caught them: none has ever run
(Principle II).

**So:** a fix comes with a test that fails without it, and that has been seen to
fail. Where that is impractical, say so in the pull request rather than implying
coverage.

### IV. One implementation, for the release and the website

The website's per-event BioPAX and the release's species files came from two
converters, by two routes, and nothing held them together.

**So:** this repository is to have two parts:

- a library module: graph in, BioPAX model out, published as a versioned artefact;
- a thin command line for the release, built on that library.

The website is to depend on the published library, not on a copy. The two modules
exist (`lib` and `cli`); nothing is published yet. Until it is, a change lands in
the library both will use.

### V. Production services are not test fixtures

Reactome's production services (the website, its APIs and its databases) are
not used for development, comparison or verification: a burst of requests can
load the live site.

**The reference is the release's own published bulk files**, static files in the
release's download directory (`download.reactome.org/{release}/`). Fetching them
is allowed, one file at a time, only when a comparison needs them, and they are
kept out of the repository. **A private copy of the release's graph database** is
the source for runs.

**So:** no request to the production services. The published files, fetched as
above, are the only exception.

### VI. People's names stay out of the repository

This repository is public. The BioPAX it writes carries the names of authors and
curators, in its literature and its curation comments. A fixture or a test log
copied from real output would publish them.

**So:** fixtures replace those values with placeholders. Comparisons hash them,
and never print them. This is about exported data, not about contributor credits
such as the pom's developer list.

## Quality Gates

Every change is to pass the following checks. The first spec's first tasks make
each one real (Principle II):

- **The unit tests run and pass.** Each fix comes with a test seen failing first.
  Today `mvn test` compiles none, so making it run them comes first.
- **`mvn ktlint:check`, with no format step**, in CI. `main` must be brought
  clean first.
- **The BioPAX validator over the output for a fixture graph**, with its reports
  read. The version and how it runs are chosen in the first spec.
- **For a change to what is written:** the comparison with the release's
  published files (Principle I), with its result in the pull request.

## Development Workflow

- **Specs first.** Feature-sized work goes through Spec Kit before any code:
  `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`. The specs live in
  `specs/`. The pull request that completes a task ticks it, with its number. A
  single-PR fix names the spec it belongs to, if any.
- **Every change is a pull request.** `main` requires one approving review.
  Admins can still merge past it, and no CI check is required yet. Until the
  checks above are required, run them before asking for a merge.
- **Work that also touches the website** keeps its website part in the website's
  own specs, and links across.

## Governance

Amendments are made by pull request, and must state which principle changed and
what it cost to learn. A principle with no cost behind it does not belong here.

`CLAUDE.md` carries day-to-day guidance; where the two disagree, this file wins.

**Version**: 1.0.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
