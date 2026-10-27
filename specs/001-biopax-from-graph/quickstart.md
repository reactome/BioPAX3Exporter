# Quickstart: proving the exporter

Nothing here uses a production service. The published `biopax.zip` is fetched once from the release's download directory and kept outside the repository.

## The checks

1. `mvn -q test` runs the unit tests (a test made to fail fails it).
2. `mvn -q ktlint:check` checks style, rewriting nothing.
3. In CI, the fixture graph's output is validated (`ci/validate.py`) and compared with `ci/fixture/expected` (`ci/compare.py`).

## A species against the published file

1. Start a private copy of the release's graph (as sbml-exporter's dry runs do), on loopback.
2. Run the command line for one species into an output folder.
3. `unzip -p biopax.zip Plasmodium_falciparum.owl > published.owl`, outside the repository.
4. `python3 -I ci/compare.py published.owl out/Plasmodium_falciparum.owl` prints the differences by kind and count, with names hashed.
5. Expected: only FR-002's three reasons, each recorded in `differences.md`.

## One event

1. Convert an event through the library (a reaction, a pathway, a black box, a disease reaction).
2. Compare it with its species file: its triples are contained in the species file's, but for the recorded differences.
