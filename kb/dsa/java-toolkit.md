# Java Toolkit for DSA Interviews

This file turns the guide's pseudocode into Java.

---

## How to Use This File

The pattern files (01–19) teach each pattern in pseudocode. Pseudocode shows the idea. It does not show the Java calls, the Java types, or the Java bugs. This file covers those three things.

1. Learn a pattern in its own file.
2. Look up the Java calls for that pattern in **Map Each Pattern to Java**.
3. Write the pattern from **Java Templates** until you can write it without looking.
4. Read **Java Pitfalls That Cost Interviews** before every mock interview.

Every snippet assumes these imports:

```java
import java.util.*;
import java.util.stream.*;
```

Every Java block in this file compiles with JDK 17. Methods are written without `static` and without a class, the same way you write them inside LeetCode's `class Solution`.

**Terms used in this file:**
- **Amortized O(1)**: one call can cost O(n), but n calls cost O(n) in total. `ArrayList.add` is an example. When the backing array is full, `add` copies it into an array 1.5 times larger. The copies over n calls total O(n) elements, so the average cost per call is O(1).
- **Boxing**: Java stores an `int` inside an `Integer` object. Collections such as `List<Integer>` hold boxed values. **Unboxing** converts the object back to an `int`.
- **View**: a collection that reads from another collection instead of copying it. A change to one shows in the other.

---

## Collections Cheat Sheet

n is the number of elements in the structure. All times are worst case unless the table says otherwise.

### ArrayList

| Call | What it does | Time |
|------|--------------|------|
| `List<Integer> list = new ArrayList<>();` | Creates an empty list | O(1) |
| `list.add(x)` | Appends x at the end | Amortized O(1) |
| `list.add(i, x)` | Inserts x at index i and shifts the rest right | O(n) |
| `list.get(i)` / `list.set(i, x)` | Reads or writes index i | O(1) |
| `list.remove(list.size() - 1)` | Removes the last element | O(1) |
| `list.remove(i)` | Removes index i and shifts the rest left | O(n) |
| `list.contains(x)` / `list.indexOf(x)` | Scans from the start | O(n) |
| `list.size()` / `list.isEmpty()` | Reads the size | O(1) |
| `Collections.sort(list)` / `list.sort(null)` | Sorts ascending (TimSort, stable) | O(n log n) |
| `Collections.reverse(list)` | Reverses in place | O(n) |
| `Collections.swap(list, i, j)` | Swaps two indices | O(1) |
| `new ArrayList<>(other)` | Copies another collection | O(n) |
| `List.of(1, 2, 3)` | Creates an immutable list; `add` throws `UnsupportedOperationException` | O(n) |
| `listOfArrays.toArray(new int[0][])` | Converts `List<int[]>` to `int[][]` | O(n) |

**Stable sort**: equal elements keep their original order.

### int[] and Arrays

| Call | What it does | Time |
|------|--------------|------|
| `int[] a = new int[n];` | Creates an array filled with 0 | O(n) |
| `Arrays.sort(a)` | Sorts a primitive array (dual-pivot quicksort, not stable) | O(n log n) |
| `Arrays.sort(a, from, to)` | Sorts the range `[from, to)` | O(k log k), k = to − from |
| `Arrays.sort(objs, comparator)` | Sorts `Integer[]`, `int[][]`, or `String[]` with a comparator (TimSort, stable). Does not accept `int[]` | O(n log n) |
| `Arrays.fill(a, v)` | Sets every element to v | O(n) |
| `Arrays.copyOfRange(a, from, to)` | Returns a new array with `a[from..to-1]` | O(to − from) |
| `Arrays.copyOf(a, len)` / `a.clone()` | Returns a copy | O(n) |
| `Arrays.equals(a, b)` | Compares contents | O(n) |
| `Arrays.toString(a)` | Formats the array for printing | O(n) |
| `Arrays.binarySearch(a, key)` | Returns the index of key, or `-(insertionPoint) - 1` if absent. Array must be sorted | O(log n) |
| `Arrays.asList(objs)` | Returns a fixed-size list view of an object array. `add` and `remove` throw. **Caveat:** on an `int[]` it returns a `List<int[]>` with one element | O(1) |
| `Arrays.stream(a).sum()` | Sums an `int[]`. The result is `int` and can overflow | O(n) |
| `Arrays.stream(a).asLongStream().sum()` | Sums an `int[]` as `long` | O(n) |
| `Arrays.stream(a).max().getAsInt()` | Returns the maximum | O(n) |
| `Arrays.stream(a).boxed().collect(Collectors.toList())` | Converts `int[]` to `List<Integer>` | O(n) |

### HashMap and HashSet

Times are average case. The worst case is O(log n) per call when keys implement `Comparable` (Java 8 and later), and O(n) otherwise.

| Call | What it does | Time |
|------|--------------|------|
| `Map<String, Integer> map = new HashMap<>();` | Creates an empty map | O(1) |
| `map.put(k, v)` | Inserts or replaces; returns the old value or `null` | O(1) |
| `map.get(k)` | Returns the value or `null` | O(1) |
| `map.containsKey(k)` / `map.remove(k)` | Checks or removes a key | O(1) |
| `map.getOrDefault(k, 0)` | Returns the value, or 0 if k is absent | O(1) |
| `map.merge(k, 1, Integer::sum)` | Adds 1 to the count of k; inserts 1 if k is absent | O(1) |
| `map.computeIfAbsent(k, x -> new ArrayList<>()).add(v)` | Creates the list for k if absent, then appends v | O(1) |
| `map.putIfAbsent(k, v)` | Inserts only if k is absent; returns the existing value or `null` | O(1) |
| `for (Map.Entry<String, Integer> e : map.entrySet())` | Iterates pairs; read with `e.getKey()` and `e.getValue()` | O(n) total |
| `map.keySet()` / `map.values()` | Returns a view of the keys or values | O(1), O(n) to iterate |
| `Set<Integer> set = new HashSet<>();` | Creates an empty set | O(1) |
| `set.add(x)` | Adds x; returns `false` if x was already present | O(1) |
| `set.contains(x)` / `set.remove(x)` | Checks or removes x | O(1) |

