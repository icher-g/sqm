package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for IsNullPredicateRenderer.
 */
class IsNullPredicateRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Column IS NULL")
    void is_null() {
        var pred = col("status").isNull();
        String result = render(pred);
        assertEquals("status IS NULL", result);
    }

    @Test
    @DisplayName("Column IS NOT NULL")
    void is_not_null() {
        var pred = col("status").isNotNull();
        String result = render(pred);
        assertEquals("status IS NOT NULL", result);
    }

    @Test
    @DisplayName("Qualified column IS NULL")
    void qualified_column_is_null() {
        var pred = col("t", "status").isNull();
        String result = render(pred);
        assertEquals("t.status IS NULL", result);
    }

    @Test
    @DisplayName("Qualified column IS NOT NULL")
    void qualified_column_is_not_null() {
        var pred = col("t", "status").isNotNull();
        String result = render(pred);
        assertEquals("t.status IS NOT NULL", result);
    }
}
