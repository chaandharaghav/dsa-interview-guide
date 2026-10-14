# DSA Interview Mastery Guide
## Pattern-First Approach to Crack Any Coding Interview

**Target:** Product-based companies (FAANG-level)  
**Focus:** Medium and Hard problems  
**Philosophy:** Build problem-solving intuition, not memorized tricks

---

## Start Here (Returning to DSA)

1. **[How to Choose a Pattern](./how-to-choose-a-pattern.md)**: the six-step procedure, the 15 deciding questions, decision trees, 12 worked walkthroughs, and 20 recognition drills. This file is the core of the guide.
2. **[Java Toolkit](./java-toolkit.md)**: the Java collections, the Java pitfalls that cost interviews, and a Java template for each core pattern.
3. **[12-Week Study Plan](./study-plan.md)**: Week 0 is a re-entry week. It covers items 1 and 2.
4. Work through the patterns in the order of the study plan. Each pattern file ends with a "Commonly Confused With" table. That table links back to the deciding questions.

---

## 🆕 Quick Links

| Resource | Description |
|----------|-------------|
| 🧭 [How to Choose a Pattern](./how-to-choose-a-pattern.md) | Decide which pattern to use, step by step |
| ☕ [Java Toolkit](./java-toolkit.md) | Java collections, pitfalls, and templates |
| 📅 [12-Week Study Plan](./study-plan.md) | Day-by-day structured prep plan |
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

1. **Study one pattern at a time** - Don't rush. Spend 3-5 days per pattern.
2. **Understand the "why"** - Before looking at solutions, struggle with the brute force.
3. **Practice recognition** - The hardest part is identifying WHICH pattern to use.
4. **Build muscle memory** - Solve 5-10 problems per pattern before moving on.
5. **Review regularly** - Come back to patterns you've completed.

---

## Pattern Index

| # | Pattern | Difficulty | Priority |
|---|---------|------------|----------|
| 01 | [Sliding Window](./01-sliding-window/README.md) | ⭐⭐ | HIGH |
| 02 | [Two Pointers](./02-two-pointers/README.md) | ⭐⭐ | HIGH |
| 03 | [Fast & Slow Pointers + Linked List Techniques](./03-fast-slow-pointers/README.md) | ⭐⭐ | MEDIUM |
| 04 | [Prefix Sum / Difference Array](./04-prefix-sum/README.md) | ⭐⭐ | HIGH |
| 05 | [Hashing & Frequency Maps](./05-hashing-frequency/README.md) | ⭐⭐ | HIGH |
| 06 | [Binary Search (Classic & On Answer)](./06-binary-search-on-answer/README.md) | ⭐⭐⭐ | HIGH |
| 07 | [Monotonic Stack / Queue](./07-monotonic-stack-queue/README.md) | ⭐⭐⭐ | HIGH |
| 08 | [Recursion & Backtracking](./08-recursion-backtracking/README.md) | ⭐⭐⭐ | HIGH |
| 09 | [Divide and Conquer](./09-divide-and-conquer/README.md) | ⭐⭐⭐ | MEDIUM |
| 10 | [Tree DFS / BFS Patterns](./10-tree-patterns/README.md) | ⭐⭐⭐ | HIGH |
| 11 | [Graph Traversal Patterns](./11-graph-traversal/README.md) | ⭐⭐⭐ | HIGH |
| 12 | [Topological Sort](./12-topological-sort/README.md) | ⭐⭐⭐ | MEDIUM |
| 13 | [Union Find](./13-union-find/README.md) | ⭐⭐⭐ | MEDIUM |
| 14 | [Greedy Patterns](./14-greedy/README.md) | ⭐⭐⭐ | HIGH |
| 15 | [Dynamic Programming](./15-dynamic-programming/README.md) | ⭐⭐⭐⭐ | CRITICAL |
| 16 | [Heap / Priority Queue](./16-heap-priority-queue/README.md) | ⭐⭐⭐ | HIGH |
| 17 | [Trie (Prefix Tree)](./17-trie/README.md) | ⭐⭐⭐ | MEDIUM |
| 18 | [Shortest Paths (Dijkstra, Bellman-Ford, Floyd-Warshall)](./18-shortest-paths/README.md) | ⭐⭐⭐ | HIGH |
| 19 | [Bit Manipulation](./19-bit-manipulation/README.md) | ⭐⭐⭐ | MEDIUM |
| 20 | [Intervals & Sweep Line](./20-intervals/README.md) | ⭐⭐⭐ | HIGH |

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
| 0 | Re-entry: Java toolkit, how to choose a pattern, warm-up problems |
| 1–2 | Sliding window, two pointers, fast/slow + linked lists, prefix sum, hashing, binary search |
| 3 | Monotonic stack, backtracking, divide and conquer |
| 4–5 | Trees, graph traversal, topological sort, union find |
| 6–7 | Intervals, greedy, heap, trie, bit manipulation |
| 8 | Shortest paths, MST, mixed recognition drills |
| 9–11 | Dynamic programming |
| 12 | Mock interviews and review |

---

*Start with [How to Choose a Pattern →](./how-to-choose-a-pattern.md), then [Pattern 01: Sliding Window](./01-sliding-window/README.md)*
