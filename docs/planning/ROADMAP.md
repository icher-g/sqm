# SQM Roadmap

This roadmap is prioritized for practical delivery and adoption.
Priority levels:
- `P0` = required for broad production use
- `P1` = high-value next step
- `P2` = nice to have / strategic

## Goals
- Expand dialect coverage beyond ANSI/PostgreSQL/MySQL.
- Add first-class SQL transpilation before expanding to additional dialect families.
- Keep middleware and validation usable across dialects.
- Keep DDL as a separate product/architecture decision rather than assuming it belongs in the framework.

## P0 (Required)

### Epic R0: DML Foundation (Completed)
- Statement-level model introduced (Statement) with query and DML statement families.
- Core neutral DML nodes delivered: InsertStatement, UpdateStatement, DeleteStatement.
- ANSI baseline parser/renderer support delivered for INSERT/UPDATE/DELETE.
- Node-contract support delivered (visitor/transformer/match/json).
- Integration coverage delivered in sqm-it (statement round-trip).
- Compatibility boundaries and deferred DML extensions are tracked in GitHub issues.
- Result-clause support (`RETURNING`/`OUTPUT`) was delivered in later dialect work on top of the DML foundation.

### Epic R1: MySQL Dialect Support (Completed)
- Add `sqm-parser-mysql` and `sqm-render-mysql`.
- Define `MySqlSpecs` feature matrix.
- Cover core query syntax parity with existing ANSI/PostgreSQL flows.
- Add integration tests in `sqm-it` for round-trip parse/render.
- Delivery split: `R1A` baseline + `R1B` common MySQL-specific features (completed).

### Epic R2: PostgreSQL DML Extensions (Completed)
- Implement PostgreSQL-specific DML features on top of ANSI DML baseline.
- Delivered scope: `INSERT ... RETURNING`, `UPDATE ... FROM`, `DELETE ... USING`, `INSERT ... ON CONFLICT`.
- Writable CTE support delivered for PostgreSQL `INSERT ... RETURNING`, `UPDATE ... RETURNING`, and `DELETE ... RETURNING` shapes.
- Parser/render capability gating and deterministic SQL output delivered.
- Integration round-trip coverage delivered in `sqm-it`.
- Alias/keyword ambiguity hardening added for PostgreSQL DML target parsing.

### Epic R3: MySQL DML Extensions (Completed)
- Implemented MySQL-specific DML features on top of ANSI DML baseline.
- Delivered scope: `INSERT ... ON DUPLICATE KEY UPDATE`, `INSERT IGNORE`, `REPLACE INTO`, joined `UPDATE`, canonical `DELETE FROM ... USING ... JOIN ...`.
- Parser/render capability gating and deterministic SQL output delivered.
- Integration round-trip coverage delivered in `sqm-it`.
- Alias/index-hint hardening added for prioritized MySQL joined DML target/source shapes.
- Version-aware MySQL `RETURNING` gating is explicit and remains unsupported for current supported MySQL versions.

### Epic MYSQL-R2: MySQL Join Semantics Hardening (Completed)
- Delivered scope: qualified MySQL joined-`UPDATE` assignment targets and `STRAIGHT_JOIN`.
- Parser/render capability gating and deterministic canonical SQL output delivered.
- Integration round-trip coverage delivered in `sqm-it`.

### Epic MYSQL-R2B: MySQL Optimizer Hint Context Expansion (Completed)
- Delivered scope: optimizer hint comments in MySQL `SELECT`, `UPDATE`, and `DELETE` statement contexts.
- Core statement models now retain typed statement hints for those supported contexts.
- Parser/render capability gating and deterministic canonical SQL output delivered.
- Integration round-trip coverage delivered in `sqm-it`.

### Epic MYSQL-R2C: MySQL SQL Mode-Aware Parser Options (Completed)
- Delivered scope: explicit SQL-mode parser options in `MySqlSpecs`.
- `ANSI_QUOTES` is modeled as a first-class parser mode and enables double-quoted identifiers during parsing.
- Default parser behavior remains backward compatible with backtick-only MySQL identifier quoting.

### Epic MYSQL-R2D: MySQL Built-in Function Coverage (Completed)
- Delivered scope: explicit MySQL parser/renderer coverage for prioritized built-in JSON/date/string functions.
- Coverage includes prioritized built-ins such as `JSON_EXTRACT`, `JSON_OBJECT`, `DATE_ADD`, `DATE_SUB`, `CONCAT_WS`, and `SUBSTRING_INDEX`.
- MySQL interval coverage includes canonical `INTERVAL '...'` literals plus MySQL input forms such as `INTERVAL 1 DAY` and `INTERVAL -1 DAY`.

### Epic MYSQL-R2E: MySQL Validation Hardening (Completed)
- Delivered scope: optional MySQL schema-validation rule for conflicting index-hint combinations.
- `sqm-validate-mysql` now reports overlapping `USE INDEX` + `FORCE INDEX` combinations on the same effective table scope before execution.

### Epic MYSQL-R2F: MySQL Optimizer Hint Normalization (Completed)
- Delivered scope: opt-in MySQL optimizer-hint normalization policy for rendering.
- Default rendering remains pass-through, while explicit policies can trim or normalize hint-body whitespace for `SELECT`, `UPDATE`, and `DELETE`.
- MySQL interval literal support covers both canonical `INTERVAL '...'` and MySQL input forms like `INTERVAL 1 DAY`, which render back to the quoted canonical form.

