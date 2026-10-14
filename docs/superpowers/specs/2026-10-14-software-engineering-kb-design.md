# Software Engineering Knowledge Base: Design Spec

- **Date:** 2026-10-14 (revision 2)
- **Status:** Approved by the user on 2026-10-14, with the revision 2 changes.
- **Scope:** The repo changes from a DSA interview guide into a knowledge base (KB) for software engineering.

## Revision 2 Changes

The user gave these instructions after revision 1:

1. Drop the mobile requirement. The user reads the KB on a laptop.
2. Use the best reader for a laptop. Be detailed. Do not cut content to fit a screen.
3. The KB must be the only resource the user needs for interview preparation. It must not skip a topic.

This revision makes these changes:

- Section 6 now has laptop reading rules. The mobile rules are gone.
- The main reader is a local MkDocs Material site (section 2.2).
- The KB content moves into a `kb/` folder, because MkDocs needs a content folder below the config file.
- Section 3 adds DSA foundations, 8 new DSA patterns, more Java and Spring pages, more HLD and LLD pages, and practice files for each subject.

---

## 1. Goal and Decisions

### 1.1 Goal

The KB makes the user a better software developer. Interview preparation has the highest weight. The KB is the only resource the user needs for interview preparation. The KB also covers the topics that matter on the job, even when interviews ask about them less often.

### 1.2 Decisions the user made

| Decision | Value |
|---|---|
| Depth | Each topic has fundamentals and an interview angle (option C) |
| Priority | Interview preparation first |
| Coverage | All important topics. The KB is the only resource for preparation |
| Drafting | Sonnet subagents may draft. Opus reviews every draft |
| Reading | Laptop. Detail is more important than page length |

### 1.3 Decisions this spec makes

| Decision | Value | Reason |
|---|---|---|
| Repo | One repo, one folder per subject | Files can link across subjects |
| Content root | `kb/` | MkDocs does not accept the folder of its config file as the content folder |
| Existing DSA content | Moves into `kb/dsa/` with `git mv` | Git history follows the files |
| Repo folder name | No change | The Claude project memory path depends on it. The user can rename it later |
| Reader | MkDocs Material, served on the laptop | It gives a sidebar, full-text search, a contents panel, Mermaid, and collapsible answers |
| Format | Markdown with MkDocs Material extensions | GitHub, VS Code, and Obsidian can also show the files. They show the extensions as plain text |
| Java version | Java 21 for new content | JDK 21 is installed. Existing DSA code stays valid on Java 17 |
| Distributed systems | A section inside `kb/hld/` | HLD interviews test these ideas. One location prevents duplicate content |

---

## 2. Repo Structure and Reader

### 2.1 Structure

```
dsa-interview-guide/                  (folder name unchanged)
├── README.md                         how to open the KB site
├── mkdocs.yml                        site configuration
├── requirements.txt                  pinned MkDocs packages
├── kb/                               all KB content
│   ├── README.md                     KB home page and index
│   ├── study-plan.md                 plan across all subjects
│   ├── glossary.md                   one definition per term
│   ├── dsa/                          current content, plus new pages
│   ├── lld/
│   ├── hld/
│   ├── java/
│   ├── databases/
│   ├── networking/
│   ├── operating-systems/
│   ├── api-design/
│   ├── security/
│   ├── engineering-practice/
│   ├── behavioral/
│   └── ai-engineering/
├── tools/                            checker scripts
└── docs/superpowers/                 specs and plans (not KB content)
```

Each subject folder has a `README.md`. That file lists the pages in study order and gives the interview priority of each page. MkDocs uses each `README.md` as the index page of its folder.

Files and folders inside a subject use numeric prefixes (`01-`, `02-`) for study order. MkDocs sorts the sidebar by file name, so the prefixes also set the sidebar order.

### 2.2 Reader

- The user runs `tools/serve.sh`. The script creates a Python virtual environment, installs `requirements.txt`, and runs `mkdocs serve`. The site opens at `http://127.0.0.1:8000`.
- Site features: navigation tabs for each subject, a sidebar, search, light and dark themes, a copy button on code blocks, Mermaid diagrams, admonitions, collapsible blocks, and content tabs.
- `mkdocs build --strict` must pass. Strict mode fails the build on a broken link.
- The site is local. Nothing is published.

---

## 3. Topic Inventory

Each line is one file. Paths are relative to `kb/<subject>/`.

Priority labels:
- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

### 3.1 `dsa/`

