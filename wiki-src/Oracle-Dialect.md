# Oracle Dialect

SQM supports Oracle query and DML parsing, rendering, validation, DSL construction,
Java code generation, JSON, and middleware integration. The default SQM capability
baseline is Oracle 19.0; version-gated features such as LATERAL and
MATCH_RECOGNIZE are enabled from 12.1.

## Modules

Add the Oracle parser and renderer with the same version as your other SQM modules:

```xml
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-parser-oracle</artifactId>
  <version>${sqm.version}</version>
</dependency>
<dependency>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-render-oracle</artifactId>
  <version>${sqm.version}</version>
</dependency>
```

Add `sqm-validate-oracle` for Oracle semantic checks and `sqm-catalog-oracle`
for Oracle JDBC type mapping. Shared model nodes and DSL helpers remain in
`sqm-core`; the Oracle-specific capability matrix is in `sqm-core-oracle`.

## Parse and render

```java
import io.sqm.core.Query;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;

var parser = ParseContext.of(new OracleSpecs());
var renderer = RenderContext.of(new OracleDialect());
var parsed = parser.parse(Query.class, """
    SELECT u.id, u.name
    FROM users u
    WHERE u.id = :id
    ORDER BY u.id
    FETCH FIRST 10 ROWS ONLY
    """);
if (!parsed.ok()) throw new IllegalArgumentException(parsed.errorMessage());
var query = parsed.value();
var sql = renderer.render(query).sql();
```

Oracle rendering emits table aliases without `AS`. Column aliases may use `AS`,
and quoted identifiers preserve their spelling.

## Build a common query through DSL

```java
import static io.sqm.dsl.Dsl.*;

var page = select(col("u", "id"), col("u", "name"))
    .from(tbl("users").as("u"))
    .where(col("u", "id").gt(lit(0L)))
    .orderBy(col("u", "id"))
    .limit(10)
    .offset(20)
    .build();
var pageSql = renderer.render(page).sql();
```

The shared limit/offset model renders as Oracle `OFFSET ... FETCH`, rather than
`LIMIT`.

## Supported feature families

| Area | Shipped Oracle support |
|---|---|
| Common queries | Joins, grouping, CTEs, set operations, subqueries, quoted identifiers, and pagination |
| DML | INSERT, UPDATE, DELETE, baseline MERGE, and RETURNING ... INTO variable targets |
| Hints | Typed optimizer hint comments on supported query/DML statements |
| Sequences | NEXTVAL and CURRVAL expressions |
| Hierarchical queries | START WITH, CONNECT BY, NOCYCLE, PRIOR, and ORDER SIBLINGS BY |
| Relation transforms | Typed PIVOT and UNPIVOT, with dialect-specific restrictions |
| JSON relations | Typed JSON_TABLE sources and columns |
| Table access | Flashback, partition selection, and sampling in the supported grammar |
| Time and locking | AT TIME ZONE, FOR UPDATE, OF, NOWAIT, SKIP LOCKED, and WAIT |
| Pattern recognition | Typed MATCH_RECOGNIZE |
| Multidimensional calculations | Typed MODEL clauses, reference models, cell rules, FOR generators, and iteration |

