# Pattern 05: Hashing & Frequency Maps

## 1. Pattern Name
**Hashing & Frequency Maps** (HashMaps, HashSets, and Counting Techniques)

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

Hashing is about achieving **O(1) average-case lookups** by trading space for time. When you need to:
- Check if something exists
- Count occurrences
- Find pairs/complements
- Track seen elements

...HashMap/HashSet is often the answer.

**The Key Insight:**  
Instead of searching through data repeatedly (O(n) per search), preprocess into a hash structure (O(n) once), then query in O(1).

**Mental Model:**
Think of a HashMap as a magical filing cabinet where you can instantly access any drawer by its label, regardless of how many drawers exist.

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Count", "Frequency", "Occurrences"
- "Find if exists", "Contains", "Duplicates"
- "Two numbers that sum to", "Pair with property"
- "First unique", "Most frequent"
- "Anagram", "Permutation"
- "Group by", "Categorize"

### Constraint Hints:
- Need O(1) lookup after preprocessing
- "Check in O(1) time"
- Large input but need fast queries

### Input/Output Hints:
- Input: Array of elements (often with duplicates)
- Output: Count, boolean, specific element(s)

### When to Use What:
| Structure | Use Case |
|-----------|----------|
| HashSet | Just need existence check |
| HashMap<key, count> | Need to count occurrences |
| HashMap<key, index> | Need to find element position |
| HashMap<key, list> | Need to group elements |

---

## 4. Brute Force Thought Process

### Two Sum Brute Force:
```
Problem: Find indices of two numbers that sum to target

Brute Force Thinking:
- For each element at index i
  - For each element at index j > i
    - Check if arr[i] + arr[j] == target

Time: O(n²)
Space: O(1)
```

### Duplicate Detection Brute Force:
```
Problem: Check if array has duplicates

Brute Force Thinking:
- For each element at index i
  - For each element at index j > i
    - Check if arr[i] == arr[j]

Time: O(n²)
Space: O(1)
```

---

## 5. Optimization Insight (The "Aha" Moment)

### Two Sum Insight:
**"For each number, I know exactly what I'm looking for."**

If I'm at number `x` and need sum = target, I'm looking for `target - x`.
Instead of scanning for it, I can ask: "Have I seen `target - x` before?"

HashMap gives O(1) answer!

### Frequency Counting Insight:
**"Process once, query infinitely."**

Building a frequency map takes O(n), but then any frequency query is O(1).

### Anagram Insight:
**"Same characters, different order."**

Anagrams have identical character frequencies. Compare frequency maps instead of comparing all permutations.

---

## 6. Generalized Templates

### Template 1: Two Sum Pattern
```
function twoSum(arr, target):
    seen = new HashMap()  // value -> index
    
    for i = 0 to n-1:
        complement = target - arr[i]
        
        if seen.contains(complement):
            return [seen[complement], i]
        
        seen[arr[i]] = i
    
    return NOT_FOUND
```

### Template 2: Frequency Map
```
function buildFrequencyMap(arr):
    freq = new HashMap()
    
    for each element in arr:
        freq[element] = freq.getOrDefault(element, 0) + 1
    
    return freq
```

### Template 3: Group Anagrams
```
function groupAnagrams(strings):
    groups = new HashMap()  // sorted_string -> list of originals
    
    for each str in strings:
        key = sort(str)  // or frequency representation
        if key not in groups:
            groups[key] = []
        groups[key].add(str)
    
    return groups.values()
```

### Template 4: First Unique Element
```
function firstUnique(arr):
    freq = buildFrequencyMap(arr)
    
    for each element in arr:  // Second pass, preserve order
        if freq[element] == 1:
            return element
    
    return NOT_FOUND
```

### Template 5: Check Permutation/Anagram
```
function isAnagram(str1, str2):
    if length(str1) != length(str2):
        return false
    
    freq = new HashMap()
    
    // Build from str1
    for each char in str1:
        freq[char] = freq.getOrDefault(char, 0) + 1
    
    // Decrement from str2
    for each char in str2:
        freq[char] = freq.getOrDefault(char, 0) - 1
        if freq[char] < 0:
            return false
    
    return true  // All counts are 0
```

### Template 6: Longest Subarray with K Distinct
```
function longestWithKDistinct(arr, k):
    freq = new HashMap()
    left = 0
    maxLen = 0
    
    for right = 0 to n-1:
        // Add right element
        freq[arr[right]] = freq.getOrDefault(arr[right], 0) + 1
        
        // Shrink while too many distinct
        while freq.size() > k:
            freq[arr[left]] -= 1
            if freq[arr[left]] == 0:
                freq.remove(arr[left])
            left += 1
        
        maxLen = max(maxLen, right - left + 1)
    
    return maxLen
```

