# Middleware Release Checklist

This checklist is the PR1.10 release gate.

## Build + Test

1. `mvn -B verify` passes.
2. Middleware release smoke job passes:
   - packaging (`sqm-middleware-rest`, `sqm-middleware-mcp`)
   - startup smoke tests.
3. Nightly NFR workflow latest run passes.

## Configuration + Security

1. Deployment profile templates reviewed for target environment.
2. Production values for secrets/credentials provided via secure store.
3. REST auth/rate-limit settings validated.
4. MCP protocol limits reviewed and validated.

## Operability

1. Health/readiness endpoints validated in target environment.
2. Telemetry logging enabled and observable.
3. Audit publisher mode validated (`logging` or `file` in non-dev).
4. Runbook links distributed to on-call.

## SLO Readiness

1. SLI collection available from telemetry/audit data.
2. Alerts configured for readiness and latency/error thresholds.
3. Incident response contacts verified.