**Existing content.** The 20 pattern folders and the root DSA files move here. Opus fixes their links. Their content does not change, except for these updates:
- `how-to-choose-a-pattern.md` adds routes to patterns 21 to 28.
- `cheatsheet.md`, `tricks-at-a-glance.md`, and `study-plan.md` add patterns 21 to 28.
- Each existing pattern page that only mentions a new topic (for example, segment trees in pattern 04) links to the new page.

**New: `foundations/`.** These pages come before pattern 01 in the study order.

| File | Content | Priority |
|---|---|---|
| `01-complexity-analysis.md` | Big-O, Big-Theta, Big-Omega. Amortized analysis. Recursion trees. Space complexity. How input limits set the target complexity | P1 |
| `02-arrays-strings-linked-lists.md` | Memory layout, dynamic array growth, string immutability in Java, singly and doubly linked lists | P1 |
| `03-stacks-queues-deques.md` | Array and linked implementations, circular buffers, `ArrayDeque` | P1 |
| `04-hash-tables.md` | Hash functions, chaining, open addressing, load factor, resize cost | P1 |
| `05-trees-and-balanced-bsts.md` | Tree terms, BST operations, AVL and red-black trees (rotations at the concept level), B-trees in brief | P1 |
| `06-heaps.md` | Binary heap layout, sift up and sift down, heapify in O(n) | P1 |
| `07-graph-representations.md` | Adjacency list, adjacency matrix, edge list, implicit graphs | P1 |
| `08-sorting-algorithms.md` | Insertion, selection, bubble, merge, quick, heap, counting, radix, bucket sort. Stability. What `Arrays.sort` uses for primitive and object arrays | P1 |

**New patterns.**

| Folder | Content | Priority |
|---|---|---|
| `21-segment-tree-and-fenwick-tree/` | Range queries with updates, lazy propagation, Fenwick tree, when prefix sums are not enough | P2 |
| `22-string-algorithms/` | KMP, Z-function, Rabin-Karp and rolling hash, Manacher | P2 |
| `23-math-and-number-theory/` | GCD and LCM, sieve, modular arithmetic, fast exponentiation, modular inverse, combinations, basic geometry | P1 |
| `24-advanced-graphs/` | Minimum spanning tree (Kruskal and Prim), strongly connected components (Kosaraju and Tarjan), bridges and articulation points, Euler paths, maximum flow in brief | P2 |
| `25-matrix-and-simulation/` | Traversal orders, rotation, in-place marking, simulation problems | P1 |
| `26-design-data-structures/` | Min stack, LFU cache, insert-delete-getRandom in O(1), median finder, iterators, time-based key-value store, hit counter | P1 |
| `27-advanced-dp/` | Bitmask DP, digit DP, tree DP, rerooting, state-machine DP, probability DP | P2 |
| `28-randomized-algorithms/` | Reservoir sampling, Fisher-Yates shuffle, weighted random choice, rejection sampling | P2 |

Each new pattern page uses the existing 10-section DSA template and ends with the "Commonly Confused With" table.

### 3.2 `lld/` (low-level design)

Root files:

| File | Content | Priority |
|---|---|---|
| `how-to-approach-lld.md` | The LLD interview procedure: requirements, entities, relationships, class diagram, APIs, code, extensions. A time budget for a 45-minute round | P1 |
| `machine-coding-round.md` | The 90-minute machine-coding round: package layout, in-memory storage, a driver class, tests, what evaluators score | P1 |
| `cheatsheet.md` | One page: SOLID, each design pattern with its intent and its trigger, the procedure | P1 |
| `practice-and-mock-interviews.md` | Extra LLD problems without solutions, timed mock sets, self-evaluation rubric | P1 |

`fundamentals/`

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

### 3.3 `hld/` (high-level design)

Root files:

| File | Content | Priority |
|---|---|---|
| `how-to-approach-hld.md` | The HLD interview procedure: requirements, estimation, API, data model, diagram, deep dives, trade-offs. Time budget | P1 |
| `back-of-envelope-estimation.md` | Powers of 2, latency numbers, QPS, storage, bandwidth. Worked examples | P1 |
| `cheatsheet.md` | One page: building blocks, which tool solves which problem, key numbers | P1 |
| `practice-and-mock-interviews.md` | Extra HLD prompts without solutions, timed mock sets, self-evaluation rubric | P1 |

`fundamentals/`

