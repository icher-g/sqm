# Literal Transforms

`LiteralTransforms` provides helpers for parameterization and literal normalization.

Examples below use static imports from `io.sqm.dsl.Dsl`.

## `parameterize(node)`

Replaces inline literals with ordinal parameters and returns both the rewritten node and the collected values.

Java:

```java
var parameterized = LiteralTransforms.parameterize(query);
var rewritten = parameterized.node();
var values = parameterized.values();
```

Before:

```sql
select id
from users
where tenant_id = 42
  and active = true
```

After:

```sql
select id
from users
where tenant_id = $1
  and active = $2
```

Collected values:

```java
[42, true]
```

Use this when a statement is authored with inline literals but should be executed later with bind parameters.

## `parameterize(node, paramCreator)`

Same idea as `parameterize(node)`, but with a custom parameter factory.

Java:

```java
var parameterized = LiteralTransforms.parameterize(
    query,
    i -> ParamExpr.named("p" + i)
);
```

Before:

```sql
select 'a', 'b'
from users
```

After:

```sql
select :p1, :p2
from users
```

Collected values:

```java
["a", "b"]
```

Use this when downstream rendering or execution expects named parameters instead of ordinal parameters.

## `normalizeLiterals(...)`

Normalizes a node by replacing inline literals with ordinal parameters so queries that differ only by literal values share the same shape.

Java:

```java
var normalized = LiteralTransforms.normalizeLiterals(query);
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
where tenant_id = $1
```

Use this for shape-based comparisons such as query fingerprinting or cache keys where the literal values themselves should not affect the normalized form.

## `Parameterized<T>`

`parameterize(...)` returns a `Parameterized<T>` record with:

- `node()`: the rewritten tree
- `valuesByParam()`: parameter node to original literal value
- `values()`: literal values in encounter order

This is the main entry point for turning a literal-heavy statement into a bind-ready statement plus a parameter value list.