### TreeMap and TreeSet

A TreeMap keeps keys sorted. It is a red-black tree (a self-balancing binary search tree). Lookup methods return `null` when no key matches. Unboxing that `null` into an `int` throws `NullPointerException`.

| Call | What it does | Time |
|------|--------------|------|
| `TreeMap<Integer, Integer> tm = new TreeMap<>();` | Creates an empty sorted map | O(1) |
| `put` / `get` / `remove` / `containsKey` | Same meaning as HashMap | O(log n) |
| `tm.floorKey(k)` | Greatest key ≤ k, or `null` | O(log n) |
| `tm.ceilingKey(k)` | Least key ≥ k, or `null` | O(log n) |
| `tm.lowerKey(k)` | Greatest key < k, or `null` | O(log n) |
| `tm.higherKey(k)` | Least key > k, or `null` | O(log n) |
| `tm.firstKey()` / `tm.lastKey()` | Smallest or largest key; throws `NoSuchElementException` if empty | O(log n) |
| `tm.pollFirstEntry()` / `tm.pollLastEntry()` | Removes and returns the smallest or largest entry, or `null` if empty | O(log n) |
| `tm.headMap(k)` | View of keys < k. `headMap(k, true)` includes k | O(log n), O(size) to iterate |
| `tm.tailMap(k)` | View of keys ≥ k. `tailMap(k, false)` excludes k | O(log n), O(size) to iterate |
| `TreeSet<Integer> ts = new TreeSet<>();` | Creates an empty sorted set | O(1) |
| `ts.floor(x)` / `ts.ceiling(x)` / `ts.lower(x)` / `ts.higher(x)` | Same meaning as the TreeMap key methods | O(log n) |
| `ts.first()` / `ts.last()` / `ts.pollFirst()` / `ts.pollLast()` | Smallest or largest element | O(log n) |

### ArrayDeque

Declare it as `Deque<Integer> dq = new ArrayDeque<>();`. One class serves as a stack, a queue, and a deque (double-ended queue). All calls below are O(1); adds are amortized O(1). `ArrayDeque` rejects `null`.

| Role | Call | What it does | Empty deque |
|------|------|--------------|-------------|
| Stack | `push(x)` | Adds x at the top (the front) | — |
| Stack | `pop()` | Removes and returns the top | Throws `NoSuchElementException` |
| Stack | `peek()` | Returns the top | Returns `null` |
| Queue | `offer(x)` | Adds x at the back | — |
| Queue | `poll()` | Removes and returns the front | Returns `null` |
| Queue | `peek()` | Returns the front | Returns `null` |
| Deque | `offerFirst(x)` / `offerLast(x)` | Adds at the front or the back | — |
| Deque | `pollFirst()` / `pollLast()` | Removes and returns the front or the back | Returns `null` |
| Deque | `peekFirst()` / `peekLast()` | Returns the front or the back | Returns `null` |
| All | `size()` / `isEmpty()` | Reads the size | — |

`push` and `offer` add at opposite ends. Do not mix stack calls and queue calls on one deque.

### PriorityQueue

A PriorityQueue is a binary heap. `peek` returns the smallest element under the comparator.

| Call | What it does | Time |
|------|--------------|------|
| `new PriorityQueue<Integer>()` | Min-heap (smallest on top) | O(1) |
| `new PriorityQueue<Integer>(Collections.reverseOrder())` | Max-heap | O(1) |
| `new PriorityQueue<Integer>((a, b) -> Integer.compare(b, a))` | Max-heap | O(1) |
| `new PriorityQueue<int[]>((a, b) -> Integer.compare(a[0], b[0]))` | Min-heap of `int[]` ordered by `a[0]` | O(1) |
| `new PriorityQueue<>(collection)` | Builds a heap from a collection | O(n) |
| `pq.offer(x)` | Inserts x | O(log n) |
| `pq.poll()` | Removes and returns the top, or `null` if empty | O(log n) |
| `pq.peek()` | Returns the top, or `null` if empty | O(1) |
| `pq.size()` / `pq.isEmpty()` | Reads the size | O(1) |
| `pq.remove(x)` / `pq.contains(x)` | Scans the heap for x | O(n) |

### LinkedList

`LinkedList` implements both `List` and `Deque`. It accepts `null`. Use `ArrayDeque` for stacks and queues. Use `LinkedList` when the queue must hold `null`.

| Call | What it does | Time |
|------|--------------|------|
| `addFirst(x)` / `addLast(x)` | Adds at either end | O(1) |
| `removeFirst()` / `removeLast()` | Removes from either end; throws if empty | O(1) |
| `get(i)` / `set(i, x)` | Walks from the nearer end to index i | O(n) |
| `iterator.remove()` | Removes the element the iterator returned last | O(1) |

### StringBuilder

| Call | What it does | Time |
|------|--------------|------|
| `StringBuilder sb = new StringBuilder();` | Creates an empty builder | O(1) |
| `sb.append(x)` | Appends a char, int, String, or other value | Amortized O(1) per char |
| `sb.insert(i, x)` | Inserts at index i and shifts the rest | O(n) |
| `sb.reverse()` | Reverses in place | O(n) |
| `sb.setCharAt(i, c)` | Replaces the char at index i | O(1) |
| `sb.deleteCharAt(i)` | Removes the char at index i and shifts the rest | O(n − i) |
| `sb.deleteCharAt(sb.length() - 1)` | Removes the last char | O(1) |
| `sb.setLength(len)` | Truncates to len chars when len is smaller | O(1) |
| `sb.charAt(i)` / `sb.length()` | Reads a char or the length | O(1) |
| `sb.toString()` | Copies the content into a new String | O(n) |

### String

A String is immutable. Every method that "changes" a String returns a new String.

