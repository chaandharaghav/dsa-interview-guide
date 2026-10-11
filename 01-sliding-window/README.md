# Pattern 01: Sliding Window

## 1. Pattern Name
**Sliding Window** (Fixed Size & Variable Size)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

The sliding window pattern is about **maintaining a "view" into a contiguous portion of data** and efficiently updating that view as you move through the input.

**The Key Insight:**  
Instead of recalculating everything for each possible window from scratch, we **add one element** and **remove one element** at a time, keeping our calculation incrementally updated.

Think of it like a **train window** - as the train moves, you don't re-observe everything from the start. You just note what new scenery enters and what old scenery leaves.

### Two Flavors:

1. **Fixed Window:** Window size is given (e.g., "find max sum of k consecutive elements")
2. **Variable Window:** Window size changes based on some condition (e.g., "find smallest subarray with sum ≥ target")

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Subarray" or "Substring" (NOT subsequence - that's different!)
- "Contiguous"
- "Window"
- "Consecutive"
- "Maximum/Minimum of all subarrays of size k"
- "Longest/Shortest subarray satisfying..."

### Constraint Hints:
- Input size is large (10^5 or more) - brute force O(n²) won't work
- You need to find something about ALL subarrays or an OPTIMAL subarray

### Input/Output Hints:
- Input: Array or String
- Output: Single value (max, min, count) or a subarray/substring

