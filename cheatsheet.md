# Ultimate Problem-Solving Cheatsheet

Print this and keep it handy during practice sessions.

---

## 🎯 Pattern Recognition Matrix

| If you see... | Think... | Complexity |
|---------------|----------|------------|
| Contiguous subarray/substring | **Sliding Window** | O(n) |
| Sorted array + pairs | **Two Pointers** | O(n) |
| Linked list + cycle/middle | **Fast & Slow Pointers** | O(n) |
| Range sum queries | **Prefix Sum** | O(1) per query |
| Find pair/check existence | **Hash Map** | O(1) lookup |
| Min/max that satisfies condition | **Binary Search on Answer** | O(n log X) |
| Next greater/smaller element | **Monotonic Stack** | O(n) |
| All permutations/combinations | **Backtracking** | O(n!) or O(2^n) |
| Tree height/path/validation | **Tree DFS** | O(n) |
| Level-order/min depth | **Tree BFS** | O(n) |
| Connected components | **Graph DFS/BFS or Union Find** | O(V+E) |
| Dependencies/ordering | **Topological Sort** | O(V+E) |
| Dynamic connectivity | **Union Find** | O(α(n)) ≈ O(1) |
| Greedy seems right | **Greedy** (verify it works!) | Usually O(n log n) |
| Count ways/min-max with choices | **Dynamic Programming** | O(states × transitions) |
| k + optimal (smallest/largest/closest) | **Heap / Priority Queue** | O(n log k) |
| Running median / two-group order stats | **Two Heaps** (max-heap + min-heap) | O(n log n) |
| Prefix / autocomplete / wildcard words | **Trie** | O(L) per query |
| Weighted shortest path | **Dijkstra** (non-neg) / **Bellman-Ford** (neg/K-hops) | O((V+E) log V) |
| All-pairs shortest path, n ≤ 100 | **Floyd-Warshall** | O(n³) |
| Find single/missing number, O(1) space | **XOR** | O(n) |
| n ≤ 20, track visited subset | **Bitmask DP** | O(n × 2^n) |

---

## ⏱️ Constraint → Complexity Guide

| n ≤ | Target Complexity | Likely Approach |
|-----|-------------------|-----------------|
| 10 | O(n!) | Backtracking, brute force |
| 20 | O(2^n) | Bitmask DP, backtracking |
| 100 | O(n³) | 3D DP, Floyd-Warshall |
| 1,000 | O(n²) | 2D DP, nested loops |
| 10,000 | O(n²) or O(n√n) | Optimized DP |
| 100,000 | O(n log n) | Sorting, binary search |
| 1,000,000 | O(n) | Linear scan, sliding window |
| 10^8+ | O(log n) or O(1) | Math, binary search |

---

## ⚡ At-a-Glance Tricks

> Full explanations with templates: see [tricks-at-a-glance.md](./tricks-at-a-glance.md)

| Trigger | Trick | Example LC # |
|---------|-------|-------------|
| Every number appears twice except one | XOR all elements | 136 |
| Is n a power of 2? | `n > 0 && (n & (n-1)) == 0` | 231 |
| Count 1-bits in integer | Brian Kernighan: `n &= (n-1)` in loop | 191 |
| Subarray XOR = k, count | Prefix XOR + hashmap (like prefix sum) | 1310 |
| Subarray sum = k, count | Prefix sum + hashmap | 560 |
| Exactly k distinct, count subarrays | atMost(k) − atMost(k−1) | 992 |
| Find two numbers summing to target | Hashmap: check `target - x` | 1 |
| Every number appears twice except TWO | XOR all → find differing bit → partition | 260 |
| Majority element, O(1) space | Boyer-Moore: candidate + cancel-out count | 169 |
| Sort 0/1/2 in-place, O(n) | Dutch National Flag: lo/mid/hi pointers | 75 |
| Water trapped between bars | Two-pass: maxLeft[i] and maxRight[i] precomputed | 42 |
| Product except self, no division | Two-pass: prefix products × suffix products | 238 |
| Both left AND right neighbor rule | Two-sweep (left→right then right→left) | 135 |
| Operations may delete/change head node | Dummy sentinel node before head | 19 |
| Max of every window of size k | Monotonic deque (front = window max) | 239 |
| Cycle entry in linked list | Floyd two-phase: meet → reset one to head | 142 |
| Nearest source from multiple start points | Multi-source BFS: add all sources at t=0 | 994 |
| Longest path in binary tree | DFS: left_h + right_h at each node | 543 |
| 0 or 1 edge weights, shortest path | 0-1 BFS via deque (0→front, 1→back) | 1368 |
| 0/1 knapsack, reduce 2D DP to 1D | Reverse inner loop (high→low capacity) | 416 |
| Optimal range split (burst, chain mult) | Interval DP: fill by increasing length | 312 |
| GCD / divisibility / step sync | Euclidean GCD: `gcd(a,b) = gcd(b, a%b)` | 1071 |
| Large exponent mod m | Binary exponentiation (square and halve) | 50 |
| n+1 items in [1..n], duplicate exists | Pigeonhole — guaranteed, no search needed | 287 |

