# SQM Query Manipulation Review

## Context

This note answers a broader framework question:

> Before moving to the next epic, what in the current SQM model and implementation could still be improved, keeping in mind that SQM is primarily a framework for SQL query manipulation?

The focus here is not a specific dialect epic. The goal is to look at the current model through the lens of:

- semantic clarity
- transformation ergonomics
- dialect isolation
- long-term extensibility

This is a draft intended as a roadmap reference, not as a hard acceptance-criteria document.

## Executive Summary

SQM is already strong in a few areas that matter for manipulation:

- broad AST coverage
- consistent visitor / transformer support
- explicit parser / renderer separation
- growing dialect completeness discipline

The main opportunities are not about adding more syntax. They are about improving places where the model still reflects source SQL shape more than manipulation intent.

The highest-value themes are:

1. tighten the line between shared semantics and dialect-specific syntax
2. prefer typed semantic nodes over stringly or syntax-shaped fields
3. make support boundaries explicit in every layer, especially for dialect-specific features
4. keep improving AST ergonomics for transformations, not only coverage

## 1. Shared Semantics vs Dialect Syntax

### Why this matters

A manipulation framework works best when the core AST says what a construct means, while dialect modules decide how that meaning is spelled.

When the model is too close to one dialect's syntax, a few problems appear:

- transformations become vendor-shaped
- transpilation becomes harder to reason about
- core nodes need to be reshaped every time a dialect adds a near-equivalent feature

### Example

The `RETURNING` / `OUTPUT` area is a good example.

These SQL forms are different syntactically:

```sql
INSERT INTO users(name) VALUES ('a') RETURNING id
```

```sql
UPDATE users
SET name = 'b'
OUTPUT inserted.id
```

But at a higher level they share a semantic idea:

- a DML statement emits a result row set

Where they differ:

- SQL Server has pseudo-row sources like `inserted` / `deleted`
- SQL Server also supports `OUTPUT ... INTO ...`
- other dialects may not

### Current situation

SQM already has the beginnings of the right separation:

- `ResultClause`
- `ResultItem`
- `ResultInto`
- SQL Server-specific output item nodes

This is good, but this area still represents an ongoing design pressure:

- how much should stay as shared DML-result semantics?
- how much should remain dialect-shaped?

### Suggested improvement

Treat the pattern as a framework design rule:

- shared core nodes should capture "statement returns rows"
- dialect-specific nodes should capture only the parts that truly have no neutral semantic equivalent

### Concrete recommendation

Continue reviewing dialect features with this question:

> Is this a new semantic concept in SQM, or just a dialect-specific spelling of an existing concept?

If the answer is "existing concept", prefer:

- shared node
- dialect-specific parser / renderer / validation behavior

If the answer is "truly vendor-specific concept", prefer:

- dialect support around a clearly isolated node or support rule

## 2. Hints Are Still Under-Modeled

### Why this matters

Hints affect SQL shape, performance, and sometimes behavior. In a query manipulation framework, hints should ideally be inspectable, transformable, and dialect-aware.

String-based hint storage is convenient, but weak for manipulation.

### Example

A string-based hint API looks like:

```java
select(...)
    .optimizerHint("MAX_EXECUTION_TIME(1000)")
```

This is easy to store, but hard to manipulate. A transformer cannot safely answer:

- what kind of hint is this?
- what arguments does it have?
- is this equivalent to another hint?
- can this be rewritten for another dialect?

### Problem

If hints are raw strings:

- validation can only do coarse checks
- codegen has to preserve text, not meaning
- transpilation has no structure to work with
- downstream tooling cannot inspect them semantically

### Better shape

A typed model would allow nodes like:

```java
StatementHint.of("MAX_EXECUTION_TIME", List.of(lit(1000)))
```

or, even better, a richer family if needed:

- generic statement hint node
- generic table hint node
- dialect-specific rendering rules

### Suggested improvement

Move hints gradually from:

- `List<String>`

to:

- typed hint nodes

without necessarily designing every dialect hint family up front.

### Incremental path

1. Introduce a generic typed hint node in `sqm-core`
2. Keep rendering close to existing output
3. Let dialect validators reject unsupported typed hints
4. Add richer subtype families only where the framework gets real manipulation value

## 3. Some Model Areas Are Still Syntax-First

### Why this matters

A syntax-first node tells you how something was written.

A semantics-first node tells you what kind of thing it is.

Manipulation frameworks need the second more than the first.

### Example: variable tables

A SQL Server table variable:

```sql
OUTPUT inserted.id INTO @audit(id)
```

should not be modeled as a normal base `Table` just because it appears in a table-like position.

If it were modeled as:

```java
Table.of(null, Identifier.of("@audit"), ...)
```

then the AST would misleadingly say:

- this is a base table

even though semantically it is:

- a variable-backed relation reference

The move to `VariableTableRef` is a good example of improving semantic precision.

### Broader lesson

This question should be applied elsewhere too:

> Are we modeling a concept, or just preserving surface spelling?

### Suggested improvement

Keep favoring dedicated semantic nodes when:

- two things look similar in SQL
- but behave differently during transformation or dialect handling

That does not mean "make the tree huge". It means:

- split only where the distinction changes manipulation behavior

## 4. Dialect Boundaries Should Stay Explicit in Every Layer

### Why this matters

For a manipulation framework, it is dangerous when a construct is:

- parsed
- represented
- maybe even transformed

but only much later discovered to be unsupported for a target dialect.

