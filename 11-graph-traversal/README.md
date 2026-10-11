# Pattern 11: Graph Traversal Patterns

## 1. Pattern Name
**Graph Traversal** (DFS & BFS on Graphs)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Graphs are about **relationships between entities**. Unlike trees, graphs can have:
- **Cycles:** Must track visited nodes
- **Multiple paths:** Same node reachable from different routes
- **Disconnected components:** May need to start DFS/BFS from multiple nodes

**The Key Insight:**  
Every graph problem starts with: "How do I represent this graph?" and "What traversal helps me answer the question?"

**When to Use What:**
- **DFS:** Finding paths, detecting cycles, exploring all possibilities
- **BFS:** Shortest path (unweighted), level-by-level exploration

---

## 3. Recognition Signals

### Graph Problem Indicators:
- "Connected", "Reachable", "Path between"
- "Network", "Grid", "Maze"
- "Dependencies", "Prerequisites"
- "Islands", "Regions", "Components"
- Node relationships described (edges)

### BFS Indicators:
- "Shortest path" (unweighted)
- "Minimum steps/moves"
- "Level", "Distance"
- "Nearest", "Closest"

### DFS Indicators:
- "All paths", "Any path"
- "Cycle detection"
- "Connected components"
- "Backtracking in graph"

---

## 4. Graph Representations

### Adjacency List (Most Common)
```
// For graph: 0 -> 1, 0 -> 2, 1 -> 2
graph = {
    0: [1, 2],
    1: [2],
    2: []
}

// Building from edge list
function buildGraph(n, edges):
    graph = new Map()
    for i = 0 to n - 1:
        graph[i] = []
    for each [u, v] in edges:
        graph[u].add(v)
        graph[v].add(u)  // If undirected
    return graph
```

### Adjacency Matrix (Dense graphs)
```
// For n nodes
matrix = new int[n][n]
matrix[u][v] = 1  // if edge exists
matrix[u][v] = weight  // if weighted
```

### Edge List
```
edges = [[0, 1], [0, 2], [1, 2]]
```

---

## 5. Core Templates

### Template 1: DFS (Recursive)
```
function dfs(graph, node, visited):
    if node in visited:
        return
    
    visited.add(node)
    process(node)
    
    for neighbor in graph[node]:
        dfs(graph, neighbor, visited)
```

### Template 2: DFS (Iterative)
```
function dfsIterative(graph, start):
    visited = new Set()
    stack = [start]
    
    while stack is not empty:
        node = stack.pop()
        
        if node in visited:
            continue
        
        visited.add(node)
        process(node)
        
        for neighbor in graph[node]:
            if neighbor not in visited:
                stack.push(neighbor)
```

### Template 3: BFS
```
function bfs(graph, start):
    visited = new Set([start])
    queue = [start]
    
    while queue is not empty:
        node = queue.dequeue()
        process(node)
        
        for neighbor in graph[node]:
            if neighbor not in visited:
                visited.add(neighbor)
                queue.enqueue(neighbor)
```

### Template 4: BFS Shortest Path (Unweighted)
```
function shortestPath(graph, start, end):
    if start == end:
        return 0
    
    visited = new Set([start])
    queue = [(start, 0)]  // (node, distance)
    
    while queue is not empty:
        (node, dist) = queue.dequeue()
        
        for neighbor in graph[node]:
            if neighbor == end:
                return dist + 1
            
            if neighbor not in visited:
                visited.add(neighbor)
                queue.enqueue((neighbor, dist + 1))
    
    return -1  // No path
```

### Template 5: Count Connected Components
```
function countComponents(n, edges):
    graph = buildGraph(n, edges)
    visited = new Set()
    count = 0
    
    for node = 0 to n - 1:
        if node not in visited:
            dfs(graph, node, visited)
            count += 1
    
    return count
```

