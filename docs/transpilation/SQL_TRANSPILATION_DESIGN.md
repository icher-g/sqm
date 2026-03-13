# SQL Transpilation Design

This document proposes a first-class SQL transpilation layer for SQM so that a statement can be parsed in one dialect, transformed with dialect-aware semantic rewrite rules, validated against a target dialect, and rendered in target syntax.

The goal is not cosmetic SQL reformatting. The goal is controlled, inspectable conversion of a statement from one SQL dialect to another, with explicit handling for:

- exact rewrites
- approximate rewrites
- unsupported constructs
- target capability validation
- schema-aware semantic differences

## Problem Statement

SQM already provides the core pipeline:

1. parse SQL into a shared immutable AST
2. optionally rewrite the AST
3. validate the AST
4. render SQL for a target dialect

That is a strong foundation, but true cross-dialect conversion needs one additional layer between parse and render:

```text
source SQL
  -> parse with source dialect
  -> normalize/canonicalize
  -> transpile to target semantics
  -> validate against target dialect
  -> render with target dialect
  -> transpilation report
```

Without this explicit layer, conversion logic tends to leak into renderers, which creates several problems:

- renderers become responsible for semantic rewrites rather than syntax emission
- unsupported target behavior is discovered too late
- lossy rewrites cannot be reported clearly
- rule provenance is hard to audit and test

SQM's current renderer guidance is already clear: unsupported features should be rejected, not silently rewritten. The transpilation layer preserves that contract.

## Scope

This design covers:

- query and DML statement transpilation
- dialect-to-dialect AST rewrites
- rule metadata and rewrite reporting
- integration with existing validator and renderer flows
- exact vs approximate vs unsupported outcomes

This design does not require:

- a fully canonical SQL IR independent of `sqm-core`
- automatic schema migration or DDL conversion
- guaranteed conversion for every dialect-specific feature
- optimizer-grade semantic equivalence proofs

## Design Principles

### 1. Transpilation is a separate phase

Parsing identifies what the source statement means in its own dialect.
Rendering emits syntax for the target dialect.
Transpilation bridges the semantic gap between them.

### 2. Unsupported is a valid outcome

Some features do not have a safe target equivalent. The API must make that explicit instead of forcing a best-effort render.

### 3. Rewrites must be inspectable

Every rewrite should have:

- a stable rule id
- a source dialect
- a target dialect
- a classification
- an explanation
- optional warnings

### 4. Portable semantics belong in `sqm-core`

If a construct is conceptually dialect-neutral, model it in `sqm-core` and let parsers map dialect syntax into that node.

If a construct is truly dialect-specific, it may remain a `DialectNode`, but transpilation must either:

- rewrite it into portable core nodes
- rewrite it into target-specific nodes
- reject it as unsupported

### 5. Validation happens after transpilation

The rewritten AST must be validated against target capabilities and target schema rules before rendering.

## Existing Foundation in SQM

The repo already has the major building blocks required for this design:

- shared immutable AST in `sqm-core`
- transformer infrastructure in `io.sqm.core.transform`
- statement rewrite pipeline in `sqm-control`
- dialect capabilities in `io.sqm.core.dialect`
- target validation in `sqm-validate`
- target rendering in `sqm-render`

Relevant extension points already exist:

- `io.sqm.render.spi.SqlDialect#beforeRender(...)`
- `io.sqm.control.pipeline.SqlStatementRewriter`
- `io.sqm.core.transform.RecursiveNodeTransformer`
- dialect-specific parser specs and validation dialects

However, `beforeRender(...)` is intentionally too narrow for full transpilation. It is appropriate for render preparation, parameter shaping, and dialect-local normalization, but not for source-to-target semantic conversion.

## Proposed Module

Add a new module:

- `sqm-transpile`

Recommended responsibilities:

- public transpilation API
- rule registry and execution engine
- transpilation report/result types
- source/target dialect descriptors
- shared normalization and preflight pipeline
- reusable dialect-pair rewrite rules

Optional future split if the feature grows:

- `sqm-transpile` for API and engine
- `sqm-transpile-postgresql`
- `sqm-transpile-mysql`
- `sqm-transpile-sqlserver`

For the current repo size, a single `sqm-transpile` module is likely the simplest starting point.

## High-Level Architecture

```text
SqlTranspiler
  -> source parser
  -> normalization pipeline
  -> transpilation rule planner
  -> ordered rewrite execution
  -> target validator
  -> target renderer
  -> TranspileResult
```

### Pipeline Phases

#### 1. Parse

Parse the input using source dialect specs.

Output:

- `Statement` AST
- parser diagnostics if parsing fails

#### 2. Normalize

Apply dialect-safe normalization that does not change semantics.

Examples:

- identifier normalization
- canonical alias layout
- literal parameterization policy if requested
- stable operator/function normalization where semantics are unchanged

This phase should reuse existing rewrite infrastructure where possible.

#### 3. Transpile

Apply ordered source-to-target rewrite rules.

Examples:

- PostgreSQL `ILIKE` to a lowercasing rewrite for targets without native `ILIKE`
- PostgreSQL `DISTINCT ON` to a window-function based rewrite if explicitly enabled
- PostgreSQL `RETURNING` to unsupported for current MySQL

#### 4. Validate Target

Validate the transpiled AST against:

- target dialect capabilities
- target semantic rules
- optional target catalog/schema

If validation fails, the result should remain inspectable and explain why rendering was not allowed.

#### 5. Render

If validation succeeds, render the final AST using the existing target dialect renderer.

