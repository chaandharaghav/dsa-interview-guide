# Pattern 14: Greedy Patterns

## 1. Pattern Name
**Greedy Algorithms**

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Greedy is about making the **locally optimal choice** at each step, hoping it leads to a globally optimal solution.

**The Key Insight:**  
Greedy works when the problem has **optimal substructure** AND **greedy choice property** - making the best immediate decision doesn't prevent finding the overall best solution.

**Mental Model:**  
Imagine eating at a buffet where you can only pick one item at a time and can't go back. If picking the "best looking" item each time gives you the best meal overall, that's a greedy-friendly situation.

**CRITICAL:** Not all optimization problems are greedy! You must prove (or strongly believe) that local optimality leads to global optimality.

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Maximum/Minimum" with simple constraint
- "Optimal" without complex dependencies
- "Earliest", "Latest"
- "Assign", "Schedule"
- Sorting often helps reveal greedy approach

### When Greedy Works:
- Activity selection (non-overlapping intervals)
- Huffman coding
- Fractional knapsack (but NOT 0/1 knapsack!)
- Minimum coins (with certain denominations)
- Interval scheduling

### RED FLAGS - NOT Greedy:
- "Count all ways" (likely DP or backtracking)
- Complex dependencies between choices
- Need to consider all subsets
- 0/1 Knapsack (DP, not greedy)
- Problem where local best ≠ global best

---

## 4. Common Greedy Patterns

### Pattern A: Interval Scheduling
**Goal:** Select maximum non-overlapping intervals

**Greedy Strategy:** Sort by END time, always pick the earliest ending interval

**Why it works:** Picking earliest end leaves maximum room for future intervals

### Pattern B: Interval Merging
**Goal:** Merge overlapping intervals

**Greedy Strategy:** Sort by START time, merge with previous if overlapping

### Pattern C: Task Scheduling
**Goal:** Minimize total/maximum time

**Greedy Strategy:** Often involves sorting by deadline or duration

### Pattern D: Partition Problems
**Goal:** Divide array optimally

**Greedy Strategy:** Sort and pair/group optimally

### Pattern E: Huffman-style
**Goal:** Build optimal structure

**Greedy Strategy:** Always combine smallest elements first

---

## 5. Algorithm Templates

### Template 1: Maximum Non-Overlapping Intervals
```
function maxNonOverlapping(intervals):
    if intervals is empty:
        return 0
    
    // Sort by end time
    sort(intervals, by end time)
    
    count = 1
    lastEnd = intervals[0].end
    
    for i = 1 to intervals.length - 1:
        if intervals[i].start >= lastEnd:
            // No overlap, include this interval
            count += 1
            lastEnd = intervals[i].end
    
    return count
```

### Template 2: Minimum Intervals to Remove
```
function eraseOverlapIntervals(intervals):
    // Minimum removals = Total - Maximum non-overlapping
    return intervals.length - maxNonOverlapping(intervals)
```

### Template 3: Merge Intervals
```
function mergeIntervals(intervals):
    if intervals is empty:
        return []
    
    // Sort by start time
    sort(intervals, by start time)
    
    result = [intervals[0]]
    
    for i = 1 to intervals.length - 1:
        lastMerged = result.last()
        current = intervals[i]
        
        if current.start <= lastMerged.end:
            // Overlap, merge
            lastMerged.end = max(lastMerged.end, current.end)
        else:
            // No overlap, add new interval
            result.add(current)
    
    return result
```

### Template 4: Minimum Meeting Rooms
```
function minMeetingRooms(intervals):
    if intervals is empty:
        return 0
    
    // Separate start and end times
    starts = [interval.start for interval in intervals]
    ends = [interval.end for interval in intervals]
    
    sort(starts)
    sort(ends)
    
    roomsNeeded = 0
    maxRooms = 0
    
    s = 0, e = 0
    while s < starts.length:
        if starts[s] < ends[e]:
            // Strict < means: if a meeting ends exactly when another starts, they share a room.
            // Use <= here if the problem treats intervals as closed [start, end] and coincident
            // times require separate rooms.
            roomsNeeded += 1
            maxRooms = max(maxRooms, roomsNeeded)
            s += 1
        else:
            // Meeting ends
            roomsNeeded -= 1
            e += 1
    
    return maxRooms
```

