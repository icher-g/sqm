## Epic

### Title
`Epic: R9 Transformation Ergonomics`

### Problem Statement
SQM already has strong structural manipulation infrastructure:

- nearly all nodes are visitable
- recursive transformers preserve immutability correctly
- matchers provide uniform dispatch
- JSON and codegen support usually keep pace with model growth

That completeness is a real strength, but there is still a gap between:

- being able to traverse the tree
- being able to manipulate the tree ergonomically

Today, many real transformation tasks are technically possible but still require too much low-level AST knowledge about:

- which concrete subtype carries a value
- how semantic values are nested through wrapper nodes
- which parent must be rebuilt after a child change
- which factory or builder recreates the parent canonically

This creates framework-level risks:

- users write transformations that are too syntax-shaped rather than semantic
- rewrite logic becomes brittle as node families evolve
- common manipulations require repeated boilerplate
- SQM feels internally complete but externally less ergonomic than it should

### Epic Goal
Improve the authoring ergonomics of SQM transformations without replacing the current visitor, matcher, and recursive transformer model.

The target is not a new traversal framework. The target is a small, explicit ergonomic layer that helps users and maintainers perform common semantic manipulations with less incidental node-shape knowledge.

### Business Value
- Makes SQM easier to use as a SQL manipulation framework rather than only a complete AST.
- Reduces maintenance cost by replacing repeated handwritten rewrite logic with clearer shared utilities.
- Improves resilience of transformation code when model details evolve.
- Aligns transformation ergonomics with DSL and codegen ergonomics so public APIs feel more coherent.
- Gives tests, transpilation rules, normalization logic, and examples a cleaner canonical style.

### Relationship To Other Work

#### Informed By
- `SQM_QUERY_MANIPULATION_REVIEW.md`
- `SQM_MODELING_RULES.md`

#### Coordinates With
- `R4` transpilation work, because transpilation is one of the biggest consumers of transformation APIs
- `R7` typed hint modeling, because richer semantic nodes benefit from clearer manipulation helpers
- DSL and codegen follow-up work where awkward construction often signals awkward transformation surfaces

#### DDL Boundary
- This epic does not introduce DDL scope.
- It improves shared manipulation ergonomics for existing framework surfaces only.

### Scope Boundaries

#### In Scope
- semantic inspection helpers where they materially improve manipulation readability
- task-oriented transformation utilities for repeated rewrite patterns
- selective convenience methods on existing nodes when they expose stable semantics
- consistency review of builder and `of(...)` conventions where parent reconstruction is awkward
- canonical examples in docs/tests that demonstrate preferred transformation patterns
- adoption of first-wave ergonomic helpers in selected real use sites

#### Out Of Scope
- replacing visitors, matchers, or recursive transformers with a new abstraction
- hiding the AST behind opaque "magic" APIs
- adding semantic helpers for every node family without evidence of reuse
- broad model redesign unrelated to manipulation ergonomics
- DDL, stored procedures, or dialect-expansion work that is not needed for the ergonomic layer itself

### Non-Goals
- optimizing for brevity alone
- introducing parser- or renderer-only helper APIs that bypass the core model
- building a generic rewrite engine that guesses user intent
- weakening the existing identity-preservation and immutability guarantees

### Definition of Done
- A first-wave ergonomic helper layer exists for repeated, high-value manipulation tasks.
- The helper layer preserves the current explicit visitor/transformer model rather than replacing it.
- Selected semantic inspection helpers exist only where they represent stable, reusable categories.
- DSL and transformation ergonomics are reviewed together for the affected node families.
- Canonical examples in docs or tests demonstrate preferred manipulation patterns.
- At least a small set of existing real use sites adopts the new helpers so the design is validated against production-style code.
- Tests cover the new helper behavior and preserve current immutability and identity expectations.

### Suggested Labels
`epic`, `ergonomics`, `transform`, `dsl`, `transpile`, `api`

---

## Design Overview

### 1. Preserve The Current Structural Guarantees

This epic must not weaken the current strengths of SQM:

- immutability
- explicit visitors
- recursive transformer identity preservation
- matcher completeness

Every ergonomic addition should compose with those guarantees rather than bypass them.

