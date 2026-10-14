# Pattern 20: Intervals & Sweep Line

## 1. Pattern Name
**Intervals & Sweep Line** (Merge, Greedy Selection, Event Sweep, Heap of End Times, Difference Array)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

An **interval** is a pair `[start, end]` that describes a range of time or positions.
Interval problems ask how a set of intervals relate to each other: which ones overlap, how many overlap at once, and which ones you can keep without overlap.

**The Key Insight:**
Unsorted intervals force you to compare every pair, which costs O(n²).
After you sort them, each interval only needs to be compared with its neighbor or with a small summary of the past (the last end, a heap of ends, or a running count).
Sorting costs O(n log n), and the scan after it costs O(n) or O(n log n).

**The second insight:** the sort key depends on the question.
- Sort by **start** when you build the union of intervals (merge, insert, free time).
- Sort by **end** when you choose a maximum set of non-overlapping intervals.
- Sort **events** (each start and each end as a separate point) when you count how many intervals are active at once.

A **sweep line** is a scan over sorted event points from left to right.
At each point, you update a running state (a counter, a heap, or a set) instead of looking at all intervals again.

### Two Interval Conventions:

1. **Half-open** `[start, end)`: the interval contains `start` but not `end`. Meetings use this convention. A meeting `[1, 3)` and a meeting `[3, 5)` can share one room.
2. **Closed** `[start, end]`: the interval contains both endpoints. Balloons (LC 452) and interval intersections (LC 986) use this convention. `[1, 3]` and `[3, 5]` share the point 3.

Read the problem statement to find which convention it uses. The convention decides every `<` versus `<=` in your code.

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Intervals", "ranges", "segments", "time slots"
- "Meetings", "bookings", "reservations", "calendar", "schedule"
- "Overlap", "overlapping", "conflict", "intersect"
- "Merge", "insert", "union", "free time", "gaps"
- "Minimum number of rooms / platforms / arrows / resources"
- "Maximum number of events you can attend"
- "Passengers picked up at X and dropped at Y", "capacity at any time"

### Constraint Hints:
- n up to 10^4 or 10^5: O(n²) pairwise comparison is too slow, O(n log n) after sorting fits.
- Coordinates bounded by a small value (for example, ≤ 1000 or ≤ 10^6): a difference array over coordinates is possible.
- Coordinates up to 10^9: a difference array is too large. Sort events or use a TreeMap instead.
- Bookings arrive one at a time and each needs an answer before the next: use a TreeMap (an online problem).

### Input/Output Hints:
- Input: a list of pairs `[start, end]`, or triples like `[passengers, from, to]`.
- Output: a merged list, a count (rooms, removals, arrows), a yes/no per booking, or a list of gaps.

### The Overlap Test:
Two intervals do **not** overlap when one ends before the other starts.
Negate that statement to get the overlap test.

```
Half-open [s, e):   overlap(a, b) = a.start <  b.end  AND  b.start <  a.end
Closed    [s, e]:   overlap(a, b) = a.start <= b.end  AND  b.start <= a.end
```

If the intervals are already sorted by start (so `a.start <= b.start`), the test reduces to one comparison:
`b.start < a.end` (half-open) or `b.start <= a.end` (closed).

### Which comparison each classic problem uses:

| Problem | Touching endpoints `[1,4]` and `[4,5]` | Comparison in code |
|---------|----------------------------------------|--------------------|
| 56 Merge Intervals | Merge them | `cur.start <= last.end` |
| 57 Insert Interval | Merge them | `iv.start <= new.end` |
| 435 Non-overlapping Intervals | Do not overlap | keep if `iv.start >= lastEnd` |
| 646 Maximum Length of Pair Chain | Do not chain (needs `b < c`) | keep if `iv.start > lastEnd` |
| 452 Min Arrows (closed) | Overlap, one arrow bursts both | new arrow only if `iv.start > arrowPos` |
| 986 Interval List Intersections (closed) | Intersect at point 4 | keep if `lo <= hi` |
| 252 / 253 Meeting Rooms (half-open) | Do not conflict | reuse room if `minEnd <= start` |
| 729 My Calendar I (half-open) | Do not conflict | conflict if `prev.end > start` |
| 1094 Car Pooling | Drop-off happens before pick-up | `diff[to] -= p` |

