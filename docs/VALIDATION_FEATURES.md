# Validation Features (sqm-validate)

This document describes the current capabilities of `sqm-validate`, how to use it, and what can be improved next.

## Scope

`sqm-validate` performs semantic validation of SQM query models against a provided catalog schema (`CatalogSchema`).

It is currently dialect-agnostic by default (core SQL semantics), with extension hooks for dialect-specific rules.

## Public API

- Main validator:
  - `io.sqm.validate.schema.SchemaQueryValidator`
- Schema model:
  - `io.sqm.catalog.model.CatalogSchema`
  - `io.sqm.catalog.model.CatalogTable`
  - `io.sqm.catalog.model.CatalogColumn`
  - `io.sqm.catalog.model.CatalogType`
- Extension points:
  - `io.sqm.validate.schema.SchemaValidationSettings`
  - `io.sqm.validate.schema.dialect.SchemaValidationDialect`
  - `io.sqm.validate.schema.rule.SchemaValidationRule`

## Supported Validations

### 1. Table and Column Resolution

- Table existence (`TABLE_NOT_FOUND`)
- Ambiguous unqualified table names (`TABLE_AMBIGUOUS`)
- Unknown table aliases (`UNKNOWN_TABLE_ALIAS`)
- Column existence (`COLUMN_NOT_FOUND`)
- Ambiguous unqualified columns (`COLUMN_AMBIGUOUS`)
- Duplicate table aliases in the same `SELECT` scope (`DUPLICATE_TABLE_ALIAS`)

### 2. Join Validation

- `USING` column existence and compatibility (`JOIN_USING_INVALID_COLUMN`, `TYPE_MISMATCH`)
- `ON` missing predicate detection (`JOIN_ON_MISSING_PREDICATE`)
- `ON` alias visibility validation for join order (`JOIN_ON_INVALID_REFERENCE`)
- `ON` boolean expression validation (`JOIN_ON_INVALID_BOOLEAN_EXPRESSION`)

### 3. Type Compatibility

- Comparison predicates (`=`, `<`, etc.) (`TYPE_MISMATCH`)
- `BETWEEN` bounds and probe compatibility (`TYPE_MISMATCH`)
- `LIKE` / pattern compatibility (`TYPE_MISMATCH`)
- `IN` compatibility (`TYPE_MISMATCH`, `IN_ROW_SHAPE_MISMATCH`)
- `ANY`/`ALL` compatibility (`TYPE_MISMATCH`, `SUBQUERY_SHAPE_MISMATCH`)
- `IS DISTINCT FROM` compatibility (`TYPE_MISMATCH`)
- Unary predicate boolean checks (`TYPE_MISMATCH`)

### 4. Subquery Shape Validation

- Scalar contexts require one projected column (`SUBQUERY_SHAPE_MISMATCH`)
- `IN` tuple/row shape checks (`IN_ROW_SHAPE_MISMATCH`)
- Set-operation shape checks (`SET_OPERATION_COLUMN_COUNT_MISMATCH`)

### 5. Aggregation and Functions

- Non-aggregated select item misuse with grouping (`AGGREGATION_MISUSE`)
- Function signature checks: argument count/kind (`FUNCTION_SIGNATURE_MISMATCH`)
- Function return type participates in type inference (for other rules)

### 6. Ordering / Grouping / Pagination

- `ORDER BY` ordinal range checks (`ORDER_BY_INVALID_ORDINAL`)
- `GROUP BY` ordinal range checks (`GROUP_BY_INVALID_ORDINAL`)
- `LIMIT/OFFSET` expression validation (`LIMIT_OFFSET_INVALID`)
- Set-operation `ORDER BY` restrictions (`SET_OPERATION_ORDER_BY_INVALID`)

### 7. CTE Validation

- Duplicate CTE names (`DUPLICATE_CTE_NAME`)
- CTE alias count vs projection width (`CTE_COLUMN_ALIAS_COUNT_MISMATCH`)
- Non-recursive self-reference checks (`CTE_RECURSION_NOT_ALLOWED`)
- Recursive CTE shape checks (`CTE_RECURSIVE_STRUCTURE_INVALID`)
- Recursive anchor/recursive type compatibility (`CTE_RECURSIVE_TYPE_MISMATCH`)

### 8. Window Validation

- Missing window references (`WINDOW_NOT_FOUND`)
- Duplicate window names (`DUPLICATE_WINDOW_NAME`)
- Window inheritance cycles (`WINDOW_INHERITANCE_CYCLE`)
- Window frame validity (`WINDOW_FRAME_INVALID`)

### 9. Locking

- `FOR ... OF` lock target visibility (`LOCK_TARGET_NOT_FOUND`)

## PostgreSQL Dialect Module (`sqm-validate-postgresql`)

