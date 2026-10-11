# Pattern 07: Monotonic Stack & Queue

## 1. Pattern Name
**Monotonic Stack** & **Monotonic Queue** (Deque)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Monotonic structures maintain elements in a **sorted order** (increasing or decreasing) while processing a sequence. They're perfect for problems asking about "next greater/smaller" or "max/min in a window."

**The Key Insight:**  
Some elements become "useless" for future queries. By removing them eagerly, we maintain only relevant candidates.

**Monotonic Stack Mental Model:**
Imagine standing in a line looking at buildings ahead. Once a taller building blocks your view of shorter ones behind it, those shorter buildings become irrelevant for "what's the next taller building" questions.

**Monotonic Queue Mental Model:**
Same concept, but elements also expire from the front (sliding window constraint).

---

## 3. Recognition Signals

### Monotonic Stack Keywords:
- "Next greater element"
- "Next smaller element"  
- "Previous greater/smaller element"
- "Days until warmer temperature"
- "Stock span" (days of consecutive smaller/equal prices)
- "Largest rectangle in histogram"
- "Trapping rain water"

### Monotonic Queue Keywords:
- "Maximum/minimum in sliding window"
- "Fixed window size k"
- "Shortest subarray with sum at least K"

### Constraint Hints:
- Need O(n) time but looking for relationships between elements
- Each element needs to "find" another element based on relative value
- Sliding window max/min problems

---

## 4. Brute Force Thought Process

### Next Greater Element Brute Force:
```
Problem: For each element, find the next greater element to its right

Brute Force Thinking:
- For each index i
  - Scan right until finding arr[j] > arr[i]
  - Store result

Time: O(n²)
Space: O(n) for result
```

### Sliding Window Maximum Brute Force:
```
Problem: Find maximum in each window of size k

Brute Force Thinking:
- For each window position
  - Scan k elements to find maximum

Time: O(n * k)
Space: O(n - k + 1) for result
```

---

## 5. Optimization Insight (The "Aha" Moment)

### Monotonic Stack Insight:
**"Elements that will never be someone's answer can be discarded."**

For "next greater element":
- Process from right to left
- Maintain a stack of "candidates" for next greater
- When we see element X:
  - Pop all elements ≤ X (X blocks view of smaller elements)
  - Stack top is next greater of X
  - Push X onto stack

**Why this is O(n):** Each element is pushed once and popped at most once. Total operations ≤ 2n.

### Monotonic Queue Insight:
**"Keep only elements that could potentially be the maximum."**

