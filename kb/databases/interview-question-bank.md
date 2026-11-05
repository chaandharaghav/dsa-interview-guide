---
priority: P1
---
# Databases Interview Question Bank

This page collects the questions that interviewers ask most often about the database pages of this knowledge base. The questions follow the page order: relational model and SQL, schema design, indexing, transactions, query tuning, storage engines, NoSQL, Redis, scaling, OLTP and OLAP, and the SQL problems. The first pages have more questions, because interviews test them most. Use the page in two steps. First, read the question and answer it aloud, as you would in the interview, and say the reason as well as the result. Second, open the block and compare your answer with the model answer. Each model answer is short. It gives the direct answer, then the reason, then the trade-off or the edge case, and it ends with a link to the section that explains the topic in depth. If your answer missed a point, read that section before you move on. Where an answer depends on the product (PostgreSQL, MySQL InnoDB, Oracle, Redis), the answer names the product, because a correct answer for one product is often wrong for another.

## Contents

- [Relational Model and SQL](#relational-model-and-sql)
- [Schema Design and Normalization](#schema-design-and-normalization)
- [Indexing](#indexing)
- [Transactions and Isolation](#transactions-and-isolation)
- [Query Execution and Tuning](#query-execution-and-tuning)
- [Storage Engines](#storage-engines)
- [NoSQL Families](#nosql-families)
- [Redis](#redis)
- [Scaling a Database](#scaling-a-database)
- [OLTP, OLAP, and Warehousing](#oltp-olap-and-warehousing)
- [SQL Interview Problems](#sql-interview-problems)

## Relational Model and SQL

??? question "What is the logical order of evaluation of a SQL query, and why can `WHERE` not use a `SELECT` alias?"

    The order is `FROM` and joins, `WHERE`, `GROUP BY`, `HAVING`, window functions, `SELECT`, `DISTINCT`, set operations, `ORDER BY`, and `LIMIT`. `WHERE` runs before `SELECT`, so the alias does not exist yet. For the same reason `WHERE` cannot hold an aggregate (use `HAVING`) or a window function (wrap the query in a CTE or a derived table). The order is logical: the optimizer may run the steps differently if the result stays equal. Products differ on aliases (SQLite accepts one in `WHERE`), so portable code repeats the expression. See [Relational Model and SQL](01-relational-model-and-sql.md#the-logical-order-of-query-processing).

??? question "Explain `NULL` and three-valued logic. Why does `NOT IN` sometimes return no rows?"

    A comparison with `NULL` returns UNKNOWN, not FALSE, and `WHERE` keeps a row only when the predicate is TRUE. `x NOT IN (a, b, NULL)` expands to `x <> a AND x <> b AND x <> NULL`, and the last term is UNKNOWN, so no row passes. The fix is `NOT EXISTS`, or a `WHERE col IS NOT NULL` filter inside the subquery. Also remember that `GROUP BY` and `DISTINCT` treat all `NULL` values as one group, and a `CHECK` constraint passes a `NULL` while `WHERE` rejects it. See [Relational Model and SQL](01-relational-model-and-sql.md#null-and-three-valued-logic).

??? question "What is the difference between `WHERE` and `HAVING`?"

    `WHERE` filters rows before grouping. `HAVING` filters groups after aggregation, so it can use aggregate functions. A condition on a plain column belongs in `WHERE`, because it reduces the input of `GROUP BY` and lets the database use an index. Both keep a row or group only when the predicate is TRUE. See [Relational Model and SQL](01-relational-model-and-sql.md#grouping-and-aggregates).

??? question "What is the difference between `COUNT(*)`, `COUNT(col)`, and `COUNT(DISTINCT col)`?"

    `COUNT(*)` counts rows. `COUNT(col)` counts rows where `col` is not `NULL`. `COUNT(DISTINCT col)` counts the distinct non-null values. After a `LEFT JOIN`, `COUNT(*)` counts the null-extended row of an unmatched left row and returns 1, while `COUNT(right.id)` returns 0. On empty input without `GROUP BY`, `COUNT` returns 0 and `SUM` returns `NULL`. See [Relational Model and SQL](01-relational-model-and-sql.md#grouping-and-aggregates).

??? question "Does a condition in `ON` give the same result as the same condition in `WHERE`?"

    For an inner join, yes. For an outer join, no. The `ON` condition decides which rows match, and then the database adds the unmatched rows of the preserved side with `NULL` columns. `WHERE` runs after the join, so a condition on a column of the optional side is UNKNOWN for the null-extended rows and removes them, and the outer join behaves like an inner join. The rule: filters on the optional side go in `ON`, and filters on the preserved side go in `WHERE`. See [Relational Model and SQL](01-relational-model-and-sql.md#joins).

??? question "How do you find the rows of table A that have no match in table B? Compare the three forms."

    The forms are `NOT EXISTS`, `LEFT JOIN ... WHERE b.key IS NULL`, and `NOT IN`. `NOT EXISTS` is the safest: it handles `NULL` correctly and states the intent. The `LEFT JOIN` form gives the same rows if the tested column is never `NULL` in a real match, so test the primary key of B. `NOT IN` returns no rows if the subquery returns any `NULL`, so use it only for constants or a `NOT NULL` column, and it does not always get the anti-join plan. See [Relational Model and SQL](01-relational-model-and-sql.md#joins).

??? question "What is the difference between `ROW_NUMBER`, `RANK`, and `DENSE_RANK`? How do you get the top 2 earners in each department?"

    All three number the rows of a partition by the window order. `ROW_NUMBER` gives each row a unique number and cuts ties at an arbitrary row, `RANK` gives ties the same number and skips the next numbers (1, 2, 2, 4), and `DENSE_RANK` gives ties the same number and does not skip (1, 2, 2, 3). A window function cannot be in `WHERE`, so compute `DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC)` in a CTE and filter `r <= 2` outside. State your tie rule in the interview. See [Relational Model and SQL](01-relational-model-and-sql.md#window-functions).

??? question "What is the default window frame, and what goes wrong with `LAST_VALUE` or a running total with ties?"

    If the window has an `ORDER BY` and no frame clause, the frame is `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`, which includes the peers of the current row. So `LAST_VALUE` returns the current row's value (or the last peer's), not the last value of the partition, and a running total over a sort key with ties gives all peers the same total. The fix is an explicit `ROWS` frame: `UNBOUNDED FOLLOWING` for `LAST_VALUE`, and `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` for a row-by-row total. Without `ORDER BY`, the frame is the whole partition. See [Relational Model and SQL](01-relational-model-and-sql.md#window-functions).

??? question "How do you write an upsert, and why is it better than SELECT followed by INSERT or UPDATE?"

    PostgreSQL and SQLite use `INSERT ... ON CONFLICT (key) DO UPDATE` (or `DO NOTHING`). MySQL uses `INSERT ... ON DUPLICATE KEY UPDATE`. The standard has `MERGE`, but PostgreSQL does not promise an atomic insert-or-update for it, so use `ON CONFLICT` for concurrent upserts. The single statement checks the unique key and writes atomically, while a `SELECT` followed by an `INSERT` lets two sessions both see "no row". A unique constraint on the key is required in every form. See [Relational Model and SQL](01-relational-model-and-sql.md#dml-insert-update-delete-upsert-returning).

??? question "What does a recursive CTE do? How do you stop it from running forever?"

    A recursive CTE has a seed query and a recursive term joined by `UNION ALL`: the database runs the seed, then runs the recursive term on the previous round's rows until a round returns no rows. It fits hierarchies such as an org chart or a category tree. A cycle in the data makes it infinite, so stop it with a depth limit, a path column that is checked for repeats, or the PostgreSQL 14 `CYCLE` clause. MySQL stops with an error after `cte_max_recursion_depth` rounds (default 1000). Sort the final result explicitly. See [Relational Model and SQL](01-relational-model-and-sql.md#ctes-and-recursive-ctes).

## Schema Design and Normalization

??? question "Normalize a table to 3NF. What is your procedure?"

    Start from the functional dependencies (FDs), because normal forms are rules about FDs and the current data cannot show them. Compute the candidate keys with attribute closure. Then fix 1NF (one value in each cell, a list becomes child rows), 2NF (move attributes that depend on part of a composite key), and 3NF (move attributes that depend on another non-key attribute). Check each split: the join of the fragments must return the original rows (lossless), and each FD should stay inside one fragment (dependency preserving). Finish by saying where you would denormalize a hot read. See [Schema Design and Normalization](02-schema-design-and-normalization.md#normal-forms).

??? question "What is the difference between 3NF and BCNF? Give an example where a table is in 3NF and not in BCNF."

    3NF allows an FD `X -> A` if `X` is a superkey or `A` is a prime attribute, and BCNF allows it only if `X` is a superkey. They differ when a non-superkey determines a prime attribute, which needs overlapping composite candidate keys. Example: `Enrollment(student, course, instructor)` with `student, course -> instructor` and `instructor -> course` is in 3NF and not in BCNF. The BCNF split is lossless but loses `student, course -> instructor`. A dependency-preserving BCNF decomposition does not always exist, while a lossless and dependency-preserving 3NF one always does. See [Schema Design and Normalization](02-schema-design-and-normalization.md#bcnf-when-3nf-is-not-enough).

??? question "Natural key or surrogate key for the primary key?"

    Use a surrogate key as the primary key and keep a `UNIQUE` constraint on the natural key. Natural keys change (an email address) and are often long, and every foreign key and index that references them copies them. A surrogate key is small and stable, and the unique constraint keeps the business rule. The costs are an extra index and an extra join to read the meaning of a reference. A junction table uses its two foreign keys as a composite primary key. See [Schema Design and Normalization](02-schema-design-and-normalization.md#keys).

??? question "When do you denormalize, and how do you keep the copy correct?"

    Denormalize after you measure: normalize first, find a read that is too slow, try the index and the query first, and denormalize only if that is not enough. Usual forms are a summary column, a duplicated column, a materialized view, and a counter. The cost is write amplification: if one code path misses the copy, it is wrong with no error. So keep one normalized source of truth, update the copy in the same transaction, and run a monitor query and a rebuild job. See [Schema Design and Normalization](02-schema-design-and-normalization.md#denormalization).

??? question "Why should the database enforce constraints when the application already validates?"

    The database is the one component that every writer passes through. A second service, a batch job, a manual fix, or a later bug bypasses application checks. A constraint also settles races: two requests that both pass an application check "email does not exist" both insert, and only a `UNIQUE` constraint makes one fail. Validate in the application for clear messages, and keep `NOT NULL`, `CHECK`, `UNIQUE`, and foreign keys as the last line. Rules that depend on data outside the database are the exception. See [Schema Design and Normalization](02-schema-design-and-normalization.md#constraints-the-database-enforces-the-invariants).

??? question "How do you model a many-to-many relationship? What does the primary key of the junction table look like?"

    Use a junction table with a foreign key to each side. The two foreign keys form the composite primary key, which stops duplicate pairs and gives an index for lookups from the first column. Add an index on the second column for lookups from the other side. Attributes of the relationship (quantity, price at purchase) live in the junction table. If the row gets its own children, or the pair can repeat over time, use a surrogate key instead. See [Schema Design and Normalization](02-schema-design-and-normalization.md#relationships).

??? question "How do you store money and time stamps?"

    Money is an exact type: `NUMERIC(p, s)` or an integer of minor units (cents), with a currency code in a separate column. Floating-point types hold approximations (`0.1 + 0.2` does not equal `0.3`), so in Java use `BigDecimal` or a `long`, not `double`. An instant is a UTC time stamp: in PostgreSQL use `timestamptz`, because `timestamp without time zone` applies no conversion, and MySQL `TIMESTAMP` stops at 2038-01-19 03:14:07 UTC. A future local event needs the local date and time plus the IANA zone name. See [Schema Design and Normalization](02-schema-design-and-normalization.md#data-types).

??? question "Users delete accounts and later register with the same email. The table has `UNIQUE (email)` and a soft delete. What goes wrong?"

    The unique constraint counts soft-deleted rows, so the second registration fails although the first user is gone. The fix is a partial unique index over live rows only, `... ON app_account (email) WHERE deleted_at IS NULL`, in PostgreSQL and SQLite. MySQL 8.4 has no `WHERE` in `CREATE INDEX`, but a unique index on an expression that is `NULL` for deleted rows works, because a unique index permits many `NULL` values. Soft delete also adds the `deleted_at IS NULL` filter to every query, and an archive table is the alternative. See [Schema Design and Normalization](02-schema-design-and-normalization.md#soft-delete-and-unique-constraints).

??? question "How do you add a `NOT NULL` column with a default to a table with 500 million rows, with no downtime?"

    First check what the engine does. In PostgreSQL, `ADD COLUMN` with a non-volatile default stores the default in the catalog and does not rewrite the table. In MySQL 8.4, `ADD COLUMN` uses `ALGORITHM=INSTANT` by default, with limits. If the column needs backfilled values, use expand and contract: add a nullable column, write both old and new, backfill in batches by primary key range, add `CHECK (c IS NOT NULL) NOT VALID`, run `VALIDATE CONSTRAINT`, and then `SET NOT NULL`. Set `lock_timeout` so the `ALTER` fails fast instead of queuing behind a long transaction. See [Schema Design and Normalization](02-schema-design-and-normalization.md#schema-evolution).

## Indexing

??? question "Why does a database use a B+ tree for an index and not a binary tree or a hash table?"

    A disk lookup costs page reads, not comparisons. A B+ tree fills each node with hundreds of keys, so one billion keys need four levels (an assumed fanout of 400), and the top levels stay in memory. A binary tree needs about 27 node visits for 10^8 keys. The B+ tree keeps keys sorted and links the leaves, so one structure serves equality, ranges, `ORDER BY`, and prefix matches, while a hash index serves equality only. The cost is a page write for each index on each insert, and page splits. See [Indexing](03-indexing.md#the-structure-of-a-b-tree-index).

??? question "What is the difference between a clustered index and a secondary index? How does InnoDB differ from PostgreSQL?"

    A clustered index stores the table rows in its leaf pages in key order. In InnoDB the table is the primary key B+ tree, and a secondary index entry holds the primary key, so a secondary lookup searches two trees. PostgreSQL stores a heap, and every index (including the primary key) is a separate B+ tree whose entries hold a TID (heap page and slot). Consequences: in InnoDB a long or random primary key enlarges every secondary index. In PostgreSQL an update that creates a new row version needs new entries in every index, unless it is a HOT update. See [Indexing](03-indexing.md#clustered-index-against-heap-table).

??? question "A table has an index on `(a, b, c)`. Which queries can use it: `WHERE a = 1`, `WHERE b = 2`, `WHERE a = 1 AND c = 3`, `WHERE a = 1 AND b > 2 AND c = 3`?"

    The index is sorted by `a`, then `b`, then `c`. `a = 1` narrows to its range. `b = 2` has no condition on `a`, so the index cannot narrow the scan unless the engine has a skip scan: PostgreSQL 18, MySQL (8.0.13 and later, with narrow conditions), and SQLite (after `ANALYZE`) can skip over an `a` that has few distinct values. `a = 1 AND c = 3` narrows by `a` and checks `c` on each entry in the range, and `a = 1 AND b > 2 AND c = 3` narrows by `a` and `b` and checks `c` inside that range. So put equality columns first and the range column after them, then check the real plan. See [Indexing](03-indexing.md#composite-indexes-and-the-leftmost-prefix-rule).

??? question "What is a covering index? What is an index-only scan, and why does PostgreSQL still visit the heap sometimes?"

    A covering index contains every column that a query reads, so the database answers from the index and skips the row fetch. In InnoDB a secondary index holds the primary key too, so it covers a query on those columns. PostgreSQL index entries carry no visibility data, so the scan checks the visibility map: a set bit for a heap page means that all its rows are visible, and the scan skips the heap. `VACUUM` sets the bits, so a table with many recent updates shows high `Heap Fetches`. `INCLUDE` adds payload columns without making them part of the key. See [Indexing](03-indexing.md#covering-indexes-and-index-only-scans).

??? question "You add an index and the query is still slow. The plan shows a sequential scan. List the reasons, and how you find the real one."

    Usual reasons: a function or expression on the indexed column, a leading wildcard in `LIKE`, an implicit type conversion, a predicate that is not selective (a sequential scan is correct then), a missing leftmost column, `OR` with no index on one side, a small table, stale statistics, or a bad estimate for correlated columns. To find the real one, run `EXPLAIN (ANALYZE, BUFFERS)` with real parameter values, compare estimated and actual rows, and check `pg_stats`. Test with `SET enable_seqscan = off` in a session: if the index plan is slower, the planner was right. See [Indexing](03-indexing.md#when-the-database-does-not-use-an-index).

??? question "Why can adding indexes slow a system down? How many indexes are too many?"

    Every index is a B+ tree that the database updates on each insert, and on each update or delete that changes an indexed column. Six indexes mean seven structure updates for each insert, plus more log, buffer pool use, and replica replay. In PostgreSQL, an index on a frequently updated column removes the chance of HOT updates. There is no fixed number: the test is whether a real query uses the index and the read benefit beats the write cost. Find unused indexes with `pg_stat_user_indexes` (`idx_scan` of zero) or `sys.schema_unused_indexes` in MySQL, and drop duplicates. See [Indexing](03-indexing.md#the-write-cost-of-an-index).

??? question "Why is a random UUID primary key a problem for a B+ tree table, and what do you do about it?"

    A sequential key inserts at the rightmost leaf, which is in memory and fills before it splits. A random key (UUIDv4) inserts into a random leaf, so an index larger than memory reads a leaf from disk for most inserts, splits happen all over the tree, and pages end partly full. In InnoDB the 16-byte key also enlarges every secondary index, and in PostgreSQL random inserts write more full-page images to the WAL. Use a time-ordered ID (UUIDv7, generated by `uuidv7()` in PostgreSQL 18) or a `bigint` key with the random ID in a separate unique column. UUIDv7 leaks the creation time. See [Indexing](03-indexing.md#random-keys-against-ordered-keys).

??? question "How do you add an index to a table with 500 million rows in production without an outage?"

    In PostgreSQL use `CREATE INDEX CONCURRENTLY`, because a normal `CREATE INDEX` blocks writes. The concurrent build takes no write-blocking lock, but it scans the table twice, waits for older transactions, and cannot run inside a transaction. If it fails, it leaves an `INVALID` index, so check `pg_index.indisvalid`, drop it, and retry. In MySQL InnoDB, `ALTER TABLE ... ADD INDEX` runs in place, and stating `ALGORITHM=INPLACE, LOCK=NONE` makes it fail at once if the server cannot honor it, but a metadata lock can queue behind a long transaction, so check for those first. After the build, run `ANALYZE` and verify the plan. See [Indexing](03-indexing.md#building-an-index-on-a-live-table).

??? question "What is selectivity, and how does the planner know whether to use an index?"

    Selectivity is the fraction of rows that a predicate selects. The planner multiplies it by the row count to estimate the cardinality of each plan node, using statistics from `ANALYZE`: the null fraction, distinct values, most common values, and a histogram. It then prices each plan with cost constants (`seq_page_cost` 1.0 and `random_page_cost` 4.0 by default in PostgreSQL). A selective predicate favors the index, and a predicate that returns a large share favors the scan. Wrong estimates give wrong plans: fix them with `ANALYZE`, a higher statistics target, or `CREATE STATISTICS`. See [Indexing](03-indexing.md#selectivity-cardinality-and-statistics).

## Transactions and Isolation

??? question "What are the four SQL isolation levels, and which anomaly does each one forbid?"

    `READ UNCOMMITTED` forbids nothing beyond dirty writes, `READ COMMITTED` also forbids dirty reads, `REPEATABLE READ` also forbids non-repeatable reads, and `SERIALIZABLE` also forbids phantoms and makes the result equal to some serial order. The standard defines the levels by three phenomena only, and Berenson et al. (1995) show that this omits lost updates, read skew, and write skew. The names are a floor and not a contract, so name the product in your answer. See [Transactions and Isolation](04-transactions-and-isolation.md#the-sql-standard-levels).

??? question "What isolation level does each of PostgreSQL, MySQL InnoDB, and Oracle use by default, and what does that level still allow?"

    PostgreSQL defaults to `READ COMMITTED`: each statement takes a fresh snapshot, so non-repeatable reads, phantoms, read skew, lost updates for read-then-write code, and write skew remain. InnoDB defaults to `REPEATABLE READ`: plain `SELECT` statements read one snapshot, but write skew and lost updates remain for code that reads and then writes, because `UPDATE` works on the newest committed row. Oracle defaults to `READ COMMITTED`, and its `SERIALIZABLE` is a snapshot level that raises `ORA-08177` on a conflicting write. The same `@Transactional` code can therefore behave differently on PostgreSQL and MySQL. See [Transactions and Isolation](04-transactions-and-isolation.md#what-real-products-do).

??? question "How does PostgreSQL `REPEATABLE READ` differ from InnoDB `REPEATABLE READ`? Does either allow phantoms?"

    PostgreSQL `REPEATABLE READ` is snapshot isolation: the transaction sees one fixed snapshot, the manual says it does not allow phantom reads, and an update of a row changed after the snapshot aborts with `40001`. InnoDB uses the snapshot for plain reads only, while `UPDATE`, `DELETE`, and locking reads work on the newest committed row and take next-key locks, which block phantoms for those statements. So a lost update that PostgreSQL turns into an error is silent on InnoDB. At `READ COMMITTED`, InnoDB disables gap locks, so phantoms can occur. See [Transactions and Isolation](04-transactions-and-isolation.md#what-real-products-do).

??? question "Explain write skew with an example. Which levels allow it, and how do you prevent it?"

    Two transactions read overlapping data, and each writes a different row. Each write is valid alone, and together they break an invariant. Example: two on-call doctors each check that two doctors are on call, and each sets their own row to off call, which leaves zero. It occurs at `READ COMMITTED`, at PostgreSQL `REPEATABLE READ`, at InnoDB `REPEATABLE READ` with plain reads, and at Oracle `SERIALIZABLE`. Prevent it with `SERIALIZABLE` and a retry on `40001`, with `SELECT ... FOR UPDATE` on all rows that the decision reads, with a shared row that both writes touch, or with a constraint. See [Transactions and Isolation](04-transactions-and-isolation.md#write-skew-a5b).

??? question "How does MVCC work? What is a snapshot, and why do readers not block writers?"

    A write creates a new version of the row and keeps the old one. Each version records the transaction that created it and the one that deleted it (`xmin` and `xmax` in PostgreSQL). A snapshot says which transactions were committed, and a transaction sees a version if its creator is visible and its deleter is not. Readers read an old version while writers create a new one, so they touch different data, but two writers of one row still conflict. The cost is that old versions stay while a snapshot may need them: PostgreSQL cleans up with VACUUM and InnoDB with purge, and a long transaction delays both. See [Transactions and Isolation](04-transactions-and-isolation.md#multi-version-concurrency-control).

??? question "Two users try to book the last seat at the same time. How do you make the database prevent a double booking?"

    Let the database arbitrate. The best tools, in order: a unique constraint on (show, seat), a conditional update with a row count check, and then a row lock. For example `UPDATE ... SET status = 'HELD' WHERE ... AND status = 'FREE'` and a check that one row changed: the second statement waits for the row lock, re-reads the row, and changes 0 rows. At PostgreSQL `REPEATABLE READ` or `SERIALIZABLE`, the second statement fails with `40001` instead, and the retry sees `HELD`. Do not read the status, check it in Java, and write without a lock or a condition, because that is a check-then-act race. See [Transactions and Isolation](04-transactions-and-isolation.md#patterns-that-make-writes-correct).

??? question "What is the difference between optimistic and pessimistic locking? When do you choose each?"

    Pessimistic locking locks first: `SELECT ... FOR UPDATE` takes an exclusive row lock, and other writers wait. It is correct without retries, and it costs waits, deadlock risk, and a lock held for the whole transaction. Optimistic locking holds no lock: the row has a `version`, the write is `UPDATE ... WHERE id = ? AND version = ?`, and a row count of 0 means a conflict, so the application re-reads and retries (JPA does this with `@Version`). Choose optimistic when conflicts are rare or a user thinks between read and write, and pessimistic when conflicts are frequent on a few rows. The retry must run the whole transaction again. See [Transactions and Isolation](04-transactions-and-isolation.md#optimistic-locking).

??? question "How do deadlocks occur, how does the engine handle them, and how do you prevent them?"

    Two transactions each hold a row lock and wait for the other's lock, for example two transfers that update accounts 1 and 2 in opposite order. The engine finds the cycle in a wait-for graph and aborts one transaction: PostgreSQL checks after `deadlock_timeout` (1 second by default) and returns `40P01`, and InnoDB detects at once and rolls back the transaction that changed the fewest rows. The application must catch the error and retry the whole transaction. Prevent deadlocks with one global lock order, short transactions, and indexes on the columns of locking statements, because a lower isolation level does not remove them. See [Transactions and Isolation](04-transactions-and-isolation.md#deadlocks).

??? question "What does `SELECT ... FOR UPDATE SKIP LOCKED` do, and how would you build a job queue with it?"

    `FOR UPDATE` locks the rows that the query returns, and `SKIP LOCKED` leaves out rows that another transaction has locked, so several workers run the same query and each gets different rows. A worker selects pending jobs with `LIMIT`, processes them, updates their status, and commits, and a crashed worker's transaction rolls back so the rows become pending again. The result is an inconsistent view by design, so use it only for queue-like tables. For slow jobs, claim rows with `UPDATE ... RETURNING` and a `RUNNING` status, commit, and process outside the transaction. Delivery is at least once, so jobs must be idempotent. See [Transactions and Isolation](04-transactions-and-isolation.md#patterns-that-make-writes-correct).

??? question "How does PostgreSQL implement SERIALIZABLE, and what must the application do?"

    PostgreSQL uses Serializable Snapshot Isolation: transactions run on snapshots, and PostgreSQL also records what they read in predicate locks (`SIReadLock`) that never block. If the read-write dependencies among concurrent transactions can produce a result that no serial order gives, it aborts one transaction with SQLSTATE `40001`. The abort can be a false positive. The application must retry the whole transaction, because PostgreSQL gives no automatic retry. Keep transactions short, and use `SERIALIZABLE READ ONLY DEFERRABLE` for long reports. See [Transactions and Isolation](04-transactions-and-isolation.md#what-real-products-do).

## Query Execution and Tuning

??? question "A query is slow in production. Walk me through what you do."

    Start from data: rank statements by total time with `pg_stat_statements` or the slow query log, get the real parameter values, and run `EXPLAIN (ANALYZE, BUFFERS)`. Read the plan from the innermost node and find the first node where the estimated rows differ from the actual rows by a factor of 10 or more. Form one hypothesis and change one thing (`ANALYZE`, an index, a non-sargable predicate, extended statistics), then measure again with the same data and compare plans. Check side effects, because an index slows every write. If the query is fast but the endpoint is slow, count queries per request, because the cause may be N+1. See [Query Execution and Tuning](05-query-execution-and-tuning.md#a-tuning-workflow).

??? question "How does the query optimizer choose a plan, and why does a wrong estimate give a bad plan?"

    A cost-based optimizer builds candidate plans, estimates the cost of each from statistics and cost constants, and picks the cheapest. The estimate at one step feeds the choice above it: if the planner thinks a filter returns 3 rows, it picks an index nested loop, and if it returns 48,000 rows, that loop runs 48,000 lookups and a hash join would be cheaper. Causes of wrong estimates are stale statistics, correlated columns (the planner multiplies selectivities as if independent), a function on a column, and a skewed value. Fix the estimate rather than forcing the plan. See [Query Execution and Tuning](05-query-execution-and-tuning.md#statistics-and-cardinality-estimates).

??? question "Explain the three join algorithms and when the database picks each."

    A nested loop takes each outer row and looks for matches in the inner input (N lookups with an index on the inner column, N × M comparisons without), so it suits a small outer input or an early `LIMIT`. A hash join builds a hash table on the smaller input and probes it, at about N + M cost, and it needs memory (it spills to disk in batches if the table exceeds `work_mem` × `hash_mem_multiplier`) and an equality condition in PostgreSQL. A merge join sorts both inputs and scans them once, and it is cheap when the inputs are already sorted. MySQL has no merge join and has hash join since 8.0.18, and SQLite has only nested loops. See [Query Execution and Tuning](05-query-execution-and-tuning.md#join-algorithms).

??? question "How do you read `EXPLAIN ANALYZE` output? What do you look at first?"

    Read from the innermost node outward. For each node check the access method (a `Seq Scan` with high `Rows Removed by Filter` suggests an index), estimated rows against actual rows (a gap of 10 times or more shows a statistics problem), the time (`actual time` is per execution, so multiply by `loops`), and `Buffers` (`read` against `hit`). Also look for sorts that spill, hash joins with several batches, and `Heap Fetches`. `EXPLAIN ANALYZE` runs the statement, so wrap a write in a transaction and roll it back. See [Query Execution and Tuning](05-query-execution-and-tuning.md#reading-a-plan).

??? question "What is the N+1 problem? How do you detect it and fix it?"

    The code runs one query to load N parents and then one query for each parent, so it runs 1 + N queries and pays a round trip for each. ORMs cause it through lazy loading inside a loop. The slow query log does not show it, because each query is fast. Detect it by counting queries: assert the statement count in a test, look for a statement with a very high `calls` count in `pg_stat_statements`, or look for many identical database spans in one trace. Fix it with one join, a batch `IN` query, a `GROUP BY` for counts, or in JPA `JOIN FETCH`, an entity graph, or `@BatchSize`. See [Query Execution and Tuning](05-query-execution-and-tuning.md#the-n1-problem).

??? question "Compare offset pagination and keyset pagination. When does each fit?"

    Offset pagination (`LIMIT n OFFSET k`) makes the database produce and discard the first k rows, so deep pages cost more, and pages drift when rows are inserted or deleted. Keyset pagination remembers the sort key of the last row and asks for rows after it, such as `WHERE (created_at, id) > (?, ?)`, so with an index every page costs the same and pages are stable. Keyset needs an index on the sort columns and a unique tie-breaker, and it cannot jump to page 50. Use keyset for infinite scroll and API cursors, and offset for small results with page numbers. See [Query Execution and Tuning](05-query-execution-and-tuning.md#pagination-offset-against-keyset).

??? question "What are prepared statements, and what is the plan-caching pitfall?"

    A prepared statement separates the SQL text from the parameter values, so the database parses once, the values cannot inject SQL, and a plan can be reused. In PostgreSQL the first five executions use custom plans, and then the server compares a generic plan with the average of the custom plans and uses the generic plan if it is not much more expensive. The pitfall is skew: if one value matches 95 percent of rows and another matches 0.1 percent, one generic plan suits only one of them. The symptom is a query that is fast in `psql` and slow from the application. Check the plan with the real value, and set `plan_cache_mode = force_custom_plan` if needed. See [Query Execution and Tuning](05-query-execution-and-tuning.md#prepared-statements-and-plan-caching).

??? question "A query is fast in the development database and slow in production. Why?"

    The data differs, so the plan differs. A table with 1,000 rows makes the planner choose a sequential scan and hides a missing index. Production can have skew (one customer with 2 million rows), stale statistics after a bulk load, a generic plan that suits the development values, and concurrent load such as lock waits, a cold buffer pool, or an exhausted pool. Reproduce with production-sized data or its statistics, the real parameters, and `EXPLAIN (ANALYZE, BUFFERS)`. See [Query Execution and Tuning](05-query-execution-and-tuning.md#a-tuning-workflow).

??? question "`EXISTS` or `IN`? `UNION` or `UNION ALL`? `SELECT *` or a column list?"

    Modern planners often turn `EXISTS` and `IN` into the same semijoin, so read the plan first. The real difference is the negative form: `NOT IN` returns no rows if the subquery returns a `NULL`, so write `NOT EXISTS`. `UNION` removes duplicates with a sort or hash over the whole result, so use `UNION ALL` when the parts cannot overlap. `SELECT *` sends unused columns, prevents an index-only scan, and breaks when a column is added, so name the columns. See [Query Execution and Tuning](05-query-execution-and-tuning.md#query-rewrites).

??? question "Why is a query that the buffer pool serves still slower than a Redis lookup?"

    A buffer-pool hit removes the disk read and not the other work. The database still parses or finds a prepared statement, plans it, runs the plan tree, descends the B+ tree through buffer pages with latches, checks row version visibility against the snapshot, converts the row to the wire format, and sends it over a pooled connection. Redis does a hash lookup and returns bytes. That is why a cache helps for hot keys, but tune the query first, because a cache adds an invalidation problem and a second system. See [Query Execution and Tuning](05-query-execution-and-tuning.md#why-a-buffer-pool-hit-is-still-slower-than-a-cache-lookup).

## Storage Engines

??? question "What is the write-ahead log, and why does a database need it?"

    The write-ahead log (WAL) is an append-only file in which the engine records each change before it writes the changed data page. A commit returns after the engine flushes the log, not after it writes the data pages. A transaction changes several pages in random places, so writing them at commit costs several random writes and a crash leaves a broken state. With the log, a commit is one sequential append and one flush, and recovery replays the log. The flush is the cost of durability, group commit lets many transactions share it, and `synchronous_commit = off` trades the last commits for speed. See [Storage Engines](06-storage-engines.md#the-write-ahead-log).

??? question "What happens when a database crashes and restarts? Explain the recovery steps."

    In the ARIES model, recovery starts at the last checkpoint and runs three passes. Analysis finds the dirty pages and the transactions that had not committed, redo repeats history from the oldest change that may not be on disk (it is idempotent because a record applies only if the page LSN is smaller than the record LSN), and undo rolls back the uncommitted transactions and writes compensation records. PostgreSQL needs effectively only the redo pass, because an uncommitted transaction's row versions stay invisible. InnoDB redoes from the redo log and then rolls back with the undo logs. See [Storage Engines](06-storage-engines.md#crash-recovery-and-aries).

??? question "What is a torn page, and how do PostgreSQL and InnoDB protect against it?"

    A torn page is a page that a crash left half-written, because the disk writes in sectors of 512 bytes or 4 KB and a page is 8 kB or 16 KB. The log alone cannot repair it, because a redo record assumes a known page state. PostgreSQL, with `full_page_writes` (default `on`), writes the whole page into the WAL on its first change after each checkpoint. InnoDB writes pages to doublewrite files first and then to their positions. Both cost extra writes, and you can turn them off only if the storage guarantees atomic page writes. See [Storage Engines](06-storage-engines.md#torn-pages-full-page-writes-and-the-doublewrite-buffer).

??? question "When would you choose an LSM engine, and when a B-tree engine?"

    Choose an LSM engine for a high write rate, append-heavy data, and reads mostly by key: a write is a log append plus a memtable insert, and a flush writes an immutable sorted file (an SSTable) that background compaction merges. Choose a B+ tree engine for mixed reads and writes, range scans, secondary indexes, and mature transactions. LSM costs are read amplification, compaction write amplification, space amplification, and write stalls, and B+ tree costs are random page writes and a whole page written for one row. Say what you measured: write rate, key pattern, and read mix. See [Storage Engines](06-storage-engines.md#b-tree-against-lsm-tree).

??? question "Why does PostgreSQL need VACUUM, and how do InnoDB undo logs differ from PostgreSQL dead tuples?"

    PostgreSQL does not overwrite a row on `UPDATE` or remove it on `DELETE`. It writes a new row version in the table, and the old version becomes a dead tuple when no snapshot can see it. VACUUM reclaims that space, updates the visibility map and statistics, and freezes old rows against transaction ID wraparound. InnoDB updates the row in place and keeps old values in the undo log, which purge threads clean. In both engines a long transaction blocks cleanup: bloat in PostgreSQL, and a growing history list in InnoDB. See [Storage Engines](06-storage-engines.md#the-physical-side-of-mvcc).

## NoSQL Families

??? question "What does NoSQL give up, and what does it give in return?"

    NoSQL is not one design, so answer by family. Across the families a database gives up ad hoc queries and joins, a wide transaction scope (one item, document, or partition), and relational constraints. In return it gives a layout where one query reads one partition, so latency stays predictable as data grows, partitioning that is part of the design, and a flexible schema that moves into the application. The condition is known and narrow access patterns. If queries vary or invariants span rows, stay relational. See [NoSQL Families](07-nosql-families.md#what-nosql-changes).

??? question "Design a DynamoDB table for orders with these queries: get an order by id, list a user's latest orders."

    Start from the queries: the table has partition key `orderId`, and `GetItem` serves the lookup. For the list, add a global secondary index (GSI) with partition key `userId` and sort key `createdAt`, and run a `Query` with `ScanIndexForward = false` and a `Limit`. The trade-off is that a GSI is eventually consistent and costs write capacity, so a user can miss an order just placed. The other design uses partition key `userId` and sort key `createdAt#orderId` in the table plus a GSI on `orderId`, which makes the list strongly consistent. Pick by which query is more frequent. See [NoSQL Families](07-nosql-families.md#key-value-and-dynamodb).

??? question "What is the difference between a GSI and an LSI in DynamoDB?"

    A GSI has its own partition key and sort key, lives in its own partitions, can be added at any time, and supports only eventually consistent reads (20 per table by default). An LSI has the same partition key as the table and a different sort key, must be created with the table, supports strongly consistent reads, and allows 5 per table. The LSI cost is that the item collection for one partition key value cannot exceed 10 GB. Choose an LSI only for a strongly consistent read on a second sort order with bounded data per key. See [NoSQL Families](07-nosql-families.md#key-value-and-dynamodb).

??? question "In MongoDB, when do you embed and when do you reference?"

    Embed data that is read with its parent and stays bounded, such as an order's line items. A single-document write is atomic, so embedding gives atomicity without a transaction. Reference data that has its own life, is shared by many parents, or grows without bound, because a BSON document is limited to 16 MiB. Embedding duplicates shared data, so an update must change every copy. The subset pattern combines both: embed a bounded preview and keep the full list in another collection. See [NoSQL Families](07-nosql-families.md#document-stores-and-mongodb).

??? question "What do write concern `majority` and read concern `majority` guarantee in MongoDB?"

    A write with `w: "majority"` returns after a majority of the data-bearing voting members have it, so a primary failover does not roll it back. `w: 1` returns after the primary applies it, and a failover before replication can lose it. A read with read concern `majority` returns only majority-acknowledged data, while the default `local` gives no such guarantee. An acknowledged majority write says nothing about the other documents in an invariant: only a transaction covers several documents. See [NoSQL Families](07-nosql-families.md#document-stores-and-mongodb).

??? question "Why does Cassandra need one table for each query, and what does that cost?"

    Cassandra answers a query well when it reads one partition: the partition key tells the coordinator which replicas to ask, and the clustering columns order the rows. It has no joins and a fixed sort order, so you list the queries first and build one table for each, and the same fact lives in several tables. The cost is write amplification: the application writes to every table, and an update must change every copy. A new query often needs a new table and a backfill. Accept this for write-heavy loads with known queries and multi-data-center availability. See [NoSQL Families](07-nosql-families.md#wide-column-stores-cassandra-bigtable-and-hbase).

??? question "What is a tombstone in Cassandra, and why does it slow reads?"

    SSTables are immutable, so a delete writes a tombstone, a marker with a time stamp, and an expired TTL also becomes one. A read merges data with the tombstones that cover it, so a partition with many tombstones forces the read to scan them all. A queue built on a table is the classic failure. Cassandra warns at 1,000 tombstones in one read and aborts at 100,000 by default. Compaction drops a tombstone only after `gc_grace_seconds` (ten days by default), so run repair inside that period or deleted data can return. See [NoSQL Families](07-nosql-families.md#wide-column-stores-cassandra-bigtable-and-hbase).

??? question "A Cassandra cluster has RF = 3. What do you choose for the read and write consistency level?"

    If a read must see the latest acknowledged write, you need `W + R > RF`. With RF = 3, `QUORUM` for both (2 + 2 > 3) gives that and survives one node failure. In a multi-data-center cluster use `LOCAL_QUORUM` so a request does not wait for a remote data center. Use `ONE` where a stale read is acceptable, and avoid `ALL`, which fails when any replica is down. A quorum does not make Cassandra linearizable: concurrent writes resolve by time stamp, and compare-and-set needs a lightweight transaction. See [NoSQL Families](07-nosql-families.md#wide-column-stores-cassandra-bigtable-and-hbase).

??? question "How do you design a Bigtable row key for time-series data without creating a hotspot?"

    Bigtable sorts rows by row key, and one server serves a range of rows. A key that starts with a time stamp sends every new write to the last range. Put a high-cardinality value first, for example `deviceId#metric#timestamp`, so writes spread across devices and a prefix scan returns one series in time order. For newest-first order, store a reversed time stamp. A hashed prefix spreads the load even more, and it costs the natural sort order. See [NoSQL Families](07-nosql-families.md#wide-column-stores-cassandra-bigtable-and-hbase).

??? question "A DynamoDB table throttles requests while the table-level throughput is far below the quota. What happened?"

    Throttling is per partition. A partition delivers at most 3,000 read units and 1,000 write units per second, so a hot partition throttles even when the table-level quota is not used. The usual cause is a partition key with low cardinality or one value with most of the traffic. Adaptive capacity moves unused throughput to busy partitions but does not lift the per-partition maximum. Find the hot key with CloudWatch Contributor Insights, then raise the key cardinality, add a write-sharding suffix (readers query all suffixes), cache hot reads, or aggregate before the write. See [NoSQL Families](07-nosql-families.md#key-value-and-dynamodb).

??? question "Do NoSQL databases support transactions?"

    Some do, with a smaller scope than a relational database, so name the scope for each product. DynamoDB `TransactWriteItems` covers up to 100 items in one Region, MongoDB has single-document atomicity and multi-document transactions at a higher cost with a 60-second default limit, Cassandra lightweight transactions cover one partition, and Bigtable and HBase are atomic for one row. Spanner and CockroachDB give multi-row transactions across partitions, at the price of a distributed commit. If the invariant spans more than the scope, change the model so that the data that must change together lives in one unit. See [NoSQL Families](07-nosql-families.md#trade-offs-and-when-to-use).

## Redis

??? question "Redis runs commands on one thread. Why is it fast, and what does that give you?"

    The data is in RAM, each command is short, and one thread avoids locks and context switches. The single thread also makes each command atomic. The cost of a request is mostly the network and system calls, and a pipeline raises throughput because many commands share one read and one write. The edge case is that one slow command blocks all clients: `KEYS`, `SMEMBERS` on a huge set, a long Lua script, and `DEL` of a huge key. Redis uses other threads for socket I/O (`io-threads`, off by default), background `fsync`, and lazy freeing, but the main thread runs the commands. See [Redis](08-redis.md#architecture-one-thread-runs-the-commands).

??? question "What can you lose with RDB, with AOF `everysec`, and with AOF `always`? Which do you use?"

    RDB can lose the writes since the last snapshot (minutes with the default rules), AOF `everysec` (the default) can lose about one second, and AOF `always` fsyncs before it replies, at a large throughput cost. For a cache, use no persistence or RDB only. For data that you cannot rebuild and can lose for a second, use AOF `everysec` with the RDB preamble, plus RDB copies for backup. A single node is one machine, and replication is asynchronous, so a failover can still lose acknowledged writes. See [Redis](08-redis.md#persistence).

??? question "Is a write to Redis lost if the leader fails right after it acknowledges the write? How do you reduce the risk?"

    It can be lost. Redis replication is asynchronous, so the leader acknowledges the client before the replicas have the write, and a promoted replica lacks it; the same holds for Sentinel and Cluster failovers. Mitigations are partial: `WAIT numreplicas timeout` blocks until replicas acknowledge, `WAITAOF` also waits for fsync, and `min-replicas-to-write` makes the leader refuse writes when no replica is close, but none of them makes Redis a CP system. Keep the source of truth in a durable store and make Redis data rebuildable. See [Redis](08-redis.md#replication-sentinel-and-redis-cluster).

??? question "Explain MULTI/EXEC, WATCH, and Lua scripts. Does Redis roll back a transaction?"

    `MULTI`/`EXEC` runs a queued batch with no other client in between, and Redis does not roll back: if a command is wrong at queue time, `EXEC` refuses the whole transaction, and if a command fails at run time, the others still run. `WATCH` adds optimistic locking: `EXEC` aborts (returns null) if a watched key changed, and the client retries. A Lua script runs a read-decide-write routine atomically in one round trip, which `MULTI` cannot do, but it blocks the server while it runs. Redis 8.4 adds `SET ... IFEQ` and `DELEX ... IFEQ` for compare-and-set. See [Redis](08-redis.md#transactions-scripts-and-pipelining).

??? question "How does Redis Cluster decide which node holds a key, and what are the limits for multi-key operations?"

    `CRC16(key) mod 16384` gives one of 16,384 hash slots, and each leader owns a range of slots. A multi-key command, transaction, or script works only if all keys are in one slot, which you force with a hash tag such as `{user42}`. The client keeps a slot map. A node that does not own the slot answers `-MOVED` and the client updates its map, while `-ASK` during a migration means retry once at the target without updating the map. Only database 0 exists, and a tag that puts too many keys in one slot makes a hot slot. See [Redis](08-redis.md#replication-sentinel-and-redis-cluster).

??? question "How does Redis expire keys? Can an expired key still use memory?"

    Redis uses lazy expiry (a client access deletes a timed-out key) and active expiry (a background cycle that examines keys with a TTL). The cycle runs 10 times each second by default, examines batches of 20 keys, and repeats while more than 10% of a batch is expired, with a CPU cap. So an expired key that nobody reads can stay in memory for a time, but reads never return it. Replicas do not expire keys: the leader sends `DEL`. Do not confuse expiry with eviction, which removes live keys at `maxmemory`. See [Redis](08-redis.md#key-expiry).

??? question "Design a leaderboard with Redis. What happens with ties?"

    Use one sorted set for each board: `ZINCRBY` or `ZADD` to update a score, `ZRANGE key 0 99 REV WITHSCORES` for the top 100, and `ZREVRANK` for a rank, each O(log N) or O(log N + M). A sorted set keeps a skip list and a hash table, so the data is always sorted. Equal scores order by member name, so if the product says that the earlier achiever wins, encode time in the score and keep it below 2^53 so the double is exact. `ZINCRBY` is not idempotent, so a replayed event adds twice: use `ZADD` with an absolute score. See [Redis](08-redis.md#common-uses).

??? question "Pub/sub, a list, and a stream can all pass messages. When do you pick each?"

    Pub/sub is for live fan-out where a missed message is fine: delivery is at most once and nothing is stored. A list is a simple queue where a lost job is acceptable, because a consumer that crashes after the pop loses the job. A stream with a consumer group stores entries, keeps each in a pending list until `XACK`, and lets another consumer take it with `XAUTOCLAIM`, which gives at-least-once delivery, so consumers must be idempotent. A stream lives in one node's memory, so it does not replace a partitioned log such as Kafka for large volume. See [Redis](08-redis.md#pubsub-and-streams).

??? question "Is a Redis lock safe? What would you use it for?"

    `SET key token NX PX ttl` is safe as an efficiency lock, where a rare double execution is harmless. It is not safe as a correctness lock: a lease can expire while the holder is paused, and asynchronous replication can lose the lock in a failover. Release with a compare-and-delete (`DELEX key IFEQ token` in 8.4, or a Lua script) so a client does not delete another client's lock. For correctness, make the protected resource check a fencing token, use a unique constraint or an idempotent operation, or use a lock service built on consensus. See [Redis](08-redis.md#common-uses).

??? question "A Redis instance has high latency spikes. Where do you look?"

    Find what blocks the main thread. Run `SLOWLOG GET` for slow commands (`KEYS`, `HGETALL`, `SMEMBERS`, scripts), check `INFO` for `latest_fork_usec` and memory swap, use `LATENCY DOCTOR`, and find large keys with `redis-cli --bigkeys`. Also check for many keys with the same TTL expiring at once, the disk and `appendfsync`, and that transparent huge pages are off. Fixes: `SCAN` instead of `KEYS`, `UNLINK` instead of `DEL`, split big keys, shorter scripts, TTL jitter, and a smaller instance so the fork pause is acceptable. See [Redis](08-redis.md#operational-pitfalls).

??? question "Redis changed its license. What happened, and what does it mean for you?"

    Redis was BSD-licensed up to 7.2.x, and from 7.4 it was under the dual RSALv2 and SSPLv1 licenses, which are not OSI open source licenses. The Linux Foundation announced Valkey, a BSD fork of Redis 7.2.4, on 2024-03-28, and Redis 8.0 added AGPLv3 as a third, OSI-approved option. For most companies that use Redis as a component, the change has no effect, but a cloud provider that sells Redis as a service needs a license from Redis or uses Valkey. If policy forbids AGPL and source-available licenses, use a pre-7.4 build or Valkey, and check the feature set, because the two have diverged. See [Redis](08-redis.md#license-history-and-valkey).

## Scaling a Database

??? question "The database is at 90 percent CPU. How do you scale it?"

    Measure first and scale in order of cost. Find the queries with the most total time and fix them (a missing index, an N+1 pattern, stale statistics), then scale up, then offload repeated reads to a cache and tolerant reads to read replicas. If the CPU comes from writes, replicas do not help, because each replica applies every write. Partition a huge table to cut maintenance cost, and shard only when one primary cannot carry the write rate or the data size. Each step adds operational cost, so take the next step only when the signal says the current one is used up. See [Scaling a Database](09-scaling-a-database.md#the-order-of-steps).

??? question "Why can a smaller connection pool give higher throughput?"

    The database runs only as many queries in parallel as it has CPU cores and disk parallelism. A larger pool adds context switches, lock contention, and memory use per connection, and queries wait inside the database instead of in the pool. The HikariCP wiki gives a starting formula, `(core_count * 2) + effective_spindle_count`, and you test around it. The server also bounds it: pool size times instance count must stay below `max_connections`. If threads wait for connections, look for long transactions and leaked connections before you raise the size. See [Scaling a Database](09-scaling-a-database.md#connections).

??? question "A user posts a comment and does not see it after the redirect. The service reads from a replica. What do you do?"

    This is replication lag: the write went to the primary and the read reached a replica that has not applied it yet. The goal is read-your-writes for that user. Options: read from the primary for a short window after the user's write, read from the primary for data the user can modify, or store the commit's log position and read only from a replica that has applied it. A synchronous standby with `synchronous_commit = remote_apply` also works, at the cost of commit latency. Skip replicas above a lag limit, and monitor lag so the window is longer than the real lag. See [Scaling a Database](09-scaling-a-database.md#read-replicas).

??? question "What is the difference between partitioning and sharding, and what is the catch with primary keys on partitioned tables?"

    Partitioning splits one table into physical tables inside one server, and sharding splits data across servers. Partitioning helps maintenance and queries that prune to one partition and adds no capacity beyond the server, while sharding adds write capacity and storage and costs cross-shard joins and transactions. The catch in PostgreSQL: a primary key or unique constraint on a partitioned table must include all partition key columns, so a table partitioned by `occurred_at` needs a key like `(event_id, occurred_at)`. MySQL has the same rule. See [Scaling a Database](09-scaling-a-database.md#table-partitioning-inside-one-database).

??? question "Someone ran `DELETE FROM orders` without a `WHERE` at 14:03. How do you recover?"

    A replica does not help, because it replayed the delete, so use point-in-time recovery. In PostgreSQL, restore a base backup, set `restore_command` and a recovery target of 14:02, and replay the archived WAL; check the table, then promote, and copy the lost rows back so that production keeps the writes after 14:03. In MySQL, use a full backup plus `mysqlbinlog` with a stop point before the statement. The prerequisite is that backups and archiving were running and that someone tested the restore. The data you can lose is the unarchived WAL (the RPO), and the RTO is the restore time. See [Scaling a Database](09-scaling-a-database.md#backups-and-point-in-time-recovery).

## OLTP, OLAP, and Warehousing

??? question "Why do you not run analytics queries on the production OLTP database?"

    OLTP serves many short transactions that touch a few rows. An analytics query scans millions of rows, so on a row store it reads every column, fills the buffer pool with pages that web requests do not need, uses I/O that transactions need, and holds a snapshot open, which delays cleanup of old row versions. A read replica isolates the primary but keeps the row-store cost and adds conflicts and lag. The lasting answer is a column store fed by a pipeline, usually with change data capture (CDC). The trade-off is a delayed copy and pipeline operations work. See [OLTP, OLAP, and Warehousing](10-oltp-olap-and-warehousing.md#oltp-against-olap).

??? question "Why is a column store faster for aggregates and slower for single-row writes?"

    An aggregate such as `SUM(amount)` needs one column, and a column store keeps each column together, so it reads only that column. The values have one type and compress well (run-length, dictionary, and bit-packing encoding), and the engine processes vectors of values. A new row must be written to every column, and compressed, sorted columns are hard to update in place. So load in batches (ClickHouse writes each insert as a new part and merges parts in the background), avoid single-row updates, and keep OLTP in a row store. See [OLTP, OLAP, and Warehousing](10-oltp-olap-and-warehousing.md#row-storage-against-column-storage).

??? question "Design a star schema for an online store's sales reporting. What is the grain?"

    Declare the grain first: one row for each order line, the lowest level of detail, because you can roll up atomic rows and cannot split aggregated rows. The fact table `fact_sales` holds foreign keys `date_key`, `product_key`, and `customer_key`, and additive measures `quantity` and `amount_cents`. Dimensions are `dim_date`, `dim_product`, and `dim_customer`, each with a surrogate key and denormalized attributes. Compute ratios such as average price at query time from sums, because a ratio is not additive. A snowflake schema normalizes the dimensions, which saves repeated text and costs joins. See [OLTP, OLAP, and Warehousing](10-oltp-olap-and-warehousing.md#dimensional-modeling).

??? question "A product changes category. How does the warehouse keep the history correct? Explain SCD Type 2."

    A Type 2 slowly changing dimension keeps one row for each version of the product, each with its own surrogate key, `valid_from`, `valid_to`, and `is_current`. When the category changes, the loader closes the old row and inserts a new row with a new surrogate key. New fact rows get the surrogate key of the version valid at the event, so a join on the surrogate key shows each sale under the category of its time. Type 1 overwrites and loses history, and Type 3 adds a `previous_category` column that keeps one old value. Type 2 costs storage and a more complex load. See [OLTP, OLAP, and Warehousing](10-oltp-olap-and-warehousing.md#dimensional-modeling).

??? question "How do you get data from the order service's PostgreSQL database into the warehouse?"

    Three methods exist: a nightly full copy is simple and does not scale, an incremental query on `updated_at` is cheap but misses hard deletes, and CDC reads the transaction log, so it sees every insert, update, and delete in commit order. With Debezium on PostgreSQL, a connector takes a snapshot and streams logical decoding output, usually into Kafka. The risk is that the connector holds a replication slot, so a stopped connector can fill the primary's disk: monitor slot lag and set `max_slot_wal_keep_size`. Delivery is at least once, so loads must be idempotent. See [OLTP, OLAP, and Warehousing](10-oltp-olap-and-warehousing.md#etl-elt-and-change-data-capture).

## SQL Interview Problems

??? question "How do you return the Nth highest distinct salary, or `NULL` if it does not exist?"

    Use `DENSE_RANK() OVER (ORDER BY salary DESC)` in a subquery, and an outer `MAX(salary)` where the rank equals N, so an empty result gives `NULL`. `DENSE_RANK` handles ties, so "second highest" means the second distinct value and not the second row. A bare `LIMIT 1 OFFSET 1` returns zero rows instead of `NULL` when no value exists, and it needs `WHERE salary IS NOT NULL`, because PostgreSQL sorts `NULL` first in a `DESC` order. See [SQL Interview Problems](sql-interview-problems.md#the-nth-highest-value).

??? question "How do you get the top N rows in each group, for example the top three salaries in each department?"

    Compute `DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC)` in a CTE and filter `rank <= 3` outside, because a window function cannot be in `WHERE`. Pick `ROW_NUMBER`, `RANK`, or `DENSE_RANK` from the tie rule: `ROW_NUMBER` drops tied rows, and `RANK` leaves gaps after ties. For the top one row per group, a row-value `IN` on `MAX` also works. See [SQL Interview Problems](sql-interview-problems.md#ranking-and-top-n-per-group).

??? question "How do you delete duplicate rows and keep one?"

    Keep the row with the smallest id for each key and delete the rest: `DELETE ... WHERE id NOT IN (SELECT MIN(id) ... GROUP BY email)`. `GROUP BY` puts all `NULL` emails in one group, so one `NULL` row stays, and `NOT IN` is safe because `MIN(id)` of a primary key is never `NULL`. MySQL raises an error when a `DELETE` selects from the same table in a subquery, so it needs the multi-table `DELETE a FROM t a JOIN t b` form. PostgreSQL has `DELETE ... USING`. Run the matching `SELECT` first and check the row count. See [SQL Interview Problems](sql-interview-problems.md#duplicates-single-occurrences-and-shared-values).

??? question "How do you find runs of consecutive values, for example numbers that appear in at least three consecutive rows?"

    Use the gaps-and-islands technique: `id - ROW_NUMBER() OVER (ORDER BY id)` stays constant while ids are consecutive and jumps at a gap. Group by that value (and by the number) and use `COUNT(*)` as the island length. A `LAG`/`LEAD` comparison looks at neighbor rows and accepts a run that has an id gap, so it is wrong when ids are missing. A self join works for a fixed length, and the island technique scales to a variable length. See [SQL Interview Problems](sql-interview-problems.md#gaps-and-islands-consecutive-rows).

??? question "Why does a running total give the same value to rows with the same sort key, and how do you fix it?"

    With an `ORDER BY` and no frame clause, the default frame is `RANGE ... UNBOUNDED PRECEDING AND CURRENT ROW`, which adds all peers of the current row. The fix is `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` and a unique `ORDER BY`. A `ROWS` frame counts rows, and a `RANGE` frame counts values, which matters for a moving average over days. See [SQL Interview Problems](sql-interview-problems.md#running-totals).

??? question "How do you compute a ratio or rate safely, for example a click-through rate with zero impressions?"

    Multiply by `1.0` and divide by `NULLIF(divisor, 0)`: `1.0 * clicks / NULLIF(impressions, 0)`. Integer division gives 0 for `1 / 2` in SQLite and PostgreSQL, and division by zero raises an error in PostgreSQL, while SQLite and MySQL return `NULL` in a `SELECT`. Use `COUNT(right.key)` and not `COUNT(*)` after a `LEFT JOIN`, so groups without rows count as 0. Average the sums, not the ratios. See [SQL Interview Problems](sql-interview-problems.md#ratios-and-rates).

??? question "How do you compute the median per group in SQL?"

    PostgreSQL has `percentile_cont(0.5) WITHIN GROUP (ORDER BY x)`, and it ignores `NULL` inputs. SQLite and MySQL 8.4 have no median function, so number the values with `ROW_NUMBER`, count them with `COUNT(*) OVER`, and take the `AVG` of rows `(cnt + 1) / 2` and `(cnt + 2) / 2`. That gives the middle row for an odd count and the mean of the two middle rows for an even count. It relies on integer division, and in MySQL `/` returns a decimal, so use `DIV` there or the query returns a wrong median with no error. See [SQL Interview Problems](sql-interview-problems.md#medians).

??? question "Which SQL differences between SQLite, MySQL, and PostgreSQL cost points in an interview?"

    Integer division: `1 / 2` is 0 in SQLite and PostgreSQL, and 0.5 in MySQL. `NULL` ordering: `NULL` is the smallest in SQLite and MySQL (first in `ASC`), and the largest in PostgreSQL (last in `ASC`). Date arithmetic differs (`DATEDIFF` in MySQL, `a - b` for dates in PostgreSQL). MySQL needs a multi-table `DELETE` form, and `||` is a logical OR in MySQL unless `PIPES_AS_CONCAT` is set. Ask which database the interviewer uses, and write standard SQL (`COALESCE`, `CASE`, `ROW_NUMBER`, CTEs) where it exists. See [SQL Interview Problems](sql-interview-problems.md#dialect-differences).
