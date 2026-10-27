#!/usr/bin/env bash
# Fails if a person's name could have survived in the fixture graph (spec 001, T006),
# by an allowlist: every Person holds extract.py's placeholders and nothing else but
# its dbId and class, every InstanceEdit's display name and note are placeholders, and
# no other node holds a property that names or reaches a person.
# Usage: ci/fixture/check-placeholders.sh [ci/fixture/fixture.cypher]
set -euo pipefail
fixture=${1:-$(dirname "$0")/fixture.cypher}
status=0

person='^CREATE \(:[^{]*`Person`[^{]* \{`dbId`: [0-9]+, `displayName`: "Person([0-9]+), P", `firstname`: "Placeholder", `initial`: "P", `schemaClass`: "Person", `surname`: "Person([0-9]+)"\}\);$'
while IFS= read -r line; do
  if ! [[ $line =~ $person ]] || [[ ${BASH_REMATCH[1]} != "${BASH_REMATCH[2]}" ]]; then
    echo "a Person that is not a placeholder: ${line:0:60}"
    status=1
  fi
done < <(grep -E '^CREATE \(:[^{]*`Person`' "$fixture")

while IFS= read -r line; do
  if ! [[ $line =~ \`displayName\`:\ \"Placeholder\ edit\ [0-9]+\" ]] ||
     { [[ $line =~ \`note\`: ]] && ! [[ $line =~ \`note\`:\ \"Placeholder\ note\" ]]; }; then
    echo "an InstanceEdit that is not a placeholder: ${line:0:60}"
    status=1
  fi
done < <(grep -E '^CREATE \(:[^{]*`InstanceEdit`' "$fixture")

# Properties that name or reach a person, on any node (a URL here is a database's or
# an identifier's link, and is left alone).
if grep -nE '`(orcidId|eMailAddress|project)`:' "$fixture" | cut -c1-60 | grep .; then
  echo "a property that names or reaches a person, above"
  status=1
fi

people=$(grep -cE '^CREATE \(:[^{]*`Person`' "$fixture" || true)
edits=$(grep -cE '^CREATE \(:[^{]*`InstanceEdit`' "$fixture" || true)
echo "$people people, $edits instance edits checked"
exit $status