#### 6. Report

Return SQL, the final AST, and a detailed transpilation report.

## Public API Proposal

### Primary Entry Point

```java
package io.sqm.transpile;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.Statement;
import io.sqm.parser.spi.Specs;
import io.sqm.render.spi.SqlDialect;
import io.sqm.validate.schema.SchemaValidationSettings;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Transpiles SQL statements from a source dialect to a target dialect.
 */
public interface SqlTranspiler {
    /**
     * Creates a builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new DefaultSqlTranspiler.Builder();
    }

    /**
     * Transpiles SQL text.
     *
     * @param sql source SQL text
     * @return transpilation result
     */
    TranspileResult transpile(String sql);

    /**
     * Transpiles an already parsed statement.
     *
     * @param statement source statement model
     * @return transpilation result
     */
    TranspileResult transpile(Statement statement);

    /**
     * Builder for transpiler instances.
     */
    interface Builder {
        Builder sourceDialect(SqlDialectId sourceDialect);

        Builder targetDialect(SqlDialectId targetDialect);

        Builder parser(Supplier<Specs> sourceSpecsFactory);

        Builder renderer(Supplier<SqlDialect> targetDialectFactory);

        Builder targetValidation(Supplier<SchemaValidationSettings> targetValidationFactory);

        Builder sourceSchema(CatalogSchema sourceSchema);

        Builder targetSchema(CatalogSchema targetSchema);

        Builder options(TranspileOptions options);

        Builder registry(TranspileRuleRegistry registry);

        SqlTranspiler build();
    }
}
```

### Dialect Identifier

```java
package io.sqm.transpile;

/**
 * Stable identifier for a SQL dialect family.
 *
 * @param value normalized dialect id
 */
public record SqlDialectId(String value) {
    public SqlDialectId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static SqlDialectId of(String value) {
        return new SqlDialectId(value.trim().toLowerCase());
    }
}
```

### Result Model

```java
package io.sqm.transpile;

import io.sqm.core.Statement;
import io.sqm.validate.api.ValidationProblem;

import java.util.List;
import java.util.Optional;

/**
 * Result of a transpilation attempt.
 *
 * @param status overall result status
 * @param sourceAst parsed source AST when available
 * @param transpiledAst final AST after rewrite when available
 * @param sql rendered target SQL when available
 * @param steps applied transpilation steps
 * @param problems errors or blocking diagnostics
 * @param warnings non-blocking diagnostics
 */
public record TranspileResult(
    TranspileStatus status,
    Optional<Statement> sourceAst,
    Optional<Statement> transpiledAst,
    Optional<String> sql,
    List<TranspileStep> steps,
    List<TranspileProblem> problems,
    List<TranspileWarning> warnings
) {
    public boolean success() {
        return status == TranspileStatus.SUCCESS || status == TranspileStatus.SUCCESS_WITH_WARNINGS;
    }
}
```

### Status and Diagnostics

```java
package io.sqm.transpile;

/**
 * Overall transpilation outcome.
 */
public enum TranspileStatus {
    PARSE_FAILED,
    UNSUPPORTED,
    VALIDATION_FAILED,
    RENDER_FAILED,
    SUCCESS,
    SUCCESS_WITH_WARNINGS
}
```

```java
package io.sqm.transpile;

/**
 * Severity of a transpilation rule outcome.
 */
public enum TranspileSeverity {
    INFO,
    WARNING,
    ERROR
}
```

```java
package io.sqm.transpile;

/**
 * Describes whether a rewrite preserves semantics exactly.
 */
public enum RewriteFidelity {
    EXACT,
    APPROXIMATE,
    UNSUPPORTED
}
```

```java
package io.sqm.transpile;

import java.util.Optional;

/**
 * A single applied or considered transpilation step.
 *
 * @param ruleId stable rewrite rule identifier
 * @param fidelity rewrite fidelity
 * @param description human-readable summary
 * @param changed whether the rule changed the AST
 */
public record TranspileStep(
    String ruleId,
    RewriteFidelity fidelity,
    String description,
    boolean changed
) {
}
```

```java
package io.sqm.transpile;

/**
 * Blocking problem encountered during transpilation.
 *
 * @param code stable code
 * @param message description
 * @param stage pipeline stage
 */
public record TranspileProblem(
    String code,
    String message,
    TranspileStage stage
) {
}
```

```java
package io.sqm.transpile;

/**
 * Non-blocking transpilation warning.
 *
 * @param code stable code
 * @param message description
 */
public record TranspileWarning(
    String code,
    String message
) {
}
```

```java
package io.sqm.transpile;

/**
 * Pipeline stage used for diagnostics.
 */
public enum TranspileStage {
    PARSE,
    NORMALIZE,
    REWRITE,
    VALIDATE,
    RENDER
}
```

### Options

```java
package io.sqm.transpile;

/**
 * Options controlling transpilation behavior.
 *
 * @param allowApproximateRewrites whether lossy rewrites may be applied
 * @param failOnWarnings whether warnings should fail the operation
 * @param validateTarget whether target validation is enabled
 * @param renderSql whether SQL rendering should be attempted on success
 */
public record TranspileOptions(
    boolean allowApproximateRewrites,
    boolean failOnWarnings,
    boolean validateTarget,
    boolean renderSql
) {
    public static TranspileOptions defaults() {
        return new TranspileOptions(false, false, true, true);
    }
}
```

## Rule Model

The key abstraction is a source/target-aware rewrite rule that can:

- decide whether it applies
- inspect AST nodes
- transform the statement
- emit diagnostics
- declare whether it is exact or approximate

Rules should not be constrained to exactly one dialect pair.

Some rules are naturally pair-specific, but others are reusable across multiple mappings.

Examples:

- PostgreSQL operator syntax lowered to a function form accepted by several targets
- a shared rejection rule for one source construct across multiple targets
- a normalization rule that applies to many targets in the same function family

The registry should therefore build a concrete execution plan for a specific `(source, target)` request from a reusable rule set.

### Rule Interface

```java
package io.sqm.transpile.rule;

import io.sqm.core.Statement;
import io.sqm.transpile.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;

import java.util.Set;

/**
 * Source-to-target transpilation rule.
 *
 * <p>A rule may be reusable across multiple source/target mappings.</p>
 */
public interface TranspileRule {
    /**
     * Stable rule id.
     *
     * @return rule id
     */
    String id();

    /**
     * Source dialects supported by this rule.
     *
     * <p>An empty set means applicability is determined entirely at runtime.</p>
     *
     * @return supported source dialects
     */
    Set<SqlDialectId> sourceDialects();

    /**
     * Target dialects supported by this rule.
     *
     * <p>An empty set means applicability is determined entirely at runtime.</p>
     *
     * @return supported target dialects
     */
    Set<SqlDialectId> targetDialects();

    /**
     * Order of execution. Lower values run first.
     *
     * @return order
     */
    default int order() {
        return 0;
    }

    /**
     * Indicates whether the rule may run for the current request.
     *
     * <p>The default implementation checks the declared source and target
     * dialect sets. Rules may override this for richer applicability logic.</p>
     *
     * @param context transpilation context
     * @return {@code true} if the rule is applicable
     */
    default boolean supports(TranspileContext context) {
        boolean sourceSupported = sourceDialects().isEmpty() || sourceDialects().contains(context.sourceDialect());
        boolean targetSupported = targetDialects().isEmpty() || targetDialects().contains(context.targetDialect());
        return sourceSupported && targetSupported;
    }

    /**
     * Applies the rule.
     *
     * @param statement current statement
     * @param context transpilation context
     * @return rule result
     */
    TranspileRuleResult apply(Statement statement, TranspileContext context);
}
```

### Rule Result

```java
package io.sqm.transpile;

import io.sqm.core.Statement;

import java.util.List;

/**
 * Result of a single transpilation rule.
 *
 * @param statement output statement
 * @param changed whether the rule changed the AST
 * @param fidelity semantic fidelity of the applied behavior
 * @param warnings emitted warnings
 * @param problems emitted blocking problems
 * @param description step description
 */
public record TranspileRuleResult(
    Statement statement,
    boolean changed,
    RewriteFidelity fidelity,
    List<TranspileWarning> warnings,
    List<TranspileProblem> problems,
    String description
) {
    public static TranspileRuleResult unchanged(Statement statement, String description) {
        return new TranspileRuleResult(statement, false, RewriteFidelity.EXACT, List.of(), List.of(), description);
    }

    public static TranspileRuleResult rewritten(Statement statement, RewriteFidelity fidelity, String description) {
        return new TranspileRuleResult(statement, true, fidelity, List.of(), List.of(), description);
    }

    public static TranspileRuleResult unsupported(Statement statement, String code, String message) {
        return new TranspileRuleResult(
            statement,
            false,
            RewriteFidelity.UNSUPPORTED,
            List.of(),
            List.of(new TranspileProblem(code, message, TranspileStage.REWRITE)),
            message
        );
    }
}
```

### Context

```java
package io.sqm.transpile;

import io.sqm.catalog.model.CatalogSchema;

import java.util.Optional;

/**
 * Runtime context for transpilation rules.
 *
 * @param sourceDialect source dialect id
 * @param targetDialect target dialect id
 * @param options options
 * @param sourceSchema optional source schema
 * @param targetSchema optional target schema
 */
public record TranspileContext(
    SqlDialectId sourceDialect,
    SqlDialectId targetDialect,
    TranspileOptions options,
    Optional<CatalogSchema> sourceSchema,
    Optional<CatalogSchema> targetSchema
) {
}
```

### Rule Registry

```java
package io.sqm.transpile.rule;

import io.sqm.transpile.SqlDialectId;

import java.util.List;

/**
 * Registry of transpilation rules by source/target pair.
 */
public interface TranspileRuleRegistry {
    /**
     * Returns ordered rules for a source/target pair.
     *
     * <p>The returned execution plan may be assembled from reusable rules
     * that support multiple dialect mappings.</p>
     *
     * @param source source dialect
     * @param target target dialect
     * @return ordered rules
     */
    List<TranspileRule> rulesFor(SqlDialectId source, SqlDialectId target);
}
```

Recommended registry behavior:

- keep a reusable global rule set
- select only rules that match the current request
- sort deterministically by `order()` and then `id()`
- allow pair-specific additions or overrides where necessary

## Recommended Internal Class Layout

The following internal classes would keep responsibilities narrow:

- `DefaultSqlTranspiler`
- `DefaultTranspileRuleRegistry`
- `TranspilePlanner`
- `TranspileExecutor`
- `TargetValidationAdapter`
- `DialectServiceRegistry`
- `TranspileBuiltIns`

### `DefaultSqlTranspiler`

Responsibilities:

- parse source SQL if needed
- create `TranspileContext`
- execute normalization and transpilation rules
- run target validation
- invoke target rendering
- assemble `TranspileResult`

