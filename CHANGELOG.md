# 📜 Changelog

## [v0.1.1] - 2025-11-12
### Added
- Initial public release of **SQM (Structured Query Model)**.
- Introduced full immutable AST model (`Node` hierarchy) for SQL representation.
- Added pattern-matching API (`matchX()` methods) for type-safe traversal and extraction.
- Implemented **ANSI SQL parser** (`sqm-parser` module) converting SQL → SQM AST.
- Implemented **ANSI SQL renderer** (`sqm-render-ansi` module) with dialect-based repository.
- Added **CTE**, **window functions**, **composite queries**, and **joins** support.
- Added fluent **DSL builders** for programmatic query construction.
- Provided **JSON serialization** for the model.
- Integrated **JUnit 5** testing with JaCoCo (~73% coverage).
- Project built on **Java 21+** (Maven).

### Known Limitations
- Parameters (`?`) and arithmetic operations not yet supported in parser.
- No DML (INSERT/UPDATE/DELETE/MERGE) support yet.
- Optimizer and validator not yet implemented.
- PostgreSQL dialect pending.

### Roadmap
- [ ] Parse query parameters (`WHERE col = ?`)
- [ ] Support arithmetic in SQL (`SELECT salary + bonus`)
- [ ] Add DML operations
- [ ] PostgreSQL dialect renderer/parser
- [ ] Query optimizer and validator

---
