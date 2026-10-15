#!/usr/bin/env python3
"""Check KB pages for code line width, template sections, and question counts.

Usage: python3 tools/check_format.py [PATH ...]   (default: kb)
Exit code 1 if any page fails.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mdblocks import (  # noqa: E402
    front_matter, h2_headings, markers_anywhere, md_files, parse_blocks, prose_lines)

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
QUESTION = re.compile(r'^\s*\?\?\?\+?\s+question\b')
PLACEHOLDER = re.compile(r"@@[^@\n]*@@|\b(TODO|TBD|FIXME)\b")
QUESTION_BANK = re.compile(r"(^|/)kb/.+question-bank\.md$")
QUESTION_BANK_MINIMUM = 25
NO_PRIORITY = re.compile(r"(^|/)kb/(README|glossary|study-plan)\.md$|(^|/)README\.md$")


def _posix(path: Path) -> str:
    return path.resolve().as_posix() if not path.is_absolute() else path.as_posix()


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
    last_index, last_name = -1, None
    for section in SECTIONS[template]:
        wanted = section.lower()
        if wanted in omitted:
            continue
        found = [i for i, h in enumerate(headings) if h.startswith(wanted)]
        if not found:
            errors.append(f"{path}: missing section '## {section}' ({template} template)")
            continue
        index = next((i for i in found if i > last_index), None)
        if index is None:
            errors.append(f"{path}: section '## {section}' is out of order "
                          f"(it must come after '## {last_name}')")
            continue
        last_index, last_name = index, section


def _check_key_code(path: Path, text: str, errors: list) -> None:
    lines = text.split("\n")
    start = next((i for i, l in enumerate(lines) if re.match(r"^##\s+(\d+\.\s+)?Key Code", l)), None)
    if start is None:
        return
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## ")), len(lines))
    section = "\n".join(lines[start:end])
    if not any(b.lang == "java" for b in parse_blocks(section, path)):
        errors.append(f"{path}: '## Key Code' has no java block")


def _count_questions(text: str) -> int:
    return sum(1 for line in prose_lines(text) if QUESTION.match(line))


def _check_priority(path: Path, text: str, template, errors: list) -> None:
    priority = front_matter(text).get("priority")
    if priority not in MIN_QUESTIONS:
        errors.append(f"{path}: front matter needs 'priority: P1|P2|P3'")
        return
    if template in COUNTS_QUESTIONS:
        needed = MIN_QUESTIONS[priority]
    elif QUESTION_BANK.search(_posix(path)):
        needed = QUESTION_BANK_MINIMUM
    else:
        return
    found = _count_questions(text)
    if found < needed:
        errors.append(
            f"{path}: page needs {needed} '??? question' blocks ({priority}), found {found}")


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
    for line in prose_lines(text):
        if PLACEHOLDER.search(line):
            errors.append(f"{path}: unfilled placeholder in text: {line.strip()[:80]}")
    template = template_for(path)
    if template == "lld-case":
        _check_key_code(path, text, errors)
    if template:
        _check_sections(path, text, template, errors)
    if template or not NO_PRIORITY.search(_posix(path)):
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
