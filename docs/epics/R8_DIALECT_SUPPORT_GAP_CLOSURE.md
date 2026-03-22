## Epic

### Title
`Epic: R8 Dialect Support Gap Closure`

### Problem Statement
`docs/model/MODEL.md` now distinguishes between:

- features that are not supported by a dialect
- features that are supported by the dialect but not yet implemented by SQM

The second category represents the most actionable framework follow-up work because:

- the shared model already has a representational foothold for the feature
- the dialect capability is expected to exist
- the current gap is in SQM delivery rather than framework scope

If these gaps are left scattered across the model notes, they risk being implemented inconsistently or not at all. That would weaken one of SQM's core goals: being a reliable framework for SQL query manipulation across dialects, with explicit and complete support boundaries.

This epic creates a single roadmap container for all constructs currently marked `Not implemented by SQM` in `MODEL.md`.

### Epic Goal
Close the currently documented `Not implemented by SQM` dialect gaps in a deliberate, end-to-end way across model, parser, renderer, validation, transpilation, DSL, codegen, control, middleware, and tests.

The epic goal is not to add speculative new features. It is to finish the feature support that SQM already documents as dialect-supported but not yet implemented.

### Business Value
- Turns documented implementation gaps into a concrete delivery plan instead of leaving them as passive notes.
- Improves confidence in the support matrix by making `Not implemented by SQM` a temporary backlog status rather than a long-lived state.
- Expands the usable manipulation surface of SQM in dialects that already justify support for the feature.
- Reduces drift between core representability and actual shipped dialect coverage.

### Relationship To Other Epics

#### Depends On
- `R5` and `R5B` for the current SQL Server baseline and advanced feature direction.
- `R7` for typed-hint modeling where a feature family depends on richer hint semantics in the future.

#### Informs
- future dialect roadmap work
- future transpilation expansion stories
- future support-matrix and documentation cleanup

#### DDL Boundary
- This epic does not pull DDL into scope.
- Any DDL-related feature remains outside this epic unless the repository instructions are explicitly changed.

### Scope Boundaries

#### Query
- In scope for query-level constructs currently marked `Not implemented by SQM` in the support matrix.

#### DML
- In scope for DML result and relation constructs currently marked `Not implemented by SQM` in the support matrix.

#### DDL
- Out of scope.

### Non-Goals
- Reclassifying already-supported features as new implementation work.
- Implementing features currently marked `Not supported by the dialect`.
- Adding speculative dialect features that are not yet represented in `MODEL.md`.
- Stored procedures, procedural SQL, batches, variable declarations unrelated to the documented support gaps, and DDL.

### Definition of Done
- Every feature currently marked `Not implemented by SQM` in `MODEL.md` is either:
  - implemented end to end, or
  - reclassified in `MODEL.md` after explicit design review if the dialect-support assumption proves incorrect
- For each implemented feature:
  - parser support is delivered where the dialect should parse it
  - renderer support is delivered where the dialect should render it
  - validation support is delivered where semantic checks are required
  - transpilation impact is reviewed and implemented or explicitly rejected
  - DSL helpers exist for all new public model shapes
  - codegen and plugin impact are reviewed and updated where needed
  - control, middleware, and integration impact are reviewed where relevant
  - unit and integration tests cover happy paths, failure paths, and dialect boundaries
- Documentation is updated in `MODEL.md`, relevant wiki pages, and any affected epic/design docs.

### Suggested Labels
`epic`, `dialect`, `support-matrix`, `parser`, `renderer`, `validation`, `transpile`

---

## Scope Summary

### Source of Truth
This epic is driven by the entries currently marked `Not implemented by SQM` in [MODEL.md](../model/MODEL.md).

Those entries currently include:

- `ArrayExpr` / `ArraySubscriptExpr` / `ArraySliceExpr`
  - MySQL
  - SQL Server
- `AtTimeZoneExpr`
  - SQL Server
- `ResultClause`
  - MySQL
- `TopSpec`
  - PostgreSQL
  - MySQL
- `Lateral`
  - MySQL
  - SQL Server
- `FunctionTable`
  - MySQL
  - SQL Server
- `VariableTableRef`
  - PostgreSQL
  - MySQL

If `MODEL.md` changes, this epic should be updated accordingly.

### Explicit Delivery Rule
Each story under this epic must confirm the exact dialect and version support before coding.

If a story discovers that the current `Not implemented by SQM` status is wrong and the feature is actually not part of the intended dialect surface, the story must:

- stop implementation
- update `MODEL.md`
- update this epic
- record the reason for the reclassification

This prevents the epic from turning ambiguous support assumptions into incorrect implementation commitments.

---

## Design Overview

### 1. Epic Strategy

This epic should be executed as a collection of focused dialect-feature stories, not as one large patch.

Each story should follow the same sequence:

1. confirm dialect/version capability
2. confirm or adjust core-model fit
3. implement parser and renderer support
4. add validation
5. review transpilation impact
6. add DSL/codegen reachability
7. add tests and update docs

### 2. Modeling Rule

The epic should follow the repo modeling rules already documented in:

- [SQM_MODELING_RULES.md](../reports/SQM_MODELING_RULES.md)

In particular:

- prefer shared semantic nodes over syntax-only hacks when the distinction matters for manipulation
- keep dialect syntax localized to dialect modules where possible
- keep support boundaries explicit in every layer
- avoid stringly modeling for features that matter to transformation or transpilation

