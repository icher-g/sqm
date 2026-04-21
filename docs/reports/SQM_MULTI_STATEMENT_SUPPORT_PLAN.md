# SQM Multi-Statement Support Plan

## Purpose

This document outlines the changes needed to support SQL inputs containing
multiple statements separated by semicolons.

The goal is to make multi-statement support explicit across parser, playground,
code generation, validation, rendering, transpilation, and middleware behavior.
The plan keeps existing single-statement APIs usable while introducing batch or
statement-sequence APIs where needed.

## Recommended Direction

SQM should remain compatible with existing single-statement callers.

Instead of changing every parser API from `Statement` to `List<Statement>`, add
an explicit top-level abstraction for multi-statement input:

```java
StatementSequence
```

The existing API should remain single-statement oriented:

```java
ctx.parse(Statement.class, sql)
```

A new API should handle multi-statement inputs:

```java
ctx.parse(StatementSequence.class, sql)
```

or:

```java
ctx.parseStatementSequence(sql)
```

This keeps the current model stable and makes multi-statement parsing opt-in.

## Resolved Scope Decisions

The initial multi-statement implementation should use these decisions:

- The canonical container name is `StatementSequence`.
- Empty statements are ignored, for example `select 1;;` parses as one statement.
- Direct single-statement parsing accepts one optional trailing semicolon.
- Final rendered statement sequence SQL always includes a trailing semicolon.
- Middleware must not allow partial execution of a batch.
- Playground multi-statement render and transpile output is combined output only.
- Playground parse inspection remains statement-aware for AST, JSON, and DSL views.
- Playground parse responses send one combined AST/JSON/DSL payload; per-statement
  views are derived by the frontend from the `StatementSequence` root payload.
- Code generation exposes both a sequence-level method and per-statement methods.
- Batch APIs are introduced alongside existing single-statement APIs.
- `ctx.parse(StatementSequence.class, sql)` is the batch parsing entry point.
- The `Parser` interface remains unchanged.
- DDL remains out of scope unless a separate DDL decision is made.
- Middleware multi-statement support is enabled by default.
- Middleware must deny the whole batch when any statement is denied, invalid,
  unsupported, or unsafe.
- Query-only batches are treated as read batches.
- Any batch containing `INSERT`, `UPDATE`, `DELETE`, or `MERGE` is treated as a
  write batch.
- Mixed read/write batches are allowed when write execution is allowed.
- `ExecutionMode.ANALYZE` is non-executing analysis and may analyze batches that
  contain DML. SQM does not currently expose a separate read-only execution
  policy mode; adding one would be a separate middleware policy story.
- Statement comments and blank lines do not need to be preserved in rendered
  output, assuming comments cannot carry semantic hints.
- Codegen per-statement method names should use statement intent plus the main
  table name, with numeric suffixes for collisions. Use stable `statementN`
  fallbacks when no main table can be determined.

## Parser Changes

### Semicolon Token

Add a dedicated semicolon token in `sqm-parser`:

- `sqm-parser/src/main/java/io/sqm/parser/core/TokenType.java`
- `sqm-parser/src/main/java/io/sqm/parser/core/Lexer.java`

The lexer should emit a `SEMICOLON` token for `;`.

This must not be implemented by splitting the SQL string on `;`, because
semicolons may appear inside strings, comments, quoted identifiers, and
PostgreSQL dollar-quoted strings.

Examples that must remain single statement content:

```sql
select ';';
select 'a;b';
select $$abc;def$$;
```

### Single-Statement Parsing

For direct `Statement` parsing, support an optional trailing semicolon:

```sql
select 1;
```

This should still reject additional statements:

```sql
select 1; select 2
```

The likely integration point is top-level parse finalization, currently where
EOF is enforced.

### Statement Sequence Parser

Add a dedicated statement sequence or batch parser that parses:

```text
statement (';' statement)* ';'?
```

The statement sequence parser should operate on tokens and cursor boundaries, not raw string
splitting.

Statement parsing inside a statement sequence must treat top-level `SEMICOLON` as a
statement boundary while still allowing semicolons in lexical constructs such as
strings and comments.

Empty statements should be ignored. For example, `select 1;;` should produce a
`StatementSequence` with one statement.

## Diagnostics

Diagnostics need to identify which statement failed.

Add or expose statement-indexed diagnostic metadata, such as:

- `statementIndex`
- statement kind
- source start offset
- source end offset
- line and column

Playground diagnostics should be able to show errors like:

```text
Statement 2, line 5, column 12: Expected FROM
```

