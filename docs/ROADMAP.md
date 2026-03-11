# SQM Roadmap

This roadmap is prioritized for practical delivery and adoption.
Priority levels:
- `P0` = required for broad production use
- `P1` = high-value next step
- `P2` = nice to have / strategic

## Goals
- Expand dialect coverage beyond ANSI/PostgreSQL/MySQL.
- Add first-class DDL model/parse/render support.
- Keep middleware and validation usable across dialects.

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

### Epic R4: SQL Server Dialect Support
- Add `sqm-parser-sqlserver` and `sqm-render-sqlserver`.
- Define `SqlServerSpecs`.
- Support SQL Server pagination/identifier/function differences.
- Add integration round-trip tests and middleware smoke coverage.

### Epic R5: DDL MVP (Cross-Dialect Core)
- Extend `sqm-core` with DDL nodes:
  - `CreateTable`, `AlterTable`, `DropTable`
  - column defs, constraints (PK/FK/unique/check), index basics
- Add parser/renderer support for ANSI + PostgreSQL first.
- Add visitor/transformer/match/json support for all new DDL nodes.
- Add baseline semantic validation for DDL shape and references.

### Epic R6: Middleware DDL Policy Controls
- Add explicit DDL decision controls in `sqm-control`:
  - allowlist/denylist by operation (`CREATE`, `ALTER`, `DROP`)
  - scope by principal/tenant/object
- Add audit guidance and reason codes for DDL decisions.
- Add REST/MCP examples and integration tests.

## P1 (High Value)

### Epic R7: Oracle Dialect Support
- Add parser/renderer/specs modules for Oracle-specific syntax.
- Focus on top query constructs and compatibility gaps.

### Epic R8: DDL Coverage Expansion
- Add:
  - `CreateSchema`, `DropSchema`
  - `CreateIndex`, `DropIndex`, `Rename`
  - richer `AlterTable` actions
- Expand validation and cross-dialect render fallback behavior.

### Epic R9: Catalog/Introspection Unification for New Dialects
- Provide dialect-aware type mapping packs.
- Ensure catalog schema loading supports new dialect quirks.
- Add conformance tests per dialect for schema/validator interplay.

## P2 (Nice to Have)

### Epic R10: Cloud Dialect Packs
- Evaluate `Snowflake`, `BigQuery`, `Trino/Presto` packs.
- Prioritize read-only query support first.

### Epic R11: Query Optimization/Normalization Packs
- Optional optimizer passes:
  - predicate simplification
  - projection pruning
  - canonical forms for cache/fingerprint stability
- Keep optimizer opt-in and deterministic.

### Epic R12: DML Advanced Expansion
- Expand beyond the delivered DML baseline with advanced dialect-specific DML features (for example SQL Server `OUTPUT`, richer MySQL assignment/target variants, and MERGE-family support where applicable).
- Ensure parser/render/validate/rewrite parity with existing query flows.

## Suggested Implementation Order
1. `R4` SQL Server
2. `R5` DDL MVP core
3. `R6` DDL middleware controls
4. `R8` DDL expansion
5. `R7` Oracle
6. `R9` catalog unification
7. `R10`/`R11`/`R12` as capacity permits

## Exit Criteria Per Epic
- Model + parser + renderer + tests + docs complete.
- Visitor/transformer/match/json coverage for every new node.
- Integration tests added (`sqm-it`, and middleware IT where relevant).
- README/wiki/docs updated with examples.
