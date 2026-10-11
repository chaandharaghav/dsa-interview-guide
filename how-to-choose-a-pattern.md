# How to Choose a Pattern

## A Repeatable Procedure for "Which Technique Do I Use Here?"

Keyword tables ("subarray → sliding window") fail on the problems that matter. Many medium and hard problems contain the keyword of one pattern and need a different one. This file replaces keyword matching with a fixed sequence of questions. Each question removes candidate patterns. The questions that separate similar patterns are in [Step 4](#step-4-ask-the-deciding-questions). Those questions do most of the work.

Use this file in two ways:

1. **During practice:** run the six steps on every new problem before you write code. Write your answer to each step in one line.
2. **During review:** when you chose the wrong pattern, find the step where your reasoning went wrong. Record it in your misrecognition log ([Part 4](#part-4-how-to-train-recognition)).

---

## Contents

- [Part 1: The Six-Step Procedure](#part-1-the-six-step-procedure)
  - [Step 1: Read the constraints](#step-1-read-the-constraints)
  - [Step 2: Name the input shape](#step-2-name-the-input-shape)
  - [Step 3: Name the question type](#step-3-name-the-question-type)
  - [Step 4: Ask the deciding questions](#step-4-ask-the-deciding-questions)
  - [Step 5: If nothing matches, find the repeated work](#step-5-if-nothing-matches-find-the-repeated-work)
  - [Step 6: Check the choice before you code](#step-6-check-the-choice-before-you-code)
- [Part 2: Decision Trees](#part-2-decision-trees)
- [Part 3: Worked Walkthroughs (the first guess is wrong)](#part-3-worked-walkthroughs)
- [Part 4: How to Train Recognition](#part-4-how-to-train-recognition)
- [Part 5: Recognition Drills](#part-5-recognition-drills)

---

# Part 1: The Six-Step Procedure

```
┌──────────────────────────────────────────────────────────────────────┐
│ 1. CONSTRAINTS     n, value range, q  →  time budget (e.g. O(n log n))│
│ 2. INPUT SHAPE     array? sorted? string? tree? graph? intervals?    │
│ 3. QUESTION TYPE   count? longest? min/max? all solutions? order?    │
│        → intersect: candidates from 2 ∩ candidates from 3            │
│ 4. DECIDING Qs     negatives? contiguous? monotonic? weights? ...    │
│        → one or two patterns remain                                  │
│ 5. NOTHING FITS?   brute force → find the repeated work → remove it  │
│ 6. CHECK           complexity fits budget? trace example? greedy     │
│                    counterexample?                                   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Step 1: Read the Constraints

The constraints tell you the time budget before you think about the algorithm. For budgeting, assume about 10^8 simple operations per second. This figure is an assumption, not a measurement. Most online judges allow 1–2 seconds per test.

### Size of n → allowed complexity

| n up to | Allowed complexity | Patterns that fit |
|---------|-------------------|-------------------|
| 10–12 | O(n!), O(n · n!) | Permutations, backtracking |
| 20–25 | O(2^n), O(n · 2^n) | Subsets, bitmask DP, meet in the middle (n ≤ 40) |
| 100–500 | O(n³) | Floyd-Warshall, interval DP, 3 nested loops |
| 1,000–5,000 | O(n²) | 2D DP, all pairs, O(n²) LIS |
| 10^5–10^6 | O(n log n) or O(n) | Sorting, heap, binary search, sliding window, two pointers, prefix sum, monotonic stack, union find, BFS/DFS |
| 10^9 and more | O(log n) or O(1) | Binary search on the answer, math formula, fast exponentiation |

### Other constraint signals

| Signal in the statement | What it tells you |
|-------------------------|-------------------|
| `n ≤ 20` together with "visit all", "assign each" | The state includes a subset → bitmask DP or BFS with a bitmask |
| `n ≤ 10^5` but values up to `10^9` | Do not index arrays by value. Use a hashmap, sorting, or coordinate compression. Binary search on the value range is possible. |
| Values are in `[1, n]` or `[0, n]` | The array index can act as a hash. See cyclic sort in [Pattern 05, Variation F](./05-hashing-frequency/README.md). |
| `q` queries, `q` up to 10^5 | Precompute once, answer each query in O(1) or O(log n): prefix sum, sparse structure, union find |
| "Return the answer modulo 10^9 + 7" | The answer is a count that grows exponentially → counting DP or combinatorics. Backtracking cannot enumerate this many results. |
| "Return all ..." | The output size sets the lower bound. Backtracking is expected. Do not try to beat the output size. |
| "Return any valid ..." | A greedy or constructive solution can be enough. Check it with Q5. |
| "O(1) extra space", "do not modify the array" | Rules out hashmaps → two pointers, XOR, Floyd cycle detection, Boyer-Moore, in-place index marking |
| Values can be negative | Window sums stop being monotonic → sliding window on sums fails. See [Q2](#q2-can-values-be-negative). |
| Grid up to 10^3 × 10^3 | 10^6 cells → each cell can be visited a constant number of times (BFS/DFS/DP). |

---

## Step 2: Name the Input Shape

Write the shape in one phrase: "unsorted int array", "two strings", "directed graph as edge list", "grid of chars". Each shape has a short list of candidate patterns.

| Input shape | Candidate patterns |
|-------------|-------------------|
| Sorted array | Binary search, two pointers (opposite ends), merge step |
| Unsorted array | Hashmap, sort first, prefix sum, sliding window, monotonic stack, heap, DP |
| Array with values in `[1..n]` | Cyclic sort / index marking, Floyd cycle (LC 287), XOR |
| One string | Sliding window, hashmap counts, two pointers (palindromes), DP (palindromic substrings, partitions), stack (brackets) |
| Two strings or two sequences | 2D DP (LCS, edit distance), two pointers (subsequence check), sliding window + counts (anagram in string) |
| Linked list | Fast/slow pointers, in-place reversal, dummy node, merge |
| Binary tree | DFS (path, height, subtree aggregates), BFS (levels, min depth) |
| Binary search tree | In-order traversal = sorted order, range pruning |
| Graph given as edges | Build adjacency list → BFS/DFS, topological sort, union find, shortest paths, MST |
| Grid | Grid BFS/DFS (each cell is a node), grid DP (only right/down moves), Dijkstra (cell costs) |
| Implicit graph (states and moves) | BFS over states: word ladder, lock combinations, puzzles |
| Intervals `[start, end]` | Sort by start (merge), sort by end (greedy selection), sweep line, heap of end times |
| Stream of values | Heap, two heaps (median), reservoir sampling, running counters |
| Set of words / dictionary | Trie (prefix queries), hashset (exact lookup), BFS (word ladder) |
| Single integer `n` | Math, bit manipulation, DP on `n`, binary search on answer |

---

## Step 3: Name the Question Type

Write what the output is: one boolean, one number, one subarray, a list of all solutions, an ordering.

| Question type | Candidate patterns |
|---------------|-------------------|
| Does a pair / triple exist with property X? | Hashmap (unsorted), two pointers (sorted or sortable) |
| Count subarrays with property X | Prefix sum + hashmap, sliding window `atMost(k) − atMost(k−1)`, monotonic stack (contribution counting) |
| Longest / shortest contiguous segment with property X | Sliding window (if the property is monotonic), prefix sum + hashmap (first occurrence) |
| Min or max value over choices (one optimal number) | DP, greedy, binary search on answer |
| "Minimize the maximum" / "maximize the minimum" | Binary search on answer + greedy check |
| k-th smallest / top k / k closest | Heap of size k, quickselect, bucket sort, binary search on value |
| All permutations / combinations / partitions | Backtracking |
| Number of ways | DP (counting), combinatorics |
| Minimum number of steps / moves | BFS (equal step cost), Dijkstra (different costs), DP (if moves only go forward) |
| An order that respects dependencies | Topological sort |
| Are A and B connected? How many groups? | DFS/BFS, union find |
| Next greater / previous smaller per element | Monotonic stack |
| Max / min of every window | Monotonic deque |
| Many range-sum queries | Prefix sum (static), difference array (all updates before queries), Fenwick / segment tree (updates and queries mixed) |
| Running median | Two heaps |
| Words that start with a prefix | Trie |

**Intersect the two lists.** Take the candidates from Step 2 and keep only those that also appear in Step 3. In most problems, two to four candidates remain. Step 4 decides between them.

---

## Step 4: Ask the Deciding Questions

Each question below splits two or more patterns that look alike. Ask only the questions that apply to your remaining candidates.

### Q1. Contiguous or not?

- **Contiguous** (subarray, substring, consecutive, window) → sliding window, prefix sum, Kadane, monotonic stack.
- **Not contiguous** (subsequence, subset, any elements) → DP, backtracking, sorting, hashmap.
- **Why it matters:** sliding window and prefix sum depend on the elements being adjacent. A subsequence can skip elements, so no window exists.
- **Example:** LC 3 (longest substring without repeats) → sliding window. LC 300 (longest increasing subsequence) → DP.

### Q2. Can values be negative?

- **All values ≥ 0** → when the window grows, the sum cannot decrease. That property makes sliding window on sums correct.
- **Negatives allowed** → adding an element can decrease the sum. The window cannot decide when to shrink. Use prefix sum + hashmap (exact sum, LC 560) or prefix sum + monotonic deque (sum ≥ K, LC 862).
- **Test:** "If I add one more element, does the property only move in one direction?" If no, sliding window fails.

### Q3. Is the input sorted, or can I sort it?

- **Sorted already** → binary search, two pointers.
- **Unsorted, and order does not matter for the answer** → sort first (O(n log n)), then two pointers or greedy. Example: 3Sum (LC 15).
- **Unsorted, and the answer needs original positions or original order** → do not sort. Use a hashmap (Two Sum, LC 1, returns indices) or a structure that keeps order (monotonic stack).
- **Test:** "Does the answer change if I shuffle the input?" If no, sorting is allowed.

### Q4. All solutions, or one number?

- **List every solution** → backtracking. The output can be exponential, so no faster method exists.
- **Count the solutions, or find the best one** → DP, if subproblems repeat (Q6). Counting with backtracking is too slow when the count is large.
- **Example:** LC 39 (list all combinations that sum to target) → backtracking. LC 518 (number of combinations) → DP.

### Q5. Can a locally best choice need to be undone later?

- **No: the best local choice is always part of some optimal answer** → greedy.
- **Yes, or you cannot prove "no"** → DP.
- **Test:** find a small counterexample in 2 minutes. Coin change with coins `{1, 3, 4}` and amount `6`: greedy takes `4 + 1 + 1` (3 coins), but `3 + 3` uses 2 coins. Greedy fails → DP (LC 322).
- **Sign that greedy works:** an exchange argument exists. "If an optimal answer does not use my greedy choice, I can swap my choice in without making the answer worse." Example: for interval scheduling, choose the interval that ends first (LC 435).

### Q6. Do subproblems repeat?

- **Write the brute-force recursion.** List its arguments. If the same arguments appear on different branches, the subproblems overlap → memoize → DP. The arguments are the DP state.
- **Subproblems are independent** (left half and right half share nothing) → divide and conquer (merge sort, tree recursion).
- **No repeated subproblems, and you must try every branch** → backtracking with pruning.

### Q7. Is feasibility monotonic in the answer?

- Rephrase the problem as "Can I achieve answer X?". Call that check `feasible(X)`.
- **Minimization:** if `feasible(X)` implies `feasible(X+1)`, the results look like `F F F T T T`. Binary search for the first `T`.
- **Maximization:** if `feasible(X)` implies `feasible(X−1)`, the results look like `T T T F F F`. Binary search for the last `T`.
- **Requirements:** `feasible(X)` must run fast (for example, an O(n) greedy pass), and the answer range must be bounded.
- **Example:** Koko eating bananas (LC 875). If speed `s` finishes in time, every speed above `s` also finishes. Binary search the speed.

### Q8. What are the edge weights?

| Edge weights | Algorithm | Complexity |
|--------------|-----------|------------|
| None (every step costs 1) | BFS | O(V + E) |
| Only 0 or 1 | 0-1 BFS with a deque | O(V + E) |
| Non-negative | Dijkstra with a min-heap | O((V + E) log V) |
| Negative edges, or "at most K edges" | Bellman-Ford (K rounds for the K-edge limit) | O(V · E) or O(K · E) |
| All pairs, V ≤ ~400 | Floyd-Warshall | O(V³) |
| Graph is a DAG | Topological order + relaxation | O(V + E) |

- **Not a shortest-path problem:** "connect all nodes with minimum total cost" asks for a minimum spanning tree (Kruskal or Prim), not a shortest path.

### Q9. Is the graph fixed, or do edges arrive over time?

- **Fixed graph, one question** → DFS or BFS.
- **Edges arrive one by one, and you ask "connected yet?" after each** → union find. Example: LC 684 (redundant connection), LC 1101 (earliest moment everyone is connected).
- **Edges are removed** → union find cannot delete edges. Reverse the time order: start from the final graph and add the edges back in reverse order (LC 803).

### Q10. Do you need an order, or only "is there a cycle"?

- **Need a valid order** → topological sort (Kahn's algorithm with in-degrees, or DFS post-order reversed).
- **Only need yes/no for a cycle in a directed graph** → topological sort (cycle if fewer than V nodes are output) or DFS with three colors.
- **Cycle in an undirected graph** → DFS that ignores the parent edge, or union find (a cycle exists if an edge joins two nodes with the same root).

### Q11. Top k: which tool?

| Situation | Tool | Complexity |
|-----------|------|------------|
| Data arrives as a stream, or k is much smaller than n | Heap of size k | O(n log k) |
| All data is available, average case is enough | Quickselect | O(n) average, O(n²) worst |
| Values or frequencies are bounded by n | Bucket sort | O(n) |
| k is close to n, or code simplicity matters | Sort | O(n log n) |
| k-th smallest over a huge implicit set (e.g. a multiplication table) | Binary search on the value + count | O(n log V) |

### Q12. Nearest greater/smaller: per element or per window?

- **For each element, the next (or previous) greater or smaller element** → monotonic stack. LC 739, LC 496.
- **The max or min of every window of size k** → monotonic deque. LC 239.
- **The k-th largest of a changing set, or arbitrary deletions** → heap or TreeMap.

### Q13. Prefix queries or exact lookups?

- **Only "is this exact word present?"** → hashset. It is simpler than a trie and has the same complexity.
- **"Which words start with this prefix?", wildcard search, or matching many words against a grid** → trie.
- **Maximum XOR of two numbers** → a trie over the bits of each number (LC 421).

### Q14. Is O(1) extra space required?

When the problem forbids extra space, the usual hashmap answer is not allowed. Use one of these:

| Problem shape | O(1)-space tool |
|---------------|-----------------|
| Every value appears twice except one | XOR all values |
| Majority element (more than n/2) | Boyer-Moore voting |
| Values in `[1..n]`, find missing or duplicate | Cyclic sort, or mark index `v−1` as negative |
| Duplicate in `[1..n]`, array is read-only | Floyd cycle detection (LC 287) |
| Pair sum in a sorted array | Two pointers |
| Cycle in a linked list | Fast and slow pointers |

### Q15. Range queries: are there updates?

- **No updates** → prefix sum. O(1) per query.
- **All updates come before all queries** → difference array. Apply each update in O(1), then run one prefix sum.
- **Updates and queries are mixed** → Fenwick tree (binary indexed tree) or segment tree. O(log n) per operation. These two structures are rare in interviews. Know that they exist and what they cost.

---

## Step 5: If Nothing Matches, Find the Repeated Work

Sometimes no pattern stands out. Then use this method. It works on most problems.

1. **Write the brute force.** Say it aloud. State its complexity.
2. **Find the repeated work.** Ask: "Which computation does the brute force do more than once?"
3. **Choose the tool that removes that repeated work.** Use the table below.

| The brute force repeats this work | Remove it with | Pattern |
|-----------------------------------|----------------|---------|
| Sums the same range again for each query | Prefix sums | [04](./04-prefix-sum/README.md) |
| Rebuilds the window for each start index | Add one element on the right, remove one on the left | [01](./01-sliding-window/README.md) |
| Scans the array to find `target − x` | Hashmap of seen values | [05](./05-hashing-frequency/README.md) |
| Scans left to find the previous greater element | Monotonic stack | [07](./07-monotonic-stack-queue/README.md) |
| Rescans each window to find its max | Monotonic deque | [07](./07-monotonic-stack-queue/README.md) |
| Calls the same recursive function with the same arguments | Memoization / DP table | [15](./15-dynamic-programming/README.md) |
| Scans sorted data from left to right | Binary search | [06](./06-binary-search-on-answer/README.md) |
| Tries every answer value `1, 2, 3, ...` | Binary search on the answer | [06](./06-binary-search-on-answer/README.md) |
| Sorts again after each insert to find the min/max | Heap | [16](./16-heap-priority-queue/README.md) |
| Runs DFS again after each new edge to check connectivity | Union find | [13](./13-union-find/README.md) |
| Compares each query word with every dictionary word | Trie | [17](./17-trie/README.md) |
| Tries all pairs (i, j) of a sorted array | Two pointers | [02](./02-two-pointers/README.md) |

### More ways to find the approach when you are stuck

| Technique | What to do | Example |
|-----------|-----------|---------|
| **Solve small cases** | Solve n = 1, 2, 3 by hand. Look at how the answer for n uses the answer for n − 1. When such a relation exists, it is the DP recurrence. | Climbing stairs: `f(n) = f(n−1) + f(n−2)` |
| **Look at the last step** | Ask: "What is the last decision in an optimal answer?" Each option for the last step becomes one term of the recurrence. | Burst balloons (LC 312): choose the balloon that bursts last |
| **Fix one variable** | Loop over one index. Solve the rest with a known pattern. | 3Sum: fix `i`, run two pointers on the rest. Maximal rectangle (LC 85): fix the bottom row, then solve the histogram (LC 84). |
| **Sort the input** | Sort and look at the data again. Many greedy and two-pointer solutions appear only after sorting. | Frequency of the most frequent element (LC 1838): sort, then sliding window |
| **Transform the input** | Change the values so a known pattern applies. | Equal 0s and 1s (LC 525): replace 0 with −1, then find a subarray with sum 0 |
| **Reverse the direction** | Process from the right, or reverse time. | Remove edges → add them back in reverse (LC 803) |
| **Count the complement** | Count all cases, then subtract the bad cases. "Minimum removals" equals `n −` "maximum kept". | LC 435: removals = n − max non-overlapping intervals |
| **Add state** | When BFS or DP gives a wrong answer, the state is missing information. Add it: `(node, keysCollected)`, `(cell, obstaclesLeft)`, `(day, holding)`. | LC 864, LC 1293, LC 309 |
| **Ask what would make it easy** | "If the array were sorted…", "If there were no negatives…", "If k were 1…". Then find a way to reach that easy case. | Exactly k distinct = atMost(k) − atMost(k−1) (LC 992) |

---

## Step 6: Check the Choice Before You Code

Spend one minute on these checks. A wrong pattern found here costs one minute. A wrong pattern found after coding costs fifteen.

- [ ] The complexity fits the budget from Step 1.
- [ ] You traced the given example through the idea by hand, and the answer is correct.
- [ ] For greedy: you tried to build a counterexample, and it failed.
- [ ] For sliding window: the property is monotonic when the window grows (Q2).
- [ ] For binary search on the answer: `feasible` is monotonic (Q7), and you chose the bounds `lo` and `hi`.
- [ ] For DP: you can say the state in one sentence ("`dp[i]` = the best answer for the first i elements").
- [ ] For graphs: you know whether edges are directed and what they weigh.
- [ ] You listed the edge cases: empty input, one element, all equal values, negative values, overflow.

---

# Part 2: Decision Trees

Use these trees after Steps 1–3. They show the order in which to ask the Step 4 questions.

### Tree A: Array or string, contiguous segment

```
Contiguous subarray / substring?
│
├── Fixed length k ──────────────────────────────► Fixed sliding window
│        └── need max/min of each window ────────► Monotonic deque
│
├── Longest / shortest with a condition
│        ├── condition is monotonic as the window grows
│        │   (all values ≥ 0, counts of chars, "at most k") ► Variable sliding window
│        └── not monotonic (negatives)
│                 ├── exact sum = k ─────────────► Prefix sum + hashmap (first index)
│                 └── sum ≥ k ───────────────────► Prefix sum + monotonic deque (LC 862)
│
├── Count subarrays
│        ├── sum / xor = k ──────────────────────► Prefix sum + hashmap of counts
│        ├── "exactly k distinct / odd numbers" ─► atMost(k) − atMost(k−1)
│        └── sum of min/max over all subarrays ──► Monotonic stack (contribution)
│
└── Max sum / product of any subarray ───────────► Kadane (track min too for product)
```

### Tree B: Array, not contiguous

```
Elements need not be adjacent?
│
├── Find a pair / triple with a target
│        ├── need original indices ──────────────► Hashmap
│        └── values only ────────────────────────► Sort + two pointers
│
├── List ALL subsets / permutations / combos ────► Backtracking
│
├── Count ways or best value over choices
│        ├── greedy choice provable ─────────────► Greedy (sort first if order is free)
│        └── otherwise ──────────────────────────► DP (knapsack, LIS, subsequence DP)
│
├── "Minimize the max" / "maximize the min" ─────► Binary search on answer + greedy check
│
├── k-th / top k ────────────────────────────────► Heap / quickselect / bucket (Q11)
│
└── Values in [1..n], O(1) space ────────────────► Cyclic sort / index marking / XOR
```

### Tree C: Graph or grid

```
Graph or grid?
│
├── Shortest path / fewest steps
│        └── apply Q8 (weights) ─────────────────► BFS / 0-1 BFS / Dijkstra / Bellman-Ford
│                 └── extra limit (keys, k stops, obstacles) ► add it to the state
│
├── Connect all nodes at min total cost ─────────► MST (Kruskal + union find, or Prim)
│
├── Order with dependencies ─────────────────────► Topological sort (cycle = no order)
│
├── Components / islands / regions
│        ├── fixed graph ────────────────────────► DFS / BFS flood fill
│        └── edges added over time ──────────────► Union find
│
├── Nearest source to every cell ────────────────► Multi-source BFS
│
├── Two groups with no edge inside a group ──────► Bipartite check (2-coloring)
│
└── Longest path / count paths
         ├── graph is a DAG (or grid moves only right/down) ► DP over topological order / memo DFS
         └── general graph with cycles ──────────► Backtracking (n small) or bitmask DP
```

### Tree D: Optimization ("find the min / max")

```
Find a minimum or maximum value?
│
├── Need all optimal solutions, not just the value ► Backtracking
│
├── Answer X has a monotonic feasible(X) check ──► Binary search on answer
│
├── Locally best choice is provably safe ────────► Greedy (prove with an exchange argument)
│
├── Choices create overlapping subproblems ──────► DP
│        ├── one sequence ───────────────────────► dp[i]
│        ├── two sequences ──────────────────────► dp[i][j]
│        ├── a range that splits ────────────────► interval DP dp[l][r]
│        ├── items with capacity ────────────────► knapsack dp[i][w]
│        ├── a mode such as "holding / not holding" ► state machine dp[i][state]
│        └── n ≤ 20, a set of used items ────────► bitmask DP dp[mask]
│
└── Minimum steps between states ────────────────► BFS (equal costs) / Dijkstra
```

---

# Part 3: Worked Walkthroughs

In each walkthrough below, the obvious first guess is wrong. Each one shows the question that rejects that first guess. Read the problem, cover the rest, and choose a pattern yourself. Then compare.

---

### Walkthrough 1: Subarray Sum Equals K (LC 560)

**Problem:** Count the contiguous subarrays whose sum equals `k`. `n ≤ 2·10^4`, values in `[−1000, 1000]`.

**First guess:** sliding window, because the problem says "contiguous subarray" and "sum".

**Why the guess fails (Q2):** values can be negative. Take `[1, −1, 1]` with `k = 1`. When the window grows, the sum goes up and down. The window has no rule for when to shrink.

**Reasoning:**
- Q1: contiguous → prefix sum is a candidate.
- The sum of `a[i..j]` equals `prefix[j+1] − prefix[i]`. A subarray ending at `j` has sum `k` when an earlier prefix equals `prefix[j+1] − k`.
- "Count earlier prefixes with value P" → a hashmap from prefix value to count.

**Pattern:** prefix sum + hashmap.

```java
int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    count.put(0, 1);                      // empty prefix
    int sum = 0, result = 0;
    for (int x : nums) {
        sum += x;
        result += count.getOrDefault(sum - k, 0);
        count.merge(sum, 1, Integer::sum);
    }
    return result;
}
```

**Complexity:** O(n) time, O(n) space.

**Lesson:** "contiguous + sum" with negative values points to prefix sum, not sliding window.

---

### Walkthrough 2: Coin Change (LC 322)

**Problem:** Find the minimum number of coins that make `amount`. Each coin can be used any number of times. `amount ≤ 10^4`, up to 12 coin values.

**First guess:** greedy. Always take the largest coin that fits.

**Why the guess fails (Q5):** coins `{1, 3, 4}`, amount `6`. Greedy takes `4, 1, 1` (3 coins). The optimal answer is `3, 3` (2 coins). The choice of 4 had to be undone.

**Reasoning:**
- Q4: one number (the minimum), not all solutions.
- Q6: the brute-force recursion is `minCoins(remaining) = 1 + min over coins c of minCoins(remaining − c)`. The same `remaining` value appears on many branches → overlapping subproblems.
- State: `dp[a]` = the minimum coins to make amount `a`.

**Pattern:** DP (unbounded knapsack).

```
dp[0] = 0, dp[1..amount] = INF
for a in 1..amount:
    for c in coins:
        if c <= a and dp[a - c] != INF:
            dp[a] = min(dp[a], dp[a - c] + 1)
return dp[amount] == INF ? -1 : dp[amount]
```

**Complexity:** O(amount × coins) time, O(amount) space.

**Lesson:** before you commit to greedy, spend two minutes looking for a counterexample with small numbers.

---

### Walkthrough 3: Koko Eating Bananas (LC 875)

**Problem:** There are `n` piles of bananas. Koko eats at speed `s` bananas per hour, from one pile per hour. Find the minimum `s` that finishes all piles in `h` hours. `n ≤ 10^4`, pile sizes up to `10^9`.

**First guess:** a formula, or simulation for each speed.

**Why the guess fails:** no direct formula exists. Simulation for each speed from 1 to 10^9 takes 10^9 × n operations.

**Reasoning (Q7):**
- Rephrase the problem: "Can Koko finish in `h` hours at speed `s`?" The check takes O(n): sum `ceil(pile / s)` over the piles.
- Monotonic: if speed `s` finishes in time, every speed above `s` also finishes. The pattern is `F F F T T T`.
- Find the first `T` in the range `[1, max(pile)]`.

**Pattern:** binary search on the answer.

```java
int minEatingSpeed(int[] piles, int h) {
    int lo = 1, hi = 0;
    for (int p : piles) hi = Math.max(hi, p);
    while (lo < hi) {                      // answer is in [lo, hi]
        int mid = lo + (hi - lo) / 2;
        long hours = 0;
        for (int p : piles) hours += (p + mid - 1) / mid;   // ceil(p / mid)
        if (hours <= h) hi = mid;          // mid works → answer ≤ mid
        else lo = mid + 1;                 // mid too slow → answer > mid
    }
    return lo;
}
```

**Complexity:** O(n log(max pile)) time, O(1) space.

**Lesson:** "find the minimum X such that ..." with a large range for X → ask whether `feasible(X)` is monotonic.

---

### Walkthrough 4: Shortest Subarray with Sum at Least K (LC 862)

**Problem:** Find the length of the shortest non-empty subarray with sum `≥ k`. Values can be negative. `n ≤ 10^5`.

**First guess:** variable sliding window. It solves LC 209, which is the same problem with positive values.

**Why the guess fails (Q2):** with negative values, shrinking the window from the left can increase the sum. The window cannot know when to stop shrinking.

**Reasoning:**
- Use prefix sums. For each end `j`, find the largest `i < j` with `P[j] − P[i] ≥ k`.
- Two observations remove candidates for `i`:
  1. If `P[i1] ≥ P[i2]` and `i1 < i2`, then `i1` is never a better start than `i2`. Keep the prefix values increasing in a deque.
  2. When `P[j] − P[front] ≥ k`, record the length and remove the front. A later `j` would give a longer subarray with that start.

**Pattern:** prefix sum + monotonic deque.

**Complexity:** O(n) time, O(n) space.

**Lesson:** the same question (shortest subarray with sum ≥ k) needs a different pattern when negative values are allowed. Q2 decides.

---

### Walkthrough 5: Cheapest Flights Within K Stops (LC 787)

**Problem:** Find the cheapest price from `src` to `dst` with at most `k` stops. Prices are positive. `n ≤ 100`.

**First guess:** Dijkstra, because the problem has non-negative weights and asks for the cheapest path.

**Why the plain guess fails (Q8):** plain Dijkstra keeps one best cost per node. A path that is more expensive but uses fewer stops can be the only valid path to `dst`. Plain Dijkstra discards it.

**Reasoning:**
- Q8: the limit "at most K edges" → Bellman-Ford with K + 1 rounds.
- Each round relaxes every edge, using a copy of the previous round's distances. With the copy, one round adds at most one edge to each path.

**Pattern:** Bellman-Ford limited to `k + 1` rounds. A Dijkstra variant also works if the state is `(node, stopsUsed)`.

```
dist = [INF] * n; dist[src] = 0
repeat k + 1 times:
    prev = copy of dist
    for (u, v, w) in flights:
        if prev[u] != INF and prev[u] + w < dist[v]:
            dist[v] = prev[u] + w
return dist[dst] == INF ? -1 : dist[dst]
```

**Complexity:** O(k × E) time, O(n) space.

**Lesson:** an extra limit on the path (stops, fuel, keys) means you must add it to the state or use an algorithm that counts edges.

---

### Walkthrough 6: Longest Consecutive Sequence (LC 128)

**Problem:** Find the length of the longest run of consecutive integers in an unsorted array, for example `[100, 4, 200, 1, 3, 2]` → 4. The problem requires O(n) time.

**First guess:** sort and scan.

**Why the guess fails:** sorting is O(n log n). The problem requires O(n).

**Reasoning:**
- Q1: the run does not need to be adjacent in the array → no sliding window.
- The repeated work in "for each x, count x+1, x+2, ..." is that each run is counted once from every member.
- Count a run only from its first member: start counting at `x` only if `x − 1` is not in the set.

**Pattern:** hashset + "start of run" check.

**Complexity:** O(n) time. Each element is visited at most twice.

**Lesson:** when the problem requires O(n) on unsorted data, sorting is too slow. Try a hashmap or hashset first.

---

### Walkthrough 7: Non-overlapping Intervals (LC 435)

**Problem:** Find the minimum number of intervals to remove so that the rest do not overlap. `n ≤ 10^5`.

**First guess:** DP over the intervals, because it is an optimization over choices.

**Why the guess is too slow:** with `n = 10^5`, an O(n²) DP takes 10^10 operations.

**Reasoning:**
- Count the complement: minimum removals = `n −` maximum number of non-overlapping intervals.
- Q5: the interval that ends first is always safe to keep. Exchange argument: take an optimal answer, and replace its first interval with the interval that ends first. No overlap appears, and the count stays the same.
- So sort by **end**, and keep each interval that starts at or after the last kept end.

**Pattern:** greedy (sort by end).

**Complexity:** O(n log n) time.

**Lesson:** "sort by start" merges intervals. "Sort by end" selects the most intervals. See [Pattern 20](./20-intervals/README.md).

---

### Walkthrough 8: Sliding Window Maximum (LC 239)

**Problem:** Return the maximum of every window of size `k`. `n ≤ 10^5`.

**First guess:** a max-heap of the window.

**Why the guess is not the best:** a heap cannot remove an arbitrary element in O(log n). You must store indices and skip stale entries ("lazy deletion"). This gives O(n log n). It works, but the expected answer is O(n).

**Reasoning (Q12):**
- Max of every window → monotonic deque.
- If a newer element is greater than or equal to an older element, the older element can never be a window max again. Remove it from the back.
- The front of the deque is the max. Remove the front when its index leaves the window.

**Pattern:** monotonic deque.

**Complexity:** O(n) time. Each index enters and leaves the deque once.

**Lesson:** a heap answers "max of a changing set". A monotonic deque answers "max of a window that only moves forward" in O(n).

---

### Walkthrough 9: Min Cost to Connect All Points (LC 1584)

**Problem:** Connect all points with edges. An edge costs the Manhattan distance between its two points. Find the minimum total cost. `n ≤ 1000`.

**First guess:** shortest paths (Dijkstra), because it is a graph with costs.

**Why the guess fails (Q8):** shortest paths minimize the cost from one source to each node. This problem minimizes the total cost of an edge set that connects all nodes. That is a minimum spanning tree.

**Reasoning:**
- The graph is complete: about `n² / 2 = 5·10^5` edges.
- Kruskal: sort all edges, then add each edge that joins two different components (union find). O(n² log n).
- Prim with an array instead of a heap: O(n²). This is better for a dense graph.

**Pattern:** minimum spanning tree.

**Lesson:** "cheapest way to connect everything" is an MST. "Cheapest way from A to B" is a shortest path.

---

### Walkthrough 10: Path With Minimum Effort (LC 1631)

**Problem:** In a grid of heights, move from top-left to bottom-right. The effort of a path is the maximum absolute height difference between two neighboring cells on it. Minimize the effort. Grid up to 100 × 100.

**First guess:** BFS on the grid.

**Why the guess fails:** BFS minimizes the number of steps. This problem minimizes the maximum edge weight on the path, which is a different quantity.

**Reasoning:** three correct approaches exist. Each one comes from a different deciding question.
- **Q8 (weights):** use Dijkstra, where the cost of a path is `max(effort so far, edge)` instead of a sum. It works because the cost never decreases along a path.
- **Q7 (monotonic):** "Can I reach the end with effort ≤ X?" is monotonic in X. Binary search X, and run BFS using only edges with weight ≤ X.
- **Q9 (connectivity):** sort the edges by weight. Add them with union find until top-left and bottom-right are connected. The last weight added is the answer.

**Lesson:** some problems have more than one correct pattern. Pick the one you can write correctly in the time you have. Name the others to show the interviewer you see them.

---

### Walkthrough 11: Longest Increasing Subsequence (LC 300)

**Problem:** Find the length of the longest strictly increasing subsequence. `n ≤ 2500`.

**First guess:** sliding window or a monotonic stack, because the problem says "increasing".

**Why the guess fails (Q1):** a subsequence can skip elements. A window or stack keeps only adjacent structure.

**Reasoning:**
- Q6: `dp[i]` = the length of the LIS that ends at index `i`. `dp[i] = 1 + max(dp[j])` over `j < i` with `a[j] < a[i]`. O(n²) = 6.25 · 10^6 operations, which fits the budget.
- Follow-up for `n = 10^5`: keep `tails[len]` = the smallest tail of an increasing subsequence of length `len + 1`. Binary search the position of each element in `tails`. O(n log n).

**Pattern:** DP. Binary search for the follow-up.

**Lesson:** "subsequence" rules out the contiguous patterns immediately.

---

### Walkthrough 12: K Closest Points to Origin (LC 973)

**Problem:** Return the `k` points closest to the origin. `n ≤ 10^4`.

**First guess:** sort all points by distance. This is correct, and it runs in O(n log n).

**Better options (Q11):**
- A max-heap of size `k`: O(n log k). This is the expected answer when data is streamed.
- Quickselect: O(n) on average. This is the expected answer when the interviewer asks for better than O(n log n).

**Pattern:** heap of size k, or quickselect.

**Lesson:** for top-k problems, state all three options and their trade-offs. Then write the one the interviewer prefers.

---

# Part 4: How to Train Recognition

Recognition is a separate skill from implementation. Train it on purpose.

1. **Predict before you solve.** Read a new problem. Within 2 minutes, write down the pattern you predict and the deciding question that chose it. Then solve. Compare your prediction with the real solution.
2. **Keep a misrecognition log.** For each wrong prediction, write one line: problem, your guess, the correct pattern, and the step or question that would have caught it. Read the log every week. Most people find that the same two or three questions cause most of their errors.
3. **Mix the topics.** For the first weeks, practice one pattern at a time. After that, practice with random problems from all patterns. When you know the topic of a problem in advance, you skip the recognition step and do not train it.
4. **Classify without solving.** Take 10 problems. For each one, name the pattern and the deciding question in 2 minutes, without writing code. This trains recognition five times faster than full solves. Use [Part 5](#part-5-recognition-drills).
5. **Solve one problem two ways.** For problems like LC 1631 (walkthrough 10), write two different correct solutions. This shows you where the borders between patterns are.
6. **Review on a schedule.** Solve each problem again after 1, 3, and 7 days. If you cannot name the pattern in 2 minutes, add the problem to the log.

---

# Part 5: Recognition Drills

Each line describes a problem. Name the pattern and the deciding question before you open the answer. The answers are below the table.

| # | Problem description |
|---|---------------------|
| 1 | Longest substring that contains at most 2 distinct characters. |
| 2 | Count subarrays whose sum is divisible by `k`. Values can be negative. |
| 3 | Given a sorted array, find two numbers that sum to a target. Return their 1-based indices. |
| 4 | Given task dependencies, return an order in which to do all tasks, or an empty list if this is impossible. |
| 5 | Ship packages in a fixed order within `D` days. Find the minimum ship capacity. |
| 6 | For each day, find how many days you wait for a warmer temperature. |
| 7 | Return all ways to split a string into palindromic pieces. |
| 8 | Return the number of ways to decode a digit string into letters (`1 → A` ... `26 → Z`). |
| 9 | Find the minimum number of meeting rooms for a list of meetings. |
| 10 | In a grid, each rotten orange infects its neighbors each minute. Find the time until all oranges are rotten. |
| 11 | Edges arrive one at a time in a graph that started as a tree. Return the edge that creates a cycle. |
| 12 | Find the median after each new number in a stream. |
| 13 | In a grid of letters, find every dictionary word that you can trace through neighboring cells. |
| 14 | Every number appears three times except one. Find it in O(1) extra space. |
| 15 | Find the shortest path in a grid when you may remove at most `k` obstacles. |
| 16 | Find the maximum profit from non-overlapping weighted jobs with start time, end time, and profit. `n ≤ 5·10^4`. |
| 17 | Find the minimum number of jumps to reach the last index. `nums[i]` is the maximum jump length from `i`. |
| 18 | Find the k-th smallest number in an `m × n` multiplication table. `m, n ≤ 3·10^4`. |
| 19 | Find the shortest path that visits every node of a graph with `n ≤ 12` nodes. |
| 20 | Find the length of the longest subarray of 0s and 1s with an equal count of each. |

<details>
<summary><b>Answers (open after you try all 20)</b></summary>

| # | Pattern | Deciding question |
|---|---------|-------------------|
| 1 | Variable sliding window + count map (LC 159 / 340) | Contiguous, and "at most k distinct" is monotonic as the window grows |
| 2 | Prefix sum + hashmap of `prefix mod k` (LC 974) | Q2: negatives rule out the window. Use `Math.floorMod` for negative remainders. |
| 3 | Two pointers from both ends (LC 167) | Q3: sorted input. Indices come from the sorted array, so you do not need a hashmap. |
| 4 | Topological sort, Kahn's algorithm (LC 210) | Q10: you need an order, and a cycle means no order exists |
| 5 | Binary search on answer + greedy check (LC 1011) | Q7: if capacity C works, every capacity above C works |
| 6 | Monotonic stack (LC 739) | Q12: next greater element for each index |
| 7 | Backtracking (LC 131) | Q4: return all solutions |
| 8 | DP, `dp[i]` from `dp[i−1]` and `dp[i−2]` (LC 91) | Q4 + Q6: count only, and the subproblems repeat |
| 9 | Sort by start + min-heap of end times, or a sweep line (LC 253) | Count of overlaps at the busiest moment |
| 10 | Multi-source BFS (LC 994) | All rotten oranges start at time 0, and each step costs 1 |
| 11 | Union find (LC 684) | Q9: edges arrive over time. The cycle edge joins two nodes that already share a root. |
| 12 | Two heaps (LC 295) | Stream + median: a max-heap for the lower half, a min-heap for the upper half |
| 13 | Trie + DFS backtracking (LC 212) | Q13: you match many words with shared prefixes. A trie prunes dead paths. |
| 14 | Count each bit position mod 3 (LC 137) | Q14: O(1) space. XOR alone cancels pairs, not triples. |
| 15 | BFS with state `(row, col, removalsLeft)` (LC 1293) | Q8: each step costs 1. The extra limit goes into the state. |
| 16 | Sort by end + DP + binary search for the last compatible job (LC 1235) | Q5: weights break the greedy choice. n = 5·10^4 rules out O(n²). |
| 17 | Greedy BFS-by-ranges (LC 45) | Q5: the farthest reach in the current range is always safe. DP also works in O(n²). |
| 18 | Binary search on the value + count `≤ mid` per row (LC 668) | Q11: the set is implicit and has 9·10^8 values, so a heap is too slow |
| 19 | BFS or DP over `(node, visitedMask)` (LC 847) | Step 1: n ≤ 12 → bitmask state |
| 20 | Replace 0 with −1, then prefix sum + hashmap of first index (LC 525) | "Transform the input" from Step 5 |

</details>

---

## Where to Go Next

- For the details of each pattern, use the [Pattern Index](./README.md#pattern-index).
- For the Java classes and calls that each pattern uses, use the [Java Toolkit](./java-toolkit.md).
- For problems that combine two or three patterns, use [Pattern Combinations](./pattern-combinations.md).
- For short tricks with a clear trigger, use [Tricks at a Glance](./tricks-at-a-glance.md).
