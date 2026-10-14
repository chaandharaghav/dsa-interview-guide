#!/usr/bin/env bash
# tools/check-all.sh: the exit gate for every phase. All checks must pass.
set -uo pipefail
cd "$(dirname "$0")/.."
status=0
run() { echo "== $*"; "$@" || status=1; }
tools/setup-venv.sh
run python3 -m unittest discover -s tools/tests -q
run python3 tools/check_format.py kb
run python3 tools/run_sql.py kb
run python3 tools/run_java.py kb
run .venv/bin/mkdocs build --strict --quiet
exit $status