| Call | What it does | Time |
|------|--------------|------|
| `s.charAt(i)` / `s.length()` | Reads a char or the length | O(1) |
| `s.toCharArray()` | Copies the chars into a new `char[]` | O(n) |
| `s.substring(b, e)` | Copies `s[b..e-1]` into a new String. This is a copy, not a view | O(e − b) |
| `String.valueOf(chars)` / `new String(chars)` | Builds a String from a `char[]` | O(n) |
| `String.valueOf(x)` | Converts an int, char, or other value to a String | O(digits) |
| `s.equals(t)` / `s.compareTo(t)` | Compares contents. Never use `==` on Strings | O(n) |
| `s.indexOf(t)` / `s.contains(t)` | Searches for t | O(n · m) worst case |
| `s.split(" ")` | Splits on a regular expression | O(n) |
| `String.join(",", list)` | Joins with a separator | O(total length) |
| `s + t` | Concatenates into a new String | O(n + m) |

### Character

All calls are O(1).

| Call | What it does |
|------|--------------|
| `Character.isDigit(c)` | `'0'`–`'9'` (and other Unicode digits) |
| `Character.isLetter(c)` | Letters |
| `Character.isLetterOrDigit(c)` | Letters or digits; use it for "alphanumeric" problems |
| `Character.toLowerCase(c)` / `Character.toUpperCase(c)` | Converts case |
| `Character.isUpperCase(c)` / `Character.isLowerCase(c)` | Checks case |
| `c - 'a'` | Index 0–25 for a lowercase letter |
| `c - '0'` | Numeric value of a digit char |

### Integer and Long

| Call | What it does | Time |
|------|--------------|------|
| `Integer.MAX_VALUE` | 2³¹ − 1 = 2,147,483,647 (about 2.1 × 10⁹) | O(1) |
| `Integer.MIN_VALUE` | −2³¹ = −2,147,483,648 | O(1) |
| `Long.MAX_VALUE` / `Long.MIN_VALUE` | 2⁶³ − 1 and −2⁶³ (about 9.2 × 10¹⁸) | O(1) |
| `Integer.bitCount(x)` / `Long.bitCount(x)` | Number of 1 bits | O(1) |
| `Integer.toBinaryString(x)` | Binary digits as a String, no leading zeros | O(32) |
| `Integer.parseInt(s)` / `Long.parseLong(s)` | Parses a String; throws `NumberFormatException` on bad input | O(length) |
| `Integer.parseInt(s, 2)` | Parses binary digits | O(length) |
| `Integer.compare(a, b)` | Returns negative, 0, or positive without overflow | O(1) |
| `Integer.numberOfTrailingZeros(x)` | Index of the lowest 1 bit | O(1) |

### Math

| Call | What it does |
|------|--------------|
| `Math.max(a, b)` / `Math.min(a, b)` | Larger or smaller value |
| `Math.abs(x)` | Absolute value. `Math.abs(Integer.MIN_VALUE)` returns `Integer.MIN_VALUE` |
| `Math.floorMod(a, n)` | Remainder with the sign of n. `Math.floorMod(-3, 5)` is 2 |
| `Math.floorDiv(a, b)` | Division that rounds toward negative infinity. `Math.floorDiv(-7, 2)` is −4; `-7 / 2` is −3 |
| `Math.pow(a, b)` / `Math.sqrt(x)` | Return `double`. Avoid them for exact integer work |

---

## Map Each Pattern to Java

| # | Pattern | Java structures and calls |
|---|---------|---------------------------|
| 01 | [Sliding Window](./01-sliding-window/README.md) | `int[26]` or `int[128]` counts instead of a HashMap when the alphabet is fixed. `HashMap<Character, Integer>` with `merge` when it is not. `ArrayDeque<Integer>` of indices for sliding window maximum |
| 02 | [Two Pointers](./02-two-pointers/README.md) | `Arrays.sort(int[])` first. `char[] cs = s.toCharArray()` for in-place swaps, then `String.valueOf(cs)` |
| 03 | [Fast & Slow Pointers](./03-fast-slow-pointers/README.md) | `ListNode`. Compare nodes with `==`; reference identity is the correct test here |
| 04 | [Prefix Sum / Difference Array](./04-prefix-sum/README.md) | `long[] prefix = new long[n + 1]`. `HashMap<Integer, Integer>` from prefix sum to count, seeded with `put(0, 1)`. `int[] diff = new int[n + 1]` |
| 05 | [Hashing & Frequency Maps](./05-hashing-frequency/README.md) | `HashMap` with `getOrDefault` and `merge`. `HashSet`. `computeIfAbsent` for grouping. Anagram key: sort a `char[]`, then `String.valueOf(chars)` |
| 06 | [Binary Search on Answer](./06-binary-search-on-answer/README.md) | `int mid = lo + (hi - lo) / 2`. `long` totals inside the feasibility check. `TreeMap.floorKey` / `ceilingKey` for lookups in a changing sorted set |
| 07 | [Monotonic Stack / Queue](./07-monotonic-stack-queue/README.md) | Stack: `ArrayDeque<Integer>` of indices with `push` / `pop` / `peek`. Queue: `ArrayDeque<Integer>` with `offerLast` / `pollLast` / `pollFirst` / `peekFirst` |
| 08 | [Recursion & Backtracking](./08-recursion-backtracking/README.md) | `List<Integer> path` with `add` and `remove(path.size() - 1)`. `result.add(new ArrayList<>(path))`. `boolean[] used`. `StringBuilder` with `setLength` to undo |
| 09 | [Divide and Conquer](./09-divide-and-conquer/README.md) | Recurse on index bounds `(lo, hi)` instead of copying subarrays. One shared `int[] temp` buffer for merge sort |
| 10 | [Tree DFS / BFS Patterns](./10-tree-patterns/README.md) | `TreeNode`. Recursion for DFS. `ArrayDeque<TreeNode>` with a `size` loop for level order. Check `node.left != null` before `offer` |
| 11 | [Graph Traversal Patterns](./11-graph-traversal/README.md) | `List<List<Integer>>` adjacency list. `boolean[] visited`. Grid BFS: `ArrayDeque<int[]>` of `{row, col}` and an `int[][] DIRS` array |
| 12 | [Topological Sort](./12-topological-sort/README.md) | `int[] indegree`. `ArrayDeque<Integer>` queue. `List<List<Integer>>` adjacency list |
| 13 | [Union Find](./13-union-find/README.md) | A class with `int[] parent` and `int[] size` (or `int[] rank`) |
| 14 | [Greedy Patterns](./14-greedy/README.md) | `Arrays.sort` with `Integer.compare` comparators. `PriorityQueue` for "pick the best remaining" |
| 15 | [Dynamic Programming](./15-dynamic-programming/README.md) | `int[]` / `long[]` tables. Memo as `Integer[]` (`null` = not computed) or `long[]` filled with −1. One `Arrays.fill` per row for 2D. `long` with `% 1_000_000_007` for counting |
| 16 | [Heap / Priority Queue](./16-heap-priority-queue/README.md) | `PriorityQueue` min-heap of size k for top-k. Max-heap via `Collections.reverseOrder()`. `PriorityQueue<int[]>` with `Integer.compare` on one field. Two heaps for a running median |
| 17 | [Trie (Prefix Tree)](./17-trie/README.md) | A node class with `Trie[] children = new Trie[26]` indexed by `c - 'a'` and a `boolean isWord` |
| 18 | [Shortest Paths](./18-shortest-paths/README.md) | Dijkstra: `PriorityQueue<int[]>` of `{dist, node}` with `Integer.compare` on dist; skip stale entries. `int[] dist` with `INF = 1_000_000_000`, or `long[] dist`. Bellman-Ford: `int[][] edges` and `dist.clone()` per round. Floyd-Warshall: `int[][]` with `INF = 1_000_000_000` so that `INF + INF` fits in an `int` |
| 19 | [Bit Manipulation](./19-bit-manipulation/README.md) | `Integer.bitCount`. `x & (x - 1)` clears the lowest 1 bit. `x & -x` keeps only the lowest 1 bit. `>>>` shifts in zeros; `>>` copies the sign bit. `1L << k` when k ≥ 31 |
| 20 | [Intervals & Sweep Line](./20-intervals/README.md) | `int[][]` sorted with `Arrays.sort(iv, (a, b) -> Integer.compare(a[0], b[0]))`. `List<int[]>` result, then `toArray(new int[0][])`. `PriorityQueue<Integer>` of end times for meeting rooms. `TreeMap` for calendar booking |

