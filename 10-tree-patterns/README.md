# Pattern 10: Tree DFS / BFS Patterns

## 1. Pattern Name
**Tree Traversal Patterns** (DFS & BFS on Binary Trees)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Tree problems are about recognizing **what information flows up (from children to parent)** vs **what information flows down (from parent to children)** during traversal.

**The Key Insight:**  
Most tree problems can be solved by asking:
1. What do I need from my left subtree?
2. What do I need from my right subtree?
3. How do I combine them with the current node?

**DFS vs BFS:**
- **DFS (Recursion):** Natural for problems involving paths, heights, or when you need to process entire subtrees before the parent
- **BFS (Level Order):** Natural for level-by-level processing, finding shortest path (in terms of edges), or when you need to process nodes "breadth-first"

---

## 3. Recognition Signals

### DFS Keywords:
- "Path from root to leaf"
- "Maximum depth", "Height"
- "Diameter"
- "Lowest common ancestor"
- "Validate BST"
- "Serialize/deserialize"

### BFS Keywords:
- "Level order"
- "Minimum depth"
- "Connect nodes at same level"
- "Right side view"
- "Zigzag traversal"

### Pattern Selection Guide:
| Problem Type | Best Approach |
|--------------|---------------|
| Height/Depth | DFS (postorder) |
| Path sum | DFS (preorder with accumulator) |
| Level traversal | BFS |
| Rightmost nodes | BFS or DFS (right first) |
| Serialize/Deserialize | Either (preorder DFS common) |
| Validate BST | DFS (inorder or min/max bounds) |
| LCA | DFS (postorder) |

---

## 4. Core DFS Templates

### Template 1: Basic DFS Structure
```
function dfs(node):
    if node is null:
        return baseCase
    
    // PREORDER: Process before children
    // doSomething(node)
    
    leftResult = dfs(node.left)
    rightResult = dfs(node.right)
    
    // POSTORDER: Process after children
    // combine(node, leftResult, rightResult)
    
    return result
```

### Template 2: Maximum Depth
```
function maxDepth(node):
    if node is null:
        return 0
    
    leftDepth = maxDepth(node.left)
    rightDepth = maxDepth(node.right)
    
    return 1 + max(leftDepth, rightDepth)
```

### Template 3: Path Sum (Root to Leaf)
```
function hasPathSum(node, targetSum):
    if node is null:
        return false
    
    // Leaf node check
    if node.left is null and node.right is null:
        return targetSum == node.val
    
    remaining = targetSum - node.val
    return hasPathSum(node.left, remaining) or hasPathSum(node.right, remaining)
```

### Template 4: Path Sum with All Paths
```
function pathSum(root, targetSum):
    result = []
    currentPath = []
    
    function dfs(node, remaining):
        if node is null:
            return
        
        currentPath.add(node.val)
        
        if node.left is null and node.right is null and remaining == node.val:
            result.add(copy(currentPath))
        else:
            dfs(node.left, remaining - node.val)
            dfs(node.right, remaining - node.val)
        
        currentPath.removeLast()  // Backtrack
    
    dfs(root, targetSum)
    return result
```

### Template 5: Diameter (Global Max Pattern)
```
function diameter(root):
    maxDiameter = 0
    
    function height(node):
        if node is null:
            return 0
        
        leftHeight = height(node.left)
        rightHeight = height(node.right)
        
        // Update global max (diameter passing through this node)
        maxDiameter = max(maxDiameter, leftHeight + rightHeight)
        
        return 1 + max(leftHeight, rightHeight)
    
    height(root)
    return maxDiameter
```

### Template 6: Lowest Common Ancestor
```
function lowestCommonAncestor(root, p, q):
    if root is null or root == p or root == q:
        return root
    
    left = lowestCommonAncestor(root.left, p, q)
    right = lowestCommonAncestor(root.right, p, q)
    
    // If both sides found something, root is LCA
    if left is not null and right is not null:
        return root
    
    // Otherwise, return whichever side found something
    return left if left is not null else right
```

