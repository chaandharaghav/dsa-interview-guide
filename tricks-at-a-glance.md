# Tricks at a Glance
## See the Trigger → Reach for the Tool

This is a recognition-first reference. Every entry answers: "When I see *X* in a problem, I should immediately think *Y*."

---

## How to Use This File

1. Read the **Trigger** — what the problem says or implies.
2. The **Trick** tells you the core insight.
3. The **Template** is the minimal code pattern (5 lines or fewer).
4. The **Examples** give you LeetCode anchors to verify your understanding.

---

## Category 1: XOR / Bit Tricks

---

### 1.1 Single Number in a Pair Array
**Trigger:** Every number appears twice except one. Find the unique one in O(n) time, O(1) space.

**Trick:** XOR all numbers. Pairs cancel (a ^ a = 0). The survivor is the answer (a ^ 0 = a).

```
result = 0
for num in nums:
    result ^= num
return result
```
**Examples:** LC 136 (Single Number), LC 268 (Missing Number: XOR index ^ value)

---

### 1.2 Two Singles Among Pairs
**Trigger:** Every number appears twice except **two**. Find both in O(n) time, O(1) space.

**Trick:** XOR all → get `xor = a ^ b`. Find any differing bit (use `xor & (-xor)` = lowest set bit). Partition numbers by that bit → XOR each partition separately → one gives `a`, other gives `b`.

```
xor = XOR of all numbers        // xor = a ^ b
bit = xor & (-xor)              // isolate one differing bit
a = 0, b = 0
for num in nums:
    if num & bit: a ^= num
    else:         b ^= num
return a, b
```
**Examples:** LC 260 (Single Number III)

---

### 1.3 Power of 2 Check
**Trigger:** "Is n a power of 2?" in O(1).

**Trick:** A power of 2 has exactly one set bit. `n - 1` flips all bits below that bit. Their AND = 0.

```
return n > 0 and (n & (n - 1)) == 0
// n > 0 guard is essential — 0 passes (0 & -1 == 0) falsely
```
**Examples:** LC 231 (Power of Two), LC 342 (Power of Four adds extra mask check)

---

### 1.4 Count Set Bits (Brian Kernighan)
**Trigger:** Count the number of 1-bits in an integer.

**Trick:** Each `n & (n-1)` removes the lowest set bit. Loop until n = 0.

```
count = 0
while n != 0:
    n = n & (n - 1)    // remove lowest set bit
    count += 1
return count
```
**Examples:** LC 191 (Number of 1 Bits), LC 338 (Counting Bits — dp[i] = dp[i >> 1] + (i & 1))

---

### 1.5 Isolate Lowest Set Bit
**Trigger:** Need to identify or extract the lowest set bit position.

**Trick:** `n & (-n)` = lowest set bit only. Used in Fenwick Trees and partitioning tricks.

```
lowest_bit = n & (-n)
// -n in two's complement = ~n + 1; bits above the lowest set bit flip, that bit and the zeros below it stay
```
**Examples:** Fenwick Tree updates, LC 260 (partitioning by lowest differing bit)

---

### 1.6 Bitmask Subset Enumeration
**Trigger:** "All subsets of n items" where n ≤ 20.

**Trick:** Iterate `mask` from 0 to `(1 << n) - 1`. Each bit represents inclusion of one item.

```
for mask in range(1 << n):
    subset = []
    for i in range(n):
        if (mask >> i) & 1:
            subset.append(items[i])
    process(subset)
```
**Examples:** LC 78 (Subsets), LC 1286 (Iterator for Combination)

---

### 1.7 Prefix XOR for Subarray XOR = k
**Trigger:** "Count subarrays whose XOR equals k" — exact same structure as prefix sum for subarray sum = k.

**Trick:** Define `prefix[j] = XOR(arr[0..j])`. Then `XOR(arr[i..j]) = prefix[j] ^ prefix[i-1]`. If this equals k, then `prefix[i-1] = prefix[j] ^ k`. Use a hashmap to count how many times each prefix XOR has appeared.

