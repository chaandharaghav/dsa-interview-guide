---
priority: P1
---
# HLD Practice and Mock Interviews

This page gives you design prompts without a solution, sets of timed mock rounds, and a rubric to score your own design. Read the fundamentals and do the P1 case studies first. Then use this page to practice on prompts that you have not seen.

## How to Practice

1. Set a timer for 45 minutes.
2. Read only the prompt. Do not look at notes.
3. Follow the [procedure](how-to-approach-hld.md) aloud, as if an interviewer listens. Write the requirements and your assumed answers first.
4. Do the estimate on paper. Write the decision that each number drives.
5. Draw the diagram on paper or in a Mermaid block. Walk one request through it.
6. Pick two or three deep dives. For each one, state the simple design, where it fails, the improvement, and the failure behavior.
7. When the timer ends, stop. Score your work with the rubric at the end of this page.
8. Compare your design with the related case studies (named in the table below), and write down one thing to do differently next time.

Recommendation: do one prompt every two days. Do a full mock set once a week in the last month before interviews. Practice with a partner when you can: a partner asks follow-up questions that you do not ask yourself.

## Practice Prompts

Each prompt names the case studies that use similar ideas. Read them only after your own attempt.

| # | Prompt | What the interviewer wants to see | Related case studies |
|---|---|---|---|
| 1 | **Pastebin.** Users paste text, get a short link, and set an expiry | Blob storage for content, key generation, expiry cleanup | [URL shortener](case-studies/01-url-shortener.md), [file storage](case-studies/10-file-storage-and-sync.md) |
| 2 | **Instagram Stories.** Posts that disappear after 24 hours, a viewer list, a ring of unseen stories | TTL data, fan-out of a small feed, view tracking at scale | [Photo sharing](case-studies/18-photo-sharing.md), [news feed](case-studies/05-news-feed.md) |
| 3 | **Twitter trending topics.** Top hashtags per region over the last hour | Streaming top-k, windows, approximate counting | [Leaderboard and top-k](case-studies/16-leaderboard-and-top-k.md), [ad click aggregation](case-studies/21-ad-click-aggregation.md) |
| 4 | **Food delivery platform.** Restaurants, orders, courier assignment, live tracking | Order state machine, geospatial matching, location updates | [Ride-hailing](case-studies/11-ride-hailing-and-proximity.md), [notification system](case-studies/04-notification-system.md) |
| 5 | **Hotel reservation system.** Search by city and dates, book a room type, cancel | Inventory per date, overbooking rules, idempotent booking | [Ticket booking](case-studies/12-ticket-booking.md), [payment system](case-studies/13-payment-system.md) |
| 6 | **Google Docs comments.** Comment threads on a range of text that moves while others edit | Anchors on changing text, real-time updates, permissions | [Collaborative editor](case-studies/19-collaborative-editor.md), [chat system](case-studies/06-chat-system.md) |
| 7 | **Distributed counter for likes.** Billions of likes, a count that users see, no double likes | Sharded counters, deduplication, eventual consistency | [Key-value store](case-studies/03-key-value-store.md), [distributed cache](case-studies/15-distributed-cache.md) |
| 8 | **Online multiplayer game lobby.** Matchmaking by skill and region, game session servers | Matchmaking queues, session placement, real-time connections | [Chat system](case-studies/06-chat-system.md), [job scheduler](case-studies/20-distributed-job-scheduler.md) |
| 9 | **Log aggregation service.** Collect logs from 100,000 servers, search the last 7 days | Ingest pipeline, buffering, inverted index, retention tiers | [Metrics and monitoring](case-studies/17-metrics-and-monitoring.md), [web crawler](case-studies/08-web-crawler.md) |
| 10 | **API gateway.** Authentication, routing, rate limits, and quotas for many backend services | Request path latency, rules distribution, shared rate limits | [Distributed rate limiter](case-studies/02-distributed-rate-limiter.md) |
| 11 | **Music streaming service.** Catalog, playback, playlists, offline downloads | CDN for audio, licensing by region, playlist storage | [Video streaming](case-studies/09-video-streaming.md) |
| 12 | **Coupon and promotion engine.** Codes with usage limits per user and in total, at checkout | Contended counters, idempotent redemption, rules evaluation | [E-commerce flash sale](case-studies/24-ecommerce-flash-sale.md), [payment system](case-studies/13-payment-system.md) |
| 13 | **Calendar service.** Events, invitations, free-busy lookup, reminders | Recurrence storage, time zones, scheduled reminders | [Job scheduler](case-studies/20-distributed-job-scheduler.md), [notification system](case-studies/04-notification-system.md) |
| 14 | **Feature flag service.** Flags with rules per user segment, read by thousands of services | Read-heavy configuration, push of changes, safe defaults when the service is down | [Distributed cache](case-studies/15-distributed-cache.md) |
| 15 | **Web analytics (Google Analytics-like).** Page-view events from many sites, dashboards by hour and day | High-volume ingest, rollups, late events, distinct users | [Ad click aggregation](case-studies/21-ad-click-aggregation.md), [metrics and monitoring](case-studies/17-metrics-and-monitoring.md) |
| 16 | **Price alert service.** Users set a price threshold on a stock or product and get an alert | Matching many rules against a price stream, fan-out of alerts | [Stock exchange](case-studies/22-stock-exchange.md), [notification system](case-studies/04-notification-system.md) |
| 17 | **Nearby friends.** Show friends within 5 km, updated as people move | Location updates, geospatial index, pub-sub per user | [Ride-hailing](case-studies/11-ride-hailing-and-proximity.md), [chat system](case-studies/06-chat-system.md) |
| 18 | **Digital wallet.** Balances, transfers between users, top-ups from a bank | Double-entry ledger, idempotency, reconciliation | [Payment system](case-studies/13-payment-system.md) |
| 19 | **Image processing service.** Users upload images and receive resized and filtered versions | Upload path, job queue, worker scaling, result storage | [Photo sharing](case-studies/18-photo-sharing.md), [online code judge](case-studies/25-online-code-judge.md) |
| 20 | **Q&A site (Stack Overflow-like).** Questions, answers, votes, reputation, search | Relational model, vote counting, search index, caching hot pages | [Search autocomplete](case-studies/07-search-autocomplete.md), [news feed](case-studies/05-news-feed.md) |

