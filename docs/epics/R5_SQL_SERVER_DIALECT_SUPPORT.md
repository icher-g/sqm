## Epic

### Title
`Epic: R5 SQL Server Dialect Support`

### Problem Statement
SQM currently provides first-class parser, renderer, validation, middleware, and transpilation support across ANSI, PostgreSQL, and MySQL, but SQL Server remains a planned dialect rather than a supported one.

As a result:

- users cannot parse or render SQL Server SQL through the same public APIs used for the existing dialects
- middleware and control flows cannot accept SQL Server as a supported runtime dialect
- validation and transpilation layers cannot report SQL Server exact, approximate, or unsupported behavior in a first-class way
- DSL and codegen coverage cannot guarantee that SQL Server-capable model shapes remain ergonomic and complete

SQL Server is also an important architecture checkpoint because it stresses several extension seams at once:

- bracket-quoted identifiers
- dual pagination forms (`TOP` and `OFFSET/FETCH`)
- SQL Server boolean rendering differences
- SQL Server-specific function vocabulary
- SQL Server DML follow-up on top of the existing shared DML model

### Epic Goal
Deliver first-class SQL Server support across SQM query and DML flows, including parser, renderer, validation, transpilation review, downstream runtime wiring, DSL reachability, and codegen compatibility.

R5 should explicitly exclude DDL, while ensuring that SQL Server query support is not considered complete until baseline DML support is also delivered by the end of the epic.
DDL is treated as a separate framework-level product and architecture decision, not as an assumed future extension of the SQL Server dialect track.

### Business Value
- Expands SQM into a major production dialect family.
- Proves that the current architecture can absorb a new dialect across parser, renderer, validate, transpile, control, middleware, DSL, and codegen layers.
- Creates a stable foundation for future SQL Server-specific advanced DML work.
- Reduces future dialect-epic risk by making completeness requirements explicit instead of parser/renderer-only.

### Scope Boundaries

#### Query
- In scope for the full epic.

#### DML
- In scope for the full epic.
- May be sequenced after query support, but must be complete before the epic is considered done.

#### DDL
- Out of scope for this epic.
- Must not appear in implementation scope, acceptance criteria, or story completion requirements for R5.
- Must not be described as merely deferred SQL Server work; DDL support is an open framework decision outside this epic.

### Non-Goals
- DDL support.
- SQL Server-specific advanced DML extensions that require new semantics beyond the shared baseline unless explicitly pulled into a follow-up story:
  - `OUTPUT`
  - `MERGE`
  - table hints such as `WITH (NOLOCK)`
  - `TOP ... PERCENT`
  - `TOP ... WITH TIES`
- Full SQL Server catalog/type-mapper parity beyond what is needed for runtime and codegen compatibility review.

### Definition of Done
- `sqm-core-sqlserver`, `sqm-parser-sqlserver`, and `sqm-render-sqlserver` exist and are wired into the multi-module build.
- `SqlServerCapabilities`, `SqlServerSpecs`, and `SqlServerDialect` are implemented with version-aware feature gating.
- SQL Server query support is delivered for the selected R5 slice.
- SQL Server baseline DML support is delivered for the existing shared DML model where syntax is supported by SQL Server.
- `sqm-validate` and any SQL Server-specific validation support needed for dialect completeness are implemented.
- `sqm-transpile` is reviewed and updated so SQL Server exact, approximate, and unsupported behavior is explicit for the selected slice.
- `sqm-control`, middleware, and integration tests support SQL Server dialect ids and runtime flows.
- SQL Server-capable model shapes are reachable through DSL helpers, and codegen is updated where needed so DSL-backed generated usage remains complete.
- Documentation is updated for scope, examples, and supported/deferred behavior.

### Suggested Labels
`epic`, `sqlserver`, `dialect`, `parser`, `renderer`, `dml`

---

## Scope Summary

