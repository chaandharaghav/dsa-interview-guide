# Pattern 08: Recursion & Backtracking

## 1. Pattern Name
**Recursion & Backtracking** (Exhaustive Search with Pruning)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Backtracking is about **exploring all possible solutions systematically** while **abandoning paths that can't lead to valid solutions** (pruning).

**The Key Insight:**  
When you need to find ALL solutions (or count them, or find if one exists) and the problem involves making a sequence of choices, backtracking explores the decision tree.

**Mental Model:**
Think of navigating a maze. At each intersection, you pick a direction. If you hit a dead end, you **backtrack** to the last intersection and try a different path. You don't restart from the beginning.

**When Backtracking is the Right Tool:**
- Generate all permutations/combinations/subsets
- Solve constraint satisfaction (Sudoku, N-Queens)
- Find paths in a maze/grid
- Partition problems
- String/word generation problems

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "All possible", "All combinations", "All permutations"
- "Generate", "Print all", "Find all"
- "Partition into", "Divide into"
- "Place N items such that..."
- "Valid arrangements"
- "Constraint satisfaction"

### Constraint Hints:
- Small input size (n ≤ 15-20) - exponential is okay
- Need to enumerate possibilities
- Output is a list of solutions, not a single value

### Input/Output Hints:
- Input: Set of choices, constraints
- Output: List of valid configurations

### RED FLAGS - NOT Backtracking:
- Need only the COUNT of solutions (not the actual solutions) AND overlapping subproblems exist → Convert to DP
- Large n (n > 20) with exponential complexity → Need optimization
- Can be solved with simpler iteration

---

## 4. Brute Force Thought Process

### Subsets Brute Force:
```
Problem: Generate all subsets of [1, 2, 3]

Brute Force Thinking:
- For each element, decide: include or exclude
- This gives 2^n subsets
- Enumerate all 2^n possibilities

Time: O(n × 2^n)  // n = average subset size; each of 2^n subsets costs O(n) to copy
Space: O(n × 2^n) total to store all subsets; O(n) auxiliary stack only
// Note: interviewers usually want total space here, not just auxiliary.
```

### Permutations Brute Force:
```
Problem: Generate all permutations of [1, 2, 3]

Brute Force Thinking:
- For position 0: choose any of n elements
- For position 1: choose any of remaining n-1 elements
- ... and so on
- Total: n! permutations

Time: O(n × n!)  // n! permutations, each of length n to copy
Space: O(n × n!) total to store all permutations; O(n) auxiliary stack only
```

For backtracking problems, brute force IS the solution (with smart pruning).

---

## 5. Optimization Insight (The "Aha" Moment)

### The Backtracking Framework:
```
function backtrack(currentState, choices):
    if isComplete(currentState):
        addToResult(currentState)
        return
    
    for each choice in choices:
        if isValid(choice, currentState):
            makeChoice(currentState, choice)       // Make move
            backtrack(currentState, newChoices)    // Explore
            undoChoice(currentState, choice)       // Backtrack
```

### Why Backtracking is Better Than Naive Enumeration:
1. **Pruning:** We skip entire subtrees when constraints are violated
2. **Incremental:** We build solutions piece by piece
3. **Memory efficient:** We modify in place and undo, not create copies

### The Three Key Questions:
1. **What is a "choice"?** (What decision do I make at each step?)
2. **What are the "constraints"?** (When is a choice invalid?)
3. **What is "complete"?** (When have I found a solution?)

---

## 6. Generalized Templates

### Template 1: Subsets (Include/Exclude Pattern)
```
function subsets(nums):
    result = []
    current = []
    
    function backtrack(index):
        if index == length(nums):
            result.add(copy(current))
            return
        
        // Choice 1: Include nums[index]
        current.add(nums[index])
        backtrack(index + 1)
        current.removeLast()
        
        // Choice 2: Exclude nums[index]
        backtrack(index + 1)
    
    backtrack(0)
    return result
```

### Template 2: Subsets (Iterative Add Pattern)
```
function subsets(nums):
    result = []
    current = []
    
    function backtrack(start):
        result.add(copy(current))  // Every state is a valid subset
        
        for i = start to length(nums) - 1:
            current.add(nums[i])
            backtrack(i + 1)       // Start from i+1 to avoid duplicates
            current.removeLast()
    
    backtrack(0)
    return result
```

