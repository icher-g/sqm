package io.sqm.render.defaults;

import io.sqm.core.ColumnExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.render.RenderTestDialect;
import io.sqm.render.SqlText;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSqlWriterTest {

    @Test
    void appendsStringsSpacesAndIgnoresEmpty() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append("SELECT").space().append("1").append("").append((String) null);

        assertEquals("SELECT 1", writer.toText(List.of()).sql());
    }

    @Test
    void appendsNodesAndCommaSeparatedLists() {
        var dialect = new RenderTestDialect()
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append(ColumnExpr.of("a"));
        writer.space();
        writer.comma(List.of(ColumnExpr.of("b"), ColumnExpr.of("c")), true);

        assertEquals("a (b), (c)", writer.toText(List.of()).sql());
    }

    @Test
    void supportsSingleLineMode() {
        var dialect = new RenderTestDialect()
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append("a");
        writer.singleLine();
        writer.newline();
        writer.append("b");
        writer.multiLine();
        writer.newline();
        writer.append("c");

        assertEquals("a b\nc", writer.toText(List.of()).sql());
    }

    @Test
    void enclosesAndIndentsMultiline() {
        var dialect = new RenderTestDialect()
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);
        SqlWriter writer = new DefaultSqlWriter(ctx, 2);

        writer.append(ColumnExpr.of("col"), true, true);

        assertEquals("(\n  col\n)", writer.toText(List.of()).sql());
    }

    @Test
    void toTextReturnsParams() {
        var dialect = new RenderTestDialect()
            .register(new LiteralRenderer())
            .register(new ParamRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append(OrdinalParamExpr.of(1));
        SqlText text = writer.toText(List.of(10));

        assertEquals("$1", text.sql());
        assertEquals(List.of(10), text.params());
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

    private static final class ParamRenderer implements Renderer<OrdinalParamExpr> {
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