### Template 7: Top K Frequent Elements
```
function topKFrequent(arr, k):
    // Step 1: Count frequencies
    freq = buildFrequencyMap(arr)
    
    // Step 2: Bucket sort by frequency
    buckets = new array[n + 1] of lists  // bucket[i] = elements with frequency i
    
    for each (element, count) in freq:
        buckets[count].add(element)
    
    // Step 3: Collect top k from highest buckets
    result = []
    for i = n down to 0:
        for each element in buckets[i]:
            result.add(element)
            if result.size() == k:
                return result
    
    return result
```

---

## 7. Variations of the Pattern

### Variation A: K-Sum Pattern
**Problem Type:** Find k numbers that sum to target
**Key Insight:** Reduce to 2-Sum by fixing k-2 numbers
**Technique:** Recursion + Two Sum for base case

### Variation B: Subarray with Target Property
**Problem Type:** Subarray sum = k (covered in prefix sum too)
**Key Insight:** Combine prefix sum with HashMap
**Technique:** Store prefix sums in map, look for prefix - k

### Variation C: Longest Consecutive Sequence
**Problem Type:** Find longest sequence of consecutive numbers
**Key Insight:** HashSet for O(1) lookup, start counting only from sequence start
**Technique:** Check if num-1 exists; if not, this is a sequence start

### Variation D: Design Problems
**Problem Type:** Design data structure with O(1) operations
**Examples:** LRU Cache, RandomizedSet
**Technique:** Combine HashMap with auxiliary structure (LinkedList, array)

### Variation E: Rolling Hash / Rabin-Karp

The full treatment, with collision handling and KMP as the alternative, is in [Pattern 22](../22-string-algorithms/README.md).
**Problem Type:** Pattern matching, finding repeated substrings
**Key Insight:** Hash substrings for O(1) comparison
**Watch for:** Hash collisions, verify matches

### Variation F: Cyclic Sort / Index-as-Hash
**Recognition trigger:** values in [1..n] (or [0..n]) + O(1) extra space.
**Key Insight:** When values are in [1..n], each value v has a target index: v−1. The array itself works as the hash table, so no HashMap is needed.
**Two techniques:**
1. **Cyclic sort:** swap each value into its target index value−1. Afterward, an index i with nums[i] != i+1 shows that i+1 is missing and nums[i] is a duplicate.
2. **Negation marking:** for each value v, negate nums[v−1] to record "v was seen". Read values with abs(), because an earlier step may have negated them.

```
function cyclicSort(nums):        // values expected in [1..n]
    missing = []
    i = 0
    while i < n:
        v = nums[i]
        // Swap only if v is in range and its target index does not already hold v.
        // (Testing nums[i] != i+1 instead loops forever on duplicates.)
        if v >= 1 and v <= n and nums[v - 1] != v:
            swap(nums[i], nums[v - 1])   // Do not advance i: examine the new nums[i]
        else:
            i = i + 1
    
    for i = 0 to n-1:
        if nums[i] != i + 1:
            missing.add(i + 1)    // nums[i] is a duplicate or out of range
    return missing

function markSeen(nums):          // values in [1..n]
    missing = [], duplicates = []
    for i = 0 to n-1:
        v = abs(nums[i])          // nums[i] may already be negated
        if nums[v - 1] < 0:
            duplicates.add(v)     // v was seen before (LC 442)
        else:
            nums[v - 1] = -nums[v - 1]
    
    for i = 0 to n-1:
        if nums[i] > 0:
            missing.add(i + 1)    // i+1 never appeared (LC 448)
    return missing, duplicates
```
Each swap puts one value at its target index, so cyclic sort does at most n swaps: O(n) time, O(1) extra space.