### RED FLAGS - NOT This Pattern:
- The "intervals" are subarrays of one array and you need a sum or a count of distinct values inside a window. Use Sliding Window (01) or Prefix Sum (04).
- Each interval has a **weight** and you must maximize total weight of non-overlapping intervals (LC 1235). Greedy by end fails. Use DP with binary search (15 + 06).
- You need the k-th smallest or largest value, not overlap structure. Use Heap (16).
- Ranges are ranges of values in a sorted array ("find the range of target"). Use Binary Search (06).

---

## 4. Brute Force Thought Process

### Merge Intervals Brute Force:
```
Problem: Merge all overlapping intervals.

Brute Force Thinking:
- Repeat until nothing changes:
  - For every pair (i, j):
    - If they overlap, replace both with their union.

Time: O(n²) per pass, up to n passes → O(n³) worst case
Space: O(n)
```

### Meeting Rooms II Brute Force:
```
Problem: Minimum number of rooms so no two meetings in one room overlap.

Brute Force Thinking:
- The answer is the maximum number of meetings active at one time.
- For each meeting start time t:
  - Count meetings with start <= t < end.
- Return the largest count.

Time: O(n²)
Space: O(1)
```

### Why It Times Out:
With n = 10^5, O(n²) is 10^10 operations.

**The Waste:** Without sorting, an interval can overlap any other interval, so you must check all pairs.
After sorting by start, an interval can only overlap the intervals right before it in the order.
You only need the **largest end seen so far**, not the full list.

---

## 5. Optimization Insight

### Insight 1: Sort by start, keep one "current" interval (Merge)
After sorting by start, the next interval either overlaps the current merged interval or starts after it ends.
If it starts after the current end, no later interval can overlap the current one either, because later intervals start even further right.
So you finalize the current interval and start a new one.
Time: O(n log n) for the sort plus O(n) for the scan.

### Insight 2: Sort by end, pick the earliest end (Activity Selection)
**Activity selection** is the problem of choosing the maximum number of non-overlapping intervals.
The greedy rule is: always pick the interval that ends first, then skip all intervals that overlap it.

**Why end is the right key (exchange argument):**
1. Let `x` be the interval with the smallest end.
2. Take any optimal solution. Let `y` be its interval with the smallest end.
3. `x.end <= y.end`, so replacing `y` with `x` cannot create a conflict with the other chosen intervals. They all start at or after `y.end`.
4. The new solution has the same size, so it is also optimal, and it contains `x`.
5. Repeat the argument on the intervals that start at or after `x.end`.

**Why other keys fail:**
- Sort by start: `[1,100], [2,3], [4,5]`. Picking `[1,100]` first blocks both others. Optimal is 2.
- Sort by length: `[1,5], [4,7], [6,10]`. The shortest is `[4,7]` (length 3), and it overlaps both others. Optimal is 2: `[1,5]` and `[6,10]`.

LC 435 (minimum removals) is `n - maxNonOverlapping`. LC 452 (minimum arrows) is the same greedy on closed intervals: each arrow sits at the end of the earliest-ending balloon that is not burst yet.

### Insight 3: Count active intervals with events (Sweep Line)
The number of rooms equals the maximum number of meetings active at one time.
Split each meeting into two events: `(start, +1)` and `(end, -1)`.
Sort events by time and keep a running sum. The largest running sum is the answer.

**The tie rule:** when a start and an end have the same time, the sort order decides the answer.
- Half-open intervals (meetings): process the **end (-1) before the start (+1)**. A meeting ending at 10 frees its room for a meeting starting at 10.
- Closed intervals (LC 2406): process the **start (+1) before the end (-1)**. Two intervals that share a point are both active at that point.

