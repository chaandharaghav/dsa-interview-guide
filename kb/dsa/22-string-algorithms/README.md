---
priority: P2
---
# Pattern 22: String Algorithms

## 1. Pattern Name

**String Algorithms**: single-pattern matching (KMP prefix function, Z-function, Rabin-Karp rolling hash), palindromes in linear time (Manacher), and substring hashing with binary search ("longest duplicate substring").

This page is a toolbox. Each tool answers a question about the *structure* of one string: where does a pattern occur, which prefixes repeat, which substrings are palindromes, which substrings are equal. Terms are defined where they first appear.

!!! info "Prerequisites"
    Read [Arrays, Strings, and Linked Lists](../foundations/02-arrays-strings-linked-lists.md) for how Java stores a `String` (immutable, compact strings). Rabin-Karp uses modular arithmetic. Pattern [23: Math and Number Theory](../23-math-and-number-theory/README.md) (Template 4) explains `Math.floorMod`, the modulus `1_000_000_007`, and why a product of two residues fits in `long`. This page does not repeat that. Pattern [17: Trie](../17-trie/README.md) is the right tool for *many* patterns. This page covers *one* pattern in one text.

---

## 2. Core Intuition

### What Interviewers Expect You to Think

Naive matching compares the pattern with the text at every start position. When a mismatch happens, it throws away everything it learned and starts again one position later. The algorithms on this page keep what they learned.

**Four ideas cover the page:**

1. **A border is a prefix that is also a suffix.** In `"abacaba"`, `"a"` and `"aba"` are borders (proper prefixes that are also suffixes). If the pattern matched `k` characters and then failed, the next candidate start is the next-longest border of those `k` characters. The pattern tells you this before you read any text. This is KMP.
2. **Compare a string with itself.** The Z-function stores, for every position `i`, how far the substring starting at `i` matches the *prefix* of the string. Matching a pattern becomes "find positions with a Z-value of at least `m`".
3. **Replace string equality with number equality.** A polynomial hash maps a substring to a number. Sliding the window by one character updates the number in O(1). Equal strings always give equal numbers. Different strings usually give different numbers, so you verify a match, or you accept a tiny error probability. This is Rabin-Karp.
4. **A palindrome contains a smaller palindrome that mirrors across its centre.** When the current centre lies inside a known palindrome, its mirror image already tells you a lower bound on its radius. This is Manacher.

### Mental Model

- **KMP** is a pattern that carries its own "if I fail here, restart there" table.
- **Z-function** is the string measuring itself against its own beginning.
- **Rolling hash** is a fingerprint that you update by removing the character that leaves and adding the character that enters.
- **Manacher** is the "palindrome box" idea: the rightmost palindrome seen so far acts as a mirror for the centres inside it.

---

## 3. Recognition Signals

### Keywords in the Problem Statement

- "Find the first occurrence", "all occurrences", "count occurrences of `pattern` in `text`"
- "Longest prefix which is also a suffix", "border", "period", "repeated substring"
- "Longest palindromic substring", "count palindromic substrings", "shortest palindrome by adding characters in front"
- "Longest repeated (duplicate) substring", "two substrings are equal", "distinct substrings"
- "Rotation" of a string, "string made of repeats of a smaller string"
- "Substring with a given hash value" (the statement gives you the hash formula)

### Constraint Hints

- `|text|` and `|pattern|` up to `10^5` or `10^6`: O(n·m) matching is too slow. You need O(n + m).
- "Find the longest X such that X occurs twice": the answer is monotone in length, so binary search on the length with a hash check per length (pattern [6](../06-binary-search-on-answer/README.md)).
- The time limit allows O(n log n) but the substring comparisons number in the millions: compare hashes in O(1).

### Red Flags: These Are Different Problems

- **Many patterns at once** (a dictionary of words): use a trie, or Aho-Corasick (a trie plus KMP-style failure links). See pattern [17](../17-trie/README.md).
- **Edit distance, longest common subsequence, "subsequence" in the statement**: these are dynamic programming, pattern [15](../15-dynamic-programming/README.md). Matching algorithms handle *contiguous* substrings only.
- **Anagrams or permutations of the pattern** (LC 438, 567): a fixed-size window with a frequency count, pattern [1](../01-sliding-window/README.md).
- **"Does `pattern` occur?" with a short pattern and a short text**: `String.indexOf` is enough (Section 4).
- **Regex or wildcard matching** (`?`, `*`): dynamic programming or backtracking, not these algorithms.

---

## 4. Brute Force Thought Process

### The Naive Matcher

```text
for start = 0 to n - m:
    j = 0
    while j < m and text[start + j] == pattern[j]:
        j++
    if j == m: report start

Time:  O((n - m + 1) * m) comparisons in the worst case
Space: O(1)
```

The worst case is the text `aaaa...a` (length `n`) and the pattern `aaa...ab` (length `m`). At every start position the matcher compares `m - 1` equal characters and then fails on the last one. That is `(n - m + 1) * (m - 1)` comparisons. The count is largest near `m = n / 2`, where it is about `n² / 4`. For `n = 10^6` this is `2.5 * 10^11` comparisons.

**The waste:** after a failure at `pattern[j]`, the matcher knows that `text[start..start + j - 1]` equals `pattern[0..j - 1]`. It then moves `start` by one and re-reads those characters.

### What `String.indexOf` Does (JDK 21 Source)

`String.indexOf(String)` and `String.contains(CharSequence)` use the naive method. This is the code in JDK 21.0.2 (`java.lang.StringLatin1.indexOf(byte[], int, byte[], int, int)`; the UTF-16 version in `StringUTF16.indexOfUnsafe` has the same structure with `getChar` in place of the byte read):

```java
static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
    byte first = str[0];
    int max = (valueCount - strCount);
    for (int i = fromIndex; i <= max; i++) {
        // Look for first character.
        if (value[i] != first) {
            while (++i <= max && value[i] != first);
        }
        // Found first character, now look at the rest of value
        if (i <= max) {
            int j = i + 1;
            int end = j + strCount - 1;
            for (int k = 1; j < end && value[j] == str[k]; j++, k++);
            if (j == end) {
                // Found whole string.
                return i;
            }
        }
    }
    return -1;
}
```

What this code does:

