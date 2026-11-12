# Software Engineering Knowledge Base

A knowledge base for software engineering interview preparation and engineering fundamentals. The content is in Markdown under `kb/`. A local MkDocs Material site gives navigation, search, and collapsible model answers.

## Start Learning

1. Open the site (next section), or open `kb/README.md` in an editor.
2. Read `kb/roadmap.md`. It gives the stages, the topic dependencies, and a learning path for your start point and your available time.
3. Read `kb/mastery-tracker.md`. It defines the six mastery levels, the study routine for one topic, the revision schedule, and the milestone assessments.
4. Record your level for each topic in `kb/my-progress.md`. The earliest review date on that page is what you study today.

The KB covers DSA, low-level design, high-level design, Java, databases, operating systems, and networking. API design, security, engineering practice, and behavioral interviews are not written yet. `kb/roadmap.md` states what the KB covers for each interview round.

## Open the Site

```bash
tools/serve.sh
```

The script creates a Python virtual environment in `.venv/`, installs the pinned packages from `requirements.txt`, and starts the site at <http://127.0.0.1:8000>. The first run needs network access to download the packages. Mermaid diagrams also load their script from a CDN, so diagrams need network access.

You can also read the Markdown files directly in an editor. The site extensions (collapsible blocks, tabs) then show as plain text.

## Run the Checks

```bash
tools/check-all.sh
```

The script runs these checks. All of them must pass before a change is committed.

| Check | What it does |
|---|---|
| Unit tests | Tests the checker scripts in `tools/tests/` |
| `tools/check_format.py` | Checks code line width, template sections (the lesson sections for a page with `format: lesson`), front matter, and question counts |
| `tools/run_sql.py` | Runs the SQL examples on the `sqlite3` tool (SQLite 3.39 or newer) and compares results |
| `tools/run_java.py` | Compiles every Java block with JDK 21 and runs every case-study `Main.java` |
| `mkdocs build --strict` | Builds the site and fails on a broken link or anchor |

## Layout

| Path | Content |
|---|---|
| `kb/` | All KB content. `kb/README.md` is the home page |
| `tools/` | Checker scripts and site scripts |
| `docs/superpowers/` | Design specs, implementation plans, the drafting brief, the lesson brief, and the learning-system audit. Not KB content |
| `mkdocs.yml` | Site configuration |
