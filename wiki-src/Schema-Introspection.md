# Schema Introspection

Use `sqm-schema-introspect` to load schema metadata either from JDBC or JSON snapshots.

## JSON Snapshot Provider

```java
var provider = JsonSchemaProvider.of(Path.of("schema.json"));
DbSchema schema = provider.load();
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
DbSchema schema = provider.load();
```

## Recommended Flow

1. Introspect from JDBC in controlled environment.
2. Save snapshot.
3. Use snapshot for default/dev builds.
4. Refresh snapshot periodically.

## Next

- [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation)

