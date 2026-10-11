# Pattern 17: Trie (Prefix Tree)

## 1. Pattern Name
**Trie** (also called Prefix Tree or Digital Tree)

Sub-patterns covered:
- Basic Trie — Insert, Search, StartsWith (LC 208)
- Word Dictionary with `.` wildcards (LC 211)
- Word Search II — Trie + DFS on grid (LC 212)
- Longest Common Prefix (LC 14)
- Replace Words / Prefix Replacement (LC 648)
- Maximum XOR of Two Numbers in Array (LC 421) — bit Trie

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

A Trie trades **space for lookup speed**. If you have a dictionary of n words each of length L, finding whether a word or prefix exists naively costs O(n × L) — you scan every word and compare character by character. A Trie collapses that to **O(L) per lookup, regardless of how many words are stored**.

**The Key Insight:**  
Words that share a common prefix share the same path in the tree. "apple", "app", and "application" all travel down a → p → p before branching. You build the shared prefix **once** and reuse it for every word that passes through it. The tree structure is the lookup index.

Think of it like a **filing cabinet with labeled drawers**: instead of opening every folder to find "application", you go directly to drawer A → P → P and look from there. The shared path is already pre-built.

### Why Not Just Use a HashMap?

A HashMap of strings gives O(L) for exact matches (hashing cost), but answering "does any stored word start with 'app'?" requires scanning all keys — back to O(n × L). A Trie answers that question in O(L) because you just walk to the node for 'p' (the third character). If that path exists, a prefix match exists — some stored word passes through it.

### The Two Storage Choices:

1. **Array-based children (size 26):** Fast O(1) child lookup, but wastes space when the character set is sparse. Best for lowercase English letters only.
2. **HashMap-based children:** Space-efficient for large or variable character sets (Unicode, digits + letters, etc.), but slightly slower due to HashMap overhead.

```
Array-based TrieNode:
  children[26]  ← indexed by (char - 'a')
  isEnd: bool

HashMap-based TrieNode:
  children: Map<Character, TrieNode>
  isEnd: bool
```

Use the **array** when the problem guarantees lowercase English letters.  
Use the **HashMap** for anything else (uppercase mix, digits, Unicode, case-insensitive problems).

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "prefix" / "starts with" / "autocomplete" / "type-ahead"
- "word dictionary" / "add word" / "search word"
- "find all words in a board" / "word search"
- "replace with shortest root" / "common prefix"
- "XOR" + "maximize" on integers (bit-level Trie)

### Structural Hints:
- You receive a **list of words** upfront, then answer many **prefix or search queries**
- Multiple search calls after a build phase → preprocessing into Trie pays off
- Grid problem where you need to find **multiple words simultaneously** → Trie beats calling DFS once per word

### Input/Output Hints:
- Input: Array of strings + query strings
- Input: 2D character grid + word list
- Output: Boolean (contains / starts with), list of matches, or modified strings

### RED FLAGS — NOT Trie:
- **Single word search in one string** → KMP or sliding window; no build phase
- **Only need exact match** for a small, static set → `HashSet` is simpler and faster in practice
- **Subsequence** matching (characters not contiguous) → DP, not Trie
- **Suffix** queries ("does any word end with X?") → Suffix Array / Suffix Tree, not plain Trie (or reverse-insert and prefix-query)
- **Anagram / permutation** matching → frequency map, not Trie

---

## 4. Brute Force Thought Process

### The Naive Prefix Search:
```
Problem: Given a list of words and many "startsWith" queries, answer each query.

Brute Force:
  For each query prefix p:
    For each word w in dictionary:
      If w starts with p → return true
  Return false

Time per query: O(n × L)   (n words, average length L)
Total for q queries: O(q × n × L)
Space: O(1) extra
```

### Why It Times Out:
If n = 10^4 words, L = 20, and q = 10^4 queries, that is 2 × 10^9 operations. The problem forces us to **preprocess** the word list.

**The Waste:** Every query re-scans the same words and re-compares the same prefixes. Words "cat", "car", "card", "care" all share "ca" — we compare "ca" from scratch four separate times per query.

### Brute Force for Word Search II (LC 212):
```
For each word in wordList:
  Run DFS on the entire board looking for that word

Time: O(words × rows × cols × 4^L)
If words = 30, board = 12×12, L = 10 → roughly 10^10 operations
```

