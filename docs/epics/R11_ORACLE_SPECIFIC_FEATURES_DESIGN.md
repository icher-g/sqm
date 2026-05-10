# R11 Oracle-Specific Feature Support Design

## Purpose

R10 wired Oracle into SQM across the default parser, renderer, validation, transpilation, codegen, catalog, control, middleware, and playground layers. This document designs the next slice: Oracle-specific and Oracle-heavy SQL features that are common enough to matter, while keeping SQM's primary goal in focus: easy, safe query and DML manipulation.

The design follows `docs/reports/SQM_MODELING_RULES.md`:

- Reuse existing SQM nodes where the semantics already fit.
- Add new `sqm-core` nodes only for manipulation-relevant semantics.
- Prefer generic shared nodes when other dialects support the same concept.
- Keep Oracle syntax in Oracle parser, renderer, validation, and transpilation packages.
- Make dialect support explicit. A node being representable in `sqm-core` does not mean every dialect supports it.

## Non-Goals

- Stored procedures, packages, anonymous PL/SQL blocks, cursors, procedure calls, function calls with side-effect semantics, triggers, or package variables.
- DDL, including sequence creation, table partition DDL, materialized views, object types, indexes, and schema management.
- Full PL/SQL.
- Full Oracle SQL Language Reference parity.
- Stringly modeling of features SQM will need to inspect, validate, transform, or transpile.

Stored procedure calls remain out of scope intentionally. SQM is primarily a query and DML manipulation framework, and procedure calls are usually static command invocations rather than rich relational trees that benefit from SQM manipulation.

## Current Oracle Baseline

The current Oracle slice can use the shared SQM model for:

- `SelectQuery`, standard expressions, joins, predicates, grouping, ordering, windows, CTEs, and set operations.
- `LimitOffset`, rendered as Oracle `OFFSET ... FETCH`.
- `Lateral`, where the Oracle parser/renderer support maps to existing correlated relation semantics.
- Baseline `InsertStatement`, `UpdateStatement`, `DeleteStatement`, and `MergeStatement`.
- `StatementHint` and `TableHint` for typed hints where already implemented.
- `OracleFunctionCatalog` for practical scalar, aggregate, date/time, JSON scalar, and null-handling functions.
- Oracle catalog type mapping through `OracleSqlTypeMapper`.

This document only covers features beyond that baseline.

## Design Summary

| Priority | Feature | Existing Node Reuse | Proposed Shape | Dialect Generality |
|---|---|---|---|---|
| 1 | Oracle `RETURNING ... INTO` | Reuse `ResultClause` items | Generalize result targets beyond relation targets | Oracle now; SQL Server relation target already exists; PostgreSQL direct-return target |
| 1 | Sequence value expressions | No existing semantic node | New generic `SequenceValueExpr` | Oracle, PostgreSQL, SQL Server, DB2-style dialects |
| 1 | `CONNECT BY` / `START WITH` | No existing semantic node | New `HierarchicalQueryClause` family | Oracle first, generic semantic name |
| 2 | `PIVOT` / `UNPIVOT` | Relation model can host it, but no node | New generic relation transform nodes | Oracle and SQL Server first |
| 2 | `JSON_TABLE` | Do not force into `FunctionTable` | New generic `JsonTableRef` family | Oracle and MySQL first, SQL-standard-friendly |
| 2 | Flashback query `AS OF` | `Table` can be decorated only with new field/node | New generic table version spec | Oracle first; concept reusable for temporal dialects |
| 2 | Table partition selector | `Table` identifies base table but not selected partitions | Generic table access selector | Oracle first; reusable where dialects expose partition selection |
| 2 | Table sampling | No current table sample node | Generic `TableSample` relation modifier | Oracle, PostgreSQL, SQL Server-style dialects |
| 3 | Oracle `AT TIME ZONE` | Reuse `AtTimeZoneExpr` | Add Oracle parse/render/validation | PostgreSQL and SQL Server already have related support |
| 3 | Oracle locking variants | Reuse `LockingClause` where possible | Extend locking for `WAIT n` only if needed | Oracle, PostgreSQL, SQL Server variants |
| 3 | `MATCH_RECOGNIZE` | No existing semantic node | New row-pattern recognition clause | SQL standard, Oracle, Snowflake, Trino-style dialects |
| 4 | Oracle `MODEL` clause | No existing semantic node | Dedicated model-clause family, deferred | Mostly Oracle-specific |
| 4 | Legacy outer join `(+)` | Existing joins can represent normalized semantics | Migration/transpile parser story, not a persisted syntax node | Oracle legacy only |

