# Integration Testing with Docker

SQM uses Testcontainers-based live database integration tests in `sqm-db-it`.

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
mvn -B -pl sqm-db-it -am verify -Pdocker-it -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
```

Per-dialect commands:

```bash
mvn -B -pl sqm-db-it -am verify -Pdocker-it -Dit.test=PostgresDslExecutionIT,PostgresMiddlewareExecutionIT -Dsurefire.skip=true -Dfailsafe.failIfNoSpecifiedTests=false -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
mvn -B -pl sqm-db-it -am verify -Pdocker-it -Dit.test=MySqlDslExecutionIT -Dsurefire.skip=true -Dfailsafe.failIfNoSpecifiedTests=false -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
mvn -B -pl sqm-db-it -am verify -Pdocker-it -Dit.test=SqlServerDslExecutionIT -Dsurefire.skip=true -Dfailsafe.failIfNoSpecifiedTests=false -Dapi.version=1.44 -Ddocker.host=tcp://localhost:2375
```

## CI Example

```bash
mvn -B -pl sqm-db-it -am verify -Pdocker-it -Dapi.version=1.44 -Ddocker.host=unix:///var/run/docker.sock
```

GitHub Actions runs the live suites in the dedicated workflow [`.github/workflows/live-db-it.yml`](../.github/workflows/live-db-it.yml) with one job per dialect:

- PostgreSQL: `PostgresDslExecutionIT`, `PostgresMiddlewareExecutionIT`
- MySQL: `MySqlDslExecutionIT`
- SQL Server: `SqlServerDslExecutionIT`

## Typical Failures

- `Could not find a valid Docker environment`
- Docker API version mismatch
- Missing Docker daemon permissions

See [Troubleshooting](Troubleshooting).