| File | Content | Priority |
|---|---|---|
| `01-scalability-and-availability.md` | Vertical and horizontal scaling, stateless services, latency and throughput, availability numbers, SLA, SLO, SLI | P1 |
| `02-load-balancing-dns-cdn.md` | L4 and L7 load balancers, algorithms, health checks. DNS in system design. CDN push and pull | P1 |
| `03-caching.md` | Cache-aside, read-through, write-through, write-back, write-around. Eviction, invalidation, stampede, hot keys | P1 |
| `04-choosing-a-database.md` | SQL or NoSQL for a given access pattern. Links to `databases/` for internals | P1 |
| `05-replication.md` | Leader-follower, multi-leader, leaderless. Replication lag and its effects | P1 |
| `06-partitioning-and-sharding.md` | Hash and range partitioning, consistent hashing, rebalancing, hot spots | P1 |
| `07-consistency-and-cap.md` | CAP, PACELC, consistency models, quorums | P1 |
| `08-message-queues-and-streams.md` | Kafka, RabbitMQ, SQS. Delivery semantics, ordering, backpressure, dead-letter queues | P1 |
| `09-communication-patterns.md` | REST, gRPC, WebSocket, long polling, SSE, webhooks: which to use when | P1 |
| `10-storage-types.md` | Block, file, and object storage. Blob stores, data lakes | P2 |
| `11-search-and-indexing.md` | Inverted index, Elasticsearch architecture, ranking basics | P2 |
| `12-reliability-patterns.md` | Timeouts, retries, backoff, jitter, circuit breaker, bulkhead, idempotency, graceful degradation | P1 |
| `13-rate-limiting-algorithms.md` | Token bucket, leaky bucket, fixed window, sliding log, sliding window counter | P1 |
| `14-microservices-and-monoliths.md` | Service boundaries, API gateway, service discovery, service mesh | P1 |
| `15-unique-id-generation.md` | UUID versions, Snowflake, ticket servers | P1 |
| `16-event-driven-architecture.md` | Events and commands, event sourcing, CQRS, choreography and orchestration | P1 |
| `17-batch-and-stream-processing.md` | MapReduce, Spark, Flink, Lambda and Kappa architectures | P2 |
| `18-geospatial-indexing.md` | Geohash, quadtree, S2 | P2 |
| `19-multi-region-and-disaster-recovery.md` | Active-active and active-passive, RPO and RTO, failover, data residency | P2 |
| `20-observability-in-design.md` | What to measure in a design. Links to `engineering-practice/` | P2 |
| `21-security-in-design.md` | Authentication at the gateway, encryption in transit and at rest. Links to `security/` | P2 |

`distributed-systems/`

| File | Content | Priority |
|---|---|---|
| `01-failures-and-time.md` | Partial failure, network partitions, clock drift, Lamport clocks, vector clocks | P2 |
| `02-consensus-and-leader-election.md` | Raft in detail, Paxos in brief, ZooKeeper and etcd | P1 |
| `03-distributed-transactions.md` | 2PC, saga, transactional outbox, exactly-once claims | P1 |
| `04-probabilistic-data-structures.md` | Bloom filter, count-min sketch, HyperLogLog, Merkle tree, gossip | P1 |
| `05-distributed-locks-and-leases.md` | Lease-based locks, fencing tokens, the Redlock debate | P2 |
| `06-crdts-and-conflict-resolution.md` | Last-writer-wins, version vectors, CRDTs, operational transformation | P2 |

`case-studies/`: each case study is one Markdown file.

| File | Priority |
|---|---|
| `01-url-shortener.md` | P1 |
| `02-distributed-rate-limiter.md` | P1 |
| `03-key-value-store.md` (Dynamo-style) | P1 |
| `04-notification-system.md` | P1 |
| `05-news-feed.md` | P1 |
| `06-chat-system.md` | P1 |
| `07-search-autocomplete.md` | P1 |
| `08-web-crawler.md` | P1 |
| `09-video-streaming.md` | P1 |
| `10-file-storage-and-sync.md` (Dropbox-style) | P1 |
| `11-ride-hailing-and-proximity.md` | P1 |
| `12-ticket-booking.md` | P1 |
| `13-payment-system.md` | P1 |
| `14-distributed-message-queue.md` | P2 |
| `15-distributed-cache.md` | P2 |
| `16-leaderboard-and-top-k.md` | P1 |
| `17-metrics-and-monitoring.md` | P2 |
| `18-photo-sharing.md` | P2 |
| `19-collaborative-editor.md` | P2 |
| `20-distributed-job-scheduler.md` | P2 |
| `21-ad-click-aggregation.md` | P2 |
| `22-stock-exchange.md` | P2 |
| `23-email-service.md` | P2 |
| `24-ecommerce-flash-sale.md` | P2 |
| `25-online-code-judge.md` | P2 |