## Reuse-First Decisions

### Keep Using `LimitOffset`

Oracle row limiting is already represented by `LimitOffset`. No new node is needed for:

```sql
offset 10 rows fetch next 5 rows only
fetch first 5 rows only
```

Future work should only add validation refinements, such as version gates or unsupported options like `WITH TIES` if the current `LimitOffset` shape cannot represent them exactly.

### Keep Using `Lateral`

Oracle `LATERAL`, `CROSS APPLY`, and `OUTER APPLY` should keep mapping to `Lateral` plus the existing join shapes where semantics match. The distinction between the Oracle spellings belongs in Oracle parser and renderer modules.

No new Oracle-specific lateral node should be added unless SQM must represent a semantic distinction not expressible by `Lateral` plus join kind.

### Keep Using `MergeStatement`

Baseline Oracle `MERGE` should remain on the shared `MergeStatement`, `MergeClause`, and `MergeAction` family. Oracle-specific restrictions, such as unsupported `WHEN NOT MATCHED BY SOURCE`, belong in Oracle validation and rendering guardrails.

If Oracle-specific `DELETE WHERE` placement in matched update clauses cannot be represented cleanly by current `MergeAction` fields, prefer a generic merge action extension rather than an Oracle-only merge node.

### Keep Using `StatementHint` And `TableHint`

Oracle optimizer hints should continue to use typed hints where possible:

```java
StatementHint.of("parallel", HintArg.identifier("orders"), HintArg.expression(lit(4)))
```

Oracle syntax `/*+ ... */` belongs in Oracle parsing/rendering. Hint names and typed arguments remain shared. Add new hint argument kinds only when a common Oracle hint form cannot be expressed without raw strings.

### Reuse `AtTimeZoneExpr`

Oracle supports time-zone conversion expression forms that align closely enough with the existing `AtTimeZoneExpr` concept. The current Javadoc is PostgreSQL-oriented, but the model is semantic:

- timestamp expression
- target time-zone expression

Preferred work:

- Update model docs/Javadocs to make `AtTimeZoneExpr` dialect-neutral.
- Add Oracle parser/render/validation support.
- Preserve dialect-specific syntax in renderer modules.

No new Oracle time-zone expression node is currently justified.

### Reuse `LockingClause` Where It Fits

Oracle `FOR UPDATE`, `NOWAIT`, `SKIP LOCKED`, and lock target lists should use `LockingClause` where current fields suffice.

Potential gap:

- `WAIT n`

If `WAIT n` is implemented, add a generic wait policy to `LockingClause`, for example:

```java
LockWaitPolicy {
    DEFAULT,
    NOWAIT,
    SKIP_LOCKED,
    WAIT(Expression seconds)
}
```

This should be generic because locking wait behavior exists in multiple dialect families, even if syntax differs.

## Feature Designs

## 1. Oracle `RETURNING ... INTO`

### Problem

Oracle DML uses:

```sql
insert into users (id, name)
values (users_seq.nextval, 'alice')
returning id into :id
```

This looks like PostgreSQL `RETURNING`, but it is not the same client-result semantic. PostgreSQL returns rows to the client. SQL Server `OUTPUT INTO` redirects rows into a relation target. Oracle `RETURNING ... INTO` assigns values into host or PL/SQL variables.

The pre-R11 model had:

- `ResultClause`: DML result items.
- `RelationResultTarget`: target relation for SQL Server `OUTPUT ... INTO`.

That relation target stores a `TableRef target`, which cannot represent Oracle host variables by itself.

### Model Decision

Do not add an Oracle-specific `OracleReturningInto` node.

Generalize result targets so `ResultClause` can express these shared semantic cases:

- direct result rows, such as PostgreSQL `RETURNING`
- relation sink, such as SQL Server `OUTPUT ... INTO audit_table`
- variable sink, such as Oracle `RETURNING ... INTO :id, :name`

### Proposed Generic Model

Introduce a shared result-target abstraction:

```java
public sealed interface ResultTarget extends Node
    permits DirectResultTarget, RelationResultTarget, VariableResultTarget {
}
```

Suggested variants:

```java
DirectResultTarget
```

Represents rows returned directly to the caller. This can be implicit when `ResultClause.target()` is empty.

```java
RelationResultTarget(TableRef target, List<Identifier> columns)
```

Represents SQL Server-style result redirection to a table or table variable.

```java
VariableResultTarget(List<ResultVariable> variables)
```

Represents Oracle-style assignment to host or PL/SQL variables.

```java
ResultVariable(Identifier name, VariableBindingStyle style)
```

Possible binding styles:

- `NAMED_BIND`: `:id`
- `ORDINAL_BIND`: `:1`
- `PLSQL_VARIABLE`: `id`

The exact variable representation should align with existing parameter nodes if possible. If `NamedParamExpr` and `OrdinalParamExpr` already represent bind parameters well enough, `VariableResultTarget` can store `List<ParamExpr>` instead of adding `ResultVariable`.

### Implementation Choice

The public result-target API is normalized around `ResultClause.target()`.

- Keep `ResultClause.of(items)` for direct returns.
- Use `ResultClause.of(items, ResultTarget)` for target-bearing result clauses.
- Model SQL Server table/table-variable sinks as `RelationResultTarget`.
- Model Oracle bind-variable sinks as `VariableResultTarget`.
- Do not keep a `ResultClause.into()` compatibility method or a legacy relation-target wrapper while the API is still pre-stable.
- Add DSL helpers:
  - `result(nodes...)`
  - `resultRelationTarget(table, columns...)`
  - `resultVariableTarget(params...)`

### Dialect Behavior

Oracle:

- Parse `RETURNING expr[, ...] INTO target[, ...]`.
- Render only when the target is `VariableResultTarget`.
- Validate item count matches variable count unless a bulk collect story explicitly supports collection targets.
- Reject direct `ResultClause` with no variable target.
- Reject relation target unless Oracle-specific bulk/table target semantics are designed later.

PostgreSQL:

- Render direct result target as `RETURNING`.
- Reject `VariableResultTarget`.
- Reject relation target unless a future PostgreSQL feature supports it.

SQL Server:

- Render direct result target as `OUTPUT`.
- Render relation target as `OUTPUT ... INTO`.
- Reject variable target.

MySQL:

- Continue explicit dialect support based on version and implemented feature slice.

### Transpilation

Exact:

- Direct PostgreSQL `RETURNING` to SQL Server `OUTPUT` may remain exact for compatible result item shapes.
- SQL Server direct `OUTPUT` to PostgreSQL `RETURNING` may remain exact for compatible result item shapes.

Unsupported:

- Oracle `RETURNING ... INTO` to direct-return dialects, unless the user explicitly requests a host-variable-to-client-row semantic change.
- PostgreSQL `RETURNING` to Oracle without provided target variables.
- SQL Server `OUTPUT INTO` to Oracle without provided target variables.

Approximate, opt-in only:

- Oracle variable target to direct-return result when the caller explicitly allows result channel conversion.

## 2. Sequence Value Expressions

### Problem

Oracle sequences are common in inserts:

```sql
users_seq.nextval
users_seq.currval
```

Other dialects also support sequences, but with different syntax:

- PostgreSQL: `nextval('users_seq')`, `currval('users_seq')`
- SQL Server: `NEXT VALUE FOR users_seq`
- DB2 and others: dialect-specific sequence expressions

SQM currently treats these as functions or column-like expressions depending on syntax. That loses semantics and makes transpilation awkward.

### Model Decision

Add a generic expression node, not an Oracle-only node.

```java
public non-sealed interface SequenceValueExpr extends Expression {
    QualifiedName sequence();
    SequenceValueKind kind();
}
```

```java
public enum SequenceValueKind {
    NEXT_VALUE,
    CURRENT_VALUE
}
```

