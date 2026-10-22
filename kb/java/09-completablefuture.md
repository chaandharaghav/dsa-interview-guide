---
priority: P1
---
# CompletableFuture: Asynchronous Pipelines

A `CompletableFuture<T>` is a `Future<T>` that you can complete yourself and that lets you attach steps to run when the result is ready. A *stage* is one step of such a pipeline. This page explains how to create stages, how to compose them, which thread runs each stage, how failures travel through a pipeline, how to set timeouts, and which mistakes cost the most in production. It builds on the executors in [java.util.concurrent](08-java-util-concurrent.md) and on the memory model in [Concurrency Basics](07-concurrency-basics.md). The last section compares `CompletableFuture` with virtual threads ([Virtual Threads](10-virtual-threads.md)).

## Why It Matters

Backend services call other services, and a request often needs the results of several independent calls. Interviewers ask: "How do you call three services in parallel and combine the results?", "What is the difference between `thenApply` and `thenCompose`?", "Which thread runs this callback?", "What happens to an exception in a stage?", and since Java 21, "Do virtual threads make `CompletableFuture` obsolete?". On the job, `CompletableFuture` code fails in quiet ways: an exception that nobody observes, a pipeline that blocks the common pool for the whole JVM, a timeout that fires and leaves the work running. A candidate who can name the thread, the wrapper exception, and the cancellation limit shows real experience. Every example below has deterministic output.

## Core Concepts

**Stage**: one step of an asynchronous computation. A stage completes with a value, with an exception, or by cancellation. The interface `CompletionStage<T>` defines the methods that attach and combine stages. `CompletableFuture<T>` implements both `Future<T>` and `CompletionStage<T>`.

**Source stage and dependent stage**: if you call `a.thenApply(f)`, then `a` is the source stage and the returned stage is the dependent stage. The dependent stage starts when the source completes.

**Completing a stage**: a `CompletableFuture` has an outcome field that is empty until the stage completes. `complete(value)` and `completeExceptionally(ex)` set it, once. The first call wins, and later calls return `false`. This is the "completable" in the name.

**Async variant**: for most methods there are three forms: `thenApply`, `thenApplyAsync(fn)`, and `thenApplyAsync(fn, executor)`. The async forms submit the function to an executor. The plain form runs it in the thread that triggers the stage. Section "Which thread runs a stage" has the rules.

**Default executor**: the executor that async methods use when you pass none. In the JDK 21 source it is `ForkJoinPool.commonPool()`, unless the common-pool parallelism is 1 or less (see "Creating a stage"). Subclasses can override `defaultExecutor()`.

**`CompletionException`**: an unchecked exception that wraps the failure of a stage when it passes to a dependent stage or to `join()`.

**Pipeline**: a chain or graph of stages.

## How It Works

### Why `CompletableFuture`

A plain `Future` (see [java.util.concurrent](08-java-util-concurrent.md)) has three limits. You can only block on `get()`, so a thread waits for each result. You cannot attach a callback. You cannot combine two futures without blocking a thread for each. `CompletableFuture` removes these limits: you describe what happens next, and the thread that finishes the work triggers the next step. A pipeline of ten steps needs no thread that waits.

**How it works inside (JDK 21 source comments).** A `CompletableFuture` has two `volatile` fields. The field `result` is empty until the stage completes. It holds the value, or a boxed `AltResult` for `null` and for exceptions. The field `stack` holds the dependent actions as a lock-free linked stack (a Treiber stack). When you attach a dependent stage to a source that is not complete, the library pushes a `Completion` object on the source's stack. When the source completes, it sets `result` with a CAS and then pops and runs the dependent actions. If the source is already complete when you attach the stage, the library runs the action at once. No lock is involved. Each `Completion` extends `ForkJoinTask`, so the library can run it on an executor.

### Creating a stage

| Method | Result |
|---|---|
| `CompletableFuture.supplyAsync(Supplier<U>)` | Runs the supplier on the default executor and completes with its value |
| `supplyAsync(Supplier<U>, Executor)` | The same, on your executor |
| `runAsync(Runnable)` and `runAsync(Runnable, Executor)` | Like `supplyAsync`, with no value (`CompletableFuture<Void>`) |
| `completedFuture(value)` | A stage that is already complete (also `completedStage`, Java 9) |
| `failedFuture(ex)` | A stage that is already failed (Java 9) |
| `new CompletableFuture<T>()` | An open stage that you complete later with `complete` or `completeExceptionally`. Use it to adapt a callback API |
| `completeAsync(Supplier)` (Java 9) | Completes an open stage with the value of a supplier that runs on an executor |

**The default executor and when to pass your own.** In JDK 21, `supplyAsync(supplier)` runs on `ForkJoinPool.commonPool()`. The common pool has a default parallelism (target number of active threads) of `availableProcessors() - 1`, at least 1, and its worker threads are daemon threads (source: `ForkJoinPool` private constructor and `ForkJoinWorkerThread`). Two facts follow:

- In JDK 21 to 24, if the parallelism of the common pool is 1 or less (a machine with 1 or 2 processors, or a property that sets it to 0 or 1), `CompletableFuture` does not use the common pool. It starts a new thread for each async task (source: `USE_COMMON_POOL` and `ThreadPerTaskExecutor`). JDK 25 removed `ThreadPerTaskExecutor`: the default executor is always the common pool, and if the common-pool parallelism is 0, the library raises it to 2 (JDK 25 source: `ForkJoinPool.asyncCommonPool`).
- The common pool is shared with parallel streams and every other user in the JVM. It is sized for CPU-bound work. A blocking call in a stage holds one of few threads. A daemon thread does not keep the JVM alive, so a `main` method that returns before the stages finish can end the program with the work undone.

Recommendation: pass an explicit executor to every `supplyAsync`, `runAsync`, and `...Async` call that does I/O or runs long. Use a bounded pool that you sized for the work ([java.util.concurrent](08-java-util-concurrent.md), "Sizing a pool"), or a virtual-thread executor on Java 21. Use the default only for short, CPU-bound functions.

### Composing stages

| Method | Input | The function | Result stage |
|---|---|---|---|
| `thenApply(fn)` | One value | `T -> U` | `CompletableFuture<U>` (map) |
| `thenAccept(consumer)` | One value | `T -> void` | `CompletableFuture<Void>` |
| `thenRun(runnable)` | None | `() -> void` | `CompletableFuture<Void>` |
| `thenCompose(fn)` | One value | `T -> CompletionStage<U>` | `CompletableFuture<U>` (flatMap) |
| `thenCombine(other, fn)` | Two values | `(T, U) -> V` | `CompletableFuture<V>`. The two sources run independently |
| `thenAcceptBoth`, `runAfterBoth` | Two stages | consumer, runnable | After both complete |
| `applyToEither`, `acceptEither`, `runAfterEither` | Two stages | function over the first result | After the first of the two completes |
| `allOf(cfs...)` | Many stages | none | `CompletableFuture<Void>`. Completes when all complete |
| `anyOf(cfs...)` | Many stages | none | `CompletableFuture<Object>`. Completes when the first completes |