### 3.4 `java/` (Java in depth)

| File | Content | Priority |
|---|---|---|
| `01-language-core.md` | Pass-by-value, `equals` and `hashCode`, immutability, the string pool, autoboxing, `final` and `static`, nested classes | P1 |
| `02-generics.md` | Type erasure, bounded types, wildcards, PECS | P1 |
| `03-exceptions.md` | Checked and unchecked exceptions, try-with-resources, design rules | P1 |
| `04-collections-internals.md` | How HashMap, ArrayList, LinkedHashMap, TreeMap, PriorityQueue, and ConcurrentHashMap work inside. Fail-fast iterators | P1 |
| `05-jvm-architecture.md` | Class loading, runtime data areas, bytecode, the JIT compiler | P1 |
| `06-memory-and-gc.md` | Heap layout, G1, ZGC, GC logs, memory leaks, weak and soft references | P1 |
| `07-concurrency-basics.md` | Threads, `synchronized`, `volatile`, the Java Memory Model, happens-before, deadlock | P1 |
| `08-java-util-concurrent.md` | Executors, locks, atomics, latches, semaphores, barriers, concurrent collections, BlockingQueue | P1 |
| `09-completablefuture.md` | Composition, error handling, thread pools | P1 |
| `10-virtual-threads.md` | Virtual threads (final in Java 21). Structured concurrency and scoped values, labeled as preview features | P1 |
| `11-concurrency-coding-problems.md` | Classic thread-coordination problems: odd-even printing, bounded blocking queue, reader-writer lock, thread-safe Singleton, rate-limited executor | P1 |
| `12-functional-and-streams.md` | Lambdas, functional interfaces, streams, collectors, Optional, parallel stream pitfalls | P1 |
| `13-modern-java.md` | Features from Java 9 to 21: `var`, records, sealed classes, pattern matching, switch expressions, text blocks | P1 |
| `14-reflection-and-annotations.md` | Reflection API, annotations and processors, dynamic proxies | P2 |
| `15-io-nio-and-serialization.md` | Streams and readers, NIO buffers and channels, serialization risks | P3 |
| `16-jdbc-jpa-and-hibernate.md` | JDBC, entity states, lazy and eager loading, the N+1 problem, caching levels | P1 |
| `17-spring-core.md` | IoC, dependency injection, bean scopes and lifecycle, AOP and proxies | P1 |
| `18-spring-boot.md` | Auto-configuration, starters, profiles and configuration, Actuator | P1 |
| `19-spring-data-and-transactions.md` | Repositories, `@Transactional` propagation and isolation, self-invocation pitfall | P1 |
| `20-spring-web-and-rest.md` | DispatcherServlet flow, controllers, validation, exception handling | P1 |
| `21-spring-security.md` | Filter chain, authentication, authorization, JWT resource server | P2 |
| `22-microservices-with-spring.md` | Service discovery, config server, resilience, distributed tracing | P2 |
| `23-build-tools.md` | Maven and Gradle: lifecycle, dependency resolution, conflicts | P2 |
| `interview-question-bank.md` | Short questions with model answers. Each answer links to the page that explains it | P1 |

### 3.5 `databases/`

| File | Content | Priority |
|---|---|---|
| `01-relational-model-and-sql.md` | Joins, grouping, subqueries, CTEs, window functions | P1 |
| `02-schema-design-and-normalization.md` | Normal forms, denormalization, keys | P1 |
| `03-indexing.md` | B+ tree, hash, composite, and covering indexes. Selectivity. When an index is not used | P1 |
| `04-transactions-and-isolation.md` | ACID, anomalies, isolation levels, MVCC, locks, deadlocks | P1 |
| `05-query-execution-and-tuning.md` | EXPLAIN, join algorithms, the N+1 problem | P1 |
| `06-storage-engines.md` | WAL, B-tree engines, LSM trees, compaction | P2 |
| `07-nosql-families.md` | Key-value, document, wide-column, graph, time-series. When to choose each | P1 |
| `08-redis.md` | Data types, persistence, eviction, common uses | P1 |
| `09-scaling-a-database.md` | Read replicas, connection pools, partitioning from the database view | P2 |
| `10-oltp-olap-and-warehousing.md` | Row and column stores, star schema, ETL and ELT | P2 |
| `sql-interview-problems.md` | Classic SQL interview problems with solutions | P1 |
| `interview-question-bank.md` | Short questions with model answers and links | P1 |

### 3.6 `networking/`