```
count = 0, prefXor = 0
seen = {0: 1}               // prefix XOR of empty prefix
for val in arr:
    prefXor ^= val
    count += seen.get(prefXor ^ k, 0)
    seen[prefXor] = seen.get(prefXor, 0) + 1
return count
```
**Examples:** LC 1310 (XOR Queries), LC 1442, LC 560 uses same structure with addition

---

### 1.8 XOR Range Formula
**Trigger:** "XOR of all integers from 1 to n" (or XOR of a range a..b).

**Trick:** `f(n) = XOR(1..n)` follows a 4-cycle based on `n % 4`:
- n%4 == 0 → n
- n%4 == 1 → 1
- n%4 == 2 → n+1
- n%4 == 3 → 0

Then `XOR(a..b) = f(b) ^ f(a-1)`.

```
function f(n):
    if n % 4 == 0: return n
    if n % 4 == 1: return 1
    if n % 4 == 2: return n + 1
    return 0

return f(b) ^ f(a - 1)
```
**Examples:** LC 1486 (XOR Operation in an Array, O(1) solution)

---

## Category 2: Two-Pass / Direction Tricks

---

### 2.1 Trapping Rain Water — Two Arrays Pass
**Trigger:** "Water trapped at each position = min(tallest left, tallest right) - height."

**Trick:** Precompute `maxLeft[i]` (running max left-to-right) and `maxRight[i]` (running max right-to-left). Water at i = `max(0, min(maxLeft[i], maxRight[i]) - height[i])`.

```
maxLeft[0] = height[0]
for i in 1..n-1:
    maxLeft[i] = max(maxLeft[i-1], height[i])

maxRight[n-1] = height[n-1]
for i in n-2..0:
    maxRight[i] = max(maxRight[i+1], height[i])

total = sum(max(0, min(maxLeft[i], maxRight[i]) - height[i]) for i in range(n))
```
**Examples:** LC 42 (Trapping Rain Water). Two-pointer variant does this in O(1) space.

---

### 2.2 Product of Array Except Self — Two-Pass No Division
**Trigger:** "Product of all elements except self" — can't use division; O(1) extra space.

**Trick:** Left pass builds prefix products (result[i] = product of all elements before i). Right pass multiplies in suffix products (running right product × result[i]).

```
result[0] = 1
for i in 1..n-1:
    result[i] = result[i-1] * nums[i-1]   // left products

right = 1
for i in n-1..0:
    result[i] *= right
    right *= nums[i]                        // suffix product
```
**Examples:** LC 238 (Product of Array Except Self)

---

### 2.3 Candy — Two Sweep Rule
**Trigger:** Problem requires a condition from both the left neighbor AND the right neighbor at each position.

**Trick:** Two separate greedy passes. Pass 1: left-to-right, enforce left constraint. Pass 2: right-to-left, enforce right constraint (take max of both passes per position).

```
candy[i] = 1 for all i

// Pass 1: left constraint
for i in 1..n-1:
    if rating[i] > rating[i-1]:
        candy[i] = candy[i-1] + 1

// Pass 2: right constraint
for i in n-2..0:
    if rating[i] > rating[i+1]:
        candy[i] = max(candy[i], candy[i+1] + 1)
```
**Examples:** LC 135 (Candy), same pattern in LC 42 two-pointer

---

### 2.4 Boyer-Moore Majority Vote
**Trigger:** "Find majority element (appears > n/2 times)" in O(n) time, O(1) space.

**Trick:** Maintain a candidate and a count. When count drops to 0, swap candidate. Pairs of different elements cancel each other; the majority element survives.

```
candidate = nums[0], count = 1
for i in 1..n-1:
    if nums[i] == candidate:
        count += 1
    elif count == 0:
        candidate = nums[i]
        count = 1
    else:
        count -= 1
return candidate
// Works only if majority is guaranteed to exist. Verify in a second pass if needed.
```
**Examples:** LC 169 (Majority Element), LC 229 (Majority Element II — two candidates)

---

### 2.5 Prefix/Suffix Running Extremes
**Trigger:** "Maximum of something to my left AND to my right" — or any property that decomposes into a left scan and a right scan.

**Trick:** One left-to-right pass builds the "left result" array. One right-to-left pass builds the "right result" array. Combine per index.