### Template 6: Cycle Detection (Undirected)
```
function hasCycleUndirected(graph, n):
    // NOTE: Passing parent NODE is only correct for simple graphs (at most one edge between
    // any pair of nodes). For multigraphs, pass the parent EDGE INDEX instead — otherwise
    // a second parallel edge between the same pair is falsely detected as a cycle.
    visited = new Set()
    
    function dfs(node, parent):
        visited.add(node)
        
        for neighbor in graph[node]:
            if neighbor not in visited:
                if dfs(neighbor, node):
                    return true
            elif neighbor != parent:
                // Found visited node that's not parent = cycle
                return true
        
        return false
    
    for node = 0 to n - 1:
        if node not in visited:
            if dfs(node, -1):
                return true
    
    return false
```

### Template 7: Cycle Detection (Directed) - Three Colors
```
function hasCycleDirected(graph, n):
    // WHITE = 0 (unvisited), GRAY = 1 (in progress), BLACK = 2 (done)
    color = new array[n] filled with WHITE
    
    function dfs(node):
        color[node] = GRAY
        
        for neighbor in graph[node]:
            if color[neighbor] == GRAY:
                return true  // Back edge = cycle
            if color[neighbor] == WHITE:
                if dfs(neighbor):
                    return true
        
        color[node] = BLACK
        return false
    
    for node = 0 to n - 1:
        if color[node] == WHITE:
            if dfs(node):
                return true
    
    return false
```

### Template 8: Grid BFS (4-directional)
```
function gridBFS(grid, startRow, startCol):
    rows = grid.length
    cols = grid[0].length
    directions = [[-1,0], [1,0], [0,-1], [0,1]]
    
    visited = new Set()
    visited.add((startRow, startCol))
    queue = [(startRow, startCol, 0)]  // (row, col, distance)
    
    while queue is not empty:
        (row, col, dist) = queue.dequeue()
        
        if isTarget(row, col):
            return dist
        
        for [dr, dc] in directions:
            newRow = row + dr
            newCol = col + dc
            
            if isValid(newRow, newCol) and (newRow, newCol) not in visited:
                visited.add((newRow, newCol))
                queue.enqueue((newRow, newCol, dist + 1))
    
    return -1

function isValid(row, col):
    return row >= 0 and row < rows and col >= 0 and col < cols
           and grid[row][col] != obstacle
```

### Template 9: Number of Islands (Grid DFS)
```
function numIslands(grid):
    rows = grid.length
    cols = grid[0].length
    count = 0
    
    function dfs(row, col):
        if row < 0 or row >= rows or col < 0 or col >= cols:
            return
        if grid[row][col] != '1':
            return
        
        grid[row][col] = '0'  // Mark visited
        
        dfs(row - 1, col)
        dfs(row + 1, col)
        dfs(row, col - 1)
        dfs(row, col + 1)
    
    for row = 0 to rows - 1:
        for col = 0 to cols - 1:
            if grid[row][col] == '1':
                dfs(row, col)
                count += 1
    
    return count
```

### Template 10: Clone Graph
```
function cloneGraph(node):
    if node is null:
        return null
    
    cloned = new Map()  // original -> clone
    
    function dfs(original):
        if original in cloned:
            return cloned[original]
        
        copy = new Node(original.val)
        cloned[original] = copy
        
        for neighbor in original.neighbors:
            copy.neighbors.add(dfs(neighbor))
        
        return copy
    
    return dfs(node)
```

### Template 11: Bipartite Check
```
function isBipartite(graph):
    n = graph.length
    colors = new array[n] filled with -1  // -1 = uncolored
    
    function bfs(start):
        queue = [start]
        colors[start] = 0
        
        while queue is not empty:
            node = queue.dequeue()
            
            for neighbor in graph[node]:
                if colors[neighbor] == -1:
                    colors[neighbor] = 1 - colors[node]  // Opposite color
                    queue.enqueue(neighbor)
                elif colors[neighbor] == colors[node]:
                    return false  // Same color = not bipartite
        
        return true
    
    for i = 0 to n - 1:
        if colors[i] == -1:
            if not bfs(i):
                return false
    
    return true
```

