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
 * Unit tests for UnaryOperatorExprRenderer.
 */
class UnaryOperatorExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Unary minus on literal")
    void unary_minus_literal() {
        var expr = lit(42).unary("-");
        String result = render(expr);
        assertEquals("-42", result);
    }

    @Test
    @DisplayName("Unary minus on column")
    void unary_minus_column() {
        var expr = col("balance").unary("-");
        String result = render(expr);
        assertEquals("-balance", result);
    }

    @Test
    @DisplayName("Unary plus on literal")
    void unary_plus_literal() {
        var expr = lit(42).unary("+");
        String result = render(expr);
        assertEquals("+42", result);
    }

    @Test
    @DisplayName("Unary plus on column")
    void unary_plus_column() {
        var expr = col("amount").unary("+");
        String result = render(expr);
        assertEquals("+amount", result);
    }

    @Test
    @DisplayName("Unary operator on qualified column")
    void unary_operator_qualified_column() {
        var expr = col("t", "value").unary("-");
        String result = render(expr);
        assertEquals("-t.value", result);
    }
}
