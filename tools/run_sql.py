#!/usr/bin/env python3
"""Run the sql blocks of each KB page on SQLite and compare results.

Usage: python3 tools/run_sql.py [PATH ...]   (default: kb)

The runner uses the sqlite3 command-line tool, because the sqlite3 module that
Python links can be older (3.35 on this machine) than the tool. KB_SQLITE
overrides the tool path. The tool must be SQLite 3.39 or newer (RIGHT and FULL
OUTER JOIN).

Each page gets a fresh in-memory database. SQL blocks run in page order. A block
after a `<!-- sql-skip: <dialect> -->` marker is skipped. A `text` block after
a `<!-- sql-result -->` marker must equal the output of the SQL block before it:
a header line, then one line per row, cells joined by " | ", NULL for null.
"""
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mdblocks import md_files, parse_blocks  # noqa: E402

MIN_VERSION = (3, 39, 0)
SETTINGS = ".bail on\n.headers on\n.mode list\n.separator \" | \"\n.nullvalue NULL\n"


def sqlite_tool() -> str:
    return os.environ.get("KB_SQLITE") or shutil.which("sqlite3") or "sqlite3"


def sqlite_version(tool: str):
    result = subprocess.run([tool, "--version"], capture_output=True, text=True)
    match = re.match(r"(\d+)\.(\d+)\.(\d+)", result.stdout)
    return tuple(int(part) for part in match.groups()) if match else None


def _run(tool: str, history: list, body: str):
    script = SETTINGS + ".output /dev/null\n"
    script += "\n".join(history) + "\n.output stdout\n" + body + "\n"
    result = subprocess.run([tool, "-batch", ":memory:"], input=script,
                            capture_output=True, text=True, timeout=30)
    return result.returncode, result.stdout, result.stderr


def _normalize(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.strip("\n").split("\n"))


def check_text(text: str, path: Path, tool: str = None) -> list:
    tool = tool or sqlite_tool()
    errors, history, last_result = [], [], None
    for block in parse_blocks(text, path):
        if block.lang == "sql":
            last_result = None
            if "sql-skip" in block.markers:
                continue
            code, out, err = _run(tool, history, block.body)
            if code != 0 or err.strip():
                errors.append(f"{path}:{block.line}: {err.strip() or 'sqlite3 failed'}")
                continue
            history.append(block.body)
            last_result = _normalize(out)
        elif "sql-result" in block.markers:
            expected = _normalize(block.body)
            if last_result is None:
                errors.append(f"{path}:{block.line}: sql-result block has no passing query "
                              f"before it")
            elif last_result != expected:
                errors.append(
                    f"{path}:{block.line}: SQL result differs\n--- actual ---\n{last_result}")
    return errors


def main(argv: list) -> int:
    tool = sqlite_tool()
    version = sqlite_version(tool)
    if version is None or version < MIN_VERSION:
        print(f"sql: {tool} is missing or older than 3.39 (found {version})")
        return 2
    paths = argv or ["kb"]
    errors, pages = [], 0
    for path in md_files(paths):
        text = path.read_text(encoding="utf-8")
        if not any(b.lang == "sql" for b in parse_blocks(text, path)):
            continue
        pages += 1
        errors.extend(check_text(text, path, tool))
    for error in errors:
        print(error)
    print(f"sql: {pages} pages with SQL, {len(errors)} errors "
          f"(sqlite {'.'.join(map(str, version))})")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
