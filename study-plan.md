# 10-Week DSA Interview Prep Plan

A structured, day-by-day plan to master DSA patterns for interviews.

---

## Before You Start

### Prerequisites Checklist
- [ ] Comfortable with one programming language (Java or JavaScript in your case)
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
- The "exactly k" = "at most k" - "at most k-1" trick

### Day 3-4: Hashing & Frequency Maps
**Read:** [05-hashing-frequency/README.md](./05-hashing-frequency/README.md)

**Problems:**
- [ ] LC 1: Two Sum (Easy) - Classic!
- [ ] LC 49: Group Anagrams (Medium) ⭐
- [ ] LC 128: Longest Consecutive Sequence (Medium) ⭐⭐
- [ ] LC 146: LRU Cache (Medium) ⭐⭐

### Day 5-6: Binary Search on Answer
**Read:** [06-binary-search-on-answer/README.md](./06-binary-search-on-answer/README.md)

**Problems:**
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

## Week 6: Greedy

### Day 1-4: Greedy Patterns
**Read:** [14-greedy/README.md](./14-greedy/README.md)

**Problems:**
- [ ] LC 56: Merge Intervals (Medium) ⭐⭐
- [ ] LC 435: Non-overlapping Intervals (Medium) ⭐
- [ ] LC 55: Jump Game (Medium) ⭐
- [ ] LC 45: Jump Game II (Medium) ⭐
- [ ] LC 134: Gas Station (Medium) ⭐
- [ ] LC 621: Task Scheduler (Medium) ⭐

### Day 5-7: Greedy Practice + Review
- [ ] LC 763: Partition Labels (Medium) ⭐
- [ ] LC 253: Meeting Rooms II (Medium) ⭐
- Review: When does greedy work vs DP?

---

## Week 7: Dynamic Programming I - Foundation

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

## Week 8: Dynamic Programming II - Advanced

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

## Week 9: Dynamic Programming III - Mastery

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

## Week 10: Mock Interviews & Review

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

## Tracking Progress

### Weekly Checklist
Week 1: [ ] Sliding Window [ ] Two Pointers [ ] Fast/Slow
Week 2: [ ] Prefix Sum [ ] Hashing [ ] Binary Search on Answer
Week 3: [ ] Monotonic Stack [ ] Backtracking [ ] Divide & Conquer
Week 4: [ ] Tree DFS [ ] Tree BFS [ ] Tree Construction
Week 5: [ ] Graph DFS/BFS [ ] Topological Sort [ ] Union Find
Week 6: [ ] Greedy (Intervals) [ ] Greedy (Arrays)
Week 7: [ ] 1D DP [ ] Basic DP Problems
Week 8: [ ] 2D DP [ ] Knapsack [ ] Grid DP
Week 9: [ ] Interval DP [ ] State Machine DP [ ] Hard DP
Week 10: [ ] Mock Interviews [ ] Final Review

### Problem Count Goals
- Easy: 20-30 problems
- Medium: 80-100 problems ⭐
- Hard: 20-30 problems

**Total: ~130-150 problems**

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
