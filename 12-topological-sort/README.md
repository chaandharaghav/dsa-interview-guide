# Pattern 12: Topological Sort

## 1. Pattern Name
**Topological Sort** (Ordering Dependencies)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Topological sort is about finding a **valid ordering of nodes** where all dependencies are satisfied - if A depends on B, then B comes before A.

**The Key Insight:**  
Topological ordering is only possible for **DAGs (Directed Acyclic Graphs)**. If there's a cycle, no valid ordering exists.

**Mental Model:**  
Think of getting dressed. You must put on underwear before pants, socks before shoes. A topological order is any valid sequence that respects all such "must come before" relationships.

**Two Main Approaches:**
1. **Kahn's Algorithm (BFS):** Process nodes with no incoming edges first
2. **DFS-based:** Post-order traversal gives reverse topological order

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Order", "Ordering", "Sequence"
- "Prerequisites", "Dependencies"
- "Before/After relationships"
- "Task scheduling"
- "Course schedule"
- "Build order"
- "Alien dictionary"

### Problem Structure:
- Given nodes and directed edges (dependencies)
- Need to find a valid ordering
- Or detect if ordering is possible (cycle detection)

---

## 4. Algorithm Templates

### Template 1: Kahn's Algorithm (BFS-based)
```
function topologicalSort(numNodes, edges):
    // Build graph and compute in-degrees
    graph = new Map()
    inDegree = new array[numNodes] filled with 0
    
    for i = 0 to numNodes - 1:
        graph[i] = []
    
    for each [from, to] in edges:
        graph[from].add(to)
        inDegree[to] += 1
    
    // Start with all nodes having 0 in-degree
    queue = []
    for i = 0 to numNodes - 1:
        if inDegree[i] == 0:
            queue.enqueue(i)
    
    result = []
    
    while queue is not empty:
        node = queue.dequeue()
        result.add(node)
        
        for neighbor in graph[node]:
            inDegree[neighbor] -= 1
            if inDegree[neighbor] == 0:
                queue.enqueue(neighbor)
    
    // Check if all nodes were processed (no cycle)
    if result.length != numNodes:
        return []  // Cycle detected, no valid order
    
    return result
```

### Template 2: DFS-based Topological Sort
```
function topologicalSortDFS(numNodes, edges):
    graph = buildGraph(numNodes, edges)
    visited = new array[numNodes] filled with WHITE
    result = []
    
    function dfs(node):
        if visited[node] == GRAY:
            return false  // Cycle detected
        if visited[node] == BLACK:
            return true   // Already processed
        
        visited[node] = GRAY  // Mark in progress
        
        for neighbor in graph[node]:
            if not dfs(neighbor):
                return false
        
        visited[node] = BLACK  // Mark done
        result.addFirst(node)  // Add to front (reverse post-order)
        return true
    
    for i = 0 to numNodes - 1:
        if visited[i] == WHITE:
            if not dfs(i):
                return []  // Cycle detected
    
    return result
```

### Template 3: Course Schedule (Can Finish?)
```
function canFinish(numCourses, prerequisites):
    // prerequisites[i] = [course, prereq] means prereq -> course
    graph = new Map()
    inDegree = new array[numCourses] filled with 0
    
    for i = 0 to numCourses - 1:
        graph[i] = []
    
    for each [course, prereq] in prerequisites:
        graph[prereq].add(course)
        inDegree[course] += 1
    
    queue = []
    for i = 0 to numCourses - 1:
        if inDegree[i] == 0:
            queue.enqueue(i)
    
    count = 0
    while queue is not empty:
        node = queue.dequeue()
        count += 1
        
        for neighbor in graph[node]:
            inDegree[neighbor] -= 1
            if inDegree[neighbor] == 0:
                queue.enqueue(neighbor)
    
    return count == numCourses
```

