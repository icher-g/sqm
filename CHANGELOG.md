# Changelog

## [Unreleased]

## [v0.4.0] - 2026-03-30

### Added
- First-class SQL Server delivery across the framework:
  - new SQL Server dialect modules for shared capability/model artifacts, parser, renderer, validator, and catalog type mapping
  - baseline and advanced SQL Server query/DML support including bracket identifiers, `TOP`, `TOP ... PERCENT`, `TOP ... WITH TIES`, `OFFSET/FETCH`, statement/table hints, `OUTPUT`, `OUTPUT ... INTO`, table variables, and `MERGE`
  - SQL Server downstream support across DSL, codegen, control/middleware flows, transpilation, and integration tests
- New live database integration module and engine-backed verification coverage for PostgreSQL, MySQL, and SQL Server.
- Typed hint modeling in `sqm-core` with structured statement and table hint nodes used across parser, renderer, transpilation, DSL, codegen, JSON, and tests.
- Transformation ergonomics helper layer in `sqm-core`:
  - `IdentifierTransforms`
  - `RelationTransforms`
  - `StatementTransforms`
  - `LiteralTransforms`
  - result-clause semantic helpers for DML result inspection
- Dedicated wiki guides for query transform helpers and examples.

### Changed
- Shared DML result modeling now cleanly covers PostgreSQL `RETURNING`, MySQL result-clause reachability, and SQL Server `OUTPUT` / `OUTPUT ... INTO` semantics through a unified result-clause surface.
- SQL transpilation coverage expanded for SQL Server advanced features, typed-hint handling, and clearer unsupported/approximate diagnostics.
- Core model and dialect support gaps were closed for high-value manipulation surfaces including `AT TIME ZONE`, `LATERAL`, `FunctionTable`, `TopSpec`, `VariableTableRef`, array support, and MySQL validation coverage.
- Documentation, wiki navigation, examples, and downstream guides were refreshed to reflect SQL Server support and the new transform-helper workflow.

## [v0.3.3] - 2026-03-13

### Added
- New module: `sqm-transpile` with a first-class SQL transpilation pipeline for source-to-target dialect conversion.
- New semantic core node: `ConcatExpr`, with ANSI/PostgreSQL `||` parsing/rendering and MySQL `CONCAT(...)` parsing/rendering.
- PostgreSQL -> MySQL transpilation coverage for:
  - exact concat conversion through semantic `ConcatExpr`
  - exact null-safe comparison conversion (`IS [NOT] DISTINCT FROM`)
  - exact regex subset conversion where supported by both dialects
  - approximate `ILIKE` conversion with structured warnings
  - explicit unsupported diagnostics for `RETURNING`, `DISTINCT ON`, `SIMILAR TO`, and representative PostgreSQL-specific operator families
- MySQL -> PostgreSQL transpilation coverage for:
  - exact concat conversion through semantic `ConcatExpr`
  - exact null-safe comparison conversion (`<=>`)
  - warning-based dropping of MySQL optimizer hints and index hints
  - explicit unsupported diagnostics for `ON DUPLICATE KEY UPDATE`, insert modes/modifiers, and representative MySQL JSON function families
- End-to-end transpilation integration coverage in `sqm-it`.
- Transpilation example programs in `examples`.
- New transpilation usage/design docs and wiki guides.

### Changed
- `SqlDialectId` is now the shared dialect identity value object used by `sqm-transpile` and the `sqm-control` execution pipeline.
- `sqm-control` now stores `SqlDialectId` directly in `ExecutionContext` while preserving `dialect()` as a compatibility accessor.
- README, examples, wiki navigation, and planning docs now describe the transpilation workflow and current rule matrix.

## [v0.3.2] - 2026-03-11

### Added
- New module: `sqm-catalog-mysql` with dedicated MySQL SQL-type mapping for JDBC schema introspection and codegen plugin validation.
- DML-R1 statement foundation delivery:
  - statement root support (`Statement`) with query + DML dispatch
  - neutral core `InsertStatement`, `UpdateStatement`, `DeleteStatement` model nodes
  - ANSI baseline parser/renderer for INSERT/UPDATE/DELETE
  - DML node-contract coverage (visitor/transformer/match/json)
  - statement round-trip integration coverage in `sqm-it`
- MySQL R1 dialect delivery across parser/renderer/spec modules:
  - `sqm-core-mysql`
  - `sqm-parser-mysql`
  - `sqm-render-mysql`
