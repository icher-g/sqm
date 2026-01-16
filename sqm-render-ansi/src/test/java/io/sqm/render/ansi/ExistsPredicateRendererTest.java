package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ExistsPredicateRenderer.
 */
class ExistsPredicateRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("EXISTS with subquery")
    void exists_with_subquery() {
        var subquery = select(col("1")).from(tbl("users"));
        var pred = exists(subquery);
        String result = render(pred);
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    @DisplayName("NOT EXISTS with subquery")
    void not_exists_with_subquery() {
        var subquery = select(col("1")).from(tbl("users"));
        var pred = notExists(subquery);
        String result = render(pred);
        assertTrue(result.contains("NOT EXISTS"));
        assertTrue(result.contains("SELECT"));
    }

    @Test
    @DisplayName("EXISTS is wrapped in parentheses")
    void exists_with_parentheses() {
        var subquery = select(col("1")).from(tbl("orders"));
        var pred = exists(subquery);
        String result = render(pred);
        assertTrue(result.contains("("));
        assertTrue(result.contains(")"));
    }

    @Test
    @DisplayName("NOT EXISTS is wrapped in parentheses")
    void not_exists_with_parentheses() {
        var subquery = select(col("1")).from(tbl("orders"));
        var pred = notExists(subquery);
        String result = render(pred);
        assertTrue(result.contains("("));
        assertTrue(result.contains(")"));
    }
}
