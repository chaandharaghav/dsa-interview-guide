# Phase 6: Operating Systems and Networking — Implementation Plan

**Goal:** Write the complete `kb/operating-systems/` subject (7 topic pages and a question bank) and the complete `kb/networking/` subject (9 topic pages and a question bank).

**Spec:** `docs/superpowers/specs/2026-10-14-software-engineering-kb-design.md` (sections 3.6, 3.7, 4, 5.1, 6, 7, 8, 9)

**Process:** a Sonnet drafter per assignment reads `docs/superpowers/drafting-brief.md` and its assignment in this plan. A Sonnet reviewer per finished draft reads `docs/superpowers/review-brief.md`. (User instruction, 2026-11-05: "similarly use sonnet 5.5 for this".) Opus (the main session) reads every P1 model answer after the review, runs the gate, and commits each assignment. About 5 agents run at the same time.

## Batch order

| Batch | Assignments | Why this order |
|---|---|---|
| 1 | O1–O3 (OS 01, 03, 04) and N1–N4 (networking 03, 04, 05, 06): all P1 | The core pages. Other pages link to them |
| 2 | O4, O5 (OS 02, 05, 06, 07) and N5–N7 (networking 01, 02, 07, 08, 09) | Networking 07 walks through the protocols of batch 1 |
| 3 | O6 and N8: the two question banks (after all reviews), then the index updates (Opus writes) | Each bank links to every page of its subject |

Rules for links: pages of your batch and of earlier batches exist when your batch ends, so link to them with relative links. A page of a later batch is a backticked path, for example `kb/networking/07-what-happens-when-you-load-a-url.md`. The main session converts these paths to links at the end of the phase. The DSA, LLD, HLD, Java, and Databases pages exist, so link to them. Every other planned page is a backticked path from spec section 3, with its exact file name.

## Common assignment text

Every assignment includes this text.

> Read `docs/superpowers/drafting-brief.md` in full before you start. It is binding. Then read the subject's `README.md` (`kb/operating-systems/README.md` or `kb/networking/README.md`) for the page list, and this plan's sections "Sources and versions", "Java rules", "Overlap rules", and "Promises from other pages". The reader is a Java developer who prepares for backend interviews at product companies. Each page explains how the mechanism works inside the kernel or the protocol, not only the vocabulary, and connects it to what a Java backend developer sees (a JVM thread, a socket timeout, a connection pool, an HTTP client setting). State each protocol fact from its RFC or standard, and each operating system fact from the Linux man pages (man7.org), the Linux kernel documentation (docs.kernel.org), or an original paper, and say which. Do not rely on memory for a default value, a limit, a timer, or a version.

### Sources and versions (checked on 2026-11-05; check again for each claim that you make)

- **Protocol standards:** cite the current RFC, and say when an RFC replaced an older one. Examples to check: TCP (RFC 9293 replaced RFC 793), HTTP semantics (RFC 9110), HTTP/1.1 (RFC 9112), HTTP/2 (RFC 9113), HTTP/3 (RFC 9114), QUIC (RFC 9000), TLS 1.3 (RFC 8446 and any revision), DNS (RFC 1034, 1035, and their updates), WebSocket (RFC 6455), cookies (RFC 6265 and the status of its revision), CORS and fetch (the WHATWG Fetch Standard), Server-Sent Events (the WHATWG HTML Standard). Use rfc-editor.org or datatracker.ietf.org to check the status of each RFC and draft today.
- **Linux:** check the current stable kernel on kernel.org before you name a version. Cite man pages from man7.org and kernel docs from docs.kernel.org. Where a kernel release changed a behavior that the page states (for example the default CPU scheduler), name the release.
- **JDK:** the KB code targets Java 21 (`tools/run_java.py`). Where a later JDK changed a networking or I/O API on your page (for example HTTP/3 in `java.net.http.HttpClient`), check the JEP status on openjdk.org today and say which release.
- **Numbers:** a latency, a timer, a buffer size, or a limit carries its source. Label a measured or typical number "approximate". Use `kb/hld/back-of-envelope-estimation.md` for latency numbers where it has them, so that the KB stays consistent.

### The host is macOS on Apple Silicon, not Linux

