# High-Level Design

High-level design (HLD) is the design of a distributed system: its services, data stores, and the communication between them. System design interviews test it.

## How to Study This Subject

1. Read [How to Approach an HLD Interview](how-to-approach-hld.md) and [Back-of-Envelope Estimation](back-of-envelope-estimation.md). Every case study follows this procedure.
2. Read the P1 fundamentals in order (01 to 09, then 12 to 16). Read the P2 fundamentals when a case study links to them.
3. Read distributed-systems pages 02, 03, and 04 (P1). Read 01, 05, and 06 before the case studies that link to them.
4. Do the P1 case studies. For each one: read the problem statement only, design it yourself for 45 minutes, and then compare your design with the page.
5. Use the [cheatsheet](cheatsheet.md) on the day before an interview, and the [practice page](practice-and-mock-interviews.md) for timed mock rounds.

The [cross-subject study plan](../study-plan.md) puts these steps on a calendar.

## Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

### Root files

| Page | Content | Priority |
|---|---|---|
| [How to Approach an HLD Interview](how-to-approach-hld.md) | The HLD interview procedure: requirements, estimation, API, data model, diagram, deep dives, trade-offs. Time budget | P1 |
| [Back-of-Envelope Estimation](back-of-envelope-estimation.md) | Powers of 2, latency numbers, QPS, storage, bandwidth. Worked examples | P1 |
| [HLD Cheatsheet](cheatsheet.md) | One page: building blocks, which tool solves which problem, key numbers | P1 |
| [HLD Practice and Mock Interviews](practice-and-mock-interviews.md) | Extra HLD prompts without solutions, timed mock sets, self-evaluation rubric | P1 |

### `fundamentals/`

| Page | Content | Priority |
|---|---|---|
| [Scalability and Availability](fundamentals/01-scalability-and-availability.md) | Vertical and horizontal scaling, stateless services, latency and throughput, availability numbers, SLA, SLO, SLI | P1 |
| [Load Balancing, DNS, and CDN](fundamentals/02-load-balancing-dns-cdn.md) | L4 and L7 load balancers, algorithms, health checks. DNS in system design. CDN push and pull | P1 |
| [Caching](fundamentals/03-caching.md) | Cache-aside, read-through, write-through, write-back, write-around. Eviction, invalidation, stampede, hot keys | P1 |
| [Choosing a Database](fundamentals/04-choosing-a-database.md) | SQL or NoSQL for a given access pattern. Links to `databases/` for internals | P1 |
| [Replication](fundamentals/05-replication.md) | Leader-follower, multi-leader, leaderless. Replication lag and its effects | P1 |
| [Partitioning and Sharding](fundamentals/06-partitioning-and-sharding.md) | Hash and range partitioning, consistent hashing, rebalancing, hot spots | P1 |
| [Consistency and the CAP Theorem](fundamentals/07-consistency-and-cap.md) | CAP, PACELC, consistency models, quorums | P1 |
| [Message Queues and Streams](fundamentals/08-message-queues-and-streams.md) | Kafka, RabbitMQ, SQS. Delivery semantics, ordering, backpressure, dead-letter queues | P1 |
| [Communication Patterns](fundamentals/09-communication-patterns.md) | REST, gRPC, WebSocket, long polling, SSE, webhooks: which to use when | P1 |
| [Storage Types](fundamentals/10-storage-types.md) | Block, file, and object storage. Blob stores, data lakes | P2 |
| [Search and Indexing](fundamentals/11-search-and-indexing.md) | Inverted index, Elasticsearch architecture, ranking basics | P2 |
| [Reliability Patterns](fundamentals/12-reliability-patterns.md) | Timeouts, retries, backoff, jitter, circuit breaker, bulkhead, idempotency, graceful degradation | P1 |
| [Rate Limiting Algorithms](fundamentals/13-rate-limiting-algorithms.md) | Token bucket, leaky bucket, fixed window, sliding log, sliding window counter | P1 |
| [Microservices and Monoliths](fundamentals/14-microservices-and-monoliths.md) | Service boundaries, API gateway, service discovery, service mesh | P1 |
| [Unique ID Generation](fundamentals/15-unique-id-generation.md) | UUID versions, Snowflake, ticket servers | P1 |
| [Event-Driven Architecture](fundamentals/16-event-driven-architecture.md) | Events and commands, event sourcing, CQRS, choreography and orchestration | P1 |
| [Batch and Stream Processing](fundamentals/17-batch-and-stream-processing.md) | MapReduce, Spark, Flink, Lambda and Kappa architectures | P2 |
| [Geospatial Indexing](fundamentals/18-geospatial-indexing.md) | Geohash, quadtree, S2 | P2 |
| [Multi-Region Design and Disaster Recovery](fundamentals/19-multi-region-and-disaster-recovery.md) | Active-active and active-passive, RPO and RTO, failover, data residency | P2 |
| [Observability in System Design](fundamentals/20-observability-in-design.md) | What to measure in a design. Links to `engineering-practice/` | P2 |
| [Security in System Design](fundamentals/21-security-in-design.md) | Authentication at the gateway, encryption in transit and at rest. Links to `security/` | P2 |