- It scans for the first character of the pattern. At each hit, it compares the remaining characters from left to right. At a mismatch, it moves the start by one position and resumes the scan. It keeps no table and does not skip positions.
- **Worst case: O((n - m + 1) · m) comparisons**, the same as the naive matcher. The input `aaa...a` against `aaa...ab` triggers it. **Space: O(1).**
- **Typical case:** if the first character of the pattern is rare in the text, the scan loop skips most positions, and the running time is close to O(n). The JDK documentation does not promise this.
- The JDK 21 `String.indexOf(String)` javadoc states no complexity bound.
- **HotSpot may replace the Java code.** The `indexOf` methods in `StringLatin1` and `StringUTF16` carry `@IntrinsicCandidate`. That annotation means HotSpot is allowed to substitute a hand-written machine-code routine on some CPUs. Neither the annotation nor the javadoc says what that routine does, so the page states no bound for it. The Java source above defines the worst case that the page states: naive search, O((n - m + 1) · m).
- If the text is stored in UTF-16 form and the pattern in Latin-1 form, `StringUTF16.indexOfLatin1Unsafe` runs the same loop. If the text is stored in Latin-1 form and the pattern in UTF-16 form, `String.indexOf` returns `-1` at once without a search (see `String.indexOf(String)` in the source).

### When `String.indexOf` Is Enough

| Situation | Use |
|---|---|
| One search, text up to a few thousand characters | `indexOf` |
| Random-looking text (words, logs), one search | `indexOf`. The first-character scan makes it fast in practice |
| Many searches of the same pattern in a long text, or an adversarial test with repeats (`aaaa...ab`) | KMP or Z (guaranteed O(n + m)) |
| Rotation check or "is `s` built from repeats" | `(s + s).indexOf(...)` is acceptable in an interview. Say that the worst case is not linear, and name KMP as the guarantee |
| Comparing many substrings of one string | Rolling hash with prefix hashes |
| Longest repeated substring, binary search on length | Rolling hash (this page), or a suffix array |

**Recommendation:** in an interview, if the problem is "implement `strStr`" (LC 28), the interviewer wants KMP or Rabin-Karp. If the problem only uses substring search as a step (LC 796), calling `contains` is fine, and you should state the complexity honestly.

---

## 5. Optimization Insight (The "Aha" Moment)

### KMP: The Prefix Function and Its Derivation

**Definition.** For a string `s` of length `n`, `pi[i]` is the length of the longest **proper** prefix of `s[0..i]` that is also a suffix of `s[0..i]`. "Proper" means shorter than `s[0..i]` itself. That length is the length of the longest border of `s[0..i]`.

Example, `s = "abacabab"`:

| i | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|---|
| `s[i]` | a | b | a | c | a | b | a | b |
| `pi[i]` | 0 | 0 | 1 | 0 | 1 | 2 | 3 | 2 |

At `i = 6` the substring is `abacaba` and its longest border is `aba` (length 3). At `i = 7` the substring is `abacabab`. The candidate `aba` extends only if the next character, `s[3] = c`, equals `s[7] = b`. It does not. The next shorter border of `abacaba` is `a` (length 1), and `s[1] = b` equals `s[7] = b`, so `pi[7] = 2`.

**Derivation of the O(n) computation.**

1. **Fact 1: `pi[i] ≤ pi[i - 1] + 1`.** Let `s[0..i]` have a border of length `k > 0`. Remove the last character from both copies of the border. The result is a border of `s[0..i - 1]` of length `k - 1`. So `k - 1 ≤ pi[i - 1]`.
2. **Fact 2: the borders of `s[0..i - 1]` are `pi[i - 1]`, `pi[pi[i - 1] - 1]`, `pi[pi[pi[i - 1] - 1] - 1]`, ... down to 0.** A border of a border is a border of the whole string, and every border is on this chain. The proof is by induction on the border length.
3. **The algorithm.** To compute `pi[i]`, start with `k = pi[i - 1]`. If `s[k] == s[i]`, then the border extends: `pi[i] = k + 1`. If not, set `k = pi[k - 1]` (the next-longest border) and test again. If `k` reaches 0 and `s[0] != s[i]`, then `pi[i] = 0`.
4. **Why this is O(n).** The value `k` rises by at most 1 per index `i` (Fact 1). Each fallback step lowers `k` by at least 1. `k` never goes below 0. So the total number of fallback steps across all `i` is at most `n`. This is an amortized argument (see [Complexity Analysis](../foundations/01-complexity-analysis.md)).

**Matching with the prefix function.** Run the same loop over the text, with `k` as "the number of pattern characters currently matched". On a mismatch, fall back to `pi[k - 1]`. When `k == m`, record a match and fall back to `pi[m - 1]` before reading the next text character, so that overlapping matches are found. The text index never moves backward. The total cost is O(n + m): O(m) for the table and O(n) for the scan, by the same amortized argument.

### Z-Function: The Z-Box

**Definition.** `z[i]` is the length of the longest common prefix of `s` and `s[i..]`. By convention `z[0] = n`.

Example, `s = "aabxaab"`: `z = [7, 1, 0, 0, 3, 1, 0]`. At `i = 4`, the substring `aab` matches the prefix `aab`, so `z[4] = 3`.

**The Z-box.** Keep `[l, r)`, the segment that ends furthest right among all segments `s[i..i + z[i])` found so far. Because the segment matches a prefix, `s[l..r) = s[0..r - l)`.

For a new index `i`:

- If `i ≥ r`, no information exists. Compare `s[z..]` with `s[i + z..]` from `z = 0`.
- If `i < r`, then `s[i..r)` equals `s[i - l..r - l)`. So `z[i] ≥ min(r - i, z[i - l])`. If `z[i - l] < r - i`, the value is exact and the loop that extends it stops at once. If `z[i - l] ≥ r - i`, start with `r - i` and extend by direct comparison.

**Why this is O(n).** Direct comparison only runs past `r`, and each successful comparison moves `r` right by one. `r` never moves left and never exceeds `n`. So the total number of successful comparisons is at most `n`, and each index adds at most one failed comparison.

**Matching with Z.** Build `s = pattern + sep + text`, where `sep` is a character that occurs in neither string. A position `i` in the text part with `z[i] ≥ m` (in fact `z[i] = m`, because `sep` blocks a longer match) is a match. Cost: O(n + m).

**KMP versus Z.** Both are O(n + m) and solve the same problems. Choose the one whose definition you can derive under pressure. The Z-function has a shorter proof. The prefix function gives borders and periods directly.

### Rabin-Karp: The Rolling Hash

For a base `B` and a modulus `M`, the hash of a substring `s[l..r)` of length `L = r - l` is

```text
H(l, r) = s[l]*B^(L-1) + s[l+1]*B^(L-2) + ... + s[r-1]*B^0   (mod M)
```

