# Pattern 15: Dynamic Programming (DP)

## 1. Pattern Name
**Dynamic Programming** - The Ultimate Interview Pattern

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

DP is about **recognizing that you're solving the same subproblems multiple times** and using memory to avoid redundant computation.

**The Key Insight:**  
If your recursive solution recalculates the same thing repeatedly, DP can help. Store results of subproblems and reuse them.

**Mental Model:**  
Think of climbing stairs. To know ways to reach step N, you need ways to reach step N-1 and N-2. Instead of recounting from scratch each time, remember those values.

### Two Approaches:
1. **Top-Down (Memoization):** Start from the answer, recurse down, cache results
2. **Bottom-Up (Tabulation):** Start from base cases, build up to the answer

---

## 3. How to IDENTIFY DP Problems

### The Two Properties:
1. **Overlapping Subproblems:** Same calculations repeated multiple times
2. **Optimal Substructure:** Optimal solution contains optimal solutions to subproblems

### Recognition Signals:
- "Count the number of ways"
- "Find the minimum/maximum"
- "Is it possible to...?"
- "Longest/Shortest sequence"
- Choices at each step
- Problem can be broken into similar smaller problems

### The Litmus Test:
Ask yourself: "Can I express the answer using answers to smaller versions of the same problem?"

If YES → likely DP

### From Recursion to State: The Concrete Test
The litmus question is abstract. This procedure is concrete. Write the brute-force recursion first. If the same arguments repeat, memoize. The arguments are the state.

Worked example: **House Robber (LC 198).** You cannot rob two adjacent houses. Maximize the money.

1. **List the choices at one step.** At house `i`, you rob it or skip it.
2. **Write the brute-force recursion.** Each choice becomes one recursive call.
   ```
   rob(i) = max money from houses 0..i
   rob(i) = max( rob(i-1),               // skip house i
                 rob(i-2) + nums[i] )    // rob house i, so skip house i-1
   rob(i) = 0 when i < 0                 // no houses left
   ```
3. **Expand the first calls and look for repeats.**
   ```
   rob(4) → rob(3), rob(2)
   rob(3) → rob(2), rob(1)      ← rob(2) is computed a second time
   ```
   A repeated call means overlapping subproblems, so DP applies. If no call repeats, plain recursion is enough.
4. **Name the state.** The state is the set of arguments that change between calls. Here it is `i` alone, so the table is one-dimensional: `dp[i]`.
5. **Memoize.** Store `rob(i)` in `memo[i]` the first time. Return the stored value on later calls. Time drops from exponential (about 1.6^n calls) to O(n).
6. **Convert to a table.** Fill `dp` from the smallest argument upward, so every value a cell needs already exists.
   ```
   if n == 1: return nums[0]                  // dp[1] does not exist
   dp[0] = nums[0]
   dp[1] = max(nums[0], nums[1])
   dp[i] = max(dp[i-1], dp[i-2] + nums[i])    for i = 2 to n-1
   answer = dp[n-1]
   ```

These steps produce the five parts of Section 4. The arguments are the state. The recursion is the transition. The stopping condition is the base case. "Smallest argument first" is the order.

---

## 4. The DP Framework (Most Important Section)

### Step 1: Define the State
**What information do I need to describe a subproblem?**

This is the hardest part. The state should:
- Capture enough info to compute the answer
- Be as minimal as possible (fewer dimensions = faster)

Common states:
- `dp[i]` = answer considering first i elements
- `dp[i][j]` = answer for subarray [i, j]
- `dp[i][j]` = answer using first i items with capacity j
- `dp[i][target]` = answer for reaching target using first i elements

### Step 2: Define the Transition
**How does dp[current] relate to dp[previous states]?**

This is the recurrence relation. It describes how to build solutions from smaller subproblems.

### Step 3: Define the Base Case
**What's the answer for the smallest subproblem?**

