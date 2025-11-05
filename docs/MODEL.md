# SQM Model Hierarchy (Updated)

The SQM (Structured Query Model) provides a composable, immutable AST representation of SQL queries.  
This document preserves the detailed descriptions from the previous version and **adds the textual hierarchy first**, followed by the **Mermaid class diagram** and explanatory sections.

_Last updated: 2025-11-05_

---

## Textual Hierarchy (source of truth)

```
Node
└─ Expression
   ├─ CaseExpr
   ├─ ColumnExpr
   ├─ FunctionExpr
   │  └─ FunctionExpr.Arg
   │     ├─ FunctionExpr.Arg.Column
   │     ├─ FunctionExpr.Arg.Literal
   │     ├─ FunctionExpr.Arg.Function
   │     └─ FunctionExpr.Arg.Star
   ├─ LiteralExpr
   ├─ Predicate
   │  ├─ AnyAllPredicate
   │  ├─ BetweenPredicate
   │  ├─ ComparisonPredicate
   │  ├─ ExistsPredicate
   │  ├─ InPredicate
   │  ├─ IsNullPredicate
   │  ├─ LikePredicate
   │  ├─ NotPredicate
   │  ├─ CompositePredicate
   │  │  ├─ AndPredicate
   │  │  └─ OrPredicate
   │  └─ UnaryPredicate
   └─ ValueSet
      ├─ RowExpr
      ├─ QueryExpr
      └─ RowListExpr
└─ SelectItem
   ├─ ExprSelectItem
   ├─ StarSelectItem
   └─ QualifiedStarSelectItem
└─ Query
   ├─ CompositeQuery
   ├─ SelectQuery
   └─ WithQuery
└─ CteDef
└─ FromItem
   └─ Join
      ├─ CrossJoin
      ├─ NaturalJoin
      ├─ OnJoin
      └─ UsingJoin
   └─ TableRef
      ├─ QueryTable
      ├─ Table
      └─ ValuesTable
└─ GroupBy
└─ GroupItem
└─ WindowDef
└─ BoundSpec
   ├─ BoundSpec.UnboundedPreceding
   ├─ BoundSpec.Preceding
   ├─ BoundSpec.CurrentRow
   ├─ BoundSpec.Following      
   └─ BoundSpec.UnboundedFollowing
└─ FrameSpec
   ├─ FrameSpec.Single      
   └─ FrameSpec.Between
└─ OverSpec
   ├─ OverSpec.Ref      
   └─ OverSpec.Def
└─ PartitionBy
└─ OrderBy
└─ OrderItem
└─ WhenThen
└─ LimitOffset
```

---

## Mermaid Class Diagram

> Mermaid identifiers cannot contain dots.  
> For readability we display the original names in quotes and map them to underscored aliases.

