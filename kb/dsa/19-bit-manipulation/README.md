# Pattern 19: Bit Manipulation

## 1. Pattern Name
**Bit Manipulation** (XOR Tricks, Bitmask Enumeration, Bitmask DP)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Bit manipulation is about **treating integers as arrays of 0s and 1s** and using hardware-native logical operations to perform work in O(1) time and O(1) space that would otherwise require loops, hash maps, or entire data structures.

**The Key Insight:**  
Modern CPUs execute bitwise AND, OR, XOR, and shift operations in a single clock cycle. When you recognize a problem can be modeled as bit-level state, you can often eliminate all auxiliary memory and reduce the loop count dramatically.

Think of each integer as a **32-slot checkbox row**. Bit k is the checkbox at position k. AND clears boxes, OR sets boxes, XOR flips boxes, and shifts slide the whole row left or right.

### The Three Tiers of Bit Manipulation:

1. **Bit Tricks (O(1) per operation):** Answering questions about a single number — is it a power of 2? How many set bits? What is its lowest set bit?
2. **XOR Algebra (O(n) scan):** Leveraging the self-canceling property of XOR to find missing or unique values in a collection without extra space.
3. **Bitmask State (O(2^n × n) DP or enumeration):** Encoding a small set of boolean flags into one integer and iterating over all 2^n possible combinations — the go-to approach when n ≤ 20.

### Core Bit Facts — Memorize These:

```
Identity and Annihilator:
  a & 0  = 0        (AND with 0 always clears)
  a & a  = a        (AND with self is identity)
  a | 0  = a        (OR with 0 is identity)
  a ^ 0  = a        (XOR with 0 is identity)
  a ^ a  = 0        (XOR with self always cancels)

Shift Arithmetic:
  a << k = a × 2^k  (left-shift by k multiplies by 2^k)
  a >> k = a ÷ 2^k  (right-shift by k divides by 2^k, positive integers)

Two's Complement:
  -n = ~n + 1       (negate = flip all bits, then add 1)
```

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Without extra space" or "O(1) space" with a counting/finding task
- "Find the single/unique/missing/duplicate number"
- "Power of 2", "divisible by power of 2"
- "Count set bits" / "Hamming weight" / "Hamming distance"
- "Subsets" when n ≤ 20
- "Visited states" / "Assign tasks to workers" when n ≤ 20
- "XOR of all elements in range"
- "Subarray XOR equals k"

### Constraint Hints:
- n ≤ 20 and the problem involves "subsets", "visited", "assigned", "covered" → Bitmask DP
- Only O(1) extra space allowed with array of integers → XOR trick
- Values fit in a 32-bit integer and you are asked about their binary structure → Bit tricks

### Input/Output Hints:
- Input: Array of integers where almost everything appears an even number of times
- Input: Integer to check or transform
- Input: Small n with states that can be on/off
- Output: A count, a boolean, or an optimal value over all subsets

### RED FLAGS — NOT Bit Manipulation:
- n > 20 for bitmask DP (2^20 = ~1 million; 2^30 is a billion — exponential blows up)
- Problem requires exact arithmetic precision (use BigInteger or floating-point instead)
- "Subsequence" with large n — bitmask does not scale
- The "set of booleans" framing but n = 10^5 — that's a graph or DP problem, not a bitmask

---

## 4. Brute Force Thought Process

### Finding the Single Number (Brute Force):
```
Problem: Every number appears twice except one. Find it.

Brute Force Thinking:
- Sort the array, then scan pairs
  OR
- Use a HashMap to count occurrences
  For each number: map[num]++
  For each entry: if count == 1, return that number

Time: O(n log n) or O(n) with hash map
Space: O(1) sort / O(n) hash map
```

### Why It Wastes Space:
The HashMap approach uses O(n) space, but the problem has an elegant O(1) structure hiding in it. Every pair of identical numbers produces the same XOR (a ^ a = 0), and that zero does not contaminate other numbers (a ^ 0 = a). The structure is algebraic, not count-based.

### Power-of-2 Check (Brute Force):
```
Problem: Is n a power of 2?

Brute Force Thinking:
- While n > 1:
    if n % 2 != 0: return false
    n = n / 2
- Return true

Time: O(log n)
Space: O(1)
```