The machine that runs the drafters is macOS on an Arm (Apple Silicon) processor. A local command returns Darwin or Arm facts: the memory page size is 16 KB, `sysctl hw.cachelinesize` returns 128, `strace`, `ss`, and `/proc` do not exist, and `top`, `ps`, and `netstat` print BSD formats. **A local command never counts as evidence for a Linux or x86-64 claim.** Take Linux behavior and tool output from man7.org or docs.kernel.org, and x86-64 facts from vendor manuals (Intel, AMD) or the kernel docs. Label any tool output that you did not copy from such a source "Illustrative output, not from a real run".

### Scratch files

Save every download and every scratch file only in `scratchpad/<assignment id>/` under the repo root. Before you finish, run `git status` and report any file that you created outside your page and your scratch folder.

### One home for each shared topic inside phase 6

Several drafters write related protocols at the same time. Each topic below has one home page in this phase. Other pages give at most two sentences and link to the home (or name it as a backticked path if it is in a later batch).

| Topic | Home | Other pages |
|---|---|---|
| 0-RTT and its replay risk | Networking 05 (TLS) | Networking 04 links there for QUIC and HTTP/3 |
| The QUIC handshake and transport (streams, connection migration) | Networking 04 (HTTP) | Networking 03 gives one paragraph at the UDP level. Networking 05 gives one sentence on TLS inside QUIC (RFC 9001) |
| The HTTPS and SVCB DNS records | Networking 06 (DNS) | Networking 04 links there for HTTP/3 discovery |
| Head-of-line blocking across HTTP/1.1, HTTP/2, and HTTP/3 | Networking 04 | Networking 03 covers head-of-line blocking at the TCP level only |
| TCP keep-alive | Networking 03 | Networking 04 owns HTTP persistent connections. Networking 09 links to 03 for NAT idle timeouts |
| Context switch cost | OS 01 | OS 02 links there |
| The TLB and huge pages | OS 04 | OS 06 links there |
| The page cache | OS 05 | OS 04 gives one paragraph and links there |
| Signals | OS 07 | OS 01 gives one paragraph and links there |

### Existing KB statements on phase 6 topics

Some existing pages state facts that phase 6 now owns. The drafter and the reviewer of the named page find each statement, check it, and report any conflict to the main session. Do not edit the other page: the main session fixes it.

| Page | Statements to check |
|---|---|
| Networking 06 | `kb/hld/fundamentals/02-load-balancing-dns-cdn.md` states the JVM DNS cache (`networkaddress.cache.ttl`) and DNS TTL behavior |
| Networking 05 | `kb/hld/fundamentals/21-security-in-design.md` and `kb/hld/fundamentals/02-load-balancing-dns-cdn.md` state TLS 1.3 details (1-RTT, 0-RTT replay, no static RSA) and TLS termination |
| Networking 04 | `kb/hld/fundamentals/02-load-balancing-dns-cdn.md` and `kb/hld/fundamentals/03-caching.md` state `Cache-Control` behavior |
| Networking 08 | `kb/hld/fundamentals/09-communication-patterns.md` states WebSocket and SSE behavior |
| OS 03 | `kb/java/07-concurrency-basics.md` states the Coffman conditions and deadlock handling |

### Java rules

- Every `java` block must compile under `tools/run_java.py` (Java 21). A block with `main` also runs.
- **The sandbox blocks sockets.** A program with `main` must not open a socket, bind a port, resolve a host name, or use the network: the runner runs in a sandbox that refuses local port binding, and the output must not depend on the machine. Show the mechanism with a pure JDK simulation that prints deterministic output. Examples: a TCP state machine, a sliding window with flow control, slow start and AIMD congestion control, a CIDR and subnet calculator, a longest-prefix-match routing table, a DNS resolver walk over an in-memory zone map, a TLS 1.3 handshake message trace, an HTTP/1.1 request parser over a string, a WebSocket frame encoder and decoder over bytes, a NAT translation table, a CPU scheduling simulator, a page replacement simulator, a cache simulator that counts hits and misses.
- Real socket, `HttpClient`, `SSLContext`, or `InetAddress` code may appear as a class **without** `main`, with explicit `import` lines: the runner compiles it and does not run it. Say in the text that the class is not run.
- Do not print times, thread names, process IDs, host names, or addresses that depend on the machine. A program that uses threads joins them before it prints.

### Overlap rules (spec section 4)

Read each page that this list names for your page. Your page must not contradict it, must not repeat it, and must link to it.

