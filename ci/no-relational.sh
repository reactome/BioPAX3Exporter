#!/usr/bin/env bash
# Fails if the exporter's resolved dependencies hold a relational database driver or
# reader (spec 001, FR-001): it reads the graph alone. Reads Maven's own list of the
# runtime dependencies, transitive ones included.
# Usage: ci/no-relational.sh   (from the repository's root, with Maven on the PATH)
set -euo pipefail
list=$(mktemp)
trap 'rm -f "$list"' EXIT
mvn -B -q dependency:list -DincludeScope=runtime -DoutputFile="$list" -DappendOutput=false >/dev/null
relational='(mysql|mariadb|postgresql|ojdbc|oracle\.database|mssql|sqlserver|sqlite|hsqldb|com\.h2database|derby|jdbc|r2dbc|hibernate-core|hibernate-orm|jooq|mybatis|spring-jdbc)'
count=$(grep -cE ':.*:(jar|pom)' "$list" || true)
if grep -iE "$relational" "$list"; then
  echo "a relational database driver or reader among the $count dependencies, above"
  exit 1
fi
echo "none of the $count runtime dependencies is a relational database driver or reader"