### Counting Set Bits (Brute Force):
```
Problem: How many 1-bits are in n?

Brute Force Thinking:
- count = 0
- While n > 0:
    count += n & 1   // check last bit
    n = n >> 1       // shift right one position
- Return count

Time: O(number of bits) = O(32) = O(1) technically, but still 32 iterations worst case
```

---

## 5. Optimization Insight (The "Aha" Moment)

### XOR Insight — Self-Canceling Pairs:
**"I don't need to count anything. I just need to XOR everything together."**

Every pair cancels to 0, and XOR is commutative and associative, so order doesn't matter:
```
[2, 3, 2, 4, 4]
  → 2 ^ 3 ^ 2 ^ 4 ^ 4
  → (2 ^ 2) ^ (4 ^ 4) ^ 3
  → 0 ^ 0 ^ 3
  → 3
```
One pass, zero extra memory.

### Power-of-2 Insight — The n-1 Bit Flip:
**"A power of 2 has exactly one set bit. Subtracting 1 flips every bit below it AND clears the set bit itself."**

```
n     = 1000  (8 in binary)
n - 1 = 0111  (7 in binary)
n & (n-1) = 0000  → must be a power of 2
```

This collapses 32 iterations into one AND operation.

### Brian Kernighan Insight — Strip Lowest Set Bit:
**"I don't need to inspect every bit position. I can jump directly to set bits."**

`n & (n-1)` removes the lowest set bit. Repeating this until n = 0 takes exactly as many iterations as there are set bits, skipping all the zeros entirely.

### Bitmask DP Insight — Integer as Set:
**"A subset of n items is just an n-bit integer. I can iterate over all 2^n subsets by counting from 0 to 2^n - 1."**

Each integer from 0 to 2^n - 1 uniquely encodes which items are "in" the set. This replaces nested loops and recursive backtracking with a flat iteration over all states.

---

## 6. Generalized Templates

### Template 1: XOR to Find Single Number
```
// Problem: Every number appears twice except one. Find it.
// LC 136. Single Number

function singleNumber(nums):
    result = 0
    for num in nums:
        result = result ^ num
    // All pairs cancel (a ^ a = 0)
    // Survivor remains (a ^ 0 = a)
    return result
```

**Why it works:** XOR is commutative and associative. Every pair of duplicates produces 0. The lone survivor XORs with 0 and remains itself.

**Extension — Two Singles (LC 260):**
```
// Problem: Exactly two numbers appear once; all others appear twice.

function singleNumberIII(nums):
    xorAll = 0
    for num in nums:
        xorAll ^= num
    // xorAll = a ^ b (the two singles XORed together)

    // Find any bit that differs between a and b
    diffBit = xorAll & (-xorAll)   // isolate lowest set bit

    // Partition all numbers by that bit
    groupA = 0
    groupB = 0
    for num in nums:
        if (num & diffBit) != 0:
            groupA ^= num    // a is in this group
        else:
            groupB ^= num    // b is in this group

    return [groupA, groupB]
```

---

### Template 2: Power of 2 Check
```
// LC 231. Power of Two

function isPowerOfTwo(n):
    return n > 0 AND (n & (n - 1)) == 0

// Explanation:
// n > 0: rules out 0, which falsely passes the bit test
// (n & (n-1)) == 0: exactly one set bit means n-1 flips all lower bits,
//                 AND produces zero
```

**WARNING:** Always include `n > 0`. The number 0 satisfies `(n & (n-1)) == 0` because `(0 & -1) == 0`, but 0 is not a power of 2.

---

### Template 3: Count Set Bits — Brian Kernighan's Algorithm
```
// LC 191. Number of 1 Bits (Hamming Weight)

function countSetBits(n):
    count = 0
    while n != 0:
        n = n & (n - 1)   // strips the lowest set bit
        count += 1
    return count

// Each iteration removes exactly one set bit.
// Loop runs exactly popcount(n) times, not 32 times.
```

---

### Template 4: Isolate Lowest Set Bit
```
// Extract the lowest (rightmost) set bit of n.
// Used in Fenwick Tree (Binary Indexed Tree) updates/queries.

lowestSetBit = n & (-n)

// Why: -n = ~n + 1 (two's complement negation)
// ~n flips all bits; adding 1 carries through the trailing zeros
// and stops exactly at the position of the original lowest set bit.
// The AND keeps only that one bit.

// Example:
// n        = ...10110100  (some integer ending in ...0100)
// ~n       = ...01001011
// ~n + 1   = ...01001100
// n & (-n) = ...00000100   ← just the lowest set bit
```

