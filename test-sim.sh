#!/usr/bin/env bash
# Compile WarTech and run the headless simulation regression checks (SimCheck).
#
# Builds from clean on every run, because a stale build is just a PITA.
#
# Exit 0 is good. Exit non-zero is bad.
set -euo pipefail

cd "$(dirname "$0")"

CLASSES="target/classes"
TEST_CLASSES="target/simcheck-classes"
SIM_SRC="test/sim/SimCheck.java"

if [ ! -f "$SIM_SRC" ]; then
  echo "error: $SIM_SRC not found" >&2
  exit 1
fi

echo "==> Building with Maven..."
mvn -q clean compile

if [ ! -d "$CLASSES" ]; then
  echo "error: Maven reported success but $CLASSES does not exist" >&2
  exit 1
fi

echo "==> Compiling SimCheck..."
rm -rf "$TEST_CLASSES"
mkdir -p "$TEST_CLASSES"
javac -d "$TEST_CLASSES" -cp "$CLASSES" "$SIM_SRC"

echo "==> Running SimCheck..."
exec java -cp "$TEST_CLASSES:$CLASSES" SimCheck
