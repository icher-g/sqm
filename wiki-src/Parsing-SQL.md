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

## Error Handling

- Always check `result.ok()`.
- Bubble `errorMessage()` to logs/build output.
- For codegen, fail fast on parser errors.

## Dialect Notes

- PostgreSQL parser supports PG-only constructs like `DISTINCT ON`, arrays, `AT TIME ZONE`.
- ANSI parser validates against ANSI syntax expectations.

## Next

- [Rendering SQL](Rendering-SQL)
- [SQL File Codegen](SQL-File-Codegen)

