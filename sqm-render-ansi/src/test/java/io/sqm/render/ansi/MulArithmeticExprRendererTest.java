package io.sqm.render.ansi;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.core.MulArithmeticExpr;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MulArithmeticExprRenderer}.
 *
 * <p>These tests verify that the renderer:
 * <ul>
 *     <li>Renders simple multiplication without unnecessary parentheses.</li>
 *     <li>Wraps nested arithmetic operands in parentheses when needed.</li>
 * </ul>
 * The tests exercise the renderer through the public rendering pipeline.</p>
 */
class MulArithmeticExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void renders_simple_mul_without_parentheses() {
        // a * b
        Expression expr = col("a").mul(col("b"));

        var query = select(expr).from(tbl("t")).build();
        String sql = ctx.render(query).sql();

        assertEquals("SELECT a * b FROM t", normalize(sql));
    }

    @Test
    void renders_left_nested_mul_with_parentheses() {
        // (a * b) * c
        ArithmeticExpr inner = MulArithmeticExpr.of(col("a"), col("b"));
        ArithmeticExpr expr = MulArithmeticExpr.of(inner, col("c"));

        var query = select(expr).from(tbl("t")).build();
        String sql = ctx.render(query).sql();

        assertEquals("SELECT a * b * c FROM t", normalize(sql));
    }

    @Test
    void renders_right_nested_mul_with_parentheses() {
        // a * (b * c)
        ArithmeticExpr inner = MulArithmeticExpr.of(col("b"), col("c"));
        ArithmeticExpr expr = MulArithmeticExpr.of(col("a"), inner);

        var query = select(expr).from(tbl("t")).build();
        String sql = ctx.render(query).sql();

        assertEquals("SELECT a * b * c FROM t", normalize(sql));
    }
}
