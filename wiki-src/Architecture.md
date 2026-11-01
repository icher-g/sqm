# Architecture

Base SQL model flow:

```text
       ┌─────────────┐
       │   SQL Text  │
       └──────┬──────┘
              │ parse
              ▼
       ┌─────────────┐
       │    Model    │ <───> JSON / DSL
       └──────┬──────┘
              │ transform / rewrite / optimize
              ▼
       ┌─────────────┐
       │    Model    │
       └──────┬──────┘
              │ render
              ▼
       ┌─────────────┐
       │   SQL Text  │
       └─────────────┘
```

Middleware decision flow:

```text
       ┌─────────────┐
       │   SQL Text  │
       └──────┬──────┘
              │ parse
              ▼
       ┌─────────────┐
       │    Model    │
       └──────┬──────┘
              │ validate
              ▼
       ┌─────────────┐
       │ Guardrails  │
       │ + Policies  │
       └──────┬──────┘
              │ transform / rewrite / optimize
              ▼
       ┌─────────────┐
       │  Rewritten  │
       │    Model    │
       └──────┬──────┘
              │ render
              ▼
       ┌─────────────┐
       │   SQL Text  │
       │ + Bind Data │
       └──────┬──────┘
              │ decide
              ▼
       ┌─────────────┐
       │  Decision   │
       │ allow/deny/ │
       │   rewrite   │
       └─────────────┘
```

Core components:
- **Model (sqm-core)** - AST nodes, visitor/transformer infrastructure, match API, DSL primitives.
- **Parsers (sqm-parser-*)** - dialect parsing into model nodes.
- **Renderers (sqm-render-*)** - dialect rendering from model nodes.
- **Validation (sqm-validate*)** - schema/function/access-policy semantic checks.
- **Transpilation (sqm-transpile)** - source-to-target dialect conversion with rule diagnostics.
- **Catalog (sqm-catalog*)** - schema sources (JSON/JDBC) and type mapping.
- **Control/Middleware (sqm-control, sqm-middleware-*)** - decision engine, rewrites, runtime hosts, telemetry/audit.
- **JSON (sqm-json)** - serialization mixins.
- **Codegen (sqm-codegen*)** - SQL file code generation and plugin integration.
- **Integration tests (sqm-it)** - cross-module/runtime verification.

## Module families

| Modules | Responsibility |
|---|---|
| `sqm-core` | Immutable nodes, DSL, visitors, transformers, and match API |
| `sqm-core-{postgresql,mysql,sqlserver,oracle}` | Dialect capabilities and model support |
| `sqm-parser`, `sqm-parser-ansi`, `sqm-parser-{postgresql,mysql,sqlserver,oracle}` | Shared parsing infrastructure and dialect grammar |
| `sqm-render`, `sqm-render-ansi`, `sqm-render-{postgresql,mysql,sqlserver,oracle}` | Rendering infrastructure and dialect SQL |
| `sqm-catalog`, `sqm-catalog-{postgresql,mysql,sqlserver,oracle}` | Schema metadata, providers, and SQL type mapping |
| `sqm-validate`, `sqm-validate-{postgresql,mysql,sqlserver,oracle}` | Schema validation and dialect rule packs |
| `sqm-transpile` | Exact, approximate, and unsupported conversion rules |
| `sqm-json` | Jackson model serialization |
| `sqm-codegen`, `sqm-codegen-maven-plugin` | SQL-file-to-Java generation |
| `sqm-control` | Decision service, guardrails, and rewrites |
| `sqm-middleware-api`, `sqm-middleware-core`, `sqm-middleware-rest`, `sqm-middleware-mcp` | Transport contracts, runtime services, and REST/MCP hosts |
| `sqm-playground-api`, `sqm-playground-rest`, `sqm-playground-web` | Playground API, backend, and frontend |
| `sqm-it`, `sqm-db-it`, `sqm-middleware-it` | Cross-module, live database, and middleware integration tests |
| `examples` | Runnable usage examples |

`sqm-playground-web` is the frontend project; the Maven reactor module list is in
[pom.xml](https://github.com/icher-g/sqm/blob/main/pom.xml).

## Model and dialect boundaries

SQM defines a rich, type-safe model (AST) to represent SQL statements internally.
This model is shared across DSL, parser, renderer, validator, transform/rewrite, middleware, JSON, and codegen modules.

Model presence and dialect support are intentionally different concepts:

- a node in `sqm-core` is representable by the framework
- parser, renderer, validation, and transpilation support remain dialect-specific
- support details and ambiguous node notes live in `docs/model/MODEL.md`

Multi-statement SQL text is represented by `StatementSequence`. Parsers ignore
empty statements, renderers emit semicolon-separated SQL for real scripts, and
validation/transpilation/middleware process the sequence as an all-or-nothing
unit. DDL remains out of scope unless it is introduced by a separate design
decision.

➡️ [View the full hierarchy in docs/model/MODEL.md](https://github.com/icher-g/sqm/blob/main/docs/model/MODEL.md)

## Example use cases

- Building complex SQL dynamically in backend applications
- Converting SQL text into structured form for static analysis or auditing
- Generating dialect-specific SQL (PostgreSQL, MySQL, SQL Server, etc.)
- Visual query builders or query explorers
- Integrating with DSL or JSON-based query definitions

See [Core Model](Core-Model) and [Getting Started](Getting-Started).
