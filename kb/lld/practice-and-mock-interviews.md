---
priority: P1
---
# LLD Practice and Mock Interviews

This page gives you problems to design without a solution, sets of timed mock rounds, and a rubric to score your own design. Do the 24 case studies first. Then use this page to practice on problems that you have not seen.

## How to Practice

1. Set a timer for 45 minutes (an LLD interview) or 90 minutes (a machine-coding round).
2. Read only the problem statement. Do not look at notes.
3. Follow the [procedure](how-to-approach-lld.md) aloud, as if an interviewer listens. Write the clarifying questions and your assumed answers first.
4. Draw the class diagram on paper or in a Mermaid block. Then write the core code in one Java file.
5. When the timer ends, stop. Score your work with the rubric at the end of this page.
6. Compare your design with the closest case study (named in the table below), and write down one thing to do differently next time.

Recommendation: do one practice problem every two days. Do a full mock set once a week in the last month before interviews.

## Practice Problems

Each problem names the case studies that use similar ideas. Read them only after your own attempt.

| # | Problem | What the interviewer wants to see | Related case studies |
|---|---|---|---|
| 1 | **Car rental system.** Customers reserve cars of a category for a date range at a branch, pick up, return, and pay late fees | Date-range availability, reservation states, pricing Strategy | [Hotel booking](case-studies/20-hotel-booking/README.md), [library](case-studies/13-library-management/README.md) |
| 2 | **Meeting room scheduler.** Book rooms for time slots, check conflicts, find a free room of a given size, cancel | Interval overlap check, per-room locking | [Hotel booking](case-studies/20-hotel-booking/README.md) |
| 3 | **Coffee machine.** Recipes, ingredient stock, low-stock alerts, several outlets at the same time | Recipes as data, inventory lock, Observer for alerts | The worked example in [How to Approach an LLD Interview](how-to-approach-lld.md) |
| 4 | **Cab invoice generator.** Fares per ride type, invoices per user, monthly summaries | Strategy for fares, money as integer cents | [Ride sharing](case-studies/17-ride-sharing/README.md) |
| 5 | **In-memory search engine.** Index documents, search by words with AND and OR, rank by term frequency | Inverted index, query parsing, Interpreter for queries | [Library](case-studies/13-library-management/README.md) |
| 6 | **Social feed (classes only).** Users, posts, follows, a home feed in time order, likes | Feed assembly, pagination, Observer or pull model | [Pub-sub](case-studies/15-pub-sub-message-queue/README.md) |
| 7 | **Version control system.** Commits, branches, checkout, a simple merge, history | Immutable snapshots, Command, Memento | [Key-value store](case-studies/19-key-value-store-with-ttl/README.md), [file system](case-studies/14-in-memory-file-system/README.md) |
| 8 | **Spreadsheet.** Cells with values and formulas, recalculation when a cell changes, cycle detection | Dependency graph, Observer, Interpreter for formulas | [Task scheduler](case-studies/16-task-scheduler/README.md) |
| 9 | **Customer support ticket system.** Tickets, priorities, assignment to agents, escalation after a time limit | State, assignment Strategy, scheduled escalation | [Task scheduler](case-studies/16-task-scheduler/README.md), [food delivery](case-studies/21-food-delivery/README.md) |
| 10 | **Traffic signal controller.** Intersections, phases, timers, an emergency override | State pattern, timed transitions with an injected clock | [Elevator](case-studies/04-elevator-system/README.md) |
| 11 | **Question and answer site.** Questions, answers, votes, reputation, tags, search | Entities and aggregates, vote concurrency | [Library](case-studies/13-library-management/README.md) |
| 12 | **Music player.** Library, playlists, queue, shuffle, repeat modes | Iterator, Strategy for play order, Command | [Snake and ladder](case-studies/10-snake-and-ladder/README.md) |
| 13 | **Calendar.** Events, recurring events, invitations, time zones, conflict detection | Recurrence rules, `java.time`, interval overlap | [Task scheduler](case-studies/16-task-scheduler/README.md) |
| 14 | **Payroll system.** Employees with salary types, deductions, monthly payslips | Strategy for pay rules, money as `BigDecimal` | [Splitwise](case-studies/06-splitwise/README.md) |
| 15 | **URL shortener (classes only).** Create short codes, redirect, expiry, per-user limits | Code generation, collision handling, TTL | [Key-value store](case-studies/19-key-value-store-with-ttl/README.md), [rate limiter](case-studies/03-rate-limiter/README.md) |
| 16 | **Cricket scoreboard.** Balls, overs, wickets, batting order, extras, a live score view | Event model, Observer, derived state | [Tic-tac-toe](case-studies/09-tic-tac-toe/README.md) |
| 17 | **Warehouse management.** Bins, stock moves, picking lists, reservations for orders | Inventory reservation, concurrency | [Shopping cart](case-studies/18-shopping-cart-and-orders/README.md) |
| 18 | **Bowling game scorer.** Frames, strikes, spares, the tenth frame | Rules modeling, test cases for edge frames | [Tic-tac-toe](case-studies/09-tic-tac-toe/README.md) |
| 19 | **Professional network connections.** Users, connection requests, degrees of connection, suggestions | Graph model, BFS for degree, request states | [DSA graph traversal](../dsa/11-graph-traversal/README.md) |
| 20 | **Stock brokerage (single process).** Orders, an order book, matching of buy and sell orders, trades | Order book with priority queues, price-time priority, concurrency | [Online auction](case-studies/22-online-auction/README.md) |