Running one DFS per word is catastrophic. The Trie version runs one DFS pass and checks all words simultaneously.

---

## 5. Optimization Insight (The "Aha" Moment)

### Insight 1: Shared Prefixes = Shared Work
**"Build the prefix structure once; answer every query in O(L)."**

When you insert "app", "apple", "apply" into a Trie:
- Node 'a' is created once, shared by all three.
- Node 'a→p' is created once, shared by all three.
- Node 'a→p→p' is created once, shared by all three.
- "app" ends at the second 'p' (isEnd = true). Both longer words continue to 'l', then diverge: 'e' vs 'y'.

Querying "startsWith('ap')" walks two steps and returns true in O(2), regardless of having 10^6 words all starting with "ap".

### Insight 2: Word Search II — One DFS to Rule Them All
**"Put all target words in a Trie, then do a single DFS on the grid. At each cell, follow the Trie instead of checking every word separately."**

The Trie acts as a **shared navigator**: the DFS path on the grid mirrors a path in the Trie. If the current prefix doesn't exist in the Trie, we prune the DFS immediately — no point continuing in that direction. This transforms O(words × grid × depth) into O(grid × depth) because all words share one traversal.

### Insight 3: Delete Found Words to Avoid Duplicates
When a word is found during Word Search II, **remove its terminal marker** (and prune empty nodes) from the Trie. This prevents the same word from being added to results twice and makes the Trie smaller as words are collected, speeding up remaining DFS calls.

---

## 6. Generalized Templates

### Template 0: TrieNode Structure
```
// Array-based (lowercase English letters only)
TrieNode:
    children: array of 26 TrieNode references, all null initially
    isEnd: boolean = false

// HashMap-based (any character set)
TrieNode:
    children: HashMap<Character, TrieNode>, empty initially
    isEnd: boolean = false

// For Word Search II: store the word itself at terminal nodes
TrieNode (augmented):
    children: HashMap<Character, TrieNode>
    word: String = null     // non-null means end of a word
```

### Template 1: Basic Trie — Insert, Search, StartsWith (LC 208)
```
class Trie:
    root = new TrieNode()

    function insert(word):
        node = root
        for each char c in word:
            index = c - 'a'                     // or children.get(c) for HashMap
            if node.children[index] is null:
                node.children[index] = new TrieNode()
            node = node.children[index]
        node.isEnd = true

    function search(word):
        node = root
        for each char c in word:
            index = c - 'a'
            if node.children[index] is null:
                return false
            node = node.children[index]
        return node.isEnd                       // must reach an end marker

    function startsWith(prefix):
        node = root
        for each char c in prefix:
            index = c - 'a'
            if node.children[index] is null:
                return false
            node = node.children[index]
        return true                             // just reaching here is enough
```

**Key difference between search and startsWith:** `search` requires `node.isEnd == true` at the last character. `startsWith` only requires that the path exists. Forgetting this distinction is the single most common Trie bug.

### Template 2: Word Dictionary with Wildcards (LC 211)
```
class WordDictionary:
    root = new TrieNode()

    function addWord(word):
        // Identical to Trie insert above
        node = root
        for each char c in word:
            if c not in node.children:
                node.children[c] = new TrieNode()
            node = node.children[c]
        node.isEnd = true

    function search(word):
        return dfsSearch(root, word, 0)

    function dfsSearch(node, word, index):
        if index == length(word):
            return node.isEnd

        c = word[index]

        if c == '.':
            // Try every possible child
            for each child in node.children.values():
                if dfsSearch(child, word, index + 1):
                    return true
            return false
        else:
            if c not in node.children:
                return false
            return dfsSearch(node.children[c], word, index + 1)
```

**Why DFS and not iterative?** The `.` wildcard branches into every child simultaneously — you need to explore all branches. Recursion (or an explicit stack) is the natural fit.