The [model hierarchy and support notes](https://github.com/icher-g/sqm/blob/main/docs/model/MODEL.md)
define the detailed support boundary. A shared node does not imply acceptance by
every dialect.

## DML and returned values

Parse DML using `Statement.class`:

```java
import io.sqm.core.Statement;

var inserted = parser.parse(Statement.class, """
    INSERT INTO users (id, name)
    VALUES (:id, :name)
    RETURNING id INTO :out_id
    """);
if (!inserted.ok()) throw new IllegalArgumentException(inserted.errorMessage());
var insertSql = renderer.render(inserted.value()).sql();
```

Other forms:

```sql
UPDATE users
SET name = :name
WHERE id = :id
RETURNING id INTO :out_id;

DELETE FROM users
WHERE id = :id
RETURNING id INTO :out_id;

MERGE INTO users u
USING incoming_users s ON (u.id = s.id)
WHEN MATCHED THEN UPDATE SET name = s.name
WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name);
```

`RETURNING ... INTO` uses variable targets in the shared `ResultClause` model.
Register and bind output variables through your database execution layer or JDBC
driver. SQM constructs and processes SQL; it does not execute DML for you.

See [DML Statements](DML-Statements) and [Result Clause Helpers](Result-Clause-Helpers).

## Hierarchical queries

```sql
SELECT id, parent_id, name
FROM categories
START WITH parent_id IS NULL
CONNECT BY NOCYCLE PRIOR id = parent_id
ORDER SIBLINGS BY name
```

Hierarchical queries use `HierarchicalQueryClause` and dedicated traversal,
validation, rendering, and transpilation support.

## Pattern recognition with MATCH_RECOGNIZE

```sql
SELECT account_id, start_id, end_id
FROM pattern_events MATCH_RECOGNIZE (
    PARTITION BY account_id
    ORDER BY event_id
    MEASURES FIRST(A.event_id) AS start_id, LAST(B.event_id) AS end_id
    ONE ROW PER MATCH
    AFTER MATCH SKIP PAST LAST ROW
    PATTERN (A B+)
    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
) mr
ORDER BY account_id, start_id
```

The corresponding relation can be built through the public DSL:

```java
var recognized = matchRecognize(tbl("pattern_events"))
    .partitionBy(col("account_id"))
    .orderBy(col("event_id").asc())
    .measure(first(patternColumn("A", "event_id")), "start_id")
    .measure(last(patternColumn("B", "event_id")), "end_id")
    .oneRowPerMatch()
    .skipPastLastRow()
    .pattern(patternSequence(patternVar("A"), oneOrMore(patternVar("B"))))
    .define("A", patternColumn("A", "kind").eq(lit("A")))
    .define("B", patternColumn("B", "kind").eq(lit("B")))
    .as("mr")
    .build();
var matches = select(col("mr", "account_id"), col("mr", "start_id"), col("mr", "end_id"))
    .from(recognized)
    .orderBy(col("mr", "account_id"), col("mr", "start_id"))
    .build();
```

`PatternRecognitionTable` owns partitioning, ordering, measures, the typed
pattern tree, definitions, subsets, row-output mode, and skip behavior. Oracle
validation checks variable scopes and expression restrictions. The delivered
subset includes quantifiers, alternation, anchors, permutations, exclusions,
navigation, CLASSIFIER, MATCH_NUMBER, and RUNNING/FINAL measures.

See the [pattern-recognition design](https://github.com/icher-g/sqm/blob/main/docs/epics/R11_8_MATCH_RECOGNIZE_DESIGN.md).

## Multidimensional calculations with MODEL

This example is self-contained and requires only Oracle's DUAL table:

```sql
SELECT year_key, sales
FROM dual
MODEL
    DIMENSION BY (2025 AS year_key)
    MEASURES (100 AS sales)
    RULES ITERATE (3) (
        sales[2026 + ITERATION_NUMBER] = sales[2025 + ITERATION_NUMBER] * 2
    )
ORDER BY year_key
```

Its complete result is 100, 200, 400, and 800 for years 2025 through 2028.
The same query through DSL:

```java
var forecast = select(col("year_key"), col("sales"))
    .from(tbl("dual"))
    .model(model().dimension(lit(2025L), "year_key").measure(lit(100L), "sales")
        .rules(modelRules().iteration(iterationSpec(lit(3L)))
            .rule(cellTarget("sales", lit(2026L).add(iterationNumber())),
                cellRef("sales", lit(2025L).add(iterationNumber())).mul(lit(2L)))
            .build()).build())
    .orderBy(col("year_key"))
    .build();
var forecastSql = renderer.render(forecast).sql();
```

Ordinary column declarations can use `.dimension("product")` or
`.measure("amount", "sales")`. Computed declarations supply an expression and
output name. Rules accept targets and values directly, without an intermediate
`modelRule(...)` call.

The MODEL family includes partition/dimension/measure declarations, navigation
and uniqueness options, UPDATE/UPSERT modes, sequential/automatic and ordered
rules, symbolic and positional cells, FOR values/ranges/queries, iteration,
presence checks, aggregates, and named reference models. Optional fields are
nullable; nodes remain immutable.

Oracle validation separates input, main-rule, reference-query, and output scopes.
It checks declaration names, cell roles and arity, known types, generator limits,
expression placement, and query compatibility. Runtime data constraints remain
the engine's responsibility.

See the [MODEL design](https://github.com/icher-g/sqm/blob/main/docs/epics/ORACLE_MODEL_CLAUSE_DESIGN.md)
and [live MODEL matrix](https://github.com/icher-g/sqm/blob/main/docs/testing/ORACLE_MODEL_LIVE_EXECUTION.md).

## Validate Oracle semantics

```java
import io.sqm.validate.oracle.OracleValidationDialect;
import io.sqm.validate.schema.SchemaStatementValidator;

var validator = SchemaStatementValidator.of(schema, OracleValidationDialect.of());
var validation = validator.validate(query);
validation.problems().forEach(System.out::println);
```

Here `schema` is your catalog schema, supplied manually, from JSON, or through
JDBC introspection. `OracleSqlTypeMapper` maps Oracle JDBC types for schema-backed
validation and code generation.

See [Schema Validation](Schema-Validation), [Schema Introspection](Schema-Introspection),
and the [validation feature reference](https://github.com/icher-g/sqm/blob/main/docs/validation/VALIDATION_FEATURES.md).

## Transpilation and downstream support

Oracle is a built-in dialect for the transpiler, middleware contexts, playground,
and SQL-file codegen plugin (`<dialect>oracle</dialect>`).

Oracle-to-Oracle transpilation preserves supported MODEL and pattern-recognition
trees exactly. Other target dialects reject these families with feature-specific
problems; MODEL rejection happens before rewriting and includes source/target
dialects and the clause path. There is no general approximate MODEL rewrite.

Other feature families have their own exact, approximate, and unsupported rules.
Consult [SQL Transpilation](SQL-Transpilation) and the
[downstream support matrix](https://github.com/icher-g/sqm/blob/main/docs/downstream/DOWNSTREAM_SUPPORT_MATRIX.md).

## Live execution

```powershell
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=OracleDslExecutionIT" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
```

The existing Oracle CI job runs 70 cases, including 41 MODEL cases, in one
container. A method selector can run only the MODEL factory; see
[Integration Testing with Docker](Integration-Testing-with-Docker).

## Boundaries

- Legacy outer-join `(+)` syntax is intentionally unsupported; use ANSI joins.
- DDL, PL/SQL blocks, packages, and stored procedure calls are outside the current framework scope.
- Parser, renderer, validation, and transpilation support remain explicit per dialect and capability version.
- Oracle product features outside the shipped SQM subset are not implied by Oracle dialect support.

See [Unsupported Features](Unsupported-Features), [DML Statements](DML-Statements),
and [Rendering SQL](Rendering-SQL).