### Template 7: Validate BST
```
function isValidBST(root):
    function validate(node, minVal, maxVal):
        if node is null:
            return true
        
        if node.val <= minVal or node.val >= maxVal:
            return false
        
        return validate(node.left, minVal, node.val) and
               validate(node.right, node.val, maxVal)
    
    return validate(root, -INFINITY, +INFINITY)
```

### Template 8: BST Inorder (Sorted Property)
```
function kthSmallest(root, k):
    count = 0
    result = null
    
    function inorder(node):
        if node is null or result is not null:
            return
        
        inorder(node.left)
        
        count++
        if count == k:
            result = node.val
            return
        
        inorder(node.right)
    
    inorder(root)
    return result
```

---

## 5. Core BFS Templates

### Template 9: Level Order Traversal
```
function levelOrder(root):
    if root is null:
        return []
    
    result = []
    queue = [root]
    
    while queue is not empty:
        levelSize = queue.size()
        currentLevel = []
        
        for i = 0 to levelSize - 1:
            node = queue.dequeue()
            currentLevel.add(node.val)
            
            if node.left is not null:
                queue.enqueue(node.left)
            if node.right is not null:
                queue.enqueue(node.right)
        
        result.add(currentLevel)
    
    return result
```

### Template 10: Minimum Depth (BFS)
```
function minDepth(root):
    if root is null:
        return 0
    
    queue = [(root, 1)]  // (node, depth)
    
    while queue is not empty:
        (node, depth) = queue.dequeue()
        
        // First leaf node = minimum depth
        if node.left is null and node.right is null:
            return depth
        
        if node.left is not null:
            queue.enqueue((node.left, depth + 1))
        if node.right is not null:
            queue.enqueue((node.right, depth + 1))
    
    return 0
```

### Template 11: Right Side View
```
function rightSideView(root):
    if root is null:
        return []
    
    result = []
    queue = [root]
    
    while queue is not empty:
        levelSize = queue.size()
        
        for i = 0 to levelSize - 1:
            node = queue.dequeue()
            
            // Last node in level is visible from right
            if i == levelSize - 1:
                result.add(node.val)
            
            if node.left is not null:
                queue.enqueue(node.left)
            if node.right is not null:
                queue.enqueue(node.right)
    
    return result
```

### Template 12: Zigzag Level Order
```
function zigzagLevelOrder(root):
    if root is null:
        return []
    
    result = []
    queue = [root]
    leftToRight = true
    
    while queue is not empty:
        levelSize = queue.size()
        currentLevel = []
        
        for i = 0 to levelSize - 1:
            node = queue.dequeue()
            currentLevel.add(node.val)
            
            if node.left is not null:
                queue.enqueue(node.left)
            if node.right is not null:
                queue.enqueue(node.right)
        
        if not leftToRight:
            reverse(currentLevel)
        
        result.add(currentLevel)
        leftToRight = not leftToRight
    
    return result
```

---

## 6. Advanced Tree Patterns

### Pattern A: Serialize/Deserialize (Preorder)
```
function serialize(root):
    result = []
    
    function preorder(node):
        if node is null:
            result.add("null")
            return
        
        result.add(node.val.toString())
        preorder(node.left)
        preorder(node.right)
    
    preorder(root)
    return result.join(",")

function deserialize(data):
    values = data.split(",")
    index = 0
    
    function buildTree():
        if values[index] == "null":
            index++
            return null
        
        node = new TreeNode(parseInt(values[index]))
        index++
        node.left = buildTree()
        node.right = buildTree()
        return node
    
    return buildTree()
```

### Pattern B: Construct Tree from Traversals
```
function buildTree(preorder, inorder):
    inorderMap = {}
    for i = 0 to length(inorder) - 1:
        inorderMap[inorder[i]] = i
    
    preorderIndex = 0
    
    function build(left, right):
        if left > right:
            return null
        
        rootVal = preorder[preorderIndex]
        preorderIndex++
        
        root = new TreeNode(rootVal)
        root.left = build(left, inorderMap[rootVal] - 1)
        root.right = build(inorderMap[rootVal] + 1, right)
        
        return root
    
    return build(0, length(inorder) - 1)
```

