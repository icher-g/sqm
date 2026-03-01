# Middleware Runbook

This document provides PR1.10 operational runbooks.

## 1. Startup Failure (General)

Symptoms:
- process exits during startup
- CI startup smoke fails

Actions:
1. Check startup logs for schema bootstrap summary/error.
2. Verify runtime mode/config:
   - `sqm.middleware.runtime.mode`
   - `sqm.middleware.productionMode`
3. Confirm required schema source keys are present for configured source.
4. In production mode, ensure `manual` source has explicit `sqm.middleware.schema.defaultJson.path`.

## 2. Schema Source Failure

Symptoms:
- REST readiness returns `NOT_READY`
- decisions return `DENY_PIPELINE_ERROR` with schema bootstrap message

Actions:
1. Call:
   - `GET /sqm/middleware/v1/readiness`
2. Review:
   - `schemaSource`
   - `schemaState`
   - `schemaDescription`
   - `schemaErrorMessage`
3. Fix source-specific issue:
   - `json`: missing/unreadable/invalid file
   - `jdbc`: URL/credentials/driver/network/metadata permissions
   - `manual`: missing default path in strict production mode
4. Restart host after configuration correction.

## 3. Overload / Backpressure

Symptoms:
- elevated latency
- rate-limit responses (REST `429`)
- request timeout denials

Actions:
1. Verify host controls:
   - `sqm.middleware.host.maxInFlight`
   - `sqm.middleware.host.acquireTimeoutMillis`
   - `sqm.middleware.host.requestTimeoutMillis`
2. Verify REST abuse controls:
   - `sqm.middleware.rest.abuse.rateLimitEnabled`
   - window + quota settings
3. Check telemetry logger for request/decision trends.
4. If needed:
   - temporarily increase host concurrency or reduce upstream rate
   - scale out host replicas

## 4. Security Incident (Malformed/Abusive Inputs)

Actions:
1. Collect request correlation IDs (`X-Correlation-Id`) and timestamps.
2. Verify deterministic error mappings:
   - REST: stable error envelope
   - MCP: `-32700/-32600/-32602/-32603`
3. Tighten limits:
   - REST request size/rate
   - MCP header/body limits
4. Preserve logs/audit files for post-incident review.
