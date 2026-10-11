# Pattern 16: Heap / Priority Queue

## 1. Pattern Name
**Heap / Priority Queue** (Top K, Two-Heap, Merge K Sources, Running Order Statistics)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

The heap pattern is about **efficiently tracking the best (or worst) element among a dynamically changing set** without paying the full cost of re-sorting every time something changes.

**The Key Insight:**  
A heap lets you find the minimum (or maximum) element in O(1) and insert/remove in O(log n). This makes it ideal whenever you need repeated access to "the k-th best" or "the current extremum" while elements are being added or removed.

Think of it like a **tournament bracket that repairs itself** — whenever a new player enters, the bracket updates in O(log n) rather than replaying the entire tournament from scratch.

### The Mental Model:

Heap problems almost always reduce to one of four micro-patterns:

1. **Top K:** Keep the k best elements seen so far, discarding the rest.
2. **Kth Order Statistic:** Find the k-th largest/smallest efficiently without full sort.
3. **Merge K Sorted Sources:** The next globally-smallest element is always the minimum across all current heads.
4. **Running Order Statistics:** Maintain a partition of a stream so you can answer "what is the median right now?" in O(1).

### Why Not Just Sort?

Sorting costs O(n log n) and requires all data upfront. A heap costs O(n log k) for "top k" problems and works on **streaming data** (data arriving one item at a time). When k << n or data is infinite, heap wins decisively.

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Top k", "k largest", "k smallest", "k most frequent"
- "Kth largest element", "Kth smallest element"
- "K closest points", "K nearest"
- "Merge k sorted lists/arrays/files"
- "Median from data stream", "running median"
- "Task scheduler", "CPU scheduling with cooldown"
- "Minimum meeting rooms", "room allocation"
- "Find if any interval overlaps"

### Constraint Hints That Signal Heap:
- n can be very large (10^6+) but k is small → O(n log k) beats O(n log n)
- Data arrives as a stream, one element at a time
- You need repeated retrieval of the min or max of a mutable set
- Multiple sorted sequences that must be combined

### Input/Output Hints:
- Input: unsorted array + integer k
- Input: stream of numbers (no fixed size)
- Input: list of lists/linked lists (all sorted)
- Output: single element (kth), or array of k elements, or per-query answer

### RED FLAGS — NOT Heap (see full section 9):
- Need all elements sorted → use sort directly
- Only need one global maximum/minimum → use a single pass (O(n), no heap needed)
- k equals n → sort beats a heap of size n
- Counting occurrences without ranking → just use a hash map

---

## 4. Brute Force Thought Process

### Top K Elements — Brute Force:
```
Problem: Find the k largest elements in an array.

Brute Force:
  Sort the entire array in descending order.
  Return the first k elements.

Time: O(n log n)
Space: O(1) extra (ignoring output)
```

**The Waste:** We sorted ALL n elements when we only care about k of them.  
If n = 10^6 and k = 10, we did roughly 20 million comparisons to find 10 numbers.

### Kth Largest — Brute Force:
```
Problem: Find the kth largest element.

Brute Force:
  Sort descending.
  Return element at index k-1.

Time: O(n log n)
```

**The Waste:** Same as above — sorting more information than the question asks for.

### Merge K Sorted Lists — Brute Force:
```
Problem: Merge k sorted linked lists into one sorted list.

Brute Force:
  Dump all node values into a single array.
  Sort the array.
  Rebuild a linked list from sorted values.

Time: O(N log N) where N = total number of nodes across all lists
```

**The Waste:** We ignored the fact that each list is already sorted.  
The first element of each list is already guaranteed to be smaller than everything behind it.

### Median from Stream — Brute Force:
```
Problem: Add numbers one at a time; after each addition, find the median.

Brute Force:
  Keep a sorted list.
  After each insertion (binary search + shift): O(n).
  Read median from middle: O(1).

Total for n insertions: O(n²)
```

**The Waste:** We re-sort or shift the entire structure on each insertion.

---

## 5. Optimization Insight (The "Aha" Moment)

### Top K Insight:
**"I only need to evict elements that cannot possibly be in the top k."**

Maintain a **min-heap of size k**. The top of the heap is the smallest among the k candidates we're keeping.

