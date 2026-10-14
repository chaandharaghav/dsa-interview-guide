# High-Level Design

High-level design (HLD) is the design of a distributed system: its services, data stores, and the communication between them. System design interviews test it.

!!! info "Status"
    Planned for phase 2. The pages below do not exist yet. Phase 2 writes them, and then this page links to each one.

## Planned Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

### Root files

| File | Content | Priority |
|---|---|---|
| `how-to-approach-hld.md` | The HLD interview procedure: requirements, estimation, API, data model, diagram, deep dives, trade-offs. Time budget | P1 |
| `back-of-envelope-estimation.md` | Powers of 2, latency numbers, QPS, storage, bandwidth. Worked examples | P1 |
| `cheatsheet.md` | One page: building blocks, which tool solves which problem, key numbers | P1 |
| `practice-and-mock-interviews.md` | Extra HLD prompts without solutions, timed mock sets, self-evaluation rubric | P1 |

### `fundamentals/`

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

### `distributed-systems/`

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
