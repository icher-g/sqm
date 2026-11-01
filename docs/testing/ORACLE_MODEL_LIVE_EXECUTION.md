# Oracle MODEL live execution

Story [#508](https://github.com/icher-g/sqm/issues/508) adds a MODEL test factory
to the existing `OracleDslExecutionIT` suite in `sqm-db-it`. MODEL, the Oracle
baseline, and MATCH_RECOGNIZE share the same container and Oracle CI job.

## Run only MODEL

From the repository root, with Docker running:

```powershell
mvn -B -pl sqm-db-it -am verify -Pdocker-it "-Dit.test=OracleDslExecutionIT#executes_oracle_model_cases_against_live_db" "-Dsurefire.skip=true" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dapi.version=1.44"
```

Quote the entire `-D` argument in PowerShell. `surefire.skip` skips the reactor's
unit tests; `it.test` selects exactly the MODEL factory in the existing integration class. The suite
fails when Docker is unavailable instead of silently reporting a skipped live
test. The existing `oracle` job in `.github/workflows/live-db-it.yml` selects
`OracleDslExecutionIT`, running both factories on Linux with its Unix Docker
socket configured explicitly. For the same complete Oracle run locally, replace
the method selector with `"-Dit.test=OracleDslExecutionIT"`.

For fast fixture checks without Docker:

```powershell
mvn -B -pl sqm-db-it -am test "-Dtest=OracleModelExecutionCasesTest,OracleLiveFeatureCoverageTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Those checks parse every SQL fixture, render and reparse every query, compare
the complete AST, run Oracle validation, and check unique case identifiers and
coverage of every declared MODEL live feature. They do not replace live execution.

## Fixtures and assertions

The suite shares the Oracle 23 Free Testcontainers image, JDBC support, readiness
check, and streaming startup-log consumer with the existing Oracle harness.
Each dynamic test recreates only its five compact MODEL fixture tables. Setup
uses JDBC autocommit, so every case starts with committed, isolated data.

`model_sales` contains two partitions, `N` and `S`, with two products (`Bike`,
`Car`) and two years (2025, 2026). The N amounts are 10, 20, 30, and NULL; S uses
100, 200, 300, and NULL. `model_rates` supplies rates 2 and 3. Two small tables
supply scalar and tuple FOR queries; `model_output` receives DML results.
No case uses wall-clock data, randomness, sleeps, or an external database.

All 35 query cases compare every returned column and row in explicit SQL order.
The four DML cases compare both affected-row counts and the complete ordered
destination table. Two further runtime rejection cases assert Oracle's duplicate
dimension and self-cyclic multi-cell error codes and then compare the complete
unchanged input rows. Tests do not accept successful execution alone. The DSL case
uses concise declarations and incrementally built cells, without direct Impl
construction.

## Execution matrix

| Area                         | Cases / distinguishing behavior                                                                                                                                     |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Partitions and returned rows | Separate N/S calculations, explicit RETURN ALL, RETURN UPDATED with unchanged rows excluded                                                                         |
| Rule modes                   | UPDATE ignores missing cells; UPSERT inserts positional targets but does not insert symbolic targets; UPSERT ALL inserts symbolic combinations; per-rule overrides  |
| Ordering                     | SEQUENTIAL and AUTOMATIC use the same dependency example with different expected results; a descending ordered rule reads cells updated earlier in that order       |
| Navigation                   | KEEP NAV and IGNORE NAV read both an existing NULL and a missing cell                                                                                               |
| Uniqueness                   | UNIQUE DIMENSION; UNIQUE SINGLE REFERENCE updates duplicate dimension rows without an ambiguous RHS read                                                            |
| Addressing                   | Positional cells, IN/BETWEEN symbolic selectors reading an unchanged measure, wildcard self-updates with ANY, named and bare CV, case-sensitive quoted declarations |
| Aggregation                  | SUM over a multidimensional cell range; UPDATE with a partitioned analytic SUM                                                                                      |
| FOR values and queries       | Scalar IN values, tuple IN values, scalar SELECT source, tuple SELECT source                                                                                        |
| FOR ranges                   | Inclusive increment, decrement, and a LIKE template generating string coordinates                                                                                   |
| Iteration                    | Fixed iteration with ITERATION_NUMBER, iteration-generated coordinates, UNTIL/PREVIOUS termination with an observed step counter                                    |
| Presence                     | PRESENTV distinguishes missing from NULL; PRESENTNNV distinguishes NULL from a value; IS PRESENT observes the original input even after another rule inserts a cell |
| References                   | Qualified reference and named-main reads; nested single-cell reads and a static nested target under AUTOMATIC ORDER                                                 |
| Fluent DSL                   | A complete two-product/two-year generated forecast, constructed without parsing SQL                                                                                 |
| DML query sources            | INSERT SELECT, MERGE source query, UPDATE scalar subquery, DELETE EXISTS subquery                                                                                   |
| Runtime rejection            | Duplicate dimensions (ORA-32638) and a bounded IN/BETWEEN self-referencing sequential rule (ORA-32637); both leave the fixture unchanged                            |

The wildcard self-update and the bounded IN/BETWEEN self-reference deliberately
have separate expectations. On the tested Oracle image the wildcard form
executes, while the bounded form raises ORA-32637. The symbolic and analytic
positive cases read an unchanged measure, avoiding that dependency problem.

## Local execution evidence

On 2026-11-01, the method selector above executed only the MODEL factory in
`io.sqm.dbit.oracle.OracleDslExecutionIT` against the shared
`gvenzl/oracle-free:23-slim-faststart` container: **41 tests, zero failures, zero
errors, zero skips**. This covers 35 queries, four DML statements, and two runtime
rejections. JavaDoc generation was skipped on the final focused rerun; a separate
`mvn -B verify` passed all 43 repository modules, including JavaDoc generation,
the offline MODEL matrix, and the playground service/endpoint tests. JaCoCo
reports 100% line coverage for `ExampleCatalog`, the only changed production
class. No production classes are introduced.

The complete `OracleDslExecutionIT` run passed 70 baseline, MATCH_RECOGNIZE, MODEL,
and flashback-regression cases with zero failures, errors, or skips and one
container startup. The new regression reads a freshly recreated flashback fixture
three times. Its timestamp is checked with TIMESTAMP_TO_SCN against the committed
fixture's ORA_ROWSCN boundary, replacing the fixed timestamp offset and assumptions
about time spent in earlier cases. [PR #513](https://github.com/icher-g/sqm/pull/513)
records the final CI, live-engine, and Codecov checks for the delivery commit.

## Support and completion boundary

MODEL is represented by shared semantic nodes, with Oracle-specific acceptance
in parsing, rendering, and validation. ANSI, PostgreSQL, MySQL, and SQL Server
reject it explicitly. Oracle-to-Oracle transpilation preserves it exactly when
the target capabilities support MODEL; other targets receive a path-aware
`UNSUPPORTED_MODEL_CLAUSE` diagnostic before any rewrite. No approximate rewrite
or framework DDL support is introduced here.

The playground's Sales Forecast example is constructed using the public DSL
(`.dimension("product")`, `.measure("amount", "sales")`, and direct `.rule(...)`).
The Reference Forecast demonstrates a named reference, qualified reads, a FOR
range, and CV. Both examples support AST/JSON inspection, formatting, validation,
and Java DSL generation through the existing playground tests.
The new Iterative Forecast is also built through DSL and is self-contained: it
starts with 100 in 2025 and generates 200, 400, and 800 for 2026–2028 using
ITERATION_NUMBER. It requires only Oracle's DUAL table; the live matrix checks
the same query and all four result rows.

Final MODEL delivery is [PR #513](https://github.com/icher-g/sqm/pull/513) for
#508, following the merged #504–#507 stories. The design is complete once that
PR is merged; [epic #503](https://github.com/icher-g/sqm/issues/503) records
closure after all five stories are merged. The execution and coverage evidence
above supports that completion record.
