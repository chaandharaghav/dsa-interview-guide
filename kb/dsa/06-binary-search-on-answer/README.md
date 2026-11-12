---
priority: P1
format: lesson
---
# Pattern 06: Binary Search (Classic and On the Answer)

## Overview

Binary search finds a position, or a value, in a sequence. It compares one middle candidate with the goal and discards the half that cannot hold the answer. The technique needs one property: a yes/no question (a **predicate**) whose answers over the sequence read `false, false, ..., true, true`. A predicate with this shape is called **monotonic**: once it turns true, it stays true. A sorted array gives that property for questions such as "is `a[i] >= target`?". A range of possible answers can give it too, and then the problem has no array at all: the function that answers the yes/no question for one candidate answer is the **feasibility function**.

After this page you can:

- Write a binary search for an exact match, a lower bound, and an upper bound from the definition of the search space, and justify the start values, the loop condition, the two updates, and the returned value without a template.
- Show with a failing input why `lo = mid` with the lower middle never ends, and why `hi = mid - 1` loses the answer.
- Rewrite "find target", "first position >= target", "first position > target", and "first and last position" ([LC 34: Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/)) as one question: where does a predicate change from false to true?
- Recognize a problem that has no sorted array as a binary search on the answer, choose the bounds of the answer space with a reason for each bound, and write the feasibility function.
- Search a rotated sorted array ([LC 33: Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/), [LC 153: Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)), find a peak ([LC 162: Find Peak Element](https://leetcode.com/problems/find-peak-element/)), and search a 2D matrix ([LC 74: Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/)), and state the invariant that makes each search correct.
- Test whether a predicate is monotonic, and show the wrong answer that binary search gives when it is not.

## Prerequisites

| You need | Why this page needs it | Where to learn it |
|---|---|---|
| Arrays and zero-based indexing | Every search here reads `a[mid]` and moves two indices over an array | [Arrays, Strings, and Linked Lists](../foundations/02-arrays-strings-linked-lists.md) |
| Complexity analysis and logarithms | The cost of the search is the number of halvings, which is a logarithm | [Complexity Analysis](../foundations/01-complexity-analysis.md), [Math for Complexity](../foundations/00-math-for-complexity.md) |
| Sorting as the source of order | A sorted array makes "a[i] >= target" change from false to true only once | [Sorting Algorithms](../foundations/08-sorting-algorithms.md) |
| Integer overflow of `int` | `lo + hi` can pass 2,147,483,647 | Taught in the Derivation section of this page, Question 4 |
| A Java lambda such as `i -> i >= 5` and the interface `IntPredicate` | The generic search takes the yes/no question as an argument | Taught in the Derivation section of this page, Question 5 |

## Motivation

**The problem.** A sorted array holds 16 numbers:

```text
index   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
value   3   8  12  15  19  23  27  31  36  40  44  51  58  62  70  77
```

Find the index of the number 77. If the number is absent, report -1. The answer for 77 is 15.

**Attempt 1: scan from the left.** Compare 77 with each element, from index 0, until one matches. The program below counts the comparisons. It then counts something else: how many candidates remain after each comparison against the middle element, if one side is always dropped.

```java
public class CountSteps {
    public static void main(String[] args) {
        int[] a = {3, 8, 12, 15, 19, 23, 27, 31, 36, 40, 44, 51, 58, 62, 70, 77};
        int target = 77;

        // Linear scan: compare each element with the target, left to right.
        int linearComparisons = 0;
        for (int i = 0; i < a.length; i++) {
            linearComparisons++;
            if (a[i] == target) break;
        }
        System.out.println("linear scan for " + target + ": " + linearComparisons + " comparisons");

        // Candidates left after each middle comparison, if one side is always dropped.
        for (int n : new int[] {16, 1_000_000}) {
            int candidates = n;
            int steps = 0;
            System.out.print("n = " + n + ": " + candidates);
            while (candidates > 1) {
                candidates = (candidates + 1) / 2; // at most this many remain (rounded up)
                steps++;
                if (n == 16 || steps <= 4 || steps >= 18) System.out.print(" -> " + candidates);
                else if (steps == 5) System.out.print(" -> ...");
            }
            System.out.println("   (" + steps + " steps)");
        }
    }
}
```

<!-- expected-output -->
```text
linear scan for 77: 16 comparisons
n = 16: 16 -> 8 -> 4 -> 2 -> 1   (4 steps)
n = 1000000: 1000000 -> 500000 -> 250000 -> 125000 -> 62500 -> ... -> 4 -> 2 -> 1   (20 steps)
```

The linear scan makes 16 comparisons here, and up to 1,000,000 for an array of 1,000,000 elements. Every comparison tells the scan one fact: "this one element is not the target". The scan ignores a second fact that the sorted order gives for free.

**The question.** Compare 77 with the middle element, `a[7] = 31`. The array is sorted, so `a[0]`, `a[1]`, ..., `a[7]` are all at most 31. They are all smaller than 77. One comparison with the middle element therefore removes 8 of the 16 candidates: half. The next comparison, on the 8 that remain, removes 4 of them, and so on.

**Count the halvings, before any formula.** The program counted: 16 candidates need 4 halvings to reach 1 candidate (16, 8, 4, 2, 1). 1,000,000 candidates need 20 halvings (the program rounds up when a count is odd). The reason for 20: 2 multiplied by itself 19 times is 524,288, which is less than 1,000,000, and 2 multiplied by itself 20 times is 1,048,576, which is at least 1,000,000. The number of halvings that bring `n` down to 1 is the logarithm base 2 of `n`, rounded up. It is written log2(n). So log2(16) = 4 and log2(1,000,000) is about 19.93, which rounds up to 20.

**The waste that the technique removes.** The linear scan compares elements that the sorted order has already ruled out. The scan takes 1,000,000 comparisons where 20 comparisons are enough: a factor of 50,000. The gap that remains is practical: writing the loop with correct bounds. Many correct-looking loops never end, or return a wrong index. The next section derives the loop from a definition, so that each line has a reason.

## Derivation

### Question 1: what does the loop keep track of?

Name the thing that shrinks. The **search space** is the set of positions that can still hold the answer. Two indices, `lo` and `hi`, mark its ends. Every line of the loop must keep one statement true:

> **Invariant.** If `target` is in the array, its index is in `[lo, hi]` (both ends included).

Now derive each line from this statement.

| Line | Question | Answer from the invariant |
|---|---|---|
| Start values | Which positions can hold the target before any comparison? | All of them: `lo = 0`, `hi = n - 1` |
| Loop condition | When has the search space no position left? | When `lo > hi`. So the loop runs while `lo <= hi` |
| `mid` | Which position do we compare? | Any position inside the space. Take `lo + (hi - lo) / 2`. Integer division rounds down, so if the space has an even number of positions, `mid` is the left one of the two middle positions. This page calls it the **lower middle**. (Question 4 explains why the formula is not `(lo + hi) / 2`.) |
| `a[mid] == target` | What if the comparison matches? | The answer is `mid`: return it |
| `a[mid] < target` | What does the array tell us? | The array is sorted, so every position from `lo` to `mid` holds a value at most `a[mid]`, which is below `target`. None of them holds the target. New space: `lo = mid + 1` |
| `a[mid] > target` | Same question, mirrored | Every position from `mid` to `hi` holds a value above `target`. New space: `hi = mid - 1` |
| Returned value | What does an empty space mean? | By the invariant, if the target were present, it would be inside the space. The space is empty, so the target is absent: return -1 |

The program below is these lines, with a print statement in the loop. It searches for 77 (present), 50 (absent), 3 (at the first position), and 5 in an empty array.

```java
public class ClosedTrace {
    // Returns the index of target in the sorted array a, or -1 if target is absent.
    static int search(int[] a, int target) {
        int lo = 0, hi = a.length - 1;          // every index can still hold target
        while (lo <= hi) {                      // the search space is not empty
            int mid = lo + (hi - lo) / 2;       // an index inside the search space
            System.out.printf("  lo=%2d hi=%2d mid=%2d a[mid]=%2d", lo, hi, mid, a[mid]);
            if (a[mid] == target) {
                System.out.println("  equal -> return " + mid);
                return mid;
            }
            if (a[mid] < target) {
                System.out.println("  less    -> lo = mid + 1");
                lo = mid + 1;                   // a[0..mid] are all smaller than target
            } else {
                System.out.println("  greater -> hi = mid - 1");
                hi = mid - 1;                   // a[mid..n-1] are all larger than target
            }
        }
        System.out.println("  lo=" + lo + " hi=" + hi + "  the search space is empty -> return -1");
        return -1;
    }

    public static void main(String[] args) {
        int[] a = {3, 8, 12, 15, 19, 23, 27, 31, 36, 40, 44, 51, 58, 62, 70, 77};
        System.out.println("search 77:");
        search(a, 77);
        System.out.println("search 50:");
        search(a, 50);
        System.out.println("search 3 (first element):");
        search(a, 3);
        System.out.println("search 5 in an empty array:");
        search(new int[0], 5);
    }
}
```

<!-- expected-output -->
```text
search 77:
  lo= 0 hi=15 mid= 7 a[mid]=31  less    -> lo = mid + 1
  lo= 8 hi=15 mid=11 a[mid]=51  less    -> lo = mid + 1
  lo=12 hi=15 mid=13 a[mid]=62  less    -> lo = mid + 1
  lo=14 hi=15 mid=14 a[mid]=70  less    -> lo = mid + 1
  lo=15 hi=15 mid=15 a[mid]=77  equal -> return 15
search 50:
  lo= 0 hi=15 mid= 7 a[mid]=31  less    -> lo = mid + 1
  lo= 8 hi=15 mid=11 a[mid]=51  greater -> hi = mid - 1
  lo= 8 hi=10 mid= 9 a[mid]=40  less    -> lo = mid + 1
  lo=10 hi=10 mid=10 a[mid]=44  less    -> lo = mid + 1
  lo=11 hi=10  the search space is empty -> return -1
search 3 (first element):
  lo= 0 hi=15 mid= 7 a[mid]=31  greater -> hi = mid - 1
  lo= 0 hi= 6 mid= 3 a[mid]=15  greater -> hi = mid - 1
  lo= 0 hi= 2 mid= 1 a[mid]= 8  greater -> hi = mid - 1
  lo= 0 hi= 0 mid= 0 a[mid]= 3  equal -> return 0
search 5 in an empty array:
  lo=0 hi=-1  the search space is empty -> return -1
```

**Why the loop is correct.** At the start, the space is the whole array, so the invariant holds. Each round has three outcomes. If `a[mid] == target`, the loop returns the right index. If `a[mid] < target`, the sorted order shows that the target is not in `a[lo..mid]`. The invariant said that the target, if present, is in `[lo, hi]`. So it is in `[mid + 1, hi]`, and the update `lo = mid + 1` keeps the invariant. The case `a[mid] > target` is the mirror image. Both updates remove `mid` from the space, so the space loses at least one position per round and the loop must end. It ends when the space is empty, and the invariant then says that the target is absent.

**The cost, counted.** Let `s` be the size of the space, `s = hi - lo + 1`. The positions left of `mid` number `mid - lo`, which is `(s - 1) / 2` rounded down. The positions right of `mid` number `hi - mid`, which is `(s - 1) / 2` rounded up. Neither side is larger than `s / 2` rounded down. So the space at least halves each round. From 16 positions the sizes are at most 16, 8, 4, 2, 1, 0: five rounds, and the trace for 77 shows five rounds. For 1,000,000 positions the worst case is 20 rounds (the largest count over every target position, checked by a script). In general, the worst case is the number of times `n` can be halved (rounding down) before it reaches 0. That number is floor(log2(n)) + 1, which is 5 for 16 and 20 for 1,000,000. In O-notation this is O(log n). The linear scan costs O(n).

### Question 2: what if the array has duplicates, and we want the first match?

The exact-match loop returns as soon as `a[mid] == target`. On `[1, 2, 2, 2, 4, 4, 7, 9]` and target 2, it returns whichever 2 it meets first. A problem such as "find the first position of 2" needs a different loop. It must keep a match as a candidate instead of returning it.

Change the definition of the search space. A position can hold the answer "the first index `i` with `a[i] >= target`". That answer can also be `n`, the position after the last element, when every element is smaller than the target. So the space has `n + 1` possible answers: `0` to `n`. The new invariant:

> **Invariant (first position `>= target`).** Every index below `lo` holds a value smaller than `target`. Every index at or above `hi` holds a value of at least `target`, where index `n` counts as holding "infinity". So the answer is in `[lo, hi]`.

Derive each line again.

| Line | Question | Answer from the invariant |
|---|---|---|
| Start values | Which answers are possible? | `0` to `n`, so `lo = 0` and `hi = n` (not `n - 1`) |
| Loop condition | When is the answer known? | When one candidate is left: `lo == hi`. The loop runs while `lo < hi` |
| `mid` | Which position do we read? | `lo + (hi - lo) / 2`. Because `lo < hi`, this value is at most `hi - 1`, which is at most `n - 1`. So `a[mid]` exists |
| `a[mid] >= target` | What does it say about the answer? | Position `mid` holds a valid answer, and no answer to the right of it can be first. The answer is in `[lo, mid]`: `hi = mid`. Keep `mid`, because it may be the answer |
| `a[mid] < target` | What does it say? | Position `mid` and everything left of it hold smaller values. The answer is in `[mid + 1, hi]`: `lo = mid + 1` |
| Returned value | What is the answer when `lo == hi`? | The one candidate left: `lo` |

```java
// First index i with a[i] >= target, or a.length if there is none.
static int lowerBound(int[] a, int target) {
    int lo = 0, hi = a.length;           // the answer can be a.length, so hi is not a.length - 1
    while (lo < hi) {                    // more than one candidate answer remains
        int mid = lo + (hi - lo) / 2;    // lo <= mid < hi, so a[mid] exists
        if (a[mid] < target) {
            lo = mid + 1;                // a[0..mid] are all smaller than target
        } else {
            hi = mid;                    // a[mid] >= target: mid may be the answer, keep it
        }
    }
    return lo;                           // lo == hi
}
```

**Trace on an array with duplicates.** Array `[1, 2, 2, 2, 4, 4, 7, 9]`, target 2. The first index with `a[i] >= 2` is 1.

```text
lowerBound([1, 2, 2, 2, 4, 4, 7, 9], 2)
  lo=0 hi=8 mid=4 a[mid]=4  a[mid] >= 2 is true -> hi = mid
  lo=0 hi=4 mid=2 a[mid]=2  a[mid] >= 2 is true -> hi = mid
  lo=0 hi=2 mid=1 a[mid]=2  a[mid] >= 2 is true -> hi = mid
  lo=0 hi=1 mid=0 a[mid]=1  a[mid] >= 2 is false -> lo = mid + 1
  first index with a[i] >= 2: 1
```

The loop never returns early. At `mid = 2` and `mid = 1` the value is 2, which is `>= 2`, so `hi` moves left and keeps the match as a candidate. The loop stops at index 1, the first 2.

**The two forms are consistent with their own definitions.** The first loop is the **closed** form: the search space `[lo, hi]` contains only positions that can be tested, and both ends are included. The second loop is the **half-open** form: the positions that are still to be tested are `lo` to `hi - 1`, and `hi` is excluded from testing but stays a possible answer (it can be the position `n` after the last element). Both forms keep the invariant after every update, and both updates shrink the space:

| Form | Space | Loop | Updates | Why each update shrinks the space |
|---|---|---|---|---|
| Closed: find a value | Indices that can hold the target: `[lo, hi]` | `while (lo <= hi)` | `lo = mid + 1`, `hi = mid - 1` | Both remove `mid` |
| Half-open: find a boundary | Possible answers `[lo, hi]`, where `hi` may be `n` | `while (lo < hi)` | `lo = mid + 1`, `hi = mid` | `mid < hi`, so `hi = mid` lowers `hi`. `lo = mid + 1` raises `lo` |

**Test each form on arrays of length 0, 1, and 2.** A loop bound is correct only if the smallest inputs work. Step each form by hand on these inputs:

| Input | Closed loop `search` | Half-open loop `lowerBound` |
|---|---|---|
| `[]`, target 5 | `lo=0, hi=-1`: `lo <= hi` is false, return -1 | `lo=0, hi=0`: `lo < hi` is false, return 0 |
| `[4]`, target 4 | `mid=0`, equal, return 0 | `lo=0, hi=1, mid=0`: `4 < 4` is false, `hi = 0`, return 0 |
| `[4]`, target 5 | `mid=0`, less, `lo=1`, now `lo > hi`, return -1 | `mid=0`: `4 < 5` is true, `lo = 1`, return 1 |
| `[4, 9]`, target 9 | `mid=0`, less, `lo=1`. `mid=1`, equal, return 1 | `lo=0, hi=2, mid=1`: `9 < 9` is false, `hi = 1`. `mid=0`: `4 < 9`, `lo = 1`, return 1 |
| `[4, 9]`, target 1 | `mid=0`, greater, `hi=-1`, return -1 | `mid=1`: false, `hi = 1`. `mid=0`: `4 < 1` is false, `hi = 0`, return 0 |
| `[4, 9]`, target 10 | `mid=0`, less, `lo=1`. `mid=1`, less, `lo=2`, return -1 | `mid=1`: `9 < 10`, `lo = 2`, return 2 |

Each row ends in one or two rounds, and `mid` is a valid index in every round.

**A closed-range variant for a boundary.** The closed loop can also find a boundary if it records a candidate before it shrinks the space. For the first index with `a[i] >= target`: `result = n` at the start, and when `a[mid] >= target`, set `result = mid` and `hi = mid - 1`. The loop ends when `lo > hi`, and `result` holds the answer. Both forms are correct. This page uses the half-open form for boundaries, because it needs no extra variable.

### Question 3: what goes wrong if one line does not follow the definition?

**Bug 1: the loop that never ends.** Suppose a reader wants "the last index with `a[i] <= target`". The reader keeps the half-open loop `while (lo < hi)`, and writes the update that keeps `mid`: `lo = mid`. For `a = [1, 2]` and `target = 5`, the first round has `lo = 0`, `hi = 1`, and `mid = lo + (hi - lo) / 2 = 0`. Then `a[0] <= 5` holds, so `lo = mid = 0`. Nothing changed. The same state repeats forever.

**Bug 2: the lost element.** Suppose a reader wants "the first index with `a[i] >= target`" and copies the closed-form updates: `hi = mid - 1`. For `a = [1, 5, 9]` and `target = 5`, the first round has `mid = 1` and `a[1] = 5 >= 5`, so `hi = mid - 1 = 0`. The index 1 left the space, but index 1 was the answer. The loop returns index 0.

The program runs both bugs. Bug 1 has a guard that stops it after 5 rounds.

```java
public class BugDemo {
    public static void main(String[] args) {
        // Bug 1: find the last index with a[i] <= target, but use the lower middle and lo = mid.
        int[] a = {1, 2};
        int target = 5;
        int lo = 0, hi = a.length - 1;
        int rounds = 0;
        while (lo < hi && rounds < 5) {
            int mid = lo + (hi - lo) / 2;      // lower middle: for lo = 0, hi = 1 this is 0
            if (a[mid] <= target) lo = mid;    // lo stays 0
            else hi = mid - 1;
            rounds++;
            System.out.println("round " + rounds + ": lo = " + lo + ", hi = " + hi
                    + ", mid = " + mid);
        }
        System.out.println("stopped by the guard after " + rounds + " rounds, lo < hi is still "
                + (lo < hi));

        // Bug 2: first index with a[i] >= target, but hi = mid - 1 with the loop lo < hi.
        int[] b = {1, 5, 9};
        int want = 5;
        lo = 0;
        hi = b.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            System.out.println("lost: lo = " + lo + ", hi = " + hi + ", mid = " + mid
                    + ", b[mid] = " + b[mid]);
            if (b[mid] < want) lo = mid + 1;
            else hi = mid - 1;                 // drops mid, but b[mid] = 5 is the answer
        }
        System.out.println("returned index " + lo + " (value " + b[lo] + "), correct index is 1");
    }
}
```

<!-- expected-output -->
```text
round 1: lo = 0, hi = 1, mid = 0
round 2: lo = 0, hi = 1, mid = 0
round 3: lo = 0, hi = 1, mid = 0
round 4: lo = 0, hi = 1, mid = 0
round 5: lo = 0, hi = 1, mid = 0
stopped by the guard after 5 rounds, lo < hi is still true
lost: lo = 0, hi = 2, mid = 1, b[mid] = 5
returned index 0 (value 1), correct index is 1
```

Each bug breaks one of two rules. Check both rules for every update:

1. **Safety.** After the update, does the new space still contain every position that can be the answer? Bug 2 fails: `hi = mid - 1` removes `mid`, but `mid` can be the answer.
2. **Progress.** After the update, is the new space strictly smaller? Bug 1 fails: `lo = mid` removes nothing when `mid == lo`.

Three combinations satisfy both rules:

| Loop | Updates | Middle | Use |
|---|---|---|---|
| `while (lo <= hi)` | `lo = mid + 1`, `hi = mid - 1` | `lo + (hi - lo) / 2` | Exact match |
| `while (lo < hi)` | `lo = mid + 1`, `hi = mid` | `lo + (hi - lo) / 2` (lower middle) | First true |
| `while (lo < hi)` | `lo = mid`, `hi = mid - 1` | `hi - (hi - lo) / 2` (upper middle) | Last true |

In the third row, `lo = mid` makes progress only if `mid > lo`. With `hi = lo + 1`, the lower middle equals `lo` and the upper middle equals `hi`. So the loop uses the upper middle. The last-true loop is derived in Question 5.

### Question 4: why `lo + (hi - lo) / 2` and not `(lo + hi) / 2`?

In Java, an `int` holds values up to 2,147,483,647. If `lo` and `hi` are both large, `lo + hi` passes that limit and wraps around to a negative number. Then `mid` is negative and `a[mid]` throws an exception, or the answer space leaves its range.

```java
public class Overflow {
    public static void main(String[] args) {
        int lo = 1_500_000_000;
        int hi = 2_000_000_000;
        System.out.println("lo + hi            = " + (lo + hi));         // does not fit in an int
        System.out.println("(lo + hi) / 2      = " + (lo + hi) / 2);
        System.out.println("lo + (hi - lo) / 2 = " + (lo + (hi - lo) / 2)); // hi - lo fits
        System.out.println("(lo + hi) >>> 1    = " + ((lo + hi) >>> 1));      // unsigned shift
    }
}
```

<!-- expected-output -->
```text
lo + hi            = -794967296
(lo + hi) / 2      = -397483648
lo + (hi - lo) / 2 = 1750000000
(lo + hi) >>> 1    = 1750000000
```

`hi - lo` never passes the limit if `0 <= lo <= hi`, so `lo + (hi - lo) / 2` is safe. The expression `(lo + hi) >>> 1` is also safe when `lo` and `hi` are not negative, because `>>>` reads the 32 bits of the sum as an unsigned number. The JDK uses `>>> 1` in `Arrays.binarySearch`. Arrays have fewer than 2^31 elements, so the overflow is rare for array indices. It is common in a search on the answer, where `hi` can be 10^9 or 2 * 10^9.

### Question 5: one idea behind every form (the predicate view)

Look again at the half-open loop. The loop never uses the word "target" except in one comparison: `a[mid] >= target`. Replace that comparison by any yes/no question `p(i)` about the index. The loop works whenever the answers to `p` read `false, false, ..., false, true, true, ..., true`. The loop then finds the first `true`.

```text
index    0     1     2     3     4     5     6     7     8
p(i)     F     F     F     T     T     T     T     T     T(sentinel)
                           ^ the first true: what the loop returns
```

A **sentinel** is a position that you add on purpose, at the end, so that the answer always exists. Here the sentinel is index `n` (or the top of the answer range). It lets the loop return "no element qualifies" as an ordinary answer.

Many problems are the same loop with another predicate.

| Problem | Predicate `p(i)` (true from the answer onward) | Search finds | Answer |
|---|---|---|---|
| Find target (closed form, or lower bound plus a check) | `a[i] >= target` | first true | `i` if `i < n` and `a[i] == target`, else absent |
| Lower bound ([LC 35: Search Insert Position](https://leetcode.com/problems/search-insert-position/)) | `a[i] >= target` | first true | `i` |
| Upper bound | `a[i] > target` | first true | `i` |
| First and last position ([LC 34: Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/)) | first: `a[i] >= target`. past the last: `a[i] > target` | two first-true searches | `[lowerBound, upperBound - 1]` if `lowerBound < upperBound`, else `[-1, -1]` |
| First Bad Version ([LC 278: First Bad Version](https://leetcode.com/problems/first-bad-version/)) | `isBadVersion(i)` | first true | `i` |
| Square root rounded down ([LC 69: Sqrt(x)](https://leetcode.com/problems/sqrtx/)) | `i * i > x` (use `long`), searched over `[0, x + 1]` | first true | `i - 1` |

Here is a generic loop for the first true. It receives the predicate as a function. In Java, a **lambda** such as `i -> i >= 5` is a small function that takes `i` and returns a value, here a `boolean`. The interface `java.util.function.IntPredicate` is the type of such a function, and `p.test(mid)` calls it. Add `import java.util.function.IntPredicate;` when you copy the code.

```java
// First index in [lo, hi] where p is true. Assumes p(hi) is true (hi is the sentinel)
// and that p reads false, ..., false, true, ..., true.
static int firstTrue(int lo, int hi, IntPredicate p) {
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (p.test(mid)) {
            hi = mid;                    // mid may be the first true: keep it
        } else {
            lo = mid + 1;                // mid and everything left of it are false
        }
    }
    return lo;
}
```

Lower bound is `firstTrue(0, n, i -> i == n || a[i] >= target)`. The predicate treats the sentinel `n` as true because `a[n]` does not exist.

**The last true.** Some problems ask for the largest value that satisfies a condition. Then the predicate reads `true, ..., true, false, ..., false` and the answer is the last `true`. Derive the loop from the third row of the table above. The space is `[lo, hi]`, and `p(lo)` is true: `lo` is the sentinel on the left. If `p(mid)` is true, the answer is at `mid` or to its right: `lo = mid`. If `p(mid)` is false, `mid` and everything right of it are false: `hi = mid - 1`. The update `lo = mid` needs `mid > lo`, so `mid` must be the upper middle. A second way to get the last true: run `firstTrue` on the opposite predicate ("is `p` false?"), which finds the first false, and subtract 1. The first way needs a position at the left end where `p` is true. The second way needs a position at the right end where `p` is false.

```java
// Last index in [lo, hi] where p is true. Assumes p(lo) is true (lo is the sentinel)
// and that p reads true, ..., true, false, ..., false.
static int lastTrue(int lo, int hi, IntPredicate p) {
    while (lo < hi) {
        int mid = hi - (hi - lo) / 2;    // upper middle: lo < mid <= hi when lo < hi
        if (p.test(mid)) {
            lo = mid;                    // mid may be the last true: keep it
        } else {
            hi = mid - 1;                // mid and everything right of it are false
        }
    }
    return lo;
}
```

The expression `hi - (hi - lo) / 2` is the upper middle. For `hi - lo = 1` it gives `hi`. For `hi - lo = 2` it gives `lo + 1`. It cannot overflow, even if `hi - lo` is 2,147,483,647, which is not true for `lo + (hi - lo + 1) / 2`.

**The cost.** The search space has `R` positions and the loop halves it each round. So the loop makes about log2(R) calls to `p`. If one call costs `C`, the whole search costs `C * log2(R)`. For `p(i) = a[i] >= target`, `C` is one comparison, so the cost is O(log n). For a predicate that scans an input of size `n`, `C` is O(n). That observation leads to the next idea.

## The Pattern

**The idea.** Find a position or a value where a yes/no question changes from false to true (or from true to false) along a line of candidates. Compare at the middle, discard the half that cannot hold the change point, and repeat. The line of candidates can be the indices of a sorted array, or the range of possible answers to an optimization problem.

**The invariant.** The change point is always inside `[lo, hi]`. Every update keeps the change point inside, and every update shrinks the range.

**The vocabulary.** Each term below names something that you already did.

| Term | What it was in this page | The question to ask in a new problem |
|---|---|---|
| Search space | `[lo, hi]`: positions that can still hold the answer | What are all the possible answers, from smallest to largest? |
| Predicate | `a[i] >= target`, `hoursNeeded(speed) <= h` | What yes/no question has a different answer on the two sides of the answer? |
| Monotonic predicate | A predicate that reads `F...F T...T` or `T...T F...F` | If the answer `x` passes, does every larger answer pass (or every smaller one)? Why? |
| Sentinel | Index `n`, or the top of the answer range | Is there always at least one `true`? If not, add a position that makes it so |
| Feasibility check | The function that evaluates the predicate for a candidate answer | Can I decide the predicate for one candidate in O(n) or O(n log n)? |
| First true / last true | The two loop shapes | Do I minimize (first true) or maximize (last true)? |

**The variants, and the property that selects each one.**

| Variant | Selected by this property of the problem | Loop shape |
|---|---|---|
| Exact match in a sorted array | The array is sorted, and the answer is one index with `a[i] == target` | Closed range, `lo <= hi`, `return mid` |
| First true (boundary) | The answer is the first index where a monotone predicate is true (lower bound, upper bound, minimize) | Half-open, `lo < hi`, `hi = mid` |
| Last true | The answer is the last index where a monotone predicate is true (maximize) | Half-open, `lo < hi`, `lo = mid`, upper middle |
| Search on the answer | No array to search. The answer is a number in a known range, and a check decides if a candidate works | First true or last true over the range of answers |
| Search on structure | The array is not sorted, but one comparison at `mid` still shows which half keeps an answer (rotated array, peak) | Half-open or closed, with a custom invariant |
| Real numbers | The answer is a real number | A fixed number of iterations on `double` values |

### Variations A to E

The variations of search on the answer keep the same structure: a range, a feasibility check, and a monotone predicate. The table gives a summary. Worked Examples 1 to 4 derive A, B, C, and the Split Array problem in full, and Worked Example 8 covers D. Read the table again after them. Two words that the table uses: `ceil(x)` is `x` rounded up to a whole number (`ceil(7 / 2)` is 4), and a **greedy** check is a rule that makes the locally best choice at each step and never undoes it (Worked Example 2 shows one).

| Variation | Problem | Insight (why the predicate is monotone) | Feasibility check |
|---|---|---|---|
| A. Koko Eating Bananas | Smallest eating speed that finishes all bananas in `h` hours ([LC 875: Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/)) | If speed `K` works, speed `K + 1` works: faster eating never needs more hours | Sum of `ceil(pile / K)` over all piles is at most `h`. In Java, `ceil(pile / K)` is `(pile + K - 1) / K`, and the sum is a `long`: for example, 10^4 piles of 10^9 bananas at `K = 1` sum to 10^13 hours |
| B. Capacity to Ship Packages | Smallest ship capacity that ships all packages in `D` days ([LC 1011: Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/)) | Same as Split Array Largest Sum: minimize the largest daily load. A larger capacity never needs more days | Greedy packing, count the days |
| C. Aggressive Cows / Magnetic Force | Maximize the minimum distance between placed items ([LC 1552: Magnetic Force Between Two Balls](https://leetcode.com/problems/magnetic-force-between-two-balls/)) | If distance `D` is possible, distance `D - 1` is possible (the same placement has gaps at least `D`, so at least `D - 1`) | Greedy placement with minimum gap `D`, count the items placed |
| D. Minimize Max Distance to Gas Station | Add `K` gas stations to minimize the largest gap ([LC 774: Minimize Max Distance to Gas Station](https://leetcode.com/problems/minimize-max-distance-to-gas-station/) (premium)) | If largest gap `G` is possible, every larger gap is possible | For each gap `g`, the stations needed for largest gap `d` is `ceil(g / d) - 1`. The answer is a real number, so use the floating-point form |
| E. Kth Smallest | Kth smallest element in a sorted matrix ([LC 378: Kth Smallest Element in a Sorted Matrix](https://leetcode.com/problems/kth-smallest-element-in-a-sorted-matrix/)) | "At least `K` elements are `<= v`" is false below the answer and true from the answer upward | Count elements `<= v`: binary search in each row, or a walk along the staircase |

## Worked Examples

Each example changes one thing from the page so far. The first example removes the sorted array. The last examples keep an array but change what a comparison at `mid` means.

### Worked Example 1: Koko Eating Bananas ([LC 875: Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/), Medium)

**Statement.** Koko has piles of bananas, `piles[i]` bananas in pile `i`. A guard returns in `h` hours. Koko picks an integer speed `s` (bananas per hour). Each hour she chooses one pile and eats `s` bananas from it, or the whole pile if it holds fewer. She does not move to another pile in the same hour. Find the smallest `s` that lets her finish all piles within `h` hours.

Example: `piles = [3, 6, 7, 11]`, `h = 8`. The answer is 4.

**What changed.** There is no sorted array. The things to search over are the speeds `1, 2, 3, ...`. There is no `a[i]` to read. A function computes the information that `a[mid]` gave before.

**Brute force.** Try each speed from 1 upward and stop at the first speed that finishes in time. At speed `s`, pile `p` takes `ceil(p / s)` hours, because each hour eats at most `s` from one pile.

| speed | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hours | 27 | 15 | 10 | 8 | 8 | 6 | 5 | 5 | 5 | 5 | 4 |
| hours <= 8 | F | F | F | T | T | T | T | T | T | T | T |

The last row reads `F F F T T T T T T T T`. The brute force tests up to the answer, which is about `max(piles)` speeds in the worst case. Each test costs O(n). With `max(piles)` up to 10^9, this is too slow.

**Why the row is `F...F T...T`.** The predicate "can finish at speed `s`" never changes from true back to false as `s` grows. The reason: for one pile, `ceil(p / s)` does not increase when `s` increases, because `p / s` gets smaller. The total hours is a sum of such terms, so it does not increase either. If the total is at most `h` at speed `s`, it is at most `h` at speed `s + 1`. So once the predicate becomes true, it stays true.

**The bounds of the answer space, with a reason for each.**

- `lo = 1`. A speed of 0 eats nothing, so it is not a valid speed. (It also makes `ceil(p / 0)` divide by zero.)
- `hi = max(piles)`. At that speed every pile takes exactly 1 hour, so the total is `piles.length` hours. The problem guarantees that `h` is at least the number of piles. So this speed always finishes in time: `piles.length <= h`, and `hi` is a true sentinel. A speed above `max(piles)` does not reduce the hours further: each pile still takes 1 hour.

**The feasibility function.**

```java
// Hours Koko needs at the given speed. A pile of p bananas takes ceil(p / speed) hours.
static long hoursNeeded(int[] piles, int speed) {
    long hours = 0;                               // long: the sum can pass 2^31 - 1
    for (int pile : piles) {
        hours += (pile + speed - 1) / speed;      // integer ceiling of pile / speed
    }
    return hours;
}
```

**The search.** The space is `[1, max(piles)]`. The predicate is `hoursNeeded(piles, s) <= h`. We want the first true.

```java
static int minEatingSpeed(int[] piles, int h) {
    int lo = 1;
    int hi = 0;
    for (int pile : piles) hi = Math.max(hi, pile);   // at this speed each pile takes 1 hour
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        long hours = 0;
        for (int pile : piles) hours += (pile + mid - 1) / mid;
        if (hours <= h) {
            hi = mid;                                 // mid works, and a smaller speed may work
        } else {
            lo = mid + 1;                             // mid is too slow
        }
    }
    return lo;
}
```

**The trace** (printed by the program in the Implementation section):

```text
Koko piles [3, 6, 7, 11], h = 8
  lo= 1 hi=11 mid= 6 hours= 6  hours <= 8 is true  -> hi = mid
  lo= 1 hi= 6 mid= 3 hours=10  hours <= 8 is false -> lo = mid + 1
  lo= 4 hi= 6 mid= 5 hours= 8  hours <= 8 is true  -> hi = mid
  lo= 4 hi= 5 mid= 4 hours= 8  hours <= 8 is true  -> hi = mid
  minimum speed: 4
```

Round 1: speed 6 takes 6 hours, which is at most 8, so the answer is 6 or lower: `hi = 6`. Round 2: speed 3 takes 10 hours, so the answer is above 3: `lo = 4`. Round 3: speed 5 works. Round 4: speed 4 works, `hi = 4`, and `lo == hi`. The answer is 4. Only 4 feasibility checks ran instead of up to 11.

**Cost.** The space has `max(piles)` values, so the loop runs about log2(max(piles)) rounds. Each round runs one feasibility check, which reads all `n` piles: O(n). The total is O(n log max(piles)). In general, the cost of a search on the answer is **log(range) times the cost of one feasibility check**. The space is O(1) extra.

### Worked Example 2: Capacity To Ship Packages Within D Days ([LC 1011: Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/), Medium)

**Statement.** Packages with weights `weights[0..n-1]` must ship in the given order on one ship. Each day, the ship carries a prefix of the remaining packages, with total weight at most the capacity `c`. Find the smallest `c` that ships everything within `days` days.

Example: `weights = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]`, `days = 5`. The answer is 15.

**What changed.** The predicate is no longer a sum of independent terms. It is a greedy simulation (a rule that makes the locally best choice at each step and never undoes it): load packages in order, and start a new day when the next package does not fit. The structure of the search stays the same.

**Reasoning.**

- Predicate: "capacity `c` ships everything in at most `days` days". A larger capacity cannot need more days: any plan that works with capacity `c` also works with capacity `c + 1`. So the row reads `F...F T...T`.
- `lo = max(weights)`. A smaller capacity cannot carry the heaviest package, so the predicate is false below it.
- `hi = sum(weights)`. With that capacity, one day ships everything, so the predicate is true: `hi` is a true sentinel.
- The check: the greedy packing. The greedy packing counts the fewest days for the same reason as in Worked Example 4, which proves it for the same check.

```java
static int shipWithinDays(int[] weights, int days) {
    int lo = 0;
    long sum = 0;
    for (int w : weights) {
        lo = Math.max(lo, w);                // the ship must carry the heaviest package
        sum += w;
    }
    int hi = (int) sum;                      // one day carries everything
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        int daysNeeded = 1, load = 0;
        for (int w : weights) {
            if (load + w > mid) {            // the package does not fit today: start a new day
                daysNeeded++;
                load = 0;
            }
            load += w;
        }
        if (daysNeeded <= days) hi = mid; else lo = mid + 1;
    }
    return lo;
}
```

**Trace** (`weights = 1..10`, `days = 5`; `lo = 10`, `hi = 55`):

```text
lo=10 hi=55 mid=32 daysNeeded=2  <= 5 true   -> hi = 32
lo=10 hi=32 mid=21 daysNeeded=3  <= 5 true   -> hi = 21
lo=10 hi=21 mid=15 daysNeeded=5  <= 5 true   -> hi = 15
lo=10 hi=15 mid=12 daysNeeded=6  <= 5 false  -> lo = 13
lo=13 hi=15 mid=14 daysNeeded=6  <= 5 false  -> lo = 15
lo=15 hi=15                                   -> answer 15
```

At capacity 15 the days are `[1,2,3,4,5]`, `[6,7]`, `[8]`, `[9]`, `[10]`: five days. At capacity 14 they are `[1,2,3,4]`, `[5,6]`, `[7]`, `[8]`, `[9]`, `[10]`: six days.

**Cost.** O(n log(sum - max)), which is O(n log sum). The space is O(1).

### Worked Example 3: Maximize the Minimum, Magnetic Force Between Two Balls ([LC 1552: Magnetic Force Between Two Balls](https://leetcode.com/problems/magnetic-force-between-two-balls/), Medium)

**Statement.** Baskets sit at distinct positions on a line, `position[i]`. Place `m` balls in `m` different baskets. The force between two balls is their distance. Maximize the smallest distance between any two balls.

Example: `position = [1, 2, 3, 4, 7, 9]`, `m = 3`. The answer is 3 (balls at 1, 4, 7).

**What changed.** The goal is a maximum, not a minimum. The predicate flips: "distance `d` is possible" reads `true, ..., true, false, ..., false`, and the search finds the last true.

**Reasoning.** The formula for `hi` divides by `m - 1`, so it needs `m >= 2`. With `m = 1` there is no pair of balls and the question has no answer.

- Predicate `p(d)`: "we can place `m` balls with every two balls at least `d` apart". If `d` is possible, then `d - 1` is possible: the same placement has every gap at least `d`, so at least `d - 1`. So `p` reads `T...T F...F`.
- `lo = 1`. The positions are distinct integers, so any two balls are at least 1 apart, and `m <= n` balls can be placed. So `p(1)` is true: `lo` is a true sentinel.
- `hi = (max - min) / (m - 1)`, rounded down. `m` balls have `m - 1` gaps between neighbors. All gaps are at least `d`, and the gaps add up to at most `max - min`. So `(m - 1) * d <= max - min`.
- Check `p(d)`: sort the positions. Put the first ball at the first position, and then repeatedly put the next ball at the first position that is at least `d` away from the last ball. Count the balls. The check is true if the count is at least `m`. Why this works: take any valid placement, with balls at `q1 < q2 < ... < qm`. The greedy ball `g1` is `p[0]`, which is at or left of `q1`. If greedy ball `gj` is at or left of `qj`, then the position of `q(j+1)` is at least `d` right of `qj`, so at least `d` right of `gj`. The greedy rule takes the first such position, so `g(j+1)` is at or left of `q(j+1)`. So greedy places at least `m` balls whenever any valid placement does.

The search finds the last true, so the update is `lo = mid` and `mid` is the upper middle. The two-element case shows why. Take `lo = 3` and `hi = 4`. The lower middle is `3`. If `p(3)` is true, the update `lo = mid` gives `lo = 3` again: no progress, the loop never ends. The upper middle is `4`, so `lo` or `hi` always changes.

```java
static int maxMinDistance(int[] position, int m) {
    int[] p = position.clone();
    Arrays.sort(p);                                   // the greedy check walks left to right
    int lo = 1;                                       // p(1) is true
    int hi = (p[p.length - 1] - p[0]) / (m - 1);      // m balls need (m - 1) gaps of size d
    while (lo < hi) {
        int mid = hi - (hi - lo) / 2;                 // upper middle: lo < mid <= hi
        int placed = 1, last = p[0];
        for (int i = 1; i < p.length; i++) {
            if (p[i] - last >= mid) {                 // far enough from the last ball
                placed++;
                last = p[i];
            }
        }
        if (placed >= m) lo = mid; else hi = mid - 1;
    }
    return lo;
}
```

**Trace** (printed by the program in the Implementation section):

```text
Balls at [1, 2, 3, 4, 7, 9], m = 3
  lo=1 hi=4 mid=3 placed=3  placed >= 3 is true  -> lo = mid
  lo=3 hi=4 mid=4 placed=2  placed >= 3 is false -> hi = mid - 1
  largest minimum distance: 3
```

Round 1: distance 3 places 3 balls (positions 1, 4, 7), so `lo = 3`. Round 2: `lo = 3`, `hi = 4`, `mid = 4` (the upper middle). Distance 4 places only 2 balls (1 and 7), so `hi = 3`. Now `lo == hi`, the answer is 3.

**Cost.** Sorting costs O(n log n). The search runs about log2(max - min) rounds, each O(n). The total is O(n log n + n log(max - min)).

### Worked Example 4: Split Array Largest Sum ([LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/), Hard)

**Statement.** Split an array of non-negative integers into `m` non-empty contiguous subarrays so that the largest subarray sum is as small as possible. Return that sum. (LeetCode names the parameter `k`.)

Example: `nums = [7, 2, 5, 10, 8]`, `m = 2`. The answer is 18: split `[7, 2, 5]` and `[10, 8]`, with sums 14 and 18.

**What changed.** The old page used this problem as its main example. It is the same shape as Capacity to Ship: "minimize the largest group" is a first-true search. This example adds the explanation of why the greedy check is correct.

**Brute force.** Place `m - 1` dividers in all possible ways: `C(n - 1, m - 1)` placements, and each costs O(n) to sum. The count grows as about `n^(m-1)` divided by `(m-1)!`, so it explodes when `m` grows. For `n = 1000` and `m = 10`, `C(999, 9)` is about 2.6 * 10^21.

**The reframe.** Do not ask "where do the dividers go?". Ask: "If every subarray sum may be at most `x`, can the array be split into at most `m` parts?" That is a yes/no question about `x`.

**Monotone.** If `x = 10` allows at most `m` parts, then `x = 11` also allows at most `m` parts: the same split has all sums at most 10, so at most 11. The row reads `F...F T...T`.

**Bounds.** `lo = max(nums)`: a smaller `x` cannot hold the largest element in any part. `hi = sum(nums)`: one part holds everything, so the predicate is true there.

**The check, and why it is correct.**

```java
static int splitArray(int[] nums, int m) {
    int lo = 0;
    long sum = 0;
    for (int x : nums) {
        lo = Math.max(lo, x);                 // no part can be below the largest element
        sum += x;
    }
    int hi = (int) sum;                       // one part holds everything
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;         // the candidate limit for each part
        int groups = 1, current = 0;          // the first group exists before any element
        for (int x : nums) {
            if (current + x > mid) {          // x does not fit: close the group, open a new one
                groups++;
                current = 0;
            }
            current += x;
        }
        if (groups <= m) hi = mid; else lo = mid + 1;
    }
    return lo;
}
```

The check closes a group only when the next element does not fit. Claim: this greedy rule gives the fewest groups for the limit `x`, so `groups <= m` decides the predicate correctly. The reason, in steps (the numbers must not be negative, as the statement guarantees):

1. Take any valid split into `g` parts with all sums at most `x`. Call its group ends `v1 < v2 < ... < vg = n`. Call the greedy group ends `e1, e2, ...`. The claim is `ej >= vj` for every `j`: after `j` groups, greedy has covered at least as many elements as the valid split.
2. For `j = 1`: greedy takes the longest prefix with sum at most `x`. The first valid group is a prefix with sum at most `x`, so it is not longer. So `e1 >= v1`.
3. From `j` to `j + 1`: if `ej >= v(j+1)`, the claim holds. Otherwise, greedy's group `j + 1` starts at `ej + 1`, which is inside valid group `j + 1` (that group runs from `vj + 1` to `v(j+1)`). The elements from `ej + 1` to `v(j+1)` are a part of that valid group. The numbers are not negative, so a part of a group has a sum at most the sum of the whole group, which is at most `x`. So greedy's group `j + 1` fits at least up to `v(j+1)`: `e(j+1) >= v(j+1)`.
4. So greedy covers all `n` elements in at most `g` groups.

If any valid split with limit `x` exists, the greedy count is at most that split's count. So greedy gives the minimum number of groups.

**Why the numbers must not be negative.** Step 3 uses "a part of a group has a sum at most the sum of the whole group", and the "at most `m`" versus "exactly `m`" argument below uses "cutting a group never raises the largest sum". Both fail with a negative number. Take `nums = [1, 1, -4]` and the limit `x = 1`. The whole array is one part with sum -2, which is at most 1, so one part is enough. The greedy check adds 1, then tries 1 + 1 = 2, which is above 1, so it closes the group and counts 2 groups. The check says that `x = 1` needs 2 parts, but 1 part is enough.

**"At most `m`" versus "exactly `m`".** The statement asks for exactly `m` non-empty parts, and `m <= n`. If the greedy count is `g < m`, split some group further. Cutting a group into two groups never raises the largest sum. Repeat until there are `m` groups. So "at most `m` groups" and "exactly `m` groups" give the same predicate.

**Trace** (`nums = [7, 2, 5, 10, 8]`, `m = 2`; `lo = 10`, `hi = 32`):

```text
lo=10 hi=32 mid=21 groups=2  <= 2 true   -> hi = 21
lo=10 hi=21 mid=15 groups=3  <= 2 false  -> lo = 16
lo=16 hi=21 mid=18 groups=2  <= 2 true   -> hi = 18
lo=16 hi=18 mid=17 groups=3  <= 2 false  -> lo = 18
lo=18 hi=18                               -> answer 18
```

At limit 18 the greedy groups are `[7, 2, 5]` (sum 14) and `[10, 8]` (sum 18). At limit 17 they are `[7, 2, 5]`, `[10]`, `[8]`: three groups, because 10 + 8 = 18 is above 17.

**Cost.** O(n log sum): about log2(sum) rounds, each O(n). The space is O(1). The DP solution of the same statement costs O(m * n^2) time; Tell Them Apart compares the two.

### Worked Example 5: Search in a Rotated Sorted Array ([LC 33: Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/), Medium) and Find Minimum ([LC 153: Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/), Medium)

**Statement ([LC 33: Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/)).** A sorted array of distinct integers was rotated: the first 3 elements moved to the end. For example `[0, 1, 2, 4, 5, 6, 7]` becomes `[4, 5, 6, 7, 0, 1, 2]`. Given the rotated array and a target, return the index of the target, or -1. The time must be O(log n). **[LC 153: Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)** asks for the minimum value of the rotated array.

**What changed.** The array is not sorted, so `a[mid] < target` no longer shows which side to discard. A different fact must show it.

**The picture.** Plot the values of `[4, 5, 6, 7, 0, 1, 2]`:

```text
value
 7 |          *
 6 |       *
 5 |    *
 4 | *
 3 |
 2 |                   *
 1 |                *
 0 |             *
   +--------------------
     0  1  2  3  4  5  6    index
     run 1 (indices 0-3)  |  run 2 (indices 4-6)
                  the one drop: between index 3 and index 4
```

The array has two sorted runs, and one drop between them. Every value in run 2 is smaller than every value in run 1.

**One half is always sorted.** Take a window `[lo, hi]` of the array and `mid` in it. The window has at most one drop. The two halves `[lo..mid]` and `[mid..hi]` share only `mid`. The drop sits between two neighbors `i` and `i + 1`. If `i + 1 <= mid`, the drop is inside the left half. If `i >= mid`, the drop is inside the right half. It cannot be in both. So at least one half has no drop, and that half is sorted.

**How to tell which half is sorted.** If `a[lo] <= a[mid]`, the left half is sorted. If a drop were inside `[lo..mid]`, then `a[mid]` would be in run 2, and every value of run 2 is below `a[lo]`; so `a[mid] < a[lo]`. The values are distinct, so `a[lo] == a[mid]` happens only if `lo == mid`, and a one-element half is sorted. If `a[lo] > a[mid]`, the left half has the drop, so the right half is sorted.

**Use the sorted half.** The sorted half has a known smallest and largest value, so one range check tells whether the target is inside it. If it is, keep that half. If not, keep the other half.

```java
static int searchRotated(int[] a, int target) {
    int lo = 0, hi = a.length - 1;                    // closed range: we compare with a[lo], a[hi]
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (a[mid] == target) return mid;
        if (a[lo] <= a[mid]) {                        // the left half [lo..mid] is sorted
            if (a[lo] <= target && target < a[mid]) {
                hi = mid - 1;                         // the target is inside the sorted half
            } else {
                lo = mid + 1;
            }
        } else {                                      // the right half [mid..hi] is sorted
            if (a[mid] < target && target <= a[hi]) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
    }
    return -1;
}
```

**Trace** (`a = [4, 5, 6, 7, 0, 1, 2]`, `target = 0`):

```text
lo=0 hi=6 mid=3 a[mid]=7  a[lo]=4 <= 7: left half sorted;  4 <= 0 < 7 is false -> lo = 4
lo=4 hi=6 mid=5 a[mid]=1  a[lo]=0 <= 1: left half sorted;  0 <= 0 < 1 is true  -> hi = 4
lo=4 hi=4 mid=4 a[mid]=0  equal -> return 4
```

**Find the minimum ([LC 153: Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)).** Compare `a[mid]` with `a[hi]`, not with `a[lo]`. The invariant is "the minimum is in `[lo, hi]`".

- If `a[mid] > a[hi]`, then the drop is between `mid` and `hi`. The minimum is right of `mid`: `lo = mid + 1`.
- Otherwise `a[mid] <= a[hi]`, so `[mid..hi]` is sorted, and the minimum is `a[mid]` or to its left: `hi = mid`. Keep `mid`.

Comparing with `a[lo]` does not work. For `[1, 2, 3, 4]` and `[3, 4, 1, 2]`, the first `mid` is index 1, and `a[mid] >= a[lo]` holds in both arrays. But the minimum is on the left in the first array and on the right in the second. The comparison with `a[hi]` separates them: `2 <= 4` in the first (go left), `4 > 2` in the second (go right).

```java
static int findMinRotated(int[] a) {
    int lo = 0, hi = a.length - 1;           // hi is a valid index: the loop reads a[hi]
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;        // mid < hi, so mid and hi are different indices
        if (a[mid] > a[hi]) {
            lo = mid + 1;                    // the drop is right of mid
        } else {
            hi = mid;                        // a[mid..hi] is sorted: a[mid] may be the minimum
        }
    }
    return a[lo];
}
```

Trace on `[4, 5, 6, 7, 0, 1, 2]`: `lo=0 hi=6 mid=3: 7 > 2 -> lo = 4`. `lo=4 hi=6 mid=5: 1 > 2 is false -> hi = 5`. `lo=4 hi=5 mid=4: 0 > 1 is false -> hi = 4`. Return `a[4] = 0`.

The predicate view: "`a[i] <= a[n - 1]`" reads `F F F F T T T` over the indices, and the minimum is at the first true. This is the same loop as `firstTrue`.

**Cost.** O(log n): each round discards half. The space is O(1). With duplicates, this halving fails ([LC 81: Search in Rotated Sorted Array II](https://leetcode.com/problems/search-in-rotated-sorted-array-ii/)); see Tell Them Apart.

### Worked Example 6: Find Peak Element ([LC 162: Find Peak Element](https://leetcode.com/problems/find-peak-element/), Medium)

**Statement.** In an array, a peak is an element strictly greater than its neighbors. Treat `a[-1]` and `a[n]` as negative infinity. Adjacent elements are never equal. Return the index of any peak, in O(log n).

Example: `[1, 3, 5, 4, 2, 6, 3]` has two peaks: index 2 (value 5) and index 5 (value 6). Either index is a correct answer.

**What changed.** The array is not sorted, and a comparison at `mid` does not tell where one specific value is. It tells where *a* peak must exist. A search does not need a sorted array. It needs a rule that discards one half and keeps at least one answer in the other.

**Derive the rule.** Compare `a[mid]` with `a[mid + 1]`.

- If `a[mid] < a[mid + 1]`, the values rise to the right of `mid`. Walk right from `mid + 1`. Either the values keep rising until the last element. That element is greater than its left neighbor and greater than `a[n]`, which is negative infinity, so it is a peak. Or the values drop after some index `j`. Then `a[j - 1] < a[j] > a[j + 1]`, so `a[j]` is a peak. Either way, a peak exists in `[mid + 1, hi]`. So `lo = mid + 1`.
- Otherwise `a[mid] > a[mid + 1]` (no equal neighbors). Walk left from `mid`: either the values keep rising toward the left until `a[0]`, which beats `a[-1]` and its right neighbor, or they drop at some index, which is then a peak. Or `a[mid]` itself is a peak. A peak exists in `[lo, mid]`. So `hi = mid`.

**The invariant.** `a[lo - 1] < a[lo]` (or `lo = 0`) and `a[hi] > a[hi + 1]` (or `hi = n - 1`). Then a peak is in `[lo, hi]`: from `lo`, the values rise at the left edge and fall at the right edge, so a highest point lies between. Each update keeps this statement: `lo = mid + 1` sets `a[lo - 1] = a[mid] < a[mid + 1] = a[lo]`, and `hi = mid` sets `a[hi] = a[mid] > a[mid + 1]`.

```java
static int findPeak(int[] a) {
    int lo = 0, hi = a.length - 1;           // hi is a valid index: the loop reads a[mid + 1]
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;        // mid < hi, so mid + 1 <= hi is valid
        if (a[mid] < a[mid + 1]) {
            lo = mid + 1;                    // rising: a peak exists to the right
        } else {
            hi = mid;                        // falling: a[mid] or a peak to the left
        }
    }
    return lo;
}
```

**Trace** (`[1, 3, 5, 4, 2, 6, 3]`):

```text
lo=0 hi=6 mid=3  a[3]=4, a[4]=2: 4 < 2 is false -> hi = 3
lo=0 hi=3 mid=1  a[1]=3, a[2]=5: 3 < 5 is true  -> lo = 2
lo=2 hi=3 mid=2  a[2]=5, a[3]=4: 5 < 4 is false -> hi = 2
lo=2 hi=2                                       -> return 2 (value 5, a peak)
```

**Cost.** O(log n), O(1) space.

### Worked Example 7: Search a 2D Matrix ([LC 74: Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/), Medium)

**Statement.** An `m x n` matrix has sorted rows, and the first number of each row is greater than the last number of the row before it. Decide whether a target is in the matrix.

**What changed.** The data is 2D, but the order is one-dimensional. Read the matrix row by row and you get one sorted sequence of `m * n` numbers. Binary search over the position `i` in `[0, m * n - 1]`, and convert `i` to a cell: row `i / n`, column `i % n`, where `n` is the number of columns.

Example matrix `[[1, 3, 5, 7], [10, 11, 16, 20], [23, 30, 34, 60]]`. Position 6 is row `6 / 4 = 1`, column `6 % 4 = 2`: the value 16.

```java
static boolean searchMatrix(int[][] matrix, int target) {
    int rows = matrix.length, cols = matrix[0].length;
    int lo = 0, hi = rows * cols - 1;                 // positions of the flattened array
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        int value = matrix[mid / cols][mid % cols];   // row = mid / cols, column = mid % cols
        if (value == target) return true;
        if (value < target) lo = mid + 1; else hi = mid - 1;
    }
    return false;
}
```

Trace for target 13: `lo=0 hi=11 mid=5 value=11 -> lo = 6`; `lo=6 hi=11 mid=8 value=23 -> hi = 7`; `lo=6 hi=7 mid=6 value=16 -> hi = 5`; `lo=6 > hi=5` so the target is absent.

**Cost.** O(log(m * n)), O(1) space. The related problem [LC 240: Search a 2D Matrix II](https://leetcode.com/problems/search-a-2d-matrix-ii/) sorts rows and columns separately, and the rows do not continue each other. Binary search does not apply: start at the top-right cell and move left or down, O(m + n).

### Worked Example 8: the floating-point form

**Statement.** Find the square root of `x` (with `x >= 1`) to full `double` precision, without `Math.sqrt`.

**What changed.** The candidates are real numbers. A real range has no "next number", so the loops above (`lo = mid + 1`) have no meaning. Instead, keep `lo` on the false side and `hi` on the true side, and move one of them to `mid` each round. The predicate `mid * mid >= x` reads `F...F T...T`.

```java
static double sqrtByBisection(double x) {            // requires x >= 1
    double lo = 1, hi = x;                           // lo * lo <= x <= hi * hi
    for (int iteration = 0; iteration < 100; iteration++) {
        double mid = (lo + hi) / 2;
        if (mid * mid >= x) {
            hi = mid;                                // mid is on the true side
        } else {
            lo = mid;                                // mid is on the false side
        }
    }
    return hi;
}
```

**Why a fixed number of iterations.** Each round halves `hi - lo`. A `double` has 53 bits of precision. After about 52 halvings the gap between `lo` and `hi` is the gap between two neighboring doubles. After that, `(lo + hi) / 2` equals `lo` or `hi`, and no update changes anything. A loop `while (hi - lo > epsilon)` with `epsilon` below that gap never ends. A fixed count always ends. How many rounds are enough? Each round removes one bit of the gap, so the count is the number of bits between the starting width and the gap between neighboring doubles at the answer. For the square root of `x` with `x` up to 10^18, that is about 52 + 30 = 82 bits, so 100 rounds are enough. For a wider range, raise the count.

```java
public class FloatDemo {
    public static void main(String[] args) {
        // Square root of 2: the predicate mid * mid >= 2 is F..F T..T over real numbers.
        double lo = 1.0, hi = 2.0;
        for (int i = 1; i <= 100; i++) {
            double mid = (lo + hi) / 2;
            if (mid * mid >= 2) hi = mid; else lo = mid;
            if (i <= 5 || i == 20 || i == 100) {
                System.out.println("iteration " + i + ": lo = " + lo + ", hi = " + hi);
            }
        }
        System.out.println("Math.sqrt(2)  = " + Math.sqrt(2));

        // A loop that waits for hi - lo <= 1e-18 never ends: the width stops shrinking at 2.2e-16.
        lo = 1.0;
        hi = 2.0;
        int rounds = 0;
        while (hi - lo > 1e-18 && rounds < 200) {
            double mid = (lo + hi) / 2;
            if (mid * mid >= 2) hi = mid; else lo = mid;
            rounds++;
        }
        System.out.println("after " + rounds + " rounds: hi - lo = " + (hi - lo));
    }
}
```

<!-- expected-output -->
```text
iteration 1: lo = 1.0, hi = 1.5
iteration 2: lo = 1.25, hi = 1.5
iteration 3: lo = 1.375, hi = 1.5
iteration 4: lo = 1.375, hi = 1.4375
iteration 5: lo = 1.40625, hi = 1.4375
iteration 20: lo = 1.4142131805419922, hi = 1.4142141342163086
iteration 100: lo = 1.414213562373095, hi = 1.4142135623730951
Math.sqrt(2)  = 1.4142135623730951
after 200 rounds: hi - lo = 2.220446049250313E-16
```

The first loop converges to the neighbors `1.414213562373095` and `1.4142135623730951`, and `Math.sqrt(2)` is the second one. The second loop waits for `hi - lo <= 1e-18`. The width stops at 2.2e-16 (the gap between neighboring doubles near 1.4), so the loop ran until the guard stopped it at 200 rounds. This form appears in [LC 774: Minimize Max Distance to Gas Station](https://leetcode.com/problems/minimize-max-distance-to-gas-station/) (premium), where the answer is a real distance.

## Implementation

The templates, each derived above:

| Template | Derived in | Use |
|---|---|---|
| Exact match, closed range | Derivation, Question 1 | Return an index of the target, or -1 |
| `lowerBound`, `upperBound` | Derivation, Question 2 | First position `>= target`, first position `> target`, first and last occurrence |
| `firstTrue` | Derivation, Question 5 | Minimize: the smallest answer where a monotone predicate is true |
| `lastTrue` | Derivation, Question 5 | Maximize: the largest answer where a monotone predicate is true |
| Fixed-iteration real search | Worked Example 8 | Real-valued answers |

Explanations of lines that are not obvious:

- `hi = a.length` in `lowerBound`: the answer can be `n`. With `hi = n - 1`, the search can never return `n`.
- `lo < hi` in the half-open form, `lo <= hi` in the closed form: in the half-open form, `lo == hi` means one candidate answer is left, and it needs no test. In the closed form, `lo == hi` means one position is left, and that position is not tested yet.
- `lo = mid + 1` and `hi = mid`: the first update drops `mid`, which is safe because `mid` is not the answer. The second keeps `mid`, because `mid` may be the answer.
- `hi - (hi - lo) / 2` (upper middle) belongs only to the last-true loop, where the update is `lo = mid`.
- In Java, `(pile + speed - 1) / speed` is the ceiling of `pile / speed` for positive integers. Use `long` for sums.

### The Java library: `Arrays.binarySearch` and `Collections.binarySearch`

Both methods require a sorted input and return an `int`:

- If the key is present, they return its index.
- If the key is absent, they return `-(insertion point) - 1`. The insertion point is the index of the first element greater than the key, or the length if every element is smaller. The return value is at least 0 if and only if the key is found.
- If the input has several elements equal to the key, there is no guarantee which index they return. (JDK 21 javadoc for `Arrays.binarySearch` and `Collections.binarySearch`.)
- `Collections.binarySearch` on a `LinkedList` (a list without fast index access) still makes O(log n) comparisons, but it walks the links, so the cost is O(n). Use it on an `ArrayList`.

So for a lower bound with duplicates, do not call these methods. Write the loop above. To recover the insertion point from a negative result, compute `-result - 1`.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibraryDemo {
    public static void main(String[] args) {
        int[] a = {1, 3, 5, 7};
        System.out.println("key 5 is at index    " + Arrays.binarySearch(a, 5));
        System.out.println("key 4 is absent:     " + Arrays.binarySearch(a, 4)
                + "  = -(insertion point 2) - 1");
        System.out.println("key 0 is absent:     " + Arrays.binarySearch(a, 0)
                + "  = -(insertion point 0) - 1");
        System.out.println("key 9 is absent:     " + Arrays.binarySearch(a, 9)
                + "  = -(insertion point 4) - 1");
        List<Integer> list = new ArrayList<>(List.of(1, 3, 5, 7));
        System.out.println("list, key 4 absent:  " + Collections.binarySearch(list, 4));

        int[] dup = {1, 2, 2, 2, 2, 2, 3};
        System.out.println("dup, key 2 -> index  " + Arrays.binarySearch(dup, 2)
                + "  (the javadoc promises no particular index)");
        int insertionPoint = -Arrays.binarySearch(a, 4) - 1;
        System.out.println("insertion point of 4 = " + insertionPoint);
    }
}
```

<!-- expected-output -->
```text
key 5 is at index    2
key 4 is absent:     -3  = -(insertion point 2) - 1
key 0 is absent:     -1  = -(insertion point 0) - 1
key 9 is absent:     -5  = -(insertion point 4) - 1
list, key 4 absent:  -3
dup, key 2 -> index  3  (the javadoc promises no particular index)
insertion point of 4 = 2
```

The answer for `dup` is whatever the JDK 21 implementation reaches. The javadoc does not promise it, so the code must not depend on it.

### A complete program with traces and edge cases

The program below runs the lower bound trace, the Koko trace, and the Magnetic Force trace used in the sections above. It then tests the edge cases: an empty array, one element, all elements equal, a target before the first and after the last element, one pile, and `h` equal to the number of piles.

```java
import java.util.Arrays;

public class BinarySearchLab {
    public static void main(String[] args) {
        // 1. Lower bound on an array with duplicates.
        int[] dups = {1, 2, 2, 2, 4, 4, 7, 9};
        System.out.println("lowerBound(" + Arrays.toString(dups) + ", 2)");
        int lo = 0, hi = dups.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            boolean isTrue = dups[mid] >= 2;              // predicate: a[i] >= target
            System.out.printf("  lo=%d hi=%d mid=%d a[mid]=%d  a[mid] >= 2 is %s -> %s%n",
                    lo, hi, mid, dups[mid], isTrue, isTrue ? "hi = mid" : "lo = mid + 1");
            if (isTrue) hi = mid; else lo = mid + 1;
        }
        System.out.println("  first index with a[i] >= 2: " + lo);
        System.out.println("  first and last position of 2: [" + lowerBound(dups, 2) + ", "
                + (upperBound(dups, 2) - 1) + "]");
        System.out.println("  first and last position of 3: lowerBound = " + lowerBound(dups, 3)
                + ", upperBound = " + upperBound(dups, 3) + " (equal, so the value is absent)");

        // 2. Koko Eating Bananas: first speed with hours(speed) <= h.
        int[] piles = {3, 6, 7, 11};
        int h = 8;
        System.out.println("Koko piles " + Arrays.toString(piles) + ", h = " + h);
        lo = 1;
        hi = 11;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            long hours = hoursNeeded(piles, mid);
            boolean ok = hours <= h;
            System.out.printf("  lo=%2d hi=%2d mid=%2d hours=%2d  hours <= 8 is %-5s -> %s%n",
                    lo, hi, mid, hours, ok, ok ? "hi = mid" : "lo = mid + 1");
            if (ok) hi = mid; else lo = mid + 1;
        }
        System.out.println("  minimum speed: " + lo);

        // 3. Magnetic Force: last distance with placed(distance) >= m.
        int[] pos = {1, 2, 3, 4, 7, 9};
        int m = 3;
        System.out.println("Balls at " + Arrays.toString(pos) + ", m = " + m);
        lo = 1;
        hi = (pos[pos.length - 1] - pos[0]) / (m - 1);
        while (lo < hi) {
            int mid = hi - (hi - lo) / 2;
            int placed = ballsPlaced(pos, mid);
            boolean ok = placed >= m;
            System.out.printf("  lo=%d hi=%d mid=%d placed=%d  placed >= 3 is %-5s -> %s%n",
                    lo, hi, mid, placed, ok, ok ? "lo = mid" : "hi = mid - 1");
            if (ok) lo = mid; else hi = mid - 1;
        }
        System.out.println("  largest minimum distance: " + lo);

        // 4. Edge cases.
        System.out.println("edge: lowerBound on empty array = " + lowerBound(new int[0], 5));
        System.out.println("edge: lowerBound on {4}, target 4 = " + lowerBound(new int[] {4}, 4)
                + ", target 5 = " + lowerBound(new int[] {4}, 5));
        int[] same = {6, 6, 6, 6};
        System.out.println("edge: all equal, first and last of 6 = [" + lowerBound(same, 6) + ", "
                + (upperBound(same, 6) - 1) + "]");
        System.out.println("edge: target before first = " + lowerBound(dups, 0)
                + ", after last = " + lowerBound(dups, 10));
        System.out.println("edge: Koko with one pile {10}, h = 3 -> speed "
                + minSpeed(new int[] {10}, 3));
        System.out.println("edge: Koko with h = piles.length -> speed "
                + minSpeed(new int[] {30, 11, 23, 4, 20}, 5));
    }

    static long hoursNeeded(int[] piles, int speed) {
        long hours = 0;
        for (int pile : piles) hours += (pile + speed - 1) / speed;   // ceil(pile / speed)
        return hours;
    }

    static int minSpeed(int[] piles, int h) {
        int lo = 1, hi = Arrays.stream(piles).max().getAsInt();
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (hoursNeeded(piles, mid) <= h) hi = mid; else lo = mid + 1;
        }
        return lo;
    }

    static int ballsPlaced(int[] sortedPos, int gap) {
        int placed = 1, last = sortedPos[0];
        for (int i = 1; i < sortedPos.length; i++) {
            if (sortedPos[i] - last >= gap) {
                placed++;
                last = sortedPos[i];
            }
        }
        return placed;
    }

    static int lowerBound(int[] a, int target) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] < target) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    static int upperBound(int[] a, int target) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] <= target) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
}
```

<!-- expected-output -->
```text
lowerBound([1, 2, 2, 2, 4, 4, 7, 9], 2)
  lo=0 hi=8 mid=4 a[mid]=4  a[mid] >= 2 is true -> hi = mid
  lo=0 hi=4 mid=2 a[mid]=2  a[mid] >= 2 is true -> hi = mid
  lo=0 hi=2 mid=1 a[mid]=2  a[mid] >= 2 is true -> hi = mid
  lo=0 hi=1 mid=0 a[mid]=1  a[mid] >= 2 is false -> lo = mid + 1
  first index with a[i] >= 2: 1
  first and last position of 2: [1, 3]
  first and last position of 3: lowerBound = 4, upperBound = 4 (equal, so the value is absent)
Koko piles [3, 6, 7, 11], h = 8
  lo= 1 hi=11 mid= 6 hours= 6  hours <= 8 is true  -> hi = mid
  lo= 1 hi= 6 mid= 3 hours=10  hours <= 8 is false -> lo = mid + 1
  lo= 4 hi= 6 mid= 5 hours= 8  hours <= 8 is true  -> hi = mid
  lo= 4 hi= 5 mid= 4 hours= 8  hours <= 8 is true  -> hi = mid
  minimum speed: 4
Balls at [1, 2, 3, 4, 7, 9], m = 3
  lo=1 hi=4 mid=3 placed=3  placed >= 3 is true  -> lo = mid
  lo=3 hi=4 mid=4 placed=2  placed >= 3 is false -> hi = mid - 1
  largest minimum distance: 3
edge: lowerBound on empty array = 0
edge: lowerBound on {4}, target 4 = 0, target 5 = 1
edge: all equal, first and last of 6 = [0, 3]
edge: target before first = 0, after last = 8
edge: Koko with one pile {10}, h = 3 -> speed 4
edge: Koko with h = piles.length -> speed 30
```

## Complexity and Trade-offs

| Variant | Time | Space | Reason |
|---|---|---|---|
| Exact match, lower bound, upper bound | O(log n) | O(1) | The space at least halves each round |
| Search on the answer | O(C * log R) | O(1) extra | `R` is the size of the answer range, `C` is the cost of one feasibility check |
| Koko ([LC 875: Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/)) | O(n log max(piles)) | O(1) | `C = O(n)`, `R = max(piles)` |
| Capacity to Ship ([LC 1011: Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/)) | O(n log sum) | O(1) | `C = O(n)`, `R <= sum` |
| Split Array ([LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/)) | O(n log sum) | O(1) | `C = O(n)`, `R <= sum` |
| Magnetic Force ([LC 1552: Magnetic Force Between Two Balls](https://leetcode.com/problems/magnetic-force-between-two-balls/)) | O(n log n + n log(max - min)) | O(1) beyond the sort | Sort once, then `C = O(n)` |
| Rotated array, distinct values ([LC 33: Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/), [LC 153: Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)) | O(log n) | O(1) | One half is sorted, so the other half is discarded |
| Rotated array with duplicates ([LC 81: Search in Rotated Sorted Array II](https://leetcode.com/problems/search-in-rotated-sorted-array-ii/), [LC 154: Find Minimum in Rotated Sorted Array II](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array-ii/)) | O(n) worst case | O(1) | Equal values at `lo`, `mid`, `hi` hide the sorted half |
| Peak element ([LC 162: Find Peak Element](https://leetcode.com/problems/find-peak-element/)) | O(log n) | O(1) | The slope at `mid` shows which half keeps a peak |
| Matrix with row-to-row order ([LC 74: Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/)) | O(log(m * n)) | O(1) | One sorted array of `m * n` numbers |
| Real-valued answer | O(100 * C) | O(1) | A fixed number of halvings |
| Median of two sorted arrays ([LC 4: Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/)) | O(log(min(m, n))) | O(1) | Binary search on the cut in the shorter array |

**Alternatives and when each is better.**

| Alternative | Cost | Better than binary search when |
|---|---|---|
| Linear scan | O(n) per query | The array is unsorted and there is one query. Sorting costs O(n log n), more than one scan |
| Hash set / hash map | O(1) per lookup after O(n) to build | You need only "present or absent" or a count. A hash map cannot answer "first element >= x" |
| `TreeMap` / `TreeSet` (`floorKey`, `ceilingKey`) | O(log n) per query and per update | The data changes between queries. Binary search on a sorted array needs a re-sort or an O(n) insert |
| Sort, then scan | O(n log n) | The problem asks for all answers, not one |
| DP | Depends on the recurrence | The objective is not monotone, or you need more than the optimal value (see Tell Them Apart) |

## Recognition Guide

**Signals in the statement:**

- "Minimize the maximum" or "maximize the minimum".
- "Smallest value such that ..." or "largest value such that ..." for a number that is not in the input.
- "Is it possible to achieve X?", "within `D` days", "with at most `K` operations".
- "Allocate", "divide", "split" into groups with a limit.
- The statement says the array is sorted, or sorted and rotated, and asks for O(log n).
- "Kth smallest" in a set that is too large to store (a multiplication table, the pairs of an array).

**Signals in the constraints:**

- The answer can be as large as 10^9 or 10^18, so trying every value is too slow.
- `n` is up to 10^5 or 10^6, with a time limit that allows O(n log n) but not O(n^2).
- A check "does answer `X` work?" is simple, and runs in O(n) or O(n log n).

**The steps to identify a search on the answer.**

1. Is there a range of possible answers? Can you name the smallest and the largest, with a reason for each?
2. Is there a constraint that makes some answers invalid ("within `D` days", "at most `K` parts")?
3. Is the validity monotonic: if `X` works, does `X + 1` (or `X - 1`) work? Argue it by transforming a solution for `X` into a solution for the neighbor.
4. Can you check one candidate in O(n) or O(n log n)?

| Pattern | Minimize (first true) | Maximize (last true) |
|---|---|---|
| Allocation / distribution | Smallest capacity that finishes in time | Largest items per person |
| Distance / gap | Smallest largest gap | Largest smallest gap |
| Rate / speed | Smallest speed that finishes | Largest tasks per unit of time |
| Division / partitioning | Smallest largest part sum | Largest smallest part sum |

### The monotonicity test, with a counterexample

Binary search on the answer is correct only if the predicate is monotone. The test is to pick two answers `x < y` and ask: "if `x` works, does `y` work?" (for minimize), or "if `y` works, does `x` work?" (for maximize). Give a reason that holds for every input, such as "the same plan stays valid".

A problem can look like "minimize the maximum" and fail the test. Take this statement: *cut the array into contiguous parts that all have the same sum `x`. Find the smallest possible `x`.* It looks like [LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/): cut an array into parts, minimize a number. But the condition here is "every part has sum exactly `x`", not "every part has sum at most `x`".

For `nums = [3, 3, 3, 3, 6]`, `x = 6` works (`[3, 3] [3, 3] [6]`), but `x = 7` does not: no prefix sum equals 7.

```java
public class NonMonotone {
    // Can the array be cut into contiguous parts that all have sum exactly x?
    static boolean cutIntoPartsOfSum(int[] nums, int x) {
        int cur = 0;
        for (int v : nums) {
            cur += v;
            if (cur == x) cur = 0;       // a part ends here
            else if (cur > x) return false;
        }
        return cur == 0;
    }

    public static void main(String[] args) {
        int[] nums = {3, 3, 3, 3, 6};
        int total = 18;
        StringBuilder row = new StringBuilder();
        for (int x = 1; x <= total; x++) row.append(cutIntoPartsOfSum(nums, x) ? 'T' : 'F');
        System.out.println("x = 1..18: " + row);

        // Brute force: try every x from 1 up, stop at the first that works.
        int brute = -1;
        for (int x = 1; x <= total && brute < 0; x++) if (cutIntoPartsOfSum(nums, x)) brute = x;

        // Binary search for the first true, as if the row were F..F T..T.
        int lo = 1, hi = total;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            boolean ok = cutIntoPartsOfSum(nums, mid);
            System.out.println("lo = " + lo + ", hi = " + hi + ", mid = " + mid + " -> " + ok);
            if (ok) hi = mid; else lo = mid + 1;
        }
        System.out.println("brute force answer = " + brute + ", binary search answer = " + lo);
    }
}
```

<!-- expected-output -->
```text
x = 1..18: FFFFFTFFTFFFFFFFFT
lo = 1, hi = 18, mid = 9 -> true
lo = 1, hi = 9, mid = 5 -> false
lo = 6, hi = 9, mid = 7 -> false
lo = 8, hi = 9, mid = 8 -> false
brute force answer = 6, binary search answer = 9
```

The row reads `F F F F F T F F T ...`: true at 6, false at 7, true at 9, false at 10 to 17, true at 18. The row is not `F...F T...T`. The binary search tests `x = 9` first, finds true, drops everything above 9, and then every middle value below 9 is false. It returns 9. The brute force returns 6. The wrong answer comes from the predicate, not from the loop. The loop is correct; its assumption is false.

The preconditions of the technique, each with a counterexample:

| Precondition | A counterexample | What goes wrong |
|---|---|---|
| The array is sorted (for index search) | Search for 1 in the unsorted `[2, 3, 1]` with the closed loop | `mid = 1`: `3 > 1`, so `hi = 0`. Then `mid = 0`: `2 > 1`, so `hi = -1`. The loop returns -1, but 1 is at index 2 |
| The predicate is monotone (for search on the answer) | The exact-sum cut above | The search returns 9 instead of 6 |
| The answer space contains the answer | `lowerBound` with `hi = n - 1` on `[1, 3, 5]` for target 7 | The answer 3 is outside `[0, 2]`. The loop returns 2 |
| The feasibility check costs O(n) or O(n log n) | A check that tries all subsets | The search is correct but `log(range)` times an exponential check is still exponential |
| The values are integers (for the `lo`, `hi`, `mid` updates) | `lo = 1`, `hi = 2`, real answer 1.414... | `lo = mid + 1` jumps over the answer. Use the floating-point form |

### Looks similar, but needs another technique

| Problem | The property that rules out binary search | The right pattern |
|---|---|---|
| [LC 240: Search a 2D Matrix II](https://leetcode.com/problems/search-a-2d-matrix-ii/) | Each row and column is sorted, but a row does not continue the previous row. Order is not one-dimensional | Staircase walk from the top-right cell, O(m + n). See [Two Pointers](../02-two-pointers/README.md) |
| [LC 215: Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) | The set is stored, the values are not in a range with a monotone check, and `k` can be small. A heap keeps the `k` largest | [Heap and Priority Queue](../16-heap-priority-queue/README.md) |
| [LC 1004: Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/) | A window can grow and shrink with a monotone condition, so one pass finds the answer in O(n). Binary search on the length also works but costs O(n log n) | [Sliding Window](../01-sliding-window/README.md) |
| [LC 1235: Maximum Profit in Job Scheduling](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) | The objective is a sum over a chosen set, not a threshold with a monotone check. Binary search finds the previous compatible job inside a DP | [Dynamic Programming](../15-dynamic-programming/README.md) |
| Find a value in an unsorted array, many queries | No order, so no half can be discarded. A hash set answers in O(1) | [Hashing and Frequency](../05-hashing-frequency/README.md) |

## Common Mistakes

Each mistake has a failing input. The program after the list runs the inputs of mistakes 1, 2, 5, 6, 7, 9, 10, 11, 12, and 13 and prints the wrong result next to the correct one. Mistakes 3 and 4 run in Derivation, Question 3, and mistake 8 runs in Question 4.

1. **Wrong bounds on the answer.** Koko with `lo = 0`: for `piles = [1, 1]`, the first `mid` is 0, and `ceil(p / 0)` divides by zero. For ship capacity with `lo = 0`: `weights = [1, 10]`, `days = 2`, the search returns 1 (a ship that cannot carry the weight-10 package). Fix: `lo = 1` for Koko, `lo = max(weights)` for capacity. Take each bound from a reason, as in the Worked Examples.
2. **The answer is outside the search space.** `lowerBound` with `hi = n - 1` on `[1, 3, 5]` and target 7 returns 2, but the correct answer is 3. Fix: `hi = n`, because the answer can be `n`.
3. **The loop never ends** (`lo = mid` with the lower middle). Input `[1, 2]`, target 5, last index with `a[i] <= target`. Fix: use the upper middle `hi - (hi - lo) / 2`.
4. **The lost element** (`hi = mid - 1` when `mid` can be the answer). Input `[1, 5, 9]`, first index with `a[i] >= 5`. The loop returns 0 instead of 1. Fix: `hi = mid`.
5. **A wrong feasibility check: groups start at 0.** For `nums = [7, 2, 5, 10, 8]` and `m = 2`, a check that starts `groups = 0` counts one group too few and returns 14, not 18. Fix: `groups = 1`, because the first group exists before the first element.
6. **A check that does not reject an element above the limit.** The old template `groups++; current = num` accepts a `num` that is already above `mid`. Ship weights `[1, 10]` with limit 5 pass as 2 groups. Fix: start `lo` at `max(weights)`, so the check never sees a limit below the heaviest element, or return false inside the check.
7. **The wrong direction.** For a minimization, `if (hours <= h) lo = mid + 1` returns the largest speed. Koko on `[3, 6, 7, 11]`, `h = 8` returns 11, not 4. Fix: when the check passes in a minimization, `hi = mid`. In a maximization, `lo = mid`.
8. **Overflow of `mid`.** `(lo + hi) / 2` with `lo = 1,500,000,000` and `hi = 2,000,000,000` gives -397,483,648. Fix: `lo + (hi - lo) / 2`.
9. **Overflow of the sum in the check.** At speed 1, three piles of 10^9 sum to 3 * 10^9. An `int` sum wraps to -1,294,967,296, which is at most `h`, so the check says that speed 1 works. Fix: a `long` sum.
10. **Reading past the end.** The peak search with `hi = n` reads `a[mid + 1]`. For `[1, 2, 3]` it throws `ArrayIndexOutOfBoundsException` (index 3). Fix: `hi = n - 1` whenever the loop reads `a[hi]` or `a[mid + 1]`.
11. **A strict comparison in the rotated search.** With `a[lo] < a[mid]`, the array `[3, 1]` and target 1 returns -1. When `lo == mid`, `a[lo] == a[mid]`, and the left half (one element) is sorted. Fix: `a[lo] <= a[mid]`.
12. **An unsorted input in a check that assumes order.** Magnetic Force on `[7, 1, 4, 2, 3]` with `m = 3` returns 1 instead of 3, because the greedy placement walks in array order. Fix: sort first.
13. **No guard for empty input.** `findMinRotated(new int[0])` throws `ArrayIndexOutOfBoundsException`: `lo = 0`, `hi = -1`, the loop is skipped, and `a[0]` does not exist. Check `a.length == 0` first if the statement allows it.

```java
import java.util.Arrays;

public class Mistakes {
    public static void main(String[] args) {
        // Mistake: Koko with lo = 0. The first middle value can be 0.
        try {
            int lo = 0, hi = 1;
            int mid = lo + (hi - lo) / 2;
            long hours = (1 + mid - 1) / mid;
            System.out.println("koko lo=0: hours = " + hours);
        } catch (ArithmeticException e) {
            System.out.println("koko lo=0, piles {1, 1}: ArithmeticException: " + e.getMessage());
        }

        // Mistake: the Koko check sums the hours in an int.
        int[] big = {1_000_000_000, 1_000_000_000, 1_000_000_000};
        int h = 1_000_000_000;
        int hoursInt = 0;
        long hoursLong = 0;
        for (int p : big) {
            hoursInt += (p + 1 - 1) / 1;       // speed 1
            hoursLong += (p + 1 - 1) / 1;
        }
        System.out.println("speed 1: int sum = " + hoursInt + " (<= h is " + (hoursInt <= h)
                + "), long sum = " + hoursLong + " (<= h is " + (hoursLong <= h) + ")");
        int lo, hi;

        // Mistake: ship capacity with lo = 0 and a check that does not reject a heavy package.
        int[] w = {1, 10};
        lo = 0;
        hi = 11;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            int groups = 1, cur = 0;
            for (int x : w) {
                if (cur + x > mid) { groups++; cur = x; } else cur += x;
            }
            if (groups <= 2) hi = mid; else lo = mid + 1;
        }
        System.out.println("ship lo=0, weights {1, 10}, 2 days: capacity " + lo + ", correct 10");

        // Mistake: count groups from 0 in the split check.
        int[] nums = {7, 2, 5, 10, 8};
        lo = 10;
        hi = 32;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            int groups = 0, cur = 0;
            for (int x : nums) {
                if (cur + x > mid) { groups++; cur = 0; }
                cur += x;
            }
            if (groups <= 2) hi = mid; else lo = mid + 1;
        }
        System.out.println("split groups from 0, nums " + Arrays.toString(nums) + ", m = 2: " + lo
                + ", correct 18");

        // Mistake: wrong direction for a minimization (lo = mid + 1 when the check passes).
        int[] piles = {3, 6, 7, 11};
        lo = 1;
        hi = 11;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            long hours = 0;
            for (int p : piles) hours += (p + mid - 1) / mid;
            if (hours <= 8) lo = mid + 1; else hi = mid;
        }
        System.out.println("koko wrong direction: speed " + lo + ", correct 4");

        // Mistake: lower bound with hi = n - 1, so the answer n cannot be returned.
        int[] a = {1, 3, 5};
        lo = 0;
        hi = a.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] < 7) lo = mid + 1; else hi = mid;
        }
        System.out.println("lowerBound hi = n - 1, {1, 3, 5}, target 7: " + lo + ", correct 3");

        // Mistake: peak search with hi = n reads a[mid + 1] past the end.
        int[] rising = {1, 2, 3};
        try {
            lo = 0;
            hi = rising.length;
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;
                if (rising[mid] < rising[mid + 1]) lo = mid + 1; else hi = mid;
            }
            System.out.println("peak hi = n: " + lo);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("peak hi = n, {1, 2, 3}: " + e.getMessage());
        }

        // Mistake: rotated search with a strict test a[lo] < a[mid].
        int[] rot = {3, 1};
        int target = 1;
        lo = 0;
        hi = 1;
        int found = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (rot[mid] == target) { found = mid; break; }
            if (rot[lo] < rot[mid]) {          // wrong: lo == mid means a[lo] == a[mid]
                if (rot[lo] <= target && target < rot[mid]) hi = mid - 1; else lo = mid + 1;
            } else {
                if (rot[mid] < target && target <= rot[hi]) lo = mid + 1; else hi = mid - 1;
            }
        }
        System.out.println("rotated strict <, {3, 1}, target 1: " + found + ", correct 1");

        // Mistake: no guard for empty input in the rotated-array minimum.
        try {
            int[] empty = new int[0];
            lo = 0;
            hi = empty.length - 1;
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;
                if (empty[mid] > empty[hi]) lo = mid + 1; else hi = mid;
            }
            System.out.println("min of empty: " + empty[lo]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("rotated minimum on an empty array: " + e.getMessage());
        }

        // Mistake: Magnetic Force without sorting the positions.
        int[] pos = {7, 1, 4, 2, 3};
        int m = 3;
        lo = 1;
        hi = 3;
        while (lo < hi) {
            int mid = hi - (hi - lo) / 2;
            int placed = 1, last = pos[0];
            for (int i = 1; i < pos.length; i++) {
                if (pos[i] - last >= mid) { placed++; last = pos[i]; }
            }
            if (placed >= m) lo = mid; else hi = mid - 1;
        }
        System.out.println("magnetic force unsorted, " + Arrays.toString(pos) + ", m = 3: " + lo
                + ", correct 3");
    }
}
```

<!-- expected-output -->
```text
koko lo=0, piles {1, 1}: ArithmeticException: / by zero
speed 1: int sum = -1294967296 (<= h is true), long sum = 3000000000 (<= h is false)
ship lo=0, weights {1, 10}, 2 days: capacity 1, correct 10
split groups from 0, nums [7, 2, 5, 10, 8], m = 2: 14, correct 18
koko wrong direction: speed 11, correct 4
lowerBound hi = n - 1, {1, 3, 5}, target 7: 2, correct 3
peak hi = n, {1, 2, 3}: Index 3 out of bounds for length 3
rotated strict <, {3, 1}, target 1: -1, correct 1
rotated minimum on an empty array: Index 0 out of bounds for length 0
magnetic force unsorted, [7, 1, 4, 2, 3], m = 3: 1, correct 3
```

## Practice Problems

The ladder goes from easy to hard. Each row adds one thing to the problem before it. The last column tells you how to work each problem. For the five problems that have no hint form here (P1, P2, P8, P9, P10), attempt the problem first, for 15 minutes, and only then read the section that the column names.

| # | Problem | LC | Difficulty | What it adds | How to work it |
|---|---|---|---|---|---|
| P1 | [Binary Search](https://leetcode.com/problems/binary-search/) | 704 | Easy | The closed-range exact match | Attempt first. Then read Derivation, Question 1 |
| P2 | [Search Insert Position](https://leetcode.com/problems/search-insert-position/) | 35 | Easy | The answer can be `n` | Attempt first. Then read Derivation, Question 2 |
| P3 | [First Bad Version](https://leetcode.com/problems/first-bad-version/) | 278 | Easy | The array is replaced by a function: a predicate you call | Hints and solution below |
| P4 | [Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) | 34 | Medium | Two boundaries, and a "not found" test | Hints and solution below |
| P5 | [Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) | 153 | Medium | The array is not sorted, but a predicate over indices is monotone | Hints and solution below |
| P6 | [Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) | 33 | Medium | Two sorted runs, and a range check on the target | Hints and solution below |
| P7 | [Find Peak Element](https://leetcode.com/problems/find-peak-element/) | 162 | Medium | No order at all | Hints and solution below |
| P8 | [Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) | 875 | Medium | No array: the answer is a number in a range | Attempt first. Then read Worked Example 1 |
| P9 | [Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) | 1011 | Medium | The check is a simulation, and the range has two bounds | Attempt first. Then read Worked Example 2 |
| P10 | [Magnetic Force Between Two Balls](https://leetcode.com/problems/magnetic-force-between-two-balls/) | 1552 | Medium | Maximize instead of minimize | Attempt first. Then read Worked Example 3 |
| P11 | [Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) | 410 | Hard | The same shape as P9 under another statement, and a proof of the check | Hints and solution below. Worked Example 4 has the full proof |
| P12 | [Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/) | 4 | Hard | A search over a cut position in two arrays at once | Hints and solution below |

### P3. First Bad Version ([LC 278](https://leetcode.com/problems/first-bad-version/), Easy)

Versions `1..n` are numbered in order. Once a version is bad, every later version is bad. A function `isBadVersion(v)` tells whether version `v` is bad. Find the first bad version with as few calls as possible. Example: `n = 5`, the first bad version is 4.

??? tip "Hint 1: where to look"

    Write the answers of `isBadVersion(1), ..., isBadVersion(n)` for the example. What pattern do they form?

??? tip "Hint 2: the idea"

    The answers read `false, false, false, true, true`. That is a monotone predicate, and the first bad version is the first true. Version `n` is a sentinel: the problem guarantees that some version is bad, so `isBadVersion(n)` is true.

??? tip "Hint 3: the plan"

    Set `lo = 1` and `hi = n`. While `lo < hi`, test `mid`. If it is bad, `hi = mid`. If not, `lo = mid + 1`. Return `lo`.

??? success "Solution"

    The predicate is the API call. `hi = n` is a true sentinel, so no extra position is needed.

    ```text
    n = 5, first bad = 4
    lo=1 hi=5 mid=3  isBad(3) = false -> lo = 4
    lo=4 hi=5 mid=4  isBad(4) = true  -> hi = 4
    lo=4 hi=4        -> return 4
    ```

    ```java
    static int firstBad = 4;                                // test stub for the API

    static boolean isBadVersion(int version) {
        return version >= firstBad;
    }

    static int firstBadVersion(int n) {
        int lo = 1, hi = n;                // n is bad, so the answer is in [1, n]
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;  // for n near 2^31, lo + hi would overflow
            if (isBadVersion(mid)) {
                hi = mid;                  // mid may be the first bad version
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }
    ```

    The cost is O(log n) calls. The mistake that this problem invites: `(lo + hi) / 2`. If `n` is close to 2^31 - 1, then `lo + hi` passes 2,147,483,647, and the middle becomes negative. This is the reason for the formula, not a constraint quoted from the problem.

### P4. Find First and Last Position of Element in Sorted Array ([LC 34](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/), Medium)

A sorted array may hold the target several times. Return the first and the last index of the target, or `[-1, -1]` if it is absent. The time must be O(log n). Example: `[5, 7, 7, 8, 8, 10]`, target 8: the answer is `[3, 4]`.

??? tip "Hint 1: where to look"

    Which two predicates over the indices change from false to true exactly at the first 8 and just after the last 8?

??? tip "Hint 2: the idea"

    "`a[i] >= 8`" first becomes true at the first 8. "`a[i] > 8`" first becomes true just after the last 8. Two first-true searches give both ends.

??? tip "Hint 3: the plan"

    Compute `first = lowerBound(a, target)` and `after = upperBound(a, target)`. If `first == after`, the target is absent, so return `[-1, -1]`. Otherwise return `[first, after - 1]`.

??? success "Solution"

    The brute force scans the array from both ends: O(n). With 10^5 equal elements, even a binary search that finds one match and then walks left and right costs O(n). Two boundary searches cost O(log n) each, whatever the number of duplicates.

    ```text
    a = [5, 7, 7, 8, 8, 10], target = 8
    lowerBound: first i with a[i] >= 8  -> 3
    upperBound: first i with a[i] >  8  -> 5
    first = 3, after = 5, first != after -> [3, 5 - 1] = [3, 4]
    ```

    ```java
    static int[] searchRange(int[] a, int target) {
        int first = boundary(a, target, false);
        int after = boundary(a, target, true);
        if (first == after) return new int[] {-1, -1};   // no element equals target
        return new int[] {first, after - 1};
    }

    // strict = false: first index with a[i] >= target. strict = true: first index with a[i] > target.
    static int boundary(int[] a, int target, boolean strict) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            boolean isTrue = strict ? a[mid] > target : a[mid] >= target;
            if (isTrue) hi = mid; else lo = mid + 1;
        }
        return lo;
    }
    ```

    The cost is O(log n) time and O(1) space. The mistake that this problem invites: testing `a[first] == target` without first checking `first < n`. For target 11 in the array above, `lowerBound` returns 6, and `a[6]` does not exist. The test `first == after` avoids that read.

### P5. Find Minimum in Rotated Sorted Array ([LC 153](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/), Medium)

A sorted array of distinct values was rotated. Return its minimum in O(log n). Example: `[3, 4, 5, 1, 2]`, the answer is 1.

??? tip "Hint 1: where to look"

    The array is not sorted, so "is `a[i]` at least the target?" has no meaning. Is there another yes/no question about index `i`, which compares `a[i]` with one fixed element, whose answers over `i` read `false, ..., false, true, ..., true`? Try `[3, 4, 5, 1, 2]`.

??? tip "Hint 2: the idea"

    The answers read `false, false, false, true, true`. The minimum is the first element that is at most the last element. The array is not sorted, but this predicate is monotone.

??? tip "Hint 3: the plan"

    Use the first-true loop on the indices `[0, n - 1]`, with the predicate `a[i] <= a[n - 1]`. Index `n - 1` is true, so it is a sentinel.

??? success "Solution"

    The brute force scans for the minimum in O(n). The predicate view needs no new idea: it is `firstTrue` over indices.

    ```text
    a = [3, 4, 5, 1, 2], last = 2
    index    0  1  2  3  4
    a[i]<=2  F  F  F  T  T
    lo=0 hi=4 mid=2  5 <= 2 false -> lo = 3
    lo=3 hi=4 mid=3  1 <= 2 true  -> hi = 3
    lo=3 hi=3        -> return a[3] = 1
    ```

    ```java
    static int findMin(int[] a) {
        int last = a[a.length - 1];
        int lo = 0, hi = a.length - 1;      // index n - 1 satisfies the predicate
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] <= last) hi = mid; else lo = mid + 1;
        }
        return a[lo];
    }
    ```

    The cost is O(log n), O(1) space. The mistake that this problem invites: comparing `a[mid]` with `a[lo]`, which cannot tell `[1, 2, 3, 4]` from `[3, 4, 1, 2]`. With duplicates ([LC 154: Find Minimum in Rotated Sorted Array II](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array-ii/)), `a[mid] == a[hi]` hides the side, and the worst case is O(n).

### P6. Search in Rotated Sorted Array ([LC 33](https://leetcode.com/problems/search-in-rotated-sorted-array/), Medium)

The same input as P5, with a target to find. Return the target's index, or -1. Example: `[4, 5, 6, 7, 0, 1, 2]`, target 0: the answer is 4.

??? tip "Hint 1: where to look"

    A rotated array is two sorted runs. After you know where the second run starts, how many ordinary binary searches do you need?

??? tip "Hint 2: the idea"

    Find the index of the minimum (the start of the second run), as in P5. The target can only be in one of the two runs, and each run is sorted.

??? tip "Hint 3: the plan"

    Find `pivot`, the index of the minimum. If `target >= a[0]`, search `[0, pivot - 1]`. Otherwise search `[pivot, n - 1]`. If `pivot == 0`, search the whole array. Use the closed loop on the chosen range.

??? success "Solution"

    Worked Example 5 solves this in one pass, by testing which half is sorted. This is a second solution with two passes. It has a simpler argument: each pass is a binary search that you already derived.

    ```text
    a = [4, 5, 6, 7, 0, 1, 2], target = 0
    pivot search: lo=0 hi=6 mid=3 7 > 2 -> lo = 4; lo=4 hi=6 mid=5 1 > 2 no -> hi = 5;
                  lo=4 hi=5 mid=4 0 > 1 no -> hi = 4 -> pivot = 4
    target 0 < a[0] = 4, so search [4, 6]: mid=5 value 1 > 0 -> hi = 4; mid=4 value 0 -> return 4
    ```

    ```java
    static int searchViaPivot(int[] a, int target) {
        int n = a.length, lo = 0, hi = n - 1;
        while (lo < hi) {                        // find the index of the minimum
            int mid = lo + (hi - lo) / 2;
            if (a[mid] > a[hi]) lo = mid + 1; else hi = mid;
        }
        int pivot = lo;
        if (pivot == 0) {                        // not rotated: search everything
            lo = 0;
            hi = n - 1;
        } else if (target >= a[0]) {             // the target can only be in the first run
            lo = 0;
            hi = pivot - 1;
        } else {                                 // the target can only be in the second run
            lo = pivot;
            hi = n - 1;
        }
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[mid] < target) lo = mid + 1; else hi = mid - 1;
        }
        return -1;
    }
    ```

    The cost is O(log n): two searches. The mistake that this problem invites: the strict comparison `a[lo] < a[mid]` in the one-pass version. It returns -1 for `[3, 1]` and target 1.

### P7. Find Peak Element ([LC 162](https://leetcode.com/problems/find-peak-element/), Medium)

Return the index of any peak (an element greater than both neighbors; the two ends count as having negative infinity outside). Adjacent values differ. Time O(log n). Example: `[1, 2, 1, 3, 5, 6, 4]`: index 1 and index 5 are peaks, and either is correct.

??? tip "Hint 1: where to look"

    Start at any index and keep stepping to a larger neighbor. Where can this walk end, and why can it not leave the array?

??? tip "Hint 2: the idea"

    If `a[mid] < a[mid + 1]`, walking right never ends at a valley, because the right end is a boundary of negative infinity. A peak is on the right.

??? tip "Hint 3: the plan"

    Keep `[lo, hi]` with `hi = n - 1`. If `a[mid] < a[mid + 1]`, set `lo = mid + 1`. Otherwise set `hi = mid`. Return `lo`.

??? success "Solution"

    The brute force scans for an element greater than both neighbors: O(n). The predicate view gives another code with the same behavior: `p(i) = a[i] > a[i + 1]`, with `p(n - 1)` true as the sentinel. The first true index `i` is a peak: `p(i - 1)` is false, so `a[i - 1] < a[i]`.

    ```text
    a = [1, 2, 1, 3, 5, 6, 4]
    lo=0 hi=6 mid=3  a[3]=3 > a[4]=5 ? no  -> lo = 4
    lo=4 hi=6 mid=5  a[5]=6 > a[6]=4 ? yes -> hi = 5
    lo=4 hi=5 mid=4  a[4]=5 > a[5]=6 ? no  -> lo = 5
    lo=5 hi=5        -> return 5
    ```

    ```java
    static int findPeakElement(int[] a) {
        int lo = 0, hi = a.length - 1;         // p(n - 1) is true: a[n] is negative infinity
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;      // mid + 1 <= hi, so a[mid + 1] exists
            if (a[mid] > a[mid + 1]) hi = mid; else lo = mid + 1;
        }
        return lo;
    }
    ```

    The cost is O(log n). The predicate `a[i] > a[i + 1]` is not monotone: for `[1, 3, 2, 4, 1]` it reads `F T F T T`. The loop is still correct, because its invariant is "a peak is in `[lo, hi]`", and not "the predicate is monotone". The mistake that this problem invites: `hi = n`, which makes `a[mid + 1]` read index `n`.

### P11. Split Array Largest Sum ([LC 410](https://leetcode.com/problems/split-array-largest-sum/), Hard)

The statement and the solution are in Worked Example 4. Here the question is the plan.

??? tip "Hint 1: where to look"

    What single number measures how good a split is? If you fix a limit for that number, what yes/no question can you ask about the array?

??? tip "Hint 2: the idea"

    "Can the array be split into at most `m` parts, each with sum at most `x`?" If `x` works, a larger `x` works. The answer is the smallest `x` that works.

??? tip "Hint 3: the plan"

    `lo = max(nums)`, `hi = sum(nums)`. For each `mid`, run the greedy check: add elements to the current group until the next one does not fit, then start a new group. Count the groups. If the count is at most `m`, set `hi = mid`. Otherwise `lo = mid + 1`.

??? success "Solution"

    The brute force tries every placement of `m - 1` dividers: exponential. The search on the answer takes O(n log sum). The code is `splitArray` in Worked Example 4. For `nums = [7, 2, 5, 10, 8]`, `m = 2`, the trace in that section ends at 18.

    The mistake that this problem invites: `groups = 0` at the start of the check. The check then counts one group too few, and the search returns 14 instead of 18 for the example above.

### P12. Median of Two Sorted Arrays ([LC 4](https://leetcode.com/problems/median-of-two-sorted-arrays/), Hard)

Two sorted arrays have sizes `m` and `n`. Return the median of all `m + n` values in O(log(min(m, n))). Example: `[1, 3, 8, 9, 15]` and `[7, 11, 18, 19, 21, 25]`: the merged array is `[1, 3, 7, 8, 9, 11, 15, 18, 19, 21, 25]` and the median is 11.

??? tip "Hint 1: where to look"

    The median splits all values into a left half and a right half of nearly equal size. If you choose how many elements of the first array go to the left half, how many of the second array go to the left half?

??? tip "Hint 2: the idea"

    Let `A` be the shorter array. Cut `A` at position `i` (elements `A[0..i-1]` go left). The left half has `half = (m + n + 1) / 2` elements, so `B` is cut at `j = half - i`. The cut is right when every element on the left is at most every element on the right. Because each array is sorted, only two comparisons matter: `A[i - 1] <= B[j]` and `B[j - 1] <= A[i]`.

??? tip "Hint 3: the plan"

    Search `i` in `[0, m]`. The predicate "`A[i] >= B[j - 1]`" reads `false, ..., false, true, ..., true` as `i` grows, because `A[i]` grows and `B[j - 1]` shrinks. Find the first true `i`. Read the median from the four elements around the cut.

??? success "Solution"

    The brute force merges both arrays: O(m + n). The search finds the cut directly.

    Why the first true `i` is a valid cut. Let `i` be the first `i` where `A[i] >= B[j - 1]` (treat `A[m]` as infinity and `B[-1]` as negative infinity). That is one of the two required comparisons. For the other, `i - 1` is false: `A[i - 1] < B[(half - (i - 1)) - 1] = B[j]`. So `A[i - 1] <= B[j]`. If `i = 0`, no element of `A` is on the left, and the comparison has nothing to check.

    Why `A` is the shorter array. With `m <= n`, `j = half - i` lies in `[0, n]` for every `i` in `[0, m]`. With the longer array as `A`, `j` can be negative.

    ```text
    A = [1, 3, 8, 9, 15], B = [7, 11, 18, 19, 21, 25], m = 5, n = 6, half = 6
    lo=0 hi=5 i=2 j=4  A[2]=8  >= B[3]=19 ? no  -> lo = 3
    lo=3 hi=5 i=4 j=2  A[4]=15 >= B[1]=11 ? yes -> hi = 4
    lo=3 hi=4 i=3 j=3  A[3]=9  >= B[2]=18 ? no  -> lo = 4
    lo=4 hi=4          -> i = 4, j = 2
    left side: A[0..3] = 1 3 8 9, B[0..1] = 7 11. Largest on the left = 11.
    m + n = 11 is odd, so the median is 11.
    ```

    ```java
    static double findMedianSortedArrays(int[] x, int[] y) {
        int[] a = x.length <= y.length ? x : y;      // a is the shorter array
        int[] b = x.length <= y.length ? y : x;
        int m = a.length, n = b.length;
        int half = (m + n + 1) / 2;                  // size of the left side
        int lo = 0, hi = m;                          // the cut in a: how many elements go left
        while (lo < hi) {
            int i = lo + (hi - lo) / 2;
            int j = half - i;                        // the cut in b
            if (j > 0 && i < m && a[i] < b[j - 1]) {
                lo = i + 1;                          // the cut in a is too far left
            } else {
                hi = i;
            }
        }
        int i = lo, j = half - i;
        int leftMax = Math.max(i == 0 ? Integer.MIN_VALUE : a[i - 1],
                               j == 0 ? Integer.MIN_VALUE : b[j - 1]);
        if ((m + n) % 2 == 1) return leftMax;
        int rightMin = Math.min(i == m ? Integer.MAX_VALUE : a[i],
                                j == n ? Integer.MAX_VALUE : b[j]);
        return (leftMax + (double) rightMin) / 2;    // double: the sum can pass 2^31 - 1
    }
    ```

    The cost is O(log(min(m, n))), O(1) space. The mistake that this problem invites: searching the longer array, which gives a negative `j`. A second one: forgetting the two sentinel cases at the array ends (`i = 0`, `i = m`, `j = 0`, `j = n`).

### Tell Them Apart

??? question "Pair 1. LC 410 solved by binary search on the answer, or the same statement solved by DP. Which one when?"

    Both solve the same statement.

    Binary search on the answer needs a monotone predicate: "all parts at most `x`". It runs in O(n log sum). It returns only the optimal value, and it works because the objective is "minimize the largest part".

    The DP defines `best[k][i]` as the smallest possible largest sum when the first `i` numbers form `k` parts. The recurrence is `best[k][i] = min over j of max(best[k-1][j], prefix[i] - prefix[j])`. It runs in O(m * n^2).

    ```java
    static int splitArrayDp(int[] nums, int m) {
        int n = nums.length;
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];
        long inf = Long.MAX_VALUE / 4;
        long[][] best = new long[m + 1][n + 1];      // best[k][i]: k parts for the first i numbers
        for (long[] row : best) Arrays.fill(row, inf);
        best[0][0] = 0;
        for (int k = 1; k <= m; k++) {
            for (int i = 1; i <= n; i++) {
                for (int j = 0; j < i; j++) {        // the last part is nums[j..i-1]
                    if (best[k - 1][j] < inf) {
                        best[k][i] = Math.min(best[k][i],
                                Math.max(best[k - 1][j], prefix[i] - prefix[j]));
                    }
                }
            }
        }
        return (int) best[m][n];
    }
    ```

    Binary search is better when `n` is large, because O(n^2) per part is too slow, and when the objective is a maximum or a minimum with a monotone check. DP is better when the objective changes to one with no monotone check, for example "minimize the sum of the squares of the part sums", or when the problem needs the number of ways or the actual splits. The deciding property: is there a yes/no question about one number `x` that is monotone in `x`?

??? question "Pair 2. LC 875 (Koko), against a problem where feasibility is not monotone: cut the array into parts that all have sum exactly x, and find the smallest x. Which one is a binary search?"

    Koko is a binary search. The predicate "Koko finishes at speed `s`" is monotone in `s`: the hours for each pile do not increase when `s` grows, so the total does not increase.

    The exact-sum cut is not. For `[3, 3, 3, 3, 6]` the predicate is true at 6, false at 7, true at 9. The same statement style ("find the smallest `x` such that a condition holds") hides a condition that is not monotone, and the binary search returns 9 instead of 6 (see the program in Recognition Guide). The deciding property: if `x` passes, does a larger `x` pass? For Koko, yes, by a transformation of the plan. For the exact-sum cut, no.

    For the exact-sum cut, the answer must be a prefix sum that divides the total, so the right method is to try each prefix sum that divides the total.

??? question "Pair 3. LC 33 (distinct values), against LC 81 (duplicates). Do both run in O(log n)?"

    LC 33 runs in O(log n). LC 81 does not: its worst case is O(n).

    The argument that one half is always sorted needs `a[lo] <= a[mid]` to prove that the left half is sorted. With duplicates, `a[lo] == a[mid] == a[hi]` shows nothing: the drop can be on either side. For the array of 1s with one 2, `[1, 1, 1, 1, 2, 1, 1, 1]`, and target 3, the search cannot discard half. The only safe move is `lo++` and `hi--`, which discards two elements per round.

    ```java
    public class Dup81 {
        static int steps;

        // LC 81 style search: returns true if target is in the rotated array (duplicates allowed).
        static boolean contains(int[] a, int target) {
            int lo = 0, hi = a.length - 1;
            while (lo <= hi) {
                steps++;
                int mid = lo + (hi - lo) / 2;
                if (a[mid] == target) return true;
                if (a[lo] == a[mid] && a[mid] == a[hi]) { // cannot tell which half is sorted
                    lo++;
                    hi--;
                } else if (a[lo] <= a[mid]) {
                    if (a[lo] <= target && target < a[mid]) hi = mid - 1; else lo = mid + 1;
                } else {
                    if (a[mid] < target && target <= a[hi]) lo = mid + 1; else hi = mid - 1;
                }
            }
            return false;
        }

        public static void main(String[] args) {
            for (int n : new int[] {16, 1024, 1_048_576}) {
                int[] ones = new int[n];
                java.util.Arrays.fill(ones, 1);
                ones[n / 3] = 2;                  // one 2 among the 1s: a rotation of [1, ..., 1, 2]
                steps = 0;
                boolean found = contains(ones, 3);
                int[] distinct = new int[n];
                for (int i = 0; i < n; i++) distinct[i] = i;
                int distinctSteps = 0;
                int lo = 0, hi = n - 1;
                while (lo <= hi) {
                    distinctSteps++;
                    int mid = lo + (hi - lo) / 2;
                    if (distinct[mid] < n) lo = mid + 1; else hi = mid - 1;
                }
                System.out.println("n = " + n + ", found = " + found + ", rounds with duplicates = "
                        + steps + ", rounds with distinct values = " + distinctSteps);
            }
        }
    }
    ```

    <!-- expected-output -->
    ```text
    n = 16, found = false, rounds with duplicates = 8, rounds with distinct values = 5
    n = 1024, found = false, rounds with duplicates = 349, rounds with distinct values = 11
    n = 1048576, found = false, rounds with duplicates = 349543, rounds with distinct values = 21
    ```

    The rounds with duplicates grow in proportion to `n`: 8 for 16 elements, 349 for 1,024, 349,543 for 1,048,576. The rounds without duplicates grow as log2(n): 5, 11, 21. The deciding property: do equal values at `lo`, `mid`, and `hi` hide the sorted half?

??? question "Pair 4. LC 74 (binary search on a matrix), against LC 240. Which one is a binary search?"

    LC 74 is. Each row starts after the previous row ends, so the matrix is one sorted array of `m * n` numbers, and the binary search on the flattened index runs in O(log(m * n)).

    LC 240 sorts rows and columns separately. Row `i` can hold a value larger than the first value of row `i + 1`. No single order exists, so a middle cell does not discard half of the matrix. The staircase walk from the top-right cell discards one row or one column per step: O(m + n). The deciding property: does each row continue the previous row?

### More problems, by group

The groups below keep every problem of the previous version of this page.

**Easy: classic binary search**

| Problem | Difficulty | Key insight | LC # |
|---|---|---|---|
| [Binary Search](https://leetcode.com/problems/binary-search/) | Easy | Closed `[lo, hi]` loop with exact-match return | 704 |
| [Search Insert Position](https://leetcode.com/problems/search-insert-position/) | Easy | Return the lower bound | 35 |
| [First Bad Version](https://leetcode.com/problems/first-bad-version/) | Easy | First true in `F F F T T T` over versions | 278 |
| [Guess Number Higher or Lower](https://leetcode.com/problems/guess-number-higher-or-lower/) | Easy | Closed `[lo, hi]` loop, the API gives the direction | 374 |

**Medium: classic binary search.** These problems search for a position in an array, not for a value in an answer range.

| Problem | Difficulty | Key insight | LC # |
|---|---|---|---|
| [Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) | Medium | Lower bound and upper bound minus 1 | 34 |
| [Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) | Medium | Find the sorted half, range-check the target | 33 |
| [Search in Rotated Sorted Array II](https://leetcode.com/problems/search-in-rotated-sorted-array-ii/) | Medium | Same as 33. On `a[lo] == a[mid] == a[hi]`, shrink both ends (worst case O(n)) | 81 |
| [Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) | Medium | Compare `a[mid]` with `a[hi]` | 153 |
| [Find Peak Element](https://leetcode.com/problems/find-peak-element/) | Medium | Keep the side where `a[mid] < a[mid + 1]` points | 162 |
| [Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/) | Medium | One sorted array of length `m * n`, index to `[i / n][i % n]` | 74 |
| [Search a 2D Matrix II](https://leetcode.com/problems/search-a-2d-matrix-ii/) | Medium | Not binary search: staircase from the top-right, O(m + n) | 240 |

**Medium: binary search on the answer**

| Problem | Difficulty | Key insight | LC # |
|---|---|---|---|
| [Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) | Medium | Binary search on speed, verify hours | 875 |
| [Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) | Medium | Binary search on capacity | 1011 |
| [Find the Smallest Divisor Given a Threshold](https://leetcode.com/problems/find-the-smallest-divisor-given-a-threshold/) | Medium | Binary search on divisor | 1283 |
| [Magnetic Force Between Two Balls](https://leetcode.com/problems/magnetic-force-between-two-balls/) | Medium | Maximize the minimum distance | 1552 |
| [Minimum Limit of Balls in a Bag](https://leetcode.com/problems/minimum-limit-of-balls-in-a-bag/) | Medium | Binary search on the maximum bag size | 1760 |
| [Kth Smallest Element in a Sorted Matrix](https://leetcode.com/problems/kth-smallest-element-in-a-sorted-matrix/) | Medium | Binary search on the value, count elements `<=` mid | 378 |
| [Find K Closest Elements](https://leetcode.com/problems/find-k-closest-elements/) | Medium | Binary search on the window start | 658 |
| [Minimized Maximum of Products Distributed to Any Store](https://leetcode.com/problems/minimized-maximum-of-products-distributed-to-any-store/) | Medium | Binary search on the maximum per store, count the stores needed | 2064 |

**Hard (differentiators)**

| Problem | Difficulty | Key insight | LC # |
|---|---|---|---|
| [Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/) | Hard | Binary search on the partition | 4 |
| [Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) | Hard | Binary search on the maximum sum | 410 |
| [Find Minimum in Rotated Sorted Array II](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array-ii/) | Hard | 153 with duplicates. On `a[mid] == a[hi]`, do `hi -= 1` (worst case O(n)) | 154 |
| [Kth Smallest Number in Multiplication Table](https://leetcode.com/problems/kth-smallest-number-in-multiplication-table/) | Hard | Binary search on the value, count per row | 668 |
| [Find K-th Smallest Pair Distance](https://leetcode.com/problems/find-k-th-smallest-pair-distance/) | Hard | Binary search on the distance, count the pairs | 719 |
| [Maximum Running Time of N Computers](https://leetcode.com/problems/maximum-running-time-of-n-computers/) | Hard | Binary search on the running time | 2141 |
| [Minimize Max Distance to Gas Station](https://leetcode.com/problems/minimize-max-distance-to-gas-station/) (premium) | Hard | Binary search on a real-valued distance. Stop after a fixed number of iterations | 774 |

## Interview Follow-ups

??? question "Why do you write `mid = lo + (hi - lo) / 2` and not `(lo + hi) / 2`?"

    Both give the same value for small indices. But in Java, `lo + hi` is an `int` addition, and the result wraps to a negative number when it passes 2,147,483,647. For `lo = 1,500,000,000` and `hi = 2,000,000,000`, `(lo + hi) / 2` is -397,483,648. `hi - lo` fits in an `int` when `0 <= lo <= hi`, so `lo + (hi - lo) / 2` is safe. The JDK's own code uses `(low + high) >>> 1`, which is also safe for non-negative indices. This matters most in a search on the answer, where `hi` can be 10^9 or larger.

??? question "What is the invariant of your loop, and how does it justify the return value?"

    For the exact-match loop, the invariant is: if the target is in the array, its index is in `[lo, hi]`. Each update removes only positions that cannot hold the target, so the invariant holds. The loop ends when the space is empty (`lo > hi`), and then the target is absent. For the first-true loop, the invariant is: the first true index is in `[lo, hi]`. The loop ends when `lo == hi`, so `lo` is the first true index.

??? question "How do you find the first and the last occurrence of a value in a sorted array with duplicates?"

    Run two first-true searches. The first occurrence is the first index with `a[i] >= target` (lower bound). The position after the last occurrence is the first index with `a[i] > target` (upper bound). The target is present if the two results differ, and then the answer is `[lower, upper - 1]`. Both searches run in O(log n), whatever the number of duplicates. A single search that finds one match and then scans outward costs O(n) when all elements are equal.

??? question "What do `Arrays.binarySearch` and `Collections.binarySearch` return, and what can you not rely on?"

    If the key is found, they return an index of the key. If not, they return `-(insertion point) - 1`, where the insertion point is the index of the first element greater than the key (or the length if there is none). So a negative result means "absent", and `-result - 1` gives the insertion point. When the input contains several equal elements, the javadoc gives no guarantee about which index is returned, so you must not use the result as a first or last position. For that, write a lower bound loop.

??? question "How do you recognize a binary search on the answer when the problem gives no sorted array?"

    Look for three things. The answer is a number in a known range (a speed, a capacity, a distance). A check "does answer `x` work?" runs in O(n) or O(n log n). And the check is monotone: if `x` works, every larger `x` works (for a minimization), or every smaller `x` works (for a maximization). The phrases "minimize the maximum" and "maximize the minimum" are the usual signs. I argue the monotonicity by transforming a solution for `x` into a solution for the next value, for example "the same plan stays valid with a larger capacity".

??? question "How do you choose `lo` and `hi` for the answer?"

    `lo` is the smallest value that can be a valid answer, and `hi` is a value that is always valid, so the predicate is true at `hi`. For ship capacity, `lo = max(weights)` (the ship must carry the heaviest package) and `hi = sum(weights)` (one day carries all). For Koko, `lo = 1` and `hi = max(piles)`. I give each bound a reason, because a wrong bound either excludes the answer or makes the check divide by zero.

??? question "What is the time complexity of Koko Eating Bananas, and why?"

    O(n log M), with `n` piles and `M = max(piles)`. The binary search runs about log2(M) rounds, and each round runs the feasibility check, which reads all `n` piles. In general, the cost of a search on the answer is the number of halvings of the answer range, times the cost of one check.

??? question "Why does the rotated-array search become O(n) with duplicates?"

    The search relies on `a[lo] <= a[mid]` to know that the left half is sorted. With duplicates, `a[lo] == a[mid] == a[hi]` can hold while the drop is on either side, as in `[1, 1, 1, 2, 1, 1, 1]`. The search can only discard `a[lo]` and `a[hi]`, one element at each end. In the worst case, an array of equal values with one different value, it makes a number of rounds proportional to `n`: each round discards at most two elements, so the rounds can reach `n / 2`. The program in Tell Them Apart, Pair 3, measures about `n / 3` rounds for `n = 1,048,576`.

??? question "How many iterations do you use for a binary search on real numbers, and why not an epsilon?"

    A fixed number such as 100. Each iteration halves `hi - lo`, and a `double` has 53 bits of precision. After about 52 halvings, `lo` and `hi` are neighboring doubles and `mid` equals one of them. A loop `while (hi - lo > epsilon)` with an `epsilon` below the gap between neighboring doubles at that magnitude never ends. A fixed count always ends.

??? question "The same statement can be solved by binary search on the answer and by DP. How do you choose?"

    I ask whether a monotone yes/no question exists. For LC 410, "all parts at most `x`" is monotone, so binary search gives O(n log sum). DP takes O(m * n^2), and I choose it when the objective has no monotone check (for example, minimize the sum of squares of the parts), or when the problem needs the number of ways or the split itself.

??? question "Your binary search on the answer returns a wrong answer on some inputs. What do you check first?"

    I check the monotonicity of the predicate, not the loop. I evaluate the predicate on every candidate for a small input, and look for a true that has a false after it (in a minimization). If the row is not `F...F T...T`, no loop can be correct. Then I check the bounds: whether the true answer lies inside `[lo, hi]`, and whether `hi` is a true sentinel.

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Binary search on the answer for "minimize the max" | DP ([Dynamic Programming](../15-dynamic-programming/README.md)) | Can you write a fast `feasible(X)` check, and is it monotonic in X? If yes, binary search ([LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/)). If the check needs the full search itself, DP. |
| Greedy for "minimum speed / capacity" | Binary search on the answer, with the greedy as the check ([Greedy](../14-greedy/README.md)) | Is the answer one number in a large range, with a yes/no check per value? If yes, binary search wraps the greedy check ([LC 875: Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/), [LC 1011: Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/)). |
| Heap for "k-th smallest" | Binary search on the value, plus a count | Is the set implicit or too large to store? If yes, binary search on the value ([LC 668: Kth Smallest Number in Multiplication Table](https://leetcode.com/problems/kth-smallest-number-in-multiplication-table/)). If you can store it and `k` is small, a heap ([LC 215: Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/)). |
| Binary search on a sorted 2D matrix | Staircase walk from the top-right corner | Does each row start after the previous row ends? If yes, binary search on the flattened index ([LC 74: Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/)). If only rows and columns are sorted, the staircase, O(m + n) ([LC 240: Search a 2D Matrix II](https://leetcode.com/problems/search-a-2d-matrix-ii/)). |
| Sliding window for "longest segment with at most k changes" | Sliding window, not binary search | Can the window grow and shrink with a monotonic condition? If yes, sliding window in O(n) ([LC 1004: Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/)). Binary search on the length also works, but costs O(n log n). |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

## Quick Reference Card

```text
WHEN TO USE
  - A sorted (or sorted-then-rotated) array and a question about a position.
  - "Minimize the max" / "maximize the min" / "smallest x such that ..." over a number range.
  - A monotone predicate: F..F T..T (first true) or T..T F..F (last true).

