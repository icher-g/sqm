# Middleware SLO / SLI

This document defines PR1.10 SLI/SLO baselines for middleware hosts.

## SLI Definitions

- Availability SLI:
  - `% of successful middleware host requests (2xx for REST; successful JSON-RPC result for MCP)`
- Latency SLI:
  - `p95` middleware decision latency
- Error-rate SLI:
  - `% of requests ending in transport/runtime errors`
- Policy-deny ratio SLI:
  - `% of requests denied by policy (`DENY_*`)`
- Rewrite ratio SLI:
  - `% of requests rewritten (`REWRITE_*`)`

## SLO Targets

- Availability: `>= 99.9%` monthly
- Latency: `p95 <= 300ms` for analyze/enforce
- Error rate: `< 0.5%` (excluding policy denials)
- Startup readiness:
  - production: ready within `60s` after process start when schema source is valid
- Recovery:
  - schema/bootstrap misconfiguration diagnosis actionable within `5 minutes` using status endpoints/logs

## Measurement Sources

- Telemetry logs (`sqm.middleware.metrics.*`)
- Audit events (`sqm.middleware.audit.*`)
- REST status endpoints:
  - `/sqm/middleware/v1/health`
  - `/sqm/middleware/v1/readiness`
- MCP startup diagnostics stderr line (schema bootstrap state/source/error)

## Alert Suggestions

- readiness `NOT_READY` longer than 2 minutes
- `DENY_PIPELINE_ERROR` spike above baseline
- sustained p95 above SLO for 5 minutes
- unusual increase in malformed request classes (`INVALID_REQUEST`, MCP `-32600/-32700`)
