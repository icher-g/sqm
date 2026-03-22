# SQM Modeling Rules

## Purpose

These rules capture the core modeling and implementation principles that emerged from the recent review work.

They are intended to guide future epics and code changes in SQM, especially where the framework must balance:

- shared semantic modeling
- dialect-specific syntax
- explicit dialect support boundaries

They are deliberately short and operational.

## Rule 1: Prefer Shared Semantics Over Dialect Syntax in `sqm-core`

When a SQL construct represents a semantic concept that can reasonably exist across dialects, model that concept in `sqm-core` rather than encoding one dialect’s syntax as the primary abstraction.

### Meaning

Core nodes should answer:

- what this thing is
- what it means in the query model

not primarily:

- how one dialect spells it

### Guidance

Use a shared core node when:

- multiple dialects express the same underlying idea differently
- transformations and transpilation benefit from one semantic representation
- the concept is useful independently of a single vendor syntax

Keep syntax-specific behavior in dialect layers:

- parser
- renderer
- validation
- transpilation rules

### Example

Good:

- `ResultClause` as a shared “DML returns rows” concept
- `VariableTableRef` as a shared semantic relation kind

Not preferred:

- making a dialect keyword or spelling the primary core abstraction when the underlying concept is broader

### Practical question

Before adding a new node to `sqm-core`, ask:

> Is this a new semantic concept in SQM, or only a dialect-specific spelling of an existing concept?

If it is only spelling, prefer dialect-specific parse/render behavior over a new syntax-shaped core node.

---

## Rule 2: Prefer Semantic Nodes When the Distinction Matters for Manipulation

If two constructs look similar in SQL but behave differently for transformation, validation, transpilation, or dialect support, model them as different semantic node kinds.

### Meaning

SQM is a SQL manipulation framework, so semantic distinctions matter whenever they change how code should inspect or rewrite the tree.

### Guidance

Do not split nodes merely because syntax differs.

Do split nodes when the difference changes any of the following:

- support rules
- rewrite behavior
- validation rules
- transpilation behavior
- meaning of identifiers or arguments

### Example

A table variable should not be modeled as a base `Table` just because it appears in a table-like position.

Why:

- a base table is a catalog object
- a variable table is a variable-backed relation reference
- the manipulation semantics differ

That is why `VariableTableRef` is better than overloading `Table`.

### Counter-balance

This rule does not mean “make the tree as large as possible.”

The test is:

> Does the distinction materially improve manipulation correctness or clarity?

If not, prefer the simpler shared node.

---

## Rule 3: Representability in `sqm-core` Must Not Imply Dialect Support

A node existing in `sqm-core` means it is representable in the SQM model. It does not mean every dialect supports it.

### Meaning

SQM core is broader than any single dialect. Therefore support must always be explicit.

### Guidance

For every meaningful dialect feature, review all of these layers:

- parser
- renderer
- validation
- transpilation
- DSL
- codegen

Each layer must be explicit about whether the feature is:

- supported
- rejected
- approximated
- unsupported

### Example

`VariableTableRef` exists in `sqm-core`.

That does not mean:

- ANSI supports it

Instead:

- ANSI parser rejects it
- ANSI renderer rejects it
- SQL Server parser supports its syntax
- SQL Server renderer supports its syntax

That is correct SQM behavior.

### Practical rule

When adding or expanding a core node, always ask:

> Which dialects support this, and how is that support made explicit in every relevant layer?

Do not leave support implied by generic fallbacks.

---

## Rule 4: Dialect Syntax Belongs in Dialect Packages

Once a concept is modeled semantically in `sqm-core`, dialect-specific spelling should remain in dialect packages unless there is a strong framework reason to do otherwise.

### Meaning

Core owns semantic concepts.

Dialect modules own:

- concrete token forms
- SQL text rendering shape
- local syntax restrictions

### Example

Good:

- `VariableTableRef` in `sqm-core`
- `@audit` parsing/rendering in SQL Server parser/renderer modules

Not preferred:

- encoding the SQL Server `@` syntax itself into the core node identity

---

## Rule 5: Future Work Must Be Evaluated Through Manipulation Value

When deciding whether to add, split, or generalize a model node, the main evaluation criterion should be whether the change improves SQM as a manipulation framework.

### Questions to ask

- Does this make transformations safer?
- Does this make transpilation clearer?
- Does this reduce stringly handling?
- Does this remove ambiguity from validation or rendering?
- Does this improve API clarity for developers building SQL trees?

If the answer is mostly “no”, the change is probably too syntax-driven or too implementation-specific.

---

## Rule 6: Avoid Stringly Modeling for Features That Matter to Manipulation or Transpilation

If a feature family is expected to participate in transformation, validation, or transpilation, prefer structured AST nodes over raw strings.

### Meaning

Stringly modeling is acceptable only when SQM does not need to reason semantically about the feature.

If SQM needs to answer questions like:

- what kind of feature is this?
- what are its arguments?
- is it equivalent to another construct?
- is it exact, approximate, or unsupported in another dialect?

then the feature should not remain a raw string.

### Guidance

Prefer typed nodes when the framework needs to:

- inspect the feature structurally
- rewrite it safely
- validate arguments or combinations
- transpile it with explicit diagnostics

### Example

Raw string form:

```java
optimizerHint("MAX_EXECUTION_TIME(1000)")
```

Structured form:

```java
statementHint("MAX_EXECUTION_TIME", lit(1000))
```

The structured form is better because transpilation, validation, and transformation can reason over:

- hint kind
- normalized name
- typed arguments

### Practical rule

Before storing a feature as a string, ask:

> Will SQM ever need to inspect, transform, validate, or transpile this by meaning rather than by exact text?

If yes, model it structurally.

---

## Recommended Usage

These rules should be applied during:

- epic design
- model reviews
- dialect feature additions
- transpilation story design
- DSL and codegen surface review

They are especially important whenever a feature could be implemented either as:

- a new core node
- a dialect-only parser/render rule
- a stringly field

In those cases, these rules should drive the decision.
