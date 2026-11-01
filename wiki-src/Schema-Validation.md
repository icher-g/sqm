# Schema Validation

`sqm-validate` performs semantic checks against a provided schema model.

## Basic Usage

```java
CatalogSchema schema = CatalogSchema.of(
  CatalogTable.of("public", "users",
    CatalogColumn.of("id", CatalogType.LONG),
    CatalogColumn.of("user_name", CatalogType.STRING)
  )
);

var validator = SchemaStatementValidator.of(schema);
var result = validator.validate(query);
if (!result.ok()) {
  result.problems().forEach(System.out::println);
}
```

## What It Catches

- Missing tables/columns
- Ambiguous references
- Join/alias issues
- Aggregation and subquery shape issues
- Invalid ordinals in `ORDER BY`/`GROUP BY`

## Customization

```java
var settings = SchemaValidationSettings.builder()
  .functionCatalog(customCatalog)
  .addRule(customRule)
  .build();
var validator = SchemaStatementValidator.of(schema, settings);
```

### Load Settings from JSON/YAML (including tenant policies)

```java
String json = """
{
  "tenantRequirementMode": "REQUIRED",
  "accessPolicy": {
    "tenants": [
      { "name": "tenant-a", "deniedTables": ["payments"] },
      { "name": "tenant-b", "deniedColumns": ["users.ssn"] }
    ]
  }
}
""";

var settings = SchemaValidationSettingsLoader.fromJson(json);
var validator = SchemaStatementValidator.of(schema, settings);
```

## Typical Problem Handling

```java
var result = validator.validate(query);
if (!result.ok()) {
  for (var p : result.problems()) {
    System.err.println(p.code() + ": " + p.message());
    if (p.clausePath() != null) {
      System.err.println("clause=" + p.clausePath());
    }
  }
  throw new IllegalStateException("Query is semantically invalid");
}
```

## Dialect rule packs

Supply the dialect's validation pack for feature/version checks and semantic rules:

```java
var oracleValidator = SchemaStatementValidator.of(schema, OracleValidationDialect.of());
var oracleValidation = oracleValidator.validate(query);
```

The optional packs are `sqm-validate-postgresql`, `sqm-validate-mysql`,
`sqm-validate-sqlserver`, and `sqm-validate-oracle`. Oracle rules include MODEL
scopes, pattern-recognition variables, and query/DML compatibility checks.

Full reference: [Validation Features](https://github.com/icher-g/sqm/blob/main/docs/validation/VALIDATION_FEATURES.md).

## Related guides

- [PostgreSQL Validation](PostgreSQL-Validation)
- [Schema Introspection](Schema-Introspection)
- [Oracle Dialect](Oracle-Dialect)

