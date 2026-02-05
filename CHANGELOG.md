# 📜 Changelog

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