---

## 🔧 Quick Templates

### Sliding Window (Variable)
```
left = 0
for right in range(n):
    # expand: add arr[right]
    while invalid():
        # shrink: remove arr[left]
        left += 1
    # update result with window [left, right]
```

### Two Pointers (Opposite)
```
left, right = 0, n-1
while left < right:
    if condition:
        left += 1
    else:
        right -= 1
```

### Binary Search
```
lo, hi = min_possible, max_possible
while lo < hi:
    mid = lo + (hi - lo) // 2
    if can_achieve(mid):
        hi = mid  # for first true
    else:
        lo = mid + 1
return lo
```

### BFS
```
queue = [start]
visited = {start}
while queue:
    node = queue.pop(0)
    for neighbor in graph[node]:
        if neighbor not in visited:
            visited.add(neighbor)
            queue.append(neighbor)
```

### DFS
```
def dfs(node, visited):
    if node in visited:
        return
    visited.add(node)
    for neighbor in graph[node]:
        dfs(neighbor, visited)
```

### Backtracking
```
def backtrack(state):
    if is_complete(state):
        result.append(state.copy())
        return
    for choice in choices:
        if is_valid(choice):
            state.add(choice)
            backtrack(state)
            state.remove(choice)  # undo
```

### Topological Sort (Kahn's)
```
in_degree = [0] * n
# build in_degree from edges
queue = [i for i in range(n) if in_degree[i] == 0]
order = []
while queue:
    node = queue.pop(0)
    order.append(node)
    for neighbor in graph[node]:
        in_degree[neighbor] -= 1
        if in_degree[neighbor] == 0:
            queue.append(neighbor)
return order if len(order) == n else []  # cycle if not all included
```

### Union Find
```
def find(x):
    if parent[x] != x:
        parent[x] = find(parent[x])  # path compression
    return parent[x]

def union(x, y):
    px, py = find(x), find(y)
    if px != py:
        parent[px] = py
```

---

## 📝 DP State Definition Cheat Sheet

| Problem Type | State |
|--------------|-------|
| Linear array | `dp[i]` = answer for first i elements |
| Two arrays/strings | `dp[i][j]` = answer for s1[0:i] and s2[0:j] |
| Interval/range | `dp[i][j]` = answer for range [i, j] |
| Knapsack | `dp[i][w]` = answer using first i items with capacity w |
| Grid | `dp[i][j]` = answer to reach cell (i, j) |
| State machine | `dp[i][state]` = answer at position i in given state |

---

## 🐛 Common Bug Checklist

- [ ] Off-by-one errors (< vs <=, start index)
- [ ] Integer overflow (use long for intermediate results)
- [ ] Empty input handling
- [ ] Null pointer checks
- [ ] Visited set for graphs (avoid infinite loops)
- [ ] Copy vs reference when storing results
- [ ] Reverse order for 0/1 knapsack space optimization
- [ ] Base case initialization in DP
- [ ] Duplicate handling (sort + skip)
- [ ] Boundary checks for arrays/grids

---

## 🎤 Interview Phrases

**Starting:**
> "Let me make sure I understand..."
> "What are the constraints?"
> "Can the input be empty/negative/unsorted?"

