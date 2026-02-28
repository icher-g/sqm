# Middleware NFR Suite

This document defines the non-functional validation suite for middleware hosts (PR1.8).

## Scope

- Load + concurrency benchmark coverage for REST and MCP adapters.
- Soak coverage for long-running MCP stdio loop stability.
- Security/malformed payload checks are covered by existing transport tests:
  - REST: request-size, rate-limit, unauthorized, malformed payload
  - MCP: malformed framing/protocol shape/error mapping

## Test Entry Points

- `io.sqm.middleware.it.NfrMiddlewareLoadTest`
  - Concurrent load against REST and MCP adapters.
  - Enforces p95 latency and minimum throughput thresholds.
- `io.sqm.middleware.it.NfrMcpSoakTest`
  - Repeated MCP stdio tool-call loop.
  - Enforces max total duration threshold.

## Threshold Configuration

Thresholds are JVM system properties so CI/nightly can tighten without code changes.

- `sqm.nfr.concurrent.requests` (default `300`)
- `sqm.nfr.concurrent.threads` (default `12`)
- `sqm.nfr.max.p95.ms` (default `250`)
- `sqm.nfr.min.throughput.rps` (default `200`)
- `sqm.nfr.mcp.soak.iterations` (default `1500`)
- `sqm.nfr.mcp.soak.max.duration.ms` (default `15000`)

## Local Run

```bash
mvn -pl sqm-middleware-it -am test -Dtest=NfrMiddlewareLoadTest,NfrMcpSoakTest -Dsurefire.failIfNoSpecifiedTests=false
```

## Nightly CI Run

Nightly workflow: `.github/workflows/middleware-nfr-nightly.yml`

It runs the same test classes with explicit thresholds and fails the run when limits are exceeded.