### In Scope
- Query support.
- Baseline DML support using the existing shared DML model.
- New dialect modules:
  - `sqm-core-sqlserver`
  - `sqm-parser-sqlserver`
  - `sqm-render-sqlserver`
- Shared SQL Server capability matrix and version-aware specs.
- SQL Server identifier quoting:
  - bracket quoting (`[name]`)
  - optional double-quoted identifiers when explicitly enabled
- SQL Server pagination:
  - `TOP n`
  - `TOP (expr)`
  - `ORDER BY ... OFFSET ... ROWS`
  - `ORDER BY ... OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`
- SQL Server boolean rendering behavior for predicates and literals.
- Prioritized SQL Server function parsing/rendering coverage using existing generic `FunctionExpr` infrastructure.
- SQL Server parser/render support for the shared DML baseline:
  - `INSERT`
  - `UPDATE`
  - `DELETE`
- `sqm-validate` coverage for SQL Server-supported query and DML shapes.
- `sqm-transpile` review and initial SQL Server-target/source handling for the selected supported subset.
- Runtime registration in `sqm-control` and middleware flows.
- DSL/helper additions needed to express any newly reachable model surface ergonomically.
- `sqm-codegen` and `sqm-codegen-maven-plugin` updates needed to keep node coverage and generated DSL usage aligned with supported model shapes.
- README, downstream support matrix, and epic/docs updates.

### Explicitly Deferred
- DDL.
- `OUTPUT`.
- `MERGE`.
- table/query hints such as `WITH (NOLOCK)`.
- `TOP ... PERCENT`.
- `TOP ... WITH TIES`.
- SQL Server-specific advanced DML syntax that requires new core semantics beyond the shared baseline.
- Large catalog/introspection/type-mapper expansion unless required by an accepted codegen/runtime story.

---

## Design Overview

### 1. Module Layout

SQL Server should follow the same family split used by MySQL and PostgreSQL:

- `sqm-core-sqlserver`
  - shared SQL Server capability matrix
- `sqm-parser-sqlserver`
  - SQL Server parser specs, lookups, and parser overrides
- `sqm-render-sqlserver`
  - SQL Server dialect implementation, identifier quoter, pagination style, and render overrides

This keeps `sqm-core` dialect-neutral while still allowing parser and renderer modules to share one SQL Server feature matrix.

### 2. Dialect Identity

`io.sqm.core.dialect.SqlDialectId` should gain a built-in SQL Server constant and normalization aliases:

- canonical id: `sqlserver`
- accepted aliases: `sqlserver`, `mssql`, `tsql`

This id must be used consistently in:

- `sqm-control`
- middleware request handling
- `sqm-transpile`
- tests and documentation examples

### 3. Shared Capabilities

Add `io.sqm.core.sqlserver.dialect.SqlServerCapabilities` in `sqm-core-sqlserver`.

The capability matrix should cover both query and the R5 DML baseline. Expected supported areas include:

- identifier quoting behaviors already represented by the shared model
- query pagination through existing `LimitOffset` semantics
- expression/function support that already maps through existing core nodes
- baseline `INSERT`, `UPDATE`, and `DELETE` statement families where SQL Server supports the shared syntax directly

Expected explicitly unsupported capabilities in R5 include:

- `DML_RESULT_CLAUSE`
- `INSERT_ON_CONFLICT`
- `INSERT_ON_DUPLICATE_KEY_UPDATE`
- `REPLACE_INTO`
- `UPDATE_JOIN`
- `DELETE_USING_JOIN`
- `ORDER_BY_USING`
- MySQL/PostgreSQL-specific literal/operator features

Important design point:
R5 should prefer capability gating over parser ambiguity. If a syntax family is out of scope, parser, renderer, validate, and transpile behavior should reject it clearly instead of partially accepting it.

### 4. Parser Architecture

Add `SqlServerSpecs` in `sqm-parser-sqlserver`, following the same role as `MySqlSpecs` and `PostgresSpecs`.

Suggested baseline behavior:

