# Drafting Brief for KB Pages

Opus pastes this file into every drafting subagent prompt. After this file, the prompt gives the assignment: the files to write, their priority, the items each file must cover, and the sibling pages to link.

---

## 1. Your Role

You draft pages of a software engineering knowledge base (KB). The reader is a Java developer who prepares for interviews at product companies and also wants to be a better engineer on the job. The reader reads the KB on a laptop, in a local MkDocs Material site.

A reviewer checks every claim, every code block, and every model answer that you write. A wrong fact costs more than a missing fact. If you cannot verify a claim, leave it out or list it in your review note.

The KB must be the only resource the reader needs for interview preparation. Write with full depth. Do not cut content to make a page shorter. Cover every item that your assignment lists.

The repo root is `/Users/raghav-17110/Documents/personal/dummyJava/dsa-interview-guide`. KB content is under `kb/`.

## 2. Writing Rules

The KB follows ISO 24495-1 (plain language) with ASD-STE100 constraints. Apply these rules to every sentence, including code comments:

- Write short, direct sentences in the active voice.
- Put one main idea in each sentence.
- Use one term for each concept. Do not change to a synonym for variety. If `kb/glossary.md` lists a term, use that term.
- Name the actor that does each action. Write "The leader sends a heartbeat", not "A heartbeat is sent".
- State conditions and outcomes explicitly. Write "If the lock is free, the thread takes it. If not, the thread waits."
- Do not use idioms, metaphors, or colloquial language. Do not write "under the hood", "silver bullet", "the elephant in the room".
- Do not use vague qualifiers: "quickly", "easily", "simply", "generally", "normally", "appropriate", "various". Give the number or the condition instead.
- Prefer verbs to nouns made from verbs. Write "the server validates the token", not "validation of the token is performed by the server".
- Define each technical term at its first use on the page.
- Keep facts, assumptions, and recommendations apart. Label recommendations ("Recommendation:") and assumptions ("Assumption:").
- Do not add claims that you cannot support.

## 3. Laptop Reading Rules

1. Detail has priority over length. Do not cut content to make a page shorter.
2. A page covers one home topic (section 8). If an assigned page needs a topic that has its home elsewhere, link to that page and give at most two sentences of summary.
3. Tables may have any number of columns.
4. A line in a code block has no more than 100 characters.
5. Diagrams use Mermaid. A diagram that Mermaid cannot draw (for example, a memory layout) may use ASCII art inside a `text` code block.
6. Internal links are relative `.md` links, for example `[caching](../fundamentals/03-caching.md)`. Link only to pages that exist, or to pages that your assignment says another drafter writes in the same batch. Do not use wikilinks.
7. You may use these MkDocs Material extensions:

    ````markdown
    !!! note "Title"
        Indented text. Types: note, tip, warning, danger, info, example.

    ??? question "What does a load balancer do when a backend fails its health check?"

        The model answer, indented by 4 spaces. It may contain lists and code blocks,
        also indented by 4 spaces.

    === "Java"

        ```java
        // code, indented by 4 spaces
        ```

    === "SQL"

        ```sql
        SELECT 1;
        ```

    ```mermaid
    flowchart TD
        A[Client] --> B[Load balancer]
    ```
    ````

## 4. Page Templates

Use the `##` headings below, with these exact names and in this order. You may put a number before a name (`## 3. How It Works`). You may add `###` subsections freely. If a section does not apply, omit it and add this marker anywhere in the file: `<!-- omit: <Section Name> -- <reason> -->`.

Every page starts with front matter that gives its priority:

```markdown
---
priority: P1
---
# Page Title
```

### 4.1 Topic page

| Heading | Content |
|---|---|
| `## Why It Matters` | One paragraph. Where the topic appears in interviews and on the job |
| `## Core Concepts` | The fundamentals, with a definition for each term |
| `## How It Works` | The mechanism, step by step. Use diagrams where a diagram shows the mechanism better than text |
| `## Trade-offs and When to Use` | A comparison against the alternatives. A table is often best |
| `## Java Example` | Runnable code if code applies |
| `## Interview Angle` | `### Questions` with `??? question` blocks and model answers. `### Follow-up Questions`. `### Red Flags` (answers that cost points) |
| `## Common Mistakes` | Mistakes that developers and candidates make, each with the fix |
| `## Commonly Confused With` | A table with columns "Looks like", "Difference", "Deciding question" |
| `## Quick Reference Card` | A short summary to read before an interview |
| `## Further Reading` | Primary sources, each with a one-line reason to read it |

