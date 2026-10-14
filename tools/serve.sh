#!/usr/bin/env bash
# tools/serve.sh: open the KB site at http://127.0.0.1:8000
set -euo pipefail
cd "$(dirname "$0")/.."
tools/setup-venv.sh
exec .venv/bin/mkdocs serve