**Examples:** LC 42, LC 238, LC 2012 (Sum of Beauty in the Array: prefix max and suffix min). General pattern for any "how am I relative to everything on my left/right" question.

---

## Category 3: Complement / Inverse Tricks

---

### 3.1 Two Sum — Check Complement
**Trigger:** "Find two numbers that sum to target" in O(n).

**Trick:** For each element, check if `target - element` is already in a hashmap. Store each element as you go.

```
seen = {}
for i, num in enumerate(nums):
    complement = target - num
    if complement in seen:
        return [seen[complement], i]
    seen[num] = i
```
**Examples:** LC 1 (Two Sum), LC 653 (Two Sum IV on BST), extension to 3Sum (sort + fix one + this)

---

### 3.2 Subarray Sum = k — Prefix Sum + Hashmap
**Trigger:** "Count subarrays with sum equal to k."

**Trick:** Define prefix sum. The number of subarrays ending at j with sum = k equals the number of previous prefix sums equal to `prefixSum[j] - k`. Track counts in a hashmap.

```
count = 0, prefSum = 0
seen = {0: 1}
for num in nums:
    prefSum += num
    count += seen.get(prefSum - k, 0)
    seen[prefSum] = seen.get(prefSum, 0) + 1
return count
```
**Examples:** LC 560 (Subarray Sum Equals K), LC 523 (Continuous Subarray Sum), LC 974 (Divisible by K)

---

### 3.3 Exactly K Distinct — atMost(k) − atMost(k−1)
**Trigger:** "Count subarrays with EXACTLY k distinct elements."

**Trick:** Sliding window naturally handles "AT MOST k distinct." Exactly k = atMost(k) − atMost(k−1).

**Precondition:** the counted quantity must never decrease when the window grows. Distinct counts meet this. The number of odd values in the window (LC 1248) also meets it. A sum meets it only when every value is ≥ 0 (LC 930 has only 0s and 1s). With negative values, use prefix sum + hashmap (3.2).

```
function atMost(k):
    count = 0, left = 0, freq = {}
    for right in range(n):
        freq[arr[right]] = freq.get(arr[right], 0) + 1
        while len(freq) > k:
            freq[arr[left]] -= 1
            if freq[arr[left]] == 0:
                del freq[arr[left]]
            left += 1
        count += right - left + 1
    return count

return atMost(k) - atMost(k - 1)
```
**Examples:** LC 992 (Subarrays with K Different Integers), LC 1248 (Count Number of Nice Subarrays)

---

### 3.4 Count Complement — Total Minus Bad
**Trigger:** "Count valid configurations" where valid is hard to define directly but invalid is easy.

**Trick:** Count total configurations − count invalid configurations.

**Examples:** LC 2364 (Count Number of Bad Pairs: total pairs − good pairs), number of pairs NOT satisfying a property.

---

### 3.5 Inclusion-Exclusion
**Trigger:** "Count elements in A or B" → count(A) + count(B) − count(A and B).

**Trick:** Applies when two sets overlap and you'd otherwise double-count.

**Examples:** LC 1201 (Ugly Number III uses GCD/LCM + inclusion-exclusion), any "union of sets" counting.

---

## Category 4: Mathematical Shortcuts

---

### 4.1 GCD and LCM
**Trigger:** Divisibility, fraction simplification, "how many steps until sync," cyclic problems.

```
function gcd(a, b):
    while b != 0:
        a, b = b, a % b
    return a

lcm(a, b) = a // gcd(a, b) * b   // divide first to avoid overflow
```
**Examples:** LC 1071 (GCD of Strings), LC 365 (Water and Jug — check using GCD), LC 1201 (Ugly Number III)

---

### 4.2 Fast Binary Exponentiation
**Trigger:** "Compute x^n" for large n in O(log n).

**Trick:** Square and halve. If exponent is odd, multiply in one extra factor.

```
function pow(x, n):
    if n == 0: return 1
    if n < 0: return 1 / pow(x, -n)   // Java: store n in a long first; -Integer.MIN_VALUE overflows
    if n % 2 == 0:
        half = pow(x, n // 2)
        return half * half
    return x * pow(x, n - 1)
```
**Examples:** LC 50 (Pow(x, n)), matrix exponentiation for Fibonacci in O(log n)

