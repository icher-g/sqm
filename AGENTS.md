# Agent Instructions
Use this file as the primary, repo-specific instruction set for any agent working in `c:\github\sqm`.
Agents should read it first and follow it throughout the task.

## Environment
- OS: Windows (PowerShell)
- Repo root: `c:\github\sqm`

## Goals
- Improve unit test coverage of parsers across modules (e.g., `sqm-parser*`).

## Modeling Rules
- For model design decisions, semantic-node shape, and dialect-boundary questions, follow [`docs/reports/SQM_MODELING_RULES.md`](docs/reports/SQM_MODELING_RULES.md).
- Treat that file as the canonical reference for:
  - shared semantics vs dialect syntax
  - when a distinction deserves a semantic node
  - representability vs dialect support
  - whether syntax belongs in `sqm-core` or dialect packages

## Minimum Requirements
- Every new public class or public method must include JavaDoc.
- Avoid Javadoc warnings by documenting explicit public no-arg constructors and public fields/record components (`@param`/`@return` where applicable).
- Every new behavior must be covered by tests; prefer unit tests close to the change.
- Avoid instantiating `Impl` classes directly; use interface `of(...)` factories.
- Preserve node immutability contracts: transformers must return new nodes when changed and the same instance when unchanged.
- Dialect-specific features must be validated at both parse time and render time.
- New parser/renderer implementations must be registered in the corresponding dialect registries.
- For any new node types, ensure:
  - Dedicated visitor interface methods and RecursiveNodeVisitor implementations (leaf nodes only).
  - RecursiveNodeTransformer coverage.
  - Matchers API coverage.
  - JSON mixins coverage.
  - MODEL.md updated with the new node(s).
- Each node must have a dedicated parser and renderer (even if unsupported in a dialect).
- Each new node interface must implement its own {@code accept()} method; avoid {@code instanceof} dispatch in base nodes.
- Only add a dedicated matcher interface when a node type represents a real variant family with multiple concrete implementations that the matcher selects between. Do not add matcher wrappers for single-implementation node types.
- When planning or delivering a dialect epic, treat parser, renderer, validation, transpilation, DSL, and codegen impact as mandatory review areas; do not stop at parser/renderer only.
- For dialect epics where DML is in scope, the epic is not complete until DML parse/render/validate/transpile coverage is also supported, even if DML work is scheduled at the end of the epic.
- Treat DDL as a separate framework-level product and architecture decision, not as an assumed part of the roadmap or dialect work.
- Unless the repo instructions are explicitly updated with a DDL decision, keep DDL out of implementation and planning acceptance criteria to avoid accidental scope creep.

## Conventions
- Prefer small, focused tests that cover edge cases and error handling.
- Keep tests deterministic; avoid external I/O unless explicitly required.
- Use existing test utilities and patterns already in the repo.
- Tests should use DSL/helper methods for query model creation and serve as examples for developers.
- Avoid direct node factory usage in tests when a DSL/helper method can express the same intent.
- If a needed DSL/helper method does not exist for a test scenario, add it (with JavaDoc) and use it in the test.
- Review generated test/model-building code from a developer perspective: if the API usage feels awkward or unintuitive, simplify it (prefer DSL helpers) before finalizing.
- Test both happy paths and failure paths (unsupported features, invalid syntax/input, and boundary values).
- For SQL render assertions, normalize whitespace when formatting differences are not semantically relevant.
- Any new public model surface or node shape must be reachable through DSL helpers; if it is not, add DSL support before considering the work complete.
- When adding or changing nodes, verify codegen can express them via DSL and update codegen as needed; do not assume parser/renderer support alone is sufficient.
- When a derived parser or renderer would need to duplicate substantial base-class logic, refactor the base class into smaller overridable hooks instead of copying the whole implementation.
- Do not introduce parser or renderer state objects unless they are genuinely needed to carry shared phase data that cannot be derived cleanly at the point of use.

## Workflow
- Implement model changes first, then parser/renderer wiring, then DSL/helpers, then tests/docs.
- Locate parser entry points and associated test suites.
- For dialect work, review and update all affected modules as needed:
  - `sqm-core`
  - `sqm-parser-*`
  - `sqm-render-*`
  - `sqm-validate*`
  - `sqm-transpile`
  - `sqm-control`
  - `sqm-codegen` and `sqm-codegen-maven-plugin`
  - middleware and integration-test modules
- Add missing tests for:
  - Valid inputs (happy paths)
  - Invalid inputs (error paths)
  - Boundary conditions

## Notes
- Update this file with more specific instructions as needed.
- Parsers should only accept features supported by their dialect.
- Renderers should only render features supported by their dialect, otherwise reject.
- `sqm-core` must not depend on parser/renderer modules.
- Documentation is part of done: update README/docs/MODEL when behavior or model changes.
- For node types that already have a builder, keep a single canonical `of(...)` factory covering the full state. Do not add telescoping convenience factory overloads; prefer the builder for ergonomic construction.
- Before introducing any new identifier-like SQL keyword or reserved word in parser logic, first check whether it should be added as a dedicated `TokenType` and lexer keyword instead of matching it as a plain `IDENT`.
- In parsers, prefer `cur.expect(...)` over manual `consumeIf(...)` + error branches when a token is required after the parser has already committed to a branch. Use `consumeIf(...)` for truly optional syntax probes.
- `sqm-validate` and dialect-specific validation modules must be considered part of dialect completeness, not optional follow-up work.
- `sqm-transpile` must be reviewed for every new dialect epic so unsupported, approximate, and exact behavior stays explicit.
- When a dialect epic includes transpilation scope, document the expected next rule families explicitly in the epic or story design; do not leave future transpile expansion as a vague placeholder.
- Epic designs should explicitly state scope for Query, DML, and DDL separately so completion criteria are unambiguous.
- Current repo position: DDL support is not assumed for the framework and requires a separate explicit design decision before it should be planned as implementation work.
- If a dialect implementation temporarily falls back to a generic or default implementation because a dialect-specific piece is missing, do not leave that fallback implicit.
- For every such fallback, either:
  - fix the current design so the missing dialect-specific piece is included in the active story or epic, or
  - record the missing piece explicitly in the follow-up design/story so it cannot be forgotten.
