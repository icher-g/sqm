## Epic

### Title
`Epic: R10 Oracle Dialect Support`

### Problem Statement

SQM currently ships ANSI, PostgreSQL, MySQL, and SQL Server dialect slices. Oracle is the next high-value enterprise dialect because it differs materially from the existing set in areas that matter to a SQL manipulation framework:

- identifier normalization and quoted identifier behavior
- row limiting through `OFFSET ... FETCH` and legacy `ROWNUM` idioms
- DML `RETURNING ... INTO`
- Oracle `MERGE` syntax and action restrictions
- `LATERAL`, `CROSS APPLY`, and `OUTER APPLY`
- hierarchical queries through `CONNECT BY`
- Oracle function and type names
- optimizer hints through `/*+ ... */`
- table-producing JSON features such as `JSON_TABLE`

This design defines how to add Oracle support across all SQM layers without letting dialect syntax leak into `sqm-core`, and without accidentally treating DDL as part of the scope.

### Epic Goal

- Add a coherent Oracle dialect slice across parser, renderer, validation, transpilation, control, codegen, catalog, middleware, playground, docs, and tests.
- Keep Oracle support explicit at parse time, render time, validation time, and transpilation time.
- Reuse shared semantic nodes where Oracle syntax expresses already-modeled semantics.
- Add new `sqm-core` nodes only when Oracle introduces a manipulation-relevant semantic concept that should be portable or inspectable.
- Make version-gated Oracle behavior explicit, with Oracle 19c as the baseline support target and later features gated separately.

### Business Value

- Adds a major enterprise dialect to SQM's supported SQL surface.
- Makes SQM more useful for migration, interoperability, and governance workflows involving Oracle estates.
- Exercises the dialect architecture against Oracle-specific features that differ from PostgreSQL, MySQL, and SQL Server.
- Keeps parser, renderer, validation, transpilation, DSL, codegen, catalog, and middleware support aligned from the start.

### Definition of Done

- Oracle modules are in the reactor and released artifacts.
- Parser, renderer, validation, transpilation, control, codegen, catalog, middleware, playground, integration tests, and docs all recognize Oracle explicitly.
- Query and DML scope are implemented or rejected with clear diagnostics.
- DDL remains out of scope and documented as such.
- Every new public class or method has JavaDoc.
- Every new behavior has tests.
- No Oracle syntax is modeled in `sqm-core` unless it passes the shared-semantics review in `SQM_MODELING_RULES.md`.

### Suggested Labels
`epic`, `oracle`, `dialect`, `parser`, `renderer`, `validation`, `transpile`

---

## Non-Goals

- Full PL/SQL support.
- Stored procedures, packages, anonymous blocks, triggers, cursors, procedural variables, or host-language execution semantics.
- DDL.
- Full Oracle SQL Language Reference parity.
- Silent renderer rewrites of unsupported Oracle features.
- Modeling `JSON_TABLE`, hierarchical queries, or `MODEL` clause as raw strings if SQM needs to inspect, validate, transform, or transpile them.

## Scope Boundaries

### Query

In scope for baseline Oracle query support:

- ordinary `SELECT`, joins, subqueries, CTEs, set operations, grouping, ordering, windows, and predicates already represented in SQM
- quoted identifiers with Oracle normalization rules
- `OFFSET ... FETCH` row limiting using the existing `LimitOffset` family where possible
- `NULLS FIRST` / `NULLS LAST`
- Oracle function catalog coverage for common scalar, aggregate, string, numeric, date/time, and null-handling functions
- optimizer hints in Oracle hint-comment form where they fit the existing typed statement-hint model
- `LATERAL` inline views and `CROSS APPLY` / `OUTER APPLY` where the existing `Lateral` relation model fits
- `CONNECT BY` only as a dedicated follow-up story, because it is a real semantic query shape
- `JSON_TABLE` only as a dedicated follow-up story, because it is richer than the current generic `FunctionTable` model

### DML

In scope for baseline Oracle DML support:

- `INSERT`, `UPDATE`, and `DELETE` in the shared DML shape
- Oracle `RETURNING ... INTO` as an Oracle-specific rendering/parsing of mutation result semantics, with host/PLSQL target handling explicit
- Oracle `MERGE` where it maps cleanly to the existing `MergeStatement` and `MergeClause` family
- Oracle DML hints where they fit typed statement hints

