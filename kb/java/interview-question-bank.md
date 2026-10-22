---
priority: P1
---
# Java Interview Question Bank

This page collects the questions that interviewers ask most often about the Java pages of this knowledge base. The questions follow the page order, 01 to 23. Use the page in two steps. First, read the question and answer it aloud, as you would in the interview, and say the reason as well as the result. Second, open the block and compare your answer with the model answer. Each model answer is short. It gives the direct answer, then the reason or the key detail, and it ends with a link to the section that explains the topic in depth. If your answer missed a point, read that section before you move on. Where an answer depends on a version (Java 21 versus Java 24 or 25, Spring Boot 3.5 versus Spring Boot 4.1, Spring Security 6 versus Spring Security 7), the answer names the version.

## Contents

- [Java Language Core](#java-language-core)
- [Generics](#generics)
- [Exceptions](#exceptions)
- [Collections Internals](#collections-internals)
- [JVM Architecture](#jvm-architecture)
- [Memory and Garbage Collection](#memory-and-garbage-collection)
- [Concurrency Basics](#concurrency-basics)
- [java.util.concurrent](#javautilconcurrent)
- [CompletableFuture](#completablefuture)
- [Virtual Threads](#virtual-threads)
- [Concurrency Coding Problems](#concurrency-coding-problems)
- [Functional Programming and Streams](#functional-programming-and-streams)
- [Modern Java (9 to 21)](#modern-java-9-to-21)
- [Reflection and Annotations](#reflection-and-annotations)
- [I/O, NIO, and Serialization](#io-nio-and-serialization)
- [JDBC, JPA, and Hibernate](#jdbc-jpa-and-hibernate)
- [Spring Core](#spring-core)
- [Spring Boot](#spring-boot)
- [Spring Data and Transactions](#spring-data-and-transactions)
- [Spring Web and REST](#spring-web-and-rest)
- [Spring Security](#spring-security)
- [Microservices with Spring](#microservices-with-spring)
- [Build Tools](#build-tools)

## Java Language Core

??? question "Is Java pass-by-value or pass-by-reference?"

    Java is always pass-by-value (JLS 8.4.1). The method receives a copy of each argument value. For an object, the copy is the reference, so the callee can change the object and the caller sees the change, but the callee cannot make the caller's variable point to another object. Proof: a `swap(a, b)` method cannot swap the caller's variables, for primitives or for references. Details: [Java Language Core](01-language-core.md#pass-by-value-for-primitives-and-references).

??? question "What is the contract between `equals` and `hashCode`, and what breaks if you override only `equals`?"

    If `a.equals(b)` is true, `a.hashCode() == b.hashCode()` must be true, and the hash code must stay the same while the fields that `equals` uses do not change. If you override only `equals`, the class keeps the identity-based `hashCode` of `Object`, so two equal objects have different hash codes with near certainty. `HashMap` and `HashSet` find the bucket from the hash code, so `get` and `contains` miss the entry that `put` stored. Override both from the same fields, and annotate `equals(Object)` with `@Override` so that the compiler catches an `equals(Point)` overload. Details: [Java Language Core](01-language-core.md#the-equals-contract-and-the-bugs-from-breaking-it).

??? question "Why is `new String(\"a\") == \"a\"` false, and what does `intern` do?"

    The JVM interns every string literal, so all literals with the same characters share one object (JLS 3.10.5). `new String("a")` always creates a new heap object, so `==` compares two different references. `s.intern()` returns the pooled instance with the same characters, and adds `s` to the pool if the pool has none, so `new String("a").intern() == "a"` is true. Compare strings with `equals`, never with `==`. Details: [Java Language Core](01-language-core.md#string-pool-intern-and-compile-time-constants).

??? question "Why is `String` immutable?"

    Each reason depends on characters that cannot change. The string pool can give one instance to every literal with the same characters. A `String` key in a `HashMap` keeps its hash after the insert, and `String` computes the hash once and caches it in a field. Threads share a string without locks, and a file path or class name cannot change between a security check and its use. The cost is a new object for each change, so use `StringBuilder` for repeated changes. Details: [Java Language Core](01-language-core.md#follow-up-questions) and [Immutability](01-language-core.md#immutability).

??? question "Why is `Integer a = 127, b = 127; a == b` true, and the same code with 128 false?"

    Autoboxing calls `Integer.valueOf`, and its javadoc says that it always caches the values -128 to 127 (JLS 5.1.7 gives the same guarantee for constant expressions). So both variables refer to one cached object, and the result for 127 is guaranteed. Two boxings of 128 allocate two objects on a default HotSpot JVM, so `==` is false. The 128 result is not guaranteed, because the `-XX:AutoBoxCacheMax` flag raises the upper bound of the cache, so compare boxed values with `equals`. Details: [Java Language Core](01-language-core.md#autoboxing-and-the-integer-cache).

## Generics

??? question "What is type erasure, and what does the compiler do?"

    Erasure removes type parameters and type arguments from the bytecode (JLS 4.6). The compiler checks the program with the full generic types, replaces each type variable with its leftmost bound (`Object` if there is none), inserts casts, and adds bridge methods where erasure changes an overriding signature. The reason is compatibility with code compiled before Java 5 (JLS 4.7). The cost is that the JVM cannot tell `List<String>` from `List<Integer>` at run time, so `new T[n]` and `o instanceof List<String>` on an `Object` do not compile. Details: [Generics](02-generics.md#type-erasure).

??? question "Explain PECS with an example."

    Producer extends, consumer super. A parameter that produces values for the method uses `? extends T`. A parameter that consumes values uses `? super T`. `Collections.copy(List<? super T> dest, List<? extends T> src)` is the standard example: the source produces `T` values and the destination consumes them. With `? extends T` the compiler allows reads and blocks writes (except `null`). With `? super T` it allows writes of `T`, and reads return only `Object`. Details: [Generics](02-generics.md#wildcards-and-pecs).

??? question "Why is `List<Integer>` not a `List<Number>`?"

    Generic types are invariant. If `List<Integer>` were a `List<Number>`, code could add a `Double` through the `List<Number>` view, and the original list would hold a `Double`. Arrays are covariant, and they catch the wrong store at run time with `ArrayStoreException` (JLS 10.5). To read from a list of any subtype, use `List<? extends Number>`. Details: [Generics](02-generics.md#invariance-and-covariant-arrays).

## Exceptions

??? question "What is the difference between checked and unchecked exceptions? Which do you prefer?"

    JLS 11.1.1: `RuntimeException`, `Error`, and their subclasses are unchecked, and every other exception class (including `Throwable` and `Exception`) is checked. The compiler forces the caller to catch or declare a checked exception (JLS 11.2). Checked exceptions document a failure that the caller can recover from. They also force every caller to handle them, leak implementation types into signatures, and do not work with the `java.util.function` interfaces and streams. Recommendation: use a checked exception only for a recoverable condition, and an unchecked exception for a violated precondition (*Effective Java* item 70). Details: [Exceptions](03-exceptions.md#checked-and-unchecked).

??? question "What does `try { return 1; } finally { return 2; }` return? Does `finally` always run?"

    It returns 2. A `return` in `finally` completes the statement abruptly and discards the saved value 1 and any pending exception (JLS 14.20.2). `finally` does not always run: it does not run if the JVM stops while the thread is in the `try` block (`System.exit`, `Runtime.halt`, a crash, a kill signal), or if the thread never leaves the block. Never write `return`, `break`, or `continue` in `finally`. Details: [Exceptions](03-exceptions.md#try-catch-finally).

??? question "How does `try-with-resources` work, and why is it better than `try`-`finally`?"

    The compiler translates it (JLS 14.20.3) into a `try` with a hidden `finally` that closes each resource that is not `null`, in the reverse order of declaration, before any `catch` or `finally` clause of the statement runs. If the body throws and `close` also throws, the body exception stays the primary exception, and the `close` exception is attached with `addSuppressed`. In a hand-written `try`-`finally`, the exception from `close` replaces the body exception, so the original failure is lost. Details: [Exceptions](03-exceptions.md#try-with-resources).

??? question "How should you handle `InterruptedException`?"

    The exception means that another thread asked this thread to stop, and the method that throws it clears the interrupt flag first. If the method can declare it, propagate it. If it cannot (for example `Runnable.run`), finish the cleanup and restore the flag with `Thread.currentThread().interrupt()`. Do not log and continue, because then `shutdownNow` and task cancellation stop working. Only the code that owns the thread, such as the worker loop of a pool, may swallow the interrupt. Details: [Exceptions](03-exceptions.md#interruptedexception).

## Collections Internals

??? question "How does `HashMap` work inside?"

    A `HashMap` keeps a table whose length is a power of two. It spreads the hash with `h ^ (h >>> 16)`, takes `(length - 1) & hash` as the bucket index, and compares the stored hash and then `equals` along the bucket. When the size passes `capacity × load factor` (0.75 by default), it doubles the table, and each entry stays at index `j` or moves to `j + oldCapacity`. Since JDK 8, a bucket that receives its 9th node becomes a red-black tree if the table has at least 64 buckets; with fewer buckets, the map resizes instead. The expected cost of `get` and `put` is O(1) with well-spread hash codes. Details: [Collections Internals](04-collections-internals.md#hashmap).

??? question "How does `ConcurrentHashMap` stay thread-safe in Java 8 and later?"

    It uses no global lock. `get` takes no lock and reads `volatile` fields. An insert into an empty bucket uses a CAS. An insert, update, or removal in a non-empty bucket takes `synchronized` on the first node of that bucket, so threads that work on different buckets do not wait for each other. The size uses a `baseCount` and counter cells, and a resize is cooperative. It rejects `null` keys and values, and a check-then-act sequence such as `containsKey` then `put` is still not atomic, so use `putIfAbsent` or `computeIfAbsent`. Details: [Collections Internals](04-collections-internals.md#concurrenthashmap).

??? question "Why does removing an element inside a for-each loop throw `ConcurrentModificationException`?"

    The collection increments a `modCount` counter on each structural change, and the fail-fast iterator copies it at creation. In `next()`, the iterator compares the two values and throws if they differ. The for-each loop uses an iterator, so `list.remove` in the body makes the next `next()` throw. The check is best-effort (javadoc), so a program must not depend on it. Safe tools: `Iterator.remove`, `removeIf`, or a loop over a copy. Details: [Collections Internals](04-collections-internals.md#fail-fast-iterators-and-concurrentmodificationexception).

??? question "`ArrayList` or `LinkedList`: which one, and why?"

    Use `ArrayList` for lists. It stores references in one array, so `get(i)` is O(1), an append is amortized O(1), and a scan reads memory in sequence. `LinkedList` allocates a node for each element, and `get(i)` is O(n). Its O(1) insert in the middle needs an iterator that already stands at the position, and reaching that position costs O(n). For stacks and queues, `ArrayDeque` is better. Details: [Collections Internals](04-collections-internals.md#arraylist).

## JVM Architecture

??? question "Explain class loading in the JVM. What are the phases?"

    A class passes through loading, linking, and initialization (JVMS chapter 5). Loading finds the bytes and builds the class. Linking has verification (type-safe bytecode), preparation (static fields get default values), and resolution (symbolic references become direct ones). Initialization runs `<clinit>` once, under a per-class lock (JVMS 5.5). HotSpot loads lazily, when a running program first needs the class. Details: [JVM Architecture](05-jvm-architecture.md#the-lifecycle-of-a-class).

??? question "What is parent delegation? Why does it exist, and when would you break it?"

    A loader first checks whether it already defined the class. If not, it asks its parent, and only if the parent fails does it call its own `findClass`. The reasons are security and consistency: core classes such as `java.lang.String` always come from the bootstrap loader, and one name means one class in a loader tree. Frameworks break the order on purpose: Tomcat's web application loader looks in `/WEB-INF/classes` and `/WEB-INF/lib` after the JVM bootstrap classes and before the parent loaders, so each web application can ship its own library versions. The cost is two copies of one class: a class's identity is its name plus its defining loader, so a cast between the copies fails with `ClassCastException`. Details: [JVM Architecture](05-jvm-architecture.md#parent-delegation).

??? question "What is the difference between `ClassNotFoundException` and `NoClassDefFoundError`?"

    `ClassNotFoundException` is a checked exception: code asks a loader for a class by name (`Class.forName`, `loadClass`) and the loader finds no bytes. `NoClassDefFoundError` is a `LinkageError`: the class existed at compile time, but the JVM cannot find it at run time, or its static initializer failed in an earlier attempt. In the second case, the first failure threw `ExceptionInInitializerError`, and every later use throws `NoClassDefFoundError: Could not initialize class X`. Check the run-time class path for a `provided` or `optional` dependency, and search the log for the first `ExceptionInInitializerError`. Details: [JVM Architecture](05-jvm-architecture.md#classnotfoundexception-and-noclassdeffounderror).

??? question "Describe the JVM runtime data areas. Which are per thread, and which are shared?"

    All threads share the heap (objects and arrays) and the method area (class metadata and the run-time constant pool, which HotSpot keeps in Metaspace in native memory). Each thread has a PC register, a JVM stack with one frame per call (local variables and the operand stack), and a native method stack. A local variable holds a primitive value or a reference, and the object itself lives on the heap. `StackOverflowError` comes from the JVM stack, `OutOfMemoryError: Java heap space` from the heap, and `OutOfMemoryError: Metaspace` from class metadata. The process also uses memory outside these areas (code cache, direct buffers, thread stacks), so a container limit must be larger than `-Xmx`. Details: [JVM Architecture](05-jvm-architecture.md#runtime-data-areas).

??? question "What is the JIT compiler? Explain tiered compilation."

    HotSpot starts a method in the interpreter, counts calls and loop back-edges, and compiles hot methods on a background thread. Tier 0 is the interpreter, tiers 1 to 3 are C1, and tier 4 is C2. The usual path is interpreter, C1 with full profiling after about 200 invocations, then C2 after about 5,000 (JDK 21 `PrintFlagsFinal` defaults; the policy also weighs back-edges and the compile queue). C2 uses the profile for speculative optimizations: inlining, devirtualization, and escape analysis. The cost is warm-up time, and if a C2 assumption fails, the JVM deoptimizes the code and returns to the interpreter. Details: [JVM Architecture](05-jvm-architecture.md#the-execution-engine-interpreter-c1-and-c2).

## Memory and Garbage Collection

??? question "How does the garbage collector decide which objects to free, and what is the generational hypothesis?"

    The collector uses reachability, not reference counting. It starts from the GC roots (local variables of live threads, static fields, JNI references), marks every object that a chain of references reaches, and reclaims the unmarked objects. The generational hypothesis says that most objects die young, so the JVM collects the young generation often and copies the few survivors, and it collects the old generation rarely. Serial, Parallel, and G1 record old-to-young references with a card table and a write barrier, so a young collection does not scan the whole old generation. Details: [Memory and Garbage Collection](06-memory-and-gc.md#how-a-collector-finds-garbage).

??? question "How does G1 work, and why is it the default?"

    G1 splits the heap into equal regions, and each region is Eden, survivor, old, or humongous. Young collections are stop-the-world pauses that evacuate live objects. When heap occupancy passes the initiating threshold (`InitiatingHeapOccupancyPercent`, default 45, adapted at run time), G1 starts concurrent marking, and then it runs mixed collections that add the old regions with the most garbage. It sizes the collection set to meet `MaxGCPauseMillis` (default 200 ms), which is a goal and not a guarantee. G1 is the default since JDK 9 (JEP 248) because it balances throughput and pause time and needs little tuning. Details: [Memory and Garbage Collection](06-memory-and-gc.md#g1-garbage-first).

??? question "How is ZGC different from G1, and when do you use it?"

    ZGC does marking, relocation, and reference updates concurrently, so its pauses do not depend on the heap size. JEP 439 states the goal of pauses under 1 millisecond. In JDK 21 you enable the generational version with `-XX:+UseZGC -XX:+ZGenerational`. It became the default mode of ZGC in JDK 23 (JEP 474), and JDK 24 removed the non-generational mode (JEP 490); G1 stays the JVM default collector. The costs are throughput and CPU from the load barriers, and 8-byte references (no compressed oops). Recommendation: use ZGC when G1 pauses violate a latency requirement, or for a very large heap. Details: [Memory and Garbage Collection](06-memory-and-gc.md#zgc).

??? question "What are strong, soft, weak, and phantom references? When do you use each?"

    A strong reference keeps its object alive. The collector clears a `SoftReference` when memory is short, and always before `OutOfMemoryError`. It clears a `WeakReference` at the next collection once no strong or soft reference reaches the object. A `PhantomReference` always returns `null` from `get()`, and the collector enqueues it after the object becomes phantom reachable, so a cleanup thread learns that the object is gone. Uses: weak references for canonical maps (`WeakHashMap`), phantom references and `Cleaner` for native resource cleanup, and soft references for memory-sensitive caches, although a bounded cache library gives a policy that you control. Details: [Memory and Garbage Collection](06-memory-and-gc.md#reference-types).

??? question "How do you find a memory leak in a running Java service?"

    First confirm the symptom in the GC log: the heap after full or mixed collections rises over time. Then take two class histograms with `jcmd <pid> GC.class_histogram`, ten minutes apart, and look at the classes whose instance count grows. Take a heap dump and open it in Eclipse MAT. Use the dominator tree and "path to GC roots, excluding weak and soft references". The last link before the root is the cause, for example a static map, a `ThreadLocal`, or a listener list. Details: [Memory and Garbage Collection](06-memory-and-gc.md#how-to-find-a-leak).

## Concurrency Basics

??? question "What does `volatile` guarantee, and what does it not guarantee?"

    `volatile` guarantees visibility and ordering for one variable: a write happens-before every later read of that field (JLS 17.4.5), and the reader also sees the writes that the writer made before it. It makes `long` and `double` reads and writes atomic. It does not make a compound action atomic, so `count++` on a volatile field can lose updates, and it gives no mutual exclusion. Use `volatile` for a flag or for a reference to an immutable object that one thread replaces as a whole. Details: [Concurrency Basics](07-concurrency-basics.md#volatile).

??? question "What is the happens-before relation? Name the rules."

    Happens-before is a partial order over actions: if x happens-before y, then x is visible to y and ordered before it (JLS 17.4.5). The rules: program order within one thread; an unlock happens-before every later lock of the same monitor; a volatile write happens-before every later read of that field; `Thread.start()` happens-before the actions of the started thread; the actions of a thread happen-before another thread returns from `join()` on it. The relation is transitive. Two conflicting accesses without such an edge are a data race. Details: [Concurrency Basics](07-concurrency-basics.md#happens-before-in-the-jls).

??? question "Why must you call `wait()` in a `while` loop?"

    A thread can wake up without a notification (a spurious wakeup, allowed by JLS 17.2.1). Also, between the `notify` and the moment the woken thread reacquires the monitor, another thread can make the condition false again. With `if`, the thread continues although the condition does not hold. With `while`, the thread tests the condition again and waits again if needed. Details: [Concurrency Basics](07-concurrency-basics.md#the-guarded-block-pattern-and-the-spurious-wakeup).

??? question "What is the difference between `sleep()` and `wait()`?"

    `sleep` is a static method of `Thread`. It pauses the current thread for a time and keeps all monitors that the thread owns. `wait` is an instance method of `Object`. The caller must own that object's monitor, and `wait` releases that one monitor until a `notify`, an interrupt, a timeout, or a spurious wakeup. A call to `wait` outside `synchronized` throws `IllegalMonitorStateException`. Details: [Concurrency Basics](07-concurrency-basics.md#questions) and [Thread states](07-concurrency-basics.md#thread-states).

??? question "What are the four conditions for deadlock, and how do you prevent it?"

    Mutual exclusion, hold and wait, no preemption, and circular wait (Coffman, 1971). All four must hold, so breaking one prevents the deadlock. The common fix is a global lock order, which breaks circular wait. Other fixes: hold one lock only, or use `tryLock` with a timeout and release on failure. To find a deadlock in production, take a thread dump with `jstack <pid>` or `jcmd <pid> Thread.print`, which reports "Found one Java-level deadlock" for monitors and for `java.util.concurrent` locks. Details: [Concurrency Basics](07-concurrency-basics.md#deadlock).

## java.util.concurrent

??? question "Explain the parameters of `ThreadPoolExecutor` and what happens when you submit a task."

    The pool has a core size, a maximum size, a keep-alive time, a work queue, a thread factory, and a rejection handler. On `execute`, if fewer than `corePoolSize` workers run, the pool starts a new worker. Otherwise it offers the task to the queue. If the queue is full, it starts a new worker up to `maximumPoolSize`. If that fails, it calls the rejection handler. The pool grows past the core size only after the queue is full, so with an unbounded queue the maximum has no effect. Details: [java.util.concurrent](08-java-util-concurrent.md#threadpoolexecutor-the-parameters).

??? question "Why is `Executors.newFixedThreadPool` considered dangerous in production?"

    It creates a pool with the core size equal to the maximum size and an unbounded `LinkedBlockingQueue`. The pool never rejects a task, so if tasks arrive faster than the threads finish them, the queue grows until the heap is full and the JVM throws `OutOfMemoryError`. Callers also get no back-pressure. In production, construct `ThreadPoolExecutor` with a bounded queue, a maximum size, named threads, and a rejection policy that you chose. Details: [java.util.concurrent](08-java-util-concurrent.md#why-executorsnewfixedthreadpool-uses-an-unbounded-queue).

??? question "How do you size a thread pool?"

    The size depends on what the tasks wait for. For CPU-bound tasks, use about the number of cores, or cores plus one. For I/O-bound tasks, the *Java Concurrency in Practice* heuristic is `N_cpu * U_cpu * (1 + W/C)`, where `U_cpu` is the target CPU use, `W` the wait time, and `C` the compute time. Measure `W` and `C`, then load-test, because a downstream limit such as a connection pool can cap the useful size. Keep separate pools for CPU work and I/O work, and on Java 21 consider virtual threads for the waiting tasks. Details: [java.util.concurrent](08-java-util-concurrent.md#sizing-a-pool).

??? question "What is the difference between `synchronized` and `ReentrantLock`?"

    Both are reentrant mutual-exclusion locks with the same visibility guarantees. `synchronized` is built into the language and releases the monitor automatically, also on an exception. `ReentrantLock` adds `tryLock`, a timed `tryLock`, `lockInterruptibly`, an optional fair mode, and several `Condition` objects, but you must call `unlock()` in a `finally` block. Use `synchronized` by default, and `ReentrantLock` when you need one of its features. On Java 21, a virtual thread that blocks inside `synchronized` pins its carrier, and JEP 491 (Java 24) removes most of that pinning. Details: [java.util.concurrent](08-java-util-concurrent.md#locks).

??? question "How does a CAS-based atomic class work, and what is the ABA problem?"

    A CAS is an atomic hardware operation that stores a new value only if the variable still holds an expected value. `AtomicInteger.incrementAndGet` loops on `compareAndSet(current, current + 1)` until it succeeds, with no lock. The ABA problem: a thread reads `A`, another thread changes the value to `B` and back to `A`, and the first thread's CAS still succeeds although the state changed. The fix is a version stamp, as in `AtomicStampedReference`. Details: [java.util.concurrent](08-java-util-concurrent.md#atomics-and-compare-and-set).

??? question "What is the difference between `CountDownLatch` and `CyclicBarrier`?"

    A `CountDownLatch` is a one-shot gate: some threads call `countDown()`, other threads wait in `await()` until the count reaches 0, and the latch cannot be reset. A `CyclicBarrier` makes a fixed number of threads wait for each other: every thread calls `await()`, all continue when the last one arrives, and the barrier resets for the next round. If one party is interrupted or times out, the barrier breaks and the others get `BrokenBarrierException`. Use a latch for "wait until n things happened" and a barrier for "all threads reach a point together, again and again". Details: [java.util.concurrent](08-java-util-concurrent.md#synchronizers).

## CompletableFuture

??? question "Why use `CompletableFuture` instead of `Future`?"

    A `Future` can only block: `get()` holds a thread until the result is ready, and you cannot attach a callback or combine two futures without blocking. `CompletableFuture` implements `CompletionStage`, so you attach steps (`thenApply`, `thenCompose`, `thenCombine`) that run when the result is ready, and no thread waits in between. You can complete it yourself with `complete`, which adapts callback APIs, and it has timeouts (`orTimeout`) and error handlers (`handle`, `exceptionally`). The cost is more complex code, wrapper exceptions, and an executor to manage. Details: [CompletableFuture](09-completablefuture.md#why-completablefuture).

??? question "What is the difference between `thenApply` and `thenCompose`?"

    `thenApply` maps a value to a value. If its function returns a `CompletableFuture<U>`, the result is a nested `CompletableFuture<CompletableFuture<U>>`. `thenCompose` takes a function that returns a stage and flattens it, like `flatMap`. Use `thenApply` for a synchronous step and `thenCompose` for a step that is itself asynchronous. For two independent calls, use `thenCombine` or `allOf` so that they run in parallel. Details: [CompletableFuture](09-completablefuture.md#composing-stages).

??? question "What is the default executor of `supplyAsync`, and which thread runs a callback such as `thenApply`?"

    In JDK 21 the default executor is `ForkJoinPool.commonPool()`, with a default parallelism of `availableProcessors() - 1` and daemon threads. In JDK 21 to 24, if that parallelism is 1 or less, `CompletableFuture` starts a new thread for each task instead, and JDK 25 removed that fallback. The common pool is shared with parallel streams and sized for CPU-bound work, so pass your own bounded executor for I/O or long tasks. A plain `thenApply` runs on the thread that calls it if the source is already complete, and otherwise on the thread that completes the source. `thenApplyAsync` runs on the default executor, or on yours if you pass one, and a stage never inherits the executor of its source. Details: [CompletableFuture](09-completablefuture.md#which-thread-runs-a-stage).

??? question "How do exceptions propagate in a `CompletableFuture` pipeline?"

    If a stage's function throws, the stage completes exceptionally with the exception wrapped in a `CompletionException`, and dependent stages with normal functions are skipped. `exceptionally` replaces a failure with a value, `handle` sees both value and exception, and `whenComplete` observes the outcome without changing it. `join()` throws the unchecked `CompletionException`, and `get()` throws the checked `ExecutionException`, so unwrap the cause before you test the type. If nobody observes a failed stage, nothing is reported. Details: [CompletableFuture](09-completablefuture.md#error-handling).

## Virtual Threads

??? question "What is a virtual thread, and how is it different from a platform thread?"

    A virtual thread (JEP 444, final in Java 21) is a `java.lang.Thread` that the JDK schedules, not the OS. A platform thread holds one OS thread for its whole life. A virtual thread holds an OS thread (its carrier) only while it runs, so a JVM can run millions of them on a few OS threads. You create one for each task and do not pool it. Virtual threads give throughput for waiting work, not faster execution. Details: [Virtual Threads](10-virtual-threads.md#the-model-many-virtual-threads-on-few-carriers).

??? question "When do virtual threads help, and when do they not?"

    They help when many tasks run at once and each task mostly waits, such as a server that calls a database and other services for each request. JEP 444 says more than a few thousand concurrent tasks, and not CPU-bound. They do not help CPU-bound work, because the scheduler does not preempt, and they do not remove other limits: a connection pool of 10 still allows 10 concurrent queries. On Java 21, long blocking inside `synchronized` pins the carrier. Details: [Virtual Threads](10-virtual-threads.md#trade-offs-and-when-to-use).

??? question "What is pinning? How do you find it and fix it?"

    Pinning means a virtual thread blocks and cannot unmount, so it keeps its carrier. In Java 21 the causes are blocking inside `synchronized` and blocking inside a native frame. Find it with the JFR event `jdk.VirtualThreadPinned`, or on Java 21 with `-Djdk.tracePinnedThreads=full` (JDK 24 removed that property). On Java 21, fix it by replacing a frequently run `synchronized` block that guards blocking I/O with a `ReentrantLock`. JEP 491 (Java 24) removes the `synchronized` and `Object.wait` cases, but native frames that call back into blocking Java code, class loading, and class initializers still pin. Details: [Virtual Threads](10-virtual-threads.md#pinning-in-java-21).

??? question "How do you enable virtual threads in Spring Boot, and what does it change?"

    Set `spring.threads.virtual.enabled=true` on Java 21 or later with Spring Boot 3.2 or later (the property has the same name in Spring Boot 4.x). Tomcat and Jetty then handle requests on virtual threads, and the auto-configured `applicationTaskExecutor` starts a virtual thread for each task. It does not change the connection pool size (HikariCP defaults to 10), and your own `ThreadPoolTaskExecutor` beans stay on platform threads. Virtual threads are daemon threads, so an application with no web server (only listeners or `@Scheduled` jobs) needs `spring.main.keep-alive=true`. Details: [Virtual Threads](10-virtual-threads.md#spring-boot-support).

## Concurrency Coding Problems

??? question "Write code that prints odd and even numbers alternately with two threads."

    Keep the next number in a field that one monitor protects. The odd thread waits in a `while` loop while the next number is even, and the even thread waits while it is odd. The thread that has its turn prints the number, increments it, and calls `notifyAll`. Explain `while` (the condition may be false again after `wait()` returns) and `notifyAll` (the safe default when threads wait for different conditions). The alternative is two semaphores. Details: [Concurrency Coding Problems](11-concurrency-coding-problems.md#problem-1-print-odd-and-even-numbers-with-two-threads).

??? question "Implement a bounded blocking queue."

    Use a ring buffer and one `ReentrantLock` with two conditions, `notFull` and `notEmpty`. `put` waits on `notFull` while the buffer is full, stores the item, and signals `notEmpty`. `take` does the reverse. Every wait is in a `while` loop, and all state changes happen under the lock. With one condition, a `signal` can wake a waiter of the same kind, and the other kind never wakes (a lost wakeup). Details: [Concurrency Coding Problems](11-concurrency-coding-problems.md#problem-4-bounded-blocking-queue-from-scratch).

??? question "Write a thread-safe singleton. Why does double-checked locking need `volatile`?"

    The best choices are the initialization-on-demand holder (a nested class with a `static final` instance, initialized once under the class initialization lock) or an enum with one constant, which is also safe against serialization and reflection. Double-checked locking also works with a `volatile` field. `instance = new Service()` is allocate, construct, store. Without `volatile`, another thread can see the stored reference before the constructor's writes, so the unsynchronized check can return a half-built object. Details: [Concurrency Coding Problems](11-concurrency-coding-problems.md#problem-6-thread-safe-singleton).

??? question "Solve the dining philosophers problem. Which deadlock condition do you remove?"

    The wrong solution takes the left fork and then the right fork with blocking locks, so all philosophers can wait in a circle. Remove the circular wait with lock ordering: number the forks and take the lower number first. Other solutions: for five philosophers, a `Semaphore(4)` that lets at most four try to eat, or `tryLock` with a timeout and a random back-off (this removes hold and wait). These remove deadlock, not starvation, and fair locks reduce starvation at a cost in throughput. Details: [Concurrency Coding Problems](11-concurrency-coding-problems.md#problem-7-dining-philosophers).

## Functional Programming and Streams

??? question "Streams are lazy. What does that mean, and what happens in `list.stream().filter(f).map(g).findFirst()`?"

    Intermediate operations such as `filter` and `map` only add a stage to the pipeline. No element moves until a terminal operation runs. Then the terminal operation pushes the elements one by one through the whole chain. `findFirst` is short-circuiting, so the traversal stops at the first element that passes `filter`: `f` runs for the elements up to and including that match, and `g` runs for that element only. A side effect in a stage can therefore run zero times or fewer times than you expect. Details: [Functional Programming and Streams](12-functional-and-streams.md#how-a-stream-pipeline-runs).

??? question "What is the difference between `map` and `flatMap`, and between `reduce` and `collect`?"

    `map` turns each element into one element. `flatMap` turns each element into a stream and joins the streams, for a one-to-many step such as `orders.stream().flatMap(o -> o.items().stream())`. `reduce` folds elements into an immutable value with an associative operation and an identity (`reduce(0, Integer::sum)`). `collect` does a mutable reduction into a container with a supplier, an accumulator, and a combiner. Use `collect` for lists, maps, and strings, because a `reduce` that builds a `List` copies it at each step and is not safe in parallel. Details: [Functional Programming and Streams](12-functional-and-streams.md#questions) and [Collectors](12-functional-and-streams.md#collectors).

??? question "Are parallel streams always faster? When are they slower or dangerous?"

    No. A parallel stream splits the source, runs the parts in `ForkJoinPool.commonPool()`, and merges the results, and that overhead costs time. It is slower for small data sets, cheap work per element, sources that split badly (`LinkedList`), boxed streams, and order-dependent operations. It is dangerous with shared mutable state (a data race) and with blocking lambdas, which block common-pool threads for every other user. Use it for large, CPU-bound, stateless work after a benchmark shows a gain. Details: [Functional Programming and Streams](12-functional-and-streams.md#parallel-streams).

??? question "When would you use `Optional`, and when is it a mistake?"

    Use it as a return type for a method that can have no result, so that the caller must handle the empty case. Chain `map`, `filter`, and `orElseGet` instead of `isPresent()` followed by `get()`. It is a mistake as a field, as a method parameter, as a collection element, and as a return type for a collection (return an empty collection). `orElse(x)` evaluates `x` even if a value is present, so use `orElseGet` when `x` is costly. Details: [Functional Programming and Streams](12-functional-and-streams.md#optional).

## Modern Java (9 to 21)

??? question "What is the difference between a switch statement and a switch expression?"

    A switch expression (JEP 361, Java 14) produces a value. With arrow cases there is no fall-through, a case can list several constants, and a multi-statement branch uses `yield`. A switch expression must be exhaustive, and the compiler enforces it. So it removes two classic bugs, the missing `break` and the forgotten case. If an enum gets a new constant after compilation, a switch expression throws `MatchException` in Java 21 (Java 14 to 20 threw `IncompatibleClassChangeError`). Details: [Modern Java (9 to 21)](13-modern-java.md#switch-expressions-jep-361).

??? question "What is a record? What does the compiler generate, and what are the limits?"

    A record (JEP 395, Java 16) is a final class for immutable data. For `record Point(int x, int y)`, the compiler generates private final fields, a canonical constructor, accessors `x()` and `y()`, and `equals`, `hashCode`, and `toString`. A record cannot extend a class or declare extra instance fields. Its immutability is shallow, so copy a mutable component such as a `List` in the compact constructor with `List.copyOf`. Records suit DTOs, keys, and messages, not JPA entities. Details: [Modern Java (9 to 21)](13-modern-java.md#records-jep-395).

??? question "What are sealed classes for, and how does pattern matching for `switch` use them?"

    A sealed class or interface (JEP 409, Java 17) lists the subtypes that may extend it with `permits`. Each permitted subtype must be `final`, `sealed`, or `non-sealed`. The compiler then knows all the cases, so a pattern `switch` (JEP 441, final in Java 21) over the type needs no `default`, and it fails to compile if a case is missing. Put specific labels before general ones, because the compiler reports dominated labels as errors. A sealed type is wrong for an extension point. Details: [Modern Java (9 to 21)](13-modern-java.md#sealed-classes-and-interfaces-jep-409).

??? question "How would you model a closed set of results (success, validation error, not found) in Java 21?"

    Use a sealed interface with record cases: `sealed interface Result permits Ok, Invalid, NotFound`. The caller uses a pattern `switch` with no `default`, and the compiler guarantees that it handles every case. If a later change adds a case, each `switch` fails to compile until a developer handles it. Exceptions need less code in the caller but give no compiler check. Recommendation: use result types when failure is a normal outcome, and exceptions for unexpected failures. Details: [Modern Java (9 to 21)](13-modern-java.md#algebraic-data-types-records-sealed-types-and-switch-patterns-together).

## Reflection and Annotations

??? question "How much slower is reflection, and how do you make it faster?"

    A reflective call boxes the arguments into an `Object[]`, and checks the types at each call, so it costs more than a direct call. The JIT compiler inlines through `Method.invoke` only in limited cases, such as a `Method` in a `static final` field (JEP 416, Java 18). A lookup such as `getMethod` costs more still. Look up once and keep the `Method` in a `static final` field, call `setAccessible(true)` once, or use a `MethodHandle` in a `static final` field with `invokeExact`, which the JIT compiler can inline. Measure first, because one database or network call can cost more than the reflection overhead. Details: [Reflection and Annotations](14-reflection-and-annotations.md#performance-of-reflection-and-methodhandle).

??? question "How does a JDK dynamic proxy work, and how does Spring use proxies for `@Transactional`?"

    `Proxy.newProxyInstance` generates a class at run time that implements the interfaces that you give, and each call goes to `InvocationHandler.invoke`. Only interfaces can be proxied. Spring wraps the bean in a proxy that starts a transaction before the call and commits or rolls back after it. A CGLIB proxy is a generated subclass, so it cannot advise `final` or `private` methods. Spring Framework picks a JDK proxy for a bean with interfaces, but Spring Boot makes CGLIB the default (`spring.aop.proxy-target-class` is `true`), and both proxy types have the self-invocation limit. Details: [Reflection and Annotations](14-reflection-and-annotations.md#how-spring-uses-proxies-and-annotations).

## I/O, NIO, and Serialization

??? question "Why is Java deserialization dangerous, and how do you protect a service?"

    `ObjectInputStream.readObject` creates the classes that the byte stream names and runs their `readObject` methods before the application can check the type. An attacker who controls the bytes can chain methods of classes on the class path into a harmful operation (a gadget chain). Best protection: do not deserialize untrusted data with Java serialization, and use JSON or Protobuf. If you must, set a serialization filter with an allow list (JEP 290, JDK 9), and on JDK 17 or later use a filter factory (JEP 415). A filter lowers the risk but does not remove the need to avoid untrusted input. Details: [I/O, NIO, and Serialization](15-io-nio-and-serialization.md#why-deserialization-is-dangerous).

??? question "What does `serialVersionUID` do, and what happens if you leave it out?"

    It identifies the version of a serializable class. `ObjectInputStream` compares the value in the stream with the value of the local class, and throws `InvalidClassException` if they differ. If you do not declare it, the runtime computes it from class details, so a recompile or a harmless change, such as a new method, can change it, and old data then fails to load. Declare `private static final long serialVersionUID`, keep the value for a compatible change, and change it only to reject old data. Details: [I/O, NIO, and Serialization](15-io-nio-and-serialization.md#serialversionuid).

??? question "What is the difference between a byte stream and a character stream, and when does the difference cause bugs?"

    A byte stream (`InputStream`, `OutputStream`) moves raw bytes. A character stream (`Reader`, `Writer`) moves `char` values and uses a charset to convert. The bug appears at the conversion: if the writer and the reader use different charsets, the characters change. Before JDK 18 the default charset depended on the operating system, and JDK 18 (JEP 400) made UTF-8 the default for the standard APIs (`System.out` still follows the console). Pass `StandardCharsets.UTF_8` explicitly. Details: [I/O, NIO, and Serialization](15-io-nio-and-serialization.md#byte-streams-character-streams-and-the-decorator-chain).

## JDBC, JPA, and Hibernate

??? question "What is the difference between `Statement` and `PreparedStatement`, and why does it matter for security?"

    A `PreparedStatement` takes SQL with `?` placeholders and binds the values through setters. The database treats a bound value as data, never as SQL syntax, so input such as `x' OR '1'='1` is only a string to compare. A `Statement` that builds SQL by string concatenation lets the input change the statement, which is SQL injection. Placeholders replace values only, so pick a table name or a sort column from an allow-list in code. JPQL has the same risk when the query string is concatenated, so use named parameters there too. Details: [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md#statement-versus-preparedstatement-and-sql-injection).

??? question "What is the N+1 problem in JPA? How do you find it and fix it?"

    One query loads N parents. The code then touches a lazy association on each parent, and Hibernate runs one more query for each, so the total is 1 + N. Find it by counting statements with Hibernate `Statistics`, SQL logging, or a test that asserts the statement count. Fix it with `JOIN FETCH`, an entity graph, `@BatchSize` (which turns N queries into N / size), or a DTO projection. Details: [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md#the-n1-problem-and-its-fixes).

??? question "What is the difference between `LAZY` and `EAGER`, and why do you get a `LazyInitializationException`?"

    `LAZY` loads an association when the code first uses it, and `EAGER` loads it with the owner. In JPA, `@ManyToOne` and `@OneToOne` are `EAGER` by default, and `@OneToMany` and `@ManyToMany` are `LAZY`. Recommendation: set all associations to `LAZY` and choose what to load for each use case. A lazy association loads through the `Session`, so touching it after the session closed throws the exception. Fix it by loading the data while the transaction is open, with `JOIN FETCH`, an entity graph, or a DTO, and do not switch to `EAGER` to silence it. Details: [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md#lazy-and-eager-loading).

??? question "What is the persistence context, and what are the four entity states?"

    The persistence context is the set of entity instances that one `EntityManager` manages. It keeps one instance for each row, and it compares each entity with a snapshot at flush time (dirty checking), so a change to a managed entity becomes an `UPDATE` without an explicit `save`. The states are new, managed, detached, and removed. `persist` makes a new entity managed, and `merge` copies a detached object onto a managed instance and returns that instance, while the argument stays detached. Details: [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md#the-persistence-context-and-the-first-level-cache).

??? question "How does optimistic locking with `@Version` work?"

    Hibernate increments the `@Version` field on each update, and the `UPDATE` has `where id = ? and version = ?` with the version that the entity was loaded with. If another transaction committed first, 0 rows match, and Hibernate throws `OptimisticLockException`, which Spring translates to `ObjectOptimisticLockingFailureException`. The caller retries the whole transaction or tells the user. It holds no lock between read and write, so it suits low contention. For hot rows, pessimistic locking (`SELECT ... FOR UPDATE`) avoids repeated retries but makes threads wait. Details: [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md#optimistic-locking-with-version).

## Spring Core

??? question "Why is constructor injection preferred over field injection?"

    A constructor guarantees that the object has all mandatory dependencies when it exists, and the fields can be `final`. A unit test creates the class with `new` and a fake, without a container or reflection. A circular dependency through constructors fails at startup, so the error shows early. A constructor with many parameters exposes a class with too many responsibilities. The limit: constructor injection does not express an optional dependency well, so use `ObjectProvider<T>` or `Optional<T>` there. Details: [Spring Core](17-spring-core.md#injection-styles-and-why-the-constructor-wins).

??? question "Describe the lifecycle of a singleton bean."

    The container instantiates the bean, injects dependencies, calls the `Aware` callbacks, runs `postProcessBeforeInitialization`, runs the init callbacks (`@PostConstruct`, then `afterPropertiesSet`, then the custom init method), and runs `postProcessAfterInitialization`, where AOP replaces the bean with a proxy. At close, it runs `@PreDestroy`, then `DisposableBean.destroy`, then the custom destroy method. The constructor runs before injection, so use `@PostConstruct` for logic that needs dependencies. Spring does not call destroy callbacks for prototype beans. Details: [Spring Core](17-spring-core.md#the-bean-lifecycle).

??? question "What are the bean scopes, and what goes wrong if you inject a prototype bean into a singleton?"

    The scopes are `singleton` (the default), `prototype`, and the web scopes `request`, `session`, `application`, and `websocket`. The container injects dependencies once, when it creates the singleton, so the singleton gets one prototype instance and keeps it, and the prototype scope has no effect. Fixes: inject an `ObjectProvider<T>` and call `getObject()` for each use, declare the prototype with a scoped proxy, or use a `@Lookup` method. The same fix applies to a request-scoped bean inside a singleton. Details: [Spring Core](17-spring-core.md#the-prototype-in-singleton-problem).

??? question "How does Spring resolve circular dependencies? What changed in Spring Boot 2.6?"

    A cycle through constructors cannot be resolved and fails with `BeanCurrentlyInCreationException`. For setter and field injection, Spring registers an early reference to the bean in creation (one of three singleton caches), and the partner receives that reference. Spring Boot 2.6 turned these cycles off by default (`spring.main.allow-circular-references=false`), because a cycle shows a design problem and one bean receives a partner that is not fully initialized; plain Spring Framework still allows them. The best fix is to remove the cycle with a third bean or an event, and `@Lazy` on one injection point is the quick bridge. Details: [Spring Core](17-spring-core.md#circular-dependencies).

??? question "Why does `@Transactional` (or any aspect) not apply when a method calls another method of the same class?"

    The caller outside the bean holds the proxy, and the proxy calls the target. Inside the target, `this` is the target, so `this.other()` is a plain Java call that skips the proxy and runs no advice. Fixes: move the method to another bean (best), inject the bean into itself with `@Lazy`, use `AopContext.currentProxy()` (needs `exposeProxy = true`), or use AspectJ weaving. A `private` method has a different cause: a proxy cannot override it, so `@Transactional` there has no effect. Since Spring Framework 6.0, a CGLIB proxy also applies `@Transactional` to `protected` and package-private methods, but only for calls through the proxy, so make transactional methods public. Details: [Spring Core](17-spring-core.md#why-an-internal-call-skips-the-proxy) and [Spring Data and Transactions](19-spring-data-and-transactions.md#self-invocation-and-visibility).

## Spring Boot

??? question "What does `@SpringBootApplication` do?"

    It combines three annotations: `@SpringBootConfiguration` (a `@Configuration` class), `@EnableAutoConfiguration` (turns on auto-configuration), and `@ComponentScan` (scans the package of the class and its sub-packages). The scan has exclude filters that skip auto-configuration classes and test configuration. So put the main class in the root package and keep the application packages below it, or the scan misses your beans. Details: [Spring Boot](18-spring-boot.md#springbootapplication).

??? question "How does Spring Boot auto-configuration work?"

    `@SpringBootApplication` includes `@EnableAutoConfiguration`, which imports `AutoConfigurationImportSelector`. The selector reads the `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` files from the jars on the classpath (Boot 3.0 and later read this file, not `spring.factories`). Each candidate class carries conditions such as `@ConditionalOnClass` and `@ConditionalOnMissingBean`. The selector runs after the application's own configuration, so a bean that you define makes `@ConditionalOnMissingBean` false and the default backs off. To see the result, run with `--debug` for the condition evaluation report. Details: [Spring Boot](18-spring-boot.md#auto-configuration).

??? question "In which order does Spring Boot resolve a property that is set in several places?"

    The closer a setting is to the process launch, the higher its precedence. The main sources, from the top: test annotations, command-line arguments, `SPRING_APPLICATION_JSON`, Java system properties, OS environment variables, config data files, `@PropertySource`, and default properties. Among config data files, files outside the jar beat files inside it, and profile-specific files beat the general files. So `--server.port=9000` overrides `SERVER_PORT=8081`, which overrides `server.port=8080` in `application.properties`. Details: [Spring Boot](18-spring-boot.md#property-source-precedence).

??? question "What is the difference between `@ConfigurationProperties` and `@Value`?"

    `@ConfigurationProperties` binds a group of properties with one prefix into a typed object (a class or a record), with relaxed name matching, rich conversion (`Duration`, `DataSize`), optional validation with `@Validated`, and IDE metadata. `@Value` injects one property, supports SpEL, and has limited relaxed binding. Use `@ConfigurationProperties` for related settings, and `@Value` for a single value or an expression. Neither one refreshes at run time without extra support, such as Spring Cloud's refresh scope. Details: [Spring Boot](18-spring-boot.md#configurationproperties-versus-value).

## Spring Data and Transactions

??? question "How does `@Transactional` work?"

    Spring wraps the bean in a proxy. The proxy sends each external call to a `TransactionInterceptor`, which asks the `PlatformTransactionManager` for a transaction according to the propagation rule. For a new transaction, `JpaTransactionManager` opens an `EntityManager` and a connection and binds them to the current thread. If the method returns, the interceptor commits. If it throws an exception that matches the rollback rules, it rolls back. Only calls through the proxy get a transaction, and a transaction does not cross threads. Details: [Spring Data and Transactions](19-spring-data-and-transactions.md#how-transactional-works).

??? question "Name the seven propagation types and say when you would use `REQUIRES_NEW`."

    `REQUIRED` (default: join or create), `REQUIRES_NEW` (suspend the current one and create an independent one), `SUPPORTS`, `NOT_SUPPORTED`, `MANDATORY`, `NEVER`, and `NESTED` (a savepoint in the existing transaction). Use `REQUIRES_NEW` for work that must commit or fail on its own, such as an audit log that must survive the rollback of the caller. The risks: it takes a second connection while the first stays open, so it can exhaust the pool, and it can wait for a lock that the suspended outer transaction holds. Details: [Spring Data and Transactions](19-spring-data-and-transactions.md#propagation-the-seven-types).

??? question "What happens if an inner `@Transactional` method throws, and the outer method catches the exception?"

    With `REQUIRED`, the inner call joined the outer transaction. When the exception passed through the inner proxy, the interceptor marked the shared transaction as rollback-only. The outer method returns normally, but the commit fails with `UnexpectedRollbackException`, and nothing is saved. To let the inner work fail without losing the outer work, use `NESTED` (rolls back to a savepoint) or `REQUIRES_NEW` (a separate transaction). Details: [Spring Data and Transactions](19-spring-data-and-transactions.md#propagation-the-seven-types).

??? question "Does a checked exception roll back the transaction?"

    Not by default. Spring rolls back for `RuntimeException` and `Error` and commits for checked exceptions. Use `rollbackFor = Exception.class` on the annotation, or, since Spring Framework 6.2, `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)` for the whole application. The exception must also leave the method: if the method catches it, nothing rolls back, unless an inner participant already marked the transaction rollback-only. Details: [Spring Data and Transactions](19-spring-data-and-transactions.md#rollback-rules).

??? question "What are the transaction isolation levels, and how do you set one in Spring?"

    The four levels are `READ_UNCOMMITTED`, `READ_COMMITTED`, `REPEATABLE_READ`, and `SERIALIZABLE`, and they differ in the anomalies that they allow: dirty reads, non-repeatable reads, and phantom reads. Set the level with `@Transactional(isolation = Isolation.SERIALIZABLE)`. Spring applies it only when the call starts a new transaction, so a method that joins a transaction cannot change it. The database defines the real behavior: PostgreSQL and Oracle default to read committed, and MySQL InnoDB to repeatable read. Details: [Spring Data and Transactions](19-spring-data-and-transactions.md#isolation).

## Spring Web and REST

??? question "Walk me through what happens from the moment an HTTP request arrives until the JSON response leaves."

    The servlet container parses the request and runs the filter chain, and the last part of the chain is the `DispatcherServlet`. It asks the `HandlerMapping` beans for a handler, which matches the path, method, `consumes`, and `produces`. It runs the interceptors' `preHandle`. The handler adapter builds the arguments (`@PathVariable`, `@RequestBody` with Jackson, validation) and calls the method. For a `@RestController`, a message converter chosen by the `Accept` header writes the result. If the method throws, the exception resolvers (`@ExceptionHandler` first) build the response. Details: [Spring Web and REST](20-spring-web-and-rest.md#the-request-flow).

??? question "What is the difference between a servlet filter and a Spring interceptor?"

    A filter belongs to the Servlet specification. It wraps the whole servlet call, sees the raw request, can replace the request or response with a wrapper, and also runs when no Spring handler exists. An interceptor belongs to Spring MVC. It runs inside the `DispatcherServlet` after the handler is chosen, so it can read the handler's annotations, and it does not run for `404` or `405`. Use a filter for rules that apply to every request (authentication, CORS), and an interceptor for rules that depend on the handler. An exception in a filter never reaches `@ControllerAdvice`. Details: [Spring Web and REST](20-spring-web-and-rest.md#filters-versus-interceptors).

??? question "How do you design error handling for a REST API in Spring?"

    Use one format for all errors, built in one place: a `@RestControllerAdvice` that extends `ResponseEntityExceptionHandler`, which maps the framework exceptions to `ProblemDetail` bodies (`application/problem+json`, RFC 9457). Add handlers for domain exceptions with a stable `type` URI. A last handler for `Exception` returns a generic `500` and logs the cause with a correlation ID, so the client never sees a stack trace; it must not catch `AccessDeniedException` from method security, or a `403` becomes a `500`. The advice does not see exceptions from filters, so security errors need an `AuthenticationEntryPoint` and an `AccessDeniedHandler` that produce the same format. Details: [Spring Web and REST](20-spring-web-and-rest.md#exception-handling-and-problemdetail).

??? question "`RestTemplate`, `RestClient`, `WebClient`: which one do you use and why?"

    Use `RestClient` for synchronous calls in new code: a fluent API, the same message converters as MVC, and error handling with `retrieve().onStatus(...)`. Spring Framework 7.0 deprecated `RestTemplate` in its documentation in favor of `RestClient`, plans `@Deprecated(forRemoval = true)` in 7.1, and plans removal in 8.0, so keep it only in existing code. `WebClient` is the non-blocking client for reactive code and streaming, and it needs Reactor. Set connect and read timeouts on all three, because a call without a timeout holds the calling thread. Details: [Spring Web and REST](20-spring-web-and-rest.md#http-clients-restclient-resttemplate-webclient).

??? question "When would you choose WebFlux over Spring MVC, now that Java 21 has virtual threads?"

    WebFlux fits when the stack is non-blocking end to end and the load is many concurrent I/O waits or streams, such as server-sent events, WebSocket, or a gateway. It handles requests on a few event-loop threads, and a blocking call on such a thread stalls others, so JDBC and JPA do not fit. Virtual threads remove the old argument for WebFlux in a CRUD-style service: with `spring.threads.virtual.enabled=true`, a blocking call parks only the virtual thread. So for a new service with blocking JDBC on Java 21, choose MVC with virtual threads, and choose WebFlux for streaming, backpressure, or a fully reactive pipeline. Details: [Spring Web and REST](20-spring-web-and-rest.md#webflux-when-reactive-helps).

## Spring Security

??? question "Explain the architecture of Spring Security. What are `DelegatingFilterProxy`, `FilterChainProxy`, and `SecurityFilterChain`?"

    Spring Security is a chain of servlet filters. The container registers a `DelegatingFilterProxy`, which looks up the Spring bean `springSecurityFilterChain` and delegates to it. That bean is the `FilterChainProxy`. It applies a firewall and then chooses the first `SecurityFilterChain` whose request matcher accepts the request. A `SecurityFilterChain` is a matcher plus an ordered list of security filters. The two layers let one application hold several chains, for example a stateless JWT chain for `/api/**` and a session chain for the web pages. Details: [Spring Security](21-spring-security.md#where-security-sits-three-layers-of-filters).

??? question "What is the difference between `401` and `403` in Spring Security, and which component produces each?"

    `401` means that the caller is not authenticated, and `403` means that the caller is authenticated and lacks permission. `ExceptionTranslationFilter` handles both after `AuthorizationFilter` or method security throws. For an anonymous caller, it calls the `AuthenticationEntryPoint` (a `302` redirect to `/login` for a browser, a `401` with `WWW-Authenticate` for an API). For an authenticated caller, it calls the `AccessDeniedHandler`, which sends `403`. A bad bearer token gets `401` from the token filter even on a `permitAll` URL, because that filter runs before the authorization check. Details: [Spring Security](21-spring-security.md#the-filters-of-a-chain-in-order).

??? question "How do you secure a REST API with JWT in Spring Boot, and do you disable CSRF?"

    Configure `oauth2ResourceServer(o -> o.jwt(...))` in a `SecurityFilterChain` with a stateless session policy, and let a separate authorization server issue the tokens. The `JwtDecoder` verifies the signature, `exp` and `nbf` (with 60 seconds of skew), the `typ` header (absent or `JWT`), and the issuer if you configure an issuer URI, but not `aud`. Add an audience validator through `JwtValidators.createDefaultWithValidators`, because a custom validator list replaces the defaults, and in Spring Security 7 it also drops the `typ` check, which moved from Nimbus into the default validators. Disable CSRF only when no ambient credential authenticates the request: a stateless API that reads a bearer token from the `Authorization` header qualifies, and an application that uses a session cookie does not. Details: [Spring Security](21-spring-security.md#the-oauth2-resource-server-with-jwt).

??? question "How do you store passwords with Spring Security?"

    Expose a `PasswordEncoder` bean, for example `PasswordEncoderFactories.createDelegatingPasswordEncoder()`. It hashes with BCrypt by default and stores the algorithm as a prefix (`{bcrypt}$2a$10$...`), so you can change the algorithm later and old hashes still verify. BCrypt adds a random salt and has a cost factor (10 by default) that you raise as hardware improves. `upgradeEncoding` reports a stored hash that is weaker than the current setting, so re-hash on the next successful login. Never use MD5 or an unsalted SHA. Details: [Spring Security](21-spring-security.md#users-and-passwords).

## Microservices with Spring

??? question "How do you protect a call to a slow downstream service in a Spring application?"

    Use layers, from the inside out. First, connect and read timeouts on the client. Second, a bulkhead or `@ConcurrencyLimit` (Spring Framework 7) to cap concurrent calls. Third, a circuit breaker (Resilience4j) that opens on the failure rate or the slow-call rate, with a fallback. Fourth, a retry with backoff and jitter, only for idempotent calls and at one layer. The annotations work through proxies, so a call from inside the same class skips all of them. Details: [Microservices with Spring](22-microservices-with-spring.md#resilience-resilience4j-and-the-spring-framework-7-core-features).

??? question "Feign or the HTTP interface client: which do you pick for a new service, and why?"

    The HTTP interface client (`@HttpExchange`). Spring Cloud OpenFeign is feature-complete, and its documentation recommends Spring HTTP Service Clients. Since Spring Cloud 5.0, the HTTP service client groups integrate with load balancing and circuit breakers, so the main reason to choose Feign is gone. Recommendation: keep OpenFeign in existing code and migrate with a plan. Details: [Microservices with Spring](22-microservices-with-spring.md#declarative-http-clients-http-service-clients-and-openfeign).

## Build Tools

??? question "Explain the Maven build lifecycle. What does `mvn package` do?"

    Maven has three lifecycles: `clean`, `default`, and `site`. The default lifecycle is an ordered list of phases, and a phase does no work by itself: plugin goals do the work, bound to phases by the packaging type. `mvn package` runs every phase of the default lifecycle up to and including `package`: it compiles, runs the unit tests, and builds the jar in `target/`. It does not install the jar (`install`) or upload it (`deploy`), and a failing test stops the build before `package`. Details: [Build Tools](23-build-tools.md#maven-lifecycle-phases-plugins-and-goals).

??? question "What are Maven dependency scopes, and how does `provided` differ from `runtime`?"

    The scope decides on which class paths a dependency appears and whether it is transitive. `compile` (the default) is on all class paths and is transitive. `provided` is on the compile and test class paths but not at run time, and it is not transitive, because the runtime environment supplies it (the servlet API in a servlet container). `runtime` is not needed to compile but is on the test and runtime class paths (a JDBC driver). `test` is only on the test class paths, and `import` is only for a BOM in `dependencyManagement`. Details: [Build Tools](23-build-tools.md#dependency-scopes).

??? question "How does Maven choose a version when two libraries need different versions of a third library?"

    Maven uses "nearest definition wins": it picks the version at the shortest path from the project in the dependency graph, and on a tie the first declaration in the POM wins. It does not pick the highest version, so it can choose a version that is too old for one library, which gives `NoSuchMethodError` at run time. Control it by declaring the artifact directly or setting its version in `dependencyManagement`. Diagnose it with `mvn dependency:tree -Dverbose`. Details: [Build Tools](23-build-tools.md#transitive-resolution-and-nearest-wins).
