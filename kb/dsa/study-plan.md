# DSA Interview Prep Plan (12 Core Weeks, Plus 3)

A day-by-day plan to learn the DSA patterns for interviews. Week 0 is a short re-entry week for developers who have not done DSA for a while. Weeks 1 to 12 cover patterns 01 to 20. Week 13 covers the new P1 patterns, and weeks 14 and 15 are stretch weeks for the P2 patterns.

---

## Before You Start

### Prerequisites Checklist
- [ ] Comfortable with Java (read [java-toolkit.md](./java-toolkit.md) for the collections and pitfalls)
- [ ] Understand basic data structures (arrays, linked lists, stacks, queues, trees, graphs). If not, read the [foundations](./README.md#foundations) pages in Week 0
- [ ] Can trace and write a recursive method. If not, read [Recursion](./foundations/09-recursion.md) before Week 3
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

### How to Study One Pattern
Patterns 01 to 20 are lesson pages. For each one, follow the [study loop](../mastery-tracker.md#the-study-loop-for-one-topic): attempt the anchor problem first, read the derivation, trace by hand, explain aloud, write the code from an empty file, and do the practice ladder with the hints closed. Record your level in [My Progress](../my-progress.md). The problem lists in the weeks below are extra practice after the ladder of the page.

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
**Read:** the [foundations](./README.md#foundations) pages for any structure that you do not know well. At minimum, read [Complexity Analysis](./foundations/01-complexity-analysis.md), [Hash Tables](./foundations/04-hash-tables.md), and [Recursion](./foundations/09-recursion.md).

- [ ] Answer three `??? question` blocks on each page that you read before you open the model answers

### Day 3: Warm-up
- [ ] [LC 1: Two Sum](https://leetcode.com/problems/two-sum/) (Easy)
- [ ] [LC 20: Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) (Easy)
- [ ] [LC 704: Binary Search](https://leetcode.com/problems/binary-search/) (Easy)
- [ ] [LC 206: Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) (Easy)
- [ ] [LC 104: Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) (Easy)

---

## Week 1: Foundation Patterns I

### Day 1-2: Sliding Window
**Read:** [01-sliding-window/README.md](./01-sliding-window/README.md)

**Problems:**
- [ ] [LC 643: Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) (Easy) - Warm up
- [ ] [LC 3: Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) (Medium) ⭐
- [ ] [LC 76: Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) (Hard) ⭐⭐

**Key Practice:**
- Fixed window: Build window, then slide
- Variable window: Expand right, shrink left while valid

### Day 3-4: Two Pointers
**Read:** [02-two-pointers/README.md](./02-two-pointers/README.md)

**Problems:**
- [ ] [LC 167: Two Sum II - Input Array Is Sorted](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) (Medium)
- [ ] [LC 15: 3Sum](https://leetcode.com/problems/3sum/) (Medium) ⭐⭐
- [ ] [LC 11: Container With Most Water](https://leetcode.com/problems/container-with-most-water/) (Medium) ⭐

**Key Practice:**
- Opposite direction on sorted arrays
- Duplicate skipping technique

### Day 5: Fast & Slow Pointers
**Read:** [03-fast-slow-pointers/README.md](./03-fast-slow-pointers/README.md)

**Problems:**
- [ ] [LC 141: Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) (Easy)
- [ ] [LC 142: Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/) (Medium) ⭐
- [ ] [LC 287: Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) (Medium) ⭐
- [ ] [LC 92: Reverse Linked List II](https://leetcode.com/problems/reverse-linked-list-ii/) (Medium) ⭐ - in-place reversal
- [ ] [LC 143: Reorder List](https://leetcode.com/problems/reorder-list/) (Medium) - middle + reverse + merge

### Day 6-7: Review & Practice
- Revisit problems you struggled with
- Solve 2 new problems from each pattern without hints
- [ ] [LC 438: Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) (Medium)
- [ ] [LC 42: Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) (Hard) - combines patterns!

---

## Week 2: Foundation Patterns II

### Day 1-2: Prefix Sum
**Read:** [04-prefix-sum/README.md](./04-prefix-sum/README.md)

**Problems:**
- [ ] [LC 303: Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/) (Easy)
- [ ] [LC 560: Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) (Medium) ⭐⭐
- [ ] [LC 525: Contiguous Array](https://leetcode.com/problems/contiguous-array/) (Medium) ⭐

**Key Practice:**
- Prefix sum + HashMap technique
- Prefix sum + hashmap works with negative values. The sliding window does not.
- The "exactly k" = "at most k" − "at most k−1" trick is safe for counts of distinct values ([LC 992](https://leetcode.com/problems/subarrays-with-k-different-integers/)). For sums, it requires all values ≥ 0. It fails on [LC 560](https://leetcode.com/problems/subarray-sum-equals-k/).

### Day 3-4: Hashing & Frequency Maps
**Read:** [05-hashing-frequency/README.md](./05-hashing-frequency/README.md)

**Problems:**
- [ ] [LC 1: Two Sum](https://leetcode.com/problems/two-sum/) (Easy) - Classic!
- [ ] [LC 49: Group Anagrams](https://leetcode.com/problems/group-anagrams/) (Medium) ⭐
- [ ] [LC 128: Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/) (Medium) ⭐⭐
- [ ] [LC 146: LRU Cache](https://leetcode.com/problems/lru-cache/) (Medium) ⭐⭐

### Day 5-6: Binary Search (Classic & On Answer)
**Read:** [06-binary-search-on-answer/README.md](./06-binary-search-on-answer/README.md)

**Problems:**
- [ ] [LC 34: Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) (Medium) ⭐ - lower/upper bound
- [ ] [LC 33: Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) (Medium) ⭐⭐
- [ ] [LC 875: Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) (Medium) ⭐
- [ ] [LC 1011: Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) (Medium) ⭐
- [ ] [LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) (Hard) ⭐⭐

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
- [ ] [LC 739: Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) (Medium) ⭐
- [ ] [LC 84: Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) (Hard) ⭐⭐
- [ ] [LC 907: Sum of Subarray Minimums](https://leetcode.com/problems/sum-of-subarray-minimums/) (Medium) ⭐

### Day 3-5: Recursion & Backtracking
**Read:** [08-recursion-backtracking/README.md](./08-recursion-backtracking/README.md)

**Problems:**
- [ ] [LC 78: Subsets](https://leetcode.com/problems/subsets/) (Medium) ⭐
- [ ] [LC 46: Permutations](https://leetcode.com/problems/permutations/) (Medium) ⭐
- [ ] [LC 39: Combination Sum](https://leetcode.com/problems/combination-sum/) (Medium) ⭐
- [ ] [LC 90: Subsets II](https://leetcode.com/problems/subsets-ii/) (Medium) - with duplicates
- [ ] [LC 51: N-Queens](https://leetcode.com/problems/n-queens/) (Hard) ⭐⭐

**Key Practice:**
- Include/exclude pattern
- Backtracking with undo step

### Day 6-7: Divide and Conquer + Review
**Read:** [09-divide-and-conquer/README.md](./09-divide-and-conquer/README.md)

**Problems:**
- [ ] [LC 912: Sort an Array](https://leetcode.com/problems/sort-an-array/) (Medium) - implement merge sort
- [ ] [LC 215: Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) (Medium) ⭐

---

## Week 4: Trees

### Day 1-3: Tree DFS Patterns
**Read:** [10-tree-patterns/README.md](./10-tree-patterns/README.md)

**Problems:**
- [ ] [LC 104: Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) (Easy)
- [ ] [LC 112: Path Sum](https://leetcode.com/problems/path-sum/) (Easy)
- [ ] [LC 543: Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) (Easy) ⭐
- [ ] [LC 236: Lowest Common Ancestor](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/) (Medium) ⭐⭐
- [ ] [LC 98: Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) (Medium) ⭐
- [ ] [LC 124: Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) (Hard) ⭐⭐

### Day 4-5: Tree BFS Patterns
**Problems:**
- [ ] [LC 102: Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) (Medium) ⭐
- [ ] [LC 199: Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/) (Medium) ⭐
- [ ] [LC 297: Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/) (Hard) ⭐

### Day 6-7: Tree Construction & Review
**Problems:**
- [ ] [LC 105: Construct Binary Tree from Preorder and Inorder](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) (Medium) ⭐
- [ ] [LC 230: Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) (Medium)
- Review all tree problems

---

## Week 5: Graphs I

### Day 1-3: Graph Traversal
**Read:** [11-graph-traversal/README.md](./11-graph-traversal/README.md)

**Problems:**
- [ ] [LC 200: Number of Islands](https://leetcode.com/problems/number-of-islands/) (Medium) ⭐⭐
- [ ] [LC 133: Clone Graph](https://leetcode.com/problems/clone-graph/) (Medium) ⭐
- [ ] [LC 994: Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) (Medium) ⭐ - Multi-source BFS
- [ ] [LC 417: Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/) (Medium) ⭐

### Day 4-5: Topological Sort
**Read:** [12-topological-sort/README.md](./12-topological-sort/README.md)

**Problems:**
- [ ] [LC 207: Course Schedule](https://leetcode.com/problems/course-schedule/) (Medium) ⭐⭐
- [ ] [LC 210: Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) (Medium) ⭐
- [ ] [LC 269: Alien Dictionary](https://leetcode.com/problems/alien-dictionary/) (premium) (Hard) ⭐⭐

### Day 6-7: Union Find
**Read:** [13-union-find/README.md](./13-union-find/README.md)

**Problems:**
- [ ] [LC 547: Number of Provinces](https://leetcode.com/problems/number-of-provinces/) (Medium)
- [ ] [LC 684: Redundant Connection](https://leetcode.com/problems/redundant-connection/) (Medium) ⭐
- [ ] [LC 721: Accounts Merge](https://leetcode.com/problems/accounts-merge/) (Medium) ⭐⭐

---

## Week 6: Greedy & Intervals

### Day 1-2: Intervals & Sweep Line
**Read:** [20-intervals/README.md](./20-intervals/README.md)

**Problems:**
- [ ] [LC 56: Merge Intervals](https://leetcode.com/problems/merge-intervals/) (Medium) ⭐⭐
- [ ] [LC 57: Insert Interval](https://leetcode.com/problems/insert-interval/) (Medium) ⭐
- [ ] [LC 435: Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) (Medium) ⭐ - sort by end
- [ ] [LC 452: Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) (Medium)

### Day 3-4: Greedy Patterns
**Read:** [14-greedy/README.md](./14-greedy/README.md)

**Problems:**
- [ ] [LC 55: Jump Game](https://leetcode.com/problems/jump-game/) (Medium) ⭐
- [ ] [LC 45: Jump Game II](https://leetcode.com/problems/jump-game-ii/) (Medium) ⭐
- [ ] [LC 134: Gas Station](https://leetcode.com/problems/gas-station/) (Medium) ⭐
- [ ] [LC 621: Task Scheduler](https://leetcode.com/problems/task-scheduler/) (Medium) ⭐

### Day 5-7: Greedy Practice + Review
- [ ] [LC 763: Partition Labels](https://leetcode.com/problems/partition-labels/) (Medium) ⭐
- [ ] [LC 253: Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) (premium) (Medium) ⭐
- Review: When does greedy work vs DP? (Q5 in [how-to-choose-a-pattern.md](./how-to-choose-a-pattern.md#q5-can-a-locally-best-choice-need-to-be-undone-later))

---

## Week 7: Heaps, Tries & Bits

### Day 1-3: Heap / Priority Queue
**Read:** [16-heap-priority-queue/README.md](./16-heap-priority-queue/README.md)

**Problems:**
- [ ] [LC 215: Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) (Medium) ⭐ - heap vs quickselect
- [ ] [LC 347: Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) (Medium) ⭐⭐ - heap vs bucket sort
- [ ] [LC 23: Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) (Hard) ⭐⭐
- [ ] [LC 295: Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) (Hard) ⭐⭐ - two heaps

### Day 4-5: Trie
**Read:** [17-trie/README.md](./17-trie/README.md)

**Problems:**
- [ ] [LC 208: Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) (Medium) ⭐⭐
- [ ] [LC 211: Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/) (Medium) ⭐
- [ ] [LC 212: Word Search II](https://leetcode.com/problems/word-search-ii/) (Hard) ⭐

### Day 6-7: Bit Manipulation
**Read:** [19-bit-manipulation/README.md](./19-bit-manipulation/README.md)

**Problems:**
- [ ] [LC 136: Single Number](https://leetcode.com/problems/single-number/) (Easy)
- [ ] [LC 191: Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/) (Easy)
- [ ] [LC 338: Counting Bits](https://leetcode.com/problems/counting-bits/) (Easy)
- [ ] [LC 137: Single Number II](https://leetcode.com/problems/single-number-ii/) (Medium) ⭐

---

## Week 8: Graphs II - Weighted Graphs

### Day 1-4: Shortest Paths
**Read:** [18-shortest-paths/README.md](./18-shortest-paths/README.md)

**Problems:**
- [ ] [LC 743: Network Delay Time](https://leetcode.com/problems/network-delay-time/) (Medium) ⭐⭐ - Dijkstra
- [ ] [LC 787: Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/) (Medium) ⭐⭐ - Bellman-Ford with K rounds
- [ ] [LC 1631: Path With Minimum Effort](https://leetcode.com/problems/path-with-minimum-effort/) (Medium) ⭐ - solve it two ways
- [ ] [LC 1368: Minimum Cost to Make at Least One Valid Path in a Grid](https://leetcode.com/problems/minimum-cost-to-make-at-least-one-valid-path-in-a-grid/) (Hard) - 0-1 BFS

### Day 5-6: Minimum Spanning Tree & Graph Review
**Problems:**
- [ ] [LC 1584: Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/) (Medium) ⭐ - Kruskal or Prim
- [ ] [LC 127: Word Ladder](https://leetcode.com/problems/word-ladder/) (Hard) ⭐⭐ - BFS on an implicit graph

### Day 7: Mixed Recognition Review
- [ ] Do all 20 drills in [how-to-choose-a-pattern.md Part 5](./how-to-choose-a-pattern.md#part-5-recognition-drills) without looking at the answers
- [ ] Read your misrecognition log. Find the deciding question that appears most in the log.

---

## Week 9: Dynamic Programming I - Foundation

### Day 1-7: 1D DP
**Read:** [15-dynamic-programming/README.md](./15-dynamic-programming/README.md) (sections 1-7)

**Problems:**
- [ ] [LC 70: Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) (Easy)
- [ ] [LC 198: House Robber](https://leetcode.com/problems/house-robber/) (Medium) ⭐
- [ ] [LC 213: House Robber II](https://leetcode.com/problems/house-robber-ii/) (Medium) ⭐
- [ ] [LC 53: Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) (Medium) - Kadane's
- [ ] [LC 300: Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) (Medium) ⭐⭐
- [ ] [LC 322: Coin Change](https://leetcode.com/problems/coin-change/) (Medium) ⭐⭐
- [ ] [LC 139: Word Break](https://leetcode.com/problems/word-break/) (Medium) ⭐⭐

**Focus:**
- State definition
- Transition relation
- Base cases

---

## Week 10: Dynamic Programming II - Advanced

### Day 1-3: Two Sequence DP
**Problems:**
- [ ] [LC 1143: Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) (Medium) ⭐⭐
- [ ] [LC 72: Edit Distance](https://leetcode.com/problems/edit-distance/) (Medium) ⭐⭐
- [ ] [LC 115: Distinct Subsequences](https://leetcode.com/problems/distinct-subsequences/) (Hard)

### Day 4-5: Knapsack
**Problems:**
- [ ] [LC 416: Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/) (Medium) ⭐⭐
- [ ] [LC 494: Target Sum](https://leetcode.com/problems/target-sum/) (Medium) ⭐
- [ ] [LC 518: Coin Change II](https://leetcode.com/problems/coin-change-ii/) (Medium) ⭐

### Day 6-7: Grid DP
**Problems:**
- [ ] [LC 62: Unique Paths](https://leetcode.com/problems/unique-paths/) (Medium)
- [ ] [LC 64: Minimum Path Sum](https://leetcode.com/problems/minimum-path-sum/) (Medium) ⭐
- [ ] [LC 221: Maximal Square](https://leetcode.com/problems/maximal-square/) (Medium) ⭐⭐

---

## Week 11: Dynamic Programming III - Mastery

### Day 1-2: Interval DP
**Problems:**
- [ ] [LC 5: Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) (Medium) ⭐
- [ ] [LC 516: Longest Palindromic Subsequence](https://leetcode.com/problems/longest-palindromic-subsequence/) (Medium) ⭐

### Day 3-5: State Machine DP
**Problems:**
- [ ] [LC 121: Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) (Easy)
- [ ] [LC 122: Best Time to Buy and Sell Stock II](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-ii/) (Medium)
- [ ] [LC 309: Best Time to Buy and Sell Stock with Cooldown](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/) (Medium) ⭐⭐
- [ ] [LC 188: Best Time to Buy and Sell Stock IV](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iv/) (Hard) ⭐

### Day 6-7: Hard DP
**Problems:**
- [ ] [LC 312: Burst Balloons](https://leetcode.com/problems/burst-balloons/) (Hard) ⭐⭐
- [ ] [LC 10: Regular Expression Matching](https://leetcode.com/problems/regular-expression-matching/) (Hard) ⭐

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
- [ ] [LC 204: Count Primes](https://leetcode.com/problems/count-primes/) (Medium)
- [ ] [LC 50: Pow(x, n)](https://leetcode.com/problems/powx-n/) (Medium)
- [ ] [LC 172: Factorial Trailing Zeroes](https://leetcode.com/problems/factorial-trailing-zeroes/) (Medium)
- [ ] [LC 1201: Ugly Number III](https://leetcode.com/problems/ugly-number-iii/) (Medium)

### Day 3-4: Matrix and Simulation
**Read:** [Pattern 25](./25-matrix-and-simulation/README.md)
- [ ] [LC 54: Spiral Matrix](https://leetcode.com/problems/spiral-matrix/) (Medium)
- [ ] [LC 48: Rotate Image](https://leetcode.com/problems/rotate-image/) (Medium)
- [ ] [LC 73: Set Matrix Zeroes](https://leetcode.com/problems/set-matrix-zeroes/) (Medium)
- [ ] [LC 289: Game of Life](https://leetcode.com/problems/game-of-life/) (Medium)

### Day 5-7: Design Data Structures
**Read:** [Pattern 26](./26-design-data-structures/README.md)
- [ ] [LC 155: Min Stack](https://leetcode.com/problems/min-stack/) (Medium)
- [ ] [LC 380: Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/) (Medium)
- [ ] [LC 981: Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/) (Medium)
- [ ] [LC 295: Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) (Hard)
- [ ] [LC 460: LFU Cache](https://leetcode.com/problems/lfu-cache/) (Hard)

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
