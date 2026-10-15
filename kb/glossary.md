# Glossary

This page gives one definition for each term that the KB uses. Every page uses the term that this page lists. A page does not use a different word for the same concept.

## Entry Format

Each entry has this form:

> **Term** (abbreviation): definition in one or two sentences. See [the page that explains it](README.md).

Each phase of the KB adds the terms that its pages introduce.

## Terms

### A–F

**Aggregate**: a group of objects that the code changes as one unit, with one entry object (the aggregate root) that protects the group's rules. See [Domain Modeling](lld/fundamentals/05-domain-modeling.md).

**Aggregation (UML)**: a whole-part association where the part can exist without the whole and can belong to more than one whole. UML gives it weak semantics, so a plain association is often the safer choice. See [UML Diagrams](lld/fundamentals/04-uml-class-and-sequence-diagrams.md).

**Amortized time complexity**: the average cost of one operation over a worst-case sequence of operations. Example: `ArrayList.add` is O(1) amortized, because the occasional resize cost spreads over the many adds that come before it. See [the Java toolkit](dsa/java-toolkit.md).

**Composition (UML)**: a whole-part association where a part belongs to at most one whole at a time, and deleting the whole deletes its parts. See [UML Diagrams](lld/fundamentals/04-uml-class-and-sequence-diagrams.md).

**Composition over inheritance**: the design rule that a class gets behavior by holding other objects, not by extending a parent class. See [OOP](lld/fundamentals/01-oop.md).

**Critical section**: a block of code that only one thread may run at a time, because it reads and writes shared state. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Dependency injection**: a technique in which an object receives the objects it depends on (usually through its constructor) instead of creating them. It is one way to apply dependency inversion. See [SOLID](lld/fundamentals/02-solid.md).

**Dependency inversion**: the design rule that high-level code and low-level code both depend on abstractions, not on each other's concrete classes. See [SOLID](lld/fundamentals/02-solid.md).

**Design pattern**: a named, reusable solution to a recurring design problem, with a defined intent and structure. The GoF book catalogs 23 of them. See [Creational Patterns](lld/fundamentals/06-creational-patterns.md).

**Double-entry ledger**: a record of money in which every transfer writes a debit entry and a credit entry of the same amount, so the sum of all entries is zero. See [Digital Wallet](lld/case-studies/23-digital-wallet/README.md).

**DSA (data structures and algorithms)**: the subject of how to store data and how to compute results from it within time and memory limits. See [DSA](dsa/README.md).

**Entity**: a domain object with an identity that stays the same while its attributes change (an order, a member). Compare value object. See [Domain Modeling](lld/fundamentals/05-domain-modeling.md).

### G–L

**GoF (Gang of Four)**: Gamma, Helm, Johnson, and Vlissides, the authors of *Design Patterns* (1994), the book that cataloged the 23 classic design patterns. See [Creational Patterns](lld/fundamentals/06-creational-patterns.md).

**Happens-before**: the Java Memory Model relation that guarantees one action's effects are visible to another action. Examples: an unlock happens-before a later lock of the same monitor. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**HLD (high-level design)**: the design of a distributed system: its services, data stores, and the communication between them. See [HLD](hld/README.md).

**Idempotency key**: a unique key that a client sends with a request, so that the server performs the request at most once and returns the stored result for a repeated request. See [Digital Wallet](lld/case-studies/23-digital-wallet/README.md).

**Immutable object**: an object whose state cannot change after construction. An immutable object needs no lock to be shared between threads. See [Design Principles](lld/fundamentals/03-design-principles.md).

**LLD (low-level design)**: the design of the classes, interfaces, and interactions inside one program. See [LLD](lld/README.md).

**Lock ordering**: a rule that every thread takes several locks in one global order (for example by account id), which prevents deadlock. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

### M–R

**Machine-coding round**: an interview round of 90 to 120 minutes in which the candidate writes a complete, working program that the evaluator runs and reads. See [The Machine-Coding Round](lld/machine-coding-round.md).

**Optimistic locking**: a concurrency method that takes no lock while a change is prepared, and checks a version number when it writes. If the version changed, the write fails and the caller retries. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Pattern (in DSA)**: a reusable method of solving a family of problems, for example the sliding window. See [How to Choose a Pattern](dsa/how-to-choose-a-pattern.md).

**Race condition**: a defect in which the result depends on the timing of threads, for example two threads that both book the last seat. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Repository**: an object that gives the domain a collection-like interface to stored objects and hides how they are stored. See [Architectural Patterns](lld/fundamentals/10-architectural-patterns.md).

### S–Z

**SOLID**: five object-oriented design principles: single responsibility, open-closed, Liskov substitution, interface segregation, and dependency inversion. See [SOLID](lld/fundamentals/02-solid.md).

**Time complexity**: how the running time of an algorithm grows with the size of its input, written in Big-O notation. See [the DSA cheatsheet](dsa/cheatsheet.md).

**Value object**: a domain object that has no identity and is defined only by its values (money, a date range). Value objects are immutable. See [Domain Modeling](lld/fundamentals/05-domain-modeling.md).
