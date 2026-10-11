# Common Interview Mistakes & How to Avoid Them

Learn from others' mistakes so you don't make them yourself.

---

## 🚫 Technical Mistakes

### Mistake 1: Jumping to Code Immediately

**What happens:**
```
Interviewer: "Find two numbers that sum to target"
Candidate: *starts typing immediately*
```

**Why it's bad:**
- You might miss constraints
- You might choose wrong approach
- You seem impulsive

**Fix:**
> "Before I code, let me understand the problem and discuss my approach..."
> [Ask 2-3 clarifying questions]
> [Propose solution and get buy-in]
> [Then code]

---

### Mistake 2: Not Considering Edge Cases

**Common edge cases people forget:**
- Empty input (`[]`, `""`, `null`)
- Single element (`[5]`)
- Two elements (minimum for pairs)
- All same elements (`[2,2,2,2]`)
- Negative numbers
- Very large numbers (overflow)
- Sorted vs unsorted input

**Fix:**
After proposing approach, say:
> "Let me think about edge cases... What if the array is empty? What if there's only one element?"

---

### Mistake 3: Off-by-One Errors

**Classic examples:**
```
// WRONG: Loop runs one too many times
for (int i = 0; i <= arr.length; i++)

// WRONG: Accessing index out of bounds
arr[mid + 1]  // when mid = arr.length - 1

// WRONG: Window size calculation
windowSize = right - left  // Should be right - left + 1
```

**Fix:**
- Trace through with small examples (3-4 elements)
- Double-check loop bounds
- Verify array access is in bounds

---

### Mistake 4: Mutating Input Unintentionally

**Problem:**
```java
// Sorting modifies original array!
Arrays.sort(nums);
// Now indices don't match original positions

// Modifying grid during DFS
grid[i][j] = 0;  // Can't use grid again
```

**Fix:**
- Ask: "Can I modify the input array?"
- If not sure, make a copy
- If modifying intentionally, mention it

---

### Mistake 5: Integer Overflow

**Dangerous operations:**
```java
// Overflow when calculating mid
int mid = (left + right) / 2;  // Can overflow if left + right > INT_MAX

// Fix:
int mid = left + (right - left) / 2;

// Overflow in multiplication
int result = a * b;  // Use long if needed
long result = (long) a * b;
```

---

### Mistake 6: Not Handling Null/Empty

**Common crash:**
```java
if (head.next == null)  // Crashes if head is null!

// Fix:
if (head == null || head.next == null)
```

---

### Mistake 7: Wrong Data Structure Choice

| Need | Wrong Choice | Right Choice |
|------|--------------|--------------|
| O(1) lookup | Array (O(n)) | HashMap (O(1)) |
| Maintain order + O(1) lookup | HashMap | LinkedHashMap |
| Always get min/max | Array | Heap (PriorityQueue) |
| LIFO operations | Queue | Stack |
| FIFO operations | Stack | Queue |

---

### Mistake 8: Inefficient String Operations

**Problem:**
```java
// O(n²) due to string concatenation
String result = "";
for (char c : chars) {
    result += c;  // Creates new string each time!
}

// Fix: Use StringBuilder
StringBuilder sb = new StringBuilder();
for (char c : chars) {
    sb.append(c);
}
return sb.toString();
```

---

## 🗣️ Communication Mistakes

### Mistake 9: Going Silent

**What happens:**
Candidate thinks for 2 minutes in complete silence.
Interviewer has no idea what's happening.

**Fix:**
Think out loud:
> "I'm considering using a hash map here... let me think about what to store... I could store the value and its index..."

---

### Mistake 10: Not Asking Clarifying Questions

**Bad:**
```
Interviewer: "Find the median of a data stream"
Candidate: *assumes constraints and starts coding*
```

**Good:**
> "A few questions:
> - How many numbers can be added? (Helps choose data structure)
> - Is memory a concern? 
> - How frequently is median queried vs numbers added?"

---

### Mistake 11: Arguing with Interviewer

**Bad:**
> "No, that's not right. My solution IS correct."

**Good:**
> "Let me trace through the example you mentioned... Ah, I see the issue now. Let me fix that."

---

### Mistake 12: Giving Up Too Easily

**Bad:**
> "I don't know how to do this."

**Good:**
> "I'm not immediately sure of the optimal approach, but let me start with what I know... A brute force solution would be... Let me think about how to optimize..."

---

### Mistake 13: Over-Explaining

**Bad:** 5-minute explanation of basic concepts the interviewer already knows.

**Good:** Concise explanations, check for understanding:
> "I'll use a sliding window here—are you familiar with that pattern, or should I explain?"

---

## ⚙️ Process Mistakes

### Mistake 14: Not Testing Your Code

**Bad:**
> "I think this is correct."

**Good:**
> "Let me trace through the example: [walks through step by step]
> Now let me try an edge case: [tests empty input]
> And one more: [tests boundary condition]"

---

### Mistake 15: Poor Time Management

**45-minute interview breakdown:**
- 5 min: Understand problem, ask questions
- 5 min: Discuss approach, get buy-in
- 25 min: Code
- 10 min: Test, discuss

**Signs you're spending too long:**
- More than 10 minutes without writing any code
- Still explaining after 10 minutes
- No time left to test

---

### Mistake 16: Writing Unreadable Code

**Bad:**
```java
public int f(int[] a, int t) {
    int l=0,r=a.length-1;
    while(l<r){int m=(l+r)/2;if(a[m]<t)l=m+1;else r=m;}
    return l;
}
```

**Good:**
```java
public int binarySearch(int[] nums, int target) {
    int left = 0;
    int right = nums.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    return left;
}
```

---

### Mistake 17: Not Optimizing When Asked

**Interviewer:** "Can you do better than O(n²)?"

**Bad response:** "I don't think so."

**Good response:** 
> "Let me think about what's causing the n² complexity... The nested loop is searching for each element. If I use a hash map, I can reduce lookup to O(1), bringing total to O(n)."

---

### Mistake 18: Forgetting to State Complexity

Always end with:
> "The time complexity is O(n) because we visit each element once. Space complexity is O(n) for the hash map."

---

## 📋 Pre-Interview Checklist

### Day Before:
- [ ] Review top patterns
- [ ] Review company-specific focus areas
- [ ] Prepare behavioral stories (for Amazon especially)
- [ ] Test your setup (camera, mic, internet, IDE)
- [ ] Get good sleep!

### 30 Minutes Before:
- [ ] Water nearby
- [ ] Quiet environment
- [ ] Phone on silent
- [ ] Have pen and paper for quick notes
- [ ] Relax, take deep breaths

### During Interview:
- [ ] Clarify before coding
- [ ] State approach before implementing
- [ ] Think out loud
- [ ] Test your solution
- [ ] Discuss complexity

---

## 🎯 Quick Recovery Phrases

When you realize you made a mistake:
> "Actually, I see a bug here. Let me fix that."

When you're stuck:
> "I'm thinking through the constraints... Let me consider what data structure would help here."

When you don't understand the question:
> "Could you clarify what you mean by [X]?"

When you finish early:
> "I've completed the solution. Should I walk through test cases, or would you like to discuss optimizations?"

When asked about something you don't know:
> "I'm not familiar with that specific algorithm, but based on the problem structure, I would approach it by..."

---

## 📈 Improvement Tracker

After each practice session or interview, note:

| Date | Problem/Interview | What Went Wrong | Lesson Learned |
|------|-------------------|-----------------|----------------|
| | | | |
| | | | |
| | | | |

Review weekly and look for patterns in your mistakes!

---

*Return to [Main Guide →](./README.md)*
