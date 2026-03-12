# Downstream Support Matrix

This matrix tracks downstream module support for the dialects and statement kinds currently shipped by SQM.

Status legend:

- `Yes` = supported in the module's intended public surface
- `No` = not supported

| Module | ANSI Query | ANSI DML | PostgreSQL Query | PostgreSQL DML | MySQL Query | MySQL DML | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `sqm-validate` | Yes | Yes | Yes | Yes | Yes | Yes | Base schema validation plus optional PostgreSQL/MySQL dialect rule packs. |
| `sqm-control` | Yes | Yes | Yes | Yes | Yes | Yes | Statement-aware parse/validate/rewrite/render decision pipeline. |
| `sqm-codegen` | Yes | Yes | Yes | Yes | Yes | Yes | SQL-file parsing and Java emission support top-level statements. |
| `sqm-codegen-maven-plugin` | Yes | Yes | Yes | Yes | Yes | Yes | JSON and JDBC schema providers support ANSI/PostgreSQL/MySQL codegen flows. |
| `sqm-middleware-core` | Yes | Yes | Yes | Yes | Yes | Yes | Transport-neutral middleware service delegates to statement-aware control. |
| `sqm-middleware-rest` | Yes | Yes | Yes | Yes | Yes | Yes | REST analyze/enforce/explain endpoints support MySQL and DML payloads. |
| `sqm-middleware-mcp` | Yes | Yes | Yes | Yes | Yes | Yes | MCP analyze/enforce/explain tools support MySQL and DML payloads. |

Current scope notes:

- DDL is intentionally outside this matrix.
- `sqm-codegen-maven-plugin` JDBC schema loading uses dedicated PostgreSQL and MySQL type mappers where available.