### Template 4: Course Schedule II (Return Order)
```
function findOrder(numCourses, prerequisites):
    // Same as above, but return the order
    graph = new Map()
    inDegree = new array[numCourses] filled with 0
    
    for i = 0 to numCourses - 1:
        graph[i] = []
    
    for each [course, prereq] in prerequisites:
        graph[prereq].add(course)
        inDegree[course] += 1
    
    queue = []
    for i = 0 to numCourses - 1:
        if inDegree[i] == 0:
            queue.enqueue(i)
    
    order = []
    while queue is not empty:
        node = queue.dequeue()
        order.add(node)
        
        for neighbor in graph[node]:
            inDegree[neighbor] -= 1
            if inDegree[neighbor] == 0:
                queue.enqueue(neighbor)
    
    if order.length != numCourses:
        return []  // Cycle, impossible
    
    return order
```

### Template 5: Alien Dictionary
```
function alienOrder(words):
    // Build graph from adjacent word comparisons
    graph = new Map()
    inDegree = new Map()
    
    // Initialize all characters
    for each word in words:
        for each char in word:
            if char not in graph:
                graph[char] = new Set()
                inDegree[char] = 0
    
    // Compare adjacent words to find order
    for i = 0 to words.length - 2:
        word1 = words[i]
        word2 = words[i + 1]
        
        // Check for invalid case: "abc" before "ab"
        if word1.startsWith(word2) and word1.length > word2.length:
            return ""  // Invalid
        
        // Find first different character
        for j = 0 to min(word1.length, word2.length) - 1:
            if word1[j] != word2[j]:
                if word2[j] not in graph[word1[j]]:
                    graph[word1[j]].add(word2[j])
                    inDegree[word2[j]] += 1
                break
    
    // Kahn's algorithm
    queue = []
    for each char in inDegree:
        if inDegree[char] == 0:
            queue.enqueue(char)
    
    result = []
    while queue is not empty:
        char = queue.dequeue()
        result.add(char)
        
        for neighbor in graph[char]:
            inDegree[neighbor] -= 1
            if inDegree[neighbor] == 0:
                queue.enqueue(neighbor)
    
    if result.length != graph.size():
        return ""  // Cycle detected
    
    return result.join("")
```

### Template 6: Parallel Courses (Minimum Semesters)
```
function minimumSemesters(n, relations):
    graph = new Map()
    inDegree = new array[n + 1] filled with 0
    
    for i = 1 to n:
        graph[i] = []
    
    for each [prevCourse, nextCourse] in relations:
        graph[prevCourse].add(nextCourse)
        inDegree[nextCourse] += 1
    
    queue = []
    for i = 1 to n:
        if inDegree[i] == 0:
            queue.enqueue(i)
    
    semesters = 0
    coursesCompleted = 0
    
    while queue is not empty:
        semesters += 1
        levelSize = queue.size()
        
        for i = 0 to levelSize - 1:
            course = queue.dequeue()
            coursesCompleted += 1
            
            for next in graph[course]:
                inDegree[next] -= 1
                if inDegree[next] == 0:
                    queue.enqueue(next)
    
    return coursesCompleted == n ? semesters : -1
```

### Template 7: All Topological Sorts (Backtracking)
```
function allTopologicalSorts(n, edges):
    graph = buildGraph(n, edges)
    inDegree = computeInDegree(n, edges)
    
    result = []
    current = []
    visited = new boolean[n]
    
    function backtrack():
        if current.length == n:
            result.add(copy(current))
            return
        
        for i = 0 to n - 1:
            if not visited[i] and inDegree[i] == 0:
                // Choose
                visited[i] = true
                current.add(i)
                for neighbor in graph[i]:
                    inDegree[neighbor] -= 1
                
                backtrack()
                
                // Unchoose
                visited[i] = false
                current.removeLast()
                for neighbor in graph[i]:
                    inDegree[neighbor] += 1
    
    backtrack()
    return result
```

---

## 5. Key Variations

### Variation A: Check If Prerequisites Are Satisfiable
Just detect if cycle exists using topological sort.