### `TranspilePlanner`

Responsibilities:

- fetch ordered rules for `(source, target)`
- merge built-in and custom rules
- ensure deterministic execution order
- filter reusable rules down to the concrete request

This may remain trivial at first and grow later.

### `DialectServiceRegistry`

Responsibilities:

- resolve source parser specs factory
- resolve target renderer dialect factory
- resolve target validation settings factory

This avoids hard-coding all dialect wiring inside `DefaultSqlTranspiler`.

## Integration with Existing SQM Layers

### With Parser Modules

No parser contract changes are required for the first version.

Transpilation should parse with the source dialect exactly as current callers do:

- PostgreSQL input with `PostgresSpecs`
- MySQL input with `MySqlSpecs`
- ANSI input with `AnsiSpecs`

### With `sqm-core`

The transpiler should reuse `RecursiveNodeTransformer` for most rewrites.

Some new `sqm-core` modeling may still be valuable when a construct is portable but currently represented in a dialect-specific way.

Examples worth evaluating:

- a dedicated portable string concatenation expression
- a portable null-safe equality abstraction if multiple dialects support it differently
- richer function metadata in `FunctionExpr`

For the first transpilation milestone, string concatenation should move from "worth evaluating" to an explicit planned core abstraction.

Recommendation:

- introduce `ConcatExpr` in `sqm-core`
- parse PostgreSQL `||` chains into `ConcatExpr`
- parse function-based forms such as `CONCAT(...)` into `ConcatExpr` when they are being used as plain string concatenation
- let dialect renderers decide whether `ConcatExpr` becomes operator syntax or function syntax

This is similar in spirit to `CastExpr`: callers work with a semantic node, while parsers and renderers preserve dialect-specific syntax at the boundaries.

The rule of thumb should be:

- add new core node only if the semantic concept is portable and likely reusable
- keep dialect-specific node if the concept is tied to one family of SQL semantics

### With `sqm-control`

`sqm-control` already has statement rewrite composition. There are two reasonable options:

1. Keep transpilation separate from middleware rewrite rules.
2. Reuse `StatementRewriteRule` internally with an adapter.

Recommendation:

- keep the public transpilation API separate
- optionally reuse the rewrite mechanics internally through adapters

Reason:

- middleware rewrites mostly optimize or enforce policy within one dialect
- transpilation rewrites bridge source and target semantics and need richer reporting

### With Validator Modules

After transpilation, the statement should be passed into the target validator using existing dialect-aware validation settings.

This is required because a rewrite may still produce a target AST that is:

- syntactically renderable
- but semantically invalid for the target dialect or schema

### With Renderer Modules

No renderer contract change is strictly required.

Renderers should continue to:

- render supported target nodes
- reject unsupported nodes

The transpiler should aim to produce a target-valid AST so renderers do not become fallback translators.

## Worked Example: PostgreSQL `||` and `CONCAT(...)` via `ConcatExpr`

### Source SQL

```sql
SELECT first_name || ' ' || last_name AS full_name
FROM users
```

### Source Parse

A PostgreSQL parser should produce a semantic `ConcatExpr` rather than preserving the `||` chain as generic `BinaryOperatorExpr` nodes.

Conceptually:

```text
ConcatExpr(
  args = [first_name, ' ', last_name]
)
```

### Alternate Source Form

MySQL input such as:

```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name
FROM users
```

should also parse to the same semantic node:

```text
ConcatExpr(
  args = [first_name, ' ', last_name]
)
```

### Target Render

The target renderer then chooses the appropriate surface syntax.

Rendered by PostgreSQL:

```sql
SELECT first_name || ' ' || last_name AS full_name
FROM users
```

Rendered by MySQL:

```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name
FROM users
```

### Why This Is Better Than a Transpile Rule

The string concatenation concept is:

- common
- semantic
- reusable across multiple dialects
- not inherently tied to either operator syntax or function syntax

That makes it a strong fit for a dedicated core node:

```text
ConcatExpr(
  args = [first_name, ' ', last_name]
)
```

### Notes

`ConcatExpr` should be treated as a planned first-wave semantic node rather than as a discovery candidate deferred until later.

Transpilation rules may still be useful around concatenation for edge cases or future dialect quirks, but the normal PostgreSQL/MySQL conversion path should not depend on a `|| -> CONCAT(...)` rule.

## Worked Example: PostgreSQL `ILIKE`

### Source SQL

```sql
SELECT *
FROM users
WHERE name ILIKE 'al%'
```

### Candidate Rewrite

Possible target rewrite for a dialect without native `ILIKE`:

```sql
SELECT *
FROM users
WHERE LOWER(name) LIKE LOWER('al%')
```

### Fidelity

This should usually be classified as `APPROXIMATE`, not `EXACT`, because:

- collation behavior may differ
- index usage may differ
- locale-specific case folding may differ

The API should expose that warning clearly.

## Worked Example: PostgreSQL `RETURNING` to MySQL

### Source SQL

```sql
INSERT INTO users(name) VALUES ('alice') RETURNING id
```

### Outcome

For current supported MySQL versions in this repo, the correct transpilation result is likely:

- status: `UNSUPPORTED`
- blocking problem with a stable code such as `UNSUPPORTED_RETURNING`

This is important because there may be application-level ways to emulate the behavior, but they are not SQL-level rewrites inside a single statement.

## Rule Categories

The initial built-in rule set should distinguish several categories.

### 1. Syntax normalization rules

