# Statement Transforms

`StatementTransforms` provides `WHERE`-oriented runtime adaptation helpers.

Examples below use static imports from `io.sqm.dsl.Dsl`.

## `andWhere(...)`

Appends one concrete predicate to the current statement block using `AND`.

Java:

```java
var rewritten = StatementTransforms.andWhere(
    query,
    col("tenant_id").eq(lit(42))
);
```

Before:

```sql
select id
from users
where active = true
```

After:

```sql
select id
from users
where active = true
  and tenant_id = 42
```

Use this when you already know the exact predicate that should be added to the statement.

## `andWhereIfMissing(...)`

Appends a predicate only when its conjuncts are not already present in the current `WHERE`.

Java:

```java
var rewritten = StatementTransforms.andWhereIfMissing(
    query,
    col("tenant_id").eq(lit(42)).and(col("active").eq(lit(true)))
);
```

Before:

```sql
select id
from users
where tenant_id = 42
```

After:

```sql
select id
from users
where tenant_id = 42
  and active = true
```

Use this when multiple rewrite stages may apply the same guard more than once.

Note:

- duplicate detection is exact structural equality over `AND` conjuncts
- this is not semantic theorem proving, so equivalent but differently shaped predicates are not treated as duplicates

## `andWherePerTable(...)`

Builds predicates from the real tables visible in the current statement block and appends the combined result to that block's `WHERE`.

Java:

```java
var rewritten = StatementTransforms.andWherePerTable(query, binding -> switch (binding.qualifier().value()) {
    case "u" -> col("u", "tenant_id").eq(lit(42));
    case "o" -> col("o", "tenant_id").eq(lit(42));
    default -> null;
});
```

Before:

```sql
select u.id, o.id
from users u
join orders o on o.user_id = u.id
```

After:

```sql
select u.id, o.id
from users u
join orders o on o.user_id = u.id
where u.tenant_id = 42
  and o.tenant_id = 42
```

Use this when the runtime policy is tied to visible tables or aliases instead of being one global predicate.

Important:

- this helper only affects the current query or DML block
- nested subqueries are left unchanged

## `andWherePerTableRecursively(...)`

Applies the same per-table resolver to the current block and all nested query blocks.

Java:

```java
var rewritten = StatementTransforms.andWherePerTableRecursively(query, binding ->
    "u".equals(binding.qualifier().value()) ? col("u", "tenant_id").eq(lit(42)) : null
);
```

Before:

```sql
select sq.id
from (
    select u.id
    from users u
) sq
```

After:

```sql
select sq.id
from (
    select u.id
    from users u
    where u.tenant_id = 42
) sq
```

Use this when nested subqueries should receive their own local table filters as well.

## Choosing Between the Helpers

- Use `andWhere(...)` when you already have one concrete predicate.
- Use `andWhereIfMissing(...)` when duplicate rewrite stages are possible.
- Use `andWherePerTable(...)` when the added predicates depend on tables or aliases in the current block.
- Use `andWherePerTableRecursively(...)` when the same table-driven policy should also be applied inside nested subqueries.
