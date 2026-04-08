# SQM Playground Frontend Design

## Purpose

This document defines the frontend for the SQM playground.
The frontend should be easy to read, easy to extend, and implemented in small steps suitable for learning.

The frontend is a client for the shared contracts defined in [SQM Playground Architecture](./SQM_PLAYGROUND_ARCHITECTURE.md).

## Frontend Goals

- provide a pleasant SQL playground experience
- keep the component tree simple
- make network interactions easy to trace
- favor explicit state transitions over magic
- keep the first version static-host friendly

## Recommended Stack

- React
- TypeScript
- Vite
- Monaco Editor
- plain CSS or a very small styling layer

Recommended hosting:

- Cloudflare Pages

Recommended deployment model:

- static build
- backend URL configured through environment variables

## Design Principles

- Start with a single page.
- Avoid introducing routing until a second real page exists.
- Keep server state and local UI state clearly separated.
- Prefer composition over custom state frameworks.
- Keep each component short enough to read in one sitting.

## Page Layout

V1 should be a single-screen workstation with three regions:

1. top control bar
2. main editor column
3. results column

### Top Control Bar

Controls:

- input dialect select
- output dialect select
- operation buttons: `Parse`, `Render`, `Validate`, `Transpile`
- example picker
- reset button

### Main Editor Column

Contains:

- Monaco SQL editor
- optional small helper text

### Results Column

Tabbed or stacked panels:

- `AST`
- `JSON`
- `Rendered SQL`
- `Diagnostics`
- `About Result`

For the very first slice, stacked panels are simpler than tabs.

## Proposed Component Structure

Initial structure:

- `App`
- `PlaygroundPage`
- `ControlBar`
- `SqlEditorPanel`
- `ResultsPanel`
- `AstTreeCard`
- `JsonResultCard`
- `SqlResultCard`
- `DiagnosticsList`
- `ExampleSelector`

Do not add deep component nesting early.
It is better to keep a little state in `PlaygroundPage` first and extract later only when patterns are obvious.

## Frontend State Model

Keep state small and explicit.

### Local UI State

- `sqlText`
- `sourceDialect`
- `targetDialect`
- `selectedExampleId`
- `activeOperation`

### Remote Operation State

- `isLoading`
- `lastResponse`
- `lastError`

If you use TanStack Query later, add it after the first manual fetch implementation is understood.
For learning, the first few stories should use direct `fetch` wrappers.

## API Client Structure

Use one small API module:

- `src/api/playgroundApi.ts`

Initial functions:

- `fetchExamples()`
- `parseSql(request)`
- `renderSql(request)`
- `validateSql(request)`
- `transpileSql(request)`

Keep request and response TypeScript types near these functions.
Do not scatter ad hoc fetch calls through components.

## Styling Direction

Use an intentional but simple visual language.

Suggested direction:

- warm light background
- strong editor surface contrast
- monospace for SQL and JSON
- one accent color for actions
- clear status colors for success, warning, error

Avoid heavyweight design systems in the first learning iteration.

## Error Handling

The UI should distinguish:

- network failure
- backend validation/parse failure
- successful response with warnings

Display diagnostics as structured items, not raw JSON blobs.

Each diagnostic row should aim to show:

- severity
- phase
- message
- line and column if available

## AST Presentation

The parse result should support two representations:

- `AST` view for structural exploration
- `JSON` view for serialized model inspection

The `AST` view is the better primary learning tool because it teaches users how SQM is shaped.
The `JSON` view is still valuable because it is precise, copyable, and useful for debugging.

### Recommended AST UI

Show a collapsible tree with:

- node type as the main label
- top-level SQM interface as secondary metadata
- optional `kind` and `category` in a detail row
- child collections rendered in source order

Suggested initial behavior:

- root expanded by default
- one-click expand/collapse
- selecting a node shows details in a small inspector panel

For V1, keep the AST tree read-only.
Do not add bidirectional editor synchronization yet.

### Recommended AST Inspector Fields

When a tree node is selected, show:

- `nodeType`
- `nodeInterface`
- `kind`
- `category`
- scalar `details`
- child slot names

This keeps the tree readable while still teaching the user how the SQM model is organized.

## Accessibility Basics

V1 should include:

- label associations for selects and buttons
- keyboard reachable controls
- sufficient color contrast
- visible loading state

## Frontend Iteration Plan

The frontend should be built in very small stories.
Each story should leave the app working.

### FE-01: App Shell

As a developer, I want a minimal React app shell so that I can run the playground locally.

Acceptance:

- app starts locally
- placeholder layout renders
- no backend integration yet

### FE-02: Control Bar Skeleton

As a user, I want dialect selectors and operation buttons so that I can understand what the playground will do.

Acceptance:

- source dialect select
- target dialect select
- disabled action buttons

### FE-03: SQL Editor

As a user, I want a SQL editor with starter text so that I can experiment with input quickly.

Acceptance:

- editor renders
- SQL text is editable
- starter example loads by default

### FE-04: Results Area Skeleton

As a user, I want a results area so that I can see where outputs will appear.

Acceptance:

- AST card placeholder
- JSON card placeholder
- rendered SQL card placeholder
- diagnostics placeholder

### FE-05: Examples Fetch

As a user, I want to load example SQL from the backend so that I can start without writing queries from scratch.

Acceptance:

- examples fetched from backend
- example picker updates editor text
- loading and error states are shown

### FE-06: Parse Flow

As a user, I want to send SQL to the parse endpoint so that I can inspect SQM output.

Acceptance:

- parse button calls backend
- AST result is displayed
- JSON result is displayed
- parse diagnostics are shown

### FE-07: Render Flow

As a user, I want to render SQL for a target dialect so that I can compare dialect output.

Acceptance:

- render button calls backend
- rendered SQL result is displayed
- warnings are shown

### FE-08: Validate Flow

As a user, I want to validate SQL so that I can see whether SQM accepts it for the selected dialect.

Acceptance:

- validate button calls backend
- validation state is shown clearly
- diagnostics panel is reused

### FE-09: Transpile Flow

As a user, I want to transpile SQL across dialects so that I can learn what SQM can and cannot convert.

Acceptance:

- transpile button calls backend
- outcome badge is shown
- transpiled SQL is displayed

### FE-10: UX Cleanup

As a user, I want a clearer workflow so that the tool feels easier to use.

Acceptance:

- better button disabling rules
- loading state per operation
- clearer success and failure styling

## Suggested Frontend Folder Layout

```text
src/
  api/
    playgroundApi.ts
  components/
    AstTreeCard.tsx
    ControlBar.tsx
    DiagnosticsList.tsx
    ExampleSelector.tsx
    JsonResultCard.tsx
    ResultsPanel.tsx
    SqlEditorPanel.tsx
    SqlResultCard.tsx
  pages/
    PlaygroundPage.tsx
  styles/
    app.css
  types/
    api.ts
  App.tsx
  main.tsx
```

## Testing Approach

Start small.

- unit tests for pure formatting helpers
- component tests for result rendering
- one integration-style test for a button triggering an API call

Do not start with broad end-to-end browser automation.
That can come later once the page stops changing rapidly.

## Teaching Notes

For learning value, prefer code that answers these questions clearly:

- where does the state live
- when does the network call happen
- how does data move from response to UI
- where are loading and error states handled

The AST view adds one more important learning question:

- how is a backend domain model transformed into a frontend-friendly tree view

If a pattern makes the code shorter but harder to trace, prefer the more explicit version first.
