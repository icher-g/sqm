package io.sqm.json;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class PatternRecognitionJsonTest {
    @Test
    void roundTripsCompletePatternRecognitionRelation() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var quotedA = id("A", QuoteStyle.DOUBLE_QUOTE);
        var quotedAlias = id("Match Result", QuoteStyle.DOUBLE_QUOTE);
        var pattern = patternSequence(
            patternStart(),
            patternAlternation(
                excludePattern(patternVar(quotedA)),
                patternPermute(patternVar("B"), repeatPattern(patternVar("C"), 1, 3, true))
            ),
            emptyPattern(),
            patternEnd()
        );
        var table = matchRecognize(tbl("events"))
            .partitionBy(col("account_id"))
            .orderBy(col("event_time").asc())
            .measure(running(first(patternColumn(quotedA, id("amount")), 1)), id("first_amount"))
            .measure(classifier(), "classifier_name")
            .measure(matchNumber(), "match_no")
            .rowsPerMatch(allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY))
            .afterMatchSkip(skipToLast(quotedA))
            .pattern(pattern)
            .subset(patternSubset(id("ABC"), quotedA, id("B"), id("C")))
            .define(patternDefinition(quotedA, patternColumn(quotedA, id("amount")).gt(0)))
            .define(patternDefinition("B", patternColumn("B", "amount").gt(prev(patternColumn("B", "amount")))))
            .define(patternDefinition("C", patternColumn("C", "amount").gt(next(patternColumn("C", "amount"), 2))))
            .as(quotedAlias)
            .build();

        var json = mapper.writeValueAsString(table);
        var roundTrip = mapper.readValue(json, TableRef.class);

        assertEquals(table, roundTrip);
        assertInstanceOf(PatternRecognitionTable.class, roundTrip);
        assertTrue(json.contains("\"kind\":\"pattern_recognition_table\""));
        assertTrue(json.contains("\"kind\":\"pattern-sequence\""));
        assertTrue(json.contains("\"kind\":\"pattern-quantified\""));
        assertTrue(json.contains("\"kind\":\"pattern-navigation\""));
        assertFalse(json.contains("rawSql"));
    }

    @Test
    void roundTripsEveryPatternVariantThroughPatternRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var a = patternVar("A");
        var b = patternVar("B");
        List<MatchPattern> patterns = List.of(
            a,
            patternSequence(a, b),
            patternAlternation(a, b),
            patternPermute(a, b),
            patternStart(),
            emptyPattern(),
            excludePattern(a),
            repeatPattern(a, 0, 4, true)
        );

        for (var pattern : patterns) {
            var json = mapper.writeValueAsString(pattern);
            assertEquals(pattern, mapper.readValue(json, MatchPattern.class));
        }
    }

    @Test
    void roundTripsEveryScopedExpressionThroughExpressionRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        List<Expression> expressions = List.of(
            patternColumn("A", "amount"),
            classifier(),
            classifier("A"),
            matchNumber(),
            prev(patternColumn("A", "amount"), 2),
            finalValue(last(patternColumn("A", "amount")))
        );

        for (var expression : expressions) {
            var json = mapper.writeValueAsString(expression);
            assertEquals(expression, mapper.readValue(json, Expression.class));
        }
    }

    @Test
    void roundTripsClauseItemsAndOptionsThroughNodeRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        List<Node> nodes = List.of(
            patternMeasure(matchNumber(), "match_no"),
            patternDefinition("A", patternColumn("A", "amount").gt(0)),
            patternSubset("AB", "A", "B"),
            allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED),
            skipToFirst("A")
        );

        for (var node : nodes) {
            var json = mapper.writeValueAsString(node);
            assertEquals(node, mapper.readValue(json, Node.class));
        }
    }
}