---

### Template 5: Bitmask Subset Enumeration
```
// LC 78. Subsets
// Enumerate all 2^n subsets of an n-item collection.

function subsets(items):
    n = length(items)
    result = []

    for mask = 0 to (1 << n) - 1:
        subset = []
        for i = 0 to n-1:
            if ((mask >> i) & 1) == 1:   // is item i in this subset?
                subset.append(items[i])
        result.append(subset)

    return result

// Bit operations for subset manipulation:
//   Check if item i is in mask:  ((mask >> i) & 1) == 1
//   Add item i to mask:          mask | (1 << i)
//   Remove item i from mask:     mask & ~(1 << i)
//   Toggle item i in mask:       mask ^ (1 << i)
//   Number of items in mask:     popcount(mask)  (or Brian Kernighan loop)
```

**NOTE:** When i can reach 31 or more, use 1L << i in Java/C++ — in a 32-bit signed int, 1 << 31 is negative (Java gives Integer.MIN_VALUE), and 1 << 32 wraps around to 1 in Java.

---

### Template 6: Prefix XOR for Subarray XOR = k
```
// Exact mirror of the prefix-sum + hashmap pattern for subarray sum = k.
// LC 1310. XOR Queries of a Subarray  |  LC 1442.

// Precompute prefix XOR array:
function buildPrefixXOR(arr):
    prefix = array of size n+1, all zeros
    for i = 0 to n-1:
        prefix[i+1] = prefix[i] ^ arr[i]
    return prefix
    // XOR of arr[l..r] = prefix[r+1] ^ prefix[l]

// Count subarrays with XOR equal to k:
function countSubarraysWithXOR(arr, k):
    count = 0
    prefixCount = {0: 1}   // prefix XOR 0 has been seen once (before index 0)
    xorSoFar = 0

    for num in arr:
        xorSoFar ^= num
        // We want: xorSoFar ^ prefix[i-1] = k
        // => prefix[i-1] = xorSoFar ^ k
        target = xorSoFar ^ k
        count += prefixCount.getOrDefault(target, 0)
        prefixCount[xorSoFar] = prefixCount.getOrDefault(xorSoFar, 0) + 1

    return count
```

**Key analogy:** `prefix[j] ^ prefix[i-1] = k` → `prefix[i-1] = prefix[j] ^ k`. This is identical in shape to the prefix-sum pattern; only `+`/`-` are replaced by `^`.

---

### Template 7: XOR Range Formula
```
// XOR of all integers from 1 to n follows a 4-cycle:

function xorUpTo(n):
    switch n % 4:
        case 0: return n
        case 1: return 1
        case 2: return n + 1
        case 3: return 0

// XOR of integers from a to b (inclusive):
function xorRange(a, b):
    if a == 0: return xorUpTo(b)       // xorUpTo(-1) matches no case; XOR with 0 changes nothing
    return xorUpTo(b) ^ xorUpTo(a - 1)

// Why off-by-one matters: it's f(b) ^ f(a-1), NOT f(b) ^ f(a).
// A common mistake is using f(a) and getting wrong answers at the left boundary.
```

---

### Template 8: Bitmask DP
```
// When n ≤ 20 and you need to track a subset of visited/assigned/covered items.
// LC 847. Shortest Path Visiting All Nodes  |  LC 943. Find the Shortest Superstring

// General bitmask DP skeleton:

function bitmaskDP(items):
    n = length(items)
    FULL = (1 << n) - 1    // all items visited
    dp = 2D array [1 << n][n], initialized to INFINITY

    // Base case: start at each item individually
    for i = 0 to n-1:
        dp[1 << i][i] = 0

    // Fill in order of increasing mask (more bits = more items visited)
    for mask = 1 to (1 << n) - 1:
        for last = 0 to n-1:
            if ((mask >> last) & 1) == 0: continue    // last not in mask
            if dp[mask][last] == INFINITY: continue

            // Try adding each unvisited item next
            for next = 0 to n-1:
                if ((mask >> next) & 1) == 1: continue  // already visited
                newMask = mask | (1 << next)
                cost = dp[mask][last] + transition(last, next)
                dp[newMask][next] = min(dp[newMask][next], cost)

    // Answer: best way to cover all items
    answer = INFINITY
    for last = 0 to n-1:
        answer = min(answer, dp[FULL][last])
    return answer

// Time:  O(2^n × n²)
// Space: O(2^n × n)
// Only feasible when n ≤ 20 (2^20 ≈ 10^6 states).
```