Sliding the window by one character is O(1):

```text
H(l+1, r+1) = (H(l, r) - s[l] * B^(L-1)) * B + s[r]   (mod M)
```

**Key properties.**

- Equal strings have equal hashes, always. So a hash mismatch proves the strings differ.
- A hash match does **not** prove equality. Two different strings can collide.
- **Collision bound.** Fix two different strings of length at most `L`. Choose `B` uniformly at random in `[0, M)` with `M` prime. The difference of the two hashes is a polynomial in `B` of degree at most `L - 1`, not identically zero. It has at most `L - 1` roots modulo `M`. So the collision probability is at most `(L - 1) / M`. This holds for one pair. With a fixed base, an adversary who knows the base can build colliding strings.
- **Birthday effect.** If you store `N` hashes in a set and `M ≈ 10^9`, the expected number of colliding pairs is about `N² / (2M)`. For `N = 30,000` (the size of the input of LC 1044), that is about `0.45` pairs, so a single-modulus set without verification fails on a noticeable share of inputs. This is why the templates verify or use two moduli.
- **Two kinds of Rabin-Karp.** *Verify on match* (Las Vegas): after a hash match, compare the strings character by character. The answer is always correct. Only the running time can degrade, and the expected extra cost is small. *Trust the hash* (Monte Carlo): skip the comparison and accept a small error probability. Use this only when the hash is wide enough (two moduli), and say so out loud in an interview.
- **Do not use natural `long` overflow as the modulus.** Arithmetic modulo `2^64` breaks in two ways. For an *odd* base, the Thue-Morse strings of length 1024 collide (Section 6, Template 8 gives the proof and a run). For an *even* base, `B^64 ≡ 0 (mod 2^64)`, so the hash ignores every character except the last 64, and any two strings that share their last 64 characters collide.

### Manacher: The Palindrome Box

**Definitions.** `d1[i]` is the number of odd-length palindromes centred at `i` (the radius, counting the centre). The longest one has length `2 * d1[i] - 1`. `d2[i]` is the number of even-length palindromes centred between `i - 1` and `i`. The longest one has length `2 * d2[i]`.

Keep `[l, r]`, the rightmost palindrome found so far. For a new centre `i` inside `[l, r]`, the mirror position is `j = l + r - i`. Because `[l, r]` is a palindrome, the palindrome at `j` reflects to `i`. So `d1[i] ≥ min(d1[j], r - i + 1)`. The `min` stops the mirror from crossing the right edge of the box. Then extend by direct comparison.

**Why this is O(n).** Direct comparison starts at or beyond `r`. Each successful comparison moves `r` right by one. `r` never moves left. So the total work is O(n). The count of all palindromic substrings is `sum(d1) + sum(d2)`.

### Hash Plus Binary Search for "Longest Duplicate Substring"

If a substring of length `L` occurs twice, then a substring of length `L - 1` also occurs twice: cut the last character from both occurrences. So "a duplicate of length `L` exists" is monotone in `L` (true for small `L`, then false). Binary search finds the largest true `L`. Each test hashes all windows of length `L` in O(n) and looks for a repeated hash. Total: O(n log n) expected time, O(n) space.

---

## 6. Generalized Templates

All templates were tested against brute-force versions on random strings (fixed seed 42, 20,000 strings over alphabets of 1 to 3 letters, lengths 1 to 30) before they went on this page.

### Template 1: Prefix Function and KMP Search

```java
// pi[i] = length of the longest proper prefix of s[0..i] that is also a suffix of s[0..i]
static int[] prefixFunction(String s) {
    int n = s.length();
    int[] pi = new int[n];
    for (int i = 1; i < n; i++) {
        int k = pi[i - 1];                               // longest border so far
        while (k > 0 && s.charAt(i) != s.charAt(k)) {
            k = pi[k - 1];                               // try the next-longest border
        }
        if (s.charAt(i) == s.charAt(k)) {
            k++;
        }
        pi[i] = k;
    }
    return pi;
}

// All start indexes where pattern occurs in text, in increasing order. O(n + m).
static List<Integer> kmpSearch(String text, String pattern) {
    List<Integer> result = new ArrayList<>();
    int m = pattern.length();
    if (m == 0) {
        return result;
    }
    int[] pi = prefixFunction(pattern);
    int k = 0;                                           // pattern characters matched
    for (int i = 0; i < text.length(); i++) {
        while (k > 0 && (k == m || text.charAt(i) != pattern.charAt(k))) {
            k = pi[k - 1];                               // k == m: restart after a full match
        }
        if (text.charAt(i) == pattern.charAt(k)) {
            k++;
        }
        if (k == m) {
            result.add(i - m + 1);
        }
    }
    return result;
}
```

- **Time:** O(n + m). **Space:** O(m) for `pi`, plus the result list.
- The empty pattern returns an empty list here. LC 28 defines it as index 0, so handle it before the call if the problem allows it.
- `k == m` in the loop condition handles overlapping matches. Without it, `pattern.charAt(k)` reads past the end.

### Template 2: Borders and Periods From the Prefix Function

```java
// prefixFunction repeated from Template 1 so that this block compiles on its own
static int[] prefixFunction(String s) {
    int n = s.length();
    int[] pi = new int[n];
    for (int i = 1; i < n; i++) {
        int k = pi[i - 1];
        while (k > 0 && s.charAt(i) != s.charAt(k)) {
            k = pi[k - 1];
        }
        if (s.charAt(i) == s.charAt(k)) {
            k++;
        }
        pi[i] = k;
    }
    return pi;
}

// Length of the longest proper prefix that is also a suffix of s (LC 1392). s is not empty.
static int longestBorder(String s) {
    return prefixFunction(s)[s.length() - 1];
}

// Smallest p such that s is the string s[0..p) repeated n/p times. Returns n if none (LC 459).
static int smallestPeriod(String s) {
    int n = s.length();
    int p = n - prefixFunction(s)[n - 1];               // candidate period length
    return n % p == 0 ? p : n;
}
```

- **Why `n - pi[n - 1]`.** A border of length `b` means `s[i] = s[i + (n - b)]` for all valid `i`, so the string has a "period" `n - b` in the loose sense. The shortest such loose period uses the longest border. The string is an exact repetition only if that period divides `n`.
- LC 459 returns `smallestPeriod(s) < s.length()`.

### Template 3: Z-Function and Z Search