| Topic | Home page | What the phase 6 page does |
|---|---|---|
| Java threads, `synchronized`, `volatile`, the Java Memory Model, deadlock in Java code | `kb/java/07-concurrency-basics.md` | OS 01 and OS 03 explain the kernel and hardware side (kernel threads, futex, spinlock, scheduling) and link there |
| `java.util.concurrent`: locks, CAS, `LongAdder`, thread pools | `kb/java/08-java-util-concurrent.md` | OS 03 and OS 06 explain the hardware instructions and cache effects and link there |
| Classic concurrency problems in Java code (producer-consumer, reader-writer lock, dining philosophers) | `kb/java/11-concurrency-coding-problems.md` | OS 03 states each classic problem, its OS-level analysis (which deadlock condition it breaks, starvation), and links there for the code. OS 03 adds only problems that page 11 lacks |
| Virtual threads | `kb/java/10-virtual-threads.md` | OS 01 explains the thread models (1:1, M:N) and links there |
| JVM heap, GC, object layout | `kb/java/06-memory-and-gc.md` | OS 04 explains virtual memory, paging, and the TLB, and links there for the heap |
| Java streams, NIO buffers, channels, selectors | `kb/java/15-io-nio-and-serialization.md` | OS 05 explains the kernel side (file descriptors, the page cache, `epoll`, zero-copy) and links there for the Java API |
| Load balancers, L4 and L7, TLS termination, GeoDNS, anycast, CDNs | `kb/hld/fundamentals/02-load-balancing-dns-cdn.md` | Networking 06 owns the DNS protocol, networking 09 owns proxies at the protocol level, networking 05 owns the TLS handshake. Each links there for the design view |
| Real-time push techniques in a design (polling, long polling, SSE, WebSocket) | `kb/hld/fundamentals/09-communication-patterns.md` | Networking 08 owns the protocol level: handshake bytes, frames, masking, close codes, SSE event format, reconnection |
| Security design (mTLS between services, encryption in transit) | `kb/hld/fundamentals/21-security-in-design.md` | Networking 05 owns the TLS protocol and links there |
| CORS and CSRF in Spring | `kb/java/21-spring-security.md` | Networking 04 owns the CORS protocol (preflight, the headers) and links there for Spring configuration. CSRF has its home in the security pages (phase 7): one sentence and a backticked path |
| HTTP semantics against REST design | Networking 04 (this phase) is the home of HTTP semantics (RFC 9110): methods, safe and idempotent methods, status code classes, headers, caching, cookies | `kb/api-design/01-rest-principles.md` (phase 7) will cover how a REST API uses them. Networking 04 refers to it by backticked path |
| Caching headers in a design | `kb/hld/fundamentals/03-caching.md` and `02-load-balancing-dns-cdn.md` | Networking 04 owns the header semantics (RFC 9111) and links there for cache placement |
| Latency numbers | `kb/hld/back-of-envelope-estimation.md` | Every page uses its numbers and links there |
| Cryptography (ciphers, hashes, signatures) | `kb/security/05-cryptography-basics.md` (phase 7) | Networking 05 explains what TLS uses each primitive for, in one or two sentences each, and refers there by backticked path |

### Promises from other pages

Other pages already point to the phase 6 pages with backticked paths. Each promise below is a coverage item of the named page.

| Page | Promised content |
|---|---|
| Networking 01 | The OSI layer numbers that "L4" and "L7" refer to (`hld/fundamentals/02`) |
| Networking 04 | HTTP caching header details, including `stale-while-revalidate` and `stale-if-error` (`hld/fundamentals/02`, `hld/fundamentals/03`); HTTP details for REST (`hld/fundamentals/09`) |
| Networking 05 | Why TLS termination at the edge shortens the handshake for the user (`hld/fundamentals/02`); TLS protocol details behind certificate renewal (`hld/fundamentals/21`) |
| Networking 06 | DNS protocol details: resolution, caching and TTL, negative caching (RFC 2308) (`hld/fundamentals/02`, `hld/fundamentals/19`, `hld/case-studies/08`) |
| Networking 08 | WebSocket frames, the handshake bytes, close codes; the connection-balancing problem of long-lived connections at the protocol level (`hld/fundamentals/02`, `hld/fundamentals/09`, `hld/case-studies/06`) |
| Networking 09 | Forward and reverse proxies at the protocol level (`hld/fundamentals/02`) |
| OS 03 | The operating system view of synchronization (`java/07`, `java/08`, `lld/fundamentals/09`) |
| OS 05 | I/O models and the event loop at the kernel level (`kb/prep-timeline.html`, JavaScript track) |

## Batch 1: P1 pages

