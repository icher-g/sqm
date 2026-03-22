## Epic

### Title
`Epic: R5B SQL Server Advanced Support (Completed)`

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

### Delivered Areas
- SQL Server `OUTPUT` support for prioritized DML statements
- SQL Server `MERGE` support through a deliberate model path
- SQL Server table/query hint support in selected statement contexts
- SQL Server `TOP ... PERCENT`
- SQL Server `TOP ... WITH TIES`
- broader SQL Server transpilation coverage for the advanced feature slice
- SQL Server catalog/type-mapper follow-up needed for downstream support
- DSL, codegen, middleware, validation, and integration closure for the delivered advanced features

### Boundary
- DDL remained out of scope throughout this epic.

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

---

## Exit Notes

This epic completed the advanced SQL Server follow-up from `R5` and is complete.
