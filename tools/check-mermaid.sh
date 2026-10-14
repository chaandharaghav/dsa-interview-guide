#!/usr/bin/env bash
# tools/check-mermaid.sh: parse every Mermaid diagram with Mermaid 11 in headless Chrome.
# Needs network access to unpkg.com. Outside a sandbox that blocks Chrome's profile folder.
# Usage: tools/check-mermaid.sh [PATH ...]   (default: kb). Exit code 1 if a diagram fails.
set -uo pipefail
cd "$(dirname "$0")/.."
page=$(python3 tools/check_mermaid.py "$@" | sed 's/.*-> //')
chrome="${KB_CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
profile="${TMPDIR:-/tmp}/kb-chrome-profile"
mkdir -p "$profile"
result=$("$chrome" --headless=new --disable-gpu --no-first-run --user-data-dir="$profile" \
  --virtual-time-budget=30000 --dump-dom "$page" 2>/dev/null \
  | python3 -c 'import sys,re,html; m=re.search(r"<pre id=\"out\">(.*?)</pre>", sys.stdin.read(), re.S); print(html.unescape(m.group(1)) if m else "MERMAID CHECK DID NOT RUN")')
echo "$result"
case "$result" in "MERMAID OK"*) exit 0 ;; *) exit 1 ;; esac
