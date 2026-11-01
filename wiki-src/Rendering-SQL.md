# Rendering SQL

Render SQM model nodes back to SQL text with dialect-aware rules.

## ANSI Rendering

```java
var renderCtx = RenderContext.of(new AnsiDialect());
var sql = renderCtx.render(query).sql();
```

## PostgreSQL Rendering

```java
var renderCtx = RenderContext.of(new PostgresDialect());
var sql = renderCtx.render(query).sql();
```

## Oracle Rendering

For Oracle:

```java
var oracleSql = RenderContext.of(new OracleDialect()).render(query).sql();
```

See [Oracle Dialect](Oracle-Dialect) for pagination, aliases, MODEL, and pattern
recognition, and [Parameters and Binding](Parameters-and-Binding) for literal
parameterization and bind output.

## Behavior

- Renderers only emit syntax supported by their dialect.
- Unsupported dialect features should be rejected, not silently rewritten.

## Use Cases

- Query previews/debug logging
- SQL normalization
- Dialect migration testing

## Next

- [PostgreSQL Validation](PostgreSQL-Validation)
- [Integration Testing with Docker](Integration-Testing-with-Docker)
- [Parameters and Binding](Parameters-and-Binding)

