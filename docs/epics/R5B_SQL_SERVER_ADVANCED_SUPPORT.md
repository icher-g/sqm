## Epic

### Title
`Epic: R5B SQL Server Advanced Support`

### Problem Statement
`R5` establishes SQL Server baseline support across query, baseline DML, validation, transpilation awareness, DSL, codegen, control, and middleware. That baseline intentionally excludes several important SQL Server capabilities that are commonly needed in production:

- `OUTPUT` in DML statements
- `MERGE`
- table and query hints such as `WITH (NOLOCK)` and related hint forms
- `TOP ... PERCENT`
- `TOP ... WITH TIES`
- broader SQL Server catalog/introspection and type-mapper support
- broader SQL Server transpilation coverage

Without a follow-up design, those gaps risk being implemented piecemeal across unrelated stories, leading to:

- inconsistent core modeling choices
- parser/render/validate/transpile drift
- incomplete DSL and codegen support for new SQL Server semantics
- confusion about which SQL Server work belongs in the SQL Server track versus generic DDL or generic catalog epics

### Epic Goal
Deliver the advanced SQL Server features intentionally deferred by `R5`, while keeping DDL explicitly out of scope and aligning new work with the existing roadmap boundaries for advanced DML (`R13`) and catalog unification (`R10`).

This epic is the SQL Server-specific follow-up that completes the deferred feature set from `R5` without collapsing into DDL work.
DDL is treated as a separate framework-level decision, not as an assumed later stage of SQL Server support.

### Business Value
- Completes the most important SQL Server-specific production features left after baseline dialect support.
- Provides a coherent path for advanced SQL Server DML instead of scattering the work across unrelated follow-ups.
- Makes SQL Server a more credible target for migration and transpilation workflows.
- Reduces future rework by forcing new semantics through the full parser/render/validate/transpile/DSL/codegen pipeline.

### Relationship To Other Epics

#### Depends On
- `R5` for baseline SQL Server query and baseline DML support.

#### Coordinates With
- `R10` for broader dialect catalog/type-mapper unification.
- `R13` for the project-wide advanced DML program.

#### DDL Boundary
- This epic does not assume SQM should support DDL.
- If DDL is ever explored, it requires a separate framework-level design decision.

### Scope Boundaries

#### Query
- In scope only for advanced SQL Server query features that were deferred by `R5`, primarily `TOP ... PERCENT`, `TOP ... WITH TIES`, and SQL Server hint forms attached to queries.

#### DML
- In scope for advanced SQL Server DML features, especially `OUTPUT`, `MERGE`, and SQL Server hint forms that affect DML statements.

#### DDL
- Out of scope.
- Any SQL Server DDL syntax is outside this follow-up and outside the current assumed framework scope unless a separate DDL decision is made.

### Non-Goals
- General DDL support.
- Generic non-SQL Server advanced DML work unless it is needed to support shared core abstractions.
- Full SQL Server procedural T-SQL support.
- Stored procedures, batches, variable declarations, or control-flow syntax.

### Definition of Done
- New SQL Server-specific advanced semantics required by this epic are modeled in `sqm-core` with full visitor/transformer/matcher/json/docs coverage.
- Parser/render/validate/transpile behavior is implemented for the selected advanced SQL Server slice.
- DSL coverage exists for any new public model surface.
- `sqm-codegen` and `sqm-codegen-maven-plugin` are updated where new node shapes or dialect flows require it.
- `sqm-control`, middleware, and integration tests cover the delivered advanced SQL Server features where relevant.
- DDL remains out of scope and is not introduced accidentally through this epic.

### Suggested Labels
`epic`, `sqlserver`, `advanced`, `dml`, `transpile`, `catalog`

---

## Scope Summary

