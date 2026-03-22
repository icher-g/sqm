## Epic

### Title
`Epic: R7 Typed Hint Modeling`

### Problem Statement
SQM currently supports several hint-like constructs, but most of the model surface is still too string-oriented or too syntax-shaped for a framework whose main purpose is SQL query manipulation.

Today this leads to a few recurring problems:

- hint semantics are not represented structurally in `sqm-core`
- transformations cannot reason safely about hint kind, arguments, or equivalence
- validation can only perform coarse, dialect-specific checks
- transpilation cannot distinguish exact, approximate, and unsupported hint behavior reliably
- codegen and DSL APIs preserve text more than meaning

Examples of the current weakness:

```java
select(...)
    .optimizerHint("MAX_EXECUTION_TIME(1000)")
```

```java
update(...)
    .optimizerHint("INDEX(users idx_users_name)")
```

These forms are easy to store, but difficult to manipulate. A transformer cannot answer questions like:

- Is this a statement hint or a table hint?
- What is the hint name?
- What are its arguments?
- Can it be merged with another hint?
- Is it exact, approximate, or unsupported in another dialect?

This is a model-design issue, not only a dialect issue.

### Epic Goal
Introduce a typed hint model in `sqm-core` and align parser, renderer, validation, transpilation, DSL, codegen, and JSON behavior around that model so hints become first-class manipulation targets instead of opaque strings.

### Business Value
- Improves SQM’s value as a SQL manipulation framework rather than only a parse/render library.
- Makes hint-aware transformations safe and explicit.
- Enables stronger dialect validation and clearer unsupported diagnostics.
- Creates a structured foundation for future dialect hint work without repeating stringly implementations.
- Improves codegen and DSL ergonomics by expressing hint meaning instead of raw SQL fragments.

### Definition of Done
- A typed hint model exists in `sqm-core`.
- Existing hint-bearing surfaces are migrated or adapted to use the typed model where appropriate.
- ANSI and dialect parsers/renderers/validators are updated for the selected first-wave hint scope.
- Transpilation rule infrastructure can inspect hint nodes structurally.
- DSL and codegen support all new public model surface.
- JSON mixins, visitors, transformers, matchers, and `MODEL.md` are updated for new nodes.
- Documentation includes examples and explicit support boundaries.

### Suggested Labels
`epic`, `model`, `hints`, `dsl`, `transpile`, `validation`

---

## Scope Boundaries

### In Scope
- Statement-level hints
- Table-level hints
- Generic typed hint abstractions in `sqm-core`
- Dialect support boundaries for hint parsing/rendering/validation
- Transpilation diagnostics based on typed hint structure
- DSL / codegen / JSON / visitor / transformer / matcher updates

### Out of Scope
- DDL hints
- Stored procedures or procedural SQL hint semantics
- Exhaustive support for every vendor hint family in the first wave
- Query planner simulation or cost-model semantics

---

## Design Goals

### 1. Model Hint Meaning, Not Only Raw Text

Hints should be represented as structured nodes so SQM can reason about them semantically.

### 2. Keep the Core Model Neutral

The core model should describe generic hint concepts, not hard-code one dialect’s textual spelling as the primary abstraction.

### 3. Preserve Dialect Ownership of Syntax

Dialects should continue to own:

- which hints are parsed
- how hints render
- which contexts support them
- whether a hint is exact / approximate / unsupported in transpilation

### 4. Support Incremental Rollout

The first iteration should provide meaningful structure without requiring SQM to pre-model every hint family exhaustively.

---

## Current Pain Points

### A. Statement Hints Are Stringly

Current-style API:

```java
select(...)
    .optimizerHint("MAX_EXECUTION_TIME(1000)")
```

Problems:

- no typed hint kind
- no structured arguments
- no reliable equality / normalization semantics beyond raw string comparison
- difficult to rewrite or validate safely

### B. Table Hints Are Unevenly Structured

Some table hints already have more structure than statement hints, but the model is still too tied to current syntax slices and closed enums.

Problems:

- difficult additive evolution
- possible future hint families may force repeated model reshaping
- mixed ergonomics between table hints and statement hints

