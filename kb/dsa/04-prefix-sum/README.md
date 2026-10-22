# Pattern 04: Prefix Sum & Difference Array

## 1. Pattern Name
**Prefix Sum** (Cumulative Sum) & **Difference Array** (Range Update Optimization)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

These patterns are about **preprocessing data** to answer range queries or perform range updates efficiently.

**Prefix Sum Key Insight:**  
Instead of calculating sum from scratch for every range query, precompute cumulative sums. Any range sum becomes a simple subtraction.

**Difference Array Key Insight:**  
Instead of updating each element in a range, record only where changes START and END. Apply changes in one final pass.

**Mental Model - Prefix Sum:**
Think of it like a running total. If you know the total up to position j and the total up to position i-1, the sum between them is just the difference.

**Mental Model - Difference Array:**
Think of it like temperature adjustments. Instead of changing every hour's temperature, just mark "add 5°" at hour 3 and "subtract 5°" at hour 8. The effect propagates.

---

## 3. Recognition Signals

### Prefix Sum Keywords:
- "Sum of elements between indices i and j"
- "Multiple range sum queries"
- "Subarray sum equals k"
- "Count subarrays with sum..."
- "Cumulative" or "Running total"

### Difference Array Keywords:
- "Add value to all elements in range [i, j]"
- "Multiple range updates"
- "Increment/decrement range"
- "After all operations, return final array"

### Constraint Hints:
- Many queries (Q) on same array → Preprocess!
- Range operations (sum, update) mentioned
- Time limit suggests O(n + Q) instead of O(n * Q)

### RED FLAGS - NOT These Patterns:
- Single range query (just use loop)
- Need to modify and query simultaneously (may need a segment tree or Fenwick tree: see [Pattern 21](../21-segment-tree-and-fenwick-tree/README.md))
- Need max/min of range (different technique)
- Non-contiguous elements

---

## 4. Brute Force Thought Process

### Range Sum Brute Force:
```
Problem: Answer Q queries, each asking for sum of arr[l...r]

Brute Force Thinking:
- For each query (l, r)
  - Sum elements from l to r

Time: O(Q * n) where Q is number of queries
Space: O(1)
```

### Why It Times Out:
If Q = 10^5 and n = 10^5, we do 10^10 operations. Way too slow.

### Range Update Brute Force:
```
Problem: Apply Q updates, each adding val to arr[l...r]

Brute Force Thinking:
- For each update (l, r, val)
  - For each index from l to r
    - arr[i] += val

Time: O(Q * n)
Space: O(1)
```

Same problem: 10^10 operations.

---

## 5. Optimization Insight (The "Aha" Moment)

### Prefix Sum Insight:
**"Precompute once, query in O(1)."**

Build prefix array where:
```
prefix[i] = arr[0] + arr[1] + ... + arr[i-1]
prefix[0] = 0 (sum of nothing)
```

Then: `sum(l, r) = prefix[r+1] - prefix[l]`

**Why this works:**
```
prefix[r+1] = arr[0] + arr[1] + ... + arr[r]
prefix[l]   = arr[0] + arr[1] + ... + arr[l-1]
-------------------------------------------
difference  = arr[l] + arr[l+1] + ... + arr[r]  ← Exactly what we want!
```

### Difference Array Insight:
**"Mark boundaries, propagate at the end."**

Instead of updating every element:
1. Create difference array: `diff[i] = arr[i] - arr[i-1]`
2. For update (l, r, val): `diff[l] += val; diff[r+1] -= val`
3. Reconstruct: `arr[i] = arr[i-1] + diff[i]`

**Why this works:**
- Adding val at diff[l] means "from index l onwards, add val"
- Subtracting val at diff[r+1] means "from index r+1 onwards, cancel that addition"
- Net effect: val is added only to range [l, r]

---

## 6. Generalized Templates

### Template 1: Basic Prefix Sum (1D)
```
function buildPrefixSum(arr):
    n = length(arr)
    prefix = new array[n + 1]
    prefix[0] = 0
    
    for i = 0 to n-1:
        prefix[i + 1] = prefix[i] + arr[i]
    
    return prefix

function rangeSum(prefix, l, r):
    // Returns sum of arr[l...r] (inclusive)
    return prefix[r + 1] - prefix[l]
```

### Template 2: Prefix Sum with HashMap (Subarray Sum = K)
```
function countSubarraysWithSum(arr, k):
    prefixSumCount = new HashMap()
    prefixSumCount[0] = 1  // Empty prefix has sum 0
    
    count = 0
    currentSum = 0
    
    for i = 0 to n-1:
        currentSum += arr[i]
        
        // If (currentSum - k) exists in map, we found subarrays
        if prefixSumCount.contains(currentSum - k):
            count += prefixSumCount[currentSum - k]
        
        // Record current prefix sum
        prefixSumCount[currentSum] = prefixSumCount.getOrDefault(currentSum, 0) + 1
    
    return count
```

