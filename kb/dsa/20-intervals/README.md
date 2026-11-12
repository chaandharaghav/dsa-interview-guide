---
priority: P1
format: lesson
---
# Pattern 20: Intervals and Sweep Line

## Overview

An **interval** is a pair of numbers, a start and an end, that describes a stretch of time or of positions. Interval problems ask how a set of intervals relates to itself: which ones overlap, what their union is, how many are active at one time, and which ones the code can keep when no two may overlap. The technique sorts the intervals (or their endpoints) by one key, so that each interval needs a comparison with one small summary of the earlier intervals and not with every other interval. A scan over the sorted endpoints from left to right that updates such a summary (the largest end so far, a counter, a heap of end times, or a map of heights) is a **sweep line**.

After this page you can:

1. State the overlap test for closed and for half-open intervals by negating "one interval ends before the other starts", and show the input on which `<` and `<=` give different answers.
2. Derive "sort by start, keep one current interval" from the pairwise merge, state its invariant (a statement that is true before the first step and stays true after every step), and show why the `max` on the end is needed with an input on which the code without it gives a wrong answer.
3. Choose the sort key from the question: start for a union, end for the largest set without overlap, the sorted endpoints for the number of active intervals. Give a counterexample for each wrong key.
4. Solve "the largest number of meetings at one time" two ways, with a sweep line and with a min heap of end times, and state the tie order of the events for half-open and for closed intervals with an input that shows the wrong order.
5. Choose between a difference array, a sweep over sorted events, and a `TreeMap` for load and booking problems, with the property of the input that decides.
6. Write the sweep for the skyline problem ([LC 218: The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/)) with a `TreeMap` of heights, and name the three defects that produce a wrong skyline.

## Prerequisites

| You need | Why this page needs it | Where to learn it |
|---|---|---|
| Counting operations and O-notation | The page counts the pair tests of the brute force before it states a bound | [Complexity Analysis](../foundations/01-complexity-analysis.md) |
| Sorting, and `Arrays.sort` with a comparator | Every solution starts with a sort. The sort key is the main choice of this pattern | [Sorting Algorithms](../foundations/08-sorting-algorithms.md), [Java Toolkit](../java-toolkit.md) |
| Balanced binary search trees and `TreeMap` | The booking problems and the skyline use `floorKey`, `ceilingKey`, and `lastKey` | [Trees and Balanced BSTs](../foundations/05-trees-and-balanced-bsts.md) |
| Heaps and `PriorityQueue` | The second solution of Meeting Rooms II uses a min heap of end times | [Heaps](../foundations/06-heaps.md), [Pattern 16: Heap and Priority Queue](../16-heap-priority-queue/README.md) |
| The exchange argument of greedy algorithms | The proof that "earliest end first" is optimal. This page gives a short version and links to the full proof | [Pattern 14: Greedy](../14-greedy/README.md) |
| Difference arrays and prefix sums | Worked Example 5 shows that a difference array is a sweep line with one cell for each position | [Pattern 04: Prefix Sum](../04-prefix-sum/README.md) |
| Two pointers on sorted arrays | The intersection of two sorted lists moves one pointer in each list | [Pattern 02: Two Pointers](../02-two-pointers/README.md) |

Words that the page uses from the start:

- The **start** and the **end** of an interval are its two **endpoints**. Every interval on this page has `start < end`, except where the text says that a point is allowed.
- Two intervals **overlap** if they share a stretch of positive length (half-open) or at least one position (closed). The Motivation defines both conventions and the test for each.
- Two intervals **touch** if one ends at the position where the other starts, such as `[1, 3]` and `[3, 5]`.
- The **union** of a set of intervals is the set of positions that at least one interval covers. The code writes the union as the shortest list of intervals that do not overlap.
- An interval is **active** at a position if the interval covers that position.
- A **comparator** is a function `compare(a, b)` that returns a negative number if `a` must come before `b`, zero if the order does not matter, and a positive number if `b` must come before `a`.

The vocabulary table of The Pattern defines **event** and **difference array**, and Worked Examples 3 and 5 use them.

## Motivation

### Two conventions, and the overlap test

An interval needs a rule that says whether its endpoints belong to it. Two rules are in use.

- A **closed** interval `[s, e]` holds its start and its end. The interval `[1, 3]` holds the positions 1, 2, and 3.
- A **half-open** interval `[s, e)` holds its start and not its end. The interval `[1, 3)` holds the positions 1 and 2. A meeting that runs from 1 to 3 and a meeting that runs from 3 to 5 do not share a position.

The problem statement says which rule applies. The rule decides every `<` and `<=` in the code. The page writes `[s, e)` for half-open intervals and `[s, e]` for closed intervals, and it writes `[s, e]` in traces where the rule is clear from the text.

**Deriving the test.** Start with the sentence "two intervals do not overlap when one of them ends before the other one starts". Call the intervals `a` and `b`.

| Convention | `a` and `b` do not overlap when | Negation (De Morgan): `a` and `b` overlap when |
|---|---|---|
| Half-open | `a.end <= b.start` or `b.end <= a.start` | `a.end > b.start` and `b.end > a.start` |
| Closed | `a.end < b.start` or `b.end < a.start` | `a.end >= b.start` and `b.end >= a.start` |

The negation of "A or B" is "not A and not B", and the negation of `<=` is `>`. Read the right column as code: half-open intervals overlap if `a.start < b.end && b.start < a.end`, and closed intervals overlap if `a.start <= b.end && b.start <= a.end`.

If the code has sorted the intervals by start, then `a.start <= b.start`. The condition `a.start < b.end` is then true for both conventions, because `a.start <= b.start < b.end` (half-open) and `a.start <= b.start <= b.end` (closed). So the test for two intervals in start order is one comparison: `b.start < a.end` for half-open intervals, and `b.start <= a.end` for closed intervals.

The program applies both tests to five pairs. The first pair touches:

```java
public class TouchingDemo {

    // Half-open [s, e): the interval holds s and does not hold e.
    static boolean overlapsHalfOpen(int[] a, int[] b) {
        return a[0] < b[1] && b[0] < a[1];
    }

    // Closed [s, e]: the interval holds s and e.
    static boolean overlapsClosed(int[] a, int[] b) {
        return a[0] <= b[1] && b[0] <= a[1];
    }

    public static void main(String[] args) {
        int[][][] pairs = {
            {{1, 3}, {3, 5}},   // touching: the first ends where the second starts
            {{1, 3}, {2, 5}},   // a real overlap
            {{1, 3}, {4, 5}},   // a gap between them
            {{1, 10}, {2, 3}},  // the second is nested in the first
            {{1, 3}, {1, 3}},   // equal
        };
        for (int[][] p : pairs) {
            System.out.printf("[%d,%d] and [%d,%d]: half-open %-5b closed %-5b%n",
                    p[0][0], p[0][1], p[1][0], p[1][1],
                    overlapsHalfOpen(p[0], p[1]), overlapsClosed(p[0], p[1]));
        }
    }
}
```

<!-- expected-output -->
```text
[1,3] and [3,5]: half-open false closed true 
[1,3] and [2,5]: half-open true  closed true 
[1,3] and [4,5]: half-open false closed false
[1,10] and [2,3]: half-open true  closed true 
[1,3] and [1,3]: half-open true  closed true 
```

On the touching pair `[1, 3]` and `[3, 5]`, the half-open test says `false` and the closed test says `true`. All other rows give the same answer for both tests. This one input is where `<` and `<=` differ, and each problem on this page has a rule for it.

### The anchor problem: merge overlapping intervals

