# FAQ

## Is JDBC schema validation enabled by default?

No. Default is `schemaProvider=none` unless configured. In `examples`, default is JSON snapshot.

## Why keep JSON snapshots if JDBC exists?

Snapshots make builds deterministic and avoid requiring a running DB on every build.

## Can validation warnings be non-blocking?

Yes. Use `sqm.codegen.failOnValidationError=false` and inspect generated report files.

## Where are validation reports written?

- JSON: `validationReportPath`
- summary: `validationReportPath + ".txt"`

## Where is JDBC schema cache metadata stored?

At `${schemaCachePath}.meta.properties`.

## How do I publish these wiki pages?

Use `scripts/publish-wiki.ps1` after setting your wiki repo URL.

