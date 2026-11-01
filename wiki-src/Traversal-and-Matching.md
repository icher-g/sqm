# Traversal and Matching

Examples use a common query with a join:

```java
import io.sqm.core.*;
import io.sqm.core.match.Match;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import java.util.LinkedHashSet;
import java.util.Set;
import static io.sqm.dsl.Dsl.*;

var query = select(col("u", "user_name"), col("o", "status"))
    .from(tbl("users").as("u"))
    .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
    .build();
Query originalQuery = query;
var predicate = col("u", "id").eq(lit(7L));
var expr = col("u", "user_name");
```

The SQM model provides powerful traversal and transformation mechanisms built on top of the **Visitor pattern**.
Two common examples are *collectors* (for extracting information from a query tree) and *transformers* (for producing modified copies).

---

### Column Collector Example

A **collector** walks through the query tree and gathers information, such as all referenced column names.
Collectors typically extend `RecursiveNodeVisitor<R>` and accumulate results internally.

```java
class QueryColumnCollector extends RecursiveNodeVisitor<Void> {
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
        columns.add(c.tableAlias() == null
            ? c.name().value()
            : c.tableAlias().value() + "." + c.name().value());
        return super.visitColumnExpr(c);
    }
}
```

#### Usage

```java
var collector = new QueryColumnCollector();
query.accept(collector);
Set<String> usedColumns = collector.getColumns();
usedColumns.forEach(System.out::println);
```

This visitor automatically traverses all expressions, so you only need to override the specific node type you want to handle (in this case `ColumnExpr`).
The recursive base visitor (`RecursiveNodeVisitor`) ensures all sub-nodes are visited.

---

### Column Transformer Example

A **transformer** produces a modified copy of the query tree.
Transformers extend `RecursiveNodeTransformer` (a subclass of `NodeTransformer<Node>`) and return new node instances where changes are required.

```java
class RenameColumnTransformer extends RecursiveNodeTransformer {
    @Override
    public Node visitColumnExpr(ColumnExpr c) {
        if (c.tableAlias() != null
            && "u".equals(c.tableAlias().value())
            && "id".equals(c.name().value())) {
            return col("u", "user_id");
        }
        return c;
    }
}
```

#### Usage

```java
var transformer = new RenameColumnTransformer();
var transformed = (Query) transformer.transform(originalQuery);
```

The `RecursiveNodeTransformer` automatically handles traversal and reconstruction of immutable nodes.
You only override methods for nodes you wish to modify - all others are traversed and returned unchanged.

---

### Summary

| Concept                             | Description                                                  |
|-------------------------------------|--------------------------------------------------------------|
| **Visitor**                         | Walks the query tree and can perform analysis or collection. |
| **Transformer**                     | Walks the query tree and produces a modified copy.           |
| **Recursive Visitors/Transformers** | Provide automatic traversal of all subnodes.                 |
| **Collectors**                      | Typically accumulate results in a `Set`, `List`, or `Map`.   |
| **Transformers**                    | Typically override a few methods to replace or rename nodes. |

---

### Typical Use Cases

- Collect all referenced tables or columns in a query.
- Rewrite column names, table aliases, or function calls.
- Apply dialect-specific rewrites or optimizations.
- Extract metadata for validation or analysis.
- Implement automatic query normalization or anonymization.

---

## Match API

The `Match` API provides a fluent, pattern-style mechanism to perform type-safe dispatching across SQM model node hierarchies. It replaces complex `instanceof` chains with a clear, functional interface for handling each node subtype.

### Overview

A `Match<R>` represents a lazy evaluation of matching arms (handlers) against a specific model node. The result type `R` defines what each handler returns.

The core idea:

```java
var queryKind = Match
    .<String>query(query)
    .select(q -> "SELECT")
    .with(w -> "WITH")
    .composite(c -> "set operation")
    .orElseThrow(() -> new IllegalStateException("Unknown query type"));
```

Each subtype of the SQM hierarchy (e.g., `Query`, `Expression`, `Predicate`, `Join`) has a corresponding `Match` interface. These interfaces define dedicated handler registration methods for each subtype and terminate with `otherwise(...)` or convenience variants like `orElse(...)`.

