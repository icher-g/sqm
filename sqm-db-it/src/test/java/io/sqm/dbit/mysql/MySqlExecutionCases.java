package io.sqm.dbit.mysql;

import io.sqm.dbit.support.DialectExecutionCase;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MySqlExecutionCases {
    private static final List<DialectExecutionCase<MySqlLiveFeature, MySqlExecutionHarness>> CASES = List.of(
        new DialectExecutionCase<>(
            "insert-ignore",
            EnumSet.of(MySqlLiveFeature.INSERT_IGNORE),
            harness -> {
                var statement = insert("users")
                    .ignore()
                    .columns(id("id"), id("name"), id("active"), id("payload"), id("created_at"))
                    .values(row(
                        lit(1),
                        lit("Ignored"),
                        lit(false),
                        func("JSON_OBJECT", arg(lit("user")), arg(func("JSON_OBJECT", arg(lit("id")), arg(lit(99))))),
                        lit("2024-02-01 00:00:00")
                    ))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.startsWith("INSERT IGNORE"));
                assertEquals(0, harness.executeUpdate(sql, List.of()));
                assertEquals(List.of("Alice"), harness.queryRows("select name from users where id = 1"));
            }
        ),
        new DialectExecutionCase<>(
            "on-duplicate-key-update",
            EnumSet.of(MySqlLiveFeature.ON_DUPLICATE_KEY_UPDATE),
            harness -> {
                var statement = insert("users")
                    .columns(id("id"), id("name"), id("active"), id("payload"), id("created_at"))
                    .values(row(
                        lit(1),
                        lit("Alicia"),
                        lit(false),
                        func("JSON_OBJECT", arg(lit("user")), arg(func("JSON_OBJECT", arg(lit("id")), arg(lit(1))))),
                        lit("2024-02-01 00:00:00")
                    ))
                    .onConflictDoUpdate(List.of(set("name", lit("Alicia"))))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
                assertTrue(harness.executeUpdate(sql, List.of()) > 0);
                assertEquals(List.of("Alicia"), harness.queryRows("select name from users where id = 1"));
            }
        ),
        new DialectExecutionCase<>(
            "replace-into",
            EnumSet.of(MySqlLiveFeature.REPLACE_INTO),
            harness -> {
                var statement = insert("users")
                    .replace()
                    .columns(id("id"), id("name"), id("active"), id("payload"), id("created_at"))
                    .values(row(
                        lit(4),
                        lit("Dylan"),
                        lit(true),
                        func("JSON_OBJECT", arg(lit("user")), arg(func("JSON_OBJECT", arg(lit("id")), arg(lit(4))))),
                        lit("2024-02-02 11:00:00")
                    ))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.startsWith("REPLACE INTO"));
                assertTrue(harness.executeUpdate(sql, List.of()) > 0);
                assertEquals(List.of("Dylan"), harness.queryRows("select name from users where id = 4"));
            }
        ),
        new DialectExecutionCase<>(
            "joined-update-with-index-hints",
            EnumSet.of(
                MySqlLiveFeature.JOINED_UPDATE,
                MySqlLiveFeature.JOINED_UPDATE_QUALIFIED_TARGET,
                MySqlLiveFeature.INDEX_HINTS
            ),
            harness -> {
                var statement = update(tbl("users").as("u").useIndex("idx_users_name"))
                    .join(inner(tbl("orders").as("o").forceIndex("idx_orders_user"))
                        .on(col("u", "id").eq(col("o", "user_id"))))
                    .set(io.sqm.core.QualifiedName.of("u", "name"), lit("PaidUser"))
                    .where(col("o", "status").eq(lit("closed")))
                    .build();

                var sql = harness.render(statement);
                assertTrue(sql.contains("USE INDEX"));
                assertTrue(sql.contains("FORCE INDEX"));
                assertEquals(3, harness.executeUpdate(sql, List.of()));
                assertEquals(
                    List.of("1|PaidUser", "2|PaidUser", "3|PaidUser"),
                    harness.queryRows("select id, name from users order by id")
                );
            }
        ),
        new DialectExecutionCase<>(
            "delete-using-join",
            EnumSet.of(MySqlLiveFeature.DELETE_USING_JOIN),
            harness -> {
                var statement = delete(tbl("orders"))
                    .using(tbl("orders"))
                    .join(inner(tbl("users")).on(col("orders", "user_id").eq(col("users", "id"))))
                    .where(col("users", "active").eq(lit(false)))
                    .build();

                var sql = harness.render(statement);
                assertTrue(normalize(sql).contains("USING orders INNER JOIN users"));
                assertEquals(1, harness.executeUpdate(sql, List.of()));
                assertEquals(List.of("10", "11", "12"), harness.queryRows("select id from orders order by id"));
            }
        ),
        new DialectExecutionCase<>(
            "straight-join-with-statement-hint",
            EnumSet.of(MySqlLiveFeature.OPTIMIZER_HINT_COMMENT, MySqlLiveFeature.STRAIGHT_JOIN),
            harness -> {
                var query = select(col("u", "id"))
                    .from(tbl("users").as("u"))
                    .join(straight(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
                    .where(col("o", "status").eq(lit("closed")))
                    .orderBy(order(col("u", "id")).asc())
                    .hint("MAX_EXECUTION_TIME", 1000)
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.startsWith("SELECT /*+ MAX_EXECUTION_TIME(1000) */"));
                assertTrue(sql.contains("STRAIGHT_JOIN"));
                assertEquals(List.of("1", "2", "3"), harness.queryRows(sql));
            }
        ),
        new DialectExecutionCase<>(
            "mysql-functions-and-interval",
            EnumSet.of(
                MySqlLiveFeature.JSON_EXTRACT,
                MySqlLiveFeature.DATE_ADD_INTERVAL,
                MySqlLiveFeature.CONCAT_WS
            ),
            harness -> {
                var query = select(
                    func("CONCAT_WS", arg(lit("-")), arg(lit("user")), arg(col("name")))
                ).from(tbl("users"))
                    .where(
                        func("JSON_EXTRACT", arg(col("payload")), arg(lit("$.user.id"))).eq(lit(1))
                            .and(func("DATE_ADD", arg(col("created_at")), arg(interval("1", "DAY")))
                                .eq(lit("2024-01-02 10:15:00")))
                    )
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("JSON_EXTRACT"));
                assertTrue(sql.contains("DATE_ADD"));
                assertTrue(sql.contains("CONCAT_WS"));
                assertEquals(List.of("user-Alice"), harness.queryRows(sql));
            }
        )
    );

    private MySqlExecutionCases() {
    }

    static List<DialectExecutionCase<MySqlLiveFeature, MySqlExecutionHarness>> cases() {
        return CASES;
    }

    static Set<MySqlLiveFeature> coveredFeatures() {
        EnumSet<MySqlLiveFeature> covered = EnumSet.noneOf(MySqlLiveFeature.class);
        for (DialectExecutionCase<MySqlLiveFeature, MySqlExecutionHarness> testCase : CASES) {
            covered.addAll(testCase.features());
        }
        return covered;
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
