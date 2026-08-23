# R11-8 SQL Row Pattern Recognition (`MATCH_RECOGNIZE`) Design

**Status:** Complete. R11-8A through R11-8E implement the typed model, shared
infrastructure, Oracle support, validation, transpilation, codegen, documentation,
and deterministic Oracle live-engine coverage.

**Primary delivery target:** Oracle 12.1+.

**Model scope:** Shared SQL row-pattern recognition model in `sqm-core`, with
dialect-specific support and restrictions enforced by parser, renderer,
validation, and transpilation layers.

## Purpose

This document defines an implementation-ready design for SQL row-pattern
recognition as exposed by `MATCH_RECOGNIZE`.

The feature is not Oracle-only. It belongs to the SQL:2016 row-pattern family
and compatible forms are implemented by Oracle, Snowflake, Trino, BigQuery,
Flink, and other analytical engines. SQM should therefore model the semantics
once and keep spelling, feature subsets, version gates, and validation in the
dialect layers.

This design replaces the short deferred sketch in
`R11_ORACLE_SPECIFIC_FEATURES_DESIGN.md`.

## Decision Summary

1. Model `MATCH_RECOGNIZE` as a relation transform named `PatternRecognitionTable`
   that implements `TableRef`.
2. Do not add a `matchRecognize` field to `SelectQuery`.
3. Model the row-pattern grammar as a typed AST. Do not retain a raw pattern
   string.
4. Reuse `PartitionBy`, `OrderBy`, `Expression`, `Predicate`, and ordinary
   aggregate `FunctionExpr` nodes where their semantics already fit.
5. Add typed nodes for pattern-variable column references and the special
   row-pattern expressions whose meaning and legal scope differ from ordinary
   SQL functions.
6. Deliver Oracle parsing, rendering, validation, codegen, and live-engine
   coverage first. Other currently shipped dialects reject the feature
   explicitly.
7. Keep future Snowflake, Trino, BigQuery, and Flink differences in capability
   and validation rules; do not encode their syntax into the core node names.
8. Do not attempt automatic transpilation to windows, recursive CTEs, or joins.
   Such rewrites are generally not semantics-preserving.
9. Query support is in scope. DML changes are not required. DDL remains out of
   scope.

## Naming Convention

The word `Row` is intentionally omitted from public type names. Pattern
recognition in this feature is inherently performed over ordered rows, so the
extra word adds length without resolving an ambiguity.

Use the following vocabulary consistently:

| Role | Public name | Reason |
|---|---|---|
| Relation transform | `PatternRecognitionTable` | Describes the semantic operation; `Table` identifies its `TableRef` role. |
| Pattern grammar root | `MatchPattern` | Distinguishes this grammar from string, `LIKE`, regex, and JSON-path patterns. |
| Grammar variants | `MatchPattern.Variable`, `.Sequence`, `.Alternation`, `.Permutation`, `.Anchor`, `.Empty`, `.Exclusion`, `.Quantified` | Nested variants avoid eight repetitive top-level class names. |
| Clause items | `PatternMeasure`, `PatternDefinition`, `PatternSubset` | The `Pattern` qualifier is retained only where `Measure`, `Definition`, or `Subset` alone would be too broad. |
| Output cardinality | `RowsPerMatch` | Matches the SQL concept and is unambiguous without another prefix. |
| Resume policy | `AfterMatchSkip` | Matches the SQL concept and describes behavior directly. |
| Scoped expressions | `PatternColumnExpr`, `ClassifierExpr`, `MatchNumberExpr`, `PatternNavigationExpr`, `PatternEvaluationExpr` | Exact SQL function names stay short; only otherwise-generic expression roles retain `Pattern`. |
| Builder | `PatternRecognitionTable.Builder` | Nested with the relation type, consistently with other model builders. |
| Visitor | `PatternRecognitionVisitor` | Covers the complete feature family, not only the pattern grammar. |
| Pattern matcher API | `PatternMatch` | Selects between `MatchPattern` variants while following SQM's matcher convention. |

Parser and renderer implementation names follow their target rather than all
sharing one prefix:

```text
PatternRecognitionTableParser / PatternRecognitionTableRenderer
MatchPatternParser / MatchPatternRenderer
PatternVariableParser / PatternVariableRenderer
PatternSequenceParser / PatternSequenceRenderer
PatternAlternationParser / PatternAlternationRenderer
...
```

The public DSL remains syntax-oriented and compact—`matchRecognize(source)`,
`patternVar("A")`, `oneOrMore(...)`, `classifier()`, and `matchNumber()`—so
normal model construction rarely needs the concrete type names.

## Why This Is A `TableRef`

`MATCH_RECOGNIZE` accepts a relation and returns a relation. Oracle, Snowflake,
Trino, and BigQuery place it after a table, view, subquery, or other FROM item:

```sql
SELECT mr.customer_id, mr.match_no, mr.start_date, mr.end_date
FROM sales
MATCH_RECOGNIZE (
    PARTITION BY customer_id
    ORDER BY sale_date
    MEASURES
        MATCH_NUMBER() AS match_no,
        FIRST(start_row.sale_date) AS start_date,
        LAST(up_row.sale_date) AS end_date
    ONE ROW PER MATCH
    AFTER MATCH SKIP PAST LAST ROW
    PATTERN (start_row down_row+ up_row+)
    DEFINE
        down_row AS down_row.amount < PREV(down_row.amount),
        up_row AS up_row.amount > PREV(up_row.amount)
) mr;
```

The transform changes both row cardinality and the visible output columns. It
therefore has the same structural role as `PivotTable`, `UnpivotTable`, and
`SampledTable` rather than query-level clauses such as `WHERE` or `GROUP BY`.

The proposed shape is:

```text
TableRef
└── PatternRecognitionTable
    ├── source: TableRef
    ├── partitionBy: PartitionBy?
    ├── orderBy: OrderBy?
    ├── measures: PatternMeasure[*]
    ├── rowsPerMatch: RowsPerMatch
    ├── afterMatchSkip: AfterMatchSkip
    ├── pattern: MatchPattern
    ├── subsets: PatternSubset[*]
    ├── definitions: PatternDefinition[*]
    └── alias: Identifier?
```

