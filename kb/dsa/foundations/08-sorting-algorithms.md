---
priority: P1
---
# Sorting Algorithms

## Why It Matters

Sorting is the most common preparation step in coding rounds. Many problems become easy after one sort: pairs that add up to a target (two pointers), merged intervals, the k-th largest element, and greedy choices. Interviewers also ask the theory behind it directly: "Why is quicksort O(n²) in the worst case?", "Which sort is stable and why does it matter?", "What does `Arrays.sort` use?", "Why is `(a, b) -> a - b` a bug?". On the job, the stable object sort of the JDK sorts every `List<T>` that you order by a key, and the comparator bugs on this page cause wrong orders and `IllegalArgumentException` in production code. This page covers nine sorting algorithms in Java, the lower bound for comparison sorts, the JDK sorting methods, and quickselect.

## Core Concepts

**Sorting**: rearranging the elements of an array so that a given order holds between neighbors. This page sorts into ascending order unless it says otherwise.

**Comparison sort**: a sort that learns about the order of the input only by comparing two elements ("is `a[i]` smaller than `a[j]`?"). Insertion, selection, bubble, merge, quick, and heap sort are comparison sorts. **Counting, radix, and bucket sort** are not comparison sorts: they read the key itself (its value or its digits) and use it as an array index.

**Stable sort**: a sort that keeps equal elements in their input order. If the records `(a, 2)` and `(b, 2)` have equal keys and `a` comes first in the input, a stable sort puts `a` first in the output. Stability matters when you sort by one key after another. You sort by name, then stably by age, and people of the same age stay in name order.

**In-place sort**: a sort that uses O(1) extra memory besides the input array. This page also calls a sort in place if it needs O(log n) extra memory for the recursion stack, and it says so in the table.

**Adaptive sort**: a sort that runs faster on input that is already nearly sorted. Insertion sort and TimSort are adaptive.

**Inversion**: a pair of positions `i < j` with `a[i] > a[j]`. A sorted array has 0 inversions. A reversed array of `n` elements has n(n − 1)/2. Insertion sort does work in proportion to the number of inversions.

**Partition**: rearranging `a[lo..hi]` around a pivot value, so that the elements less than the pivot come before the elements that are not. Quicksort and quickselect use it.

**Run**: a maximal ascending (or descending) block of the input. TimSort finds runs and merges them.

**External sort**: sorting data that does not fit in memory. The usual method sorts chunks in memory, writes each sorted chunk to disk, and merges the chunks with a k-way merge (see [heap pattern](../16-heap-priority-queue/README.md)).

### Comparison of all nine sorts

`n` is the number of elements. `k` is the size of the key range (`max - min + 1`) for counting sort, and the number of buckets for bucket sort. `d` is the number of digits and `b` is the base for radix sort.

| Algorithm | Best | Average | Worst | Extra space | Stable | Comparison sort |
|---|---|---|---|---|---|---|
| Insertion | O(n) | O(n²) | O(n²) | O(1) | Yes | Yes |
| Selection | O(n²) | O(n²) | O(n²) | O(1) | No | Yes |
| Bubble (with the early-exit flag) | O(n) | O(n²) | O(n²) | O(1) | Yes | Yes |
| Merge (array) | O(n log n) | O(n log n) | O(n log n) | O(n) | Yes | Yes |
| Quick | O(n log n) | O(n log n) | O(n²) | O(log n) stack, with the smaller-part-first rule | No | Yes |
| Heap | O(n log n) | O(n log n) | O(n log n) | O(1) | No | Yes |
| Counting | O(n + k) | O(n + k) | O(n + k) | O(n + k) | Yes (backward fill) | No |
| Radix (LSD) | O(d · (n + b)) | O(d · (n + b)) | O(d · (n + b)) | O(n + b) | Yes | No |
| Bucket | O(n + k) | O(n + k) for evenly spread input | All elements in one bucket: O(n²) with insertion sort inside the buckets (as in CLRS), O(n log n) with a comparison sort such as `Collections.sort` | O(n + k) | Yes, if the bucket sort is stable | No |

Notes on the table:

- Quicksort's best case O(n log n) is for the two-way partitions on this page. A three-way partition makes the best case O(n) when all keys are equal.
- Quicksort's stack depth is guaranteed O(log n) if the code recurses into the smaller part first and loops on the larger part. A plain two-call quicksort with a random pivot has O(log n) expected depth and O(n) depth in the worst case.
- Merge sort's best case is O(n log n) for the plain algorithm. The implementation on this page skips the merge when the two halves are already in order, so it takes O(n) on sorted input.
- Selection sort makes at most n − 1 swaps. That is an advantage only when a write is much more costly than a read.
- Insertion sort does n − 1 comparisons on sorted input and about n + I operations in general, where I is the number of inversions.

### Which patterns use this

- [Two pointers](../02-two-pointers/README.md): sort the array, then move two pointers toward each other (pair sum, 3Sum).
- [Binary search](../06-binary-search-on-answer/README.md): binary search needs sorted input, so a sort of O(n log n) often comes first.
- [Intervals](../20-intervals/README.md): sort by start time, then merge or scan.
- [Greedy](../14-greedy/README.md): many greedy solutions sort by a key first, and the proof depends on that order.
- [Heap and priority queue](../16-heap-priority-queue/README.md): heap sort, top-k, the k-way merge, and quickselect as the alternative to a heap for the k-th element.
- [Divide and conquer](../09-divide-and-conquer/README.md): merge sort is the standard example. Counting inversions and "count of smaller numbers after self" extend its merge step.
- [Hashing and frequency](../05-hashing-frequency/README.md): counting sort and bucket sort replace a sort by frequency (LeetCode 347, 451).
- `kb/dsa/21-segment-tree-and-fenwick-tree/README.md`: a second way to count inversions, with a Fenwick tree.

Related foundations: [complexity analysis](01-complexity-analysis.md) gives the master theorem used in the merge sort and quicksort recurrences. [Heaps](06-heaps.md) gives the heap that heap sort uses.

## How It Works

### Insertion sort

Insertion sort keeps a sorted prefix `a[0..i-1]`. For each `i` it takes `a[i]` as the key and shifts the larger elements of the prefix one slot to the right. Then it writes the key into the gap. Cards in a hand are sorted the same way.

```java
class ElementarySorts {
    // Insertion sort: grow a sorted prefix. Each new element moves left past larger elements.
    // Stable, because the shift loop stops at an element that is <= the key.
    static void insertionSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j]; // shift one slot right
                j--;
            }
            a[j + 1] = key;
        }
    }

    // Selection sort: find the minimum of the unsorted suffix and swap it into place.
    // Unstable, because the swap can jump an element over an equal one.
    static void selectionSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[min]) min = j;
            }
            int tmp = a[i]; a[i] = a[min]; a[min] = tmp; // at most n - 1 swaps in total
        }
    }

    // Bubble sort: swap adjacent out-of-order pairs. After pass k, the k largest are in place.
    // The swapped flag ends the sort after a pass with no swap, so sorted input costs O(n).
    static void bubbleSort(int[] a) {
        for (int pass = 0; pass < a.length - 1; pass++) {
            boolean swapped = false;
            for (int j = 0; j < a.length - 1 - pass; j++) {
                if (a[j] > a[j + 1]) {
                    int tmp = a[j]; a[j] = a[j + 1]; a[j + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) return;
        }
    }
}
```

- **Time.** The inner loop runs once for each inversion that `a[i]` takes part in. The total is n − 1 comparisons plus I shifts, where I is the number of inversions. Sorted input has I = 0, so the time is O(n). Reversed input has I = n(n − 1)/2, so the time is O(n²). Random input has I = n(n − 1)/4 on average, so the average time is O(n²).
- **Space.** O(1).
- **Stable.** Yes. The shift loop stops at the first element that is less than or equal to the key (`a[j] > key` is false), so the key never passes an equal element.
- **When to use it.** For small arrays (approximate: fewer than about 30 to 50 elements; the JDK thresholds below are 32 and 44) and for nearly sorted input. The JDK uses insertion sort for small ranges, inside its faster sorts (see "Sorting in the JDK").

### Selection sort

Selection sort finds the minimum of the unsorted suffix `a[i..n-1]` and swaps it to index `i`. The code is in the block above.