```java
// z[i] = length of the longest common prefix of s and s[i..]; z[0] = n
static int[] zFunction(String s) {
    int n = s.length();
    int[] z = new int[n];
    if (n == 0) {
        return z;
    }
    z[0] = n;
    int l = 0, r = 0;                                    // Z-box: s[l..r) equals s[0..r-l)
    for (int i = 1; i < n; i++) {
        if (i < r) {
            z[i] = Math.min(r - i, z[i - l]);            // reuse the mirror value
        }
        while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
            z[i]++;                                      // extend by direct comparison
        }
        if (i + z[i] > r) {
            l = i;
            r = i + z[i];
        }
    }
    return z;
}

// All start indexes of pattern in text. The separator must not occur in either string.
static List<Integer> zSearch(String text, String pattern) {
    List<Integer> result = new ArrayList<>();
    int m = pattern.length();
    if (m == 0) {
        return result;
    }
    String s = pattern + '\u0000' + text;
    int[] z = zFunction(s);
    for (int i = m + 1; i < s.length(); i++) {
        if (z[i] >= m) {
            result.add(i - m - 1);                       // position inside text
        }
    }
    return result;
}
```

- **Time:** O(n + m). **Space:** O(n + m) for the combined string and the `z` array.
- If the strings can contain any `char`, do not use a separator. Compute `z` of the pattern, then run the same loop with the Z-box over the text, stopping each extension at `m`.

### Template 4: Rabin-Karp With Two Moduli and Verification

```java
static final long MOD1 = 1_000_000_007L, MOD2 = 998_244_353L;   // both prime
static final long BASE1 = 911_382_323L, BASE2 = 972_663_749L;   // both below the moduli

// All start indexes where pattern occurs in text. Expected O(n + m), worst case O(n * m).
static List<Integer> rabinKarp(String text, String pattern) {
    List<Integer> result = new ArrayList<>();
    int n = text.length(), m = pattern.length();
    if (m == 0 || m > n) {
        return result;
    }
    long ph1 = 0, ph2 = 0, th1 = 0, th2 = 0;             // pattern and window hashes
    long pow1 = 1, pow2 = 1;                             // BASE^(m-1)
    for (int i = 0; i < m; i++) {
        ph1 = (ph1 * BASE1 + pattern.charAt(i)) % MOD1;
        ph2 = (ph2 * BASE2 + pattern.charAt(i)) % MOD2;
        th1 = (th1 * BASE1 + text.charAt(i)) % MOD1;
        th2 = (th2 * BASE2 + text.charAt(i)) % MOD2;
        if (i > 0) {
            pow1 = pow1 * BASE1 % MOD1;
            pow2 = pow2 * BASE2 % MOD2;
        }
    }
    for (int i = 0; ; i++) {
        if (th1 == ph1 && th2 == ph2 && text.startsWith(pattern, i)) {   // verify on match
            result.add(i);
        }
        if (i + m >= n) {
            break;
        }
        // remove text[i], then append text[i + m]
        th1 = Math.floorMod(th1 - text.charAt(i) * pow1 % MOD1, MOD1);
        th1 = (th1 * BASE1 + text.charAt(i + m)) % MOD1;
        th2 = Math.floorMod(th2 - text.charAt(i) * pow2 % MOD2, MOD2);
        th2 = (th2 * BASE2 + text.charAt(i + m)) % MOD2;
    }
    return result;
}
```

- **Time:** O(n + m) for hashing. Each true match costs O(m) to verify, so a text with `k` matches costs O(n + k·m) in total. Hash collisions that are not matches add an expected cost that is negligible with two moduli.
- **Why `floorMod`:** `th1 - text.charAt(i) * pow1 % MOD1` can be negative. Plain `%` would keep the negative sign. See pattern [23](../23-math-and-number-theory/README.md), Template 4.
- **Why `long`:** each product has two factors below `2^30`, so it is below `2^60` and fits in `long`. A `char` times a residue is also safe.
- **Safer base:** pick the bases at random at program start (for example `3 + rnd.nextInt(MOD - 3)`). A random base gives the `(L - 1) / M` bound from Section 5 even against inputs chosen in advance. The constants above keep the page output reproducible.
- **Remove the verification** only if you accept a Monte Carlo answer. With two moduli near `10^9` the combined hash has about `10^18` values.

### Template 5: Prefix Hashes for O(1) Substring Hash

```java
class PrefixHash {
    static final long M1 = 1_000_000_007L, M2 = 998_244_353L;
    static final long B1 = 911_382_323L, B2 = 972_663_749L;
    final long[] h1, h2, p1, p2;                         // prefix hashes and powers of the bases

    PrefixHash(String s) {
        int n = s.length();
        h1 = new long[n + 1];
        h2 = new long[n + 1];
        p1 = new long[n + 1];
        p2 = new long[n + 1];
        p1[0] = 1;
        p2[0] = 1;
        for (int i = 0; i < n; i++) {
            h1[i + 1] = (h1[i] * B1 + s.charAt(i)) % M1;
            h2[i + 1] = (h2[i] * B2 + s.charAt(i)) % M2;
            p1[i + 1] = p1[i] * B1 % M1;
            p2[i + 1] = p2[i] * B2 % M2;
        }
    }

    // Hash of s[l..r), half-open. Both moduli packed into one long (each is below 2^30).
    long get(int l, int r) {
        long a = Math.floorMod(h1[r] - h1[l] * p1[r - l] % M1, M1);
        long b = Math.floorMod(h2[r] - h2[l] * p2[r - l] % M2, M2);
        return a << 32 | b;
    }
}
```

- **Build:** O(n) time and space. **Query:** O(1).
- The formula is `H(l, r) = h[r] - h[l] * B^(r - l)`. Prefix `h[l]` carries `B^(r - l)` extra weight in `h[r]`, and the subtraction removes it.
- Use this class when the problem compares many substrings at arbitrary positions (LC 1923, LC 1044 with arbitrary comparisons, palindrome checks with a forward and a reverse hash).

### Template 6: Manacher and Longest Palindromic Substring

