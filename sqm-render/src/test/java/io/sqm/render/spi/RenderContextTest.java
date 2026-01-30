package io.sqm.render.spi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.render.RenderTestDialect;
import io.sqm.render.SqlText;
import io.sqm.render.SqlWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderContextTest {

    @Test
    void renderInlineUsesFormatterAndNoParams() {
        var dialect = new RenderTestDialect()
            .register(new LiteralRenderer())
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);

        SqlText text = ctx.render(Expression.literal(7));

        assertEquals("7", text.sql());
        assertTrue(text.params().isEmpty());
    }

    @Test
    void renderBindParameterizesLiterals() {
        var dialect = new RenderTestDialect()
            .register(new OrdinalParamRenderer());
        var ctx = RenderContext.of(dialect);

        SqlText text = ctx.render(Expression.literal(9), RenderOptions.of(ParameterizationMode.Bind));

        assertEquals("$1", text.sql());
        assertEquals(1, text.params().size());
        assertEquals(9, text.params().getFirst());
    }

    @Test
    void beforeRenderThrowsWhenParamsAlreadyPresent() {
        var dialect = new RenderTestDialect();

        var ex = assertThrows(IllegalStateException.class, () ->
            dialect.beforeRender(OrdinalParamExpr.of(1), RenderOptions.of(ParameterizationMode.Bind)));

        assertTrue(ex.getMessage().contains("already has parameters"));
    }

    private static final class LiteralRenderer implements Renderer<LiteralExpr> {
        @Override
        public void render(LiteralExpr node, RenderContext ctx, SqlWriter w) {
            w.append(ctx.dialect().formatter().format(node.value()));
        }

        @Override
        public Class<LiteralExpr> targetType() {
            return LiteralExpr.class;
        }
    }

    private static final class ColumnRenderer implements Renderer<ColumnExpr> {
        @Override
        public void render(ColumnExpr node, RenderContext ctx, SqlWriter w) {
            w.append(node.name());
        }

        @Override
        public Class<ColumnExpr> targetType() {
            return ColumnExpr.class;
        }
    }

    private static final class OrdinalParamRenderer implements Renderer<OrdinalParamExpr> {
        @Override
        public void render(OrdinalParamExpr node, RenderContext ctx, SqlWriter w) {
            w.append("$" + node.index());
        }

        @Override
        public Class<OrdinalParamExpr> targetType() {
            return OrdinalParamExpr.class;
        }
    }
}