---

## 6. Advanced Patterns

### Pattern A: Multi-Source BFS
**Problem:** Find distance from any source to all cells (01 Matrix)
```
function updateMatrix(mat):
    rows = mat.length
    cols = mat[0].length
    queue = []
    
    // Start BFS from all 0s simultaneously
    for row = 0 to rows - 1:
        for col = 0 to cols - 1:
            if mat[row][col] == 0:
                queue.enqueue((row, col))
            else:
                mat[row][col] = INFINITY
    
    directions = [[-1,0], [1,0], [0,-1], [0,1]]
    
    while queue is not empty:
        (row, col) = queue.dequeue()
        
        for [dr, dc] in directions:
            newRow = row + dr
            newCol = col + dc
            
            if isValid(newRow, newCol) and mat[newRow][newCol] > mat[row][col] + 1:
                mat[newRow][newCol] = mat[row][col] + 1
                queue.enqueue((newRow, newCol))
    
    return mat
```

### Pattern B: BFS with State
**Problem:** Shortest path with keys and doors
```
function shortestPathWithState(grid):
    // State = (row, col, keysCollected)
    // Use bitmask for keys: key 'a' = bit 0, 'b' = bit 1, etc.
    
    visited = new Set()
    queue = [(startRow, startCol, 0, 0)]  // (row, col, keys, steps)
    
    while queue is not empty:
        (row, col, keys, steps) = queue.dequeue()
        
        if keys == allKeys:
            return steps
        
        state = (row, col, keys)
        if state in visited:
            continue
        visited.add(state)
        
        for neighbor:
            if isKey(neighbor):
                newKeys = keys | (1 << keyIndex)
                queue.enqueue((newRow, newCol, newKeys, steps + 1))
            elif isDoor(neighbor) and hasKey(keys, doorIndex):
                queue.enqueue((newRow, newCol, keys, steps + 1))
            elif isPassable(neighbor):
                queue.enqueue((newRow, newCol, keys, steps + 1))
```

### Pattern C: DFS with Memoization on Graphs
**Problem:** Count all paths from source to target (DAG)
```
function countPaths(graph, target):
    memo = new Map()
    
    function dfs(node):
        if node == target:
            return 1
        
        if node in memo:
            return memo[node]
        
        count = 0
        for neighbor in graph[node]:
            count += dfs(neighbor)
        
        memo[node] = count
        return count
    
    return dfs(0)
```

---

## 7. Classic Interview Problems

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Number of Islands** | Grid DFS/BFS, mark visited | 200 |
| **Clone Graph** | DFS with hashmap for clones | 133 |
| **Course Schedule** | Cycle detection in directed graph | 207 |
| **Course Schedule II** | Topological sort | 210 |
| **Pacific Atlantic Water Flow** | Multi-source DFS from borders | 417 |
| **Number of Connected Components** | DFS/BFS or Union Find | 323 |
| **Graph Valid Tree** | n-1 edges + connected + no cycle | 261 |
| **Rotting Oranges** | Multi-source BFS | 994 |
| **01 Matrix** | Multi-source BFS from 0s | 542 |
| **Surrounded Regions** | DFS from borders | 130 |
| **Is Graph Bipartite** | BFS/DFS with 2-coloring | 785 |
| **All Paths From Source to Target** | DFS on DAG | 797 |
| **Keys and Rooms** | DFS, collect keys | 841 |
| **Shortest Bridge** | DFS to find island + BFS to expand | 934 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Word Ladder** | BFS on word transformation graph | 127 |
| **Word Ladder II** | BFS + backtracking for all paths | 126 |
| **Alien Dictionary** | Topological sort from comparisons | 269 |
| **Shortest Path in a Grid with Obstacles** | BFS with state (position + obstacles) | 1293 |
| **Evaluate Division** | Graph with weighted edges, DFS | 399 |
| **Reconstruct Itinerary** | Hierholzer's algorithm (Eulerian path) | 332 |
| **Critical Connections** | Tarjan's algorithm (bridges) | 1192 |

