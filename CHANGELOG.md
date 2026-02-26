# Changelog

## [Unreleased]
### Added
- Middleware framework example in `examples`:
  - `examples/src/main/java/io/sqm/examples/Middleware_EndToEndPolicyFlow.java`
- Middleware wiki guide:
  - `wiki-src/SQL-Middleware-Framework.md`
- Middleware policy templates wiki guide:
  - `wiki-src/SQL-Middleware-Policy-Templates.md`
- PostgreSQL middleware integration tests (`sqm-it`):
  - `sqm-it/src/test/java/io/sqm/it/PostgresMiddlewareIntegrationTest.java`
- Docker-free middleware flow tests (`sqm-control`):
  - `sqm-control/src/test/java/io/sqm/control/AiSqlMiddlewareFlowTest.java`

### Changed
- Updated wiki navigation and home quickstart to include middleware guidance.
- Updated README with `sqm-control` middleware usage overview and links to example/tests.
- Clarified `docs/MODEL.md` scope note: middleware composes behavior on top of existing AST nodes (no new node types introduced by middleware).

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
- Implemented **ANSI SQL parser** (`sqm-parser` module) converting SQL â†’ SQM AST.
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