| File | Content | Priority |
|---|---|---|
| `01-osi-and-tcp-ip-models.md` | The layers and what each layer does | P2 |
| `02-ip-addressing-and-routing.md` | IPv4 and IPv6, subnets and CIDR, routing basics | P2 |
| `03-tcp-and-udp.md` | Handshake, flow control, congestion control, TIME_WAIT | P1 |
| `04-http.md` | HTTP/1.1, HTTP/2, HTTP/3 and QUIC. Methods, status codes, caching headers, cookies, CORS | P1 |
| `05-tls-and-https.md` | The TLS 1.3 handshake, certificates, the chain of trust | P1 |
| `06-dns.md` | Resolution steps, record types, TTL | P1 |
| `07-what-happens-when-you-load-a-url.md` | The full request path, step by step | P1 |
| `08-websockets-and-realtime.md` | WebSocket, SSE, long polling at the protocol level | P2 |
| `09-proxies-and-nat.md` | Forward and reverse proxies, NAT | P2 |
| `interview-question-bank.md` | Short questions with model answers and links | P1 |

### 3.7 `operating-systems/`

| File | Content | Priority |
|---|---|---|
| `01-processes-and-threads.md` | Process and thread, context switch, user mode and kernel mode | P1 |
| `02-cpu-scheduling.md` | Scheduling algorithms and their trade-offs | P2 |
| `03-synchronization.md` | Mutex, semaphore, monitor, deadlock conditions. Classic problems in Java | P1 |
| `04-memory-management.md` | Virtual memory, paging, TLB, page replacement | P1 |
| `05-file-systems-and-io-models.md` | Inodes, buffering, blocking and non-blocking I/O, epoll, zero-copy | P2 |
| `06-computer-architecture-for-developers.md` | Memory hierarchy, CPU caches, cache lines, false sharing, branch prediction | P2 |
| `07-linux-for-developers.md` | Signals, permissions, shell basics, and diagnostic tools: `top`, `strace`, `lsof`, `ss` | P2 |
| `interview-question-bank.md` | Short questions with model answers and links | P1 |

### 3.8 `api-design/`

| File | Content | Priority |
|---|---|---|
| `01-rest-principles.md` | Resources, methods, idempotency, status codes | P1 |
| `02-api-design-practices.md` | Pagination, filtering, versioning, error format (RFC 9457), idempotency keys, rate limit headers | P1 |
| `03-grpc-and-protobuf.md` | Service definitions, streaming, schema evolution | P2 |
| `04-graphql.md` | Schema, resolvers, the N+1 problem, when to choose GraphQL | P2 |
| `05-webhooks-and-async-apis.md` | Delivery, retries, signatures | P2 |
| `06-api-design-in-interviews.md` | How to design an API in an interview. A review checklist | P1 |

### 3.9 `security/`

| File | Content | Priority |
|---|---|---|
| `01-security-fundamentals.md` | CIA triad, threat modeling with STRIDE, least privilege | P2 |
| `02-authentication.md` | Password hashing, sessions and tokens, MFA | P1 |
| `03-authorization-oauth-oidc-jwt.md` | RBAC, ABAC, OAuth 2.0 flows, OIDC, JWT pitfalls | P1 |
| `04-owasp-top-10.md` | The current OWASP Top 10, each with a Java example and a fix | P1 |
| `05-cryptography-basics.md` | Symmetric and asymmetric encryption, hashes, HMAC, signatures, key management | P2 |
| `06-secure-coding-in-java.md` | SQL injection, deserialization, secrets, input validation | P2 |
| `07-infrastructure-security.md` | Firewalls, VPN, mTLS, zero trust, secrets management, DDoS protection | P2 |

### 3.10 `engineering-practice/`

| File | Content | Priority |
|---|---|---|
| `01-clean-code.md` | Names, functions, comments, error handling | P2 |
| `02-refactoring-and-code-smells.md` | Common smells and the refactorings that fix them | P2 |
| `03-testing.md` | Test pyramid, test doubles, JUnit 5, Mockito, TDD, contract tests | P1 |
| `04-code-review.md` | What to check, how to write review comments | P2 |
| `05-git-in-depth.md` | Merge and rebase, bisect, reflog, internals | P2 |
| `06-ci-cd-and-deployment.md` | Pipelines, blue-green, canary, feature flags, rollback | P2 |
| `07-containers-and-kubernetes.md` | Docker images and layers, the main Kubernetes objects | P2 |
| `08-observability.md` | Metrics, logs, traces, RED and USE methods, SLO alerts, OpenTelemetry | P2 |
| `09-debugging-and-incidents.md` | A debugging procedure, profiling, incident response, postmortems | P2 |
| `10-performance-engineering.md` | Latency percentiles, load testing, JFR, flame graphs | P2 |
| `11-design-docs-and-adrs.md` | How to write a design doc and an architecture decision record | P3 |
| `12-cloud-fundamentals.md` | Regions and zones, compute, storage, and network services | P2 |
| `13-agile-and-estimation.md` | Scrum and Kanban, story points, how to estimate and track work | P3 |