- **Time.** O(n²) in all cases. The scan for the minimum makes n − 1 − i comparisons for each `i`, whatever the order of the input.
- **Space.** O(1). It makes at most n − 1 swaps.
- **Unstable.** The swap moves `a[i]` to a far position, and it can jump over an element that equals it. Example: `[(a,2), (b,2), (c,1)]`. The first pass swaps `(a,2)` with `(c,1)`, and the result `[(c,1), (b,2), (a,2)]` has `b` before `a`. The program in the "Java Example" section prints this.

### Bubble sort

Bubble sort scans the array and swaps each adjacent pair that is out of order. After pass `p`, the `p` largest elements are at the end, so the next pass can stop earlier. If a whole pass makes no swap, the array is sorted and the code returns. The code is in the block above.

- **Time.** O(n²) on average and in the worst case. O(n) on sorted input, only because of the `swapped` flag. Without the flag, the best case is O(n²).
- **Space.** O(1). **Stable**: it swaps only strictly out-of-order neighbors.
- **Interview note.** Bubble sort is slower than insertion sort in practice. Both do work for each inversion, but bubble sort removes an inversion with a swap (three writes) and insertion sort with a shift (one write). Bubble sort also keeps comparing elements that are already in place. Do not offer it as your answer, except to show that you know its flag.

### Merge sort

Merge sort splits the array into two halves, sorts each half recursively, and merges the two sorted halves into one sorted array. The merge walks both halves with one index each and always takes the smaller front element.

```java
class MergeSort {
    static void sort(int[] a) {
        int[] aux = new int[a.length]; // one buffer, allocated once: O(n) extra space
        sort(a, aux, 0, a.length - 1);
    }

    private static void sort(int[] a, int[] aux, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;  // not (lo + hi) / 2: that can overflow for huge arrays
        sort(a, aux, lo, mid);
        sort(a, aux, mid + 1, hi);
        if (a[mid] <= a[mid + 1]) return; // already ordered: skip the merge
        merge(a, aux, lo, mid, hi);
    }

    // Merge the sorted ranges a[lo..mid] and a[mid+1..hi].
    private static void merge(int[] a, int[] aux, int lo, int mid, int hi) {
        System.arraycopy(a, lo, aux, lo, hi - lo + 1);
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = aux[j++];
            else if (j > hi) a[k] = aux[i++];
            else if (aux[j] < aux[i]) a[k] = aux[j++]; // strict <: on a tie, take the left one
            else a[k] = aux[i++];                      // so equal elements keep their order
        }
    }
}
```

- **Recurrence.** T(n) = 2·T(n/2) + Θ(n). Two subproblems of half the size, and a linear merge. The master theorem (case 2, see the [complexity page](01-complexity-analysis.md)) gives Θ(n log n). The recursion tree has log₂ n levels, and each level does Θ(n) work in total.
- **Time.** O(n log n) in all cases. The check `a[mid] <= a[mid + 1]` makes sorted input O(n) in this implementation, but the guarantee for other input stays O(n log n).
- **Space.** O(n) for the buffer `aux`, plus O(log n) for the recursion stack. The buffer is allocated once. A buffer allocated in each merge call gives the same bound but more garbage.
- **Stable.** Yes. In `merge`, `aux[j] < aux[i]` is strict. On a tie, the left element wins, and the left element came first in the input.
- **Mid index.** `lo + (hi - lo) / 2` does not overflow. `(lo + hi) / 2` overflows when `lo + hi` exceeds `Integer.MAX_VALUE`. Joshua Bloch described this bug in the JDK binary search in 2006.
- **Linked lists.** Merge sort suits linked lists. The merge relinks nodes and needs no buffer, and it needs no random access. LeetCode 148 ("Sort List") asks for it. A top-down version uses O(log n) stack, and a bottom-up version uses O(1) extra space.
- **Counting inversions.** While merging, each time the code takes an element from the right half, it jumps over all the elements that are still in the left half. Each of those pairs is an inversion. Adding the count of those elements gives the total number of inversions in O(n log n). LeetCode 315 and 493 use this idea.

### Quicksort

Quicksort picks a **pivot**, partitions the array so that the elements smaller than the pivot come first, and sorts the two parts recursively. Unlike merge sort, the work is in the partition (before the recursive calls), and the combine step is empty.

The next block has the two classic partition schemes, a random pivot with bounded stack depth, and a three-way partition.

```java
import java.util.concurrent.ThreadLocalRandom;

class QuickSorts {
    private static void swap(int[] a, int i, int j) {
        int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
    }

    // ---- Lomuto partition: pivot = last element, one scan pointer ----
    // Invariant: a[lo..i] < pivot (i is the last "small" slot), a[i+1..j-1] >= pivot.
    static int lomuto(int[] a, int lo, int hi) {
        int pivot = a[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (a[j] < pivot) swap(a, ++i, j);
        }
        swap(a, i + 1, hi); // put the pivot between the two parts
        return i + 1;       // the pivot is now at its final index
    }

    static void lomutoSort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = lomuto(a, lo, hi);
        lomutoSort(a, lo, p - 1);
        lomutoSort(a, p + 1, hi);
    }

    // ---- Hoare partition: two pointers move toward each other ----
    // Returns j such that every element in a[lo..j] <= every element in a[j+1..hi].
    // The pivot is the middle element. The pivot a[hi] would loop forever in this form.
    static int hoare(int[] a, int lo, int hi) {
        int pivot = a[lo + (hi - lo) / 2];
        int i = lo - 1, j = hi + 1;
        while (true) {
            do { i++; } while (a[i] < pivot);
            do { j--; } while (a[j] > pivot);
            if (i >= j) return j;
            swap(a, i, j);
        }
    }

    static void hoareSort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = hoare(a, lo, hi);
        hoareSort(a, lo, p);      // the pivot is NOT fixed in place: the left part includes p
        hoareSort(a, p + 1, hi);
    }

    // ---- Random pivot: no fixed input is bad ----
    static void randomizedSort(int[] a, int lo, int hi) {
        while (lo < hi) {
            int r = ThreadLocalRandom.current().nextInt(lo, hi + 1);
            swap(a, r, hi);              // move the random pivot to the end, then use Lomuto
            int p = lomuto(a, lo, hi);
            // Recurse into the smaller part and loop on the larger part.
            // The stack depth is then at most log2(n), even if the splits are bad.
            if (p - lo < hi - p) {
                randomizedSort(a, lo, p - 1);
                lo = p + 1;
            } else {
                randomizedSort(a, p + 1, hi);
                hi = p - 1;
            }
        }
    }

    // ---- Three-way partition (Dijkstra's Dutch national flag) ----
    // After the loop: a[lo..lt-1] < pivot, a[lt..gt] == pivot, a[gt+1..hi] > pivot.
    static void threeWaySort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int pivot = a[ThreadLocalRandom.current().nextInt(lo, hi + 1)];
        int lt = lo, i = lo, gt = hi;
        while (i <= gt) {
            if (a[i] < pivot) swap(a, lt++, i++);
            else if (a[i] > pivot) swap(a, i, gt--); // no i++: a[i] is unseen
            else i++;
        }
        threeWaySort(a, lo, lt - 1);   // the equal block is already in place
        threeWaySort(a, gt + 1, hi);
    }
}
```

#### Lomuto partition

Lomuto uses one scan pointer `j` and a boundary `i`. The pivot is the last element. Elements smaller than the pivot go to the front section. At the end, the pivot swaps into the slot after the front section, and that index is its final position. Lomuto is short and easy to get right, and it is the form that most interview answers use. It does more swaps than Hoare, and it handles equal keys badly: an element equal to the pivot goes to the right part, so an array of equal keys splits as 0 and n − 1 at every level and costs O(n²).

#### Hoare partition

Hoare uses two pointers that start at the two ends and move toward each other. The left pointer stops at an element that is not less than the pivot. The right pointer stops at an element that is not greater than the pivot. If the pointers have not crossed, the code swaps the two elements. The function returns the crossing index `j`. Two details matter:

- **The pivot does not end at its final position.** The recursive calls are on `[lo, j]` and `[j + 1, hi]`. If the code uses `[lo, j - 1]` and `[j + 1, hi]`, it loses an element.
- **The pivot must not be `a[hi]` in this form.** With the last element as the pivot, a call can return `j == hi`, and the recursion on `[lo, j]` then repeats the same range forever. The middle element avoids this.