| Problem | How cyclic sort / index-as-hash applies |
|---------|------------------------------------------|
| Missing Number (LC 268) | Values in [0..n], length n. The target index of v is v. Value n has no slot, so skip it. Sum formula or XOR also work. |
| Find All Numbers Disappeared (LC 448) | Negation marking; positive slots at the end are missing values |
| Find All Duplicates (LC 442) | Negation marking; a slot that is already negative marks a duplicate |
| First Missing Positive (LC 41) | Ignore values ≤ 0 or > n. After cyclic sort, the first i with nums[i] != i+1 gives i+1; if none, the answer is n+1 |
| Find the Duplicate Number (LC 287) | The problem forbids modifying the array, so cyclic sort is not allowed → Floyd's cycle detection ([Pattern 03](../03-fast-slow-pointers/README.md), Template 5) |

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight |
|---------|-------------|
| Two Sum | HashMap for complement lookup |
| Contains Duplicate | HashSet for seen elements |
| Valid Anagram | Frequency map comparison |
| Single Number | XOR or HashMap |
| Intersection of Two Arrays | HashSet intersection |
| Missing Number (LC 268) | Cyclic sort, or sum/XOR |
| Find All Numbers Disappeared in an Array (LC 448) | Negate nums[v-1] to mark v as seen |
| Isomorphic Strings (LC 205) | Two HashMaps for bidirectional mapping |
| Word Pattern (LC 290) | Two-way mapping pattern↔word |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Group Anagrams** | Sorted string as key | 49 |
| **Top K Frequent Elements** | Bucket sort by frequency | 347 |
| **Longest Consecutive Sequence** | Only start from sequence heads | 128 |
| **Longest Substring Without Repeating** | HashMap for last position | 3 |
| **4Sum II** | Split into two groups, count sums | 454 |
| **Subarray Sum Equals K** | Prefix sum in HashMap | 560 |
| **Encode and Decode TinyURL** | HashMap for URL mapping | 535 |
| **LRU Cache** | HashMap + Doubly Linked List | 146 |
| **Insert Delete GetRandom O(1)** | HashMap + Array | 380 |
| **Copy List with Random Pointer** | HashMap for old→new mapping | 138 |
| **Find All Duplicates in an Array** | Negation marking (index-as-hash) | 442 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Minimum Window Substring** | Two frequency maps + valid window | 76 |
| **Substring with Concatenation of All Words** | Word frequency matching | 30 |
| **First Missing Positive** | Array itself as hash table (cyclic sort, Variation F) | 41 |
| **LFU Cache** | HashMap + Frequency buckets | 460 |
| **All O'one Data Structure** | HashMap + Doubly Linked List | 432 |

---

## 9. How to Practice This Pattern Effectively

### Week 1: Foundation
**Day 1:**
- Solve "Two Sum"
- Understand complement lookup pattern

**Day 2:**
- Solve "Contains Duplicate" (HashSet)
- Solve "Valid Anagram" (frequency map)

**Day 3:**
- Solve "Group Anagrams"
- Master the "canonical form as key" technique

**Day 4:**
- Solve "Longest Consecutive Sequence"
- Understand "start from sequence head" optimization

**Day 5:**
- Solve "Top K Frequent Elements"
- Learn bucket sort technique

### Week 2: Advanced Applications
**Day 1:**
- Solve "4Sum II"
- Practice divide-and-conquer with hashing

**Day 2:**
- Solve "LRU Cache"
- Combine HashMap with LinkedList

**Day 3:**
- Solve "Insert Delete GetRandom O(1)"
- Combine HashMap with Array

**Day 4:**
- Solve "Copy List with Random Pointer"
- HashMap for old-to-new node mapping

**Day 5:**
- Review design problems
- Can you implement LRU Cache without hints?

### Self-Check Questions:
- Can I identify when O(1) lookup helps?
- Do I know when to use HashSet vs HashMap?
- Can I combine hashing with other techniques?
- Do I handle hash collisions in my mind?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Not Handling Missing Keys
```
// WRONG: NullPointerException if key doesn't exist
count = map.get(key) + 1

// CORRECT:
count = map.getOrDefault(key, 0) + 1
```

### Mistake 2: Modifying HashMap While Iterating
```
// WRONG: ConcurrentModificationException
for each key in map.keys():
    if someCondition:
        map.remove(key)

// CORRECT: Use iterator with remove, or collect keys first
keysToRemove = []
for each key in map.keys():
    if someCondition:
        keysToRemove.add(key)
for each key in keysToRemove:
    map.remove(key)
```

### Mistake 3: Using Mutable Objects as Keys
```
// WRONG: List as HashMap key - hash changes if list modified
map[someList] = value
someList.add(element)  // Now map lookup fails!

// CORRECT: Convert to immutable or use tuple/string
map[tuple(someList)] = value
```

### Mistake 4: Forgetting to Remove Zero-Count Entries
```
// WRONG for sliding window: Size becomes incorrect
map[char] -= 1
// Map still contains char with count 0

// CORRECT:
map[char] -= 1
if map[char] == 0:
    map.remove(char)
```

### Mistake 5: Checking Existence After Modification
```
// WRONG for Two Sum: Element finds itself
for i = 0 to n-1:
    map[arr[i]] = i  // Add first
    if map.contains(target - arr[i]):  // Might find itself!
        return ...

// CORRECT: Check first, add later
for i = 0 to n-1:
    if map.contains(target - arr[i]):
        return [map[target - arr[i]], i]
    map[arr[i]] = i
```