### 2. Prefer Semantic Access Over Incidental Shape Knowledge

The main design principle is to reduce how often callers must know:

- the exact wrapper path to reach a semantic value
- which parent node needs to be rebuilt
- which of several subtypes represents the relevant category

The goal is not to hide structure completely. The goal is to make common semantic intents more obvious.

### 3. Keep The Helper Layer Small And Explicit

Ergonomic APIs should remain:

- composable
- readable
- selective
- easy to remove or revise if they do not prove useful

This epic should start with a narrow first wave rather than attempting a broad utility surface immediately.

### 4. Review DSL And Transformation Ergonomics Together

If a node family is awkward to build through DSL or codegen, it is often also awkward to inspect and rewrite.

For the node families touched by this epic, the review must ask:

- is it easy to build?
- is it easy to inspect?
- is it easy to rewrite?

Those questions should be treated as one API-quality concern.

### 5. Validate Helpers In Real Use Sites

This epic should not stop at adding utilities in isolation.

The first wave must be adopted in selected:

- transpilation rules
- normalization code
- tests
- documentation examples

That adoption is the proof that the helpers improve real code rather than adding a second abstraction layer nobody uses.

---

## Candidate Improvement Areas

### A. Semantic Inspection Helpers

Introduce a small set of helpers for stable semantic categories that recur in real manipulation code.

Candidate examples:

- relation kind checks
- whether a DML statement has a result sink
- whether a result clause uses dialect-specific shapes
- whether a table reference is catalog-backed, variable-backed, or derived

Delivery rule:

- only add these helpers when the category is stable and the readability gain is meaningful

### B. Task-Oriented Transformation Utilities

Introduce reusable utilities for rewrite tasks that appear repeatedly across tests, transpilation, normalization, and codegen.

Candidate families:

- `IdentifierTransforms`
- `RelationTransforms`
- `ResultClauseTransforms`
- `ExpressionTransforms`

Delivery rule:

- utilities should internally use the standard transformer infrastructure and remain explicit about what they rewrite

### C. Selective Node Convenience Methods

Where stable semantics are repeatedly rediscovered by callers, add focused convenience methods on existing nodes.

Candidate examples:

- `ResultClause.hasIntoTarget()`
- `ResultClause.usesDialectSpecificResultItems()`
- `ResultInto.isVariableTarget()`
- `ResultInto.isBaseTableTarget()`

Delivery rule:

- do not turn nodes into large utility objects

### D. Transformer-Friendly Construction Consistency

Review affected node families for consistency in:

- canonical `of(...)` factories
- optional-field handling
- parent reconstruction clarity
- identity-preserving child replacement behavior

The point is not to redesign the model broadly. It is to remove avoidable reconstruction friction in high-value manipulation paths.

### E. Canonical Transformation Examples

Add a small set of preferred examples that demonstrate how SQM transformations should be authored.

Recommended example families:

- rename identifiers
- rewrite result sink targets
- normalize relation references
- parameterize literals
- strip or adapt unsupported features during normalization or transpilation

These examples should live where users already learn the framework:

- focused tests
- docs
- codegen-friendly examples where relevant

---

## Delivery Strategy

### 1. Survey Repetitive Manipulation Patterns First

The first implementation step should inspect:

- transformer-heavy tests
- transpilation rules
- identifier normalization flows
- codegen emitters
- DSL-heavy examples

The goal is to find repeated patterns that genuinely justify helpers.

### 2. Start With A Narrow First Wave

Recommended first-wave areas:

- relation classification helpers
- result-clause semantic helpers
- one or two reusable transformation utility families
- a small canonical example set

### 3. Adopt Helpers In Real Production-Style Code

First-wave helpers should be used in selected:

- transpilation code
- normalization utilities
- tests

This should happen before expanding the helper surface.

### 4. Expand Selectively Based On Proven Value

Only add more helpers after the first wave demonstrates:

- reduced boilerplate
- clearer semantic intent
- stable semantics
- real adoption

---

## Risks

### 1. Helper Surface Creep

If every mildly repetitive pattern becomes a public helper, the ergonomic layer will become noisy and harder to learn than the raw AST.

Mitigation:

- require evidence of repeated use
- prefer a small first wave