### Template 5: Jump Game (Can Reach End?)
```
function canJump(nums):
    maxReach = 0
    
    for i = 0 to nums.length - 1:
        if i > maxReach:
            return false  // Can't reach this position
        
        maxReach = max(maxReach, i + nums[i])
        
        if maxReach >= nums.length - 1:
            return true
    
    return true
```

### Template 6: Jump Game II (Minimum Jumps)
```
function minJumps(nums):
    if nums.length <= 1:
        return 0
    
    jumps = 0
    currentEnd = 0  // End of current jump range
    farthest = 0    // Farthest we can reach
    
    for i = 0 to nums.length - 2:  // Don't need to jump from last position
        farthest = max(farthest, i + nums[i])
        
        if i == currentEnd:
            // Must jump now
            jumps += 1
            currentEnd = farthest
            
            if currentEnd >= nums.length - 1:
                break
    
    return jumps
```

### Template 7: Gas Station
```
function canCompleteCircuit(gas, cost):
    totalGas = 0
    totalCost = 0
    tank = 0
    start = 0
    
    for i = 0 to gas.length - 1:
        totalGas += gas[i]
        totalCost += cost[i]
        tank += gas[i] - cost[i]
        
        if tank < 0:
            // Can't reach i+1 from current start
            start = i + 1
            tank = 0
    
    return totalGas >= totalCost ? start : -1
```

### Template 8: Task Scheduler
```
function leastInterval(tasks, n):
    // Count frequencies
    freq = countFrequencies(tasks)
    maxFreq = max(freq.values())
    maxCount = count of tasks with maxFreq
    
    // Formula: (maxFreq - 1) * (n + 1) + maxCount
    // Explanation: maxFreq-1 full cycles, each of length n+1, plus final partial cycle
    
    minLength = (maxFreq - 1) * (n + 1) + maxCount
    
    // Answer is max of formula and total tasks
    return max(minLength, tasks.length)
```

### Template 9: Boats to Save People
```
function numRescueBoats(people, limit):
    sort(people)
    
    boats = 0
    left = 0
    right = people.length - 1
    
    while left <= right:
        // Always take the heaviest person
        if people[left] + people[right] <= limit:
            // Can take lightest too
            left += 1
        right -= 1  // Heaviest person boards
        boats += 1
    
    return boats
```

### Template 10: Partition Labels
```
function partitionLabels(s):
    // Find last occurrence of each character
    lastIndex = new Map()
    for i = 0 to s.length - 1:
        lastIndex[s[i]] = i
    
    result = []
    start = 0
    end = 0
    
    for i = 0 to s.length - 1:
        end = max(end, lastIndex[s[i]])
        
        if i == end:
            // Current partition complete
            result.add(end - start + 1)
            start = end + 1
    
    return result
```

---

## 6. Proving Greedy Correctness

### The Exchange Argument:
1. Assume there's an optimal solution that differs from greedy
2. Show you can "exchange" to make it more like greedy without worsening it
3. Conclude greedy is also optimal

### Example: Activity Selection
- Optimal solution O includes activity A
- Greedy picks activity B with earlier end time
- Replace A with B in O → still valid, still optimal
- Therefore, greedy choice is safe

---

## 7. Classic Interview Problems

### MEDIUM
| Problem | Greedy Strategy | LC # |
|---------|-----------------|------|
| **Non-overlapping Intervals** | Sort by end, maximize kept | 435 |
| **Merge Intervals** | Sort by start, merge overlapping | 56 |
| **Meeting Rooms II** | Sweep line or sort start/end | 253 |
| **Jump Game** | Track max reachable | 55 |
| **Jump Game II** | Track farthest in current range | 45 |
| **Gas Station** | Single valid start exists | 134 |
| **Task Scheduler** | Fill with most frequent first | 621 |
| **Boats to Save People** | Two pointers, pair heavy+light | 881 |
| **Partition Labels** | Extend partition to include all occurrences | 763 |
| **Queue Reconstruction by Height** | Sort and insert by position | 406 |
| **Candy** | Two passes, left-to-right and right-to-left | 135 |
| **Assign Cookies** | Sort both, match greedily | 455 |
| **Lemonade Change** | Prefer keeping $5 bills | 860 |
| **Minimum Number of Arrows** | Same as non-overlapping intervals | 452 |

