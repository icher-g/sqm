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
 * Unit tests for NamedParamExprRenderer.
 * Note: ANSI does not support named parameters, so they render as "?"
 */
class NamedParamExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Named parameter renders as question mark (ANSI limitation)")
    void named_param_renders_as_question_mark() {
        var param = Dsl.param("userId");
        var query = select(param).from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        assertTrue(result.contains("?"), "Named parameters render as ? in ANSI");
    }

    @Test
    @DisplayName("Multiple named parameters all render as question marks")
    void multiple_named_params() {
        var query = select(Dsl.param("id"), Dsl.param("name"))
            .from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        int count = 0;
        for (char c : result.toCharArray()) {
            if (c == '?') count++;
        }
        assertEquals(2, count, "Should have 2 question marks for 2 named parameters");
    }

    @Test
    @DisplayName("Named parameter in WHERE clause")
    void named_param_in_where() {
        var param = Dsl.param("status");
        var query = select(param).from(tbl("users"));
        String result = normalize(ctx.render(query).sql());
        assertTrue(result.contains("?"));
    }

    @Test
    @DisplayName("Named parameters with different names all render the same")
    void different_named_params_render_identically() {
        var param1 = Dsl.param("firstName");
        var param2 = Dsl.param("lastName");
        String result1 = normalize(ctx.render(param1).sql());
        String result2 = normalize(ctx.render(param2).sql());
        assertEquals("?", result1);
        assertEquals("?", result2);
    }
}
