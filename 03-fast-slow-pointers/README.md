# Pattern 03: Fast & Slow Pointers (Floyd's Cycle Detection)

## 1. Pattern Name
**Fast & Slow Pointers** (also known as Tortoise and Hare Algorithm)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Fast & Slow pointers is about **detecting cycles and finding cycle properties** using two pointers that move at different speeds through a sequence.

**The Key Insight:**  
If there's a cycle, a fast pointer (moving 2 steps) will eventually catch up to a slow pointer (moving 1 step). If there's no cycle, the fast pointer reaches the end first.

**Mental Model:**  
Imagine two runners on a circular track. If the track is circular, the faster runner WILL lap the slower runner eventually. If the track is a straight line, the faster runner just finishes first.

**Why Different Speeds Work:**
- In each iteration, fast gains 1 position on slow
- If cycle has length L, they'll meet within L iterations after slow enters cycle
- This guarantees O(n) time without extra space

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Cycle" or "Loop"
- "Circular"
- "Linked List" + "detect something"
- "Find the duplicate" (in specific conditions)
- "Happy number"
- "Middle of linked list"
- "Starting point of cycle"

### Constraint Hints:
- Must use O(1) space (can't use HashSet to track visited)
- Problem involves linked list structure
- Sequence that can potentially repeat

### Input/Output Hints:
- Input: Linked List, or array where values point to indices
- Output: Boolean (cycle exists), Node (cycle start), Number (middle element)

### RED FLAGS - NOT Fast/Slow Pointers:
- Need to detect cycle in graph (use DFS with visited set instead)
- Multiple possible cycles (this pattern finds one)
- Need to find ALL nodes in cycle simultaneously

---

## 4. Brute Force Thought Process

### Cycle Detection Brute Force:
```
Problem: Detect if linked list has a cycle

Brute Force Thinking:
- Use a HashSet to store visited nodes
- Traverse list, add each node to set
- If we see a node again, cycle exists
- If we reach null, no cycle

Time: O(n)
Space: O(n)  ← This is the problem!
```

### Why Space is a Problem:
- Many interview problems explicitly require O(1) space
- For very large lists, O(n) space might not be available
- Fast & Slow achieves same result with O(1) space

### Finding Middle Brute Force:
```
Problem: Find middle of linked list

Brute Force Thinking:
- Traverse once to count nodes (n)
- Traverse again to position n/2

Time: O(n) + O(n) = O(n)
Space: O(1)
```

Fast & Slow does it in ONE pass.

---

## 5. Optimization Insight (The "Aha" Moment)

### Cycle Detection Insight:
**"Fast closes the gap by 1 each iteration."**

- Slow moves 1 step, Fast moves 2 steps
- Relative speed of fast compared to slow = 1
- If there's a cycle of length L, after slow enters cycle:
  - Fast is at most L steps behind
  - Fast gains 1 step per iteration
  - They meet within L iterations

**Mathematical Proof:**
After slow enters cycle:
- Let slow be at position 0 in cycle
- Let fast be at position k in cycle (where 0 ≤ k < L)
- Distance fast needs to close = L - k
- Fast gains 1 per iteration
- They meet in L - k iterations

### Finding Cycle Start Insight:
**"The distance from head to cycle start equals distance from meeting point to cycle start."**

This is the magical property that makes Floyd's algorithm complete.

Let:
- Distance from head to cycle start = A
- Distance from cycle start to meeting point = B  
- Cycle length = L

When they meet:
- Slow traveled: A + B
- Fast traveled: A + B + n*L (some complete cycles)
- Fast = 2 * Slow → A + B + n*L = 2(A + B)
- Solving: A = n*L - B = (n-1)*L + (L-B)

This means: Starting from head and from meeting point, moving at same speed, they meet at cycle start!

### Middle Element Insight:
**"When fast reaches end, slow is at middle."**

- Fast moves 2x speed of slow
- When fast has traveled n steps, slow has traveled n/2 steps
- Position n/2 is the middle!

---

## 6. Generalized Templates

### Template 1: Cycle Detection
```
function hasCycle(head):
    if head is null:
        return false
    
    slow = head
    fast = head
    
    while fast is not null and fast.next is not null:
        slow = slow.next          // Move 1 step
        fast = fast.next.next     // Move 2 steps
        
        if slow == fast:
            return true           // Cycle detected
    
    return false                  // Fast reached end, no cycle
```

### Template 2: Find Cycle Start
```
function findCycleStart(head):
    if head is null:
        return null
    
    // Phase 1: Detect cycle
    slow = head
    fast = head
    hasCycle = false
    
    while fast is not null and fast.next is not null:
        slow = slow.next
        fast = fast.next.next
        
        if slow == fast:
            hasCycle = true
            break
    
    if not hasCycle:
        return null
    
    // Phase 2: Find cycle start
    // Reset one pointer to head, keep other at meeting point
    slow = head
    while slow != fast:
        slow = slow.next
        fast = fast.next         // Both move at same speed now
    
    return slow                  // They meet at cycle start
```

### Template 3: Find Middle of List
```
function findMiddle(head):
    if head is null:
        return null
    
    slow = head
    fast = head
    
    while fast is not null and fast.next is not null:
        slow = slow.next
        fast = fast.next.next
    
    return slow                  // Slow is at middle
    
// Note: For even length, this returns second middle
// For [1,2,3,4], returns 3
// For [1,2,3,4,5], returns 3
```

### Template 4: Find Length of Cycle
```
function cycleLength(head):
    // First detect cycle and find meeting point
    slow, fast = head, head
    
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
        if slow == fast:
            break
    
    if fast is null or fast.next is null:
        return 0                 // No cycle
    
    // Count cycle length
    length = 1
    current = slow.next
    while current != slow:
        length = length + 1
        current = current.next
    
    return length
```

### Template 5: Array as Linked List (Find Duplicate)
```
// For array where each element is in range [1, n]
// Element value = "next pointer" to that index
function findDuplicate(nums):
    // Treat array as linked list: nums[i] points to index nums[i]
    slow = nums[0]
    fast = nums[0]
    
    // Phase 1: Find intersection
    do:
        slow = nums[slow]
        fast = nums[nums[fast]]
    while slow != fast
    
    // Phase 2: Find entrance to cycle (the duplicate)
    slow = nums[0]
    while slow != fast:
        slow = nums[slow]
        fast = nums[fast]
    
    return slow
```

---

## 7. Variations of the Pattern

### Variation A: Happy Number
**Problem Type:** Determine if repeatedly summing squares of digits reaches 1
**Key Insight:** Either reaches 1 or enters a cycle
**Approach:** Treat sum-of-squares as "next pointer", use cycle detection

```
function isHappy(n):
    slow = n
    fast = n
    
    do:
        slow = sumOfSquares(slow)
        fast = sumOfSquares(sumOfSquares(fast))
    while slow != fast
    
    return slow == 1
```

### Variation B: Palindrome Linked List
**Problem Type:** Check if linked list is palindrome
**Key Insight:** Find middle → Reverse second half → Compare
**Uses Fast/Slow:** To find middle in one pass

### Variation C: Rearrange Linked List
**Problem Type:** Reorder L0→Ln→L1→Ln-1→...
**Uses Fast/Slow:** To find middle, then merge two halves

### Variation D: Find kth from End
**Problem Type:** Find kth last element
**Variation of Fast/Slow:** One pointer starts k steps ahead

```
function kthFromEnd(head, k):
    fast = head
    slow = head
    
    // Move fast k steps ahead
    for i = 0 to k-1:
        fast = fast.next
    
    // Move both until fast reaches end
    while fast is not null:
        slow = slow.next
        fast = fast.next
    
    return slow
```

### Variation E: Intersection of Two Lists
**Problem Type:** Find node where two lists merge
**Key Insight:** Length difference determines offset
**Alternative:** Concatenate lists conceptually (a+b and b+a have same length)

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight |
|---------|-------------|
| Linked List Cycle | Basic cycle detection |
| Middle of Linked List | Fast travels 2x |
| Remove Nth Node From End | Fast starts n steps ahead |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Linked List Cycle II** | Find cycle start (two phases) | 142 |
| **Happy Number** | Cycle detection on digit sums | 202 |
| **Find the Duplicate Number** | Array as linked list | 287 |
| **Palindrome Linked List** | Find middle → Reverse → Compare | 234 |
| **Reorder List** | Find middle → Reverse → Merge | 143 |
| **Intersection of Two Linked Lists** | Two pointer length normalization | 160 |
| **Sort List** | Find middle for merge sort | 148 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **First Missing Positive** | Use indices as "pointers" | 41 |
| **Longest Duplicate Substring** | Binary Search + Rolling Hash | 1044 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Core Concepts
**Day 1:**
- Solve "Linked List Cycle" (detection only)
- Trace through with a drawing to understand why they meet

**Day 2:**
- Solve "Linked List Cycle II" (find start)
- PROVE to yourself why the math works (draw it out!)

**Day 3:**
- Solve "Middle of Linked List"
- Handle both odd and even length cases

**Day 4:**
- Solve "Happy Number"
- Understand the cycle detection in a non-linked-list context

**Day 5:**
- Solve "Find the Duplicate Number"
- This is the key insight: array can be treated as linked list

### Week 2: Applications
**Day 1:**
- Solve "Palindrome Linked List"
- Combine: find middle + reverse + compare

**Day 2:**
- Solve "Reorder List"
- Combine: find middle + reverse + interleave

**Day 3:**
- Solve "Intersection of Two Linked Lists"
- Understand the a+b = b+a length normalization trick

**Day 4:**
- Practice explaining the mathematical proof
- Can you explain WHY cycle start detection works?

**Day 5:**
- Review all problems without looking at solutions

### Self-Check Questions:
- Can I prove why fast and slow meet in a cycle?
- Can I prove why distance from head = distance from meeting point to cycle start?
- Do I handle null checks correctly?
- Can I identify when array-as-linked-list applies?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Wrong Null Checks
```
// WRONG: Will crash if fast.next is null
while fast is not null:
    fast = fast.next.next

// CORRECT: Check both fast AND fast.next
while fast is not null and fast.next is not null:
    fast = fast.next.next
```

### Mistake 2: Two Conventions for Finding Middle — Know Which You Need
```
// Convention A: Both start at head (returns SECOND middle for even-length lists)
slow = head
fast = head
// [1,2,3,4] → slow ends at 3 (second middle). Preferred for merge sort on linked lists.

// Convention B: fast starts one ahead (returns FIRST middle for even-length lists)
slow = head
fast = head.next
// [1,2,3,4] → slow ends at 2 (first middle). Preferred for palindrome check
//   so first half is never longer than second half.
```
Neither convention is universally "correct" — choose based on what the downstream step needs.

### Mistake 3: Wrong Condition for Cycle Detection vs Middle Finding
```
// For cycle detection, we check if they MEET:
if slow == fast:
    return true

// For middle finding, we check if fast REACHES END:
while fast is not null and fast.next is not null:
    // Keep going until fast hits end
```

### Mistake 4: Off-by-One in Finding kth from End
```
// WRONG: Moving fast k times puts it k+1 positions ahead
for i = 0 to k:
    fast = fast.next

// CORRECT: Move exactly k times for k positions ahead
for i = 0 to k-1:
    fast = fast.next
```

### Mistake 5: Not Understanding Array-as-Linked-List
```
// For "Find Duplicate Number" with array [1,3,4,2,2]:
// Array indices: 0, 1, 2, 3, 4
// Array values:  1, 3, 4, 2, 2
// 
// Starting at index 0:
// 0 → nums[0]=1 → nums[1]=3 → nums[3]=2 → nums[2]=4 → nums[4]=2 (CYCLE!)
//
// The duplicate value IS the cycle entrance
```

### Mistake 6: Forgetting to Reset Pointer in Phase 2
```
// WRONG: Both stay at meeting point
while slow != fast:
    slow = slow.next
    fast = fast.next  // They're already equal!

// CORRECT: Reset one to head
slow = head
while slow != fast:
    slow = slow.next
    fast = fast.next
```

### Mistake 7: Infinite Loop with Single Node Self-Cycle
```
// Edge case: Single node pointing to itself
// head → head (cycle of length 1)
// 
// Make sure your while condition handles this:
while fast is not null and fast.next is not null:
    slow = slow.next
    fast = fast.next.next
    if slow == fast:
        return true  // Correctly detects single-node cycle
```

---

## Bonus: Visual Proof of Cycle Start Formula

```
     A            B
[head]----→[cycle start]----→[meeting point]
               ↑                    ↓
               ←───────────────────←
                     (L - B)

When they meet:
- Slow traveled: A + B
- Fast traveled: A + B + k*L (completed k full cycles)
- Fast = 2 * Slow
- A + B + k*L = 2A + 2B
- k*L = A + B
- A = k*L - B = (k-1)*L + (L-B)

This means:
- Distance A (head to cycle start)
- = (k-1) complete cycles + (L-B) remaining distance
- = Distance from meeting point to cycle start!

So if we put one pointer at head and one at meeting point,
moving at same speed, they'll meet at cycle start.
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                 FAST & SLOW POINTERS CHEAT SHEET                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  CYCLE DETECTION:                                                │
│  - slow = slow.next                                              │
│  - fast = fast.next.next                                         │
│  - Meet → cycle exists                                           │
│  - Fast hits null → no cycle                                     │
│                                                                  │
│  FIND CYCLE START (After detection):                             │
│  - Reset slow to head                                            │
│  - Move both at same speed                                       │
│  - They meet at cycle start                                      │
│                                                                  │
│  FIND MIDDLE:                                                    │
│  - When fast reaches end, slow is at middle                      │
│  - Even length → returns second middle                           │
│                                                                  │
│  ARRAY AS LINKED LIST:                                           │
│  - If values are in range [1,n], treat value as "next pointer"   │
│  - Duplicate value = cycle entrance                              │
│                                                                  │
│  NULL CHECKS:                                                    │
│  - Always check: fast != null AND fast.next != null              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 04: Prefix Sum / Difference Array →](../04-prefix-sum/README.md)*