### HARD
| Problem | Greedy Strategy | LC # |
|---------|-----------------|------|
| **Course Schedule III** | Sort by deadline, use max-heap | 630 |
| **IPO** | Two heaps, pick affordable with max profit | 502 |
| **Minimum Cost to Hire K Workers** | Ratio-based greedy + heap | 857 |
| **Create Maximum Number** | Merge two greedy selections | 321 |
| **Patching Array** | Greedily extend coverage | 330 |

---

## 8. Common Mistakes

### Mistake 1: Assuming Greedy Works Without Proof
```
// WRONG for 0/1 Knapsack: Greedy by value/weight ratio
// Doesn't give optimal solution!

// Greedy: Pick item with best ratio until full
// But sometimes combinations of lower-ratio items are better
```

### Mistake 2: Wrong Sorting Criterion
```
// WRONG for activity selection: Sort by start time
sort(intervals, by start time)
// Picking earliest starting doesn't maximize count!

// CORRECT: Sort by end time
sort(intervals, by end time)
```

### Mistake 3: Off-by-One in Jump Game
```
// WRONG: Iterating to last index
for i = 0 to nums.length - 1:
    if i == currentEnd:
        jumps++  // Unnecessary jump from last position

// CORRECT: Stop before last index
for i = 0 to nums.length - 2:
    // ...
```

### Mistake 4: Not Handling Edge Cases
```
// WRONG for merge intervals: Empty input
if intervals.length == 0:
    // Missing check leads to crash

// CORRECT: Check early
if intervals is empty:
    return []
```

### Mistake 5: Confusing Interval Conditions
```
// Overlapping intervals:
// [1,3] and [2,4] overlap because 2 < 3

// WRONG:
if current.start < last.end:  // What about touching? [1,2] and [2,3]

// CORRECT (depends on problem):
// If touching means overlap: current.start <= last.end
// If touching is separate: current.start < last.end
```

---

## 9. How to Practice

### Week 1: Interval Problems
- Merge Intervals
- Non-overlapping Intervals
- Meeting Rooms I & II
- Minimum Arrows to Burst Balloons

### Week 2: Array Greedy
- Jump Game I & II
- Gas Station
- Candy
- Task Scheduler
- Boats to Save People

### Skill to Develop:
1. Identify if greedy might work
2. Figure out the sorting/selection criterion
3. Prove or convince yourself it's correct
4. Handle edge cases

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                     GREEDY CHEAT SHEET                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  KEY QUESTION: Does local optimal lead to global optimal?        │
│                                                                  │
│  INTERVAL PROBLEMS:                                              │
│  - Max non-overlapping: Sort by END time                         │
│  - Merge intervals: Sort by START time                           │
│  - Meeting rooms: Sweep line or two sorted arrays                │
│                                                                  │
│  COMMON STRATEGIES:                                              │
│  - Sort then process greedily                                    │
│  - Two pointers from ends                                        │
│  - Track running max/min                                         │
│  - Use heap for dynamic selection                                │
│                                                                  │
│  GREEDY DOES NOT WORK FOR:                                       │
│  - 0/1 Knapsack (use DP)                                         │
│  - Longest Common Subsequence (use DP)                           │
│  - Problems requiring all combinations                           │
│                                                                  │
│  GREEDY DOES WORK FOR:                                           │
│  - Activity selection                                            │
│  - Fractional knapsack                                           │
│  - Huffman coding                                                │
│  - Job scheduling with deadlines                                 │
│  - Minimum spanning tree (Kruskal/Prim)                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 15: Dynamic Programming →](../15-dynamic-programming/README.md)*