### In Scope
- SQL Server `OUTPUT` support for `INSERT`, `UPDATE`, and `DELETE`.
- SQL Server `MERGE` support, if introduced through a deliberate shared-model design.
- SQL Server table/query hints in supported statement contexts.
- SQL Server `TOP ... PERCENT`.
- SQL Server `TOP ... WITH TIES`.
- SQL Server catalog/type-mapper follow-up required for real SQL Server downstream support.
- Broader SQL Server transpilation coverage for the advanced SQL Server slice, including:
  - explicit exact rewrites where SQL Server and non-SQL Server model shapes are safely transformable
  - explicit unsupported rules for advanced SQL Server source and target features with no safe mapping
  - SQL Server function-family transpilation rules where mappings are exact or clearly approximate
  - advanced pagination transpilation rules for `TOP ... PERCENT`, `TOP ... WITH TIES`, and `OFFSET/FETCH` semantics where applicable
- Validation, DSL, codegen, control, middleware, and integration coverage for all new behavior.

### Explicitly Deferred
- DDL.
- Stored procedures and procedural T-SQL.
- Temporary table lifecycle semantics unless required by catalog work.
- SQL Server-specific security/admin statements.

---

## Design Overview

### 1. Core Modeling Strategy

This epic should not force SQL Server semantics into parser/renderer-only hacks. If a feature cannot be represented cleanly by the existing shared model, the model must be extended first.

Likely model additions or expansions:

- DML output projection model for SQL Server `OUTPUT`
- advanced pagination metadata for `TOP ... PERCENT` and `TOP ... WITH TIES`
- statement-level or table-reference-level hint models
- possibly a first-class `MergeStatement` model if `MERGE` is accepted into scope

Any new node introduced here must satisfy the standard repository requirements:

- dedicated interface and `accept()` method
- visitor coverage
- recursive transformer coverage
- matchers coverage
- JSON mixins coverage
- `MODEL.md` updates
- dedicated parser and renderer, even where unsupported in some dialects

### 2. SQL Server OUTPUT

`OUTPUT` is the most important SQL Server-specific advanced DML feature deferred from `R5`.

#### Design goals
- Support `INSERT ... OUTPUT ...`
- Support `UPDATE ... OUTPUT ...`
- Support `DELETE ... OUTPUT ...`
- Support `OUTPUT ... INTO ...` only if the target semantics can be modeled cleanly in shared SQM structures

#### Core design question
The project must decide whether `OUTPUT` is:

- SQL Server-specific enough to warrant its own DML output node family, or
- abstractable into a broader shared DML result-projection semantic that could later relate to PostgreSQL `RETURNING`

Recommended direction:
introduce a shared DML result-projection abstraction if it can model both SQL Server `OUTPUT` and existing/future `RETURNING`-style features cleanly. If not, use a SQL Server-specific node now, but document why a shared abstraction was rejected.

#### Required downstream work
- parser support
- renderer support
- validation support
- transpilation unsupported/exact handling
- DSL helpers
- codegen review
- round-trip and unsupported tests

### 3. SQL Server MERGE

`MERGE` should not be added casually. It is a large semantic surface and should be introduced only through an explicit shared-model design.

#### Preconditions for inclusion
- a first-class `MergeStatement` model is designed in `sqm-core`
- model semantics are broad enough to justify cross-dialect reuse where applicable
- unsupported dialects reject it cleanly

#### Minimum SQL Server slice if accepted
- target table
- source relation
- `ON` predicate
- matched / not matched action branches
- basic `UPDATE`, `DELETE`, and `INSERT` actions within `MERGE`

#### Explicitly defer unless deliberately accepted
- highly specialized SQL Server `MERGE` options or output combinations that overly complicate the first model

This should be treated as the highest-risk feature in the epic and may need to be split into a separate story gate even within the follow-up.

### 4. SQL Server Hints

This epic should cover SQL Server hint forms that are important for real SQL Server usage and cannot be represented today.

Likely categories:

- table hints:
  - `WITH (NOLOCK)`
  - `WITH (UPDLOCK)`
  - `WITH (HOLDLOCK)`
  - related table-level hint lists
