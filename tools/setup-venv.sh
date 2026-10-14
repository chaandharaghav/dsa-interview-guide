#!/usr/bin/env bash
# tools/setup-venv.sh: create .venv with the pinned site packages.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -x .venv/bin/mkdocs ]; then
  python3 -m venv .venv
  .venv/bin/pip install --quiet --disable-pip-version-check -r requirements.txt
fi
