# Practice Problems with Detailed Solutions

This document contains step-by-step solutions to key problems from each pattern. Study the **thinking process**, not just the code.

---

## 1. Sliding Window: Minimum Window Substring (LC #76 - HARD)

### Problem
Given strings `s` and `t`, find the minimum window in `s` that contains all characters of `t`.

### Step 1: Recognize the Pattern
- Keywords: "substring", "contains all characters", "minimum"
- This is **variable-size sliding window** (finding shortest valid window)
- **Deciding question:** [Q1](./how-to-choose-a-pattern.md#q1-contiguous-or-not) — The answer is a substring, which is contiguous, so a sliding window applies and subsequence DP does not.

### Step 2: Brute Force Analysis
```
For each starting position i:
    For each ending position j >= i:
        Check if s[i:j] contains all chars of t
Time: O(n² × m) where m = length of t
```

### Step 3: Optimization Insight
Instead of checking from scratch each time:
- Maintain frequency map of window
- Expand right to include more characters
- **Shrink left while window is still valid** (to minimize)

### Step 4: Solution
```
function minWindow(s, t):
    if t is empty: return ""
    
    // Build target frequency map
    targetFreq = buildFrequencyMap(t)
    required = targetFreq.size()  // Number of unique chars needed
    
    windowFreq = new Map()
    formed = 0  // Number of unique chars with required frequency
    
    left = 0
    minLen = INFINITY
    minStart = 0
    
    for right = 0 to s.length - 1:
        // Add character from right
        char = s[right]
        windowFreq[char] = windowFreq.getOrDefault(char, 0) + 1
        
        // Check if this char's frequency matches required
        if char in targetFreq and windowFreq[char] == targetFreq[char]:
            formed++
        
        // Try to shrink window while valid
        while formed == required:
            // Update minimum
            if right - left + 1 < minLen:
                minLen = right - left + 1
                minStart = left
            
            // Remove left character
            leftChar = s[left]
            windowFreq[leftChar]--
            if leftChar in targetFreq and windowFreq[leftChar] < targetFreq[leftChar]:
                formed--
            left++
    
    return minLen == INFINITY ? "" : s.substring(minStart, minStart + minLen)
```

### Key Insights:
1. Track "formed" count to know when window is valid in O(1)
2. Shrink **while valid** (not until invalid) for shortest window
3. Update result **before** shrinking

### Complexity: O(n + m) time, O(A) space, where A = alphabet size (both maps hold at most A keys)

---

## 2. Two Pointers: 3Sum (LC #15 - MEDIUM)

### Problem
Find all unique triplets that sum to zero.

### Step 1: Recognize the Pattern
- Looking for triplets (3 elements)
- Sum condition
- "Unique" means handle duplicates
- **Deciding question:** [Q3](./how-to-choose-a-pattern.md#q3-is-the-input-sorted-or-can-i-sort-it) — The output is triplet values, not indices, so sorting is allowed and two pointers replace the Two Sum hashmap.

### Step 2: Approach
Fix one element, then it becomes **Two Sum** on sorted array!

### Step 3: Solution
```
function threeSum(nums):
    sort(nums)
    result = []
    
    for i = 0 to nums.length - 3:
        // Skip duplicates for first element
        if i > 0 and nums[i] == nums[i-1]:
            continue
        
        // Early termination: if smallest is > 0, no solution
        if nums[i] > 0:
            break
        
        target = -nums[i]
        left = i + 1
        right = nums.length - 1
        
        while left < right:
            sum = nums[left] + nums[right]
            
            if sum == target:
                result.add([nums[i], nums[left], nums[right]])
                
                // Skip duplicates for second element
                while left < right and nums[left] == nums[left + 1]:
                    left++
                // Skip duplicates for third element
                while left < right and nums[right] == nums[right - 1]:
                    right--
                
                left++
                right--
            else if sum < target:
                left++
            else:
                right--
    
    return result
```

### Key Insights:
1. **Sort first** - enables two-pointer approach
2. Skip duplicates at **all three positions**
3. After finding a match, skip duplicates **then** move both pointers

### Complexity: O(n²) time, O(1) space (excluding output)

---

## 3. Binary Search on Answer: Koko Eating Bananas (LC #875 - MEDIUM)

### Problem
Koko has `piles` of bananas and `h` hours. Find minimum eating speed `k` to finish all bananas.

### Step 1: Recognize the Pattern
- "Minimum" speed to achieve something
- If speed k works, speed k+1 also works (monotonic!)
- Large search space (speed can be 1 to max(piles))

This is **Binary Search on Answer**!

**Deciding question:** [Q7](./how-to-choose-a-pattern.md#q7-is-feasibility-monotonic-in-the-answer) — If speed k finishes within h hours, every speed above k also finishes, so binary search on the speed replaces trying every speed.

### Step 2: Define Verification Function
```
function canFinish(piles, k, h):
    hours = 0                       // Java: use long; the sum can exceed 2^31 - 1
    for pile in piles:
        hours += (pile + k - 1) / k   // ceil(pile / k) in integer math
    return hours <= h
```

### Step 3: Binary Search
```
function minEatingSpeed(piles, h):
    lo = 1
    hi = max(piles)
    
    while lo < hi:
        mid = lo + (hi - lo) / 2
        
        if canFinish(piles, mid, h):
            hi = mid      // mid works, try smaller
        else:
            lo = mid + 1  // mid doesn't work, need faster
    
    return lo
```

### Key Insights:
1. **Binary search on speed**, not on array
2. `canFinish` is the key verification function
3. Use `lo < hi` pattern with `hi = mid` for finding first valid

### Complexity: O(n log m) where m = max(piles)

---

## 4. Monotonic Stack: Largest Rectangle in Histogram (LC #84 - HARD)

### Problem
Find the largest rectangle area in a histogram.

### Step 1: Key Observation
For each bar at index i:
- Rectangle height = height[i]
- Width = distance between **previous smaller** and **next smaller** elements

**Deciding question:** [Q12](./how-to-choose-a-pattern.md#q12-nearest-greatersmaller-per-element-or-per-window) — Each bar needs its own previous smaller bar and next smaller bar, so a monotonic stack fits, not a sliding-window deque.

### Step 2: Solution
```
function largestRectangleArea(heights):
    n = heights.length
    stack = []  // Stores indices, maintaining increasing heights
    maxArea = 0
    
    for i = 0 to n:  // Note: iterate to n (virtual bar of height 0)
        h = (i == n) ? 0 : heights[i]
        
        while stack not empty and heights[stack.top()] > h:
            height = heights[stack.pop()]
            
            // Width calculation:
            // Left boundary: element before popped (stack.top() after pop)
            // Right boundary: current index i
            width = stack.isEmpty() ? i : i - stack.top() - 1
            
            maxArea = max(maxArea, height * width)
        
        stack.push(i)
    
    return maxArea
```

### Step 3: Trace Through Example
For heights = [2, 1, 5, 6, 2, 3]:
```
i=0: push 0, stack=[0]
i=1: h[0]=2 > h[1]=1, pop 0, width=1, area=2, push 1, stack=[1]
i=2: push 2, stack=[1,2]
i=3: push 3, stack=[1,2,3]
i=4: h[3]=6 > h[4]=2, pop 3, width=1, area=6
     h[2]=5 > h[4]=2, pop 2, width=2, area=10 ← MAX
     push 4, stack=[1,4]
i=5: push 5, stack=[1,4,5]
i=6 (virtual 0): pop all remaining
```

### Key Insights:
1. **Increasing stack** - pop when current is smaller
2. Add **virtual bar of height 0** at end to flush remaining
3. Width = i - stack.top() - 1 (or i if stack empty)

### Complexity: O(n) time, O(n) space

---

## 5. Backtracking: Word Search (LC #79 - MEDIUM)

### Problem
Find if word exists in a grid by moving adjacent cells (no revisit).

### Step 1: Recognize the Pattern
- Search for a path
- Can't revisit cells
- Need to explore all possibilities → **Backtracking**
- **Deciding question:** [Q6](./how-to-choose-a-pattern.md#q6-do-subproblems-repeat) — A call depends on (row, col, index) and on the set of visited cells, so memoizing on (row, col, index) gives wrong answers, and backtracking is the correct tool.

### Step 2: Solution
```
function exist(board, word):
    rows = board.length
    cols = board[0].length
    
    function backtrack(row, col, index):
        // Base case: found entire word
        if index == word.length:
            return true
        
        // Boundary and character check
        if row < 0 or row >= rows or col < 0 or col >= cols:
            return false
        if board[row][col] != word[index]:
            return false
        
        // Mark as visited
        temp = board[row][col]
        board[row][col] = '#'
        
        // Explore all 4 directions
        found = backtrack(row + 1, col, index + 1)
             or backtrack(row - 1, col, index + 1)
             or backtrack(row, col + 1, index + 1)
             or backtrack(row, col - 1, index + 1)
        
        // Restore (backtrack)
        board[row][col] = temp
        
        return found
    
    // Try each cell as starting point
    for row = 0 to rows - 1:
        for col = 0 to cols - 1:
            if backtrack(row, col, 0):
                return true
    
    return false
```

### Key Insights:
1. **Mark visited in-place** by changing to special character
2. **Restore after returning** (the backtrack step!)
3. Short-circuit with `or` - stop exploring if found

### Complexity: O(m × n × 4^L) where L = word length

---

## 6. Graph: Course Schedule II (LC #210 - MEDIUM)

### Problem
Return course order to finish all courses (or empty if impossible).

### Step 1: Recognize the Pattern
- "Order" with dependencies → **Topological Sort**
- "Impossible" → Cycle detection
- **Deciding question:** [Q10](./how-to-choose-a-pattern.md#q10-do-you-need-an-order-or-only-is-there-a-cycle) — The problem asks for a valid order, not only whether a cycle exists, so use topological sort (Kahn's algorithm), which also detects the cycle.

### Step 2: Solution (Kahn's Algorithm)
```
function findOrder(numCourses, prerequisites):
    // Build graph and in-degree array
    graph = new Map()
    inDegree = new array[numCourses] filled with 0
    
    for i = 0 to numCourses - 1:
        graph[i] = []
    
    for [course, prereq] in prerequisites:
        graph[prereq].add(course)
        inDegree[course]++
    
    // Initialize queue with courses having no prerequisites
    queue = []
    for i = 0 to numCourses - 1:
        if inDegree[i] == 0:
            queue.enqueue(i)
    
    // Process
    order = []
    while queue is not empty:
        course = queue.dequeue()
        order.add(course)
        
        for next in graph[course]:
            inDegree[next]--
            if inDegree[next] == 0:
                queue.enqueue(next)
    
    // Check if all courses included (no cycle)
    return order.length == numCourses ? order : []
```

### Key Insights:
1. Edge direction: **prereq → course** (prereq enables course)
2. Process nodes with **in-degree 0** first
3. If result size < n, there's a **cycle**

### Complexity: O(V + E)

---

## 7. Dynamic Programming: Coin Change (LC #322 - MEDIUM)

### Problem
Find minimum coins to make amount. Return -1 if impossible.

### Step 1: Apply DP Framework

**Deciding question:** [Q5](./how-to-choose-a-pattern.md#q5-can-a-locally-best-choice-need-to-be-undone-later) — Greedy fails: with coins {1, 3, 4} and amount 6, greedy takes 4 + 1 + 1 (3 coins), but 3 + 3 uses 2 coins, so use DP.

**State:** dp[a] = minimum coins to make amount a

**Transition:** dp[a] = min(dp[a - coin] + 1) for all coins

**Base Case:** dp[0] = 0 (zero coins for amount 0)

**Order:** Fill from 1 to amount

**Answer:** dp[amount]

### Step 2: Solution
```
function coinChange(coins, amount):
    dp = new array[amount + 1] filled with INFINITY
    dp[0] = 0
    
    for a = 1 to amount:
        for coin in coins:
            if coin <= a and dp[a - coin] != INFINITY:
                dp[a] = min(dp[a], dp[a - coin] + 1)
    
    return dp[amount] == INFINITY ? -1 : dp[amount]
```

### Step 3: Trace for coins=[1,3,4], amount=6
```
dp[0] = 0
dp[1] = min(dp[0]+1) = 1
dp[2] = min(dp[1]+1) = 2
dp[3] = min(dp[2]+1, dp[0]+1) = min(3, 1) = 1
dp[4] = min(dp[3]+1, dp[1]+1, dp[0]+1) = min(2, 2, 1) = 1
dp[5] = min(dp[4]+1, dp[2]+1, dp[1]+1) = min(2, 3, 2) = 2
dp[6] = min(dp[5]+1, dp[3]+1, dp[2]+1) = min(3, 2, 3) = 2
```
Answer: 2 (coins 3+3)

### Key Insights:
1. Greedy fails! (greedy: 4+1+1=3 coins, optimal: 3+3=2 coins)
2. Must check `dp[a - coin] != INFINITY` before using
3. Initialize with INFINITY, not 0

### Complexity: O(amount × coins) time, O(amount) space

---

## 8. DP: Longest Increasing Subsequence (LC #300 - MEDIUM)

### Problem
Find length of longest strictly increasing subsequence.

**Deciding question:** [Q1](./how-to-choose-a-pattern.md#q1-contiguous-or-not) — A subsequence can skip elements, so no window exists, and DP replaces sliding window.

### Approach 1: O(n²) DP
```
function lengthOfLIS(nums):
    n = nums.length
    dp = new array[n] filled with 1  // Each element is LIS of length 1
    
    for i = 1 to n - 1:
        for j = 0 to i - 1:
            if nums[j] < nums[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    
    return max(dp)
```

### Approach 2: O(n log n) with Binary Search
```
function lengthOfLIS(nums):
    // tails[i] = smallest ending element of LIS of length i+1
    tails = []
    
    for num in nums:
        // Find position to insert/replace
        pos = lowerBound(tails, num)  // first index with tails[pos] >= num
        
        if pos == tails.length:
            tails.add(num)  // Extend LIS
        else:
            tails[pos] = num  // Replace to keep smallest tail
    
    return tails.length
```

### Trace for [10, 9, 2, 5, 3, 7, 101, 18]:
```
num=10: tails=[10]
num=9:  tails=[9]      (replace 10 with smaller 9)
num=2:  tails=[2]      (replace 9 with smaller 2)
num=5:  tails=[2,5]    (extend)
num=3:  tails=[2,3]    (replace 5 with smaller 3)
num=7:  tails=[2,3,7]  (extend)
num=101: tails=[2,3,7,101] (extend)
num=18: tails=[2,3,7,18]   (replace 101)
```
Answer: 4

### Key Insight:
`tails` is NOT the actual LIS, but its length equals LIS length!

---

## 9. DP: Edit Distance (LC #72 - MEDIUM)

### Problem
Find minimum operations to convert word1 to word2 (insert, delete, replace).

**Deciding question:** [Q6](./how-to-choose-a-pattern.md#q6-do-subproblems-repeat) — The brute-force recursion on (i, j) reaches the same pair from different branches, so memoize it as a 2D DP table.

### Step 1: State Definition
dp[i][j] = min operations to convert word1[0:i] to word2[0:j]

### Step 2: Transition
```
if word1[i-1] == word2[j-1]:
    dp[i][j] = dp[i-1][j-1]  // No operation needed
else:
    dp[i][j] = 1 + min(
        dp[i-1][j-1],  // Replace word1[i-1] with word2[j-1]
        dp[i-1][j],    // Delete word1[i-1]
        dp[i][j-1]     // Insert word2[j-1]
    )
```

### Step 3: Base Cases
```
dp[0][j] = j  // Insert j characters to convert "" to word2[0:j]
dp[i][0] = i  // Delete i characters to convert word1[0:i] to ""
```

### Full Solution
```
function minDistance(word1, word2):
    m = word1.length
    n = word2.length
    dp = new array[m + 1][n + 1]
    
    // Base cases
    for i = 0 to m: dp[i][0] = i
    for j = 0 to n: dp[0][j] = j
    
    // Fill table
    for i = 1 to m:
        for j = 1 to n:
            if word1[i-1] == word2[j-1]:
                dp[i][j] = dp[i-1][j-1]
            else:
                dp[i][j] = 1 + min(dp[i-1][j-1], dp[i-1][j], dp[i][j-1])
    
    return dp[m][n]
```

### Complexity: O(m × n) time and space

---

## 10. DP: House Robber II (LC #213 - MEDIUM)

### Problem
Houses in a circle. Can't rob adjacent. Find max money.

**Deciding question:** [Q5](./how-to-choose-a-pattern.md#q5-can-a-locally-best-choice-need-to-be-undone-later) — Greedy that takes the largest house first fails: on the circle [1, 3, 4, 3] it takes 4 + 1 = 5, but 3 + 3 = 6, so use DP.

### Key Insight
Circle means first and last are adjacent. Break into two cases:
1. Rob houses [0, n-2] (skip last)
2. Rob houses [1, n-1] (skip first)

Answer = max of both cases

### Solution
```
function rob(nums):
    n = nums.length
    if n == 1: return nums[0]
    
    // Helper: rob linear array
    function robLinear(start, end):
        if start > end: return 0
        if start == end: return nums[start]
        
        prev2 = nums[start]
        prev1 = max(nums[start], nums[start + 1])
        
        for i = start + 2 to end:
            curr = max(prev1, prev2 + nums[i])
            prev2 = prev1
            prev1 = curr
        
        return prev1
    
    return max(robLinear(0, n-2), robLinear(1, n-1))
```

### Key Insight:
Transform circular problem into **two linear problems**!

---

*Continue to [Interview Tips →](./interview-tips.md)*