### Insight 4: Keep a min-heap of end times (Meeting Rooms II, alternative)
Sort meetings by start. Keep a **min-heap** (a priority queue that returns the smallest element first) of end times of rooms in use.
For each meeting, if the smallest end is `<= start`, that room is free: remove it from the heap.
Push the meeting's end. The heap size never decreases, because each step removes at most one and adds one, so the final heap size is the maximum.
The heap also tells you **which** room frees first. You need that information in LC 2402.

### Insight 5: Difference array for bounded coordinates
When coordinates are small integers, add `+value` at `start` and `-value` at `end` in an array.
A prefix sum over the array gives the load at each coordinate.
Time: O(n + C), where C is the coordinate range. No sorting is needed.

### Decision Table: Which Tool Answers Which Question

| Tool | Question it answers | Typical problems | Time |
|------|---------------------|------------------|------|
| **Sort by start + merge** | "What is the union of these intervals?" "Where are the gaps?" | 56, 57, 759, 1288 | O(n log n) |
| **Sort by end + greedy** | "What is the maximum number of intervals with no overlap?" "Minimum removals?" "Minimum points that hit every interval?" | 435, 452, 646 | O(n log n) |
| **Events sweep (+1 / -1)** | "What is the maximum number of intervals active at one time?" | 253, 2406, 1094 (large coords) | O(n log n) |
| **Min-heap of end times** | "Which resource frees first?" "Assign each interval to a resource." | 253, 2402, 1851 | O(n log n) |
| **Difference array** | "What is the load at every coordinate?" (coordinates small, all updates known first) | 1094, 1109, 1854 | O(n + C) |
| **TreeMap floor / ceiling** | "Does this new interval conflict with the ones booked so far?" (online) | 729, 731, 732, 715 | O(log n) per insert for 729 |
| **Two pointers on two sorted lists** | "What do these two lists of intervals have in common?" | 986 | O(m + n) |

---

## 6. Generalized Templates

Pseudocode below uses half-open intervals unless a template says otherwise.

### Template 1: Merge Intervals (sort by start) — LC 56
```
function merge(intervals):
    sort intervals by start
    result = []

    for iv in intervals:
        if result is empty OR result.last.end < iv.start:
            result.append(copy of iv)              // gap → new interval
        else:
            result.last.end = max(result.last.end, iv.end)   // overlap → extend

    return result
```
Use `max` when you extend. An interval can sit fully inside the current one: `[1,10]` then `[2,3]`.
LC 56 merges touching intervals, so the "new interval" test is strict: `last.end < iv.start`.

### Template 2: Insert Interval (input already sorted, no sort needed) — LC 57
```
function insert(intervals, newIv):
    result = []
    i = 0
    n = length(intervals)

    // Phase 1: intervals that end before newIv starts
    while i < n AND intervals[i].end < newIv.start:
        result.append(intervals[i]); i += 1

    // Phase 2: intervals that overlap newIv → grow newIv
    while i < n AND intervals[i].start <= newIv.end:
        newIv.start = min(newIv.start, intervals[i].start)
        newIv.end   = max(newIv.end,   intervals[i].end)
        i += 1
    result.append(newIv)

    // Phase 3: intervals that start after newIv ends
    while i < n:
        result.append(intervals[i]); i += 1

    return result
```
Time: O(n). The input is already sorted and non-overlapping, so a sort would waste O(n log n).

### Template 3: Maximum Non-Overlapping (sort by end) — LC 435, 452
```
function maxNonOverlapping(intervals):
    sort intervals by end
    count = 0
    lastEnd = -INFINITY

    for iv in intervals:
        if iv.start >= lastEnd:          // half-open: touching is allowed
            count += 1
            lastEnd = iv.end

    return count

// LC 435: return n - maxNonOverlapping(intervals)

// LC 452 (closed balloons): one arrow per kept interval
function minArrows(points):
    sort points by end
    arrows = 0
    arrowPos = -INFINITY                 // in Java: use a flag or long, values reach Integer.MIN_VALUE
    for p in points:
        if arrows == 0 OR p.start > arrowPos:   // closed: touching balloons share an arrow
            arrows += 1
            arrowPos = p.end
    return arrows
```

