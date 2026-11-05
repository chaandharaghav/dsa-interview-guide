---
priority: P1
---
# SQL Interview Problems

## Why It Matters

Many backend interviews include a live SQL round. The interviewer gives two or three tables and asks for a query: "the second highest salary", "the top three products per category", "delete the duplicate rows", "the users who logged in three days in a row". The round tests four things: whether you read a requirement exactly, whether you choose the right tool (join, grouping, window function, recursive query), whether you handle `NULL`, ties, and empty groups, and whether you can explain why the query is correct. On the job, the same patterns produce reports, clean-up scripts, and data checks. A wrong answer here is usually silent: the query runs and returns a plausible number.

This page has 32 problems in 13 technique groups. 27 of them are LeetCode database problems. The other 5 are classic interview tasks that are not on LeetCode (one of them is a variant of a premium LeetCode problem). Each problem has its own sample data with the edge cases (ties, `NULL`, empty groups, a single row). Try each problem before you open the solution. The runner of this KB runs every solution on SQLite 3.51 and checks the printed result. The theory (joins, `NULL`, grouping, windows, CTEs) is in [relational model and SQL](01-relational-model-and-sql.md), and the cost of a query is in [query execution and tuning](05-query-execution-and-tuning.md). The questions about concepts are in [Databases Interview Question Bank](interview-question-bank.md).

!!! info "Versions and conventions"
    All solutions run on SQLite 3.51. A MySQL 8.4 or PostgreSQL 18 form that SQLite cannot run carries a `sql-skip` marker, and the text before it names the dialect. The problem statements and the sample data are original. Each LeetCode problem is named by its number, title, and difficulty from the LeetCode problem list (`leetcode.com/api/problems/database/`, read on 2026-11-05). All SQL blocks of this page share one database in page order. Each setup block starts with `DROP TABLE IF EXISTS`.

