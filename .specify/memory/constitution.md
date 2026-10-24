# BioPAX3Exporter Constitution

These principles are not aspirations. Each one is here because breaking it has
already cost Reactome's BioPAX something specific. The cost is named so the rule
can be argued with rather than merely obeyed.

## Core Principles

### I. Proven against the published files, by meaning

This exporter is to make Reactome's BioPAX Level 3, both the release's
`biopax.zip` and the website's per-event files, in place of Pathway-Exchange's
converters, which read the relational database.

The Kotlin port was merged with unit tests on mocks and no comparison with the
files Reactome publishes. Measured against release 97's `biopax.zip` on 24 Oct
2026, it writes:

- no entity references and no cross-references;
- no stoichiometry, no catalysis controllers and no regulation;
- no literature and no complex components;
- empty evidence.

"It doesn't produce identical files" was true, and hid all of that.

**So:** a change to what is written is measured against the release's published
files. Both files are reduced to (class, property, target) triples, with each
`rdf:ID` replaced by its content. Every difference is either fixed or recorded,
with its reason, in the spec that owns it. Byte equality is not the bar: the old
converter's order and ids cannot be reproduced even against itself.

### II. Verify the instrument

The BioPAX validator (6.0.0, measured) exits 0 whatever it finds, so a CI step that trusts its exit
code passes every file. The release's own check of `biopax.zip` reads
`xml:base`, and this exporter writes a different form.

**So:** name what a check actually reads. Read the validator's reports, not its
exit code. When a number is surprising, suspect the instrument first.

### III. Prove a test fails before believing it passes

The event-list mode (`-l`) builds an empty pathway: the events it collects are
never added. Species mode writes sub-pathways more than once, and drops the
events in no pathway. Every test passed, because none of them looked at what
was written.

**So:** a fix comes with a test that fails without it, and that has been seen to
fail. Where that is impractical, say so in the pull request rather than implying
coverage.

### IV. One implementation, for the release and the website

The website's per-event BioPAX and the release's species files came from two
converters, by two routes, and nothing held them together.

**So:** this repository has a library module (graph in, BioPAX model out) and a
thin command line for the release. The website depends on the published library,
not on a copy. A change lands in the library, or it lands nowhere.

### V. Production is not a test fixture

Reactome's production services are not used for development, comparison or
verification: one burst of research requests is enough to hurt the site. The
release's published files in its download directory are the reference for the
bulk output. A private copy of the release's graph database is the source for
runs.

**So:** no request to the production services. The published files are fetched
one at a time when a comparison needs them, and kept out of the repository.

### VI. People's names stay out of the repository

BioPAX carries authors and curation comments with people's names in them.

**So:** fixtures replace personal values with placeholders. Comparisons hash
them, and never print them.

## Quality Gates

Every change passes:

- `mvn test`: unit tests, each fix with a test seen failing first;
- ktlint, as the Docker build's lint stage runs it;
- the BioPAX validator over the output for a fixture graph, with its reports read;
- for a change to what is written, the comparison with the release's published
  files (Principle I), with its result in the pull request.

## Development Workflow

- **Specs first.** Feature-sized work goes through Spec Kit before any code:
  `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`. The specs live in
  `specs/`. The pull request that completes a task ticks it, with its number.
- **`main` is protected.** Every change is a pull request.
- **Work that also touches the website** keeps its website part in the website's
  own specs, and links across.

## Governance

Amendments are made by pull request, and must state which principle changed and
what it cost to learn. A principle with no cost behind it does not belong here.

`CLAUDE.md` carries day-to-day guidance; where the two disagree, this file wins.

**Version**: 1.0.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