---

## 7. Variations of the Pattern

### Variation A: Missing Number (XOR with Index)
**Problem Type:** Given [0, n] with one number missing, find it.  
**Key Insight:** XOR all elements with their indices 0..n. Every present number cancels with its index; only the missing number's index survives.  
**Template:** `result = n; for i, v in enumerate(nums): result ^= i ^ v`  
**LC:** 268

### Variation B: Counting Bits for 0..n (DP + Bit Trick)
**Problem Type:** Return an array where `result[i]` = number of 1-bits in i.  
**Key Insight:** `popcount(i) = popcount(i >> 1) + (i & 1)`. The high bits of i are already computed; just add the last bit.  
**Template:** `dp[i] = dp[i >> 1] + (i & 1)` for i from 1 to n.  
**LC:** 338

### Variation C: Maximum XOR of Two Numbers in Array (Trie + Greedy)
**Problem Type:** Find max XOR of any two elements in the array.  
**Key Insight:** Build a binary trie of all numbers. For each number, greedily try to choose the opposite bit at each level.  
**Bitmask involvement:** Bit extraction `(num >> i) & 1` at each trie level.  
**LC:** 421

### Variation D: Reverse Bits
**Problem Type:** Reverse the 32-bit binary representation of an unsigned integer.  
**Key Insight:** Extract LSB, shift it into the result from the MSB side, repeat 32 times.  
**Template:** `for _ in range(32): result = (result << 1) | (n & 1); n >>= 1`  
**LC:** 190

### Variation E: Bitmask DP for Traveling Salesman / Assignment

More DP forms (digit DP, tree DP, rerooting) are in [Pattern 27](../27-advanced-dp/README.md).
**Problem Type:** Assign n tasks to n workers optimally; find minimum cost Hamiltonian path.  
**Key Insight:** `dp[mask][i]` = minimum cost to complete exactly the tasks in `mask`, ending on task i.  
**Transition:** `dp[mask | (1<<j)][j] = min(..., dp[mask][i] + cost[i][j])` for each unvisited j.  
**LC:** 1986 Minimum Number of Work Sessions to Finish the Tasks, 1879 Minimum XOR Sum of Two Arrays

### Variation F: XOR Swap
**Problem Type:** Swap two variables without a temporary variable (interview trivia).  
**Template:** `a ^= b; b ^= a; a ^= b`  
**Caveat:** Breaks when `a` and `b` are the same memory location (aliasing). Use only with distinct variables.

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Single Number** | XOR all — pairs cancel, survivor remains | 136 |
| **Number of 1 Bits** | Brian Kernighan: n = n & (n-1) each step | 191 |
| **Power of Two** | n > 0 AND (n & (n-1)) == 0 | 231 |
| **Reverse Bits** | Extract LSB 32 times, shift into result | 190 |
| **Missing Number** | XOR indices 0..n with array values | 268 |
| **Counting Bits** | dp[i] = dp[i >> 1] + (i & 1) | 338 |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Single Number II** | Count bits mod 3; bit that appears once survives | 137 |
| **Single Number III** | XOR all, partition by differing bit | 260 |
| **Subsets** | Iterate mask from 0 to 2^n - 1 | 78 |
| **XOR Queries of a Subarray** | Prefix XOR + range formula | 1310 |
| **Count Triplets That Form Two Equal XOR** | Prefix XOR; any split of equal halves works | 1442 |
| **Iterator for Combination** | Bitmask enumeration with carry | 1286 |
| **Minimum Flips to Make OR Equal** | Inspect each bit independently | 1318 |
| **Maximum XOR of Two Numbers in Array** | Greedy bit trie, or bit-by-bit prefix hash set | 421 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Shortest Path Visiting All Nodes** | BFS + bitmask DP; state = (node, visitedMask) | 847 |
| **Minimum Number of Flips to Convert Binary Matrix** | BFS over board states encoded as a bitmask (m, n ≤ 3) | 1284 |
| **Number of Ways to Wear Different Hats** | Bitmask DP; dp[mask] = ways to assign hats | 1434 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Single-Number Tricks and Bit Facts
**Day 1:**
- Solve LC 136 (Single Number) — pure XOR, one line
- Solve LC 191 (Number of 1 Bits) — write both naive and Kernighan versions
- Manually trace through several small examples to internalize `a ^ a = 0`

