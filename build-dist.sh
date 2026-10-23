#!/usr/bin/env bash
# Build WarTech with Maven/TeaVM and stage a static, host-anywhere docs/ directory.
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Building with Maven..."
mvn -q clean package

GEN_DIR="target/generated/wasm/teavm"
DIST_DIR="docs"

if [ ! -f "$GEN_DIR/classes.wasm" ]; then
  echo "error: expected build output at $GEN_DIR/classes.wasm, but it doesn't exist" >&2
  exit 1
fi

echo "==> Staging $DIST_DIR..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/teavm"

cp "$GEN_DIR/classes.wasm" "$DIST_DIR/teavm/"
cp "$GEN_DIR/classes.wasm-runtime.js" "$DIST_DIR/teavm/"
cp index.html "$DIST_DIR/"
cp assets_data.js "$DIST_DIR/"

WASM=$(wc -c < "$DIST_DIR/teavm/classes.wasm" | tr -d ' ')
RT=$(wc -c < "$DIST_DIR/teavm/classes.wasm-runtime.js" | tr -d ' ')
echo "==> Done: module ${WASM} B wasm + ${RT} B runtime, staged in $DIST_DIR/"
echo "    Serve it with e.g.: ./serve.sh"