- default version targets a modern SQL Server line that supports `OFFSET/FETCH`
- bracket identifiers are always accepted
- double-quoted identifiers are controlled explicitly through a SQL Server parser option analogous to `QUOTED_IDENTIFIER`

Suggested public surface:

- `SqlServerSpecs()`
- `SqlServerSpecs(SqlDialectVersion version)`
- `SqlServerSpecs(SqlDialectVersion version, boolean quotedIdentifierMode)`

Parser wiring should start from `io.sqm.parser.ansi.Parsers.ansiCopy()` and register SQL Server-specific overrides.

#### Core parser responsibilities

- `SqlServerLookups`
  - SQL Server keywords and reserved words
  - function/operator vocabulary needed by parser disambiguation
- `SqlServerSelectQueryParser`
  - extend ANSI `SelectQueryParser`
  - parse `TOP` in `parseAfterSelectKeyword(...)`
  - map `TOP` into existing `LimitOffset.limit(...)` when there is no offset
  - reject invalid combinations such as `TOP` mixed with offset/fetch in a single baseline query
- `SqlServerLimitOffsetParser`
  - reuse ANSI `OFFSET/FETCH` parsing shape where possible
  - add SQL Server-specific validation:
    - `OFFSET/FETCH` requires `ORDER BY`
    - `FETCH` without an effective offset should normalize through `OFFSET 0` only on render, not through a new core node
- SQL Server DML parsers
  - start from ANSI DML parsers
  - accept shared SQL Server-supported statement shapes for `INSERT`, `UPDATE`, and `DELETE`
  - reject explicitly unsupported SQL Server-only or non-SQL Server DML extensions through clear capability-aware errors

#### TOP scope decision

R5 should support:

- `TOP 10`
- `TOP (10)`
- `TOP (@n)` if the token stream already supports the expression form used there

R5 should reject:

- `TOP 10 PERCENT`
- `TOP 10 WITH TIES`

Those forms are not representable through the current `LimitOffset` model without adding SQL Server-only semantics to `sqm-core`, which is out of scope for R5.

### 5. Renderer Architecture

Add `SqlServerDialect` in `sqm-render-sqlserver`, following the same SPI contract as existing dialects.

Recommended supporting classes:

- `SqlServerIdentifierQuoter`
- `SqlServerPaginationStyle`
- `SqlServerBooleans`
- `SqlServerOperators`
- `SqlServerRenderers`

#### Identifier quoting

The core model already preserves SQL Server bracket quote style through `QuoteStyle.BRACKETS`, so no new core node is needed.

Rendering rules should be:

- default quote style: brackets
- bracket escaping by doubling `]`
- optional double-quote support only when enabled in the dialect configuration

#### Pagination rendering

The current renderer architecture is already close to what SQL Server needs:

- `SelectQueryRenderer` can inject `TOP` after `SELECT`
- `LimitOffsetRenderer` already supports `OFFSET/FETCH`
- `PaginationStyle` can report support for both `TOP` and `OFFSET/FETCH`

The SQL Server renderer should adopt this policy:

- limit only, no offset: render `TOP`
- offset present: render `ORDER BY ... OFFSET ... [FETCH NEXT ...]`
- limit-all: reject
- limit/offset without required `ORDER BY`: reject with a clear dialect error

This means R5 should avoid introducing a SQL Server-specific pagination node. Existing `SelectQuery` plus `LimitOffset` is sufficient for the baseline query slice.

#### Boolean behavior

SQL Server does not expose ANSI-style boolean literals in the same way as PostgreSQL/MySQL. R5 should therefore define SQL Server boolean rendering explicitly rather than inheriting `AnsiBooleans`.

Design expectation:

- render `TRUE` as `1`
- render `FALSE` as `0`
- require explicit predicates where the renderer already uses `Booleans.requireExplicitPredicate()`

This should be tested carefully because it is one of the most meaningful renderer-behavior differences for SQL Server.

#### DML rendering

