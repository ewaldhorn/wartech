#!/bin/sh
set -e
cd "$(dirname "$0")"

PORT="${PORT:-8080}"
DIST="docs"

if [ ! -d "$DIST" ]; then
  echo "serve: $DIST does not exist. Running ./build-dist.sh..."
  ./build-dist.sh
fi

echo "serving dist/ on http://127.0.0.1:$PORT/  (Ctrl-C to stop)"
exec python3 -m http.server "$PORT" --bind 127.0.0.1 --directory "$DIST"
