## Epic

### Title
`Epic: R6 Dialect Live DB Execution Coverage (Completed)`

### Problem Statement
SQM currently has strong parser, renderer, validation, transpilation, DSL, codegen, and round-trip coverage, but most dialect tests still stop before executing the rendered SQL against a real database engine.

That leaves an important remaining risk:

- SQL can parse and render correctly in SQM while still being rejected by the actual database
- dialect-specific syntax edge cases may be modeled correctly in AST tests but never proven executable
- downstream and runtime pipelines can look complete while lacking real-engine syntax confirmation
- coverage for supported dialect features is spread across many tests without a single way to prove that every shipped feature participates in at least one executable real-DB case

The repository already has some Docker/Testcontainers-based PostgreSQL execution tests, but they currently live alongside non-Docker integration tests in `sqm-it`.
That makes the boundary between fast integration coverage and heavy live-DB execution coverage less clear than it should be.

### Epic Goal
Introduce a dedicated live-database integration test track that:

- runs rendered SQL against real database engines in Docker
- is separated from fast integration coverage
- provides per-dialect executable syntax coverage
- explicitly tracks feature coverage so every supported dialect feature participates in at least one live execution case

### Business Value
- Improves trust that SQM-rendered SQL is not only syntactically modeled, but executable on real engines.
- Reduces production risk for dialect-specific syntax regressions.
- Creates a durable, trackable coverage contract for each shipped dialect.
- Keeps the normal CI pipeline fast while allowing a heavier, higher-confidence live-engine pipeline.

### Definition of Done
- A dedicated live-DB integration module exists and is wired into the Maven reactor.
- Existing Docker-backed PostgreSQL execution tests are migrated out of `sqm-it`.
- Each shipped dialect with live-engine support has:
  - a container-backed execution harness
  - a feature manifest
  - executable syntax cases
  - a meta-test proving feature coverage completeness
- A dedicated CI/profile path exists for running the live-DB suites separately from normal CI.
- Documentation explains how to run the live-DB suites locally and in CI.

### Suggested Labels
`epic`, `integration`, `docker`, `testcontainers`, `dialect`, `quality`

---

## User Stories

### Story A1

#### Title
`Story: Introduce dedicated live DB integration module`

#### User Story
As a SQM maintainer, I want a dedicated module for real-database execution tests so Docker-backed suites are clearly separated from fast integration tests.

#### Acceptance Criteria
- `sqm-db-it` exists and is wired into the reactor
- Failsafe/Testcontainers/JDBC dependencies are configured
- live DB tests can run via dedicated Maven profile

#### Labels
`story`, `integration`, `docker`, `module`

---

### Story A2

#### Title
`Story: Migrate existing PostgreSQL Docker-backed execution tests out of sqm-it`

#### User Story
As a SQM maintainer, I want existing PostgreSQL live DB tests moved into the new module so `sqm-it` remains focused on fast non-Docker integration coverage.

#### Acceptance Criteria
- existing PostgreSQL live DB tests are moved to `sqm-db-it`
- `sqm-it` no longer contains Docker-backed real-engine tests
- migrated tests remain green under the Docker profile

#### Labels
`story`, `integration`, `postgresql`, `docker`

#### Depends On
A1

---

### Story A3

#### Title
`Story: Add shared live execution harness and feature manifest framework`

#### User Story
As a SQM maintainer, I want reusable live-DB test infrastructure so dialect suites follow one predictable pattern and feature coverage can be enforced.

#### Acceptance Criteria
- shared execution harness exists
- reusable case manifest structure exists
- completeness meta-test pattern exists

#### Labels
`story`, `integration`, `infrastructure`, `testcontainers`

#### Depends On
A1

---

### Story A4

#### Title
`Story: Add PostgreSQL live execution coverage manifest`

#### User Story
As a SQM maintainer, I want PostgreSQL shipped features mapped to executable real-DB cases so rendered PostgreSQL SQL is proven against a live engine.

#### Acceptance Criteria
- PostgreSQL feature enum exists
- executable PostgreSQL cases exist
- completeness meta-test passes

#### Labels
`story`, `integration`, `postgresql`, `coverage`

#### Depends On
A2, A3

---

### Story A5

#### Title
`Story: Add MySQL live execution coverage manifest`

#### User Story
As a SQM maintainer, I want MySQL shipped features mapped to executable real-DB cases so rendered MySQL SQL is proven against a live engine.

#### Acceptance Criteria
- MySQL feature enum exists
- executable MySQL cases exist
- completeness meta-test passes

#### Labels
`story`, `integration`, `mysql`, `coverage`

#### Depends On
A3

---

### Story A6

#### Title
`Story: Add SQL Server live execution coverage manifest`

#### User Story
As a SQM maintainer, I want SQL Server shipped features mapped to executable real-DB cases so rendered SQL Server SQL is proven against a live engine.

#### Acceptance Criteria
- SQL Server feature enum exists
- executable SQL Server cases exist
- completeness meta-test passes

#### Labels
`story`, `integration`, `sqlserver`, `coverage`

#### Depends On
A3

---

### Story A7

#### Title
`Story: Add dedicated CI pipeline for live DB execution suites`

#### User Story
As a SQM maintainer, I want a separate CI pipeline for live DB suites so heavy Docker-backed execution coverage does not slow or destabilize the default CI path.

#### Acceptance Criteria
- live DB suites run in a dedicated pipeline
- local and CI docs are updated
- dialect jobs or matrix jobs are documented

#### Labels
`story`, `integration`, `ci`, `docker`

#### Depends On
A4, A5, A6

---

## Suggested Delivery Order

1. `A1` create `sqm-db-it`
2. `A2` migrate PostgreSQL live tests
3. `A3` shared harness + manifest infrastructure
4. `A4` PostgreSQL manifest
5. `A5` MySQL manifest
6. `A6` SQL Server manifest
7. `A7` CI pipeline split

---

## Exit Notes

This epic delivered the live DB execution coverage track and is complete.