### Template 3: Permutations (Swap Pattern)
```
function permutations(nums):
    result = []
    
    function backtrack(start):
        if start == length(nums):
            result.add(copy(nums))
            return
        
        for i = start to length(nums) - 1:
            swap(nums[start], nums[i])
            backtrack(start + 1)
            swap(nums[start], nums[i])  // Undo swap
    
    backtrack(0)
    return result
```

### Template 4: Permutations (Used Array Pattern)
```
function permutations(nums):
    result = []
    current = []
    used = new boolean[length(nums)]
    
    function backtrack():
        if length(current) == length(nums):
            result.add(copy(current))
            return
        
        for i = 0 to length(nums) - 1:
            if used[i]:
                continue
            
            used[i] = true
            current.add(nums[i])
            backtrack()
            current.removeLast()
            used[i] = false
    
    backtrack()
    return result
```

### Template 5: Combinations (Choose k from n)
```
function combinations(n, k):
    result = []
    current = []
    
    function backtrack(start):
        if length(current) == k:
            result.add(copy(current))
            return
        
        // Pruning: need (k - current.size) more elements
        // Only iterate while enough elements remain
        for i = start to n - (k - length(current)) + 1:
            current.add(i)
            backtrack(i + 1)
            current.removeLast()
    
    backtrack(1)
    return result
```

### Template 6: Combination Sum (Reusable Elements)
```
function combinationSum(candidates, target):
    result = []
    current = []
    
    function backtrack(start, remaining):
        if remaining == 0:
            result.add(copy(current))
            return
        if remaining < 0:
            return
        
        for i = start to length(candidates) - 1:
            current.add(candidates[i])
            backtrack(i, remaining - candidates[i])  // i, not i+1 (can reuse)
            current.removeLast()
    
    backtrack(0, target)
    return result
```

### Template 7: Subsets/Permutations with Duplicates
```
function subsetsWithDup(nums):
    sort(nums)  // Critical: sort first to group duplicates
    result = []
    current = []
    
    function backtrack(start):
        result.add(copy(current))
        
        for i = start to length(nums) - 1:
            // Skip duplicates at same level
            if i > start and nums[i] == nums[i - 1]:
                continue
            
            current.add(nums[i])
            backtrack(i + 1)
            current.removeLast()
    
    backtrack(0)
    return result
```

### Template 8: N-Queens
```
function solveNQueens(n):
    result = []
    board = new array[n] filled with -1  // board[row] = column of queen
    
    function isValid(row, col):
        for prevRow = 0 to row - 1:
            prevCol = board[prevRow]
            // Same column or same diagonal
            if prevCol == col or abs(prevCol - col) == abs(prevRow - row):
                return false
        return true
    
    function backtrack(row):
        if row == n:
            result.add(formatBoard(board))
            return
        
        for col = 0 to n - 1:
            if isValid(row, col):
                board[row] = col
                backtrack(row + 1)
                board[row] = -1
    
    backtrack(0)
    return result
```

### Template 9: Sudoku Solver
```
function solveSudoku(board):
    function isValid(row, col, num):
        // Check row, column, and 3x3 box
        for i = 0 to 8:
            if board[row][i] == num:
                return false
            if board[i][col] == num:
                return false
        
        boxRow = (row / 3) * 3
        boxCol = (col / 3) * 3
        for i = 0 to 2:
            for j = 0 to 2:
                if board[boxRow + i][boxCol + j] == num:
                    return false
        
        return true
    
    function backtrack():
        for row = 0 to 8:
            for col = 0 to 8:
                if board[row][col] == '.':
                    for num = '1' to '9':
                        if isValid(row, col, num):
                            board[row][col] = num
                            if backtrack():
                                return true
                            board[row][col] = '.'
                    return false  // No valid number, backtrack
        return true  // All cells filled
    
    backtrack()
```