### 3.11 `behavioral/`

| File | Content | Priority |
|---|---|---|
| `01-interview-process-overview.md` | The rounds at product companies, what each round tests, a preparation timeline | P1 |
| `02-resume-and-job-search.md` | Resume structure, impact statements, referrals, applications | P1 |
| `03-how-behavioral-rounds-work.md` | What interviewers score at each level | P1 |
| `04-star-and-story-bank.md` | The STAR method and a worksheet for the user's own stories | P1 |
| `05-common-questions.md` | Conflict, failure, ambiguity, leadership, with answer structures | P1 |
| `06-project-deep-dive.md` | How to present a past project: architecture, decisions, numbers, failures | P1 |
| `07-amazon-leadership-principles.md` | Each principle, what it tests, and a story outline | P1 |
| `08-engineering-levels-and-growth.md` | What changes from mid-level to senior to staff | P2 |
| `09-offers-and-negotiation.md` | How to compare offers and negotiate | P2 |

### 3.12 `ai-engineering/`

| File | Content | Priority |
|---|---|---|
| `01-llm-fundamentals.md` | Tokens, context windows, sampling, embeddings | P2 |
| `02-prompting-and-tool-use.md` | Prompt structure, tool calling, structured output | P2 |
| `03-rag.md` | Chunking, embeddings, vector search, evaluation | P2 |
| `04-designing-llm-systems.md` | Latency, cost, caching, evaluation, guardrails: the HLD view | P2 |
| `05-ai-assisted-development.md` | How to work with coding agents and how to review their output | P2 |

### 3.13 Root files

| File | Content |
|---|---|
| `README.md` (repo root) | How to open the site with `tools/serve.sh` |
| `kb/README.md` | The KB home page. It gives a reading order for interview preparation and a reading order for the job |
| `kb/study-plan.md` | A plan across subjects. It reuses the 12-week DSA plan and adds LLD, HLD, Java, and fundamentals tracks |
| `kb/glossary.md` | One definition per term, with a link to the page that explains it |

### 3.14 Size

The inventory has about 230 new files, including about 24 `Main.java` files and 12 subject index files. The work runs in phases (section 10).

---

## 4. Overlap Boundaries

A topic has one home page. Other pages link to that home page and do not repeat it.

| Topic | Home page | Other pages |
|---|---|---|
| Java collection APIs for problem solving | `dsa/java-toolkit.md` | `java/04-collections-internals.md` covers internals and links back |
| Data structure internals for DSA | `dsa/foundations/` | `java/04` covers the JDK implementations |
| Coding-round communication | `dsa/interview-tips.md` | `behavioral/` links to it |
| Company problem lists | `dsa/company-prep.md` | `behavioral/` covers company values only |
| LRU cache | `lld/case-studies/02-lru-and-lfu-cache` | `dsa/26-design-data-structures` links to it |
| Rate limiting algorithms | `hld/fundamentals/13-rate-limiting-algorithms.md` | The LLD and HLD case studies link to it |
| Notification service | LLD case study 24 covers classes. HLD case study 04 covers the distributed system | Each links to the other |
| Database internals | `databases/` | `hld/` covers which database to choose |
| Thread-safety primitives | `java/07` and `java/08` | `lld/fundamentals/09` and `operating-systems/03` link to them |
| Load balancers | `hld/fundamentals/02` | `networking/09` covers the protocol level only |
| Observability | `engineering-practice/08-observability.md` | `hld/fundamentals/20` covers what to measure in a design |
| The N+1 problem | `databases/05` | `java/16` and `api-design/04` link to it |
| Architectural patterns (CQRS, event sourcing) | `hld/fundamentals/16` | `lld/fundamentals/10` covers code-level architecture only |

---

## 5. Page Templates

### 5.1 Topic page

Each topic page has these sections in this order. The writer may omit a section only if it does not apply. The writer then states the reason in the review note.

1. **Why It Matters:** one paragraph. It says where the topic appears in interviews and on the job.
2. **Core Concepts:** the fundamentals, with definitions.
3. **How It Works:** the mechanism, with diagrams where a diagram shows the mechanism better than text.
4. **Trade-offs and When to Use:** a comparison against the alternatives.
5. **Java Example:** runnable code, if code applies to the topic.
6. **Interview Angle:**
   - the questions interviewers ask. Each model answer is in a collapsible block (`??? question "..."`), so the reader can answer first and then check;
   - the follow-up questions;
   - the answers that cost points (red flags).
