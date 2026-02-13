# Troubleshooting

## Testcontainers: Docker Not Found

Symptom:

- `Could not find a valid Docker environment`

Checks:

1. `docker version` works in terminal.
2. Docker daemon socket/host is reachable.
3. JVM has `-Ddocker.host=...` and `-Dapi.version=1.44` when needed.

## Docker API Version Too Old

Symptom:

- `client version 1.32 is too old. Minimum supported API version is 1.44`

Fix:

- Add JVM/Maven option: `-Dapi.version=1.44`

## Codegen Fails on Semantic Validation

Symptom:

- `semantic validation failed: COLUMN_NOT_FOUND`

Fix:

1. Validate schema snapshot freshness.
2. Check table/column aliases in SQL files.
3. For diagnosis, set `failOnValidationError=false` and inspect report files.

## Cache Not Reused

Check:

1. `schemaCacheRefresh` is `false`.
2. Cache file exists.
3. TTL is not expired.
4. Sidecar metadata matches expected product/version constraints.

