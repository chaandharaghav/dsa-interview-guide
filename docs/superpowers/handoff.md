# Handoff: Where the Work Stopped

- **Saved:** 2026-10-15, at the user's request. The user continues in a later session.
- **Branch:** `knowledge-base`. `main` is unchanged since the spec and plan commits. Merging is the user's decision at the end of the project.

## SESSION 3 (2026-10-21): HLD finish and Phase 3 DSA

User instruction: keep everything in the local repo (no claude.ai artifacts). Complete HLD and DSA in parts, with subagents. This overrides the earlier "stop after HLD" rule. Reviewers: Opus for all HLD pages and for DSA P1 pages, Sonnet for DSA P2 pages. About 5 agents at a time.

- HLD Batch 6: done and committed (cheatsheet, practice page, README index, `.nav.yml`, glossary terms, study-plan HLD track, home-page status). `kb/prep-timeline.html` is the local timeline page.
- HLD case studies 21–25: reviewed and committed. **HLD (phase 2) is complete.** Mermaid 159 diagrams OK, HLD format, Java, and SQL checks pass.
- Phase 3 plan: `docs/superpowers/plans/2026-10-21-phase-3-dsa.md`. F1 reviewed and committed. F2 in Opus review. F3, F4, N3, N5 drafting. Queued: N1, N2, N4, N6, N7, N8, Batch 3.

## Read First

1. Spec: `docs/superpowers/specs/2026-10-14-software-engineering-kb-design.md`
2. Phase 1 plan: `docs/superpowers/plans/2026-10-14-phase-1-lld.md`
3. Drafting brief (for Sonnet drafters): `docs/superpowers/drafting-brief.md`
4. Review brief (for Opus reviewers): `docs/superpowers/review-brief.md`
5. Phase 1 ledger (rulings): `.superpowers/sdd/2026-10-14-phase-1-lld/progress.md` (git-ignored, local only)

## Process That Works

- One Sonnet drafter per assignment (`model: sonnet`). The prompt tells the drafter to read the drafting brief and its assignment in the phase plan.
- One Opus reviewer per finished draft (`model: opus`). The prompt points to the review brief and lists the drafter's "claims I could not verify".
- Opus (the main session) reads each report, spot-checks the page, and runs the gate.
- Gate: `tools/check-all.sh` (unit tests, format, SQL, Java, `mkdocs build --strict`). Also run `tools/check-mermaid.sh kb/lld` at each batch end. It needs the sandbox disabled, because Chrome must create its profile folder, and it needs network access to unpkg.com.

## Phase Status

| Phase | Status |
|---|---|
| 0: restructure, site, tools | Done. Reviewed by an Opus reviewer. All findings fixed |
| 1: LLD | Done (2026-10-15): 13 pages, 24 case studies, cheatsheet, practice page, all Opus-reviewed |
| 2: HLD | Done (2026-10-21): all pages Opus-reviewed, Batch 6 done |
| 3 to 11 | Not started |

## Phase 1 Detail

| File | Drafted | Opus review |
|---|---|---|
| `fundamentals/01-oop.md` | Yes | Done |
| `fundamentals/02-solid.md` | Yes | Done |
| `fundamentals/03-design-principles.md` | Yes | Done |
| `fundamentals/04-uml-class-and-sequence-diagrams.md` | Yes | Done (finished after the save) |
| `fundamentals/05-domain-modeling.md` | Yes | Done (finished after the save) |
| `fundamentals/06-creational-patterns.md` | Yes | Done |
| `fundamentals/07-structural-patterns.md` | Yes | Done |
| `fundamentals/08-behavioral-patterns.md` | Yes | Done (finished after the save; links to case studies are now backticked paths) |
| `fundamentals/09-concurrency-in-lld.md` | Yes | Done |
| `fundamentals/10-architectural-patterns.md` | Yes | Done |
| `fundamentals/11-anti-patterns.md` | Yes | Done |
| `how-to-approach-lld.md` | Yes | Done (finished after the save) |
| `machine-coding-round.md` | Yes | Done (finished after the save) |

