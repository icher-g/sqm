## Epic

### Title
`Epic: R4 SQL Transpilation Foundation`

### Problem Statement
SQM currently provides strong parse, validate, rewrite, and render capabilities per dialect, but it does not yet provide a first-class source-to-target SQL transpilation layer.

As a result:

- cross-dialect conversion work would leak into renderers or ad hoc rewrite flows
- exact versus approximate versus unsupported conversions are not reported explicitly
- reusable conversion logic is not organized behind a dedicated API
- roadmap expansion to additional dialects would happen before there is a structured conversion story between supported dialects

The current PostgreSQL and MySQL support already expose places where a semantic core abstraction is more appropriate than pairwise conversion rules. String concatenation is the first such case: PostgreSQL commonly uses `||`, while MySQL commonly uses `CONCAT(...)`, but both represent the same semantic concept and should map to one shared node.

### Epic Goal
Deliver `sqm-transpile` as a first-class module for source-to-target SQL conversion, with reusable rule infrastructure, explicit transpilation diagnostics, and an initial PostgreSQL/MySQL slice.

The epic should establish the architectural foundation for future dialect expansion while promoting clearly portable semantics into `sqm-core` where appropriate.

### Business Value
- Reduces the cost of adding new dialects by introducing a dedicated conversion layer before roadmap expansion.
- Keeps parser and renderer responsibilities clean by separating semantic conversion from syntax emission.
- Makes cross-dialect behavior auditable through explicit exact, approximate, and unsupported outcomes.
- Creates a clear upgrade path for shared semantic nodes such as `ConcatExpr`.
- Improves downstream usability for tools that need migration, normalization, or dialect conversion support.

### Definition of Done
- `sqm-transpile` module exists and is wired into the multi-module build.
- Public transpilation API and result model are implemented with explicit diagnostics.
- Reusable transpilation rule and registry contracts are implemented.
- `ConcatExpr` is introduced in `sqm-core` and used by PostgreSQL/MySQL parsing and rendering for plain string concatenation.
- Initial PostgreSQL/MySQL transpilation coverage exists for a small exact subset plus explicit unsupported cases.
- Tests cover model behavior, parser/render integration, transpile execution, and unsupported flows.
- Documentation is updated for architecture, rollout, and examples.

### Suggested Labels
`epic`, `transpile`, `sql`, `architecture`

---

## User Stories

### Story T1

#### Title
`Story: Add ConcatExpr semantic node to sqm-core`

#### User Story
As a SQM maintainer, I want plain string concatenation to be represented by a dedicated semantic node so that PostgreSQL `||` and MySQL `CONCAT(...)` can share one portable model representation instead of relying on transpilation rules for the normal path.

#### Acceptance Criteria
- A new `ConcatExpr` node is added to `sqm-core`.
- `ConcatExpr` has full node-contract support:
  visitor, transformer, matcher, JSON mixins, and documentation updates.
- PostgreSQL parsing maps plain string-concatenation `||` chains to `ConcatExpr`.
- MySQL parsing maps plain `CONCAT(...)` calls to `ConcatExpr` where the call is being used as ordinary string concatenation.
- PostgreSQL rendering emits `||` syntax from `ConcatExpr`.
- MySQL rendering emits `CONCAT(...)` syntax from `ConcatExpr`.
- Tests cover unchanged/changed transformer behavior and parse/render round-trips for PostgreSQL and MySQL.

#### Labels
`story`, `transpile`, `sqm-core`, `parser`, `renderer`

#### Depends On
None

---

### Story T2

#### Title
`Story: Create sqm-transpile module and public API`

#### User Story
As a library user, I want a first-class transpilation API so that I can request source-to-target SQL conversion without composing parser, rewrite, validation, and rendering steps manually.

#### Acceptance Criteria
- A new `sqm-transpile` module is added to the build.
- `SqlTranspiler`, `SqlDialectId`, `TranspileOptions`, `TranspileResult`, and core diagnostic types are implemented.
- The API supports transpiling both SQL text and already parsed statements.
- The result model captures success, warnings, unsupported outcomes, and final rendered SQL when available.
- Documentation includes a minimal usage example.

#### Labels
`story`, `transpile`, `api`

#### Depends On
T1

---

### Story T3

#### Title
`Story: Implement reusable transpilation rule and registry infrastructure`

#### User Story
As a SQM maintainer, I want transpilation rules to be reusable across multiple source/target mappings so that common rewrite behavior can be shared instead of duplicated for every dialect pair.

#### Acceptance Criteria
- `TranspileRule` supports reusable applicability across multiple source and target dialects.
- `TranspileRuleRegistry` can assemble a concrete execution plan for a requested source/target pair from reusable rules.
- Rule execution order is deterministic.
- Rule results support exact, approximate, and unsupported outcomes with diagnostics.
- Tests cover rule selection, ordering, unchanged behavior, and blocking unsupported behavior.