### Template 3: Word Search II — Trie + Grid DFS (LC 212)
```
function findWords(board, words):
    // Step 1: Build a Trie from all target words
    root = new TrieNode()
    for each word in words:
        node = root
        for each char c in word:
            if c not in node.children:
                node.children[c] = new TrieNode()
            node = node.children[c]
        node.word = word                        // store word at terminal

    result = []
    rows = board.rowCount
    cols = board.colCount

    // Step 2: DFS from every cell
    for r = 0 to rows-1:
        for c = 0 to cols-1:
            dfsTrie(board, r, c, root, result)

    return result

function dfsTrie(board, r, c, node, result):
    // Bounds or visited check
    if r < 0 or r >= rows or c < 0 or c >= cols:
        return
    if board[r][c] == '#':                      // already visited this cell
        return

    ch = board[r][c]
    if ch not in node.children:
        return                                  // Trie prune: no word goes here

    nextNode = node.children[ch]

    if nextNode.word is not null:
        result.add(nextNode.word)
        nextNode.word = null                    // mark found; avoid duplicates

    // Mark visited
    board[r][c] = '#'

    // Explore all 4 directions
    dfsTrie(board, r+1, c, nextNode, result)
    dfsTrie(board, r-1, c, nextNode, result)
    dfsTrie(board, r, c+1, nextNode, result)
    dfsTrie(board, r, c-1, nextNode, result)

    // Restore cell
    board[r][c] = ch

    // Optional pruning: remove leaf node from Trie if it has no more words
    if nextNode has no children and nextNode.word is null:
        remove ch from node.children
```

**The pruning matters:** Without removing found words and empty leaf nodes, the DFS revisits terminal nodes repeatedly on large boards. Deletion makes the Trie shrink as you collect words.

### Template 4: Longest Common Prefix (LC 14)
```
function longestCommonPrefix(strs):
    if strs is empty: return ""

    // Option A: Trie approach
    root = new TrieNode()
    for each word in strs:
        insert(root, word)

    // Walk down until we branch or hit a terminal
    prefix = []
    node = root
    while true:
        nonNullChildren = count of non-null entries in node.children
        if nonNullChildren != 1 or node.isEnd:
            break
        (char, child) = the one non-null entry in node.children   // skip null slots
        prefix.append(char)
        node = child
    return join(prefix)

    // Option B: Simpler — sort and compare first vs last string
    sort(strs)
    first = strs[0]
    last  = strs[last]
    i = 0
    while i < length(first) and first[i] == last[i]:
        i++
    return first[0..i-1]
```

**Interview note:** Option B is often preferable for LC 14 — sorting is O(n log n × L) but the code is simpler. The Trie approach is better when you need to answer many such queries over a changing dictionary.

### Template 5: Replace Words / Prefix Replacement (LC 648)
```
function replaceWords(dictionary, sentence):
    // Build Trie from root words
    root = new TrieNode()
    for each rootWord in dictionary:
        insert(root, rootWord)   // same as standard insert

    words = split(sentence, " ")
    result = []

    for each word in words:
        result.append(findShortestRoot(root, word))

    return join(result, " ")

function findShortestRoot(root, word):
    node = root
    for i = 0 to length(word)-1:
        c = word[i]
        if c not in node.children:
            return word                         // no matching root; keep original
        node = node.children[c]
        if node.isEnd:
            return word[0..i]                   // found a root prefix; return it
    return word
```

### Template 6: Maximum XOR — Bit Trie (LC 421)
```
// Each number is stored as 32 bits, MSB first
// At each Trie level, children[0] = bit 0, children[1] = bit 1

function findMaximumXOR(nums):
    root = new TrieNode()

    // Insert all numbers bit by bit
    for each num in nums:
        node = root
        for bit = 31 downto 0:
            b = (num >> bit) & 1
            if node.children[b] is null:
                node.children[b] = new TrieNode()
            node = node.children[b]

    maxXOR = 0

    // For each number, greedily pick the opposite bit at each level
    for each num in nums:
        node = root
        currentXOR = 0
        for bit = 31 downto 0:
            b = (num >> bit) & 1
            want = 1 - b                        // greedy: flip to maximize XOR
            if node.children[want] is not null:
                currentXOR = (currentXOR << 1) | 1
                node = node.children[want]
            else:
                currentXOR = (currentXOR << 1) | 0
                node = node.children[b]
        maxXOR = max(maxXOR, currentXOR)

    return maxXOR
```

---

## 7. Variations of the Pattern

### Variation A: Counting Words With Prefix
**Problem Type:** How many inserted words start with a given prefix?  
**Extension:** Add a `count` field to each TrieNode. Increment it on every insert path (not just at `isEnd`). A prefix query then returns `node.count` at the end of the prefix path.

