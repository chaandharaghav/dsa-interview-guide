# Lesson Brief for DSA Pages

This brief adds to `docs/superpowers/drafting-brief.md`. Read that file first: its writing rules (section 2), laptop reading rules (section 3), markers (section 6), verification duties (section 7), and output rules (section 9) apply here without change. This brief replaces its section 4.4 for every page that the assignment names as a lesson page.

## 1. Goal

A lesson page teaches the reader to solve a problem that the reader has not seen before. It does not give the reader a template to memorize.

The reader is a Java developer who returns to DSA after a long break. The reader knows Java syntax, loops, arrays, and methods. Assume nothing else. If the lesson needs an idea, the lesson teaches the idea or links to the KB page that teaches it.

The page succeeds if the reader can do these four things after it:

1. Rebuild the technique from a blank page, starting from the brute-force solution.
2. Say why the technique gives a correct answer.
3. Decide, for a new problem, whether the technique applies, and name the property that decides it.
4. Write the Java code without a template to copy.

The model for the lesson style is `kb/dsa/spike-dp-house-robber.md`. Read it in full before you write. It is the user's own sample and the user's file: do not edit it, move it, or link to it. Copy its method, not its HTML: use plain-text traces and Mermaid diagrams, not `<span>` cells or `<style>` blocks.

## 2. Rules of the Lesson Style

1. **Concrete before abstract.** Start each idea with one small input that the reader can hold in the head (5 to 8 elements). Give the definition after the reader has seen the idea work.
2. **Show the attempts that fail.** Before the technique, show the first idea that a reader has (the brute force, or a simple rule). Give a small input on which it is too slow or wrong. State the exact work that it wastes, or the exact case that it misses.
3. **Derive, do not announce.** Each step of the technique must answer a problem that the text has shown. Do not write "the key insight is", "the trick is", "this is a standard technique", or "it is obvious that". Write the question that leads to the step, then the step.
4. **Trace every algorithm.** Each algorithm has at least one complete trace on a concrete input: one row or one block for each step, with the variable values and the decision. The trace must match the code line by line. A runnable Java program must print the same trace (see section 5).
5. **Prove it at interview depth.** State the invariant (the statement that stays true after every step) and give the reason in 3 to 8 sentences. The reader must be able to repeat the reason to an interviewer.
6. **Name the terms last.** Introduce the vocabulary after the reader has done the thing that the term names. Then give a table: term, what it was in the example, the question to ask in a new problem.
7. **State when the technique fails.** Give a counterexample input for each precondition (for example, negative numbers for a sum window). The reader must see the wrong output, not only a warning.
8. **No templates without a derivation.** A generalized template may appear only after a worked example has derived each line of it.
9. **No unsupported claims.** Do not state how often companies ask a problem. Do not invent problem numbers, titles, or difficulties.

## 3. Page Structure

The page starts with this front matter. `format: lesson` selects the lesson template in `tools/check_format.py` and makes `tools/run_java.py` compile the page with JDK 21 and run its programs.

```markdown
---
priority: P1
format: lesson
---
# Pattern NN: Name
```

Use these `##` headings, with these exact names, in this order. You may add `###` subsections freely. Do not number the `##` headings.

| Heading | Content |
|---|---|
| `## Overview` | Two to four sentences: what the technique is, in plain words. Then "After this page you can:" with 4 to 6 outcomes that a reader can test |
| `## Prerequisites` | A table with the columns "You need", "Why this page needs it", "Where to learn it". Link each row to a KB page. If the KB has no page for a prerequisite, teach it here in a short subsection |
| `## Motivation` | One anchor problem, stated in full with a small input and its answer. The first attempts and why each fails or is too slow (rule 2). End with the exact waste or gap that the technique removes |
| `## Derivation` | Build the technique on the anchor problem, one question at a time (rule 3). A complete trace (rule 4). The reason it is correct (rule 5). The cost, counted from the trace before it is stated in O-notation |
| `## The Pattern` | The reusable idea in 3 to 6 sentences. The invariant. The vocabulary table (rule 6). The variants of the pattern, each with the property of the problem that selects it |
| `## Worked Examples` | Two to four more problems that each change one thing about the anchor problem. Each has: the statement, "What changed", the reasoning from brute force to the solution, a trace, the Java code, the complexity with its reason. Order them from easy to hard |
| `## Implementation` | The Java templates, each derived above. Explain each line that is not obvious (a loop bound, `<` against `<=`, the order of two statements). One complete program with `main` that prints a trace, followed by its `<!-- expected-output -->` block |
| `## Complexity and Trade-offs` | A table: variant, time, space, and the reason for each bound. Then the alternatives (for example sort and scan, or a hash map) and when each one is better |
| `## Recognition Guide` | Signals in the problem statement and in the constraints. Then "Looks similar, but needs another technique": at least 4 problems, each with the property that rules this pattern out and a link to the right pattern. Then the preconditions with a counterexample each (rule 7) |
| `## Common Mistakes` | Each mistake with a failing input, the wrong output, and the fix. Keep every correct mistake from the old page |
| `## Practice Problems` | The practice ladder (section 4) |
| `## Interview Follow-ups` | At least 8 `??? question` blocks: the questions an interviewer asks after the solution ("What if the input has negative numbers?", "Can you do it in one pass?", "Why is this O(n) and not O(n²)?"). Each model answer gives the direct answer, then the reason |
| `## Commonly Confused With` | Keep the old page's table (columns "Looks like", "Difference", "Deciding question") and its links to `how-to-choose-a-pattern.md` |
| `## Quick Reference Card` | The short summary for revision: when to use, the invariant, the templates by name, the complexity, the top mistakes |
| `## Mastery Checklist` | A task list (`- [ ]`) of 6 to 10 statements that the reader can test without the page, for example "I can derive the shrink condition for a new constraint without the page". Order them by the levels in `kb/mastery-tracker.md`: explain, solve a familiar problem, solve a new problem, solve under time |
| `## Next Topics` | The pages to read next, each with one sentence on what it adds. Link to `kb/roadmap.md` |

