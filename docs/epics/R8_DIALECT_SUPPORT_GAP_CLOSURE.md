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

- `AtTimeZoneExpr`
  - SQL Server
- `Lateral`
- `FunctionTable`
  - SQL Server

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

### 4. Validation Rule Hardening Principle

Parser and renderer capability gates are necessary, but they are not sufficient on their own for good pre-execution diagnostics.

Where a dialect already has feature-aware validation rules, those rules should be treated as the proactive user-facing boundary for unsupported, version-gated, or shape-constrained features.

That means follow-up work under this epic should not leave a feature enforced only by parse-time or render-time rejection if the same limitation can be reported earlier and more clearly through schema validation.

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

#### B1. Variable-backed result targets beyond SQL Server
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

### Bucket E: Validation Rule Coverage Hardening

#### E1. Dialect feature-validation rule audit
- Goal:
  review the dialect feature-validation rules for shipped dialects and add missing feature checks so unsupported or version-gated constructs are reported during validation before parser/render execution paths become the first user-visible failure point.
- Mandatory outputs:
  - inventory of existing dialect feature-validation rules and their current gaps
  - added validation checks for missing shipped features where validation can provide earlier diagnostics
  - explicit decision notes for cases that should remain parser-only or renderer-only
  - tests covering validation-time diagnostics for the added feature checks

---

## Risks

### 1. False-Positive Support Assumptions
Some current `Not implemented by SQM` entries may reflect a support assumption that needs validation. This epic must treat `MODEL.md` as a working roadmap input, not as an unquestionable dialect specification.

### 2. Forcing Dialects Into the Wrong Shared Node
Because the features are already represented in `sqm-core`, there will be pressure to implement them by reusing the existing node shape even when the dialect semantics do not truly match. Stories must revisit the model fit before coding.

### 3. Parser/Renderer-Only Delivery
These gaps are only truly closed if validation, transpilation, DSL, codegen, and tests are reviewed too. This epic must not regress into parser/render-only work.

### 4. Validation Coverage Drift
If dialects gain parse/render gating without parallel validation-rule coverage, users will keep discovering unsupported features too late in the pipeline. This epic should reduce that gap rather than expand it.

### 5. Documentation Drift
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
2. `Lateral` and `FunctionTable` stories
3. `VariableTableRef` follow-up stories
4. `TopSpec` stories only after re-validating the model fit
5. validation-rule hardening story across shipped dialects
6. array-family stories only after re-validating the target dialect semantics

---

## User Stories

### Story R8-1

#### Title
`Story: Add SQL Server AT TIME ZONE support`

#### User Story
As an SQM user targeting SQL Server, I want `AtTimeZoneExpr` to parse, render, validate, and participate in transpilation review so time-zone conversion logic already represented in the shared model is actually usable in the SQL Server dialect slice.

#### Acceptance Criteria
- SQL Server capability and version assumptions for `AT TIME ZONE` are confirmed before implementation.
- SQL Server parser support is added for the existing `AtTimeZoneExpr` node where syntax and semantics align.
- SQL Server renderer support is added and rejects unsupported combinations explicitly.
- Validation coverage is added for SQL Server-specific semantic expectations.
- Transpilation impact is reviewed and implemented or rejected explicitly.
- DSL, codegen, control, and middleware impact are reviewed and updated where needed.
- Unit tests cover valid SQL, invalid SQL, and dialect-boundary failures.
- `MODEL.md` and relevant docs/wiki pages are updated.

#### Labels
`story`, `dialect`, `sqlserver`, `parser`, `renderer`, `validation`, `transpile`, `expression`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

Implementation note:
- PostgreSQL and MySQL were reclassified to `Not supported by the dialect` for `TopSpec`.
- Their row-limiting features are already modeled through `LimitOffset` and related pagination styles, not through SQL Server-style `TOP (...)`, `PERCENT`, or `WITH TIES`.

---

### Story R8-2

#### Title
`Story: Add MySQL LATERAL support`

#### User Story
As an SQM user targeting MySQL, I want `Lateral` relations to be supported where the dialect and version allow them so correlated relation semantics already represented in the shared model become available in the MySQL slice.