Use `QualifiedName` so schema-qualified sequences are representable:

```sql
app.users_seq.nextval
```

### Dialect Behavior

Oracle:

- Parse `sequence.NEXTVAL` and `sequence.CURRVAL`.
- Render `sequence.NEXTVAL` and `sequence.CURRVAL`.

PostgreSQL:

- Parse `nextval('sequence')` and `currval('sequence')` into `SequenceValueExpr` when the argument is a simple sequence name literal.
- Render `nextval('sequence')` and `currval('sequence')`.
- Leave dynamic function calls as ordinary `FunctionExpr`.

SQL Server:

- Parse `NEXT VALUE FOR sequence` into `SequenceValueExpr(NEXT_VALUE)`.
- Render `NEXT VALUE FOR sequence`.
- Reject `CURRENT_VALUE` unless a supported SQL Server equivalent is designed.

MySQL:

- Reject `SequenceValueExpr` in the current MySQL dialect unless a MariaDB-specific dialect or version gate is introduced.

ANSI:

- Reject by default.

### Required Cross-Dialect Scope

Because sequences exist outside Oracle, the sequence story must include:

- core node
- DSL helper
- parser/render/validation support or explicit rejection for every shipped dialect
- support matrix update for every shipped dialect
- transpilation tests for Oracle/PostgreSQL/SQL Server exact or unsupported paths

This must not land as an Oracle-only parser feature.

### DSL

Suggested helpers:

```java
nextValue("users_seq")
currentValue("users_seq")
nextValue(qname("app", "users_seq"))
```

## 3. Hierarchical Queries: `CONNECT BY`

### Problem

Oracle hierarchical queries are common and semantically distinct:

```sql
select id, parent_id, level
from categories
start with parent_id is null
connect by prior id = parent_id
order siblings by name
```

This is not just recursive CTE syntax. It has traversal semantics, `PRIOR`, `LEVEL`, cycle behavior, sibling ordering, and Oracle pseudocolumns.

### Model Decision

Add a generic semantic clause family, not an Oracle-syntax node:

```java
HierarchicalQueryClause {
    Predicate startWith;
    Predicate connectBy;
    boolean noCycle;
    OrderBy orderSiblingsBy;
}
```

Attach it to `SelectQuery`, most likely alongside `where`, `groupBy`, `having`, and `orderBy`.

Add `PriorExpr` only if `PRIOR` cannot be represented safely as a unary expression:

```java
PriorExpr(Expression expression)
```

Add pseudocolumn support through ordinary identifiers first:

- `LEVEL`
- `CONNECT_BY_ISLEAF`
- `CONNECT_BY_ISCYCLE`

Only add dedicated pseudocolumn nodes if manipulation needs to distinguish these from normal columns.

### Dialect Behavior

Oracle:

- Parse/render `START WITH`, `CONNECT BY`, `NOCYCLE`, `PRIOR`, and `ORDER SIBLINGS BY`.
- Validate placement and combinations.

Other dialects:

- Reject `HierarchicalQueryClause` explicitly.
- Simple single-table hierarchical queries can be transpiled to recursive CTEs when `CONNECT BY` is an equality between one `PRIOR` parent column and one child column, projections are plain columns or `LEVEL`, and `NOCYCLE` / `ORDER SIBLINGS BY` are absent.
- More complex hierarchical queries remain unsupported for recursive CTE rewriting because exact behavior is not guaranteed.

### Transpilation

Unsupported initially for all non-Oracle targets:

- stable code: `UNSUPPORTED_HIERARCHICAL_QUERY`

Future expansion of recursive CTE transpilation must explicitly cover cycle handling, sibling ordering, joins, filtering order, and pseudocolumn semantics.

## 4. `PIVOT` And `UNPIVOT`

### Problem

Oracle and SQL Server both support pivot-style relational transforms, but syntax and capabilities differ.

Oracle example:

```sql
select *
from sales
pivot (
    sum(amount) as total
    for quarter in ('Q1' as q1, 'Q2' as q2)
)
```

SQL Server example:

```sql
select *
from sales
pivot (
    sum(amount) for quarter in ([Q1], [Q2])
) p
```

Since multiple dialects support the concept, this should be generic.

