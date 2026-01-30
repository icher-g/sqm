package io.sqm.render.spi;

import io.sqm.core.Expression;
import io.sqm.core.RowExpr;
import io.sqm.core.ValuesTable;
import io.sqm.render.RenderTestDialect;
import io.sqm.render.SqlWriter;
import io.sqm.render.defaults.DefaultSqlWriter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RendererDefaultsTest {

    @Test
    void renderAliasedWritesAliasAndColumns() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var values = ValuesTable.of(RowExpr.of(List.of(Expression.literal(1))))
            .as("v")
            .columnAliases("c1", "c2");

        new ValuesTableRenderer().renderAliased(values, ctx, w);

        assertEquals("AS \"v\"(\"c1\", \"c2\")", w.toText(List.of()).sql());
    }

    @Test
    void renderAliasedSkipsWhenNoAlias() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var values = ValuesTable.of(RowExpr.of(List.of(Expression.literal(1))));

        new ValuesTableRenderer().renderAliased(values, ctx, w);

        assertEquals("", w.toText(List.of()).sql());
    }

    private static final class ValuesTableRenderer implements Renderer<ValuesTable> {
        @Override
        public void render(ValuesTable node, RenderContext ctx, SqlWriter w) {
            w.append("VALUES");
        }

        @Override
        public Class<ValuesTable> targetType() {
            return ValuesTable.class;
        }
    }
}