### C. Transpilation Cannot Reason Well About Strings

If a hint is stored as raw text, transpilation must:

- reparse the text
- reject it coarsely
- or silently ignore structure it cannot inspect

That is not a strong foundation for a manipulation framework.

---

## Proposed Model

## 1. Core Abstractions

Introduce a small typed hierarchy in `sqm-core`.

Recommended shape:

- `Hint`
- `StatementHint`
- `TableHint`
- `HintArg`

Possible initial sketch:

```java
public sealed interface Hint extends Node permits StatementHint, TableHint {
    Identifier name();
    List<HintArg> args();
}
```

```java
public non-sealed interface StatementHint extends Hint {
}
```

```java
public non-sealed interface TableHint extends Hint {
}
```

`HintArg` should be typed rather than plain strings. A simple first wave can model:

- identifier argument
- literal argument
- expression argument
- qualified-name argument

This avoids overfitting too early while still giving structure.

## 2. Canonical Representation

The canonical representation should be:

- normalized hint name
- ordered typed arguments

Not:

- raw SQL fragment

### Example

Instead of:

```java
"MAX_EXECUTION_TIME(1000)"
```

Prefer:

```java
StatementHint.of(
    id("MAX_EXECUTION_TIME"),
    List.of(HintArg.literal(lit(1000)))
)
```

Instead of:

```java
"INDEX(users idx_users_name)"
```

Prefer:

```java
StatementHint.of(
    id("INDEX"),
    List.of(
        HintArg.qualifiedName(QualifiedName.of("users")),
        HintArg.identifier(id("idx_users_name"))
    )
)
```

### Why this is better

- renderers know how to print it
- validators know what it is
- transformers can rewrite arguments safely
- transpilers can classify it structurally

---

## Statement Hints vs Table Hints

The distinction should stay explicit because they differ in:

- attachment point
- rendering shape
- dialect support rules
- conflict rules

### Statement Hint Example

MySQL:

```sql
SELECT /*+ MAX_EXECUTION_TIME(1000) */ * FROM users
```

Possible SQM shape:

```java
select(...)
    .hint(statementHint("MAX_EXECUTION_TIME", lit(1000)))
```

### Table Hint Example

SQL Server:

```sql
FROM users WITH (NOLOCK, HOLDLOCK)
```

Possible SQM shape:

```java
tbl("users")
    .hint(tableHint("NOLOCK"))
    .hint(tableHint("HOLDLOCK"))
```

### Important design point

The same textual hint name in two dialects should not automatically mean the same semantic node. The node carries generic structure; the dialect decides support and meaning.

---

## Where Hints Should Attach

## Statement-Level

Applicable to:

- `SelectQuery`
- `InsertStatement`
- `UpdateStatement`
- `DeleteStatement`
- `MergeStatement`

Suggested change:

- replace or deprecate string-based hint lists
- expose `List<StatementHint>`

## Table-Level

Applicable to:

- `Table`
- and possibly future relation kinds if a dialect truly supports them

Suggested change:

- evolve current table-hint modeling toward generic `TableHint` nodes
- keep convenience DSL methods for popular dialect hints

---

## Hint Arguments

## Why typed arguments matter

These are not equivalent argument kinds:

```sql
INDEX(users idx_users_name)
```

```sql
MAX_EXECUTION_TIME(1000)
```

```sql
USE_INDEX(idx_users_name)
```

Some arguments are:

- identifiers
- qualified names
- numeric literals
- expressions

If SQM stores all of them as strings, that loses useful semantics.

## Suggested initial `HintArg` family

- `IdentifierHintArg`
- `QualifiedNameHintArg`
- `ExpressionHintArg`

This is likely enough for a first wave.

If later needed, more specialized variants can be added:

- keyword arg
- assignment-like arg
- hint-list arg

---

## Parsing Strategy

## Principle

Parsers should continue to be dialect-specific, but they should emit shared typed hint nodes.

### Example: MySQL Statement Hint

Input:

```sql
SELECT /*+ MAX_EXECUTION_TIME(1000) */ id FROM users
```

Output:

- `SelectQuery`
- statement hint list containing typed `StatementHint`

### Example: SQL Server Table Hint

Input:

```sql
FROM users WITH (NOLOCK, HOLDLOCK)
```

Output:

- `Table`
- typed `TableHint` list

## Guidance

Do not create parser-only string wrappers if the hint is in scope for manipulation.

If a hint cannot yet be represented structurally, then either:

- keep it out of the current epic scope
- or explicitly design the needed argument model

---

## Rendering Strategy

Renderers should own the syntax shape for each dialect.

The same underlying node may render differently across dialects.

### Example

A typed statement hint might render as:

- MySQL / Oracle-style comment hint syntax
- SQL Server query option syntax in a future slice
- unsupported in ANSI

### Rule

Core hint structure should not imply universal render support.

Each dialect renderer must explicitly:

- support it
- reject it
- or ignore only if that is a deliberate design decision and documented

---

## Validation Strategy

Typed hints unlock stronger validation.

## What validation can do better

- detect duplicate hints by normalized name
- detect conflicting hint combinations
- reject unsupported hint contexts
- validate argument shape and count
- validate known hint families more precisely

### Example

Instead of only seeing:

```java
"NOLOCK"
```

validation can reason over:

- hint kind: `NOLOCK`
- attachment point: table
- dialect: SQL Server
- conflicting peers present: `UPDLOCK`, `HOLDLOCK`, etc.

## Validation recommendation

Use a layered strategy:

1. generic structural validation in shared modules
2. dialect-specific compatibility validation in dialect validation modules

---

## Transpilation Strategy

This is one of the strongest reasons to introduce typed hints.

## Desired behavior

For each hint, transpilation should be able to say:

- exact
- approximate
- unsupported

### Example

Input:

```java
statementHint("MAX_EXECUTION_TIME", lit(1000))
```

Possible target results:

- exact: target dialect has equivalent typed hint
- approximate: target dialect has a near-equivalent but different semantics
- unsupported: target dialect has no safe equivalent

Without typed modeling, this is much harder.

## Recommendation

Transpile rules should inspect:

- hint type
- normalized name
- typed arguments
- attachment point

not raw strings.

---

## DSL Design

The DSL should support both:

- generic typed hint construction
- dialect-friendly convenience helpers

## Generic API

Example:

```java
statementHint("MAX_EXECUTION_TIME", lit(1000))
tableHint("NOLOCK")
```

## Dialect-Friendly Helpers

Example:

```java
maxExecutionTime(1000)
nolock()
updlock()
holdlock()
```

These helpers should just build typed hints, not bypass the model.

## Recommendation

Keep one canonical structured construction path and let convenience helpers delegate to it.

---

## Codegen Strategy

If a typed hint model is added, codegen should emit:

- generic hint constructors where possible
- dialect-friendly helpers where they materially improve readability

### Example

Preferred:

```java
select(...)
    .hint(maxExecutionTime(1000))
```

Fallback:

```java
select(...)
    .hint(statementHint("MAX_EXECUTION_TIME", lit(1000)))
```

The important point is that generated code should preserve structure, not raw string fragments.

---

## JSON / Visitor / Transformer / Matcher Requirements

Because hints become first-class nodes, the following are mandatory:

- dedicated node interfaces
- dedicated `accept()` methods
- visitor coverage
- recursive transformer coverage
- matcher coverage
- JSON mixins coverage
- `MODEL.md` updates

This is not optional if hints are promoted into `sqm-core`.

---

## Migration Strategy

## Phase 1: Introduce typed nodes alongside existing surfaces

- add core hint nodes
- add DSL constructors
- add JSON / visitor / transformer / matcher support
- add parser / renderer support for first-wave dialect features

## Phase 2: Adapt public statement surfaces

- move statement hint storage from `List<String>` toward `List<StatementHint>`
- adapt table hint surfaces toward typed nodes

## Phase 3: Deprecate raw-string APIs where appropriate

- retain compatibility temporarily if needed
- document the typed API as canonical

---

## First-Wave Scope Recommendation

To keep the epic manageable, the first wave should not attempt every vendor hint family.