### Pattern C: Binary Tree Maximum Path Sum
```
function maxPathSum(root):
    maxSum = -INFINITY
    
    function maxGain(node):
        if node is null:
            return 0
        
        // Max gain from left/right (ignore negative paths)
        leftGain = max(0, maxGain(node.left))
        rightGain = max(0, maxGain(node.right))
        
        // Path through current node
        currentPathSum = node.val + leftGain + rightGain
        maxSum = max(maxSum, currentPathSum)
        
        // Return max gain for parent (can only use one side)
        return node.val + max(leftGain, rightGain)
    
    maxGain(root)
    return maxSum
```

### Pattern D: Flatten Binary Tree to Linked List
```
function flatten(root):
    prev = null
    
    function reversePreorder(node):
        // Traversal order: Right-Left-Root (= reverse preorder, NOT reverse postorder)
        // Processing right before left in this post-order builds the list in forward preorder.
        if node is null:
            return
        
        reversePreorder(node.right)
        reversePreorder(node.left)
        
        node.right = prev
        node.left = null
        prev = node
    
    reversePreorder(root)
```

### Pattern E: Vertical Order Traversal
```
function verticalOrder(root):
    if root is null:
        return []
    
    columnMap = {}  // column -> list of (row, value)
    minCol = 0, maxCol = 0
    
    queue = [(root, 0, 0)]  // (node, row, col)
    
    while queue is not empty:
        (node, row, col) = queue.dequeue()
        
        if col not in columnMap:
            columnMap[col] = []
        columnMap[col].add((row, node.val))
        
        minCol = min(minCol, col)
        maxCol = max(maxCol, col)
        
        if node.left is not null:
            queue.enqueue((node.left, row + 1, col - 1))
        if node.right is not null:
            queue.enqueue((node.right, row + 1, col + 1))
    
    result = []
    for col = minCol to maxCol:
        sortedColumn = sort(columnMap[col], by row then value)
        result.add(values from sortedColumn)
    
    return result
```

---

## 7. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight |
|---------|-------------|
| Maximum Depth of Binary Tree | DFS return height |
| Invert Binary Tree | Swap children recursively |
| Same Tree | Compare structure and values |
| Symmetric Tree | Compare left.left with right.right |
| Path Sum | DFS with remaining sum |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Binary Tree Level Order Traversal** | BFS with level tracking | 102 |
| **Binary Tree Zigzag Level Order** | BFS + alternate reverse | 103 |
| **Binary Tree Right Side View** | BFS, last in level OR DFS right-first | 199 |
| **Validate Binary Search Tree** | DFS with min/max bounds | 98 |
| **Lowest Common Ancestor of Binary Tree** | DFS, return found nodes | 236 |
| **Binary Tree Maximum Path Sum** | Global max with gain function | 124 |
| **Construct Binary Tree from Preorder and Inorder** | D&C with hashmap | 105 |
| **Kth Smallest Element in BST** | Inorder traversal | 230 |
| **Serialize and Deserialize Binary Tree** | Preorder with null markers | 297 |
| **Flatten Binary Tree to Linked List** | Reverse postorder | 114 |
| **Path Sum II** | DFS with path tracking | 113 |
| **Diameter of Binary Tree** | Global max, height DFS | 543 |
| **Count Good Nodes in Binary Tree** | DFS with max-so-far | 1448 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Vertical Order Traversal** | BFS with column tracking | 987 |
| **Binary Tree Cameras** | Greedy DFS from leaves | 968 |
| **Recover Binary Search Tree** | Inorder to find swapped | 99 |
| **Maximum Sum BST in Binary Tree** | DFS returning (isValid, min, max, sum) | 1373 |

---

## 8. How to Practice This Pattern Effectively

### Week 1: DFS Foundation
**Day 1:**
- Solve "Maximum Depth" and "Minimum Depth"
- Understand return value propagation

**Day 2:**
- Solve "Path Sum" and "Path Sum II"
- Practice path tracking and backtracking

**Day 3:**
- Solve "Validate BST"
- Understand the min/max bounds technique