**`thenApply` versus `thenCompose`.** `thenApply` takes a function that returns a plain value. If the function returns a `CompletableFuture`, the result is a nested `CompletableFuture<CompletableFuture<U>>`. `thenCompose` takes a function that returns a stage and flattens it. Use `thenApply` for a synchronous transformation and `thenCompose` for a next step that is itself asynchronous (a second remote call that needs the result of the first). This is the same difference as `map` and `flatMap` in streams and `Optional`.

**`thenCombine`** joins two independent stages. Start both stages first, and then combine them, so that they run in parallel. If you write `a.thenCompose(x -> b())`, `b` starts only after `a` completes, so the two calls run one after the other.

**`allOf` and `anyOf`.**

- `allOf` returns `CompletableFuture<Void>`. It carries no results. After it completes, read each source with `join()` (which does not block then). If any source fails, `allOf` completes exceptionally after *all* sources complete, with a `CompletionException` whose cause is one of the failures (javadoc). It does not cancel the other stages.
- `anyOf` returns `CompletableFuture<Object>`, so you cast the result. It completes with the outcome of the *first* source that completes, even if that outcome is an exception (javadoc). With no arguments, it never completes.

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CfCompose {
    record User(int id, String name) {}

    static CompletableFuture<User> findUser(int id, ExecutorService io) {
        return CompletableFuture.supplyAsync(() -> new User(id, "user" + id), io);
    }

    static CompletableFuture<Integer> orderCount(User u, ExecutorService io) {
        return CompletableFuture.supplyAsync(() -> u.id() * 10, io);
    }

    public static void main(String[] args) throws Exception {
        ExecutorService io = Executors.newFixedThreadPool(4);
        try {
            // thenApply: a function from value to value (map)
            CompletableFuture<String> upper =
                    findUser(1, io).thenApply(u -> u.name().toUpperCase());
            System.out.println("thenApply: " + upper.join());

            // thenApply with a function that returns a stage gives a nested stage
            CompletableFuture<CompletableFuture<Integer>> nested =
                    findUser(2, io).thenApply(u -> orderCount(u, io));
            System.out.println("thenApply nests: " + nested.join().join());

            // thenCompose: a function from value to stage (flatMap)
            CompletableFuture<Integer> flat = findUser(2, io).thenCompose(u -> orderCount(u, io));
            System.out.println("thenCompose: " + flat.join());

            // thenCombine: two independent stages, one function over both results
            CompletableFuture<String> both = findUser(3, io).thenCombine(
                    findUser(4, io), (a, b) -> a.name() + "+" + b.name());
            System.out.println("thenCombine: " + both.join());

            // thenAccept and thenRun end a chain with no result
            CompletableFuture<Void> end = findUser(5, io).thenAccept(u -> { });
            System.out.println("thenAccept result: " + end.join());

            // allOf: wait for all, then read each result
            List<CompletableFuture<User>> users = List.of(
                    findUser(6, io), findUser(7, io), findUser(8, io));
            CompletableFuture<Void> all = CompletableFuture.allOf(
                    users.toArray(new CompletableFuture[0]));
            List<String> names = all.thenApply(v -> users.stream()
                    .map(CompletableFuture::join).map(User::name).toList()).join();
            System.out.println("allOf: " + names);

            // anyOf: the first stage that completes. Here only one is complete.
            CompletableFuture<String> never = new CompletableFuture<>();
            CompletableFuture<String> ready = CompletableFuture.completedFuture("ready");
            Object first = CompletableFuture.anyOf(never, ready).join();
            System.out.println("anyOf: " + first);
        } finally {
            io.shutdown();
        }
    }
}
```

<!-- expected-output -->
```text
thenApply: USER1
thenApply nests: 20
thenCompose: 20
thenCombine: user3+user4
thenAccept result: null
allOf: [user6, user7, user8]
anyOf: ready
```

Reading the output: `thenApply` returns the transformed value. `thenApply` with a function that returns a stage gives a nested stage, and `thenCompose` flattens it to the same value 20. `thenCombine` gets both users. `thenAccept` yields a `Void` stage whose result is `null`. `allOf` gives the three names after all stages complete. `anyOf` returns "ready", because only that stage is complete.

### Which thread runs a stage

The `CompletableFuture` javadoc gives the rules. An action of a non-async method (`thenApply`) "may be performed by the thread that completes the current `CompletableFuture`, or by any other caller of a completion method". The JDK 21 behavior, which the demo below shows:

| Situation | Thread that runs the function |
|---|---|
| Source already complete when you call `thenApply` | The thread that calls `thenApply` |
| Source not yet complete | The thread that completes the source (often an executor thread, and for a timeout the delay-scheduler thread) |
| `thenApplyAsync(fn)` | The default executor (common pool) |
| `thenApplyAsync(fn, executor)` | Your executor |

The first two rows mean that the thread depends on a race between the registration and the completion. The code works the same either way, but the thread is not fixed. Do not assume that a non-async callback runs on the thread you started the pipeline in, or on a pool thread.

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CfThreads {
    static final String PREFIX = "my-pool-";

    static String where(Thread caller) {
        Thread t = Thread.currentThread();
        if (t == caller) {
            return "caller thread";
        } else if (t.getName().startsWith(PREFIX)) {
            return "my executor";
        } else if (t instanceof ForkJoinWorkerThread) {
            return "common pool";
        }
        return "other";
    }

    public static void main(String[] args) throws Exception {
        AtomicInteger n = new AtomicInteger();
        ThreadFactory tf = r -> new Thread(r, PREFIX + n.incrementAndGet());
        ExecutorService mine = Executors.newFixedThreadPool(2, tf);
        Thread main = Thread.currentThread();
        try {
            // 1. The source stage is already complete: thenApply runs in the calling thread
            CompletableFuture<String> done = CompletableFuture.completedFuture("x");
            System.out.println("1 completed + thenApply:  "
                    + done.thenApply(v -> where(main)).join());

            // 2. Stage not complete yet: thenApply runs in the thread that completes the source
            CountDownLatch gate = new CountDownLatch(1);
            CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
                try {
                    gate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "x";
            }, mine);
            CompletableFuture<String> dependent = slow.thenApply(v -> where(main));
            gate.countDown();
            System.out.println("2 pending + thenApply:    " + dependent.join());

            // 3. thenApplyAsync with no executor: the default pool, whatever completed the source
            System.out.println("3 thenApplyAsync:         "
                    + done.thenApplyAsync(v -> where(main)).join());

            // 4. thenApplyAsync with an executor: that executor
            System.out.println("4 thenApplyAsync(mine):   "
                    + done.thenApplyAsync(v -> where(main), mine).join());

            // 5. supplyAsync with no executor
            System.out.println("5 supplyAsync:            "
                    + CompletableFuture.supplyAsync(() -> where(main)).join());
        } finally {
            mine.shutdown();
        }
    }
}
```