---

### 4.3 Pigeonhole Principle
**Trigger:** "Prove that a duplicate/collision must exist" or find it.

**Trick:** n+1 items in n slots → at least one slot has two items. No searching needed — existence is guaranteed by pigeonhole.

**Examples:** LC 287 (Find Duplicate Number — n+1 numbers in range [1..n]), LC 41 (First Missing Positive — n numbers, so the answer lies in [1..n+1])

---

### 4.4 Sum / Series Formulas
**Trigger:** Need sum of range without iterating.

```
Sum 1..n         = n*(n+1)/2
Sum of squares   = n*(n+1)*(2n+1)/6
Sum of cubes     = (n*(n+1)/2)^2
Arithmetic sum   = count * (first + last) / 2
Geometric sum    = first * (ratio^count - 1) / (ratio - 1)
```
**Examples:** LC 268 (Missing Number = n*(n+1)/2 − sum(nums)), LC 1512 (Number of Good Pairs)

---

### 4.5 Circular Index via Modulo
**Trigger:** Array treated as circular — index wraps around.

**Trick:** Use `index % n` to wrap. For circular DP, either extend array to length 2n or split into two linear problems.

```
next_index = (current_index + 1) % n

// For circular subarray max:
// Option 1: max subarray of length <= n in arr + arr (length 2n); needs prefix sums + monotonic deque
// Option 2: max(linear_max_subarray, total_sum - linear_min_subarray)
//           if linear_max_subarray < 0 (all values negative), return linear_max_subarray
```
**Examples:** LC 918 (Maximum Sum Circular Subarray), LC 213 (House Robber II — split into two linear)

---

### 4.6 Modular Arithmetic
**Trigger:** "Return answer mod 10^9+7" — result would overflow without modding.

```
MOD = 10**9 + 7

// Always reduce intermediate results:
result = (a + b) % MOD
result = (a * b) % MOD
// Division: use modular inverse (Fermat's little theorem for prime MOD)
inv_b = pow(b, MOD - 2, MOD)   // only for prime MOD
result = (a * inv_b) % MOD
```
**Examples:** LC 1269 (Number of Ways to Stay in Same Place), LC 1220 (Count Vowels Permutation)

---

## Category 5: Data Structure Shortcuts

---

### 5.1 Dutch National Flag — 3-Way Partition
**Trigger:** "Sort array of 3 distinct values (0/1/2, red/white/blue)" in O(n), O(1) space.

**Trick:** Three pointers: `lo` (next 0 position), `mid` (current), `hi` (next 2 position).

```
lo = 0, mid = 0, hi = n - 1

while mid <= hi:
    if arr[mid] == 0:
        swap(arr[lo], arr[mid])
        lo++; mid++
    elif arr[mid] == 2:
        swap(arr[mid], arr[hi])
        hi--          // do NOT increment mid (arr[hi] is unknown)
    else:             // arr[mid] == 1
        mid++
```
**Examples:** LC 75 (Sort Colors), partitioning around a pivot in QuickSort

---

### 5.2 Dummy Sentinel Node for Linked Lists
**Trigger:** Operations that might affect the head node (deletion, insertion before head), causing special-case logic.

**Trick:** Create a dummy node before the head. All operations become uniform (no head-special-casing). Return `dummy.next` as the new head.

```
dummy = new Node(0)
dummy.next = head
curr = dummy

// ... all operations on curr.next ...

return dummy.next
```
**Examples:** LC 19 (Remove Nth Node From End), LC 21 (Merge Two Sorted Lists), LC 82 (Remove Duplicates II)

---

### 5.3 Monotonic Deque for Sliding Window Max/Min
**Trigger:** "Maximum (or minimum) of every window of size k" in O(n).

**Trick:** Maintain a deque of indices in decreasing order (for max). Front = current window's max. Pop front when it slides out; pop back when a new element is ≥ back element.

