#!/usr/bin/env python3
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
