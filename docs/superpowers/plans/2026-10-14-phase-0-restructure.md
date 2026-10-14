# Phase 0: Restructure, Site, and Checkers — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the DSA content into `kb/dsa/`, add a local MkDocs Material site, and add the checker scripts that gate every later phase.

**Architecture:** All KB content lives under `kb/`. `mkdocs.yml` at the repo root builds `kb/` into a site. The `mkdocs-awesome-nav` plugin orders the sidebar through `.nav.yml` files. Three Python scripts (`check_format.py`, `run_java.py`, `run_sql.py`) share one Markdown block parser (`mdblocks.py`). `tools/check-all.sh` runs the scripts, their unit tests, and `mkdocs build --strict`.

**Tech Stack:** Python 3.10 (standard library only for the checkers), unittest, MkDocs 1.6.1, Material for MkDocs 9.7.7, mkdocs-awesome-nav 3.3.0, pymdown-extensions 12.2, JDK 21 (Zulu 21.0.7 at `~/.jenv/versions/21`), SQLite 3.51 through Python's `sqlite3` module.

**Spec:** `docs/superpowers/specs/2026-10-14-software-engineering-kb-design.md`

## Global Constraints

- Content root is `kb/`. Repo folder name does not change.
- Existing DSA content moves with `git mv`. Its text changes only where a link breaks.
- Pin `mkdocs==1.6.1`. MkDocs 2.0 removes the plugin system (Material team warning, 2026-02-18).
- New Java content compiles with `javac --release 21`. Existing DSA blocks (`kb/dsa/` root files and pattern folders `01`–`20`) compile with `--release 17`.
- Code lines in new content are at most 100 characters.
- Topic pages and case studies carry front matter `priority: P1|P2|P3`.
- Minimum `??? question` blocks: P1 = 8, P2 = 4, P3 = 2 (topic pages and HLD case studies).
- Commit messages use the `docs:`, `chore:`, or `feat:` prefix, and contain no Claude attribution.
- Git writes may need the sandbox disabled (project memory note).

## Review Focus

1. A code block inside an admonition or a collapsible block is indented by 4 spaces. The parser must find it and strip the indent. (Test in Task 1.)
2. A Java block that declares `public class Foo` must compile as `Foo.java`, not `Snippet.java`. (Test in Task 3.)
3. A Java block with its own `import` lines must keep the imports at the top when the runner wraps it in a class. (Test in Task 3.)
4. A heading with a number prefix and a suffix (`## 5. Optimization Insight (The "Aha" Moment)`) must match the required section name "Optimization Insight". (Test in Task 2.)
5. A SQL page with no `sql-result` block must still fail when a statement has a syntax error. (Test in Task 4.)

---

## File Structure

| Path | Responsibility |
|---|---|
| `kb/dsa/**` | Existing DSA content, moved |
| `kb/README.md` | KB home page |
| `kb/study-plan.md` | Cross-subject plan (skeleton) |
| `kb/glossary.md` | Glossary (skeleton) |
| `kb/<subject>/README.md` | Subject index with the planned page list |
| `kb/.nav.yml`, `kb/dsa/.nav.yml` | Sidebar order and titles |
| `README.md` | How to open the site |
| `mkdocs.yml`, `requirements.txt` | Site build |
| `tools/mdblocks.py` | Shared parser: fenced blocks, markers, front matter, headings |
| `tools/check_format.py` | Code line width, template headings, question counts |
| `tools/run_java.py` | Compile Java blocks; run `Main.java` programs |
| `tools/run_sql.py` | Run SQL blocks on SQLite; compare results |
| `tools/java-support/*.java` | `ListNode`, `TreeNode` for LeetCode-style snippets |
| `tools/tests/test_*.py` | Unit tests for the tools |
| `tools/setup-venv.sh`, `tools/serve.sh`, `tools/check-all.sh` | Entry points |
| `docs/superpowers/drafting-brief.md` | Shared instructions pasted into each subagent prompt |

### Marker conventions (used by all tools)

A marker is an HTML comment on its own line, in the lines before a fenced block. Blank lines may separate the marker and the fence.

| Marker | Meaning |
|---|---|
| `<!-- fragment -->` | Java block is not compilable on purpose. The runner skips it |
| `<!-- preview -->` | Java block uses a Java 21 preview feature |
| `<!-- expected-output -->` | `text` block holds the expected output of the folder's `Main.java` |
| `<!-- sql-skip: <dialect> -->` | SQL block uses a non-SQLite dialect. The runner skips it |
| `<!-- sql-result -->` | `text` block holds the expected result of the previous SQL block |
| `<!-- omit: <Section> -- <reason> -->` | Template section is omitted on purpose (anywhere in the file) |

---

### Task 1: Shared Markdown block parser

**Files:**
- Create: `tools/mdblocks.py`
- Test: `tools/tests/test_mdblocks.py`

**Interfaces:**
- Produces:
  - `Block(path: Path, line: int, lang: str, body: str, markers: dict[str, str])`
  - `parse_blocks(text: str, path: Path = Path("<string>")) -> list[Block]`
  - `front_matter(text: str) -> dict[str, str]`
  - `h2_headings(text: str) -> list[str]` (headings outside code blocks, number prefix removed)
  - `md_files(paths: list[str]) -> list[Path]`

- [ ] **Step 1: Write the failing tests**

