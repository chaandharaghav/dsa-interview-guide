---
priority: P1
---
# HLD Cheatsheet

This page summarizes the HLD subject for revision before an interview. Each section links to the page that explains it. Do not learn the subject from this page: it has no reasons and no examples.

## The Procedure

Source: [How to Approach an HLD Interview](how-to-approach-hld.md). Times are for a 45-minute round. They are a recommendation of this KB.

| Step | Minutes | Output |
|---|---|---|
| 1. Clarify | 5 | Numbered functional and non-functional requirements, and assumptions |
| 2. Estimate | 3 | Peak QPS, storage, bandwidth, cache size, and the decision each number drives |
| 3. API | 3 | 3 to 6 operations with inputs, outputs, errors, idempotency, pagination |
| 4. Data model | 3 | Access patterns, keys, the database choice with a reason, blobs in an object store |
| 5. High-level design | 7 | 6 to 10 components and one request walked through the diagram |
| 6. Deep dives | 13 | Two to four components: simple design, where it fails, improvement, cost, failure behavior |
| 7. Bottlenecks and 8. Trade-offs | 4 | What fails at 10 times the load, what fails when a part fails, each decision with its alternative |
| 9. Summary | 2 | The design in one minute, what is not covered, next steps |

Say out loud: the step, the number, the reason, the rejected alternative, and the failure behavior.

## Key Numbers

Sources: [Back-of-the-Envelope Estimation](back-of-envelope-estimation.md) and [Scalability and Availability](fundamentals/01-scalability-and-availability.md). All latency numbers are approximate (Dean, LADIS 2009).

| Quantity | Value |
|---|---|
| Seconds in a day | 86,400, rounded to 10^5 |
| Seconds in a year | 3.15 × 10^7 |
| 2^10, 2^20, 2^30, 2^40 | About 10^3, 10^6, 10^9, 10^12 |
| Memory reference | 100 ns |
| SSD random read | About 16 µs |
| Round trip in one data center | 0.5 ms |
| Disk seek | 10 ms |
| Round trip across a continent | 150 ms |
| 99.9% availability | 8.76 hours of downtime a year, 43.2 minutes in 30 days |
| 99.99% availability | 52.56 minutes of downtime a year |

