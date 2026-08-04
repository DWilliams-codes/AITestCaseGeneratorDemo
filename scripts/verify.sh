#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
harness_only=false

if [[ "${1:-}" == "--harness-only" ]]; then
  harness_only=true
elif [[ $# -gt 0 ]]; then
  echo "Usage: $0 [--harness-only]" >&2
  exit 2
fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command '$1' was not found. Install it explicitly; this script does not modify the environment." >&2
    exit 1
  fi
}

cd "$repository_root"
require_command python3
python3 -c 'import sys; assert sys.version_info >= (3, 11), "Python 3.11 or newer is required for tomllib."'

echo '==> TestForge deterministic harness'
python3 scripts/validate-harness.py

if [[ "$harness_only" == true ]]; then
  echo 'TestForge harness-only verification passed.'
  exit 0
fi

for command_name in java mvn node npm; do
  require_command "$command_name"
done

java_version="$(java -version 2>&1 | head -n 1)"
if [[ "$java_version" != *'version "21.'* ]]; then
  echo "Java 21 is required; found: $java_version" >&2
  exit 1
fi

node -e 'const [major, minor] = process.versions.node.split(".").map(Number); if (major < 22 || (major === 22 && minor < 12)) { throw new Error(`Node 22.12.0 or newer is required; found ${process.versions.node}.`); }'

if [[ ! -d frontend/node_modules ]]; then
  echo 'frontend/node_modules is missing. Run npm ci explicitly before verification.' >&2
  exit 1
fi

echo '==> Backend verify'
(cd backend && mvn --batch-mode --no-transfer-progress verify)

echo '==> Frontend verify'
(
  cd frontend
  npm run format:check
  npm run lint
  npm run typecheck
  npm run test:coverage
  npm run build
)

echo 'TestForge repository verification passed.'
