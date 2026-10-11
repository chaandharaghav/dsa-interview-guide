# Pattern 06: Binary Search (Classic & On Answer)

## 1. Pattern Name
**Binary Search** in two forms:
- **Classic:** find a position in a sorted (or rotated sorted) array.
- **On the Answer** (also called Binary Search on Result Space): search the range of possible answers with a monotonic `feasible(X)` check.

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

This is one of the most **powerful and underused** patterns. Instead of searching for an element in an array, we **search for the optimal value in a range of possible answers**.

**The Key Insight:**  
If you can phrase the problem as "find the minimum/maximum X such that some condition holds", AND the condition is **monotonic** (once true, stays true, or once false, stays false), you can binary search!

**Mental Model:**  
Imagine a line of possible answers from min to max. Somewhere on this line is a "boundary" where answers go from invalid to valid (or vice versa). Binary search finds this boundary in O(log n) instead of O(n).

**The Magic Question:**  
"Can I achieve this answer?"

If you can answer this question quickly (O(n) or O(n log n)), and the answer changes from NO to YES (or YES to NO) at some point, binary search works!

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Minimum of maximum" or "Maximum of minimum"
- "At least" or "At most"
- "Minimize the maximum"
- "Maximize the minimum"
- "Is it possible to achieve X?"
- "Allocate", "Divide", "Split"
- "Within K operations/moves"
- "Kth smallest/largest" (sometimes)

### Constraint Hints:
- Large search space (answer can be up to 10^9)
- Small time limit despite large values
- Verification function is straightforward

### The Monotonicity Test:
Let `feasible(X)` mean "answer X satisfies the constraint". The templates in section 6 call this function `canAchieve`.
The direction of the test depends on whether you minimize or maximize.

- **Minimization** ("find the smallest X that works"): check that `feasible(X)` ⇒ `feasible(X+1)`.
  Over increasing X, the results look like `F F F T T T`. The answer is the **first T**.
  Example: if capacity 10 ships everything in D days, capacity 11 also does.
- **Maximization** ("find the largest X that works"): check that `feasible(X)` ⇒ `feasible(X−1)`.
  Over increasing X, the results look like `T T T F F F`. The answer is the **last T**.
  Example: if minimum gap 5 between placed balls is possible, minimum gap 4 is also possible.

If neither implication holds, the results can switch between T and F more than once, and binary search on the answer does not apply.

### Signals for Classic Binary Search (Part A):
- The input array is sorted, or sorted and then rotated.
- The question asks for an index: "first position", "last position", "insert position", "find the minimum", "find a peak".
- The problem requires O(log n) time.
- A rule at `mid` tells you which half cannot contain the answer, even if the array is not sorted (LC 162).

---

## 4. Brute Force Thought Process

### Example: Split Array Largest Sum
```
Problem: Split array into m subarrays to minimize the maximum subarray sum

Brute Force Thinking:
- Try all possible ways to place m-1 dividers
- For each configuration, calculate max subarray sum
- Return minimum of all configurations

Time: O(C(n-1, m-1) * n) = Exponential
Space: O(m)
```

### Why It's Wrong:
The number of ways to split grows exponentially. For n=1000, m=10, there are billions of combinations.

---

## 5. Optimization Insight (The "Aha" Moment)

### The Reframe:
Instead of asking "Where do I place dividers?", ask:
**"If the maximum allowed subarray sum is X, can I split into at most m parts?"**

This is the key mental transformation!

### The Verification:
```
function canSplit(arr, maxSum, m):
    groups = 1
    currentSum = 0
    
    for each num in arr:
        if currentSum + num > maxSum:
            groups += 1
            currentSum = num
        else:
            currentSum += num
    
    return groups <= m
```

This runs in O(n)!

### The Monotonicity:
- If maxSum = 10 allows splitting into ≤ m parts
- Then maxSum = 11 ALSO allows splitting into ≤ m parts (more room!)
- This is monotonic: once possible, stays possible for larger values

