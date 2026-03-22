# SQM Transformation Ergonomics Design

## Purpose

This document turns the review note

> Matchers / Visitors / Transformers are complete but can still be more ergonomic

into a concrete low-level design reference.

The goal is not to replace the current visitor / matcher / transformer infrastructure. That infrastructure is a strength of SQM and should remain the foundation.

The goal is to improve how easy it is to use that infrastructure for real SQL manipulation work.

This is a framework-quality design note, not a dialect-specific story.

---

## Problem Statement

SQM already provides broad structural coverage:

- nearly all nodes are visitable
- recursive transformers preserve immutability correctly
- matchers provide a uniform dispatch style
- JSON and codegen support generally keep pace with node additions

This is good for correctness and completeness.

However, there is still a difference between:

- being able to traverse the tree
- being able to manipulate the tree ergonomically

Today, many transformations are technically possible but still require too much low-level AST knowledge.

That creates a few risks:

- users write overly syntax-shaped transformation logic
- transformations become brittle when model details evolve
- common rewrite tasks require too much boilerplate
- the framework feels “internally complete” but not yet “externally ergonomic”

---

## Design Goals

### 1. Preserve Current Structural Guarantees

Do not weaken any of the current strengths:

- immutability
- visitor completeness
- transformer identity-preservation rules
- matcher coverage

### 2. Improve Transformation Authoring Experience

Common manipulations should require less boilerplate and less knowledge of incidental node shape.

### 3. Keep Semantics First

Ergonomic improvements should encourage semantic manipulation, not hide the tree behind opaque shortcuts.

### 4. Stay Incremental

This should be a gradual improvement track, not a disruptive rewrite of all traversal APIs.

---

## Current Strengths

Before discussing gaps, it is important to state what should be preserved.

### A. RecursiveNodeTransformer has a strong contract

Transformers currently support a critical SQM guarantee:

- return the same instance if unchanged
- return a new instance if changed

This is excellent for structural sharing and predictable rewrites.

### B. Node coverage is broad

The current system usually ensures that new nodes receive:

- visitor support
- recursive traversal support
- matcher support
- JSON support

That completeness is valuable and should remain mandatory.

### C. Matchers reduce raw `instanceof` branching

The matcher APIs already improve readability in many places, especially for tests and inspections.

---

## Current Ergonomic Gaps

## 1. Common rewrites still require too much node-shape knowledge

### Example

Suppose a user wants to rename a table variable target:

```java
new RecursiveNodeTransformer() {
    @Override
    public Node visitVariableTableRef(VariableTableRef t) {
        return tableVar("audit_archive");
    }
}
```

This is acceptable.

But once the transformation becomes even slightly broader, users often need to know:

- exactly which node subtype carries the value
- how that node is nested
- which wrapper node holds the relevant child
- which factory recreates the parent correctly

### Example

A user might want to “rewrite every DML result sink target”.

Today that requires knowing:

- result sink lives in `ResultClause`
- sink is held via `ResultInto`
- target is under `ResultInto.target()`
- target may itself be one of several `TableRef` variants

This is structurally sound, but still more mechanical than ideal.

### Improvement direction

Add semantic helpers for common manipulation entry points.

Examples:

- helper methods that expose “has mutation result sink”
- helper methods that normalize access to “result projection items”
- helper predicates for common relation categories

These helpers should not replace the AST. They should reduce incidental shape knowledge.

---

## 2. Matchers are uniform, but not always semantic enough

Current matchers are primarily structural dispatch tools.

That is useful, but sometimes manipulation code needs semantic categories rather than raw subtype splits.

### Example

A transformation may want to answer:

- is this relation a catalog-backed table?
- is this relation variable-backed?
- is this relation derived?

Today the code often answers by matching on specific node types:

- `Table`
- `VariableTableRef`
- `QueryTable`
- etc.

That works, but can feel too low-level when the real concern is semantic category.

### Improvement direction

Add semantic classification helpers where they materially improve manipulation.

Possible examples:

- `TableRefKinds`
- utility predicates
- category helpers in a small `core.util` or `core.inspect` package

Example sketch:

```java
TableRefKinds.isCatalogTable(ref)
TableRefKinds.isVariableTable(ref)
TableRefKinds.isDerivedTable(ref)
```

This should be done selectively, only for categories with real transformation value.

---

## 3. Rebuilding parent nodes is still verbose in some families

The recursive transformer base class does a good job structurally, but custom transformations still sometimes require repeating parent reconstruction logic.

### Example

A user might conceptually want:

- “rewrite all identifiers using a normalization strategy”

But in practice, they often must know which parent node reconstructs which child and which factory to call.

### Problem

This is especially awkward when:

- the same semantic element appears in several node families
- each family has different reconstruction conventions

### Improvement direction

Prefer small normalization / transformation helpers for repeated semantic rewrite tasks.

Examples:

- identifier normalization helpers
- relation-target replacement helpers
- result-projection replacement helpers

The target is not a magical generic rewrite engine. The target is to reduce repeated handwritten reconstruction for common framework manipulations.

---

## 4. Transformation APIs are structurally complete but not task-oriented

The current APIs answer:

- how do I visit or transform this node type?

They less often answer:

- how do I perform a common SQM manipulation task?

### Example common tasks

- rename identifiers
- qualify / dequalify columns
- rewrite result sinks
- replace literals with parameters
- normalize table references
- inspect unsupported dialect features