- MySQL round-trip integration coverage in `sqm-it`.
- PostgreSQL DML extension delivery:
  - `INSERT ... RETURNING`
  - `UPDATE ... FROM`
  - `DELETE ... USING`
  - `INSERT ... ON CONFLICT DO NOTHING / DO UPDATE`
  - writable CTE `INSERT ... RETURNING`, `UPDATE ... RETURNING`, and `DELETE ... RETURNING` support
  - PostgreSQL DML round-trip integration coverage in `sqm-it`
- MySQL R1B feature coverage:
  - null-safe equality predicate (`<=>`)
  - regex predicates (`REGEXP`, `RLIKE`)
  - locking modifiers (`FOR SHARE`, `NOWAIT`, `SKIP LOCKED`)
  - `GROUP BY ... WITH ROLLUP`
- MySQL DML extension delivery:
  - `INSERT IGNORE`
  - `INSERT ... ON DUPLICATE KEY UPDATE`
  - `REPLACE INTO`
  - joined `UPDATE`
  - qualified joined-`UPDATE` assignment targets
  - optimizer hint comments on `UPDATE` and `DELETE`
  - `STRAIGHT_JOIN`
  - canonical `DELETE FROM ... USING ... JOIN ...`
  - alias/index-hint hardening and round-trip integration coverage
- MySQL SQL-mode-aware parser options:
  - explicit `MySqlSqlMode` support in `MySqlSpecs`
  - `ANSI_QUOTES` parser mode for double-quoted identifiers
- MySQL built-in function coverage:
  - prioritized JSON/date/string function support for `JSON_EXTRACT`, `JSON_OBJECT`, `DATE_ADD`, `DATE_SUB`, `CONCAT_WS`, and `SUBSTRING_INDEX`
  - MySQL interval literal support for date-arithmetic function arguments, including MySQL input forms like `INTERVAL 1 DAY`
  - signed MySQL interval input forms such as `INTERVAL -1 DAY`
- New module: `sqm-validate-mysql` with optional MySQL semantic validation for overlapping `USE INDEX` and `FORCE INDEX` table hints.
- MySQL renderer now supports explicit optimizer-hint normalization policies, with default pass-through behavior and opt-in whitespace trimming/normalization.

### Changed
- `sqm-codegen-maven-plugin` now selects JDBC schema type mapping by configured dialect and uses dedicated MySQL/PostgreSQL catalog mappers where available.
- Downstream support documentation now includes a repository-owned support matrix covering ANSI, PostgreSQL, and MySQL query/DML support across validation, control, codegen, plugin, and middleware modules.
- DML documentation updated to reflect delivered ANSI, PostgreSQL, and MySQL statement support.
- MySQL docs now reflect delivered DML extensions, canonical alias/index-hint rendering, and current `RETURNING` limitations by version.
- MySQL roadmap/docs now reflect post-R3 join-semantic hardening with qualified update targets and `STRAIGHT_JOIN`.
- MySQL docs and roadmap now reflect optimizer hint support across `SELECT`, `UPDATE`, and `DELETE`.
- MySQL parser docs now describe explicit SQL-mode configuration via `MySqlSpecs`.
- MySQL docs and roadmap now reflect prioritized built-in function coverage and canonical rendering of quoted and unquoted MySQL interval-literal date-function inputs.
- MySQL docs now describe the optional `sqm-validate-mysql` index-hint validation layer.
- MySQL docs and roadmap now reflect opt-in optimizer-hint normalization controls.
- Middleware docs and examples now reflect MySQL and DML transport coverage across core, REST, and MCP adapters.
- Dialect docs and roadmap updated to reflect MySQL R1/R3 completion and PostgreSQL DML extension completion.
- PostgreSQL documentation now reflects full writable CTE `INSERT ... RETURNING`, `UPDATE ... RETURNING`, and `DELETE ... RETURNING` support.

## [v0.3.1] - 2026-03-01
### Added
- New framework modules introduced in this release:
  - `sqm-control`
  - `sqm-middleware-api`
  - `sqm-middleware-core`
  - `sqm-middleware-rest`
  - `sqm-middleware-mcp`
  - `sqm-middleware-it`
- First release of SQL decision/middleware pipeline capabilities:
  - parse -> validate -> rewrite -> render -> decision
  - unified runtime config, guardrails, and built-in rewrite wiring
- Middleware framework example in `examples`:
  - `examples/src/main/java/io/sqm/examples/Middleware_EndToEndPolicyFlow.java`