```python
# tools/tests/test_mdblocks.py
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from mdblocks import parse_blocks, front_matter, h2_headings  # noqa: E402

DOC = """---
priority: P1
---
# Title

## 1. Why It Matters

<!-- fragment -->

```java
int x = 1;
```

??? question "Q1"

    ```java
    class A {}
    ```

````markdown
```java
not a block
```
````

## 5. Optimization Insight (The "Aha" Moment)
"""


class ParseBlocksTest(unittest.TestCase):
    def test_finds_top_level_block_with_marker(self):
        blocks = parse_blocks(DOC)
        self.assertEqual(blocks[0].lang, "java")
        self.assertEqual(blocks[0].body, "int x = 1;")
        self.assertIn("fragment", blocks[0].markers)

    def test_strips_indent_inside_collapsible(self):
        blocks = parse_blocks(DOC)
        self.assertEqual(blocks[1].body, "class A {}")
        self.assertEqual(blocks[1].markers, {})

    def test_longer_fence_contains_inner_fence(self):
        blocks = parse_blocks(DOC)
        self.assertEqual(len(blocks), 3)
        self.assertEqual(blocks[2].lang, "markdown")
        self.assertIn("not a block", blocks[2].body)

    def test_line_numbers_are_one_based(self):
        self.assertEqual(parse_blocks(DOC)[0].line, 10)


class FrontMatterTest(unittest.TestCase):
    def test_reads_keys(self):
        self.assertEqual(front_matter(DOC), {"priority": "P1"})

    def test_no_front_matter(self):
        self.assertEqual(front_matter("# Title\n"), {})


class HeadingsTest(unittest.TestCase):
    def test_strips_number_prefix_and_ignores_code(self):
        self.assertEqual(
            h2_headings(DOC),
            ["Why It Matters", 'Optimization Insight (The "Aha" Moment)'],
        )


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `python3 -m unittest discover -s tools/tests -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'mdblocks'`

- [ ] **Step 3: Write the implementation**

```python
# tools/mdblocks.py
"""Shared Markdown parsing for the KB checker scripts.

The parser understands fenced code blocks (``` and ~~~, any indent),
HTML-comment markers before a block, YAML-style front matter, and
level-2 headings.
"""
import re
from dataclasses import dataclass, field
from pathlib import Path

FENCE_OPEN = re.compile(r"^(?P<indent>\s*)(?P<fence>`{3,}|~{3,})\s*(?P<lang>[\w+-]*)")
MARKER = re.compile(r"^\s*<!--\s*(?P<name>[\w-]+)(?::\s*(?P<value>.*?))?\s*-->\s*$")
H2 = re.compile(r"^##\s+(?:\d+\.\s+)?(?P<title>.+?)\s*#*\s*$")


@dataclass
class Block:
    path: Path
    line: int
    lang: str
    body: str
    markers: dict = field(default_factory=dict)


def _closes(line: str, fence: str) -> bool:
    stripped = line.strip()
    return (
        len(stripped) >= len(fence)
        and set(stripped) == {fence[0]}
    )


def _markers_before(lines: list[str], index: int) -> dict:
    markers = {}
    k = index - 1
    while k >= 0:
        match = MARKER.match(lines[k])
        if match:
            markers[match.group("name")] = match.group("value") or ""
        elif lines[k].strip():
            break
        k -= 1
    return markers


def _iter_fences(lines: list[str]):
    """Yield (open_index, close_index, indent, lang) for each top-level fence."""
    i = 0
    while i < len(lines):
        match = FENCE_OPEN.match(lines[i])
        if not match:
            i += 1
            continue
        fence = match.group("fence")
        j = i + 1
        while j < len(lines) and not _closes(lines[j], fence):
            j += 1
        yield i, j, match.group("indent"), match.group("lang").lower()
        i = j + 1


def parse_blocks(text: str, path: Path = Path("<string>")) -> list[Block]:
    lines = text.split("\n")
    blocks = []
    for start, end, indent, lang in _iter_fences(lines):
        body = []
        for line in lines[start + 1:end]:
            body.append(line[len(indent):] if line.startswith(indent) else line.lstrip())
        blocks.append(Block(path, start + 1, lang, "\n".join(body), _markers_before(lines, start)))
    return blocks


def front_matter(text: str) -> dict:
    if not text.startswith("---\n"):
        return {}
    end = text.find("\n---", 4)
    if end == -1:
        return {}
    result = {}
    for line in text[4:end].split("\n"):
        if ":" in line:
            key, value = line.split(":", 1)
            result[key.strip()] = value.strip()
    return result


def h2_headings(text: str) -> list[str]:
    lines = text.split("\n")
    inside = set()
    for start, end, _, _ in _iter_fences(lines):
        inside.update(range(start, end + 1))
    titles = []
    for index, line in enumerate(lines):
        if index in inside:
            continue
        match = H2.match(line)
        if match:
            titles.append(match.group("title"))
    return titles


def markers_anywhere(text: str, name: str) -> list[str]:
    values = []
    for line in text.split("\n"):
        match = MARKER.match(line)
        if match and match.group("name") == name:
            values.append(match.group("value") or "")
    return values


def md_files(paths: list[str]) -> list[Path]:
    files = []
    for raw in paths:
        path = Path(raw)
        if path.is_dir():
            files.extend(sorted(path.rglob("*.md")))
        elif path.suffix == ".md":
            files.append(path)
    return files
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `python3 -m unittest discover -s tools/tests -v`
Expected: 7 tests, OK

- [ ] **Step 5: Commit**

```bash
git add tools/mdblocks.py tools/tests/test_mdblocks.py
git commit -m "feat: add shared Markdown block parser for KB checkers"
```

---

### Task 2: Format checker

**Files:**
- Create: `tools/check_format.py`
- Test: `tools/tests/test_check_format.py`

**Interfaces:**
- Consumes: `parse_blocks`, `front_matter`, `h2_headings`, `markers_anywhere`, `md_files` from Task 1.
- Produces:
  - `template_for(path: Path) -> str | None` returning `"topic"`, `"lld-case"`, `"hld-case"`, `"dsa-pattern"`, or `None`
  - `is_legacy(path: Path) -> bool`
  - `check_file(path: Path, text: str) -> list[str]` (error strings `path: message`)
  - CLI: `python3 tools/check_format.py kb` exits 1 on any error.

Path rules (relative to the repo root, POSIX form):

| Template | Path regex |
|---|---|
| `topic` | `kb/(lld/fundamentals\|hld/fundamentals\|hld/distributed-systems\|dsa/foundations\|java\|databases\|networking\|operating-systems\|api-design\|security\|engineering-practice\|behavioral\|ai-engineering)/\d\d-[^/]+\.md` |
| `lld-case` | `kb/lld/case-studies/\d\d-[^/]+/README\.md` |
| `hld-case` | `kb/hld/case-studies/\d\d-[^/]+\.md` |
| `dsa-pattern` | `kb/dsa/(2[1-9]\|[3-9]\d)-[^/]+/README\.md` |

Legacy (no width check, no template check): `kb/dsa/[^/]+\.md` and `kb/dsa/(0\d|1\d|20)-[^/]+/README\.md`.

