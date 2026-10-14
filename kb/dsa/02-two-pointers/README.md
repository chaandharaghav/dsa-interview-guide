# Pattern 02: Two Pointers

## 1. Pattern Name
**Two Pointers** (Opposite Direction & Same Direction)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Two Pointers is about **reducing search space intelligently** by using two indices that move based on some condition. Instead of checking all pairs O(n²), we eliminate impossible pairs in each step.

**The Key Insight:**  
If the array has some **structure** (sorted, or problem has monotonic property), we can make intelligent decisions about which direction to move each pointer, eliminating large chunks of possibilities at once.

**Mental Model:**  
Imagine two people searching a sorted bookshelf from opposite ends. Based on what they find, they can decide who should move inward, because certain combinations become impossible.

### Two Main Flavors:

1. **Opposite Direction:** Start from both ends, move toward middle
   - Classic: Two Sum in sorted array, Container with Most Water
   
2. **Same Direction:** Both start from beginning/same point, move forward at different speeds
   - Classic: Remove duplicates, Merge sorted arrays

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Sorted array" + finding pairs
- "In-place" modification
- "Two elements that satisfy..."
- "Pair", "Triplet"
- "Partitioning"
- "Palindrome"
- "Merge" sorted arrays/lists

### Constraint Hints:
- Array is sorted (or can be sorted without losing information)
- Need O(n) or O(n log n) time
- Space requirement is O(1)
- Looking for pairs/triplets with some sum/difference condition

### Input/Output Hints:
- Input: Often a sorted array or string
- Output: Pair(s) meeting criteria, or modified array in-place

### RED FLAGS - NOT Two Pointers:
- Original indices matter AND sorting the array would destroy them (e.g., you need pairs by their original positions, not values)
- Array is unsorted AND sorting would lose important position info
- Need to consider non-adjacent elements without clear movement rules

---

## 4. Brute Force Thought Process

### Pair Finding Brute Force:
```
Problem: Find if there exist two numbers in sorted array that sum to target

Brute Force Thinking:
- For each element at index i
  - For each element at index j > i
    - Check if arr[i] + arr[j] == target

Time: O(n²)
Space: O(1)
```

### Why It Times Out:
For n = 10^5, we'd do 5 * 10^9 operations.

**The Waste:** We're checking pairs we could have ruled out immediately.
- If arr[0] + arr[n-1] < target, then arr[0] + arr[anything less than n-1] is also < target
- We checked all those pairs unnecessarily!

### Merging Brute Force:
```
Problem: Merge sorted arrays into one sorted array

Brute Force Thinking:
- Concatenate arrays
- Sort the result

Time: O((n+m) log(n+m))
Space: O(n+m) for new array
```

Two pointers can do this in O(n+m) time.

---

## 5. Optimization Insight (The "Aha" Moment)

### Opposite Direction Insight:
**"Each comparison eliminates an entire row or column of possibilities."**

For Two Sum on sorted array:
- Start with left = 0, right = n-1
- If sum > target: We know arr[right] is too large to pair with ANYTHING from left onwards (since array is sorted). Move right--
- If sum < target: We know arr[left] is too small to pair with ANYTHING from right backwards. Move left++
- If sum == target: Found it!

**Why this works:** Each move eliminates one element from consideration permanently. We never re-visit eliminated elements.

### Same Direction Insight:
**"The slow pointer tracks where to place the next valid element."**

For removing duplicates:
- Slow pointer = position to write next unique element
- Fast pointer = current element being examined
- When we find something new, write it at slow position and advance slow

**Why this works:** We maintain two conceptual regions:
- [0, slow): Processed, valid elements
- [slow, fast): Processed, invalid/duplicate elements  
- [fast, n): Unprocessed

---

## 6. Generalized Templates

### Template 1: Opposite Direction (Two Sum variant)
```
function oppositePointers(arr, target):
    left = 0
    right = length(arr) - 1
    
    while left < right:
        currentValue = calculate(arr[left], arr[right])
        
        if currentValue == target:
            return [left, right]  // or process the pair
        else if currentValue < target:
            left = left + 1       // need larger sum
        else:
            right = right - 1     // need smaller sum
    
    return NOT_FOUND
```

### Template 2: Opposite Direction (Collect All Pairs)
```
function findAllPairs(arr, target):
    sort(arr)  // if not already sorted
    left = 0
    right = length(arr) - 1
    results = []
    
    while left < right:
        sum = arr[left] + arr[right]
        
        if sum == target:
            results.add([arr[left], arr[right]])
            
            // Skip duplicates (important for unique pairs)
            while left < right and arr[left] == arr[left + 1]:
                left = left + 1
            while left < right and arr[right] == arr[right - 1]:
                right = right - 1
            
            left = left + 1
            right = right - 1
        else if sum < target:
            left = left + 1
        else:
            right = right - 1
    
    return results
```

