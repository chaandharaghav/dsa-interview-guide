# Low-Level Design

Low-level design (LLD) is the design of classes, interfaces, and their interactions for one program. LLD interviews and machine-coding rounds test it.

!!! info "Status"
    Planned for phase 1. The pages below do not exist yet. Phase 1 writes them, and then this page links to each one.

## Planned Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

### Root files

| File | Content | Priority |
|---|---|---|
| `how-to-approach-lld.md` | The LLD interview procedure: requirements, entities, relationships, class diagram, APIs, code, extensions. A time budget for a 45-minute round | P1 |
| `machine-coding-round.md` | The 90-minute machine-coding round: package layout, in-memory storage, a driver class, tests, what evaluators score | P1 |
| `cheatsheet.md` | One page: SOLID, each design pattern with its intent and its trigger, the procedure | P1 |
| `practice-and-mock-interviews.md` | Extra LLD problems without solutions, timed mock sets, self-evaluation rubric | P1 |

### `fundamentals/`

| File | Content | Priority |
|---|---|---|
| `01-oop.md` | Encapsulation, abstraction, inheritance, polymorphism. Interfaces and abstract classes. Composition and inheritance | P1 |
| `02-solid.md` | The five SOLID principles, each with a violation and a fix in Java | P1 |
| `03-design-principles.md` | DRY, KISS, YAGNI, Law of Demeter, cohesion and coupling, tell-don't-ask, immutability | P2 |
| `04-uml-class-and-sequence-diagrams.md` | Association, aggregation, composition, dependency, realization. Sequence diagrams. All in Mermaid | P1 |
| `05-domain-modeling.md` | How to find entities, value objects, enums, and state machines in a problem statement | P1 |
| `06-creational-patterns.md` | Singleton (with thread-safe forms), Factory Method, Abstract Factory, Builder, Prototype | P1 |
| `07-structural-patterns.md` | Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy | P1 |
| `08-behavioral-patterns.md` | Chain of Responsibility, Command, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor. Interpreter in brief | P1 |
| `09-concurrency-in-lld.md` | Thread safety in a design, locks, immutable objects, producer-consumer, seat and slot locking | P1 |
| `10-architectural-patterns.md` | Layered, MVC, hexagonal and clean architecture, repository, DTO, dependency injection | P1 |
| `11-anti-patterns.md` | God object, anemic model, overuse of inheritance, overuse of Singleton, other design smells | P2 |

`case-studies/`: each case study is a folder with `README.md` and one runnable `Main.java`.

| Folder | Main design points | Priority |
|---|---|---|
| `01-parking-lot` | Entities, Strategy for spot allocation, concurrency | P1 |
| `02-lru-and-lfu-cache` | HashMap with a doubly linked list, O(1) LFU | P1 |
| `03-rate-limiter` | Token bucket, sliding window, Strategy | P1 |
| `04-elevator-system` | State pattern, scheduling algorithms | P1 |
| `05-movie-ticket-booking` | Seat locks, concurrency, payment timeout | P1 |
| `06-splitwise` | Expense split strategies, debt simplification | P1 |
| `07-vending-machine` | State pattern | P1 |
| `08-logging-framework` | Chain of Responsibility, Singleton, appenders | P1 |
| `09-tic-tac-toe` | Game loop, win check in O(1) | P1 |
| `10-snake-and-ladder` | Game modeling, dice Strategy | P2 |
| `11-chess` | Piece hierarchy, move validation | P2 |
| `12-atm` | State pattern, Chain of Responsibility for cash dispensing | P2 |
| `13-library-management` | Entities, search, fines | P2 |
| `14-in-memory-file-system` | Composite pattern | P2 |
| `15-pub-sub-message-queue` | Observer, consumer offsets, concurrency | P1 |
| `16-task-scheduler` | Delayed tasks, PriorityQueue, worker threads | P1 |
| `17-ride-sharing` | Matching Strategy, trip state | P2 |
| `18-shopping-cart-and-orders` | Inventory, order states, discount Strategy | P2 |
| `19-key-value-store-with-ttl` | Expiry, transactions, snapshots | P2 |
| `20-hotel-booking` | Room inventory, booking state, concurrency | P2 |
| `21-food-delivery` | Order lifecycle, partner assignment, Observer for status | P2 |
| `22-online-auction` | Bidding, concurrency, auction state | P2 |
| `23-digital-wallet` | Transfers, idempotency, ledger entries | P2 |
| `24-notification-service` | Channel Strategy, templates, retries | P1 |