## Mock Interview Sets

Each set is one practice session. Do the problems in the set on different days if the session is too long.

| Set | Round 1 (45 minutes) | Round 2 (45 minutes) | Focus |
|---|---|---|---|
| A | Parking lot (case study 01) | Meeting room scheduler (problem 2) | Strategy and interval overlap |
| B | LRU cache (case study 02) | In-memory search engine (problem 5) | Data structures inside a design |
| C | Vending machine (case study 07) | Traffic signal controller (problem 10) | State pattern |
| D | Movie ticket booking (case study 05) | Car rental system (problem 1) | Concurrency and holds |
| E | Logging framework (case study 08) | Spreadsheet (problem 8) | Chain of Responsibility, Observer, Interpreter |
| F | Machine-coding round, 90 minutes: Splitwise (case study 06) | — | Working code, package layout, tests |
| G | Machine-coding round, 90 minutes: Customer support ticket system (problem 9) | — | An unseen problem under time pressure |

For sets A to E, the first round is a case study that you have read. Rebuild it from memory in 45 minutes. The second round is a problem that you have not seen.

## Self-Evaluation Rubric

Score each line from 0 to 2. A total of 16 or more out of 20 is a strong round.

| Area | 0 | 1 | 2 |
|---|---|---|---|
| Requirements | Started to design at once | Asked some questions | Asked about format, priority flows, scale, and edge rules, and wrote the answers |
| Entities | Missing core entities | Core entities, some wrong responsibilities | Each class has one clear responsibility |
| Relationships | No diagram | Diagram with errors | Correct diagram with multiplicity where it matters |
| Patterns | None, or forced | Correct but unexplained | Each pattern tied to a named requirement, with the rejected alternative |
| Interfaces | Concrete classes everywhere | Some interfaces | Interfaces at the variation points, constructor injection |
| Code | Does not compile or run | Runs, with gaps | Core flow runs, clean names, no God class |
| Edge cases | Not handled | Some handled | Invalid input and failure paths handled |
| Concurrency | Not mentioned | Mentioned, no fix | Named the race and gave a correct fix |
| Extensions | Needed a rewrite | Partly handled | Each follow-up changes few classes |
| Communication | Silent | Explained after coding | Explained each decision while working |

## After Each Session

Write three lines in a practice log:

1. The score from the rubric.
2. The area with the lowest score.
3. The page in this subject that covers that area. Read it before the next session.
