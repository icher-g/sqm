package io.sqm.dbit.oracle;

import io.sqm.core.LockWaitMode;
import io.sqm.core.QuoteStyle;
import io.sqm.core.TableSampleSpec;
import io.sqm.dbit.support.DialectExecutionCase;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

final class OracleExecutionCases {
    private static final List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> CASES = List.of(
        new DialectExecutionCase<>(
            "quoted-fetch-pagination",
            EnumSet.of(
                OracleLiveFeature.DOUBLE_QUOTED_IDENTIFIERS,
                OracleLiveFeature.TABLE_ALIAS,
                OracleLiveFeature.FETCH_FIRST,
                OracleLiveFeature.OFFSET_FETCH
            ),
            harness -> {
                var quotedUsers = id("USERS", QuoteStyle.DOUBLE_QUOTE);
                var quotedAlias = id("U", QuoteStyle.DOUBLE_QUOTE);
                var quotedId = id("ID", QuoteStyle.DOUBLE_QUOTE);
                var query = select(col(quotedAlias, quotedId))
                    .from(tbl(quotedUsers).as(quotedAlias))
                    .orderBy(col(quotedAlias, quotedId))
                    .limitOffset(limitOffset(lit(1), lit(1)))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("\"USERS\""));
                assertTrue(sql.contains("OFFSET 1 ROWS FETCH FIRST 1 ROWS ONLY"));
                assertEquals(List.of("2"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "derived-and-lateral-table-aliases",
            EnumSet.of(OracleLiveFeature.DERIVED_TABLE_ALIAS, OracleLiveFeature.LATERAL_DERIVED_TABLE),
            harness -> {
                var derived = select(col("id"))
                    .from(tbl("users"))
                    .where(col("active").eq(lit(true)))
                    .build();
                var derivedQuery = select(col("active_users", "id"))
                    .from(tbl(derived).as("active_users"))
                    .orderBy(col("active_users", "id"))
                    .build();
                var lateral = select(col("id"))
                    .from(tbl("orders"))
                    .where(col("user_id").eq(col("u", "id")))
                    .build();
                var lateralQuery = select(col("u", "id"), col("o", "id"))
                    .from(tbl("users").as("u"))
                    .join(io.sqm.core.CrossJoin.of(tbl(lateral).as("o").lateral()))
                    .orderBy(col("u", "id").asc(), col("o", "id").asc())
                    .build();

                var derivedSql = harness.render(derivedQuery);
                var lateralSql = harness.render(lateralQuery);
                assertFalse(derivedSql.contains(") AS active_users"));
                assertFalse(lateralSql.contains(") AS o"));
                assertEquals(List.of("1"), harness.queryRows(derivedSql));
                assertEquals(List.of("1|10", "1|11", "2|12"), harness.queryRows(lateralSql));
            }
        ),
        new DialectExecutionCase<>(
            "optimizer-hint-query",
            EnumSet.of(OracleLiveFeature.OPTIMIZER_HINT_COMMENT),
            harness -> {
                var query = select(col("id"))
                    .hint("FULL", id("users"))
                    .from(tbl("users"))
                    .orderBy(col("id"))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("/*+ FULL(users) */"));
                assertEquals(List.of("1", "2"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "dml-optimizer-hints",
            EnumSet.of(OracleLiveFeature.DML_OPTIMIZER_HINT_COMMENT),
            harness -> {
                var insert = insert("users")
                    .hint("APPEND")
                    .columns(id("id"), id("name"), id("active"))
                    .values(row(lit(4), lit("Dana"), lit(true)))
                    .build();
                var update = update("users")
                    .hint("FULL", id("users"))
                    .set("name", lit("Dana Updated"))
                    .where(col("id").eq(lit(4)))
                    .build();
                var delete = delete("users")
                    .hint("FULL", id("users"))
                    .where(col("id").eq(lit(4)))
                    .build();

                var insertSql = harness.render(insert);
                var updateSql = harness.render(update);
                var deleteSql = harness.render(delete);
                assertTrue(insertSql.startsWith("INSERT /*+ APPEND */"));
                assertTrue(updateSql.startsWith("UPDATE /*+ FULL(users) */"));
                assertTrue(deleteSql.startsWith("DELETE /*+ FULL(users) */"));
                assertEquals(1, harness.executeUpdate(insertSql, List.of()));
                assertEquals(1, harness.executeUpdate(updateSql, List.of()));
                assertEquals(1, harness.executeUpdate(deleteSql, List.of()));
            }
        ),
        new DialectExecutionCase<>(
            "insert-update-delete",
            EnumSet.of(OracleLiveFeature.INSERT, OracleLiveFeature.UPDATE, OracleLiveFeature.DELETE),
            harness -> {
                var insert = insert("users")
                    .columns(id("id"), id("name"), id("active"))
                    .values(row(lit(4), lit("Dana"), lit(true)))
                    .build();
                var update = update("users")
                    .set("name", lit("Dana Updated"))
                    .where(col("id").eq(lit(4)))
                    .build();
                var delete = delete("users")
                    .where(col("id").eq(lit(4)))
                    .build();

                assertEquals(1, harness.executeUpdate(harness.render(insert), List.of()));
                assertEquals(1, harness.executeUpdate(harness.render(update), List.of()));
                assertEquals(List.of("Dana Updated"), harness.queryRows("select name from users where id = 4"));
                assertEquals(1, harness.executeUpdate(harness.render(delete), List.of()));
                assertEquals(List.of(), harness.queryRows("select id from users where id = 4"));
            }
        ),
        new DialectExecutionCase<>(
            "insert-returning-into",
            EnumSet.of(OracleLiveFeature.RETURNING_INTO),
            harness -> {
                var statement = insert("users")
                    .columns(id("id"), id("name"), id("active"))
                    .values(row(lit(4), lit("Dana"), lit(true)))
                    .result(resultVariableTarget(param(1)), col("id"))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("RETURNING id INTO :1"));
                assertEquals(4L, harness.executeReturningInto(sql));
            }
        ),
        new DialectExecutionCase<>(
            "merge-update-and-insert",
            EnumSet.of(OracleLiveFeature.MERGE_MATCHED_UPDATE, OracleLiveFeature.MERGE_NOT_MATCHED_INSERT),
            harness -> {
                var statement = merge("users")
                    .source(tbl("src_users").as("src"))
                    .on(col("users", "id").eq(col("src", "id")))
                    .whenMatchedUpdate(set("name", col("src", "name")))
                    .whenNotMatchedInsert(
                        List.of(id("id"), id("name"), id("active")),
                        row(col("src", "id"), col("src", "name"), col("src", "active"))
                    )
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("WHEN MATCHED THEN UPDATE"));
                assertEquals(2, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("1|Alicia|1", "2|Bob|0", "3|Carol|1"),
                    harness.queryRows("select id, name, active from users order by id")
                );
            }
        ),
        new DialectExecutionCase<>(
            "sequence-next-and-current-value",
            EnumSet.of(OracleLiveFeature.SEQUENCE_NEXT_VALUE, OracleLiveFeature.SEQUENCE_CURRENT_VALUE),
            harness -> {
                var query = select(nextValue("users_seq"), currentValue("users_seq")).from(tbl("dual")).build();

                var sql = harness.render(query);
                assertTrue(sql.contains("users_seq.NEXTVAL"));
                assertTrue(sql.contains("users_seq.CURRVAL"));
                assertEquals(List.of("100|100"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "hierarchical-connect-by",
            EnumSet.of(OracleLiveFeature.HIERARCHICAL_QUERY),
            harness -> {
                var query = select(col("id"), col("LEVEL"))
                    .from(tbl("categories"))
                    .hierarchical(hierarchy(
                        col("parent_id").isNull(),
                        prior(col("id")).eq(col("parent_id")),
                        false,
                        orderBy(col("name"))
                    ))
                    .build();

                var sql = harness.render(query);
                assertTrue(normalizeSql(sql).contains("START WITH parent_id IS NULL CONNECT BY PRIOR id = parent_id ORDER SIBLINGS BY name"));
                assertEquals(List.of("1|1", "2|2", "3|2"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "pivot-and-unpivot",
            EnumSet.of(OracleLiveFeature.PIVOT, OracleLiveFeature.UNPIVOT),
            harness -> {
                var pivotSource = select(col("quarter"), col("amount"))
                    .from(tbl("sales"))
                    .build();
                var pivotQuery = select(star())
                    .from(pivot(
                        tbl(pivotSource).as("source"),
                        List.of(pivotMeasure(func("sum", col("amount")), "total")),
                        col("quarter"),
                        pivotValue(lit("Q1"), "q1"), pivotValue(lit("Q2"), "q2")
                    ))
                    .build();
                var unpivotQuery = select(col("quarter"), col("amount"))
                    .from(unpivot(
                        tbl("sales_wide"),
                        "amount",
                        "quarter",
                        unpivotInput("q1", lit("Q1")), unpivotInput("q2", lit("Q2"))
                    ))
                    .orderBy(col("quarter"))
                    .build();

                var pivotSql = harness.render(pivotQuery);
                var unpivotSql = harness.render(unpivotQuery);
                assertTrue(pivotSql.contains("PIVOT"));
                assertTrue(unpivotSql.contains("UNPIVOT"));
                assertEquals(List.of("30|30"), harness.queryRows(pivotSql));
                assertEquals(List.of("Q1|10", "Q2|20"), harness.queryRows(unpivotSql));
            }
        ),
        new DialectExecutionCase<>(
            "json-table",
            EnumSet.of(OracleLiveFeature.JSON_TABLE),
            harness -> {
                var query = select(col("jt", "id"))
                    .from(jsonTable(
                        lit("{\"id\":42}"),
                        jsonPath("$"),
                        jsonScalar("id", type("NUMBER"), jsonPath("$.id"))
                    ).as("jt"))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("JSON_TABLE"));
                assertFalse(sql.contains(") AS jt"));
                assertEquals(List.of("42"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "table-access-modifiers",
            EnumSet.of(
                OracleLiveFeature.FLASHBACK_AS_OF_TIMESTAMP,
                OracleLiveFeature.TABLE_PARTITION,
                OracleLiveFeature.TABLE_SAMPLING
            ),
            harness -> {
                var flashbackQuery = select(col("id"))
                    .from(tbl("users").withVersion(asOfTimestamp(param("as_of"))))
                    .orderBy(col("id"))
                    .build();
                var partitionQuery = select(col("id"))
                    .from(tbl("sales").withPartitionSpec(tablePartition("sales_q1")))
                    .orderBy(col("id"))
                    .build();
                var samplingQuery = select(col("u", "id"))
                    .from(sampled(
                        tbl("users"),
                        tableSample(
                            TableSampleSpec.SampleMethod.DIALECT_DEFAULT,
                            TableSampleSpec.SampleUnit.PERCENT,
                            lit(100),
                            lit(42)
                        )
                    ).as(id("u")))
                    .orderBy(col("u", "id"))
                    .build();

                var flashbackSql = harness.render(flashbackQuery);
                var partitionSql = harness.render(partitionQuery);
                var samplingSql = harness.render(samplingQuery);
                assertTrue(flashbackSql.contains("AS OF TIMESTAMP :as_of"));
                assertTrue(partitionSql.contains("PARTITION (sales_q1)"));
                assertTrue(samplingSql.contains("SAMPLE (100) SEED (42)"));
                assertEquals(List.of("1", "2"), harness.queryRows(flashbackSql, List.of(harness.currentDatabaseTimestamp())));
                assertEquals(List.of("1", "2"), harness.queryRows(partitionSql));
                assertEquals(List.of("1", "2"), harness.queryRows(samplingSql));
            }
        ),
        new DialectExecutionCase<>(
            "time-zone-and-lock-wait",
            EnumSet.of(OracleLiveFeature.AT_TIME_ZONE, OracleLiveFeature.LOCK_WAIT),
            harness -> {
                var timeZoneQuery = select(col("created_at").atTimeZone(lit("UTC")))
                    .from(tbl("events"))
                    .build();
                var lockingQuery = select(col("id"))
                    .from(tbl("users"))
                    .where(col("id").eq(lit(1)))
                    .lockFor(update(), List.of(), LockWaitMode.WAIT, lit(1))
                    .build();

                var timeZoneSql = harness.render(timeZoneQuery);
                var lockingSql = harness.render(lockingQuery);
                assertTrue(timeZoneSql.contains("AT TIME ZONE 'UTC'"));
                assertTrue(lockingSql.endsWith("FOR UPDATE WAIT 1"));
                assertEquals(1, harness.queryRows(timeZoneSql).size());
                assertEquals(List.of("1"), harness.queryRows(lockingSql));
            }
        )
    );

    private OracleExecutionCases() {
    }

    static List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> cases() {
        return CASES;
    }

    static Set<OracleLiveFeature> coveredFeatures() {
        EnumSet<OracleLiveFeature> covered = EnumSet.noneOf(OracleLiveFeature.class);
        for (DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness> testCase : CASES) {
            covered.addAll(testCase.features());
        }
        return covered;
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
