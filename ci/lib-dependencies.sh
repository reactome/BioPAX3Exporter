#!/usr/bin/env bash
# Fails unless the library module (lib) depends directly on graph-core, Paxtools and
# Kotlin's standard library alone, and on no command-line or release dependency at any
# depth (spec 001 T009, constitution IV): the website is to use it as it is.
# Usage: ci/lib-dependencies.sh   (from the repository's root, with Maven on the PATH)
set -euo pipefail
direct=$(mktemp)
all=$(mktemp)
trap 'rm -f "$direct" "$all"' EXIT
mvn -B -q -pl lib dependency:list -DincludeScope=runtime -DexcludeTransitive=true -DoutputFile="$direct" -DappendOutput=false
mvn -B -q -pl lib dependency:list -DincludeScope=runtime -DoutputFile="$all" -DappendOutput=false
allowed='org.reactome.server.graph:graph-core
org.biopax.paxtools:paxtools-core
org.jetbrains.kotlin:kotlin-stdlib
org.jetbrains.kotlin:kotlin-stdlib-jdk8'
found=$(grep -oE '^ +[^: ]+:[^: ]+:(jar|pom)' "$direct" | sed -E 's/^ +//; s/:(jar|pom)$//' | sort)
if [ -z "$found" ]; then
  echo "Maven listed no direct dependencies: nothing was checked"
  exit 1
fi
status=0
if [ "$found" != "$(sort <<<"$allowed")" ]; then
  echo "the library's direct dependencies are not the allowed ones:"
  diff <(sort <<<"$allowed") <(echo "$found") | sed 's/^</  allowed, missing:/; s/^>/  not allowed:/' | grep allowed
  status=1
fi
# The command line's options and the validator, at any depth.
if grep -iE '(jsap|biopax-validator|validator-core|logback-classic):' "$all"; then
  echo "a command-line or release dependency reaches the library, above"
  status=1
fi
[ $status = 0 ] && echo "the library depends on: $(echo $found | tr ' ' ',')"
exit $status