### Common Methods

| Method                        | Description                                                                        |
|-------------------------------|------------------------------------------------------------------------------------|
| `otherwise(Function<T,R> f)`  | Terminal operation that finalizes the match and applies `f` if no handler matched. |
| `otherwiseEmpty()`            | Returns an `Optional<R>` containing the matched result or empty if no arm matched. |
| `orElse(R defaultValue)`      | Returns the matched result or `defaultValue` if none matched.                      |
| `orElseGet(Supplier<R> s)`    | Returns the matched result or a lazily supplied default value.                     |
| `orElseThrow(Supplier<X> ex)` | Throws an exception if no handler matched.                                         |

The `Match` interface also includes an internal `sneakyThrow(Throwable)` helper to rethrow checked exceptions without declaring them. This enables fluent use of `orElseThrow(...)` with checked exceptions.

### Specialized Matchers

Each major SQM node type has its own specialized matcher interface:

* **`QueryMatch<R>`** - matches query subtypes (`SelectQuery`, `WithQuery`, `CompositeQuery`).
* **`JoinMatch<R>`** - matches join types (`OnJoin`, `UsingJoin`, `NaturalJoin`, `CrossJoin`).
* **`ExpressionMatch<R>`** - matches expression types (`CaseExpr`, `ColumnExpr`, `FunctionExpr`, etc.).
* **`PredicateMatch<R>`** - matches predicate kinds (`BetweenPredicate`, `InPredicate`, `LikePredicate`, etc.).
* **`SelectItemMatch<R>`** - matches select item types (`ExprSelectItem`, `StarSelectItem`, `QualifiedStarSelectItem`).
* **`TableMatch<R>`** - matches table reference types (`Table`, `QueryTable`, `ValuesTable`).
* etc.

Each provides fluent methods corresponding to their subtype structure. For example:

```java
String predicateKind = Match
    .<String>predicate(predicate)
    .comparison(p -> "comparison")
    .in(p -> "IN")
    .isNull(p -> "IS NULL")
    .orElse("(unsupported predicate)");
```

### Advantages

* Eliminates verbose `instanceof`/casting chains.
* Enforces type safety for each node subtype.
* Provides a single, expressive entry point per hierarchy.
* Integrates cleanly with functional style and lambda expressions.

### Expression Match Example

```java
String expressionDescription = Match
    .<String>expression(expr)
    .column(c -> c.tableAlias() == null
        ? c.name().value()
        : c.tableAlias().value() + "." + c.name().value())
    .literal(l -> String.valueOf(l.value()))
    .func(f -> String.join(".", f.name().values()) + "(...)" )
    .orElse("<unknown expression>");
```

This example demonstrates how the `Match` API cleanly separates handling logic per subtype without requiring explicit type checks.

### Accessing Specific Field Example

```text
SELECT u.user_name, o.status, count(*) AS cnt
FROM orders AS o
INNER JOIN users AS u ON u.id = o.user_id
```

```java
// get the name of the column in a join.
// here the Match class is called internally on each sub type.
var columnName = query.matchQuery()
    .select(s -> s.joins().getFirst().matchJoin()
        .on(j -> j.on().matchPredicate()
            .comparison(cmp -> cmp.rhs().matchExpression()
                .column(c -> c.name().value())
                .orElse(null) // each match needs to provide alternative.
            )
            .orElse(null)
        )
        .orElse(null)
    )
    .orElse(null);
```

In this example a specific node is accessed via matchX() methods. Each matchX() method calls Match.x() internally.

## Task-oriented alternatives

For common renames, relation remapping, predicate injection, or parameterization,
start with [Query Transform Helpers](Query-Transform-Helpers). Use a visitor or
transformer when the rewrite requires node-specific logic. Unchanged nodes retain
their identity; changed nodes are reconstructed as immutable values.

See [Core Model](Core-Model), [Identifier Transforms](Identifier-Transforms), and
[Literal Transforms](Literal-Transforms).
