# Interview Tips & Communication Guide

Master the **soft skills** that separate good coders from great interviewees.

---

## The Interview Timeline (45 minutes)

| Time | Activity | Your Focus |
|------|----------|------------|
| 0-5 min | Problem Understanding | Ask questions, clarify |
| 5-10 min | Approach Discussion | Think aloud, propose solutions |
| 10-35 min | Coding | Write clean code, explain as you go |
| 35-40 min | Testing | Trace through examples |
| 40-45 min | Discussion | Optimizations, follow-ups |

---

## Phase 1: Problem Understanding (Critical!)

### What to Ask EVERY Time:

1. **Input Constraints**
   - "What's the size of the input? This helps me choose the right algorithm."
   - "Can there be negative numbers?"
   - "Is the array sorted? Can it have duplicates?"

2. **Edge Cases**
   - "What if the input is empty?"
   - "What should I return if no solution exists?"
   - "Are there any special cases I should handle?"

3. **Clarify Requirements**
   - "Should I find ALL solutions or just ONE?"
   - "By 'maximum', do you mean the value or the count?"
   - "Does the order of output matter?"

### Example Communication:

> **Interviewer:** "Find two numbers that sum to target."
>
> **You:** "Before I start, let me make sure I understand. 
> - Are the numbers in a sorted array or unsorted?
> - Can I use the same element twice?
> - Is there guaranteed to be exactly one solution, or could there be none or multiple?
> - Should I return indices or the values themselves?
> - What's the size of the array? This will help me choose between O(n²) and O(n) approaches."

---

## Phase 2: Approach Discussion

### The STAR Format for Solutions

**S - State the brute force**
> "The naive approach would be to check all pairs, which is O(n²)."

**T - Tell why it's suboptimal**
> "Given n can be up to 10^5, that's 10^10 operations—too slow."

**A - Articulate the optimization**
> "Instead, I can use a hash map to achieve O(n). As I iterate, I'll check if the complement exists."

**R - Review the complexity**
> "Time: O(n) for one pass. Space: O(n) for the hash map."

### Pattern Recognition Script

> "This problem has [keyword] which suggests [pattern]. Let me verify:
> - The input is [type]
> - We need [output type]
> - The constraint is [constraint]
> 
> Yes, I'll use [pattern] because [reason]."

