# Agent Instructions

This repository does not currently have an AGENTS.md. Add project-specific guidance below so agents can
work consistently with your expectations.

## Environment
- OS: Windows (PowerShell)
- Repo root: `c:\github\sqm`

## Goals
- Improve unit test coverage of parsers across modules (e.g., `sqm-parser*`).

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
