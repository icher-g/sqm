package io.sqm.render.ansi;

import io.sqm.core.AddArithmeticExpr;
import io.sqm.core.ArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link AddArithmeticExprRenderer}.
 *
 * <p>These tests verify that the renderer:
 * <ul>
 *     <li>Renders simple addition without unnecessary parentheses.</li>
 *     <li>Wraps nested arithmetic operands in parentheses when needed.</li>
 *     <li>Delegates to the {@link SqlWriter} with the correct
 *         {@code wrapInParentheses} flag for left and right operands.</li>
 * </ul>
 * The tests use the public rendering pipeline to exercise the renderer
 * in a realistic context.</p>
 */
class AddArithmeticExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void renders_simple_add_without_parentheses() {
        // a + b
        Expression expr = col("a").add(col("b"));

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // Outer SELECT/ FROM shape may be dialect-dependent, but the core
        // expression should be "a + b" without parentheses.
        assertEquals("SELECT a + b FROM t", normalize(sql));
    }

    @Test
    void renders_left_nested_add_with_parentheses() {
        // (a + b) + c
        ArithmeticExpr inner = AddArithmeticExpr.of(col("a"), col("b"));
        ArithmeticExpr expr = AddArithmeticExpr.of(inner, col("c"));

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // The left operand is an ArithmeticExpr, so it should be wrapped in
        // parentheses by the renderer.
        assertEquals("SELECT (a + b) + c FROM t", normalize(sql));
    }

    @Test
    void renders_right_nested_add_with_parentheses() {
        // a + (b + c)
        ArithmeticExpr inner = AddArithmeticExpr.of(col("b"), col("c"));
        ArithmeticExpr expr = AddArithmeticExpr.of(col("a"), inner);

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // The right operand is an ArithmeticExpr, so it should be wrapped in
        // parentheses by the renderer.
        assertEquals("SELECT a + (b + c) FROM t", normalize(sql));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
