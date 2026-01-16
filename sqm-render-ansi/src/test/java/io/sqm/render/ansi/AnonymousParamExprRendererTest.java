package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for AnonymousParamExprRenderer.
 */
class AnonymousParamExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Anonymous parameter renders as question mark")
    void anonymous_param_renders_as_question_mark() {
        var query = select(param()).from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        assertTrue(result.contains("?"));
    }

    @Test
    @DisplayName("Multiple anonymous parameters")
    void multiple_anonymous_params() {
        var query = select(param(), param(), param()).from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        // Count question marks
        int count = 0;
        for (char c : result.toCharArray()) {
            if (c == '?') count++;
        }
        assertEquals(3, count, "Should have 3 question marks for 3 parameters");
    }

    @Test
    @DisplayName("Anonymous parameter in WHERE clause")
    void anonymous_param_in_where() {
        var query = select(param()).from(tbl("users")).where(param().eq(5));
        String result = normalize(ctx.render(query).sql());
        // Should contain multiple question marks
        int count = 0;
        for (char c : result.toCharArray()) {
            if (c == '?') count++;
        }
        assertTrue(count >= 2, "Should have at least 2 question marks");
    }

    @Test
    @DisplayName("Anonymous parameter renders consistently")
    void anonymous_param_consistency() {
        var expr1 = param();
        var expr2 = param();
        String result1 = normalize(ctx.render(expr1).sql());
        String result2 = normalize(ctx.render(expr2).sql());
        assertEquals("?", result1);
        assertEquals("?", result2);
    }
}