Usually when:
- Array is empty (dp[0] = ?)
- Target is 0 (dp[0] = ?)
- First element only (dp[1] = ?)

### Step 4: Determine the Order
**In what order should I fill the DP table?**

Fill in order such that when computing dp[current], all dp[previous] values are already computed.

### Step 5: Locate the Answer
**Where in the DP table is my final answer?**

Often dp[n] or dp[n-1] or max/min of entire table.

---

## 5. DP vs Greedy: The Critical Difference

| Aspect | Greedy | Dynamic Programming |
|--------|--------|---------------------|
| Decision | Best local choice | Consider all choices |
| Subproblems | Doesn't revisit | Stores and reuses |
| Guarantee | Works only for certain problems | Works for optimal substructure |
| Complexity | Usually O(n) or O(n log n) | Usually O(n²) or O(n × target) |

### When to Use Which:
- **Greedy:** Local choice doesn't affect future options' optimality
- **DP:** Current choice affects what's optimal later

### Classic Example: Coin Change
- **Greedy:** Always pick largest coin. FAILS for [1, 3, 4] with target 6 (greedy: 4+1+1=3 coins, optimal: 3+3=2 coins)
- **DP:** Consider all possibilities, pick optimal

---

## 6. Classic DP Patterns

### Pattern A: Linear DP (1D)
**State:** dp[i] = answer for first i elements

```
// Climbing Stairs
dp[0] = 1, dp[1] = 1
dp[i] = dp[i-1] + dp[i-2]

// House Robber
dp[i] = max(dp[i-1], dp[i-2] + nums[i])

// Maximum Subarray (Kadane)
dp[i] = max(nums[i], dp[i-1] + nums[i])
```

### Pattern B: Two-String DP
**State:** dp[i][j] = answer for first i chars of s1 and first j chars of s2

```
// Longest Common Subsequence
if s1[i-1] == s2[j-1]:
    dp[i][j] = dp[i-1][j-1] + 1
else:
    dp[i][j] = max(dp[i-1][j], dp[i][j-1])

// Edit Distance
if s1[i-1] == s2[j-1]:
    dp[i][j] = dp[i-1][j-1]
else:
    dp[i][j] = 1 + min(dp[i-1][j-1], dp[i-1][j], dp[i][j-1])
    // replace, delete, insert
```

### Pattern C: 0/1 Knapsack
**State:** dp[i][w] = max value using first i items with capacity w

```
// Include or exclude item i
dp[i][w] = max(
    dp[i-1][w],                        // Don't take item i
    dp[i-1][w-weight[i]] + value[i]    // Take item i
)
```

### Pattern D: Unbounded Knapsack
**State:** Same but can use items multiple times

```
// Can take item i again
dp[i][w] = max(
    dp[i-1][w],                    // Don't take item i
    dp[i][w-weight[i]] + value[i]  // Take item i (note: dp[i], not dp[i-1])
)
```

### Pattern E: Interval DP
**State:** dp[i][j] = answer for subarray/substring from i to j

```
// Process all lengths from 2 to n
for len = 2 to n:
    for i = 0 to n - len:
        j = i + len - 1
        for k = i to j-1:  // Try all split points
            dp[i][j] = best(dp[i][j], combine(dp[i][k], dp[k+1][j]))  // best = min or max
```

### Pattern F: Decision Making DP
**State:** dp[i][decision] = answer at position i with certain decision made

```
// Best Time to Buy and Sell Stock with Cooldown
dp[i][hold] = max(dp[i-1][hold], dp[i-1][cooldown] - price[i])
dp[i][sold] = dp[i-1][hold] + price[i]
dp[i][cooldown] = max(dp[i-1][cooldown], dp[i-1][sold])
```

### Pattern G: Counting DP
**State:** dp[i] = number of ways to reach state i

```
// Coin Change II (count ways)
for each coin:
    for amount = coin to target:
        dp[amount] += dp[amount - coin]
```

