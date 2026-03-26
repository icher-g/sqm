# SQL Transpilation

SQM provides first-class source-to-target SQL transpilation through the `sqm-transpile` module.

The transpilation pipeline is:

1. parse with the source dialect
2. apply ordered transpilation rules
3. optionally validate against the target dialect
4. render target SQL

The result is returned as a `TranspileResult` with:

- overall status
- rendered target SQL when available
- warnings
- blocking problems
- ordered transpilation steps

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

System.out.println(result.status());
System.out.println(result.sql().orElseThrow());
```

Expected SQL:

```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users
```

## Approximate Rewrite Example

Approximate rewrites are disabled by default.

Enable them with `TranspileOptions`:

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

## Current Initial Slice

### PostgreSQL -> MySQL

- Exact:
  - string concatenation through `ConcatExpr`
  - `IS DISTINCT FROM` / `IS NOT DISTINCT FROM`
  - supported regex predicate subset
- Approximate:
  - `ILIKE`
- Unsupported:
  - `RETURNING`
  - `DISTINCT ON`
  - `SIMILAR TO`
  - representative PostgreSQL operator families

### MySQL -> non-MySQL target

- Exact:
  - string concatenation through `ConcatExpr`
  - `<=>`
  - supported regex predicate subset
- Warning-based rewrite:
  - MySQL statement and table hints are dropped for any non-MySQL target
- Unsupported:
  - `ON DUPLICATE KEY UPDATE`
  - `INSERT IGNORE`
  - `REPLACE`
  - representative MySQL JSON function family

### SQL Server -> non-SQL Server target

- Exact:
  - `TOP` to standard row-limiting model / target `LIMIT`
- Warning-based rewrite:
  - SQL Server statement and table hints are dropped for any non-SQL Server target
- Unsupported:
  - `TOP ... PERCENT`
  - `TOP ... WITH TIES`
  - `OUTPUT`
  - `MERGE`

## More

- [Transpilation Examples](SQL-Transpilation-Examples)
- [Examples Module Guide](Examples-Module-Guide)
- [Rendering SQL](Rendering-SQL)
