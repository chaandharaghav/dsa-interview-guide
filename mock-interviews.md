# Mock Interview Question Sets

Practice these timed sessions to simulate real interviews.

---

## How to Use

1. **Set a timer** for the indicated time
2. **No looking up solutions** during the session
3. **Speak out loud** as if an interviewer is watching
4. **After time expires:** Review solution, note what you missed

---

## Mock Session 1: Array & String Focus (45 min)

**Target:** Warm up, build confidence

### Problem 1 (20 min): Two Sum II - Sorted Array
**Difficulty:** Medium | **Pattern:** Two Pointers

Given a **sorted** array and a target sum, find indices of two numbers that add up to target.
```
Input: numbers = [2,7,11,15], target = 9
Output: [1,2]
```

**Hints (reveal if stuck):**
<details>
<summary>Hint 1</summary>
Since array is sorted, you can use two pointers instead of hash map.
</details>
<details>
<summary>Hint 2</summary>
Start pointers at both ends. If sum < target, move left up. If sum > target, move right down.
</details>

---

### Problem 2 (25 min): Longest Substring Without Repeating Characters
**Difficulty:** Medium | **Pattern:** Sliding Window

Find the length of the longest substring without repeating characters.
```
Input: s = "abcabcbb"
Output: 3 (The answer is "abc")
```

**Hints:**
<details>
<summary>Hint 1</summary>
Use a sliding window with a set to track characters in current window.
</details>
<details>
<summary>Hint 2</summary>
When you find a duplicate, shrink window from left until no duplicate.
</details>

---

## Mock Session 2: Tree Focus (45 min)

**Target:** Practice tree recursion and DFS/BFS

### Problem 1 (20 min): Validate Binary Search Tree
**Difficulty:** Medium | **Pattern:** Tree DFS

Given a binary tree, determine if it is a valid BST.

**Hints:**
<details>
<summary>Hint 1</summary>
A node's value must be within a valid range based on its ancestors.
</details>
<details>
<summary>Hint 2</summary>
Pass min and max bounds down the recursion.
</details>

---

### Problem 2 (25 min): Binary Tree Level Order Traversal
**Difficulty:** Medium | **Pattern:** Tree BFS

Return level order traversal as a list of lists.
```
Input: root = [3,9,20,null,null,15,7]
Output: [[3],[9,20],[15,7]]
```

**Hints:**
<details>
<summary>Hint 1</summary>
Use BFS with a queue.
</details>
<details>
<summary>Hint 2</summary>
Track level size at the start of each level to know when level ends.
</details>

---

## Mock Session 3: Graph Focus (45 min)

**Target:** Graph traversal mastery

### Problem 1 (20 min): Number of Islands
**Difficulty:** Medium | **Pattern:** Graph DFS/BFS

Given a 2D grid, count the number of islands (connected 1s).
```
Input: grid = [
  ["1","1","0","0","0"],
  ["1","1","0","0","0"],
  ["0","0","1","0","0"],
  ["0","0","0","1","1"]
]
Output: 3
```

**Hints:**
<details>
<summary>Hint 1</summary>
DFS/BFS from each unvisited '1', marking all connected '1's as visited.
</details>

---

### Problem 2 (25 min): Course Schedule
**Difficulty:** Medium | **Pattern:** Topological Sort / Cycle Detection

Can you finish all courses given prerequisites?
```
Input: numCourses = 2, prerequisites = [[1,0]]
Output: true (Take course 0, then course 1)
```

