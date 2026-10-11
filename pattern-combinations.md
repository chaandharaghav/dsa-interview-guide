# Pattern Combinations: Multi-Pattern Problems

The hardest interview problems require **combining multiple patterns**. This guide teaches you to recognize and solve these hybrid problems.

---

## Why Pattern Combinations Matter

**Easy problems:** One pattern
**Medium problems:** One pattern with edge cases, or two simple patterns
**Hard problems:** Almost always require 2-3 patterns working together

**The Interview Reality:**
Top companies specifically ask combination problems to test:
1. Can you recognize multiple patterns in one problem?
2. Can you integrate techniques seamlessly?
3. Do you understand when one pattern feeds into another?

---

## Common Pattern Combinations

### Combination 1: Prefix Sum + HashMap
**Classic Problem:** Subarray Sum Equals K (LC #560)

**Why Two Patterns:**
- Prefix Sum: Convert range sum to simple subtraction
- HashMap: O(1) lookup for complementary prefix sums

**How They Work Together:**
```
Problem: Count subarrays with sum = k

Insight: sum(i, j) = prefix[j] - prefix[i-1]
If prefix[j] - prefix[i-1] = k, then prefix[i-1] = prefix[j] - k

Solution:
- Compute running prefix sum
- For each prefix[j], check if (prefix[j] - k) exists in HashMap
- HashMap stores count of each prefix sum seen so far
```

**Template:**
```
function subarraySum(nums, k):
    prefixCount = {0: 1}  // Empty prefix has sum 0
    currentSum = 0
    count = 0
    
    for num in nums:
        currentSum += num
        
        // Check if (currentSum - k) exists
        if (currentSum - k) in prefixCount:
            count += prefixCount[currentSum - k]
        
        // Record current prefix
        prefixCount[currentSum] = prefixCount.get(currentSum, 0) + 1
    
    return count
```

**Similar Problems:**
- LC 525: Contiguous Array (0s and 1s)
- LC 974: Subarray Sums Divisible by K
- LC 523: Continuous Subarray Sum

---

### Combination 2: Binary Search + Greedy Validation
**Classic Problem:** Split Array Largest Sum (LC #410)

**Why Two Patterns:**
- Binary Search: Search the answer space (minimum possible maximum sum)
- Greedy: Validate if a particular answer is achievable

**How They Work Together:**
```
Problem: Split array into m subarrays, minimize the largest sum

Binary Search on Answer:
- lo = max(nums)  // Can't have sum less than largest element
- hi = sum(nums)  // Worst case: one subarray
- Search for minimum valid maximum sum

Greedy Validation:
- Given target max sum, can we split into ≤ m parts?
- Greedily pack elements until exceeding target
- Count partitions needed
```

**Template:**
```
function splitArray(nums, m):
    function canSplit(maxSum):
        partitions = 1
        currentSum = 0
        for num in nums:
            if currentSum + num > maxSum:
                partitions++
                currentSum = num
            else:
                currentSum += num
        return partitions <= m
    
    lo = max(nums)
    hi = sum(nums)
    
    while lo < hi:
        mid = lo + (hi - lo) / 2
        if canSplit(mid):
            hi = mid
        else:
            lo = mid + 1
    
    return lo
```

**Similar Problems:**
- LC 875: Koko Eating Bananas
- LC 1011: Capacity To Ship Packages Within D Days
- LC 1482: Minimum Number of Days to Make m Bouquets

---

### Combination 3: DFS/BFS + Memoization (DP on Graphs)
**Classic Problem:** Longest Increasing Path in a Matrix (LC #329)

**Why Two Patterns:**
- DFS: Explore paths in all directions
- Memoization: Cache results to avoid recomputation

**How They Work Together:**
```
Problem: Find longest increasing path in matrix

DFS explores all valid paths from each cell
BUT: Same cell may be visited from multiple starting points
Memoization: Store longest path starting from each cell

Key Insight: No cycles possible (strictly increasing)
             → Can memoize without cycle issues
```

**Template:**
```
function longestIncreasingPath(matrix):
    rows, cols = matrix dimensions
    memo = new int[rows][cols]  // 0 = not computed
    
    function dfs(row, col):
        if memo[row][col] != 0:
            return memo[row][col]
        
        maxPath = 1
        for each direction (dr, dc):
            newRow, newCol = row + dr, col + dc
            if inBounds(newRow, newCol) and matrix[newRow][newCol] > matrix[row][col]:
                maxPath = max(maxPath, 1 + dfs(newRow, newCol))
        
        memo[row][col] = maxPath
        return maxPath
    
    result = 0
    for each cell (r, c):
        result = max(result, dfs(r, c))
    
    return result
```

**Similar Problems:**
- LC 576: Out of Boundary Paths
- LC 1240: Tiling a Rectangle with the Fewest Squares
- LC 1444: Number of Ways of Cutting a Pizza

---

### Combination 4: Sliding Window + HashMap (Frequency Tracking)
**Classic Problem:** Minimum Window Substring (LC #76)

**Why Two Patterns:**
- Sliding Window: Efficiently consider all substrings
- HashMap: Track character frequencies in window vs target

**How They Work Together:**
```
Problem: Find minimum window containing all characters of target

Sliding Window:
- Expand right to include characters
- Shrink left to minimize window

HashMap:
- targetFreq: Required character counts
- windowFreq: Current window character counts
- formed: Count of characters meeting requirement
```

**Template:**
```
function minWindow(s, t):
    targetFreq = frequency map of t
    required = targetFreq.size()
    
    windowFreq = {}
    formed = 0
    left = 0
    result = ""
    minLen = INFINITY
    
    for right = 0 to s.length - 1:
        char = s[right]
        windowFreq[char] = windowFreq.get(char, 0) + 1
        
        if char in targetFreq and windowFreq[char] == targetFreq[char]:
            formed++
        
        while formed == required:
            // Update result
            if right - left + 1 < minLen:
                minLen = right - left + 1
                result = s[left:right+1]
            
            // Shrink
            leftChar = s[left]
            windowFreq[leftChar]--
            if leftChar in targetFreq and windowFreq[leftChar] < targetFreq[leftChar]:
                formed--
            left++
    
    return result
```

**Similar Problems:**
- LC 438: Find All Anagrams in a String
- LC 567: Permutation in String
- LC 992: Subarrays with K Different Integers

---

### Combination 5: Two Pointers + Sorting
**Classic Problem:** 3Sum (LC #15)

**Why Two Patterns:**
- Sorting: Enables two-pointer approach, helps skip duplicates
- Two Pointers: Efficient pair finding after fixing one element

**How They Work Together:**
```
Problem: Find all unique triplets summing to zero

Step 1: Sort array
Step 2: For each element nums[i]:
        - Use two pointers on remaining array
        - Find pairs that sum to -nums[i]
Step 3: Skip duplicates using sorted property
```

**Template:**
```
function threeSum(nums):
    sort(nums)
    result = []
    
    for i = 0 to n - 3:
        // Skip duplicates
        if i > 0 and nums[i] == nums[i-1]:
            continue
        
        left = i + 1
        right = n - 1
        target = -nums[i]
        
        while left < right:
            sum = nums[left] + nums[right]
            if sum == target:
                result.add([nums[i], nums[left], nums[right]])
                // Skip duplicates
                while left < right and nums[left] == nums[left+1]: left++
                while left < right and nums[right] == nums[right-1]: right--
                left++
                right--
            else if sum < target:
                left++
            else:
                right--
    
    return result
```

**Similar Problems:**
- LC 16: 3Sum Closest
- LC 18: 4Sum
- LC 259: 3Sum Smaller

---

### Combination 6: Monotonic Stack + DP
**Classic Problem:** Sum of Subarray Minimums (LC #907)

**Why Two Patterns:**
- Monotonic Stack: Find previous/next smaller element efficiently
- DP-like Counting: Calculate contribution of each element

**How They Work Together:**
```
Problem: Sum of minimum elements of all subarrays

For each element arr[i]:
- Find how many subarrays have arr[i] as minimum
- Contribution = arr[i] × count_of_such_subarrays

Monotonic Stack gives:
- prevSmaller[i]: Index of previous smaller element
- nextSmaller[i]: Index of next smaller element

Count of subarrays where arr[i] is minimum:
- left choices: i - prevSmaller[i]
- right choices: nextSmaller[i] - i
- total: left × right
```

**Template:**
```
function sumSubarrayMins(arr):
    n = arr.length
    prevSmaller = [-1] * n
    nextSmaller = [n] * n
    
    // Find previous smaller (monotonic stack)
    stack = []
    for i = 0 to n-1:
        while stack not empty and arr[stack.top()] >= arr[i]:
            stack.pop()
        if stack not empty:
            prevSmaller[i] = stack.top()
        stack.push(i)
    
    // Find next smaller
    stack = []
    for i = n-1 down to 0:
        while stack not empty and arr[stack.top()] > arr[i]:
            stack.pop()
        if stack not empty:
            nextSmaller[i] = stack.top()
        stack.push(i)
    
    // Calculate contributions
    result = 0
    for i = 0 to n-1:
        left = i - prevSmaller[i]
        right = nextSmaller[i] - i
        result += arr[i] * left * right
    
    return result % MOD
```

**Similar Problems:**
- LC 84: Largest Rectangle in Histogram
- LC 85: Maximal Rectangle
- LC 2104: Sum of Subarray Ranges

---

### Combination 7: BFS + State Compression (Bitmask)
**Classic Problem:** Shortest Path to Get All Keys (LC #864)

**Why Two Patterns:**
- BFS: Find shortest path
- Bitmask: Track which keys are collected (state)

**How They Work Together:**
```
Problem: Find shortest path collecting all keys in a grid

State = (row, col, keysCollected)
- keysCollected is a bitmask: bit i = 1 means key 'a'+i is collected

BFS explores states level by level
Same cell with different key sets = different states
Can revisit cell if we have more keys now
```

**Template:**
```
function shortestPathAllKeys(grid):
    // Find start position and count keys
    startRow, startCol, totalKeys = scan grid
    
    // State: (row, col, keys bitmask)
    visited = set()
    queue = [(startRow, startCol, 0, 0)]  // row, col, keys, steps
    
    while queue not empty:
        row, col, keys, steps = queue.dequeue()
        
        // Check if all keys collected
        if keys == (1 << totalKeys) - 1:
            return steps
        
        state = (row, col, keys)
        if state in visited:
            continue
        visited.add(state)
        
        for each neighbor (newRow, newCol):
            cell = grid[newRow][newCol]
            
            if cell is wall:
                continue
            if cell is door and we don't have key:
                continue
            
            newKeys = keys
            if cell is key:
                newKeys = keys | (1 << keyIndex)
            
            queue.enqueue((newRow, newCol, newKeys, steps + 1))
    
    return -1
```

**Similar Problems:**
- LC 847: Shortest Path Visiting All Nodes
- LC 1197: Minimum Knight Moves (with optimization)
- LC 1293: Shortest Path in a Grid with Obstacles Elimination

---

### Combination 8: Union Find + Sorting/Processing Order
**Classic Problem:** Accounts Merge (LC #721)

**Why Two Patterns:**
- Union Find: Group connected components
- HashMap: Map emails to account indices

**How They Work Together:**
```
Problem: Merge accounts with common emails

Step 1: For each email, track first account that uses it
Step 2: If email appears in new account, union with first account
Step 3: Group all emails by their root account
Step 4: Format output
```

**Similar Problems:**
- LC 684: Redundant Connection
- LC 1584: Min Cost to Connect All Points (Kruskal's MST)
- LC 959: Regions Cut By Slashes

---

### Combination 9: Trie + DFS/Backtracking
**Classic Problem:** Word Search II (LC #212)

**Why Two Patterns:**
- Trie: Efficiently store and search word dictionary
- DFS/Backtracking: Explore all paths in grid

**How They Work Together:**
```
Problem: Find all dictionary words in a grid

Trie enables:
- Prefix checking in O(1) per character
- Early termination if prefix doesn't exist
- Efficient handling of multiple words

DFS explores grid paths while checking Trie
```

**Template:**
```
function findWords(board, words):
    // Build Trie from words
    trie = buildTrie(words)
    result = []
    
    function dfs(row, col, trieNode, path):
        if trieNode.isWord:
            result.add(path)
            trieNode.isWord = false  // Avoid duplicates
        
        if out of bounds or visited:
            return
        
        char = board[row][col]
        if char not in trieNode.children:
            return  // Prefix doesn't exist
        
        board[row][col] = '#'  // Mark visited
        
        for each direction:
            dfs(newRow, newCol, trieNode.children[char], path + char)
        
        board[row][col] = char  // Restore
    
    for each cell:
        dfs(row, col, trie.root, "")
    
    return result
```

**Similar Problems:**
- LC 211: Design Add and Search Words Data Structure
- LC 421: Maximum XOR of Two Numbers in an Array
- LC 1268: Search Suggestions System

---

### Combination 10: Heap + Greedy + Custom Sorting
**Classic Problem:** Meeting Rooms II (LC #253)

**Why Three Patterns:**
- Sorting: Process meetings in chronological order
- Heap: Track ongoing meeting end times efficiently
- Greedy: Assign to any available room

**How They Work Together:**
```
Problem: Find minimum meeting rooms needed

Sort by start time
Min-heap tracks end times of ongoing meetings
For each meeting:
- If earliest ending meeting ends before this starts, reuse that room
- Otherwise, need new room
```

**Template:**
```
function minMeetingRooms(intervals):
    if intervals is empty:
        return 0
    
    sort(intervals by start time)
    
    minHeap = new MinHeap()  // Stores end times
    minHeap.add(intervals[0].end)
    
    for i = 1 to intervals.length - 1:
        // If earliest ending meeting ends before current starts
        if minHeap.peek() <= intervals[i].start:
            minHeap.poll()  // Reuse that room
        
        minHeap.add(intervals[i].end)
    
    return minHeap.size()
```

**Similar Problems:**
- LC 1094: Car Pooling
- LC 218: The Skyline Problem
- LC 630: Course Schedule III

---

## 🎯 Pattern Combination Recognition Guide

| If you see... | Likely Combination |
|---------------|-------------------|
| Subarray sum + count/check | Prefix Sum + HashMap |
| Minimize maximum / Maximize minimum | Binary Search + Greedy |
| Grid paths + optimal | DFS + Memoization |
| Substring with conditions | Sliding Window + HashMap |
| k-Sum problems | Sorting + Two Pointers |
| Element contribution to all subarrays | Monotonic Stack + Math |
| Shortest path + multiple states | BFS + Bitmask |
| Grouping by connectivity | Union Find + HashMap |
| Multiple string search in grid | Trie + DFS |
| Scheduling / Resource allocation | Heap + Sorting + Greedy |

---

## Practice Problems by Combination

### Prefix Sum + HashMap
- [ ] LC 560: Subarray Sum Equals K
- [ ] LC 525: Contiguous Array
- [ ] LC 974: Subarray Sums Divisible by K
- [ ] LC 930: Binary Subarrays With Sum

### Binary Search + Greedy
- [ ] LC 410: Split Array Largest Sum
- [ ] LC 875: Koko Eating Bananas
- [ ] LC 1011: Capacity To Ship Packages
- [ ] LC 1482: Minimum Number of Days to Make m Bouquets

### DFS + Memoization
- [ ] LC 329: Longest Increasing Path in a Matrix
- [ ] LC 576: Out of Boundary Paths
- [ ] LC 494: Target Sum (also DP)
- [ ] LC 1239: Maximum Length of a Concatenated String

### Sliding Window + HashMap
- [ ] LC 76: Minimum Window Substring
- [ ] LC 438: Find All Anagrams in a String
- [ ] LC 567: Permutation in String
- [ ] LC 30: Substring with Concatenation of All Words

### BFS + State
- [ ] LC 864: Shortest Path to Get All Keys
- [ ] LC 847: Shortest Path Visiting All Nodes
- [ ] LC 1293: Shortest Path with Obstacles Elimination
- [ ] LC 752: Open the Lock

### Monotonic Stack + Contribution
- [ ] LC 907: Sum of Subarray Minimums
- [ ] LC 2104: Sum of Subarray Ranges
- [ ] LC 84: Largest Rectangle in Histogram
- [ ] LC 85: Maximal Rectangle

---

## Tips for Solving Combination Problems

1. **Identify both patterns first** before coding
2. **Design the interface** between patterns (what does pattern A output that pattern B needs?)
3. **Code and test** each pattern separately if possible
4. **Watch for shared state** - make sure patterns don't interfere

---

*Return to [Main Guide →](./README.md)*
