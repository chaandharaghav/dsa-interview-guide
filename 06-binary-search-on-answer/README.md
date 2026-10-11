# Pattern 06: Binary Search on Answer

## 1. Pattern Name
**Binary Search on Answer** (also called Binary Search on Result Space)

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
Ask yourself: "If answer = X is achievable, is answer = X+1 also achievable?"
- If YES (for minimization problems): Binary search works!
- If YES (for maximization problems): Also works!

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

### MEDIUM (Interview Favorites)
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


> **Note on related-but-different problems:** The three problems below search on *positions* in an
> array (finding where a value/pivot/peak lives), not on an external *answer space*. They use
> binary search logic but do NOT fit the `canAchieve(mid)` feasibility predicate model above.
> Listed here for cross-reference; mentally file them as "Modified Binary Search on Position."

| **Search in Rotated Sorted Array** | Binary search on position, not answer space | 33 |
| **Find Minimum in Rotated Sorted Array** | Locate rotation pivot by position | 153 |
| **Peak Element** | Navigate by local slope, not by feasibility | 162 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Median of Two Sorted Arrays** | Binary search on partition | 4 |
| **Kth Smallest Number in Multiplication Table** | BS on value + count rows | 668 |
| **Find K-th Smallest Pair Distance** | BS on distance + count pairs | 719 |
| **Minimized Maximum of Products Distributed** | BS on max products | 2064 |
| **Maximum Running Time of N Computers** | BS on runtime | 2141 |
| **Maximum Average Pass Ratio** | Heap or BS on contribution | 1792 |

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

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│              BINARY SEARCH ON ANSWER CHEAT SHEET                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  WHEN TO USE:                                                    │
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
