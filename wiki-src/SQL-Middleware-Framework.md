# SQL Middleware Framework (`sqm-control`)

`sqm-control` is the policy and decision layer for SQL produced by users, services, or AI systems.

It provides a deterministic pipeline:

`parse -> validate -> rewrite -> render -> decision`

with framework entry points:

- `analyze(sql, context)`
- `enforce(sql, context)`
- `explainDecision(sql, context)`

## Core Concepts

- `ExecutionContext` â€” dialect, principal, tenant, mode, and parameterization mode.
- `DecisionResult` â€” `ALLOW`, `DENY`, or `REWRITE` with reason code, optional rewritten SQL, params, fingerprint.
- `SqlDecisionServiceConfig.Builder` â€” composition point for default and custom middleware wiring.
- `BuiltInRewriteRules` â€” source of built-in `StatementRewriteRule` lists.
- `SqlStatementRewriter.builder()` â€” composes configured built-in/custom rules into an executable rewrite pipeline.

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

## Tenant-Aware Validation Settings (JSON/YAML)

Middleware can load tenant-scoped access rules (inside one access policy object) from inline config keys:

- `sqm.validation.settings.json` / `SQM_VALIDATION_SETTINGS_JSON`
- `sqm.validation.settings.yaml` / `SQM_VALIDATION_SETTINGS_YAML`
- optional runtime strictness override:
  - `sqm.middleware.validation.tenantRequirementMode`
  - `SQM_MIDDLEWARE_VALIDATION_TENANT_REQUIREMENT_MODE`

Example JSON:

```json
{
  "tenantRequirementMode": "REQUIRED",
  "accessPolicy": {
    "tenants": [
      {
        "name": "tenant-a",
        "deniedTables": ["payments"],
        "deniedColumns": ["users.ssn"]
      },
      {
        "name": "tenant-b",
        "deniedTables": ["audit_logs"]
      }
    ]
  }
}
```

Equivalent YAML:

```yaml
tenantRequirementMode: REQUIRED
accessPolicy:
  tenants:
    - name: tenant-a
      deniedTables:
        - payments
      deniedColumns:
        - users.ssn
    - name: tenant-b
      deniedTables:
        - audit_logs
```

Evaluation precedence per request:

- `global` + `principal` + `tenant` + `tenant+principal`
- deny rules are additive across these scopes
- function allowlists are additive across these scopes

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

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .buildValidationConfig()
);

var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
var decision = middleware.analyze("select id, name from users where active = true", context);
```

## 2) Full Flow: Validation + Rewrite + Render

Use this when you need normalized/restricted SQL output and deterministic reason codes.

```java
var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .builtInRewriteSettings(
            BuiltInRewriteSettings.builder()
                .defaultLimitInjectionValue(1000)
                .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
                .build()
        )
        .rewriteRules(
            BuiltInRewriteRule.LIMIT_INJECTION,
            BuiltInRewriteRule.TENANT_PREDICATE,
            BuiltInRewriteRule.CANONICALIZATION
        )
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

MySQL DML flows are supported through the same middleware entry points:

```java
var mysqlDecision = middleware.enforce(
    "insert ignore into users (id, name) values (1, 'alice')",
    ExecutionContext.of("mysql", "agent", "tenant-a", ExecutionMode.EXECUTE, ParameterizationMode.OFF)
);
```

## 3) Parameterization Modes

`ExecutionContext` controls renderer output mode:

- `ParameterizationMode.OFF` â†’ inline literals in rewritten SQL
- `ParameterizationMode.BIND` â†’ placeholders + `DecisionResult.sqlParams()`

This is renderer-driven behavior (not a standalone rewrite rule).

## 4) Runtime Guardrails

Guardrails are evaluated in middleware runtime flow:

- max SQL length
- timeout
- max rows (`LIMIT` guardrail)
- optional `EXPLAIN` dry-run in `enforce(...)`

```java
var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .guardrails(new RuntimeGuardrails(10_000, 2_000L, 100, false))
        .buildValidationConfig()
);
```

## 5) Custom Implementations

All major pipeline components are replaceable.

### 5.1 Custom Parser

```java
var parser = SqlStatementParser.dialectAware(Map.of(
    "ansi", AnsiSpecs::new,
    "postgresql", PostgresSpecs::new,
    "mysql", MySqlSpecs::new
));

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
    .statementParser(parser)
        .buildValidationConfig()
);
```

### 5.2 Custom Validator

```java
SqlStatementValidator validator = (query, context) -> {
    if (context.tenant() == null) {
        return StatementValidateResult.failure(ReasonCode.DENY_VALIDATION, "tenant is required");
    }
    return StatementValidateResult.ok();
};

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
    .statementValidator(validator)
        .buildValidationConfig()
);
```

### 5.3 Custom Rewriter

```java
StatementRewriteRule noOpRule = (query, context) -> StatementRewriteResult.unchanged(query);

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .statementRewriter(SqlStatementRewriter.chain(noOpRule))
    .statementRenderer(SqlStatementRenderer.standard())
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

List<StatementRewriteRule> builtInRules = BuiltInRewriteRules.selected(
    BuiltInRewriteSettings.builder().defaultLimitInjectionValue(500).build(),
    selected
);

SqlStatementRewriter rewriter = SqlStatementRewriter.chain(
    builtInRules.toArray(StatementRewriteRule[]::new)
);

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .statementRewriter(rewriter)
    .statementRenderer(SqlStatementRenderer.standard())
        .buildValidationAndRewriteConfig()
);
```

### 5.4 Custom Renderer

```java
SqlStatementRenderer renderer = SqlStatementRenderer.standard();

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
    .statementRenderer(renderer)
        .buildValidationAndRewriteConfig()
);
```

### 5.5 Custom Explainer and Audit Publisher

```java
var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
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


## Runtime transports

See [Middleware Transports](Middleware-Transports) for REST/MCP startup,
request framing, endpoints, error contracts, configuration, and named-pipe tools.

## Runtime validation settings and access policies



`SqlDecisionServiceConfig.builder(schema)` now resolves validation settings in this order:

1. explicit `validationSettings(...)`
2. explicit `validationSettingsJson(...)` / `validationSettingsYaml(...)`
3. runtime config keys:
   - `sqm.validation.settings.json` or `SQM_VALIDATION_SETTINGS_JSON`
   - `sqm.validation.settings.yaml` or `SQM_VALIDATION_SETTINGS_YAML`
4. fallback: `SchemaValidationSettings.defaults()`

Example JSON payload:

```json
{
  "accessPolicy": {
    "deniedTables": ["users"],
    "deniedColumns": ["users.secret"],
    "allowedFunctions": ["count", "lower"],
    "principals": [
      {
        "name": "analyst",
        "deniedTables": ["payments"],
        "deniedColumns": ["users.email"],
        "allowedFunctions": ["sum", "avg"]
      }
    ]
  },
  "limits": {
    "maxJoinCount": 5,
    "maxSelectColumns": 50
  }
}
```

Equivalent YAML payload:

```yaml
accessPolicy:
  deniedTables:
    - users
  deniedColumns:
    - users.secret
  allowedFunctions:
    - count
    - lower
  principals:
    - name: analyst
      deniedTables:
        - payments
      deniedColumns:
        - users.email
      allowedFunctions:
        - sum
        - avg
limits:
  maxJoinCount: 5
  maxSelectColumns: 50
```