These rules only normalize different syntactic forms of the same concept.

Examples:

- operator spelling normalization
- alias canonicalization
- quote-style normalization in identifiers

### 2. Semantic lowering rules

These rules rewrite a higher-level source construct into more primitive target constructs.

Examples:

- `DISTINCT ON` to a row-number based subquery rewrite
- PostgreSQL `x ILIKE y` to lowercased `LIKE`

Note:

- plain string concatenation should move out of this category once `ConcatExpr` exists in `sqm-core`

### 3. Capability rejection rules

These rules detect constructs with no safe target equivalent and fail early.

Examples:

- `RETURNING` to unsupported target
- PostgreSQL-only JSON path/operator semantics without target equivalent
- dialect-specific lock clauses with no target support

### 4. Schema-aware rewrite rules

These rules require catalog knowledge.

Examples:

- rewriting identifier case based on actual schema objects
- type-sensitive function/operator rewrites
- resolving ambiguous null or text coercion behavior

These should remain optional unless schemas are supplied.

## Rule Ordering

Rule order must be deterministic.

Recommended order:

1. source normalization
2. portable semantic lowering
3. target capability rejection
4. target-specific canonicalization
5. validation

Important constraint:

- rejection rules should run after exact lowerings that may eliminate the unsupported feature
- but before final rendering

## Exact vs Approximate vs Unsupported

Every rule and every transpilation result should make fidelity explicit.

### Exact

The resulting SQL is intended to preserve semantics under supported target behavior.

Examples:

- operator token spelling changes
- some function-name substitutions
- equivalent pagination syntax changes

### Approximate

The resulting SQL is executable and intended to be close in behavior, but semantic differences are known.

Examples:

- `ILIKE` rewritten through `LOWER(...) LIKE LOWER(...)`
- time zone or timestamp coercion rewrites where engine behavior differs
- regex dialect rewrites

### Unsupported

No safe SQL-only rewrite exists inside SQM's scope.

Examples:

- DML `RETURNING` with no single-statement target equivalent
- source-specific operator families with no target analog
- procedural or planner-hint semantics that do not transfer meaningfully

## Failure Model

The transpiler should avoid exceptions for expected incompatibility cases.

Expected incompatibilities should produce `TranspileResult` with diagnostics.

Exceptions should be reserved for:

- invalid builder configuration
- broken internal rule wiring
- impossible invariant violations

## Testing Strategy

This feature needs both rule-unit tests and end-to-end pipeline tests.

### Unit Tests

Each rule should have focused tests for:

- match/apply happy path
- unchanged path
- approximate-warning path
- unsupported path
- immutability contract

### Pair Tests

Each supported source/target pair should have curated coverage for:

- exact rewrites
- approximate rewrites
- unsupported features

Examples:

- PostgreSQL -> MySQL
- MySQL -> PostgreSQL
- ANSI -> PostgreSQL

### End-to-End Tests

End-to-end tests in integration style should verify:

1. parse source SQL
2. transpile
3. validate target
4. render target SQL
5. inspect diagnostics

### Golden Tests

Golden tests are a good fit for:

- stable input SQL
- stable target SQL
- stable expected step reports

## Suggested Initial Milestone

A practical first milestone should stay intentionally small.

### Phase T1

Deliver:

- `sqm-transpile` module
- `SqlTranspiler` public API
- `TranspileResult` and diagnostics model
- default rule registry
- PostgreSQL -> MySQL support for a small exact subset

Suggested first exact rules:

- null-safe equality mapping where semantics are explicitly supported
- quote/identifier normalization where needed
- selected built-in function renames with known exact semantics

Suggested first exact core-model promotion:

- introduce `ConcatExpr` and remove plain string concatenation from the initial PostgreSQL/MySQL transpile-rule backlog

Suggested first unsupported rules:

- PostgreSQL `RETURNING` to MySQL
- PostgreSQL `DISTINCT ON` unless explicit approximate rewrites are enabled
- PostgreSQL-only operators with no MySQL equivalent

### Phase T2

Add:

- approximate rewrite opt-in
- richer step reporting
- schema-aware rules
- MySQL -> PostgreSQL coverage

### Phase T3

Evaluate:

- portable semantic nodes in `sqm-core`
- rule cost/planning metadata
- DDL transpilation
- multi-statement transpilation support

## Recommended Rollout Plan

The recommended delivery approach is incremental.

Do not try to redesign the full core model before any transpilation work exists.
Do not commit to pairwise rules forever either.

Instead, use transpilation as the discovery mechanism for portable semantics.

### Step 1: Build `sqm-transpile`

Add the dedicated transpilation module and public API first.

This creates the right architectural home for:

- exact cross-dialect rewrites
- approximate rewrites
- unsupported cases
- diagnostic reporting

At this stage, avoid large `sqm-core` changes unless a semantic abstraction is already obviously reusable.

### Step 2: Implement the first dialect pair

Use PostgreSQL <-> MySQL as the first supported pair.

This pair is a strong starting point because:

- both dialects already exist in the repo
- both have meaningful syntax and behavior differences
- both include a mix of portable and non-portable features

Initial work should focus on a small curated rule set:

- exact rewrites that are easy to validate
- explicit unsupported cases
- no aggressive approximate rewrites unless they are opt-in

Where possible, rules in this first milestone should already be written for reuse.

That means:

- do not hard-code a rule to exactly one pair unless it is truly pair-specific
- prefer generalized rules with explicit source/target applicability
- let the registry build a per-pair execution plan from those reusable rules