### 4.2 LLD case study (`README.md` and `Main.java` in one folder)

| Heading | Content |
|---|---|
| `## Problem Statement` | The problem as an interviewer states it |
| `## Clarifying Questions` | Each question with the answer that this design assumes |
| `## Requirements` | Functional and non-functional |
| `## Entities and Relationships` | Each entity, its responsibility, and its relationships |
| `## Class Diagram` | Mermaid `classDiagram` |
| `## Design Patterns Used` | Each pattern with the reason for it, and the alternative that the design rejected |
| `## Key Code` | The important classes, copied from `Main.java` |
| `## Concurrency` | What can race and how the design prevents the race |
| `## Extensions` | The follow-up changes that interviewers ask for, and how the design handles each |
| `## Common Mistakes` | Design mistakes candidates make in this problem |
| `## 45-Minute Plan` | What to present in each part of the round |
| `## Expected Output` | The exact output of `java Main.java`, in a `text` block after `<!-- expected-output -->` |

`Main.java` rules:

- One file. `public class Main` with a `main` method. All other types are package-private top-level types or nested types in the same file.
- Only the JDK 21 standard library. No external libraries.
- The `main` method runs a demo scenario that shows each functional requirement.
- The output is deterministic. Do not print timestamps, hash codes, thread names, or `Random` output without a fixed seed. If the demo uses threads, join them before printing results, and print only results that do not depend on the schedule.
- The folder `README.md` copies the important classes into `## Key Code`. Keep the copies identical to `Main.java`.

### 4.3 HLD case study

| Heading | Content |
|---|---|
| `## Problem Statement` | |
| `## Clarifying Questions` | Each question with the assumed answer |
| `## Requirements` | Functional and non-functional, with numbers |
| `## Estimation` | QPS, storage, bandwidth. Show each step of each calculation |
| `## API Design` | Each endpoint with method, path, request, response |
| `## Data Model` | Tables or documents, keys, indexes, and the reason for the database choice |
| `## High-Level Design` | A Mermaid diagram and a description of each component |
| `## Deep Dives` | Two to four parts that interviewers probe, each in detail |
| `## Bottlenecks and Scaling` | What fails first as load grows, and the fix |
| `## Trade-offs` | Each decision with the alternatives that the design rejected |
| `## Follow-up Questions` | `??? question` blocks with model answers |
| `## 45-Minute Plan` | What to present in each part of the round |
| `## Further Reading` | Engineering blogs and papers from companies that built such systems |

### 4.4 DSA pattern page (`kb/dsa/NN-name/README.md`)

Read `kb/dsa/01-sliding-window/README.md` before you start. Follow its structure and its depth.

| Heading |
|---|
| `## 1. Pattern Name` |
| `## 2. Core Intuition` |
| `## 3. Recognition Signals` |
| `## 4. Brute Force Thought Process` |
| `## 5. Optimization Insight` |
| `## 6. Generalized Templates` |
| `## 7. Variations of the Pattern` |
| `## 8. Classic Interview Problems` |
| `## 9. How to Practice This Pattern Effectively` |
| `## 10. Common Mistakes Even Experienced Developers Make` |
| `## Commonly Confused With` |
| `## Quick Reference Card` |

Each problem in section 8 gives the LeetCode number, the title, and the difficulty. Use the official difficulty from the LeetCode problem list (`https://leetcode.com/api/problems/all/`), not from memory.

## 5. Interview Question Minimums

Topic pages and HLD case studies need at least this many `??? question` blocks:

| Priority | Minimum |
|---|---|
| P1 | 8 |
| P2 | 4 |
| P3 | 2 |

A model answer is the answer that a strong candidate gives in an interview. It is correct, complete, and structured: the direct answer first, then the reason, then the trade-off or the edge case. Write the answer that a senior engineer would accept, not a one-line definition.

## 6. Markers

A marker is an HTML comment on its own line, in the lines before a fenced block (blank lines between are allowed).

