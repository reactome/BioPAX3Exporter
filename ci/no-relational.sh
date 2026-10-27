#!/usr/bin/env bash
# Fails if the exporter's resolved dependencies hold a relational database driver or
# reader (spec 001, FR-001): it reads the graph alone. Reads Maven's own list of the
# runtime dependencies, transitive ones included.
# Usage: ci/no-relational.sh   (from the repository's root, with Maven on the PATH)
set -euo pipefail
list=$(mktemp)
trap 'rm -f "$list"' EXIT
mvn -B -q dependency:list -DincludeScope=runtime -DoutputFile="$list" -DappendOutput=false
# Drivers, connection pools and ORMs by their artifacts; a Neo4j JDBC driver reads the graph
# and is no relational one.
relational='(mysql|mariadb|postgresql|ojdbc|oracle\.database|mssql|sqlserver|sqlite|hsqldb|com\.h2database|derby|jtds|com\.ibm\.db2|jaybird|firebird|duckdb|snowflake|teradata|jdbc|r2dbc|hikaricp|c3p0|commons-dbcp|hibernate-core|hibernate-orm|eclipselink|jakarta\.persistence|javax\.persistence|spring-data-jpa|jooq|mybatis|spring-jdbc)'
count=$(grep -cE ':.*:(jar|pom)' "$list" || true)
if [ "$count" -eq 0 ]; then
  echo "Maven listed no dependencies: nothing was checked"
  exit 1
fi
if grep -iE "$relational" "$list" | grep -iv neo4j; then
  echo "a relational database driver or reader among the $count dependencies, above"
  exit 1
fi
echo "none of the $count runtime dependencies is a relational database driver or reader"