### Template 10: Word Search in Grid
```
function wordSearch(board, word):
    rows = board.rows
    cols = board.cols
    
    function backtrack(row, col, index):
        if index == length(word):
            return true
        
        if row < 0 or row >= rows or col < 0 or col >= cols:
            return false
        if board[row][col] != word[index]:
            return false
        
        // Mark as visited
        temp = board[row][col]
        board[row][col] = '#'
        
        // Explore 4 directions
        found = backtrack(row + 1, col, index + 1)
             or backtrack(row - 1, col, index + 1)
             or backtrack(row, col + 1, index + 1)
             or backtrack(row, col - 1, index + 1)
        
        // Restore
        board[row][col] = temp
        
        return found
    
    for row = 0 to rows - 1:
        for col = 0 to cols - 1:
            if backtrack(row, col, 0):
                return true
    
    return false
```

---

## 7. Variations of the Pattern

### Variation A: Palindrome Partitioning
**Problem:** Partition string into all possible palindrome substrings
**Key Insight:** At each position, try all palindrome prefixes
```
for end = start to n-1:
    if isPalindrome(start, end):
        current.add(substring)
        backtrack(end + 1)
        current.removeLast()
```

### Variation B: Generate Parentheses
**Problem:** Generate all valid combinations of n pairs of parentheses
**Key Insight:** Track open and close counts, constraints are:
- open < n (can add open)
- close < open (can add close)

### Variation C: Letter Combinations of Phone Number
**Problem:** Generate all letter combinations from digits
**Key Insight:** For each digit, iterate through its letters

### Variation D: Expression Add Operators
**Problem:** Insert +, -, * to reach target
**Key Insight:** Track previous operand for multiplication precedence

### Variation E: Path Sum in Grid
**Problem:** Find all paths from top-left to bottom-right
**Key Insight:** Can only move right or down, backtrack when done

---

## 8. Classic Interview Problems

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Subsets** | Include/exclude each element | 78 |
| **Subsets II** | Sort + skip duplicates | 90 |
| **Permutations** | Swap or used array | 46 |
| **Permutations II** | Sort + skip duplicates | 47 |
| **Combinations** | Choose k from n | 77 |
| **Combination Sum** | Can reuse (i, not i+1) | 39 |
| **Combination Sum II** | Can't reuse + duplicates | 40 |
| **Combination Sum III** | Fixed count k, target n | 216 |
| **Generate Parentheses** | Track open/close counts | 22 |
| **Letter Combinations of Phone Number** | Multi-level choices | 17 |
| **Palindrome Partitioning** | Try all palindrome prefixes | 131 |
| **Word Search** | Grid DFS with backtracking | 79 |
| **Restore IP Addresses** | Partition into 4 valid parts | 93 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **N-Queens** | Place row by row, validate | 51 |
| **N-Queens II** | Count solutions only | 52 |
| **Sudoku Solver** | Fill cell by cell, validate | 37 |
| **Word Search II** | Trie + backtracking | 212 |
| **Expression Add Operators** | Handle multiplication precedence | 282 |
| **Permutation Sequence** | Factorial number system | 60 |
| **Word Ladder II** | BFS for distance + DFS for paths | 126 |
| **Stickers to Spell Word** | Memoization + backtracking | 691 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Foundation
**Day 1:**
- Implement "Subsets" both ways (include/exclude and iterative)
- Trace through execution to understand recursion tree

**Day 2:**
- Solve "Permutations" both ways (swap and used array)
- Understand when each approach is better

**Day 3:**
- Solve "Combinations"
- Implement pruning optimization

**Day 4:**
- Solve "Subsets II" and "Permutations II"
- Master the duplicate-skipping technique

**Day 5:**
- Solve "Combination Sum" and "Combination Sum II"
- Understand i vs i+1 for reusability

### Week 2: Advanced
**Day 1:**
- Solve "Generate Parentheses"
- Practice constraint tracking

**Day 2:**
- Solve "Palindrome Partitioning"
- Combine backtracking with palindrome checking

**Day 3:**
- Solve "Word Search"
- Practice grid-based backtracking

**Day 4:**
- Solve "N-Queens"
- Master constraint validation

**Day 5:**
- Attempt "Sudoku Solver"
- This combines everything

