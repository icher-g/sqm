## Epic

### Title
`Epic: SQM Playground V1 Foundation`

### Problem Statement
SQM has strong parser, renderer, validation, JSON, and transpilation capabilities, but there is no small public-facing web playground that lets users experiment with those capabilities interactively.

This makes it harder to:

- demonstrate SQM to new users
- learn how SQM behaves across dialects
- onboard contributors through a visible end-to-end project
- practice frontend and backend development around the existing SQM modules

The playground should be delivered in very small learning-friendly slices.
To keep the implementation understandable, the work should be sequenced as:

1. API contract
2. backend host
3. frontend client

### Epic Goal
Deliver the V1 foundation for an SQM playground with:

- a stable shared API contract
- a dedicated Spring Boot backend
- a simple frontend that can call the backend and present examples, AST, JSON, SQL render results, validation diagnostics, and transpilation results

### Business Value
- creates a public demo surface for SQM
- provides a practical learning project inside the repo
- improves understanding of SQM AST and dialect behavior
- creates a clean foundation for later hosted deployment

### Relationship To Other Epics

#### Depends On
- existing parser, renderer, validation, JSON, and transpilation module baselines
- existing REST host patterns in `sqm-middleware-rest`

#### Informs
- future public documentation and demos
- future hosted playground deployment work
- future AST visualization improvements

#### DDL Boundary
- DDL remains out of scope for playground V1.

### Scope Boundaries

#### Query
- In scope.

#### DML
- In scope where already supported by SQM and practical for the playground.

#### DDL
- Out of scope.

### Non-Goals
- authentication
- saved user projects
- database execution against user-provided connections
- collaborative editing
- broad middleware policy workflows
- DDL support

### Definition of Done
- shared playground API contracts are documented and implemented
- backend endpoints exist for health, examples, parse, render, validate, and transpile
- parse responses expose both SQM JSON and AST tree DTOs
- frontend can call the backend and display results clearly
- each story is implemented with tests appropriate to its layer
- docs stay aligned with the delivered playground behavior

### Suggested Labels
`epic`, `playground`, `api`, `backend`, `frontend`, `docs`

---

## Scope Summary

This epic is intentionally ordered to support small-step learning:

1. API stories define the contract first
2. backend stories implement the contract
3. frontend stories consume the contract

The API-first sequence reduces churn and makes each later layer easier to reason about.

---

## Design Overview

### 1. Delivery Strategy

The playground should be built as small vertical slices, but with the implementation order constrained by learning goals:

1. define API DTOs and contracts
2. build backend endpoints against those contracts
3. build frontend UI against those contracts

### 2. Module Direction

The intended playground module shape is:

- `sqm-playground-api`
- `sqm-playground-rest`
- `sqm-playground-web`

### 3. AST Rule

The parse contract should expose:

- serialized SQM JSON
- AST tree DTO

The AST tree DTO should stay aligned with:

- `Node.getTopLevelInterface()`
- `sqm-json` `kind` values where practical
- named child slots such as `selectItems`, `from`, `where`, and `joins`

### 4. Story Completion Rule

No story is complete unless it keeps the code readable for a learner.

Each story should:

- be small
- keep the app runnable
- add tests
- avoid premature abstraction

---

## User Stories

### Story API-01

#### Title
`Story: Define playground dialect and common response contracts`

#### User Story
As a developer, I want explicit shared DTOs for dialect identifiers and common response metadata so that backend and frontend can evolve against the same stable contract.

#### Acceptance Criteria
- create the `sqm-playground-api` module
- define shared dialect identifiers for `ansi`, `postgresql`, `mysql`, and `sqlserver`
- define common response fields such as `requestId`, `success`, `durationMs`, and `diagnostics`
- define a diagnostic DTO with severity, code, message, phase, line, and column fields
- add unit tests for DTO serialization where useful
- update playground docs if field names change from the current design docs

#### Labels
`story`, `playground`, `api`

#### Depends On
- epic foundation only

---

### Story API-02

#### Title
`Story: Define parse, render, validate, transpile, and examples DTOs`

#### User Story
As a developer, I want explicit request and response DTOs for each playground operation so that the backend implementation and frontend client have a clear contract.

#### Acceptance Criteria
- define request and response DTOs for `examples`, `parse`, `render`, `validate`, and `transpile`
- keep operation-specific responses separate rather than using one overloaded payload
- parse response DTO includes both `sqmJson` and `ast`
- summary metadata includes root node type and root interface where applicable
- add unit tests for representative serialization shapes
- document the endpoint payloads in the playground docs

#### Labels
`story`, `playground`, `api`