<!-- expected-output -->
```text
1 completed + thenApply:  caller thread
2 pending + thenApply:    my executor
3 thenApplyAsync:         common pool
4 thenApplyAsync(mine):   my executor
5 supplyAsync:            common pool
```

Reading the output: case 1 runs in the caller, because the source is complete. Case 2 runs in the pool thread that completes the source (the gate opens after the callback is registered). Cases 3 and 5 use the common pool. Case 4 uses the executor that you pass. On JDK 21 to 24, a machine whose common-pool parallelism is 1 or less prints "other" for cases 3 and 5, because the default executor then starts a new plain thread for each task.

Recommendation: choose by cost of the function. A short, non-blocking function (a field copy, a mapping) belongs in a plain `thenApply`: it avoids a thread hand-off. A function that blocks or computes for a long time belongs in an `...Async` method with your executor, so it does not run on the thread that completes the source, which can be a thread of a different pool, an I/O event-loop thread, or the delay-scheduler thread.

Also note that the method `thenApplyAsync(fn)` with no executor always uses the default executor, even if the source ran on your executor. The stage does not inherit the executor of its source. Pass the executor again at each step.

### Error handling

When a stage fails, the failure passes down the pipeline. Each dependent stage that has a normal-result function (`thenApply`, `thenAccept`, `thenCompose`, `thenCombine`) is skipped and completes with the same failure. A handler stage sees the failure.

**Wrapping.** The failure of a stage is stored as a `CompletionException` that has the original exception as its cause, when the failure passes to a dependent stage or comes from an exception that a function threw (JDK 21 source: `encodeThrowable` wraps in `CompletionException`). The exception that `join()` and the handlers see is therefore a `CompletionException`. `get()` throws a checked `ExecutionException` with the original cause. `join()` throws the unchecked `CompletionException` (or `CancellationException` for a cancelled stage). The exception to this rule: if you complete a stage directly with `completeExceptionally(ex)`, the handlers on *that* stage see the raw `ex`, and the handlers on dependent stages see the wrapped form.

| Method | Called when | Receives | Returns | Can change the outcome? |
|---|---|---|---|---|
| `exceptionally(fn)` | Only on failure | The exception | A replacement value | Yes, a failure becomes a value |
| `handle((v, t) -> r)` | Always | The value or the exception (one is `null`) | A new value | Yes, both outcomes |
| `whenComplete((v, t) -> ...)` | Always | The value or the exception | Nothing | No. The original outcome continues. If the action itself throws and the stage was successful, the stage fails with that exception |
| `exceptionallyCompose(fn)` (Java 12) | Only on failure | The exception | A stage | Yes. Use it to retry or to switch to a fallback call |
| `exceptionallyAsync(fn)` (Java 12) | Only on failure | The exception | A replacement value | Yes, and the function runs on an executor |

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class CfErrors {
    static CompletableFuture<Integer> failing() {
        return CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("db down");
        });
    }

    static String describe(Throwable t) {
        Throwable c = t.getCause();
        String own = t.getClass().getSimpleName();
        return c == null ? own : own + " -> " + c.getClass().getSimpleName()
                + "(" + c.getMessage() + ")";
    }

    static Integer log(String label, Throwable t, int replacement) {
        System.out.println("  " + label + " saw " + describe(t));
        return replacement;
    }

    public static void main(String[] args) throws Exception {
        // join wraps the failure in CompletionException. get wraps it in ExecutionException.
        try {
            failing().join();
        } catch (CompletionException e) {
            System.out.println("join: " + describe(e));
        }
        try {
            failing().get();
        } catch (ExecutionException e) {
            System.out.println("get:  " + describe(e));
        }

        // exceptionally on the stage whose function threw: it receives the CompletionException
        int fixed = failing().exceptionally(t -> log("exceptionally", t, -1)).join();
        System.out.println("exceptionally returned " + fixed);

        // completeExceptionally stores the raw exception. A dependent stage gets it wrapped.
        CompletableFuture<Integer> manual = new CompletableFuture<>();
        manual.completeExceptionally(new IllegalArgumentException("bad input"));
        manual.exceptionally(t -> log("manual stage", t, 0));
        manual.thenApply(v -> v + 1).exceptionally(t -> log("dependent stage", t, 0));

        // A failed stage skips thenApply and passes the failure on
        CompletableFuture<Integer> chain = failing().thenApply(v -> v + 1).thenApply(v -> v * 2);
        System.out.println("chain failed: " + chain.handle((v, t) -> t != null).join());

        // handle: sees the value and the exception, and can replace both
        System.out.println("handle ok:   " + CompletableFuture.completedFuture(5)
                .handle((v, t) -> t == null ? "value " + v : "recovered").join());
        System.out.println("handle fail: " + failing()
                .handle((v, t) -> t == null ? "value " + v : "recovered").join());

        // whenComplete: observes the outcome and does not change it
        try {
            failing().whenComplete((v, t) -> log("whenComplete", t, 0)).join();
        } catch (CompletionException e) {
            System.out.println("still failed after whenComplete: " + describe(e));
        }

        // exceptionallyCompose (Java 12): recover with another stage
        System.out.println("exceptionallyCompose: " + failing()
                .exceptionallyCompose(t -> CompletableFuture.completedFuture(99)).join());

        // Unwrap to get the real cause
        Throwable seen = failing().handle((v, t) -> t).join();
        Throwable cause = seen instanceof CompletionException && seen.getCause() != null
                ? seen.getCause() : seen;
        System.out.println("unwrapped: " + describe(cause));
    }
}
```

<!-- expected-output -->
```text
join: CompletionException -> IllegalStateException(db down)
get:  ExecutionException -> IllegalStateException(db down)
  exceptionally saw CompletionException -> IllegalStateException(db down)
exceptionally returned -1
  manual stage saw IllegalArgumentException
  dependent stage saw CompletionException -> IllegalArgumentException(bad input)
chain failed: true
handle ok:   value 5
handle fail: recovered
  whenComplete saw CompletionException -> IllegalStateException(db down)