This affects parse, validate, render, and transpile diagnostics.

## Core Model

A multi-statement abstraction should be considered a top-level container, not a
new SQL semantic node inside query expressions.

The container should preserve statement ordering and expose immutable statement
lists.

Possible shape:

```java
public interface StatementSequence extends Node {
    List<Statement> statements();
}
```

If source spans are needed, consider a dedicated statement wrapper:

```java
public record ParsedStatement(
    int index,
    Statement statement,
    int startOffset,
    int endOffset
) {
}
```

## Rendering

Existing renderers can continue rendering one `Statement`.

Batch rendering should be a separate helper or renderer that:

- renders statements in order
- joins statements with semicolons
- always ends the final output with a semicolon
- concatenates render parameters in statement order

Parameter handling needs explicit attention. If bind placeholders are ordinal,
batch rendering may need to renumber placeholders after concatenating statement
outputs.

## Validation

Validation should validate each statement independently and aggregate results.

Affected areas include:

- `sqm-validate`
- `sqm-validate-mysql`
- `sqm-validate-postgresql`
- `sqm-validate-sqlserver`
- `sqm-control` validation pipeline
- playground validation service

Validation result aggregation should preserve statement index and should define
whether one invalid statement makes the whole batch invalid. The initial answer
should probably be yes.

## Transpilation

Transpilation currently operates on a single `Statement`.

Add a batch-oriented flow, such as:

```java
transpileStatementSequence(String sql)
transpileBatch(StatementSequence sequence)
```

The result model should be able to represent:

- per-statement source statement
- per-statement rewritten statement
- per-statement diagnostics
- combined rendered SQL
- aggregate outcome

Recommended aggregate outcome rules:

- `exact` only when every statement transpiles exactly.
- `approximate` when at least one statement is approximate and none are unsupported.
- `unsupported` when any statement is unsupported or fails.

The implementation should decide whether partial rendered SQL is returned when
some statements fail. Middleware must not allow partial execution, but developer
tools may still choose whether to expose partial diagnostic output.

## Middleware and Control Pipeline

Multi-statement support is a security and policy boundary.

Current control flow assumes:

- one SQL request
- one parsed `Statement`
- one validation result
- one rewrite result
- one rendered SQL string
- one decision
- one audit event

Batch support needs explicit aggregation behavior.

Important decisions:

- If one statement is denied, is the whole batch denied?
- Can safe statements still execute?
- Are audit events emitted per statement, per batch, or both?
- Does `maxRows` apply per statement or to the batch as a whole?
- How are query fingerprints represented for a batch?
- Can rewrite rules operate across statement boundaries?

Recommended initial policy:

- Enable multi-statement execution by default in middleware.
- Add a maximum statement count such as `maxStatementsPerRequest`.
- Deny the entire batch if any statement is denied.
- Do not allow partial batch execution.
- Treat query-only batches as read batches.
- Treat any batch containing `INSERT`, `UPDATE`, `DELETE`, or `MERGE` as a write
  batch.
- Allow mixed read/write batches only when write execution is allowed.
- `ExecutionMode.ANALYZE` remains non-executing analysis and may analyze DML.
  SQM does not currently expose a separate read-only execution policy mode.
- Reject DDL statements because DDL remains out of scope.
- Apply rewrites per statement only.
- Render a full rewritten batch only after every statement passes validation.

## Playground API

Current playground API responses are singular.

`ParseResponseDto` currently contains one:

- statement kind
- SQM JSON string
- SQM DSL string
- AST
- summary

Parse responses should keep a single canonical parse payload. For multi-statement
input, the root payload is a `StatementSequence`; the frontend derives
per-statement AST, JSON, and DSL views from that root.

Similar batch-aware structures are needed for:

- render response
- validate response
- transpile response

For multi-statement render and transpile responses, the output SQL should be
combined output only. Per-statement render and transpile details may exist in
diagnostics or metadata, but the main SQL output should represent the full
statement sequence.

Diagnostics may include `statementIndex` when the failing operation is tied to a
specific statement in a sequence.

## Playground UI

The UI needs more than multiple AST and JSON trees.

Expected updates:

- statement selector tabs or list
- per-statement AST view
- per-statement JSON view
- per-statement DSL view
- combined rendered SQL view
- statement-indexed diagnostics
- editor navigation to diagnostic positions
- aggregate validation and transpilation summaries

The result panel should make it clear whether an operation succeeded for the
whole batch or only for selected statements.

## Code Generation

The SQL file code generator needs an explicit policy for multi-statement files.

