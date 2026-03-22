## Epic

### Title
`Epic: R4 SQL Transpilation Foundation (Completed)`

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

### Business Value
- makes SQL dialect migration and interoperability a first-class feature instead of an ad hoc downstream concern
- preserves the role of renderers as syntax emitters, rather than overloading them with conversion behavior
- creates a reusable conversion architecture before more dialects are added
- gives users explicit fidelity reporting instead of silent semantic drift

### Definition of Done
- `sqm-transpile` module exists and is wired into the build
- source and target dialect conversion is modeled as a first-class API
- rule registry supports exact, approximate, and unsupported outcomes
- initial PostgreSQL/MySQL slice is implemented end to end
- diagnostics and tests cover representative supported and unsupported cases
- docs describe the transpilation architecture, scope, and limitations

### Suggested Labels
`epic`, `transpile`, `core`, `postgresql`, `mysql`

---

## Scope Boundaries

### In Scope
- a dedicated transpilation API/module
- reusable transpilation rule contracts
- fidelity/result reporting
- a first delivered PostgreSQL <-> MySQL slice
- integration with existing parser/validator/renderer phases

### Out of Scope
- universal dialect parity from day one
- optimizer-like semantic rewrites unrelated to dialect conversion
- DDL transpilation
- speculative conversion support for dialects that SQM does not yet support

---

## Design Overview

### 1. High-Level Pipeline

Recommended pipeline:

1. parse source SQL with the source dialect parser
2. normalize to shared semantic structures where appropriate
3. apply ordered conversion rules for source -> target
4. validate against the target dialect
5. render using the target dialect renderer

### 2. Core Principle

Transpilation should operate on semantic nodes where possible, not on rendered SQL text.

### 3. Result Model

The transpilation result should explicitly classify outcomes as:

- exact
- approximate
- unsupported

### 4. Initial Slice

The first delivered slice should focus on a small PostgreSQL/MySQL subset with representative exact and unsupported cases rather than chasing broad early coverage.

---

## User Stories

### Story T1

#### Title
`Story: Introduce sqm-transpile module and core transpilation contracts`

#### User Story
As a SQM maintainer, I want a dedicated transpilation module and result model so dialect conversion becomes a first-class framework capability.

#### Acceptance Criteria
- `sqm-transpile` exists in the reactor
- core result and rule contracts exist
- explicit exact/approximate/unsupported outcomes are modeled
- docs explain the new module’s role

#### Labels
`story`, `transpile`, `core`

---

### Story T2

#### Title
`Story: Add rule registry and execution plan infrastructure`

#### User Story
As a SQM maintainer, I want reusable source/target rule assembly so conversion behavior is organized and testable.

#### Acceptance Criteria
- rule registry exists
- rule execution ordering is explicit
- representative unit tests cover rule assembly behavior

#### Labels
`story`, `transpile`, `infrastructure`

#### Depends On
T1

---

### Story T3

#### Title
`Story: Deliver initial PostgreSQL to MySQL and MySQL to PostgreSQL transpilation slice`

#### User Story
As a SQM user, I want an initial cross-dialect transpilation slice so supported conversions and unsupported boundaries are explicit for the first shipped pair.

#### Acceptance Criteria
- representative PostgreSQL/MySQL exact conversions are implemented
- representative unsupported cases are diagnosed clearly
- tests cover both directions

#### Labels
`story`, `transpile`, `postgresql`, `mysql`

#### Depends On
T1, T2

---

## Suggested Delivery Order

1. `T1` module and contracts
2. `T2` registry infrastructure
3. `T3` initial PostgreSQL/MySQL slice

---

## Exit Notes

This epic established the first-class transpilation foundation for SQM and is complete.
