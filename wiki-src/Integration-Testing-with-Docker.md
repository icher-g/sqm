# Integration Testing with Docker

SQM uses Testcontainers-based integration tests for real PostgreSQL flows.

## Local (Windows + Docker Desktop)

1. Enable Docker daemon TCP exposure in Docker Desktop.
2. Run Maven/IDE tests with:

```text
-Ddocker.host=tcp://localhost:2375
-Dapi.version=1.44
```

PowerShell one-liner:

```powershell
$env:MAVEN_OPTS='-Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375'
```

## Maven Command

```bash
mvn -B verify -Pdocker-it -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
```

## CI Example

```bash
mvn -B verify -Pdocker-it -Dapi.version=1.44 -Ddocker.host=unix:///var/run/docker.sock
```

## Typical Failures

- `Could not find a valid Docker environment`
- Docker API version mismatch
- Missing Docker daemon permissions

See [Troubleshooting](Troubleshooting).