- query hints, where applicable

#### Modeling guidance
- avoid raw-string-only handling if hint structure matters semantically
- preserve enough structure for validation, deterministic rendering, and possible transpilation diagnostics
- keep unsupported or unsafe hint translations explicit in transpilation

#### Validation guidance
- detect conflicting or duplicate hint combinations when they can be recognized safely
- reject hint use in unsupported statement contexts

### 5. Advanced TOP Semantics

`R5` intentionally deferred `TOP ... PERCENT` and `TOP ... WITH TIES` because the existing `LimitOffset` model does not capture those semantics.

This follow-up should resolve that gap explicitly.

#### Design options
- extend existing pagination structures with optional advanced metadata
- introduce a new SQL Server-specific pagination node
- introduce a more general top/fetch abstraction that can safely coexist with `LimitOffset`

Recommended design principle:
prefer a model that preserves semantics rather than forcing lossy lowering into `LimitOffset`.

Required support:

- parsing `TOP (expr) PERCENT`
- parsing `TOP (expr) WITH TIES`
- rendering canonical SQL Server syntax
- validation of `WITH TIES` requiring meaningful `ORDER BY`
- transpilation diagnostics for dialects without safe equivalents

### 6. Validation

All advanced SQL Server features in this epic must include validation support.

Validation should cover:

- `OUTPUT` legality by statement family
- `MERGE` action-shape correctness
- required conditions for `WITH TIES`
- hint legality and conflicting hint combinations where practical
- unsupported advanced-feature combinations

If a SQL Server-specific validation pack is needed, it should be added explicitly rather than relying only on parser or renderer failures.

### 7. Transpilation

This epic should broaden SQL Server transpilation coverage from `R5`’s minimal awareness to explicit advanced-feature behavior.

Required outcomes:

- SQL Server advanced features are classified as exact, approximate, or unsupported
- `OUTPUT`, `MERGE`, hints, and advanced `TOP` behavior all have explicit rule outcomes
- diagnostics explain why a feature cannot be converted when no safe mapping exists
- transpilation rule coverage is expanded deliberately rather than left implicit in renderer or validator failures

Important principle:
many SQL Server advanced features will likely be unsupported for most targets. That is acceptable, but the unsupported outcome must be first-class and tested.

Expected follow-up rule areas include:

- SQL Server advanced `TOP` rewrites and unsupported outcomes:
  - `TOP ... PERCENT`
  - `TOP ... WITH TIES`
- SQL Server `OUTPUT` exact or unsupported source/target rules
- SQL Server hint exact, approximate, dropped-with-warning, or unsupported rules depending on target semantics
- `MERGE` unsupported rules for targets without a safe equivalent
- SQL Server function-family mappings where a deliberate conversion exists, for example:
  - `LEN` and length-family equivalents
  - `ISNULL` and `COALESCE`
  - selected date/time function families
- SQL Server boolean and predicate-shape rewrites where dialect rendering or semantics differ materially
- additional SQL Server source-side unsupported rules for advanced features that cannot be preserved safely

### 8. Catalog And Type Mapper Follow-Up

This epic should define the SQL Server-specific part of downstream catalog support that was intentionally left out of `R5`.

Current explicit gap carried from `R5`:

- `sqm-codegen-maven-plugin` currently accepts SQL Server dialect selection, but JDBC schema introspection still falls back to the generic `DefaultSqlTypeMapper` because there is no dedicated SQL Server catalog/type-mapper module yet.
- That fallback is acceptable only as an explicit bridge to this follow-up epic and must not be treated as completed SQL Server catalog support.

Expected areas:

- SQL Server JDBC metadata mapping review
- SQL Server type-name mapping pack
- codegen compatibility for SQL Server catalog-derived schemas
- conformance tests for validator/codegen interplay with SQL Server metadata