**Statement.** [LC 56: Merge Intervals](https://leetcode.com/problems/merge-intervals/). The input is a list of closed intervals in any order. Return the shortest list of intervals that holds exactly the same positions, so that no two intervals of the answer overlap or touch. The problem treats intervals that touch as overlapping, so `[1, 4]` and `[4, 5]` become `[1, 5]`.

For the input `[8,10] [1,3] [2,6] [15,18] [17,20]` the answer is `[1,6] [8,10] [15,20]`. The intervals `[1,3]` and `[2,6]` share the positions 2 and 3. The intervals `[15,18]` and `[17,20]` share the positions 17 and 18. The interval `[8,10]` has no neighbor.

### First attempt: one pass in the order of the input

The simplest idea reads the list once. It keeps the interval that it builds, and it merges each next interval into it if the two overlap. On the anchor input, the code starts with `[8,10]`, reads `[1,3]` next, and the test `1 <= 10` is true, so the code treats `[1,3]` as overlapping `[8,10]`, although the two share no position. The merge keeps the larger end, 10, and keeps the start 8, so the positions 1 to 3 are lost. The program `IntervalTraces` in the Implementation section prints the result of this code, and the result of the correct code, for the same input:

```text
without sort on [8,10] [1,3] [2,6] [15,18] [17,20]: [8,10] [15,20]
with sort on    [8,10] [1,3] [2,6] [15,18] [17,20]: [1,6] [8,10] [15,20]
```

The first line is the result of the code that skips the sort. The second line is the correct answer. The answer `[8,10] [15,20]` has lost the positions 1 to 6. The code needs an order in which a gap in the list is a gap in the positions.

### Second attempt: compare every pair, and repeat

The brute force tests each pair of intervals. If a pair overlaps, it replaces the two intervals by their union. One merge can make a new overlap with another interval, so the code starts the search again after each merge, and stops when a full search finds no overlap.

The program counts the pair tests of this code on four inputs. The code is correct on every input. The counts show the cost.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BruteMerge {

    // Merge by pairs. After every merge, start the search again from the first pair.
    // comparisons[0] counts the pairs that the code tests for an overlap.
    static int[][] mergeByPairs(int[][] intervals, long[] comparisons) {
        List<int[]> list = new ArrayList<>();
        for (int[] iv : intervals) list.add(iv.clone());
        boolean merged = true;
        while (merged) {
            merged = false;
            for (int i = 0; i < list.size() && !merged; i++) {
                for (int j = i + 1; j < list.size() && !merged; j++) {
                    comparisons[0]++;
                    int[] a = list.get(i), b = list.get(j);
                    // closed overlap: touching counts
                    if (a[0] <= b[1] && b[0] <= a[1]) {
                        a[0] = Math.min(a[0], b[0]);
                        a[1] = Math.max(a[1], b[1]);
                        list.remove(j);
                        merged = true;
                    }
                }
            }
        }
        list.sort((a, b) -> Integer.compare(a[0], b[0]));
        return list.toArray(new int[0][]);
    }

    static String show(int[][] intervals) {
        StringBuilder sb = new StringBuilder();
        for (int[] iv : intervals) sb.append(Arrays.toString(iv).replace(" ", "")).append(' ');
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        int[][] anchor = {{8, 10}, {1, 3}, {2, 6}, {15, 18}, {17, 20}};
        long[] count = {0};
        int[][] result = mergeByPairs(anchor, count);
        System.out.println("anchor: " + show(result) + " after " + count[0] + " pair tests");

        // 1,000 intervals that touch no other interval: no merge ever happens.
        int n = 1000;
        int[][] apart = new int[n][];
        for (int i = 0; i < n; i++) apart[i] = new int[]{3 * i, 3 * i + 1};
        count[0] = 0;
        mergeByPairs(apart, count);
        System.out.println("1000 separate intervals: " + count[0] + " pair tests, no merge");

        // The worst case: m separate intervals first, then a chain of m intervals that each touch
        // the next.
        // Every merge happens in the last pairs, so each restart repeats all the earlier pair
        // tests.
        for (int m : new int[]{50, 100, 200}) {
            int[][] worst = new int[2 * m][];
            for (int i = 0; i < m; i++) worst[i] = new int[]{1000 + 3 * i, 1000 + 3 * i + 1};
            for (int i = 0; i < m; i++) worst[m + i] = new int[]{2 * i, 2 * i + 2};
            count[0] = 0;
            int[][] merged = mergeByPairs(worst, count);
            System.out.println("n = " + (2 * m) + ": " + merged.length + " intervals left after "
                    + count[0]
                    + " pair tests");
        }

        // The sort, counted with the same input size as the first test.
        int[][] shuffled = apart.clone();
        Random random = new Random(7);
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] tmp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = tmp;
        }
        long[] sortComparisons = {0};
        Arrays.sort(shuffled, (a, b) -> {
            sortComparisons[0]++;
            return Integer.compare(a[0], b[0]);
        });
        System.out.println("sort of 1000 shuffled intervals: " + sortComparisons[0]
                + " comparisons, then 999 pair tests in one scan");
    }
}
```

<!-- expected-output -->
```text
anchor: [1,6] [8,10] [15,20] after 14 pair tests
1000 separate intervals: 499500 pair tests, no merge
n = 100: 51 intervals left after 125049 pair tests
n = 200: 101 intervals left after 1000099 pair tests
n = 400: 201 intervals left after 8000199 pair tests
sort of 1000 shuffled intervals: 8674 comparisons, then 999 pair tests in one scan
```

On the anchor, the code makes 14 pair tests. On 1,000 intervals that do not overlap, it makes 499,500 pair tests and finds nothing: every pair `(i, j)` with `i < j` is tested once, and there are `1000 x 999 / 2 = 499,500` pairs. A single scan costs `n(n - 1) / 2` tests.

The worst case is worse. Each merge removes one interval, so there are at most `n - 1` merges. Each merge restarts the scan. The input of the program puts `m` separate intervals first and a chain of `m` touching intervals after them. Every merge happens in the last pairs, so every restart repeats the tests of the separate intervals. When `n` doubles from 100 to 200 and to 400, the count grows from 125,049 to 1,000,099 and to 8,000,199. Each doubling multiplies the count by about 8, which is the growth of `n^3`. The cost is O(n^3) in the worst case.

### The waste, and the question that removes it

The code tests pairs of intervals that cannot overlap. In the input of 1,000 separate intervals, 499,500 tests find nothing. The reason is the order of the input: the code has no information about where an interval is relative to another one, so any interval can overlap any other one.

The gap that the technique closes is the information about order. The question for the next section: if the list has an order, which of the earlier intervals can the next interval overlap, and what does the code need to remember about the others?

## Derivation

The derivation builds the merge from three questions. Each question comes from a defect of the attempts above.

### Question 1: if the list is sorted by start, which earlier intervals can the next interval overlap or touch?

Sort the list by start. Take the next interval `x` with start `s`. Every earlier interval `y` has `y.start <= s`. By the test for intervals in start order, `x` overlaps `y` exactly when `s <= y.end` (closed intervals, and touching intervals merge).

So the question about `y` is "does `y` reach the position `s`?" The earlier interval with the largest end reaches `s` if any earlier interval does. The code does not need the list of earlier intervals. It needs one number: the largest end so far. Call it `reach`.

### Question 2: what does the code do with `x`?

There are two cases.

1. **`s <= reach`.** The interval `x` overlaps or touches the interval with the largest end. Both belong to one merged interval. The merged interval starts where its first interval started, and it ends at `max(reach, x.end)`.
2. **`s > reach`.** The interval `x` starts after every earlier interval has ended. Every later interval starts at `s` or later, so no later interval can reach an earlier interval either. The merged interval that the code has built is complete. The code writes it to the result and starts a new merged interval with `x`.

The code keeps one interval, the **current** interval, which is the merged interval that it builds. Its end is `reach`.

### The trace

The trace is on the anchor input. The program prints these lines. Each line names the interval that the code reads, the comparison, and the action.

```text
input: [8,10] [1,3] [2,6] [15,18] [17,20]
sorted by start: [1,3] [2,6] [8,10] [15,18] [17,20]
take [1,3] as the current interval
[2,6]: start 2 <= current end 3 -> extend, end = max(3, 6) = 6
[8,10]: start 8 > current end 6 -> close [1,6], current = [8,10]
[15,18]: start 15 > current end 10 -> close [8,10], current = [15,18]
[17,20]: start 17 <= current end 18 -> extend, end = max(18, 20) = 20
close [15,20]
result: [1,6] [8,10] [15,20]
```

The input `[8,10] [1,3] [2,6] [15,18] [17,20]` becomes `[1,6] [8,10] [15,20]`. The comparison `2 <= 3` merges `[2,6]` into `[1,3]`. The comparison `8 > 6` closes `[1,6]`. The comparison `15 > 10` closes `[8,10]`. The comparison `17 <= 18` merges `[17,20]` into `[15,18]`.

### Why the end is a `max`

The line "extend, end = max(3, 6) = 6" has a `max`. A reader can ask why the code does not write `current end = x.end`. The reason is an interval that lies inside the current interval. Take `[1,10] [2,3] [4,5]`. All three intervals lie inside `[1,10]`, so the answer is `[1,10]`. Without the `max`, the code sets the end to 3 after it reads `[2,3]`, and then `4 > 3` closes the interval:

```text
without max on [1,10] [2,3] [4,5]: [1,3] [4,5]
with max on    [1,10] [2,3] [4,5]: [1,10]
```

The first line is the code without the `max`. The answer `[1,3] [4,5]` has lost the positions 6 to 10. With the `max`, the end stays at 10 and the answer is `[1,10]`. In Question 1 the number to keep is the largest end, and `max` is the operation that keeps it.

### Why the code is correct

**Invariant.** After the code has read the first `i` intervals of the sorted list, the result list plus the current interval is the shortest list of intervals, in order, that holds exactly the positions of those `i` intervals, and no two of its intervals overlap or touch. The end of the current interval is the largest end among the `i` intervals.

**Reason.** The reason has a start and a step.

- *Start.* For `i = 1`, the current interval is the first interval, the result list is empty, and the invariant holds.
- *Step, case `s <= reach`.* The new interval `x` starts at `s`, and the current interval starts at or before `s` (the list is sorted by start). So `x` starts inside the current interval, and the union of the two is the one interval from the start of the current interval to `max(reach, x.end)`. The code writes this interval to the current interval, so the invariant holds for `i + 1`. The earlier intervals in the result list end before the current interval starts, so they do not touch the new current interval.
- *Step, case `s > reach`.* Every interval of the first `i` intervals ends at or before `reach`, which is before `s`. The interval `x` is separate from all of them. The code moves the current interval into the result list and takes `x` as the new current interval. The invariant holds for `i + 1`. No later interval starts before `s`, so no later interval can touch an interval in the result list.

After the last interval, the code moves the current interval into the result list, and the invariant says that the result list is the answer.

**The edge cases.** If the input is empty, there is no first interval to take as the current interval. The code returns an empty list before it reads the first element. If the input has one interval, the loop does not run, and the answer is that interval. If all intervals are equal, each one satisfies `s <= reach`, and the answer is one interval. The program prints these cases:

```text
(none) -> (none)
[4,7] -> [4,7]
[1,10] [2,3] [4,5] -> [1,10]
[1,4] [4,5] -> [1,5]
[1,2] [3,4] -> [1,2] [3,4]
[2,5] [2,5] [2,5] -> [2,5]
[5,6] [1,2] -> [1,2] [5,6]
```

The case `[1,4] [4,5]` gives `[1,5]`, because the test is `<=` and touching intervals merge. The case `[1,2] [3,4]` gives two intervals, because `3 > 2`. The last case shows that the sort puts `[1,2]` before `[5,6]`.

### The cost, counted from the trace

The trace for 5 intervals has 4 comparisons between a start and `reach`. The scan makes `n - 1` comparisons for `n` intervals, and each comparison and each `max` takes constant time. The scan costs O(n).

The sort comes first. The program of the Motivation counted the comparisons that `Arrays.sort` makes for 1,000 intervals in random order: 8,674 comparisons. The brute force made 499,500 pair tests for the same number of intervals. A sort of `n` elements makes about `n log2 n` comparisons, and `1000 x log2(1000)` is about 9,966, which is the same size as the count. The sort is the largest part, so the time is **O(n log n)**. The space is O(n): the sorted copy and the result list.

### The code

The code follows the trace line by line. The comments mark the lines that a new problem changes.

```java
static int[][] mergeIntervals(int[][] intervals) {
    if (intervals.length == 0) return new int[0][];            // nothing to merge
    int[][] sorted = intervals.clone();                        // keep the caller's order
    Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0])); // sort by start
    List<int[]> merged = new ArrayList<>();
    // the interval that is being built
    int[] current = sorted[0].clone();
    for (int i = 1; i < sorted.length; i++) {
        if (sorted[i][0] <= current[1]) {                      // <= : touching intervals merge
            // max: the next one can be nested
            current[1] = Math.max(current[1], sorted[i][1]);
        } else {
            // a gap: nothing later can reach it
            merged.add(current);
            current = sorted[i].clone();
        }
    }
    merged.add(current);
    return merged.toArray(new int[0][]);
}
```

The code sorts a copy (`clone`) so that the caller's array keeps its order. It also clones the interval that becomes the current interval, because `current[1] = ...` writes into it, and the write must not change an interval of the input. The sort uses `Integer.compare(a[0], b[0])`. The difference `a[0] - b[0]` can overflow (see Common Mistakes).

## The Pattern

An unsorted list of intervals gives the code no information about where an interval lies relative to the others, so every interval can overlap every other interval, and the code must test every pair. After a sort, the code needs one summary of the past: the largest end so far (merge), the end of the last interval kept (selection), a counter (concurrency), a heap of end times (assignment), or a map of the active heights (skyline). The choice of the sort key and of the summary depends on the question, and the convention (closed or half-open) decides each `<` and `<=`.

**The invariant of the pattern.** After the code has processed the first `i` items in the sorted order, the summary holds exactly what the answer for the later items needs to know about those `i` items. For the merge, the summary is the current interval and its end `reach`. The proof of each solution on this page states what the summary holds, shows that a step keeps it, and checks the empty input and the single interval.

### The vocabulary

| Term | What it was in the merge | The question to ask in a new problem |
|---|---|---|
| **Convention** (closed or half-open) | Closed: `[1,4]` and `[4,5]` merge | Does an interval that ends at `t` overlap an interval that starts at `t`? |
| **Overlap test** | `s <= reach` for intervals in start order | Which comparison is the negation of "one ends before the other starts" for this convention? |
| **Sort key** | The start | Which key makes the earlier intervals reduce to one summary? Start for a union. End for a largest set without overlap |
| **Summary** (the running state) | The current interval and `reach`, the largest end so far | What does the answer for later intervals need to know about the earlier ones? |
| **Gap** | `s > reach`: a position between two merged intervals | Does the problem ask for the gaps, or for the merged intervals? |
| **Event** | Not used by the merge. An event is a position where the answer can change: a start or an end. The code turns each interval into two events | Does the answer change only at endpoints? Then sort the endpoints |
| **Sweep line** | The scan from the smallest start to the largest start, with one summary | What does the scan update at each event? |
| **Tie order** | Not used by the merge, because the merge compares an interval with a number. Events at the same position need an order | Which event goes first at the same position, the end or the start? |
| **Difference array** | Not used by the merge. An array whose cell `p` holds the change in the count at position `p`. The prefix sum of the array is the count | Are the positions small integers, so that one cell for each position fits in memory? |

### Which tool answers which question

Read the table from the left. The property of the question picks the sort key or the structure.

| The question | The property that selects the tool | Sort key or structure | The summary | Problems | Time |
|---|---|---|---|---|---|
| What is the union? Where are the gaps? Do any two overlap? | The answer is fixed by the input. The problem has no choice to make | Sort by **start** | The largest end so far | [LC 56](https://leetcode.com/problems/merge-intervals/), [LC 759](https://leetcode.com/problems/employee-free-time/) (premium), [LC 252](https://leetcode.com/problems/meeting-rooms/) (premium), [LC 1288](https://leetcode.com/problems/remove-covered-intervals/) | O(n log n) |
| The same, and the list is already sorted and disjoint | The input has an order | No sort. Three phases | The new interval, widened | [LC 57](https://leetcode.com/problems/insert-interval/) | O(n) |
| What is the largest set of intervals with no overlap? How many removals? How few points touch every interval? | The code chooses which intervals to keep. Every interval weighs the same | Sort by **end** | The end of the last interval kept | [LC 435](https://leetcode.com/problems/non-overlapping-intervals/), [LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/), [LC 646](https://leetcode.com/problems/maximum-length-of-pair-chain/) | O(n log n) |
| How many intervals are active at one time? (rooms, groups, platforms) | The question asks for a count, not for an assignment | Sort the **events**, with a tie order. Or sort the starts and the ends in two arrays | A counter | [LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium), [LC 2406](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/) | O(n log n) |
| Which room is free first? Assign each interval to a resource | The code must name the resource | Sort by start. A **min heap** of end times | One end time for each resource | [LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium), [LC 2402](https://leetcode.com/problems/meeting-rooms-iii/) | O(n log n) |
| Which active interval is the best one for a query? | The answer depends on which interval is active and best (the smallest), not on how many are active | Sort the intervals by start and the queries by position. A **min heap** of the active intervals, ordered by size | The active intervals, smallest on top. The code removes a top interval that ends before the query | [LC 1851](https://leetcode.com/problems/minimum-interval-to-include-each-query/) | O(n log n) |
| What is the load at every position? | All updates are known first, and the positions are small integers | A **difference array** | The array, then a prefix sum | [LC 1094](https://leetcode.com/problems/car-pooling/), [LC 1109](https://leetcode.com/problems/corporate-flight-bookings/), [LC 1854](https://leetcode.com/problems/maximum-population-year/) | O(n + C) for C positions |
| Does this new interval conflict with the booked ones? | Each booking needs an answer before the next one arrives | A **`TreeMap`** with `floorKey` and `ceilingKey` | The booked intervals, sorted | [LC 729](https://leetcode.com/problems/my-calendar-i/), [LC 731](https://leetcode.com/problems/my-calendar-ii/), [LC 732](https://leetcode.com/problems/my-calendar-iii/) | O(log n) for LC 729 |
| What do two sorted lists of intervals have in common? | Two inputs, each sorted and disjoint | **Two pointers** | The two current intervals | [LC 986](https://leetcode.com/problems/interval-list-intersections/) | O(m + n) |
| What is the tallest active building at each position? | The summary needs add, remove, and maximum | Sort the events. A **`TreeMap`** of height to count | The active heights | [LC 218](https://leetcode.com/problems/the-skyline-problem/) | O(n log n) |
| Each interval has a weight, and the code maximizes the total | A weight breaks the exchange argument of the sort by end | Not this pattern | Dynamic programming | [LC 1235](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) | See [Pattern 15](../15-dynamic-programming/README.md) |

### The comparison that each classic problem uses

The convention of the problem decides each row. Read the column "Touching endpoints" with the pair `[1,4]` and `[4,5]`, or with `[1,4)` and `[4,5)` for the half-open problems.

| Problem | Convention | Touching endpoints | The comparison in the code |
|---|---|---|---|
| [LC 56](https://leetcode.com/problems/merge-intervals/) | Closed | They merge | `start <= current end` extends the current interval |
| [LC 57](https://leetcode.com/problems/insert-interval/) | Closed | They merge | `end < new.start` copies an interval. `start <= new.end` merges it |
| [LC 435](https://leetcode.com/problems/non-overlapping-intervals/) | Half-open | They do not overlap | `start >= lastEnd` keeps the interval |
| [LC 646](https://leetcode.com/problems/maximum-length-of-pair-chain/) | Closed | They cannot follow each other | `start > lastEnd` keeps the pair |
| [LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) | Closed | One point touches both | `start > lastPoint` needs a new point |
| [LC 986](https://leetcode.com/problems/interval-list-intersections/) | Closed | They intersect in one position | `lo <= hi` writes an intersection |
| [LC 252](https://leetcode.com/problems/meeting-rooms/) (premium) | Half-open | They do not conflict | `start < previous end` returns `false` |
| [LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium) | Half-open | They share a room | `earliest end <= start` frees the room |
| [LC 729](https://leetcode.com/problems/my-calendar-i/) | Half-open | They do not conflict | `before.end > start` rejects the booking |
| [LC 1094](https://leetcode.com/problems/car-pooling/) | Half-open in the positions | The passengers leave before others board | The departure goes to `diff[to]` |

### The variants, and the property that selects each

**A. Merge and union.** Problems: merge ([LC 56](https://leetcode.com/problems/merge-intervals/)), insert ([LC 57](https://leetcode.com/problems/insert-interval/)), the gaps in the schedules of several employees ([LC 759](https://leetcode.com/problems/employee-free-time/) (premium)), remove covered intervals ([LC 1288](https://leetcode.com/problems/remove-covered-intervals/)). The selecting property: the answer is a function of the input, and the code makes no choice. The sort key is the start. The summary is the current interval, or the largest end so far.

**B. Largest set without overlap.** Problems: minimum removals ([LC 435](https://leetcode.com/problems/non-overlapping-intervals/)), minimum arrows ([LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/)), longest pair chain ([LC 646](https://leetcode.com/problems/maximum-length-of-pair-chain/)). The selecting property: the code chooses a subset, and every interval counts one. The sort key is the end. The summary is the end of the last interval kept.

**C. Maximum concurrency.** Problems: meeting rooms ([LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium)), minimum groups ([LC 2406](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/)), platforms at a station. The selecting property: the answer is the largest number of intervals that cover one position. The tools are the event sweep with a tie order and the min heap of end times. Half-open against closed decides the tie order.

**D. Resource assignment, and the best active interval.** Problems: the room that hosts the most meetings ([LC 2402](https://leetcode.com/problems/meeting-rooms-iii/)), the smallest interval that contains each query ([LC 1851](https://leetcode.com/problems/minimum-interval-to-include-each-query/)). The selecting property: the answer depends on which active interval is the best one (the room that frees first, the smallest interval that covers the query), and not only on how many intervals are active. The tool is a heap. In [LC 2402](https://leetcode.com/problems/meeting-rooms-iii/), a delayed meeting ends later than the input says, so end times grow past the input range. The code stores them as `long`.

**E. Online booking.** Problems: [LC 729](https://leetcode.com/problems/my-calendar-i/), [LC 731](https://leetcode.com/problems/my-calendar-ii/), [LC 732](https://leetcode.com/problems/my-calendar-iii/), [LC 715: Range Module](https://leetcode.com/problems/range-module/), [LC 352: Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/). The selecting property: each request needs an answer before the next request arrives, so the code cannot sort all requests first. The tool is a `TreeMap`. `floorEntry` and `ceilingEntry` find the neighbors of a new interval in O(log n).

**F. Load at each position.** Problems: [LC 1094](https://leetcode.com/problems/car-pooling/), [LC 1109](https://leetcode.com/problems/corporate-flight-bookings/), [LC 1854](https://leetcode.com/problems/maximum-population-year/). The selecting property: the code needs the count at every position and all updates are known first. The tool is a difference array when the positions are small integers, and sorted events when they are not.

**G. Two lists.** Problems: [LC 986](https://leetcode.com/problems/interval-list-intersections/), [LC 1229: Meeting Scheduler](https://leetcode.com/problems/meeting-scheduler/) (premium). The selecting property: two inputs, each a list of intervals with no overlap inside the list. If a list is not sorted (LC 1229), the code sorts it by start first. The tool is two pointers. The code advances the pointer of the interval that ends first.

**H. Sweep line with a height structure.** Problem: [LC 218](https://leetcode.com/problems/the-skyline-problem/). The selecting property: each interval carries a value, and the answer at a position is the maximum value among the active intervals. The tool is a sorted sweep with a `TreeMap` of value to count, or a max heap with **lazy deletion** (the code removes an element from the heap only when it reaches the top). `PriorityQueue.remove(Object)` costs O(n), so a call for each event makes the solution O(n^2).

## Worked Examples

Each example changes one thing about the anchor problem. The examples go from the easiest to the hardest. Attempt each problem that has an LC number before you read its section.

### Example 1: Insert Interval (LC 57), the input is already sorted

**Statement.** [LC 57: Insert Interval](https://leetcode.com/problems/insert-interval/). The input is a list of closed intervals, sorted by start, in which no two intervals overlap, and one new interval. Insert the new interval, and merge every interval that it overlaps or touches. For the list `[1,2] [3,5] [6,7] [8,10] [12,16]` and the new interval `[4,8]`, the answer is `[1,2] [3,10] [12,16]`.

**What changed.** The anchor problem gave an unordered list. This list is sorted and disjoint, so the sort is not needed.

**From the brute force.** The brute force appends the new interval to the list and calls the merge of the anchor problem. It sorts again, so it costs O(n log n) and ignores the order that the input already has.

**Question.** In a sorted, disjoint list, where does the new interval `[a, b]` fall? Both the starts and the ends of the list increase from left to right. So each interval of the list is in exactly one of three groups, and the groups follow each other:

1. **Before.** The interval ends before `a`: `end < a`. It cannot touch the new interval. These intervals are a prefix of the list, because the ends increase.
2. **Overlapping.** The interval is not in group 1 (`end >= a`), and it starts at or before `b` (`start <= b`). It touches the new interval, so it becomes part of it. These intervals follow group 1 without a gap, because the starts increase: the first interval that starts after `b` ends the group.
3. **After.** The interval starts after `b`: `start > b`. It cannot touch the new interval, and neither can any later interval.

The code copies group 1, merges group 2 into the new interval (the new start is the smaller start, the new end is the larger end), writes the new interval, and copies group 3. Each interval is read once.

**Trace.** The program prints the three phases for the example:

```text
input: [1,2] [3,5] [6,7] [8,10] [12,16]
new interval [4,8]
phase 1: [1,2] ends at 2 < 4 -> copy
phase 2: [3,5] starts at 3 <= 8 -> new interval becomes [3,8]
phase 2: [6,7] starts at 6 <= 8 -> new interval becomes [3,8]
phase 2: [8,10] starts at 8 <= 8 -> new interval becomes [3,10]
add the new interval [3,10]
phase 3: [12,16] starts after the new interval -> copy
result: [1,2] [3,10] [12,16]
```

The interval `[8,10]` is in group 2 because `8 <= 8`: it touches the new interval at the position 8, and touching intervals merge. The interval `[12,16]` starts after 10, so it is in group 3.

**Code.**

```java
static int[][] insertInterval(int[][] intervals, int[] added) {
    List<int[]> result = new ArrayList<>();
    int start = added[0], end = added[1];
    int i = 0;
    // Phase 1: intervals that end before the new one starts. They stay as they are.
    while (i < intervals.length && intervals[i][1] < start) {
        result.add(intervals[i++]);
    }
    // Phase 2: intervals that overlap the new one. Each one widens the new one.
    while (i < intervals.length && intervals[i][0] <= end) {
        start = Math.min(start, intervals[i][0]);
        end = Math.max(end, intervals[i][1]);
        i++;
    }
    result.add(new int[]{start, end});
    // Phase 3: intervals that start after the new one ends. They stay as they are.
    while (i < intervals.length) {
        result.add(intervals[i++]);
    }
    return result.toArray(new int[0][]);
}
```

Phase 1 uses `<`, because an interval that ends at `a` touches the new interval and belongs to phase 2. Phase 2 uses `<=` for the same reason. The edge cases:

```text
empty list, add [5,6]: [5,6]
add before all, [0,0]: [0,0] [1,2] [3,5] [6,7] [8,10] [12,16]
add after all, [20,21]: [1,2] [3,5] [6,7] [8,10] [12,16] [20,21]
add inside a gap, [10,11]: [1,2] [3,5] [6,7] [8,11] [12,16]
add touching the first, [2,3]: [1,5] [6,7] [8,10] [12,16]
add covering all, [0,20]: [0,20]
```

The new interval can go into an empty list, before all intervals, after all intervals, into a gap (`[10,11]` touches `[8,10]` and extends it to `[8,11]`), or over all intervals.

**Complexity.** Time O(n): the three loops move the index `i` from 0 to `n` once. Space O(n) for the result. The code copies group 1 and group 3 by reference, so it does not copy the intervals.

**The mistake that this problem invites.** The code is correct only for a sorted, disjoint list. On the list `[6,7] [1,2] [3,5]` the three phases give a wrong answer (Common Mistakes, mistake 7).

### Example 2: Non-overlapping Intervals (LC 435), the code chooses, so the sort key is the end

**Statement.** [LC 435: Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/). The input is a list of half-open intervals in any order. Return the smallest number of intervals to remove so that no two remaining intervals overlap. Intervals that only touch (`[1,2)` and `[2,3)`) do not overlap. For `[0,5) [1,2) [2,4) [4,6) [5,9) [8,10)` the answer is 2.

**What changed.** The merge had no choice: the answer was a function of the input. Here the code chooses which intervals to keep. The answer is `n` minus the size of the largest set of intervals that do not overlap, so the problem is to find that set.

**From the brute force.** The brute force tries every subset of the `n` intervals and keeps the largest one that has no overlap. There are `2^n` subsets, so 40 intervals give about a trillion subsets.

**First attempt: sort by start, keep an interval if it fits.** The code reads the intervals in start order and keeps each interval that does not overlap the last kept one. The program runs it on `[1,100) [2,3) [4,5)`:

```text
sort by end on [1,100] [2,3] [4,5]: kept 2
sort by start on [1,100] [2,3] [4,5]: kept 1
```

The code with the start as the key keeps only `[1,100)`, because `[1,100)` comes first and blocks the other two. The best set is `[2,3) [4,5)`, with size 2. A long interval that starts early takes the room of every interval after it.

**Question.** Which interval is safe to keep first? The one that leaves the most room for the others: the interval with the smallest end. Call it `g`.

**Why `g` is safe.** Take any largest set `O` of intervals that do not overlap, and take its interval `o` with the smallest end. Because `g` has the smallest end of all intervals, `g.end <= o.end`. Every other interval of `O` starts at or after `o.end`, so it starts at or after `g.end`. Replace `o` by `g`: the new set has the same size, and no two of its intervals overlap. So some largest set contains `g`. After the code keeps `g`, the intervals that remain are exactly those that start at or after `g.end`, and the same argument applies to them. [Pattern 14: Greedy](../14-greedy/README.md) proves this exchange argument in full, with the edge cases (no intervals, equal ends) and the second form of the proof ("stays ahead"). This page uses the result.

**The scan.** Sort by end. Keep `lastEnd`, the end of the last kept interval. The first interval in the sorted order with `start >= lastEnd` is the interval with the smallest end among the intervals that fit. Keep it, and set `lastEnd` to its end.

**Trace.**

```text
input: [0,5] [1,2] [2,4] [4,6] [5,9] [8,10]
sorted by end: [1,2] [2,4] [0,5] [4,6] [5,9] [8,10]
[1,2]: start 1 >= last end none -> keep, kept = 1
[2,4]: start 2 >= last end 2 -> keep, kept = 2
[0,5]: start 0 <  last end 4 -> skip
[4,6]: start 4 >= last end 4 -> keep, kept = 3
[5,9]: start 5 <  last end 6 -> skip
[8,10]: start 8 >= last end 6 -> keep, kept = 4
kept 4, removed 2
```

The code keeps `[1,2)`, `[2,4)`, `[4,6)`, and `[8,10)`, and removes `[0,5)` and `[5,9)`. Two of the keeps are touching intervals: `2 >= 2` and `4 >= 4`. The program also runs both keys on this input and prints the number of kept intervals:

```text
sort by end on [0,5] [1,2] [2,4] [4,6] [5,9] [8,10]: kept 4
sort by start on [0,5] [1,2] [2,4] [4,6] [5,9] [8,10]: kept 2
```

The sort by end keeps 4, and the sort by start keeps 2.

**Code.**

```java
// Half-open intervals. Returns the largest number of intervals that do not overlap.
static int maxNonOverlapping(int[][] intervals) {
    int[][] sorted = intervals.clone();
    Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));   // sort by END
    int kept = 0;
    long lastEnd = Long.MIN_VALUE;            // the end of the last interval kept
    for (int[] iv : sorted) {
        if (iv[0] >= lastEnd) {               // >= : [1,2) and [2,3) do not overlap
            kept++;
            lastEnd = iv[1];
        }
    }
    return kept;
}
```

The method returns the size of the largest set. For LC 435, return `intervals.length` minus that size. The variable `lastEnd` is a `long`, and it starts at `Long.MIN_VALUE`, so the first interval always passes, and the code needs no flag for "no interval kept yet". The test `>=` keeps an interval that starts where the last one ends.

**What changes for the closed variants.**

- [LC 452: Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) asks for the fewest points that touch every closed interval. The scan is the same, and each point goes at the earliest end. An interval that starts exactly at the point is hit, so a new point is needed only when `start > lastPoint`. Pattern 14 solves this problem in full (its practice problem P6).
- [LC 646: Maximum Length of Pair Chain](https://leetcode.com/problems/maximum-length-of-pair-chain/) asks for the longest chain of pairs in which each pair starts after the previous one ends. The test is also strict, `start > lastEnd`. It is practice problem P5 on this page.

```java
// Closed intervals. Returns the fewest points that touch every interval (LC 452 shape).
static int minPoints(int[][] intervals) {
    int[][] sorted = intervals.clone();
    Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));   // sort by END
    int points = 0;
    long lastPoint = Long.MIN_VALUE;
    for (int[] iv : sorted) {
        // > : a closed interval that starts AT the point is hit
        if (iv[0] > lastPoint) {
            points++;
            lastPoint = iv[1];                // the point goes at the earliest end
        }
    }
    return points;
}
```

The program shows the effect of the two tests on the touching input `[1,2] [2,3]`:

```text
half-open [1,2] [2,3]: kept 2, with > instead of >=: kept 1
closed    [1,2] [2,3]: points 1
```

As half-open intervals, the two do not overlap, and `>=` keeps both. With `>` in place of `>=`, the code keeps 1, and the answer for LC 435 would be 1 removal in place of 0. As closed intervals, the two share the position 2, and one point touches both.

**Complexity.** Time O(n log n) for the sort, then O(n) for the scan. Space O(n) for the sorted copy. The sort makes about `n log2 n` comparisons.

**Edge cases.**

```text
(none) -> kept 0, points 0
[3,4] -> kept 1, points 1
[1,2] [1,2] [1,2] -> kept 1, points 1
[1,5] [2,6] [3,7] -> kept 1, points 1
```

For an empty input, the code keeps 0 intervals. For equal intervals, it keeps one. The largest set of `[1,5) [2,6) [3,7)` has size 1, because the three intervals share the positions 3 and 4.

### Example 3: Meeting Rooms II (LC 253, premium), count the active intervals

**Statement.** [LC 253: Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) (premium). The input is a list of meetings, each a half-open interval `[start, end)`. Return the smallest number of rooms such that no two meetings that overlap share a room. A meeting that ends at time 10 and a meeting that starts at time 10 do not overlap. For `[0,30) [5,10) [10,25) [15,20) [28,35)` the answer is 3.

**What changed.** The merge and the selection kept or joined intervals. This problem asks for a count: the largest number of meetings that run at the same time. Each room holds one meeting at a time, so the answer is that largest number.

**From the brute force.** For each start time `t`, count the meetings with `start <= t < end`. The largest count is the answer. The code makes `n` counts of `n` meetings each, so the cost is O(n^2). It tests only the start times, because the count can rise only at a start.

**Question 1: at which times can the count change?** Only at a start (the count rises by 1) and at an end (the count falls by 1). Between two such times the count stays the same. So the code does not need to look at the meetings again. It turns each meeting into two **events**, `(start, +1)` and `(end, -1)`, sorts the events by time, and scans them with a counter. The largest value of the counter is the answer. This scan is a sweep line: it moves along the time axis and updates one number at each event.

**Question 2: what is the order of two events at the same time?** In the input, the meeting `[5,10)` ends at 10 and the meeting `[10,25)` starts at 10. The two events have the same time. If the start comes first, the counter rises before it falls, and the code counts both meetings as active at time 10. A half-open meeting does not hold its end, so the meeting `[5,10)` is not active at time 10. The end must come first. The tie order for half-open intervals is: **end (-1) before start (+1)**.

**The counterexample.** The input `[0,10) [10,20)` needs 1 room. The program runs the sweep with both tie orders:

```text
half-open [0,10] [10,20]: ends first gives 1, starts first gives 2
closed    [0,10] [10,20]: starts first gives 2, ends first gives 1
```

With the ends first, the half-open input needs 1 room. With the starts first, it needs 2. The second row is the closed case: two closed intervals `[0,10]` and `[10,20]` share the position 10, so they need 2 rooms, and the starts must come first (this is the rule for [LC 2406](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/)).

**Trace.** The program prints the sorted events and the counter for the example. The two events at time 10 appear in the order end, start:

```text
meetings: [0,30] [5,10] [10,25] [15,20] [28,35]
time  0  start (+1)  active = 1  most = 1
time  5  start (+1)  active = 2  most = 2
time 10  end   (-1)  active = 1  most = 2
time 10  start (+1)  active = 2  most = 2
time 15  start (+1)  active = 3  most = 3
time 20  end   (-1)  active = 2  most = 3
time 25  end   (-1)  active = 1  most = 3
time 28  start (+1)  active = 2  most = 3
time 30  end   (-1)  active = 1  most = 3
time 35  end   (-1)  active = 0  most = 3
answer: 3
```

**Code.**

```java
// Returns the largest number of intervals that are active at one time.
// halfOpen = true : [s, e) intervals, an end at time t frees the room for a start at time t.
// halfOpen = false: [s, e] intervals, a start at time t shares the point t with an end at t.
static int maxActiveBySweep(int[][] intervals, boolean halfOpen) {
    int[][] events = new int[intervals.length * 2][];
    int k = 0;
    for (int[] iv : intervals) {
        events[k++] = new int[]{iv[0], +1};      // a start adds one active interval
        events[k++] = new int[]{iv[1], -1};      // an end removes one
    }
    // The tie order: at equal times, ends first for half-open, starts first for closed.
    Comparator<int[]> order = (a, b) -> {
        if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
        return halfOpen ? Integer.compare(a[1], b[1]) : Integer.compare(b[1], a[1]);
    };
    Arrays.sort(events, order);
    int active = 0, most = 0;
    for (int[] event : events) {
        active += event[1];
        most = Math.max(most, active);
    }
    return most;
}
```

The comparator sorts by time. For equal times it compares the change: `Integer.compare(a[1], b[1])` puts `-1` before `+1`, which is the half-open order. `Integer.compare(b[1], a[1])` puts `+1` first, which is the closed order.

**Why the largest count is the number of rooms.** Lower bound: if `k` meetings are active at one time, each pair of them overlaps, so they need `k` different rooms. Upper bound: give each meeting, in the order of the starts, a room that is free at its start, and open a new room only if none is free. Suppose the code opens room `r + 1` for a meeting that starts at `s`. Then each of the `r` rooms in use holds a meeting that is still active at `s`, so `r + 1` meetings are active at `s`. So the code never opens more rooms than the largest count. If the list is empty, no event exists, the counter stays 0, and the answer is 0.

**The same count from two sorted arrays.** At a time `t`, the number of active half-open meetings is the number of starts at or before `t` minus the number of ends at or before `t`. The code needs only these two counts, so the pairing of a start with its end does not matter. Sort the starts in one array and the ends in another array. Scan the starts with a pointer `s`. Before the code counts the start `starts[s]`, a second pointer `e` moves over every end with `ends[e] <= starts[s]` and takes one active meeting away for each. The test `<=` is the end-first tie order of the sweep, because a meeting that ends at `t` is not active at `t`. For closed intervals the test is `<`, which is the start-first tie order. [Pattern 14: Greedy](../14-greedy/README.md) gives the code of this form. The program in the Implementation section runs the sweep, the heap, the two-array form, and the brute force on the example, and prints the answers of all four:

```text
sweep 3, heap 3, two arrays 3, brute force 3
```

**The second solution: a min heap of end times.** Sort the meetings by start. A new meeting starts at time `s`. Which room can it use? A room is free if its meeting has ended by `s`. If any room is free, the room that frees earliest is free. So the code needs the smallest end time among the rooms in use, and the set of end times changes at every meeting. A min heap gives the smallest value in O(1) and replaces a value in O(log n). For each meeting: if the smallest end time is at most `s`, remove it (the room is reused). Add the end time of the new meeting. The answer is the size of the heap. [Pattern 16: Heap and Priority Queue](../16-heap-priority-queue/README.md) (Worked Example 6) gives the invariant and the proof. The program prints the heap after each meeting:

```text
[0,30]: no room yet, open one; end times now [30]
[5,10]: earliest end 30 > 5, open a room; end times now [10, 30]
[10,25]: earliest end 10 <= 10, reuse that room; end times now [25, 30]
[15,20]: earliest end 25 > 15, open a room; end times now [20, 25, 30]
[28,35]: earliest end 20 <= 28, reuse that room; end times now [25, 30, 35]
answer: 3
```

```java
static int maxActiveByHeap(int[][] intervals) {
    int[][] sorted = intervals.clone();
    Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));   // sort by start
    // end time of each room in use
    PriorityQueue<Integer> ends = new PriorityQueue<>();
    for (int[] iv : sorted) {
        if (!ends.isEmpty() && ends.peek() <= iv[0]) {
            ends.poll();                                          // the earliest room is free
        }
        ends.offer(iv[1]);
    }
    return ends.size();
}
```

**Sweep or heap?** The sweep gives only the count. The heap holds one end time for each room in use, so the code can also say which room a meeting uses, or which room frees first. Use the sweep when the question asks for a count, and the heap when the code must assign each meeting to a room ([LC 2402: Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/)).

**Complexity.** Sweep: the sort of `2n` events costs O(n log n), and the scan costs O(n). Space O(n). Heap: the sort costs O(n log n), and each of the `n` meetings makes at most one `poll` and one `offer` on a heap of at most `n` entries, so the loop costs O(n log n). Space O(n).

**Edge cases.** The program prints these results for both solutions:

```text
(none) -> sweep 0, heap 0
[4,9] -> sweep 1, heap 1
[1,5] [1,5] [1,5] -> sweep 3, heap 3
[1,2] [2,3] [3,4] -> sweep 1, heap 1
[1,10] [2,3] [4,5] -> sweep 2, heap 2
```

An empty input needs 0 rooms. Three equal meetings need 3 rooms. The meetings `[1,2) [2,3) [3,4)` touch, and one room is enough. The nested meetings `[1,10) [2,3) [4,5)` need 2 rooms, because `[2,3)` and `[4,5)` do not overlap each other.

### Example 4: Interval List Intersections (LC 986), two sorted lists

**Statement.** [LC 986: Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/). The input is two lists of closed intervals. Each list is sorted by start, and no two intervals of one list share a position. Return the intersection of the two lists: all positions that an interval of the first list and an interval of the second list share, as a list of intervals. For `A = [0,2] [5,10] [13,23] [24,25]` and `B = [1,5] [8,12] [15,24] [25,26]` the answer is `[1,2] [5,5] [8,10] [15,23] [24,24] [25,25]`.

**What changed.** The input is two lists, and the answer is the part that the lists share.

**From the brute force.** Test every interval of `A` against every interval of `B`. The intersection of `[a1, a2]` and `[b1, b2]` is `[max(a1, b1), min(a2, b2)]`, and it is empty if the start is larger than the end (`lo > hi`). The brute force makes `m x n` tests.

**Question.** After the code tests `A[i]` against `B[j]`, one of the two intervals has no further use. Which one? Suppose `A[i].end < B[j].end`. Every later interval of `B` starts after `B[j].end` (the list is disjoint and sorted), so it starts after `A[i].end`. The interval `A[i]` cannot meet any later interval of `B`, and the code has tested it against `B[j]`. So the code drops `A[i]`. The interval `B[j]` can still meet `A[i + 1]`, so the code keeps it. The rule: **drop the interval that ends first**. If the two ends are equal, neither interval can meet the next interval of the other list, so dropping either one is correct.

**The counterexample for the wrong pointer.** If the code drops the interval that starts first, the wrong interval can go. The program runs both rules on `A = [0,10]` and `B = [1,2] [3,4]`:

```text
A [0,10], B [1,2] [3,4]
advance the smaller end:   [1,2] [3,4]
advance the smaller start: [1,2]
```

The rule "advance the smaller start" drops `[0,10]` after the first test, because `0 < 1`. The second interval `[3,4]` of `B` then has nothing to meet, and the answer misses `[3,4]`.

**Trace.**

```text
A: [0,2] [5,10] [13,23] [24,25]
B: [1,5] [8,12] [15,24] [25,26]
lo 1 <= hi 2 -> add [1,2]; A0 ends first, i++
lo 5 <= hi 5 -> add [5,5]; B0 ends first (or tie), j++
lo 8 <= hi 10 -> add [8,10]; A1 ends first, i++
lo 13 >  hi 12 -> none; B1 ends first (or tie), j++
lo 15 <= hi 23 -> add [15,23]; A2 ends first, i++
lo 24 <= hi 24 -> add [24,24]; B2 ends first (or tie), j++
lo 25 <= hi 25 -> add [25,25]; A3 ends first, i++
result: [1,2] [5,5] [8,10] [15,23] [24,24] [25,25]
```

Each line is one test. At the line `lo 13 > hi 12`, the intervals `[13,23]` and `[8,12]` do not meet, and the code drops `B1` because it ends first. At the line `lo 24 <= hi 24`, the two intervals share one position, and the closed test `lo <= hi` keeps it.

**Code.**

```java
// Both lists hold closed intervals, sorted by start, and no two intervals of one list share a
// point.
static int[][] intervalIntersections(int[][] a, int[][] b) {
    List<int[]> result = new ArrayList<>();
    int i = 0, j = 0;
    while (i < a.length && j < b.length) {
        // the intersection starts at the later start
        int lo = Math.max(a[i][0], b[j][0]);
        int hi = Math.min(a[i][1], b[j][1]);          // and ends at the earlier end
        // <= : one shared point is an intersection
        if (lo <= hi) {
            result.add(new int[]{lo, hi});
        }
        if (a[i][1] < b[j][1]) i++;                   // drop the interval that ends first
        else j++;
    }
    return result.toArray(new int[0][]);
}
```

**Why the code is correct.** The invariant: every intersection of an interval `A[p]` with an interval `B[q]`, where `p < i` or `q < j`, is already in the result. At the start, `i = j = 0`, and no such pair exists. Take one step. The code tests `A[i]` against `B[j]` and writes their intersection if it exists. Suppose `A[i]` ends first, so the code drops it. `A[i]` cannot meet `B[j + 1]` or any later interval of `B`, because those start after `B[j].end`, and `B[j].end >= A[i].end`. The code has tested `A[i]` against `B[j]`. The code has also tested `A[i]` against every `B[q]` with `q < j`: the invariant covers those pairs, because `q < j`. So the code has handled every pair of `A[i]`, and the invariant holds for `i + 1`. The case where `B[j]` ends first is the same with the lists exchanged. If the ends are equal, either choice is correct, as shown above. The loop stops when `i = a.length` or `j = b.length`. Then every pair has `p < i` or `q < j`, so the invariant says that the result holds every intersection. If a list is empty, the loop does not run, and the result is empty.

**Edge cases.**

```text
empty A: (none)
touching [1,3] and [3,5]: [3,3]
apart [1,2] and [3,4]: (none)
same [1,5] and [1,5]: [1,5]
```

An empty list gives an empty answer. The closed intervals `[1,3]` and `[3,5]` give the one-position interval `[3,3]`. With half-open intervals the test would be `lo < hi`.

**Complexity.** Each loop step moves `i` or `j`, so the loop runs at most `m + n` times. Time O(m + n), space O(1) besides the answer. The input lists are already sorted, so the code does not sort.

### Example 5: Car Pooling (LC 1094), a difference array is a sweep line with one cell for each position

**Statement.** [LC 1094: Car Pooling](https://leetcode.com/problems/car-pooling/). A vehicle has `capacity` seats. Each trip is `[passengers, from, to]`: the passengers board at position `from` and leave at position `to`. Return whether the load never goes above `capacity`. A passenger who leaves at `to` has left before another passenger boards at `to`, so a trip covers the positions `from` up to `to - 1`. For the trips `[3,2,7] [3,7,9] [8,3,9]` the load is at most 11, so the answer is `true` for capacity 11 and 14, and `false` for capacity 10.

**What changed.** The question is the same as in Example 3, "how many are active at each position?", with a weight for each interval (the passengers) instead of 1. The new property is in the input: the positions are small integers, and all trips are known before the code answers.

**From the sweep.** The code can use the events of Example 3: `(from, +passengers)` and `(to, -passengers)`. It sorts them by position, puts the smaller change first at equal positions (so the leaving passengers go first), and checks the load after each event. The cost is O(n log n).

**Question.** What does the sort do? It puts each change into the order of its position. If the positions are small integers, the code can put each change into a cell of an array at its position instead, which costs O(1) for each change. The array is a **difference array**: the cell `p` holds the sum of the changes at position `p`. A running sum over the cells gives the load at each position. [Pattern 04: Prefix Sum](../04-prefix-sum/README.md) derives the difference array from the question "what changes between neighbors when a range gets `v`?", and it solves [LC 1109: Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) in full. This page uses the result and shows the link to the sweep.

**Trace.**

```text
trips (passengers, from, to): [3,2,7] [3,7,9] [8,3,9]
diff = [0, 0, 3, 8, 0, 0, 0, 0, 0, -11]
load = [0, 0, 3, 11, 11, 11, 11, 11, 11, 0]
capacity 11: difference array true, sorted events true
capacity 10: difference array false, sorted events false
capacity 14: difference array true, sorted events true
```

The three trips write `+3` and `-3` at positions 2 and 7, `+3` and `-3` at positions 7 and 9, and `+8` and `-8` at positions 3 and 9. The cell 7 holds `-3 + 3 = 0`: the first trip ends at 7 and the second begins at 7. The cell 9 holds `-3 - 8 = -11`. The running sum is the load. The load is 11 from position 3 to position 8.

**Code: the difference array and the sorted events.**

```java
// Each trip is {passengers, from, to}. The passengers leave at `to`, before anyone boards
// there.
static boolean carPoolingByDifference(int[][] trips, int capacity) {
    int last = 0;
    for (int[] t : trips) last = Math.max(last, t[2]);
    int[] diff = new int[last + 1];             // one cell for each position 0 .. last
    for (int[] t : trips) {
        diff[t[1]] += t[0];                     // the passengers board at `from`
        diff[t[2]] -= t[0];                     // and leave at `to`
    }
    int load = 0;
    for (int change : diff) {
        load += change;                         // prefix sum: the load at this position
        if (load > capacity) return false;
    }
    return true;
}
```

```java
static boolean carPoolingByEvents(int[][] trips, int capacity) {
    int[][] events = new int[trips.length * 2][];
    int k = 0;
    for (int[] t : trips) {
        events[k++] = new int[]{t[1], +t[0]};   // boarding
        events[k++] = new int[]{t[2], -t[0]};   // leaving
    }
    // At equal positions the smaller change comes first, so the leaving passengers go first.
    Arrays.sort(events, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
            : Integer.compare(a[1], b[1]));
    int load = 0;
    for (int[] event : events) {
        load += event[1];
        if (load > capacity) return false;
    }
    return true;
}
```

The two methods give the same answer on the same input: in the trace, both give `true` for capacity 11 and for capacity 14. The difference array replaces the sort by a placement into cells.

**The tie, in both forms.** The change at `to` is `-passengers`. In the events, the smaller change sorts first, so the leaving passengers go before the boarding passengers at the same position. In the array, the cell holds the sum of both changes, so the order does not matter. The program shows what happens when the code writes the departure one cell too late (`to + 1`). Take the trips `[3,1,5] [3,5,7]` with capacity 3: the first trip ends at 5 and the second begins at 5.

```text
trips [3,1,5] [3,5,7], capacity 3: leave at to -> true, leave at to + 1 -> false
```

**Which form to choose.**

| Form | Time | Memory | Use it when |
|---|---|---|---|
| Difference array | O(n + C) for `C` positions | One cell for each position from 0 to the largest `to` | The largest position is small enough to hold one `int` for each position |
| Sorted events | O(n log n) | O(n) | The positions are large or sparse |

For a largest position of 1,000,000,000, an `int` array of that length uses 4,000,000,000 bytes, which is about 4 GB. The sorted events use two cells for each trip. The decision depends on the largest position and on the number of trips.

**Edge cases.**

```text
no trips: true, one trip of 4 with capacity 4: true, capacity 3: false
```

An empty list of trips does not exceed any capacity. One trip of 4 passengers fits in capacity 4 and does not fit in capacity 3.

### Example 6: My Calendar (LC 729, 731, 732), the requests arrive one at a time

**Statement of LC 729.** [LC 729: My Calendar I](https://leetcode.com/problems/my-calendar-i/). Build a calendar class with a method `book(start, end)`. The method adds the half-open interval `[start, end)` and returns `true` if it does not overlap any interval that is already booked. Otherwise it adds nothing and returns `false`. The calendar answers each request before the next request arrives.

**What changed.** The code cannot sort all requests first, because it does not know the later requests. The code must keep the booked intervals in a structure that finds the intervals near a new request in O(log n) time.

**From the brute force.** Keep the booked intervals in a list. For each request, test the new interval against every booked interval with the overlap test of the Motivation. A request costs O(n), and `n` requests cost O(n^2).

**Question: which booked intervals can overlap the new interval `[s, e)`?** The booked intervals do not overlap each other, so sorted by start they are also sorted by end. Look at two groups.

- *Booked intervals that start before `s`.* The one with the largest start, `before`, also has the largest end of this group. Any interval of the group overlaps the new interval only if its end is greater than `s`. If any interval of the group does that, `before` does. So the test is `before.end > s`.
- *Booked intervals that start at `s` or later.* The one with the smallest start, `after`, is the only one that the new interval can reach first. The new interval overlaps an interval of this group only if its start is below `e`. If any interval of the group does that, `after` does. So the test is `after.start < e`.

The code needs two lookups in a sorted map: the largest key at most `s`, and the smallest key at least `s`. A `TreeMap` does both in O(log n). The program checks each `TreeMap` method that this page uses, on the map `{10=20, 30=40, 50=60}` (start to end):

```java
import java.util.TreeMap;