still failed after whenComplete: CompletionException -> IllegalStateException(db down)
exceptionallyCompose: 99
unwrapped: IllegalStateException
```

Reading the output: `join()` throws `CompletionException` and `get()` throws `ExecutionException`, both caused by the original `IllegalStateException`. `exceptionally` on the failed stage receives the `CompletionException` too. A stage completed with `completeExceptionally` shows the raw exception to its own handler and the wrapped form to a dependent stage. A failed stage skips the later `thenApply` calls. `handle` converts a failure into "recovered". `whenComplete` observes the failure and the stage still fails afterwards. `exceptionallyCompose` replaces the failure with another stage.

**Unwrapping.** Code that tests the exception type must unwrap first, or the test fails:

```java
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

class Unwrap {
    static Throwable rootOf(Throwable t) {
        // The wrappers hold the real failure as their cause
        while ((t instanceof CompletionException || t instanceof ExecutionException)
                && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
```

**Unobserved failures.** A stage that fails with no handler and with nobody who calls `join()` or `get()` reports nothing. The JVM prints no stack trace and the failure disappears. Always end a pipeline with a handler (`handle`, `exceptionally`, or `whenComplete` that logs) or return the stage to a caller that observes it. The `CfMistakes` demo below shows this.

### Timeouts

A `Future.get(timeout)` throws `TimeoutException` in the waiting thread. It does not change the stage. Java 9 added timeouts to the stage itself:

| Method | Behavior |
|---|---|
| `orTimeout(time, unit)` | If the stage is not complete after the time, completes it exceptionally with `TimeoutException`. Returns the same stage |
| `completeOnTimeout(value, time, unit)` | If the stage is not complete after the time, completes it with `value` |
| `delayedExecutor(delay, unit[, executor])` | An executor that runs a task after a delay. Use it to build a delayed or retried stage |

In JDK 21, both timeout methods schedule a task on a single shared daemon thread named `CompletableFutureDelayScheduler` (source: `Delayer`, a `ScheduledThreadPoolExecutor` with one thread). When the timeout fires, that thread completes the stage. A non-async dependent stage that is registered before then runs on the same thread (the demo shows this). A blocking function in such a stage delays every other timeout in the JVM. Add `thenApplyAsync(fn, executor)` or use an `...Async` handler after a timeout. If the stage completes in time, the library cancels the scheduled task.

JDK 25 changed the mechanism (JDK-8319447, "Improve performance of delayed task handling"). `Delayer` is gone. `ForkJoinPool` now implements `ScheduledExecutorService`, and the timeouts run on the delay-scheduler thread of the common pool, named `ForkJoinPool.commonPool-delayScheduler` (JDK 25 source: `arrangeTimeout` and `DelayScheduler`). The timeout task is an "immediate" task, so the scheduler thread itself completes the stage, and the rule above still holds: a non-async dependent runs on that one shared thread.

A timeout completes the *stage*. It does not stop the work behind it. The supplier that is still running keeps its thread until it ends.

### Cancellation

`cancel(mayInterruptIfRunning)` completes the stage with a `CancellationException` if the stage is not yet complete, and returns `true` if the stage is now cancelled (javadoc: "true if this task is now cancelled"). It returns `false` if the stage completed in another way first. It does *not* interrupt the thread that runs the function: the javadoc says that the argument has no effect, because the class does not use interrupts to control its processing. The function runs to the end, and its result is discarded. A dependent stage fails with a `CompletionException` caused by the `CancellationException`.

```java
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CfTimeouts {
    public static void main(String[] args) throws Exception {
        // orTimeout: completes the stage with TimeoutException if it is still open after 50 ms
        CompletableFuture<String> slow =
                new CompletableFuture<String>().orTimeout(50, TimeUnit.MILLISECONDS);
        try {
            slow.join();
        } catch (CompletionException e) {
            System.out.println("orTimeout: join throws CompletionException caused by "
                    + e.getCause().getClass().getSimpleName());
        }
        try {
            slow.get();
        } catch (ExecutionException e) {
            System.out.println("orTimeout: get throws ExecutionException caused by "
                    + e.getCause().getClass().getSimpleName());
        }

        // The timeout fires on one shared scheduler thread. A dependent stage that is registered
        // before the timeout runs on that thread too. Register the dependent first, then arm the
        // timeout, so that the dependent is always registered before the timeout fires.
        CompletableFuture<String> timed = new CompletableFuture<>();
        CompletableFuture<Boolean> onSchedulerStage = timed.handle((v, t) ->
                Thread.currentThread().getName().equals("CompletableFutureDelayScheduler"));
        timed.orTimeout(50, TimeUnit.MILLISECONDS);
        boolean onScheduler = onSchedulerStage.join();
        System.out.println("dependent of a timed-out stage ran on the delay scheduler: "
                + onScheduler);

        // completeOnTimeout: a fallback value
        String fallback = new CompletableFuture<String>()
                .completeOnTimeout("default", 50, TimeUnit.MILLISECONDS).join();
        System.out.println("completeOnTimeout: " + fallback);

        // A stage that finishes in time is not affected
        System.out.println("fast stage with orTimeout: "
                + CompletableFuture.completedFuture("fast").orTimeout(1, TimeUnit.SECONDS).join());

        // get(timeout) throws TimeoutException but does not complete the stage
        CompletableFuture<String> open = new CompletableFuture<>();
        try {
            open.get(50, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("get(50 ms): TimeoutException, stage done? " + open.isDone());
        }

        // Cancellation completes the stage. It does not stop the running function.
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean ranToEnd = new AtomicBoolean();
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
            ranToEnd.set(true);
            finished.countDown();
            return "result";
        });
        started.await();
        CompletableFuture<String> dependent = task.thenApply(String::toUpperCase);
        System.out.println("cancel(true) returned: " + task.cancel(true));
        System.out.println("task cancelled: " + task.isCancelled());
        try {
            dependent.join();
        } catch (CompletionException e) {
            System.out.println("dependent stage: " + e.getCause().getClass().getSimpleName()
                    + ", isCancelled=" + dependent.isCancelled());
        } catch (CancellationException e) {
            System.out.println("dependent: CancellationException");
        }
        release.countDown();
        finished.await();
        System.out.println("function was interrupted: " + interrupted.get()
                + ", function ran to the end: " + ranToEnd.get());
    }
}
```

<!-- expected-output -->
```text
orTimeout: join throws CompletionException caused by TimeoutException
orTimeout: get throws ExecutionException caused by TimeoutException
dependent of a timed-out stage ran on the delay scheduler: true
completeOnTimeout: default
fast stage with orTimeout: fast
get(50 ms): TimeoutException, stage done? false
cancel(true) returned: true
task cancelled: true
dependent stage: CancellationException, isCancelled=false
function was interrupted: false, function ran to the end: true
```

Reading the output: `orTimeout` produces a `TimeoutException`, wrapped as in the table above. A timeout stage's dependent runs on the delay scheduler thread. (On JDK 25 the third line prints `false`, because the scheduler thread there is named `ForkJoinPool.commonPool-delayScheduler`.) `completeOnTimeout` supplies the default value, and a fast stage is not affected. `get(50 ms)` throws `TimeoutException` but leaves the stage open. After `cancel(true)`, the task stage is cancelled, the dependent stage has a `CancellationException` as cause (and is not itself cancelled), the function was not interrupted, and it ran to the end.

To stop work for real, make the function check a flag or an interrupt that you control. Keep the `Future` that the executor returns (`ExecutorService.submit`) and complete a `CompletableFuture` from it if you need `cancel(true)` to interrupt the thread. Alternatively, use structured concurrency (preview), which cancels sibling tasks when one fails ([Virtual Threads](10-virtual-threads.md)).

### Common mistakes

#### Blocking inside a stage

A stage that calls `join()` or `get()` on another stage holds its thread while it waits. When a common-pool worker calls `join()` or `get()`, the call goes through `ForkJoinPool.managedBlock`, so the pool may start a spare thread to compensate (source: `Signaller`). The compensation has a limit (256 spare threads by default for the common pool, `DEFAULT_COMMON_MAX_SPARES`). On a plain `ThreadPoolExecutor`, nothing compensates. If a task blocks on a stage that needs a thread of the same bounded pool, all threads can wait and none is left to run the inner stage: a *pool-starvation deadlock*.

A stuck `join()` is hard to undo. `join()` ignores interrupts: the JDK 21 source (`waitingGet(false)`) records the interrupt and parks the thread again. So `shutdownNow()` cannot free the thread, and `close()` waits for it without a limit. If the pool threads are not daemon threads, the JVM never exits. The demo uses a pool of one thread. A latch makes sure that the inner task is queued before `main` measures, and `get` with a 200 ms limit shows that the outer stage is stuck. Then `main` takes the queued inner task out of the pool and runs it, which frees the stuck thread so that the program ends. The second part shows the fix with `thenCompose`.

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CfMistakes {
    public static void main(String[] args) throws Exception {
        // Mistake 1: a task blocks on a stage that needs a thread of the same bounded pool
        ExecutorService one = Executors.newFixedThreadPool(1);
        CountDownLatch innerQueued = new CountDownLatch(1);
        CompletableFuture<Integer> outer = CompletableFuture.supplyAsync(() -> {
            CompletableFuture<Integer> inner = CompletableFuture.supplyAsync(() -> 42, one);
            innerQueued.countDown();          // inner waits in the queue: the only thread is busy
            return inner.join() + 1;          // the only thread waits for a task that cannot start
        }, one);
        innerQueued.await();
        try {
            outer.get(200, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("outer is stuck: done=" + outer.isDone());
        }
        // Release the stuck thread so that the program can end: take the queued inner task
        // out of the pool and run it here. join() ignores interrupts, so shutdownNow() alone
        // cannot free the thread.
        List<Runnable> queued = one.shutdownNow();
        queued.forEach(Runnable::run);
        boolean terminated = one.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("queued tasks: " + queued.size() + ", outer after release: "
                + outer.join() + ", pool terminated: " + terminated);

        // The fix: compose, do not block
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            CompletableFuture<Integer> composed = CompletableFuture.supplyAsync(() -> 0, pool)
                    .thenCompose(x -> CompletableFuture.supplyAsync(() -> 42, pool))
                    .thenApply(v -> v + 1);
            System.out.println("composed: " + composed.join());
        } finally {
            pool.shutdown();
        }

        // Mistake 2: nobody observes the failure, so nothing reports it
        CompletableFuture<Void> lost = CompletableFuture.runAsync(() -> {
            throw new IllegalStateException("nobody will see this");
        });
        lost.handle((v, t) -> null).join();       // waits, and does not observe the failure
        System.out.println("lost stage failed: " + lost.isCompletedExceptionally()
                + ", and the JVM printed nothing");
    }
}
```

<!-- expected-output -->
```text
outer is stuck: done=false
queued tasks: 1, outer after release: 43, pool terminated: true
composed: 43
lost stage failed: true, and the JVM printed nothing
```

Reading the output: the outer stage does not complete within 200 ms, because the only thread waits for a task that waits in the queue for that thread. `shutdownNow()` returns the one queued task (the inner stage). When `main` runs it, the inner stage completes with 42, the stuck thread wakes, the outer stage completes with 43, and the pool terminates. In production nobody runs the queued task by hand, so the thread stays stuck. The composed version completes with 43, because it holds no thread while it waits. The last line shows a stage that failed and reported nothing: the program printed no stack trace.

The section "Common Mistakes" below lists the other mistakes, each with its fix.

### `CompletableFuture` and virtual threads

A virtual thread (JEP 444, final in Java 21) is a thread that the JVM schedules and that costs little memory. A virtual thread that blocks on I/O releases its carrier thread. Blocking code on virtual threads reaches the throughput that asynchronous code reaches, and it reads like sequential code. See [Virtual Threads](10-virtual-threads.md).

| Question | `CompletableFuture` | Virtual threads with blocking code |
|---|---|---|
| Style | Callbacks and composition | Sequential code, one thread for each task |
| Waiting threads | None: the pipeline holds no thread while it waits | A virtual thread waits, and it costs little |
| Stack traces | The stack shows the executor and library frames, not the chain of stages | The stack shows the call chain |
| Error handling | `handle`, `exceptionally`, wrapper exceptions | `try` and `catch` |
| Timeouts | `orTimeout`, `completeOnTimeout` | `Future.get(timeout)`, structured concurrency (preview) |
| Parallel fan-out and combine | `allOf`, `thenCombine` | Submit tasks, `get()` each |
| Thread-pool tuning | You must size the pool for the blocking | No sizing for blocking tasks |

The two work together. A virtual-thread executor is an ordinary `Executor`, so `supplyAsync(supplier, virtualExecutor)` runs each stage on a virtual thread. You keep the composition and drop the pool sizing. The demo below runs a fan-out both ways.

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class CfVirtual {
    static String fetch(int id) {                       // pretend this blocks on I/O
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "item" + id;
    }

    public static void main(String[] args) {
        // CompletableFuture with a virtual-thread executor: the stages stay, the pool goes away
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, 5)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> fetch(i), vt)).toList();
            System.out.println(futures.stream().map(CompletableFuture::join).toList());
            CompletableFuture<Boolean> isVirtual =
                    CompletableFuture.supplyAsync(() -> Thread.currentThread().isVirtual(), vt);
            System.out.println("runs on a virtual thread: " + isVirtual.join());
        }

        // The same fan-out in plain blocking style on virtual threads
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<String>> fs = IntStream.rangeClosed(1, 5)
                    .mapToObj(i -> vt.submit(() -> fetch(i))).toList();
            StringBuilder sb = new StringBuilder();
            for (var f : fs) {
                try {
                    sb.append(f.get()).append(' ');
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println(sb.toString().trim());
        }
    }
}
```

<!-- expected-output -->
```text
[item1, item2, item3, item4, item5]
runs on a virtual thread: true
item1 item2 item3 item4 item5
```

Reading the output: the first block collects five results from stages that run on virtual threads, and the check prints `true`. The second block gives the same values with plain `submit` and `get` calls on virtual threads and no composition.

When `CompletableFuture` still helps: you need composition of several asynchronous results (`allOf`, `thenCombine`) with timeouts and fallbacks; an API already returns `CompletionStage` (for example `HttpClient.sendAsync`); or you adapt a callback API through `new CompletableFuture<>()` and `complete`. When virtual threads fit better: the code is sequential, blocking, and I/O-bound, and the main goal is a simple thread-per-request model. CPU-bound work gains nothing from virtual threads and needs a pool sized to the cores.

### Java 22 and later

No public `CompletableFuture` method was added after Java 21. The newest `@since` tags in the JDK 25 source and in the current mainline source are `@since 19` (`resultNow`, `exceptionNow`, `state`). The implementation and the platform around it changed:

| Version | Change | Source |
|---|---|---|
| Java 24 | JEP 491: a virtual thread can unmount while it blocks inside `synchronized`. This affects blocking code on a virtual-thread executor | JEP 491 |
| Java 25 | The default executor is always the common pool. `ThreadPerTaskExecutor` is gone, and a common-pool parallelism of 0 is raised to 2 | JDK 25 source, `ASYNC_POOL = ForkJoinPool.asyncCommonPool()` |
| Java 25 | `orTimeout`, `completeOnTimeout`, and `delayedExecutor` use the delay scheduler of the common pool (thread `ForkJoinPool.commonPool-delayScheduler`) instead of the `CompletableFutureDelayScheduler` thread | JDK-8319447, JDK 25 source |

Structured concurrency, a preview API that also composes concurrent subtasks, is still a preview feature in JDK 27. [Virtual Threads](10-virtual-threads.md) gives its status in each release.

## Trade-offs and When to Use

| Option | Use it when | Cost |
|---|---|---|
| `CompletableFuture` on a bounded executor | Several asynchronous results must be combined, with timeouts and fallbacks | Callback style, harder stack traces, you manage the executor |
| `ExecutorService.submit` and `Future.get` | A few independent tasks, and blocking the caller is acceptable | The caller blocks for each result. No composition |
| `parallelStream` | CPU-bound work on a collection | Uses the common pool. Poor for blocking calls ([Functional Programming and Streams](12-functional-and-streams.md)) |
| Virtual threads with blocking code | I/O-bound request handling with sequential logic | CPU-bound work gains nothing. Pinning on Java 21 with `synchronized` |
| Reactive streams (Reactor, RxJava) | Streams of many values with back-pressure | A new programming model and a learning cost. Out of scope here |

Recommendation: on Java 21, write new I/O-bound request code as blocking code on virtual threads, and use `CompletableFuture` where you must compose independent calls or where an API requires a stage. In older code bases, keep `CompletableFuture`, give each call an explicit executor, and end each pipeline with a handler.

## Java Example

The example asks three shops for a price and picks the lowest. Shop A answers. Shop B fails. Shop C hangs and never answers. The pipeline turns the failure of B and the timeout of C into "no offer" values, so the combination always completes, and it picks A. The pool uses daemon threads, so the hanging task of shop C does not keep the JVM alive.

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CfPrices {
    record Quote(String shop, int price) {}

    static ExecutorService newDaemonPool(int n) {
        return Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public static void main(String[] args) {
        ExecutorService io = newDaemonPool(4);
        CountDownLatch neverReleased = new CountDownLatch(1);    // models a shop that hangs
        try {
            CompletableFuture<Quote> a =
                    CompletableFuture.supplyAsync(() -> new Quote("A", 120), io);

            CompletableFuture<Quote> b = CompletableFuture.<Quote>supplyAsync(() -> {
                throw new IllegalStateException("shop B is down");
            }, io).exceptionally(t -> new Quote("B", Integer.MAX_VALUE));  // failed: never wins

            CompletableFuture<Quote> c = CompletableFuture.supplyAsync(() -> {
                try {
                    neverReleased.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new Quote("C", 90);
            }, io).completeOnTimeout(new Quote("C", Integer.MAX_VALUE), 100, TimeUnit.MILLISECONDS);

            List<CompletableFuture<Quote>> all = List.of(a, b, c);
            Quote best = CompletableFuture.allOf(a, b, c)
                    .thenApply(v -> all.stream().map(CompletableFuture::join)
                            .min((x, y) -> Integer.compare(x.price(), y.price())).orElseThrow())
                    .join();
            System.out.println("best quote: " + best);
        } finally {
            io.shutdownNow();
        }
    }
}
```

