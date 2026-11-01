# DSL Usage

Use SQM DSL builders to construct query models programmatically.

## Example

```java
Query q = select(
    col("u", "user_name"),
    func("count", starArg()).as("cnt")
  )
  .from(tbl("users").as("u"))
  .where(col("u", "status").eq(lit("ACTIVE")))
  .groupBy(col("u", "user_name"))
  .orderBy(col("cnt"))
  .limit(10)
  .build();
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
var insert = insert("users")
  .columns(id("id"), id("name"))
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

## Joins, grouping, and rendering

```java
Query q = select(
        col("u", "user_name"),
        col("o", "status"),
        func("count", starArg()).as("cnt")
    )
    .from(tbl("orders").as("o"))
    .join(
        inner(tbl("users").as("u"))
            .on(col("u", "id").eq(col("o", "user_id")))
    )
    .where(col("o", "status").in("A", "B"))
    .groupBy(col("u", "user_name"), col("o", "status"))
    .having(func("count", starArg()).gt(10))
    .orderBy(col("cnt"))
    .limit(10)
    .offset(20)
    .build();

var ctx = RenderContext.of(new AnsiDialect());
var sql = ctx.render(q).sql();

System.out.println(sql);
```

Rendered ANSI SQL:

```sql
SELECT u.user_name, o.status, count(*) AS cnt
FROM orders AS o
INNER JOIN users AS u ON u.id = o.user_id
WHERE o.status IN ('A', 'B')
GROUP BY u.user_name, o.status
HAVING count(*) > 10
ORDER BY cnt
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
```

## Arithmetic expressions

SQM supports arithmetic expressions as part of the SQL model.
Arithmetic nodes allow SQL engines and transformations to handle numeric operations in a fully structured and type-safe way.

### Supported operations

**Unary**

- `-a` -> `NegativeArithmeticExpr`

**Additive**

- `a + b` -> `AddArithmeticExpr`
- `a - b` -> `SubArithmeticExpr`

**Multiplicative**

- `a * b` -> `MulArithmeticExpr`
- `a / b` -> `DivArithmeticExpr`
- `a % b` -> `ModArithmeticExpr`

### Example

```java
var expr = col("price").add(col("quantity").mul(lit(2)));
```

This produces a model equivalent to:

```sql
price + quantity * 2
```

### Notes

- Operator precedence is preserved by the parser (`* / %` before `+ -`).
- Arithmetic expressions can be nested and combined with predicates, functions, CASE statements, and more.
- All arithmetic nodes extend `ArithmeticExpr` and integrate seamlessly with visitors and transformations.