#### Depends On
- `Story API-01`

---

### Story API-03

#### Title
`Story: Define AST tree DTO contract for the playground`

#### User Story
As a developer, I want a dedicated AST tree DTO contract so that the backend can expose real SQM structure in a browser-friendly shape and the frontend can render it without guessing.

#### Acceptance Criteria
- define AST node DTO fields for `nodeType`, `nodeInterface`, `kind`, `category`, `label`, `details`, and `children`
- define child slot DTO fields for `slot`, `multiple`, and ordered `nodes`
- document the intended mapping to `Node.getTopLevelInterface()` and `sqm-json` `kind`
- define category values for the first supported node families
- add serialization tests for a representative AST payload
- keep the DTO contract independent from Spring

#### Labels
`story`, `playground`, `api`, `ast`

#### Depends On
- `Story API-01`

---

### Story BE-01

#### Title
`Story: Create the playground Spring Boot host with health endpoint`

#### User Story
As a developer, I want a minimal playground REST host so that I can run the backend locally and understand the application structure before adding SQM operations.

#### Acceptance Criteria
- create the `sqm-playground-rest` module
- application starts locally
- `GET /sqm/playground/api/v1/health` returns a successful response using the shared API contract where appropriate
- add startup and health endpoint tests
- include JavaDoc for new public types and methods

#### Labels
`story`, `playground`, `backend`

#### Depends On
- `Story API-01`

---

### Story BE-02

#### Title
`Story: Add the playground examples endpoint`

#### User Story
As a user, I want a small catalog of built-in SQL examples so that I can explore the playground without writing every query from scratch.

#### Acceptance Criteria
- implement `GET /sqm/playground/api/v1/examples`
- return a small fixed in-memory example list
- examples include id, title, dialect, and SQL text
- add controller and service tests
- keep example data out of the controller class

#### Labels
`story`, `playground`, `backend`

#### Depends On
- `Story API-02`
- `Story BE-01`

---

### Story BE-03

#### Title
`Story: Implement the playground parse endpoint with SQM JSON output`

#### User Story
As a user, I want to parse SQL and receive SQM JSON so that I can inspect the serialized model produced by SQM.

#### Acceptance Criteria
- implement `POST /sqm/playground/api/v1/parse`
- parse request accepts SQL and source dialect
- successful parse returns SQM JSON and summary metadata
- parse failure returns structured diagnostics rather than raw exceptions
- add service and controller tests for success and failure paths

#### Labels
`story`, `playground`, `backend`, `parser`, `json`

#### Depends On
- `Story API-02`
- `Story BE-01`

---

### Story BE-04

#### Title
`Story: Add AST tree mapping to the parse endpoint`

#### User Story
As a user, I want parse results to include a navigable AST tree so that I can understand SQM structure through node interfaces and child slots rather than JSON alone.

#### Acceptance Criteria
- implement AST mapping for the parse response
- AST root includes `nodeType`, `nodeInterface`, `kind`, `category`, `label`, `details`, and `children`
- mapper uses `Node.getTopLevelInterface()` for interface identity
- mapper preserves named child slots for common query structures
- if a node family is not richly mapped yet, the mapper still returns basic node metadata instead of failing
- add unit tests for representative AST mapping output

#### Labels
`story`, `playground`, `backend`, `ast`

#### Depends On
- `Story API-03`
- `Story BE-03`

---

### Story BE-05

#### Title
`Story: Implement render and validate playground endpoints`

#### User Story
As a user, I want render and validate endpoints so that I can compare SQL output across dialects and inspect validation diagnostics from the backend.

#### Acceptance Criteria
- implement `POST /sqm/playground/api/v1/render`
- implement `POST /sqm/playground/api/v1/validate`
- render endpoint returns rendered SQL or diagnostics
- validate endpoint returns validity and diagnostics
- add service and controller tests for both endpoints
- keep service methods operation-specific and easy to trace

#### Labels
`story`, `playground`, `backend`, `renderer`, `validation`

#### Depends On
- `Story API-02`
- `Story BE-01`

---

### Story BE-06

#### Title
`Story: Implement the playground transpile endpoint`

#### User Story
As a user, I want to transpile SQL across dialects so that I can see exact, approximate, and unsupported conversion outcomes.

#### Acceptance Criteria
- implement `POST /sqm/playground/api/v1/transpile`
- response exposes outcome classification and rendered SQL where applicable
- unsupported and approximate outcomes are returned through diagnostics and explicit outcome fields
- add service and controller tests for success and unsupported cases

#### Labels
`story`, `playground`, `backend`, `transpile`

