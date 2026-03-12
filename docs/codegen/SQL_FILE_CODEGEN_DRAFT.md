# SQL File Codegen Draft

## Goal
Provide compile-time generation of SQM DSL code from `*.sql` files.
The generated API should return SQM model objects with parameter nodes preserved.
Parameter values are bound later at SQL rendering or execution time.

## Scope (V1)
- Input source: `src/main/sql/**/*.sql`.
- Output source: generated Java classes under `target/generated-sources/sqm-codegen`.
- Grouping rule: one generated class per folder under `src/main/sql`.
- Method rule: one zero-argument method per SQL file.
- Dialect-aware parse validation at build time.
- Build failure on invalid SQL or unsupported dialect features.

## Non-goals (V1)
- Java method parameters for SQL placeholders.
- Runtime parameter binding implementation.
- Cross-file SQL include/macros.
- IDE plugin support.

## Repository Integration

### New modules
1. `sqm-codegen`
- Pure library that scans SQL files, parses SQL, transforms parsed model to Java source, and writes deterministic files.

2. `sqm-codegen-maven-plugin`
- Maven plugin wrapper that invokes `sqm-codegen` in `generate-sources`.
- Adds generated folder to project sources.

### Dependencies
- Parser modules by dialect (`sqm-parser-ansi`, `sqm-parser-postgresql`, etc.).
- Core model and DSL modules (`sqm-core`, parser output types).
- Optional render modules only for snapshot tests.

## Input Layout and Naming

### Base directory
- Default: `src/main/sql`
- Configurable via plugin parameter: `sqlDirectory`

### Folder to class mapping
- Relative folder path from `sqlDirectory` maps to class name.
- Segment conversion: `snake_case` / `kebab-case` / `space` -> PascalCase.
- Class suffix: `Queries`.

Examples:
- `src/main/sql/user` -> `UserQueries`
- `src/main/sql/reporting/daily` -> `ReportingDailyQueries`
- root folder (`src/main/sql`) -> `RootQueries`

### File to method mapping
- File name without extension maps to method name.
- Conversion: `snake_case` / `kebab-case` -> camelCase.
- Extension must be exactly `.sql`.

Examples:
- `find_by_id.sql` -> `findById()`
- `list-active.sql` -> `listActive()`

### Collision policy
Build fails with a clear diagnostic when:
- Two folders map to the same generated class name.
- Two files in one folder map to the same method name.

## Generated API Contract

### Package name
- Configurable plugin parameter: `basePackage`.
- Default: `io.sqm.codegen.generated`.

### Class shape
Generated classes are `public final` and non-instantiable.

```java
package io.sqm.codegen.generated;

import io.sqm.core.query.Query;

/**
 * Generated from src/main/sql/user/*.sql.
 */
public final class UserQueries {

    private UserQueries() {
    }

    /**
     * SQL source: src/main/sql/user/find_by_id.sql
     */
    public static Query findById() {
        // generated DSL builder code that recreates parsed SQM model
    }

    /**
     * Parameters referenced by findById.sql.
     */
    public static java.util.Set<String> findByIdParams() {
        return java.util.Set.of("id");
    }
}
```

### Return type
- V1 default: `io.sqm.core.query.Query`.
- Optional future enhancement: narrow return type based on statement kind.

### Parameter handling
- Query method remains zero-arg.
- SQL placeholders are preserved as parameter nodes in returned model.
- Companion metadata method (`<method>Params`) exposes discovered parameter names.

## Diagnostics and Error Model

### Parse errors
- Fail build with path, line, column, and parser message.
- Example:
  - `src/main/sql/user/find_by_id.sql:3:14 unexpected token 'FROMM'`

### Dialect unsupported features
- Fail build with file and dialect name.
- Example:
  - `src/main/sql/user/lock.sql:1:32 feature 'FOR UPDATE SKIP LOCKED' not supported by ansi`

### Generation conflicts
- Fail build with exact conflicting input files/folders.

## Determinism Rules
- Stable file traversal order: lexicographic by relative path.
- Stable member order: lexicographic by method name.
- Stable formatting (fixed indentation and import sorting).
- Output only changes when input SQL/config changes.

## Maven Plugin Draft

### Coordinates
- `io.sqm:sqm-codegen-maven-plugin:${sqm.version}`

