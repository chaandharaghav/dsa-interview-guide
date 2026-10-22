# Java in Depth

This subject covers how Java and the JVM work inside, Java concurrency, and the Spring ecosystem. Java backend interviews test these topics.

## How to Study This Subject

1. Read pages 01 to 04 first: the language, generics, exceptions, and the collection internals. Interviewers ask these in the first technical round.
2. Read the concurrency pages in order: 07, 08, 09, 10, then do the problems on page 11 with a timer.
3. Read 05 and 06 (JVM and GC), then 12 and 13 (streams and modern Java).
4. For a Spring backend role, read 16 to 20 in order. Read 21 and 22 if the role uses Spring Security or Spring Cloud.
5. Use the [question bank](interview-question-bank.md) for revision. Each answer links to the page that explains it.

The code on each page targets Java 21. Spring code targets Spring Boot 4.1 and states where Spring Boot 3.5 differs.

## Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

| Page | Content | Priority |
|---|---|---|
| [Java Language Core](01-language-core.md) | Pass-by-value, `equals` and `hashCode`, immutability, the string pool, autoboxing, `final` and `static`, nested classes | P1 |
| [Generics](02-generics.md) | Type erasure, bounded types, wildcards, PECS | P1 |
| [Exceptions](03-exceptions.md) | Checked and unchecked exceptions, try-with-resources, design rules | P1 |
| [Collections Internals](04-collections-internals.md) | How HashMap, ArrayList, LinkedHashMap, TreeMap, PriorityQueue, and ConcurrentHashMap work inside. Fail-fast iterators | P1 |
| [JVM Architecture](05-jvm-architecture.md) | Class loading, runtime data areas, bytecode, the JIT compiler | P1 |
| [Memory and Garbage Collection](06-memory-and-gc.md) | Heap layout, G1, ZGC, GC logs, memory leaks, weak and soft references | P1 |
| [Concurrency Basics: Threads, Locks, and the Java Memory Model](07-concurrency-basics.md) | Threads, `synchronized`, `volatile`, the Java Memory Model, happens-before, deadlock | P1 |
| [java.util.concurrent: Executors, Locks, Atomics, and Synchronizers](08-java-util-concurrent.md) | Executors, locks, atomics, latches, semaphores, barriers, concurrent collections, BlockingQueue | P1 |
| [CompletableFuture: Asynchronous Pipelines](09-completablefuture.md) | Composition, error handling, thread pools | P1 |
| [Virtual Threads, Structured Concurrency, and Scoped Values](10-virtual-threads.md) | Virtual threads (final in Java 21). Structured concurrency and scoped values, labeled as preview features | P1 |
| [Concurrency Coding Problems: Classic Thread-Coordination Exercises](11-concurrency-coding-problems.md) | Classic thread-coordination problems: odd-even printing, bounded blocking queue, reader-writer lock, thread-safe Singleton, rate-limited executor | P1 |
| [Functional Programming and Streams](12-functional-and-streams.md) | Lambdas, functional interfaces, streams, collectors, Optional, parallel stream pitfalls | P1 |
| [Modern Java (9 to 21)](13-modern-java.md) | Features from Java 9 to 21: `var`, records, sealed classes, pattern matching, switch expressions, text blocks | P1 |
| [Reflection and Annotations](14-reflection-and-annotations.md) | Reflection API, annotations and processors, dynamic proxies | P2 |
| [I/O, NIO, and Serialization](15-io-nio-and-serialization.md) | Streams and readers, NIO buffers and channels, serialization risks | P3 |
| [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md) | JDBC, entity states, lazy and eager loading, the N+1 problem, caching levels | P1 |
| [Spring Core: IoC, Beans, and AOP](17-spring-core.md) | IoC, dependency injection, bean scopes and lifecycle, AOP and proxies | P1 |
| [Spring Boot](18-spring-boot.md) | Auto-configuration, starters, profiles and configuration, Actuator | P1 |
| [Spring Data and Transactions](19-spring-data-and-transactions.md) | Repositories, `@Transactional` propagation and isolation, self-invocation pitfall | P1 |
| [Spring Web and REST](20-spring-web-and-rest.md) | DispatcherServlet flow, controllers, validation, exception handling | P1 |
| [Spring Security](21-spring-security.md) | Filter chain, authentication, authorization, JWT resource server | P2 |
| [Microservices with Spring](22-microservices-with-spring.md) | Service discovery, config server, resilience, distributed tracing | P2 |
| [Build Tools: Maven and Gradle](23-build-tools.md) | Maven and Gradle: lifecycle, dependency resolution, conflicts | P2 |
| [Interview Question Bank](interview-question-bank.md) | Short questions with model answers. Each answer links to the page that explains it | P1 |
