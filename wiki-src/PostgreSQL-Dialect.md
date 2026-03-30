# PostgreSQL Dialect

SQM supports PostgreSQL parsing, rendering, validation, DSL/codegen reachability, and downstream runtime coverage for the current delivered PostgreSQL query and DML scope.

This page is a practical guide to the PostgreSQL syntax currently supported by the framework.

## Overview

Current PostgreSQL support includes:

- PostgreSQL query syntax such as:
  - `DISTINCT ON`
  - arrays, subscripts, and slices
  - `AT TIME ZONE`
  - PostgreSQL pattern predicates and operator families in the shipped parser/render scope
  - `LATERAL`
  - function tables and `WITH ORDINALITY`
  - grouping extensions such as `GROUPING SETS`, `ROLLUP`, and `CUBE`
  - locking clauses such as `FOR UPDATE`, `FOR SHARE`, `NOWAIT`, and `SKIP LOCKED`
- PostgreSQL DML extensions such as:
  - `INSERT ... RETURNING`
  - `UPDATE ... FROM`
  - `DELETE ... USING`
  - `INSERT ... ON CONFLICT DO NOTHING / DO UPDATE`
  - writable CTE DML with `RETURNING`
  - `MERGE`
- PostgreSQL semantic validation through `sqm-validate-postgresql`

## Parse And Render

```java
var parseCtx = ParseContext.of(new PostgresSpecs());
var renderCtx = RenderContext.of(new PostgresDialect());

var query = parseCtx.parse(Query.class, """
    SELECT DISTINCT ON (u.id) u.id, u.name
    FROM users u
    ORDER BY u.id, u.name
    """).value();

var sql = renderCtx.render(query).sql();
System.out.println(sql);
```

## Query Examples

`DISTINCT ON`:

```sql
SELECT DISTINCT ON (u.id) u.id, u.name
FROM users u
ORDER BY u.id, u.name
```

`AT TIME ZONE`:

```sql
SELECT created_at AT TIME ZONE 'UTC'
FROM users
```

Array access:

```sql
SELECT tags[1], tags[2:4]
FROM users
```

Locking:

```sql
SELECT id
FROM users
FOR UPDATE SKIP LOCKED
```

## DML Examples

`INSERT ... RETURNING`:

```sql
INSERT INTO users (name)
VALUES ('alice')
RETURNING id
```

`UPDATE ... FROM`:

```sql
UPDATE users u
SET name = src.name
FROM source_users src
WHERE u.id = src.id
```

`DELETE ... USING`:

```sql
DELETE FROM users
USING source_users src
WHERE users.id = src.id
```

`INSERT ... ON CONFLICT`:

```sql
INSERT INTO users (id, name)
VALUES (1, 'alice')
ON CONFLICT (id) DO UPDATE
SET name = 'alice2'
```

## MERGE Example

```sql
MERGE INTO users AS u
USING incoming_users AS s
ON u.id = s.id
WHEN MATCHED THEN
  UPDATE SET name = s.name
WHEN NOT MATCHED THEN
  INSERT (id, name) VALUES (s.id, s.name)
RETURNING u.id
```

## Validation Notes

- `DISTINCT ON` ordering rules are validated explicitly.
- PostgreSQL window and clause consistency checks are available through `sqm-validate-postgresql`.
- Dialect-specific syntax is validated during both parse and render phases where applicable.

## Related Pages

- [Parsing SQL](Parsing-SQL)
- [Rendering SQL](Rendering-SQL)
- [PostgreSQL Validation](PostgreSQL-Validation)
- [Unsupported Features](Unsupported-Features)
- [SQL Middleware Framework](SQL-Middleware-Framework)