### Template 3: 2D Prefix Sum
```
function build2DPrefixSum(matrix):
    rows = matrix.rows
    cols = matrix.cols
    prefix = new array[rows + 1][cols + 1]  // All initialized to 0
    
    for i = 1 to rows:
        for j = 1 to cols:
            prefix[i][j] = matrix[i-1][j-1] 
                         + prefix[i-1][j] 
                         + prefix[i][j-1] 
                         - prefix[i-1][j-1]
    
    return prefix

function rangeSum2D(prefix, r1, c1, r2, c2):
    // Sum of submatrix from (r1,c1) to (r2,c2) inclusive
    return prefix[r2+1][c2+1] 
         - prefix[r1][c2+1] 
         - prefix[r2+1][c1] 
         + prefix[r1][c1]
```

### Template 4: Difference Array (Range Update)
```
function applyRangeUpdates(n, updates):
    // updates is list of (l, r, val)
    diff = new array[n + 1] initialized to 0
    
    for each (l, r, val) in updates:
        diff[l] += val
        diff[r + 1] -= val
    
    // Reconstruct the array
    result = new array[n]
    runningSum = 0
    
    for i = 0 to n-1:
        runningSum += diff[i]
        result[i] = runningSum
    
    return result
```

### Template 5: Difference Array with Initial Values
```
function applyUpdatesToExisting(arr, updates):
    n = length(arr)
    
    // Build difference array from original
    diff = new array[n + 1]
    diff[0] = arr[0]
    for i = 1 to n-1:
        diff[i] = arr[i] - arr[i-1]
    diff[n] = 0
    
    // Apply updates
    for each (l, r, val) in updates:
        diff[l] += val
        diff[r + 1] -= val
    
    // Reconstruct
    result = new array[n]
    result[0] = diff[0]
    for i = 1 to n-1:
        result[i] = result[i-1] + diff[i]
    
    return result
```

---

## 7. Variations of the Pattern

### Variation A: Subarray Sum Equals K
**Problem Type:** Count subarrays with exact sum k
**Key Insight:** If prefix[j] - prefix[i] = k, subarray (i,j] has sum k
**Technique:** HashMap to store prefix sum frequencies

### Variation B: Subarray Sum Divisible by K
**Problem Type:** Count subarrays whose sum is divisible by k
**Key Insight:** If prefix[j] % k == prefix[i] % k, subarray sum divisible by k
**Technique:** HashMap to store remainder frequencies

### Variation C: Maximum Subarray Sum
**Problem Type:** Find maximum sum subarray (LC 53)
**Key Insight:** max(prefix[j] - prefix[i]) where j > i. Track the minimum prefix seen so far: O(n) time, O(1) space.
**Alternative:** Kadane's algorithm. It has the same O(n) time and O(1) space. It is the standard interview answer.

**Kadane's algorithm: extend or restart.** At index i, the best subarray that ends at i has two options:
- Extend: add arr[i] to the best subarray that ends at i-1.
- Restart: begin a new subarray at arr[i].

Pick the larger one. Restart happens when `current` (the best sum ending at i-1) is negative, because a negative prefix only lowers the total.

**Link to DP:** Let dp[i] be the best sum of a subarray that ends at index i. Then `dp[i] = max(arr[i], dp[i-1] + arr[i])`, and the answer is the maximum dp[i]. Kadane keeps only dp[i-1], so space drops from O(n) to O(1).
```
function maxSubArray(arr):
    current = arr[0]          // Best sum of a subarray ending at index i
    best = arr[0]             // Best sum seen so far
    
    for i = 1 to n-1:
        current = max(arr[i], current + arr[i])   // Restart or extend
        best = max(best, current)
    
    return best
```
Initialize with arr[0], not 0. With 0, an all-negative array returns 0 instead of its largest element.

**Maximum Product Subarray (LC 152):** Track both the max and the min product that ends at i. A negative arr[i] turns the smallest product into the largest. A zero resets both through the candidate arr[i].
```
function maxProduct(arr):
    curMax = arr[0]
    curMin = arr[0]
    best = arr[0]
    
    for i = 1 to n-1:
        // Build candidates from the OLD curMax and curMin before overwriting them
        candidates = (arr[i], curMax * arr[i], curMin * arr[i])
        curMax = max(candidates)
        curMin = min(candidates)
        best = max(best, curMax)
    
    return best
```

### Variation D: Range XOR Queries
**Problem Type:** XOR of elements in range
**Key Insight:** XOR has same property: xor[l,r] = prefixXor[r+1] ^ prefixXor[l]