#### Acceptance Criteria
- MySQL version support for `LATERAL` is confirmed before implementation.
- MySQL parser and renderer support is added for the existing `Lateral` node where the model fit is valid.
- Validation and transpilation impact are reviewed and updated explicitly.
- DSL, codegen, control, middleware, and integration impact are reviewed and updated where needed.
- Unit tests cover supported usage, invalid syntax, and unsupported-boundary cases.
- `MODEL.md`, docs, and wiki pages are updated.

Implementation note:
- Shipped as MySQL lateral derived-table support from MySQL `8.0.14` onward.
- MySQL support is intentionally narrower than the shared node: the dialect accepts `LATERAL` only for derived tables and requires an alias.

#### Labels
`story`, `dialect`, `mysql`, `parser`, `renderer`, `validation`, `transpile`, `from-clause`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

Implementation note:
- MySQL and SQL Server were reclassified to `Not supported by the dialect` for the current shared array-expression family.
- Their shipped array-like capabilities are JSON-oriented and do not map cleanly to `ArrayExpr`, `ArraySubscriptExpr`, or `ArraySliceExpr` as currently modeled.

---

### Story R8-3

#### Title
`Story: Add SQL Server lateral-equivalent relation support`

#### User Story
As an SQM user targeting SQL Server, I want the SQL Server relation semantics equivalent to `Lateral` to be implemented or explicitly reclassified so SQM either supports the shared `Lateral` abstraction correctly or documents why the current support-matrix entry should change.

#### Acceptance Criteria
- The exact SQL Server feature and version assumptions behind the current `Lateral` entry are confirmed before coding.
- If the existing `Lateral` node is the right fit, parser and renderer support are added end to end.
- If the shared model is not the right fit, `MODEL.md` and this epic are updated instead of forcing an incorrect implementation.
- Validation, transpilation, DSL, codegen, control, middleware, and integration impact are reviewed explicitly.
- Tests cover shipped support or reclassification boundaries clearly.
- Docs and wiki pages are updated to match the final decision.

#### Labels
`story`, `dialect`, `sqlserver`, `parser`, `renderer`, `validation`, `transpile`, `from-clause`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

Implementation note:
- Shipped using the existing shared `Lateral` node, with SQL Server syntax mapped through `CROSS APPLY` and `OUTER APPLY`.
- Validation and rendering intentionally accept only APPLY-compatible join shapes for lateral relations.

---

### Story R8-4

#### Title
`Story: Add SQL Server table-valued function support and resolve MySQL FunctionTable classification`

#### User Story
As an SQM user working with dialects that justify table-valued function support, I want SQL Server `FunctionTable` support to be available where the shared model fits, and I want the MySQL support-matrix entry corrected if the shipped MySQL surface does not match the current generic `FunctionTable` node.

#### Acceptance Criteria
- The supported MySQL and SQL Server table-valued function surfaces are confirmed before coding.
- Parser and renderer support are added for SQL Server if the shared `FunctionTable` model is a valid fit.
- If MySQL does not truly match the shared semantics, `MODEL.md` and this epic are updated instead of forcing support.
- Validation, transpilation, DSL, codegen, control, middleware, and integration impact are reviewed and updated where needed.
- Tests cover SQL Server happy paths plus invalid and unsupported-boundary cases for both dialect decisions.
- `MODEL.md`, docs, and wiki pages are updated.

#### Labels
`story`, `dialect`, `mysql`, `sqlserver`, `parser`, `renderer`, `validation`, `transpile`, `from-clause`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

Implementation note:
- SQL Server fits the shared `FunctionTable` node for table-valued function calls in `FROM`.
- MySQL was reclassified to `Not supported by SQM` for the current `FunctionTable` node because its shipped `JSON_TABLE()` support requires structure beyond the current generic `FunctionTable` model.

---

### Story R8-5

#### Title
`Story: Resolve VariableTableRef support beyond SQL Server`

#### User Story
As an SQM maintainer, I want the PostgreSQL and MySQL `VariableTableRef` support-matrix entries to be implemented or explicitly reclassified so the shared model no longer carries ambiguous dialect claims around variable-backed relation sinks.

#### Acceptance Criteria
- PostgreSQL and MySQL capability assumptions behind `VariableTableRef` are confirmed before coding.
- If a dialect truly supports relation-shaped variable sinks compatible with `VariableTableRef`, end-to-end support is added.
- If the current support assumption is incorrect, `MODEL.md` and this epic are updated instead of forcing unrelated variable syntax into the shared node.
- Validation, transpilation, DSL, codegen, control, middleware, and integration impact are reviewed explicitly.
- Tests and documentation clearly reflect the final supported or reclassified scope.

