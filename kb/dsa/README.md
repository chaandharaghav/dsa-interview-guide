# DSA Interview Mastery Guide
## Pattern-First Approach to Crack Any Coding Interview

**Target:** Product-based companies (FAANG-level)  
**Focus:** Medium and Hard problems  
**Philosophy:** Build problem-solving intuition, not memorized tricks

---

## Start Here (Returning to DSA)

The [learning roadmap](../roadmap.md) shows which page needs which earlier page. The [mastery tracker](../mastery-tracker.md) gives the routine for one pattern: attempt, derive, trace, explain, write, practice, tell apart, record.

1. **[How to Choose a Pattern](./how-to-choose-a-pattern.md)**: the six-step procedure, the 15 deciding questions, decision trees, 12 worked walkthroughs, and 20 recognition drills. This file is the core of the guide.
2. **[Java Toolkit](./java-toolkit.md)**: the Java collections, the Java pitfalls that cost interviews, and a Java template for each core pattern.
3. **[Study Plan](./study-plan.md)** (12 core weeks, plus 3): Week 0 is a re-entry week. It covers items 1 and 2, and the [foundations](#foundations) pages.
4. Work through the patterns in the order of the study plan. Each pattern file ends with a "Commonly Confused With" table. That table links back to the deciding questions.

---

## 🆕 Quick Links

| Resource | Description |
|----------|-------------|
| 🧭 [How to Choose a Pattern](./how-to-choose-a-pattern.md) | Decide which pattern to use, step by step |
| ☕ [Java Toolkit](./java-toolkit.md) | Java collections, pitfalls, and templates |
| 📅 [Study Plan](./study-plan.md) | Day-by-day structured prep plan |
| 📋 [Ultimate Cheatsheet](./cheatsheet.md) | One-page reference for all patterns |
| ⚡ [Tricks at a Glance](./tricks-at-a-glance.md) | See trigger → apply trick instantly (41 tricks, 7 categories) |
| 📝 [Practice Solutions](./practice-solutions.md) | Step-by-step solutions to key problems |
| 🎤 [Interview Tips](./interview-tips.md) | Communication & soft skills guide |
| 🏢 [Company-Specific Prep](./company-prep.md) | Google, Amazon, Meta, Microsoft focus |
| 🎯 [Mock Interviews](./mock-interviews.md) | Timed practice question sets |
| ⚠️ [Common Mistakes](./common-mistakes.md) | Mistakes to avoid & recovery tips |
| 🔗 [Pattern Combinations](./pattern-combinations.md) | Multi-pattern problem strategies |

---

## How to Use This Guide

1. **Attempt first.** On a lesson page, read "Motivation" up to the problem statement, close the page, and try the problem for 10 minutes.
2. **Derive.** Read "Derivation". At each question in the text, stop and answer before you read on.
3. **Trace and explain.** Trace the algorithm by hand on a new input, then explain it aloud without the page.
4. **Practice with the hints closed.** Give each ladder problem 15 minutes before you open "Hint 1".
5. **Tell the patterns apart.** Do "Tell Them Apart" on each page, and practice recognition with [How to Choose a Pattern](./how-to-choose-a-pattern.md).
6. **Record and review.** Write your level in [My Progress](../my-progress.md), and review on the dates of the [revision schedule](../mastery-tracker.md#the-revision-schedule).

The full routine, with the level that each step reaches, is in the [mastery tracker](../mastery-tracker.md#the-study-loop-for-one-topic).

---

## Foundations

Read these pages in Week 0, or when a pattern uses a structure that you do not know well. Each page explains how the structure works, what each operation costs, and the theory questions that interviewers ask.

| # | Page | Priority |
|---|------|----------|
| F0 | [Math for Complexity Analysis](./foundations/00-math-for-complexity.md) | P2 |
| F1 | [Complexity Analysis](./foundations/01-complexity-analysis.md) | P1 |
| F2 | [Arrays, Strings, and Linked Lists](./foundations/02-arrays-strings-linked-lists.md) | P1 |
| F3 | [Stacks, Queues, and Deques](./foundations/03-stacks-queues-deques.md) | P1 |
| F4 | [Hash Tables](./foundations/04-hash-tables.md) | P1 |
| F5 | [Trees and Balanced BSTs](./foundations/05-trees-and-balanced-bsts.md) | P1 |
| F6 | [Heaps](./foundations/06-heaps.md) | P1 |
| F7 | [Graph Representations](./foundations/07-graph-representations.md) | P1 |
| F8 | [Sorting Algorithms](./foundations/08-sorting-algorithms.md) | P1 |
| F9 | [Recursion](./foundations/09-recursion.md) | P1 |

---

## Pattern Index

The "Format" column states how each page teaches:

- **Lesson:** the page derives the technique from a brute-force solution, traces it, gives the reason that it is correct, and has a practice ladder with three hints and a solution for each problem. It ends with a mastery checklist. Patterns 01 to 17 and [Recursion](./foundations/09-recursion.md) have this format.
- **Template sheet:** the page states the idea and gives pseudocode templates and a problem list. It does not derive the technique and has no hints. Patterns 18 to 20 still have this format. Use them with the foundations pages and with [How to Choose a Pattern](./how-to-choose-a-pattern.md).
- **Topic page:** the page explains the mechanism with Java code, traces, and interview questions with model answers. It has no practice ladder with hints. Patterns 21 to 28 have this format.

| # | Pattern | Difficulty | Priority | Format |
|---|---------|------------|----------|--------|
| 01 | [Sliding Window](./01-sliding-window/README.md) | ⭐⭐ | HIGH | Lesson |
| 02 | [Two Pointers](./02-two-pointers/README.md) | ⭐⭐ | HIGH | Lesson |
| 03 | [Fast & Slow Pointers + Linked List Techniques](./03-fast-slow-pointers/README.md) | ⭐⭐ | MEDIUM | Lesson |
| 04 | [Prefix Sum / Difference Array](./04-prefix-sum/README.md) | ⭐⭐ | HIGH | Lesson |
| 05 | [Hashing & Frequency Maps](./05-hashing-frequency/README.md) | ⭐⭐ | HIGH | Lesson |
| 06 | [Binary Search (Classic & On Answer)](./06-binary-search-on-answer/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 07 | [Monotonic Stack / Queue](./07-monotonic-stack-queue/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 08 | [Recursion & Backtracking](./08-recursion-backtracking/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 09 | [Divide and Conquer](./09-divide-and-conquer/README.md) | ⭐⭐⭐ | MEDIUM | Lesson |
| 10 | [Tree DFS / BFS Patterns](./10-tree-patterns/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 11 | [Graph Traversal Patterns](./11-graph-traversal/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 12 | [Topological Sort](./12-topological-sort/README.md) | ⭐⭐⭐ | MEDIUM | Lesson |
| 13 | [Union Find](./13-union-find/README.md) | ⭐⭐⭐ | MEDIUM | Lesson |
| 14 | [Greedy Patterns](./14-greedy/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 15 | [Dynamic Programming](./15-dynamic-programming/README.md) | ⭐⭐⭐⭐ | CRITICAL | Lesson |
| 16 | [Heap / Priority Queue](./16-heap-priority-queue/README.md) | ⭐⭐⭐ | HIGH | Lesson |
| 17 | [Trie (Prefix Tree)](./17-trie/README.md) | ⭐⭐⭐ | MEDIUM | Lesson |
| 18 | [Shortest Paths (Dijkstra, Bellman-Ford, Floyd-Warshall)](./18-shortest-paths/README.md) | ⭐⭐⭐ | HIGH | Template sheet |
| 19 | [Bit Manipulation](./19-bit-manipulation/README.md) | ⭐⭐⭐ | MEDIUM | Template sheet |
| 20 | [Intervals & Sweep Line](./20-intervals/README.md) | ⭐⭐⭐ | HIGH | Template sheet |
| 21 | [Segment Tree and Fenwick Tree](./21-segment-tree-and-fenwick-tree/README.md) | ⭐⭐⭐⭐ | MEDIUM (P2) | Topic page |
| 22 | [String Algorithms](./22-string-algorithms/README.md) | ⭐⭐⭐⭐ | MEDIUM (P2) | Topic page |
| 23 | [Math and Number Theory](./23-math-and-number-theory/README.md) | ⭐⭐⭐ | HIGH (P1) | Topic page |
| 24 | [Advanced Graphs](./24-advanced-graphs/README.md) | ⭐⭐⭐⭐ | MEDIUM (P2) | Topic page |
| 25 | [Matrix and Simulation](./25-matrix-and-simulation/README.md) | ⭐⭐ | HIGH (P1) | Topic page |
| 26 | [Design Data Structures](./26-design-data-structures/README.md) | ⭐⭐⭐ | HIGH (P1) | Topic page |
| 27 | [Advanced DP](./27-advanced-dp/README.md) | ⭐⭐⭐⭐ | MEDIUM (P2) | Topic page |
| 28 | [Randomized Algorithms](./28-randomized-algorithms/README.md) | ⭐⭐⭐ | MEDIUM (P2) | Topic page |

---

## Interview Mindset Framework

### The 5-Minute Rule
When you see a new problem:
1. **Minute 1-2:** Understand the problem. Repeat it back. Ask clarifying questions.
2. **Minute 2-3:** Identify constraints. What's the input size? This hints at expected complexity.
3. **Minute 3-4:** Pattern recognition. Run the steps in [How to Choose a Pattern](./how-to-choose-a-pattern.md).
4. **Minute 4-5:** Propose approach. Start with brute force, then optimize.

### Constraint-to-Complexity

The table that maps input size to the allowed complexity is in [How to Choose a Pattern, Step 1](./how-to-choose-a-pattern.md#step-1-read-the-constraints).

---

## Pattern Recognition

- Step-by-step procedure and deciding questions: [how-to-choose-a-pattern.md](./how-to-choose-a-pattern.md)
- One-page "if you see... think..." table: [cheatsheet.md](./cheatsheet.md#pattern-recognition-matrix)

---

## Study Schedule

The full day-by-day plan with problems is in [study-plan.md](./study-plan.md). Summary:

| Weeks | Topics |
|-------|--------|
| 0 | Re-entry: Java toolkit, how to choose a pattern, foundations pages, warm-up problems |
| 1–2 | Sliding window, two pointers, fast/slow + linked lists, prefix sum, hashing, binary search |
| 3 | Monotonic stack, backtracking, divide and conquer |
| 4–5 | Trees, graph traversal, topological sort, union find |
| 6–7 | Intervals, greedy, heap, trie, bit manipulation |
| 8 | Shortest paths, MST, mixed recognition drills |
| 9–11 | Dynamic programming |
| 12 | Mock interviews and review |
| 13 | Patterns 23, 25, 26 (P1): math, matrix and simulation, design data structures |
| 14–15 | Patterns 21, 22, 24, 27, 28 (P2 stretch): range trees, string algorithms, advanced graphs, advanced DP, randomized |

---

*Start with [How to Choose a Pattern →](./how-to-choose-a-pattern.md), then [Pattern 01: Sliding Window](./01-sliding-window/README.md)*