That creates implicit support.

### Example

Suppose a core node exists for some construct and a dialect renderer accepts it accidentally through a generic path. Then users may assume:

- this dialect supports it

even if:

- the parser never intended to accept it
- the validator never reviewed it
- transpilation does not understand it

### Current direction

SQM is already improving here. The SQL Server work recently reinforced the pattern that dialect features should be reviewed across:

- parser
- renderer
- validation
- transpilation
- DSL
- codegen

That is exactly the right direction.

### Suggested improvement

Make this a standing framework rule:

> A node being representable in the model does not imply support in a dialect.

Then keep explicit behavior in every layer:

- parse support
- render support
- validation support
- transpilation policy

### Small example

If `VariableTableRef` exists in core:

- ANSI parser should reject it
- ANSI renderer should reject it
- SQL Server parser may accept `@name`
- SQL Server renderer may render `@name`

That is good framework behavior because support is explicit.

## 5. Matchers / Visitors / Transformers Are Complete but Can Still Be More Ergonomic

### Why this matters

SQM has good traversal infrastructure. That is a major strength.

But there are two different goals:

- completeness of traversal
- ease of expressing transformations

The first is already strong. The second can still improve.

### Example

Today a transform often looks like:

```java
new RecursiveNodeTransformer() {
    @Override
    public Node visitVariableTableRef(VariableTableRef t) {
        return tableVar("audit_archive");
    }
}
```

This is workable, but some transformations still feel low-level because users need to know many exact node shapes to perform common semantic rewrites.

### Problem

If the AST is technically complete but not pleasant to manipulate:

- framework users will write ad hoc logic
- transformations will be more brittle
- APIs will feel internal rather than ergonomic

### Suggested improvement

Review the model from a transformation-user perspective:

- what are the most common rewrites?
- what node families are awkward to inspect?
- where do users need too much syntax knowledge?

### Concrete idea

Consider adding more helper APIs where they improve real manipulations, for example:

- semantic helper predicates
- normalization helpers
- better DSL constructors for structurally meaningful nodes

The goal is not to hide the tree. It is to make common manipulations easier to express correctly.

## 6. Stringly Modeling Still Limits Transpilation Quality

### Why this matters

Transpilation depends on knowing which parts of the tree are:

- exact equivalents
- approximations
- unsupported

Stringly fields weaken that analysis.

### Example

If a hint, modifier, or clause option is stored as plain text:

```java
"WITH TIES"
```

or:

```java
"MAX_EXECUTION_TIME(1000)"
```

then transpilation logic must either:

- parse strings again
- ignore them
- reject them coarsely

### Suggested improvement

For any feature family likely to matter in transpilation, prefer:

- structured AST nodes

That enables rule families like:

- exact rewrite
- approximate rewrite with warning
- unsupported with explicit diagnostic

## 7. Distinguish Persistent Tables, Temporary Tables, and Variable Tables Carefully

### Why this matters

Relation-like things in SQL are not all the same kind of relation.

That distinction matters for manipulation.

### Example

These are not equivalent:

```sql
INTO audit_log
```

```sql
INTO #audit_log
```

```sql
INTO @audit_log
```

Possible interpretation:

- `audit_log` -> base table
- `#audit_log` -> still a table, but temporary in lifecycle
- `@audit_log` -> variable-backed relation

### Design value

Being precise here helps transformations answer questions like:

- is this a catalog object?
- is this a temporary table object?
- is this variable-scoped state?

### Suggested improvement

Keep the distinctions semantic where they affect framework behavior.

Likely direction:

- temp tables can remain `Table`
- variable-backed relations deserve `VariableTableRef`

This is a good example of where "table-like" is not enough for a manipulation framework.

## 8. Documentation Can Better Explain Support vs Representability

### Why this matters

Framework users often infer support from model shape.

If a node exists in core, many users will naturally assume:

- every dialect probably supports it somehow

That is not always true and should not be implied.

### Suggested improvement

Improve documentation around three separate ideas:

1. representable in SQM core
2. supported by a given dialect parser / renderer
3. transpilable to another dialect

### Example wording

Instead of only saying:

- "`VariableTableRef` is a core node"

also document:

- ANSI: representable but rejected
- SQL Server: parsed / rendered / validated
- transpilation: currently unsupported unless rule added

That framing fits SQM’s purpose much better.

## Suggested Backlog

Below is a practical improvement backlog ordered by likely framework impact.

### Near-term

1. Introduce typed hint nodes instead of relying on raw strings
2. Continue reviewing DML result modeling with a strict semantics-vs-syntax lens
3. Document model representability vs dialect support more explicitly

### Mid-term

4. Review other syntax-shaped areas for semantic node opportunities
5. Improve transformer ergonomics for common manipulations
6. Strengthen transpilation rule design around structured rather than stringly inputs

### Ongoing

7. For every new dialect feature, review parser / renderer / validation / transpilation / DSL / codegen together
8. Prefer semantic node naming in `sqm-core`, and dialect-specific syntax only in dialect packages

## Final Assessment

The current SQM direction is good for a manipulation framework, but the next quality step is not primarily "support more SQL".

It is:

- make the model more semantic where it changes manipulation value
- keep dialect syntax at the edges
- reduce stringly representation where structural reasoning matters
- keep support boundaries explicit in every layer

If SQM keeps following that path, it will become not only a bigger SQL model, but a more reliable framework for inspection, rewriting, validation, and transpilation.