#### Labels
`story`, `dialect`, `postgresql`, `mysql`, `parser`, `renderer`, `validation`, `transpile`, `dml`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

Implementation note:
- PostgreSQL and MySQL were reclassified to `Not supported by the dialect` for `VariableTableRef`.
- Their shipped temp-table features remain plain `Table` semantics, and their variable features do not provide SQL Server-style relation-backed table variables such as `@audit`.

---

### Story R8-6

#### Title
`Story: Resolve TOP support-matrix entries for PostgreSQL and MySQL`

#### User Story
As an SQM maintainer, I want the PostgreSQL and MySQL `TopSpec` support-matrix entries to be implemented or explicitly reclassified so the roadmap no longer implies `TOP`-style support in dialects where the shared abstraction may not actually belong.

#### Acceptance Criteria
- PostgreSQL and MySQL capability assumptions behind the current `TopSpec` entries are confirmed before coding.
- If the shared `TopSpec` node is genuinely appropriate for either dialect, end-to-end support is added.
- If the current support assumption is incorrect, `MODEL.md` and this epic are updated instead of forcing `LIMIT`-style syntax into `TopSpec`.
- Transpilation, validation, DSL, codegen, control, middleware, and integration impact are reviewed explicitly.
- Tests and docs clearly reflect the final supported or reclassified scope.

#### Labels
`story`, `dialect`, `postgresql`, `mysql`, `parser`, `renderer`, `validation`, `transpile`, `pagination`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

---

### Story R8-7

#### Title
`Story: Resolve array-family support-matrix entries for MySQL and SQL Server`

#### User Story
As an SQM maintainer, I want the MySQL and SQL Server array-family support-matrix entries to be implemented or explicitly reclassified so `ArrayExpr`, `ArraySubscriptExpr`, and `ArraySliceExpr` are only promised where the dialect semantics truly justify the shared nodes.

#### Acceptance Criteria
- MySQL and SQL Server capability assumptions for the array-family entries are confirmed before coding.
- If the existing shared array nodes are the right fit, parser, renderer, validation, and transpilation support are added end to end.
- If the support assumptions are incorrect or only approximate, `MODEL.md` and this epic are updated instead of forcing an invalid implementation.
- DSL, codegen, control, middleware, and integration impact are reviewed explicitly.
- Tests and docs clearly reflect the final supported or reclassified scope.

#### Labels
`story`, `dialect`, `mysql`, `sqlserver`, `parser`, `renderer`, `validation`, `transpile`, `expression`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

---

### Story R8-8

#### Title
`Story: Audit and complete dialect feature-validation rules`

#### User Story
As an SQM user, I want dialect feature-validation rules to report unsupported, version-gated, and shape-constrained features before parser or renderer execution becomes the first failure point, so validation provides earlier and clearer diagnostics across shipped dialects.

#### Acceptance Criteria
- The existing dialect feature-validation rules are reviewed for all shipped dialect slices that already rely on parser/render feature gating.
- Missing validation checks are added where a dialect feature can be diagnosed earlier during validation.
- Unsupported, version-gated, and dialect-shape-constrained features are prioritized for validation coverage.
- Any feature intentionally left parser-only or renderer-only is documented with a brief reason in the story design or follow-up notes.
- Tests cover validation-time diagnostics for the newly added checks.
- Epic/docs are updated so validation hardening is treated as part of dialect completeness, not optional cleanup.

#### Labels
`story`, `dialect`, `validation`, `parser`, `renderer`, `transpile`, `quality`

#### Depends On
- `Epic: R8 Dialect Support Gap Closure`

---

## Publishing GitHub Issues

The epic and stories can be published to GitHub issues from this markdown source.

Preview:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r8-dialect-gap-issues.ps1 -WhatIf
```

Publish:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r8-dialect-gap-issues.ps1
```

The wrapper delegates to the generic publisher in `scripts/create-github-issues-from-epic-md.ps1`.

This order prioritizes the stories that are most likely to map cleanly onto the current model before the more questionable support-matrix entries.
