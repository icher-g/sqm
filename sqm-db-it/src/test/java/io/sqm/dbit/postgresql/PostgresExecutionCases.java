package io.sqm.dbit.postgresql;

import io.sqm.dbit.support.DialectExecutionCase;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PostgresExecutionCases {
    private static final List<DialectExecutionCase<PostgresLiveFeature, PostgresExecutionHarness>> CASES = List.of(
        new DialectExecutionCase<>(
            "distinct-on-query",
            EnumSet.of(PostgresLiveFeature.DISTINCT_ON),
            harness -> {
                var query = select(col("e", "user_id"), col("e", "version"))
                    .from(tbl("events").as("e"))
                    .distinct(distinctOn(col("e", "user_id")))
                    .orderBy(
                        col("e", "user_id").asc(),
                        col("e", "version").desc()
                    )
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("DISTINCT ON"));
                assertEquals(List.of("1|2", "2|3"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "insert-returning",
            EnumSet.of(PostgresLiveFeature.INSERT_RETURNING),
            harness -> {
                var statement = insert("users")
                    .columns(id("id"), id("name"), id("active"))
                    .values(row(lit(6), lit("Frank"), lit(true)))
                    .result(col("id").toSelectItem(), col("name").toSelectItem())
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("RETURNING"));
                assertEquals(List.of("6|Frank"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "update-from",
            EnumSet.of(PostgresLiveFeature.UPDATE_FROM),
            harness -> {
                var statement = update("users")
                    .set(id("name"), col("src", "name"))
                    .from(tbl("source_users").as("src"))
                    .where(col("users", "id").eq(col("src", "id")))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("FROM source_users AS src"));
                assertEquals(2, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("1|Alicia", "2|Bob", "3|Carol"),
                    harness.queryRows("select id, name from users order by id")
                );
            }
        ),
        new DialectExecutionCase<>(
            "delete-using",
            EnumSet.of(PostgresLiveFeature.DELETE_USING),
            harness -> {
                var statement = delete("users")
                    .using(tbl("source_users").as("src"))
                    .where(col("users", "id").eq(col("src", "id")).and(col("src", "active").eq(lit(false))))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("USING source_users AS src"));
                assertEquals(1, harness.executeUpdate(sql, List.of()));
                assertEquals(List.of("1", "3"), harness.queryRows("select id from users order by id"));
            }
        ),
        new DialectExecutionCase<>(
            "insert-on-conflict-do-nothing",
            EnumSet.of(PostgresLiveFeature.ON_CONFLICT_DO_NOTHING),
            harness -> {
                var statement = insert("users")
                    .columns(id("id"), id("name"), id("active"))
                    .values(row(lit(1), lit("ignored"), lit(false)))
                    .onConflictDoNothing(id("id"))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("ON CONFLICT"));
                assertEquals(0, harness.executeUpdate(sql, List.of()));
                assertEquals(List.of("Alice"), harness.queryRows("select name from users where id = 1"));
            }
        ),
        new DialectExecutionCase<>(
            "insert-on-conflict-do-update",
            EnumSet.of(PostgresLiveFeature.ON_CONFLICT_DO_UPDATE),
            harness -> {
                var statement = insert("users")
                    .columns(id("id"), id("name"), id("active"))
                    .values(row(lit(1), lit("Alice Updated"), lit(false)))
                    .onConflictDoUpdate(
                        List.of(id("id")),
                        List.of(set("name", lit("Alice Updated")), set("active", lit(false)))
                    )
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("DO UPDATE"));
                assertEquals(1, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("Alice Updated|false"),
                    harness.queryRows("select name, active from users where id = 1")
                );
            }
        ),
        new DialectExecutionCase<>(
            "writable-cte-insert-returning",
            EnumSet.of(PostgresLiveFeature.WRITABLE_CTE_INSERT_RETURNING),
            harness -> {
                var query = with(
                    cte(
                        "ins",
                        insert("users")
                            .columns(id("id"), id("name"), id("active"))
                            .values(row(lit(7), lit("Grace"), lit(true)))
                            .result(col("id").toSelectItem())
                            .build()
                    )
                ).body(
                    select(col("ins", "id"))
                        .from(tbl("ins"))
                        .build()
                );

                var sql = harness.render(query);
                assertTrue(sql.startsWith("WITH"));
                assertEquals(List.of("7"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "writable-cte-update-returning",
            EnumSet.of(PostgresLiveFeature.WRITABLE_CTE_UPDATE_RETURNING),
            harness -> {
                var query = with(
                    cte(
                        "upd",
                        update("users")
                            .set(id("name"), lit("Alice CTE"))
                            .where(col("id").eq(lit(1)))
                            .result(col("id").toSelectItem())
                            .build()
                    )
                ).body(
                    select(col("upd", "id"))
                        .from(tbl("upd"))
                        .build()
                );

                var sql = harness.render(query);
                assertTrue(sql.contains("RETURNING"));
                assertEquals(List.of("1"), harness.queryRows(sql));
                assertEquals(List.of("Alice CTE"), harness.queryRows("select name from users where id = 1"));
            }
        ),
        new DialectExecutionCase<>(
            "writable-cte-delete-returning",
            EnumSet.of(PostgresLiveFeature.WRITABLE_CTE_DELETE_RETURNING),
            harness -> {
                var query = with(
                    cte(
                        "del",
                        delete("users")
                            .where(col("id").eq(lit(2)))
                            .result(col("id").toSelectItem())
                            .build()
                    )
                ).body(
                    select(col("del", "id"))
                        .from(tbl("del"))
                        .build()
                );

                var sql = harness.render(query);
                assertTrue(sql.contains("DELETE FROM"));
                assertEquals(List.of("2"), harness.queryRows(sql));
                assertEquals(List.of("1", "3"), harness.queryRows("select id from users order by id"));
            }
        ),
        new DialectExecutionCase<>(
            "merge-update-insert-do-nothing-returning",
            EnumSet.of(
                PostgresLiveFeature.MERGE_MATCHED_UPDATE,
                PostgresLiveFeature.MERGE_NOT_MATCHED_INSERT,
                PostgresLiveFeature.MERGE_NOT_MATCHED_BY_SOURCE_DO_NOTHING,
                PostgresLiveFeature.MERGE_RETURNING
            ),
            harness -> {
                var statement = merge("users")
                    .source(tbl("source_users").as("src"))
                    .on(col("users", "id").eq(col("src", "id")))
                    .whenMatchedUpdate(col("src", "active").eq(lit(true)), set("name", col("src", "name")))
                    .whenNotMatchedInsert(
                        col("src", "name").isNotNull(),
                        List.of(id("id"), id("name"), id("active")),
                        row(col("src", "id"), col("src", "name"), col("src", "active"))
                    )
                    .whenNotMatchedBySourceDoNothing(col("users", "active").eq(lit(false)))
                    .result(col("users", "id").toSelectItem())
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("MERGE INTO"));
                assertEquals(List.of("1", "4", "5"), harness.queryRows(sql));
                assertEquals(
                    List.of("1|Alicia|true", "2|Bob|false", "3|Carol|true", "4|Dave|true", "5|Eve|true"),
                    harness.queryRows("select id, name, active from users order by id")
                );
            }
        ),
        new DialectExecutionCase<>(
            "merge-delete-branches",
            EnumSet.of(
                PostgresLiveFeature.MERGE_MATCHED_DELETE,
                PostgresLiveFeature.MERGE_NOT_MATCHED_BY_SOURCE_DELETE
            ),
            harness -> {
                harness.executeStatements("delete from orders where user_id = 3");

                var statement = merge("users")
                    .source(tbl("source_users").as("src"))
                    .on(col("users", "id").eq(col("src", "id")))
                    .whenMatchedDelete(col("src", "active").eq(lit(false)))
                    .whenNotMatchedBySourceDelete(col("users", "active").eq(lit(true)))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("WHEN MATCHED"));
                assertEquals(2, harness.executeUpdate(sql, List.of()));
                assertEquals(List.of("1"), harness.queryRows("select id from users order by id"));
            }
        ),
        new DialectExecutionCase<>(
            "merge-do-nothing-branches",
            EnumSet.of(
                PostgresLiveFeature.MERGE_MATCHED_DO_NOTHING,
                PostgresLiveFeature.MERGE_NOT_MATCHED_DO_NOTHING
            ),
            harness -> {
                var statement = merge("users")
                    .source(tbl("source_users").as("src"))
                    .on(col("users", "id").eq(col("src", "id")))
                    .whenMatchedDoNothing(col("src", "id").eq(lit(1)))
                    .whenNotMatchedDoNothing(col("src", "id").eq(lit(4)))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("DO NOTHING"));
                assertEquals(0, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("1|Alice|true", "2|Bob|false", "3|Carol|true"),
                    harness.queryRows("select id, name, active from users order by id")
                );
            }
        )
    );

    private PostgresExecutionCases() {
    }

    static List<DialectExecutionCase<PostgresLiveFeature, PostgresExecutionHarness>> cases() {
        return CASES;
    }

    static Set<PostgresLiveFeature> coveredFeatures() {
        EnumSet<PostgresLiveFeature> covered = EnumSet.noneOf(PostgresLiveFeature.class);
        for (DialectExecutionCase<PostgresLiveFeature, PostgresExecutionHarness> testCase : CASES) {
            covered.addAll(testCase.features());
        }
        return covered;
    }
}
