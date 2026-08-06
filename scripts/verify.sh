#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if ! command -v python3 >/dev/null 2>&1; then
  echo "Required command 'python3' was not found. Install it explicitly; this script does not modify the environment." >&2
  exit 1
fi

exec python3 "$repository_root/scripts/verify.py" "$@"
