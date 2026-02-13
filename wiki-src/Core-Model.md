# Core Model

SQM represents queries as immutable, typed nodes.

## Main Concepts

- `Query`: root query node (`SelectQuery`, `WithQuery`, `CompositeQuery`)
- `Expression`: columns, literals, functions, arithmetic, etc.
- `Predicate`: `AND/OR`, comparison, `IN`, `EXISTS`, etc.
- `TableRef` and joins

Detailed hierarchy: `docs/MODEL.md`.

## Traversal

- `RecursiveNodeVisitor<R>` for analysis
- `RecursiveNodeTransformer` for rewrites
- `Match` API for typed dispatch without `instanceof` chains

## Example: Collect Used Columns

```java
class ColumnCollector extends RecursiveNodeVisitor<Void> {
  private final Set<String> cols = new LinkedHashSet<>();
  @Override protected Void defaultResult() { return null; }
  @Override public Void visitColumnExpr(ColumnExpr c) {
    cols.add(c.tableAlias() == null ? c.name() : c.tableAlias() + "." + c.name());
    return super.visitColumnExpr(c);
  }
  Set<String> columns() { return cols; }
}
```

## Next

- [DSL Usage](DSL-Usage)
- [Schema Validation](Schema-Validation)

