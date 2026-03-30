# Relation Transforms

`RelationTransforms` provides table-focused rewrite helpers.

Examples below use static imports from `io.sqm.dsl.Dsl`.

## `rewriteTableRefs(...)`

Rewrites every visited `TableRef`, including normal tables, table variables, derived query tables, values tables, function tables, and `LATERAL` wrappers.

Java:

```java
var rewritten = RelationTransforms.rewriteTableRefs(statement, tableRef ->
    tableRef.matchTableRef()
        .variableTable(variable -> tableVar("@audit_archive"))
        .otherwise(ref -> ref)
);
```

Before:

```sql
update users
set name = 'alice'
output inserted.id into @audit_rows(user_id)
```

After:

```sql
update users
set name = 'alice'
output inserted.id into @audit_archive(user_id)
```

Use this when the rewrite needs access to the full `TableRef` family, not only normal catalog tables.

## `rewriteTables(...)`

Rewrites only real `Table` nodes and leaves other `TableRef` variants unchanged.

Java:

```java
var rewritten = RelationTransforms.rewriteTables(query, table ->
    table.schema() == null
        ? Table.of(id("app"), table.name(), table.alias(), table.inheritance(), table.hints())
        : table
);
```

Before:

```sql
select u.id
from users u
join audit a on a.user_id = u.id
```

After:

```sql
select u.id
from app.users u
join app.audit a on a.user_id = u.id
```

Use this when you want direct control over `Table` nodes but do not want to reason about table variables or derived `FROM` items.

## `qualifyUnqualifiedTables(...)`

Adds the same schema to every unqualified table in a query.

Java:

```java
var rewritten = RelationTransforms.qualifyUnqualifiedTables(query, "tenant42");
```

Before:

```sql
select u.id
from users u
join orders o on o.user_id = u.id
```

After:

```sql
select u.id
from tenant42.users u
join tenant42.orders o on o.user_id = u.id
```

Use this when a query is authored against logical table names during development and bound to a concrete customer schema just before execution.

## `remapTables(node, Function<Identifier, Identifier>)`

Remaps table names with a function while preserving schema, alias, inheritance, and hints.

Java:

```java
var rewritten = RelationTransforms.remapTables(statement, name -> switch (name.value()) {
    case "users" -> id("tenant_users");
    case "orders" -> id("tenant_orders");
    default -> name;
});
```

Before:

```sql
update users u
set name = 'alice'
from orders o
where u.id = o.user_id
```

After:

```sql
update tenant_users u
set name = 'alice'
from tenant_orders o
where u.id = o.user_id
```

Use this when the runtime mapping is computed dynamically.

## `remapTables(node, Map<String, String>)`

Remaps table names with a simple source-to-target map.

Java:

```java
var rewritten = RelationTransforms.remapTables(
    statement,
    Map.of(
        "users", "tenant_users",
        "orders", "tenant_orders",
        "audit", "tenant_audit"
    )
);
```

Before:

```sql
update users u
set name = 'alice'
from orders o
output inserted.id into audit(user_id)
```

After:

```sql
update tenant_users u
set name = 'alice'
from tenant_orders o
output inserted.id into tenant_audit(user_id)
```

Use this when deployment-specific table names are already available as a simple mapping.

## `renameTable(...)`

Renames one table everywhere it appears.

Java:

```java
var rewritten = RelationTransforms.renameTable(statement, "audit", "audit_log");
```

Before:

```sql
update users u
set name = 'alice'
from audit a
output inserted.id into audit(user_id)
```

After:

```sql
update users u
set name = 'alice'
from audit_log a
output inserted.id into audit_log(user_id)
```

Use this when a single logical table was renamed across environments.

## Note About `ResultInto`

Relation rewrites already affect `OUTPUT ... INTO ...` table targets because `ResultInto.target()` is part of the normal `TableRef` tree. A dedicated result-target transform is usually unnecessary unless you specifically need to rewrite only the `INTO` target and not other tables.
