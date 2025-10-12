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
- **SpecParsers** — turn SQL strings into model objects.
- **Renderers** — convert model objects back into SQL (dialect-aware).
- **DSL Builders** — programmatic query construction.
- **JSON Mixins** — serialization/deserialization for external tools.

---

## 🚀 Quick Example

### Build a query with the DSL

```java
import static io.cherlabs.sqm.dsl.Dsl.*;

var q = query()
   .select(col("u", "user_name"), col("o", "status"), func("count", star()).as("cnt"))
   .from(table("orders").as("o"))
   .where(col("o", "status").in("A", "B"))
   .join(inner(table("users").as("u")).on(col("u", "id").eq(col("o", "user_id"))))
   .groupBy(group("u", "user_name"), group("o", "status"))
   .having(func("count", star()).gt(10));
```

**Rendered (ANSI):**
```sql
SELECT u.user_name, o.status, count(*) AS cnt
FROM orders AS o
INNER JOIN users AS u ON u.id = o.user_id
WHERE o.status in ('A', 'B')
GROUP BY u.user_name, o.status
HAVING count(*) > 10
```

---

### Parse SQL statements into a Model and Re-render

```java
import io.cherlabs.sqm.parser.dsl.QueryBuilder;

QueryBuilder qb = io.cherlabs.sqm.parser.dsl.QueryBuilder.newBuilder();

qb.select("u.user_name", "o.status", "count(*) AS cnt")
  .from("orders AS o")
  .where("o.status IN ('A','B')")
  .innerJoin("users AS u ON u.id = o.user_id")
  .groupBy("u.user_name, o.status")
  .having("count(*) > 10");

var q = qb.build();
var sql = Renderers.render(q).sql();

System.out.println(sql);
```

### Parse SQL into a Model

```java
import io.cherlabs.sqm.parser.Parsers;

var sql = """
    SELECT u.user_name, o.status, count(*) AS cnt
    FROM orders AS o
    INNER JOIN users AS u ON u.id = o.user_id
    WHERE o.status in ('A', 'B')
    GROUP BY u.user_name, o.status
    HAVING count(*) > 10""";

var parser = Parsers.defaultRepository().require(Query.class);
var pr = parser.parse(sql);
if (!pr.ok()) {
    throw new RuntimeException(pr.errorMessage());
}
var query = pr.value();
```


---

### Serialize to JSON

```java
ObjectMapper mapper = SqmMapperFactory.createDefault();
String json = mapper.writeValueAsString(model);
```

Output:
```json
{
  "kind" : "select",
  "columns" : [ 
    { "kind" : "func", "name" : "count", "args" : [ { "kind" : "star" } ], "distinct" : false, "alias" : "cnt" } 
  ],
  "joins" : [ 
    { "kind" : "table", "joinType" : "Inner", "table" : { "kind" : "table", "name" : "users", "alias" : "u", "schema" : null },
      "on" : { "kind" : "column", "column" : { "kind" : "named", "name" : "id", "alias" : null, "table" : "u" },
      "op" : "Eq",
      "values" : { "kind" : "column", "column" : { "kind" : "named", "name" : "user_id", "alias" : null, "table" : "o" } } }
  } ],
  "groupBy" : [ 
    { "column" : { "kind" : "named", "name" : "user_name", "alias" : null, "table" : "u" }, "ordinal" : false }, 
    { "column" : { "kind" : "named", "name" : "status", "alias" : null, "table" : "o" }, "ordinal" : false } 
  ],
  "orderBy" : [ ],
  "name" : null,
  "table" : { "kind" : "table", "name" : "orders", "alias" : "o", "schema" : null },
  "where" : { "kind" : "column", "column" : { "kind" : "named", "name" : "status", "alias" : null, "table" : "o" },
    "op" : "In",
    "values" : { "kind" : "list", "items" : [ "A", "B" ] }
  },
  "having" : { "kind" : "column", "column" : { "kind" : "func", "name" : "count", "args" : [ { "kind" : "star" } ], "distinct" : false,  "alias" : null
    },
    "op" : "Gt",
    "values" : { "kind" : "single", "value" : 10 }
  },
  "distinct" : null,
  "limit" : null,
  "offset" : null
}
```

---

## 🧩 Core Modules

| Module | Description |
|--------|--------------|
| `sqm-core` | Core model, renderers, DSL |
| `sqm-parser` | Default SQL parser implementation |
| `sqm-renderer` | Base SQL renderer interfaces |
| `sqm-renderer-ansi` | ANSI SQL renderer |
| `sqm-json` | JSON serialization mixins |
| `sqm-it` | SQM integration tests |

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
  <groupId>io.cherlabs</groupId>
  <artifactId>sqm</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🧭 Roadmap

- [ ] Arithmetic operations in SQL statements (SELECT salary + bonus AS total_income)
- [ ] Add support for INSERT | UPDATE | DELETE | MERGE
- [ ] PostgreSQL renderer & parser
- [ ] SQL Server renderer & parser
- [ ] Function & operator registry
- [ ] Query optimizer & rewrite utilities
- [ ] AST visualization & JSON schema

---

## 🪪 License

Licensed under the **Apache License, Version 2.0**.  
See [LICENSE](LICENSE) for details.

---

## 📚 Learn More

- [Documentation (coming soon)](https://icher-g.github.io/sqm)
- [Project examples](examples/)
- [GitHub Issues](https://github.com/icher-g/sqm/issues)

---

### 🧠 About

**SQM (Structured Query Model)** is developed and maintained by [icher-g](https://github.com/icher-g).  
It evolved from the original `sql-model` project, renamed to avoid conflicts with the Python `SQLModel` library and to better represent its purpose.