**Day 2:**
- Solve LC 231 (Power of Two) — practice adding the `n > 0` guard
- Solve LC 268 (Missing Number) — apply XOR with index trick
- Write out the XOR range formula from memory and verify with n = 4..7

**Day 3:**
- Solve LC 190 (Reverse Bits) and LC 338 (Counting Bits)
- For LC 338: code both the naive version and the dp[i >> 1] version
- Compare run times mentally

**Day 4-5:**
- Solve LC 260 (Single Number III) — the two-group partition is the key step
- Solve LC 1310 (XOR Queries) — practice building prefix XOR array
- Review: can you write all 8 templates from memory in 5 minutes?

### Week 2: Bitmask and Advanced
**Day 1-2:**
- Solve LC 78 (Subsets) — code the bitmask enumeration template explicitly
- Solve LC 1442 (Count Triplets) — prefix XOR in a counting context

**Day 3-4:**
- Solve LC 847 (Shortest Path Visiting All Nodes) — the BFS + bitmask state is hard; trace through a 3-node example by hand first
- Solve LC 421 (Maximum XOR) — introduces trie-based greedy bit choice

**Day 5:**
- Attempt LC 1879 or LC 1434 — full bitmask DP with transitions
- If stuck: write the state definition first (`dp[mask] = ...`), then the transition, then the base case

### Practice Methodology:
1. **First 3 minutes:** What type of bit problem is this — trick, XOR algebra, or bitmask state?
2. **Next 5 minutes:** Write the invariant in English (`"all pairs cancel except the survivor"`)
3. **Next 10 minutes:** Write the pseudocode template and trace a 4-element example
4. **Remaining time:** Implement, then check edge cases (n=0, all same, n=1)
5. **After solving:** Try the follow-up — can you generalize (e.g., "each number appears k times except one")?

### Self-Check Questions:
- Can I state why `n & (n-1)` removes the lowest set bit?
- Can I write the XOR range formula and identify the off-by-one error?
- Can I describe what `n & (-n)` computes and why?
- Can I enumerate subsets of 4 items using mask 0..15?
- Do I know the bitmask DP state definition for "minimum cost to visit all nodes"?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Missing the n > 0 Guard in Power-of-2 Check
```
// WRONG: 0 falsely passes the bit test
function isPowerOfTwo(n):
    return (n & (n - 1)) == 0    // 0 & -1 = 0, so 0 passes!

// CORRECT: Explicitly exclude 0 and negative numbers
function isPowerOfTwo(n):
    return n > 0 AND (n & (n - 1)) == 0
```
Zero has no set bits, so `0 & (0-1) = 0 & -1 = 0`. Always guard with `n > 0`.

---

### Mistake 2: Integer Overflow When Shifting with 1 << k
```
// WRONG (Java/C++ with 32-bit int): 1 << 31 overflows signed int
int mask = 1 << 31;       // C++: undefined behavior; Java: negative (Integer.MIN_VALUE)

// CORRECT: Use a long literal
long mask = 1L << 31;     // safe in Java; 64-bit value

// Also applies to bitmask DP when n approaches 30+:
long fullMask = (1L << n) - 1;
```
In languages with fixed-width signed integers, shifting a 1 into the sign bit is undefined or produces a negative number. Use `1L` (long) whenever n > 30 or close to it.

---

### Mistake 3: Off-by-One in the XOR Range Formula
```
// WRONG: Using f(a) instead of f(a-1) for the left boundary
function xorRange(a, b):
    return xorUpTo(b) ^ xorUpTo(a)     // incorrect!

// CORRECT: XOR(a..b) = XOR(1..b) ^ XOR(1..a-1)
function xorRange(a, b):
    if a == 0: return xorUpTo(b)
    return xorUpTo(b) ^ xorUpTo(a - 1)

// Example: XOR(3..5) = 3^4^5 = 2
// WRONG:   xorUpTo(5) ^ xorUpTo(3) = 1 ^ 0 = 1  ← WRONG (drops the 3)
// CORRECT: xorUpTo(5) ^ xorUpTo(2) = 1 ^ 3 = 2  ← CORRECT
// Test edge a=1: XOR(1..5) should be xorUpTo(5)
// WRONG formula: xorUpTo(5) ^ xorUpTo(1) = 1 ^ 1 = 0  ← WRONG
// CORRECT:       xorUpTo(5) ^ xorUpTo(0) = 1 ^ 0 = 1  ← CORRECT
```
The left boundary is *exclusive of a* — we want everything *before* index a, meaning `f(a-1)`.

