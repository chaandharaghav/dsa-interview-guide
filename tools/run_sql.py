#!/usr/bin/env python3
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