```mermaid
classDiagram
    class Node

    %% Top-level subclasses of Node
    Node <|-- Expression
    Node <|-- SelectItem
    Node <|-- Query
    Node <|-- CteDef
    Node <|-- FromItem
    Node <|-- GroupBy
    Node <|-- GroupItem
    Node <|-- WindowDef
    Node <|-- BoundSpec
    Node <|-- FrameSpec
    Node <|-- OverSpec
    Node <|-- PartitionBy
    Node <|-- OrderBy
    Node <|-- OrderItem
    Node <|-- WhenThen
    Node <|-- LimitOffset

    %% ===================== Expressions =====================
    class Expression
    Expression <|-- CaseExpr
    Expression <|-- ColumnExpr
    Expression <|-- FunctionExpr
    Expression <|-- LiteralExpr
    Expression <|-- Predicate
    Expression <|-- ValueSet

    %% FunctionExpr.Args
    class "FunctionExpr.Arg" as FunctionExpr_Arg
    class "FunctionExpr.Arg.Column" as FunctionExpr_Arg_Column
    class "FunctionExpr.Arg.Literal" as FunctionExpr_Arg_Literal
    class "FunctionExpr.Arg.Function" as FunctionExpr_Arg_Function
    class "FunctionExpr.Arg.Star" as FunctionExpr_Arg_Star

    FunctionExpr <|-- FunctionExpr_Arg
    FunctionExpr_Arg <|-- FunctionExpr_Arg_Column
    FunctionExpr_Arg <|-- FunctionExpr_Arg_Literal
    FunctionExpr_Arg <|-- FunctionExpr_Arg_Function
    FunctionExpr_Arg <|-- FunctionExpr_Arg_Star

    %% Predicates
    class Predicate
    Predicate <|-- AnyAllPredicate
    Predicate <|-- BetweenPredicate
    Predicate <|-- ComparisonPredicate
    Predicate <|-- ExistsPredicate
    Predicate <|-- InPredicate
    Predicate <|-- IsNullPredicate
    Predicate <|-- LikePredicate
    Predicate <|-- NotPredicate
    Predicate <|-- CompositePredicate
    Predicate <|-- UnaryPredicate

    class AndPredicate
    class OrPredicate
    CompositePredicate <|-- AndPredicate
    CompositePredicate <|-- OrPredicate

    %% ValueSet
    class ValueSet
    ValueSet <|-- RowExpr
    ValueSet <|-- QueryExpr
    ValueSet <|-- RowListExpr

    %% ===================== Select Items =====================
    class SelectItem
    SelectItem <|-- ExprSelectItem
    SelectItem <|-- StarSelectItem
    SelectItem <|-- QualifiedStarSelectItem

    %% ===================== Queries =====================
    class Query
    Query <|-- CompositeQuery
    Query <|-- SelectQuery
    Query <|-- WithQuery

    %% ===================== From items and joins =====================
    class FromItem
    class Join
    class TableRef
    FromItem <|-- Join
    FromItem <|-- TableRef

    class CrossJoin
    class NaturalJoin
    class OnJoin
    class UsingJoin
    Join <|-- CrossJoin
    Join <|-- NaturalJoin
    Join <|-- OnJoin
    Join <|-- UsingJoin

    class QueryTable
    class Table
    class ValuesTable
    TableRef <|-- QueryTable
    TableRef <|-- Table
    TableRef <|-- ValuesTable

    %% ===================== Windowing =====================
    class OverSpec
    class "OverSpec.Ref" as OverSpec_Ref
    class "OverSpec.Def" as OverSpec_Def
    OverSpec <|-- OverSpec_Ref
    OverSpec <|-- OverSpec_Def

    class FrameSpec
    class "FrameSpec.Single" as FrameSpec_Single
    class "FrameSpec.Between" as FrameSpec_Between
    FrameSpec <|-- FrameSpec_Single
    FrameSpec <|-- FrameSpec_Between

    class BoundSpec
    class "BoundSpec.UnboundedPreceding" as BoundSpec_UnboundedPreceding
    class "BoundSpec.Preceding" as BoundSpec_Preceding
    class "BoundSpec.CurrentRow" as BoundSpec_CurrentRow
    class "BoundSpec.Following" as BoundSpec_Following
    class "BoundSpec.UnboundedFollowing" as BoundSpec_UnboundedFollowing
    BoundSpec <|-- BoundSpec_UnboundedPreceding
    BoundSpec <|-- BoundSpec_Preceding
    BoundSpec <|-- BoundSpec_CurrentRow
    BoundSpec <|-- BoundSpec_Following
    BoundSpec <|-- BoundSpec_UnboundedFollowing

    %% Partition & Order
    class PartitionBy
    class OrderBy
    class OrderItem
```

---

## 🧩 Key Concepts

### Expressions
Represent value-producing nodes, including literals, columns, functions, and predicates.

| Type           | Description                                                                                                |
|----------------|------------------------------------------------------------------------------------------------------------|
| `ColumnExpr`   | Column reference, possibly qualified by table alias.                                                       |
| `LiteralExpr`  | Constant literal (string, numeric, boolean, etc.).                                                         |
| `CaseExpr`     | SQL `CASE WHEN ... THEN ... END` expression.                                                               |
| `FunctionExpr` | Function call (aggregate, scalar, or analytic). Supports `DISTINCT`, `FILTER`, `WITHIN GROUP`, and `OVER`. |
| `Predicate`    | Boolean-valued expressions for conditions and comparisons.                                                 |
| `ValueSet`     | Tuple, subquery, or row list used in expressions like `IN` or `VALUES`.                                    |

---

### Function Arguments
Modeled under `FunctionExpr.Arg`:

| Variant        | Description                        |
|----------------|------------------------------------|
| `Arg.Column`   | Column reference used as argument. |
| `Arg.Literal`  | Literal value.                     |
| `Arg.Function` | Nested function call as argument.  |
| `Arg.Star`     | `*` argument (e.g., `COUNT(*)`).   |

---

### Predicates

Support for common SQL predicate forms:

| Predicate Type        | Example                         |
|-----------------------|---------------------------------|
| `ComparisonPredicate` | `a = b`                         |
| `BetweenPredicate`    | `a BETWEEN b AND c`             |
| `InPredicate`         | `a IN (1, 2, 3)`                |
| `LikePredicate`       | `name LIKE 'X%'`                |
| `IsNullPredicate`     | `a IS NULL`                     |
| `ExistsPredicate`     | `EXISTS (subquery)`             |
| `AnyAllPredicate`     | `a > ANY (subquery)`            |
| `CompositePredicate`  | Logical connectors: `AND`, `OR` |
| `NotPredicate`        | Logical negation: `NOT expr`    |

---

### Windowing and Analytic Functions

#### WindowDef
Defines a named window inside a `SELECT`’s `WINDOW` clause:
```sql
WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
```

#### OverSpec
Defines how a window function is evaluated:

| Variant        | Example                                    | Description                |
|----------------|--------------------------------------------|----------------------------|
| `OverSpec.Ref` | `OVER w`                                   | References a named window. |
| `OverSpec.Def` | `OVER (PARTITION BY dept ORDER BY salary)` | Inline specification.      |

#### PartitionBy
Represents a `PARTITION BY` clause inside a window specification:
```sql
PARTITION BY dept, region
```

#### FrameSpec
Defines the row or range frame visible to the function:

| Variant             | Example                                    | Meaning             |
|---------------------|--------------------------------------------|---------------------|
| `FrameSpec.Single`  | `ROWS 5 PRECEDING`                         | Single-bound frame. |
| `FrameSpec.Between` | `ROWS BETWEEN 5 PRECEDING AND CURRENT ROW` | Two-bound frame.    |

#### BoundSpec
Specifies the frame boundary:

| Variant              | SQL Example           |
|----------------------|-----------------------|
| `UnboundedPreceding` | `UNBOUNDED PRECEDING` |
| `Preceding`          | `5 PRECEDING`         |
| `CurrentRow`         | `CURRENT ROW`         |
| `Following`          | `2 FOLLOWING`         |
| `UnboundedFollowing` | `UNBOUNDED FOLLOWING` |

> Note: `ORDER BY` inside `OVER(...)` / `WINDOW ... AS (...)` supports `ASC` / `DESC` and (by ANSI 2011) `NULLS FIRST` / `NULLS LAST`. Dialect flags in renderers can restrict emission accordingly.

---

### Grouping and Ordering

| Node        | Example                                                           | Description               |
|-------------|-------------------------------------------------------------------|---------------------------|
| `GroupBy`   | `GROUP BY dept, region`                                           | Defines grouping columns. |
| `GroupItem` | Single grouping element.                                          |
| `OrderBy`   | `ORDER BY salary DESC`                                            | Ordering clause.          |
| `OrderItem` | Represents an ordering expression, direction, and nulls ordering. |

---

### Queries

| Node             | Description                                        |
|------------------|----------------------------------------------------|
| `SelectQuery`    | Represents a `SELECT ... FROM ...` query.          |
| `CompositeQuery` | Combines queries (`UNION`, `INTERSECT`, `EXCEPT`). |
| `WithQuery`      | CTE query: `WITH ... AS (...) SELECT ...`.         |

---

### From Items and Joins

| Node          | Example                                  | Description                       |
|---------------|------------------------------------------|-----------------------------------|
| `Table`       | `FROM employees`                         | Base table reference.             |
| `ValuesTable` | `(VALUES (1, 'A'), (2, 'B')) AS v(x, y)` | Inline values table.              |
| `QueryTable`  | `(SELECT ...) AS t`                      | Subquery as table.                |
| `Join`        | `a JOIN b ON a.id = b.id`                | Generic join abstraction.         |
| `OnJoin`      | `JOIN ... ON condition`                  | Conditional join.                 |
| `UsingJoin`   | `JOIN ... USING (col)`                   | Join using common columns.        |
| `CrossJoin`   | `CROSS JOIN`                             | Cartesian product.                |
| `NaturalJoin` | `NATURAL JOIN`                           | Automatic join by shared columns. |

---

### Other Structural Nodes

| Node          | Purpose                                                     |
|---------------|-------------------------------------------------------------|
| `CteDef`      | CTE definition within a `WITH` clause.                      |
| `WhenThen`    | A single `WHEN ... THEN ...` branch in a `CASE` expression. |
| `LimitOffset` | Pagination clause for limiting and skipping rows.           |

---

## Example (complete)

```sql
SELECT
    dept,
    RANK() OVER w AS dept_rank,
    SUM(salary) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_sum
FROM employees
WINDOW w AS (PARTITION BY dept ORDER BY salary DESC);
```

---

**Status:** Updated (includes `OverSpec`, `FrameSpec`, `BoundSpec`, `WindowDef`, and `PartitionBy`)  
**Module:** `sqm-core`
