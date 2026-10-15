---
priority: P1
---
# LLD Cheatsheet

This page summarizes the LLD subject for revision before an interview. Each section links to the page that explains it. Do not learn the subject from this page: it has no reasons and no examples.

## The Procedure

Source: [How to Approach an LLD Interview](how-to-approach-lld.md).

1. **Clarify.** Ask about the format (how much code), the priority flows, the scale, the actors, and the rules that the statement does not give. Write the answers down.
2. **List the use cases.** One line per action that an actor takes.
3. **Find the entities.** Nouns become candidate classes. Classify each one: entity, value object, enum, or attribute ([domain modeling](fundamentals/05-domain-modeling.md)).
4. **Define the relationships.** Association, aggregation, composition, inheritance, realization, dependency ([UML](fundamentals/04-uml-class-and-sequence-diagrams.md)).
5. **Draw the class diagram.** Show the important classes, their key fields and methods, and the relationships.
6. **Define the interfaces.** Write the method signatures of the main services.
7. **Choose the patterns.** Use a pattern only where a requirement names a variation (see the trigger table below).
8. **Write the core code.** The main flow first, then the edge cases.
9. **Handle concurrency.** Name what can race, and the fix ([concurrency](fundamentals/09-concurrency-in-lld.md)).
10. **Discuss extensions.** Show which classes change for each new requirement.

## SOLID

Source: [SOLID](fundamentals/02-solid.md).

| Principle | Rule | Sign of a violation |
|---|---|---|
| Single Responsibility | A class has one reason to change: one actor asks for its changes | One class changes for unrelated requests (pricing and printing) |
| Open-Closed | Add behavior with new code, not by editing existing code | A `switch` on a type that grows with each new case |
| Liskov Substitution | A subtype works everywhere its base type works | A subclass throws on an inherited method, or checks `instanceof` |
| Interface Segregation | Clients do not depend on methods they do not use | Implementations with empty methods or `UnsupportedOperationException` |
| Dependency Inversion | High-level code and low-level code both depend on abstractions | A service creates its own repository with `new` |

Dependency inversion is a design rule. Dependency injection is one technique to apply it (pass the dependency in, usually through the constructor).

## Design Patterns: Intent and Trigger

Sources: [creational](fundamentals/06-creational-patterns.md), [structural](fundamentals/07-structural-patterns.md), [behavioral](fundamentals/08-behavioral-patterns.md).

### Creational

| Pattern | Intent | Use when the requirement says |
|---|---|---|
| Singleton | One instance of a class, with a global access point | "Exactly one" of a resource. Prefer one instance created at the composition root and injected |
| Factory Method | A subclass decides which class to create | A framework step must create an object whose class varies by subclass |
| Abstract Factory | Create families of related objects without naming their classes | Several products must match (one theme, one database vendor) |
| Builder | Build a complex object step by step, and validate it at the end | Many optional parameters (four or more), or an object that must be valid when built |
| Prototype | Create a new object by copying an existing one | Objects are costly to build and differ little. In Java, prefer a copy constructor |
| Object Pool | Reuse a limited set of costly objects | Connections or buffers that are costly to create. Never pool virtual threads |

### Structural

| Pattern | Intent | Use when the requirement says |
|---|---|---|
| Adapter | Convert one interface to the interface that the client expects | "Integrate this existing library or provider" |
| Bridge | Separate an abstraction from its implementation, so both vary on their own | Two dimensions of variation (notification kind and channel) |
| Composite | Treat one object and a group of objects through one interface | A tree: files and folders, bundles of products |
| Decorator | Add behavior to an object at run time with the same interface | Optional features that combine (logging, retry, encryption) |
| Facade | One simple interface over a subsystem | A use case that needs many calls to several classes |
| Flyweight | Share the common state of many small objects | Very many similar objects, with a measured memory problem |
| Proxy | Control access to an object with the same interface | Lazy loading, access control, caching, a remote call |

The four wrapper patterns differ by what they change: the interface (Adapter), the behavior (Decorator), the number of classes the client must know (Facade), or the access (Proxy).

### Behavioral

| Pattern | Intent | Use when the requirement says |
|---|---|---|
| Chain of Responsibility | Pass a request along a chain until a handler deals with it | Approval levels, validation steps, cash dispensing by note value |
| Command | Turn a request into an object | Undo and redo, queued or logged operations |
| Iterator | Traverse a collection without exposing its structure | A custom collection that clients must loop over |
| Mediator | One object coordinates how a group of objects interact | Many objects that would otherwise reference each other (chat room, form fields) |
| Memento | Capture and restore an object's state without exposing it | Snapshots, undo by state |
| Observer | Notify dependents when an object changes | "When X happens, notify Y and Z" |
| State | An object changes its behavior when its state changes | Several operations whose behavior depends on the current state (vending machine, ATM) |
| Strategy | Swap one algorithm for another behind one interface | "Support several ways to do X" (pricing, matching, allocation) |
| Template Method | A base class fixes the steps, and subclasses fill in some steps | The same workflow with different details per type |
| Visitor | Add operations to a fixed set of types without changing them | Many operations over a stable type hierarchy. In Java 21, consider a sealed interface and a pattern `switch` |
| Interpreter | Represent a simple grammar as classes | Rule or filter expressions |
| Null Object (not GoF) | An object that does nothing, in place of `null` | An optional collaborator (no-op logger, no discount) |

