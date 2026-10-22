---
priority: P1
---
# Concurrency Coding Problems: Classic Thread-Coordination Exercises

## Why It Matters

Interviewers at product companies ask a small set of concurrency coding problems again and again: print odd and even numbers with two threads, print in order with three threads, build a bounded blocking queue, write a reader-writer lock, write a thread-safe singleton. The problems look small, but each one tests a different rule: the guarded wait loop, the lost wakeup, the happens-before edge, the lock order. A candidate who writes a solution that works once and cannot say why it works loses points. A candidate who states the wait condition, picks the right primitive, and names the failure of the obvious solution gets full credit. On the job, the same patterns appear in every hand-written queue, cache, and connection pool. Each problem below has the statement, the common wrong solution and why it fails, and a correct solution that you can run. Every runnable program prints the same output on every run, because it joins its threads before it prints the result or because the protocol itself fixes the order. The primitives come from [Concurrency Basics](07-concurrency-basics.md) and [java.util.concurrent](08-java-util-concurrent.md).

## Core Concepts

**Guarded wait**: a thread that must wait for a condition checks the condition inside a `while` loop and calls `wait()` (or `Condition.await()`) in the loop. The loop is required because a thread can wake up when the condition is still false: another thread can take the item first, and the JVM can wake a thread with no `notify` (a *spurious wakeup*, `Object.wait` javadoc). See the guarded-block pattern in [Concurrency Basics](07-concurrency-basics.md).

**Lost wakeup**: a `notify` or `signal` that wakes no thread that can use it, or that wakes the wrong kind of thread. The waiting thread that needed the signal then waits forever. Two causes: one wait set for two kinds of waiter with `notify`, and a signal sent before the waiter started waiting without a state variable that records the signal.

**Condition variable**: a wait queue that belongs to a lock. `Object.wait`/`notify` give each monitor one queue. `ReentrantLock.newCondition()` gives a lock any number of queues, for example one for "not full" and one for "not empty".

**Poison pill**: a special item that a producer puts in a queue to tell a consumer to stop. The consumer ends when it takes the pill. Use one pill for each consumer.

**Lock ordering**: a rule that every thread acquires several locks in one global order. It removes the circular wait, one of the four Coffman conditions of a deadlock ([Concurrency Basics](07-concurrency-basics.md)).

**Hand-off protocol**: the rule that decides which thread runs next, kept in shared state that a lock protects (a turn variable, a semaphore permit, a latch count). A correct solution to an ordering problem is a hand-off protocol, not a timing guess.

**Deterministic demo**: a program whose output is the same on every run. The rules in this KB: join every thread before you print a result that threads computed, never print thread names or times, and print a time only as a lower bound that the code guarantees ("took at least 50 ms: true"). A program that prints from several threads is deterministic only if a protocol fixes the order of the prints.

## How It Works

### A method for every problem

Use the same five steps on each problem in an interview. Say them aloud.

1. **Name the shared state.** Which variables do two threads touch? (A turn number, a queue, a count of readers.)
2. **Name the wait conditions.** For each kind of thread, say what it waits for in one sentence: "an odd-printing thread waits until the next number is odd".
3. **Choose the primitive.** One lock with `wait`/`notifyAll`, a lock with several `Condition` objects, a `Semaphore`, a `CountDownLatch`, or a `BlockingQueue`. Section "Trade-offs and When to Use" gives the table.
4. **Write the guarded wait with `while`, and change the state before you signal.** A thread that signals must have already changed the state that the waiter checks.
5. **Say how it ends.** How does each thread stop, who calls `join`, and what happens on an interrupt?

### Problem 1: Print odd and even numbers with two threads

**Problem.** One thread prints the odd numbers, another prints the even numbers. Together they print 1, 2, 3, ..., 10 in this order, and the program ends.

**Common wrong solution.** The turn check uses `if` instead of `while`:

```java
class WrongOddEven {
    private int next = 1;

    synchronized void print(boolean odd) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            if ((next % 2 == 1) != odd) {     // if, not while
                wait();
            }
            System.out.println(next++);
            notify();
        }
    }
}
```

*Why it fails.* After `wait()` returns, the thread does not check the turn again. A spurious wakeup lets the thread print out of turn, and the output has a wrong order or a duplicate. Other common failures of this problem: a `boolean` turn flag without `volatile` and a busy loop (`while (turn != me) {}`), which can loop forever because the thread never sees the write (see the JMM section of the concurrency basics page) and burns a CPU core even if it works; and `Thread.sleep` to "give the other thread time", which depends on the machine. Calling `wait()` outside `synchronized` throws `IllegalMonitorStateException`.

**Correct solution.** The state is the number `next`. The odd thread waits while `next` is even (and not past the end), and the even thread waits while `next` is odd. A thread prints, increments `next`, and calls `notifyAll`. The method is `synchronized`, so the check, the print, and the increment form one critical section. At the end, the last thread calls `notifyAll` so that the other thread stops waiting.

```java
public class OddEven {
    private final int max;
    private int next = 1;   // guarded by this: the next number to print

    OddEven(int max) {
        this.max = max;
    }

    // One thread calls print(true) and prints the odd numbers.
    // The other thread calls print(false) and prints the even numbers.
    synchronized void print(boolean odd) throws InterruptedException {
        while (true) {
            while (next <= max && (next % 2 == 1) != odd) {
                wait();                       // not my turn: release the monitor and wait
            }
            if (next > max) {
                notifyAll();                  // wake the other thread so that it can stop
                return;
            }
            System.out.println((odd ? "odd:  " : "even: ") + next);
            next++;
            notifyAll();                      // it is the other thread's turn
        }
    }

    public static void main(String[] args) throws InterruptedException {
        OddEven printer = new OddEven(10);
        Thread odd = new Thread(() -> run(printer, true));
        Thread even = new Thread(() -> run(printer, false));
        even.start();                         // the start order does not matter
        odd.start();
        odd.join();
        even.join();
    }

    private static void run(OddEven printer, boolean odd) {
        try {
            printer.print(odd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

<!-- expected-output -->
```text
odd:  1
even: 2
odd:  3
even: 4
odd:  5
even: 6
odd:  7
even: 8
odd:  9
even: 10
```

Variant: with two `Semaphore` objects (`oddTurn` starts with 1 permit, `evenTurn` with 0), the odd thread does `oddTurn.acquire(); print; evenTurn.release()` and the even thread does the reverse. The semaphore holds the turn, so no loop is needed. It generalizes to *n* threads in a ring (see the next problem).

### Problem 2: Print in order with three threads

**Problem.** Three threads call `first()`, `second()`, and `third()` of one object. The threads can start in any order, but the output must be `first second third`. (A related problem prints `ABCABC...` with three threads that take turns.)

**Common wrong solution.** Use sleeps to order the threads:

```java
class WrongOrder {
    void first(Runnable r) {
        r.run();
    }

    void second(Runnable r) throws InterruptedException {
        Thread.sleep(100);                    // hope that first() ran by now
        r.run();
    }

    void third(Runnable r) throws InterruptedException {
        Thread.sleep(200);                    // hope that second() ran by now
        r.run();
    }
}
```

*Why it fails.* A sleep is a guess about timing. On a loaded machine the thread that runs `first()` can start later than 100 ms, and the output has a wrong order. The solution works in a test and fails in production. A flag that is not `volatile`, with a loop that checks it, has the visibility failure of problem 1.

**Correct solution.** Use one `CountDownLatch(1)` for each hand-off. `second()` waits on the first latch. `third()` waits on the second. `await` returns after the matching `countDown`, and `countDown` happens-before the return of `await` (`CountDownLatch` javadoc), so the effects of the earlier method are also visible. The program starts the threads in the worst order, third first.

```java
import java.util.concurrent.CountDownLatch;

public class PrintInOrder {
    private final CountDownLatch firstDone = new CountDownLatch(1);
    private final CountDownLatch secondDone = new CountDownLatch(1);

    void first(Runnable printFirst) {
        printFirst.run();
        firstDone.countDown();               // opens the gate for second()
    }

    void second(Runnable printSecond) throws InterruptedException {
        firstDone.await();                   // wait until first() finished
        printSecond.run();
        secondDone.countDown();
    }

    void third(Runnable printThird) throws InterruptedException {
        secondDone.await();                  // wait until second() finished
        printThird.run();
    }

    public static void main(String[] args) throws InterruptedException {
        PrintInOrder order = new PrintInOrder();
        StringBuffer out = new StringBuffer();
        // Start the threads in the worst order: third, second, first.
        Thread c = new Thread(() -> run(() -> order.third(() -> out.append("third"))));
        Thread b = new Thread(() -> run(() -> order.second(() -> out.append("second "))));
        Thread a = new Thread(() -> order.first(() -> out.append("first ")));
        c.start();
        b.start();
        a.start();
        a.join();
        b.join();
        c.join();
        System.out.println(out);
    }