**Day 4:**
- Solve "Lowest Common Ancestor"
- Master the "return what you found" pattern

**Day 5:**
- Solve "Diameter of Binary Tree"
- Practice the global maximum pattern

### Week 2: BFS and Advanced
**Day 1:**
- Solve "Level Order Traversal" and "Zigzag"
- Master the level-by-level BFS template

**Day 2:**
- Solve "Right Side View"
- Compare BFS vs DFS approaches

**Day 3:**
- Solve "Serialize and Deserialize"
- Practice preorder with null markers

**Day 4:**
- Solve "Binary Tree Maximum Path Sum"
- Combine global tracking with gain calculation

**Day 5:**
- Solve "Construct from Preorder and Inorder"
- Understand the divide and conquer tree building

### Self-Check Questions:
- Can I identify if a problem needs DFS or BFS?
- Do I understand what to return vs what to track globally?
- Can I handle BST-specific properties?

---

## 9. Common Mistakes Even Experienced Developers Make

### Mistake 1: Forgetting Null Checks
```
// WRONG: Crashes on null node
function maxDepth(node):
    return 1 + max(maxDepth(node.left), maxDepth(node.right))

// CORRECT:
function maxDepth(node):
    if node is null:
        return 0
    return 1 + max(maxDepth(node.left), maxDepth(node.right))
```

### Mistake 2: Confusing Leaf Node Check
```
// WRONG: Null is not a leaf
if node is null:
    return true  // Found path!

// CORRECT: Leaf has no children
if node.left is null and node.right is null:
    return targetSum == node.val
```

### Mistake 3: Wrong BST Validation
```
// WRONG: Only checking parent-child relationship
function isValidBST(node):
    if node.left and node.left.val >= node.val:
        return false
    // This misses violations deeper in the tree!

// CORRECT: Pass bounds down
function isValidBST(node, min, max):
    if node.val <= min or node.val >= max:
        return false
    return isValidBST(node.left, min, node.val) and
           isValidBST(node.right, node.val, max)
```

### Mistake 4: Not Handling Single Child
```
// WRONG: Assuming both children exist
leftHeight = maxDepth(node.left)
rightHeight = maxDepth(node.right)
// For min depth, this gives wrong answer with single child
```

### Mistake 5: Mutating Input in BFS
```
// WRONG: Modifying queue size during iteration
while queue is not empty:
    node = queue.dequeue()
    // ... add children to queue
    // Loop condition changes mid-iteration!

// CORRECT: Capture level size first
levelSize = queue.size()
for i = 0 to levelSize - 1:
    node = queue.dequeue()
    // ...
```

### Mistake 6: Forgetting Backtrack in Path Problems
```
// WRONG: Path keeps growing
function findPaths(node, path):
    path.add(node.val)
    findPaths(node.left, path)
    findPaths(node.right, path)
    // path still contains node.val for sibling processing!

// CORRECT:
function findPaths(node, path):
    path.add(node.val)
    findPaths(node.left, path)
    findPaths(node.right, path)
    path.removeLast()  // Backtrack
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                    TREE PATTERNS CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  DFS PATTERNS:                                                   │
│  - Height/Depth: Return 1 + max(left, right)                     │
│  - Path Sum: Pass remaining sum down                             │
│  - Diameter/Max Path: Global variable + return best single path  │
│  - LCA: Return node if found, combine at ancestor                │
│  - Validate BST: Pass min/max bounds down                        │
│                                                                  │
│  BFS PATTERNS:                                                   │
│  - Level Order: Track level size before processing               │
│  - Min Depth: Return at first leaf found                         │
│  - Right View: Last node of each level                           │
│                                                                  │
│  BST PROPERTIES:                                                 │
│  - Inorder = sorted order                                        │
│  - All left subtree < root < all right subtree                   │
│  - Search, insert, delete: O(h) where h = height                 │
│                                                                  │
│  KEY QUESTIONS:                                                  │
│  - What info flows UP? (Return value)                            │
│  - What info flows DOWN? (Parameters)                            │
│  - What's global? (External variable)                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 11: Graph Traversal Patterns →](../11-graph-traversal/README.md)*
