# MySQL Dialect

SQM supports MySQL parsing, rendering, validation, DSL/codegen reachability, and downstream runtime coverage for the current delivered MySQL query and DML scope.

This page is a practical guide to the MySQL syntax currently supported by the framework.

## Overview

Current MySQL support includes:

- MySQL query syntax such as:
  - null-safe equality with `<=>`
  - regex predicates with `REGEXP` and `RLIKE`
  - locking modifiers such as `FOR SHARE`, `NOWAIT`, and `SKIP LOCKED`
  - `GROUP BY ... WITH ROLLUP`
  - selected MySQL built-in function coverage
- MySQL DML extensions such as:
  - `INSERT IGNORE`
  - `INSERT ... ON DUPLICATE KEY UPDATE`
  - `REPLACE INTO`
  - joined `UPDATE`
  - qualified joined-`UPDATE` assignment targets
  - canonical `DELETE FROM ... USING ... JOIN ...`
  - optimizer hints and index hints in the shipped contexts
  - `STRAIGHT_JOIN`
- optional MySQL semantic validation for conflicting index-hint combinations through `sqm-validate-mysql`

## Parse And Render

```java
var parseCtx = ParseContext.of(new MySqlSpecs());
var renderCtx = RenderContext.of(new MySqlDialect());

var statement = parseCtx.parse(Statement.class, """
    UPDATE users AS u USE INDEX (idx_users_name)
    INNER JOIN orders AS o FORCE INDEX FOR JOIN (idx_orders_user)
      ON u.id = o.user_id
    SET u.name = 'alice'
    WHERE o.state = 'closed'
    """).value();

var sql = renderCtx.render(statement).sql();
System.out.println(sql);
```

## Query Examples

Null-safe comparison:

```sql
SELECT id
FROM users
WHERE email <=> 'a@example.com'
```

Regex predicate:

```sql
SELECT id
FROM users
WHERE name REGEXP '^al'
```

Rollup:

```sql
SELECT role, COUNT(*)
FROM users
GROUP BY role WITH ROLLUP
```

## DML Examples

`INSERT IGNORE`:

```sql
INSERT IGNORE INTO users (id, name)
VALUES (1, 'alice')
```

`ON DUPLICATE KEY UPDATE`:

```sql
INSERT INTO users (id, name)
VALUES (1, 'alice')
ON DUPLICATE KEY UPDATE name = 'alice2'
```

`REPLACE INTO`:

```sql
REPLACE INTO users (id, name)
VALUES (1, 'alice')
```

Joined `UPDATE`:

```sql
UPDATE users AS u
INNER JOIN orders AS o ON u.id = o.user_id
SET u.name = 'alice'
WHERE o.state = 'closed'
```

Joined `DELETE`:

```sql
DELETE FROM users
USING users
INNER JOIN orders ON users.id = orders.user_id
WHERE orders.state = 'closed'
```

## Validation Notes

- MySQL SQL-mode-aware parser configuration is available through `MySqlSpecs`.
- `sqm-validate-mysql` can reject overlapping or conflicting index-hint combinations.
- MySQL-only syntax is validated during both parse and render phases where applicable.

## Related Pages

- [Parsing SQL](Parsing-SQL)
- [Rendering SQL](Rendering-SQL)
- [Unsupported Features](Unsupported-Features)
- [SQL Middleware Framework](SQL-Middleware-Framework)
