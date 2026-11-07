# Parsing SQL

SQM can parse SQL text into the typed query model.

## ANSI Parsing

```java
var ctx = ParseContext.of(new AnsiSpecs());
var result = ctx.parse(Query.class, "select id from users");
if (!result.ok()) {
  throw new IllegalStateException(result.errorMessage());
}
Query query = result.value();
```

## PostgreSQL Parsing

```java
var ctx = ParseContext.of(new PostgresSpecs());
var result = ctx.parse(Query.class, "select distinct on (u.id) u.id from users u");
```

## Oracle Parsing

```java
var oracleParser = ParseContext.of(new OracleSpecs());
var oracleQuery = oracleParser.parse(Query.class,
    "SELECT id FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY");
```

See [Oracle Dialect](Oracle-Dialect) for query/DML syntax and capability versions.
For DML, parse `Statement.class`; see [DML Statements](DML-Statements).

## Set-query clause scope

Trailing `ORDER BY` and pagination on an unparenthesized `UNION`, `INTERSECT`,
or `EXCEPT` expression belong to the enclosing `CompositeQuery`:

```sql
SELECT id FROM users
UNION
SELECT id FROM orders
ORDER BY id DESC LIMIT 5 OFFSET 2
```

Here both SELECT terms have no local ordering or pagination. To apply those
clauses to one term, parenthesize that term:

```sql
SELECT id FROM users
UNION
(SELECT id FROM orders ORDER BY id DESC LIMIT 5 OFFSET 2)
```

Local and composite-level clauses can coexist. Pagination syntax and restrictions
remain dialect-specific; for example, SQL Server uses `OFFSET/FETCH` and requires
an `ORDER BY` on the query owning the pagination. Rendering and reparsing preserve
the distinction between local and composite-level clauses.

Java code generation retains both scopes. The DSL can also construct the full
composite state with `compose(terms, operators, orderBy, limitOffset)`; ordering
and pagination arguments may be `null`.
Validation resolves composite ordering names against the first term's projected
output, including aliases, rather than against an individual term's table scope.

## Checking errors

- Always check `result.ok()`.
- Bubble `errorMessage()` to logs/build output.
- For codegen, fail fast on parser errors.

## Dialect Notes

- PostgreSQL parser supports PG-only constructs like `DISTINCT ON`, arrays, `AT TIME ZONE`.
- ANSI parser validates against ANSI syntax expectations.

## Next

- [Rendering SQL](Rendering-SQL)
- [SQL File Codegen](SQL-File-Codegen)

