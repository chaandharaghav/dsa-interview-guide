# Operating Systems

This subject covers how the operating system runs a program: processes and threads, scheduling, synchronization, memory, files and I/O, the hardware facts that affect performance, and the Linux tools that a backend developer uses to diagnose a service. Each page connects the kernel mechanism to what a Java developer sees.

## How to Study This Subject

1. Read the P1 pages first, in this order: [Processes and Threads](01-processes-and-threads.md), [Synchronization](03-synchronization.md), and [Memory Management](04-memory-management.md). Interviewers ask about these topics in the technical rounds.
2. Read [File Systems and I/O Models](05-file-systems-and-io-models.md) before the Java NIO and virtual threads pages, because it explains `epoll` and the page cache under them.
3. Read [CPU Scheduling](02-cpu-scheduling.md) and [Computer Architecture for Developers](06-computer-architecture-for-developers.md) for performance questions and for senior roles.
4. Keep [Linux for Developers](07-linux-for-developers.md) open when you debug a service. Practice its tools on a Linux machine or a container.
5. Use the [question bank](interview-question-bank.md) for revision. Each answer links to the page that explains it.

The pages describe Linux on x86-64 unless they say otherwise. The Java code targets Java 21 and runs as simulations, so its output is the same on every machine.

The Java side of the same topics is on the Java pages: [Concurrency Basics](../java/07-concurrency-basics.md), [java.util.concurrent](../java/08-java-util-concurrent.md), [Virtual Threads](../java/10-virtual-threads.md), [Memory and Garbage Collection](../java/06-memory-and-gc.md), and [I/O, NIO, and Serialization](../java/15-io-nio-and-serialization.md).

## Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

| Page | Content | Priority |
|---|---|---|
| [Processes and Threads](01-processes-and-threads.md) | Process and thread, `fork` and `exec`, context switch, user mode and kernel mode, IPC, how a Java thread maps to an OS thread | P1 |
| [CPU Scheduling](02-cpu-scheduling.md) | Scheduling algorithms, Linux CFS and EEVDF, scheduling classes, CPU limits in containers | P2 |
| [Synchronization](03-synchronization.md) | Atomic instructions, spinlocks, futex-based mutexes, semaphores, monitors, deadlock, priority inversion, the classic problems | P1 |
| [Memory Management](04-memory-management.md) | Virtual memory, paging, the TLB, huge pages, page faults, page replacement, the OOM killer, the JVM in containers | P1 |
| [File Systems and I/O Models](05-file-systems-and-io-models.md) | Inodes and file descriptors, the page cache, `fsync`, blocking and non-blocking I/O, `epoll`, `io_uring`, zero-copy | P2 |
| [Computer Architecture for Developers](06-computer-architecture-for-developers.md) | The memory hierarchy, cache lines, false sharing, memory ordering, branch prediction, SIMD, NUMA | P2 |
| [Linux for Developers](07-linux-for-developers.md) | Signals, permissions, shell basics, and diagnostic tools: `top`, `vmstat`, `strace`, `lsof`, `ss`, `perf` | P2 |
| [Interview Question Bank](interview-question-bank.md) | Short questions with model answers. Each answer links to the page that explains it | P1 |
