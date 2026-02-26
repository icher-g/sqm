# SQM Wiki

SQM is a Java library for SQL modeling, parsing, rendering, validation, and code generation.

## Start Here

- [Getting Started](Getting-Started)
- [Examples Module Guide](Examples-Module-Guide)
- [Troubleshooting](Troubleshooting)

## Middleware Quickstart

```java
var middleware = SqlMiddleware.create(
  SqlMiddlewareConfig.builder(schema)
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

## Top Commands

```bash
mvn -B test
mvn -B -pl examples -am test
mvn -B -pl sqm-codegen-maven-plugin -am test
mvn -B -pl sqm-codegen-maven-plugin -am verify -Pdocker-it -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
```

