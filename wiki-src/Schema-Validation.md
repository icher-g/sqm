# Schema Validation

`sqm-validate` performs semantic checks against a provided schema model.

## Basic Usage

```java
DbSchema schema = DbSchema.of(
  DbTable.of("public", "users",
    DbColumn.of("id", DbType.LONG),
    DbColumn.of("user_name", DbType.STRING)
  )
);

var validator = SchemaQueryValidator.of(schema);
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
var validator = SchemaQueryValidator.of(schema, settings);
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

## Next

- [PostgreSQL Validation](PostgreSQL-Validation)
- [Schema Introspection](Schema-Introspection)
