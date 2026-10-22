# Pattern 18: Shortest Paths

## 1. Pattern Name
**Shortest Paths** (Dijkstra · Bellman-Ford · Floyd-Warshall · 0-1 BFS)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Shortest-path problems are about **finding the least-cost route between nodes in a graph**, where "cost" might be distance, time, price, effort — or anything else attached to edges.

**The Key Insight:**  
There is no single "shortest-path algorithm." There are four main algorithms, each built for a different class of graph. The interview skill is recognising *which one applies* before you write a single line of code.

Think of it like a **travel booking engine**:
- **Dijkstra** = direct flights, all non-negative prices. Greedily book the cheapest next leg.
- **Bellman-Ford** = some airlines have "rebate" legs (negative weights), or the itinerary is capped at K connections. Simulate every round.
- **Floyd-Warshall** = you need the cheapest fare between *every* pair of cities and the city list is small. Precompute all at once.
- **0-1 BFS** = every edge costs either 0 or 1 — skip the heap entirely and use a deque.

### The Greedy vs. Relaxation vs. DP Mental Model:

| Algorithm | Strategy | Key Data Structure |
|-----------|----------|--------------------|
| Dijkstra | Greedy: always expand the globally cheapest known node | Min-heap (priority queue) |
| Bellman-Ford | Relaxation: repeat over all edges V-1 times | Plain array (no heap needed) |
| Floyd-Warshall | DP over intermediates: dp[i][j] via node k | 2-D array |
| 0-1 BFS | Deque: 0-weight → front, 1-weight → back | Double-ended queue |

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Minimum cost / distance / time / effort to reach"
- "Cheapest / shortest path from source to destination"
- "Network delay", "signal propagation time"
- "At most K stops / hops / edges"
- "Minimum number of operations / flips to reach"
- "All pairs distances" or "cities within threshold"
- "Probability of reaching" (max-probability → flip to log-space min-cost Dijkstra)

### Constraint Hints:
- **n ≤ 100, asking for all-pairs** → Floyd-Warshall is viable (O(n³) = 10^6 ops)
- **"At most K stops"** → Bellman-Ford limited to K+1 rounds
- **Weighted grid** → Dijkstra treating each cell as a node
- **All edge weights ∈ {0, 1}** → 0-1 BFS (faster than Dijkstra in practice)
- **Negative weights or negative-cycle detection** → Bellman-Ford

### Input/Output Hints:
- Input: Edge list, adjacency list, or weighted grid
- Output: Integer distance, -1 if unreachable, or a reconstructed path

### RED FLAGS — NOT a straightforward shortest-path problem:
- "Longest path" in a general graph (NP-Hard; only easy in DAGs via topological sort)
- Unweighted graph asking for minimum hops → plain BFS is sufficient (do NOT over-engineer)
- "Number of paths" → count DP, not shortest-path
- "Minimum spanning tree" → Prim/Kruskal, not Dijkstra ([Pattern 24](../24-advanced-graphs/README.md))

---

## 4. Brute Force Thought Process

### Dijkstra Brute Force (without heap):
```
Problem: Find shortest distance from src to all nodes in a weighted graph.

Brute Force Thinking:
- Maintain dist[] array, all initialised to INF, dist[src] = 0
- Repeat V times:
  - Pick the unvisited node u with the smallest dist[u]  (O(V) scan)
  - Mark u as visited
  - For each neighbor v of u:
    - If dist[u] + weight(u,v) < dist[v]: update dist[v]

Time:  O(V²)          acceptable when V ≤ 10³
Space: O(V)
```

### Why V² is Too Slow for Large Sparse Graphs:
If V = 10^5, we do 10^10 operations. The inner scan for the minimum dist is the bottleneck.

**The Waste:** Scanning all nodes to find the minimum when only a handful of distances changed.  
Using a min-heap reduces that scan from O(V) to O(log V).