```java
// Longest palindromic substring in O(n). d1 handles odd lengths, d2 handles even lengths.
static String longestPalindrome(String s) {
    int n = s.length();
    int[] d1 = new int[n];                               // odd: length 2 * d1[i] - 1
    int l = 0, r = -1;                                   // rightmost palindrome [l, r]
    for (int i = 0; i < n; i++) {
        int k = i > r ? 1 : Math.min(d1[l + r - i], r - i + 1);
        while (i - k >= 0 && i + k < n && s.charAt(i - k) == s.charAt(i + k)) {
            k++;
        }
        d1[i] = k;
        if (i + k - 1 > r) {
            l = i - k + 1;
            r = i + k - 1;
        }
    }
    int[] d2 = new int[n];                               // even: length 2 * d2[i], centre i-1|i
    l = 0;
    r = -1;
    for (int i = 0; i < n; i++) {
        int k = i > r ? 0 : Math.min(d2[l + r - i + 1], r - i + 1);
        while (i - k - 1 >= 0 && i + k < n && s.charAt(i - k - 1) == s.charAt(i + k)) {
            k++;
        }
        d2[i] = k;
        if (i + k - 1 > r) {
            l = i - k;
            r = i + k - 1;
        }
    }
    int bestStart = 0, bestLen = 0;
    for (int i = 0; i < n; i++) {
        if (2 * d1[i] - 1 > bestLen) {
            bestLen = 2 * d1[i] - 1;
            bestStart = i - d1[i] + 1;
        }
        if (2 * d2[i] > bestLen) {
            bestLen = 2 * d2[i];
            bestStart = i - d2[i];
        }
    }
    return s.substring(bestStart, bestStart + bestLen);
}
```

- **Time:** O(n). **Space:** O(n).
- **Count of palindromic substrings (LC 647):** `sum(d1[i] + d2[i])`. Each `d1[i]` value counts the odd palindromes centred at `i`, and each `d2[i]` value counts the even ones. The tests above checked this sum against brute force.
- **The alternative with a separator:** insert `#` between all characters (`abc` becomes `#a#b#c#`), then one array handles both parities. The `d1`/`d2` form avoids a second string and the question of whether `#` occurs in the input.
- **Expand-around-centre** (the O(n²) method) is the usual first answer to LC 5. It is accepted for `n ≤ 1000`. Manacher is the answer to the follow-up "can you do it in linear time?".

### Template 7: Longest Duplicate Substring (Hash and Binary Search)

```java
static final long MOD = 1_000_000_007L, BASE = 911_382_323L;

// Longest substring that occurs at least twice (occurrences may overlap); "" if none (LC 1044)
static String longestDupSubstring(String s) {
    int n = s.length();
    int lo = 1, hi = n - 1, bestStart = -1, bestLen = 0;
    while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        int start = findDuplicate(s, mid);               // start of the second occurrence, or -1
        if (start >= 0) {
            bestStart = start;
            bestLen = mid;
            lo = mid + 1;                                // a longer duplicate may exist
        } else {
            hi = mid - 1;
        }
    }
    return bestStart < 0 ? "" : s.substring(bestStart, bestStart + bestLen);
}

// Start of a window of the given length that equals an earlier window, or -1.
static int findDuplicate(String s, int len) {
    int n = s.length();
    long pow = 1;                                        // BASE^(len-1)
    for (int i = 1; i < len; i++) {
        pow = pow * BASE % MOD;
    }
    long h = 0;
    for (int i = 0; i < len; i++) {
        h = (h * BASE + s.charAt(i)) % MOD;
    }
    Map<Long, List<Integer>> seen = new HashMap<>();     // hash -> start indexes
    seen.computeIfAbsent(h, x -> new ArrayList<>()).add(0);
    for (int i = 1; i + len <= n; i++) {
        h = Math.floorMod(h - s.charAt(i - 1) * pow % MOD, MOD);
        h = (h * BASE + s.charAt(i + len - 1)) % MOD;
        List<Integer> bucket = seen.computeIfAbsent(h, x -> new ArrayList<>());
        for (int j : bucket) {
            if (s.regionMatches(j, s, i, len)) {         // verify: a hash match can be a collision
                return i;
            }
        }
        bucket.add(i);
    }
    return -1;
}
```

- **Time:** O(n log n) expected: `log n` binary search steps, each O(n) hashing plus the expected cost of verifying hash matches. **Space:** O(n) for the map.
- **Why the buckets store lists:** with a single modulus and `n = 3 * 10^4`, collisions between different windows are likely (Section 5, birthday effect). Verification with `regionMatches` makes the answer correct. A bucket with a false match costs one O(len) comparison.
- **Worst case:** a string like `aaaa...a` makes every window equal, and the first match returns at once, so this input is cheap. The slow case is many hash collisions between different windows, which two moduli or a random base make improbable.
- **Alternative:** a suffix array with an LCP array solves this exactly in O(n log n) with no probability. It is outside this page.

### Template 8: Why Natural Overflow Breaks (Runnable Demo)

```java
public class ThueMorseDemo {
    static long hash(String s, long base) {              // arithmetic modulo 2^64 (no modulus)
        long h = 0;
        for (char c : s.toCharArray()) {
            h = h * base + c;
        }
        return h;
    }

    public static void main(String[] args) {
        String a = "a", b = "b";
        for (int i = 0; i < 10; i++) {                   // Thue-Morse construction
            String na = a + b, nb = b + a;
            a = na;
            b = nb;
        }
        System.out.println("length " + a.length() + ", strings equal: " + a.equals(b));
        for (long base : new long[] {31, 131, 911_382_323L, 1_000_003}) {
            System.out.println("base " + base + ", same hash: " + (hash(a, base) == hash(b, base)));
        }
    }
}
```

<!-- expected-output -->
```text
length 1024, strings equal: false
base 31, same hash: true
base 131, same hash: true
base 911382323, same hash: true
base 1000003, same hash: true
```

The two strings differ at every position, and they have the same overflow hash for each of the four (odd) bases tested. This is the reason to use a prime modulus.

**Why it works for every odd base.** Write `A_0 = "a"`, `B_0 = "b"`, `A_{k+1} = A_k B_k`, `B_{k+1} = B_k A_k`. Let `D_k = H(A_k) - H(B_k)`, with `H` the polynomial hash in base `x` (no modulus, so the value is reduced modulo `2^64` by `long` overflow). Appending `B_k` after `A_k` gives `H(A_k B_k) = H(A_k) * x^L + H(B_k)` with `L = 2^k`, and likewise for `B_k A_k`. Subtracting gives `D_{k+1} = (x^(2^k) - 1) * D_k`, and `D_0 = -1` (the characters `a` and `b` differ by 1). So

```text
D_k = -(x - 1)(x^2 - 1)(x^4 - 1) ... (x^(2^(k-1)) - 1)
```