---

## Java Pitfalls That Cost Interviews

Each pitfall shows a WRONG snippet and a CORRECT snippet. Every WRONG snippet compiles. Each one fails at runtime, returns a wrong result, or runs slower than it must.

### 1. Comparator `(a, b) -> a - b` overflows

`a - b` overflows when a and b have opposite signs and the difference exceeds the `int` range. The sort then puts elements in the wrong order.

WRONG:
```java
Integer[] arr = {3, Integer.MIN_VALUE, -3};
Arrays.sort(arr, (a, b) -> a - b);   // result [3, MIN_VALUE, -3]: MIN_VALUE - 3 overflows to a positive number
```

CORRECT:
```java
Integer[] arr = {3, Integer.MIN_VALUE, -3};
Arrays.sort(arr, (a, b) -> Integer.compare(a, b));   // [MIN_VALUE, -3, 3]
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
```

### 2. `==` on `Integer` values above 127

Java caches `Integer` objects from −128 to 127. Outside that range, `==` compares two different object references.

WRONG:
```java
Map<Character, Integer> a = new HashMap<>(), b = new HashMap<>();
a.put('x', 1000);
b.put('x', 1000);
boolean same = a.get('x') == b.get('x');   // false: two different Integer objects
```

CORRECT:
```java
Map<Character, Integer> a = new HashMap<>(), b = new HashMap<>();
a.put('x', 1000);
b.put('x', 1000);
boolean same = a.get('x').equals(b.get('x'));   // true
int countA = a.get('x');                          // or unbox to int, then use ==
boolean same2 = countA == b.get('x');             // true: one side is int, so Java unboxes
```

### 3. `Arrays.asList(int[])` returns `List<int[]>`

`Arrays.asList` takes objects. An `int[]` is one object, so the list holds one element.

WRONG:
```java
int[] nums = {3, 1, 2};
List<int[]> list = Arrays.asList(nums);   // list.size() == 1
// List<Integer> bad = Arrays.asList(nums);   // does not compile
```

CORRECT:
```java
int[] nums = {3, 1, 2};
List<Integer> list = Arrays.stream(nums).boxed().collect(Collectors.toList());
Integer[] boxed = {3, 1, 2};
List<Integer> view = Arrays.asList(boxed);   // fixed size: set() works, add() throws
```

### 4. `int` overflow in `mid`, sums, and products

`lo + hi` overflows when both are above 2³⁰. `a * b` is computed as `int` before Java widens it to `long`.

WRONG:
```java
int lo = 1_500_000_000, hi = 2_000_000_000;
int mid = (lo + hi) / 2;          // lo + hi overflows: mid is negative
int a = 100_000, b = 100_000;
long product = a * b;             // overflows as int, then widens the wrong value
int[] nums = {2_000_000_000, 2_000_000_000};
int sum = 0;
for (int x : nums) sum += x;      // overflows
```

CORRECT:
```java
int lo = 1_500_000_000, hi = 2_000_000_000;
int mid = lo + (hi - lo) / 2;
int a = 100_000, b = 100_000;
long product = (long) a * b;      // cast one operand before the multiply
int[] nums = {2_000_000_000, 2_000_000_000};
long sum = 0;
for (int x : nums) sum += x;
```

### 5. `Math.abs(Integer.MIN_VALUE)` is negative

−2³¹ has no positive `int` counterpart. `Math.abs` returns the input unchanged.

WRONG:
```java
int x = Integer.MIN_VALUE;
int y = Math.abs(x);              // y == -2147483648
```

