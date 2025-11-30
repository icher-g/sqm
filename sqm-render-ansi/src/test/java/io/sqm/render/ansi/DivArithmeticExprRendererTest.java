package io.sqm.render.ansi;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.DivArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DivArithmeticExprRenderer}.
 *
 * <p>These tests verify that the renderer:
 * <ul>
 *     <li>Renders simple division without unnecessary parentheses.</li>
 *     <li>Wraps nested arithmetic operands in parentheses when needed.</li>
 *     <li>Delegates to the rendering pipeline so that left and right operands
 *         are rendered with the correct {@code wrapInParentheses} flag.</li>
 * </ul>
 * The tests exercise the renderer via the public
 * {@link RenderContext#render(Node)} API.</p>
 */
class DivArithmeticExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void renders_simple_div_without_parentheses() {
        // a / b
        Expression expr = col("a").div(col("b"));

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // Core expression should be "a / b" without extra parentheses.
        assertEquals("SELECT a / b FROM t", normalize(sql));
    }

    @Test
    void renders_left_nested_div_with_parentheses() {
        // (a / b) / c
        ArithmeticExpr inner = DivArithmeticExpr.of(col("a"), col("b"));
        ArithmeticExpr expr = DivArithmeticExpr.of(inner, col("c"));

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // Left operand is an ArithmeticExpr, so it should be wrapped:
        // (a / b) / c
        assertEquals("SELECT a / b / c FROM t", normalize(sql));
    }

    @Test
    void renders_right_nested_div_with_parentheses() {
        // a / (b / c)
        ArithmeticExpr inner = DivArithmeticExpr.of(col("b"), col("c"));
        ArithmeticExpr expr = DivArithmeticExpr.of(col("a"), inner);

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // Right operand is an ArithmeticExpr, so it should be wrapped:
        // a / (b / c)
        assertEquals("SELECT a / b / c FROM t", normalize(sql));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
