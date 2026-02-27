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
- `BuiltInRewriteRules` â€” source of built-in `QueryRewriteRule` lists.
- `SqlQueryRewriter.builder()` â€” composes configured built-in/custom rules into an executable rewrite pipeline.

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
var parser = SqlQueryParser.dialectAware(Map.of(
    "ansi", AnsiSpecs::new,
    "postgresql", PostgresSpecs::new
));

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
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

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .queryValidator(validator)
        .buildValidationConfig()
);
```

### 5.3 Custom Rewriter

```java
QueryRewriteRule noOpRule = (query, context) -> QueryRewriteResult.unchanged(query);

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
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

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .queryRewriter(rewriter)
        .queryRenderer(SqlQueryRenderer.standard())
        .buildValidationAndRewriteConfig()
);
```

### 5.4 Custom Renderer

```java
SqlQueryRenderer renderer = SqlQueryRenderer.standard();

var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .queryRenderer(renderer)
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

## 8) Runtime Transport Hosts

The middleware service can be hosted in two runtime transports:

- REST host (`sqm-middleware-rest`) â€” Spring Boot HTTP API
- MCP host (`sqm-middleware-mcp`) â€” long-running stdio JSON-RPC server

Both hosts delegate to the same transport-neutral service (`SqlMiddlewareService`) so decision behavior remains parity-aligned.

### 8.1 REST host

Run:

```bash
mvn -pl sqm-middleware-rest -am spring-boot:run
```

Endpoints:

- `POST /sqm/middleware/analyze`
- `POST /sqm/middleware/enforce`
- `POST /sqm/middleware/explain`

### 8.2 MCP host (stdio JSON-RPC)

Run:

```bash
mvn -pl sqm-middleware-mcp -am exec:java -Dexec.mainClass=io.sqm.middleware.mcp.SqlMiddlewareMcpApplication
```

The MCP host is a persistent process waiting for framed messages on stdin and writing framed responses on stdout.

Frame format:

```text
Content-Length: <bytes>

<json-rpc-body>
```

Common requests:

`initialize`

```text
Content-Length: 58

{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
```

`tools/list`

```text
Content-Length: 58

{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

`tools/call` (`middleware.enforce`)

```text
Content-Length: 205

{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"middleware.enforce","arguments":{"sql":"select id from users","context":{"dialect":"postgresql","mode":"EXECUTE","parameterizationMode":"OFF"}}}}
```

Tool names:

- `middleware.analyze`
- `middleware.enforce`
- `middleware.explain`

### 8.3 Host configuration (REST + MCP)

Both hosts load runtime configuration from Java system properties (or corresponding environment variables):

Complete generated key table (single source of truth):
- `docs/MIDDLEWARE_CONFIG_KEYS.md` (generated from `ConfigKeys`; run `scripts/generate-middleware-config-keys-doc.ps1`)

- `sqm.middleware.schema.source` (`manual|json|jdbc`)
- `sqm.middleware.schema.defaultJson.path` (optional file path used by `manual` source)
- `sqm.middleware.schema.json.path`
- `sqm.middleware.jdbc.url`, `sqm.middleware.jdbc.user`, `sqm.middleware.jdbc.password`
- `sqm.middleware.jdbc.driver`, `sqm.middleware.jdbc.schemaPattern`
- `sqm.middleware.rewrite.enabled`
- `sqm.middleware.rewrite.rules` (comma-separated built-in names)
- `sqm.middleware.validation.maxJoinCount`, `sqm.middleware.validation.maxSelectColumns`
- `sqm.middleware.guardrails.maxSqlLength`, `sqm.middleware.guardrails.timeoutMillis`, `sqm.middleware.guardrails.maxRows`, `sqm.middleware.guardrails.explainDryRun`
- `sqm.validation.settings.json` / `SQM_VALIDATION_SETTINGS_JSON`
- `sqm.validation.settings.yaml` / `SQM_VALIDATION_SETTINGS_YAML`

Optional rewrite setting keys:

- `sqm.middleware.rewrite.defaultLimitInjectionValue`
- `sqm.middleware.rewrite.maxAllowedLimit`
- `sqm.middleware.rewrite.limitExcessMode`
- `sqm.middleware.rewrite.qualificationDefaultSchema`
- `sqm.middleware.rewrite.qualificationFailureMode`
- `sqm.middleware.rewrite.identifierNormalizationCaseMode`

### 8.4 Validation access policy config examples (JSON/YAML)

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

