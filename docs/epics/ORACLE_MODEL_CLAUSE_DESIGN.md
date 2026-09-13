# Oracle `MODEL` Clause Detailed Design

**Status:** #504 core model/DSL implemented locally and awaiting maintainer review. SQL and downstream support are not yet available.

**Origin:** Standalone follow-up to R11. R11 remains complete. This document
resolves the deferred design decision in
[`R11_ORACLE_SPECIFIC_FEATURES_DESIGN.md`](R11_ORACLE_SPECIFIC_FEATURES_DESIGN.md).

**Target dialect:** Oracle. The semantic model lives in `sqm-core`; dialect
support remains explicit and Oracle-only in the initial implementation.

**GitHub epic:** [#503 — Oracle MODEL Clause Support](https://github.com/icher-g/sqm/issues/503)

**Roadmap identifier:** Intentionally unassigned. Later R-numbers are already in
use, so the stories use the stable `ORACLE-MODEL` prefix.

## 1. Purpose

Oracle's `MODEL` clause treats a query result as a multidimensional array. It
declares partition, dimension, and measure columns, then applies spreadsheet-like
rules that read and update cells. Rules can address or create cells, execute in
dependency order, iterate, and read from reference models.

This is more than parser syntax. A complete implementation must preserve:

- declared columns and their roles;
- multidimensional cell addressing;
- update and upsert semantics;
- rule ordering and iteration;
- reference-model scope;
- model-only expressions and predicates; and
- downstream traversal, validation, transpilation, JSON, DSL, and codegen.

No part of the clause may be retained as raw SQL.

Primary references:

- [Oracle Database 19c `SELECT`](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html)
- [Oracle SQL Modeling for Data Warehousing](https://docs.oracle.com/en/database/oracle/oracle-database/26/dwhsg/sql-modeling-data-warehouses.html)
- [Oracle Model Functions](https://docs.oracle.com/en/database/oracle/oracle-database/18/sqlrf/Model-Functions.html)

## 2. Decision Summary

1. Add `ModelClause` to `SelectQuery`. MODEL applies to a query block; it is not
   a `TableRef` or statement-level clause.
2. Represent declarations, rules, cells, selectors, iteration, and reference
   models as immutable typed nodes in `sqm-core`.
3. Use distinct types for readable cells and writable rule targets. A target is
   not an arbitrary `Expression`.
4. Reuse ordinary `Expression`, `Predicate`, `Query`, `OrderBy`, identifiers,
   and operators only where semantics are unchanged.
5. Store semantic defaults explicitly. Oracle renderers may omit default tokens.
6. Give every node a dedicated parser/renderer pair. ANSI pairs reject;
   Oracle pairs implement the syntax.
7. Add `MODEL_CLAUSE` to `SqlFeature`. Oracle supports it across SQM's current
   Oracle baselines; other dialects reject it.
8. Transpilation is exact only to a fully capable target. Do not initially
   approximate MODEL with relational rewrites.
9. Use vertical stories, but declare completion only after every downstream
   layer and live Oracle execution is covered.

## 3. Structural Placement

MODEL transforms an entire query block after grouping and before final ordering:

```sql
SELECT country, product, year, sales
FROM sales_view
MODEL
  PARTITION BY (country)
  DIMENSION BY (product, sales_year year)
  MEASURES (amount sales)
  RULES (
    sales['Total', 2026] =
      sales['Bikes', 2026] + sales['Cars', 2026]
  )
ORDER BY country, product, year
```

The clause consumes rows produced by `FROM`, `WHERE`, hierarchical query,
grouping, and `HAVING`. It defines columns visible to the select list and final
`ORDER BY`.

Therefore:

- `PatternRecognitionTable extends TableRef` is not a placement precedent;
- `ModelClause` is an optional child of `SelectQuery`;
- each subquery can independently contain a model;
- a reference model contains a typed `Query`; and
- validation treats the declaration as a scope boundary.

The shared clause order becomes:

```text
FROM -> WHERE -> hierarchical -> GROUP BY -> HAVING -> MODEL
     -> WINDOW -> ORDER BY -> pagination -> locking
```

Oracle rejects shared clauses it does not support, such as `WINDOW`. Base query
parser and renderer classes gain small MODEL hooks; Oracle must not duplicate
their complete logic.

Representative advanced shapes that the typed model must preserve include:

```sql
-- Reference-model read
MODEL
  REFERENCE currency ON (SELECT code, rate FROM currency_rates)
    DIMENSION BY (code)
    MEASURES (rate)
  MAIN forecast
    DIMENSION BY (product, year)
    MEASURES (sales)
    RULES (
      sales['Bike', 2026] =
        sales['Bike', 2025] * currency.rate['USD']
    )

-- Symbolic multi-cell aggregation
RULES (
  sales[product = 'Total', year = 2026] =
    SUM(sales)[product <> 'Total', year = 2026]
)

-- Generated targets
RULES UPSERT ALL (
  sales[FOR product IN ('Bike', 'Car'),
        FOR year FROM 2026 TO 2028 INCREMENT 1] =
    sales[CV(product), CV(year) - 1]
)

-- Iterative termination
RULES ITERATE (100) UNTIL (
  ABS(PREVIOUS(sales['Total', 2026]) - sales['Total', 2026]) < 1
) (
  sales['Total', 2026] = sales['Total', 2026] / 2
)
```

These fragments illustrate independent grammar features; they are not intended
to form one combined executable query.

## 4. Query, DML, And DDL Scope

### Query

The full Oracle query-block family is in scope:

- main and reference models;
- partition, dimension, and measure declarations;
- return-row, NAV, and uniqueness options;
- `UPDATE`, `UPSERT`, and `UPSERT ALL`;
- sequential/automatic order and iteration;
- positional, symbolic, single-cell, and multi-cell addressing;
- `FOR` generators and ordered rules;
- model aggregates; and
- MODEL-specific functions and predicates.

### DML

No MODEL-specific DML node is needed. Existing DML may consume a `SelectQuery`
containing `ModelClause` wherever a source query is already allowed. All pipeline
layers must verify this composition. A model rule changes query-result cells; it
does not update the underlying table and is not an `UpdateStatement`.

### DDL

DDL is out of scope and remains a separate architecture decision.

## 5. Cross-Dialect Position

The semantics merit a typed shared model even though initial syntax is
Oracle-specific. Core representability does not imply dialect support.

| Dialect | Parse | Render | Validate | Transpile target |
|---|---:|---:|---:|---:|
| Oracle | Yes | Yes | Yes | Exact |
| ANSI | Reject | Reject | Reject | Unsupported |
| PostgreSQL | Reject | Reject | Reject | Unsupported |
| MySQL | Reject | Reject | Reject | Unsupported |
| SQL Server | Reject | Reject | Reject | Unsupported |

BigQuery's `CREATE MODEL` is unrelated machine-learning DDL. Shared node names
describe semantics so a future equivalent engine can implement different syntax.

## 6. Naming

Use qualifiers where the bare noun is ambiguous, without starting every class
with `Model`.

| Concept | Public name | Reason |
|---|---|---|
| Query clause | `ModelClause` | Established SQL concept. |
| Main/read-only model | `MainModel`, `ReferenceModel` | Clear in clause context. |
| Column declarations | `ModelColumn` | Bare names conflict with other SQL concepts. |
| Rule collection/rule | `ModelRules`, `ModelRule` | Identifies the semantic domain. |
| Readable cell | `CellRefExpr` | It is an expression. |
| Writable cell | `CellTarget` | Assignment-only, not an expression. |
| Read address element | `CellSelector` | Nested variants keep names concise. |
| Writable address element | `CellAddress` | Also permits a target-only `CellFor`. |
| Selector condition | Existing `Predicate` | Reuse boolean semantics; validate in MODEL scope. |
| Wildcard predicate | `IsAnyPredicate` | MODEL wildcard, distinct from quantified `AnyAllPredicate`. |
| `FOR` generator | `CellFor` | Generates selector values. |
| Aggregate plus cells | `ModelAggregateExpr` | Ordinary aggregate with MODEL suffix. |
| `CV()` | `CurrentDimensionExpr` | Semantic rather than token spelling. |
| `ITERATION_NUMBER` | `IterationNumberExpr` | Already precise. |
| `PREVIOUS` | `PreviousModelValueExpr` | Not window `LAG`. |
| `PRESENTV`/`PRESENTNNV` | `PresenceValueExpr` | One shape with a mode. |
| `IS PRESENT` | `CellPresentPredicate` | MODEL-specific predicate. |

`CellAddress`, `CellSelector`, and `CellFor` are sealed
interfaces with nested record variants such as `CellSelector.Value`. This avoids
a class-name explosion while preserving exhaustive matching. The extra address
level prevents a read expression from containing a target-only `FOR` generator.

## 7. Core Model

All nodes are immutable. Optional MODEL state uses nullable fields and record
components, not `Optional` members. The corresponding factories, builder setters,
and accessors accept or return `null` for omission, as documented in JavaDoc.
Required children and enum defaults remain non-null. Every public type, method, record component, and
explicit constructor has complete JavaDoc.

Update the sealed `Node`, `Expression`, and `Predicate` permits hierarchies for
their new direct subtypes. Each new interface implements its own `accept()`;
base nodes must not use `instanceof` dispatch.

### 7.1 Query And Clause

Add nullable `ModelClause model()` to `SelectQuery`, include it in the canonical
full-state `of(...)`, and add `model(...)`/`clearModel()` to its builder. This
matches existing optional query-clause accessors such as `hierarchical()` and
`having()`. Do not add telescoping factories.

```java
public interface ModelClause extends Node {
    ReturnRows returnRows();
    List<ReferenceModel> references();
    MainModel main();

    static ModelClause of(
        ReturnRows returnRows,
        List<ReferenceModel> references,
        MainModel main);

    static Builder builder();
}
```

The builder implementation is an internal nested class of `ModelClause`. The
canonical factory covers full state; ergonomics belong in the builder and DSL.

```java
enum NavigationMode { KEEP, IGNORE }
enum UniquenessMode { DIMENSION, SINGLE_REFERENCE }
enum ReturnRows { ALL, UPDATED }
```

Defaults are explicit: `KEEP`, `DIMENSION`, and `ALL`. Oracle allows cell options
at clause and individual-model positions. Store effective behavior rather than
token position: each reference and the main model retain their effective modes.
While parsing, clause-level options become defaults for all contained models and
local options override them. The redundant global spelling is not persisted.
Rendering may hoist modes shared by every contained model to the clause-level
position; otherwise it emits model-local options. `ReturnRows.ALL` remains on
`ModelClause` because it controls clause output rather than one model.

### 7.2 Main And Reference Models

```java
public interface MainModel extends Node {
    Identifier name();
    List<ModelColumn> partitions();
    List<ModelColumn> dimensions();
    List<ModelColumn> measures();
    NavigationMode navigationMode();
    UniquenessMode uniquenessMode();
    ModelRules rules();
}

public interface ReferenceModel extends Node {
    Identifier name();
    Query query();
    List<ModelColumn> dimensions();
    List<ModelColumn> measures();
    NavigationMode navigationMode();
    UniquenessMode uniquenessMode();
}
```

Main-model name and partitions are optional. Dimensions, measures, and rules are
non-empty. A reference model is read-only, has no partitions/rules, and uses a
full typed query while Oracle validation enforces lack of correlation.

### 7.3 Declared Columns

Do not reuse `ExprSelectItem`; these are not select-list items.

```java
public interface ModelColumn extends Node {
    Expression expression();
    Identifier name();
}
```

The semantic name is mandatory. Parsing derives it from a simple unaliased
column; other expressions require an alias. Rendering may omit an alias only
when the natural expression name is identical. One shared `ModelColumn` represents
the declaration; its containing `partitions()`, `dimensions()`, or `measures()`
list determines the role. No role enum or role-specific declaration subtype is
needed. Validation, schema propagation, and role-specific transformations inspect
that parent context. The DSL retains `.partition(...)`, `.dimension(...)`, and
`.measure(...)`, with `modelColumn(...)` for standalone declarations. Dedicated
parser/renderer work in #505/#506 must register one ModelColumn pair and apply
role-specific restrictions from the containing clause.

The two properties belong to different namespaces:

- `expression()` is evaluated against the input rows entering the MODEL clause;
- `name()` is the resolved model-column name used by cell references, rules, the
  select list, and the query's final `ORDER BY`.

For example:

| SQL declaration | `expression()` | `name()` |
|---|---|---|
| `DIMENSION BY (product)` | `ColumnExpr(product)` | `Identifier(product)` |
| `DIMENSION BY (sales_year year)` | `ColumnExpr(sales_year)` | `Identifier(year)` |
| `DIMENSION BY (sales_year + 1 next_year)` | the typed arithmetic expression | `Identifier(next_year)` |

The apparent duplication in the first row is intentional normalization inside
the immutable AST. Users should not have to provide it twice. Keeping the
resolved name explicit makes rule resolution and transformations deterministic,
including after the source expression is replaced.

### 7.4 Rules

```java
public interface ModelRules extends Node {
    RuleMode defaultMode();
    RuleOrder order();
    IterationSpec iteration();
    List<ModelRule> rules();
}
enum RuleMode { UPDATE, UPSERT, UPSERT_ALL }
enum RuleOrder { SEQUENTIAL, AUTOMATIC }

public interface IterationSpec extends Node {
    Expression limit();
    Predicate until();
}

public interface ModelRule extends Node {
    RuleMode mode();
    CellTarget target();
    OrderBy orderBy();
    Expression value();
}
```

Defaults are `UPSERT` and `SEQUENTIAL`. A missing per-rule mode inherits the
collection mode. Iteration limit remains an expression for consistent literal
handling; Oracle validation requires a positive integer constant. The optional
rule `OrderBy` belongs to its target, not to the query.

### 7.5 Cell Reads And Writes

```java
public interface CellRefExpr extends Expression {
    Identifier model();
    Identifier measure();
    List<CellSelector> selectors();
}

public interface CellTarget extends Node {
    Identifier measure();
    List<CellAddress> addresses();
}
```

A read may qualify the main or a reference model. A target cannot qualify a
reference model. The separate target type prevents arbitrary expressions and
read-only reference cells on the left side of a rule.

### 7.6 Addresses, Selectors, And Conditions

```java
public sealed interface CellAddress extends Node
    permits CellSelector, CellFor {}

public sealed interface CellSelector extends CellAddress {
    record Value(Expression value) implements CellSelector {}
    record Condition(Predicate predicate) implements CellSelector {}
}

public interface IsAnyPredicate extends Predicate {
    Expression dimension();
}
```

`Value` is positional and `Condition` holds a complete symbolic predicate.
Reuse `ComparisonPredicate`, `BetweenPredicate`, `InPredicate`,
`IsNullPredicate`, and boolean composition instead of duplicating their shapes.
The dimension operand is part of the predicate; do not store a second dimension
identifier on the selector. Name resolution and legality are contextual checks,
not marker interfaces added to general-purpose predicates.

`IsAnyPredicate` represents both `product IS ANY` (an explicit dimension
expression) and bare `ANY` (a null dimension, inferred from the selector's
position). There is no separate wildcard variant of `CellSelector`. A bare
wildcard is valid only as the complete predicate of a selector; compound
predicates need an explicit dimension. Unlike `AnyAllPredicate`, it performs
no comparison against a quantified source. Its meaning is to match every
dimension value, including null, in MODEL.

A readable `CellRefExpr` accepts only `CellSelector`; a writable target
accepts the broader `CellAddress`, including `CellFor`. Existing predicate
structure does not imply that all of its forms are legal in a model selector.
The #505/#506 parser and renderer boundaries and #507 validator must check the
applicable context: dimension references, legal operators and flags, permitted
expression kinds, and restrictions on subqueries, aggregates, and current-value
expressions. Subquery-generated values in this designed scope belong to
`CellFor.Values` backed by `QueryExpr`, not an ordinary symbolic condition.

Do not normalize positional values to equality predicates: that can change null
matching and cell-update semantics.

### 7.7 `FOR` Generators

```java
public sealed interface CellFor extends CellAddress {
    List<Identifier> dimensions();

    record Values(List<Identifier> dimensions,
                  ValueSet values) implements CellFor {}
    record Range(Identifier dimension,
                 Expression likePattern,
                 Expression from,
                 Expression to,
                 RangeDirection direction,
                 Expression step) implements CellFor {}
}
enum RangeDirection { INCREMENT, DECREMENT }
```

`Values` reuses the existing `ValueSet` family: `RowExpr` for a non-empty scalar
list with one dimension, `RowListExpr` for non-empty tuples whose widths match
the dimension count, and `QueryExpr` for query-produced tuples. There is no
separate query-generator variant. The constructor checks explicit source shape;
query projection width, correlation, and other Oracle restrictions belong to
the contextual parser/render/validation work in #505/#506/#507. Child sources
must use their existing registered parser/renderer pairs.

`Range` retains its bounds, positive step, direction, and optional
`LIKE ... FROM ... TO ...` template. Neither generator stores an `InPredicate`
or `BetweenPredicate`: generation is not a Boolean membership test.

The DSL uses one helper name for every values source:

```java
cellForValues("sales_year", 2026, 2027);
cellForValues("sales_year", row(2026, 2027));
cellForValues(List.of(id("product"), id("sales_year")),
    rows(row("Bike", 2026), row("Car", 2027)));
cellForValues("sales_year",
    select(col("forecast_year")).from(tbl("forecast_years")).build());
```

### 7.8 Special Expressions And Predicates

Oracle aggregate cell selection is not a function call or array subscript:

```sql
SUM(sales)[product = 'Bike', year BETWEEN 2020 AND 2025]
```

```java
public interface ModelAggregateExpr extends Expression {
    FunctionExpr aggregate();
    List<CellSelector> selectors();
}

public interface CurrentDimensionExpr extends Expression {
    Identifier dimension();
}

public interface PreviousModelValueExpr extends Expression {
    CellRefExpr cell();
}

public interface PresenceValueExpr extends Expression {
    PresenceMode mode();
    CellRefExpr cell();
    Expression whenPresent();
    Expression whenAbsent();
}
enum PresenceMode { CELL, NON_NULL_VALUE }

public interface CellPresentPredicate extends Predicate {
    CellRefExpr cell();
}
```

`IterationNumberExpr` is a dedicated leaf. `CurrentDimensionExpr` represents
`CV()`/`CV(dimension)`. `PreviousModelValueExpr` is not window `LAG`.
`PresenceValueExpr` represents `PRESENTV` and `PRESENTNNV`.

## 8. Builder And DSL

Every public surface is reachable through `Dsl`; tests and codegen never
instantiate `Impl` classes.

```java
var model = model()
    .partition("country")
    .dimension("product")
    .dimension("sales_year", "year")
    .measure("amount", "sales")
    .rule(
        cellTarget("sales", lit("Total"), lit(2026)),
        cellRef("sales", lit("Bikes"), lit(2026))
            .add(cellRef("sales", lit("Cars"), lit(2026))))
    .build();

var query = select(col("country"), col("product"), col("year"), col("sales"))
    .from(table("sales_view"))
    .model(model)
    .orderBy(col("country"), col("product"), col("year"))
    .build();
```

Helper families cover model/reference builders, declarations, rules/options,
cells/targets, all selector and `FOR` variants, model aggregates, current
dimension, iteration number, previous value, presence values, and cell presence.

Column declarations use convenience overloads on the MODEL builder:

```java
// Common unqualified column; model name is derived from the column name.
Builder dimension(String column);

// Unqualified source column with a different model name.
Builder dimension(String column, String name);

// Qualified or quote-aware column; model name is derived from the final part.
Builder dimension(ColumnExpr column);

// Computed expression, renamed column, or explicit quoted model name.
Builder dimension(Expression expression, String name);
Builder dimension(Expression expression, Identifier name);
```

The same overload pattern applies to `partition(...)` and `measure(...)`.
Examples:

```java
.dimension("product")
.dimension("sales_year", "year")
.dimension(col("s", "product"))
.dimension(col("sales_year").add(lit(1)), "next_year")
.measure("sales")
.measure("amount", "sales")
```

There is deliberately no `dimension(Expression)` overload because a general
expression has no reliable natural model name. The canonical node factory stays
`ModelColumn.of(Expression, Identifier)`; convenience belongs to the builder
and DSL rather than telescoping factories. If tests or generated code feel
awkward, improve these helpers before merge.

Cell construction supports both compact varargs and incremental builders:

```java
var previous = cellRef("sales")
    .selector("Bike")
    .selector(2025)
    .build();
var forecast = cellTarget("sales")
    .address("Bike")
    .address(cellForValues("sales_year", 2026, 2027))
    .build();
var baseline = cellRef("sales")
    .model("baseline")
    .selector("Bike")
    .selector(2025)
    .build();
```

`CellRefExpr.Builder` and `CellTarget.Builder` are nested in the node interfaces.
Builders require a measure and at least one coordinate at `build()`, copy lists,
and produce immutable snapshots. `cellRefExpr(Identifier, Identifier)` is the
identifier-based entry point without a prebuilt selector list. The full-state
list helpers and canonical node factories remain available.

Both DSL styles share conversions: literals (including Java `null` as SQL NULL),
expressions, predicates, and explicit selectors are accepted; only targets accept
FOR generators. Use `cellValue("Bike")` or `cellValue(2026)` for an explicit
positional wrapper, replacing the longer `cellSelectorValue` helper. A predicate
explicitly wrapped with `cellValue(predicate)` remains a value expression rather
than becoming a symbolic condition. A string coordinate is always a literal;
use `col("year")` for a column expression. Model qualification uses `.model(...)`
so `cellRef("sales", "Bike")` continues to mean measure plus string coordinate.

Predicate selectors reuse the existing expression DSL:

```java
cellTarget("sales", "Bike", col("sales_year").between(2023, 2026))
cellTarget("sales", "Bike", col("sales_year").in(2025, 2026))
cellTarget("sales", col("product").isAny(), 2026)
cellTarget("sales", isAny(), 2026)
```

`cellCondition(Predicate)` provides an explicit selector wrapper, and
`cellAny()` wraps bare `isAny()`. Convenience conversion checks `Predicate`
before `Expression`, since predicates also implement the expression interface.

## 9. Parser Design

### Tokens And Lexer

Audit every new word against the lexer before writing parser branches. At minimum
inspect:

```text
MODEL MEASURES RULES UPDATE UPSERT UPDATED REFERENCE MAIN
RETURN ROWS KEEP IGNORE NAV UNIQUE DIMENSION SINGLE
AUTOMATIC SEQUENTIAL ITERATE UNTIL INCREMENT DECREMENT
CV ITERATION_NUMBER PRESENTV PRESENTNNV PREVIOUS PRESENT ANY
```

Identifier-like MODEL words that require keyword behavior become explicit
`TokenType` values. Required grammar after committing to a branch uses
`cur.expect(...)`; `consumeIf(...)` is for optional probes.

### Query Integration

`SelectQueryParser` receives a protected hook after `HAVING` and before the
shared `WINDOW` hook. The default/ANSI implementation detects MODEL and delegates
to its registered rejecting parser. Oracle registers the real implementation.
Do not duplicate the query parser in the Oracle module.

### Dedicated Parsers

Each semantic node has a registered parser, including clause/main/reference,
declarations, rules/iteration/rule, cell read/target, selectors/FOR,
aggregate, special expressions, and presence/wildcard predicates. Selector conditions delegate to existing predicate pairs. ANSI pairs exist even if
they only report unsupported syntax. Oracle overrides only real syntax behavior.

### Parse Scope

Within MODEL, declared names distinguish normal identifiers, measure cell reads,
reference-qualified measures, symbolic dimensions, and aggregate cell suffixes.
A local immutable `ModelParseScope` is justified to carry only names declared in
the current clause. It must not leak across query blocks or become general mutable
parser state.

Expression parsing gains narrow atomic/postfix hooks for Oracle to recognize
MODEL expressions while this scope is active. It must not copy the full
expression parser.

### Grammar Order

Enforce Oracle order rather than accepting any order that fits object fields:

1. clause-level cell-reference options;
2. return-row option;
3. zero or more reference models;
4. optional named main model;
5. optional partition declaration;
6. required dimensions;
7. required measures;
8. main-model cell-reference options;
9. rule options; and
10. one or more rules.

Focused failures cover reordered and duplicated clauses. Use shared
`Parser.parseItems(...)` helpers for ordinary comma-delimited lists rather than
duplicated `do/while` loops. Add a focused reusable tuple helper only where list
arity makes the shared helper insufficient.

## 10. Renderer Design

`SelectQueryRenderer` gets a matching protected hook. Oracle delegates to
registered MODEL renderers. Shared/unsupported dialect renderers reject through
`MODEL_CLAUSE` capability checks.

Every child has a dedicated renderer; parent renderers only compose them. The
Oracle renderer:

- emits declarations/rules in canonical order;
- omits aliases only when natural and semantic names match;
- emits non-default effective options;
- preserves explicit choices when omission could change behavior;
- qualifies reference-model reads;
- renders ordered rules on the target side; and
- treats selector brackets as MODEL addressing, never array access.

Checks occur at query entry and directly invoked node renderers so detached-node
rendering cannot bypass support boundaries.

## 11. Validation

Validation is structural, scoped, dialect-specific, and schema-aware.

### Structural Validation

Reject:

- empty dimensions, measures, or rules;
- duplicate declared/model names under identifier quoting/case rules;
- partitions or rules on a reference model;
- empty selector/FOR value lists;
- tuple width different from dimension count;
- invalid iteration limits/range steps;
- targets qualified by a reference model;
- targets that name partitions/dimensions rather than measures; and
- selector lists whose effective arity does not address all dimensions.

Factories reject locally knowable invalid state. Context-dependent problems use
precise validator node paths.

### Scope Validation

Establish four scopes:

1. input rows for declaration expressions;
2. main model for rules;
3. each reference model for qualified reads; and
4. post-model output for select items and final `ORDER BY`.

Verify declaration resolution and unique aliases, symbolic dimension names,
measure/model resolution, unambiguous unqualified reads, uncorrelated reference
queries, post-model column visibility, and isolation of nested subqueries.

### Oracle Semantic Validation

Cover at least:

- `UPDATE`, `UPSERT`, and `UPSERT ALL` target restrictions;
- single-cell/multi-cell and positional/symbolic rules;
- read-only and single-cell restrictions of reference models;
- automatic ordering with iteration;
- legal contexts for `PREVIOUS` and `ITERATION_NUMBER`;
- ordered-rule restrictions;
- aggregate and analytic-function restrictions;
- prohibition of rule-expression subqueries outside legal FOR forms;
- nested cell-reference restrictions;
- query compatibility including recursive members, limiting, and locking; and
- use from existing DML query sources.

Back every compatibility conclusion with focused unit/live tests instead of
assumptions.

### Output Shape

The output schema is known:

```text
partition columns + dimension columns + measure columns
```

Derive types from declaration expressions. Rules may change values/cardinality,
not the declared column set. Type-check rule values against target measures when
catalog type information is available.

## 12. Visitors, Transformers, Matchers, And JSON

- Add dedicated visitor methods for every interface, including leaf nodes.
- Recursive visitors walk children in source-semantic order.
- `RecursiveNodeTransformer` covers `SelectQuery.model()`, reference queries,
  all declarations/rules/cells/selectors, and all expression children.
- Identity tests require the same instance when unchanged and a new immutable
  path when a descendant changes.
- Add matcher arms only for real variant families: `CellAddress`,
  `CellSelector` and `CellFor`. Reuse predicate matchers for conditions. Do not wrap
  single-implementation nodes.
- Register JSON mixins and stable type identifiers for every polymorphic node.
- Round-trip minimal and full models, and verify old `SelectQuery` JSON without
  MODEL still deserializes with a null clause field.

## 13. Capabilities And Registration

Add `SqlFeature.MODEL_CLAUSE`. Oracle capabilities enable it for every Oracle
version currently supported by SQM; MODEL predates the current baseline. Other
dialects omit it.

Registration tests fail for omitted parsers, renderers, visitor methods, JSON
subtypes, or dialect implementations. Do not add empty dialect adapters.

## 14. Transpilation

Add a path-aware `ModelClauseUnsupportedRule`:

- Oracle to a MODEL-capable Oracle target is exact and retains the node;
- incapable targets report an unsupported problem at the clause path;
- nested clauses in subqueries/reference queries are discovered; and
- every `TranspileProblem` contains source and target dialects.

Do not initially approximate with CTEs, joins, windows, `PIVOT`, or client code.
Cell creation, NAV behavior, rule ordering, multi-cell addressing, and iteration
make general rewrites unsafe.

Future rule families are explicit:

1. fixed non-iterative single-cell rules provably equivalent to conditional
   projection/aggregation;
2. `UPDATE`-only sequential rules without references or cell creation; and
3. diagnostic decomposition identifying exactly which constructs block a rewrite.

Each needs its own design and stays opt-in until equivalence is proven.

## 15. Codegen, Control, And Middleware

`SqmDslVisitor` emits the public DSL for the entire clause, including reference
queries and selector variants. Generated code compiles and rebuilds an equal
model without using `Impl` classes. The Maven plugin gets an integration case.

Control/middleware pipelines preserve MODEL through parse, validate, transform,
transpile, render, and interception. Unsupported-target diagnostics remain
visible rather than degrading to generic render failures. Add an
`ExampleCatalog` example with the first executable vertical slice.

## 16. Test Strategy

Every introduced class must have at least 80% meaningful line coverage, with
branch tests rather than incidental traversal.

### Core

- factories, nested builders, defaults, defensive copies, invalid inputs;
- DSL coverage for every public surface;
- visitor order and leaf visits;
- transformer identity/change behavior;
- sealed matcher variants;
- minimal/full JSON round trips and legacy compatibility.

### Parser

Happy paths cover minimal/named/partitioned models, references, all options,
every rule mode/order, iteration, all selector forms, every FOR family, ordered
rules, aggregates, and special expressions/predicates.

Failures cover unsupported dialects, required punctuation/aliases/declarations,
reordered/duplicate clauses, empty/wrong-arity lists, illegal targets/names,
invalid option combinations, reference/subquery restrictions, and malformed
selectors/FOR forms.

### Renderer And Round Trip

- canonical Oracle SQL for every node;
- direct-node unsupported failures;
- parse-render-parse semantic equality; and
- whitespace-normalized assertions where formatting is irrelevant.

### Validation And Transpilation

- structural, scope, type, and dialect problems;
- exact Oracle retention;
- non-Oracle problems with source/target/path;
- nested subquery/reference paths; and
- MODEL queries composed as existing DML sources.

### Live Oracle

Use deterministic cases for:

- basic positional rules and partition isolation;
- `RETURN ALL/UPDATED`, `KEEP/IGNORE NAV`, uniqueness;
- `UPDATE`, `UPSERT`, `UPSERT ALL`;
- sequential and automatic ordering;
- symbolic/multi-cell/aggregate reads;
- every FOR family and ordered rules;
- iteration, `UNTIL`, `ITERATION_NUMBER`, `PREVIOUS`;
- `PRESENTV`, `PRESENTNNV`, `IS PRESENT`;
- reference-model qualified reads; and
- one query constructed entirely with DSL.

Fixtures use committed rows and no timing assumptions. Container startup logs use
the existing shared streaming support.

## 17. Documentation

Implementation updates `docs/model/MODEL.md`, dialect support tables, README
parser/renderer/DSL examples, transpile diagnostics, `ExampleCatalog`, and this
document's status/story table.

Docs distinguish shared representability, Oracle acceptance, other-dialect
rejection, and lack of general transpilation.

## 18. Implementation Stories

The GitHub stories are ordered dependencies under epic #503.

### [ORACLE-MODEL-A](https://github.com/icher-g/sqm/issues/504): Core Query And Model

- Add the query field and every typed node.
- Add nested builders, complete DSL, visitors, transformers, matchers, JSON.
- Update model docs and core tests.

Acceptance: typed immutable state, no raw SQL, full JavaDoc, transformer identity,
and at least 80% meaningful coverage per class.

Core-only delivery boundary: query renderers reject a populated `model()` field
instead of omitting it, and codegen rejects MODEL queries and special expressions.
Dedicated ANSI rejecting parser/renderer pairs and Oracle syntax support belong
to #505/#506. MODEL scope/schema validation, transpilation diagnostics, codegen,
and control/middleware integration remain explicit #507 work; generic traversal
of the new nodes is not evidence that those downstream semantics are supported.
Live execution and executable catalog examples remain #508 work. Each core node
and selector/generator variant includes an illustrative Oracle SQL JavaDoc example;
these examples describe the intended syntax, not current renderer availability.

### [ORACLE-MODEL-B](https://github.com/icher-g/sqm/issues/505): Basic Oracle Parser And Renderer

- Add tokens/capability and base query hooks.
- Add ANSI rejecting pairs.
- Parse/render a main model, declarations/options, positional single-cell rules,
  and rule modes.

Acceptance: a useful vertical query parses/renders/reparses, unsupported dialects
fail clearly, base query logic is not copied, and every child owns its pair.

### [ORACLE-MODEL-C](https://github.com/icher-g/sqm/issues/506): Advanced Grammar And Semantics

- Add references, symbolic/multi-cell selectors, all FOR forms.
- Add ordered rules, aggregates, automatic order, iteration, special expressions.

Acceptance: the full designed grammar round-trips with happy, error, and boundary
tests; expression integration uses narrow hooks/local scope.

### [ORACLE-MODEL-D](https://github.com/icher-g/sqm/issues/507): Validation, Transpilation, And Downstream

- Add structural/scope/schema/Oracle validation.
- Add exact/unsupported transpilation, codegen/plugin, control/middleware.
- Test DML source-query composition.

Acceptance: focused tests for rules, complete problem metadata/path, compiling
equal codegen, and explicit behavior in every downstream module.

### [ORACLE-MODEL-E](https://github.com/icher-g/sqm/issues/508): Live Engine, Examples, And Completion

- Add the live matrix, `ExampleCatalog`, README/dialect docs.
- Review coverage and reconcile all status documents.

Acceptance: deterministic live coverage, every class at least 80%, CI/Codecov
green, and no implicit fallback.

Each story follows repository delivery gates: implement locally, maintainer
review, create/push a branch only after explicit approval, wait for CI/coverage,
merge only after explicit approval, then clean branches before the next story.

## 19. Risks And Mitigations

| Risk | Mitigation |
|---|---|
| Grammar collapses into oversized/string nodes | Keep declarations, rules, cells, selectors, and expressions independently typed. |
| Shared core implies dialect support | Gate parse/render/validate/transpile with `MODEL_CLAUSE`. |
| Ambiguity with array subscripts/identifiers | Explicit tokens where needed and query-local `ModelParseScope`. |
| Oracle copies query parser/renderer | Add narrow base hooks at semantic position. |
| Arbitrary expression becomes rule target | Dedicated `CellTarget`. |
| Post-model names are resolved incorrectly | Explicit output scope from declarations. |
| Reference queries become traversal blind spots | Include them in all recursive/downstream paths. |
| Approximate transpilation changes results | Reject by default; require separate proof-oriented design. |
| Story boundaries leave a half-feature | B is executable; feature completion waits through E. |
| DSL becomes cumbersome | Treat tests/codegen as API reviews and improve helpers before merge. |

## 20. Completion Criteria

Support is complete only when:

- the typed model covers every designed construct without raw SQL;
- `SelectQuery` owns it at the correct position;
- Oracle parses, renders, validates, and executes it;
- unsupported dialects reject at all boundaries;
- visitors, transformers, matchers, JSON, DSL, codegen, control, middleware, and
  DML source composition are covered;
- broad live Oracle cases pass deterministically;
- documentation matches behavior;
- every introduced class has at least 80% meaningful coverage; and
- all stories are merged and this status is marked complete.