THE INVARIANT
  - The answer is always inside [lo, hi]. Each update keeps it, and each update shrinks it.

TEMPLATES (derive each line; do not memorize)
  Exact match   lo=0, hi=n-1, while lo <= hi, lo = mid+1 / hi = mid-1, return -1
  First true    lo=0, hi=n,   while lo < hi,  true: hi = mid, false: lo = mid+1, return lo
  Last true     while lo < hi, mid = hi - (hi-lo)/2, true: lo = mid, false: hi = mid-1
  Real numbers  for 100 iterations: true: hi = mid, false: lo = mid

PREDICATES
  find target   a[i] >= target, then check a[i] == target
  lower bound   a[i] >= target       upper bound   a[i] > target
  LC 34         [lowerBound, upperBound - 1]
  rotated min   a[i] <= a[n-1]       peak          a[i] > a[i+1]

SEARCH ON THE ANSWER
  1. Name the range of answers; give a reason for lo and for hi (hi is a true sentinel).
  2. Write feasible(x) in O(n); argue it is monotone in x.
  3. Minimize: first true. Maximize: last true, with the upper middle.
  4. Cost = log2(range) * cost of one check. Use long for sums.

COMPLEXITY
  Index search O(log n). Search on the answer O(n log range). Rotated with duplicates O(n).

