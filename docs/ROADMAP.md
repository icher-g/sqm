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

### Epic R2: SQL Server Dialect Support
- Add `sqm-parser-sqlserver` and `sqm-render-sqlserver`.
- Define `SqlServerSpecs`.
- Support SQL Server pagination/identifier/function differences.
- Add integration round-trip tests and middleware smoke coverage.

### Epic R3: DDL MVP (Cross-Dialect Core)
- Extend `sqm-core` with DDL nodes:
  - `CreateTable`, `AlterTable`, `DropTable`
  - column defs, constraints (PK/FK/unique/check), index basics
- Add parser/renderer support for ANSI + PostgreSQL first.
- Add visitor/transformer/match/json support for all new DDL nodes.
- Add baseline semantic validation for DDL shape and references.

### Epic R4: Middleware DDL Policy Controls
- Add explicit DDL decision controls in `sqm-control`:
  - allowlist/denylist by operation (`CREATE`, `ALTER`, `DROP`)
  - scope by principal/tenant/object
- Add audit guidance and reason codes for DDL decisions.
- Add REST/MCP examples and integration tests.

## P1 (High Value)

### Epic R5: Oracle Dialect Support
- Add parser/renderer/specs modules for Oracle-specific syntax.
- Focus on top query constructs and compatibility gaps.

### Epic R6: DDL Coverage Expansion
- Add:
  - `CreateSchema`, `DropSchema`
  - `CreateIndex`, `DropIndex`, `Rename`
  - richer `AlterTable` actions
- Expand validation and cross-dialect render fallback behavior.

### Epic R7: Catalog/Introspection Unification for New Dialects
- Provide dialect-aware type mapping packs.
- Ensure catalog schema loading supports new dialect quirks.
- Add conformance tests per dialect for schema/validator interplay.

## P2 (Nice to Have)

### Epic R8: Cloud Dialect Packs
- Evaluate `Snowflake`, `BigQuery`, `Trino/Presto` packs.
- Prioritize read-only query support first.

### Epic R9: Query Optimization/Normalization Packs
- Optional optimizer passes:
  - predicate simplification
  - projection pruning
  - canonical forms for cache/fingerprint stability
- Keep optimizer opt-in and deterministic.

### Epic R10: DML Advanced Expansion
- Expand beyond DML-R1 baseline with advanced dialect-specific DML features (for example PostgreSQL `RETURNING`, SQL Server `OUTPUT`, MySQL upsert/multi-table forms, and MERGE-family support where applicable).
- Ensure parser/render/validate/rewrite parity with existing query flows.

## Suggested Implementation Order
1. `R2` SQL Server
2. `R3` DDL MVP core
3. `R4` DDL middleware controls
4. `R6` DDL expansion
5. `R5` Oracle
6. `R7` catalog unification
7. `R8`/`R9`/`R10` as capacity permits

## Exit Criteria Per Epic
- Model + parser + renderer + tests + docs complete.
- Visitor/transformer/match/json coverage for every new node.
- Integration tests added (`sqm-it`, and middleware IT where relevant).
- README/wiki/docs updated with examples.

