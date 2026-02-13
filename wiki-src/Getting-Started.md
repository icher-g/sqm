# Getting Started

## What SQM Gives You

- A typed SQL model (AST)
- Parser and renderer with dialect support
- Semantic validation against schema metadata
- SQL-to-Java code generation from `.sql` files

## Add Dependencies

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-core</artifactId>
  <version>${sqm.version}</version>
</dependency>
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-parser-ansi</artifactId>
  <version>${sqm.version}</version>
</dependency>
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-render-ansi</artifactId>
  <version>${sqm.version}</version>
</dependency>
```

For PostgreSQL parsing/rendering:

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-parser-postgresql</artifactId>
  <version>${sqm.version}</version>
</dependency>
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-render-postgresql</artifactId>
  <version>${sqm.version}</version>
</dependency>
```

## Minimal Parse + Render

```java
var sql = "select u.id from users u where u.id = :id";
var parseCtx = ParseContext.of(new AnsiSpecs());
var parsed = parseCtx.parse(Query.class, sql);
if (!parsed.ok()) throw new IllegalStateException(parsed.errorMessage());

var renderCtx = RenderContext.of(new AnsiDialect());
var rendered = renderCtx.render(parsed.value()).sql();
```

## Minimal DSL + Render

```java
Query q = select(sel("u", "id"))
  .from(tbl("users").as("u"))
  .where(col("u", "id").eq(param("id")));

var sql = RenderContext.of(new AnsiDialect()).render(q).sql();
```

## Next

- [Core Model](Core-Model)
- [Parsing SQL](Parsing-SQL)
- [Rendering SQL](Rendering-SQL)
