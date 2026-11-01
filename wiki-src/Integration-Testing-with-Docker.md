# Integration Testing with Docker

Live database execution uses Testcontainers in `sqm-db-it`. Fast unit and
cross-module integration tests remain part of `mvn -B verify`.

## Docker setup

Start Docker Desktop or your Docker daemon and confirm that `docker version`
reports both a client and a server. Let Testcontainers discover the local engine
unless your environment needs an explicit `docker.host`.

The commands below set `api.version=1.44` for the repository's Testcontainers
version. Quote the complete `-D` arguments in PowerShell.

## Run all live suites

```powershell
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dapi.version=1.44"
```

## Select a dialect

```powershell
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=PostgresDslExecutionIT,PostgresMiddlewareExecutionIT" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=MySqlDslExecutionIT" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=SqlServerDslExecutionIT" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=OracleDslExecutionIT" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
```

`surefire.skip` skips reactor unit tests, while Failsafe runs only the integration
class selected by `it.test`.

## Oracle MODEL only

```powershell
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=OracleDslExecutionIT#executes_oracle_model_cases_against_live_db" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
```

This selects the 41 MODEL cases in the existing Oracle class. The full Oracle
suite runs 70 cases, sharing one container across baseline, pattern recognition,
MODEL, and flashback-regression coverage. Startup output streams through the
shared container-log support.

See [Oracle Dialect](Oracle-Dialect) and the
[MODEL execution matrix](https://github.com/icher-g/sqm/blob/main/docs/testing/ORACLE_MODEL_LIVE_EXECUTION.md).

## CI

The [live DB workflow](https://github.com/icher-g/sqm/blob/main/.github/workflows/live-db-it.yml)
runs one job per dialect:

- PostgreSQL: `PostgresDslExecutionIT` and `PostgresMiddlewareExecutionIT`
- MySQL: `MySqlDslExecutionIT`
- SQL Server: `SqlServerDslExecutionIT`
- Oracle: `OracleDslExecutionIT`, including MODEL

Linux runners explicitly set `"-Ddocker.host=unix:///var/run/docker.sock"`.
Use an endpoint appropriate to your environment when automatic discovery is not
available; the local commands do not require enabling Docker TCP exposure.

## Typical failures

- Docker is not running or the engine cannot be discovered.
- Client/server Docker API versions are incompatible.
- The process lacks access to the engine socket or named pipe.

See [Troubleshooting](Troubleshooting) and [Development and Testing](Development-and-Testing).