### Keep what is correct

The old page has value: its problem tables, its variations, its mistakes, its "Commonly Confused With" table, and its LeetCode links. Keep every correct item. Move each item to the section where it belongs. Rewrite its text to follow the writing rules. If an old statement is wrong, correct it and list the correction in your review note. Remove emoji, the phrases "Aha moment" and "What Interviewers Expect You to Think", and week-by-week schedules (the study plan owns the schedule).

Other pages link to anchors on this page. Before you change a heading, run `grep -rn "<folder-name>/README.md#" kb --include='*.md'`. For each anchor that another page uses, keep the anchor: add `{ #the-old-anchor }` at the end of the heading that now holds that content.

## 4. The Practice Ladder

`## Practice Problems` has 8 to 12 problems in a table, from easy to hard: title (linked), difficulty, and "What it adds" (the one new thing compared with the problem before it). Do not put the approach in the table.

After the table, give the full treatment to at least 6 problems, with at least one Hard. Each one has this form, so that the reader can stop at any level of help:

````markdown
### P3. Title (LC 209, Medium)

The statement in your own words, with one small example and its answer.

??? tip "Hint 1: where to look"

    One question that points at the property to notice. It does not name the technique step.

??? tip "Hint 2: the idea"

    The idea in one or two sentences, without code.

??? tip "Hint 3: the plan"

    The steps of the algorithm in words.

??? success "Solution"

    The reasoning from brute force to the final approach, a short trace, the Java code,
    the complexity, and the mistake that this problem invites.
````

After the ladder, add `### Tell Them Apart`: 3 or 4 pairs of problems whose statements look alike and whose techniques differ. For each pair, state both problems, ask the reader which technique each one needs, and give the answer with the deciding property in a `??? question` block. At least one pair must put a problem of this pattern next to a problem of another pattern.

## 5. Java Code

- Java 21, standard library only. Follow drafting-brief section 7 for runnable blocks: a block with `main` needs all its imports, has the class with `main` first, and is followed by `<!-- expected-output -->` and a `text` block with the exact output.
- Every Java block must compile under `python3 tools/run_java.py <page>`. The old page has pseudocode in unlabeled blocks. Replace each pseudocode template with Java. Keep pseudocode only for the first statement of an idea, in a `text` block.
- Method names say what the method returns (`longestWithoutRepeat`, not `solve`). Comments explain why, not what.
- Each program with `main` tests the edge cases too (empty input, one element, all equal, the answer at the first or last position) and prints their results.
- Check each solution on at least 5 inputs, including the edge cases, before you put it on the page. Where a brute-force solution exists, write a throwaway randomized test in your scratch folder that compares the two on 1,000 small random inputs. Report the result in your review note.

## 6. Diagrams and Traces

- A trace is a `text` block or a table. Mark the cells that a step reads and writes with brackets, and give the legend:

    ```text
    index    0   1   2   3   4   5
    value    2   3   1   2   4   3
             L-----------R            window = [2, 3, 1, 2], sum = 8 >= 7, length 4
    ```

- Use Mermaid for a recursion tree, a decision tree, a state machine, or a graph. In a sequence diagram, write `#59;` for a semicolon inside a message.
- Use content tabs (`=== "Step 1"`) for a trace that has more than 6 steps, so that the reader clicks through the steps.

## 7. LeetCode References

- Prose and checklists link the token: `[LC 3: Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/)`. A problem table links the title cell and gives the number in its own column. A paid problem carries `(premium)` after the link.
- Take every number, title, slug, difficulty, and premium flag from `scratchpad/lc-links/lc_all.json` (the LeetCode problem list, saved on 2026-10-21). Do not use memory. `python3 scratchpad/lc-links/check_links.py <page>` checks the links of a page.
- State a problem in your own words. Do not copy a LeetCode statement.

## 8. Review Note

End with the review note of drafting-brief section 9, and add:

1. each old-page item that you removed, with the reason;
2. each old-page statement that you corrected;
3. each anchor that you kept for inbound links;
4. the result of the randomized tests.

## 9. Defects That Reviews of the First Lessons Found

Check your page for each of these before you finish.

1. A fenced block inside a `???` block must have its fences at exactly 4 spaces of indentation (8 spaces inside a nested block or a tab). A marker comment such as `<!-- expected-output -->` takes the same indentation. With a wrong indentation the site shows the code as an indented block and the runner does not see it.
2. "Hint 1" named the technique step. Hint 1 points only at a property of the input or of the brute force.
3. The "Mastery Checklist" items had no level labels or were out of order. Start each item with its level (`**L2**` to `**L6**`), in ascending order, and use the time limits of `kb/mastery-tracker.md` (25 minutes for a Medium problem, 40 minutes for a Hard problem).
4. A script that adds links produced "(premium) (premium)" and a repeated title after the link. Search the finished page for both.
5. A proof skipped the edge case (no valid window, an empty range, an element that does not exist). State the edge case in the proof.
6. A template used an abstract interface that hid the logic. Templates are concrete methods with comments at the lines that a new problem changes.
7. The same code appeared twice on the page. Show it once and link to it.
8. A term appeared in a table or a heading before the text defined it. Define each term at its first use, or say where the page defines it.
9. A statement about a LeetCode constraint came from memory. The saved list has no statements. Write "the problem guarantees ..." only for a fact that the solution needs, and do not quote limits.
10. For each problem of the ladder that the lesson text solves in full, the table must say so and tell the reader to attempt the problem before reading that section.