Each page uses the topic template (drafting brief section 4.1).

### O1 — `kb/operating-systems/01-processes-and-threads.md` (P1)

What a process is (address space, the process control block, file descriptors, credentials) and what a thread is (its own stack, registers, and thread-local state; shared address space). Process states and their transitions. `fork`, `exec`, `wait`, `exit`, zombie and orphan processes, copy-on-write on `fork`. Threads in Linux: `clone` and the task model, kernel threads against user threads, the 1:1 model (Linux NPTL, Java platform threads) against M:N (Go, Java virtual threads; link `kb/java/10-virtual-threads.md`). User mode against kernel mode, the system call path, and its cost. A context switch: what the kernel saves and restores, why a thread switch is cheaper than a process switch, the indirect costs (cache and TLB effects), and approximate numbers with a source. Interprocess communication: pipes, signals (one paragraph; the home is OS 07), shared memory, sockets, message queues. Process against thread for isolation, crash containment, and memory cost; why Chrome uses processes and a JVM uses threads. How a Java thread maps to an OS thread (`Thread.start` creates a native thread in HotSpot; link `kb/java/07-concurrency-basics.md` for the Java side). Java example: a simulation of the process state machine, or a program that shows thread-shared heap state against per-thread stack state (deterministic, joined threads).

### O2 — `kb/operating-systems/03-synchronization.md` (P1)

The critical-section problem and its three requirements (mutual exclusion, progress, bounded waiting). Hardware support: atomic test-and-set, compare-and-swap, fetch-and-add, memory barriers (link `kb/java/08-java-util-concurrent.md` for CAS in Java). Spinlocks against blocking locks, and when spinning wins. How Linux builds a blocking mutex on the futex system call (the fast path in user space, the slow path in the kernel). Semaphores (Dijkstra's P and V, counting and binary), and a semaphore against a mutex (ownership). Monitors and condition variables: Hoare against Mesa semantics, and why Mesa semantics require the `while` loop that Java uses (link `kb/java/07-concurrency-basics.md`). Readers-writers locks and their fairness choices. Deadlock: the four Coffman conditions, the resource-allocation graph, and the four strategies (prevention by breaking a condition, avoidance with the banker's algorithm, detection and recovery, ignoring it), with what real systems do. Livelock and starvation in brief (Java examples are on `java/07`). Priority inversion, the Mars Pathfinder case, and priority inheritance. The classic problems: producer-consumer, readers-writers, dining philosophers, sleeping barber; for each, the OS-level analysis and a link to `kb/java/11-concurrency-coding-problems.md` where that page has the Java code. Java example: the banker's algorithm safety check, or a deadlock detector over a resource-allocation graph (pure JDK, deterministic).

### O3 — `kb/operating-systems/04-memory-management.md` (P1)

Why virtual memory: isolation, the illusion of a large contiguous space, sharing, overcommit. Address translation: pages and frames, page tables, multi-level page tables (x86-64 four and five levels), the page table entry bits, the TLB and its misses, huge pages and transparent huge pages. Page faults: minor against major, demand paging, copy-on-write. Page replacement: FIFO, optimal, LRU, clock (second chance), Belady's anomaly, and what Linux does (the active and inactive lists; the multi-generational LRU in newer kernels: check the kernel docs and name the release). Thrashing and the working set. Swap, the OOM killer and `oom_score_adj`, overcommit modes (`vm.overcommit_memory`). Memory-mapped files (`mmap`) and the page cache (one paragraph; OS 05 is the home of the page cache). Segmentation in one paragraph (history, and why x86-64 does not use it for protection). The process address space layout (text, data, heap, stack, mmap area) and `brk` against `mmap` for `malloc`. What this means for a JVM: the heap is virtual memory, `-Xms` against resident memory, `-XX:+AlwaysPreTouch`, large pages for the heap, container memory limits (link `kb/java/06-memory-and-gc.md` for the heap and GC). Java example: a page replacement simulator for FIFO, LRU, OPT, and clock that prints the fault counts and shows Belady's anomaly for FIFO (pure JDK, deterministic).

### N1 — `kb/networking/03-tcp-and-udp.md` (P1)