- [ ] **Step 1: Write the failing tests**

```python
# tools/tests/test_check_format.py
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import check_format as cf  # noqa: E402

TOPIC_SECTIONS = [
    "Why It Matters", "Core Concepts", "How It Works", "Trade-offs and When to Use",
    "Java Example", "Interview Angle", "Common Mistakes", "Commonly Confused With",
    "Quick Reference Card", "Further Reading",
]


def topic_page(priority="P2", questions=4, skip=None, extra=""):
    parts = [f"---\npriority: {priority}\n---\n# T\n"]
    for number, name in enumerate(TOPIC_SECTIONS, 1):
        if name != skip:
            parts.append(f"## {number}. {name}\n\ntext\n")
    parts.append("".join(f'??? question "Q{i}"\n    a\n' for i in range(questions)))
    parts.append(extra)
    return "\n".join(parts)


class TemplateForTest(unittest.TestCase):
    def test_paths(self):
        self.assertEqual(cf.template_for(Path("kb/java/01-language-core.md")), "topic")
        self.assertEqual(cf.template_for(Path("kb/hld/fundamentals/03-caching.md")), "topic")
        self.assertEqual(
            cf.template_for(Path("kb/lld/case-studies/01-parking-lot/README.md")), "lld-case")
        self.assertEqual(
            cf.template_for(Path("kb/hld/case-studies/01-url-shortener.md")), "hld-case")
        self.assertEqual(
            cf.template_for(Path("kb/dsa/21-segment-tree-and-fenwick-tree/README.md")),
            "dsa-pattern")
        self.assertIsNone(cf.template_for(Path("kb/dsa/01-sliding-window/README.md")))
        self.assertIsNone(cf.template_for(Path("kb/java/README.md")))
        self.assertIsNone(cf.template_for(Path("kb/java/interview-question-bank.md")))

    def test_legacy(self):
        self.assertTrue(cf.is_legacy(Path("kb/dsa/cheatsheet.md")))
        self.assertTrue(cf.is_legacy(Path("kb/dsa/20-intervals/README.md")))
        self.assertFalse(cf.is_legacy(Path("kb/dsa/foundations/01-complexity-analysis.md")))
        self.assertFalse(cf.is_legacy(Path("kb/dsa/21-segment-tree-and-fenwick-tree/README.md")))


class CheckFileTest(unittest.TestCase):
    path = Path("kb/java/01-language-core.md")

    def test_complete_page_passes(self):
        self.assertEqual(cf.check_file(self.path, topic_page()), [])

    def test_missing_section_fails(self):
        errors = cf.check_file(self.path, topic_page(skip="Java Example"))
        self.assertEqual(len(errors), 1)
        self.assertIn("Java Example", errors[0])

    def test_omit_marker_allows_missing_section(self):
        text = topic_page(skip="Java Example", extra="<!-- omit: Java Example -- no code -->\n")
        self.assertEqual(cf.check_file(self.path, text), [])

    def test_omit_marker_needs_reason(self):
        text = topic_page(skip="Java Example", extra="<!-- omit: Java Example -->\n")
        self.assertTrue(any("reason" in e for e in cf.check_file(self.path, text)))

    def test_too_few_questions_for_priority(self):
        errors = cf.check_file(self.path, topic_page(priority="P1", questions=7))
        self.assertTrue(any("8" in e for e in errors))

    def test_missing_priority(self):
        text = topic_page().replace("priority: P2", "owner: me")
        self.assertTrue(any("priority" in e for e in cf.check_file(self.path, text)))

    def test_long_code_line_fails(self):
        text = topic_page(extra="```java\n" + "x" * 101 + "\n```\n")
        self.assertTrue(any("101" in e for e in cf.check_file(self.path, text)))

    def test_legacy_file_skips_width(self):
        text = "```\n" + "x" * 150 + "\n```\n"
        self.assertEqual(cf.check_file(Path("kb/dsa/cheatsheet.md"), text), [])

    def test_heading_prefix_match(self):
        names = [
            "Pattern Name", "Core Intuition", "Recognition Signals",
            "Brute Force Thought Process", 'Optimization Insight (The "Aha" Moment)',
            "Generalized Templates", "Variations of the Pattern", "Classic Interview Problems",
            "How to Practice This Pattern Effectively",
            "Common Mistakes Even Experienced Developers Make",
            "Commonly Confused With", "Quick Reference Card",
        ]
        text = "---\npriority: P2\n---\n" + "\n".join(f"## {n}\n" for n in names)
        path = Path("kb/dsa/22-string-algorithms/README.md")
        self.assertEqual(cf.check_file(path, text), [])


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `python3 -m unittest tools/tests/test_check_format.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'check_format'`

- [ ] **Step 3: Write the implementation**

```python
#!/usr/bin/env python3
# tools/check_format.py
"""Check KB pages for code line width, template sections, and question counts.

Usage: python3 tools/check_format.py [PATH ...]   (default: kb)
Exit code 1 if any page fails.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mdblocks import front_matter, h2_headings, markers_anywhere, md_files, parse_blocks  # noqa: E402

MAX_CODE_WIDTH = 100
MIN_QUESTIONS = {"P1": 8, "P2": 4, "P3": 2}

TOPIC_DIRS = (
    "lld/fundamentals|hld/fundamentals|hld/distributed-systems|dsa/foundations|java|"
    "databases|networking|operating-systems|api-design|security|engineering-practice|"
    "behavioral|ai-engineering"
)
TEMPLATE_PATHS = [
    ("topic", re.compile(rf"(^|/)kb/({TOPIC_DIRS})/\d\d-[^/]+\.md$")),
    ("lld-case", re.compile(r"(^|/)kb/lld/case-studies/\d\d-[^/]+/README\.md$")),
    ("hld-case", re.compile(r"(^|/)kb/hld/case-studies/\d\d-[^/]+\.md$")),
    ("dsa-pattern", re.compile(r"(^|/)kb/dsa/(2[1-9]|[3-9]\d)-[^/]+/README\.md$")),
]
LEGACY = re.compile(r"(^|/)kb/dsa/([^/]+\.md|(0\d|1\d|20)-[^/]+/README\.md)$")

SECTIONS = {
    "topic": [
        "Why It Matters", "Core Concepts", "How It Works", "Trade-offs and When to Use",
        "Java Example", "Interview Angle", "Common Mistakes", "Commonly Confused With",
        "Quick Reference Card", "Further Reading",
    ],
    "lld-case": [
        "Problem Statement", "Clarifying Questions", "Requirements",
        "Entities and Relationships", "Class Diagram", "Design Patterns Used", "Key Code",
        "Concurrency", "Extensions", "Common Mistakes", "45-Minute Plan", "Expected Output",
    ],
    "hld-case": [
        "Problem Statement", "Clarifying Questions", "Requirements", "Estimation",
        "API Design", "Data Model", "High-Level Design", "Deep Dives",
        "Bottlenecks and Scaling", "Trade-offs", "Follow-up Questions", "45-Minute Plan",
        "Further Reading",
    ],
    "dsa-pattern": [
        "Pattern Name", "Core Intuition", "Recognition Signals", "Brute Force Thought Process",
        "Optimization Insight", "Generalized Templates", "Variations of the Pattern",
        "Classic Interview Problems", "How to Practice This Pattern Effectively",
        "Common Mistakes", "Commonly Confused With", "Quick Reference Card",
    ],
}
COUNTS_QUESTIONS = {"topic", "hld-case"}
QUESTION = re.compile(r'^\s*\?\?\?\+?\s+question\b', re.MULTILINE)


def _posix(path: Path) -> str:
    return path.as_posix()


def template_for(path: Path):
    text = _posix(path)
    for name, pattern in TEMPLATE_PATHS:
        if pattern.search(text):
            return name
    return None


def is_legacy(path: Path) -> bool:
    return bool(LEGACY.search(_posix(path)))


def _omitted(text: str, path: Path, errors: list) -> set:
    names = set()
    for value in markers_anywhere(text, "omit"):
        name, _, reason = value.partition(" -- ")
        if not reason.strip():
            errors.append(f"{path}: omit marker for '{name.strip()}' has no reason")
        names.add(name.strip().lower())
    return names


def _check_sections(path: Path, text: str, template: str, errors: list) -> None:
    headings = [h.lower() for h in h2_headings(text)]
    omitted = _omitted(text, path, errors)
    for section in SECTIONS[template]:
        wanted = section.lower()
        if wanted in omitted:
            continue
        if not any(h.startswith(wanted) for h in headings):
            errors.append(f"{path}: missing section '## {section}' ({template} template)")


def _check_priority(path: Path, text: str, template: str, errors: list) -> None:
    priority = front_matter(text).get("priority")
    if priority not in MIN_QUESTIONS:
        errors.append(f"{path}: front matter needs 'priority: P1|P2|P3'")
        return
    if template in COUNTS_QUESTIONS:
        found = len(QUESTION.findall(text))
        needed = MIN_QUESTIONS[priority]
        if found < needed:
            errors.append(
                f"{path}: {priority} page needs {needed} '??? question' blocks, found {found}")


def check_file(path: Path, text: str) -> list:
    errors = []
    if is_legacy(path):
        return errors
    for block in parse_blocks(text, path):
        for offset, line in enumerate(block.body.split("\n"), 1):
            if len(line) > MAX_CODE_WIDTH:
                errors.append(
                    f"{path}:{block.line + offset}: code line has {len(line)} characters "
                    f"(max {MAX_CODE_WIDTH})")
    template = template_for(path)
    if template:
        _check_sections(path, text, template, errors)
        _check_priority(path, text, template, errors)
    return errors


def main(argv: list) -> int:
    paths = argv or ["kb"]
    errors = []
    files = md_files(paths)
    for path in files:
        errors.extend(check_file(path, path.read_text(encoding="utf-8")))
    for error in errors:
        print(error)
    print(f"format: {len(files)} files checked, {len(errors)} errors")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `python3 -m unittest discover -s tools/tests -v`
Expected: all tests OK (Task 1 and Task 2)

- [ ] **Step 5: Commit**

```bash
git add tools/check_format.py tools/tests/test_check_format.py
git commit -m "feat: add format checker for KB page templates and code width"
```

---

### Task 3: Java runner

**Files:**
- Create: `tools/run_java.py`, `tools/java-support/ListNode.java`, `tools/java-support/TreeNode.java`
- Test: `tools/tests/test_run_java.py`

**Interfaces:**
- Consumes: `parse_blocks`, `md_files`, `Block` from Task 1.
- Produces:
  - `release_for(path: Path) -> str` (`"17"` for legacy DSA, else `"21"`)
  - `candidates(body: str) -> list[tuple[str, str]]` (file name, source) in try order
  - `compile_block(block: Block, javac: Path, support_cp: Path) -> str | None` (None = success, else the error text)
  - `run_program(main_java: Path, java: Path) -> str | None` (None = output matches)
  - CLI: `python3 tools/run_java.py kb`
- Environment: `KB_JDK21` overrides the JDK home (default `~/.jenv/versions/21`).

- [ ] **Step 1: Write the failing tests**

```python
# tools/tests/test_run_java.py
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import run_java as rj  # noqa: E402
from mdblocks import Block  # noqa: E402


class CandidatesTest(unittest.TestCase):
    def test_public_class_uses_its_name_first(self):
        names = [name for name, _ in rj.candidates("public class Foo {}")]
        self.assertEqual(names[0], "Foo.java")

    def test_imports_stay_on_top_when_wrapped(self):
        body = "import java.math.BigInteger;\nBigInteger x = BigInteger.ONE;"
        wrapped = [src for name, src in rj.candidates(body) if name == "Snippet.java"]
        for source in wrapped:
            self.assertLess(source.index("import java.math.BigInteger;"), source.index("class Snippet"))


class ReleaseTest(unittest.TestCase):
    def test_release(self):
        self.assertEqual(rj.release_for(Path("kb/dsa/java-toolkit.md")), "17")
        self.assertEqual(rj.release_for(Path("kb/dsa/05-hashing-frequency/README.md")), "17")
        self.assertEqual(rj.release_for(Path("kb/java/10-virtual-threads.md")), "21")


@unittest.skipUnless(rj.JDK.joinpath("bin", "javac").exists(), "JDK 21 not installed")
class CompileTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.support = rj.build_support(rj.JDK / "bin" / "javac")

    def compile(self, body, path="kb/java/x.md", markers=None):
        block = Block(Path(path), 1, "java", body, markers or {})
        return rj.compile_block(block, rj.JDK / "bin" / "javac", self.support)

    def test_member_snippet_compiles(self):
        self.assertIsNone(self.compile("int twice(int x) { return 2 * x; }"))

    def test_statement_snippet_compiles(self):
        self.assertIsNone(self.compile("List<Integer> xs = new ArrayList<>();\nxs.add(1);"))

    def test_tree_node_is_available(self):
        self.assertIsNone(self.compile("int depth(TreeNode n) { return n == null ? 0 : 1; }"))

    def test_record_needs_release_21(self):
        self.assertIsNone(self.compile("record P(int x) {}"))
        self.assertIsNotNone(self.compile("record P(int x) {}", path="kb/dsa/cheatsheet.md"))

    def test_broken_code_reports_error(self):
        self.assertIn("error", self.compile("int x = ;"))


@unittest.skipUnless(rj.JDK.joinpath("bin", "java").exists(), "JDK 21 not installed")
class RunProgramTest(unittest.TestCase):
    def write_case(self, expected):
        folder = Path(tempfile.mkdtemp())
        (folder / "Main.java").write_text(
            'public class Main { public static void main(String[] a) '
            '{ System.out.println("hi"); } }')
        (folder / "README.md").write_text(
            f"## Expected Output\n\n<!-- expected-output -->\n```text\n{expected}\n```\n")
        return folder / "Main.java"

    def test_matching_output(self):
        self.assertIsNone(rj.run_program(self.write_case("hi"), rj.JDK / "bin" / "java"))

    def test_different_output(self):
        self.assertIn("differs", rj.run_program(self.write_case("bye"), rj.JDK / "bin" / "java"))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `python3 -m unittest tools/tests/test_run_java.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'run_java'`

- [ ] **Step 3: Write the support classes**

```java
// tools/java-support/ListNode.java
public class ListNode {
    public int val;
    public ListNode next;

    public ListNode() {}

    public ListNode(int val) { this.val = val; }

    public ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}
```

```java
// tools/java-support/TreeNode.java
public class TreeNode {
    public int val;
    public TreeNode left;
    public TreeNode right;

    public TreeNode() {}

    public TreeNode(int val) { this.val = val; }

    public TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}
```

- [ ] **Step 4: Write the runner**

```python
#!/usr/bin/env python3
# tools/run_java.py
"""Compile every ```java block in the KB and run every case-study Main.java.

Usage: python3 tools/run_java.py [PATH ...]   (default: kb)

A block compiles if one of these forms compiles:
  1. the block as a file (named after its public type, else Snippet.java);
  2. the block as members of `class Snippet`;
  3. the block as statements inside a method of `class Snippet`.
Blocks after a `<!-- fragment -->` marker are skipped.
Each Main.java runs with `java Main.java`. Its output must equal the `text`
block after `<!-- expected-output -->` in the README.md of the same folder.
"""
import os
import re
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mdblocks import Block, md_files, parse_blocks  # noqa: E402

JDK = Path(os.environ.get("KB_JDK21", Path.home() / ".jenv" / "versions" / "21"))
SUPPORT_DIR = Path(__file__).resolve().parent / "java-support"
LEGACY = re.compile(r"(^|/)kb/dsa/([^/]+\.md|(0\d|1\d|20)-[^/]+/README\.md)$")
PUBLIC_TYPE = re.compile(
    r"^public\s+(?:(?:final|abstract|sealed|non-sealed|static)\s+)*"
    r"(?:class|interface|enum|record|@interface)\s+(\w+)", re.MULTILINE)
IMPORT_LINE = re.compile(r"^\s*import\s+[\w.*]+\s*;\s*$", re.MULTILINE)
IMPORTS = "\n".join(f"import {p}.*;" for p in [
    "java.util", "java.util.function", "java.util.stream", "java.util.concurrent",
    "java.util.concurrent.atomic", "java.util.concurrent.locks", "java.io", "java.nio.file",
    "java.time", "java.math",
])
ENV = {k: v for k, v in os.environ.items() if k != "JAVA_TOOL_OPTIONS"}


def release_for(path: Path) -> str:
    return "17" if LEGACY.search(path.as_posix()) else "21"


def candidates(body: str) -> list:
    forms = []
    public = PUBLIC_TYPE.search(body)
    forms.append((f"{public.group(1)}.java" if public else "Snippet.java", IMPORTS + "\n" + body))
    own_imports = "\n".join(m.group(0).strip() for m in IMPORT_LINE.finditer(body))
    rest = IMPORT_LINE.sub("", body)
    head = IMPORTS + "\n" + own_imports + "\n"
    forms.append(("Snippet.java", head + "class Snippet {\n" + rest + "\n}\n"))
    forms.append((
        "Snippet.java",
        head + "class Snippet {\nstatic void run() throws Exception {\n" + rest + "\n}\n}\n"))
    return forms


def _javac(javac: Path, release: str, preview: bool, files: list, out: Path, cp: Path = None):
    command = [str(javac), "--release", release, "-nowarn", "-Xlint:none", "-proc:none",
               "-d", str(out)]
    if preview:
        command.append("--enable-preview")
    if cp:
        command += ["-cp", str(cp)]
    command += [str(f) for f in files]
    return subprocess.run(command, capture_output=True, text=True, env=ENV)


def build_support(javac: Path) -> Path:
    out = Path(tempfile.mkdtemp(prefix="kb-java-support-"))
    result = _javac(javac, "17", False, sorted(SUPPORT_DIR.glob("*.java")), out)
    if result.returncode != 0:
        raise RuntimeError(result.stderr)
    return out


def compile_block(block: Block, javac: Path, support_cp: Path):
    release = release_for(block.path)
    preview = "preview" in block.markers
    if preview:
        release = "21"
    first_error = None
    for name, source in candidates(block.body):
        work = Path(tempfile.mkdtemp(prefix="kb-java-"))
        source_file = work / name
        source_file.write_text(source, encoding="utf-8")
        result = _javac(javac, release, preview, [source_file], work / "out", support_cp)
        if result.returncode == 0:
            return None
        if first_error is None:
            first_error = result.stderr or result.stdout
    return first_error


def _expected_output(readme: Path):
    if not readme.exists():
        return None
    for block in parse_blocks(readme.read_text(encoding="utf-8"), readme):
        if "expected-output" in block.markers:
            return block.body
    return None


def _normalize(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.strip("\n").split("\n"))


def run_program(main_java: Path, java: Path):
    expected = _expected_output(main_java.parent / "README.md")
    if expected is None:
        return f"{main_java}: README.md has no '<!-- expected-output -->' text block"
    try:
        result = subprocess.run([str(java), "Main.java"], cwd=main_java.parent,
                                capture_output=True, text=True, timeout=60, env=ENV)
    except subprocess.TimeoutExpired:
        return f"{main_java}: did not finish in 60 seconds"
    if result.returncode != 0:
        return f"{main_java}: exit code {result.returncode}\n{result.stderr}"
    if _normalize(result.stdout) != _normalize(expected):
        return (f"{main_java}: output differs from README expected output\n"
                f"--- actual ---\n{result.stdout}")
    return None


def main(argv: list) -> int:
    paths = argv or ["kb"]
    javac, java = JDK / "bin" / "javac", JDK / "bin" / "java"
    support = build_support(javac)
    blocks = [b for f in md_files(paths)
              for b in parse_blocks(f.read_text(encoding="utf-8"), f) if b.lang == "java"]
    skipped = [b for b in blocks if "fragment" in b.markers]
    to_compile = [b for b in blocks if "fragment" not in b.markers]
    with ThreadPoolExecutor(max_workers=8) as pool:
        errors = list(pool.map(lambda b: compile_block(b, javac, support), to_compile))
    failures = 0
    for block, error in zip(to_compile, errors):
        if error:
            failures += 1
            short = "\n".join(error.strip().split("\n")[:12])
            print(f"{block.path}:{block.line}: java block does not compile "
                  f"(release {release_for(block.path)})\n{short}\n")
    programs = sorted(p for raw in paths for p in Path(raw).rglob("Main.java"))
    program_failures = 0
    for program in programs:
        error = run_program(program, java)
        if error:
            program_failures += 1
            print(error + "\n")
    print(f"java: {len(to_compile) - failures}/{len(to_compile)} blocks compile, "
          f"{len(skipped)} fragments skipped, "
          f"{len(programs) - program_failures}/{len(programs)} programs match")
    return 1 if failures or program_failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `python3 -m unittest discover -s tools/tests -v`
Expected: all tests OK

- [ ] **Step 6: Commit**

```bash
git add tools/run_java.py tools/java-support tools/tests/test_run_java.py
git commit -m "feat: add Java runner that compiles KB code blocks and runs case studies"
```

---

### Task 4: SQL runner

**Files:**
- Create: `tools/run_sql.py`
- Test: `tools/tests/test_run_sql.py`

**Interfaces:**
- Consumes: `parse_blocks`, `md_files` from Task 1.
- Produces:
  - `check_text(text: str, path: Path) -> list[str]`
  - `format_rows(description, rows) -> str` (header line, then one line per row, cells joined by ` | `, `None` as `NULL`)
  - CLI: `python3 tools/run_sql.py kb`

Rules: each Markdown file gets one in-memory SQLite database. SQL blocks run in file order. A block with `sql-skip` is skipped. A `text` block with `sql-result` must equal the formatted result of the last row-returning statement of the previous SQL block.

- [ ] **Step 1: Write the failing tests**

```python
# tools/tests/test_run_sql.py
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import run_sql as rs  # noqa: E402

