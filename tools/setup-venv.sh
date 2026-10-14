#!/usr/bin/env bash
# tools/setup-venv.sh: create .venv with the pinned site packages.
set -euo pipefail
cd "$(dirname "$0")/.."
wanted=$(shasum requirements.txt | cut -d' ' -f1)
if [ ! -x .venv/bin/mkdocs ] || [ "$(cat .venv/.requirements.sha 2>/dev/null)" != "$wanted" ]; then
  [ -d .venv ] || python3 -m venv .venv
  .venv/bin/pip install --quiet --disable-pip-version-check -r requirements.txt
  echo "$wanted" > .venv/.requirements.sha
fi