TCP as a reliable byte stream over an unreliable network; UDP as a datagram service. The TCP header fields that matter. The three-way handshake (with sequence numbers), and why three messages; SYN flood and SYN cookies. Connection teardown with FIN, the half-close, RST. The TCP state machine, TIME_WAIT (why it exists, its duration as 2×MSL in the RFC against what Linux uses: check `TCP_TIMEWAIT_LEN` in the kernel source and whether it is tunable; the problems it causes on busy clients and servers; the real fixes, such as connection reuse and `tcp_tw_reuse`, against harmful ones; check whether `tcp_tw_recycle` still exists before you name it), CLOSE_WAIT and what it means about the application. Reliability: sequence numbers, cumulative ACKs, retransmission timeout (RTO computation, RFC 6298), fast retransmit, selective acknowledgment (SACK). Flow control: the receive window, window scaling, zero window. Congestion control: slow start, congestion avoidance (AIMD), fast recovery, the congestion window, CUBIC (RFC 9438) as the Linux default, BBR in brief (check its standards status), and the bandwidth-delay product. Nagle's algorithm and delayed ACKs, and why `TCP_NODELAY` matters for request-response protocols. Keep-alive. Head-of-line blocking in TCP. UDP: header, no connection, no ordering; when to choose it (DNS, video, games, QUIC). What a Java developer sees: `connect` and `read` timeouts, `SO_TIMEOUT`, connection pool reuse to skip handshakes, `Connection reset by peer`, ephemeral port exhaustion. Java example: a simulation of slow start and AIMD over a list of round trips with a loss event, or a TCP state machine driven by an event list (pure JDK).

### N2 — `kb/networking/04-http.md` (P1)

HTTP semantics (RFC 9110): the request and response model, methods (GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS), safe and idempotent methods and why the difference matters for retries, status code classes and the codes that interviews ask (200, 201, 204, 301, 302, 303, 304, 307, 308, 400, 401, 403, 404, 409, 412, 422, 429, 500, 502, 503, 504), headers. HTTP/1.1 (RFC 9112): the message format, persistent connections, pipelining and why browsers do not use it, chunked transfer, `Host`. HTTP/2 (RFC 9113): binary framing, streams and multiplexing over one connection, HPACK, flow control, server push and its removal from browsers, head-of-line blocking that moves to TCP. HTTP/3 (RFC 9114) over QUIC (RFC 9000): streams in the transport, 0-RTT and its replay risk, connection migration, QPACK, how a client discovers HTTP/3 (`Alt-Svc`, the HTTPS DNS record). Caching (RFC 9111): `Cache-Control` directives (`max-age`, `s-maxage`, `no-cache`, `no-store`, `private`, `public`, `must-revalidate`, `immutable`, `stale-while-revalidate`, `stale-if-error`), validators (`ETag`, `Last-Modified`), conditional requests (`If-None-Match`, `If-Modified-Since`, `If-Match` for optimistic concurrency), `Vary`. Cookies: attributes (`Domain`, `Path`, `Secure`, `HttpOnly`, `SameSite`, `Max-Age`), and their security purpose. CORS (WHATWG Fetch): the same-origin policy, simple requests against preflighted requests, the `Access-Control-*` headers, credentials; link `kb/java/21-spring-security.md` for Spring. Content negotiation and compression in brief. What a Java developer sees: `java.net.http.HttpClient` and its HTTP/2 support (check the HTTP/3 JEP status), connection reuse. Java example: an HTTP/1.1 request parser and a conditional-GET cache decision over in-memory strings (pure JDK), plus an optional `HttpClient` class without `main`.

### N3 — `kb/networking/05-tls-and-https.md` (P1)

What TLS gives: confidentiality, integrity, server (and optionally client) authentication. The TLS 1.3 handshake (RFC 8446) message by message: `ClientHello` with key shares, `ServerHello`, the encrypted extensions, `Certificate`, `CertificateVerify`, `Finished`; 1-RTT, and 0-RTT resumption with its replay risk. What changed from TLS 1.2 (removed static RSA key exchange and other legacy features, forward secrecy always). Key exchange with ECDHE, the key schedule with HKDF in brief, the AEAD record protection; each primitive gets one or two sentences and a backticked reference to `kb/security/05-cryptography-basics.md`. Certificates: X.509 fields that matter (subject alternative names, validity, key usage), the chain of trust from the leaf to an intermediate to a root, how a client validates a chain, trust stores (the JDK `cacerts`), revocation (CRL, OCSP, OCSP stapling, and the current status of OCSP in the Web PKI: check), Certificate Transparency, certificate lifetimes (check the current CA/Browser Forum ballot on maximum validity), ACME and automated renewal. SNI and Encrypted Client Hello (check its RFC status). Post-quantum hybrid key exchange (check the current deployment status of X25519MLKEM768 from primary sources). mTLS (link `kb/hld/fundamentals/21-security-in-design.md` for the design). TLS termination and why it shortens the handshake for the user (link `kb/hld/fundamentals/02-load-balancing-dns-cdn.md`). HTTPS-specific items: HSTS, mixed content. What a Java developer sees: `SSLHandshakeException` and `PKIX path building failed`, trust store and key store configuration, the JDK TLS defaults (check the JDK 21 security docs). Java example: a trace of the TLS 1.3 handshake messages and the chain-validation steps over an in-memory chain model (pure JDK), plus an optional `SSLContext` class without `main`.