public class TreeMapMethods {
    public static void main(String[] args) {
        TreeMap<Integer, Integer> booked = new TreeMap<>();   // start -> end
        booked.put(10, 20);
        booked.put(30, 40);
        booked.put(50, 60);
        System.out.println("map: " + booked);
        System.out.println("floorKey(30)      = " + booked.floorKey(30) + "   largest key <= 30");
        System.out.println("floorKey(35)      = " + booked.floorKey(35) + "   largest key <= 35");
        System.out.println("floorKey(5)       = " + booked.floorKey(5) + "  no key <= 5");
        System.out.println("ceilingKey(30)    = " + booked.ceilingKey(30)
                + "   smallest key >= 30");
        System.out.println("ceilingKey(31)    = " + booked.ceilingKey(31)
                + "   smallest key >= 31");
        System.out.println("ceilingKey(61)    = " + booked.ceilingKey(61) + "  no key >= 61");
        System.out.println("lowerKey(30)      = " + booked.lowerKey(30) + "   largest key < 30");
        System.out.println("higherKey(30)     = " + booked.higherKey(30) + "   smallest key > 30");
        System.out.println("floorEntry(35)    = " + booked.floorEntry(35));
        System.out.println("ceilingEntry(31)  = " + booked.ceilingEntry(31));
        System.out.println("firstKey()        = " + booked.firstKey());
        System.out.println("lastKey()         = " + booked.lastKey());

        TreeMap<Integer, Integer> counts = new TreeMap<>();   // time -> change in the active count
        counts.merge(5, 1, Integer::sum);
        counts.merge(9, -1, Integer::sum);
        counts.merge(9, 1, Integer::sum);
        System.out.println("merge at one time: " + counts
                + "   the change +1 and -1 at time 9 add to 0");
        System.out.println("values in key order: " + counts.values());
    }
}
```

<!-- expected-output -->
```text
map: {10=20, 30=40, 50=60}
floorKey(30)      = 30   largest key <= 30
floorKey(35)      = 30   largest key <= 35
floorKey(5)       = null  no key <= 5
ceilingKey(30)    = 30   smallest key >= 30
ceilingKey(31)    = 50   smallest key >= 31
ceilingKey(61)    = null  no key >= 61
lowerKey(30)      = 10   largest key < 30
higherKey(30)     = 50   smallest key > 30
floorEntry(35)    = 30=40
ceilingEntry(31)  = 50=60
firstKey()        = 10
lastKey()         = 50
merge at one time: {5=1, 9=0}   the change +1 and -1 at time 9 add to 0
values in key order: [1, 0]
```

`floorKey(x)` returns the largest key that is at most `x`, and `null` if no such key exists. `ceilingKey(x)` returns the smallest key that is at least `x`, and `null` if none exists. `lowerKey` and `higherKey` are the strict versions. `floorEntry` and `ceilingEntry` return the key and its value together. When the key `30` is in the map, `floorKey(30)` and `ceilingKey(30)` both return it.

The method `merge(key, 1, Integer::sum)` adds `1` to the value of `key`, or stores `1` if the key is new. The last two output lines show that a `+1` and a `-1` at the same time become one entry with the value 0. LC 731 uses this.

**The counterexample for one lookup.** The code that tests only `before` misses a booked interval that starts inside the new one. The program books `[5,8)` and then `[3,6)`:

```text
book [5,8]: wrong true, right true
book [3,6]: wrong true, right false
```

The booking `[3,6)` overlaps `[5,8)` on the position 5. `floorEntry(3)` is `null`, because no booked interval starts at 3 or earlier, so the floor-only code accepts the booking. The ceiling lookup finds `5=8`, and `5 < 6` rejects it.

**Trace.** The program prints the two lookups before each request, and the result:

```text
book [10,20]: floor null, ceiling null -> true
book [15,25]: floor 10=20, ceiling null -> false
book [20,30]: floor 10=20, ceiling null -> true
book [5,10]: floor null, ceiling 10=20 -> true
book [8,12]: floor 5=10, ceiling 10=20 -> false
book [30,31]: floor 20=30, ceiling null -> true
```

The request `[15,25)` fails because the floor entry `10=20` has `20 > 15`. The request `[20,30)` succeeds: `20 > 20` is false, so the half-open interval `[10,20)` does not reach it. The request `[8,12)` fails because the ceiling entry `10=20` has `10 < 12` and the floor entry `5=10` has `10 > 8`.

**Code.**

```java
static class MyCalendar {
    // start -> end, half-open
    private final TreeMap<Integer, Integer> booked = new TreeMap<>();

    boolean book(int start, int end) {
        // largest start <= start
        Map.Entry<Integer, Integer> before = booked.floorEntry(start);
        // smallest start >= start
        Map.Entry<Integer, Integer> after = booked.ceilingEntry(start);
        // it runs into the new one
        if (before != null && before.getValue() > start) return false;
        // the new one runs into it
        if (after != null && after.getKey() < end) return false;
        booked.put(start, end);
        return true;
    }
}
```

**Complexity.** A `TreeMap` guarantees log(n) time for `get`, `put`, and `remove` (JDK 21 javadoc). In the JDK 21 source, `floorEntry` and `ceilingEntry` follow one path from the root of the tree, and the path has O(log n) nodes in a balanced tree. So `book` costs O(log n), and `n` requests cost O(n log n). Space O(n).

**LC 731: at most two bookings at one position.** [LC 731: My Calendar II](https://leetcode.com/problems/my-calendar-ii/). The method `book(start, end)` accepts the interval if it creates no position that three bookings cover. The two-neighbor test is not enough, because the booked intervals may now overlap each other, so they are not sorted by end. The code goes back to the events of Example 3. It keeps a `TreeMap` from a time to the change in the active count. A booking adds `+1` at `start` and `-1` at `end`. The code sweeps the map in key order. If the counter reaches 3, the code removes the booking (it adds the opposite changes) and returns `false`.

```java
static class MyCalendarTwo {
    // time -> change in active count
    private final TreeMap<Integer, Integer> delta = new TreeMap<>();