### Variation B: Minimum Time to Complete All Tasks
Use level-by-level BFS - each level is a parallel batch.

### Variation C: Find All Possible Orderings
Backtracking with topological constraints.

### Variation D: Lexicographically Smallest Order
Use min-heap instead of queue in Kahn's algorithm.

---

## 6. Classic Interview Problems

### MEDIUM
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Course Schedule** | Cycle detection with topo sort | 207 |
| **Course Schedule II** | Return one valid order | 210 |
| **Parallel Courses** | Count levels in BFS | 1136 |
| **Find All Possible Recipes** | Topo sort with string nodes | 2115 |
| **Minimum Height Trees** | Trim leaves iteratively | 310 |
| **Loud and Rich** | Reverse topo sort | 851 |

### HARD
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Alien Dictionary** | Build graph from word comparisons | 269 |
| **Course Schedule IV** | Topo sort + reachability | 1462 |
| **Sequence Reconstruction** | Unique topo sort check | 444 |
| **Parallel Courses III** | Topo sort with DP for longest path | 2050 |
| **Sort Items by Groups Respecting Dependencies** | Two-level topo sort | 1203 |

---

## 7. Common Mistakes

### Mistake 1: Wrong Edge Direction
```
// Problem says: [course, prerequisite]
// This means: prerequisite -> course (take prereq first)

// WRONG: Edge from course to prereq
graph[course].add(prereq)

// CORRECT: Edge from prereq to course
graph[prereq].add(course)
inDegree[course] += 1
```

### Mistake 2: Not Detecting Cycles
```
// WRONG: Returning partial result when cycle exists
return result  // Might be incomplete!

// CORRECT: Check if all nodes processed
if result.length != numNodes:
    return []  // Cycle detected
return result
```

### Mistake 3: Missing Node Initialization
```
// WRONG: Only adding nodes that appear in edges
for [from, to] in edges:
    graph[from].add(to)
// Isolated nodes are missing!

// CORRECT: Initialize all nodes first
for i = 0 to n - 1:
    graph[i] = []
```

### Mistake 4: DFS Order Reversal
```
// WRONG: Adding to result before processing neighbors
function dfs(node):
    result.add(node)  // Too early!
    for neighbor in graph[node]:
        dfs(neighbor)

// CORRECT: Add after processing all neighbors (post-order)
function dfs(node):
    for neighbor in graph[node]:
        dfs(neighbor)
    result.addFirst(node)  // Add to front for correct order
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                 TOPOLOGICAL SORT CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  KAHN'S ALGORITHM (BFS):                                         │
│  1. Compute in-degree for all nodes                              │
│  2. Add nodes with in-degree 0 to queue                          │
│  3. Process node: add to result, decrement neighbor in-degrees   │
│  4. If new in-degree = 0, add to queue                           │
│  5. Cycle exists if result.length < numNodes                     │
│                                                                  │
│  DFS-BASED:                                                      │
│  1. Use three colors: WHITE, GRAY, BLACK                         │
│  2. GRAY during recursion = cycle if revisited                   │
│  3. Add to result in post-order (after all neighbors done)       │
│  4. Use addFirst/prepend on finish → correct order, NO reversal  │
│     OR use addLast + reverse the list at the end                 │
│                                                                  │
│  COMMON APPLICATIONS:                                            │
│  - Course scheduling                                             │
│  - Build systems                                                 │
│  - Dependency resolution                                         │
│  - Alien dictionary                                              │
│                                                                  │
│  PARALLEL COURSES:                                               │
│  - Level-by-level BFS                                            │
│  - Each level = one time unit                                    │
│                                                                  │
│  KEY INSIGHT:                                                    │
│  - Only DAGs have topological ordering                           │
│  - Multiple valid orderings may exist                            │
│  - Use min-heap for lexicographically smallest                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 13: Union Find →](../13-union-find/README.md)*
