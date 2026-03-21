package io.sqm.dbit.sqlserver;

import io.sqm.core.OrderItem;
import io.sqm.core.QualifiedName;
import io.sqm.core.QuoteStyle;
import io.sqm.dbit.support.DialectExecutionCase;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqlServerExecutionCases {
    private static final List<DialectExecutionCase<SqlServerLiveFeature, SqlServerExecutionHarness>> CASES = List.of(
        new DialectExecutionCase<>(
            "bracketed-top-query",
            EnumSet.of(SqlServerLiveFeature.BRACKET_IDENTIFIERS, SqlServerLiveFeature.TOP),
            harness -> {
                var query = select(col(id("u", QuoteStyle.BRACKETS), id("id", QuoteStyle.BRACKETS)))
                    .from(tbl(id("users", QuoteStyle.BRACKETS)).as(id("u", QuoteStyle.BRACKETS)))
                    .top(top(1))
                    .orderBy(order(col(id("u", QuoteStyle.BRACKETS), id("id", QuoteStyle.BRACKETS))).asc())
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("TOP (1)"));
                assertEquals(List.of("1"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "top-percent-query",
            EnumSet.of(SqlServerLiveFeature.TOP_PERCENT),
            harness -> {
                var query = select(col("id"))
                    .from(tbl("users"))
                    .top(topPercent(lit(50)))
                    .orderBy(order(col("score")).desc(), order(col("id")).asc())
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("PERCENT"));
                assertEquals(List.of("1", "2"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "top-with-ties-query",
            EnumSet.of(SqlServerLiveFeature.TOP_WITH_TIES),
            harness -> {
                var query = select(col("id"))
                    .from(tbl("users"))
                    .top(topWithTies(lit(1)))
                    .orderBy(order(col("score")).desc())
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("WITH TIES"));
                assertEquals(List.of("1", "2"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "offset-fetch-query",
            EnumSet.of(SqlServerLiveFeature.OFFSET_FETCH),
            harness -> {
                var query = select(col("id"))
                    .from(tbl("users"))
                    .orderBy(OrderItem.of(col("id")))
                    .limitOffset(io.sqm.core.LimitOffset.of(lit(2), lit(1)))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("OFFSET 1 ROWS FETCH NEXT 2 ROWS ONLY"));
                assertEquals(List.of("2", "3"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "select-with-nolock",
            EnumSet.of(SqlServerLiveFeature.TABLE_LOCK_HINT_NOLOCK),
            harness -> {
                var query = select(col("id"))
                    .from(tbl("users").withNoLock())
                    .orderBy(order(col("id")).asc())
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("WITH (NOLOCK)"));
                assertEquals(List.of("1", "2", "3", "4"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "select-with-updlock-holdlock",
            EnumSet.of(SqlServerLiveFeature.TABLE_LOCK_HINT_UPDLOCK, SqlServerLiveFeature.TABLE_LOCK_HINT_HOLDLOCK),
            harness -> {
                var query = select(col("u", "id"))
                    .from(tbl("users").as("u").withUpdLock().withHoldLock())
                    .where(col("u", "id").eq(lit(1)))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("WITH (UPDLOCK, HOLDLOCK)"));
                assertEquals(List.of("1"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "scalar-functions-query",
            EnumSet.of(
                SqlServerLiveFeature.LEN,
                SqlServerLiveFeature.DATEADD,
                SqlServerLiveFeature.DATEDIFF,
                SqlServerLiveFeature.ISNULL
            ),
            harness -> {
                var query = select(
                    len(col("name")),
                    isNullFn(col("nickname"), lit("unknown"))
                ).from(tbl("users"))
                    .where(
                        col("id").eq(lit(1))
                            .and(dateDiff("day", col("created_at"), dateAdd("day", lit(2), col("created_at"))).eq(lit(2)))
                    )
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("DATEDIFF"));
                assertTrue(sql.contains("DATEADD"));
                assertEquals(List.of("5|unknown"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "string-agg-query",
            EnumSet.of(SqlServerLiveFeature.STRING_AGG),
            harness -> {
                var query = select(
                    stringAgg(col("name"), lit(",")).withinGroup(orderBy(order(col("name"))))
                ).from(tbl("users"))
                    .where(col("active").eq(lit(1)))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("STRING_AGG"));
                assertEquals(List.of("Alice,Carol,Dana"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "insert-output",
            EnumSet.of(SqlServerLiveFeature.INSERT_OUTPUT),
            harness -> {
                var statement = insert("users")
                    .columns(id("id"), id("name"), id("nickname"), id("active"), id("score"), id("created_at"))
                    .result(inserted("id").as("user_id"))
                    .values(row(lit(6), lit("Frank"), lit(null), lit(true), lit(70), lit("2024-01-06T05:00:00")))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("OUTPUT inserted.id"));
                assertEquals(List.of("6"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "update-output-into",
            EnumSet.of(SqlServerLiveFeature.UPDATE_OUTPUT_INTO),
            harness -> {
                var statement = update("users")
                    .set(id("name"), lit("Alicia"))
                    .result(resultInto("audit_names", "old_name", "new_name"), deleted("name"), inserted("name"))
                    .where(col("id").eq(lit(1)))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("OUTPUT deleted.name, inserted.name INTO audit_names"));
                assertEquals(1, harness.executeUpdate(sql, List.of()));
                assertEquals(List.of("Alice|Alicia"), harness.queryRows("select [old_name], [new_name] from [audit_names]"));
            }
        ),
        new DialectExecutionCase<>(
            "delete-output",
            EnumSet.of(SqlServerLiveFeature.DELETE_OUTPUT),
            harness -> {
                var statement = delete("users")
                    .result(deleted("id").as("deleted_id"))
                    .where(col("id").eq(lit(4)))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("OUTPUT deleted.id"));
                assertEquals(List.of("4"), harness.queryRows(sql));
                assertEquals(List.of("1", "2", "3"), harness.queryRows("select [id] from [users] order by [id]"));
            }
        ),
        new DialectExecutionCase<>(
            "merge-top-percent-upsert-delete",
            EnumSet.of(
                SqlServerLiveFeature.MERGE_MATCHED_UPDATE,
                SqlServerLiveFeature.MERGE_NOT_MATCHED_INSERT,
                SqlServerLiveFeature.MERGE_NOT_MATCHED_BY_SOURCE_DELETE,
                SqlServerLiveFeature.MERGE_CLAUSE_PREDICATE,
                SqlServerLiveFeature.MERGE_TOP_PERCENT
            ),
            harness -> {
                var statement = merge(tbl("users").withHoldLock())
                    .source(tbl("src_users").as("s"))
                    .on(col("users", "id").eq(col("s", "id")))
                    .top(topPercent(lit(100)))
                    .whenMatchedUpdate(col("s", "active").eq(lit(true)), List.of(set("name", col("s", "name"))))
                    .whenNotMatchedInsert(
                        col("s", "name").isNotNull(),
                        List.of(id("id"), id("name"), id("nickname"), id("active"), id("score"), id("created_at")),
                        row(col("s", "id"), col("s", "name"), lit(null), col("s", "active"), col("s", "score"), col("s", "created_at"))
                    )
                    .whenNotMatchedBySourceDelete(col("users", "active").eq(lit(false)))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("TOP (100) PERCENT"));
                assertEquals(4, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("1|Alicia", "3|Carol", "4|Dana", "5|Eve"),
                    harness.queryRows("select [id], [name] from [users] order by [id]")
                );
            }
        ),
        new DialectExecutionCase<>(
            "merge-top-delete-and-by-source-update",
            EnumSet.of(
                SqlServerLiveFeature.MERGE_MATCHED_DELETE,
                SqlServerLiveFeature.MERGE_NOT_MATCHED_BY_SOURCE_UPDATE,
                SqlServerLiveFeature.MERGE_TOP
            ),
            harness -> {
                harness.executeStatements(
                    "delete from [src_users]",
                    "insert into [src_users]([id], [name], [active], [score], [created_at]) values " +
                        "(1, 'Alicia', 0, 100, '2024-01-01T10:15:00')," +
                        "(4, 'Dana', 1, 80, '2024-01-04T07:45:00')"
                );

                var statement = merge("users")
                    .source(tbl("src_users").as("s"))
                    .on(col("users", "id").eq(col("s", "id")))
                    .top(top(2))
                    .whenMatchedDelete(col("s", "active").eq(lit(false)))
                    .whenNotMatchedBySourceUpdate(col("users", "active").eq(lit(true)), List.of(set(QualifiedName.of("users", "nickname"), lit("stale"))))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("TOP (2)"));
                assertEquals(2, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("2|null", "3|stale", "4|null"),
                    harness.queryRows("select [id], [nickname] from [users] order by [id]")
                );
            }
        )
    );

    private SqlServerExecutionCases() {
    }

    static List<DialectExecutionCase<SqlServerLiveFeature, SqlServerExecutionHarness>> cases() {
        return CASES;
    }

    static Set<SqlServerLiveFeature> coveredFeatures() {
        EnumSet<SqlServerLiveFeature> covered = EnumSet.noneOf(SqlServerLiveFeature.class);
        for (DialectExecutionCase<SqlServerLiveFeature, SqlServerExecutionHarness> testCase : CASES) {
            covered.addAll(testCase.features());
        }
        return covered;
    }
}