    boolean book(int start, int end) {
        delta.merge(start, 1, Integer::sum);
        delta.merge(end, -1, Integer::sum);
        int active = 0;
        // the values come in the order of the times
        for (int change : delta.values()) {
            active += change;
            // a triple booking: undo this booking
            if (active >= 3) {
                delta.merge(start, -1, Integer::sum);
                delta.merge(end, 1, Integer::sum);
                return false;
            }
        }
        return true;
    }
}
```

The `TreeMap` keeps the events sorted, so the code needs no sort. The map holds one value for each time, which is the sum of the changes at that time. The change `-1` of one booking and the change `+1` of another booking at the same time add up in one entry. So the counter takes the end before the start at the same time, which is the half-open tie order of Example 3, without a comparator.

```text
book [10,20] -> true
book [50,60] -> true
book [10,40] -> true
book [5,15] -> false
book [5,10] -> true
book [25,55] -> true
```

The request `[5,15)` fails because positions 10 to 14 are already covered twice (by `[10,20)` and `[10,40)`). The request `[5,10)` succeeds: it touches `[10,20)` and `[10,40)` only at the position 10, which the half-open intervals do not hold. Each booking sweeps the whole map, so one request costs O(n) and `n` requests cost O(n^2). A segment tree over the positions (a dynamic one, or one on compressed coordinates) supports a range add and a range maximum in O(log C) for `C` positions, so it avoids the sweep of the whole map. [Pattern 21: Segment Tree and Fenwick Tree](../21-segment-tree-and-fenwick-tree/README.md) covers segment trees.

**LC 732: the largest overlap.** [LC 732: My Calendar III](https://leetcode.com/problems/my-calendar-iii/) uses the same map of changes and asks for the largest number of bookings that cover one position. It is practice problem P10, with hints and a full solution.

**The next structure: disjoint ranges.** [LC 715: Range Module](https://leetcode.com/problems/range-module/) and [LC 352: Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/) keep a set of disjoint intervals in a `TreeMap` from start to end. The code finds the neighbors of a new range with `floorEntry` and `ceilingEntry`, as in LC 729. Then it combines or trims the intervals that the range overlaps, with the merge idea of Example 1. This page does not solve these two problems.

### Example 7: Meeting Rooms (LC 252, premium), the merge as a yes or no test

**Statement of LC 252.** [LC 252: Meeting Rooms](https://leetcode.com/problems/meeting-rooms/) (premium). The input is a list of half-open meeting intervals. Return `true` if one person can attend all of them, which means that no two meetings overlap. For `[0,30) [5,10) [15,20)` the answer is `false`. For `[7,10) [2,4)` the answer is `true`.

**What changed.** The merge built the union. This problem asks only whether any two intervals overlap. It is a yes or no answer.

**From the brute force.** Test every pair: O(n^2) tests.

**Question.** After the sort by start, which pairs can overlap if any pair does? Suppose the meeting `i` overlaps a later meeting `j`. Then `start_j < end_i`. The meeting `i + 1` starts at or before `start_j` (the sort), so `start_{i+1} <= start_j < end_i`, and the meetings `i` and `i + 1` overlap. So if any pair overlaps, some pair of neighbors overlaps, and the code tests only the neighbors.

```java
static boolean canAttendAll(int[][] meetings) {
    int[][] sorted = meetings.clone();
    Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
    for (int i = 1; i < sorted.length; i++) {
        // < : a meeting that starts at the end is fine
        if (sorted[i][0] < sorted[i - 1][1]) return false;
    }
    return true;
}
```

The test is `<`: a meeting that starts at the end of the previous one does not overlap it. The program prints these results:

```text
[0,30] [5,10] [15,20]: false
[7,10] [2,4]: true
[1,3] [3,5]: true
[1,10] [2,3] [11,12]: false
empty: true, one meeting: true
```

The input `[1,3) [3,5)` gives `true` (touching meetings are compatible). The input `[1,10) [2,3) [11,12)` gives `false`, because the nested meeting `[2,3)` starts before `[1,10)` ends. The neighbor test finds this pair. An empty list and a single meeting give `true`. The cost is O(n log n) for the sort and O(n) for the scan.

The same sort-and-scan idea also gives the free time of several employees: [LC 759: Employee Free Time](https://leetcode.com/problems/employee-free-time/) (premium) asks for the gaps that the merge leaves open. It is practice problem P11, with hints and a full solution.

### Example 8: The Skyline Problem (LC 218), a sweep with a height structure

**Statement.** [LC 218: The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/). Each building is a rectangle on the ground, given as `[left, right, height]`. The building covers the positions from `left` up to, but not including, `right`. This page assumes `left < right` and `height > 0`. The ground has height 0. The skyline is the outline of the union of all buildings, seen from far away. Return its **key points**: the points `[x, h]` where the height of the outline changes to `h` at position `x`, from left to right. The last key point has height 0. For the buildings `[1,5,3] [2,4,5] [3,8,4] [9,11,2]` the key points are `[1,3] [2,5] [4,4] [8,0] [9,2] [11,0]`.

**What changed.** Each interval now carries a value (the height), and the answer at a position is the largest value among the active intervals. The summary of the sweep must support three operations: add a height, remove a height, and read the largest height.

**From the brute force.** The height of the outline can change only at the left edge or the right edge of a building. For each edge, find the tallest building that covers it, and write a key point when the height differs from the previous one. Each edge needs a test of all buildings, so the cost is O(n^2).

**Question 1: where can the outline change?** Only at the edges. So the code turns each building into two events, `(left, +height)` and `(right, -height)`, and sorts the events by position, as in Example 3.

**Question 2: what must the code know at a position?** The tallest active building. The set of active buildings gains a building at a left edge and loses a building at a right edge. The code needs a structure that adds, removes any element, and reads the maximum. The options:

| Structure | Add | Remove an arbitrary height | Read the maximum | Problem |
|---|---|---|---|---|
| Sorted list | O(n) | O(n) | O(1) | Each event costs O(n), so the total is O(n^2) |
| `PriorityQueue` (max heap) with `remove(Object)` | O(log n) | O(n) | O(1) | `remove(Object)` scans the heap |
| Max heap with lazy deletion | O(log n) | O(log n) later, when the element is at the top | O(1) | Correct, and more code: a map of pending removals ([Pattern 16](../16-heap-priority-queue/README.md), Worked Example 7) |
| `TreeMap` from height to count | O(log n) | O(log n) | O(log n) with `lastKey` | The count lets two buildings of the same height coexist |

This page uses the `TreeMap`. A set of heights is not enough: two buildings can have the same height, and removing one of them must keep the height in the structure while the other one is active.

**Question 3: what if several events have the same position?** The code must apply all events at that position before it reads the height. If it reads after each event, it can report a height that exists for no position at all.

**Question 4: when is a position a key point?** When the tallest active height differs from the height of the outline before it. If it is the same, the code writes nothing.

**Trace.**

```text
buildings (left, right, height): [1,5,3] [2,4,5] [3,8,4] [9,11,2]
x = 1: add 3; active {3=1}, tallest 3 -> key point [1,3]
x = 2: add 5; active {3=1, 5=1}, tallest 5 -> key point [2,5]
x = 3: add 4; active {3=1, 4=1, 5=1}, tallest 5 -> same height, no key point
x = 4: remove 5; active {3=1, 4=1}, tallest 4 -> key point [4,4]
x = 5: remove 3; active {4=1}, tallest 4 -> same height, no key point
x = 8: remove 4; active {}, tallest 0 -> key point [8,0]
x = 9: add 2; active {2=1}, tallest 2 -> key point [9,2]
x = 11: remove 2; active {}, tallest 0 -> key point [11,0]
key points: [1,3] [2,5] [4,4] [8,0] [9,2] [11,0]
```

At `x = 3`, the building of height 4 starts, but the tallest active building still has height 5, so the code writes nothing. At `x = 4`, the building of height 5 ends, and the tallest height drops to 4. At `x = 5`, the building of height 3 ends, and the tallest height is still 4. At `x = 8`, the last active building ends, and the height drops to 0.

**Code.**

```java
// Each building is {left, right, height}, with left < right and height > 0. The building covers
// [left, right).
static List<int[]> skyline(int[][] buildings) {
    int[][] events = new int[buildings.length * 2][];
    int k = 0;
    for (int[] b : buildings) {
        events[k++] = new int[]{b[0], b[2]};        // the building starts: add its height
        events[k++] = new int[]{b[1], -b[2]};       // the building ends: remove its height
    }
    Arrays.sort(events, (a, b) -> Integer.compare(a[0], b[0]));
    // height -> number of active buildings
    TreeMap<Integer, Integer> active = new TreeMap<>();
    List<int[]> result = new ArrayList<>();
    int previous = 0;                               // the height of the skyline so far
    int i = 0;
    while (i < events.length) {
        int x = events[i][0];
        // apply every event at x before reading
        while (i < events.length && events[i][0] == x) {
            int h = events[i][1];
            if (h > 0) {
                active.merge(h, 1, Integer::sum);
            } else if (active.merge(-h, -1, Integer::sum) == 0) {
                active.remove(-h);                  // the last building of this height is gone
            }
            i++;
        }
        int current = active.isEmpty() ? 0 : active.lastKey();   // the tallest active building
        if (current != previous) {                  // a key point only where the height changes
            result.add(new int[]{x, current});
            previous = current;
        }
    }
    return result;
}
```

The invariant: after the code has applied all events at a position `x`, the map `active` holds one count for each height of the buildings with `left <= x < right`, and `previous` is the height of the outline at `x`. A step to the next position applies the events that add and remove buildings, so the map holds the buildings of that position. The outline at `x` is the largest key, or 0 if the map is empty. The code writes a point only where this height differs from `previous`, so each key point is a position where the outline changes. Before the first position, the map is empty and `previous` is 0, so the first building gets a key point at its left edge.

The inner loop applies every event at the position `x`. The variable `previous` holds the height of the outline so far, and it starts at 0 (the ground). `active.merge(-h, -1, Integer::sum)` subtracts 1 from the count and returns the new count. If the count is 0, the code removes the height from the map.

**The three defects, each with a failing input.**

```text
buildings [1,3,5] [3,6,2]
apply all events at x: [1,5] [3,2] [6,0]
read after each event: [1,5] [3,0] [3,2] [6,0]
buildings [0,2,3] [2,5,3]
compare with the previous height: [0,3] [5,0]
no comparison:                    [0,3] [2,3] [5,0]
buildings [1,4,3] [2,6,3]
count of each height: [1,3] [6,0]
set of heights:       [1,3] [4,0]
```

- *Read after each event.* For `[1,3,5] [3,6,2]`, the building of height 5 ends at 3, and the building of height 2 starts at 3. If the code reads the height after the end event, it writes `[3,0]`, and then it writes `[3,2]`. The first point is wrong, because the outline never has height 0 at position 3. The correct answer has `[3,2]` only.
- *No comparison with the previous height.* For `[0,2,3] [2,5,3]`, two buildings of the same height touch at 2. The outline stays at height 3, so the correct answer is `[0,3] [5,0]`. Without the comparison, the code writes `[2,3]`.
- *A set of heights in place of a count.* For `[1,4,3] [2,6,3]`, two buildings have height 3. When the first one ends at 4, the code removes the height 3 from the set, but the second building is still active. The code writes `[4,0]`. The correct answer is `[1,3] [6,0]`.

**Edge cases.**

```text
no buildings: (none)
one building [1,2,1]: [1,1] [2,0]
identical [1,3,4] twice: [1,4] [3,0]
nested [1,10,2] [3,5,9]: [1,2] [3,9] [5,2] [10,0]
```

With no buildings, the answer is empty. A single building gives its start and its end. Two identical buildings give the same answer as one. In the nested case `[1,10,2] [3,5,9]`, the tall building hides the low one from 3 to 5, and the low one shows again after 5.

**Complexity.** The sort of `2n` events costs O(n log n). Each event makes one operation on the `TreeMap` of at most `n` heights, which costs O(log n), so the loop costs O(n log n). Space O(n).

## Implementation

Each method in the Worked Examples is one template. The comments in the code mark the lines that a new problem changes. The table lists them.

| Template | Methods | What a new problem changes |
|---|---|---|
| Sort by start, keep one current interval | `mergeIntervals`, `employeeFreeTime`, `canAttendAll`, `countNotCovered` | The test (`<=` merges touching intervals, `<` does not), and what the code does at a gap (close the interval, write a gap, return `false`) |
| Three phases on a sorted, disjoint list | `insertInterval` | The two tests of phase 1 and phase 2 (`<` and `<=` for closed intervals that touch) |
| Sort by end, keep the interval that fits | `maxNonOverlapping`, `minPoints`, `longestPairChain` | The test against the last end (`>=` for half-open intervals, `>` for closed intervals and for chains) |
| Sorted events and a counter | `maxActiveBySweep`, `minGroups`, `carPoolingByEvents` | The tie order of the events, and the change that an event carries (`+-1` or `+-passengers`) |
| Min heap of end times | `maxActiveByHeap` | The test for "the room is free" (`<=` for half-open intervals) |
| Two pointers on two sorted lists | `intervalIntersections` | The test for an intersection (`lo <= hi` for closed intervals, `lo < hi` for half-open intervals) |
| Difference array | `carPoolingByDifference`, `maximumPopulationYear` | The cell of the second write (`to` for passengers who have left at `to`), and the size of the array |
| `TreeMap` of booked intervals | `MyCalendar` | Which neighbors the code reads (`floorEntry` and `ceilingEntry`), and the test against each |
| `TreeMap` of changes in time | `MyCalendarTwo`, `MyCalendarThree` | The limit on the counter (3 in LC 731), and whether the code returns a boolean or the maximum |
| Sorted events and a `TreeMap` of heights | `skyline` | What the map holds (a count for each height), and when the code writes a point |

Notes on the lines that readers get wrong:

- Every sort uses `Integer.compare(a[k], b[k])`. The comparator `a[k] - b[k]` overflows for values that are far apart (Common Mistakes, mistake 1).
- `intervals.clone()` copies the array of rows and not the rows. The code that writes into a row (`current[1] = ...`) must clone the row first, or it changes the input.
- In the merge, the check for an empty input comes before `sorted[0]`.
- In the selection, `lastEnd` is a `long` that starts at `Long.MIN_VALUE`, which is below every `int` start. With `int lastEnd = Integer.MIN_VALUE` and the test `>`, an interval that starts at `Integer.MIN_VALUE` would be skipped. The `long` start value also removes the flag for "no interval kept yet".
- In the sweep, the sort comparator orders the events at the same time. The order is part of the answer (Example 3).
- In the difference array, the departure is written at `to`, and the loop reads every cell, including the last one.
- In the skyline, the inner `while` applies all events at one position before the code reads the height.

### Java facts used on this page

| Fact | Source |
|---|---|
| `Arrays.sort(T[], Comparator)` is stable. Its implementation is TimSort, an adaptive iterative merge sort that "requires far fewer than n lg(n) comparisons when the input array is partially sorted" and, for a nearly sorted array, "approximately n comparisons". On a sorted array of 1,000 elements, a counting comparator saw 999 comparisons (Common Mistakes, mistake 7). The page did not measure `Arrays.sort(int[])` | Javadoc, implementation note, and a run |
| A comparator returns a negative number, zero, or a positive number. `Integer.compare(x, y)` does this for two `int` values and cannot overflow. The difference `x - y` can overflow | Javadoc of `Comparator` and `Integer.compare`, and a run (mistake 1) |
| `TreeMap` guarantees log(n) time for `containsKey`, `get`, `put`, and `remove` | Javadoc |
| `floorKey(x)` returns the greatest key at most `x`. `ceilingKey(x)` returns the least key at least `x`. `lowerKey` and `higherKey` are strict. All four return `null` if no key matches. `floorEntry` and `ceilingEntry` return the key and the value | Javadoc, and the program of Example 6 |
| `floorEntry` and `ceilingEntry` follow one path from the root of the tree | `TreeMap.getFloorEntry` and `getCeilingEntry` in the JDK 21 source |
| `map.merge(key, v, f)` stores `v` if the key has no value, and stores `f(old, v)` otherwise. If `f` returns `null`, the map removes the key. A result of 0 stays in the map | Javadoc of `Map.merge`, and the program of Example 6 |
| `PriorityQueue.remove(Object)` takes linear time | Javadoc of `PriorityQueue` |

### The full program

The program `IntervalTraces` prints the traces of this page: the merge, the insert, the selection with both sort keys, the sweep and the heap for meeting rooms with both tie orders, the intersection with both pointer rules, the load with both forms of the departure, the three calendars, the free time, the skyline with its three wrong versions, and the ladder problems. It also prints the edge cases of each solution: an empty input, one element, equal elements, and the answer at the first or the last position. The text above shows every method that the examples need. So the program is in a collapsed block. The runner compiles each block alone, so the program repeats the methods of the examples, and it adds the traced versions and the wrong versions that print their steps.

??? example "The full program: IntervalTraces"

    ```java
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.Comparator;
    import java.util.List;
    import java.util.Map;
    import java.util.PriorityQueue;
    import java.util.TreeMap;

    public class IntervalTraces {

        public static void main(String[] args) {
            mergeSection();
            insertSection();
            selectionSection();
            roomsSection();
            intersectionSection();
            loadSection();
            calendarSection();
            freeTimeSection();
            skylineSection();
            ladderSection();
        }

        // ---------------------------------------------------------------- helpers

        static String fmt1(int[] iv) {
            return "[" + iv[0] + "," + iv[1] + "]";
        }

        static String fmt(int[][] intervals) {
            StringBuilder sb = new StringBuilder();
            for (int[] iv : intervals) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(fmt1(iv));
            }
            return sb.length() == 0 ? "(none)" : sb.toString();
        }

        static String fmt(List<int[]> intervals) {
            return fmt(intervals.toArray(new int[0][]));
        }

        // ------------------------------------------------------- 1. merge (LC 56)

        static int[][] mergeIntervals(int[][] intervals) {
            if (intervals.length == 0) return new int[0][];            // nothing to merge
            int[][] sorted = intervals.clone();                        // keep the caller's order
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0])); // sort by start
            List<int[]> merged = new ArrayList<>();
            // the interval that is being built
            int[] current = sorted[0].clone();
            for (int i = 1; i < sorted.length; i++) {
                if (sorted[i][0] <= current[1]) {                      // <= : touching intervals merge
                    // max: the next one can be nested
                    current[1] = Math.max(current[1], sorted[i][1]);
                } else {
                    // a gap: nothing later can reach it
                    merged.add(current);
                    current = sorted[i].clone();
                }
            }
            merged.add(current);
            return merged.toArray(new int[0][]);
        }

        static int[][] mergeTraced(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
            System.out.println("sorted by start: " + fmt(sorted));
            List<int[]> merged = new ArrayList<>();
            int[] current = sorted[0].clone();
            System.out.println("take " + fmt1(current) + " as the current interval");
            for (int i = 1; i < sorted.length; i++) {
                int s = sorted[i][0], e = sorted[i][1];
                String name = fmt1(sorted[i]);
                if (s <= current[1]) {
                    int newEnd = Math.max(current[1], e);
                    System.out.println(name + ": start " + s + " <= current end " + current[1]
                            + " -> extend, end = max(" + current[1] + ", " + e + ") = " + newEnd);
                    current[1] = newEnd;
                } else {
                    System.out.println(name + ": start " + s + " > current end " + current[1]
                            + " -> close " + fmt1(current) + ", current = " + name);
                    merged.add(current);
                    current = sorted[i].clone();
                }
            }
            merged.add(current);
            System.out.println("close " + fmt1(current));
            return merged.toArray(new int[0][]);
        }

        // The wrong versions of Common Mistakes, kept in the program so that the page shows real
        // output.
        static int[][] mergeWithoutMax(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
            List<int[]> merged = new ArrayList<>();
            int[] current = sorted[0].clone();
            for (int i = 1; i < sorted.length; i++) {
                if (sorted[i][0] <= current[1]) {
                    current[1] = sorted[i][1];                         // WRONG: no max
                } else {
                    merged.add(current);
                    current = sorted[i].clone();
                }
            }
            merged.add(current);
            return merged.toArray(new int[0][]);
        }

        static int[][] mergeWithoutSort(int[][] intervals) {
            int[][] sorted = intervals.clone();                        // WRONG: no sort
            List<int[]> merged = new ArrayList<>();
            int[] current = sorted[0].clone();
            for (int i = 1; i < sorted.length; i++) {
                if (sorted[i][0] <= current[1]) {
                    current[1] = Math.max(current[1], sorted[i][1]);
                } else {
                    merged.add(current);
                    current = sorted[i].clone();
                }
            }
            merged.add(current);
            return merged.toArray(new int[0][]);
        }

        static void mergeSection() {
            System.out.println("== merge trace ==");
            int[][] anchor = {{8, 10}, {1, 3}, {2, 6}, {15, 18}, {17, 20}};
            System.out.println("input: " + fmt(anchor));
            int[][] result = mergeTraced(anchor);
            System.out.println("result: " + fmt(result));

            System.out.println("== merge edge cases ==");
            int[][][] cases = {
                {},
                {{4, 7}},
                {{1, 10}, {2, 3}, {4, 5}},
                {{1, 4}, {4, 5}},
                {{1, 2}, {3, 4}},
                {{2, 5}, {2, 5}, {2, 5}},
                {{5, 6}, {1, 2}},
            };
            for (int[][] c : cases) {
                System.out.println(fmt(c) + " -> " + fmt(mergeIntervals(c)));
            }

            System.out.println("== merge wrong versions ==");
            int[][] nested = {{1, 10}, {2, 3}, {4, 5}};
            System.out.println("without max on " + fmt(nested) + ": " + fmt(mergeWithoutMax(nested)));
            System.out.println("with max on    " + fmt(nested) + ": " + fmt(mergeIntervals(nested)));
            System.out.println("without sort on " + fmt(anchor) + ": " + fmt(mergeWithoutSort(anchor)));
            System.out.println("with sort on    " + fmt(anchor) + ": " + fmt(mergeIntervals(anchor)));
        }

        // ---------------------------------------------------- 2. insert (LC 57)

        static int[][] insertInterval(int[][] intervals, int[] added) {
            List<int[]> result = new ArrayList<>();
            int start = added[0], end = added[1];
            int i = 0;
            // Phase 1: intervals that end before the new one starts. They stay as they are.
            while (i < intervals.length && intervals[i][1] < start) {
                result.add(intervals[i++]);
            }
            // Phase 2: intervals that overlap the new one. Each one widens the new one.
            while (i < intervals.length && intervals[i][0] <= end) {
                start = Math.min(start, intervals[i][0]);
                end = Math.max(end, intervals[i][1]);
                i++;
            }
            result.add(new int[]{start, end});
            // Phase 3: intervals that start after the new one ends. They stay as they are.
            while (i < intervals.length) {
                result.add(intervals[i++]);
            }
            return result.toArray(new int[0][]);
        }

        static int[][] insertTraced(int[][] intervals, int[] added) {
            List<int[]> result = new ArrayList<>();
            int start = added[0], end = added[1];
            int i = 0;
            System.out.println("new interval " + fmt1(added));
            while (i < intervals.length && intervals[i][1] < start) {
                System.out.println("phase 1: " + fmt1(intervals[i]) + " ends at " + intervals[i][1]
                        + " < " + start + " -> copy");
                result.add(intervals[i++]);
            }
            while (i < intervals.length && intervals[i][0] <= end) {
                int newStart = Math.min(start, intervals[i][0]);
                int newEnd = Math.max(end, intervals[i][1]);
                System.out.println("phase 2: " + fmt1(intervals[i]) + " starts at " + intervals[i][0]
                        + " <= " + end + " -> new interval becomes [" + newStart + "," + newEnd + "]");
                start = newStart;
                end = newEnd;
                i++;
            }
            System.out.println("add the new interval [" + start + "," + end + "]");
            result.add(new int[]{start, end});
            while (i < intervals.length) {
                System.out.println("phase 3: " + fmt1(intervals[i])
                        + " starts after the new interval -> copy");
                result.add(intervals[i++]);
            }
            return result.toArray(new int[0][]);
        }

        static void insertSection() {
            System.out.println("== insert trace ==");
            int[][] list = {{1, 2}, {3, 5}, {6, 7}, {8, 10}, {12, 16}};
            System.out.println("input: " + fmt(list));
            System.out.println("result: " + fmt(insertTraced(list, new int[]{4, 8})));

            System.out.println("== insert edge cases ==");
            System.out.println("empty list, add [5,6]: " + fmt(insertInterval(new int[0][], new int[]{5,
                    6})));
            System.out.println("add before all, [0,0]: " + fmt(insertInterval(list, new int[]{0, 0})));
            System.out.println("add after all, [20,21]: " + fmt(insertInterval(list, new int[]{20,
                    21})));
            System.out.println("add inside a gap, [10,11]: " + fmt(insertInterval(list, new int[]{10,
                    11})));
            System.out.println("add touching the first, [2,3]: " + fmt(insertInterval(list, new int[]{2,
                    3})));
            System.out.println("add covering all, [0,20]: " + fmt(insertInterval(list, new int[]{0,
                    20})));
        }

        // ------------------------------------- 3. selection (LC 435, LC 452, LC 646)

        // Half-open intervals. Returns the largest number of intervals that do not overlap.
        static int maxNonOverlapping(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));   // sort by END
            int kept = 0;
            long lastEnd = Long.MIN_VALUE;            // the end of the last interval kept
            for (int[] iv : sorted) {
                if (iv[0] >= lastEnd) {               // >= : [1,2) and [2,3) do not overlap
                    kept++;
                    lastEnd = iv[1];
                }
            }
            return kept;
        }

        // Closed intervals. Returns the fewest points that touch every interval (LC 452 shape).
        static int minPoints(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));   // sort by END
            int points = 0;
            long lastPoint = Long.MIN_VALUE;
            for (int[] iv : sorted) {
                // > : a closed interval that starts AT the point is hit
                if (iv[0] > lastPoint) {
                    points++;
                    lastPoint = iv[1];                // the point goes at the earliest end
                }
            }
            return points;
        }

        static int maxNonOverlappingSortedByStart(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));   // WRONG key
            int kept = 0;
            long lastEnd = Long.MIN_VALUE;
            for (int[] iv : sorted) {
                if (iv[0] >= lastEnd) {
                    kept++;
                    lastEnd = iv[1];
                }
            }
            return kept;
        }

        static int maxNonOverlappingWithGreaterThan(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));
            int kept = 0;
            long lastEnd = Long.MIN_VALUE;
            for (int[] iv : sorted) {
                if (iv[0] > lastEnd) {                // WRONG for half-open intervals
                    kept++;
                    lastEnd = iv[1];
                }
            }
            return kept;
        }

        static int maxNonOverlappingTraced(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));
            System.out.println("sorted by end: " + fmt(sorted));
            int kept = 0;
            long lastEnd = Long.MIN_VALUE;
            for (int[] iv : sorted) {
                String last = lastEnd == Long.MIN_VALUE ? "none" : String.valueOf(lastEnd);
                if (iv[0] >= lastEnd) {
                    kept++;
                    System.out.println(fmt1(iv) + ": start " + iv[0] + " >= last end " + last
                            + " -> keep, kept = " + kept);
                    lastEnd = iv[1];
                } else {
                    System.out.println(fmt1(iv) + ": start " + iv[0] + " <  last end " + last
                            + " -> skip");
                }
            }
            return kept;
        }

        static void selectionSection() {
            System.out.println("== selection trace ==");
            int[][] input = {{0, 5}, {1, 2}, {2, 4}, {4, 6}, {5, 9}, {8, 10}};
            System.out.println("input: " + fmt(input));
            int kept = maxNonOverlappingTraced(input);
            System.out.println("kept " + kept + ", removed " + (input.length - kept));

            System.out.println("== selection wrong key ==");
            System.out.println("sort by end on " + fmt(input) + ": kept " + maxNonOverlapping(input));
            System.out.println("sort by start on " + fmt(input) + ": kept "
                    + maxNonOverlappingSortedByStart(input));
            int[][] hundred = {{1, 100}, {2, 3}, {4, 5}};
            System.out.println("sort by end on " + fmt(hundred) + ": kept "
                    + maxNonOverlapping(hundred));
            System.out.println("sort by start on " + fmt(hundred) + ": kept "
                    + maxNonOverlappingSortedByStart(hundred));

            System.out.println("== selection touching ==");
            int[][] touching = {{1, 2}, {2, 3}};
            System.out.println("half-open " + fmt(touching) + ": kept " + maxNonOverlapping(touching)
                    + ", with > instead of >=: kept " + maxNonOverlappingWithGreaterThan(touching));
            System.out.println("closed    " + fmt(touching) + ": points " + minPoints(touching));

            System.out.println("== selection edge cases ==");
            int[][][] cases = {{}, {{3, 4}}, {{1, 2}, {1, 2}, {1, 2}}, {{1, 5}, {2, 6}, {3, 7}}};
            for (int[][] c : cases) {
                System.out.println(fmt(c) + " -> kept " + maxNonOverlapping(c) + ", points "
                        + minPoints(c));
            }
        }

        // ----------------------------------------- 4. meeting rooms II (LC 253)

        // Returns the largest number of intervals that are active at one time.
        // halfOpen = true : [s, e) intervals, an end at time t frees the room for a start at time t.
        // halfOpen = false: [s, e] intervals, a start at time t shares the point t with an end at t.
        static int maxActiveBySweep(int[][] intervals, boolean halfOpen) {
            int[][] events = new int[intervals.length * 2][];
            int k = 0;
            for (int[] iv : intervals) {
                events[k++] = new int[]{iv[0], +1};      // a start adds one active interval
                events[k++] = new int[]{iv[1], -1};      // an end removes one
            }
            // The tie order: at equal times, ends first for half-open, starts first for closed.
            Comparator<int[]> order = (a, b) -> {
                if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
                return halfOpen ? Integer.compare(a[1], b[1]) : Integer.compare(b[1], a[1]);
            };
            Arrays.sort(events, order);
            int active = 0, most = 0;
            for (int[] event : events) {
                active += event[1];
                most = Math.max(most, active);
            }
            return most;
        }

        static int maxActiveByHeap(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));   // sort by start
            // end time of each room in use
            PriorityQueue<Integer> ends = new PriorityQueue<>();
            for (int[] iv : sorted) {
                if (!ends.isEmpty() && ends.peek() <= iv[0]) {
                    ends.poll();                                          // the earliest room is free
                }
                ends.offer(iv[1]);
            }
            return ends.size();
        }

        static int maxActiveByTwoArrays(int[][] intervals) {
            int n = intervals.length;
            int[] starts = new int[n], ends = new int[n];
            for (int i = 0; i < n; i++) {
                starts[i] = intervals[i][0];
                ends[i] = intervals[i][1];
            }
            Arrays.sort(starts);
            Arrays.sort(ends);
            int active = 0, most = 0, e = 0;
            for (int s = 0; s < n; s++) {
                while (ends[e] <= starts[s]) {
                    active--;
                    e++;
                }
                active++;
                most = Math.max(most, active);
            }
            return most;
        }

        static int maxActiveBrute(int[][] intervals) {
            int most = 0;
            for (int[] probe : intervals) {
                int t = probe[0];
                int count = 0;
                for (int[] iv : intervals) {
                    if (iv[0] <= t && t < iv[1]) count++;     // half-open: active at time t
                }
                most = Math.max(most, count);
            }
            return most;
        }

        static int maxActiveSweepTraced(int[][] intervals) {
            int[][] events = new int[intervals.length * 2][];
            int k = 0;
            for (int[] iv : intervals) {
                events[k++] = new int[]{iv[0], +1};
                events[k++] = new int[]{iv[1], -1};
            }
            Arrays.sort(events, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                    : Integer.compare(a[1], b[1]));
            int active = 0, most = 0;
            for (int[] event : events) {
                active += event[1];
                most = Math.max(most, active);
                System.out.printf("time %2d  %s  active = %d  most = %d%n",
                        event[0], event[1] > 0 ? "start (+1)" : "end   (-1)", active, most);
            }
            return most;
        }

        static int maxActiveHeapTraced(int[][] intervals) {
            int[][] sorted = intervals.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
            PriorityQueue<Integer> ends = new PriorityQueue<>();
            for (int[] iv : sorted) {
                String action;
                if (!ends.isEmpty() && ends.peek() <= iv[0]) {
                    int freed = ends.poll();
                    action = "earliest end " + freed + " <= " + iv[0] + ", reuse that room";
                } else if (ends.isEmpty()) {
                    action = "no room yet, open one";
                } else {
                    action = "earliest end " + ends.peek() + " > " + iv[0] + ", open a room";
                }
                ends.offer(iv[1]);
                System.out.println(fmt1(iv) + ": " + action + "; end times now " + sortedList(ends));
            }
            return ends.size();
        }

        static List<Integer> sortedList(PriorityQueue<Integer> heap) {
            List<Integer> list = new ArrayList<>(heap);
            list.sort(null);
            return list;
        }

        static void roomsSection() {
            System.out.println("== rooms sweep trace ==");
            int[][] meetings = {{0, 30}, {5, 10}, {10, 25}, {15, 20}, {28, 35}};
            System.out.println("meetings: " + fmt(meetings));
            System.out.println("answer: " + maxActiveSweepTraced(meetings));

            System.out.println("== rooms heap trace ==");
            System.out.println("answer: " + maxActiveHeapTraced(meetings));

            System.out.println("== rooms tie order ==");
            int[][] touching = {{0, 10}, {10, 20}};
            System.out.println("half-open " + fmt(touching) + ": ends first gives "
                    + maxActiveBySweep(touching, true)
                    + ", starts first gives " + maxActiveBySweep(touching, false));
            System.out.println("closed    " + fmt(touching) + ": starts first gives "
                    + maxActiveBySweep(touching, false)
                    + ", ends first gives " + maxActiveBySweep(touching, true));

            System.out.println("== rooms agree ==");
            System.out.println("sweep " + maxActiveBySweep(meetings, true) + ", heap "
                    + maxActiveByHeap(meetings)
                    + ", two arrays " + maxActiveByTwoArrays(meetings) + ", brute force "
                            + maxActiveBrute(meetings));

            System.out.println("== rooms edge cases ==");
            int[][][] cases = {{}, {{4, 9}}, {{1, 5}, {1, 5}, {1, 5}}, {{1, 2}, {2, 3}, {3, 4}}, {{1,
                    10}, {2, 3}, {4, 5}}};
            for (int[][] c : cases) {
                System.out.println(fmt(c) + " -> sweep " + maxActiveBySweep(c, true) + ", heap "
                        + maxActiveByHeap(c));
            }
        }

        // -------------------------------------------- 5. intersections (LC 986)

        // Both lists hold closed intervals, sorted by start, and no two intervals of one list share a
        // point.
        static int[][] intervalIntersections(int[][] a, int[][] b) {
            List<int[]> result = new ArrayList<>();
            int i = 0, j = 0;
            while (i < a.length && j < b.length) {
                // the intersection starts at the later start
                int lo = Math.max(a[i][0], b[j][0]);
                int hi = Math.min(a[i][1], b[j][1]);          // and ends at the earlier end
                // <= : one shared point is an intersection
                if (lo <= hi) {
                    result.add(new int[]{lo, hi});
                }
                if (a[i][1] < b[j][1]) i++;                   // drop the interval that ends first
                else j++;
            }
            return result.toArray(new int[0][]);
        }

        static int[][] intersectionsAdvanceBySmallerStart(int[][] a, int[][] b) {
            List<int[]> result = new ArrayList<>();
            int i = 0, j = 0;
            while (i < a.length && j < b.length) {
                int lo = Math.max(a[i][0], b[j][0]);
                int hi = Math.min(a[i][1], b[j][1]);
                if (lo <= hi) result.add(new int[]{lo, hi});
                if (a[i][0] < b[j][0]) i++;                   // WRONG: advances by the start
                else j++;
            }
            return result.toArray(new int[0][]);
        }

        static int[][] intersectionsTraced(int[][] a, int[][] b) {
            List<int[]> result = new ArrayList<>();
            int i = 0, j = 0;
            while (i < a.length && j < b.length) {
                int lo = Math.max(a[i][0], b[j][0]);
                int hi = Math.min(a[i][1], b[j][1]);
                String found;
                if (lo <= hi) {
                    result.add(new int[]{lo, hi});
                    found = "lo " + lo + " <= hi " + hi + " -> add [" + lo + "," + hi + "]";
                } else {
                    found = "lo " + lo + " >  hi " + hi + " -> none";
                }
                String moved;
                if (a[i][1] < b[j][1]) {
                    moved = "A" + i + " ends first, i++";
                    i++;
                } else {
                    moved = "B" + j + " ends first (or tie), j++";
                    j++;
                }
                System.out.println(found + "; " + moved);
            }
            return result.toArray(new int[0][]);
        }

        static void intersectionSection() {
            System.out.println("== intersection trace ==");
            int[][] a = {{0, 2}, {5, 10}, {13, 23}, {24, 25}};
            int[][] b = {{1, 5}, {8, 12}, {15, 24}, {25, 26}};
            System.out.println("A: " + fmt(a));
            System.out.println("B: " + fmt(b));
            System.out.println("result: " + fmt(intersectionsTraced(a, b)));

            System.out.println("== intersection wrong pointer ==");
            int[][] wideA = {{0, 10}};
            int[][] smallB = {{1, 2}, {3, 4}};
            System.out.println("A " + fmt(wideA) + ", B " + fmt(smallB));
            System.out.println("advance the smaller end:   " + fmt(intervalIntersections(wideA,
                    smallB)));
            System.out.println("advance the smaller start: "
                    + fmt(intersectionsAdvanceBySmallerStart(wideA, smallB)));

            System.out.println("== intersection edge cases ==");
            System.out.println("empty A: " + fmt(intervalIntersections(new int[0][], b)));
            System.out.println("touching [1,3] and [3,5]: " + fmt(intervalIntersections(new int[][]{{1,
                    3}}, new int[][]{{3, 5}})));
            System.out.println("apart [1,2] and [3,4]: " + fmt(intervalIntersections(new int[][]{{1,
                    2}}, new int[][]{{3, 4}})));
            System.out.println("same [1,5] and [1,5]: " + fmt(intervalIntersections(new int[][]{{1, 5}},
                    new int[][]{{1, 5}})));
        }

        // --------------------------------------------- 6. load (LC 1094, LC 1109)

        // Each trip is {passengers, from, to}. The passengers leave at `to`, before anyone boards
        // there.
        static boolean carPoolingByDifference(int[][] trips, int capacity) {
            int last = 0;
            for (int[] t : trips) last = Math.max(last, t[2]);
            int[] diff = new int[last + 1];             // one cell for each position 0 .. last
            for (int[] t : trips) {
                diff[t[1]] += t[0];                     // the passengers board at `from`
                diff[t[2]] -= t[0];                     // and leave at `to`
            }
            int load = 0;
            for (int change : diff) {
                load += change;                         // prefix sum: the load at this position
                if (load > capacity) return false;
            }
            return true;
        }

        static boolean carPoolingByEvents(int[][] trips, int capacity) {
            int[][] events = new int[trips.length * 2][];
            int k = 0;
            for (int[] t : trips) {
                events[k++] = new int[]{t[1], +t[0]};   // boarding
                events[k++] = new int[]{t[2], -t[0]};   // leaving
            }
            // At equal positions the smaller change comes first, so the leaving passengers go first.
            Arrays.sort(events, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                    : Integer.compare(a[1], b[1]));
            int load = 0;
            for (int[] event : events) {
                load += event[1];
                if (load > capacity) return false;
            }
            return true;
        }

        static boolean carPoolingWithTooLateDeparture(int[][] trips, int capacity) {
            int last = 0;
            for (int[] t : trips) last = Math.max(last, t[2] + 1);
            int[] diff = new int[last + 1];
            for (int[] t : trips) {
                diff[t[1]] += t[0];
                // WRONG: the passengers stay one position too long
                diff[t[2] + 1] -= t[0];
            }
            int load = 0;
            for (int change : diff) {
                load += change;
                if (load > capacity) return false;
            }
            return true;
        }

        static void loadSection() {
            System.out.println("== load trace ==");
            int[][] trips = {{3, 2, 7}, {3, 7, 9}, {8, 3, 9}};
            int capacity = 11;
            int last = 9;
            int[] diff = new int[last + 1];
            for (int[] t : trips) {
                diff[t[1]] += t[0];
                diff[t[2]] -= t[0];
            }
            System.out.println("trips (passengers, from, to): [3,2,7] [3,7,9] [8,3,9]");
            System.out.println("diff = " + Arrays.toString(diff));
            int[] loads = new int[last + 1];
            int load = 0;
            for (int p = 0; p <= last; p++) {
                load += diff[p];
                loads[p] = load;
            }
            System.out.println("load = " + Arrays.toString(loads));
            System.out.println("capacity " + capacity + ": difference array "
                    + carPoolingByDifference(trips, capacity)
                    + ", sorted events " + carPoolingByEvents(trips, capacity));
            System.out.println("capacity 10: difference array " + carPoolingByDifference(trips, 10)
                    + ", sorted events " + carPoolingByEvents(trips, 10));
            System.out.println("capacity 14: difference array " + carPoolingByDifference(trips, 14)
                    + ", sorted events " + carPoolingByEvents(trips, 14));

            System.out.println("== load drop-off tie ==");
            int[][] handover = {{3, 1, 5}, {3, 5, 7}};
            System.out.println("trips [3,1,5] [3,5,7], capacity 3: leave at to -> "
                    + carPoolingByDifference(handover, 3) + ", leave at to + 1 -> "
                    + carPoolingWithTooLateDeparture(handover, 3));

            System.out.println("== load edge cases ==");
            System.out.println("no trips: " + carPoolingByDifference(new int[0][], 0)
                    + ", one trip of 4 with capacity 4: " + carPoolingByDifference(new int[][]{{4, 0,
                            3}}, 4)
                    + ", capacity 3: " + carPoolingByDifference(new int[][]{{4, 0, 3}}, 3));
        }

        // ------------------------------------ 7. online booking (LC 729, 731, 732)

        static class MyCalendar {
            // start -> end, half-open
            private final TreeMap<Integer, Integer> booked = new TreeMap<>();

            boolean book(int start, int end) {
                // largest start <= start
                Map.Entry<Integer, Integer> before = booked.floorEntry(start);
                // smallest start >= start
                Map.Entry<Integer, Integer> after = booked.ceilingEntry(start);
                // it runs into the new one
                if (before != null && before.getValue() > start) return false;
                // the new one runs into it
                if (after != null && after.getKey() < end) return false;
                booked.put(start, end);
                return true;
            }
        }

        static class MyCalendarTwo {
            // time -> change in active count
            private final TreeMap<Integer, Integer> delta = new TreeMap<>();

            boolean book(int start, int end) {
                delta.merge(start, 1, Integer::sum);
                delta.merge(end, -1, Integer::sum);
                int active = 0;
                // the values come in the order of the times
                for (int change : delta.values()) {
                    active += change;
                    // a triple booking: undo this booking
                    if (active >= 3) {
                        delta.merge(start, -1, Integer::sum);
                        delta.merge(end, 1, Integer::sum);
                        return false;
                    }
                }
                return true;
            }
        }

        static class MyCalendarThree {
            private final TreeMap<Integer, Integer> delta = new TreeMap<>();

            int book(int start, int end) {
                delta.merge(start, 1, Integer::sum);
                delta.merge(end, -1, Integer::sum);
                int active = 0, most = 0;
                for (int change : delta.values()) {
                    active += change;
                    most = Math.max(most, active);                  // the largest overlap so far
                }
                return most;
            }
        }

        static class MyCalendarFloorOnly {
            private final TreeMap<Integer, Integer> booked = new TreeMap<>();

            boolean book(int start, int end) {
                Map.Entry<Integer, Integer> before = booked.floorEntry(start);
                // WRONG: no ceiling check
                if (before != null && before.getValue() > start) return false;
                booked.put(start, end);
                return true;
            }
        }

        static void calendarSection() {
            System.out.println("== calendar I trace ==");
            MyCalendar calendar = new MyCalendar();
            int[][] requests = {{10, 20}, {15, 25}, {20, 30}, {5, 10}, {8, 12}, {30, 31}};
            TreeMap<Integer, Integer> shadow = new TreeMap<>();
            for (int[] r : requests) {
                Map.Entry<Integer, Integer> before = shadow.floorEntry(r[0]);
                Map.Entry<Integer, Integer> after = shadow.ceilingEntry(r[0]);
                boolean ok = calendar.book(r[0], r[1]);
                if (ok) shadow.put(r[0], r[1]);
                System.out.println("book " + fmt1(r) + ": floor " + entry(before) + ", ceiling "
                        + entry(after)
                        + " -> " + ok);
            }

            System.out.println("== calendar I floor only ==");
            MyCalendarFloorOnly wrong = new MyCalendarFloorOnly();
            MyCalendar right = new MyCalendar();
            System.out.println("book [5,8]: wrong " + wrong.book(5, 8) + ", right " + right.book(5, 8));
            System.out.println("book [3,6]: wrong " + wrong.book(3, 6) + ", right " + right.book(3, 6));

            System.out.println("== calendar II trace ==");
            MyCalendarTwo two = new MyCalendarTwo();
            int[][] twoRequests = {{10, 20}, {50, 60}, {10, 40}, {5, 15}, {5, 10}, {25, 55}};
            for (int[] r : twoRequests) {
                System.out.println("book " + fmt1(r) + " -> " + two.book(r[0], r[1]));
            }

            System.out.println("== calendar III trace ==");
            MyCalendarThree three = new MyCalendarThree();
            int[][] threeRequests = {{10, 20}, {50, 60}, {10, 40}, {5, 15}, {5, 10}, {25, 55}};
            for (int[] r : threeRequests) {
                System.out.println("book " + fmt1(r) + " -> most " + three.book(r[0], r[1]));
            }
        }

        static String entry(Map.Entry<Integer, Integer> e) {
            return e == null ? "null" : e.getKey() + "=" + e.getValue();
        }

        // ------------------------------------ 8. free time (LC 759), meeting rooms (LC 252)

        // Each employee has a sorted list of busy intervals. Returns the gaps of positive length
        // in which nobody is busy, between the earliest start and the latest end.
        static List<int[]> employeeFreeTime(List<List<int[]>> schedules) {
            List<int[]> all = new ArrayList<>();
            for (List<int[]> schedule : schedules) all.addAll(schedule);
            List<int[]> free = new ArrayList<>();
            if (all.isEmpty()) return free;
            all.sort((a, b) -> Integer.compare(a[0], b[0]));       // sort by start, as in the merge
            int reach = all.get(0)[1];                              // the largest end so far
            for (int i = 1; i < all.size(); i++) {
                int[] busy = all.get(i);
                if (busy[0] > reach) {                              // > : touching leaves no gap
                    // the merge would close an interval here
                    free.add(new int[]{reach, busy[0]});
                }
                reach = Math.max(reach, busy[1]);
            }
            return free;
        }

        static boolean canAttendAll(int[][] meetings) {
            int[][] sorted = meetings.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
            for (int i = 1; i < sorted.length; i++) {
                // < : a meeting that starts at the end is fine
                if (sorted[i][0] < sorted[i - 1][1]) return false;
            }
            return true;
        }

        static void freeTimeSection() {
            System.out.println("== free time trace ==");
            List<List<int[]>> schedules = List.of(
                    List.of(new int[]{1, 3}, new int[]{6, 7}),
                    List.of(new int[]{2, 4}),
                    List.of(new int[]{2, 5}, new int[]{9, 12}));
            System.out.println("employee A: [1,3] [6,7]");
            System.out.println("employee B: [2,4]");
            System.out.println("employee C: [2,5] [9,12]");
            List<int[]> all = new ArrayList<>();
            for (List<int[]> s : schedules) all.addAll(s);
            all.sort((a, b) -> Integer.compare(a[0], b[0]));
            System.out.println("all busy intervals sorted by start: " + fmt(all));
            int reach = all.get(0)[1];
            System.out.println("take " + fmt1(all.get(0)) + ", reach = " + reach);
            for (int i = 1; i < all.size(); i++) {
                int[] busy = all.get(i);
                if (busy[0] > reach) {
                    System.out.println(fmt1(busy) + ": start " + busy[0] + " > reach " + reach
                            + " -> free [" + reach + ","
                            + busy[0] + "]");
                } else {
                    System.out.println(fmt1(busy) + ": start " + busy[0] + " <= reach " + reach
                            + " -> no gap");
                }
                reach = Math.max(reach, busy[1]);
                System.out.println("    reach = " + reach);
            }
            System.out.println("free time: " + fmt(employeeFreeTime(schedules)));

            System.out.println("== free time edge cases ==");
            System.out.println("touching busy [1,3] [3,5]: "
                    + fmt(employeeFreeTime(List.of(List.of(new int[]{1, 3}, new int[]{3, 5})))));
            System.out.println("nobody is busy: " + fmt(employeeFreeTime(List.of(List.of(),
                    List.of()))));
            System.out.println("one busy interval: " + fmt(employeeFreeTime(List.of(List.of(new int[]{2,
                    9})))));

            System.out.println("== meeting rooms I ==");
            System.out.println("[0,30] [5,10] [15,20]: " + canAttendAll(new int[][]{{0, 30}, {5, 10},
                    {15, 20}}));
            System.out.println("[7,10] [2,4]: " + canAttendAll(new int[][]{{7, 10}, {2, 4}}));
            System.out.println("[1,3] [3,5]: " + canAttendAll(new int[][]{{1, 3}, {3, 5}}));
            System.out.println("[1,10] [2,3] [11,12]: " + canAttendAll(new int[][]{{1, 10}, {2, 3}, {11,
                    12}}));
            System.out.println("empty: " + canAttendAll(new int[0][]) + ", one meeting: "
                    + canAttendAll(new int[][]{{1, 2}}));
        }

        // ------------------------------------------------ 9. skyline (LC 218)

        // Each building is {left, right, height}, with left < right and height > 0. The building covers
        // [left, right).
        static List<int[]> skyline(int[][] buildings) {
            int[][] events = new int[buildings.length * 2][];
            int k = 0;
            for (int[] b : buildings) {
                events[k++] = new int[]{b[0], b[2]};        // the building starts: add its height
                events[k++] = new int[]{b[1], -b[2]};       // the building ends: remove its height
            }
            Arrays.sort(events, (a, b) -> Integer.compare(a[0], b[0]));
            // height -> number of active buildings
            TreeMap<Integer, Integer> active = new TreeMap<>();
            List<int[]> result = new ArrayList<>();
            int previous = 0;                               // the height of the skyline so far
            int i = 0;
            while (i < events.length) {
                int x = events[i][0];
                // apply every event at x before reading
                while (i < events.length && events[i][0] == x) {
                    int h = events[i][1];
                    if (h > 0) {
                        active.merge(h, 1, Integer::sum);
                    } else if (active.merge(-h, -1, Integer::sum) == 0) {
                        active.remove(-h);                  // the last building of this height is gone
                    }
                    i++;
                }
                int current = active.isEmpty() ? 0 : active.lastKey();   // the tallest active building
                if (current != previous) {                  // a key point only where the height changes
                    result.add(new int[]{x, current});
                    previous = current;
                }
            }
            return result;
        }

        static List<int[]> skylineReadAfterEachEvent(int[][] buildings) {
            int[][] events = new int[buildings.length * 2][];
            int k = 0;
            for (int[] b : buildings) {
                events[k++] = new int[]{b[0], b[2]};
                events[k++] = new int[]{b[1], -b[2]};
            }
            Arrays.sort(events, (a, b) -> Integer.compare(a[0], b[0]));
            TreeMap<Integer, Integer> active = new TreeMap<>();
            List<int[]> result = new ArrayList<>();
            int previous = 0;
            // WRONG: reads the height after each single event
            for (int[] event : events) {
                int h = event[1];
                if (h > 0) active.merge(h, 1, Integer::sum);
                else if (active.merge(-h, -1, Integer::sum) == 0) active.remove(-h);
                int current = active.isEmpty() ? 0 : active.lastKey();
                if (current != previous) {
                    result.add(new int[]{event[0], current});
                    previous = current;
                }
            }
            return result;
        }

        static List<int[]> skylineWithoutComparison(int[][] buildings) {
            int[][] events = new int[buildings.length * 2][];
            int k = 0;
            for (int[] b : buildings) {
                events[k++] = new int[]{b[0], b[2]};
                events[k++] = new int[]{b[1], -b[2]};
            }
            Arrays.sort(events, (a, b) -> Integer.compare(a[0], b[0]));
            TreeMap<Integer, Integer> active = new TreeMap<>();
            List<int[]> result = new ArrayList<>();
            int i = 0;
            while (i < events.length) {
                int x = events[i][0];
                while (i < events.length && events[i][0] == x) {
                    int h = events[i][1];
                    if (h > 0) active.merge(h, 1, Integer::sum);
                    else if (active.merge(-h, -1, Integer::sum) == 0) active.remove(-h);
                    i++;
                }
                int current = active.isEmpty() ? 0 : active.lastKey();
                // WRONG: no comparison with the previous height
                result.add(new int[]{x, current});
            }
            return result;
        }

        static List<int[]> skylineWithSet(int[][] buildings) {
            int[][] events = new int[buildings.length * 2][];
            int k = 0;
            for (int[] b : buildings) {
                events[k++] = new int[]{b[0], b[2]};
                events[k++] = new int[]{b[1], -b[2]};
            }
            Arrays.sort(events, (a, b) -> Integer.compare(a[0], b[0]));
            // WRONG: a set holds one entry for each height
            java.util.TreeSet<Integer> active = new java.util.TreeSet<>();
            List<int[]> result = new ArrayList<>();
            int previous = 0;
            int i = 0;
            while (i < events.length) {
                int x = events[i][0];
                while (i < events.length && events[i][0] == x) {
                    int h = events[i][1];
                    if (h > 0) active.add(h);
                    else active.remove(-h);
                    i++;
                }
                int current = active.isEmpty() ? 0 : active.last();
                if (current != previous) {
                    result.add(new int[]{x, current});
                    previous = current;
                }
            }
            return result;
        }

        static List<int[]> skylineTraced(int[][] buildings) {
            int[][] events = new int[buildings.length * 2][];
            int k = 0;
            for (int[] b : buildings) {
                events[k++] = new int[]{b[0], b[2]};
                events[k++] = new int[]{b[1], -b[2]};
            }
            Arrays.sort(events, (a, b) -> Integer.compare(a[0], b[0]));
            TreeMap<Integer, Integer> active = new TreeMap<>();
            List<int[]> result = new ArrayList<>();
            int previous = 0;
            int i = 0;
            while (i < events.length) {
                int x = events[i][0];
                StringBuilder applied = new StringBuilder();
                while (i < events.length && events[i][0] == x) {
                    int h = events[i][1];
                    applied.append(h > 0 ? " add " : " remove ").append(Math.abs(h)).append(';');
                    if (h > 0) {
                        active.merge(h, 1, Integer::sum);
                    } else if (active.merge(-h, -1, Integer::sum) == 0) {
                        active.remove(-h);
                    }
                    i++;
                }
                int current = active.isEmpty() ? 0 : active.lastKey();
                String decision = current != previous ? "key point [" + x + "," + current + "]"
                        : "same height, no key point";
                System.out.println("x = " + x + ":" + applied + " active " + active + ", tallest "
                        + current + " -> " + decision);
                if (current != previous) {
                    result.add(new int[]{x, current});
                    previous = current;
                }
            }
            return result;
        }

        static List<int[]> skylineBrute(int[][] buildings) {
            int maxX = 0;
            for (int[] b : buildings) maxX = Math.max(maxX, b[1]);
            List<int[]> result = new ArrayList<>();
            int previous = 0;
            for (int x = 0; x <= maxX; x++) {
                int h = 0;
                for (int[] b : buildings) {
                    if (b[0] <= x && x < b[1]) h = Math.max(h, b[2]);
                }
                if (h != previous) {
                    result.add(new int[]{x, h});
                    previous = h;
                }
            }
            return result;
        }

        static void skylineSection() {
            System.out.println("== skyline trace ==");
            int[][] buildings = {{1, 5, 3}, {2, 4, 5}, {3, 8, 4}, {9, 11, 2}};
            System.out.println("buildings (left, right, height): [1,5,3] [2,4,5] [3,8,4] [9,11,2]");
            System.out.println("key points: " + fmt(skylineTraced(buildings)));

            System.out.println("== skyline wrong versions ==");
            int[][] oneAfterAnother = {{1, 3, 5}, {3, 6, 2}};
            System.out.println("buildings [1,3,5] [3,6,2]");
            System.out.println("apply all events at x: " + fmt(skyline(oneAfterAnother)));
            System.out.println("read after each event: "
                    + fmt(skylineReadAfterEachEvent(oneAfterAnother)));
            int[][] sameHeight = {{0, 2, 3}, {2, 5, 3}};
            System.out.println("buildings [0,2,3] [2,5,3]");
            System.out.println("compare with the previous height: " + fmt(skyline(sameHeight)));
            System.out.println("no comparison:                    "
                    + fmt(skylineWithoutComparison(sameHeight)));
            int[][] twoThrees = {{1, 4, 3}, {2, 6, 3}};
            System.out.println("buildings [1,4,3] [2,6,3]");
            System.out.println("count of each height: " + fmt(skyline(twoThrees)));
            System.out.println("set of heights:       " + fmt(skylineWithSet(twoThrees)));

            System.out.println("== skyline edge cases ==");
            System.out.println("no buildings: " + fmt(skyline(new int[0][])));
            System.out.println("one building [1,2,1]: " + fmt(skyline(new int[][]{{1, 2, 1}})));
            System.out.println("identical [1,3,4] twice: " + fmt(skyline(new int[][]{{1, 3, 4}, {1, 3,
                    4}})));
            System.out.println("nested [1,10,2] [3,5,9]: " + fmt(skyline(new int[][]{{1, 10, 2}, {3, 5,
                    9}})));
        }

        // ------------------------------------------------------- 10. the ladder

        // Counts the intervals that no other interval covers. [a, b] is covered by [c, d] if c <= a and
        // b <= d.
        static int countNotCovered(int[][] intervals) {
            int[][] sorted = intervals.clone();
            // Start ascending. For equal starts, the longer interval first, so it covers the shorter
            // one.
            Arrays.sort(sorted, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                    : Integer.compare(b[1], a[1]));
            int kept = 0;
            // the largest end among the earlier intervals
            int reach = Integer.MIN_VALUE;
            for (int[] iv : sorted) {
                if (iv[1] > reach) {                        // it ends beyond every earlier interval
                    kept++;
                    reach = iv[1];
                }                                           // otherwise an earlier interval covers it
            }
            return kept;
        }

        // A pair [c, d] can follow [a, b] only if b < c. Returns the longest chain.
        static int longestPairChain(int[][] pairs) {
            int[][] sorted = pairs.clone();
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));   // sort by END
            int length = 0;
            long lastEnd = Long.MIN_VALUE;
            for (int[] p : sorted) {
                // > : the next pair must start AFTER the end
                if (p[0] > lastEnd) {
                    length++;
                    lastEnd = p[1];
                }
            }
            return length;
        }

        // Closed intervals. Returns the fewest groups such that no two intervals of a group share a
        // point.
        static int minGroups(int[][] intervals) {
            long[] events = new long[intervals.length * 2];
            int k = 0;
            for (int[] iv : intervals) {
                // a start: the low bit is 0, so it sorts first
                events[k++] = iv[0] * 2L;
                // an end: the low bit is 1, so it sorts after
                events[k++] = iv[1] * 2L + 1;
            }
            Arrays.sort(events);
            int active = 0, most = 0;
            for (long event : events) {
                if ((event & 1) == 0) {
                    active++;
                    most = Math.max(most, active);
                } else {
                    active--;
                }
            }
            return most;
        }

        // Each person is {birth, death}: alive in the years birth, birth + 1, ..., death - 1.
        // Returns the earliest year with the largest population, or -1 if there is nobody.
        static int maximumPopulationYear(int[][] logs) {
            if (logs.length == 0) return -1;
            int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE;
            for (int[] p : logs) {
                first = Math.min(first, p[0]);
                last = Math.max(last, p[1]);
            }
            int[] diff = new int[last - first + 1];     // one cell for each year first .. last
            for (int[] p : logs) {
                diff[p[0] - first]++;                   // alive from the birth year
                diff[p[1] - first]--;                   // not alive from the death year
            }
            int alive = 0, best = 0, bestYear = first;
            for (int i = 0; i < diff.length; i++) {
                alive += diff[i];
                // > : the first year with the largest count wins
                if (alive > best) {
                    best = alive;
                    bestYear = first + i;
                }
            }
            return bestYear;
        }

        static int countNotCoveredTiesAscending(int[][] intervals) {
            int[][] sorted = intervals.clone();
            // WRONG: ties keep input order
            Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
            int kept = 0;
            int reach = Integer.MIN_VALUE;
            for (int[] iv : sorted) {
                if (iv[1] > reach) {
                    kept++;
                    reach = iv[1];
                }
            }
            return kept;
        }

        static void ladderSection() {
            System.out.println("== covered trace ==");
            int[][] covered = {{1, 4}, {3, 6}, {2, 8}, {2, 5}, {7, 9}};
            int[][] sorted = covered.clone();
            Arrays.sort(sorted, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                    : Integer.compare(b[1], a[1]));
            System.out.println("sorted by start, then longer first: " + fmt(sorted));
            int kept = 0;
            int reach = Integer.MIN_VALUE;
            for (int[] iv : sorted) {
                if (iv[1] > reach) {
                    kept++;
                    System.out.println(fmt1(iv) + ": end " + iv[1] + " > reach "
                            + (reach == Integer.MIN_VALUE ? "none" : reach)
                            + " -> keep, kept = " + kept);
                    reach = iv[1];
                } else {
                    System.out.println(fmt1(iv) + ": end " + iv[1] + " <= reach " + reach
                            + " -> covered");
                }
            }
            System.out.println("answer: " + countNotCovered(covered));
            System.out.println("empty: " + countNotCovered(new int[0][]) + ", one: "
                    + countNotCovered(new int[][]{{1, 2}})
                    + ", same twice: " + countNotCovered(new int[][]{{1, 5}, {1, 5}})
                    + ", same start [1,2] [1,9]: " + countNotCovered(new int[][]{{1, 2}, {1, 9}}));

            System.out.println("== population ==");
            int[][] logs = {{2, 6}, {4, 9}, {5, 7}, {8, 10}};
            int firstYear = 2, lastYear = 10;
            int[] popDiff = new int[lastYear - firstYear + 1];
            for (int[] p : logs) {
                popDiff[p[0] - firstYear]++;
                popDiff[p[1] - firstYear]--;
            }
            System.out.println("people (birth, death): [2,6] [4,9] [5,7] [8,10]");
            System.out.println("years      2  3  4  5  6  7  8  9 10");
            StringBuilder diffRow = new StringBuilder("diff     ");
            StringBuilder aliveRow = new StringBuilder("alive    ");
            int running = 0;
            for (int d : popDiff) {
                running += d;
                diffRow.append(String.format("%3d", d));
                aliveRow.append(String.format("%3d", running));
            }
            System.out.println(diffRow);
            System.out.println(aliveRow);
            System.out.println("answer: " + maximumPopulationYear(logs));
            System.out.println("one person [5,6]: " + maximumPopulationYear(new int[][]{{5, 6}})
                    + ", nobody: " + maximumPopulationYear(new int[0][])
                    + ", two people alive in year 3 and year 7 (tie): "
                    + maximumPopulationYear(new int[][]{{3, 4}, {7, 8}}));

            System.out.println("== covered wrong ties ==");
            int[][] sameStart = {{1, 2}, {1, 9}};
            System.out.println("intervals [1,2] [1,9]: longer first gives " + countNotCovered(sameStart)
                    + ", ties in input order gives " + countNotCoveredTiesAscending(sameStart));
            System.out.println("== chain ==");
            int[][] pairs = {{1, 2}, {2, 3}, {3, 4}, {1, 6}, {7, 8}};
            System.out.println("pairs " + fmt(pairs) + ": longest chain " + longestPairChain(pairs));
            System.out.println("empty: " + longestPairChain(new int[0][]) + ", one: "
                    + longestPairChain(new int[][]{{5, 6}}));
            int[][] sortedByEnd = pairs.clone();
            Arrays.sort(sortedByEnd, (a, b) -> Integer.compare(a[1], b[1]));
            System.out.println("sorted by end: " + fmt(sortedByEnd));
            int taken = 0;
            long last = Long.MIN_VALUE;
            for (int[] p : sortedByEnd) {
                String lastText = last == Long.MIN_VALUE ? "none" : String.valueOf(last);
                if (p[0] > last) {
                    taken++;
                    last = p[1];
                    System.out.println(fmt1(p) + ": start " + p[0] + " > last end " + lastText
                            + " -> take, length = " + taken);
                } else {
                    System.out.println(fmt1(p) + ": start " + p[0] + " <= last end " + lastText
                            + " -> skip");
                }
            }
            int withGreaterOrEqual = 0;
            long lastWrong = Long.MIN_VALUE;
            for (int[] p : sortedByEnd) {
                if (p[0] >= lastWrong) {
                    withGreaterOrEqual++;
                    lastWrong = p[1];
                }
            }
            System.out.println("with >= in place of >: " + withGreaterOrEqual);

            System.out.println("== groups ==");
            int[][] closed = {{1, 5}, {5, 8}, {9, 12}};
            System.out.println("closed " + fmt(closed) + ": groups " + minGroups(closed)
                    + ", as half-open the sweep gives " + maxActiveBySweep(closed, true));
            System.out.println("empty: " + minGroups(new int[0][]) + ", one: "
                    + minGroups(new int[][]{{1, 1}}));

            System.out.println("== groups trace ==");
            int[][] closedTrace = {{1, 5}, {5, 8}, {9, 12}};
            long[] ev = new long[closedTrace.length * 2];
            int q = 0;
            for (int[] iv : closedTrace) {
                ev[q++] = iv[0] * 2L;
                ev[q++] = iv[1] * 2L + 1;
            }
            Arrays.sort(ev);
            int act = 0;
            for (long e : ev) {
                boolean isStart = (e & 1) == 0;
                act += isStart ? 1 : -1;
                System.out.println("time " + Math.floorDiv(e, 2) + (isStart ? " start" : " end  ")
                        + " -> active " + act);
            }
            System.out.println("== calendar III ladder ==");
            MyCalendarThree t = new MyCalendarThree();
            System.out.println("first booking [1,5): " + t.book(1, 5) + ", second [5,9): " + t.book(5,
                    9)
                    + ", third [3,6): " + t.book(3, 6));
        }
    }
    ```

    <!-- expected-output -->
    ```text
    == merge trace ==
    input: [8,10] [1,3] [2,6] [15,18] [17,20]
    sorted by start: [1,3] [2,6] [8,10] [15,18] [17,20]
    take [1,3] as the current interval
    [2,6]: start 2 <= current end 3 -> extend, end = max(3, 6) = 6
    [8,10]: start 8 > current end 6 -> close [1,6], current = [8,10]
    [15,18]: start 15 > current end 10 -> close [8,10], current = [15,18]
    [17,20]: start 17 <= current end 18 -> extend, end = max(18, 20) = 20
    close [15,20]
    result: [1,6] [8,10] [15,20]
    == merge edge cases ==
    (none) -> (none)
    [4,7] -> [4,7]
    [1,10] [2,3] [4,5] -> [1,10]
    [1,4] [4,5] -> [1,5]
    [1,2] [3,4] -> [1,2] [3,4]
    [2,5] [2,5] [2,5] -> [2,5]
    [5,6] [1,2] -> [1,2] [5,6]
    == merge wrong versions ==
    without max on [1,10] [2,3] [4,5]: [1,3] [4,5]
    with max on    [1,10] [2,3] [4,5]: [1,10]
    without sort on [8,10] [1,3] [2,6] [15,18] [17,20]: [8,10] [15,20]
    with sort on    [8,10] [1,3] [2,6] [15,18] [17,20]: [1,6] [8,10] [15,20]
    == insert trace ==
    input: [1,2] [3,5] [6,7] [8,10] [12,16]
    new interval [4,8]
    phase 1: [1,2] ends at 2 < 4 -> copy
    phase 2: [3,5] starts at 3 <= 8 -> new interval becomes [3,8]
    phase 2: [6,7] starts at 6 <= 8 -> new interval becomes [3,8]
    phase 2: [8,10] starts at 8 <= 8 -> new interval becomes [3,10]
    add the new interval [3,10]
    phase 3: [12,16] starts after the new interval -> copy
    result: [1,2] [3,10] [12,16]
    == insert edge cases ==
    empty list, add [5,6]: [5,6]
    add before all, [0,0]: [0,0] [1,2] [3,5] [6,7] [8,10] [12,16]
    add after all, [20,21]: [1,2] [3,5] [6,7] [8,10] [12,16] [20,21]
    add inside a gap, [10,11]: [1,2] [3,5] [6,7] [8,11] [12,16]
    add touching the first, [2,3]: [1,5] [6,7] [8,10] [12,16]
    add covering all, [0,20]: [0,20]
    == selection trace ==
    input: [0,5] [1,2] [2,4] [4,6] [5,9] [8,10]
    sorted by end: [1,2] [2,4] [0,5] [4,6] [5,9] [8,10]
    [1,2]: start 1 >= last end none -> keep, kept = 1
    [2,4]: start 2 >= last end 2 -> keep, kept = 2
    [0,5]: start 0 <  last end 4 -> skip
    [4,6]: start 4 >= last end 4 -> keep, kept = 3
    [5,9]: start 5 <  last end 6 -> skip
    [8,10]: start 8 >= last end 6 -> keep, kept = 4
    kept 4, removed 2
    == selection wrong key ==
    sort by end on [0,5] [1,2] [2,4] [4,6] [5,9] [8,10]: kept 4
    sort by start on [0,5] [1,2] [2,4] [4,6] [5,9] [8,10]: kept 2
    sort by end on [1,100] [2,3] [4,5]: kept 2
    sort by start on [1,100] [2,3] [4,5]: kept 1
    == selection touching ==
    half-open [1,2] [2,3]: kept 2, with > instead of >=: kept 1
    closed    [1,2] [2,3]: points 1
    == selection edge cases ==
    (none) -> kept 0, points 0
    [3,4] -> kept 1, points 1
    [1,2] [1,2] [1,2] -> kept 1, points 1
    [1,5] [2,6] [3,7] -> kept 1, points 1
    == rooms sweep trace ==
    meetings: [0,30] [5,10] [10,25] [15,20] [28,35]
    time  0  start (+1)  active = 1  most = 1
    time  5  start (+1)  active = 2  most = 2
    time 10  end   (-1)  active = 1  most = 2
    time 10  start (+1)  active = 2  most = 2
    time 15  start (+1)  active = 3  most = 3
    time 20  end   (-1)  active = 2  most = 3
    time 25  end   (-1)  active = 1  most = 3
    time 28  start (+1)  active = 2  most = 3
    time 30  end   (-1)  active = 1  most = 3
    time 35  end   (-1)  active = 0  most = 3
    answer: 3
    == rooms heap trace ==
    [0,30]: no room yet, open one; end times now [30]
    [5,10]: earliest end 30 > 5, open a room; end times now [10, 30]
    [10,25]: earliest end 10 <= 10, reuse that room; end times now [25, 30]
    [15,20]: earliest end 25 > 15, open a room; end times now [20, 25, 30]
    [28,35]: earliest end 20 <= 28, reuse that room; end times now [25, 30, 35]
    answer: 3
    == rooms tie order ==
    half-open [0,10] [10,20]: ends first gives 1, starts first gives 2
    closed    [0,10] [10,20]: starts first gives 2, ends first gives 1
    == rooms agree ==
    sweep 3, heap 3, two arrays 3, brute force 3
    == rooms edge cases ==
    (none) -> sweep 0, heap 0
    [4,9] -> sweep 1, heap 1
    [1,5] [1,5] [1,5] -> sweep 3, heap 3
    [1,2] [2,3] [3,4] -> sweep 1, heap 1
    [1,10] [2,3] [4,5] -> sweep 2, heap 2
    == intersection trace ==
    A: [0,2] [5,10] [13,23] [24,25]
    B: [1,5] [8,12] [15,24] [25,26]
    lo 1 <= hi 2 -> add [1,2]; A0 ends first, i++
    lo 5 <= hi 5 -> add [5,5]; B0 ends first (or tie), j++
    lo 8 <= hi 10 -> add [8,10]; A1 ends first, i++
    lo 13 >  hi 12 -> none; B1 ends first (or tie), j++
    lo 15 <= hi 23 -> add [15,23]; A2 ends first, i++
    lo 24 <= hi 24 -> add [24,24]; B2 ends first (or tie), j++
    lo 25 <= hi 25 -> add [25,25]; A3 ends first, i++
    result: [1,2] [5,5] [8,10] [15,23] [24,24] [25,25]
    == intersection wrong pointer ==
    A [0,10], B [1,2] [3,4]
    advance the smaller end:   [1,2] [3,4]
    advance the smaller start: [1,2]
    == intersection edge cases ==
    empty A: (none)
    touching [1,3] and [3,5]: [3,3]
    apart [1,2] and [3,4]: (none)
    same [1,5] and [1,5]: [1,5]
    == load trace ==
    trips (passengers, from, to): [3,2,7] [3,7,9] [8,3,9]
    diff = [0, 0, 3, 8, 0, 0, 0, 0, 0, -11]
    load = [0, 0, 3, 11, 11, 11, 11, 11, 11, 0]
    capacity 11: difference array true, sorted events true
    capacity 10: difference array false, sorted events false
    capacity 14: difference array true, sorted events true
    == load drop-off tie ==
    trips [3,1,5] [3,5,7], capacity 3: leave at to -> true, leave at to + 1 -> false
    == load edge cases ==
    no trips: true, one trip of 4 with capacity 4: true, capacity 3: false
    == calendar I trace ==
    book [10,20]: floor null, ceiling null -> true
    book [15,25]: floor 10=20, ceiling null -> false
    book [20,30]: floor 10=20, ceiling null -> true
    book [5,10]: floor null, ceiling 10=20 -> true
    book [8,12]: floor 5=10, ceiling 10=20 -> false
    book [30,31]: floor 20=30, ceiling null -> true
    == calendar I floor only ==
    book [5,8]: wrong true, right true
    book [3,6]: wrong true, right false
    == calendar II trace ==
    book [10,20] -> true
    book [50,60] -> true
    book [10,40] -> true
    book [5,15] -> false
    book [5,10] -> true
    book [25,55] -> true
    == calendar III trace ==
    book [10,20] -> most 1
    book [50,60] -> most 1
    book [10,40] -> most 2
    book [5,15] -> most 3
    book [5,10] -> most 3
    book [25,55] -> most 3
    == free time trace ==
    employee A: [1,3] [6,7]
    employee B: [2,4]
    employee C: [2,5] [9,12]
    all busy intervals sorted by start: [1,3] [2,4] [2,5] [6,7] [9,12]
    take [1,3], reach = 3
    [2,4]: start 2 <= reach 3 -> no gap
        reach = 4
    [2,5]: start 2 <= reach 4 -> no gap
        reach = 5
    [6,7]: start 6 > reach 5 -> free [5,6]
        reach = 7
    [9,12]: start 9 > reach 7 -> free [7,9]
        reach = 12
    free time: [5,6] [7,9]
    == free time edge cases ==
    touching busy [1,3] [3,5]: (none)
    nobody is busy: (none)
    one busy interval: (none)
    == meeting rooms I ==
    [0,30] [5,10] [15,20]: false
    [7,10] [2,4]: true
    [1,3] [3,5]: true
    [1,10] [2,3] [11,12]: false
    empty: true, one meeting: true
    == skyline trace ==
    buildings (left, right, height): [1,5,3] [2,4,5] [3,8,4] [9,11,2]
    x = 1: add 3; active {3=1}, tallest 3 -> key point [1,3]
    x = 2: add 5; active {3=1, 5=1}, tallest 5 -> key point [2,5]
    x = 3: add 4; active {3=1, 4=1, 5=1}, tallest 5 -> same height, no key point
    x = 4: remove 5; active {3=1, 4=1}, tallest 4 -> key point [4,4]
    x = 5: remove 3; active {4=1}, tallest 4 -> same height, no key point
    x = 8: remove 4; active {}, tallest 0 -> key point [8,0]
    x = 9: add 2; active {2=1}, tallest 2 -> key point [9,2]
    x = 11: remove 2; active {}, tallest 0 -> key point [11,0]
    key points: [1,3] [2,5] [4,4] [8,0] [9,2] [11,0]
    == skyline wrong versions ==
    buildings [1,3,5] [3,6,2]
    apply all events at x: [1,5] [3,2] [6,0]
    read after each event: [1,5] [3,0] [3,2] [6,0]
    buildings [0,2,3] [2,5,3]
    compare with the previous height: [0,3] [5,0]
    no comparison:                    [0,3] [2,3] [5,0]
    buildings [1,4,3] [2,6,3]
    count of each height: [1,3] [6,0]
    set of heights:       [1,3] [4,0]
    == skyline edge cases ==
    no buildings: (none)
    one building [1,2,1]: [1,1] [2,0]
    identical [1,3,4] twice: [1,4] [3,0]
    nested [1,10,2] [3,5,9]: [1,2] [3,9] [5,2] [10,0]
    == covered trace ==
    sorted by start, then longer first: [1,4] [2,8] [2,5] [3,6] [7,9]
    [1,4]: end 4 > reach none -> keep, kept = 1
    [2,8]: end 8 > reach 4 -> keep, kept = 2
    [2,5]: end 5 <= reach 8 -> covered
    [3,6]: end 6 <= reach 8 -> covered
    [7,9]: end 9 > reach 8 -> keep, kept = 3
    answer: 3
    empty: 0, one: 1, same twice: 1, same start [1,2] [1,9]: 1
    == population ==
    people (birth, death): [2,6] [4,9] [5,7] [8,10]
    years      2  3  4  5  6  7  8  9 10
    diff       1  0  1  1 -1 -1  1 -1 -1
    alive      1  1  2  3  2  1  2  1  0
    answer: 5
    one person [5,6]: 5, nobody: -1, two people alive in year 3 and year 7 (tie): 3
    == covered wrong ties ==
    intervals [1,2] [1,9]: longer first gives 1, ties in input order gives 2
    == chain ==
    pairs [1,2] [2,3] [3,4] [1,6] [7,8]: longest chain 3
    empty: 0, one: 1
    sorted by end: [1,2] [2,3] [3,4] [1,6] [7,8]
    [1,2]: start 1 > last end none -> take, length = 1
    [2,3]: start 2 <= last end 2 -> skip
    [3,4]: start 3 > last end 2 -> take, length = 2
    [1,6]: start 1 <= last end 4 -> skip
    [7,8]: start 7 > last end 4 -> take, length = 3
    with >= in place of >: 4
    == groups ==
    closed [1,5] [5,8] [9,12]: groups 2, as half-open the sweep gives 1
    empty: 0, one: 1
    == groups trace ==
    time 1 start -> active 1
    time 5 start -> active 2
    time 5 end   -> active 1
    time 8 end   -> active 0
    time 9 start -> active 1
    time 12 end   -> active 0
    == calendar III ladder ==
    first booking [1,5): 1, second [5,9): 1, third [3,6): 2
    ```

## Complexity and Trade-offs

| Variant | Time | Space | Reason |
|---|---|---|---|
| Merge by pairs (brute force) | O(n^3) worst case | O(n) | At most `n - 1` merges, and each merge restarts a scan of `n(n - 1) / 2` pair tests |
| Merge, sort by start ([LC 56](https://leetcode.com/problems/merge-intervals/)) | O(n log n) | O(n) | The sort costs about `n log n` comparisons. The scan makes `n - 1` comparisons. The output and the sorted copy hold up to `n` intervals |
| Insert into a sorted list ([LC 57](https://leetcode.com/problems/insert-interval/)) | O(n) | O(n) | The three loops move one index from 0 to `n` once |
| Largest set without overlap ([LC 435](https://leetcode.com/problems/non-overlapping-intervals/), [LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/), [LC 646](https://leetcode.com/problems/maximum-length-of-pair-chain/)) | O(n log n) | O(n) | The sort by end dominates. The scan makes `n` comparisons |
| Active count by a sweep ([LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium), [LC 2406](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/)) | O(n log n) | O(n) | `2n` events are sorted, and the scan reads each event once |
| Active count by a min heap ([LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium)) | O(n log n) | O(n) | The sort costs O(n log n). Each meeting makes at most one `poll` and one `offer` on a heap of at most `n` end times |
| Intersection of two sorted lists ([LC 986](https://leetcode.com/problems/interval-list-intersections/)) | O(m + n) | O(1) besides the answer | Each step moves one of two pointers, so there are at most `m + n` steps |
| Load by a difference array ([LC 1094](https://leetcode.com/problems/car-pooling/), [LC 1109](https://leetcode.com/problems/corporate-flight-bookings/), [LC 1854](https://leetcode.com/problems/maximum-population-year/)) | O(n + C) | O(C) | Each of the `n` intervals makes two writes. The prefix sum reads `C` cells, where `C` is the number of positions |
| Booking with a `TreeMap` of intervals ([LC 729](https://leetcode.com/problems/my-calendar-i/)) | O(log n) for each booking | O(n) | Two navigation calls and one `put`, each O(log n) |
| Booking with a `TreeMap` of changes ([LC 731](https://leetcode.com/problems/my-calendar-ii/), [LC 732](https://leetcode.com/problems/my-calendar-iii/)) | O(n) for each booking | O(n) | The code sweeps all entries of the map after each booking |
| Free time and attendance ([LC 759](https://leetcode.com/problems/employee-free-time/) (premium), [LC 252](https://leetcode.com/problems/meeting-rooms/) (premium)) | O(N log N) for `N` intervals | O(N) | The sort dominates. The scan makes `N - 1` comparisons |
| Skyline ([LC 218](https://leetcode.com/problems/the-skyline-problem/)) | O(n log n) | O(n) | `2n` events are sorted. Each event makes one `TreeMap` operation of O(log n) |

**The alternatives.** The last column names the property that decides.

| Alternative | Time | When it is better | The property that decides |
|---|---|---|---|
| Test every pair | O(n^2) for one pass | The list is so short that the sort and the extra code do not pay | Is `n` small, for example below 10? |
| Difference array instead of sorted events | O(n + C) | The positions are small integers, and all updates are known first | Is the largest position small enough to hold one `int` for each position? |
| Sorted events instead of a difference array | O(n log n) | The positions are large, sparse, negative, or not integers | Does a cell for each position fit in memory? |
| Sweep instead of a heap | O(n log n) | The question asks only for the count | Does the code need to name the room? |
| Heap instead of a sweep | O(n log n) | The code assigns each interval to a resource, or asks which resource frees first | Does the answer name a resource? |
| `TreeMap` instead of a sorted list with binary search | O(log n) for each booking | The calendar changes between requests | A sorted `ArrayList` finds a place in O(log n), but each insert shifts up to `n` elements |
| Segment tree for LC 731 and LC 732 | See [Pattern 21](../21-segment-tree-and-fenwick-tree/README.md) | The number of requests is large | Is O(n) for each booking too slow for the number of requests? |
| Counting sort of the starts | O(n + C) | The coordinates are bounded by a small `C`, and the code needs only the sorted order | Is `C` small? |

Recommendation: write the sort-and-scan solution first, state the cost, and then name the alternative that fits the constraints. The choice of the key (start, end, or the endpoints) is the part that an interviewer checks.

## Recognition Guide

**Signals in the statement.**

- "Intervals", "ranges", "segments", "time slots".
- "Meetings", "bookings", "reservations", "calendar", "schedule".
- "Overlap", "conflict", "intersect", "covered".
- "Merge", "insert", "union", "free time", "gaps".
- "The minimum number of rooms, platforms, arrows, or groups".
- "The most intervals that you can keep or attend".
- "Passengers picked up at one position and dropped at another", "the capacity at any time".
- "The outline", "the highest at each position".

**Signals in the constraints.**

- The number of intervals is large enough that a test of every pair is too slow, and a sort fits.
- The positions are small integers: a difference array is possible. If the positions are large, sort events or use a `TreeMap`.
- Each booking needs its answer before the next booking arrives: the problem is online, and a `TreeMap` fits.
- The input lists are already sorted: look for a scan without a sort (two pointers, or three phases).

**Signals in the input.** A list of pairs `[start, end]`, or triples like `[passengers, from, to]` and `[left, right, height]`. The output is a merged list, a count (rooms, removals, arrows, groups), a yes or no for each booking, a list of gaps, or a list of key points.

### Looks similar, but needs another technique

| Problem | Why it looks like an interval problem | The property that rules out this pattern | The right technique |
|---|---|---|---|
| [LC 1235: Maximum Profit in Job Scheduling](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) | Jobs are intervals, and no two chosen jobs may overlap, as in LC 435 | Each job has a profit. The exchange argument replaces a job by a job that ends earlier, and a profit can make that swap lose value | Dynamic programming with binary search. See [Pattern 15](../15-dynamic-programming/README.md), and [Pattern 14](../14-greedy/README.md) for the failing greedy rule |
| [LC 560: Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) | A subarray is a range of indexes | The ranges are not given. The code searches for the ranges whose sum equals `k`, and the question is about sums | Prefix sums and a hash map. See [Pattern 04](../04-prefix-sum/README.md) |
| [LC 34: Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) | The answer is a range of indexes | There is one sorted array and one target. The code searches for two boundaries, and it has no list of intervals | Binary search. See [Pattern 06](../06-binary-search-on-answer/README.md) |
| [LC 239: Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) | Each window is a range of `k` indexes | The ranges all have the same length and move one step at a time. The question is the maximum inside each range | Monotonic deque. See [Pattern 07](../07-monotonic-stack-queue/README.md) |
| [LC 228: Summary Ranges](https://leetcode.com/problems/summary-ranges/) | The answer is a list of ranges | The input is one sorted array of distinct values. The code groups neighbors that differ by 1, and it has no overlap to test | Two pointers or one scan. See [Pattern 02](../02-two-pointers/README.md) |
| The k-th smallest start among many intervals | The input is a list of intervals | The question asks for a rank, and it has no overlap structure | Heap or quickselect. See [Pattern 16](../16-heap-priority-queue/README.md) |

### The preconditions of the pattern

Each precondition below comes with an input that breaks the code. The wrong outputs come from the programs of this page.

1. **The merge needs the sorted order.** The code that reads the intervals in the order of the input gives `[8,10] [15,20]` for the anchor input `[8,10] [1,3] [2,6] [15,18] [17,20]`. The correct answer is `[1,6] [8,10] [15,20]`. The fix: sort by start first.
2. **The three phases of an insert need a sorted, disjoint list.** For the list `[6,7] [1,2] [3,5]` and the new interval `[4,6]`, the three phases give `[1,7]`. The correct answer is `[1,2] [3,7]`. The fix: use the merge of the anchor problem for an unsorted list.
3. **The earliest-end rule needs every interval to weigh the same.** For the jobs `[0,10)` with profit 100, `[0,5)` with profit 1, and `[5,10)` with profit 1, the rule keeps the last two and gets a profit of 2. The best profit is 100, from `[0,10)` alone. The fix: dynamic programming.
4. **The difference array needs positions that fit in memory.** For a largest position of 1,000,000,000, the array has 1,000,000,000 cells. An `int` array of that length uses 4,000,000,000 bytes. The fix: sorted events or a `TreeMap`.
5. **The two-neighbor test of LC 729 needs booked intervals that do not overlap.** If the booked set holds `[1,10)` and `[2,3)`, the floor entry of the new start 5 is `2=3`, and `3 > 5` is false. The test finds no conflict for `[5,6)`, but `[1,10)` overlaps it. The program prints this case (Common Mistakes, mistake 12). The fix: the sweep over the changes of LC 731.
6. **The two pointers of an intersection need both lists sorted and disjoint.** For `A = [5,6] [1,2]` and `B = [1,6]`, the pointers give `[5,6]` and miss `[1,2]`. Testing every pair gives `[5,6] [1,2]`. The fix: sort each list, or merge the intervals of each list first.
7. **The sweep needs the tie order of the convention.** For `[0,10) [10,20)` the order "starts first" gives 2 rooms, and the right answer is 1 (Example 3).

## Common Mistakes

Each mistake has an input that fails, the wrong output, and the fix. The outputs come from two programs: `IntervalTraces` (Implementation) and `MistakeDemos` (at the end of this section).

### Mistake 1: a comparator that subtracts

The comparator `(a, b) -> a[0] - b[0]` overflows when the two values are far apart. Take the intervals `[2000000000, 2000000005]` and `[-2000000000, -1999999995]`. The difference of the starts is `2000000000 - (-2000000000) = 4000000000`, which does not fit in an `int`.

<!-- fragment -->
```java
// WRONG: the subtraction can overflow
Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

