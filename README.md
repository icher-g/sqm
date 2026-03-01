# SQM - Structured Query Model for Java

[![Build](https://github.com/icher-g/sqm/actions/workflows/publish-maven.yml/badge.svg?branch=main)](https://github.com/icher-g/sqm/actions/workflows/publish-maven.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Packages](https://img.shields.io/badge/Maven-GitHub%20Packages-blue)](https://github.com/icher-g/sqm/packages)
[![codecov](https://codecov.io/gh/icher-g/sqm/graph/badge.svg)](https://codecov.io/gh/icher-g/sqm)

## Description

**SQM (Structured Query Model)** is a Java framework for representing SQL as a typed immutable model and running end-to-end SQL pipelines.
It supports parse, validate, transform/rewrite, render, serialize, and runtime policy enforcement across multiple dialects and transports.

## Wiki

Project wiki with feature guides and examples: https://github.com/icher-g/sqm/wiki

## Contributor Rules

Repository development rules for contributors and coding agents are defined in `AGENTS.md`.

---

## Features

- **Typed immutable SQL model** - composable AST for queries, expressions, predicates, joins, and dialect-specific nodes.
- **Dialect support** - ANSI + PostgreSQL parser/renderer/spec implementations.
- **Validation framework** - schema-aware query validation with configurable limits and access policies (principal/tenant aware).
- **Rewrite and normalization pipeline** - built-in and custom rewrite rules (limit injection, qualification, canonicalization, tenant predicate, etc.).
- **Middleware decision engine** - analyze/enforce/explain workflow with guardrails, telemetry, auditing, and flow control.
- **Transport hosts** - REST and MCP runtimes for externalized middleware usage.
- **DSL and code generation** - fluent query construction plus SQL-file-to-Java generation.
- **JSON support** - model serialization/deserialization via Jackson mixins.
- **Extensibility** - registries/contracts for dialects, functions, validation rules, and rewrite strategies.

---

## Architecture Overview

Base SQL model flow:

```text
       ┌─────────────┐
       │   SQL Text  │
       └──────┬──────┘
              │ parse
              ▼
       ┌─────────────┐
       │    Model    │ <───> JSON / DSL
       └──────┬──────┘
              │ transform / rewrite / optimize
              ▼
       ┌─────────────┐
       │    Model    │
       └──────┬──────┘
              │ render
              ▼
       ┌─────────────┐
       │   SQL Text  │
       └─────────────┘
```

Middleware decision flow:

```text
       ┌─────────────┐
       │   SQL Text  │
       └──────┬──────┘
              │ parse
              ▼
       ┌─────────────┐
       │    Model    │
       └──────┬──────┘
              │ validate
              ▼
       ┌─────────────┐
       │ Guardrails  │
       │ + Policies  │
       └──────┬──────┘
              │ transform / rewrite / optimize
              ▼
       ┌─────────────┐
       │  Rewritten  │
       │    Model    │
       └──────┬──────┘
              │ render
              ▼
       ┌─────────────┐
       │   SQL Text  │
       │ + Bind Data │
       └──────┬──────┘
              │ decide
              ▼
       ┌─────────────┐
       │  Decision   │
       │ allow/deny/ │
       │   rewrite   │
       └─────────────┘
```

Core components:
- **Model (sqm-core)** - AST nodes, visitor/transformer infrastructure, match API, DSL primitives.
- **Parsers (sqm-parser-*)** - dialect parsing into model nodes.
- **Renderers (sqm-render-*)** - dialect rendering from model nodes.
- **Validation (sqm-validate*)** - schema/function/access-policy semantic checks.
- **Catalog (sqm-catalog*)** - schema sources (JSON/JDBC) and type mapping.
- **Control/Middleware (sqm-control, sqm-middleware-*)** - decision engine, rewrites, runtime hosts, telemetry/audit.
- **JSON (sqm-json)** - serialization mixins.
- **Codegen (sqm-codegen*)** - SQL file code generation and plugin integration.
- **Integration tests (sqm-it)** - cross-module/runtime verification.

---

## Model Hierarchy

SQM defines a rich, type-safe model (AST) to represent SQL queries internally.
This model is shared across DSL, parser, renderer, validator, transform/rewrite, middleware, JSON, and codegen modules.

➡️ [View the full hierarchy in docs/MODEL.md](docs/MODEL.md)

---

## 🚀 Quick Example

### Build a query with the DSL and Render

```java
Query q = select(
        col("u", "user_name"),
        col("o", "status"),
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
    .offset(20)
    .build();

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
ORDER BY cnt
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

### SQL Middleware Framework (`sqm-control`)

SQM includes a framework layer for runtime SQL governance (including AI-generated SQL traffic).

Entry points:

- `analyze(sql, context)`
- `enforce(sql, context)`
- `explainDecision(sql, context)`

Validation-only setup:

```java
var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .buildValidationConfig()
);

var decision = middleware.analyze(sql, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
```

Full flow with rewrite + bind rendering:

`BuiltInRewriteRules` provides built-in rule lists, and `SqlQueryRewriter.builder()` composes
those rules into the effective rewriter pipeline.

```java
var middleware = SqlDecisionService.create(
    SqlDecisionServiceConfig.builder(schema)
        .validationSettings(SchemaValidationSettings.defaults())
        .builtInRewriteSettings(
            BuiltInRewriteSettings.defaults()
                .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
                .tenantTablePolicy("public.orders", TenantRewriteTablePolicy.required("tenant_id"))
        )
        .rewriteRules(
            BuiltInRewriteRule.TENANT_PREDICATE,
            BuiltInRewriteRule.LIMIT_INJECTION,
            BuiltInRewriteRule.CANONICALIZATION
        )
        .buildValidationAndRewriteConfig()
);

var decision = middleware.enforce(
    sql,
    ExecutionContext.of("postgresql", "agent", "tenant-a", ExecutionMode.EXECUTE, ParameterizationMode.BIND)
);
```

See:

- Wiki guide: [wiki-src/SQL-Middleware-Framework.md](wiki-src/SQL-Middleware-Framework.md)
- Example class: [examples/src/main/java/io/sqm/examples/Middleware_EndToEndPolicyFlow.java](examples/src/main/java/io/sqm/examples/Middleware_EndToEndPolicyFlow.java)
- Integration tests: [sqm-it/src/test/java/io/sqm/it/PostgresMiddlewareIntegrationTest.java](sqm-it/src/test/java/io/sqm/it/PostgresMiddlewareIntegrationTest.java)

#### Transport Runtimes (REST + MCP)

REST runtime (Spring Boot application):

```bash
mvn -pl sqm-middleware-rest -am spring-boot:run
```

HTTP endpoints:

- `POST /sqm/middleware/v1/analyze`
- `POST /sqm/middleware/v1/enforce`
- `POST /sqm/middleware/v1/explain`

Versioning:

- REST API is path-versioned.
- Current version: `v1` (no unversioned compatibility routes).

Example request body:

```json
{
    "sql": "select id from users",
    "context": {
        "dialect": "postgresql",
        "principal": "agent",
        "tenant": "tenant-a",
        "mode": "ANALYZE",
        "parameterizationMode": "OFF"
    }
}
```

MCP runtime (long-running stdio JSON-RPC server):

```bash
mvn -pl sqm-middleware-mcp -am exec:java -Dexec.mainClass=io.sqm.middleware.mcp.SqlMiddlewareMcpApplication
```

MCP uses framed messages on stdio (`Content-Length` + JSON-RPC body).

`initialize` request:

```text
Content-Length: 58

{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
```

`tools/list` request:

```text
Content-Length: 58

{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

`tools/call` request (`middleware.analyze`):

```text
Content-Length: 205

{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"middleware.analyze","arguments":{"sql":"select id from users","context":{"dialect":"postgresql","mode":"ANALYZE","parameterizationMode":"OFF"}}}}
```

Tool names exposed by MCP runtime:

- `middleware.analyze`
- `middleware.enforce`
- `middleware.explain`

MCP lifecycle and protocol hardening:

- `tools/list` and `tools/call` require `initialize` first (default behavior)
- `shutdown` marks server as draining; further tool calls are rejected
- `exit` terminates the serving loop
- invalid framing is mapped to deterministic JSON-RPC errors with `error.data.category="INVALID_FRAME"`

Transport error contracts:

- REST error envelope:
    - shape: `{"code":"...","message":"...","path":"..."}`
    - examples:
        - `UNAUTHORIZED` (`401`)
        - `RATE_LIMIT_EXCEEDED` (`429`)
        - `REQUEST_TOO_LARGE` (`413`)
        - `INVALID_REQUEST` (`400`, malformed payload)
- MCP JSON-RPC error mapping:
    - parse error: `-32700`
    - invalid request (protocol shape): `-32600`
    - method not found: `-32601`
    - invalid params/tool arguments: `-32602` with `error.data.category="INVALID_PARAMS"`
    - internal failures: `-32603` with `error.data.category="INTERNAL_ERROR"`

REST correlation id:

- every `/sqm/middleware/v1/**` response includes `X-Correlation-Id`
- if client sends `X-Correlation-Id`, the same value is echoed back
- if missing, runtime generates one per request

Runtime configuration (applies to both REST and MCP hosts):

Complete generated key table (single source of truth):
- [docs/MIDDLEWARE_CONFIG_KEYS.md](docs/MIDDLEWARE_CONFIG_KEYS.md) (generated from `ConfigKeys`; run `scripts/generate-middleware-config-keys-doc.ps1`)

- Schema source:
    - `sqm.middleware.schema.source` (`manual` | `json` | `jdbc`)
    - env: `SQM_MIDDLEWARE_SCHEMA_SOURCE`
    - `sqm.middleware.schema.bootstrap.failFast` / `SQM_MIDDLEWARE_SCHEMA_BOOTSTRAP_FAIL_FAST` (`true` by default)
      - `true`: startup fails when schema bootstrap fails
      - `false`: startup continues in degraded mode (not-ready, requests denied with pipeline error)
    - `manual` loads default schema from JSON file/resource, not from hardcoded Java model
    - optional manual default JSON override: `sqm.middleware.schema.defaultJson.path` / `SQM_MIDDLEWARE_SCHEMA_DEFAULT_JSON_PATH`
- Runtime mode:
    - `sqm.middleware.runtime.mode` / `SQM_MIDDLEWARE_RUNTIME_MODE` (`dev` | `production`)
    - `sqm.middleware.productionMode` / `SQM_MIDDLEWARE_PRODUCTION_MODE` (`true` enables strict production mode)
    - production mode is also activated by Spring profile `prod`/`production` (`spring.profiles.active` / `SPRING_PROFILES_ACTIVE`)
    - in production mode:
        - `sqm.middleware.schema.source` must be explicitly configured
        - `manual` source requires explicit `sqm.middleware.schema.defaultJson.path` (bundled fallback is disallowed)
- JSON schema source:
    - `sqm.middleware.schema.json.path`
    - env: `SQM_MIDDLEWARE_SCHEMA_JSON_PATH`
- JDBC schema source:
    - `sqm.middleware.jdbc.url` / `SQM_MIDDLEWARE_JDBC_URL` (required for `jdbc` source)
    - `sqm.middleware.jdbc.user` / `SQM_MIDDLEWARE_JDBC_USER`
    - `sqm.middleware.jdbc.password` / `SQM_MIDDLEWARE_JDBC_PASSWORD`
    - `sqm.middleware.jdbc.driver` / `SQM_MIDDLEWARE_JDBC_DRIVER` (optional class name)
    - `sqm.middleware.jdbc.schemaPattern` / `SQM_MIDDLEWARE_JDBC_SCHEMA_PATTERN`
- Rewrite and validation:
    - `sqm.validation.settings.json` / `SQM_VALIDATION_SETTINGS_JSON` (inline JSON text for `SchemaValidationSettings`)
    - `sqm.validation.settings.yaml` / `SQM_VALIDATION_SETTINGS_YAML` (inline YAML text for `SchemaValidationSettings`)
    - `sqm.middleware.rewrite.enabled` / `SQM_MIDDLEWARE_REWRITE_ENABLED` (`true` by default)
    - `sqm.middleware.rewrite.rules` / `SQM_MIDDLEWARE_REWRITE_RULES` (comma-separated built-in rule names)
    - `sqm.middleware.rewrite.tenant.tablePolicies` / `SQM_MIDDLEWARE_REWRITE_TENANT_TABLE_POLICIES` (CSV: `schema.table:tenant_column[:REQUIRED|OPTIONAL|SKIP],...`)
    - `sqm.middleware.rewrite.tenant.fallbackMode` / `SQM_MIDDLEWARE_REWRITE_TENANT_FALLBACK_MODE` (`DENY` | `SKIP`)
    - `sqm.middleware.rewrite.tenant.ambiguityMode` / `SQM_MIDDLEWARE_REWRITE_TENANT_AMBIGUITY_MODE` (`DENY` | `SKIP`)
    - `sqm.middleware.validation.maxJoinCount` / `SQM_MIDDLEWARE_VALIDATION_MAX_JOIN_COUNT`
    - `sqm.middleware.validation.maxSelectColumns` / `SQM_MIDDLEWARE_VALIDATION_MAX_SELECT_COLUMNS`
    - `sqm.middleware.validation.tenantRequirementMode` / `SQM_MIDDLEWARE_VALIDATION_TENANT_REQUIREMENT_MODE` (`OPTIONAL` | `REQUIRED`)
- Guardrails:
    - `sqm.middleware.guardrails.maxSqlLength` / `SQM_MIDDLEWARE_GUARDRAILS_MAX_SQL_LENGTH`
    - `sqm.middleware.guardrails.timeoutMillis` / `SQM_MIDDLEWARE_GUARDRAILS_TIMEOUT_MILLIS`
    - `sqm.middleware.guardrails.maxRows` / `SQM_MIDDLEWARE_GUARDRAILS_MAX_ROWS`
    - `sqm.middleware.guardrails.explainDryRun` / `SQM_MIDDLEWARE_GUARDRAILS_EXPLAIN_DRY_RUN`
- Audit:
    - `sqm.middleware.audit.publisher` / `SQM_MIDDLEWARE_AUDIT_PUBLISHER` (`noop` | `logging` | `file`)
    - `sqm.middleware.audit.logger.name` / `SQM_MIDDLEWARE_AUDIT_LOGGER_NAME`
    - `sqm.middleware.audit.logger.level` / `SQM_MIDDLEWARE_AUDIT_LOGGER_LEVEL`
    - `sqm.middleware.audit.file.path` / `SQM_MIDDLEWARE_AUDIT_FILE_PATH` (required for `file` mode)
- Telemetry:
    - `sqm.middleware.metrics.enabled` / `SQM_MIDDLEWARE_METRICS_ENABLED`
    - `sqm.middleware.metrics.logger.name` / `SQM_MIDDLEWARE_METRICS_LOGGER_NAME`
    - `sqm.middleware.metrics.logger.level` / `SQM_MIDDLEWARE_METRICS_LOGGER_LEVEL`
- Host flow control:
    - `sqm.middleware.host.maxInFlight` / `SQM_MIDDLEWARE_HOST_MAX_IN_FLIGHT`
    - `sqm.middleware.host.acquireTimeoutMillis` / `SQM_MIDDLEWARE_HOST_ACQUIRE_TIMEOUT_MILLIS`
    - `sqm.middleware.host.requestTimeoutMillis` / `SQM_MIDDLEWARE_HOST_REQUEST_TIMEOUT_MILLIS`
- MCP protocol hardening:
    - `sqm.middleware.mcp.maxContentLengthBytes` / `SQM_MIDDLEWARE_MCP_MAX_CONTENT_LENGTH_BYTES`
    - `sqm.middleware.mcp.maxHeaderLineLengthBytes` / `SQM_MIDDLEWARE_MCP_MAX_HEADER_LINE_LENGTH_BYTES`
    - `sqm.middleware.mcp.maxHeaderBytes` / `SQM_MIDDLEWARE_MCP_MAX_HEADER_BYTES`
    - `sqm.middleware.mcp.requireInitializeBeforeTools` / `SQM_MIDDLEWARE_MCP_REQUIRE_INITIALIZE_BEFORE_TOOLS`

Example (JSON schema source + rewrite rules):

```bash
mvn -pl sqm-middleware-rest -am spring-boot:run \
    -Dsqm.middleware.schema.source=json \
    -Dsqm.middleware.schema.json.path=./schema.json \
    -Dsqm.middleware.rewrite.rules=TENANT_PREDICATE,LIMIT_INJECTION,CANONICALIZATION \
    -Dsqm.middleware.rewrite.tenant.tablePolicies=public.users:tenant_id:REQUIRED,public.orders:tenant_id:OPTIONAL \
    -Dsqm.middleware.rewrite.tenant.fallbackMode=DENY \
    -Dsqm.middleware.rewrite.tenant.ambiguityMode=DENY
```

Runtime status endpoints (REST host):

- `GET /sqm/middleware/v1/health`
  - liveness-style signal (`status=UP`) + schema bootstrap diagnostics
- `GET /sqm/middleware/v1/readiness`
  - readiness signal:
    - `status=READY` when schema bootstrap is ready
    - `status=NOT_READY` when started in degraded mode
  - includes schema source, state, description, and optional error message

Production operations docs:

- NFR suite: [docs/MIDDLEWARE_NFR.md](docs/MIDDLEWARE_NFR.md)
- Deployment profiles/artifacts: [docs/MIDDLEWARE_DEPLOYMENT_PROFILES.md](docs/MIDDLEWARE_DEPLOYMENT_PROFILES.md)
- SLO/SLI: [docs/MIDDLEWARE_SLO_SLI.md](docs/MIDDLEWARE_SLO_SLI.md)
- Runbook: [docs/MIDDLEWARE_RUNBOOK.md](docs/MIDDLEWARE_RUNBOOK.md)
- Release checklist: [docs/MIDDLEWARE_RELEASE_CHECKLIST.md](docs/MIDDLEWARE_RELEASE_CHECKLIST.md)

Validation settings example with tenant access policies (JSON):

```json
{
  "tenantRequirementMode": "REQUIRED",
  "accessPolicy": {
    "deniedTables": ["users"],
    "principals": [
      {
        "name": "analyst",
        "deniedColumns": ["users.email"]
      }
    ],
    "tenants": [
      {
        "name": "tenant-a",
        "deniedTables": ["payments"],
        "deniedColumns": ["users.ssn"],
        "allowedFunctions": ["count", "lower"]
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
  deniedTables:
    - users
  principals:
    - name: analyst
      deniedColumns:
        - users.email
  tenants:
    - name: tenant-a
      deniedTables:
        - payments
      deniedColumns:
        - users.ssn
      allowedFunctions:
        - count
        - lower
    - name: tenant-b
      deniedTables:
        - audit_logs
```

Policy evaluation precedence for a request:
- `global rules` + `principal rules` + `tenant rules` + `tenant+principal rules`
- deny rules are additive across scopes
- function allowlists are additive across scopes

Config templates included in the repo:

- [sqm-middleware-rest/src/main/resources/application.properties](sqm-middleware-rest/src/main/resources/application.properties)
- [sqm-middleware-mcp/.env.example](sqm-middleware-mcp/.env.example)

Windows named-pipe bridge helpers:

```powershell
# terminal 1: start bridge (stdio MCP <-> named pipes)
powershell -ExecutionPolicy Bypass -File .\scripts\start-mcp-named-pipe-bridge.ps1

# terminal 2: run sample requests through the named pipes
powershell -ExecutionPolicy Bypass -File .\scripts\test-mcp-named-pipe.ps1
```

---

### SQL File Codegen (Maven)

Generate Java query classes from `src/main/sql/**/*.sql` at build time.

```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${project.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <dialect>postgresql</dialect>
    <basePackage>com.acme.generated</basePackage>
    <sqlDirectory>${project.basedir}/src/main/sql</sqlDirectory>
    <generatedSourcesDirectory>${project.build.directory}/generated-sources/sqm-codegen</generatedSourcesDirectory>
  </configuration>
</plugin>
```

Example SQL file:

```sql
-- src/main/sql/user/find_by_id.sql
select u.id, u.user_name
from users u
where u.id = :id
```

Generated usage:

```java
import com.acme.generated.UserQueries;

var query = UserQueries.findById();
var params = UserQueries.findByIdParams(); // ["id"]
```

The plugin runs in `generate-sources`, writes generated Java files under `target/generated-sources/sqm-codegen`, and Maven compiles them together with your source code.

### Schema-Aware Codegen Validation

`sqm-codegen-maven-plugin` can validate parsed SQL files against a schema before generating Java classes.
The schema can be loaded from:
- JSON snapshot (`JsonSchemaProvider`)
- JDBC introspection (`JdbcSchemaProvider`) with optional local cache reuse

See full setup and all configuration options in:
- [docs/SQL_FILE_CODEGEN_SCHEMA_VALIDATION.md](docs/SQL_FILE_CODEGEN_SCHEMA_VALIDATION.md)

Quick-start (JSON snapshot):

```xml
<configuration>
  <dialect>postgresql</dialect>
  <schemaProvider>json</schemaProvider>
  <schemaSnapshotPath>${project.basedir}/src/main/sqm/schema.json</schemaSnapshotPath>
</configuration>
```

Examples module also provides JDBC mode via profile:

```bash
mvn -pl examples -am generate-sources -Pjdbc-schema-validate
```
---

### PostgreSQL Dialect Support

SQM includes PostgreSQL parsing and rendering with dialect-specific capabilities.

```java
var sql = """
    SELECT a OPERATOR(pg_catalog.##) b
    FROM t
    WHERE ts AT TIME ZONE 'UTC' > now() - interval '1 day'
""";

var parseCtx = ParseContext.of(new PostgresSpecs());
var query = parseCtx.parse(Query.class, sql).value();

var renderCtx = RenderContext.of(new PostgresDialect());
var rendered = renderCtx.render(query).sql();
```

Highlights:
- Custom operators (including OPERATOR(...)) with PostgreSQL-style precedence tiers
- PostgreSQL `::` type casts
- Expression `COLLATE`
- `AT TIME ZONE` expressions
- Exponentiation operator (`^`)
- DISTINCT ON
- ILIKE / SIMILAR TO
- IS DISTINCT FROM / IS NOT DISTINCT FROM
- Regex predicates (~, ~*, !~, !~*)
- Arrays (ARRAY literals, subscripts, slices)
- LATERAL, function tables, WITH ORDINALITY
- GROUPING SETS / ROLLUP / CUBE
- ORDER BY ... USING
- SELECT locking clauses (FOR UPDATE/SHARE, NOWAIT, SKIP LOCKED)

### Schema Validation (sqm-validate)

SQM includes semantic query validation against a provided schema model.

Add dependency:

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-validate</artifactId>
  <version>0.3.1</version>
</dependency>
```

Basic usage:

```java
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaQueryValidator;

CatalogSchema schema = CatalogSchema.of(
    CatalogTable.of("public", "users",
        CatalogColumn.of("id", CatalogType.LONG),
        CatalogColumn.of("name", CatalogType.STRING)
    )
);

var validator = SchemaQueryValidator.of(schema);
var result = validator.validate(query);

if (!result.ok()) {
    result.problems().forEach(System.out::println);
}
```

Custom settings and dialect extension:

```java
import io.sqm.validate.schema.SchemaValidationSettings;

var settings = SchemaValidationSettings.builder()
    .functionCatalog(customCatalog)
    .addRule(customRule)
    .build();

var validator1 = SchemaQueryValidator.of(schema, settings);
var validator2 = SchemaQueryValidator.of(schema, myDialect); // SchemaValidationDialect
```

See full validation features and planned improvements in [docs/VALIDATION_FEATURES.md](docs/VALIDATION_FEATURES.md).

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
    "name": { "value": "users", "quoteStyle": "NONE" },
    "alias": { "value": "u", "quoteStyle": "NONE" }
  },
  "joinKind" : "INNER",
  "on" : {
    "kind": "comparison",
    "lhs":  {
      "kind" : "column",
      "tableAlias" : { "value": "u", "quoteStyle": "NONE" },
      "name" : { "value": "id", "quoteStyle": "NONE" }
    },
    "operator": "EQ",
    "rhs": {
      "kind" : "column",
      "tableAlias" : { "value": "o", "quoteStyle": "NONE" },
      "name" : { "value": "user_id", "quoteStyle": "NONE" }
    }
  }
}
```

---

### Collectors & Transformers

The SQM model provides powerful traversal and transformation mechanisms built on top of the **Visitor pattern**.  
Two common examples are *collectors* (for extracting information from a query tree) and *transformers* (for producing modified copies).

---

#### Column Collector Example

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
        columns.add(c.tableAlias() == null
            ? c.name().value()
            : c.tableAlias().value() + "." + c.name().value());
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

#### Column Transformer Example

A **transformer** produces a modified copy of the query tree.  
Transformers extend `RecursiveNodeTransformer` (a subclass of `NodeTransformer<Node>`) and return new node instances where changes are required.

```java
public static class RenameColumnTransformer extends RecursiveNodeTransformer {
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

##### Usage

```java
var transformer = new RenameColumnTransformer();
Query transformed = transformer.transform(originalQuery);
```

The `RecursiveNodeTransformer` automatically handles traversal and reconstruction of immutable nodes.  
You only override methods for nodes you wish to modify - all others are traversed and returned unchanged.

---

#### Summary

| Concept                             | Description                                                  |
|-------------------------------------|--------------------------------------------------------------|
| **Visitor**                         | Walks the query tree and can perform analysis or collection. |
| **Transformer**                     | Walks the query tree and produces a modified copy.           |
| **Recursive Visitors/Transformers** | Provide automatic traversal of all subnodes.                 |
| **Collectors**                      | Typically accumulate results in a `Set`, `List`, or `Map`.   |
| **Transformers**                    | Typically override a few methods to replace or rename nodes. |

---

#### Typical Use Cases

- Collect all referenced tables or columns in a query.  
- Rewrite column names, table aliases, or function calls.  
- Apply dialect-specific rewrites or optimizations.  
- Extract metadata for validation or analysis.  
- Implement automatic query normalization or anonymization.

---

### Match API

The `Match` API provides a fluent, pattern-style mechanism to perform type-safe dispatching across SQM model node hierarchies. It replaces complex `instanceof` chains with a clear, functional interface for handling each node subtype.

#### Overview

A `Match<R>` represents a lazy evaluation of matching arms (handlers) against a specific model node. The result type `R` defines what each handler returns.

The core idea:

```java
var result = Match
    .<String>query(query)
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

* **`QueryMatch<R>`** - matches query subtypes (`SelectQuery`, `WithQuery`, `CompositeQuery`).
* **`JoinMatch<R>`** - matches join types (`OnJoin`, `UsingJoin`, `NaturalJoin`, `CrossJoin`).
* **`ExpressionMatch<R>`** - matches expression types (`CaseExpr`, `ColumnExpr`, `FunctionExpr`, etc.).
* **`PredicateMatch<R>`** - matches predicate kinds (`BetweenPredicate`, `InPredicate`, `LikePredicate`, etc.).
* **`SelectItemMatch<R>`** - matches select item types (`ExprSelectItem`, `StarSelectItem`, `QualifiedStarSelectItem`).
* **`TableMatch<R>`** - matches table reference types (`Table`, `QueryTable`, `ValuesTable`).
* etc.

Each provides fluent methods corresponding to their subtype structure. For example:

```java
String result = Match
    .<String>predicate(predicate)
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

#### Expression Match Example

```java
String sql = Match
    .<String>expression(expr)
    .column(c -> c.tableAlias() == null
        ? c.name().value()
        : c.tableAlias().value() + "." + c.name().value())
    .literal(l -> String.valueOf(l.value()))
    .func(f -> String.join(".", f.name().values()) + "(...)" )
    .orElse("<unknown expression>");
```

This example demonstrates how the `Match` API cleanly separates handling logic per subtype without requiring explicit type checks.

#### Accessing Specific Field Example

```text
SELECT u.user_name, o.status, count(*) AS cnt
FROM orders AS o
INNER JOIN users AS u ON u.id = o.user_id
```

```java
// get the name of the column in a join.
// here the Match class is called internally on each sub type.
var columnName = q.matchQuery()
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

---

### Parameters & Binding

SQM provides a unified model for representing parameters inside SQL queries and a flexible rendering pipeline that determines how these parameters appear in the final SQL string.  
The core idea is that parameterization is **dialect-independent** at the model level and **dialect-dependent** at render time.

The parameterization workflow consists of three parts:

1. **Parameter Expressions in the AST** (`OrdinalParamExpr`, `NamedParamExpr`, `AnonymousParamExpr`)
2. **Parameterization Mode** (`ParameterizationMode`)

---

#### Parameter Types in the Model

##### **1. Literals**
Any value embedded directly in the AST:

```java
col("age").gt(lit(21))
```

Depending on the parameterization mode, literals may be:
- kept inline (default rendering), or
- converted into bind parameters (Bind mode)

---

##### **2. Named Parameters**
Explicit parameters such as:

```sql
WHERE a = :name
```

Represented by:

```java
NamedParamExpr.of("name")
```

---

##### **3. Anonymous Parameters**
Parameters without names - the typical `?` placeholder:

```sql
WHERE a = ?
```

Represented internally by:

```java
AnonymousParamExpr.of()
```

These are typically introduced automatically by the renderer or during parameterization.

---

#### Parameterization Modes

Controlled by:

```java
RenderOptions(parameterizationMode)
```

SQM supports two main strategies:

##### **1. `Inline`**
(Default)  
Literals stay inline; named parameters are kept as `:name`.

```sql
WHERE age > 21
WHERE a = :x AND b = :y
```

##### **2. `Bind`**
All literals are converted to `?` placeholders, and a `SqlText` is produced with a fully ordered parameter list.

This mode is used for JDBC-style parameter binding and safety.

---

#### Rendering Example

##### Code

```java
var q = select(col("a"), col("b"))
    .from(tbl("t"))
    .where(
        col("a").eq(10).and(col("b").eq("x"))
    )
    .build();

var opts = RenderOptions.of(ParameterizationMode.Bind);

SqlText t = ctx.render(q, opts);
```

##### Output

**SQL**

```sql
SELECT a, b
FROM t
WHERE a = ? AND b = ?
```

**Parameters:**

```
[10, "x"]
```

---

### Arithmetic Expressions

SQM supports arithmetic expressions as part of the SQL model.  
Arithmetic nodes allow SQL engines and transformations to handle numeric operations in a fully structured and type-safe way.

#### Supported operations

**Unary**

- `-a` -> `NegativeArithmeticExpr`

**Additive**

- `a + b` -> `AddArithmeticExpr`
- `a - b` -> `SubArithmeticExpr`

**Multiplicative**

- `a * b` -> `MulArithmeticExpr`
- `a / b` -> `DivArithmeticExpr`
- `a % b` -> `ModArithmeticExpr`

#### Example

```java
var expr = col("price").add(col("quantity").mul(lit(2)));
```

This produces a model equivalent to:

```sql
price + quantity * 2
```

#### Notes

- Operator precedence is preserved by the parser (`* / %` before `+ -`).
- Arithmetic expressions can be nested and combined with predicates, functions, CASE statements, and more.
- All arithmetic nodes extend `ArithmeticExpr` and integrate seamlessly with visitors and transformations.

---

## Core Modules

| Module                     | Description                                                                 |
|----------------------------|-----------------------------------------------------------------------------|
| `sqm-core`                 | Core SQL model (AST), visitors/transformers, match API, base DSL primitives |
| `sqm-core-postgresql`      | PostgreSQL-specific model capabilities                                      |
| `sqm-parser`               | Parser SPI and shared parser contracts                                      |
| `sqm-parser-ansi`          | ANSI parser implementation                                                  |
| `sqm-parser-postgresql`    | PostgreSQL parser implementation                                            |
| `sqm-render`               | Renderer SPI and shared rendering contracts                                 |
| `sqm-render-ansi`          | ANSI renderer implementation                                                |
| `sqm-render-postgresql`    | PostgreSQL renderer implementation                                          |
| `sqm-json`                 | Jackson mixins and JSON serialization support                               |
| `sqm-catalog`              | Schema/catalog model and providers (JSON/JDBC)                              |
| `sqm-catalog-postgresql`   | PostgreSQL catalog-specific implementations                                 |
| `sqm-validate`             | Schema-aware semantic validator                                             |
| `sqm-validate-postgresql`  | PostgreSQL validation extensions                                            |
| `sqm-control`              | Decision engine, rewrites, guardrails, audit abstractions                   |
| `sqm-middleware-api`       | Transport-neutral middleware request/response contracts                     |
| `sqm-middleware-core`      | Runtime factory/services (flow control, telemetry, bootstrap)               |
| `sqm-middleware-rest`      | Spring Boot REST host adapter                                               |
| `sqm-middleware-mcp`       | MCP stdio host adapter                                                      |
| `sqm-codegen`              | SQL-to-Java query model/code generation                                     |
| `sqm-codegen-maven-plugin` | Maven plugin for SQL file code generation                                   |
| `sqm-it`                   | Cross-module SQL integration tests                                          |
| `sqm-middleware-it`        | Middleware end-to-end integration/NFR tests                                 |
| `examples`                 | Usage examples and reference flows                                          |

---

## Example Use Cases

- Building complex SQL dynamically in backend applications
- Converting SQL text into structured form for static analysis or auditing
- Generating dialect-specific SQL (PostgreSQL, SQL Server, etc.)
- Visual query builders or query explorers
- Integrating with DSL or JSON-based query definitions

---

## Testing & Validation

SQM includes:
- Round-trip tests: SQL -> Model -> SQL (golden files)
- Fuzz & property tests: verify idempotency and equivalence
- Renderer compatibility checks per dialect
- JSON serialization consistency tests
- Middleware runtime integration and NFR suites

---

## Development Setup

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

## Maven Coordinates

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-core</artifactId>
  <version>${project.version}</version>
</dependency>
```

Current version in this repository: `0.3.1-SNAPSHOT`.

---

## Roadmap

Roadmap is tracked in project docs and GitHub issues:

- [docs/ROADMAP.md](docs/ROADMAP.md)
- https://github.com/icher-g/sqm/issues

---

## License

Licensed under the **MIT License**.  
See [LICENSE](LICENSE) for details.

---

## Learn More

- [Project wiki](https://github.com/icher-g/sqm/wiki)
- [Project docs folder](docs)
- [Project examples](examples/src/main/java/io/sqm/examples)
- [GitHub Issues](https://github.com/icher-g/sqm/issues)

---

### About

**SQM (Structured Query Model)** is developed and maintained by [icher-g](https://github.com/icher-g).
