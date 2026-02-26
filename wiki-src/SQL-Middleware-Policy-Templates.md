# SQL Middleware Policy Templates

This page provides copy-paste starter templates for common middleware rollout profiles.

All templates use `SqlMiddlewareConfig.builder(schema)` and differ only in strictness and rewrite behavior.

`schema` can be created manually in code or loaded from JSON/JDBC.
For alternatives, see:

- [SQL Middleware Framework](SQL-Middleware-Framework) (Schema Source Options)
- [Schema Introspection](Schema-Introspection)

### Reusable Schema Loader Snippet

Use this helper once and call it from any template below.

```java
static CatalogSchema loadSchema(String mode, DataSource ds, Path snapshotPath) {
    return switch (mode) {
        case "manual" -> CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
        );
        case "json" -> JsonSchemaProvider.of(snapshotPath).load();
        case "jdbc" -> JdbcSchemaProvider.of(
            ds,
            null,
            "public",
            List.of("TABLE", "VIEW"),
            PostgresSqlTypeMapper.standard()
        ).load();
        default -> throw new IllegalArgumentException("Unsupported schema mode: " + mode);
    };
}

CatalogSchema schema = loadSchema("json", dataSource, Path.of("schema.json"));
```

## Template 1: Read-Only Analytics (Safe Default)

Use when AI is allowed to query but never modify data.

```java
// Schema source: use loadSchema(...) helper from snippet above.

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .guardrails(new RuntimeGuardrails(10_000, 2_000L, 500, false))
        .buildValidationConfig()
);

var decision = middleware.analyze(sql, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
```

Behavior:

- validates SQL against schema/policy
- denies DDL/DML in execute-intent flows
- does not rewrite SQL

## Template 2: Rewrite-First Execution

Use when you want middleware to shape SQL before execution (e.g., inject `LIMIT`).

```java
// Schema source: use loadSchema(...) helper from snippet above.

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
        .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.CANONICALIZATION)
        .guardrails(new RuntimeGuardrails(10_000, 2_000L, 1000, false))
        .buildValidationAndRewriteConfig()
);

var context = ExecutionContext.of(
    "postgresql", "agent", "tenant-a", ExecutionMode.EXECUTE, ParameterizationMode.BIND
);
var decision = middleware.enforce(sql, context);
```

Behavior:

- validates SQL
- applies configured rewrites
- renders rewritten SQL with bind parameters when `ParameterizationMode.BIND`

## Template 3: Strict Guardrails + Dry-Run

Use for canary rollout when you want execute-intent requests converted to `EXPLAIN` SQL.

```java
// Schema source: use loadSchema(...) helper from snippet above.

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
        .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
        .guardrails(new RuntimeGuardrails(8_000, 1_000L, 200, true))
        .buildValidationAndRewriteConfig()
);

var decision = middleware.enforce(sql, ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
```

Behavior:

- evaluate policy and rewrites normally
- return `REWRITE` decision with `EXPLAIN <effective-sql>` in execute mode
- does not execute SQL (caller remains execution owner)

## Template 4: Custom Validator + Custom Rewriter

Use when you need business-specific rules beyond built-ins.

```java
// Schema source: use loadSchema(...) helper from snippet above.

SqlQueryValidator validator = (query, context) -> {
    if (context.tenant() == null || context.tenant().isBlank()) {
        return QueryValidateResult.failure(ReasonCode.DENY_VALIDATION, "tenant is required");
    }
    return QueryValidateResult.ok();
};

QueryRewriteRule noopRule = (query, context) -> QueryRewriteResult.unchanged(query);

var middleware = SqlMiddleware.create(
    SqlMiddlewareConfig.builder(schema)
        .queryValidator(validator)
        .queryRewriter(SqlQueryRewriter.chain(noopRule))
        .queryRenderer(SqlQueryRenderer.standard())
        .buildValidationAndRewriteConfig()
);
```

Behavior:

- plugs custom logic into the same decision pipeline
- keeps middleware contract unchanged for callers

## Decision Handling Snippet

```java
switch (decision.kind()) {
    case ALLOW -> executeOriginal(sql);
    case REWRITE -> execute(decision.rewrittenSql(), decision.sqlParams());
    case DENY -> throw new SecurityException(decision.reasonCode() + ": " + decision.message());
}
```

## Related Pages

- [SQL Middleware Framework](SQL-Middleware-Framework)
- [Examples Module Guide](Examples-Module-Guide)