Possible outputs:

- one generated method returning a batch or statement sequence object
- one generated method per statement
- both, with stable statement method names

Recommended initial behavior:

- Treat a multi-statement `.sql` file as one generated statement-sequence method.
- Expose individual statements using a stable generated naming convention.
- Preserve statement order.
- Extract named parameters across the whole statement sequence.
- Name per-statement methods from statement intent and main table name, for
  example `getUsers`, `updateUsers`, or `insertAuditLog`.
- Add numeric suffixes for collisions, for example `getUsers2`.
- Use stable fallbacks such as `statement1` and `statement2` when the statement
  has no clear main table, such as `select 1`, CTE-only queries, set operations,
  or derived-table-only queries.

Parameter handling must be specified. For example:

```sql
update users set name = :name where id = :id;
insert into audit(user_id) values (:id);
```

The generator should define whether shared parameter names are one method
argument or separate per-statement arguments. The most ergonomic initial rule is
to deduplicate named parameters across the statement sequence and pass each name once.

## Middleware and Integration Modules

Review these modules for single-statement assumptions:

- `sqm-control`
- `sqm-middleware-api`
- `sqm-middleware-core`
- `sqm-middleware-rest`
- `sqm-middleware-mcp`
- `sqm-it`
- `sqm-db-it`

REST and MCP APIs may need explicit batch request or batch response shapes.
Avoid silently changing existing single-statement contracts unless the API is
versioned or clearly documented.

## Documentation

Update documentation where behavior changes:

- parser documentation
- playground documentation
- codegen documentation
- middleware documentation
- validation documentation
- transpilation documentation

Document that DDL remains out of scope unless the repository instructions are
updated with a separate DDL decision.

## Testing Plan

Parser tests:

- lex semicolon
- parse single statement with trailing semicolon
- parse multiple statements
- reject extra statements in single-statement parsing
- handle semicolons in strings
- handle semicolons in comments
- handle semicolons in dollar strings
- handle empty statements according to the chosen policy

Playground tests:

- parse response contains multiple statement entries
- diagnostics include statement index
- AST and JSON rendering work per statement
- render, validate, and transpile responses aggregate per-statement results

Codegen tests:

- multi-statement SQL file generates expected Java API
- statement order is stable
- shared parameters are deduplicated or scoped according to the chosen policy
- invalid statement reports file, statement index, line, and column

Control and middleware tests:

- multi-statement request allowed by default
- statement-count guardrail denies requests above the configured maximum
- any denied statement denies the batch
- rewrites are applied per statement
- rendered rewritten SQL joins statements with semicolons
- audit contains one request-level decision event for the batch

Transpilation tests:

- exact batch transpilation
- approximate batch transpilation
- unsupported statement fails aggregate result
- diagnostics preserve statement index
- combined rendered SQL preserves statement order

## Phased Implementation

### Phase 1: Statement Terminator

- Add `SEMICOLON` token.
- Accept optional trailing semicolon for single-statement parsing.
- Add lexer and parser tests.

### Phase 2: Statement Sequence Parser

- Add `StatementSequence` container.
- Add statement sequence parser.
- Add source boundary metadata if needed for diagnostics.
- Add parser tests for tricky semicolon locations.

### Phase 3: Playground Batch Parse

- Add batch parse DTOs.
- Update parse service.
- Update UI statement selector and AST/JSON/DSL views.

### Phase 4: Batch Render and Validate

- Add render aggregation.
- Add validation aggregation.
- Add statement-indexed diagnostics.

Current shared behavior:

- `sqm-render` registers a `StatementSequence` renderer through the ANSI base
  registry used by dialect renderers. It renders statements in order, separates
  them with newlines, and terminates every rendered statement with a semicolon.
- `sqm-control` can render both a single `Statement` and a `StatementSequence`
  through the standard dialect-aware rendering API.
- `sqm-validate` validates each statement in a `StatementSequence`
  independently, aggregates all validation problems, and annotates each problem
  with the one-based statement index.
- Playground render and validate services parse `StatementSequence` and reuse
  the shared render and validation APIs instead of carrying local batch logic.
- Validation diagnostics include `statementIndex` when tied to a statement.

### Phase 5: Batch Transpile

- Add batch transpilation API.
- Add aggregate outcome rules.
- Add combined SQL rendering.

Current transpile behavior:

- `SqlTranspiler` parses SQL through `StatementSequence`, so single-statement
  and multi-statement inputs use the same entry point.
