# Identifier Transforms

`IdentifierTransforms` provides column-focused rewrite helpers.

Examples below use static imports from `io.sqm.dsl.Dsl`.

## `rewriteColumns(...)`

Rewrites every visited `ColumnExpr` with a custom mapping function.

Java:

```java
var rewritten = IdentifierTransforms.rewriteColumns(
    query,
    column -> "id".equals(column.name().value()) ? col("user_id") : column
);
```

Before:

```sql
select id
from users
where id = 1
```

After:

```sql
select user_id
from users
where user_id = 1
```

Use this when the rule is more complex than a single rename and you want full access to each visited `ColumnExpr`.

## `renameColumn(...)`

Renames one specific column reference everywhere it appears.

Java:

```java
var rewritten = IdentifierTransforms.renameColumn(
    query,
    "u",
    "id",
    "user_id"
);
```

Before:

```sql
select u.id
from users u
where u.id = u.manager_id
```

After:

```sql
select u.user_id
from users u
where u.user_id = u.manager_id
```

Use this when one logical column changed name and the same statement should be retargeted at runtime.

## `remapColumns(node, Function<ColumnRef, Identifier>)`

Remaps columns using a qualifier-aware descriptor that exposes:

- `tableQualifier`
- `columnName`

Java:

```java
var rewritten = IdentifierTransforms.remapColumns(query, ref -> {
    if (!"tenant_id".equals(ref.columnName().value())) {
        return ref.columnName();
    }
    if (ref.tableQualifier() != null && "u".equals(ref.tableQualifier().value())) {
        return id("customer_id");
    }
    if (ref.tableQualifier() != null && "o".equals(ref.tableQualifier().value())) {
        return id("account_tenant_id");
    }
    return ref.columnName();
});
```

Before:

```sql
select u.tenant_id, o.tenant_id
from users u
join orders o on o.user_id = u.id
where u.tenant_id = o.tenant_id
```

After:

```sql
select u.customer_id, o.account_tenant_id
from users u
join orders o on o.user_id = u.id
where u.customer_id = o.account_tenant_id
```

Use this when the same column name must be rewritten differently depending on table alias or qualifier.

## `remapColumns(node, BiFunction<Identifier, Identifier, Identifier>)`

This is a shorter overload of the same qualifier-aware remap idea. The function receives:

- the table qualifier
- the column name

Java:

```java
var rewritten = IdentifierTransforms.remapColumns(query, (qualifier, column) -> {
    if (!"tenant_id".equals(column.value())) {
        return column;
    }
    return qualifier != null && "u".equals(qualifier.value())
        ? id("customer_id")
        : column;
});
```

Before:

```sql
select u.tenant_id, o.tenant_id
from users u
join orders o on o.user_id = u.id
```

After:

```sql
select u.customer_id, o.tenant_id
from users u
join orders o on o.user_id = u.id
```

Use this overload when you want the same behavior as `ColumnRef`-based remapping but prefer a lighter lambda shape.
