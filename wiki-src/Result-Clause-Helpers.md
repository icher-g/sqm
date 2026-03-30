# Result Clause Helpers

SQM also includes a few semantic helper methods on result-clause model nodes. These are useful when you need to inspect DML result behavior without re-matching low-level node shapes by hand.

## `ResultClause.hasIntoTarget()`

Checks whether the statement result writes into an `INTO` target.

Java:

```java
if (statement.result() != null && statement.result().hasIntoTarget()) {
    // handle OUTPUT ... INTO ...
}
```

Before:

```sql
update users
set name = 'alice'
output inserted.id into audit_rows(user_id)
```

After:

```sql
update users
set name = 'alice'
output inserted.id into audit_rows(user_id)
```

This helper is about inspection, not rewriting. The query shape does not change.

## `ResultClause.usesDialectSpecificResultItems()`

Checks whether the result item list contains dialect-shaped result items such as SQL Server `inserted` or `deleted` references.

Java:

```java
boolean dialectSpecific = statement.result() != null
    && statement.result().usesDialectSpecificResultItems();
```

Before:

```sql
update users
set name = 'alice'
output inserted.id, deleted.name
```

After:

```sql
update users
set name = 'alice'
output inserted.id, deleted.name
```

This helper is useful in feature inspection, validation, and transpilation decisions.

## `ResultInto.isBaseTableTarget()`

Checks whether the `INTO` target is a normal table.

Java:

```java
if (resultInto.isBaseTableTarget()) {
    // target is a normal table
}
```

Before:

```sql
output inserted.id into audit_rows(user_id)
```

After:

```sql
output inserted.id into audit_rows(user_id)
```

## `ResultInto.isVariableTarget()`

Checks whether the `INTO` target is a table variable.

Java:

```java
if (resultInto.isVariableTarget()) {
    // target is a table variable
}
```

Before:

```sql
output inserted.id into @audit_rows(user_id)
```

After:

```sql
output inserted.id into @audit_rows(user_id)
```

## `ResultInto.isDerivedTarget()`

Checks whether the `INTO` target is a derived table-like form rather than a normal table or table variable.

Java:

```java
if (resultInto.isDerivedTarget()) {
    // target is a derived relation form
}
```

Before:

```sql
output inserted.id into (select ...)
```

After:

```sql
output inserted.id into (select ...)
```

These `ResultInto` helpers are inspection-oriented and are mainly useful when feature handling depends on the kind of target being used.