<!-- expected-output -->
```text
best quote: Quote[shop=A, price=120]
```

Reading the output: shop B fails and shop C times out after 100 ms, so both turn into a quote with the maximum price. `allOf` completes after all three stages complete, and the minimum is shop A at 120.

## Interview Angle

### Questions

??? question "Why use `CompletableFuture` instead of `Future`?"

    A `Future` can only block: `get()` holds a thread until the result is ready, you cannot attach a callback, and you cannot combine two futures without blocking. `CompletableFuture` implements `CompletionStage`, so you attach steps (`thenApply`, `thenCompose`, `thenCombine`) that run when the result is ready, and no thread waits in between. You can also complete it yourself with `complete`, which lets you adapt callback APIs, and you get timeouts (`orTimeout`) and error handlers (`handle`, `exceptionally`).

    The trade-off is more complex code, wrapper exceptions, and the need to manage the executor.

??? question "What is the difference between `thenApply` and `thenCompose`?"

    `thenApply` maps a value to a value: its function returns a plain `U`. If the function returns a `CompletableFuture<U>`, the result is a nested `CompletableFuture<CompletableFuture<U>>`. `thenCompose` takes a function that returns a stage and flattens it to `CompletableFuture<U>`. It is the `flatMap` of the type.

    Use `thenApply` for a synchronous step and `thenCompose` for a step that is itself asynchronous, such as a second remote call that needs the result of the first. For two independent calls, use `thenCombine` or `allOf` instead, so that they run in parallel.