### 3. Completion Rule Per Story

No story is complete if it stops at parser or renderer only.

Mandatory review areas for every story:

- `sqm-core`
- `sqm-parser-*`
- `sqm-render-*`
- `sqm-validate*`
- `sqm-transpile`
- `sqm-control`
- middleware and integration tests
- `sqm-codegen` and `sqm-codegen-maven-plugin`
- docs and wiki source

---

## Candidate Story Buckets

### Bucket A: Shared Expression Families With Missing Dialect Coverage

#### A1. Arrays in MySQL
- Goal:
  confirm the supported MySQL array-related surface, then implement the subset that maps cleanly to existing `ArrayExpr`, `ArraySubscriptExpr`, and `ArraySliceExpr` semantics.
- Mandatory outputs:
  - parser and renderer coverage
  - validation stance
  - transpilation review
  - examples and tests

#### A2. Arrays in SQL Server
- Goal:
  confirm the supported SQL Server feature family that justifies the current `Not implemented by SQM` status, then implement it if the existing array nodes are the right fit.
- Design warning:
  if SQL Server support is only approximate or not actually array-shaped, update `MODEL.md` instead of forcing it through the array nodes.

#### A3. `AT TIME ZONE` in SQL Server
- Goal:
  add SQL Server parser/render/validate support for the existing `AtTimeZoneExpr` node if the SQL Server syntax and semantics align closely enough with the current shared node.
- Review focus:
  - expression semantics
  - data-type behavior and validation expectations
  - transpilation exactness versus approximation

### Bucket B: Shared DML Result Gaps

#### B1. MySQL `ResultClause`
- Goal:
  implement the dialect-supported MySQL result-clause surface that maps to the shared `ResultClause` model.
- Review focus:
  - exact MySQL version scope
  - parse/render behavior
  - validation and transpilation stance
  - interaction with existing DML coverage

#### B2. Variable-backed result targets beyond SQL Server
- Goal:
  implement dialect support for `VariableTableRef` in dialects currently marked `Not implemented by SQM`, if the dialect really supports a relation-like variable sink that fits the shared semantics.
- Design warning:
  do not force unrelated variable constructs into `VariableTableRef` if the dialect feature is not truly relation-shaped.

### Bucket C: Query Table-Reference Gaps

#### C1. `Lateral` in MySQL
- Goal:
  add shipped support for `Lateral` if the intended MySQL version surface supports the required semantics.

#### C2. `Lateral` in SQL Server
- Goal:
  map the SQL Server equivalent relation semantics to the existing `Lateral` abstraction if that is the right modeling fit.
- Design warning:
  if SQL Server requires a different semantic node or a narrower mapping, resolve the model question first.

#### C3. `FunctionTable` in MySQL
- Goal:
  add table-valued function support where the MySQL dialect surface justifies the existing model status.

#### C4. `FunctionTable` in SQL Server
- Goal:
  add SQL Server support for table-valued function relations using the shared `FunctionTable` abstraction where appropriate.

### Bucket D: Pagination Gaps

#### D1. `TopSpec` in PostgreSQL
- Goal:
  confirm the PostgreSQL feature surface behind the current `Not implemented by SQM` classification and implement it if the current shared `TopSpec` abstraction is the right fit.
- Design warning:
  if PostgreSQL does not truly support the modeled `TOP` semantics, reclassify the matrix entry instead of adapting PostgreSQL to a SQL Server-shaped node.

#### D2. `TopSpec` in MySQL
- Goal:
  confirm the MySQL feature surface behind the current `Not implemented by SQM` classification and implement it only if the shared `TopSpec` node is genuinely appropriate.
- Design warning:
  avoid equating `LIMIT` support with `TOP` support unless the model is intentionally broadened.

---

## Risks

### 1. False-Positive Support Assumptions
Some current `Not implemented by SQM` entries may reflect a support assumption that needs validation. This epic must treat `MODEL.md` as a working roadmap input, not as an unquestionable dialect specification.

### 2. Forcing Dialects Into the Wrong Shared Node
Because the features are already represented in `sqm-core`, there will be pressure to implement them by reusing the existing node shape even when the dialect semantics do not truly match. Stories must revisit the model fit before coding.

### 3. Parser/Renderer-Only Delivery
These gaps are only truly closed if validation, transpilation, DSL, codegen, and tests are reviewed too. This epic must not regress into parser/render-only work.

### 4. Documentation Drift
If the implementation or reclassification happens without updating `MODEL.md`, the support matrix will lose credibility again.

---

## Acceptance Checklist Per Story

- exact dialect/version capability verified
- model fit reviewed
- parser added or updated
- renderer added or updated
- validation added or updated
- transpilation reviewed and updated
- DSL support added or confirmed
- codegen/plugin impact reviewed
- control/middleware impact reviewed
- tests added
- `MODEL.md` updated
- relevant wiki page updated

---

## Recommended Execution Order

1. `AtTimeZoneExpr` for SQL Server
2. MySQL `ResultClause`
3. `Lateral` and `FunctionTable` stories
4. `VariableTableRef` follow-up stories
5. `TopSpec` stories only after re-validating the model fit
6. array-family stories only after re-validating the target dialect semantics

This order prioritizes the stories that are most likely to map cleanly onto the current model before the more questionable support-matrix entries.