| Where | What you find |
|---|---|
| Problems | A statement, the setup SQL, and a collapsed solution with its result and its reason |
| [Dialect Differences](#dialect-differences) | Functions that differ between SQLite, MySQL, and PostgreSQL |
| [Common Mistakes](#common-mistakes) | The errors that cost points |

## How to Solve a SQL Problem

Use the same five steps every time. They work in an interview and on the job.

1. **Restate the output.** Write the output columns and the grain: one row per what? "One row per department" and "one row per employee" lead to different queries. Ask what to return when the answer is empty (zero rows or one row with `NULL`).
2. **Find the tables and the join path.** Name each table that holds a needed column. Choose the join type from the rows that must survive: if rows without a match must appear (customers without orders), use a `LEFT JOIN`. If rows without a match must disappear, use an inner join. If you need the rows without a match only, use an anti-join (`NOT EXISTS`).
3. **Choose grouping or windows.** If the answer has fewer rows than the input (one row per group), use `GROUP BY`. If every input row stays and gets a new column (rank, running total, group size), use a window function. If the question needs "per group, the row that has the maximum", use a window `ROW_NUMBER` or `RANK`, or a row-value `IN` on `MAX`.
4. **Handle `NULL`, ties, and empty groups.** Ask three questions. What happens when a value is `NULL` (comparison, aggregate, `NOT IN`)? What happens when two rows tie (`ROW_NUMBER`, `RANK`, or `DENSE_RANK`)? What happens for a group without rows (`LEFT JOIN` and `COUNT` of a column, not `COUNT(*)`)?
5. **Check with the sample data.** Write a small table with the edge cases, compute the answer by hand, and compare it with the query result. Say the check aloud in an interview. Then think about the cost: a correlated subquery runs once per row, and a missing index turns a lookup into a scan.

!!! tip "Interview habit"
    Write the query in the order of the logical processing: `FROM` and `JOIN`, `WHERE`, `GROUP BY`, `HAVING`, window functions, `SELECT`, `ORDER BY`, `LIMIT`. This order explains why `WHERE` cannot use a window function result and why a filter on a `LEFT JOIN` right table in `WHERE` turns the join into an inner join. See [the logical order of query processing](01-relational-model-and-sql.md).

## Technique Index

| Technique | Problems |
|---|---|
| Aggregate on empty set | 8 (LC 619), 10 (LC 176), 11 (LC 177) |
| Anti-join | 3 (LC 183) |
| COALESCE/NULLIF | 24 (LC 1934) |
| Conditional aggregation | 22 (LC 1179), 23 (LC 1193) |
| Cumulative SUM | 20 (LC 1204) |
| DELETE with subquery | 7 (LC 196) |
| DENSE_RANK | 11 (LC 177), 12 (LC 178), 14 (LC 185) |
| Daily rate | 25 (LC 262) |
| Date arithmetic | 18 (LC 197), 31 (LC 550) |
| Default value | 28 (LC 1164) |
| Divide by zero | 26 (not LC) |
| Filtering by join | 25 (LC 262) |
| First event | 31 (LC 550) |
| First row per group | 29 (LC 1070), 30 (LC 1174) |
| Frame ties | 21 (not LC) |
| GROUP BY / HAVING | 5 (LC 570), 6 (LC 182), 8 (LC 619) |
| GROUP BY month | 23 (LC 1193) |
| Gaps and islands | 15 (LC 180), 16 (LC 601), 17 (not LC) |
| HAVING COUNT DISTINCT | 4 (LC 1045) |
| Hierarchy | 27 (not LC) |
| Integer division | 24 (LC 1934), 26 (not LC), 32 (not LC) |
| LAG with a date check | 18 (LC 197) |
| LEAD | 17 (not LC) |
| LEFT JOIN | 1 (LC 175) |
| LEFT JOIN with empty group | 24 (LC 1934) |
| Latest row per group | 28 (LC 1164) |
| Median | 32 (not LC) |
| Moving average | 19 (LC 1321) |
| NOT EXISTS | 3 (LC 183) |
| NOT IN trap | 3 (LC 183) |
| NULL group | 23 (LC 1193) |
| NULL ordering | 10 (LC 176) |
| NULLIF | 26 (not LC) |
| Nth highest | 10 (LC 176), 11 (LC 177) |
| Path and depth | 27 (not LC) |
| Percentage | 30 (LC 1174) |
| Pivot | 22 (LC 1179) |
| RANGE frame | 19 (LC 1321) |
| RANK | 29 (LC 1070) |
| RANK vs DENSE_RANK | 12 (LC 178) |
| RANK vs ROW_NUMBER | 14 (LC 185) |
| ROW_NUMBER | 7 (LC 196), 28 (LC 1164), 32 (not LC) |
| Ratio | 24 (LC 1934), 25 (LC 262), 26 (not LC), 30 (LC 1174) |
| Recursive CTE | 27 (not LC) |
| Relational division | 4 (LC 1045) |
| Retention | 31 (LC 550) |
| Row-value IN | 13 (LC 184), 29 (LC 1070) |
| Running total | 20 (LC 1204), 21 (not LC) |
| Self join | 2 (LC 181), 5 (LC 570), 15 (LC 180), 18 (LC 197) |
| Ties | 13 (LC 184) |
| Top-1 per group | 13 (LC 184) |
| Top-N per group | 14 (LC 185) |
| Window COUNT | 16 (LC 601) |
| Window COUNT over partition | 9 (LC 585) |
| Window frame ROWS | 19 (LC 1321), 21 (not LC) |
| id - ROW_NUMBER | 15 (LC 180), 16 (LC 601) |


The list of all problems:

| # | Problem | LeetCode | Difficulty |
|---|---|---|---|
| 1 | Combine Two Tables | 175 | Easy |
| 2 | Employees Earning More Than Their Managers | 181 | Easy |
| 3 | Customers Who Never Order | 183 | Easy |
| 4 | Customers Who Bought All Products | 1045 | Medium |
| 5 | Managers with at Least 5 Direct Reports | 570 | Medium |
| 6 | Duplicate Emails | 182 | Easy |
| 7 | Delete Duplicate Emails | 196 | Easy |
| 8 | Biggest Single Number | 619 | Easy |
| 9 | Investments in 2016 | 585 | Medium |
| 10 | Second Highest Salary | 176 | Medium |
| 11 | Nth Highest Salary | 177 | Medium |
| 12 | Rank Scores | 178 | Medium |
| 13 | Department Highest Salary | 184 | Medium |
| 14 | Department Top Three Salaries | 185 | Hard |
| 15 | Consecutive Numbers | 180 | Medium |
| 16 | Human Traffic of Stadium | 601 | Hard |
| 17 | Missing ticket numbers | - | - |
| 18 | Rising Temperature | 197 | Easy |
| 19 | Restaurant Growth | 1321 | Medium |
| 20 | Last Person to Fit in the Bus | 1204 | Medium |
| 21 | Running balance per account | - | - |
| 22 | Reformat Department Table | 1179 | Easy |
| 23 | Monthly Transactions I | 1193 | Medium |
| 24 | Confirmation Rate | 1934 | Medium |
| 25 | Trips and Users | 262 | Hard |
| 26 | Click-through rate with zero impressions | - | - |
| 27 | All reports of a manager (recursive) | - | - |
| 28 | Product Price at a Given Date | 1164 | Medium |
| 29 | Product Sales Analysis III | 1070 | Medium |
| 30 | Immediate Food Delivery II | 1174 | Medium |
| 31 | Game Play Analysis IV | 550 | Medium |
| 32 | Median salary per department | - | - |


## Joins and Anti-joins

These problems test the join type, the position of a filter, and the treatment of rows without a match. The theory is in [joins](01-relational-model-and-sql.md).

### Problem 1: Combine Two Tables

**LeetCode 175, Combine Two Tables (Easy).** A `person` table holds people. An `address` table holds at most one address per person. Return the first name, last name, city, and state of every person. If a person has no address, return `NULL` for the city and the state.

```sql
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS person;
CREATE TABLE person (person_id INTEGER PRIMARY KEY, last_name TEXT, first_name TEXT);
CREATE TABLE address (address_id INTEGER PRIMARY KEY, person_id INTEGER, city TEXT, state TEXT);
INSERT INTO person VALUES (1, 'Wang', 'Allen'), (2, 'Alice', 'Bob'), (3, 'Diaz', 'Carol');
INSERT INTO address VALUES (1, 2, 'New York', 'NY'), (2, 3, 'Leeds', 'UK');
INSERT INTO person VALUES (4, 'Roy', 'Dev');
```

??? example "Solution"

    ```sql
    SELECT p.first_name, p.last_name, a.city, a.state
    FROM person p
    LEFT JOIN address a ON a.person_id = p.person_id
    ORDER BY p.person_id;
    ```

    <!-- sql-result -->

    ```text
    first_name | last_name | city | state
    Allen | Wang | NULL | NULL
    Bob | Alice | New York | NY
    Carol | Diaz | Leeds | UK
    Dev | Roy | NULL | NULL
    ```

    The `LEFT JOIN` keeps every row of `person`. For a person without a matching `address` row, the database fills the `address` columns with `NULL`. An inner join would drop Allen and Dev. If a person could have two addresses, the join would return two rows for that person. The problem rules this out, and a real schema must enforce it with a unique constraint on `address.person_id`.


### Problem 2: Employees Earning More Than Their Managers

**LeetCode 181, Employees Earning More Than Their Managers (Easy).** An `emp_mgr` table stores each employee with a salary and the id of the manager, who is a row of the same table. Return the names of employees who earn more than their manager.

```sql
DROP TABLE IF EXISTS emp_mgr;
CREATE TABLE emp_mgr (emp_id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, manager_id INTEGER);
INSERT INTO emp_mgr VALUES
  (1, 'Joe', 70, 3), (2, 'Henry', 80, 4), (3, 'Sam', 60, NULL),
  (4, 'Max', 90, NULL), (5, 'Ann', 60, 3);
```

??? example "Solution"

    ```sql
    SELECT e.name AS employee
    FROM emp_mgr e
    JOIN emp_mgr m ON m.emp_id = e.manager_id
    WHERE e.salary > m.salary
    ORDER BY e.emp_id;
    ```

    <!-- sql-result -->

    ```text
    employee
    Joe
    ```

    The query reads the table twice, with the aliases `e` (employee) and `m` (manager). The join condition links an employee to the manager row. Max and Sam have `manager_id` of `NULL`, so the equality never holds and the inner join drops them. Ann earns the same as her manager Sam, so the strict `>` excludes her. Henry earns less than Max, so he does not appear.


### Problem 3: Customers Who Never Order

**LeetCode 183, Customers Who Never Order (Easy).** Return the customers who have no row in the `orders` table. One order row has a `NULL` customer id (a guest checkout).

```sql
DROP TABLE IF EXISTS orders_g;
DROP TABLE IF EXISTS customers_g;
CREATE TABLE customers_g (id INTEGER PRIMARY KEY, name TEXT);
CREATE TABLE orders_g (order_id INTEGER PRIMARY KEY, customer_id INTEGER);
INSERT INTO customers_g VALUES (1, 'Ann'), (2, 'Bob'), (3, 'Cy'), (4, 'Di');
INSERT INTO orders_g VALUES (1, 1), (2, 3), (3, NULL);
```

??? example "Solution"

    ```sql
    SELECT c.name AS customer
    FROM customers_g c
    WHERE NOT EXISTS (SELECT 1 FROM orders_g o WHERE o.customer_id = c.id)
    ORDER BY c.id;
    ```

    <!-- sql-result -->

    ```text
    customer
    Bob
    Di
    ```

    `NOT EXISTS` is an anti-join: it keeps a customer when the subquery finds no row. The `NULL` customer id never equals a customer id, so it does not disturb the result. A second form joins and keeps the rows without a match:

    ```sql
    SELECT c.name AS customer
    FROM customers_g c
    LEFT JOIN orders_g o ON o.customer_id = c.id
    WHERE o.order_id IS NULL
    ORDER BY c.id;
    ```

    <!-- sql-result -->

    ```text
    customer
    Bob
    Di
    ```

    Test the right table's primary key (or any `NOT NULL` column) for `NULL`, not a column that can be `NULL`. The `NOT IN` form is the trap. If the subquery returns one `NULL`, `id NOT IN (1, 3, NULL)` evaluates to `UNKNOWN` for every id that is not in the list, so the query returns no row. See [NULL and three-valued logic](01-relational-model-and-sql.md).

    ```sql
    SELECT COUNT(*) AS rows_returned
    FROM customers_g
    WHERE id NOT IN (SELECT customer_id FROM orders_g);
    ```

    <!-- sql-result -->

    ```text
    rows_returned
    0
    ```

    The result is 0 rows instead of Bob and Di. The fix is `NOT EXISTS`, or `WHERE customer_id IS NOT NULL` inside the subquery.


### Problem 4: Customers Who Bought All Products

**LeetCode 1045, Customers Who Bought All Products (Medium).** A `purchase` table lists (customer, product) rows, and a `product_list` table lists all products. Return the customers who bought every product at least once. A customer can buy the same product twice.

```sql
DROP TABLE IF EXISTS purchase;
DROP TABLE IF EXISTS product_list;
CREATE TABLE product_list (product_id INTEGER PRIMARY KEY);
CREATE TABLE purchase (customer_id INTEGER, product_id INTEGER);
INSERT INTO product_list VALUES (1), (2), (3);
INSERT INTO purchase VALUES
  (1, 1), (1, 2), (1, 3),
  (2, 1), (2, 2),
  (3, 3), (3, 2), (3, 1), (3, 1),
  (4, 1);
```

??? example "Solution"

    ```sql
    SELECT customer_id
    FROM purchase
    GROUP BY customer_id
    HAVING COUNT(DISTINCT product_id) = (SELECT COUNT(*) FROM product_list)
    ORDER BY customer_id;
    ```

    <!-- sql-result -->

    ```text
    customer_id
    1
    3
    ```

    This is relational division: "find the groups that contain all of a set". The query counts the distinct products of each customer and compares the count with the size of the product list. `DISTINCT` matters: customer 3 has four rows but three products, and without `DISTINCT` a customer who bought product 1 three times would pass with a wrong count. The query assumes that every `purchase.product_id` exists in `product_list` (a foreign key). If not, restrict the count with a join to `product_list`.


### Problem 5: Managers with at Least 5 Direct Reports

**LeetCode 570, Managers with at Least 5 Direct Reports (Medium).** An `emp_rep` table stores employees and their manager ids. Return the names of managers with at least five direct reports.

```sql
DROP TABLE IF EXISTS emp_rep;
CREATE TABLE emp_rep (id INTEGER PRIMARY KEY, name TEXT, manager_id INTEGER);
INSERT INTO emp_rep VALUES
  (1, 'Ann', NULL), (2, 'E2', 1), (3, 'E3', 1), (4, 'E4', 1), (5, 'E5', 1), (6, 'E6', 1),
  (7, 'Bob', NULL), (8, 'F8', 7), (9, 'F9', 7), (10, 'F10', 7), (11, 'F11', 7);
```

??? example "Solution"

    ```sql
    SELECT m.name
    FROM emp_rep m
    JOIN emp_rep e ON e.manager_id = m.id
    GROUP BY m.id, m.name
    HAVING COUNT(*) >= 5;
    ```

    <!-- sql-result -->

    ```text
    name
    Ann
    ```

    The join pairs each manager with each direct report. `GROUP BY` the manager gives one group per manager, and `HAVING` filters the groups after counting. Bob has four reports, so he is excluded at the boundary. Group by `m.id` and not only by `m.name`, because two managers can have the same name. A second form avoids the join:

    ```sql
    SELECT name
    FROM emp_rep
    WHERE id IN (SELECT manager_id
                 FROM emp_rep
                 GROUP BY manager_id
                 HAVING COUNT(*) >= 5);
    ```

    <!-- sql-result -->

    ```text
    name
    Ann
    ```

    The subquery returns a `NULL` group for the top managers. The `IN` list then contains `NULL`, which is harmless for `IN` (it only matters for `NOT IN`).


## Duplicates, Single Occurrences, and Shared Values

Grouping finds the values that repeat or appear once. A window `COUNT` finds the same facts without collapsing rows.

### Problem 6: Duplicate Emails

**LeetCode 182, Duplicate Emails (Easy).** A `mail` table has an email column. Return each email that appears more than once. Two rows have a `NULL` email, and the result must not list `NULL`.

```sql
DROP TABLE IF EXISTS mail;
CREATE TABLE mail (id INTEGER PRIMARY KEY, email TEXT);
INSERT INTO mail VALUES
  (1, 'a@x.com'), (2, 'b@x.com'), (3, 'a@x.com'), (4, 'c@x.com'), (5, 'c@x.com'),
  (6, 'a@x.com'), (7, NULL), (8, NULL);
```

??? example "Solution"

    ```sql
    SELECT email
    FROM mail
    WHERE email IS NOT NULL
    GROUP BY email
    HAVING COUNT(*) > 1
    ORDER BY email;
    ```

    <!-- sql-result -->

    ```text
    email
    a@x.com
    c@x.com
    ```

    `GROUP BY` puts all `NULL` values into one group, so without the `WHERE` filter the two `NULL` rows would form a group of two and `NULL` would appear in the result. `HAVING COUNT(*) > 1` keeps the groups with more than one row. Case matters too: `A@x.com` and `a@x.com` are different strings in SQLite and PostgreSQL. Ask the interviewer whether to apply `LOWER`.


### Problem 7: Delete Duplicate Emails

**LeetCode 196, Delete Duplicate Emails (Easy).** In a `mail_dup` table, keep one row for each email: the row with the smallest id. Delete all other rows. Treat the `NULL` emails as equal to each other for this task.

```sql
DROP TABLE IF EXISTS mail_dup;
CREATE TABLE mail_dup (id INTEGER PRIMARY KEY, email TEXT);
INSERT INTO mail_dup VALUES
  (1, 'a@x.com'), (2, 'b@x.com'), (3, 'a@x.com'), (4, 'c@x.com'), (5, 'b@x.com'),
  (6, 'a@x.com'), (7, NULL), (8, NULL);
```

??? example "Solution"

    First, preview the rows to delete. `ROW_NUMBER` numbers the rows of each email by id, so every row with a number above 1 is a duplicate.

    ```sql
    SELECT id
    FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) AS rn
          FROM mail_dup)
    WHERE rn > 1
    ORDER BY id;
    ```

    <!-- sql-result -->

    ```text
    id
    3
    5
    6
    8
    ```

    Now delete. The subquery returns the smallest id of each email group. The statement deletes every row whose id is not in that list.

    ```sql
    DELETE FROM mail_dup
    WHERE id NOT IN (SELECT MIN(id) FROM mail_dup GROUP BY email);
    ```

    ```sql
    SELECT id, email FROM mail_dup ORDER BY id;
    ```

    <!-- sql-result -->

    ```text
    id | email
    1 | a@x.com
    2 | b@x.com
    4 | c@x.com
    7 | NULL
    ```

    `GROUP BY` puts the `NULL` emails in one group, so the query keeps id 7 and deletes id 8. The `NOT IN` is safe here because `MIN(id)` is never `NULL` (id is the primary key). Two rules for other databases: MySQL raises an error when a `DELETE` selects from the same table in a subquery ("You cannot delete from a table and select from the same table in a subquery", MySQL 8.4 `DELETE` page), so MySQL needs the multi-table form. PostgreSQL has `DELETE ... USING`. Both forms compare with `=`, so they do not delete duplicate `NULL` rows.

    MySQL 8.4 form (not run here):

    <!-- sql-skip: mysql -->

    ```sql
    DELETE e1
    FROM mail_dup e1
    JOIN mail_dup e2 ON e1.email = e2.email AND e1.id > e2.id;
    ```

    PostgreSQL 18 form (not run here):

    <!-- sql-skip: postgresql -->

    ```sql
    DELETE FROM mail_dup a
    USING mail_dup b
    WHERE a.email = b.email AND a.id > b.id;
    ```

    In production, take a backup or run the statement in a transaction, check the number of deleted rows, and add a `UNIQUE` constraint on `email` to prevent new duplicates.


### Problem 8: Biggest Single Number

**LeetCode 619, Biggest Single Number (Easy).** A `nums` table holds integers, with repeats. Return the largest number that appears exactly once. If no number appears once, return `NULL` (one row, not zero rows).

```sql
DROP TABLE IF EXISTS nums;
DROP TABLE IF EXISTS nums_none;
CREATE TABLE nums (n INTEGER);
CREATE TABLE nums_none (n INTEGER);
INSERT INTO nums VALUES (8), (8), (3), (3), (1), (4), (5), (6);
INSERT INTO nums_none VALUES (5), (5), (7), (7);
```

??? example "Solution"

    ```sql
    SELECT MAX(n) AS num
    FROM (SELECT n FROM nums GROUP BY n HAVING COUNT(*) = 1);
    ```

    <!-- sql-result -->

    ```text
    num
    6
    ```

    The inner query keeps the numbers that appear once: 1, 4, 5, and 6. The outer `MAX` returns 6. An aggregate without `GROUP BY` always returns exactly one row, even over zero input rows. In that case `MAX` returns `NULL`:

    ```sql
    SELECT MAX(n) AS num
    FROM (SELECT n FROM nums_none GROUP BY n HAVING COUNT(*) = 1);
    ```

    <!-- sql-result -->

    ```text
    num
    NULL
    ```

    If you wrote `ORDER BY n DESC LIMIT 1` instead, the empty case would return no row, which is a different answer.


### Problem 9: Investments in 2016

**LeetCode 585, Investments in 2016 (Medium).** An `ins` table stores policies with a 2015 value, a 2016 value, and a location (latitude and longitude). Sum the 2016 values of the policies that meet both rules: another policy has the same 2015 value, and no other policy has the same location.

```sql
DROP TABLE IF EXISTS ins;
CREATE TABLE ins (pid INTEGER PRIMARY KEY, tiv_2015 REAL, tiv_2016 REAL, lat REAL, lon REAL);
INSERT INTO ins VALUES
  (1, 10, 5, 10, 10), (2, 20, 20, 20, 20), (3, 10, 30, 20, 20),
  (4, 10, 40, 40, 40), (5, 30, 7, 50, 50);
```

??? example "Solution"

    ```sql
    SELECT SUM(tiv_2016) AS total_2016
    FROM (SELECT tiv_2016,
                 COUNT(*) OVER (PARTITION BY tiv_2015) AS same_tiv,
                 COUNT(*) OVER (PARTITION BY lat, lon) AS same_loc
          FROM ins)
    WHERE same_tiv > 1 AND same_loc = 1;
    ```

    <!-- sql-result -->

    ```text
    total_2016
    45.0
    ```

    A window `COUNT` attaches the group size to every row without collapsing the rows, so one pass tests both rules. Policies 1, 3, and 4 share the 2015 value 10. Policies 2 and 3 share a location, so policy 3 fails the second rule. Policies 1 and 4 pass: 5 + 40 = 45. If no policy qualifies, `SUM` returns `NULL`. Use `COALESCE(SUM(...), 0)` if the caller wants 0.


## The Nth Highest Value

The rule for ties (distinct values or rows) and the empty result (one `NULL` row, or no row) decide the answer.

### Problem 10: Second Highest Salary

**LeetCode 176, Second Highest Salary (Medium).** A `sal` table lists employee salaries. Return the second highest distinct salary, or `NULL` if there is none. The table has two employees with the top salary and one `NULL` salary.

```sql
DROP TABLE IF EXISTS sal;
DROP TABLE IF EXISTS sal_one;
CREATE TABLE sal (emp_id INTEGER PRIMARY KEY, salary INTEGER);
CREATE TABLE sal_one (emp_id INTEGER PRIMARY KEY, salary INTEGER);
INSERT INTO sal VALUES (1, 300), (2, 200), (3, 300), (4, NULL), (5, 100);
INSERT INTO sal_one VALUES (1, 500);
```

??? example "Solution"

    ```sql
    SELECT MAX(salary) AS second_highest
    FROM sal
    WHERE salary < (SELECT MAX(salary) FROM sal);
    ```

    <!-- sql-result -->

    ```text
    second_highest
    200
    ```

    The subquery finds the highest salary (300). The outer query takes the highest salary below it. Two employees share 300, so "second highest" means the second distinct value (200), not the second row. The `NULL` salary fails the comparison and never wins `MAX`. If only one distinct salary exists, the `WHERE` filter removes every row, and `MAX` over zero rows returns `NULL` as the problem needs:

    ```sql
    SELECT MAX(salary) AS second_highest
    FROM sal_one
    WHERE salary < (SELECT MAX(salary) FROM sal_one);
    ```

    <!-- sql-result -->

    ```text
    second_highest
    NULL
    ```

    A second form uses `OFFSET`. It needs the outer `SELECT` around the subquery, because a bare `LIMIT 1 OFFSET 1` returns zero rows (not `NULL`) when no second value exists. It also needs `WHERE salary IS NOT NULL`: PostgreSQL sorts `NULL` first in a `DESC` order, so without the filter `NULL` becomes the "highest" value there (PostgreSQL 18 `SELECT` page).

    ```sql
    SELECT (SELECT DISTINCT salary
            FROM sal
            WHERE salary IS NOT NULL
            ORDER BY salary DESC
            LIMIT 1 OFFSET 1) AS second_highest;
    ```

    <!-- sql-result -->

    ```text
    second_highest
    200
    ```


### Problem 11: Nth Highest Salary

**LeetCode 177, Nth Highest Salary (Medium).** Generalize the previous problem: return the Nth highest distinct salary of the `sal` table from the previous problem, or `NULL` if the table has fewer than N distinct salaries. Show N = 3 and N = 4.

The problem uses the `sal` table of Problem 10.

??? example "Solution"

    ```sql
    WITH r AS (SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rk
               FROM sal
               WHERE salary IS NOT NULL)
    SELECT MAX(salary) AS nth_highest FROM r WHERE rk = 3;
    ```

    <!-- sql-result -->

    ```text
    nth_highest
    100
    ```

    ```sql
    WITH r AS (SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rk
               FROM sal
               WHERE salary IS NOT NULL)
    SELECT MAX(salary) AS nth_highest FROM r WHERE rk = 4;
    ```

    <!-- sql-result -->

    ```text
    nth_highest
    NULL
    ```

    `DENSE_RANK` gives tied salaries the same rank and does not skip ranks, so rank N is the Nth distinct value. `RANK` would be wrong here: after two employees tie at rank 1, `RANK` skips rank 2. The outer `MAX` turns "no row has rank 4" into one `NULL` row. LeetCode asks for a function. SQLite has no stored functions, so the query above fixes N in the `WHERE` clause. MySQL 8.4 allows local variables in `LIMIT` inside a stored program, but not expressions. That is why the MySQL function runs `SET N = N - 1` first.

    MySQL 8.4 form (not run here). MySQL rejects a stored function that declares none of `DETERMINISTIC`, `NO SQL`, or `READS SQL DATA` while binary logging is on (error 1418, MySQL 8.4 "Stored Program Binary Logging" page), so the declaration includes `READS SQL DATA`. In the `mysql` client, wrap the statement in `DELIMITER //` and `//`.

    <!-- sql-skip: mysql -->

    ```sql
    CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
    READS SQL DATA
    BEGIN
      SET N = N - 1;
      RETURN (SELECT DISTINCT salary FROM sal WHERE salary IS NOT NULL
              ORDER BY salary DESC LIMIT 1 OFFSET N);
    END
    ```

    With `N` less than 1, `OFFSET` would be negative and MySQL raises an error. Add a guard if the input is not trusted.


## Ranking and Top-N per Group

`ROW_NUMBER`, `RANK`, and `DENSE_RANK` differ only in how they treat ties. Choose the function from the tie rule of the question.

### Problem 12: Rank Scores

**LeetCode 178, Rank Scores (Medium).** A `scores` table has a score per game. Return each score with its rank, highest first. Equal scores share a rank, and the next rank has no gap.

```sql
DROP TABLE IF EXISTS scores;
CREATE TABLE scores (id INTEGER PRIMARY KEY, score REAL);
INSERT INTO scores VALUES (1, 3.5), (2, 3.65), (3, 4.0), (4, 3.85), (5, 4.0), (6, 3.65);
```

??? example "Solution"

    ```sql
    SELECT score, DENSE_RANK() OVER (ORDER BY score DESC) AS rnk
    FROM scores
    ORDER BY score DESC;
    ```

    <!-- sql-result -->

    ```text
    score | rnk
    4.0 | 1
    4.0 | 1
    3.85 | 2
    3.65 | 3
    3.65 | 3
    3.5 | 4
    ```

    `DENSE_RANK` assigns the same rank to equal scores and continues with the next integer. Compare the three ranking functions on the same data:

    ```sql
    SELECT score,
           ROW_NUMBER() OVER (ORDER BY score DESC) AS row_num,
           RANK()       OVER (ORDER BY score DESC) AS rnk,
           DENSE_RANK() OVER (ORDER BY score DESC) AS dense_rnk
    FROM scores
    ORDER BY score DESC, id;
    ```

    <!-- sql-result -->

    ```text
    score | row_num | rnk | dense_rnk
    4.0 | 1 | 1 | 1
    4.0 | 2 | 1 | 1
    3.85 | 3 | 3 | 2
    3.65 | 4 | 4 | 3
    3.65 | 5 | 4 | 3
    3.5 | 6 | 6 | 4
    ```

    `ROW_NUMBER` never ties. `RANK` ties and then skips (1, 1, 3). `DENSE_RANK` ties and does not skip (1, 1, 2). A form without windows counts the distinct scores that are at least as high:

    ```sql
    SELECT s.score,
           (SELECT COUNT(DISTINCT t.score) FROM scores t WHERE t.score >= s.score) AS rnk
    FROM scores s
    ORDER BY s.score DESC;
    ```

    <!-- sql-result -->

    ```text
    score | rnk
    4.0 | 1
    4.0 | 1
    3.85 | 2
    3.65 | 3
    3.65 | 3
    3.5 | 4
    ```

    This form runs a subquery for every row, so it costs O(n squared) without an index on `score`. Prefer the window function. Name the alias `rnk`: `RANK` is a reserved word in MySQL 8.


### Problem 13: Department Highest Salary

**LeetCode 184, Department Highest Salary (Medium).** Tables `dept_d` and `emp_d` hold departments and employees. For each department that has employees, return the employees with the highest salary of that department. The data has a tie for the top salary, a department with no employee, and an employee without a department.

```sql
DROP TABLE IF EXISTS emp_d;
DROP TABLE IF EXISTS dept_d;
CREATE TABLE dept_d (id INTEGER PRIMARY KEY, name TEXT);
CREATE TABLE emp_d (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, dept_id INTEGER);
INSERT INTO dept_d VALUES (1, 'Eng'), (2, 'Sales'), (3, 'Ops');
INSERT INTO emp_d VALUES
  (1, 'Ann', 90, 1), (2, 'Bob', 90, 1), (3, 'Cy', 70, 1),
  (4, 'Di', 80, 2), (5, 'Eve', 99, NULL);
```

??? example "Solution"

    ```sql
    SELECT d.name AS department, e.name AS employee, e.salary
    FROM emp_d e
    JOIN dept_d d ON d.id = e.dept_id
    WHERE (e.dept_id, e.salary) IN (SELECT dept_id, MAX(salary) FROM emp_d GROUP BY dept_id)
    ORDER BY d.id, e.id;
    ```

    <!-- sql-result -->

    ```text
    department | employee | salary
    Eng | Ann | 90
    Eng | Bob | 90
    Sales | Di | 80
    ```

    The subquery returns the top salary of each department. The outer query keeps every employee whose (department, salary) pair is in that list, so a tie returns both Ann and Bob. Ops has no employee and does not appear. Eve has no department: the inner join drops her, which matches the question. The row-value `IN` works in SQLite, MySQL, and PostgreSQL. A window form ranks the salaries in each department:

    ```sql
    SELECT department, employee, salary
    FROM (SELECT d.name AS department, e.name AS employee, e.salary, d.id AS did, e.id AS eid,
                 RANK() OVER (PARTITION BY d.id ORDER BY e.salary DESC) AS rk
          FROM emp_d e
          JOIN dept_d d ON d.id = e.dept_id)
    WHERE rk = 1
    ORDER BY did, eid;
    ```

    <!-- sql-result -->

    ```text
    department | employee | salary
    Eng | Ann | 90
    Eng | Bob | 90
    Sales | Di | 80
    ```

    Use `RANK` (or `DENSE_RANK`) so that ties stay. `ROW_NUMBER` would return only one of the tied employees. See [window functions](01-relational-model-and-sql.md).


### Problem 14: Department Top Three Salaries

**LeetCode 185, Department Top Three Salaries (Hard).** For each department, return the employees whose salary is among the three highest distinct salaries of the department. The data has ties at the top, a department with only one distinct salary, and a department with fewer than three employees.

```sql
DROP TABLE IF EXISTS emp_t;
DROP TABLE IF EXISTS dept_t;
CREATE TABLE dept_t (id INTEGER PRIMARY KEY, name TEXT);
CREATE TABLE emp_t (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, dept_id INTEGER);
INSERT INTO dept_t VALUES (1, 'Eng'), (2, 'Sales'), (3, 'Ops');
INSERT INTO emp_t VALUES
  (1, 'Ann', 90, 1), (2, 'Bob', 90, 1), (3, 'Cy', 80, 1), (4, 'Dan', 70, 1), (5, 'Eli', 60, 1),
  (6, 'Fay', 50, 2), (7, 'Gus', 50, 2),
  (8, 'Hal', 40, 3), (9, 'Ivy', 30, 3);
```

??? example "Solution"

    ```sql
    SELECT department, employee, salary
    FROM (SELECT d.name AS department, e.name AS employee, e.salary, d.id AS did,
                 DENSE_RANK() OVER (PARTITION BY d.id ORDER BY e.salary DESC) AS rk
          FROM emp_t e
          JOIN dept_t d ON d.id = e.dept_id)
    WHERE rk <= 3
    ORDER BY did, salary DESC, employee;
    ```

    <!-- sql-result -->

    ```text
    department | employee | salary
    Eng | Ann | 90
    Eng | Bob | 90
    Eng | Cy | 80
    Eng | Dan | 70
    Sales | Fay | 50
    Sales | Gus | 50
    Ops | Hal | 40
    Ops | Ivy | 30
    ```

    `PARTITION BY d.id` restarts the ranking in each department. `DENSE_RANK` counts distinct salaries, so "top three salaries" keeps four people in Eng (two at 90, then 80, then 70). Sales has one distinct salary, so both employees stay. The function choice changes the row count:

    ```sql
    SELECT SUM(rn <= 3) AS row_number_rows,
           SUM(rk <= 3) AS rank_rows,
           SUM(dr <= 3) AS dense_rank_rows
    FROM (SELECT ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn,
                 RANK()       OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rk,
                 DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS dr
          FROM emp_t);
    ```

    <!-- sql-result -->

    ```text
    row_number_rows | rank_rows | dense_rank_rows
    7 | 7 | 8
    ```

    `ROW_NUMBER` cuts a tie arbitrarily and returns at most three rows per department. `RANK` keeps ties but counts the tied rows in the position. `DENSE_RANK` matches the usual meaning of "top three salaries". State the tie rule to the interviewer before you write the query. Without window functions (MySQL 5.7), count the distinct higher salaries in a correlated subquery: `WHERE (SELECT COUNT(DISTINCT s.salary) FROM emp_t s WHERE s.dept_id = e.dept_id AND s.salary > e.salary) < 3`. For performance, see [query execution and tuning](05-query-execution-and-tuning.md).


## Gaps and Islands, Consecutive Rows

An island is a run of consecutive rows. The difference between a sequence value and `ROW_NUMBER()` is constant inside an island.

### Problem 15: Consecutive Numbers

**LeetCode 180, Consecutive Numbers (Medium).** A `logs` table has an id and a number. Return each number that appears in at least three rows with consecutive ids. The ids have a gap: id 10 is missing.

```sql
DROP TABLE IF EXISTS logs;
CREATE TABLE logs (id INTEGER PRIMARY KEY, num INTEGER);
INSERT INTO logs VALUES
  (1, 1), (2, 1), (3, 1), (4, 2), (5, 1), (6, 2), (7, 2), (8, 2), (9, 3), (11, 3), (12, 3);
```

??? example "Solution"

    ```sql
    SELECT DISTINCT num AS consecutive_num
    FROM (SELECT num, COUNT(*) AS run_length
          FROM (SELECT num, id - ROW_NUMBER() OVER (PARTITION BY num ORDER BY id) AS grp
                FROM logs)
          GROUP BY num, grp)
    WHERE run_length >= 3
    ORDER BY num;
    ```

    <!-- sql-result -->

    ```text
    consecutive_num
    1
    2
    ```

    This is the "gaps and islands" technique. Within one number, `id - ROW_NUMBER()` stays constant while the ids are consecutive, and jumps when an id is missing. Rows with the same (number, `grp`) pair form one island, and `COUNT(*)` is the island length. Number 1 has ids 1, 2, 3 and 5: the island 1-3 has length 3, so 1 qualifies. Number 3 has ids 9, 11, 12: `grp` is 8, 9, 9, so the longest island has length 2 and 3 does not qualify. A `LAG`/`LEAD` solution (`num = LAG(num, 1) = LAG(num, 2)`) compares neighbor rows and would accept 9, 11, 12 when the ids have gaps. A self join checks the ids directly:

    ```sql
    SELECT DISTINCT a.num AS consecutive_num
    FROM logs a
    JOIN logs b ON b.id = a.id + 1 AND b.num = a.num
    JOIN logs c ON c.id = a.id + 2 AND c.num = a.num
    ORDER BY a.num;
    ```

    <!-- sql-result -->

    ```text
    consecutive_num
    1
    2
    ```

    The self join is simple for a fixed length of three. For a variable length, the island technique scales better.


### Problem 16: Human Traffic of Stadium

**LeetCode 601, Human Traffic of Stadium (Hard).** A `stadium` table has one row per visit day, with an id, a date, and a count of people. Return the rows of every run of three or more consecutive ids where each row has at least 100 people. The data has a run of five days, a run of two days, and a gap in the ids (id 10) that separates a long run from a single high day.

```sql
DROP TABLE IF EXISTS stadium;
CREATE TABLE stadium (id INTEGER PRIMARY KEY, visit_date TEXT, people INTEGER);
INSERT INTO stadium VALUES
  (1, '2024-01-01', 10), (2, '2024-01-02', 109), (3, '2024-01-03', 150),
  (4, '2024-01-04', 99), (5, '2024-01-05', 145), (6, '2024-01-06', 1455),
  (7, '2024-01-07', 199), (8, '2024-01-08', 188), (9, '2024-01-09', 120),
  (11, '2024-01-11', 200);
```

??? example "Solution"

    ```sql
    WITH big AS (
      SELECT id, visit_date, people, id - ROW_NUMBER() OVER (ORDER BY id) AS grp
      FROM stadium
      WHERE people >= 100)
    SELECT id, visit_date, people
    FROM big
    WHERE grp IN (SELECT grp FROM big GROUP BY grp HAVING COUNT(*) >= 3)
    ORDER BY visit_date;
    ```

    <!-- sql-result -->

    ```text
    id | visit_date | people
    5 | 2024-01-05 | 145
    6 | 2024-01-06 | 1455
    7 | 2024-01-07 | 199
    8 | 2024-01-08 | 188
    9 | 2024-01-09 | 120
    ```

    The filter `people >= 100` runs first, so the remaining ids have gaps wherever a day was dropped. `id - ROW_NUMBER()` is then constant inside each run of consecutive ids. The run 2-3 has length 2 and fails. Id 11 is alone. The run 5-9 has length 5 and passes. The gap at id 10 keeps id 11 out of that run: without the `id - ROW_NUMBER()` step, a plain count of the high days would merge id 11 into it. A window `COUNT` gives the same result in one pass:

    ```sql
    SELECT id, visit_date, people
    FROM (SELECT id, visit_date, people,
                 COUNT(*) OVER (PARTITION BY grp) AS run_length
          FROM (SELECT id, visit_date, people,
                       id - ROW_NUMBER() OVER (ORDER BY id) AS grp
                FROM stadium
                WHERE people >= 100))
    WHERE run_length >= 3
    ORDER BY visit_date;
    ```

    <!-- sql-result -->

    ```text
    id | visit_date | people
    5 | 2024-01-05 | 145
    6 | 2024-01-06 | 1455
    7 | 2024-01-07 | 199
    8 | 2024-01-08 | 188
    9 | 2024-01-09 | 120
    ```


### Problem 17: Missing ticket numbers

**Not on LeetCode.** A `tickets` table holds sold ticket numbers. Return each range of missing numbers between the smallest and the largest sold number, as a start and an end.

```sql
DROP TABLE IF EXISTS tickets;
CREATE TABLE tickets (no INTEGER PRIMARY KEY);
INSERT INTO tickets VALUES (1), (2), (3), (7), (8), (10), (15);
```

??? example "Solution"

    ```sql
    SELECT no + 1 AS gap_start, next_no - 1 AS gap_end
    FROM (SELECT no, LEAD(no) OVER (ORDER BY no) AS next_no FROM tickets)
    WHERE next_no - no > 1
    ORDER BY gap_start;
    ```

    <!-- sql-result -->

    ```text
    gap_start | gap_end
    4 | 6
    9 | 9
    11 | 14
    ```

    `LEAD` reads the next number in order. If the next number is more than one above the current number, the numbers between them are missing. The last row has `next_no` of `NULL`, and the comparison with `NULL` is not true, so it produces no gap. The result has three ranges: 4-6, 9-9, and 11-14. This problem is not on LeetCode.


## Date Comparisons and Rolling Windows

Date arithmetic is the least portable part of SQL. A missing day also breaks solutions that compare neighbor rows.

### Problem 18: Rising Temperature

**LeetCode 197, Rising Temperature (Easy).** A `weather` table has one temperature per date. The dates have a gap: 2024-03-05 is missing. Return the dates whose temperature is higher than on the day before.

```sql
DROP TABLE IF EXISTS weather;
CREATE TABLE weather (record_date TEXT PRIMARY KEY, temp INTEGER);
INSERT INTO weather VALUES
  ('2024-03-01', 10), ('2024-03-02', 25), ('2024-03-03', 20), ('2024-03-04', 30),
  ('2024-03-06', 35), ('2024-03-07', 35);
```

??? example "Solution"

    ```sql
    SELECT w.record_date
    FROM weather w
    JOIN weather p ON p.record_date = date(w.record_date, '-1 day')
    WHERE w.temp > p.temp
    ORDER BY w.record_date;
    ```

    <!-- sql-result -->

    ```text
    record_date
    2024-03-02
    2024-03-04
    ```

    The join pairs each date with the row of the previous calendar day. If that row does not exist, the date has no pair. Date 2024-03-06 follows the missing day, so it does not appear, even though 35 is higher than 30. Date 2024-03-07 is equal to the day before (35), and `>` excludes it. A `LAG` solution compares with the previous row, not the previous day, so it must also check the day difference:

    ```sql
    SELECT record_date
    FROM (SELECT record_date, temp,
                 LAG(temp) OVER (ORDER BY record_date) AS prev_temp,
                 LAG(record_date) OVER (ORDER BY record_date) AS prev_date
          FROM weather)
    WHERE temp > prev_temp AND julianday(record_date) - julianday(prev_date) = 1
    ORDER BY record_date;
    ```

    <!-- sql-result -->

    ```text
    record_date
    2024-03-02
    2024-03-04
    ```

    SQLite has no `DATEDIFF`. `julianday` returns a day number, and the difference of two day numbers is a number of days. MySQL and PostgreSQL write the same check differently (see [Dialect Differences](#dialect-differences)):

    MySQL 8.4 form (not run here):

    <!-- sql-skip: mysql -->

    ```sql
    SELECT w.record_date
    FROM weather w
    JOIN weather p ON DATEDIFF(w.record_date, p.record_date) = 1
    WHERE w.temp > p.temp;
    ```

    PostgreSQL 18 form (not run here):

    <!-- sql-skip: postgresql -->

    ```sql
    SELECT w.record_date
    FROM weather w
    JOIN weather p ON w.record_date - p.record_date = 1
    WHERE w.temp > p.temp;
    ```

    The PostgreSQL form needs `record_date` of type `date`: `date - date` returns an integer number of days (PostgreSQL 18 date/time functions page). The join on a computed expression cannot use an index on `record_date`. Prefer the equality `p.record_date = w.record_date - 1` (PostgreSQL) or `DATE_SUB` (MySQL) when the table is large.


### Problem 19: Restaurant Growth

**LeetCode 1321, Restaurant Growth (Medium).** A `sales` table has one or more rows per day with an amount. For each day that has six earlier days of data, return the total and the average (rounded to two decimals) of the daily totals of that day and the six days before it.

```sql
DROP TABLE IF EXISTS sales;
CREATE TABLE sales (id INTEGER PRIMARY KEY, visited_on TEXT, amount INTEGER);
INSERT INTO sales (visited_on, amount) VALUES
  ('2024-01-01', 100), ('2024-01-02', 110), ('2024-01-03', 120), ('2024-01-03', 30),
  ('2024-01-04', 130), ('2024-01-05', 140), ('2024-01-06', 150), ('2024-01-07', 160),
  ('2024-01-08', 170), ('2024-01-08', 25), ('2024-01-09', 180);
```

??? example "Solution"

    ```sql
    WITH daily AS (
      SELECT visited_on, SUM(amount) AS amt FROM sales GROUP BY visited_on),
    w AS (
      SELECT visited_on,
             SUM(amt) OVER win AS amount,
             ROUND(AVG(amt) OVER win, 2) AS average_amount,
             ROW_NUMBER() OVER (ORDER BY visited_on) AS rn
      FROM daily
      WINDOW win AS (ORDER BY visited_on ROWS BETWEEN 6 PRECEDING AND CURRENT ROW))
    SELECT visited_on, amount, average_amount FROM w WHERE rn >= 7 ORDER BY visited_on;
    ```

    <!-- sql-result -->

    ```text
    visited_on | amount | average_amount
    2024-01-07 | 940 | 134.29
    2024-01-08 | 1035 | 147.86
    2024-01-09 | 1105 | 157.86
    ```

    First, `GROUP BY` collapses the days that have several rows (01-03 and 01-08) to one daily total. The frame `ROWS BETWEEN 6 PRECEDING AND CURRENT ROW` covers seven rows. The first six days do not have a full window, so `rn >= 7` removes them. `ROWS` counts rows, not days. If a day is missing, the seven rows span more than seven days. A `RANGE` frame over the day number counts days:

    ```sql
    WITH daily AS (
      SELECT visited_on, SUM(amount) AS amt FROM sales GROUP BY visited_on)
    SELECT visited_on, amount, average_amount
    FROM (SELECT visited_on,
                 SUM(amt) OVER win AS amount,
                 ROUND(AVG(amt) OVER win, 2) AS average_amount
          FROM daily
          WINDOW win AS (ORDER BY julianday(visited_on)
                         RANGE BETWEEN 6 PRECEDING AND CURRENT ROW))
    WHERE visited_on >= (SELECT date(MIN(visited_on), '+6 days') FROM daily)
    ORDER BY visited_on;
    ```

    <!-- sql-result -->

    ```text
    visited_on | amount | average_amount
    2024-01-07 | 940 | 134.29
    2024-01-08 | 1035 | 147.86
    2024-01-09 | 1105 | 157.86
    ```

    SQLite allows `RANGE` with a numeric `ORDER BY` expression, so `julianday` turns dates into numbers. MySQL 8.4 accepts `RANGE BETWEEN INTERVAL 6 DAY PRECEDING AND CURRENT ROW` on a temporal column. PostgreSQL 18 accepts an interval offset such as `RANGE BETWEEN INTERVAL '6 days' PRECEDING AND CURRENT ROW` on a `date` or `timestamp` column.


## Running Totals

A running total is a window `SUM` with an `ORDER BY`. The frame decides what happens with ties.

### Problem 20: Last Person to Fit in the Bus

**LeetCode 1204, Last Person to Fit in the Bus (Medium).** People board a bus in order of `turn`. The bus carries at most 1000 kg. Return the name of the last person who can board, when people board one by one in turn order and boarding stops at the first person who does not fit.

```sql
DROP TABLE IF EXISTS bus_queue;
CREATE TABLE bus_queue (person_id INTEGER PRIMARY KEY, name TEXT, weight INTEGER, turn INTEGER);
INSERT INTO bus_queue VALUES
  (1, 'Ann', 250, 1), (2, 'Bob', 350, 2), (3, 'Cy', 400, 3),
  (4, 'Di', 200, 4), (5, 'Eli', 175, 5), (6, 'Fay', 500, 6);
```

??? example "Solution"

    ```sql
    SELECT name
    FROM (SELECT name, SUM(weight) OVER (ORDER BY turn) AS total FROM bus_queue)
    WHERE total <= 1000
    ORDER BY total DESC
    LIMIT 1;
    ```

    <!-- sql-result -->

    ```text
    name
    Cy
    ```

    `SUM(weight) OVER (ORDER BY turn)` is a running total in turn order. The totals are 250, 600, 1000, 1200, 1375, and 1875. Cy brings the total to exactly 1000, which fits (`<=`). Weights are positive, so the running total only grows. The rows with `total <= 1000` form a prefix of the queue, and the largest total in that prefix belongs to the last person. The default frame of `ORDER BY` is `RANGE ... CURRENT ROW`. It is correct here because `turn` is unique. See the next problem for what happens with ties.


### Problem 21: Running balance per account

**Not on LeetCode.** A `txn` table holds account transactions. Return each transaction with the running balance of its account in date order. Two transactions of account A fall on the same date. One amount is `NULL`.

```sql
DROP TABLE IF EXISTS txn;
CREATE TABLE txn (tx_id INTEGER PRIMARY KEY, acct TEXT, tx_date TEXT, amount INTEGER);
INSERT INTO txn VALUES
  (1, 'A', '2024-01-01', 100), (2, 'A', '2024-01-02', -30), (3, 'A', '2024-01-02', 50),
  (4, 'A', '2024-01-05', -20), (5, 'B', '2024-01-01', 200), (6, 'B', '2024-01-03', NULL),
  (7, 'B', '2024-01-04', -75);
```

??? example "Solution"

    ```sql
    SELECT acct, tx_id, amount,
           SUM(amount) OVER (PARTITION BY acct ORDER BY tx_date, tx_id
                             ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
    FROM txn
    ORDER BY acct, tx_date, tx_id;
    ```

    <!-- sql-result -->

    ```text
    acct | tx_id | amount | balance
    A | 1 | 100 | 100
    A | 2 | -30 | 70
    A | 3 | 50 | 120
    A | 4 | -20 | 100
    B | 5 | 200 | 200
    B | 6 | NULL | 200
    B | 7 | -75 | 125
    ```

    `PARTITION BY acct` restarts the total for each account. `ORDER BY tx_date, tx_id` gives a total order, and the `ROWS` frame adds one row at a time. `SUM` ignores the `NULL` amount, so the balance of transaction 6 stays at 200. If an account had only `NULL` amounts, `SUM` would return `NULL`. Now remove the tie breaker and the explicit frame:

    ```sql
    SELECT tx_id, tx_date,
           SUM(amount) OVER (PARTITION BY acct ORDER BY tx_date) AS default_frame_balance
    FROM txn
    WHERE acct = 'A'
    ORDER BY tx_id;
    ```

    <!-- sql-result -->

    ```text
    tx_id | tx_date | default_frame_balance
    1 | 2024-01-01 | 100
    2 | 2024-01-02 | 120
    3 | 2024-01-02 | 120
    4 | 2024-01-05 | 100
    ```

    The default frame is `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`. `RANGE` treats rows with the same `ORDER BY` value as peers and includes all peers in the total. Transactions 2 and 3 have the same date, so both show 120, and no row shows the balance after only the first of them. Always write `ROWS` and a unique order for a running total. See [window functions](01-relational-model-and-sql.md).


## Conditional Aggregation and Pivots

`SUM(CASE WHEN ...)` turns row values into columns and splits one total into several.

### Problem 22: Reformat Department Table

**LeetCode 1179, Reformat Department Table (Easy).** A `dept_rev` table has one row per department and month. Return one row per department with a column for each of the months January to April. Show `NULL` for a month without data.

```sql
DROP TABLE IF EXISTS dept_rev;
CREATE TABLE dept_rev (id INTEGER, revenue INTEGER, month TEXT);
INSERT INTO dept_rev VALUES
  (1, 8000, 'Jan'), (1, 9000, 'Mar'), (2, 9000, 'Feb'), (2, 3000, 'Apr'),
  (3, 1000, 'Jan'), (3, 500, 'Jan');
```

??? example "Solution"

    ```sql
    SELECT id,
           SUM(CASE WHEN month = 'Jan' THEN revenue END) AS jan_revenue,
           SUM(CASE WHEN month = 'Feb' THEN revenue END) AS feb_revenue,
           SUM(CASE WHEN month = 'Mar' THEN revenue END) AS mar_revenue,
           SUM(CASE WHEN month = 'Apr' THEN revenue END) AS apr_revenue
    FROM dept_rev
    GROUP BY id
    ORDER BY id;
    ```

    <!-- sql-result -->

    ```text
    id | jan_revenue | feb_revenue | mar_revenue | apr_revenue
    1 | 8000 | NULL | 9000 | NULL
    2 | NULL | 9000 | NULL | 3000
    3 | 1500 | NULL | NULL | NULL
    ```

    This is conditional aggregation. `CASE` without `ELSE` returns `NULL` for the other months. `SUM` ignores `NULL`, so each column adds only its own month. If a department has no row for a month, all inputs of that `SUM` are `NULL`, and `SUM` returns `NULL` (not 0), which matches the requirement. Department 3 has two January rows, and `SUM` adds them (1500). The columns are fixed in the query text. A pivot with an unknown number of columns needs dynamic SQL or the PostgreSQL `crosstab` function.


### Problem 23: Monthly Transactions I

**LeetCode 1193, Monthly Transactions I (Medium).** A `trans` table stores payments with a country, a state (`approved` or `declined`), an amount, and a date. For each month and country, return the number of transactions, the number of approved transactions, the total amount, and the total approved amount. One row has a `NULL` country.

```sql
DROP TABLE IF EXISTS trans;
CREATE TABLE trans (id INTEGER PRIMARY KEY, country TEXT, state TEXT, amount INTEGER,
                    trans_date TEXT);
INSERT INTO trans VALUES
  (1, 'US', 'approved', 100, '2024-01-05'), (2, 'US', 'declined', 50, '2024-01-20'),
  (3, 'US', 'approved', 200, '2024-02-02'), (4, 'DE', 'declined', 70, '2024-01-11'),
  (5, 'DE', 'declined', 30, '2024-01-12'), (6, NULL, 'approved', 10, '2024-01-30');
```

??? example "Solution"

    ```sql
    SELECT strftime('%Y-%m', trans_date) AS month, country,
           COUNT(*) AS trans_count,
           SUM(CASE WHEN state = 'approved' THEN 1 ELSE 0 END) AS approved_count,
           SUM(amount) AS trans_total_amount,
           SUM(CASE WHEN state = 'approved' THEN amount ELSE 0 END) AS approved_total_amount
    FROM trans
    GROUP BY strftime('%Y-%m', trans_date), country
    ORDER BY month, country;
    ```

    <!-- sql-result -->

    ```text
    month | country | trans_count | approved_count | trans_total_amount | approved_total_amount
    2024-01 | NULL | 1 | 1 | 10 | 10
    2024-01 | DE | 2 | 0 | 100 | 0
    2024-01 | US | 2 | 1 | 150 | 100
    2024-02 | US | 1 | 1 | 200 | 200
    ```

    The group key is the month text plus the country. `GROUP BY` puts the `NULL` country in its own group, and `ORDER BY` shows it first in SQLite. For the approved columns, `ELSE 0` makes the sum 0 for a group without approved rows (DE in January). Without `ELSE 0`, the column would show `NULL`. The month function is a dialect difference:

    MySQL 8.4 form (not run here):

    <!-- sql-skip: mysql -->

    ```sql
    SELECT DATE_FORMAT(trans_date, '%Y-%m') AS month, country, COUNT(*) AS trans_count
    FROM trans
    GROUP BY DATE_FORMAT(trans_date, '%Y-%m'), country;
    ```

    PostgreSQL 18 form (not run here):

    <!-- sql-skip: postgresql -->

    ```sql
    SELECT to_char(trans_date, 'YYYY-MM') AS month, country, COUNT(*) AS trans_count
    FROM trans
    GROUP BY to_char(trans_date, 'YYYY-MM'), country;
    ```

    MySQL formats with `DATE_FORMAT` and `%Y-%m`. PostgreSQL formats with `to_char` and the pattern `YYYY-MM`. Both need `trans_date` to be a date or timestamp column.


## Ratios and Rates

A ratio has two traps: integer division truncates, and a zero divisor either fails or returns `NULL`.

### Problem 24: Confirmation Rate

**LeetCode 1934, Confirmation Rate (Medium).** A `signups` table lists users. A `confirmations` table lists the confirmation attempts of users, each with an action `confirmed` or `timeout`. Return each user with the confirmation rate: confirmed attempts divided by all attempts, rounded to two decimals. A user without any attempt has the rate 0.

```sql
DROP TABLE IF EXISTS confirmations;
DROP TABLE IF EXISTS signups;
CREATE TABLE signups (user_id INTEGER PRIMARY KEY);
CREATE TABLE confirmations (user_id INTEGER, action TEXT);
INSERT INTO signups VALUES (1), (2), (3), (4), (5);
INSERT INTO confirmations VALUES
  (1, 'confirmed'), (1, 'timeout'), (2, 'confirmed'),
  (4, 'timeout'), (4, 'timeout'), (4, 'confirmed'), (5, 'timeout');
```

??? example "Solution"

    ```sql
    SELECT s.user_id,
           ROUND(COALESCE(1.0 * SUM(CASE WHEN c.action = 'confirmed' THEN 1 ELSE 0 END)
                          / NULLIF(COUNT(c.action), 0), 0), 2) AS confirmation_rate
    FROM signups s
    LEFT JOIN confirmations c ON c.user_id = s.user_id
    GROUP BY s.user_id
    ORDER BY s.user_id;
    ```

    <!-- sql-result -->

    ```text
    user_id | confirmation_rate
    1 | 0.5
    2 | 1.0
    3 | 0.0
    4 | 0.33
    5 | 0.0
    ```

    Three details decide the answer. First, the `LEFT JOIN` keeps user 3, who has no attempt. Second, `COUNT(c.action)` counts only non-`NULL` values, so user 3 has a count of 0 and `NULLIF(0, 0)` turns the divisor into `NULL`. The division then returns `NULL`, and `COALESCE` turns it into 0. This avoids a divide-by-zero error in databases that raise one. Third, `1.0 *` forces decimal arithmetic. Integer division truncates:

    ```sql
    SELECT 1 / 2 AS integer_division, 1.0 / 2 AS decimal_division;
    ```

    <!-- sql-result -->

    ```text
    integer_division | decimal_division
    0 | 0.5
    ```

    In SQLite and PostgreSQL, `1 / 2` is 0 because both operands are integers. In MySQL, `/` always returns a decimal (`1 / 2` is 0.5), and `DIV` is the integer division (MySQL 8.4 arithmetic functions page). A shorter form averages a comparison. It works in SQLite and MySQL, where a comparison is a number, but not in PostgreSQL, where `AVG` does not accept `boolean`:

    ```sql
    SELECT s.user_id,
           ROUND(COALESCE(AVG(c.action = 'confirmed'), 0), 2) AS confirmation_rate
    FROM signups s
    LEFT JOIN confirmations c ON c.user_id = s.user_id
    GROUP BY s.user_id
    ORDER BY s.user_id;
    ```

    <!-- sql-result -->

    ```text
    user_id | confirmation_rate
    1 | 0.5
    2 | 1.0
    3 | 0.0
    4 | 0.33
    5 | 0.0
    ```

    For user 3, the joined row has `c.action` of `NULL`. The comparison returns `NULL`, `AVG` ignores it and returns `NULL`, and `COALESCE` gives 0.


### Problem 25: Trips and Users

**LeetCode 262, Trips and Users (Hard).** A `trips` table has client and driver ids, a status (`completed`, `cancelled_by_driver`, or `cancelled_by_client`), and a request date. A `users` table marks each user as banned or not. For each day from 2024-10-01 to 2024-10-03, return the cancellation rate: cancelled trips divided by all trips, counting only trips where both the client and the driver are not banned. Skip days without such trips.

```sql
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS users_t;
CREATE TABLE users_t (users_id INTEGER PRIMARY KEY, banned TEXT, role TEXT);
CREATE TABLE trips (id INTEGER PRIMARY KEY, client_id INTEGER, driver_id INTEGER,
                    status TEXT, request_at TEXT);
INSERT INTO users_t VALUES
  (1, 'No', 'client'), (2, 'Yes', 'client'), (3, 'No', 'client'),
  (4, 'No', 'driver'), (5, 'No', 'driver'), (6, 'Yes', 'driver');
INSERT INTO trips VALUES
  (1, 1, 4, 'completed', '2024-10-01'), (2, 2, 4, 'cancelled_by_driver', '2024-10-01'),
  (3, 3, 5, 'cancelled_by_client', '2024-10-01'), (4, 1, 5, 'completed', '2024-10-02'),
  (5, 3, 4, 'completed', '2024-10-02'), (6, 1, 6, 'cancelled_by_driver', '2024-10-02'),
  (7, 2, 6, 'completed', '2024-10-03'), (8, 1, 4, 'cancelled_by_driver', '2024-10-04');
```

??? example "Solution"

    ```sql
    SELECT t.request_at AS day,
           ROUND(1.0 * SUM(CASE WHEN t.status <> 'completed' THEN 1 ELSE 0 END) / COUNT(*), 2)
             AS cancellation_rate
    FROM trips t
    JOIN users_t c ON c.users_id = t.client_id AND c.banned = 'No'
    JOIN users_t d ON d.users_id = t.driver_id AND d.banned = 'No'
    WHERE t.request_at BETWEEN '2024-10-01' AND '2024-10-03'
    GROUP BY t.request_at
    ORDER BY t.request_at;
    ```

    <!-- sql-result -->

    ```text
    day | cancellation_rate
    2024-10-01 | 0.5
    2024-10-02 | 0.0
    ```

    The two inner joins to `users_t` act as filters: a trip survives only if both its client and its driver are not banned. The join order does not matter for the result. Trip 2 (banned client) and trip 6 (banned driver) disappear. All trips of 2024-10-03 involve a banned user, so that day has no group and does not appear. Because the join removes the rows before `GROUP BY`, `COUNT(*)` is never 0 in a group, and no `NULLIF` is needed. Day 1 has two trips and one cancellation (0.5). Day 2 has two completed trips (0.0). The filter on the date range uses the text order of ISO dates, which equals the date order.


### Problem 26: Click-through rate with zero impressions

**Not on LeetCode.** An `ad_stats` table holds impressions and clicks per campaign. Return the click-through rate in percent, rounded to two decimals. A campaign with no impressions shows `NULL`, not an error.

```sql
DROP TABLE IF EXISTS ad_stats;
CREATE TABLE ad_stats (campaign TEXT, impressions INTEGER, clicks INTEGER);
INSERT INTO ad_stats VALUES ('Spring', 1000, 50), ('Summer', 0, 0), ('Fall', 3, 1);
```

??? example "Solution"

    ```sql
    SELECT campaign, impressions, clicks,
           ROUND(100.0 * clicks / NULLIF(impressions, 0), 2) AS ctr_percent
    FROM ad_stats
    ORDER BY campaign DESC;
    ```

    <!-- sql-result -->

    ```text
    campaign | impressions | clicks | ctr_percent
    Summer | 0 | 0 | NULL
    Spring | 1000 | 50 | 5.0
    Fall | 3 | 1 | 33.33
    ```

    `NULLIF(impressions, 0)` returns `NULL` when `impressions` is 0, and any division by `NULL` is `NULL`. This works in every database. Without `NULLIF`, PostgreSQL raises the error `division by zero`, and MySQL and SQLite return `NULL` for `x / 0` (MySQL 8.4 arithmetic functions page; checked in SQLite 3.51 by running it). Do not rely on that, because a MySQL `INSERT` or `UPDATE` can raise an error in strict mode. The factor `100.0` makes the arithmetic decimal. The integer form loses the fraction:

    ```sql
    SELECT campaign, clicks * 100 / impressions AS ctr_integer
    FROM ad_stats
    WHERE impressions > 0
    ORDER BY campaign DESC;
    ```

    <!-- sql-result -->

    ```text
    campaign | ctr_integer
    Spring | 5
    Fall | 33
    ```

    For Fall, 1 click in 3 impressions is 33.33 percent, but `1 * 100 / 3` is 33 in SQLite and PostgreSQL. In MySQL it is 33.3333. Ask which rounding the report needs. A rate over a group also needs care: average the sums (`SUM(clicks) / SUM(impressions)`), not the average of the per-row rates.


## Hierarchies with Recursive CTEs

A recursive CTE walks a parent-child table level by level.

### Problem 27: All reports of a manager (recursive)

**Not on LeetCode.** An `org` table stores employees and manager ids. Return every direct and indirect report of the employee with id 1, with the depth below that employee and the management path.

```sql
DROP TABLE IF EXISTS org;
CREATE TABLE org (emp_id INTEGER PRIMARY KEY, name TEXT, manager_id INTEGER);
INSERT INTO org VALUES
  (1, 'Asha', NULL), (2, 'Bala', 1), (3, 'Chen', 1), (4, 'Dev', 2), (5, 'Esha', 2),
  (6, 'Farid', 4), (7, 'Gita', 3), (8, 'Hari', NULL), (9, 'Indu', 8);
```

??? example "Solution"

    ```sql
    WITH RECURSIVE tree(emp_id, name, depth, path) AS (
      SELECT emp_id, name, 0, name FROM org WHERE emp_id = 1
      UNION ALL
      SELECT o.emp_id, o.name, t.depth + 1, t.path || ' > ' || o.name
      FROM org o
      JOIN tree t ON o.manager_id = t.emp_id)
    SELECT name, depth, path FROM tree ORDER BY path;
    ```

    <!-- sql-result -->

    ```text
    name | depth | path
    Asha | 0 | Asha
    Bala | 1 | Asha > Bala
    Dev | 2 | Asha > Bala > Dev
    Farid | 3 | Asha > Bala > Dev > Farid
    Esha | 2 | Asha > Bala > Esha
    Chen | 1 | Asha > Chen
    Gita | 2 | Asha > Chen > Gita
    ```

    A recursive CTE has an anchor query (the start row) and a recursive query that joins the table with the rows found in the previous step. The database repeats the recursive step until it returns no new row. Here, step 1 finds Bala and Chen, step 2 finds Dev, Esha, and Gita, and step 3 finds Farid. Hari and Indu form another tree and never appear. If the data can contain a cycle (an employee who reports to their own report), the query would run forever: add a `depth < 20` condition, or use `UNION` instead of `UNION ALL` when the rows have no path column. PostgreSQL has `CYCLE` clauses for this. The `||` operator concatenates in SQLite and PostgreSQL. In MySQL 8.4, `||` is a logical OR by default, so write `CONCAT(t.path, ' > ', o.name)`. MySQL also fixes the type of a recursive CTE column from the anchor row, so `path` is as wide as `'Asha'`, and a longer path raises a `Data too long` error in strict mode. Cast the anchor column, for example `CAST(name AS CHAR(200))`. Recursive CTEs are explained in [CTEs and recursive CTEs](01-relational-model-and-sql.md).


## Latest and First Row per Group

Choose between a correlated subquery, a row-value `IN` on `MIN` or `MAX`, and a window `ROW_NUMBER` or `RANK`.

### Problem 28: Product Price at a Given Date

**LeetCode 1164, Product Price at a Given Date (Medium).** A `price_chg` table records price changes of products. Each product costs 10 until its first change. A `prod_list` table lists all products. Return the price of every product on 2024-02-01. A change on that date counts.

```sql
DROP TABLE IF EXISTS price_chg;
DROP TABLE IF EXISTS prod_list;
CREATE TABLE prod_list (product_id INTEGER PRIMARY KEY);
CREATE TABLE price_chg (product_id INTEGER, new_price INTEGER, change_date TEXT);
INSERT INTO prod_list VALUES (1), (2), (3), (4);
INSERT INTO price_chg VALUES
  (1, 20, '2024-01-10'), (1, 50, '2024-02-01'), (1, 30, '2024-03-05'),
  (2, 100, '2024-03-10'), (3, 60, '2024-02-01');
```

??? example "Solution"

    ```sql
    SELECT p.product_id, COALESCE(c.new_price, 10) AS price
    FROM prod_list p
    LEFT JOIN (SELECT product_id, new_price,
                      ROW_NUMBER() OVER (PARTITION BY product_id ORDER BY change_date DESC) AS rn
               FROM price_chg
               WHERE change_date <= '2024-02-01') c
           ON c.product_id = p.product_id AND c.rn = 1
    ORDER BY p.product_id;
    ```

    <!-- sql-result -->

    ```text
    product_id | price
    1 | 50
    2 | 10
    3 | 60
    4 | 10
    ```

    The subquery keeps only changes up to the date and numbers them from the latest. The row with `rn = 1` is the price that applies. The `LEFT JOIN` from `prod_list` keeps products without a change up to the date (products 2 and 4), and `COALESCE` gives them the default 10. Filtering with `<=` includes a change on the exact date (product 3). A correlated subquery gives the same result:

    ```sql
    SELECT p.product_id,
           COALESCE((SELECT c.new_price
                     FROM price_chg c
                     WHERE c.product_id = p.product_id AND c.change_date <= '2024-02-01'
                     ORDER BY c.change_date DESC
                     LIMIT 1), 10) AS price
    FROM prod_list p
    ORDER BY p.product_id;
    ```

    <!-- sql-result -->

    ```text
    product_id | price
    1 | 50
    2 | 10
    3 | 60
    4 | 10
    ```

    An index on `(product_id, change_date)` makes the correlated form a short index lookup for each product. If two changes of one product could have the same date, `ORDER BY` needs a tie breaker. See [indexing](03-indexing.md).


### Problem 29: Product Sales Analysis III

**LeetCode 1070, Product Sales Analysis III (Medium).** A `sales_a` table has product sales with a year, a quantity, and a price. For each product, return all sales of the first year in which the product was sold.

```sql
DROP TABLE IF EXISTS sales_a;
CREATE TABLE sales_a (sale_id INTEGER PRIMARY KEY, product_id INTEGER, sale_year INTEGER,
                      quantity INTEGER, price INTEGER);
INSERT INTO sales_a VALUES
  (1, 100, 2008, 10, 5000), (2, 100, 2009, 12, 5000), (3, 200, 2011, 15, 9000),
  (4, 100, 2008, 3, 5000), (5, 300, 2010, 5, 100), (6, 300, 2011, 8, 100);
```

??? example "Solution"

    ```sql
    SELECT product_id, sale_year AS first_year, quantity, price
    FROM sales_a
    WHERE (product_id, sale_year) IN (SELECT product_id, MIN(sale_year)
                                      FROM sales_a
                                      GROUP BY product_id)
    ORDER BY sale_id;
    ```

    <!-- sql-result -->

    ```text
    product_id | first_year | quantity | price
    100 | 2008 | 10 | 5000
    200 | 2011 | 15 | 9000
    100 | 2008 | 3 | 5000
    300 | 2010 | 5 | 100
    ```

    The subquery returns one (product, first year) pair for each product. The outer query keeps every sale that matches a pair, so product 100 returns both 2008 rows. A window form uses `RANK` (not `ROW_NUMBER`, which would keep only one of the rows of the first year):

    ```sql
    SELECT product_id, sale_year AS first_year, quantity, price
    FROM (SELECT *, RANK() OVER (PARTITION BY product_id ORDER BY sale_year) AS rk
          FROM sales_a)
    WHERE rk = 1
    ORDER BY sale_id;
    ```

    <!-- sql-result -->

    ```text
    product_id | first_year | quantity | price
    100 | 2008 | 10 | 5000
    200 | 2011 | 15 | 9000
    100 | 2008 | 3 | 5000
    300 | 2010 | 5 | 100
    ```


### Problem 30: Immediate Food Delivery II

**LeetCode 1174, Immediate Food Delivery II (Medium).** A `delivery` table stores orders with the order date and the preferred delivery date. An order is immediate if both dates are equal. Return the percentage of customers whose first order (earliest order date) is immediate, rounded to two decimals.

```sql
DROP TABLE IF EXISTS delivery;
CREATE TABLE delivery (delivery_id INTEGER PRIMARY KEY, customer_id INTEGER,
                       order_date TEXT, preferred_date TEXT);
INSERT INTO delivery VALUES
  (1, 1, '2024-01-01', '2024-01-02'), (2, 1, '2024-01-05', '2024-01-05'),
  (3, 2, '2024-01-02', '2024-01-02'), (4, 3, '2024-01-03', '2024-01-03'),
  (5, 4, '2024-01-04', '2024-01-10');
```

??? example "Solution"

    ```sql
    SELECT ROUND(100.0 * SUM(CASE WHEN order_date = preferred_date THEN 1 ELSE 0 END)
                 / COUNT(*), 2) AS immediate_percentage
    FROM delivery
    WHERE (customer_id, order_date) IN (SELECT customer_id, MIN(order_date)
                                        FROM delivery
                                        GROUP BY customer_id);
    ```

    <!-- sql-result -->

    ```text
    immediate_percentage
    50.0
    ```

    The subquery finds the first order date of each customer, so the outer query sees one row per customer. Customer 1 first ordered a scheduled delivery. Their later immediate order does not count. Customers 2 and 3 count as immediate, customer 4 does not: 2 of 4 is 50.00 percent. The query assumes that a customer has at most one order per date. If not, add a tie breaker with `ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY order_date, delivery_id)`.


## Retention

A retention query finds the first event of an entity and then checks for a later event at a fixed distance.

### Problem 31: Game Play Analysis IV

**LeetCode 550, Game Play Analysis IV (Medium).** An `activity` table has one row per player and login date. Return the fraction of players who logged in on the day after their first login, rounded to two decimals.

```sql
DROP TABLE IF EXISTS activity;
CREATE TABLE activity (player_id INTEGER, event_date TEXT, games_played INTEGER,
                       PRIMARY KEY (player_id, event_date));
INSERT INTO activity VALUES
  (1, '2024-03-01', 5), (1, '2024-03-02', 6), (2, '2024-03-01', 3), (2, '2024-03-03', 1),
  (3, '2024-03-02', 7), (4, '2024-03-04', 2), (4, '2024-03-05', 4);
```

??? example "Solution"

    ```sql
    WITH first_login AS (
      SELECT player_id, MIN(event_date) AS first_date FROM activity GROUP BY player_id)
    SELECT ROUND(1.0 * COUNT(a.player_id) / COUNT(*), 2) AS fraction
    FROM first_login f
    LEFT JOIN activity a
      ON a.player_id = f.player_id AND a.event_date = date(f.first_date, '+1 day');
    ```

    <!-- sql-result -->

    ```text
    fraction
    0.5
    ```

    The CTE finds the first login of each player. The `LEFT JOIN` looks for a row on the next calendar day. `COUNT(*)` counts all players, and `COUNT(a.player_id)` counts the players with a match. Players 1 and 4 returned on day 2, players 2 and 3 did not: 2 of 4 is 0.50. The primary key `(player_id, event_date)` guarantees that the join finds at most one row, so a player counts once. Player 2 logged in on day 3, which is not the next day. This pattern generalizes to day-7 retention: change `'+1 day'` to `'+7 day'`. The same query in PostgreSQL uses `f.first_date + 1` (the column must have the type `date`, not `text` as in this SQLite table), and in MySQL `DATE_ADD(f.first_date, INTERVAL 1 DAY)`:

    MySQL 8.4 form (not run here):

    <!-- sql-skip: mysql -->

    ```sql
    SELECT ROUND(COUNT(a.player_id) / COUNT(*), 2) AS fraction
    FROM (SELECT player_id, MIN(event_date) AS first_date FROM activity GROUP BY player_id) f
    LEFT JOIN activity a
      ON a.player_id = f.player_id
     AND a.event_date = DATE_ADD(f.first_date, INTERVAL 1 DAY);
    ```

    PostgreSQL 18 form (not run here):

    <!-- sql-skip: postgresql -->

    ```sql
    SELECT ROUND(1.0 * COUNT(a.player_id) / COUNT(*), 2) AS fraction
    FROM (SELECT player_id, MIN(event_date) AS first_date FROM activity GROUP BY player_id) f
    LEFT JOIN activity a
      ON a.player_id = f.player_id AND a.event_date = f.first_date + 1;
    ```


## Medians

SQL has no portable median function. Number the rows and pick the middle one or two.

### Problem 32: Median salary per department

**Not on LeetCode.** It is a variant of LeetCode 569, Median Employee Salary (Hard, premium). A `sal_med` table holds salaries per department. Return the median salary of each department. For an even number of values, the median is the mean of the two middle values. Ignore `NULL` salaries. The data has an even group, an odd group with a `NULL`, a single row, and a tie.

```sql
DROP TABLE IF EXISTS sal_med;
CREATE TABLE sal_med (emp_id INTEGER PRIMARY KEY, dept TEXT, salary INTEGER);
INSERT INTO sal_med VALUES
  (1, 'A', 10), (2, 'A', 20), (3, 'A', 30), (4, 'A', 40),
  (5, 'B', 5), (6, 'B', 15), (7, 'B', 100), (8, 'B', NULL),
  (9, 'C', 7), (10, 'D', 50), (11, 'D', 50);
```

??? example "Solution"

    ```sql
    WITH r AS (
      SELECT dept, salary,
             ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary) AS rn,
             COUNT(*) OVER (PARTITION BY dept) AS cnt
      FROM sal_med
      WHERE salary IS NOT NULL)
    SELECT dept, AVG(salary) AS median
    FROM r
    WHERE rn IN ((cnt + 1) / 2, (cnt + 2) / 2)
    GROUP BY dept
    ORDER BY dept;
    ```

    <!-- sql-result -->

    ```text
    dept | median
    A | 25.0
    B | 15.0
    C | 7.0
    D | 50.0
    ```

    Number the values of each department in order and count them. For an odd count `cnt`, the middle row is `(cnt + 1) / 2`, and `(cnt + 2) / 2` gives the same row, so the `AVG` of one row is that value. For an even count, the two expressions give the two middle rows (for `cnt` = 4: rows 2 and 3), and `AVG` returns their mean (25.0 for department A). This depends on integer division, which truncates in SQLite and PostgreSQL. In MySQL, `/` returns a decimal (5 / 2 is 2.5). For `cnt` = 4, the expressions give 2.5 and 3.0, so only row 3 matches, and the query returns a wrong median with no error. Write `DIV`. The `NULL` filter belongs in the CTE, so that `COUNT(*)` does not count the `NULL` row. PostgreSQL has no `median` function, but its ordered-set aggregate `percentile_cont` computes it. A department whose salaries are all `NULL` produces no row in these solutions, because the `NULL` filter removes every row of the department. Use a `LEFT JOIN` from a department table if the report needs a `NULL` row for it.

    MySQL 8.4 form (not run here):

    <!-- sql-skip: mysql -->

    ```sql
    WITH r AS (
      SELECT dept, salary,
             ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary) AS rn,
             COUNT(*) OVER (PARTITION BY dept) AS cnt
      FROM sal_med
      WHERE salary IS NOT NULL)
    SELECT dept, AVG(salary) AS median
    FROM r
    WHERE rn IN ((cnt + 1) DIV 2, (cnt + 2) DIV 2)
    GROUP BY dept;
    ```

    PostgreSQL 18 form (not run here):

    <!-- sql-skip: postgresql -->

    ```sql
    SELECT dept, percentile_cont(0.5) WITHIN GROUP (ORDER BY salary) AS median
    FROM sal_med
    GROUP BY dept;
    ```

    `percentile_cont` ignores `NULL` inputs and interpolates between the two middle values (PostgreSQL 18 aggregate functions page). MySQL 8.4 has no median function. This problem is a per-department variant of LeetCode 569 (Median Employee Salary, Hard, premium).


## Dialect Differences

The page solutions use SQLite. This table shows the forms that differ in MySQL 8.4 and PostgreSQL 18. Each entry comes from the vendor documentation pages listed in Further Reading, except where the last column says otherwise.

| Task | SQLite 3.51 | MySQL 8.4 | PostgreSQL 18 |
|---|---|---|---|
| Days between two dates | `julianday(a) - julianday(b)` (a real number) | `DATEDIFF(a, b)` (a minus b, in days) | `a - b` for `date` values (an integer) |
| Add one day | `date(d, '+1 day')` | `DATE_ADD(d, INTERVAL 1 DAY)` | `d + 1` for `date`, or `d + INTERVAL '1 day'` (gives a `timestamp`) |
| Month label | `strftime('%Y-%m', d)` | `DATE_FORMAT(d, '%Y-%m')` | `to_char(d, 'YYYY-MM')` |
| Start of month | `date(d, 'start of month')` | `DATE_FORMAT(d, '%Y-%m-01')` (text) | `date_trunc('month', d)` |
| String aggregation | `group_concat(x, ',')`, alias `string_agg(x, ',')` | `GROUP_CONCAT(x ORDER BY x SEPARATOR ',')` | `string_agg(x, ',' ORDER BY x)` |
| Limit and offset | `LIMIT n OFFSET m`, or `LIMIT m, n` | `LIMIT n OFFSET m`, or `LIMIT m, n` | `LIMIT n OFFSET m`, or `OFFSET m ROWS FETCH FIRST n ROWS ONLY` |
| Default for `NULL` | `COALESCE(a, b)`, `IFNULL(a, b)` | `COALESCE(a, b)`, `IFNULL(a, b)` | `COALESCE(a, b)` (no `IFNULL`) |
| Integer division | `1 / 2` is 0 | `1 / 2` is 0.5. `1 DIV 2` is 0 | `1 / 2` is 0 |
| Division by zero | `NULL` | `NULL` in a `SELECT`. `INSERT` or `UPDATE` raises an error when `ERROR_FOR_DIVISION_BY_ZERO` and strict mode are on (both are in the 8.4 default) | Error `division by zero` |
| Concatenate | `a \|\| b` | `CONCAT(a, b)`. `\|\|` is a logical OR (deprecated) unless `PIPES_AS_CONCAT` is set | `a \|\| b` |
| `AVG` of a comparison | Works (a comparison is 0, 1, or `NULL`) | Works | Error: `avg` does not accept `boolean`. Use `SUM(CASE WHEN ... THEN 1 ELSE 0 END)` |
| `NULL` in `ORDER BY` | `NULL` is the smallest: first in `ASC`, last in `DESC` | The same | `NULL` is the largest: last in `ASC`, first in `DESC`. Add `NULLS FIRST` or `NULLS LAST` |
| Upsert | `INSERT ... ON CONFLICT (k) DO UPDATE SET c = excluded.c` | `INSERT ... ON DUPLICATE KEY UPDATE c = new.c` (with a row alias `AS new`) | `INSERT ... ON CONFLICT (k) DO UPDATE SET c = EXCLUDED.c` |
| `DELETE` using the same table | A subquery on the same table works | A subquery on the same table is an error. Use `DELETE a FROM t a JOIN t b ...` | `DELETE FROM t a USING t b WHERE ...` |
| Median | No built-in function | No built-in function | `percentile_cont(0.5) WITHIN GROUP (ORDER BY x)` |
| `FULL OUTER JOIN` | Yes (3.39 and later) | No. Combine `LEFT JOIN` and `RIGHT JOIN` with `UNION` | Yes |

Recommendation: in an interview, ask which database the interviewer uses, and then write the functions of that database. Write standard SQL where it exists (`COALESCE`, `CASE`, `ROW_NUMBER`, `CTE`), because it runs on all three products.

## Common Mistakes

| Mistake | Why it fails | Fix |
|---|---|---|
| `NOT IN` with a subquery that can return `NULL` | `x NOT IN (1, NULL)` is never true, so the query returns no row | Use `NOT EXISTS`, or filter `NULL` in the subquery |
| A `WHERE` filter on the right table of a `LEFT JOIN` | The filter removes the `NULL`-extended rows, so the join acts as an inner join | Move the condition to the `ON` clause |
| `COUNT(*)` after a `LEFT JOIN` | It counts the one `NULL`-extended row of a group without matches | `COUNT(right.key)` |
| `ROW_NUMBER` where ties must stay | It breaks a tie arbitrarily and drops rows | `RANK` or `DENSE_RANK` |
| `RANK` for "Nth distinct value" | `RANK` leaves gaps after ties | `DENSE_RANK` |
| Running total without a frame or a unique order | The default `RANGE` frame adds all peers of the current row | `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` and a unique `ORDER BY` |
| Integer division in a ratio | `1 / 2` is 0 in SQLite and PostgreSQL | Multiply by `1.0` first |
| Divide by zero | PostgreSQL raises an error. SQLite and MySQL return `NULL` in a `SELECT`, but a strict-mode MySQL `INSERT` or `UPDATE` raises an error | `NULLIF(divisor, 0)` |
| Comparing neighbor rows with `LAG` when ids or dates have gaps | The previous row is not the previous id or day | Join on the exact id or date, or use the island technique |
| `GROUP BY` on a name instead of the key | Two entities with the same name merge | Group by the key and select the name |
| Selecting a column that is not in `GROUP BY` | SQLite returns an arbitrary value. MySQL 8.4 raises an error by default (`ONLY_FULL_GROUP_BY` is on) and returns an arbitrary value only if the mode is off. PostgreSQL raises an error | Aggregate it or add it to `GROUP BY` |
| Wrong empty-result shape | `LIMIT 1` returns zero rows, an aggregate returns one `NULL` row | Wrap the query in an aggregate or a scalar subquery, as the problem needs |
| `DELETE` without a check | A wrong condition deletes valid rows | Run the matching `SELECT` first, use a transaction, and check the row count |

## Quick Reference Card

- Output first: columns, grain, and the empty-result shape.
- Keep unmatched rows: `LEFT JOIN`. Keep only unmatched rows: `NOT EXISTS`. Never `NOT IN` over a nullable column.
- One row per group: `GROUP BY` and `HAVING`. Every row plus a group value: a window function.
- Top-N per group: `DENSE_RANK() OVER (PARTITION BY g ORDER BY v DESC) <= N`. Pick `ROW_NUMBER`, `RANK`, or `DENSE_RANK` from the tie rule.
- Nth highest: `DENSE_RANK`, and an outer `MAX` to return `NULL` when it does not exist.
- Consecutive rows: `id - ROW_NUMBER() OVER (ORDER BY id)` is constant in an island. Group by it.
- Running total: `SUM(x) OVER (PARTITION BY g ORDER BY k ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)`.
- Moving average: a `ROWS` frame counts rows, a `RANGE` frame counts values (days).
- Pivot: `SUM(CASE WHEN col = 'v' THEN x END)` per output column.
- Ratio: `1.0 * a / NULLIF(b, 0)`. Average the sums, not the ratios.
- Latest row per group: `ROW_NUMBER() OVER (PARTITION BY g ORDER BY ts DESC) = 1`, or a row-value `IN` on `MAX`.
- Recursive CTE: anchor, `UNION ALL`, recursive step, and a guard against cycles.
- Median: `ROW_NUMBER` and `COUNT` per group, rows `(cnt + 1) / 2` and `(cnt + 2) / 2` (use `DIV` in MySQL).
- Dates: check the dialect table before you write `DATEDIFF`.

## Further Reading

- [SQLite: Window functions](https://www.sqlite.org/windowfunctions.html). Frames, `RANGE` with numeric offsets, the default frame, and the ranking functions.
- [SQLite: Date and time functions](https://www.sqlite.org/lang_datefunc.html). `date`, `julianday`, `strftime`, and the modifiers.
- [SQLite: Built-in aggregate functions](https://www.sqlite.org/lang_aggfunc.html). `group_concat`, `string_agg`, and `ORDER BY` inside an aggregate.
- [SQLite: Expressions](https://www.sqlite.org/lang_expr.html). Integer division and the `IS` operator.
- [MySQL 8.4: Date and time functions](https://dev.mysql.com/doc/refman/8.4/en/date-and-time-functions.html). `DATEDIFF`, `DATE_ADD`, `DATE_FORMAT`.
- [MySQL 8.4: Arithmetic operators](https://dev.mysql.com/doc/refman/8.4/en/arithmetic-functions.html). `/`, `DIV`, and division by zero.
- [MySQL 8.4: DELETE](https://dev.mysql.com/doc/refman/8.4/en/delete.html). The same-table subquery rule and the multi-table form.
- [MySQL 8.4: SELECT](https://dev.mysql.com/doc/refman/8.4/en/select.html). `LIMIT` forms and local variables in stored programs.
- [MySQL 8.4: Window function frame specification](https://dev.mysql.com/doc/refman/8.4/en/window-functions-frames.html). `RANGE` with `INTERVAL` and the default frame.
- [MySQL 8.4: Aggregate functions](https://dev.mysql.com/doc/refman/8.4/en/aggregate-functions.html). `GROUP_CONCAT` and `group_concat_max_len`.
- [MySQL 8.4: Flow control functions](https://dev.mysql.com/doc/refman/8.4/en/flow-control-functions.html). `IFNULL` and `NULLIF`.
- [PostgreSQL 18: Date/time functions and operators](https://www.postgresql.org/docs/current/functions-datetime.html). `date - date`, `date + integer`, `date_trunc`.
- [PostgreSQL 18: Aggregate functions](https://www.postgresql.org/docs/current/functions-aggregate.html). `string_agg`, `percentile_cont`, and the argument types of `avg`.
- [PostgreSQL 18: SELECT](https://www.postgresql.org/docs/current/sql-select.html). `LIMIT`, `FETCH FIRST`, and the default `NULL` order.
- [PostgreSQL 18: Window function calls](https://www.postgresql.org/docs/current/sql-expressions.html). `RANGE` with an interval offset and the default frame.
- [LeetCode database problem list](https://leetcode.com/problemset/database/). The numbers, titles, and difficulties that this page uses. Some problems are premium.
- [Relational model and SQL](01-relational-model-and-sql.md), [indexing](03-indexing.md), and [query execution and tuning](05-query-execution-and-tuning.md). The theory and the cost of the queries on this page.