- Middleware wiki guides:
  - `wiki-src/SQL-Middleware-Framework.md`
  - `wiki-src/SQL-Middleware-Policy-Templates.md`
- PostgreSQL middleware integration tests (`sqm-it`):
  - `sqm-it/src/test/java/io/sqm/it/PostgresMiddlewareIntegrationTest.java`
- Docker-free middleware flow tests (`sqm-control`):
  - `sqm-control/src/test/java/io/sqm/control/AiSqlMiddlewareFlowTest.java`
- Runtime/ops documentation:
  - `docs/MIDDLEWARE_CONFIG_KEYS.md`
  - `docs/MIDDLEWARE_DEPLOYMENT_PROFILES.md`
  - `docs/MIDDLEWARE_RELEASE_CHECKLIST.md`
  - `docs/MIDDLEWARE_NFR.md`
  - `docs/MIDDLEWARE_RUNBOOK.md`
  - `docs/MIDDLEWARE_SLO_SLI.md`

### Changed
- Middleware/control baseline hardened for production-readiness:
  - strict production-mode config checks and schema bootstrap diagnostics
  - structured REST error contract and transport-level hardening
  - host flow control (in-flight limits, acquire timeout, request timeout)
  - telemetry/audit publisher wiring from centralized config keys
- REST host updates:
  - path-versioned API under `/sqm/middleware/v1/*`
  - readiness semantics aligned with bootstrap status (`200` when ready, `503` when not ready)
  - proxy-aware rate-limiting client key resolution (`X-Forwarded-For` style support)
  - request validation and correlation-id behavior hardened
- MCP host updates:
  - framing/protocol hardening and deterministic JSON-RPC error mapping
  - server version metadata resolution from build/package or `sqm.version`
- Audit durability updates:
  - file audit publisher supports optional rotation/retention (`maxBytes`, `maxHistory`)
- Core model/DSL cleanup:
  - identifier/qualified-name migration stabilized and docs/examples refreshed
  - builder-first model creation patterns expanded in tests/examples
- CI and release smoke improvements for middleware and codegen flows.

### Documentation
- Updated README and wiki navigation to reflect middleware architecture, modules, and versioned REST routes.
- Clarified model scope note in `docs/MODEL.md`: middleware composes behavior on top of existing AST nodes.

## [v0.3.0] - 2026-02-13
### Added
- New module: `sqm-core-postgresql` for shared PostgreSQL model/capability artifacts reused across parser, renderer, and validator layers.
- New module: `sqm-validate-postgresql` with PostgreSQL semantic validation dialect.
- PostgreSQL semantic rules including:
  - `DISTINCT ON` leftmost `ORDER BY` consistency validation.
  - Window-frame legality checks and PostgreSQL clause consistency checks.
  - PostgreSQL function-catalog backed validation support.
- New module: `sqm-schema-introspect`:
  - `JdbcSchemaProvider` for schema loading from live database metadata.
  - `JsonSchemaProvider` for snapshot-based schema loading/saving.
  - PostgreSQL SQL-type mapping for schema introspection.
- SQL codegen schema validation integration:
  - `sqm-codegen` now validates parsed queries against schema before emitting Java.
  - Dialect-aware validator selection (`ANSI` and `POSTGRESQL`).
- `sqm-codegen-maven-plugin` schema provider integration:
  - `schemaProvider=none|json|jdbc`.
  - Secure JDBC credential resolution via direct fields, environment variables, or Maven `settings.xml` server id.
  - JDBC cache support with refresh/write options.
  - Cache TTL (`schemaCacheTtlMinutes`) and metadata sidecar (`${schemaCachePath}.meta.properties`).
  - Schema/table include/exclude regex filters for JDBC introspection.
  - Expected DB product/version cache pinning.
  - Validation behavior toggle: `failOnValidationError`.
  - Validation report outputs (JSON + text summary) with `formatVersion`.
- Examples integration for schema-aware codegen:
  - Default JSON snapshot flow.
  - Optional JDBC profile (`jdbc-schema-validate`).

### Changed
- Refactored `GenerateMojo` internals into dedicated classes:
  - `CachingSchemaProvider`
  - `RegexFilteringSchemaProvider`
  - `JdbcDriverManagerDataSource`
  - `SchemaCacheMetadata`
  - `JdbcCredentials`
  - `ValidationReportWriter`
- Added dedicated JavaDoc for newly introduced public APIs and extracted helper components.
- Updated root README with schema-aware codegen guidance and Wiki link.