### Model Decision

Add shared relation transform nodes. Do not encode Oracle spelling in the node names.

Possible shape:

```java
public non-sealed interface PivotTable extends TableRef {
    TableRef source();
    List<PivotMeasure> measures();
    Expression forExpression();
    List<PivotValue> values();
    Identifier alias();
}
```

```java
PivotMeasure(FunctionExpr aggregateFunction, Identifier alias)
PivotValue(Expression value, Identifier alias)
```

For unpivot:

```java
public non-sealed interface UnpivotTable extends TableRef {
    TableRef source();
    List<Identifier> valueColumns();
    Identifier nameColumn();
    List<UnpivotInput> inputs();
    NullTreatment nullTreatment();
    Identifier alias();
}
```

```java
UnpivotInput(List<Identifier> sourceColumns, Expression label)
NullTreatment { INCLUDE_NULLS, EXCLUDE_NULLS, DIALECT_DEFAULT }
```

### Scope Control

Start with the intersection that can be validated and rendered safely:

- single pivot-for expression
- aggregate measures
- explicit pivot values
- Oracle and SQL Server rendering

Defer:

- Oracle `PIVOT XML`
- subquery pivot value lists
- multiple pivot-for columns
- dynamic pivot generation

### Dialect Behavior

Oracle:

- Support baseline `PIVOT` and `UNPIVOT`.
- Gate Oracle-only extensions such as `XML`.

SQL Server:

- Support the compatible subset.
- Reject Oracle-only syntax.

PostgreSQL/MySQL/ANSI:

- Reject unless a future transpilation story rewrites to conditional aggregation.

### Transpilation

Exact:

- Oracle subset to SQL Server subset when shapes match.
- SQL Server subset to Oracle subset when shapes match.

Approximate:

- Pivot to conditional aggregation can be opt-in only because column naming and null behavior can differ.

## 5. `JSON_TABLE`

### Problem

Oracle `JSON_TABLE` and MySQL `JSON_TABLE` are table-producing, but they are richer than the current `FunctionTable`:

- root JSON expression
- root path
- mandatory `COLUMNS`
- ordinality columns
- scalar path columns with types
- exists columns
- nested path column groups
- `ON EMPTY` / `ON ERROR`

Forcing this into `FunctionTable(FunctionExpr)` would be too stringly and would hide important manipulation semantics.

### Model Decision

Add a generic `JsonTableRef` relation node.

```java
public non-sealed interface JsonTableRef extends AliasedTableRef {
    Expression json();
    JsonPathSpec rootPath();
    List<JsonTableColumn> columns();
    Identifier alias();
}
```

Column variants:

```java
JsonTableScalarColumn(
    Identifier name,
    TypeName type,
    JsonPathSpec path,
    JsonTableWrapper wrapper,
    JsonTableEmptyBehavior onEmpty,
    JsonTableErrorBehavior onError
)
```

```java
JsonTableOrdinalityColumn(Identifier name)
```

```java
JsonTableExistsColumn(
    Identifier name,
    TypeName type,
    JsonPathSpec path
)
```

```java
JsonTableNestedPathColumn(
    JsonPathSpec path,
    List<JsonTableColumn> columns
)
```

Represent JSON path as a structured value object at first:

```java
JsonPathSpec(String text)
```

Do not parse the JSON path language in the first story. It is acceptable to keep the path body opaque as long as all SQL-level structure around it is typed. If future manipulation needs to inspect JSON paths, design a JSON path AST separately.

### Dialect Behavior

Oracle:

- Support baseline `JSON_TABLE` with typed columns, nested paths, ordinality, and error/empty behavior.

MySQL:

- Support the compatible subset.
- Validate dialect differences.

PostgreSQL/SQL Server/ANSI:

- Reject the node unless a future mapping to dialect-specific JSON row expansion is designed.

### Transpilation

Exact:

- Oracle to MySQL only for compatible subset.
- MySQL to Oracle only for compatible subset.

Unsupported:

- Any nested/error/empty behavior not exactly representable.
- Any target dialect without `JSON_TABLE` support.

## 6. Flashback Query / Temporal Table Versioning

### Problem