// CORRECT
Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
```

```text
a - b = -294967296, Integer.compare(a, b) = 1
sorted with x[0] - y[0]:            [2000000000,2000000005] [-2000000000,-1999999995]
sorted with Integer.compare:        [-2000000000,-1999999995] [2000000000,2000000005]
merge after the subtraction sort:   [2000000000,2000000005]
merge after the Integer.compare sort: [-2000000000,-1999999995] [2000000000,2000000005]
```

The difference wraps to `-294967296`, so the comparator says that `a` comes before `b`. The wrong sort leaves the larger start first. The merge after the wrong sort takes `[2000000000,2000000005]` as the current interval. It then reads `[-2000000000,-1999999995]`, whose start is below the current end, so it treats the two as overlapping and keeps only the larger end. The second interval disappears: the answer is one interval, and the correct answer is two. The fix: `Integer.compare`. The page uses `Integer.compare` in every sort.

### Mistake 2: the end is assigned and not maximized

In the merge, `current[1] = sorted[i][1]` shrinks the current interval if the next interval is nested. For `[1,10] [2,3] [4,5]`:

```text
without max on [1,10] [2,3] [4,5]: [1,3] [4,5]
with max on    [1,10] [2,3] [4,5]: [1,10]
```

The first line is the code without `max`: it loses the positions 6 to 10. The fix: `current[1] = Math.max(current[1], sorted[i][1])`.

### Mistake 3: the wrong tie order in the sweep

For the half-open meetings `[0,10) [10,20)`, the order "start before end" at time 10 counts both meetings as active. For the closed intervals `[0,10] [10,20]`, the order "end before start" counts one.

```text
half-open [0,10] [10,20]: ends first gives 1, starts first gives 2
closed    [0,10] [10,20]: starts first gives 2, ends first gives 1
```

The fix: look at the convention. Half-open intervals put the end first. Closed intervals put the start first.

### Mistake 4: the start as the sort key for the largest set without overlap

For `[1,100) [2,3) [4,5)`, the largest set has two intervals.

```text
sort by end on [1,100] [2,3] [4,5]: kept 2
sort by start on [1,100] [2,3] [4,5]: kept 1
```

The sort by start keeps `[1,100)` and blocks the two others. The fix: sort by end.

### Mistake 5: `<` against `<=`

The merge of LC 56 needs `<=`: the intervals `[1,4]` and `[4,5]` touch, and the problem merges them. The selection of LC 435 needs `>=`: the intervals `[1,2)` and `[2,3)` do not overlap, and the code keeps both.

```text
touching intervals merge:        [1,5]
with < in place of <=:           [1,4] [4,5]
```

```text
half-open [1,2] [2,3]: kept 2, with > instead of >=: kept 1
closed    [1,2] [2,3]: points 1
```

With `<` in the merge, the touching pair stays as two intervals. With `>` in the selection, the half-open pair loses one interval. The fix: write the convention of the problem before the first comparison, and test with a pair that touches.

### Mistake 6: the wrong pointer in an intersection

The code must drop the interval that ends first. If the code drops the interval that starts first, it can drop an interval that still meets the next interval of the other list.

```text
A [0,10], B [1,2] [3,4]
advance the smaller end:   [1,2] [3,4]
advance the smaller start: [1,2]
```

The fix: compare the ends.

### Mistake 7: the order of the input

There are two errors here, and they point in opposite directions.

**The three phases on a list that is not sorted.** The code of Example 1 is correct only for a sorted, disjoint list.

```text
list [6,7] [1,2] [3,5], insert [4,6]: [1,7]
list [1,2] [3,5] [6,7], insert [4,6]: [1,2] [3,7]
```

For the list `[6,7] [1,2] [3,5]`, phase 2 merges `[6,7]` first and then reaches back to `[1,2]`, so the answer joins `[1,2]` with the rest: `[1,7]`. The fix: use the merge of the anchor problem for an unsorted list.

**A sort of a list that is already sorted.** The code that appends the new interval and sorts again is correct. A common claim is that the extra sort "costs O(n log n)". That is the worst-case bound of a sort in general. The program counted the comparisons of `Arrays.sort(T[], Comparator)` on an array of `int[]` rows, with a counting comparator, under JDK 21. The appended interval `[1500,1501]` has a start in the middle of the list:

```text
sorted input, n = 1000: 999 comparisons
sorted input plus one interval appended, n = 1001: 1019 comparisons
shuffled input, n = 1000: 8674 comparisons
```

The sort for an array of objects is TimSort, an adaptive merge sort. The JDK 21 javadoc says that on a nearly sorted array it "requires approximately n comparisons". It made 999 comparisons for 1,000 sorted intervals, and 1,019 for the sorted list with one interval appended in the middle. A random list of 1,000 intervals needed 8,674. So for an array of objects the extra sort costs about `n` comparisons on this input. The page did not measure the sort of a primitive array (`Arrays.sort(int[])`), and the numbers do not carry over to it. The three-phase scan has a bound of O(n) that does not depend on the sort, and it needs no copy of the list, so it is still the answer that an interviewer expects. The mistake is not a wrong output. It is the failure to use the order that the input has.

### Mistake 8: `int` for accumulated times

In LC 2402, a delayed meeting ends at the time when its room frees, plus its duration. The delays add up, so an end time can be larger than any value in the input. For a room that frees at 2,000,000,000 and a meeting of length 2,000,000,000:

```text
int:  2000000000 + 2000000000 = -294967296
long: 2000000000 + 2000000000 = 4000000000
```

The `int` sum wraps to a negative number, so the room looks free at the start of time. The fix: store end times as `long`, and write `(long) a + b`.

### Mistake 9: no check for an empty input

The code `int[] current = sorted[0].clone()` reads the first interval. For an empty list it throws an exception.

```text
current = sorted[0] on an empty array throws: Index 0 out of bounds for length 0
```

The fix: `if (intervals.length == 0) return new int[0][];` before the first read.

### Mistake 10: the departure one cell too late in a difference array

A trip covers the positions `from` to `to - 1`. The code that writes the departure at `to + 1` keeps the passengers on board for one more position. For the trips `[3,1,5] [3,5,7]` and capacity 3, the first trip has left when the second boards.

```text
trips [3,1,5] [3,5,7], capacity 3: leave at to -> true, leave at to + 1 -> false
```

The fix: write the departure at `to`.

### Mistake 11: three defects in the skyline

The program ran three wrong versions of the skyline. Each group of lines shows the buildings, the correct answer, and the wrong answer.

```text
buildings [1,3,5] [3,6,2]
apply all events at x: [1,5] [3,2] [6,0]
read after each event: [1,5] [3,0] [3,2] [6,0]
buildings [0,2,3] [2,5,3]
compare with the previous height: [0,3] [5,0]
no comparison:                    [0,3] [2,3] [5,0]
buildings [1,4,3] [2,6,3]
count of each height: [1,3] [6,0]
set of heights:       [1,3] [4,0]
```

- Reading the height after each event writes `[3,0]` at a position where the outline never touches the ground. The fix: apply all events at one position, then read.
- Writing a key point at every position writes `[2,3]` where the height does not change. The fix: compare with the previous height.
- A set of heights removes the height 3 when the first of two buildings of height 3 ends. The fix: a `TreeMap` from height to count.

### Mistake 12: one neighbor, or two neighbors on bookings that overlap

The calendar test of LC 729 needs both `floorEntry` and `ceilingEntry`. The code that reads only the floor entry accepts `[3,6)` after `[5,8)` is booked.

```text
book [5,8]: wrong true, right true
book [3,6]: wrong true, right false
```

The two-neighbor test also needs booked intervals that do not overlap each other. If the booked set holds `[1,10)` and `[2,3)` (as in LC 731), the floor entry of 5 is `2=3`, and the test misses `[1,10)`.

```text
booked {1=10, 2=3}, new [5,6): floor 2=3, ceiling null, two-neighbor test says conflict = false
test against every booked interval says conflict = true
```

The fix: for LC 731 and LC 732, sweep the map of changes (Example 6).

### Mistake 13: the code sorts the caller's array

`Arrays.sort(intervals, ...)` reorders the array that the caller passed. If the caller needs the original order, the method must sort a copy.

```text
the caller's array after the sort: [1,2] [5,6]
the caller's array after sorting a copy: [5,6] [1,2]
```

The fix: `int[][] sorted = intervals.clone();`.

### Mistake 14: a greedy rule on intervals with weights

[LC 1235](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) looks like LC 435 and has a profit for each job. The rule "sort by end, keep what fits" does not maximize the profit.

```text
jobs [0,10) profit 100, [0,5) profit 1, [5,10) profit 1
sort by end and keep what fits: total profit 2
keeping only [0,10): total profit 100
```

The fix: dynamic programming. See [Pattern 15](../15-dynamic-programming/README.md).

### Mistake 15: two pointers on lists that are not sorted

The two-pointer scan of LC 986 is correct only for sorted, disjoint lists.

```text
A [5,6] [1,2], B [1,6]: two pointers give [5,6]
testing every pair gives [5,6] [1,2]
```

The fix: sort each list first, and merge the overlapping intervals of each list.

### The full program for the mistakes

The program `MistakeDemos` prints the outputs that this section quotes and that `IntervalTraces` does not print. It is in a collapsed block.

??? example "The full program: MistakeDemos"

    ```java
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;

    public class MistakeDemos {

        public static void main(String[] args) {
            comparatorOverflow();
            strictTest();
            emptyInput();
            sortsTheCallersArray();
            alreadySortedInput();
            threePhasesOnUnsortedInput();
            longEndTimes();
            weightedIntervals();
            twoNeighborsOnOverlappingBookings();
            intersectionsOnUnsortedLists();
            freeTimeWithNonStrictTest();
        }

        static String fmt(int[][] intervals) {
            StringBuilder sb = new StringBuilder();
            for (int[] iv : intervals) {
                if (sb.length() > 0) sb.append(' ');
                sb.append('[').append(iv[0]).append(',').append(iv[1]).append(']');
            }
            return sb.length() == 0 ? "(none)" : sb.toString();
        }

        static int[][] merge(int[][] sorted, boolean touchingMerges) {
            List<int[]> merged = new ArrayList<>();
            int[] current = sorted[0].clone();
            for (int i = 1; i < sorted.length; i++) {
                boolean overlap = touchingMerges ? sorted[i][0] <= current[1]
                        : sorted[i][0] < current[1];
                if (overlap) {
                    current[1] = Math.max(current[1], sorted[i][1]);
                } else {
                    merged.add(current);
                    current = sorted[i].clone();
                }
            }
            merged.add(current);
            return merged.toArray(new int[0][]);
        }

        static void comparatorOverflow() {
            System.out.println("== comparator overflow ==");
            int a = 2_000_000_000, b = -2_000_000_000;
            System.out.println("a - b = " + (a - b) + ", Integer.compare(a, b) = " + Integer.compare(a,
                    b));
            int[][] input = {{2_000_000_000, 2_000_000_005}, {-2_000_000_000, -1_999_999_995}};
            int[][] bySubtraction = input.clone();
            Arrays.sort(bySubtraction, (x, y) -> x[0] - y[0]);
            int[][] byCompare = input.clone();
            Arrays.sort(byCompare, (x, y) -> Integer.compare(x[0], y[0]));
            System.out.println("sorted with x[0] - y[0]:            " + fmt(bySubtraction));
            System.out.println("sorted with Integer.compare:        " + fmt(byCompare));
            System.out.println("merge after the subtraction sort:   " + fmt(merge(bySubtraction,
                    true)));
            System.out.println("merge after the Integer.compare sort: " + fmt(merge(byCompare, true)));
        }

        static void strictTest() {
            System.out.println("== strict test in the merge ==");
            int[][] touching = {{1, 4}, {4, 5}};
            System.out.println("touching intervals merge:        " + fmt(merge(touching, true)));
            System.out.println("with < in place of <=:           " + fmt(merge(touching, false)));
        }

        static void emptyInput() {
            System.out.println("== empty input ==");
            int[][] empty = new int[0][];
            try {
                int[] first = empty[0].clone();
                System.out.println(Arrays.toString(first));
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("current = sorted[0] on an empty array throws: " + e.getMessage());
            }
        }

        static void sortsTheCallersArray() {
            System.out.println("== sorting the caller's array ==");
            int[][] input = {{5, 6}, {1, 2}};
            Arrays.sort(input, (x, y) -> Integer.compare(x[0], y[0]));
            System.out.println("the caller's array after the sort: " + fmt(input));
            int[][] kept = {{5, 6}, {1, 2}};
            int[][] copy = kept.clone();
            Arrays.sort(copy, (x, y) -> Integer.compare(x[0], y[0]));
            System.out.println("the caller's array after sorting a copy: " + fmt(kept));
        }

        static void alreadySortedInput() {
            System.out.println("== sorting input that is already sorted ==");
            int n = 1000;
            int[][] sorted = new int[n][];
            for (int i = 0; i < n; i++) sorted[i] = new int[]{3 * i, 3 * i + 1};
            int[] count = {0};
            Arrays.sort(sorted.clone(), (x, y) -> {
                count[0]++;
                return Integer.compare(x[0], y[0]);
            });
            System.out.println("sorted input, n = 1000: " + count[0] + " comparisons");
            int[][] plusOne = Arrays.copyOf(sorted, n + 1);
            plusOne[n] = new int[]{1500, 1501};
            count[0] = 0;
            Arrays.sort(plusOne, (x, y) -> {
                count[0]++;
                return Integer.compare(x[0], y[0]);
            });
            System.out.println("sorted input plus one interval appended, n = 1001: " + count[0]
                    + " comparisons");
            int[][] shuffled = sorted.clone();
            java.util.Random random = new java.util.Random(7);
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int[] tmp = shuffled[i];
                shuffled[i] = shuffled[j];
                shuffled[j] = tmp;
            }
            count[0] = 0;
            Arrays.sort(shuffled, (x, y) -> {
                count[0]++;
                return Integer.compare(x[0], y[0]);
            });
            System.out.println("shuffled input, n = 1000: " + count[0] + " comparisons");
        }

        static int[][] insertWithoutSort(int[][] intervals, int[] added) {
            // The three phases applied to the input in the order that the caller gave.
            List<int[]> result = new ArrayList<>();
            int start = added[0], end = added[1];
            int i = 0;
            while (i < intervals.length && intervals[i][1] < start) result.add(intervals[i++]);
            while (i < intervals.length && intervals[i][0] <= end) {
                start = Math.min(start, intervals[i][0]);
                end = Math.max(end, intervals[i][1]);
                i++;
            }
            result.add(new int[]{start, end});
            while (i < intervals.length) result.add(intervals[i++]);
            return result.toArray(new int[0][]);
        }

        static void threePhasesOnUnsortedInput() {
            System.out.println("== three phases on unsorted input ==");
            int[][] unsorted = {{6, 7}, {1, 2}, {3, 5}};
            System.out.println("list " + fmt(unsorted) + ", insert [4,6]: "
                    + fmt(insertWithoutSort(unsorted, new int[]{4, 6})));
            int[][] sorted = {{1, 2}, {3, 5}, {6, 7}};
            System.out.println("list " + fmt(sorted) + ", insert [4,6]: "
                    + fmt(insertWithoutSort(sorted, new int[]{4, 6})));
        }

        static void longEndTimes() {
            System.out.println("== accumulated times ==");
            int roomFreeAt = 2_000_000_000;
            int duration = 2_000_000_000;
            int intEnd = roomFreeAt + duration;
            long longEnd = (long) roomFreeAt + duration;
            System.out.println("int:  " + roomFreeAt + " + " + duration + " = " + intEnd);
            System.out.println("long: " + roomFreeAt + " + " + duration + " = " + longEnd);
        }

        static void weightedIntervals() {
            System.out.println("== weighted intervals ==");
            int[][] jobs = {{0, 10, 100}, {0, 5, 1}, {5, 10, 1}};   // start, end, profit
            int[][] byEnd = jobs.clone();
            Arrays.sort(byEnd, (a, b) -> Integer.compare(a[1], b[1]));
            int kept = 0;
            long lastEnd = Long.MIN_VALUE;
            for (int[] job : byEnd) {
                if (job[0] >= lastEnd) {
                    kept += job[2];
                    lastEnd = job[1];
                }
            }
            System.out.println("jobs [0,10) profit 100, [0,5) profit 1, [5,10) profit 1");
            System.out.println("sort by end and keep what fits: total profit " + kept);
            System.out.println("keeping only [0,10): total profit " + jobs[0][2]);
        }

        static void twoNeighborsOnOverlappingBookings() {
            System.out.println("== two neighbors on bookings that overlap ==");
            java.util.TreeMap<Integer, Integer> booked = new java.util.TreeMap<>();
            booked.put(1, 10);                       // [1,10)
            booked.put(2, 3);                        // [2,3) lies inside [1,10)
            int start = 5, end = 6;                  // the new interval [5,6)
            java.util.Map.Entry<Integer, Integer> before = booked.floorEntry(start);
            java.util.Map.Entry<Integer, Integer> after = booked.ceilingEntry(start);
            boolean conflict = (before != null && before.getValue() > start)
                    || (after != null && after.getKey() < end);
            System.out.println("booked {1=10, 2=3}, new [5,6): floor " + before + ", ceiling " + after
                    + ", two-neighbor test says conflict = " + conflict);
            boolean real = false;
            for (java.util.Map.Entry<Integer, Integer> e : booked.entrySet()) {
                if (e.getKey() < end && start < e.getValue()) real = true;
            }
            System.out.println("test against every booked interval says conflict = " + real);
        }

        static void intersectionsOnUnsortedLists() {
            System.out.println("== intersections on an unsorted list ==");
            int[][] a = {{5, 6}, {1, 2}};
            int[][] b = {{1, 6}};
            List<int[]> result = new ArrayList<>();
            int i = 0, j = 0;
            while (i < a.length && j < b.length) {
                int lo = Math.max(a[i][0], b[j][0]);
                int hi = Math.min(a[i][1], b[j][1]);
                if (lo <= hi) result.add(new int[]{lo, hi});
                if (a[i][1] < b[j][1]) i++;
                else j++;
            }
            System.out.println("A " + fmt(a) + ", B " + fmt(b) + ": two pointers give "
                    + fmt(result.toArray(new int[0][])));
            List<int[]> everyPair = new ArrayList<>();
            for (int[] x : a) {
                for (int[] y : b) {
                    int lo = Math.max(x[0], y[0]);
                    int hi = Math.min(x[1], y[1]);
                    if (lo <= hi) everyPair.add(new int[]{lo, hi});
                }
            }
            System.out.println("testing every pair gives " + fmt(everyPair.toArray(new int[0][])));
        }

        static void freeTimeWithNonStrictTest() {
            System.out.println("== free time with a non-strict test ==");
            int[][] busy = {{1, 3}, {3, 5}};
            List<int[]> gaps = new ArrayList<>();
            int reach = busy[0][1];
            for (int i = 1; i < busy.length; i++) {
                // WRONG: >= in place of >
                if (busy[i][0] >= reach) gaps.add(new int[]{reach, busy[i][0]});
                reach = Math.max(reach, busy[i][1]);
            }
            System.out.println("busy [1,3] [3,5], test >=: " + fmt(gaps.toArray(new int[0][])));
        }
    }
    ```

    <!-- expected-output -->
    ```text
    == comparator overflow ==
    a - b = -294967296, Integer.compare(a, b) = 1
    sorted with x[0] - y[0]:            [2000000000,2000000005] [-2000000000,-1999999995]
    sorted with Integer.compare:        [-2000000000,-1999999995] [2000000000,2000000005]
    merge after the subtraction sort:   [2000000000,2000000005]
    merge after the Integer.compare sort: [-2000000000,-1999999995] [2000000000,2000000005]
    == strict test in the merge ==
    touching intervals merge:        [1,5]
    with < in place of <=:           [1,4] [4,5]
    == empty input ==
    current = sorted[0] on an empty array throws: Index 0 out of bounds for length 0
    == sorting the caller's array ==
    the caller's array after the sort: [1,2] [5,6]
    the caller's array after sorting a copy: [5,6] [1,2]
    == sorting input that is already sorted ==
    sorted input, n = 1000: 999 comparisons
    sorted input plus one interval appended, n = 1001: 1019 comparisons
    shuffled input, n = 1000: 8674 comparisons
    == three phases on unsorted input ==
    list [6,7] [1,2] [3,5], insert [4,6]: [1,7]
    list [1,2] [3,5] [6,7], insert [4,6]: [1,2] [3,7]
    == accumulated times ==
    int:  2000000000 + 2000000000 = -294967296
    long: 2000000000 + 2000000000 = 4000000000
    == weighted intervals ==
    jobs [0,10) profit 100, [0,5) profit 1, [5,10) profit 1
    sort by end and keep what fits: total profit 2
    keeping only [0,10): total profit 100
    == two neighbors on bookings that overlap ==
    booked {1=10, 2=3}, new [5,6): floor 2=3, ceiling null, two-neighbor test says conflict = false
    test against every booked interval says conflict = true
    == intersections on an unsorted list ==
    A [5,6] [1,2], B [1,6]: two pointers give [5,6]
    testing every pair gives [5,6] [1,2]
    == free time with a non-strict test ==
    busy [1,3] [3,5], test >=: [3,3]
    ```

## Practice Problems

The ladder goes from Easy to Hard. The column "What it adds" names the one new thing compared with the problem before it. A problem that the text solves in full says so: attempt that problem before you read the section, and use the text to check your work. The problems P1, P5, P6, P9, P10, and P11 have hints and a solution below the table.

| # | Problem | Difficulty | What it adds |
|---|---|---|---|
| P1 | [LC 1854: Maximum Population Year](https://leetcode.com/problems/maximum-population-year/) | Easy | The first difference array on a timeline, with a half-open lifetime and the earliest year on a tie |
| P2 | [LC 56: Merge Intervals](https://leetcode.com/problems/merge-intervals/) | Medium | The anchor problem. Solved in full in the Derivation: attempt it before you read that section |
| P3 | [LC 57: Insert Interval](https://leetcode.com/problems/insert-interval/) | Medium | The input is sorted and disjoint, so there is no sort. Solved in full in Worked Example 1: attempt it first |
| P4 | [LC 435: Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) | Medium | The code chooses, so the sort key is the end. Solved in full in Worked Example 2: attempt it first |
| P5 | [LC 646: Maximum Length of Pair Chain](https://leetcode.com/problems/maximum-length-of-pair-chain/) | Medium | The same rule as P4 with a strict test, because pairs that touch cannot follow each other |
| P6 | [LC 1288: Remove Covered Intervals](https://leetcode.com/problems/remove-covered-intervals/) | Medium | A sort by start with a tie rule on the end, and a test for "covered" in place of "overlapping" |
| P7 | [LC 253: Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) (premium) | Medium | A count of active intervals, with a tie order. Solved in full in Worked Example 3: attempt it first |
| P8 | [LC 986: Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/) | Medium | Two sorted lists and two pointers. Solved in full in Worked Example 4: attempt it first |
| P9 | [LC 2406: Divide Intervals Into Minimum Number of Groups](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/) | Medium | The convention flips the tie order of P7 |
| P10 | [LC 732: My Calendar III](https://leetcode.com/problems/my-calendar-iii/) | Hard | An online sweep over a `TreeMap` of changes, which returns the maximum |
| P11 | [LC 759: Employee Free Time](https://leetcode.com/problems/employee-free-time/) (premium) | Hard | The output is the gaps that the merge leaves open, from the lists of several employees |
| P12 | [LC 218: The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/) | Hard | Each interval has a height, so the sweep needs a structure that adds, removes, and reads the maximum. Solved in full in Worked Example 8: attempt it first |

Worked Example 5 solves [LC 1094](https://leetcode.com/problems/car-pooling/). Worked Example 6 solves [LC 729](https://leetcode.com/problems/my-calendar-i/) and [LC 731](https://leetcode.com/problems/my-calendar-ii/). Worked Example 7 solves [LC 252](https://leetcode.com/problems/meeting-rooms/) (premium). Attempt each before you read it.

### P1. Maximum Population Year (LC 1854, Easy)

Each person has a birth year and a death year. A person counts in the population of every year from the birth year up to, but not including, the death year. Return the earliest year with the largest population. For the people `[2,6] [4,9] [5,7] [8,10]` the answer is 5: three people are alive in year 5.

??? tip "Hint 1: where to look"

    Look at what changes between year `x` and year `x + 1`. How many people can enter or leave the population at one year boundary, and which people do that?

??? tip "Hint 2: the idea"

    The population changes only in a birth year (it rises) and in a death year (it falls). Store the changes. The population of a year is the sum of all changes up to that year.

??? tip "Hint 3: the plan"

    1. Find the first birth year and the last death year, so that the array covers the years that matter.
    2. Add 1 at the birth year and subtract 1 at the death year.
    3. Walk the years from left to right with a running sum. Remember the year where the running sum is larger than every earlier value.

??? success "Solution"

    The brute force counts the people alive in each year: O(Y x n) for `Y` years. The changes need only two writes for each person, and one pass gives every population, as in Worked Example 5. The program prints the array and the population for the example (years 2 to 10):

    ```text
    people (birth, death): [2,6] [4,9] [5,7] [8,10]
    years      2  3  4  5  6  7  8  9 10
    diff       1  0  1  1 -1 -1  1 -1 -1
    alive      1  1  2  3  2  1  2  1  0
    answer: 5
    one person [5,6]: 5, nobody: -1, two people alive in year 3 and year 7 (tie): 3
    ```

    The person `[2,6]` writes `+1` at year 2 and `-1` at year 6. In year 6, the first person has left and no one has entered, so the population falls from 3 to 2. The largest value is 3, and year 5 is the first year with that value.

    ```java
    // Each person is {birth, death}: alive in the years birth, birth + 1, ..., death - 1.
    // Returns the earliest year with the largest population, or -1 if there is nobody.
    static int maximumPopulationYear(int[][] logs) {
        if (logs.length == 0) return -1;
        int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE;
        for (int[] p : logs) {
            first = Math.min(first, p[0]);
            last = Math.max(last, p[1]);
        }
        int[] diff = new int[last - first + 1];     // one cell for each year first .. last
        for (int[] p : logs) {
            diff[p[0] - first]++;                   // alive from the birth year
            diff[p[1] - first]--;                   // not alive from the death year
        }
        int alive = 0, best = 0, bestYear = first;
        for (int i = 0; i < diff.length; i++) {
            alive += diff[i];
            // > : the first year with the largest count wins
            if (alive > best) {
                best = alive;
                bestYear = first + i;
            }
        }
        return bestYear;
    }
    ```

    The test `alive > best` is strict, so a later year with the same count does not replace the earlier year. For the people `[3,4] [7,8]`, years 3 and 7 both have one person, and the code returns 3. Time O(n + Y), space O(Y), where `Y` is the span from the first birth year to the last death year. If the span is too large for an array, sort the events as in Example 3.

    The mistake that this problem invites: writing the decrement at `death + 1`. A person is not alive in the death year, so the decrement belongs at `death`. With `death + 1`, the person counts in one more year.

### P5. Maximum Length of Pair Chain (LC 646, Medium)

Each pair is `[a, b]` with `a < b`. A pair `[c, d]` can follow a pair `[a, b]` if `b < c`. Choose any subset of the pairs, in any order, to form the longest chain. For `[1,2] [2,3] [3,4] [1,6] [7,8]` the answer is 3: the chain `[1,2] [3,4] [7,8]`.

??? tip "Hint 1: where to look"

    A chain cannot hold two pairs that touch or overlap, and the code may pick the pairs in any order and may skip any pair. What kind of set of pairs does a chain describe?

??? tip "Hint 2: the idea"

    A chain is a set of pairs in which no two pairs overlap or touch. The rule that leaves the most room is the same as in Worked Example 2. The difference is in the test for pairs that touch.

??? tip "Hint 3: the plan"

    1. Sort the pairs by end.
    2. Keep the end of the last pair in the chain.
    3. Take a pair if its start is strictly larger than that end.

??? success "Solution"

    The brute force sorts by start and fills a table: `dp[i]` is the longest chain that ends with pair `i`, and each entry looks at all earlier pairs, O(n^2). The exchange argument of Worked Example 2 holds for this problem: after the sort by end, the pair with the smallest end leaves the most room, so some longest chain contains it. The difference is the touching case. The pair `[2,3]` does not follow `[1,2]`, because `2 < 2` is false. So the test is `start > lastEnd` and not `start >= lastEnd`.

    ```text
    pairs [1,2] [2,3] [3,4] [1,6] [7,8]: longest chain 3
    empty: 0, one: 1
    sorted by end: [1,2] [2,3] [3,4] [1,6] [7,8]
    [1,2]: start 1 > last end none -> take, length = 1
    [2,3]: start 2 <= last end 2 -> skip
    [3,4]: start 3 > last end 2 -> take, length = 2
    [1,6]: start 1 <= last end 4 -> skip
    [7,8]: start 7 > last end 4 -> take, length = 3
    with >= in place of >: 4
    ```

    The trace takes `[1,2]`, skips `[2,3]` (`2 <= 2`), takes `[3,4]`, skips `[1,6]`, and takes `[7,8]`. The last line shows the effect of the mistake: with `>=` the code takes `[2,3]` too and reports 4, and no chain of four pairs exists.

    ```java
    // A pair [c, d] can follow [a, b] only if b < c. Returns the longest chain.
    static int longestPairChain(int[][] pairs) {
        int[][] sorted = pairs.clone();
        Arrays.sort(sorted, (a, b) -> Integer.compare(a[1], b[1]));   // sort by END
        int length = 0;
        long lastEnd = Long.MIN_VALUE;
        for (int[] p : sorted) {
            // > : the next pair must start AFTER the end
            if (p[0] > lastEnd) {
                length++;
                lastEnd = p[1];
            }
        }
        return length;
    }
    ```

    Time O(n log n), space O(n). The test must also hold for an empty input (0) and a single pair (1).

### P6. Remove Covered Intervals (LC 1288, Medium)

An interval `[a, b)` is covered by an interval `[c, d)` if `c <= a` and `b <= d`. The traces below write the intervals as `[a,b]`. Remove every interval that another interval covers, and return the number of intervals that remain. For `[1,4) [3,6) [2,8) [2,5) [7,9)` the answer is 3. The interval `[3,6)` and the interval `[2,5)` are covered by `[2,8)`.

??? tip "Hint 1: where to look"

    In a pair where one interval covers the other, which of the two coordinates of the covering interval is the smaller, and which is the larger? Which intervals can cover a given interval, and what do they have in common?

??? tip "Hint 2: the idea"

    After the sort by start, the next interval is covered exactly when an earlier interval reaches at least as far to the right. So the code needs the largest end so far. Intervals with the same start need an order.

??? tip "Hint 3: the plan"

    1. Sort by start ascending. For equal starts, sort by end descending.
    2. Keep the largest end so far.
    3. Count an interval if its end is larger than that value, and update the value.

??? success "Solution"

    The brute force tests every pair: O(n^2). After the sort by start, an earlier interval has `c <= a`, so the only open question is `b <= d`. If any earlier interval has `d >= b`, the one with the largest end does, so one number is enough: the largest end so far. This is the `reach` of the merge.

    Two intervals can have the same start. If the shorter one comes first, the code counts it as not covered, and then counts the longer one, so it counts two intervals where one covers the other. The longer interval must come first. The program shows both orders for `[1,2] [1,9]`:

    ```text
    intervals [1,2] [1,9]: longer first gives 1, ties in input order gives 2
    ```

    The trace for the example:

    ```text
    sorted by start, then longer first: [1,4] [2,8] [2,5] [3,6] [7,9]
    [1,4]: end 4 > reach none -> keep, kept = 1
    [2,8]: end 8 > reach 4 -> keep, kept = 2
    [2,5]: end 5 <= reach 8 -> covered
    [3,6]: end 6 <= reach 8 -> covered
    [7,9]: end 9 > reach 8 -> keep, kept = 3
    answer: 3
    empty: 0, one: 1, same twice: 1, same start [1,2] [1,9]: 1
    ```

    ```java
    // Counts the intervals that no other interval covers. [a, b] is covered by [c, d] if c <= a and
    // b <= d.
    static int countNotCovered(int[][] intervals) {
        int[][] sorted = intervals.clone();
        // Start ascending. For equal starts, the longer interval first, so it covers the shorter
        // one.
        Arrays.sort(sorted, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                : Integer.compare(b[1], a[1]));
        int kept = 0;
        // the largest end among the earlier intervals
        int reach = Integer.MIN_VALUE;
        for (int[] iv : sorted) {
            if (iv[1] > reach) {                        // it ends beyond every earlier interval
                kept++;
                reach = iv[1];
            }                                           // otherwise an earlier interval covers it
        }
        return kept;
    }
    ```

    The interval `[2,8]` comes before `[2,5]` because of the tie rule, so `[2,5]` is covered (`5 <= 8`). The code starts `reach` at `Integer.MIN_VALUE`, so the first interval always counts, and an empty input gives 0. Time O(n log n), space O(n). The method counts two equal intervals as one remaining interval, because the second one has `end <= reach`.

### P9. Divide Intervals Into Minimum Number of Groups (LC 2406, Medium)

The input is a list of closed intervals. Put the intervals in groups so that no two intervals of a group share a position. Return the smallest number of groups. For `[1,5] [5,8] [9,12]` the answer is 2: the intervals `[1,5]` and `[5,8]` share the position 5, so they need different groups.

??? tip "Hint 1: where to look"

    Suppose some position is covered by `k` intervals. What does the problem say about those `k` intervals, and what does that give as a lower bound on the number of groups?

??? tip "Hint 2: the idea"

    The answer is the largest number of intervals that cover one position. This is the question of Meeting Rooms II. The convention is different: closed intervals share their end position.

??? tip "Hint 3: the plan"

    1. Turn each interval into a start event and an end event.
    2. Sort the events by time. At equal times, the start comes first.
    3. Scan with a counter, and return its largest value.

??? success "Solution"

    The lower bound: the `k` intervals that cover one position pairwise share that position, so they need `k` different groups. The upper bound: process the intervals by start. Give each interval a group in which the last interval has ended before the new start. If no such group exists, every group has an interval that covers the new start, so the position is covered by `groups + 1` intervals. So the number of groups never exceeds the largest overlap. The answer equals the largest overlap.

    For closed intervals, the end position belongs to the interval, so at equal times the start must come first. The code encodes an event as the number `time * 2` for a start and `time * 2 + 1` for an end. A sort of the numbers puts a start before an end at the same time. The trace for the example:

    ```text
    time 1 start -> active 1
    time 5 start -> active 2
    time 5 end   -> active 1
    time 8 end   -> active 0
    time 9 start -> active 1
    time 12 end   -> active 0
    ```

    At time 5, the start of `[5,8]` comes before the end of `[1,5]`, and the counter reaches 2.

    ```java
    // Closed intervals. Returns the fewest groups such that no two intervals of a group share a
    // point.
    static int minGroups(int[][] intervals) {
        long[] events = new long[intervals.length * 2];
        int k = 0;
        for (int[] iv : intervals) {
            // a start: the low bit is 0, so it sorts first
            events[k++] = iv[0] * 2L;
            // an end: the low bit is 1, so it sorts after
            events[k++] = iv[1] * 2L + 1;
        }
        Arrays.sort(events);
        int active = 0, most = 0;
        for (long event : events) {
            if ((event & 1) == 0) {
                active++;
                most = Math.max(most, active);
            } else {
                active--;
            }
        }
        return most;
    }
    ```

    The program prints the answer of the closed order, and the answer of the sweep with the half-open order (the end first) for the same input:

    ```text
    closed [1,5] [5,8] [9,12]: groups 2, as half-open the sweep gives 1
    empty: 0, one: 1
    ```

    The first number is correct. The half-open order says 1 group for the closed input, which is wrong. Time O(n log n), space O(n). The encoding uses `long`, so `time * 2 + 1` does not overflow for any `int` time.

### P10. My Calendar III (LC 732, Hard)

Build a calendar class with a method `book(start, end)` for the half-open interval `[start, end)`. The method adds the interval and returns the largest number of added intervals that cover one position. Every booking is accepted. For the bookings `[1,5)`, `[5,9)`, `[3,6)` the answers are 1, 1, 2.

??? tip "Hint 1: where to look"

    The answer after each booking depends on how many intervals cover each position. At which positions can that count change, and does the code need the intervals themselves?

??? tip "Hint 2: the idea"

    The count changes only at starts and ends. Store, for each time, the net change in the count, in a map that stays sorted by time. The largest running sum over the sorted map is the answer.

??? tip "Hint 3: the plan"

    1. In a sorted map from time to change, add +1 at `start` and -1 at `end`.
    2. Walk the map in key order with a running sum.
    3. Return the largest value of the running sum.

??? success "Solution"

    The brute force keeps the list of bookings and counts the overlap at every position after each booking: O(n x C) for `C` positions. The map of changes is the structure of Worked Example 6 (LC 731), and this problem drops the rejection. Each booking sweeps the whole map.

    ```java
    static class MyCalendarThree {
        private final TreeMap<Integer, Integer> delta = new TreeMap<>();

        int book(int start, int end) {
            delta.merge(start, 1, Integer::sum);
            delta.merge(end, -1, Integer::sum);
            int active = 0, most = 0;
            for (int change : delta.values()) {
                active += change;
                most = Math.max(most, active);                  // the largest overlap so far
            }
            return most;
        }
    }
    ```

    The trace:

    ```text
    first booking [1,5): 1, second [5,9): 1, third [3,6): 2
    ```

    The booking `[5,9)` starts at the time where `[1,5)` ends. The map holds `-1 + 1 = 0` at time 5, so the running sum does not rise, and the answer stays 1. This is the half-open tie order of Example 3. The booking `[3,6)` covers the positions 3 and 4 together with `[1,5)`, and the answer rises to 2.

    Time O(n) for each booking, because the map has up to `2n` entries. Space O(n). The mistake that this problem invites: returning the largest single change and not the largest running sum.

### P11. Employee Free Time (LC 759, Hard, premium)

Each employee has a list of busy intervals, sorted by start, with no two intervals of one employee overlapping. Return the list of intervals in which every employee is free, as finite intervals of positive length, in order. The time before the earliest busy start and after the latest busy end is not finite, so it is not part of the answer. The LeetCode version gives the intervals as objects of a class `Interval`. This page uses `int[]` pairs. For employee A busy in `[1,3] [6,7]`, employee B in `[2,4]`, and employee C in `[2,5] [9,12]`, the answer is `[5,6] [7,9]`.

??? tip "Hint 1: where to look"

    A time at which every employee is free is a time at which no busy interval of any employee covers it. What does the set of such times look like between busy intervals?

??? tip "Hint 2: the idea"

    A free time is a gap between merged busy intervals. The merge of LC 56 finds a gap at the place where it closes an interval. Write the gap instead of closing the interval.

??? tip "Hint 3: the plan"

    1. Put the busy intervals of all employees in one list, and sort it by start.
    2. Keep the largest end so far.
    3. If the next interval starts after that end, the stretch between them is free. Write it.
    4. Update the largest end.

??? success "Solution"

    The brute force tests, for each position between the first start and the last end, whether any interval covers it. The positions can be large, and the merge avoids the test of each position. After the sort by start, the next interval `[s, e]` starts after the largest end `reach` exactly when `s > reach`, and then the stretch from `reach` to `s` is covered by no interval: every earlier interval ends at or before `reach`, and every later interval starts at or after `s`. The test is strict, because two busy intervals that touch leave no gap of positive length. The program prints the scan, with `reach` after each step:

    ```text
    employee A: [1,3] [6,7]
    employee B: [2,4]
    employee C: [2,5] [9,12]
    all busy intervals sorted by start: [1,3] [2,4] [2,5] [6,7] [9,12]
    take [1,3], reach = 3
    [2,4]: start 2 <= reach 3 -> no gap
        reach = 4
    [2,5]: start 2 <= reach 4 -> no gap
        reach = 5
    [6,7]: start 6 > reach 5 -> free [5,6]
        reach = 7
    [9,12]: start 9 > reach 7 -> free [7,9]
        reach = 12
    free time: [5,6] [7,9]
    ```

    ```java
    // Each employee has a sorted list of busy intervals. Returns the gaps of positive length
    // in which nobody is busy, between the earliest start and the latest end.
    static List<int[]> employeeFreeTime(List<List<int[]>> schedules) {
        List<int[]> all = new ArrayList<>();
        for (List<int[]> schedule : schedules) all.addAll(schedule);
        List<int[]> free = new ArrayList<>();
        if (all.isEmpty()) return free;
        all.sort((a, b) -> Integer.compare(a[0], b[0]));       // sort by start, as in the merge
        int reach = all.get(0)[1];                              // the largest end so far
        for (int i = 1; i < all.size(); i++) {
            int[] busy = all.get(i);
            if (busy[0] > reach) {                              // > : touching leaves no gap
                // the merge would close an interval here
                free.add(new int[]{reach, busy[0]});
            }
            reach = Math.max(reach, busy[1]);
        }
        return free;
    }
    ```

    The edge cases:

    ```text
    touching busy [1,3] [3,5]: (none)
    nobody is busy: (none)
    one busy interval: (none)
    ```

    Two touching busy intervals, `[1,3]` and `[3,5]`, leave no gap. If nobody is busy, or if there is one busy interval, no finite gap exists, so the answer is empty. Time O(N log N) for `N` busy intervals in total, space O(N). Each list is already sorted, so a `k`-way merge with a heap gives the sorted order in O(N log k) for `k` employees ([Pattern 16](../16-heap-priority-queue/README.md)).

    The mistake that this problem invites: the test `s >= reach`, which writes the zero-length gap `[3,3]` for two busy intervals that touch.

    ```text
    busy [1,3] [3,5], test >=: [3,3]
    ```

### More problems by difficulty

| Problem | LC # | What it adds, or where this page solves it |
|---|---|---|
| [LC 252: Meeting Rooms](https://leetcode.com/problems/meeting-rooms/) (premium) | 252 | Easy. Sort by start and compare neighbors. Worked Example 7 |
| [LC 228: Summary Ranges](https://leetcode.com/problems/summary-ranges/) | 228 | Easy. Build ranges from a sorted array in one scan. It has no overlap to test |
| [LC 495: Teemo Attacking](https://leetcode.com/problems/teemo-attacking/) | 495 | Easy. The poisoned time is the union of intervals of equal length: for each attack, add the smaller of the duration and the gap to the next attack, and add the full duration for the last attack |
| [LC 1094: Car Pooling](https://leetcode.com/problems/car-pooling/) | 1094 | Medium. Worked Example 5, a difference array and the same load with sorted events |
| [LC 1109: Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) | 1109 | Medium. A difference array, then a prefix sum. [Pattern 04](../04-prefix-sum/README.md) solves it in full |
| [LC 452: Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) | 452 | Medium. Sort by end, with closed intervals. [Pattern 14](../14-greedy/README.md) solves it in full (its practice problem P6) |
| [LC 729: My Calendar I](https://leetcode.com/problems/my-calendar-i/) | 729 | Medium. Worked Example 6, `floorEntry` and `ceilingEntry` |
| [LC 731: My Calendar II](https://leetcode.com/problems/my-calendar-ii/) | 731 | Medium. Worked Example 6, a `TreeMap` of changes with an undo |
| [LC 1229: Meeting Scheduler](https://leetcode.com/problems/meeting-scheduler/) (premium) | 1229 | Medium. Find the earliest slot of a given length that fits two people. The problem guarantees that the slots of one person do not overlap. Sort each list by start. The common slots are the intersections of LC 986, and the code returns the first one that is long enough |
| [LC 2402: Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/) | 2402 | Hard. Two heaps (free rooms by number, busy rooms by end time), with `long` end times |
| [LC 1851: Minimum Interval to Include Each Query](https://leetcode.com/problems/minimum-interval-to-include-each-query/) | 1851 | Hard. Sort the queries and the intervals. Keep a min heap, ordered by size, of the intervals that start at or before the query. Remove a top interval that ends before the query |
| [LC 352: Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/) | 352 | Hard. A `TreeMap` of disjoint intervals from start to end. A new value joins the interval that ends right before it and the interval that starts right after it, if they exist |
| [LC 715: Range Module](https://leetcode.com/problems/range-module/) | 715 | Hard. A `TreeMap` of disjoint intervals from start to end. The code adds a range by replacing the intervals that it overlaps with one interval, and removes a range by trimming or splitting each interval that it overlaps |
| [LC 1235: Maximum Profit in Job Scheduling](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) | 1235 | Hard. Not this pattern: each job has a profit, and the greedy rule fails. [Pattern 15](../15-dynamic-programming/README.md) |

### Tell Them Apart

For each pair, decide which technique each problem needs before you open the answer.

??? question "Pair 1: [LC 435: Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) (the fewest removals) and [LC 1235: Maximum Profit in Job Scheduling](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) (the largest total profit)"

    [LC 435](https://leetcode.com/problems/non-overlapping-intervals/) needs the greedy rule "earliest end first". [LC 1235](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) needs dynamic programming.

    The deciding property is the weight. In LC 435 every interval counts one, so the exchange argument can replace the first interval of any best set by the interval with the smallest end, and the size does not change. With profits, the swap can replace a profit of 100 by a profit of 1. The jobs `[0,10)` with profit 100, `[0,5)` with profit 1, and `[5,10)` with profit 1 give a profit of 2 for the greedy rule and 100 for the best choice (Common Mistakes, mistake 14). See [Pattern 15](../15-dynamic-programming/README.md), and [Pattern 14](../14-greedy/README.md) for the exchange argument.

??? question "Pair 2: [LC 56: Merge Intervals](https://leetcode.com/problems/merge-intervals/) and [LC 435: Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/)"

    Both sort a list of intervals and scan it once. LC 56 sorts by start. LC 435 sorts by end.

    The deciding property is whether the code has a choice. In LC 56 the answer is fixed by the input, so there is nothing to optimize, and the start order lets one number (`reach`) summarize the past. In LC 435 the code chooses which intervals to keep, and the choice needs a rule that has a proof: the earliest end. The touching case also differs: LC 56 merges `[1,4]` and `[4,5]`, and LC 435 keeps `[1,2)` and `[2,3)`.

??? question "Pair 3: [LC 253: Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) (premium) and [LC 2402: Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/)"

    [LC 253](https://leetcode.com/problems/meeting-rooms-ii/) needs a count. A sweep over sorted events answers it, and a heap is optional. [LC 2402](https://leetcode.com/problems/meeting-rooms-iii/) needs a heap.

    The deciding property is whether the answer names a resource. LC 253 asks for the number of rooms, which equals the largest overlap. LC 2402 gives each room a number, delays a meeting until a room is free, and asks which room hosts the most meetings. The code must know which room frees first, and the room with the smallest number among the free rooms. A counter cannot do that. See [Pattern 16](../16-heap-priority-queue/README.md).

??? question "Pair 4: [LC 1109: Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) (many range updates, one read) and [LC 303: Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/) (one build, many range reads)"

    Both use a running sum over an array. LC 1109 needs a difference array. LC 303 needs a prefix sum array.

    The deciding property is the direction. In LC 1109, the code changes ranges and reads the array once at the end, so it records two changes for each range and sums once. In LC 303, the code reads many ranges of a fixed array, so it sums once at the start, and each read subtracts two prefix sums. The difference array is the inverse of the prefix sum. See [Pattern 04](../04-prefix-sum/README.md).

## Interview Follow-ups

??? question "Why do you sort by start for a merge, and what does the sort give you?"

    The sort gives the code an order in which a gap in the list is a gap in the positions. After the sort by start, the next interval starts at or after every earlier start. So it overlaps an earlier interval only if it starts at or before the largest end so far. The code keeps that one number, `reach`, in place of the list of earlier intervals. If the next interval starts after `reach`, no later interval can reach back, and the code closes the current interval. The sort costs O(n log n), and the scan costs O(n).

??? question "Why is the `max` needed when you extend the current interval?"

    The next interval can lie inside the current one. For `[1,10] [2,3] [4,5]`, the second interval ends at 3. Without the `max`, the code sets the end to 3, and then `4 > 3` closes the interval and the answer is `[1,3] [4,5]`. The right answer is `[1,10]`. The `max` keeps the largest end, which is the number that the next comparison needs.

??? question "How does the code change if the problem uses half-open intervals?"

    The overlap test comes from the negation of "one ends before the other starts". For half-open intervals, "ends before" means `a.end <= b.start`, so the overlap test is `b.start < a.end`. For closed intervals, it is `a.end < b.start`, so the overlap test is `b.start <= a.end`. Only touching intervals change the result: `[1,3]` and `[3,5]` merge as closed intervals and stay separate as half-open intervals. I would write the convention first, and then test with a pair that touches.

??? question "Why does sorting by end work for the largest set without overlap, and why does sorting by start fail?"

    The interval with the smallest end leaves the most room. Take any best set, and replace its first interval by the interval with the smallest end overall. Every other interval of the set starts at or after the old end, so it starts at or after the new end. The set stays valid and keeps its size, so some best set contains that interval. The same argument applies to the intervals that remain. The sort by start fails on `[1,100) [2,3) [4,5)`: it keeps `[1,100)` and gets 1, and the best set has 2. [Pattern 14](../14-greedy/README.md) has the full proof.

??? question "Solve the number of meeting rooms two ways. When is each better?"

    The first way is a sweep: turn each meeting into a start event and an end event, sort them with the end first at equal times, and keep the largest value of a running counter. The second way is a min heap: sort by start, and for each meeting remove the smallest end time if it is at most the start, then add the end of this meeting. The answer is the heap size. Both cost O(n log n). The sweep is shorter and gives only the count. The heap holds one end time for each room, so it can also tell which room is free first, which [LC 2402](https://leetcode.com/problems/meeting-rooms-iii/) needs.

??? question "What changes if the intervals are closed?"

    The tie order flips. For half-open meetings, the end comes first at equal times, because a meeting that ends at 10 frees its room at 10. For closed intervals, the end position belongs to the interval, so `[1,5]` and `[5,8]` are both active at 5. The start must come first. In the heap form, the test for a free room changes from `peek() <= start` to `peek() < start`. [LC 2406](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/) is the closed case.

??? question "The coordinates go up to 1,000,000,000. Can you still use a difference array?"

    A difference array needs one cell for each position. For 1,000,000,000 positions, an `int` array uses about 4 GB, which is too much. The events carry the same information: each interval gives two events, and a sort puts them in order of position. The code scans the sorted events with a running sum, in O(n log n) time and O(n) space. The difference array is the same sweep with a bucket sort in place of the comparison sort, so its time O(n + C) is below O(n log n) only when the number of positions `C` is small.

??? question "The requests arrive one at a time and each needs an answer. What structure do you use?"

    A `TreeMap`. For the question "does the new interval conflict with the booked ones?" and a booked set that does not overlap itself, only two booked intervals matter: the one with the largest start at or before the new start (`floorEntry`), and the one with the smallest start at or after it (`ceilingEntry`). Each lookup costs O(log n), and so does the insert. If the booked intervals may overlap, as in [LC 731](https://leetcode.com/problems/my-calendar-ii/), the code keeps a `TreeMap` of changes in the count and sweeps it, in O(n) for each request.

??? question "How do you merge the free time of several employees?"

    Put all busy intervals in one list, sort by start, and run the merge scan. At the place where the merge would close an interval (`start > reach`), write the stretch from `reach` to `start` as free time. The test is strict, so two busy intervals that touch leave no gap. The cost is O(N log N) for `N` intervals. If each employee list is sorted, a `k`-way merge with a min heap gives the sorted order in O(N log k).

??? question "How does the skyline code stay at O(n log n)?"

    It sorts the `2n` edge events once, and each event makes one operation on a `TreeMap` from height to count: add, subtract, or read the largest key. Each operation costs O(log n). A `PriorityQueue` with `remove(Object)` would cost O(n) for each removal, so the code would take O(n^2). A max heap with lazy deletion also gives O(n log n), and it needs a map of pending removals.

??? question "Can you do better than O(n log n) for the merge?"

    If the positions are bounded by a small number `C`, a bucket for each start position replaces the comparison sort. The code puts each interval in the bucket of its start, keeps the largest end for each bucket, and scans the buckets from left to right. The cost is O(n + C). If the positions are not bounded, the code uses the comparison sort, and the sort is the largest cost.

??? question "What if the input has intervals with a start equal to the end?"

    The answer depends on the convention. A half-open interval `[3,3)` holds no position, so it must not count as active. In the sweep, its end event goes before its start event at the same time, so the counter does not rise. A closed interval `[3,3]` holds one position, and the closed order (start first) counts it as active at 3. I would ask the interviewer, and I would test the zero-length interval with the code before I submit.

### Before you write the code

1. Write the convention of the problem (closed or half-open) and decide what touching intervals do.
2. Pick the question in the decision table of The Pattern: union or gaps, largest set without overlap, active count, assignment, load, online booking, or two lists.
3. Write the steps of the template in words: the sort key, the summary, and the test.
4. Write the code, and test it with touching intervals, nested intervals, an empty list, and one interval.

### How to say it in an interview

1. "The input is a list of intervals and the question is about overlap. The convention is [closed or half-open], so touching intervals [overlap or do not overlap]. I will write that down first."
2. "Testing every pair costs O(n^2). A sort lets me compare each interval with one summary of the earlier intervals."
3. "The question is [the union, the largest set without overlap, the number active at one time], so I sort by [start, end, the events]. The summary is [the largest end so far, the end of the last interval kept, a counter, a heap of end times]."
4. "The invariant: after the first `i` intervals in the sorted order, the summary holds [what the later intervals need to know]."
5. "The sort costs O(n log n) and the scan costs O(n)."
6. "Let me trace two touching intervals and one nested interval before I code." Then trace them.
7. After the code: "These are my edge cases: an empty list, one interval, equal intervals, nested intervals, and touching intervals."

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| Greedy ([Pattern 14](../14-greedy/README.md)) | Activity selection ([LC 435](https://leetcode.com/problems/non-overlapping-intervals/), [LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/)) is a greedy problem. Pattern 14 covers the general exchange argument. This page covers the interval-specific sort keys and the merge, which has no choice to prove | Does the code choose a subset? If yes, it needs a proof of the rule (sort by end). If the answer is fixed by the input (merge, gaps), sort by start and scan |
| Heap ([Pattern 16](../16-heap-priority-queue/README.md)) | A heap appears in [LC 253](https://leetcode.com/problems/meeting-rooms-ii/) (premium) and [LC 2402](https://leetcode.com/problems/meeting-rooms-iii/), but the first step is the sort by start. Pattern 16 covers questions of the form "the k-th" or "the top k" with no overlap structure | Does the question ask for a count of active intervals, or for the resource that frees first? A count needs a sweep. A resource needs a heap |
| Prefix sum and difference array ([Pattern 04](../04-prefix-sum/README.md)) | [LC 1094](https://leetcode.com/problems/car-pooling/) is a difference array. It is the sweep line with one cell for each position | Are the positions small integers, and are all updates known first? If yes, a difference array. If not, sorted events |
| Weighted intervals ([Pattern 15](../15-dynamic-programming/README.md)) | [LC 1235](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) looks like LC 435 and has a profit for each job. The greedy rule fails | Does each interval have a weight that the answer adds up? If yes, dynamic programming with binary search |
| Two pointers ([Pattern 02](../02-two-pointers/README.md)) | [LC 986](https://leetcode.com/problems/interval-list-intersections/) is two pointers on two sorted lists of intervals | Are there two sorted inputs, and does each step drop one interval? If yes, advance the pointer of the interval that ends first |
| Balanced trees and `TreeMap` ([Foundations: trees](../foundations/05-trees-and-balanced-bsts.md)) | The online booking problems need a sorted structure that changes between requests | Does each request need its answer before the next one arrives? If yes, a `TreeMap`. If all requests are known first, sort once |

For a side-by-side guide across all patterns, see [How to Choose a Pattern](../how-to-choose-a-pattern.md).

## Quick Reference Card

**When to use.** The input is a list of intervals (or booking requests) and the question is about overlap: the union, the gaps, the largest set without overlap, the number active at one time, the load at each position, a conflict with earlier bookings, or the outline.

**The invariant.** After the code has processed the first `i` items in the sorted order, one summary holds what the later items need to know: the largest end so far (merge), the end of the last interval kept (selection), a counter (concurrency), a heap of end times (assignment), a map of active heights (skyline).

**The overlap test** (after the sort by start, for a start `s` and the largest end `reach`):

| Convention | The next interval overlaps the current one when |
|---|---|
| Closed `[s, e]` | `s <= reach` |
| Half-open `[s, e)` | `s < reach` |

| Template | Sort key or structure | Rule |
|---|---|---|
| Merge, free time, any overlap | Start | `s <= reach`: extend to `max(reach, e)`. Else close the interval (or write the gap) |
| Insert into a sorted list | None | Copy the intervals that end before the new one, widen the new one over the overlapping intervals, copy the rest |
| Largest set without overlap | End | Keep an interval if `start >= lastEnd` (half-open), or `start > lastPoint` (closed, and chains) |
| Active count | Events | Sort by time. Half-open: end first. Closed: start first. Keep the largest counter |
| Assignment of rooms | Start, and a min heap of ends | If `peek() <= start`, `poll`. Then `offer(end)` |
| Two sorted lists | None | `lo = max(starts)`, `hi = min(ends)`. If `lo <= hi`, write it. Drop the interval that ends first |
| Load at each position | Difference array | `+v` at `from`, `-v` at `to`, then a prefix sum |
| Online booking | `TreeMap` | Floor and ceiling entries. Or a map of changes and a sweep |
| Skyline | Events and a `TreeMap` of counts | Apply all events at one position, then compare the tallest height with the previous one |

**Complexity.** Sort and scan: O(n log n) time, O(n) space. Insert into a sorted list and two sorted lists: O(n) and O(m + n). Difference array: O(n + C). One `TreeMap` booking: O(log n).

**Java.** `Integer.compare(a[0], b[0])` in every sort, and never `a[0] - b[0]`. `clone()` the array and the rows that the code changes. `long` for accumulated times. `TreeMap.floorEntry`, `ceilingEntry`, and `merge` for bookings.

**The top mistakes.**

1. The end is assigned and not maximized, so a nested interval shrinks the current interval.
2. The wrong tie order in the sweep, or `<` against `<=` for touching intervals.
3. The start as the sort key for the largest set without overlap.
4. The wrong pointer in an intersection (compare the ends, not the starts).
5. A comparator that subtracts, `int` for accumulated times, and no check for an empty input.
6. A skyline that reads the height after each event, a set where a count is needed, or a key point without a change.
7. A greedy rule on intervals that carry a weight.

## Mastery Checklist

The levels are from the [Mastery Tracker](../../mastery-tracker.md). Test each statement with the page closed.

- [ ] **L2** Understand: I trace the merge on a new list of 6 unsorted intervals that includes a nested pair and a touching pair, and write `reach` and the result after each step.
- [ ] **L2** Understand: I trace the event sweep for meeting rooms on 5 new meetings, with two events at the same time, and write the counter after each event.
- [ ] **L3** Explain: I explain in under 3 minutes why the merge sorts by start, why the end is a `max`, and why the scan is correct, and I give the input on which the code without the `max` fails.
- [ ] **L3** Explain: I explain in under 3 minutes why the largest set without overlap sorts by end, with the exchange argument and the input `[1,100) [2,3) [4,5)` on which the sort by start fails. I state the tie order of the sweep for closed and for half-open intervals.
- [ ] **L4** Solve a familiar problem: I write [LC 56](https://leetcode.com/problems/merge-intervals/) and [LC 435](https://leetcode.com/problems/non-overlapping-intervals/) from an empty file, without a template, and state the complexity of each.
- [ ] **L4** Solve a familiar problem: I write [LC 57](https://leetcode.com/problems/insert-interval/) and [LC 986](https://leetcode.com/problems/interval-list-intersections/) from an empty file, and I name which pointer or phase each test controls.
- [ ] **L4** Solve a familiar problem: I write the skyline sweep ([LC 218](https://leetcode.com/problems/the-skyline-problem/)) with a `TreeMap` of heights from an empty file, and I name its three defects.
- [ ] **L5** Solve a new problem: for an interval problem that I have not seen, I choose between sort by start, sort by end, a sweep, a heap, a difference array, and a `TreeMap`, and I name the property of the problem that decides.
- [ ] **L6** Solve under time: I solve a Medium interval problem that the page does not solve in full, such as [LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/), in 25 minutes, speaking aloud, with the edge cases (empty input, one interval, touching intervals, nested intervals).
- [ ] **L6** Solve under time: I solve a Hard interval problem that the page does not solve in full, such as [LC 2402](https://leetcode.com/problems/meeting-rooms-iii/) or [LC 1851](https://leetcode.com/problems/minimum-interval-to-include-each-query/), in 40 minutes, speaking aloud, with tests and complexity.

## Next Topics

- [Pattern 14: Greedy](../14-greedy/README.md): the exchange argument and the "stays ahead" proof behind the sort by end, and the greedy view of the interval problems.
- [Pattern 16: Heap and Priority Queue](../16-heap-priority-queue/README.md): the min heap of end times for rooms, and lazy deletion, which gives a second solution of the skyline.
- [Pattern 04: Prefix Sum](../04-prefix-sum/README.md): the difference array in full, with range updates and range reads.
- [Pattern 21: Segment Tree and Fenwick Tree](../21-segment-tree-and-fenwick-tree/README.md): range updates and range maximum for the bookings that need better than O(n) for each request.
- [Pattern 15: Dynamic Programming](../15-dynamic-programming/README.md): the weighted version of the largest set without overlap.
- [Roadmap](../../roadmap.md): the order of all patterns and where this page sits.

## Further Reading

- [`java.util.TreeMap` (JDK 21 javadoc)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html): the guaranteed log(n) cost of `get`, `put`, and `remove`, and the meaning of `floorKey`, `ceilingKey`, `lowerKey`, and `higherKey`.
- [`java.util.Arrays` (JDK 21 javadoc)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html): the implementation note of `sort(T[], Comparator)`: stable, adaptive, far fewer than n lg(n) comparisons on partially sorted input, and approximately n comparisons on nearly sorted input.
- [`java.util.Map.merge` (JDK 21 javadoc)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html#merge(K,V,java.util.function.BiFunction)): the rule for an absent key and for a `null` result of the function.
- [`Integer.compare` (JDK 21 javadoc)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html#compare(int,int)): the comparison that cannot overflow.
- Cormen, Leiserson, Rivest, and Stein, *Introduction to Algorithms*, the chapter on greedy algorithms: the activity-selection problem and its proof.
- Kleinberg and Tardos, *Algorithm Design*, the chapter on greedy algorithms: interval scheduling, and the partition of intervals into the fewest groups, which is the argument behind P9.