### RED FLAGS - NOT Sliding Window:
- "Subsequence" (elements don't need to be contiguous)
- Need to consider ALL subsets
- Elements can be rearranged
- Need to track individual element positions that aren't adjacent

---

## 4. Brute Force Thought Process

### Fixed Window Brute Force:
```
Problem: Find maximum sum of any subarray of size k

Brute Force Thinking:
- For each starting index i from 0 to n-k
  - Sum elements from i to i+k-1
  - Track maximum

Time: O(n * k)
Space: O(1)
```

### Why It Times Out:
If n = 10^5 and k = 10^4, we do 10^9 operations. Too slow.

**The Waste:** We're recalculating almost the same sum repeatedly. 
- Window [0,1,2,3,4] sums: a[0]+a[1]+a[2]+a[3]+a[4]
- Window [1,2,3,4,5] sums: a[1]+a[2]+a[3]+a[4]+a[5]
- We recalculated a[1]+a[2]+a[3]+a[4] unnecessarily!

### Variable Window Brute Force:
```
Problem: Find shortest subarray with sum ≥ target

Brute Force Thinking:
- For each starting index i
  - For each ending index j (j >= i)
    - Calculate sum from i to j
    - If sum >= target, record length

Time: O(n³) or O(n²) with prefix sum
Space: O(1) or O(n)
```

---

## 5. Optimization Insight (The "Aha" Moment)

### Fixed Window Insight:
**"I only need to update what changes."**

When sliding from window [i, i+k-1] to [i+1, i+k]:
- REMOVE: element at index i (leaving the window)
- ADD: element at index i+k (entering the window)

```
New Sum = Old Sum - arr[i] + arr[i+k]
```

This transforms O(k) per window → O(1) per window.

### Variable Window Insight:
**"I never need to shrink and re-expand from scratch."**

Key realization: Once we've found a valid window, we only need to:
1. Try to SHRINK it (to find a smaller valid window)
2. When invalid, EXPAND by moving right pointer

**Why this works:** If window [i,j] is invalid (doesn't meet condition), then any window starting at i with end < j is also invalid. So we don't need to re-check them.

The two-pointer nature means each element is visited at most twice (once when expanding, once when shrinking).

---

## 6. Generalized Templates

### Template 1: Fixed Size Window
```
function fixedWindow(arr, k):
    n = length(arr)
    
    // Build first window
    windowValue = calculateInitialWindow(arr, 0, k-1)
    result = windowValue
    
    // Slide the window
    for i = k to n-1:
        // Add new element (entering window)
        windowValue = addElement(windowValue, arr[i])
        
        // Remove old element (leaving window)
        windowValue = removeElement(windowValue, arr[i-k])
        
        // Update result
        result = updateResult(result, windowValue)
    
    return result
```

### Template 2: Variable Size Window (Shrinkable)
```
function variableWindow(arr):
    left = 0
    result = initialValue
    windowState = initialState
    
    for right = 0 to n-1:
        // Expand: add arr[right] to window
        updateWindowState(windowState, arr[right])
        
        // Shrink: while window is invalid or can be optimized
        while windowIsInvalidOrCanShrink(windowState):
            // Remove arr[left] from window
            removeFromWindowState(windowState, arr[left])
            left = left + 1
        
        // Update result with current valid window
        result = updateResult(result, right - left + 1, windowState)
    
    return result
```

### Template 3: Variable Window (Find Longest Valid)
```
function longestValidWindow(arr):
    left = 0
    maxLength = 0
    windowState = initialState
    
    for right = 0 to n-1:
        // Expand window
        addToWindow(arr[right])
        
        // Shrink until valid
        while not isValid(windowState):
            removeFromWindow(arr[left])
            left = left + 1
        
        // Window [left, right] is now valid
        maxLength = max(maxLength, right - left + 1)
    
    return maxLength
```

### Template 4: Variable Window (Find Shortest Valid)
```
function shortestValidWindow(arr, target):
    left = 0
    minLength = INFINITY
    windowState = initialState
    
    for right = 0 to n-1:
        // Expand window
        addToWindow(arr[right])
        
        // Shrink while still valid (to minimize length)
        while isValid(windowState):
            minLength = min(minLength, right - left + 1)
            removeFromWindow(arr[left])
            left = left + 1
    
    return minLength == INFINITY ? -1 : minLength
```

---

## 7. Variations of the Pattern

### Variation A: Maximum/Minimum of Fixed Windows
**Problem Type:** Find max/min element in each window of size k
**Challenge:** Simple sliding doesn't work because you can't easily "remove" max
**Solution:** Use Monotonic Deque (covered in Pattern 07)

### Variation B: Window with Frequency Constraint
**Problem Type:** Longest substring with at most k distinct characters
**Key Structure:** HashMap to track frequency of elements in window
**Shrink Condition:** When distinct count > k

### Variation C: Window with Multiple Conditions
**Problem Type:** Find all anagrams of pattern in string
**Key Structure:** Two frequency maps (pattern and current window)
**Match Condition:** Both maps are equal

### Variation D: Counting Valid Windows
**Problem Type:** Count subarrays with sum exactly equal to k
**Trick:** Can't directly use sliding window for "exactly k"
**Solution:** Count(sum ≤ k) - Count(sum ≤ k-1), only if all values are non-negative (e.g. LC 930, LC 1248)
**Condition:** The atMost trick for sums requires every value ≥ 0. Removing a negative value from the left raises the window sum, so shrinking does not always restore sum ≤ k. With negative values, use prefix sum + hashmap (LC 560, Pattern 04).

### Variation E: Sum of Subarray Minimums (uses a monotonic stack)
**Problem Type:** Find sum of min of all subarrays
**Key:** Not a sliding window. Use a monotonic stack to find the span where each element is the minimum, then count its contribution (LC 907, Pattern 07)

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight |
|---------|-------------|
| Maximum Sum Subarray of Size K | Basic fixed window template |
| Average of Subarrays of Size K | Fixed window, track sum |
| Max Consecutive Ones | Variable window, count zeros |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Longest Substring Without Repeating Characters** | Variable window + HashSet | 3 |
| **Minimum Window Substring** | Variable window + 2 frequency maps | 76 |
| **Longest Substring with At Most K Distinct** | Variable window + HashMap | 340 |
| **Find All Anagrams in a String** | Fixed window + frequency match | 438 |
| **Permutation in String** | Fixed window + frequency match | 567 |
| **Fruit Into Baskets** | At most 2 distinct (same as k=2) | 904 |
| **Max Consecutive Ones III** | At most k zeros in window | 1004 |
| **Subarray Product Less Than K** | Variable window, shrink when ≥k | 713 |
| **Minimum Size Subarray Sum** | Shortest window with sum ≥ target | 209 |
| **Grumpy Bookstore Owner** | Fixed window optimization | 1052 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Sliding Window Maximum** | Monotonic deque | 239 |
| **Minimum Window Subsequence** | DP + Two Pointers | 727 |
| **Substring with Concatenation of All Words** | Multiple fixed windows | 30 |
| **Subarrays with K Different Integers** | atMost(k) - atMost(k-1) | 992 |
| **Count Subarrays Where Max Element Appears at Least K Times** | Track max element count | 2962 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Fixed Window Mastery
**Day 1-2:**
- Solve 3-4 basic fixed window problems
- Focus on: Building the initial window, sliding correctly

**Day 3-4:**
- Solve "Find All Anagrams" and "Permutation in String"
- Focus on: Frequency map comparison techniques

**Day 5:**
- Review and identify any gaps

### Week 2: Variable Window Mastery
**Day 1-2:**
- Solve "Longest Substring Without Repeating Characters"
- Solve "Longest Substring with At Most K Distinct"
- Focus on: When to shrink, when to expand

**Day 3-4:**
- Solve "Minimum Size Subarray Sum"
- Solve "Minimum Window Substring" (this is THE classic)
- Focus on: Finding shortest vs longest

**Day 5:**
- Solve "Subarrays with K Different Integers"
- Master the atMost(k) - atMost(k-1) technique

### Practice Methodology:
1. **First 5 minutes:** Identify it's a sliding window problem. Which template?
2. **Next 10 minutes:** Write pseudocode with your template
3. **Remaining time:** Implement and debug
4. **After solving:** Can you optimize space? Can you handle edge cases?

### Self-Check Questions:
- Can I identify sliding window within 2 minutes?
- Do I know when to use fixed vs variable?
- Can I write the shrinking condition correctly?
- Do I handle empty array / k > n cases?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Off-by-One Errors in Window Boundaries
```
// WRONG: Window size is k, but this iterates k+1 times
for i = 0 to k:
    sum += arr[i]

// CORRECT: 
for i = 0 to k-1:
    sum += arr[i]
```

### Mistake 2: Forgetting to Handle Initial Window Separately
```
// WRONG: Starts sliding immediately
for right = 0 to n-1:
    add arr[right]
    remove arr[right - k]  // right - k is negative initially!

// CORRECT: Build first window, then slide
for i = 0 to k-1:
    add arr[i]
for right = k to n-1:
    add arr[right]
    remove arr[right - k]
```

### Mistake 3: Shrinking Too Much or Too Little
```
// WRONG: Shrinks even when unnecessary
while left < right:
    shrink()

// CORRECT: Shrink only when condition violated
while not isValid():
    shrink()
```

### Mistake 4: Using Sliding Window for "Exactly K" Problems
```
// WRONG approach for "count subarrays with exactly k distinct"
// Direct sliding window doesn't work!

// CORRECT: Use the atMost technique
return atMost(k) - atMost(k-1)
```

### Mistake 5: Not Updating Result at the Right Time
```
// For "longest valid window" - update AFTER shrinking
// For "shortest valid window" - update WHILE shrinking

// WRONG for shortest:
while isValid():
    shrink()
minLen = right - left + 1  // Window might be invalid now!

// CORRECT for shortest:
while isValid():
    minLen = min(minLen, right - left + 1)
    shrink()
```

### Mistake 6: HashMap Not Cleaned Up Properly
```
// WRONG: Just decrement (when distinct count is measured via map.size())
map[char] -= 1

// CORRECT (for size()-based distinctness): Remove entry when count reaches 0
map[char] -= 1
if map[char] == 0:
    remove char from map
```
> **Qualifier:** Remove-at-zero is correct **only** when you measure distinct characters via
> `map.size()`. For min-window-substring style problems that use per-character count thresholds,
> zero and negative counts are intentional — removing the entry causes incorrect `formed` counter
> updates. Check which distinctness model your problem uses before applying this pattern.

### Mistake 7: Confusing Subsequence with Subarray
- **Subarray:** Contiguous → Sliding Window works
- **Subsequence:** Not necessarily contiguous → Sliding Window WON'T work

---

## Bonus: Interview Script for Sliding Window Problems

When you recognize a sliding window problem, here's how to communicate:

> "I notice this problem involves finding an optimal **contiguous** subarray/substring, which suggests a sliding window approach. Let me first consider the brute force: checking all O(n²) subarrays would be too slow given the constraints of n = 10^5.
>
> The key insight is that as I move my window, I can maintain the state incrementally rather than recalculating from scratch. 
>
> For this problem, I'll use a [fixed/variable] size window because [explain why]. My window state will track [sum/frequency map/etc.], and I'll [expand right pointer always / shrink left when condition X].
>
> Let me trace through a small example before coding..."

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Sliding window on "subarray sum = k" | Prefix sum + hashmap (Pattern 04) | Can values be negative? If yes, the window sum is not monotonic → prefix sum + hashmap (LC 560) |
| Sliding window on "find a pair with sum = target" | Two pointers (Pattern 02) | Does the answer depend on 2 elements, or on every element in a contiguous range? 2 elements → two pointers (LC 167) |
| Sliding window for "max of each window of size k" | Monotonic deque (Pattern 07) | Must the window state support removing the max or min? If yes → monotonic deque (LC 239) |
| Sliding window on "longest common subsequence" | Dynamic programming (Pattern 15) | Does the problem say "subsequence"? If yes, elements need not be contiguous → DP (LC 1143) |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                    SLIDING WINDOW CHEAT SHEET                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  FIXED WINDOW:                                                   │
│  - Build initial window of size k                                │
│  - Slide: add right, remove left                                 │
│  - Update result each step                                       │
│                                                                  │
│  VARIABLE WINDOW (Longest):                                      │
│  - Expand right always                                           │
│  - Shrink left while INVALID                                     │
│  - Update result AFTER shrinking                                 │
│                                                                  │
│  VARIABLE WINDOW (Shortest):                                     │
│  - Expand right always                                           │
│  - Shrink left while VALID                                       │
│  - Update result BEFORE/DURING shrinking                         │
│                                                                  │
│  "EXACTLY K" PROBLEMS:                                           │
│  - Use: atMost(k) - atMost(k-1)                                  │
│  - For SUMS: only if all values ≥ 0 (else prefix sum + hashmap)  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 02: Two Pointers →](../02-two-pointers/README.md)*