### Template 4: Meeting Rooms I (any overlap?) — LC 252
```
function canAttendAll(meetings):
    sort meetings by start
    for i = 1 to n-1:
        if meetings[i].start < meetings[i-1].end:
            return false
    return true
```

### Template 5a: Meeting Rooms II with a min-heap — LC 253
```
function minRooms(meetings):
    sort meetings by start
    heap = empty min-heap of end times

    for m in meetings:
        if heap not empty AND heap.peek() <= m.start:
            heap.pop()                   // that room is free now
        heap.push(m.end)

    return heap.size()
```

### Template 5b: Meeting Rooms II with a sweep line — LC 253
```
function minRooms(meetings):
    events = []
    for m in meetings:
        events.append((m.start, +1))
        events.append((m.end,   -1))

    // Sort by time. On equal time, -1 before +1 (half-open: end frees the room first).
    // For closed intervals (LC 2406), put +1 before -1 instead.
    sort events by (time, delta)         // -1 < +1, so ends come first

    active = 0
    best = 0
    for (time, delta) in events:
        active += delta
        best = max(best, active)

    return best
```
An equivalent form: sort all starts into one array and all ends into another, then walk both with two pointers. If `starts[i] < ends[j]`, the meeting needs a new room. Otherwise it reuses the room that ended at `ends[j]`, and `j` moves forward.

### Template 6: Interval List Intersections (two pointers, closed) — LC 986
```
function intersect(A, B):
    i = 0, j = 0
    result = []
    while i < len(A) AND j < len(B):
        lo = max(A[i].start, B[j].start)
        hi = min(A[i].end,   B[j].end)
        if lo <= hi:                     // closed: a single point counts
            result.append([lo, hi])
        // Move past the interval that ends first. It cannot meet anything later.
        if A[i].end < B[j].end: i += 1
        else:                   j += 1
    return result
```
Time: O(m + n).

### Template 7: Employee Free Time — LC 759
```
function freeTime(schedules):
    all = flatten every employee's intervals into one list
    sort all by start                    // or k-way merge with a heap: O(N log k)
    result = []
    prevEnd = all[0].end
    for iv in all (from the second one):
        if iv.start > prevEnd:           // gap of positive length
            result.append([prevEnd, iv.start])
        prevEnd = max(prevEnd, iv.end)
    return result
```
This is merge (Template 1), but you output the gaps instead of the merged intervals.
Time: O(N log N) for N total intervals.

### Template 8: My Calendar I with a TreeMap — LC 729
```
bookings = TreeMap<start, end>

function book(start, end):              // half-open [start, end)
    prev = bookings.floorEntry(start)   // largest start <= start
    next = bookings.ceilingEntry(start) // smallest start >= start
    if prev != null AND prev.end > start: return false
    if next != null AND next.start < end: return false
    bookings.put(start, end)
    return true
```
Time: O(log n) per booking. Existing bookings never overlap each other, so only the two neighbors can conflict.

### Template 9: My Calendar II with a TreeMap of counts — LC 731
```
delta = TreeMap<time, count>

function book(start, end):
    delta[start] += 1
    delta[end]   -= 1
    active = 0
    for (time, d) in delta in sorted order:   // sweep line over stored events
        active += d
        if active >= 3:
            delta[start] -= 1                 // undo this booking
            delta[end]   += 1
            return false
    return true
```
Time: O(n) per booking, O(n²) total. LC 731 allows at most 1000 calls, so this fits.
The TreeMap keeps events sorted, so the sweep needs no extra sort.
The same code with `max(active)` returned answers LC 732 (My Calendar III).