---

## 8. How to Practice This Pattern Effectively

### Week 1: Foundation
**Day 1:**
- Implement DFS and BFS from scratch
- Solve "Number of Islands"

**Day 2:**
- Solve "Clone Graph"
- Practice DFS with hashmap

**Day 3:**
- Solve "Course Schedule" (cycle detection)
- Understand the three-color technique

**Day 4:**
- Solve "Rotting Oranges"
- Master multi-source BFS

**Day 5:**
- Solve "Is Graph Bipartite"
- Practice 2-coloring technique

### Week 2: Advanced
**Day 1:**
- Solve "Pacific Atlantic Water Flow"
- Multi-source DFS from different starting points

**Day 2:**
- Solve "Word Ladder"
- BFS on implicit graph

**Day 3:**
- Solve "Shortest Path with Obstacles"
- BFS with state

**Day 4:**
- Solve "Evaluate Division"
- Weighted graph traversal

**Day 5:**
- Review all patterns
- Practice identifying which template to use

---

## 9. Common Mistakes

### Mistake 1: Forgetting Visited Set
```
// WRONG: Infinite loop in cyclic graph
function dfs(node):
    for neighbor in graph[node]:
        dfs(neighbor)

// CORRECT:
function dfs(node, visited):
    if node in visited:
        return
    visited.add(node)
    for neighbor in graph[node]:
        dfs(neighbor, visited)
```

### Mistake 2: Wrong BFS Visited Timing
```
// WRONG: Add to visited when dequeuing
while queue is not empty:
    node = queue.dequeue()
    visited.add(node)  // Too late! Might enqueue same node multiple times

// CORRECT: Add to visited when enqueuing
while queue is not empty:
    node = queue.dequeue()
    for neighbor:
        if neighbor not in visited:
            visited.add(neighbor)  // Before enqueue
            queue.enqueue(neighbor)
```

### Mistake 3: Cycle Detection in Undirected (Parent Check)
```
// WRONG: Not excluding parent
if neighbor in visited:
    return true  // Might be parent!

// CORRECT:
if neighbor in visited and neighbor != parent:
    return true
```

### Mistake 4: Grid Bounds Check
```
// WRONG: Check after accessing
grid[row][col]  // Might be out of bounds!
if row < 0:
    return

// CORRECT: Check before accessing
if row < 0 or row >= rows or col < 0 or col >= cols:
    return
// Now safe to access grid[row][col]
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                  GRAPH TRAVERSAL CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  DFS USE CASES:                                                  │
│  - Cycle detection, Connected components                         │
│  - Path finding (all paths), Topological sort                    │
│                                                                  │
│  BFS USE CASES:                                                  │
│  - Shortest path (unweighted), Level traversal                   │
│  - Multi-source distance                                         │
│                                                                  │
│  CYCLE DETECTION:                                                │
│  - Undirected: Check if neighbor != parent AND visited           │
│  - Directed: Three colors (WHITE, GRAY, BLACK)                   │
│                                                                  │
│  GRID GRAPHS:                                                    │
│  - 4 directions: [[-1,0],[1,0],[0,-1],[0,1]]                     │
│  - 8 directions: Add diagonals                                   │
│  - Always check bounds before accessing                          │
│                                                                  │
│  VISITED TIMING:                                                 │
│  - DFS: Mark when entering node                                  │
│  - BFS: Mark when ENQUEUING (not dequeuing)                      │
│                                                                  │
│  MULTI-SOURCE BFS:                                               │
│  - Start with ALL sources in queue                               │
│  - Useful for "nearest" from multiple points                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 12: Topological Sort →](../12-topological-sort/README.md)*