### Variation B: Deleting Words from a Trie
**Problem Type:** Support delete operation.  
**Approach:** Walk to the terminal node, set `isEnd = false`. Then walk back up recursively and remove nodes that have no children and no `isEnd`. Requires a recursive insert-style traversal or post-order cleanup.

### Variation C: Trie for Autocomplete (Design)
**Problem Type:** Design a search autocomplete system.  
**Extension:** At each node, maintain a sorted list (or bounded min-heap) of the top-k most frequent completions. On insert, propagate the new entry up through all ancestor nodes. Query walks the prefix path and returns the cached list at the final node.

### Variation D: Trie with Wildcard Matching
**Covered in Template 2.** The key extension is that `.` forces DFS branching. If the wildcard is `*` (zero or more characters), the logic becomes more complex — closer to regex matching and requires a DP/memoization layer on top.

### Variation E: Suffix Trie / Suffix Array
**Problem Type:** Does string S contain substring P?  
**Approach:** Insert all suffixes of S into a Trie. Then "startsWith(P)" tells you if P is a substring. In practice, Suffix Arrays with LCP arrays are preferred for space efficiency.

### Variation F: Bit Trie for XOR Problems
**Covered in Template 6.** Whenever the problem involves maximizing or minimizing XOR of two elements from an array, think bit Trie. The greedy "pick the opposite bit" strategy works because XOR is 1 exactly when bits differ.

---

## 8. Classic Interview Problems

### EASY (Warm-up)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Longest Common Prefix** | Sort + compare first/last, or traverse Trie until branch | 14 |
| **Index Pairs of a String** | Scan text with Trie to find all matching word start positions | 1065 |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Implement Trie (Prefix Tree)** | isEnd flag separates search from startsWith | 208 |
| **Design Add and Search Words** | DFS with wildcard `.` branching into all children | 211 |
| **Replace Words** | Find shortest prefix root; walk Trie and stop at first isEnd | 648 |
| **Map Sum Pairs** | Prefix sum via Trie; store values at nodes | 677 |
| **Longest Word in Dictionary** | Insert all words; BFS/DFS through Trie keeping only isEnd paths | 720 |
| **Maximum XOR of Two Numbers in Array** | Bit Trie (32 levels); greedy opposite-bit traversal | 421 |

### HARD (Differentiators)
| Problem | Key Insight | LC # |
|---------|-------------|------|
| **Word Search II** | Trie + grid DFS; prune empty Trie nodes after finding words | 212 |
| **Palindrome Pairs** | Trie of reversed words; complex case analysis | 336 |
| **Word Squares** | Trie to enumerate valid next words given column constraints | 425 |
| **Stream of Characters** | Reversed Trie: insert reversed words, slide window on stream | 1032 |

---

## 9. How to Practice This Pattern Effectively

### Phase 1: Build the Foundation (LC 208)
**Goal:** Get the TrieNode structure in your muscle memory.

- Implement insert, search, and startsWith from scratch **without looking**.
- Do it once with an array of 26, once with a HashMap.
- Know the answer to: "What does `node.isEnd` being false mean at the end of a search?" (It means the path exists as a prefix but not as a complete word — classic confusion point.)

### Phase 2: Extend to Wildcards (LC 211)
**Goal:** Be comfortable switching from iteration to DFS when the query requires branching.

