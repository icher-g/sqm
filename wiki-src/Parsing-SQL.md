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