### Template 3: Same Direction (Remove/Transform In-place)
```
function removeInPlace(arr, condition):
    slow = 0  // next position to write
    
    for fast = 0 to length(arr) - 1:
        if shouldKeep(arr[fast], condition):
            arr[slow] = arr[fast]
            slow = slow + 1
    
    return slow  // new length of valid portion
```

### Template 4: Same Direction (Partition)
```
function partition(arr, pivotValue):
    insertPos = 0
    
    for i = 0 to length(arr) - 1:
        if arr[i] < pivotValue:
            swap(arr[insertPos], arr[i])
            insertPos = insertPos + 1
    
    return insertPos  // all elements before this are < pivot
```

### Template 5: Three Pointers (Dutch National Flag)
```
function threeWayPartition(arr, mid):
    low = 0           // boundary for "less than mid"
    high = length(arr) - 1  // boundary for "greater than mid"
    current = 0
    
    while current <= high:
        if arr[current] < mid:
            swap(arr[low], arr[current])
            low = low + 1
            current = current + 1
        else if arr[current] > mid:
            swap(arr[high], arr[current])
            high = high - 1
            // Don't increment current - need to examine swapped element
        else:
            current = current + 1
```

---

## 7. Variations of the Pattern

### Variation A: Three Sum (Extension to Pairs)
**Problem Type:** Find triplets that sum to target
**Key Insight:** Fix one element, then it becomes Two Sum
**Complexity:** O(n²) instead of O(n³)
```
for i = 0 to n-3:
    target = -arr[i]
    // Two pointer on arr[i+1...n-1]
```

### Variation B: Closest Sum
**Problem Type:** Find pair with sum closest to target
**Key Insight:** Same two pointer approach, but track minimum difference
```
if abs(sum - target) < abs(closest - target):
    closest = sum
```

### Variation C: Squares of Sorted Array
**Problem Type:** Square each element, return sorted
**Key Insight:** Largest squares are at either end (negatives!)
**Approach:** Two pointers from ends, compare absolute values

### Variation D: Palindrome Validation
**Problem Type:** Check if string/array is palindrome
**Key Insight:** Compare from both ends, move inward
```
while left < right:
    if arr[left] != arr[right]:
        return false
    left++, right--
```

### Variation E: Container With Most Water
**Problem Type:** Max area between two lines
**Key Insight:** Start wide, move the shorter line inward
**Why:** Moving shorter line MIGHT find taller, moving taller line can only decrease area

### Variation F: Trapping Rain Water
**Problem Type:** Calculate water trapped between bars
**Key Insight:** Water at position = min(maxLeft, maxRight) - height[i]
**Optimization:** Track maxLeft and maxRight with two pointers

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight |
|---------|-------------|
| Valid Palindrome | Opposite direction, skip non-alphanumeric |
| Merge Sorted Array (LC 88) | Same direction merge |
| Move Zeroes | Same direction, partition zeros to end |
| Remove Duplicates from Sorted Array | Same direction, slow-fast |
| Squares of a Sorted Array (LC 977) | Two pointers from ends, compare abs |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **3Sum** | Fix one + Two Sum | 15 |
| **3Sum Closest** | Fix one + closest pair | 16 |
| **Container With Most Water** | Move shorter line inward | 11 |
| **Sort Colors** | Dutch National Flag (3 pointers) | 75 |
| **Partition Labels** | Two pointers to find segment boundaries | 763 |
| **Boats to Save People** | Greedy pairing lightest + heaviest | 881 |
| **Interval List Intersections** | Two pointers on interval lists | 986 |
| **4Sum** | Fix two + Two Sum | 18 |
| **Longest Mountain in Array** | Expand from peaks | 845 |
| **Two Sum II - Input Array Is Sorted** | Basic opposite direction | 167 |
| **3Sum Smaller** | Count pairs less than target | 259 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Trapping Rain Water** | Two pointers with maxLeft/maxRight | 42 |
| **Minimum Window Substring** | (Overlaps with Sliding Window) | 76 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Foundation
**Day 1:**
- Solve "Two Sum II - Sorted Array"
- Understand WHY moving pointers is correct

**Day 2:**
- Solve "Valid Palindrome" and "Valid Palindrome II"
- Practice the "skip and check" pattern

**Day 3:**
- Solve "Move Zeroes" and "Remove Duplicates"
- Master the slow/fast pointer partition

**Day 4:**
- Solve "3Sum" - this is THE classic
- Focus on duplicate skipping logic

**Day 5:**
- Solve "Container With Most Water"
- Understand the greedy choice of moving shorter line

### Week 2: Advanced
**Day 1-2:**
- Solve "Sort Colors" (Dutch National Flag)
- This is tricky - practice until confident

**Day 3:**
- Solve "Trapping Rain Water"
- Both O(n) space (with arrays) and O(1) space (two pointers)

**Day 4:**
- Solve "Boats to Save People"
- Practice greedy pairing

