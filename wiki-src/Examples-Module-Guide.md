# Examples Module Guide

The `examples` module demonstrates end-to-end SQM usage and code generation.

## Middleware Example

The examples module includes a middleware-focused walkthrough:

- `examples/src/main/java/io/sqm/examples/Middleware_EndToEndPolicyFlow.java`

It demonstrates:

- validation-only flow (`buildValidationConfig` + `analyze`)
- full flow with rewrites (`buildValidationAndRewriteConfig` + `enforce`)
- bind parameterization output (`ParameterizationMode.BIND`)
- custom extension points (`queryRewriter`, `explainer`, `auditPublisher`)

Run all examples module tests/build:

```bash
mvn -pl examples -am test
```

## Default Mode

- Uses `sqm-codegen-maven-plugin` with `schemaProvider=json`
- Snapshot file: `examples/src/main/sqm/schema.json`

Run:

```bash
mvn -pl examples -am generate-sources
```

## JDBC Validation Mode (Opt-in)

Profile: `jdbc-schema-validate`

Run:

```bash
mvn -pl examples -am generate-sources -Pjdbc-schema-validate
```

Required env vars:

- `SQM_SCHEMA_JDBC_URL`
- `SQM_SCHEMA_JDBC_USERNAME`
- `SQM_SCHEMA_JDBC_PASSWORD`

## Why Two Modes

- JSON mode is deterministic and fast.
- JDBC mode validates against live DB metadata when needed.