CORRECT:
```java
int x = Integer.MIN_VALUE;
long y = Math.abs((long) x);      // 2147483648
```

### 6. `%` on a negative number is negative

Java's `%` keeps the sign of the left operand. A circular index such as `(i - 1) % n` returns −1 when i is 0.

WRONG:
```java
int n = 5, i = 0;
int prev = (i - 1) % n;           // -1: ArrayIndexOutOfBoundsException on use
```

CORRECT:
```java
int n = 5, i = 0;
int prev = Math.floorMod(i - 1, n);   // 4
int prev2 = ((i - 1) % n + n) % n;    // 4, the same result without floorMod
```

### 7. Modifying a collection while iterating

A for-each loop throws `ConcurrentModificationException` if the loop body adds to or removes from the same collection.

WRONG:
```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4));
for (Integer x : list) {
    if (x % 2 == 0) list.remove(x);   // ConcurrentModificationException
}
```

CORRECT:
```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4));
list.removeIf(x -> x % 2 == 0);
Map<Character, Integer> count = new HashMap<>(Map.of('a', 0, 'b', 2));
count.entrySet().removeIf(e -> e.getValue() == 0);
Iterator<Integer> it = list.iterator();
while (it.hasNext()) {
    if (it.next() > 2) it.remove();   // Iterator.remove is safe
}
```

### 8. `Stack` is a legacy class

`Stack` extends `Vector`. Every call is synchronized (locked), which adds cost. Its iterator runs from bottom to top, which is the reverse of pop order.

WRONG:
```java
Stack<Integer> stack = new Stack<>();
stack.push(1);
stack.push(2);
System.out.println(stack);        // [1, 2]: bottom first
```

CORRECT:
```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);
stack.push(2);
System.out.println(stack);        // [2, 1]: top first
int top = stack.pop();            // 2
```

### 9. `ArrayDeque` rejects `null`

`offer(null)` and `push(null)` throw `NullPointerException`. This breaks tree BFS code that enqueues missing children.

WRONG:
```java
Deque<Integer> queue = new ArrayDeque<>();
Integer missingChild = null;
queue.offer(missingChild);        // NullPointerException
```

CORRECT:
```java
Deque<Integer> queue = new ArrayDeque<>();
Integer missingChild = null;
if (missingChild != null) queue.offer(missingChild);   // skip nulls
Queue<Integer> withNulls = new LinkedList<>();
withNulls.offer(null);            // LinkedList accepts null, for tree serialization
```

### 10. `PriorityQueue` iteration order is not sorted

A for-each loop and `toString()` read the internal heap array. Only `poll()` returns elements in sorted order.

WRONG:
```java
PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(5, 1, 4, 2, 3));
System.out.println(pq);           // [1, 2, 4, 5, 3]: heap layout, not sorted
```

CORRECT:
```java
PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(5, 1, 4, 2, 3));
List<Integer> sorted = new ArrayList<>();
while (!pq.isEmpty()) sorted.add(pq.poll());   // [1, 2, 3, 4, 5]
```

### 11. String concatenation in a loop is O(n²)

`s += x` copies the whole String on every pass.

WRONG:
```java
int n = 10_000;
String s = "";
for (int i = 0; i < n; i++) s += i;   // n copies of a growing String
```

CORRECT:
```java
int n = 10_000;
StringBuilder sb = new StringBuilder();
for (int i = 0; i < n; i++) sb.append(i);
String s = sb.toString();
```

### 12. `result.add(path)` stores a reference

The result holds the same list object that backtracking keeps changing. At the end, every entry shows the final state of `path`.

WRONG:
```java
List<List<Integer>> result = new ArrayList<>();
List<Integer> path = new ArrayList<>();
path.add(1);
result.add(path);                 // same object as path
path.remove(path.size() - 1);     // result is now [[]]
```

CORRECT:
```java
List<List<Integer>> result = new ArrayList<>();
List<Integer> path = new ArrayList<>();
path.add(1);
result.add(new ArrayList<>(path));   // snapshot copy
path.remove(path.size() - 1);        // result is still [[1]]
```

### 13. `char` arithmetic

A `char` is a 16-bit number. `'a'` is 97. Arithmetic on a `char` produces an `int`.

WRONG:
```java
String s = "abc";
int[] count = new int[26];
for (char c : s.toCharArray()) count[c]++;   // index 97: ArrayIndexOutOfBoundsException
System.out.println('a' + 1);                 // prints 98, not b
```

CORRECT:
```java
String s = "abc";
int[] count = new int[26];
for (char c : s.toCharArray()) count[c - 'a']++;   // 0..25
int i = 2;
char letter = (char) ('a' + i);                    // 'c'
int digit = '7' - '0';                             // 7
```

### 14. Deep recursion throws `StackOverflowError`

The default thread stack is 512 KB to 1 MB, depending on the platform. Recursion depth near 10⁴ to 10⁵ can exhaust it. A DFS on a path-shaped graph or a skewed tree reaches that depth.

WRONG:
```java
int depth(int n) {
    return n == 0 ? 0 : 1 + depth(n - 1);   // depth(100_000) throws StackOverflowError
}
```

CORRECT:
```java
int depth(int n) {
    int d = 0;
    while (n > 0) {                          // same logic as a loop
        n--;
        d++;
    }
    return d;
}

// For recursive DFS: replace the call stack with an explicit ArrayDeque.
// If you keep recursion, tell the interviewer the depth limit and the iterative fix.
```

### 15. 2D array initialization

`Arrays.fill(dp, row)` stores the same row object in every slot. `Arrays.fill(grid, -1)` on an `int[][]` compiles and throws `ArrayStoreException`.

WRONG:
```java
int[][] dp = new int[3][];
Arrays.fill(dp, new int[4]);      // all 3 slots point to one array
dp[0][0] = 5;                     // dp[1][0] and dp[2][0] are 5 too
int[][] grid = new int[3][4];
Arrays.fill(grid, -1);            // ArrayStoreException at runtime
```

