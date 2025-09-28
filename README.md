# sqlmodel

A dialect-aware SQL **model + renderer + parser** for the JVM.

`sqlmodel` lets you **describe queries as structured models** instead of concatenating strings.  
It separates **what** you want to query from **how** it is rendered for a specific SQL dialect.

---

## Why sqlmodel?

- **Safer than string SQL**  
  Build queries from typed objects (`Query`, `Column`, `Filter`, `Join`, …) instead of manual string concatenation. This prevents common mistakes like missing quotes, wrong placeholders, or broken joins.

- **Dialect-aware**  
  The same model can be rendered differently depending on the target database dialect (ANSI, PostgreSQL, SQL Server, Oracle, …). Dialects handle quoting, placeholders (`?` vs `$1`), pagination (`LIMIT/OFFSET`, `OFFSET/FETCH`, `TOP`), null sorting, and boolean values.

- **Parameterization-first**  
  Values are automatically collected into a parameter list instead of embedded directly into SQL text. This makes queries safe against injection and ready for JDBC prepared statements.

- **Readable and maintainable**  
  Use the **DSL** for programmatic construction, or the **SPEC-DSL** to parse SQL-like snippets into the model. Both produce the same typed AST and can be rendered consistently.

- **Composable**  
  Filters, joins, subqueries, and values can be built independently and reused across different queries. Useful for building dynamic queries or libraries on top.

- **Testable**  
  Since queries are models, you can render them in unit tests and assert both the generated SQL string and the parameter list. No database needed.

---

## Modules

- `sqlmodel-core` – data model (`Query`, `NamedTable`, `NamedColumn`, `Join`, `ColumnFilter`, `CompositeFilter`, `TupleFilter`, `OrderItem`, `GroupItem`, `Values`, etc.)
- `sqlmodel-render` – `SqlWriter`, `SqlText`, `Renderer<T>`, `RenderContext`/SPI (`SqlDialect`, `IdentifierQuoter`, `Placeholders`, `Operators`, `Booleans`, `NullSorting`, `PaginationStyle`, `ValueFormatter`, `ParamSink`)
- `sqlmodel-render-ansi` – `AnsiSqlDialect`, `AnsiRenderContext`, `AnsiParamSink`, `DefaultValueFormatter`, concrete ANSI renderers
- `sqlmodel-parser` – SPEC parsers, `SpecParsers.defaultRepository()`, and the `dsl.QueryBuilder`

---

## Build a model with the DSL
### Example
#### SQL
```sql
SELECT u.user_name, o.status, count(*) AS cnt
FROM orders AS o
INNER JOIN users AS u ON u.id = o.user_id
GROUP BY u.user_name, o.status
HAVING count(*) > 10
```
#### DSL
```java
Query q = q()
   .select(c("u", "user_name"), c("o", "status"), func("count", star()).as("cnt"))
   .from(t("orders").as("o"))
   .where(in(List.of("A", "B")))
   .join(inner(t("users").as("u")).on(eq(c("u", "id"), c("o", "user_id"))))
   .groupBy(g(c("u", "user_name")), g(c("o", "status")))
   .having(gt(func("count", star()), 10));
```
#### SQL DSL
```java
QueryBuilder qb = QueryBuilder.newBuilder();

qb.select("u.user_name", "o.status", "count(*) AS cnt")
  .from("orders AS o")
  .where("o.status IN ('A','B')")
  .innerJoin("users AS u ON u.id = o.user_id")
  .groupBy("u.user_name, o.status")
  .having("count(*) > 10");

Query q = qb.build();
```
