# Unsupported Features

This page documents SQL features that are currently outside the supported SQM framework surface.

The goal is to make two things explicit:

- features that are intentionally out of scope for the framework as a whole
- features that are not supported in a given dialect implementation even though they may exist in that database product

This page is about support boundaries, not just shared-model representability.
For nodes that exist in `sqm-core` but are dialect-gated in parser, renderer, validation, or transpilation, see `docs/model/MODEL.md`.

## General Framework Boundaries

The following areas are currently outside the supported framework scope:

- DDL as a general framework feature
- stored procedures and stored procedure definitions
- stored procedure invocation (`CALL`, `EXEC`, `EXECUTE`) as first-class statement support
- procedural SQL and vendor procedural languages:
  - T-SQL procedure bodies
  - PL/pgSQL
  - PL/SQL
  - MySQL stored program control flow
- stored functions, triggers, packages, batches, variable declarations, and control-flow blocks

These are not omitted accidentally. They are outside scope because SQM is primarily designed for:

- SQL parsing into a structured model
- semantic validation
- rewriting and optimization
- dialect rendering
- transpilation
- middleware inspection and policy enforcement

Procedure calls and procedure bodies provide much less value in those areas than regular query and DML statements, because the meaningful data-access semantics usually live inside the procedure body rather than in the invocation syntax.

## Stored Procedures

Stored procedures are intentionally not supported across all currently shipped dialects in SQM.

That means:

- no parser support for procedure invocation statements such as `CALL` or `EXEC`
- no renderer support for procedure invocation statements
- no shared core node model for stored procedure calls
- no support for `CREATE PROCEDURE`
- no procedural body modeling

Even though several database products support stored procedures, SQM does not currently treat them as part of the framework surface.

## Dialect Notes

### ANSI

Not supported:

- DDL in general
- stored procedures
- procedural SQL extensions

### PostgreSQL

Supported in SQM:

- query features within the shipped PostgreSQL parser/render/validate scope
- shipped PostgreSQL DML extensions such as `RETURNING`, `ON CONFLICT`, and `MERGE`

Not supported in SQM:

- stored procedures (`CALL`, `CREATE PROCEDURE`)
- procedural languages such as PL/pgSQL
- triggers and function-body modeling
- PostgreSQL features outside the shipped parser/render/model scope

### MySQL

Supported in SQM:

- query features within the shipped MySQL parser/render/validate scope
- shipped MySQL DML extensions such as `INSERT IGNORE`, `ON DUPLICATE KEY UPDATE`, `REPLACE`, joined `UPDATE`, joined `DELETE`, optimizer hints, and selected built-in functions

Not supported in SQM:

- stored procedures (`CALL`, `CREATE PROCEDURE`)
- MySQL stored program blocks and control flow
- triggers, events, and stored routine body modeling
- MySQL features outside the shipped parser/render/model scope

### SQL Server

Supported in SQM:

- bracket-quoted identifiers
- advanced `TOP` semantics
- SQL Server table lock hints in the shipped contexts
- selected SQL Server built-in functions
- `OUTPUT`
- `MERGE`
- schema/type-mapper downstream support for the delivered SQL Server slice

Not supported in SQM:

- stored procedures (`EXEC`, `EXECUTE`, `CREATE PROCEDURE`)
- T-SQL batches, variable declarations, and procedural control flow
- SQL Server admin/security statements
- temporary-table lifecycle semantics
- DDL in general
- SQL Server features outside the shipped parser/render/model scope

## How To Interpret "Unsupported"

There are two different cases:

1. Framework-out-of-scope

Example:

```sql
EXEC dbo.RefreshUserCache @userId = 1
```

This is unsupported because stored procedure invocation is not part of the framework surface.

2. Dialect-specific feature not yet implemented

This page should mention such cases only when the framework intentionally supports that feature family but the specific dialect slice is still incomplete.

For SQL Server, PostgreSQL, and MySQL, stored procedures belong to the first category, not the second one.

## Related Pages

- [Home](Home)
- [Getting Started](Getting-Started)
- [SQL Server Dialect](SQL-Server-Dialect)
- [Parsing SQL](Parsing-SQL)
- [Rendering SQL](Rendering-SQL)
- [SQL Transpilation](SQL-Transpilation)
