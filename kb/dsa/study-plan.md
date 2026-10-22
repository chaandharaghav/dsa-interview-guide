# DSA Interview Prep Plan (12 Core Weeks, Plus 3)

A day-by-day plan to learn the DSA patterns for interviews. Week 0 is a short re-entry week for developers who have not done DSA for a while. Weeks 1 to 12 cover patterns 01 to 20. Week 13 covers the new P1 patterns, and weeks 14 and 15 are stretch weeks for the P2 patterns.

---

## Before You Start

### Prerequisites Checklist
- [ ] Comfortable with Java (read [java-toolkit.md](./java-toolkit.md) for the collections and pitfalls)
- [ ] Understand basic data structures (arrays, linked lists, stacks, queues, trees, graphs)
- [ ] Know Big O notation basics
- [ ] LeetCode account ready
- [ ] 2-3 hours available daily

### Daily Routine Template
| Time | Activity |
|------|----------|
| 30 min | Review previous day's concepts |
| 60 min | Learn new pattern / theory |
| 60 min | Solve 2-3 problems |
| 30 min | Review solutions, note patterns |
| 10 min | From Week 3: classify 3 unseen problems without solving them ([how-to-choose-a-pattern.md](./how-to-choose-a-pattern.md#part-4-how-to-train-recognition)) |

### Rule for Every Problem
Before you write code, write two lines: the pattern you predict, and the deciding question that chose it. Use the six steps in [How to Choose a Pattern](./how-to-choose-a-pattern.md). When your prediction is wrong, add one line to your misrecognition log.

---

## Week 0: Re-entry (2–3 days)

### Day 1: Java for DSA
**Read:** [java-toolkit.md](./java-toolkit.md)

- [ ] Write from memory: `ArrayDeque` as a stack and as a queue, `PriorityQueue` with a comparator, `HashMap.merge`, `TreeMap.floorKey`
- [ ] Read the "Java pitfalls" section. Write each CORRECT version once.

### Day 2: The recognition procedure
**Read:** [how-to-choose-a-pattern.md](./how-to-choose-a-pattern.md) (Part 1 and Part 2)

- [ ] Learn the constraint → complexity table in Step 1
- [ ] Read walkthroughs 1–4 in Part 3. Cover the solution and predict the pattern first.

### Day 2–3: Foundations (as needed)
**Read:** the [foundations](./README.md#foundations) pages for any structure that you do not know well. At minimum, read [Complexity Analysis](./foundations/01-complexity-analysis.md) and [Hash Tables](./foundations/04-hash-tables.md).

- [ ] Answer three `??? question` blocks on each page that you read before you open the model answers

### Day 3: Warm-up
- [ ] LC 1: Two Sum (Easy)
- [ ] LC 20: Valid Parentheses (Easy)
- [ ] LC 704: Binary Search (Easy)
- [ ] LC 206: Reverse Linked List (Easy)
- [ ] LC 104: Maximum Depth of Binary Tree (Easy)

---

## Week 1: Foundation Patterns I

### Day 1-2: Sliding Window
**Read:** [01-sliding-window/README.md](./01-sliding-window/README.md)

**Problems:**
- [ ] LC 643: Maximum Average Subarray I (Easy) - Warm up
- [ ] LC 3: Longest Substring Without Repeating Characters (Medium) ⭐
- [ ] LC 76: Minimum Window Substring (Hard) ⭐⭐

**Key Practice:**
- Fixed window: Build window, then slide
- Variable window: Expand right, shrink left while valid

### Day 3-4: Two Pointers
**Read:** [02-two-pointers/README.md](./02-two-pointers/README.md)

**Problems:**
- [ ] LC 167: Two Sum II - Input Array Is Sorted (Medium)
- [ ] LC 15: 3Sum (Medium) ⭐⭐
- [ ] LC 11: Container With Most Water (Medium) ⭐

**Key Practice:**
- Opposite direction on sorted arrays
- Duplicate skipping technique

### Day 5: Fast & Slow Pointers
**Read:** [03-fast-slow-pointers/README.md](./03-fast-slow-pointers/README.md)

**Problems:**
- [ ] LC 141: Linked List Cycle (Easy)
- [ ] LC 142: Linked List Cycle II (Medium) ⭐
- [ ] LC 287: Find the Duplicate Number (Medium) ⭐
- [ ] LC 92: Reverse Linked List II (Medium) ⭐ - in-place reversal
- [ ] LC 143: Reorder List (Medium) - middle + reverse + merge

### Day 6-7: Review & Practice
- Revisit problems you struggled with
- Solve 2 new problems from each pattern without hints
- [ ] LC 438: Find All Anagrams in a String (Medium)
- [ ] LC 42: Trapping Rain Water (Hard) - combines patterns!

---

## Week 2: Foundation Patterns II

### Day 1-2: Prefix Sum
**Read:** [04-prefix-sum/README.md](./04-prefix-sum/README.md)

**Problems:**
- [ ] LC 303: Range Sum Query - Immutable (Easy)
- [ ] LC 560: Subarray Sum Equals K (Medium) ⭐⭐
- [ ] LC 525: Contiguous Array (Medium) ⭐

**Key Practice:**
- Prefix sum + HashMap technique
- Prefix sum + hashmap works with negative values. The sliding window does not.
- The "exactly k" = "at most k" − "at most k−1" trick is safe for counts of distinct values (LC 992). For sums, it requires all values ≥ 0. It fails on LC 560.

### Day 3-4: Hashing & Frequency Maps
**Read:** [05-hashing-frequency/README.md](./05-hashing-frequency/README.md)

**Problems:**
- [ ] LC 1: Two Sum (Easy) - Classic!
- [ ] LC 49: Group Anagrams (Medium) ⭐
- [ ] LC 128: Longest Consecutive Sequence (Medium) ⭐⭐
- [ ] LC 146: LRU Cache (Medium) ⭐⭐

### Day 5-6: Binary Search (Classic & On Answer)
**Read:** [06-binary-search-on-answer/README.md](./06-binary-search-on-answer/README.md)

**Problems:**
- [ ] LC 34: Find First and Last Position of Element in Sorted Array (Medium) ⭐ - lower/upper bound
- [ ] LC 33: Search in Rotated Sorted Array (Medium) ⭐⭐
- [ ] LC 875: Koko Eating Bananas (Medium) ⭐
- [ ] LC 1011: Capacity To Ship Packages Within D Days (Medium) ⭐
- [ ] LC 410: Split Array Largest Sum (Hard) ⭐⭐

**Key Practice:**
- Define search space bounds
- Write clear verification function

### Day 7: Weekly Review
- [ ] Solve 1 problem from each pattern (timed: 25 min each)
- [ ] Write down pattern recognition signals

---

## Week 3: Stack & Recursion

### Day 1-2: Monotonic Stack
**Read:** [07-monotonic-stack-queue/README.md](./07-monotonic-stack-queue/README.md)

**Problems:**
- [ ] LC 739: Daily Temperatures (Medium) ⭐
- [ ] LC 84: Largest Rectangle in Histogram (Hard) ⭐⭐
- [ ] LC 907: Sum of Subarray Minimums (Medium) ⭐

### Day 3-5: Recursion & Backtracking
**Read:** [08-recursion-backtracking/README.md](./08-recursion-backtracking/README.md)

**Problems:**
- [ ] LC 78: Subsets (Medium) ⭐
- [ ] LC 46: Permutations (Medium) ⭐
- [ ] LC 39: Combination Sum (Medium) ⭐
- [ ] LC 90: Subsets II (Medium) - with duplicates
- [ ] LC 51: N-Queens (Hard) ⭐⭐

**Key Practice:**
- Include/exclude pattern
- Backtracking with undo step

### Day 6-7: Divide and Conquer + Review
**Read:** [09-divide-and-conquer/README.md](./09-divide-and-conquer/README.md)

**Problems:**
- [ ] LC 912: Sort an Array (Medium) - implement merge sort
- [ ] LC 215: Kth Largest Element in an Array (Medium) ⭐

---

## Week 4: Trees

### Day 1-3: Tree DFS Patterns
**Read:** [10-tree-patterns/README.md](./10-tree-patterns/README.md)

**Problems:**
- [ ] LC 104: Maximum Depth of Binary Tree (Easy)
- [ ] LC 112: Path Sum (Easy)
- [ ] LC 543: Diameter of Binary Tree (Easy) ⭐
- [ ] LC 236: Lowest Common Ancestor (Medium) ⭐⭐
- [ ] LC 98: Validate Binary Search Tree (Medium) ⭐
- [ ] LC 124: Binary Tree Maximum Path Sum (Hard) ⭐⭐

### Day 4-5: Tree BFS Patterns
**Problems:**
- [ ] LC 102: Binary Tree Level Order Traversal (Medium) ⭐
- [ ] LC 199: Binary Tree Right Side View (Medium) ⭐
- [ ] LC 297: Serialize and Deserialize Binary Tree (Hard) ⭐

### Day 6-7: Tree Construction & Review
**Problems:**
- [ ] LC 105: Construct Binary Tree from Preorder and Inorder (Medium) ⭐
- [ ] LC 230: Kth Smallest Element in a BST (Medium)
- Review all tree problems

---

## Week 5: Graphs I

### Day 1-3: Graph Traversal
**Read:** [11-graph-traversal/README.md](./11-graph-traversal/README.md)

**Problems:**
- [ ] LC 200: Number of Islands (Medium) ⭐⭐
- [ ] LC 133: Clone Graph (Medium) ⭐
- [ ] LC 994: Rotting Oranges (Medium) ⭐ - Multi-source BFS
- [ ] LC 417: Pacific Atlantic Water Flow (Medium) ⭐

### Day 4-5: Topological Sort
**Read:** [12-topological-sort/README.md](./12-topological-sort/README.md)

**Problems:**
- [ ] LC 207: Course Schedule (Medium) ⭐⭐
- [ ] LC 210: Course Schedule II (Medium) ⭐
- [ ] LC 269: Alien Dictionary (Hard) ⭐⭐

### Day 6-7: Union Find
**Read:** [13-union-find/README.md](./13-union-find/README.md)

**Problems:**
- [ ] LC 547: Number of Provinces (Medium)
- [ ] LC 684: Redundant Connection (Medium) ⭐
- [ ] LC 721: Accounts Merge (Medium) ⭐⭐

---

## Week 6: Greedy & Intervals

### Day 1-2: Intervals & Sweep Line
**Read:** [20-intervals/README.md](./20-intervals/README.md)

**Problems:**
- [ ] LC 56: Merge Intervals (Medium) ⭐⭐
- [ ] LC 57: Insert Interval (Medium) ⭐
- [ ] LC 435: Non-overlapping Intervals (Medium) ⭐ - sort by end
- [ ] LC 452: Minimum Number of Arrows to Burst Balloons (Medium)

### Day 3-4: Greedy Patterns
**Read:** [14-greedy/README.md](./14-greedy/README.md)

**Problems:**
- [ ] LC 55: Jump Game (Medium) ⭐
- [ ] LC 45: Jump Game II (Medium) ⭐
- [ ] LC 134: Gas Station (Medium) ⭐
- [ ] LC 621: Task Scheduler (Medium) ⭐

### Day 5-7: Greedy Practice + Review
- [ ] LC 763: Partition Labels (Medium) ⭐
- [ ] LC 253: Meeting Rooms II (Medium) ⭐
- Review: When does greedy work vs DP? (Q5 in [how-to-choose-a-pattern.md](./how-to-choose-a-pattern.md#q5-can-a-locally-best-choice-need-to-be-undone-later))

---

## Week 7: Heaps, Tries & Bits

### Day 1-3: Heap / Priority Queue
**Read:** [16-heap-priority-queue/README.md](./16-heap-priority-queue/README.md)

**Problems:**
- [ ] LC 215: Kth Largest Element in an Array (Medium) ⭐ - heap vs quickselect
- [ ] LC 347: Top K Frequent Elements (Medium) ⭐⭐ - heap vs bucket sort
- [ ] LC 23: Merge k Sorted Lists (Hard) ⭐⭐
- [ ] LC 295: Find Median from Data Stream (Hard) ⭐⭐ - two heaps

### Day 4-5: Trie
**Read:** [17-trie/README.md](./17-trie/README.md)

**Problems:**
- [ ] LC 208: Implement Trie (Prefix Tree) (Medium) ⭐⭐
- [ ] LC 211: Design Add and Search Words Data Structure (Medium) ⭐
- [ ] LC 212: Word Search II (Hard) ⭐

### Day 6-7: Bit Manipulation
**Read:** [19-bit-manipulation/README.md](./19-bit-manipulation/README.md)

**Problems:**
- [ ] LC 136: Single Number (Easy)
- [ ] LC 191: Number of 1 Bits (Easy)
- [ ] LC 338: Counting Bits (Easy)
- [ ] LC 137: Single Number II (Medium) ⭐

---

## Week 8: Graphs II - Weighted Graphs

### Day 1-4: Shortest Paths
**Read:** [18-shortest-paths/README.md](./18-shortest-paths/README.md)

**Problems:**
- [ ] LC 743: Network Delay Time (Medium) ⭐⭐ - Dijkstra
- [ ] LC 787: Cheapest Flights Within K Stops (Medium) ⭐⭐ - Bellman-Ford with K rounds
- [ ] LC 1631: Path With Minimum Effort (Medium) ⭐ - solve it two ways
- [ ] LC 1368: Minimum Cost to Make at Least One Valid Path in a Grid (Hard) - 0-1 BFS

### Day 5-6: Minimum Spanning Tree & Graph Review
**Problems:**
- [ ] LC 1584: Min Cost to Connect All Points (Medium) ⭐ - Kruskal or Prim
- [ ] LC 127: Word Ladder (Hard) ⭐⭐ - BFS on an implicit graph

### Day 7: Mixed Recognition Review
- [ ] Do all 20 drills in [how-to-choose-a-pattern.md Part 5](./how-to-choose-a-pattern.md#part-5-recognition-drills) without looking at the answers
- [ ] Read your misrecognition log. Find the deciding question that appears most in the log.

---

## Week 9: Dynamic Programming I - Foundation

### Day 1-7: 1D DP
**Read:** [15-dynamic-programming/README.md](./15-dynamic-programming/README.md) (sections 1-7)

**Problems:**
- [ ] LC 70: Climbing Stairs (Easy)
- [ ] LC 198: House Robber (Medium) ⭐
- [ ] LC 213: House Robber II (Medium) ⭐
- [ ] LC 53: Maximum Subarray (Medium) - Kadane's
- [ ] LC 300: Longest Increasing Subsequence (Medium) ⭐⭐
- [ ] LC 322: Coin Change (Medium) ⭐⭐
- [ ] LC 139: Word Break (Medium) ⭐⭐

**Focus:**
- State definition
- Transition relation
- Base cases

---

## Week 10: Dynamic Programming II - Advanced

### Day 1-3: Two Sequence DP
**Problems:**
- [ ] LC 1143: Longest Common Subsequence (Medium) ⭐⭐
- [ ] LC 72: Edit Distance (Medium) ⭐⭐
- [ ] LC 115: Distinct Subsequences (Hard)

### Day 4-5: Knapsack
**Problems:**
- [ ] LC 416: Partition Equal Subset Sum (Medium) ⭐⭐
- [ ] LC 494: Target Sum (Medium) ⭐
- [ ] LC 518: Coin Change II (Medium) ⭐

### Day 6-7: Grid DP
**Problems:**
- [ ] LC 62: Unique Paths (Medium)
- [ ] LC 64: Minimum Path Sum (Medium) ⭐
- [ ] LC 221: Maximal Square (Medium) ⭐⭐

---

## Week 11: Dynamic Programming III - Mastery

### Day 1-2: Interval DP
**Problems:**
- [ ] LC 5: Longest Palindromic Substring (Medium) ⭐
- [ ] LC 516: Longest Palindromic Subsequence (Medium) ⭐

### Day 3-5: State Machine DP
**Problems:**
- [ ] LC 121: Best Time to Buy and Sell Stock (Easy)
- [ ] LC 122: Best Time to Buy and Sell Stock II (Medium)
- [ ] LC 309: Best Time to Buy and Sell Stock with Cooldown (Medium) ⭐⭐
- [ ] LC 188: Best Time to Buy and Sell Stock IV (Hard) ⭐

### Day 6-7: Hard DP
**Problems:**
- [ ] LC 312: Burst Balloons (Hard) ⭐⭐
- [ ] LC 10: Regular Expression Matching (Hard) ⭐

---

## Week 12: Mock Interviews & Review

### Day 1-2: Timed Practice
- Set 45-minute timer
- Pick random medium/hard problem
- Simulate real interview conditions
- Review performance

### Day 3-4: Weak Areas
- Revisit patterns you struggled with
- Re-solve problems you couldn't finish
- Focus on recognition speed

### Day 5-6: Full Mock Interviews
- Use Pramp, Interviewing.io, or practice with a friend
- 2 mock interviews per day
- Get feedback, iterate

### Day 7: Final Review
- Read all cheat sheets
- Review top 20 most-solved problems
- Mental preparation

---

## Week 13: New P1 Patterns

### Day 1-2: Math and Number Theory
**Read:** [Pattern 23](./23-math-and-number-theory/README.md)
- [ ] LC 204: Count Primes (Medium)
- [ ] LC 50: Pow(x, n) (Medium)
- [ ] LC 172: Factorial Trailing Zeroes (Medium)
- [ ] LC 1201: Ugly Number III (Medium)

### Day 3-4: Matrix and Simulation
**Read:** [Pattern 25](./25-matrix-and-simulation/README.md)
- [ ] LC 54: Spiral Matrix (Medium)
- [ ] LC 48: Rotate Image (Medium)
- [ ] LC 73: Set Matrix Zeroes (Medium)
- [ ] LC 289: Game of Life (Medium)

### Day 5-7: Design Data Structures
**Read:** [Pattern 26](./26-design-data-structures/README.md)
- [ ] LC 155: Min Stack (Medium)
- [ ] LC 380: Insert Delete GetRandom O(1) (Medium)
- [ ] LC 981: Time Based Key-Value Store (Medium)
- [ ] LC 295: Find Median from Data Stream (Hard)
- [ ] LC 460: LFU Cache (Hard)

---

## Weeks 14-15: P2 Patterns (Stretch)

Do these weeks if your target companies ask hard problems, or if you have time before your interviews. For each pattern, read the page, then solve the first three Medium and the first Hard problem in its section 8 table.

| Days | Pattern |
|------|---------|
| Week 14, days 1-3 | [Pattern 21: Segment Tree and Fenwick Tree](./21-segment-tree-and-fenwick-tree/README.md) |
| Week 14, days 4-7 | [Pattern 24: Advanced Graphs](./24-advanced-graphs/README.md) |
| Week 15, days 1-3 | [Pattern 27: Advanced DP](./27-advanced-dp/README.md) |
| Week 15, days 4-5 | [Pattern 22: String Algorithms](./22-string-algorithms/README.md) |
| Week 15, days 6-7 | [Pattern 28: Randomized Algorithms](./28-randomized-algorithms/README.md) |

---

## Tracking Progress

### Weekly Checklist
Week 0: [ ] Java Toolkit [ ] How to Choose a Pattern [ ] Foundations [ ] Warm-up
Week 1: [ ] Sliding Window [ ] Two Pointers [ ] Fast/Slow + Linked List Reversal
Week 2: [ ] Prefix Sum [ ] Hashing [ ] Binary Search (Classic & On Answer)
Week 3: [ ] Monotonic Stack [ ] Backtracking [ ] Divide & Conquer
Week 4: [ ] Tree DFS [ ] Tree BFS [ ] Tree Construction
Week 5: [ ] Graph DFS/BFS [ ] Topological Sort [ ] Union Find
Week 6: [ ] Intervals [ ] Greedy
Week 7: [ ] Heap [ ] Trie [ ] Bit Manipulation
Week 8: [ ] Shortest Paths [ ] MST [ ] Recognition Drills
Week 9: [ ] 1D DP [ ] Basic DP Problems
Week 10: [ ] 2D DP [ ] Knapsack [ ] Grid DP
Week 11: [ ] Interval DP [ ] State Machine DP [ ] Hard DP
Week 12: [ ] Mock Interviews [ ] Final Review
Week 13: [ ] Math [ ] Matrix and Simulation [ ] Design Data Structures
Weeks 14-15 (stretch): [ ] Range Trees [ ] Advanced Graphs [ ] Advanced DP [ ] String Algorithms [ ] Randomized

### Problem Count Goals
- Easy: 20-30 problems
- Medium: 80-100 problems ⭐
- Hard: 20-30 problems

**Total: ~150-170 problems**

---

## Problem Difficulty Legend

⭐ = Important, frequently asked
⭐⭐ = Must solve, very common in interviews

---

## Final Tips

1. **Quality over quantity** - Understanding one problem deeply > rushing through many
2. **Time yourself** - Practice with 25-30 min limit for medium, 40-45 min for hard
3. **Explain out loud** - Even when practicing alone
4. **Review weekly** - Don't just move forward; consolidate
5. **Sleep well** - Memory consolidation happens during sleep

---

*You've got this! Consistency beats intensity. Good luck! 🚀*