Any agent that was running at save time may have stopped partway. In the next session, compare each such file with the WIP commit (`git diff`). Then run a fresh Opus review of each file whose review did not finish.

## Case-Study Progress (session 2, 2026-10-15)

| Assignment | Folders | Drafted | Opus review |
|---|---|---|---|
| C1 | 01-parking-lot, 02-lru-and-lfu-cache | Yes | Done, committed |
| C2 | 03-rate-limiter, 04-elevator-system | Yes | Done, committed |
| C3 | 05-movie-ticket-booking, 06-splitwise | Yes | Done, committed |
| C4 | 07-vending-machine, 08-logging-framework | Yes | Done, committed |
| C5 | 09-tic-tac-toe, 10-snake-and-ladder | Yes | Done, committed |
| C6 | 11-chess, 12-atm | Yes | Done, committed |
| C7 | 13-library-management, 14-in-memory-file-system | Yes | Done, committed |
| C8 | 15-pub-sub-message-queue, 16-task-scheduler | Yes | Done, committed |
| C9 | 17-ride-sharing, 18-shopping-cart-and-orders | Yes | Done, committed |
| C10 | 19-key-value-store-with-ttl, 20-hotel-booking | Yes | Done, committed |
| C11 | 21-food-delivery, 22-online-auction | Yes | Done, committed |
| C12 | 23-digital-wallet, 24-notification-service | Yes | Running |

Fundamentals 09 and 10: reviewed and committed. All Batch 1 and Batch 2 pages are done. Mermaid check passed for C1 to C5.
Case-study files are not committed until their review finishes. If the session stops, run `git status` to see the drafted folders, then review the ones whose review did not finish.

## NO NEW AGENTS (2026-10-15, user instruction)

The user said: do not start any more agents. All agents that ran at that time have finished. The Opus reviews of S4, S7, S8, S9 and S10 are committed. S11 and S12 are drafted and saved as WIP commits, not reviewed.

S11 drafted, NOT reviewed (WIP commit). Points for its future Opus review: FIX tag numbers and message types from memory (35=D/F/G/8/9, tags 11, 54, 38, 40, 44, 59, 150, 39, 37, 32, 31, 151, 14, session messages); ITCH Order Replace and message P semantics; Photon (SIGMOD 2013) IdRegistry and Paxos details; Mesa (VLDB 2014) versions; the Flink windows docs URL; SEC Rule 15c3-5 text; the Disruptor multi-producer claim; A/B redundant feeds.

S12 drafted, NOT reviewed (WIP commit; all checks and Mermaid pass). Points for its future Opus review:
- Page 23 (email): RFC section numbers cited from memory (RFC 5321 6.3; RFCs 3501, 1870, 2033, 5965, 3464, 3207); SMTP reply codes 450, 451, 452; the 5-minute first retry (an assumption that departs from the RFC 30-minute advice).
- Page 24 (flash sale): Redis, Queue-it and PolarDB-X URLs (from memory or search results); the 100,000 ops/s per Redis shard assumption; the Alibaba hot-row claims (one blog opened).
- Page 25 (code judge): Judge0 default limits (the changelog and the docs disagree); MOSS from memory; the ICPC rules link is the home page only; the Judge0 architecture claims.

When the session resumes: ask the user before starting agents. Still to do for HLD: Opus reviews of S11 and S12 drafts; Batch 6 (cheatsheet, practice page, `kb/hld/README.md` index, `kb/hld/.nav.yml`, glossary terms, study-plan HLD track, home-page status). Reviewed and committed case studies so far: 01–20. Not reviewed: 21–25.

## RESUMED (2026-10-15, after a stop)

The user stopped the session once to save usage, then resumed it. Running after the resume: Opus reviews of H1, H7, H8, H9; Sonnet drafter D2. Queued: reviews of H10, H11, D1; drafter D3, P1, S1 to S12.