| Formula | Expression |
|---|---|
| Average QPS | Users × actions per user per day / 10^5 |
| Peak QPS | Average × 2 or 3 |
| Storage | Records × bytes × days kept × replication factor |
| Bandwidth | QPS × bytes × 8 bits per second |
| Cache | Distinct objects read per day × 0.2 × bytes (rule of thumb) |
| Requests in flight (Little's law) | QPS × latency in seconds |
| Series availability | A1 × A2 × ... × An |
| Parallel availability | 1 − (1 − A1)(1 − A2)...(1 − An) |
| Slow fan-out | 1 − (1 − p)^n. For p = 1% and n = 100, 63% of requests are slow |

## Building Blocks: Which Tool Solves Which Problem

| Problem | Tool | Page |
|---|---|---|
| One server cannot take the load | Stateless service, horizontal scaling behind a load balancer | [Scalability](fundamentals/01-scalability-and-availability.md) |
| Spread traffic, route by path | L7 load balancer. L4 for a static IP and the highest rate | [Load balancing, DNS, CDN](fundamentals/02-load-balancing-dns-cdn.md) |
| Users far from the servers, static content | CDN, versioned URLs, origin shield | [Load balancing, DNS, CDN](fundamentals/02-load-balancing-dns-cdn.md) |
| Reads too slow or too many | Cache-aside with a TTL, update the database then delete the key | [Caching](fundamentals/03-caching.md) |
| Pick a store | Access pattern first. Transactions or constraints: start relational | [Choosing a database](fundamentals/04-choosing-a-database.md) |
| Survive a node failure, scale reads | Replication: single leader by default | [Replication](fundamentals/05-replication.md) |
| Data or writes outgrow one machine | Partitioning by a key from the top query, consistent hashing | [Partitioning](fundamentals/06-partitioning-and-sharding.md) |
| Decide the consistency per operation | Linearizable for money, locks, uniqueness. Eventual for feeds, counters | [Consistency and CAP](fundamentals/07-consistency-and-cap.md) |
| Decouple work, level load | Queue for tasks, log (Kafka) for fan-out and replay | [Queues and streams](fundamentals/08-message-queues-and-streams.md) |
| Push updates to clients | SSE one-way, WebSocket two-way, webhook to a partner server | [Communication patterns](fundamentals/09-communication-patterns.md) |
| Store files, images, video | Object storage, presigned upload URL, metadata in a database | [Storage types](fundamentals/10-storage-types.md) |
| Free-text search, ranking | Inverted index (Elasticsearch, OpenSearch) fed by CDC | [Search and indexing](fundamentals/11-search-and-indexing.md) |
| A dependency fails or slows | Timeouts, retries with jitter, circuit breaker, bulkhead, load shedding | [Reliability patterns](fundamentals/12-reliability-patterns.md) |
| Abuse or overload by one client | Token bucket at the gateway, shared state in Redis with Lua | [Rate limiting](fundamentals/13-rate-limiting-algorithms.md) |
| Teams and services | Modular monolith first. Split on a trigger. API gateway, discovery | [Microservices](fundamentals/14-microservices-and-monoliths.md) |
| IDs across many servers | Snowflake (64-bit, time-ordered) or UUID version 7 | [Unique IDs](fundamentals/15-unique-id-generation.md) |
| React to changes across services | Events, outbox, CDC, CQRS only when a read model needs it | [Event-driven architecture](fundamentals/16-event-driven-architecture.md) |
| Aggregate large or unbounded data | Batch (Spark) or stream (Flink, Kafka Streams) with event-time windows | [Batch and stream](fundamentals/17-batch-and-stream-processing.md) |
| Find nearby objects | Geohash or S2 cells, query the cell and its neighbors | [Geospatial indexing](fundamentals/18-geospatial-indexing.md) |
| Survive a region failure | Active-passive or active-active, RPO and RTO targets | [Multi-region and DR](fundamentals/19-multi-region-and-disaster-recovery.md) |
| Know when the system is unhealthy | Golden signals, RED and USE, SLO burn-rate alerts | [Observability in design](fundamentals/20-observability-in-design.md) |
| Protect the system | TLS, gateway authentication, mTLS inside, KMS envelope encryption | [Security in design](fundamentals/21-security-in-design.md) |
| Agree on a leader or a value | Raft (etcd) or ZooKeeper, 3 or 5 nodes | [Consensus](distributed-systems/02-consensus-and-leader-election.md) |
| A change across two services | Saga with compensations, outbox, idempotent consumers | [Distributed transactions](distributed-systems/03-distributed-transactions.md) |
| Membership test, counts, distinct counts at scale | Bloom filter, count-min sketch, HyperLogLog | [Probabilistic structures](distributed-systems/04-probabilistic-data-structures.md) |
| Mutual exclusion across machines | Lease plus fencing token, or no lock (compare-and-set) | [Locks and leases](distributed-systems/05-distributed-locks-and-leases.md) |
| Concurrent writes in many replicas | Version vectors, CRDTs, or one home region | [CRDTs](distributed-systems/06-crdts-and-conflict-resolution.md) |

## Trade-off Pairs

| Pair | Choose the first when | Choose the second when |
|---|---|---|
| SQL or NoSQL | Transactions, constraints, ad hoc queries | One dominant key access pattern at a scale one primary cannot serve |
| Strong or eventual consistency | An invariant must hold (balance, seat, uniqueness) | Stale reads for seconds are acceptable (feed, likes) |
| Push (fan-out on write) or pull (fan-out on read) | Readers far outnumber writers and follower counts are bounded | A writer has millions of followers (celebrity) |
| Synchronous or asynchronous call | The caller needs the result to answer | The work can finish later, or many consumers react |
| Queue or log | Tasks with per-message retry and delay | Fan-out to many consumers, replay, high volume |
| Cache-aside or write-through | Read-heavy data, tolerance for a short staleness window | The cache must equal the database after each write |
| Hash or range partitioning | Even spread, lookups by key | Range scans by the key (time, name) |
| Leader-follower or leaderless | Simple reasoning, read-your-writes from the leader | Writes must continue when any node fails |
| Monolith or microservices | One team, an early product | Independent scaling, release, or ownership is required |
| LWW or CRDT or single writer | Loss of a concurrent write is acceptable | Merge must keep every update (CRDT), or the invariant needs one writer |
| Polling, SSE, or WebSocket | Rare updates (polling) | Frequent one-way (SSE) or two-way (WebSocket) updates |
| Exactly-once claim or idempotency | Kafka-to-Kafka processing inside one transaction | Any external effect: use at-least-once delivery plus an idempotent effect |

## Distributed-Systems Facts to Get Right

Sources: the [distributed-systems](distributed-systems/01-failures-and-time.md) pages and [Consistency and CAP](fundamentals/07-consistency-and-cap.md).

- CAP applies during a partition. Answer "CP or AP" per operation, not per system.
- A quorum with R + W > N makes the read and write sets overlap. It does not alone give linearizability.
- A majority is ⌊n/2⌋ + 1. Five nodes tolerate two failures. An even node count adds nothing.
- A timeout means "unknown", not "failed". Retry with the same idempotency key, then query the status.
- A lease without a fencing token does not give mutual exclusion when the holder pauses.
- Use the wall clock for a point in time and the monotonic clock for a duration.
- 2PC blocks when the coordinator fails after the votes. Sagas give no isolation.
- Replication is not a backup.

## Red Flags in an HLD Round

- A diagram before the requirements and the estimate.
- A product name with no reason ("use Kafka").
- A large distributed design for a load that one database serves.
- No answer to "what happens when this component fails?"
- One deep dive for the whole round, or no deep dive.
- Averages instead of percentiles for latency.
- "Exactly-once" with no scope.
- Silence while you think.

## Case-Study Index by Main Building Block

| Main building block | Case studies |
|---|---|
| Caching and read scaling | [URL shortener](case-studies/01-url-shortener.md), [distributed cache](case-studies/15-distributed-cache.md), [photo sharing](case-studies/18-photo-sharing.md) |
| Partitioning, replication, quorums | [Key-value store](case-studies/03-key-value-store.md), [distributed message queue](case-studies/14-distributed-message-queue.md) |
| Fan-out and queues | [Notification system](case-studies/04-notification-system.md), [news feed](case-studies/05-news-feed.md), [email service](case-studies/23-email-service.md) |
| Real-time connections | [Chat system](case-studies/06-chat-system.md), [collaborative editor](case-studies/19-collaborative-editor.md) |
| Search and tries | [Search autocomplete](case-studies/07-search-autocomplete.md), [web crawler](case-studies/08-web-crawler.md) |
| Blob storage and CDN | [Video streaming](case-studies/09-video-streaming.md), [file storage and sync](case-studies/10-file-storage-and-sync.md) |
| Geospatial | [Ride-hailing and proximity](case-studies/11-ride-hailing-and-proximity.md) |
| Contention and correctness | [Ticket booking](case-studies/12-ticket-booking.md), [payment system](case-studies/13-payment-system.md), [e-commerce flash sale](case-studies/24-ecommerce-flash-sale.md), [stock exchange](case-studies/22-stock-exchange.md) |
| Counting and aggregation | [Distributed rate limiter](case-studies/02-distributed-rate-limiter.md), [leaderboard and top-k](case-studies/16-leaderboard-and-top-k.md), [ad click aggregation](case-studies/21-ad-click-aggregation.md), [metrics and monitoring](case-studies/17-metrics-and-monitoring.md) |
| Scheduling and isolation | [Distributed job scheduler](case-studies/20-distributed-job-scheduler.md), [online code judge](case-studies/25-online-code-judge.md) |