CORRECT:
```java
int[][] dp = new int[3][4];       // 3 separate rows of 4 zeros
int[][] grid = new int[3][4];
for (int[] row : grid) Arrays.fill(row, -1);
```

### 16. `List.remove(int index)` vs `List.remove(Object)`

On a `List<Integer>`, `remove(int)` removes by index. `remove(Integer)` removes by value. An `int` argument always picks the index version.

WRONG:
```java
List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
list.remove(1);                   // removes index 1 (value 20), not the value 1
int val = 30;
list.remove(val);                 // IndexOutOfBoundsException: 30 is read as an index
```

CORRECT:
```java
List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
int val = 30;
list.remove(Integer.valueOf(val));   // removes the value 30
```

### 17. `HashMap` with `int[]` keys uses identity

Arrays do not override `equals` and `hashCode`. Two arrays with equal contents are different keys.

WRONG:
```java
Map<int[], Integer> map = new HashMap<>();
map.put(new int[]{1, 2}, 5);
Integer v = map.get(new int[]{1, 2});   // null
```

CORRECT:
```java
int r = 1, c = 2, cols = 1000;
Map<String, Integer> byString = new HashMap<>();
byString.put(r + "," + c, 5);              // String key
Map<List<Integer>, Integer> byList = new HashMap<>();
byList.put(List.of(r, c), 5);              // List key: List.equals compares contents
Map<Long, Integer> byLong = new HashMap<>();
byLong.put((long) r * cols + c, 5);        // encode the pair as one long
```

### 18. `Integer.MAX_VALUE` as INF overflows on `+`

`Integer.MAX_VALUE + w` wraps to a negative number. The relaxation check then accepts a path that does not exist.

WRONG:
```java
int[] dist = new int[3];
Arrays.fill(dist, Integer.MAX_VALUE);
dist[0] = 0;
int w = 5;
if (dist[1] + w < dist[2]) dist[2] = dist[1] + w;   // true: dist[2] becomes negative
```

CORRECT:
```java
final int INF = 1_000_000_000;    // INF + INF still fits in an int
int[] dist = new int[3];
Arrays.fill(dist, INF);
dist[0] = 0;
int w = 5;
if (dist[1] != INF && dist[1] + w < dist[2]) dist[2] = dist[1] + w;
```

---

## Java Templates

Each template is a method or a class you can paste into `class Solution`. The line above each template says when to use it and its cost.

### Sliding Window (Variable Size, Longest Valid)

**Use when:** you need the longest substring or subarray that satisfies a condition. Time O(n), space O(alphabet).

```java
// LC 3: longest substring without repeating characters
int lengthOfLongestSubstring(String s) {
    int[] count = new int[128];               // ASCII counts inside the window
    int left = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        count[c]++;                           // expand: add s[right]
        while (count[c] > 1) {                // invalid: c appears twice
            count[s.charAt(left)]--;          // shrink: remove s[left]
            left++;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

### Two Pointers (Opposite Ends)

**Use when:** the array is sorted and you search for a pair. Time O(n), space O(1).

```java
// LC 167: indices of two numbers that sum to target in a sorted array
int[] twoSumSorted(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left < right) {
        int sum = nums[left] + nums[right];
        if (sum == target) return new int[]{left, right};
        if (sum < target) left++;             // need a larger sum
        else right--;                         // need a smaller sum
    }
    return new int[]{-1, -1};
}
```

### Binary Search (Lower Bound)

**Use when:** you need the first index whose value is ≥ target in a sorted array. Time O(log n), space O(1).

```java
// Returns the first index i with nums[i] >= target, or nums.length if none
int lowerBound(int[] nums, int target) {
    int lo = 0, hi = nums.length;             // search space [lo, hi)
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;         // avoids lo + hi overflow
        if (nums[mid] >= target) hi = mid;    // mid can be the answer
        else lo = mid + 1;
    }
    return lo;
}
```

### Binary Search on Answer

**Use when:** you need the minimum value that passes a check, and the check is monotonic (once it passes, every larger value passes). Time O(n log M), M = answer range; space O(1).

```java
// LC 875: minimum eating speed to finish all piles within h hours
int minEatingSpeed(int[] piles, int h) {
    int lo = 1, hi = 0;
    for (int p : piles) hi = Math.max(hi, p);  // speed max(piles) always works
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (canFinish(piles, h, mid)) hi = mid;   // works: try a smaller speed
        else lo = mid + 1;
    }
    return lo;
}

boolean canFinish(int[] piles, int h, int speed) {
    long hours = 0;                           // long: the total can exceed int
    for (int p : piles) hours += (p - 1) / speed + 1;   // ceil(p / speed) without overflow
    return hours <= h;
}
```

### Prefix Sum + HashMap Count

**Use when:** you count subarrays with sum exactly k, and the array can hold negative numbers. Time O(n), space O(n).

```java
// LC 560: number of subarrays that sum to k
int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> seen = new HashMap<>();   // prefix sum -> times seen
    seen.put(0, 1);                                 // the empty prefix
    int sum = 0, count = 0;
    for (int x : nums) {
        sum += x;
        count += seen.getOrDefault(sum - k, 0);     // earlier prefixes that leave sum k
        seen.merge(sum, 1, Integer::sum);
    }
    return count;
}
```

### Monotonic Stack (Next Greater Element)

**Use when:** each element needs the nearest larger or smaller element to its left or right. Time O(n), space O(n).

```java
// result[i] = first value to the right of i that is greater than nums[i], or -1
int[] nextGreater(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    Arrays.fill(result, -1);
    Deque<Integer> stack = new ArrayDeque<>();      // indices; values decrease toward the top
    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
            result[stack.pop()] = nums[i];          // nums[i] answers the popped index
        }
        stack.push(i);
    }
    return result;
}
```

### BFS on a Grid

**Use when:** you need the fewest steps between cells in an unweighted grid. Time O(m · n), space O(m · n).

```java
static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

