# Downstream Support Matrix

This matrix tracks downstream module support for the dialects and statement kinds currently shipped by SQM.

Status legend:

- `Yes` = supported in the module's intended public surface
- `No` = not supported

| Module                     | ANSI Query | ANSI DML | PostgreSQL Query | PostgreSQL DML | MySQL Query | MySQL DML | SQL Server Query | SQL Server DML | Notes                                                                                                                    |
|----------------------------|------------|----------|------------------|----------------|-------------|-----------|------------------|----------------|--------------------------------------------------------------------------------------------------------------------------|
| `sqm-validate`             | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Base schema validation plus optional PostgreSQL/MySQL/SQL Server dialect rule packs.                                     |
| `sqm-control`              | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Statement-aware parse/validate/rewrite/render decision pipeline.                                                         |
| `sqm-codegen`              | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | SQL-file parsing and Java emission support top-level statements.                                                         |
| `sqm-codegen-maven-plugin` | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | JSON schema providers and JDBC introspection both support SQL Server flows through the dedicated SQL Server type mapper. |
| `sqm-middleware-core`      | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Transport-neutral middleware service delegates to statement-aware control.                                               |
| `sqm-middleware-rest`      | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | REST analyze/enforce/explain endpoints support SQL Server query and advanced DML payloads within the shipped dialect scope. |
| `sqm-middleware-mcp`       | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | MCP analyze/enforce/explain tools support SQL Server query and advanced DML payloads within the shipped dialect scope.    |

Current scope notes:

- DDL is intentionally outside this matrix.
- `sqm-codegen-maven-plugin` JDBC schema loading uses dedicated PostgreSQL, MySQL, and SQL Server type mappers where available.
- SQL Server support in this matrix includes the advanced R5B closure shipped across validation, control, codegen, middleware, and integration layers.