**Day 5:**
- Review all problems. Can you:
  - Identify which template to use within 1 minute?
  - Explain WHY the pointer movement is correct?

### Self-Check Questions:
- Can I explain why each pointer move is safe?
- Do I handle duplicates correctly in 3Sum?
- Can I identify sorted array + pairs = two pointers?
- Do I know when to use opposite vs same direction?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Using Two Pointers on Unsorted Array
```
// WRONG: Array must be sorted for this to work!
function twoSum(arr, target):
    left = 0, right = n-1
    // If array is unsorted, moving pointers doesn't eliminate possibilities correctly
```

### Mistake 2: Not Handling Duplicates in K-Sum Problems
```
// WRONG: Will produce duplicate triplets
for i = 0 to n-3:
    left = i+1, right = n-1
    while left < right:
        if sum == 0:
            add to result
            left++, right--

// CORRECT: Skip duplicates
for i = 0 to n-3:
    if i > 0 and arr[i] == arr[i-1]:
        continue  // Skip duplicate starting element
    left = i+1, right = n-1
    while left < right:
        if sum == 0:
            add to result
            while left < right and arr[left] == arr[left+1]: left++
            while left < right and arr[right] == arr[right-1]: right--
            left++, right--
```

### Mistake 3: Wrong Pointer Movement in Container With Water
```
// WRONG: Always moving left pointer
left++

// CORRECT: Move the SHORTER line
if height[left] < height[right]:
    left++
else:
    right--
```

### Mistake 4: Incrementing Current in Dutch National Flag When Swapping High
```
// WRONG:
if arr[current] > mid:
    swap(arr[high], arr[current])
    high--
    current++  // BUG! Swapped element not examined

// CORRECT:
if arr[current] > mid:
    swap(arr[high], arr[current])
    high--
    // Don't increment current - examine the swapped element
```

### Mistake 5: Wrong Boundary Conditions
```
// WRONG: Should be left < right for pairs
while left <= right:
    // When left == right, we're comparing element with itself!

// CORRECT:
while left < right:
```

### Mistake 6: Not Handling Empty/Single Element Arrays
```
// WRONG: No check for minimum size
function twoSum(arr, target):
    left = 0, right = n-1
    // If n < 2, this fails

// CORRECT:
function twoSum(arr, target):
    if length(arr) < 2:
        return NOT_FOUND
    left = 0, right = length(arr) - 1
```

### Mistake 7: Confusing Two Pointers with Sliding Window
- **Two Pointers:** Both pointers can move independently based on conditions
- **Sliding Window:** Right always moves forward, left only shrinks

---

## Bonus: Two Pointers vs Sliding Window

| Aspect | Two Pointers | Sliding Window |
|--------|--------------|----------------|
| Movement | Independent (based on condition) | Right always forward, left catches up |
| Direction | Often opposite | Always same direction |
| Array Type | Sorted for opposite-direction; no requirement for same-direction | No ordering requirement |
| Problem Type | Pairs, partitioning | Subarrays, substrings |
| Window Size | N/A | Fixed or variable |

### When They Overlap:
Some problems can be solved by either:
- "Minimum Size Subarray Sum" - Sliding window is more natural
- Finding palindromic substring - Two pointers expansion works well

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Two pointers on "find two numbers that sum to target" | Hashmap (Pattern 05) | Is the array sorted, or may you sort it? If unsorted and the output needs original indices → hashmap (LC 1 vs LC 167) |
| Two pointers on "longest/shortest subarray with property X" | Sliding window (Pattern 01) | Does the answer cover every element between the pointers? If yes → sliding window (LC 209) |
| Same-direction slow/fast on an array | Fast & slow pointers (Pattern 03) | Do the pointers move at different speeds to find a cycle or a middle? If yes → Floyd's algorithm (LC 141, LC 876) |
| Two pointers on "values in [1..n], find missing/duplicate" | Cyclic sort / index-as-hash (Pattern 05) | Are values in [1..n] and is extra space limited to O(1)? If yes → place each value at index value−1 (LC 448) |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                    TWO POINTERS CHEAT SHEET                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  OPPOSITE DIRECTION:                                             │
│  - Array should be SORTED                                        │
│  - Move based on comparison with target                          │
│  - sum < target → left++                                         │
│  - sum > target → right--                                        │
│                                                                  │
│  SAME DIRECTION:                                                 │
│  - Slow = write position                                         │
│  - Fast = read position                                          │
│  - Use for in-place modifications                                │
│                                                                  │
│  K-SUM FORMULA:                                                  │
│  - 3Sum = fix one + 2Sum = O(n²)                                 │
│  - 4Sum = fix one + 3Sum = O(n³)                                 │
│  - kSum = O(n^(k-1))                                             │
│                                                                  │
│  DUPLICATE HANDLING:                                             │
│  - Skip same starting elements                                   │
│  - After finding pair, skip same left/right                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 03: Fast & Slow Pointers + Linked List Techniques →](../03-fast-slow-pointers/README.md)*
