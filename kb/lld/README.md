# Low-Level Design

Low-level design (LLD) is the design of the classes, interfaces, and interactions inside one program. Product companies test it in two kinds of round:

- **LLD interview** (45 to 60 minutes): you clarify requirements, draw a class diagram, explain the patterns, and write the core code.
- **Machine-coding round** (90 to 120 minutes): you write a complete, working program that the evaluator runs and reads.

Each case study in this subject has a runnable `Main.java`. Run it with `java Main.java` (Java 21) in the case-study folder.

## How to Study This Subject

1. Read [How to Approach an LLD Interview](how-to-approach-lld.md). It is the procedure that every case study follows.
2. Read the fundamentals in order. Pages 01, 02, 05, 06, 07, 08, and 09 are the minimum before the first case study.
3. Do the P1 case studies. For each one: read the problem statement only, design it yourself for 45 minutes, and then compare your design with the page.
4. Read [The Machine-Coding Round](machine-coding-round.md) before a machine-coding round, and do one case study as a timed 90-minute build.
5. Use the [cheatsheet](cheatsheet.md) on the day before an interview, and the [practice page](practice-and-mock-interviews.md) for timed mock rounds.

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.

## Procedure and Reference

| Page | Priority | Content |
|---|---|---|
| [How to Approach an LLD Interview](how-to-approach-lld.md) | P1 | The step-by-step procedure, the time budget, and what interviewers score |
| [The Machine-Coding Round](machine-coding-round.md) | P1 | The 90-minute format, the package layout, tests, and the reasons candidates fail |
| [Cheatsheet](cheatsheet.md) | P1 | SOLID, every pattern with its trigger, the procedure, and the concurrency choices on one page |
| [Practice and Mock Interviews](practice-and-mock-interviews.md) | P1 | Extra problems, timed mock sets, and a self-evaluation rubric |

## Fundamentals

| Page | Priority | Content |
|---|---|---|
| [01 · OOP](fundamentals/01-oop.md) | P1 | Encapsulation, abstraction, inheritance, polymorphism, interfaces and abstract classes, composition |
| [02 · SOLID](fundamentals/02-solid.md) | P1 | The five principles, each with a violation and a fix |
| [03 · Design Principles](fundamentals/03-design-principles.md) | P2 | DRY, KISS, YAGNI, Law of Demeter, cohesion and coupling, immutability |
| [04 · UML Diagrams](fundamentals/04-uml-class-and-sequence-diagrams.md) | P1 | Class, sequence, and state diagrams in Mermaid |
| [05 · Domain Modeling](fundamentals/05-domain-modeling.md) | P1 | From a problem statement to entities, value objects, aggregates, and state machines |
| [06 · Creational Patterns](fundamentals/06-creational-patterns.md) | P1 | Singleton, Factory Method, Abstract Factory, Builder, Prototype, Object Pool |
| [07 · Structural Patterns](fundamentals/07-structural-patterns.md) | P1 | Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy |
| [08 · Behavioral Patterns](fundamentals/08-behavioral-patterns.md) | P1 | The eleven GoF behavioral patterns and Null Object |
| [09 · Concurrency in LLD](fundamentals/09-concurrency-in-lld.md) | P1 | Races in LLD problems, lock choices, lock ordering, optimistic locking |
| [10 · Architectural Patterns](fundamentals/10-architectural-patterns.md) | P1 | Layered, MVC, hexagonal, clean architecture, repository, dependency injection |
| [11 · Anti-Patterns](fundamentals/11-anti-patterns.md) | P2 | God object, anemic model, and other design smells |

## Case Studies

| Case study | Priority | Main design points |
|---|---|---|
| [01 · Parking Lot](case-studies/01-parking-lot/README.md) | P1 | Spot allocation Strategy, lock-free spot claim, payment on exit |
| [02 · LRU and LFU Cache](case-studies/02-lru-and-lfu-cache/README.md) | P1 | O(1) LRU and LFU, eviction policy Strategy, thread-safe and TTL decorators |
| [03 · Rate Limiter](case-studies/03-rate-limiter/README.md) | P1 | Four algorithms behind one interface, per-client state, injected time source |
| [04 · Elevator System](case-studies/04-elevator-system/README.md) | P1 | State pattern, dispatch Strategy, LOOK scheduling |
| [05 · Movie Ticket Booking](case-studies/05-movie-ticket-booking/README.md) | P1 | Seat holds with expiry, one lock per show, payment outside the lock |
| [06 · Splitwise](case-studies/06-splitwise/README.md) | P1 | Split Strategy, pairwise ledger, debt simplification |
| [07 · Vending Machine](case-studies/07-vending-machine/README.md) | P1 | State pattern, change-making search |
| [08 · Logging Framework](case-studies/08-logging-framework/README.md) | P1 | Handler chain, appender Strategy, async Decorator |
| [09 · Tic-Tac-Toe](case-studies/09-tic-tac-toe/README.md) | P1 | O(1) win check, player Strategy, undo |
| [10 · Snake and Ladder](case-studies/10-snake-and-ladder/README.md) | P2 | Board validation, dice Strategy, turn order |
| [11 · Chess](case-studies/11-chess/README.md) | P2 | Move-rule Strategy, check detection, special moves |
| [12 · ATM](case-studies/12-atm/README.md) | P2 | State pattern, note-dispensing chain, bank port |
| [13 · Library Management](case-studies/13-library-management/README.md) | P2 | Books and copies, loans, reservations, fines |
| [14 · In-Memory File System](case-studies/14-in-memory-file-system/README.md) | P2 | Composite pattern, path resolution, read-write lock |
| [15 · Pub-Sub Message Queue](case-studies/15-pub-sub-message-queue/README.md) | P1 | Partitions, consumer groups, offsets, retry and dead letters |
| [16 · Task Scheduler](case-studies/16-task-scheduler/README.md) | P1 | Delayed and recurring tasks, priority queue, worker pool |
| [17 · Ride Sharing](case-studies/17-ride-sharing/README.md) | P2 | Matching Strategy, trip states, fare Decorator |
| [18 · Shopping Cart and Orders](case-studies/18-shopping-cart-and-orders/README.md) | P2 | Inventory reservation, discounts, idempotent checkout |
| [19 · Key-Value Store with TTL](case-studies/19-key-value-store-with-ttl/README.md) | P2 | Expiry, nested transactions, snapshots |
| [20 · Hotel Booking](case-studies/20-hotel-booking/README.md) | P2 | Date-range availability, per-room locks, cancellation policy |
| [21 · Food Delivery](case-studies/21-food-delivery/README.md) | P2 | Order lifecycle, partner assignment, status Observer |
| [22 · Online Auction](case-studies/22-online-auction/README.md) | P2 | Concurrent bids, auction states, automatic close |
| [23 · Digital Wallet](case-studies/23-digital-wallet/README.md) | P2 | Double-entry ledger, lock ordering, idempotency keys |
| [24 · Notification Service](case-studies/24-notification-service/README.md) | P1 | Channel Strategy, templates, retries, per-user rate limits |