---

## 7. Essential DP Templates

### Template 1: 1D DP (Fibonacci Style)
```
function solve(n):
    if n <= 1:
        return baseCase
    
    dp = new array[n + 1]
    dp[0] = base0
    dp[1] = base1
    
    for i = 2 to n:
        dp[i] = transition(dp[i-1], dp[i-2], ...)
    
    return dp[n]
```

### Template 2: 1D DP with Space Optimization
```
function solve(n):
    prev2 = base0
    prev1 = base1
    
    for i = 2 to n:
        curr = transition(prev1, prev2)
        prev2 = prev1
        prev1 = curr
    
    return prev1
```

### Template 3: 2D DP (Two Sequences)
```
function solve(s1, s2):
    m = s1.length
    n = s2.length
    dp = new array[m + 1][n + 1]
    
    // Base cases
    for i = 0 to m:
        dp[i][0] = baseCase
    for j = 0 to n:
        dp[0][j] = baseCase
    
    // Fill table
    for i = 1 to m:
        for j = 1 to n:
            if s1[i-1] == s2[j-1]:
                dp[i][j] = matchTransition(dp[i-1][j-1])
            else:
                dp[i][j] = noMatchTransition(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    
    return dp[m][n]
```

### Template 4: 0/1 Knapsack
```
function knapsack(weights, values, capacity):
    n = weights.length
    dp = new array[n + 1][capacity + 1] filled with 0
    
    for i = 1 to n:
        for w = 0 to capacity:
            // Don't take item i
            dp[i][w] = dp[i-1][w]
            
            // Take item i (if fits)
            if weights[i-1] <= w:
                dp[i][w] = max(dp[i][w], dp[i-1][w - weights[i-1]] + values[i-1])
    
    return dp[n][capacity]
```

### Template 5: Unbounded Knapsack (1D optimized)
```
function unboundedKnapsack(weights, values, capacity):
    n = weights.length
    dp = new array[capacity + 1] filled with 0
    
    for w = 1 to capacity:
        for i = 0 to n - 1:
            if weights[i] <= w:
                dp[w] = max(dp[w], dp[w - weights[i]] + values[i])
    
    return dp[capacity]
```

### Template 6: Coin Change (Minimum Coins)
```
function coinChange(coins, amount):
    dp = new array[amount + 1] filled with INFINITY
    dp[0] = 0
    
    for a = 1 to amount:
        for coin in coins:
            if coin <= a and dp[a - coin] != INFINITY:
                dp[a] = min(dp[a], dp[a - coin] + 1)
    
    return dp[amount] == INFINITY ? -1 : dp[amount]
```

### Template 7: Longest Increasing Subsequence (O(n²))
```
function LIS(nums):
    n = nums.length
    dp = new array[n] filled with 1  // Each element is a subsequence of length 1
    
    for i = 1 to n - 1:
        for j = 0 to i - 1:
            if nums[j] < nums[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    
    return max(dp)
```

### Template 8: LIS (O(n log n) with Binary Search)
```
function LIS_optimized(nums):
    tails = []  // tails[i] = smallest tail of LIS of length i+1
    
    for num in nums:
        pos = binarySearchInsertPosition(tails, num)
        if pos == tails.length:
            tails.add(num)
        else:
            tails[pos] = num
    
    return tails.length
```

### Template 9: Longest Palindromic Subsequence
```
function longestPalinSubseq(s):
    n = s.length
    dp = new array[n][n] filled with 0
    
    // Base: single characters
    for i = 0 to n - 1:
        dp[i][i] = 1
    
    // Fill for lengths 2 to n
    for len = 2 to n:
        for i = 0 to n - len:
            j = i + len - 1
            if s[i] == s[j]:
                dp[i][j] = dp[i+1][j-1] + 2
            else:
                dp[i][j] = max(dp[i+1][j], dp[i][j-1])
    
    return dp[0][n-1]
```

