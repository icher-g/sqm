# SQM Group By Expression Support Design

## Purpose

This document describes the design for supporting general expressions in
`GROUP BY` clauses.

The immediate motivating example is the MySQL query:

```sql
SELECT
    DATE_FORMAT(o.order_date, '%Y-%m-01') AS month_start,
    o.customer_id,
    SUM(o.total_amount) AS monthly_total
FROM orders o
WHERE o.order_date >= '2025-01-01'
GROUP BY
    DATE_FORMAT(o.order_date, '%Y-%m-01'),
    o.customer_id
```

SQM currently fails this shape because simple group items are parsed as either
column references or ordinal positions. The `DATE_FORMAT(...)` call in the
`GROUP BY` list is therefore not consumed as one group item, leaving the opening
parenthesis to fail later as unexpected trailing input.

## Recommendation

Support general expressions in `GroupItem.SimpleGroupItem`.

The existing core model already stores an `Expression` for simple group items:

```java
GroupItem.SimpleGroupItem {
    Expression expr();
    Integer ordinal();
}
```

The model does not need a new node type for this feature. The missing behavior is
primarily parser coverage, plus validation, renderer, transpilation, DSL, and
documentation review to keep the support boundary explicit.

## Dialect Position

`GROUP BY` expressions are broadly supported and should be treated as shared SQL
semantics rather than a MySQL-only feature.

Target support:

- ANSI SQM: support expression group items where the expression is valid in the
  current SQM expression grammar.
- PostgreSQL: support expression group items.
- MySQL: support expression group items, including function calls such as
  `DATE_FORMAT(...)`.
- SQL Server: support expression group items.

Dialect-specific differences remain out of this feature unless they are already
represented elsewhere:

- `GROUP BY` select-list aliases are not part of this story.
- Dialect-specific grouping extensions such as `ROLLUP`, `CUBE`, and
  `GROUPING SETS` keep their existing parser and capability gates.
- Dialect-specific expression availability remains governed by each dialect's
  expression parser, renderer, and validator.

## Modeling Decision

Do not add a new semantic node.

`GROUP BY DATE_FORMAT(...)`, `GROUP BY LOWER(name)`, and `GROUP BY a + b` are all
simple grouping items whose key is an expression. The semantic distinction is not
between "column group item" and "function group item"; it is between:

- ordinal grouping, such as `GROUP BY 1`
- expression grouping, such as `GROUP BY DATE_FORMAT(...)`
- grouping extensions, such as `ROLLUP(...)`

The current `GroupItem.SimpleGroupItem` shape already captures this distinction.
Changing the parser to produce `GroupItem.of(expression)` for any valid
expression follows the existing model and the SQM modeling rules.

## Implementation Plan

### 1. Parser

Update the simple group-item parser:

- Keep positive integer handling for ordinal group items.
- Parse non-ordinal group items through `Expression.class` instead of
  `ColumnExpr.class`.
- Preserve existing parse behavior for column references because columns are
  expressions.

Likely file:

- `sqm-parser-ansi/src/main/java/io/sqm/parser/ansi/SimpleGroupItemParser.java`

This should make the behavior available to ANSI-derived dialect parser
registries unless a dialect intentionally overrides the group item parser.

### 2. Ambiguity Handling

`GROUP BY 1` should remain an ordinal group item, not a numeric literal
expression. This preserves current behavior and common SQL semantics.

If SQM later needs to support numeric literal grouping as an expression, that
should be a separate design decision because it conflicts syntactically with
ordinal grouping.

Parenthesized grouping sets must keep precedence over expression parsing in the
delegating `GroupItemParser`. The existing dispatch order should continue to
try grouping-set parsers before falling back to `SimpleGroupItem`.

### 3. Renderer

Review existing renderers for `GroupItem.SimpleGroupItem`.

Expected behavior:

- ordinal group items render the ordinal value
- expression group items render the stored expression

No new renderer type should be needed if the current renderer already delegates
to the expression renderer for `expr()`.

Add tests to confirm rendering function and arithmetic expressions in `GROUP BY`
for each supported dialect slice.

