# Learning-System Audit and Curriculum Design (2026-11-11)

The user asked for the repository to become a complete, self-contained learning system that builds the ability to solve unfamiliar interview problems. This file records the audit, the design decisions, the batch plan, and the gaps that remain. The status of the batches is in `docs/superpowers/handoff.md`.

## 1. State Before This Work

- 213 Markdown pages under `kb/`, about 1.78 million words. Seven subjects complete (DSA, LLD, HLD, Java, Databases, Operating Systems, Networking). Five subjects are index pages that say "Planned" (API Design, Security, Engineering Practice, Behavioral, AI Engineering).
- The gate (`tools/check-all.sh`) compiles every Java block, runs every program and SQL example, checks the page templates, and builds the site with link validation.
- Every topic page outside DSA patterns 01 to 20 was drafted and reviewed in phases 1 to 6 against one template: fundamentals first, then interview questions with collapsible model answers.

## 2. Findings

Measured with `grep` over `kb/` and by reading sample pages in full.

| # | Finding | Evidence | Severity |
|---|---|---|---|
| 1 | DSA patterns 01 to 20 are template sheets, not lessons. They state the insight and list pseudocode templates. They do not derive the technique, trace it, or prove it | 0 Java blocks, 0 `??? question` blocks, 0 Mermaid diagrams, 0 traces in pages 01, 02, 08, 10, 11, 14, 15. Page 15 has 12 pseudocode templates. Pages 01 to 15 have 1,800 to 3,500 words each; pages 21 to 28 have 7,000 to 12,700 | High: these 20 pages are the base of the coding round |
| 2 | No page teaches recursion from zero. Patterns 08, 09, 10, 11, and 15 assume it | The foundations cover complexity, arrays, stacks, hash tables, trees, heaps, graphs, sorting. Pattern 08 starts at subsets | High |
| 3 | Prerequisites are not stated on the pages | A "prerequisite" line exists in 12 of 50 DSA files and in 3 of about 150 other topic pages | Medium |
| 4 | Practice has no levels of help. A problem table gives the "key insight" next to the title, so the reader sees the approach before the attempt | Hint blocks: 1 DSA file. `practice-solutions.md` covers about 10 problems | High |
| 5 | No mastery levels, no revision dates, no assessments with a pass rule. The DSA plan mentions a misrecognition log and reviews after 1, 3, and 7 days in one paragraph | No tracker page. `mock-interviews.md` has a rubric and no pass rule | High |
| 6 | Mixed practice is thin: 20 classify-only drills. No sets of unlabeled problems to solve, no pairs of look-alike problems | `how-to-choose-a-pattern.md` Part 5 | Medium |
| 7 | One calendar (21 weeks) for every reader. No path for a reader who is short of time or who returns after earlier preparation, and no topic dependency graph | `kb/study-plan.md` | Medium |
| 8 | The legacy DSA pages break the KB writing rules: emoji headings, "Aha moment", unsupported claims | "This trains recognition five times faster than full solves" in `how-to-choose-a-pattern.md` | Low |
| 9 | Subjects that the target interviews include are missing: behavioral, API design, security, engineering practice (testing, debugging, code review) | Index pages only | Medium for behavioral and API design. They are phases 7 to 9 of the existing plan |
| 10 | The topic pages of the other six subjects explain mechanisms well and have model answers, but they have no "Prerequisites", no "Mastery Checklist", and no "Next Topics" | Template in the spec, section 5.1 | Low to medium |

Strengths to keep: `how-to-choose-a-pattern.md` (a real procedure with deciding questions and 12 walkthroughs in which the first guess is wrong), the foundations pages, the runnable and checked code, the collapsible model answers, the LeetCode links, and the "Commonly Confused With" tables.

## 3. Design Decisions

