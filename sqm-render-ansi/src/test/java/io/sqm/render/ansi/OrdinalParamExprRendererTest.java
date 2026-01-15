package io.sqm.render.ansi;

import io.sqm.dsl.Dsl;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for OrdinalParamExprRenderer.
 * Note: ANSI does not support ordinal parameters, so they render as "?"
 */
class OrdinalParamExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Ordinal parameter renders as question mark (ANSI limitation)")
    void ordinal_param_renders_as_question_mark() {
        var param = Dsl.param(1);
        var query = select(param).from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        assertTrue(result.contains("?"), "Ordinal parameters render as ? in ANSI");
    }

    @Test
    @DisplayName("Multiple ordinal parameters all render as question marks")
    void multiple_ordinal_params() {
        var query = select(Dsl.param(1), Dsl.param(2), Dsl.param(3))
            .from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        int count = 0;
        for (char c : result.toCharArray()) {
            if (c == '?') count++;
        }
        assertEquals(3, count, "Should have 3 question marks for 3 ordinal parameters");
    }

    @Test
    @DisplayName("Ordinal parameters with different indices all render as question mark")
    void different_ordinal_indices_render_same() {
        var param1 = Dsl.param(1);
        var param2 = Dsl.param(5);
        var param3 = Dsl.param(10);
        String result1 = normalize(ctx.render(param1).sql());
        String result2 = normalize(ctx.render(param2).sql());
        String result3 = normalize(ctx.render(param3).sql());
        assertEquals("?", result1);
        assertEquals("?", result2);
        assertEquals("?", result3);
    }

    @Test
    @DisplayName("Ordinal parameter in WHERE clause")
    void ordinal_param_in_where() {
        var param = Dsl.param(1);
        var query = select(param).from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        assertTrue(result.contains("?"));
    }
}