### 2. Opaque Utility Abstractions

If helpers hide too much structure, the framework may become harder to reason about and less trustworthy.

Mitigation:

- keep utilities explicit
- continue using the standard transformer model underneath

### 3. Local Improvements That Do Not Generalize

A helper that only makes one test shorter is probably not worth becoming part of the framework surface.

Mitigation:

- validate each first-wave helper in multiple real use sites

### 4. DSL And Transformation Drift

If node-construction ergonomics and rewrite ergonomics are improved independently, the public API may remain inconsistent.

Mitigation:

- review DSL, codegen, and transformation usage together for affected node families

---

## User Stories

### Story E1

#### Title
`Story: Document canonical transformation patterns for common SQM rewrites`

#### User Story
As a SQM user, I want clear examples of preferred transformation patterns so I can build on the existing visitor and transformer model without rediscovering framework conventions by trial and error.

#### Acceptance Criteria
- canonical examples exist for a first-wave set of common rewrites
- examples demonstrate preferred use of recursive transformers, matchers, and DSL helpers
- examples live in docs or tests where maintainers and users will actually find them

#### Labels
`story`, `ergonomics`, `docs`, `transform`

#### Depends On
Epic design agreement

---

### Story E2

#### Title
`Story: Add semantic inspection helpers for relation categories and result structures`

#### User Story
As a SQM maintainer, I want stable semantic inspection helpers so common transformation code can express intent directly instead of matching repeatedly on low-level node shape.

#### Acceptance Criteria
- first-wave helpers exist for selected relation categories and DML result structures
- helpers represent stable semantic categories rather than arbitrary subtype shortcuts
- tests cover the helper semantics and boundary cases

#### Labels
`story`, `ergonomics`, `inspect`, `api`

#### Depends On
E1

---

### Story E3

#### Title
`Story: Introduce reusable identifier and relation transformation utilities`

#### User Story
As a SQM user, I want reusable transformation helpers for repeated rewrite tasks so common identifier and relation rewrites require less handwritten AST reconstruction.

#### Acceptance Criteria
- at least one identifier-focused and one relation-focused transform utility family exists
- utilities internally use the standard transformer model
- helper behavior preserves immutability and unchanged-instance rules
- tests cover representative rewrite scenarios

#### Labels
`story`, `ergonomics`, `transform`, `api`

#### Depends On
E2

---

### Story E4

#### Title
`Story: Review selected node families for semantic convenience methods and construction consistency`

#### User Story
As a SQM maintainer, I want high-friction node families reviewed for convenience and construction consistency so custom transformers remain readable and predictable.

#### Acceptance Criteria
- selected node families are reviewed for stable semantic convenience methods
- canonical construction paths remain clear through `of(...)` or builder usage
- changes do not weaken existing immutability or identity-preservation rules
- DSL impact is reviewed for the affected node families

#### Labels
`story`, `ergonomics`, `dsl`, `model`

#### Depends On
E2, E3

---

### Story E5

#### Title
`Story: Adopt first-wave ergonomic helpers in transpilation, normalization, and tests`

#### User Story
As a SQM maintainer, I want the new ergonomic helpers used in real framework code so we can verify they improve maintainability instead of becoming unused abstractions.

#### Acceptance Criteria
- selected transpilation or normalization use sites adopt the new helpers
- selected tests are rewritten to use the preferred transformation style
- the adoption demonstrates reduced incidental node-shape knowledge in real code
- follow-up gaps discovered during adoption are documented explicitly

#### Labels
`story`, `ergonomics`, `transpile`, `validation`

#### Depends On
E1, E2, E3, E4

---

## Suggested Delivery Order

1. `E1` canonical examples
2. `E2` semantic inspection helpers
3. `E3` reusable transform utilities
4. `E4` convenience-method and construction-consistency review
5. `E5` real adoption in transpilation, normalization, and tests

---

## Definition Of Success

This epic is successful if future SQM transformation code:

- uses less incidental node-shape knowledge
- expresses semantic intent more directly
- keeps the explicit visitor and transformer model intact
- becomes easier to read in tests, transpilation, and normalization code

The desired outcome is not a new transformation framework.

It is a more ergonomic use of the framework SQM already has.