### Minimal configuration
```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${project.version}</version>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <dialect>postgresql</dialect>
        <basePackage>com.acme.sql</basePackage>
        <sqlDirectory>${project.basedir}/src/main/sql</sqlDirectory>
        <generatedSourcesDirectory>${project.build.directory}/generated-sources/sqm-codegen</generatedSourcesDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Plugin parameters
- `dialect` (required): `ansi`, `postgresql`, ...
- `basePackage` (optional)
- `sqlDirectory` (optional)
- `generatedSourcesDirectory` (optional)
- `failOnWarning` (optional, default `false`)

## Example

Input:
- `src/main/sql/user/find_by_id.sql`

```sql
select u.id, u.name
from users u
where u.id = :id
```

Generated method contract:
- `UserQueries.findById()` returns `Query` with one named parameter node (`id`).
- `UserQueries.findByIdParams()` returns `Set.of("id")`.

## Bigger Query Candidate

Use this SQL as a high-coverage codegen input:

```sql
with recent_orders as (
    select
        o.user_id,
        o.id as order_id,
        o.status,
        o.total_amount,
        o.created_at
    from orders o
    where o.created_at >= :from_ts
      and o.created_at < :to_ts
),
user_totals as (
    select
        ro.user_id,
        count(*) as order_cnt,
        sum(ro.total_amount) as total_amount,
        row_number() over (
            partition by ro.user_id
            order by sum(ro.total_amount) desc
        ) as rn
    from recent_orders ro
    where ro.status in (:status_open, :status_closed)
    group by ro.user_id
)
select
    u.id,
    u.user_name,
    ut.order_cnt,
    ut.total_amount,
    coalesce(addr.city, :default_city) as city,
    case
        when ut.total_amount > :vip_amount then :tier_vip
        when ut.total_amount > :pro_amount then :tier_pro
        else :tier_basic
    end as tier
from users u
left join user_totals ut on ut.user_id = u.id
left join lateral (
    select a.city
    from addresses a
    where a.user_id = u.id
      and a.active = true
    order by a.updated_at desc
    limit 1
) addr on true
where u.deleted_at is null
  and (u.country = :country or u.country is null)
  and u.user_name ilike :name_pattern
  and u.id in (
      select p.user_id
      from permissions p
      where p.scope = :scope
        and p.enabled = true
  )
order by ut.total_amount desc nulls last, u.id asc
limit :limit
offset :offset
for update of u skip locked
```

Covered features in one query:
- CTEs and nested subqueries.
- Joins, including lateral join.
- Function calls, aggregates, and window `over(...)`.
- `case` expression and aliasing.
- Complex predicates: `in`, `is null`, boolean composition, `ilike`.
- Ordering with nulls policy, pagination, and row locking.

## Implementation Plan

### Phase 1: Core generator
- Implement SQL file scanner and naming strategy.
- Parse each SQL file with selected dialect parser.
- Implement SQM model -> Java DSL emitter.
- Write generated files with deterministic formatting.

### Phase 2: Maven plugin
- Add `generate` goal.
- Wire generated-sources directory into compilation.
- Expose plugin configuration and validation.

### Phase 3: Tests
- Unit tests (in `sqm-codegen`):
  - naming strategy and collision detection.
  - parser diagnostics propagation.
  - emitted code snapshot tests.
  - parameter metadata extraction.
- Plugin tests (in `sqm-codegen-maven-plugin`):
  - integration test with sample project and compile success.
  - failing build on invalid SQL.

## Test Matrix (V1)

### Happy paths
- Single folder, multiple SQL files -> one class, multiple methods.
- Nested folders -> one class per folder.
- Named parameters preserved in AST.
- SQL with no parameters -> empty params set.

### Error paths
- Invalid SQL tokenization/parsing.
- Unsupported dialect feature.
- Duplicate method name after normalization.
- Duplicate class name after folder normalization.

### Boundary cases
- Root folder SQL files (`RootQueries`).
- Empty folder (no class generated).
- Mixed case file/folder names.
- Windows path separators handled correctly.

## Open Decisions
1. Should generated methods return `Query` only, or statement-specific interfaces (`SelectQuery`, `UpdateQuery`, ...)?
2. Do we want sidecar metadata files (`.sql.json`) in V1 for explicit method/class naming overrides?
3. Should parameter metadata also include positional index support for dialects using positional placeholders?
4. Should we add a strict mode that forbids `select *` in generated queries?

## Acceptance Criteria
- `mvn -pl <consumer-module> compile` generates Java classes from SQL files.
- Invalid SQL fails compilation with accurate file and position diagnostics.
- Generated query methods are zero-arg and preserve parameter nodes.
- Generated output is deterministic across repeated builds.