Recommended first wave:

### Statement hints

- typed statement-hint model
- migration of existing string-based statement hints
- first parser / renderer / validation support for currently implemented hint-bearing dialects

### Table hints

- typed table-hint model
- migration path from current SQL Server table-hint representation
- preserve existing convenience DSL helpers

### Explicitly defer

- exhaustive Oracle hint families
- complex nested hint argument grammars
- every possible MySQL index-hint variation if not already needed

---

## Example Target API Sketch

```java
SelectQuery query = select(col("id"))
    .from(tbl("users").hint(tableHint("NOLOCK")))
    .hint(statementHint("MAX_EXECUTION_TIME", lit(1000)))
    .build();
```

Dialect-friendly version:

```java
SelectQuery query = select(col("id"))
    .from(tbl("users").hint(nolock()))
    .hint(maxExecutionTime(1000))
    .build();
```

Possible internal model:

- `SelectQuery.statementHints(): List<StatementHint>`
- `Table.hints(): List<TableHint>`

---

## Open Design Questions

### 1. Should `Hint` be one hierarchy or two separate roots?

Recommendation:

- one shared `Hint` root with typed subinterfaces for attachment point

Reason:

- shared infrastructure
- explicit distinction where needed

### 2. Should table hints remain partially specialized?

Recommendation:

- yes, convenience helpers may remain specialized
- but the underlying stored model should be generic typed hints

### 3. Should unknown hints be representable?

Recommendation:

- yes, if they can still be structurally represented with generic name + args
- support should still be dialect-validated explicitly

This keeps the framework extensible without requiring immediate deep modeling of every hint family.

### 4. Should hints support comments or formatting preservation?

Recommendation:

- no, not in the semantic model
- preserve meaning, not source formatting

---

## Risks

### Risk 1: Over-modeling too early

If the first version tries to model every vendor hint family exhaustively, the epic may become too broad.

Mitigation:

- start with generic typed hint + typed args
- specialize only where there is clear manipulation value

### Risk 2: Partial migration leaves dual APIs confusing

Mitigation:

- document typed APIs as canonical
- keep compatibility bridges temporary and explicit

### Risk 3: Table-hint migration conflicts with existing dialect slices

Mitigation:

- preserve convenience helper methods
- change stored representation first, helper syntax second

---

## Definition of Success

This epic is successful if, after implementation, SQM can do all of the following structurally:

- inspect hint kind and arguments without reparsing strings
- rewrite hint arguments in a transformer
- validate duplicates and conflicts using typed data
- produce transpilation diagnostics with semantic awareness
- render dialect syntax from typed nodes
- generate DSL code that preserves structure rather than opaque text

---

## User Stories

### Story H1

#### Title
`Story: Introduce core typed hint nodes in sqm-core`

#### User Story
As a SQM maintainer, I want typed hint nodes added to `sqm-core` so hint semantics become first-class model elements instead of opaque strings.

#### Acceptance Criteria
- a shared typed hint model exists in `sqm-core`
- statement and table hint attachment points are represented explicitly
- hint arguments are modeled structurally for the selected first-wave scope
- visitors, transformers, matchers, JSON mixins, and `MODEL.md` are updated for the new node surface
- docs explain the selected hint-node shape and scope boundaries

#### Labels
`story`, `model`, `hints`, `sqm-core`

#### Depends On
Epic model decision for `R7`

---

### Story H2

#### Title
`Story: Add DSL and codegen support for typed hints`

#### User Story
As a SQM user, I want typed hints expressible through DSL and generated code so new hint semantics are usable ergonomically rather than only through low-level model construction.

#### Acceptance Criteria
- generic DSL helpers exist for statement and table hints
- convenience helpers delegate to the canonical typed hint construction path where appropriate
- `sqm-codegen` emits typed hint construction instead of raw string fragments
- generated examples preserve hint structure clearly
- tests cover DSL and codegen output for typed hints

#### Labels
`story`, `dsl`, `codegen`, `hints`

#### Depends On
H1

---

### Story H3

#### Title
`Story: Migrate statement-level hints from raw strings to StatementHint`

