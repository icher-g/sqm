# SQL Middleware Framework (`sqm-control`)

`sqm-control` is the policy and decision layer for SQL produced by users, services, or AI systems.

It provides a deterministic pipeline:

`parse -> validate -> rewrite -> render -> decision`

with framework entry points:

- `analyze(sql, context)`
- `enforce(sql, context)`
- `explainDecision(sql, context)`

## Core Concepts

- `ExecutionContext` — dialect, principal, tenant, mode, and parameterization mode.
- `DecisionResult` — `ALLOW`, `DENY`, or `REWRITE` with reason code, optional rewritten SQL, params, fingerprint.
- `SqlMiddlewareConfig.Builder` — composition point for default and custom middleware wiring.
- `BuiltInRewriteRules` — source of built-in `QueryRewriteRule` lists.
- `SqlQueryRewriter.builder()` — composes configured built-in/custom rules into an executable rewrite pipeline.

## Schema Source Options (Manual / JSON / DB)

All middleware examples in this page use a `CatalogSchema schema` variable. You can provide it from multiple sources.

### Option A: Manual in code (quick start)

```java
CatalogSchema schema = CatalogSchema.of(
    CatalogTable.of("public", "users",
        CatalogColumn.of("id", CatalogType.LONG),
        CatalogColumn.of("name", CatalogType.STRING),
        CatalogColumn.of("active", CatalogType.BOOLEAN)
    )
);
```

### Option B: JSON snapshot (recommended default for deterministic environments)

```java
// Requires sqm-catalog module
CatalogSchema schema = JsonSchemaProvider.of(Path.of("schema.json")).load();
```

### Option C: Live DB introspection via JDBC (recommended for refresh workflows)

```java
// Requires sqm-catalog + JDBC DataSource
DataSource ds = ...;
CatalogSchema schema = JdbcSchemaProvider.of(
    ds,
    null,
    "public",
    List.of("TABLE", "VIEW"),
    PostgresSqlTypeMapper.standard()
).load();
```

Recommended workflow:

1. Load from JDBC in controlled environment.
2. Save as JSON snapshot.
3. Use JSON snapshot in default app/runtime builds.

See detailed provider docs:

- [Schema Introspection](Schema-Introspection)
- [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation)

## 1) Default Usage: Validation-Only Flow

Use this when you only want policy validation without SQL rewriting.

```java
var schema = CatalogSchema.of(
    CatalogTable.of("public", "users",
        CatalogColumn.of("id", CatalogType.LONG),
        CatalogColumn.of("name", CatalogType.STRING),
        CatalogColumn.of("active", CatalogType.BOOLEAN)
    )
);

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .buildValidationConfig()
);

var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
var decision = middleware.analyze("select id, name from users where active = true", context);
```

## 2) Full Flow: Validation + Rewrite + Render

Use this when you need normalized/restricted SQL output and deterministic reason codes.

```java
var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .builtInRewriteSettings(
            BuiltInRewriteSettings.builder()
                .defaultLimitInjectionValue(1000)
                .build()
        )
        .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.CANONICALIZATION)
        .buildValidationAndRewriteConfig()
);

var context = ExecutionContext.of(
    "postgresql", "agent", "tenant-a", ExecutionMode.EXECUTE, ParameterizationMode.BIND
);

var decision = middleware.enforce("select id from users where id = 7", context);

if (decision.kind() == DecisionKind.REWRITE) {
    String sql = decision.rewrittenSql();
    List<Object> params = decision.sqlParams();
}
```

## 3) Parameterization Modes

`ExecutionContext` controls renderer output mode:

- `ParameterizationMode.OFF` → inline literals in rewritten SQL
- `ParameterizationMode.BIND` → placeholders + `DecisionResult.sqlParams()`

This is renderer-driven behavior (not a standalone rewrite rule).

## 4) Runtime Guardrails

Guardrails are evaluated in middleware runtime flow:

- max SQL length
- timeout
- max rows (`LIMIT` guardrail)
- optional `EXPLAIN` dry-run in `enforce(...)`

```java
var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .guardrails(new RuntimeGuardrails(10_000, 2_000L, 100, false))
        .buildValidationConfig()
);
```

## 5) Custom Implementations

All major pipeline components are replaceable.

### 5.1 Custom Parser

```java
var parser = SqlQueryParser.dialectAware(Map.of(
    "ansi", AnsiSpecs::new,
    "postgresql", PostgresSpecs::new
));

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .queryParser(parser)
        .buildValidationConfig()
);
```

### 5.2 Custom Validator

```java
SqlQueryValidator validator = (query, context) -> {
    if (context.tenant() == null) {
        return QueryValidateResult.failure(ReasonCode.DENY_VALIDATION, "tenant is required");
    }
    return QueryValidateResult.ok();
};

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .queryValidator(validator)
        .buildValidationConfig()
);
```

### 5.3 Custom Rewriter

```java
QueryRewriteRule noOpRule = (query, context) -> QueryRewriteResult.unchanged(query);

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .queryRewriter(SqlQueryRewriter.chain(noOpRule))
        .queryRenderer(SqlQueryRenderer.standard())
        .buildValidationAndRewriteConfig()
);
```

### 5.3.1 Advanced: Compose from BuiltInRewriteRules Directly

Use this when you want explicit control over how built-in rule lists are selected and composed.

```java
Set<BuiltInRewriteRule> selected = Set.of(
    BuiltInRewriteRule.LIMIT_INJECTION,
    BuiltInRewriteRule.CANONICALIZATION
);

List<QueryRewriteRule> builtInRules = BuiltInRewriteRules.selected(
    BuiltInRewriteSettings.builder().defaultLimitInjectionValue(500).build(),
    selected
);

SqlQueryRewriter rewriter = SqlQueryRewriter.chain(
    builtInRules.toArray(QueryRewriteRule[]::new)
);

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .queryRewriter(rewriter)
        .queryRenderer(SqlQueryRenderer.standard())
        .buildValidationAndRewriteConfig()
);
```

### 5.4 Custom Renderer

```java
SqlQueryRenderer renderer = SqlQueryRenderer.standard();

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .queryRenderer(renderer)
        .buildValidationAndRewriteConfig()
);
```

### 5.5 Custom Explainer and Audit Publisher

```java
var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .explainer((query, ctx, decision) -> "kind=" + decision.kind() + ", reason=" + decision.reasonCode())
        .auditPublisher(event -> System.out.println(event.decision().kind() + " " + event.normalizedSql()))
        .buildValidationConfig()
);
```

## 6) Decision Handling Patterns

### Allow

```java
if (decision.kind() == DecisionKind.ALLOW) {
    executeOriginalSql();
}
```

### Rewrite

```java
if (decision.kind() == DecisionKind.REWRITE) {
    execute(decision.rewrittenSql(), decision.sqlParams());
}
```

### Deny

```java
if (decision.kind() == DecisionKind.DENY) {
    throw new SecurityException(decision.reasonCode() + ": " + decision.message());
}
```

## 7) Practical Reference

- Example class: `examples/src/main/java/io/sqm/examples/Middleware_EndToEndPolicyFlow.java`
- Docker-backed middleware integration: `sqm-it/src/test/java/io/sqm/it/PostgresMiddlewareIntegrationTest.java`
- Docker-free flow checks: `sqm-control/src/test/java/io/sqm/control/AiSqlMiddlewareFlowTest.java`
