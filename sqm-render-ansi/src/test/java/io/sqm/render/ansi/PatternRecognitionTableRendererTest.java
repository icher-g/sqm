package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.core.RowsPerMatch;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatternRecognitionTableRendererTest {
    private final RenderContext ansi = RenderContext.of(new AnsiDialect());
    private final RenderContext enabled = RenderContext.of(new PatternAnsiDialect());

    @Test
    void rejectsPatternRecognitionWhenCapabilityIsDisabled() {
        var table = matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", col("amount").gt(0))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> ansi.render(table));
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansi.render(patternVar("A")));
    }

    @Test
    void rendersCompletePatternRecognitionTableCanonically() {
        var pattern = patternSequence(
            patternStart(),
            repeatPattern(patternVar("A"), 1, null, true),
            repeatPattern(patternAlternation(patternVar("B"), patternVar("C")), 2, 5, true),
            excludePattern(oneOrMore(patternVar("D"))),
            patternEnd()
        );
        var table = matchRecognize(tbl("sales"))
            .partitionBy(col("customer_id"))
            .orderBy(col("sale_date").asc())
            .measure(matchNumber(), "match_no")
            .measure(classifier(), "label")
            .measure(first(patternColumn("A", "amount")), "first_amount")
            .measure(finalValue(last(patternColumn("B", "amount"), 1)), "last_amount")
            .rowsPerMatch(allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED))
            .afterMatchSkip(skipToFirst("B"))
            .pattern(pattern)
            .subset("U", "B", "C")
            .define("A", patternColumn("A", "amount").gt(0))
            .define("B", patternColumn("B", "amount").gt(prev(patternColumn("B", "amount"))))
            .define("C", patternColumn("C", "amount").gt(0))
            .define("D", patternColumn("D", "amount").gt(0))
            .as("mr")
            .build();

        assertEquals(
            "sales MATCH_RECOGNIZE ( PARTITION BY customer_id ORDER BY sale_date ASC "
                + "MEASURES MATCH_NUMBER() AS match_no, CLASSIFIER() AS label, "
                + "FIRST(A.amount) AS first_amount, FINAL LAST(B.amount, 1) AS last_amount "
                + "ALL ROWS PER MATCH WITH UNMATCHED ROWS AFTER MATCH SKIP TO FIRST B "
                + "PATTERN (^ A+? (B | C){2,5}? {- D+ -} $) "
                + "SUBSET U = (B, C) DEFINE A AS A.amount > 0, "
                + "B AS B.amount > PREV(B.amount), C AS C.amount > 0, D AS D.amount > 0 ) AS mr",
            normalize(enabled.render(table).sql())
        );
    }

    @Test
    void rendersPatternPrecedenceWithoutPersistedGroupingNodes() {
        assertEquals("(A | B)+", renderPattern(oneOrMore(patternAlternation(patternVar("A"), patternVar("B")))));
        assertEquals("A (B | C)", renderPattern(patternSequence(
            patternVar("A"), patternAlternation(patternVar("B"), patternVar("C")))));
        assertEquals("A B | C", renderPattern(patternAlternation(
            patternSequence(patternVar("A"), patternVar("B")), patternVar("C"))));
        assertEquals("(A+)?", renderPattern(optionalPattern(oneOrMore(patternVar("A")))));
        assertEquals("A{0}", renderPattern(repeatPattern(patternVar("A"), 0, 0, false)));
        assertEquals("A{,5}", renderPattern(repeatPattern(patternVar("A"), 0, 5, false)));
        assertEquals("A{2,}", renderPattern(repeatPattern(patternVar("A"), 2, null, false)));
        assertEquals("PERMUTE(A, B | C)", renderPattern(patternPermute(
            patternVar("A"), patternAlternation(patternVar("B"), patternVar("C")))));
        assertEquals("()", renderPattern(emptyPattern()));
    }

    @Test
    void rendersRowsPerMatchAndSkipVariants() {
        assertEquals("ONE ROW PER MATCH", normalize(enabled.render(oneRowPerMatch()).sql()));
        assertEquals("ALL ROWS PER MATCH", normalize(enabled.render(allRowsPerMatch()).sql()));
        assertEquals("ALL ROWS PER MATCH SHOW EMPTY MATCHES", normalize(enabled.render(
            allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY)).sql()));
        assertEquals("ALL ROWS PER MATCH OMIT EMPTY MATCHES", normalize(enabled.render(
            allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.OMIT_EMPTY)).sql()));
        assertEquals("ALL ROWS PER MATCH WITH UNMATCHED ROWS", normalize(enabled.render(
            allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED)).sql()));
        assertEquals("AFTER MATCH SKIP PAST LAST ROW", normalize(enabled.render(skipPastLastRow()).sql()));
        assertEquals("AFTER MATCH SKIP TO NEXT ROW", normalize(enabled.render(skipToNextRow()).sql()));
        assertEquals("AFTER MATCH SKIP TO A", normalize(enabled.render(skipToPattern("A")).sql()));
        assertEquals("AFTER MATCH SKIP TO FIRST A", normalize(enabled.render(skipToFirst("A")).sql()));
        assertEquals("AFTER MATCH SKIP TO LAST A", normalize(enabled.render(skipToLast("A")).sql()));
    }

    private String renderPattern(MatchPattern pattern) {
        return normalize(enabled.render(pattern).sql());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class PatternAnsiDialect extends AnsiDialect {
        private final DialectCapabilities capabilities = VersionedDialectCapabilities
            .builder(SqlDialectVersion.of(2016))
            .supports(SqlFeature.MATCH_RECOGNIZE)
            .build();

        @Override public DialectCapabilities capabilities() {
            return capabilities;
        }
    }
}