#### User Story
As a SQM user, I want statement-level hints stored as typed nodes so transformations and downstream tooling can inspect them safely without reparsing strings.

#### Acceptance Criteria
- statement hint-bearing surfaces store typed `StatementHint` values for the selected scope
- existing raw-string statement hint APIs are adapted or deprecated explicitly
- compatibility behavior is documented where transitional APIs remain
- tests cover typed statement hint construction, inspection, and migration behavior

#### Labels
`story`, `hints`, `model`, `api`

#### Depends On
H1, H2

---

### Story H4

#### Title
`Story: Migrate table-hint modeling toward TableHint`

#### User Story
As a SQM user, I want table hints represented through typed `TableHint` nodes so table-level hint semantics are consistent with the new structured hint model.

#### Acceptance Criteria
- existing table-hint storage moves toward typed `TableHint` nodes
- current convenience helpers continue to work through the typed model
- table-hint APIs remain ergonomic for supported dialect scenarios
- tests cover migration of existing table-hint behavior onto the typed representation

#### Labels
`story`, `hints`, `table`, `model`

#### Depends On
H1, H2

---

### Story H5

#### Title
`Story: Add parser and renderer support for first-wave typed hints`

#### User Story
As a SQM user, I want supported dialect parsers and renderers to read and write typed hints so the new model works end to end for the first-wave hint scope.

#### Acceptance Criteria
- supported dialect parsers emit typed statement and table hints for the selected first-wave cases
- supported dialect renderers render typed hints correctly in supported contexts
- unsupported dialect or context combinations fail explicitly rather than silently degrading
- tests cover representative happy-path and unsupported-path parse/render scenarios

#### Labels
`story`, `parser`, `renderer`, `hints`, `dialect`

#### Depends On
H1, H3, H4

---

### Story H6

#### Title
`Story: Add validation support for typed hints`

#### User Story
As a SQM user, I want typed hints validated structurally so duplicate, conflicting, and unsupported hint combinations are diagnosed clearly.

#### Acceptance Criteria
- shared and dialect-specific validation layers inspect typed hints structurally
- duplicate and conflicting hint combinations are rejected where practical
- unsupported contexts, names, argument counts, or argument shapes are diagnosed clearly
- tests cover positive and negative validation cases for typed hints

#### Labels
`story`, `validation`, `hints`, `dialect`

#### Depends On
H1, H3, H4, H5

---

### Story H7

#### Title
`Story: Add transpilation diagnostics and rules for typed hints`

#### User Story
As a SQM user, I want transpilation to inspect typed hints structurally so exact, approximate, and unsupported outcomes are explicit and safe.

#### Acceptance Criteria
- transpile rules inspect hint kind, attachment point, and typed arguments rather than raw strings
- representative hint cases produce explicit exact, approximate, or unsupported outcomes
- diagnostics explain when typed hints cannot be preserved safely across dialects
- tests cover typed-hint transpilation outcomes and warnings

#### Labels
`story`, `transpile`, `hints`, `dialect`

#### Depends On
H1, H3, H4, H5, H6

---

## Suggested Delivery Order

1. `H1` core typed hint nodes
2. `H2` DSL and codegen support
3. `H3` statement-hint migration
4. `H4` table-hint migration
5. `H5` parser and renderer support
6. `H6` validation support
7. `H7` transpilation support

---

## Final Recommendation

Typed hint modeling is worth a dedicated epic because it is a framework-quality improvement, not just a dialect feature.

It directly supports SQM’s central goal:

- SQL query manipulation based on semantic structure

The recommended implementation strategy is:

- generic typed hint nodes in core
- dialect-owned parse/render/validation behavior
- gradual migration away from raw strings
- first-wave scope intentionally limited to the hint families SQM already touches today

---

## Publishing GitHub Issues

The epic and stories can be published to GitHub issues from this markdown source.

Preview:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r7-typed-hint-issues.ps1 -WhatIf
```

Publish:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\publish-r7-typed-hint-issues.ps1
```

The wrapper delegates to the generic publisher in `scripts/create-github-issues-from-epic-md.ps1`.
