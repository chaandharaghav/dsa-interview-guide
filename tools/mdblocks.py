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
    return len(stripped) >= len(fence) and set(stripped) == {fence[0]}


def _markers_before(lines: list, index: int) -> dict:
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


def _iter_fences(lines: list):
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


def parse_blocks(text: str, path: Path = Path("<string>")) -> list:
    lines = text.split("\n")
    blocks = []
    for start, end, indent, lang in _iter_fences(lines):
        body = []
        for line in lines[start + 1:end]:
            body.append(line[len(indent):] if line.startswith(indent) else line.lstrip())
        blocks.append(
            Block(path, start + 1, lang, "\n".join(body), _markers_before(lines, start)))
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


def h2_headings(text: str) -> list:
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


def markers_anywhere(text: str, name: str) -> list:
    values = []
    for line in text.split("\n"):
        match = MARKER.match(line)
        if match and match.group("name") == name:
            values.append(match.group("value") or "")
    return values


def md_files(paths: list) -> list:
    files = []
    for raw in paths:
        path = Path(raw)
        if path.is_dir():
            files.extend(sorted(path.rglob("*.md")))
        elif path.suffix == ".md":
            files.append(path)
    return files