### Mistake 6: Wrong Equals/HashCode for Custom Objects
```
// WRONG: Custom class without proper equals/hashCode
class Point { int x, y; }
map = new HashMap<Point, Integer>()
map.put(new Point(1, 2), 5)
map.get(new Point(1, 2))  // Returns null! Different object

// CORRECT: Override equals() and hashCode()
class Point {
    int x, y;
    equals(other) { return x == other.x && y == other.y }
    hashCode() { return 31 * x + y }
}
```

### Mistake 7: Not Considering Negative Numbers/Zero
```
// WRONG for "Two Sum": Assuming positive numbers
// If target = 0 and arr = [0, 0], need to handle properly

// WRONG for "Product of Array": Division by zero
// If arr contains 0, can't use product/current_element approach
```

### Mistake 8: HashSet for Order-Sensitive Problems
```
// WRONG: HashSet doesn't preserve insertion order
set = new HashSet()
set.add(3), set.add(1), set.add(2)
// Iteration order is NOT guaranteed to be 3, 1, 2

// CORRECT: Use LinkedHashSet for insertion order
set = new LinkedHashSet()
```

---

## Bonus: Choosing the Right Hash Structure

| Need | Structure | Example |
|------|-----------|---------|
| Just existence | HashSet | Check if seen |
| Count occurrences | HashMap<T, Integer> | Frequency count |
| Store index | HashMap<T, Integer> | Two Sum |
| Store all indices | HashMap<T, List<Integer>> | All occurrences |
| Group items | HashMap<Key, List<T>> | Group anagrams |
| Ordered iteration | LinkedHashMap/LinkedHashSet | LRU Cache |
| Sorted iteration | TreeMap/TreeSet | Range queries |

---

## Design Pattern: Combining HashMap with Other Structures

### LRU Cache = HashMap + Doubly Linked List
```
HashMap: key → ListNode (O(1) access)
DLL: maintains recency order (O(1) move to front, O(1) remove end)
```

### Insert Delete GetRandom = HashMap + ArrayList
```
HashMap: value → index in array (O(1) lookup)
ArrayList: stores values (O(1) random access)

Delete trick: swap with last element, then pop
```

### LFU Cache = HashMap + Frequency Buckets

The tested O(1) template is in [Pattern 26](../26-design-data-structures/README.md).
```
HashMap: key → (value, frequency)
HashMap: frequency → LinkedHashSet of keys
Track minimum frequency separately
```

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Hashmap for "count letters / compare frequencies" | Counting array `int[26]` | Is the key set small and fixed (e.g. 26 lowercase letters)? If yes → counting array, no hashing (LC 242) |
| Hashmap for "find the missing / duplicate values" | Cyclic sort (Variation F) | Are values in [1..n] and is extra space limited to O(1)? If yes → cyclic sort or negation marking (LC 448) |
| Hashmap for "pair with sum = target" | Two pointers (Pattern 02) | Is the array already sorted? If yes → two pointers, O(1) space (LC 167) |
| HashSet for "contains duplicate" | Sorting | Is O(1) extra space required, and may you reorder the input? If yes → sort, then compare neighbors in O(n log n) (LC 217) |
| Hashmap + sort for "top k frequent" | Heap (Pattern 16) or bucket sort | Is the input a stream, or is k much smaller than n? If yes → size-k heap (LC 347) |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                 HASHING & FREQUENCY CHEAT SHEET                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  TWO SUM PATTERN:                                                │
│  - Look for complement = target - current                        │
│  - Check BEFORE adding current to map                            │
│                                                                  │
│  FREQUENCY MAP:                                                  │
│  - map[x] = map.getOrDefault(x, 0) + 1                           │
│  - Remove when count reaches 0 (for size-based logic)            │
│                                                                  │
│  ANAGRAM CHECK:                                                  │
│  - Same length + same frequency map                              │
│  - Sorted string as canonical form                               │
│                                                                  │
│  LONGEST CONSECUTIVE:                                            │
│  - Put all in HashSet                                            │
│  - Only count from sequence START (num-1 not in set)             │
│                                                                  │
│  CYCLIC SORT (values in [1..n], O(1) space):                     │
│  - Swap nums[i] to index nums[i]-1, or negate nums[v-1] to mark  │
│  - Must not modify array (LC 287) → Floyd, Pattern 03            │
│                                                                  │
│  DESIGN PROBLEMS:                                                │
│  - LRU: HashMap + Doubly Linked List                             │
│  - RandomSet: HashMap + ArrayList (swap-delete trick)            │
│                                                                  │
│  GOTCHAS:                                                        │
│  - getOrDefault for missing keys                                 │
│  - Don't modify while iterating                                  │
│  - Mutable objects as keys = trouble                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 06: Binary Search on Answer →](../06-binary-search-on-answer/README.md)*
