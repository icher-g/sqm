# Downstream Support Matrix

This matrix tracks downstream module support for the dialects and statement kinds currently shipped by SQM.

Status legend:

- `Yes` = supported in the module's intended public surface
- `No` = not supported

| Module                     | ANSI Query | ANSI DML | PostgreSQL Query | PostgreSQL DML | MySQL Query | MySQL DML | SQL Server Query | SQL Server DML | Oracle Query | Oracle DML | Notes                                                                                                                              |
|----------------------------|------------|----------|------------------|----------------|-------------|-----------|------------------|----------------|--------------|------------|------------------------------------------------------------------------------------------------------------------------------------|
| `sqm-validate`             | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | Base schema validation plus optional PostgreSQL/MySQL/SQL Server/Oracle dialect rule packs.                                        |
| `sqm-control`              | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | Statement-aware parse/validate/rewrite/render decision pipeline.                                                                   |
| `sqm-codegen`              | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | SQL-file parsing and Java emission support top-level statements.                                                                   |
| `sqm-codegen-maven-plugin` | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | JSON schema providers and JDBC introspection use dedicated PostgreSQL, MySQL, SQL Server, and Oracle type mappers where available. |
| `sqm-middleware-core`      | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | Transport-neutral middleware service delegates to statement-aware control.                                                         |
| `sqm-middleware-rest`      | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | REST analyze/enforce/explain endpoints accept Oracle contexts within the shipped dialect scope.                                    |
| `sqm-middleware-mcp`       | Yes        | Yes      | Yes              | Yes            | Yes         | Yes       | Yes              | Yes            | Yes          | Yes        | MCP analyze/enforce/explain tools accept Oracle contexts within the shipped dialect scope.                                         |

Current scope notes:

- DDL is intentionally outside this matrix.
- `sqm-codegen-maven-plugin` JDBC schema loading uses dedicated PostgreSQL, MySQL, SQL Server, and Oracle type mappers where available.
- SQL Server support in this matrix includes the advanced R5B closure shipped across validation, control, codegen, middleware, and integration layers.
- Oracle support includes its 19c query/DML baseline, `RETURNING ... INTO`, baseline `MERGE`, and Oracle-aware catalog type mapping. R11 additionally ships modeled hierarchical queries (`CONNECT BY`), baseline `PIVOT` / `UNPIVOT` relation transforms, and typed Oracle 12.1+ `MATCH_RECOGNIZE`. Row-pattern recognition is supported across parsing, rendering, validation, control, codegen, JSON, and exact same-dialect transpilation, with deterministic Oracle live-engine coverage; non-Oracle targets reject it explicitly. `MODEL` remains deferred and legacy outer-join `(+)` syntax remains unsupported.