```
deque = []  // stores indices, front = max index
result = []

for i in range(n):
    // Remove elements outside window
    while deque and deque[0] < i - k + 1:
        deque.pop_front()
    
    // Maintain decreasing order
    while deque and arr[deque[-1]] <= arr[i]:
        deque.pop_back()
    
    deque.append(i)
    
    if i >= k - 1:
        result.append(arr[deque[0]])
```
**Examples:** LC 239 (Sliding Window Maximum), LC 862 (Shortest Subarray with Sum at Least K)

---

### 5.4 Stack for Matching / Nesting
**Trigger:** Matching pairs (parentheses, brackets), "valid nesting", "distance to matching element."

**Trick:** Push open elements. On close element, check or pop from top.

```
stack = []
for char in s:
    if isOpen(char):
        stack.push(char)
    else:
        if stack is empty or not matches(stack.top(), char):
            return false
        stack.pop()
return stack is empty
```
**Examples:** LC 20 (Valid Parentheses), LC 32 (Longest Valid Parentheses), LC 394 (Decode String)

---

### 5.5 Reservoir Sampling
**Trigger:** "Pick k random elements from a stream of unknown length" uniformly.

**Trick:** For element at position i (1-indexed), replace the current reservoir item with probability k/i.

```
// For k=1:
reservoir = stream[0]
for i in 1..n-1:
    j = random(0, i)       // inclusive
    if j == 0:
        reservoir = stream[i]
return reservoir
```
**Examples:** LC 382 (Linked List Random Node), LC 398 (Random Pick Index)

---

### 5.6 Coordinate Compression
**Trigger:** Values are large (up to 10^9) but only a small number of distinct values matter.

**Trick:** Map the O(n) distinct values to indices 0..m-1. Replace all values with their compressed index before processing.

```
sorted_unique = sorted(set(values))
compress = {val: i for i, val in enumerate(sorted_unique)}
compressed = [compress[v] for v in values]
```
**Examples:** LC 315 (Count of Smaller Numbers After Self), LC 327 (Count of Range Sum), segment tree problems

---

## Category 6: Graph / Tree Tricks

---

### 6.1 Floyd's Cycle Entry Point (Two-Phase)
**Trigger:** Found a cycle in a linked list — now find where the cycle begins.

**Trick:** Phase 1: slow/fast meet somewhere inside the cycle. Phase 2: reset one pointer to head. Move both at speed 1. They meet at the cycle entry.

```
// Phase 1: find meeting point
slow = head, fast = head
while fast and fast.next:
    slow = slow.next
    fast = fast.next.next
    if slow == fast: break
if fast is null or fast.next is null: return null   // no cycle

// Phase 2: find entry
slow = head
while slow != fast:
    slow = slow.next
    fast = fast.next
return slow  // cycle entry
```
**Examples:** LC 142 (Linked List Cycle II), LC 287 (Find Duplicate — treat array as linked list)

---

### 6.2 Multi-Source BFS
**Trigger:** "Minimum distance from any source to all nodes" — e.g., nearest 0, nearest rotten orange, nearest gate.

**Trick:** Add ALL source nodes to the BFS queue at the start with distance 0. BFS naturally expands level by level from all sources simultaneously.

```
queue = []
for each source:
    queue.append((source, 0))
    dist[source] = 0

while queue:
    node, d = queue.pop_front()
    for neighbor in graph[node]:
        if dist[neighbor] == UNVISITED:
            dist[neighbor] = d + 1
            queue.append((neighbor, d + 1))
```
**Examples:** LC 994 (Rotting Oranges), LC 286 (Walls and Gates), LC 1162 (As Far from Land as Possible)

---

### 6.3 Tree Diameter Through Any Node
**Trigger:** "Longest path in a tree (not necessarily through root)."

**Trick:** At each node, the best path THROUGH that node = `leftHeight + rightHeight`. Track the global max. Return `max(leftHeight, rightHeight) + 1` upward (only one side can be used by the parent).

```
maxDiameter = 0

function height(node):
    if node is null: return 0
    left = height(node.left)
    right = height(node.right)
    maxDiameter = max(maxDiameter, left + right)  // path through node
    return max(left, right) + 1
```
**Examples:** LC 543 (Diameter of Binary Tree), LC 124 (Binary Tree Maximum Path Sum — use value instead of height)