### Epic R3X: Downstream MySQL and DML Parity (Completed)
- Delivered statement-aware downstream support for ANSI, PostgreSQL, and MySQL query/DML flows in `sqm-validate`, `sqm-control`, and `sqm-codegen`.
- `sqm-codegen-maven-plugin` now supports schema-backed MySQL code generation with dedicated PostgreSQL and MySQL catalog mappers.
- Middleware adapters (`sqm-middleware-core`, `sqm-middleware-rest`, `sqm-middleware-mcp`) now include MySQL and DML transport coverage.
- A repository-owned downstream support matrix documents the supported dialect/statement combinations and is regression-checked in tests.

### Epic R4: SQL Transpilation Foundation (Completed)
- Add `sqm-transpile` as a dedicated module for source-to-target SQL conversion.
- Introduce a first-class transpilation pipeline: parse source dialect, normalize, rewrite to target semantics, validate target, render target.
- Deliver reusable transpilation rule contracts and a registry that assembles concrete source/target execution plans from shared rules.
- Support exact, approximate, and unsupported outcomes with explicit diagnostics and rewrite reporting.
- Deliver an initial PostgreSQL <-> MySQL transpilation slice focused on a small exact subset plus explicit unsupported cases.
- Backlog follow-up: consolidate dialect identity usage across modules around shared `SqlDialectId` instead of mixed raw strings and service-specific dialect naming.

Reference epic doc: `docs/epics/R4_SQL_TRANSPILATION_FOUNDATION_COMPLETED.md`

### Epic R5: SQL Server Dialect Support (Completed)
- Add `sqm-parser-sqlserver` and `sqm-render-sqlserver`.
- Define `SqlServerSpecs`.
- Support SQL Server pagination/identifier/function differences.
- Add integration round-trip tests and middleware smoke coverage.
Reference epic doc: `docs/epics/R5_SQL_SERVER_DIALECT_SUPPORT_COMPLETED.md`

### Epic R5B: SQL Server Advanced Support (Completed)
- Deliver the advanced SQL Server features intentionally deferred by `R5`.
- Keep DDL explicitly out of scope while completing advanced DML/query semantics, validation, transpilation, DSL, codegen, and downstream wiring.
Reference epic doc: `docs/epics/R5B_SQL_SERVER_ADVANCED_SUPPORT_COMPLETED.md`

### Epic R6: Dialect Live DB Execution Coverage (Completed)
- Add a dedicated live-database execution test track.
- Separate Docker-backed real-engine verification from fast integration coverage.
- Track executable coverage by shipped dialect feature families.
Reference epic doc: `docs/epics/R6_DIALECT_LIVE_DB_EXECUTION_COVERAGE_COMPLETED.md`

### Epic R7: Typed Hint Modeling
- Introduce a typed hint model in `sqm-core`.
- Align parser, renderer, validation, transpilation, DSL, codegen, and JSON behavior around first-class hint semantics.

### DDL Track (Decision Required)
- DDL is not currently committed as a framework feature.
- Before any DDL epic is scheduled, SQM needs a separate design decision on:
  - whether DDL belongs in the framework at all
  - whether DDL belongs in the same shared core model as query/DML
  - what the cross-dialect contract would be if supported
- Until that decision is made, DDL should remain out of scope for dialect epics and downstream completion criteria.

## P1 (High Value)

### Epic R8: Dialect Support Gap Closure
- Close the currently documented `Not implemented by SQM` dialect gaps from `MODEL.md`.
- Require each story to confirm dialect/version capability before coding and either implement or reclassify deliberately.

### Epic R9: Transformation Ergonomics
- Improve transformation authoring ergonomics without replacing the current visitor/matcher/transformer model.
- Add a small, explicit ergonomic layer for repeated semantic inspection and rewrite tasks.

### Epic R10: Catalog/Introspection Unification for New Dialects
- Provide dialect-aware type mapping packs.
- Ensure catalog schema loading supports new dialect quirks.
- Add conformance tests per dialect for schema/validator interplay.

### Epic ORACLE-R1: Oracle Dialect Support
- Add parser/renderer/specs modules for Oracle-specific syntax.
- Focus on top query constructs and compatibility gaps.

## P2 (Nice to Have)

### Epic R11: Cloud Dialect Packs
- Evaluate `Snowflake`, `BigQuery`, `Trino/Presto` packs.
- Prioritize read-only query support first.

### Epic R12: Query Optimization/Normalization Packs
- Optional optimizer passes:
  - predicate simplification
  - projection pruning
  - canonical forms for cache/fingerprint stability
- Optional follow-up: evaluate a `StatementFingerprint` abstraction for `INSERT`/`UPDATE`/`DELETE`, but only after explicit normalization semantics are defined for DML.
- Keep optimizer opt-in and deterministic.

### Epic R13: DML Advanced Expansion
- Expand beyond the delivered DML baseline with remaining advanced dialect-specific DML features (for example richer MySQL assignment/target variants and MERGE-family support where applicable).
- Ensure parser/render/validate/rewrite parity with existing query flows.

## Suggested Implementation Order
1. `R4` SQL transpilation foundation
2. `R5` SQL Server
3. `R5B` SQL Server advanced support
4. `R6` live DB execution coverage
5. `R7` typed hint modeling
6. `R8` dialect support gap closure
7. `R9` transformation ergonomics
8. `ORACLE-R1` Oracle
9. `R10` catalog unification
10. Separate DDL decision, only if explicitly accepted
11. `R11`/`R12`/`R13` as capacity permits

## Exit Criteria Per Epic
- Model + parser + renderer + tests + docs complete.
- Visitor/transformer/match/json coverage for every new node.
- Integration tests added (`sqm-it`, and middleware IT where relevant).
- README/wiki/docs updated with examples.