**Hints:**
<details>
<summary>Hint 1</summary>
Model as a directed graph. If there's a cycle, impossible to finish.
</details>
<details>
<summary>Hint 2</summary>
Use topological sort (Kahn's algorithm) or DFS with cycle detection.
</details>

---

## Mock Session 4: DP Focus (60 min)

**Target:** Dynamic Programming pattern recognition

### Problem 1 (25 min): House Robber
**Difficulty:** Medium | **Pattern:** 1D DP

Rob houses without robbing adjacent ones. Maximize money.
```
Input: nums = [2,7,9,3,1]
Output: 12 (Rob houses 0, 2, 4: 2 + 9 + 1 = 12)
```

**Hints:**
<details>
<summary>Hint 1</summary>
dp[i] = max money robbing houses 0 to i
</details>
<details>
<summary>Hint 2</summary>
dp[i] = max(dp[i-1], dp[i-2] + nums[i])
</details>

---

### Problem 2 (35 min): Coin Change
**Difficulty:** Medium | **Pattern:** Unbounded Knapsack

Find minimum coins to make amount.
```
Input: coins = [1,2,5], amount = 11
Output: 3 (5 + 5 + 1)
```

**Hints:**
<details>
<summary>Hint 1</summary>
dp[i] = minimum coins for amount i
</details>
<details>
<summary>Hint 2</summary>
For each amount, try all coins: dp[a] = min(dp[a], dp[a-coin] + 1)
</details>

---

## Mock Session 5: Mixed Hard (60 min)

**Target:** Interview-level difficulty

### Problem 1 (30 min): Merge k Sorted Lists
**Difficulty:** Hard | **Pattern:** Heap / Divide & Conquer

Merge k sorted linked lists into one sorted list.

**Hints:**
<details>
<summary>Hint 1</summary>
Use a min-heap to always get the smallest element.
</details>
<details>
<summary>Hint 2</summary>
Alternative: Divide and conquer - merge pairs, halve the problem.
</details>

---

### Problem 2 (30 min): Word Ladder
**Difficulty:** Hard | **Pattern:** BFS

Transform beginWord to endWord by changing one letter at a time. Find shortest transformation length.
```
Input: beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log","cog"]
Output: 5 ("hit" -> "hot" -> "dot" -> "dog" -> "cog")
```

**Hints:**
<details>
<summary>Hint 1</summary>
This is BFS on an implicit graph. Each word is a node, edges connect words differing by one letter.
</details>
<details>
<summary>Hint 2</summary>
Use pattern matching: "hot" matches "*ot", "h*t", "ho*". Build adjacency via patterns.
</details>

---

## Mock Session 6: Meta/Facebook Style (45 min - 2 problems!)

**Target:** Speed under pressure

### Problem 1 (20 min): Valid Palindrome
Given a string, determine if it is a palindrome considering only alphanumeric characters.
```
Input: s = "A man, a plan, a canal: Panama"
Output: true
```

---

### Problem 2 (20 min): Add Strings
Given two non-negative integers represented as strings, return their sum as a string.
```
Input: num1 = "456", num2 = "77"
Output: "533"
```

**No converting entire string to integer allowed!**

---

## Mock Session 7: Amazon Style (45 min + Behavioral)

### Coding (30 min): Meeting Rooms II
Find minimum number of conference rooms required.
```
Input: intervals = [[0,30],[5,10],[15,20]]
Output: 2
```

### Behavioral (15 min)
Prepare answers using STAR method:

1. **Customer Obsession:** Tell me about a time you went above and beyond for a customer/user.

2. **Ownership:** Describe a project where you took initiative beyond your job description.

3. **Dive Deep:** Tell me about a time you had to debug a complex issue.

---

## Mock Session 8: Google Style (45 min + Follow-ups)

### Main Problem (30 min): LRU Cache
Design a data structure for LRU Cache with O(1) get and put.

### Follow-up Questions (15 min)
After implementing, interviewer asks:

1. "What if we need TTL (time-to-live) for each entry?"
2. "How would you make this thread-safe?"
3. "How would you implement this distributed across multiple machines?"

---

## Self-Assessment Rubric

After each mock session, rate yourself:

| Criteria | 1 (Poor) | 2 (Fair) | 3 (Good) | 4 (Excellent) |
|----------|----------|----------|----------|---------------|
| **Understanding** | Didn't grasp problem | Partial understanding | Good understanding | Asked great clarifying Qs |
| **Approach** | No clear approach | Brute force only | Optimal approach | Multiple approaches discussed |
| **Coding** | Couldn't code | Partial solution | Working code | Clean, bug-free code |
| **Testing** | Didn't test | Basic test | Edge cases | Comprehensive testing |
| **Communication** | Silent | Minimal | Explained well | Excellent narration |
| **Time** | Didn't finish | Finished late | On time | Time to spare |

**Score yourself after each session. Track improvement over time!**

---

## Weekly Mock Schedule

| Day | Session Type | Duration |
|-----|--------------|----------|
| Monday | Arrays/Strings | 45 min |
| Wednesday | Trees/Graphs | 45 min |
| Friday | DP/Mixed | 60 min |
| Sunday | Full Mock (2 problems) | 45 min |

---

*Return to [Main Guide →](./README.md)*