### Variation E: Prefix Sum with Binary Search
**Problem Type:** Find shortest subarray with sum ≥ target (positive numbers only)
**Key Insight:** Prefix array is sorted (if all positive) → Binary search for target

### Variation F: 2D Range Sum with Updates
**Problem Type:** 2D range queries with point updates
**Note:** Prefix sum needs rebuild; use a 2D Fenwick tree instead ([Pattern 21](../21-segment-tree-and-fenwick-tree/README.md))

### Variation G: Range Increment + Point Query
**Problem Type:** After all range updates, query individual positions
**Solution:** Difference array + single reconstruction

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight |
|---------|-------------|
| Running Sum of 1D Array | Basic prefix sum |
| Range Sum Query - Immutable | Precompute prefix, O(1) query |
| Find Pivot Index | prefixLeft == prefixRight |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Subarray Sum Equals K** | Prefix sum + HashMap | 560 |
| **Contiguous Array** | Replace 0 with -1, find sum=0 subarray | 525 |
| **Range Sum Query 2D - Immutable** | 2D prefix sum | 304 |
| **Product of Array Except Self** | Prefix & suffix products | 238 |
| **Subarray Sums Divisible by K** | Prefix sum mod + HashMap | 974 |
| **Continuous Subarray Sum** | Sum divisible by k (length ≥ 2) | 523 |
| **Range Addition** | Difference array | 370 |
| **Corporate Flight Bookings** | Difference array | 1109 |
| **Car Pooling** | Difference array (timeline) | 1094 |
| **Maximum Sum of Two Non-Overlapping Subarrays** | Prefix sum + tracking | 1031 |
| **Maximum Subarray** | Kadane: extend or restart | 53 |
| **Maximum Product Subarray** | Kadane variant: track max and min | 152 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Max Sum of Rectangle No Larger Than K** | 2D prefix + TreeSet | 363 |
| **Count of Range Sum** | Merge sort with counting | 327 |
| **Range Module** | Interval management | 715 |
| **Number of Submatrices That Sum to Target** | 2D to 1D reduction + prefix | 1074 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Prefix Sum Fundamentals
**Day 1:**
- Implement basic prefix sum from scratch
- Solve "Range Sum Query - Immutable"

**Day 2:**
- Solve "Subarray Sum Equals K"
- Understand the prefix[j] - prefix[i] = k insight
- Master the HashMap technique

**Day 3:**
- Solve "Contiguous Array"
- Key transformation: replace 0 with -1

**Day 4:**
- Solve "Subarray Sums Divisible by K"
- Handle negative remainders correctly!

**Day 5:**
- Solve "Product of Array Except Self"
- Understand prefix AND suffix idea

### Week 2: Advanced Applications
**Day 1:**
- Implement 2D prefix sum from scratch
- Solve "Range Sum Query 2D - Immutable"

**Day 2:**
- Implement difference array
- Solve "Range Addition" and "Corporate Flight Bookings"

**Day 3:**
- Solve "Car Pooling"
- Model problem as timeline with difference array

**Day 4:**
- Solve "Number of Submatrices That Sum to Target"
- Reduce 2D to 1D using row prefix sums

**Day 5:**
- Review: What determines prefix sum vs difference array?
- Practice identifying which to use

### Self-Check Questions:
- Can I build prefix sum without off-by-one errors?
- Do I understand the HashMap technique for "sum = k" problems?
- Can I extend to 2D correctly?
- Do I know when difference array is better than prefix sum?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Off-by-One in Prefix Array Size
```
// WRONG: Prefix array same size as original
prefix = new array[n]

// CORRECT: Prefix array needs n+1 elements
prefix = new array[n + 1]
// prefix[0] = 0 (sum of empty prefix)
// prefix[n] = sum of entire array
```

### Mistake 2: Wrong Formula for Range Sum
```
// WRONG: Inclusive bounds confusion
sum(l, r) = prefix[r] - prefix[l]  // Excludes arr[l]!

// CORRECT: 
sum(l, r) = prefix[r + 1] - prefix[l]  // Includes arr[l] through arr[r]
```

### Mistake 3: Not Initializing HashMap with 0
```
// WRONG for "Subarray Sum = K": 
map = empty HashMap

// This misses subarrays starting from index 0!
// If prefix[j] = k, we need prefix[0] = 0 in map

// CORRECT:
map = {0: 1}  // One way to have sum 0 (empty prefix)
```

### Mistake 4: Negative Remainder Handling
```
// WRONG: Negative mod in some languages
remainder = prefixSum % k  // Could be negative!

// CORRECT: Normalize to positive
remainder = ((prefixSum % k) + k) % k
```