PostgreSQL-specific validation is implemented in a dedicated dialect module:
- `io.sqm.validate.postgresql.PostgresValidationDialect`

### PostgreSQL-Specific Feature Validation

- Version-gated PostgreSQL features (`DIALECT_FEATURE_UNSUPPORTED`), including:
  - `DISTINCT ON`
  - locking variants (`FOR SHARE`, `FOR KEY SHARE`, `FOR NO KEY UPDATE`, `OF`, `NOWAIT`, `SKIP LOCKED`)
  - `LATERAL`, function tables and `WITH ORDINALITY`
  - PostgreSQL expression/predicate features (`ILIKE`, `SIMILAR TO`, regex predicates, arrays, `COLLATE`, `AT TIME ZONE`, custom operators, PostgreSQL cast syntax, power operator)
  - grouping extensions (`GROUPING SETS`, `ROLLUP`, `CUBE`)
  - window extensions (`GROUPS`, `EXCLUDE`)
  - CTE materialization hints

### PostgreSQL Clause Consistency Validation

- `DISTINCT ON` must match leftmost `ORDER BY` prefix (`DISTINCT_ON_ORDER_BY_MISMATCH`)
- PostgreSQL clause consistency (`DIALECT_CLAUSE_INVALID`), including:
  - `ORDER BY ... USING` constraints
  - `LATERAL` wrapper constraints
  - invalid lock context combinations
  - duplicate `FOR ... OF` targets
  - locking nullable side of outer joins

### PostgreSQL Function Catalog

- PostgreSQL-aware function signatures and return-type inference are provided by
  `io.sqm.validate.postgresql.function.PostgresFunctionCatalog`.

## Extension Architecture

### SchemaValidationSettings

Use `SchemaValidationSettings` to customize:
- function catalog (`FunctionCatalog`)
- additional rules (`SchemaValidationRule`)
- access policy (`CatalogAccessPolicy`)

### SchemaValidationSettingsLoader (JSON/YAML)

`SchemaValidationSettingsLoader` can build `SchemaValidationSettings` from configuration text:

- `SchemaValidationSettingsLoader.fromJson(...)`
- `SchemaValidationSettingsLoader.fromYaml(...)`

Example JSON access policy config:

```json
{
  "accessPolicy": {
    "deniedTables": ["users"],
    "deniedColumns": ["users.secret"],
    "allowedFunctions": ["count", "lower"],
    "principals": [
      {
        "name": "analyst",
        "deniedTables": ["payments"],
        "deniedColumns": ["users.email"],
        "allowedFunctions": ["sum", "avg"]
      }
    ]
  },
  "limits": {
    "maxJoinCount": 5,
    "maxSelectColumns": 50
  }
}
```

Equivalent YAML access policy config:

```yaml
accessPolicy:
  deniedTables:
    - users
  deniedColumns:
    - users.secret
  allowedFunctions:
    - count
    - lower
  principals:
    - name: analyst
      deniedTables:
        - payments
      deniedColumns:
        - users.email
      allowedFunctions:
        - sum
        - avg
limits:
  maxJoinCount: 5
  maxSelectColumns: 50
```

### SchemaValidationDialect

Use `SchemaValidationDialect` to package dialect-specific behavior:
- dialect name
- dialect function catalog
- additional rules

Create validator with:
- `SchemaQueryValidator.of(schema, settings)`
- `SchemaQueryValidator.of(schema, dialect)`

Default behavior is unchanged when using:
- `SchemaQueryValidator.of(schema)`
- `SchemaQueryValidator.of(schema, functionCatalog)`

## Known Constraints (Current State)

- Type inference is intentionally conservative and may return `UNKNOWN` for complex expressions.
- Validation operates on the SQM model, not on actual database metadata snapshots at runtime.
- It validates semantic model consistency, not execution plans or optimizer behavior.

## Improvement Backlog

### High Priority

1. Expand PostgreSQL function catalog coverage and version granularity.
2. Add richer diagnostics payload (rule id, offending node summary, optional hint).
3. Add end-to-end parser-to-validator PostgreSQL integration tests.

### Medium Priority

1. Expand type inference across more expression forms.
2. Add configurable severity levels (error/warn) per rule.
3. Add configurable rule toggles for strict vs relaxed modes.

### Nice to Have

1. Validation report formatter (human-readable grouped output).
2. Rule execution metrics (which rules fired and counts).
3. Optional “quick-fix hints” for common failures.

## Test Coverage Notes

- `sqm-validate` has focused unit tests for both valid and invalid scenarios.
- Extension path has tests proving:
  - settings-based construction works
  - additional dialect rules are applied

## Suggested Next Step

1. Continue expanding dialect-specific rule packs starting with deeper PostgreSQL semantic checks.
2. Extend dialect/version matrix coverage for newly added clause-consistency rules.
3. Document per-dialect rule inventories and examples.