Some of these already have helpers or partial support, but the framework does not yet present them as a coherent ergonomic layer.

### Improvement direction

Introduce a small “task-oriented manipulation utilities” layer over the raw transformer APIs.

Possible categories:

- identifier utilities
- relation utilities
- result-clause utilities
- hint utilities
- expression normalization utilities

This layer should remain explicit and composable, not magical.

---

## 5. DSL ergonomics and transformation ergonomics should be reviewed together

This is an important low-level design rule.

If a node is awkward to build, it is often also awkward to transform.

### Example

If tests or codegen need awkward construction patterns for a node, that is a sign the public manipulation surface may also be too incidental.

### Improvement direction

When reviewing transformation ergonomics, also ask:

- is the node easy to build through DSL?
- is the node easy to inspect?
- is the node easy to rewrite?

These should be treated as related API quality concerns, not separate ones.

---

## Proposed Improvement Areas

## A. Semantic Inspection Helpers

Introduce a small set of helper APIs for answering common semantic questions without repeated raw matcher code.

### Candidate examples

- relation kind checks
- whether a DML statement has a result sink
- whether a result clause uses dialect-specific row sources
- whether a relation is catalog-backed vs derived vs variable-backed

### Non-goal

Do not add helpers just to avoid one line of matcher usage.

Only add helpers where:

- the pattern is common
- the semantic category is stable
- the helper improves readability meaningfully

---

## B. Task-Oriented Transformation Utilities

Introduce reusable manipulation helpers for tasks that appear repeatedly across tests, codegen, normalization, and transpilation.

### Candidate utility families

- `IdentifierTransforms`
- `RelationTransforms`
- `ResultClauseTransforms`
- `ExpressionTransforms`

### Example direction

Instead of each caller writing custom tree logic for:

- “rename every identifier with strategy X”

provide a reusable helper that internally uses the standard transformer infrastructure.

This keeps the official node-level transformer contract while making common tasks easier to author.

---

## C. Better Semantic Convenience Methods on Existing Nodes

Selective convenience methods can make manipulation clearer without hiding structure.

### Example candidates

For `ResultClause`:

- `hasIntoTarget()`
- `usesDialectSpecificResultItems()`

For `ResultInto`:

- `isVariableTarget()`
- `isBaseTableTarget()`

For `TableRef`:

- semantic category checks if they prove broadly useful

### Caution

Do not turn nodes into large utility objects.

Only add methods that:

- express stable semantics
- reduce repeated low-value boilerplate

---

## D. Transformer-Friendly Builder Consistency

Where parent reconstruction is common, ensure factory/builder conventions are consistent enough that custom transformers remain easy to write.

### Example review questions

- does the node have one obvious canonical `of(...)`?
- are optional fields handled consistently?
- can unchanged children preserve identity cleanly?

This is partly already covered by repo rules, but it should also be treated as a transformation ergonomics concern.

---

## E. Stronger Examples and Reference Patterns

One of the easiest ergonomic wins is better examples.

### Why

Users often learn the framework through:

- tests
- DSL examples
- codegen output

If those examples show awkward transformation patterns, users will copy them.

### Suggested improvement

Add a small set of “canonical transformation examples” to tests or docs, such as:

- rename identifiers
- rewrite result sink target
- normalize relation references
- strip unsupported hints
- parameterize literals

These examples should demonstrate preferred use of:

- recursive transformers
- matchers
- DSL helpers

---

## What Not To Do

## 1. Do not replace visitors / transformers with opaque utility magic

The current traversal model is explicit and trustworthy. That is good.

The goal is to add ergonomic layers around it, not to hide it behind an overly abstract API.

## 2. Do not optimize for brevity alone

Shorter code is not automatically better.

The real target is:

- clearer manipulation intent
- lower incidental AST knowledge
- safer rewrites

## 3. Do not add semantic helpers for every single node family

That would create noise and dilute value.

Only add helpers where they support common or error-prone manipulation tasks.

---

## Proposed Rollout Plan

## Phase 1: Identify repetitive manipulation patterns

Survey:

- transformer tests
- transpilation rules
- identifier normalization logic
- codegen emitters
- DSL-heavy tests

Goal:

- find which patterns are repeated often enough to justify ergonomic helpers

## Phase 2: Add a very small helper layer

Start with:

- relation classification helpers
- result-clause semantic helpers
- one or two reusable transformation utility families

Keep the scope narrow.

## Phase 3: Validate against real use sites

Adopt the new helpers in:

- selected tests
- selected transpile rules
- selected normalization utilities

Goal:

- confirm they improve real code rather than adding another abstraction layer nobody uses

## Phase 4: Expand selectively

Only after the first wave proves useful, add additional semantic helpers or task-oriented utilities.

---

## Example Candidate Stories

### Story E1

Document canonical transformation patterns for common SQM rewrites.

### Story E2

Add semantic inspection helpers for relation categories and DML result structures.

### Story E3

Introduce reusable identifier and relation transformation utilities.

### Story E4

Review selected node families for convenience methods that improve manipulation intent.

### Story E5

Apply the first-wave ergonomic helpers in transpilation and normalization code to validate utility.

---

## Success Criteria

This design direction is successful if future SQM transformation code:

- uses less incidental node-shape knowledge
- expresses more semantic intent directly
- still preserves the explicit visitor / transformer model
- becomes easier to read in tests, transpilation, and normalization code

The desired outcome is not a new transformation framework.

It is a more ergonomic use of the framework SQM already has.
