package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class PatternRecognitionTableTest {
    @Test
    void builderCreatesCompleteImmutableRelationWithSemanticDefaults() {
        var table = matchRecognize(tbl("sales"))
            .partitionBy(col("customer_id"))
            .orderBy(col("sale_date").asc())
            .measure(matchNumber(), "match_no")
            .measure(first(patternColumn("start_row", "sale_date")), "start_date")
            .pattern(patternSequence(patternVar("start_row"), oneOrMore(patternVar("up_row"))))
            .subset("movement", "start_row", "up_row")
            .define("start_row", patternColumn("start_row", "amount").gte(0))
            .define("up_row", patternColumn("up_row", "amount").gt(prev(patternColumn("up_row", "amount"))))
            .as("mr")
            .build();

        assertEquals("sales", ((Table) table.source()).name().value());
        assertEquals(List.of(col("customer_id")), table.partitionBy().items());
        assertEquals(2, table.measures().size());
        assertEquals(RowsPerMatch.Mode.ONE, table.rowsPerMatch().mode());
        assertEquals(RowsPerMatch.EmptyMatchHandling.DEFAULT, table.rowsPerMatch().emptyMatchHandling());
        assertEquals(AfterMatchSkip.Kind.PAST_LAST_ROW, table.afterMatchSkip().kind());
        assertEquals(2, table.definitions().size());
        assertEquals("mr", table.alias().value());
        assertInstanceOf(MatchPattern.Sequence.class, table.pattern());
    }

    @Test
    void canonicalFactoryDefensivelyCopiesListsAndAliasCopiesPreserveState() {
        var measures = new ArrayList<>(List.of(patternMeasure(matchNumber(), "match_no")));
        var subsets = new ArrayList<>(List.of(patternSubset("all_rows", "A")));
        var definitions = new ArrayList<>(List.of(patternDefinition("A", patternColumn("A", "amount").gt(0))));
        var table = PatternRecognitionTable.of(
            tbl("sales"),
            null,
            null,
            measures,
            oneRowPerMatch(),
            skipPastLastRow(),
            patternVar("A"),
            subsets,
            definitions,
            null
        );

        measures.clear();
        subsets.clear();
        definitions.clear();

        assertEquals(1, table.measures().size());
        assertEquals(1, table.subsets().size());
        assertEquals(1, table.definitions().size());
        assertThrows(UnsupportedOperationException.class, () -> table.measures().clear());

        var aliased = table.as(id("MR"));
        assertNotSame(table, aliased);
        assertEquals(table.source(), aliased.source());
        assertEquals(table.pattern(), aliased.pattern());
        assertEquals("MR", aliased.alias().value());
    }

    @Test
    void builderCopyPreservesEveryFieldAndAllowsIndependentAppend() {
        var original = matchRecognize(tbl("sales"))
            .partitionBy(col("account_id"))
            .orderBy(col("event_time").desc())
            .measure(classifier(), "class_name")
            .allRowsPerMatch()
            .afterMatchSkip(skipToFirst("B"))
            .pattern(patternSequence(patternVar("A"), patternVar("B")))
            .subset(patternSubset("AB", "A", "B"))
            .define(patternDefinition("A", patternColumn("A", "amount").gt(0)))
            .define(patternDefinition("B", patternColumn("B", "amount").gt(0)))
            .as("mr")
            .build();

        var copy = PatternRecognitionTable.builder(original).build();
        assertEquals(original, copy);

        var extended = PatternRecognitionTable.builder(original)
            .measure(matchNumber(), "match_no")
            .build();
        assertEquals(1, original.measures().size());
        assertEquals(2, extended.measures().size());
    }

    @Test
    void rejectsInvalidRequiredStateAndStructuredOptions() {
        assertThrows(NullPointerException.class, () -> patternMeasure(null, "m"));
        assertThrows(NullPointerException.class, () -> PatternMeasure.of(matchNumber(), null));
        assertThrows(IllegalArgumentException.class, () -> PatternSubset.of(id("S"), List.of()));
        assertThrows(
            IllegalArgumentException.class,
            () -> RowsPerMatch.of(RowsPerMatch.Mode.ONE, RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY)
        );
        assertThrows(
            NullPointerException.class,
            () -> AfterMatchSkip.of(AfterMatchSkip.Kind.TO_VARIABLE, AfterMatchSkip.Position.DEFAULT, null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AfterMatchSkip.of(AfterMatchSkip.Kind.TO_NEXT_ROW, AfterMatchSkip.Position.FIRST, id("A"))
        );
        assertThrows(
            IllegalStateException.class,
            () -> matchRecognize(tbl("sales")).pattern(patternVar("A")).build()
        );
        assertThrows(
            NullPointerException.class,
            () -> PatternRecognitionTable.builder()
                .pattern(patternVar("A"))
                .define("A", patternColumn("A", "amount").gt(0))
                .build()
        );
    }
}