- Notice the structural similarity: the search loop in LC 208 becomes a recursive DFS in LC 211.
- Ask yourself: "Why can't I solve this iteratively?" (Because `.` fans out — you'd need to manage a queue of nodes, which is just BFS and equally valid but less natural.)

### Phase 3: Grid Integration (LC 212)
**Goal:** The hardest application — combining Trie with matrix DFS.

- Solve LC 79 (Word Search I, single word) first if you haven't. Get the grid DFS pattern solid.
- Then overlay the Trie: instead of comparing against a fixed word string with an index, you follow `node.children[ch]`.
- Add the pruning step last: verify correctness first, then add leaf-node deletion for efficiency.

### Phase 4: Problem Variants
- **LC 648** (Replace Words) — straight prefix matching, good confidence builder.
- **LC 421** (Maximum XOR) — entirely different flavor. Practice explaining the bit-level greedy.
- **LC 14** (Longest Common Prefix) — understand both the Trie approach and the sort approach, and explain why you'd choose each.

### Practice Methodology:
1. **First 3 minutes:** Identify Trie. Decide array vs HashMap. Sketch TrieNode on paper.
2. **Next 5 minutes:** Write insert. Write search. Write startsWith. These are boilerplate — they should be fast.
3. **Remaining time:** Write the problem-specific logic (DFS for wildcards, grid DFS for Word Search II, etc.).
4. **After solving:** Trace through a small input manually to verify `isEnd` logic is correct.

### Self-Check Questions:
- Can I implement a Trie from scratch in under 10 minutes?
- Do I know when `search("app")` returns false but `startsWith("app")` returns true?
- Can I describe the pruning strategy for Word Search II and explain why it matters?
- Can I explain the space–time tradeoff between array children and HashMap children?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Forgetting the `isEnd` Flag Entirely
```
// WRONG: TrieNode only has children, no isEnd
// Consequence: search("app") returns true even if only "apple" was inserted

TrieNode:
    children[26]     // missing: isEnd

// CORRECT:
TrieNode:
    children[26]
    isEnd: boolean = false
```

The `isEnd` flag is what distinguishes a word from a prefix. Without it, you cannot differentiate "insert('apple'), search('app')" correctly. This is mistake number one for candidates who implement Trie from memory.

### Mistake 2: `search` Not Checking `isEnd` — Confusing It with `startsWith`
```
// WRONG: returns true for any prefix, not just complete words
function search(word):
    node = root
    for each char c in word:
        if c not in node.children: return false
        node = node.children[c]
    return true         // BUG: should be node.isEnd, not true

// CORRECT:
    return node.isEnd
```

This is the most common runtime error in Trie problems. `startsWith` returns `true` when the loop finishes. `search` returns `node.isEnd`. If you mix these up, LC 208 test cases will catch it immediately — but under interview pressure, it is easy to miss.

### Mistake 3: Mutating the Trie During Word Search II Causes Missed Words
```
// WRONG: setting isEnd = false without thinking about the path
// If two words share a prefix and you null out isEnd prematurely,
// the second word may not be found.

// CORRECT approach:
// Store the word string in node.word instead of using isEnd.
// Set node.word = null when collected.
// This marks "found" without disturbing the structural path for sibling words.
```

LC 212 is notoriously tricky here. The fix is to store the actual word string at the terminal node, collect it, null it out (to prevent duplicates), and continue. Using a boolean `isEnd` and setting it to `false` when found is semantically wrong if the node still serves as a path for other words.

### Mistake 4: Choosing the Wrong Children Structure
```
// WRONG for Unicode/mixed-case: wastes memory, index calculation is incorrect
children: array of 26    // only works for 'a'-'z'

// If problem has uppercase + lowercase, digits, or special chars:
// Array would need size 128 (ASCII) or 65536 (Unicode) — wasteful.

// CORRECT: use HashMap when character set is not strictly lowercase 26 letters
children: HashMap<Character, TrieNode>
```

Always read the problem constraints before writing TrieNode. "All lowercase English letters" → array of 26. Anything else → HashMap. Getting this wrong causes an `ArrayIndexOutOfBoundsException` for characters outside 'a'–'z' (for example, `'A' - 'a'` is -32).

### Mistake 5: Prefix vs Exact Search Confusion in Problem Logic
```
// Problem: "Replace with shortest root in dictionary"
// WRONG: use search() — only matches exact words, not prefixes

function findShortestRoot(word):
    if search(word): return word    // this does NOT find "car" as root of "carrying"

// CORRECT: walk the Trie and stop at the FIRST isEnd encountered
function findShortestRoot(word):
    node = root
    for i = 0 to len(word)-1:
        if word[i] not in node.children: return word
        node = node.children[word[i]]
        if node.isEnd: return word[0..i]   // earliest prefix wins
    return word
```

LC 648 is specifically about finding the **shortest** matching prefix, not an exact match. Using `search()` is flat-out wrong here.

### Mistake 6: Not Pruning Empty Nodes in Word Search II
```
// WRONG: Found a word, but leave the Trie structure intact.
// Impact: The DFS will re-enter this branch on subsequent cell visits
// and add the word to results again (or waste time on dead branches).

// CORRECT: After dfsTrie returns from exploring a cell direction,
// check if the Trie node for that character is now "dead" (no children, no word).
// If dead, remove it from the parent's children map.

if nextNode has no children and nextNode.word is null:
    remove ch from node.children
```

This pruning is what separates a correct-but-TLE solution from an accepted one on large boards with long word lists. Without it, the time complexity degrades back toward the brute-force case.

### Mistake 7: Building the Trie After Queries Instead of Before
```
// WRONG ordering for any Trie design problem:
for each query:
    search in Trie
    insert into Trie    // inserting too late; earlier queries missed earlier words

// CORRECT: always separate the build phase from the query phase
Phase 1: insert all words into Trie
Phase 2: answer all queries
```

In design problems (LC 208, 211) this is explicit. In one-off problems like LC 212 or 648, make sure you build the entire Trie from the word list **before** starting your main algorithm.

---

## Bonus: Interview Script for Trie Problems

When you recognize a Trie problem, here's how to communicate it out loud:

> "This problem involves [prefix matching / searching many words / autocomplete], which is a strong signal for a Trie. The brute-force approach would be scanning every word for each query — O(n × L) per query. A Trie preprocesses the word list so each lookup costs only O(L), the length of the query string, regardless of dictionary size.
>
> I'll build a TrieNode with [26-element array / HashMap of children] — I'll use [array / HashMap] because [the problem guarantees lowercase letters / the character set is broader]. Each node also has an `isEnd` flag to mark complete words.
>
> [For LC 212:] The tricky part is Word Search II — rather than running one DFS per word, I'll build a Trie of all target words and run one DFS over the grid, using the Trie to prune paths that can't lead to any word. When I find a word, I'll null out its terminal marker in the Trie to prevent duplicates and shrink dead branches.
>
> Let me trace a small example before coding..."

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Dictionary of words with lookups | `HashSet<String>` | Does any query ask about a *prefix* ("starts with", autocomplete, shortest root)? No, only exact lookups → HashSet. |
| Find many words in a grid | One DFS per word ([Pattern 8](../08-recursion-backtracking/README.md)) | Is there more than one target word? One word → plain grid DFS (LC 79). Many words → Trie + DFS (LC 212). |
| Find one pattern inside a text | String matching (KMP) or sliding window | Is there a fixed word list to preprocess? No, one pattern and one text → no Trie. |
| Prefix Trie for "does a word end with X?" | Trie of reversed words | Does the query look at the *end* of a word? Yes → insert each word reversed, then query the reversed suffix as a prefix (Stream of Characters, LC 1032). |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```
┌──────────────────────────────────────────────────────────────────────┐
│                        TRIE CHEAT SHEET                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  TRINODE STRUCTURE:                                                  │
│  - children[26]  (lowercase only)  OR  HashMap<Char, TrieNode>      │
│  - isEnd: boolean  (marks complete word, NOT just prefix)            │
│  - word: String    (optional, used in Word Search II)                │
│                                                                      │
│  INSERT:  Walk/create nodes char by char → set isEnd = true          │
│  SEARCH:  Walk nodes char by char → return node.isEnd                │
│  STARTS WITH: Walk nodes char by char → return true if path exists   │
│                                                                      │
│  KEY DISTINCTION:                                                    │
│  search("app")    → node.isEnd  (must be a complete word)            │
│  startsWith("app")→ true        (path existing is enough)            │
│                                                                      │
│  WILDCARD SEARCH (LC 211):                                           │
│  '.' char → DFS into ALL children, OR any child matches              │
│                                                                      │
│  WORD SEARCH II (LC 212):                                            │
│  Build Trie → single grid DFS → follow Trie instead of string index  │
│  Prune: null out word when found; remove dead leaf nodes             │
│                                                                      │
│  RECOGNITION:                                                        │
│  "prefix" / "starts with" / "autocomplete"  →  Trie                 │
│  words list + grid search                   →  Trie + DFS            │
│  wildcard word search                       →  Trie + DFS            │
│  maximize XOR                               →  Bit Trie              │
│                                                                      │
│  NOT TRIE:                                                           │
│  Single word in string  →  KMP / sliding window                      │
│  Exact match only, small set  →  HashSet                             │
│  Subsequence matching  →  DP                                         │
│                                                                      │
│  COMPLEXITY:                                                         │
│  Insert:     O(L)    L = word length                                 │
│  Search:     O(L)                                                    │
│  StartsWith: O(L)                                                    │
│  Space:      O(total chars across all words × branching factor)      │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

*Next: [Pattern 18: Shortest Paths →](../18-shortest-paths/README.md)*