### N4 — `kb/networking/06-dns.md` (P1)

The DNS name space and zones, authoritative servers, recursive resolvers, root servers, TLD servers. Recursive against iterative resolution, step by step for `www.example.com`. Caching at each level: the TTL, negative caching (RFC 2308), the stub resolver, the operating system cache, and the JVM cache (`networkaddress.cache.ttl` and `networkaddress.cache.negative.ttl`: check the JDK 21 defaults and the security manager condition). Record types: A, AAAA, CNAME (and why a CNAME cannot sit at the zone apex), ALIAS or ANAME, MX, NS, SOA, TXT, SRV, CAA, PTR, HTTPS and SVCB. The message format and transport: UDP port 53, truncation and fallback to TCP, EDNS(0). Security: cache poisoning and the Kaminsky attack, source port randomization, DNSSEC (signatures, the chain of trust, DS records) and what it does not protect, DNS over TLS (RFC 7858) and DNS over HTTPS (RFC 8484). DNS in operations: TTL choice before a migration, propagation as cache expiry, split-horizon DNS, Kubernetes service discovery DNS in brief. GeoDNS and anycast have their home in `kb/hld/fundamentals/02-load-balancing-dns-cdn.md`: link there. Java example: an iterative resolver walk over an in-memory map of zones that follows NS referrals and CNAMEs and applies TTL caching on a simulated clock (pure JDK, deterministic).

## Batch 2: P2 pages and the URL walkthrough

### O4 — `kb/operating-systems/02-cpu-scheduling.md` (P2), `kb/operating-systems/06-computer-architecture-for-developers.md` (P2)

`02`: scheduling goals (throughput, turnaround, response time, fairness) and the conflicts between them; preemptive against non-preemptive. Algorithms: FCFS, SJF and SRTF, round robin and the time quantum, priority scheduling and aging, multilevel feedback queues, lottery and stride scheduling in brief. Linux: CFS (virtual runtime, the red-black tree) and its replacement by EEVDF (check the release that made EEVDF the default), scheduling classes (`SCHED_OTHER`, `SCHED_FIFO`, `SCHED_RR`, `SCHED_DEADLINE`), nice values, `sched_ext` in brief (check the release). Multiprocessor scheduling: load balancing, CPU affinity, NUMA. Containers: CPU shares against CPU quotas (CFS bandwidth control), throttling, and what it means for a JVM (`availableProcessors`, GC threads; link `kb/java/06-memory-and-gc.md`). Java example: a scheduling simulator for FCFS, SJF, round robin, and MLFQ over a fixed job list that prints the waiting and turnaround times (pure JDK).

`06`: the memory hierarchy with approximate latencies (registers, L1, L2, L3, DRAM, SSD; link `kb/hld/back-of-envelope-estimation.md`). Caches: lines (64 bytes on current x86-64 and many Arm cores: check), associativity, spatial and temporal locality, why array traversal order matters, row-major against column-major. Cache coherence (the MESI protocol in brief), false sharing and its fixes (padding, `@Contended` in the JDK, `LongAdder`; link `kb/java/08-java-util-concurrent.md`). Memory ordering and barriers in hardware (x86-TSO against weaker Arm ordering), and why the Java Memory Model exists (link `kb/java/07-concurrency-basics.md`). Pipelining, branch prediction and the cost of a misprediction, speculative execution and the Spectre class in one paragraph. SIMD in brief (the Vector API is incubating: check its JEP status). NUMA. What this means for data structures: arrays against linked lists, object layout and pointer chasing. Java example: a set-associative cache simulator that counts hits and misses for row-major and column-major traversal of a matrix (pure JDK, deterministic; do not measure time).

### O5 — `kb/operating-systems/05-file-systems-and-io-models.md` (P2), `kb/operating-systems/07-linux-for-developers.md` (P2)

