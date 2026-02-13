# SQL File Codegen Schema Validation

Codegen can validate parsed SQL against schema metadata before emitting Java.

Detailed reference: `docs/SQL_FILE_CODEGEN_SCHEMA_VALIDATION.md`.

## Providers

- `none` (default)
- `json` via `JsonSchemaProvider`
- `jdbc` via `JdbcSchemaProvider` with optional local cache

## Key Options

- `sqm.codegen.schemaProvider`
- `sqm.codegen.schemaSnapshotPath`
- `sqm.codegen.schemaJdbcUrl`
- `sqm.codegen.schemaJdbcUsername` / `sqm.codegen.schemaJdbcPassword`
- `sqm.codegen.schemaJdbcServerId`
- `sqm.codegen.schemaCachePath`
- `sqm.codegen.schemaCacheRefresh`
- `sqm.codegen.schemaCacheWrite`
- `sqm.codegen.schemaCacheTtlMinutes`
- `sqm.codegen.schemaCacheExpectedDatabaseProduct`
- `sqm.codegen.schemaCacheExpectedDatabaseMajorVersion`
- `sqm.codegen.failOnValidationError`
- `sqm.codegen.validationReportPath`

## Cache Sidecar

When JDBC cache is written, metadata sidecar is created at:

- `${schemaCachePath}.meta.properties`

It stores dialect/DB metadata and generation timestamp used for cache reuse checks.

## Report Files

- JSON report: `validationReportPath`
- text summary: `validationReportPath + ".txt"`
- JSON contains `formatVersion` (currently `1`)

## Example: Secure JDBC Credentials

Use Maven settings server or environment variables instead of storing creds in POM.

```xml
<schemaJdbcServerId>app-db</schemaJdbcServerId>
<schemaJdbcUsernameEnv>SQM_SCHEMA_JDBC_USERNAME</schemaJdbcUsernameEnv>
<schemaJdbcPasswordEnv>SQM_SCHEMA_JDBC_PASSWORD</schemaJdbcPasswordEnv>
```

## Example: Non-Blocking Validation Report

```xml
<configuration>
  <schemaProvider>json</schemaProvider>
  <schemaSnapshotPath>${project.basedir}/src/main/sqm/schema.json</schemaSnapshotPath>
  <failOnValidationError>false</failOnValidationError>
  <validationReportPath>${project.build.directory}/sqm-codegen/validation-report.json</validationReportPath>
</configuration>
```

Output files:

- `${project.build.directory}/sqm-codegen/validation-report.json`
- `${project.build.directory}/sqm-codegen/validation-report.json.txt`