Oracle supports flashback query:

```sql
select *
from orders as of timestamp :ts
```

This is a table access semantic: read a table as of a database time or SCN.

Other dialects have related concepts:

- SQL Server temporal tables: `FOR SYSTEM_TIME AS OF`
- Some warehouses have time travel syntax.

### Model Decision

Add a generic table version spec, not an Oracle-only node.

```java
TableVersionSpec {
    TableVersionKind kind;
    Expression value;
}
```

Kinds:

- `AS_OF_TIMESTAMP`
- `AS_OF_SCN`
- `FROM_TO`
- `BETWEEN`
- `CONTAINED_IN`
- `ALL`

Attach to `Table` if versioning only makes sense for base table references. If derived tables need versioning in a future dialect, introduce a wrapper `VersionedTableRef`.

Preferred first slice:

- Extend `Table` with optional `TableVersionSpec`.
- Keep factories compatible through builder/factory additions.

### Dialect Behavior

Oracle:

- `AS OF SCN expr`
- `AS OF TIMESTAMP expr`
- Possibly `VERSIONS BETWEEN ...` in a later slice.

SQL Server:

- `FOR SYSTEM_TIME AS OF expr` can reuse `AS_OF_TIMESTAMP`.
- Other `FOR SYSTEM_TIME` ranges can map to range kinds later.

Other dialects:

- Reject explicitly.

## 7. Table Partition Selection

### Problem

Oracle supports selecting partitions in table references:

```sql
select *
from sales partition (sales_q1)
```

This is not DDL. It is a query access selector that affects which physical partition is scanned.

### Model Decision

Add a generic table access selector if manipulation value is clear.

```java
TableAccessSelector {
    PartitionSelector partition;
    SubpartitionSelector subpartition;
}
```

```java
PartitionSelector(List<Identifier> names)
SubpartitionSelector(List<Identifier> names)
```

Attach to `Table`, not to every `TableRef`, unless a dialect allows partition selection on derived references.

### Dialect Behavior

Oracle:

- Render `PARTITION (...)` and `SUBPARTITION (...)`.

Other dialects:

- Reject unless a compatible feature is added.

### Transpilation

Unsupported by default. Dropping a partition selector changes performance and possibly data visibility when partition names encode retention windows, so it should not be approximate by default.

## 8. Table Sampling

### Problem

Oracle:

```sql
from users sample block (10) seed (42)
```

Other dialects also support table sampling with different syntax.

### Model Decision

Add a generic relation modifier:

```java
TableSample {
    SampleMethod method;
    Expression percentage;
    Expression seed;
}
```

Methods:

- `ROWS`
- `BLOCK`
- `BERNOULLI`
- `SYSTEM`
- `DIALECT_DEFAULT`

Attach to `Table` or to a wrapper `SampledTableRef`. A wrapper is more flexible and avoids overloading `Table` with every relation modifier:

```java
SampledTableRef(TableRef source, TableSample sample)
```

### Dialect Behavior

Oracle:

- `SAMPLE` and `SAMPLE BLOCK`.

PostgreSQL:

- `TABLESAMPLE BERNOULLI/SYSTEM`.

SQL Server:

- `TABLESAMPLE`.

MySQL/ANSI:

- Reject by default.

## 9. `MATCH_RECOGNIZE`

### Problem

Oracle row pattern recognition is powerful and structured:

```sql
match_recognize (
    partition by customer_id
    order by order_date
    measures match_number() as match_no
    pattern (A B+)
    define B as B.amount > A.amount
)
```

This is not a function or window clause. It has row-pattern semantics and is part of the SQL standard family.

### Model Decision

Defer implementation, but when implemented, use a generic row-pattern model:

```java
RowPatternRecognitionClause {
    PartitionBy partitionBy;
    OrderBy orderBy;
    List<SelectItem> measures;
    RowPattern pattern;
    List<RowPatternDefinition> definitions;
    RowPatternRowsPerMatch rowsPerMatch;
    RowPatternAfterMatch afterMatch;
}
```

Do not model `MATCH_RECOGNIZE` as raw text. If we cannot parse the row pattern meaningfully, keep the feature unsupported.

### Dialect Behavior