??? question "Which thread runs a callback such as `thenApply`?"

    It depends. If the source stage is already complete when you register the callback, the thread that calls `thenApply` runs it. If the source is not complete, the thread that completes the source runs it. `thenApplyAsync` runs it on the default executor (the common pool) and `thenApplyAsync(fn, executor)` runs it on your executor. The stage does not inherit the executor of its source.

    Because the thread is not fixed for plain methods, keep non-async functions short and non-blocking. Put blocking or long functions in an `...Async` method with your own executor.

??? question "What is the default executor of `supplyAsync`, and why would you pass your own?"

    In JDK 21 it is `ForkJoinPool.commonPool()`, whose default parallelism is `availableProcessors() - 1` (at least 1), with daemon worker threads. In JDK 21 to 24, if the parallelism is 1 or less, `CompletableFuture` starts a new thread for each task instead. JDK 25 removed that fallback and always uses the common pool. The common pool is shared with parallel streams and other code, and it is sized for CPU-bound work. A blocking call in a stage holds one of few threads and slows every other user.

    Pass a bounded executor that you sized for the work for any I/O or long task. Also, daemon threads do not keep the JVM alive, so you want control over the lifecycle.

??? question "How do exceptions propagate in a `CompletableFuture` pipeline?"

    If a stage's function throws, the stage completes exceptionally with the exception wrapped in a `CompletionException`. Dependent stages with normal functions are skipped and complete with the same failure. `exceptionally` replaces a failure with a value, `handle` sees both the value and the exception and returns a new value, and `whenComplete` observes the outcome without changing it. `exceptionallyCompose` recovers with another stage.

    `join()` throws the unchecked `CompletionException`. `get()` throws the checked `ExecutionException`. Both carry the original exception as the cause, so unwrap before you test the type. If nobody observes a failed stage, nothing is reported.