### Mistake 5: Difference Array Boundary
```
// WRONG: diff[r+1] out of bounds when r = n-1
diff[r + 1] -= val  // ArrayIndexOutOfBounds!

// CORRECT: Make diff array size n+1
diff = new array[n + 1]
diff[r + 1] -= val  // Now safe even when r = n-1
```

### Mistake 6: 2D Prefix Sum Inclusion-Exclusion
```
// WRONG: Sign errors in 2D
prefix[i][j] = matrix[i-1][j-1] + prefix[i-1][j] + prefix[i][j-1] + prefix[i-1][j-1]
                                                                    // Should be MINUS!

// CORRECT:
prefix[i][j] = matrix[i-1][j-1] 
             + prefix[i-1][j] 
             + prefix[i][j-1] 
             - prefix[i-1][j-1]  // Subtract because it's counted twice
```

### Mistake 7: Updating After Query in Same Loop
```
// WRONG for "Subarray Sum = K": Update before check
for i = 0 to n-1:
    currentSum += arr[i]
    map[currentSum]++              // Update first
    count += map[currentSum - k]   // Count includes current prefix

// CORRECT: Check first, then update
for i = 0 to n-1:
    currentSum += arr[i]
    count += map.getOrDefault(currentSum - k, 0)  // Check
    map[currentSum]++                             // Then update
```

---

## Bonus: Prefix Sum vs Difference Array Decision Guide

| Scenario | Use This |
|----------|----------|
| Multiple range SUM queries | Prefix Sum |
| Multiple range UPDATE operations | Difference Array |
| Find subarray with specific sum | Prefix Sum + HashMap |
| Timeline problems (when do events overlap?) | Difference Array |
| 2D range sum queries | 2D Prefix Sum |
| "Add X to range [l,r]" multiple times | Difference Array |

### Transformation Relationship:
```
Difference Array of Prefix Sum = Original Array
Prefix Sum of Difference Array = Original Array

They are INVERSES of each other!

Original:   [1, 3, 5, 2, 4]
Prefix:     [0, 1, 4, 9, 11, 15]
Difference: [1, 2, 2, -3, 2]  (diff[i] = arr[i] - arr[i-1])
```

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Prefix sum + hashmap for "shortest/longest subarray with sum ≥ k" | Sliding window (Pattern 01) | Are all values non-negative? If yes → sliding window, O(1) space (LC 209); if negatives are allowed → prefix sum + hashmap (LC 560) or prefix sum + monotonic deque (LC 862) |
| Prefix sum for "range sum queries" | Fenwick tree (binary indexed tree: point update and prefix sum in O(log n); [Pattern 21](../21-segment-tree-and-fenwick-tree/README.md)) | Does the array change between queries? If yes, a prefix array needs an O(n) rebuild per update → Fenwick tree (LC 307) |
| Prefix sum for "add val to every element in [l, r]" | Difference array | Are the operations range updates, with reads only after all updates? If yes → difference array (LC 1109) |
| Prefix sum for "range min/max" | Monotonic deque (Pattern 07) | Is the operation min or max? Subtraction cannot undo a min or max → deque for fixed windows (LC 239) |
| Prefix sum + hashmap for "maximum subarray sum" | Kadane's algorithm | Does the problem give a target sum k? If yes → prefix sum + hashmap (LC 560); if it asks for the largest sum → Kadane (LC 53) |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│              PREFIX SUM & DIFFERENCE ARRAY CHEAT SHEET           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  PREFIX SUM:                                                     │
│  - prefix[i+1] = prefix[i] + arr[i]                              │
│  - prefix[0] = 0                                                 │
│  - sum(l,r) = prefix[r+1] - prefix[l]                            │
│  - Build: O(n), Query: O(1)                                      │
│                                                                  │
│  SUBARRAY SUM = K:                                               │
│  - Map stores {prefixSum: count}                                 │
│  - Initialize map with {0: 1}                                    │
│  - Check map[currentSum - k] before updating                     │
│                                                                  │
│  DIFFERENCE ARRAY:                                               │
│  - For range [l,r] add val:                                      │
│    diff[l] += val, diff[r+1] -= val                              │
│  - Reconstruct with running sum                                  │
│  - Update: O(1), Final build: O(n)                               │
│                                                                  │
│  2D PREFIX SUM:                                                  │
│  - Build: prefix[i][j] = mat + up + left - diagonal              │
│  - Query: bottom_right - top - left + top_left                   │
│                                                                  │
│  KADANE (Max Subarray):                                          │
│  - cur = max(arr[i], cur + arr[i]); best = max(best, cur)        │
│  - Init with arr[0]; for products track both max and min         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 05: Hashing & Frequency Maps →](../05-hashing-frequency/README.md)*
