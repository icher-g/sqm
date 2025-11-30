package io.sqm.render.ansi;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.core.ModArithmeticExpr;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ModArithmeticExprRenderer}.
 *
 * <p>These tests verify that the renderer:
 * <ul>
 *     <li>Renders simple modulo operations without unnecessary parentheses.</li>
 *     <li>Wraps nested arithmetic operands in parentheses when needed.</li>
 * </ul>
 * The tests exercise the renderer through the public rendering pipeline.</p>
 */
class ModArithmeticExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void renders_simple_mod_without_parentheses() {
        // a % b   (or equivalent dialect-specific symbol)
        Expression expr = col("a").mod(col("b"));

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        // Adjust this if your AnsiDialect renders MOD as a function instead.
        assertEquals("SELECT MOD(a, b) FROM t", normalize(sql));
    }

    @Test
    void renders_left_nested_mod_with_parentheses() {
        // (a % b) % c
        ArithmeticExpr inner = ModArithmeticExpr.of(col("a"), col("b"));
        ArithmeticExpr expr = ModArithmeticExpr.of(inner, col("c"));

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        assertEquals("SELECT MOD(MOD(a, b), c) FROM t", normalize(sql));
    }

    @Test
    void renders_right_nested_mod_with_parentheses() {
        // a % (b % c)
        ArithmeticExpr inner = ModArithmeticExpr.of(col("b"), col("c"));
        ArithmeticExpr expr = ModArithmeticExpr.of(col("a"), inner);

        var query = select(expr).from(tbl("t"));
        String sql = ctx.render(query).sql();

        assertEquals("SELECT MOD(a, MOD(b, c)) FROM t", normalize(sql));
    }
}