### Template 10: Difference Array — LC 1094 Car Pooling
```
function carPooling(trips, capacity):
    diff = array of size (maxLocation + 1), all 0
    for (passengers, from, to) in trips:
        diff[from] += passengers
        diff[to]   -= passengers         // passengers leave at 'to' before new ones board

    load = 0
    for x = 0 to maxLocation:
        load += diff[x]
        if load > capacity: return false
    return true
```
Time: O(n + C) with C = 1001 for LC 1094.

### Java: Merge Intervals (LC 56)
```java
static int[][] merge(int[][] intervals) {
    // Integer.compare, never a[0] - b[0]: subtraction overflows for values near Integer.MIN_VALUE / MAX_VALUE.
    Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
    List<int[]> merged = new ArrayList<>();
    for (int[] iv : intervals) {
        if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < iv[0]) {
            merged.add(new int[]{iv[0], iv[1]});
        } else {
            int[] last = merged.get(merged.size() - 1);
            last[1] = Math.max(last[1], iv[1]);
        }
    }
    return merged.toArray(new int[0][]);
}
```

### Java: Meeting Rooms II with a min-heap (LC 253)
```java
static int minMeetingRooms(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
    PriorityQueue<Integer> ends = new PriorityQueue<>();   // min-heap of end times
    for (int[] iv : intervals) {
        if (!ends.isEmpty() && ends.peek() <= iv[0]) {
            ends.poll();                                    // reuse the room that frees first
        }
        ends.offer(iv[1]);
    }
    return ends.size();
}
```

**Why `Integer.compare` and not `a - b`:** in LC 452 the coordinates range over the full `int` range, from `-2^31` to `2^31 - 1`.
For `a = 2^31 - 1` and `b = -1`, `a - b` overflows to a negative number, and the sort order becomes wrong.
`Integer.compare(a, b)` never overflows.

---

## 7. Variations of the Pattern

### Variation A: Merge and Union
**Problem Type:** Merge intervals, insert interval, remove covered intervals, total covered length.
**Sort key:** start.
**State:** the current merged interval (or the largest end so far).

### Variation B: Maximum Non-Overlapping Selection
**Problem Type:** Minimum removals (435), minimum arrows (452), longest pair chain (646).
**Sort key:** end.
**State:** the end of the last chosen interval.

### Variation C: Maximum Concurrency
**Problem Type:** Meeting rooms (253), minimum groups (2406), platforms needed at a station.
**Tool:** event sweep with the tie rule, or a min-heap of ends.
**Watch for:** half-open versus closed decides the tie order.

### Variation D: Resource Assignment
**Problem Type:** Which room hosts the most meetings (2402), smallest interval that contains each query (1851).
**Tool:** heaps. You need to know which resource frees first, not only how many are busy.
**Watch for:** LC 2402 delays meetings, so end times grow past the input range. Store them as `long`.

### Variation E: Online Booking
**Problem Type:** My Calendar I/II/III (729, 731, 732), Range Module (715), Data Stream as Disjoint Intervals (352).
**Tool:** TreeMap. `floorEntry` and `ceilingEntry` find the neighbors of a new interval in O(log n).

### Variation F: Load at Each Point
**Problem Type:** Car pooling (1094), corporate flight bookings (1109), maximum population year (1854).
**Tool:** difference array when coordinates are small; sorted events when they are large.

### Variation G: Two Sorted Lists
**Problem Type:** Interval list intersections (986), meeting scheduler (1229).
**Tool:** two pointers. Advance the pointer whose interval ends first.

