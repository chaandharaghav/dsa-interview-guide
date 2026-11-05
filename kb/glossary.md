# Glossary

This page gives one definition for each term that the KB uses. Every page uses the term that this page lists. A page does not use a different word for the same concept.

## Entry Format

Each entry has this form:

> **Term** (abbreviation): definition in one or two sentences. See [the page that explains it](README.md).

Each phase of the KB adds the terms that its pages introduce.

## Terms

### A–F

**ACID**: the four guarantees of a database transaction: atomicity (all or nothing), consistency (the transaction keeps the constraints), isolation (concurrent transactions do not see each other's partial work, to the degree that the isolation level promises), and durability (a committed transaction survives a crash). See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**Adjacency list**: a graph representation that stores, for each node, the list of its neighbors. It uses O(V + E) memory. See [Graph Representations](dsa/foundations/07-graph-representations.md).

**Aggregate**: a group of objects that the code changes as one unit, with one entry object (the aggregate root) that protects the group's rules. See [Domain Modeling](lld/fundamentals/05-domain-modeling.md).

**Aggregation (UML)**: a whole-part association where the part can exist without the whole and can belong to more than one whole. UML gives it weak semantics, so a plain association is often the safer choice. See [UML Diagrams](lld/fundamentals/04-uml-class-and-sequence-diagrams.md).

**Amortized time complexity**: the average cost of one operation over a worst-case sequence of operations. Example: `ArrayList.add` is O(1) amortized, because the occasional resize cost spreads over the many adds that come before it. See [the Java toolkit](dsa/java-toolkit.md).

**Autoboxing**: the automatic conversion between a primitive value and its wrapper object, for example `int` to `Integer`. Unboxing a `null` wrapper throws `NullPointerException`. See [Java Language Core](java/01-language-core.md).

**B+ tree**: a balanced search tree whose nodes are disk pages with hundreds of keys each. Inner pages route a search, the leaf pages hold the entries in key order, and the leaves link to each other for range scans. Most relational indexes are B+ trees. See [Indexing](databases/03-indexing.md).

**Balanced BST (balanced binary search tree)**: a binary search tree that keeps its height at O(log n) by rebalancing on insert and delete, for example an AVL tree or a red-black tree. `TreeMap` is a red-black tree. See [Trees and Balanced BSTs](dsa/foundations/05-trees-and-balanced-bsts.md).

**Bean (Spring)**: an object that the Spring container creates, configures, and manages. See [Spring Core](java/17-spring-core.md).

**Big-O notation**: an upper bound on the growth of a cost function: f(n) is O(g(n)) when f(n) ≤ c·g(n) for some constant c and all large n. See [Complexity Analysis](dsa/foundations/01-complexity-analysis.md).

**Binary heap**: a complete binary tree stored in an array, where each parent is no larger (min-heap) or no smaller (max-heap) than its children. `PriorityQueue` is a binary min-heap. See [Heaps](dsa/foundations/06-heaps.md).

**Bloom filter**: a bit array with k hash functions that answers "possibly in the set" or "definitely not in the set". It has false positives and no false negatives. See [Probabilistic Data Structures](hld/distributed-systems/04-probabilistic-data-structures.md).

**Buffer pool**: the memory area where a database keeps copies of disk pages, so that repeated reads skip the disk. PostgreSQL calls it `shared_buffers`. See [Storage Engines](databases/06-storage-engines.md).

**CAP theorem**: the result (Gilbert and Lynch, 2002) that during a network partition a replicated system cannot give both linearizable reads and writes and a response from every non-failing node. See [Consistency and CAP](hld/fundamentals/07-consistency-and-cap.md).

**Carrier thread**: a platform thread that runs a virtual thread while the virtual thread is mounted on it. See [Virtual Threads](java/10-virtual-threads.md).

**CDN (content delivery network)**: a set of edge servers near users that cache content from an origin server, to cut latency and origin load. See [Load Balancing, DNS, and CDN](hld/fundamentals/02-load-balancing-dns-cdn.md).

**Change data capture (CDC)**: a method that reads the committed changes of a database from its log (for example the PostgreSQL WAL or the MySQL binlog) and sends them as events to other systems. See [OLTP, OLAP, and Warehousing](databases/10-oltp-olap-and-warehousing.md).

**Circuit breaker**: a client-side component that stops calls to a failing dependency for a time (OPEN), then lets a few test calls through (HALF_OPEN) before normal calls resume (CLOSED). See [Reliability Patterns](hld/fundamentals/12-reliability-patterns.md).

**Class loader**: the JVM component that finds the bytes of a class and defines the class. Java 9 and later use the boot, platform, and application loaders. See [JVM Architecture](java/05-jvm-architecture.md).

**Clustered index**: an index whose leaf pages hold the table rows themselves, in key order. In InnoDB the primary key is the clustered index, and each secondary index stores the primary key of the row. See [Indexing](databases/03-indexing.md).

**Column store**: a storage layout that keeps the values of each column together, so that a query that reads few columns of many rows reads less data and compresses better. Analytical databases use it. See [OLTP, OLAP, and Warehousing](databases/10-oltp-olap-and-warehousing.md).

**Compaction (LSM tree)**: the background merge of sorted files of an LSM tree into fewer, larger files, which drops overwritten values and expired tombstones. See [Storage Engines](databases/06-storage-engines.md).

**Composition (UML)**: a whole-part association where a part belongs to at most one whole at a time, and deleting the whole deletes its parts. See [UML Diagrams](lld/fundamentals/04-uml-class-and-sequence-diagrams.md).

**Composition over inheritance**: the design rule that a class gets behavior by holding other objects, not by extending a parent class. See [OOP](lld/fundamentals/01-oop.md).

**Consistent hashing**: a partitioning method that places nodes and keys on a hash ring, so that a change in the node count moves only about 1/N of the keys. See [Partitioning and Sharding](hld/fundamentals/06-partitioning-and-sharding.md).

**Covering index**: an index that holds every column that a query reads, so that the database answers the query from the index without reading the table row. See [Indexing](databases/03-indexing.md).

**CQRS (command query responsibility segregation)**: a design with separate models for writes and for reads, where the read model updates from the write side and can lag behind it. See [Event-Driven Architecture](hld/fundamentals/16-event-driven-architecture.md).

**CRDT (conflict-free replicated data type)**: a data type whose replicas accept updates independently and merge to the same state without coordination. See [CRDTs and Conflict Resolution](hld/distributed-systems/06-crdts-and-conflict-resolution.md).

**Critical section**: a block of code that only one thread may run at a time, because it reads and writes shared state. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Dependency injection container**: a framework component that creates objects and supplies their dependencies, for example the Spring `ApplicationContext`. See [Spring Core](java/17-spring-core.md).

**Dependency injection**: a technique in which an object receives the objects it depends on (usually through its constructor) instead of creating them. It is one way to apply dependency inversion. See [SOLID](lld/fundamentals/02-solid.md).

**Dependency inversion**: the design rule that high-level code and low-level code both depend on abstractions, not on each other's concrete classes. See [SOLID](lld/fundamentals/02-solid.md).

**Design pattern**: a named, reusable solution to a recurring design problem, with a defined intent and structure. The GoF book catalogs 23 of them. See [Creational Patterns](lld/fundamentals/06-creational-patterns.md).

**Double-entry ledger**: a record of money in which every transfer writes a debit entry and a credit entry of the same amount, so the sum of all entries is zero. See [Digital Wallet](lld/case-studies/23-digital-wallet/README.md).

**DSA (data structures and algorithms)**: the subject of how to store data and how to compute results from it within time and memory limits. See [DSA](dsa/README.md).

**Entity**: a domain object with an identity that stays the same while its attributes change (an order, a member). Compare value object. See [Domain Modeling](lld/fundamentals/05-domain-modeling.md).

**Event sourcing**: a design that stores every change to an entity as an append-only event and computes the current state by replaying the events. See [Event-Driven Architecture](hld/fundamentals/16-event-driven-architecture.md).

**Eventual consistency**: the guarantee that replicas reach the same value when updates stop. It gives no bound on how stale a read can be before that. See [Consistency and CAP](hld/fundamentals/07-consistency-and-cap.md).

**Fan-out on write**: a feed design that writes each new post into the stored feed of every follower when the post is created. Its opposite, fan-out on read, builds the feed when a user reads it. See [Design a News Feed](hld/case-studies/05-news-feed.md).

**Fencing token**: a number that increases with each lock or lease grant. The protected resource rejects a request with a token lower than the highest token it has seen. See [Distributed Locks and Leases](hld/distributed-systems/05-distributed-locks-and-leases.md).

**Fenwick tree (binary indexed tree)**: an array-based structure that supports a point update and a prefix-sum query in O(log n) each. See [Segment Tree and Fenwick Tree](dsa/21-segment-tree-and-fenwick-tree/README.md).

**Functional dependency**: a rule X → Y of the business that says that rows with equal values of attributes X have equal values of attributes Y. Normal forms are defined with functional dependencies. See [Schema Design and Normalization](databases/02-schema-design-and-normalization.md).

### G–L

**Garbage collector (GC)**: the JVM component that frees memory used by objects that no live reference reaches. JDK 21 uses G1 by default on a server-class machine. See [Memory and Garbage Collection](java/06-memory-and-gc.md).

**GoF (Gang of Four)**: Gamma, Helm, Johnson, and Vlissides, the authors of *Design Patterns* (1994), the book that cataloged the 23 classic design patterns. See [Creational Patterns](lld/fundamentals/06-creational-patterns.md).

**Happens-before**: the Java Memory Model relation that guarantees one action's effects are visible to another action. Examples: an unlock happens-before a later lock of the same monitor. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**HLD (high-level design)**: the design of a distributed system: its services, data stores, and the communication between them. See [HLD](hld/README.md).

**Idempotency key**: a unique key that a client sends with a request, so that the server performs the request at most once and returns the stored result for a repeated request. See [Digital Wallet](lld/case-studies/23-digital-wallet/README.md).

**Immutable object**: an object whose state cannot change after construction. An immutable object needs no lock to be shared between threads. See [Design Principles](lld/fundamentals/03-design-principles.md).

**Isolation level**: the setting that says which anomalies of concurrent transactions a database prevents. The SQL standard names four levels: read uncommitted, read committed, repeatable read, and serializable. Products implement each level differently. See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**Java Memory Model (JMM)**: the part of the Java Language Specification (chapter 17) that defines which values a read of a shared variable can see, through the happens-before relation. See [Concurrency Basics](java/07-concurrency-basics.md).

**JIT compiler (just-in-time compiler)**: the JVM component that compiles frequently run bytecode to machine code while the program runs. HotSpot has the C1 and C2 compilers. See [JVM Architecture](java/05-jvm-architecture.md).

**Lease**: a lock with an expiry time. The holder must renew it before it expires, or another node can take it. See [Distributed Locks and Leases](hld/distributed-systems/05-distributed-locks-and-leases.md).

**Linearizability**: the consistency model in which every operation appears to take effect at one instant between its start and its end, so that all clients see one order of operations that matches real time. See [Consistency and CAP](hld/fundamentals/07-consistency-and-cap.md).

**LLD (low-level design)**: the design of the classes, interfaces, and interactions inside one program. See [LLD](lld/README.md).

**Load factor**: the number of entries divided by the number of buckets in a hash table. `HashMap` doubles its bucket array when the load factor passes 0.75. See [Hash Tables](dsa/foundations/04-hash-tables.md).

**Lock ordering**: a rule that every thread takes several locks in one global order (for example by account id), which prevents deadlock. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Lost update**: an anomaly in which two transactions read the same value, each computes a new value from it, and the second write overwrites the first, so one change disappears. See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**LSM tree (log-structured merge tree)**: a storage structure that writes changes to a sorted in-memory table, flushes it to immutable sorted files, and merges the files in the background. It turns random writes into sequential writes, and a read may check several files. See [Storage Engines](databases/06-storage-engines.md).

### M–R

**Machine-coding round**: an interview round of 90 to 120 minutes in which the candidate writes a complete, working program that the evaluator runs and reads. See [The Machine-Coding Round](lld/machine-coding-round.md).

**Minimum spanning tree (MST)**: a set of edges that connects all nodes of a weighted undirected graph with the minimum total weight. Kruskal's and Prim's algorithms find one. See [Advanced Graphs](dsa/24-advanced-graphs/README.md).

**Modular inverse**: for an integer a and a modulus m with gcd(a, m) = 1, the integer x with a·x ≡ 1 (mod m). It replaces division in modular arithmetic. See [Math and Number Theory](dsa/23-math-and-number-theory/README.md).

**MVCC (multi-version concurrency control)**: a concurrency method in which a write creates a new version of a row and keeps the old one, so that each transaction reads the versions that its snapshot can see. Readers do not block writers. See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**N+1 query problem**: a data-access defect in which code runs one query for a list and then one more query for each item in the list. JPA lazy loading causes it. See [Query Execution and Tuning](databases/05-query-execution-and-tuning.md) for the theory and [JDBC, JPA, and Hibernate](java/16-jdbc-jpa-and-hibernate.md) for the JPA causes.

**Normal form**: a rule about the functional dependencies of a table that removes a class of update anomalies. The forms that interviews ask about are 1NF, 2NF, 3NF, and BCNF (Boyce-Codd normal form). See [Schema Design and Normalization](databases/02-schema-design-and-normalization.md).

**Optimistic locking**: a concurrency method that takes no lock while a change is prepared, and checks a version number when it writes. If the version changed, the write fails and the caller retries. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Partitioning (sharding)**: the split of a data set into parts that different nodes store, so that data size and write load can grow beyond one machine. See [Partitioning and Sharding](hld/fundamentals/06-partitioning-and-sharding.md).

**Pattern (in DSA)**: a reusable method of solving a family of problems, for example the sliding window. See [How to Choose a Pattern](dsa/how-to-choose-a-pattern.md).

**Persistence context**: the set of managed entity instances that one JPA `EntityManager` tracks. It works as a first-level cache and detects changes at flush time. See [JDBC, JPA, and Hibernate](java/16-jdbc-jpa-and-hibernate.md).

**Phantom read**: an anomaly in which a transaction runs the same query with a condition twice and gets a different set of rows, because another transaction inserted or deleted a matching row. See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**Pinning (virtual threads)**: the state in which a virtual thread cannot unmount from its carrier thread while it blocks, so the carrier thread blocks too. See [Virtual Threads](java/10-virtual-threads.md).

**Proxy (Spring AOP)**: an object that Spring creates around a bean to add behavior, such as transactions, before and after method calls. A call from the bean to its own method does not go through the proxy. See [Spring Core](java/17-spring-core.md).

**Query plan**: the tree of operations (scans, joins, sorts) that the optimizer of a database chooses to run a query. `EXPLAIN` shows it. See [Query Execution and Tuning](databases/05-query-execution-and-tuning.md).

**Quorum**: the minimum number of replicas that must answer a read (R) or a write (W). With N replicas, R + W > N makes every read set overlap every write set. See [Replication](hld/fundamentals/05-replication.md).

**Race condition**: a defect in which the result depends on the timing of threads, for example two threads that both book the last seat. See [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md).

**Replication lag**: the delay between a write on the leader and its arrival on a follower. Reads from a lagging follower return old data. See [Replication](hld/fundamentals/05-replication.md).

**Repository**: an object that gives the domain a collection-like interface to stored objects and hides how they are stored. See [Architectural Patterns](lld/fundamentals/10-architectural-patterns.md).

**Reservoir sampling**: a one-pass method that keeps a uniform random sample of k items from a stream of unknown length. See [Randomized Algorithms](dsa/28-randomized-algorithms/README.md).

**Rolling hash**: a hash of a fixed-length window that the code updates in O(1) when the window moves by one character. Rabin-Karp search uses it. See [String Algorithms](dsa/22-string-algorithms/README.md).

**RPO and RTO (recovery point objective, recovery time objective)**: RPO is the maximum data loss, measured in time, that a failure may cause. RTO is the maximum time to restore service. See [Multi-Region Design and Disaster Recovery](hld/fundamentals/19-multi-region-and-disaster-recovery.md).

### S–Z

**Saga**: a sequence of local transactions across services, where each step has a compensating action that undoes it semantically if a later step fails. See [Distributed Transactions](hld/distributed-systems/03-distributed-transactions.md).

**Sargable predicate**: a condition that a database can answer with an index range scan, for example `created_at >= ?`. A function on the column, such as `date(created_at) = ?`, makes the condition not sargable. See [Query Execution and Tuning](databases/05-query-execution-and-tuning.md).

**Segment tree**: a binary tree over an array in which each node stores an aggregate (sum, minimum, maximum) of a range. It answers a range query and applies an update in O(log n). See [Segment Tree and Fenwick Tree](dsa/21-segment-tree-and-fenwick-tree/README.md).

**Selectivity**: the fraction of rows that a condition selects. The optimizer estimates it from statistics and uses it to choose between an index scan and a full scan. See [Indexing](databases/03-indexing.md).

**SLI, SLO, SLA**: an SLI (service level indicator) is a measured value such as the ratio of successful requests. An SLO (service level objective) is the target for an SLI. An SLA (service level agreement) is a contract with customers, with a penalty, and it is looser than the SLO. See [Scalability and Availability](hld/fundamentals/01-scalability-and-availability.md).

**Snapshot isolation**: an isolation level in which each transaction reads from one consistent snapshot and a write fails if another transaction committed a change to the same row first. It prevents lost updates and allows write skew. PostgreSQL repeatable read is snapshot isolation. See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**SOLID**: five object-oriented design principles: single responsibility, open-closed, Liskov substitution, interface segregation, and dependency inversion. See [SOLID](lld/fundamentals/02-solid.md).

**Stable sort**: a sort that keeps the input order of elements with equal keys. `Arrays.sort` on object arrays is stable (TimSort). Stability does not matter for primitive arrays, because two equal primitive values cannot be told apart. See [Sorting Algorithms](dsa/foundations/08-sorting-algorithms.md).

**Star schema**: a data warehouse model with one fact table of measured events in the center and dimension tables (date, product, customer) that describe each event. See [OLTP, OLAP, and Warehousing](databases/10-oltp-olap-and-warehousing.md).

**Strongly connected component (SCC)**: a maximal set of nodes in a directed graph in which every node can reach every other node. Tarjan's and Kosaraju's algorithms find all of them in O(V + E). See [Advanced Graphs](dsa/24-advanced-graphs/README.md).

**Time complexity**: how the running time of an algorithm grows with the size of its input, written in Big-O notation. See [the DSA cheatsheet](dsa/cheatsheet.md).

**Token bucket**: a rate-limiting algorithm that adds tokens at a fixed rate up to a capacity and spends one token per request. It allows bursts up to the capacity. See [Rate Limiting Algorithms](hld/fundamentals/13-rate-limiting-algorithms.md).

**Tombstone**: a marker that an LSM-based store writes for a delete, because it cannot change its immutable files in place. A read must skip the deleted data, and compaction removes the tombstone later. See [NoSQL Families](databases/07-nosql-families.md).

**Transaction propagation**: the rule that decides what a `@Transactional` method does when a transaction already exists, for example join it (`REQUIRED`) or suspend it and start a new one (`REQUIRES_NEW`). See [Spring Data and Transactions](java/19-spring-data-and-transactions.md).

**Transactional outbox**: a pattern that writes a business change and its event row in one local transaction. A relay then publishes the event rows, at least once. See [Distributed Transactions](hld/distributed-systems/03-distributed-transactions.md).

**Type erasure**: the compiler step that removes generic type arguments, so that `List<String>` and `List<Integer>` have the same class at run time. See [Generics](java/02-generics.md).

**Value object**: a domain object that has no identity and is defined only by its values (money, a date range). Value objects are immutable. See [Domain Modeling](lld/fundamentals/05-domain-modeling.md).

**Virtual thread**: a lightweight thread that the JDK schedules onto a small number of carrier threads, final in Java 21 (JEP 444). It suits code that waits on I/O. See [Virtual Threads](java/10-virtual-threads.md).

**Watermark**: in stream processing, an estimate that no more events with an event time earlier than a given time will arrive. It decides when a window can close. See [Batch and Stream Processing](hld/fundamentals/17-batch-and-stream-processing.md).

**Window function**: an SQL function that computes a value for each row from a set of related rows (its window), without collapsing the rows as `GROUP BY` does. Examples: `ROW_NUMBER`, `RANK`, `LAG`, and a running `SUM`. See [The Relational Model and SQL](databases/01-relational-model-and-sql.md).

**Write skew**: an anomaly in which two transactions read overlapping data, each writes a different row, and together the writes break a rule that each transaction checked. Snapshot isolation allows it. See [Transactions and Isolation](databases/04-transactions-and-isolation.md).

**Write-ahead log (WAL)**: a log of changes that a database writes to disk before it changes the data pages, so that it can redo committed changes after a crash. See [Storage Engines](databases/06-storage-engines.md).