R5 DML rendering should cover the shared SQL Server-supported baseline:

- `INSERT`
- `UPDATE`
- `DELETE`

Renderer responsibilities:

- render baseline shared DML syntax deterministically
- reject PostgreSQL/MySQL-only DML extensions clearly
- keep SQL Server-specific advanced DML extensions out of scope unless explicitly added later

### 6. Validation

R5 must include validation review and implementation, not just parser/render support.

Required validation outcomes:

- `sqm-validate` base flows accept supported SQL Server query and DML shapes
- unsupported SQL Server features are surfaced as explicit validation problems when validation is the right enforcement layer
- if SQL Server requires any optional dialect rule pack, it should be added explicitly rather than left implicit

Validation should cover:

- pagination constraints such as `OFFSET/FETCH` requiring `ORDER BY`
- DML unsupported-feature reporting for rejected extensions
- any SQL Server-specific rules that are necessary to prevent parser/render acceptance gaps

### 7. Transpilation

R5 must include `sqm-transpile` review and updates so SQL Server support is explicit at the architecture level.

R5 does not need to deliver a broad SQL Server transpilation matrix, but it must avoid leaving SQL Server invisible in the transpilation layer.

Minimum transpilation expectations:

- SQL Server can be represented as a source and target dialect id
- clearly exact constructs in the selected query/DML baseline are supported where rule infrastructure already allows it
- unsupported SQL Server source/target features are reported explicitly
- documentation calls out what SQL Server transpilation coverage exists versus what remains deferred

### 8. Function Coverage Strategy

R5 should not introduce SQL Server-specific function nodes unless a function truly requires its own semantic model. Most SQL Server functions can remain generic `FunctionExpr` instances.

The baseline function slice should focus on names that provide immediate user value and exercise parser/renderer differences without new model work. Recommended first-wave coverage:

- `LEN(...)`
- `DATALENGTH(...)`
- `GETDATE()`
- `DATEADD(...)`
- `DATEDIFF(...)`
- `ISNULL(...)`
- `STRING_AGG(...)`

Acceptance for this slice:

- parser accepts these functions in ordinary expression positions
- renderer preserves canonical SQL Server spelling
- tests verify round-trip stability

If a function already works through generic function parsing without SQL Server-specific code, R5 should not add special handling just to register it.

### 9. DSL And Codegen Completeness

R5 must not stop at AST support. Any newly relevant public model surface must remain ergonomic and reachable through DSL helpers.

Required outcomes:

- if a supported SQL Server shape depends on a model construct that is not ergonomic in DSL today, add or refine DSL helpers
- tests should prefer DSL/helper usage when constructing model examples
- `sqm-codegen` must be reviewed to confirm all relevant nodes are expressible via DSL methods
- `sqm-codegen-maven-plugin` must be updated if SQL Server support changes dialect selection, validation, or emitted DSL usage expectations

R5 should treat DSL/codegen review as a completion gate, not optional cleanup.

### 10. Downstream Wiring

R5 should add SQL Server support to the runtime layers that choose parser/renderer implementations by dialect id.

Required registration points:

- parent `pom.xml` module list
- `sqm-control`:
  - `SqlStatementParser.standard()`
  - `SqlStatementRenderer.standard()`
- middleware smoke tests
- SQL dialect documentation/examples

The downstream support story for R5 should cover query and baseline DML runtime flows. DDL remains intentionally outside this epic and outside the assumed framework scope unless separately approved.

### 11. Documentation Updates

R5 should update:

- `README.md`
- `docs/planning/ROADMAP.md` once delivered
- `docs/downstream/DOWNSTREAM_SUPPORT_MATRIX.md`
- SQL Server usage examples comparable to the PostgreSQL/MySQL sections
- any transpilation or validation docs touched by SQL Server support

The documentation should make the scope boundary explicit:

- query is supported
- baseline DML is supported by epic completion
- DDL is out of scope and not assumed to be part of the framework roadmap
- advanced SQL Server-specific features are deferred