For "sliding window maximum":
- Maintain decreasing deque of (value, index) or just indices
- When adding new element:
  - Remove elements smaller than it from back (they'll never be max)
  - Add new element to back
- When moving window:
  - Remove elements outside window from front
- Front of deque is always the maximum

---

## 6. Generalized Templates

### Template 1: Next Greater Element (Right)
```
function nextGreaterElement(arr):
    n = length(arr)
    result = new array[n] filled with -1  // -1 means no greater element
    stack = empty stack  // Stores indices
    
    for i = n-1 down to 0:  // Process right to left
        // Pop elements smaller than or equal to current
        while stack not empty and arr[stack.top()] <= arr[i]:
            stack.pop()
        
        // Stack top is next greater (if exists)
        if stack not empty:
            result[i] = arr[stack.top()]
        
        // Push current index
        stack.push(i)
    
    return result
```

### Template 2: Next Greater Element (Circular Array)
```
function nextGreaterCircular(arr):
    n = length(arr)
    result = new array[n] filled with -1
    stack = empty stack
    
    // Iterate twice to handle circular
    for i = 2*n - 1 down to 0:
        actualIndex = i % n
        
        while stack not empty and arr[stack.top()] <= arr[actualIndex]:
            stack.pop()
        
        if i < n:  // Only record results in first pass
            if stack not empty:
                result[actualIndex] = arr[stack.top()]
        
        stack.push(actualIndex)
    
    return result
```

### Template 3: Previous Greater Element
```
function prevGreaterElement(arr):
    n = length(arr)
    result = new array[n] filled with -1
    stack = empty stack
    
    for i = 0 to n-1:  // Process left to right
        while stack not empty and arr[stack.top()] <= arr[i]:
            stack.pop()
        
        if stack not empty:
            result[i] = arr[stack.top()]
        
        stack.push(i)
    
    return result
```

### Template 4: Sliding Window Maximum (Monotonic Deque)
```
function slidingWindowMax(arr, k):
    n = length(arr)
    result = []
    deque = empty deque  // Stores indices, front is max
    
    for i = 0 to n-1:
        // Remove indices outside current window
        while deque not empty and deque.front() <= i - k:
            deque.popFront()
        
        // Remove elements smaller than current (they'll never be max)
        while deque not empty and arr[deque.back()] <= arr[i]:
            deque.popBack()
        
        deque.pushBack(i)
        
        // Window is complete, record maximum
        if i >= k - 1:
            result.add(arr[deque.front()])
    
    return result
```

### Template 5: Largest Rectangle in Histogram
```
function largestRectangle(heights):
    n = length(heights)
    stack = empty stack  // Stores indices
    maxArea = 0
    
    for i = 0 to n:  // Note: iterate to n (one past end)
        // Current height (0 for position n to flush remaining)
        h = (i == n) ? 0 : heights[i]
        
        while stack not empty and heights[stack.top()] > h:
            height = heights[stack.pop()]
            // Width: from previous stack element to current position
            width = stack.isEmpty() ? i : i - stack.top() - 1
            maxArea = max(maxArea, height * width)
        
        stack.push(i)
    
    return maxArea
```

### Template 6: Sum of Subarray Minimums
```
function sumOfSubarrayMins(arr):
    n = length(arr)
    MOD = 10^9 + 7
    
    // For each element, find how many subarrays have it as minimum
    // Need: previous less element (PLE) and next less element (NLE)
    
    ple = new array[n] filled with -1   // Index of previous less
    nle = new array[n] filled with n    // Index of next less
    stack = empty stack
    
    // Find previous less element
    for i = 0 to n-1:
        while stack not empty and arr[stack.top()] >= arr[i]:
            stack.pop()
        if stack not empty:
            ple[i] = stack.top()
        stack.push(i)
    
    // Clear and find next less element
    stack.clear()
    for i = n-1 down to 0:
        while stack not empty and arr[stack.top()] > arr[i]:  // > not >= to handle duplicates
            stack.pop()
        if stack not empty:
            nle[i] = stack.top()
        stack.push(i)
    
    // Calculate contribution of each element
    result = 0
    for i = 0 to n-1:
        leftCount = i - ple[i]        // Subarrays starting from ple[i]+1 to i
        rightCount = nle[i] - i       // Subarrays ending from i to nle[i]-1
        contribution = arr[i] * leftCount * rightCount
        result = (result + contribution) % MOD
    
    return result
```

---

## 7. Variations of the Pattern

### Variation A: Daily Temperatures
**Problem:** Days until warmer temperature
**Key Insight:** This is "next greater index" - subtract to get days
```
result[i] = nextGreaterIndex[i] - i
```

### Variation B: Stock Span
**Problem:** Count consecutive days with price ≤ today
**Key Insight:** This is "previous greater index" - calculate span
```
span[i] = i - previousGreaterIndex[i]
```

### Variation C: Trapping Rain Water
**Problem:** Calculate trapped water between bars
**Approach 1:** Stack-based (horizontal layers)
**Approach 2:** Two pointers with maxLeft/maxRight (more common)

### Variation D: Maximal Rectangle in Binary Matrix
**Problem:** Largest rectangle of 1s in a 2D matrix
**Key Insight:** Convert each row to histogram, apply largest rectangle
```
For each row:
  heights[j] = matrix[i][j] == 1 ? heights[j] + 1 : 0
  Apply largestRectangle(heights)
```

### Variation E: Remove K Digits
**Problem:** Remove k digits to get smallest number
**Key Insight:** Monotonic stack to keep increasing digits

---

## 8. Classic Interview Problems

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Next Greater Element I** | HashMap + Stack for subset | 496 |
| **Next Greater Element II** | Circular array, iterate 2x | 503 |
| **Daily Temperatures** | Next greater, return difference | 739 |
| **Online Stock Span** | Previous greater, count span | 901 |
| **Sliding Window Maximum** | Monotonic deque | 239 |
| **132 Pattern** | Track min on left, stack for middle | 456 |
| **Remove K Digits** | Maintain increasing stack | 402 |
| **Remove Duplicate Letters** | Stack + last occurrence + used | 316 |
| **Sum of Subarray Minimums** | Contribution counting | 907 |
| **Largest Rectangle in Histogram** | Classic stack problem | 84 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Maximal Rectangle** | Per-row histogram + largest rect | 85 |
| **Trapping Rain Water** | Stack or two pointers | 42 |
| **Shortest Subarray with Sum at Least K** | Monotonic deque + prefix sum | 862 |
| **Maximum Width Ramp** | Decreasing stack for starts | 962 |
| **Sum of Subarray Ranges** | Sum of max - sum of min | 2104 |
| **Number of Visible People in a Queue** | Stack counting | 1944 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Monotonic Stack Basics
**Day 1:**
- Implement "Next Greater Element" from scratch
- Understand why each element is pushed/popped once

**Day 2:**
- Solve "Daily Temperatures"
- Solve "Next Greater Element II" (circular)

**Day 3:**
- Solve "Online Stock Span" (previous greater)
- Note: It's the mirror of next greater

**Day 4:**
- Solve "Largest Rectangle in Histogram"
- This is THE classic monotonic stack problem
- Draw out examples manually first

**Day 5:**
- Review: Can you explain why O(n)?
- Practice identifying next greater vs previous greater

### Week 2: Advanced Applications
**Day 1:**
- Solve "Sum of Subarray Minimums"
- Master the contribution counting technique

**Day 2:**
- Solve "Maximal Rectangle"
- Combine with histogram technique

**Day 3:**
- Solve "Sliding Window Maximum" (monotonic deque)
- Understand front removal for window expiry

**Day 4:**
- Solve "Remove K Digits"
- Stack for building smallest number

**Day 5:**
- Mixed practice: Given a problem, decide if monotonic stack helps

### Key Skills to Develop:
1. Recognize "next/previous greater/smaller" pattern
2. Know when to process left-to-right vs right-to-left
3. Handle duplicates correctly (> vs >=)
4. Calculate contributions for sum problems

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Wrong Comparison Direction
```
// WRONG for next greater: Using >= instead of >
while stack not empty and arr[stack.top()] >= arr[i]:
    stack.pop()
// This pops equal elements, which might be the answer!

// CORRECT for next greater:
while stack not empty and arr[stack.top()] <= arr[i]:
    stack.pop()
// Keep strictly greater elements

// Note: For some problems (handling duplicates), you DO need >= or <
// Understand the problem requirements!
```

### Mistake 2: Processing Direction Confusion
```
// Next greater element: Process RIGHT to LEFT
// Why? Stack holds "future" elements (to the right)

// Previous greater element: Process LEFT to RIGHT
// Why? Stack holds "past" elements (to the left)
```

### Mistake 3: Not Handling Remaining Stack Elements
```
// WRONG for largest rectangle:
for i = 0 to n-1:
    while stack not empty and heights[stack.top()] > heights[i]:
        // process
    stack.push(i)
// Elements left in stack never processed!

// CORRECT: Process remaining by adding dummy 0 at end
for i = 0 to n:  // Note: iterate to n
    h = (i == n) ? 0 : heights[i]
    // ... rest of logic
```

### Mistake 4: Width Calculation in Histogram
```
// WRONG: Not handling empty stack case
width = i - stack.top() - 1

// CORRECT:
width = stack.isEmpty() ? i : i - stack.top() - 1
// If stack empty, bar extends from 0 to i-1
```

### Mistake 5: Duplicate Handling in Contribution Counting
```
// WRONG: Using same comparison for both sides
// This double counts subarrays with duplicate minimums

// CORRECT: Use strict inequality on one side
// Previous less: >= (include equal on left)
// Next less: > (exclude equal on right)
// OR vice versa, but be consistent
```

### Mistake 6: Off-by-One in Sliding Window
```
// WRONG: Starting to record results too early
for i = 0 to n-1:
    // ... deque operations
    result.add(arr[deque.front()])  // Records before window is complete!

// CORRECT: Start recording when window is complete
for i = 0 to n-1:
    // ... deque operations
    if i >= k - 1:
        result.add(arr[deque.front()])
```

### Mistake 7: Using Value Instead of Index
```
// WRONG: Stack stores values, can't compute width/distance
stack.push(arr[i])

// CORRECT: Stack stores indices, look up value when needed
stack.push(i)
value = arr[stack.top()]
```

---

## Bonus: Monotonic Stack Cheat Sheet for Different Problems

| Problem Type | Stack Order | Process Direction | Comparison |
|--------------|-------------|-------------------|------------|
| Next Greater | Decreasing | Right to Left | > |
| Next Smaller | Increasing | Right to Left | < |
| Prev Greater | Decreasing | Left to Right | > |
| Prev Smaller | Increasing | Left to Right | < |
| Largest Rectangle | Increasing | Left to Right | < |
| Sliding Max | Decreasing | Left to Right | ≥ |
| Sliding Min | Increasing | Left to Right | ≤ |

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│              MONOTONIC STACK/QUEUE CHEAT SHEET                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  MONOTONIC STACK:                                                │
│  - Stores indices (not values)                                   │
│  - Each element pushed once, popped once → O(n)                  │
│  - Decreasing stack → finds next/prev GREATER                    │
│  - Increasing stack → finds next/prev SMALLER                    │
│                                                                  │
│  MONOTONIC DEQUE (Sliding Window):                               │
│  - Front: maximum/minimum of current window                      │
│  - Remove expired indices from front                             │
│  - Remove "useless" elements from back before adding             │
│                                                                  │
│  CONTRIBUTION COUNTING (Sum of Subarray Mins):                   │
│  - Find range where each element is minimum                      │
│  - Contribution = value × leftCount × rightCount                 │
│  - Handle duplicates with strict vs non-strict comparison        │
│                                                                  │
│  LARGEST RECTANGLE:                                              │
│  - Increasing stack                                              │
│  - Pop when current height is smaller                            │
│  - Width = (stack empty) ? i : i - stack.top() - 1               │
│  - Add dummy 0 at end to flush remaining                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 08: Recursion & Backtracking →](../08-recursion-backtracking/README.md)*
