# Cross-Subject Study Plan

This page puts all subjects on one calendar. Each phase of the KB adds its subject to this plan.

!!! info "Status"
    The DSA, LLD, HLD, and Java tracks are complete. The other tracks are planned. The table below names the phase that writes each track.

## Tracks

| Track | Plan | Status |
|---|---|---|
| DSA | [DSA plan](dsa/study-plan.md): 12 core weeks, week 13 for the new P1 patterns, weeks 14 and 15 as stretch | Complete |
| LLD | [LLD track](#lld-track) below | Complete |
| HLD | [HLD track](#hld-track) below | Complete |
| Java in depth | [Java track](#java-track) below | Complete |
| Fundamentals (databases, networking, operating systems) | P1 pages in order | Phases 5 and 6 |
| Behavioral | Story bank, then mock rounds | Phase 9 |

## LLD Track

The LLD track takes 8 weeks at about 5 hours each week. Start it after Week 0 of the DSA plan. Each case study takes two sessions: one session to design it yourself in 45 minutes, and one session to compare your design with the page and run its `Main.java`.

| Week | Read | Do |
|---|---|---|
| 1 | [How to Approach an LLD Interview](lld/how-to-approach-lld.md), [OOP](lld/fundamentals/01-oop.md), [SOLID](lld/fundamentals/02-solid.md) | Case study 09 (tic-tac-toe) |
| 2 | [UML Diagrams](lld/fundamentals/04-uml-class-and-sequence-diagrams.md), [Domain Modeling](lld/fundamentals/05-domain-modeling.md), [Creational Patterns](lld/fundamentals/06-creational-patterns.md) | Case studies 01 (parking lot) and 02 (LRU and LFU cache) |
| 3 | [Structural Patterns](lld/fundamentals/07-structural-patterns.md), [Behavioral Patterns](lld/fundamentals/08-behavioral-patterns.md) | Case studies 07 (vending machine) and 08 (logging framework) |
| 4 | [Concurrency in LLD](lld/fundamentals/09-concurrency-in-lld.md) | Case studies 03 (rate limiter) and 05 (movie ticket booking) |
| 5 | [Architectural Patterns](lld/fundamentals/10-architectural-patterns.md), [The Machine-Coding Round](lld/machine-coding-round.md) | Case study 06 (Splitwise) as a 90-minute machine-coding build |
| 6 | [Design Principles](lld/fundamentals/03-design-principles.md), [Anti-Patterns](lld/fundamentals/11-anti-patterns.md) | Case studies 04 (elevator), 15 (pub-sub), and 16 (task scheduler) |
| 7 | The P2 case studies that match your target companies | Case study 24 (notification service), two P2 case studies, mock sets A and B from the [practice page](lld/practice-and-mock-interviews.md) |
| 8 | [Cheatsheet](lld/cheatsheet.md) | Mock sets C to G, scored with the rubric |

## Java Track

The Java track takes 7 weeks at about 4 hours each week. It runs in the fundamentals slot of the calendar, in weeks 2 to 8. For each page, answer three of its `??? question` blocks before you read the model answers, and run its Java examples.

| Week | Read | Do |
|---|---|---|
| 1 | [Java Language Core](java/01-language-core.md), [Generics](java/02-generics.md), [Exceptions](java/03-exceptions.md) | Run the programs on each page and predict their output first |
| 2 | [Collections Internals](java/04-collections-internals.md) | Explain `HashMap` put, resize, and treeification aloud in 3 minutes |
| 3 | [Concurrency Basics](java/07-concurrency-basics.md), [java.util.concurrent](java/08-java-util-concurrent.md) | Read together with week 4 of the LLD track (concurrency in LLD) |
| 4 | [CompletableFuture](java/09-completablefuture.md), [Virtual Threads](java/10-virtual-threads.md) | Problems 1 to 4 on [Concurrency Coding Problems](java/11-concurrency-coding-problems.md), 30 minutes each |
| 5 | [JVM Architecture](java/05-jvm-architecture.md), [Memory and Garbage Collection](java/06-memory-and-gc.md) | Problems 5 to 9 on the concurrency coding page |
| 6 | [Functional Programming and Streams](java/12-functional-and-streams.md), [Modern Java](java/13-modern-java.md), [JDBC, JPA, and Hibernate](java/16-jdbc-jpa-and-hibernate.md) | Rewrite one loop-heavy method from your own code with streams, then compare readability |
| 7 | [Spring Core](java/17-spring-core.md), [Spring Boot](java/18-spring-boot.md), [Spring Data and Transactions](java/19-spring-data-and-transactions.md), [Spring Web and REST](java/20-spring-web-and-rest.md) | The [question bank](java/interview-question-bank.md), one section each day |

P2 and P3 pages (reflection, I/O, Spring Security, microservices with Spring, build tools) are stretch reading. Read them when a role asks for them.

## HLD Track

The HLD track takes 10 weeks at about 5 hours each week. Start it after week 6 of the LLD track, when you have read the LLD fundamentals. Each case study takes one session: design it yourself in 45 minutes, then compare your design with the page and answer its follow-up questions.

| Week | Read | Do |
|---|---|---|
| 1 | [How to Approach an HLD Interview](hld/how-to-approach-hld.md), [Back-of-Envelope Estimation](hld/back-of-envelope-estimation.md), [Scalability and Availability](hld/fundamentals/01-scalability-and-availability.md) | The estimation examples on paper |
| 2 | [Load Balancing, DNS, and CDN](hld/fundamentals/02-load-balancing-dns-cdn.md), [Caching](hld/fundamentals/03-caching.md), [Choosing a Database](hld/fundamentals/04-choosing-a-database.md) | Case study 01 (URL shortener) |
| 3 | [Replication](hld/fundamentals/05-replication.md), [Partitioning and Sharding](hld/fundamentals/06-partitioning-and-sharding.md), [Consistency and CAP](hld/fundamentals/07-consistency-and-cap.md) | Case study 03 (key-value store) |
| 4 | [Message Queues and Streams](hld/fundamentals/08-message-queues-and-streams.md), [Communication Patterns](hld/fundamentals/09-communication-patterns.md), [Reliability Patterns](hld/fundamentals/12-reliability-patterns.md) | Case studies 04 (notification system) and 06 (chat system) |
| 5 | [Rate Limiting Algorithms](hld/fundamentals/13-rate-limiting-algorithms.md), [Microservices and Monoliths](hld/fundamentals/14-microservices-and-monoliths.md), [Unique ID Generation](hld/fundamentals/15-unique-id-generation.md), [Event-Driven Architecture](hld/fundamentals/16-event-driven-architecture.md) | Case studies 02 (rate limiter) and 05 (news feed) |
| 6 | [Consensus](hld/distributed-systems/02-consensus-and-leader-election.md), [Distributed Transactions](hld/distributed-systems/03-distributed-transactions.md), [Probabilistic Data Structures](hld/distributed-systems/04-probabilistic-data-structures.md) | Case studies 13 (payment system) and 16 (leaderboard) |
| 7 | [Storage Types](hld/fundamentals/10-storage-types.md), [Search and Indexing](hld/fundamentals/11-search-and-indexing.md) | Case studies 07 (autocomplete), 09 (video streaming), and 10 (file sync) |
| 8 | [Geospatial Indexing](hld/fundamentals/18-geospatial-indexing.md), [Failures and Time](hld/distributed-systems/01-failures-and-time.md), [Distributed Locks and Leases](hld/distributed-systems/05-distributed-locks-and-leases.md) | Case studies 08 (web crawler), 11 (ride-hailing), and 12 (ticket booking) |
| 9 | P2 stretch: fundamentals 17 to 21, [CRDTs](hld/distributed-systems/06-crdts-and-conflict-resolution.md) | Two P2 case studies that match your target companies, mock sets A and B from the [practice page](hld/practice-and-mock-interviews.md) |
| 10 | [Cheatsheet](hld/cheatsheet.md) | Mock sets C to G, scored with the rubric |

## How the Tracks Fit Together

The calendar below combines the tracks into 21 weeks (week 0 to week 20). It assumes about 20 hours each week: 2.5 hours on weekdays and 4 hours on each weekend day. DSA takes about 10 hours, the design track (LLD, then HLD) about 5 hours, the fundamentals track about 4 hours, and review about 1 hour. The [interactive timeline](prep-timeline.html) shows the same calendar as a chart.

| Weeks | DSA | Design | Fundamentals and other |
|---|---|---|---|
| 0 | DSA plan week 0 and the [foundations](dsa/README.md#foundations) pages | — | — |
| 1–8 | DSA plan weeks 1 to 8 | [LLD track](#lld-track) weeks 1 to 8 | [Java track](#java-track) weeks 1 to 7, from week 2 |
| 9–12 | DSA plan weeks 9 to 12 | [HLD track](#hld-track) weeks 1 to 4 | Databases P1 pages (planned: phase 5) |
| 13–15 | Patterns 23, 25, 26 (P1), then 21, 22, 24, 27, 28 (P2 stretch) | HLD track weeks 5 to 7 | Operating systems and networking P1 pages (planned: phase 6) |
| 16–17 | Two mixed problems each day | HLD track weeks 8 and 9 | API design and security P1 pages (planned: phase 7). Resume in week 17 (planned: phase 9) |
| 18–20 | Two mixed problems each day | HLD track week 10, then full mock loops | JavaScript basics in weeks 18 and 19 (outside the KB), behavioral story bank in weeks 19 and 20 |

To shorten the plan, skip the P2 stretch work. This saves about 3 weeks. Until a planned subject has pages, use the outside sources that the [interactive timeline](prep-timeline.html) names.