7. **Common Mistakes**
8. **Commonly Confused With:** a table with the columns "Looks like", "Difference", and "Deciding question". This matches the DSA pages.
9. **Quick Reference Card:** a short summary to read before an interview.
10. **Further Reading:** primary sources.

### 5.2 LLD case study (`README.md` and `Main.java`)

1. **Problem Statement**
2. **Clarifying Questions:** each question with the answer the case study assumes.
3. **Requirements:** functional and non-functional.
4. **Entities and Relationships**
5. **Class Diagram:** Mermaid `classDiagram`.
6. **Design Patterns Used:** each pattern with the reason for it.
7. **Key Code:** the important classes. The full code is in `Main.java`.
8. **Concurrency:** what can race and how the design prevents the race.
9. **Extensions:** the follow-up changes interviewers ask for, and how the design handles each one.
10. **Common Mistakes**
11. **45-Minute Plan:** what to present in each part of the round.

`Main.java` is a single-file program. The command `java Main.java` runs it without a separate compile step. Its `main` method runs a demo scenario. The scenario prints output that shows the requirements work. The README shows the expected output.

### 5.3 HLD case study

1. **Problem Statement**
2. **Clarifying Questions**
3. **Requirements:** functional and non-functional.
4. **Estimation:** QPS, storage, and bandwidth, with each step shown.
5. **API Design**
6. **Data Model**
7. **High-Level Design:** a Mermaid diagram and a description of each component.
8. **Deep Dives:** two to four parts that interviewers probe.
9. **Bottlenecks and Scaling**
10. **Trade-offs:** each decision with the alternatives the design rejected.
11. **Follow-up Questions:** with model answers in collapsible blocks.
12. **45-Minute Plan**
13. **Further Reading:** engineering blogs and papers from the companies that built such systems.

### 5.4 DSA pattern page

New DSA pattern pages use the existing 10-section template of `dsa/01-sliding-window/README.md`, the "Commonly Confused With" table, and the "Quick Reference Card".

---

## 6. Laptop Reading Rules

1. Detail has priority over length. A writer does not cut content to make a page shorter.
2. A page covers one home topic (section 4). If a page has more than one home topic, the writer splits it.
3. Tables may have any number of columns.
4. A line in a code block has no more than 100 characters, so it fits the site's content width.
5. Diagrams use Mermaid. A diagram that Mermaid cannot draw (for example, a memory layout) may use ASCII art inside a code block.
6. Internal links are relative `.md` links. The KB does not use wikilinks.
7. Allowed MkDocs Material extensions: admonitions (`!!! note`), collapsible blocks (`??? question`), content tabs (`=== "Java"`), and Mermaid. Pages use collapsible blocks for model answers.

---

## 7. Writing Rules

All KB content follows the writing standard from the user's `~/.claude/CLAUDE.md`: ISO 24495-1 with ASD-STE100 constraints.

- Short, direct sentences, in the active voice.
- One main idea per sentence.
- One term per concept. The term in `glossary.md` is the term every page uses.
- The text names the actor for each action.
- The text states conditions and outcomes explicitly.
- No idioms, metaphors, or colloquial language.
- No vague qualifiers ("quickly", "easily", "normally").
- The text defines each unfamiliar term at first use.
- Facts, assumptions, and recommendations stay separate.

---

## 8. Verification

### 8.1 Java code

- The runner `tools/run-java.sh` extracts every `java` block from the Markdown files. It compiles each block with JDK 21 (`javac --release 21`).
- If a block is not a complete program, the runner wraps it in a class with common imports. If that compile also fails, the block needs the marker `<!-- fragment -->` in the line before it. The reviewer must approve each fragment marker.
- Each LLD `Main.java` must run. Its output must match the expected output in the README.
- Code that uses a preview feature compiles with `--enable-preview`. The page labels the feature as "preview in Java 21".
- Spring and JPA examples compile against Spring Boot dependencies through Gradle, if the network allows the download. If it does not, the page states "checked against the Spring reference documentation, not compiled".
- The runner does not compile existing DSA code with Java 21 flags that break Java 17. Existing DSA blocks compile with `--release 17`.

### 8.2 SQL

- SQL examples run on SQLite 3.51 with the sample data on the page.
- If an example uses a PostgreSQL or MySQL feature, the page names the dialect. The reviewer checks the example against that vendor's documentation.