// Fewest steps from (0, 0) to (m-1, n-1); 0 = open, 1 = wall; -1 if unreachable
int shortestPath(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    if (grid[0][0] == 1) return -1;
    boolean[][] visited = new boolean[m][n];
    Deque<int[]> queue = new ArrayDeque<>();
    queue.offer(new int[]{0, 0});
    visited[0][0] = true;
    for (int steps = 0; !queue.isEmpty(); steps++) {
        for (int size = queue.size(); size > 0; size--) {   // one BFS level
            int[] cell = queue.poll();
            if (cell[0] == m - 1 && cell[1] == n - 1) return steps;
            for (int[] d : DIRS) {
                int r = cell[0] + d[0], c = cell[1] + d[1];
                if (r < 0 || r >= m || c < 0 || c >= n) continue;
                if (grid[r][c] == 1 || visited[r][c]) continue;
                visited[r][c] = true;               // mark on enqueue, not on dequeue
                queue.offer(new int[]{r, c});
            }
        }
    }
    return -1;
}
```

### BFS on an Adjacency List with Levels

**Use when:** you need the shortest edge count from one source in an unweighted graph. Time O(V + E), space O(V).

```java
// dist[v] = edges on the shortest path from src to v, or -1 if unreachable
int[] bfsLevels(List<List<Integer>> graph, int src) {
    int[] dist = new int[graph.size()];
    Arrays.fill(dist, -1);                    // -1 also means "not visited"
    Deque<Integer> queue = new ArrayDeque<>();
    queue.offer(src);
    dist[src] = 0;
    while (!queue.isEmpty()) {
        int u = queue.poll();
        for (int v : graph.get(u)) {
            if (dist[v] == -1) {
                dist[v] = dist[u] + 1;
                queue.offer(v);
            }
        }
    }
    return dist;
}
```

### DFS (Recursive and Iterative)

**Use when:** you need to visit every node reachable from a source (components, cycle checks, flood fill). Time O(V + E), space O(V).

```java
void dfs(List<List<Integer>> graph, int u, boolean[] visited) {
    visited[u] = true;
    for (int v : graph.get(u)) {
        if (!visited[v]) dfs(graph, v, visited);
    }
}

// Same visit set without recursion; safe for depth 10^5 and above
void dfsIterative(List<List<Integer>> graph, int src, boolean[] visited) {
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(src);
    while (!stack.isEmpty()) {
        int u = stack.pop();
        if (visited[u]) continue;             // a node can be pushed more than once
        visited[u] = true;
        for (int v : graph.get(u)) {
            if (!visited[v]) stack.push(v);
        }
    }
}
```

### Topological Sort (Kahn's Algorithm)

**Use when:** tasks have "u before v" dependencies and you need a valid order or a cycle check. Time O(V + E), space O(V + E).

```java
// edge {u, v} means u must come before v; returns an empty array if there is a cycle
int[] topoSort(int n, int[][] edges) {
    List<List<Integer>> graph = new ArrayList<>();
    for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
    int[] indegree = new int[n];
    for (int[] e : edges) {
        graph.get(e[0]).add(e[1]);
        indegree[e[1]]++;
    }
    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        if (indegree[i] == 0) queue.offer(i); // no remaining prerequisites
    }
    int[] order = new int[n];
    int idx = 0;
    while (!queue.isEmpty()) {
        int u = queue.poll();
        order[idx++] = u;
        for (int v : graph.get(u)) {
            if (--indegree[v] == 0) queue.offer(v);
        }
    }
    return idx == n ? order : new int[0];     // idx < n: some nodes are on a cycle
}
```

### Union Find (Path Compression + Union by Size)

**Use when:** you merge groups and ask "are a and b in the same group?" Time O(α(n)) per call, where α is the inverse Ackermann function (at most 4 for any real input); space O(n).

```java
class UnionFind {
    private final int[] parent, size;
    private int count;                        // number of separate groups

    UnionFind(int n) {
        parent = new int[n];
        size = new int[n];
        count = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;                    // each node starts as its own root
            size[i] = 1;
        }
    }

    int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);   // path compression
        return parent[x];
    }

    boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;           // already in one group
        if (size[ra] < size[rb]) {            // union by size: ra is the larger root
            int t = ra; ra = rb; rb = t;
        }
        parent[rb] = ra;
        size[ra] += size[rb];
        count--;
        return true;
    }

    int count() { return count; }
}
```

### Dijkstra with Lazy Deletion

**Use when:** you need shortest paths from one source and every edge weight is ≥ 0. Time O((V + E) log V), space O(V + E). **Lazy deletion** means you leave outdated heap entries in place and skip them when you poll them.

```java
// graph[u] holds {v, w} pairs; returns dist[], with Integer.MAX_VALUE for unreachable nodes.
// Switch dist and the heap entries to long if a path total can exceed 2^31 - 1.
int[] dijkstra(List<int[]>[] graph, int src) {
    int n = graph.length;
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
    pq.offer(new int[]{0, src});              // {dist, node}
    while (!pq.isEmpty()) {
        int[] top = pq.poll();
        int d = top[0], u = top[1];
        if (d > dist[u]) continue;            // stale entry: u already has a shorter path
        for (int[] e : graph[u]) {
            int v = e[0], nd = d + e[1];      // d is finite here, so no INF overflow
            if (nd < dist[v]) {
                dist[v] = nd;
                pq.offer(new int[]{nd, v});
            }
        }
    }
    return dist;
}
```

### Building an Adjacency List from an Edge Array

**Use when:** the input gives edges as `int[][]`. Time O(V + E), space O(V + E).

```java
// Unweighted, undirected: edge {u, v}
List<List<Integer>> buildGraph(int n, int[][] edges) {
    List<List<Integer>> graph = new ArrayList<>();
    for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
    for (int[] e : edges) {
        graph.get(e[0]).add(e[1]);
        graph.get(e[1]).add(e[0]);            // remove this line for a directed graph
    }
    return graph;
}

