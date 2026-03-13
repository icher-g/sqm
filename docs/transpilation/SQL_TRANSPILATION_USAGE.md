# SQL Transpilation Usage

This page documents the current user-facing `sqm-transpile` API and the initial PostgreSQL/MySQL slice that is already implemented.

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

## Current Supported Slice

### PostgreSQL -> MySQL

- Exact:
  - `ConcatExpr` / string concatenation
  - `IS DISTINCT FROM` / `IS NOT DISTINCT FROM`
  - supported regex predicate subset
- Approximate:
  - `ILIKE`
- Unsupported:
  - `RETURNING`
  - `DISTINCT ON`
  - `SIMILAR TO`
  - representative PostgreSQL-specific operator families
  - PostgreSQL case-insensitive regex variants

### MySQL -> PostgreSQL

- Exact:
  - `ConcatExpr` / string concatenation
  - `<=>`
  - supported regex predicate subset
- Warning-based rewrite:
  - optimizer comments and index hints are dropped
- Unsupported:
  - `ON DUPLICATE KEY UPDATE`
  - `INSERT IGNORE`
  - `REPLACE`
  - representative MySQL JSON function family

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

## Strict Warning Policy

If warnings should fail transpilation, enable `failOnWarnings`:

```java
var options = new TranspileOptions(true, true, true, true);
```

This is useful when dropped hints or approximate rewrites must be rejected in CI or migration tooling.