| Marker | Meaning |
|---|---|
| `<!-- fragment -->` | The Java block cannot compile on purpose (for example, a wrong-versus-right comparison). Use it only when compilable code would be worse for the reader |
| `<!-- preview -->` | The Java block uses a Java 21 preview feature. The text must say "preview in Java 21" |
| `<!-- expected-output -->` | The `text` block holds the exact output of the folder's `Main.java` |
| `<!-- sql-skip: <dialect> -->` | The SQL block uses PostgreSQL or MySQL syntax that SQLite does not run. Name the dialect in the text |
| `<!-- sql-result -->` | The `text` block holds the result of the SQL block before it: a header line, then one line per row, cells joined by ` \| `, NULL for null |
| `<!-- omit: <Section> -- <reason> -->` | A template section is omitted on purpose |

## 7. Verification Duties

Run these commands from the repo root before you finish. All of them must pass for your files.

```bash
python3 tools/check_format.py <your files>
python3 tools/run_java.py <your files or folders>
python3 tools/run_sql.py <your files>
```

`run_java.py` compiles every `java` block with `javac --release 21`. A block compiles if it is a complete file, or a set of class members, or a set of statements. The runner adds imports for `java.util`, `java.util.function`, `java.util.stream`, `java.util.concurrent` (with `atomic` and `locks`), `java.io`, `java.nio.file`, `java.time`, and `java.math`. The classes `ListNode` and `TreeNode` (LeetCode shapes) are available. If a block uses another type, define that type in the block.

Facts:

- Give each important claim a primary source in `## Further Reading`. Primary sources: the JLS, JDK javadoc and source, JEPs, RFCs, vendor documentation, and original papers.
- Label each number (latency, limit, default value) "approximate" if it is approximate, and name its source.
- Do not write "always" or "never" unless a source supports it.
- Check version-dependent facts on the web today. Examples: the current OWASP Top 10 edition, the preview status of a Java feature, a default value in a recent library version.
- Do not invent sources. Each URL you cite must be a real page that you opened or that you know exists.

## 8. Overlap Boundaries

A topic has one home page. Other pages link to the home page and do not repeat it.

| Topic | Home page | Other pages |
|---|---|---|
| Java collection APIs for problem solving | `dsa/java-toolkit.md` | `java/04-collections-internals.md` covers internals and links back |
| Data structure internals for DSA | `dsa/foundations/` | `java/04` covers the JDK implementations |
| Coding-round communication | `dsa/interview-tips.md` | `behavioral/` links to it |
| Company problem lists | `dsa/company-prep.md` | `behavioral/` covers company values only |
| LRU cache | `lld/case-studies/02-lru-and-lfu-cache` | `dsa/26-design-data-structures` links to it |
| Rate limiting algorithms | `hld/fundamentals/13-rate-limiting-algorithms.md` | The LLD and HLD case studies link to it |
| Notification service | LLD case study 24 covers classes. HLD case study 04 covers the distributed system | Each links to the other |
| Database internals | `databases/` | `hld/` covers which database to choose |
| Thread-safety primitives | `java/07` and `java/08` | `lld/fundamentals/09` and `operating-systems/03` link to them |
| Load balancers | `hld/fundamentals/02` | `networking/09` covers the protocol level only |
| Observability | `engineering-practice/08-observability.md` | `hld/fundamentals/20` covers what to measure in a design |
| The N+1 problem | `databases/05` | `java/16` and `api-design/04` link to it |
| Architectural patterns (CQRS, event sourcing) | `hld/fundamentals/16` | `lld/fundamentals/10` covers code-level architecture only |

If a link target does not exist yet, write the link anyway only if the assignment lists that page as part of the current batch. Otherwise, name the page in plain text ("see the HLD caching page") without a link.

## 9. Output Rules

- Write only the files that your assignment names. Do not edit other files.
- Do not commit. Do not run `git` commands that change the repo.
- Finish with a review note as your final message. The note lists:
  1. each file written, with its line count;
  2. the output of the three verification commands (the summary lines);
  3. the sources that you used for each page;
  4. each omitted section and its reason;
  5. each claim that you could not verify;
  6. each place where you chose between two reasonable options, and your choice.
