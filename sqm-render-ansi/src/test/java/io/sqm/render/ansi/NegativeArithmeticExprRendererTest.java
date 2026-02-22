package io.sqm.render.ansi;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link NegativeArithmeticExprRenderer}.
 *
 * <p>These tests verify that the renderer:
 * <ul>
 *     <li>Renders unary negation without unnecessary parentheses.</li>
 *     <li>Wraps nested arithmetic operands in parentheses when needed.</li>
 * </ul>
 * The tests exercise the renderer through the public rendering pipeline.</p>
 */
class NegativeArithmeticExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void renders_simple_neg_without_parentheses() {
        // -a
        Expression expr = col("a").neg();

        var query = select(expr).from(tbl("t")).build();
        String sql = ctx.render(query).sql();

        assertEquals("SELECT -a FROM t", normalize(sql));
    }

    @Test
    void renders_nested_neg_with_parentheses_for_arithmetic_operand() {
        // -(a + b)
        ArithmeticExpr inner = col("a").add(col("b"));
        Expression expr = inner.neg();

        var query = select(expr).from(tbl("t")).build();
        String sql = ctx.render(query).sql();

        assertEquals("SELECT -(a + b) FROM t", normalize(sql));
    }
}