Oracle first, then any dialect with compatible SQL row pattern recognition.

## 10. Oracle `MODEL` Clause

### Problem

Oracle `MODEL` is complex, multidimensional, and comparatively uncommon in day-to-day application SQL.

### Model Decision

Explicitly defer. If implemented, it needs its own epic/design.

Do not parse it as a raw string. Until designed:

- Oracle parser rejects with a clear diagnostic.
- Transpilation rejects with `UNSUPPORTED_MODEL_CLAUSE`.
- Docs list it as deferred.

## 11. Legacy Outer Join Operator `(+)`

### Problem

Oracle legacy outer join syntax:

```sql
where a.id = b.a_id(+)
```

The semantic target is already expressible using `Join` nodes. The issue is legacy syntax migration, not a new relational concept.

### Model Decision

Do not add a persisted `(+)` node.

If supported, implement as a parser/transpilation migration feature:

- parse legacy syntax
- normalize immediately to existing `Join` nodes when safe
- reject ambiguous or unsupported legacy combinations

This should be separate from the primary Oracle feature model work.

## Capability Additions

Add or review these `SqlFeature` entries:

- `DML_RESULT_VARIABLE_TARGET`
- `SEQUENCE_VALUE_EXPRESSION`
- `HIERARCHICAL_QUERY`
- `PIVOT`
- `UNPIVOT`
- `JSON_TABLE`
- `TABLE_VERSION_AS_OF`
- `TABLE_PARTITION_SELECTOR`
- `TABLE_SAMPLE`
- `MATCH_RECOGNIZE`
- `MODEL_CLAUSE`
- `LOCK_WAIT_TIMEOUT`

Each feature must be version-gated in dialect capability classes where relevant.

## Parser And Renderer Registration Rules

For each accepted node:

- Add parser support only in dialects that support the feature syntax.
- Add renderer support only in dialects that can render exactly.
- Add renderer rejections for all unsupported dialects.
- Avoid inheriting generic parser or renderer behavior implicitly when a dialect should reject.
- Add parser tests for invalid syntax after committed branches using `cur.expect(...)` conventions.

## Validation Rules

Each accepted node needs:

- Dialect support validation.
- Version-gated validation.
- Shape validation.
- Cross-field validation.
- Schema validation if catalog metadata is relevant.

Examples:

- `RETURNING ... INTO`: item count must match target count.
- Sequence values: sequence name may be catalog-validated later; current schema model may need a sequence catalog extension.
- `PIVOT`: pivot measures must be aggregate expressions in dialects that require aggregates.
- `JSON_TABLE`: duplicate column names should be rejected where visible.
- Flashback: `SCN` must be numeric-like if type info is available.
- Partition selector: partition names may be catalog-validated only after catalog model supports partitions.

## Transpilation Rules

Default stance:

- Exact if the target dialect has equivalent semantics and all modeled options match.
- Unsupported if semantics differ.
- Approximate only when explicitly enabled and diagnostics explain the loss.

Required unsupported rule families:

- `oracle-returning-into-unsupported`
- `sequence-value-unsupported`
- `hierarchical-query-unsupported`
- `pivot-unsupported`
- `unpivot-unsupported`
- `json-table-unsupported`
- `table-version-unsupported`
- `partition-selector-unsupported`
- `table-sample-unsupported`
- `match-recognize-unsupported`
- `model-clause-unsupported`

## DSL And Codegen

Every new public model surface must have DSL support before the story is done.

Suggested DSL additions:

```java
nextValue("users_seq")
currentValue("users_seq")
resultVariableTarget(param("id"), param("name"))
hierarchy().startWith(...).connectBy(...)
pivot(source).measure(...).forExpr(...).in(...)
jsonTable(jsonExpr, "$").column(...)
asOfTimestamp(table, param("ts"))
sample(table, 10)
```

Codegen must emit these helpers, not direct `Impl` classes or awkward factories.

## JSON, Visitors, Transformers, Matchers

For each new node:

- Update `Node` permits.
- Add visitor methods.
- Add `RecursiveNodeVisitor` traversal.
- Add `RecursiveNodeTransformer` support with structural sharing.
- Add matchers only for real variant families.
- Add JSON mixins and round-trip tests.
- Update `docs/model/MODEL.md`.