### Testing
- Added/expanded unit tests across:
  - `sqm-validate-postgresql` semantic rules and dialect behavior.
  - `sqm-codegen` schema validation paths.
  - `sqm-codegen-maven-plugin` JSON/JDBC provider behavior, cache reuse/refresh/TTL, and reporting.
  - `sqm-schema-introspect` JSON and JDBC providers.
- Added Docker-backed integration tests for PostgreSQL schema introspection and JDBC-backed codegen validation.
- CI updated to run Docker-based verification flow with explicit Docker API compatibility settings.

### Documentation
- Added `docs/SQL_FILE_CODEGEN_SCHEMA_VALIDATION.md` covering all configuration modes and options.
- Expanded docs/README references for secure credentials, caching strategy, and validation reports.
- Added repository-managed wiki source set (`wiki-src/`) and publish script (`scripts/publish-wiki.ps1`).

### Known Limitations
- No DML (INSERT/UPDATE/DELETE/MERGE) support yet.
- Query optimizer is not implemented yet.
- PostgreSQL SELECT INTO (table-creating SELECT) not supported.
- TABLESAMPLE not supported.
- Recursive CTE SEARCH/CYCLE not supported.
- FETCH FIRST ... WITH TIES not supported.

---

## [v0.2.0] - 2026-02-05
### Added
- PostgreSQL parser support (sqm-parser-postgresql) integrated with ANSI base parsers.
- PostgreSQL custom operator precedence tiers with OPERATOR(...) support.
- Expression-level COLLATE support with dialect gating.
- PostgreSQL AT TIME ZONE expressions and exponentiation operator support.
- DISTINCT ON support.
- Pattern predicates: ILIKE, SIMILAR TO, regex (~, ~*, !~, !~*).
- IS DISTINCT FROM / IS NOT DISTINCT FROM predicate support.
- Type system modeling for CAST/TypeName and PostgreSQL :: casts.
- LATERAL and function-table support, including WITH ORDINALITY.
- GROUPING SETS / ROLLUP / CUBE group by extensions.
- ORDER BY ... USING operator support.
- SELECT locking clauses (FOR UPDATE/SHARE, NOWAIT, SKIP LOCKED).
- Array literals, subscripts, and slices.
- Generic operator lexing for PostgreSQL custom operators.
- Expanded unit tests for PostgreSQL operator precedence and policy behavior.

### Known Limitations
- No DML (INSERT/UPDATE/DELETE/MERGE) support yet.
- Optimizer and validator not yet implemented.
- PostgreSQL SELECT INTO (table-creating SELECT) not supported.
- TABLESAMPLE not supported.
- Recursive CTE SEARCH/CYCLE not supported.
- FETCH FIRST ... WITH TIES not supported.

### Roadmap
- [ ] Add DML operations
- [ ] Query optimizer and validator
- [ ] Add MySQL dialect (parser + renderer)
- [ ] Add PostgreSQL extensions coverage (parser + renderer)
- [ ] Add SQL Server dialect (parser + renderer)
- [ ] Add Oracle dialect (parser + renderer)
- [ ] Add SQLite dialect (parser + renderer)

---

## [v0.1.1] - 2025-11-12
### Added
- Initial public release of **SQM (Structured Query Model)**.
- Introduced full immutable AST model (`Node` hierarchy) for SQL representation.
- Added pattern-matching API (`matchX()` methods) for type-safe traversal and extraction.
- Implemented **ANSI SQL parser** (`sqm-parser` module) converting SQL → SQM AST.
- Implemented **ANSI SQL renderer** (`sqm-render-ansi` module) with dialect-based repository.
- Implemented **PostgreSQL SQL renderer** (`sqm-render-postgresql` module).
- Added **CTE**, **window functions**, **composite queries**, and **joins** support.
- Added fluent **DSL builders** for programmatic query construction.
- Provided **JSON serialization** for the model.
- Integrated **JUnit 5** testing with JaCoCo (~73% coverage).
- Project built on **Java 21+** (Maven).

### Known Limitations
- Parameters (`?`) and arithmetic operations not yet supported in parser.
- No DML (INSERT/UPDATE/DELETE/MERGE) support yet.
- Optimizer and validator not yet implemented.
- PostgreSQL dialect provided in renderer module.

### Roadmap
- [X] Parse query parameters (`WHERE col = ?`)
- [X] Support arithmetic in SQL (`SELECT salary + bonus`)
- [ ] Add DML operations
- [X] PostgreSQL dialect renderer
- [ ] Query optimizer and validator

---

