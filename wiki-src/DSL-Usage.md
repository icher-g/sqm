# DSL Usage

Use SQM DSL builders to construct query models programmatically.

## Example

```java
Query q = select(
    sel("u", "user_name"),
    func("count", starArg()).as("cnt")
  )
  .from(tbl("users").as("u"))
  .where(col("u", "status").eq(lit("ACTIVE")))
  .groupBy(group("u", "user_name"))
  .orderBy(order("cnt"))
  .limit(10);
```

## Typed Hints

Statement and table hints can be expressed directly through the DSL:

```java
var query = select(col("id"))
  .from(tbl("users").hint("NOLOCK"))
  .hint("MAX_EXECUTION_TIME", 1000)
  .build();
```

Standalone construction is also available when you want to build hints separately:

```java
var query = select(col("id"))
  .from(tbl("users").hint(tableHint("USE_INDEX", "idx_users_name")))
  .hint(statementHint("QB_NAME", lit("main")))
  .build();
```

The same `hint(...)` style is available on DML builders:

```java
var insert = insertInto(tbl("users"))
  .columns(col("id"), col("name"))
  .values(row(lit(1), lit("alice")))
  .hint("MAX_EXECUTION_TIME", 1000)
  .build();
```

## Tips

- Keep aliases stable (`u`, `o`, etc.) for readability.
- Build reusable fragments for predicates/order blocks.
- Run schema validation on built queries before execution.

## Common Pitfalls

- Ambiguous unqualified columns in multi-table queries.
- Using PostgreSQL-only constructs under ANSI dialect.
- Attaching dialect-specific hints and assuming every renderer/transpiler will preserve them exactly.

## Next

- [Rendering SQL](Rendering-SQL)
- [Schema Validation](Schema-Validation)

