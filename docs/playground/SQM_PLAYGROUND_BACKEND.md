# SQM Playground Backend Design

## Purpose

This document defines a dedicated backend for the SQM playground.
It should expose a small public REST API focused on exploration, not a general-purpose production gateway.

The backend should reuse existing SQM modules directly.
It should remain separate from the existing `sqm-middleware-rest` transport because the playground use case is different:

- simpler request model
- simpler response model
- public browser client
- no middleware policy workflow in V1

## Backend Goals

- provide a small, clear REST API
- keep service methods easy to trace from controller to SQM call
- return structured diagnostics
- stay stateless
- be deployable on a low-cost host

## Recommended Stack

- Java 21
- Spring Boot
- Jackson
- JUnit 5
- existing SQM modules

## Recommended Module Shape

### `sqm-playground-api`

Purpose:

- request/response DTOs
- shared enums
- diagnostic DTOs

This module should have no Spring dependency.

### `sqm-playground-rest`

Purpose:

- Spring Boot application
- controllers
- service layer
- error mapping
- configuration

This module depends on:

- `sqm-playground-api`
- relevant SQM modules

## High-Level Flow

```text
HTTP Request
  -> Controller
  -> PlaygroundService
  -> SQM parser / renderer / validator / transpiler
  -> DTO mapper
  -> HTTP Response
```

Keep the service methods small and operation-specific.

## Endpoint Design

The shared contract is defined in [SQM Playground Architecture](./SQM_PLAYGROUND_ARCHITECTURE.md).
This backend should implement:

- `GET /api/v1/health`
- `GET /api/v1/examples`
- `POST /api/v1/parse`
- `POST /api/v1/render`
- `POST /api/v1/validate`
- `POST /api/v1/transpile`

## Suggested Package Structure

For `sqm-playground-rest`:

```text
io.sqm.playground.rest
  config
  controller
  error
  example
  service
  support
```

Initial classes:

- `SqmPlaygroundRestApplication`
- `PlaygroundController`
- `HealthController`
- `PlaygroundService`
- `ExampleCatalog`
- `RestExceptionHandler`

## Service Responsibilities

### Controller Layer

Responsibilities:

- accept HTTP requests
- delegate to service
- avoid business logic

### Service Layer

Responsibilities:

- resolve dialects
- call SQM APIs
- map SQM nodes into browser-friendly AST tree DTOs
- measure duration
- produce structured responses

### Example Catalog

Responsibilities:

- return built-in example definitions
- keep example data out of controllers

Start with in-memory examples in code.
Move to resource files only if the list becomes large.

## Diagnostics Design

Diagnostics are a first-class response element.
They should avoid leaking implementation details while still being useful.

Suggested fields:

- `severity`
- `phase`
- `code`
- `message`
- `line`
- `column`

Suggested phases:

- `parse`
- `render`
- `validate`
- `transpile`
- `http`

## Error Handling Rules

- Domain failures should map to `200` with `success=false` when the request is syntactically valid but the SQL operation failed in a normal, expected way.
- Transport failures should map to `4xx` or `5xx`.
- Never return stack traces to the browser.

Examples:

- invalid JSON body -> `400`
- request too large -> `413`
- unsupported HTTP method -> framework default
- parse failure -> `200` with diagnostics
- unsupported transpilation -> `200` with diagnostics

## AST Output Design

The backend should expose both:

- serialized SQM JSON
- AST tree DTO

The AST tree DTO should represent SQM structure in a form the frontend can render directly.
It should be close to the real SQM node hierarchy, but it should not leak Java object identity or framework internals that are not useful in the browser.

Suggested DTO shape:

```json
{
  "nodeType": "SelectQuery",
  "nodeInterface": "io.sqm.core.Query",
  "kind": "select",
  "category": "statement",
  "label": "SelectQuery",
  "details": [
    {
      "name": "interfaceSimpleName",
      "value": "Query"
    }
  ],
  "children": [
    {
      "slot": "selectItems",
      "multiple": true,
      "nodes": []
    }
  ]
}
```

### Why Not Return Raw Java Interfaces

The browser cannot consume Java interfaces directly.
Also, the exact Java implementation shape is not a stable HTTP contract.

The right design is:

- keep SQM interfaces as the backend domain model
- map them to a dedicated AST response DTO
- optionally include interface names in metadata so the user can still learn the model shape

### AST Mapper Inputs

The mapper should derive AST metadata from:

- runtime SQM node type
- `Node.getTopLevelInterface()` for interface identity
- known child-bearing properties for each node family
- Jackson `kind` names where available through `sqm-json` conventions

### AST Mapper Output Rules

Each AST node should include:

- `nodeType`
- `nodeInterface`
- `kind`
- `category`
- `label`
- `details`
- `children`

Each child group should be emitted as a named slot with ordered nodes.
Do not flatten child properties into an unlabeled list.

### Suggested AST Mapper Rules

- use semantic node type names as the primary label
- use `getTopLevelInterface()` to represent the real SQM interface family shown to users
- align `kind` with `sqm-json` discriminators when available
- group children by named slots such as `from`, `where`, `selectItems`, and `joins`
- preserve child order
- include only useful scalar details
- avoid giant repeated payloads for derived or redundant fields
- keep null handling explicit

