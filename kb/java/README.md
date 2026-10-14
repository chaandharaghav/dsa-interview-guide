# Java in Depth

This subject covers how Java and the JVM work inside, Java concurrency, and the Spring ecosystem. Java backend interviews test these topics.

!!! info "Status"
    Planned for phase 4. The pages below do not exist yet. Phase 4 writes them, and then this page links to each one.

## Planned Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

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