### Variation H: Sweep Line with a Height Structure
**Problem Type:** The Skyline Problem (218).
**Tool:** sort building edges as events. Keep the active heights in a TreeMap of height → count, or in a max-heap with **lazy deletion** (remove a top element only when its building has ended).
**Watch for:** `PriorityQueue.remove(Object)` costs O(n). Calling it per event makes the solution O(n²). Lazy deletion or a TreeMap keeps it O(n log n).

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Meeting Rooms** | Sort by start, check neighbors | 252 |
| **Summary Ranges** | Build ranges from a sorted array in one scan | 228 |
| **Teemo Attacking** | Add `min(duration, next - cur)` per attack | 495 |
| **Maximum Population Year** | Difference array over years | 1854 |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Merge Intervals** | Sort by start, extend with max | 56 |
| **Insert Interval** | Three phases, no sort needed | 57 |
| **Non-overlapping Intervals** | Sort by end, n - kept | 435 |
| **Minimum Number of Arrows to Burst Balloons** | Sort by end, closed intervals | 452 |
| **Meeting Rooms II** | Min-heap of ends, or sweep with ends first on ties | 253 |
| **Interval List Intersections** | Two pointers, advance the earlier end | 986 |
| **My Calendar I** | TreeMap floor / ceiling neighbors | 729 |
| **My Calendar II** | TreeMap of +1/-1 counts, undo on triple | 731 |
| **Car Pooling** | Difference array over locations | 1094 |
| **Corporate Flight Bookings** | Difference array, then prefix sum | 1109 |
| **Remove Covered Intervals** | Sort by start asc, end desc | 1288 |
| **Maximum Length of Pair Chain** | Activity selection, sort by end | 646 |
| **Divide Intervals Into Minimum Number of Groups** | Meeting rooms on closed intervals: starts before ends on ties | 2406 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **The Skyline Problem** | Sweep edges + TreeMap of heights (or heap with lazy deletion) | 218 |
| **Employee Free Time** | Merge all schedules, output gaps | 759 |
| **Meeting Rooms III** | Heap of free room indices + heap of (end, room); `long` end times | 2402 |
| **My Calendar III** | Sweep a TreeMap of counts, return max active | 732 |
| **Minimum Interval to Include Each Query** | Sort queries and intervals, min-heap by size | 1851 |
| **Data Stream as Disjoint Intervals** | TreeMap, merge with neighbors on insert | 352 |
| **Range Module** | TreeMap of disjoint intervals, split and merge | 715 |
| **Maximum Profit in Job Scheduling** | Weighted intervals: DP + binary search, not greedy | 1235 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Sorting Keys
**Day 1-2:**
- Solve 252, 56, 57.
- Focus on: the overlap test and the `<` versus `<=` choice for each problem.

**Day 3-4:**
- Solve 435, 452, 646.
- Focus on: why the sort key is end. Write the exchange argument in your own words.

**Day 5:**
- Solve 986 and 1288.
- Review the decision table in section 5.

### Week 2: Counting and Assigning
**Day 1-2:**
- Solve 253 twice: once with a heap, once with a sweep line.
- Solve 2406 and note the reversed tie rule for closed intervals.

**Day 3:**
- Solve 1094 and 1109 with a difference array.
- Then solve 1094 again with sorted events, as if coordinates were up to 10^9.

**Day 4:**
- Solve 729 and 731 with a TreeMap.

**Day 5:**
- Solve 759 and 2402. Attempt 218.

### Practice Methodology:
1. **First 2 minutes:** Is each interval half-open or closed? Write it down.
2. **Next 3 minutes:** Which question from the decision table does the problem ask? Pick the tool.
3. **Next 10 minutes:** Write pseudocode with the template.
4. **Remaining time:** Implement and test with touching intervals and nested intervals.

### Self-Check Questions:
- Can I state the overlap test for half-open and closed intervals without looking?
- Can I explain why sorting by end is correct for 435 and why sorting by start is not?
- Do I know the tie rule for the sweep line, and why it flips for closed intervals?
- Do I handle an empty input and a single interval?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Comparator Overflow
<!-- fragment -->
```java
// WRONG: overflows when values span the int range (LC 452)
Arrays.sort(points, (a, b) -> a[1] - b[1]);

// CORRECT:
Arrays.sort(points, (a, b) -> Integer.compare(a[1], b[1]));
```

### Mistake 2: Forgetting `max` When Extending a Merged Interval
```
// WRONG: [1,10] then [2,3] becomes [1,3]
last.end = iv.end

// CORRECT:
last.end = max(last.end, iv.end)
```