### Template 10: Matrix Chain Multiplication
```
function matrixChainOrder(dimensions):
    n = dimensions.length - 1  // Number of matrices
    dp = new array[n][n] filled with 0
    
    // len is chain length
    for len = 2 to n:
        for i = 0 to n - len:
            j = i + len - 1
            dp[i][j] = INFINITY
            
            for k = i to j - 1:  // Split point
                cost = dp[i][k] + dp[k+1][j] + dimensions[i] * dimensions[k+1] * dimensions[j+1]
                dp[i][j] = min(dp[i][j], cost)
    
    return dp[0][n-1]
```

### Template 11: Partition Equal Subset Sum
```
function canPartition(nums):
    totalSum = sum(nums)
    if totalSum % 2 != 0:
        return false
    
    target = totalSum / 2
    dp = new boolean[target + 1]
    dp[0] = true
    
    for num in nums:
        for t = target down to num:  // Reverse to avoid using same element twice
            dp[t] = dp[t] or dp[t - num]
    
    return dp[target]
```

### Template 12: Word Break
```
function wordBreak(s, wordDict):
    wordSet = new Set(wordDict)
    n = s.length
    dp = new boolean[n + 1]
    dp[0] = true  // Empty string is valid
    
    for i = 1 to n:
        for j = 0 to i - 1:
            if dp[j] and s.substring(j, i) in wordSet:
                dp[i] = true
                break
    
    return dp[n]
```

---

## 8. Space Optimization Techniques

### Technique 1: Rolling Array (2D → 1D)
When dp[i] only depends on dp[i-1]:
```
// Instead of dp[n][capacity]
// Use dp[capacity] and update in reverse

for i = 1 to n:
    for w = capacity down to weights[i-1]:  // REVERSE order!
        dp[w] = max(dp[w], dp[w - weights[i-1]] + values[i-1])
```

### Technique 2: Two Rows Only
When dp[i] depends only on dp[i-1]:
```
prev = new array[...]
curr = new array[...]

for each i:
    // Fill curr using prev
    swap(prev, curr)

return prev[answer_position]
```

### Technique 3: Constant Space
When dp[i] depends only on a few previous values:
```
// Fibonacci
prev2, prev1 = 1, 1
for i = 2 to n:
    curr = prev1 + prev2
    prev2 = prev1
    prev1 = curr
return prev1
```

---

## 9. Classic Interview Problems by Category

### 1D Linear DP
| Problem | State Definition | LC # |
|---------|------------------|------|
| **Climbing Stairs** | dp[i] = ways to reach step i | 70 |
| **House Robber** | dp[i] = max money robbing houses 0 to i | 198 |
| **House Robber II** | Handle circular with two passes | 213 |
| **Maximum Subarray** | dp[i] = max sum ending at i (Kadane) | 53 |
| **Longest Increasing Subsequence** | dp[i] = LIS ending at i | 300 |
| **Word Break** | dp[i] = can form s[0:i] | 139 |
| **Coin Change** | dp[i] = min coins for amount i | 322 |
| **Perfect Squares** | dp[i] = min squares summing to i | 279 |
| **Decode Ways** | dp[i] = ways to decode first i chars | 91 |

### Two Sequence DP
| Problem | State Definition | LC # |
|---------|------------------|------|
| **Longest Common Subsequence** | dp[i][j] = LCS of s1[0:i] and s2[0:j] | 1143 |
| **Edit Distance** | dp[i][j] = min ops to convert s1[0:i] to s2[0:j] | 72 |
| **Interleaving String** | dp[i][j] = can form s3 using s1[0:i] and s2[0:j] | 97 |
| **Distinct Subsequences** | dp[i][j] = ways s1[0:i] contains s2[0:j] | 115 |

