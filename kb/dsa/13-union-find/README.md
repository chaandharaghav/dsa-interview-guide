# Pattern 13: Union Find (Disjoint Set Union)

## 1. Pattern Name
**Union Find** (DSU - Disjoint Set Union)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Union Find is about **efficiently tracking connected components** when elements are dynamically grouped together.

**The Key Insight:**  
When you need to repeatedly ask "are these two elements connected?" and "merge these two groups", Union Find provides O(α(n)) ≈ O(1) amortized operations — but **only when both path compression AND union by rank are used together** (see Template 2). Path compression alone gives O(log n) amortized (Template 1).

**Mental Model:**  
Think of people forming groups. Each group has a leader (root/parent). To check if two people are in the same group, find their leaders. To merge groups, make one leader point to another.

**Two Core Operations:**
1. **Find:** Which group does element X belong to? (Find the root)
2. **Union:** Merge the groups of X and Y

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Connected components"
- "Merge", "Group together"
- "Same group/set"
- "Friends of friends"
- "Network connectivity"
- "Dynamic connectivity"

### When Union Find Beats DFS/BFS:
- Multiple queries about connectivity
- Elements are merged incrementally
- Need to track component properties (size, count)

### Problem Types:
- Count connected components
- Check if two elements are connected
- Detect cycles in undirected graph
- Minimum Spanning Tree (Kruskal's)
- Account merge / Friend circles

---

## 4. Algorithm Template

### Template 1: Basic Union Find with Path Compression Only
// Complexity: O(log n) amortized per operation (path compression without rank)
// Use Template 2 for O(α(n)) ≈ O(1) amortized
```
class UnionFind:
    function __init__(n):
        parent = new array[n]
        for i = 0 to n-1:
            parent[i] = i  // Each element is its own parent initially
    
    function find(x):
        if parent[x] != x:
            parent[x] = find(parent[x])  // Path compression
        return parent[x]
    
    function union(x, y):
        rootX = find(x)
        rootY = find(y)
        
        if rootX != rootY:
            parent[rootX] = rootY  // Make rootX point to rootY
```

### Template 2: Union Find with Rank (Union by Rank)
```
class UnionFind:
    function __init__(n):
        parent = new array[n]
        rank = new array[n] filled with 0
        for i = 0 to n-1:
            parent[i] = i
    
    function find(x):
        if parent[x] != x:
            parent[x] = find(parent[x])
        return parent[x]
    
    function union(x, y):
        rootX = find(x)
        rootY = find(y)
        
        if rootX != rootY:
            // Attach smaller tree under larger tree
            if rank[rootX] < rank[rootY]:
                parent[rootX] = rootY
            elif rank[rootX] > rank[rootY]:
                parent[rootY] = rootX
            else:
                parent[rootY] = rootX
                rank[rootX] += 1
```

### Template 3: Union Find with Size Tracking
```
class UnionFind:
    function __init__(n):
        parent = new array[n]
        size = new array[n] filled with 1
        componentCount = n
        
        for i = 0 to n-1:
            parent[i] = i
    
    function find(x):
        if parent[x] != x:
            parent[x] = find(parent[x])
        return parent[x]
    
    function union(x, y):
        rootX = find(x)
        rootY = find(y)
        
        if rootX != rootY:
            // Union by size
            if size[rootX] < size[rootY]:
                parent[rootX] = rootY
                size[rootY] += size[rootX]
            else:
                parent[rootY] = rootX
                size[rootX] += size[rootY]
            
            componentCount -= 1
            return true  // Union happened
        
        return false  // Already connected
    
    function isConnected(x, y):
        return find(x) == find(y)
    
    function getSize(x):
        return size[find(x)]
    
    function getComponentCount():
        return componentCount
```

---

## 5. Classic Applications

### Application 1: Number of Connected Components
```
function countComponents(n, edges):
    uf = new UnionFind(n)
    
    for each [u, v] in edges:
        uf.union(u, v)
    
    return uf.getComponentCount()
```

### Application 2: Detect Cycle in Undirected Graph
```
function hasCycle(n, edges):
    uf = new UnionFind(n)
    
    for each [u, v] in edges:
        if uf.isConnected(u, v):
            return true  // Adding this edge creates cycle
        uf.union(u, v)
    
    return false
```

### Application 3: Number of Islands (Union Find approach)
```
function numIslands(grid):
    rows = grid.length
    cols = grid[0].length
    
    function index(row, col):
        return row * cols + col
    
    uf = new UnionFind(rows * cols)
    landCount = 0
    
    for row = 0 to rows - 1:
        for col = 0 to cols - 1:
            if grid[row][col] == '1':
                landCount += 1
    
    // Initially, each land cell is its own component
    // Now merge adjacent land cells
    
    for row = 0 to rows - 1:
        for col = 0 to cols - 1:
            if grid[row][col] == '1':
                // Check right and down (to avoid double counting)
                if col + 1 < cols and grid[row][col + 1] == '1':
                    if uf.union(index(row, col), index(row, col + 1)):
                        landCount -= 1
                if row + 1 < rows and grid[row + 1][col] == '1':
                    if uf.union(index(row, col), index(row + 1, col)):
                        landCount -= 1
    
    return landCount
```

### Application 4: Accounts Merge
```
function accountsMerge(accounts):
    // Map email -> first account index that contains it
    emailToIndex = new Map()
    emailToName = new Map()
    uf = new UnionFind(accounts.length)
    
    for i = 0 to accounts.length - 1:
        account = accounts[i]
        name = account[0]
        
        for j = 1 to account.length - 1:
            email = account[j]
            emailToName[email] = name
            
            if email in emailToIndex:
                // This email seen before, union the accounts
                uf.union(i, emailToIndex[email])
            else:
                emailToIndex[email] = i
    
    // Group emails by root account
    indexToEmails = new Map()
    for each email in emailToIndex:
        root = uf.find(emailToIndex[email])
        if root not in indexToEmails:
            indexToEmails[root] = []
        indexToEmails[root].add(email)
    
    // Build result
    result = []
    for each root in indexToEmails:
        emails = sort(indexToEmails[root])
        name = emailToName[emails[0]]
        result.add([name] + emails)
    
    return result
```

### Application 5: Kruskal's MST
```
function kruskalMST(n, edges):
    // edges = [[u, v, weight], ...]
    sort(edges, by weight ascending)
    
    uf = new UnionFind(n)
    mstWeight = 0
    mstEdges = []
    
    for each [u, v, weight] in edges:
        if not uf.isConnected(u, v):
            uf.union(u, v)
            mstWeight += weight
            mstEdges.add([u, v])
            
            if mstEdges.length == n - 1:
                break  // MST complete
    
    return mstWeight
```

**Recognize MST (minimum spanning tree):** The problem asks for the cheapest set of edges that connects *all* nodes. Typical wording: "connect all cities/points at minimum total cost". The answer is one total cost, not a path between two nodes. A path cost from one source is a shortest-path problem instead ([Pattern 18](../18-shortest-paths/README.md)).

**Two algorithms, same answer:**
- **Kruskal** (above): sort the edges, then add each edge whose endpoints are in different components. Time: O(E log E) for the sort. Use it when the input is an edge list.
- **Prim** (below): grow one tree from a start node. A min-heap holds the edges that leave the tree. Time: O(E log V) with a heap. On a complete graph (every pair of nodes has an edge, as in LC 1584), use the array version: O(V²).

```
function primMST(n, graph):
    // graph[u] = list of (v, weight), undirected
    inTree = new boolean[n]
    minHeap = [(0, 0)]                  // (edge weight, node); start at node 0
    mstWeight = 0
    added = 0

    while minHeap is not empty and added < n:
        (w, u) = minHeap.pop()
        if inTree[u]:
            continue                    // stale entry: u joined by a cheaper edge
        inTree[u] = true
        mstWeight += w
        added += 1
        for each (v, weight) in graph[u]:
            if not inTree[v]:
                minHeap.push((weight, v))

    return added == n ? mstWeight : -1  // -1: graph is disconnected
```

### Application 6: Redundant Connection (Find the Extra Edge)
```
function findRedundantConnection(edges):
    // Given n edges for n nodes (tree + 1 edge), find the extra edge
    n = edges.length
    uf = new UnionFind(n + 1)
    
    for each [u, v] in edges:
        if uf.isConnected(u, v):
            return [u, v]  // This edge creates cycle
        uf.union(u, v)
    
    return []
```

### Application 7: Largest Component Size by Common Factor
```
function largestComponentSize(nums):
    // Group numbers that share a prime factor
    maxVal = max(nums)
    uf = new UnionFind(maxVal + 1)
    
    for each num in nums:
        // Find all prime factors and union with num
        for factor = 2 to sqrt(num):
            if num % factor == 0:
                uf.union(num, factor)
                uf.union(num, num / factor)
    
    // Count component sizes
    count = new Map()
    maxSize = 0
    
    for each num in nums:
        root = uf.find(num)
        count[root] = count.getOrDefault(root, 0) + 1
        maxSize = max(maxSize, count[root])
    
    return maxSize
```

---

## 6. Classic Interview Problems

### MEDIUM
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Number of Provinces** | Count components | 547 |
| **Redundant Connection** | First edge creating cycle | 684 |
| **Accounts Merge** | Group by shared emails | 721 |
| **Most Stones Removed** | Stones in same row/col = connected | 947 |
| **Satisfiability of Equality Equations** | Union equals, check not equals | 990 |
| **Longest Consecutive Sequence** | Alternative to HashSet | 128 |
| **Number of Connected Components** | Basic UF application | 323 |
| **Graph Valid Tree** | n-1 edges, no cycle, connected | 261 |
| **Smallest String With Swaps** | Group swappable indices | 1202 |

### HARD
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Redundant Connection II** | Directed graph, complex cases | 685 |
| **Largest Component Size by Common Factor** | Union by prime factors | 952 |
| **Number of Islands II** | Dynamic island merging | 305 |
| **Bricks Falling When Hit** | Reverse time simulation | 803 |
| **Swim in Rising Water** | Binary search + UF | 778 |

---

## 7. Common Mistakes

### Mistake 1: Not Using Path Compression
```
// WRONG: Linear time find
function find(x):
    while parent[x] != x:
        x = parent[x]
    return x

// CORRECT: Path compression gives O(log n) amortized on its own;
// add union by rank/size (Template 2) to reach O(α(n)) ≈ O(1) amortized
function find(x):
    if parent[x] != x:
        parent[x] = find(parent[x])  // Flatten the tree
    return parent[x]
```

### Mistake 2: Comparing Before Finding Roots
```
// WRONG: Comparing elements directly
if x == y:
    return  // They might have same root but x != y!

// CORRECT: Compare their roots
rootX = find(x)
rootY = find(y)
if rootX == rootY:
    return  // Already connected
```

### Mistake 3: Forgetting to Update Size
```
// WRONG: Size not updated after union
parent[rootX] = rootY
// size[rootY] should include size[rootX] now!

// CORRECT:
parent[rootX] = rootY
size[rootY] += size[rootX]
```

### Mistake 4: 2D Grid Indexing
```
// WRONG: Wrong index calculation
index = row + col  // Multiple cells map to same index!

// CORRECT:
index = row * numCols + col  // Unique index for each cell
```

### Mistake 5: Not Handling Self-Loops
```
// For some problems, need to check:
if u == v:
    continue  // or handle specially
```

---

## 8. Complexity Analysis

| Operation | Without Optimization | With Path Compression | With Both Optimizations |
|-----------|---------------------|----------------------|------------------------|
| Find | O(n) | O(log n) amortized | O(α(n)) ≈ O(1) amortized |
| Union | O(n) | O(log n) amortized | O(α(n)) ≈ O(1) amortized |

α(n) is the inverse Ackermann function, practically constant (< 5 for any realistic n).

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Connected components or cycle check | DFS/BFS ([Pattern 11](../11-graph-traversal/README.md)) | Is the full graph known before any query? Yes → one DFS pass is enough. Edges arrive over time with a query after each → Union Find. |
| "Remove an edge" or "cut a connection" | Reverse the timeline, or rerun DFS | Does the problem delete edges? Union Find cannot undo a union. Process the deletions backward as additions (Bricks Falling When Hit, LC 803), or rerun DFS. |
| "Redundant edge" in a directed graph | Union Find plus in-degree checks (LC 685) | Are the edges directed? Plain Union Find detects cycles only in undirected graphs. |
| Minimum cost to connect all nodes | MST (Kruskal/Prim), not Dijkstra | Does the answer cover *all* nodes as one total, or a path from one source? All nodes → MST. One source → Dijkstra. |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                    UNION FIND CHEAT SHEET                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  CORE OPERATIONS:                                                │
│  - find(x): Return root of x's component                         │
│  - union(x, y): Merge components of x and y                      │
│  - isConnected(x, y): find(x) == find(y)                         │
│                                                                  │
│  OPTIMIZATIONS:                                                  │
│  - Path Compression: parent[x] = find(parent[x])                 │
│  - Union by Rank/Size: Attach smaller tree under larger          │
│                                                                  │
│  TEMPLATE:                                                       │
│  find(x):                                                        │
│      if parent[x] != x:                                          │
│          parent[x] = find(parent[x])                             │
│      return parent[x]                                            │
│                                                                  │
│  union(x, y):                                                    │
│      rootX, rootY = find(x), find(y)                             │
│      if rootX != rootY:                                          │
│          parent[rootX] = rootY (+ rank/size logic)               │
│                                                                  │
│  COMMON USES:                                                    │
│  - Connected components                                          │
│  - Cycle detection (undirected)                                  │
│  - Kruskal's MST                                                 │
│  - Dynamic grouping                                              │
│                                                                  │
│  GRID INDEXING:                                                  │
│  index = row * numCols + col                                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 14: Greedy Patterns →](../14-greedy/README.md)*
