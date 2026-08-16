package io.sqm.dbit.oracle;

import io.sqm.core.QuoteStyle;
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
            "sequence-next-value",
            EnumSet.of(OracleLiveFeature.SEQUENCE_NEXT_VALUE),
            harness -> {
                var query = select(nextValue("users_seq")).from(tbl("dual")).build();

                var sql = harness.render(query);
                assertTrue(sql.contains("users_seq.NEXTVAL"));
                assertEquals(List.of("100"), harness.queryRows(sql));
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
}
