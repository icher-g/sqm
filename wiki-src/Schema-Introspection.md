# Schema Introspection

Use `sqm-catalog` providers to load schema metadata either from JDBC or JSON snapshots.

## JSON Snapshot Provider

```java
var provider = JsonSchemaProvider.of(Path.of("schema.json"));
CatalogSchema schema = provider.load();
provider.save(schema);
```

## JDBC Provider

```java
DataSource ds = ...;
var provider = JdbcSchemaProvider.of(
  ds,
  null,                 // catalog
  "public",             // schema pattern
  List.of("TABLE", "VIEW"),
  PostgresSqlTypeMapper.standard()
);
CatalogSchema schema = provider.load();
```

## Recommended Flow

1. Introspect from JDBC in controlled environment.
2. Save snapshot.
3. Use snapshot for default/dev builds.
4. Refresh snapshot periodically.

## Oracle type mapping

Use `OracleSqlTypeMapper.standard()` from `sqm-catalog-oracle` when introspecting
Oracle schemas, in place of the PostgreSQL mapper in the example above.

```java
var oracleSchema = JdbcSchemaProvider.of(
    ds, null, "SQM", List.of("TABLE", "VIEW"), OracleSqlTypeMapper.standard()
).load();
```

See [Oracle Dialect](Oracle-Dialect) and [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation).

## Related guides

- [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation)

