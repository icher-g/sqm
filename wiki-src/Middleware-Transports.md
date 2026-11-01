# Middleware Transports

REST and MCP host the same transport-neutral SQL decision service.

## REST host

Spring Boot HTTP application:

```bash
mvn -B -pl sqm-middleware-rest -am install "-Dsurefire.skip=true"
mvn -f sqm-middleware-rest/pom.xml exec:java "-Dexec.mainClass=io.sqm.middleware.rest.SqlMiddlewareRestApplication"
```

HTTP endpoints:

- `POST /sqm/middleware/v1/analyze`
- `POST /sqm/middleware/v1/enforce`
- `POST /sqm/middleware/v1/explain`

The request contract remains a single `sql` text field. That field may contain
a single statement or a semicolon-separated script; no separate batch DTO is
required. Script decisions are all-or-nothing: any denied statement denies the
request, rewrites are rendered back as one combined SQL script, and DDL remains
out of scope.

Versioning:

- REST API is path-versioned.
- Current version: `v1` (no unversioned compatibility routes).

Example request body:

```json
{
    "sql": "insert ignore into users (id, name) values (1, 'alice')",
    "context": {
        "dialect": "mysql",
        "principal": "agent-app",
        "tenant": "tenant-a",
        "mode": "EXECUTE",
        "parameterizationMode": "OFF"
    }
}
```

## MCP host

Long-running stdio JSON-RPC server:

```bash
mvn -B -pl sqm-middleware-mcp -am install "-Dsurefire.skip=true"
mvn -f sqm-middleware-mcp/pom.xml exec:java "-Dexec.mainClass=io.sqm.middleware.mcp.SqlMiddlewareMcpApplication"
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
Content-Length: 214

{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"middleware.analyze","arguments":{"sql":"delete from users where id = 1","context":{"dialect":"mysql","mode":"ANALYZE","parameterizationMode":"OFF"}}}}
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

## Host configuration

Configuration applies to both hosts.

Complete generated key table (single source of truth):
- [docs/middleware/MIDDLEWARE_CONFIG_KEYS.md](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_CONFIG_KEYS.md) (generated from `ConfigKeys`; run `scripts/generate-middleware-config-keys-doc.ps1`)

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
mvn -B -pl sqm-middleware-rest -am install "-Dsurefire.skip=true"
mvn -f sqm-middleware-rest/pom.xml exec:java "-Dexec.mainClass=io.sqm.middleware.rest.SqlMiddlewareRestApplication" \
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

- NFR suite: [docs/middleware/MIDDLEWARE_NFR.md](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_NFR.md)
- Deployment profiles/artifacts: [docs/middleware/MIDDLEWARE_DEPLOYMENT_PROFILES.md](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_DEPLOYMENT_PROFILES.md)
- SLO/SLI: [docs/middleware/MIDDLEWARE_SLO_SLI.md](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_SLO_SLI.md)
- Runbook: [docs/middleware/MIDDLEWARE_RUNBOOK.md](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_RUNBOOK.md)
- Release checklist: [docs/middleware/MIDDLEWARE_RELEASE_CHECKLIST.md](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_RELEASE_CHECKLIST.md)

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

- [sqm-middleware-rest/src/main/resources/application.properties](https://github.com/icher-g/sqm/blob/main/sqm-middleware-rest/src/main/resources/application.properties)
- [sqm-middleware-mcp/.env.example](https://github.com/icher-g/sqm/blob/main/sqm-middleware-mcp/.env.example)

## Windows named-pipe bridge

```powershell
# terminal 1: start bridge (stdio MCP <-> named pipes)
powershell -ExecutionPolicy Bypass -File .\scripts\start-mcp-named-pipe-bridge.ps1

# terminal 2: run sample requests through the named pipes
powershell -ExecutionPolicy Bypass -File .\scripts\test-mcp-named-pipe.ps1
```

## Related guides

- [SQL Middleware Framework](SQL-Middleware-Framework)
- [Policy Templates](SQL-Middleware-Policy-Templates)
- [Deployment profiles](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_DEPLOYMENT_PROFILES.md)
- [Operations runbook](https://github.com/icher-g/sqm/blob/main/docs/middleware/MIDDLEWARE_RUNBOOK.md)