---

### 6.4 Tree / Graph Serialization with Null Markers
**Trigger:** "Serialize a binary tree to a string and deserialize back."

**Trick:** Preorder traversal; write `null` (or a sentinel like "#") for missing children. Deserialization reads values in the same order, using a pointer/queue to consume them.

```
function serialize(root):
    if root is null: return "null,"
    return str(root.val) + "," + serialize(root.left) + serialize(root.right)

function deserialize(data):
    queue = data.split(",")
    function build():
        val = queue.pop_front()
        if val == "null": return null
        node = new Node(int(val))
        node.left = build()
        node.right = build()
        return node
    return build()
```
**Examples:** LC 297 (Serialize and Deserialize Binary Tree), LC 449 (BST version uses preorder + BST property)

---

### 6.5 Bipartite Check via 2-Colouring
**Trigger:** "Can we divide nodes into two groups with no edge within the same group?" — or "Is this graph bipartite?"

**Trick:** BFS/DFS colour alternation. If any neighbour has the same colour as current node → not bipartite.

```
color = [-1] * n
for start in range(n):
    if color[start] == -1:
        color[start] = 0
        queue = [start]
        while queue:
            node = queue.pop_front()
            for neighbor in graph[node]:
                if color[neighbor] == -1:
                    color[neighbor] = 1 - color[node]
                    queue.append(neighbor)
                elif color[neighbor] == color[node]:
                    return false   // same color = not bipartite
return true
```
**Examples:** LC 785 (Is Graph Bipartite?), LC 886 (Possible Bipartition)

---

### 6.6 0-1 BFS via Deque
**Trigger:** Graph edges have only two possible weights: 0 or 1. Need shortest path faster than Dijkstra.

**Trick:** Use a deque instead of a priority queue. Weight-0 edges go to the **front** (free move). Weight-1 edges go to the **back** (costs one unit).

```
dist = [INF] * n
dist[src] = 0
deque = [(0, src)]

while deque:
    d, node = deque.pop_front()
    if d > dist[node]: continue
    for neighbor, weight in graph[node]:
        if dist[node] + weight < dist[neighbor]:
            dist[neighbor] = dist[node] + weight
            if weight == 0:
                deque.push_front((dist[neighbor], neighbor))
            else:
                deque.push_back((dist[neighbor], neighbor))
```
**Examples:** LC 1368 (Minimum Cost to Make at Least One Valid Path), LC 2290 (Minimum Obstacle Removal to Reach Corner)

---

## Category 7: DP / Counting Tricks

---

### 7.1 DP Offset — dp[i+1] for First i Elements
**Trigger:** DP table where `dp[i]` represents "answer for the first i elements" to avoid off-by-one bounds.

**Trick:** Use a 1-indexed DP array of size n+1. `dp[0]` = base case (empty prefix). `dp[i]` uses `s[i-1]` (0-indexed string character). Eliminates `i-1` indexing bugs and avoids separate base case initialization.

```
dp = [0] * (n + 1)
dp[0] = base_case

for i in 1..n:
    // dp[i] considers the character at s[i-1]
    if condition(s[i-1]):
        dp[i] = dp[i-1] + something
    else:
        dp[i] = other_transition
```
**Examples:** LC 72 (Edit Distance), LC 1143 (LCS), LC 91 (Decode Ways)

---

### 7.2 State Compression with Bitmask
**Trigger:** n ≤ 20 items; problem requires tracking which items have been "used" or "visited."

**Trick:** Use an integer as the state bitmask. `dp[mask]` = answer for the subset encoded by `mask`.

```
dp = [INF] * (1 << n)
dp[0] = 0  // empty set base case

for mask in range(1 << n):
    for i in range(n):
        if (mask >> i) & 1:   // item i is in mask
            prev_mask = mask ^ (1 << i)
            dp[mask] = min(dp[mask], dp[prev_mask] + cost(i, prev_mask))
```
**Examples:** LC 847 (Shortest Path Visiting All Nodes), LC 1284 (Minimum Flips), LC 691 (Stickers to Spell Word)

