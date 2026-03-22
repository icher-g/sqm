## Epic

### Title
`Epic: R5 SQL Server Dialect Support (Completed)`

### Problem Statement
`R4` adds transpilation as a first-class capability and sets the expectation that new dialect work must think beyond parse/render only. The next major practical gap is SQL Server.

SQL Server is a high-value target because it differs materially from PostgreSQL/MySQL/ANSI in several areas that matter to a manipulation framework:

- pagination syntax (`TOP`, `OFFSET/FETCH`)
- bracket-quoted identifiers
- built-in function surface differences
- variable/result-target idioms
- table and query constructs that affect both modeling and rendering choices

Without first-class SQL Server support:

- SQM remains less useful for real migration and interoperability workloads
- transpilation has no strong target beyond PostgreSQL/MySQL
- middleware, validation, and codegen support remain skewed toward the currently implemented dialects

This epic defines the baseline SQL Server delivery slice for SQM, with explicit boundaries so the work stays focused and does not accidentally expand into full T-SQL coverage or DDL.

### Epic Goal
Add baseline SQL Server dialect support across parser, renderer, validation-aware downstreams, DSL reachability, codegen, and integration tests.

The goal is not full SQL Server parity. The goal is a practical, coherent first SQL Server slice that:

- fits the shared semantic model
- renders canonical SQL Server syntax where supported
- rejects unsupported SQL Server-only constructs explicitly
- keeps the framework architecture clean for future SQL Server expansion

### Business Value
- Adds a major production-relevant dialect beyond ANSI/PostgreSQL/MySQL.
- Improves SQM's value for SQL migration, transpilation, and multi-dialect tooling.
- Forces the framework to validate that parser/render/model/downstream integration patterns generalize beyond the initial dialect set.
- Creates the foundation for later SQL Server-specific advanced features without mixing them into the baseline delivery.

### Relationship To Other Epics

#### Depends On
- `R4` SQL transpilation foundation for the new cross-dialect architecture and fidelity concepts.

#### Informs
- future SQL Server advanced feature work
- future transpilation rules involving SQL Server as source or target
- future catalog/introspection work for SQL Server

#### DDL Boundary
- This epic does not assume SQL Server DDL support.
- DDL remains out of scope unless the repository makes a separate explicit framework decision.

### Scope Boundaries

#### Query
- In scope for core SQL Server query syntax differences needed for a baseline useful dialect.

#### DML
- In scope for baseline SQL Server DML syntax that fits the current shared statement model without forcing advanced SQL Server-only constructs into scope.

#### DDL
- Explicitly out of scope.

### Non-Goals
- Full T-SQL language support.
- Stored procedures, batches, variable declarations unrelated to the delivered SQL model.
- SQL Server advanced features such as `MERGE`, `OUTPUT`, advanced hint families, or broader SQL Server-only DML extensions unless explicitly required by the baseline and accepted into scope.
- General DDL support.

### Definition of Done
- `sqm-parser-sqlserver` and `sqm-render-sqlserver` exist and are wired into the reactor.
- A baseline `SqlServerSpecs` capability set exists.
- Shared-model coverage is reviewed for every delivered SQL Server syntax difference.
- Parser/render support exists for the selected SQL Server baseline query and DML slice.
- Validation, transpilation awareness, DSL reachability, codegen impact, control/middleware integration, and tests are reviewed and updated where needed.
- Documentation clearly states both delivered support and deferred advanced SQL Server features.

### Suggested Labels
`epic`, `sqlserver`, `dialect`, `parser`, `renderer`, `transpile`

---

## Scope Summary

### In Scope
- SQL Server parser module
- SQL Server renderer module
- baseline dialect specs/capability gating
- bracket identifier quoting
- baseline pagination support
- selected SQL Server function surface needed for practical baseline support
- baseline SQL Server DML syntax within current shared statement scope
- downstream integration updates where SQL Server support must be recognized

### Explicitly Deferred
- `MERGE`
- `OUTPUT`
- advanced SQL Server hint families
- `TOP ... PERCENT`
- `TOP ... WITH TIES`
- broader SQL Server catalog/type-mapper completeness
- DDL

These deferred items should be handled by follow-up epic work rather than being silently absorbed into the baseline.

---

## User Stories

### Story S1

#### Title
`Story: Add SQL Server parser and renderer modules`

#### User Story
As a SQM maintainer, I want dedicated SQL Server parser and renderer modules so SQL Server syntax support is explicit and isolated from existing dialect modules.

#### Acceptance Criteria
- `sqm-parser-sqlserver` and `sqm-render-sqlserver` exist
- modules are registered in the build and dialect registries
- baseline smoke tests prove parser/render wiring works

#### Labels
`story`, `sqlserver`, `parser`, `renderer`

---

### Story S2

#### Title
`Story: Define baseline SqlServerSpecs and dialect capability behavior`

#### User Story
As a SQM maintainer, I want SQL Server capability gates defined explicitly so parse/render behavior stays deliberate rather than implicit.

#### Acceptance Criteria
- baseline `SqlServerSpecs` exists
- parser and renderer consult capability settings where required
- tests cover representative supported and unsupported capability behavior

#### Labels
`story`, `sqlserver`, `specs`

#### Depends On
S1

---

### Story S3

#### Title
`Story: Implement baseline SQL Server query and DML syntax support`

#### User Story
As a SQM user, I want baseline SQL Server query and DML syntax supported so common SQL Server statements can round-trip through SQM.

#### Acceptance Criteria
- prioritized baseline query syntax is parsed and rendered
- baseline SQL Server DML syntax in scope is parsed and rendered
- tests cover happy-path and unsupported-path cases

#### Labels
`story`, `sqlserver`, `query`, `dml`

#### Depends On
S1, S2

---

### Story S4

#### Title
`Story: Add downstream SQL Server integration awareness`

#### User Story
As a SQM maintainer, I want validation, transpilation, DSL, codegen, control, and middleware layers to recognize the SQL Server baseline so dialect support is productized rather than parser/render only.

#### Acceptance Criteria
- downstream modules are reviewed and updated where required
- tests cover representative SQL Server downstream flows
- docs describe the delivered baseline and explicit deferrals

#### Labels
`story`, `sqlserver`, `dsl`, `codegen`, `middleware`

#### Depends On
S3

---

## Suggested Delivery Order

1. `S1` parser and renderer modules
2. `S2` dialect specs
3. `S3` baseline syntax support
4. `S4` downstream integration awareness

---

## Exit Notes

This epic delivered the baseline SQL Server dialect foundation and is complete.
