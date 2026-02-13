# JSON Serialization

SQM provides Jackson mixins for model serialization/deserialization.

## Example

```java
ObjectMapper mapper = SqmJsonMixins.createPretty();
String json = mapper.writeValueAsString(query);
Query restored = mapper.readValue(json, Query.class);
```

## When to Use

- Persisting query models
- Inter-service exchange
- Snapshot testing query structures

## Notes

- Prefer stable node shapes in tests instead of exact whitespace output comparisons.
- Keep model and mixin versions aligned.

## Next

- [Core Model](Core-Model)
- [Schema Validation](Schema-Validation)