### Knapsack Variants
| Problem | Type | LC # |
|---------|------|------|
| **Partition Equal Subset Sum** | 0/1 Knapsack (existence) | 416 |
| **Target Sum** | 0/1 Knapsack (count ways) | 494 |
| **Coin Change II** | Unbounded Knapsack (count ways) | 518 |
| **Ones and Zeroes** | 2D Knapsack | 474 |
| **Profitable Schemes** | Knapsack with extra constraint | 879 |

### Interval DP
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Longest Palindromic Subsequence** | dp[i][j] for range | 516 |
| **Longest Palindromic Substring** | dp[i][j] = isPalindrome | 5 |
| **Burst Balloons** | Last balloon to burst | 312 |
| **Minimum Cost Tree From Leaf Values** | Stack-based or interval DP | 1130 |

### Grid DP
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Unique Paths** | dp[i][j] = paths to reach (i,j) | 62 |
| **Unique Paths II** | Handle obstacles | 63 |
| **Minimum Path Sum** | dp[i][j] = min sum to reach (i,j) | 64 |
| **Triangle** | Bottom-up or top-down | 120 |
| **Maximal Square** | dp[i][j] = largest square ending at (i,j) | 221 |

### State Machine DP
| Problem | States | LC # |
|---------|--------|------|
| **Best Time to Buy/Sell Stock** | One transaction | 121 |
| **Buy/Sell Stock II** | Unlimited transactions | 122 |
| **Buy/Sell Stock III** | At most 2 transactions | 123 |
| **Buy/Sell Stock IV** | At most k transactions | 188 |
| **Buy/Sell Stock with Cooldown** | hold/sold/cooldown states | 309 |
| **Buy/Sell Stock with Transaction Fee** | hold/cash states | 714 |

### HARD DP
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Regular Expression Matching** | Match *, . patterns | 10 |
| **Wildcard Matching** | Match *, ? patterns | 44 |
| **Palindrome Partitioning II** | Min cuts for palindrome partition | 132 |
| **Dungeon Game** | Bottom-right to top-left DP | 174 |
| **Cherry Pickup** | Two paths simultaneously | 741 |
| **Frog Jump** | dp[stone][lastJump] | 403 |
| **Super Egg Drop** | Binary search + DP | 887 |

---

## 10. Common Mistakes in DP

### Mistake 1: Wrong State Definition
```
// WRONG for LIS: dp[i] = length of LIS in first i elements
// Can't determine if we can extend from dp[i-1]

// CORRECT: dp[i] = length of LIS ENDING at index i
// Now we know the last element, can check if extendable
```

### Mistake 2: Wrong Loop Order for Space Optimization
```
// WRONG for 0/1 knapsack with 1D array:
for w = 0 to capacity:  // Forward iteration
    dp[w] = max(dp[w], dp[w - weight[i]] + value[i])
// Uses updated dp values (same item multiple times!)

// CORRECT:
for w = capacity down to weight[i]:  // Reverse iteration
    dp[w] = max(dp[w], dp[w - weight[i]] + value[i])
```

### Mistake 3: Off-by-One in Base Cases
```
// WRONG: dp array too small
dp = new array[n]  // Can't access dp[n]!

// CORRECT:
dp = new array[n + 1]  // dp[0] to dp[n] all valid
```

### Mistake 4: Missing Base Cases
```
// WRONG for word break:
dp[0] = ???  // Forgot to set!

// CORRECT:
dp[0] = true  // Empty string can always be formed
```

### Mistake 5: Wrong Else-Branch in Two-String DP
```
// WRONG for LCS: correct 1-based indexing, but wrong else-branch
// (dp uses 1-based i,j over 0-indexed strings, so s1[i-1] and s2[j-1])
if s1[i-1] == s2[j-1]:
    dp[i][j] = dp[i-1][j-1] + 1
else:
    dp[i][j] = dp[i-1][j-1]  // Bug: ignores the option of skipping one character

// CORRECT: take the best of skipping either character
if s1[i-1] == s2[j-1]:
    dp[i][j] = dp[i-1][j-1] + 1
else:
    dp[i][j] = max(dp[i-1][j], dp[i][j-1])
```