### Step 3: Let repeated patterns emerge

As PostgreSQL -> MySQL and MySQL -> PostgreSQL rules grow, look for the same semantic conversion appearing repeatedly.

That repetition is the signal that a concept may belong in `sqm-core` instead of remaining a dialect-pair rule.

Examples of signals:

- the same rule shape appears in both directions
- the same concept appears again when adding a third dialect
- multiple renderers could naturally emit different syntax from one shared semantic node
- the current pairwise rules are mostly compensating for missing portable structure in the AST

### Step 4: Promote only proven portable semantics

When a concept is clearly reusable, introduce a dedicated node in `sqm-core`.

After that:

- parsers map dialect syntax into the portable node
- renderers emit dialect syntax from the portable node
- transpilation rules for that concept can be simplified or removed

This keeps the core model semantic rather than vendor-driven.

### Step 5: Keep non-portable behavior in transpilation

Not everything should become a core node.

Features with target-specific semantics, lossy emulation, or no safe equivalent should stay in `sqm-transpile` as:

- rewrite rules
- approximate rewrites
- unsupported outcomes

Examples include:

- `RETURNING` across incompatible DML dialects
- `DISTINCT ON`
- optimizer hints
- dialect-specific JSON/operator families
- planner or execution semantics that are not just syntax differences

## Current Slice Backlog And Limitations

For the current PostgreSQL -> MySQL slice, the following items remain intentionally out of scope or only partially covered and should stay visible as follow-up backlog:

- PostgreSQL case-insensitive regex variants such as `~*` and `!~*` are currently reported as unsupported rather than rewritten.
- PostgreSQL operator-family handling is still representative rather than exhaustive; additional array, range, JSON-path, and custom-operator cases may need explicit rejection rules.
- PostgreSQL `ILIKE` is currently lowered through `LOWER(...) LIKE LOWER(...)`, which is useful but still approximate because collation, locale, and index behavior can differ.
- PostgreSQL `DISTINCT ON` remains unsupported unless and until an explicit opt-in approximate rewrite strategy is added.
- PostgreSQL `RETURNING` remains unsupported for the current MySQL target slice.
- Date/time arithmetic, time zone conversion, lock-clause translation, and broader JSON rewrites are still backlog items rather than part of the initial slice.

For the current MySQL -> PostgreSQL slice, the following items remain intentionally out of scope or only partially covered and should stay visible as follow-up backlog:

- MySQL null-safe equality with `<=>` is covered for exact rewrites, but broader query-shape coverage should continue to grow through joins, subqueries, and more complex boolean compositions.
- MySQL optimizer comments and table index hints are currently dropped with warnings rather than preserved or rewritten semantically, including leading hints and `USE`/`FORCE`/`IGNORE INDEX` variants.
- MySQL JSON function handling is intentionally conservative and currently rejects the broader `JSON_*` function family rather than attempting partial PostgreSQL rewrites.
- MySQL `ON DUPLICATE KEY UPDATE`, `INSERT IGNORE`, and `REPLACE` remain unsupported rather than being lowered to PostgreSQL conflict-handling forms.
- Collation-sensitive string behavior, date/time arithmetic, lock/modifier translation, and JSON semantic rewrites are still backlog items rather than part of the initial slice.

## Initial Rule Matrix

The current initial transpilation slice should stay explicit about what is exact, what is approximate, and what is intentionally unsupported.

### PostgreSQL -> MySQL

| Category | Source construct | Current handling | Notes |
|----------|------------------|------------------|-------|
| Exact | `ConcatExpr` / string concatenation | Shared semantic node rendered as `CONCAT(...)` | Handled through `sqm-core`, not a pair-specific rule |
| Exact | `IS NOT DISTINCT FROM` | Rewritten to MySQL null-safe equality | Uses canonical distinctness semantics |
| Exact | `IS DISTINCT FROM` | Rewritten to `NOT (<=>)` form | Preserves exact distinctness semantics |
| Exact | Regex predicate subset | Rendered through existing regex semantic support | Limited to the subset that already maps cleanly |
| Approximate | `ILIKE` | Lowered to `LOWER(lhs) LIKE LOWER(rhs)` with warning | Includes `NOT ILIKE` and `ESCAPE`; collation and indexing may differ |
| Unsupported | `RETURNING` | Rejected with structured transpilation problem | No safe current MySQL equivalent in this slice |
| Unsupported | `DISTINCT ON` | Rejected with structured transpilation problem | Query-shape semantics, not a token substitution |
| Unsupported | `SIMILAR TO` | Rejected with structured transpilation problem | No safe equivalent in current scope |
| Unsupported | Representative PostgreSQL operator families | Rejected with structured transpilation problem | Includes representative JSON and specialized operator cases |
| Unsupported | PostgreSQL case-insensitive regex variants | Rejected with structured transpilation problem | `~*` and `!~*` remain backlog |

### MySQL -> PostgreSQL

| Category | Source construct | Current handling | Notes |
|----------|------------------|------------------|-------|
| Exact | `ConcatExpr` / string concatenation | Shared semantic node rendered with `||` | Handled through `sqm-core`, not a pair-specific rule |
| Exact | `<=>` | Rewritten to PostgreSQL distinctness predicates | Uses canonical SQM semantic form before render |
| Exact | Regex predicate subset | Rendered through existing regex semantic support | Limited to the subset that already maps cleanly |
| Approximate | Optimizer comments and index hints | Dropped with warning | Rewritten out of the AST so resulting SQL stays executable |
| Unsupported | `ON DUPLICATE KEY UPDATE` | Rejected with structured transpilation problem | No exact PostgreSQL lowering in the initial slice |
| Unsupported | `INSERT IGNORE` / `REPLACE` | Rejected with structured transpilation problem | Conflict-handling semantics remain backlog |
| Unsupported | MySQL JSON function family | Rejected with structured transpilation problem | Intentionally conservative to avoid misleading rewrites |