**Reviewed and committed (HLD fundamentals):** 03, 04, 05, 06, 07, 08, 09, 10.

**Drafted but NOT reviewed** (saved in a WIP commit; their reviewers were stopped partway, so a page may contain a partial reviewer edit): fundamentals 01, 02 (H1), 11, 12 (H7), 13, 14 (H8), 15, 16 (H9), 17, 18 (H10), 19, 20, 21 (H11), and distributed-systems 01, 02 (D1). Each needs a fresh Opus review (the user chose Opus reviews for all HLD pages). The unverified points for H8 to H11 and D1 are listed below. For H1 and H7, the review prompts listed: Tail at Scale figures, HAProxy options, Nginx `least_time`, Java `networkaddress.cache.ttl` (H1); Resilience4j aspect order, Envoy retry budgets, `grpc-timeout`, Idempotency-Key draft, Lucene BM25 `(k1+1)` factor, OpenSearch fork (H7).

**Not started:** D2, D3, P1 (procedure pages), S1 to S12 (25 case studies), Batch 6 (cheatsheet, practice page, index, nav, glossary, study plan).

**Resume rules:** about 5 agents at a time; Opus reviewers for every HLD page; stop after HLD and tell the user; then the main session moves to Sonnet and P2/P3 pages get Sonnet reviewers.

## Phase 1 Batch 5 and Phase 2 Status (session 2)

- LLD Batch 5: done and committed.
- Phase 2 plan: `docs/superpowers/plans/2026-10-15-phase-2-hld.md` (committed).
- Phase 2 status (update as agents finish): ALL 21 FUNDAMENTALS REVIEWED AND COMMITTED.
  - Reviewed and committed: H2 (03), H3 (04–05), H4 (06–07), H5 (08), H6 (09–10).
  - Drafted, Opus review running: H1 (01–02), H7 (11–12).
  - Drafted, review queued: H8 (13–14, both P1). Unverified points from the drafter: earlier draft header names `RateLimit-Limit/-Remaining/-Reset`; `Retry-After` wording (RFC 9110 10.2.3); Redis Cluster scripts and single-slot keys; Kubernetes `kube-proxy`, readiness probes; Consul interfaces; Eureka as client-side discovery; the "8 to 10 engineers" rule of thumb (no source).
  - Drafted, review queued: H10 (17–18, both P2). Unverified points: Kafka Streams RocksDB stores, changelog topics, standby replicas, one task per partition; Spark Structured Streaming micro-batch and its exactly-once conditions; Kafka tiered storage (KIP-405); Flink and Spark batch modes; PostGIS GiST `<->` kNN and `ST_DWithin` index use; MongoDB `2dsphere` and CockroachDB on S2; Elasticsearch geohash aggregations; Finkel and Bentley 1974 not opened; the geohash precision table is derived (111.19 km per degree); ride-hailing scale numbers labeled assumptions.
  - Drafted, review queued: H9 (15–16, both P1). Unverified points: Snowflake 41/10/12 layout (cited to the README and `IdWorker.scala`, not the blog); Confluent Schema Registry compatibility modes (backward, forward, full, and transitive); CloudEvents attribute names; InnoDB auto-increment gaps after rollback; PostgreSQL sequences do not roll back; PostgreSQL full-page writes for random UUID inserts; Twitter `id_str`; PostgreSQL 18 `uuidv7()` (official docs not opened); "NTP keeps clocks within milliseconds" (unsourced).
  - Drafted, review queued: H11 (19–21, all P2). Unverified points: DR strategy RTO/RPO ranges (labeled indicative, from an earlier whitepaper figure); TLS 1.3 details from RFC 8446 (1-RTT, 0-RTT replay, no static RSA); SPIFFE ID, SVID, Workload API, SPIRE; Route 53 latency routing; GDPR transfer wording; OWASP API Top 10 edition (2023: BOLA first); SRE book "every page must be actionable" paraphrase. Opus checked: KMS Encrypt 4,096-byte limit; Prometheus 1–2 bytes per sample; fiber ~200,000 km/s; NYC–London ~5,570 km.
  - Drafted, review queued: D1 (distributed-systems 01 P2, 02 P1). Unverified points: NTP accuracy (sub-ms LAN to tens of ms Internet); `System.nanoTime` javadoc; Dynamo vector-clock truncation (checked in H3 review: threshold about 10 pairs); Raft dissertation items (single-server membership change, PreVote); Kafka KRaft; MongoDB elections based on Raft; Patroni with etcd, ZooKeeper, or Consul; etcd `concurrency.Election` and lowest-creation-revision; Consul, CockroachDB, TiKV as Raft users; "commit wait is about 2ε" (derived). Opus checked: Deutsch's 7 fallacies plus Gosling's eighth; Cloudflare 2017 leap-second Go negative duration.
  - Review order for the queue: D1 (H9, H10, H11 reviews started).
  - Not started: D2, D3, P1 (procedure pages), S1 to S12 (case studies), Batch 6.
