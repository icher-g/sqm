# SQM — Structured Query Model for Java

[![Build](https://github.com/cherlabs/sqm/actions/workflows/ci.yml/badge.svg)](https://github.com/cherlabs/sqm/actions)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.cherlabs/sqm.svg)](https://central.sonatype.com/artifact/io.cherlabs/sqm)

**SQM (Structured Query Model)** is a lightweight Java library for modeling SQL queries as composable, strongly-typed objects.  
It enables **bidirectional transformations** between SQL, JSON, and DSL forms — making it ideal for query generation, analysis, rewriting, and serialization across dialects.

---

## ✨ Features

- 🧩 **Structured model** — fully object-oriented representation of SQL (Query, Table, Column, Filter, Join, etc.)
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
- [ ] Add support for parsing SELECT from sub query (SELECT * FROM (SELECT * FROM))
- [ ] Arithmetic operations in SQL statements (SELECT salary + bonus AS total_income)
- [ ] Add support for INSERT | UPDATE | DELETE | MERGE
- [ ] PostgreSQL renderer & parser
- [ ] SQL Server renderer & parser
- [ ] Query optimizer & rewrite utilities

---

## 🪪 License

Licensed under the **Apache License, Version 2.0**.  
See [LICENSE](LICENSE) for details.

---

## 📚 Learn More

- [Documentation (coming soon)](https://icher-g.github.io/sqm)
- [Project examples](examples/src/main/java/io/sqm/examples/)
- [GitHub Issues](https://github.com/icher-g/sqm/issues)

---

### 🧠 About

**SQM (Structured Query Model)** is developed and maintained by [icher-g](https://github.com/icher-g).  
It evolved from the original `sql-model` project, renamed to avoid conflicts with the Python `SQLModel` library and to better represent its purpose.