??? question "What do `allOf` and `anyOf` return, and how do they handle failures?"

    `allOf` returns `CompletableFuture<Void>` that completes when all sources complete. It has no results, so you read each source with `join()` afterwards. If any source fails, `allOf` fails after all sources complete, with a `CompletionException` caused by one of the failures, and it does not cancel the others. `anyOf` returns `CompletableFuture<Object>` that completes with the outcome of the first source to complete, including a failure. You must cast the result.

    If you need every result even with some failures, handle each source first (for example with `handle` or `exceptionally`), and then call `allOf`.

??? question "How do you add a timeout to a `CompletableFuture`?"

    Use `orTimeout(time, unit)`, which fails the stage with `TimeoutException`, or `completeOnTimeout(value, time, unit)`, which completes it with a fallback value. Both were added in Java 9. They schedule the completion on one shared scheduler thread: `CompletableFutureDelayScheduler` in JDK 21 to 24, and the common pool's delay scheduler in JDK 25. `get(timeout)` only stops the wait in the caller and leaves the stage open.

    A timeout completes the stage and does not stop the work behind it, so the supplier keeps its thread. Also, do not run blocking code in a non-async dependent of a timed-out stage, because it would run on the shared scheduler thread.

??? question "Does `cancel(true)` stop the running task?"

    No. `CompletableFuture.cancel` completes the stage with a `CancellationException`, and the argument has no effect, because the class does not use interrupts. The function keeps running and its result is thrown away. Dependent stages fail with a `CompletionException` caused by the `CancellationException`.

    To stop work, check a cancellation flag in the function, or keep the `Future` of the executor and interrupt it, or use structured concurrency (preview), which cancels sibling tasks.

??? question "What happens if you call `join()` inside a stage that runs on a bounded pool?"

    The thread blocks until the inner stage completes. If the inner stage needs a thread of the same pool and all threads are blocked in the same way, no thread remains to run it: a pool-starvation deadlock. The common pool reduces the risk because `join()` can add compensation threads through `ForkJoinPool.managedBlock`, but the number of spare threads has a limit and blocking still wastes threads.

    The deadlock is also hard to recover from. `join()` ignores interrupts (it records the interrupt and parks again), so `shutdownNow()` does not free the stuck threads, and non-daemon pool threads keep the JVM from exiting. The fix is to compose with `thenCompose` instead of blocking, or to use a separate pool for the inner work. If a stage must wait, use `get(timeout, unit)` or `orTimeout`, so that the wait has a limit.

??? question "Do virtual threads make `CompletableFuture` obsolete?"

    No, but they change the default for I/O-bound code. A virtual thread that blocks on I/O releases its carrier thread, so blocking code on virtual threads reaches the throughput that callback code reaches, with sequential code and normal stack traces. You do not need a pool sized for the waiting. `CompletableFuture` remains useful to compose independent asynchronous results with timeouts and fallbacks, to work with APIs that return `CompletionStage`, and to adapt callbacks. You can combine them: pass a virtual-thread executor to `supplyAsync`.

    CPU-bound work gains nothing from virtual threads. On Java 21, a virtual thread that blocks inside `synchronized` pins its carrier, which JEP 491 reduces in Java 24.

??? question "A pipeline's exception never appears in the logs. Why?"

    A failed `CompletableFuture` stores the exception and does not print it. If no code calls `join()` or `get()` and no handler is attached, nobody reads it. A `runAsync` task that throws in a fire-and-forget call is the typical case. End each pipeline with a handler that logs (`whenComplete` or `handle`), or return the stage to a caller who observes it.

### Follow-up Questions

??? question "What does `thenCombine` do if one of the two stages fails?"

    The combined stage fails with the failure of the stage that failed (wrapped in a `CompletionException`), and the function is not called.

??? question "Why does `thenApplyAsync` without an executor not use the executor of the previous stage?"

    The async methods without an executor argument always use `defaultExecutor()`, the common pool in JDK 21. A stage does not remember the executor of its source. Pass the executor at each step.

??? question "How do you retry a failed stage?"

    Use `exceptionallyCompose` (Java 12) or `handle` followed by `thenCompose` to start the call again, with a counter or a delay from `delayedExecutor`. Limit the number of attempts and add a back-off.

??? question "How do you wrap a callback-based API in a `CompletableFuture`?"

    Create `new CompletableFuture<T>()`, pass a callback that calls `complete(value)` on success and `completeExceptionally(error)` on failure, and return the stage. The first completion wins, so later calls have no effect.

??? question "Why can a `CompletableFuture` program exit before the work finishes?"

    The common pool threads are daemon threads, and a daemon thread does not keep the JVM alive. If `main` ends before the stage finishes, the JVM exits. Call `join()` at the end, or use an executor with non-daemon threads.

??? question "Is `CompletableFuture` thread-safe?"

    Yes. The outcome is set with a CAS, so exactly one `complete` call wins, and a thread that reads the result through `join` or `get` sees it. The functions that you pass are your responsibility: two stages that run in parallel and share a mutable object need synchronization.

### Red Flags

- "`CompletableFuture` is always faster than threads." It only avoids waiting threads. It does not make code faster.
- Calling `join()` or `get()` inside a stage, or after every step, which makes the code sequential.
- Using the default executor for blocking I/O.
- "`cancel(true)` interrupts the task."
- Not knowing that `join()` and handlers see `CompletionException` and not the original exception.
- A pipeline without a final handler.
- `thenApply` returning a `CompletableFuture` (a nested stage).
- "`allOf` returns the results."