1. **Target interviews:** backend software engineer at product companies, Java. Rounds: coding, LLD or machine coding, HLD, language and fundamentals, behavioral. Taken from the content and from the spec.
2. **Priority:** finding 1 first. A reader who cannot derive sliding window or DP gains little from a tracker. Then findings 2, 4, 5, 6, 7.
3. **Lesson format for DSA pages:** `docs/superpowers/lesson-brief.md`. It follows the user's own sample (`kb/dsa/spike-dp-house-robber.md`): the attempts that fail, the derivation one question at a time, a full trace, the reason for correctness, the vocabulary last. Pictures are plain-text traces and Mermaid diagrams, not HTML cells, so that the pages stay readable in an editor and need no custom CSS. Recommendation for the user: if the colored cells of the sample helped more than the text traces, say so, and a later batch can add them to the traces.
4. **Hints and solutions stay on the lesson page** in collapsible blocks (three hints, then the solution). The site already uses this mechanism for model answers. A separate solutions file per pattern would double the number of files.
5. **A lesson page is marked** with `format: lesson` in its front matter. The checker then requires the 16 lesson sections and 8 question blocks, and the Java runner compiles the page with JDK 21 and runs its programs. A legacy page without the mark is still exempt.
6. **Mastery system:** `kb/mastery-tracker.md` (six levels with a test each, the study loop, the revision schedule, the weak-area log, milestone rules) and `kb/my-progress.md` (the reader's sheet; no tool rewrites it).
7. **Roadmap:** `kb/roadmap.md` (stages with exit tests, the DSA dependency graph, dependencies between subjects, five learning paths). `kb/study-plan.md` stays the week-by-week calendar.

## 4. Batches and Their State (2026-11-12)

The user set the limit of this work at pattern 14 during the session. Patterns 16 to 20 and the two practice pages are not started.

| Batch | Content | State |
|---|---|---|
| A | Audit, lesson brief, checker rules and tests, roadmap, mastery tracker, progress sheet, navigation, README pages, glossary terms | Done |
| B1 | Lessons: foundation F9 (recursion), patterns 15, 01, 08, 06 | Drafted and reviewed |
| B2 | Lessons: patterns 02, 03, 04, 05, 07 | Drafted and reviewed |
| B3 | Lessons: patterns 09, 10, 11, 12, 13, 14 | Drafted and reviewed |
| B4 | Lessons: patterns 16, 17, 18, 19, 20 | Not started |
| B5 | `kb/dsa/mixed-practice.md` (sets without pattern labels, milestone sets) and `kb/dsa/problem-solving-method.md` | Not started. `kb/mastery-tracker.md` gives a rule for building a mixed set until the page exists |

Process for each lesson: one Sonnet drafter (it reads `drafting-brief.md` and `lesson-brief.md`), then one Sonnet reviewer with two passes (expert, then learner). Each reviewer ran its own randomized tests against brute force. The main session read each report, checked samples, and ran the gate.

Tool changes in this work:

- `tools/check_format.py`: the `dsa-lesson` template for a page with `format: lesson`; two rules for the indentation of code fences inside collapsed blocks (two drafts had fences that the site would show as plain text).
- `tools/run_java.py`: a lesson page in a legacy path compiles with JDK 21 and its programs run.
- `tools/java-support/UnionFind.java`: a support class, so that a block that calls `UnionFind` compiles without a copy of the class.

## 5. Gaps That Remain

1. Patterns 16 to 20 (heap, trie, shortest paths, bit manipulation, intervals) are still template sheets. They are P1 pages. This is the largest gap in the coding track.
2. No page of ready mixed sets and milestone sets, and no page that shows the full problem-solving method on unseen problems (batch B5).
3. Patterns 21 to 28 and the foundations F0 to F8 keep their earlier templates. They have traces, Java, and questions, but no practice ladder with hints and no mastery checklist.
4. The topic pages of LLD, HLD, Java, Databases, Operating Systems, and Networking have no "Prerequisites", "Mastery Checklist", or "Next Topics" sections. The roadmap gives the dependencies between subjects at the level of pages.
5. API Design, Security, Engineering Practice (testing, debugging, code review), Behavioral, and AI Engineering are not written (phases 7 to 11 of the spec).
6. The legacy reference pages (`cheatsheet.md`, `tricks-at-a-glance.md`, `pattern-combinations.md`, `common-mistakes.md`, `interview-tips.md`, `company-prep.md`, `mock-interviews.md`, `practice-solutions.md`) keep their old style. `company-prep.md` makes claims about company focus areas that have no source. Some of these pages refer to section names of the old pattern pages (for example "Template 3"); the links still resolve, because they have no anchors, but the names no longer exist on the lesson pages.
7. Statements about LeetCode problem constraints on the lesson pages come from the writers' memory, because the saved problem list has no statements. Each page words them as "the problem guarantees" and uses them only where a solution needs them.
8. Proofs that the pages state as not complete: the skip rule of LC 316 (pattern 07) rests on a brute-force test. The union find cost bounds cite the papers and are not proved.
9. No learner has used the lessons yet. The levels, the review intervals, and the time limits are recommendations until the user tests them. The user's sample used colored HTML cells for traces; the lessons use text traces and Mermaid diagrams.