#### Depends On
- `Story API-02`
- `Story BE-01`

---

### Story BE-07

#### Title
`Story: Add basic playground abuse protection and deployment configuration`

#### User Story
As an operator, I want basic request protection and explicit environment configuration so that the public playground is safer and easier to host cheaply.

#### Acceptance Criteria
- add request-size limiting
- add conservative rate limiting
- add configurable CORS allowlist for the frontend origin
- document the required environment variables
- add tests for abuse-protection behavior

#### Labels
`story`, `playground`, `backend`, `ops`

#### Depends On
- `Story BE-02`
- `Story BE-03`
- `Story BE-05`

---

### Story FE-01

#### Title
`Story: Create the playground frontend app shell`

#### User Story
As a developer, I want a minimal frontend shell so that I can run the playground locally and understand the page structure before wiring backend operations.

#### Acceptance Criteria
- create the `sqm-playground-web` app
- app starts locally
- page renders a placeholder layout for controls, editor, and results
- no backend integration is required yet

#### Labels
`story`, `playground`, `frontend`

#### Depends On
- `Story API-01`

---

### Story FE-02

#### Title
`Story: Add the SQL editor and control bar skeleton`

#### User Story
As a user, I want an editor and operation controls so that I can see how the playground workflow is organized.

#### Acceptance Criteria
- add SQL editor with starter text
- add source and target dialect selectors
- add operation buttons for parse, render, validate, and transpile
- add a results area skeleton including AST, JSON, rendered SQL, and diagnostics placeholders

#### Labels
`story`, `playground`, `frontend`

#### Depends On
- `Story FE-01`

---

### Story FE-03

#### Title
`Story: Load built-in examples from the backend into the editor`

#### User Story
As a user, I want to load built-in examples from the backend so that I can start experimenting quickly.

#### Acceptance Criteria
- call `GET /sqm/playground/api/v1/examples`
- display an example selector
- loading an example updates the editor content and dialect state when appropriate
- show loading and error UI for the examples request

#### Labels
`story`, `playground`, `frontend`

#### Depends On
- `Story API-02`
- `Story BE-02`
- `Story FE-02`

---

### Story FE-04

#### Title
`Story: Display parse results with JSON and AST tabs`

#### User Story
As a user, I want parse results to appear in both JSON and AST views so that I can learn SQM from both serialized and structural perspectives.

#### Acceptance Criteria
- call `POST /sqm/playground/api/v1/parse`
- show JSON results in a dedicated panel
- show AST results in a dedicated panel or tab
- AST tree supports expand and collapse
- selecting an AST node shows node metadata such as interface, kind, and category
- parse diagnostics are shown clearly

#### Labels
`story`, `playground`, `frontend`, `ast`

#### Depends On
- `Story API-03`
- `Story BE-04`
- `Story FE-02`

---

### Story FE-05

#### Title
`Story: Display render and validate results in the frontend`

#### User Story
As a user, I want render and validate results in the UI so that I can compare generated SQL and understand validation feedback.

#### Acceptance Criteria
- call `POST /sqm/playground/api/v1/render`
- call `POST /sqm/playground/api/v1/validate`
- rendered SQL is displayed in a readable result panel
- validation state and diagnostics are displayed clearly
- loading and failure states are handled for both operations

#### Labels
`story`, `playground`, `frontend`

#### Depends On
- `Story BE-05`
- `Story FE-02`

---

### Story FE-06

#### Title
`Story: Display transpilation results in the frontend`

#### User Story
As a user, I want transpilation results in the UI so that I can explore cross-dialect conversion outcomes visually.

#### Acceptance Criteria
- call `POST /sqm/playground/api/v1/transpile`
- display transpiled SQL when available
- display exact, approximate, or unsupported outcome state
- diagnostics and warnings are visible in the shared diagnostics area

#### Labels
`story`, `playground`, `frontend`, `transpile`

#### Depends On
- `Story BE-06`
- `Story FE-02`

---

### Story FE-07

#### Title
`Story: Refine playground UX for learning and hosted use`

#### User Story
As a user, I want a clearer and safer interface so that the playground is easier to learn from and more predictable when hosted.

#### Acceptance Criteria
- improve button disabling and loading behavior
- improve empty-state and failure-state messaging
- expose backend request id in a low-noise way for debugging
- keep the UI keyboard reachable and visually clear
- update frontend docs if the workflow changes

#### Labels
`story`, `playground`, `frontend`, `ux`

#### Depends On
- `Story FE-03`
- `Story FE-04`
- `Story FE-05`
- `Story FE-06`

