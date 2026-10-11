# Pattern 09: Divide and Conquer

## 1. Pattern Name
**Divide and Conquer**

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Divide and Conquer is about **breaking a problem into smaller subproblems**, solving them independently, and **combining results** to get the final answer.

**The Key Insight:**  
If you can solve smaller instances of the same problem, and combine those solutions in a meaningful way, you have a D&C solution.

**Mental Model:**  
Think of organizing a massive pile of papers. Instead of sorting the entire pile at once, divide it into smaller piles, sort each independently, then merge the sorted piles together.

**The Three Steps:**
1. **Divide:** Split problem into smaller subproblems
2. **Conquer:** Solve subproblems recursively
3. **Combine:** Merge subproblem solutions into final answer

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Can be broken into subproblems"
- "Merge", "Combine"
- Problems that look like merge sort or quick sort
- "Count inversions", "Count pairs"
- "Build expression tree"

### Constraint Hints:
- O(n log n) is expected (suggests divide by 2, combine in O(n))
- Problem has recursive structure
- Subproblems don't overlap (otherwise DP is better)

### Classic D&C Problems:
- Merge Sort
- Quick Sort
- Binary Search
- Finding Maximum Subarray (Kadane's is better, but D&C works)
- Closest Pair of Points
- Count Inversions

---

## 4. Brute Force Thought Process

### Count Inversions Brute Force:
```
Problem: Count pairs (i, j) where i < j and arr[i] > arr[j]

Brute Force Thinking:
- For each pair (i, j) where i < j
  - Check if arr[i] > arr[j]

Time: O(n²)
Space: O(1)
```

### Why D&C is Better:
By leveraging the merge sort process, we can count inversions while sorting:
- Left half inversions + Right half inversions + Split inversions
- Split inversions counted during merge in O(n)
- Total: O(n log n)

---

## 5. Optimization Insight (The "Aha" Moment)

### The D&C Framework:
```
function divideAndConquer(problem):
    // Base case
    if problem is trivially solvable:
        return trivialSolution
    
    // Divide
    subproblems = split(problem)
    
    // Conquer
    subResults = []
    for each subproblem in subproblems:
        subResults.add(divideAndConquer(subproblem))
    
    // Combine
    return combine(subResults)
```

### When D&C Works Well:
- Subproblems are independent (no overlap)
- Combining is efficient (O(n) or O(n log n))
- Division reduces problem size significantly (typically by half)

### D&C vs DP:
- **D&C:** Subproblems don't overlap → Solve each once
- **DP:** Subproblems overlap → Memoize to avoid recomputation

---

## 6. Generalized Templates

### Template 1: Merge Sort
```
function mergeSort(arr, left, right):
    if left >= right:
        return
    
    mid = left + (right - left) / 2
    
    // Divide and Conquer
    mergeSort(arr, left, mid)
    mergeSort(arr, mid + 1, right)
    
    // Combine
    merge(arr, left, mid, right)

function merge(arr, left, mid, right):
    leftPart = arr[left...mid]
    rightPart = arr[mid+1...right]
    
    i = 0, j = 0, k = left
    
    while i < length(leftPart) and j < length(rightPart):
        if leftPart[i] <= rightPart[j]:
            arr[k++] = leftPart[i++]
        else:
            arr[k++] = rightPart[j++]
    
    // Copy remaining
    while i < length(leftPart):
        arr[k++] = leftPart[i++]
    while j < length(rightPart):
        arr[k++] = rightPart[j++]
```

### Template 2: Quick Sort
```
function quickSort(arr, low, high):
    if low >= high:
        return
    
    // Divide
    pivotIndex = partition(arr, low, high)
    
    // Conquer (no combine needed - in-place)
    quickSort(arr, low, pivotIndex - 1)
    quickSort(arr, pivotIndex + 1, high)

function partition(arr, low, high):
    pivot = arr[high]
    i = low - 1
    
    for j = low to high - 1:
        if arr[j] < pivot:
            i++
            swap(arr[i], arr[j])
    
    swap(arr[i + 1], arr[high])
    return i + 1
```

### Template 3: Count Inversions
```
function countInversions(arr):
    return mergeSortAndCount(arr, 0, length(arr) - 1)

function mergeSortAndCount(arr, left, right):
    if left >= right:
        return 0
    
    mid = left + (right - left) / 2
    
    count = 0
    count += mergeSortAndCount(arr, left, mid)
    count += mergeSortAndCount(arr, mid + 1, right)
    count += mergeAndCount(arr, left, mid, right)
    
    return count

function mergeAndCount(arr, left, mid, right):
    leftPart = arr[left...mid]
    rightPart = arr[mid+1...right]
    
    inversions = 0
    i = 0, j = 0, k = left
    
    while i < length(leftPart) and j < length(rightPart):
        if leftPart[i] <= rightPart[j]:
            arr[k++] = leftPart[i++]
        else:
            // All remaining elements in left are greater than rightPart[j]
            inversions += length(leftPart) - i
            arr[k++] = rightPart[j++]
    
    // Copy remaining (no inversions here)
    while i < length(leftPart):
        arr[k++] = leftPart[i++]
    while j < length(rightPart):
        arr[k++] = rightPart[j++]
    
    return inversions
```

### Template 4: Maximum Subarray (D&C approach)
```
function maxSubArray(arr, left, right):
    if left == right:
        return arr[left]
    
    mid = left + (right - left) / 2
    
    // Maximum in left half
    leftMax = maxSubArray(arr, left, mid)
    
    // Maximum in right half
    rightMax = maxSubArray(arr, mid + 1, right)
    
    // Maximum crossing the middle
    crossMax = maxCrossingSubarray(arr, left, mid, right)
    
    return max(leftMax, rightMax, crossMax)

function maxCrossingSubarray(arr, left, mid, right):
    // Maximum sum going left from mid
    leftSum = -INFINITY
    sum = 0
    for i = mid down to left:
        sum += arr[i]
        leftSum = max(leftSum, sum)
    
    // Maximum sum going right from mid+1
    rightSum = -INFINITY
    sum = 0
    for i = mid + 1 to right:
        sum += arr[i]
        rightSum = max(rightSum, sum)
    
    return leftSum + rightSum
```

### Template 5: Closest Pair of Points
```
function closestPair(points):
    // Sort by x-coordinate
    sortByX(points)
    return closestPairRec(points, 0, length(points) - 1)

function closestPairRec(points, left, right):
    if right - left <= 2:
        return bruteForceClosest(points, left, right)
    
    mid = left + (right - left) / 2
    midX = points[mid].x
    
    // Divide
    leftMin = closestPairRec(points, left, mid)
    rightMin = closestPairRec(points, mid + 1, right)
    
    d = min(leftMin, rightMin)
    
    // Combine: Check strip around middle line
    strip = []
    for i = left to right:
        if abs(points[i].x - midX) < d:
            strip.add(points[i])
    
    // Sort strip by y and check nearby points
    // NOTE: Sorting the strip here at every level makes this O(n log²n), not O(n log n).
    // The O(n log n) version requires merging pre-sorted Y lists from the sub-calls instead.
    sortByY(strip)
    stripMin = closestInStrip(strip, d)
    
    return min(d, stripMin)

function closestInStrip(strip, d):
    minDist = d
    for i = 0 to length(strip) - 1:
        // Only need to check next 7 points (mathematical proof)
        for j = i + 1 to min(i + 7, length(strip) - 1):
            if strip[j].y - strip[i].y >= minDist:
                break
            minDist = min(minDist, distance(strip[i], strip[j]))
    return minDist
```

### Template 6: Build Tree from Traversals
```
function buildTree(preorder, inorder):
    inorderMap = buildIndexMap(inorder)
    return build(preorder, 0, length(preorder) - 1,
                 inorder, 0, length(inorder) - 1,
                 inorderMap)

function build(preorder, preStart, preEnd,
               inorder, inStart, inEnd, inorderMap):
    if preStart > preEnd:
        return null
    
    rootVal = preorder[preStart]
    root = new TreeNode(rootVal)
    
    rootIndex = inorderMap[rootVal]
    leftSize = rootIndex - inStart
    
    root.left = build(preorder, preStart + 1, preStart + leftSize,
                      inorder, inStart, rootIndex - 1, inorderMap)
    root.right = build(preorder, preStart + leftSize + 1, preEnd,
                       inorder, rootIndex + 1, inEnd, inorderMap)
    
    return root
```

---

## 7. Variations of the Pattern

### Variation A: Reverse Pairs
**Problem:** Count pairs where i < j and nums[i] > 2*nums[j]
**Key Insight:** Modified merge sort, count before merging
**Difference from Inversions:** Condition is different, check separately

### Variation B: Count Smaller Numbers After Self
**Problem:** For each element, count how many elements to its right are smaller
**Key Insight:** Merge sort on indices, track original positions

### Variation C: Different Ways to Add Parentheses
**Problem:** Compute all possible results by adding parentheses
**Key Insight:** Divide at each operator, combine all possibilities

### Variation D: Beautiful Array
**Problem:** Construct array where no i, j, k with A[k]*2 = A[i] + A[j]
**Key Insight:** Divide by odd/even, scale and interleave

---

## 8. Classic Interview Problems

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Sort an Array** | Merge sort or quick sort | 912 |
| **Kth Largest Element** | Quick select (D&C) | 215 |
| **Construct Binary Tree from Preorder and Inorder** | D&C with maps | 105 |
| **Construct Binary Tree from Inorder and Postorder** | Similar approach | 106 |
| **Maximum Subarray** | D&C or Kadane's | 53 |
| **Different Ways to Add Parentheses** | D&C on operators | 241 |
| **Search a 2D Matrix II** | D&C or staircase | 240 |
| **Beautiful Array** | D&C construction | 932 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Count of Smaller Numbers After Self** | Merge sort with index tracking | 315 |
| **Reverse Pairs** | Modified merge sort | 493 |
| **Count of Range Sum** | Merge sort on prefix sums | 327 |
| **Median of Two Sorted Arrays** | Binary search (D&C variant) | 4 |
| **Closest Pair of Points** | Classic D&C geometry | - |
| **The Skyline Problem** | D&C or sweep line | 218 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Foundation
**Day 1:**
- Implement Merge Sort from scratch
- Understand the divide-conquer-combine structure

**Day 2:**
- Implement Quick Sort from scratch
- Understand partition function

**Day 3:**
- Solve "Count Inversions" (may need to find on other platforms)
- Key insight: count during merge

**Day 4:**
- Solve "Construct Binary Tree from Preorder and Inorder"
- Practice recursive tree construction

**Day 5:**
- Solve "Different Ways to Add Parentheses"
- Practice combining results from subproblems

### Week 2: Advanced
**Day 1:**
- Solve "Kth Largest Element" using Quick Select
- Understand partial sorting

**Day 2:**
- Solve "Count of Smaller Numbers After Self"
- Most challenging D&C problem

**Day 3:**
- Solve "Reverse Pairs"
- Practice modified counting conditions

**Day 4:**
- Solve "The Skyline Problem"
- Combine D&C with clever merging

**Day 5:**
- Review: What makes a problem suitable for D&C?

### Self-Check Questions:
- Can I identify independent subproblems?
- Can I define the combine step clearly?
- Do I understand why D&C achieves O(n log n)?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Inefficient Combine Step
```
// WRONG: O(n²) combine defeats the purpose
function combine(left, right):
    for each x in left:
        for each y in right:
            // ...

// CORRECT: O(n) combine
// Use the sorted property from subproblems
```

### Mistake 2: Not Handling Base Cases
```
// WRONG: Infinite recursion
function divideAndConquer(arr, left, right):
    mid = (left + right) / 2
    divideAndConquer(arr, left, mid)
    divideAndConquer(arr, mid + 1, right)

// CORRECT: Base case
function divideAndConquer(arr, left, right):
    if left >= right:
        return
    // ...
```

### Mistake 3: Incorrect Index Calculation
```
// WRONG: Off-by-one in subarray bounds
leftPart = arr[left...mid]
rightPart = arr[mid...right]  // mid counted twice!

// CORRECT:
leftPart = arr[left...mid]
rightPart = arr[mid+1...right]
```

### Mistake 4: Forgetting to Sort or Maintain Order
```
// WRONG for count inversions: Counting but not sorting
// Future merges will give wrong counts!

// CORRECT: Sort during merge
// The sorted arrays enable efficient counting
```

### Mistake 5: Space Complexity Oversight
```
// WRONG: Creating new arrays at each level
function merge(arr, left, mid, right):
    result = new array[right - left + 1]  // New allocation in every call
    // Total allocation is O(n log n); peak extra space is still O(n),
    // because only one merge buffer is in use at a time. The cost is allocation time.

// CORRECT: Reuse temp array or merge in-place
// Pre-allocate temp array once
```

---

## Bonus: Master Theorem for D&C Complexity

For recurrence T(n) = aT(n/b) + O(n^d):

| Case | Condition | Complexity |
|------|-----------|------------|
| 1 | d < log_b(a) | O(n^(log_b(a))) |
| 2 | d = log_b(a) | O(n^d log n) |
| 3 | d > log_b(a) | O(n^d) |

**Examples:**
- Merge Sort: T(n) = 2T(n/2) + O(n) → d=1, log_2(2)=1 → O(n log n)
- Binary Search: T(n) = T(n/2) + O(1) → d=0, log_2(1)=0 → O(log n)
- Naive Matrix Mult: T(n) = 8T(n/2) + O(n²) → d=2, log_2(8)=3 → O(n³)
- Strassen's: T(n) = 7T(n/2) + O(n²) → d=2, log_2(7)≈2.8 → O(n^2.8)

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| D&C recursion on "f(n) from smaller f values" | Dynamic programming (Pattern 15) | Do two recursive branches call the same arguments? If yes, subproblems overlap → memoize or DP (LC 70, LC 509) |
| D&C for "search in a sorted array" | Binary search (Pattern 06) | Does each step discard one half without solving it? If yes, there is no combine step → binary search (LC 704) |
| D&C (Template 4) for "maximum subarray sum" | Kadane's algorithm (Pattern 04) | Is the input one array with no range queries? If yes → Kadane, O(n) instead of O(n log n) (LC 53) |
| Quickselect for "kth largest" | Heap (Pattern 16) | Is the input a stream, or must the array stay unmodified? If yes → size-k heap, O(n log k); if no → quickselect, O(n) average (LC 215) |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                 DIVIDE AND CONQUER CHEAT SHEET                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  THREE STEPS:                                                    │
│  1. DIVIDE: Split into smaller subproblems                       │
│  2. CONQUER: Solve subproblems recursively                       │
│  3. COMBINE: Merge solutions into final answer                   │
│                                                                  │
│  WHEN TO USE:                                                    │
│  - Subproblems are independent (no overlap)                      │
│  - Problem reduces by factor (typically /2)                      │
│  - Combine step is efficient (O(n) or better)                    │
│                                                                  │
│  COMMON PATTERNS:                                                │
│  - Merge Sort: Count inversions, range queries                   │
│  - Quick Select: Kth element in O(n) average                     │
│  - Tree Construction: Build from traversals                      │
│  - Expression: Different ways to parenthesize                    │
│                                                                  │
│  COMPLEXITY (typically):                                         │
│  - T(n) = 2T(n/2) + O(n) → O(n log n) [Closest Pair impl is O(n log²n); see template note]│
│  - T(n) = T(n/2) + O(n) → O(n) avg (Quick Select; simplified recurrence, not exact)│
│                                                                  │
│  GOTCHAS:                                                        │
│  - Don't forget base case                                        │
│  - Combine must be efficient                                     │
│  - Watch for off-by-one in indices                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 10: Tree DFS / BFS Patterns →](../10-tree-patterns/README.md)*
