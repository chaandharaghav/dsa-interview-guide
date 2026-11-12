---
priority: P1
---
# Mastery Tracker

Reading a page makes the topic familiar. It does not make you able to use the topic in an interview. This page defines six levels between "I have read this" and "I can do this in an interview", gives a test for each level, and gives the routine that moves a topic up the levels. Record your level for each topic in [My Progress](my-progress.md). The [roadmap](roadmap.md) uses these levels as its stage exits.

## The Six Levels

A level counts only if you pass its test with the page closed. If you are between two levels, record the lower one.

| Level | Name | The test | What fails the test |
|---|---|---|---|
| L1 | Read | You have read the page once | Nothing. L1 gives no evidence of skill |
| L2 | Understand | With the page closed, you trace the technique by hand on a new small input and get the right result | You need the page to continue the trace |
| L3 | Explain | You explain aloud, in under 3 minutes, why the brute force is too slow, how the technique removes the waste, and why the result is correct. You answer "when does it not apply?" with a counterexample | You recite the steps but cannot give the reason for one of them |
| L4 | Solve a familiar problem | You write a correct Java solution to a problem from the page's ladder, from an empty file, without hints, and you state its complexity | You look at a template, or the code fails a case that the page names |
| L5 | Solve a new problem | You get a problem without a pattern label. You choose the technique, give the deciding property, and solve the problem without hints | You needed the label, a hint, or the solution |
| L6 | Solve under interview conditions | You do L5 in the time limit (25 minutes for a Medium problem, 40 minutes for a Hard problem), speaking aloud, with tests and complexity, on the first attempt | The time ran out, the code had a defect that you did not find, or you were silent |

For the subjects without code (HLD, and the theory pages of Java, Databases, Operating Systems, and Networking), use these tests in place of L4 to L6:

| Level | The test for a theory or design topic |
|---|---|
| L4 | You answer the page's `??? question` blocks aloud before you open them, and your answers contain the main points of at least 80% of the model answers |
| L5 | You answer a question that the page does not list: a follow-up, a "what if" change, or a question from the subject's question bank that you have not seen |
| L6 | You use the topic inside a full design or a mock round under time, and you defend the trade-off when someone challenges it |

Two rules keep the levels honest:

1. **A level expires.** If the review date in [My Progress](my-progress.md) passes and you fail the level's test, lower the level.
2. **Recognition is not recall.** "I would have thought of that" after you read a solution is L1 for that problem. Only what you produce counts.

## The Study Loop for One Topic

Use these steps for each topic page. The steps for a DSA lesson page are in the left column. The right column gives the step for a theory page.