`05`: files and file descriptors (the per-process table, the open file table, inodes), inodes and directories, hard links against symbolic links, what `rm` does to an open file. Journaling (ext4 modes) and why `fsync` matters, with `fsync` on the directory for a new file (link `kb/databases/06-storage-engines.md` for the database use). The page cache, buffered writes, write-back, `O_DIRECT`. I/O models: blocking, non-blocking, I/O multiplexing (`select`, `poll`, `epoll` with level-triggered against edge-triggered), signal-driven, asynchronous I/O (POSIX AIO and `io_uring`: check its current status). The C10K problem, the reactor pattern and the event loop (Netty, Node.js), and how Java NIO selectors map to `epoll` (link `kb/java/15-io-nio-and-serialization.md`). Zero-copy: `sendfile`, `splice`, `mmap`, and `FileChannel.transferTo` in Java. Blocking I/O and virtual threads (link `kb/java/10-virtual-threads.md`). Java example: a simulation of a readiness-based event loop over in-memory "connections" that shows level-triggered against edge-triggered behavior, or a model of the descriptor, open-file, and inode tables (pure JDK).

`07`: processes and signals (`SIGTERM` against `SIGKILL`, `SIGINT`, `SIGHUP`, `SIGQUIT` and the JVM thread dump, `SIGSEGV`), graceful shutdown in a container (PID 1, `exec` form, JVM shutdown hooks). Permissions (owner, group, other; `rwx`; `chmod` in octal; setuid and the sticky bit; `umask`), users and root, capabilities in brief. Shell basics a developer needs: pipes, redirection, exit codes, environment variables, `grep`, `awk` and `sed` in one-liners, `find`, `xargs`. Diagnostic tools, each with what it shows and a worked example of reading its output: `top` and `htop` (load average and what it counts on Linux, CPU states, `wa`, `st`), `vmstat`, `iostat`, `free` (and why "free" memory is low), `ps`, `strace`, `lsof`, `ss` (and the socket states from the TCP page), `netstat` as the old tool, `tcpdump`, `dig`, `curl -v`, `perf` in brief, `dmesg` for the OOM killer, `journalctl`. JVM tools on Linux: `jcmd`, `jstack`, async-profiler in brief. A troubleshooting walkthrough: a Java service with high CPU, then one with high latency. Use `bash` and `text` blocks for commands and output; label output that you did not capture on a real machine "Illustrative output". Java example: a shutdown hook and signal-safe cleanup pattern as a class without process-dependent output, or omit the section with an `omit` marker and a reason.

### N5 — `kb/networking/01-osi-and-tcp-ip-models.md` (P2), `kb/networking/02-ip-addressing-and-routing.md` (P2)

`01`: the seven OSI layers and the four TCP/IP layers, what each layer does, its unit of data (frame, packet, segment, message), and the protocols at each layer; encapsulation and decapsulation, headers added at each layer; where devices work (switch at L2, router at L3, L4 and L7 load balancers: link `kb/hld/fundamentals/02-load-balancing-dns-cdn.md`); MTU, fragmentation, and path MTU discovery; Ethernet and ARP in brief; why the OSI model is a teaching model. Java example: an encapsulation simulation that wraps an application message in TCP, IP, and Ethernet header records and prints the sizes and the overhead (pure JDK).

`02`: IPv4 addresses, private ranges (RFC 1918), special ranges (loopback, link-local); subnets and CIDR (RFC 4632) with worked calculations (network address, broadcast, host count); IPv6 (RFC 8200) addressing, notation, why NAT is not needed, dual stack, Happy Eyeballs (check the current RFC); routing basics: the routing table, longest prefix match, the default gateway, how a packet hops; BGP and autonomous systems in one section (why BGP incidents take sites down); ICMP, `ping`, `traceroute` and how TTL makes `traceroute` work. Java example: a CIDR calculator and a longest-prefix-match lookup over a small routing table (pure JDK, integer arithmetic, no `InetAddress` resolution).

### N6 — `kb/networking/07-what-happens-when-you-load-a-url.md` (P1)

