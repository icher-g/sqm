package io.sqm.dbit.oracle;

import io.sqm.core.Query;
import io.sqm.dbit.support.DialectExecutionCase;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;

import java.util.EnumSet;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OraclePatternRecognitionExecutionCases {
    private static final List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> CASES = List.of(
        parsedCase(
            "match-recognize-one-row-two-partitions",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_ONE_ROW_PER_MATCH),
            """
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
                """,
            List.of("1|1|3", "1|4|6", "2|1|2", "2|3|5")
        ),
        parsedCase(
            "match-recognize-all-rows",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_ALL_ROWS_PER_MATCH),
            """
                SELECT account_id, event_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id
                    ORDER BY event_id
                    ALL ROWS PER MATCH
                    PATTERN (A B+)
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, event_id
                """,
            List.of("1|1", "1|2", "1|3", "1|4", "1|5", "1|6", "2|1", "2|2", "2|3", "2|4", "2|5")
        ),
        parsedCase(
            "match-recognize-overlap-skip-next-row",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_SKIP_NEXT_ROW),
            """
                SELECT account_id, start_id, end_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id
                    ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, LAST(B.event_id) AS end_id
                    ONE ROW PER MATCH
                    AFTER MATCH SKIP TO NEXT ROW
                    PATTERN (A B)
                    DEFINE A AS A.amount > 0, B AS B.amount > A.amount
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|2", "1|2|3", "1|4|5", "1|5|6", "2|1|2", "2|3|4", "2|4|5")
        ),
        parsedCase(
            "match-recognize-skip-to-variable",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_SKIP_TO_VARIABLE),
            """
                SELECT account_id, start_id, end_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id
                    ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, LAST(C.event_id) AS end_id
                    ONE ROW PER MATCH
                    AFTER MATCH SKIP TO B
                    PATTERN (A B C)
                    DEFINE A AS A.amount > 0, B AS B.amount > 0, C AS C.amount > 0
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|3", "1|2|4", "1|3|5", "1|4|6", "2|1|3", "2|2|4", "2|3|5")
        ),
        parsedComparisonCase(
            "match-recognize-greedy-and-reluctant",
            EnumSet.of(
                OracleLiveFeature.MATCH_RECOGNIZE_GREEDY_QUANTIFIER,
                OracleLiveFeature.MATCH_RECOGNIZE_RELUCTANT_QUANTIFIER
            ),
            """
                SELECT account_id, start_id, end_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, LAST(B.event_id) AS end_id
                    PATTERN (A+ B)
                    DEFINE A AS A.amount > 0, B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|6", "2|1|5"),
            """
                SELECT account_id, start_id, end_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, LAST(B.event_id) AS end_id
                    PATTERN (A+? B)
                    DEFINE A AS A.amount > 0, B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|2", "1|3|5", "2|1|2", "2|3|4")
        ),
        parsedCase(
            "match-recognize-bounded-quantifier",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_BOUNDED_QUANTIFIER),
            """
                SELECT account_id, start_id, end_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, LAST(A.event_id) AS end_id
                    PATTERN (A{2,3})
                    DEFINE A AS A.amount > 0
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|3", "1|4|6", "2|1|3", "2|4|5")
        ),
        parsedCase(
            "match-recognize-alternation-and-anchors",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_ALTERNATION_ANCHORS),
            """
                SELECT account_id, first_a, last_b
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS first_a, LAST(B.event_id) AS last_b
                    PATTERN (^ (A | B)+ $)
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY account_id
                """,
            List.of("1|1|6", "2|1|5")
        ),
        parsedCase(
            "match-recognize-permute",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_PERMUTE),
            """
                SELECT account_id, match_no, a_id, b_id
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES MATCH_NUMBER() AS match_no,
                             FIRST(A.event_id) AS a_id,
                             FIRST(B.event_id) AS b_id
                    PATTERN (PERMUTE(A, B))
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, match_no
                """,
            List.of("1|1|1|2", "1|2|4|3", "2|1|1|2", "2|2|3|4")
        ),
        parsedCase(
            "match-recognize-exclusion-all-rows",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_EXCLUSION),
            """
                SELECT account_id, event_id, label
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES CLASSIFIER() AS label
                    ALL ROWS PER MATCH
                    PATTERN (A {- B -} C)
                    DEFINE A AS A.amount > 0, B AS B.amount > 0, C AS C.amount > 0
                ) mr
                ORDER BY account_id, event_id
                """,
            List.of("1|1|A", "1|3|C", "1|4|A", "1|6|C", "2|1|A", "2|3|C")
        ),
        parsedCase(
            "match-recognize-subset-measure",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_SUBSET),
            """
                SELECT account_id, start_id, total_amount
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, SUM(U.amount) AS total_amount
                    PATTERN (A B+)
                    SUBSET U = (A, B)
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|36", "1|4|33", "2|1|42", "2|3|58")
        ),
        parsedCase(
            "match-recognize-match-number-and-classifier",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_MATCH_NUMBER_CLASSIFIER),
            """
                SELECT account_id, event_id, match_no, label
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES MATCH_NUMBER() AS match_no, CLASSIFIER() AS label
                    ALL ROWS PER MATCH
                    PATTERN (A B+)
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, event_id
                """,
            List.of(
                "1|1|1|A", "1|2|1|B", "1|3|1|B", "1|4|2|A", "1|5|2|B", "1|6|2|B",
                "2|1|1|A", "2|2|1|B", "2|3|2|A", "2|4|2|B", "2|5|2|B"
            )
        ),
        parsedCase(
            "match-recognize-logical-physical-compound-navigation",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_NAVIGATION),
            """
                SELECT account_id, start_id, first_b, last_b, compound_previous
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id,
                             FIRST(B.amount) AS first_b,
                             LAST(B.amount) AS last_b,
                             PREV(LAST(B.amount, 1), 1) AS compound_previous
                    PATTERN (A B+)
                    DEFINE A AS A.kind = 'A',
                           B AS B.kind = 'B' AND B.amount > PREV(B.amount)
                ) mr
                ORDER BY account_id, start_id
                """,
            List.of("1|1|12|14|10", "1|4|11|13|9", "2|1|22|22|null", "2|3|19|21|18")
        ),
        parsedCase(
            "match-recognize-running-and-final",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_RUNNING_FINAL),
            """
                SELECT account_id, event_id, running_amount, final_amount
                FROM pattern_events MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES RUNNING LAST(B.amount) AS running_amount,
                             FINAL LAST(B.amount) AS final_amount
                    ALL ROWS PER MATCH
                    PATTERN (A B+)
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY account_id, event_id
                """,
            List.of(
                "1|1|null|14", "1|2|12|14", "1|3|14|14", "1|4|null|13", "1|5|11|13", "1|6|13|13",
                "2|1|null|22", "2|2|22|22", "2|3|null|21", "2|4|19|21", "2|5|21|21"
            )
        ),
        parsedCase(
            "match-recognize-query-table-and-result-alias",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_QUERY_TABLE_ALIAS),
            """
                SELECT mr.account_id, mr.start_id, mr.end_id
                FROM (
                    SELECT account_id, event_id, amount, kind
                    FROM pattern_events
                    WHERE account_id = 1
                ) MATCH_RECOGNIZE (
                    PARTITION BY account_id ORDER BY event_id
                    MEASURES FIRST(A.event_id) AS start_id, LAST(B.event_id) AS end_id
                    PATTERN (A B+)
                    DEFINE A AS A.kind = 'A', B AS B.kind = 'B'
                ) mr
                ORDER BY mr.start_id
                """,
            List.of("1|1|3", "1|4|6")
        ),
        new DialectExecutionCase<>(
            "match-recognize-representative-dsl",
            EnumSet.of(OracleLiveFeature.MATCH_RECOGNIZE_DSL),
            harness -> {
                var recognition = matchRecognize(tbl("pattern_events"))
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
                var query = select(col("mr", "account_id"), col("mr", "start_id"), col("mr", "end_id"))
                    .from(recognition)
                    .orderBy(col("mr", "account_id"), col("mr", "start_id"))
                    .build();

                var sql = harness.render(query);
                assertTrue(sql.contains("MATCH_RECOGNIZE"));
                assertEquals(List.of("1|1|3", "1|4|6", "2|1|2", "2|3|5"), harness.queryRows(sql));
            }
        )
    );

    private OraclePatternRecognitionExecutionCases() {
    }

    static List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> cases() {
        return CASES;
    }

    private static DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness> parsedCase(
        String id,
        EnumSet<OracleLiveFeature> features,
        String sql,
        List<String> expectedRows
    ) {
        var query = parse(sql);
        return new DialectExecutionCase<>(id, features, harness -> assertRows(harness, query, expectedRows));
    }

    private static DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness> parsedComparisonCase(
        String id,
        EnumSet<OracleLiveFeature> features,
        String firstSql,
        List<String> firstExpectedRows,
        String secondSql,
        List<String> secondExpectedRows
    ) {
        var firstQuery = parse(firstSql);
        var secondQuery = parse(secondSql);
        return new DialectExecutionCase<>(id, features, harness -> {
            assertRows(harness, firstQuery, firstExpectedRows);
            assertRows(harness, secondQuery, secondExpectedRows);
        });
    }

    private static Query parse(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, sql);
        if (result.isError()) {
            throw new IllegalArgumentException("Invalid Oracle live-engine fixture: " + result.errorMessage());
        }
        return result.value();
    }

    private static void assertRows(OracleExecutionHarness harness, Query query, List<String> expectedRows)
        throws Exception {
        var rendered = harness.render(query);
        assertTrue(rendered.contains("MATCH_RECOGNIZE"));
        assertEquals(expectedRows, harness.queryRows(rendered));
    }
}
