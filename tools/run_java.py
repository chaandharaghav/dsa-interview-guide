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
import shutil
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
IMPORT_LINE = re.compile(r"^\s*import\s+(?:static\s+)?[\w.*]+\s*;\s*$", re.MULTILINE)
IMPORTS = "\n".join(f"import {p}.*;" for p in [
    "java.util", "java.util.function", "java.util.stream", "java.util.concurrent",
    "java.util.concurrent.atomic", "java.util.concurrent.locks", "java.io", "java.nio.file",
    "java.time", "java.math",
])
FORM_NAMES = ["as a file", "as class members", "as method statements"]
ENV = {k: v for k, v in os.environ.items() if k != "JAVA_TOOL_OPTIONS"}


def _posix(path: Path) -> str:
    return path.as_posix() if path.is_absolute() else path.resolve().as_posix()


def release_for(path: Path) -> str:
    return "17" if LEGACY.search(_posix(path)) else "21"


def release_of(block: Block) -> str:
    return "21" if "preview" in block.markers else release_for(block.path)


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


def _javac(javac: Path, release: str, preview: bool, files: list, out: Path, cp: str = None):
    command = [str(javac), "--release", release, "-nowarn", "-Xlint:none", "-proc:none",
               "-d", str(out)]
    if preview:
        command.append("--enable-preview")
    if cp:
        command += ["-cp", cp]
    command += [str(f) for f in files]
    return subprocess.run(command, capture_output=True, text=True, env=ENV)


def build_support(javac: Path) -> Path:
    out = Path(tempfile.mkdtemp(prefix="kb-java-support-"))
    result = _javac(javac, "17", False, sorted(SUPPORT_DIR.glob("*.java")), out)
    if result.returncode != 0:
        raise RuntimeError(result.stderr)
    return out


def compile_block(block: Block, javac: Path, support_cp: Path):
    """Return None if one form of the block compiles, else a report of each form's errors."""
    release = release_of(block)
    preview = "preview" in block.markers
    reports = []
    for form_name, (name, source) in zip(FORM_NAMES, candidates(block.body)):
        with tempfile.TemporaryDirectory(prefix="kb-java-") as work:
            source_file = Path(work) / name
            source_file.write_text(source, encoding="utf-8")
            result = _javac(javac, release, preview, [source_file], Path(work) / "out",
                            str(support_cp))
        if result.returncode == 0:
            return None
        errors = [line for line in (result.stderr or result.stdout).split("\n") if line.strip()]
        reports.append(f"  [{form_name}]\n    " + "\n    ".join(errors[:6]))
    return "\n".join(reports)


def _code_lines(text: str) -> list:
    return [line.strip() for line in text.split("\n") if line.strip()]


def matches_main(block: Block, main_source: str) -> bool:
    """True if the block's non-blank lines appear in Main.java as one contiguous run."""
    wanted, have = _code_lines(block.body), _code_lines(main_source)
    if not wanted:
        return True
    return any(have[i:i + len(wanted)] == wanted for i in range(len(have) - len(wanted) + 1))


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


MAIN_METHOD = re.compile(r"\bstatic\s+void\s+main\s*\(")


def run_block(block: Block, java: Path, expected):
    """Run a standalone block that has a main method. Return None or an error text."""
    name, source = candidates(block.body)[0]
    command = [str(java), "--source", "21"]
    if "preview" in block.markers:
        command.append("--enable-preview")
    with tempfile.TemporaryDirectory(prefix="kb-java-run-") as work:
        source_file = Path(work) / name
        source_file.write_text(source, encoding="utf-8")
        try:
            result = subprocess.run(command + [str(source_file)], cwd=work, capture_output=True,
                                    text=True, timeout=30, env=ENV)
        except subprocess.TimeoutExpired:
            return "  program did not finish in 30 seconds"
    if result.returncode != 0:
        hint = ""
        if "can't find main" in result.stderr:
            hint = ("\n  Put the class that has main() first in the block, because the Java "
                    "source launcher runs the first top-level class.")
        lines = [line for line in result.stderr.split("\n") if line.strip()][:6]
        return f"  program exits with code {result.returncode}\n    " + "\n    ".join(lines) + hint
    if expected is not None and _normalize(result.stdout) != _normalize(expected):
        return f"  output differs from the expected-output block\n--- actual ---\n{result.stdout}"
    return None


def find_programs(paths: list) -> list:
    """Main.java files under each folder argument, or next to each file argument."""
    found = set()
    for raw in paths:
        path = Path(raw)
        if path.is_dir():
            found.update(path.rglob("Main.java"))
        elif path.name == "Main.java":
            found.add(path)
        elif (path.parent / "Main.java").exists():
            found.add(path.parent / "Main.java")
    return sorted(found)


def main(argv: list) -> int:
    paths = argv or ["kb"]
    javac, java = JDK / "bin" / "javac", JDK / "bin" / "java"
    if not javac.exists():
        print(f"java: no JDK 21 found (tried {JDK}). Set KB_JDK21 to a JDK 21 home.")
        return 2
    md_paths = list(paths) + [str(p.parent / "README.md") for p in find_programs(paths)]
    files = sorted(set(md_files(md_paths)))
    blocks, expected_after = [], {}
    for f in files:
        page_blocks = parse_blocks(f.read_text(encoding="utf-8"), f)
        for index, block in enumerate(page_blocks):
            if block.lang != "java":
                continue
            blocks.append(block)
            following = page_blocks[index + 1] if index + 1 < len(page_blocks) else None
            if following is not None and "expected-output" in following.markers:
                expected_after[id(block)] = following.body
    skipped = [b for b in blocks if "fragment" in b.markers]
    to_compile = [b for b in blocks if "fragment" not in b.markers]
    programs = find_programs(paths)
    main_sources = {p.parent.resolve(): p.read_text(encoding="utf-8") for p in programs}
    copies = [b for b in to_compile if b.path.parent.resolve() in main_sources]
    standalone = [b for b in to_compile if b.path.parent.resolve() not in main_sources]
    support = build_support(javac)
    with ThreadPoolExecutor(max_workers=8) as pool:
        errors = list(pool.map(lambda b: compile_block(b, javac, support), standalone))
    shutil.rmtree(support, ignore_errors=True)
    failures = 0
    for block, error in zip(standalone, errors):
        if error:
            failures += 1
            print(f"{block.path}:{block.line}: java block does not compile "
                  f"(release {release_of(block)})\n{error}\n")
    runnable = [b for b, e in zip(standalone, errors)
                if e is None and release_of(b) == "21" and MAIN_METHOD.search(b.body)]
    with ThreadPoolExecutor(max_workers=8) as pool:
        run_errors = list(pool.map(
            lambda b: run_block(b, java, expected_after.get(id(b))), runnable))
    for block, error in zip(runnable, run_errors):
        if error:
            failures += 1
            print(f"{block.path}:{block.line}: java block does not run\n{error}\n")
    for block in copies:
        if not matches_main(block, main_sources[block.path.parent.resolve()]):
            failures += 1
            print(f"{block.path}:{block.line}: java block is not a contiguous copy of the "
                  f"folder's Main.java (case-study code blocks must be copied from Main.java)\n")
    program_failures = 0
    for program in programs:
        error = run_program(program, java)
        if error:
            program_failures += 1
            print(error + "\n")
    print(f"java: {len(to_compile) - failures}/{len(to_compile)} blocks pass "
          f"({len(runnable)} programs run), "
          f"{len(skipped)} fragments skipped, "
          f"{len(programs) - program_failures}/{len(programs)} programs match")
    return 1 if failures or program_failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