- Usage rule (user request, 2026-10-15): about 5 agents at a time by default; the user raised it to about 10 after the limit reset. User decision (Option A): Opus reviewers for every HLD page. **Stop when HLD is complete and tell the user.** After HLD: main session on Sonnet, Opus reviewers for P1 pages only, Sonnet reviewers for P2/P3.

## Next Steps, in Order

1. Run the Opus review of `fundamentals/09` and `fundamentals/10` (the only unreviewed pages; see the notes below). Every other Batch 1 and Batch 2 page is reviewed and committed. No agent is running.
2. Run `python3 "$TMPDIR/add_imports.py"` on pages whose runnable blocks lack imports. If `$TMPDIR` was cleared, regenerate the script: for each Java block with `main` that fails to compile as written, add explicit `import` lines for the missing JDK classes. `tools/run_java.py kb/lld` lists these blocks as "does not run ... cannot find symbol".
3. Run `tools/check-all.sh` and `tools/check-mermaid.sh kb/lld`. Fix the failures.
4. Commit Batch 1 and Batch 2 (`docs: add LLD fundamentals and procedure pages`).
5. Batch 3 and Batch 4: the 24 case studies (assignments C1 to C12 in the phase plan). Run at most 5 drafters at the same time.
6. Batch 5: cheatsheet, practice page, `kb/lld/README.md` index, `kb/lld/.nav.yml` titles, glossary terms, study plan track, and home page status.
7. Then Phase 2 (HLD): write its plan in the same form as the Phase 1 plan.

## Notes for the 09 and 10 Review

Give these points to the Opus reviewer:

- The pages use short references (`java/07`, `java/08`, `databases/04`, `operating-systems/03`, `hld/fundamentals/16`). Change each one to the full backticked path from the spec inventory, for example `kb/java/07-concurrency-basics.md`.
- Claims the drafter did not verify: the Fowler catalog summaries for Repository, Unit of Work, Service Layer, and DTO; the MVC dates and attribution (Reenskaug, Xerox PARC) and the MVP/MVVM table; Fowler's CQRS warning (paraphrased); the Guava `Striped`, jcstress, and MapStruct mentions; "ConcurrentHashMap locks the head node of a bin (JDK 8+)"; `LinkedBlockingQueue` uses two locks and `ArrayBlockingQueue` uses one.
- Page 09 passed 7 runs in a row, so its thread demos are stable.

## Open Items

- Deferred minor findings: two `public` top-level types in one block still pass. The checker does not enforce the Interview Angle subsections or the "preview in Java 21" label. Unit tests print runner output.
- `kb/dsa/.nav.yml` must put `foundations/` before the patterns, and add patterns 21 to 28 under Patterns (Phase 3).
- Plain-text references to planned pages use backticked `kb/...` paths. Phase 11 turns them into links.
