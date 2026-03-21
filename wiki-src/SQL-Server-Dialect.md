# SQL Server Dialect

SQM supports SQL Server parsing, rendering, validation, DSL/codegen reachability, and downstream runtime coverage for the current delivered SQL Server query and DML scope.

This page is a practical guide to the SQL Server syntax currently supported by the framework.

## Overview

Current SQL Server support includes:

- bracket-quoted identifiers such as `[users]` and `[name]`
- `TOP (...)`, `TOP (...) PERCENT`, `TOP (...) WITH TIES`
- `OFFSET ... FETCH`
- table lock hints:
  - `WITH (NOLOCK)`
  - `WITH (UPDLOCK)`
  - `WITH (HOLDLOCK)`
- selected SQL Server built-in functions:
  - `LEN`
  - `DATEADD`
  - `DATEDIFF`
  - `ISNULL`
  - `STRING_AGG`
- baseline DML:
  - `INSERT`
  - `UPDATE`
  - `DELETE`
- `OUTPUT` and `OUTPUT ... INTO`
- `MERGE` with:
  - `WHEN MATCHED` update/delete
  - `WHEN NOT MATCHED` insert
  - `WHEN NOT MATCHED BY SOURCE` update/delete
  - clause predicates (`WHEN ... AND ...`)
  - `TOP (...)`
  - `TOP (...) PERCENT`
  - `OUTPUT`

## Parse And Render

```java
var parseCtx = ParseContext.of(new SqlServerSpecs());
var renderCtx = RenderContext.of(new SqlServerDialect());

var query = parseCtx.parse(Query.class, """
    SELECT TOP (5) [u].[id], LEN([u].[name]) AS [name_len]
    FROM [users] AS [u]
    ORDER BY [u].[id]
    """).value();

var sql = renderCtx.render(query).sql();
System.out.println(sql);
```

## Query Examples

Simple `TOP`:

```sql
SELECT TOP (5) [id], [name]
FROM [users]
ORDER BY [id]
```

Advanced `TOP`:

```sql
SELECT TOP (10) PERCENT [u].[id]
FROM [users] AS [u]
ORDER BY [u].[id]
```

`TOP ... WITH TIES`:

```sql
SELECT TOP (10) WITH TIES [u].[id], [u].[score]
FROM [users] AS [u]
ORDER BY [u].[score] DESC
```

Pagination with `OFFSET/FETCH`:

```sql
SELECT [id], [name]
FROM [users]
ORDER BY [name]
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
```

Table hints:

```sql
SELECT TOP (10) PERCENT [u].[id]
FROM [users] AS [u] WITH (NOLOCK)
ORDER BY [u].[id]
```

## DML Examples

`INSERT ... OUTPUT`:

```sql
INSERT INTO [users] ([name])
OUTPUT inserted.[id]
VALUES ('alice')
```

`UPDATE ... OUTPUT ... INTO`:

```sql
UPDATE [users]
SET [name] = 'alice'
OUTPUT deleted.[name], inserted.[name]
INTO [audit] ([old_name], [new_name])
WHERE [id] = 1
```

`DELETE`:

```sql
DELETE FROM [users]
WHERE [id] = 1
```

## MERGE Examples

Basic merge:

```sql
MERGE INTO [users] AS [u]
USING [incoming_users] AS [s]
ON [u].[id] = [s].[id]
WHEN MATCHED THEN
  UPDATE SET [name] = [s].[name]
WHEN NOT MATCHED THEN
  INSERT ([id], [name]) VALUES ([s].[id], [s].[name])
```

Advanced merge with predicate, `BY SOURCE`, `TOP`, hint, and `OUTPUT`:

```sql
MERGE TOP (10) PERCENT INTO [users] WITH (HOLDLOCK)
USING [users] AS [s]
ON [users].[id] = [s].[id]
WHEN MATCHED AND [s].[id] = 1 THEN
  UPDATE SET [name] = [s].[name]
WHEN NOT MATCHED BY SOURCE AND [users].[active] = 0 THEN
  DELETE
OUTPUT deleted.[id]
```

## DSL Example

```java
var statement = merge(tbl("users").withHoldLock())
    .source(tbl("users").as("s"))
    .on(col("users", "id").eq(col("s", "id")))
    .top(TopSpec.of(lit(10L), true, false))
    .whenMatchedUpdate(
        col("s", "id").eq(lit(1L)),
        set(id("name", QuoteStyle.BRACKETS), col("s", "name"))
    )
    .whenNotMatchedBySourceDelete(
        col("users", "active").eq(lit(0L))
    )
    .result(deleted(id("id", QuoteStyle.BRACKETS)))
    .build();
```

## Validation Notes

- `TOP ... WITH TIES` requires `ORDER BY`.
- duplicate or conflicting SQL Server table lock hints are rejected.
- SQL Server-only syntax is validated both during parse and render phases.
- `MERGE` branch ordering and branch-family rules are validated explicitly for SQL Server.

## Related Pages

- [Parsing SQL](Parsing-SQL)
- [Rendering SQL](Rendering-SQL)
- [Schema Validation](Schema-Validation)
- [SQL File Codegen](SQL-File-Codegen)
- [SQL Middleware Framework](SQL-Middleware-Framework)
