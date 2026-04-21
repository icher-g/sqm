# SQL Transpilation Usage

This page documents the current user-facing `sqm-transpile` API and the current PostgreSQL/MySQL/SQL Server slice that is already implemented.

For the design rationale and backlog, see [SQL_TRANSPILATION_DESIGN.md](SQL_TRANSPILATION_DESIGN.md).

## What It Does

`sqm-transpile` converts SQL from one supported source dialect into the target dialect by running:

1. source parsing
2. ordered transpilation rules
3. optional target validation
4. target rendering

The result is returned as a `TranspileResult` with:

- final status
- rendered SQL when available
- structured problems
- structured warnings
- applied transpilation steps

## Basic Example

```java
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.SqlTranspiler;

var transpiler = SqlTranspiler.builder()
    .sourceDialect(SqlDialectId.of("postgresql"))
    .targetDialect(SqlDialectId.of("mysql"))
    .build();

var result = transpiler.transpile(
    "SELECT first_name || ' ' || last_name AS full_name FROM users"
);

if (result.success()) {
    System.out.println(result.sql().orElseThrow());
}
```

Expected SQL:

```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users
```

## Approximate Rewrite Example

Approximate rewrites are disabled by default.

To allow them, enable `allowApproximateRewrites` in `TranspileOptions`:

```java
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileOptions;

var transpiler = SqlTranspiler.builder()
    .sourceDialect(SqlDialectId.of("postgresql"))
    .targetDialect(SqlDialectId.of("mysql"))
    .options(new TranspileOptions(true, false, true, true))
    .build();

var result = transpiler.transpile(
    "SELECT * FROM users WHERE name ILIKE 'al%'"
);

System.out.println(result.status());
System.out.println(result.sql().orElseThrow());
result.warnings().forEach(System.out::println);
```

Expected SQL:

```sql
SELECT * FROM users WHERE LOWER(name) LIKE LOWER('al%')
```

Expected warning:

- `APPROXIMATE_ILIKE_LOWERING`

## Multi-Statement Scripts

`SqlTranspiler` parses SQL text through `StatementSequence`, so the same
`transpile(String)` entry point supports both single statements and
semicolon-separated scripts:

```java
var result = transpiler.transpile("""
    SELECT id FROM users;
    SELECT first_name || ' ' || last_name AS full_name FROM users;
    """);
```

Statements are transpiled in source order and rendered back as one combined SQL
script only when the full sequence succeeds. The aggregate outcome is exact only
when every statement is exact, warning-based when at least one statement is
approximate or warning-producing, and unsupported/failed when any statement is
unsupported, invalid, or cannot be rendered. Bind parameterization applies to the
combined render result and `result.params()` preserves statement order.

## Current Supported Slice

### PostgreSQL -> MySQL

- Exact:
  - `ConcatExpr` / string concatenation
  - `IS DISTINCT FROM` / `IS NOT DISTINCT FROM`
  - supported regex predicate subset
- Approximate:
  - `ILIKE`
- Unsupported:
  - `MERGE`
  - `RETURNING`
  - `DISTINCT ON`
  - `SIMILAR TO`
  - representative PostgreSQL-specific operator families
  - PostgreSQL case-insensitive regex variants

### MySQL -> non-MySQL target

- Exact:
  - `ConcatExpr` / string concatenation
  - `<=>`
  - supported regex predicate subset
- Warning-based rewrite:
  - MySQL statement and table hints are dropped for non-MySQL targets
- Unsupported:
  - `ON DUPLICATE KEY UPDATE`
  - `INSERT IGNORE`
  - `REPLACE`
  - representative MySQL JSON function family

### Standard / ANSI -> SQL Server

- Exact:
  - row limiting rewrite from `LIMIT` to SQL Server `TOP`

### SQL Server -> non-SQL Server target

- Exact:
  - `TOP` to standard row-limiting model / target `LIMIT`
- Warning-based rewrite:
  - SQL Server statement and table hints are dropped for non-SQL Server targets
- Unsupported:
  - `TOP ... PERCENT`
  - `TOP ... WITH TIES`
  - `DISTINCT ON` when targeting SQL Server from PostgreSQL source
  - deferred SQL Server advanced DML features such as `OUTPUT` and `MERGE`

## Reading Results

Typical status values:

- `SUCCESS`
- `SUCCESS_WITH_WARNINGS`
- `PARSE_FAILED`
- `UNSUPPORTED`
- `VALIDATION_FAILED`
- `RENDER_FAILED`

Use:

- `result.sql()` for rendered target SQL
- `result.problems()` for blocking issues
- `result.warnings()` for non-blocking rewrites or dropped behavior
- `result.steps()` for the ordered rule trace

SQL Server note:

- The current SQL Server transpilation slice is intentionally baseline-focused.
- More SQL Server-specific rule families are tracked in `docs/epics/R5B_SQL_SERVER_ADVANCED_SUPPORT_COMPLETED.md`.

## Strict Warning Policy

If warnings should fail transpilation, enable `failOnWarnings`:

```java
var options = new TranspileOptions(true, true, true, true);
```

This is useful when dropped hints or approximate rewrites must be rejected in CI or migration tooling.