The full path of `https://www.example.com/page` step by step, with the timing cost of each step and a link to the page that explains it: URL parsing and HSTS check; the browser cache and the service worker; DNS resolution (link `06-dns.md`); the TCP handshake (link `03-tcp-and-udp.md`) or the QUIC handshake for HTTP/3; the TLS handshake (link `05-tls-and-https.md`); the HTTP request (link `04-http.md`); on the server side the path through a CDN, a load balancer, a reverse proxy, the application server, a cache, and the database (link the HLD pages); the response, caching headers, rendering in brief (HTML parse, CSS, JavaScript, the critical rendering path) and the follow-up requests; connection reuse. A Mermaid sequence diagram of the whole path. A table of the round trips for a cold connection against a warm one, and for HTTP/1.1 against HTTP/2 against HTTP/3. Interview angle: how to structure the answer, how deep to go at each step, and the follow-up questions that interviewers ask at each step. The page file name starts with `07-`, so `check_format.py` enforces the full topic template. Fill "Trade-offs and When to Use" with the comparisons that the walkthrough raises (a cold connection against a warm one, HTTP/1.1 against HTTP/2 against HTTP/3, a CDN hit against an origin request) and "Commonly Confused With" with pairs such as DNS resolution against the HTTP redirect, TLS termination at the edge against end-to-end TLS, and the browser cache against a CDN cache. Omit a section only with an `omit` marker and a reason; do not pad. Java example: a simulation that adds up the round trips of each step for a cold and a warm request, from a given round-trip time (pure JDK).

### N7 — `kb/networking/08-websockets-and-realtime.md` (P2), `kb/networking/09-proxies-and-nat.md` (P2)

`08`: the WebSocket opening handshake (`Upgrade`, `Sec-WebSocket-Key`, `Sec-WebSocket-Accept` computed with SHA-1 and the GUID from RFC 6455), the frame format (FIN, opcode, mask bit, payload length forms, masking key and why clients mask), control frames (ping, pong, close), close codes, subprotocols, WebSocket over HTTP/2 (RFC 8441) in brief. Server-Sent Events: the `text/event-stream` format, fields (`data`, `event`, `id`, `retry`), `Last-Event-ID` and reconnection. Long polling at the protocol level. Proxy and load balancer behavior for long-lived connections: idle timeouts, keep-alive pings, sticky routing, and the connection-balancing problem after a deploy (link `kb/hld/fundamentals/02-load-balancing-dns-cdn.md`); the design choice between techniques lives in `kb/hld/fundamentals/09-communication-patterns.md`. Java example: compute `Sec-WebSocket-Accept` for the RFC 6455 sample key (the RFC gives the expected value), and encode and decode a masked text frame over bytes (pure JDK).

`09`: forward proxy against reverse proxy at the protocol level: the absolute-form request target, `CONNECT` tunneling for HTTPS, transparent proxies; headers that proxies add (`Via`, `X-Forwarded-For`, `X-Forwarded-Proto`, `Forwarded` (RFC 7239)) and why a server must trust them only from known proxies; the PROXY protocol (HAProxy) for L4 proxies; the design view of reverse proxies and load balancers lives in `kb/hld/fundamentals/02-load-balancing-dns-cdn.md`. NAT: why it exists, source NAT and port translation (NAPT), destination NAT and port forwarding, the translation table and its timeouts, NAT types in brief, NAT traversal (STUN, TURN, ICE) for peer-to-peer and WebRTC, carrier-grade NAT, and the effect of NAT idle timeouts on long-lived TCP connections (keep-alive). Java example: a NAPT translation table simulation that maps internal (address, port) pairs to external ports, expires idle entries on a simulated clock, and drops unsolicited inbound packets (pure JDK).

## Batch 3: Question banks and index

### O6 — `kb/operating-systems/interview-question-bank.md` (P1), and N8 — `kb/networking/interview-question-bank.md` (P1)

Each bank: at least 40 `??? question` blocks (the checker needs 25), grouped by page in page order, more questions for P1 pages than for P2 pages. Each answer is two to five sentences, correct, and ends with a link to the section of the page that explains it. Draw the questions from the reviewed pages: do not introduce facts that no page states. Front matter `priority: P1`. These pages use no section template. Verify anchors with `.venv/bin/mkdocs build --strict` (never `--quiet`).

### Index updates (Opus writes)

`kb/operating-systems/README.md` and `kb/networking/README.md` indexes with links; the two `.nav.yml` files; glossary terms; an OS and Networking track in `kb/study-plan.md`; the status in `kb/README.md`; the status section and the per-subject rows of `kb/prep-timeline.html` (including its `<code>` references to the phase 6 pages); the conversion of every backticked `kb/networking/...` and `kb/operating-systems/...` path in the KB to a relative link; the handoff and the project memory.