    interface Task {
        void call() throws InterruptedException;
    }

    private static void run(Task task) {
        try {
            task.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

<!-- expected-output -->
```text
first second third
```

For the cyclic problem, give each thread its own `Semaphore`: thread *i* acquires its semaphore, prints, and releases the semaphore of thread *i + 1* (modulo 3). Only the semaphore of thread A starts with a permit. At each moment exactly one permit exists, so exactly one thread can run.

```java
import java.util.concurrent.Semaphore;

public class AbcCycle {
    public static void main(String[] args) throws InterruptedException {
        int rounds = 4;
        // Each thread waits on its own semaphore and releases the semaphore of the next thread.
        Semaphore[] turn = { new Semaphore(1), new Semaphore(0), new Semaphore(0) };
        char[] letters = { 'A', 'B', 'C' };
        StringBuffer out = new StringBuffer();
        Thread[] threads = new Thread[3];
        for (int i = 0; i < 3; i++) {
            int me = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int r = 0; r < rounds; r++) {
                        turn[me].acquire();               // wait for my turn
                        out.append(letters[me]);
                        turn[(me + 1) % 3].release();     // give the turn to the next thread
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        for (int i = 2; i >= 0; i--) {
            threads[i].start();                           // start in reverse order on purpose
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println(out);
    }
}
```

<!-- expected-output -->
```text
ABCABCABCABC
```

### Problem 3: Producer-consumer with `wait`/`notify`, and with `BlockingQueue`

**Problem.** Producers put items into a buffer of fixed capacity. Consumers take items out. A producer waits if the buffer is full, and a consumer waits if it is empty. No item is lost or taken twice.

**Common wrong solution.** `if` instead of `while`, and `notify` with two kinds of waiter in one wait set:

```java
class WrongBuffer {
    private final Queue<Integer> items = new ArrayDeque<>();

    synchronized void put(int item) throws InterruptedException {
        if (items.size() == 3) {              // if, not while
            wait();
        }
        items.add(item);
        notify();                             // may wake another producer
    }

    synchronized int take() throws InterruptedException {
        if (items.isEmpty()) {                // if, not while
            wait();
        }
        int item = items.remove();            // can throw NoSuchElementException
        notify();                             // may wake another consumer
        return item;
    }
}
```

*Why it fails.* Two independent failures. First, with `if`, a consumer that wakes up does not re-check the condition. If a second consumer took the item between the `notify` and the wake-up (or the wake-up was spurious), `remove()` runs on an empty queue and throws `NoSuchElementException`. A producer that wakes the same way adds an item to a full buffer. Second, even after you change `if` to `while`, `notify` stays wrong. Producers and consumers share one wait set, and `notify` wakes one arbitrary thread. Example with a capacity of 1: two producers wait because the buffer is full, and a second consumer also waits (it found the buffer empty earlier). The first consumer takes the only item and calls `notify`, and the JVM wakes the second consumer instead of a producer. The first consumer loops, finds the buffer empty, and waits. That consumer finds the buffer empty and waits again. No thread wakes a producer, and if every thread now waits, the program hangs. This is a lost wakeup.

**Correct solution with `wait`/`notifyAll`.** Check with `while`. Use `notifyAll`, because the wait set holds both kinds of thread, and each thread re-checks its own condition. The program uses one producer and one consumer, so it can check that the order is preserved: a bounded buffer is first in, first out.

```java
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

public class WaitNotifyBuffer {
    static final class Buffer {
        private final Queue<Integer> items = new ArrayDeque<>();   // guarded by this
        private final int capacity;

        Buffer(int capacity) {
            this.capacity = capacity;
        }

        synchronized void put(int item) throws InterruptedException {
            while (items.size() == capacity) {      // while, not if: re-check after each wake-up
                wait();
            }
            items.add(item);
            notifyAll();                            // producers and consumers share one wait set
        }

        synchronized int take() throws InterruptedException {
            while (items.isEmpty()) {
                wait();
            }
            int item = items.remove();
            notifyAll();
            return item;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Buffer buffer = new Buffer(3);
        int count = 20;
        List<Integer> consumed = new ArrayList<>();        // only the consumer thread writes it

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= count; i++) {
                    buffer.put(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    consumed.add(buffer.take());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();
        producer.start();
        producer.join();
        consumer.join();                                   // join makes consumed visible to main

        System.out.println("consumed " + consumed.size() + " items");
        System.out.println("in order: " + consumed.equals(
                IntStream.rangeClosed(1, count).boxed().toList()));
    }
}
```

<!-- expected-output -->
```text
consumed 20 items
in order: true
```

**Correct solution with `BlockingQueue`.** In real code, use the library class (Effective Java, Item 81, prefers concurrency utilities to `wait` and `notify`). `ArrayBlockingQueue.put` blocks while the queue is full, and `take` blocks while it is empty. To stop the consumers, the main thread waits for all producers to end, and then puts one poison pill for each consumer.

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class QueueProducerConsumer {
    private static final int POISON = -1;       // a value that no producer sends

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);   // bounded: put() blocks
        int producers = 3;
        int consumers = 2;
        int perProducer = 100;
        AtomicLong sum = new AtomicLong();
        AtomicInteger taken = new AtomicInteger();

        Thread[] producerThreads = new Thread[producers];
        for (int p = 0; p < producers; p++) {
            producerThreads[p] = new Thread(() -> {
                try {
                    for (int i = 1; i <= perProducer; i++) {
                        queue.put(i);                // blocks while the queue is full
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        Thread[] consumerThreads = new Thread[consumers];
        for (int c = 0; c < consumers; c++) {
            consumerThreads[c] = new Thread(() -> {
                try {
                    while (true) {
                        int item = queue.take();     // blocks while the queue is empty
                        if (item == POISON) {
                            return;                  // one poison pill stops one consumer
                        }
                        sum.addAndGet(item);
                        taken.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        for (Thread t : consumerThreads) {
            t.start();
        }
        for (Thread t : producerThreads) {
            t.start();
        }
        for (Thread t : producerThreads) {
            t.join();
        }
        for (int c = 0; c < consumers; c++) {
            queue.put(POISON);                       // after all producers ended: one pill each
        }
        for (Thread t : consumerThreads) {
            t.join();
        }
        System.out.println("items taken: " + taken.get());
        System.out.println("sum: " + sum.get() + " (expected " + producers * 5050 + ")");
    }
}
```

<!-- expected-output -->
```text
items taken: 300
sum: 15150 (expected 15150)
```

A wrong way to stop consumers: `while (!queue.isEmpty()) { queue.take(); }`. The queue can be empty for a moment while a producer is still running, so the consumer stops too early. And if the queue becomes empty between `isEmpty()` and `take()`, the consumer blocks forever. Another wrong choice is an unbounded `LinkedBlockingQueue` in a system where producers are faster than consumers: the queue grows until the JVM runs out of memory. A bounded queue gives *back pressure*, because it slows the producers down.

### Problem 4: Bounded blocking queue from scratch

**Problem.** Implement `BoundedBlockingQueue<T>` with `put` (blocks while full), `take` (blocks while empty), and `size`. It must be correct for many producers and many consumers.

**Common wrong solution.** One `Condition` for both waiter kinds, `signal` instead of `signalAll`, and `if` instead of `while`:

```java
class WrongBoundedQueue<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();    // one condition for both kinds
    private final Object[] items = new Object[4];
    private int head;
    private int tail;
    private int count;

    void put(T item) throws InterruptedException {
        lock.lock();
        try {
            if (count == items.length) {                      // if, not while
                changed.await();
            }
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            changed.signal();                                 // may wake a producer
        } finally {
            lock.unlock();
        }
    }
}
```

*Why it fails.* The same two independent failures as problem 3, with `Condition` in place of `wait`. With `if`, a producer that wakes up (after a spurious wakeup, or after another producer filled the free slot) does not re-check, writes into a full queue, and overwrites the oldest item. And even with `while`, one condition with `signal` is wrong: a producer's `signal` can wake another producer, which finds the queue full and waits again, while the consumers that wait for items stay asleep. If you must use one condition, use `while` and `signalAll`. The better design uses two conditions.

**Correct solution.** One `ReentrantLock` and two conditions: `notFull` for producers and `notEmpty` for consumers. `put` signals `notEmpty` (one consumer can proceed) and `take` signals `notFull`. Each uses `signal`, which is safe here because every waiter of a condition waits for the same thing and each state change makes room for one waiter. The queue is a ring buffer: `head` is the oldest item, `tail` is the next free slot. The test starts 3 producers and 2 consumers on a queue of capacity 4. A consumer first claims an item with an atomic counter, and then takes it, so the consumers stop without a poison pill. `ArrayBlockingQueue` uses the same design: one lock and two conditions (JDK 21 source of `ArrayBlockingQueue`). `LinkedBlockingQueue` uses two locks, `putLock` with the condition `notFull` and `takeLock` with the condition `notEmpty`, so a producer and a consumer can run at the same time. The price, in the JDK 21 source: the element count is an `AtomicInteger`, because two locks guard it, and a thread that must signal the condition of the other lock first takes that lock (`signalNotEmpty`, `signalNotFull`). See [java.util.concurrent](08-java-util-concurrent.md).

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedQueueDemo {
    static final class BoundedBlockingQueue<T> {
        private final Object[] items;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();    // producers wait here
        private final Condition notEmpty = lock.newCondition();   // consumers wait here
        private int head;     // index of the oldest item, guarded by lock
        private int tail;     // index of the next free slot, guarded by lock
        private int count;    // number of items, guarded by lock

        BoundedBlockingQueue(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            items = new Object[capacity];
        }

        void put(T item) throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == items.length) {
                    notFull.await();                  // releases the lock while it waits
                }
                items[tail] = item;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal();                    // one consumer can proceed
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        T take() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == 0) {
                    notEmpty.await();
                }
                T item = (T) items[head];
                items[head] = null;                   // let the garbage collector free it
                head = (head + 1) % items.length;
                count--;
                notFull.signal();                     // one producer can proceed
                return item;
            } finally {
                lock.unlock();
            }
        }

        int size() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int capacity = 4;
        BoundedBlockingQueue<Integer> queue = new BoundedBlockingQueue<>(capacity);
        int producers = 3;
        int consumers = 2;
        int perProducer = 100;
        int total = producers * perProducer;
        AtomicInteger remaining = new AtomicInteger(total);   // items that consumers still take
        AtomicLong sum = new AtomicLong();
        AtomicInteger maxSize = new AtomicInteger();

        Thread[] threads = new Thread[producers + consumers];
        for (int p = 0; p < producers; p++) {
            threads[p] = new Thread(() -> {
                try {
                    for (int i = 1; i <= perProducer; i++) {
                        queue.put(i);
                        maxSize.accumulateAndGet(queue.size(), Math::max);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        for (int c = 0; c < consumers; c++) {
            threads[producers + c] = new Thread(() -> {
                try {
                    while (remaining.decrementAndGet() >= 0) {   // claim one item, then take it
                        sum.addAndGet(queue.take());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("sum: " + sum.get() + " (expected " + producers * 5050 + ")");
        System.out.println("size never above capacity: " + (maxSize.get() <= capacity));
        System.out.println("queue empty at the end: " + (queue.size() == 0));
    }
}
```

<!-- expected-output -->
```text
sum: 15150 (expected 15150)
size never above capacity: true
queue empty at the end: true
```

!!! tip "Follow-up that interviewers ask"
    "Add `offer(item, timeout)`." Use `notFull.awaitNanos(remaining)` in the `while` loop. It returns the remaining time, so a spurious wakeup does not extend the total wait. "Add `close()`." Add a `closed` flag under the lock, make `put` throw after close, and make `take` return `null` (or throw) when the queue is empty and closed. `close()` must call `signalAll` on both conditions.

### Problem 5: Reader-writer lock from scratch

**Problem.** Implement a lock that allows many readers at the same time, or one writer, and never a reader together with a writer. State what happens to a writer when readers keep arriving.

**Common wrong solution.** Plain fields, a check followed by an action, and a busy loop:

```java
class WrongReadWriteLock {
    private int readers;                       // plain field: no lock, no volatile
    private volatile boolean writing;

    void lockRead() {
        while (writing) {
            Thread.onSpinWait();
        }
        readers++;                             // not atomic, and the check above is stale
    }

    void unlockRead() {
        readers--;
    }

    void lockWrite() {
        while (writing || readers > 0) {
            Thread.onSpinWait();
        }
        writing = true;                        // two writers can both pass the check
    }

    void unlockWrite() {
        writing = false;
    }
}
```

*Why it fails.* The check and the update are two steps, so another thread runs between them (check-then-act). Two writers can both see `writing == false` and both enter. A reader can pass the `writing` check, a writer then sets `writing`, and the reader enters with the writer. `readers++` and `readers--` on a plain field lose updates, so the count is wrong and a writer can wait forever or enter too early. The spin loops burn CPU while they wait.

**Correct solution.** One monitor protects three fields: `activeReaders`, `writing`, and `waitingWriters`. A reader waits while a writer writes or waits. A writer waits while a writer writes or readers read. The `waitingWriters` count gives writers priority, so a steady stream of new readers cannot starve a writer. The price is that readers wait behind a writer, and a steady stream of writers can starve the readers. The last reader to leave and every writer that leaves call `notifyAll`. The lock is not reentrant, and a thread that holds the read lock must not request the write lock (it would wait for itself).

The test has two parts. Part 1 checks that three readers hold the lock together: each reader counts down a latch inside the lock and waits for the latch, and the latch opens only if all three are inside. Part 2 starts two writers, which change two fields in two steps, and four readers, which check that the fields are equal. The count of torn reads must be 0.

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RwLockDemo {
    // Many readers or one writer. Writers have priority: a new reader waits
    // while a writer waits, so a steady stream of readers cannot starve a writer.
    // Not reentrant, and no upgrade from read to write.
    static final class SimpleReadWriteLock {
        private int activeReaders;    // guarded by this
        private boolean writing;      // guarded by this
        private int waitingWriters;   // guarded by this

        synchronized void lockRead() throws InterruptedException {
            while (writing || waitingWriters > 0) {
                wait();
            }
            activeReaders++;
        }

        synchronized void unlockRead() {
            activeReaders--;
            if (activeReaders == 0) {
                notifyAll();          // a waiting writer can proceed
            }
        }

        synchronized void lockWrite() throws InterruptedException {
            waitingWriters++;
            try {
                while (writing || activeReaders > 0) {
                    wait();
                }
            } catch (InterruptedException e) {
                waitingWriters--;
                notifyAll();          // readers that waited for this writer can proceed
                throw e;
            }
            waitingWriters--;
            writing = true;
        }

        synchronized void unlockWrite() {
            writing = false;
            notifyAll();              // wake both readers and writers
        }
    }

    private static int a;             // invariant: a == b, whenever no writer is inside
    private static int b;

    public static void main(String[] args) throws Exception {
        SimpleReadWriteLock lock = new SimpleReadWriteLock();

        // Test 1: three readers are inside the read lock at the same time.
        CountDownLatch allInside = new CountDownLatch(3);
        AtomicInteger together = new AtomicInteger();
        Thread[] readers = new Thread[3];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = new Thread(() -> {
                try {
                    lock.lockRead();
                    try {
                        allInside.countDown();
                        // This returns true only if all three readers hold the lock now.
                        if (allInside.await(10, TimeUnit.SECONDS)) {
                            together.incrementAndGet();
                        }
                    } finally {
                        lock.unlockRead();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            readers[i].start();
        }
        for (Thread t : readers) {
            t.join();
        }
        System.out.println("readers inside together: " + together.get());

        // Test 2: writers update two fields in two steps. Readers must never see a half update.
        AtomicInteger torn = new AtomicInteger();
        Thread[] threads = new Thread[6];
        for (int i = 0; i < threads.length; i++) {
            boolean writer = i < 2;
            threads[i] = new Thread(() -> {
                try {
                    for (int n = 0; n < 200; n++) {
                        if (writer) {
                            lock.lockWrite();
                            try {
                                a++;
                                Thread.yield();      // widens the window for a torn read
                                b++;
                            } finally {
                                lock.unlockWrite();
                            }
                        } else {
                            lock.lockRead();
                            try {
                                if (a != b) {
                                    torn.incrementAndGet();
                                }
                            } finally {
                                lock.unlockRead();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("torn reads: " + torn.get());
        System.out.println("a=" + a + " b=" + b);
    }
}
```

<!-- expected-output -->
```text
readers inside together: 3
torn reads: 0
a=400 b=400
```

The JDK class is `ReentrantReadWriteLock` (and `StampedLock` for optimistic reads), see [java.util.concurrent](08-java-util-concurrent.md). Its default mode is non-fair. Its javadoc allows a downgrade (take the read lock while you hold the write lock, then release the write lock) but not an upgrade: a thread that holds the read lock and calls `writeLock().lock()` waits for itself and blocks forever. Name the reader-writer trade-off in an interview: a reader-preference lock gives more read throughput but can starve writers, and a writer-preference lock avoids that but delays readers.

### Problem 6: Thread-safe singleton

**Problem.** Write a class that has at most one instance in the JVM, that many threads can request at the same time, and that creates its instance lazily.

**Common wrong solution.** Double-checked locking without `volatile`:

```java
class BrokenSingleton {
    private static BrokenSingleton instance;      // not volatile

    private BrokenSingleton() { }

    static BrokenSingleton getInstance() {
        if (instance == null) {
            synchronized (BrokenSingleton.class) {
                if (instance == null) {
                    instance = new BrokenSingleton();
                }
            }
        }
        return instance;
    }
}
```

*Why it fails.* The first check reads `instance` without a lock and without a happens-before edge. `new BrokenSingleton()` is three steps: allocate, run the constructor, store the reference. The Java memory model allows the store to become visible before the constructor's writes (JLS chapter 17). A second thread can then see a non-null `instance` and use an object that is not fully built. The failure is rare, depends on the JIT and the CPU, and no unit test finds it reliably. So this KB does not run it: its output would not be the same on every run. The fix is one word, `volatile`. A plain lazy `if (instance == null) instance = new ...` without any lock is worse, because two threads can create two instances.

**Correct solutions.** Three idioms. The program starts eight threads at the same time for each idiom and counts the distinct instances and the constructor calls. Both must be 1.

| Idiom | Why it is thread-safe | Cost |
|---|---|---|
| Initialization-on-demand holder | A nested class `Holder` has the instance in a `static final` field. The JVM initializes `Holder` once, with a lock, when `getInstance()` first uses it (JLS 12.4.1, 12.4.2). The class initialization lock gives the happens-before edge | No lock on the fast path. Lazy. Simple |
| `enum` with one constant | The JVM creates the constant once. An enum also resists serialization and reflection attacks that create a second instance (Effective Java, Item 3) | The simplest. It cannot extend another class. The JVM creates the constant when the enum class first initializes |
| Double-checked locking with `volatile` | The volatile write publishes a fully built object. The volatile read on the fast path sees it. The `synchronized` block makes sure that one thread creates it | One volatile read on the fast path. More code |

```java
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SingletonDemo {
    static final AtomicInteger CONSTRUCTIONS = new AtomicInteger();

    // 1. Initialization-on-demand holder: the JVM runs the Holder initializer once,
    //    when getInstance() first touches Holder (JLS 12.4.1 and 12.4.2).
    static final class HolderSingleton {
        private HolderSingleton() {
            CONSTRUCTIONS.incrementAndGet();
        }

        private static final class Holder {
            static final HolderSingleton INSTANCE = new HolderSingleton();
        }

        static HolderSingleton getInstance() {
            return Holder.INSTANCE;
        }
    }

    // 2. Enum: the JVM creates each constant once. It also survives serialization and reflection.
    enum EnumSingleton {
        INSTANCE;

        EnumSingleton() {
            CONSTRUCTIONS.incrementAndGet();
        }
    }

    // 3. Double-checked locking: the field must be volatile.
    static final class DclSingleton {
        private static volatile DclSingleton instance;

        private DclSingleton() {
            CONSTRUCTIONS.incrementAndGet();
        }

        static DclSingleton getInstance() {
            DclSingleton local = instance;           // one volatile read on the fast path
            if (local == null) {
                synchronized (DclSingleton.class) {
                    local = instance;
                    if (local == null) {
                        local = new DclSingleton();
                        instance = local;            // volatile write publishes a built object
                    }
                }
            }
            return local;
        }
    }

    // Eight threads start together and each reads the instance. Count the distinct objects.
    private static int distinctInstances(Supplier<Object> getter) throws InterruptedException {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        CountDownLatch go = new CountDownLatch(1);
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                seen.add(getter.get());
            });
            threads[i].start();
        }
        go.countDown();
        for (Thread t : threads) {
            t.join();
        }
        return seen.size();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("holder: instances=" + distinctInstances(HolderSingleton::getInstance)
                + " constructions=" + CONSTRUCTIONS.getAndSet(0));
        System.out.println("enum:   instances=" + distinctInstances(() -> EnumSingleton.INSTANCE)
                + " constructions=" + CONSTRUCTIONS.getAndSet(0));
        System.out.println("dcl:    instances=" + distinctInstances(DclSingleton::getInstance)
                + " constructions=" + CONSTRUCTIONS.getAndSet(0));
    }
}
```

<!-- expected-output -->
```text
holder: instances=1 constructions=1
enum:   instances=1 constructions=1
dcl:    instances=1 constructions=1
```

Recommendation: use the holder idiom or an enum. Use double-checked locking only if you need lazy creation with a constructor argument or instance state that the holder cannot give, and then use `volatile`. In a Spring application, a singleton bean replaces all of these (see [Spring Core: IoC, Beans, and AOP](17-spring-core.md)).

### Problem 7: Dining philosophers

**Problem.** Five philosophers sit at a round table with five forks, one between each pair. A philosopher needs both adjacent forks to eat. Each philosopher thinks, takes both forks, eats, and puts them down. Write code that cannot deadlock. (A follow-up asks for more: that no philosopher starves, that is, waits forever while the others keep eating.)

**Common wrong solution.** Every philosopher takes the left fork and then the right fork with blocking `lock()`:

```java
class WrongPhilosopher implements Runnable {
    private final ReentrantLock left;
    private final ReentrantLock right;

    WrongPhilosopher(ReentrantLock left, ReentrantLock right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        for (int meal = 0; meal < 1000; meal++) {
            left.lock();
            right.lock();                        // may wait for a neighbor who waits for me
            try {
                // eat
            } finally {
                right.unlock();
                left.unlock();
            }
        }
    }
}
```

*Why it fails.* If all five philosophers take their left fork at the same moment, each one holds one fork and waits for the right fork, which the next philosopher holds. This is a circular wait: all four Coffman conditions hold (mutual exclusion, hold and wait, no preemption, circular wait). The program hangs. A thread dump (`jstack <pid>`) shows every philosopher thread in state `WAITING (parking)` inside `ReentrantLock.lock`, and ends with a "Found one Java-level deadlock" report for the cycle. On JDK 21, plain `jstack` reports `ReentrantLock` deadlocks as well as monitor deadlocks, and `jstack -l` adds the list of `java.util.concurrent` locks that each thread holds ([Concurrency Basics](07-concurrency-basics.md)). The deadlock is rare in a test, because it needs a specific interleaving, so the bug can pass the tests.

**Correct solution.** Break the circular wait with lock ordering: number the forks, and let each philosopher take the fork with the lower number first. Philosopher 4 sits between fork 4 and fork 0, so philosopher 4 takes fork 0 first, then fork 4. Now no cycle can form, because a philosopher that holds a high-numbered fork has already taken the lower one. Two other solutions:

- Allow at most four philosophers at the table with a `Semaphore(4)`. Five forks and four philosophers cannot form a cycle of five, so at least one philosopher can always get both forks.
- Dijkstra's own solution (1971, the paper that introduced the problem): one mutex protects a state for each philosopher (thinking, hungry, eating), and each philosopher has a private semaphore. A hungry philosopher eats only when neither neighbor eats. A philosopher who stops eating checks both neighbors and wakes the ones that can now eat. Dijkstra noted that this solution can starve a philosopher whose two neighbors take turns eating.

Lock ordering removes the deadlock, but not starvation: a non-fair `ReentrantLock` lets a thread that arrives later take a fork before a thread that waits. For starvation freedom, use fair locks (`new ReentrantLock(true)`), which grant the lock to the thread that waited longest, at the cost of lower throughput (the JDK 21 `ReentrantLock` javadoc: programs that use fair locks with many threads "may display lower overall throughput (i.e., are slower; often much slower) than those using the default setting, but have smaller variances in times to obtain locks and guarantee lack of starvation").

The program has two parts. Part 1 makes the wrong order fail on purpose, and does it deterministically: a barrier makes every philosopher hold the left fork before anyone asks for the right fork, and `tryLock` with a 200 ms timeout shows that nobody can get it (the first barrier makes the circular wait certain, and a second barrier makes all philosophers wait until all five attempts ended, so no one lets go early). Part 2 runs the lock ordering for 1000 meals each.

```java
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Philosophers {
    static final int N = 5;

    static ReentrantLock[] newForks() {
        ReentrantLock[] forks = new ReentrantLock[N];
        for (int i = 0; i < N; i++) {
            forks[i] = new ReentrantLock();
        }
        return forks;
    }

    // Wrong order: every philosopher takes the left fork first. The barrier makes all five
    // threads hold their left fork before any thread asks for its right fork, so the circular
    // wait is certain. tryLock with a timeout shows the deadlock without a hang.
    static int philosophersThatCannotEat() throws InterruptedException {
        ReentrantLock[] forks = newForks();
        CyclicBarrier allHoldLeft = new CyclicBarrier(N);
        CyclicBarrier allTried = new CyclicBarrier(N);
        AtomicInteger stuck = new AtomicInteger();
        Thread[] threads = new Thread[N];
        for (int i = 0; i < N; i++) {
            int left = i;
            int right = (i + 1) % N;
            threads[i] = new Thread(() -> {
                forks[left].lock();
                try {
                    allHoldLeft.await();
                    if (forks[right].tryLock(200, TimeUnit.MILLISECONDS)) {
                        forks[right].unlock();             // never reached: the wait is circular
                    } else {
                        stuck.incrementAndGet();
                    }
                    allTried.await();                      // nobody lets go before all tried
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    forks[left].unlock();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        return stuck.get();
    }

    // Right order: every philosopher takes the fork with the lower number first.
    // Philosopher 4 takes fork 0 before fork 4, so the circular wait cannot form.
    static int[] dineWithLockOrdering(int meals) throws InterruptedException {
        ReentrantLock[] forks = newForks();
        int[] eaten = new int[N];                          // each thread writes only its own slot
        Thread[] threads = new Thread[N];
        for (int i = 0; i < N; i++) {
            int me = i;
            int first = Math.min(i, (i + 1) % N);
            int second = Math.max(i, (i + 1) % N);
            threads[i] = new Thread(() -> {
                for (int m = 0; m < meals; m++) {
                    forks[first].lock();
                    forks[second].lock();
                    try {
                        eaten[me]++;
                    } finally {
                        forks[second].unlock();
                        forks[first].unlock();
                    }
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();                                      // join makes eaten[] visible
        }
        return eaten;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("left fork first: " + philosophersThatCannotEat()
                + " of " + N + " philosophers cannot get the right fork");
        System.out.println("lower fork first: meals eaten = "
                + Arrays.toString(dineWithLockOrdering(1000)));
    }
}
```

<!-- expected-output -->
```text
left fork first: 5 of 5 philosophers cannot get the right fork
lower fork first: meals eaten = [1000, 1000, 1000, 1000, 1000]
```

`tryLock` with a timeout is also a deadlock *recovery* tool: a thread that cannot get the second lock releases the first and retries. Retrying without a random delay can cause a livelock (see [Concurrency Basics](07-concurrency-basics.md)).

### Problem 8: A rate-limited executor

**Problem.** Write an executor that starts at most *r* tasks per second, with a short burst allowed, and runs the tasks on a worker pool. (The algorithms are on the HLD page [Rate Limiting Algorithms](../hld/fundamentals/13-rate-limiting-algorithms.md): token bucket, leaky bucket, fixed and sliding window. This problem is the in-process, single-JVM version.)

**Common wrong solution.** Throttle inside each worker with a sleep:

```java
class WrongRateLimit {
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    void submit(Runnable task) {
        pool.execute(() -> {
            task.run();
            try {
                Thread.sleep(10);               // "100 tasks a second"
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
```

*Why it fails.* Each of the four workers sleeps for 10 ms, so up to four tasks start every 10 ms, which is four times the target rate. The rate also changes with the duration of the task, because the sleep comes after the task. And the first four tasks start at the same moment, with no control over the burst. The limit must be on the *start* of a task, in one place, with one shared clock.

**Correct solution.** Two classes. `TokenBucket` keeps a credit that grows with time and stops at the capacity. One token costs `nanosPerToken` of credit. `reserve()` takes one token and returns 0, or returns how many nanoseconds to wait. All of its state changes are in one `synchronized` method. The clock is injected as a `LongSupplier`, so a test can use a fake clock. `RateLimitedExecutor` has one dispatcher thread: it takes a task from a queue, waits in `acquire()` until the bucket allows one start, and gives the task to a worker pool. `shutdownAndWait()` puts a stop marker behind all accepted tasks, so every accepted task runs.

Test 1 uses the fake clock. A bucket with capacity 3 and 1 token a second allows a burst of 3, refuses the fourth, gives back one token after 1 second, and after a long pause stops at 3 tokens, not more. Test 2 uses the real clock: 6 tasks at 100 a second need 5 tokens after the first one, so the run takes at least 50 ms. The program prints that as a lower bound, which the code guarantees.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

public class RateLimitDemo {
    // A token bucket (see the HLD page on rate limiting algorithms). The bucket stores credit as
    // nanoseconds: one token costs nanosPerToken, and the credit never exceeds the capacity.
    static final class TokenBucket {
        private final long maxCredit;
        private final long nanosPerToken;
        private final LongSupplier clock;     // injected, so a test can use a fake clock
        private long credit;                  // guarded by this
        private long lastRefill;              // guarded by this

        TokenBucket(int capacity, long nanosPerToken, LongSupplier clock) {
            this.maxCredit = capacity * nanosPerToken;
            this.nanosPerToken = nanosPerToken;
            this.clock = clock;
            this.credit = maxCredit;          // the bucket starts full
            this.lastRefill = clock.getAsLong();
        }

        // Takes one token and returns 0, or returns the nanoseconds to wait for the next token.
        synchronized long reserve() {
            long now = clock.getAsLong();
            credit = Math.min(maxCredit, credit + (now - lastRefill));
            lastRefill = now;
            if (credit >= nanosPerToken) {
                credit -= nanosPerToken;
                return 0;
            }
            return nanosPerToken - credit;
        }

        boolean tryAcquire() {
            return reserve() == 0;
        }

        void acquire() throws InterruptedException {
            long wait;
            while ((wait = reserve()) > 0) {
                LockSupport.parkNanos(wait);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        }
    }

    // One dispatcher thread takes tasks from a queue, waits for a token, and then hands the
    // task to the worker pool. The rate limits the start of tasks, not their duration.
    static final class RateLimitedExecutor {
        private static final Runnable STOP = () -> { };

        private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        private final ExecutorService workers;
        private final Thread dispatcher;
        private boolean closed;               // guarded by this

        RateLimitedExecutor(TokenBucket bucket, int workerThreads) {
            workers = Executors.newFixedThreadPool(workerThreads);
            dispatcher = new Thread(() -> {
                try {
                    while (true) {
                        Runnable task = queue.take();
                        if (task == STOP) {
                            return;
                        }
                        bucket.acquire();            // blocks until the rate allows one start
                        workers.execute(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            dispatcher.start();
        }

        synchronized void execute(Runnable task) {
            if (closed) {
                throw new RejectedExecutionException("executor is closed");
            }
            queue.add(task);
        }

        // Runs the queued tasks, then stops.
        void shutdownAndWait() throws InterruptedException {
            synchronized (this) {
                closed = true;
                queue.add(STOP);                      // queued after all accepted tasks
            }
            dispatcher.join();
            workers.shutdown();
            workers.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    public static void main(String[] args) throws Exception {
        // Test 1: a fake clock makes the bucket fully deterministic.
        long[] now = { 0 };
        TokenBucket bucket = new TokenBucket(3, 1_000_000_000L, () -> now[0]);   // 1 token a second
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            results.add(bucket.tryAcquire());         // burst of 3, then empty
        }
        now[0] += 1_000_000_000L;                     // one second later: one token back
        results.add(bucket.tryAcquire());
        results.add(bucket.tryAcquire());
        now[0] += 60_000_000_000L;                    // a long pause: the credit stops at 3
        for (int i = 0; i < 4; i++) {
            results.add(bucket.tryAcquire());
        }
        System.out.println(results);

        // Test 2: the real clock. 6 tasks at 100 a second (10 ms for each token), capacity 1.
        // Five tokens must be earned after the first one, so the run takes at least 50 ms.
        long start = System.nanoTime();
        TokenBucket real = new TokenBucket(1, 10_000_000L, System::nanoTime);
        RateLimitedExecutor executor = new RateLimitedExecutor(real, 4);
        AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < 6; i++) {
            executor.execute(ran::incrementAndGet);
        }
        executor.shutdownAndWait();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("tasks run: " + ran.get());
        System.out.println("took at least 50 ms: " + (elapsedMillis >= 50));
        try {
            executor.execute(() -> { });
        } catch (RejectedExecutionException e) {
            System.out.println("rejected after shutdown: true");
        }
    }
}
```

<!-- expected-output -->
```text
[true, true, true, false, true, false, true, true, true, false]
tasks run: 6
took at least 50 ms: true
rejected after shutdown: true
```

Limits of this design: the dispatcher is one thread, so one slow `acquire` delays all tasks (that is the purpose); the queue is unbounded, so add a capacity and reject when it is full if the producers are faster than the rate; the limit is per JVM, so a distributed limit needs a shared store (see the HLD page). The JDK has no rate limiter. Guava's `RateLimiter` is a library class for the same purpose.

### Problem 9: A simple thread pool from scratch

**Problem.** Implement a fixed-size thread pool with `execute(Runnable)`, `shutdown()`, and `awaitTermination`. Tasks that were accepted must run. A task that throws must not kill a worker.

**Common wrong solution.** A non-thread-safe queue, a busy loop, and a flag that threads may not see:

```java
class WrongPool {
    private final Queue<Runnable> queue = new LinkedList<>();   // not thread-safe
    private boolean shutdown;                                   // not volatile

    WrongPool(int threads) {
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                while (!shutdown) {                             // may never see true
                    Runnable task = queue.poll();               // data race with add()
                    if (task != null) {
                        task.run();                             // an exception ends this worker
                    }
                }
            }).start();
        }
    }

    void execute(Runnable task) {
        queue.add(task);
    }

    void shutdown() {
        shutdown = true;                                        // queued tasks are dropped
    }
}
```

*Why it fails.* `LinkedList` is not thread-safe, so concurrent `add` and `poll` corrupt it and lose tasks. The worker loop spins at full CPU speed when the queue is empty. The `shutdown` flag is not `volatile`, so a worker may never see it (a data race). When `shutdown` is seen, the workers stop at once and drop the queued tasks. A task that throws ends its worker thread, and the pool shrinks until no worker is left. The workers are not daemon threads, so a worker that never sees the flag keeps the JVM running forever, and no method lets a caller wait until the workers end.

**Correct solution.** A bounded `ArrayBlockingQueue` is the thread-safe queue, and `take()` blocks the worker while the queue is empty (no spinning). The worker catches `Throwable` from a task, counts it, and goes on. `execute` and the `shutdown` flag share one lock, so a task is never accepted after shutdown starts. `execute` uses the non-blocking `offer` and rejects a task when the queue is full or the pool is closed (a *rejection policy*, like `AbortPolicy` of `ThreadPoolExecutor`). `shutdown` first sets the flag, and then puts one poison pill for each worker behind the accepted tasks, so all accepted tasks run before the workers stop. `awaitTermination` joins the workers against one deadline.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolDemo {
    static final class SimplePool {
        private static final Runnable STOP = () -> { };     // poison pill: one stops one worker

        private final BlockingQueue<Runnable> queue;
        private final List<Thread> workers = new ArrayList<>();
        private final AtomicInteger failures = new AtomicInteger();
        private boolean shutdown;                          // guarded by this

        SimplePool(int threads, int queueCapacity) {
            queue = new ArrayBlockingQueue<>(queueCapacity);
            for (int i = 0; i < threads; i++) {
                Thread worker = new Thread(this::workerLoop);
                workers.add(worker);
                worker.start();
            }
        }

        private void workerLoop() {
            try {
                while (true) {
                    Runnable task = queue.take();           // blocks while the queue is empty
                    if (task == STOP) {
                        return;
                    }
                    try {
                        task.run();
                    } catch (Throwable t) {
                        failures.incrementAndGet();         // a failed task must not kill a worker
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Rejects the task if the pool is shut down or the queue is full (a bounded queue).
        synchronized void execute(Runnable task) {
            if (shutdown || !queue.offer(task)) {
                throw new RejectedExecutionException("pool is shut down or the queue is full");
            }
        }

        // Accepted tasks still run. Then each worker takes one pill and ends.
        void shutdown() throws InterruptedException {
            synchronized (this) {
                if (shutdown) {
                    return;
                }
                shutdown = true;             // no execute() call can add a task after this point
            }
            for (int i = 0; i < workers.size(); i++) {
                queue.put(STOP);             // waits for space if the queue is full
            }
        }

        boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            for (Thread worker : workers) {
                long left = deadline - System.nanoTime();
                worker.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(left)));
            }
            return workers.stream().noneMatch(Thread::isAlive);
        }

        int failures() {
            return failures.get();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SimplePool pool = new SimplePool(4, 200);
        AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            pool.execute(ran::incrementAndGet);
        }
        pool.execute(() -> {
            throw new IllegalStateException("task failure");   // the worker must survive this
        });
        pool.shutdown();
        boolean finished = pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("tasks run: " + ran.get());
        System.out.println("failed tasks: " + pool.failures());
        System.out.println("all workers ended: " + finished);
        try {
            pool.execute(() -> { });
        } catch (RejectedExecutionException e) {
            System.out.println("rejected after shutdown: true");
        }
    }
}
```

<!-- expected-output -->
```text
tasks run: 100
failed tasks: 1
all workers ended: true
rejected after shutdown: true
```

What the JDK `ThreadPoolExecutor` adds ([java.util.concurrent](08-java-util-concurrent.md)): a core and a maximum size, a keep-alive time for idle threads, a choice of queue and of rejection policy, a `ThreadFactory`, `Future` results, `shutdownNow` that interrupts the workers, and hooks (`beforeExecute`, `afterExecute`). In an interview, build the small version, and then name these features.

## Trade-offs and When to Use

| Problem type | Best primitive | Why | Watch for |
|---|---|---|---|
| Fixed order of steps (A before B before C) | `CountDownLatch` (one-shot), or a `Semaphore` chain | The latch states "this step is done". No loop, no flag | A latch cannot be reused. Use a `Semaphore` for repeated rounds |
| Turn taking in a ring (ABCABC) | One `Semaphore` for each thread | Exactly one permit exists. Only one thread can run | Initialize exactly one semaphore with a permit |
| Producer and consumer | `BlockingQueue` (bounded) | The library has the lock, the conditions, and the memory visibility | Bounded capacity. A poison pill for each consumer |
| Producer and consumer from scratch | `ReentrantLock` with two `Condition` objects | Each kind of waiter has its own queue, so `signal` is safe | `while` loop. Change the state before `signal` |
| Same, with `synchronized` | `wait` and `notifyAll` | One wait set for both kinds, so `notifyAll` is required | `if` instead of `while`. `notify` instead of `notifyAll` |
| Many readers or one writer | `ReentrantReadWriteLock` (JDK). One monitor in a from-scratch answer | Reads do not exclude each other | Writer starvation, no upgrade from read to write |
| One instance | Holder idiom or `enum` | The JVM class initialization gives the safety | Double-checked locking needs `volatile` |
| Several locks at once | Lock ordering (or `tryLock` with a timeout) | Removes the circular wait | One global order for every code path |
| Limit the rate of starts | Token bucket (one dispatcher, injected clock) | Allows a burst and a steady rate. Testable | One shared bucket. The limit is per JVM |
| Reusable workers | `ThreadPoolExecutor` | Bounded threads, bounded queue, rejection policy | Do not use `Executors.newFixedThreadPool` (unbounded queue) where overload can happen |

Recommendation: in an interview, state the library solution first ("in production I would use a `BlockingQueue`"), and then write the from-scratch version that the interviewer asks for. In production code, write your own primitive only when no library class fits.

## Java Example

The programs above test their solutions with the same few tools: start the threads together, join them, and read the result after the join. The last example is a small test harness that does this, and it also solves a common test bug: an exception in a worker thread does not fail the test, because the exception ends only that thread. The harness runs a task on *n* threads that start at the same moment, records the first uncaught exception, and rethrows it in the caller.

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StressDemo {
    // Runs the task on n threads that start at the same moment. It joins all threads and
    // rethrows the first exception that any thread threw.
    static void runTogether(int n, Runnable task) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startGate.await();               // all threads wait here, then run together
                    task.run();
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);  // keep the first failure
                }
            });
            threads[i].start();
        }
        startGate.countDown();
        for (Thread t : threads) {
            t.join();                                // join: the results are visible to the caller
        }
        Throwable t = failure.get();
        if (t instanceof RuntimeException re) {
            throw re;
        }
        if (t instanceof Error e) {
            throw e;
        }
        if (t != null) {
            throw new IllegalStateException(t);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // A correct counter: 8 threads, 10,000 increments each.
        AtomicInteger counter = new AtomicInteger();
        runTogether(8, () -> {
            for (int i = 0; i < 10_000; i++) {
                counter.incrementAndGet();
            }
        });
        System.out.println("counter: " + counter.get());

        // A task that fails in one thread. Without the harness, main would not notice.
        AtomicInteger arrivals = new AtomicInteger();
        try {
            runTogether(4, () -> {
                if (arrivals.incrementAndGet() == 3) {
                    throw new IllegalStateException("boom");
                }
            });
            System.out.println("harness reported: nothing");
        } catch (IllegalStateException e) {
            System.out.println("harness reported: " + e.getMessage());
        }
    }
}
```

<!-- expected-output -->
```text
counter: 80000
harness reported: boom
```

A stress test can show that a solution is wrong, but it cannot show that a solution is right: a run that passes proves nothing about an interleaving that did not happen. Use the happens-before rules to argue that a solution is correct, and use tests (and tools such as jcstress, linked below) to look for the failures you did not think of.

## Interview Angle

### Questions

??? question "Write code that prints odd and even numbers alternately with two threads."

    Keep the next number in a field that one monitor protects. The odd thread waits in a `while` loop while the next number is even. The even thread waits while it is odd. A thread that has its turn prints the number, increments it, and calls `notifyAll`. At the end, the last thread calls `notifyAll` so that the other thread can stop.

    Say why `while`: after `wait()` returns, the condition may be false again, because of a spurious wakeup or another thread. Say why `notifyAll`: it is the safe default when threads wait for different conditions. The alternative is two semaphores that pass the turn. The full program is in [Problem 1](#problem-1-print-odd-and-even-numbers-with-two-threads).

??? question "Three threads call `first()`, `second()`, and `third()` in any order. How do you make them run in order?"

    Use two `CountDownLatch(1)` objects. `first()` runs and counts down the first latch. `second()` awaits the first latch, runs, and counts down the second latch. `third()` awaits the second latch and runs. The latch is a hand-off, so the order does not depend on timing, and the `countDown` happens-before the return of `await`, so the effects of the earlier method are visible.

    Do not use `Thread.sleep` (a guess about timing) or a non-volatile flag with a loop (invisible writes, wasted CPU). For repeated rounds, such as `ABCABC`, use one `Semaphore` for each thread in a ring with a single starting permit.

??? question "Implement a bounded blocking queue."

    Use a ring buffer and one `ReentrantLock` with two conditions: `notFull` and `notEmpty`. `put` waits on `notFull` while the buffer is full, stores the item, and signals `notEmpty`. `take` waits on `notEmpty` while it is empty, removes the item, and signals `notFull`. Every wait is in a `while` loop, and all state is changed under the lock.

    Explain the two conditions: with one condition, a `signal` can wake a waiter of the same kind, and the other kind never wakes (a lost wakeup). `ArrayBlockingQueue` has this design. `LinkedBlockingQueue` uses two locks, so that producers and consumers do not block each other. See [Problem 4](#problem-4-bounded-blocking-queue-from-scratch).

??? question "Why must `wait()` be called in a loop, and why does `notify` need care?"

    A thread that returns from `wait()` has no guarantee that its condition is true. The `Object.wait` javadoc names the spurious wakeup, and in addition another thread can take the item first. So the thread must re-check the condition in a `while` loop.

    `notify` wakes one arbitrary thread from the wait set. If producers and consumers share one wait set, `notify` can wake a thread of the wrong kind, which goes back to waiting, and the thread that should have woken never does (a lost wakeup). Use `notifyAll`, or use two `Condition` objects so that each kind of waiter has its own queue. See the guarded-block section in [Concurrency Basics](07-concurrency-basics.md).

??? question "How do you stop the consumers of a producer-consumer system?"

    The usual way is a poison pill: after all producers end, the main thread puts one special item in the queue for each consumer, and a consumer that takes the pill returns. The pills come after all real items, so the consumers drain the queue first. Another way is to interrupt the consumers, and a consumer that is blocked in `take()` then gets an `InterruptedException`. A third way, with a known total count, is an atomic counter that tells each consumer how many items remain.

    Do not use `while (!queue.isEmpty())`: the queue can be empty while a producer still runs, and the consumer can block in `take()` after it passed the check.

??? question "Implement a reader-writer lock. What happens to writers?"

    One monitor holds `activeReaders`, `writing`, and `waitingWriters`. A reader waits while a writer writes (and, for writer priority, while a writer waits). A writer waits while a writer writes or readers read. The last reader to leave, and every writer that leaves, calls `notifyAll`.

    The trade-off is starvation. A reader-preference lock lets new readers in while a writer waits, so a steady stream of readers starves the writer. Counting `waitingWriters` and making new readers wait gives the writer priority, and then readers can wait behind a writer. The JDK class is `ReentrantReadWriteLock`. It allows a downgrade (write lock, then read lock, then release the write lock) but not an upgrade: a thread that holds the read lock and asks for the write lock waits for itself forever. Release the read lock first, take the write lock, and re-check the state.

??? question "Write a thread-safe singleton. Why does double-checked locking need `volatile`?"

    Best choices: the initialization-on-demand holder (a nested class with a `static final` instance, which the JVM initializes once under the class initialization lock), or an enum with one constant (also safe against serialization and reflection). Double-checked locking also works, with a `volatile` field.

    `instance = new Service()` is allocate, construct, store. Without `volatile`, the memory model lets another thread see the stored reference before the constructor's writes, so the first unsynchronized check can return a half-built object. A volatile write happens-before every later volatile read of that field, so the reader sees a built object. See [Concurrency Basics](07-concurrency-basics.md).

??? question "Solve the dining philosophers problem. Which deadlock condition do you remove?"

    The wrong solution takes the left fork and then the right fork with blocking locks. If all philosophers take the left fork at once, each waits for the next one: a circular wait, so the program deadlocks. Remove the circular wait with lock ordering: number the forks and take the lower number first. The last philosopher then takes fork 0 before fork 4, and no cycle forms.

    Other solutions: a `Semaphore(4)` that lets at most four philosophers try to eat (at least one can then get both forks), or `tryLock` with a timeout and a release of the first fork if the second is not free (this removes hold and wait, and needs a random back-off to avoid livelock). Lock ordering is the cheapest, and it is the rule behind the account transfer fix in [Concurrency Basics](07-concurrency-basics.md). Dijkstra's original 1971 solution keeps a state for each philosopher (thinking, hungry, eating) under one mutex and lets a hungry philosopher eat only when neither neighbor eats.

    Name the limit: these solutions remove deadlock, not starvation. With non-fair locks, two neighbors can keep taking a fork before a waiting philosopher (Dijkstra noted the same for his solution). Fair locks (`new ReentrantLock(true)`) remove that, at a cost in throughput.

??? question "Design a rate-limited executor."

    Put a token bucket in front of a worker pool. The bucket stores credit that grows with time up to a capacity (the burst size). A task needs one token to start. One dispatcher thread takes tasks from a queue, waits until the bucket gives a token, and passes the task to the pool. The limit is on the start of tasks, in one place, with one clock.

    Make the clock injectable so that a test with a fake clock is deterministic. Do not throttle by sleeping in the workers: with *k* workers the rate is *k* times higher, and it depends on the task duration. For a limit across many servers, use a shared store (see the HLD page on rate limiting algorithms). A bounded queue in front of the dispatcher adds back pressure.

??? question "Build a thread pool from scratch."

    Use a bounded `BlockingQueue` for tasks and a fixed set of worker threads that loop on `take()`. A worker runs a task inside `try`/`catch (Throwable)` so that a failing task does not end the worker. `execute` rejects a task when the queue is full or the pool is shut down. `shutdown` sets a flag under the same lock as `execute`, and then puts one poison pill for each worker behind the accepted tasks, so that they all run. `awaitTermination` joins the workers.

    Then name what `ThreadPoolExecutor` adds: core and maximum size, keep-alive, rejection policies, `Future`, `shutdownNow`, and hooks. Say that the common mistakes are a non-thread-safe queue, a non-volatile shutdown flag, a busy loop, and dropped tasks on shutdown.

??? question "How do you test concurrent code, and why is a passing test not a proof?"

    Start many threads at the same moment (a latch as a start gate), run the code under test many times, join the threads, and check the invariants after the join (no lost update, no torn read, size within the capacity). Collect exceptions from the threads, because an exception only ends its own thread and the test would pass. Run the test on several CPU counts.

    A pass proves that the interleavings that happened were correct. It does not prove the rest, and a failure such as the unsafe publication in double-checked locking may never show in a test. So also argue correctness with the happens-before rules, and use tools such as jcstress for memory-model questions.

### Follow-up Questions

??? question "Why does the bounded queue use `signal` and not `signalAll`?"

    There are two conditions, and each state change makes room for exactly one waiter of the other condition: a `put` adds one item, so one consumer can proceed. Every waiter of one condition waits for the same thing, so any one of them can take the item. `signalAll` would also be correct, but it wakes threads that then wait again. With a single condition for both kinds, `signal` is wrong.

??? question "Why does the demo `SimpleReadWriteLock` call `notifyAll` in `unlockRead` only when the count reaches zero?"

    The only threads that wait for readers to leave are writers, and a writer can proceed only when `activeReaders` is zero. A `notifyAll` for every leaving reader wakes threads that cannot proceed yet. The check keeps the same behavior with fewer wake-ups.

??? question "Is the enum singleton lazy?"

    The enum constant is created when the enum class is initialized, and the JVM initializes a class when the code first uses it (JLS 12.4.1). So it is lazy at the level of the class: it is created at the first use of `EnumSingleton`. It cannot take a constructor argument from the caller, and it cannot extend another class.

??? question "What changes in these solutions if the threads are virtual threads?"

    The logic stays the same, because virtual threads follow the same memory model and use the same primitives. A blocked virtual thread unmounts, so a waiting thread costs no carrier. On Java 21, the monitor-based solutions differ: a virtual thread that blocks to enter a `synchronized` block pins its carrier, and `Object.wait` also pins it (the JDK compensates for `wait` by adding a carrier for as long as the wait blocks the carrier, JEP 444). `ReentrantLock` with `Condition` unmounts. JEP 491 removes both monitor cases in Java 24. Two more points: JEP 444 says not to pool virtual threads, so problem 9 stays a platform-thread exercise, and a `Semaphore` (not a pool size) limits concurrency. See [Virtual Threads](10-virtual-threads.md).

### Red Flags

- `Thread.sleep` to order threads or to "wait for the other thread".
- `if` around `wait()` or `await()`.
- `notify()` where producers and consumers share one wait set.
- A shared flag without `volatile` in a spin loop (`while (!ready) { }`).
- Double-checked locking without `volatile`.
- Taking two locks in different orders in different code paths.
- A `catch (InterruptedException e) { }` that swallows the interrupt, in a worker loop.
- A queue without a bound in a producer-consumer design with no back-pressure story.
- Calling a solution "tested" because it passed one run.

## Common Mistakes

| Mistake | Why it fails | Fix |
|---|---|---|
| `if` instead of `while` around a wait | A spurious wakeup, or another thread, makes the condition false again | `while (!condition) wait();` |
| Signal before the state change | The waiter wakes, checks the old state, and waits again, and no second signal comes | Change the state, then signal |
| One condition and `signal` for two kinds of waiter | The signal wakes the wrong kind. The right kind waits forever | Two `Condition` objects, or `notifyAll`/`signalAll` |
| Print from threads and expect a fixed order without a protocol | The scheduler decides the order | A hand-off protocol (latch, semaphore, turn variable), or collect results and print after `join` |
| Stop consumers with `isEmpty()` | The queue is empty for a moment while producers still work | Poison pills, or interrupt |
| Hold a lock while you call unknown code or block | Other threads wait, and a deadlock can form | Copy the state under the lock, and call outside it |
| Forget the `finally` that releases a `Lock` | An exception leaves the lock held, and every other thread blocks | `lock(); try { ... } finally { unlock(); }` |
| Swallow `InterruptedException` | The thread cannot be stopped | Restore the flag with `Thread.currentThread().interrupt()`, or end the loop |
| Let an exception end a pool worker | The pool loses threads one by one | Catch `Throwable` around the task in the worker loop |
| Test only with one run | An interleaving that did not occur is untested | Many threads, many repetitions, a start gate, and an argument with happens-before |

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| `notify` and `notifyAll` | `notify` wakes one arbitrary waiter, and `notifyAll` wakes all of them. Each woken thread then competes for the monitor | Do all waiters wait for the same condition? |
| `wait()` and `Thread.sleep()` | `wait` releases the monitor and ends on a notify. `sleep` keeps all locks and ends on a time | Am I waiting for a condition or for time? |
| `CountDownLatch` and `CyclicBarrier` | A latch opens once and is not reusable. A barrier makes a group of threads wait for each other and resets | Does one party wait for events, or do all parties wait for each other? |
| `Semaphore` and `ReentrantLock` | A semaphore counts permits, and any thread can release. A lock has an owner thread | Do I need an owner, or only a count? |
| `signal()` and `signalAll()` | Same difference as `notify`/`notifyAll`, but per `Condition` | Is each `Condition` shared by waiters with the same wait condition? |
| Deadlock and livelock | Deadlock: threads wait for each other and do nothing. Livelock: threads keep changing state and make no progress | Does the CPU stay busy while no work completes? |
| Lock ordering and `tryLock` | Ordering prevents the circular wait. `tryLock` with a timeout detects the failure and backs off | Can every path use one global order? |
| Singleton by holder and by enum | Both are initialized by the JVM. The enum also stops serialization and reflection from making a second instance | Do I need protection from serialization and reflection? |

## Quick Reference Card

- Five steps for any problem: name the shared state, name each wait condition, choose the primitive, write the guarded wait with `while` and change the state before you signal, say how it ends.
- Wait in a `while` loop. Spurious wakeups and competing threads make `if` wrong.
- Two kinds of waiter: two `Condition` objects, or `notifyAll`. Do not use `notify` with one wait set for both kinds.
- Order of steps: `CountDownLatch`. Ring of turns: one `Semaphore` for each thread.
- Producer-consumer: bounded `BlockingQueue`, poison pill for each consumer.
- Bounded queue from scratch: ring buffer, one lock, `notFull` and `notEmpty`.
- Reader-writer: one monitor, `activeReaders`, `writing`, `waitingWriters`. Choose reader or writer priority and name the starvation.
- Singleton: holder idiom or enum. Double-checked locking needs `volatile`.
- Dining philosophers: lock ordering removes the circular wait (not starvation; fair locks for that). Alternatives: `Semaphore(4)`, Dijkstra's state-based solution, `tryLock` with back-off.
- Rate limiter: token bucket, one place, injected clock. Do not sleep in the workers.
- Thread pool: thread-safe bounded queue, workers that survive task exceptions, pills for shutdown, a lock shared by `execute` and `shutdown`.
- Deterministic demo: join before print, no thread names, no times except a guaranteed lower bound.

## Further Reading

- [`Object.wait` javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html). The spurious wakeup warning and the loop idiom.
- [`java.util.concurrent` package javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html). The memory consistency properties of latches, queues, and locks.
- [`Condition` javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/Condition.html). The `await` and `signal` contract, with a bounded buffer example.
- [`ArrayBlockingQueue` source (JDK 21)](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/concurrent/ArrayBlockingQueue.java). One lock and two conditions, the design of problem 4.
- [`LinkedBlockingQueue` source (JDK 21)](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/concurrent/LinkedBlockingQueue.java). Two locks, one for each end of the queue.
- [`ThreadPoolExecutor` javadoc (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html). The full pool that problem 9 imitates.
- [JLS 12.4.2: Detailed Initialization Procedure](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2). The class initialization lock behind the holder idiom.
- [JLS chapter 17: Threads and Locks](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html). The memory model, happens-before, and why double-checked locking needs `volatile`.
- [Manson, Goetz. "JSR 133 (Java Memory Model) FAQ"](https://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html). Double-checked locking explained.
- [jcstress](https://openjdk.org/projects/code-tools/jcstress/). The OpenJDK harness for memory-model outcomes.
- Goetz, Bloch, Bowbeer, Lea, Holmes, Peierls. *Java Concurrency in Practice*. Addison-Wesley, 2006. Chapter 14 (condition queues, the guarded wait), chapter 5 (blocking queues, latches), and chapter 10 (deadlock).
- Bloch. *Effective Java*, 3rd edition. Addison-Wesley, 2018. Item 3 (singleton with an enum), Item 81 (prefer concurrency utilities to `wait` and `notify`), Item 83 (lazy initialization).
- Coffman, Elphick, Shoshani. "System Deadlocks". *ACM Computing Surveys* 3(2), 1971. The four conditions that the dining philosophers solution breaks.
- Dijkstra. "Hierarchical ordering of sequential processes". *Acta Informatica* 1(2), 115–138, 1971 ([EWD310 transcription](https://www.cs.utexas.edu/~EWD/transcriptions/EWD03xx/EWD310.html)). The origin of the dining philosophers problem: it rejects "left fork, then right fork" because of the deadly embrace (deadlock), gives the state-based solution, and notes that it can starve a philosopher.
- [Concurrency Basics](07-concurrency-basics.md) and [java.util.concurrent](08-java-util-concurrent.md). The primitives that these solutions use.
- [Virtual Threads](10-virtual-threads.md). What changes when the threads are virtual.
- [Rate Limiting Algorithms](../hld/fundamentals/13-rate-limiting-algorithms.md). The algorithms behind problem 8, and the distributed version.
- [Concurrency in LLD](../lld/fundamentals/09-concurrency-in-lld.md). How to use these primitives in a design round.