## Common Mistakes

| Mistake | Why it hurts | Fix |
|---|---|---|
| Block on `join()` inside a stage on a bounded pool | Pool-starvation deadlock, or wasted threads | Use `thenCompose`, or a separate pool |
| Run blocking I/O on the common pool | Delays parallel streams and all default-pool stages | Pass your own executor, or a virtual-thread executor |
| No handler at the end of a pipeline | The failure is lost | End with `handle` or `whenComplete` that logs |
| Test `e instanceof IOException` on the exception from `join()` | The exception is a `CompletionException` | Unwrap the cause first |
| Assume `orTimeout` stops the work | The supplier keeps its thread | Check a flag in the work, or cancel the real task |
| Assume `cancel(true)` interrupts | The function runs to the end | Use a flag, or interrupt the executor's `Future` |
| `thenApply` with a function that returns a stage | A nested stage and an unobserved failure | Use `thenCompose` |
| Write `a.thenCompose(x -> callB())` for independent calls | The calls run in series | Start both, then `thenCombine` or `allOf` |
| Do blocking work in a plain `thenApply` after a timeout | It runs on the shared delay-scheduler thread | Use `thenApplyAsync(fn, executor)` |
| Let `main` return with stages in flight | Daemon threads end with the JVM | `join()` the final stage |
| Fan out without a limit | Pool or downstream exhaustion | Bound the executor, or use a `Semaphore` |
| Read the result of `allOf` | It returns `Void` | Keep the source stages and `join()` each |
| Write `completedFuture(compute())` to run work asynchronously | `compute()` runs in the calling thread at once, before any stage exists | Pass a lambda: `supplyAsync(() -> compute(), executor)` |
| Throw from a `whenComplete` action | On a successful stage, the returned stage fails with that exception | Catch inside the action, or use `handle` |

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| `thenApply` vs `thenCompose` | Map a value vs flatten a stage | Does the function return a stage? |
| `thenApply` vs `thenApplyAsync` | Runs in the triggering thread vs on an executor | May the function block or run long? |
| `handle` vs `exceptionally` vs `whenComplete` | Both outcomes and a new value / failure only and a replacement value / both outcomes with no change | Must the outcome change? |
| `join` vs `get` | Unchecked `CompletionException` vs checked `ExecutionException`. `get` can be interrupted and has a timeout form | Do you want checked exceptions or a time limit? |
| `allOf` vs `anyOf` | Waits for all, returns `Void` vs first outcome, returns `Object` | Do you need every result or the first? |
| `thenCombine` vs `thenCompose` | Two independent stages vs a dependent second stage | Does the second call need the first result? |
| `orTimeout` vs `completeOnTimeout` | Fails with `TimeoutException` vs completes with a value | Should the caller see a failure or a default? |
| `get(timeout)` vs `orTimeout` | Stops the wait in the caller vs completes the stage | Must the stage itself end? |
| `CompletableFuture` vs virtual threads | Composition of stages vs sequential blocking code | Is the logic a composition, or a sequence? |
| `completeExceptionally` vs a throw in a function | Raw exception on that stage vs `CompletionException` wrapper | Do you test the exception type in a handler on the same stage? |

## Quick Reference Card

- `CompletableFuture` = `Future` + `CompletionStage`. Complete it with `complete` or `completeExceptionally`. The first call wins.
- Create: `supplyAsync`, `runAsync`, `completedFuture`, `failedFuture`, `new CompletableFuture<>()`.
- Default executor in JDK 21: `ForkJoinPool.commonPool()` (daemon threads, parallelism `availableProcessors() - 1`). In JDK 21 to 24, if its parallelism is 1 or less, one new thread for each task. JDK 25 always uses the common pool. Pass your own executor for I/O.
- `thenApply` maps. `thenCompose` flattens. `thenCombine` joins two independent stages. `thenAccept` and `thenRun` end a chain.
- `allOf` gives `Void` and fails after all complete. `anyOf` gives `Object` and takes the first outcome, also a failure.
- Plain `thenX` runs in the calling thread if the source is complete, otherwise in the completing thread. `thenXAsync` uses the default executor, or the one you pass. The executor is not inherited.
- Failures are wrapped in `CompletionException`. `join` throws it. `get` throws `ExecutionException`. Unwrap the cause.
- `exceptionally`: failure to value. `handle`: both outcomes. `whenComplete`: observe only. `exceptionallyCompose`: failure to stage.
- `orTimeout` and `completeOnTimeout` (Java 9) use one shared scheduler thread. They do not stop the work.
- `cancel` completes with `CancellationException`. It does not interrupt.
- Do not block with `join` inside a stage on a bounded pool. Compose with `thenCompose`.
- End every pipeline with a handler, or the failure is lost.
- Virtual threads plus blocking code replace many `CompletableFuture` pipelines. Keep `CompletableFuture` for composition, timeouts with fallbacks, and `CompletionStage` APIs.

## Further Reading

- [`CompletableFuture` javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html). The policies for async and non-async methods, `CompletionException`, and cancellation.
- [`CompletionStage` javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletionStage.html). The rules for which thread runs a dependent action and the exception wrapping.
- [`CompletableFuture` source (JDK 21u)](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/concurrent/CompletableFuture.java). The header comment on the result field and the completion stack, `Delayer`, and `USE_COMMON_POOL`.
- [`ForkJoinPool` javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ForkJoinPool.html). The common pool and its parallelism property.
- [`java.util.concurrent` package javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html). The memory consistency properties.
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444). Why blocking code on virtual threads competes with asynchronous code.
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491). The Java 24 change for blocking inside `synchronized`.
- [JEP 533: Structured Concurrency (Seventh Preview)](https://openjdk.org/jeps/533). The preview API that cancels sibling tasks, in JDK 27.
- [JDK-8319447: Improve performance of delayed task handling](https://bugs.openjdk.org/browse/JDK-8319447). The JDK 25 change that moved `CompletableFuture` timeouts to the common pool's delay scheduler.
- Goetz, Bloch, Bowbeer, Lea, Holmes, Peierls. *Java Concurrency in Practice*. Addison-Wesley, 2006. Chapter 6 (task execution) and chapter 7 (cancellation).
- Urma, Fusco, Mycroft. *Modern Java in Action*, 2nd edition. Manning, 2018. Chapter 16: composable asynchronous programming with `CompletableFuture`.
- [java.util.concurrent](08-java-util-concurrent.md), [Virtual Threads](10-virtual-threads.md), and [Concurrency Coding Problems](11-concurrency-coding-problems.md). The executors, the replacement for blocking pools, and practice problems.