- If a new element is **larger** than the heap top: it belongs in the top k. Push it and pop the current minimum (which is now evicted from contention).
- If a new element is **smaller or equal**: it cannot displace any current candidate. Discard it.

After processing all n elements, the heap holds exactly the k largest.

This costs O(n log k) — the log k factor comes from the heap operations, not the full n log n of sorting.

### Kth Largest Insight:
Same as Top K. Maintain a min-heap of size k. After scanning all n elements, the heap top is the kth largest.

**Why min-heap for "largest"?** Because the heap top is the smallest keeper — the element you'd evict if something better showed up. When the heap is full at size k, `heap.top()` = kth largest.

### Merge K Sorted Lists Insight:
**"The globally next smallest is always one of the k current heads."**

Each list is sorted, so the smallest unprocessed element from list i is always its current head. Maintain a min-heap of (value, list-index) pairs over all current heads. Each extraction gives the globally next smallest in O(log k), and we advance that list's head.

Total cost: O(N log k) where N = total nodes. Since log k << N in practice, this is a major win over brute force.

### Median from Stream Insight:
**"Split the data at the median — left half in a max-heap, right half in a min-heap."**

Keep a **max-heap for the lower half** and a **min-heap for the upper half**. Maintain the invariant:

> `left.size() == right.size()` OR `left.size() == right.size() + 1`

The max-heap top is the largest of the small numbers; the min-heap top is the smallest of the large numbers. The median is either `left.top()` (odd total count) or the average of both tops (even total count).

Each insertion costs O(log n) for two heap operations. Each median query costs O(1).

---

## 6. Generalized Templates

### Template 1: Top K Elements (min-heap of size k)
```
function topKLargest(elements, k):
    create empty min-heap

    for each element in elements:
        push element onto heap

        if heap.size() > k:
            heap.pop()              // evict smallest — it can't be in top k

    // heap now contains exactly the k largest elements
    return all elements in heap
```

