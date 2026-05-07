# SQL Transpilation Usage

This page documents the current user-facing `sqm-transpile` API and the current PostgreSQL/MySQL/SQL Server/Oracle slice that is already implemented.

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

### Oracle -> non-Oracle target

- Exact:
  - Oracle row limiting (`OFFSET ... FETCH`) through the shared `LimitOffset` model
  - Oracle limit-only row limiting to SQL Server `TOP`
- Warning-based rewrite:
  - Oracle statement and table hints are dropped for non-Oracle targets
- Unsupported:
  - Oracle DML `RETURNING ... INTO` remains parse-time unsupported until result targets are modeled
  - advanced Oracle-only query features such as hierarchical queries and `MODEL` clauses require separate model stories

### non-Oracle source -> Oracle

- Exact:
  - shared SELECT/DML forms rendered with Oracle defaults
  - standard/PostgreSQL row limiting rendered as Oracle `OFFSET ... FETCH`
  - portable PostgreSQL `MERGE` subset rendered as Oracle `MERGE`
  - SQL Server baseline `TOP` rewritten to the shared row-limiting model and rendered as Oracle `FETCH FIRST`
- Warning-based rewrite:
  - MySQL and SQL Server hints are dropped when targeting Oracle
- Unsupported:
  - generic DML result clauses, because Oracle requires `RETURNING ... INTO`
  - SQL Server `OUTPUT` and advanced `MERGE`
  - PostgreSQL `DISTINCT ON`
  - PostgreSQL `MERGE DO NOTHING` and `WHEN NOT MATCHED BY SOURCE` when targeting Oracle

## Rule Matrix

| Source                | Target                | Rule family                        | Outcome                                                    |
|-----------------------|-----------------------|------------------------------------|------------------------------------------------------------|
| Oracle                | PostgreSQL/MySQL/ANSI | `OFFSET ... FETCH`                 | Exact shared row-limiting render                           |
| Oracle                | SQL Server            | limit-only row limiting            | Exact rewrite to `TOP`                                     |
| Oracle                | non-Oracle            | hints                              | Approximate drop with `ORACLE_HINTS_DROPPED`               |
| PostgreSQL/MySQL/ANSI | Oracle                | shared row limiting                | Exact Oracle `OFFSET ... FETCH` render                     |
| SQL Server            | Oracle                | baseline `TOP`                     | Exact rewrite to Oracle `FETCH FIRST`                      |
| PostgreSQL            | Oracle                | `RETURNING`/generic result clause  | Unsupported with `UNSUPPORTED_ORACLE_RESULT_CLAUSE`        |
| PostgreSQL            | Oracle                | `DISTINCT ON`                      | Unsupported with `UNSUPPORTED_DISTINCT_ON`                 |
| PostgreSQL            | Oracle                | portable `MERGE` subset            | Exact Oracle `MERGE` render                                |
| PostgreSQL            | Oracle                | `MERGE DO NOTHING`                 | Unsupported with `UNSUPPORTED_MERGE_DO_NOTHING`            |
| PostgreSQL            | Oracle                | `MERGE WHEN NOT MATCHED BY SOURCE` | Unsupported with `UNSUPPORTED_MERGE_NOT_MATCHED_BY_SOURCE` |
| PostgreSQL            | SQL Server            | portable `MERGE` subset            | Exact SQL Server `MERGE` render                            |
| PostgreSQL            | SQL Server            | `MERGE DO NOTHING`                 | Unsupported with `UNSUPPORTED_MERGE_DO_NOTHING`            |
| PostgreSQL            | ANSI/MySQL            | `MERGE`                            | Unsupported with `UNSUPPORTED_POSTGRES_MERGE`              |
| SQL Server            | Oracle                | `OUTPUT`                           | Unsupported with `UNSUPPORTED_SQLSERVER_OUTPUT`            |
| SQL Server            | Oracle                | `MERGE`                            | Unsupported with `UNSUPPORTED_SQLSERVER_MERGE`             |
| MySQL/SQL Server      | Oracle                | hints                              | Approximate drop with dialect-specific hint warning        |

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
