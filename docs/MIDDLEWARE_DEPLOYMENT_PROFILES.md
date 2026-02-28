# Middleware Deployment Profiles

This document covers PR1.9 deployment artifacts and release profiles.

## Config Templates

Environment templates are versioned under `deploy/config`:

- REST host:
  - `deploy/config/rest-dev.properties`
  - `deploy/config/rest-stage.properties`
  - `deploy/config/rest-prod.properties`
- MCP host:
  - `deploy/config/mcp-dev.properties`
  - `deploy/config/mcp-stage.properties`
  - `deploy/config/mcp-prod.properties`

These templates are baseline starting points and are expected to be overridden via
environment-specific secret/config management.

## Container Artifacts

Docker build contexts:

- REST host: `deploy/docker/rest/Dockerfile`
- MCP host: `deploy/docker/mcp/Dockerfile`

## CI Release Smoke

CI job `middleware-release-smoke` in `.github/workflows/ci.yml` enforces:

1. packaging for both middleware host modules
2. startup smoke tests for REST and MCP application entry points

This is a pull-request gate for release-candidate quality.

## Local Smoke Validation

- REST host smoke script:
  - `scripts/smoke-middleware-rest.ps1`
- MCP stdio smoke script:
  - `scripts/test-mcp-stdio.ps1`