---

### Mistake 4: Iterating Bitmask DP in the Wrong Order
```
// WRONG: Iterating masks in arbitrary or reverse order
for mask = (1 << n) - 1 downTo 0:
    for last = 0 to n-1:
        // tries to use dp[newMask] which may not be computed yet

// CORRECT: Always iterate masks from 0 upward (fewer bits → more bits)
for mask = 0 to (1 << n) - 1:
    for last = 0 to n-1:
        // dp[mask] is fully populated before dp[mask | (1 << next)] is written
```
Since `newMask = mask | (1 << next)` always has more bits than `mask`, iterating `mask` from 0 upward guarantees all sub-states are computed before they are needed.

---

### Mistake 5: Signed vs. Unsigned Right Shift
```
// In Java: >> is arithmetic (sign-extending), >>> is logical (zero-filling)
int n = -1;
n >> 1  = -1        // arithmetic: preserves sign bit, stays -1
n >>> 1 = 2147483647  // logical: fills with 0, gives MAX_INT

// When using right shift for bit extraction on potentially-negative values:
// WRONG (may loop forever if n < 0 and using >>):
while n != 0:
    bit = n & 1
    n = n >> 1    // n stays negative: sign bit refills

// CORRECT: Use unsigned shift, or work with positive values / unsigned types
n = n >>> 1       // Java unsigned right shift
```
This matters for LC 190 (Reverse Bits) and any bit problem where the input could be negative. Always clarify whether inputs are signed and use the appropriate shift operator.

---

### Mistake 6: Forgetting to Initialize Prefix XOR with 0
```
// WRONG: Missing the initial "empty prefix" entry in the hash map
function countSubarraysWithXOR(arr, k):
    prefixCount = {}     // empty — misses subarrays starting at index 0
    xorSoFar = 0
    count = 0
    for num in arr:
        xorSoFar ^= num
        target = xorSoFar ^ k
        count += prefixCount.getOrDefault(target, 0)
        prefixCount[xorSoFar] += 1

// CORRECT: Seed with {0: 1} to account for a prefix XOR that equals k exactly
function countSubarraysWithXOR(arr, k):
    prefixCount = {0: 1}   // "before the array" has XOR = 0
    xorSoFar = 0
    count = 0
    for num in arr:
        xorSoFar ^= num
        target = xorSoFar ^ k
        count += prefixCount.getOrDefault(target, 0)
        prefixCount[xorSoFar] = prefixCount.getOrDefault(xorSoFar, 0) + 1
    return count
```
This is the exact same mistake as forgetting `{0: 1}` in the prefix-sum-equals-k pattern. The empty prefix must be seeded to catch subarrays that start at index 0.

---

### Mistake 7: Using Bitmask DP When n Is Too Large
```
// If n = 25: 2^25 = 33 million states × 25 = ~800 million operations → TLE
// If n = 30: 2^30 ≈ 1 billion → MLE/TLE

// SIGNAL: Before reaching for bitmask DP, confirm n ≤ 20.
// Typical safe upper bound: n ≤ 20 (2^20 × 20^2 ≈ 400 million — borderline)
//                           n ≤ 15 (2^15 × 15^2 ≈ 7 million — comfortable)

// If n > 20 with "subset/assignment" framing → look for greedy, flow, or
// polynomial DP (knapsack variants), not bitmask DP.
```

---

## Bonus: Interview Script for Bit Manipulation Problems

When you recognize a bit manipulation problem, here is how to communicate:

> "I notice this problem involves finding a unique or missing element with an O(1) space constraint, which suggests an XOR-based approach. Let me think through why: every duplicate pair XORs to zero since a ^ a = 0, and zero XORed with any value returns that value since a ^ 0 = a. So XORing everything together leaves only the survivor.
>
> For the brute force: a hash map approach would work in O(n) time but O(n) space. The XOR trick runs in O(n) time and O(1) space, satisfying the constraint.
>
> Let me trace through a small example before coding: [2, 3, 2] → 2^3^2 = (2^2)^3 = 0^3 = 3. Correct.
>
> Edge case: what if the array has one element? Then we XOR it with 0 (the initial value), and get the element itself. That's correct too."

