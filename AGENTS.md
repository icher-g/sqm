# Agent Instructions
Use this file as the primary, repo-specific instruction set for any agent working in `c:\github\sqm`.
Agents should read it first and follow it throughout the task.

## Environment
- OS: Windows (PowerShell)
- Repo root: `c:\github\sqm`

## Goals
- Improve unit test coverage of parsers across modules (e.g., `sqm-parser*`).

## Minimum Requirements
- Every new public class or public method must include JavaDoc.
- Every new behavior must be covered by tests; prefer unit tests close to the change.
- Avoid instantiating `Impl` classes directly; use interface `of(...)` factories.
- For any new node types, ensure:
  - Dedicated visitor interface methods and RecursiveNodeVisitor implementations (leaf nodes only).
  - RecursiveNodeTransformer coverage.
  - Matchers API coverage.
  - JSON mixins coverage.
  - MODEL.md updated with the new node(s).
- Each node must have a dedicated parser and renderer (even if unsupported in a dialect).
- Each new node interface must implement its own {@code accept()} method; avoid {@code instanceof} dispatch in base nodes.

## Conventions
- Prefer small, focused tests that cover edge cases and error handling.
- Keep tests deterministic; avoid external I/O unless explicitly required.
- Use existing test utilities and patterns already in the repo.

## Workflow
- Locate parser entry points and associated test suites.
- Add missing tests for:
  - Valid inputs (happy paths)
  - Invalid inputs (error paths)
  - Boundary conditions

## Notes
- Update this file with more specific instructions as needed.
- All dialect features must be modeled in `sqm-core` nodes.
- Parsers should only accept features supported by their dialect.
- Renderers should only render features supported by their dialect, otherwise reject.
