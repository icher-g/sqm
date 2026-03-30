# Query Transform Helpers

SQM includes a small set of task-oriented helper APIs for common runtime query adaptation work.

These helpers are intended for cases where a query or statement is authored once during development and then adjusted at runtime for a concrete environment, tenant, schema, or execution mode.

Examples on this page family use the SQM DSL.

## Helper Pages

- [Identifier Transforms](Identifier-Transforms)
- [Relation Transforms](Relation-Transforms)
- [Statement Transforms](Statement-Transforms)
- [Literal Transforms](Literal-Transforms)
- [Result Clause Helpers](Result-Clause-Helpers)

## Typical Runtime Use Cases

- rename or remap columns for customer-specific schemas
- remap tables or apply a runtime schema before execution
- inject `WHERE` predicates such as tenant or visibility filters
- parameterize inline literals before bind execution
- normalize literal-heavy queries so shape-based comparisons ignore literal values

## Notes

- All helpers preserve identity when nothing changes.
- Relation rewrites already cover `ResultInto` table targets because those targets are part of the normal `TableRef` tree.
- Per-table `WHERE` helpers operate on real `Table` references visible in a statement block, not derived `FROM` items such as subquery aliases.
