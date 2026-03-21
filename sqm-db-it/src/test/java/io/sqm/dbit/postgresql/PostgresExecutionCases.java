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
            "join-aggregate-query",
            EnumSet.of(
                PostgresLiveFeature.INNER_JOIN,
                PostgresLiveFeature.AGGREGATE_FUNCTION,
                PostgresLiveFeature.WHERE_PREDICATE,
                PostgresLiveFeature.GROUP_BY,
                PostgresLiveFeature.ORDER_BY
            ),
            harness -> {
                var query = select(
                        col("u", "name"),
                        func("count", starArg()).as("cnt"),
                        func("sum", arg(col("o", "amount"))).as("total")
                    )
                    .from(tbl("users").as("u"))
                    .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
                    .where(col("u", "active").eq(true).and(col("o", "status").eq("PAID")))
                    .groupBy(group("u", "name"))
                    .orderBy(order(col("u", "name")).asc())
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("JOIN"));
                assertTrue(sql.contains("GROUP BY"));

                assertEquals(
                    List.of("Alice|1|12.50", "Carol|1|5.25"),
                    harness.queryRows(sql)
                );
            }
        ),
        new DialectExecutionCase<>(
            "distinct-on-query",
            EnumSet.of(
                PostgresLiveFeature.DISTINCT_ON,
                PostgresLiveFeature.ORDER_BY
            ),
            harness -> {
                var query = select(col("e", "user_id"), col("e", "version"))
                    .from(tbl("events").as("e"))
                    .distinct(distinctOn(col("e", "user_id")))
                    .orderBy(
                        order(col("e", "user_id")).asc(),
                        order(col("e", "version")).desc()
                    )
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("DISTINCT ON"));

                assertEquals(
                    List.of("1|2", "2|3"),
                    harness.queryRows(sql)
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