This work should stay coordinated with `R10`, but the SQL Server-specific design belongs here so it does not get forgotten.

### 9. DSL And Codegen

Any new model surface introduced in this epic must be expressible ergonomically through DSL helpers.

Required outcomes:

- add DSL helpers for new DML output, hint, merge, or advanced pagination nodes
- ensure tests use the DSL where that improves clarity
- update `sqm-codegen` so generated code can construct new node shapes through DSL methods rather than low-level internal types
- update `sqm-codegen-maven-plugin` if SQL Server catalog support changes code generation flows

### 10. Downstream Wiring

Advanced SQL Server support must propagate beyond parser/render layers.

Required downstream review:

- `sqm-control`
- `sqm-middleware-core`
- `sqm-middleware-rest`
- `sqm-middleware-mcp`
- `sqm-it`
- `sqm-middleware-it`
- downstream support matrix documentation

The expected standard is the same as baseline dialect work: no feature is considered done if it only exists in parser/render layers but not in the user-facing runtime pipeline where applicable.

---

## Risks And Design Constraints

### 1. MERGE Has High Semantic Cost

`MERGE` is likely the single riskiest feature in this follow-up. It can easily force broad core-model expansion and should be gated behind a deliberate model review.

### 2. OUTPUT May Want A Shared Abstraction

If SQL Server `OUTPUT` and PostgreSQL-style `RETURNING` can share semantics cleanly, this epic should prefer the shared abstraction. If they cannot, the design should document that tradeoff explicitly.

### 3. Hints Can Degrade Into Raw Strings

A raw-string implementation may feel cheap in the short term but undermines validation, rendering determinism, and future transpilation. This epic should resist that shortcut unless structure truly provides no value.

### 4. Catalog Work Can Expand Endlessly

SQL Server type and metadata mapping can become a large program by itself. This epic should focus on the SQL Server-specific downstream pieces required for practical support, while coordinating with `R10` for broader unification.

### 5. DDL Must Stay Out

SQL Server advanced support can easily drift into DDL requests. This epic should explicitly reject that expansion. DDL is not merely postponed here; it requires a separate framework-level decision before it should be designed at all.

---

## User Stories

### Story A1

#### Title
`Story: Design shared model support for SQL Server OUTPUT`

#### User Story
As a SQM maintainer, I want SQL Server `OUTPUT` modeled cleanly so that advanced DML support does not depend on parser/renderer hacks.

#### Acceptance Criteria
- A model design is chosen for SQL Server `OUTPUT`.
- The design explains whether a shared DML result-projection abstraction is used.
- New nodes, if introduced, have full node-contract coverage.
- `MODEL.md` and related docs are updated.

#### Labels
`story`, `sqlserver`, `output`, `sqm-core`

#### Depends On
`R5`

---

### Story A2

#### Title
`Story: Implement SQL Server OUTPUT parse/render/validate support`

#### User Story
As a SQM user, I want SQL Server `OUTPUT` supported for prioritized DML statements so that result-projection workflows are first-class in the dialect.

#### Acceptance Criteria
- `OUTPUT` is supported for the selected SQL Server DML statements.
- Unsupported combinations are rejected clearly.
- Parser, renderer, validator, transpiler diagnostics, DSL, and tests are updated.

#### Labels
`story`, `sqlserver`, `output`, `dml`

#### Depends On
A1

---

### Story A3

#### Title
`Story: Design and implement SQL Server advanced TOP semantics`

#### User Story
As a SQM user, I want `TOP ... PERCENT` and `TOP ... WITH TIES` modeled correctly so that SQL Server pagination semantics are preserved rather than approximated away.

#### Acceptance Criteria
- A model strategy is chosen for advanced `TOP` semantics.
- Parser/render/validate support is implemented.
- `WITH TIES` behavior is validated appropriately.
- Transpilation outcomes are explicit.

#### Labels
`story`, `sqlserver`, `pagination`