### DDL

Out of scope. Oracle DDL must not be planned or implemented as part of this dialect epic unless the repo instructions are updated with a separate DDL framework decision.

## External Capability Notes

This design was checked against Oracle documentation current enough for planning on 2026-05-06:

- Oracle `SELECT` documents `LATERAL`, `CROSS APPLY`, `OUTER APPLY`, grouping extensions, hierarchical queries, `ORDER BY`, and row limiting syntax in the SELECT grammar. Oracle also documents restrictions such as `table_reference` not being a lateral inline view under `CROSS APPLY` / `OUTER APPLY` in the cited 12.2 reference. [Oracle SELECT](https://docs.oracle.com/en/database/oracle/oracle-database/12.2/sqlrf/SELECT.html)
- Oracle `MERGE` requires at least one update or insert clause and has Oracle-specific action syntax and restrictions. [Oracle MERGE](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/MERGE.html)
- Oracle DML `RETURNING INTO` exists for `DELETE`, `INSERT`, and `UPDATE` in 19c SQL/PLSQL documentation; Oracle 26 documentation also includes `MERGE` in this family, so `MERGE RETURNING` must be version-gated rather than silently assumed for 19c. [Oracle RETURNING INTO](https://docs.oracle.com/en/database/oracle/oracle-database/19/lnpls/RETURNING-INTO-clause.html), [Oracle 26 INSERT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html)
- Oracle `JSON_TABLE` creates a virtual table through a mandatory `COLUMNS` clause and has nested-path behavior. It should not be forced into the current generic `FunctionTable` node without a model review. [Oracle JSON_TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/19/adjsn/function-JSON_TABLE.html)

## Module Additions

Add these Maven modules and register them in the parent `pom.xml`:

- `sqm-core-oracle`
- `sqm-parser-oracle`
- `sqm-render-oracle`
- `sqm-validate-oracle`
- `sqm-catalog-oracle`

Review existing modules for Oracle mappings:

- `sqm-core`
- `sqm-parser`
- `sqm-render`
- `sqm-validate`
- `sqm-transpile`
- `sqm-control`
- `sqm-codegen`
- `sqm-codegen-maven-plugin`
- `sqm-middleware-api`
- `sqm-middleware-core`
- `sqm-middleware-rest`
- `sqm-middleware-mcp`
- `sqm-playground-api`
- `sqm-playground-rest`
- `sqm-playground-web`
- `sqm-it`
- `sqm-db-it`
- `examples`

## Dialect Identity

Update `io.sqm.core.dialect.SqlDialectId`:

- add `ORACLE = new SqlDialectId("oracle")`
- normalize aliases: `oracle`, `ora`
- avoid aliases that imply PL/SQL, because this epic is SQL-only

Update Javadocs in all dialect-aware APIs that currently list supported aliases:

- `SqlStatementParser`
- `SqlStatementRenderer`
- `SqlStatementValidator`
- `DefaultSqlTranspiler`
- `SqlCodegenDialect`
- Maven plugin parameter docs

## Oracle Capabilities

Create `io.sqm.core.oracle.dialect.OracleCapabilities`.

Baseline:

```java
/**
 * Shared Oracle feature matrix used by parser, renderer, and validation modules.
 */
public final class OracleCapabilities {
    private static final SqlDialectVersion LATEST_SUPPORTED = SqlDialectVersion.of(19, 0);

    public static DialectCapabilities of(SqlDialectVersion version) {
        return VersionedDialectCapabilities.builder(version)
            .supports(SqlFeature.DATE_TYPED_LITERAL)
            .supports(SqlFeature.TIMESTAMP_TYPED_LITERAL)
            .supports(SqlFeature.INTERVAL_LITERAL)
            .supports(SqlFeature.DML_RESULT_CLAUSE)
            .supports(SqlFeature.MERGE_STATEMENT)
            .supports(SqlFeature.LATERAL, SqlDialectVersion.of(12, 1))
            .supports(SqlFeature.GROUPING_SETS)
            .supports(SqlFeature.ROLLUP)
            .supports(SqlFeature.CUBE)
            .supports(SqlFeature.LOCKING_CLAUSE)
            .supports(SqlFeature.LOCKING_NOWAIT)
            .supports(SqlFeature.LOCKING_SKIP_LOCKED)
            .supports(SqlFeature.OPTIMIZER_HINT_COMMENT)
            .build();
    }
}
```

Capability additions to review before implementation:

- Add `ROW_LIMIT_FETCH` only if the existing `LimitOffset` feature checks need a separate capability.
- Add `HIERARCHICAL_QUERY` for `CONNECT BY`.
- Add `JSON_TABLE` only after the model shape is decided.
- Add `ORACLE_RETURNING_INTO` only if shared `DML_RESULT_CLAUSE` is too broad for validation and transpilation diagnostics.
- Add `MERGE_RESULT_CLAUSE` for Oracle only when the implementation targets a version that supports `MERGE RETURNING`.

## Parser Design

Create `sqm-parser-oracle` with package `io.sqm.parser.oracle`.

Primary classes:

- `io.sqm.parser.oracle.spi.OracleSpecs`
- `io.sqm.parser.oracle.Parsers`
- `OracleIdentifierQuoting` only if `IdentifierQuoting` cannot express Oracle needs
- dialect-specific parsers where Oracle syntax differs from ANSI-derived behavior

`OracleSpecs` should mirror the shipped pattern:

- baseline constructor uses `SqlDialectVersion.of(19, 0)`
- `parsers()` returns `Parsers.oracle()`
- `lookups()` can start from `AnsiLookups`
- `identifierQuoting()` accepts double quotes
- `capabilities()` returns `OracleCapabilities.of(version)`
- `operatorPolicy()` can start from `AnsiOperatorPolicy`, then specialize only for Oracle-specific operators

Parser work items:

- Add Oracle keywords to lexer/token definitions before matching them as `IDENT`: `CONNECT`, `START`, `PRIOR`, `SIBLINGS`, `RETURNING`, `INTO`, `MERGE`, `USING`, `MATCHED`, `LATERAL`, `APPLY`, `OFFSET`, `FETCH`, `ROWS`, `ONLY`, `TIES`, `JSON_TABLE`, `MODEL`.
- Preserve parser discipline: use `cur.expect(...)` after committing to required Oracle syntax branches.
- Register Oracle parsers in `Parsers.oracle()` instead of inheriting behavior implicitly.
- Add dedicated parser classes for every node or Oracle-specific syntax form that differs materially.

Baseline parser coverage:

- `SelectQueryParser` for Oracle row limiting, hints, and relation syntax.
- `LimitOffsetParser` for `OFFSET n ROWS FETCH NEXT m ROWS ONLY` and `FETCH FIRST m ROWS ONLY`.
- `JoinParser` for `LATERAL`, `CROSS APPLY`, and `OUTER APPLY`, mapped to shared `Lateral` where legal.
- `FunctionExprParser` for Oracle function synonyms and special forms only where they fit existing function nodes.
- `InsertStatementParser`, `UpdateStatementParser`, and `DeleteStatementParser` for `RETURNING expr[, ...] INTO :bind[, ...]`.
- `MergeStatementParser` for Oracle `MERGE INTO ... USING ... ON ... WHEN MATCHED THEN UPDATE ... DELETE WHERE ... WHEN NOT MATCHED THEN INSERT ...`.

Deferred parser stories:

- `HierarchicalQueryParser` for `START WITH` / `CONNECT BY` / `PRIOR`.
- `JsonTableParser` after the model decision.
- `ModelClauseParser` only after a separate design. The Oracle `MODEL` clause is not baseline.
- Legacy outer join operator `(+)`; this should be rejected initially with a clear parse diagnostic rather than partially modeled.
- Legacy pagination rewrites with `ROWNUM`; parse only if a model/transpilation story explicitly accepts it.

## Renderer Design

Create `sqm-render-oracle` with package `io.sqm.render.oracle`.

Primary classes:

- `io.sqm.render.oracle.spi.OracleDialect`
- `io.sqm.render.oracle.spi.OracleIdentifierQuoter`
- `io.sqm.render.oracle.spi.OraclePaginationStyle`
- `io.sqm.render.oracle.spi.OracleBooleans`
- `io.sqm.render.oracle.Renderers`

`OracleDialect` responsibilities:

- `name()` returns `Oracle`
- `quoter()` emits double-quoted identifiers
- unquoted identifiers render in Oracle's normal uppercase convention only if the core identifier model and existing renderers allow that without changing semantic identity
- `booleans()` renders boolean expressions safely for SQL contexts where Oracle SQL lacks a plain boolean scalar in 19c
- `paginationStyle()` emits `OFFSET ... FETCH` for baseline Oracle 12c+ row limiting
- `capabilities()` returns `OracleCapabilities.of(version)`
- `renderers()` returns `Renderers.oracle()`

Renderer work items:

- Render `LimitOffset` as Oracle row limiting.
- Render `Lateral` using `LATERAL` for inline views or `CROSS APPLY` / `OUTER APPLY` where the SQM join shape demands apply semantics.
- Render DML result clauses as `RETURNING ... INTO ...` only when the model includes the target sink required by Oracle.
- Render `MERGE` in Oracle syntax; reject SQL Server-only branches such as `WHEN NOT MATCHED BY SOURCE`.
- Render optimizer hints with Oracle `/*+ ... */` syntax if represented by typed statement hints.
- Reject unsupported Oracle features explicitly through `UnsupportedDialectFeatureException`.

Important renderer boundary:

Oracle `RETURNING ... INTO` does not mean the same thing as PostgreSQL `RETURNING` in a normal SQL result set. Rendering a generic `ResultClause` without a result target must fail for Oracle unless a model story adds a safe representation for host/PLSQL target variables.

## Core Model Impact

Start with no speculative core-node additions. Reuse existing nodes where semantics fit:

- `LimitOffset` for row limiting.
- `Lateral` for correlated relation wrappers.
- `MergeStatement` and `MergeClause` for baseline Oracle `MERGE`.
- `ResultClause` for DML result expressions only if paired with a target concept that can represent `INTO`.
- `StatementHint` / `TableHint` families for typed hints if current hint modeling supports Oracle.

Model reviews required before implementation:

### Oracle Returning Target

Oracle `RETURNING ... INTO :bind` stores returned values into host or PL/SQL variables instead of returning rows to the client as a query result.

Options:

- Add a semantic `RelationResultTarget` variant that can represent variable/host targets outside SQL Server table targets.
- Add an Oracle-specific result target node such as `HostVariableResultTarget`.
- Keep Oracle DML `RETURNING` unsupported in baseline render/parse until the target model exists.

Recommendation:

- Add a shared result-target abstraction only if it can represent both Oracle host targets and SQL Server table targets without making either one ambiguous.
- Do not render Oracle `RETURNING` from a plain PostgreSQL-style `ResultClause`.

### Hierarchical Query

`CONNECT BY` is not a syntax-only variant of recursive CTEs. It has Oracle-specific traversal semantics, pseudocolumns, sibling ordering, and `PRIOR` behavior.

Recommendation:

- Add a dedicated semantic query node or clause family only when the project commits to manipulating hierarchical queries.
- Until then, parser and renderer should reject `CONNECT BY`.

### JSON_TABLE

Oracle `JSON_TABLE` is table-producing but has mandatory column projection, nested path clauses, error/empty behavior, and return typing.

Recommendation:

- Do not force it through generic `FunctionTable`.
- Add a dedicated `JsonTable` relation family only after comparing Oracle and MySQL `JSON_TABLE` semantics.

## Validation Design

Create `sqm-validate-oracle` with package `io.sqm.validate.oracle`.

Primary classes:

- `io.sqm.validate.oracle.OracleValidationDialect`
- `io.sqm.validate.oracle.function.OracleFunctionCatalog`
- `io.sqm.validate.oracle.rule.OracleExpressionFeatureValidationRule`
- `OracleSelectValidationRule`
- `OracleInsertStatementValidationRule`
- `OracleUpdateStatementValidationRule`
- `OracleDeleteStatementValidationRule`
- `OracleMergeStatementValidationRule`
- `OracleHintValidationRule`

Validation responsibilities:

- Version-gate `LATERAL`, row limiting, `MERGE`, and any future `MERGE RETURNING` support.
- Reject PostgreSQL-only features: `DISTINCT ON`, `ILIKE`, `SIMILAR TO`, PostgreSQL arrays, custom operators, `::` casts, dollar strings, escape strings, function-table ordinality, `ON CONFLICT`.
- Reject MySQL-only features: `INSERT IGNORE`, `ON DUPLICATE KEY UPDATE`, `REPLACE`, `UPDATE ... JOIN`, `DELETE ... USING ... JOIN`, index hints, `SQL_CALC_FOUND_ROWS`, null-safe equality.
- Reject SQL Server-only features: `TOP`, `OUTPUT`, `OUTPUT INTO`, pseudo-row sources `inserted` / `deleted`, table variables, table lock hints, `WHEN NOT MATCHED BY SOURCE`.
- Validate Oracle `RETURNING ... INTO` expression-to-target count and statement-type restrictions if the model supports it.
- Validate Oracle row limiting combinations and deterministic-order warnings if the validation framework gains warning severity.
- Validate `CROSS APPLY` / `OUTER APPLY` restrictions around lateral inline views.
- Validate `MERGE` shape: at least one update or insert branch, valid delete-where placement, no unsupported action families.

The validation dialect must be wired into:

- `SchemaStatementValidator.of(schema, OracleValidationDialect.of())`
- `SqlStatementValidator.defaultDialectAware(...)`
- `DefaultSqlTranspiler.defaultValidationFactory(...)`
- codegen validation paths

## Transpilation Design

Update `sqm-transpile`:

- add `SqlDialectId.ORACLE` support in default parser, renderer, and validation factories
- add Oracle-aware built-in rules to `DefaultTranspileRuleRegistry.defaults()`
- add registry tests proving Oracle rules are selected only for Oracle source/target pairs

Initial exact rules:

- ANSI/PostgreSQL/MySQL `LimitOffset` to Oracle `OFFSET ... FETCH` render path when no dialect-specific rewrite is needed.
- Oracle `OFFSET ... FETCH` to PostgreSQL/MySQL `LIMIT/OFFSET` through shared `LimitOffset`.
- Oracle `LATERAL` / apply-compatible `Lateral` to PostgreSQL `LATERAL` and SQL Server `APPLY` when join shape is compatible.
- Oracle `MERGE` to SQL Server/PostgreSQL only for the subset that fits the shared `MergeStatement` family exactly.

Initial unsupported rules:

- Oracle `RETURNING ... INTO` to PostgreSQL `RETURNING` unless the target API explicitly wants returned rows instead of host variables.
- PostgreSQL `RETURNING` to Oracle `RETURNING ... INTO` without target variables.
- SQL Server `OUTPUT` / `OUTPUT INTO` to Oracle unless an explicit result-target mapping exists.
- Oracle hierarchical queries to every other dialect.
- Oracle `JSON_TABLE` to current `FunctionTable`.
- Oracle hint comments to targets without equivalent typed hint semantics, unless the rule drops them with warning and `allowApproximateRewrites` is enabled.

Initial approximate rules, disabled by default:

- `NVL(a, b)` and `COALESCE(a, b)` normalization only when type behavior is known to be compatible.
- Oracle empty-string-as-null behavior must not be approximated silently.
- Date arithmetic rewrites must be conservative and schema/type-aware.

Rule ids should use stable names:

- `oracle-returning-into-unsupported`
- `oracle-hierarchical-query-unsupported`
- `oracle-json-table-unsupported`
- `oracle-hint-dropping`
- `standard-limit-to-oracle-fetch`
- `oracle-fetch-to-standard-limit`
- `oracle-merge-unsupported`

## Control And Middleware Integration

Update `sqm-control`:

- add Oracle mappings in `SqlStatementParser.defaultDialectAware()`
- add Oracle mappings in `SqlStatementRenderer.defaultDialectAware()`
- add Oracle mappings in `SqlStatementValidator.defaultDialectAware()`
- update Javadocs and tests for Oracle aliases

Update middleware modules:

- accept `oracle` in request DTO validation and OpenAPI schemas
- expose Oracle in dialect capability endpoints, if present
- update REST and MCP tests that enumerate dialects
- ensure audit events and execution contexts preserve the normalized `oracle` id

Update playground:

- add Oracle to dialect selectors
- add Oracle examples for baseline query and DML syntax
- keep unsupported examples visible as explicit diagnostics rather than hidden UI options

## Codegen And Maven Plugin Integration

Update `sqm-codegen`:

- add `ORACLE` to `SqlCodegenDialect`
- accept aliases `oracle`, `ora`
- add Oracle `ParseStage` with `OracleSpecs`
- add Oracle validation with `OracleValidationDialect`
- add tests for Oracle SQL files
- ensure emitted Java DSL uses helper methods, not direct node factory calls, for Oracle-relevant constructs

Update `sqm-codegen-maven-plugin`:

- depend on `sqm-parser-oracle`, `sqm-validate-oracle`, and `sqm-catalog-oracle`
- wire `OracleSqlTypeMapper`
- update plugin parameter docs and cache metadata tests

Codegen DSL impact:

- Add DSL helpers for any new public Oracle-relevant model surface before codegen emits it.
- If Oracle `RETURNING INTO`, `CONNECT BY`, or `JSON_TABLE` get new nodes, add emitter coverage and developer-friendly helper methods in the same story.

## Catalog Integration

Create `sqm-catalog-oracle`:

- `io.sqm.catalog.oracle.OracleSqlTypeMapper`

Baseline type mappings:

- character: `CHAR`, `NCHAR`, `VARCHAR2`, `NVARCHAR2`, `CLOB`, `NCLOB`
- numeric: `NUMBER`, `FLOAT`, `BINARY_FLOAT`, `BINARY_DOUBLE`
- date/time: `DATE`, `TIMESTAMP`, `TIMESTAMP WITH TIME ZONE`, `TIMESTAMP WITH LOCAL TIME ZONE`, `INTERVAL YEAR TO MONTH`, `INTERVAL DAY TO SECOND`
- binary: `RAW`, `BLOB`
- row identity: `ROWID`, `UROWID`
- JSON: map Oracle `JSON` type when targeting versions that support it; otherwise treat JSON-bearing text/blob columns conservatively

Catalog tests:

- verify representative Oracle type strings map to SQM catalog types
- verify precision/scale handling for `NUMBER(p,s)`
- verify timestamp time-zone variants
- verify unknown types remain explicit rather than misclassified

## JSON And Serialization

Update `sqm-json` only if new model nodes are introduced.

For each new node:

- add JSON mixin coverage
- add round-trip tests
- update `docs/model/MODEL.md`
- add visitor and transformer coverage
- add DSL and codegen support

If the first Oracle slice adds no nodes, update only support matrix documentation.

## Test Strategy

Unit test requirements:

- parser happy paths for each supported Oracle syntax family
- parser invalid syntax and unsupported syntax cases
- renderer happy paths and unsupported feature exceptions
- validation diagnostics for Oracle feature boundaries
- transpilation exact, unsupported, and approximate-disabled paths
- codegen dialect parsing and DSL emission paths
- catalog type mapping
- control default dialect-aware parser/renderer/validator mapping

Integration test requirements:

- middleware parse/render/validate flow with `dialect=oracle`
- playground API dialect listing
- codegen Maven plugin sample with Oracle SQL files
- optional `sqm-db-it` Oracle container profile only if project infrastructure can support Oracle XE or Free images deterministically

Test examples should use DSL/helper methods. If the Oracle scenario feels awkward to express, add a helper before writing final tests.

## Documentation Updates

Update:

- `docs/model/MODEL.md` support matrix with an Oracle column
- `docs/validation/VALIDATION_FEATURES.md`
- `docs/transpilation/SQL_TRANSPILATION_DESIGN.md`
- `docs/downstream/DOWNSTREAM_SUPPORT_MATRIX.md`
- `docs/codegen/SQL_FILE_CODEGEN_SCHEMA_VALIDATION.md`
- README dialect list
- wiki source pages generated from model/docs, if applicable

Documentation must state Query, DML, and DDL scope separately.

## Support Matrix Draft

| Construct                       | Oracle baseline status                                                     | Notes                                                                    |
|---------------------------------|----------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `DistinctSpec`                  | `Support` for ANSI `DISTINCT`                                              | `DISTINCT ON` remains PostgreSQL-only.                                   |
| `LimitOffset`                   | `Support`                                                                  | Render as `OFFSET ... FETCH`; version-gate if supporting pre-12c.        |
| `TopSpec`                       | `Not supported by the dialect`                                             | Do not map Oracle row limiting to SQL Server `TOP`.                      |
| `Lateral`                       | `Support`                                                                  | Map Oracle `LATERAL` / apply-compatible forms where shape is valid.      |
| `FunctionTable`                 | `Support` for generic table functions only after function inventory review | Do not treat `JSON_TABLE` as generic `FunctionTable`.                    |
| `JsonTable` future node         | `Not implemented by SQM`                                                   | Requires dedicated model design.                                         |
| `AtTimeZoneExpr`                | `Not supported by SQM` initially                                           | Oracle time-zone conversion needs semantic review against existing node. |
| `ResultClause`                  | `Not implemented by SQM` initially for Oracle                              | Needs `RETURNING ... INTO` target model.                                 |
| `RelationResultTarget`          | `Not implemented by SQM` for Oracle host targets                           | Existing SQL Server table-target semantics are not enough.               |
| `MergeStatement`                | `Support` for baseline Oracle subset                                       | Reject SQL Server/PostgreSQL-only branches.                              |
| `VariableTable`                 | `Not supported by the dialect`                                             | PL/SQL variables are not relation references in SQM baseline.            |
| `HierarchicalQuery` future node | `Not implemented by SQM`                                                   | Dedicated story for `CONNECT BY`.                                        |
| DDL nodes                       | Out of scope                                                               | Requires separate framework decision.                                    |

## Delivery Plan

### Story R10-1

#### Title
`Story: Add Oracle dialect identity and capability module`

#### User Story
As a SQM maintainer, I want Oracle to have a first-class dialect id and shared capability matrix so all downstream layers can make explicit Oracle support decisions.

#### Acceptance Criteria

- `SqlDialectId.ORACLE` exists with aliases.
- `sqm-core-oracle` exists and is registered in the reactor.
- `OracleCapabilities` covers baseline feature gates.
- Unit tests cover alias normalization and capability version gates.

#### Labels
`story`, `oracle`, `dialect`, `core`, `capabilities`

---

### Story R10-2

#### Title
`Story: Add Oracle parser and renderer modules`

#### User Story
As a SQM maintainer, I want dedicated Oracle parser and renderer modules so Oracle syntax is isolated from existing dialect implementations and registered deliberately.

#### Acceptance Criteria

- `sqm-parser-oracle` and `sqm-render-oracle` exist and compile.
- `OracleSpecs` and `OracleDialect` are registered and tested.
- Baseline `SELECT 1 FROM dual` or equivalent smoke tests parse and render.
- Unsupported Oracle-only syntax emits clear diagnostics until its model story lands.

#### Labels
`story`, `oracle`, `parser`, `renderer`

#### Depends On
R10-1

---

### Story R10-3

#### Title
`Story: Implement baseline Oracle query support`

#### User Story
As a SQM user targeting Oracle, I want baseline Oracle query syntax to parse, validate, and render so common read workflows can round-trip through SQM.

#### Acceptance Criteria

- Oracle identifiers, literals, common expressions, joins, grouping, ordering, windows, and row limiting parse and render.
- `LATERAL` / apply support is implemented only for model-compatible shapes.
- Validation covers Oracle query feature boundaries.
- Tests cover happy paths, invalid syntax, unsupported syntax, and version-gated syntax.

#### Labels
`story`, `oracle`, `query`, `parser`, `renderer`, `validation`

#### Depends On
R10-2

---

### Story R10-4

#### Title
`Story: Implement baseline Oracle DML and MERGE support`

#### User Story
As a SQM user targeting Oracle, I want supported Oracle DML and MERGE shapes to parse, validate, render, and reject non-Oracle mutation semantics explicitly.

#### Acceptance Criteria

- `INSERT`, `UPDATE`, `DELETE`, and baseline `MERGE` parse/render/validate.
- Oracle `RETURNING ... INTO` is either implemented with a reviewed target model or explicitly rejected in parser, renderer, validation, and transpilation.
- SQL Server-only `OUTPUT` and PostgreSQL-only `RETURNING` semantics do not leak into Oracle.
- Tests cover valid DML, invalid DML, and dialect-boundary cases.

#### Labels
`story`, `oracle`, `dml`, `merge`, `parser`, `renderer`, `validation`, `transpile`

#### Depends On
R10-2, R10-3

---

### Story R10-5

#### Title
`Story: Add Oracle validation and function catalog`

#### User Story
As a SQM user, I want Oracle-specific validation and function signatures so unsupported features and function misuse are diagnosed before rendering or execution.

#### Acceptance Criteria

- `OracleValidationDialect` exists.
- `OracleFunctionCatalog` covers a practical baseline function set.
- Oracle-specific feature validation rules are registered.
- `docs/validation/VALIDATION_FEATURES.md` is updated.

#### Labels
`story`, `oracle`, `validation`, `functions`

#### Depends On
R10-1, R10-3, R10-4

---

### Story R10-6

#### Title
`Story: Add Oracle transpilation awareness`

#### User Story
As a SQM user converting SQL between dialects, I want Oracle source and target behavior to be represented by exact, approximate, and unsupported transpilation outcomes.

#### Acceptance Criteria

- `DefaultSqlTranspiler` supports Oracle defaults.
- Oracle built-in transpilation rules are registered.
- Tests cover Oracle as source and target for exact, unsupported, and approximate-disabled outcomes.
- Transpilation docs list the Oracle rule matrix.

#### Labels
`story`, `oracle`, `transpile`, `validation`

#### Depends On
R10-3, R10-4, R10-5

---

### Story R10-7

#### Title
`Story: Add Oracle catalog, codegen, control, middleware, and playground integration`

#### User Story
As a SQM maintainer, I want Oracle wired into product and developer-facing layers so Oracle support is usable beyond parser and renderer unit tests.

#### Acceptance Criteria

- `OracleSqlTypeMapper` exists and is tested.
- Codegen accepts `oracle` and validates with Oracle parser/validator.
- Maven plugin can use Oracle catalog type mapping.
- Control default dialect-aware parser/renderer/validator includes Oracle.
- Middleware and playground expose Oracle where dialect lists are shown.

#### Labels
`story`, `oracle`, `catalog`, `codegen`, `control`, `middleware`, `playground`

#### Depends On
R10-1, R10-2, R10-5

---

### Story R10-8

#### Title
`Story: Decide dedicated model work for Oracle advanced features`

#### User Story
As a SQM maintainer, I want Oracle advanced features reviewed through the modeling rules before implementation so SQM avoids stringly or syntax-shaped abstractions.

#### Acceptance Criteria

- Design decisions are recorded for `RETURNING ... INTO`, `CONNECT BY`, `JSON_TABLE`, and Oracle time-zone conversion.
- Each accepted node follows repo requirements: visitor, transformer, matchers where justified, JSON mixins, DSL helpers, codegen, parser, renderer, validation, transpilation, tests, and `MODEL.md`.
- Rejected/deferred features are documented explicitly.

#### Labels
`story`, `oracle`, `model`, `dsl`, `codegen`, `json`, `transpile`

#### Depends On
R10-3, R10-4

---

## Risks

- Oracle `RETURNING ... INTO` looks like PostgreSQL `RETURNING` but has different client-result semantics.
- Oracle `JSON_TABLE` looks like a table-valued function but has enough structure to deserve a dedicated model review.
- `CONNECT BY` can be mistaken for recursive CTE syntax, but its traversal semantics are distinct.
- Empty-string-as-null behavior can make apparently exact expression rewrites lossy.
- Oracle 19c, 23ai, and 26ai have different feature surfaces; capability gates must not treat all versions as one product.
- Oracle live DB integration may be heavier than PostgreSQL/MySQL test containers; keep deterministic unit coverage first.

## Publishing GitHub Issues

The epic and stories can be published to GitHub issues from this markdown source.

Preview:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r10-oracle-issues.ps1 -WhatIf
```

Publish:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r10-oracle-issues.ps1
```

The wrapper delegates to the generic publisher in `scripts/create-github-issues-from-epic-md.ps1`.

## Definition of Done

- Oracle modules are in the reactor and released artifacts.
- Parser, renderer, validation, transpilation, control, codegen, catalog, middleware, playground, integration tests, and docs all recognize Oracle explicitly.
- Query and DML scope are implemented or rejected with clear diagnostics.
- DDL remains out of scope and documented as such.
- Every new public class or method has JavaDoc.
- Every new behavior has tests.
- No Oracle syntax is modeled in `sqm-core` unless it passes the shared-semantics review in `SQM_MODELING_RULES.md`.
