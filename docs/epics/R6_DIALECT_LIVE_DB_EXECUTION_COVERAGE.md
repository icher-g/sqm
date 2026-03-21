## Epic

### Title
`Epic: R6 Dialect Live DB Execution Coverage`

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

### Relationship To Other Epics

#### Depends On
- Existing shipped dialect support epics (`R1`, `R2`, `R3`, `R5`, `R5B`)

#### Coordinates With
- `R4` SQL transpilation foundation
- `R10` catalog/introspection follow-up work

#### DDL Boundary
- This epic does not introduce general DDL support.
- Setup DDL inside test harnesses is allowed only as test infrastructure, not as framework product scope.

### Scope Boundaries

#### In Scope
- New dedicated live-DB integration test module
- Migration of existing Docker-backed PostgreSQL execution tests into the new module
- New per-dialect live execution suites for PostgreSQL, MySQL, and SQL Server
- Feature-coverage manifests proving every shipped dialect feature participates in at least one live execution case
- Separate Maven profile and CI pipeline for live-DB execution tests
- Supporting test harnesses, schema setup utilities, and coverage documentation

#### Out Of Scope
- General DDL modeling in SQM
- Stored procedures and procedural SQL support
- Exhaustive semantic verification of every SQL feature beyond practical syntax/execution coverage
- Performance benchmarking as a primary goal

### Non-Goals
- Replacing existing parser/render/round-trip tests
- Moving all integration tests out of `sqm-it`
- Building a full SQL conformance suite for every database feature
- Supporting dialect features that are not already part of the SQM framework surface

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

## Why A Separate Module

The repository should split fast integration tests from live-engine execution tests.

Recommended structure:

- `sqm-it`
  - parser/render/round-trip integration tests
  - JSON/control/transpile integration tests that do not require Docker
  - fast and deterministic integration coverage
- `sqm-db-it`
  - all tests that require a real database engine
  - Testcontainers-backed execution suites
  - dialect-specific syntax execution coverage

This split improves:

- CI clarity
- local developer ergonomics
- dependency hygiene
- Maven profile isolation
- execution time predictability

---

## Module Design

### New Module

Add a new reactor module:

- `sqm-db-it`

Suggested role:

- dedicated live-engine integration tests for shipped dialects

Suggested dependencies:

- SQM parser/render/core/control/transpile modules needed by the test suites
- JUnit Jupiter
- Testcontainers core + JUnit integration
- dialect-specific Testcontainers modules where available
- dialect JDBC drivers

Suggested plugin setup:

- use Maven Failsafe for `*IT.java`
- keep normal `test` phase light
- run the module in `verify` under a dedicated Docker profile

Suggested Maven invocation:

```bash
mvn -pl sqm-db-it -am verify -Pdocker-it
```

### Existing Test Migration

Move existing real-DB PostgreSQL tests out of `sqm-it`, especially:

- PostgreSQL middleware execution tests
- PostgreSQL DSL execution tests
- any future Docker-backed dialect execution suites

Keep in `sqm-it`:

- round-trip parser/render tests
- AST-level integration tests
- non-Docker downstream integration tests

---

## Test Architecture

### 1. Dialect Execution Harness

Each dialect should have a reusable harness that provides:

- container lifecycle
- JDBC connection opening
- schema setup/reset
- rendered SQL execution helpers
- query result helpers
- row-count and mutation assertions

Suggested shape:

```java
abstract class DialectExecutionHarness {
    abstract Connection openConnection() throws Exception;
    abstract String render(Statement statement);
    abstract void resetSchema() throws Exception;

    void execute(String sql) throws Exception { ... }
    List<List<Object>> query(String sql) throws Exception { ... }
}
```

Then dialect-specific subclasses:

- `PostgresExecutionHarness`
- `MySqlExecutionHarness`
- `SqlServerExecutionHarness`

### 2. Feature Coverage Manifest

Each dialect should define an explicit manifest of shipped features that must participate in at least one live execution case.

Example:

```java
enum SqlServerLiveFeature {
    BRACKET_IDENTIFIERS,
    TOP,
    TOP_PERCENT,
    TOP_WITH_TIES,
    OFFSET_FETCH,
    TABLE_LOCK_HINT_NOLOCK,
    TABLE_LOCK_HINT_UPDLOCK,
    TABLE_LOCK_HINT_HOLDLOCK,
    LEN,
    DATEADD,
    DATEDIFF,
    ISNULL,
    STRING_AGG,
    INSERT,
    UPDATE,
    DELETE,
    OUTPUT,
    OUTPUT_INTO,
    MERGE_MATCHED_UPDATE,
    MERGE_MATCHED_DELETE,
    MERGE_NOT_MATCHED_INSERT,
    MERGE_NOT_MATCHED_BY_SOURCE_UPDATE,
    MERGE_NOT_MATCHED_BY_SOURCE_DELETE,
    MERGE_CLAUSE_PREDICATE,
    MERGE_TOP,
    MERGE_TOP_PERCENT
}
```

Equivalent enums should exist for PostgreSQL and MySQL.

### 3. Executable Case Registry

Each dialect should register cases in one place rather than spreading them across unrelated test methods.

Suggested shape:

```java
record DialectExecutionCase<F extends Enum<F>>(
    String id,
    Set<F> features,
    SchemaSetup setup,
    Statement statement,
    ExecutionAssertion assertion
) {}
```

The benefits are:

- one canonical list of live execution cases
- direct mapping from shipped features to executable tests
- easy meta-test enforcement
- easier later expansion for new dialect features

### 4. Meta-Test For Completeness

Each dialect should include a meta-test like:

```java
@Test
void all_sqlserver_live_features_have_execution_coverage() {
    assertEquals(
        EnumSet.allOf(SqlServerLiveFeature.class),
        SqlServerExecutionCases.coveredFeatures()
    );
}
```

This is the key guardrail that turns “we think everything is covered” into an enforceable contract.

---

## What A Live Execution Case Should Prove

The default case should prove:

- SQM model renders to dialect SQL
- the real engine accepts the SQL
- the SQL executes successfully

For many features, that is enough.

For higher-risk features, the case should also prove observable semantics, for example:

- row count
- returned rows
- `OUTPUT`/`RETURNING` result shape
- `MERGE` mutation result
- `TOP ... WITH TIES` tie behavior

Recommended split:

- syntax/execution confirmation by default
- deeper semantic assertions only where the syntax is subtle or historically risky

---

## Dialect Suite Design

### PostgreSQL

Suggested initial coverage:

- `RETURNING`
- `UPDATE ... FROM`
- `DELETE ... USING`
- `ON CONFLICT`
- `MERGE`
- writable CTE result flows
- `DISTINCT ON`
- arrays / `AT TIME ZONE` / selected PostgreSQL-only syntax if already shipped

Suggested migration:

- move existing Docker-backed PostgreSQL execution tests from `sqm-it`
- refactor them onto the new harness/manifest pattern

### MySQL

Suggested initial coverage:

- `INSERT IGNORE`
- `ON DUPLICATE KEY UPDATE`
- `REPLACE`
- joined `UPDATE`
- joined `DELETE`
- optimizer hints in shipped contexts
- `STRAIGHT_JOIN`
- selected shipped MySQL built-in functions and interval syntax

### SQL Server

Suggested initial coverage:

- bracket-quoted identifiers
- `TOP`
- `TOP ... PERCENT`
- `TOP ... WITH TIES`
- `OFFSET/FETCH`
- `WITH (NOLOCK)`, `WITH (UPDLOCK)`, `WITH (HOLDLOCK)`
- selected shipped SQL Server functions
- `INSERT`, `UPDATE`, `DELETE`
- `OUTPUT`
- `OUTPUT ... INTO`
- `MERGE`
- `MERGE TOP (...)`
- `MERGE TOP (...) PERCENT`
- `WHEN MATCHED`
- `WHEN NOT MATCHED`
- `WHEN NOT MATCHED BY SOURCE`
- clause predicates

---

## Schema Strategy

Live execution suites should not depend on production-like schemas.
Use small deterministic schemas designed for syntax coverage.

Recommended approach:

- per dialect, define a compact baseline schema fixture
- reset schema before each test or each case group
- include only the tables needed for shipped feature coverage

Example tables:

- `users`
- `orders`
- `audit`
- `incoming_users`

Use test-only setup DDL directly through JDBC in the harness.
This does not imply DDL support in SQM itself.

---

## Assertions Strategy

Recommended assertion layers:

1. Render
- render the statement with the dialect renderer

2. Execute
- execute the rendered SQL through JDBC

3. Observe
- assert one of:
  - query returns expected rows
  - mutation count matches expectation
  - output rows match expectation
  - post-state in tables matches expectation

Avoid over-asserting exact planner or execution details.
The suite should validate executable dialect syntax and practical correctness, not optimizer internals.

---

## CI And Pipeline Design

This work should run in a dedicated pipeline, separate from normal unit/integration CI.

Recommended pipeline split:

- default CI
  - unit tests
  - parser/render/validate/transpile tests
  - fast integration tests in `sqm-it`
- live DB CI
  - `sqm-db-it`
  - one job per dialect or a dialect matrix job

Recommended dialect jobs:

- PostgreSQL live execution
- MySQL live execution
- SQL Server live execution

Recommended Maven profile:

- `docker-it`

Optional profile split if needed later:

- `docker-it-postgresql`
- `docker-it-mysql`
- `docker-it-sqlserver`

---

## Migration Plan

### Phase 1: Module Split

- create `sqm-db-it`
- wire it into the parent reactor
- add Failsafe/Testcontainers/JDBC dependencies
- keep `sqm-it` unchanged functionally at first

### Phase 2: Move Existing PostgreSQL Live Tests

- move existing Docker-backed PostgreSQL execution tests from `sqm-it`
- update docs and CI commands
- ensure `sqm-it` remains green without Docker-only tests

### Phase 3: Build Shared Harness

- add container harness base classes
- add JDBC execution helpers
- add schema reset helpers
- add case manifest and meta-test infrastructure

### Phase 4: PostgreSQL Coverage Manifest

- define PostgreSQL live feature enum
- register executable cases
- add completeness meta-test

### Phase 5: MySQL Coverage Manifest

- add MySQL container support
- define MySQL live feature enum
- register executable cases
- add completeness meta-test

### Phase 6: SQL Server Coverage Manifest

- add SQL Server container support
- define SQL Server live feature enum
- register executable cases
- add completeness meta-test

### Phase 7: CI Hardening

- create separate live-DB pipeline
- publish dialect-specific results
- document local and CI execution commands

---

## Risks And Constraints

### 1. Coverage Drift

If shipped dialect features are not mirrored in the live feature manifest, the completeness guarantee becomes meaningless.

Mitigation:

- treat the manifest as part of dialect completion
- require manifest updates when adding new supported features

### 2. Test Runtime Growth

Live DB suites can become expensive and slow.

Mitigation:

- keep schemas small
- reuse containers per class when safe
- use manifest-driven focused cases rather than large random scenarios

### 3. Docker Environment Fragility

CI and local environments may differ in Docker behavior.

Mitigation:

- isolate this work in a dedicated pipeline
- keep existing fast CI independent of Docker
- document Docker requirements clearly

### 4. Over-Testing Semantics

Trying to deeply validate every semantic detail can make the suite brittle.

Mitigation:

- default to syntax/execution coverage
- add deeper semantics only for risk-heavy features

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

This epic is complete only when:

- live DB execution coverage is separated cleanly from fast integration coverage
- existing PostgreSQL Docker-backed tests are migrated
- every shipped dialect feature is represented in a live feature manifest
- every manifest is backed by executable real-DB cases
- a separate CI path exists to run the suites reliably

This epic improves confidence in shipped dialect syntax.
It does not change the framework boundary on DDL, stored procedures, or procedural SQL.

---

## Publishing GitHub Issues

The epic and stories can be published to GitHub issues from this markdown source.

Preview:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r6-live-db-issues.ps1 -WhatIf
```

Publish:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r6-live-db-issues.ps1
```

The wrapper delegates to the generic publisher in `scripts/create-github-issues-from-epic-md.ps1`.