Simpler forms are often enough: an enum with a transition table instead of State, a `java.util.function` interface or a lambda instead of a Strategy class, a `Consumer` callback instead of an Observer interface.

### Patterns Often Confused

| Pair | Deciding question |
|---|---|
| Strategy vs State | Does the client choose the behavior (Strategy), or does the object change it by itself as its state changes (State)? |
| Strategy vs Template Method | Is the variation an object passed in (Strategy), or a subclass that overrides steps (Template Method)? |
| Decorator vs Proxy | Does the wrapper add a feature that the caller chooses (Decorator), or control access to the object (Proxy)? |
| Adapter vs Facade | Does the wrapper convert one interface (Adapter), or simplify several (Facade)? |
| Observer vs Mediator | Do the objects only receive events (Observer), or do they also talk to each other through the coordinator (Mediator)? |
| Bridge vs Strategy | Does the abstraction side also have its own subclasses (Bridge)? |
| Factory Method vs Abstract Factory | Does one method create one product (Factory Method), or does one object create a family of products (Abstract Factory)? |

## Concurrency Choices

Source: [Concurrency in LLD](fundamentals/09-concurrency-in-lld.md).

| Situation | First choice | Why |
|---|---|---|
| One shared counter | `AtomicLong` / `AtomicInteger` | Lock-free increment |
| Check-then-act on one map key | `ConcurrentHashMap.compute` or `putIfAbsent` | Atomic per key |
| Invariant across several fields of one object | `synchronized` on that object | One lock protects the whole invariant |
| Need `tryLock`, a timeout, or fairness | `ReentrantLock` | Features that `synchronized` does not have |
| Two objects must change together (transfer) | Lock both in a fixed global order (by id) | Prevents deadlock |
| Many readers, few writers | `ReentrantReadWriteLock` or an immutable snapshot | Readers do not block each other |
| Producer and consumer threads | `BlockingQueue` (bounded) | Back-pressure and hand-off in one class |
| Rare conflicts, long edits | Optimistic locking with a version number | No lock held while the user works |
| Seat or slot hold | Hold with an expiry time, then confirm | A slow user does not block the resource |

Rules that interviewers check:

- Say what races before you add a lock.
- Keep slow calls (payment, network, listeners) outside the lock.
- Never call unknown code (a listener, a strategy) while you hold a lock, unless you state the risk.
- Make objects immutable where you can. An immutable object needs no lock.
- A concurrency demo prints only results that do not depend on the thread schedule.

## Architecture in the Code

Source: [Architectural Patterns](fundamentals/10-architectural-patterns.md).

- Layers: controller or CLI → service → repository → model. Each layer calls only the layer below.
- The service owns the repository interface. The in-memory implementation is a detail.
- Use constructor injection. Wire the objects in one place (the composition root, often `main`).
- Keep business rules in the domain objects, not in the controller or the repository.

## Red Flags in an LLD Round

- Code before requirements.
- A God class that does everything (`ParkingLotManager` with 30 methods).
- Inheritance where composition fits (`Penguin extends Bird` with a `fly` that throws).
- Patterns with no requirement behind them.
- `double` for money. Use `long` minor units or `BigDecimal`.
- No answer to "what if two users do this at the same time?"
- A design that cannot take the follow-up requirement without a rewrite.

## Case-Study Index by Main Pattern

| Main pattern | Case studies |
|---|---|
| State | [Vending machine](case-studies/07-vending-machine/README.md), [ATM](case-studies/12-atm/README.md), [elevator](case-studies/04-elevator-system/README.md) |
| Strategy | [Parking lot](case-studies/01-parking-lot/README.md), [rate limiter](case-studies/03-rate-limiter/README.md), [Splitwise](case-studies/06-splitwise/README.md), [ride sharing](case-studies/17-ride-sharing/README.md), [notification service](case-studies/24-notification-service/README.md) |
| Composite | [In-memory file system](case-studies/14-in-memory-file-system/README.md) |
| Chain of Responsibility | [Logging framework](case-studies/08-logging-framework/README.md), [ATM](case-studies/12-atm/README.md) |
| Observer | [Parking lot](case-studies/01-parking-lot/README.md), [food delivery](case-studies/21-food-delivery/README.md), [pub-sub](case-studies/15-pub-sub-message-queue/README.md) |
| Concurrency first | [Movie ticket booking](case-studies/05-movie-ticket-booking/README.md), [hotel booking](case-studies/20-hotel-booking/README.md), [online auction](case-studies/22-online-auction/README.md), [digital wallet](case-studies/23-digital-wallet/README.md), [task scheduler](case-studies/16-task-scheduler/README.md) |
| Data structure first | [LRU and LFU cache](case-studies/02-lru-and-lfu-cache/README.md), [key-value store](case-studies/19-key-value-store-with-ttl/README.md), [tic-tac-toe](case-studies/09-tic-tac-toe/README.md) |
