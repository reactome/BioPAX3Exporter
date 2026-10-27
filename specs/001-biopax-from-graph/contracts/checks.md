# Contract: the checks

Each is shown failing on the named breakage before it is trusted (constitution III).

| Check | Holds | Shown failing by |
| --- | --- | --- |
| Unit tests (`mvn test`) | The builders' behaviour on mocked graph objects | A test made to fail: CI fails |
| Lint (`mvn ktlint:check` at `validate`) | Style, without rewriting | A violation added: CI fails, and nothing is rewritten |
| Validation (`ci/validate.py` over the validator's XML) | No validator error in the fixture's output | An element made invalid in the fixture's output: CI fails, naming the code |
| The fixture's output (`ci/compare.py` against `ci/fixture/expected`) | What a change writes for the fixture is what it is expected to write | A written value changed: CI fails, naming the triples |
| Placeholders (`ci/fixture/check-placeholders.sh`) | Every person's name and every "Authored:", "Reviewed:" and "Edited:" comment in the fixture is a placeholder (an allowlist of placeholders, so no real name is listed in the repository) | A value not on the allowlist put in: CI fails |
| Parallel conversions | Two conversions at once write, byte for byte, what each writes alone | The shared counters put back: the check fails |
| The same bytes every run | Two runs of the fixture write identical bytes (FR-006) | An order taken from a hash set: the check fails |
| Nothing relational | The library's and the command line's dependency lists hold no relational database driver or reader (FR-001) | A JDBC driver added: the check fails |
| The species comparison (`ci/compare.py`, full run, not in CI) | Each species file matches the published one but for FR-002's three reasons | Run on a file with one coefficient changed: three triples each side |

**Counts that may only fall:** the recorded differences per kind, from the full run onward.
