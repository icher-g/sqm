package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for LikePredicateRenderer.
 */
class LikePredicateRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Simple LIKE with literal pattern")
    void simple_like() {
        var pred = col("name").like(lit("John%"));
        String result = render(pred);
        assertEquals("name LIKE 'John%'", result);
    }

    @Test
    @DisplayName("NOT LIKE with literal pattern")
    void not_like() {
        var pred = col("email").notLike(lit("%@example.com"));
        String result = render(pred);
        assertEquals("email NOT LIKE '%@example.com'", result);
    }

    @Test
    @DisplayName("LIKE with ESCAPE clause")
    void like_with_escape() {
        var pred = col("text").like(lit("100\\%")).escape(lit("\\"));
        String result = render(pred);
        assertEquals("text LIKE '100\\%' ESCAPE '\\'", result);
    }

    @Test
    @DisplayName("NOT LIKE with ESCAPE clause")
    void not_like_with_escape() {
        var pred = col("text").notLike(lit("50\\%")).escape(lit("\\"));
        String result = render(pred);
        assertEquals("text NOT LIKE '50\\%' ESCAPE '\\'", result);
    }

    @Test
    @DisplayName("Qualified column LIKE")
    void qualified_column_like() {
        var pred = col("users", "name").like(lit("A%"));
        String result = render(pred);
        assertEquals("users.name LIKE 'A%'", result);
    }
}
