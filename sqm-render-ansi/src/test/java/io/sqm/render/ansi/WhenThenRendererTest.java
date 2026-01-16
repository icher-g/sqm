package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for WhenThenRenderer.
 */
class WhenThenRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("WHEN with simple comparison")
    void when_simple_comparison() {
        var caseExpr = when(col("status").eq(lit(1))).then(lit(0));
        String result = render(caseExpr);
        assertTrue(result.contains("WHEN"));
        assertTrue(result.contains("THEN"));
    }

    @Test
    @DisplayName("WHEN clause with column condition")
    void when_column_condition() {
        var caseExpr = when(col("threshold").eq(lit("adult"))).then(lit("minor"));
        String result = render(caseExpr);
        assertTrue(result.contains("WHEN"));
        assertTrue(result.contains("THEN"));
    }

    @Test
    @DisplayName("Multiple WHEN clauses")
    void multiple_when_clauses() {
        var caseExpr = kase(
            when(lit("A").eq(lit(95))).then(0),
            when(lit("B").eq(lit(85))).then(1),
            when(lit("C").eq(lit(75))).then(2));
        String result = render(caseExpr);
        // Count occurrences of WHEN
        int whenCount = countOccurrences(result);
        assertEquals(3, whenCount, "Should have 3 WHEN clauses");
    }

    @Test
    @DisplayName("WHEN clause with complex expressions")
    void when_complex_expressions() {
        var caseExpr = when(col("base").add(col("bonus")).eq(lit("high"))).then(lit("low"));
        String result = render(caseExpr);
        assertTrue(result.contains("WHEN"));
        assertTrue(result.contains("THEN"));
    }

    private int countOccurrences(String text) {
        return text.split("WHEN", -1).length - 1;
    }
}
