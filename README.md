# DSA Interview Mastery Guide
## Pattern-First Approach to Crack Any Coding Interview

**Target:** Product-based companies (FAANG-level)  
**Focus:** Medium and Hard problems  
**Philosophy:** Build problem-solving intuition, not memorized tricks

---

## 🆕 Quick Links

| Resource | Description |
|----------|-------------|
| 📅 [10-Week Study Plan](./study-plan.md) | Day-by-day structured prep plan |
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
| 03 | [Fast & Slow Pointers](./03-fast-slow-pointers/README.md) | ⭐⭐ | MEDIUM |
| 04 | [Prefix Sum / Difference Array](./04-prefix-sum/README.md) | ⭐⭐ | HIGH |
| 05 | [Hashing & Frequency Maps](./05-hashing-frequency/README.md) | ⭐⭐ | HIGH |
| 06 | [Binary Search on Answer](./06-binary-search-on-answer/README.md) | ⭐⭐⭐ | HIGH |
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

---

## Interview Mindset Framework

### The 5-Minute Rule
When you see a new problem:
1. **Minute 1-2:** Understand the problem. Repeat it back. Ask clarifying questions.
2. **Minute 2-3:** Identify constraints. What's the input size? This hints at expected complexity.
3. **Minute 3-4:** Pattern recognition. Which pattern(s) might apply?
4. **Minute 4-5:** Propose approach. Start with brute force, then optimize.

### Constraint-to-Complexity Cheat Sheet

| Input Size (n) | Expected Complexity | Common Patterns |
|----------------|---------------------|-----------------|
| n ≤ 10 | O(n!), O(2^n) | Backtracking, Brute Force |
| n ≤ 20 | O(2^n), O(n * 2^n) | Bitmask DP, Backtracking |
| n ≤ 100 | O(n³) | Floyd-Warshall, 3D DP |
| n ≤ 1,000 | O(n²) | 2D DP, Nested loops |
| n ≤ 10,000 | O(n²) barely | Optimized n² or n log n |
| n ≤ 100,000 | O(n log n), O(n) | Sorting, Sliding Window, Two Pointers |
| n ≤ 1,000,000 | O(n), O(n log n) | Linear scan, Binary Search |
| n ≤ 10^8 | O(n), O(log n) | Math, Binary Search |
| n ≤ 10^12 | O(log n), O(1) | Math formula, Binary Search |

---

## Quick Pattern Recognition Cheat Sheet

| If you see... | Think... |
|---------------|----------|
| "Subarray", "Substring", "Contiguous" | Sliding Window, Two Pointers |
| "Sorted array" | Binary Search, Two Pointers |
| "All permutations/combinations" | Backtracking |
| "Maximum/Minimum with constraint" | DP, Binary Search on Answer, Greedy |
| "Shortest path" | BFS (unweighted), Dijkstra (weighted) |
| "Connected components" | DFS/BFS, Union Find |
| "Dependency ordering" | Topological Sort |
| "Tree structure" | DFS, BFS, Recursion |
| "Overlapping subproblems" | Dynamic Programming |
| "Make decisions at each step" | Greedy (if locally optimal = globally optimal) |
| "Next greater/smaller element" | Monotonic Stack |
| "Frequency", "Count occurrences" | HashMap |
| "k-th largest/smallest" | Heap, QuickSelect |
| "Cycle detection" | Fast & Slow Pointers, DFS |
| "Top K", "K closest", "K most frequent" | Heap / Priority Queue |
| "Running median", "median from stream" | Two Heaps (max-heap + min-heap) |
| "Prefix", "autocomplete", "starts with", "multi-word grid search" | Trie |
| "Weighted shortest path" | Dijkstra (non-neg weights), Bellman-Ford (neg/K-hops) |
| "Single/missing/duplicate number in O(1) space" | XOR |
| "n ≤ 20 + subset/visited tracking" | Bitmask DP |

---

## Study Schedule Recommendation (8-10 weeks)

### Weeks 1-2: Foundation Patterns
- Sliding Window
- Two Pointers
- Hashing & Frequency Maps
- Prefix Sum

### Weeks 3-4: Search & Stack Patterns
- Binary Search on Answer
- Monotonic Stack/Queue
- Fast & Slow Pointers

### Weeks 5-6: Tree & Graph Patterns
- Tree DFS/BFS
- Graph Traversal
- Topological Sort
- Union Find

### Weeks 7-8: Advanced Patterns
- Recursion & Backtracking
- Divide and Conquer
- Greedy

### Weeks 9-10: Dynamic Programming
- DP Identification
- 1D DP
- 2D DP
- Interval DP
- State Compression

---

*Start with Pattern 01: Sliding Window →*