For odd `x`, every factor is even, and the factor `x^(2^i) - 1` is divisible by `2^(i+2)` for `i ≥ 1` (it is the product of `x - 1` and the factors `x^(2^j) + 1`, and `x^2 - 1` is divisible by 8 for odd `x`). For `k = 10` the powers of two multiply to at least `2^(1 + 3 + 4 + ... + 11) = 2^64`. So `D_10 ≡ 0 (mod 2^64)` for every odd base: the strings of length `2^10 = 1024` collide. This derivation is the one behind the "Thue-Morse attack on hashing modulo 2^64" that competitive-programming write-ups describe. The page proves it above and the run confirms it. I could not open the Codeforces write-up (the site refused the request), so the page cites no source for it. A direct run of the demo with even bases (2, 32, 100) shows no collision, which matches the even-base explanation in Section 5: for those bases a collision needs strings with the same last 64 characters.

---

## 7. Variations of the Pattern

### Variation A: Shortest Palindrome (LC 214) with the Prefix Function

**Problem Type:** Add characters in front of `s` to make the shortest palindrome.
**Insight:** The answer keeps the longest palindromic *prefix* of `s` and mirrors the rest. Build `t = s + "#" + reverse(s)`. The last value of `prefixFunction(t)` is the length of the longest prefix of `s` that equals a suffix of `reverse(s)`, which is the longest palindromic prefix of `s`. The separator stops the border from crossing the middle.

```java
// prefixFunction repeated from Template 1 so that this block compiles on its own
static int[] prefixFunction(String s) {
    int n = s.length();
    int[] pi = new int[n];
    for (int i = 1; i < n; i++) {
        int k = pi[i - 1];
        while (k > 0 && s.charAt(i) != s.charAt(k)) {
            k = pi[k - 1];
        }
        if (s.charAt(i) == s.charAt(k)) {
            k++;
        }
        pi[i] = k;
    }
    return pi;
}

static String shortestPalindrome(String s) {
    String t = s + "#" + new StringBuilder(s).reverse();
    int keep = prefixFunction(t)[t.length() - 1];       // length of the longest palindromic prefix
    return new StringBuilder(s.substring(keep)).reverse() + s;
}
```

**Complexity:** O(n) time and space. The character `#` must not occur in `s`. For LC 214 the input has lowercase letters only.

### Variation B: Longest Happy Prefix (LC 1392)

The answer is `s.substring(0, pi[n - 1])`. This is Template 2.

### Variation C: Repeated Substring Pattern (LC 459) and Rotate String (LC 796)

- LC 459: `(s + s).indexOf(s, 1) != s.length()`. If `s` is made of repeats, `s` also occurs inside `s + s` at an offset smaller than `n`. The KMP form is `smallestPeriod(s) < s.length()` (Template 2), which has a linear worst-case guarantee.
- LC 796: `s.length() == goal.length() && (s + s).contains(goal)`. Every rotation of `s` is a substring of `s + s`, and the length check removes false matches.

### Variation D: Repeated String Match (LC 686)

Find the smallest `k` such that `b` is a substring of `a` repeated `k` times. If `b` fits, it fits within `ceil(|b| / |a|)` or one more copy, because `b` starts inside the first copy of `a`. Check `k = ceil(|b| / |a|)`, then `k + 1`, then return `-1`.

```java
static int repeatedStringMatch(String a, String b) {
    int k = (b.length() + a.length() - 1) / a.length();  // ceil(|b| / |a|)
    StringBuilder sb = new StringBuilder(a.repeat(k));
    if (sb.indexOf(b) >= 0) {
        return k;
    }
    sb.append(a);
    return sb.indexOf(b) >= 0 ? k + 1 : -1;
}
```

`StringBuilder.indexOf` runs the same naive search as `String.indexOf`. Use `kmpSearch` if the worst case matters.

### Variation E: Fixed-Length Window Hashes (LC 187)

"Repeated DNA sequences" asks for all 10-letter substrings that occur more than once. The alphabet has 4 letters, so a 10-letter window fits in 20 bits (2 bits per letter). This is a rolling hash with base 4 and no collisions. Put the codes in a `HashSet`. Time O(n).

### Variation F: A Hash With a Given Formula (LC 2156)

The statement defines `hash(s, p, m) = sum(val(s[i]) * p^i) mod m`, with the *lowest* power on the first character. The values `val` run from 1 for `a` to 26 for `z` (never 0). Slide the window from the right end to the left. Then the new character enters at power 0, and the character that leaves is the old rightmost one at power `k - 1`. The modulus need not be prime, because the update uses no modular inverse. Reduce `power` modulo `modulo` first, because both can be up to `10^9`. The update is `h = (h - val(leaving) * p^(k-1)) * p + val(entering)`, all modulo `m`. This is the usual rolling hash in the opposite direction. Use `long` and `floorMod`. The answer is the *first* matching window from the left, so keep overwriting the best index while you slide: the last match found is the leftmost.

### Variation G: Common Substring Across Several Strings (LC 1923)

"Longest common subpath" of several paths of integers. Binary search on the length `L` (monotone: a common subpath of length `L` has common subpaths of every shorter length). For each path, collect the set of hashes of its windows of length `L`, and intersect the sets. The upper bound of the search is the length of the shortest path. Use a rolling hash on integer arrays with two moduli, and map city `c` to `c + 1` so that no value is 0. A false match makes a wrong "common" decision, and there is no cheap verification across many paths. Total input size is at most `10^5`, so each test of one length is O(total size) and the whole search is O(total size · log(shortest path)).

### Variation H: Aho-Corasick (Awareness Only)

Many patterns at once: a trie plus failure links, which are the prefix function generalized to a trie. The failure link of a node points to the node for the longest proper suffix of its string that is also a prefix of some pattern. A breadth-first pass over the trie builds the links. Time O(total text + total pattern + number of matches). The usual interview answer to "search for a dictionary of words in a text" is a trie walk from each start (pattern [17](../17-trie/README.md)), and Aho-Corasick is the follow-up.

---

## 8. Classic Interview Problems

Difficulties come from the LeetCode problem list (`https://leetcode.com/api/problems/all/`), saved on the day of writing.

