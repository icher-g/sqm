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

## Tips

- Keep aliases stable (`u`, `o`, etc.) for readability.
- Build reusable fragments for predicates/order blocks.
- Run schema validation on built queries before execution.

## Common Pitfalls

- Ambiguous unqualified columns in multi-table queries.
- Using PostgreSQL-only constructs under ANSI dialect.

## Next

- [Rendering SQL](Rendering-SQL)
- [Schema Validation](Schema-Validation)