For bitmask DP problems:

> "Since n ≤ 15, I have at most 2^15 = 32,768 possible subsets. I'll use a bitmask DP where dp[mask] represents [your state definition]. The key transition is: for each unvisited item, extend the current state and update the new mask. I'll iterate masks from 0 upward to ensure all sub-states are populated before I need them."

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| "Find the number that appears once" | HashMap of counts ([Pattern 5](../05-hashing-frequency/README.md)) | Does the problem require O(1) extra space, and does every other value appear an even number of times? Yes → XOR. Other counts (for example, "appears 3 times") → count bits mod 3, or use a HashMap if O(n) space is allowed. |
| "Generate all subsets" with a bitmask loop | Backtracking ([Pattern 8](../08-recursion-backtracking/README.md)) | Do you need to prune branches early (a sum limit, a duplicate skip)? Yes → backtracking. No pruning and n ≤ 20 → the mask loop is simpler. |
| Backtracking over "which items are used" | Bitmask DP | Is n ≤ 20, and do different orders reach the same set of used items? Yes → `dp[mask]` computes each subset once. n > 20 → bitmask DP is too slow. |
| Subarray XOR equals k | Prefix sum + HashMap ([Pattern 4](../04-prefix-sum/README.md)) | Does the problem ask about the XOR of a contiguous range? Yes → prefix XOR + HashMap of seen prefixes, the same steps as prefix sum with `^` in place of `+` and `-`. |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌──────────────────────────────────────────────────────────────────────┐
│                   BIT MANIPULATION CHEAT SHEET                       │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  CORE IDENTITIES:                                                    │
│  a & 0 = 0      a & a = a      a | 0 = a                            │
│  a ^ 0 = a      a ^ a = 0      -n = ~n + 1                          │
│  a << k = a × 2^k              a >> k = a ÷ 2^k (positive a)        │
│                                                                      │
│  SINGLE-NUMBER TRICKS:                                               │
│  XOR all → pairs cancel, lone survivor remains          (LC 136)     │
│  Two singles: XOR all, split by any differing bit       (LC 260)     │
│  Missing number: XOR indices 0..n with array values     (LC 268)     │
│                                                                      │
│  BIT PROPERTY CHECKS:                                                │
│  Is power of 2:  n > 0 AND (n & (n-1)) == 0            (LC 231)     │
│  Count set bits: while n != 0: n = n & (n-1); count++  (LC 191)     │
│  Lowest set bit: n & (-n)   [used in Fenwick Tree]                  │
│  Check bit i:   ((n >> i) & 1) == 1                                 │
│  Set bit i:      n | (1 << i)                                       │
│  Clear bit i:    n & ~(1 << i)                                      │
│  Toggle bit i:   n ^ (1 << i)                                       │
│                                                                      │
│  PREFIX XOR / RANGE XOR:                                             │
│  XOR(a..b) = xorUpTo(b) ^ xorUpTo(a-1)                             │
│  xorUpTo(n): n%4==0→n, n%4==1→1, n%4==2→n+1, n%4==3→0             │
│  Subarray XOR = k:  use prefix XOR + hashmap {0:1}     (LC 1310)    │
│                                                                      │
│  BITMASK ENUMERATION (n ≤ 20):                                      │
│  All subsets: for mask = 0 to (1<<n)-1                  (LC 78)     │
│  Item i in mask: ((mask >> i) & 1) == 1                             │
│  Add item i:  mask | (1 << i)                                       │
│  Remove item i: mask & ~(1 << i)                                    │
│                                                                      │
│  BITMASK DP (n ≤ 20):                                               │
│  State: dp[mask][last] — iterate mask 0 → 2^n-1         (LC 847)    │
│  Transition: dp[mask|(1<<next)][next] from dp[mask][last]           │
│                                                                      │
│  RED FLAGS:                                                          │
│  n > 20 for bitmask DP → exponential, won't scale                   │
│  Missing n > 0 guard in power-of-2 check → false positive on 0      │
│  1 << 31 in signed int → overflow; use 1L << 31                     │
│  XOR range: f(b) ^ f(a-1), NOT f(b) ^ f(a)                         │
│  Prefix XOR map: seed with {0: 1} or miss index-0 subarrays         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 20: Intervals & Sweep Line →](../20-intervals/README.md)*
