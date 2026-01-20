# SQM — Structured Query Model for Java

AI Coding Agent Instructions for the SQM codebase.

## Project Overview

SQM is a **bidirectional SQL transformation library** for Java 21+ that provides:
- SQL text → structured AST model → SQL text (parse and render)
- JSON serialization of SQL queries for external tools
- Fluent DSL for programmatic query construction
- Visitor/transformer patterns for query analysis and rewriting

**Core Philosophy**: SQL as composable, strongly-typed objects with dialect-aware rendering.

## Architecture & Module Structure

SQM uses a **multi-module Maven project** with clear separation of concerns:

- **`sqm-core`** — Core AST model (`io.sqm.core.*`), DSL builders (`io.sqm.dsl.Dsl`), and visitor/transformer infrastructure
- **`sqm-parser`** — Base parser interfaces (`Parser`, `ParseContext`, `ParseResult`)
- **`sqm-parser-ansi`** — ANSI SQL parser implementation
- **`sqm-render`** — Base renderer interfaces (`Renderer`, `RenderContext`, `SqlWriter`)
- **`sqm-render-ansi`** — ANSI SQL renderer with dialect support
- **`sqm-json`** — Jackson mixins for JSON serialization (`SqmJsonMixins`)
- **`sqm-it`** — Integration tests for round-trip validation (parse → model → render)
- **`examples`** — Usage examples

**Module Dependencies**:
```
sqm-core (no deps except JUnit for tests)
  ↑
  ├── sqm-parser → sqm-parser-ansi
  ├── sqm-render → sqm-render-ansi
  └── sqm-json
        ↑
        └── sqm-it (test scope: combines all modules for integration testing)
```

## Model Design Patterns

### 1. Sealed Interfaces + Record Implementations

All AST nodes implement the **sealed `Node` interface** with exhaustive type hierarchies:

```java
public sealed interface Node permits Expression, Query, Predicate, Join, ... {
    <R> R accept(NodeVisitor<R> v);
}

public sealed interface Expression extends Node permits ColumnExpr, LiteralExpr, FunctionExpr, ... {}
```

**Implementation Convention**: Each interface has a corresponding `*Impl` record in `io.sqm.core.internal`:
- Interface: `io.sqm.core.ColumnExpr`
- Implementation: `io.sqm.core.internal.ColumnExprImpl`
- Records are **immutable** with builder-style methods returning new instances

### 2. Visitor Pattern (RecursiveNodeVisitor)

Use `RecursiveNodeVisitor<R>` for **collecting information** from the AST:

```java
class ColumnCollector extends RecursiveNodeVisitor<Void> {
    private final Set<String> columns = new LinkedHashSet<>();
    
    @Override
    public Void visitColumnExpr(ColumnExpr c) {
        columns.add(c.name());
        return super.visitColumnExpr(c); // Continue traversal
    }
}
```

### 3. Transformer Pattern (RecursiveNodeTransformer)

Use `RecursiveNodeTransformer` for **modifying/rewriting** queries:

```java
class RenameColumnTransformer extends RecursiveNodeTransformer {
    @Override
    public Node visitColumnExpr(ColumnExpr c) {
        if ("old_name".equals(c.name())) {
            return ColumnExpr.of("new_name");
        }
        return c; // Return unchanged
    }
}
```

### 4. Match API (Type-Safe Dispatching)

Replace `instanceof` chains with the **fluent Match API**:

```java
String sql = Match.<String>expression(expr)
    .column(c -> c.name())
    .literal(l -> String.valueOf(l.value()))
    .func(f -> f.name() + "(...)")
    .orElse("<unknown>");
```

Each node type has a corresponding matcher: `QueryMatch`, `PredicateMatch`, `ExpressionMatch`, etc.

## Key Conventions

### DSL Entry Point: `io.sqm.dsl.Dsl`

**Static import for fluent query building**:

```java
import static io.sqm.dsl.Dsl.*;

Query q = select(col("id"), col("name"))
    .from(tbl("users"))
    .where(col("active").eq(lit(true)));
```

Short method names: `col()`, `tbl()`, `func()`, `lit()`, `sel()`, `order()`, `group()`

### Parser/Renderer Context Pattern

**Parsing**: `ParseContext.of(new AnsiSpecs())` → `ctx.parse(Query.class, sql)`  
**Rendering**: `RenderContext.of(new AnsiDialect())` → `ctx.render(query).sql()`

Both return immutable results (`ParseResult<T>`, `SqlText`).

### Parameterization Modes

- `ParameterizationMode.Inline` (default): Literals stay inline, named params as `:name`
- `ParameterizationMode.Bind`: Convert all literals to `?` placeholders for JDBC binding

Example:
```java
SqlText result = ctx.render(query, RenderOptions.of(ParameterizationMode.Bind));
// result.sql() = "SELECT * FROM t WHERE age > ? AND name = ?"
// result.params() = [21, "Alice"]
```

### Renderers and Parsers: SPI Pattern

**Each AST node type has its own Renderer/Parser**:
- Renderer: `interface Renderer<T extends Node>` with `void render(T node, RenderContext ctx, SqlWriter w)`
- Parser: `interface Parser<T extends Node>` with `ParseResult<T> parse(ParseContext ctx, TokenStream ts)`