SETUP = "```sql\nCREATE TABLE t(id INTEGER, name TEXT);\nINSERT INTO t VALUES (1,'a'),(2,NULL);\n```\n"


class RunSqlTest(unittest.TestCase):
    def test_matching_result(self):
        text = SETUP + (
            "```sql\nSELECT id, name FROM t ORDER BY id;\n```\n"
            "<!-- sql-result -->\n```text\nid | name\n1 | a\n2 | NULL\n```\n")
        self.assertEqual(rs.check_text(text, Path("x.md")), [])

    def test_wrong_result(self):
        text = SETUP + (
            "```sql\nSELECT id FROM t ORDER BY id;\n```\n"
            "<!-- sql-result -->\n```text\nid\n9\n```\n")
        self.assertEqual(len(rs.check_text(text, Path("x.md"))), 1)

    def test_syntax_error_without_result_block(self):
        text = SETUP + "```sql\nSELEC id FROM t;\n```\n"
        errors = rs.check_text(text, Path("x.md"))
        self.assertEqual(len(errors), 1)
        self.assertIn("syntax error", errors[0])

    def test_skip_marker(self):
        text = "<!-- sql-skip: postgres -->\n```sql\nSELECT now()::date;\n```\n"
        self.assertEqual(rs.check_text(text, Path("x.md")), [])


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `python3 -m unittest tools/tests/test_run_sql.py -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'run_sql'`

