# SQM Wiki

SQM is a Java library for SQL modeling, parsing, rendering, validation, and code generation.

Support note:

- nodes documented in the shared model are representable by SQM
- dialect support remains parser/render/validate/transpile specific
- see [Core Model](Core-Model), `docs/model/MODEL.md`, and [Unsupported Features](Unsupported-Features) for the distinction

## Start Here

- [Getting Started](Getting-Started)
- [Examples Module Guide](Examples-Module-Guide)
- [Troubleshooting](Troubleshooting)

## Middleware Quickstart

```java
var middleware = SqlDecisionService.create(
  SqlDecisionServiceConfig.builder(schema)
    .validationSettings(SchemaValidationSettings.defaults())
    .builtInRewriteSettings(
      BuiltInRewriteSettings.builder()
        .defaultLimitInjectionValue(1000)
        .build()
    )
    .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
    .buildValidationAndRewriteConfig()
);

var decision = middleware.enforce(
  sql,
  ExecutionContext.of("postgresql", "agent", "tenant-a", ExecutionMode.EXECUTE, ParameterizationMode.BIND)
);
```

More:

- [SQL Middleware Framework](SQL-Middleware-Framework)
- [SQL Middleware Policy Templates](SQL-Middleware-Policy-Templates)

## Feature Index

- Core model and traversal:
  - [Core Model](Core-Model)
  - [DSL Usage](DSL-Usage)
- SQL processing:
  - [Parsing SQL](Parsing-SQL)
  - [Rendering SQL](Rendering-SQL)
  - [SQL Server Dialect](SQL-Server-Dialect)
  - [Unsupported Features](Unsupported-Features)
  - [SQL Transpilation](SQL-Transpilation)
  - [SQL Transpilation Examples](SQL-Transpilation-Examples)
  - [SQL Middleware Framework](SQL-Middleware-Framework)
- Serialization:
  - [JSON Serialization](JSON-Serialization)
- Semantic validation:
  - [Schema Validation](Schema-Validation)
  - [PostgreSQL Validation](PostgreSQL-Validation)
- Schema sources:
  - [Schema Introspection](Schema-Introspection)
- Build-time generation:
- [SQL File Codegen](SQL-File-Codegen)
  - [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation)
- Testing and operations:
  - [Integration Testing with Docker](Integration-Testing-with-Docker)
  - [FAQ](FAQ)

Current shipped model highlights:

- shared typed statement and table hints
- statement-level `hints()` on `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `MERGE`
- cross-dialect hint dropping with warnings during transpilation when a target dialect does not share the source hint system

## Top Commands

```bash
mvn -B test
mvn -B -pl examples -am test
mvn -B -pl sqm-codegen-maven-plugin -am test
mvn -B -pl sqm-codegen-maven-plugin -am verify -Pdocker-it -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
```

