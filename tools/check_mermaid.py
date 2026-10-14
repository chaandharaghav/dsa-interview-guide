#!/usr/bin/env python3
"""Write an HTML page that parses every Mermaid diagram in the KB with Mermaid 11.

Usage: python3 tools/check_mermaid.py [PATH ...]   (default: kb)

The script prints the path of the HTML page. Open the page in a browser with
network access (it loads Mermaid 11 from unpkg, the same source that Material
for MkDocs uses). The page shows "MERMAID OK: <n> diagrams" or one line for
each diagram that fails to parse. The page title holds the same summary.
"""
import html
import json
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mdblocks import md_files, parse_blocks  # noqa: E402

PAGE = """<!doctype html>
<meta charset="utf-8">
<title>MERMAID CHECK RUNNING</title>
<pre id="out">running</pre>
<script src="https://unpkg.com/mermaid@11/dist/mermaid.min.js"></script>
<script>
const diagrams = __DIAGRAMS__;
(async () => {
  mermaid.initialize({ startOnLoad: false });
  const failures = [];
  for (const d of diagrams) {
    try {
      await mermaid.parse(d.code);
    } catch (e) {
      failures.push(d.where + ": " + String(e.message || e).split("\\n").slice(0, 3).join(" | "));
    }
  }
  const summary = failures.length
    ? "MERMAID FAILED: " + failures.length + " of " + diagrams.length
    : "MERMAID OK: " + diagrams.length + " diagrams";
  document.title = summary;
  document.getElementById("out").textContent = summary + "\\n" + failures.join("\\n");
})();
</script>
"""


def main(argv: list) -> int:
    diagrams = []
    for path in md_files(argv or ["kb"]):
        for block in parse_blocks(path.read_text(encoding="utf-8"), path):
            if block.lang == "mermaid":
                diagrams.append({"where": f"{path}:{block.line}", "code": block.body})
    out = Path(tempfile.gettempdir()) / "kb-mermaid-check.html"
    payload = json.dumps(diagrams).replace("</", "<\\/")
    out.write_text(PAGE.replace("__DIAGRAMS__", payload), encoding="utf-8")
    print(f"mermaid: {len(diagrams)} diagrams -> file://{html.escape(str(out))}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
