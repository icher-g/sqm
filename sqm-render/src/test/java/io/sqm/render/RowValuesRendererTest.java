package io.sqm.render;

import io.sqm.core.RowValues;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RowValuesRendererTest {

    @Test
    void delegatesSingleRowToRowExprRenderer() {
        var dialect = new RenderTestDialect()
            .register(new RowValuesRenderer())
            .register(new RowExprRenderer());
        var ctx = RenderContext.of(dialect);

        RowValues rowValues = row(lit(1));
        var sql = ctx.render(rowValues).sql();

        assertEquals("ROW-EXPR-STUB", sql);
    }

    @Test
    void delegatesRowListToRowListRenderer() {
        var dialect = new RenderTestDialect()
            .register(new RowValuesRenderer())
            .register(new RowListRenderer());
        var ctx = RenderContext.of(dialect);

        RowValues rowValues = io.sqm.core.RowListExpr.of(java.util.List.of(row(lit(1)), row(lit(2))));
        var sql = ctx.render(rowValues).sql();

        assertEquals("ROW-LIST-STUB", sql);
    }

    private static final class RowExprRenderer implements Renderer<io.sqm.core.RowExpr> {
        @Override
        public void render(io.sqm.core.RowExpr node, RenderContext ctx, SqlWriter w) {
            w.append("ROW-EXPR-STUB");
        }

        @Override
        public Class<io.sqm.core.RowExpr> targetType() {
            return io.sqm.core.RowExpr.class;
        }
    }

    private static final class RowListRenderer implements Renderer<io.sqm.core.RowListExpr> {
        @Override
        public void render(io.sqm.core.RowListExpr node, RenderContext ctx, SqlWriter w) {
            w.append("ROW-LIST-STUB");
        }

        @Override
        public Class<io.sqm.core.RowListExpr> targetType() {
            return io.sqm.core.RowListExpr.class;
        }
    }
}