Hoare stops both pointers at elements equal to the pivot, and swaps them. Equal keys therefore split the array in the middle, and an array of equal keys costs O(n log n) and not O(n²).

#### Why the worst case is O(n²)

If every partition puts the pivot at one end, the two parts have sizes 0 and n − 1. The recurrence is T(n) = T(n − 1) + Θ(n), which gives Θ(n²). With the last element as the pivot (Lomuto), sorted input, reversed input, and all-equal input all trigger it. The recursion depth is then n, so a plain recursive quicksort can also throw `StackOverflowError`.

#### Why the average case is O(n log n)

A split does not need to be balanced. Any split with a constant ratio keeps the depth at O(log n). For a 9-to-1 split, T(n) = T(9n/10) + T(n/10) + Θ(n). The recursion tree has depth log₁₀⁄₉ n = O(log n), and each level costs at most Θ(n), so the total is O(n log n) (CLRS section 7.2). With a random pivot, half of the choices fall in the middle half of the sorted order, and each such choice gives a split no worse than 3-to-1. For a random pivot (or a randomly shuffled input) and distinct keys, the expected number of comparisons is about 2n ln n, which is about 1.39 n log₂ n. CLRS section 7.4.2 proves the O(n log n) bound on the expected comparisons. Sedgewick and Wayne (section 2.3, and their [COS 226 quicksort lecture](https://www.cs.princeton.edu/courses/archive/spring19/cos226/lectures/23Quicksort.pdf)) give the constant: about 2N ln N compares on average for N distinct keys.

#### Pivot choices

| Pivot | Behavior |
|---|---|
| First or last element | Worst case on sorted or reversed input, which is common in real data |
| Middle element | Good on sorted input. Some inputs still force O(n²) |
| Random element | Expected O(n log n) for every fixed input. No input is bad in advance (CLRS section 7.3). The running time depends on the random choices |
| Median of three (first, middle, last) | A heuristic that avoids the sorted-input case. Adversarial inputs still exist |
| Median of medians | Guaranteed O(n) pivot selection and O(n log n) sort. The constant is large, so libraries rarely use it |
| Introsort | Quicksort that switches to heap sort when the recursion depth passes a limit. It gives an O(n log n) worst case. The JDK's primitive sort does this since JDK 14 (see below) |

#### Random pivot and bounded stack depth

`randomizedSort` swaps a random element to the end and then uses the Lomuto partition. It recurses into the smaller part and loops on the larger part. The smaller part has at most half of the elements, so the recursion depth is at most log₂ n, even if the splits are bad. The time is still O(n²) in the worst case, but no fixed input forces that case when the pivot is random.

#### Three-way partition

The three-way partition (the Dutch national flag problem, by Dijkstra) splits `a[lo..hi]` into three zones: less than, equal to, and greater than the pivot. The loop keeps three boundaries, `lt`, `i`, and `gt`. If `a[i] < pivot`, it swaps `a[i]` into the "less" zone and advances both `lt` and `i`. If `a[i] > pivot`, it swaps `a[i]` with `a[gt]` and decrements `gt`, but it does **not** advance `i`, because the element that came from `gt` is not yet examined. If `a[i] == pivot`, it advances `i`. The recursion skips the whole equal zone. An array with `m` distinct keys sorts in O(n · log m) expected time, and all-equal input costs O(n). LeetCode 75 ("Sort Colors") is the three-way partition with the pivot value 1.

#### Quicksort is unstable

A partition swaps elements that are far apart, so it can move an element past an equal one. A stable partition needs O(n) extra memory, which removes the main advantage of quicksort.

### Heap sort

Heap sort builds a max-heap in the array, then repeats: it swaps the root (the maximum) with the last element of the heap, shrinks the heap by one, and sifts the new root down. The sorted part grows from the right end. The heap itself is explained in [heaps](06-heaps.md).

```java
class HeapSort {
    static void sort(int[] a) {
        int n = a.length;
        // Build a max-heap bottom-up: sift down every non-leaf, from the last one to the root.
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);
        // Move the maximum to the end, shrink the heap by one, restore the heap.
        for (int end = n - 1; end > 0; end--) {
            int tmp = a[0]; a[0] = a[end]; a[end] = tmp;
            siftDown(a, 0, end);
        }
    }

    // Sift a[i] down inside the heap a[0..size-1]. Children of i are 2i+1 and 2i+2.
    private static void siftDown(int[] a, int i, int size) {
        int value = a[i];
        while (true) {
            int child = 2 * i + 1;
            if (child >= size) break;
            if (child + 1 < size && a[child + 1] > a[child]) child++; // pick the larger child
            if (a[child] <= value) break;
            a[i] = a[child];
            i = child;
        }
        a[i] = value;
    }
}
```

- **Time.** Building the heap bottom-up costs O(n) (the sum of the sift-down heights, see [heaps](06-heaps.md)). Each of the n − 1 extractions costs O(log n). The total is O(n log n) in all cases.
- **Space.** O(1). The code uses no recursion.
- **Unstable.** The swap of the root with the last element moves equal elements out of order.
- **In practice.** Heap sort guarantees O(n log n) with O(1) space, but its memory access jumps between a parent index and child indexes that are far apart. Quicksort scans memory in order. So heap sort makes more cache misses, and it is usually slower than quicksort on large random arrays. This is the common explanation; LaMarca and Ladner measured the cache behavior of the sorts (see Further Reading). The size of the effect depends on the data and the hardware (approximate). Libraries use heap sort as the fallback of quicksort and not as the main sort.

### Counting sort

Counting sort counts how many times each key occurs, turns the counts into end positions with a running sum, and places each element at its position. It needs integer keys in a known range of size `k`.

- Step 1: count. `count[x - min]++` for each element.
- Step 2: running sum. After the loop, `count[v]` is the number of elements that are less than or equal to the value `v + min`, which is the index after the last slot of that value.
- Step 3: place. Walk the input **from the right to the left** and write each element at `--count[key]`. The backward walk makes the sort stable: among equal keys, the last one in the input goes to the last slot.

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class LinearTimeSorts {
    // Counting sort for ints. Time O(n + k), space O(n + k), where k = max - min + 1.
    // The backward placement loop makes the sort stable (it matters when sorting records by key).
    static int[] countingSort(int[] a) {
        if (a.length == 0) return a.clone();
        int min = a[0], max = a[0];
        for (int x : a) { min = Math.min(min, x); max = Math.max(max, x); }
        int[] count = new int[max - min + 1]; // assumes max - min + 1 fits in an int and in memory
        for (int x : a) count[x - min]++;
        for (int v = 1; v < count.length; v++) count[v] += count[v - 1]; // count[v] = end index + 1
        int[] out = new int[a.length];
        for (int i = a.length - 1; i >= 0; i--) {
            out[--count[a[i] - min]] = a[i];
        }
        return out;
    }

    // LSD radix sort for int, base 256: 4 passes, each a stable counting sort on one byte.
    // Time O(4 * (n + 256)) = O(n). Space O(n). Handles negative numbers.
    static void radixSort(int[] a) {
        int[] src = a, dst = new int[a.length];
        for (int shift = 0; shift < 32; shift += 8) {
            int[] count = new int[257];
            for (int x : src) count[digit(x, shift) + 1]++;
            for (int d = 0; d < 256; d++) count[d + 1] += count[d]; // count[d] = start index of d
            for (int x : src) dst[count[digit(x, shift)]++] = x;    // forward loop: stable
            int[] tmp = src; src = dst; dst = tmp;
        }
        // After 4 passes (an even number), src is the original array again, so the data is in a.
    }

    // The byte of x at the given shift. For the top byte, flip the sign bit so that negative
    // numbers (top byte 0x80..0xFF) sort before non-negative numbers (0x00..0x7F).
    private static int digit(int x, int shift) {
        int d = (x >>> shift) & 0xFF;
        return shift == 24 ? d ^ 0x80 : d;
    }

    // Bucket sort for doubles in [0, 1). Expected O(n) if the values are spread evenly.
    static void bucketSort(double[] a) {
        int n = a.length;
        if (n == 0) return;
        List<List<Double>> buckets = new ArrayList<>();
        for (int i = 0; i < n; i++) buckets.add(new ArrayList<>());
        for (double x : a) buckets.get((int) (x * n)).add(x); // x in [0,1) gives index 0..n-1
        int k = 0;
        for (List<Double> bucket : buckets) {
            Collections.sort(bucket); // each bucket is small on average
            for (double x : bucket) a[k++] = x;
        }
    }
}
```

- **Time.** O(n + k): one pass over the input to find the range, one to count, one over the counts, one to place.
- **Space.** O(n + k) for the output and the counts.
- **Use it** if k is not much larger than n. For example, sorting ages, grades, or characters (k = 26 or 256). It is a bad choice for `k` near 10⁹. The comparison table in this page shows that a comparison sort then wins.
- **Stable**: yes, with the backward placement loop. This matters, because radix sort uses counting sort as its inner step.

### Radix sort

LSD (least significant digit) radix sort sorts the numbers by their last digit, then by the next digit, and so on up to the most significant digit. Each pass must be **stable**. Why this works, by induction: after the pass on digit `p`, the numbers are sorted by their lowest `p + 1` digits. The next pass sorts by digit `p + 1` and keeps the earlier order among equal digits, so the numbers are sorted by their lowest `p + 2` digits.

The `radixSort` method above uses base 256 on `int` values: 4 passes, each a counting sort on one byte. The top byte needs a sign fix. Negative numbers have the top bit set, so their top byte (0x80 to 0xFF) would sort after the non-negative numbers (0x00 to 0x7F). The code flips the top bit of that byte with `^ 0x80`, which puts negative numbers first.

- **Time.** O(d · (n + b)) for `d` digits in base `b`. For a `w`-bit key and a base of 2^r, d = ⌈w / r⌉ passes, each costing O(n + 2^r). For 32-bit ints with r = 8, the cost is 4 · (n + 256), which is O(n).
- **Space.** O(n + b) for the second array and the counts.
- **Stable**: yes, because each pass is a stable counting sort.
- **Choosing the base.** A larger base means fewer passes and a larger count array. A base near n makes the count array about as large as the input. Sorting n integers in the range 0 to n² − 1 takes 2 passes in base n, so it runs in O(n). An exercise in CLRS section 8.3 (exercise 8.3-4 in the 3rd edition) asks for the same idea with the range 0 to n³ − 1 (3 passes).
- **When it beats a comparison sort.** If keys are fixed-width integers or fixed-length strings, and n is large. The constant factors are higher than they look, so measure first.
- **MSD radix sort** starts from the most significant digit and recurses on each group. It suits variable-length strings, but it is more complex. LeetCode 164 ("Maximum Gap") has an O(n) solution with radix sort or with buckets.

### Bucket sort

Bucket sort distributes the elements into `k` buckets by value range, sorts each bucket, and concatenates the buckets. The code above sorts doubles in [0, 1) with `n` buckets: the element `x` goes to bucket `(int) (x * n)`.

- **Time.** If the input is spread evenly, each bucket holds O(1) elements on average, and the expected time is O(n) (CLRS section 8.4 proves it for uniform input). If all elements land in one bucket, the cost is the cost of the bucket's own sort, which is O(n²) for insertion sort and O(n log n) for a good sort.
- **Space.** O(n + k).
- **Stable**: yes, if the elements are added to buckets in input order and each bucket is sorted by a stable sort. `Collections.sort` is stable.
- **Frequency buckets.** A common interview use has the bucket index equal to a count, and not a value range. For "Top K Frequent Elements" (LeetCode 347), count the frequency of each number with a hash map. The frequency of any number is at most `n`, so put each number into `bucket[frequency]` and read the buckets from the highest index down. This gives O(n) time, and it is the answer when the interviewer asks for better than O(n log n).

### The lower bound: Ω(n log n) for comparison sorts

**Claim.** Every comparison sort needs Ω(n log n) comparisons in the worst case.

**Proof, with a decision tree.** Fix an algorithm and an input size `n` with distinct elements. Draw all its possible executions as a binary tree. Each internal node is one comparison `a[i] < a[j]`, and its two children are the two outcomes. Each leaf is a final answer, which is a permutation of the input.

1. The algorithm must give a different answer for each of the n! orderings of the input, so the tree has at least n! leaves.
2. A binary tree of height `h` has at most 2^h leaves. So 2^h ≥ n!, and h ≥ log₂(n!).
3. The worst-case number of comparisons is the height `h`, which is the longest path from the root to a leaf.
4. n! ≥ (n/2)^(n/2), because the n/2 largest factors of n! (from n down to n/2) are each at least n/2. So log₂(n!) ≥ (n/2) · log₂(n/2) = Ω(n log n).

Therefore h = Ω(n log n). Merge sort and heap sort reach the bound with O(n log n), so both are asymptotically optimal among comparison sorts. A small check: for n = 10, 10! = 3,628,800, and log₂(10!) is about 21.8, so any comparison sort needs at least 22 comparisons in the worst case on 10 elements.

**What the bound does not cover.** Counting, radix, and bucket sort are faster than n log n because they never compare two elements. They use the key as an array index, and they need assumptions about the keys (integers in a small range, fixed width, or an even spread). The bound also does not apply to a sort that knows the data is almost sorted: TimSort and insertion sort adapt to this, but the worst case on arbitrary input stays Ω(n log n).

### Sorting in the JDK

The data below comes from the JDK 21 javadoc and source.

| Call | Algorithm | Stable | Source |
|---|---|---|---|
| `Arrays.sort(int[])`, and the other primitive arrays (`long[]`, `short[]`, `char[]`, `byte[]`, `float[]`, `double[]`) | Dual-Pivot Quicksort (Yaroslavskiy, Bentley, Bloch). The javadoc says "O(n log(n)) performance on all data sets" | Not applicable: equal primitives cannot be told apart | `Arrays` javadoc |
| `Arrays.sort(Object[])` | Stable, adaptive, iterative mergesort (TimSort, class `ComparableTimSort`) | Yes, by specification | `Arrays` javadoc and source |
| `Arrays.sort(T[], Comparator)` | TimSort (class `TimSort`) | Yes, by specification | `Arrays` javadoc and source |
| `List.sort(Comparator)` | Default method: copies the list to an array, sorts the array (TimSort), writes the elements back. `ArrayList` overrides it and sorts its own array with `Arrays.sort(elementData, 0, size, c)` | Yes, by specification | `List` javadoc, `ArrayList` source |
| `Collections.sort(List)` and `Collections.sort(List, Comparator)` | Calls `List.sort`. The first form passes a `null` comparator, which means natural order | Yes, by specification | `Collections` javadoc |
| `Arrays.parallelSort(int[])` and the other primitive arrays | Dual-Pivot Quicksort, with partitions sorted in parallel in the ForkJoin common pool | Not applicable | `Arrays` javadoc and `DualPivotQuicksort` source |
| `Arrays.parallelSort(T[])` and `Arrays.parallelSort(T[], Comparator)` | Parallel sort-merge: splits the array into sub-arrays, sorts each with the matching `Arrays.sort` method in the ForkJoin common pool, and merges them. Needs working space up to the size of the array | Yes, the javadoc guarantees it | `Arrays` javadoc |

Details:

- **Why two algorithms.** The sort for objects must be stable by specification, and quicksort is not stable. For primitives, stability has no meaning, so the JDK uses the faster Dual-Pivot Quicksort. This is an inference from the specification, and the javadoc does not state the reason.
- **Primitive sort in the source** (`java.util.DualPivotQuicksort`, JDK 21). It uses insertion sort for a small leftmost range (fewer than 44 elements, `MAX_INSERTION_SORT_SIZE`) and a mixed insertion sort for small parts elsewhere (fewer than 65 elements plus a term that grows with the recursion depth). It merges runs if the array is mostly sorted, and it switches to heap sort if the recursion depth goes past a limit (`MAX_RECURSION_DEPTH`). The last rule gives the O(n log n) worst case. It uses counting sort for `byte[]` of more than 64 elements and for `short[]` and `char[]` of more than 1750 elements. These thresholds are implementation details and can change between JDK versions.
- **History of the primitive sort.** JDK 7 replaced the one-pivot quicksort with Dual-Pivot Quicksort (JDK-6880672). JDK 14 rewrote it (JDK-8226297) and added the heap sort fallback and the mixed insertion sort. The JDK 7 to 13 source has no heap sort fallback, and the JDK 8 javadoc promises only "O(n log(n)) performance on many data sets that cause other quicksorts to degrade to quadratic performance". The JDK 21 javadoc says "on all data sets".
- **TimSort.** It finds natural runs in the input, extends short runs to a minimum length with binary insertion sort, and merges runs with a stack invariant and galloping. Arrays shorter than 32 elements (`MIN_MERGE`) are sorted by binary insertion sort without a merge. The javadoc says it needs "approximately n" comparisons on nearly sorted input, and temporary storage from a small constant up to n/2 object references. Tim Peters designed it for Python. Joshua Bloch wrote the JDK port (he is the `@author` of `TimSort.java`), and it replaced the old mergesort in JDK 7 (JDK-6804124).
- **`double` order.** `Arrays.sort(double[])` uses the total order of `Double.compareTo`: `-0.0` is less than `0.0`, and `NaN` is greater than every other value. The `<` operator does not give a total order on `double`.
- **Sorting a `LinkedList`.** The default `List.sort` copies the elements to an array, so it costs O(n log n). The javadoc says this avoids the n² log n cost of an in-place sort of a linked list.
- **Descending order.** `Arrays.sort(int[])` has no comparator. Sort ascending and reverse the array, or use `Integer[]` with `Collections.reverseOrder()`. Do not negate values to reverse the order: `-Integer.MIN_VALUE` is still `Integer.MIN_VALUE`.

### Comparators and the overflow bug in `a - b`

The [Java toolkit](../java-toolkit.md) lists this bug among its Java pitfalls. This section gives the full contract and the reasons. A `Comparator` returns a negative number, zero, or a positive number. Its javadoc requires three properties for all `x`, `y`, `z`:

1. **Antisymmetry**: `signum(compare(x, y)) == -signum(compare(y, x))`.
2. **Transitivity**: if `compare(x, y) > 0` and `compare(y, z) > 0`, then `compare(x, z) > 0`.
3. **Consistency of ties**: if `compare(x, y) == 0`, then `compare(x, z)` and `compare(y, z)` have the same sign for every `z`.

The expression `(a, b) -> a - b` looks like it follows these rules, and it fails when the subtraction overflows. For example, `Integer.MIN_VALUE - 1` wraps around to `Integer.MAX_VALUE`, so `compare(MIN_VALUE, 1)` returns a positive number, which says that the smallest `int` is larger than 1. The comparator is now wrong, and it can also break transitivity. If TimSort detects the broken contract, it throws `IllegalArgumentException: Comparison method violates its general contract!`. TimSort does not always detect it (arrays with fewer than 32 elements are never merged), so a bad comparator can silently give a wrong order on small inputs and throw only on large inputs in production.

Use these forms:

| Wrong | Correct |
|---|---|
| `(a, b) -> a - b` | `Integer::compare` or `(a, b) -> Integer.compare(a, b)` |
| `(x, y) -> x[0] - y[0]` | `(x, y) -> Integer.compare(x[0], y[0])` or `Comparator.comparingInt(x -> x[0])` |
| `(a, b) -> (int) (a - b)` for `long` values | `Long::compare` |
| `(a, b) -> (int) (a.score - b.score)` for `double` scores | `Double.compare(a.score, b.score)` (a cast to `int` loses fractions below 1) |
| `b - a` for descending order | `Integer.compare(b, a)` or `Comparator.reverseOrder()` |

The subtraction is safe only if both operands lie in a known small range, for example both between 0 and 10⁹. Use `Integer.compare` anyway, because it needs no such proof.

A comparator that is **inconsistent with equals** (`compare(x, y) == 0` while `!x.equals(y)`) is legal, but `TreeSet` and `TreeMap` then treat `x` and `y` as the same key. The `Comparator` javadoc asks for a note in the documentation of such a comparator.

For several keys, chain the comparators: `Comparator.comparingInt(Item::key).reversed().thenComparing(Item::name)`. Because the sort is stable, sorting twice (by the minor key, then by the major key) gives the same result.

### Quickselect: the k-th element in expected O(n)

Quickselect (Hoare's FIND) finds the k-th smallest element without a full sort. It partitions like quicksort, but it recurses into only one side: the side that contains index `k`.

```java
import java.util.concurrent.ThreadLocalRandom;

class Quickselect {
    // Returns the element that would be at index k (0-based) if a were sorted.
    // Expected time O(n) with a random pivot. Worst case O(n^2). Space O(1). Reorders a.
    static int select(int[] a, int k) {
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int pivot = a[ThreadLocalRandom.current().nextInt(lo, hi + 1)];
            // Three-way partition of a[lo..hi] around the pivot.
            int lt = lo, i = lo, gt = hi;
            while (i <= gt) {
                if (a[i] < pivot) { int t = a[lt]; a[lt++] = a[i]; a[i++] = t; }
                else if (a[i] > pivot) { int t = a[i]; a[i] = a[gt]; a[gt--] = t; }
                else i++;
            }
            if (k < lt) hi = lt - 1;        // the answer is in the "less than" part
            else if (k > gt) lo = gt + 1;   // the answer is in the "greater than" part
            else return pivot;              // k falls in the block of values equal to the pivot
        }
        return a[k];
    }

    // LeetCode 215: the k-th largest is the (n - k)-th smallest, counting from 0.
    static int findKthLargest(int[] nums, int k) {
        return select(nums, nums.length - k);
    }
}
```

- **Time.** O(n) expected with a random pivot, and O(n²) in the worst case.
- **Why expected O(n).** A random pivot lies in the middle half of the sorted order with probability 1/2. In that case the remaining range has at most 3n/4 elements. So the expected number of partitions that shrink the range by a factor of 3/4 is 2. The expected total work is at most 2n · (1 + 3/4 + (3/4)² + ...) = 8n, which is O(n). The sum is a geometric series. CLRS section 9.2 gives a tighter proof.
- **Worst case O(n) with the median of medians** (CLRS section 9.3, groups of 5). The constant is large, so interviews and libraries use the random pivot.
- **Space.** O(1). The method reorders the input array. Copy the array first if the caller needs it unchanged.
- **Three-way partition** makes the code correct and fast on many equal keys: if `k` falls in the equal block, the method returns at once.
- **Compared with other solutions for LeetCode 215:**

| Method | Time | Space | Changes the input |
|---|---|---|---|
| Sort, then index | O(n log n) | O(1) or O(n), see the sort | Yes (in place) |
| Min-heap of size k | O(n log k) | O(k) | No |
| Quickselect | O(n) expected, O(n²) worst case | O(1) | Yes |

A heap suits a stream of data, or a `k` much smaller than `n`, because it holds only `k` elements and does not need the whole input in memory. Quickselect suits a single call on an array. See the [heap pattern](../16-heap-priority-queue/README.md) for the heap solution.

### Which sort do you choose?

| Situation | Choice | Reason |
|---|---|---|
| General objects in Java | `List.sort` or `Arrays.sort(T[], Comparator)` | Stable, adaptive, O(n log n) guaranteed |
| General `int[]` or `long[]` in Java | `Arrays.sort(int[])` | The JDK's tuned Dual-Pivot Quicksort |
| Fewer than about 50 elements, or nearly sorted data | Insertion sort | O(n + I) for I inversions |
| Need stability and O(n log n) in your own code | Merge sort | Stable and guaranteed |
| Need O(1) extra space and O(n log n) worst case | Heap sort | The only one in the table with both |
| Many equal keys | Three-way quicksort | O(n) when all keys are equal |
| Small integer range | Counting sort | O(n + k) |
| Fixed-width integers or strings, large `n` | Radix sort | O(d · (n + b)) |
| Evenly spread floating-point input | Bucket sort | Expected O(n) |
| Linked list | Merge sort | No random access needed. Relinking needs no buffer |
| Only the k-th element or the top k | Quickselect or a heap | No full sort needed |
| Data larger than memory | External merge sort | Sorted chunks and a k-way merge |

## Trade-offs and When to Use

| Criterion | Best choice | Why |
|---|---|---|
| Guaranteed O(n log n), stable | Merge sort | O(n log n) in every case and stable, at the cost of O(n) space |
| Guaranteed O(n log n), no extra memory | Heap sort | O(1) space, at the cost of stability and cache behavior |
| Fastest in practice on arrays of primitives | Quicksort variants (the JDK uses Dual-Pivot Quicksort) | Few operations per element and sequential memory access (measure on your data) |
| Almost sorted input | Insertion sort or TimSort | Work in proportion to the number of inversions or runs |
| Keys are small integers | Counting sort | Linear time, no comparisons |
| Memory is tight | Heap sort, or in-place quicksort | No O(n) buffer |
| Input arrives as a stream and you need the top k | Heap | Holds k elements. Sorting needs all data |
| Records sorted by several keys | Stable sort, chained comparators | Equal keys keep the order from the earlier sort |

Recommendation: in interviews and on the job, call the library sort and write the comparator carefully. Write your own sort only when the problem asks for it, or when you need a property that the library does not give (counting sort for a tiny key range, quickselect for the k-th element, merge sort with a counting step).

### Problems that use each sort

All difficulties come from the LeetCode problem list.

| Problem | Difficulty | Sorting idea |
|---|---|---|
| 1122 Relative Sort Array | Easy | Counting sort over the value range |
| 75 Sort Colors | Medium | Three-way partition with pivot 1, in one pass and O(1) space |
| 912 Sort an Array | Medium | Write merge sort, heap sort, or quicksort yourself. A Lomuto quicksort with a fixed pivot can time out on sorted or all-equal input |
| 215 Kth Largest Element in an Array | Medium | Quickselect or a heap |
| 148 Sort List | Medium | Merge sort on a linked list |
| 56 Merge Intervals | Medium | Sort by start, then scan |
| 179 Largest Number | Medium | Sort strings with the comparator `(a + b)` against `(b + a)` |
| 347 Top K Frequent Elements | Medium | Bucket sort by frequency, or a heap |
| 451 Sort Characters By Frequency | Medium | Counting sort or bucket sort by frequency |
| 164 Maximum Gap | Medium | Radix sort or buckets, for an O(n) solution |
| 969 Pancake Sorting | Medium | A sort by one operation (reverse a prefix), like selection sort |
| 315 Count of Smaller Numbers After Self | Hard | Merge sort with index tracking, or a Fenwick tree |
| 493 Reverse Pairs | Hard | Count pairs during the merge step of merge sort |

## Java Example

The sorting algorithms are above, each in its own block. The next program runs on its own. It shows four behaviors that interviewers ask about: selection sort is unstable while `List.sort` is stable, the `a - b` comparator is wrong for extreme values, comparators chain for several keys, and `Arrays.sort(double[])` orders `-0.0` and `NaN`.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortDemo {
    record Item(String name, int key) {}

    // Selection sort on records, to show that it is not stable.
    static void selectionSort(Item[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j].key() < a[min].key()) min = j;
            }
            Item tmp = a[i]; a[i] = a[min]; a[min] = tmp;
        }
    }

    public static void main(String[] args) {
        // 1. Stability. The names a and b both have key 2.
        Item[] items = {new Item("a", 2), new Item("b", 2), new Item("c", 1)};
        Item[] copy = items.clone();
        selectionSort(copy);
        System.out.println("selection sort: " + Arrays.toString(copy));
        List<Item> list = new ArrayList<>(Arrays.asList(items));
        list.sort(Comparator.comparingInt(Item::key)); // stable: a stays before b
        System.out.println("List.sort:      " + list);

        // 2. The overflow bug of "a - b".
        System.out.println("MIN_VALUE - 1 = " + (Integer.MIN_VALUE - 1));
        List<Integer> wrong = new ArrayList<>(List.of(1, Integer.MIN_VALUE));
        wrong.sort((a, b) -> a - b);                 // BUG: overflows
        List<Integer> right = new ArrayList<>(List.of(1, Integer.MIN_VALUE));
        right.sort(Integer::compare);                // correct
        System.out.println("a - b:           " + wrong);
        System.out.println("Integer.compare: " + right);

        // 3. Descending order and several keys.
        Integer[] boxed = {3, 1, 2};
        Arrays.sort(boxed, Collections.reverseOrder());
        System.out.println("descending: " + Arrays.toString(boxed));
        List<Item> people = new ArrayList<>(List.of(
            new Item("zoe", 30), new Item("amy", 30), new Item("bob", 25)));
        people.sort(Comparator.comparingInt(Item::key).reversed().thenComparing(Item::name));
        System.out.println("by key desc, then name: " + people);

        // 4. Arrays.sort(double[]) uses the total order of Double.compareTo.
        double[] d = {Double.NaN, 0.0, -0.0, -1.5};
        Arrays.sort(d);
        System.out.println("doubles: " + Arrays.toString(d));
    }
}
```

<!-- expected-output -->
```text
selection sort: [Item[name=c, key=1], Item[name=b, key=2], Item[name=a, key=2]]
List.sort:      [Item[name=c, key=1], Item[name=a, key=2], Item[name=b, key=2]]
MIN_VALUE - 1 = 2147483647
a - b:           [1, -2147483648]
Integer.compare: [-2147483648, 1]
descending: [3, 2, 1]
by key desc, then name: [Item[name=amy, key=30], Item[name=zoe, key=30], Item[name=bob, key=25]]
doubles: [-1.5, -0.0, 0.0, NaN]
```

The `a - b` result `[1, -2147483648]` is wrong: the list is not in ascending order. This is the output of a comparator bug, and the sort did not throw an exception.

## Interview Angle

### Questions

??? question "Why is quicksort O(n²) in the worst case, and how do you avoid it?"

    Quicksort partitions around a pivot and recurses on both sides. If the pivot is always the smallest or the largest element, one side has n − 1 elements and the other has none. The recurrence is T(n) = T(n − 1) + Θ(n), which is Θ(n²). A fixed rule such as "last element" triggers this on sorted, reversed, or all-equal input.

    To avoid it: pick a random pivot (no fixed input is bad, and the expected time is O(n log n)), or use the median of three, or use a three-way partition for many equal keys. For a guarantee, use introsort, which switches to heap sort when the recursion gets too deep. The JDK's primitive sort does this. Also recurse into the smaller part first, so the stack depth stays at O(log n).

??? question "What does 'stable' mean, which sorts are stable, and when does it matter?"

    A stable sort keeps equal elements in their input order. Insertion, bubble, merge, counting, and radix sort are stable (with the standard implementation). Selection, quick, and heap sort are not.

    It matters when you sort records by several keys in turn, or when the input order carries meaning. For example, sort employees by name, then stably by department, and each department stays in name order. It also matters for radix sort, because each pass must keep the order that the earlier pass made. In Java, the object sorts (`List.sort`, `Arrays.sort(T[], Comparator)`) are stable by specification. The primitive sorts have no stability, because equal primitives are identical.

??? question "Why does merge sort need O(n) extra space, and can you avoid it?"

    The merge step reads from two sorted halves and writes into one array. If it writes into the same array that it reads, it overwrites elements that it has not read yet. So it copies one half (or both) into a buffer first, and the buffer needs up to n slots. The code can allocate the buffer once and reuse it.

    Merge sort can be done in place, but the in-place merge algorithms are complex and slow in practice, so nobody uses them in interviews. For linked lists, the merge relinks nodes and needs no buffer, so a linked-list merge sort uses O(1) extra space for the merge and O(log n) for the recursion. A bottom-up version removes the recursion too.

??? question "What does `Arrays.sort` use in Java, and why are there two different algorithms?"

    For primitive arrays (`int[]`, `double[]`, and so on), JDK 21 uses a Dual-Pivot Quicksort, and the javadoc says it gives O(n log n) on all data sets. Since JDK 14, the source falls back to heap sort when the recursion gets too deep, which backs that guarantee. Before JDK 14 the javadoc said "on many data sets", and no fallback existed. For object arrays, `Collections.sort`, and `List.sort`, it uses TimSort, a stable, adaptive mergesort.

    The reason for two algorithms: the object sorts must be stable by specification, and quicksort is not stable. Stability has no meaning for primitives, so the JDK picks the faster algorithm there. TimSort also needs far fewer comparisons on nearly sorted input (about n), which helps when the comparisons are costly method calls.

??? question "What is wrong with `Arrays.sort(arr, (a, b) -> a - b)`?"

    The subtraction can overflow. With `a = Integer.MIN_VALUE` and `b = 1`, `a - b` wraps around to a large positive number, so the comparator says `a > b`. The comparator is then wrong, and it can violate transitivity. TimSort may throw `IllegalArgumentException: Comparison method violates its general contract!`, or it may silently return a wrong order. TimSort does not detect it every time.

    The fix: `Integer.compare(a, b)`, or `Comparator.comparingInt(...)`. For `long` use `Long.compare`. For `double` use `Double.compare`, because a cast of the difference to `int` also drops fractions. The subtraction is safe only if you can prove that the values have a small range, and `Integer.compare` needs no such proof.

??? question "Prove that a comparison sort needs Ω(n log n) comparisons."

    Model the algorithm as a binary decision tree: each internal node is a comparison, each leaf is an output order. The algorithm must separate all n! input orders, so the tree has at least n! leaves. A binary tree of height `h` has at most 2^h leaves, so h ≥ log₂(n!). Since n! ≥ (n/2)^(n/2), log₂(n!) ≥ (n/2) · log₂(n/2) = Ω(n log n). The height is the worst-case number of comparisons, so every comparison sort needs Ω(n log n) in the worst case.

    The proof assumes that the algorithm learns about the order only through comparisons. Counting sort, radix sort, and bucket sort use the key value as an index, so they are not bound by it.

??? question "When would you use counting sort or radix sort instead of a comparison sort? What are the limits?"

    Use counting sort if the keys are integers in a small range `k` and k = O(n). The time is O(n + k) and the space is O(n + k). Examples: ages, exam scores, characters. If `k` is much larger than `n` (for example 10⁹), the count array is too large and the sort loses.

    Use radix sort for fixed-width keys (32-bit integers, fixed-length strings). It runs a stable counting sort on each digit, so the time is O(d · (n + b)). With base 256 on 32-bit integers, that is 4 passes. The limits: it needs fixed-width keys or a key that you can split into digits, it needs O(n) extra space, and its constant factor is higher than the O(n) suggests. For a key range of 0 to n² − 1, radix sort in base n sorts in 2 passes, so it is O(n).

??? question "Which sort do you use for a linked list, and why?"

    Merge sort. A linked list has no random access, so heap sort and quicksort partition schemes that index into the array do not fit. Merge sort walks the list in order, and its merge step relinks nodes, so it needs no O(n) buffer. The time is O(n log n). The recursion stack is O(log n) for the top-down version. A bottom-up version, which merges runs of size 1, 2, 4, and so on, uses O(1) extra space (LeetCode 148). Insertion sort also works on a list, but it costs O(n²).

??? question "Find the k-th largest element of an array. What are your options and their costs?"

    Three options.

    - Sort and index: O(n log n) time. It is the simplest.
    - Min-heap of size k: push each element and pop when the size exceeds k. O(n log k) time and O(k) space. It does not change the input, and it works on a stream.
    - Quickselect: partition around a random pivot and recurse into the side that holds the index `n - k`. O(n) expected time, O(n²) worst case, O(1) space. It reorders the input.

    I would start with the heap or the sort and say that quickselect gives O(n) expected. The median-of-medians pivot gives a worst-case O(n) guarantee, with a large constant. See the [heap pattern](../16-heap-priority-queue/README.md).

??? question "Compare Lomuto and Hoare partitions."

    Lomuto scans with one pointer, uses the last element as the pivot, and puts the pivot at its final index. It is short and easy to write. Equal keys all go to one side, so all-equal input costs O(n²). It does more swaps.

    Hoare moves two pointers from the two ends toward each other and swaps out-of-place pairs. It does fewer swaps. It stops at elements equal to the pivot, so equal keys split evenly and the time on all-equal input is O(n log n). Its details are easier to get wrong: it returns a split index and not the pivot position, the recursive ranges are `[lo, j]` and `[j + 1, hi]`, and the pivot must not be the last element in the common form. Three-way partition handles many equal keys best, at O(n) for all-equal input.

??? question "Why is heap sort not the default if it is O(n log n) in the worst case with O(1) space?"

    Its constant factor is worse in practice. Heap sort accesses indexes `i`, `2i + 1`, and `2i + 2`, which are far apart for large heaps, so it makes more cache misses, while quicksort and merge sort scan memory in sequence. The size of the gap depends on the data and the hardware. Heap sort is also not stable, and it is not adaptive: it takes O(n log n) even on sorted input. It earns its place as a fallback (introsort) that gives quicksort a worst-case guarantee, and as the choice when O(1) space and a worst-case bound are both required.

??? question "When is insertion sort the right choice?"

    For small arrays (a few dozen elements) and for nearly sorted data. Its cost is O(n + I) for I inversions, and it has a tiny constant: no recursion, no allocation, sequential access. This is why the JDK's Dual-Pivot Quicksort and TimSort both use insertion sort for small ranges. It is also the right answer for "keep a list sorted while you add a few new items".

### Follow-up Questions

??? question "How would you sort 100 GB of integers on a machine with 8 GB of memory?"

    Use external merge sort. Read the data in chunks that fit in memory (for example 4 GB), sort each chunk in memory, and write it to disk as a sorted run. That makes about 25 runs. Then run a k-way merge: keep one input buffer for each run and a min-heap with the front element of each run. Pop the smallest, write it to the output, and refill from the same run. The merge reads each element once and costs O(n log k) comparisons for `k` runs. If `k` is too large for the memory buffers, merge in several rounds.

??? question "How would you count inversions in an array in O(n log n)?"

    Modify merge sort. During the merge of the sorted halves `left` and `right`, if the code takes an element from `right`, then that element is smaller than every element still in `left`. Each such pair is an inversion, so add the number of remaining `left` elements to the counter. The merge step stays linear, so the total time is O(n log n). The same idea solves LeetCode 315 (with index tracking) and 493 (count the pairs before the merge). A Fenwick tree is the second method, see `kb/dsa/21-segment-tree-and-fenwick-tree/README.md`.

??? question "How do you sort an array that contains only 0, 1, and 2, in one pass and O(1) space?"

    Use the three-way partition with the pivot value 1 (LeetCode 75). Keep `lt` (end of the 0 zone), `i` (the scan index), and `gt` (start of the 2 zone). If `a[i] == 0`, swap it with `a[lt]` and advance both. If `a[i] == 2`, swap it with `a[gt]` and decrement `gt` without advancing `i`. If it is 1, advance `i`. The loop stops when `i > gt`. Each element is examined at most once, so the time is O(n).

??? question "Is Java's `Arrays.sort(int[])` safe against adversarial input that triggers quadratic time?"

    On JDK 14 and later, yes. The JDK 21 javadoc states "O(n log(n)) performance on all data sets". The source supports this: if the recursion depth passes a limit, `DualPivotQuicksort` switches to heap sort for that range. On JDK 7 to 13, the source has no such fallback, and the JDK 8 javadoc promises O(n log n) only "on many data sets". A quicksort without a fallback has inputs that cost O(n²), and a public service that sorts untrusted input with it can be attacked.

    Recommendation: on an old JDK, sort untrusted primitive data as objects (TimSort has an O(n log n) worst case), or shuffle it first.

### Red Flags

- "Quicksort is O(n log n)", with no mention of the worst case. It is O(n²) in the worst case.
- "Merge sort is in place."
- Writing a comparator as `a - b` or `(int) (a - b)`.
- Writing Lomuto quicksort with the last element as pivot and not stating that sorted or all-equal input is the worst case.
- Using Hoare's partition with the wrong recursion ranges, or with `a[hi]` as the pivot.
- Not knowing which sorts are stable, or saying that `Arrays.sort` is stable for `int[]`. The question has no meaning for primitives.
- Claiming that a comparison sort can run in O(n), or that counting sort beats the lower bound without stating its assumptions.
- Offering bubble sort or selection sort as the answer to "sort efficiently".
- Using `list.sort(null)` or `Collections.sort` on elements that do not implement `Comparable`, and getting a `ClassCastException`.
- Sorting an array to find the minimum or the maximum, which costs O(n log n) for an O(n) task.

## Common Mistakes

| Mistake | Why it fails | Fix |
|---|---|---|
| `(a, b) -> a - b` as a comparator | Overflow gives a wrong order, or `IllegalArgumentException` from TimSort | `Integer.compare(a, b)` or `Comparator.comparingInt` |
| Sorting `int[]` with a comparator | `Arrays.sort(int[], Comparator)` does not exist | Use `Integer[]`, or sort and reverse |
| Reversing the order with `-x` | `-Integer.MIN_VALUE` is `Integer.MIN_VALUE` | Sort ascending and reverse, or use `Comparator.reverseOrder()` |
| `(lo + hi) / 2` in merge sort or binary search | `lo + hi` can overflow | `lo + (hi - lo) / 2` |
| Lomuto quicksort with a fixed pivot on sorted input | O(n²) time and a deep recursion | Random pivot, or median of three, or a three-way partition |
| Recursing on both sides of quicksort with no depth control | The stack depth can reach n and throw `StackOverflowError` | Recurse into the smaller part and loop on the larger |
| Hoare partition with `[lo, j - 1]` as the left range | The element at `j` is lost | Recurse on `[lo, j]` and `[j + 1, hi]` |
| Taking the right element on a tie in `merge` | The merge becomes unstable | Take the left element on a tie: test `aux[j] < aux[i]` with a strict `<` |
| Counting sort that fills the output from left to right | The sort is not stable, and radix sort breaks | Fill from right to left |
| Counting sort with a negative key | `count[x]` has a negative index | Subtract the minimum, as in `x - min` |
| Allocating a new buffer in each merge call | Many allocations, garbage collection pressure | Allocate one buffer and pass it down |
| Sorting a list in place while iterating it | `ConcurrentModificationException` (`ArrayList.sort` checks `modCount`) | Sort before the loop or on a copy |
| Assuming that `Arrays.sort(Object[])` works on any object | `ClassCastException` if the elements are not `Comparable` | Pass a `Comparator` |
| Quickselect with a fixed pivot | O(n²) on sorted input | Random pivot |
| Sorting twice by two keys in the wrong order | The first sort is lost, if the sort is not stable | Sort by the minor key first, then by the major key. Or chain comparators |

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| Stable vs in-place | Stable keeps the order of equal keys. In-place uses O(1) extra memory | Do equal keys carry information, or is memory the limit? |
| Quicksort vs merge sort | Both divide and conquer. Quicksort works before the recursion (partition) and is unstable and in place. Merge sort works after (merge) and is stable and needs a buffer | Do you need stability or a worst-case guarantee? |
| Lomuto vs Hoare | One scan pointer, returns the pivot position, vs two pointers, returns a split index | Are there many equal keys? Do you want fewer swaps? |
| Quickselect vs quicksort | Quickselect recurses into one side and is O(n) expected. Quicksort recurses into both sides | Do you need the whole order or one rank? |
| Heap sort vs `PriorityQueue` sort | Heap sort works in place on an array in O(1) space. Adding all items to a `PriorityQueue` and polling costs O(n) extra space | Is the data already in an array, and is memory tight? |
| Counting sort vs radix sort | Counting sorts by the whole key with `k` counters. Radix runs counting sort on one digit at a time | Is the key range small (counting), or is the key wide but fixed (radix)? |
| Bucket sort vs counting sort | Buckets hold ranges of values and are sorted inside. Counters hold exact values | Are the keys spread evenly over a range, or are they exact small integers? |
| `Arrays.sort` vs `Collections.sort` | One sorts an array, the other a `List`. For objects, both end in TimSort | Is the data an array or a list? |
| `Comparator` vs `Comparable` | `Comparable` is the natural order inside the class. `Comparator` is an order that you pass from outside | Do you need more than one order? |
| `compareTo` consistent with equals vs not | If `compare(x, y) == 0` but `x.equals(y)` is false, sorted sets treat them as one element | Do you put the elements in a `TreeSet` or `TreeMap`? |
| Best, average, and worst case of quicksort | O(n log n), O(n log n), O(n²) | Does the input contain adversarial or sorted data with a fixed pivot rule? |

## Quick Reference Card

- Comparison sorts need Ω(n log n) comparisons (decision tree: n! leaves, height at least log₂(n!)).
- Insertion: O(n) best, O(n²) worst, stable, O(1). Best for small or nearly sorted input.
- Selection: O(n²) in all cases, unstable, at most n − 1 swaps.
- Bubble: O(n²), O(n) with the early-exit flag, stable.
- Merge: O(n log n) in all cases, stable, O(n) space. The choice for linked lists and for counting inversions.
- Quick: O(n log n) average, O(n²) worst, unstable, O(log n) stack if you recurse into the smaller part first.
- Heap: O(n log n) in all cases, O(1) space, unstable.
- Counting: O(n + k), stable with a backward fill. Radix: O(d · (n + b)), needs a stable inner sort. Bucket: expected O(n) for evenly spread input.
- Lomuto: last element pivot, one pointer. Hoare: two pointers, recurse on `[lo, j]` and `[j + 1, hi]`. Three-way: handles equal keys, O(n) when all equal.
- Random pivot: expected O(n log n) for every fixed input.
- JDK 21: `Arrays.sort(primitive[])` is Dual-Pivot Quicksort, with a heap sort fallback since JDK 14. `Arrays.sort(Object[])`, `List.sort`, and `Collections.sort` are TimSort (stable).
- Comparator: use `Integer.compare`, not `a - b`.
- Quickselect: k-th smallest in O(n) expected, O(1) space, reorders the input.
- Mid index: `lo + (hi - lo) / 2`.

## Further Reading

- Cormen, Leiserson, Rivest, Stein, *Introduction to Algorithms* (CLRS): chapter 2 (insertion sort, merge sort), chapter 6 (heapsort), chapter 7 (quicksort, with the randomized version and the analysis), chapter 8 (the decision-tree lower bound, counting sort, radix sort, bucket sort), and chapter 9 (selection in expected and worst-case linear time). The chapter numbers are the same in the 3rd and 4th editions. The Lomuto partition is CLRS's `PARTITION`, and the Hoare partition is problem 7-1.
- Sedgewick and Wayne, *Algorithms*, 4th edition, chapter 2: mergesort, quicksort with a Hoare-style two-pointer partition and the three-way partition, the ~2N ln N average comparisons (Proposition K, section 2.3), and heapsort (section 2.4). The book's code is in the [algs4 repository](https://github.com/kevin-wayne/algs4).
- [`Arrays` javadoc (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html): the specified algorithms and the stability statements for each `sort` method, and the total order that `sort(double[])` uses.
- [`List.sort` javadoc (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#sort(java.util.Comparator)): the default implementation, the stability guarantee, and the implementation note on the mergesort.
- [`Collections.sort` javadoc (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#sort(java.util.List)): the stability guarantee and the delegation to `List.sort`.
- [`Comparator` javadoc (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html): the three contract rules, and the comparator factories `comparingInt`, `thenComparing`, and `reversed`.
- JDK 21 source of `java.util.DualPivotQuicksort`, `java.util.TimSort`, and `java.util.ArrayList.sort` (OpenJDK repository, `src/java.base/share/classes/java/util`): the thresholds, the heap sort fallback, and the `MIN_MERGE` constant of 32.
- Tim Peters, ["listsort.txt"](https://github.com/python/cpython/blob/main/Objects/listsort.txt) in the CPython repository: the original description of TimSort, which the JDK ported.
- LaMarca and Ladner, ["The Influence of Caches on the Performance of Sorting"](https://courses.cs.washington.edu/courses/csep521/99sp/reading/cachesort.pdf), Journal of Algorithms, 1999: measurements of the cache behavior of heapsort, mergesort, and quicksort.
- Joshua Bloch, ["Extra, Extra - Read All About It: Nearly All Binary Searches and Mergesorts are Broken"](https://research.google/blog/extra-extra-read-all-about-it-nearly-all-binary-searches-and-mergesorts-are-broken/) (Google Research blog, June 2006): the `(lo + hi) / 2` overflow bug.
- Yaroslavskiy, "Dual-Pivot Quicksort algorithm" (2009): the description of the algorithm that the JDK adopted for primitive arrays in Java 7.
- OpenJDK issues [JDK-6804124](https://bugs.openjdk.org/browse/JDK-6804124) (TimSort replaces the mergesort, fixed in 7), [JDK-6880672](https://bugs.openjdk.org/browse/JDK-6880672) (Dual-Pivot Quicksort replaces quicksort, fixed in 7), and [JDK-8226297](https://bugs.openjdk.org/browse/JDK-8226297) (Dual-Pivot Quicksort rewrite, fixed in 14): the version history of the JDK sorts.
- [`Arrays` javadoc (JDK 8)](https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html): the older "on many data sets" wording for the primitive sorts.