| # | Problem | Difficulty | Key insight |
|---|---------|-----------|-------------|
| 28 | Find the Index of the First Occurrence in a String | Easy | `indexOf` solves it, but interviewers expect KMP (Template 1) or Rabin-Karp (Template 4). Handle the empty needle |
| 459 | Repeated Substring Pattern | Easy | `n - pi[n - 1]` divides `n` (Template 2), or `(s + s).indexOf(s, 1) < n` |
| 796 | Rotate String | Easy | `goal` is a rotation of `s` if the lengths are equal and `goal` occurs in `s + s` |
| 686 | Repeated String Match | Medium | Try `ceil(|b| / |a|)` copies and one more. Use KMP for a guaranteed bound |
| 5 | Longest Palindromic Substring | Medium | Expand around each centre in O(n²), or Manacher in O(n) (Template 6) |
| 647 | Palindromic Substrings | Medium | Count = `sum(d1) + sum(d2)` from Manacher. Expand-around-centre is O(n²) |
| 187 | Repeated DNA Sequences | Medium | Fixed 10-letter window, 2 bits per letter, `HashSet` of codes |
| 1392 | Longest Happy Prefix | Hard | The answer is `pi[n - 1]`. A hash comparison of prefix and suffix at each length also works (Template 5) |
| 214 | Shortest Palindrome | Hard | Prefix function of `s + "#" + reverse(s)` (Variation A) |
| 1044 | Longest Duplicate Substring | Hard | Binary search on the length, rolling hash per length, verify matches (Template 7) |
| 2156 | Find Substring With Given Hash Value | Hard | Rolling hash from the right end. The powers run in the opposite direction (Variation F) |
| 1923 | Longest Common Subpath | Hard | Binary search on length. Intersect window-hash sets across paths (Variation G) |

### Walkthrough: LeetCode 1044, Longest Duplicate Substring

**Step 1, restate.** Given a string `s` with `2 ≤ n ≤ 3 * 10^4`, return any longest substring that occurs at least twice. Occurrences may overlap. Return `""` if none exists.
**Step 2, brute force.** For each length from `n - 1` down to 1, put all substrings of that length in a `HashSet<String>`. There are O(n²) windows in total, and building and hashing each `String` costs O(n), so the time is O(n³). The memory is also O(n²) characters for one length. Both are too large for `n = 3 * 10^4`.
**Step 3, observe monotonicity.** If length `L` has a duplicate, then so does `L - 1`. So binary search on `L`.
**Step 4, test one length in O(n).** Use a rolling hash so that each window hash costs O(1) (Template 7).
**Step 5, avoid wrong answers.** The set of `n` hashes in `[0, 10^9 + 7)` has an expected `0.45` colliding pairs at `n = 3 * 10^4`. Store the start indexes in each bucket and verify with `regionMatches`. Or use two moduli.
**Step 6, complexity.** `log n` rounds, each O(n) expected. Total O(n log n). The map holds up to `n` entries.

---

## 9. How to Practice This Pattern Effectively

### Phase 1: Matching (3 problems)
- LC 28 with KMP, then again with Z, then again with Rabin-Karp. Write each from memory.
- LC 459 and LC 796 with both `indexOf` and the prefix function.
- Goal: derive the fallback `k = pi[k - 1]` and state why the total work is O(n).

### Phase 2: Palindromes (3 problems)
- LC 5 with expand-around-centre, then with Manacher.
- LC 647 with the Manacher arrays.
- LC 214 with the prefix function.
- Goal: explain why the mirror value is a lower bound and why `r` only moves right.

### Phase 3: Hashing (3 problems)
- LC 187 (exact hash, no collisions).
- LC 1044 (binary search plus rolling hash with verification).
- LC 2156 (a hash with a given formula).
- Goal: write the `floorMod` slide update without a bug, and state the collision handling.

### Phase 4: Hard variants (3 problems)
- LC 1392, LC 686, LC 1923.

### Self-Check Questions
- Can I compute the prefix function of `"abacabab"` by hand?
- Can I state the worst-case input for `String.indexOf` and its cost?
- Can I explain why a hash match needs verification, or why two moduli reduce the risk?
- Can I explain why `L - 1` also has a duplicate when `L` has one?
- Do I know the separator rule in `s + "#" + reverse(s)` and in `pattern + sep + text`?

### Tips
- Test on `aaaa`, `abab`, `aaab`, a single character, and the empty pattern. Repeats expose most bugs in all four algorithms.
- Write a brute-force checker and a random-string test (alphabet of 2 letters). Short strings over a small alphabet produce many borders and many collisions.
- In the interview, name the algorithm and its guarantee: "KMP gives O(n + m) in the worst case. `indexOf` has no such bound."

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Not Falling Back After a Full Match in KMP
```text
// WRONG: after k == m, the next comparison reads pattern[m]  (out of range)
if (k == m) { record; }

// CORRECT: when k == m, set k = pi[m - 1] before the next character
while (k > 0 && (k == m || mismatch)) k = pi[k - 1];
```
Without the fallback, overlapping matches (`aa` in `aaaa`) are lost or the code throws.

### Mistake 2: Falling Back to `pi[k]` Instead of `pi[k - 1]`
`pi[k - 1]` is the border of the *matched* `k` characters. `pi[k]` belongs to a string that is one character longer. The wrong index gives wrong answers or an infinite loop.

### Mistake 3: Using the Z-Function Without a Separator
`z` of `text + pattern` mixes the two strings, and `z[i]` can run across the boundary. Put a separator that occurs in neither string between them, or compute `z` of the pattern alone and extend over the text.

### Mistake 4: Trusting a Hash Match
A single modulus near `10^9` collides about once in `10^9` pairs, and a program compares millions of pairs. Verify on match, or use two moduli, and say which one you chose.

### Mistake 5: Natural Overflow as the Modulus
`h = h * 31 + c` on a `long` is hashing modulo `2^64`. Thue-Morse strings of length 1024 collide for every odd base (Template 8), and an even base keeps only the last 64 characters. The `hashCode` of `String` (modulo `2^32`, base 31) is the same kind of hash. By the same argument, Thue-Morse strings of length 128 collide under it (the exponent sum `1 + 3 + 4 + ... + 8 = 34` reaches 32), and `"Aa"` and `"BB"` collide as well. Do not use it as a fingerprint of substrings in a correctness-critical step.

### Mistake 6: Negative Remainders When Removing the Old Character
```text
// WRONG: (h - c * pow) % MOD can be negative
// CORRECT: Math.floorMod(h - c * pow % MOD, MOD)
```
See pattern [23](../23-math-and-number-theory/README.md), Mistake 2.

### Mistake 7: Overflow Before Reducing
`(int) h * base` overflows `int`. Keep hashes in `long`, keep residues below the modulus, and reduce after each multiplication. A residue below `2^30` times a base below `2^30` is below `2^60`.

### Mistake 8: Mapping a Character to 0
If `'a'` maps to 0, then `"a"` and `"aa"` have the same hash. Use `c - 'a' + 1`, or use the raw `char` value as the templates do.

### Mistake 9: Off-by-One in Manacher Indexes
`d1` counts the centre (radius 1 for a single character). `d2[i]` is centred *between* `i - 1` and `i`, so the start is `i - d2[i]` and the length is `2 * d2[i]`. Mixing the two conventions gives palindromes that are one character too long or too short. Test with `"babad"`, `"cbbd"`, and `"a"`.

