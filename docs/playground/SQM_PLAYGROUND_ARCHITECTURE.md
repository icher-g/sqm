# SQM Playground Architecture

## Purpose

This document defines a small public web application that lets users experiment with SQM in the browser.
The design optimizes for:

- low hosting cost
- simple deployment
- small implementation steps
- educational readability over maximal abstraction
- reuse of existing SQM modules where that reuse is clear

This playground is intentionally separate from the existing middleware decision-engine product.
It should reuse SQM parser, renderer, validation, JSON, and transpilation modules directly, but it should not force playground concerns into the middleware API.

Related documents:

- [Frontend Design](./SQM_PLAYGROUND_FRONTEND.md)
- [Backend Design](./SQM_PLAYGROUND_BACKEND.md)

## Product Goals

- Let users enter SQL and see what SQM does with it.
- Demonstrate SQM parser, renderer, validation, JSON, and transpilation capabilities.
- Provide a compact learning project for frontend and backend development.
- Keep the first hosted version stateless and cheap to operate.

## Non-Goals For V1

- user accounts
- persistent saved projects
- collaborative editing
- executing SQL against live user databases
- DDL support
- exposing the full middleware policy/enforcement product

## Recommended Technical Direction

### Frontend

- React
- TypeScript
- Vite
- Monaco Editor
- Hosted as a static site on Cloudflare Pages

### Backend

- Spring Boot
- Jackson
- Existing SQM parser, renderer, validation, transpilation, and JSON modules
- Hosted only after local-first development is comfortable

Recommended deployment progression:

1. local frontend + local backend
2. hosted frontend + local backend for private demos
3. hosted frontend + hosted backend once usage and cost controls are understood

## Proposed Repository Shape

The docs in this folder do not force implementation naming, but the cleanest future shape is:

- `sqm-playground-api`
  - request/response DTOs shared by backend tests and documented contracts
- `sqm-playground-rest`
  - Spring Boot REST host for the playground
- `sqm-playground-web`
  - frontend application

This keeps the playground separate from `sqm-middleware-*` while still allowing shared utility extraction later if needed.

## Core User Journeys

### Journey 1: Parse SQL

The user:

- selects an input dialect
- pastes SQL
- clicks `Parse`

The system returns:

- parse success or failure
- normalized SQM JSON
- AST tree data suitable for an interface-oriented viewer
- optional summary metadata such as statement type

### Journey 2: Render SQL

The user:

- parses SQL
- selects an output dialect
- clicks `Render`

The system returns:

- rendered SQL
- warnings when the target dialect changes semantics or unsupported features are encountered

### Journey 3: Validate SQL

The user:

- provides SQL and dialect
- clicks `Validate`

The system returns:

- validation success or failure
- structured diagnostics

Catalog-backed validation can be introduced later. V1 should focus on syntax and feature validation.

### Journey 4: Transpile SQL

The user:

- chooses source and target dialects
- enters SQL
- clicks `Transpile`

The system returns:

- transpiled SQL when possible
- exact, approximate, or unsupported outcome
- diagnostic messages

### Journey 5: Explore Examples

The user:

- chooses an example query
- loads it into the editor
- experiments with parse, render, validate, and transpile flows

This is important for learning and for first-time visitors.

## Shared UI/Backend Contract

The frontend should depend on simple, operation-specific endpoints rather than a single overloaded endpoint.

For parse-oriented operations, the backend should support two complementary result representations:

- raw JSON for users who want a transport-friendly serialized model
- AST tree data for users who want to explore node types and structure as a tree

The UI should expose both, preferably as tabs or a representation selector.

### Dialect IDs

Use stable string identifiers that match existing SQM dialect naming as closely as practical:

- `ansi`
- `postgresql`
- `mysql`
- `sqlserver`

### Shared Response Rules

All operation responses should include:

- `requestId`
- `success`
- `durationMs`
- `diagnostics`

Suggested diagnostic shape:

```json
{
  "severity": "error",
  "code": "PARSER_UNEXPECTED_TOKEN",
  "message": "Expected FROM but found WHERE",
  "line": 1,
  "column": 15,
  "phase": "parse"
}
```

### Endpoints

#### `POST /sqm/playground/api/v1/parse`

Request:

```json
{
  "sql": "select * from customer",
  "dialect": "postgresql"
}
```

Response:

```json
{
  "requestId": "req-123",
  "success": true,
  "durationMs": 12,
  "statementKind": "query",
  "sqmJson": "{ ... }",
  "ast": {
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
        "nodes": [
          {
            "nodeType": "ExprSelectItem",
            "nodeInterface": "io.sqm.core.SelectItem",
            "kind": "exprSelectItem",
            "category": "selectItem",
            "label": "ExprSelectItem",
            "details": [],
            "children": []
          }
        ]
      }
    ]
  },
  "summary": {
    "rootNodeType": "SelectQuery",
    "rootInterface": "io.sqm.core.Query"
  },
  "diagnostics": []
}
```

#### `POST /sqm/playground/api/v1/render`

Request:

```json
{
  "sql": "select * from customer",
  "sourceDialect": "postgresql",
  "targetDialect": "mysql"
}
```