#### Labels
`story`, `transpile`, `architecture`

#### Depends On
T2

---

### Story T4

#### Title
`Story: Integrate target validation and rendering into transpilation pipeline`

#### User Story
As a library user, I want transpiled statements to be validated against the target dialect before rendering so that invalid or unsupported results are detected explicitly rather than surfacing late in rendering or execution.

#### Acceptance Criteria
- The transpilation pipeline executes parse, normalize, transpile, validate target, and render in the documented order.
- Target validation uses existing dialect-aware validation settings.
- Validation failures are returned as structured `TranspileResult` diagnostics.
- Rendering only runs when the transpiled AST is valid for the target path.
- Tests cover successful and validation-failed flows.

#### Labels
`story`, `transpile`, `validate`, `renderer`

#### Depends On
T2, T3

---

### Story T5

#### Title
`Story: Deliver initial PostgreSQL to MySQL transpilation slice`

#### User Story
As a SQM user, I want a small but reliable PostgreSQL-to-MySQL transpilation slice so that the new module proves real end-to-end value before the project expands to additional dialects.

#### Acceptance Criteria
- An initial exact subset is supported for PostgreSQL to MySQL transpilation.
- The initial exact subset explicitly includes:
  - shared semantic-node rendering such as `ConcatExpr`
  - null-safe comparison rules where PostgreSQL and MySQL syntax differ but semantics are portable enough to model exactly
  - regex predicate cases that already map cleanly through existing SQM semantic nodes
- The initial approximate subset explicitly includes:
  - PostgreSQL `ILIKE` rewrite support for MySQL with warning diagnostics
- The initial unsupported subset explicitly includes:
  - PostgreSQL `RETURNING`
  - PostgreSQL `DISTINCT ON`
  - PostgreSQL `SIMILAR TO`
  - representative PostgreSQL-specific operator families, including JSON/operator cases that do not have a safe MySQL equivalent
- String concatenation is handled through `ConcatExpr`, not through a pair-specific transpile rule.
- A small number of explicit unsupported cases are reported clearly, including representative PostgreSQL-only features without a safe MySQL equivalent.
- End-to-end tests cover parse, transpile, validate, render, and diagnostics for this slice.
- Documentation includes at least one PostgreSQL-to-MySQL example and one unsupported example.

#### Labels
`story`, `transpile`, `postgresql`, `mysql`

#### Depends On
T1, T2, T3, T4

---

### Story T6

#### Title
`Story: Deliver initial MySQL to PostgreSQL transpilation slice`

#### User Story
As a SQM user, I want a small but reliable MySQL-to-PostgreSQL transpilation slice so that reusable rules and semantic-node decisions are exercised in both directions from the start.

#### Acceptance Criteria
- An initial exact subset is supported for MySQL to PostgreSQL transpilation.
- The initial exact subset explicitly includes:
  - shared semantic-node rendering such as `ConcatExpr`
  - MySQL null-safe comparison syntax mapped into the canonical SQM semantic form and rendered as PostgreSQL syntax
  - regex predicate cases that already map cleanly through existing SQM semantic nodes
- The initial unsupported subset explicitly includes:
  - MySQL optimizer/index hints
  - MySQL `ON DUPLICATE KEY UPDATE` when no exact PostgreSQL rewrite is implemented
  - representative MySQL-specific JSON/function/operator cases that do not have a safe PostgreSQL equivalent
- Shared semantic constructs such as `ConcatExpr` render correctly in PostgreSQL output.
- Unsupported MySQL-only features in the selected slice are reported explicitly.
- End-to-end tests cover parse, transpile, validate, render, and diagnostics for this slice.

#### Labels
`story`, `transpile`, `mysql`, `postgresql`

#### Depends On
T1, T2, T3, T4

---

### Story T7

#### Title
`Story: Document transpilation architecture and contribution guidelines`

#### User Story
As a contributor, I want clear transpilation architecture and story-level guidance so that new rules and semantic nodes follow a consistent design and testing approach.

#### Acceptance Criteria
- Documentation explains when to add a core semantic node versus a transpilation rule.
- Documentation explains reusable rule applicability and target validation responsibilities.
- Documentation references `ConcatExpr` as the first-wave semantic-node example.
- Documentation includes a maintained matrix of exact, approximate, and unsupported PostgreSQL/MySQL and MySQL/PostgreSQL rules in the initial slice.
- Publishing instructions exist for creating epic and story issues from the markdown source.

#### Labels
`story`, `docs`, `transpile`

#### Depends On
T1, T2, T3, T4, T5, T6
