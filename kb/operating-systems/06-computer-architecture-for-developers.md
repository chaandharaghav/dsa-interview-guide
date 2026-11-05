---
priority: P2
---
# Computer Architecture for Developers

## Why It Matters

Two loops that do the same work can differ in speed by a large factor, because one loop reads memory in the order that the CPU caches favor and the other does not. Interviews ask about this in questions such as "Why is it faster to traverse a matrix row by row?", "What is false sharing, and how do you fix it?", "Why does `volatile` exist if the CPU has cache coherence?", "Why is an `ArrayList` usually faster than a `LinkedList`?", and "What is branch prediction?". A candidate who says "caches are fast" gets partial credit. A candidate who says that the CPU moves memory in lines of 64 bytes on x86-64, that coherence works per line, that two counters on one line make two cores fight over it, and that a store buffer lets a CPU reorder a store after a later load, gets full credit. On the job, the same knowledge explains why a `LongAdder` beats an `AtomicLong` under contention, why a thread pool with more threads can run slower, why a request counter slows a service when the threads write to neighboring fields, and why the Java Memory Model forbids code that works on your x86 laptop. This page covers the memory hierarchy, cache lines and associativity, locality, cache coherence and false sharing, memory ordering, pipelining and branch prediction, speculative execution, SIMD, NUMA, and the consequences for data structures. The Java Memory Model is in [Concurrency Basics](../java/07-concurrency-basics.md). Atomic operations and locks at the instruction level are in [Synchronization](03-synchronization.md). The TLB and huge pages are in [Memory Management](04-memory-management.md). The scheduler side of NUMA and CPU affinity is in [CPU Scheduling](02-cpu-scheduling.md).

## Core Concepts

**Memory hierarchy**: a stack of memories where each level is larger and slower than the one above it: registers, the L1 cache, the L2 cache, the L3 cache, main memory (DRAM), and storage (SSD or disk).

**Cache**: a small, fast memory that holds copies of recently used data from a slower memory. A **hit** means that the data is in the cache. A **miss** means that the CPU must fetch it from the next level.

**Cache line (cache block)**: the unit that a cache stores and moves. A cache never loads one byte. It loads a whole line. On x86-64 the line is 64 bytes in the processors that this page considers (see the facts below).

**Locality**: the tendency of a program to reuse data. **Temporal locality**: a program that used an address will use it again soon. **Spatial locality**: a program that used an address will use a nearby address soon. Caches work because programs have both.

**Associativity**: the number of places in a cache where one memory line may go. In a **direct-mapped** cache each line has exactly one place. In an **N-way set-associative** cache the line may go to any of N places in one set. In a **fully associative** cache the line may go anywhere.

**Set, index, tag**: the cache divides the address into three fields. The low bits are the offset inside the line. The next bits are the index that selects a set. The remaining high bits are the tag that identifies the line inside the set.

**Cache coherence**: the property that all cores see a consistent value of each memory location, although each core has its own cache. The protocol keeps one writer or many readers for each line. **MESI**: a common coherence protocol with four line states: Modified, Exclusive, Shared, Invalid.

**False sharing**: two cores write different variables that live in the same cache line, so the line moves between the cores although the program shares no data.

**Memory ordering (memory consistency model)**: the rules that say in which order the memory operations of one core can become visible to other cores. **Memory barrier (fence)**: an instruction that forbids some reorderings across it.

**Store buffer**: a queue inside a core that holds stores that have not yet reached the cache, so that the core does not wait for them.

**Pipeline**: the CPU splits an instruction into stages and works on several instructions at once. **Branch prediction**: the CPU guesses the outcome of a conditional branch so that the pipeline stays full. **Speculative execution**: the CPU runs instructions after a predicted branch before it knows that the prediction is right.

**SIMD (single instruction, multiple data)**: an instruction that applies one operation to several values in one register.

**NUMA (non-uniform memory access)**: a machine where memory is attached to specific CPU sockets, and access to the memory of another socket takes longer.

## How It Works

### The memory hierarchy and its latencies

Each level trades size for speed. The numbers below come from [Back-of-Envelope Estimation](../hld/back-of-envelope-estimation.md), which uses the table "Numbers Everyone Should Know" from Jeff Dean (LADIS 2009). They describe hardware from about 2009, and they are **approximate**. Use them for ratios, not for a prediction on a current CPU.

| Level | Latency (approximate, Dean, LADIS 2009) | Ratio to L1 |
|---|---|---|
| L1 cache reference | 0.5 ns | 1 |
| Branch mispredict | 5 ns | 10 |
| L2 cache reference | 7 ns | 14 |
| Mutex lock and unlock | 25 ns | 50 |
| Main memory reference | 100 ns | 200 |
| SSD random read | 16,000 ns (16 µs) | 32,000 |
| Disk seek | 10,000,000 ns (10 ms) | 20,000,000 |

The SSD value is from the model of Colin Scott's page "Numbers Every Programmer Should Know By Year", which the estimation page describes. Dean's slide has no row for an L3 cache and no row for an SSD. The L3 cache lies between L2 and main memory, and its latency depends on the CPU, so this page gives no number for it.

A second source gives the same shape in cycles. Drepper, "What Every Programmer Should Know About Memory" (2007), quotes the numbers that Intel listed for a Pentium M: a register at most 1 cycle, L1 data cache about 3 cycles, L2 about 14 cycles, and main memory about 240 cycles. These are old numbers too. The lesson is the same in both tables: main memory costs about 80 times an L1 hit in Drepper's cycles (240 / 3) and about 200 times in Dean's table (100 ns / 0.5 ns), and a program that misses the cache on every access runs at the speed of main memory.

!!! note "Cycles and nanoseconds"
    A CPU at 3 GHz runs 3 cycles in 1 ns. At 2 GHz, 240 cycles are 120 ns, which is the same order as the 100 ns of Dean's table. Do not mix the two tables in one calculation.

### Cache lines

A cache moves data in lines. When a program reads one `int`, the CPU loads the whole line that holds it. The line size is a property of the CPU, not of the language:

- **x86-64**: 64 bytes in the processors that this page considers. The Intel optimization manual lists a 64-byte line for the 32 KB, 8-way L1 data cache of the Skylake family. The Linux kernel sets `CONFIG_X86_L1_CACHE_SHIFT` to 6 for `X86_64` (`arch/x86/Kconfig.cpu`), which is 64 bytes. Drepper uses a 64-byte line in his examples. On some Intel cores, the adjacent-line prefetcher fetches two neighboring lines together, so contention can behave as if the line had 128 bytes (the `perf c2c` man page has the option `--double-cl` for this reason).
- **Arm**: the line size depends on the core. The kernel for arm64 defines `L1_CACHE_BYTES` as 64 and `ARCH_DMA_MINALIGN` as 128 (`arch/arm64/include/asm/cache.h`). The kernel comment says that the DMA alignment exists so that memory for a DMA buffer never shares a cache line with unrelated data, so the 128 is a conservative upper bound for all supported cores. The register `CTR_EL0` reports the line sizes of the CPU: its field `DminLine` holds the base-2 logarithm of the number of 4-byte words in the smallest data cache line, and the kernel reads it at boot. On macOS, `sysctl hw.cachelinesize` reports 128 on Apple M1 (the Go project changed its M1 constant from 64 to 128 for this reason, golang/go#53075). So the Arm line is 64 bytes on the common server cores that the kernel assumes, and 128 bytes on Apple M1 under macOS.
- **The JVM**: the HotSpot flag `ContendedPaddingWidth` has the default value 128 bytes (JDK 21.0.7 source, `globals.hpp`). The JVM pads by 128 bytes, not by 64, so the padding also works with a 128-byte line and with an adjacent-line prefetcher.

Recommendation: do not hard-code 64 in code that must run on both x86-64 and Arm. Pad to 128 bytes or ask the platform.

#### How an address maps to a cache location

The cache splits an address into offset, index, and tag. For the cache that the simulator below uses (32 KB, 64-byte lines, 8-way), the arithmetic is:

```text
cache size 32 KB / line size 64 B     = 512 lines
512 lines / 8 ways                    = 64 sets
offset  = low 6 bits  (64-byte line)
index   = next 6 bits (64 sets)
tag     = all higher bits

address:  | tag (the rest) | index (6 bits) | offset (6 bits) |
```

The address `A` goes to set `(A / 64) mod 64`. The tag compare checks the 8 lines of that set. Two addresses that are `64 * 64 = 4096` bytes apart fall into the same set. A stride of 4096 bytes, or of any multiple of it, uses very few sets, and the lines evict each other although the cache has room. This is a **conflict miss**.

The three kinds of miss (the "3C" model of Mark Hill, which Hennessy and Patterson use):

| Miss | Cause | Fix |
|---|---|---|
| Compulsory | The first access to a line | Prefetch, or accept it |
| Capacity | The working set is larger than the cache | Reduce the working set, or block the loops |
| Conflict | Too many lines map to one set | Change the stride, pad the data, or raise associativity |

A cache that writes a modified line back to memory only when it evicts the line is a **write-back** cache. Drepper (section 3.3.3) describes the write-back policy, and most x86 and Arm data caches use it for normal memory. A cache that uses the **write-allocate** policy loads the line first on a write miss, so a write miss costs a line read. Hennessy and Patterson describe both policies. The policy is a property of each cache and memory type, so read the processor manual before you rely on it.

### Locality and the order of traversal

The CPU loads a 64-byte line on each miss. A program that reads an array of `int` in order gets 16 values from each line (64 / 4). So the first access to each line misses, and the next 15 hit: a miss rate of 1/16, that is 6.25 percent. A program that jumps over memory with a large stride gets a hit rate near zero, because each access needs a new line.

A 2D array in Java is an array of arrays: `int[][] a` holds references to row arrays (JLS chapter 10: the component of an array can itself be an array). Each row is one contiguous block. The rows are separate objects, and the JVM does not promise that two rows are adjacent. So `a[i][j]` and `a[i][j + 1]` are neighbors in memory, and `a[i][j]` and `a[i + 1][j]` are not. Row-major traversal (the inner loop on `j`) reads in memory order. Column-major traversal (the inner loop on `i`) jumps from row to row.

The simulator below counts the misses of a set-associative LRU cache for both orders. It models a 32 KB, 8-way cache with 64-byte lines. It does not measure time. The matrices hold 4-byte `int` values, and row `r`, column `c` lives at byte `(r * rowLength + c) * 4`. The last case pads each row by 16 ints (64 bytes).

```java
public class Main {
    /** A set-associative cache with LRU replacement. It counts hits and misses. */
    static final class Cache {
        final int lineBytes;
        final int sets;
        final int ways;
        final long[][] tags;   // tags[set][way], -1 means empty
        final long[][] lastUse;
        long clock;
        long hits;
        long misses;

        Cache(int sizeBytes, int lineBytes, int ways) {
            this.lineBytes = lineBytes;
            this.ways = ways;
            this.sets = sizeBytes / (lineBytes * ways);
            this.tags = new long[sets][ways];
            this.lastUse = new long[sets][ways];
            for (long[] row : tags) {
                java.util.Arrays.fill(row, -1);
            }
        }

        void access(long address) {
            long line = address / lineBytes;
            int set = (int) (line % sets);
            long tag = line / sets;
            clock++;
            int victim = 0;
            for (int w = 0; w < ways; w++) {
                if (tags[set][w] == tag) {
                    hits++;
                    lastUse[set][w] = clock;
                    return;
                }
                if (tags[set][w] == -1) {
                    victim = w;
                    lastUse[set][w] = Long.MIN_VALUE;   // an empty way is always the first victim
                } else if (lastUse[set][victim] != Long.MIN_VALUE
                        && lastUse[set][w] < lastUse[set][victim]) {
                    victim = w;
                }
            }
            misses++;
            tags[set][victim] = tag;
            lastUse[set][victim] = clock;
        }
    }

    static final int INT_BYTES = 4;

    /** Walks an n x n matrix of int. rowLength is the number of ints that one row occupies. */
    static void walk(String label, int n, int rowLength, boolean rowMajor) {
        Cache cache = new Cache(32 * 1024, 64, 8);
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                int row = rowMajor ? a : b;
                int col = rowMajor ? b : a;
                cache.access(((long) row * rowLength + col) * INT_BYTES);
            }
        }
        long total = cache.hits + cache.misses;
        System.out.printf("%-46s accesses=%7d misses=%7d miss rate=%6.2f%%%n",
                label, total, cache.misses, 100.0 * cache.misses / total);
    }

    public static void main(String[] args) {
        System.out.println("Cache: 32 KB, 64-byte lines, 8-way, LRU (512 lines, 64 sets)");
        System.out.println("Matrix of 4-byte ints, stored row by row");
        walk("64 x 64   row-major", 64, 64, true);
        walk("64 x 64   column-major", 64, 64, false);
        walk("512 x 512 row-major", 512, 512, true);
        walk("512 x 512 column-major", 512, 512, false);
        walk("512 x 512 column-major, rows padded by 16", 512, 528, false);
    }
}
```

<!-- expected-output -->
```text
Cache: 32 KB, 64-byte lines, 8-way, LRU (512 lines, 64 sets)
Matrix of 4-byte ints, stored row by row
64 x 64   row-major                            accesses=   4096 misses=    256 miss rate=  6.25%
64 x 64   column-major                         accesses=   4096 misses=    256 miss rate=  6.25%
512 x 512 row-major                            accesses= 262144 misses=  16384 miss rate=  6.25%
512 x 512 column-major                         accesses= 262144 misses= 262144 miss rate=100.00%
512 x 512 column-major, rows padded by 16      accesses= 262144 misses=  16384 miss rate=  6.25%
```

How to read the output:

- **64 x 64.** The matrix has 4,096 ints, 16 KB. It fits in the 32 KB cache. Both orders miss only once for each line: 256 lines, 6.25 percent. When the data fits, the order does not matter.
- **512 x 512, row-major.** The matrix is 1 MB, larger than the cache. Each line holds 16 ints and the loop reads them in order, so each line misses once: 16,384 misses, 6.25 percent.
- **512 x 512, column-major.** Each row is 512 × 4 = 2,048 bytes, which is 32 lines. A walk down a column moves 32 lines at each step. The set index is `line mod 64`, so the walk touches only 2 different sets (line numbers 0, 32, 64, ... map to sets 0 and 32). Two sets with 8 ways hold 16 lines, but one column needs 512. By the time the walk returns to the first row for the next column, LRU has evicted the line. Every access misses: 100 percent. This is a mix of a capacity miss and a conflict miss, and the stride of a power of two causes the conflict.
- **Padded rows.** With 16 extra ints, a row takes 2,112 bytes, which is 33 lines. Now consecutive rows move the set index by 33 (mod 64). Because 33 and 64 have no common factor, the 512 rows spread over all 64 sets, 8 rows for each set. A column then fits exactly in the 8 ways of each set, and the next column finds its lines in the cache (a line holds 16 ints, so 16 consecutive columns share lines). The miss rate falls from 100 percent to 6.25 percent. This is why code that processes a power-of-two sized matrix sometimes pads its rows.

The model has no hardware prefetcher, and real CPUs have one. Drepper (section 6.3) describes hardware prefetching: the processor loads the next line when it detects a sequential pattern. A real CPU therefore hides part of the cost of row-major traversal, and it cannot hide the cost of a stride that jumps by a large step. The model also uses LRU, and real caches use approximations. Use the output to see the mechanism, not to predict a time.

**Blocking (tiling)** is the standard fix for a traversal that cannot change its order, such as a matrix transpose. The program processes the matrix in small square tiles that fit in the cache, so each loaded line is used many times before eviction.

### Cache coherence and MESI

Each core has its own L1 and L2 cache. If two cores cache the same line and one writes it, the other core must not read the old value. A **coherence protocol** keeps the copies consistent. Drepper describes MESI, and each line in a cache has one of four states:

| State | Meaning (Drepper, section 3.3.4) |
|---|---|
| Modified | The local processor modified the line. It is the only copy in any cache |
| Exclusive | The line is not modified, and no other processor's cache has it |
| Shared | The line is not modified, and other caches might have it |
| Invalid | The line is not valid (unused) |

A read of a line that no cache has loads it as Exclusive. A read of a line that another cache has makes both copies Shared. A write to a Shared line first sends a message that makes all other copies Invalid (the "request for ownership"), and then the writer holds the line as Modified. A write to a line that another core holds as Modified makes that core send the line to the writer and drop its copy. Coherence works on whole lines, not on single variables.

This has a cost that a program sees: a line that two cores write in turn moves between the caches at each write. The data and the message travel between the cores, and each move takes much longer than an L1 hit.

### False sharing

**False sharing** means that two cores write different variables that sit in the same line. The program has no data race and no shared variable, yet each write invalidates the copy of the other core, and the line moves between the cores at each write.

Drepper (section 6.4.1) measured it on a machine with four Pentium 4 processors. Each thread incremented its own variable 500 million times. When the variables were on separate lines, the program scaled without a penalty. When all the variables shared one line, Drepper computed the overhead as the run time with one shared line divided by the run time with separate lines. The result was 390 percent for two threads, 734 percent for three threads, and 1,147 percent for four threads: the shared-line runs took about 3.9, 7.3, and 11.5 times as long. These numbers are from 2007 hardware, with threads pinned to cores. Treat them as approximate, and note that each extra core adds delay, because the line passes through all of them.

The second program counts the line transfers of the invalidate protocol for two cores that alternate writes. It models the protocol in a simplified way: a write to a line that the other core holds invalidates that copy, and each such event counts as one transfer.

```java
public class Main {
    /** Two cores with private caches that use an invalidate protocol (a simplified MESI).
     *  A core can write a line only if no other core holds it. A write that finds the line in
     *  another core makes that core drop its copy: this is one coherence transfer. */
    static final class Machine {
        final int lineBytes;
        final long[] holder = new long[2];   // line number held by each core, -1 if none
        long transfers;

        Machine(int lineBytes) {
            this.lineBytes = lineBytes;
            holder[0] = -1;
            holder[1] = -1;
        }

        void write(int core, long address) {
            long line = address / lineBytes;
            int other = 1 - core;
            if (holder[other] == line) {
                holder[other] = -1;     // invalidate the copy of the other core
                transfers++;
            }
            holder[core] = line;
        }
    }

    static void run(String label, long addressOfCore1) {
        Machine m = new Machine(64);
        int rounds = 1000;
        for (int i = 0; i < rounds; i++) {
            m.write(0, 0);               // core 0 increments its counter at address 0
            m.write(1, addressOfCore1);  // core 1 increments its counter
        }
        System.out.printf("%-34s writes=%d line transfers=%d%n", label, 2 * rounds, m.transfers);
    }

    public static void main(String[] args) {
        System.out.println(
                "Counters are 8-byte longs. Cache line: 64 bytes. Cores alternate writes.");
        run("Counters 8 bytes apart (one line)", 8);
        run("Counters 64 bytes apart (padded)", 64);
    }
}
```

<!-- expected-output -->
```text
Counters are 8-byte longs. Cache line: 64 bytes. Cores alternate writes.
Counters 8 bytes apart (one line)  writes=2000 line transfers=1999
Counters 64 bytes apart (padded)   writes=2000 line transfers=0
```

With the counters in one line, every write after the first moves the line. With the counters 64 bytes apart, no write moves a line. The real machine adds the cost of each move (the model counts events and has no time).

#### Fixes for false sharing

| Fix | How it works | Cost |
|---|---|---|
| Padding | Put unused bytes between the hot fields so that each field has its own line | More memory. The size of a line differs between CPUs |
| `@jdk.internal.vm.annotation.Contended` | The JVM adds padding around the annotated field or class. The default padding is 128 bytes (`ContendedPaddingWidth`) | JDK internal API. The JVM honors the annotation on your classes only with `-XX:-RestrictContended`. The compiler needs `--add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED`, because `java.base` does not export the package to application code |
| Separate arrays or objects for each thread | Each thread writes only its own object, and the allocator places the objects far apart | The JVM does not promise the placement of two objects, so use this with the padding |
| Reduce the writes | Keep a per-thread local value and combine at the end | A different design. The reader sees a delayed total |
| `LongAdder` | Several cells, each padded with `@Contended`, so threads update different lines | The `sum()` is not an atomic snapshot (see [`java.util.concurrent`](../java/08-java-util-concurrent.md)) |

The JDK source of `@Contended` (`jdk.internal.vm.annotation.Contended`, JDK 21.0.7) says that the annotation expresses that objects or fields "are expected to encounter memory contention, generally in the form of 'false sharing'", that it "serves as a hint", and that its effects "will nearly always add significant space overhead to objects". The JVM flag `RestrictContended` has the description "Restrict @Contended to trusted classes" and the default value true (`globals.hpp`). The class file parser ignores the annotation when `RestrictContended` is true and the class is not privileged (`classFileParser.cpp`, the check `RestrictContended && !privileged`). The module descriptor of `java.base` exports the package only to a fixed list of JDK modules (`module-info.java`, JDK 21.0.7). `ConcurrentHashMap` uses `@Contended` on its counter cells, and `LongAdder` uses it on its cells (see [Collections Internals](../java/04-collections-internals.md)).

The class below shows the portable manual fix: spread the counters of several threads over one array with a stride of 16 `long` values (128 bytes). It has no `main`, because the speed difference needs a timing measurement that this KB does not make.

```java
import java.util.concurrent.atomic.AtomicLongArray;

/** Not run by the KB runner. Each thread uses its own slot, 128 bytes away from the next slot. */
class PaddedCounters {
    private static final int STRIDE = 16;   // 16 longs * 8 bytes = 128 bytes
    private final AtomicLongArray slots;

    PaddedCounters(int threads) {
        this.slots = new AtomicLongArray(threads * STRIDE);
    }

    void increment(int threadIndex) {
        slots.incrementAndGet(threadIndex * STRIDE);
    }

    long sum(int threads) {
        long total = 0;
        for (int i = 0; i < threads; i++) {
            total += slots.get(i * STRIDE);
        }
        return total;
    }
}
```

### Memory ordering and barriers

A core does not execute its loads and stores strictly in program order as other cores see them. The compiler and the CPU both reorder. The **memory model** of a CPU states which reorderings another core can observe.

#### x86-TSO

Sewell, Sarkar, Owens, Zappa Nardelli, and Myreen, "x86-TSO: A Rigorous and Usable Programmer's Model for x86 Multiprocessors" (Communications of the ACM, 2010) describe the model in terms of an abstract machine. Each hardware thread has a FIFO **store buffer** in front of a shared memory. Their description:

- A thread that reads must read its own most recent buffered write to that address, if there is one. Otherwise the read is satisfied from shared memory.
- A buffered write reaches shared memory later, in order.
- An `MFENCE` instruction flushes the store buffer of that thread.
- A `LOCK`ed instruction (a locked read-modify-write) takes a global lock, and at its end it flushes the store buffer. `XCHG` with a memory operand is implicitly locked.

The effect: on x86, the one reordering that another core can observe is that a load can complete before an earlier store to a different address has reached memory. Loads are not reordered with loads, and stores are not reordered with stores. The CACM paper shows this with the store-buffering test (called SB, for "store buffering", in the literature): thread 0 runs `x = 1; r1 = y`, and thread 1 runs `y = 1; r2 = x`, with `x = y = 0` at the start. Under sequential consistency the outcome `r1 = 0 and r2 = 0` cannot happen. On x86 it can, because each store waits in its buffer while the load reads the old value from memory. An `MFENCE` between the store and the load forbids it.

The third program enumerates every interleaving of this test in two models: sequential consistency (a write reaches memory at once) and a store buffer (a write reaches memory in a later step that the model chooses). It also shows the effect of a fence. It is a model of the idea, not a CPU simulator.

```java
import java.util.Set;
import java.util.TreeSet;

/** Store-buffer litmus test. Thread 0: x = 1; r1 = y. Thread 1: y = 1; r2 = x. Start: x = y = 0. */
public class Main {
    // State: pc[t] (0 = write, 1 = fence, 2 = read, 3 = done), buffered[t] (a write waits),
    // memory x and y, and the registers r1 and r2.
    static boolean useFence;
    static boolean useBuffer;
    static final Set<String> OUTCOMES = new TreeSet<>();

    static void explore(int[] pc, boolean[] buffered, int[] mem, int[] reg) {
        boolean moved = false;
        for (int t = 0; t < 2; t++) {
            int mine = t;          // the variable that thread t writes: 0 = x, 1 = y
            int other = 1 - t;     // the variable that thread t reads
            // Step 1: a buffered write reaches memory (only the store-buffer model has this step).
            if (buffered[t]) {
                moved = true;
                boolean[] b = buffered.clone();
                int[] m = mem.clone();
                b[t] = false;
                m[mine] = 1;
                explore(pc, b, m, reg);
            }
            // Step 2: the next instruction of thread t.
            if (pc[t] == 0) {
                moved = true;
                int[] p = pc.clone();
                p[t] = useFence ? 1 : 2;
                if (useBuffer) {
                    boolean[] b = buffered.clone();
                    b[t] = true;
                    explore(p, b, mem, reg);
                } else {
                    int[] m = mem.clone();
                    m[mine] = 1;
                    explore(p, buffered, m, reg);
                }
            } else if (pc[t] == 1 && !buffered[t]) {     // fence: waits until the buffer is empty
                moved = true;
                int[] p = pc.clone();
                p[t] = 2;
                explore(p, buffered, mem, reg);
            } else if (pc[t] == 2) {
                moved = true;
                int[] p = pc.clone();
                int[] r = reg.clone();
                p[t] = 3;
                r[t] = mem[other];                        // the read checks memory only
                explore(p, buffered, mem, r);
            }
        }
        if (!moved) {
            OUTCOMES.add("(" + reg[0] + "," + reg[1] + ")");
        }
    }

    static void run(String label, boolean buffer, boolean fence) {
        useBuffer = buffer;
        useFence = fence;
        OUTCOMES.clear();
        explore(new int[2], new boolean[2], new int[2], new int[2]);
        System.out.println(label + ": (r1,r2) in " + OUTCOMES);
    }

    public static void main(String[] args) {
        run("Sequential consistency      ", false, false);
        run("Store buffer (x86-TSO style)", true, false);
        run("Store buffer plus a fence   ", true, true);
    }
}
```

<!-- expected-output -->
```text
Sequential consistency      : (r1,r2) in [(0,1), (1,0), (1,1)]
Store buffer (x86-TSO style): (r1,r2) in [(0,0), (0,1), (1,0), (1,1)]
Store buffer plus a fence   : (r1,r2) in [(0,1), (1,0), (1,1)]
```

The thread reads only memory in this model, and that is correct for this test, because each thread reads the variable that the other thread writes, not its own.

#### Arm

Arm has a weaker model. The Arm documentation (the Learn page on memory consistency) states that, when there are no dependencies between ordinary loads and stores, "the CPU can reorder these operations." So on Arm, loads can pass loads, stores can pass stores, and a load can pass a store, unless a dependency or a barrier forbids it. The load-acquire instruction `LDAR` "prevents later memory accesses from reordering before it", and the store-release instruction `STLR` "prevents earlier memory accesses from reordering after it" (Arm Learn, thread synchronization examples). The instruction `DMB` is the data memory barrier.

The consequence: a program with a data race can behave on x86 as the author expected and break on Arm. Code that "works on my x86 server" and fails on Graviton or Apple Silicon is a known pattern. The reverse can also hold: a barrier that x86 does not need costs something on Arm.

#### Linux and the kernel barriers

The kernel document `memory-barriers.txt` states that "a CPU may actually perform the memory operations in any order it likes" as long as the single-thread result is the same. It defines `smp_wmb()` as "a partial ordering on stores only", `smp_rmb()` as "a partial ordering on loads only", and `smp_mb()` as "a partial ordering over both loads and stores". It describes ACQUIRE as a one-way barrier (later operations stay after it) and RELEASE as a one-way barrier (earlier operations stay before it). It also states that "the Linux kernel does not require systems to be multicopy atomic", which means that two cores can see the stores of a third core in different orders unless a general barrier intervenes.

#### Why the Java Memory Model exists

The JVM runs on x86-64, on Arm, and on other CPUs, and the JIT compiler reorders code as well. If Java promised the behavior of one CPU, it would be wrong on another. The JMM (JLS chapter 17) defines the guarantees in terms of happens-before relations, so that a correct program is correct on every CPU. The JVM chooses the barrier instructions for each CPU when it compiles `volatile`, `synchronized`, and the `java.util.concurrent` atomics. This page does not repeat the rules: they are in [Concurrency Basics](../java/07-concurrency-basics.md), and the atomic instructions (`lock cmpxchg`, `LDXR` and `STXR`, `CAS`) are in [Synchronization](03-synchronization.md). The point for this page: the store buffer is the hardware reason that a plain write may stay invisible to another core, and a fence or a locked instruction drains it.

### Pipelining, branch prediction, and speculative execution

A modern core does not finish one instruction before it starts the next. It splits the work into stages (fetch, decode, execute, memory access, write back) and works on several instructions at once. This is a **pipeline**. A conditional branch is a problem: the CPU does not know which instructions come next until the branch condition is computed. To keep the pipeline full, the CPU **predicts** the branch and fetches and executes the instructions of the predicted path. If the prediction is wrong (a **misprediction**), the CPU discards the work and restarts from the right path. Dean's table gives about 5 ns for a branch mispredict (approximate, 2009), 10 times an L1 reference. The cost is the work that the pipeline throws away.

A branch that follows a pattern (a loop that runs many times, a check that nearly always has the same result) predicts well. A branch that depends on random data predicts badly. The standard example is a loop that sums the values above a threshold in an array: with sorted data the branch is predictable, and with random data it is not. A program that replaces the branch with arithmetic (a conditional move, or a mask) avoids the misprediction, and a JIT compiler sometimes does that. This KB does not time the effect, because the effect depends on the CPU and on what the JIT generates. The honest advice: measure with a benchmark harness such as JMH before you rewrite a branch.

**Speculative execution** goes further. The CPU runs instructions beyond a predicted branch before it knows the result. If the prediction was right, the results are kept. If it was wrong, the CPU discards the register results. **Spectre** (Kocher et al., "Spectre Attacks: Exploiting Speculative Execution", IEEE S&P 2019) showed that the discarded work still leaves traces in the CPU state, mainly in the caches, and that an attacker can read the traces through timing. The abstract states that "speculative logic is unfaithful in how it executes, can access the victim's memory and registers, and can perform operations with measurable side effects", and that the attacks affect processors from Intel, AMD, and Arm. The paper describes two variants. Variant 1 exploits conditional branches: the attacker trains the predictor so that a bounds check is predicted as passed, and the CPU speculatively reads memory beyond the bounds. Variant 2 exploits indirect branches: the attacker trains the branch target buffer to send speculation to code that the attacker chooses. The paper states that these attacks break assumptions of operating system process separation, containerization, and JIT compilation. The consequences for developers: browsers and runtimes added mitigations, and OS and CPU vendors added others. Meltdown (Lipp et al., USENIX Security 2018) is a related attack: on affected CPUs, an out-of-order load from a kernel address puts the kernel data into a temporary register before the privilege check stops the instruction, and the cache keeps a trace that the attacker reads. Some mitigations cost speed (for example kernel page-table isolation, which fights Meltdown, and which [Processes and Threads](01-processes-and-threads.md) covers for system calls). Recommendation: in a multi-tenant system, do not run untrusted code next to secrets in the same process, and follow the mitigation guidance of your platform.

### SIMD

A SIMD instruction applies one operation to several values in a wide register. For example, one instruction adds 8 pairs of 32-bit integers when the register is 256 bits wide. Typical widths: 128 bits on Arm NEON (the AArch64 vector registers V0 to V31), 256 bits on x86 AVX2 (the `__m256` types of the Intel intrinsics), 512 bits on AVX-512 (the `__m512` type). Arm SVE and some other designs use other widths.

In Java, there are two routes to SIMD:

- **Automatic**: the HotSpot C2 compiler has an auto-vectorizer, called SuperWord (`src/hotspot/share/opto/superword.cpp` in the JDK source), that turns eligible loops over arrays into vector instructions. You do not control it, and a small change in the loop can switch it off.
- **Explicit**: the **Vector API** (`jdk.incubator.vector`) lets code express vector operations that the JIT compiles into the best instructions of the CPU. The API is still an **incubator** API. The JEP history on openjdk.org lists the incubator rounds from JEP 338 (JDK 16) through JEP 448 (JDK 21), JEP 508 (JDK 25), and JEP 529 (JDK 26). JEP 537, "Vector API (Twelfth Incubator)", is listed as Closed / Delivered in JDK 27. The JEP text states: "The Vector API will incubate until necessary features of Project Valhalla become available as preview features." So the API is still in incubation, with no date for a final form. In JDK 21 the module is `jdk.incubator.vector`, and you must enable it with `--add-modules jdk.incubator.vector` at compile time and at run time.

SIMD matters most for numeric loops, text search, and compression. It also depends on data layout: the data must sit in contiguous arrays, which is the same rule as for cache lines.

### NUMA

On a machine with several sockets, each socket has its own memory controller and its own memory. A core reads its **local** memory faster than the **remote** memory of another socket. The default Linux policy places a page on the node of the CPU that first touches it (the kernel document on NUMA memory policy: "local allocation"). A program that initializes all its data on one thread and then reads it from threads on all sockets makes most reads remote. Remedies: spread the initialization across the threads that use the data, set a policy with `numactl` or `set_mempolicy`, or pin threads to a node. The scheduler side (load balancing across nodes and CPU affinity) is in [CPU Scheduling](02-cpu-scheduling.md). The JDK 21 `java` documentation states that `-XX:+UseNUMA` is disabled by default. The same text says that the option is available only with the parallel collector, but JEP 345 added NUMA-aware allocation to G1 in JDK 14, so the sentence is out of date.

### What this means for data structures

| Structure | Memory layout | Cache behavior |
|---|---|---|
| `int[]` | One contiguous block of 4-byte values | A line holds 16 values. Sequential access hits 15 of 16 times, and the hardware prefetcher helps |
| `ArrayList<Integer>` | A contiguous array of references, and each `Integer` is a separate object | The reference array is dense, but each element read can follow a pointer to an object somewhere else in the heap |
| `LinkedList<Integer>` | A node object for each element, and each node points to the next node and to an `Integer` object | Each step follows a pointer, and the next node can be anywhere in memory, so each step can miss |
| `HashMap` | An array of buckets that point to nodes | A lookup follows a bucket pointer and a node pointer. A miss at each step costs main memory latency |
| Array of structures against structure of arrays | One array of objects, or several parallel arrays | A loop that uses one field reads less memory with parallel arrays |

[Memory and Garbage Collection](../java/06-memory-and-gc.md) gives the Java object layout that makes this cost concrete: on a 64-bit HotSpot JDK 21, the object header takes 12 bytes, and an `Integer` takes 16 bytes (12 header bytes and a 4-byte `int`). So a list of one million `Integer` values uses a reference array of about 4 MB (with compressed references) plus about 16 MB of `Integer` objects, while an `int[]` of one million values uses about 4 MB. The array has 4 times less memory and 16 values in each cache line, while the list reads at best one 16-byte object after another, and the objects can be far apart in the heap, because the allocation order and the garbage collector decide their positions. Dean's table makes the point with the ratio: a pointer step that misses costs about 100 ns, and a hit in L1 costs about 0.5 ns.

This is why `ArrayList` beats `LinkedList` in most Java benchmarks even for operations where the textbook says that the linked list wins (the textbook counts steps, not cache misses). It is also why a **pointer-chasing** structure (a linked list, a tree with a node for each key, a skip list) is slower than a structure with a contiguous layout (an array, a B-tree with wide nodes, an open-addressing hash table) in the range where the data does not fit in the cache. For databases, the same effect is one reason for B-trees with wide nodes. [Collections Internals](../java/04-collections-internals.md) covers the JDK implementations.

The TLB is the cache of address translations, and it also depends on locality: a program that touches many different pages takes TLB misses even if the data fits in the CPU caches. The home of the TLB, the page walk, and huge pages is [Memory Management](04-memory-management.md).

## Trade-offs and When to Use

| Technique | Gain | Cost | Use when |
|---|---|---|---|
| Row-major traversal | Uses each loaded line fully | None | Always, if the order of the loops is free |
| Blocking (tiling) | Keeps a tile in the cache across many reuses | More complex loops | A traversal cannot change its order, and the data is larger than the cache |
| Padding rows or fields | Removes conflict misses and false sharing | More memory | A power-of-two stride shows conflict misses, or a profile shows false sharing |
| `@Contended` or manual padding | Separates hot fields onto different lines | Memory. Internal API or an arithmetic trick | A measured false sharing between threads |
| `LongAdder` over `AtomicLong` | Less contention and no false sharing | `sum()` is not an exact snapshot | A write-heavy counter that is read rarely |
| Arrays of primitives over lists of boxed values | A dense layout and fewer pointers | Less convenient API | A hot loop over many numbers |
| Branch-free code | No misprediction on unpredictable data | Harder to read. The JIT may already do it | A measured misprediction on random data |
| Vector API | Explicit SIMD | Incubator API. Needs `--add-modules` | A numeric kernel with contiguous arrays, after you measured |
| Pinning threads to a NUMA node | Local memory access | The scheduler cannot balance them | A multi-socket host and a measured remote-access cost |
| Huge pages | Fewer TLB misses (see [Memory Management](04-memory-management.md)) | Compaction stalls, a reserved pool | A TLB-bound workload with a large heap |

## Java Example

The three programs above are the Java examples of this page, and the class `PaddedCounters` shows a manual padding. The first program (a cache simulator) counts the hits and misses of a 32 KB, 8-way, 64-byte-line cache for row-major and column-major traversal. The second counts the line transfers of two cores that write counters on one line or on separate lines. The third enumerates the outcomes of the store-buffering litmus test in three models. All three are deterministic simulations, and none measures time. A measurement of a real cache effect needs JMH (the Java Microbenchmark Harness), because the JIT compiler, the garbage collector, and the CPU frequency all change the result. That is outside the scope of this KB.

## Interview Angle

### Questions

??? question "Why is it faster to traverse a matrix row by row than column by column?"

    A CPU cache loads memory in lines of 64 bytes on x86-64. A Java `int[][]` stores each row as a contiguous array. Row-by-row traversal reads 16 consecutive `int` values from each loaded line, so only the first access of each line misses: a miss rate of 1/16, about 6 percent. Column-by-column traversal jumps by a whole row between accesses. If the matrix is larger than the cache, every access needs a new line, and the line is evicted before the next column uses it.

    In a simulation of a 32 KB, 8-way, 64-byte-line cache on a 512 by 512 `int` matrix, row-major traversal missed 6.25 percent of the accesses and column-major traversal missed 100 percent. The stride of 2,048 bytes also made all the lines map to 2 of the 64 sets (a conflict miss). Padding each row by 64 bytes spread the lines over all sets and brought the miss rate back to 6.25 percent. When the whole matrix fits in the cache (64 by 64), the order does not matter. Real CPUs add a hardware prefetcher that hides part of the cost of sequential access.

??? question "What is a CPU cache line, and why does it matter to a programmer?"

    A cache line is the unit that a cache stores and moves: 64 bytes on x86-64, and 64 or 128 bytes on Arm (macOS reports 128 on Apple M1). The CPU never loads a single byte. It loads the whole line. Two consequences follow. First, spatial locality pays: data that sits close together arrives together, so contiguous arrays are fast and pointer-linked structures are slow. Second, cache coherence works per line, so two variables in one line behave as one variable for the purpose of sharing between cores. That second effect is false sharing. Code that must run on x86-64 and Arm should not hard-code 64: HotSpot pads `@Contended` fields by 128 bytes by default for this reason.

??? question "What is false sharing, how do you detect it, and how do you fix it?"

    False sharing happens when two threads on different cores write different variables that live in the same cache line. The coherence protocol (MESI) allows one writer for each line, so every write by one core invalidates the copy in the other core, and the line moves between the cores. The program has no data race, but it runs slowly, and it scales worse as you add threads. Drepper measured overheads of 390, 734, and 1,147 percent for 2, 3, and 4 threads on a four-processor machine in 2007.

    I detect it by symptoms (throughput that falls when threads increase, with each thread on its own data) and by tools: a profiler that reads hardware performance counters (on Linux, `perf c2c` ranks the cache lines that cores contend for, per the `perf-c2c(1)` man page), or a JMH benchmark that adds padding and checks the change. The fixes: pad the hot fields so that each has its own line (`@Contended` with `-XX:-RestrictContended`, or manual padding that uses 128 bytes), give each thread its own object, or reduce the writes. In the JDK, `LongAdder` and the counter cells of `ConcurrentHashMap` use `@Contended` for this reason. The trade-off is memory: padding multiplies the size of a field.

??? question "Why do we need `volatile` if the CPU already has cache coherence?"

    Coherence makes the caches agree on the value of one line, but it does not control when a write leaves the core, or in which order different writes become visible. A core has a store buffer: a write waits there before it reaches the cache, and a later load can complete first. On x86 (the x86-TSO model of Sewell et al.), that is the one visible reordering: a load can pass an earlier store to a different address. On Arm, loads and stores can be reordered in more ways. And the compiler and the JIT also reorder and keep values in registers, which no cache protocol can fix.

    `volatile` and the other synchronization actions give the guarantees of the Java Memory Model. The JVM compiles them to the right fence or acquire and release instruction for each CPU. Without them, a program with a data race can work on x86 and fail on Arm. The rules are in [Concurrency Basics](../java/07-concurrency-basics.md).

??? question "Explain the store-buffering litmus test. Which outcomes can x86 produce?"

    Two threads start with `x = y = 0`. Thread 0 runs `x = 1; r1 = y`. Thread 1 runs `y = 1; r2 = x`. In a sequentially consistent world, the outcome `r1 = 0 and r2 = 0` is impossible, because one of the two stores comes first in the global order, and the other thread's load then sees it. On x86 the outcome is possible. Each core puts its store in a store buffer and then executes its load from memory, which does not yet hold the other core's store. An `MFENCE` (or a locked instruction) between the store and the load drains the buffer and forbids the outcome. In my simulation of the idea, sequential consistency gives three outcomes: (0,1), (1,0), and (1,1). The store-buffer model adds (0,0), and the fence removes it again.

    The Java answer: in Java, a plain field shows this behavior. If both fields are `volatile`, the JMM forbids (0,0), and the JVM inserts the needed barrier on each CPU.

??? question "How do branch prediction and speculative execution work, and what is Spectre?"

    A pipelined CPU works on many instructions at once. At a conditional branch it does not know which instructions come next, so it predicts the outcome and runs the predicted path speculatively. If the prediction is right, the work is kept. If it is wrong, the CPU discards the work and restarts, which costs roughly the work that the pipeline lost (Dean's table: about 5 ns, approximate). Predictable branches are cheap, and branches that depend on random data are not.

    Spectre (Kocher et al., 2019) showed that the discarded speculative work still leaves traces in the caches, which an attacker can measure. In variant 1, the attacker trains the predictor so that a bounds check passes, and the CPU speculatively reads an out-of-bounds value that the attacker then recovers through cache timing. In variant 2, the attacker trains the branch target buffer to send speculation to chosen code. The attack crosses the boundary between processes, containers, and JIT-compiled sandboxes. Mitigations exist in software and hardware, and some cost speed. For a developer: do not run untrusted code in the same address space as secrets, and apply the platform mitigations.

??? question "Why is `ArrayList` faster than `LinkedList` in practice, although the textbook gives the linked list `O(1)` insertion?"

    The textbook counts steps and treats every step as equal. A real CPU does not. An `ArrayList` keeps its references in one contiguous array, so a scan reads cache lines in order and the hardware prefetcher helps. A `LinkedList` allocates a node for each element, and each step follows a pointer to a node that can be anywhere in the heap, so each step can miss the cache. A miss costs about 100 ns against about 0.5 ns for an L1 hit (Dean's table, approximate). Finding the position for the insertion needs a scan in the linked list, and that scan dominates the `O(1)` pointer change. For a list of boxed `Integer` values both lists chase a pointer to the object, so a primitive array wins by a larger margin. A linked list wins only in narrow cases, for example when you already hold an iterator at the position and insert often there.

### Follow-up Questions

??? question "What is the difference between a conflict miss, a capacity miss, and a compulsory miss?"

    A compulsory miss is the first access to a line. A capacity miss happens because the working set is larger than the cache. A conflict miss happens because too many lines map to the same set, although the cache as a whole has room. A stride that is a multiple of `sets * line size` (4,096 bytes in the simulated cache) causes conflict misses. Padding the stride fixes it.

??? question "What does the Vector API do, and is it production-ready?"

    It lets Java code express operations on vectors of values, and the JIT compiler turns them into SIMD instructions of the CPU. It is an incubator API in `jdk.incubator.vector`. openjdk.org lists JEP 537 (twelfth incubator) as delivered in JDK 27, and the JEP says that the API incubates until the needed features of Project Valhalla are preview features. So the API can still change, and it needs `--add-modules jdk.incubator.vector`. I would use it for a measured numeric kernel, and I would isolate it behind an interface.

??? question "What is NUMA, and how can it hurt a Java service?"

    On a multi-socket machine, memory is attached to a socket, and a core reads local memory faster than remote memory. Linux allocates a page on the node of the CPU that first touches it. If one thread initializes a large data structure and threads on other sockets read it, most reads are remote. Fixes: initialize the data from the threads that use it, set a memory policy with `numactl`, or pin threads. In Java, `-XX:+UseNUMA` is off by default. It works with the parallel collector and, since JDK 14 (JEP 345), with G1.

### Red Flags

- "The cache loads exactly the bytes that the program reads." It loads a whole line.
- "False sharing is a data race." It is not: the program is correct and slow.
- "Cache coherence makes `volatile` unnecessary." Coherence does not control the visibility time or the order of writes, and the compiler also reorders.
- "x86 never reorders memory operations." Stores can be delayed after later loads (the store buffer).
- "A cache line is always 64 bytes." On Arm, lines can be 128 bytes. Ask the platform.
- "A linked list is faster because insertion is `O(1)`." This ignores the cache misses of the traversal.
- Giving a measured speedup from a micro-benchmark without a harness, warm-up, and a repeat.
- "The Vector API is a stable feature." It is an incubator API.

## Common Mistakes

| Mistake | Why it hurts | Fix |
|---|---|---|
| Looping column by column over a large row-major array | Each access loads a new line, and the loaded line is evicted before reuse | Swap the loops, or block the loops |
| A matrix with a power-of-two row length and a column walk | The stride maps all lines to a few sets, which gives conflict misses | Pad each row by one line |
| Updating per-thread counters in adjacent fields or in adjacent array slots | False sharing: the counters share a line | Pad, use `LongAdder`, or give each thread a local value |
| Hard-coding a cache line size of 64 bytes | The size differs on some Arm CPUs | Pad to 128 bytes, or read it from the platform |
| Using `@Contended` in application code without `-XX:-RestrictContended` | The JVM ignores the annotation for non-trusted classes (`RestrictContended` is true by default) | Set the flag, export the package, and measure. Consider manual padding or `LongAdder` |
| Relying on x86 ordering in a data-race program | The program breaks on Arm, or after a JIT change | Use `volatile`, locks, or atomics, and follow the JMM |
| Using `LinkedList` for a general list | Pointer chasing and a node object for each element | Use `ArrayList` or `ArrayDeque` unless a measurement shows a gain |
| Using `List<Integer>` for a hot numeric loop | A pointer and an object header for each value | Use `int[]` or a primitive collection |
| Rewriting a branch without a benchmark | The JIT may already generate branch-free code, and the CPU may predict the branch | Measure with JMH first |
| Quoting latency numbers as exact | The numbers are approximate and from different hardware generations | Label them approximate, and use them for ratios |
| Timing a cache effect with `System.nanoTime()` in a plain `main` | The JIT, the garbage collector, and the warm-up distort the result | Use JMH |
| Treating the local laptop as the server | An Apple Silicon laptop (Arm, 128-byte lines under macOS, 16 KB pages) differs from an x86-64 server in line size, ordering, and page size | Test on the target hardware |

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| Cache coherence and memory consistency | Coherence keeps copies of one location consistent. Consistency defines the order in which operations on different locations become visible | Is the question about one address or about the order of several? |
| True sharing and false sharing | True sharing means threads access the same variable. False sharing means they access different variables in the same line | Do the threads use the same variable? |
| A cache line and a page | A line (64 bytes) is the unit of the CPU cache. A page (4 KB or more) is the unit of address translation | Is the unit a cache entry or a translation entry? |
| The CPU cache and the TLB | A cache stores data. The TLB stores address translations | Does it store bytes or a page mapping? (see [Memory Management](04-memory-management.md)) |
| Temporal and spatial locality | Temporal locality reuses the same address. Spatial locality uses nearby addresses | Is the same address or a nearby one reused? |
| Capacity miss and conflict miss | A capacity miss comes from too much data. A conflict miss comes from too many lines in one set | Does a larger cache of the same layout fix it? |
| A branch misprediction and a cache miss | A misprediction wastes pipeline work. A cache miss waits for memory | Is the CPU waiting for data or redoing work? |
| `volatile` and `LongAdder` | `volatile` gives visibility and order for one variable. `LongAdder` spreads updates over cells to reduce contention | Is the problem visibility, or contention on one counter? |
| Spectre and Meltdown | Spectre abuses branch prediction and works across many CPUs. Meltdown abuses delayed permission checks on some CPUs | Does the attack misuse prediction or a delayed check? |
| NUMA node and CPU socket | A node is a unit of memory locality. A socket is a physical package, and it usually holds one node | Is the question about memory placement or the physical chip? |

## Quick Reference Card

- **Latency** (approximate, Dean 2009): L1 0.5 ns, branch mispredict 5 ns, L2 7 ns, main memory 100 ns, SSD read 16 µs, disk seek 10 ms. Memory is about 200 times L1.
- **Cache line**: 64 bytes on x86-64. 64 or 128 on Arm. The JVM pads `@Contended` by 128 bytes. A cache moves whole lines.
- **Address split**: offset (inside the line), index (set), tag. 32 KB, 8-way, 64-byte lines gives 512 lines, 64 sets.
- **Misses**: compulsory, capacity, conflict. A power-of-two stride gives conflict misses.
- **Simulator**: 512 by 512 `int` matrix, 32 KB cache: row-major 6.25 percent misses, column-major 100 percent, column-major with padded rows 6.25 percent.
- **MESI**: Modified, Exclusive, Shared, Invalid. A write needs ownership. Coherence works per line.
- **False sharing**: different variables, same line. Fix with padding, `@Contended`, `LongAdder`, or local values. Drepper: 390, 734, and 1,147 percent overhead for 2, 3, and 4 threads (2007 hardware).
- **Ordering**: x86-TSO lets a load pass an earlier store (store buffer). `MFENCE` and locked instructions drain the buffer. Arm allows more reordering. `LDAR` and `STLR` are acquire and release.
- **JMM**: gives one rule set for all CPUs and for the JIT. Use `volatile`, locks, and atomics.
- **Branches**: a misprediction wastes pipeline work. Measure before you remove a branch.
- **Spectre**: speculation leaves cache traces. Variant 1 abuses a bounds check, variant 2 abuses indirect branches.
- **Vector API**: incubator. JEP 537 (twelfth incubator) delivered in JDK 27. In JDK 21 use `--add-modules jdk.incubator.vector`.
- **NUMA**: local allocation by default, first touch decides the node. `-XX:+UseNUMA` is off by default (it works with Parallel and G1).
- **Data structures**: arrays beat pointer structures when data exceeds the cache. `ArrayList` over `LinkedList`, `int[]` over `List<Integer>`.

## Further Reading

- Ulrich Drepper, [What Every Programmer Should Know About Memory](https://akkadia.org/drepper/cpumemory.pdf) (2007). The cache hierarchy, MESI, hardware prefetching, and the false sharing measurement that this page quotes.
- P. Sewell, S. Sarkar, S. Owens, F. Zappa Nardelli, and M. O. Myreen, [x86-TSO: A Rigorous and Usable Programmer's Model for x86 Multiprocessors](https://www.cl.cam.ac.uk/~pes20/weakmemory/cacm.pdf), Communications of the ACM, 2010. The store buffer model and the litmus tests.
- [Linux kernel documentation: `memory-barriers.txt`](https://www.kernel.org/doc/Documentation/memory-barriers.txt). The barrier types, acquire and release, and the statement on multicopy atomicity.
- [Arm Learn: thread synchronization examples (memory consistency)](https://learn.arm.com/learning-paths/servers-and-cloud-computing/memory_consistency/examples/). Reordering on Arm, and `LDAR` and `STLR`.
- P. Kocher et al., [Spectre Attacks: Exploiting Speculative Execution](https://spectreattack.com/spectre.pdf), IEEE Symposium on Security and Privacy, 2019. The two variants and their effect on isolation.
- [OpenJDK 21u source: `Contended.java`](https://github.com/openjdk/jdk21u/blob/jdk-21.0.7-ga/src/java.base/share/classes/jdk/internal/vm/annotation/Contended.java) and [`globals.hpp`](https://github.com/openjdk/jdk21u/blob/jdk-21.0.7-ga/src/hotspot/share/runtime/globals.hpp). The annotation, `ContendedPaddingWidth` (128), and `RestrictContended`. The `module-info.java` of `java.base` shows the qualified export.
- [JEP 537: Vector API (Twelfth Incubator)](https://openjdk.org/jeps/537) and [JEP 529: Vector API (Eleventh Incubator)](https://openjdk.org/jeps/529). The current status and the link to Project Valhalla.
- [Linux source: `arch/x86/Kconfig.cpu`](https://github.com/torvalds/linux/blob/master/arch/x86/Kconfig.cpu) and [`arch/arm64/include/asm/cache.h`](https://github.com/torvalds/linux/blob/master/arch/arm64/include/asm/cache.h). The cache line constants that the kernel uses.
- [Intel 64 and IA-32 Architectures Optimization Reference Manual](https://www.intel.com/content/dam/doc/manual/64-ia-32-architectures-optimization-manual.pdf). The cache parameters of Intel cores, including the 64-byte line.
- M. Lipp et al., [Meltdown: Reading Kernel Memory from User Space](https://meltdownattack.com/meltdown.pdf), USENIX Security 2018. How out-of-order execution leaks kernel memory.
- [perf-c2c(1)](https://man7.org/linux/man-pages/man1/perf-c2c.1.html). The tool that ranks contended cache lines.
- [JEP 345: NUMA-Aware Memory Allocation for G1](https://openjdk.org/jeps/345) and [OpenJDK 21u `superword.cpp`](https://github.com/openjdk/jdk21u/blob/jdk-21.0.7-ga/src/hotspot/share/opto/superword.cpp). NUMA in G1 and the C2 auto-vectorizer.
- [golang/go issue 53075](https://github.com/golang/go/issues/53075). The report that macOS gives 128 as the cache line size on Apple M1.
- [Linux kernel documentation: NUMA memory policy](https://docs.kernel.org/admin-guide/mm/numa_memory_policy.html). Local allocation and the policy modes.
- [JDK 21 `java` command reference](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html). `-XX:+UseNUMA` and related flags.
- J. L. Hennessy and D. A. Patterson, *Computer Architecture: A Quantitative Approach*. The standard text on caches, the 3C miss model, pipelines, and branch prediction.
- [Back-of-envelope estimation](../hld/back-of-envelope-estimation.md) in this KB. The latency numbers that this page uses.
- [Concurrency Basics](../java/07-concurrency-basics.md) and [`java.util.concurrent`](../java/08-java-util-concurrent.md) in this KB. The Java Memory Model and `LongAdder`.