Response:

```json
{
  "requestId": "req-124",
  "success": true,
  "durationMs": 15,
  "renderedSql": "SELECT * FROM customer",
  "diagnostics": []
}
```

#### `POST /sqm/playground/api/v1/validate`

Request:

```json
{
  "sql": "select * from customer",
  "dialect": "postgresql"
}
```

Response:

```json
{
  "requestId": "req-125",
  "success": true,
  "durationMs": 10,
  "valid": true,
  "diagnostics": []
}
```

#### `POST /sqm/playground/api/v1/transpile`

Request:

```json
{
  "sql": "select * from customer limit 5",
  "sourceDialect": "postgresql",
  "targetDialect": "sqlserver"
}
```

Response:

```json
{
  "requestId": "req-126",
  "success": true,
  "durationMs": 18,
  "outcome": "exact",
  "renderedSql": "SELECT TOP 5 * FROM customer",
  "diagnostics": []
}
```

#### `GET /sqm/playground/api/v1/examples`

Response:

```json
{
  "requestId": "req-127",
  "success": true,
  "durationMs": 2,
  "examples": [
    {
      "id": "basic-select",
      "title": "Basic SELECT",
      "dialect": "ansi",
      "sql": "select id, name from customer"
    }
  ],
  "diagnostics": []
}
```

## Cross-Cutting Decisions

### Stateless Backend

V1 should be stateless.
Each request contains the SQL text and selected dialects.
This keeps hosting simpler and avoids database work.

### No Authentication In V1

If the site is public, rely on:

- CORS restriction to the playground frontend origin
- request-size limits
- low rate limits
- basic request logging

### Error Contract

Prefer structured diagnostics over raw stack traces.
Frontend users should always get a readable message plus location information when available.

### AST Representation Contract

The playground should not try to send Java interface objects directly to the browser.
Instead, it should expose an AST-view model derived from SQM nodes.

The AST view model should be anchored to existing SQM concepts:

- `Node` is the shared root
- interface identity should come from `Node.getTopLevelInterface()`
- `kind` should align with the Jackson `kind` discriminator used by `sqm-json` where practical
- `category` should reflect the main SQM family such as `statement`, `expression`, `predicate`, or `fromItem`

Suggested AST node shape:

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

This gives users something close to the real node structure while staying stable and browser-friendly.
It also avoids forcing the frontend to infer structure from arbitrary JSON fields.

### AST Node DTO

Recommended fields:

- `nodeType`
  - simple Java type name such as `SelectQuery` or `ColumnExpr`
- `nodeInterface`
  - fully qualified top-level SQM interface name
- `kind`
  - JSON discriminator name when available, such as `select`, `insert`, or `orderBy`
- `category`
  - high-level SQM family, such as `statement`, `expression`, `predicate`, `fromItem`, `tableRef`, `resultItem`
- `label`
  - short UI label, usually the same as `nodeType`
- `details`
  - scalar metadata entries helpful to users
- `children`
  - explicit child slots preserving source or model order

### AST Child Slot DTO

Recommended fields:

- `slot`
  - semantic property name such as `from`, `where`, `selectItems`, `joins`, `orderBy`
- `multiple`
  - whether the slot is a collection
- `nodes`
  - ordered child AST nodes

This slot-oriented shape is preferable to a flat child list because SQM nodes are easier to understand when parent-child relationships retain property names.

### Rate Limiting

Start conservatively because public playground traffic is bursty.

Initial suggestion:

- 20 requests/minute per IP
- small maximum request body
- no separate batch endpoints in V1; existing parse, render, validate, and
  transpile endpoints accept semicolon-separated scripts and return one combined
  payload. The frontend derives per-statement AST/JSON/DSL views from that
  payload instead of receiving duplicate whole-script and per-statement data.

### Observability

V1 only needs:

- request logs
- latency timing
- error counts by endpoint

Avoid adding tracing or external telemetry before it is clearly needed.

## Learning-Oriented Delivery Strategy

Implementation should happen in very small slices.
Each slice should produce a visible result in the browser or in a backend test.

### Phase 0: Foundation

- create the docs
- create the frontend app shell
- create the backend app shell
- create a tiny shared contract for one endpoint

### Phase 1: First Vertical Slice

- hard-coded example list in backend
- frontend fetches example list
- user can load an example into the editor

### Phase 2: Parse Slice

- implement `/parse`
- show SQM JSON in a result panel
- show AST tree in a second parse-oriented result panel
- add parse error rendering

### Phase 3: Render Slice

- implement `/render`
- show rendered SQL in a second panel

### Phase 4: Validate Slice

- implement `/validate`
- show diagnostics panel

### Phase 5: Transpile Slice

- implement `/transpile`
- show outcome classification and warnings

## Story Sizing Rule

User stories for this playground should usually fit one of these sizes:

- UI-only story
- one backend endpoint story
- one end-to-end vertical slice story
- one cleanup or refactor story

Avoid stories that combine multiple new operations.

## Acceptance Principles

- each story should be runnable locally
- each story should add or update tests
- each story should keep the UI understandable to a beginner
- each story should prefer explicit code over clever abstraction
