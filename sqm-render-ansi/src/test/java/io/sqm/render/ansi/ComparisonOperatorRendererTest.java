package io.sqm.render.ansi;

import io.sqm.core.ComparisonOperator;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for ComparisonOperatorRenderer.
 */
class ComparisonOperatorRendererTest {

    private final ComparisonOperatorRenderer renderer = new ComparisonOperatorRenderer();
    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    @DisplayName("EQ operator renders as '='")
    void eq_operator() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(ComparisonOperator.EQ, ctx, w);
        assertEquals("=", w.toText(null).sql());
    }

    @Test
    @DisplayName("NE operator renders as '<>'")
    void ne_operator() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(ComparisonOperator.NE, ctx, w);
        assertEquals("<>", w.toText(null).sql());
    }

    @Test
    @DisplayName("LT operator renders as '<'")
    void lt_operator() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(ComparisonOperator.LT, ctx, w);
        assertEquals("<", w.toText(null).sql());
    }

    @Test
    @DisplayName("LTE operator renders as '<='")
    void lte_operator() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(ComparisonOperator.LTE, ctx, w);
        assertEquals("<=", w.toText(null).sql());
    }

    @Test
    @DisplayName("GT operator renders as '>'")
    void gt_operator() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(ComparisonOperator.GT, ctx, w);
        assertEquals(">", w.toText(null).sql());
    }

    @Test
    @DisplayName("GTE operator renders as '>='")
    void gte_operator() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(ComparisonOperator.GTE, ctx, w);
        assertEquals(">=", w.toText(null).sql());
    }
}
