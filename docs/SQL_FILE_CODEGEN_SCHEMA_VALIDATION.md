# SQL File Codegen Schema Validation

This document describes schema-backed validation in `sqm-codegen-maven-plugin` and all related configuration options.

## Overview

`sqm-codegen-maven-plugin:generate` can validate parsed SQL query models against a database schema before emitting Java sources.

Schema source options:

- `none` (default): no schema validation.
- `json`: load schema from a JSON snapshot file.
- `jdbc`: introspect schema from a live database via JDBC (with optional local cache).

Validation is executed after parsing and before Java emission.

## Quick Start

### 1) JSON snapshot validation

```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${project.version}</version>
  <configuration>
    <dialect>postgresql</dialect>
    <schemaProvider>json</schemaProvider>
    <schemaSnapshotPath>${project.basedir}/src/main/sqm/schema.json</schemaSnapshotPath>
  </configuration>
</plugin>
```

### 2) JDBC validation with cache

```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${project.version}</version>
  <configuration>
    <dialect>postgresql</dialect>
    <schemaProvider>jdbc</schemaProvider>
    <schemaJdbcUrl>jdbc:postgresql://localhost:5432/app</schemaJdbcUrl>
    <schemaJdbcServerId>app-db</schemaJdbcServerId>
    <schemaJdbcSchemaPattern>public</schemaJdbcSchemaPattern>
    <schemaCachePath>${project.build.directory}/sqm-codegen/schema-cache.json</schemaCachePath>
    <schemaCacheRefresh>false</schemaCacheRefresh>
    <schemaCacheWrite>true</schemaCacheWrite>
    <schemaCacheTtlMinutes>60</schemaCacheTtlMinutes>
    <tableIncludePatterns>users|orders</tableIncludePatterns>
    <tableExcludePatterns>tmp_.*</tableExcludePatterns>
  </configuration>
</plugin>
```

## Full Plugin Configuration

### Base codegen options

- `sqm.codegen.skip` (`false`)
- `sqm.codegen.dialect` (`ansi`)  
  Supported: `ansi`, `postgresql` (`postgres`, `pg` aliases).
- `sqm.codegen.basePackage` (`io.sqm.codegen.generated`)
- `sqm.codegen.sqlDirectory` (`${project.basedir}/src/main/sql`)
- `sqm.codegen.generatedSourcesDirectory` (`${project.build.directory}/generated-sources/sqm-codegen`)
- `sqm.codegen.cleanupStaleFiles` (`true`)
- `sqm.codegen.includeGenerationTimestamp` (`false`)
- `sqm.codegen.failOnValidationError` (`true`)
- `sqm.codegen.validationReportPath` (`${project.build.directory}/sqm-codegen/validation-report.json`)

### Schema validation options

- `sqm.codegen.schemaProvider` (`none`)  
  Supported: `none`, `json`, `jdbc`.

#### JSON provider options

- `sqm.codegen.schemaSnapshotPath` (required when `schemaProvider=json`)

#### JDBC provider options

- `sqm.codegen.schemaJdbcUrl` (required when `schemaProvider=jdbc`)
- `sqm.codegen.schemaJdbcUsername` (optional)
- `sqm.codegen.schemaJdbcPassword` (optional)
- `sqm.codegen.schemaJdbcServerId` (optional, Maven `settings.xml` server id)
- `sqm.codegen.schemaJdbcUsernameEnv` (`SQM_SCHEMA_JDBC_USERNAME`)
- `sqm.codegen.schemaJdbcPasswordEnv` (`SQM_SCHEMA_JDBC_PASSWORD`)
- `sqm.codegen.schemaJdbcCatalog` (optional)
- `sqm.codegen.schemaJdbcSchemaPattern` (optional)
- `sqm.codegen.schemaCachePath` (`${project.build.directory}/sqm-codegen/schema-cache.json`)
- `sqm.codegen.schemaCacheRefresh` (`false`)
- `sqm.codegen.schemaCacheWrite` (`true`)
- `sqm.codegen.schemaCacheTtlMinutes` (`0`, disabled)
- `sqm.codegen.schemaIncludePatterns` (optional comma-separated regex list)
- `sqm.codegen.schemaExcludePatterns` (optional comma-separated regex list)
- `sqm.codegen.tableIncludePatterns` (optional comma-separated regex list)
- `sqm.codegen.tableExcludePatterns` (optional comma-separated regex list)
- `sqm.codegen.schemaCacheExpectedDatabaseProduct` (optional)
- `sqm.codegen.schemaCacheExpectedDatabaseMajorVersion` (optional)

## Credential Resolution (JDBC)

Username/password are resolved in this order:

1. explicit plugin fields: `schemaJdbcUsername` / `schemaJdbcPassword`
2. environment variables: `schemaJdbcUsernameEnv` / `schemaJdbcPasswordEnv`
3. Maven settings server (`schemaJdbcServerId`)

### Maven settings example

`~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>app-db</id>
      <username>db_user</username>
      <password>db_password</password>
    </server>
  </servers>
</settings>
```

### Environment variable example

```powershell
$env:SQM_SCHEMA_JDBC_USERNAME="db_user"
$env:SQM_SCHEMA_JDBC_PASSWORD="db_password"
```

## JDBC Cache Behavior

When `schemaProvider=jdbc`:

- if `schemaCacheRefresh=false` and cache file exists, plugin loads schema from cache and does not call DB.
- otherwise plugin introspects schema from DB.
- if `schemaCacheWrite=true`, introspected schema is written back to cache.
- if `schemaCacheTtlMinutes > 0`, expired cache is ignored and refreshed.
- cache metadata sidecar (`*.meta.properties`) pins:
  - sidecar file path is `${schemaCachePath}.meta.properties`
  - SQL dialect
  - DB product name/version (when available)
  - generated timestamp
- when expected DB product/version is configured, cache is reused only when metadata matches.

## Failure Behavior

Generation fails with `MojoFailureException` when:

- SQL parsing fails.
- schema loading fails (`json` read error / JDBC connectivity or metadata error).
- semantic schema validation finds problems (for example `COLUMN_NOT_FOUND`) and `failOnValidationError=true`.

When `failOnValidationError=false`, generation continues and validation issues are written to:
- JSON report: `validationReportPath`
- text summary: `validationReportPath + ".txt"`

JSON report includes `formatVersion` (currently `1`) for forward-compatible report parsing.

## Example: Secure JDBC Config (No credentials in POM)

```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${project.version}</version>
  <configuration>
    <dialect>postgresql</dialect>
    <schemaProvider>jdbc</schemaProvider>
    <schemaJdbcUrl>jdbc:postgresql://localhost:5432/app</schemaJdbcUrl>
    <schemaJdbcServerId>app-db</schemaJdbcServerId>
    <schemaJdbcSchemaPattern>public</schemaJdbcSchemaPattern>
  </configuration>
</plugin>
```

## Example: Snapshot-Only Flow

Use `json` provider in normal builds:

```xml
<schemaProvider>json</schemaProvider>
<schemaSnapshotPath>${project.basedir}/src/main/sqm/schema.json</schemaSnapshotPath>
```

Refresh snapshot periodically (separate step) using `jdbc` provider and cache write enabled.