### `distributed-systems/`

| Page | Content | Priority |
|---|---|---|
| [Failures and Time in Distributed Systems](distributed-systems/01-failures-and-time.md) | Partial failure, network partitions, clock drift, Lamport clocks, vector clocks | P2 |
| [Consensus and Leader Election](distributed-systems/02-consensus-and-leader-election.md) | Raft in detail, Paxos in brief, ZooKeeper and etcd | P1 |
| [Distributed Transactions](distributed-systems/03-distributed-transactions.md) | 2PC, saga, transactional outbox, exactly-once claims | P1 |
| [Probabilistic Data Structures and Gossip](distributed-systems/04-probabilistic-data-structures.md) | Bloom filter, count-min sketch, HyperLogLog, Merkle tree, gossip | P1 |
| [Distributed Locks and Leases](distributed-systems/05-distributed-locks-and-leases.md) | Lease-based locks, fencing tokens, the Redlock debate | P2 |
| [CRDTs and Conflict Resolution](distributed-systems/06-crdts-and-conflict-resolution.md) | Last-writer-wins, version vectors, CRDTs, operational transformation | P2 |

`case-studies/`: each case study is one Markdown file.

| Page | Priority |
|---|---|
| [URL Shortener](case-studies/01-url-shortener.md) | P1 |
| [Distributed Rate Limiter](case-studies/02-distributed-rate-limiter.md) | P1 |
| [Distributed Key-Value Store (Dynamo Style)](case-studies/03-key-value-store.md) (Dynamo-style) | P1 |
| [Notification System](case-studies/04-notification-system.md) | P1 |
| [News Feed](case-studies/05-news-feed.md) | P1 |
| [Chat System](case-studies/06-chat-system.md) | P1 |
| [Search Autocomplete System](case-studies/07-search-autocomplete.md) | P1 |
| [Web Crawler](case-studies/08-web-crawler.md) | P1 |
| [Video Streaming Service](case-studies/09-video-streaming.md) | P1 |
| [File Storage and Sync Service](case-studies/10-file-storage-and-sync.md) (Dropbox-style) | P1 |
| [Ride-Hailing Service and Proximity Search](case-studies/11-ride-hailing-and-proximity.md) | P1 |
| [Ticket Booking System](case-studies/12-ticket-booking.md) | P1 |
| [Payment System](case-studies/13-payment-system.md) | P1 |
| [Distributed Message Queue (Kafka Style)](case-studies/14-distributed-message-queue.md) | P2 |
| [Distributed Cache](case-studies/15-distributed-cache.md) | P2 |
| [Leaderboard and Top-K System](case-studies/16-leaderboard-and-top-k.md) | P1 |
| [Metrics and Monitoring System](case-studies/17-metrics-and-monitoring.md) | P2 |
| [Photo Sharing Service](case-studies/18-photo-sharing.md) | P2 |
| [Collaborative Editor](case-studies/19-collaborative-editor.md) | P2 |
| [Distributed Job Scheduler](case-studies/20-distributed-job-scheduler.md) | P2 |
| [Ad Click Aggregation System](case-studies/21-ad-click-aggregation.md) | P2 |
| [Stock Exchange](case-studies/22-stock-exchange.md) | P2 |
| [Email Service](case-studies/23-email-service.md) | P2 |
| [E-commerce Flash Sale](case-studies/24-ecommerce-flash-sale.md) | P2 |
| [Online Code Judge](case-studies/25-online-code-judge.md) | P2 |