When two patterns both fit, say the deciding question aloud: "The values can be negative, so a sliding window will not work. I'll use prefix sums with a hashmap." The interviewer then sees that you rejected a pattern for a reason. The deciding questions are in [How to Choose a Pattern](./how-to-choose-a-pattern.md#step-4-ask-the-deciding-questions).

### Example:

> "This problem asks for the 'minimum window containing all characters', which suggests **sliding window**. Let me verify:
> - We're looking at substrings (contiguous)
> - We need the minimum/shortest one
> - We need to satisfy a character frequency condition
>
> Yes, this is a classic variable-size sliding window. I'll expand right to include characters and shrink left while still valid."

---

## Phase 3: Coding

### Best Practices

1. **Start with function signature**
   ```
   "I'll write the function signature first: it takes nums array and target, returns indices..."
   ```

2. **Write helper functions if needed**
   - Keep main logic clean
   - Show good code organization

3. **Use meaningful variable names**
   ```
   // Bad
   int l, r, m;
   
   // Good
   int left, right, mid;
   ```

4. **Explain as you code**
   > "Now I'm initializing my window... Here I'm expanding right... This condition checks if window is valid..."

5. **Handle edge cases explicitly**
   ```
   if (nums == null || nums.length == 0) {
       return -1;  // "Handling empty input first"
   }
   ```

### If You Get Stuck

**Option 1: Talk through examples**
> "Let me trace through a small example to see what's happening..."

**Option 2: Simplify the problem**
> "Let me first solve a simpler version—what if there were only 2 elements?"

**Option 3: Ask for a hint**
> "I'm considering [approach A] and [approach B]. Could you give me a hint about which direction to explore?"

**Option 4: State what you know**
> "I know I need to track [X]. I'm stuck on how to efficiently update [Y]. Let me think about what data structure could help..."

---

## Phase 4: Testing

### Testing Checklist

1. **Trace through given example**
   > "Let me walk through the example: with input [1,2,3] and target 3..."

2. **Try edge cases**
   - Empty input
   - Single element
   - All same elements
   - Maximum/minimum values

3. **Try to break your code**
   > "What if target is larger than any sum? Let me check... yes, I handle that on line 15."

### How to Trace

```
"Let me trace through [2,7,11,15] with target 9:
- i=0: num=2, need=7, map={}, 7 not in map, add {2:0}
- i=1: num=7, need=2, map={2:0}, 2 IS in map! Return [0,1]"
```

---

## Phase 5: Follow-up Questions

### Common Follow-ups and How to Handle Them

**"Can you optimize space?"**
> "Currently using O(n) space. Let me think... if the array is sorted, I can use two pointers with O(1) space."

**"What if input doesn't fit in memory?"**
> "I'd use external sorting or streaming algorithms. Process chunks and merge results."

**"How would you test this in production?"**
> "Unit tests for edge cases, property-based testing for invariants, benchmark tests for performance."

**"What if there are multiple solutions?"**
> "I can modify to collect all solutions instead of returning early. Should I implement that?"

---

## Red Flags to Avoid

### ❌ DON'T:

1. **Start coding immediately**
   - Always discuss approach first

2. **Stay silent while thinking**
   - Think aloud, even if saying "I'm considering..."

3. **Ignore the interviewer's hints**
   - They're trying to help you succeed

4. **Give up without trying**
   - Show resilience and problem-solving attitude

5. **Argue about the problem**
   - If you think there's an issue, ask clarifying questions

6. **Optimize prematurely**
   - Get a working solution first, then optimize

7. **Forget to test**
   - Always trace through at least one example

### ✅ DO:

1. **Ask clarifying questions**
2. **State your thought process**
3. **Start with brute force, then optimize**
4. **Write clean, readable code**
5. **Test your solution**
6. **Discuss tradeoffs**
7. **Be honest about what you don't know**

---

## Communication Templates

### Starting the Problem
> "Let me make sure I understand the problem correctly. [Restate problem]. Did I get that right?"

### Proposing an Approach
> "I'm thinking of using [pattern] because [reason]. This would give us O([time]) time and O([space]) space. Should I proceed with this approach?"

### When You Realize a Mistake
> "Actually, I see an issue here. Let me fix that... [fix it]. Good catch—this is why testing is important."

### When You're Stuck
> "I'm stuck on [specific part]. Let me think about what properties could help... [think aloud]"

### After Completing
> "I've implemented the solution. The time complexity is O([X]) and space is O([Y]). Let me trace through the example to verify..."

---

## Body Language & Presence (Video/Onsite)

1. **Maintain eye contact** (camera for video)
2. **Sit up straight**
3. **Speak clearly and at moderate pace**
4. **Show enthusiasm**—problems should be interesting, not annoying
5. **Smile occasionally**—you're having a technical conversation, not an interrogation

---

## Handling Different Problem Difficulties

### If It Seems Too Easy
- There might be a follow-up
- Make sure you're not missing edge cases
- Discuss complexity and alternatives

### If It Seems Too Hard
- Break it into smaller parts
- Solve a simpler version first
- Ask clarifying questions—maybe you're overcomplicating

### If You've Seen It Before
- Don't rush through—explain your thinking
- You can mention you've seen something similar, but still derive the solution
- Interviewer might ask follow-up variations

---

## Sample Full Interview Dialogue

**Interviewer:** "Given an array of integers, find two numbers that add up to a target."

**You:** "Thanks for the problem. Before I dive in, a few questions:
1. Is the array sorted?
2. Can I use the same element twice?
3. Is there exactly one solution?
4. What's the typical input size?"

**Interviewer:** "Unsorted, can't use same element twice, exactly one solution exists, up to 10^4 elements."

**You:** "Got it. Let me think about approaches.

The brute force would check all pairs—O(n²). With n=10^4, that's 10^8 operations, borderline acceptable but not ideal.

I can optimize using a hash map. As I iterate:
- Calculate complement = target - current number
- Check if complement exists in map
- If yes, return indices; if no, add current to map

This gives O(n) time, O(n) space. Should I code this?"

**Interviewer:** "Yes, please."

**You:** *[Writes code, explains each part]*

"Let me trace through example [2,7,11,15] with target 9:
- i=0: need 7, map empty, add 2→0
- i=1: need 2, map has 2→0, return [0,1]

Looks correct. Edge cases to consider: empty array, no solution... but problem guarantees one solution exists. Want me to add validation anyway?"

**Interviewer:** "The solution looks good. What if we needed O(1) space?"

**You:** "For O(1) space, I'd sort the array first—O(n log n)—then use two pointers from both ends. But since we need original indices, I'd store index pairs before sorting, which still uses O(n) space. 

Alternatively, if indices aren't needed, two pointers achieves O(n log n) time with O(1) space."

---

## Final Checklist Before Interview

- [ ] Review the 15 patterns and their recognition signals
- [ ] Practice explaining your thought process OUT LOUD
- [ ] Have a few template phrases ready
- [ ] Review complexity analysis
- [ ] Practice on a shared document/whiteboard (not your IDE!)
- [ ] Prepare questions to ask the interviewer
- [ ] Get good sleep the night before
- [ ] Have water nearby

---

*Good luck! You've got this! 🚀*

*Return to [Main Guide →](./README.md)*