- [ ] **Step 3: Write the implementation**

```python
#!/usr/bin/env python3
# tools/run_sql.py
"""Run the ```sql blocks of each KB page on SQLite and compare results.

Usage: python3 tools/run_sql.py [PATH ...]   (default: kb)
"""
import sqlite3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mdblocks import md_files, parse_blocks  # noqa: E402


def format_rows(description, rows) -> str:
    lines = [" | ".join(column[0] for column in description)]
    for row in rows:
        lines.append(" | ".join("NULL" if value is None else str(value) for value in row))
    return "\n".join(lines)


def _statements(body: str):
    buffer = ""
    for line in body.split("\n"):
        buffer += line + "\n"
        if sqlite3.complete_statement(buffer):
            yield buffer.strip()
            buffer = ""
    if buffer.strip():
        yield buffer.strip()


def check_text(text: str, path: Path) -> list:
    errors = []
    connection = sqlite3.connect(":memory:")
    last_result = None
    for block in parse_blocks(text, path):
        if block.lang == "sql":
            if "sql-skip" in block.markers:
                last_result = None
                continue
            last_result = None
            for statement in _statements(block.body):
                try:
                    cursor = connection.execute(statement)
                except sqlite3.Error as error:
                    errors.append(f"{path}:{block.line}: {error}")
                    break
                if cursor.description:
                    last_result = format_rows(cursor.description, cursor.fetchall())
        elif "sql-result" in block.markers:
            expected = "\n".join(line.rstrip() for line in block.body.strip().split("\n"))
            if last_result is None:
                errors.append(f"{path}:{block.line}: sql-result block has no query before it")
            elif last_result != expected:
                errors.append(
                    f"{path}:{block.line}: SQL result differs\n--- actual ---\n{last_result}")
    connection.close()
    return errors


def main(argv: list) -> int:
    paths = argv or ["kb"]
    errors = []
    pages = 0
    for path in md_files(paths):
        text = path.read_text(encoding="utf-8")
        if "```sql" not in text:
            continue
        pages += 1
        errors.extend(check_text(text, path))
    for error in errors:
        print(error)
    print(f"sql: {pages} pages with SQL, {len(errors)} errors")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `python3 -m unittest discover -s tools/tests -v`
Expected: all tests OK

- [ ] **Step 5: Commit**

```bash
git add tools/run_sql.py tools/tests/test_run_sql.py
git commit -m "feat: add SQL runner that checks KB queries against SQLite"
```

---

### Task 5: Move the DSA content and add the site

**Files:**
- Move: every DSA file and folder → `kb/dsa/` (with `git mv`)
- Create: `kb/README.md`, `kb/study-plan.md`, `kb/glossary.md`, `kb/.nav.yml`, `kb/dsa/.nav.yml`, `kb/<subject>/README.md` for the 11 planned subjects, `README.md`, `mkdocs.yml`, `requirements.txt`, `tools/setup-venv.sh`, `tools/serve.sh`, `tools/check-all.sh`
- Modify: `.gitignore`, `kb/dsa/README.md` (anchor fix `#-pattern-recognition-matrix` → `#pattern-recognition-matrix`)

**Interfaces:**
- Consumes: the tools from Tasks 1–4.
- Produces: `tools/check-all.sh` (the exit gate for every phase), `.venv/` with MkDocs.

- [ ] **Step 1: Move the content**

```bash
mkdir -p kb/dsa
git mv [0-2][0-9]-* kb/dsa/
git mv cheatsheet.md common-mistakes.md company-prep.md how-to-choose-a-pattern.md \
  interview-tips.md java-toolkit.md mock-interviews.md pattern-combinations.md \
  practice-solutions.md README.md study-plan.md tricks-at-a-glance.md kb/dsa/
git status --short | head
```

Expected: only `R` (rename) lines.

- [ ] **Step 2: Write `requirements.txt`, `mkdocs.yml`, and the scripts**

```text
# requirements.txt
mkdocs==1.6.1
mkdocs-material==9.7.7
mkdocs-awesome-nav==3.3.0
pymdown-extensions==12.2
```

```yaml
# mkdocs.yml
site_name: Software Engineering Knowledge Base
site_description: Interview preparation and engineering fundamentals
docs_dir: kb
site_dir: site
theme:
  name: material
  features:
    - navigation.tabs
    - navigation.indexes
    - navigation.top
    - navigation.footer
    - navigation.tracking
    - search.suggest
    - search.highlight
    - content.code.copy
    - content.tabs.link
    - toc.follow
  palette:
    - media: "(prefers-color-scheme: light)"
      scheme: default
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode
    - media: "(prefers-color-scheme: dark)"
      scheme: slate
      toggle:
        icon: material/brightness-4
        name: Switch to light mode
plugins:
  - search
  - awesome-nav
markdown_extensions:
  - admonition
  - attr_list
  - def_list
  - footnotes
  - md_in_html
  - tables
  - toc:
      permalink: true
  - pymdownx.details
  - pymdownx.highlight:
      anchor_linenums: true
  - pymdownx.inlinehilite
  - pymdownx.superfences:
      custom_fences:
        - name: mermaid
          class: mermaid
          format: !!python/name:pymdownx.superfences.fence_code_format
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.tasklist:
      custom_checkbox: true
validation:
  omitted_files: warn
  absolute_links: warn
  unrecognized_links: warn
  anchors: warn
  not_found: warn
```

```bash
#!/usr/bin/env bash
# tools/setup-venv.sh: create .venv with the pinned site packages.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -x .venv/bin/mkdocs ]; then
  python3 -m venv .venv
  .venv/bin/pip install --quiet --disable-pip-version-check -r requirements.txt
fi
```

```bash
#!/usr/bin/env bash
# tools/serve.sh: open the KB site at http://127.0.0.1:8000
set -euo pipefail
cd "$(dirname "$0")/.."
tools/setup-venv.sh
exec .venv/bin/mkdocs serve
```

```bash
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
```

`.gitignore` adds:

```text
.venv/
site/
__pycache__/
```

- [ ] **Step 3: Write the navigation files**

```yaml
# kb/.nav.yml
nav:
  - Home: README.md
  - study-plan.md
  - glossary.md
  - DSA: dsa
  - LLD: lld
  - HLD: hld
  - Java: java
  - Databases: databases
  - Networking: networking
  - Operating Systems: operating-systems
  - API Design: api-design
  - Security: security
  - Engineering Practice: engineering-practice
  - Behavioral: behavioral
  - AI Engineering: ai-engineering
  - "*"
```

`kb/dsa/.nav.yml` lists the DSA README first, the reference files in study order, then the patterns with titles taken from each pattern's H1, then `"*"`:

```yaml
# kb/dsa/.nav.yml
nav:
  - Overview: README.md
  - how-to-choose-a-pattern.md
  - java-toolkit.md
  - study-plan.md
  - Patterns:
      - "01 · Sliding Window": 01-sliding-window
      - "02 · Two Pointers": 02-two-pointers
      - "03 · Fast & Slow Pointers": 03-fast-slow-pointers
      - "04 · Prefix Sum & Difference Array": 04-prefix-sum
      - "05 · Hashing & Frequency Maps": 05-hashing-frequency
      - "06 · Binary Search": 06-binary-search-on-answer
      - "07 · Monotonic Stack & Queue": 07-monotonic-stack-queue
      - "08 · Recursion & Backtracking": 08-recursion-backtracking
      - "09 · Divide and Conquer": 09-divide-and-conquer
      - "10 · Tree DFS / BFS": 10-tree-patterns
      - "11 · Graph Traversal": 11-graph-traversal
      - "12 · Topological Sort": 12-topological-sort
      - "13 · Union Find": 13-union-find
      - "14 · Greedy": 14-greedy
      - "15 · Dynamic Programming": 15-dynamic-programming
      - "16 · Heap / Priority Queue": 16-heap-priority-queue
      - "17 · Trie": 17-trie
      - "18 · Shortest Paths": 18-shortest-paths
      - "19 · Bit Manipulation": 19-bit-manipulation
      - "20 · Intervals & Sweep Line": 20-intervals
  - Reference:
      - cheatsheet.md
      - tricks-at-a-glance.md
      - pattern-combinations.md
      - practice-solutions.md
      - common-mistakes.md
  - Interview:
      - interview-tips.md
      - mock-interviews.md
      - company-prep.md
  - "*"
```

- [ ] **Step 4: Write the home page, the skeletons, and the subject index pages**

`kb/README.md` contains: what the KB is; how to read it (site, search, collapsible answers); a table of subjects with priority and status (DSA: complete; others: planned, with phase number); a reading order for interview preparation; a reading order for the job. Links go only to pages that exist.

`kb/study-plan.md` contains: the link to `dsa/study-plan.md`, and a table of the tracks that later phases add (LLD, HLD, Java, fundamentals), each with the phase that writes it.

`kb/glossary.md` contains: the entry format (`**Term**: definition. See [page](link).`), the rule "one term per concept", and sections A–Z that later phases fill.

Each `kb/<subject>/README.md` (lld, hld, java, databases, networking, operating-systems, api-design, security, engineering-practice, behavioral, ai-engineering) contains: an H1 with the subject name; a sentence that states the status ("Planned for phase N"); and the planned page list from spec section 3 as a table (file, content, priority) with no links.

`README.md` (repo root) contains: what the repo is, `tools/serve.sh` to open the site, `tools/check-all.sh` to run every check, and the layout of `kb/`, `tools/`, and `docs/`.

- [ ] **Step 5: Fix the known anchor and build the site**

```bash
sed -i '' 's/#-pattern-recognition-matrix/#pattern-recognition-matrix/' kb/dsa/README.md
tools/setup-venv.sh
.venv/bin/mkdocs build --strict
```

Expected: `Documentation built`. If strict mode reports another link or anchor, fix the link in the source file and build again.

- [ ] **Step 6: Run the full gate on the existing content**

Run: `tools/check-all.sh`

Expected: unit tests OK; format OK (legacy files exempt); SQL: 0 pages; mkdocs OK. The Java runner may report legacy blocks that do not compile. Go to Task 6 for those. Do not commit a red gate except in Task 6 Step 1.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "chore: move DSA guide into kb/ and add MkDocs site

The repo becomes a multi-subject knowledge base. Content lives under kb/
so MkDocs can build it; subject index pages list the planned topics."
```

---

### Task 6: Baseline the existing Java blocks

**Files:**
- Modify: `kb/dsa/**` (fragment markers or small compile fixes only)

- [ ] **Step 1: List the failures**

Run: `python3 tools/run_java.py kb > "$TMPDIR/java-baseline.txt"; tail -1 "$TMPDIR/java-baseline.txt"`

- [ ] **Step 2: Triage each failure**

For each failing block, read the error and the block:
- A real bug in the code (wrong type, missing semicolon, wrong method name): fix the code. The memory note says all DSA code was checked, so a real bug is rare and must be reported in the phase summary.
- The block refers to a type that the page defines in another block (for example `Node`, `Interval`, `Trie`): add `<!-- fragment -->` before the block.
- The block is a signature list or an API table written as Java: add `<!-- fragment -->`.

- [ ] **Step 3: Run the full gate**

Run: `tools/check-all.sh`
Expected: exit code 0.

- [ ] **Step 4: Commit**

```bash
git add kb/dsa
git commit -m "fix: mark non-compilable DSA snippets and fix real compile errors"
```

---

### Task 7: Drafting brief, spec sync, and memory

**Files:**
- Create: `docs/superpowers/drafting-brief.md`
- Modify: `docs/superpowers/specs/2026-10-14-software-engineering-kb-design.md` (sections 5.2, 8.1, 8.4)
- Modify: project memory `project_dsa_guide.md` and `MEMORY.md`

- [ ] **Step 1: Write the drafting brief**

The brief is the text Opus pastes into each Sonnet subagent prompt. It contains, in this order:
1. Role: "You draft one or more pages of a software engineering KB. A reviewer checks every claim."
2. The writing rules (spec section 7), verbatim.
3. The laptop reading rules (spec section 6), verbatim.
4. The page templates (spec section 5), verbatim, with the exact `##` heading names that `check_format.py` requires.
5. The front matter rule (`priority: P1|P2|P3`) and the question minimums.
6. The marker conventions table from this plan.
7. Verification duties: compile every Java block with `python3 tools/run_java.py <file>`; run `python3 tools/check_format.py <file>`; run `python3 tools/run_sql.py <file>`; for each number or "always/never" claim, name a primary source in Further Reading; check version-dependent facts on the web.
8. The overlap boundaries (spec section 4), verbatim.
9. Output rules: write only the assigned files; do not commit; finish with a review note that lists the sources used, each omitted section with its reason, and each claim the drafter could not verify.

- [ ] **Step 2: Sync the spec**

- Section 5.2: add item 12, "Expected Output: the `text` block after `<!-- expected-output -->`".
- Section 8.1: replace `tools/run-java.sh` with `tools/run_java.py`. Add the support classes `ListNode` and `TreeNode`.
- Section 8.4: replace the table with: `check_format.py` (width, template sections, front matter, question minimums), `run_java.py`, `run_sql.py`, `mkdocs build --strict` (links and anchors through MkDocs validation), and `tools/check-all.sh` (all of these).

- [ ] **Step 3: Update the project memory**

Update `project_dsa_guide.md`: the content root is now `kb/`; the DSA files are under `kb/dsa/`; the site and the gate commands; the spec and plan paths; phase status. Update its line in `MEMORY.md`.

- [ ] **Step 4: Run the gate and commit**

```bash
tools/check-all.sh
git add docs
git commit -m "docs: add drafting brief and sync spec with phase 0 tooling"
```