### Bellman-Ford Brute Force (already its own algorithm):
```
Problem: Shortest path with possible negative weights.

Brute Force Thinking:
- A shortest path visits at most V-1 edges (no repeated nodes)
- Run V-1 relaxation rounds over ALL edges
- In each round: for every edge (u,v,w): if dist[u]+w < dist[v]: relax

Time:  O(V * E)
Space: O(V)
```

There is no smarter approach for negative-weight graphs — Bellman-Ford is near-optimal for this class.

---

## 5. Optimization Insight (The "Aha" Moment)

### Dijkstra Insight — "Process Cheapest First":
**"If I always extend the node with the globally smallest known distance, I will never need to revise it later."**

This is the greedy correctness argument: because all edge weights are non-negative, any future path through another node will only be longer. Once a node is popped from the min-heap with distance d, d is final.

This insight transforms O(V²) → O((V+E) log V) by replacing the linear scan with a heap.

**Why it breaks with negative weights:**  
A negative-weight edge encountered later could create a cheaper path to a node already "finalised." The greedy guarantee collapses.

### Bellman-Ford Insight — "Relax Layer by Layer":
**"The shortest path from src to any node using at most k edges can be derived from the shortest paths using at most k-1 edges."**

This is pure DP. Each round propagates shortest-path knowledge one hop further. After V-1 rounds, all paths of length ≤ V-1 (the maximum possible in a simple path) are optimal.

**Bonus:** If any distance still decreases in round V, a negative cycle exists — you can detect it for free.

**K-stops twist:** Run only K+1 rounds instead of V-1. But critically: use a *copy* of the previous round's dist array when relaxing, so you don't propagate multiple hops in a single round.

### Floyd-Warshall Insight — "Try Every Intermediate Node":
**"The shortest path from i to j either doesn't use node k, or goes i → k → j."**

```
dp[i][j] = min(dp[i][j],  dp[i][k] + dp[k][j])
```

Iterate k from 0 to n-1 in the outermost loop. The order matters — when processing intermediate k, all paths using intermediates 0..k-1 are already optimal.

This reduces all-pairs shortest paths to a clean O(n³) DP — no graph traversal needed at all.

### 0-1 BFS Insight — "Deque as a Poor Man's Heap":
**"With only two edge weights (0 and 1), the heap degenerates — we just need to keep 0-cost extensions at the front."**