**Thinking:**
> "The brute force would be O(n²), but I think we can do better..."
> "This looks like a [pattern] problem because..."
> "Let me think about what invariant I need to maintain..."

**Coding:**
> "I'll start with the helper function..."
> "This variable tracks..."
> "Let me add a comment here for clarity..."

**Testing:**
> "Let me trace through the example..."
> "What about edge cases like..."
> "I'll verify my boundary conditions..."

**When Stuck:**
> "I'm stuck on X, let me think about what property could help..."
> "Could I get a small hint about the direction?"
> "Let me try a simpler version first..."

---

## 🧠 Mental Models

### Sliding Window
```
[-----window-----]
     →→→→→→→→→→→→   expands
←←←←←              shrinks when invalid
```

### Two Pointers
```
[L→         ←R]   opposite direction
[S→    F→→→→→]    same direction (fast/slow)
```

### Binary Search on Answer
```
Invalid | Valid
--------|-------
   ↓
   Find boundary
```

### Monotonic Stack
```
Stack: [2, 5, 8]  ← increasing (for next smaller)
New element 3:
- Pop 8 (3 is next smaller for 8)
- Pop 5 (3 is next smaller for 5)
- Push 3: [2, 3]
```

### BFS Levels
```
Level 0: [start]
Level 1: [neighbors of start]
Level 2: [neighbors of level 1]
...
→ First time reaching target = shortest path
```

---

## 📊 Space-Time Tradeoffs

| Scenario | Trade-off |
|----------|-----------|
| Hash map for lookups | O(n) space → O(1) lookup |
| Memoization | O(states) space → avoid recomputation |
| Precompute prefix sum | O(n) space → O(1) range queries |
| Sort then two pointers | O(n log n) time → O(1) space (vs hash map) |
| Rolling array DP | Lose ability to reconstruct path → save space |

---

## 🚀 Quick Wins for Common Problems

| Problem | Key Insight |
|---------|-------------|
| Two Sum | Hash map: check complement |
| 3Sum | Sort + fix one + two pointers |
| Valid Parentheses | Stack: push open, pop for close |
| Merge Intervals | Sort by start, extend end |
| LRU Cache | HashMap + Doubly Linked List |
| Word Break | DP: dp[i] = can form s[0:i] |
| Number of Islands | DFS/BFS, mark visited |
| Course Schedule | Topological sort (cycle = impossible) |
| Coin Change | DP: dp[amount] = min coins |
| Binary Tree Max Path | DFS, track global max |

---

## 🔗 Pattern Combinations (Hard Problems)

| Combination | When to Use | Classic Problem |
|-------------|-------------|-----------------|
| Prefix Sum + HashMap | Subarray sum = k | LC 560 Subarray Sum Equals K |
| Binary Search + Greedy | Minimize max / Maximize min | LC 410 Split Array Largest Sum |
| DFS + Memoization | Grid paths with caching | LC 329 Longest Increasing Path |
| Sliding Window + HashMap | Substring with frequency constraint | LC 76 Minimum Window Substring |
| Sorting + Two Pointers | k-Sum problems | LC 15 3Sum |
| Monotonic Stack + Math | Contribution to all subarrays | LC 907 Sum of Subarray Minimums |
| BFS + Bitmask | Shortest path with state | LC 864 Shortest Path to Get All Keys |
| Union Find + HashMap | Group by connectivity | LC 721 Accounts Merge |
| Trie + DFS | Multi-word search in grid | LC 212 Word Search II |
| Heap + Sorting + Greedy | Scheduling / allocation | LC 253 Meeting Rooms II |

**Key Insight:** Hard problems = 2-3 patterns combined. Identify each pattern first!

---

## Final Reminder

1. **Don't panic.** Every problem has a solution.
2. **Start with brute force.** It gives you a baseline.
3. **Look for patterns.** Most problems fit known patterns.
4. **Think aloud.** Interviewer can't help if they don't know your thinking.
5. **Test your code.** Bugs are expected; finding them shows skill.

---

*You've prepared well. Trust your preparation. Good luck! 🌟*