- Statement sequences are transpiled statement-by-statement while preserving
  source order.
- Aggregate outcomes follow the planned rules: `exact` only when all statements
  are exact, `approximate` when at least one statement is approximate and none
  are unsupported, and `unsupported` when any statement is unsupported or fails.
- Combined SQL is rendered only when the full sequence is transpiled
  successfully; partial rendered SQL is not returned for failing batches.
- Render parameterization is controlled by `TranspileOptions` and propagated to
  the combined render result, including bind parameters.
- Playground transpile responses expose combined SQL and combined parameters
  for multi-statement input.

### Phase 6: Codegen

- Define multi-statement `.sql` file API.
- Implement statement-sequence generation.
- Generate both sequence-level and per-statement methods.
- Add parameter handling tests.

Current codegen behavior:

- SQL files are parsed through `StatementSequence`.
- Single-statement SQL files keep the existing generated API shape: the file
  method returns the concrete statement type and the file params method returns
  all named parameters.
- Multi-statement SQL files generate a file-level method returning
  `StatementSequence`.
- Multi-statement SQL files also generate one method per statement, preserving
  source order.
- Per-statement methods are named from statement intent and main table name,
  for example `getUsers`, `updateUsers`, `insertAuditLog`, with numeric suffixes
  when a name is already used in the generated class.
- Statements without a clear main table use stable fallbacks such as
  `statement1`.
- File-level params are deduplicated across the whole sequence, and
  per-statement params methods expose only parameters used by that statement.
- Generated sources use the `statementSequence(...)` DSL helper.

### Phase 7: Middleware

- Add `allowMultiStatement` and statement-count guardrails.
- Add batch decision aggregation.
- Add audit behavior.
- Update REST and MCP contracts where needed.

Current middleware/control behavior:

- `SqlStatementParser.standard()` parses through `StatementSequence` and returns
  the single statement for one-statement input or a `StatementSequence` for
  multi-statement input.
- `SqlDecisionService` and `SqlDecisionEngine` evaluate either a `Statement` or
  `StatementSequence`.
- Validation is applied per statement for a sequence. The first failing
  statement denies the whole batch, with the statement number included in the
  message.
- Rewrite rules are applied per statement. The final rewritten SQL is rendered
  only after all statements pass validation and rewrite processing.
- Sequence rendering uses the existing dialect-aware `SqlStatementRenderer`.
- `RuntimeGuardrails` now supports `maxStatementsPerRequest`; exceeding it
  denies the whole request with `DENY_MAX_STATEMENTS`. Middleware runtime
  configuration can set this through
  `sqm.middleware.guardrails.maxStatementsPerRequest` or
  `SQM_MIDDLEWARE_GUARDRAILS_MAX_STATEMENTS_PER_REQUEST`.
- Query-only statement sequences receive a combined fingerprint. Sequences that
  include DML return no query fingerprint.
- REST and MCP middleware adapters compile and pass their tests against the
  updated control API without DTO changes.

## Completion Status

The initial multi-statement epic is complete.

Completed behavior:

- Parser support for semicolons, optional trailing semicolons, ignored empty
  statements, and `StatementSequence` parsing.
- Core model support for immutable ordered statement sequences.
- DSL support for constructing statement sequences.
- JSON support for `StatementSequence`.
- Renderer support for `StatementSequence`, including trailing semicolons and
  parameter aggregation.
- Validation support for statement sequences with one-based statement-indexed
  diagnostics.
- Transpile support for statement sequences, aggregate outcomes, combined SQL,
  and parameterized output.
- Playground parse/render/validate/transpile support, including per-statement
  AST/JSON/DSL views derived on the frontend from the single combined payload.
- Code generation support for sequence-level methods, per-statement methods,
  stable per-statement names, and parameter extraction.
- Middleware/control support for batch parsing, validation, rewrite, rendering,
  all-or-nothing decisions, request-level audit events, query-only fingerprints,
  and statement-count guardrails.
- REST and MCP middleware adapters pass against the updated control API without
  DTO shape changes.

Final decisions:

- Comments and blank lines are not preserved in normalized statement-sequence
  rendering. Current supported semantic hints are modeled explicitly, not carried
  by raw SQL comments.
- Middleware provides policy-level all-or-nothing approval. It does not enforce
  database transaction atomicity because SQM middleware does not execute SQL.
- DDL remains out of scope and requires a separate product and architecture
  decision before planning or implementation.
- A future read-only execution policy mode, if needed, should be designed as a
  separate middleware policy story. It is not part of the initial
  multi-statement epic.