---

### 7.3 Space Optimization — Reverse Loop for 0/1 Knapsack
**Trigger:** 0/1 knapsack problem — reduce 2D DP to 1D by iterating capacity in reverse.

**Trick:** If you iterate capacity forward, you can pick the same item twice (unbounded knapsack). Reverse iteration ensures you only use each item once.

```
dp = [0] * (capacity + 1)

for item_weight, item_value in items:
    for w in range(capacity, item_weight - 1, -1):  // REVERSE
        dp[w] = max(dp[w], dp[w - item_weight] + item_value)
```
**Examples:** LC 416 (Partition Equal Subset Sum), LC 474 (Ones and Zeroes), LC 494 (Target Sum)

---

### 7.4 DP on Intervals — Fill Order
**Trigger:** "Optimal way to split/merge a range" — burst balloons, matrix chain multiplication, stone merge.

**Trick:** Fill DP by increasing subarray LENGTH. For length `len`, iterate all starting positions `i`; ending position is `j = i + len - 1`. Base case: length 1.

```
for length in range(2, n + 1):
    for i in range(n - length + 1):
        j = i + length - 1
        for k in range(i, j):    // split point
            dp[i][j] = max/min(dp[i][j], dp[i][k] + dp[k+1][j] + combine(i, k, j))
```
**Examples:** LC 312 (Burst Balloons), LC 1000 (Minimum Cost to Merge Stones), LC 516 (Longest Palindromic Subsequence)

---

### 7.5 Counting Complement — "Total − Invalid"
**Trigger:** Counting valid items directly is hard, but counting invalid ones is easy.

**Trick:** total = n*(n-1)/2 for pairs, or total arrangements for other structures. Compute invalid count and subtract.

**Examples:** LC 2364 (Count Number of Bad Pairs), any "count pairs that do NOT satisfy X" → invert.

---

## Quick Lookup Table

| Trigger Phrase | Trick | Category |
|----------------|-------|----------|
| Every number appears twice except one | XOR all | 1.1 |
| Power of 2? O(1) | `(n & (n-1)) == 0 and n > 0` | 1.3 |
| Count 1-bits | Brian Kernighan: `n &= (n-1)` | 1.4 |
| Count subarrays with XOR = k | Prefix XOR + hashmap | 1.7 |
| Water trapped / rain water | Two-pass maxLeft/maxRight | 2.1 |
| Product except self, no division | Two-pass prefix × suffix | 2.2 |
| Both left AND right neighbor constraint | Two-sweep passes | 2.3 |
| Majority element, O(1) space | Boyer-Moore vote | 2.4 |
| Subarray sum = k, count | Prefix sum + hashmap | 3.2 |
| Exactly k distinct in subarray | atMost(k) − atMost(k−1) | 3.3 |
| Find two numbers summing to target | Hashmap complement | 3.1 |
| GCD, step sync, fraction simplify | Euclidean GCD | 4.1 |
| Large exponent mod m | Binary exponentiation | 4.2 |
| n+1 items in n slots, must have duplicate | Pigeonhole | 4.3 |
| Missing number in range 1..n | Total sum − actual sum | 4.4 |
| Sort 0/1/2 in O(n), O(1) | Dutch National Flag | 5.1 |
| Operations may affect head of linked list | Dummy sentinel node | 5.2 |
| Max of every window of size k | Monotonic deque | 5.3 |
| Matching brackets/nesting | Stack push/pop | 5.4 |
| Cycle entry in linked list | Floyd's two-phase | 6.1 |
| Min distance from any of several sources | Multi-source BFS | 6.2 |
| Longest path in tree | DFS: left + right heights | 6.3 |
| Only 0 or 1 edge weights, shortest path | 0-1 BFS deque | 6.6 |
| n ≤ 20, track visited items | Bitmask DP | 7.2 |
| 0/1 knapsack in 1D | Reverse loop | 7.3 |
| Optimal range split/merge | Interval DP, fill by length | 7.4 |
| Counting valid ≈ total − invalid | Complement count | 7.5 |

---

*Cross-reference: the [cheatsheet.md](./cheatsheet.md) has a compact version of this table.*