#### Depends On
`R5`

---

### Story A4

#### Title
`Story: Add SQL Server hint modeling and support`

#### User Story
As a SQM user, I want SQL Server table/query hints represented and validated explicitly so that hint-bearing SQL can be analyzed and rendered deterministically.

#### Acceptance Criteria
- Supported hint families are modeled explicitly.
- Parser/render/validate support exists in the selected statement contexts.
- Conflicting or duplicate hints are diagnosed where practical.
- DSL and tests are updated.

#### Labels
`story`, `sqlserver`, `hints`

#### Depends On
`R5`

---

### Story A5

#### Title
`Story: Introduce MERGE statement support for SQL Server`

#### User Story
As a SQM user, I want SQL Server `MERGE` modeled and supported so that merge-based write workflows are represented as first-class statements.

#### Acceptance Criteria
- A first-class merge statement model exists if the feature is accepted.
- Parser/render/validate coverage exists for the selected initial SQL Server `MERGE` slice.
- Unsupported dialects reject `MERGE` explicitly.
- DSL, codegen, and tests are updated.

#### Labels
`story`, `sqlserver`, `merge`, `dml`

#### Depends On
Model review gate

---

### Story A6

#### Title
`Story: Expand SQL Server transpilation coverage for advanced features`

#### User Story
As a SQM user, I want advanced SQL Server features to produce explicit transpilation outcomes so that unsupported conversions are visible and safe.

#### Acceptance Criteria
- Advanced SQL Server features have explicit exact/approximate/unsupported outcomes.
- Diagnostics explain unsupported conversions.
- Tests cover representative advanced SQL Server source and target cases.
- The story explicitly reviews and adds transpilation rules where appropriate instead of relying on downstream validation or rendering failures alone.

#### Labels
`story`, `sqlserver`, `transpile`

#### Depends On
A2, A3, A4, A5

---

### Story A7

#### Title
`Story: Add SQL Server catalog and type-mapper downstream support`

#### User Story
As a SQM maintainer, I want SQL Server metadata and type handling improved so that validator and codegen flows can operate against SQL Server-backed schemas more reliably.

#### Acceptance Criteria
- SQL Server type mapping design is documented and implemented where selected.
- Codegen and validator flows work for the accepted SQL Server metadata slice.
- Conformance tests cover schema/validator/codegen interplay.

#### Labels
`story`, `sqlserver`, `catalog`, `codegen`

#### Depends On
`R5`

---

### Story A8

#### Title
`Story: Close DSL, codegen, middleware, and integration gaps for advanced SQL Server support`

#### User Story
As a SQM maintainer, I want all advanced SQL Server features to be usable through DSL, codegen, and runtime integration layers so that they are fully productized rather than partially implemented.

#### Acceptance Criteria
- DSL helpers exist for new public model surface.
- `sqm-codegen` and `sqm-codegen-maven-plugin` are updated as needed.
- Middleware/control flows cover the advanced features where applicable.
- Integration tests cover delivered advanced SQL Server functionality end to end.

#### Labels
`story`, `sqlserver`, `dsl`, `codegen`, `middleware`

#### Depends On
A2, A3, A4, A5, A7

---

## Suggested Delivery Order

1. `A1` OUTPUT model design
2. `A3` advanced TOP design and implementation
3. `A4` hint modeling and support
4. `A2` OUTPUT implementation
5. `A5` MERGE model review and implementation
6. `A6` transpilation coverage
7. `A7` catalog/type-mapper support
8. `A8` DSL/codegen/middleware/integration closure

## Exit Notes

This epic should be considered complete only when the deferred SQL Server features from `R5` are either:

- implemented with full downstream support, or
- explicitly rejected with documented rationale and roadmap reassignment

DDL should not be absorbed into this epic under any circumstances. That boundary is intentional and should remain aligned with the repository roadmap and the current position that DDL support is not assumed for the framework.