### 8.3 Facts

- Each important claim has a primary source in **Further Reading**. Primary sources are: the JLS, JDK javadoc and source, JEPs, RFCs, vendor documentation, and original papers (for example Dynamo, Raft, Bigtable, Spanner, and Kafka).
- A number such as a latency or a limit carries the label "approximate" and names its source.
- A claim that says "always" or "never" needs a source, or the reviewer removes it.
- The reviewer checks version-dependent facts on the date of writing. Examples: the current OWASP Top 10 edition, and the preview status of Java features.
- LeetCode problem numbers and difficulty labels come from the LeetCode problem list, as in the existing DSA content.

### 8.4 Mechanical checks

| Script | What it checks |
|---|---|
| `tools/check-links.py` | Every relative link and every `#anchor` resolves |
| `tools/check-format.py` | Code line width (rule 6.4), template section headings for each page type |
| `tools/run-java.sh` | Section 8.1 |
| `tools/run-sql.sh` | Section 8.2 |
| `mkdocs build --strict` | The site builds without warnings |

Every phase ends only when all five checks pass.

### 8.5 Opus review checklist

A Sonnet subagent writes a draft. Opus (the main session) then checks the draft against this list. Opus fixes the draft or returns it to a subagent. A skim does not count as a review.

1. The page has all template sections, or each omission has a reason.
2. Each claim in the Interview Angle section is correct. Opus checks each model answer.
3. Each number and each "always" or "never" claim has a source.
4. Each code block compiles. Runnable code produces the output the text states.
5. The page follows the writing rules (section 7).
6. The page does not repeat content that has its home on another page (section 4). It links to that page.
7. The terms match `glossary.md`.
8. The mechanical checks pass.
9. The depth matches the priority. A P1 page has at least 8 interview questions with model answers. A P2 page has at least 4. A P3 page has at least 2.
10. The page covers every item that section 3 lists for it.

---

## 9. Drafting and Review Process

1. Before a phase starts, Opus writes an implementation plan for that phase.
2. Opus starts Sonnet subagents with the Agent tool (`model: "sonnet"`). Each subagent drafts 1 to 3 files. Up to 5 subagents run at the same time.
3. Each subagent prompt contains:
   - the writing rules (section 7);
   - the template for the page type (section 5);
   - the laptop reading rules (section 6);
   - the verification duties (section 8);
   - the overlap boundaries (section 4) and a list of the sibling files for links.

   Subagents do not see this conversation, so the prompt must contain all of these. Opus keeps these blocks in one shared file, `docs/superpowers/drafting-brief.md`, and pastes that file into each prompt.
4. Opus reviews each draft with the checklist in section 8.5.
5. Opus runs the mechanical checks.
6. Opus commits each subject, or each batch inside a large subject, when its files pass. Commit messages use the repo's `docs:` prefix. They contain no Claude attribution, because the user's `CLAUDE.md` forbids it.
7. At the end of each phase, Opus updates the subject `README.md`, `kb/README.md`, `kb/study-plan.md`, `kb/glossary.md`, and the project memory.

---

## 10. Phases

The order follows interview priority. Each phase has its own implementation plan.

| Phase | Work |
|---|---|
| 0 | Restructure. `git mv` the DSA content into `kb/dsa/`. Fix all links. Add `mkdocs.yml`, `requirements.txt`, and `tools/serve.sh`. Write the checker scripts and the drafting brief. Write `kb/README.md`, the `kb/glossary.md` skeleton, and the `kb/study-plan.md` skeleton. Update the project memory |
| 1 | LLD: procedure, machine-coding guide, fundamentals, 24 case studies, cheatsheet, practice file |
| 2 | HLD: procedure, estimation, fundamentals, distributed systems, 25 case studies, cheatsheet, practice file |
| 3 | DSA completion: foundations, patterns 21 to 28, updates to the routing and summary files |
| 4 | Java in depth, Spring, JPA |
| 5 | Databases and SQL |
| 6 | Operating systems and networking |
| 7 | API design and security |
| 8 | Engineering practice |
| 9 | Behavioral and career |
| 10 | AI engineering |
| 11 | Final integration: complete the study plan and the glossary, check cross-links between subjects, run all checks on the full KB |

---

## 11. Resolved Questions

1. **Reader:** a local MkDocs Material site on the laptop. Nothing goes to an external service.
2. **Topic list:** the user asked for full coverage. Revision 2 adds the topics in its change list.
3. **Spring:** included, and split into six pages (`java/17` to `java/22`).