// Weighted, directed: edge {u, v, w}
@SuppressWarnings("unchecked")
List<int[]>[] buildWeighted(int n, int[][] edges) {
    List<int[]>[] graph = new ArrayList[n];   // Java forbids new List<int[]>[n]; use the raw type
    for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
    for (int[] e : edges) graph[e[0]].add(new int[]{e[1], e[2]});
    return graph;
}
```

### Backtracking (Subsets and Permutations)

**Use when:** you must list every subset, combination, or ordering. Subsets: time O(n · 2ⁿ). Permutations: time O(n · n!). Space O(n) besides the output.

```java
// LC 78: all subsets
List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    buildSubsets(nums, 0, new ArrayList<>(), result);
    return result;
}

void buildSubsets(int[] nums, int start, List<Integer> path, List<List<Integer>> result) {
    result.add(new ArrayList<>(path));        // copy: path keeps changing
    for (int i = start; i < nums.length; i++) {
        path.add(nums[i]);                    // choose
        buildSubsets(nums, i + 1, path, result);   // explore
        path.remove(path.size() - 1);         // undo
    }
}

// LC 46: all permutations
List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    buildPerms(nums, new boolean[nums.length], new ArrayList<>(), result);
    return result;
}

void buildPerms(int[] nums, boolean[] used, List<Integer> path, List<List<Integer>> result) {
    if (path.size() == nums.length) {
        result.add(new ArrayList<>(path));
        return;
    }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        used[i] = true;
        path.add(nums[i]);
        buildPerms(nums, used, path, result);
        path.remove(path.size() - 1);
        used[i] = false;
    }
}
```

### Memoized DP (Top-Down)

**Use when:** the recurrence is clear and only some states are reachable. Time O(states × transitions), space O(states) plus recursion depth.

```java
static final int INF = 1_000_000_000;         // INF + 1 does not overflow

// LC 322: fewest coins that sum to amount, or -1
int coinChange(int[] coins, int amount) {
    Integer[] memo = new Integer[amount + 1]; // null = not computed yet
    int ans = minCoins(coins, amount, memo);
    return ans >= INF ? -1 : ans;
}

int minCoins(int[] coins, int rem, Integer[] memo) {
    if (rem == 0) return 0;
    if (memo[rem] != null) return memo[rem];
    int best = INF;
    for (int c : coins) {
        if (c <= rem) best = Math.min(best, minCoins(coins, rem - c, memo) + 1);
    }
    return memo[rem] = best;
}
// For long results, use long[] memo with Arrays.fill(memo, -1) and test memo[i] != -1.
```

### Bottom-Up DP

**Use when:** every state is needed, or recursion depth is too large. Time O(states × transitions), space O(states).

```java
// LC 322 again, as a table: dp[x] = fewest coins that sum to x
int coinChangeBottomUp(int[] coins, int amount) {
    final int INF = 1_000_000_000;
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, INF);
    dp[0] = 0;
    for (int x = 1; x <= amount; x++) {
        for (int c : coins) {
            if (c <= x) dp[x] = Math.min(dp[x], dp[x - c] + 1);
        }
    }
    return dp[amount] >= INF ? -1 : dp[amount];
}
```

### Trie (Array Children)

**Use when:** you look up words or prefixes over a fixed lowercase alphabet. Time O(L) per call, L = word length; space O(total chars × 26).

```java
// LC 208
class Trie {
    private final Trie[] children = new Trie[26];   // children[c - 'a']
    private boolean isWord;

    void insert(String word) {
        Trie node = this;
        for (char c : word.toCharArray()) {
            int i = c - 'a';
            if (node.children[i] == null) node.children[i] = new Trie();
            node = node.children[i];
        }
        node.isWord = true;
    }

    boolean search(String word) {
        Trie node = walk(word);
        return node != null && node.isWord;
    }

    boolean startsWith(String prefix) {
        return walk(prefix) != null;
    }

    private Trie walk(String s) {                   // node at the end of s, or null
        Trie node = this;
        for (char c : s.toCharArray()) {
            node = node.children[c - 'a'];
            if (node == null) return null;
        }
        return node;
    }
}
```

### Heap Top-K (Min-Heap of Size k)

**Use when:** you need the k largest items or the kth largest item. Time O(n log k), space O(k).

```java
// LC 215: kth largest element
int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>();   // min-heap
    for (int x : nums) {
        heap.offer(x);
        if (heap.size() > k) heap.poll();     // drop the smallest; k largest remain
    }
    return heap.peek();                       // smallest of the k largest
}

// LC 347: k most frequent values, heap of {value, count} ordered by count
int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : nums) freq.merge(x, 1, Integer::sum);
    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));
    for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
        heap.offer(new int[]{e.getKey(), e.getValue()});
        if (heap.size() > k) heap.poll();
    }
    int[] result = new int[k];
    for (int i = 0; i < k; i++) result[i] = heap.poll()[0];
    return result;
}
```

### Custom Sort of `int[][]` Intervals

**Use when:** the input is a list of intervals. Sort by start, then merge or schedule. Time O(n log n), space O(n).

```java
// LC 56: merge overlapping intervals
int[][] merge(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));   // by start
    List<int[]> merged = new ArrayList<>();
    for (int[] cur : intervals) {
        int[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
        if (last == null || last[1] < cur[0]) {
            merged.add(cur);                  // no overlap: start a new interval
        } else {
            last[1] = Math.max(last[1], cur[1]);   // overlap: extend the last one
        }
    }
    return merged.toArray(new int[0][]);
}

// Two keys: start ascending, then end descending
void sortByStartThenEndDesc(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] != b[0]
            ? Integer.compare(a[0], b[0])
            : Integer.compare(b[1], a[1]));
}
```

---

## Common LeetCode Signatures

LeetCode defines these classes for linked list and tree problems. It shows them in a comment above `class Solution`. Write them yourself when you practice in an IDE. LeetCode declares them `public`; that needs one file per class, so the versions below drop `public`.

```java
// Definition for singly-linked list.
class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}
```

```java
// Definition for a binary tree node.
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode() {}
    TreeNode(int val) { this.val = val; }
    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}
```