TOP MISTAKES
  - hi = n - 1 when the answer can be n.         - lo = mid with the lower middle (never ends).
  - hi = mid - 1 when mid can be the answer.     - (lo + hi) / 2 overflow.
  - groups = 0 in the check; int sum of hours.    - a predicate that is not monotone.
```

## Mastery Checklist

The levels are from the [Mastery Tracker](../../mastery-tracker.md). Test each statement with the page closed.

- [ ] L2 Understand: I trace the exact-match loop and the first-true loop by hand on a new sorted array of 6 elements, and on an array of length 0, 1, and 2, and I get the right `lo`, `hi`, and `mid` at each step.
- [ ] L3 Explain: I explain in under 3 minutes why `lo = mid` with the lower middle never ends and why `hi = mid - 1` can lose the answer, and I give a failing input for each.
- [ ] L3 Explain: I state the invariant of the exact-match loop and of the first-true loop, and I derive the start values, the loop condition, the two updates, and the returned value from each.
- [ ] L3 Explain: I give a problem that looks like "minimize the maximum" but has a predicate that is not monotone, and I show the wrong answer of the binary search.
- [ ] L4 Solve a familiar problem: I write lower bound, upper bound, and [LC 34: Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) from an empty file, as first-true searches.
- [ ] L4 Solve a familiar problem: I write [LC 875: Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) and [LC 1011: Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) from an empty file, with a reason for each bound and for the monotonicity, and I state the complexity.
- [ ] L4 Solve a familiar problem: I write [LC 33: Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/), [LC 153: Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/), and [LC 162: Find Peak Element](https://leetcode.com/problems/find-peak-element/), and I state the fact that discards half of the range in each.
- [ ] L4 Solve a familiar problem: I explain why the greedy feasibility check of [LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) gives the minimum number of groups.
- [ ] L5 Solve a new problem: for a problem that I have not seen, such as [LC 1482: Minimum Number of Days to Make m Bouquets](https://leetcode.com/problems/minimum-number-of-days-to-make-m-bouquets/) or [LC 2187: Minimum Time to Complete Trips](https://leetcode.com/problems/minimum-time-to-complete-trips/), I find the range with a reason for each bound, the check, and the direction (minimize or maximize), without a hint.
- [ ] L6 Solve under time: I solve an unseen Medium search-on-the-answer problem in 25 minutes, and a Hard one such as [LC 410: Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) in 40 minutes, speaking aloud, with the edge cases (empty input, one element, the answer at the first or last position).

## Next Topics

- [Pattern 14: Greedy](../14-greedy/README.md): the feasibility checks on this page are greedy rules. That page shows when a greedy rule is correct and how to prove it.
- [Pattern 21: Segment Tree and Fenwick Tree](../21-segment-tree-and-fenwick-tree/README.md): the same search, run as a descent on a tree to find the first prefix that meets a condition.
- [Pattern 07: Monotonic Stack and Queue](../07-monotonic-stack-queue/README.md): the next pattern in the series.
- [Roadmap](../../roadmap.md): the order of the patterns and where this page sits in it.
- [Mastery tracker](../../mastery-tracker.md): record your level for this pattern.