### Suggested Category Mapping

The mapper should classify nodes into coarse categories so the UI can style and filter them consistently.

Initial categories:

- `statement`
- `query`
- `expression`
- `predicate`
- `selectItem`
- `resultItem`
- `fromItem`
- `tableRef`
- `join`
- `grouping`
- `ordering`
- `hint`
- `cte`
- `window`
- `other`

### AST Mapper Delivery Strategy

Implement AST mapping incrementally.

Suggested first slice:

- support root statement node
- support common query nodes
- support expressions used in select and where clauses
- support common collection slots such as `selectItems`, `from`, `joins`, `where`, `orderBy`

If a node family is not yet richly mapped, the mapper should still emit a node with basic metadata and an empty child list rather than failing the whole request.

## Security And Cost Controls

V1 should be defensive but simple.

### Required Controls

- CORS allowlist for the frontend host
- request-size limit
- conservative rate limiting
- no authentication in V1

### Recommended Initial Limits

- max body size: small, enough for playground SQL only
- rate limit: 20 requests/minute per IP
- timeout: short per request

### Environment Variables

The backend should be configurable with environment variables for hosted deployment.

Recommended variables:

- `SQM_PLAYGROUND_ALLOWED_ORIGINS`
- `SQM_PLAYGROUND_RATE_LIMIT_ENABLED`
- `SQM_PLAYGROUND_REQUESTS_PER_WINDOW`
- `SQM_PLAYGROUND_WINDOW_SECONDS`
- `SQM_PLAYGROUND_MAX_REQUEST_BYTES`
- `SQM_PLAYGROUND_TRUST_PROXY_HEADERS`
- `SQM_PLAYGROUND_CLIENT_IP_HEADER`

### Deployment Safety Modes

#### Mode A: Local Only

- backend runs only on the developer machine
- no public hosting cost

#### Mode B: Private Demo

- frontend hosted
- backend runs locally when needed

This can be done later with a temporary tunnel, but do not design V1 around the tunnel.

#### Mode C: Public Playground

- backend hosted publicly
- strict rate limiting enabled
- low memory and CPU sizing

## Reuse From Existing Repo Modules

The backend should reuse:

- parser modules for dialect parsing
- render modules for dialect rendering
- validation modules for validation
- `sqm-json` for JSON serialization of SQM model
- `sqm-transpile` for dialect conversion

The backend should learn from `sqm-middleware-rest` for:

- Spring Boot configuration style
- error handling structure
- health endpoint patterns
- request-size and rate-limit patterns

The backend should not reuse the middleware request DTOs because the playground API is conceptually different.

## Learning-Oriented Implementation Rules

- one endpoint at a time
- one service method per endpoint
- one test class per endpoint family
- avoid generic executor frameworks until repetition is obvious
- prefer direct code paths over abstraction layers

## Backend Iteration Plan

### BE-01: Spring Boot App Shell

As a developer, I want a minimal Spring Boot app so that I can run the backend locally and understand the host lifecycle.

Acceptance:

- app starts
- `/api/v1/health` returns success

### BE-02: Example Endpoint

As a user, I want built-in example SQL so that I can explore the playground without writing queries from scratch.

Acceptance:

- `/api/v1/examples` returns a small fixed list
- tests cover response shape

### BE-03: Parse Endpoint

As a user, I want to parse SQL into SQM JSON so that I can inspect the structured model.

Acceptance:

- `/api/v1/parse` accepts SQL and dialect
- successful parse returns SQM JSON
- successful parse returns AST tree DTO
- parse errors return structured diagnostics

### BE-04: Render Endpoint

As a user, I want to render SQL for a chosen dialect so that I can compare output syntax.

Acceptance:

- `/api/v1/render` accepts source and target dialects
- rendered SQL is returned
- unsupported render paths return diagnostics

### BE-05: Validate Endpoint

As a user, I want validation feedback so that I can see whether my SQL fits the selected dialect.

Acceptance:

- `/api/v1/validate` returns valid or invalid state
- diagnostics are returned consistently

### BE-06: Transpile Endpoint

As a user, I want transpilation results so that I can learn what SQM can convert between dialects.

Acceptance:

- `/api/v1/transpile` returns outcome classification
- exact, approximate, and unsupported cases are exposed

### BE-07: Abuse Protection

As an operator, I want basic request protection so that the public service is less likely to be abused.

Acceptance:

- request-size limit enabled
- rate limiter enabled
- tests cover both

### BE-08: Deployment Profile

As a developer, I want a simple deployment profile so that I can learn how the backend behaves in hosted environments.

Acceptance:

- environment-based CORS configuration
- environment-based frontend origin configuration
- production-safe defaults documented

## Backend Test Strategy

Start with controller and service tests.

Suggested layers:

- service tests for success and failure mapping
- controller tests for HTTP contracts
- small integration test for application startup and health

Do not start with full hosted-environment tests.

## API Evolution Rules

- keep DTOs explicit
- version endpoints under `/api/v1`
- add fields in a backward-compatible way
- do not rename dialect IDs casually

## Open Questions For Later

- whether to add catalog-backed validation in the playground
- whether to let users share playground state by URL only or also by short links
- whether to expose SQM tree summaries in addition to raw JSON

These are intentionally not part of the first delivery slices.