### The Binary Search:
- Lower bound: max(arr) (can't have sum less than largest element)
- Upper bound: sum(arr) (worst case: one group)
- Binary search for minimum valid maxSum

---

## 6. Generalized Templates

This section has two parts.
**Part A** covers classic binary search, where you search for a position in an array.
**Part B** covers binary search on the answer, where you search for a value in a range of possible answers.
Both parts use the same idea: find the boundary in a sequence that looks like `F F F T T T`.

---

### Part A: Classic Binary Search on a Sorted Array

#### Template 0a: Lower Bound and Upper Bound (half-open `[lo, hi)`)
- **Lower bound** of `target`: the first index `i` with `a[i] >= target`. It returns `n` if no such index exists.
- **Upper bound** of `target`: the first index `i` with `a[i] > target`. It returns `n` if no such index exists.

```
function lowerBound(a, target):
    lo = 0
    hi = n                       // unchecked indices are [lo, hi); the answer can equal hi, because n means "not found"
    // INVARIANT: every index < lo has a[i] <  target
    //            every index >= hi has a[i] >= target
    while lo < hi:               // the range [lo, hi) still has unchecked indices
        mid = lo + (hi - lo) / 2 // rounds down, so lo <= mid < hi: a[mid] is a valid index
        if a[mid] < target:
            lo = mid + 1         // mid and everything left of it are < target
        else:
            hi = mid             // mid is >= target; it may be the answer, so keep it
    return lo                    // lo == hi: the first index with a[i] >= target

function upperBound(a, target):
    same loop, but the test is:  if a[mid] <= target: lo = mid + 1  else: hi = mid
```

Lower bound is Template 3 (Find First True) from Part B with the predicate `a[i] >= target` over indices `[0, n]`.
Each loop step removes at least one index, so the loop runs O(log n) times.

#### Template 0b: First and Last Occurrence (LC 34), Search Insert Position (LC 35)
```
first = lowerBound(a, target)
if first == n OR a[first] != target:  return [-1, -1]
last  = upperBound(a, target) - 1
return [first, last]

// LC 35 Search Insert Position: return lowerBound(a, target).
// It is the index of target if present, and the index where target would be inserted if absent.
```
Time: O(log n). A linear scan from `first` to find `last` costs O(n) when every element equals `target`.

#### Template 0c: Search in Rotated Sorted Array (LC 33)
A **rotated sorted array** is a sorted array that was cut at one index and the two pieces were swapped, for example `[4,5,6,7,0,1,2]`.
LC 33 has distinct values.

**Which half is sorted:** split at `mid`. At least one of `[lo..mid]` and `[mid..hi]` contains no rotation point, so it is sorted.
If `a[lo] <= a[mid]`, the left half is sorted. Otherwise the right half is sorted.
In the sorted half, a range check tells you whether `target` lies inside it. If it does, keep that half. If it does not, keep the other half.

```
function searchRotated(a, target):
    lo = 0, hi = n - 1                     // closed [lo, hi]: we read a[lo] and a[hi]
    while lo <= hi:
        mid = lo + (hi - lo) / 2
        if a[mid] == target: return mid
        if a[lo] <= a[mid]:                // left half [lo..mid] is sorted
            if a[lo] <= target AND target < a[mid]: hi = mid - 1
            else:                                   lo = mid + 1
        else:                              // right half [mid..hi] is sorted
            if a[mid] < target AND target <= a[hi]: lo = mid + 1
            else:                                   hi = mid - 1
    return -1
```
Time: O(log n). With duplicates (LC 81), `a[lo] == a[mid] == a[hi]` hides the sorted half, and the worst case becomes O(n).

#### Template 0d: Find Minimum in Rotated Sorted Array (LC 153)
Compare `a[mid]` with `a[hi]`, not with `a[lo]`.
- If `a[mid] > a[hi]`, the rotation point is to the right of `mid`. The minimum is in `[mid+1, hi]`.
- Otherwise, `[mid..hi]` is sorted. The minimum is `a[mid]` or something to its left: `[lo, mid]`.

```
function findMin(a):
    lo = 0, hi = n - 1                     // hi must be a valid index: we read a[hi]
    while lo < hi:
        mid = lo + (hi - lo) / 2           // mid < hi, so mid and hi are different indices
        if a[mid] > a[hi]: lo = mid + 1
        else:              hi = mid
    return a[lo]
```
Time: O(log n). The array is not sorted, but the predicate "`a[i] <= a[n-1]`" is `F F F T T T` over indices, and the minimum is the first T.

#### Template 0e: Find Peak Element (LC 162) — binary search without sorted input
A **peak** is an element strictly greater than its neighbors. LC 162 treats `a[-1]` and `a[n]` as negative infinity and guarantees `a[i] != a[i+1]`.

The array is not sorted. A local property still tells you which half to keep:
- If `a[mid] < a[mid+1]`, the values rise to the right of `mid`. Walk right from `mid+1`: either the values keep rising until `a[n-1]`, which is then a peak, or they drop after some index `j`, and `a[j]` is a peak. So a peak exists in `[mid+1, hi]`.
- Otherwise `a[mid] > a[mid+1]`. By the same argument walking left, a peak exists in `[lo, mid]`.

```
function findPeak(a):
    lo = 0, hi = n - 1                     // hi must be a valid index: we read a[mid+1]
    while lo < hi:
        mid = lo + (hi - lo) / 2           // mid < hi, so mid+1 <= hi is valid
        if a[mid] < a[mid+1]: lo = mid + 1
        else:                 hi = mid
    return lo
```
Time: O(log n). Binary search needs a rule that discards half the range and keeps at least one answer. Sorted order is one such rule, but not the only one.

#### Template 0f: Search a 2D Matrix
**LC 74:** each row is sorted, and each row's first value is greater than the previous row's last value.
Read the matrix row by row and you get one sorted array of length `m * n`. Binary search over the index `i` in `[0, m*n)` and read the value at `matrix[i / n][i % n]`, where `n` is the number of columns.
Time: O(log(m·n)).

**LC 240:** each row is sorted and each column is sorted, but rows do not continue each other. This is **not** binary search.
Start at the top-right cell. If the value is greater than `target`, move left (the whole column below is also greater). If it is smaller, move down (the whole row to the left is also smaller).
Each step removes one row or one column, so the time is O(m + n).

#### Median of Two Sorted Arrays (LC 4, Hard)
Binary search on the **partition index** in the shorter array. A partition splits both arrays so that the left parts together hold half the elements.
The partition is correct when `maxLeftA <= minRightB` and `maxLeftB <= minRightA`. If `maxLeftA > minRightB`, move the partition in A to the left; otherwise move it to the right.
Time: O(log(min(m, n))).

#### How to decide lo, hi, the loop condition, and the update
1. **Write the predicate.** State a yes/no question about index (or value) `i` whose answers look like `F F F T T T`. You want the first T. For "last T" in `T T T F F F`, find the first F and subtract 1.
2. **Choose `lo` and `hi` so the answer is always inside `[lo, hi]`.** If "no T exists" is a possible result, include one extra position for it: for an array, `hi = n`. If the loop body reads `a[hi]` or `a[mid+1]` (LC 153, LC 162), set `hi = n - 1` so every index you read is valid.
3. **Loop `while lo < hi`.** The loop stops when one candidate remains, and that candidate is the answer. In this form, no `result` variable is needed.
4. **Compute `mid = lo + (hi - lo) / 2`.** It rounds down, so `lo <= mid < hi`. It never equals `hi`, so `a[mid]` and `a[mid+1]` are valid when `hi <= n - 1`.
5. **Update with `hi = mid` when the predicate is T, and `lo = mid + 1` when it is F.** A T at `mid` may be the first T, so you keep `mid`. An F at `mid` cannot be the answer, so you drop it. Both updates shrink the range by at least one, so the loop always ends.
6. **The closed-range form `while lo <= hi` with `hi = mid - 1` and `lo = mid + 1` is also correct in two cases.** (a) You return from inside the loop on an exact match (LC 33, LC 704). (b) You record `result = mid` before you shrink the range. Template 1 in Part B is the closed-range form of first-T, and Template 2 is the closed-range form of last-T. In the closed form, the range `[lo, hi]` becomes empty when `lo > hi`.

#### Java: Lower Bound
```java
// First index i with a[i] >= target, or a.length if none.
static int lowerBound(int[] a, int target) {
    int lo = 0, hi = a.length;           // search range [lo, hi)
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;    // lo <= mid < hi
        if (a[mid] < target) {
            lo = mid + 1;                // a[0..mid] are all < target
        } else {
            hi = mid;                    // a[mid] >= target: mid may be the answer
        }
    }
    return lo;
}
```

---

### Part B: Binary Search on the Answer

Template 1 finds the first T (minimization, `F F F T T T`). Template 2 finds the last T (maximization, `T T T F F F`).

### Template 1: Minimize Maximum (Most Common)
```
function minimizeMaximum(arr, ...params):
    lo = minimumPossibleAnswer()
    hi = maximumPossibleAnswer()
    result = hi
    
    while lo <= hi:
        mid = lo + (hi - lo) / 2
        
        if canAchieve(mid):
            result = mid       // mid is achievable, try smaller
            hi = mid - 1
        else:
            lo = mid + 1       // mid not achievable, need larger
    
    return result
```

### Template 2: Maximize Minimum
```
function maximizeMinimum(arr, ...params):
    lo = minimumPossibleAnswer()
    hi = maximumPossibleAnswer()
    result = lo
    
    while lo <= hi:
        mid = lo + (hi - lo) / 2
        
        if canAchieve(mid):
            result = mid       // mid is achievable, try larger
            lo = mid + 1
        else:
            hi = mid - 1       // mid not achievable, need smaller
    
    return result
```

### Template 3: Find First True (Generic)
```
function firstTrue(lo, hi, condition):
    while lo < hi:
        mid = lo + (hi - lo) / 2
        
        if condition(mid):
            hi = mid           // mid might be answer, search left
        else:
            lo = mid + 1       // mid not answer, search right
    
    return lo                  // First position where condition is true
```

### Template 4: Find Last True
```
function lastTrue(lo, hi, condition):
    while lo < hi:
        mid = lo + (hi - lo + 1) / 2  // Round up to avoid infinite loop
        
        if condition(mid):
            lo = mid           // mid might be answer, search right
        else:
            hi = mid - 1       // mid not answer, search left
    
    return lo                  // Last position where condition is true
```

### Template 5: Floating Point Binary Search
```
function floatBinarySearch(lo, hi, epsilon):
    // For problems requiring decimal precision
    for iteration = 0 to 100:  // Or while (hi - lo > epsilon)
        mid = (lo + hi) / 2
        
        if condition(mid):
            hi = mid
        else:
            lo = mid
    
    return lo
```

---

## 7. Variations of the Pattern

### Variation A: Koko Eating Bananas
**Problem:** Find minimum eating speed to finish all bananas in H hours
**Insight:** If speed K works, speed K+1 also works (faster eating)
**Verification:** Sum of ceil(pile/K) for all piles ≤ H

### Variation B: Capacity to Ship Packages
**Problem:** Find minimum ship capacity to ship all packages in D days
**Insight:** Same as "Split Array Largest Sum" - minimize max daily load
**Verification:** Greedy packing, count days needed

### Variation C: Aggressive Cows / Magnetic Force
**Problem:** Maximize minimum distance between placed elements
**Insight:** If distance D is achievable, D-1 is also achievable (easier)
**Verification:** Greedy placement with minimum gap D

### Variation D: Minimize Maximum Distance to Gas Station
**Problem:** Add K gas stations to minimize max gap
**Insight:** If max gap G is achievable, G+1 is also achievable
**Verification:** For each gap, calculate how many stations needed

### Variation E: Find Kth Smallest
**Problem:** Find kth smallest element in sorted matrix
**Insight:** Binary search on value, count elements ≤ mid
**Verification:** For each row, binary search for count ≤ mid

---

## 8. Classic Interview Problems

### EASY — Classic Binary Search (Part A)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Binary Search** | Closed `[lo, hi]` loop with exact-match return | 704 |
| **Search Insert Position** | Return lower bound | 35 |
| **First Bad Version** | First T in `F F F T T T` over versions | 278 |
| **Guess Number Higher or Lower** | Closed `[lo, hi]` loop, the API gives the direction | 374 |

### MEDIUM — Classic Binary Search (Part A)
These problems search for a *position* in an array, not for a value in an answer range.

| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Find First and Last Position of Element in Sorted Array** | Lower bound and upper bound − 1 | 34 |
| **Search in Rotated Sorted Array** | Find the sorted half, range-check the target | 33 |
| **Search in Rotated Sorted Array II** | Same as 33; on `a[lo] == a[mid] == a[hi]`, shrink both ends (worst case O(n)) | 81 |
| **Find Minimum in Rotated Sorted Array** | Compare `a[mid]` with `a[hi]` | 153 |
| **Find Peak Element** | Keep the side where `a[mid] < a[mid+1]` points | 162 |
| **Search a 2D Matrix** | One sorted array of length m·n, index → `[i / n][i % n]` | 74 |
| **Search a 2D Matrix II** | NOT binary search: staircase from top-right, O(m + n) | 240 |

### MEDIUM — Binary Search on Answer (Part B)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Koko Eating Bananas** | Binary search on speed, verify hours | 875 |
| **Capacity To Ship Packages Within D Days** | Binary search on capacity | 1011 |
| **Split Array Largest Sum** | Binary search on max sum | 410 |
| **Find the Smallest Divisor** | Binary search on divisor | 1283 |
| **Magnetic Force Between Two Balls** | Maximize minimum distance | 1552 |
| **Minimum Limit of Balls in a Bag** | Binary search on max bag size | 1760 |
| **Kth Smallest Element in Sorted Matrix** | BS on value + count | 378 |
| **Find K Closest Elements** | Binary search on window start | 658 |
| **Minimized Maximum of Products Distributed to Any Store** | BS on the max per store + count stores needed | 2064 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Median of Two Sorted Arrays** | Binary search on partition | 4 |
| **Find Minimum in Rotated Sorted Array II** | 153 with duplicates; on `a[mid] == a[hi]`, do `hi -= 1` (worst case O(n)) | 154 |
| **Kth Smallest Number in Multiplication Table** | BS on value + count rows | 668 |
| **Find K-th Smallest Pair Distance** | BS on distance + count pairs | 719 |
| **Maximum Running Time of N Computers** | BS on runtime | 2141 |
| **Minimize Max Distance to Gas Station** | BS on a real-valued distance; stop after a fixed number of iterations | 774 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Core Pattern
**Day 1:**
- Understand the "binary search on answer" concept deeply
- Solve "Koko Eating Bananas" - cleanest example

**Day 2:**
- Solve "Capacity To Ship Packages Within D Days"
- Note similarity to Koko - same pattern!

**Day 3:**
- Solve "Split Array Largest Sum"
- This is the classic problem for this pattern

**Day 4:**
- Solve "Magnetic Force Between Two Balls"
- Practice "maximize minimum" variant

**Day 5:**
- Review: Write the verification function for each
- Pattern recognition drill

### Week 2: Advanced Applications
**Day 1:**
- Solve "Find K-th Smallest Pair Distance"
- Combine BS on answer with two-pointer counting

**Day 2:**
- Solve "Kth Smallest Element in Sorted Matrix"
- Practice value-based binary search

**Day 3:**
- Solve "Median of Two Sorted Arrays"
- Most elegant and difficult application

**Day 4:**
- Practice mixed problems
- Given a problem, determine bounds and verification

**Day 5:**
- Speed drill: Identify if BS on answer applies
- Practice writing bounds quickly

### The Key Skill to Develop:
1. **Recognize:** "This asks for min/max with constraint"
2. **Reframe:** "If answer is X, can I verify?"
3. **Verify:** Write the O(n) verification function
4. **Bound:** Determine lo and hi
5. **Search:** Apply appropriate template

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Wrong Bounds
```
// WRONG for "minimum ship capacity":
lo = 0  // Can't have zero capacity!

// CORRECT:
lo = max(weights)  // Must at least hold heaviest package
hi = sum(weights)  // Worst case: ship everything in one day
```

### Mistake 2: Off-by-One in Binary Search
```
// WRONG: Infinite loop when lo = hi - 1
while lo < hi:
    mid = (lo + hi) / 2
    if condition:
        lo = mid      // lo never increases if mid = lo!

// CORRECT for "find last true":
while lo < hi:
    mid = (lo + hi + 1) / 2  // Round UP
    if condition:
        lo = mid
    else:
        hi = mid - 1
```

### Mistake 3: Verification Logic Error
```
// WRONG for "split into m parts":
function canSplit(maxSum, m):
    groups = 0      // Should start at 1 (first group always exists)
    ...

// CORRECT:
function canSplit(maxSum, m):
    groups = 1      // First group starts automatically
    currentSum = 0
    for num in arr:
        if currentSum + num > maxSum:
            groups++
            currentSum = num
            if num > maxSum:   // Single element exceeds limit
                return false
        else:
            currentSum += num
    return groups <= m
```

### Mistake 4: Not Handling Single Element > mid
```
// WRONG: Doesn't check if single element exceeds limit
for num in arr:
    if currentSum + num > maxSum:
        groups++
        currentSum = num  // num might be > maxSum!

// CORRECT:
lo = max(arr)  // Ensure mid is always >= max element
// OR check inside verification
```

### Mistake 5: Wrong Direction of Binary Search
```
// WRONG for MINIMIZE problem:
if canAchieve(mid):
    lo = mid + 1   // We can achieve mid, but we want SMALLER!

// CORRECT for MINIMIZE:
if canAchieve(mid):
    hi = mid - 1   // Try to find smaller achievable value

// CORRECT for MAXIMIZE:
if canAchieve(mid):
    lo = mid + 1   // Try to find larger achievable value
```

### Mistake 6: Integer Overflow
```
// WRONG: Overflow when lo and hi are large
mid = (lo + hi) / 2

// CORRECT:
mid = lo + (hi - lo) / 2
```

### Mistake 7: Forgetting Edge Cases in Verification
```
// WRONG: Empty array, single element cases
function verify(mid):
    // Assumes array has multiple elements
    
// CORRECT: Handle edge cases
function verify(mid):
    if arr is empty:
        return true  // or appropriate value
    if length(arr) == 1:
        return arr[0] <= mid
```

---

## Bonus: How to Identify Binary Search on Answer Problems

### Step-by-Step Identification:

1. **Is there a range of possible answers?**
   - Can you define minimum and maximum possible values?
   
2. **Is there a constraint that makes some answers invalid?**
   - "Within D days", "With at most K operations"
   
3. **Is the validity monotonic?**
   - If X works, does X+1 (or X-1) also work?
   
4. **Can you verify quickly?**
   - Given answer X, can you check validity in O(n) or O(n log n)?

### Common Problem Patterns:

| Pattern | Minimize | Maximize |
|---------|----------|----------|
| Allocation/Distribution | Min capacity to finish in time | Max items per person |
| Distance/Gap | Min max gap | Max min gap |
| Rate/Speed | Min speed to finish | Max tasks per unit |
| Division/Partitioning | Min max partition sum | Max min partition |

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Binary search on the answer for "minimize the max" | DP (Pattern 15) | Can you write a fast `feasible(X)` check, and is it monotonic in X? If yes → binary search (LC 410). If the check needs the full search itself → DP. |
| Greedy for "minimum speed / capacity" | Binary search on the answer + greedy check | Is the answer one number in a large range, with a yes/no check per value? If yes → binary search wraps the greedy check (LC 875, LC 1011). |
| Heap for "k-th smallest" | Binary search on the value + count | Is the set implicit or too large to store? If yes → binary search on the value (LC 668). If you can store it and k is small → heap (LC 215). |
| Binary search on a sorted 2D matrix | Staircase walk from the top-right corner | Does each row start after the previous row ends? If yes → binary search on the flattened index (LC 74). If only rows and columns are sorted → staircase, O(m + n) (LC 240). |
| Sliding window for "longest segment with ≤ k changes" | Sliding window, not binary search | Can the window grow and shrink with a monotonic condition? If yes → sliding window in O(n) (LC 1004). Binary search on the length also works, but costs O(n log n). |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                    BINARY SEARCH CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  CLASSIC (sorted array, find an index):                          │
│  - lowerBound: lo = 0, hi = n, while lo < hi                     │
│  -   a[mid] < target ? lo = mid + 1 : hi = mid                   │
│  - upperBound: same, with a[mid] <= target                       │
│  - Rotated: find the sorted half, range-check the target         │
│  - Reads a[hi] or a[mid+1]? Use hi = n - 1                       │
│                                                                  │
│  ANSWER SPACE - WHEN TO USE:                                     │
│  - "Find min/max that satisfies constraint"                      │
│  - Validity is MONOTONIC                                         │
│  - Verification is O(n) or O(n log n)                            │
│                                                                  │
│  MINIMIZE MAXIMUM:                                               │
│  - lo = minimum possible, hi = maximum possible                  │
│  - If canAchieve(mid): hi = mid - 1 (try smaller)               │
│  - Else: lo = mid + 1                                            │
│                                                                  │
│  MAXIMIZE MINIMUM:                                               │
│  - lo = minimum possible, hi = maximum possible                  │
│  - If canAchieve(mid): lo = mid + 1 (try larger)                │
│  - Else: hi = mid - 1                                            │
│                                                                  │
│  BOUNDS CHECKLIST:                                               │
│  - lo: What's the smallest valid answer? (often max of array)    │
│  - hi: What's the largest meaningful answer? (often sum)         │
│                                                                  │
│  VERIFICATION CHECKLIST:                                         │
│  - Does it handle single element > mid?                          │
│  - Does it start counting from 1 (not 0)?                        │
│  - Does it handle empty/edge cases?                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 07: Monotonic Stack / Queue →](../07-monotonic-stack-queue/README.md)*
