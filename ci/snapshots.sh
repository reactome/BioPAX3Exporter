#!/usr/bin/env bash
# The snapshot dependencies this build resolved, by content (spec 001 T010).
#
# graph-core is 3.0.0-SNAPSHOT, the build sbml-exporter pins the same way. A -SNAPSHOT
# version is a moving target, and Maven's own listings print "-SNAPSHOT", not which
# build was used, so this lists the sha1 of each resolved snapshot jar and pom in the
# local repository, and "check" fails when that differs from ci/snapshots.sha1. The
# image's build runs it over a fresh local repository (Dockerfile).
#
#   ci/snapshots.sh list    print the list (to update ci/snapshots.sha1 deliberately)
#   ci/snapshots.sh check   fail if the resolved snapshots differ from ci/snapshots.sha1
#
set -euo pipefail

repo="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"
here="$(cd "$(dirname "$0")" && pwd)"

list() {
    (cd "$repo" &&
        find . -type f \( -name '*-SNAPSHOT.jar' -o -name '*-SNAPSHOT.pom' \) |
        sed 's|^\./||' | LC_ALL=C sort | xargs sha1sum)
}

case "${1:-}" in
    list) list ;;
    check)
        if ! diff "$here/snapshots.sha1" <(list); then
            echo "The resolved snapshot dependencies differ from ci/snapshots.sha1 (above: < expected, > resolved)." >&2
            echo "A snapshot changed under the build. Pin a release, or update the list deliberately." >&2
            exit 1
        fi
        echo "Snapshot dependencies match ci/snapshots.sha1."
        ;;
    *) echo "usage: $0 list|check" >&2; exit 2 ;;
esac