## Contribution Guidelines

Transpilation changes should stay predictable for contributors and users. The following rules are the recommended contribution contract for new semantic nodes and transpilation rules.

### When To Add A Core Semantic Node

Add or promote a `sqm-core` node when most of the following are true:

- the concept exists in at least two dialects
- the concept can be described semantically rather than by token spelling
- at least two parsers can map into the node naturally
- at least two renderers can emit dialect syntax from the same node
- keeping the concept out of `sqm-core` would create repeated pairwise rules

`ConcatExpr` is the first-wave example of this pattern:

- PostgreSQL `||` parses to `ConcatExpr`
- MySQL `CONCAT(...)` parses to `ConcatExpr`
- renderers emit dialect syntax from the shared semantic node

### When To Add A Transpilation Rule

Add a transpilation rule when any of the following are true:

- the feature is pair-specific
- the feature is only approximately portable
- the feature has no safe target equivalent
- the feature is target-capability driven rather than just syntax driven
- the feature would make `sqm-core` vendor-shaped instead of semantic

Examples:

- `ILIKE` lowering remains a rule because it is approximate in MySQL
- optimizer/index hint dropping remains a rule because it is target-specific behavior
- `RETURNING` rejection remains a rule because it is explicitly unsupported in the current slice

### Rule Authoring Checklist

When adding a new rule:

- prefer reusable applicability across dialect sets over a one-off pair lock unless the rule is truly pair-specific
- keep `id()` stable and descriptive
- choose the right fidelity: `EXACT`, `APPROXIMATE`, or `UNSUPPORTED`
- emit warnings for non-blocking information loss
- emit blocking problems for unsupported behavior
- preserve immutability: unchanged input should return the same instance; changed input should return a new statement
- add focused unit tests for changed, unchanged, and unsupported or warning paths
- add or update an end-to-end `DefaultSqlTranspilerTest` case when the rule affects the public pipeline

### Target Validation Responsibilities

Target validation remains a required phase of transpilation rather than an optional renderer concern.

Contributors should assume this order:

1. parse with source dialect
2. normalize or transpile through ordered rules
3. validate against target dialect and optional target schema
4. render only if the target AST is valid

That means:

- rules should not assume renderers will silently fix unsupported target constructs
- renderers should stay strict and reject unsupported target features
- validation failures should be surfaced as `TranspileResult` diagnostics rather than late rendering surprises

## Publishing GitHub Issues

The transpilation epic and stories can be published from the markdown source in `docs/epics/R4_SQL_TRANSPILATION_FOUNDATION.md`.

