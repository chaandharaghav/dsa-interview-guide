---
priority: P1
---
# Operating Systems Interview Question Bank

This page collects the questions that interviewers ask most often about the operating systems pages of this knowledge base. The questions follow the page order: processes and threads, CPU scheduling, synchronization, memory management, file systems and I/O models, computer architecture for developers, and Linux for developers. The three P1 pages (processes and threads, synchronization, memory management) have more questions than the other pages, because interviews test them most. Use the page in two steps. First, read the question and answer it aloud, as you would in the interview, and say the reason as well as the result. Second, open the block and compare your answer with the model answer. Each model answer is short. It gives the direct answer, then the reason, then the trade-off or the edge case, and it ends with a link to the section that explains the topic in depth. If your answer missed a point, read that section before you move on. Where an answer depends on the platform (Linux on x86-64, JDK 21 or JDK 17), the answer names it, because a correct answer for one platform can be wrong for another.

## Contents

- [Processes and Threads](#processes-and-threads)
- [CPU Scheduling](#cpu-scheduling)
- [Synchronization](#synchronization)
- [Memory Management](#memory-management)
- [File Systems and I/O Models](#file-systems-and-io-models)
- [Computer Architecture for Developers](#computer-architecture-for-developers)
- [Linux for Developers](#linux-for-developers)

## Processes and Threads

??? question "What is the difference between a process and a thread?"

    A process has its own address space, file descriptor table, and credentials. A thread is one flow of execution inside a process: it has its own stack, registers, and thread-local state, and it shares the address space and the descriptors with the other threads of the process. The difference is isolation against cost: a crash stays inside one process, while threads exchange data through shared memory but one buggy thread can corrupt the data of all of them. On Linux both are tasks that `clone(2)` creates, and the sharing flags (`CLONE_VM`, `CLONE_FILES`, `CLONE_SIGHAND`, `CLONE_THREAD`) decide which one you get. See [Processes and Threads](01-processes-and-threads.md#threads-in-linux).

??? question "What happens when a process calls `fork`, and why is it not slow for a large process?"

    The kernel creates a child task with a new PID and a copy of the page tables of the parent, not a copy of the memory pages. It marks the writable private pages read-only in both processes, and the first write to a page causes a fault and a copy of that one page (copy-on-write). So `fork` followed by `exec` copies almost nothing. The cost still grows with the size of the parent, because the kernel copies the page tables. In a multithreaded parent the child has only the calling thread, so a lock that another thread held stays locked in the child. See [Processes and Threads](01-processes-and-threads.md#fork).

??? question "Why does Unix split process creation into `fork` and `exec`?"

    `fork` copies the process and `exec` replaces the program in the same process, so the code between the two calls can change the environment of the child. A shell redirects descriptors 0, 1, and 2, or sets limits, in the child before it calls `exec`, and the new program inherits that setup without knowing it. A single spawn call would need many parameters to describe all these changes. Open descriptors stay open across `exec` unless they have the close-on-exec flag, so a child can inherit a descriptor that it should not have. See [Processes and Threads](01-processes-and-threads.md#exec).

??? question "What is a zombie process? How does it differ from an orphan, and how do you remove it?"

    A zombie is a process that ended and whose parent has not called `wait`. It keeps only its process table entry and exit status, so it uses no CPU and almost no memory, but it holds a slot, and a full table prevents new processes. An orphan is still running and has lost its parent, and `init` (PID 1) or the nearest subreaper adopts it. You cannot kill a zombie, because it is already dead: make the parent call `wait`, or end the parent. In a container, an application that runs as PID 1 and never reaps its adopted children collects zombies, so a small init program fixes it. See [Processes and Threads](01-processes-and-threads.md#exit-and-wait).

??? question "What does a context switch cost, and why is a thread switch cheaper than a process switch?"

    A context switch saves the registers of the running task, restores those of the next task, and switches the kernel stack. The direct cost is on the order of microseconds: one measurement on an Intel Haswell CPU gave about 1.2 to 1.5 microseconds for a thread switch pinned to one core (approximate, it varies with the CPU and the kernel). A switch between two threads of one process keeps the address space, so the page tables and TLB entries stay valid, while a process switch loads another page-table base. The indirect cost can be larger, because the new task finds the CPU caches full of the data of the old task. See [Processes and Threads](01-processes-and-threads.md#context-switch).

??? question "What is a system call, and is it a context switch?"

    A system call is a request from user mode to the kernel: the program puts the call number in a register and executes the `syscall` instruction, and the CPU switches to kernel mode at a fixed entry point. It is not a context switch, because the privilege level changes on the same task and the scheduler does not run, unless the call blocks. It still costs more than a function call (the cheapest call took about 0.08 to 0.2 microseconds in one kernel benchmark, depending on the Meltdown mitigation), and a call that copies data or waits costs far more. This is why buffered streams exist: they make fewer system calls. See [Processes and Threads](01-processes-and-threads.md#user-mode-kernel-mode-and-the-system-call-path).

??? question "How does a Java thread map to an OS thread, and what does `OutOfMemoryError: unable to create native thread` mean?"

    A Java platform thread maps 1:1 to an OS thread: `Thread.start()` makes HotSpot call `pthread_create`, and the C library creates a task with `clone`. Each thread costs a kernel task and a stack, whose size comes from `-Xss` (1024 KB by default on Linux x64). The error means that `pthread_create` failed, not that the Java heap is full: the cause is an OS limit such as `ulimit -u`, `kernel.threads-max`, a container PID limit, `vm.max_map_count`, or too little memory for the stack. Virtual threads (Java 21) use M:N scheduling on a few carrier threads, so they avoid the limit for blocking code. See [Processes and Threads](01-processes-and-threads.md#how-a-java-thread-maps-to-an-os-thread).

??? question "Compare the IPC mechanisms. When do you choose a pipe, shared memory, or a socket?"

    A pipe is a one-direction kernel buffer that fits a parent and child or a shell pipeline, and the kernel handles the synchronization. A socket gives two-way communication with the same API for local and remote peers, so it is the default choice, because a local design can later span machines. Shared memory has the highest throughput, because after the setup no system call or copy is needed, but the processes must synchronize access themselves. A signal carries almost no data, so use it for notification only. See [Processes and Threads](01-processes-and-threads.md#inter-process-communication).

## CPU Scheduling

??? question "Compare FCFS, SJF, SRTF, and round robin. Which gives the lowest average waiting time?"

    FCFS runs jobs in arrival order, has no starvation, and suffers the convoy effect. SJF is optimal for average waiting time only when all jobs are ready at the same time, and SRTF, its preemptive form, is the one that stays lowest when jobs arrive over time. On the simulator of the page (bursts 8, 4, 9, 5), the average waiting time is 8.75 for FCFS, 7.75 for SJF, 6.50 for SRTF, and 11.75 for round robin with quantum 4. The cost of SJF and SRTF is that the kernel does not know the next burst and long jobs can starve, while round robin gives every job the CPU within one round and so bounds the response time. See [CPU Scheduling](02-cpu-scheduling.md#a-scheduling-simulator).

??? question "What is starvation, and how do aging and a multilevel feedback queue address it?"

    Starvation means that a runnable task waits without a bound because other tasks always have priority. Strict priority scheduling and SJF cause it. Aging raises the priority of a task in proportion to its waiting time, so it eventually runs. A multilevel feedback queue (MLFQ) moves all tasks to the top queue after a period S (a priority boost), and it counts the total time that a task uses at a level, so a task cannot keep its priority by yielding just before the quantum ends. See [CPU Scheduling](02-cpu-scheduling.md#classic-algorithms).

??? question "How does the Linux scheduler choose the next task, and what changed in Linux 6.6?"

    The kernel asks the scheduling classes in the order stop, deadline, real-time, fair, and idle, and normal tasks use the fair class with one run queue for each CPU. The nice value gives a weight (1024 at nice 0, about 1.25 times for each step), and the virtual runtime of a task grows by the run time scaled by 1024 divided by the weight, so each task gets a CPU share in proportion to its weight. Until Linux 6.5, CFS ran the task with the lowest virtual runtime. Linux 6.6 replaced that pick rule with EEVDF, which runs the eligible task (lag at least zero) with the earliest virtual deadline. See [CPU Scheduling](02-cpu-scheduling.md#eevdf-replaces-the-cfs-pick-logic-linux-66).

??? question "What is the difference between `SCHED_FIFO`, `SCHED_RR`, and `SCHED_DEADLINE`? Why can a real-time priority freeze a machine?"

    `SCHED_FIFO` and `SCHED_RR` are fixed-priority real-time policies with priorities 1 to 99: `SCHED_FIFO` has no time slicing, and `SCHED_RR` adds a quantum among tasks of equal priority (100 ms by default). `SCHED_DEADLINE` takes a runtime, a deadline, and a period, and the kernel schedules by earliest deadline first after an admission test (95 percent cap by default). A real-time task runs before every normal task, so a `SCHED_FIFO` task that never blocks starves all normal tasks on its CPU. Real-time throttling limits this to 0.95 s of each second by default, and these policies need `CAP_SYS_NICE`. See [CPU Scheduling](02-cpu-scheduling.md#scheduling-classes-and-policies).

??? question "A Java service in a Kubernetes pod has a CPU limit of 2 and shows latency spikes while the node is idle. What do you check?"

    The pattern points to CFS bandwidth throttling: a limit of 2 is a quota of 200 ms in each 100 ms period, and a burst of threads can use the whole quota in a fraction of the period. The kernel then stops all threads of the container until the next period, so a request can wait for the rest of it. Read `cpu.stat` in the cgroup and check whether `nr_throttled` and `throttled_usec` grow. Then reduce the parallel threads (`-XX:ActiveProcessorCount`, smaller pools, fewer GC threads), raise the limit, or keep the request and drop the limit, which trades isolation for idle CPU. The JDK 21 JVM computes `availableProcessors()` as the quota divided by the period, rounded up, and ignores CPU shares, so a pod with a request and no limit makes a JDK 21 JVM see all the CPUs of the node. See [CPU Scheduling](02-cpu-scheduling.md#containers-cpu-weight-and-cpu-quota).

## Synchronization

??? question "What are the requirements for a solution to the critical-section problem? Why does a plain boolean flag fail?"

    A solution needs mutual exclusion, progress (when the section is free, the threads that want to enter decide among themselves without waiting forever, and a thread outside its section cannot block them), and bounded waiting (no thread loses every time). A plain flag fails because the test ("is it free?") and the set ("take it") are two steps, so two threads can both see "free" and both enter. Peterson's algorithm solves it in software for two threads, but it needs memory barriers on modern CPUs. A real lock uses one atomic read-modify-write instruction: `xchg` or `lock cmpxchg` on x86-64, or a load-exclusive and store-exclusive pair (or `cas`) on Arm. Disabling interrupts works only for one CPU in kernel mode. See [Synchronization](03-synchronization.md#the-critical-section-problem).

??? question "How does a mutex work on Linux? What happens when two threads call lock at the same time?"

    A pthread mutex is an integer in user space with three states: 0 unlocked, 1 locked with no waiter, 2 locked with possible waiters (Drepper, "Futexes Are Tricky"). The lock tries one atomic compare-and-swap from 0 to 1, and if it works, no system call happens. If it fails, the thread sets the value to 2 and calls `futex(FUTEX_WAIT)`, and the kernel blocks the thread only if the value is still 2, which prevents a lost wakeup. Unlock wakes one waiter with `FUTEX_WAKE` only if the old value showed a waiter. The trade-off is that a contended lock costs system calls and context switches. See [Synchronization](03-synchronization.md#the-futex-based-mutex).

??? question "When is a spinlock better than a mutex that blocks?"

    A spinlock wins when the wait is shorter than a block-and-wake cycle, which needs two context switches and two system calls. That means a critical section of a few instructions, with an owner that runs on another CPU, or code that cannot sleep, such as a kernel interrupt handler. It loses on a single CPU, when the owner can be preempted or can block inside the section, and under heavy contention, where the waiters move the cache line between cores. Practical locks are hybrids: the Linux kernel mutex spins while the owner runs and then sleeps, and HotSpot spins briefly before it parks a thread. See [Synchronization](03-synchronization.md#spinlocks).

??? question "What is the difference between a mutex and a semaphore?"

    A mutex has an owner: the thread that locks it must unlock it. A semaphore has no owner, because any thread can call V. So a mutex protects a critical section, and a semaphore counts resources (a pool of N connections) or signals an event between threads. The ownership decides what the system can do: it can apply priority inheritance and detect a wrong unlock on a mutex, and it can do neither for a semaphore. A semaphore also remembers a V that comes before the P, while a condition variable signal with no waiter is lost. See [Synchronization](03-synchronization.md#semaphores).

??? question "What is the difference between Hoare and Mesa monitors, and why does Java require a `while` loop around `wait()`?"

    With Hoare semantics, `signal` hands the monitor to the waiter at once, so the waiter can assume that the condition is true and an `if` is enough. With Mesa semantics the signaler continues and the waiter runs later, so another thread can enter first and make the condition false again. Java and POSIX threads use Mesa semantics, so a waiter must test the condition again: `while (!condition) wait();`. Three reasons make the loop necessary: another thread can take the item first, `notifyAll` wakes threads that cannot all proceed, and a thread can wake without a signal (a spurious wakeup). See [Synchronization](03-synchronization.md#monitors-and-condition-variables).

??? question "Name the four conditions for a deadlock and the four strategies for dealing with it. What do real systems do?"

    The conditions (Coffman et al., 1971) are mutual exclusion, hold and wait, no preemption, and circular wait, and all four must hold. The strategies are prevention (break one condition, usually circular wait with a global lock order), avoidance (the banker's algorithm), detection and recovery, and ignoring the problem. Databases detect a cycle in a wait-for graph and abort a victim transaction, the Linux kernel prevents deadlock with lock-order rules and checks them with `lockdep`, and the JVM can report a Java-level deadlock but does not break it. General systems do not use avoidance, because it needs the maximum need of each thread in advance. See [Synchronization](03-synchronization.md#deadlock).

??? question "What is priority inversion, and how did it affect the Mars Pathfinder?"

    Priority inversion happens when a high-priority thread waits for a lock that a low-priority thread holds, while a medium-priority thread keeps the low-priority holder off the CPU. The holder cannot release the lock, so the high-priority thread waits as long as the medium thread runs. On Pathfinder (1997, VxWorks), a mutex that guards the `select()` descriptor list was created without priority inheritance, the bus distribution task blocked on it, and the system reset itself. The fixes are priority inheritance (`PTHREAD_PRIO_INHERIT`, PI futexes, `rt_mutex` on Linux), a priority ceiling (`PTHREAD_PRIO_PROTECT`), or no shared lock between tasks of different priorities. See [Synchronization](03-synchronization.md#priority-inversion-and-priority-inheritance).

??? question "Readers-writers: what are the fairness choices, and what does Linux do?"

    A readers-writers lock lets many readers or one writer in. Reader preference maximizes read throughput and can starve the writer. Writer preference makes new readers wait when a writer waits and can starve the readers, and a fair lock follows arrival order and gives up some read parallelism. glibc `pthread_rwlock_t` prefers readers by default, and the kernel `rw_semaphore` is fair on kernels without `PREEMPT_RT`. A readers-writers lock can still be slower than a mutex for short read sections, because every reader writes the shared reader count. See [Synchronization](03-synchronization.md#readers-writers-locks).

??? question "In the bounded-buffer solution with semaphores, why does the order of `P(mutex)` and `P(empty)` matter?"

    If the producer calls `P(mutex)` first and the buffer is full, it holds the mutex while it waits for a free slot. The consumer needs the mutex to remove an item, so it blocks too, and neither can proceed. This is a deadlock by hold and wait. The correct order waits on the counting semaphore first, without a lock, and takes the mutex only for the short update of the buffer. See [Synchronization](03-synchronization.md#the-classic-problems-at-the-os-level).

## Memory Management

??? question "What is virtual memory, and why do operating systems use it?"

    Each process uses virtual addresses, and the MMU translates each one to a physical address through page tables that the kernel maintains. It gives isolation (a process can touch only the pages that its page table maps), a flat contiguous space over scattered frames, sharing (shared libraries, copy-on-write after `fork`, `MAP_SHARED`), and overcommit with demand paging. The cost is translation, and the TLB hides it for the pages in use. See [Memory Management](04-memory-management.md#why-virtual-memory-exists).

??? question "Walk through address translation on x86-64. Why are page tables multi-level, and what does the TLB do?"

    With 4 KB pages and 48-bit addresses, the low 12 bits are the offset and the other 36 bits are four 9-bit indexes. `CR3` points to the top table (PGD), and the indexes select the PGD, PUD, PMD, and PTE entries in turn, and the PTE holds the frame number. A flat table for a 48-bit space needs 2^36 entries, which is 512 GB at 8 bytes each for every process, so the tables form a tree that allocates a lower level only for ranges in use. A walk reads up to four table entries from memory, so the TLB caches recent translations, and a miss costs a walk. See [Memory Management](04-memory-management.md#multi-level-page-tables-on-x86-64).

??? question "What happens on a page fault? Distinguish a minor fault from a major fault."

    The MMU raises an exception, and the kernel finds the virtual memory area of the address. If no area contains the address, or the access violates the permissions, the kernel sends `SIGSEGV`. Otherwise the fault is legal, and the kernel resolves it and the CPU restarts the instruction. A minor fault needs no disk I/O (first touch of an anonymous page, a page cache hit, a copy-on-write copy), and a major fault needs disk I/O (a file page that is not cached, or a swap-in). A major fault costs a disk read, so a program with many of them is slow. See [Memory Management](04-memory-management.md#page-faults).

??? question "What are huge pages? When would you enable them for a JVM, and what is the risk?"

    A huge page is 2 MB (or 1 GB) on x86-64, and a PMD (or PUD) entry maps it directly, so one TLB entry covers 512 times more memory and the walk is shorter. Linux offers explicit huge pages (`hugetlbfs`, a pool that the administrator reserves) and transparent huge pages (THP). For a JVM, `-XX:+UseLargePages` and `-XX:+UseTransparentHugePages` are both off by default, and I would try them only for a large heap with a measured TLB problem. The risks are compaction stalls with THP, and a reserved pool that other programs cannot use, which can also make the heap allocation fail. Measure before and after under realistic load. See [Memory Management](04-memory-management.md#huge-pages-and-transparent-huge-pages).

??? question "Compare FIFO, LRU, OPT, and clock page replacement. What is Belady's anomaly?"

    FIFO evicts the oldest page, OPT evicts the page whose next use is farthest away (a benchmark, because it needs the future), LRU evicts the page unused for the longest time, and clock approximates LRU with the hardware `ACCESSED` bit. Belady's anomaly means that more frames cause more faults: FIFO takes 9 faults with 3 frames and 10 with 4 on the string 1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5. LRU and OPT have the stack property, so they never show it. Linux does not run exact LRU, because it would need an update on every access: it uses active and inactive lists, or the multi-generational LRU since 6.1. See [Memory Management](04-memory-management.md#page-replacement).

??? question "What is thrashing, and how do you stop it?"

    Thrashing is the state in which the system spends more time paging than on useful work, because the working sets of the running processes together exceed RAM. Each fault evicts a page that another process needs soon, so the disk is busy, the CPU idles, and throughput collapses. The fixes are to reduce the demand (fewer processes, smaller caches, a smaller heap) and then to add RAM. For a JVM, a heap larger than the RAM that the container grants is the common cause, because a full garbage collection touches every heap page. See [Memory Management](04-memory-management.md#thrashing-and-the-working-set).

??? question "A Java service in Kubernetes is `OOMKilled`, but the heap was half full and there was no `OutOfMemoryError`. Explain."

    `OOMKilled` (exit code 137, which is 128 plus signal 9) means that the kernel killed the process: usage reached the cgroup `memory.max` limit and the kernel could not reclaim enough, so the OOM killer sent `SIGKILL`. The JVM cannot catch `SIGKILL`, so it throws nothing. The limit counts more than the heap: Metaspace, thread stacks, the code cache, direct buffers, native memory, and the page cache that the cgroup holds. Diagnose with `memory.events` (`oom_kill`) and Native Memory Tracking, then lower the heap share or cap the non-heap areas. See [Memory Management](04-memory-management.md#swap-overcommit-and-the-oom-killer).

??? question "What are `vm.overcommit_memory` and `oom_score_adj`? How does the OOM killer choose a victim?"

    `vm.overcommit_memory` sets the policy for requests of more virtual memory than the system can back: mode 0 (the default) refuses only obvious overcommit, mode 1 never refuses, and mode 2 refuses when the commit would exceed swap plus `overcommit_ratio` percent of RAM. In modes 0 and 1 the failure moves to the first touch, where the kernel runs the OOM killer. The killer scores each process from its resident pages, swap entries, and page-table bytes, adds `oom_score_adj` (-1000 to +1000) scaled by the total pages divided by 1000, and kills the highest score. In Kubernetes the QoS class sets `oom_score_adj`: -997 for `Guaranteed` and 1000 for `BestEffort`. See [Memory Management](04-memory-management.md#swap-overcommit-and-the-oom-killer).

??? question "What is the difference between `-Xms`, the committed heap, and the resident set size? What does `-XX:+AlwaysPreTouch` change?"

    `-Xmx` reserves a range of virtual addresses, and `-Xms` sets the initial committed size, but the kernel gives a frame only when the program first touches a page. So the resident set size (RSS) starts below the committed heap and grows as the allocator and the collector touch new regions. `-XX:+AlwaysPreTouch` touches every page of the committed heap at startup: the start is slower and the RSS equals the committed heap from the start, but no allocation later waits for a page fault. Use it for a latency-sensitive service with `-Xms` equal to `-Xmx`, and count the full heap in the container limit. See [Memory Management](04-memory-management.md#what-this-means-for-a-jvm).

## File Systems and I/O Models

??? question "A service writes a log file. An administrator deletes it, but `df` shows that no space came back. Why?"

    The process still has the file open. `rm` calls `unlink`, which removes the directory entry and lowers the link count, but the kernel frees the inode and its blocks only when the link count is 0 and no open file description points to it. `du` walks names, so it cannot see the file, while `df` asks the file system, which still counts the blocks. `lsof +L1` finds open files that were unlinked. Restart the process or make it reopen the log, and in the future truncate the file in place instead of deleting it. See [File Systems and I/O Models](05-file-systems-and-io-models.md#inodes-directories-and-links).

??? question "`write` returned success, then the power failed and the data is gone. What does `fsync` guarantee?"

    `write` copies the data into the page cache and marks the page dirty, and it does not wait for the device, so a power failure before write-back loses it. `fsync` flushes the modified data and metadata of the file to the device and returns when the device reports completion, and it is the only way to know that the data survives a crash. An error from `fsync` means that the write failed, so the program must not retry and assume success. For a new or renamed file, `fsync` the directory too, because the directory entry is a separate write. The journal protects the file system structure, not the latest writes of the application. See [File Systems and I/O Models](05-file-systems-and-io-models.md#writing-safely-journaling-and-fsync).

??? question "What is the page cache, and how do buffered reads, buffered writes, and `O_DIRECT` interact with it?"

    The page cache is the kernel cache of file contents in RAM. A buffered read hits or misses the cache, and the kernel reads ahead for sequential access. A buffered write copies data into cache pages, marks them dirty, and returns, and the kernel writes them back later. `O_DIRECT` skips the cache, so it suits software with its own cache, but it needs aligned buffers and does not promise durability. The cache uses idle RAM, so `free` is low on a healthy host: read `MemAvailable`, and remember that a cgroup counts the page cache of its container. See [File Systems and I/O Models](05-file-systems-and-io-models.md#the-page-cache).

??? question "Compare `select`, `poll`, and `epoll`. What is the difference between level-triggered and edge-triggered?"

    `select` cannot watch a descriptor number of `FD_SETSIZE` (1024) or more, and it overwrites its sets, so the caller rebuilds them for each call. `poll` takes an array of `pollfd` and has no such limit, but the caller passes the whole array on each call. `epoll` keeps the interest list in the kernel, so the program registers a descriptor once and `epoll_wait` returns only the ready list, which is why it suits thousands of connections. Level-triggered mode reports a descriptor for as long as it is ready, and edge-triggered mode reports it only on a change, so an edge-triggered handler must read until `EAGAIN` or the connection can hang. See [File Systems and I/O Models](05-file-systems-and-io-models.md#select-poll-and-epoll).

??? question "Why can a Netty or Node.js server handle tens of thousands of connections with a few threads? How does the Java `Selector` relate to `epoll`?"

    Most connections are idle most of the time, so an event loop registers all sockets with `epoll`, sleeps in `epoll_wait`, and runs a handler only for the sockets that are ready. A thread-per-connection design pays a stack and a scheduler entry for each idle connection. On Linux, `Selector.open()` creates an `epoll` instance, `register` becomes `epoll_ctl`, and `select()` becomes `epoll_wait`, and the JDK selector is level-triggered. The rule of the loop is that a handler must not block, because it would stop every connection on that thread. Virtual threads give the same scaling with blocking code, but file I/O still blocks the carrier thread. See [File Systems and I/O Models](05-file-systems-and-io-models.md#c10k-the-reactor-and-the-event-loop).

??? question "What is zero-copy, and when does `sendfile` not help?"

    With `read` and `write`, a file sent to a socket is copied from the page cache to a user buffer and then to the socket buffer. `sendfile` moves the data inside the kernel and skips the user buffer, and `FileChannel.transferTo` lets the JDK use such a call. It helps only when the program does not need the bytes: a server that must compress or encrypt in user space (TLS in the JVM) has to read them into memory. It does not remove the disk read on a cache miss, and `transferTo` may transfer fewer bytes than requested, so call it in a loop. See [File Systems and I/O Models](05-file-systems-and-io-models.md#zero-copy).

## Computer Architecture for Developers

??? question "Why is it faster to traverse a matrix row by row than column by column?"

    A cache loads a line of 64 bytes on x86-64, and a Java `int[][]` stores each row as one contiguous array, so row-by-row traversal reads 16 `int` values from each loaded line and misses only 1 time in 16. Column-by-column traversal jumps by a whole row, so on a matrix larger than the cache every access needs a new line. In the simulation of the page (32 KB, 8-way cache, 512 by 512 `int` matrix), row-major missed 6.25 percent and column-major missed 100 percent, and padding each row by 64 bytes brought the rate back to 6.25 percent. When the matrix fits in the cache (64 by 64), the order does not matter. See [Computer Architecture for Developers](06-computer-architecture-for-developers.md#locality-and-the-order-of-traversal).

??? question "What is false sharing, how do you detect it, and how do you fix it?"

    False sharing happens when threads on different cores write different variables that live in the same cache line, so the coherence protocol moves the line between the cores at each write although the program shares no data. The program is correct and slow, and it scales worse as you add threads. Detect it from the symptoms and with `perf c2c` or a JMH benchmark that adds padding. Fix it with padding (HotSpot pads `@Contended` by 128 bytes, and the annotation needs `-XX:-RestrictContended` in application code), a separate object for each thread, fewer writes, or `LongAdder`. The trade-off is memory. See [Computer Architecture for Developers](06-computer-architecture-for-developers.md#false-sharing).

??? question "Why do we need `volatile` if the CPU already has cache coherence?"

    Coherence keeps the copies of one line consistent, but it does not control when a write leaves the core or in which order different writes become visible. A core has a store buffer, so on x86 a load can complete before an earlier store to a different address, and Arm allows more reordering. The compiler and the JIT also reorder and keep values in registers, which no cache protocol can fix. `volatile` gives the guarantees of the Java Memory Model, and the JVM picks the right fence or acquire and release instruction for each CPU. See [Computer Architecture for Developers](06-computer-architecture-for-developers.md#memory-ordering-and-barriers).

??? question "Why is `ArrayList` faster than `LinkedList` in practice, although the linked list has `O(1)` insertion?"

    The textbook counts steps and treats each step as equal, but a real CPU does not. An `ArrayList` keeps its references in one contiguous array, so a scan reads cache lines in order and the prefetcher helps. A `LinkedList` follows a pointer to a node that can be anywhere in the heap, and a miss costs about 100 ns against about 0.5 ns for an L1 hit (approximate, Dean 2009). Finding the insertion position needs a scan, and the scan dominates the `O(1)` pointer change. See [Computer Architecture for Developers](06-computer-architecture-for-developers.md#what-this-means-for-data-structures).

??? question "What is NUMA, and how can it hurt a Java service?"

    On a multi-socket machine, memory is attached to a socket, and a core reads local memory faster than remote memory. Linux allocates a page on the node of the CPU that first touches it, so a thread that initializes a large structure decides where it lives. If threads on other sockets then read it, most reads are remote. Fixes: initialize the data from the threads that use it, set a policy with `numactl`, or pin threads. In Java, `-XX:+UseNUMA` is off by default, and it works with the parallel collector and, since JDK 14, with G1. See [Computer Architecture for Developers](06-computer-architecture-for-developers.md#numa).

## Linux for Developers

??? question "What is the difference between `SIGTERM` and `SIGKILL`? What do `SIGINT`, `SIGHUP`, and `SIGQUIT` do to a Java process?"

    `SIGTERM` (15) asks a process to end, and the process can catch it and clean up. `SIGKILL` (9) cannot be caught, blocked, or ignored, so no shutdown hook, `finally` block, or flush runs. The order for an operator is `SIGTERM`, a wait, and then `SIGKILL`, which is what Docker (10 seconds by default) and Kubernetes (30 seconds by default) do. On the JVM, `SIGTERM`, `SIGINT`, and `SIGHUP` start the shutdown hooks and give exit status 143, 130, and 129, while `SIGQUIT` prints a thread dump and does not end the JVM. See [Linux for Developers](07-linux-for-developers.md#signals).

??? question "How do you shut down a Java service in Kubernetes without dropping requests?"

    The kubelet runs the `preStop` hook, sends `SIGTERM` to PID 1, waits for the grace period (30 seconds by default), and then sends `SIGKILL`, while the control plane removes the Pod from the endpoints in parallel. So `java` must be PID 1: use the exec form of `ENTRYPOINT` or `exec java` in a script, because a shell as PID 1 does not pass the signal. On `SIGTERM`, fail the readiness probe, stop accepting new work, and finish the requests in progress, with a drain timeout below the grace period. Exit status 143 means a clean exit after `SIGTERM`, and 137 means `SIGKILL` (the grace period ran out or the OOM killer acted). See [Linux for Developers](07-linux-for-developers.md#graceful-shutdown-in-a-container).

??? question "What does the load average count on Linux? A server shows a load of 20 on 8 CPUs with the CPU at 10 percent. What is going on?"

    The load average is the average over 1, 5, and 15 minutes of the tasks that are runnable or in uninterruptible sleep, so it includes tasks that wait for I/O and not only tasks that wait for a CPU. A load of 20 with 10 percent CPU means that most counted tasks are in `D` state and wait for the disk or a network file system, so the CPUs have nothing to run. Check `vmstat 1` (`b` and `wa`), `iostat -x 1` (`await` and queue size), and the kernel function in `wchan`. If the CPU were high, the same load would mean a CPU shortage with tasks in the run queue. See [Linux for Developers](07-linux-for-developers.md#load-average-uptime-and-top).

??? question "`free` shows only 400 MB free on a 16 GB server. Is the server out of memory?"

    Not necessarily, because the kernel uses idle RAM as page cache and gives it back on demand. Read the `available` column: it estimates how much memory is available for new applications without swapping. If `available` is large, the host is fine, and if it is small, swap use grows (`si` and `so` in `vmstat`), or `dmesg` shows OOM kills, the host is short of memory. Do not use `drop_caches` as a fix, because the next reads go to disk. See [Linux for Developers](07-linux-for-developers.md#free-vmstat-and-memory).

??? question "A Java service uses 400 percent CPU. Walk me through the diagnosis."

    I find the busy threads, map them to Java stacks, and decide whether the cause is application code, the garbage collector, or the JIT. `top -H -p <pid>` gives the busy thread IDs, and three `jcmd <pid> Thread.print -l` dumps taken 5 to 10 seconds apart show what persists. Match the thread ID to `nid`: JDK 21 prints `nid` in decimal and JDK 17 in hexadecimal, so convert only for the older JDK. The same application stack in all dumps means a hot loop, GC threads mean heap pressure, compiler threads after a deploy mean warm-up, and a high `sy` means system calls or lock contention. Confirm with a profile (`asprof -d 30 -f cpu.html <pid>`), and look for the trigger. See [Linux for Developers](07-linux-for-developers.md#a-java-service-with-high-cpu).

??? question "A service has high latency but low CPU. How do you find the cause?"

    Low CPU with high latency means that the threads wait, so I find what they wait for. `curl -w` splits the client-side delay into DNS, TCP, TLS, and time to first byte (the values are cumulative, so subtract). `vmstat` and `iostat -x` show blocked processes, swapping, and disk `await`, and `ss` shows an accept queue that grows (`Recv-Q` on a listening socket) or many `CLOSE_WAIT` sockets. Three thread dumps show where threads wait: an exhausted pool, a slow remote read, or one contended monitor. Also check the GC log for pauses and the cgroup `cpu.stat` for throttling. See [Linux for Developers](07-linux-for-developers.md#a-java-service-with-high-latency-and-low-cpu).
