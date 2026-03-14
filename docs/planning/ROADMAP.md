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
- RETURNING/OUTPUT support remains deferred to post-R1 dialect work items tracked in GitHub issues.

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
- Core statement models now retain immutable optimizer hint bodies where those contexts are supported.
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

### Epic R5: SQL Server Dialect Support
- Add `sqm-parser-sqlserver` and `sqm-render-sqlserver`.
- Define `SqlServerSpecs`.
- Support SQL Server pagination/identifier/function differences.
- Add integration round-trip tests and middleware smoke coverage.

### DDL Track (Decision Required)
- DDL is not currently committed as a framework feature.
- Before any DDL epic is scheduled, SQM needs a separate design decision on:
  - whether DDL belongs in the framework at all
  - whether DDL belongs in the same shared core model as query/DML
  - what the cross-dialect contract would be if supported
- Until that decision is made, DDL should remain out of scope for dialect epics and downstream completion criteria.

## P1 (High Value)

### Epic R8: Oracle Dialect Support
- Add parser/renderer/specs modules for Oracle-specific syntax.
- Focus on top query constructs and compatibility gaps.

### Epic R10: Catalog/Introspection Unification for New Dialects
- Provide dialect-aware type mapping packs.
- Ensure catalog schema loading supports new dialect quirks.
- Add conformance tests per dialect for schema/validator interplay.

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
- Expand beyond the delivered DML baseline with advanced dialect-specific DML features (for example SQL Server `OUTPUT`, richer MySQL assignment/target variants, and MERGE-family support where applicable).
- Ensure parser/render/validate/rewrite parity with existing query flows.

## Suggested Implementation Order
1. `R4` SQL transpilation foundation
2. `R5` SQL Server
3. `R8` Oracle
4. `R10` catalog unification
5. Separate DDL decision, only if explicitly accepted
6. `R11`/`R12`/`R13` as capacity permits

## Exit Criteria Per Epic
- Model + parser + renderer + tests + docs complete.
- Visitor/transformer/match/json coverage for every new node.
- Integration tests added (`sqm-it`, and middleware IT where relevant).
- README/wiki/docs updated with examples.