---

## Risks And Design Constraints

### 1. Pagination Semantics Are Not Fully Symmetric

SQL Server supports both `TOP` and `OFFSET/FETCH`, but not as interchangeable free-form syntax. The design must preserve these constraints:

- `TOP` is the canonical render target for limit-only queries
- `OFFSET/FETCH` requires `ORDER BY`
- `LIMIT ALL` has no SQL Server equivalent

This is the highest-value correctness area in the epic.

### 2. SQL Server Boolean Semantics Differ From ANSI

Using ANSI boolean rendering would likely produce invalid or misleading SQL Server output. SQL Server boolean literal handling must therefore be a first-class dialect decision in R5.

### 3. DML Completeness Must Not Slip Out Of The Epic

Because the shared DML model already exists, it would be easy to declare SQL Server “supported” after query delivery and leave DML for later. R5 should explicitly forbid that outcome. Query-only completion is not sufficient.

### 4. Advanced T-SQL Features Can Easily Overexpand Scope

SQL Server has many tempting extensions:

- `OUTPUT`
- table hints
- `MERGE`
- `TOP ... WITH TIES`
- variable syntax and procedural extensions

R5 should remain disciplined: baseline query + baseline DML + required downstream completeness, but no DDL and no unnecessary semantic expansion.

### 5. DSL And Codegen Gaps Are Easy To Miss

Parser/renderer work can appear complete even when developers cannot express the same nodes ergonomically through DSL or generated code. R5 should require an explicit DSL/codegen checklist before closure.

---

## User Stories

### Story S1

#### Title
`Story: Add SQL Server core capability module and dialect identity`

#### User Story
As a SQM maintainer, I want SQL Server capabilities and dialect identity defined in shared core structures so that parser, renderer, validate, control, and transpile layers can agree on one SQL Server contract.

#### Acceptance Criteria
- `sqm-core-sqlserver` is added to the multi-module build.
- `SqlServerCapabilities` exposes a shared, version-aware capability matrix.
- `SqlDialectId` includes built-in SQL Server normalization and aliases.
- Tests verify expected supported and unsupported SQL Server features for the default version.

#### Labels
`story`, `sqlserver`, `sqm-core`

#### Depends On
None

---

### Story S2

#### Title
`Story: Add SQL Server parser specs and query registrations`

#### User Story
As a SQM user, I want a dedicated SQL Server parser configuration so that I can parse T-SQL query syntax through the same `ParseContext` API used for other dialects.

#### Acceptance Criteria
- `sqm-parser-sqlserver` is added to the build.
- `SqlServerSpecs` is implemented with version-aware capabilities.
- SQL Server parser registrations are assembled from an ANSI repository copy plus SQL Server overrides.
- Identifier quoting supports SQL Server brackets by default and optional double quotes when enabled.
- Tests cover default and explicit parser-mode behavior.

#### Labels
`story`, `sqlserver`, `parser`, `query`

#### Depends On
S1

---

### Story S3

#### Title
`Story: Parse SQL Server TOP and OFFSET/FETCH query pagination`

#### User Story
As a SQM user, I want SQL Server query pagination to map into the existing SQM query model so that I can analyze and transform paginated T-SQL queries without a SQL Server-only AST.

#### Acceptance Criteria
- `TOP n` and `TOP (expr)` parse into existing `SelectQuery` and `LimitOffset` structures.
- `OFFSET/FETCH` parses through existing pagination model structures.
- Parser rejects unsupported or invalid baseline combinations, including:
  - `TOP` with `PERCENT`
  - `TOP` with `WITH TIES`
  - `OFFSET/FETCH` without `ORDER BY`
  - ambiguous combinations of `TOP` and `OFFSET/FETCH`
- Tests cover happy paths, rejection paths, and canonical round-trip expectations.

#### Labels
`story`, `sqlserver`, `parser`, `pagination`

#### Depends On
S2

---

### Story S4