Implementations registered in `RenderersRepository` / `ParsersRepository`.

## Testing Patterns

### Unit Tests (per module)

- **Renderers**: Test individual renderers in `sqm-render-ansi/src/test/java/io/sqm/render/ansi/*RendererTest.java`
- **Parsers**: Test individual parsers in `sqm-parser-ansi/src/test/java/io/sqm/parser/ansi/*ParserTest.java`
- **Model**: Test visitors/transformers in `sqm-core/src/test/java/io/sqm/core/walk/*Test.java`

### Integration Tests (`sqm-it`)

**Round-trip validation**: Parse SQL → Model → Render SQL, assert equality:

```java
@Test
void roundTripTest() {
    String originalSql = "SELECT a, b FROM t WHERE x > 10";
    Query query = parseContext.parse(Query.class, originalSql).value();
    String renderedSql = renderContext.render(query).sql();
    assertEquals(originalSql, renderedSql);
}
```

Located in `sqm-it/src/test/java/io/sqm/it/`.

## Build & Test Commands

**Build everything**:
```bash
mvn clean install
```

**Run all tests**:
```bash
mvn test
```

**Run tests for specific module**:
```bash
cd sqm-core && mvn test
```

**Generate Javadocs** (sqm-core only):
```bash
cd sqm-core && mvn javadoc:javadoc
```

**Code coverage** (JaCoCo):
```bash
mvn clean test  # Generates target/jacoco.exec in each module
```

## Common Tasks

### Adding a New AST Node Type

1. Define sealed interface in `sqm-core/src/main/java/io/sqm/core/MyNode.java`
2. Add record implementation in `sqm-core/src/main/java/io/sqm/core/internal/MyNodeImpl.java`
3. Update parent sealed interface's `permits` clause
4. Implement `accept(NodeVisitor<R> v)` to call `v.visitMyNode(this)`
5. Add `visitMyNode(MyNode n)` method to `NodeVisitor` interface
6. Add renderer in `sqm-render-ansi` implementing `Renderer<MyNode>`
7. Add parser in `sqm-parser-ansi` implementing `Parser<MyNode>`
8. Register renderer/parser in respective repositories
9. Add DSL helper method in `Dsl.java` if appropriate

### Adding Support for a New SQL Dialect

1. Create new module `sqm-render-<dialect>` or `sqm-parser-<dialect>`
2. Implement `Dialect` interface (e.g., `PostgresDialect extends AnsiDialect`)
3. Override specific renderers for dialect-specific syntax
4. Use `DialectNode` for dialect-specific extensions to the model
5. Add integration tests in `sqm-it` with dialect-specific expectations

## Documentation References

- **Model Hierarchy**: See [docs/MODEL.md](../docs/MODEL.md) for complete AST structure
- **Usage Examples**: See [examples/src/main/java/io/sqm/examples/](../examples/src/main/java/io/sqm/examples/)
- **README**: [README.md](../README.md) has quick examples and feature overview

## Key Files to Understand First

1. [sqm-core/src/main/java/io/sqm/core/Node.java](../sqm-core/src/main/java/io/sqm/core/Node.java) — Root of AST hierarchy
2. [sqm-core/src/main/java/io/sqm/dsl/Dsl.java](../sqm-core/src/main/java/io/sqm/dsl/Dsl.java) — Fluent DSL entry point
3. [sqm-core/src/main/java/io/sqm/core/Query.java](../sqm-core/src/main/java/io/sqm/core/Query.java) — Top-level query node
4. [sqm-core/src/main/java/io/sqm/core/walk/RecursiveNodeVisitor.java](../sqm-core/src/main/java/io/sqm/core/walk/RecursiveNodeVisitor.java) — Visitor base class
5. [sqm-render/src/main/java/io/sqm/render/spi/Renderer.java](../sqm-render/src/main/java/io/sqm/render/spi/Renderer.java) — Renderer SPI

## Testing Patterns and Best Practices

### Unit Test Structure (Per Module)

**Core Model Tests** (`sqm-core/src/test/java/io/sqm/core/`):
- Test factory methods, equality, immutability
- Test visitor acceptance and traversal
- Test Match API integration
- Test node transformation (RecursiveNodeTransformer)

**Parser Tests** (`sqm-parser-ansi/src/test/java/io/sqm/parser/ansi/`):
- Parse individual SQL fragments with `parseContext.parse(NodeType.class, sql)`
- Test case insensitivity
- Test with various expression types (literals, columns, functions)
- Document parser limitations as commented tests with TODO

**Renderer Tests** (`sqm-render-ansi/src/test/java/io/sqm/render/ansi/`):
- Use DSL to build model nodes
- Test rendering in different SQL contexts (WHERE, JOIN, HAVING, SELECT)
- **Important**: Renderer outputs **multiline formatted SQL** — either check parts separately or normalize whitespace
- Test dialect-specific keywords and syntax