Recommended commands:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\create-github-issues-from-epic-md.ps1 -Path docs\epics\R4_SQL_TRANSPILATION_FOUNDATION.md -WhatIf
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\create-github-issues-from-epic-md.ps1 -Path docs\epics\R4_SQL_TRANSPILATION_FOUNDATION.md
```

The generic GitHub issue publisher in `scripts/create-github-issues-from-epic-md.ps1` should be used directly. Epic markdown is treated as publish-once planning input rather than something that is repeatedly republished after future scope changes.

### Practical Rule of Thumb

Use this decision rule when a new conversion appears:

- if it is common, semantic, and reusable across multiple dialects, consider a core node
- if it is pair-specific, lossy, or hard to define portably, keep it as a transpilation rule

In short:

1. start with `sqm-transpile`
2. prove the first pair with PostgreSQL <-> MySQL
3. observe repeated semantic rewrites
4. extract only the proven portable concepts into `sqm-core`

## PostgreSQL vs MySQL Consolidation Candidates

The following table captures current high-signal candidates for consolidation based on the dialect support that already exists in this repository.

The intent is not to prove full semantic equivalence for every item. The intent is to identify where a dedicated `sqm-core` node is likely to reduce pairwise transpilation rules without overfitting the model to one dialect.

| Feature                               | PostgreSQL form                                                   | MySQL form                                                                    | Candidate core node?                    | Recommendation                                                | Notes                                                                                                                                                                       |
|---------------------------------------|-------------------------------------------------------------------|-------------------------------------------------------------------------------|-----------------------------------------|---------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| String concatenation                  | `a \|\| b`                                                        | `CONCAT(a, b)`                                                                | Yes                                     | Introduce `ConcatExpr` now                                    | This is the strongest current candidate. It should be modeled directly in `sqm-core` instead of relying on transpile rules for the normal PostgreSQL/MySQL path.            |
| Regex match                           | `a ~ b`, `a ~* b`, `a !~ b`, `a !~* b`                            | `a REGEXP b`, `a NOT REGEXP b`                                                | Already consolidated                    | Keep current approach                                         | SQM already models this through `RegexPredicate`, which is a good example of a successful semantic abstraction over different surface syntax.                               |
| Null-safe comparison                  | `a IS NOT DISTINCT FROM b` and related distinctness predicates    | `a <=> b`                                                                     | Partially                               | Reuse existing semantic abstractions before adding more nodes | SQM already has `IsDistinctFromPredicate` and null-safe comparison support. The next question is canonicalization policy, not necessarily a new node.                       |
| Case-insensitive pattern match        | `a ILIKE b`                                                       | no direct syntax equivalent                                                   | Maybe                                   | Keep in transpilation for now                                 | The concept is semantic, but target behavior often requires approximate rewrites such as `LOWER(a) LIKE LOWER(b)`, which may differ in collation and indexing behavior.     |
| SQL regex pattern match               | `a SIMILAR TO b`                                                  | no direct equivalent                                                          | No for now                              | Keep in transpilation or unsupported handling                 | Superficially portable, but the target-side equivalents are weak and semantics are not cleanly portable enough for a core node yet.                                         |
| Time zone conversion                  | `ts AT TIME ZONE zone`                                            | often `CONVERT_TZ(ts, from_tz, to_tz)`-style function usage                   | Already modeled, but not fully portable | Keep current node and use transpilation where needed          | `AtTimeZoneExpr` is already a semantic node, but cross-dialect translation still needs rewrite logic because the required inputs and semantics do not line up cleanly.      |
| Date arithmetic                       | interval arithmetic and timestamp plus/minus interval expressions | `DATE_ADD(...)`, `DATE_SUB(...)`, interval-aware functions                    | Not yet                                 | Keep mostly in transpilation for now                          | There is shared structure via interval literals, but the operation families and coercion behavior are still too dialect-shaped to collapse into one obvious new node today. |
| JSON field extraction                 | `payload -> 'a'`, `payload ->> 'a'`                               | `JSON_EXTRACT(payload, '$.a')`, `JSON_UNQUOTE(JSON_EXTRACT(...))`-style usage | No for now                              | Keep dialect-specific or transpilation-based                  | The shapes look similar, but return typing and path semantics differ enough that a premature shared node would likely be misleading.                                        |
| JSON containment                      | `payload @> '{"a":1}'`                                            | JSON containment-style functions with different behavior                      | No                                      | Keep dialect-specific                                         | This is not just syntax variation; the semantics and supported operand forms are too different.                                                                             |
| Array overlap / specialized operators | `tags && other_tags` and similar operator families                | no true equivalent in the current MySQL surface                               | No                                      | Keep dialect-specific or unsupported                          | This is a strong example of behavior that should not be forced into a shared node.                                                                                          |
| Distinctness by row prefix            | `DISTINCT ON (...)`                                               | no direct equivalent                                                          | No                                      | Keep as transpilation or unsupported                          | This is query-shape semantics, not a simple operator/function substitution.                                                                                                 |
| DML returning rows                    | `RETURNING`                                                       | no equivalent for current supported MySQL versions                            | No                                      | Keep as unsupported or non-SQL rewrite                        | This should remain outside renderer-only translation and outside portable node modeling.                                                                                    |

### Immediate Candidate Summary

If the project wants to add one new semantic node immediately based on current evidence, it should be `ConcatExpr`.

If the project wants to use existing abstractions as guidance, these are already in the right direction:

- `RegexPredicate`
- `IsDistinctFromPredicate`
- `AtTimeZoneExpr`

These show that the best semantic nodes are:

- conceptually meaningful
- reusable across multiple dialects
- not tied to one concrete token spelling

## Recommended Package Layout

```text
sqm-transpile/
  src/main/java/io/sqm/transpile/
    SqlTranspiler.java
    DefaultSqlTranspiler.java
    SqlDialectId.java
    TranspileOptions.java
    TranspileResult.java
    TranspileStatus.java
    TranspileStage.java
    TranspileContext.java
    TranspileProblem.java
    TranspileWarning.java
    TranspileStep.java
    RewriteFidelity.java
  src/main/java/io/sqm/transpile/rule/
    TranspileRule.java
    TranspileRuleResult.java
    TranspileRuleRegistry.java
    DefaultTranspileRuleRegistry.java
  src/main/java/io/sqm/transpile/builtin/
    PostgresToMySqlIlikeRule.java
    PostgresToMySqlReturningUnsupportedRule.java
```

## Compatibility with Current Contracts

This design intentionally preserves current module responsibilities.

### Parser contract

No required breaking change.

### Core AST contract

No required breaking change for the first milestone.

### Renderer contract

No required breaking change.
Renderers remain renderers, not transpilers.

### Validator contract

No required breaking change.
The transpiler simply invokes target validation at the correct point in the pipeline.

## Open Questions

These are the main design decisions still worth settling before implementation.

### 1. Should some dialect-pair rewrites become core semantic nodes?

Recommendation:

- only after at least two or three rules prove the concept is broadly reusable

### 2. Should transpilation reuse `StatementRewriteRule` directly?

Recommendation:

- no at the public API level
- maybe through an internal adapter if it removes boilerplate

### 3. Should approximate rewrites be enabled by default?

Recommendation:

- no
- require explicit opt-in through `TranspileOptions`

### 4. Should transpilation depend on schema catalogs?

Recommendation:

- schema support should be optional
- the first milestone should work without catalogs
- schema-aware rules can be additive

## Summary

SQM already has most of the machinery required for cross-dialect conversion. The missing piece is a dedicated transpilation phase with explicit rule execution and fidelity reporting.

The clean architectural move is:

1. add `sqm-transpile`
2. introduce `SqlTranspiler` as a first-class API
3. implement ordered dialect-pair rewrite rules
4. validate against the target dialect after rewrite
5. keep renderers strict and focused on syntax emission

This keeps the current architecture coherent while making room for real source-to-target SQL conversion with safe failure modes and testable behavior.