**Key decision:** "Top k LARGEST" → min-heap (so the heap top is what you'd evict).  
"Top k SMALLEST" → max-heap (so the heap top is what you'd evict).

**Invariant to maintain:** heap.size() <= k at all times.

### Template 2: Kth Largest Element (LC 215)
```
function kthLargest(arr, k):
    create empty min-heap

    for each element in arr:
        push element onto heap
        if heap.size() > k:
            heap.pop()

    // heap has exactly k elements; top is the kth largest
    return heap.top()
```

**Time:** O(n log k)  
**Space:** O(k)

### Template 3: Kth Largest in a Stream (LC 703)
```
class KthLargest:
    k = given
    min-heap = empty

    function add(val):
        push val onto heap
        while heap.size() > k:
            heap.pop()
        return heap.top()
```

**Invariant:** After each add(), the heap holds exactly min(k, totalSeen) elements, and `heap.top()` is the kth largest seen so far.

### Template 4: K Closest Points to Origin (LC 973)
```
function kClosest(points, k):
    // Use a MAX-heap of (distance, point).
    // We keep the k SMALLEST distances, so we evict the LARGEST distance.
    create empty max-heap keyed by distance

    for each point in points:
        dist = point.x * point.x + point.y * point.y
        push (dist, point) onto heap
        if heap.size() > k:
            heap.pop()     // removes the farthest point seen so far

    return all points in heap
```

**Why max-heap here?** Because we want the k SMALLEST distances, so we evict the LARGEST. The heap top is always "the farthest among our current k candidates."

**Note:** You can also heapify all n points into a min-heap and pop k times. That costs O(n + k log n) time but O(n) space, and it needs all points up front. The size-k max-heap approach costs O(n log k) time and O(k) space, and it works on a stream.

### Template 5: Merge K Sorted Lists (LC 23)
```
function mergeKLists(lists):
    create empty min-heap keyed by node value
    dummyHead = new node
    current = dummyHead

    // Seed heap with the head of each non-empty list
    for each list at index i in lists:
        if lists[i] is not empty:
            push (lists[i].value, i, lists[i]) onto heap

    while heap is not empty:
        (val, listIdx, node) = heap.pop()   // globally smallest unprocessed node
        current.next = node
        current = current.next

        if node.next is not empty:
            push (node.next.value, listIdx, node.next) onto heap

    return dummyHead.next
```

**Why store listIdx?** In case two nodes across different lists have equal values, the list index acts as a tiebreaker, keeping the comparator stable and avoiding errors in languages where heap comparisons must be strict weak orderings.

**Time:** O(N log k) where N = total nodes, k = number of lists.  
**Space:** O(k) for the heap (plus O(N) output).

### Template 6: Median from Data Stream (LC 295)
```
class MedianFinder:
    left  = max-heap    // lower half (smaller values)
    right = min-heap    // upper half (larger values)

    // Invariant: left.size() == right.size()
    //         OR left.size() == right.size() + 1

    function addNum(num):
        // Step 1: Route to correct half
        if left is empty OR num <= left.top():
            push num onto left
        else:
            push num onto right

        // Step 2: Rebalance to maintain size invariant
        if left.size() > right.size() + 1:
            push left.pop() onto right
        else if right.size() > left.size():
            push right.pop() onto left

    function findMedian():
        if left.size() > right.size():
            return left.top()
        else:
            return (left.top() + right.top()) / 2.0
```

**Critical invariant:** left always has >= right's count, and never more than 1 extra.  
After rebalancing: `left.size()` is either `right.size()` or `right.size() + 1`. Never the reverse.

**Why left gets the extra?** Convention. You could flip it — just be consistent.

**Time per operation:** O(log n) for addNum, O(1) for findMedian.  
**Space:** O(n) total.

### Template 7: Task Scheduler (LC 621)
```
function leastInterval(tasks, n):
    // Count frequency of each task type
    freq = frequency map of tasks
    
    // Max-heap of frequencies (we always want to schedule the most frequent task)
    max-heap = all frequency values from freq
    
    // Queue of (frequency, availableAtTime) for tasks in cooldown
    cooldown-queue = empty queue
    
    time = 0

    while max-heap is not empty OR cooldown-queue is not empty:
        time += 1

        if max-heap is not empty:
            count = max-heap.pop() - 1      // execute one instance of the task
            if count > 0:
                cooldown-queue.push((count, time + n))  // available after cooldown

        // Release tasks whose cooldown has expired
        if cooldown-queue is not empty AND cooldown-queue.front().availableAt == time:
            (count, _) = cooldown-queue.pop-front()
            max-heap.push(count)

    return time
```

**Key insight:** Always schedule the most frequent remaining task to minimize idle time. If no task is available (all are in cooldown), the CPU idles (time still increments).

**Alternative formula (no heap):** The minimum intervals needed is:  
`max( len(tasks), (maxFreq - 1) * (n + 1) + countOfTasksWithMaxFreq )`  
The heap approach generalizes better to variants like "return the actual schedule."

### Template 8: Meeting Rooms II (LC 253)
```
function minMeetingRooms(intervals):
    sort intervals by start time

    // Min-heap of end times (one entry per currently active meeting room)
    min-heap = empty

    for each interval (start, end) in sorted intervals:
        if heap is not empty AND heap.top() <= start:
            // Reuse the room that finished earliest
            heap.pop()
        
        // Assign this meeting to a room (new or reused) ending at 'end'
        heap.push(end)

    // Number of rooms in heap = number of concurrent meetings at peak
    return heap.size()
```

**Key insight:** We only need to know WHEN the earliest-finishing room frees up, not which room it is. The heap top always answers "what is the soonest a room becomes available?"

**Time:** O(n log n) — sorting dominates.  
**Space:** O(n) worst case (all meetings overlap).

---

## 7. Variations of the Pattern

### Variation A: Top K Frequent Elements
**Problem:** Given an array, return the k most frequent elements.  
**Approach:** Build a frequency map (hash map), then run Template 1 (min-heap of size k) on (frequency, element) pairs, keyed by frequency.  
**Shortcut alternative:** Bucket sort by frequency — O(n) — when k is not much smaller than n.

### Variation B: Sort Characters by Frequency
**Problem:** Return a string with characters sorted by descending frequency.  
**Approach:** Max-heap of (frequency, char) pairs. Pop all and build result string.  
**Time:** O(n log m) where m = number of distinct chars (at most 26 for lowercase).

### Variation C: Find Median of a Sorted Matrix / Stream with Constraints
**Variant of LC 295** with sliding window constraints (e.g., "median of last k elements").  
**Approach:** Two heaps plus a mechanism to lazily evict expired elements (lazy deletion using a set or counter).

### Variation D: Ugly Numbers / Super Ugly Numbers
**Problem:** Find the nth number whose prime factors are only from a given set.  
**Approach:** Min-heap seeded with 1; on each pop, push multiples by each allowed prime factor. Deduplicate via a visited set.

### Variation E: Reorganize String / Interleave Tasks
**Closely related to Task Scheduler.** The key is always "keep the most frequent element in play, subject to placement rules."  
**Approach:** Max-heap of (frequency, char); at each position, pop the top (or top-two if the previous char was the same).

### Variation F: K-th Smallest in a Sorted Matrix
**Problem:** n×n matrix where each row and column is sorted; find kth smallest.  
**Approach:** Min-heap over the first element of each row. Pop k times, pushing the next element in the same row after each pop.  
**Time:** O(k log n).

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | LC # | Key Insight |
|---------|------|-------------|
| Last Stone Weight | 1046 | Max-heap; simulate smashing the two heaviest |
| Kth Largest Element in a Stream | 703 | Min-heap of size k; top = answer |
| Find K Closest Elements | 658 | Binary search + expand outward; or max-heap of size k |

### MEDIUM (Interview Favorites)
| Problem | LC # | Key Insight |
|---------|------|-------------|
| **Kth Largest Element in an Array** | 215 | Min-heap size k; also learnable via QuickSelect O(n) avg |
| **K Closest Points to Origin** | 973 | Max-heap size k keyed by distance; evict farthest |
| **Top K Frequent Elements** | 347 | Frequency map + min-heap size k (or bucket sort) |
| **Sort Characters by Frequency** | 451 | Max-heap of (freq, char); reconstruct string |
| **Merge K Sorted Lists** | 23 | Min-heap over list heads; O(N log k) |
| **Task Scheduler** | 621 | Max-heap of task frequencies + cooldown queue |
| **Meeting Rooms II** | 253 | Min-heap of end times; size = rooms needed |
| **Reorganize String** | 767 | Max-heap of char frequencies; interleave greedily |
| **Find Median from Data Stream** | 295 | Two-heap split; rebalance on every insert |
| **K Pairs with Smallest Sums** | 373 | Min-heap over initial pairs; advance column on pop |
| **Ugly Number II** | 264 | Min-heap with dedup; multiply by 2, 3, 5 on pop |

### HARD (Differentiators)
| Problem | LC # | Key Insight |
|---------|------|-------------|
| **Sliding Window Median** | 480 | Two heaps + lazy deletion for expired elements |
| **IPO (Maximize Capital)** | 502 | Two heaps: projects sorted by capital (min) + profit (max) |
| **Trapping Rain Water II** | 407 | Min-heap BFS from boundary inward |
| **Find K-th Smallest Pair Distance** | 719 | Binary search + sliding window (heap gives TLE) |
| **Minimum Cost to Hire K Workers** | 857 | Sort by wage/quality ratio + max-heap for quality sum |
| **Employee Free Time** | 759 | Merge all intervals via min-heap over interval heads |

---

## 9. RED FLAGS — When NOT to Use a Heap

### 1. You Only Need One Global Maximum or Minimum
A single linear scan finds the global max/min in O(n) with O(1) space.  
A heap needs O(n) extra space, and pushing items one at a time costs O(n log n). Both are strictly worse.

```
// You want the single maximum:
// BAD: push everything into a max-heap and pop once — O(n log n), O(n) space
// GOOD: scan once and track running max — O(n)
```

### 2. You Need the Entire Array Sorted
If you need every element in order, sort the array. A heap sort is also O(n log n) but has worse cache performance and more constant-factor overhead than TimSort/IntroSort. Just sort.

### 3. k Equals n
When k = n, "top k" is the same as "all elements sorted." A sort is simpler, better cached, and the asymptotic cost is identical.

### 4. You Need Exact Rank Without Repeated Queries
QuickSelect finds the kth element in O(n) average time, O(n²) worst case (randomized pivot → O(n) expected). For a one-shot kth-element query with no stream updates, QuickSelect beats a heap on average.

### 5. The Input Is Already Sorted
If the array is sorted, the kth largest is `arr[n-k]` in O(1). No heap needed.

### 6. You Are Counting or Aggregating, Not Ranking
"Find all elements appearing more than n/3 times" is a counting problem. Use a frequency map or the Boyer-Moore majority vote algorithm — not a heap.

### 7. k Is Large and Close to n
When k is, say, 90% of n, the overhead of maintaining a heap and respecting the size-k invariant is not worth it. A sort + slice is simpler and asymptotically equivalent.

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Using the Wrong Heap Type (Max vs Min)
The most common error. The rule is counterintuitive:

```
// To find the k LARGEST elements → use a MIN-heap of size k.
//   Why: heap.top() = the "weakest" keeper. If a new element beats it, swap.

// To find the k SMALLEST elements → use a MAX-heap of size k.
//   Why: heap.top() = the "weakest" keeper. If a new element beats it, swap.

// Memory aid: the heap type is the OPPOSITE of what you're looking for.
```

Getting this backwards means your heap evicts the elements you want to keep and retains the ones you want to discard.

### Mistake 2: Forgetting to Enforce the Size-k Constraint
Pushing onto the heap but never popping when size exceeds k turns your O(n log k) algorithm into O(n log n).

```
// WRONG: heap grows unbounded
for each element:
    push element onto heap

// CORRECT: pop immediately when overfull
for each element:
    push element onto heap
    if heap.size() > k:
        heap.pop()
```

### Mistake 3: Comparing Custom Objects Without a Proper Comparator
When heap elements are objects (points, intervals, task objects), you must define an ordering. Using the default comparator on a struct or class that does not implement comparison will cause undefined behavior or a runtime error.

```
// WRONG: push Point objects without telling the heap how to compare them
heap.push(Point(3, 4))

// CORRECT: push (distance, point) tuples, OR define a comparator
heap.push( (distance(point), point) )
// or: heap.push(point) with a comparator function: (a, b) -> distance(a) < distance(b)
```

In the Merge K Lists template, storing `(value, listIndex, node)` is critical: if two nodes from different lists have equal values, comparing on `(value, listIndex)` gives a tiebreaker, preventing crashes or incorrect results in strictly-ordered comparators.

### Mistake 4: Wrong Rebalancing in the Two-Heap (Median) Pattern
The rebalancing logic must fire unconditionally after every insert — not just when the heaps are "obviously" imbalanced.

```
// WRONG: only rebalance when size difference exceeds 2
if abs(left.size() - right.size()) > 2:
    rebalance()

// CORRECT: rebalance on every insert so invariant holds after every addNum()
// Invariant: left.size() == right.size() or left.size() == right.size() + 1
```

Also, the routing step (deciding which heap to insert into first) must consider `left.top()`, not just the total count. Inserting naively by count without checking the value can place a large number on the left heap, corrupting the partition.

### Mistake 5: Not Handling an Empty Heap Before Calling top() or pop()
Calling top() or pop() on an empty heap is undefined behavior or throws an exception. This bites you when:
- All k lists in the merge problem are empty.
- The stream has received zero elements and findMedian() is called.
- The cooldown queue is empty and you still try to peek at it.

```
// CORRECT pattern:
if heap is not empty:
    value = heap.top()
else:
    // handle the empty case explicitly
```

### Mistake 6: Using a Heap When QuickSelect Is Faster for a One-Shot Query
If the problem is "find the kth largest element in a static array" (no stream, no repeated queries), QuickSelect achieves O(n) average time. A heap achieves O(n log k). QuickSelect is asymptotically better and has no extra space overhead. Many candidates default to a heap when an interviewer is hinting at an O(n) solution.

### Mistake 7: Forgetting That Heap Order Is Not Full Sort Order
A heap guarantees only that the top element is the min (or max). The remaining elements are NOT in sorted order. Do not try to iterate a heap and assume you'll see elements in sorted sequence.

```
// WRONG: iterating heap elements and expecting sorted output
for element in heap:    // heap storage order is heap-order, NOT sorted order
    print element       // output is NOT guaranteed sorted

// CORRECT: pop() repeatedly to get elements in sorted order
while heap is not empty:
    print heap.pop()
```

---

## Bonus: The Interview Script for Heap Problems

When you recognize a heap problem, here is how to communicate it:

> "This problem asks for the [top k / kth / running median] of a [large dataset / stream], which suggests a heap. A naive sort would be O(n log n), but I only care about k elements, so I can maintain a [min/max]-heap of size k for O(n log k) time and O(k) space.
>
> The key insight is: I use a [min-heap for top-k-largest / max-heap for top-k-smallest] — the heap type is the opposite of what I'm looking for — because the heap top always represents the 'weakest keeper' that I'd evict when something better comes along.
>
> [For two-heap median:] I'll split the data around the median, keeping the lower half in a max-heap and the upper half in a min-heap, maintaining the invariant that left.size() equals right.size() or is exactly one larger.
>
> Let me trace a small example before coding..."

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Top k with a size-k heap | Sort | Is k close to n, or do you need every element in order? Yes → sort, O(n log n). |
| Kth largest with a heap | Quickselect (partition around a pivot, recurse into one side) | Is the array static and queried once? Yes → quickselect, O(n) average. Data arrives as a stream → heap. |
| Top k frequent with a heap | Bucket sort by frequency | Do the values you rank by lie in a small range? Frequencies lie in 1..n, so bucket `i` holds the elements seen `i` times → O(n). |
| Sliding window median with two heaps | Sorted structure (`TreeMap` with counts) | Must elements leave the window? A heap cannot delete an arbitrary element in O(log n). Use lazy deletion, or a `TreeMap`, which deletes in O(log n) directly. |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   HEAP / PRIORITY QUEUE CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  PATTERN RECOGNITION:                                                   │
│  "k + optimal (smallest/largest/closest)"  → heap of size k            │
│  "Running median / running order stats"    → two-heap split             │
│  "Merge k sorted sources"                  → heap over heads            │
│  n ≤ 10^6 with k-related output            → O(n log k) vs O(n log n)  │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  HEAP TYPE RULE (counterintuitive!):                                    │
│  Top k LARGEST  → MIN-heap of size k  (evict smallest)                 │
│  Top k SMALLEST → MAX-heap of size k  (evict largest)                  │
│  K Closest      → MAX-heap of size k  (evict farthest)                 │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  TOP K TEMPLATE:                                                        │
│  for each element:                                                      │
│      push onto heap                                                     │
│      if heap.size() > k: heap.pop()    ← never skip this!              │
│  return heap contents                                                   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  TWO-HEAP MEDIAN INVARIANT:                                             │
│  left (max-heap) = lower half                                           │
│  right (min-heap) = upper half                                          │
│  left.size() == right.size()  OR  left.size() == right.size() + 1      │
│  median = left.top()  OR  avg(left.top(), right.top())                 │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  MERGE K SORTED LISTS:                                                  │
│  Seed heap with (value, listIdx, headNode) for each list.              │
│  Pop min → append to result → push next node from same list.           │
│  Time: O(N log k)  Space: O(k)                                         │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  MEETING ROOMS II:                                                      │
│  Sort by start. Min-heap of end times.                                 │
│  If heap.top() <= start: pop (reuse room). Push end.                   │
│  Answer: heap.size()                                                    │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  COMPLEXITY SUMMARY:                                                    │
│  Build heap from n items:     O(n)                                     │
│  Push / Pop:                  O(log n)                                  │
│  Peek top:                    O(1)                                      │
│  Top k from n items:          O(n log k)                               │
│  Merge k lists (N total nodes): O(N log k)                             │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  RED FLAGS — DO NOT USE HEAP WHEN:                                      │
│  - Need all elements sorted      → just sort                           │
│  - Only need single global max   → one linear scan                     │
│  - k == n                        → sort is cleaner                     │
│  - Static array, one-shot kth    → QuickSelect O(n) avg                │
│  - Input is already sorted       → index directly O(1)                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 17: Trie →](../17-trie/README.md)*