**Integration Tests** (`sqm-it/src/test/java/io/sqm/it/`):
- Test **full queries** using `Query.class`, not specific predicate subtypes
- Round-trip: Parse SQL → Model → Render SQL → Assert equality
- **Always normalize whitespace** in assertions: `normalizeWhitespace(expected)` vs `normalizeWhitespace(actual)`
- Use JSON comparison for semantic equality (see `SelectRoundTripTest`)

### Common Testing Pitfalls (CRITICAL)

1. **ParseResult API**:
   - ✅ Use `parseResult.ok()` (not `isOk()`)
   - ✅ Use `parseResult.value()` to get the parsed node
   - ✅ Use `parseResult.errorMessage()` for diagnostics

2. **Cursor API**:
   - ✅ `Cursor.of(sql)` — single String parameter only
   - ❌ Don't pass `AnsiSpecs` to `Cursor.of()`

3. **Match API**:
   - ✅ Always use explicit type parameters: `Match.<String>predicate(pred)`
   - ❌ Don't rely on type inference — it fails for Match API

4. **Imports and Packages**:
   - ✅ `io.sqm.render.ansi.spi.AnsiDialect` (note the `.spi` package)
   - ✅ `io.sqm.parser.ansi.AnsiSpecs`
   - ✅ `io.sqm.parser.spi.ParseContext`
   - ✅ `io.sqm.render.spi.RenderContext`

5. **Numeric Literals**:
   - ✅ Parser returns `Long` for numeric literals, not `Integer`
   - ✅ Use `assertEquals(10L, literal.value())` in assertions

6. **Integration Test Parsing**:
   - ✅ Parse `Query.class` for full SQL statements
   - ✅ Parse `Predicate.class` for standalone predicates (not specific subtypes like `IsDistinctFromPredicate.class`)
   - ❌ Don't parse specific predicate subtypes directly — parser doesn't support it

7. **SQL Whitespace Normalization**:
   - ✅ Renderer outputs **multiline formatted SQL** with indentation
   - ✅ Always normalize: `sql.replaceAll("\\s+", " ").trim()`
   - ✅ Add helper method in test classes:
     ```java
     private String normalizeWhitespace(String sql) {
         return sql.replaceAll("\\s+", " ").trim();
     }
     ```

8. **Parser/Renderer Registration**:
   - ✅ Register new parsers in `sqm-parser-ansi/src/main/java/io/sqm/parser/ansi/Parsers.java`
   - ✅ Register new renderers in `sqm-render-ansi/src/main/java/io/sqm/render/ansi/Renderers.java`
   - ✅ Check existing registrations before adding duplicates

### Test Naming Conventions

- Model tests: `<NodeType>Test.java` (e.g., `IsDistinctFromPredicateTest.java`)
- Match tests: `<NodeType>MatchTest.java`
- Visitor tests: `<NodeType>VisitorTest.java`
- Parser tests: `<NodeType>ParserTest.java`
- Renderer tests: `<NodeType>RendererTest.java`
- Integration tests: `<NodeType>IntegrationTest.java`

### Example Test Templates

**Unit Test (Core)**:
```java
@Test
void shouldCreatePredicateWithFactoryMethod() {
    var pred = IsDistinctFromPredicate.of(col("a"), col("b"));
    assertFalse(pred.negated());
    assertEquals("a", ((ColumnExpr)pred.lhs()).name());
}
```

**Parser Test**:
```java
@Test
void shouldParseIsDistinctFrom() {
    var parseContext = ParseContext.of(new AnsiSpecs());
    var result = parseContext.parse(Predicate.class, "a IS DISTINCT FROM b");
    assertTrue(result.ok());
    assertInstanceOf(IsDistinctFromPredicate.class, result.value());
}
```

**Renderer Test**:
```java
@Test
void shouldRenderInWhereClause() {
    var query = select(col("*"))
        .from(tbl("t"))
        .where(col("a").isDistinctFrom(col("b")));
    
    var ctx = RenderContext.of(new AnsiDialect());
    var sql = ctx.render(query).sql();
    
    assertTrue(sql.contains("IS DISTINCT FROM"));
}
```

**Integration Test**:
```java
@Test
void testRoundTripIsDistinctFrom() {
    String originalSql = "SELECT * FROM t WHERE a IS DISTINCT FROM b";
    
    var parseResult = parseContext.parse(Query.class, originalSql);
    assertTrue(parseResult.ok());
    
    var query = parseResult.value();
    var renderedSql = renderContext.render(query).sql();
    
    assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
}
```

## Anti-Patterns to Avoid

- ❌ **Don't mutate AST nodes** — they're immutable records. Use transformers to create modified copies.
- ❌ **Don't use `instanceof` chains** — use the Match API for type-safe dispatching.
- ❌ **Don't parse in renderer or render in parser** — keep concerns separated by module.
- ❌ **Don't hardcode SQL syntax in model classes** — syntax belongs in renderers.
- ❌ **Don't skip round-trip tests** — every new parser/renderer feature needs integration tests in `sqm-it`.
- ❌ **Don't forget whitespace normalization** — renderer outputs formatted SQL, tests must normalize for comparison.
- ❌ **Don't use wrong API methods** — `parseResult.ok()` not `isOk()`, `Match.<Type>` not implicit inference.