A standard deque suffices: push weight-0 neighbours to the front (they don't increase cost, so they should be processed next), push weight-1 neighbours to the back. This achieves the same ordering guarantee as Dijkstra but in O(V+E) rather than O((V+E) log V).

---

## 6. Generalized Templates

### Template 1: Dijkstra's Algorithm
```
function dijkstra(graph, src, V):
    dist = array of size V, all set to INF
    dist[src] = 0

    minHeap = min-priority-queue                  // ordered by (distance, node)
    minHeap.push( (0, src) )

    while minHeap is not empty:
        (d, u) = minHeap.pop()                    // cheapest unvisited node

        // CRITICAL: skip stale heap entries
        if d > dist[u]:
            continue

        for each neighbour v with edge weight w in graph[u]:
            newDist = dist[u] + w
            if newDist < dist[v]:
                dist[v] = newDist
                minHeap.push( (newDist, v) )

    return dist          // dist[i] = shortest distance from src to i
                         // dist[i] == INF means i is unreachable
```

**Key invariants:**
- `dist[src] = 0`, all others `INF` before the heap loop begins.
- The `if d > dist[u]: continue` guard discards stale entries (same node may be in heap multiple times).
- Never push a `visited[]` array — the stale-entry guard is sufficient and simpler.

**Time:** O((V+E) log V)  
**Space:** O(V+E)  
**Constraint:** Edge weights must be non-negative.

---

### Template 2: Bellman-Ford (Standard — All Shortest Paths)
```
function bellmanFord(edges, src, V):
    // edges = list of (u, v, weight)
    dist = array of size V, all set to INF
    dist[src] = 0

    // Relax all edges V-1 times
    for round = 1 to V-1:
        updated = false
        for each edge (u, v, w) in edges:
            if dist[u] != INF and dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
                updated = true
        if not updated:
            break                             // early exit if stable

    // Optional: detect negative cycle
    for each edge (u, v, w) in edges:
        if dist[u] != INF and dist[u] + w < dist[v]:
            return ERROR "negative cycle detected"

    return dist
```

**Time:** O(V × E)  
**Space:** O(V)

---

### Template 3: Bellman-Ford (K-Hop Variant — "At Most K Stops")
```
function bellmanFordKHops(edges, src, dst, n, K):
    // edges = list of (from, to, price)
    // At most K stops = at most K+1 edges
    dist = array of size n, all set to INF
    dist[src] = 0

    for round = 1 to K+1:                    // K+1 rounds = K stops
        // CRITICAL: copy dist BEFORE this round's relaxations
        //           to prevent multi-hop propagation in one round
        temp = copy of dist

        for each edge (u, v, w) in edges:
            if dist[u] != INF and dist[u] + w < temp[v]:
                temp[v] = dist[u] + w

        dist = temp                           // commit this round's changes

    if dist[dst] == INF:
        return -1
    return dist[dst]
```

**Why the copy is mandatory:**  
Without it, a single round might chain: src → A → B → C, effectively using 3 edges in one pass.  
The copy ensures each round can only extend paths by exactly one edge.

**Time:** O(K × E)  
**Space:** O(V)

---

### Template 4: Floyd-Warshall (All-Pairs Shortest Paths)
```
function floydWarshall(n, edges):
    // n = number of nodes (0-indexed)
    dp = n x n matrix, all set to INF

    // Base cases
    for i = 0 to n-1:
        dp[i][i] = 0                          // distance to self is 0

    for each edge (u, v, w) in edges:
        dp[u][v] = min(dp[u][v], w)           // keep the cheapest parallel edge; a positive self-loop cannot replace 0
        // If undirected: dp[v][u] = min(dp[v][u], w)

    // CRITICAL: intermediate node k must be the OUTERMOST loop
    for k = 0 to n-1:                         // try each node as intermediate
        for i = 0 to n-1:
            for j = 0 to n-1:
                if dp[i][k] != INF and dp[k][j] != INF:
                    dp[i][j] = min(dp[i][j], dp[i][k] + dp[k][j])

    // Optional: detect negative cycle
    for i = 0 to n-1:
        if dp[i][i] < 0:
            return ERROR "negative cycle"

    return dp           // dp[i][j] = shortest distance from i to j
```

**Why k must be outermost:**  
When we process intermediate k, we need dp[i][k] and dp[k][j] to already reflect all paths using intermediates 0..k-1. Swapping the loop order breaks this guarantee.

**Time:** O(n³)  
**Space:** O(n²)  
**Sweet spot:** n ≤ 100–200 (10^6 to 8×10^6 operations).

---

### Template 5: 0-1 BFS (Deque-Based Shortest Path)
```
function zeroOneBFS(graph, src, V):
    // graph[u] = list of (v, weight) where weight ∈ {0, 1}
    dist = array of size V, all set to INF
    dist[src] = 0

    deque = double-ended queue
    deque.pushFront(src)

    while deque is not empty:
        u = deque.popFront()

        for each (v, w) in graph[u]:
            if dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
                if w == 0:
                    deque.pushFront(v)        // 0-cost: process before others
                else:
                    deque.pushBack(v)         // 1-cost: process after current level

    return dist
```

**Intuition behind pushFront vs pushBack:**  
A 0-weight edge is "free" — the neighbour is just as cheap as the current node and should be explored next. A 1-weight edge costs something — defer it like BFS defers the next level.

**Time:** O(V+E)  
**Space:** O(V)

---

## 7. Variations of the Pattern

### Variation A: Shortest Path in a Weighted Grid
**Problem Type:** "Find minimum cost path from top-left to bottom-right" where each cell has a cost.  
**Reduction:** Treat each cell (r, c) as a node. Edges connect adjacent cells with the adjacent cell's cost (or edge cost depending on problem phrasing).  
**Algorithm:** Dijkstra. Grid size n×m → V = n×m, E = 4×n×m.  
**Examples:** LC 1631 (minimum effort), LC 1368 (0-1 BFS on grid).

### Variation B: Maximum Probability Path (Dijkstra in Disguise)
**Problem Type:** "Find the path with maximum probability of success."  
**Reduction:** Multiply probabilities along a path (instead of summing weights). Use a **max-heap** and keep the maximum probability to each node.  
**Key observation:** Probabilities ∈ [0, 1] are non-negative → Dijkstra applies.  
**Example:** LC 1514.

### Variation C: Shortest Path Visiting All Nodes (BFS + Bitmask)
**Problem Type:** "Find shortest path that visits every node at least once."  
**State:** `(node, visitedMask)` — can revisit nodes.  
**Algorithm:** BFS over the state space (unweighted graph, so BFS is optimal).  
**State space:** V × 2^V → feasible only for V ≤ ~20.  
**Example:** LC 847 (V ≤ 12, so 12 × 4096 = 49152 states — fine for BFS).

### Variation D: Restricted Dijkstra (Custom Comparator)
**Problem Type:** Two-dimensional cost (minimize time, break ties by cost, etc.)  
**Approach:** Push `(primary_cost, secondary_cost, node)` tuples into the heap. The template is identical; only the comparison changes.  
**Example:** Problems with "minimize fuel, then minimize distance."

### Variation E: Negative Weights → Shift + Dijkstra?
**Common misconception:** "Just add a constant to all weights to make them non-negative."  
**Why it fails:** Adding a constant c to every edge inflates paths with more edges disproportionately. A path with 3 edges gains 3c, but a path with 1 edge gains only c, so the cheapest path can change.  
**Correct approach:** Use Bellman-Ford whenever weights can be negative.

### Variation F: Bidirectional Dijkstra
**Problem Type:** Single source to single destination on a very large graph.  
**Approach:** Run Dijkstra simultaneously from source and destination; stop when the frontiers meet.  
**Practical gain:** Explores roughly half the graph in many real-world networks (used in map routing engines).  
**Interview note:** Rarely required in LeetCode-style interviews, but good to mention as an optimization.

---

## 8. Classic Interview Problems

### MEDIUM (Interview Favorites)
| Problem | Algorithm | Key Insight | LC # |
|---------|-----------|-------------|------|
| **Network Delay Time** | Dijkstra | SSSP; return max of dist[] | 743 |
| **Cheapest Flights Within K Stops** | Bellman-Ford (K+1 rounds) | Copy dist each round; K stops = K+1 edges | 787 |
| **Find City With Smallest Neighbor Count** | Floyd-Warshall | All-pairs; n ≤ 100 | 1334 |
| **Path With Minimum Effort** | Dijkstra on grid | State = cell; cost = max edge along path | 1631 |
| **Path With Maximum Probability** | Dijkstra (max-heap) | Multiply probabilities; max-heap instead of min | 1514 |

### HARD (Differentiators)
| Problem | Algorithm | Key Insight | LC # |
|---------|-----------|-------------|------|
| **Minimum Cost to Make Valid Path** | 0-1 BFS on grid | Following existing arrow = 0 cost, changing it = 1 | 1368 |
| **Shortest Path Visiting All Nodes** | BFS + bitmask DP | State = (node, visitedMask); BFS for unweighted | 847 |

### Algorithm Selection by Problem:
| Constraint | Algorithm | Why |
|------------|-----------|-----|
| Unweighted graph | BFS | Each edge = 1; BFS levels = distances |
| Weighted, all non-negative | Dijkstra | Greedy; heap gives O((V+E) log V) |
| Negative weights | Bellman-Ford | Greedy breaks; must relax all edges |
| "At most K stops" | Bellman-Ford (K+1 rounds) | Natural round-by-round structure |
| All-pairs AND n ≤ 100 | Floyd-Warshall | O(n³) is affordable; trivial to code |
| Weights only 0 or 1 | 0-1 BFS | Deque replaces heap; O(V+E) |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Dijkstra Mastery
**Day 1–2:**
- Implement Dijkstra from scratch on LC 743 (Network Delay Time)
- Focus on: heap tuple ordering, stale-entry guard, INF initialisation

**Day 3–4:**
- Solve LC 1631 (Path With Minimum Effort) — grid Dijkstra
- Solve LC 1514 (Path With Maximum Probability) — max-heap variant
- Focus on: adapting the template to different cost functions

**Day 5:**
- Review and time yourself — can you write the full Dijkstra template in under 5 minutes?

### Week 2: Bellman-Ford and Floyd-Warshall
**Day 1–2:**
- Solve LC 787 (Cheapest Flights Within K Stops)
- Write both the naive Bellman-Ford and the K-hop copy-based variant
- Focus on: understanding *why* the dist copy is mandatory

**Day 3:**
- Solve LC 1334 (Find the City With the Smallest Number of Neighbors)
- Implement Floyd-Warshall; deliberately swap the k loop to the inner position and watch it break
- Focus on: loop order correctness

**Day 4–5:**
- Solve LC 1368 (Minimum Cost to Make at Least One Valid Path) — 0-1 BFS
- Focus on: deque discipline (pushFront vs pushBack)

### Week 3: Integration and Hard Problems
**Day 1–3:**
- Solve LC 847 (Shortest Path Visiting All Nodes) — BFS + bitmask
- Focus on: state compression; unweighted BFS over a state-space graph

**Day 4–5:**
- Revisit all five problems without looking at notes
- Timed practice: 35 minutes per problem

### Practice Methodology:
1. **First 3 minutes:** Identify which algorithm applies using the decision tree at the bottom.
2. **Next 5 minutes:** Confirm the constraints (V, E, weight range, source/destination type).
3. **Next 7 minutes:** Write the pseudocode template, then adapt it to the specific cost function.
4. **Remaining time:** Implement, trace through the example, check edge cases.
5. **After solving:** Derive the time complexity from first principles — don't just recite it.

### Self-Check Questions:
- Can I explain *why* Dijkstra fails on negative weights with a concrete counterexample?
- Can I explain *why* the dist-copy is mandatory in K-hop Bellman-Ford?
- Can I recite the Floyd-Warshall loop order (k outermost) and justify it?
- Do I know when BFS is strictly better than Dijkstra (unweighted, or 0-1 weights)?
- Can I write Dijkstra's stale-entry guard without looking it up?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Using Dijkstra on a Graph with Negative Weights
```
// WRONG: Dijkstra finalises dist[u] when u is popped.
// With negative weights, a cheaper path may arrive later.

// Example: src=0, edges: 0→1 (weight 2), 0→2 (weight 3), 2→1 (weight -2)
// Dijkstra pops node 1 with dist=2 first (2 < 3), before seeing 2→1 with -2
// Correct dist[1] = 3 + (-2) = 1, but a Dijkstra that finalises on pop keeps 2. WRONG.
// (Template 1 has no visited[] array, so it re-pushes node 1 and recovers here,
//  but with negative edges its running time can grow exponentially.)

// CORRECT: Use Bellman-Ford whenever any edge weight is negative.
```

### Mistake 2: Forgetting to Copy the dist Array in K-Hop Bellman-Ford
```
// WRONG: Mutating dist directly allows chaining within one round.
for round = 1 to K+1:
    for each edge (u, v, w):
        if dist[u] + w < dist[v]:
            dist[v] = dist[u] + w       // BUG: dist[u] may already be updated
                                         //       this round → multi-hop in one pass

// CORRECT: snapshot dist before the round, write updates into the snapshot
for round = 1 to K+1:
    temp = copy of dist
    for each edge (u, v, w):
        if dist[u] != INF and dist[u] + w < temp[v]:   // read from dist (old), write to temp (new)
            temp[v] = dist[u] + w
    dist = temp
```

### Mistake 3: Missing the Stale-Entry Guard in Dijkstra
```
// WRONG: Processing every popped entry, even outdated ones.
while heap is not empty:
    (d, u) = heap.pop()
    for each (v, w) in graph[u]:
        ...
// A node can be pushed multiple times. Without the guard,
// you reprocess it with a stale (larger) distance and rescan
// its edges. With non-negative weights the answer stays
// correct, but the extra scans break the O((V+E) log V) bound.

// CORRECT: Skip stale entries immediately after pop.
while heap is not empty:
    (d, u) = heap.pop()
    if d > dist[u]:                      // stale entry — skip it
        continue
    for each (v, w) in graph[u]:
        ...
```

### Mistake 4: Wrong Loop Order in Floyd-Warshall
```
// WRONG: k (intermediate) in the inner loop.
for i = 0 to n-1:
    for j = 0 to n-1:
        for k = 0 to n-1:               // BUG: dp[i][k] and dp[k][j] not yet
            dp[i][j] = min(dp[i][j],    //      fully computed for intermediates 0..k-1
                           dp[i][k] + dp[k][j])

// CORRECT: k must be outermost so all paths through 0..k-1
//          are finalised before we try k as an intermediate.
for k = 0 to n-1:                       // outermost: try each intermediate
    for i = 0 to n-1:
        for j = 0 to n-1:
            if dp[i][k] != INF and dp[k][j] != INF:     // INF + INF overflows
                dp[i][j] = min(dp[i][j], dp[i][k] + dp[k][j])
```

### Mistake 5: Not Initialising dp[i][i] = 0 in Floyd-Warshall
```
// WRONG: Leaving dp[i][i] = INF.
// When the algorithm tries path i → k → i, it sees INF+INF = overflow.
// Also, negative-cycle detection (dp[i][i] < 0) only works if the
// baseline is 0, not INF.

// CORRECT:
for i = 0 to n-1:
    dp[i][i] = 0                         // distance from any node to itself is 0
```

### Mistake 6: Using 0-1 BFS with pushFront/pushBack Reversed
```
// WRONG: 0-cost edges to the back, 1-cost edges to the front.
if w == 0:
    deque.pushBack(v)                    // BUG: 0-cost deferred; more-expensive
else:                                    //      nodes processed first
    deque.pushFront(v)

// CORRECT:
if w == 0:
    deque.pushFront(v)                   // 0-cost: same "level" as current node
else:
    deque.pushBack(v)                    // 1-cost: next "level"
```

### Mistake 7: Returning Wrong Value When Destination Is Unreachable
```
// WRONG: Returning INF directly (may cause integer overflow downstream).
return dist[dst]

// CORRECT: Map INF to the problem's sentinel.
if dist[dst] == INF:
    return -1                            // or 0.0 for probability problems
return dist[dst]
```

### Mistake 8: Applying Floyd-Warshall to Graphs with n > 200
```
// Floyd-Warshall is O(n³).
// n = 500  → 1.25 × 10^8 ops  (borderline, often TLE in Java/Python)
// n = 1000 → 10^9 ops          (definitely TLE)

// CORRECT decision: If n > 200 and you need all pairs with non-negative
// weights, run Dijkstra from each source: O(V * (V+E) log V).
// If edges are sparse, this is far faster than Floyd-Warshall.
// If you only need one source, run Dijkstra once.
```

---

## Bonus: Interview Script for Shortest Path Problems

When you recognise a shortest-path problem, here is how to communicate it:

> "I see a graph with weighted edges and a reachability or minimum-cost question, so this is a shortest-path problem. Let me first check the constraints to pick the right algorithm.
>
> Are all edge weights non-negative? Then Dijkstra with a min-heap gives O((V+E) log V).  
> Are there negative weights, or is there a 'K stops' limit? Then Bellman-Ford is the right choice.  
> Do we need all-pairs distances and is n small (≤ 100)? Floyd-Warshall runs in O(n³).  
> Are all edge weights 0 or 1? Then 0-1 BFS is optimal at O(V+E).
>
> In this problem, [state your observation about constraints]. So I'll use [algorithm].
>
> Let me trace through the small example to confirm the algorithm gives the right answer before I write the full implementation..."

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Shortest path → Dijkstra | BFS, 0-1 BFS, or Bellman-Ford | What are the edge weights? None (all equal) → BFS. Only 0 and 1 → 0-1 BFS. Non-negative → Dijkstra. Any negative weight → Bellman-Ford. |
| Cheapest route with "at most K stops" | Bellman-Ford with K+1 rounds | Does the problem limit the number of edges? Yes → Bellman-Ford. Dijkstra keyed by node alone fails; it works only if the state is (node, stops used). |
| "Minimum cost to connect all cities" | MST: Kruskal or Prim ([Pattern 13](../13-union-find/README.md#application-5-kruskals-mst)) | Is the answer a path cost from one source, or the cheapest set of edges that connects all nodes? Set of edges → MST. |
| Minimum cost on a grid | DP ([Pattern 15](../15-dynamic-programming/README.md)) | Can you move only right/down (no cycles)? Yes → DP. Moves in all 4 directions → Dijkstra. |
| Distances between many pairs | Floyd-Warshall vs Dijkstra from each source | Is n ≤ ~200? Yes → Floyd-Warshall. Larger and sparse → Dijkstra from each source. |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌──────────────────────────────────────────────────────────────────────┐
│                   SHORTEST PATHS DECISION TREE                       │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  START: What kind of graph is this?                                  │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Unweighted (or all weights equal)?                          │    │
│  │   → BFS  (O(V+E), each edge counts as 1)                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ All edge weights ∈ {0, 1}?                                  │    │
│  │   → 0-1 BFS  (O(V+E), deque replaces heap)                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Weighted, non-negative, single source?                      │    │
│  │   → Dijkstra  (O((V+E) log V), min-heap)                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Negative weights OR "at most K edges" constraint?           │    │
│  │   → Bellman-Ford  (O(V×E) full, or O(K×E) for K-hop)      │    │
│  │   → K-hop: copy dist each round!                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ All-pairs AND n ≤ 100?                                      │    │
│  │   → Floyd-Warshall  (O(n³), k outermost loop)              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│  COMPLEXITY SUMMARY                                                  │
│                                                                      │
│  BFS              O(V+E)           unweighted                        │
│  0-1 BFS          O(V+E)           weights ∈ {0,1}                   │
│  Dijkstra         O((V+E) log V)   non-negative weights              │
│  Bellman-Ford     O(V × E)         handles negatives                 │
│  Bellman-Ford K   O(K × E)         "at most K stops"                 │
│  Floyd-Warshall   O(n³)            all-pairs, n ≤ 100                │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│  TOP MISTAKES AT A GLANCE                                            │
│                                                                      │
│  1. Dijkstra + negative weights           → wrong answer             │
│  2. K-hop Bellman-Ford, no dist copy      → multi-hop in one round   │
│  3. Dijkstra, no stale-entry guard        → wasted work / bugs       │
│  4. Floyd-Warshall, k not outermost       → incorrect result         │
│  5. Floyd-Warshall, dp[i][i] not 0        → bad self-loop baseline   │
│  6. 0-1 BFS, pushFront/Back swapped      → wrong order              │
│  7. Floyd-Warshall with n > 200           → TLE                      │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 19: Bit Manipulation →](../19-bit-manipulation/README.md)*
