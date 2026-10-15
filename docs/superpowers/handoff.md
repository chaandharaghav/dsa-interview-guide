# Handoff: Where the Work Stopped

- **Saved:** 2026-10-15, at the user's request. The user continues in a later session.
- **Branch:** `knowledge-base`. `main` is unchanged since the spec and plan commits. Merging is the user's decision at the end of the project.

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
| 1: LLD | In progress: Batch 1 and Batch 2 drafted, reviews partly done |
| 2 to 11 | Not started |

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
| `fundamentals/09-concurrency-in-lld.md` | Yes (drafter finished after the WIP commit) | Not started |
| `fundamentals/10-architectural-patterns.md` | Yes (same) | Not started |
| `fundamentals/11-anti-patterns.md` | Yes | Done |
| `how-to-approach-lld.md` | Yes | Done (finished after the save) |
| `machine-coding-round.md` | Yes | Done (finished after the save) |

Any agent that was running at save time may have stopped partway. In the next session, compare each such file with the WIP commit (`git diff`). Then run a fresh Opus review of each file whose review did not finish.

## Case-Study Progress (session 2, 2026-10-15)

| Assignment | Folders | Drafted | Opus review |
|---|---|---|---|
| C1 | 01-parking-lot, 02-lru-and-lfu-cache | Yes | Done, committed |
| C2 | 03-rate-limiter, 04-elevator-system | Yes | Running |
| C3 | 05-movie-ticket-booking, 06-splitwise | Yes | Running |
| C4 | 07-vending-machine, 08-logging-framework | Yes | Done, committed |
| C5 | 09-tic-tac-toe, 10-snake-and-ladder | Yes | Done, committed |
| C6 | 11-chess, 12-atm | Running | Not started |
| C7 | 13-library-management, 14-in-memory-file-system | Yes | Running |
| C8 | 15-pub-sub-message-queue, 16-task-scheduler | Running | Not started |
| C9 | 17-ride-sharing, 18-shopping-cart-and-orders | Running | Not started |
| C10 | 19-key-value-store-with-ttl, 20-hotel-booking | Running | Not started |
| C11 | 21-food-delivery, 22-online-auction | Running | Not started |
| C12 | 23-digital-wallet, 24-notification-service | Not started | Not started |

Fundamentals 09 and 10: Opus review running. Mermaid check passed for C1 to C5.
Case-study files are not committed until their review finishes. If the session stops, run `git status` to see the drafted folders, then review the ones whose review did not finish.

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
