# Middleware Configuration Keys

> Auto-generated from sqm-control/src/main/java/io/sqm/control/ConfigKeys.java.
> Do not edit manually; run scripts/generate-middleware-config-keys-doc.ps1.

| Constant                                     | JVM Property                                             | Environment Variable                                        |
|----------------------------------------------|----------------------------------------------------------|-------------------------------------------------------------|
| `VALIDATION_SETTINGS_JSON`                   | `sqm.validation.settings.json`                           | `SQM_VALIDATION_SETTINGS_JSON`                              |
| `VALIDATION_SETTINGS_YAML`                   | `sqm.validation.settings.yaml`                           | `SQM_VALIDATION_SETTINGS_YAML`                              |
| `SCHEMA_SOURCE`                              | `sqm.middleware.schema.source`                           | `SQM_MIDDLEWARE_SCHEMA_SOURCE`                              |
| `RUNTIME_MODE`                               | `sqm.middleware.runtime.mode`                            | `SQM_MIDDLEWARE_RUNTIME_MODE`                               |
| `PRODUCTION_MODE`                            | `sqm.middleware.productionMode`                          | `SQM_MIDDLEWARE_PRODUCTION_MODE`                            |
| `SCHEMA_DEFAULT_JSON_PATH`                   | `sqm.middleware.schema.defaultJson.path`                 | `SQM_MIDDLEWARE_SCHEMA_DEFAULT_JSON_PATH`                   |
| `SCHEMA_JSON_PATH`                           | `sqm.middleware.schema.json.path`                        | `SQM_MIDDLEWARE_SCHEMA_JSON_PATH`                           |
| `SCHEMA_BOOTSTRAP_FAIL_FAST`                 | `sqm.middleware.schema.bootstrap.failFast`               | `SQM_MIDDLEWARE_SCHEMA_BOOTSTRAP_FAIL_FAST`                 |
| `JDBC_URL`                                   | `sqm.middleware.jdbc.url`                                | `SQM_MIDDLEWARE_JDBC_URL`                                   |
| `JDBC_USER`                                  | `sqm.middleware.jdbc.user`                               | `SQM_MIDDLEWARE_JDBC_USER`                                  |
| `JDBC_PASSWORD`                              | `sqm.middleware.jdbc.password`                           | `SQM_MIDDLEWARE_JDBC_PASSWORD`                              |
| `JDBC_SCHEMA_PATTERN`                        | `sqm.middleware.jdbc.schemaPattern`                      | `SQM_MIDDLEWARE_JDBC_SCHEMA_PATTERN`                        |
| `JDBC_DRIVER`                                | `sqm.middleware.jdbc.driver`                             | `SQM_MIDDLEWARE_JDBC_DRIVER`                                |
| `REWRITE_ENABLED`                            | `sqm.middleware.rewrite.enabled`                         | `SQM_MIDDLEWARE_REWRITE_ENABLED`                            |
| `VALIDATION_MAX_JOIN_COUNT`                  | `sqm.middleware.validation.maxJoinCount`                 | `SQM_MIDDLEWARE_VALIDATION_MAX_JOIN_COUNT`                  |
| `VALIDATION_MAX_SELECT_COLUMNS`              | `sqm.middleware.validation.maxSelectColumns`             | `SQM_MIDDLEWARE_VALIDATION_MAX_SELECT_COLUMNS`              |
| `VALIDATION_TENANT_REQUIREMENT_MODE`         | `sqm.middleware.validation.tenantRequirementMode`        | `SQM_MIDDLEWARE_VALIDATION_TENANT_REQUIREMENT_MODE`         |
| `GUARDRAILS_MAX_SQL_LENGTH`                  | `sqm.middleware.guardrails.maxSqlLength`                 | `SQM_MIDDLEWARE_GUARDRAILS_MAX_SQL_LENGTH`                  |
| `GUARDRAILS_TIMEOUT_MILLIS`                  | `sqm.middleware.guardrails.timeoutMillis`                | `SQM_MIDDLEWARE_GUARDRAILS_TIMEOUT_MILLIS`                  |
| `GUARDRAILS_MAX_ROWS`                        | `sqm.middleware.guardrails.maxRows`                      | `SQM_MIDDLEWARE_GUARDRAILS_MAX_ROWS`                        |
| `GUARDRAILS_EXPLAIN_DRY_RUN`                 | `sqm.middleware.guardrails.explainDryRun`                | `SQM_MIDDLEWARE_GUARDRAILS_EXPLAIN_DRY_RUN`                 |
| `REWRITE_RULES`                              | `sqm.middleware.rewrite.rules`                           | `SQM_MIDDLEWARE_REWRITE_RULES`                              |
| `REWRITE_DEFAULT_LIMIT_INJECTION_VALUE`      | `sqm.middleware.rewrite.defaultLimitInjectionValue`      | `SQM_MIDDLEWARE_REWRITE_DEFAULT_LIMIT_INJECTION_VALUE`      |
| `REWRITE_MAX_ALLOWED_LIMIT`                  | `sqm.middleware.rewrite.maxAllowedLimit`                 | `SQM_MIDDLEWARE_REWRITE_MAX_ALLOWED_LIMIT`                  |
| `REWRITE_LIMIT_EXCESS_MODE`                  | `sqm.middleware.rewrite.limitExcessMode`                 | `SQM_MIDDLEWARE_REWRITE_LIMIT_EXCESS_MODE`                  |
| `REWRITE_QUALIFICATION_DEFAULT_SCHEMA`       | `sqm.middleware.rewrite.qualificationDefaultSchema`      | `SQM_MIDDLEWARE_REWRITE_QUALIFICATION_DEFAULT_SCHEMA`       |
| `REWRITE_QUALIFICATION_FAILURE_MODE`         | `sqm.middleware.rewrite.qualificationFailureMode`        | `SQM_MIDDLEWARE_REWRITE_QUALIFICATION_FAILURE_MODE`         |
| `REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE` | `sqm.middleware.rewrite.identifierNormalizationCaseMode` | `SQM_MIDDLEWARE_REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE` |
| `REWRITE_TENANT_TABLE_POLICIES`              | `sqm.middleware.rewrite.tenant.tablePolicies`            | `SQM_MIDDLEWARE_REWRITE_TENANT_TABLE_POLICIES`              |
| `REWRITE_TENANT_FALLBACK_MODE`               | `sqm.middleware.rewrite.tenant.fallbackMode`             | `SQM_MIDDLEWARE_REWRITE_TENANT_FALLBACK_MODE`               |
| `REWRITE_TENANT_AMBIGUITY_MODE`              | `sqm.middleware.rewrite.tenant.ambiguityMode`            | `SQM_MIDDLEWARE_REWRITE_TENANT_AMBIGUITY_MODE`              |
| `AUDIT_PUBLISHER_MODE`                       | `sqm.middleware.audit.publisher`                         | `SQM_MIDDLEWARE_AUDIT_PUBLISHER`                            |
| `AUDIT_LOGGER_NAME`                          | `sqm.middleware.audit.logger.name`                       | `SQM_MIDDLEWARE_AUDIT_LOGGER_NAME`                          |
| `AUDIT_LOGGER_LEVEL`                         | `sqm.middleware.audit.logger.level`                      | `SQM_MIDDLEWARE_AUDIT_LOGGER_LEVEL`                         |
| `AUDIT_FILE_PATH`                            | `sqm.middleware.audit.file.path`                         | `SQM_MIDDLEWARE_AUDIT_FILE_PATH`                            |
| `METRICS_ENABLED`                            | `sqm.middleware.metrics.enabled`                         | `SQM_MIDDLEWARE_METRICS_ENABLED`                            |
| `METRICS_LOGGER_NAME`                        | `sqm.middleware.metrics.logger.name`                     | `SQM_MIDDLEWARE_METRICS_LOGGER_NAME`                        |
| `METRICS_LOGGER_LEVEL`                       | `sqm.middleware.metrics.logger.level`                    | `SQM_MIDDLEWARE_METRICS_LOGGER_LEVEL`                       |
| `MCP_MAX_CONTENT_LENGTH_BYTES`               | `sqm.middleware.mcp.maxContentLengthBytes`               | `SQM_MIDDLEWARE_MCP_MAX_CONTENT_LENGTH_BYTES`               |
| `MCP_MAX_HEADER_LINE_LENGTH_BYTES`           | `sqm.middleware.mcp.maxHeaderLineLengthBytes`            | `SQM_MIDDLEWARE_MCP_MAX_HEADER_LINE_LENGTH_BYTES`           |
| `MCP_MAX_HEADER_BYTES`                       | `sqm.middleware.mcp.maxHeaderBytes`                      | `SQM_MIDDLEWARE_MCP_MAX_HEADER_BYTES`                       |
| `MCP_REQUIRE_INITIALIZE_BEFORE_TOOLS`        | `sqm.middleware.mcp.requireInitializeBeforeTools`        | `SQM_MIDDLEWARE_MCP_REQUIRE_INITIALIZE_BEFORE_TOOLS`        |
| `HOST_MAX_IN_FLIGHT`                         | `sqm.middleware.host.maxInFlight`                        | `SQM_MIDDLEWARE_HOST_MAX_IN_FLIGHT`                         |
| `HOST_ACQUIRE_TIMEOUT_MILLIS`                | `sqm.middleware.host.acquireTimeoutMillis`               | `SQM_MIDDLEWARE_HOST_ACQUIRE_TIMEOUT_MILLIS`                |
| `HOST_REQUEST_TIMEOUT_MILLIS`                | `sqm.middleware.host.requestTimeoutMillis`               | `SQM_MIDDLEWARE_HOST_REQUEST_TIMEOUT_MILLIS`                |