## Mock Interview Sets

Each set is one practice session of two rounds. Do the rounds on different days if the session is too long.

| Set | Round 1 (45 minutes) | Round 2 (45 minutes) | Focus |
|---|---|---|---|
| A | URL shortener (case study 01) | Pastebin (prompt 1) | Estimation, key generation, caching |
| B | News feed (case study 05) | Instagram Stories (prompt 2) | Fan-out and feed storage |
| C | Chat system (case study 06) | Nearby friends (prompt 17) | Real-time connections and presence |
| D | Ticket booking (case study 12) | Hotel reservation system (prompt 5) | Contention and correctness |
| E | Leaderboard and top-k (case study 16) | Twitter trending topics (prompt 3) | Counting at scale |
| F | Payment system (case study 13) | Digital wallet (prompt 18) | Ledgers, idempotency, reconciliation |
| G | Two unseen prompts: API gateway (prompt 10), then web analytics (prompt 15) | — | Unseen problems under time pressure |

For sets A to F, the first round is a case study that you have read. Rebuild it from memory in 45 minutes. The second round is a prompt that you have not seen.

## Self-Evaluation Rubric

Score each line from 0 to 2. A total of 16 or more out of 20 is a strong round. This rubric is a model of this KB, not a published company rubric.

| Area | 0 | 1 | 2 |
|---|---|---|---|
| Requirements | Started to draw at once | Asked some questions | Wrote functional and non-functional requirements with numbers, and stated the scope |
| Estimation | No numbers | Numbers with no decision | Peak QPS, storage, and bandwidth, each tied to a design decision |
| API | None | Endpoints without errors or idempotency | Operations with inputs, outputs, errors, pagination, and idempotency where writes retry |
| Data model | No model | Tables or keys without access patterns | Keys chosen from access patterns, the store chosen with a reason |
| High-level design | Missing core components | Components without a request walk-through | Complete diagram, one request walked through it |
| Deep dives | None | One shallow deep dive | Two or more deep dives with the simple design, its limit, and the improvement |
| Scaling | Not discussed | Named a fix without the bottleneck | Found the bottleneck from the numbers and fixed it (cache, replicas, partitions) |
| Failures | Not discussed | Mentioned redundancy | Described what happens when each major component fails, and how the system recovers |
| Trade-offs | None | Named choices | Each major choice with its alternative and the criterion that decided it |
| Communication | Silent | Explained after drawing | Drove the round, checked time, explained each decision |

## After Each Session

Write three lines in a practice log:

1. The score from the rubric.
2. The area with the lowest score.
3. The page in this subject that covers that area. Read it before the next session. The [cheatsheet](cheatsheet.md) maps each problem to its page.