### The Key Skill to Develop:
1. **Identify:** What are my choices at each step?
2. **Constrain:** What makes a choice invalid?
3. **Terminate:** When is the solution complete?
4. **Optimize:** What branches can I prune early?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Forgetting to Copy Before Adding
```
// WRONG: Adding reference, which will be modified
result.add(current)

// CORRECT: Add a copy
result.add(new ArrayList(current))
// or
result.add(current.slice())
```

### Mistake 2: Forgetting to Undo Changes
```
// WRONG: State corrupted for other branches
current.add(element)
backtrack(...)
// forgot to remove!

// CORRECT:
current.add(element)
backtrack(...)
current.removeLast()  // Undo
```

### Mistake 3: Not Handling Duplicates
```
// WRONG for subsets with duplicates [1,2,2]:
// Will generate [1,2] twice

// CORRECT: Sort first, then skip
sort(nums)
for i = start to n-1:
    if i > start and nums[i] == nums[i-1]:
        continue  // Skip duplicate at same recursion level
```

### Mistake 4: Wrong Loop Starting Index
```
// WRONG for subsets: Starting from 0 causes duplicates
for i = 0 to n-1:
    backtrack(i + 1)  // Still generates duplicates!

// CORRECT: Pass and use start index
function backtrack(start):
    for i = start to n-1:
        // ...
```

### Mistake 5: Confusing Combination vs Permutation
```
// Combination [1,2] and [2,1] are SAME
// Use start index, always move forward

// Permutation [1,2] and [2,1] are DIFFERENT
// Don't use start index, use visited array
```

### Mistake 6: Inefficient Validation
```
// WRONG for N-Queens: Checking entire board each time
function isValid(row, col):
    for r = 0 to n-1:
        for c = 0 to n-1:
            // Check if conflicts

// CORRECT: Only check rows above (we place row by row)
function isValid(row, col):
    for r = 0 to row - 1:
        // Only check previously placed queens
```

### Mistake 7: Not Pruning Early
```
// WRONG: Continue even when impossible
function backtrack(index, currentSum, target):
    for i = index to n-1:
        current.add(arr[i])
        backtrack(i + 1, currentSum + arr[i], target)
        current.removeLast()

// CORRECT: Prune when sum exceeds target (if positive numbers)
function backtrack(index, currentSum, target):
    if currentSum > target:
        return  // Prune
    // ...
```

### Mistake 8: Stack Overflow on Deep Recursion
```
// For very deep recursion (n > 1000), consider:
// 1. Iterative approach with explicit stack
// 2. Tail recursion optimization (language-dependent)
// 3. Increasing stack size (not recommended)
```

---

## Bonus: Backtracking vs Dynamic Programming

| Aspect | Backtracking | Dynamic Programming |
|--------|--------------|---------------------|
| Output | All solutions | Optimal value / One solution |
| Subproblems | Independent branches | Overlapping subproblems |
| Approach | Top-down exploration | Bottom-up building |
| Pruning | Cut invalid branches | N/A |
| Memoization | Sometimes helps | Core technique |
| Complexity | Exponential (with pruning) | Polynomial |

**When to convert Backtracking to DP:**
- You only need the count, not all solutions
- Same subproblems are solved multiple times
- Problem has optimal substructure

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│               BACKTRACKING CHEAT SHEET                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  TEMPLATE:                                                       │
│  function backtrack(state):                                      │
│      if isComplete(state): addResult(state); return              │
│      for choice in choices:                                      │
│          if isValid(choice): make(choice); backtrack; undo       │
│                                                                  │
│  SUBSETS: Include/exclude OR iterate-from-start                  │
│  PERMUTATIONS: Swap OR used-array                                │
│  COMBINATIONS: Iterate from start, choose k                      │
│                                                                  │
│  DUPLICATES: Sort first, skip if nums[i] == nums[i-1] && i>start │
│                                                                  │
│  REUSABILITY:                                                    │
│  - Can reuse: backtrack(i, ...)                                  │
│  - Can't reuse: backtrack(i+1, ...)                              │
│                                                                  │
│  CRITICAL REMINDERS:                                             │
│  ✓ Copy before adding to result                                  │
│  ✓ Undo changes after recursive call                             │
│  ✓ Prune early when possible                                     │
│  ✓ Handle duplicates with sort + skip                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 09: Divide and Conquer →](../09-divide-and-conquer/README.md)*