### Mistake 6: Not Handling Impossible Cases
```
// WRONG for coin change:
return dp[amount]  // Might be INFINITY!

// CORRECT:
return dp[amount] == INFINITY ? -1 : dp[amount]
```

---

## 11. Practice Roadmap

### Week 1-2: Foundation
- Climbing Stairs, House Robber
- Maximum Subarray
- Coin Change, Perfect Squares
- Unique Paths, Minimum Path Sum

### Week 3-4: Two Sequence & Knapsack
- LCS, Edit Distance
- Partition Equal Subset Sum
- Target Sum, Coin Change II
- LIS (both O(n²) and O(n log n))

### Week 5-6: Advanced
- Word Break, Word Break II
- Longest Palindromic Subsequence
- Stock problems (all variants)
- Maximal Square

### Week 7-8: Hard Problems
- Burst Balloons
- Regular Expression Matching
- Dungeon Game
- Cherry Pickup

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Optimization with an obvious "best next step" | Greedy ([Pattern 14](../14-greedy/README.md)) | Can a locally best choice ever need to be undone? Test a counterexample: coins {1, 3, 4}, amount 6 → greedy gives 4+1+1, optimal is 3+3. A counterexample exists → DP. None, and you can argue an exchange proof → greedy. |
| "Find combinations that sum to target" | Backtracking ([Pattern 8](../08-recursion-backtracking/README.md)) | Does the output list every solution, or one number (count, min, max, true/false)? List → backtracking (Combination Sum, LC 39). One number → DP (Coin Change II, LC 518). |
| DP on a graph of states | Memoized DFS, BFS, or Dijkstra | Can the state graph contain a cycle? No cycles (a DAG, directed acyclic graph) → memoized DFS is DP. Cycles, such as grid moves in all 4 directions → BFS or Dijkstra ([Pattern 18](../18-shortest-paths/README.md)). |
| DP over subsets of items | Bitmask DP ([Pattern 19](../19-bit-manipulation/README.md)) | Must the state record *which* items are used, and is n ≤ 20? Yes → `dp[mask]`. Only *how many* or *how much* matters → an ordinary index/capacity table. |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                  DYNAMIC PROGRAMMING CHEAT SHEET                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  5-STEP FRAMEWORK:                                               │
│  1. Define STATE - What describes a subproblem?                  │
│  2. Define TRANSITION - How to combine smaller solutions?        │
│  3. Define BASE CASE - Smallest subproblem answer?               │
│  4. Define ORDER - Fill table in what sequence?                  │
│  5. Locate ANSWER - Where in table is final answer?              │
│                                                                  │
│  COMMON PATTERNS:                                                │
│  - 1D Linear: dp[i] = f(dp[i-1], dp[i-2], ...)                   │
│  - Two Strings: dp[i][j] for s1[0:i] and s2[0:j]                 │
│  - 0/1 Knapsack: Take or leave each item                         │
│  - Unbounded: Can take items multiple times                      │
│  - Interval: dp[i][j] for range [i, j]                           │
│                                                                  │
│  SPACE OPTIMIZATION:                                             │
│  - 2D → 1D: When only previous row needed                        │
│  - Reverse loop for 0/1 knapsack in 1D                           │
│  - Two variables for Fibonacci-style                             │
│                                                                  │
│  RED FLAGS (Not DP):                                             │
│  - Need to list ALL solutions, not count/optimize → Backtracking │
│  - All elements independent → Greedy                             │
│  - Graph shortest path → BFS/Dijkstra                            │
│                                                                  │
│  COMPLEXITY:                                                     │
│  - Time: O(states × transition cost)                             │
│  - Space: O(states), often reducible                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Congratulations! You've completed the DSA Interview Mastery Guide.*

*Return to [Main Index →](../README.md)*
