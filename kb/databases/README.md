# Databases

This subject covers how databases work inside: SQL, schema design, indexes, transactions, query plans, storage engines, NoSQL families, Redis, scaling one database, and analytical stores. Backend interviews test these topics in the technical rounds and in system design.

## How to Study This Subject

1. Read pages 01 to 05 in order. They are the relational core, and interviewers ask about them most often: SQL, schema design, indexes, transactions and isolation, and query plans.
2. Solve the [SQL interview problems](sql-interview-problems.md) with a timer. Write each query before you open the solution.
3. Read 07 and 08 (NoSQL families and Redis) before the HLD case studies, because the designs use them.
4. Read 06, 09, and 10 (storage engines, scaling, and analytics) for senior roles and for system design follow-up questions.
5. Use the [question bank](interview-question-bank.md) for revision. Each answer links to the page that explains it.

The pages describe PostgreSQL 18, MySQL 8.4 (InnoDB), and Redis 8, and say where a newer release changed a behavior. The SQL examples run on SQLite 3.51. A block for another product names its dialect.

The choice of a database for a system design is on the HLD page [choosing a database](../hld/fundamentals/04-choosing-a-database.md). JDBC, JPA, and Spring transactions are on the Java pages [JDBC, JPA, and Hibernate](../java/16-jdbc-jpa-and-hibernate.md) and [Spring Data and Transactions](../java/19-spring-data-and-transactions.md).

## Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

| Page | Content | Priority |
|---|---|---|
| [The Relational Model and SQL](01-relational-model-and-sql.md) | Joins, `NULL` logic, grouping, subqueries, CTEs, window functions, upserts | P1 |
| [Schema Design and Normalization](02-schema-design-and-normalization.md) | Keys, functional dependencies, normal forms, denormalization, constraints, schema changes | P1 |
| [Indexing](03-indexing.md) | B+ tree, hash, composite, and covering indexes. Selectivity. When an index is not used | P1 |
| [Transactions and Isolation](04-transactions-and-isolation.md) | ACID, anomalies, isolation levels per product, MVCC, locks, deadlocks | P1 |
| [Query Execution and Tuning](05-query-execution-and-tuning.md) | `EXPLAIN`, join algorithms, the N+1 problem, pagination, a tuning workflow | P1 |
| [Storage Engines](06-storage-engines.md) | Buffer pool, write-ahead log, crash recovery, B-tree engines, LSM trees, compaction | P2 |
| [NoSQL Families](07-nosql-families.md) | Key-value, document, wide-column, graph, and time-series stores: data models and internals | P1 |
| [Redis](08-redis.md) | Data types, expiry, persistence, replication, Cluster, transactions, common uses | P1 |
| [Scaling a Database](09-scaling-a-database.md) | Connection pools, read replicas, table partitioning, backups, from the database view | P2 |
| [OLTP, OLAP, and Warehousing](10-oltp-olap-and-warehousing.md) | Row and column stores, star schema, slowly changing dimensions, ETL and ELT, change data capture | P2 |
| [SQL Interview Problems](sql-interview-problems.md) | 32 classic SQL problems with solutions that run, grouped by technique | P1 |
| [Interview Question Bank](interview-question-bank.md) | Short questions with model answers. Each answer links to the page that explains it | P1 |
