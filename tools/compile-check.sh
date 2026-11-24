#!/bin/bash
# Compile the app's Kotlin, one build at a time (several people/agents may share this checkout).
# Usage: tools/compile-check.sh [path-filter]
#   path-filter: only print errors whose file path contains this (e.g. feature/home).
cd "$(dirname "$0")/.." || exit 1
LOCK="${TMPDIR:-/tmp}/hisaabwise-gradle.lock"
until mkdir "$LOCK" 2>/dev/null; do sleep 3; done
trap 'rmdir "$LOCK"' EXIT
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
OUT=$(./gradlew :app:compileDebugKotlin --console=plain -q 2>&1)
STATUS=$?
if [ -n "$1" ]; then
  echo "$OUT" | grep -E '^(e|w): ' | grep -- "$1"
else
  echo "$OUT" | grep -E '^e: '
fi
echo "--- gradle exit $STATUS (errors in other features may be someone else's in-progress work)"