#### Title
`Story: Add SQL Server renderer and query dialect SPI`

#### User Story
As a SQM user, I want a SQL Server rendering dialect so that existing SQM query statements can be rendered as valid T-SQL where the selected feature slice is supported.

#### Acceptance Criteria
- `sqm-render-sqlserver` is added to the build.
- `SqlServerDialect` is implemented with SQL Server-specific identifier quoting, pagination style, operators, and boolean behavior.
- Renderer uses `TOP` for limit-only queries and `OFFSET/FETCH` for offset-based pagination.
- Renderer rejects unsupported SQL Server pagination shapes such as `LIMIT ALL`.
- Tests cover identifier quoting, boolean literal rendering, and pagination rendering.

#### Labels
`story`, `sqlserver`, `renderer`, `query`

#### Depends On
S1

---

### Story S5

#### Title
`Story: Deliver SQL Server baseline DML parser and renderer support`

#### User Story
As a SQM user, I want SQL Server support for the shared DML baseline so that INSERT, UPDATE, and DELETE statements are first-class citizens in the dialect by the time the epic is complete.

#### Acceptance Criteria
- SQL Server parser support exists for baseline shared-model `INSERT`, `UPDATE`, and `DELETE` shapes supported by the dialect.
- SQL Server renderer support exists for the same baseline DML scope.
- Unsupported non-SQL Server and advanced SQL Server DML extensions are rejected clearly.
- Tests cover happy paths, unsupported paths, and round-trip behavior.

#### Labels
`story`, `sqlserver`, `parser`, `renderer`, `dml`

#### Depends On
S2, S4

---

### Story S6

#### Title
`Story: Add SQL Server validation coverage`

#### User Story
As a SQM user, I want SQL Server validation support so that supported and unsupported SQL Server query/DML shapes are enforced consistently outside the parser and renderer.

#### Acceptance Criteria
- `sqm-validate` accepts supported SQL Server query and DML baseline shapes.
- SQL Server-specific validation rules are added where needed for correctness.
- Validation tests cover pagination constraints and unsupported-feature reporting.
- Validation behavior does not silently rely on parser/renderer failures alone.

#### Labels
`story`, `sqlserver`, `validate`

#### Depends On
S3, S5

---

### Story S7

#### Title
`Story: Add SQL Server transpilation awareness`

#### User Story
As a SQM user, I want SQL Server to be represented explicitly in transpilation flows so that exact, approximate, and unsupported SQL Server behavior is visible rather than implicit.

#### Acceptance Criteria
- `sqm-transpile` recognizes SQL Server as a source and target dialect.
- Exact and unsupported SQL Server behavior is explicit for the selected supported slice.
- Tests cover at least one supported and one unsupported SQL Server transpilation path.
- Documentation states the SQL Server transpilation scope delivered by R5.

#### Labels
`story`, `sqlserver`, `transpile`

#### Depends On
S1, S3, S5

---

### Story S8

#### Title
`Story: Deliver SQL Server function coverage and DSL ergonomics`

#### User Story
As a SQM user, I want common SQL Server functions and related model shapes to be both supported and ergonomic through DSL helpers so that examples, tests, and generated code remain developer-friendly.

#### Acceptance Criteria
- Prioritized first-wave SQL Server functions are accepted in parser and renderer flows.
- The baseline function slice includes at least:
  - `LEN`
  - `DATALENGTH`
  - `GETDATE`
  - `DATEADD`
  - `DATEDIFF`
  - `ISNULL`
  - `STRING_AGG`
- DSL coverage is reviewed and updated when model usage would otherwise be awkward.
- Tests prefer DSL/helper usage where applicable.

#### Labels
`story`, `sqlserver`, `functions`, `dsl`

#### Depends On
S2, S4

---

### Story S9

#### Title
`Story: Update codegen for SQL Server-capable model surface`

