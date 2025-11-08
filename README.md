# SQM — Structured Query Model for Java

[![Build](https://github.com/icher-g/sqm/actions/workflows/publish-maven.yml/badge.svg?branch=main)](https://github.com/icher-g/sqm/actions/workflows/publish-maven.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Packages](https://img.shields.io/badge/Maven-GitHub%20Packages-blue)](https://github.com/icher-g/sqm/packages)
[![codecov](https://codecov.io/gh/icher-g/sqm/graph/badge.svg)](https://codecov.io/gh/icher-g/sqm)

**SQM (Structured Query Model)** is a lightweight Java library for modeling SQL queries as composable, strongly-typed objects.  
It enables **bidirectional transformations** between SQL, JSON, and DSL forms — making it ideal for query generation, analysis, rewriting, and serialization across dialects.

---

## ✨ Features

- 🧩 **Structured model** — fully object-oriented representation of SQL (Query, Table, Column, Predicate, Join, etc.)
- 🔁 **Bidirectional flow** — parse SQL → model → render SQL again (and JSON/DSL support)
- 🧠 **Dialect-aware rendering** — ANSI core + SQL Server, PostgreSQL, etc.
- 🧪 **Extensive test coverage** — golden-file round-trip tests and property-based validation
- 🧰 **Builder DSL** — fluent helpers for programmatic query construction
- 🧾 **JSON serialization** — Jackson mixins for all core model types
- 🧱 **Extensible** — custom functions, renderers, pagination styles, and dialects

---

## 🧭 Architecture Overview

```
        ┌─────────────┐
        │   SQL Text  │
        └──────┬──────┘
               │  parse
               ▼
        ┌─────────────┐
        │    Model    │   ←→   JSON / DSL
        └──────┬──────┘
               │  render
               ▼
        ┌─────────────┐
        │   SQL Text  │
        └─────────────┘
```

Core components:
- **Model** — unified AST representing any SQL query.
- **Parsers** — turn SQL strings into model objects.
- **Renderers** — convert model objects back into SQL (dialect-aware).
- **DSL Builders** — programmatic query construction.
- **JSON Mixins** — serialization/deserialization for external tools.

---

## Model Hierarchy

SQM defines a rich, type-safe model (AST) to represent SQL queries internally.
This model is shared between the DSL, parser, and renderer modules.

➡️ [View the full hierarchy in docs/MODEL.md](docs/MODEL.md)

---

## 🚀 Quick Example

### Build a query with the DSL and Render

```java
Query q = select(
        sel("u", "user_name"),
        sel("o", "status"),
        func("count", starArg()).as("cnt")
    )
    .from(tbl("orders").as("o"))
    .join(
        inner(tbl("users").as("u"))
            .on(col("u", "id").eq(col("o", "user_id")))
    )
    .where(col("o", "status").in("A", "B"))
    .groupBy(group("u", "user_name"), group("o", "status"))
    .having(func("count", starArg()).gt(10))
    .orderBy(order("cnt"))
    .limit(10)
    .offset(20);

var ctx = RenderContext.of(new AnsiDialect());
var sql = ctx.render(q).sql();

System.out.println(sql);
```

**Rendered (ANSI):**

```sql
SELECT u.user_name, o.status, count(*) AS cnt
FROM orders AS o
INNER JOIN users AS u ON u.id = o.user_id
WHERE o.status IN ('A', 'B')
GROUP BY u.user_name, o.status
HAVING count(*) > 10
ORDER BY cnt DESC
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
```
---

### Parse SQL into a Model

```java
var sql = """
    SELECT u.user_name, o.status, count(*) AS cnt
    FROM orders AS o
    INNER JOIN users AS u ON u.id = o.user_id
    WHERE o.status in ('A', 'B')
    GROUP BY u.user_name, o.status
    HAVING count(*) > 10""";

var ctx = ParseContext.of(new AnsiSpecs());
var pr = ctx.parse(Query.class, sql);
if (pr.isError()) {
    throw new RuntimeException(pr.errorMessage());
}
var query = pr.value();
```
---

### Serialize to JSON

```java
ObjectMapper mapper = SqmJsonMixins.createPretty();
String json = mapper.writeValueAsString(query);
```

Output example:
```json
{
  "kind" : "on",
  "right" : {
    "kind": "table",
    "name": "users",
    "alias": "u"
  },
  "kind" : "INNER",
  "on" : {
    "kind": "comparison",
    "lhs":  {
      "kind" : "column",
      "tableAlias" : "u",
      "name" : "id"
    },
    "operator": "EQ",
    "rhs": {
      "kind" : "column",
      "tableAlias" : "o",
      "name" : "user_id"
    }
  }
}
```

---

### 🧮 Collectors & Transformers

The SQM model provides powerful traversal and transformation mechanisms built on top of the **Visitor pattern**.  
Two common examples are *collectors* (for extracting information from a query tree) and *transformers* (for producing modified copies).

---

#### 🧩 Column Collector Example

A **collector** walks through the query tree and gathers information, such as all referenced column names.  
Collectors typically extend `RecursiveNodeVisitor<R>` and accumulate results internally.

```java
private static class QueryColumnCollector extends RecursiveNodeVisitor<Void> {

    private final Set<String> columns = new LinkedHashSet<>();

    @Override
    protected Void defaultResult() {
        return null;
    }

    public Set<String> getColumns() {
        return columns;
    }

    @Override
    public Void visitColumnExpr(ColumnExpr c) {
        columns.add(c.tableAlias() == null ? c.name() : c.tableAlias() + "." + c.name());
        return super.visitColumnExpr(c);
    }
}
```

##### Usage

```java
var collector = new QueryColumnCollector();
query.accept(collector);
Set<String> usedColumns = collector.getColumns();
usedColumns.forEach(System.out::println);
```

This visitor automatically traverses all expressions, so you only need to override the specific node type you want to handle (in this case `ColumnExpr`).  
The recursive base visitor (`RecursiveNodeVisitor`) ensures all sub-nodes are visited.

---

#### 🔄 Column Transformer Example

A **transformer** produces a modified copy of the query tree.  
Transformers extend `RecursiveNodeTransformer` (a subclass of `NodeTransformer<Node>`) and return new node instances where changes are required.

```java
public static class RenameColumnTransformer extends RecursiveNodeTransformer {
    @Override
    public Node visitColumnExpr(ColumnExpr c) {
        if ("u".equals(c.tableAlias()) && "id".equals(c.name())) {
            return ColumnExpr.of("u", "user_id");
        }
        return c;
    }
}
```

##### Usage

```java
var transformer = new RenameColumnTransformer();
Query transformed = transformer.transform(originalQuery);
```

The `RecursiveNodeTransformer` automatically handles traversal and reconstruction of immutable nodes.  
You only override methods for nodes you wish to modify — all others are traversed and returned unchanged.

---

#### ⚙️ Summary

| Concept                             | Description                                                  |
|-------------------------------------|--------------------------------------------------------------|
| **Visitor**                         | Walks the query tree and can perform analysis or collection. |
| **Transformer**                     | Walks the query tree and produces a modified copy.           |
| **Recursive Visitors/Transformers** | Provide automatic traversal of all subnodes.                 |
| **Collectors**                      | Typically accumulate results in a `Set`, `List`, or `Map`.   |
| **Transformers**                    | Typically override a few methods to replace or rename nodes. |

---

#### 💡 Typical Use Cases

- Collect all referenced tables or columns in a query.  
- Rewrite column names, table aliases, or function calls.  
- Apply dialect-specific rewrites or optimizations.  
- Extract metadata for validation or analysis.  
- Implement automatic query normalization or anonymization.


---

### 🧭 Safe Optional Chaining with `Opts`

When navigating deep node hierarchies, it’s common to perform multiple safe casts such as  
`asSelect() → asOn() → asComparison() → asColumn()`.  
Without help, this leads to nested `flatMap` calls or verbose `ifPresent` blocks.

The `Opts` helper provides a **fluent, type-safe** way to chain these lookups.

```java
var name =
    Opts.start(transformedQuery)
        .then(Query::asSelect)
        .then(s -> s.joins().stream().findFirst())
        .then(Join::asOn)
        .then(on -> on.on().asComparison())
        .then(cmp -> cmp.lhs().asColumn())
        .map(ColumnExpr::name);

name.ifPresent(System.out::println);
```

---

#### 💡 Key Ideas

| Concept             | Description                                                     |
|---------------------|-----------------------------------------------------------------|
| `Opts.start(value)` | Begins the chain with any object (may be `null`).               |
| `then(f)`           | Applies a function that returns `Optional<U>` (like `flatMap`). |
| `thenMap(f)`        | Applies a function that returns a plain value `U` (like `map`). |
| `map(f)`            | Terminal mapping step, returns `Optional<U>`.                   |
| `toOptional()`      | Ends the chain and exposes the underlying `Optional<T>`.        |

---

#### ✅ Example

Compare the same logic using standard `Optional` chaining vs `Opts`:

##### Without `Opts`

```java
transformedQuery
    .asSelect()
    .flatMap(s -> s.joins().stream().findFirst())
    .flatMap(j -> j.asOn())
    .flatMap(on -> on.on().asComparison())
    .flatMap(cmp -> cmp.lhs().asColumn())
    .map(ColumnExpr::name)
    .ifPresent(System.out::println);
```

##### With `Opts`

```java
Opts.start(transformedQuery)
    .then(Query::asSelect)
    .then(s -> s.joins().stream().findFirst())
    .then(Join::asOn)
    .then(on -> on.on().asComparison())
    .then(cmp -> cmp.lhs().asColumn())
    .map(ColumnExpr::name)
    .ifPresent(System.out::println);
```

Both are functionally equivalent, but `Opts` allows more readable, linear traversal — especially in SQM’s deep polymorphic hierarchies.

---

#### ⚙️ Implementation Summary

`Opts` wraps each intermediate result in a lightweight `Chain<T>`:

```java
Opts.start(query)
    .then(Query::asSelect)        // Optional<SelectQuery>
    .thenMap(SelectQuery::joins)  // List<Join>
    .toOptional();                // Optional<List<Join>>
```

Each transformation step returns a new `Chain<U>`, and all nulls or empty results are handled automatically.

---

#### 🧩 Use Cases

- Safe traversal of nested SQM nodes without explicit null checks.  
- Building compact inspection or debugging utilities.  
- Replacing repetitive `flatMap` pipelines with a clean fluent syntax.

---

### Match API

The `Match` API provides a fluent, pattern-style mechanism to perform type-safe dispatching across SQM model node hierarchies. It replaces complex `instanceof` chains with a clear, functional interface for handling each node subtype.

#### Overview

A `Match<R>` represents a lazy evaluation of matching arms (handlers) against a specific model node. The result type `R` defines what each handler returns.

The core idea:

```java
var result = Queries.match(query)
    .select(q -> renderSelect(q))
    .with(w -> renderWith(w))
    .composite(c -> renderComposite(c))
    .orElseThrow(() -> new IllegalStateException("Unknown query type"));
```

Each subtype of the SQM hierarchy (e.g., `Query`, `Expression`, `Predicate`, `Join`) has a corresponding `Match` interface. These interfaces define dedicated handler registration methods for each subtype and terminate with `otherwise(...)` or convenience variants like `orElse(...)`.

#### Common Methods

| Method                        | Description                                                                        |
|-------------------------------|------------------------------------------------------------------------------------|
| `otherwise(Function<T,R> f)`  | Terminal operation that finalizes the match and applies `f` if no handler matched. |
| `otherwiseEmpty()`            | Returns an `Optional<R>` containing the matched result or empty if no arm matched. |
| `orElse(R defaultValue)`      | Returns the matched result or `defaultValue` if none matched.                      |
| `orElseGet(Supplier<R> s)`    | Returns the matched result or a lazily supplied default value.                     |
| `orElseThrow(Supplier<X> ex)` | Throws an exception if no handler matched.                                         |

The `Match` interface also includes an internal `sneakyThrow(Throwable)` helper to rethrow checked exceptions without declaring them. This enables fluent use of `orElseThrow(...)` with checked exceptions.

#### Specialized Matchers

Each major SQM node type has its own specialized matcher interface:

* **`QueryMatch<R>`** — matches query subtypes (`SelectQuery`, `WithQuery`, `CompositeQuery`).
* **`JoinMatch<R>`** — matches join types (`OnJoin`, `UsingJoin`, `NaturalJoin`, `CrossJoin`).
* **`ExpressionMatch<R>`** — matches expression types (`CaseExpr`, `ColumnExpr`, `FunctionExpr`, etc.).
* **`PredicateMatch<R>`** — matches predicate kinds (`BetweenPredicate`, `InPredicate`, `LikePredicate`, etc.).
* **`SelectItemMatch<R>`** — matches select item types (`ExprSelectItem`, `StarSelectItem`, `QualifiedStarSelectItem`).
* **`TableMatch<R>`** — matches table reference types (`Table`, `QueryTable`, `ValuesTable`).

Each provides fluent methods corresponding to their subtype structure. For example:

```java
String result = Predicates.match(predicate)
    .comparison(p -> renderComparison(p))
    .in(p -> renderIn(p))
    .isNull(p -> renderIsNull(p))
    .orElse("(unsupported predicate)");
```

#### Advantages

* Eliminates verbose `instanceof`/casting chains.
* Enforces type safety for each node subtype.
* Provides a single, expressive entry point per hierarchy.
* Integrates cleanly with functional style and lambda expressions.

#### Example

```java
String sql = Expressions.match(expr)
    .column(c -> c.tableAlias() == null ? c.name() : c.tableAlias() + "." + c.name())
    .literal(l -> String.valueOf(l.value()))
    .func(f -> f.name() + "(...)" )
    .orElse("<unknown expression>");
```

This example demonstrates how the `Match` API cleanly separates handling logic per subtype without requiring explicit type checks.

---

**See also:**

* `QueryMatch` — for matching query types.
* `PredicateMatch` — for predicate structures.
* `ExpressionMatch` — for handling all expression nodes.

Together, these interfaces provide a unified, type-safe approach for navigating and transforming the SQM model.

---

## 🧩 Core Modules

| Module              | Description                      |
|---------------------|----------------------------------|
| `sqm-core`          | Core model, renderers, DSL       |
| `sqm-parser`        | Base SQL parser interfaces       |
| `sqm-parser-ansi`   | ANSI SQL parser implementation   |
| `sqm-renderer`      | Base SQL renderer interfaces     |
| `sqm-renderer-ansi` | ANSI SQL renderer                |
| `sqm-json`          | JSON serialization mixins        |
| `sqm-it`            | SQM integration tests            |
| `examples`          | Code Examples                    |

---

## 🧱 Example Use Cases

- Building complex SQL dynamically in backend applications
- Converting SQL text into structured form for static analysis or auditing
- Generating dialect-specific SQL (PostgreSQL, SQL Server, etc.)
- Visual query builders or query explorers
- Integrating with DSL or JSON-based query definitions

---

## 🧪 Testing & Validation

SQM includes:
- Round-trip tests: SQL → Model → SQL (golden files)
- Fuzz & property tests: verify idempotency and equivalence
- Renderer compatibility checks per dialect
- JSON serialization consistency tests

---

## 🛠 Development Setup

```bash
git clone https://github.com/icher-g/sqm.git
cd sqm
mvn clean install
```

To run tests:
```bash
mvn test
```

---

## 📦 Maven Coordinates

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-core</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🧭 Roadmap

- [ ] Add support for parsing parameters in query (WHERE q = ?)
- [ ] Arithmetic operations in SQL statements (SELECT salary + bonus AS total_income)
- [ ] Add support for INSERT | UPDATE | DELETE | MERGE
- [ ] PostgresSQL renderer & parser
- [ ] SQL Server renderer & parser
- [ ] Add support for additional modifiers in function
- [ ] Query optimizer
- [ ] Query validator

---

## 🪪 License

Licensed under the **Apache License, Version 2.0**.  
See [LICENSE](LICENSE) for details.

---

## 📚 Learn More

- [Documentation (coming soon)](https://icher-g.github.io/sqm)
- [Project examples](examples/src/main/java/io/sqm/examples)
- [GitHub Issues](https://github.com/icher-g/sqm/issues)

---

### 🧠 About

**SQM (Structured Query Model)** is developed and maintained by [icher-g](https://github.com/icher-g).
