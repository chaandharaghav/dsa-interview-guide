#!/usr/bin/env python3
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



def _find_jdk() -> Path:
    """Return the first JDK 21 home that has javac. KB_JDK21 overrides the search."""
    options = [os.environ.get("KB_JDK21")]
    options += [str(Path.home() / ".jenv" / "versions" / v) for v in ("21.0", "21.0.7", "21")]
    for option in options:
        if option and Path(option, "bin", "javac").exists():
            return Path(option)
    return Path(options[1])


JDK = _find_jdk()
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