### Mistake 3: Wrong Tie Order in the Sweep Line
```
// Meetings [1,3) and [3,5) need 1 room.
// WRONG: start before end at time 3 → active reaches 2
// CORRECT for half-open: end (-1) before start (+1) at equal time
// CORRECT for closed intervals: start (+1) before end (-1) at equal time
```

### Mistake 4: Sorting by Start for Maximum Non-Overlapping
```
// WRONG: [1,100], [2,3], [4,5] sorted by start keeps only [1,100]
// CORRECT: sort by end → keeps [2,3] and [4,5]
```

### Mistake 5: Mixing Up `<` and `<=`
```
// LC 56 merges [1,4] and [4,5]. LC 435 does not count them as overlapping.
// Decide the convention from the problem statement before you write a comparison.
```

### Mistake 6: Advancing the Wrong Pointer in Intersections
```
// WRONG: always advance i, or advance the one with the smaller start
// CORRECT: advance the interval with the smaller END. The other one may still meet the next interval.
```

### Mistake 7: Sorting Input That Is Already Sorted
```
// LC 57 gives sorted, non-overlapping intervals.
// Sorting again costs O(n log n). The three-phase scan is O(n).
```

### Mistake 8: Using `int` for Accumulated Times
```
// LC 2402: a delayed meeting ends at (room free time + duration).
// Delays add up, and the end time can exceed the int range. Use long.
```

---

## Bonus: Interview Script for Interval Problems

> "The input is a list of intervals, and the question is about overlap. First I will check the convention. Here a meeting ending at 10 and one starting at 10 do not conflict, so I will treat intervals as half-open.
>
> Comparing every pair is O(n²). Sorting lets me compare each interval with a small summary of the past instead.
>
> The question is [union / maximum non-overlapping / maximum active at one time], so I will sort by [start / end / event time]. The state I keep is [the current merged interval / the last chosen end / a running count or a heap of end times].
>
> This gives O(n log n) for the sort and O(n) or O(n log n) for the scan. Let me trace it on two touching intervals and one nested interval before I code it."

---

## Commonly Confused With

- **Greedy (Pattern 14):** activity selection (435, 452) is a greedy problem. Pattern 14 covers the general exchange argument. This pattern covers the interval-specific sort keys.
- **Heap (Pattern 16):** a heap appears in 253 and 2402, but the core step is sorting by start. Use Pattern 16 when the question is "k-th" or "top k" with no overlap structure.
- **Prefix Sum / Difference Array (Pattern 04):** car pooling (1094) is a difference array. Choose it over an event sweep only when coordinates are small.
- **Weighted intervals:** 1235 looks like 435 but has profits. Greedy fails. Use DP with binary search.

For a side-by-side guide across all patterns, see [How to Choose a Pattern](../how-to-choose-a-pattern.md).

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│               INTERVALS & SWEEP LINE CHEAT SHEET                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  OVERLAP TEST:                                                   │
│  - Half-open: a.start <  b.end && b.start <  a.end               │
│  - Closed:    a.start <= b.end && b.start <= a.end               │
│                                                                  │
│  PICK THE TOOL BY THE QUESTION:                                  │
│  - Union / gaps            → sort by START, merge                │
│  - Max non-overlapping     → sort by END, greedy                 │
│  - Max active at one time  → events sweep or min-heap of ends    │
│  - Which resource is free  → min-heap of ends                    │
│  - Load per point (small C)→ difference array                    │
│  - Online booking          → TreeMap floor / ceiling             │
│  - Two sorted lists        → two pointers, advance smaller end   │
│                                                                  │
│  SWEEP TIE RULE (equal time):                                    │
│  - Half-open: END (-1) before START (+1)                         │
│  - Closed:    START (+1) before END (-1)                         │
│                                                                  │
│  JAVA:                                                           │
│  - Comparator: Integer.compare(a[0], b[0]), never a[0] - b[0]    │
│  - Merge: last.end = max(last.end, iv.end)                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [How to Choose a Pattern →](../how-to-choose-a-pattern.md)*
