#!/usr/bin/env bash
# Fails unless the library module (lib) depends directly on graph-core, Paxtools and
# Kotlin's standard library alone, and on no command-line or release dependency at any
# depth (spec 001 T009, constitution IV): the website is to use it as it is.
# The direct dependencies are read as declared, in lib's pom and the parent's, so that a
# relocated artifact (which Maven lists under its new name) is still seen; every scope but
# test counts. Maven's resolved list is used for the check at any depth.
# Usage: ci/lib-dependencies.sh   (from the repository's root, with Maven and Python 3 on the PATH)
set -euo pipefail
all=$(mktemp)
trap 'rm -f "$all"' EXIT
mvn -B -q -pl lib dependency:list -DincludeScope=runtime -DoutputFile="$all" -DappendOutput=false
allowed='org.reactome.server.graph:graph-core
org.biopax.paxtools:paxtools-core
org.jetbrains.kotlin:kotlin-stdlib
org.jetbrains.kotlin:kotlin-stdlib-jdk8'
found=$(python3 -I - pom.xml lib/pom.xml <<'PY'
import sys, xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
for path in sys.argv[1:]:
    # <dependencies> of the project itself, not <dependencyManagement>'s versions
    for d in ET.parse(path).getroot().findall("m:dependencies/m:dependency", ns):
        if (d.findtext("m:scope", "compile", ns) or "").strip() != "test":
            print(d.findtext("m:groupId", "", ns).strip() + ":" + d.findtext("m:artifactId", "", ns).strip())
PY
)
found=$(sort -u <<<"$found")
if [ -z "$found" ]; then
  echo "Maven listed no direct dependencies: nothing was checked"
  exit 1
fi
status=0
if [ "$found" != "$(sort <<<"$allowed")" ]; then
  echo "the library's direct dependencies are not the allowed ones:"
  diff <(sort <<<"$allowed") <(echo "$found") | sed 's/^</  allowed, missing:/; s/^>/  not allowed:/' | grep allowed || true
  status=1
fi
# The command line's options and the validator, at any depth.
if grep -iE '(jsap|biopax-validator|validator-core|logback-classic):' "$all"; then
  echo "a command-line or release dependency reaches the library, above"
  status=1
fi
[ $status = 0 ] && echo "the library depends on: $(echo $found | tr ' ' ',')"
exit $status
