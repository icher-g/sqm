# PostgreSQL Validation

`sqm-validate-postgresql` adds PG-specific semantic rules on top of base schema validation.

## Setup

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-validate-postgresql</artifactId>
  <version>${sqm.version}</version>
</dependency>
```

## Usage

```java
var validator = SchemaQueryValidator.of(schema, PostgresValidationDialect.of());
var result = validator.validate(query);
```

## Typical Checks

- `DISTINCT ON` consistency with leftmost `ORDER BY`
- Window frame validity constraints
- PostgreSQL clause consistency rules
- Feature availability by PG version/dialect capabilities

## See Also

- `docs/VALIDATION_FEATURES.md`
- [Schema Validation](Schema-Validation)