### Mistake 10: Expecting `indexOf` to Be Linear
`indexOf` is O((n - m + 1) · m) in the worst case (Section 4). A problem with `aaaa...ab`-style tests and `n = 10^5` can time out on a solution that uses `contains` inside a loop.

### Mistake 11: Using Strings With Characters Above `U+FFFF`
Java `char` is a UTF-16 code unit. A supplementary character is two `char` values. All templates here work on `char` positions. For text where code points matter, convert to `int[]` of code points first (`s.codePoints().toArray()`) and adapt the templates to `int[]`.

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| Find a pattern in a text | `String.indexOf` | Is the input small or random, and is a linear worst case not required? Use `indexOf`. Is a guaranteed bound required, or are repeated characters likely in the tests? Use KMP or Z |
| KMP | Z-function | Do you need borders and periods directly? Use the prefix function. Do you need "how far does each suffix match the prefix"? Use Z |
| Search a dictionary of words | Trie, pattern [17](../17-trie/README.md) | Is there more than one pattern? Use a trie (or Aho-Corasick). One pattern: this page |
| Anagram or permutation search (LC 438, 567) | Sliding window, pattern [1](../01-sliding-window/README.md) | Does order matter? If the order may change, it is a frequency window. If order is fixed, it is matching |
| Longest palindromic *subsequence* (LC 516) | Dynamic programming, pattern [15](../15-dynamic-programming/README.md) | Must the characters be contiguous? Substring: Manacher. Subsequence: DP |
| Longest common substring of two strings | Binary search plus hash, or DP | Is `n` up to `10^5`? Use hash plus binary search (like LC 1044). Is `n` up to a few thousand? A DP table is simpler (LC 718) |
| Count equal substrings or compare many substrings | Prefix hashes (Template 5) | Is each comparison between arbitrary positions? Use prefix hashes. Is it between consecutive windows? Use a rolling hash |
| Distinct substrings count | Suffix array or suffix automaton | Does the problem count *all* distinct substrings of a long string? Hash and KMP are not enough. A suffix array with an LCP array is the standard tool. For a short string (a few hundred characters), a `HashSet<String>` of all substrings is enough |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```text
┌───────────────────────────────────────────────────────────────────────────┐
│                     STRING ALGORITHMS CHEAT SHEET                         │
├───────────────────────────────────────────────────────────────────────────┤
│ indexOf:  naive. Worst case O((n - m + 1) * m) (a^n vs a^(m-1)b). No bound │
│           in the javadoc. HotSpot may use an intrinsic: do not assume O(n) │
│ pi[i]:    longest proper prefix of s[0..i] that is also a suffix           │
│           k = pi[i-1]; while (k>0 && s[i]!=s[k]) k = pi[k-1]; k += match   │
│           O(n): k rises at most 1 per step, each fallback lowers it        │
│ KMP:      same loop over the text; at k == m record, then fall back        │
│ period:   p = n - pi[n-1]; exact repeat only if n % p == 0                 │
│ Z:        z[i] = LCP(s, s[i..]). Z-box [l, r): z[i] >= min(r-i, z[i-l])    │
│           search: pattern + sep + text, match where z[i] >= m              │
│ Rolling:  H' = (H - s[l]*B^(L-1)) * B + s[r]  (mod M); floorMod            │
│ Prefix H: H(l, r) = h[r] - h[l] * B^(r-l)                                  │
│ Collide:  P <= (L-1)/M for a random base. Verify, or use two moduli        │
│           never use natural long overflow (odd base: Thue-Morse; even: 64) │
│ Manacher: odd len 2*d1[i]-1, even len 2*d2[i]; mirror j = l + r - i        │
│           count of palindromic substrings = sum(d1) + sum(d2). O(n)        │
│ Dup substr: binary search on length (monotone) + rolling hash. O(n log n)  │
│ Tests:    aaaa, abab, aaab, single char, empty pattern                     │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## Further Reading

Primary sources for the claims on this page:

- JDK 21 source, `java/lang/String.java` (`indexOf(String)`, `contains`), `java/lang/StringLatin1.java` (`indexOf(byte[], int, byte[], int, int)`), and `java/lang/StringUTF16.java` (`indexOfUnsafe`, `indexOfLatin1Unsafe`) in the OpenJDK repository at <https://github.com/openjdk/jdk21u>. Read the loop to see the first-character scan and the absence of any skip table. I read the source of JDK 21.0.2.
- [Java SE 21 `String` javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html): `indexOf`, `contains`, `regionMatches`, `hashCode` (the formula `s[0]*31^(n-1) + ... + s[n-1]`).
- [`jdk.internal.vm.annotation.IntrinsicCandidate`](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/jdk/internal/vm/annotation/IntrinsicCandidate.java): the annotation that marks methods that HotSpot may replace.
- Knuth, Morris, Pratt, "Fast Pattern Matching in Strings", *SIAM Journal on Computing* 6(2), 1977: the original KMP paper.
- Karp, Rabin, "Efficient randomized pattern-matching algorithms", *IBM Journal of Research and Development* 31(2), 1987: the original randomized Rabin-Karp paper (it chooses a random prime modulus). The `(L - 1) / M` bound on this page is the standard root-counting argument for a random base, not a result quoted from that paper.
- Manacher, "A New Linear-Time On-Line Algorithm for Finding the Smallest Initial Palindrome of a String", *Journal of the ACM* 22(3), 1975: the original Manacher paper.
- Cormen, Leiserson, Rivest, Stein, *Introduction to Algorithms* (CLRS), chapter 32, String Matching: the naive matcher, Rabin-Karp, and KMP with the prefix function and its amortized proof.
- Gusfield, *Algorithms on Strings, Trees, and Sequences* (1997), chapter 1: the Z-algorithm and its link to KMP.
- [LeetCode problem list](https://leetcode.com/problems/): the problems in Section 8.
- Secondary (not primary sources): cp-algorithms pages on the [prefix function](https://cp-algorithms.com/string/prefix-function.html), the [Z-function](https://cp-algorithms.com/string/z-function.html), [string hashing](https://cp-algorithms.com/string/string-hashing.html), and [Manacher's algorithm](https://cp-algorithms.com/string/manacher.html). Worked examples and the `d1`/`d2` formulation that Template 6 follows.

---

*Next: [Pattern 23: Math and Number Theory →](../23-math-and-number-theory/README.md)*