For single-implementation helper nodes, do not add unnecessary matcher wrappers.

## Proposed Story Breakdown

### R11-1: Generalize DML Result Targets For Oracle `RETURNING ... INTO`

Acceptance:

- Result target model can represent direct rows, relation sink, and variable sink.
- Existing PostgreSQL/SQL Server behavior remains compatible.
- Oracle parser/render/validation supports `RETURNING ... INTO`.
- Unsupported cross-dialect transpilation paths are explicit.
- DSL/codegen/docs/tests updated.

### R11-2: Add Generic Sequence Value Expressions

Acceptance:

- `SequenceValueExpr` is added as a generic expression node.
- Oracle, PostgreSQL, and SQL Server support is implemented or explicitly rejected by dialect/version.
- MySQL/ANSI reject explicitly.
- Transpilation rules cover exact Oracle/PostgreSQL/SQL Server cases where safe.
- DSL/codegen/docs/tests updated.

### R11-3: Add Oracle Hierarchical Query Model

Acceptance:

- `HierarchicalQueryClause` and any required `PriorExpr` support are modeled.
- Oracle parser/render/validation supports `START WITH`, `CONNECT BY`, `NOCYCLE`, and `ORDER SIBLINGS BY`.
- Other dialects reject explicitly.
- Transpilation is unsupported by default.
- DSL/codegen/docs/tests updated.

### R11-4: Add Generic `PIVOT` / `UNPIVOT` Relation Transforms

Acceptance:

- Shared pivot/unpivot relation nodes exist.
- Oracle and SQL Server compatible subset parse/render/validate.
- Other dialects reject explicitly.
- Oracle-only and SQL Server-only options are dialect-gated.
- DSL/codegen/docs/tests updated.

### R11-5: Add Generic `JSON_TABLE`

Acceptance:

- `JsonTableRef` and column variants are modeled.
- Oracle and MySQL compatible subset parse/render/validate.
- Other dialects reject explicitly.
- JSON path body is preserved safely as `JsonPathSpec`.
- DSL/codegen/docs/tests updated.

### R11-6: Add Table Access Modifiers

Scope:

- Flashback/temporal table versioning.
- Partition/subpartition selectors.
- Table sampling.

Acceptance:

- Generic nodes are chosen for each accepted modifier.
- Oracle support is implemented.
- Any reusable SQL Server/PostgreSQL support is added or explicitly rejected.
- Transpilation rules are conservative.
- DSL/codegen/docs/tests updated.

### R11-7: Add Oracle Time Zone And Locking Gaps

Acceptance:

- `AtTimeZoneExpr` Javadocs/docs become dialect-neutral.
- Oracle parser/render/validation supports compatible `AT TIME ZONE`.
- `LockingClause` supports Oracle gaps only through generic extensions.
- DSL/codegen/docs/tests updated where needed.

### R11-8: Defer Or Design Advanced Row Pattern Features

Scope:

- `MATCH_RECOGNIZE`
- `MODEL`
- legacy `(+)`

Acceptance:

- Each feature is either designed as a typed model or explicitly remains unsupported.
- No raw-string fallback is introduced.

## Recommended Implementation Order

1. DML result target generalization.
2. Sequence value expressions.
3. Hierarchical queries.
4. Pivot/unpivot.
5. JSON_TABLE.
6. Table access modifiers: flashback, partition selector, sampling.
7. Time-zone and locking gaps.
8. Advanced deferred features.

This order starts with high-use, high-value features and avoids opening the biggest modeling surfaces first.

## Documentation Updates Required Per Story

- `docs/model/MODEL.md`
- `docs/validation/VALIDATION_FEATURES.md`
- `docs/transpilation/SQL_TRANSPILATION_USAGE.md`
- `docs/transpilation/SQL_TRANSPILATION_DESIGN.md`
- `docs/downstream/DOWNSTREAM_SUPPORT_MATRIX.md`
- relevant README or playground docs if user-facing support changes

Each document must state Query, DML, and DDL scope separately where applicable. DDL remains out of scope unless repo instructions change.