Consequences:

- `TableRefParser.parseRelationTransforms(...)` can parse it as an infix
  transform after a source relation.
- A joined input remains expressible by wrapping the join query in a
  `QueryTable` before applying row-pattern recognition.
- The result can participate in joins and can be wrapped by compatible later
  relation transforms.
- Visitors, transformers, matchers, JSON, validation, and transpilation can
  inspect the transform without special handling in every query implementation.
- A table alias belongs to the result relation, not to `SelectQuery`.

## Delivery Scope

### In Scope For R11-8

- A shared, immutable row-pattern model.
- Oracle 12.1+ `MATCH_RECOGNIZE` parsing and rendering.
- `PARTITION BY`, `ORDER BY`, `MEASURES`, row-count mode, `AFTER MATCH
  SKIP`, `PATTERN`, `SUBSET`, and `DEFINE`.
- Pattern variables, concatenation, alternation, permutation, anchors, empty
  patterns, exclusion, and greedy/reluctant quantifiers.
- Pattern-variable column expressions.
- `CLASSIFIER`, `MATCH_NUMBER`, `FIRST`, `LAST`, `PREV`, `NEXT`, and
  `RUNNING`/`FINAL` expression semantics.
- Ordinary aggregate functions inside `MEASURES` and `DEFINE`, subject to
  Oracle restrictions.
- Parser, renderer, validation, transpilation diagnostics, DSL, codegen, JSON,
  visitor, transformer, matcher, documentation, unit, round-trip, integration,
  and Oracle live-engine coverage.
- Explicit rejection in ANSI SQL-2008, PostgreSQL, MySQL, and SQL Server.

### Deferred Extensions

- SQL row-pattern recognition in window definitions.
- BigQuery `OPTIONS (use_longest_match = ...)`.
- BigQuery-specific `MATCH_ROW_NUMBER()`.
- Engine-specific expression or aggregate extensions not accepted by Oracle.
- Snowflake, Trino, BigQuery, Flink, or Databricks dialect modules.
- Schema inference for the dynamically produced output relation beyond the
  conservative rules described below.
- Approximate transpilation to another relational construction.

### Query, DML, And DDL Boundary

| Surface | Decision |
|---|---|
| Query | In scope as a FROM-item relation transform. |
| DML | No new DML node is needed. A query containing `PatternRecognitionTable` may still be used as an existing `INSERT` source where the target dialect permits it. |
| DDL | Out of scope. No table, view, or materialized-view DDL work is implied. |

## Cross-Dialect Basis

The shared model is justified by the common relational and pattern semantics,
not by identical option sets.

| Capability | Oracle | Snowflake | Trino | BigQuery | Flink |
|---|---:|---:|---:|---:|---:|
| FROM-item `MATCH_RECOGNIZE` | Yes | Yes | Yes | Yes | Yes |
| `PARTITION BY` | Yes | Yes | Yes | Yes | Yes |
| Optional `ORDER BY` in grammar | Yes | Yes | Yes | No | Engine-specific |
| Measures | Yes | Yes | Yes | Yes | Yes |
| One/all rows per match | Yes | Yes | Yes | One row only | Subset |
| Empty/unmatched row modifiers | No baseline support | Yes | Yes | No | Subset |
| Skip past last/to next | Yes | Yes | Yes | Yes | Yes |
| Skip to pattern variable | Yes | Yes | Yes | No | Subset |
| `SUBSET` union variables | Yes | Not in current syntax | Yes | No | No |
| `PERMUTE` | Yes | Yes | Yes | No | No |
| Exclusion `{- ... -}` | Yes | Yes | Yes | No | No |
| Bounded and reluctant quantifiers | Yes | Yes | Yes | Yes | Subset |
| Vendor options | No baseline option node needed | Restrictions only | Additional standard coverage | `use_longest_match` | Streaming restrictions |

This table is a design matrix, not a claim that SQM currently ships those
dialect implementations. At R11-8 completion only Oracle is enabled.

Primary references:

- [Oracle 12.1 `SELECT` reference](https://docs.oracle.com/database/121/SQLRF/statements_10002.htm)
- [Current Oracle `SELECT` row-pattern reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [Snowflake `MATCH_RECOGNIZE`](https://docs.snowflake.com/en/sql-reference/constructs/match_recognize)
- [Trino `MATCH_RECOGNIZE`](https://trino.io/docs/current/sql/match-recognize.html)
- [BigQuery query syntax](https://docs.cloud.google.com/bigquery/docs/reference/standard-sql/query-syntax#match_recognize_clause)
- [Flink row-pattern recognition](https://nightlies.apache.org/flink/flink-docs-release-2.3/docs/sql/reference/queries/match_recognize/)

## Core Model

### `PatternRecognitionTable`

Add a public `PatternRecognitionTable` interface in `sqm-core`:

```java
public non-sealed interface PatternRecognitionTable extends TableRef {
    static PatternRecognitionTable of(
        TableRef source,
        PartitionBy partitionBy,
        OrderBy orderBy,
        List<PatternMeasure> measures,
        RowsPerMatch rowsPerMatch,
        AfterMatchSkip afterMatchSkip,
        MatchPattern pattern,
        List<PatternSubset> subsets,
        List<PatternDefinition> definitions,
        Identifier alias
    );

    TableRef source();
    PartitionBy partitionBy();
    OrderBy orderBy();
    List<PatternMeasure> measures();
    RowsPerMatch rowsPerMatch();
    AfterMatchSkip afterMatchSkip();
    MatchPattern pattern();
    List<PatternSubset> subsets();
    List<PatternDefinition> definitions();
    Identifier alias();
}
```

The interface also exposes:

- `builder()` and `builder(PatternRecognitionTable)`.
- `as(String)` and `as(Identifier)` immutable copy methods.
- Its own `accept()` implementation dispatching to
  `visitPatternRecognitionTable(...)`.

The full-state `of(...)` method is the single canonical factory. Do not add
telescoping public overloads; use the builder and DSL for ergonomics.

Construction invariants:

- `source`, `rowsPerMatch`, `afterMatchSkip`, and `pattern` are non-null.
- List fields are non-null and defensively copied.
- `partitionBy`, `orderBy`, and `alias` may be null.
- Empty `measures` and `subsets` are representable. `definitions` is non-empty
  because the shared Oracle/Snowflake/Trino/BigQuery grammar requires
  `DEFINE`; dialect validation decides whether every primary variable needs an
  explicit item.
- Cross-field and dialect rules remain in validation rather than constructors.

`TableRef` must permit `PatternRecognitionTable`. Do not add another convenience
factory to `TableRef`; the fluent entry point should live in
`Dsl.matchRecognize(source)` and the builder.

### Clause Item Nodes

Use dedicated nodes rather than reusing syntax-adjacent types where the role
has different invariants.

```java
PatternMeasure(Expression expression, Identifier alias)
```

- Alias is mandatory in the semantic model because it defines an output
  column.
- Do not reuse `SelectItem`; stars and optional aliases are not valid measure
  definitions.

```java
PatternDefinition(Identifier variable, Predicate condition)
```

- Defines the condition for a primary pattern variable.
- A primary variable missing from the list means implicit `TRUE` only in
  dialects that specify that behavior. Oracle does not: its validator requires
  every primary variable to have a definition.

```java
PatternSubset(Identifier name, List<Identifier> variables)
```

- Defines one union variable.
- Constructor requires a non-empty copied variable list.
- Membership and name-collision rules are validated against the whole table.

Each of these public types has one canonical `of(...)` factory, complete
JavaDoc, its own `accept()` method, dedicated ANSI parser and renderer, visitor
and transformer coverage, JSON registration, and focused tests.

### Rows Per Match

Represent output cardinality and empty/unmatched-row handling independently:

```java
RowsPerMatch(
    RowsPerMatch.Mode mode,
    RowsPerMatch.EmptyMatchHandling emptyMatchHandling
)
```

```java
enum Mode {
    ONE,
    ALL
}

enum EmptyMatchHandling {
    DEFAULT,
    SHOW_EMPTY,
    OMIT_EMPTY,
    WITH_UNMATCHED
}
```

Semantic defaults:

- Omitted row-count syntax becomes `ONE + DEFAULT`.
- `emptyMatchHandling != DEFAULT` requires `mode == ALL`.
- Oracle R11-8 accepts `ONE + DEFAULT` and `ALL + DEFAULT` only.
- The other variants remain representable for future Snowflake/Trino support.

The renderer may omit `ONE ROW PER MATCH` when it equals the dialect default,
but canonical SQM formatting should render it explicitly for clarity and
stable generated examples. Parser/render round trips are semantic, not a
promise to preserve omission of default syntax.

### After Match Skip

Use one structured node instead of raw text or a class per phrase:

```java
AfterMatchSkip(
    AfterMatchSkip.Kind kind,
    AfterMatchSkip.Position position,
    Identifier variable
)
```

```java
enum Kind {
    PAST_LAST_ROW,
    TO_NEXT_ROW,
    TO_VARIABLE
}

enum Position {
    DEFAULT,
    FIRST,
    LAST
}
```

Valid shapes:

| Kind | Position | Variable |
|---|---|---|
| `PAST_LAST_ROW` | `DEFAULT` | null |
| `TO_NEXT_ROW` | `DEFAULT` | null |
| `TO_VARIABLE` | `DEFAULT`, `FIRST`, or `LAST` | required |

Omitted syntax normalizes to `PAST_LAST_ROW`. Oracle rendering may emit the
canonical explicit form. The DSL supplies readable helpers such as
`skipPastLastRow()`, `skipToNextRow()`, and `skipToFirst("up_row")`; these are
DSL helpers, not additional node factories.

## Typed Row-Pattern Grammar

### Pattern Family

Add a sealed `MatchPattern` node family:

```text
MatchPattern
├── MatchPattern.Variable
├── MatchPattern.Sequence
├── MatchPattern.Alternation
├── MatchPattern.Permutation
├── MatchPattern.Anchor
├── MatchPattern.Empty
├── MatchPattern.Exclusion
└── MatchPattern.Quantified
```

These are nested public variants of `MatchPattern`, following the same compact
family style already used by types such as `BoundSpec`, `FrameSpec`, and
`OverSpec`. Each variant still implements its own `accept()` dispatch.

The variants are:

```java
MatchPattern.Variable(Identifier name)
```

One primary pattern variable occurrence.

```java
MatchPattern.Sequence(List<MatchPattern> elements)
```

An ordered sequence such as `A B+ C`. Require at least two elements; DSL and
parser construction collapse a single element to that element.

```java
MatchPattern.Alternation(List<MatchPattern> alternatives)
```

An ordered alternation such as `A | B`. Require at least two alternatives.
Order is semantically significant because it participates in match choice.

```java
MatchPattern.Permutation(List<MatchPattern> elements)
```

`PERMUTE(A, B, C)`. Require at least two elements. Dialect validation may
further restrict which child shapes are legal.

```java
MatchPattern.Anchor(MatchPattern.Anchor.Kind kind)
```

`START` renders as `^`; `END` renders as `$`.

```java
MatchPattern.Empty
```

A leaf singleton representing `()`.

```java
MatchPattern.Exclusion(MatchPattern pattern)
```

Represents `{- pattern -}`. It remains distinct from grouping because it
changes rows emitted by `ALL ROWS PER MATCH`.

```java
MatchPattern.Quantified(
    MatchPattern pattern,
    Integer minimum,
    Integer maximum,
    boolean reluctant
)
```

Quantifier normalization:

| Minimum | Maximum | Canonical rendering |
|---:|---:|---|
| 0 | null | `*` |
| 1 | null | `+` |
| 0 | 1 | `?` |
| n | n | `{n}` |
| n | null | `{n,}` |
| 0 | m | `{,m}` |
| n | m | `{n,m}` |

`reluctant == true` appends `?`. Bounds must be non-negative and minimum must
not exceed maximum. An unbounded maximum is null. A null minimum is not used;
`{,m}` normalizes to minimum zero.

Do not add a persisted grouping node. Parentheses that only express precedence
have no independent semantics. The renderer adds parentheses according to
child precedence and quantifier attachment. `MatchPattern.Empty` preserves the
one parenthesized form that does have meaning.

### Pattern Precedence

Use explicit renderer precedence:

1. Variable, anchor, empty, and permutation primaries.
2. Exclusion and parenthesized composite primaries.
3. Quantification.
4. Concatenation.
5. Alternation.

The pattern renderer must parenthesize a child whenever rendering it without
parentheses could change the tree. Examples:

```text
quantified(alternation(A, B), 1, null) -> (A | B)+
concat(A, alternation(B, C))           -> A (B | C)
alternation(concat(A, B), C)           -> A B | C
```

The `MatchPattern` family is a real variant family, so matcher support is
justified. Add `PatternMatch<R>` and cover every concrete variant.

## Row-Pattern Expressions

### Why Ordinary `FunctionExpr` Is Not Enough

Several expressions look like ordinary functions but have special row-pattern
scope and navigation semantics. Treating all of them as untyped function names
would make these operations unsafe:

- renaming a pattern variable;
- distinguishing a table qualifier from a pattern-variable qualifier;
- rejecting `FINAL` in `DEFINE`;
- validating navigation offsets;
- determining whether a target dialect has exact semantics;
- rejecting `CLASSIFIER()` outside row-pattern recognition.

Add the following expression nodes to the shared model.

### `PatternColumnExpr`

```java
PatternColumnExpr(Identifier variable, Identifier column)
```

Represents `down_row.amount`. This is deliberately not a regular qualified
`ColumnExpr`; the qualifier names a primary or union pattern variable rather
than a table. Unqualified input columns remain ordinary `ColumnExpr` nodes.

### `ClassifierExpr`

```java
ClassifierExpr(Identifier variable)
```

Represents `CLASSIFIER()` when `variable` is null and a dialect-supported
`CLASSIFIER(variable)` when present. Oracle validation accepts only the
argument-free baseline. Future Trino support can allow a primary or union
variable argument.

### `MatchNumberExpr`

A leaf expression representing `MATCH_NUMBER()`. It is distinct from a
catalog function because it is legal only inside a row-pattern expression
scope and returns the match ordinal within a partition.

### `PatternNavigationExpr`

```java
PatternNavigationExpr(
    PatternNavigationExpr.Kind kind,
    Expression expression,
    Expression offset
)
```

Kinds are `FIRST`, `LAST`, `PREV`, and `NEXT`. Offset is optional. Validation
owns these distinctions:

- Oracle `FIRST`/`LAST` default offset is zero.
- Oracle `PREV`/`NEXT` default offset is one.
- An explicit offset must be a non-negative runtime constant when that can be
  established statically.
- `PREV`/`NEXT` inputs must contain row-pattern column references, and multiple
  references must use the same pattern variable for Oracle.
- Compound navigation such as `PREV(LAST(A.amount))` is representable by
  nesting nodes.

### `PatternEvaluationExpr`

```java
PatternEvaluationExpr(
    PatternEvaluationExpr.Mode mode,
    Expression expression
)
```

Modes are `RUNNING` and `FINAL`. This wrapper can contain `FIRST`, `LAST`, or an
allowed aggregate expression. Oracle validation rejects:

- `FINAL` anywhere in `DEFINE`;
- either modifier around `PREV` or `NEXT`;
- modifiers around expressions that do not support row-pattern evaluation
  semantics.

Ordinary aggregates remain `FunctionExpr`. The wrapper supplies the special
evaluation mode without creating duplicate aggregate node families.

### Expression Matcher And Scope

Add branches for all five expression types to the existing expression matcher
API. A separate row-pattern-expression matcher is unnecessary unless later
implementations demonstrate a real independent variant-selection use case.

Each expression implements its own `accept()` method. Do not add
`instanceof`-based dispatch in `Expression`.

The parser must only produce these nodes while parsing a `PatternMeasure`
or `PatternDefinition`. The existing `ParseContext.callstack()` provides
the necessary scope signal while nested expression and predicate parsers run;
no mutable row-pattern parser state object is needed.

## Builder And DSL Design

### Builder

Add `PatternRecognitionTable.Builder` with a mutable construction surface and immutable
result:

```java
matchRecognize(table("sales"))
    .partitionBy(col("customer_id"))
    .orderBy(col("sale_date").asc())
    .measure(matchNumber(), "match_no")
    .measure(first(patternColumn("start_row", "sale_date")), "start_date")
    .measure(last(patternColumn("up_row", "sale_date")), "end_date")
    .oneRowPerMatch()
    .skipPastLastRow()
    .pattern(patternSequence(
        patternVar("start_row"),
        oneOrMore(patternVar("down_row")),
        oneOrMore(patternVar("up_row"))))
    .define("down_row", patternColumn("down_row", "amount")
        .lt(prev(patternColumn("down_row", "amount"))))
    .define("up_row", patternColumn("up_row", "amount")
        .gt(prev(patternColumn("up_row", "amount"))))
    .as("mr")
    .build();
```

Required builder behavior:

- `partitionBy(...)`, `orderBy(...)`, `measure(...)`, `rowsPerMatch(...)`,
  `afterMatchSkip(...)`, `pattern(...)`, `subset(...)`, `define(...)`, and
  `as(...)`.
- Repeated `measure`, `subset`, and `define` calls append in source order.
- `build()` supplies semantic defaults for rows-per-match and after-match skip.
- `build()` requires a pattern and at least one definition, but leaves
  dialect-dependent cross-field rules to validation.
- `builder(existing)` must preserve every field.

### DSL Helpers

Add public, documented helpers to `Dsl` for every new public model surface:

```text
matchRecognize(source)
patternVar(name)
patternColumn(variable, column)
patternSequence(patterns...)
patternAlternation(patterns...)
patternPermute(patterns...)
patternStart()
patternEnd()
emptyPattern()
excludePattern(pattern)
zeroOrMore(pattern)
oneOrMore(pattern)
optionalPattern(pattern)
repeatPattern(pattern, minimum, maximum, reluctant)
oneRowPerMatch()
allRowsPerMatch()
skipPastLastRow()
skipToNextRow()
skipToPattern(variable)
skipToFirst(variable)
skipToLast(variable)
classifier()
classifier(variable)
matchNumber()
first(expression[, offset])
last(expression[, offset])
prev(expression[, offset])
next(expression[, offset])
running(expression)
finalValue(expression)
```

Where Java overloads improve expression ergonomics, they are DSL overloads,
not multiple canonical node factories. All public methods require JavaDoc.

`PartitionBy` JavaDoc must be generalized from “used in OVER” to a shared
partition clause used by windows and row-pattern recognition.

## Parser Design

### Lexer Work

Add dedicated `TokenType` entries and lexer keyword mappings for structural
grammar words rather than matching them as generic identifiers:

```text
MATCH_RECOGNIZE, MEASURES, PATTERN, DEFINE, SUBSET, PERMUTE,
ONE, PER, MATCH, AFTER, PAST, SHOW, OMIT, UNMATCHED, RUNNING, FINAL
```

Existing tokens already cover `PARTITION`, `ORDER`, `BY`, `ALL`, `ROWS`,
`SKIP`, `TO`, `NEXT`, `FIRST`, and `LAST`.

Add `LBRACE` and `RBRACE` punctuation tokens because bounded quantifiers are
currently not lexable. Pattern punctuation otherwise maps as follows:

| Syntax | Token representation |
|---|---|
| `^`, `|`, `*`, `+`, `-` | `OPERATOR` with checked lexeme |
| `?` | existing `QMARK` |
| `$` | existing `DOLLAR` |
| `{`, `}` | new `LBRACE`, `RBRACE` |
| `{-`, `-}` | brace token plus checked `-` operator token |

Before globally classifying context function names such as `CLASSIFIER` and
`MATCH_NUMBER` as keywords, add lexer regression tests for those words as
identifiers. Prefer dedicated tokens only if the supported dialects reserve
them; otherwise the scoped row-pattern expression parser can match their
`IDENT` lexemes. This keeps unrelated schemas with columns of those names from
breaking.

Required lexer tests include every operator without whitespace, especially:

```text
A*?
A??
A{2,5}?
{-A+-}
^A+$
```

### Table Transform Parser

Add an ANSI `PatternRecognitionTableParser` implementing:

```java
MatchableParser<PatternRecognitionTable>, InfixParser<TableRef, PatternRecognitionTable>
```

Responsibilities:

1. Match `MATCH_RECOGNIZE` without consuming input.
2. Require `SqlFeature.MATCH_RECOGNIZE` after committing to the branch.
3. Parse the source supplied by `TableRefParser` plus the parenthesized clause.
4. Delegate every child node to its registered parser.
5. Parse an optional result alias after the closing parenthesis.
6. Return `PatternRecognitionTable.of(...)`; do not fold pattern parsing into this
   parent parser.

Register it with the ANSI parser repository and add it to
`TableRefParser.parseRelationTransforms(...)`. Because it is capability-gated,
ANSI SQL-2008 continues to reject it. Oracle inherits the parser and only adds
an override if an actual Oracle grammar difference is discovered.

Relation-transform ordering tests must cover:

- source table followed by `MATCH_RECOGNIZE`;
- parenthesized query source followed by `MATCH_RECOGNIZE`;
- row-pattern output followed by a supported later transform;
- row-pattern output used as a join operand;
- alias consumption at the correct transform boundary.

### Child Parsers

Add dedicated registered parsers for:

- `PatternMeasure`
- `RowsPerMatch`
- `AfterMatchSkip`
- `MatchPattern`
- each concrete `MatchPattern` variant
- `PatternSubset`
- `PatternDefinition`
- each row-pattern expression node

The row-pattern parser should use precedence-aware recursive descent:

```text
alternation    := concatenation ('|' concatenation)*
concatenation  := quantified+
quantified     := primary quantifier?
primary        := variable
                | '^'
                | '$'
                | '()'
                | '(' alternation ')'
                | '{-' alternation '-}'
                | 'PERMUTE' '(' alternation (',' alternation)+ ')'
```

This grammar is conceptual. The implementation must distinguish an empty group
from a grouped pattern and stop concatenation at `|`, `)`, `,`, and `-}`.

Use `cur.expect(...)` after consuming a branch keyword. Invalid committed
syntax should report the missing element, not fall through to a later generic
parser.

### Clause Ordering

For the Oracle and standard-compatible baseline, parse clauses in this order:

1. optional `PARTITION BY`
2. optional `ORDER BY`
3. optional `MEASURES`
4. optional rows-per-match
5. optional `AFTER MATCH SKIP`
6. required `PATTERN`
7. optional `SUBSET`
8. required `DEFINE`

Do not accept reordered clauses merely because all fields fit the model.
Dialect parsers may override small ordering hooks if a future engine differs;
refactor the base parser into hooks instead of copying it.

### Scoped Expression Parsing

While a `PatternMeasure` or `PatternDefinition` is on the parse call
stack:

- A two-part `variable.column` reference parses as `PatternColumnExpr`.
  In this scope the first part denotes a pattern variable, not an input-table
  qualifier; resolution is deferred because `DEFINE` and `SUBSET` declarations
  can occur later in the clause.
- `CLASSIFIER`, `MATCH_NUMBER`, navigation expressions, and
  `RUNNING`/`FINAL` use their dedicated parsers.
- Other expressions continue through the ordinary expression parser.

Forward references are legal, so parsing cannot require the final declaration
set. The validator resolves pattern-variable names after the full
`PatternRecognitionTable` exists.

Outside that scope, the same text retains its ordinary SQL meaning or produces
the normal dialect error. No row-pattern expression node may leak into a
general expression parse solely because a function has the same name.

## Renderer Design

### Shared Renderer

Add capability-gated ANSI renderers for `PatternRecognitionTable` and every child node.
The standard-compatible renderer is reusable by Oracle; add Oracle overrides
only for demonstrated behavior differences.

Canonical shape:

```sql
<source>
MATCH_RECOGNIZE (
    [PARTITION BY ...]
    [ORDER BY ...]
    [MEASURES ...]
    ONE ROW PER MATCH | ALL ROWS PER MATCH
    AFTER MATCH SKIP ...
    PATTERN (...)
    [SUBSET ...]
    [DEFINE ...]
) <alias>
```

Oracle table aliases must render without `AS`, consistent with the existing
Oracle table-alias policy. Measure aliases may render with `AS` because Oracle
allows it for output columns.

The renderer must:

- check `SqlFeature.MATCH_RECOGNIZE` before emitting SQL;
- delegate source and every child node through the renderer repository;
- render the stored semantic defaults canonically;
- apply the pattern precedence rules above;
- reject unsupported dialect options before emitting partial SQL;
- never fall back to `toString()` or raw pattern text.

### Unsupported Renderers

Every semantic node has an ANSI renderer pair as required by the repository
model rules. Unsupported dialects fail through capability checks with
`UnsupportedDialectFeatureException` naming `MATCH_RECOGNIZE`.

PostgreSQL, MySQL, and SQL Server must not inherit a renderer that silently
emits syntax their engines do not support.

## Validation Design

Validation is split into structural, scope, expression, dialect, and schema
layers. Constructor checks are intentionally limited to local invariants.

### Structural Rules

- A pattern is required.
- Measure aliases are required and unique under dialect identifier comparison.
- Definition variables are unique.
- Subset names are unique.
- Subset membership is non-empty and contains no duplicate variable.
- Rows-per-match and empty-match handling form a valid pair.
- After-match kind, position, and target form a valid shape.
- Quantifier bounds are valid.
- Pattern concatenations, alternations, and permutations meet minimum arity.

### Pattern Scope Rules

Build the primary-variable set from `MatchPattern.Variable` occurrences in the
pattern, using dialect-aware identifier equality.

- Oracle requires every definition name to identify a primary variable used by
  the pattern.
- A primary variable may have at most one definition.
- A union variable name must not collide with a primary variable or another
  union variable.
- Every union member must identify a primary variable.
- A union variable cannot appear as a primary pattern element.
- Every `PatternColumnExpr.variable` must resolve to a primary or union
  variable visible in the clause.
- A variable-target skip must resolve to a supported target. Oracle requires a
  primary variable with an explicit `DEFINE` item; future Trino support may
  also allow a union variable.
- `CLASSIFIER(variable)` must resolve when the dialect permits an argument.
- Renaming and identifier transforms must update declarations and all scoped
  references together.

Oracle requires every primary variable used in the pattern to have exactly one
explicit `DEFINE` item. Future Snowflake or Trino validation may permit primary
variables omitted from the definition list and treat their condition as
implicit `TRUE`.

### Expression Rules

- Row-pattern-only expressions are invalid outside `PatternMeasure` and
  `PatternDefinition` descendants.
- Every definition condition must be boolean-compatible when type information
  is available.
- `FINAL` is invalid in `DEFINE` for Oracle.
- `RUNNING`/`FINAL` wrapping is limited to supported navigation or aggregate
  expressions.
- `PREV`/`NEXT` cannot carry `RUNNING`/`FINAL` in Oracle.
- Navigation offsets must be non-negative runtime constants when statically
  knowable.
- Oracle pattern aggregates are limited to the supported aggregate family and
  reject `DISTINCT` where Oracle row-pattern semantics reject it.
- Window functions inside row-pattern expressions are rejected unless a
  dialect explicitly permits the exact form.

### Dialect And Version Rules

Add:

```java
SqlFeature.MATCH_RECOGNIZE
```

Oracle capabilities enable it from `SqlDialectVersion.of(12, 1)`. Oracle 11g
and earlier reject parsing, validation, and rendering.

For currently shipped dialects:

| Dialect | Behavior |
|---|---|
| Oracle 12.1+ | Parse, validate, and render the R11-8 Oracle subset. |
| Oracle < 12.1 | Explicit unsupported-feature error. |
| ANSI SQL-2008 | Explicit unsupported-feature error. A future SQL:2016 capability may enable the shared grammar. |
| PostgreSQL | Explicit unsupported-feature error. |
| MySQL | Explicit unsupported-feature error. |
| SQL Server | Explicit unsupported-feature error. |

Future dialect capabilities should be granular only where one coarse feature
cannot produce a clear validation outcome. Likely optional additions are:

```text
MATCH_RECOGNIZE_SUBSET
MATCH_RECOGNIZE_PERMUTE
MATCH_RECOGNIZE_EXCLUSION
MATCH_RECOGNIZE_ALL_ROWS
MATCH_RECOGNIZE_UNMATCHED_ROWS
MATCH_RECOGNIZE_SKIP_TO_VARIABLE
MATCH_RECOGNIZE_RUNNING_FINAL
```

Do not add all of these preemptively for Oracle-only delivery. Start with the
coarse feature and explicit Oracle validation; introduce granular capabilities
when a second implemented dialect needs them.

### Schema Validation And Output Shape

Schema validation should validate input references conservatively:

- `PARTITION BY` and `ORDER BY` expressions resolve against the source
  relation.
- Unqualified and pattern-variable column expressions inside measures and
  definitions resolve their column name against the source relation.
- Pattern-variable qualifiers are scope variables, not catalog relations.

Output inference:

- For `ONE ROW PER MATCH`, visible output columns are partition columns with
  nameable column expressions plus measure aliases.
- For `ALL ROWS PER MATCH`, engines expose a larger input-derived shape. Until
  the schema layer can model the dialect rules exactly, preserve the relation
  as partially known rather than inventing columns.
- Duplicate inferred output names produce the repository's existing ambiguous
  or duplicate-column diagnostics where applicable.

Lack of complete output inference must not block parsing, rendering, or
structure-only validation.

## Visitors, Transformers, Matchers, And JSON

### Visitors

Add a `PatternRecognitionVisitor<R>` and extend `NodeVisitor<R>` with it. It covers:

- `PatternRecognitionTable`
- all clause item nodes
- all pattern variants
- all row-pattern expression nodes

Leaf nodes still receive dedicated visitor methods. `RecursiveNodeVisitor`
must traverse every child in stable source order.

### Transformers

`RecursiveNodeTransformer` must:

- recursively transform the source, partition/order expressions, measures,
  pattern, subsets, definitions, and aliases where identifier transforms
  apply;
- return the original node instance when all children and identifiers are
  unchanged;
- return a new immutable node when any child changes;
- preserve list order and structural sharing;
- treat pattern-variable declaration/reference renaming consistently.

Add focused identity and changed-child tests for `PatternRecognitionTable`, every
composite pattern variant, and nested row-pattern expressions.

### Matchers

- Add `patternRecognition(...)` to `TableRefMatch`.
- Add expression matcher branches for row-pattern expressions.
- Add `PatternMatch` because `MatchPattern` is a genuine variant family.
- Do not add dedicated matcher wrappers for `PatternMeasure`,
  `PatternDefinition`, `PatternSubset`, rows-per-match, or after-match
  skip; each has one implementation.

### JSON

Add mixins/subtype aliases for:

- `PatternRecognitionTable` in the `TableRef` family;
- the `MatchPattern` sealed family and every variant;
- all row-pattern expression variants in the expression family;
- all single-implementation clause item nodes.

JSON tests must cover:

- a full clause round trip;
- every pattern variant;
- every special expression variant;
- quoted pattern variables and aliases;
- semantic defaults;
- equality of the reconstructed immutable tree.

No JSON property may contain raw SQL for the pattern or definitions.

## Transpilation Design

### Default Rule

Add `MatchRecognizeUnsupportedRule` with diagnostic code:

```text
UNSUPPORTED_MATCH_RECOGNIZE
```

The rule detects `PatternRecognitionTable` anywhere in a statement, including nested
queries and DML query sources. If the target dialect lacks exact native
support, transpilation fails with a problem that identifies the relation path.

### Exact Transpilation

Exact pass-through is possible only when:

- source and target both support `MATCH_RECOGNIZE`;
- every pattern variant is supported;
- rows-per-match and empty/unmatched behavior match;
- after-match skip behavior matches;
- subset and exclusion semantics match;
- special functions and running/final semantics match;
- output-column semantics match.

When future row-pattern dialects are implemented, add focused exact rules or a
compatibility checker that reports the first unsupported option. Do not assume
that common spelling proves exact semantics.

### No Approximate Rewrite In R11-8

Do not rewrite row-pattern recognition to windows, joins, recursive CTEs, or
procedural SQL in R11-8. Correct rewrites depend on match preference,
backtracking, empty matches, overlapping matches, exclusions, navigation, and
output cardinality. A useful approximation would require its own design and
explicit opt-in diagnostics.

## Codegen

`SqmDslVisitor` must emit only public DSL and builder calls. It must not:

- instantiate `Impl` records;
- emit raw pattern text;
- collapse a typed row-pattern expression to `func("...")`;
- lose greedy versus reluctant quantifiers;
- lose default/explicit semantic options;
- confuse pattern-variable columns with qualified table columns.

Golden codegen tests should parse representative Oracle SQL, generate Java,
compile or reparse the generated model using the existing test pattern, and
assert normalized Oracle SQL equivalence.

## Parser And Renderer Registration

The implementation must update:

- ANSI parser registry with the shared capability-gated parser pairs.
- `TableRefParser` relation-transform loop.
- ANSI renderer registry with shared capability-gated renderer pairs.
- Oracle capabilities with the 12.1 version gate.
- Oracle parser/renderer registries only if a behavior override is actually
  necessary.
- Validation dialect rule registries.
- JSON mixin registration.
- Codegen visitor registration through visitor methods.

This design intentionally avoids empty Oracle adapters. Shared syntax belongs
in the ANSI implementation; Oracle packages override only real differences.

## Testing Strategy

### Core Unit Tests

- Constructor null, copy, and arity invariants.
- All canonical quantifier forms and invalid bounds.
- Builder defaults and builder-copy fidelity.
- Alias immutability.
- Pattern precedence trees.
- Visitor coverage for every node.
- Recursive transformer same-instance and changed-instance behavior.
- Matcher coverage.
- JSON round trips.

### Lexer Tests

- Every new keyword and punctuation token.
- No-whitespace pattern operators.
- Reluctant quantifiers.
- Bounded quantifiers.
- Exclusion delimiters.
- Quoted identifiers that equal row-pattern keywords.
- Regression coverage for ordinary columns/functions named `classifier`,
  `pattern`, `define`, and `measures` where the dialect permits them.

### Parser Tests

Happy paths:

- minimal `PATTERN` with its required `DEFINE` item;
- partitioning and multi-column ordering;
- multiple measures;
- one and all rows per match;
- every skip mode;
- concatenation, alternation, grouping precedence, anchors, empty pattern,
  permutation, exclusion, bounded/unbounded, greedy/reluctant quantifiers;
- subsets;
- pattern-column references;
- classifier, match number, navigation, compound navigation, aggregates, and
  running/final wrappers;
- query-table source, alias, join use, and nested query use;
- quoted identifiers and case-sensitive variables.

Failure paths:

- unsupported dialect/version;
- missing `(`, `)`, pattern, alias, definition condition, or quantifier bound;
- illegal clause ordering;
- duplicate definitions/subsets/measures;
- missing, unresolved, or colliding variables and definitions;
- union variable used as a primary pattern element;
- invalid skip target;
- malformed exclusion and permutation;
- invalid quantifier bounds;
- row-pattern expressions outside their scope;
- invalid `FINAL`, navigation offset, aggregate, or window expression;
- trailing tokens after a committed clause.

Every child parser gets a focused test class in addition to whole-clause tests.

### Renderer Tests

- Canonical SQL for every node and option.
- Pattern precedence and minimum parentheses.
- Oracle result aliases without `AS`.
- Measure aliases in legal Oracle form.
- Unsupported dialect/version rejection before partial rendering.
- Parser-renderer-parser semantic round trips.

### Validation Tests

- Every structural, scope, expression, dialect, version, and schema rule.
- Problems identify stable clause paths such as:

```text
from.matchRecognize.pattern
from.matchRecognize.measures[1]
from.matchRecognize.subsets[0]
from.matchRecognize.definitions[2]
```

### Codegen Tests

- One compact baseline example.
- One full-pattern example containing every pattern family.
- One expression-heavy example.
- Generated code uses the DSL and never `Impl` constructors.

### Oracle Live-Engine Tests

Extend `OracleExecutionCases` and `OracleDslExecutionIT` with deterministic
fixtures and assertions for at least:

1. one row per match over two partitions;
2. all rows per match;
3. overlapping matches with `SKIP TO NEXT ROW`;
4. skip to a named variable;
5. greedy versus reluctant quantifiers;
6. bounded quantifiers;
7. alternation and anchors;
8. `PERMUTE`;
9. exclusion with all rows per match;
10. `SUBSET` referenced by a measure;
11. `MATCH_NUMBER()` and `CLASSIFIER()`;
12. `FIRST`, `LAST`, `PREV`, and compound navigation;
13. running versus final measure semantics;
14. query-table input and result alias;
15. DSL-built execution for representative cases, not parser-only SQL.

Keep datasets small and fully ordered. Assertions must compare complete result
rows, not merely successful execution. Container startup logging remains
enabled through the shared execution harness.

## Documentation Updates At Implementation Time

Update all of the following as part of implementation:

- `docs/model/MODEL.md`
- `docs/validation/VALIDATION_FEATURES.md`
- `docs/transpilation/SQL_TRANSPILATION_DESIGN.md`
- `docs/transpilation/SQL_TRANSPILATION_USAGE.md`
- `docs/downstream/DOWNSTREAM_SUPPORT_MATRIX.md`
- Oracle user-facing dialect documentation or README sections
- DSL usage examples
- JSON serialization documentation if subtype listings are maintained there

The model diagram must show `PatternRecognitionTable` under `TableRef`, the
`MatchPattern` family, clause item nodes, and row-pattern expression variants.

## Implementation Stories

### R11-8A: Core Pattern And Relation Model

Deliver:

- `PatternRecognitionTable` and builder;
- clause item nodes;
- complete `MatchPattern` family;
- special expression nodes;
- DSL;
- visitors, transformers, matchers, JSON;
- model documentation and core tests.

Acceptance:

- All public surfaces have JavaDoc.
- No raw SQL pattern field exists.
- Every node is immutable and transformer structural-sharing tests pass.
- Every public node is reachable through DSL helpers.

### R11-8B: Shared Parser And Renderer Infrastructure

Deliver:

- tokens and lexer support;
- dedicated parsers/renderers for every node;
- relation-transform registration;
- pattern precedence parser/renderer;
- scoped expression parsing;
- unsupported ANSI behavior.

Acceptance:

- Full node-level unit coverage.
- ANSI SQL-2008 rejects explicitly.
- Parser/render round trips preserve semantic trees.
- No dialect parser or renderer duplicates the full shared implementation.

### R11-8C: Oracle Support And Validation

Deliver:

- Oracle 12.1 capability gate;
- Oracle shape, scope, function, and expression validation;
- Oracle alias behavior;
- unsupported behavior for Oracle 11g and current non-supporting dialects;
- validation documentation.

Acceptance:

- Parser, renderer, and validator agree on every supported option.
- Unsupported combinations fail with specific diagnostics.
- PostgreSQL, MySQL, and SQL Server cannot render the shared node.

### R11-8D: Transpilation And Codegen

Deliver:

- unsupported transpilation rule;
- exact-native compatibility hooks for future dialects;
- DSL code generation for every node;
- codegen and transpilation documentation/tests.

Acceptance:

- No approximate rewrite is performed.
- Nested row-pattern relations are detected.
- Generated Java uses only public DSL/builders and reconstructs the tree.

### R11-8E: Oracle Live-Engine Coverage And Completion

Deliver:

- deterministic Oracle execution fixtures;
- the broad live-engine matrix listed above;
- downstream matrix and user documentation updates;
- final R11 status update.

Acceptance:

- All Oracle live cases assert result contents.
- The dedicated Oracle integration-test selection works in Maven and CI.
- Unit, integration, JSON, codegen, validation, and transpilation suites pass.
- `MODEL` remains explicitly deferred and legacy `(+)` remains explicitly
  unsupported; neither receives a raw-string fallback.

## Risks And Mitigations

| Risk | Mitigation |
|---|---|
| Pattern grammar changes expression/operator lexing | Add dedicated brace tokens, scoped parsing, and lexer regression tests before parser work. |
| Pattern variable qualifiers are mistaken for table aliases | Use `PatternColumnExpr` and validate within the completed clause scope. |
| The main parser becomes monolithic | Require dedicated child parsers and small overridable hooks. |
| Cross-dialect model drifts toward Oracle spelling | Keep semantic node names generic and document future dialect validation differences. |
| Renderer changes pattern meaning through parentheses | Use explicit precedence and tree-based golden tests. |
| Output schema is overclaimed | Infer only exact one-row output names; mark other output shape partially known. |
| Transpilation silently changes matches | Reject unless all modeled semantics are exactly supported. |
| Live tests become slow or nondeterministic | Reuse one container, use tiny ordered fixtures, and assert deterministic rows. |

## Completion Criteria

`MATCH_RECOGNIZE` implementation is complete only when:

- the typed shared model and DSL cover the full Oracle R11-8 subset;
- every node has parser, renderer, visitor, transformer, matcher-as-needed, and
  JSON coverage;
- Oracle 12.1+ parses, validates, renders, code-generates, and executes the
  supported forms;
- Oracle 11g, ANSI SQL-2008, PostgreSQL, MySQL, and SQL Server reject explicitly
  at the appropriate layers;
- transpilation is exact or explicitly unsupported;
- model, validation, transpilation, downstream, and user documentation is
  current;
- unit and broad Oracle live-engine tests pass;
- no raw SQL escape hatch exists for patterns, definitions, measures, or
  special row-pattern expressions.