#### User Story
As a SQM maintainer, I want codegen to stay aligned with SQL Server-supported nodes so that generated Java usage continues to rely on complete DSL coverage rather than unsupported low-level constructs.

#### Acceptance Criteria
- `sqm-codegen` is reviewed for all SQL Server-relevant node shapes.
- Missing DSL-backed emission support is added where needed.
- `sqm-codegen-maven-plugin` is updated if SQL Server dialect handling or validation changes require it.
- Tests cover any new codegen behavior introduced by the SQL Server epic.

#### Labels
`story`, `sqlserver`, `codegen`, `dsl`

#### Depends On
S5, S8

---

### Story S10

#### Title
`Story: Register SQL Server in control and middleware pipelines`

#### User Story
As a middleware user, I want SQL Server to be selectable as a runtime dialect so that analyze/validate/rewrite/render flows can run against T-SQL inputs using the standard service APIs.

#### Acceptance Criteria
- `sqm-control` standard parser and renderer registrations support SQL Server.
- Middleware smoke tests cover successful SQL Server query and DML request paths.
- Unsupported SQL Server features surface clear diagnostics rather than silent fallback.
- Downstream runtime support reflects query plus baseline DML coverage.

#### Labels
`story`, `sqlserver`, `middleware`, `control`

#### Depends On
S5, S6

---

### Story S11

#### Title
`Story: Add SQL Server integration coverage`

#### User Story
As a SQM maintainer, I want end-to-end SQL Server integration tests so that parser, renderer, validation, and middleware regressions are caught at the cross-module level instead of only in unit suites.

#### Acceptance Criteria
- `sqm-it` contains SQL Server query and DML round-trip tests.
- Coverage includes:
  - bracket-quoted identifiers
  - `TOP`
  - `OFFSET/FETCH`
  - prioritized SQL Server functions
  - baseline `INSERT`, `UPDATE`, and `DELETE`
  - rejection of explicitly unsupported SQL Server forms
- Middleware integration coverage includes SQL Server smoke paths.

#### Labels
`story`, `sqlserver`, `integration-test`

#### Depends On
S5, S6, S7, S10

---

### Story S12

#### Title
`Story: Document SQL Server dialect scope and downstream completeness`

#### User Story
As a contributor or library user, I want the supported SQL Server slice documented clearly so that I know what works today, what is intentionally deferred, and which downstream modules are included in epic completeness.

#### Acceptance Criteria
- README includes SQL Server parser/render usage examples.
- The downstream support matrix is updated for delivered SQL Server query and DML runtime support.
- Documentation clearly lists supported baseline features and deferred features.
- Documentation explicitly states that DDL is out of scope for R5.
- The epic markdown is suitable for splitting into implementation issues.

#### Labels
`story`, `sqlserver`, `docs`

#### Depends On
S7, S9, S10, S11

---

## Suggested Delivery Order

1. `S1` shared SQL Server capabilities and dialect identity
2. `S2` parser specs and query registrations
3. `S3` query pagination parsing
4. `S4` renderer and query dialect SPI
5. `S5` baseline DML parser/renderer support
6. `S6` validation coverage
7. `S7` transpilation awareness
8. `S8` function coverage and DSL ergonomics
9. `S9` codegen updates
10. `S10` control and middleware registration
11. `S11` integration tests
12. `S12` documentation and support-matrix updates

## Exit Notes

R5 is not complete if any of the following are missing:

- SQL Server baseline DML support
- validation coverage
- transpilation review and explicit SQL Server behavior
- DSL reachability for supported model shapes
- codegen alignment
- downstream runtime support for query plus baseline DML

If schedule pressure requires trimming, the first candidates to defer are:

- wider function coverage beyond the first-wave list
- broader middleware examples beyond smoke coverage
- catalog/type-mapper expansion not required for accepted runtime/codegen stories

The following must not be deferred without explicitly redefining the epic:

- baseline DML completion
- validation
- transpilation awareness
- DSL/codegen completeness
- DDL remaining out of scope and outside the assumed framework scope
