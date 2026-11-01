# DML Statements

SQM models INSERT, UPDATE, DELETE, and MERGE as immutable statements. Build them
with DSL, parse them using `Statement.class`, or include them in statement
sequences and supported query-source positions.

## Build through DSL

```java
import static io.sqm.dsl.Dsl.*;

var create = insert("users")
    .columns(id("id"), id("name"))
    .values(row(lit(7L), lit("Alice")))
    .build();
var change = update("users")
    .set("name", lit("Alice"))
    .where(col("id").eq(lit(7L)))
    .build();
var remove = delete("users")
    .where(col("id").eq(lit(7L)))
    .build();
```

Render a statement with the dialect that supports its syntax:

```java
var sql = RenderContext.of(new AnsiDialect()).render(change).sql();
```

## Parse existing DML

```java
var parser = ParseContext.of(new PostgresSpecs());
var statement = parser.parse(Statement.class, """
    WITH ins AS (
        INSERT INTO users (name) VALUES ('Alice') RETURNING id
    )
    SELECT id FROM ins
    """);
if (!statement.ok()) throw new IllegalArgumentException(statement.errorMessage());
var statementSql = RenderContext.of(new PostgresDialect()).render(statement.value()).sql();
```

## Dialect extensions

| Dialect | Supported families | Detailed guide |
|---|---|---|
| ANSI | Baseline INSERT VALUES/SELECT, UPDATE SET, and DELETE | [Core Model](Core-Model) |
| PostgreSQL | RETURNING, UPDATE FROM, DELETE USING, ON CONFLICT, writable CTEs, and version-gated MERGE forms | [PostgreSQL Dialect](PostgreSQL-Dialect) |
| MySQL | INSERT IGNORE, ON DUPLICATE KEY UPDATE, REPLACE, joined UPDATE/DELETE, optimizer and index hints, and STRAIGHT_JOIN | [MySQL Dialect](MySQL-Dialect) |
| SQL Server | OUTPUT/OUTPUT INTO and MERGE, including supported TOP and matched/source branches | [SQL Server Dialect](SQL-Server-Dialect) |
| Oracle | RETURNING INTO variable targets, baseline MERGE, optimizer hints, and sequence values | [Oracle Dialect](Oracle-Dialect) |

Version and dialect restrictions apply to the shared model. A MERGE or result
clause being representable does not make every combination legal in every database.

## PostgreSQL returned rows and conflicts

```sql
INSERT INTO users (name) VALUES ('Alice') RETURNING id;

UPDATE users u
SET name = src.name
FROM source_users src
WHERE u.id = src.id;

DELETE FROM users
USING source_users src
WHERE users.id = src.id;

INSERT INTO users (id, name) VALUES (7, 'Alice')
ON CONFLICT (id) DO UPDATE SET name = 'Alice';
```

PostgreSQL also supports the shipped writable CTE and MERGE subsets. See its
dialect guide for branch predicates, DO NOTHING, source matching, and version gates.

## MySQL joined updates

```sql
INSERT IGNORE INTO users (id, name) VALUES (7, 'Alice');

INSERT INTO users (id, name) VALUES (7, 'Alice')
ON DUPLICATE KEY UPDATE name = 'Alice';

UPDATE users AS u USE INDEX (idx_users_name)
INNER JOIN orders AS o FORCE INDEX FOR JOIN (idx_orders_user) ON u.id = o.user_id
SET u.name = 'Alice'
WHERE o.state = 'closed';

DELETE FROM users
USING users INNER JOIN orders ON users.id = orders.user_id
WHERE orders.state = 'closed';
```

Hints remain typed nodes. When a target dialect cannot preserve source-local
hints, transpilation reports the appropriate warning or unsupported outcome.

## SQL Server output

```sql
INSERT INTO [users] ([name])
OUTPUT inserted.[id]
VALUES ('Alice');

UPDATE [users]
SET [name] = 'Alice'
OUTPUT deleted.[name], inserted.[name] INTO [audit] ([old_name], [new_name])
WHERE [id] = 7;
```

## Oracle variable targets and MERGE

```sql
INSERT INTO users (id, name) VALUES (:id, :name)
RETURNING id INTO :out_id;

MERGE INTO users u
USING incoming_users s ON (u.id = s.id)
WHEN MATCHED THEN UPDATE SET name = s.name
WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name);
```

Output binding belongs to the execution layer. SQM models these targets through
`ResultClause`; it does not execute the statement or register driver output values.

## Hints, scripts, and query sources

- Statement hints belong to `Statement.hints()`; table hints belong to table nodes.
- Multi-statement SQL uses `StatementSequence`. Validation, transpilation, and
  middleware decisions cover the whole script rather than returning partial output.
- Existing DML query-source positions can contain supported query features such
  as Oracle MODEL; no separate MODEL-specific DML node is needed.
- DDL and procedural statements remain outside the framework scope.

## Related guides

- [Result Clause Helpers](Result-Clause-Helpers)
- [Parameters and Binding](Parameters-and-Binding)
- [SQL Transpilation](SQL-Transpilation)
- [Unsupported Features](Unsupported-Features)