| Step | DSA lesson page | Theory page | Level you reach |
|---|---|---|---|
| 1. Attempt first | Read "Motivation" up to the problem statement. Close the page. Try the problem for 10 minutes and write what you tried | Read "Why It Matters". Write what you already know about the topic in 5 lines | — |
| 2. Read to derive | Read "Derivation". At each question in the text, stop and answer before you read on | Read "Core Concepts" and "How It Works". Draw each diagram yourself | L1 |
| 3. Trace | Trace the algorithm by hand on an input that the page does not use. Then run the Java program and compare | Explain the mechanism on paper for one concrete case | L2 |
| 4. Explain | Explain the technique aloud, to a person or to a recorder, without the page | The same | L3 |
| 5. Write | Write the Java code from an empty file. Compare with the page only after your code runs | Answer the "Interview Angle" questions aloud, then open the model answers | L4 for the first problem |
| 6. Practice | Do the practice ladder. Give each problem 15 minutes before you open "Hint 1", and 10 more minutes before each later hint. Record the hints that you used | Answer the follow-up questions | L4 |
| 7. Separate | Do "Tell Them Apart". Then solve one problem of the pattern from a [mixed set](#mixed-sets) | Read "Commonly Confused With" and state each difference from memory | L5 starts here |
| 8. Record | Tick the "Mastery Checklist" items that you can do. Write your level and the first review date in [My Progress](my-progress.md) | The same | — |

Step 1 matters even when you fail. An attempt shows you which part of the problem is hard, so the derivation answers a question that you already have.

When you open a hint, you have not failed the problem. You have found the exact step that you could not make. Write that step in the [weak-area log](#the-weak-area-log), and solve the problem again without hints at the next review.

## The Revision Schedule

Recommendation: review a topic 1 day, 3 days, 7 days, 21 days, and 60 days after you first reach L4. These intervals are a starting point, not a measured optimum. Shorten the next interval when a review fails. The research in [Further Reading](#further-reading) supports the two principles behind the schedule: reviews spread over time give better long-term retention than the same reviews in one block, and a review in which you produce the answer from memory gives better retention than a review in which you read the page again.

| Review | Days after L4 | What to do (10 to 30 minutes) | If you fail |
|---|---|---|---|
| R1 | 1 | With the page closed, write the invariant and the code of the main template. Then check | Repeat steps 3 to 5 of the study loop. Next review in 1 day |
| R2 | 3 | Solve one ladder problem that you solved with hints, now without hints | Next review in 1 day |
| R3 | 7 | Solve one ladder problem that you have not tried. Do the "Interview Follow-ups" aloud | Next review in 3 days |
| R4 | 21 | Solve one problem of this pattern from a mixed set, under time | Lower the level to L4. Next review in 7 days |
| R5 | 60 | Read the "Quick Reference Card". Solve one Hard problem of the pattern under time | Lower the level by one. Next review in 21 days |

A review tests recall. Do not start a review with the page open. Reading the page first turns the review into a second reading, and it tells you nothing about what you can produce.

Rules for the daily plan:

1. A review that is due comes before new material.
2. Limit reviews to one third of the study time of a day. If the reviews need more, stop adding new topics until the queue is short again.
3. In stage 5 of the [roadmap](roadmap.md), mixed sets replace single-topic reviews. A mixed set reviews several patterns in one session and also trains the choice between them.

## The Problem Log

Keep one line for each problem that you attempt, in a file or a sheet of your choice. The log gives the facts for your level in [My Progress](my-progress.md).

| Field | What to write |
|---|---|
| Date | The date of the attempt |
| Problem | The number and the title |
| Pattern predicted | Your prediction before you solved it, and the deciding question that chose it |
| Pattern actual | The technique of the accepted solution |
| Result | Solved alone, solved with hint 1, 2, or 3, or read the solution |
| Time | Minutes to a correct solution |
| Failure class | One class from the weak-area log, if the result is not "solved alone in time" |
| Retry on | The date of the next attempt, if the result is not "solved alone" |

## The Weak-Area Log

A failed problem is useful only if you know which skill failed. Give each failure one class. After 15 to 20 entries, count the classes. The class with the highest count is what you repair next.

| Class | The sign | The repair |
|---|---|---|
| Recognition | You chose the wrong technique, or no technique | [How to Choose a Pattern](dsa/how-to-choose-a-pattern.md): find the step or the deciding question that gives the right choice. Then the "Recognition Guide" and "Tell Them Apart" parts of both patterns. Do 10 classify-only drills |
| Approach | You knew the technique but could not apply it to this problem (the state, the invariant, or the feasibility check was missing) | The "Derivation" and "Worked Examples" sections. Rebuild the technique from the brute force on this problem. Write the invariant in one sentence |
| Implementation | The idea was right and the code was wrong or slow to write | Write the main template again from an empty file on 3 different days. Read "Common Mistakes". Read the [Java Toolkit](dsa/java-toolkit.md) section for the collection that you used |
| Edge case | The code failed on an empty input, one element, duplicates, an overflow, or a bound | Before you run code, write 4 test inputs: empty, one element, the smallest case with the special property, and a large value. Trace one of them by hand |
| Complexity | You gave a wrong bound, or the solution was too slow for the constraints | [Complexity Analysis](dsa/foundations/01-complexity-analysis.md). State the time budget from the constraints before you choose an approach |
| Communication | You were silent, or the listener could not follow the plan | [Interview Tips](dsa/interview-tips.md). Record yourself on one problem each week and listen to it |
| Time | The solution was correct and late | Find the phase that took the time: understanding, choice, code, or debugging. Train that phase with timed drills |

The misrecognition log in [How to Choose a Pattern, Part 4](dsa/how-to-choose-a-pattern.md#part-4-how-to-train-recognition) is the "Recognition" class of this log. Keep one log, not two.

## Mixed Sets

A mixed set is a list of problems from several patterns, without the pattern labels. It tests the choice of the technique, which a single-pattern ladder cannot test.

The KB does not have a page of ready mixed sets yet. Until it has one, build each set with this rule, so that you do not know the pattern of a problem in advance:

1. Take the patterns that the set must cover (for a milestone, the "Covers" column below).
2. From the "More problems" tables at the end of each pattern page's "Practice Problems" section, collect the problems that you have not attempted.
3. Ask another person to choose the problems, or choose them with a random number, and copy only the links into a list. Do not copy the pattern name or the "What it adds" text.
4. Wait at least one day before you take the set.

Two more sources of practice without labels exist now: the [recognition drills](dsa/how-to-choose-a-pattern.md#part-5-recognition-drills) (classify only) and the sessions on the [mock interviews](dsa/mock-interviews.md) page.

## Milestone Assessments

A milestone is a fixed test at a stage exit of the [roadmap](roadmap.md). It uses a [mixed set](#mixed-sets) that covers the patterns of the stage. Do not look at the problems of a set before you take it.

Conditions for each milestone:

- One sitting, with a timer. No page, no notes, no hints, no search.
- Write the code in an editor without autocomplete of solutions. Run it only after you have traced one input by hand.
- Speak aloud, or write your reasoning, before the code.
- After the time limit, stop. Then score each problem with the table below.

| Milestone | Taken after | Covers | Size and time | Pass |
|---|---|---|---|---|
| M1 | Stage 1 | Patterns 01 to 08 | 4 problems, 100 minutes | 3 of 4 solved, and the technique named with the right deciding property for all 4 |
| M2 | Stage 2 | Patterns 01 to 13, 16, 17, 20 | 4 problems, 110 minutes | 3 of 4 solved, technique right for all 4 |
| M3 | Stage 3 | Patterns 01 to 20 | 4 problems, 120 minutes | 3 of 4 solved, with one of them at Hard level or from pattern 15 |
| M4 | Stage 5 | All P1 patterns, in interview form | 3 sessions on 3 days, each with 2 problems in 45 minutes, spoken | In at least 2 sessions: both problems solved, and a rubric score of 3 or more in each row of the [mock rubric](dsa/mock-interviews.md#self-assessment-rubric) |

Score for one problem:

| Score | Meaning |
|---|---|
| Solved | Correct code for the stated constraints, right complexity stated, finished inside the limit |
| Partly solved | The right technique and a plan, with code that is incomplete or has a defect |
| Not solved | A wrong technique, or no working plan |

If you fail a milestone, do not take the same set again in the same week: you now know its problems. Classify each failure in the weak-area log, repair the two weakest patterns, and then take a new set that you build with the same rule.

For LLD and HLD, the milestone is the mock sets of the [LLD practice page](lld/practice-and-mock-interviews.md) and the [HLD practice page](hld/practice-and-mock-interviews.md), scored with the rubric on each page.

## What Mastery Does Not Mean

- A page with every box ticked shows what you could do on the day that you ticked the box. The review dates show what you can do now.
- L6 on every pattern does not guarantee an offer. An interview also tests clarity, speed of recovery after a mistake, and topics outside this KB.
- The number of problems solved is not a level. Twenty problems solved alone in the mixed sets are stronger evidence than two hundred problems solved after reading the approach.

## Further Reading

- Roediger, H. L., and Karpicke, J. D. (2006). "Test-Enhanced Learning: Taking Memory Tests Improves Long-Term Retention." *Psychological Science*, 17(3), 249–255. The study behind "recall first, then check".
- Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., and Rohrer, D. (2006). "Distributed Practice in Verbal Recall Tasks: A Review and Quantitative Synthesis." *Psychological Bulletin*, 132(3), 354–380. The review behind reviews that are spread over time.
- Rohrer, D., and Taylor, K. (2007). "The Shuffling of Mathematics Problems Improves Learning." *Instructional Science*, 35(6), 481–498. The study behind mixed practice: practice sets that mix problem types gave better later test results than sets grouped by type.