### 4. Validation

Review schema validation rules that compare selected expressions, aggregate
usage, and grouped expressions.

The validator must treat any grouped expression as a grouping key, not only
columns. Examples:

```sql
SELECT LOWER(name), COUNT(*)
FROM users
GROUP BY LOWER(name)
```

should validate when function usage and argument types are valid.

The validator should still reject non-aggregated select expressions that are not
grouped or functionally dependent according to the existing validation model.
This story does not need to implement advanced expression equivalence beyond the
comparison strategy already used in SQM.

### 5. Transpilation

Review transpilation for expression group items.

Expected default behavior:

- If the expression itself can be transpiled and rendered for the target dialect,
  the group item can be preserved.
- If the expression uses a function or operator unsupported in the target
  dialect, existing expression/function/operator transpilation rules should
  report that limitation.

No special group-item transpilation rule is expected for this story.

### 6. DSL and Codegen

The core model already exposes `GroupItem.of(Expression)`, so new public DSL
surface may not be required.

Review the query builder and codegen output for grouped expressions:

- If there is already a helper that accepts `Expression` group items, add tests.
- If grouping helpers only accept column names or ordinals, add an
  expression-based helper with JavaDoc.
- Generated DSL for SQL containing `GROUP BY DATE_FORMAT(...)` should produce
  readable expression group-item construction rather than awkward direct factory
  usage.

### 7. Documentation

Update model and user-facing docs where they describe `GroupItem.SimpleGroupItem`
or `GROUP BY` support.

Likely files:

- `docs/model/MODEL.md`
- relevant parser or dialect support docs if present

## Tests

Add focused parser tests:

- ANSI/shared parser:
  - `GROUP BY LOWER(name)`
  - `GROUP BY amount + tax`
  - `GROUP BY 1` still produces an ordinal group item
- MySQL:
  - full motivating query with `DATE_FORMAT(...)`
  - direct `GroupBy` or `Query` parse of `GROUP BY DATE_FORMAT(...)`
- PostgreSQL:
  - `GROUP BY date_trunc('month', o.order_date)`
  - expression plus column grouping
- SQL Server:
  - `GROUP BY YEAR(o.order_date), MONTH(o.order_date)`

Add renderer tests:

- render grouped function expressions without losing parentheses or argument
  order
- preserve ordinal rendering for `GROUP BY 1`

Add validation tests:

- accept a selected grouped expression plus aggregate
- reject a selected non-aggregated expression that is not grouped
- preserve existing aggregate misuse checks

Add transpilation tests only if current transpilation suites already cover
`GROUP BY` expression preservation. Otherwise record the review in the story and
avoid widening this patch unnecessarily.

## Diagnostics

This feature should also improve diagnostics for the motivating case because the
parser will no longer leave `(` unconsumed after a function name in `GROUP BY`.

If an expression group item fails after the parser commits to `GROUP BY`, the
error should point to the actual expression parse failure. For example:

```text
Expected expression in GROUP BY item
```

or a more specific nested expression error.

This story does not require a comprehensive unsupported-feature diagnostic
registry. That should remain a separate parser diagnostics design.

## Non-Goals

- Supporting select-list aliases in `GROUP BY`.
- Changing ordinal semantics for `GROUP BY 1`.
- Adding new aggregate function syntax such as PostgreSQL inline aggregate
  `ORDER BY`.
- Adding PostgreSQL `ANY(array_expression)` support.
- Adding new dialect functions such as `DATE_FORMAT` validation signatures unless
  required by existing validation tests for this story.
- Changing DDL scope.

## Acceptance Criteria

- Queries can parse `GROUP BY` items that are general SQM expressions.
- `GROUP BY 1` continues to parse and render as an ordinal group item.
- Function expressions in `GROUP BY` render correctly.
- Validation treats grouped expressions as grouping keys.
- Dialect support remains explicit through existing parser, renderer, validation,
  and transpilation boundaries.
- Tests cover happy paths, failure paths, and the ordinal/expression boundary.
- Documentation is updated to describe expression group-item support.
