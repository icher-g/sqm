# Core Model

SQM represents queries as immutable, typed nodes.

## Main Concepts

- `Query`: root query node (`SelectQuery`, `WithQuery`, `CompositeQuery`)
- `Expression`: columns, literals, functions, arithmetic, etc.
- `Predicate`: `AND/OR`, comparison, `IN`, `EXISTS`, etc.
- `TableRef` and joins
- typed hints through `StatementHint`, `TableHint`, and `HintArg`

Detailed hierarchy: `docs/model/MODEL.md`.

## Representability vs Support

The core model documents what SQM can represent in the AST. That is not the same as saying every dialect currently supports that node.

- `sqm-core` representability means the framework can model the construct
- parser, renderer, validation, and transpilation support remain dialect-specific
- `docs/model/MODEL.md` contains the fuller support notes and ambiguity table for shared-model nodes such as `ResultClause`, `ResultInto`, `OutputColumnExpr`, and `VariableTableRef`

## Traversal

- `RecursiveNodeVisitor<R>` for analysis
- `RecursiveNodeTransformer` for rewrites
- `Match` API for typed dispatch without `instanceof` chains

## Hints

Hints are now part of the shared model rather than raw string baggage.

- `Statement.hints()` carries typed `StatementHint` nodes for `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `MERGE`
- `Table.hints()` carries typed `TableHint` nodes for relation-owned hint syntax
- `HintArg` preserves argument structure as identifier, qualified-name, or expression data instead of flattening everything to hint-body text

Dialects still own:

- parsing support
- rendering syntax
- validation rules
- transpilation behavior

## Example: Collect Used Columns

```java
class ColumnCollector extends RecursiveNodeVisitor<Void> {
  private final Set<String> cols = new LinkedHashSet<>();
  @Override protected Void defaultResult() { return null; }
  @Override public Void visitColumnExpr(ColumnExpr c) {
    cols.add(c.tableAlias() == null
      ? c.name().value()
      : c.tableAlias().value() + "." + c.name().value());
    return super.visitColumnExpr(c);
  }
  Set<String> columns() { return cols; }
}
```

## Next

- [DSL Usage](DSL-Usage)
- [Schema Validation](Schema-Validation)

